package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.StudentReportHistoryService;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;

class StudentReportHistoryControllerTest {
    private static final String STUDENT_ID = "student-001";

    @Test
    void mapsNormalAndEmptySearchesToDistinctPresentationStates() {
        StudentReportHistoryController controller = controllerWith(
                List.of(report("Pencil case", "Blue case with a white zipper.")));

        ReportHistoryViewState all = controller.search("");
        ReportHistoryViewState matching = controller.search("white zipper");
        ReportHistoryViewState noMatches = controller.search("umbrella");

        assertEquals(ReportHistoryViewState.Kind.RESULTS, all.kind());
        assertEquals("Submitted", all.reports().getFirst().statusLabel());
        assertEquals(ReportHistoryViewState.Kind.RESULTS, matching.kind());
        assertEquals(ReportHistoryViewState.Kind.NO_MATCHES, noMatches.kind());
    }

    @Test
    void mapsAbsentPersonalHistorySeparately() {
        StudentReportHistoryController controller = controllerWith(List.of());

        ReportHistoryViewState state = controller.search("pencil");

        assertEquals(ReportHistoryViewState.Kind.NO_REPORTS, state.kind());
    }

    @Test
    void mapsStoreFailureToSafePresentationState() {
        ReportRepository repository = new ReadOnlyRepository(
                List.of(),
                new ReportStoreException(
                        ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE));
        StudentReportHistoryController controller = new StudentReportHistoryController(
                STUDENT_ID,
                new StudentReportHistoryService(repository));

        ReportHistoryViewState state = controller.search("");

        assertEquals(ReportHistoryViewState.Kind.LOAD_FAILURE, state.kind());
        assertEquals(
                "We could not load your reports. Please try again or ask the Desk Officer.",
                state.message());
    }

    @Test
    void rejectsMissingIdentityServiceAndQuery() {
        StudentReportHistoryService service = new StudentReportHistoryService(
                new ReadOnlyRepository(List.of(), null));

        assertThrows(NullPointerException.class,
                () -> new StudentReportHistoryController(null, service));
        assertThrows(IllegalArgumentException.class,
                () -> new StudentReportHistoryController("   ", service));
        assertThrows(NullPointerException.class,
                () -> new StudentReportHistoryController(STUDENT_ID, null));

        StudentReportHistoryController controller = new StudentReportHistoryController(
                "  " + STUDENT_ID + "  ", service);
        assertThrows(NullPointerException.class,
                () -> controller.search(null));
    }

    private static StudentReportHistoryController controllerWith(
            List<ItemReport> reports) {
        return new StudentReportHistoryController(
                STUDENT_ID,
                new StudentReportHistoryService(
                        new ReadOnlyRepository(reports, null)));
    }

    private static ItemReport report(String name, String publicDescription) {
        return ItemReport.restore(
                UUID.fromString("00000000-0000-0000-0000-000000000401"),
                STUDENT_ID,
                ReportType.LOST,
                name,
                ItemCategory.STATIONERY,
                "Library",
                LocalDate.of(2026, 9, 18),
                publicDescription,
                "Synthetic private detail",
                ReportStatus.SUBMITTED,
                Instant.parse("2026-09-20T08:00:00.000Z"));
    }

    private static final class ReadOnlyRepository implements ReportRepository {
        private final List<ItemReport> reports;

        private final ReportStoreException failure;

        private ReadOnlyRepository(List<ItemReport> storedReports,
                ReportStoreException loadFailure) {
            reports = List.copyOf(storedReports);
            failure = loadFailure;
        }

        @Override
        public List<ItemReport> loadAll() throws ReportStoreException {
            if (failure != null) {
                throw failure;
            }
            return reports;
        }

        @Override
        public void insert(ItemReport report) {
            throw new UnsupportedOperationException("Not needed by controller test");
        }

        @Override
        public void replace(UUID targetId, ItemReport replacement) {
            throw new UnsupportedOperationException("Not needed by controller test");
        }
    }
}
