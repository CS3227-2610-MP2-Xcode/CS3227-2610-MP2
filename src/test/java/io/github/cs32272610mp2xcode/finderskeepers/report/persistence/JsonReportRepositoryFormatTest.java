package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

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

class JsonReportRepositoryFormatTest {
    private static final String CANONICAL_DOCUMENT = """
            {
              "schemaVersion": 1,
              "reports": [
                {
                  "reportId": "00000000-0000-0000-0000-000000000001",
                  "reporterId": "reporter-1",
                  "reportType": "LOST",
                  "itemName": "Item 1",
                  "category": "OTHER",
                  "location": "Location 1",
                  "occurrenceDate": "2026-09-11",
                  "publicDescription": "Public description 1",
                  "privateIdentifyingDetail": "Private detail 1",
                  "status": "SUBMITTED",
                  "createdAt": "2026-09-20T01:02:03.456Z"
                }
              ]
            }
            """;

    private static final String[] REQUIRED_REPORT_MEMBERS = {
        "reportId",
        "reporterId",
        "reportType",
        "itemName",
        "category",
        "location",
        "occurrenceDate",
        "publicDescription",
        "privateIdentifyingDetail",
        "status",
        "createdAt"
    };

    @TempDir
    private Path temporaryDirectory;

    @Test
    void versionOneLiteralFixturesLoadAndSuccessfulWritesUseCanonicalBytes()
            throws IOException, ReportStoreException {
        Path store = temporaryDirectory.resolve("reports.json");
        Files.writeString(store, CANONICAL_DOCUMENT, StandardCharsets.UTF_8);
        ItemReport expected = report(1);

        List<ItemReport> loaded = new JsonReportRepository(store).loadAll();

        assertTrue(loaded.size() == 1, "The independent version-one fixture must contain one report");
        assertTrue(loaded.getFirst().equals(expected), "The independent fixture must reconstruct exactly");

        Path emittedStore = temporaryDirectory.resolve("emitted.json");
        ReportRepository writer = new JsonReportRepository(emittedStore);
        writer.insert(expected);
        assertTrue(Arrays.equals(
                CANONICAL_DOCUMENT.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(emittedStore)),
                "A successful insertion must emit the canonical version-one bytes");

        Path alternateStore = temporaryDirectory.resolve("alternate.json");
        Files.writeString(alternateStore, " \n{\"reports\":[],\"schemaVersion\":1}\r\n", StandardCharsets.UTF_8);
        assertTrue(new JsonReportRepository(alternateStore).loadAll().isEmpty(),
                "Permitted whitespace and member order must not change meaning");

        Path reorderedReportStore = temporaryDirectory.resolve("reordered-report.json");
        String reorderedReportDocument = """
                {
                  "reports": [
                    {
                      "createdAt": "2026-09-20T01:02:03.456Z",
                      "status": "SUBMITTED",
                      "privateIdentifyingDetail": "Private detail 1",
                      "publicDescription": "Public description 1",
                      "occurrenceDate": "2026-09-11",
                      "location": "Location 1",
                      "category": "OTHER",
                      "itemName": "Item 1",
                      "reportType": "LOST",
                      "reporterId": "reporter-1",
                      "reportId": "00000000-0000-0000-0000-000000000001"
                    }
                  ],
                  "schemaVersion": 1
                }
                """;
        Files.writeString(reorderedReportStore, reorderedReportDocument, StandardCharsets.UTF_8);
        assertTrue(new JsonReportRepository(reorderedReportStore).loadAll().equals(List.of(expected)),
                "Permitted report-member order must not change reconstruction");
    }

