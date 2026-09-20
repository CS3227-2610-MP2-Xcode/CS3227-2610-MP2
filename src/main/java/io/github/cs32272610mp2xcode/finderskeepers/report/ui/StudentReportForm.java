package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** JavaFX form for a signed-in Student to submit a lost-or-found report. */
public final class StudentReportForm extends BorderPane {
    private final StudentReportFormController controller;

    private final ComboBox<ReportType> reportType = new ComboBox<>();

    private final TextField itemName = new TextField();

    private final ComboBox<ItemCategory> category = new ComboBox<>();

    private final TextField location = new TextField();

    private final DatePicker occurrenceDate = new DatePicker();

    private final TextArea publicDescription = new TextArea();

    private final TextArea privateIdentifyingDetail = new TextArea();

    private final Label feedback = new Label();

    private final Map<ReportFormField, Label> fieldErrors =
            new EnumMap<>(ReportFormField.class);

    /**
     * Creates the report form for one authenticated Student.
     *
     * @param username signed-in username shown above the form
     * @param formController submission controller
     */
    public StudentReportForm(String username,
            StudentReportFormController formController) {
        controller = Objects.requireNonNull(formController, "formController");
        Objects.requireNonNull(username, "username");
        getStyleClass().add("report-form-root");
        setCenter(buildContent(username));
    }

    private ScrollPane buildContent(String username) {
        Label title = new Label("Report a lost or found item");
        title.getStyleClass().add("section-title");
        Label identity = new Label("Signed in as " + username);
        identity.getStyleClass().add("status-label");
        Label introduction = new Label(
                "Tell the school what happened. Fields marked * are required.");
        introduction.setWrapText(true);

        configureInputs();
        GridPane fields = new GridPane();
        fields.setHgap(18);
        fields.setVgap(12);
        fields.setMaxWidth(760);
        fields.add(field("Lost or found *", reportType,
                ReportFormField.REPORT_TYPE), 0, 0);
        fields.add(field("Item name *", itemName,
                ReportFormField.ITEM_NAME), 1, 0);
        fields.add(field("Category *", category,
                ReportFormField.CATEGORY), 0, 1);
        fields.add(field("Location lost or found *", location,
                ReportFormField.LOCATION), 1, 1);
        fields.add(field("Date lost or found *", occurrenceDate,
                ReportFormField.OCCURRENCE_DATE), 0, 2);
        fields.add(field("Public description *", publicDescription,
                ReportFormField.PUBLIC_DESCRIPTION), 0, 3, 2, 1);

        VBox privateField = field("Private identifying detail *",
                privateIdentifyingDetail,
                ReportFormField.PRIVATE_IDENTIFYING_DETAIL);
        Label privacyNote = new Label(
                "Only school staff can see this. Add a detail that can help prove the item is yours.");
        privacyNote.setWrapText(true);
        privacyNote.getStyleClass().add("privacy-note");
        privateField.getChildren().add(1, privacyNote);
        fields.add(privateField, 0, 4, 2, 1);

        Button submit = new Button("Submit report");
        submit.setDefaultButton(true);
        submit.getStyleClass().add("primary-button");
        submit.setOnAction(event -> submit());
        Button clear = new Button("Clear");
        clear.setOnAction(event -> clearForm());
        HBox actions = new HBox(10, submit, clear);
        actions.setAlignment(Pos.CENTER_RIGHT);

        feedback.setWrapText(true);
        feedback.getStyleClass().add("submission-feedback");
        VBox content = new VBox(16, title, identity, introduction, fields,
                feedback, actions);
        content.setPadding(new Insets(28));
        content.setMaxWidth(820);
        content.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.getStyleClass().add("report-form-scroll");
        return scroll;
    }

    private void configureInputs() {
        reportType.setItems(FXCollections.observableArrayList(ReportType.values()));
        reportType.setConverter(displayConverter(ReportType::displayName));
        reportType.setPromptText("Choose lost or found");

        category.setItems(FXCollections.observableArrayList(ItemCategory.values()));
        category.setConverter(displayConverter(ItemCategory::displayName));
        category.setPromptText("Choose a category");

        itemName.setPromptText("For example, blue pencil case");
        location.setPromptText("For example, library shelf 2");
        occurrenceDate.setPromptText("Choose a date");
        configureTextArea(publicDescription,
                "Describe the item without sharing a secret identifying detail");
        configureTextArea(privateIdentifyingDetail,
                "For example, a name written inside or a unique sticker");
    }

    private VBox field(String labelText, javafx.scene.Node input,
            ReportFormField field) {
        Label label = new Label(labelText);
        label.setLabelFor(input);
        label.getStyleClass().add("field-label");
        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("field-error");
        fieldErrors.put(field, error);
        VBox box = new VBox(5, label, input, error);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private void submit() {
        SubmissionViewState state = controller.submit(new ReportFormInput(
                reportType.getValue(), itemName.getText(), category.getValue(),
                location.getText(), occurrenceDate.getValue(),
                publicDescription.getText(), privateIdentifyingDetail.getText()));
        show(state);
        if (state.successful()) {
            clearInputs();
        }
    }

    private void show(SubmissionViewState state) {
        clearErrors();
        state.fieldErrors().forEach((field, message) ->
                fieldErrors.get(field).setText(message));
        feedback.setText(state.message());
        feedback.getStyleClass().removeAll("submission-success", "submission-error");
        feedback.getStyleClass().add(
                state.successful() ? "submission-success" : "submission-error");
    }

    private void clearForm() {
        clearInputs();
        clearErrors();
        feedback.setText("");
        feedback.getStyleClass().removeAll("submission-success", "submission-error");
        reportType.requestFocus();
    }

    private void clearInputs() {
        reportType.setValue(null);
        itemName.clear();
        category.setValue(null);
        location.clear();
        occurrenceDate.setValue(null);
        publicDescription.clear();
        privateIdentifyingDetail.clear();
    }

    private void clearErrors() {
        fieldErrors.values().forEach(error -> error.setText(""));
    }

    private static void configureTextArea(TextArea textArea, String prompt) {
        textArea.setPromptText(prompt);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(3);
    }

    private static <T> StringConverter<T> displayConverter(
            java.util.function.Function<T, String> displayName) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : displayName.apply(value);
            }

            @Override
            public T fromString(String text) {
                throw new UnsupportedOperationException("Selection is not editable");
            }
        };
    }
}
