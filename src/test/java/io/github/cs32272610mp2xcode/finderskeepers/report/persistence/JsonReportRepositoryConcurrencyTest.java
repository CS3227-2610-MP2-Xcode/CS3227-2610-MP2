package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonReportRepositoryConcurrencyTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void overlappingDistinctInsertsAreEquivalentToSerialExecution() throws Exception {
        ReportRepository repository = new JsonReportRepository(temporaryDirectory.resolve("reports.json"));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<Boolean> first = executor.submit(insertTask(repository, report(1), ready, start));
            Future<Boolean> second = executor.submit(insertTask(repository, report(2), ready, start));
            ready.await();
            start.countDown();

            assertTrue(first.get(), "The first distinct insertion must succeed");
            assertTrue(second.get(), "The second distinct insertion must succeed");
            List<ItemReport> loaded = repository.loadAll();
            assertTrue(loaded.size() == 2, "Concurrent distinct insertions must persist both reports");
            assertTrue(loaded.contains(report(1)) && loaded.contains(report(2)),
                    "Concurrent distinct insertions must have a serial-equivalent result");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void overlappingSameIdentifierInsertsHaveOneDuplicateOutcome() throws Exception {
        ReportRepository repository = new JsonReportRepository(temporaryDirectory.resolve("reports.json"));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<Boolean> first = executor.submit(insertTask(repository, report(1), ready, start));
            Future<Boolean> second = executor.submit(insertTask(repository, report(1), ready, start));
            ready.await();
            start.countDown();

            boolean firstSucceeded = first.get();
            boolean secondSucceeded = second.get();
            assertTrue(firstSucceeded != secondSucceeded,
                    "Exactly one same-identifier insertion must succeed");
            assertTrue(repository.loadAll().size() == 1,
                    "Overlapping same-identifier insertions must persist one report");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void overlappingLoadAndMutationProduceAPermittedSerialOutcome() throws Exception {
        ReportRepository repository = new JsonReportRepository(temporaryDirectory.resolve("reports.json"));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<Integer> load = executor.submit(() -> {
                ready.countDown();
                start.await();
                return repository.loadAll().size();
            });
            Future<Boolean> insert = executor.submit(insertTask(repository, report(1), ready, start));
            ready.await();
            start.countDown();

            int observedCount = load.get();
            assertTrue(insert.get(), "The overlapping insertion must succeed");
            assertTrue(observedCount == 0 || observedCount == 1,
                    "The load must observe either permitted serial state");
            assertTrue(repository.loadAll().size() == 1,
                    "The final state must contain the successful insertion");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void overlappingInsertAndReplacementLoseNoSuccessfulMutation() throws Exception {
        ReportRepository repository = new JsonReportRepository(temporaryDirectory.resolve("reports.json"));
        ItemReport original = report(1);
        ItemReport replacement = replacementFor(original);
        repository.insert(original);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<Boolean> insert = executor.submit(insertTask(repository, report(2), ready, start));
            Future<Boolean> replace = executor.submit(() -> {
                ready.countDown();
                start.await();
                repository.replace(original.reportId(), replacement);
                return true;
            });
            ready.await();
            start.countDown();

            assertTrue(insert.get(), "The overlapping insertion must succeed");
            assertTrue(replace.get(), "The overlapping replacement must succeed");
            List<ItemReport> loaded = repository.loadAll();
            assertTrue(loaded.size() == 2, "Both successful mutations must be represented");
            assertTrue(loaded.contains(replacement) && loaded.contains(report(2)),
                    "The final state must be equivalent to a serial execution");
        } finally {
            executor.shutdownNow();
        }
    }

    private static Callable<Boolean> insertTask(
            ReportRepository repository,
            ItemReport report,
            CountDownLatch ready,
            CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                repository.insert(report);
                return true;
            } catch (ReportStoreException failure) {
                if (failure.reason() == ReportStoreException.Reason.DUPLICATE_REPORT_ID) {
                    return false;
                }
                throw failure;
            }
        };
    }

    private static ItemReport report(int sequence) {
        return ItemReport.restore(
                new UUID(4L, sequence),
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

    private static ItemReport replacementFor(ItemReport original) {
        return ItemReport.restore(
                original.reportId(),
                original.reporterId(),
                ReportType.FOUND,
                "Replacement item",
                ItemCategory.OTHER,
                "Replacement location",
                LocalDate.of(2026, 9, 1),
                "Replacement public description",
                "Replacement private detail",
                ReportStatus.UNDER_REVIEW,
                original.createdAt());
    }
}
