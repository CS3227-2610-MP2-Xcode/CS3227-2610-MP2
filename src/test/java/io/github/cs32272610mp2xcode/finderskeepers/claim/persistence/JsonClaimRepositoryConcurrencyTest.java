package io.github.cs32272610mp2xcode.finderskeepers.claim.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository.SubmissionOutcome;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository.TerminalOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonClaimRepositoryConcurrencyTest {
    private static final Instant BASE_TIME = Instant.parse("2026-09-22T01:02:03.456Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void conflictingSubmissionsCommitAtMostOne() throws Exception {
        ClaimRepository repository = new JsonClaimRepository(
                temporaryDirectory.resolve("claims.json"));
        Claim first = pending(1, "student-1", 101, 201);
        Claim second = pending(2, "student-2", 102, 201);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<SubmissionOutcome> one = executor.submit(
                    submitTask(repository, first, ready, start));
            Future<SubmissionOutcome> two = executor.submit(
                    submitTask(repository, second, ready, start));
            ready.await();
            start.countDown();

            List<SubmissionOutcome> outcomes = List.of(one.get(), two.get());
            assertEquals(1, outcomes.stream()
                    .filter(SubmissionOutcome.CREATED::equals).count());
            assertEquals(1, outcomes.stream()
                    .filter(SubmissionOutcome.BLOCKED::equals).count());
            assertEquals(1, repository.loadAll().size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void distinctSubmissionsBothCommit() throws Exception {
        ClaimRepository repository = new JsonClaimRepository(
                temporaryDirectory.resolve("claims.json"));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<SubmissionOutcome> one = executor.submit(submitTask(repository,
                    pending(1, "student-1", 101, 201), ready, start));
            Future<SubmissionOutcome> two = executor.submit(submitTask(repository,
                    pending(2, "student-2", 102, 202), ready, start));
            ready.await();
            start.countDown();

            assertEquals(SubmissionOutcome.CREATED, one.get());
            assertEquals(SubmissionOutcome.CREATED, two.get());
            assertEquals(2, repository.loadAll().size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void competingTerminalActionsProduceOneFinalState() throws Exception {
        ClaimRepository repository = new JsonClaimRepository(
                temporaryDirectory.resolve("claims.json"));
        Claim claim = pending(1, "student-1", 101, 201);
        repository.submit(claim);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<TerminalOutcome> approval = executor.submit(() -> {
                ready.countDown();
                start.await();
                return repository.approve(claim.claimId(), Optional.empty(),
                        BASE_TIME.plusSeconds(10)).outcome();
            });
            Future<TerminalOutcome> rejection = executor.submit(() -> {
                ready.countDown();
                start.await();
                return repository.reject(claim.claimId(), "Synthetic rejection.",
                        BASE_TIME.plusSeconds(10)).outcome();
            });
            ready.await();
            start.countDown();

            List<TerminalOutcome> outcomes = List.of(approval.get(), rejection.get());
            assertEquals(1, outcomes.stream().filter(TerminalOutcome.CHANGED::equals).count());
            assertEquals(1, outcomes.stream()
                    .filter(TerminalOutcome.ALREADY_TERMINAL::equals).count());
            assertTrue(repository.loadAll().getFirst().status().isTerminal());
        } finally {
            executor.shutdownNow();
        }
    }

    private static Callable<SubmissionOutcome> submitTask(ClaimRepository repository,
            Claim claim, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            return repository.submit(claim).outcome();
        };
    }

    private static Claim pending(int id, String claimant, long lost, long found) {
        return Claim.createPending(ClaimId.of(new UUID(0L, id)), claimant,
                new UUID(0L, lost), new UUID(0L, found), "Synthetic evidence.", BASE_TIME);
    }
}
