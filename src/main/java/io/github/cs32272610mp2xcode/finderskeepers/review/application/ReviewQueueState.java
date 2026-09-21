package io.github.cs32272610mp2xcode.finderskeepers.review.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;

/**
 * Immutable presentation state for one Desk Officer review view.
 *
 * @param available whether the authoritative queue is available
 * @param activeFilter currently selected queue filter
 * @param visibleReports ordered submitted reports matching the active filter
 * @param selectedReport selected visible canonical report, when present
 * @param queueMessage global or filtered empty-state copy, when applicable
 * @param detailsMessage neutral or unavailable details-panel copy
 * @param feedbackMessage success, stale-target, or transition-failure copy
 * @param startReviewEnabled whether the selected report can be reviewed
 * @param retryVisible whether an explicit queue-load retry is available
 */
public record ReviewQueueState(
        boolean available,
        ReviewQueueFilter activeFilter,
        List<ItemReport> visibleReports,
        Optional<ItemReport> selectedReport,
        Optional<String> queueMessage,
        String detailsMessage,
        Optional<String> feedbackMessage,
        boolean startReviewEnabled,
        boolean retryVisible) {
    /** Makes all collection and optional components immutable and non-null. */
    public ReviewQueueState {
        Objects.requireNonNull(activeFilter, "activeFilter");
        visibleReports = List.copyOf(Objects.requireNonNull(
                visibleReports, "visibleReports"));
        Objects.requireNonNull(selectedReport, "selectedReport");
        Objects.requireNonNull(queueMessage, "queueMessage");
        Objects.requireNonNull(detailsMessage, "detailsMessage");
        Objects.requireNonNull(feedbackMessage, "feedbackMessage");
    }
}
