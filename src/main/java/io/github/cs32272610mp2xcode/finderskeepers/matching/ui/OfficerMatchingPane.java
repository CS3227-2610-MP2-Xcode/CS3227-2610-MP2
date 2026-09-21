package io.github.cs32272610mp2xcode.finderskeepers.matching.ui;

import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.Availability;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.PairRow;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.ReportSummary;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.Section;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.SelectedComparison;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.SuggestionEmptyReason;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.OfficerMatchingService;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.MatchEvaluation;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportConstraints;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** JavaFX presentation for deterministic officer possible-match review. */
public final class OfficerMatchingPane extends BorderPane {
    private final OfficerMatchingService service;

    private final ListView<PairRow> suggestions = new ListView<>();

    private final ListView<PairRow> linked = new ListView<>();

    private final Label suggestionPlaceholder = new Label();

    private final Label linkedPlaceholder = new Label();

    private final Label feedback = new Label();

    private final VBox comparison = new VBox(10);

    private final Button refresh = new Button("Refresh");

    private final Button retry = new Button("Retry");

    private final Button link = new Button("Link as Possible Match");

    private final Button unlink = new Button("Unlink Possible Match");

    private MatchingWorkspaceState renderedState;

    private boolean rendering;

    /** Creates the view and performs its initial authoritative load. */
    public OfficerMatchingPane(OfficerMatchingService matchingService) {
        service = Objects.requireNonNull(matchingService, "matchingService");
        configureView();
        installDetachCleanup();
        render(service.enter());
    }

