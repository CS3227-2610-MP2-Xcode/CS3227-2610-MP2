package io.github.cs32272610mp2xcode.finderskeepers.appointment.ui;

import java.util.List;
import java.util.function.Supplier;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/** Plain-language help shown from the Student and Desk Officer appointment pages. */
final class AppointmentHelpDialog {
    private static final List<HelpStep> STUDENT_STEPS = List.of(
            new HelpStep("1. Wait for approval",
                    "You can book only after a Desk Officer approves your Claim."),
            new HelpStep("2. Book a collection time",
                    "Select an approved Claim and an available Singapore-time slot, then choose "
                            + "Book selected slot."),
            new HelpStep("3. Attend your appointment",
                    "Go to the collection desk at the booked time. The Desk Officer prepares the "
                            + "item and confirms collection after the slot starts."),
            new HelpStep("4. Check the result",
                    "Choose Refresh. Completed appointments move from Active appointment to "
                            + "Appointment history, where you can see the final status."));

    private static final List<HelpStep> OFFICER_STEPS = List.of(
            new HelpStep("1. Create a 30-minute slot",
                    "Choose +, then select a future Singapore date, hour, and either 00 or 30 "
                            + "minutes. The slot appears as enabled."),
            new HelpStep("2. Wait for a Student booking",
                    "A case appears under Booked and custody cases only after a Student books."),
            new HelpStep("3. Prepare the item",
                    "Select the case, enter where the item is stored, choose Record storage "
                            + "location, then Mark ready for collection."),
            new HelpStep("4. Confirm attendance",
                    "At or after the slot start, choose Confirm collection. If it is too early, "
                            + "the app will ask you to wait."),
            new HelpStep("5. Complete the handover",
                    "Choose Mark item returned, then Close case. The order matters and protects "
                            + "the collection record."),
            new HelpStep("6. Check the audit history",
                    "The selected case shows who performed each booking, custody, collection, and "
                            + "closure action."));

    private AppointmentHelpDialog() {
    }

    /** Creates the help button used by the Student appointment page. */
    static Button studentHelpButton() {
        return helpButton("Student appointment help", AppointmentHelpDialog::studentPage,
                "student-appointment-help");
    }

    /** Creates the help button used by the Desk Officer appointment page. */
    static Button officerHelpButton() {
        return helpButton("Desk Officer appointment help", AppointmentHelpDialog::officerPage,
                "officer-appointment-help");
    }

    static ScrollPane studentPage() {
        return buildPage(STUDENT_STEPS,
                "Need to change plans? You may cancel or reschedule before the slot starts. "
                        + "Storage locations and officer-only notes are never shown to Students.");
    }

    static ScrollPane officerPage() {
        return buildPage(OFFICER_STEPS,
                "Use NO_SHOW only after a missed slot has ended. A cancelled or no-show booking "
                        + "releases the slot so the Student can book again.");
    }

    private static Button helpButton(String title, Supplier<ScrollPane> pageSupplier, String id) {
        Button button = new Button("Help: how appointments work");
        button.setId(id);
        button.getStyleClass().add("quiet-button");
        button.setOnAction(event -> show(title, pageSupplier.get()));
        return button;
    }

    private static void show(String title, ScrollPane scroll) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("How the collection process works");
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private static ScrollPane buildPage(List<HelpStep> steps, String note) {
        Label introduction = new Label(
                "Appointments are for collecting an item after an ownership Claim is approved.");
        introduction.setWrapText(true);
        introduction.getStyleClass().add("privacy-note");

        VBox content = new VBox(14, introduction);
        content.setPadding(new Insets(8));
        for (HelpStep step : steps) {
            Label heading = new Label(step.title());
            heading.getStyleClass().add("section-title");
            Label explanation = new Label(step.explanation());
            explanation.setWrapText(true);
            explanation.getStyleClass().add("help-step-explanation");
            content.getChildren().addAll(heading, explanation);
        }
        Label finalNote = new Label(note);
        finalNote.setWrapText(true);
        finalNote.getStyleClass().add("privacy-note");
        content.getChildren().add(finalNote);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(560);
        scroll.setPrefViewportHeight(520);
        return scroll;
    }

    /** One titled instruction shown in an appointment help page. */
    private record HelpStep(String title, String explanation) {
    }
}
