package io.github.cs32272610mp2xcode.finderskeepers.claim.ui;

import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.Availability;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.DecisionKind;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.DecisionReview;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.Feedback;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.HistoryClaimRow;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.HistoryFilter;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.OfficerClaimDetail;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.OfficerClaimHandle;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.PendingClaimRow;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.View;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimValidationException;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportConstraints;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** JavaFX presentation for one authenticated Desk Officer Claims workspace. */
public final class DeskOfficerClaimsPane extends BorderPane {
    private final OfficerClaimsService service;

    private final Tab pendingTab = fixedTab("Pending review");

    private final Tab historyTab = fixedTab("Claim history");

    private final TabPane navigation = new TabPane(pendingTab, historyTab);

    private final ListView<PendingClaimRow> pending = new ListView<>();

    private final Label pendingPlaceholder = wrapped("");

    private final Label pendingFeedback = wrapped("");

    private final Button pendingRetry = new Button("Retry");

    private final VBox pendingDetail = new VBox(8);

    private final TextArea reason = new TextArea();

    private final Button approve = new Button("Approve");

    private final Button reject = new Button("Reject");

    private final ListView<HistoryClaimRow> history = new ListView<>();

    private final Label historyPlaceholder = wrapped("");

    private final Label historyFeedback = wrapped("");

    private final Button historyRetry = new Button("Retry");

    private final VBox historyDetail = new VBox(8);

    private final ToggleGroup historyFilters = new ToggleGroup();

    private OfficerClaimHandle reasonTarget;

    private OfficerClaimsState state;

    private boolean rendering;

    /**
     * Creates a thin view over a per-login Desk Officer Claims service.
     *
     * @param claimsService bound Desk Officer workflow
     */
    public DeskOfficerClaimsPane(OfficerClaimsService claimsService) {
        service = Objects.requireNonNull(claimsService, "claimsService");
        configure();
        render(service.clear());
    }

    /** Performs an authoritative Pending review load without clearing reason text. */
    public void enter() {
        rendering = true;
        try {
            navigation.getSelectionModel().select(pendingTab);
        } finally {
            rendering = false;
        }
        render(service.enter());
    }

    /**
     * Returns whether decision text would be discarded by logout.
     *
     * @return true when the reason field contains nonblank text
     */
    public boolean hasUnsavedText() {
        return !reason.getText().isBlank();
    }

    /** Clears all transient pane and service state without storage writes. */
    public void clearSessionState() {
        reason.clear();
        reasonTarget = null;
        render(service.clear());
    }

