package io.github.cs32272610mp2xcode.finderskeepers.appointment.ui;

import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.runOnFxThread;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.startToolkit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.StudentAppointmentService;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.StudentActiveAppointmentSummary;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.JsonAppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.ApprovedClaimSummary;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentApprovedClaimService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.ScrollPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudentAppointmentPaneWorkflowTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        startToolkit();
    }

    @Test
    void bookingReschedulingAndCancellationRefreshVisibleAttemptsAndDurableState()
            throws Exception {
        Path appointmentPath = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository appointments = new JsonAppointmentRepository(appointmentPath);
        SlotId first = SlotId.of(new UUID(0, 1));
        SlotId second = SlotId.of(new UUID(0, 2));
        Instant firstTime = NOW.plusSeconds(3600);
        Instant secondTime = NOW.plusSeconds(5400);
        appointments.createSlot(CollectionSlot.create(first, firstTime, NOW, "officer-a"));
        appointments.createSlot(CollectionSlot.create(second, secondTime, NOW, "officer-a"));
        JsonClaimRepository claims = new JsonClaimRepository(
                temporaryDirectory.resolve("claims.json"));
        ClaimId claimId = ClaimId.of(new UUID(0, 10));
        claims.submit(Claim.createPending(claimId, "student-a", new UUID(0, 11),
                new UUID(0, 12), "Synthetic identifying mark", NOW.minusSeconds(2)));
        claims.approve(claimId, Optional.empty(), NOW.minusSeconds(1));
        AuthenticatedUser student = new AuthenticatedUser("student-a", "student.a",
                UserRole.STUDENT);
        StudentAppointmentService service = new StudentAppointmentService(student,
                appointments, new StudentApprovedClaimService(student, claims),
                Clock.fixed(NOW, ZoneOffset.UTC), () -> new UUID(0, 20));

        runOnFxThread(() -> {
            StudentAppointmentPane pane = new StudentAppointmentPane(service);
            pane.enter();
            @SuppressWarnings("unchecked")
            ComboBox<ApprovedClaimSummary> availableClaims =
                    (ComboBox<ApprovedClaimSummary>) descendants(pane)
                            .filter(ComboBox.class::isInstance).findFirst().orElseThrow();
            List<? extends ListView<?>> lists = descendants(pane).filter(ListView.class::isInstance)
                    .map(node -> (ListView<?>) node).toList();
            @SuppressWarnings("unchecked")
            ListView<CollectionSlot> slots = (ListView<CollectionSlot>) lists.get(0);
            @SuppressWarnings("unchecked")
            ListView<StudentActiveAppointmentSummary> active =
                    (ListView<StudentActiveAppointmentSummary>) lists.get(1);
            assertEquals(1, availableClaims.getItems().size());
            assertEquals(2, slots.getItems().size());
            availableClaims.setValue(availableClaims.getItems().getFirst());
            slots.getSelectionModel().selectFirst();
            assertTrue(button(pane, "Book selected slot").isVisible());
            button(pane, "Book selected slot").fire();

            assertEquals(1, active.getItems().size());
            assertEquals(firstTime, active.getItems().getFirst().startsAt());
            assertTrue(labels(pane).contains("Appointment booked for 01 Jan 2030, 09:00."));
            Stage stage = new Stage();
            try {
                stage.setScene(new Scene(pane, 900, 800));
                stage.show();
                pane.applyCss();
                pane.layout();
                assertTrue(availableClaims.getButtonCell().getText()
                        .contains(claimId.shortReference()));
                assertTrue(renderedRowText(active).contains("BOOKED"));
                assertTrue(renderedRowText(lists.get(2)).contains("Attempt 1"));
            } finally {
                stage.close();
            }
            active.getSelectionModel().selectFirst();
            slots.getSelectionModel().selectFirst();
            assertEquals(second, slots.getItems().getFirst().slotId());
            assertTrue(button(pane, "Reschedule to selected slot").isVisible());
            button(pane, "Reschedule to selected slot").fire();
            assertEquals(secondTime, active.getItems().getFirst().startsAt());
            assertTrue(labels(pane).contains("Appointment rescheduled."));

            active.getSelectionModel().selectFirst();
            assertTrue(button(pane, "Cancel active appointment").isVisible());
            button(pane, "Cancel active appointment").fire();
            assertTrue(active.getItems().isEmpty());
            assertTrue(labels(pane).contains("Appointment cancelled."));
            assertFalse(pane.hasUnsavedText());
            pane.clearSessionState();
            assertTrue(availableClaims.getItems().isEmpty());
            return null;
        });

        var reopened = new JsonAppointmentRepository(appointmentPath).loadCases().getFirst();
        assertEquals(AppointmentStatus.CANCELLED,
                reopened.appointments().getFirst().status());
        assertEquals(second, reopened.appointments().getFirst().slotId());
    }

    @Test
    void emptyClaimAndSlotStatesThenCorruptStoreRetryRemainAccountSpecific()
            throws Exception {
        Path storePath = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository appointments = new JsonAppointmentRepository(storePath);
        JsonClaimRepository claims = new JsonClaimRepository(
                temporaryDirectory.resolve("claims.json"));
        AuthenticatedUser a = new AuthenticatedUser("student-a", "student.a",
                UserRole.STUDENT);
        AuthenticatedUser b = new AuthenticatedUser("student-b", "student.b",
                UserRole.STUDENT);
        StudentAppointmentService serviceA = new StudentAppointmentService(a,
                appointments, new StudentApprovedClaimService(a, claims),
                Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID);
        StudentAppointmentService serviceB = new StudentAppointmentService(b,
                appointments, new StudentApprovedClaimService(b, claims),
                Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID);

        runOnFxThread(() -> {
            StudentAppointmentPane empty = new StudentAppointmentPane(serviceA);
            empty.enter();
            assertTrue(labels(empty).contains("No approved Claims"));
            assertFalse(button(empty, "Book selected slot").isVisible());
            return null;
        });
        ClaimId claimId = ClaimId.of(new UUID(0, 70));
        claims.submit(Claim.createPending(claimId, "student-a", new UUID(0, 71),
                new UUID(0, 72), "Synthetic identifying mark", NOW.minusSeconds(2)));
        claims.approve(claimId, Optional.empty(), NOW.minusSeconds(1));
        runOnFxThread(() -> {
            StudentAppointmentPane aPane = new StudentAppointmentPane(serviceA);
            StudentAppointmentPane bPane = new StudentAppointmentPane(serviceB);
            aPane.enter();
            bPane.enter();
            assertTrue(labels(aPane).contains("No collection slots"));
            assertTrue(labels(bPane).contains("No approved Claims"));
            @SuppressWarnings("unchecked")
            ComboBox<ApprovedClaimSummary> aClaims =
                    (ComboBox<ApprovedClaimSummary>) descendants(aPane)
                            .filter(ComboBox.class::isInstance).findFirst().orElseThrow();
            @SuppressWarnings("unchecked")
            ComboBox<ApprovedClaimSummary> bClaims =
                    (ComboBox<ApprovedClaimSummary>) descendants(bPane)
                            .filter(ComboBox.class::isInstance).findFirst().orElseThrow();
            assertEquals(1, aClaims.getItems().size());
            assertTrue(bClaims.getItems().isEmpty());
            return null;
        });

        appointments.createSlot(CollectionSlot.create(SlotId.of(new UUID(0, 73)),
                NOW.plusSeconds(3600), NOW, "officer-a"));
        byte[] valid = Files.readAllBytes(storePath);
        Files.writeString(storePath, "invalid JSON");
        byte[] invalid = Files.readAllBytes(storePath);
        runOnFxThread(() -> {
            StudentAppointmentPane pane = new StudentAppointmentPane(serviceA);
            pane.enter();
            assertTrue(labels(pane).contains("unavailable"));
            assertTrue(java.util.Arrays.equals(invalid, Files.readAllBytes(storePath)));
            Files.write(storePath, valid);
            button(pane, "Refresh").fire();
            assertFalse(labels(pane).contains("unavailable"));
            @SuppressWarnings("unchecked")
            ListView<CollectionSlot> slots = (ListView<CollectionSlot>) descendants(pane)
                    .filter(ListView.class::isInstance).findFirst().orElseThrow();
            assertEquals(1, slots.getItems().size());
            return null;
        });
    }

    private static Button button(Node root, String text) {
        return descendants(root).filter(Button.class::isInstance).map(Button.class::cast)
                .filter(value -> value.getText().equals(text)).findFirst().orElseThrow();
    }

    private static String renderedRowText(ListView<?> list) {
        return descendants(list).filter(ListCell.class::isInstance)
                .map(node -> (ListCell<?>) node).filter(cell -> !cell.isEmpty())
                .map(cell -> cell.getText()).reduce("", (left, right) -> left + " " + right);
    }

    private static String labels(Node root) {
        return descendants(root).filter(Label.class::isInstance).map(Label.class::cast)
                .map(Label::getText).reduce("", (left, right) -> left + " " + right);
    }

    private static Stream<Node> descendants(Node root) {
        Stream<Node> children = Stream.empty();
        if (root instanceof ScrollPane scroll) {
            children = Stream.of(scroll.getContent());
        } else if (root instanceof Parent parent) {
            children = parent.getChildrenUnmodifiable().stream();
        }
        return Stream.concat(Stream.of(root), children.flatMap(StudentAppointmentPaneWorkflowTest::descendants));
    }
}
