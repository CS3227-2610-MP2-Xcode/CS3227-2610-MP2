package io.github.cs32272610mp2xcode.finderskeepers.review.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;

import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeskOfficerReviewPersistenceTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void missingStoreIsActiveQueueEmptyWithoutCreatingStorage() {
        Path store = temporaryDirectory.resolve("missing").resolve("reports.json");

        ReviewQueueState state = new DeskOfficerReviewService(
                new JsonReportRepository(store)).enter();

        assertEquals("No active reports.", state.queueMessage().orElseThrow());
        assertFalse(Files.exists(store));
        assertFalse(Files.exists(store.getParent()));
    }
}
