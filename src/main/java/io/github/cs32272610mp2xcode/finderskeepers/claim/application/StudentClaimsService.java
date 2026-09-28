package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.Availability;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.AvailableMatchCard;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.AvailableMatchGroup;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.AvailableMatchHandle;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.ExistingClaimNotice;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.Feedback;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.MyClaimRow;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.SafeMatchSummary;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.StudentClaimDetail;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.StudentClaimHandle;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.SubmissionReview;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.View;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimLedger;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimTextPolicy;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPair;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.PossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.PossibleMatchStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;

/** Stateful plain-Java workflow for one authenticated Student Claims workspace. */
public final class StudentClaimsService {
    private static final int MAXIMUM_ID_ATTEMPTS = 3;

    private static final Comparator<GroupBuilder> GROUP_ORDER =
            Comparator.comparing((GroupBuilder group) -> group.lostReport.createdAt())
                    .reversed()
                    .thenComparing(group -> group.lostReport.reportId().toString());

    private static final Comparator<AvailableMatchCard> CARD_ORDER =
            Comparator.comparing(AvailableMatchCard::foundOccurrenceDate).reversed()
                    .thenComparing(card -> card.handle().foundReportId().toString());

    private static final Comparator<MyClaimRow> MY_CLAIM_ORDER =
            Comparator.comparing(MyClaimRow::submittedAt).reversed()
                    .thenComparing(row -> row.handle().claimId().value().toString());

    private final String claimantUserId;

    private final ClaimRepository claimRepository;

    private final ReportRepository reportRepository;

    private final PossibleMatchRepository matchRepository;

    private final Clock clock;

    private final Supplier<UUID> claimIdSupplier;

    private StudentClaimsState state = emptyState();

    private Map<UUID, ItemReport> reportsById = Map.of();

    private Map<ClaimId, Claim> claimsById = Map.of();

    /**
     * Creates a workflow bound to one authenticated Student.
     *
     * @param user authenticated Student identity
     * @param claims shared Claim repository
     * @param reports shared canonical report repository
     * @param matches shared durable possible-match repository
     * @param eventClock UTC-capable event clock
     * @param idSupplier Claim UUID source
     */
    public StudentClaimsService(AuthenticatedUser user, ClaimRepository claims,
            ReportRepository reports, PossibleMatchRepository matches,
            Clock eventClock, Supplier<UUID> idSupplier) {
        Objects.requireNonNull(user, "user");
        if (user.role() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Student Claims requires the Student role.");
        }
        claimantUserId = user.userId();
        claimRepository = Objects.requireNonNull(claims, "claims");
        reportRepository = Objects.requireNonNull(reports, "reports");
        matchRepository = Objects.requireNonNull(matches, "matches");
        clock = Objects.requireNonNull(eventClock, "clock");
        claimIdSupplier = Objects.requireNonNull(idSupplier, "idSupplier");
    }

    /**
     * Enters Claims on Available matches and performs an authoritative load.
     *
     * @return current immutable state
     */
    public StudentClaimsState enter() {
        return loadAvailable(null);
    }

    /**
     * Explicitly refreshes Available matches.
     *
     * @return current immutable state
     */
    public StudentClaimsState refreshAvailable() {
        return loadAvailable(null);
    }

    /**
     * Retries Available matches after a failed load.
     *
     * @return current immutable state
     */
    public StudentClaimsState retryAvailable() {
        return state.availableAvailability() == Availability.UNAVAILABLE
                ? loadAvailable(null) : state;
    }

    /**
     * Explicitly refreshes My claims.
     *
     * @return current immutable state
     */
    public StudentClaimsState refreshMyClaims() {
        ClaimId selection = state.selectedClaim()
                .map(detail -> detail.handle().claimId()).orElse(null);
        return loadMyClaims(selection, null);
    }

    /**
     * Retries My claims after a failed load.
     *
     * @return current immutable state
     */
    public StudentClaimsState retryMyClaims() {
        return state.myClaimsAvailability() == Availability.UNAVAILABLE
                ? loadMyClaims(null, null) : state;
    }

    /**
     * Opens one current Available-match target without creating a Claim.
     *
     * @param handle opaque current card handle
     * @return safe summary when the card remains in the current state
     */
    public Optional<SafeMatchSummary> beginSubmission(AvailableMatchHandle handle) {
        return currentCard(handle).map(StudentClaimsService::summary);
    }

