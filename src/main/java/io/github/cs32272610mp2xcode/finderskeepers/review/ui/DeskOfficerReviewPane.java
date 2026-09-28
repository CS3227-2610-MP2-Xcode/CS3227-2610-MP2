package io.github.cs32272610mp2xcode.finderskeepers.review.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportConstraints;
import io.github.cs32272610mp2xcode.finderskeepers.review.application.DeskOfficerReviewService;
import io.github.cs32272610mp2xcode.finderskeepers.review.application.ReviewQueueFilter;
import io.github.cs32272610mp2xcode.finderskeepers.review.application.ReviewQueueState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** JavaFX view for the authenticated Desk Officer active report-review queue. */
public final class DeskOfficerReviewPane extends BorderPane {
    private final DeskOfficerReviewService service;

    private final Map<ReviewQueueFilter, ToggleButton> filterButtons =
            new EnumMap<>(ReviewQueueFilter.class);

    private final ToggleGroup filters = new ToggleGroup();

    private final ListView<ItemReport> queue = new ListView<>();

    private final Label queueMessage = new Label();

    private final VBox details = new VBox(8);

    private final Button retry = new Button("Retry");

    private boolean rendering;

    /**
     * Creates the view and performs its initial authoritative queue load.
     *
     * @param reviewService per-login review workflow
     */
    public DeskOfficerReviewPane(DeskOfficerReviewService reviewService) {
        service = Objects.requireNonNull(reviewService, "reviewService");
        configureView();
        render(service.enter());
    }

    private void configureView() {
        getStyleClass().add("review-root");
        setPadding(new Insets(4));
        getStylesheets().add(Objects.requireNonNull(
                DeskOfficerReviewPane.class.getResource("review.css")).toExternalForm());

        Label title = new Label("Report review");
        title.getStyleClass().add("review-title");
        VBox header = new VBox(6, title, createFilters());
        header.setPadding(new Insets(0, 0, 10, 0));
        setTop(header);

        queue.setCellFactory(ignored -> new ReportQueueCell());
        queue.setPlaceholder(queueMessage);
        queue.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    if (!rendering && selected != null) {
                        render(service.select(selected.reportId()));
                    }
                });

        details.getStyleClass().add("review-details");
        details.setPadding(new Insets(12));
        ScrollPane detailsScroll = new ScrollPane(details);
        detailsScroll.setFitToWidth(true);
        detailsScroll.getStyleClass().add("review-details-scroll");

        SplitPane content = new SplitPane(queue, detailsScroll);
        content.setDividerPositions(0.45);
        VBox.setVgrow(content, Priority.ALWAYS);
        setCenter(content);

        retry.setOnAction(event -> render(service.retry()));
        HBox actions = new HBox(10, retry);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));
        setBottom(actions);
    }

    private HBox createFilters() {
        HBox filterBar = new HBox(8);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getChildren().add(new Label("Show:"));
        for (ReviewQueueFilter filter : ReviewQueueFilter.values()) {
            ToggleButton button = new ToggleButton(filter.displayName());
            button.setUserData(filter);
            button.setToggleGroup(filters);
            filterButtons.put(filter, button);
            filterBar.getChildren().add(button);
        }
        filters.selectedToggleProperty().addListener(
                (observable, previous, selected) -> {
                    if (!rendering && selected != null) {
                        render(service.changeFilter(
                                (ReviewQueueFilter) selected.getUserData()));
                    }
                });
        return filterBar;
    }

    private void render(ReviewQueueState state) {
        rendering = true;
        try {
            filterButtons.get(state.activeFilter()).setSelected(true);
            filterButtons.values().forEach(button -> button.setDisable(!state.available()));
            queue.setDisable(!state.available());
            queue.getItems().setAll(state.visibleReports());
            queueMessage.setText(state.queueMessage().orElse(""));
            state.selectedReport().ifPresentOrElse(
                    queue.getSelectionModel()::select,
                    queue.getSelectionModel()::clearSelection);
            renderDetails(state);
            retry.setVisible(state.retryVisible());
            retry.setManaged(state.retryVisible());
        } finally {
            rendering = false;
        }
    }

    /** Reloads the active queue when its workspace tab is entered. */
    public void enter() {
        render(service.refresh());
    }

    private void renderDetails(ReviewQueueState state) {
        details.getChildren().clear();
        if (state.selectedReport().isEmpty()) {
            Label message = new Label(state.detailsMessage());
            message.setWrapText(true);
            message.getStyleClass().add("review-details-message");
            details.getChildren().add(message);
            return;
        }

        ItemReport report = state.selectedReport().orElseThrow();
        details.getChildren().addAll(
                sectionTitle("Report details"),
                detail("Report ID", report.reportId().toString()),
                detail("Reporter ID", report.reporterId()),
                detail("Type", report.reportType().displayName()),
                detail("Item name", report.itemName()),
                detail("Category", report.category().displayName()),
                detail("Location", report.location()),
                detail("Occurrence date", ReportConstraints.OCCURRENCE_DATE_FORMAT
                        .format(report.occurrenceDate())),
                new Separator(),
                sectionTitle("Public description"),
                wrappedValue(report.publicDescription()),
                new Separator(),
                sectionTitle("Private identifying detail"),
                privateNotice(),
                wrappedValue(report.privateIdentifyingDetail()),
                new Separator(),
                detail("Status", report.status().displayName()),
                detail("Created at", ReportConstraints.formatCreationTime(report.createdAt())));
    }

    private static VBox detail(String name, String value) {
        Label label = new Label(name);
        label.getStyleClass().add("review-field-label");
        return new VBox(2, label, wrappedValue(value));
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("review-section-title");
        return label;
    }

    private static Label wrappedValue(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("review-field-value");
        return label;
    }

    private static Label privateNotice() {
        Label label = new Label("Reserved for Desk Officer verification.");
        label.setWrapText(true);
        label.getStyleClass().add("review-private-notice");
        return label;
    }

    /** Renders the public report details used to scan the Officer review queue. */
    private static final class ReportQueueCell extends ListCell<ItemReport> {
        @Override
        protected void updateItem(ItemReport report, boolean empty) {
            super.updateItem(report, empty);
            if (empty || report == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            Label primary = new Label(report.reportType().displayName()
                    + " · " + report.itemName()
                    + " · " + report.category().displayName());
            primary.getStyleClass().add("review-row-primary");
            Label secondary = new Label(ReportConstraints.OCCURRENCE_DATE_FORMAT
                    .format(report.occurrenceDate()) + " · " + report.location());
            secondary.setWrapText(true);
            secondary.getStyleClass().add("review-row-secondary");
            setText(null);
            setGraphic(new VBox(3, primary, secondary));
        }
    }
}