    private void configure() {
        getStyleClass().add("claims-root");
        getStylesheets().add(Objects.requireNonNull(
                DeskOfficerClaimsPane.class.getResource("claims.css")).toExternalForm());
        setPadding(new Insets(4));
        pendingTab.setContent(pendingView());
        historyTab.setContent(historyView());
        navigation.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    if (rendering) {
                        return;
                    }
                    render(selected == pendingTab
                            ? service.refreshPending() : service.refreshHistory());
                });
        setCenter(navigation);
    }

    private BorderPane pendingView() {
        Label title = title("Pending review");
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> render(service.refreshPending()));
        pendingRetry.setOnAction(event -> render(
                state != null && state.pendingAvailability() == Availability.UNAVAILABLE
                        ? service.retryPending() : service.refreshPending()));
        HBox actions = new HBox(8, pendingRetry, refresh);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(12, title, actions);
        HBox.setHgrow(title, Priority.ALWAYS);

        pending.setCellFactory(ignored -> new PendingCell());
        pending.setPlaceholder(pendingPlaceholder);
        pending.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> selectPending(previous, selected));
        pendingDetail.getStyleClass().add("claims-detail");
        ScrollPane detailScroll = new ScrollPane(pendingDetail);
        detailScroll.setFitToWidth(true);
        detailScroll.getStyleClass().add("claims-scroll");
        SplitPane content = new SplitPane(pending, detailScroll);
        content.setDividerPositions(0.38);

        reason.setPromptText("Optional for approval; required for rejection");
        reason.setWrapText(true);
        reason.setPrefRowCount(4);
        approve.setOnAction(event -> decide(DecisionKind.APPROVE));
        reject.setOnAction(event -> decide(DecisionKind.REJECT));
        HBox decisionActions = new HBox(8, approve, reject);
        decisionActions.setAlignment(Pos.CENTER_RIGHT);
        VBox decisionBox = new VBox(6, sectionTitle("Decision reason"), reason,
                decisionActions);
        decisionBox.getStyleClass().add("claims-editor");

        BorderPane view = new BorderPane(content);
        view.setTop(new VBox(6, header, pendingFeedback));
        view.setBottom(decisionBox);
        BorderPane.setMargin(decisionBox, new Insets(10, 0, 0, 0));
        return view;
    }

    private BorderPane historyView() {
        Label title = title("Claim history");
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> render(service.refreshHistory()));
        historyRetry.setOnAction(event -> render(
                state != null && state.historyAvailability() == Availability.UNAVAILABLE
                        ? service.retryHistory() : service.refreshHistory()));
        HBox actions = new HBox(8, historyRetry, refresh);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(12, title, actions);
        HBox.setHgrow(title, Priority.ALWAYS);

        HBox filters = new HBox(8);
        filters.getChildren().addAll(filter("All", HistoryFilter.ALL),
                filter("Approved", HistoryFilter.APPROVED),
                filter("Rejected", HistoryFilter.REJECTED),
                filter("Withdrawn", HistoryFilter.WITHDRAWN));
        history.setCellFactory(ignored -> new HistoryCell());
        history.setPlaceholder(historyPlaceholder);
        history.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    if (!rendering && selected != null) {
                        render(service.selectHistory(selected.handle()));
                    }
                });
        historyDetail.getStyleClass().add("claims-detail");
        ScrollPane detailScroll = new ScrollPane(historyDetail);
        detailScroll.setFitToWidth(true);
        detailScroll.getStyleClass().add("claims-scroll");
        SplitPane content = new SplitPane(history, detailScroll);
        content.setDividerPositions(0.38);

        BorderPane view = new BorderPane(content);
        view.setTop(new VBox(6, header, filters, historyFeedback));
        return view;
    }

    private RadioButton filter(String text, HistoryFilter filter) {
        RadioButton button = new RadioButton(text);
        button.setUserData(filter);
        button.setToggleGroup(historyFilters);
        if (filter == HistoryFilter.ALL) {
            button.setSelected(true);
        }
        button.setOnAction(event -> {
            if (!rendering) {
                render(service.changeHistoryFilter(filter));
            }
        });
        return button;
    }

    private void selectPending(PendingClaimRow previous, PendingClaimRow selected) {
        if (rendering || selected == null) {
            return;
        }
        if (reasonTarget != null && !reasonTarget.equals(selected.handle())
                && !reason.getText().isBlank()) {
            rendering = true;
            try {
                pending.getSelectionModel().select(previous);
            } finally {
                rendering = false;
            }
            pendingFeedback.setText(
                    "Finish or clear the current decision reason before changing Claims.");
            show(pendingFeedback, true);
            return;
        }
        reasonTarget = selected.handle();
        render(service.selectPending(selected.handle()));
    }

    private void decide(DecisionKind kind) {
        Optional<OfficerClaimDetail> selected = state == null
                ? Optional.empty() : state.selectedDetail();
        if (selected.isEmpty() || reasonTarget == null
                || !reasonTarget.equals(selected.orElseThrow().handle())) {
            pendingFeedback.setText("Select a current Pending Claim before deciding it.");
            show(pendingFeedback, true);
            return;
        }
        final DecisionReview review;
        try {
            review = service.reviewDecision(reasonTarget, kind, reason.getText());
        } catch (ClaimValidationException failure) {
            if (kind == DecisionKind.REJECT && reason.getText().strip().isEmpty()) {
                pendingFeedback.setText("A reason is required to reject this claim.");
            } else {
                pendingFeedback.setText(kind == DecisionKind.REJECT
                        ? "The rejection reason must be valid and at most 500 characters."
                        : "The approval reason must be valid and at most 500 characters.");
            }
            show(pendingFeedback, true);
            return;
        } catch (IllegalArgumentException failure) {
            pendingFeedback.setText(
                    "Refresh and select a current Pending Claim before deciding it.");
            show(pendingFeedback, true);
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        String outcome = kind == DecisionKind.APPROVE ? "Approve" : "Reject";
        confirmation.setTitle(outcome + " claim");
        confirmation.setHeaderText(outcome + " " + review.claimReference() + "?");
        confirmation.setContentText("This final action cannot be undone."
                + review.normalizedReason().map(value -> "\n\nReason:\n" + value)
                        .orElse(""));
        ButtonType confirm = new ButtonType(outcome, ButtonBar.ButtonData.OK_DONE);
        confirmation.getButtonTypes().setAll(confirm, ButtonType.CANCEL);
        if (confirmation.showAndWait().filter(confirm::equals).isEmpty()) {
            return;
        }
        OfficerClaimsState result = kind == DecisionKind.APPROVE
                ? service.approve(review) : service.reject(review);
        if (result.feedback().filter(value -> value == Feedback.APPROVED
                || value == Feedback.REJECTED).isPresent()) {
            reason.clear();
            reasonTarget = null;
        }
        render(result);
    }

    private void render(OfficerClaimsState next) {
        state = Objects.requireNonNull(next, "state");
        rendering = true;
        try {
            renderPending(next);
            renderHistory(next);
            navigation.getSelectionModel().select(next.selectedView() == View.PENDING_REVIEW
                    ? pendingTab : historyTab);
        } finally {
            rendering = false;
        }
    }

    private void renderPending(OfficerClaimsState next) {
        pending.getItems().setAll(next.pendingRows());
        boolean unavailable = next.pendingAvailability() == Availability.UNAVAILABLE;
        pending.setDisable(unavailable);
        pendingPlaceholder.setText(unavailable
                ? "Pending Claims are unavailable. Retry to load current data."
                : "No Claims are pending review.");
        Optional<OfficerClaimDetail> detail = next.selectedView() == View.PENDING_REVIEW
                ? next.selectedDetail() : Optional.empty();
        detail.ifPresentOrElse(value -> renderDetail(pendingDetail, value),
                () -> pendingDetail.getChildren().setAll(wrapped(unavailable
                        ? "Claim details are unavailable."
                        : "Select a Pending Claim to verify its details.")));
        pending.getSelectionModel().clearSelection();
        detail.ifPresent(selected -> pending.getItems().stream()
                .filter(row -> row.handle().equals(selected.handle()))
                .findFirst().ifPresent(pending.getSelectionModel()::select));
        boolean reportsMissing = detail.filter(selected -> selected.lostReport().isEmpty()
                || selected.foundReport().isEmpty()).isPresent();
        show(pendingRetry, unavailable || reportsMissing);
        approve.setDisable(!next.decisionsEnabled());
        reject.setDisable(!next.decisionsEnabled());
        reason.setDisable(unavailable || detail.isEmpty());
        pendingFeedback.setText(feedbackText(next.feedback()));
        show(pendingFeedback, !pendingFeedback.getText().isEmpty());
    }

    private void renderHistory(OfficerClaimsState next) {
        history.getItems().setAll(next.historyRows());
        boolean unavailable = next.historyAvailability() == Availability.UNAVAILABLE;
        history.setDisable(unavailable);
        historyPlaceholder.setText(unavailable
                ? "Claim history is unavailable. Retry to load current data."
                : "No Claims match this history filter.");
        Optional<OfficerClaimDetail> detail = next.selectedView() == View.CLAIM_HISTORY
                ? next.selectedDetail() : Optional.empty();
        detail.ifPresentOrElse(value -> renderDetail(historyDetail, value),
                () -> historyDetail.getChildren().setAll(wrapped(unavailable
                        ? "Claim history details are unavailable."
                        : "Select a historical Claim to view its details.")));
        history.getSelectionModel().clearSelection();
        detail.ifPresent(selected -> history.getItems().stream()
                .filter(row -> row.handle().equals(selected.handle()))
                .findFirst().ifPresent(history.getSelectionModel()::select));
        boolean reportsMissing = detail.filter(selected -> selected.lostReport().isEmpty()
                || selected.foundReport().isEmpty()).isPresent();
        show(historyRetry, unavailable || reportsMissing);
        historyFilters.getToggles().stream()
                .filter(toggle -> toggle.getUserData() == next.historyFilter())
                .findFirst().ifPresent(historyFilters::selectToggle);
        historyFeedback.setText(feedbackText(next.feedback()));
        show(historyFeedback, !historyFeedback.getText().isEmpty());
    }

    private static void renderDetail(VBox container, OfficerClaimDetail detail) {
        container.getChildren().setAll(sectionTitle(detail.claimReference()),
                field("Claimant", detail.claimantUserId()),
                field("Status", detail.status().displayName()),
                field("Submitted", ClaimTimeFormatter.format(detail.submittedAt())),
                new Separator(), sectionTitle("Ownership evidence"),
                wrapped(detail.ownershipEvidence()));
        detail.terminalAt().ifPresent(time -> container.getChildren().add(
                field("Finalized", ClaimTimeFormatter.format(time))));
        detail.decisionReason().ifPresent(value -> container.getChildren().addAll(
                sectionTitle("Decision reason"), wrapped(value)));
        container.getChildren().addAll(new Separator(),
                reportGroup("LOST report", detail.lostReport()), new Separator(),
                reportGroup("FOUND report", detail.foundReport()));
    }

    private static VBox reportGroup(String heading, Optional<ItemReport> report) {
        VBox group = new VBox(5, sectionTitle(heading));
        group.getStyleClass().add("claims-report-group");
        if (report.isEmpty()) {
            group.getChildren().add(wrapped(
                    "Current canonical report unavailable. No report data is reconstructed."));
            return group;
        }
        ItemReport value = report.orElseThrow();
        group.getChildren().addAll(
                field("Reporter ID", value.reporterId()),
                field("Type", value.reportType().displayName()),
                field("Item name", value.itemName()),
                field("Category", value.category().displayName()),
                field("Location", value.location()),
                field("Occurrence date", ReportConstraints.OCCURRENCE_DATE_FORMAT
                        .format(value.occurrenceDate())),
                sectionTitle("Public description"), wrapped(value.publicDescription()),
                sectionTitle("Private identifying detail"),
                wrapped(value.privateIdentifyingDetail()),
                field("Status", value.status().displayName()),
                field("Created at", ReportConstraints.formatCreationTime(value.createdAt())));
        return group;
    }

    private static String feedbackText(Optional<Feedback> feedback) {
        return feedback.map(value -> switch (value) {
            case APPROVED -> "Claim approved.";
            case REJECTED -> "Claim rejected.";
            case ALREADY_TERMINAL ->
                "This Claim was already final. Current status has been refreshed.";
            case LOAD_FAILED -> "Claims could not be loaded. Please retry.";
            case DECISION_FAILED -> "The decision could not be saved. Please try again.";
            case REPORT_UNAVAILABLE ->
                "A current report is unavailable; decisions are disabled.";
            case INVALID_SELECTION -> "That Claim selection is no longer available.";
        }).orElse("");
    }

    private static Tab fixedTab(String label) {
        Tab tab = new Tab(label);
        tab.setClosable(false);
        return tab;
    }

    private static Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("claims-title");
        return label;
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("claims-section-title");
        label.setWrapText(true);
        return label;
    }

    private static VBox field(String name, String value) {
        Label label = new Label(name);
        label.getStyleClass().add("claims-field-label");
        return new VBox(2, label, wrapped(value));
    }

    private static Label wrapped(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("claims-field-value");
        return label;
    }

    private static void show(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /** Renders a pending Claim with only the information needed for review. */
    private static final class PendingCell extends ListCell<PendingClaimRow> {
        @Override
        protected void updateItem(PendingClaimRow row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            VBox content = new VBox(3, sectionTitle(row.claimReference()),
                    wrapped(row.lostItemName().orElse("Lost item unavailable")
                            + " → " + row.foundItemName().orElse("Found item unavailable")),
                    wrapped(row.foundCategory().map(category -> category.displayName())
                            .orElse("Found category unavailable")),
                    wrapped(ClaimTimeFormatter.format(row.submittedAt())));
            content.getStyleClass().add("claims-row");
            setText(null);
            setGraphic(content);
        }
    }

    /** Renders a terminal Claim in the Officer history list. */
    private static final class HistoryCell extends ListCell<HistoryClaimRow> {
        @Override
        protected void updateItem(HistoryClaimRow row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            VBox content = new VBox(3, sectionTitle(row.claimReference()),
                    wrapped(row.status().displayName() + " · "
                            + ClaimTimeFormatter.format(row.terminalAt())),
                    wrapped(row.lostItemName().orElse("Lost item unavailable")
                            + " → " + row.foundItemName().orElse("Found item unavailable")),
                    wrapped(row.foundCategory().map(category -> category.displayName())
                            .orElse("Found category unavailable")));
            content.getStyleClass().add("claims-row");
            setText(null);
            setGraphic(content);
        }
    }
}
