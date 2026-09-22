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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.Availability;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.DecisionKind;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.Feedback;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.HistoryFilter;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimValidationException;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OfficerClaimsServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-22T03:04:05.678Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    private Path temporaryDirectory;

    @Test
    void bindsOfficerRoleAndListsOnlyPendingOldestFirstWithFoundCategory()
            throws Exception {
        JsonClaimRepository claims = claims("queue.json");
        Claim laterTie = pending(2, 102, 202, NOW.minusSeconds(20));
        Claim earlierTie = pending(1, 101, 201, NOW.minusSeconds(20));
        Claim oldest = pending(3, 103, 203, NOW.minusSeconds(40));
        Claim terminal = pending(4, 104, 204, NOW.minusSeconds(60));
        submitAll(claims, laterTie, earlierTie, oldest, terminal);
        claims.withdraw(terminal.claimId(), terminal.claimantUserId(), NOW.minusSeconds(10));
        MutableReports reports = reportsFor(laterTie, earlierTie, oldest, terminal);
        OfficerClaimsService service = service(claims, reports);

        OfficerClaimsState state = service.enter();

        assertEquals(Availability.READY, state.pendingAvailability());
        assertEquals(List.of(oldest.claimId().reference(), earlierTie.claimId().reference(),
                laterTie.claimId().reference()), state.pendingRows().stream()
                        .map(OfficerClaimsState.PendingClaimRow::claimReference).toList());
        assertEquals(ItemCategory.ELECTRONICS,
                state.pendingRows().getFirst().foundCategory().orElseThrow());
        assertTrue(state.pendingRows().stream()
                .noneMatch(row -> row.claimReference().equals(terminal.claimId().reference())));
        int before = claims.loadAll().size();
        assertTrue(service.selectPending(state.pendingRows().getFirst().handle())
                .selectedDetail().isPresent());
        assertEquals(before, claims.loadAll().size());
        assertThrows(IllegalArgumentException.class, () -> new OfficerClaimsService(
                new AuthenticatedUser("student-1", "student", UserRole.STUDENT),
                claims, reports, CLOCK));
    }

    @Test
    void exposesSensitiveValuesOnlyInSelectedDetailAndDisablesMissingReport()
            throws Exception {
        JsonClaimRepository claims = claims("detail.json");
        Claim claim = pending(1, 101, 201, NOW.minusSeconds(20));
        submitAll(claims, claim);
        MutableReports reports = reportsFor(claim);
        OfficerClaimsService service = service(claims, reports);
        OfficerClaimsState initial = service.enter();

        assertFalse(initial.toString().contains("Synthetic ownership evidence"));
        assertFalse(initial.pendingRows().getFirst().toString().contains("claimant-1"));
        OfficerClaimsState selected = service.selectPending(
                initial.pendingRows().getFirst().handle());

        var detail = selected.selectedDetail().orElseThrow();
        assertEquals("claimant-1", detail.claimantUserId());
        assertEquals("Synthetic ownership evidence 1.", detail.ownershipEvidence());
        assertEquals("Synthetic private detail 101",
                detail.lostReport().orElseThrow().privateIdentifyingDetail());
        assertEquals("Synthetic private detail 201",
                detail.foundReport().orElseThrow().privateIdentifyingDetail());
        assertTrue(selected.decisionsEnabled());
        assertFalse(detail.toString().contains(detail.ownershipEvidence()));

        reports.values.removeIf(report -> report.reportId().equals(claim.foundReportId()));
        OfficerClaimsState missing = service.refreshPending();
        assertTrue(missing.pendingRows().getFirst().foundCategory().isEmpty());
        assertTrue(missing.selectedDetail().orElseThrow().foundReport().isEmpty());
        assertFalse(missing.decisionsEnabled());
        assertThrows(IllegalArgumentException.class, () -> service.reviewDecision(
                missing.pendingRows().getFirst().handle(), DecisionKind.APPROVE, ""));
    }

    @Test
    void filtersHistoryExactlyAndOrdersNewestTerminalFirst() throws Exception {
        JsonClaimRepository claims = claims("history.json");
        Claim approved = pending(1, 101, 201, NOW.minusSeconds(80));
        Claim rejected = pending(2, 102, 202, NOW.minusSeconds(70));
        Claim withdrawn = pending(3, 103, 203, NOW.minusSeconds(60));
        Claim pending = pending(4, 104, 204, NOW.minusSeconds(50));
        submitAll(claims, approved, rejected, withdrawn, pending);
        claims.approve(approved.claimId(), Optional.empty(), NOW.minusSeconds(10));
        claims.reject(rejected.claimId(), "Synthetic rejection.", NOW.minusSeconds(5));
        claims.withdraw(withdrawn.claimId(), withdrawn.claimantUserId(),
                NOW.minusSeconds(5));
        MutableReports reports = reportsFor(approved, rejected, withdrawn, pending);
        OfficerClaimsService service = service(claims, reports);

        OfficerClaimsState all = service.refreshHistory();

        assertEquals(List.of(rejected.claimId().reference(), withdrawn.claimId().reference(),
                approved.claimId().reference()), all.historyRows().stream()
                        .map(OfficerClaimsState.HistoryClaimRow::claimReference).toList());
        assertEquals(List.of(HistoryFilter.ALL, HistoryFilter.APPROVED,
                HistoryFilter.REJECTED, HistoryFilter.WITHDRAWN),
                List.of(HistoryFilter.values()));
        assertEquals(List.of(ClaimStatus.APPROVED),
                service.changeHistoryFilter(HistoryFilter.APPROVED).historyRows().stream()
                        .map(OfficerClaimsState.HistoryClaimRow::status).toList());
        assertEquals(List.of(ClaimStatus.REJECTED),
                service.changeHistoryFilter(HistoryFilter.REJECTED).historyRows().stream()
                        .map(OfficerClaimsState.HistoryClaimRow::status).toList());
        OfficerClaimsState withdrawnOnly = service.changeHistoryFilter(
                HistoryFilter.WITHDRAWN);
        var handle = withdrawnOnly.historyRows().getFirst().handle();
        reports.values.clear();
        OfficerClaimsState retained = service.refreshHistory();
        assertEquals(1, retained.historyRows().size());
        assertTrue(retained.historyRows().getFirst().foundCategory().isEmpty());
        assertTrue(service.selectHistory(handle).selectedDetail().isPresent());
        assertTrue(service.selectHistory(handle).selectedDetail().orElseThrow()
                .lostReport().isEmpty());
    }

    @Test
    void validatesDecisionReasonsAndCommitsDurableDecisions() throws Exception {
        JsonClaimRepository claims = claims("decisions.json");
        Claim claim = pending(1, 101, 201, NOW.minusSeconds(20));
        submitAll(claims, claim);
        MutableReports reports = reportsFor(claim);
        OfficerClaimsService service = service(claims, reports);
        var handle = service.enter().pendingRows().getFirst().handle();
        service.selectPending(handle);

        assertThrows(ClaimValidationException.class,
                () -> service.reviewDecision(handle, DecisionKind.REJECT, "  "));
        var approval = service.reviewDecision(handle, DecisionKind.APPROVE, "  ");
        assertTrue(approval.normalizedReason().isEmpty());
        var rejection = service.reviewDecision(handle, DecisionKind.REJECT,
                "  Synthetic rejection reason.  ");
        assertEquals(Optional.of("Synthetic rejection reason."),
                rejection.normalizedReason());
        assertFalse(rejection.toString().contains("Synthetic rejection reason"));

        OfficerClaimsState decided = service.reject(rejection);

        assertEquals(Optional.of(Feedback.REJECTED), decided.feedback());
        assertTrue(decided.pendingRows().isEmpty());
        assertTrue(decided.selectedDetail().isEmpty());
        assertEquals(ClaimStatus.REJECTED, decided.historyRows().getFirst().status());
        Claim durable = claims("decisions.json").loadAll().getFirst();
        assertEquals(ClaimStatus.REJECTED, durable.status());
        assertEquals(NOW, durable.terminalAt().orElseThrow());
        assertEquals(Optional.of("Synthetic rejection reason."), durable.decisionReason());
        assertTrue(List.of(Claim.class.getDeclaredFields()).stream()
                .noneMatch(field -> field.getName().toLowerCase().contains("officer")));
    }

    @Test
    void staleDecisionReportsAuthoritativeTerminalStateWithoutSecondWrite()
            throws Exception {
        JsonClaimRepository claims = claims("stale.json");
        Claim claim = pending(1, 101, 201, NOW.minusSeconds(20));
        submitAll(claims, claim);
        MutableReports reports = reportsFor(claim);
        OfficerClaimsService first = service(claims, reports);
        OfficerClaimsService later = service(claims, reports);
        var firstHandle = first.enter().pendingRows().getFirst().handle();
        var laterHandle = later.enter().pendingRows().getFirst().handle();
        first.selectPending(firstHandle);
        later.selectPending(laterHandle);
        var approve = first.reviewDecision(firstHandle, DecisionKind.APPROVE, "");
        var reject = later.reviewDecision(laterHandle, DecisionKind.REJECT,
                "Synthetic later reason.");

        assertEquals(Optional.of(Feedback.APPROVED), first.approve(approve).feedback());
        OfficerClaimsState stale = later.reject(reject);

        assertEquals(Optional.of(Feedback.ALREADY_TERMINAL), stale.feedback());
        assertEquals(ClaimStatus.APPROVED,
                stale.selectedDetail().orElseThrow().status());
        assertEquals(ClaimStatus.APPROVED, claims.loadAll().getFirst().status());
        assertTrue(claims.loadAll().getFirst().decisionReason().isEmpty());
    }

    @Test
    void loadAndWriteFailuresAreUnavailableOrRetryableWithoutFalseSuccess()
            throws Exception {
        JsonClaimRepository durable = claims("failures.json");
        Claim claim = pending(1, 101, 201, NOW.minusSeconds(20));
        submitAll(durable, claim);
        SwitchableClaims claims = new SwitchableClaims(durable);
        MutableReports reports = reportsFor(claim);
        OfficerClaimsService service = service(claims, reports);
        claims.failLoads = true;

        OfficerClaimsState failed = service.enter();

        assertEquals(Availability.UNAVAILABLE, failed.pendingAvailability());
        assertFalse(failed.pendingEmpty());
        assertTrue(failed.pendingRows().isEmpty());
        assertTrue(failed.selectedDetail().isEmpty());
        claims.failLoads = false;
        OfficerClaimsState recovered = service.retryPending();
        assertEquals(Availability.READY, recovered.pendingAvailability());
        var handle = recovered.pendingRows().getFirst().handle();
        service.selectPending(handle);
        var review = service.reviewDecision(handle, DecisionKind.APPROVE, "");
        claims.failWrites = true;

        OfficerClaimsState writeFailed = service.approve(review);

        assertEquals(Optional.of(Feedback.DECISION_FAILED), writeFailed.feedback());
        assertEquals(ClaimStatus.PENDING_REVIEW, durable.loadAll().getFirst().status());
        assertTrue(writeFailed.selectedDetail().isPresent());
    }

    @Test
    void committedDecisionRemainsSuccessWhenPostCommitRefreshFails()
            throws Exception {
        JsonClaimRepository durable = claims("post-commit.json");
        Claim claim = pending(1, 101, 201, NOW.minusSeconds(20));
        submitAll(durable, claim);
        SwitchableClaims claims = new SwitchableClaims(durable);
        claims.failLoadsAfterMutation = true;
        MutableReports reports = reportsFor(claim);
        OfficerClaimsService service = service(claims, reports);
        var handle = service.enter().pendingRows().getFirst().handle();
        service.selectPending(handle);
        var review = service.reviewDecision(handle, DecisionKind.APPROVE, "");

        OfficerClaimsState result = service.approve(review);

        assertEquals(Optional.of(Feedback.APPROVED), result.feedback());
        assertEquals(Availability.UNAVAILABLE, result.pendingAvailability());
        assertEquals(Availability.UNAVAILABLE, result.historyAvailability());
        assertEquals(ClaimStatus.APPROVED, durable.loadAll().getFirst().status());
    }

    private OfficerClaimsService service(ClaimRepository claims,
            ReportRepository reports) {
        return new OfficerClaimsService(
                new AuthenticatedUser("officer-1", "officer", UserRole.DESK_OFFICER),
                claims, reports, CLOCK);
    }

    private JsonClaimRepository claims(String name) {
        return new JsonClaimRepository(temporaryDirectory.resolve(name));
    }

    private static Claim pending(long claimId, long lostId, long foundId,
            Instant submittedAt) {
        return Claim.createPending(ClaimId.of(uuid(claimId)), "claimant-" + claimId,
                uuid(lostId), uuid(foundId),
                "Synthetic ownership evidence " + claimId + ".", submittedAt);
    }

    private static void submitAll(ClaimRepository repository, Claim... claims)
            throws ClaimStoreException {
        for (Claim claim : claims) {
            assertEquals(ClaimRepository.SubmissionOutcome.CREATED,
                    repository.submit(claim).outcome());
        }
    }

    private static MutableReports reportsFor(Claim... claims) {
        List<ItemReport> reports = new ArrayList<>();
        for (Claim claim : claims) {
            reports.add(report(claim.lostReportId(), claim.claimantUserId(),
                    ReportType.LOST, "Lost " + claim.claimId().reference(),
                    ItemCategory.BAGS));
            reports.add(report(claim.foundReportId(), "finder-synthetic",
                    ReportType.FOUND, "Found " + claim.claimId().reference(),
                    ItemCategory.ELECTRONICS));
        }
        return new MutableReports(reports);
    }

    private static ItemReport report(UUID id, String reporter, ReportType type,
            String itemName, ItemCategory category) {
        return ItemReport.restore(id, reporter, type, itemName, category,
                "Synthetic location", LocalDate.of(2026, 9, 21),
                "Synthetic public description " + id.getLeastSignificantBits(),
                "Synthetic private detail " + id.getLeastSignificantBits(),
                ReportStatus.SUBMITTED, NOW.minusSeconds(100));
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }

    private static final class MutableReports implements ReportRepository {
        private final List<ItemReport> values;

        MutableReports(List<ItemReport> initial) {
            values = new ArrayList<>(initial);
        }

        @Override
        public List<ItemReport> loadAll() {
            return List.copyOf(values);
        }

        @Override
        public void insert(ItemReport report) {
            throw new AssertionError("Officer Claims must not write reports");
        }

        @Override
        public void replace(UUID targetId, ItemReport replacement) {
            throw new AssertionError("Officer Claims must not write reports");
        }
    }

    private static final class SwitchableClaims implements ClaimRepository {
        private final ClaimRepository delegate;

        private boolean failLoads;

        private boolean failWrites;

        private boolean failLoadsAfterMutation;

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
            return delegate.submit(pending);
        }

        @Override
        public TerminalResult withdraw(ClaimId id, String claimantUserId,
                Instant terminalAt) throws ClaimStoreException {
            return delegate.withdraw(id, claimantUserId, terminalAt);
        }

        @Override
        public TerminalResult approve(ClaimId id, Optional<String> decisionReason,
                Instant terminalAt) throws ClaimStoreException {
            if (failWrites) {
                throw new ClaimStoreException(ClaimStoreException.Reason.WRITE_FAILURE);
            }
            TerminalResult result = delegate.approve(id, decisionReason, terminalAt);
            failLoads = failLoadsAfterMutation;
            return result;
        }

        @Override
        public TerminalResult reject(ClaimId id, String decisionReason,
                Instant terminalAt) throws ClaimStoreException {
            if (failWrites) {
                throw new ClaimStoreException(ClaimStoreException.Reason.WRITE_FAILURE);
            }
            TerminalResult result = delegate.reject(id, decisionReason, terminalAt);
            failLoads = failLoadsAfterMutation;
            return result;
        }
    }
}
