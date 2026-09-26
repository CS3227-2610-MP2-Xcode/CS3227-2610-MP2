package io.github.cs32272610mp2xcode.finderskeepers.appointment.ui;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.StudentActiveAppointmentSummary;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.StudentAppointmentHistorySummary;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.StudentAppointmentService;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.ApprovedClaimSummary;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.workspace.SessionView;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/** Student UI for booking, changing, and reviewing collection appointments. */
public final class StudentAppointmentPane extends BorderPane implements SessionView {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern(
            "dd MMM yyyy, HH:mm").withZone(ZoneId.of("Asia/Singapore"));

    private final StudentAppointmentService service;

    private final ComboBox<ApprovedClaimSummary> claims = new ComboBox<>();

    private final ListView<CollectionSlot> slots = new ListView<>();

    private final ListView<StudentActiveAppointmentSummary> active = new ListView<>();

    private final ListView<StudentAppointmentHistorySummary> history = new ListView<>();

    private final Label feedback = new Label();

    /** Creates a Student appointment pane.
     * @param appointmentService authenticated Student service */
    public StudentAppointmentPane(StudentAppointmentService appointmentService) {
        service = Objects.requireNonNull(appointmentService, "service");
        claims.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ApprovedClaimSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.claimReference());
            }
        });
        slots.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(CollectionSlot item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DISPLAY_TIME.format(item.startsAt()));
            }
        });
        active.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(StudentActiveAppointmentSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.status().name()
                        + " — " + DISPLAY_TIME.format(item.startsAt())
                        + " — " + item.appointmentId().value());
            }
        });
        history.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(StudentAppointmentHistorySummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String attempt = item.attemptNumber() == 0 ? "No appointment"
                        : "Attempt " + item.attemptNumber() + " — "
                                + item.appointmentStatus().orElseThrow().name() + " — "
                                + DISPLAY_TIME.format(item.startsAt().orElseThrow());
                setText(item.claimReference() + " — " + attempt + " — "
                        + item.status().name());
            }
        });
        Button refresh = new Button("Refresh");
        Button book = new Button("Book selected slot");
        Button cancel = new Button("Cancel active appointment");
        Button reschedule = new Button("Reschedule to selected slot");
        refresh.setOnAction(event -> enter());
        book.setOnAction(event -> book());
        cancel.setOnAction(event -> cancel());
        reschedule.setOnAction(event -> reschedule());
        slots.setMinHeight(120);
        active.setMinHeight(90);
        history.setMinHeight(120);
        feedback.setWrapText(true);
        VBox content = new VBox(8, new Label("Approved Claim"), claims,
                new Label("Available collection slots (Singapore time)"), slots,
                book, reschedule, cancel, new Separator(),
                new Label("Active appointment (Singapore time)"), active,
                new Label("Appointment history"), history, refresh, feedback);
        content.setPadding(new Insets(12));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        setCenter(scroll);
    }

    /** Refreshes authoritative Claims, slots, active appointment, and history. */
    public void enter() {
        refresh(true);
    }

    private void refresh(boolean clearFeedback) {
        if (clearFeedback) {
            feedback.setText("");
        }
        try {
            claims.getItems().setAll(service.loadApprovedClaims());
            slots.getItems().setAll(service.loadAvailableSlots());
            active.getItems().setAll(service.loadActiveAppointments());
            history.getItems().setAll(service.loadHistory());
            if (clearFeedback) {
                feedback.setText(claims.getItems().isEmpty()
                        ? "No approved Claims are currently available for booking."
                        : slots.getItems().isEmpty()
                                ? "No collection slots are currently available." : "");
            }
        } catch (ClaimStoreException | AppointmentStoreException failure) {
            claims.setValue(null);
            claims.getSelectionModel().clearSelection();
            claims.getItems().clear();
            slots.getSelectionModel().clearSelection();
            slots.getItems().clear();
            active.getSelectionModel().clearSelection();
            active.getItems().clear();
            history.getSelectionModel().clearSelection();
            history.getItems().clear();
            feedback.setText("Appointment information is unavailable. Retry later.");
        }
    }

    @Override
    public boolean hasUnsavedText() {
        return false;
    }

    @Override
    public void clearSessionState() {
        claims.getItems().clear();
        slots.getItems().clear();
        active.getItems().clear();
        history.getItems().clear();
        feedback.setText("");
    }

    private void book() {
        ApprovedClaimSummary claim = claims.getValue();
        CollectionSlot slot = slots.getSelectionModel().getSelectedItem();
        if (claim == null || slot == null) {
            feedback.setText("Select an approved Claim and an available slot first.");
            return;
        }
        try {
            AppointmentRepository.BookingResult result = service.book(claim.claimId(),
                    slot.slotId());
            feedback.setText(result.outcome() == AppointmentRepository.BookingOutcome.BOOKED
                    ? "Appointment booked for " + DISPLAY_TIME.format(slot.startsAt()) + "."
                    : "The appointment could not be booked; refresh and try again.");
            refresh(false);
        } catch (ClaimStoreException | AppointmentStoreException failure) {
            feedback.setText("The appointment could not be booked. Retry later.");
        }
    }

    private void cancel() {
        StudentActiveAppointmentSummary appointment = active.getSelectionModel()
                .getSelectedItem();
        if (appointment == null) {
            feedback.setText("Select an active appointment first.");
            return;
        }
        try {
            AppointmentRepository.AppointmentResult result = service.cancel(
                    appointment.appointmentId());
            feedback.setText(result.outcome() == AppointmentRepository.AppointmentOutcome.CHANGED
                    ? "Appointment cancelled. You may book another slot."
                    : "The appointment could not be cancelled.");
            refresh(false);
        } catch (AppointmentStoreException failure) {
            feedback.setText("The appointment could not be cancelled. Retry later.");
        }
    }

    private void reschedule() {
        StudentActiveAppointmentSummary appointment = active.getSelectionModel()
                .getSelectedItem();
        CollectionSlot slot = slots.getSelectionModel().getSelectedItem();
        if (appointment == null || slot == null) {
            feedback.setText("Select an active appointment and a new slot first.");
            return;
        }
        try {
            AppointmentRepository.BookingResult result = service.reschedule(
                    appointment.appointmentId(), slot.slotId());
            feedback.setText(result.outcome() == AppointmentRepository.BookingOutcome.BOOKED
                    ? "Appointment rescheduled."
                    : "The appointment could not be rescheduled.");
            refresh(false);
        } catch (ClaimStoreException | AppointmentStoreException failure) {
            feedback.setText("The appointment could not be rescheduled. Retry later.");
        }
    }
}
