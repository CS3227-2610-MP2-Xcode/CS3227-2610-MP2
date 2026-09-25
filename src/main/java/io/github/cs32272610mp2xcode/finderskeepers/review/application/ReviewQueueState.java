package io.github.cs32272610mp2xcode.finderskeepers.review.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;

/**
 * Immutable presentation state for one Desk Officer report-review view.
 *
 * @param available whether authoritative report and Claim data is available
 * @param activeFilter current report-type filter
 * @param visibleReports active reports matching the filter
 * @param selectedReport selected visible canonical report, when present
 * @param queueMessage empty-state copy, when applicable
 * @param detailsMessage neutral or unavailable detail-panel copy
 * @param retryVisible whether an explicit load retry is available
 */
public record ReviewQueueState(
        boolean available,
        ReviewQueueFilter activeFilter,
        List<ItemReport> visibleReports,
        Optional<ItemReport> selectedReport,
        Optional<String> queueMessage,
        String detailsMessage,
        boolean retryVisible) {
    /** Makes all collection and optional components immutable and non-null. */
    public ReviewQueueState {
        Objects.requireNonNull(activeFilter, "activeFilter");
        visibleReports = List.copyOf(Objects.requireNonNull(
                visibleReports, "visibleReports"));
        Objects.requireNonNull(selectedReport, "selectedReport");
        Objects.requireNonNull(queueMessage, "queueMessage");
        Objects.requireNonNull(detailsMessage, "detailsMessage");
    }
}
