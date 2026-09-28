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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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

    private final Button book = new Button("Book selected slot");

    private final Button cancel = new Button("Cancel active appointment");

    private final Button reschedule = new Button("Reschedule to selected slot");

    /** Creates a Student appointment pane.
     * @param appointmentService authenticated Student service */
    public StudentAppointmentPane(StudentAppointmentService appointmentService) {
        service = Objects.requireNonNull(appointmentService, "service");
        claims.setCellFactory(view -> claimReferenceCell());
        claims.setButtonCell(claimReferenceCell());
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
                        + " — " + DISPLAY_TIME.format(item.startsAt()));
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
                setText(item.displayLabel() + " — " + attempt + " — "
                        + item.status().name());
            }
        });
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> enter());
        book.setOnAction(event -> book());
        cancel.setOnAction(event -> cancel());
        reschedule.setOnAction(event -> reschedule());
        claims.valueProperty().addListener((observable, previous, selected) -> updateActions());
        slots.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> updateActions());
        active.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> updateActions());
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
        updateActions();
    }

    private static ListCell<ApprovedClaimSummary> claimReferenceCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ApprovedClaimSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayLabel());
            }
        };
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
            updateActions();
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
            updateActions();
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
        claims.setValue(null);
        claims.getSelectionModel().clearSelection();
        slots.getSelectionModel().clearSelection();
        slots.getItems().clear();
        active.getSelectionModel().clearSelection();
        active.getItems().clear();
        history.getSelectionModel().clearSelection();
        history.getItems().clear();
        feedback.setText("");
        updateActions();
    }

    private void updateActions() {
        boolean hasClaim = claims.getValue() != null;
        boolean hasSlot = slots.getSelectionModel().getSelectedItem() != null;
        boolean hasActive = active.getSelectionModel().getSelectedItem() != null;
        boolean selectedClaimAlreadyActive = hasClaim && active.getItems().stream()
                .anyMatch(appointment -> appointment.claimId().equals(claims.getValue().claimId()));
        show(book, hasClaim && hasSlot && !hasActive && !selectedClaimAlreadyActive);
        show(reschedule, hasSlot && hasActive);
        show(cancel, hasActive);
    }

    private static void show(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void book() {
        ApprovedClaimSummary claim = claims.getValue();
        CollectionSlot slot = slots.getSelectionModel().getSelectedItem();
        if (claim == null || slot == null) {
            showError("Cannot book appointment",
                    "Select an approved Claim and an available slot first.");
            return;
        }
        try {
            AppointmentRepository.BookingResult result = service.book(claim.claimId(),
                    slot.slotId());
            if (result.outcome() == AppointmentRepository.BookingOutcome.BOOKED) {
                feedback.setText("Appointment booked for "
                        + DISPLAY_TIME.format(slot.startsAt()) + ".");
            } else {
                showError("Cannot book appointment", bookingError(result.outcome()));
            }
            refresh(false);
        } catch (ClaimStoreException | AppointmentStoreException failure) {
            showError("Cannot book appointment",
                    "Appointment information is unavailable. Retry later.");
        }
    }

    private void cancel() {
        StudentActiveAppointmentSummary appointment = active.getSelectionModel()
                .getSelectedItem();
        if (appointment == null) {
            showError("Cannot cancel appointment", "Select an active appointment first.");
            return;
        }
        try {
            AppointmentRepository.AppointmentResult result = service.cancel(
                    appointment.appointmentId());
            if (result.outcome() == AppointmentRepository.AppointmentOutcome.CHANGED) {
                feedback.setText("Appointment cancelled. You may book another slot.");
            } else {
                showError("Cannot cancel appointment", switch (result.outcome()) {
                    case TOO_LATE -> "An appointment cannot be cancelled after its slot starts.";
                    case ALREADY_TERMINAL, NOT_BOOKED ->
                        "This appointment is already completed or ended.";
                    default -> "The appointment could not be cancelled. Refresh and try again.";
                });
            }
            refresh(false);
        } catch (AppointmentStoreException failure) {
            showError("Cannot cancel appointment",
                    "Appointment information is unavailable. Retry later.");
        }
    }

    private void reschedule() {
        StudentActiveAppointmentSummary appointment = active.getSelectionModel()
                .getSelectedItem();
        CollectionSlot slot = slots.getSelectionModel().getSelectedItem();
        if (appointment == null || slot == null) {
            showError("Cannot reschedule appointment",
                    "Select an active appointment and a new slot first.");
            return;
        }
        try {
            AppointmentRepository.BookingResult result = service.reschedule(
                    appointment.appointmentId(), slot.slotId());
            if (result.outcome() == AppointmentRepository.BookingOutcome.BOOKED) {
                feedback.setText("Appointment rescheduled.");
            } else {
                showError("Cannot reschedule appointment", bookingError(result.outcome()));
            }
            refresh(false);
        } catch (ClaimStoreException | AppointmentStoreException failure) {
            showError("Cannot reschedule appointment",
                    "Appointment information is unavailable. Retry later.");
        }
    }

    private static String bookingError(AppointmentRepository.BookingOutcome outcome) {
        return switch (outcome) {
            case CLAIM_ALREADY_ACTIVE -> "This Claim already has an active appointment.";
            case CASE_CLOSED -> "This collection case is already closed.";
            case SLOT_DISABLED -> "This slot is disabled. Choose another slot.";
            case SLOT_TAKEN -> "This slot was just booked. Choose another slot.";
            case TOO_LATE, INVALID_TIME -> "Choose a future available slot.";
            case NOT_AUTHORIZED -> "This Claim is no longer approved for booking.";
            case NOT_BOOKED -> "This appointment is no longer active.";
            default -> "The appointment could not be updated. Refresh and try again.";
        };
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        if (getScene() != null && getScene().getWindow() != null) {
            alert.initOwner(getScene().getWindow());
        }
        alert.showAndWait();
    }
}
