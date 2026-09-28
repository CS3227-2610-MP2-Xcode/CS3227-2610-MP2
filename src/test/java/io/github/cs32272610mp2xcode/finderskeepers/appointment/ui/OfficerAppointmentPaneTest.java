package io.github.cs32272610mp2xcode.finderskeepers.appointment.ui;

import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.runOnFxThread;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.startToolkit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestDialogs.respond;

import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.OfficerAppointmentService;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CaseStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionCase;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CustodyStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentRepository.SlotOutcome;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.JsonAppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OfficerAppointmentPaneTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        startToolkit();
    }

    @Test
    void dialogRejectsMissingAndCurrentTimeThenCreatesNextHalfHour() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        OfficerAppointmentPane pane = runOnFxThread(() -> {
            OfficerAppointmentPane view = new OfficerAppointmentPane(service(repository, 1));
            AtomicReference<Throwable> interactionFailure = new AtomicReference<>();
            Platform.runLater(() -> {
                Stage dialog = showingDialog();
                try {
                    DialogPane controls = (DialogPane) dialog.getScene().getRoot();
                    Node form = controls.getContent();
                    DatePicker date = descendants(form).filter(DatePicker.class::isInstance)
                            .map(DatePicker.class::cast).findFirst().orElseThrow();
                    @SuppressWarnings("unchecked")
                    ComboBox<Integer> hour = (ComboBox<Integer>) descendants(form)
                            .filter(ComboBox.class::isInstance)
                            .filter(node -> ((ComboBox<?>) node).getItems().size() == 24)
                            .findFirst().orElseThrow();
                    @SuppressWarnings("unchecked")
                    ComboBox<Integer> minute = (ComboBox<Integer>) descendants(form)
                            .filter(ComboBox.class::isInstance)
                            .filter(node -> ((ComboBox<?>) node).getItems().size() == 2)
                            .findFirst().orElseThrow();
                    Label error = descendants(form).filter(Label.class::isInstance)
                            .map(Label.class::cast).filter(label -> label.getText().isEmpty())
                            .findFirst().orElseThrow();
                    ButtonType addType = controls.getButtonTypes().stream()
                            .filter(type -> type.getText().equals("Add slot"))
                            .findFirst().orElseThrow();
                    Button add = (Button) controls.lookupButton(addType);

                    date.setValue(null);
                    add.fire();
                    assertEquals("Choose a date, hour, and minute.", error.getText());
                    assertTrue(dialog.isShowing());
                    date.setValue(LocalDate.of(2030, 1, 1));
                    hour.setValue(8);
                    minute.setValue(0);
                    add.fire();
                    assertEquals("Choose a time later than now.", error.getText());
                    assertTrue(dialog.isShowing());
                    minute.setValue(30);
                    add.fire();
                } catch (AssertionError | java.util.NoSuchElementException
                        | ClassCastException | IllegalStateException failure) {
                    interactionFailure.set(failure);
                    dialog.close();
                }
            });
            button(view, "+").fire();
            assertNull(interactionFailure.get());
            assertTrue(labelText(view).contains("Slot created for 01 Jan 2030, 08:30."));
            return view;
        });

        assertEquals(1, new JsonAppointmentRepository(store).loadSlots().size());
        assertEquals(Instant.parse("2030-01-01T00:30:00Z"),
                new JsonAppointmentRepository(store).loadSlots().getFirst().startsAt());
        runOnFxThread(() -> {
            pane.clearSessionState();
            return null;
        });
    }

    @Test
    void refreshedSlotCanBeDisabledAndUnsavedDraftIsClearedOnSessionEnd() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        SlotId slotId = SlotId.of(new UUID(0, 7));
        repository.createSlot(CollectionSlot.create(slotId,
                NOW.plusSeconds(3600), NOW, "officer-a"));

        runOnFxThread(() -> {
            OfficerAppointmentPane pane = new OfficerAppointmentPane(service(repository, 9));
            pane.enter();
            @SuppressWarnings("unchecked")
            ListView<CollectionSlot> slots = (ListView<CollectionSlot>) descendants(pane)
                    .filter(ListView.class::isInstance).findFirst().orElseThrow();
            assertEquals(slotId, slots.getItems().getFirst().slotId());
            slots.getSelectionModel().selectFirst();
            button(pane, "−").fire();
            assertFalse(slots.getItems().getFirst().enabled());
            assertTrue(labelText(pane).contains("Slot disabled."));

            TextField location = descendants(pane).filter(TextField.class::isInstance)
                    .map(TextField.class::cast).findFirst().orElseThrow();
            location.setText("Synthetic shelf draft");
            assertTrue(pane.hasUnsavedText());
            pane.clearSessionState();
            assertFalse(pane.hasUnsavedText());
            assertTrue(slots.getItems().isEmpty());
            return null;
        });
        assertEquals(SlotOutcome.ALREADY_DISABLED,
                repository.disableSlot(slotId, "officer-b", NOW).outcome());
    }

    @Test
    void custodyButtonsAdvanceBookedCaseAndShowStoredActorRoles() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        SlotId slotId = SlotId.of(new UUID(0, 7));
        ClaimId claimId = ClaimId.of(new UUID(0, 8));
        Instant start = NOW.plusSeconds(3600);
        repository.createSlot(CollectionSlot.create(slotId, start, NOW, "officer-a"));
        repository.book(claimId, "student-a", slotId, AppointmentId.of(new UUID(0, 9)),
                "student-a", UserRole.STUDENT, NOW);

        runOnFxThread(() -> {
            OfficerAppointmentPane pane = new OfficerAppointmentPane(
                    service(repository, 10, start));
            pane.enter();
            @SuppressWarnings("unchecked")
            ListView<CollectionCase> cases = (ListView<CollectionCase>) descendants(pane)
                    .filter(ListView.class::isInstance).skip(1).findFirst().orElseThrow();
            cases.getSelectionModel().selectFirst();
            TextField location = descendants(pane).filter(TextField.class::isInstance)
                    .map(TextField.class::cast).findFirst().orElseThrow();
            location.setText("Office shelf");
            assertTrue(pane.hasUnsavedText());
            button(pane, "Record storage location").fire();
            assertTrue(labels(pane).contains("Storage location recorded."));
            assertFalse(pane.hasUnsavedText());
            button(pane, "Mark ready for collection").fire();
            assertTrue(labels(pane).contains("Item ready for collection."));
            assertTrue(button(pane, "Confirm collection").isVisible());
            button(pane, "Confirm collection").fire();
            assertTrue(labels(pane).contains("Collection confirmed."));
            button(pane, "Mark item returned").fire();
            assertTrue(labels(pane).contains("Item marked returned."));
            button(pane, "Close case").fire();
            assertTrue(labels(pane).contains("Case closed."));
            assertFalse(button(pane, "Mark ready for collection").isVisible());
            @SuppressWarnings("unchecked")
            ListView<?> audit = (ListView<?>) descendants(pane)
                    .filter(ListView.class::isInstance).skip(2).findFirst().orElseThrow();
            assertTrue(audit.getItems().size() >= 5);
            return null;
        });

        CollectionCase restored = new JsonAppointmentRepository(store).loadCases().getFirst();
        assertEquals(CaseStatus.CLOSED, restored.status());
        assertEquals(CustodyStatus.RETURNED, restored.custodyStatus());
        assertTrue(restored.auditEvents().stream().anyMatch(event ->
                event.actorRole() == UserRole.STUDENT));
        assertTrue(restored.auditEvents().stream().anyMatch(event ->
                event.actorRole() == UserRole.DESK_OFFICER));
    }

    @Test
    void invalidStorageDraftAndEarlyNoShowLeaveBookedCaseUntouched() throws Exception {
        Path storePath = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(storePath);
        SlotId slot = SlotId.of(new UUID(0, 30));
        ClaimId claim = ClaimId.of(new UUID(0, 31));
        AppointmentId appointment = AppointmentId.of(new UUID(0, 32));
        Instant start = NOW.plusSeconds(3600);
        repository.createSlot(CollectionSlot.create(slot, start, NOW, "officer-a"));
        repository.book(claim, "student-a", slot, appointment, "student-a",
                UserRole.STUDENT, NOW);
        byte[] before = Files.readAllBytes(storePath);

        runOnFxThread(() -> {
            OfficerAppointmentPane pane = new OfficerAppointmentPane(service(repository, 40));
            pane.enter();
            @SuppressWarnings("unchecked")
            ListView<CollectionCase> cases = (ListView<CollectionCase>) descendants(pane)
                    .filter(ListView.class::isInstance).skip(1).findFirst().orElseThrow();
            cases.getSelectionModel().selectFirst();
            TextField location = descendants(pane).filter(TextField.class::isInstance)
                    .map(TextField.class::cast).findFirst().orElseThrow();
            location.setText("X".repeat(121));
            AtomicReference<Throwable> tooLong = respond("Storage location is too long",
                    "OK", dialog -> assertTrue(dialog.getContentText().contains("120")));
            button(pane, "Record storage location").fire();
            assertNull(tooLong.get());
            assertTrue(pane.hasUnsavedText());

            AtomicReference<Throwable> early = respond("Cannot record NO_SHOW",
                    "OK", dialog -> assertTrue(dialog.getContentText().contains("30-minute")));
            button(pane, "Record NO_SHOW").fire();
            assertNull(early.get());
            return null;
        });
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(storePath)));

        runOnFxThread(() -> {
            OfficerAppointmentPane pane = new OfficerAppointmentPane(
                    service(repository, 41, start.plusSeconds(1800)));
            pane.enter();
            @SuppressWarnings("unchecked")
            ListView<CollectionCase> cases = (ListView<CollectionCase>) descendants(pane)
                    .filter(ListView.class::isInstance).skip(1).findFirst().orElseThrow();
            cases.getSelectionModel().selectFirst();
            button(pane, "Record NO_SHOW").fire();
            assertTrue(labels(pane).contains("NO_SHOW recorded."));
            assertFalse(button(pane, "Record NO_SHOW").isVisible());
            return null;
        });
        assertEquals(io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus.NO_SHOW,
                new JsonAppointmentRepository(storePath).loadCases().getFirst()
                        .appointments().getFirst().status());
    }

    @Test
    void corruptStoreDisablesActionsUntilRefreshSucceeds() throws Exception {
        Path storePath = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(storePath);
        repository.createSlot(CollectionSlot.create(SlotId.of(new UUID(0, 50)),
                NOW.plusSeconds(3600), NOW, "officer-a"));
        byte[] good = Files.readAllBytes(storePath);
        Files.writeString(storePath, "invalid JSON");
        byte[] bad = Files.readAllBytes(storePath);
        runOnFxThread(() -> {
            OfficerAppointmentPane pane = new OfficerAppointmentPane(service(repository, 51));
            pane.enter();
            @SuppressWarnings("unchecked")
            ListView<CollectionSlot> slots = (ListView<CollectionSlot>) descendants(pane)
                    .filter(ListView.class::isInstance).findFirst().orElseThrow();
            assertTrue(slots.getItems().isEmpty());
            assertTrue(labels(pane).contains("unavailable"));
            assertTrue(java.util.Arrays.equals(bad, Files.readAllBytes(storePath)));
            Files.write(storePath, good);
            button(pane, "Refresh").fire();
            assertEquals(1, slots.getItems().size());
            assertFalse(labels(pane).contains("unavailable"));
            return null;
        });
    }

    @Test
    void renderedSlotCaseAndAuditRowsShowTimeCustodyAndActorRole()
            throws Exception {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId slot = SlotId.of(new UUID(0, 60));
        ClaimId claim = ClaimId.of(new UUID(0, 61));
        repository.createSlot(CollectionSlot.create(slot, NOW.plusSeconds(3600),
                NOW, "officer-a"));
        repository.book(claim, "student-a", slot, AppointmentId.of(new UUID(0, 62)),
                "student-a", UserRole.STUDENT, NOW);
        runOnFxThread(() -> {
            OfficerAppointmentPane pane = new OfficerAppointmentPane(service(repository, 63));
            pane.enter();
            Stage stage = new Stage();
            try {
                stage.setScene(new Scene(pane, 900, 800));
                stage.show();
                pane.applyCss();
                pane.layout();
                @SuppressWarnings("unchecked")
                ListView<CollectionCase> cases = (ListView<CollectionCase>) descendants(pane)
                        .filter(ListView.class::isInstance).skip(1).findFirst().orElseThrow();
                cases.getSelectionModel().selectFirst();
                pane.applyCss();
                pane.layout();
                ListView<?> slots = descendants(pane).filter(ListView.class::isInstance)
                        .map(node -> (ListView<?>) node).findFirst().orElseThrow();
                ListView<?> audit = descendants(pane).filter(ListView.class::isInstance)
                        .skip(2).map(node -> (ListView<?>) node).findFirst().orElseThrow();
                assertTrue(renderedRowText(slots).contains("01 Jan 2030, 09:00"));
                assertTrue(renderedRowText(cases).contains("AWAITING_STORAGE"));
                assertTrue(renderedRowText(audit).contains("Student student-a"));
                return null;
            } finally {
                stage.close();
            }
        });
    }

    private static String renderedRowText(ListView<?> list) {
        return descendants(list).filter(ListCell.class::isInstance)
                .map(node -> (ListCell<?>) node).filter(cell -> !cell.isEmpty())
                .map(cell -> cell.getText()).reduce("", (left, right) -> left + " " + right);
    }

    private static OfficerAppointmentService service(JsonAppointmentRepository repository,
            long id) {
        return service(repository, id, NOW);
    }

    private static OfficerAppointmentService service(JsonAppointmentRepository repository,
            long id, Instant time) {
        AuthenticatedUser officer = new AuthenticatedUser("officer-a", "desk.a",
                UserRole.DESK_OFFICER);
        return new OfficerAppointmentService(officer, repository,
                Clock.fixed(time, ZoneOffset.UTC), () -> new UUID(0, id));
    }

    private static Stage showingDialog() {
        return Window.getWindows().stream().filter(Window::isShowing)
                .filter(Stage.class::isInstance).map(Stage.class::cast)
                .filter(stage -> stage.getScene().getRoot() instanceof DialogPane)
                .findFirst().orElseThrow();
    }

    private static Button button(Node root, String text) {
        return descendants(root).filter(Button.class::isInstance).map(Button.class::cast)
                .filter(value -> value.getText().equals(text)).findFirst().orElseThrow();
    }

    private static String labelText(Node root) {
        return descendants(root).filter(Label.class::isInstance).map(Label.class::cast)
                .map(Label::getText).reduce("", (left, right) -> left + " " + right);
    }

    private static String labels(Node root) {
        return labelText(root);
    }

    private static Stream<Node> descendants(Node root) {
        Stream<Node> children = Stream.empty();
        if (root instanceof ScrollPane scroll) {
            children = Stream.of(scroll.getContent());
        } else if (root instanceof Parent parent) {
            children = parent.getChildrenUnmodifiable().stream();
        }
        return Stream.concat(Stream.of(root), children.flatMap(OfficerAppointmentPaneTest::descendants));
    }
}
