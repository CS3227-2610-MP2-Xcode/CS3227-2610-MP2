package io.github.cs32272610mp2xcode.finderskeepers.claim.ui;

import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.Availability;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.AvailableMatchCard;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.AvailableMatchGroup;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.AvailableMatchHandle;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.ExistingClaimNotice;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.Feedback;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.MyClaimRow;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.SafeMatchSummary;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.StudentClaimDetail;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.SubmissionReview;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** JavaFX presentation for one authenticated Student Claims workspace. */
public final class StudentClaimsPane extends BorderPane {
    private final StudentClaimsService service;

    private final Tab availableTab = fixedTab("Available matches");

    private final Tab myClaimsTab = fixedTab("My claims");

    private final TabPane navigation = new TabPane(availableTab, myClaimsTab);

    private final VBox availableGroups = new VBox(12);

    private final Label availablePlaceholder = wrapped("");

    private final Label availableFeedback = wrapped("");

    private final Button availableRetry = new Button("Retry");

    private final VBox submissionEditor = new VBox(8);

    private final VBox submissionSummary = new VBox(4);

    private final TextArea evidence = new TextArea();

    private final ListView<MyClaimRow> myClaims = new ListView<>();

    private final Label myClaimsPlaceholder = wrapped("");

    private final Label myClaimsFeedback = wrapped("");

    private final Button myClaimsRetry = new Button("Retry");

    private final VBox claimDetail = new VBox(8);

    private AvailableMatchHandle evidenceTarget;

    private SafeMatchSummary evidenceSummary;

    private boolean rendering;

    /**
     * Creates a thin view over a per-login Student Claims service.
     *
     * @param claimsService bound Student workflow
     */
    public StudentClaimsPane(StudentClaimsService claimsService) {
        service = Objects.requireNonNull(claimsService, "claimsService");
        configure();
        render(service.clear());
    }

    /** Performs an authoritative Available matches load without clearing drafts. */
    public void enter() {
        rendering = true;
        try {
            navigation.getSelectionModel().select(availableTab);
        } finally {
            rendering = false;
        }
        render(service.enter());
    }

    /**
     * Returns whether ownership evidence would be discarded by logout.
     *
     * @return true when the evidence field contains nonblank text
     */
    public boolean hasUnsavedText() {
        return !evidence.getText().isBlank();
    }

    /** Clears all transient pane and service state without storage writes. */
    public void clearSessionState() {
        evidence.clear();
        evidenceTarget = null;
        evidenceSummary = null;
        submissionSummary.getChildren().clear();
        render(service.clear());
    }