    @Test
    void canonicalEnumAndTemporalRepresentationsRoundTripExactly() throws IOException, ReportStoreException {
        Path store = temporaryDirectory.resolve("reports.json");
        String escaped = CANONICAL_DOCUMENT
                .replace("\"reportType\": \"LOST\"", "\"reportType\": \"FOUND\"")
                .replace("\"status\": \"SUBMITTED\"", "\"status\": \"UNDER_REVIEW\"")
                .replace("\"itemName\": \"Item 1\"", "\"itemName\": \"Item \\/\\uD83D\\uDE00\"");
        Files.writeString(store, escaped, StandardCharsets.UTF_8);

        ItemReport loaded = new JsonReportRepository(store).loadAll().getFirst();

        assertTrue(loaded.reportType() == ReportType.FOUND, "The exact report-type token must load");
        assertTrue(loaded.status() == ReportStatus.UNDER_REVIEW, "The exact status token must load");
        assertTrue(loaded.itemName().equals("Item /😀"), "Permitted escapes must reconstruct exactly");
        assertTrue(loaded.occurrenceDate().equals(LocalDate.of(2026, 9, 11)),
                "The canonical occurrence date must reconstruct exactly");
        assertTrue(loaded.createdAt().equals(Instant.parse("2026-09-20T01:02:03.456Z")),
                "The canonical creation instant must reconstruct exactly");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidDocuments")
    void invalidStoredDocumentBlocksEveryOperationAndPreservesExactBytes(String caseId, byte[] invalidBytes)
            throws IOException {
        assertInvalidStoreBlocksEveryOperation(caseId, invalidBytes);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequiredReportMembers")
    void everyRequiredReportMemberRejectsMissingNullAndWrongTypes(String caseId, byte[] invalidBytes)
            throws IOException {
        assertInvalidStoreBlocksEveryOperation(caseId, invalidBytes);
    }

    private void assertInvalidStoreBlocksEveryOperation(String caseId, byte[] invalidBytes)
            throws IOException {
        Path store = temporaryDirectory.resolve(caseId + ".json");
        Files.write(store, invalidBytes);
        ReportRepository repository = new JsonReportRepository(store);
        byte[] before = Files.readAllBytes(store);

        assertCorrupt(() -> repository.loadAll());
        assertTrue(Arrays.equals(before, Files.readAllBytes(store)),
                "A rejected load must preserve the exact invalid bytes");
        assertCorrupt(() -> repository.insert(report(2)));
        assertTrue(Arrays.equals(before, Files.readAllBytes(store)),
                "A rejected insertion must preserve the exact invalid bytes");
        assertCorrupt(() -> repository.replace(report(1).reportId(), report(1)));
        assertTrue(Arrays.equals(before, Files.readAllBytes(store)),
                "A rejected replacement must preserve the exact invalid bytes");
    }

    private static Stream<Arguments> invalidDocuments() {
        String valid = CANONICAL_DOCUMENT;
        String reportObject = valid.substring(valid.indexOf("    {"), valid.indexOf("\n    }\n") + 6);
        String duplicateReport = valid.replace("\n  ]", ",\n" + reportObject + "\n  ]");
        String nonAsciiHexEscape = valid.replace("Item 1", "bad\\" + "u００４１");
        return Stream.of(
                Arguments.of("C01", new byte[0]),
                Arguments.of("C02", "{".getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C03", new byte[] {(byte) 0xc3, 0x28}),
                Arguments.of("C04", ("\ufeff" + valid).getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C05", (valid + "false").getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C06", valid.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C07", valid.replace("\"schemaVersion\": 1", "\"schemaVersion\": 1.0")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C08", valid.replace("\"schemaVersion\": 1,", "\"schemaVersion\": 1,\n"
                        + "  \"schemaVersion\": 1,").getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C09", valid.replace("\"reports\": [", "\"unknown\": \"x\",\n  \"reports\": [")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C10", valid.replace("  \"schemaVersion\": 1,\n", "")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C11", "{\"schemaVersion\":1,\"reports\":null}"
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C12", valid.replace("\"reportType\": \"LOST\"", "\"reportType\": \"lost\"")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C13", valid.replace("\"occurrenceDate\": \"2026-09-11\"",
                        "\"occurrenceDate\": \"2026-9-11\"").getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C14", valid.replace("2026-09-20T01:02:03.456Z", "2026-09-20T01:02:03Z")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C15", valid.replace("00000000-0000-0000-0000-000000000001",
                        "00000000-0000-0000-0000-00000000001A").getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C16", valid.replace("\"category\": \"OTHER\"",
                        "\"category\": \"OTHER\",\n      \"category\": \"OTHER\"")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C17", valid.replace("\"location\": \"Location 1\",\n", "")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C18", valid.replace("\"location\": \"Location 1\"",
                        "\"location\": null").getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C19", valid.replace("\"itemName\": \"Item 1\"",
                        "\"itemName\": \"bad\\x\"").getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C20", valid.replace("\"itemName\": \"Item 1\"",
                        "\"itemName\": \"bad\\uD800\"").getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C21", valid.replace("\"reporterId\": \"reporter-1\"",
                        "\"\\u0072eporterId\": \"other\",\n      \"reporterId\": \"reporter-1\"")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C22", duplicateReport.getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C23", "[]".getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C24", "{\"schemaVersion\":1,\"reports\":{}}"
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C25", valid.replace("\"schemaVersion\": 1", "\"schemaVersion\": \"1\"")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C26", valid.replace("\"itemName\": \"Item 1\"", "\"itemName\": 1")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C27", valid.replace("\"itemName\": \"Item 1\"",
                        "\"unexpected\": \"x\",\n      \"itemName\": \"Item 1\"")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C28", valid.replace("Item 1", "raw-\u0001-control")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C29", valid.replace(
                        "\"createdAt\": \"2026-09-20T01:02:03.456Z\"\n    }",
                        "\"createdAt\": \"2026-09-20T01:02:03.456Z\",\n    }")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C30", ("\u00a0" + valid).getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C31", valid.replace("\"schemaVersion\": 1", "/*comment*/\"schemaVersion\": 1")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C32", valid.replace("\"itemName\": \"Item 1\"", "\"itemName\": {}")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C33", "{\"schemaVersion\":1}".getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C34", valid.replace("\"schemaVersion\": 1", "\"schemaVersion\": 0")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C35", valid.replace("\"schemaVersion\": 1,",
                        "\"\\u0073chemaVersion\": 1,\n  \"schemaVersion\": 1,")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C36", nonAsciiHexEscape.getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C37", valid.replace("2026-09-20T01:02:03.456Z",
                        "2026-09-20T01:02:03.456+00:00").getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C38", valid.replace("2026-09-20T01:02:03.456Z",
                        "2026-09-20T01:02:03.4Z").getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C39", valid.replace("2026-09-20T01:02:03.456Z",
                        "2026-09-20T01:02:03.456000Z").getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C40", valid.replace("2026-09-20T01:02:03.456Z",
                        "2026-09-20T01:02:03.456000000Z").getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C41", valid.replace("\"schemaVersion\": 1", "\"schemaVersion\": 1e0")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C42", valid.replace("\"reporterId\": \"reporter-1\"", "\"reporterId\": \"\"")
                        .getBytes(StandardCharsets.UTF_8)),
                Arguments.of("C43", valid.replace("\"itemName\": \"Item 1\"",
                        "\"itemName\": \"" + "x".repeat(101) + "\"").getBytes(StandardCharsets.UTF_8)));
    }

    private static Stream<Arguments> invalidRequiredReportMembers() {
        Stream.Builder<Arguments> cases = Stream.builder();
        int caseNumber = 1;
        for (String member : REQUIRED_REPORT_MEMBERS) {
            cases.add(Arguments.of(
                    String.format("M%02d-missing-%s", caseNumber++, member),
                    removeRequiredMember(CANONICAL_DOCUMENT, member).getBytes(StandardCharsets.UTF_8)));
            cases.add(Arguments.of(
                    String.format("M%02d-null-%s", caseNumber++, member),
                    replaceRequiredMemberValue(CANONICAL_DOCUMENT, member, "null")
                            .getBytes(StandardCharsets.UTF_8)));
            cases.add(Arguments.of(
                    String.format("M%02d-wrong-type-%s", caseNumber++, member),
                    replaceRequiredMemberValue(CANONICAL_DOCUMENT, member, "1")
                            .getBytes(StandardCharsets.UTF_8)));
        }
        return cases.build();
    }

    private static String removeRequiredMember(String document, String member) {
        String prefix = "      \"" + member + "\": ";
        int memberStart = document.indexOf(prefix);
        int lineEnd = document.indexOf('\n', memberStart);
        String withoutMember = document.substring(0, memberStart) + document.substring(lineEnd + 1);
        if (member.equals("createdAt")) {
            return withoutMember.replace("      \"status\": \"SUBMITTED\",\n",
                    "      \"status\": \"SUBMITTED\"\n");
        }
        return withoutMember;
    }

    private static String replaceRequiredMemberValue(String document, String member, String replacement) {
        String prefix = "      \"" + member + "\": ";
        int valueStart = document.indexOf(prefix) + prefix.length();
        int lineEnd = document.indexOf('\n', valueStart);
        int valueEnd = document.charAt(lineEnd - 1) == ',' ? lineEnd - 1 : lineEnd;
        return document.substring(0, valueStart) + replacement + document.substring(valueEnd);
    }

    private static void assertCorrupt(StoreAction action) {
        ReportStoreException failure = assertThrows(
                ReportStoreException.class, action::run, "An invalid store must block the complete operation");
        assertTrue(failure.reason() == ReportStoreException.Reason.CORRUPT_OR_UNSUPPORTED_STORE,
                "An invalid store must expose the corrupt-or-unsupported reason");
    }

    private static ItemReport report(int sequence) {
        return new ItemReport(
                new UUID(0L, sequence),
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

    @FunctionalInterface
    private interface StoreAction {
        void run() throws ReportStoreException;
    }
}
