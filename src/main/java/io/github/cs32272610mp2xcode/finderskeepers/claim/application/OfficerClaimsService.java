package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.Availability;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.DecisionKind;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.DecisionReview;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.Feedback;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.HistoryClaimRow;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.HistoryFilter;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.OfficerClaimDetail;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.OfficerClaimHandle;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.PendingClaimRow;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.View;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimTextPolicy;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;

/** Stateful plain-Java workflow for one authenticated Desk Officer Claims workspace. */
public final class OfficerClaimsService {
    private static final Comparator<PendingClaimRow> PENDING_ORDER =
            Comparator.comparing(PendingClaimRow::submittedAt)
                    .thenComparing(row -> row.handle().claimId().value().toString());

    private static final Comparator<HistoryClaimRow> HISTORY_ORDER =
            Comparator.comparing(HistoryClaimRow::terminalAt).reversed()
                    .thenComparing(row -> row.handle().claimId().value().toString());

    private final ClaimRepository claimRepository;

    private final ReportRepository reportRepository;

    private final Clock clock;

    private OfficerClaimsState state = emptyState();

    private Map<ClaimId, Claim> claimsById = Map.of();

    private Map<UUID, ItemReport> reportsById = Map.of();

    /**
     * Creates a workflow bound to one authenticated Desk Officer.
     *
     * @param user authenticated Desk Officer identity
     * @param claims shared Claim repository
     * @param reports shared canonical report repository
     * @param eventClock UTC-capable event clock
     */
    public OfficerClaimsService(AuthenticatedUser user, ClaimRepository claims,
            ReportRepository reports, Clock eventClock) {
        Objects.requireNonNull(user, "user");
        if (user.role() != UserRole.DESK_OFFICER) {
            throw new IllegalArgumentException("Officer Claims requires the Desk Officer role.");
        }
        claimRepository = Objects.requireNonNull(claims, "claims");
        reportRepository = Objects.requireNonNull(reports, "reports");
        clock = Objects.requireNonNull(eventClock, "clock");
    }

    /**
     * Enters Claims on Pending review and performs an authoritative load.
     *
     * @return current immutable state
     */
    public OfficerClaimsState enter() {
        return loadPending(null, null);
    }

    /**
     * Explicitly refreshes the pending queue.
     *
     * @return current immutable state
     */
    public OfficerClaimsState refreshPending() {
        ClaimId selection = selectedId();
        return loadPending(selection, null);
    }

    /**
     * Retries the pending queue after load failure.
     *
     * @return current immutable state
     */
    public OfficerClaimsState retryPending() {
        return state.pendingAvailability() == Availability.UNAVAILABLE
                ? loadPending(null, null) : state;
    }

    /**
     * Explicitly refreshes terminal Claim history.
     *
     * @return current immutable state
     */
    public OfficerClaimsState refreshHistory() {
        ClaimId selection = state.selectedView() == View.CLAIM_HISTORY ? selectedId() : null;
        return loadHistory(selection, null);
    }

    /**
     * Retries Claim history after load failure.
     *
     * @return current immutable state
     */
    public OfficerClaimsState retryHistory() {
        return state.historyAvailability() == Availability.UNAVAILABLE
                ? loadHistory(null, null) : state;
    }

    /**
     * Applies exactly one approved history filter.
     *
     * @param filter approved history filter
     * @return filtered current state
     */
    public OfficerClaimsState changeHistoryFilter(HistoryFilter filter) {
        Objects.requireNonNull(filter, "filter");
        state = new OfficerClaimsState(state.pendingAvailability(),
                state.historyAvailability(), View.CLAIM_HISTORY, filter,
                state.pendingRows(), state.historyRows(), Optional.empty(), Optional.empty());
        return loadHistory(null, null);
    }

    /**
     * Selects one pending queue row without changing Claim state.
     *
     * @param handle opaque pending-row handle
     * @return resulting immutable state
     */
    public OfficerClaimsState selectPending(OfficerClaimHandle handle) {
        return select(handle, View.PENDING_REVIEW, state.pendingRows().stream()
                .map(PendingClaimRow::handle).toList());
    }