    /**
     * Validates evidence and creates a read-only confirmation value.
     *
     * @param handle opaque current card handle
     * @param rawEvidence raw ownership evidence
     * @return validated review value
     */
    public SubmissionReview reviewSubmission(AvailableMatchHandle handle,
            String rawEvidence) {
        AvailableMatchCard card = currentCard(handle).orElseThrow(
                () -> new IllegalArgumentException("The selected match is no longer available."));
        return new SubmissionReview(card.handle(), summary(card),
                ClaimTextPolicy.evidence(rawEvidence));
    }

    /**
     * Revalidates and atomically submits a confirmed review value.
     *
     * @param review previously validated confirmation value
     * @return resulting immutable state
     */
    public StudentClaimsState submit(SubmissionReview review) {
        Objects.requireNonNull(review, "review");
        SubmissionSnapshot snapshot = readSubmissionSnapshot();
        if (snapshot == null) {
            return state;
        }
        ItemReport lost = snapshot.reports().get(review.handle().lostReportId());
        ItemReport found = snapshot.reports().get(review.handle().foundReportId());
        if (!eligibleReportsAndLink(lost, found, review.handle(), snapshot.links())) {
            loadAvailable(Feedback.MATCH_NO_LONGER_AVAILABLE);
            return state;
        }

        Instant submittedAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        for (int attempt = 0; attempt < MAXIMUM_ID_ATTEMPTS; attempt++) {
            Claim candidate = Claim.createPending(
                    ClaimId.of(Objects.requireNonNull(claimIdSupplier.get(), "claimId")),
                    claimantUserId, lost.reportId(), found.reportId(),
                    review.normalizedEvidence(), submittedAt);
            try {
                ClaimRepository.SubmissionResult result = claimRepository.submit(candidate);
                switch (result.outcome()) {
                    case CREATED -> {
                        Claim created = result.claim().orElseThrow();
                        loadAvailable(null);
                        loadMyClaims(created.claimId(), Feedback.SUBMITTED);
                        return ensureCommittedVisible(created, Feedback.SUBMITTED);
                    }
                    case ID_COLLISION -> {
                        // Try another injected UUID for this same confirmed action.
                    }
                    case OWN_ACTIVE_CLAIM -> {
                        Claim existing = result.claim().orElseThrow();
                        loadAvailable(null);
                        return loadMyClaims(existing.claimId(), Feedback.OWN_ACTIVE_CLAIM);
                    }
                    case BLOCKED -> {
                        return loadAvailable(Feedback.MATCH_NO_LONGER_AVAILABLE);
                    }
                    default -> throw new IllegalStateException("Unsupported submission outcome.");
                }
            } catch (ClaimStoreException failure) {
                state = withFeedback(state, Feedback.SUBMISSION_FAILED);
                return state;
            }
        }
        state = withFeedback(state, Feedback.SUBMISSION_FAILED);
        return state;
    }

    /**
     * Selects one row belonging to the authenticated Student.
     *
     * @param handle opaque current My-claims handle
     * @return resulting immutable state
     */
    public StudentClaimsState selectMyClaim(StudentClaimHandle handle) {
        if (handle == null || state.myClaimRows().stream()
                .noneMatch(row -> row.handle().equals(handle))) {
            state = withSelection(state, Optional.empty(), Feedback.INVALID_SELECTION);
            return state;
        }
        Claim claim = claimsById.get(handle.claimId());
        if (claim == null || !claim.claimantUserId().equals(claimantUserId)) {
            state = withSelection(state, Optional.empty(), Feedback.INVALID_SELECTION);
            return state;
        }
        state = withSelection(state, Optional.of(detail(claim, reportsById)), null);
        return state;
    }