    private void configureView() {
        getStyleClass().add("matching-root");
        setPadding(new Insets(4));
        getStylesheets().add(Objects.requireNonNull(
                OfficerMatchingPane.class.getResource("matching.css")).toExternalForm());

        Label title = new Label("Possible matches");
        title.getStyleClass().add("matching-title");
        Label notice = new Label("Rule points explain deterministic evidence; they are not "
                + "confidence or proof of ownership.");
        notice.setWrapText(true);
        notice.getStyleClass().add("matching-notice");
        feedback.setWrapText(true);
        feedback.getStyleClass().add("matching-feedback");
        HBox headerActions = new HBox(8, retry, refresh);
        headerActions.setAlignment(Pos.CENTER_RIGHT);
        HBox headerLine = new HBox(12, title, headerActions);
        HBox.setHgrow(title, Priority.ALWAYS);
        setTop(new VBox(6, headerLine, notice, feedback));

        suggestions.setCellFactory(ignored -> new MatchRowCell());
        linked.setCellFactory(ignored -> new MatchRowCell());
        suggestions.setPlaceholder(suggestionPlaceholder);
        linked.setPlaceholder(linkedPlaceholder);
        suggestions.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> select(Section.SUGGESTIONS, selected));
        linked.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> select(Section.LINKED, selected));

        VBox lists = new VBox(10,
                sectionTitle("Suggestions"), suggestions,
                sectionTitle("Linked possible matches"), linked);
        VBox.setVgrow(suggestions, Priority.ALWAYS);
        VBox.setVgrow(linked, Priority.ALWAYS);
        lists.setMinWidth(300);

        comparison.getStyleClass().add("matching-comparison");
        comparison.setPadding(new Insets(12));
        ScrollPane comparisonScroll = new ScrollPane(comparison);
        comparisonScroll.setFitToWidth(true);
        comparisonScroll.getStyleClass().add("matching-comparison-scroll");

        SplitPane content = new SplitPane(lists, comparisonScroll);
        content.setDividerPositions(0.42);
        setCenter(content);

        refresh.setOnAction(event -> render(service.refresh()));
        retry.setOnAction(event -> render(service.retry()));
        link.setOnAction(event -> selectedPair().ifPresent(pair -> render(
                service.link(pair.firstId(), pair.secondId()))));
        unlink.setOnAction(event -> selectedPair().ifPresent(pair -> render(
                service.unlink(pair.firstId(), pair.secondId()))));
        HBox actions = new HBox(10, link, unlink);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));
        setBottom(actions);
    }

    private void installDetachCleanup() {
        sceneProperty().addListener(new javafx.beans.value.ChangeListener<>() {
            private boolean mounted;

            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.Scene> observable,
                    javafx.scene.Scene previous, javafx.scene.Scene current) {
                if (current != null) {
                    mounted = true;
                } else if (mounted) {
                    render(service.clear());
                }
            }
        });
    }

    private void select(Section section, PairRow row) {
        if (rendering || row == null) {
            return;
        }
        render(service.select(section, row.pair().firstId(), row.pair().secondId()));
    }

    private Optional<io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPair>
            selectedPair() {
        return renderedState == null ? Optional.empty()
                : renderedState.selectedComparison().map(SelectedComparison::pair);
    }

    private void render(MatchingWorkspaceState state) {
        renderedState = state;
        rendering = true;
        try {
            suggestions.getItems().setAll(state.suggestions());
            linked.getItems().setAll(state.linkedPairs());
            suggestions.setDisable(state.availability() != Availability.READY);
            linked.setDisable(state.availability() != Availability.READY);
            refresh.setDisable(state.availability() != Availability.READY);
            retry.setVisible(state.retryVisible());
            retry.setManaged(state.retryVisible());
            link.setDisable(!state.linkEnabled());
            unlink.setDisable(!state.unlinkEnabled());
            restoreSelection(state);
            renderPlaceholders(state);
            renderComparison(state);
            feedback.setText(feedbackText(state));
            feedback.setVisible(!feedback.getText().isEmpty());
            feedback.setManaged(feedback.isVisible());
        } finally {
            rendering = false;
        }
    }

    private void restoreSelection(MatchingWorkspaceState state) {
        suggestions.getSelectionModel().clearSelection();
        linked.getSelectionModel().clearSelection();
        state.selectedComparison().ifPresent(selected -> {
            ListView<PairRow> list = selected.section() == Section.SUGGESTIONS
                    ? suggestions : linked;
            list.getItems().stream()
                    .filter(row -> row.pair().equals(selected.pair()))
                    .findFirst().ifPresent(list.getSelectionModel()::select);
        });
    }

    private void renderPlaceholders(MatchingWorkspaceState state) {
        if (state.availability() == Availability.UNAVAILABLE) {
            suggestionPlaceholder.setText("Possible-match suggestions are unavailable.");
            linkedPlaceholder.setText("Linked possible matches are unavailable.");
            return;
        }
        suggestionPlaceholder.setText(state.suggestionEmptyReason()
                .map(OfficerMatchingPane::emptyText).orElse("No unlinked suggestions."));
        linkedPlaceholder.setText("No linked possible matches.");
    }

    private void renderComparison(MatchingWorkspaceState state) {
        comparison.getChildren().clear();
        if (state.selectedComparison().isEmpty()) {
            String text = state.availability() == Availability.UNAVAILABLE
                    ? "Matching is unavailable. Retry to load current reports and relationships."
                    : "Select a possible match to compare both reports.";
            comparison.getChildren().add(wrappedValue(text));
            return;
        }
        SelectedComparison selected = state.selectedComparison().orElseThrow();
        Label relationship = sectionTitle(selected.section() == Section.LINKED
                ? "Linked possible match" : "Possible-match suggestion");
        HBox reports = new HBox(16,
                reportGroup(selected.firstReport(), selected.pair().firstId()),
                reportGroup(selected.secondReport(), selected.pair().secondId()));
        reports.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        comparison.getChildren().addAll(relationship, reports, new Separator(),
                sectionTitle("Matching-rule evidence"));
        if (selected.evaluation().isPresent()) {
            MatchEvaluation evaluation = selected.evaluation().orElseThrow();
            selected.qualifyingRulePoints().ifPresent(points -> comparison.getChildren().add(
                    wrappedValue("Rule score: " + points + " points")));
            for (String reason : evaluation.componentReasons()) {
                comparison.getChildren().add(wrappedValue(reason));
            }
        } else {
            comparison.getChildren().add(wrappedValue(
                    "Current matching-rule evidence is unavailable or the pair is ineligible."));
        }
    }

    private static VBox reportGroup(Optional<ItemReport> report, java.util.UUID reportId) {
        VBox group = new VBox(5);
        group.getStyleClass().add("matching-report-group");
        if (report.isEmpty()) {
            group.getChildren().addAll(sectionTitle("Report unavailable"),
                    detail("Report ID", reportId.toString()),
                    wrappedValue("No report content is reconstructed. The link can still be removed."));
            return group;
        }
        ItemReport value = report.orElseThrow();
        group.getChildren().addAll(
                sectionTitle(value.reportType().displayName() + " report"),
                detail("Report ID", value.reportId().toString()),
                detail("Reporter ID", value.reporterId()),
                detail("Type", value.reportType().displayName()),
                detail("Item name", value.itemName()),
                detail("Category", value.category().displayName()),
                detail("Location", value.location()),
                detail("Occurrence date", ReportConstraints.OCCURRENCE_DATE_FORMAT
                        .format(value.occurrenceDate())),
                new Separator(),
                sectionTitle("Public description"),
                wrappedValue(value.publicDescription()),
                new Separator(),
                sectionTitle("Private identifying detail"),
                privateNotice(),
                wrappedValue(value.privateIdentifyingDetail()),
                new Separator(),
                detail("Status", value.status().displayName()),
                detail("Created at", ReportConstraints.formatCreationTime(value.createdAt())));
        return group;
    }

    private static String feedbackText(MatchingWorkspaceState state) {
        if (state.feedback().isEmpty()) {
            return "";
        }
        String prefix = state.lastKnownState() ? "Showing last-known state. " : "";
        return prefix + switch (state.feedback().orElseThrow()) {
            case REPORT_LOAD_FAILED -> "Reports are unavailable. Please retry.";
            case RELATIONSHIP_LOAD_FAILED -> "Possible-match links are unavailable. Please retry.";
            case EVALUATION_FAILED -> "Possible matches could not be evaluated safely. Please retry.";
            case LINK_FAILED -> "The possible match could not be linked. Please try again.";
            case UNLINK_FAILED -> "The possible match could not be unlinked. Please try again.";
            case STALE_PAIR -> "This pair is no longer available for linking.";
            case INVALID_PAIR -> "That possible-match selection is invalid.";
            case LINKED -> "Possible-match link saved.";
            case UNLINKED -> "Possible-match link removed.";
            case ALREADY_LINKED -> "This pair is already linked as a possible match.";
            case ALREADY_UNLINKED -> "This pair is already unlinked.";
        };
    }

    private static String emptyText(SuggestionEmptyReason reason) {
        return switch (reason) {
            case NO_ELIGIBLE_PAIR -> "No eligible LOST-to-FOUND pair exists.";
            case NO_QUALIFYING_PAIR -> "No pair qualifies as a possible match.";
            case ALL_QUALIFYING_PAIRS_LINKED -> "All qualifying pairs are already linked.";
        };
    }

    private static VBox detail(String name, String value) {
        Label label = new Label(name);
        label.getStyleClass().add("matching-field-label");
        return new VBox(2, label, wrappedValue(value));
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("matching-section-title");
        return label;
    }

    private static Label wrappedValue(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("matching-field-value");
        return label;
    }

    private static Label privateNotice() {
        Label label = new Label("Officer-only information for verification; it does not prove ownership.");
        label.setWrapText(true);
        label.getStyleClass().add("matching-private-notice");
        return label;
    }

    private static final class MatchRowCell extends ListCell<PairRow> {
        @Override
        protected void updateItem(PairRow row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            VBox content = new VBox(3);
            content.getChildren().add(rowTitle(row));
            row.firstReport().ifPresent(summary -> content.getChildren().add(summaryLine(summary)));
            row.secondReport().ifPresent(summary -> content.getChildren().add(summaryLine(summary)));
            if (row.firstReport().isEmpty() || row.secondReport().isEmpty()) {
                content.getChildren().add(wrappedValue("A linked report is unavailable; select to unlink."));
            }
            row.rulePoints().ifPresent(points -> content.getChildren().add(
                    wrappedValue("Rule score: " + points + " · "
                            + String.join(" · ", row.reasonLabels()))));
            setText(null);
            setGraphic(content);
        }

        private static Label rowTitle(PairRow row) {
            String text = switch (row.kind()) {
                case SUGGESTED -> "Possible-match suggestion";
                case LINKED_QUALIFYING -> "Linked possible match";
                case LINKED_NON_QUALIFYING -> "Linked · no longer qualifying";
                case LINKED_REPORT_UNAVAILABLE -> "Linked · report unavailable";
            };
            Label label = new Label(text);
            label.getStyleClass().add("matching-row-primary");
            return label;
        }

        private static Label summaryLine(ReportSummary summary) {
            Label label = wrappedValue(summary.reportType().displayName() + " · "
                    + summary.itemName() + " · " + summary.category().displayName()
                    + " · " + ReportConstraints.OCCURRENCE_DATE_FORMAT
                            .format(summary.occurrenceDate())
                    + " · " + summary.location());
            label.getStyleClass().add("matching-row-secondary");
            return label;
        }
    }
}
