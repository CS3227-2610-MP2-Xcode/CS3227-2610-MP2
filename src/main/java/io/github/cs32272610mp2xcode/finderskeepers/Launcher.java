package io.github.cs32272610mp2xcode.finderskeepers;

import javafx.application.Application;

/** Java entry point that launches the JavaFX application. */
public final class Launcher {
    private Launcher() {
    }

    /**
     * Starts Finders Keepers.
     *
     * @param args command-line arguments passed to JavaFX
     */
    public static void main(String[] args) {
        Application.launch(FindersKeepersApp.class, args);
    }
}
