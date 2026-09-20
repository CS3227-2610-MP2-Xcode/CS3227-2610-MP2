package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import java.util.List;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportHistoryEntry;

/**
 * Presentation-ready personal report-history state.
 *
 * @param kind result, empty, or failure state
 * @param message safe user-facing state summary
 * @param reports privacy-safe matching report rows
 */
public record ReportHistoryViewState(
        Kind kind,
        String message,
        List<ReportHistoryEntry> reports) {
    /** Validates the state and protects its report rows from later mutation. */
    public ReportHistoryViewState {
        kind = Objects.requireNonNull(kind, "kind");
        message = Objects.requireNonNull(message, "message");
        reports = List.copyOf(reports);
        if (kind == Kind.RESULTS && reports.isEmpty()) {
            throw new IllegalArgumentException("Results state requires at least one report");
        }
        if (kind != Kind.RESULTS && !reports.isEmpty()) {
            throw new IllegalArgumentException("Only results state can contain reports");
        }
    }

    /**
     * Creates a state containing matching reports.
     *
     * @param matches nonempty privacy-safe result rows
     * @return results state
     */
    public static ReportHistoryViewState results(List<ReportHistoryEntry> matches) {
        int count = matches.size();
        String noun = count == 1 ? "report" : "reports";
        return new ReportHistoryViewState(
                Kind.RESULTS,
                "Showing " + count + " " + noun + ".",
                matches);
    }

    /**
     * Creates the state for a Student who has not submitted a report.
     *
     * @return empty personal-history state
     */
    public static ReportHistoryViewState noReports() {
        return new ReportHistoryViewState(
                Kind.NO_REPORTS,
                "You have not submitted any reports yet.",
                List.of());
    }

    /**
     * Creates the state for a query with no matching personal report.
     *
     * @return empty search-results state
     */
    public static ReportHistoryViewState noMatches() {
        return new ReportHistoryViewState(
                Kind.NO_MATCHES,
                "No reports match your search. Try another word or clear the search.",
                List.of());
    }

    /**
     * Creates a safe report-store failure state.
     *
     * @return storage failure state
     */
    public static ReportHistoryViewState loadFailure() {
        return new ReportHistoryViewState(
                Kind.LOAD_FAILURE,
                "We could not load your reports. Please try again or ask the Desk Officer.",
                List.of());
    }

    /** Distinct presentation outcomes for personal report history. */
    public enum Kind {
        /** At least one personal report matches the current query. */
        RESULTS,

        /** The authenticated Student owns no reports. */
        NO_REPORTS,

        /** The Student owns reports but none match the current query. */
        NO_MATCHES,

        /** The report store could not be read safely. */
        LOAD_FAILURE
    }
}