    /**
     * Atomically withdraws a selected owned Pending review Claim.
     *
     * <p>This operation deliberately does not require reports or a possible-match link.</p>
     *
     * @param handle opaque own-Claim handle
     * @return resulting immutable state
     */
    public StudentClaimsState withdraw(StudentClaimHandle handle) {
        if (handle == null) {
            state = withFeedback(state, Feedback.INVALID_SELECTION);
            return state;
        }
        Instant terminalAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        try {
            ClaimRepository.TerminalResult result = claimRepository.withdraw(
                    handle.claimId(), claimantUserId, terminalAt);
            return switch (result.outcome()) {
                case CHANGED -> refreshAfterWithdrawal(
                        result.claim().orElseThrow(), Feedback.WITHDRAWN);
                case ALREADY_TERMINAL -> refreshAfterWithdrawal(
                        result.claim().orElseThrow(), Feedback.ALREADY_TERMINAL);
                case NOT_FOUND, NOT_AUTHORIZED -> {
                    state = withFeedback(state, Feedback.INVALID_SELECTION);
                    yield state;
                }
            };
        } catch (ClaimStoreException failure) {
            state = withFeedback(state, Feedback.WITHDRAWAL_FAILED);
            return state;
        }
    }

    /**
     * Clears all in-memory Claim snapshots without writing storage.
     *
     * @return cleared immutable state
     */
    public StudentClaimsState clear() {
        reportsById = Map.of();
        claimsById = Map.of();
        state = emptyState();
        return state;
    }

    private StudentClaimsState refreshAfterWithdrawal(Claim claim, Feedback feedback) {
        loadAvailable(null);
        loadMyClaims(claim.claimId(), feedback);
        return ensureCommittedVisible(claim, feedback);
    }

    private StudentClaimsState ensureCommittedVisible(Claim claim, Feedback feedback) {
        Optional<StudentClaimDetail> selected = state.selectedClaim();
        if (selected.isEmpty() || !selected.orElseThrow().handle().claimId()
                .equals(claim.claimId())) {
            selected = Optional.of(detail(claim, Map.of()));
        }
        state = new StudentClaimsState(state.availableAvailability(),
                state.myClaimsAvailability(), View.MY_CLAIMS, state.availableGroups(),
                state.myClaimRows(), selected, Optional.of(feedback));
        return state;
    }

    private StudentClaimsState loadAvailable(Feedback feedback) {
        try {
            Map<UUID, ItemReport> reports = indexReports(reportRepository.loadAll());
            Set<PossibleMatchPair> links = Set.copyOf(matchRepository.loadAll());
            List<Claim> claims = claimRepository.loadAll();
            ClaimLedger ledger = ClaimLedger.from(claims);
            List<AvailableMatchGroup> groups = buildAvailableGroups(reports, links, ledger);
            reportsById = reports;
            claimsById = indexClaims(claims);
            state = new StudentClaimsState(Availability.READY,
                    state.myClaimsAvailability(), View.AVAILABLE_MATCHES, groups,
                    state.myClaimRows(), state.selectedClaim(),
                    Optional.ofNullable(feedback));
        } catch (ReportStoreException | PossibleMatchStoreException
                | ClaimStoreException | IllegalArgumentException failure) {
            reportsById = Map.of();
            state = new StudentClaimsState(Availability.UNAVAILABLE,
                    state.myClaimsAvailability(), View.AVAILABLE_MATCHES, List.of(),
                    state.myClaimRows(), state.selectedClaim(),
                    Optional.of(Feedback.LOAD_FAILED));
        }
        return state;
    }

    private StudentClaimsState loadMyClaims(ClaimId selectedId, Feedback feedback) {
        try {
            List<Claim> claims = claimRepository.loadAll();
            Map<UUID, ItemReport> reports = indexReports(reportRepository.loadAll());
            Map<ClaimId, Claim> indexedClaims = indexClaims(claims);
            List<MyClaimRow> rows = claims.stream()
                    .filter(claim -> claim.claimantUserId().equals(claimantUserId))
                    .map(claim -> row(claim, reports))
                    .sorted(MY_CLAIM_ORDER)
                    .toList();
            Optional<StudentClaimDetail> selected = Optional.ofNullable(selectedId)
                    .map(indexedClaims::get)
                    .filter(claim -> claim.claimantUserId().equals(claimantUserId))
                    .map(claim -> detail(claim, reports));
            reportsById = reports;
            claimsById = indexedClaims;
            state = new StudentClaimsState(state.availableAvailability(),
                    Availability.READY, View.MY_CLAIMS, state.availableGroups(), rows,
                    selected, Optional.ofNullable(feedback));
        } catch (ClaimStoreException | ReportStoreException
                | IllegalArgumentException failure) {
            claimsById = Map.of();
            reportsById = Map.of();
            state = new StudentClaimsState(state.availableAvailability(),
                    Availability.UNAVAILABLE, View.MY_CLAIMS, state.availableGroups(),
                    List.of(), Optional.empty(), Optional.of(Feedback.LOAD_FAILED));
        }
        return state;
    }

