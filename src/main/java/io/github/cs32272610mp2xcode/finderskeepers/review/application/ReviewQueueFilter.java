package io.github.cs32272610mp2xcode.finderskeepers.review.application;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;

/** Non-persisted filters available in the Desk Officer review queue. */
public enum ReviewQueueFilter {
    /** Every submitted report. */
    ALL("All", null),

    /** Submitted lost-item reports. */
    LOST(ReportType.LOST.displayName(), ReportType.LOST),

    /** Submitted found-item reports. */
    FOUND(ReportType.FOUND.displayName(), ReportType.FOUND);

    private final String displayName;

    private final ReportType reportType;

    ReviewQueueFilter(String displayLabel, ReportType matchingType) {
        displayName = displayLabel;
        reportType = matchingType;
    }

    /**
     * Returns the human-facing filter label.
     *
     * @return filter label
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns whether a submitted report belongs in this filter.
     *
     * @param report canonical report to test
     * @return whether the report matches
     */
    public boolean matches(ItemReport report) {
        return reportType == null || report.reportType() == reportType;
    }
}
