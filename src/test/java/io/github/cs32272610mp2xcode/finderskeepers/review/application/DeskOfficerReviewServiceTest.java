package io.github.cs32272610mp2xcode.finderskeepers.review.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;
import org.junit.jupiter.api.Test;

class DeskOfficerReviewServiceTest {
    @Test
    void enterBuildsSubmittedQueueInRepositoryOrderAcrossReporters() {
        ItemReport first = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        ItemReport excluded = report(2, ReportType.FOUND, ReportStatus.UNDER_REVIEW);
        ItemReport last = report(3, ReportType.FOUND, ReportStatus.SUBMITTED);
        DeskOfficerReviewService service = new DeskOfficerReviewService(
                new InMemoryRepository(List.of(first, excluded, last)));

        ReviewQueueState state = service.enter();

        assertEquals(ReviewQueueFilter.ALL, state.activeFilter());
        assertEquals(List.of(first, last), state.visibleReports());
        assertTrue(state.selectedReport().isEmpty());
        assertEquals("Select a report to view details.", state.detailsMessage());
        assertTrue(state.queueMessage().isEmpty());
        assertFalse(state.startReviewEnabled());
        assertFalse(state.retryVisible());
        assertThrows(UnsupportedOperationException.class,
                () -> state.visibleReports().add(first));
    }

    @Test
    void globalEmptyStateUsesExactCopy() {
        DeskOfficerReviewService service = new DeskOfficerReviewService(
                new InMemoryRepository(List.of(
                        report(1, ReportType.LOST, ReportStatus.UNDER_REVIEW))));

        ReviewQueueState state = service.enter();

        assertEquals("No submitted reports.", state.queueMessage().orElseThrow());
        assertTrue(state.visibleReports().isEmpty());
        assertTrue(state.selectedReport().isEmpty());
        assertFalse(state.startReviewEnabled());
    }

    @Test
    void filtersAreExactlyAllLostAndFoundAndPreserveRelativeOrder() {
        ItemReport firstLost = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        ItemReport foundReport = report(2, ReportType.FOUND, ReportStatus.SUBMITTED);
        ItemReport lastLost = report(3, ReportType.LOST, ReportStatus.SUBMITTED);
        InMemoryRepository repository = new InMemoryRepository(
                List.of(firstLost, foundReport, lastLost));
        DeskOfficerReviewService service = new DeskOfficerReviewService(repository);
        service.enter();

        ReviewQueueState lost = service.changeFilter(ReviewQueueFilter.LOST);
        ReviewQueueState foundState = service.changeFilter(ReviewQueueFilter.FOUND);

        assertEquals(List.of(ReviewQueueFilter.ALL,
                ReviewQueueFilter.LOST, ReviewQueueFilter.FOUND),
                Arrays.asList(ReviewQueueFilter.values()));
        assertEquals(List.of(firstLost, lastLost), lost.visibleReports());
        assertEquals(List.of(foundReport), foundState.visibleReports());
        assertEquals(0, repository.replacements);
    }

    @Test
    void globalAndFilteredEmptyStatesUseExactDistinctCopy() {
        ItemReport lost = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        DeskOfficerReviewService service = new DeskOfficerReviewService(
                new InMemoryRepository(List.of(lost)));
        service.enter();

        ReviewQueueState found = service.changeFilter(ReviewQueueFilter.FOUND);
        ReviewQueueState restored = service.changeFilter(ReviewQueueFilter.LOST);

        assertEquals("No submitted reports match the Found filter.",
                found.queueMessage().orElseThrow());
        assertTrue(found.visibleReports().isEmpty());
        assertEquals(List.of(lost), restored.visibleReports());
    }

    @Test
    void selectionTracksOnlyVisibleReportsAcrossFilterChanges() {
        ItemReport lost = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        ItemReport found = report(2, ReportType.FOUND, ReportStatus.SUBMITTED);
        DeskOfficerReviewService service = new DeskOfficerReviewService(
                new InMemoryRepository(List.of(lost, found)));
        service.enter();

        ReviewQueueState selected = service.select(lost.reportId());
        ReviewQueueState retained = service.changeFilter(ReviewQueueFilter.LOST);
        ReviewQueueState cleared = service.changeFilter(ReviewQueueFilter.FOUND);
        ReviewQueueState unknown = service.select(new UUID(9L, 9L));

        assertSame(lost, selected.selectedReport().orElseThrow());
        assertTrue(selected.startReviewEnabled());
        assertSame(lost, retained.selectedReport().orElseThrow());
        assertTrue(cleared.selectedReport().isEmpty());
        assertEquals("Select a report to view details.", cleared.detailsMessage());
        assertTrue(unknown.selectedReport().isEmpty());
        assertFalse(unknown.startReviewEnabled());
    }

