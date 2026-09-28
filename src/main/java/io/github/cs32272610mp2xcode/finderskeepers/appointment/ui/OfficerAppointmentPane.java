package io.github.cs32272610mp2xcode.finderskeepers.appointment.ui;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.OfficerAppointmentService;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CaseStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionCase;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CustodyStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.workspace.SessionView;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Desk Officer UI for slots, custody, and collection. */
public final class OfficerAppointmentPane extends BorderPane implements SessionView {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern(
            "dd MMM yyyy, HH:mm");

    private static final ZoneId ZONE = ZoneId.of("Asia/Singapore");

    private final OfficerAppointmentService service;

    private final TextField location = new TextField();

    private final ListView<CollectionSlot> slots = new ListView<>();

    private final ListView<CollectionCase> cases = new ListView<>();

    private final Label feedback = new Label();

    private final Button addSlot = new Button("+");

    private final Button removeSlot = new Button("−");

    private final Button store = new Button("Record storage location");

    private final Button ready = new Button("Mark ready for collection");

    private final Button confirm = new Button("Confirm collection");

    private final Button noShow = new Button("Record NO_SHOW");

    private final Button returned = new Button("Mark item returned");

    private final Button close = new Button("Close case");

    /** Creates a Desk Officer appointment pane.
     * @param appointmentService authenticated officer service */
    public OfficerAppointmentPane(OfficerAppointmentService appointmentService) {
        service = Objects.requireNonNull(appointmentService, "service");
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
        cases.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    location.clear();
                    updateControls();
                });
        slots.setMinHeight(100);
        cases.setMinHeight(140);
        feedback.setWrapText(true);
        Button refresh = new Button("Refresh");
        addSlot.setTooltip(new Tooltip("Add a 30-minute collection slot"));
        addSlot.setOnAction(event -> showCreateSlotDialog());
        removeSlot.setOnAction(event -> disableSlot());
        refresh.setOnAction(event -> enter());
        store.setOnAction(event -> recordStorage());
        ready.setOnAction(event -> markReady());
        confirm.setOnAction(event -> confirmCollection());
        noShow.setOnAction(event -> recordNoShow());
        returned.setOnAction(event -> markReturned());
        close.setOnAction(event -> closeCase());
        Label slotsLabel = new Label("Collection slots (Singapore time)");
        HBox slotHeader = new HBox(8, slotsLabel, addSlot, removeSlot);
        slotHeader.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(slotsLabel, Priority.ALWAYS);
        VBox content = new VBox(8, slotHeader, slots, new Separator(),
                new Label("Booked and custody cases"), cases, location, store, ready,
                confirm, noShow, returned, close, refresh, feedback);
        content.setPadding(new Insets(12));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        setCenter(scroll);
        updateControls();
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
            feedback.setText("Appointment information is unavailable. Retry later.");
        }
        updateControls();
    }

    @Override
    public boolean hasUnsavedText() {
        return !location.getText().isBlank();
    }

    @Override
    public void clearSessionState() {
        location.clear();
        slots.getSelectionModel().clearSelection();
        slots.getItems().clear();
        cases.getSelectionModel().clearSelection();
        cases.getItems().clear();
        feedback.setText("");
        updateControls();
    }

    private void showCreateSlotDialog() {
        Instant now = service.currentTime();
        ZonedDateTime suggested = now.atZone(ZONE).withMinute(0).withSecond(0).withNano(0);
        while (!suggested.toInstant().isAfter(now)) {
            suggested = suggested.plusMinutes(30);
        }

        DatePicker date = new DatePicker(suggested.toLocalDate());
        ComboBox<Integer> hour = new ComboBox<>();
        for (int value = 0; value < 24; value++) {
            hour.getItems().add(value);
        }
        hour.setValue(suggested.getHour());
        ComboBox<Integer> minute = new ComboBox<>();
        minute.getItems().setAll(0, 30);
        minute.setValue(suggested.getMinute());
        Label error = new Label();
        error.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.addRow(0, new Label("Date"), date);
        form.addRow(1, new Label("Hour (00–23)"), hour);
        form.addRow(2, new Label("Minute"), minute);
        form.add(error, 0, 3, 2, 1);

        ButtonType add = new ButtonType("Add slot", ButtonBar.ButtonData.OK_DONE);
        Dialog<LocalDateTime> dialog = new Dialog<>();
        dialog.setTitle("Add collection slot");
        dialog.setHeaderText("Choose a future 30-minute slot in Singapore time.");
        dialog.getDialogPane().getButtonTypes().setAll(add, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(form);
        Node addButton = dialog.getDialogPane().lookupButton(add);
        addButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (date.getValue() == null || hour.getValue() == null
                    || minute.getValue() == null) {
                error.setText("Choose a date, hour, and minute.");
                event.consume();
                return;
            }
            Instant selected = date.getValue().atTime(hour.getValue(), minute.getValue())
                    .atZone(ZONE).toInstant();
            if (!selected.isAfter(service.currentTime())) {
                error.setText("Choose a time later than now.");
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == add
                ? date.getValue().atTime(hour.getValue(), minute.getValue()) : null);
        dialog.showAndWait().ifPresent(this::createSlot);
    }

    private void createSlot(LocalDateTime local) {
        try {
            AppointmentRepository.SlotResult result = service.createSlot(local.atZone(ZONE)
                    .toInstant());
            switch (result.outcome()) {
                case CREATED -> feedback.setText("Slot created for "
                        + DISPLAY.format(local) + ".");
                case OVERLAPPING -> showError("Slot already exists",
                        "That time overlaps an existing enabled slot. Choose another time.");
                case INVALID_TIME -> showError("Invalid slot time",
                        "Choose a future time starting at :00 or :30.");
                default -> showError("Slot could not be created",
                        "The slot could not be created. Refresh and try again.");
            }
            refresh(false);
        } catch (AppointmentStoreException failure) {
            showError("Slot could not be created",
                    "Appointment information is unavailable. Retry later.");
        }
    }

    private CollectionCase selectedCase() {
        return cases.getSelectionModel().getSelectedItem();
    }

    private void disableSlot() {
        CollectionSlot selected = slots.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Cannot disable slot", "Select a slot first.");
            return;
        }
        try {
            AppointmentRepository.SlotResult result = service.disableSlot(selected.slotId());
            if (result.outcome() == AppointmentRepository.SlotOutcome.CREATED) {
                feedback.setText("Slot disabled.");
            } else {
                showError("Cannot disable slot", switch (result.outcome()) {
                    case BOOKED -> "A booked slot cannot be disabled.";
                    case ALREADY_DISABLED -> "This slot is already disabled.";
                    case INVALID_TIME -> "A started or past slot cannot be disabled.";
                    default -> "The slot could not be disabled. Refresh and try again.";
                });
            }
            refresh(false);
        } catch (AppointmentStoreException failure) {
            showError("Cannot disable slot",
                    "Appointment information is unavailable. Retry later.");
        }
    }

    private void recordStorage() {
        CollectionCase selected = selectedCase();
        if (selected == null) {
            showError("Cannot record storage location", "Select a case first.");
            return;
        }
        String enteredLocation = location.getText().trim();
        if (enteredLocation.isEmpty()) {
            showError("Cannot record storage location", "Enter a storage location first.");
            return;
        }
        if (enteredLocation.codePointCount(0, enteredLocation.length()) > 120) {
            showError("Storage location is too long",
                    "Use 120 characters or fewer for the storage location.");
            return;
        }
        try {
            AppointmentRepository.CaseOutcome outcome = service.recordStorageLocation(
                    selected.claimId(), enteredLocation).outcome();
            if (outcome == AppointmentRepository.CaseOutcome.CHANGED) {
                feedback.setText("Storage location recorded.");
                location.clear();
            } else {
                showError("Cannot record storage location", switch (outcome) {
                    case INVALID_LOCATION -> "Enter a valid storage location.";
                    case CASE_CLOSED, ALREADY_CLOSED -> "This case is already closed.";
                    default -> "The storage location was not saved. Refresh and try again.";
                });
            }
            refresh(false);
        } catch (AppointmentStoreException failure) {
            showError("Cannot record storage location",
                    "Appointment information is unavailable. Retry later.");
        }
    }

    private void markReady() {
        CollectionCase selected = selectedCase();
        if (selected == null) {
            showError("Cannot mark item ready", "Select a case first.");
            return;
        }
        runCaseAction(() -> service.markReadyForCollection(selected.claimId()),
                "Item ready for collection.", "Cannot mark item ready",
                "Record a storage location before marking the item ready.");
    }

    private void confirmCollection() {
        CollectionCase selected = selectedCase();
        if (selected == null || selected.activeAppointment().isEmpty()) {
            showError("Cannot confirm collection", "Select a booked case first.");
            return;
        }
        try {
            AppointmentRepository.AppointmentOutcome outcome = service.confirmCollection(
                    selected.activeAppointment().orElseThrow().appointmentId()).outcome();
            if (outcome == AppointmentRepository.AppointmentOutcome.CHANGED) {
                feedback.setText("Collection confirmed. You can now mark the item returned.");
            } else {
                showError("Cannot confirm collection", switch (outcome) {
                    case TOO_EARLY -> "Collection can be confirmed only after the slot starts.";
                    case INVALID_CUSTODY ->
                        "Record the storage location and mark the item ready first.";
                    case ALREADY_TERMINAL -> "This appointment is already completed or ended.";
                    default -> "Collection was not confirmed. Refresh and try again.";
                });
            }
            refresh(false);
        } catch (AppointmentStoreException failure) {
            showError("Cannot confirm collection",
                    "Appointment information is unavailable. Retry later.");
        }
    }

    private void markReturned() {
        CollectionCase selected = selectedCase();
        if (selected == null) {
            showError("Cannot mark item returned", "Select a case first.");
            return;
        }
        runCaseAction(() -> service.markReturned(selected.claimId()),
                "Item marked returned. You can now close the case.",
                "Cannot mark item returned",
                "Mark the item ready and confirm collection before marking it returned.");
    }

    private void recordNoShow() {
        CollectionCase selected = selectedCase();
        if (selected == null || selected.activeAppointment().isEmpty()) {
            showError("Cannot record NO_SHOW", "Select a booked case first.");
            return;
        }
        try {
            AppointmentRepository.AppointmentOutcome outcome = service.recordNoShow(
                    selected.activeAppointment().orElseThrow().appointmentId()).outcome();
            if (outcome == AppointmentRepository.AppointmentOutcome.CHANGED) {
                feedback.setText("NO_SHOW recorded. The Student may book another slot.");
            } else {
                showError("Cannot record NO_SHOW", switch (outcome) {
                    case TOO_EARLY -> "Wait until the 30-minute slot ends to record NO_SHOW.";
                    case ALREADY_TERMINAL -> "This appointment is already completed or ended.";
                    default -> "NO_SHOW was not recorded. Refresh and try again.";
                });
            }
            refresh(false);
        } catch (AppointmentStoreException failure) {
            showError("Cannot record NO_SHOW",
                    "Appointment information is unavailable. Retry later.");
        }
    }

    private void closeCase() {
        CollectionCase selected = selectedCase();
        if (selected == null) {
            showError("Cannot close case", "Select a case first.");
            return;
        }
        runCaseAction(() -> service.closeCase(selected.claimId()),
                "Case closed.", "Cannot close case",
                "Confirm collection and mark the item returned before closing the case.");
    }

    private void runCaseAction(CaseAction action, String successMessage, String errorTitle,
            String invalidCustodyMessage) {
        try {
            AppointmentRepository.CaseOutcome outcome = action.run().outcome();
            if (outcome == AppointmentRepository.CaseOutcome.CHANGED) {
                feedback.setText(successMessage);
            } else {
                showError(errorTitle, switch (outcome) {
                    case INVALID_CUSTODY -> invalidCustodyMessage;
                    case CASE_CLOSED, ALREADY_CLOSED -> "This case is already closed.";
                    default -> "The case was not updated. Refresh and try again.";
                });
            }
            refresh(false);
        } catch (AppointmentStoreException failure) {
            showError(errorTitle, "Appointment information is unavailable. Retry later.");
        }
    }

    private void updateControls() {
        CollectionCase selected = selectedCase();
        boolean open = selected != null && selected.status() == CaseStatus.OPEN;
        boolean mayEditStorage = open && selected.custodyStatus() != CustodyStatus.RETURNED;
        show(location, mayEditStorage);
        show(store, mayEditStorage);
        show(ready, open);
        boolean hasBookedAppointment = open && selected.activeAppointment().isPresent();
        show(confirm, hasBookedAppointment);
        show(noShow, hasBookedAppointment);
        show(returned, open);
        show(close, open);
    }

    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
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

    @FunctionalInterface
    private interface CaseAction {
        AppointmentRepository.CaseResult run() throws AppointmentStoreException;
    }
}
