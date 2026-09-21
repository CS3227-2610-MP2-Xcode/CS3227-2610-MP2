package io.github.cs32272610mp2xcode.finderskeepers.report.application;

import java.util.List;

/**
 * Result of searching one Student's reports.
 *
 * @param hasPersonalReports whether the Student owns any report before search filtering
 * @param matches privacy-safe reports matching the query
 */
public record ReportHistorySearchResult(
        boolean hasPersonalReports,
        List<ReportHistoryEntry> matches) {
    /** Protects the result from later mutation and enforces its count invariant. */
    public ReportHistorySearchResult {
        matches = List.copyOf(matches);
        if (!hasPersonalReports && !matches.isEmpty()) {
            throw new IllegalArgumentException(
                    "Search matches require at least one personal report");
        }
    }
}