    private SubmissionSnapshot readSubmissionSnapshot() {
        try {
            Map<UUID, ItemReport> reports = indexReports(reportRepository.loadAll());
            Set<PossibleMatchPair> links = Set.copyOf(matchRepository.loadAll());
            ClaimLedger.from(claimRepository.loadAll());
            return new SubmissionSnapshot(reports, links);
        } catch (ReportStoreException | PossibleMatchStoreException
                | ClaimStoreException | IllegalArgumentException failure) {
            state = withFeedback(state, Feedback.SUBMISSION_FAILED);
            return null;
        }
    }

    private boolean eligibleReportsAndLink(ItemReport lost, ItemReport found,
            AvailableMatchHandle handle, Set<PossibleMatchPair> links) {
        return lost != null && found != null
                && lost.reportType() == ReportType.LOST
                && found.reportType() == ReportType.FOUND
                && lost.reporterId().equals(claimantUserId)
                && links.contains(PossibleMatchPair.of(
                        handle.lostReportId(), handle.foundReportId()));
    }

    private List<AvailableMatchGroup> buildAvailableGroups(
            Map<UUID, ItemReport> reports, Set<PossibleMatchPair> links,
            ClaimLedger ledger) {
        Map<UUID, GroupBuilder> groups = new LinkedHashMap<>();
        for (PossibleMatchPair pair : links) {
            OrientedPair oriented = orient(pair, reports).orElse(null);
            if (oriented == null) {
                continue;
            }
            ClaimLedger.SubmissionEvaluation evaluation = ledger.evaluate(
                    claimantUserId, oriented.lost().reportId(), oriented.found().reportId());
            GroupBuilder group = groups.computeIfAbsent(oriented.lost().reportId(),
                    ignored -> new GroupBuilder(oriented.lost()));
            switch (evaluation.eligibility()) {
                case ELIGIBLE -> group.cards.add(card(oriented));
                case OWN_ACTIVE_CLAIM -> group.notices.add(new ExistingClaimNotice(
                        new StudentClaimHandle(
                                evaluation.ownActiveClaim().orElseThrow().claimId()),
                        oriented.lost().itemName()));
                case BLOCKED -> {
                    // Hidden by design; revealing the blocker would leak another Claim.
                }
                default -> throw new IllegalStateException("Unsupported eligibility.");
            }
        }
        return groups.values().stream()
                .peek(group -> {
                    group.cards.sort(CARD_ORDER);
                    group.notices = new ArrayList<>(new java.util.LinkedHashSet<>(group.notices));
                })
                .filter(group -> !group.cards.isEmpty() || !group.notices.isEmpty())
                .sorted(GROUP_ORDER)
                .map(GroupBuilder::build)
                .toList();
    }

    private Optional<OrientedPair> orient(PossibleMatchPair pair,
            Map<UUID, ItemReport> reports) {
        ItemReport first = reports.get(pair.firstId());
        ItemReport second = reports.get(pair.secondId());
        if (isOwnedLost(first) && isFound(second)) {
            return Optional.of(new OrientedPair(first, second));
        }
        if (isOwnedLost(second) && isFound(first)) {
            return Optional.of(new OrientedPair(second, first));
        }
        return Optional.empty();
    }

    private boolean isOwnedLost(ItemReport report) {
        return report != null && report.reportType() == ReportType.LOST
                && report.reporterId().equals(claimantUserId);
    }

    private static boolean isFound(ItemReport report) {
        return report != null && report.reportType() == ReportType.FOUND;
    }

    private static AvailableMatchCard card(OrientedPair pair) {
        return new AvailableMatchCard(new AvailableMatchHandle(
                pair.lost().reportId(), pair.found().reportId()),
                pair.lost().itemName(), pair.found().itemName(), pair.found().category(),
                pair.found().occurrenceDate(), pair.found().location());
    }

    private Optional<AvailableMatchCard> currentCard(AvailableMatchHandle handle) {
        if (handle == null || state.availableAvailability() != Availability.READY) {
            return Optional.empty();
        }
        return state.availableGroups().stream()
                .flatMap(group -> group.cards().stream())
                .filter(card -> card.handle().equals(handle))
                .findFirst();
    }

