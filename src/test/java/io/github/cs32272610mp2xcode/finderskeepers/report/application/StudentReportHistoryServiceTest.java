package io.github.cs32272610mp2xcode.finderskeepers.report.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;

class StudentReportHistoryServiceTest {
    private static final String STUDENT_ID = "student-001";

    private static final Instant EARLIER = Instant.parse("2026-09-19T08:00:00.000Z");

    private static final Instant LATER = Instant.parse("2026-09-20T09:30:00.000Z");

    @Test
    void blankSearchReturnsOnlyPersonalReportsNewestFirst() {
        ItemReport earlier = report(
                "00000000-0000-0000-0000-000000000001",
                STUDENT_ID,
                "Blue pencil case",
                "Blue case with a white zipper.",
                ReportStatus.SUBMITTED,
                EARLIER,
                "Synthetic star sticker");
        ItemReport later = report(
                "00000000-0000-0000-0000-000000000002",
                STUDENT_ID,
                "Green water bottle",
                "Green bottle with a carrying loop.",
                ReportStatus.UNDER_REVIEW,
                LATER,
                "Synthetic initials under the base");
        ItemReport anotherStudent = report(
                "00000000-0000-0000-0000-000000000003",
                "student-002",
                "Red umbrella",
                "Small red umbrella.",
                ReportStatus.SUBMITTED,
                LATER,
                "Synthetic yellow name tag");
        StudentReportHistoryService service = serviceWith(
                List.of(earlier, anotherStudent, later));

        ReportHistorySearchResult result = service.search(STUDENT_ID, "   ");

        assertTrue(result.hasPersonalReports());
        assertEquals(List.of(later.reportId(), earlier.reportId()),
                result.matches().stream().map(ReportHistoryEntry::reportId).toList());
        assertEquals("Under review", result.matches().getFirst().statusLabel());
        assertFalse(result.matches().getFirst().toString().contains("Synthetic initials"));
    }

    @Test
    void itemNameSearchIsTrimmedCaseInsensitiveAndUsesSubstringMatching() {
        ItemReport matching = report(
                "00000000-0000-0000-0000-000000000010",
                STUDENT_ID,
                "Blue Pencil Case",
                "Found beside the library shelf.",
                ReportStatus.SUBMITTED,
                EARLIER,
                "Synthetic private detail");
        ItemReport notMatching = report(
                "00000000-0000-0000-0000-000000000011",
                STUDENT_ID,
                "Water bottle",
                "Green bottle.",
                ReportStatus.SUBMITTED,
                LATER,
                "Synthetic private detail");

        ReportHistorySearchResult result = serviceWith(List.of(matching, notMatching))
                .search(STUDENT_ID, "  PENCIL  ");

        assertEquals(List.of(matching.reportId()),
                result.matches().stream().map(ReportHistoryEntry::reportId).toList());
    }

    @Test
    void publicDescriptionSearchMatchesWithoutSearchingPrivateDetail() {
        ItemReport report = report(
                "00000000-0000-0000-0000-000000000020",
                STUDENT_ID,
                "Pencil case",
                "Blue case with a WHITE ZIPPER.",
                ReportStatus.SUBMITTED,
                EARLIER,
                "Synthetic dragon sticker");
        StudentReportHistoryService service = serviceWith(List.of(report));

        ReportHistorySearchResult publicMatch = service.search(STUDENT_ID, "white zipper");
        ReportHistorySearchResult privateMiss = service.search(STUDENT_ID, "dragon sticker");

        assertEquals(1, publicMatch.matches().size());
        assertTrue(privateMiss.hasPersonalReports());
        assertTrue(privateMiss.matches().isEmpty());
    }

    @Test
    void noPersonalReportsIsDifferentFromNoSearchMatches() {
        ItemReport report = report(
                "00000000-0000-0000-0000-000000000030",
                STUDENT_ID,
                "Notebook",
                "Green notebook.",
                ReportStatus.SUBMITTED,
                EARLIER,
                "Synthetic private detail");
        StudentReportHistoryService service = serviceWith(List.of(report));

        ReportHistorySearchResult noMatch = service.search(STUDENT_ID, "umbrella");
        ReportHistorySearchResult noReports = service.search("student-999", "umbrella");

        assertTrue(noMatch.hasPersonalReports());
        assertTrue(noMatch.matches().isEmpty());
        assertFalse(noReports.hasPersonalReports());
        assertTrue(noReports.matches().isEmpty());
    }

