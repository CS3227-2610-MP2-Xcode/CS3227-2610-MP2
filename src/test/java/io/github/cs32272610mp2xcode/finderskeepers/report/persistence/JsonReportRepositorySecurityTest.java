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
import java.util.Random;
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

class JsonReportRepositorySecurityTest {
    private static final Instant CREATED_AT = Instant.parse("2026-09-20T01:02:03.456Z");

    @TempDir
    private Path temporaryDirectory;

    @ParameterizedTest(name = "{0}")
    @MethodSource("attackCases")
    void fixedAttackCorpusRemainsOneValueInEveryCanonicalTextField(
            String caseId, int field, String attack) throws ReportStoreException, IOException {
        Path store = temporaryDirectory.resolve("case-" + caseId + ".json");
        ItemReport attacked = withTextField(report(), field, attack);
        ItemReport before = neighbor(2);
        ItemReport after = neighbor(3);
        ReportRepository repository = new JsonReportRepository(store);

        repository.insert(before);
        repository.insert(attacked);
        repository.insert(after);
        List<ItemReport> loaded = new JsonReportRepository(store).loadAll();

        assertTrue(loaded.size() == 3, "An attacked field must not create or remove reports");
        assertTrue(loaded.get(0).equals(before), "Data before an attacked field must remain exact");
        assertTrue(loaded.get(1).equals(attacked), "An attacked field must round-trip as ordinary data");
        assertTrue(loaded.get(2).equals(after), "Data after an attacked field must remain exact");
        assertTrue(loaded.get(1).reportId().equals(attacked.reportId()),
                "An attacked field must not change report identity");
        assertTrue(loaded.get(1).status() == attacked.status(),
                "An attacked field must not change report status");
        try (Stream<Path> entries = Files.list(temporaryDirectory)) {
            assertTrue(entries.toList().equals(List.of(store)),
                    "Report data must write only to the configured store path");
        }
    }

    @Test
    void deterministicallyGeneratedUnicodeRoundTripsInEveryCanonicalTextField()
            throws ReportStoreException {
        Random random = new Random(3_227_261_000L);

        for (int iteration = 0; iteration < 256; iteration++) {
            ItemReport current = ItemReport.restore(
                    new UUID(1L, iteration + 1L),
                    generatedUnicode(random, 16),
                    ReportType.LOST,
                    generatedUnicode(random, 16),
                    ItemCategory.OTHER,
                    generatedUnicode(random, 16),
                    LocalDate.of(2026, 9, 19),
                    generatedUnicode(random, 24),
                    generatedUnicode(random, 24),
                    ReportStatus.SUBMITTED,
                    CREATED_AT);
            Path store = temporaryDirectory.resolve("unicode-" + iteration + ".json");
            new JsonReportRepository(store).insert(current);
            List<ItemReport> loaded = new JsonReportRepository(store).loadAll();
            assertTrue(loaded.size() == 1, "Generated Unicode must preserve report count");
            assertTrue(loaded.getFirst().equals(current), "Generated Unicode must round-trip exactly");
        }
    }

    @Test
    void unencodableTextFailsBeforeInsertOrReplacementMutation() throws ReportStoreException, IOException {
        Path firstStore = temporaryDirectory.resolve("first.json");
        ReportRepository firstRepository = new JsonReportRepository(firstStore);
        ItemReport invalidHigh = withTextField(report(), 1, "invalid-\ud800");
        ReportStoreException firstFailure = assertThrows(
                ReportStoreException.class,
                () -> firstRepository.insert(invalidHigh),
                "An unpaired high surrogate must be rejected before first insertion");
        assertTrue(firstFailure.reason() == ReportStoreException.Reason.UNENCODABLE_OR_OVER_LIMIT_RESULT,
                "Unencodable insertion must expose its typed reason");
        assertFalse(Files.exists(firstStore), "Unencodable first insertion must leave the store absent");

        Path existingStore = temporaryDirectory.resolve("existing.json");
        ReportRepository existingRepository = new JsonReportRepository(existingStore);
        ItemReport original = report();
        existingRepository.insert(original);
        byte[] before = Files.readAllBytes(existingStore);
        ItemReport invalidLow = withTextField(original, 4, "invalid-\udc00");
        ReportStoreException replacementFailure = assertThrows(
                ReportStoreException.class,
                () -> existingRepository.replace(original.reportId(), invalidLow),
                "An unpaired low surrogate must be rejected before replacement");
        assertTrue(replacementFailure.reason() == ReportStoreException.Reason.UNENCODABLE_OR_OVER_LIMIT_RESULT,
                "Unencodable replacement must expose its typed reason");
        assertTrue(Arrays.equals(before, Files.readAllBytes(existingStore)),
                "Unencodable replacement must preserve the store byte-for-byte");
    }

