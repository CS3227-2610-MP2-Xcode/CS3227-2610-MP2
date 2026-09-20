package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import io.github.cs32272610mp2xcode.finderskeepers.report.model.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.model.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.model.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.model.ReportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonReportRepositoryStorageTest {
    private static final String ORIGINAL_DOCUMENT = """
            {
              "schemaVersion": 1,
              "reports": [
                {
                  "reportId": "30000000-0000-0000-0000-000000000001",
                  "reporterId": "r",
                  "reportType": "LOST",
                  "itemName": "i",
                  "category": "OTHER",
                  "location": "l",
                  "occurrenceDate": "2026-09-19",
                  "publicDescription": "p",
                  "privateIdentifyingDetail": "d",
                  "status": "SUBMITTED",
                  "createdAt": "2026-09-20T01:02:03.456Z"
                }
              ]
            }
            """;

    private static final String REPLACEMENT_DOCUMENT = """
            {
              "schemaVersion": 1,
              "reports": [
                {
                  "reportId": "30000000-0000-0000-0000-000000000001",
                  "reporterId": "r",
                  "reportType": "FOUND",
                  "itemName": "replacement item",
                  "category": "OTHER",
                  "location": "replacement location",
                  "occurrenceDate": "2026-09-01",
                  "publicDescription": "replacement public description",
                  "privateIdentifyingDetail": "replacement private detail",
                  "status": "UNDER_REVIEW",
                  "createdAt": "2026-09-20T01:02:03.456Z"
                }
              ]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    @Test
    void storedDocumentByteBoundaryIsEnforcedWithoutPartialLoadOrMutation()
            throws IOException, ReportStoreException {
        int limit = JsonReportRepository.MAX_STORE_BYTES;
        Path below = temporaryDirectory.resolve("below.json");
        Path exact = temporaryDirectory.resolve("exact.json");
        Path above = temporaryDirectory.resolve("above.json");
        Files.write(below, paddedEmptyDocument(limit - 1));
        Files.write(exact, paddedEmptyDocument(limit));
        Files.write(above, paddedEmptyDocument(limit + 1));

        assertTrue(new JsonReportRepository(below).loadAll().isEmpty(),
                "A valid store below the production limit must load");
        assertTrue(new JsonReportRepository(exact).loadAll().isEmpty(),
                "A valid store exactly at the production limit must load");

        byte[] before = Files.readAllBytes(above);
        ReportRepository oversized = new JsonReportRepository(above);
        assertCorrupt(() -> oversized.loadAll());
        assertCorrupt(() -> oversized.insert(originalReport()));
        assertCorrupt(() -> oversized.replace(originalReport().reportId(), originalReport()));
        assertTrue(Arrays.equals(before, Files.readAllBytes(above)),
                "An over-limit existing store must remain byte-for-byte unchanged");
    }

    @Test
    void encodedMutationByteBoundaryIsEnforcedBeforeFilesystemMutation()
            throws IOException, ReportStoreException {
        int originalBytes = ORIGINAL_DOCUMENT.getBytes(StandardCharsets.UTF_8).length;
        Path exactStore = temporaryDirectory.resolve("exact.json");
        ReportRepository exact = new JsonReportRepository(
                exactStore, new NioReportStoreFiles(), originalBytes);
        exact.insert(originalReport());
        assertTrue(Files.size(exactStore) == originalBytes,
                "A complete candidate exactly at its configured bound must persist");

        Path overStore = temporaryDirectory.resolve("over.json");
        ReportRepository over = new JsonReportRepository(
                overStore, new NioReportStoreFiles(), originalBytes - 1);
        ReportStoreException insertionFailure = assertThrows(
                ReportStoreException.class,
                () -> over.insert(originalReport()),
                "A candidate one byte over its configured bound must fail");
        assertTrue(insertionFailure.reason() == ReportStoreException.Reason.UNENCODABLE_OR_OVER_LIMIT_RESULT,
                "An over-limit candidate must expose its typed reason");
        assertFalse(Files.exists(overStore), "An over-limit first insertion must leave storage absent");

        Path replacementStore = temporaryDirectory.resolve("replacement.json");
        new JsonReportRepository(replacementStore).insert(originalReport());
        byte[] before = Files.readAllBytes(replacementStore);
        int replacementBytes = REPLACEMENT_DOCUMENT.getBytes(StandardCharsets.UTF_8).length;
        assertTrue(before.length < replacementBytes - 1,
                "The replacement fixture must leave room to read the previous store");
        ReportRepository boundedReplacement = new JsonReportRepository(
                replacementStore, new NioReportStoreFiles(), replacementBytes - 1);
        ReportStoreException replacementFailure = assertThrows(
                ReportStoreException.class,
                () -> boundedReplacement.replace(originalReport().reportId(), replacementReport()),
                "An over-limit replacement must fail before mutation");
        assertTrue(replacementFailure.reason()
                        == ReportStoreException.Reason.UNENCODABLE_OR_OVER_LIMIT_RESULT,
                "An over-limit replacement must expose its typed reason");
        assertTrue(Arrays.equals(before, Files.readAllBytes(replacementStore)),
                "An over-limit replacement must preserve previous bytes");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mutationFaultCases")
    void filesystemMutationFaultsPreserveOldStoreOrAbsenceAndNeverReportSuccess(
            String caseId, FaultStage stage) throws IOException {
        Path existingTarget = temporaryDirectory.resolve(caseId + "-existing.json");
        byte[] oldBytes = ORIGINAL_DOCUMENT.getBytes(StandardCharsets.UTF_8);
        Files.write(existingTarget, oldBytes);
        FailingMutationFiles existingFiles = new FailingMutationFiles(Optional.of(oldBytes), stage);
        ReportRepository existing = new JsonReportRepository(
                existingTarget, existingFiles, JsonReportRepository.MAX_STORE_BYTES);

        ReportStoreException existingFailure = assertThrows(
                ReportStoreException.class,
                () -> existing.replace(originalReport().reportId(), replacementReport()),
                "A transaction-stage failure must not report replacement success");
        assertTrue(existingFailure.reason()
                        == ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE,
                "A transaction-stage failure must expose the storage reason");
        assertTrue(Arrays.equals(oldBytes, Files.readAllBytes(existingTarget)),
                "A failed replacement must preserve the previous target");

        Path absentTarget = temporaryDirectory.resolve(caseId + "-absent.json");
        FailingMutationFiles absentFiles = new FailingMutationFiles(Optional.empty(), stage);
        ReportRepository absent = new JsonReportRepository(
                absentTarget, absentFiles, JsonReportRepository.MAX_STORE_BYTES);
        ReportStoreException absentFailure = assertThrows(
                ReportStoreException.class,
                () -> absent.insert(originalReport()),
                "A transaction-stage failure must not report insertion success");
        assertTrue(absentFailure.reason()
                        == ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE,
                "A failed first insertion must expose the storage reason");
        assertFalse(Files.exists(absentTarget), "A failed first insertion must leave the target absent");
    }

    @Test
    void unreadableOrNonRegularStoreBlocksEveryOperationWithoutAlteration() throws IOException {
        Path directoryTarget = temporaryDirectory.resolve("reports.json");
        Files.createDirectory(directoryTarget);
        ReportRepository directoryRepository = new JsonReportRepository(directoryTarget);
        assertStorageFailure(() -> directoryRepository.loadAll());
        assertStorageFailure(() -> directoryRepository.insert(originalReport()));
        assertStorageFailure(() -> directoryRepository.replace(originalReport().reportId(), originalReport()));
        assertTrue(Files.isDirectory(directoryTarget), "A non-regular target must remain a directory");

        Path parentFile = temporaryDirectory.resolve("parent-file");
        byte[] parentBytes = "synthetic-parent".getBytes(StandardCharsets.UTF_8);
        Files.write(parentFile, parentBytes);
        ReportRepository invalidParent = new JsonReportRepository(parentFile.resolve("reports.json"));
        assertStorageFailure(() -> invalidParent.loadAll());
        assertStorageFailure(() -> invalidParent.insert(originalReport()));
        assertTrue(Arrays.equals(parentBytes, Files.readAllBytes(parentFile)),
                "A non-directory parent must remain unchanged");
    }

    @Test
    void orphanTemporaryArtifactsNeverBecomeAuthoritativeStorage() throws IOException, ReportStoreException {
        Path validTarget = temporaryDirectory.resolve("valid.json");
        ReportRepository validRepository = new JsonReportRepository(validTarget);
        validRepository.insert(originalReport());
        Path validOrphan = temporaryDirectory.resolve(".report-store-valid.tmp");
        Files.writeString(validOrphan, "synthetic orphan", StandardCharsets.UTF_8);
        assertTrue(validRepository.loadAll().getFirst().equals(originalReport()),
                "An orphan sibling must not override a valid target");
        assertTrue(Files.exists(validOrphan), "Loading must not delete an orphan sibling");

        Path missingTarget = temporaryDirectory.resolve("missing.json");
        Path missingOrphan = temporaryDirectory.resolve(".report-store-missing.tmp");
        Files.write(missingOrphan, ORIGINAL_DOCUMENT.getBytes(StandardCharsets.UTF_8));
        assertTrue(new JsonReportRepository(missingTarget).loadAll().isEmpty(),
                "An orphan sibling must not be promoted for a missing target");
        assertTrue(Files.exists(missingOrphan), "A missing-target load must leave an orphan sibling untouched");
    }

    @Test
    void successfulMutationIsVisibleOnlyAsACompleteDocument() throws ReportStoreException {
        MemoryFiles files = new MemoryFiles();
        Path target = temporaryDirectory.resolve("reports.json");
        ReportRepository writer = new JsonReportRepository(target, files, JsonReportRepository.MAX_STORE_BYTES);

        writer.insert(originalReport());

        assertFalse(files.beforeCommitWasPresent(), "First insertion must begin from an absent target");
        List<ItemReport> loaded = new JsonReportRepository(
                target, files, JsonReportRepository.MAX_STORE_BYTES).loadAll();
        assertTrue(loaded.size() == 1, "A successful commit must publish one complete document");
        assertTrue(loaded.getFirst().equals(originalReport()),
                "A fresh repository must reconstruct the complete committed state");
    }

    private static Stream<Arguments> mutationFaultCases() {
        return Stream.of(FaultStage.values())
                .map(stage -> Arguments.of(stage.caseId, stage));
    }

    private static byte[] paddedEmptyDocument(int byteCount) {
        byte[] prefix = "{\"schemaVersion\":1,\"reports\":[]}".getBytes(StandardCharsets.UTF_8);
        byte[] document = new byte[byteCount];
        System.arraycopy(prefix, 0, document, 0, prefix.length);
        Arrays.fill(document, prefix.length, document.length, (byte) ' ');
        return document;
    }

    private static void assertCorrupt(StoreAction action) {
        ReportStoreException failure = assertThrows(
                ReportStoreException.class, action::run, "An oversized store must block the operation");
        assertTrue(failure.reason() == ReportStoreException.Reason.CORRUPT_OR_UNSUPPORTED_STORE,
                "An oversized store must expose the corrupt-or-unsupported reason");
    }

    private static void assertStorageFailure(StoreAction action) {
        ReportStoreException failure = assertThrows(
                ReportStoreException.class, action::run, "An inaccessible path must block the operation");
        assertTrue(failure.reason()
                        == ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE,
                "An inaccessible path must expose the storage reason");
    }

    private static ItemReport originalReport() {
        return new ItemReport(
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                "r",
                ReportType.LOST,
                "i",
                ItemCategory.OTHER,
                "l",
                LocalDate.of(2026, 9, 19),
                "p",
                "d",
                ReportStatus.SUBMITTED,
                Instant.parse("2026-09-20T01:02:03.456Z"));
    }

    private static ItemReport replacementReport() {
        ItemReport original = originalReport();
        return new ItemReport(
                original.reportId(),
                original.reporterId(),
                ReportType.FOUND,
                "replacement item",
                ItemCategory.OTHER,
                "replacement location",
                LocalDate.of(2026, 9, 1),
                "replacement public description",
                "replacement private detail",
                ReportStatus.UNDER_REVIEW,
                original.createdAt());
    }

    private enum FaultStage {
        PARENT_CREATION("F01"),
        TEMPORARY_CREATION("F02"),
        TEMPORARY_WRITE("F03"),
        FORCE_OR_CLOSE("F04"),
        UNSUPPORTED_ATOMIC_MOVE("F05"),
        ATOMIC_MOVE("F06"),
        CLEANUP_AFTER_PRIMARY_FAILURE("F07");

        private final String caseId;

        FaultStage(String id) {
            this.caseId = id;
        }
    }

    private static final class FailingMutationFiles implements ReportStoreFiles {
        private final Optional<byte[]> current;

        @SuppressWarnings("unused")
        private final FaultStage stage;

        FailingMutationFiles(Optional<byte[]> initial, FaultStage failureStage) {
            this.current = initial.map(byte[]::clone);
            this.stage = failureStage;
        }

        @Override
        public Optional<byte[]> readBounded(Path target, int maximumBytes) {
            return current.map(byte[]::clone);
        }

        @Override
        public void replaceAtomically(Path target, byte[] completeDocument) throws StoreFileFailure {
            throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
        }
    }

    private static final class MemoryFiles implements ReportStoreFiles {
        private byte[] current;

        private boolean beforeCommitWasPresent;

        @Override
        public Optional<byte[]> readBounded(Path target, int maximumBytes) {
            return current == null ? Optional.empty() : Optional.of(current.clone());
        }

        @Override
        public void replaceAtomically(Path target, byte[] completeDocument) {
            beforeCommitWasPresent = current != null;
            current = completeDocument.clone();
        }

        boolean beforeCommitWasPresent() {
            return beforeCommitWasPresent;
        }
    }

    @FunctionalInterface
    private interface StoreAction {
        void run() throws ReportStoreException;
    }
}
