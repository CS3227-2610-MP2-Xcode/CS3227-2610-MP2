package io.github.cs32272610mp2xcode.finderskeepers.review.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeskOfficerReviewPersistenceTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void missingStoreIsGlobalEmptyWithoutCreatingStorage() {
        Path store = temporaryDirectory.resolve("missing").resolve("reports.json");
        ReviewQueueState state = new DeskOfficerReviewService(
                new JsonReportRepository(store)).enter();

        assertEquals("No submitted reports.", state.queueMessage().orElseThrow());
        assertFalse(Files.exists(store));
        assertFalse(Files.exists(store.getParent()));
    }

    @Test
    void successfulReviewSurvivesFreshRepositoryWithOnlyStatusChanged()
            throws ReportStoreException {
        Path store = temporaryDirectory.resolve("reports.json");
        ItemReport submitted = syntheticReport();
        ReportRepository repository = new JsonReportRepository(store);
        repository.insert(submitted);
        DeskOfficerReviewService service = new DeskOfficerReviewService(repository);
        service.enter();
        service.select(submitted.reportId());

        service.startReview();

        List<ItemReport> reconstructed = new JsonReportRepository(store).loadAll();
        assertEquals(List.of(submitted.withStatus(ReportStatus.UNDER_REVIEW)),
                reconstructed);
    }

    private static ItemReport syntheticReport() {
        return ItemReport.restore(
                UUID.fromString("3ee895da-703a-49fe-9d93-c7945d181020"),
                "synthetic-reporter",
                ReportType.FOUND,
                "Synthetic water bottle",
                ItemCategory.WATER_BOTTLES,
                "Synthetic library",
                LocalDate.of(2026, 9, 20),
                "Synthetic public description",
                "Synthetic private detail",
                ReportStatus.SUBMITTED,
                Instant.parse("2026-09-20T01:02:03.456Z"));
    }
}