    private static SafeMatchSummary summary(AvailableMatchCard card) {
        return new SafeMatchSummary(Optional.of(card.lostItemName()),
                Optional.of(card.foundItemName()), Optional.of(card.foundCategory()),
                Optional.of(card.foundOccurrenceDate()), Optional.of(card.foundLocation()));
    }

    private static MyClaimRow row(Claim claim, Map<UUID, ItemReport> reports) {
        ItemReport lost = reports.get(claim.lostReportId());
        ItemReport found = reports.get(claim.foundReportId());
        return new MyClaimRow(new StudentClaimHandle(claim.claimId()),
                Optional.ofNullable(lost).map(ItemReport::itemName),
                Optional.ofNullable(found).map(ItemReport::itemName),
                Optional.ofNullable(found).map(ItemReport::category),
                claim.status(), claim.submittedAt());
    }

    private static StudentClaimDetail detail(Claim claim,
            Map<UUID, ItemReport> reports) {
        ItemReport lost = reports.get(claim.lostReportId());
        ItemReport found = reports.get(claim.foundReportId());
        SafeMatchSummary summary = new SafeMatchSummary(
                Optional.ofNullable(lost).map(ItemReport::itemName),
                Optional.ofNullable(found).map(ItemReport::itemName),
                Optional.ofNullable(found).map(ItemReport::category),
                Optional.ofNullable(found).map(ItemReport::occurrenceDate),
                Optional.ofNullable(found).map(ItemReport::location));
        return new StudentClaimDetail(new StudentClaimHandle(claim.claimId()),
                claim.claimId().reference(), claim.ownershipEvidence(), claim.status(),
                summary, claim.submittedAt(), claim.terminalAt(), claim.decisionReason());
    }

    private static Map<UUID, ItemReport> indexReports(List<ItemReport> reports) {
        Map<UUID, ItemReport> indexed = new HashMap<>();
        for (ItemReport report : reports) {
            if (indexed.put(report.reportId(), report) != null) {
                throw new IllegalArgumentException("Duplicate report identity.");
            }
        }
        return Map.copyOf(indexed);
    }

    private static Map<ClaimId, Claim> indexClaims(List<Claim> claims) {
        Map<ClaimId, Claim> indexed = new HashMap<>();
        for (Claim claim : claims) {
            if (indexed.put(claim.claimId(), claim) != null) {
                throw new IllegalArgumentException("Duplicate Claim identity.");
            }
        }
        return Map.copyOf(indexed);
    }

    private static StudentClaimsState withFeedback(StudentClaimsState source,
            Feedback feedback) {
        return new StudentClaimsState(source.availableAvailability(),
                source.myClaimsAvailability(), source.selectedView(),
                source.availableGroups(), source.myClaimRows(), source.selectedClaim(),
                Optional.ofNullable(feedback));
    }

    private static StudentClaimsState withSelection(StudentClaimsState source,
            Optional<StudentClaimDetail> selection, Feedback feedback) {
        return new StudentClaimsState(source.availableAvailability(),
                source.myClaimsAvailability(), View.MY_CLAIMS,
                source.availableGroups(), source.myClaimRows(), selection,
                Optional.ofNullable(feedback));
    }

    private static StudentClaimsState emptyState() {
        return new StudentClaimsState(Availability.NOT_LOADED, Availability.NOT_LOADED,
                View.AVAILABLE_MATCHES, List.of(), List.of(), Optional.empty(),
                Optional.empty());
    }

    /** Accumulates match cards and notices for one lost report. */
    private static final class GroupBuilder {
        private final ItemReport lostReport;

        private final List<AvailableMatchCard> cards = new ArrayList<>();

        private List<ExistingClaimNotice> notices = new ArrayList<>();

        GroupBuilder(ItemReport lost) {
            lostReport = lost;
        }

        AvailableMatchGroup build() {
            return new AvailableMatchGroup(lostReport.itemName(), cards, notices);
        }
    }

    /** Lost-and-found report pair normalized into a stable orientation. */
    private record OrientedPair(ItemReport lost, ItemReport found) {
    }

    /** Reports and stored links read together for one Claim submission attempt. */
    private record SubmissionSnapshot(Map<UUID, ItemReport> reports,
            Set<PossibleMatchPair> links) {
    }
}
