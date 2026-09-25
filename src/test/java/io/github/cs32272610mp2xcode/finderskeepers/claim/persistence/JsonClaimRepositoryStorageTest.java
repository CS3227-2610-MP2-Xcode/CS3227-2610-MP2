package io.github.cs32272610mp2xcode.finderskeepers.claim.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JsonClaimRepositoryStorageTest {
    private static final Instant TIME = Instant.parse("2026-09-22T01:02:03.456Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void realInputBoundaryAcceptsInclusiveLimitAndRejectsOneByteOver()
            throws Exception {
        Path source = temporaryDirectory.resolve("source.json");
        new JsonClaimRepository(source).submit(pending());
        byte[] canonical = Files.readAllBytes(source);
        byte[] exact = new byte[JsonClaimRepository.MAX_STORE_BYTES];
        Arrays.fill(exact, (byte) ' ');
        System.arraycopy(canonical, 0, exact, 0, canonical.length);
        Path store = temporaryDirectory.resolve("boundary.json");
        Files.write(store, exact);

        assertEquals(1, new JsonClaimRepository(store).loadAll().size());

        Files.write(store, Arrays.copyOf(exact, exact.length + 1));
        ClaimStoreException failure = assertThrows(ClaimStoreException.class,
                () -> new JsonClaimRepository(store).loadAll());
        assertEquals(ClaimStoreException.Reason.CORRUPT_STORE, failure.reason());
        assertEquals(JsonClaimRepository.MAX_STORE_BYTES + 1, Files.size(store));
    }

    @ParameterizedTest(name = "pre-commit storage fault {0}")
    @MethodSource("faultStages")
    void everyPreCommitFaultPreservesExistingOrAbsentTarget(FaultStage stage)
            throws Exception {
        byte[] committed = committedBytes();
        ScriptedFiles existing = new ScriptedFiles(Optional.of(committed), stage);
        JsonClaimRepository existingRepository = new JsonClaimRepository(
                temporaryDirectory.resolve("existing-" + stage + ".json"), existing,
                JsonClaimRepository.MAX_STORE_BYTES);

        ClaimStoreException existingFailure = assertThrows(ClaimStoreException.class,
                () -> existingRepository.withdraw(pending().claimId(), "student-1",
                        TIME.plusSeconds(1)));

        assertEquals(ClaimStoreException.Reason.WRITE_FAILURE, existingFailure.reason());
        assertTrue(Arrays.equals(committed, existing.current.orElseThrow()));
        assertTrue(existing.primaryFailureObserved);
        assertEquals(stage == FaultStage.CLEANUP, existing.orphanTemporary);

        ScriptedFiles absent = new ScriptedFiles(Optional.empty(), stage);
        JsonClaimRepository absentRepository = new JsonClaimRepository(
                temporaryDirectory.resolve("absent-" + stage + ".json"), absent,
                JsonClaimRepository.MAX_STORE_BYTES);
        ClaimStoreException absentFailure = assertThrows(ClaimStoreException.class,
                () -> absentRepository.submit(pending()));
        assertEquals(ClaimStoreException.Reason.WRITE_FAILURE, absentFailure.reason());
        assertTrue(absent.current.isEmpty());
    }

    @Test
    void orphanSiblingTemporaryFileIsNeverPromoted() throws Exception {
        Path target = temporaryDirectory.resolve("claims.json");
        Path orphan = temporaryDirectory.resolve(".claim-store-synthetic.tmp");
        Files.writeString(orphan, "synthetic orphan");

        assertTrue(new JsonClaimRepository(target).loadAll().isEmpty());
        assertFalse(Files.exists(target));
        assertTrue(Files.exists(orphan));
    }

    private byte[] committedBytes() throws Exception {
        Path store = temporaryDirectory.resolve("committed.json");
        new JsonClaimRepository(store).submit(pending());
        return Files.readAllBytes(store);
    }

    private static Stream<FaultStage> faultStages() {
        return Stream.of(FaultStage.values());
    }

    private static Claim pending() {
        return Claim.createPending(ClaimId.of(uuid(1)), "student-1", uuid(101),
                uuid(201), "Synthetic evidence.", TIME);
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }

    private enum FaultStage {
        PARENT_CREATION,
        TEMPORARY_CREATION,
        PARTIAL_WRITE,
        FORCE,
        CLOSE,
        UNSUPPORTED_ATOMIC_MOVE,
        ATOMIC_MOVE,
        CLEANUP
    }

    private static final class ScriptedFiles implements ClaimStoreFiles {
        private Optional<byte[]> current;

        private final FaultStage stage;

        private boolean primaryFailureObserved;

        private boolean orphanTemporary;

        ScriptedFiles(Optional<byte[]> initial, FaultStage failureStage) {
            current = initial.map(byte[]::clone);
            stage = failureStage;
        }

        @Override
        public Optional<byte[]> readBounded(Path target, int maximumBytes) {
            return current.map(byte[]::clone);
        }

        @Override
        public void replaceAtomically(Path target, byte[] completeDocument)
                throws StoreFileFailure {
            primaryFailureObserved = true;
            orphanTemporary = stage == FaultStage.CLEANUP;
            throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
        }
    }
}
