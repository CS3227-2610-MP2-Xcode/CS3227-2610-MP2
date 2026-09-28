package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.Availability;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.Feedback;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.StudentClaimHandle;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.SubmissionReview;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.View;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimValidationException;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPair;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.PossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.PossibleMatchStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudentClaimsServiceTest {
    private static final String STUDENT_ID = "synthetic-student-1";

    private static final Instant NOW = Instant.parse("2026-09-22T03:04:05.678Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final UUID LOST_NEW = uuid(101);

    private static final UUID LOST_OLD = uuid(102);

    private static final UUID FOUND_NEW = uuid(201);

    private static final UUID FOUND_OLD = uuid(202);

    @TempDir
    private Path temporaryDirectory;

    @Test
    void bindsStudentRoleAndDiscoversOnlyEligibleDurableLinks() {
        MutableReports reports = new MutableReports(List.of(
                report(LOST_NEW, STUDENT_ID, ReportType.LOST, "New lost",
                        ItemCategory.BAGS, LocalDate.of(2026, 9, 20), 10),
                report(LOST_OLD, STUDENT_ID, ReportType.LOST, "Old lost",
                        ItemCategory.BOOKS, LocalDate.of(2026, 9, 10), 20),
                report(FOUND_NEW, "finder-1", ReportType.FOUND, "New found",
                        ItemCategory.ELECTRONICS, LocalDate.of(2026, 9, 21), 30),
                report(FOUND_OLD, STUDENT_ID, ReportType.FOUND, "Old found",
                        ItemCategory.STATIONERY, LocalDate.of(2026, 9, 15), 40),
                report(uuid(301), "other-student", ReportType.LOST, "Other lost",
                        ItemCategory.OTHER, LocalDate.of(2026, 9, 19), 50)));
        MutableMatches matches = new MutableMatches(Set.of(
                PossibleMatchPair.of(LOST_NEW, FOUND_OLD),
                PossibleMatchPair.of(LOST_NEW, FOUND_NEW),
                PossibleMatchPair.of(LOST_OLD, FOUND_OLD),
                PossibleMatchPair.of(uuid(301), FOUND_NEW)));
        StudentClaimsService service = service(reports, matches, ids(1));

        StudentClaimsState state = service.enter();

        assertEquals(Availability.READY, state.availableAvailability());
        assertEquals(List.of("New lost", "Old lost"), state.availableGroups().stream()
                .map(StudentClaimsState.AvailableMatchGroup::lostItemName).toList());
        assertEquals(List.of("New found", "Old found"),
                state.availableGroups().getFirst().cards().stream()
                        .map(StudentClaimsState.AvailableMatchCard::foundItemName).toList());
        assertEquals(ItemCategory.ELECTRONICS,
                state.availableGroups().getFirst().cards().getFirst().foundCategory());
        assertThrows(IllegalArgumentException.class, () -> new StudentClaimsService(
                new AuthenticatedUser("officer-1", "officer", UserRole.DESK_OFFICER),
                claims(), reports, matches, CLOCK, ids(2)));
    }

    @Test
    void reviewIsReadOnlyAndValidatedThenConfirmedSubmissionSurvivesRestart()
            throws Exception {
        MutableReports reports = standardReports();
        MutableMatches matches = standardMatches();
        ClaimRepository repository = claims();
        StudentClaimsService service = service(repository, reports, matches, ids(1));
        StudentClaimsState entered = service.enter();
        var handle = entered.availableGroups().getFirst().cards().getFirst().handle();

        assertTrue(service.beginSubmission(handle).isPresent());
        assertThrows(ClaimValidationException.class,
                () -> service.reviewSubmission(handle, " "));
        assertTrue(repository.loadAll().isEmpty());
        SubmissionReview review = service.reviewSubmission(handle,
                "  Synthetic identifying detail.  ");
        assertEquals("Synthetic identifying detail.", review.normalizedEvidence());

        StudentClaimsState submitted = service.submit(review);

        assertEquals(View.MY_CLAIMS, submitted.selectedView());
        assertEquals(Optional.of(Feedback.SUBMITTED), submitted.feedback());
        assertEquals(ClaimStatus.PENDING_REVIEW,
                submitted.selectedClaim().orElseThrow().status());
        assertEquals(1, new JsonClaimRepository(
                temporaryDirectory.resolve("claims.json")).loadAll().size());
        assertEquals(1, service.refreshAvailable().availableGroups().getFirst()
                .existingClaims().size());
        assertEquals(StudentClaimsState.AVAILABLE_EMPTY_MESSAGE,
                "No available matches right now.");
    }

    @Test
    void oneStudentsClaimHidesSharedFoundItemFromAnotherStudent() throws Exception {
        String secondStudentId = "synthetic-student-2";
        UUID secondLost = uuid(103);
        MutableReports reports = new MutableReports(List.of(
                report(LOST_NEW, STUDENT_ID, ReportType.LOST, "First lost item",
                        ItemCategory.BAGS, LocalDate.of(2026, 9, 20), 10),
                report(secondLost, secondStudentId, ReportType.LOST, "Second lost item",
                        ItemCategory.BAGS, LocalDate.of(2026, 9, 20), 20),
                report(FOUND_NEW, "finder-1", ReportType.FOUND, "Shared found item",
                        ItemCategory.BAGS, LocalDate.of(2026, 9, 21), 30)));
        MutableMatches matches = new MutableMatches(Set.of(
                PossibleMatchPair.of(LOST_NEW, FOUND_NEW),
                PossibleMatchPair.of(secondLost, FOUND_NEW)));
        ClaimRepository repository = claims();
        StudentClaimsService firstStudent = service(repository, reports, matches, ids(1));
        StudentClaimsService secondStudent = new StudentClaimsService(
                new AuthenticatedUser(secondStudentId, "student-two", UserRole.STUDENT),
                repository, reports, matches, CLOCK, ids(2));

        var firstHandle = firstStudent.enter().availableGroups().getFirst()
                .cards().getFirst().handle();
        firstStudent.submit(firstStudent.reviewSubmission(firstHandle,
                "Synthetic evidence from the first Student."));

        StudentClaimsState secondView = secondStudent.enter();
        assertTrue(secondView.availableGroups().isEmpty());
        assertTrue(secondView.availableEmpty());
        assertEquals(Availability.READY, secondView.availableAvailability());
    }

    @Test
    void submissionRevalidatesLinkAndRetriesOnlyIdCollisions() throws Exception {
        MutableReports reports = standardReports();
        MutableMatches matches = standardMatches();
        ClaimRepository repository = claims();
        Claim collision = Claim.createPending(ClaimId.of(uuid(1)), "other-student",
                uuid(999), uuid(998), "Synthetic evidence.", NOW.minusSeconds(10));
        repository.submit(collision);
        StudentClaimsService service = service(repository, reports, matches, ids(1, 2));
        var handle = service.enter().availableGroups().getFirst().cards().getFirst().handle();
        SubmissionReview review = service.reviewSubmission(handle, "Synthetic evidence.");

        assertEquals(Optional.of(Feedback.SUBMITTED), service.submit(review).feedback());
        assertEquals(ClaimId.of(uuid(2)), repository.loadAll().getLast().claimId());

        ClaimRepository staleRepository = new JsonClaimRepository(
                temporaryDirectory.resolve("stale-claims.json"));
        StudentClaimsService staleService = service(
                staleRepository, reports, matches, ids(5));
        var currentHandle = staleService.enter().availableGroups().getFirst()
                .cards().getFirst().handle();
        SubmissionReview currentReview = staleService.reviewSubmission(
                currentHandle, "Synthetic evidence.");
        matches.pairs.clear();

        assertEquals(Optional.of(Feedback.MATCH_NO_LONGER_AVAILABLE),
                staleService.submit(currentReview).feedback());
        assertTrue(staleService.refreshMyClaims().myClaimRows().isEmpty());
        assertTrue(staleRepository.loadAll().isEmpty());
    }

    @Test
    void submissionStopsAfterThreeIdCollisionsWithoutCreatingAClaim()
            throws Exception {
        ClaimRepository repository = claims();
        repository.submit(Claim.createPending(ClaimId.of(uuid(1)), "other-1",
                uuid(1001), uuid(2001), "Synthetic one.", NOW.minusSeconds(30)));
        repository.submit(Claim.createPending(ClaimId.of(uuid(2)), "other-2",
                uuid(1002), uuid(2002), "Synthetic two.", NOW.minusSeconds(20)));
        repository.submit(Claim.createPending(ClaimId.of(uuid(3)), "other-3",
                uuid(1003), uuid(2003), "Synthetic three.", NOW.minusSeconds(10)));
        StudentClaimsService service = service(repository, standardReports(),
                standardMatches(), ids(1, 2, 3));
        var handle = service.enter().availableGroups().getFirst().cards().getFirst().handle();
        SubmissionReview review = service.reviewSubmission(handle, "Synthetic evidence.");

        StudentClaimsState result = service.submit(review);

        assertEquals(Optional.of(Feedback.SUBMISSION_FAILED), result.feedback());
        assertEquals(3, repository.loadAll().size());
        assertTrue(repository.loadAll().stream()
                .noneMatch(claim -> claim.claimantUserId().equals(STUDENT_ID)));
    }

    @Test
    void myClaimsFiltersByClaimantUsesFoundCategoryAndRetainsMissingContext()
            throws Exception {
        MutableReports reports = standardReports();
        MutableMatches matches = standardMatches();
        ClaimRepository repository = claims();
        Claim own = Claim.createPending(ClaimId.of(uuid(1)), STUDENT_ID,
                LOST_NEW, FOUND_NEW, "Own synthetic evidence.", NOW.minusSeconds(10));
        Claim other = Claim.createPending(ClaimId.of(uuid(2)), "other-student",
                uuid(301), uuid(302), "Other synthetic evidence.", NOW);
        repository.submit(own);
        repository.submit(other);
        StudentClaimsService service = service(repository, reports, matches, ids(3));

        StudentClaimsState state = service.refreshMyClaims();

        assertEquals(1, state.myClaimRows().size());
        assertEquals(ItemCategory.ELECTRONICS,
                state.myClaimRows().getFirst().foundCategory().orElseThrow());
        StudentClaimHandle handle = state.myClaimRows().getFirst().handle();
        assertEquals("Own synthetic evidence.",
                service.selectMyClaim(handle).selectedClaim().orElseThrow()
                        .ownershipEvidence());

        reports.values.removeIf(report -> report.reportId().equals(FOUND_NEW));
        StudentClaimsState missing = service.refreshMyClaims();
        assertTrue(missing.myClaimRows().getFirst().foundCategory().isEmpty());
        assertTrue(service.selectMyClaim(missing.myClaimRows().getFirst().handle())
                .selectedClaim().isPresent());
    }

    @Test
    void withdrawalNeedsNeitherReportsNorLinkAndReleasesEligibility() throws Exception {
        MutableReports reports = standardReports();
        MutableMatches matches = standardMatches();
        ClaimRepository repository = claims();
        Claim pending = Claim.createPending(ClaimId.of(uuid(1)), STUDENT_ID,
                LOST_NEW, FOUND_NEW, "Synthetic evidence.", NOW.minusSeconds(10));
        repository.submit(pending);
        StudentClaimsService service = service(repository, reports, matches, ids(2));
        StudentClaimHandle handle = service.refreshMyClaims().myClaimRows().getFirst().handle();
        reports.failLoads = true;
        matches.failLoads = true;

        StudentClaimsState withdrawn = service.withdraw(handle);

        assertEquals(Optional.of(Feedback.WITHDRAWN), withdrawn.feedback());
        assertEquals(ClaimStatus.WITHDRAWN,
                repository.loadAll().getFirst().status());
        assertEquals(NOW, repository.loadAll().getFirst().terminalAt().orElseThrow());
    }

    @Test
    void staleAndFailedWithdrawalsRemainTruthfulAndNonMutating() throws Exception {
        JsonClaimRepository durable = new JsonClaimRepository(
                temporaryDirectory.resolve("withdrawal-claims.json"));
        Claim pending = Claim.createPending(ClaimId.of(uuid(1)), STUDENT_ID,
                LOST_NEW, FOUND_NEW, "Synthetic evidence.", NOW.minusSeconds(10));
        durable.submit(pending);
        MutableReports reports = standardReports();
        MutableMatches matches = standardMatches();
        StudentClaimsService staleService = service(durable, reports, matches, ids(2));
        StudentClaimHandle staleHandle = staleService.refreshMyClaims()
                .myClaimRows().getFirst().handle();
        durable.approve(pending.claimId(), Optional.empty(), NOW.minusSeconds(1));

        StudentClaimsState stale = staleService.withdraw(staleHandle);

        assertEquals(Optional.of(Feedback.ALREADY_TERMINAL), stale.feedback());
        assertEquals(ClaimStatus.APPROVED,
                stale.selectedClaim().orElseThrow().status());

        JsonClaimRepository failureDurable = new JsonClaimRepository(
                temporaryDirectory.resolve("failed-withdrawal-claims.json"));
        Claim failurePending = Claim.createPending(ClaimId.of(uuid(3)), STUDENT_ID,
                LOST_NEW, FOUND_NEW, "Synthetic evidence.", NOW.minusSeconds(10));
        failureDurable.submit(failurePending);
        SwitchableClaims failing = new SwitchableClaims(failureDurable);
        failing.failWithdrawals = true;
        StudentClaimsService failureService = service(failing, reports, matches, ids(4));
        StudentClaimHandle failureHandle = failureService.refreshMyClaims()
                .myClaimRows().getFirst().handle();
        failureService.selectMyClaim(failureHandle);

        StudentClaimsState failed = failureService.withdraw(failureHandle);

        assertEquals(Optional.of(Feedback.WITHDRAWAL_FAILED), failed.feedback());
        assertEquals(ClaimStatus.PENDING_REVIEW,
                failed.selectedClaim().orElseThrow().status());
        assertEquals(ClaimStatus.PENDING_REVIEW,
                failureDurable.loadAll().getFirst().status());
    }

    @Test
    void loadFailureIsUnavailableNotEmptyAndRetryRecovers() {
        MutableReports reports = standardReports();
        MutableMatches matches = standardMatches();
        StudentClaimsService service = service(reports, matches, ids(1));
        reports.failLoads = true;

        StudentClaimsState failed = service.enter();

        assertEquals(Availability.UNAVAILABLE, failed.availableAvailability());
        assertTrue(failed.availableRetryVisible());
        assertFalse(failed.availableEmpty());
        assertTrue(failed.availableGroups().isEmpty());
        reports.failLoads = false;
        assertEquals(Availability.READY, service.retryAvailable().availableAvailability());
    }

    @Test
    void committedSubmissionRemainsSuccessWhenPostCommitRefreshFails()
            throws Exception {
        JsonClaimRepository durable = new JsonClaimRepository(
                temporaryDirectory.resolve("post-commit-claims.json"));
        SwitchableClaims claims = new SwitchableClaims(durable);
        claims.failLoadsAfterSubmission = true;
        MutableReports reports = standardReports();
        MutableMatches matches = standardMatches();
        StudentClaimsService service = service(claims, reports, matches, ids(1));
        var handle = service.enter().availableGroups().getFirst().cards().getFirst().handle();
        SubmissionReview review = service.reviewSubmission(handle, "Synthetic evidence.");

        StudentClaimsState result = service.submit(review);

        assertEquals(Optional.of(Feedback.SUBMITTED), result.feedback());
        assertEquals(Availability.UNAVAILABLE, result.availableAvailability());
        assertEquals(Availability.UNAVAILABLE, result.myClaimsAvailability());
        assertEquals(ClaimStatus.PENDING_REVIEW,
                result.selectedClaim().orElseThrow().status());
        assertEquals(1, durable.loadAll().size());
    }

    @Test
    void clearDropsTransientStateAndFreshLoginRestoresOnlyDurableClaims()
            throws Exception {
        ClaimRepository repository = claims();
        Claim durable = Claim.createPending(ClaimId.of(uuid(1)), STUDENT_ID,
                LOST_NEW, FOUND_NEW, "Synthetic durable evidence.", NOW.minusSeconds(10));
        repository.submit(durable);
        MutableReports reports = standardReports();
        MutableMatches matches = standardMatches();
        StudentClaimsService service = service(repository, reports, matches, ids(2));
        var handle = service.refreshMyClaims().myClaimRows().getFirst().handle();
        service.selectMyClaim(handle);

        StudentClaimsState cleared = service.clear();

        assertEquals(Availability.NOT_LOADED, cleared.availableAvailability());
        assertEquals(Availability.NOT_LOADED, cleared.myClaimsAvailability());
        assertTrue(cleared.availableGroups().isEmpty());
        assertTrue(cleared.myClaimRows().isEmpty());
        assertTrue(cleared.selectedClaim().isEmpty());
        assertTrue(cleared.feedback().isEmpty());
        assertEquals(1, repository.loadAll().size());
        StudentClaimsState fresh = service(repository, reports, matches, ids(3))
                .refreshMyClaims();
        assertEquals(1, fresh.myClaimRows().size());
        assertTrue(fresh.selectedClaim().isEmpty());
        assertTrue(fresh.feedback().isEmpty());
    }

    private StudentClaimsService service(MutableReports reports,
            MutableMatches matches, Supplier<UUID> ids) {
        return service(claims(), reports, matches, ids);
    }

    private static StudentClaimsService service(ClaimRepository claims,
            MutableReports reports, MutableMatches matches, Supplier<UUID> ids) {
        return new StudentClaimsService(
                new AuthenticatedUser(STUDENT_ID, "student", UserRole.STUDENT),
                claims, reports, matches, CLOCK, ids);
    }

    private ClaimRepository claims() {
        return new JsonClaimRepository(temporaryDirectory.resolve("claims.json"));
    }

    private static MutableReports standardReports() {
        return new MutableReports(List.of(
                report(LOST_NEW, STUDENT_ID, ReportType.LOST, "Lost item",
                        ItemCategory.BAGS, LocalDate.of(2026, 9, 20), 10),
                report(FOUND_NEW, "finder-1", ReportType.FOUND, "Found item",
                        ItemCategory.ELECTRONICS, LocalDate.of(2026, 9, 21), 20)));
    }

    private static MutableMatches standardMatches() {
        return new MutableMatches(Set.of(PossibleMatchPair.of(LOST_NEW, FOUND_NEW)));
    }

    private static ItemReport report(UUID id, String reporter, ReportType type,
            String name, ItemCategory category, LocalDate date, long createdSeconds) {
        return ItemReport.restore(id, reporter, type, name, category,
                "Synthetic location", date, "Synthetic public description",
                "Synthetic private detail", ReportStatus.SUBMITTED,
                NOW.minusSeconds(createdSeconds));
    }

    private static Supplier<UUID> ids(long... values) {
        Queue<UUID> queue = new ArrayDeque<>();
        for (long value : values) {
            queue.add(uuid(value));
        }
        return queue::remove;
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }

    private static final class MutableReports implements ReportRepository {
        private final List<ItemReport> values;

        private boolean failLoads;

        MutableReports(List<ItemReport> reports) {
            values = new ArrayList<>(reports);
        }

        @Override
        public List<ItemReport> loadAll() throws ReportStoreException {
            if (failLoads) {
                throw new ReportStoreException(ReportStoreException.Reason.CORRUPT_OR_UNSUPPORTED_STORE);
            }
            return List.copyOf(values);
        }

        @Override
        public void insert(ItemReport report) {
            throw new AssertionError("Student Claims must not write reports");
        }

        @Override
        public void replace(UUID targetId, ItemReport replacement) {
            throw new AssertionError("Student Claims must not write reports");
        }
    }

    private static final class MutableMatches implements PossibleMatchRepository {
        private Set<PossibleMatchPair> pairs;

        private boolean failLoads;

        MutableMatches(Set<PossibleMatchPair> initial) {
            pairs = new HashSet<>(initial);
        }

        @Override
        public Set<PossibleMatchPair> loadAll() throws PossibleMatchStoreException {
            if (failLoads) {
                throw new PossibleMatchStoreException(
                        PossibleMatchStoreException.Reason.CORRUPT_STORE);
            }
            return Set.copyOf(pairs);
        }

        @Override
        public boolean link(PossibleMatchPair pair) {
            throw new AssertionError("Student Claims must not write links");
        }

        @Override
        public boolean unlink(PossibleMatchPair pair) {
            throw new AssertionError("Student Claims must not write links");
        }
    }

    private static final class SwitchableClaims implements ClaimRepository {
        private final ClaimRepository delegate;

        private boolean failLoads;

        private boolean failLoadsAfterSubmission;

        private boolean failWithdrawals;

        SwitchableClaims(ClaimRepository repository) {
            delegate = repository;
        }

        @Override
        public List<Claim> loadAll() throws ClaimStoreException {
            if (failLoads) {
                throw new ClaimStoreException(ClaimStoreException.Reason.READ_FAILURE);
            }
            return delegate.loadAll();
        }

        @Override
        public SubmissionResult submit(Claim pending) throws ClaimStoreException {
            SubmissionResult result = delegate.submit(pending);
            if (result.outcome() == SubmissionOutcome.CREATED) {
                failLoads = failLoadsAfterSubmission;
            }
            return result;
        }

        @Override
        public TerminalResult withdraw(ClaimId id, String claimantUserId,
                Instant terminalAt) throws ClaimStoreException {
            if (failWithdrawals) {
                throw new ClaimStoreException(ClaimStoreException.Reason.WRITE_FAILURE);
            }
            return delegate.withdraw(id, claimantUserId, terminalAt);
        }

        @Override
        public TerminalResult approve(ClaimId id, Optional<String> decisionReason,
                Instant terminalAt) throws ClaimStoreException {
            return delegate.approve(id, decisionReason, terminalAt);
        }

        @Override
        public TerminalResult reject(ClaimId id, String decisionReason,
                Instant terminalAt) throws ClaimStoreException {
            return delegate.reject(id, decisionReason, terminalAt);
        }
    }
}
