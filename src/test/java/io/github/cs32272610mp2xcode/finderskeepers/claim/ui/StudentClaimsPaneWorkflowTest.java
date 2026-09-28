package io.github.cs32272610mp2xcode.finderskeepers.claim.ui;

import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestDialogs.respond;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.button;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.descendants;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.labels;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.runOnFxThread;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.startToolkit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudentClaimsPaneWorkflowTest {
    @TempDir
    private Path temporaryDirectory;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        startToolkit();
    }

    @Test
    void validEvidenceBoundarySubmitsAndWithdrawsWithoutExposingPrivateReportText()
            throws Exception {
        ClaimsUiFixture fixture = new ClaimsUiFixture(temporaryDirectory);
        fixture.addLinkedReports();

        runOnFxThread(() -> {
            StudentClaimsPane pane = new StudentClaimsPane(fixture.student());
            pane.enter();
            assertTrue(labels(pane).contains("Blue bag"));
            assertTrue(labels(pane).contains("Found blue bag"));
            assertFalse(labels(pane).contains("Private lost mark"));
            assertFalse(labels(pane).contains("Private found mark"));
            button(pane, "Claim this found item").fire();
            TextArea evidence = descendants(pane).filter(TextArea.class::isInstance)
                    .map(TextArea.class::cast).findFirst().orElseThrow();
            evidence.setText("  ");
            button(pane, "Review claim").fire();
            assertTrue(labels(pane).contains("Ownership evidence is required"));
            assertTrue(fixture.claims().loadAll().isEmpty());

            evidence.setText("x".repeat(500));
            assertTrue(pane.hasUnsavedText());
            AtomicReference<Throwable> submitDialog = respond("Submit claim", "OK",
                    dialog -> {
                        assertTrue(dialog.getContentText().contains("x".repeat(500)));
                        assertFalse(dialog.getContentText().contains("Private found mark"));
                    });
            button(pane, "Review claim").fire();
            assertNull(submitDialog.get());
            assertTrue(labels(pane).contains("Claim submitted for Desk Officer review."));
            assertFalse(pane.hasUnsavedText());
            assertEquals(ClaimStatus.PENDING_REVIEW,
                    fixture.claims().loadAll().getFirst().status());
            assertEquals(500, fixture.claims().loadAll().getFirst()
                    .ownershipEvidence().length());

            AtomicReference<Throwable> withdrawDialog = respond("Withdraw claim", "Withdraw",
                    dialog -> assertTrue(dialog.getHeaderText()
                            .contains(ClaimsUiFixture.CLAIM_ID.reference())));
            button(pane, "Withdraw claim").fire();
            assertNull(withdrawDialog.get());
            assertTrue(labels(pane).contains("Claim withdrawn."));
            assertEquals(ClaimStatus.WITHDRAWN,
                    fixture.claims().loadAll().getFirst().status());
            pane.clearSessionState();
            assertFalse(pane.hasUnsavedText());
            return null;
        });
    }

    @Test
    void unavailableMatchesShowRetryWithoutCreatingClaim() throws Exception {
        ClaimsUiFixture fixture = new ClaimsUiFixture(temporaryDirectory);
        fixture.addLinkedReports();
        Path matchStore = temporaryDirectory.resolve("matches.txt");
        byte[] valid = Files.readAllBytes(matchStore);
        Files.writeString(matchStore, "not a valid relationship document");

        runOnFxThread(() -> {
            StudentClaimsPane pane = new StudentClaimsPane(fixture.student());
            pane.enter();
            assertTrue(labels(pane).contains("Available matches are unavailable"));
            assertTrue(fixture.claims().loadAll().isEmpty());
            Button retry = descendants(pane).filter(Button.class::isInstance)
                    .map(Button.class::cast).filter(value -> value.getText().equals("Retry")
                            && value.isVisible()).findFirst().orElseThrow();
            Files.write(matchStore, valid);
            retry.fire();
            assertTrue(labels(pane).contains("Found blue bag"));
            assertFalse(retry.isVisible());
            assertTrue(fixture.claims().loadAll().isEmpty());
            return null;
        });
    }

    @Test
    void renderedOwnClaimRowShowsStatusButKeepsEvidenceAndPrivateMarkOut()
            throws Exception {
        ClaimsUiFixture fixture = new ClaimsUiFixture(temporaryDirectory);
        fixture.addLinkedReports();
        fixture.addPendingClaim();
        runOnFxThread(() -> {
            StudentClaimsPane pane = new StudentClaimsPane(fixture.student());
            pane.enter();
            TabPane tabs = descendants(pane).filter(TabPane.class::isInstance)
                    .map(TabPane.class::cast).findFirst().orElseThrow();
            tabs.getSelectionModel().select(1);
            Stage stage = new Stage();
            try {
                stage.setScene(new Scene(pane, 900, 700));
                stage.show();
                pane.applyCss();
                pane.layout();
                ListView<?> history = descendants(pane).filter(ListView.class::isInstance)
                        .map(ListView.class::cast).findFirst().orElseThrow();
                assertEquals(1, history.getItems().size());
                ListCell<?> row = descendants(history).filter(ListCell.class::isInstance)
                        .map(ListCell.class::cast).filter(cell -> !cell.isEmpty())
                        .findFirst().orElseThrow();
                assertTrue(labels(row.getGraphic()).contains("Pending"));
                assertTrue(labels(row.getGraphic()).contains("Blue bag"));
                assertFalse(labels(row.getGraphic()).contains("Synthetic ownership evidence"));
                assertFalse(labels(row.getGraphic()).contains("Private found mark"));
                return null;
            } finally {
                stage.close();
            }
        });
    }
}
