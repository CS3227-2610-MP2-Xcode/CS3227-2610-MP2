package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.report.model.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.model.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.model.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.model.ReportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonReportRepositoryTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void missingStoreLoadsEmptyWithoutCreatingFilesystemEntries() throws ReportStoreException {
        Path missingParent = temporaryDirectory.resolve("missing-parent");
        Path store = missingParent.resolve("reports.json");
        ReportRepository repository = new JsonReportRepository(store);

        assertTrue(repository.loadAll().isEmpty(), "A missing store must load as empty");
        assertFalse(Files.exists(missingParent), "Loading must not create the parent directory");
        assertFalse(Files.exists(store), "Loading must not create the report store");
    }

    @Test
    void insertedReportRoundTripsAllElevenValuesThroughFreshRepository() throws ReportStoreException {
        Path store = temporaryDirectory.resolve("nested").resolve("reports.json");
        ItemReport expected = report(1, ReportStatus.SUBMITTED);
        ReportRepository writer = new JsonReportRepository(store);

        writer.insert(expected);
        List<ItemReport> loaded = new JsonReportRepository(store).loadAll();

        assertTrue(loaded.size() == 1, "Exactly one inserted report must be reconstructed");
        assertTrue(loaded.getFirst().equals(expected), "Every canonical report value must round-trip exactly");
    }

    @Test
    void duplicateInsertReturnsDuplicateConflictAndPreservesBytes() throws ReportStoreException, IOException {
        Path store = temporaryDirectory.resolve("reports.json");
        ItemReport original = report(1, ReportStatus.SUBMITTED);
        ReportRepository repository = new JsonReportRepository(store);
        repository.insert(original);
        byte[] before = Files.readAllBytes(store);

        ReportStoreException failure = assertThrows(
                ReportStoreException.class,
                () -> repository.insert(report(1, ReportStatus.UNDER_REVIEW)),
                "A duplicate report identifier must be rejected");

        assertTrue(failure.reason() == ReportStoreException.Reason.DUPLICATE_REPORT_ID,
                "The duplicate failure must expose its typed reason");
        assertTrue(Arrays.equals(before, Files.readAllBytes(store)),
                "A duplicate insertion must preserve the store byte-for-byte");
    }

    private static ItemReport report(int sequence, ReportStatus status) {
        return new ItemReport(
                new UUID(0L, sequence),
                "reporter-" + sequence,
                sequence % 2 == 0 ? ReportType.FOUND : ReportType.LOST,
                "Item " + sequence,
                ItemCategory.OTHER,
                "Location " + sequence,
                LocalDate.of(2026, 9, 10 + sequence),
                "Public description " + sequence,
                "Private detail " + sequence,
                status,
                Instant.parse("2026-09-20T01:02:03.456Z"));
    }
}
