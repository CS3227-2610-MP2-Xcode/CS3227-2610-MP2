package io.github.cs32272610mp2xcode.finderskeepers;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.auth.bootstrap.AuthenticationFactory;
import io.github.cs32272610mp2xcode.finderskeepers.auth.ui.AuthenticationPane;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.bootstrap.AppointmentWorkspaceFactory;
import io.github.cs32272610mp2xcode.finderskeepers.claim.bootstrap.ClaimWorkspaceFactory;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.FilePossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.PossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.bootstrap.StudentReportWorkspaceFactory;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.review.ui.DeskOfficerWorkspacePane;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

/** Initial JavaFX application shell for Finders Keepers. */
public final class FindersKeepersApp extends Application {
    private static final String SMOKE_TEST_ARGUMENT = "--smoke-test";

    private static final Duration SMOKE_TEST_DURATION = Duration.millis(750);

    private PauseTransition smokeTestExit;

    /** Creates the JavaFX application instance. */
    public FindersKeepersApp() {
    }

    /** Creates and displays the initial project window. */
    @Override
    public void start(Stage stage) {
        ReportRepository reportRepository = new JsonReportRepository(
                Path.of("data", "reports.json"));
        PossibleMatchRepository possibleMatchRepository =
                new FilePossibleMatchRepository(
                        Path.of("data", "possible-match-links.txt"));
        ClaimRepository claimRepository = new JsonClaimRepository(
                Path.of("data", "claims.json"));
        ClaimWorkspaceFactory claimWorkspaceFactory = new ClaimWorkspaceFactory(
                claimRepository, reportRepository, possibleMatchRepository,
                Clock.systemUTC(), UUID::randomUUID);
        AppointmentWorkspaceFactory appointmentWorkspaceFactory =
                new AppointmentWorkspaceFactory(Path.of("data", "appointments.json"),
                        claimWorkspaceFactory);
        AuthenticationPane content = new AuthenticationPane(
                AuthenticationFactory.createCoordinator(
                        Path.of("data", "demo-users.json")),
                StudentReportWorkspaceFactory.create(
                        reportRepository,
                        claimWorkspaceFactory::createStudentFeature,
                        appointmentWorkspaceFactory::createStudentFeature),
                user -> new DeskOfficerWorkspacePane(
                        reportRepository,
                        possibleMatchRepository,
                        claimWorkspaceFactory.createDeskOfficerFeature(user),
                        appointmentWorkspaceFactory.createOfficerFeature(user)));
        Scene scene = new Scene(content, 720, 420);
        scene.getStylesheets().add(Objects.requireNonNull(
                FindersKeepersApp.class.getResource("app.css")).toExternalForm());

        stage.setTitle(AppMetadata.NAME);
        stage.setMinWidth(560);
        stage.setMinHeight(320);
        stage.setScene(scene);
        stage.show();

        if (getParameters().getRaw().contains(SMOKE_TEST_ARGUMENT)) {
            smokeTestExit = new PauseTransition(SMOKE_TEST_DURATION);
            smokeTestExit.setOnFinished(event -> Platform.exit());
            smokeTestExit.play();
        }
    }

    /** Stops any pending automated smoke-test exit. */
    @Override
    public void stop() {
        if (smokeTestExit != null) {
            smokeTestExit.stop();
        }
    }
}
