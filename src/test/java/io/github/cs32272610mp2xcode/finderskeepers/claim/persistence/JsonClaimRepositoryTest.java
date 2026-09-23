package io.github.cs32272610mp2xcode.finderskeepers.claim.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository.SubmissionOutcome;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository.TerminalOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonClaimRepositoryTest {
    private static final Instant BASE_TIME = Instant.parse("2026-09-22T01:02:03.456Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void missingStoreLoadsEmptyWithoutCreatingFilesystemEntries() throws ClaimStoreException {
        Path parent = temporaryDirectory.resolve("missing");
        Path store = parent.resolve("claims.json");

        assertTrue(new JsonClaimRepository(store).loadAll().isEmpty());
        assertFalse(Files.exists(parent));
        assertFalse(Files.exists(store));
    }

    @Test
    void pendingAndTerminalClaimsRoundTripInStableOrder() throws ClaimStoreException {
        Path store = temporaryDirectory.resolve("claims.json");
        ClaimRepository writer = new JsonClaimRepository(store);
        Claim first = pending(1, "student-1", 101, 201, 0);
        Claim second = pending(2, "student-2", 102, 202, 10);

        assertEquals(SubmissionOutcome.CREATED, writer.submit(first).outcome());
        assertEquals(SubmissionOutcome.CREATED, writer.submit(second).outcome());
        Claim approved = writer.approve(first.claimId(), Optional.of("Synthetic approval."),
                BASE_TIME.plusSeconds(20)).claim().orElseThrow();
        Claim rejected = writer.reject(second.claimId(), "Synthetic rejection.",
                BASE_TIME.plusSeconds(30)).claim().orElseThrow();

        assertEquals(List.of(approved, rejected), new JsonClaimRepository(store).loadAll());
    }

    @Test
    void submissionOutcomesDoNotWriteRejectedCandidates() throws Exception {
        Path store = temporaryDirectory.resolve("claims.json");
        ClaimRepository repository = new JsonClaimRepository(store);
        Claim original = pending(1, "student-1", 101, 201, 0);
        repository.submit(original);
        byte[] before = Files.readAllBytes(store);

        assertEquals(SubmissionOutcome.ID_COLLISION,
                repository.submit(pending(1, "student-2", 102, 202, 10)).outcome());
        assertEquals(SubmissionOutcome.OWN_ACTIVE_CLAIM,
                repository.submit(pending(2, "student-1", 101, 202, 10)).outcome());
        assertEquals(SubmissionOutcome.BLOCKED,
                repository.submit(pending(3, "student-2", 102, 201, 10)).outcome());
        assertTrue(Arrays.equals(before, Files.readAllBytes(store)));
    }

    @Test
    void terminalOutcomesEnforceOwnerAndFirstTerminalWins() throws ClaimStoreException {
        ClaimRepository repository = new JsonClaimRepository(
                temporaryDirectory.resolve("claims.json"));
        Claim pending = pending(1, "student-1", 101, 201, 0);
        repository.submit(pending);

        assertEquals(TerminalOutcome.NOT_AUTHORIZED,
                repository.withdraw(pending.claimId(), "student-2",
                        BASE_TIME.plusSeconds(10)).outcome());
        assertEquals(TerminalOutcome.NOT_FOUND,
                repository.approve(ClaimId.of(uuid(99)), Optional.empty(),
                        BASE_TIME.plusSeconds(10)).outcome());
        assertEquals(TerminalOutcome.CHANGED,
                repository.withdraw(pending.claimId(), "student-1",
                        BASE_TIME.plusSeconds(10)).outcome());
        ClaimRepository.TerminalResult stale = repository.reject(
                pending.claimId(), "Synthetic rejection.", BASE_TIME.plusSeconds(20));
        assertEquals(TerminalOutcome.ALREADY_TERMINAL, stale.outcome());
        assertEquals(ClaimStatus.WITHDRAWN, stale.claim().orElseThrow().status());
    }

    @Test
    void failedWritePreservesExistingCommittedBytes() throws Exception {
        Path realStore = temporaryDirectory.resolve("source.json");
        Claim original = pending(1, "student-1", 101, 201, 0);
        new JsonClaimRepository(realStore).submit(original);
        byte[] committed = Files.readAllBytes(realStore);
        FailingWriteFiles files = new FailingWriteFiles(committed);
        ClaimRepository repository = new JsonClaimRepository(
                temporaryDirectory.resolve("target.json"), files,
                JsonClaimRepository.MAX_STORE_BYTES);

        ClaimStoreException failure = assertThrows(ClaimStoreException.class,
                () -> repository.withdraw(original.claimId(), "student-1",
                        BASE_TIME.plusSeconds(10)));

        assertEquals(ClaimStoreException.Reason.WRITE_FAILURE, failure.reason());
        assertTrue(Arrays.equals(committed, files.current));
    }

    @Test
    void candidateOverConfiguredBoundDoesNotPublish() {
        Path store = temporaryDirectory.resolve("claims.json");
        ClaimRepository repository = new JsonClaimRepository(
                store, new NioClaimStoreFiles(), 128);

        ClaimStoreException failure = assertThrows(ClaimStoreException.class,
                () -> repository.submit(pending(1, "student-1", 101, 201, 0)));

        assertEquals(ClaimStoreException.Reason.RESULT_TOO_LARGE, failure.reason());
        assertFalse(Files.exists(store));
    }

    @Test
    void terminalClaimCannotBeSubmittedOrCreateStorage() {
        Path store = temporaryDirectory.resolve("claims.json");
        Claim terminal = pending(1, "student-1", 101, 201, 0)
                .withdraw(BASE_TIME.plusSeconds(1));
        ClaimRepository repository = new JsonClaimRepository(store);

        assertThrows(IllegalArgumentException.class, () -> repository.submit(terminal));
        assertFalse(Files.exists(store));
    }

    @Test
    void accessFailureOnReadMapsToPrivacySafeReadFailure() {
        ClaimRepository repository = new JsonClaimRepository(
                temporaryDirectory.resolve("claims.json"), new FailingReadFiles(),
                JsonClaimRepository.MAX_STORE_BYTES);

        ClaimStoreException failure = assertThrows(
                ClaimStoreException.class, repository::loadAll);

        assertEquals(ClaimStoreException.Reason.READ_FAILURE, failure.reason());
        assertFalse(failure.getMessage().contains(temporaryDirectory.toString()));
    }

    private static Claim pending(int id, String claimant, long lost, long found,
            long seconds) {
        return Claim.createPending(ClaimId.of(uuid(id)), claimant, uuid(lost), uuid(found),
                "Synthetic evidence.", BASE_TIME.plusSeconds(seconds));
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }

    private static final class FailingWriteFiles implements ClaimStoreFiles {
        private byte[] current;

        FailingWriteFiles(byte[] initial) {
            current = initial.clone();
        }

        @Override
        public Optional<byte[]> readBounded(Path target, int maximumBytes) {
            return Optional.of(current.clone());
        }

        @Override
        public void replaceAtomically(Path target, byte[] completeDocument)
                throws StoreFileFailure {
            throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
        }
    }

    private static final class FailingReadFiles implements ClaimStoreFiles {
        @Override
        public Optional<byte[]> readBounded(Path target, int maximumBytes)
                throws StoreFileFailure {
            throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
        }

        @Override
        public void replaceAtomically(Path target, byte[] completeDocument) {
            throw new AssertionError("Read failure must prevent mutation");
        }
    }
}
