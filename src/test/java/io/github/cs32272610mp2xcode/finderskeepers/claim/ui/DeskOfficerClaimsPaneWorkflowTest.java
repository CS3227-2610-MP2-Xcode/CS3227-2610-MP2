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
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ListCell;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeskOfficerClaimsPaneWorkflowTest {
    @TempDir
    private Path temporaryDirectory;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        startToolkit();
    }

    @Test
    void approvalWithOptionalReasonClearsPendingAndAppearsInDurableHistory()
            throws Exception {
        ClaimsUiFixture fixture = new ClaimsUiFixture(temporaryDirectory);
        fixture.addLinkedReports();
        fixture.addPendingClaim();

        runOnFxThread(() -> {
            DeskOfficerClaimsPane pane = new DeskOfficerClaimsPane(fixture.officer());
            pane.enter();
            ListView<?> pending = descendants(pane).filter(ListView.class::isInstance)
                    .map(ListView.class::cast).findFirst().orElseThrow();
            assertEquals(1, pending.getItems().size());
            pending.getSelectionModel().selectFirst();
            assertTrue(labels(pane).contains("Synthetic ownership evidence"));
            assertTrue(labels(pane).contains("Private found mark"));
            TextArea reason = descendants(pane).filter(TextArea.class::isInstance)
                    .map(TextArea.class::cast).findFirst().orElseThrow();
            reason.setText("  Synthetic verification.  ");
            assertTrue(pane.hasUnsavedText());
            AtomicReference<Throwable> dialog = respond("Approve claim", "Approve",
                    controls -> assertTrue(controls.getContentText()
                            .contains("Synthetic verification.")));
            button(pane, "Approve").fire();
            assertNull(dialog.get());
            assertTrue(pending.getItems().isEmpty());
            assertFalse(pane.hasUnsavedText());
            assertTrue(labels(pane).contains("Claim approved."));
            TabPane navigation = descendants(pane).filter(TabPane.class::isInstance)
                    .map(TabPane.class::cast).findFirst().orElseThrow();
            navigation.getSelectionModel().select(1);
            ListView<?> history = descendants(pane).filter(ListView.class::isInstance)
                    .map(ListView.class::cast).skip(1).findFirst().orElseThrow();
            assertEquals(1, history.getItems().size());
            history.getSelectionModel().selectFirst();
            assertTrue(labels(pane).contains("Synthetic verification."));
            return null;
        });

        var durable = fixture.claims().loadAll().getFirst();
        assertEquals(ClaimStatus.APPROVED, durable.status());
        assertEquals("Synthetic verification.", durable.decisionReason().orElseThrow());
    }

    @Test
    void rejectionRequiresReasonAndMissingReportPreventsDecision() throws Exception {
        ClaimsUiFixture fixture = new ClaimsUiFixture(temporaryDirectory);
        fixture.addPendingClaim();

        runOnFxThread(() -> {
            DeskOfficerClaimsPane pane = new DeskOfficerClaimsPane(fixture.officer());
            pane.enter();
            ListView<?> pending = descendants(pane).filter(ListView.class::isInstance)
                    .map(ListView.class::cast).findFirst().orElseThrow();
            pending.getSelectionModel().selectFirst();
            assertTrue(button(pane, "Approve").isDisabled());
            assertTrue(button(pane, "Reject").isDisabled());
            assertTrue(descendants(pane).filter(Button.class::isInstance)
                    .map(Button.class::cast).anyMatch(value ->
                            value.getText().equals("Retry") && value.isVisible()));
            assertEquals(ClaimStatus.PENDING_REVIEW,
                    fixture.claims().loadAll().getFirst().status());
            return null;
        });

        fixture.addLinkedReports();
        runOnFxThread(() -> {
            DeskOfficerClaimsPane pane = new DeskOfficerClaimsPane(fixture.officer());
            pane.enter();
            ListView<?> pending = descendants(pane).filter(ListView.class::isInstance)
                    .map(ListView.class::cast).findFirst().orElseThrow();
            pending.getSelectionModel().selectFirst();
            button(pane, "Reject").fire();
            assertTrue(labels(pane).contains("A reason is required to reject this claim."));
            assertEquals(ClaimStatus.PENDING_REVIEW,
                    fixture.claims().loadAll().getFirst().status());
            TextArea reason = descendants(pane).filter(TextArea.class::isInstance)
                    .map(TextArea.class::cast).findFirst().orElseThrow();
            reason.setText("Not enough identifying evidence.");
            AtomicReference<Throwable> dialog = respond("Reject claim", "Reject",
                    controls -> assertTrue(controls.getContentText()
                            .contains("Not enough identifying evidence.")));
            button(pane, "Reject").fire();
            assertNull(dialog.get());
            assertTrue(pending.getItems().isEmpty());
            assertEquals(ClaimStatus.REJECTED,
                    fixture.claims().loadAll().getFirst().status());
            return null;
        });
    }

    @Test
    void corruptClaimStoreShowsRetryThenRestoresPendingQueue() throws Exception {
        ClaimsUiFixture fixture = new ClaimsUiFixture(temporaryDirectory);
        fixture.addLinkedReports();
        fixture.addPendingClaim();
        Path store = temporaryDirectory.resolve("claims.json");
        byte[] valid = Files.readAllBytes(store);
        Files.writeString(store, "invalid claims");

        runOnFxThread(() -> {
            DeskOfficerClaimsPane pane = new DeskOfficerClaimsPane(fixture.officer());
            pane.enter();
            ListView<?> pending = descendants(pane).filter(ListView.class::isInstance)
                    .map(ListView.class::cast).findFirst().orElseThrow();
            assertTrue(pending.isDisabled());
            assertTrue(((javafx.scene.control.Label) pending.getPlaceholder())
                    .getText().contains("Pending Claims are unavailable"));
            Button retry = descendants(pane).filter(Button.class::isInstance)
                    .map(Button.class::cast).filter(value -> value.getText().equals("Retry")
                            && value.isVisible()).findFirst().orElseThrow();
            Files.write(store, valid);
            retry.fire();
            assertEquals(1, pending.getItems().size());
            assertFalse(retry.isVisible());
            return null;
        });
        assertEquals(ClaimStatus.PENDING_REVIEW,
                fixture.claims().loadAll().getFirst().status());
    }

    @Test
    void renderedPendingAndHistoryRowsShowClaimStatusWithoutPrivateMarks()
            throws Exception {
        ClaimsUiFixture fixture = new ClaimsUiFixture(temporaryDirectory);
        fixture.addLinkedReports();
        fixture.addPendingClaim();
        runOnFxThread(() -> {
            DeskOfficerClaimsPane pane = new DeskOfficerClaimsPane(fixture.officer());
            pane.enter();
            Stage stage = new Stage();
            try {
                stage.setScene(new Scene(pane, 1000, 800));
                stage.show();
                pane.applyCss();
                pane.layout();
                ListView<?> pending = descendants(pane).filter(ListView.class::isInstance)
                        .map(ListView.class::cast).findFirst().orElseThrow();
                ListCell<?> pendingRow = descendants(pending)
                        .filter(ListCell.class::isInstance).map(ListCell.class::cast)
                        .filter(cell -> !cell.isEmpty()).findFirst().orElseThrow();
                assertTrue(labels(pendingRow.getGraphic()).contains("Blue bag"));
                assertTrue(labels(pendingRow.getGraphic()).contains("Found blue bag"));
                assertFalse(labels(pendingRow.getGraphic()).contains("Private found mark"));

                fixture.claims().approve(ClaimsUiFixture.CLAIM_ID, Optional.empty(),
                        ClaimsUiFixture.NOW);
                TabPane navigation = descendants(pane).filter(TabPane.class::isInstance)
                        .map(TabPane.class::cast).findFirst().orElseThrow();
                navigation.getSelectionModel().select(1);
                pane.applyCss();
                pane.layout();
                ListView<?> history = descendants(pane).filter(ListView.class::isInstance)
                        .map(ListView.class::cast).skip(1).findFirst().orElseThrow();
                ListCell<?> historyRow = descendants(history)
                        .filter(ListCell.class::isInstance).map(ListCell.class::cast)
                        .filter(cell -> !cell.isEmpty()).findFirst().orElseThrow();
                assertTrue(labels(historyRow.getGraphic()).contains("Approved"));
                assertFalse(labels(historyRow.getGraphic()).contains("Private found mark"));
                return null;
            } finally {
                stage.close();
            }
        });
    }
}
