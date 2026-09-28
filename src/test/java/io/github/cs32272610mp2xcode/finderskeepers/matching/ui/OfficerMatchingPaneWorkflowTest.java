package io.github.cs32272610mp2xcode.finderskeepers.matching.ui;

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
import java.util.List;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.matching.application.OfficerMatchingService;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.DeterministicMatcher;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.FilePossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OfficerMatchingPaneWorkflowTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        startToolkit();
    }

    @Test
    void selectedSuggestionShowsOfficerEvidenceAndLinksThenUnlinksDurably()
            throws Exception {
        JsonReportRepository reports = reports();
        FilePossibleMatchRepository relationships = relationships();

        runOnFxThread(() -> {
            OfficerMatchingPane pane = pane(reports, relationships);
            List<ListView<?>> lists = listViews(pane);
            ListView<?> suggestions = lists.get(0);
            ListView<?> linked = lists.get(1);
            assertEquals(1, suggestions.getItems().size());
            suggestions.getSelectionModel().selectFirst();
            assertTrue(labels(pane).contains("Private lost mark"));
            assertTrue(labels(pane).contains("Private found mark"));
            assertTrue(labels(pane).contains("Officer-only information"));
            assertFalse(button(pane, "Link as Possible Match").isDisabled());
            button(pane, "Link as Possible Match").fire();
            assertEquals(1, linked.getItems().size());
            assertTrue(suggestions.getItems().isEmpty());
            assertTrue(labels(pane).contains("Possible-match link saved."));
            linked.getSelectionModel().selectFirst();
            assertFalse(button(pane, "Unlink Possible Match").isDisabled());
            button(pane, "Unlink Possible Match").fire();
            assertTrue(linked.getItems().isEmpty());
            assertEquals(1, suggestions.getItems().size());
            assertTrue(labels(pane).contains("Possible-match link removed."));
            return null;
        });
        assertTrue(new FilePossibleMatchRepository(
                temporaryDirectory.resolve("matches.txt")).loadAll().isEmpty());
    }

    @Test
    void invalidRelationshipStoreDisablesActionsUntilRetryRestoresCurrentSuggestions()
            throws Exception {
        JsonReportRepository reports = reports();
        FilePossibleMatchRepository relationships = relationships();
        Path store = temporaryDirectory.resolve("matches.txt");
        Files.writeString(store, "invalid relationship store");
        byte[] badBytes = Files.readAllBytes(store);

        runOnFxThread(() -> {
            OfficerMatchingPane pane = pane(reports, relationships);
            assertTrue(labels(pane).contains("Possible-match links are unavailable"));
            assertTrue(button(pane, "Retry").isVisible());
            assertTrue(button(pane, "Link as Possible Match").isDisabled());
            assertTrue(java.util.Arrays.equals(badBytes, Files.readAllBytes(store)));
            Files.delete(store);
            button(pane, "Retry").fire();
            assertFalse(button(pane, "Retry").isVisible());
            assertEquals(1, listViews(pane).get(0).getItems().size());
            return null;
        });
        assertTrue(relationships.loadAll().isEmpty());
    }

    @Test
    void renderedSuggestionAndLinkRowsShowPublicSummariesAndCurrentStatus()
            throws Exception {
        JsonReportRepository reports = reports();
        FilePossibleMatchRepository relationships = relationships();
        runOnFxThread(() -> {
            OfficerMatchingPane pane = pane(reports, relationships);
            Stage stage = new Stage();
            try {
                stage.setScene(new Scene(pane, 1000, 800));
                stage.show();
                pane.applyCss();
                pane.layout();
                ListView<?> suggestions = listViews(pane).getFirst();
                ListCell<?> visible = descendants(suggestions)
                        .filter(ListCell.class::isInstance).map(ListCell.class::cast)
                        .filter(cell -> !cell.isEmpty()).findFirst().orElseThrow();
                assertTrue(labels(visible.getGraphic()).contains("Possible-match suggestion"));
                assertTrue(labels(visible.getGraphic()).contains("Blue bag"));
                assertFalse(labels(visible.getGraphic()).contains("Private lost mark"));
                suggestions.getSelectionModel().selectFirst();
                button(pane, "Link as Possible Match").fire();
                pane.applyCss();
                pane.layout();
                ListView<?> linked = listViews(pane).get(1);
                ListCell<?> linkedRow = descendants(linked)
                        .filter(ListCell.class::isInstance).map(ListCell.class::cast)
                        .filter(cell -> !cell.isEmpty()).findFirst().orElseThrow();
                assertTrue(labels(linkedRow.getGraphic()).contains("Linked possible match"));
                return null;
            } finally {
                stage.close();
            }
        });
    }

    private JsonReportRepository reports() throws Exception {
        JsonReportRepository repository = new JsonReportRepository(
                temporaryDirectory.resolve("reports.json"));
        repository.insert(report(1, ReportType.LOST, "Blue bag", "Private lost mark"));
        repository.insert(report(2, ReportType.FOUND, "Found blue bag", "Private found mark"));
        return repository;
    }

    private FilePossibleMatchRepository relationships() {
        return new FilePossibleMatchRepository(temporaryDirectory.resolve("matches.txt"));
    }

    private static OfficerMatchingPane pane(JsonReportRepository reports,
            FilePossibleMatchRepository relationships) {
        return new OfficerMatchingPane(new OfficerMatchingService(reports, relationships,
                new DeterministicMatcher()));
    }

    private static ItemReport report(long id, ReportType type, String name, String privateDetail) {
        return ItemReport.restore(new UUID(0, id), "synthetic-user-" + id,
                type, name, ItemCategory.BAGS, "Library", LocalDate.of(2029, 12, 31),
                "Synthetic public description", privateDetail, ReportStatus.SUBMITTED,
                NOW.minusSeconds(10));
    }

    private static List<ListView<?>> listViews(OfficerMatchingPane pane) {
        return descendants(pane).filter(ListView.class::isInstance)
                .<ListView<?>>map(node -> (ListView<?>) node).toList();
    }
}
