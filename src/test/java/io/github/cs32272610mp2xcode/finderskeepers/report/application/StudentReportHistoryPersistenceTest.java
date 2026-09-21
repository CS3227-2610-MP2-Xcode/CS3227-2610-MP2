package io.github.cs32272610mp2xcode.finderskeepers.report.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;

class StudentReportHistoryPersistenceTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void searchesReconstructedCanonicalRepositoryWithoutCrossStudentLeakage()
            throws Exception {
        Path store = temporaryDirectory.resolve("reports.json");
        JsonReportRepository writer = new JsonReportRepository(store);
        ItemReport earlier = report(
                "00000000-0000-0000-0000-000000000501",
                "student-001",
                "Pencil case",
                "Blue case with a white zipper.",
                Instant.parse("2026-09-19T08:00:00.000Z"));
        ItemReport later = report(
                "00000000-0000-0000-0000-000000000502",
                "student-001",
                "Notebook",
                "Green science notebook.",
                Instant.parse("2026-09-20T08:00:00.000Z"));
        ItemReport anotherStudent = report(
                "00000000-0000-0000-0000-000000000503",
                "student-002",
                "White zipper pouch",
                "White pouch.",
                Instant.parse("2026-09-21T08:00:00.000Z"));
        writer.insert(earlier);
        writer.insert(later);
        writer.insert(anotherStudent);

        StudentReportHistoryService service = new StudentReportHistoryService(
                new JsonReportRepository(store));
        ReportHistorySearchResult all = service.search("student-001", "");
        ReportHistorySearchResult descriptionMatch = service.search(
                "student-001", "white zipper");

        assertEquals(List.of(later.reportId(), earlier.reportId()),
                all.matches().stream().map(ReportHistoryEntry::reportId).toList());
        assertEquals(List.of(earlier.reportId()),
                descriptionMatch.matches().stream()
                        .map(ReportHistoryEntry::reportId)
                        .toList());
    }

    private static ItemReport report(String id, String reporterId,
            String itemName, String publicDescription, Instant createdAt) {
        return ItemReport.restore(
                UUID.fromString(id),
                reporterId,
                ReportType.LOST,
                itemName,
                ItemCategory.STATIONERY,
                "Library",
                LocalDate.of(2026, 9, 18),
                publicDescription,
                "Synthetic private detail",
                ReportStatus.SUBMITTED,
                createdAt);
    }
}
