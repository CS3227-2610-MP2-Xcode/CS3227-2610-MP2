package io.github.cs32272610mp2xcode.finderskeepers.review.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;

/** Stateful application service for one authenticated Desk Officer review view. */
public final class DeskOfficerReviewService {
    private static final String NO_SELECTION_MESSAGE =
            "Select a report to view details.";

    private static final String LOAD_FAILURE_MESSAGE =
            "Reports are unavailable. Please try again.";

    private static final String REVIEW_FAILURE_MESSAGE =
            "Review could not be started because reports are unavailable. Please try again.";

    private static final String REVIEW_STARTED_MESSAGE = "Review started.";

    private static final String STALE_REPORT_MESSAGE =
            "This report is no longer available for review.";

    private final ReportRepository repository;

    private List<ItemReport> submittedReports = List.of();

    private ReviewQueueFilter activeFilter = ReviewQueueFilter.ALL;

    private UUID selectedReportId;

    private boolean available;

    private String feedbackMessage;

    private boolean restoreStaleFeedbackAfterRetry;

    /**
     * Creates a review service over the shared report repository.
     *
     * @param reportRepository canonical report persistence boundary
     */
    public DeskOfficerReviewService(ReportRepository reportRepository) {
        repository = Objects.requireNonNull(reportRepository, "reportRepository");
    }

    /**
     * Enters a fresh Desk Officer view and loads the default All queue.
     *
     * @return current immutable presentation state
     */
    public ReviewQueueState enter() {
        activeFilter = ReviewQueueFilter.ALL;
        selectedReportId = null;
        feedbackMessage = null;
        restoreStaleFeedbackAfterRetry = false;
        return loadForEntryOrRetry(false);
    }

    /**
     * Explicitly retries a previously failed queue load.
     *
     * @return current immutable presentation state
     */
    public ReviewQueueState retry() {
        if (available) {
            return state();
        }
        return loadForEntryOrRetry(restoreStaleFeedbackAfterRetry);
    }

    /**
     * Applies one of the exact in-memory queue filters.
     *
     * @param filter filter to activate
     * @return current immutable presentation state
     */
    public ReviewQueueState changeFilter(ReviewQueueFilter filter) {
        Objects.requireNonNull(filter, "filter");
        if (!available) {
            return state();
        }
        activeFilter = filter;
        feedbackMessage = null;
        if (selectedReportId != null && findVisible(selectedReportId).isEmpty()) {
            selectedReportId = null;
        }
        return state();
    }

    /**
     * Selects one currently visible report by canonical identifier.
     *
     * @param reportId visible report identifier
     * @return current immutable presentation state
     */
    public ReviewQueueState select(UUID reportId) {
        Objects.requireNonNull(reportId, "reportId");
        if (!available) {
            return state();
        }
        selectedReportId = findVisible(reportId)
                .map(ItemReport::reportId)
                .orElse(null);
        feedbackMessage = null;
        return state();
    }

    /**
     * Starts review of the selected submitted report after an authoritative recheck.
     *
     * @return current immutable presentation state
     */
    public ReviewQueueState startReview() {
        if (!available || selectedReportId == null) {
            return state();
        }

        UUID targetId = selectedReportId;
        List<ItemReport> authoritative;
        try {
            authoritative = repository.loadAll();
        } catch (ReportStoreException exception) {
            feedbackMessage = REVIEW_FAILURE_MESSAGE;
            return state();
        }

        Optional<ItemReport> target = findById(authoritative, targetId);
        if (target.isEmpty() || target.orElseThrow().status() != ReportStatus.SUBMITTED) {
            return reconcileStale(authoritative);
        }

        try {
            repository.replace(targetId,
                    target.orElseThrow().withStatus(ReportStatus.UNDER_REVIEW));
        } catch (ReportStoreException exception) {
            if (exception.reason()
                    == ReportStoreException.Reason.REPLACEMENT_TARGET_NOT_FOUND) {
                return reconcileAfterMissingReplacement();
            }
            feedbackMessage = REVIEW_FAILURE_MESSAGE;
            return state();
        }

        submittedReports = submittedOnly(authoritative).stream()
                .filter(report -> !report.reportId().equals(targetId))
                .toList();
        selectedReportId = null;
        feedbackMessage = REVIEW_STARTED_MESSAGE;
        restoreStaleFeedbackAfterRetry = false;
        return state();
    }

    private ReviewQueueState loadForEntryOrRetry(boolean restoreStaleFeedback) {
        try {
            submittedReports = submittedOnly(repository.loadAll());
            selectedReportId = null;
            available = true;
            feedbackMessage = restoreStaleFeedback ? STALE_REPORT_MESSAGE : null;
            restoreStaleFeedbackAfterRetry = false;
        } catch (ReportStoreException exception) {
            submittedReports = List.of();
            selectedReportId = null;
            available = false;
            feedbackMessage = null;
            restoreStaleFeedbackAfterRetry = restoreStaleFeedback;
        }
        return state();
    }

    private ReviewQueueState reconcileStale(List<ItemReport> authoritative) {
        submittedReports = submittedOnly(authoritative);
        selectedReportId = null;
        available = true;
        feedbackMessage = STALE_REPORT_MESSAGE;
        restoreStaleFeedbackAfterRetry = false;
        return state();
    }

    private ReviewQueueState reconcileAfterMissingReplacement() {
        try {
            return reconcileStale(repository.loadAll());
        } catch (ReportStoreException exception) {
            submittedReports = List.of();
            selectedReportId = null;
            available = false;
            feedbackMessage = null;
            restoreStaleFeedbackAfterRetry = true;
            return state();
        }
    }

    private ReviewQueueState state() {
        List<ItemReport> visibleReports = visibleReports();
        Optional<ItemReport> selected = selectedReportId == null
                ? Optional.empty()
                : findById(visibleReports, selectedReportId);
        Optional<String> queueMessage = available
                ? emptyMessage(visibleReports)
                : Optional.empty();
        String detailsMessage = available ? NO_SELECTION_MESSAGE : LOAD_FAILURE_MESSAGE;
        if (selected.isPresent()) {
            detailsMessage = "";
        }
        return new ReviewQueueState(
                available,
                activeFilter,
                visibleReports,
                selected,
                queueMessage,
                detailsMessage,
                Optional.ofNullable(feedbackMessage),
                available && selected.isPresent(),
                !available);
    }

    private Optional<String> emptyMessage(List<ItemReport> visibleReports) {
        if (submittedReports.isEmpty()) {
            return Optional.of("No submitted reports.");
        }
        if (visibleReports.isEmpty() && activeFilter != ReviewQueueFilter.ALL) {
            return Optional.of("No submitted reports match the "
                    + activeFilter.displayName() + " filter.");
        }
        return Optional.empty();
    }

    private List<ItemReport> visibleReports() {
        if (!available) {
            return List.of();
        }
        return submittedReports.stream()
                .filter(activeFilter::matches)
                .toList();
    }

    private Optional<ItemReport> findVisible(UUID reportId) {
        return findById(visibleReports(), reportId);
    }

    private static Optional<ItemReport> findById(List<ItemReport> reports, UUID reportId) {
        return reports.stream()
                .filter(report -> report.reportId().equals(reportId))
                .findFirst();
    }

    private static List<ItemReport> submittedOnly(List<ItemReport> reports) {
        return reports.stream()
                .filter(report -> report.status() == ReportStatus.SUBMITTED)
                .toList();
    }
}
