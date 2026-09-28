package io.github.cs32272610mp2xcode.finderskeepers.appointment.bootstrap;

import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.descendants;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.labels;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.runOnFxThread;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.startToolkit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.JsonAppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.ui.OfficerAppointmentPane;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.ui.StudentAppointmentPane;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.bootstrap.ClaimWorkspaceFactory;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.FilePossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.workspace.WorkspaceFeature;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppointmentWorkspaceFactoryIntegrationTest {
    @TempDir
    private Path temporaryDirectory;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        startToolkit();
    }

    @Test
    void freshRoleBoundFeaturesReadSharedStoreAndClearPerSessionDraft()
            throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
        JsonAppointmentRepository appointments = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        ClaimWorkspaceFactory claims = new ClaimWorkspaceFactory(
                new JsonClaimRepository(temporaryDirectory.resolve("claims.json")),
                new JsonReportRepository(temporaryDirectory.resolve("reports.json")),
                new FilePossibleMatchRepository(temporaryDirectory.resolve("matches.txt")),
                clock, UUID::randomUUID);
        AppointmentWorkspaceFactory factory = new AppointmentWorkspaceFactory(
                appointments, claims, clock, UUID::randomUUID);
        AuthenticatedUser officer = new AuthenticatedUser("officer-a", "desk.a",
                UserRole.DESK_OFFICER);
        AuthenticatedUser student = new AuthenticatedUser("student-a", "student.a",
                UserRole.STUDENT);

        runOnFxThread(() -> {
            WorkspaceFeature officerFeature = factory.createOfficerFeature(officer);
            assertTrue(officerFeature.content() instanceof OfficerAppointmentPane);
            officerFeature.onEnter().run();
            TextField location = descendants(officerFeature.content())
                    .filter(TextField.class::isInstance).map(TextField.class::cast)
                    .findFirst().orElseThrow();
            location.setText("Unsaved synthetic shelf");
            assertTrue(officerFeature.hasUnsavedText().getAsBoolean());
            officerFeature.clearSessionState().run();
            assertFalse(officerFeature.hasUnsavedText().getAsBoolean());

            WorkspaceFeature studentFeature = factory.createStudentFeature(student);
            assertTrue(studentFeature.content() instanceof StudentAppointmentPane);
            studentFeature.onEnter().run();
            assertTrue(labels(studentFeature.content()).contains("No approved Claims"));
            assertFalse(studentFeature.hasUnsavedText().getAsBoolean());
            studentFeature.clearSessionState().run();
            assertEquals(0, appointments.loadCases().size());
            return null;
        });
        assertThrows(IllegalArgumentException.class,
                () -> factory.createStudentFeature(officer));
        assertThrows(IllegalArgumentException.class,
                () -> factory.createOfficerFeature(student));
    }
}
