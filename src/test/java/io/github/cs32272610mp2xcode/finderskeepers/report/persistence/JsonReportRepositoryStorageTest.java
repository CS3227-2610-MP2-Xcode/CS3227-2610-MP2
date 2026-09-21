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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonReportRepositoryStorageTest {
    private static final String DOCUMENT_PREFIX = "{\n  \"schemaVersion\": 1,\n  \"reports\": [";

    private static final String DOCUMENT_SUFFIX = "\n  ]\n}\n";

    private static final String CAPACITY_CONTROL = "\u0001";

    private static final int[] TEXT_LIMITS = {128, 100, 120, 500, 500};

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

    @Test
    void publicPolicyAcceptsBelowLimitAndRejectsOverLimitMutations()
            throws IOException, ReportStoreException {
        Path belowTarget = temporaryDirectory.resolve("public-below.json");
        ReportRepository belowRepository = new JsonReportRepository(belowTarget);
        belowRepository.insert(originalReport());
        belowRepository.replace(originalReport().reportId(), replacementReport());
        assertTrue(belowRepository.loadAll().getFirst().equals(replacementReport()),
                "The public production policy must accept an ordinary complete mutation");

        CapacityDocument nearLimit = createNearLimitDocument();
        assertTrue(nearLimit.bytes().length <= JsonReportRepository.MAX_STORE_BYTES,
                "The independent capacity fixture must remain within the production limit");

        Path insertTarget = temporaryDirectory.resolve("public-over-insert.json");
        Files.write(insertTarget, nearLimit.bytes());
        ReportRepository insertionRepository = new JsonReportRepository(insertTarget);
        assertOverLimit(() -> insertionRepository.insert(maximumReport(new UUID(9L, Long.MAX_VALUE))));
        assertTrue(Arrays.equals(nearLimit.bytes(), Files.readAllBytes(insertTarget)),
                "A production-limit insertion failure must preserve the existing store");

        Path replacementTarget = temporaryDirectory.resolve("public-over-replace.json");
        Files.write(replacementTarget, nearLimit.bytes());
        ReportRepository replacementRepository = new JsonReportRepository(replacementTarget);
        assertOverLimit(() -> replacementRepository.replace(
                nearLimit.target().reportId(), maximumMutableReplacement(nearLimit.target())));
        assertTrue(Arrays.equals(nearLimit.bytes(), Files.readAllBytes(replacementTarget)),
                "A production-limit replacement failure must preserve the existing store");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mutationFaultCases")
    void filesystemMutationFaultsPreserveOldStoreOrAbsenceAndNeverReportSuccess(
            String caseId, FaultStage stage) throws IOException, ReportStoreException {
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
        assertTrue(new JsonReportRepository(existingTarget).loadAll().getFirst().equals(originalReport()),
                "The preserved target must remain a complete readable document");
        assertFaultStageEffects(existingFiles, stage, false);

        Path absentTarget = temporaryDirectory.resolve(caseId + "-absent").resolve("reports.json");
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
        assertFaultStageEffects(absentFiles, stage, true);
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
        assertStorageFailure(() -> invalidParent.replace(originalReport().reportId(), originalReport()));
        assertTrue(Arrays.equals(parentBytes, Files.readAllBytes(parentFile)),
                "A non-directory parent must remain unchanged");
    }

    @Test
    void injectedReadFailureBlocksEveryOperationBeforeMutation() {
        Path target = temporaryDirectory.resolve("read-failure.json");
        FailingReadFiles files = new FailingReadFiles();
        ReportRepository repository = new JsonReportRepository(
                target, files, JsonReportRepository.MAX_STORE_BYTES);

        assertStorageFailure(() -> repository.loadAll());
        assertStorageFailure(() -> repository.insert(originalReport()));
        assertStorageFailure(() -> repository.replace(originalReport().reportId(), originalReport()));

        assertTrue(files.readAttempts() == 3,
                "Every repository operation must attempt the authoritative read");
        assertFalse(files.mutationAttempted(),
                "A failed authoritative read must prevent destination mutation");
        assertFalse(Files.exists(target), "An injected read failure must not create storage");
    }

    @Test
    void orphanTemporaryArtifactsNeverBecomeAuthoritativeStorage() throws IOException, ReportStoreException {
        Path validTarget = temporaryDirectory.resolve("valid.json");
        ReportRepository validRepository = new JsonReportRepository(validTarget);
        validRepository.insert(originalReport());
        Path validCompleteOrphan = temporaryDirectory.resolve(".report-store-valid-complete.tmp");
        Path validPartialOrphan = temporaryDirectory.resolve(".report-store-valid-partial.tmp");
        byte[] completeOrphanBytes = REPLACEMENT_DOCUMENT.getBytes(StandardCharsets.UTF_8);
        byte[] partialOrphanBytes = "{\n  \"schemaVersion\"".getBytes(StandardCharsets.UTF_8);
        Files.write(validCompleteOrphan, completeOrphanBytes);
        Files.write(validPartialOrphan, partialOrphanBytes);
        assertTrue(validRepository.loadAll().getFirst().equals(originalReport()),
                "Complete or partial orphan siblings must not override a valid target");
        assertTrue(Arrays.equals(completeOrphanBytes, Files.readAllBytes(validCompleteOrphan)),
                "Loading must not alter a complete orphan sibling");
        assertTrue(Arrays.equals(partialOrphanBytes, Files.readAllBytes(validPartialOrphan)),
                "Loading must not alter a partial orphan sibling");

        Path missingTarget = temporaryDirectory.resolve("missing.json");
        Path missingCompleteOrphan = temporaryDirectory.resolve(".report-store-missing-complete.tmp");
        Path missingPartialOrphan = temporaryDirectory.resolve(".report-store-missing-partial.tmp");
        byte[] missingCompleteBytes = ORIGINAL_DOCUMENT.getBytes(StandardCharsets.UTF_8);
        Files.write(missingCompleteOrphan, missingCompleteBytes);
        Files.write(missingPartialOrphan, partialOrphanBytes);
        assertTrue(new JsonReportRepository(missingTarget).loadAll().isEmpty(),
                "Complete or partial orphan siblings must not be promoted for a missing target");
        assertTrue(Arrays.equals(missingCompleteBytes, Files.readAllBytes(missingCompleteOrphan)),
                "A missing-target load must not alter a complete orphan sibling");
        assertTrue(Arrays.equals(partialOrphanBytes, Files.readAllBytes(missingPartialOrphan)),
                "A missing-target load must not alter a partial orphan sibling");
    }

    @Test
    void successfulMutationIsVisibleOnlyAsACompleteDocument() throws ReportStoreException {
        MemoryFiles insertionFiles = new MemoryFiles();
        Path insertionTarget = temporaryDirectory.resolve("insertion.json");
        ReportRepository insertionWriter = new JsonReportRepository(
                insertionTarget, insertionFiles, JsonReportRepository.MAX_STORE_BYTES);

        insertionWriter.insert(originalReport());

        assertFalse(insertionFiles.beforeCommitWasPresent(),
                "First insertion must begin from an absent target");
        List<ItemReport> loaded = new JsonReportRepository(
                insertionTarget, insertionFiles, JsonReportRepository.MAX_STORE_BYTES).loadAll();
        assertTrue(loaded.size() == 1, "A successful commit must publish one complete document");
        assertTrue(loaded.getFirst().equals(originalReport()),
                "A fresh repository must reconstruct the complete committed state");

        byte[] oldDocument = ORIGINAL_DOCUMENT.getBytes(StandardCharsets.UTF_8);
        MemoryFiles replacementFiles = new MemoryFiles(oldDocument);
        Path replacementTarget = temporaryDirectory.resolve("replacement.json");
        ReportRepository replacementWriter = new JsonReportRepository(
                replacementTarget, replacementFiles, JsonReportRepository.MAX_STORE_BYTES);

        replacementWriter.replace(originalReport().reportId(), replacementReport());

        assertTrue(Arrays.equals(oldDocument, replacementFiles.beforeCommitBytes()),
                "The controlled pre-commit observation must see the complete old document");
        List<ItemReport> replaced = new JsonReportRepository(
                replacementTarget, replacementFiles, JsonReportRepository.MAX_STORE_BYTES).loadAll();
        assertTrue(replaced.size() == 1, "A replacement commit must publish one complete document");
        assertTrue(replaced.getFirst().equals(replacementReport()),
                "A fresh repository must reconstruct the complete replacement document");
    }

    private static Stream<Arguments> mutationFaultCases() {
        return Stream.of(FaultStage.values())
                .map(stage -> Arguments.of(stage.caseId, stage));
    }

    private static CapacityDocument createNearLimitDocument() {
        int limit = JsonReportRepository.MAX_STORE_BYTES;
        ItemReport target = minimumReport(new UUID(9L, 0L));
        List<String> blocks = new ArrayList<>();
        blocks.add(renderReportBlock(target));
        int currentLength = DOCUMENT_PREFIX.length() + blocks.getFirst().length()
                + DOCUMENT_SUFFIX.length();
        long sequence = 1L;
        while (true) {
            ItemReport report = maximumReport(new UUID(9L, sequence));
            String block = renderReportBlock(report);
            if (currentLength + 1 + block.length() > limit) {
                break;
            }
            blocks.add(block);
            currentLength += 1 + block.length();
            sequence++;
        }

        int availableBlockLength = limit - currentLength - 1;
        ItemReport minimumFiller = minimumReport(new UUID(9L, sequence));
        if (availableBlockLength >= renderReportBlock(minimumFiller).length()) {
            ItemReport filler = largestReportAtMost(minimumFiller.reportId(), availableBlockLength);
            String fillerBlock = renderReportBlock(filler);
            blocks.add(fillerBlock);
            currentLength += 1 + fillerBlock.length();
        }

        StringBuilder document = new StringBuilder(currentLength);
        document.append(DOCUMENT_PREFIX);
        for (int index = 0; index < blocks.size(); index++) {
            if (index > 0) {
                document.append(',');
            }
            document.append(blocks.get(index));
        }
        document.append(DOCUMENT_SUFFIX);
        byte[] bytes = document.toString().getBytes(StandardCharsets.UTF_8);
        int headroom = limit - bytes.length;
        int insertionGrowth = 1 + renderReportBlock(
                maximumReport(new UUID(9L, Long.MAX_VALUE))).length();
        int replacementGrowth = renderReportBlock(maximumMutableReplacement(target)).length()
                - renderReportBlock(target).length();
        if (headroom < 0 || headroom >= insertionGrowth || headroom >= replacementGrowth) {
            throw new IllegalStateException("Capacity fixture did not reach the required safe boundary");
        }
        return new CapacityDocument(bytes, target);
    }

    private static ItemReport largestReportAtMost(UUID reportId, int maximumBlockLength) {
        ItemReport minimum = minimumReport(reportId);
        int minimumLength = renderReportBlock(minimum).length();
        int maximumAdditionalCodePoints = Arrays.stream(TEXT_LIMITS).sum() - TEXT_LIMITS.length;
        int maximumEncodedGrowth = maximumAdditionalCodePoints
                + 5 * Arrays.stream(TEXT_LIMITS).sum();
        int permittedGrowth = Math.min(maximumBlockLength - minimumLength, maximumEncodedGrowth);
        for (int growth = permittedGrowth; growth >= 0; growth--) {
            ItemReport candidate = reportWithEncodedTextGrowth(reportId, growth);
            if (candidate != null) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("No capacity report fits the requested block length");
    }

    private static ItemReport reportWithEncodedTextGrowth(UUID reportId, int growth) {
        int maximumAdditionalCodePoints = Arrays.stream(TEXT_LIMITS).sum() - TEXT_LIMITS.length;
        for (int additionalCodePoints = 0;
                additionalCodePoints <= maximumAdditionalCodePoints;
                additionalCodePoints++) {
            int escapeGrowth = growth - additionalCodePoints;
            if (escapeGrowth < 0 || escapeGrowth % 5 != 0) {
                continue;
            }
            int escapedCodePoints = escapeGrowth / 5;
            int totalCodePoints = TEXT_LIMITS.length + additionalCodePoints;
            if (escapedCodePoints <= totalCodePoints) {
                return capacityReport(reportId, totalCodePoints, escapedCodePoints);
            }
        }
        return null;
    }

    private static ItemReport capacityReport(UUID reportId, int totalCodePoints, int escapedCodePoints) {
        String[] values = new String[TEXT_LIMITS.length];
        int remainingCodePoints = totalCodePoints;
        int remainingEscapes = escapedCodePoints;
        for (int index = 0; index < TEXT_LIMITS.length; index++) {
            int remainingFields = TEXT_LIMITS.length - index - 1;
            int fieldCodePoints = Math.min(
                    TEXT_LIMITS[index], remainingCodePoints - remainingFields);
            int fieldEscapes = Math.min(fieldCodePoints, remainingEscapes);
            values[index] = CAPACITY_CONTROL.repeat(fieldEscapes)
                    + "x".repeat(fieldCodePoints - fieldEscapes);
            remainingCodePoints -= fieldCodePoints;
            remainingEscapes -= fieldEscapes;
        }
        if (remainingCodePoints != 0 || remainingEscapes != 0) {
            throw new IllegalArgumentException("Capacity text could not be distributed");
        }
        return ItemReport.restore(
                reportId,
                values[0],
                ReportType.LOST,
                values[1],
                ItemCategory.OTHER,
                values[2],
                LocalDate.of(2026, 9, 19),
                values[3],
                values[4],
                ReportStatus.SUBMITTED,
                Instant.parse("2026-09-20T01:02:03.456Z"));
    }

    private static ItemReport minimumReport(UUID reportId) {
        return capacityReport(reportId, TEXT_LIMITS.length, 0);
    }

    private static ItemReport maximumReport(UUID reportId) {
        int codePoints = Arrays.stream(TEXT_LIMITS).sum();
        return capacityReport(reportId, codePoints, codePoints);
    }

    private static ItemReport maximumMutableReplacement(ItemReport original) {
        return ItemReport.restore(
                original.reportId(),
                original.reporterId(),
                ReportType.FOUND,
                CAPACITY_CONTROL.repeat(100),
                ItemCategory.OTHER,
                CAPACITY_CONTROL.repeat(120),
                original.occurrenceDate(),
                CAPACITY_CONTROL.repeat(500),
                CAPACITY_CONTROL.repeat(500),
                ReportStatus.UNDER_REVIEW,
                original.createdAt());
    }

    private static String renderReportBlock(ItemReport report) {
        StringBuilder block = new StringBuilder();
        block.append("\n    {\n");
        appendFixtureMember(block, "reportId", report.reportId().toString(), true);
        appendFixtureMember(block, "reporterId", report.reporterId(), false);
        appendFixtureMember(block, "reportType", report.reportType().storedName(), false);
        appendFixtureMember(block, "itemName", report.itemName(), false);
        appendFixtureMember(block, "category", report.category().storedName(), false);
        appendFixtureMember(block, "location", report.location(), false);
        appendFixtureMember(block, "occurrenceDate", report.occurrenceDate().toString(), false);
        appendFixtureMember(block, "publicDescription", report.publicDescription(), false);
        appendFixtureMember(
                block, "privateIdentifyingDetail", report.privateIdentifyingDetail(), false);
        appendFixtureMember(block, "status", report.status().storedName(), false);
        appendFixtureMember(block, "createdAt", report.createdAt().toString(), false);
        block.append("\n    }");
        return block.toString();
    }

    private static void appendFixtureMember(
            StringBuilder output, String name, String value, boolean first) {
        if (!first) {
            output.append(",\n");
        }
        output.append("      \"").append(name).append("\": \"");
        appendFixtureString(output, value);
        output.append('"');
    }

    private static void appendFixtureString(StringBuilder output, String value) {
        char[] hex = "0123456789abcdef".toCharArray();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (current < 0x20) {
                        output.append("\\u")
                                .append(hex[current >>> 12])
                                .append(hex[current >>> 8 & 0x0f])
                                .append(hex[current >>> 4 & 0x0f])
                                .append(hex[current & 0x0f]);
                    } else {
                        output.append(current);
                    }
                }
            }
        }
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

    private static void assertOverLimit(StoreAction action) {
        ReportStoreException failure = assertThrows(
                ReportStoreException.class,
                action::run,
                "A production-bound candidate above 16 MiB must fail before mutation");
        assertTrue(failure.reason()
                        == ReportStoreException.Reason.UNENCODABLE_OR_OVER_LIMIT_RESULT,
                "An over-limit candidate must expose its typed reason");
    }

    private static void assertStorageFailure(StoreAction action) {
        ReportStoreException failure = assertThrows(
                ReportStoreException.class, action::run, "An inaccessible path must block the operation");
        assertTrue(failure.reason()
                        == ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE,
                "An inaccessible path must expose the storage reason");
    }

    private static void assertFaultStageEffects(
            FailingMutationFiles files, FaultStage stage, boolean initiallyMissingParent) {
        boolean parentReady = stage != FaultStage.PARENT_CREATION;
        boolean temporaryCreated = stage.ordinal() >= FaultStage.TEMPORARY_WRITE.ordinal();
        boolean completeTemporary = stage.ordinal() >= FaultStage.FORCE_OR_CLOSE.ordinal();
        boolean forceAttempted = stage.ordinal() >= FaultStage.FORCE_OR_CLOSE.ordinal();
        boolean unsupportedMove = stage == FaultStage.UNSUPPORTED_ATOMIC_MOVE;
        boolean atomicMove = stage == FaultStage.ATOMIC_MOVE
                || stage == FaultStage.CLEANUP_AFTER_PRIMARY_FAILURE;
        boolean cleanupAttempted = temporaryCreated;
        boolean orphanExpected = stage == FaultStage.CLEANUP_AFTER_PRIMARY_FAILURE;

        assertTrue(files.parentReady() == parentReady,
                "The injected stage must distinguish parent creation progress");
        assertTrue(files.temporaryCreated() == temporaryCreated,
                "The injected stage must distinguish temporary-file creation progress");
        assertTrue(files.completeTemporary() == completeTemporary,
                "The injected stage must distinguish partial from complete staging");
        assertTrue(files.forceAttempted() == forceAttempted,
                "The injected stage must distinguish force or close progress");
        assertTrue(files.unsupportedMoveAttempted() == unsupportedMove,
                "The injected stage must distinguish unsupported atomic replacement");
        assertTrue(files.atomicMoveAttempted() == atomicMove,
                "The injected stage must distinguish an attempted atomic replacement");
        assertTrue(files.cleanupAttempted() == cleanupAttempted,
                "Every created temporary artifact must reach cleanup handling");
        if (initiallyMissingParent) {
            assertTrue(Files.exists(files.targetParent()) == parentReady,
                    "Only a completed parent-creation stage may leave an empty parent directory");
        }
        if (temporaryCreated) {
            assertTrue(Files.exists(files.temporaryPath()) == orphanExpected,
                    "Only an injected cleanup failure may leave a synthetic orphan sibling");
        }
    }

    private static ItemReport originalReport() {
        return ItemReport.restore(
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
        return ItemReport.restore(
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

    private static final class FailingReadFiles implements ReportStoreFiles {
        private int readAttempts;

        private boolean mutationAttempted;

        @Override
        public Optional<byte[]> readBounded(Path target, int maximumBytes) throws StoreFileFailure {
            readAttempts++;
            throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
        }

        @Override
        public void replaceAtomically(Path target, byte[] completeDocument) {
            mutationAttempted = true;
        }

        int readAttempts() {
            return readAttempts;
        }

        boolean mutationAttempted() {
            return mutationAttempted;
        }
    }

    private static final class FailingMutationFiles implements ReportStoreFiles {
        private final Optional<byte[]> current;

        private final FaultStage stage;

        private Path targetParent;

        private Path temporaryPath;

        private boolean parentReady;

        private boolean temporaryCreated;

        private boolean completeTemporary;

        private boolean forceAttempted;

        private boolean unsupportedMoveAttempted;

        private boolean atomicMoveAttempted;

        private boolean cleanupAttempted;

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
            targetParent = target.getParent();
            if (stage == FaultStage.PARENT_CREATION) {
                throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
            }
            try {
                Files.createDirectories(targetParent);
                parentReady = true;
                if (stage == FaultStage.TEMPORARY_CREATION) {
                    throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
                }
                temporaryPath = target.resolveSibling(".synthetic-" + stage.caseId + ".tmp");
                temporaryCreated = true;
                if (stage == FaultStage.TEMPORARY_WRITE) {
                    Files.write(temporaryPath, new byte[] {'{'});
                } else {
                    Files.write(temporaryPath, completeDocument);
                    completeTemporary = true;
                    forceAttempted = true;
                    unsupportedMoveAttempted = stage == FaultStage.UNSUPPORTED_ATOMIC_MOVE;
                    atomicMoveAttempted = stage == FaultStage.ATOMIC_MOVE
                            || stage == FaultStage.CLEANUP_AFTER_PRIMARY_FAILURE;
                }
                cleanupAttempted = true;
                if (stage != FaultStage.CLEANUP_AFTER_PRIMARY_FAILURE) {
                    Files.deleteIfExists(temporaryPath);
                }
            } catch (IOException failure) {
                throw new AssertionError("Synthetic fault setup failed");
            }
            throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
        }

        Path targetParent() {
            return targetParent;
        }

        Path temporaryPath() {
            return temporaryPath;
        }

        boolean parentReady() {
            return parentReady;
        }

        boolean temporaryCreated() {
            return temporaryCreated;
        }

        boolean completeTemporary() {
            return completeTemporary;
        }

        boolean forceAttempted() {
            return forceAttempted;
        }

        boolean unsupportedMoveAttempted() {
            return unsupportedMoveAttempted;
        }

        boolean atomicMoveAttempted() {
            return atomicMoveAttempted;
        }

        boolean cleanupAttempted() {
            return cleanupAttempted;
        }
    }

    private static final class MemoryFiles implements ReportStoreFiles {
        private byte[] current;

        private byte[] beforeCommit;

        MemoryFiles() {
        }

        MemoryFiles(byte[] initialDocument) {
            current = initialDocument.clone();
        }

        @Override
        public Optional<byte[]> readBounded(Path target, int maximumBytes) {
            return current == null ? Optional.empty() : Optional.of(current.clone());
        }

        @Override
        public void replaceAtomically(Path target, byte[] completeDocument) {
            beforeCommit = current == null ? null : current.clone();
            current = completeDocument.clone();
        }

        boolean beforeCommitWasPresent() {
            return beforeCommit != null;
        }

        byte[] beforeCommitBytes() {
            return beforeCommit.clone();
        }
    }

    private record CapacityDocument(byte[] bytes, ItemReport target) {
        CapacityDocument {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    @FunctionalInterface
    private interface StoreAction {
        void run() throws ReportStoreException;
    }
}
