package io.github.cs32272610mp2xcode.finderskeepers.testsupport;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Answers a modal JavaFX dialog on the next event-loop turn without sleeps. */
public final class FxTestDialogs {
    private FxTestDialogs() {
    }

    /** Queues a response and captures assertion errors for the calling test. */
    public static AtomicReference<Throwable> respond(String title, String buttonText,
            Consumer<DialogPane> assertions) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            Stage dialog = Window.getWindows().stream().filter(Window::isShowing)
                    .filter(Stage.class::isInstance).map(Stage.class::cast)
                    .filter(stage -> stage.getScene().getRoot() instanceof DialogPane)
                    .findFirst().orElseThrow();
            try {
                if (!dialog.getTitle().equals(title)) {
                    throw new AssertionError("Unexpected dialog: " + dialog.getTitle());
                }
                DialogPane pane = (DialogPane) dialog.getScene().getRoot();
                assertions.accept(pane);
                ButtonType action = pane.getButtonTypes().stream()
                        .filter(button -> button.getText().equals(buttonText))
                        .findFirst().orElseThrow();
                ((Button) pane.lookupButton(action)).fire();
            } catch (AssertionError | java.util.NoSuchElementException
                    | ClassCastException | IllegalStateException problem) {
                failure.set(problem);
                dialog.close();
            }
        });
        return failure;
    }
}