    @Test
    void equalTimestampsUseReportIdAsDeterministicTieBreaker() {
        ItemReport second = report(
                "00000000-0000-0000-0000-000000000102",
                STUDENT_ID,
                "Second ID",
                "Synthetic description.",
                ReportStatus.SUBMITTED,
                LATER,
                "Synthetic private detail");
        ItemReport first = report(
                "00000000-0000-0000-0000-000000000101",
                STUDENT_ID,
                "First ID",
                "Synthetic description.",
                ReportStatus.SUBMITTED,
                LATER,
                "Synthetic private detail");

        ReportHistorySearchResult result = serviceWith(List.of(second, first))
                .search(STUDENT_ID, "");

        assertEquals(List.of(first.reportId(), second.reportId()),
                result.matches().stream().map(ReportHistoryEntry::reportId).toList());
    }

    @Test
    void storeFailureIsTranslatedAndRetainsCause() {
        ReportStoreException storeFailure = new ReportStoreException(
                ReportStoreException.Reason.CORRUPT_OR_UNSUPPORTED_STORE);
        StudentReportHistoryService service = new StudentReportHistoryService(
                new StubRepository(List.of(), storeFailure));

        ReportHistoryException exception = assertThrows(
                ReportHistoryException.class,
                () -> service.search(STUDENT_ID, ""));

        assertSame(storeFailure, exception.getCause());
    }

    @Test
    void rejectsMissingInputsAndProtectsSearchResults() {
        StudentReportHistoryService service = serviceWith(List.of(report(
                "00000000-0000-0000-0000-000000000200",
                STUDENT_ID,
                "Book",
                "Blue workbook.",
                ReportStatus.SUBMITTED,
                EARLIER,
                "Synthetic private detail")));

        assertThrows(NullPointerException.class,
                () -> new StudentReportHistoryService(null));
        assertThrows(NullPointerException.class,
                () -> service.search(null, ""));
        assertThrows(IllegalArgumentException.class,
                () -> service.search("   ", ""));
        assertThrows(NullPointerException.class,
                () -> service.search(STUDENT_ID, null));

        ReportHistorySearchResult result = service.search(STUDENT_ID, "");
        assertThrows(UnsupportedOperationException.class,
                () -> result.matches().clear());
        assertThrows(IllegalArgumentException.class,
                () -> new ReportHistorySearchResult(false, result.matches()));
    }

    private static StudentReportHistoryService serviceWith(List<ItemReport> reports) {
        return new StudentReportHistoryService(new StubRepository(reports, null));
    }

    private static ItemReport report(String id, String reporterId,
            String itemName, String publicDescription, ReportStatus status,
            Instant createdAt, String privateDetail) {
        return ItemReport.restore(
                UUID.fromString(id),
                reporterId,
                ReportType.LOST,
                itemName,
                ItemCategory.STATIONERY,
                "Library",
                LocalDate.of(2026, 9, 18),
                publicDescription,
                privateDetail,
                status,
                createdAt);
    }

    private static final class StubRepository implements ReportRepository {
        private final List<ItemReport> reports;

        private final ReportStoreException loadFailure;

        private StubRepository(List<ItemReport> storedReports,
                ReportStoreException failure) {
            reports = new ArrayList<>(storedReports);
            loadFailure = failure;
        }

        @Override
        public List<ItemReport> loadAll() throws ReportStoreException {
            if (loadFailure != null) {
                throw loadFailure;
            }
            return List.copyOf(reports);
        }

        @Override
        public void insert(ItemReport report) {
            throw new UnsupportedOperationException("Not needed by history test");
        }

        @Override
        public void replace(UUID targetId, ItemReport replacement) {
            throw new UnsupportedOperationException("Not needed by history test");
        }
    }
}