    private static Stream<Arguments> attackCases() {
        List<String> attacks = List.of(
                "quote-\"-slash-\\-braces-{}-array-[]-comma-,-colon-:",
                "line-one\nline-two\ttab\rreturn\bback\fform",
                "control-\u0000-\u0001-\u001f",
                "\"}, {\"reportId\":\"00000000-0000-0000-0000-000000000099\"}",
                "..\\..\\other.json /tmp/other.json C:\\other.json",
                "combining-e\u0301-supplementary-😀-𐐷");
        Stream.Builder<Arguments> cases = Stream.builder();
        int caseNumber = 1;
        for (int field = 0; field < 5; field++) {
            for (String attack : attacks) {
                cases.add(Arguments.of(String.format("A%02d", caseNumber++), field, attack));
            }
        }
        return cases.build();
    }

    private static String generatedUnicode(Random random, int codePoints) {
        StringBuilder value = new StringBuilder();
        int[] fixed = {'A', 0x00e9, 0x0301, 0x4e2d, 0x1f600, 0x10437};
        for (int index = 0; index < codePoints; index++) {
            int codePoint;
            if (index < fixed.length) {
                codePoint = fixed[index];
            } else {
                int candidate;
                do {
                    candidate = random.nextInt(Character.MAX_CODE_POINT + 1);
                } while (!Character.isValidCodePoint(candidate)
                        || Character.isSurrogate((char) candidate)
                        || isNonCharacter(candidate));
                codePoint = candidate;
            }
            value.appendCodePoint(codePoint);
        }
        return value.toString();
    }

    private static boolean isNonCharacter(int codePoint) {
        return codePoint >= 0xfdd0 && codePoint <= 0xfdef || (codePoint & 0xffff) >= 0xfffe;
    }

    private static ItemReport report() {
        return ItemReport.restore(
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                "reporter-base",
                ReportType.LOST,
                "Item base",
                ItemCategory.OTHER,
                "Location base",
                LocalDate.of(2026, 9, 19),
                "Public description base",
                "Private detail base",
                ReportStatus.SUBMITTED,
                CREATED_AT);
    }

    private static ItemReport neighbor(long sequence) {
        return ItemReport.restore(
                new UUID(2L, sequence),
                "neighbor-reporter-" + sequence,
                ReportType.FOUND,
                "Neighbor item " + sequence,
                ItemCategory.OTHER,
                "Neighbor location " + sequence,
                LocalDate.of(2026, 9, 18),
                "Neighbor public description " + sequence,
                "Neighbor private detail " + sequence,
                ReportStatus.UNDER_REVIEW,
                CREATED_AT);
    }

    private static ItemReport withTextField(ItemReport source, int field, String value) {
        String[] text = {
            source.reporterId(),
            source.itemName(),
            source.location(),
            source.publicDescription(),
            source.privateIdentifyingDetail()
        };
        text[field] = value;
        return ItemReport.restore(
                source.reportId(),
                text[0],
                source.reportType(),
                text[1],
                source.category(),
                text[2],
                source.occurrenceDate(),
                text[3],
                text[4],
                source.status(),
                source.createdAt());
    }
}
