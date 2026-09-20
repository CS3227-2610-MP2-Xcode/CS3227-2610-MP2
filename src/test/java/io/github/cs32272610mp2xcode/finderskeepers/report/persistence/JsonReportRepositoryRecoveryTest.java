package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.report.model.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.model.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.model.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.model.ReportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonReportRepositoryRecoveryTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void everyOperationRereadsTheAuthoritativeTarget() throws IOException, ReportStoreException {
        Path store = temporaryDirectory.resolve("reports.json");
        ReportRepository repository = new JsonReportRepository(store);
        ItemReport original = report(1);
        repository.insert(original);
        byte[] validBytes = Files.readAllBytes(store);
        assertTrue(repository.loadAll().getFirst().equals(original),
                "The initial valid target must load");

        byte[] corruptBytes = "synthetic corrupt document".getBytes(StandardCharsets.UTF_8);
        Files.write(store, corruptBytes);
        assertCorrupt(() -> repository.loadAll());
        assertCorrupt(() -> repository.insert(report(2)));
        assertCorrupt(() -> repository.replace(original.reportId(), original));
        assertTrue(Arrays.equals(corruptBytes, Files.readAllBytes(store)),
                "Cached state must not overwrite externally corrupted bytes");

        Files.write(store, validBytes);
        assertTrue(repository.loadAll().getFirst().equals(original),
                "The same repository instance must observe external recovery");
    }

    @Test
    void failurePrecedenceIsDeterministicAndPreservesStorage() throws IOException, ReportStoreException {
        Path store = temporaryDirectory.resolve("reports.json");
        ReportRepository repository = new JsonReportRepository(store);
        ItemReport original = report(1);
        repository.insert(original);
        byte[] validBytes = Files.readAllBytes(store);

        byte[] corruptBytes = "synthetic corrupt document".getBytes(StandardCharsets.UTF_8);
        Files.write(store, corruptBytes);
        assertReason(
                ReportStoreException.Reason.CORRUPT_OR_UNSUPPORTED_STORE,
                () -> repository.insert(original));
        assertTrue(Arrays.equals(corruptBytes, Files.readAllBytes(store)),
                "Corrupt storage must take precedence over a duplicate conflict");

        Files.write(store, validBytes);
        ItemReport invalidMissing = withIdentityAndText(
                original, new UUID(0L, 99L), original.reporterId(), "invalid-\ud800");
        assertReason(
                ReportStoreException.Reason.REPLACEMENT_TARGET_NOT_FOUND,
                () -> repository.replace(invalidMissing.reportId(), invalidMissing));
        assertTrue(Arrays.equals(validBytes, Files.readAllBytes(store)),
                "A missing target must take precedence over invalid candidate text");

        ItemReport invalidDuplicate = withIdentityAndText(
                original, original.reportId(), original.reporterId(), "invalid-\ud800");
        assertReason(
                ReportStoreException.Reason.DUPLICATE_REPORT_ID,
                () -> repository.insert(invalidDuplicate));
        assertTrue(Arrays.equals(validBytes, Files.readAllBytes(store)),
                "A duplicate conflict must take precedence over invalid candidate text");

        ItemReport invalidImmutable = withIdentityAndText(
                original, original.reportId(), "different-reporter", "invalid-\ud800");
        assertReason(
                ReportStoreException.Reason.IMMUTABLE_FIELD_MISMATCH,
                () -> repository.replace(original.reportId(), invalidImmutable));
        assertTrue(Arrays.equals(validBytes, Files.readAllBytes(store)),
                "An immutable mismatch must take precedence over invalid candidate text");
    }

    @Test
    void failureReasonsAreTypedAndDiagnosticsContainNoReportValues()
            throws IOException, ReportStoreException {
        ItemReport original = report(1);
        Path duplicateStore = temporaryDirectory.resolve("privacy-duplicate.json");
        ReportRepository duplicateRepository = new JsonReportRepository(duplicateStore);
        duplicateRepository.insert(original);

        Path corruptStore = temporaryDirectory.resolve("privacy-corrupt.json");
        Files.writeString(corruptStore, "synthetic invalid document", StandardCharsets.UTF_8);

        ReportStoreFiles inaccessibleFiles = new ReportStoreFiles() {
            @Override
            public Optional<byte[]> readBounded(Path target, int maximumBytes) throws StoreFileFailure {
                throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
            }

            @Override
            public void replaceAtomically(Path target, byte[] completeDocument)
                    throws StoreFileFailure {
                throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
            }
        };
        ReportRepository inaccessibleRepository = new JsonReportRepository(
                temporaryDirectory.resolve("privacy-inaccessible.json"),
                inaccessibleFiles,
                JsonReportRepository.MAX_STORE_BYTES);

        ItemReport immutableMismatch = withIdentityAndText(
                original, original.reportId(), "different-reporter", original.itemName());
        ItemReport unencodable = withIdentityAndText(
                original, new UUID(5L, 99L), original.reporterId(), "invalid-\ud800");

        List<FailureExpectation> expectations = List.of(
                new FailureExpectation(
                        ReportStoreException.Reason.DUPLICATE_REPORT_ID,
                        "A report with that identifier already exists."),
                new FailureExpectation(
                        ReportStoreException.Reason.REPLACEMENT_TARGET_NOT_FOUND,
                        "The report to replace does not exist."),
                new FailureExpectation(
                        ReportStoreException.Reason.IMMUTABLE_FIELD_MISMATCH,
                        "The replacement changes immutable report identity."),
                new FailureExpectation(
                        ReportStoreException.Reason.CORRUPT_OR_UNSUPPORTED_STORE,
                        "The report store is corrupt or uses an unsupported format."),
                new FailureExpectation(
                        ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE,
                        "The report store could not be accessed or safely replaced."),
                new FailureExpectation(
                        ReportStoreException.Reason.UNENCODABLE_OR_OVER_LIMIT_RESULT,
                        "The requested report state is not encodable or exceeds the supported store limit."));

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        List<ReportStoreException> failures;
        try (PrintStream capture = new PrintStream(capturedOutput, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            System.setErr(capture);
            failures = List.of(
                    captureFailure(() -> duplicateRepository.insert(original)),
                    captureFailure(() -> new JsonReportRepository(
                            temporaryDirectory.resolve("privacy-missing.json"))
                            .replace(original.reportId(), original)),
                    captureFailure(() -> duplicateRepository.replace(
                            original.reportId(), immutableMismatch)),
                    captureFailure(() -> new JsonReportRepository(corruptStore).loadAll()),
                    captureFailure(() -> inaccessibleRepository.loadAll()),
                    captureFailure(() -> new JsonReportRepository(
                            temporaryDirectory.resolve("privacy-unencodable.json"))
                            .insert(unencodable)));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        for (int index = 0; index < expectations.size(); index++) {
            FailureExpectation expectation = expectations.get(index);
            ReportStoreException failure = failures.get(index);
            assertTrue(failure.reason() == expectation.reason(), "The typed failure reason must be retained");
            assertTrue(failure.getMessage().equals(expectation.message()),
                    "Each reason must select its fixed generic diagnostic");
            assertTrue(failure.getCause() == null, "A public persistence failure must not expose a cause");
            assertTrue(failure.getSuppressed().length == 0,
                    "A public persistence failure must not expose suppressed details");
        }
        assertTrue(capturedOutput.size() == 0,
                "Persistence failures must not print report content or diagnostics");
    }

    private static void assertCorrupt(StoreAction action) {
        assertReason(ReportStoreException.Reason.CORRUPT_OR_UNSUPPORTED_STORE, action);
    }

    private static void assertReason(ReportStoreException.Reason reason, StoreAction action) {
        ReportStoreException failure = assertThrows(
                ReportStoreException.class, action::run, "The operation must expose one typed failure");
        assertTrue(failure.reason() == reason, "Failure precedence must select the approved reason");
    }

    private static ReportStoreException captureFailure(StoreAction action) {
        return assertThrows(
                ReportStoreException.class, action::run, "The operation must expose one typed failure");
    }

    private static ItemReport report(int sequence) {
        return new ItemReport(
                new UUID(5L, sequence),
                "reporter-" + sequence,
                ReportType.LOST,
                "Item " + sequence,
                ItemCategory.OTHER,
                "Location " + sequence,
                LocalDate.of(2026, 9, 10 + sequence),
                "Public description " + sequence,
                "Private detail " + sequence,
                ReportStatus.SUBMITTED,
                Instant.parse("2026-09-20T01:02:03.456Z"));
    }

    private static ItemReport withIdentityAndText(
            ItemReport source, UUID reportId, String reporterId, String itemName) {
        return new ItemReport(
                reportId,
                reporterId,
                source.reportType(),
                itemName,
                source.category(),
                source.location(),
                source.occurrenceDate(),
                source.publicDescription(),
                source.privateIdentifyingDetail(),
                source.status(),
                source.createdAt());
    }

    private record FailureExpectation(ReportStoreException.Reason reason, String message) {
    }

    @FunctionalInterface
    private interface StoreAction {
        void run() throws ReportStoreException;
    }
}