    private void configure() {
        getStyleClass().add("claims-root");
        getStylesheets().add(Objects.requireNonNull(
                StudentClaimsPane.class.getResource("claims.css")).toExternalForm());
        setPadding(new Insets(4));
        availableTab.setContent(availableView());
        myClaimsTab.setContent(myClaimsView());
        navigation.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    if (rendering) {
                        return;
                    }
                    if (selected == availableTab) {
                        render(service.refreshAvailable());
                    } else if (selected == myClaimsTab) {
                        render(service.refreshMyClaims());
                    }
                });
        setCenter(navigation);
    }

    private BorderPane availableView() {
        Label title = title("Available matches");
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> render(service.refreshAvailable()));
        availableRetry.setOnAction(event -> render(service.retryAvailable()));
        HBox actions = new HBox(8, availableRetry, refresh);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(12, title, actions);
        HBox.setHgrow(title, Priority.ALWAYS);

        ScrollPane groupsScroll = new ScrollPane(availableGroups);
        groupsScroll.setFitToWidth(true);
        groupsScroll.getStyleClass().add("claims-scroll");
        VBox groupsBox = new VBox(8, availablePlaceholder, groupsScroll);
        VBox.setVgrow(groupsScroll, Priority.ALWAYS);

        evidence.setPromptText("Describe identifying features or other ownership evidence");
        evidence.setWrapText(true);
        evidence.setPrefRowCount(5);
        Button review = new Button("Review claim");
        review.setOnAction(event -> reviewSubmission());
        HBox editorActions = new HBox(review);
        editorActions.setAlignment(Pos.CENTER_RIGHT);
        submissionEditor.getStyleClass().add("claims-editor");
        submissionEditor.getChildren().setAll(sectionTitle("Ownership evidence"),
                submissionSummary, evidence, editorActions);
        submissionEditor.setVisible(false);
        submissionEditor.setManaged(false);

        BorderPane view = new BorderPane(groupsBox);
        view.setTop(new VBox(6, header, availableFeedback));
        view.setBottom(submissionEditor);
        BorderPane.setMargin(submissionEditor, new Insets(10, 0, 0, 0));
        return view;
    }

    private BorderPane myClaimsView() {
        Label title = title("My claims");
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> render(service.refreshMyClaims()));
        myClaimsRetry.setOnAction(event -> render(service.retryMyClaims()));
        HBox actions = new HBox(8, myClaimsRetry, refresh);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(12, title, actions);
        HBox.setHgrow(title, Priority.ALWAYS);

        myClaims.setCellFactory(ignored -> new MyClaimCell());
        myClaims.setPlaceholder(myClaimsPlaceholder);
        myClaims.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    if (!rendering && selected != null) {
                        render(service.selectMyClaim(selected.handle()));
                    }
                });
        claimDetail.getStyleClass().add("claims-detail");
        ScrollPane detailScroll = new ScrollPane(claimDetail);
        detailScroll.setFitToWidth(true);
        detailScroll.getStyleClass().add("claims-scroll");
        SplitPane content = new SplitPane(myClaims, detailScroll);
        content.setDividerPositions(0.42);

        BorderPane view = new BorderPane(content);
        view.setTop(new VBox(6, header, myClaimsFeedback));
        return view;
    }

    private void openSubmission(AvailableMatchCard card) {
        if (evidenceTarget != null && !evidenceTarget.equals(card.handle())
                && !evidence.getText().isBlank()) {
            availableFeedback.setText(
                    "Finish or clear the current ownership evidence before choosing another match.");
            show(availableFeedback, true);
            return;
        }
        Optional<SafeMatchSummary> summary = service.beginSubmission(card.handle());
        if (summary.isEmpty()) {
            render(service.refreshAvailable());
            return;
        }
        evidenceTarget = card.handle();
        evidenceSummary = summary.orElseThrow();
        renderSubmissionSummary(evidenceSummary);
        show(submissionEditor, true);
        evidence.requestFocus();
    }

    private void reviewSubmission() {
        if (evidenceTarget == null || evidenceSummary == null) {
            return;
        }
        final SubmissionReview review;
        try {
            review = service.reviewSubmission(evidenceTarget, evidence.getText());
        } catch (IllegalArgumentException failure) {
            availableFeedback.setText(
                    "Ownership evidence is required, valid, and at most 500 characters.");
            show(availableFeedback, true);
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Submit claim");
        confirmation.setHeaderText("Submit this ownership claim for review?");
        confirmation.setContentText(summaryText(review.matchSummary())
                + "\n\nOwnership evidence:\n" + review.normalizedEvidence());
        if (confirmation.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }
        StudentClaimsState result = service.submit(review);
        if (result.feedback().filter(Feedback.SUBMITTED::equals).isPresent()) {
            evidence.clear();
            evidenceTarget = null;
            evidenceSummary = null;
            show(submissionEditor, false);
            rendering = true;
            try {
                navigation.getSelectionModel().select(myClaimsTab);
            } finally {
                rendering = false;
            }
        }
        render(result);
    }

    private void openExisting(ExistingClaimNotice notice) {
        StudentClaimsState loaded = service.refreshMyClaims();
        rendering = true;
        try {
            navigation.getSelectionModel().select(myClaimsTab);
        } finally {
            rendering = false;
        }
        render(loaded);
        render(service.selectMyClaim(notice.handle()));
    }

    private void withdraw(StudentClaimDetail detail) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Withdraw claim");
        confirmation.setHeaderText("Withdraw " + detail.claimReference() + "?");
        confirmation.setContentText(
                "This final action cannot be undone. No withdrawal reason is recorded.");
        ButtonType withdraw = new ButtonType("Withdraw", ButtonBar.ButtonData.OK_DONE);
        confirmation.getButtonTypes().setAll(withdraw, ButtonType.CANCEL);
        if (confirmation.showAndWait().filter(withdraw::equals).isPresent()) {
            render(service.withdraw(detail.handle()));
        }
    }

    private void render(StudentClaimsState next) {
        Objects.requireNonNull(next, "state");
        rendering = true;
        try {
            renderAvailable(next);
            renderMyClaims(next);
        } finally {
            rendering = false;
        }
    }

    private void renderAvailable(StudentClaimsState next) {
        availableGroups.getChildren().clear();
        for (AvailableMatchGroup group : next.availableGroups()) {
            VBox content = new VBox(7, sectionTitle(group.lostItemName()));
            for (AvailableMatchCard card : group.cards()) {
                Button claim = new Button("Claim this found item");
                claim.setOnAction(event -> openSubmission(card));
                VBox cardView = new VBox(4,
                        field("Found item", card.foundItemName()),
                        field("Category", card.foundCategory().displayName()),
                        field("Occurrence date", ReportConstraints.OCCURRENCE_DATE_FORMAT
                                .format(card.foundOccurrenceDate())),
                        field("Location", card.foundLocation()), claim);
                cardView.getStyleClass().add("claims-card");
                content.getChildren().add(cardView);
            }
            for (ExistingClaimNotice notice : group.existingClaims()) {
                Button existing = new Button("Open existing claim in My claims");
                existing.setOnAction(event -> openExisting(notice));
                content.getChildren().add(new VBox(4,
                        wrapped("You already have an active Claim for this lost item."),
                        existing));
            }
            availableGroups.getChildren().add(content);
        }
        boolean unavailable = next.availableAvailability() == Availability.UNAVAILABLE;
        availablePlaceholder.setText(unavailable
                ? "Available matches are unavailable. Retry to load current data."
                : StudentClaimsState.AVAILABLE_EMPTY_MESSAGE);
        show(availablePlaceholder, unavailable || next.availableEmpty());
        show(availableRetry, next.availableRetryVisible());
        availableGroups.setDisable(unavailable);
        availableFeedback.setText(feedbackText(next.feedback()));
        show(availableFeedback, !availableFeedback.getText().isEmpty());
    }

    private void renderMyClaims(StudentClaimsState next) {
        myClaims.getItems().setAll(next.myClaimRows());
        boolean unavailable = next.myClaimsAvailability() == Availability.UNAVAILABLE;
        myClaims.setDisable(unavailable);
        myClaimsPlaceholder.setText(unavailable
                ? "My claims are unavailable. Retry to load current data."
                : "No claims have been submitted yet.");
        show(myClaimsRetry, next.myClaimsRetryVisible());
        next.selectedClaim().ifPresentOrElse(this::renderClaimDetail,
                () -> renderNoClaimDetail(unavailable));
        myClaims.getSelectionModel().clearSelection();
        next.selectedClaim().ifPresent(selected -> myClaims.getItems().stream()
                .filter(row -> row.handle().equals(selected.handle()))
                .findFirst().ifPresent(myClaims.getSelectionModel()::select));
        myClaimsFeedback.setText(feedbackText(next.feedback()));
        show(myClaimsFeedback, !myClaimsFeedback.getText().isEmpty());
    }

    private void renderClaimDetail(StudentClaimDetail detail) {
        SafeMatchSummary summary = detail.matchSummary();
        claimDetail.getChildren().setAll(
                sectionTitle(detail.claimReference()),
                field("Status", detail.status().displayName()),
                field("Submitted", ClaimTimeFormatter.format(detail.submittedAt())),
                field("Lost item", summary.lostItemName().orElse("Unavailable")),
                field("Found item", summary.foundItemName().orElse("Unavailable")),
                field("Found category", summary.foundCategory()
                        .map(category -> category.displayName()).orElse("Unavailable")),
                field("Found occurrence date", summary.foundOccurrenceDate()
                        .map(ReportConstraints.OCCURRENCE_DATE_FORMAT::format)
                        .orElse("Unavailable")),
                field("Found location", summary.foundLocation().orElse("Unavailable")),
                new Separator(), sectionTitle("Ownership evidence"),
                wrapped(detail.ownershipEvidence()));
        detail.terminalAt().ifPresent(time -> claimDetail.getChildren().add(
                field("Finalized", ClaimTimeFormatter.format(time))));
        detail.decisionReason().ifPresent(reason -> claimDetail.getChildren().addAll(
                sectionTitle("Decision reason"), wrapped(reason)));
        if (detail.status() == ClaimStatus.PENDING_REVIEW) {
            Button withdraw = new Button("Withdraw claim");
            withdraw.setOnAction(event -> withdraw(detail));
            claimDetail.getChildren().add(withdraw);
        }
    }

    private void renderNoClaimDetail(boolean unavailable) {
        claimDetail.getChildren().setAll(wrapped(unavailable
                ? "Claim details are unavailable."
                : "Select one of your claims to view its details."));
    }

    private void renderSubmissionSummary(SafeMatchSummary summary) {
        submissionSummary.getChildren().setAll(
                field("Your lost item", summary.lostItemName().orElse("Unavailable")),
                field("Found item", summary.foundItemName().orElse("Unavailable")),
                field("Found category", summary.foundCategory()
                        .map(category -> category.displayName()).orElse("Unavailable")),
                field("Found occurrence date", summary.foundOccurrenceDate()
                        .map(ReportConstraints.OCCURRENCE_DATE_FORMAT::format)
                        .orElse("Unavailable")),
                field("Found location", summary.foundLocation().orElse("Unavailable")));
    }

    private static String summaryText(SafeMatchSummary summary) {
        return "Lost item: " + summary.lostItemName().orElse("Unavailable")
                + "\nFound item: " + summary.foundItemName().orElse("Unavailable")
                + "\nFound category: " + summary.foundCategory()
                        .map(category -> category.displayName()).orElse("Unavailable")
                + "\nFound occurrence date: " + summary.foundOccurrenceDate()
                        .map(ReportConstraints.OCCURRENCE_DATE_FORMAT::format)
                        .orElse("Unavailable")
                + "\nFound location: " + summary.foundLocation().orElse("Unavailable");
    }

    private static String feedbackText(Optional<Feedback> feedback) {
        return feedback.map(value -> switch (value) {
            case SUBMITTED -> "Claim submitted for Desk Officer review.";
            case WITHDRAWN -> "Claim withdrawn.";
            case OWN_ACTIVE_CLAIM -> "You already have an active Claim for this match.";
            case MATCH_NO_LONGER_AVAILABLE ->
                "This possible match is no longer available for a new Claim.";
            case ALREADY_TERMINAL -> "This Claim is already final.";
            case LOAD_FAILED -> "Claims could not be loaded. Please retry.";
            case SUBMISSION_FAILED -> "The Claim could not be submitted. Please try again.";
            case WITHDRAWAL_FAILED -> "The Claim could not be withdrawn. Please try again.";
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

    private static VBox field(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("claims-field-label");
        return new VBox(2, label, wrapped(valueText));
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

    private static final class MyClaimCell extends ListCell<MyClaimRow> {
        @Override
        protected void updateItem(MyClaimRow row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            VBox content = new VBox(3,
                    sectionTitle(row.status().displayName()),
                    wrapped(row.lostItemName().orElse("Lost item unavailable")
                            + " → " + row.foundItemName().orElse("Found item unavailable")),
                    wrapped(row.foundCategory()
                            .map(category -> category.displayName())
                            .orElse("Found category unavailable")),
                    wrapped(ClaimTimeFormatter.format(row.submittedAt())));
            content.getStyleClass().add("claims-row");
            setText(null);
            setGraphic(content);
        }
    }
}
