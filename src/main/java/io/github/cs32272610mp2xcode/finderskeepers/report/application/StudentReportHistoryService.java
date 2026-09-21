package io.github.cs32272610mp2xcode.finderskeepers.report.application;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;

/** Loads and searches the reports owned by one authenticated Student. */
public final class StudentReportHistoryService {
    private static final Comparator<ReportHistoryEntry> NEWEST_FIRST =
            Comparator.comparing(ReportHistoryEntry::createdAt)
                    .reversed()
                    .thenComparing(entry -> entry.reportId().toString());

    private final ReportRepository repository;

    /**
     * Creates the personal report-history use case.
     *
     * @param reportRepository shared canonical report repository
     */
    public StudentReportHistoryService(ReportRepository reportRepository) {
        repository = Objects.requireNonNull(reportRepository, "reportRepository");
    }

    /**
     * Loads and searches reports belonging to one exact authenticated identity.
     *
     * <p>The query is stripped, matched case-insensitively with
     * {@link Locale#ROOT}, and applied as a substring to item names and public
     * descriptions. A blank query returns all personal reports.</p>
     *
     * @param reporterId exact authenticated reporter identifier
     * @param query item-name or public-description query
     * @return personal-report presence and newest-first matching summaries
     * @throws ReportHistoryException when storage cannot be loaded safely
     */
    public ReportHistorySearchResult search(String reporterId, String query) {
        String authenticatedReporterId = requireReporterId(reporterId);
        String normalizedQuery = normalizeQuery(query);
        List<ItemReport> personalReports;
        try {
            personalReports = repository.loadAll().stream()
                    .filter(report -> report.reporterId().equals(authenticatedReporterId))
                    .toList();
        } catch (ReportStoreException exception) {
            throw new ReportHistoryException(exception);
        }

        List<ReportHistoryEntry> matches = personalReports.stream()
                .filter(report -> matches(report, normalizedQuery))
                .map(ReportHistoryEntry::from)
                .sorted(NEWEST_FIRST)
                .toList();
        return new ReportHistorySearchResult(!personalReports.isEmpty(), matches);
    }

    private static String requireReporterId(String reporterId) {
        Objects.requireNonNull(reporterId, "reporterId");
        String stripped = reporterId.strip();
        if (stripped.isEmpty()) {
            throw new IllegalArgumentException("reporterId must not be blank");
        }
        return stripped;
    }

    private static String normalizeQuery(String query) {
        Objects.requireNonNull(query, "query");
        return query.strip().toLowerCase(Locale.ROOT);
    }

    private static boolean matches(ItemReport report, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        return report.itemName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || report.publicDescription().toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }
}
