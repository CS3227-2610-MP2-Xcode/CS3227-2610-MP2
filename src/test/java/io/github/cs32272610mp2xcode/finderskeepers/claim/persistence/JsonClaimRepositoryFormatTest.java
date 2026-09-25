package io.github.cs32272610mp2xcode.finderskeepers.claim.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonClaimRepositoryFormatTest {
    private static final String CANONICAL_DOCUMENT = """
            {
              "schemaVersion": 1,
              "claims": [
                {
                  "claimId": "00000000-0000-0000-0000-000000000001",
                  "claimantUserId": "synthetic-student-1",
                  "lostReportId": "10000000-0000-0000-0000-000000000001",
                  "foundReportId": "20000000-0000-0000-0000-000000000001",
                  "ownershipEvidence": "Synthetic identifying detail.",
                  "status": "PENDING_REVIEW",
                  "submittedAt": "2026-09-22T01:02:03.456Z",
                  "terminalAt": null,
                  "decisionReason": null
                }
              ]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    @Test
    void literalFixtureLoadsAndMutationUsesExactCanonicalBytes() throws Exception {
        Path store = temporaryDirectory.resolve("claims.json");
        Files.writeString(store, CANONICAL_DOCUMENT, StandardCharsets.UTF_8);
        JsonClaimRepository repository = new JsonClaimRepository(store);

        assertEquals(List.of(pending()), repository.loadAll());
        repository.withdraw(pending().claimId(), "synthetic-student-1",
                Instant.parse("2026-09-22T02:02:03.456Z"));

        String expected = CANONICAL_DOCUMENT
                .replace("\"status\": \"PENDING_REVIEW\"", "\"status\": \"WITHDRAWN\"")
                .replace("\"terminalAt\": null",
                        "\"terminalAt\": \"2026-09-22T02:02:03.456Z\"");
        assertEquals(expected, Files.readString(store, StandardCharsets.UTF_8));
    }

    @Test
    void validNoncanonicalLayoutIsNotRewrittenByRead() throws Exception {
        Path store = temporaryDirectory.resolve("claims.json");
        String compact = CANONICAL_DOCUMENT.replaceAll("[\\r\\n ]+", " ");
        Files.writeString(store, compact, StandardCharsets.UTF_8);

        assertEquals(List.of(pending()), new JsonClaimRepository(store).loadAll());
        assertEquals(compact, Files.readString(store, StandardCharsets.UTF_8));
    }

    @ParameterizedTest(name = "invalid document partition {index}")
    @MethodSource("invalidDocuments")
    void invalidDocumentsFailWholeStoreWithoutChangingBytes(String document,
            ClaimStoreException.Reason reason) throws Exception {
        Path store = temporaryDirectory.resolve("claims.json");
        Files.writeString(store, document, StandardCharsets.UTF_8);
        byte[] before = Files.readAllBytes(store);

        ClaimStoreException failure = assertThrows(ClaimStoreException.class,
                () -> new JsonClaimRepository(store).loadAll());

        assertEquals(reason, failure.reason());
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(store)));
        assertTrue(!failure.getMessage().contains(store.toString()));
    }

    @Test
    void invalidUtf8AndBomFailWithoutChangingBytes() throws Exception {
        assertInvalidBytes(new byte[] {(byte) 0xc3, 0x28});
        byte[] canonical = CANONICAL_DOCUMENT.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[canonical.length + 3];
        withBom[0] = (byte) 0xef;
        withBom[1] = (byte) 0xbb;
        withBom[2] = (byte) 0xbf;
        System.arraycopy(canonical, 0, withBom, 3, canonical.length);
        assertInvalidBytes(withBom);
    }

    private void assertInvalidBytes(byte[] bytes) throws Exception {
        Path store = temporaryDirectory.resolve("invalid-" + bytes.length + ".json");
        Files.write(store, bytes);

        ClaimStoreException failure = assertThrows(ClaimStoreException.class,
                () -> new JsonClaimRepository(store).loadAll());

        assertEquals(ClaimStoreException.Reason.CORRUPT_STORE, failure.reason());
        assertTrue(java.util.Arrays.equals(bytes, Files.readAllBytes(store)));
    }

    private static Stream<Arguments> invalidDocuments() {
        return Stream.of(
                Arguments.of("", ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of("{}", ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace("\"schemaVersion\": 1",
                        "\"schemaVersion\": 2"),
                        ClaimStoreException.Reason.UNSUPPORTED_VERSION),
                Arguments.of(CANONICAL_DOCUMENT.replace("\"schemaVersion\": 1",
                        "\"schemaVersion\": 01"),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace("\"schemaVersion\": 1",
                        "\"schemaVersion\": ١"),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace("\"schemaVersion\": 1",
                        "\"schemaVersion\": \"1\""),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of("\u00a0" + CANONICAL_DOCUMENT,
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace("\"claims\": [",
                        "\"unknown\": 1, \"claims\": ["),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace("\"claimId\":",
                        "\"claimId\": \"duplicate\", \"claimId\":"),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace("PENDING_REVIEW", "UNKNOWN"),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace(
                        "\"ownershipEvidence\": \"Synthetic identifying detail.\"",
                        "\"ownershipEvidence\": null"),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace(
                        "Synthetic identifying detail.", "Synthetic \\q detail."),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace(
                        "Synthetic identifying detail.", "Synthetic \\uD800 detail."),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace(
                        "00000000-0000-0000-0000-000000000001",
                        "AAAAAAAA-0000-0000-0000-000000000001"),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace("\"terminalAt\": null",
                        "\"terminalAt\": \"2026-09-22T02:02:03.456Z\""),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace("\"decisionReason\": null",
                        "\"decisionReason\": \"Synthetic reason.\""),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT.replace(".456Z", "Z"),
                        ClaimStoreException.Reason.CORRUPT_STORE),
                Arguments.of(CANONICAL_DOCUMENT + "x",
                        ClaimStoreException.Reason.CORRUPT_STORE));
    }

    private static Claim pending() {
        return Claim.createPending(
                ClaimId.of(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                "synthetic-student-1",
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                "Synthetic identifying detail.",
                Instant.parse("2026-09-22T01:02:03.456Z"));
    }
}