    /**
     * Selects one terminal history row without changing Claim state.
     *
     * @param handle opaque history-row handle
     * @return resulting immutable state
     */
    public OfficerClaimsState selectHistory(OfficerClaimHandle handle) {
        return select(handle, View.CLAIM_HISTORY, state.historyRows().stream()
                .map(HistoryClaimRow::handle).toList());
    }

    /**
     * Validates decision text before an irreversible confirmation is offered.
     *
     * @param handle opaque selected pending Claim
     * @param kind explicit approval or rejection
     * @param rawReason raw optional or mandatory reason text
     * @return validated read-only confirmation value
     */
    public DecisionReview reviewDecision(OfficerClaimHandle handle,
            DecisionKind kind, String rawReason) {
        Objects.requireNonNull(kind, "kind");
        OfficerClaimDetail detail = state.selectedDetail()
                .filter(selected -> selected.handle().equals(handle))
                .filter(selected -> selected.status() == ClaimStatus.PENDING_REVIEW)
                .filter(selected -> selected.lostReport().isPresent()
                        && selected.foundReport().isPresent())
                .orElseThrow(() -> new IllegalArgumentException(
                        "The selected Claim is not currently reviewable."));
        Optional<String> reason = kind == DecisionKind.REJECT
                ? Optional.of(ClaimTextPolicy.requiredDecisionReason(rawReason))
                : ClaimTextPolicy.optionalDecisionReason(Optional.ofNullable(rawReason));
        return new DecisionReview(handle, detail.claimReference(), kind, reason);
    }

    /**
     * Revalidates and atomically approves one confirmed review.
     *
     * @param review confirmed approval review
     * @return resulting immutable state
     */
    public OfficerClaimsState approve(DecisionReview review) {
        requireKind(review, DecisionKind.APPROVE);
        return decide(review);
    }

    /**
     * Revalidates and atomically rejects one confirmed review.
     *
     * @param review confirmed rejection review
     * @return resulting immutable state
     */
    public OfficerClaimsState reject(DecisionReview review) {
        requireKind(review, DecisionKind.REJECT);
        return decide(review);
    }

    /**
     * Clears all in-memory Claim and private report detail without storage writes.
     *
     * @return cleared immutable state
     */
    public OfficerClaimsState clear() {
        claimsById = Map.of();
        reportsById = Map.of();
        state = emptyState();
        return state;
    }

    private OfficerClaimsState decide(DecisionReview review) {
        Objects.requireNonNull(review, "review");
        CurrentSnapshot snapshot = readCurrentForDecision();
        if (snapshot == null) {
            return state;
        }
        Claim current = snapshot.claims().get(review.handle().claimId());
        if (current == null) {
            state = withSelection(Optional.empty(), Feedback.INVALID_SELECTION,
                    View.PENDING_REVIEW);
            return state;
        }
        if (current.status().isTerminal()) {
            refreshAfterDecision(current, Feedback.ALREADY_TERMINAL, false);
            return state;
        }
        if (!snapshot.reports().containsKey(current.lostReportId())
                || !snapshot.reports().containsKey(current.foundReportId())) {
            state = withSelection(Optional.of(detail(current, snapshot.reports())),
                    Feedback.REPORT_UNAVAILABLE, View.PENDING_REVIEW);
            return state;
        }
        Instant terminalAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        try {
            ClaimRepository.TerminalResult result = review.kind() == DecisionKind.APPROVE
                    ? claimRepository.approve(current.claimId(),
                            review.normalizedReason(), terminalAt)
                    : claimRepository.reject(current.claimId(),
                            review.normalizedReason().orElseThrow(), terminalAt);
            switch (result.outcome()) {
                case CHANGED -> refreshAfterDecision(result.claim().orElseThrow(),
                        review.kind() == DecisionKind.APPROVE
                                ? Feedback.APPROVED : Feedback.REJECTED, true);
                case ALREADY_TERMINAL -> refreshAfterDecision(
                        result.claim().orElseThrow(), Feedback.ALREADY_TERMINAL, false);
                case NOT_FOUND, NOT_AUTHORIZED -> state = withSelection(
                        Optional.empty(), Feedback.INVALID_SELECTION, View.PENDING_REVIEW);
                default -> throw new IllegalStateException("Unsupported terminal outcome.");
            }
        } catch (ClaimStoreException failure) {
            state = withFeedback(Feedback.DECISION_FAILED);
        }
        return state;
    }

