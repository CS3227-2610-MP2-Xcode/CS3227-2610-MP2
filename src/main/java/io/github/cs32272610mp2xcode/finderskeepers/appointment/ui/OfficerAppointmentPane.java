package io.github.cs32272610mp2xcode.finderskeepers.appointment.ui;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.OfficerAppointmentService;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AuditEvent;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionCase;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.workspace.SessionView;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/** Desk Officer UI for slots, custody, collection, and audit history. */
public final class OfficerAppointmentPane extends BorderPane implements SessionView {
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern(
            "dd MMM yyyy, HH:mm");

    private static final ZoneId ZONE = ZoneId.of("Asia/Singapore");

    private final OfficerAppointmentService service;

    private final TextField slotStart = new TextField();

    private final TextField location = new TextField();

    private final ListView<CollectionSlot> slots = new ListView<>();

    private final ListView<CollectionCase> cases = new ListView<>();

    private final ListView<AuditEvent> audit = new ListView<>();

    private final Label feedback = new Label();

    /** Creates a Desk Officer appointment pane.
     * @param appointmentService authenticated officer service */
    public OfficerAppointmentPane(OfficerAppointmentService appointmentService) {
        service = Objects.requireNonNull(appointmentService, "service");
        slotStart.setPromptText("yyyy-MM-dd HH:mm (Singapore)");
        location.setPromptText("Storage location");
        slots.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(CollectionSlot item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DISPLAY.withZone(ZONE)
                        .format(item.startsAt()) + (item.enabled() ? " — enabled" : " — disabled"));
            }
        });
        cases.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(CollectionCase item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.claimId().reference()
                        + " — " + item.custodyStatus().name());
            }
        });
        audit.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(AuditEvent item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatAuditEvent(item));
            }
        });
        cases.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> audit.getItems().setAll(
                        selected == null ? java.util.List.of() : selected.auditEvents()));
        slots.setMinHeight(100);
        cases.setMinHeight(140);
        audit.setMinHeight(100);
        feedback.setWrapText(true);
        Button create = new Button("Create 30-minute slot");
        Button disable = new Button("Disable selected slot");
        Button refresh = new Button("Refresh");
        Button store = new Button("Record storage location");
        Button ready = new Button("Mark ready for collection");
        Button confirm = new Button("Confirm collection");
        Button noShow = new Button("Record NO_SHOW");
        Button returned = new Button("Mark item returned");
        Button close = new Button("Close case");
        create.setOnAction(event -> createSlot());
        disable.setOnAction(event -> disableSlot());
        refresh.setOnAction(event -> enter());
        store.setOnAction(event -> recordStorage());
        ready.setOnAction(event -> markReady());
        confirm.setOnAction(event -> confirmCollection());
        noShow.setOnAction(event -> recordNoShow());
        returned.setOnAction(event -> markReturned());
        close.setOnAction(event -> closeCase());
        VBox content = new VBox(8, new Label("Slot start (Singapore time)"), slotStart, create,
                new Label("Slots"), slots, disable, new Separator(),
                new Label("Booked and custody cases"), cases,
                new Label("Audit history (officer view)"), audit, location, store, ready,
                confirm, noShow, returned, close, refresh, feedback);
        content.setPadding(new Insets(12));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        setCenter(scroll);
    }

    /** Formats an officer-visible audit row with the recorded actor role.
     * @param event recorded audit event
     * @return role-accurate audit text */
    static String formatAuditEvent(AuditEvent event) {
        Objects.requireNonNull(event, "event");
        String role = switch (event.actorRole()) {
            case STUDENT -> "Student";
            case DESK_OFFICER -> "Desk Officer";
        };
        return DISPLAY.withZone(ZONE).format(event.occurredAt()) + " — "
                + event.eventType().name() + " — " + role + " " + event.actorUserId();
    }

    /** Refreshes slots and cases from authoritative storage. */
    public void enter() {
        refresh(true);
    }

    private void refresh(boolean clearFeedback) {
        if (clearFeedback) {
            feedback.setText("");
        }
        try {
            ClaimId selectedClaim = cases.getSelectionModel().getSelectedItem() == null
                    ? null : cases.getSelectionModel().getSelectedItem().claimId();
            slots.getItems().setAll(service.loadSlots());
            cases.getItems().setAll(service.loadCases());
            if (selectedClaim != null) {
                cases.getItems().stream()
                        .filter(caseState -> caseState.claimId().equals(selectedClaim))
                        .findFirst().ifPresent(cases.getSelectionModel()::select);
            }
        } catch (AppointmentStoreException failure) {
            slots.getSelectionModel().clearSelection();
            slots.getItems().clear();
            cases.getSelectionModel().clearSelection();
            cases.getItems().clear();
            audit.getItems().clear();
            feedback.setText("Appointment information is unavailable. Retry later.");
        }
    }

    @Override
    public boolean hasUnsavedText() {
        return !slotStart.getText().isBlank() || !location.getText().isBlank();
    }

    @Override
    public void clearSessionState() {
        slotStart.clear();
        location.clear();
        slots.getItems().clear();
        cases.getItems().clear();
        audit.getItems().clear();
        feedback.setText("");
    }

    private void createSlot() {
        try {
            LocalDateTime local = LocalDateTime.parse(slotStart.getText().trim(), INPUT);
            AppointmentRepository.SlotResult result = service.createSlot(local.atZone(ZONE)
                    .toInstant());
            feedback.setText(result.outcome() == AppointmentRepository.SlotOutcome.CREATED
                    ? "Slot created." : "The slot could not be created: " + result.outcome());
            refresh(false);
        } catch (DateTimeParseException failure) {
            feedback.setText("Enter a slot start as yyyy-MM-dd HH:mm.");
        } catch (AppointmentStoreException failure) {
            feedback.setText("The slot could not be created. Retry later.");
        }
    }

    private CollectionCase selectedCase() {
        return cases.getSelectionModel().getSelectedItem();
    }

    private void disableSlot() {
        CollectionSlot selected = slots.getSelectionModel().getSelectedItem();
        if (selected == null) {
            feedback.setText("Select a slot first.");
            return;
        }
        try {
            AppointmentRepository.SlotResult result = service.disableSlot(selected.slotId());
            feedback.setText("Slot update: " + result.outcome());
            refresh(false);
        } catch (AppointmentStoreException failure) {
            feedback.setText("The slot could not be disabled. Retry later.");
        }
    }

    private void recordStorage() {
        CollectionCase selected = selectedCase();
        if (selected == null) {
            feedback.setText("Select a case first.");
            return;
        }
        try {
            feedback.setText("Custody update: " + service.recordStorageLocation(
                    selected.claimId(), location.getText()).outcome());
            refresh(false);
        } catch (AppointmentStoreException failure) {
            feedback.setText("The storage location could not be saved. Retry later.");
        }
    }

    private void markReady() {
        CollectionCase selected = selectedCase();
        if (selected == null) {
            feedback.setText("Select a case first.");
            return;
        }
        runCaseAction(() -> service.markReadyForCollection(selected.claimId()),
                "Item ready for collection.",
                "Record a storage location before marking the item ready.");
    }

    private void confirmCollection() {
        CollectionCase selected = selectedCase();
        if (selected == null || selected.activeAppointment().isEmpty()) {
            feedback.setText("Select a booked case first.");
            return;
        }
        try {
            AppointmentRepository.AppointmentOutcome outcome = service.confirmCollection(
                    selected.activeAppointment().orElseThrow().appointmentId()).outcome();
            feedback.setText(switch (outcome) {
                case CHANGED -> "Collection confirmed. You can now mark the item returned.";
                case TOO_EARLY -> "Collection can be confirmed only after the slot starts.";
                case INVALID_CUSTODY ->
                    "Record the storage location and mark the item ready first.";
                default -> "Collection was not confirmed (" + outcome + "). Refresh and retry.";
            });
            refresh(false);
        } catch (AppointmentStoreException failure) {
            feedback.setText("Collection could not be confirmed. Retry later.");
        }
    }

    private void markReturned() {
        CollectionCase selected = selectedCase();
        if (selected == null) {
            feedback.setText("Select a case first.");
            return;
        }
        runCaseAction(() -> service.markReturned(selected.claimId()),
                "Item marked returned. You can now close the case.",
                "Mark the item ready and confirm collection before marking it returned.");
    }

    private void recordNoShow() {
        CollectionCase selected = selectedCase();
        if (selected == null || selected.activeAppointment().isEmpty()) {
            feedback.setText("Select a booked case first.");
            return;
        }
        try {
            AppointmentRepository.AppointmentOutcome outcome = service.recordNoShow(
                    selected.activeAppointment().orElseThrow().appointmentId()).outcome();
            feedback.setText(switch (outcome) {
                case CHANGED -> "NO_SHOW recorded. The Student may book another slot.";
                case TOO_EARLY -> "Wait until the 30-minute slot ends to record NO_SHOW.";
                case ALREADY_TERMINAL -> "This appointment is already completed or ended.";
                default -> "NO_SHOW was not recorded (" + outcome + "). Refresh and retry.";
            });
            refresh(false);
        } catch (AppointmentStoreException failure) {
            feedback.setText("NO_SHOW could not be recorded. Retry later.");
        }
    }

    private void closeCase() {
        CollectionCase selected = selectedCase();
        if (selected == null) {
            feedback.setText("Select a case first.");
            return;
        }
        runCaseAction(() -> service.closeCase(selected.claimId()),
                "Case closed.",
                "Confirm collection and mark the item returned before closing the case.");
    }

    private void runCaseAction(CaseAction action, String successMessage,
            String invalidCustodyMessage) {
        try {
            AppointmentRepository.CaseOutcome outcome = action.run().outcome();
            feedback.setText(switch (outcome) {
                case CHANGED -> successMessage;
                case INVALID_CUSTODY -> invalidCustodyMessage;
                case CASE_CLOSED, ALREADY_CLOSED -> "This case is already closed.";
                default -> "The case was not updated (" + outcome + "). Refresh and retry.";
            });
            refresh(false);
        } catch (AppointmentStoreException failure) {
            feedback.setText("The case could not be updated. Retry later.");
        }
    }

    @FunctionalInterface
    private interface CaseAction {
        AppointmentRepository.CaseResult run() throws AppointmentStoreException;
    }
}
