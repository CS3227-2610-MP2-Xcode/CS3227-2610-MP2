package io.github.cs32272610mp2xcode.finderskeepers.matching.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilePossibleMatchRepositoryFormatTest {
    private static final String A = "10000000-0000-0000-0000-000000000001";

    private static final String B = "20000000-0000-0000-0000-000000000002";

    private static final String LETTER_ID = "aaaaaaaa-0000-0000-0000-000000000003";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void rejectsEveryMalformedVersionOneShape() throws Exception {
        List<String> invalid = List.of(
                "",
                "   ",
                "WRONG 1\n",
                "FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS one\n",
                "FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS 1\n\n",
                "FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS 1\n" + A + "  " + B + "\n",
                "FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS 1\n"
                        + LETTER_ID.toUpperCase() + " " + B + "\n",
                "FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS 1\n" + A + " " + A + "\n",
                "FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS 1\n" + A + " " + B + "\n"
                        + B + " " + A + "\n");

        for (int index = 0; index < invalid.size(); index++) {
            Path store = temporaryDirectory.resolve("invalid-" + index + ".txt");
            Files.writeString(store, invalid.get(index), StandardCharsets.UTF_8);

            assertReason(PossibleMatchStoreException.Reason.CORRUPT_STORE,
                    new FilePossibleMatchRepository(store));
        }
    }

    @Test
    void distinguishesUnsupportedNumericVersion() throws Exception {
        Path store = temporaryDirectory.resolve("unsupported.txt");
        Files.writeString(store, "FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS 2\n",
                StandardCharsets.UTF_8);

        assertReason(PossibleMatchStoreException.Reason.UNSUPPORTED_VERSION,
                new FilePossibleMatchRepository(store));
    }

    @Test
    void invalidUtf8AndBareCarriageReturnsRemainUntouchedAndFailSafely() throws Exception {
        byte[][] invalidDocuments = {
            {(byte) 0xc3, 0x28},
            ("FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS 1\r" + A + " " + B + "\n")
                    .getBytes(StandardCharsets.UTF_8)
        };

        for (int index = 0; index < invalidDocuments.length; index++) {
            Path store = temporaryDirectory.resolve("invalid-bytes-" + index + ".txt");
            byte[] original = invalidDocuments[index];
            Files.write(store, original);
            FilePossibleMatchRepository repository = new FilePossibleMatchRepository(store);

            PossibleMatchStoreException failure = assertThrows(
                    PossibleMatchStoreException.class, repository::loadAll);

            assertEquals(PossibleMatchStoreException.Reason.CORRUPT_STORE, failure.reason());
            assertFalse(failure.getMessage().contains(store.toString()));
            assertArrayEquals(original, Files.readAllBytes(store));
        }
    }

    private static void assertReason(PossibleMatchStoreException.Reason reason,
            PossibleMatchRepository repository) {
        PossibleMatchStoreException failure = assertThrows(
                PossibleMatchStoreException.class, repository::loadAll);
        assertEquals(reason, failure.reason());
    }
}
