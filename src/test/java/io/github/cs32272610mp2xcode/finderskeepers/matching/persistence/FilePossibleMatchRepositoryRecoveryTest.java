package io.github.cs32272610mp2xcode.finderskeepers.matching.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPair;

class FilePossibleMatchRepositoryRecoveryTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void mapsReadAndWriteFailuresWithoutFalseSuccess() throws Exception {
        ScriptedFiles files = new ScriptedFiles();
        FilePossibleMatchRepository repository = new FilePossibleMatchRepository(
                temporaryDirectory.resolve("links.txt"), files, 1024);
        files.failRead = true;

        assertReason(PossibleMatchStoreException.Reason.READ_FAILURE, repository::loadAll);

        files.failRead = false;
        files.failWrite = true;
        assertReason(PossibleMatchStoreException.Reason.WRITE_FAILURE,
                () -> repository.link(pair()));
        assertFalse(files.committed);
    }

    @Test
    void oversizedReadIsCorruptAndOversizedResultDoesNotWrite() throws Exception {
        ScriptedFiles overRead = new ScriptedFiles();
        overRead.overLimitRead = true;
        FilePossibleMatchRepository readRepository = new FilePossibleMatchRepository(
                temporaryDirectory.resolve("read.txt"), overRead, 100);
        assertReason(PossibleMatchStoreException.Reason.CORRUPT_STORE,
                readRepository::loadAll);

        ScriptedFiles overWrite = new ScriptedFiles();
        FilePossibleMatchRepository writeRepository = new FilePossibleMatchRepository(
                temporaryDirectory.resolve("write.txt"), overWrite, 100);
        assertReason(PossibleMatchStoreException.Reason.RESULT_TOO_LARGE,
                () -> writeRepository.link(pair()));
        assertFalse(overWrite.committed);
    }

    @Test
    void publicFailuresHaveFixedDiagnosticsWithoutCauses() {
        for (PossibleMatchStoreException.Reason reason
                : PossibleMatchStoreException.Reason.values()) {
            PossibleMatchStoreException failure = new PossibleMatchStoreException(reason);
            assertEquals(reason, failure.reason());
            assertEquals(null, failure.getCause());
            assertEquals(0, failure.getSuppressed().length);
        }
    }

    private static PossibleMatchPair pair() {
        return PossibleMatchPair.of(new UUID(0L, 1L), new UUID(0L, 2L));
    }

    private static void assertReason(PossibleMatchStoreException.Reason reason,
            StoreAction action) {
        PossibleMatchStoreException failure = assertThrows(
                PossibleMatchStoreException.class, action::run);
        assertEquals(reason, failure.reason());
    }

    private static final class ScriptedFiles implements PossibleMatchStoreFiles {
        private boolean failRead;

        private boolean overLimitRead;

        private boolean failWrite;

        private boolean committed;

        @Override
        public Optional<byte[]> readBounded(Path target, int maximumBytes)
                throws StoreFileFailure {
            if (overLimitRead) {
                throw new StoreFileFailure(StoreFileFailure.Kind.OVER_LIMIT);
            }
            if (failRead) {
                throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
            }
            return Optional.empty();
        }

        @Override
        public void replaceAtomically(Path target, byte[] completeDocument)
                throws StoreFileFailure {
            if (failWrite) {
                throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
            }
            committed = true;
        }
    }

    @FunctionalInterface
    private interface StoreAction {
        void run() throws PossibleMatchStoreException;
    }
}
