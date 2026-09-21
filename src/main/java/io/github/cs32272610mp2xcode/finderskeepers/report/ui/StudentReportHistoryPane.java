package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportHistoryEntry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** JavaFX view of one signed-in Student's report history. */
public final class StudentReportHistoryPane extends BorderPane {
    private final StudentReportHistoryController controller;

    private final TextField searchQuery = new TextField();

    private final Label feedback = new Label();

    private final VBox resultRows = new VBox(12);

    /**
     * Creates and immediately loads a Student's personal report history.
     *
     * @param username signed-in username shown above the history
     * @param historyController authenticated personal-history controller
     */
    public StudentReportHistoryPane(String username,
            StudentReportHistoryController historyController) {
        Objects.requireNonNull(username, "username");
        controller = Objects.requireNonNull(historyController, "historyController");
        getStyleClass().add("report-history-root");
        setCenter(buildContent(username));
        refresh();
    }

    private ScrollPane buildContent(String username) {
        Label title = new Label("My reports");
        title.getStyleClass().add("section-title");
        Label identity = new Label("Signed in as " + username);
        identity.getStyleClass().add("status-label");
        Label instructions = new Label(
                "Search your reports by item name or public description.");
        instructions.setWrapText(true);

        searchQuery.setPromptText("For example, pencil case or white zipper");
        searchQuery.setOnAction(event -> refresh());
        HBox.setHgrow(searchQuery, Priority.ALWAYS);

        Button search = new Button("Search");
        search.getStyleClass().add("primary-button");
        search.setOnAction(event -> refresh());
        Button clear = new Button("Clear search");
        clear.setOnAction(event -> {
            searchQuery.clear();
            refresh();
            searchQuery.requestFocus();
        });
        HBox searchBar = new HBox(10, searchQuery, search, clear);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        feedback.setWrapText(true);
        feedback.getStyleClass().add("history-feedback");

        VBox content = new VBox(16,
                title,
                identity,
                instructions,
                searchBar,
                feedback,
                resultRows);
        content.setPadding(new Insets(28));
        content.setMaxWidth(860);
        content.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.getStyleClass().add("report-history-scroll");
        return scroll;
    }

    /** Reloads personal reports and reapplies the current search query. */
    public void refresh() {
        show(controller.search(searchQuery.getText()));
    }

    private void show(ReportHistoryViewState state) {
        feedback.setText(state.message());
        feedback.getStyleClass().removeAll("history-empty", "history-error");
        if (state.kind() == ReportHistoryViewState.Kind.LOAD_FAILURE) {
            feedback.getStyleClass().add("history-error");
        } else if (state.kind() != ReportHistoryViewState.Kind.RESULTS) {
            feedback.getStyleClass().add("history-empty");
        }
        resultRows.getChildren().setAll(
                state.reports().stream().map(StudentReportHistoryPane::reportCard).toList());
    }

    private static VBox reportCard(ReportHistoryEntry report) {
        Label name = new Label(report.itemName());
        name.getStyleClass().add("history-item-name");
        Label details = new Label(
                report.reportTypeLabel()
                        + " • " + report.categoryLabel()
                        + " • " + report.occurrenceDate());
        details.getStyleClass().add("history-item-details");
        Label description = new Label(report.publicDescription());
        description.setWrapText(true);
        Label status = new Label("Status: " + report.statusLabel());
        status.getStyleClass().add("history-status");

        VBox card = new VBox(7, name, details, description, status);
        card.getStyleClass().add("history-card");
        return card;
    }
}