    @Test
    void stateIsImmutableAndDeterministicForEquivalentInputs() {
        List<ItemReport> reports = List.of(
                report(1, ReportType.LOST, ReportStatus.SUBMITTED));
        ReviewQueueState first = new DeskOfficerReviewService(
                new InMemoryRepository(reports)).enter();
        ReviewQueueState second = new DeskOfficerReviewService(
                new InMemoryRepository(reports)).enter();

        assertEquals(first, second);
        assertThrows(UnsupportedOperationException.class,
                () -> first.visibleReports().clear());
    }

    @Test
    void startReviewPersistsOnlyStatusFromAuthoritativeReport() {
        ItemReport originallySelected = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        ItemReport authoritative = ItemReport.restore(
                originallySelected.reportId(),
                originallySelected.reporterId(),
                ReportType.FOUND,
                "Authoritative synthetic item",
                ItemCategory.BOOKS,
                "Authoritative synthetic location",
                LocalDate.of(2026, 9, 19),
                "Authoritative synthetic public description",
                "Authoritative synthetic private detail",
                ReportStatus.SUBMITTED,
                originallySelected.createdAt());
        InMemoryRepository repository = new InMemoryRepository(List.of(originallySelected));
        DeskOfficerReviewService service = new DeskOfficerReviewService(repository);
        service.enter();
        service.select(originallySelected.reportId());
        repository.setReports(List.of(authoritative));

        ReviewQueueState state = service.startReview();

        assertEquals(List.of(authoritative.withStatus(ReportStatus.UNDER_REVIEW)),
                repository.reports());
        assertEquals("Review started.", state.feedbackMessage().orElseThrow());
        assertEquals("No submitted reports.", state.queueMessage().orElseThrow());
        assertTrue(state.selectedReport().isEmpty());
        assertFalse(state.startReviewEnabled());
    }

    @Test
    void successfulReviewPreservesActiveFilterAndShowsFilteredEmptyState() {
        ItemReport lost = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        ItemReport found = report(2, ReportType.FOUND, ReportStatus.SUBMITTED);
        DeskOfficerReviewService service = new DeskOfficerReviewService(
                new InMemoryRepository(List.of(lost, found)));
        service.enter();
        service.changeFilter(ReviewQueueFilter.LOST);
        service.select(lost.reportId());

        ReviewQueueState state = service.startReview();

        assertEquals(ReviewQueueFilter.LOST, state.activeFilter());
        assertEquals("No submitted reports match the Lost filter.",
                state.queueMessage().orElseThrow());
        assertEquals("Review started.", state.feedbackMessage().orElseThrow());
    }

    @Test
    void invalidRepeatReverseAndNoSelectionCannotMutate() {
        ItemReport report = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        InMemoryRepository repository = new InMemoryRepository(List.of(report));
        DeskOfficerReviewService service = new DeskOfficerReviewService(repository);

        service.enter();
        service.startReview();
        assertEquals(0, repository.replacements);
        service.select(report.reportId());
        service.startReview();
        service.startReview();
        assertEquals(1, repository.replacements);

        DeskOfficerReviewService staleService = new DeskOfficerReviewService(repository);
        repository.setReports(List.of(report));
        staleService.enter();
        staleService.select(report.reportId());
        repository.setReports(List.of(report.withStatus(ReportStatus.UNDER_REVIEW)));
        ReviewQueueState stale = staleService.startReview();

        assertEquals(1, repository.replacements);
        assertEquals("This report is no longer available for review.",
                stale.feedbackMessage().orElseThrow());
        for (Method method : DeskOfficerReviewService.class.getMethods()) {
            assertFalse(Arrays.asList(method.getParameterTypes()).contains(ReportStatus.class));
        }
    }

    @Test
    void missingOrNonSubmittedTargetReconcilesUnderSameFilter() {
        ItemReport selected = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        ItemReport remainingFound = report(2, ReportType.FOUND, ReportStatus.SUBMITTED);
        InMemoryRepository repository = new InMemoryRepository(
                List.of(selected, remainingFound));
        DeskOfficerReviewService service = new DeskOfficerReviewService(repository);
        service.enter();
        service.changeFilter(ReviewQueueFilter.LOST);
        service.select(selected.reportId());
        repository.setReports(List.of(remainingFound));

        ReviewQueueState state = service.startReview();

        assertEquals(ReviewQueueFilter.LOST, state.activeFilter());
        assertEquals("This report is no longer available for review.",
                state.feedbackMessage().orElseThrow());
        assertEquals("No submitted reports match the Lost filter.",
                state.queueMessage().orElseThrow());
        assertTrue(state.selectedReport().isEmpty());
        assertEquals(0, repository.replacements);
    }