    private void refreshAfterDecision(Claim claim, Feedback feedback, boolean clearPending) {
        loadPending(null, null);
        loadHistory(claim.claimId(), null);
        Optional<OfficerClaimDetail> selected = clearPending
                ? Optional.empty() : Optional.of(detail(claim, reportsById));
        state = new OfficerClaimsState(state.pendingAvailability(),
                state.historyAvailability(), View.PENDING_REVIEW, state.historyFilter(),
                state.pendingRows(), state.historyRows(), selected, Optional.of(feedback));
    }

    private CurrentSnapshot readCurrentForDecision() {
        try {
            Map<ClaimId, Claim> claims = indexClaims(claimRepository.loadAll());
            Map<UUID, ItemReport> reports = indexReports(reportRepository.loadAll());
            return new CurrentSnapshot(claims, reports);
        } catch (ClaimStoreException | ReportStoreException
                | IllegalArgumentException failure) {
            state = withFeedback(Feedback.DECISION_FAILED);
            return null;
        }
    }

    private OfficerClaimsState loadPending(ClaimId selectedId, Feedback feedback) {
        try {
            List<Claim> claims = claimRepository.loadAll();
            Map<UUID, ItemReport> reports = indexReports(reportRepository.loadAll());
            Map<ClaimId, Claim> indexed = indexClaims(claims);
            List<PendingClaimRow> rows = claims.stream()
                    .filter(claim -> claim.status() == ClaimStatus.PENDING_REVIEW)
                    .map(claim -> pendingRow(claim, reports))
                    .sorted(PENDING_ORDER)
                    .toList();
            Optional<OfficerClaimDetail> selected = Optional.ofNullable(selectedId)
                    .map(indexed::get)
                    .filter(claim -> claim.status() == ClaimStatus.PENDING_REVIEW)
                    .map(claim -> detail(claim, reports));
            claimsById = indexed;
            reportsById = reports;
            state = new OfficerClaimsState(Availability.READY,
                    state.historyAvailability(), View.PENDING_REVIEW,
                    state.historyFilter(), rows, state.historyRows(), selected,
                    Optional.ofNullable(feedback));
        } catch (ClaimStoreException | ReportStoreException
                | IllegalArgumentException failure) {
            claimsById = Map.of();
            reportsById = Map.of();
            state = new OfficerClaimsState(Availability.UNAVAILABLE,
                    state.historyAvailability(), View.PENDING_REVIEW,
                    state.historyFilter(), List.of(), state.historyRows(), Optional.empty(),
                    Optional.of(Feedback.LOAD_FAILED));
        }
        return state;
    }

    private OfficerClaimsState loadHistory(ClaimId selectedId, Feedback feedback) {
        try {
            List<Claim> claims = claimRepository.loadAll();
            Map<UUID, ItemReport> reports = indexReports(reportRepository.loadAll());
            Map<ClaimId, Claim> indexed = indexClaims(claims);
            HistoryFilter filter = state.historyFilter();
            List<HistoryClaimRow> rows = claims.stream()
                    .filter(claim -> claim.status().isTerminal())
                    .filter(claim -> included(filter, claim.status()))
                    .map(claim -> historyRow(claim, reports))
                    .sorted(HISTORY_ORDER)
                    .toList();
            Optional<OfficerClaimDetail> selected = Optional.ofNullable(selectedId)
                    .map(indexed::get)
                    .filter(claim -> claim.status().isTerminal())
                    .filter(claim -> included(filter, claim.status()))
                    .map(claim -> detail(claim, reports));
            claimsById = indexed;
            reportsById = reports;
            state = new OfficerClaimsState(state.pendingAvailability(),
                    Availability.READY, View.CLAIM_HISTORY, filter,
                    state.pendingRows(), rows, selected, Optional.ofNullable(feedback));
        } catch (ClaimStoreException | ReportStoreException
                | IllegalArgumentException failure) {
            claimsById = Map.of();
            reportsById = Map.of();
            state = new OfficerClaimsState(state.pendingAvailability(),
                    Availability.UNAVAILABLE, View.CLAIM_HISTORY,
                    state.historyFilter(), state.pendingRows(), List.of(), Optional.empty(),
                    Optional.of(Feedback.LOAD_FAILED));
        }
        return state;
    }

