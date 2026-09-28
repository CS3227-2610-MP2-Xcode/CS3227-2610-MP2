package io.github.cs32272610mp2xcode.finderskeepers.review.ui;

import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.button;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.descendants;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.labels;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.runOnFxThread;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.startToolkit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.claim.application.ApprovedClaimReportService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.review.application.DeskOfficerReviewService;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeskOfficerReviewPaneWorkflowTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        startToolkit();
    }

    @Test
    void filterAndSelectionShowOfficerDetailsThenApprovalHidesBothEndpoints()
            throws Exception {
        JsonReportRepository reports = reports();
        JsonClaimRepository claims = new JsonClaimRepository(
                temporaryDirectory.resolve("claims.json"));
        ClaimId claimId = ClaimId.of(new UUID(0, 3));
        claims.submit(Claim.createPending(claimId, "student-a", new UUID(0, 1),
                new UUID(0, 2), "Synthetic ownership evidence", NOW.minusSeconds(2)));

        runOnFxThread(() -> {
            DeskOfficerReviewPane pane = new DeskOfficerReviewPane(
                    new DeskOfficerReviewService(reports,
                            new ApprovedClaimReportService(claims)));
            ListView<?> queue = descendants(pane).filter(ListView.class::isInstance)
                    .map(ListView.class::cast).findFirst().orElseThrow();
            assertEquals(2, queue.getItems().size());
            queue.getSelectionModel().selectFirst();
            assertTrue(labels(pane).contains("Private lost mark"));
            assertTrue(labels(pane).contains("Reserved for Desk Officer verification."));
            ToggleButton found = descendants(pane).filter(ToggleButton.class::isInstance)
                    .map(ToggleButton.class::cast).filter(value -> value.getText().equals("Found"))
                    .findFirst().orElseThrow();
            found.fire();
            assertEquals(1, queue.getItems().size());
            queue.getSelectionModel().selectFirst();
            assertTrue(labels(pane).contains("Private found mark"));

            claims.approve(claimId, Optional.empty(), NOW.minusSeconds(1));
            pane.enter();
            assertTrue(queue.getItems().isEmpty());
            assertTrue(((javafx.scene.control.Label) queue.getPlaceholder())
                    .getText().contains("No active"));
            assertFalse(labels(pane).contains("Private found mark"));
            return null;
        });
        assertEquals(2, new JsonReportRepository(
                temporaryDirectory.resolve("reports.json")).loadAll().size());
    }

    @Test
    void unreadableReportStoreShowsRetryAndKeepsCanonicalBytesUntilRestored()
            throws Exception {
        JsonReportRepository reports = reports();
        Path store = temporaryDirectory.resolve("reports.json");
        byte[] valid = Files.readAllBytes(store);
        Files.writeString(store, "not JSON");
        byte[] invalid = Files.readAllBytes(store);

        runOnFxThread(() -> {
            DeskOfficerReviewPane pane = new DeskOfficerReviewPane(
                    new DeskOfficerReviewService(reports));
            ListView<?> queue = descendants(pane).filter(ListView.class::isInstance)
                    .map(ListView.class::cast).findFirst().orElseThrow();
            assertTrue(queue.isDisabled());
            assertTrue(button(pane, "Retry").isVisible());
            assertTrue(java.util.Arrays.equals(invalid, Files.readAllBytes(store)));
            Files.write(store, valid);
            button(pane, "Retry").fire();
            assertEquals(2, queue.getItems().size());
            assertFalse(queue.isDisabled());
            assertFalse(button(pane, "Retry").isVisible());
            return null;
        });
    }

    @Test
    void renderedQueueRowShowsPublicReportSummaryWithoutPrivateDetail()
            throws Exception {
        JsonReportRepository reports = reports();
        runOnFxThread(() -> {
            DeskOfficerReviewPane pane = new DeskOfficerReviewPane(
                    new DeskOfficerReviewService(reports));
            Stage stage = new Stage();
            try {
                stage.setScene(new Scene(pane, 900, 700));
                stage.show();
                pane.applyCss();
                pane.layout();
                ListView<?> queue = descendants(pane).filter(ListView.class::isInstance)
                        .map(ListView.class::cast).findFirst().orElseThrow();
                ListCell<?> row = descendants(queue).filter(ListCell.class::isInstance)
                        .map(ListCell.class::cast).filter(cell -> !cell.isEmpty())
                        .findFirst().orElseThrow();
                assertTrue(labels(row.getGraphic()).contains("Synthetic blue bag"));
                assertTrue(labels(row.getGraphic()).contains("Library"));
                assertFalse(labels(row.getGraphic()).contains("Private lost mark"));
                return null;
            } finally {
                stage.close();
            }
        });
    }

    private JsonReportRepository reports() throws Exception {
        JsonReportRepository repository = new JsonReportRepository(
                temporaryDirectory.resolve("reports.json"));
        repository.insert(report(1, ReportType.LOST, "Private lost mark"));
        repository.insert(report(2, ReportType.FOUND, "Private found mark"));
        return repository;
    }

    private static ItemReport report(long id, ReportType type, String privateDetail) {
        return ItemReport.restore(new UUID(0, id), "synthetic-user-" + id,
                type, "Synthetic blue bag", ItemCategory.BAGS, "Library",
                LocalDate.of(2029, 12, 31), "Synthetic public description",
                privateDetail, ReportStatus.SUBMITTED, NOW.minusSeconds(10));
    }
}
