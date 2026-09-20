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
    void loadedReportsAreAnUnmodifiableStructuralSnapshot() throws ReportStoreException {
        Path store = temporaryDirectory.resolve("reports.json");
        ReportRepository repository = new JsonReportRepository(store);
        repository.insert(report(1, ReportStatus.SUBMITTED));
        List<ItemReport> snapshot = repository.loadAll();

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.add(report(2, ReportStatus.SUBMITTED)),
                "The loaded report list must be structurally unmodifiable");
        repository.insert(report(2, ReportStatus.SUBMITTED));
        assertTrue(snapshot.size() == 1, "A later mutation must not change an earlier snapshot");
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

    @Test
    void insertionAndReplacementPreserveStableOrderAcrossReconstruction() throws ReportStoreException {
        Path store = temporaryDirectory.resolve("reports.json");
        ReportRepository repository = new JsonReportRepository(store);
        ItemReport first = report(1, ReportStatus.SUBMITTED);
        ItemReport originalMiddle = report(2, ReportStatus.SUBMITTED);
        ItemReport last = report(3, ReportStatus.SUBMITTED);
        repository.insert(first);
        repository.insert(originalMiddle);
        repository.insert(last);
        ItemReport replacement = replacementFor(originalMiddle);

        repository.replace(originalMiddle.reportId(), replacement);
        List<ItemReport> loaded = new JsonReportRepository(store).loadAll();

        assertTrue(loaded.size() == 3, "Replacement must preserve the report count");
        assertTrue(loaded.get(0).equals(first), "The first report must retain its position and state");
        assertTrue(loaded.get(1).equals(replacement), "The replacement must occupy the target position");
        assertTrue(loaded.get(2).equals(last), "The last report must retain its position and state");
    }

    @Test
    void missingReplacementReturnsMissingConflictWithoutCreatingOrChangingStorage()
            throws ReportStoreException, IOException {
        Path missingStore = temporaryDirectory.resolve("missing").resolve("reports.json");
        ReportRepository missingRepository = new JsonReportRepository(missingStore);
        ItemReport missing = report(9, ReportStatus.SUBMITTED);

        ReportStoreException absentFailure = assertThrows(
                ReportStoreException.class,
                () -> missingRepository.replace(missing.reportId(), missing),
                "A missing replacement target must be rejected");
        assertTrue(absentFailure.reason() == ReportStoreException.Reason.REPLACEMENT_TARGET_NOT_FOUND,
                "The missing target must expose its typed reason");
        assertFalse(Files.exists(missingStore), "A missing replacement must not create storage");

        Path existingStore = temporaryDirectory.resolve("reports.json");
        ReportRepository existingRepository = new JsonReportRepository(existingStore);
        existingRepository.insert(report(1, ReportStatus.SUBMITTED));
        byte[] before = Files.readAllBytes(existingStore);
        ReportStoreException existingFailure = assertThrows(
                ReportStoreException.class,
                () -> existingRepository.replace(missing.reportId(), missing),
                "An absent target in a valid store must be rejected");
        assertTrue(existingFailure.reason() == ReportStoreException.Reason.REPLACEMENT_TARGET_NOT_FOUND,
                "The missing target reason must be stable");
        assertTrue(Arrays.equals(before, Files.readAllBytes(existingStore)),
                "A missing replacement must preserve existing bytes");
    }

    @Test
    void immutableMismatchOrRetargetAttemptFailsWithoutMutation() throws ReportStoreException, IOException {
        Path store = temporaryDirectory.resolve("reports.json");
        ReportRepository repository = new JsonReportRepository(store);
        ItemReport original = report(1, ReportStatus.SUBMITTED);
        repository.insert(original);
        byte[] before = Files.readAllBytes(store);

        List<ItemReport> invalidReplacements = List.of(
                new ItemReport(
                        new UUID(0L, 2L), original.reporterId(), original.reportType(), original.itemName(),
                        original.category(), original.location(), original.occurrenceDate(),
                        original.publicDescription(), original.privateIdentifyingDetail(), original.status(),
                        original.createdAt()),
                new ItemReport(
                        original.reportId(), "different-reporter", original.reportType(), original.itemName(),
                        original.category(), original.location(), original.occurrenceDate(),
                        original.publicDescription(), original.privateIdentifyingDetail(), original.status(),
                        original.createdAt()),
                new ItemReport(
                        original.reportId(), original.reporterId(), original.reportType(), original.itemName(),
                        original.category(), original.location(), original.occurrenceDate(),
                        original.publicDescription(), original.privateIdentifyingDetail(), original.status(),
                        original.createdAt().plusMillis(1)));

        for (ItemReport invalid : invalidReplacements) {
            ReportStoreException failure = assertThrows(
                    ReportStoreException.class,
                    () -> repository.replace(original.reportId(), invalid),
                    "An immutable-field mismatch must be rejected");
            assertTrue(failure.reason() == ReportStoreException.Reason.IMMUTABLE_FIELD_MISMATCH,
                    "An immutable mismatch must expose its typed reason");
            assertTrue(Arrays.equals(before, Files.readAllBytes(store)),
                    "An immutable mismatch must preserve existing bytes");
        }
    }

    @Test
    void nullArgumentsFailBeforeFilesystemAccess() {
        Path store = temporaryDirectory.resolve("missing").resolve("reports.json");
        ReportRepository repository = new JsonReportRepository(store);
        ItemReport report = report(1, ReportStatus.SUBMITTED);

        assertThrows(NullPointerException.class, () -> repository.insert(null),
                "A null insertion must be programmer error");
        assertThrows(NullPointerException.class, () -> repository.replace(null, report),
                "A null replacement target must be programmer error");
        assertThrows(NullPointerException.class, () -> repository.replace(report.reportId(), null),
                "A null replacement report must be programmer error");
        assertThrows(NullPointerException.class, () -> new JsonReportRepository(null),
                "A null repository path must be programmer error");
        assertFalse(Files.exists(store), "Null arguments must fail before filesystem access");
        assertFalse(Files.exists(store.getParent()), "Null arguments must not create the missing parent");
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

    private static ItemReport replacementFor(ItemReport original) {
        return new ItemReport(
                original.reportId(),
                original.reporterId(),
                original.reportType() == ReportType.LOST ? ReportType.FOUND : ReportType.LOST,
                "Replacement item",
                ItemCategory.OTHER,
                "Replacement location",
                LocalDate.of(2026, 9, 1),
                "Replacement public description",
                "Replacement private detail",
                ReportStatus.UNDER_REVIEW,
                original.createdAt());
    }
}