    private OfficerClaimsState select(OfficerClaimHandle handle, View view,
            List<OfficerClaimHandle> validHandles) {
        if (handle == null || !validHandles.contains(handle)) {
            state = withSelection(Optional.empty(), Feedback.INVALID_SELECTION, view);
            return state;
        }
        Claim claim = claimsById.get(handle.claimId());
        if (claim == null) {
            state = withSelection(Optional.empty(), Feedback.INVALID_SELECTION, view);
            return state;
        }
        state = withSelection(Optional.of(detail(claim, reportsById)), null, view);
        return state;
    }

    private OfficerClaimsState withFeedback(Feedback feedback) {
        return new OfficerClaimsState(state.pendingAvailability(),
                state.historyAvailability(), state.selectedView(), state.historyFilter(),
                state.pendingRows(), state.historyRows(), state.selectedDetail(),
                Optional.of(feedback));
    }

    private OfficerClaimsState withSelection(Optional<OfficerClaimDetail> selected,
            Feedback feedback, View view) {
        return new OfficerClaimsState(state.pendingAvailability(),
                state.historyAvailability(), view, state.historyFilter(),
                state.pendingRows(), state.historyRows(), selected,
                Optional.ofNullable(feedback));
    }

    private ClaimId selectedId() {
        return state.selectedDetail().map(detail -> detail.handle().claimId()).orElse(null);
    }

    private static PendingClaimRow pendingRow(Claim claim,
            Map<UUID, ItemReport> reports) {
        ItemReport lost = reports.get(claim.lostReportId());
        ItemReport found = reports.get(claim.foundReportId());
        return new PendingClaimRow(new OfficerClaimHandle(claim.claimId()),
                claim.claimId().reference(),
                Optional.ofNullable(lost).map(ItemReport::itemName),
                Optional.ofNullable(found).map(ItemReport::itemName),
                Optional.ofNullable(found).map(ItemReport::category), claim.submittedAt());
    }

    private static HistoryClaimRow historyRow(Claim claim,
            Map<UUID, ItemReport> reports) {
        ItemReport lost = reports.get(claim.lostReportId());
        ItemReport found = reports.get(claim.foundReportId());
        return new HistoryClaimRow(new OfficerClaimHandle(claim.claimId()),
                claim.claimId().reference(),
                Optional.ofNullable(lost).map(ItemReport::itemName),
                Optional.ofNullable(found).map(ItemReport::itemName),
                Optional.ofNullable(found).map(ItemReport::category), claim.status(),
                claim.terminalAt().orElseThrow());
    }

    private static OfficerClaimDetail detail(Claim claim,
            Map<UUID, ItemReport> reports) {
        return new OfficerClaimDetail(new OfficerClaimHandle(claim.claimId()),
                claim.claimId().reference(), claim.claimantUserId(),
                claim.ownershipEvidence(), claim.status(), claim.submittedAt(),
                claim.terminalAt(), claim.decisionReason(),
                Optional.ofNullable(reports.get(claim.lostReportId())),
                Optional.ofNullable(reports.get(claim.foundReportId())));
    }

    private static boolean included(HistoryFilter filter, ClaimStatus status) {
        return switch (filter) {
            case ALL -> status.isTerminal();
            case APPROVED -> status == ClaimStatus.APPROVED;
            case REJECTED -> status == ClaimStatus.REJECTED;
            case WITHDRAWN -> status == ClaimStatus.WITHDRAWN;
        };
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

    private static void requireKind(DecisionReview review, DecisionKind expected) {
        Objects.requireNonNull(review, "review");
        if (review.kind() != expected) {
            throw new IllegalArgumentException("Decision review kind does not match the action.");
        }
    }

    private static OfficerClaimsState emptyState() {
        return new OfficerClaimsState(Availability.NOT_LOADED, Availability.NOT_LOADED,
                View.PENDING_REVIEW, HistoryFilter.ALL, List.of(), List.of(),
                Optional.empty(), Optional.empty());
    }

    private record CurrentSnapshot(Map<ClaimId, Claim> claims,
            Map<UUID, ItemReport> reports) {
    }
}
