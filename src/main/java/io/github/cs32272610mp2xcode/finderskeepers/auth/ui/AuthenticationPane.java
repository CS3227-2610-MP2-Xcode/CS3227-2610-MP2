package io.github.cs32272610mp2xcode.finderskeepers.auth.ui;

import java.util.Objects;
import java.util.function.Supplier;

import io.github.cs32272610mp2xcode.finderskeepers.AppMetadata;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationCoordinator;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationResult;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationStatus;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.ApplicationRoute;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Login and role-navigation pane ready for application-shell integration. */
public final class AuthenticationPane extends StackPane {
    private final AuthenticationCoordinator coordinator;

    private final Supplier<? extends Node> deskOfficerContentFactory;

    /**
     * Creates and displays the login interface.
     *
     * @param authenticationCoordinator authentication and navigation module
     * @param officerContentFactory lazy factory for authenticated Desk Officer content
     */
    public AuthenticationPane(AuthenticationCoordinator authenticationCoordinator,
            Supplier<? extends Node> officerContentFactory) {
        coordinator = Objects.requireNonNull(
                authenticationCoordinator, "authenticationCoordinator");
        deskOfficerContentFactory = Objects.requireNonNull(
                officerContentFactory, "officerContentFactory");
        getStyleClass().add("app-root");
        showLogin();
    }

    private void showLogin() {
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Label error = new Label();
        error.getStyleClass().add("login-error");

        Button login = new Button("Log in");
        login.setDefaultButton(true);
        login.setOnAction(event -> {
            AuthenticationResult result = coordinator.login(
                    username.getText(), password.getText().toCharArray());
            error.setText(coordinator.message());
            if (result.status() == AuthenticationStatus.SUCCESS) {
                password.clear();
                showAuthenticatedRoute();
            }
        });

        Button clear = new Button("Clear");
        clear.setOnAction(event -> {
            username.clear();
            password.clear();
            coordinator.clearValidation();
            error.setText("");
            username.requestFocus();
        });

        HBox actions = new HBox(10, login, clear);
        actions.setAlignment(Pos.CENTER);
        VBox form = new VBox(12,
                heading(AppMetadata.NAME),
                new Label("Log in to continue"),
                username,
                password,
                error,
                actions);
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(360);
        form.setPadding(new Insets(32));
        getChildren().setAll(form);
    }

    private void showAuthenticatedRoute() {
        AuthenticatedUser user = coordinator.currentUser().orElseThrow();
        RoutePresentation presentation = RoutePresentation.forRoute(coordinator.route());
        Label identity = new Label("Signed in as " + user.username());
        identity.getStyleClass().add("status-label");
        Button logout = new Button("Log out");
        logout.setOnAction(event -> {
            coordinator.logout();
            showLogin();
        });
        VBox view = new VBox(12,
                heading(presentation.title()),
                identity,
                new Label(presentation.description()));
        if (coordinator.route() == ApplicationRoute.DESK_OFFICER) {
            Node officerContent = Objects.requireNonNull(
                    deskOfficerContentFactory.get(), "officerContentFactory result");
            VBox.setVgrow(officerContent, Priority.ALWAYS);
            view.getChildren().add(officerContent);
        }
        view.getChildren().add(logout);
        view.setAlignment(Pos.TOP_CENTER);
        view.setPadding(new Insets(20));
        getChildren().setAll(view);
    }

    private static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("app-title");
        return label;
    }
}
