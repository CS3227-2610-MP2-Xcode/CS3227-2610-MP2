package io.github.cs32272610mp2xcode.finderskeepers;

import java.util.Objects;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
        Label title = new Label(AppMetadata.NAME);
        title.getStyleClass().add("app-title");

        Label status = new Label("Project scaffold ready");
        status.getStyleClass().add("status-label");

        Label message = new Label(
                "Community Member and Desk Officer features will be added in upcoming sprints.");
        message.setWrapText(true);

        VBox content = new VBox(12, title, status, message);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(32));
        content.getStyleClass().add("app-root");

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
