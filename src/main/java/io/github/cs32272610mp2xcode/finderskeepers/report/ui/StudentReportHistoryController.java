package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportHistoryException;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportHistorySearchResult;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.StudentReportHistoryService;

/** Binds personal report-history searches to one authenticated Student. */
public final class StudentReportHistoryController {
    private final String reporterId;

    private final StudentReportHistoryService historyService;

    /**
     * Creates a controller for one authenticated Student.
     *
     * @param authenticatedReporterId stable authenticated-user identifier
     * @param reportHistoryService personal history use case
     */
    public StudentReportHistoryController(String authenticatedReporterId,
            StudentReportHistoryService reportHistoryService) {
        Objects.requireNonNull(authenticatedReporterId, "authenticatedReporterId");
        reporterId = authenticatedReporterId.strip();
        if (reporterId.isEmpty()) {
            throw new IllegalArgumentException(
                    "authenticatedReporterId must not be blank");
        }
        historyService = Objects.requireNonNull(
                reportHistoryService, "reportHistoryService");
    }

    /**
     * Reloads personal reports and applies an item-name/public-description query.
     *
     * @param query current search text; blank text displays every personal report
     * @return presentation-ready results, empty state, or safe failure
     */
    public ReportHistoryViewState search(String query) {
        Objects.requireNonNull(query, "query");
        try {
            ReportHistorySearchResult result = historyService.search(reporterId, query);
            if (!result.hasPersonalReports()) {
                return ReportHistoryViewState.noReports();
            }
            if (result.matches().isEmpty()) {
                return ReportHistoryViewState.noMatches();
            }
            return ReportHistoryViewState.results(result.matches());
        } catch (ReportHistoryException exception) {
            return ReportHistoryViewState.loadFailure();
        }
    }
}
