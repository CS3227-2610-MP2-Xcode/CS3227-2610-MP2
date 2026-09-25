package io.github.cs32272610mp2xcode.finderskeepers.matching.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPair;

class FilePossibleMatchRepositoryTest {
    private static final UUID A = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID B = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private static final UUID C = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void missingStoreIsEmptyWithoutCreatingStorage() throws Exception {
        Path store = temporaryDirectory.resolve("missing/links.txt");

        assertEquals(Set.of(), new FilePossibleMatchRepository(store).loadAll());
        assertFalse(Files.exists(store));
        assertFalse(Files.exists(store.getParent()));
    }

    @Test
    void linkAndUnlinkAreDurableSymmetricAndNonExclusive() throws Exception {
        Path store = temporaryDirectory.resolve("links.txt");
        PossibleMatchRepository repository = new FilePossibleMatchRepository(store);
        PossibleMatchPair first = PossibleMatchPair.of(A, B);
        PossibleMatchPair second = PossibleMatchPair.of(A, C);

        assertTrue(repository.link(first));
        assertFalse(repository.link(PossibleMatchPair.of(B, A)));
        assertTrue(repository.link(second));
        assertEquals(Set.of(first, second), new FilePossibleMatchRepository(store).loadAll());

        assertTrue(repository.unlink(PossibleMatchPair.of(B, A)));
        assertFalse(repository.unlink(first));
        assertEquals(Set.of(second), new FilePossibleMatchRepository(store).loadAll());
    }

    @Test
    void writesExactCanonicalBytesInPairOrder() throws Exception {
        Path store = temporaryDirectory.resolve("links.txt");
        PossibleMatchRepository repository = new FilePossibleMatchRepository(store);
        repository.link(PossibleMatchPair.of(A, C));
        repository.link(PossibleMatchPair.of(A, B));

        String expected = "FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS 1\n"
                + A + " " + B + "\n"
                + A + " " + C + "\n";
        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(store));
    }

    @Test
    void acceptsCrLfOptionalFinalNewlineAndCanonicalizesReverseInput() throws Exception {
        Path store = temporaryDirectory.resolve("links.txt");
        String stored = "FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS 1\r\n"
                + B + " " + A;
        Files.writeString(store, stored, StandardCharsets.UTF_8);

        assertEquals(Set.of(PossibleMatchPair.of(A, B)),
                new FilePossibleMatchRepository(store).loadAll());
    }
}
