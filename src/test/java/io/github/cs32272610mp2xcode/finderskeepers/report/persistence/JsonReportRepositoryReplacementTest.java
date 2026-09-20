package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonReportRepositoryReplacementTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void replacementPersistsEachRepresentableMutableFieldAndPreservesImmutableState()
            throws ReportStoreException {
        ItemReport original = originalReport();
        List<ItemReport> replacements = List.of(
                copyWithCategory(original, ItemCategory.STATIONERY),
                copy(original, ReportType.FOUND, original.itemName(), original.location(),
                        original.occurrenceDate(), original.publicDescription(),
                        original.privateIdentifyingDetail(), original.status()),
                copy(original, original.reportType(), "changed item", original.location(),
                        original.occurrenceDate(), original.publicDescription(),
                        original.privateIdentifyingDetail(), original.status()),
                copy(original, original.reportType(), original.itemName(), "changed location",
                        original.occurrenceDate(), original.publicDescription(),
                        original.privateIdentifyingDetail(), original.status()),
                copy(original, original.reportType(), original.itemName(), original.location(),
                        LocalDate.of(2026, 9, 1), original.publicDescription(),
                        original.privateIdentifyingDetail(), original.status()),
                copy(original, original.reportType(), original.itemName(), original.location(),
                        original.occurrenceDate(), "changed public description",
                        original.privateIdentifyingDetail(), original.status()),
                copy(original, original.reportType(), original.itemName(), original.location(),
                        original.occurrenceDate(), original.publicDescription(),
                        "changed private detail", original.status()),
                copy(original, original.reportType(), original.itemName(), original.location(),
                        original.occurrenceDate(), original.publicDescription(),
                        original.privateIdentifyingDetail(), ReportStatus.UNDER_REVIEW));

        for (int index = 0; index < replacements.size(); index++) {
            Path store = temporaryDirectory.resolve("mutable-" + index + ".json");
            ReportRepository repository = new JsonReportRepository(store);
            repository.insert(original);
            repository.replace(original.reportId(), replacements.get(index));
            ItemReport loaded = new JsonReportRepository(store).loadAll().getFirst();
            assertTrue(loaded.equals(replacements.get(index)),
                    "Each representable mutable field must persist independently");
            assertTrue(loaded.reportId().equals(original.reportId()),
                    "Replacement must preserve Report ID");
            assertTrue(loaded.reporterId().equals(original.reporterId()),
                    "Replacement must preserve Reporter ID");
            assertTrue(loaded.createdAt().equals(original.createdAt()),
                    "Replacement must preserve Created At");
        }
    }

    @Test
    void identicalReplacementUsesTheValidatedAtomicReplacementPath()
            throws IOException, ReportStoreException {
        Path target = temporaryDirectory.resolve("reports.json");
        ItemReport original = originalReport();
        new JsonReportRepository(target).insert(original);
        byte[] current = Files.readAllBytes(target);
        ReportStoreFiles failingCommit = new ReportStoreFiles() {
            @Override
            public Optional<byte[]> readBounded(Path ignoredTarget, int ignoredMaximum) {
                return Optional.of(current.clone());
            }

            @Override
            public void replaceAtomically(Path ignoredTarget, byte[] ignoredDocument)
                    throws StoreFileFailure {
                throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
            }
        };
        ReportRepository repository = new JsonReportRepository(
                target, failingCommit, JsonReportRepository.MAX_STORE_BYTES);

        ReportStoreException failure = assertThrows(
                ReportStoreException.class,
                () -> repository.replace(original.reportId(), original),
                "An identical replacement must still require a safe commit");

        assertTrue(failure.reason()
                        == ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE,
                "An identical replacement must expose a failed safe-commit outcome");
    }

    private static ItemReport originalReport() {
        return ItemReport.restore(
                UUID.fromString("60000000-0000-0000-0000-000000000001"),
                "reporter-1",
                ReportType.LOST,
                "Item",
                ItemCategory.OTHER,
                "Location",
                LocalDate.of(2026, 9, 19),
                "Public description",
                "Private detail",
                ReportStatus.SUBMITTED,
                Instant.parse("2026-09-20T01:02:03.456Z"));
    }

    private static ItemReport copyWithCategory(ItemReport original, ItemCategory category) {
        return ItemReport.restore(
                original.reportId(),
                original.reporterId(),
                original.reportType(),
                original.itemName(),
                category,
                original.location(),
                original.occurrenceDate(),
                original.publicDescription(),
                original.privateIdentifyingDetail(),
                original.status(),
                original.createdAt());
    }

    private static ItemReport copy(
            ItemReport original,
            ReportType reportType,
            String itemName,
            String location,
            LocalDate occurrenceDate,
            String publicDescription,
            String privateDetail,
            ReportStatus status) {
        return ItemReport.restore(
                original.reportId(),
                original.reporterId(),
                reportType,
                itemName,
                original.category(),
                location,
                occurrenceDate,
                publicDescription,
                privateDetail,
                status,
                original.createdAt());
    }
}
