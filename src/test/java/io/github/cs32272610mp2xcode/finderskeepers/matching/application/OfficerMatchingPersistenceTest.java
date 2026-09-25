package io.github.cs32272610mp2xcode.finderskeepers.matching.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.Feedback;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.Section;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.DeterministicMatcher;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.FilePossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;

class OfficerMatchingPersistenceTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void linkAndUnlinkSurviveFreshInstancesWithoutChangingReports() throws Exception {
        Path reportStore = temporaryDirectory.resolve("reports.json");
        Path linkStore = temporaryDirectory.resolve("possible-match-links.txt");
        ReportRepository reports = new JsonReportRepository(reportStore);
        ItemReport lost = report(1, ReportType.LOST);
        ItemReport found = report(2, ReportType.FOUND);
        reports.insert(lost);
        reports.insert(found);
        byte[] reportBytes = Files.readAllBytes(reportStore);

        OfficerMatchingService first = service(reportStore, linkStore);
        first.enter();
        first.select(Section.SUGGESTIONS, lost.reportId(), found.reportId());
        assertEquals(Feedback.LINKED,
                first.link(lost.reportId(), found.reportId()).feedback().orElseThrow());

        OfficerMatchingService second = service(reportStore, linkStore);
        assertEquals(1, second.enter().linkedPairs().size());
        assertEquals(List.of(lost, found), new JsonReportRepository(reportStore).loadAll());
        assertArrayEquals(reportBytes, Files.readAllBytes(reportStore));

        second.select(Section.LINKED, lost.reportId(), found.reportId());
        assertEquals(Feedback.UNLINKED,
                second.unlink(lost.reportId(), found.reportId()).feedback().orElseThrow());

        MatchingWorkspaceState reconstructed = service(reportStore, linkStore).enter();
        assertTrue(reconstructed.linkedPairs().isEmpty());
        assertEquals(1, reconstructed.suggestions().size());
        assertArrayEquals(reportBytes, Files.readAllBytes(reportStore));
    }

    private static OfficerMatchingService service(Path reportStore, Path linkStore) {
        return new OfficerMatchingService(new JsonReportRepository(reportStore),
                new FilePossibleMatchRepository(linkStore), new DeterministicMatcher());
    }

    private static ItemReport report(int sequence, ReportType type) {
        return ItemReport.restore(new UUID(0L, sequence),
                "synthetic-reporter-" + sequence, type, "Blue bottle",
                ItemCategory.WATER_BOTTLES, "School library",
                LocalDate.of(2026, 9, sequence),
                "Synthetic public description", "Synthetic private detail",
                ReportStatus.SUBMITTED, Instant.parse("2026-09-20T01:02:03.456Z"));
    }
}
