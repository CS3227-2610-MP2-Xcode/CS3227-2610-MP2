package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportHistoryEntry;

class ReportHistoryViewStateTest {
    @Test
    void resultsAreDefensivelyCopiedAndUseReadableCount() {
        List<ReportHistoryEntry> mutable = new ArrayList<>();
        mutable.add(entry());

        ReportHistoryViewState state = ReportHistoryViewState.results(mutable);
        mutable.clear();

        assertEquals(ReportHistoryViewState.Kind.RESULTS, state.kind());
        assertEquals("Showing 1 report.", state.message());
        assertEquals(1, state.reports().size());
        assertThrows(UnsupportedOperationException.class,
                () -> state.reports().clear());
    }

    @Test
    void factoriesCreateDistinctEmptyAndFailureStates() {
        assertEquals(ReportHistoryViewState.Kind.NO_REPORTS,
                ReportHistoryViewState.noReports().kind());
        assertEquals(ReportHistoryViewState.Kind.NO_MATCHES,
                ReportHistoryViewState.noMatches().kind());
        assertEquals(ReportHistoryViewState.Kind.LOAD_FAILURE,
                ReportHistoryViewState.loadFailure().kind());
    }

    @Test
    void rejectsInconsistentKindsAndRows() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReportHistoryViewState(
                        ReportHistoryViewState.Kind.RESULTS, "Results", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ReportHistoryViewState(
                        ReportHistoryViewState.Kind.NO_MATCHES,
                        "No matches",
                        List.of(entry())));
        assertThrows(NullPointerException.class,
                () -> new ReportHistoryViewState(null, "Message", List.of()));
        assertThrows(NullPointerException.class,
                () -> new ReportHistoryViewState(
                        ReportHistoryViewState.Kind.NO_REPORTS, null, List.of()));
    }

    private static ReportHistoryEntry entry() {
        return new ReportHistoryEntry(
                UUID.fromString("00000000-0000-0000-0000-000000000301"),
                "Found",
                "Pencil case",
                "Stationery",
                LocalDate.of(2026, 9, 18),
                "Blue pencil case.",
                "Submitted",
                Instant.parse("2026-09-20T08:00:00.000Z"));
    }
}