    @Test
    void replacementTargetRaceExposesRetryContext() {
        ItemReport selected = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        InMemoryRepository repository = new InMemoryRepository(List.of(selected));
        DeskOfficerReviewService service = new DeskOfficerReviewService(repository);
        service.enter();
        service.changeFilter(ReviewQueueFilter.LOST);
        service.select(selected.reportId());
        repository.failReplacementAndNextLoad();

        ReviewQueueState unavailable = service.startReview();

        assertFalse(unavailable.available());
        assertTrue(unavailable.retryVisible());
        assertEquals("Reports are unavailable. Please try again.",
                unavailable.detailsMessage());
        repository.setReports(List.of());
        ReviewQueueState retried = service.retry();
        assertEquals(ReviewQueueFilter.LOST, retried.activeFilter());
        assertEquals("This report is no longer available for review.",
                retried.feedbackMessage().orElseThrow());
        assertEquals("No submitted reports.", retried.queueMessage().orElseThrow());
    }

    @Test
    void replacementTargetRaceReconcilesWhenReloadSucceeds() {
        ItemReport selected = report(1, ReportType.LOST, ReportStatus.SUBMITTED);
        InMemoryRepository repository = new InMemoryRepository(List.of(selected));
        DeskOfficerReviewService service = new DeskOfficerReviewService(repository);
        service.enter();
        service.select(selected.reportId());
        repository.failNextReplace(
                ReportStoreException.Reason.REPLACEMENT_TARGET_NOT_FOUND);

        ReviewQueueState reconciled = service.startReview();

        assertTrue(reconciled.available());
        assertEquals("This report is no longer available for review.",
                reconciled.feedbackMessage().orElseThrow());
        assertTrue(reconciled.selectedReport().isEmpty());
        assertEquals(List.of(selected), reconciled.visibleReports());
        assertEquals(0, repository.replacements);
    }

    @Test
    void initialLoadFailureRequiresExplicitRetry() {
        InMemoryRepository repository = new InMemoryRepository(List.of());
        repository.failNextLoad(ReportStoreException.Reason.CORRUPT_OR_UNSUPPORTED_STORE);
        DeskOfficerReviewService service = new DeskOfficerReviewService(repository);

        ReviewQueueState unavailable = service.enter();

        assertFalse(unavailable.available());
        assertTrue(unavailable.visibleReports().isEmpty());
        assertTrue(unavailable.queueMessage().isEmpty());
        assertTrue(unavailable.retryVisible());
        assertFalse(unavailable.startReviewEnabled());
        assertEquals("Reports are unavailable. Please try again.",
                unavailable.detailsMessage());
        assertEquals(1, repository.loads);
        ReviewQueueState recovered = service.retry();
        assertTrue(recovered.available());
        assertEquals(2, repository.loads);
        assertEquals("No submitted reports.", recovered.queueMessage().orElseThrow());
    }

    @Test
    void everyInitialLoadFailureMapsToTheSamePrivacySafeState() {
        for (ReportStoreException.Reason reason : ReportStoreException.Reason.values()) {
            InMemoryRepository repository = new InMemoryRepository(List.of());
            repository.failNextLoad(reason);

            ReviewQueueState state = new DeskOfficerReviewService(repository).enter();

            assertFalse(state.available());
            assertEquals("Reports are unavailable. Please try again.",
                    state.detailsMessage());
            assertTrue(state.visibleReports().isEmpty());
            assertTrue(state.feedbackMessage().isEmpty());
            assertTrue(state.retryVisible());
        }
    }

    @Test
    void startReviewPrecheckFailureRetainsRetryableSelection() {
        ItemReport selected = report(1, ReportType.FOUND, ReportStatus.SUBMITTED);
        InMemoryRepository repository = new InMemoryRepository(List.of(selected));
        DeskOfficerReviewService service = new DeskOfficerReviewService(repository);
        service.enter();
        service.select(selected.reportId());
        repository.failNextLoad(
                ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE);

        ReviewQueueState state = service.startReview();

        assertSame(selected, state.selectedReport().orElseThrow());
        assertTrue(state.startReviewEnabled());
        assertEquals("Review could not be started because reports are unavailable. "
                + "Please try again.", state.feedbackMessage().orElseThrow());
        assertEquals(0, repository.replacements);
    }

