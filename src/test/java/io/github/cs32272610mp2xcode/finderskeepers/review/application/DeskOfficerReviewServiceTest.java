package io.github.cs32272610mp2xcode.finderskeepers.review.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.claim.application.ApprovedClaimReportService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeskOfficerReviewServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-22T03:04:05.678Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void approvedClaimHidesBothEndpointsAndLeavesCanonicalReportsUnchanged()
            throws Exception {
        ItemReport lost = report(101, ReportType.LOST, ReportStatus.SUBMITTED);
        ItemReport found = report(201, ReportType.FOUND, ReportStatus.UNDER_REVIEW);
        ItemReport unrelated = report(301, ReportType.FOUND, ReportStatus.SUBMITTED);
        InMemoryReports reports = new InMemoryReports(List.of(lost, found, unrelated));
        JsonClaimRepository claims = claims("approved.json");
        Claim claim = pending(1, lost.reportId(), found.reportId());
        claims.submit(claim);
        claims.approve(claim.claimId(), Optional.empty(), NOW);

        ReviewQueueState state = service(reports, claims).enter();

        assertEquals(List.of(unrelated), state.visibleReports());
        assertEquals(List.of(lost, found, unrelated), reports.loadAll());
        assertEquals(ReportStatus.SUBMITTED, lost.status());
        assertEquals(ReportStatus.UNDER_REVIEW, found.status());
        assertEquals(1, claims.loadAll().size());
        assertEquals(io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus.APPROVED,
                claims.loadAll().getFirst().status());
    }

    @Test
    void pendingRejectedAndWithdrawnClaimsHideNeitherEndpoint() throws Exception {
        Claim pending = pending(1, uuid(101), uuid(201));
        Claim rejected = pending(2, uuid(102), uuid(202));
        Claim withdrawn = pending(3, uuid(103), uuid(203));
        JsonClaimRepository claims = claims("non-approved.json");
        claims.submit(pending);
        claims.submit(rejected);
        claims.submit(withdrawn);
        claims.reject(rejected.claimId(), "Synthetic rejection.", NOW);
        claims.withdraw(withdrawn.claimId(), withdrawn.claimantUserId(), NOW);
        List<ItemReport> reports = List.of(
                report(101, ReportType.LOST, ReportStatus.SUBMITTED),
                report(201, ReportType.FOUND, ReportStatus.SUBMITTED),
                report(102, ReportType.LOST, ReportStatus.SUBMITTED),
                report(202, ReportType.FOUND, ReportStatus.SUBMITTED),
                report(103, ReportType.LOST, ReportStatus.UNDER_REVIEW),
                report(203, ReportType.FOUND, ReportStatus.UNDER_REVIEW),
                report(999, ReportType.FOUND, ReportStatus.SUBMITTED));

        ReviewQueueState state = service(new InMemoryReports(reports), claims).enter();

        assertEquals(reports, state.visibleReports());
    }

    @Test
    void refreshAppliesNewApprovalAndClearsExcludedSelection() throws Exception {
        ItemReport lost = report(101, ReportType.LOST, ReportStatus.SUBMITTED);
        ItemReport found = report(201, ReportType.FOUND, ReportStatus.SUBMITTED);
        InMemoryReports reports = new InMemoryReports(List.of(lost, found));
        JsonClaimRepository claims = claims("refresh.json");
        Claim claim = pending(1, lost.reportId(), found.reportId());
        claims.submit(claim);
        DeskOfficerReviewService service = service(reports, claims);
        service.enter();
        service.select(lost.reportId());
        claims.approve(claim.claimId(), Optional.empty(), NOW);

        ReviewQueueState refreshed = service.refresh();

        assertTrue(refreshed.visibleReports().isEmpty());
        assertTrue(refreshed.selectedReport().isEmpty());
        assertEquals("No active reports.", refreshed.queueMessage().orElseThrow());
    }

    @Test
    void submittedAndPersistedUnderReviewReportsRemainInspectable() {
        ItemReport submitted = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        ItemReport underReview = report(2, ReportType.FOUND, ReportStatus.UNDER_REVIEW);
        DeskOfficerReviewService service = new DeskOfficerReviewService(
                new InMemoryReports(List.of(submitted, underReview)));

        ReviewQueueState state = service.enter();

        assertEquals(List.of(submitted, underReview), state.visibleReports());
        assertSame(underReview, service.select(underReview.reportId())
                .selectedReport().orElseThrow());
        assertTrue(Arrays.stream(DeskOfficerReviewService.class.getMethods())
                .noneMatch(method -> method.getName().equals("startReview")));
    }

    @Test
    void filtersPreserveOrderSelectionAndUseActiveCopy() {
        ItemReport firstLost = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        ItemReport found = report(2, ReportType.FOUND, ReportStatus.UNDER_REVIEW);
        ItemReport lastLost = report(3, ReportType.LOST, ReportStatus.SUBMITTED);
        DeskOfficerReviewService service = new DeskOfficerReviewService(
                new InMemoryReports(List.of(firstLost, found, lastLost)));
        service.enter();
        service.select(firstLost.reportId());

        ReviewQueueState lost = service.changeFilter(ReviewQueueFilter.LOST);
        ReviewQueueState foundOnly = service.changeFilter(ReviewQueueFilter.FOUND);

        assertEquals(List.of(firstLost, lastLost), lost.visibleReports());
        assertSame(firstLost, lost.selectedReport().orElseThrow());
        assertEquals(List.of(found), foundOnly.visibleReports());
        assertTrue(foundOnly.selectedReport().isEmpty());
        assertEquals(List.of(ReviewQueueFilter.ALL, ReviewQueueFilter.LOST,
                ReviewQueueFilter.FOUND), List.of(ReviewQueueFilter.values()));
    }

    @Test
    void unavailableReportOrClaimStoreRequiresRetryWithoutStaleRows() throws Exception {
        ItemReport report = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        JsonClaimRepository claims = claims("failure.json");
        InMemoryReports reports = new InMemoryReports(List.of(report));
        DeskOfficerReviewService service = service(reports, claims);
        assertEquals(List.of(report), service.enter().visibleReports());
        reports.failLoads = true;

        ReviewQueueState unavailable = service.refresh();

        assertFalse(unavailable.available());
        assertTrue(unavailable.visibleReports().isEmpty());
        assertTrue(unavailable.retryVisible());
        assertEquals("Reports are unavailable. Please try again.",
                unavailable.detailsMessage());
        reports.failLoads = false;
        assertEquals(List.of(report), service.retry().visibleReports());
    }

    @Test
    void stateIsImmutableAndUnknownSelectionIsIgnored() {
        ItemReport report = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        DeskOfficerReviewService service = new DeskOfficerReviewService(
                new InMemoryReports(List.of(report)));
        ReviewQueueState state = service.enter();

        assertThrows(UnsupportedOperationException.class,
                () -> state.visibleReports().clear());
        assertTrue(service.select(uuid(999)).selectedReport().isEmpty());
    }

    private DeskOfficerReviewService service(ReportRepository reports,
            ClaimRepository claims) {
        return new DeskOfficerReviewService(reports,
                new ApprovedClaimReportService(claims));
    }

    private JsonClaimRepository claims(String name) {
        return new JsonClaimRepository(temporaryDirectory.resolve(name));
    }

    private static Claim pending(long id, UUID lost, UUID found) {
        return Claim.createPending(ClaimId.of(uuid(id)), "synthetic-student-" + id,
                lost, found, "Synthetic ownership evidence.", NOW.minusSeconds(60));
    }

    private static ItemReport report(long id, ReportType type, ReportStatus status) {
        return ItemReport.restore(uuid(id), "synthetic-reporter-" + id, type,
                "Synthetic item " + id, ItemCategory.OTHER,
                "Synthetic location", LocalDate.of(2026, 9, 20),
                "Synthetic public description", "Synthetic private detail",
                status, NOW.minusSeconds(100));
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }

    private static final class InMemoryReports implements ReportRepository {
        private final List<ItemReport> reports;

        private boolean failLoads;

        InMemoryReports(List<ItemReport> initial) {
            reports = new ArrayList<>(initial);
        }

        @Override
        public List<ItemReport> loadAll() throws ReportStoreException {
            if (failLoads) {
                throw new ReportStoreException(
                        ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE);
            }
            return List.copyOf(reports);
        }

        @Override
        public void insert(ItemReport report) {
            throw new AssertionError("Report review must not insert reports");
        }

        @Override
        public void replace(UUID targetId, ItemReport replacement) {
            throw new AssertionError("Report review must not replace reports");
        }
    }
}