    @Test
    void startReviewStorageFailuresRetainRetryableSelectionWithoutSuccess() {
        for (ReportStoreException.Reason reason : ReportStoreException.Reason.values()) {
            if (reason == ReportStoreException.Reason.REPLACEMENT_TARGET_NOT_FOUND) {
                continue;
            }
            ItemReport selected = report(1, ReportType.FOUND, ReportStatus.SUBMITTED);
            InMemoryRepository repository = new InMemoryRepository(List.of(selected));
            DeskOfficerReviewService service = new DeskOfficerReviewService(repository);
            service.enter();
            service.select(selected.reportId());
            repository.failNextReplace(reason);

            ReviewQueueState state = service.startReview();

            assertEquals("Review could not be started because reports are unavailable. "
                    + "Please try again.", state.feedbackMessage().orElseThrow());
            assertSame(selected, state.selectedReport().orElseThrow());
            assertTrue(state.startReviewEnabled());
            assertEquals(List.of(selected), repository.reports());
        }
    }

    @Test
    void selectedStateExposesCanonicalReportForCompleteRendering() {
        int sequence = 1;
        for (ItemCategory category : ItemCategory.values()) {
            ItemReport report = ItemReport.restore(
                    new UUID(0L, sequence++),
                    "synthetic-reporter",
                    sequence % 2 == 0 ? ReportType.LOST : ReportType.FOUND,
                    "Synthetic item",
                    category,
                    "Synthetic location",
                    LocalDate.of(2026, 9, 20),
                    "Synthetic public description",
                    "Synthetic private detail",
                    ReportStatus.SUBMITTED,
                    Instant.parse("2026-09-20T01:02:03.456Z"));
            DeskOfficerReviewService service = new DeskOfficerReviewService(
                    new InMemoryRepository(List.of(report)));
            service.enter();

            assertSame(report, service.select(report.reportId())
                    .selectedReport().orElseThrow());
        }
    }

    private static ItemReport report(int sequence, ReportType type, ReportStatus status) {
        return ItemReport.restore(
                new UUID(0L, sequence),
                "synthetic-reporter-" + sequence,
                type,
                "Synthetic item " + sequence,
                ItemCategory.OTHER,
                "Synthetic location " + sequence,
                LocalDate.of(2026, 9, 10 + sequence),
                "Synthetic public description " + sequence,
                "Synthetic private detail " + sequence,
                status,
                Instant.parse("2026-09-20T01:02:03.456Z"));
    }

    private static final class InMemoryRepository implements ReportRepository {
        private final List<ItemReport> reports;

        private ReportStoreException.Reason nextLoadFailure;

        private ReportStoreException.Reason nextReplaceFailure;

        private boolean failLoadAfterReplacement;

        private int loads;

        private int replacements;

        private InMemoryRepository(List<ItemReport> initialReports) {
            reports = new ArrayList<>(initialReports);
        }

        @Override
        public List<ItemReport> loadAll() throws ReportStoreException {
            loads++;
            if (nextLoadFailure != null) {
                ReportStoreException.Reason reason = nextLoadFailure;
                nextLoadFailure = null;
                throw new ReportStoreException(reason);
            }
            return List.copyOf(reports);
        }

        @Override
        public void insert(ItemReport report) {
            reports.add(report);
        }

        @Override
        public void replace(UUID targetId, ItemReport replacement)
                throws ReportStoreException {
            if (nextReplaceFailure != null) {
                ReportStoreException.Reason reason = nextReplaceFailure;
                nextReplaceFailure = null;
                if (failLoadAfterReplacement) {
                    failLoadAfterReplacement = false;
                    nextLoadFailure = ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE;
                }
                throw new ReportStoreException(reason);
            }
            for (int index = 0; index < reports.size(); index++) {
                if (reports.get(index).reportId().equals(targetId)) {
                    reports.set(index, replacement);
                    replacements++;
                    return;
                }
            }
            throw new ReportStoreException(
                    ReportStoreException.Reason.REPLACEMENT_TARGET_NOT_FOUND);
        }

        private List<ItemReport> reports() {
            return List.copyOf(reports);
        }

        private void setReports(List<ItemReport> replacementReports) {
            reports.clear();
            reports.addAll(replacementReports);
        }

        private void failNextLoad(ReportStoreException.Reason reason) {
            nextLoadFailure = reason;
        }

        private void failNextReplace(ReportStoreException.Reason reason) {
            nextReplaceFailure = reason;
        }

        private void failReplacementAndNextLoad() {
            nextReplaceFailure = ReportStoreException.Reason.REPLACEMENT_TARGET_NOT_FOUND;
            failLoadAfterReplacement = true;
        }
    }
}
