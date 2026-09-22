package io.github.cs32272610mp2xcode.finderskeepers.auth.ui;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.cs32272610mp2xcode.finderskeepers.AppMetadata;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.ApplicationRoute;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationCoordinator;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationResult;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationStatus;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.workspace.SessionView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Login and role-navigation pane ready for application-shell integration. */
public final class AuthenticationPane extends StackPane {
    private final AuthenticationCoordinator coordinator;

    private final Function<AuthenticatedUser, Node> studentViewFactory;

    private final Function<AuthenticatedUser, Node> deskOfficerContentFactory;

    /**
     * Creates and displays the login interface.
     *
     * @param authenticationCoordinator authentication and navigation module
     */
    public AuthenticationPane(AuthenticationCoordinator authenticationCoordinator) {
        this(authenticationCoordinator, null,
                (Function<AuthenticatedUser, Node>) null);
    }

    /**
     * Creates and displays authentication with an injected Student workspace.
     * Authentication owns login, session state, role routing, and logout;
     * the supplied factory owns construction of the Student feature view.
     *
     * @param authenticationCoordinator authentication and navigation module
     * @param studentFactory factory for the authenticated Student workspace
     */
    public AuthenticationPane(AuthenticationCoordinator authenticationCoordinator,
            Function<AuthenticatedUser, Node> studentFactory) {
        this(authenticationCoordinator, studentFactory,
                (Function<AuthenticatedUser, Node>) null);
    }

    /**
     * Creates and displays authentication with injected Desk Officer content.
     *
     * @param authenticationCoordinator authentication and navigation module
     * @param officerContentFactory lazy factory for Desk Officer content
     */
    public AuthenticationPane(AuthenticationCoordinator authenticationCoordinator,
            Supplier<? extends Node> officerContentFactory) {
        this(authenticationCoordinator, null, adaptOfficerFactory(
                Objects.requireNonNull(officerContentFactory, "officerContentFactory")));
    }

    /**
     * Creates and displays authentication with injected role destinations.
     * Authentication owns login, session state, role routing, and logout;
     * each supplied factory owns construction of its role-specific view.
     *
     * @param authenticationCoordinator authentication and navigation module
     * @param studentFactory factory for the authenticated Student workspace
     * @param officerContentFactory lazy factory for Desk Officer content
     */
    public AuthenticationPane(AuthenticationCoordinator authenticationCoordinator,
            Function<AuthenticatedUser, Node> studentFactory,
            Supplier<? extends Node> officerContentFactory) {
        this(authenticationCoordinator, studentFactory,
                officerContentFactory == null ? null : user -> officerContentFactory.get());
    }

    /**
     * Creates authentication with user-aware role destinations.
     *
     * @param authenticationCoordinator authentication and navigation module
     * @param studentFactory factory for the authenticated Student workspace
     * @param officerContentFactory factory for the authenticated officer workspace
     */
    public AuthenticationPane(AuthenticationCoordinator authenticationCoordinator,
            Function<AuthenticatedUser, Node> studentFactory,
            Function<AuthenticatedUser, Node> officerContentFactory) {
        coordinator = Objects.requireNonNull(
                authenticationCoordinator, "authenticationCoordinator");
        studentViewFactory = studentFactory;
        deskOfficerContentFactory = officerContentFactory;
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
        if (coordinator.route() == ApplicationRoute.STUDENT
                && studentViewFactory != null) {
            showStudentWorkspace(user, studentViewFactory.apply(user));
            return;
        }
        RoutePresentation presentation = RoutePresentation.forRoute(coordinator.route());
        Label identity = new Label("Signed in as " + user.username());
        identity.getStyleClass().add("status-label");
        Button logout = new Button("Log out");
        VBox view = new VBox(16,
                heading(presentation.title()),
                identity,
                new Label(presentation.description()));
        Node officerContent = null;
        if (coordinator.route() == ApplicationRoute.DESK_OFFICER
                && deskOfficerContentFactory != null) {
            officerContent = Objects.requireNonNull(
                    deskOfficerContentFactory.apply(user),
                    "officerContentFactory result");
            VBox.setVgrow(officerContent, Priority.ALWAYS);
            view.getChildren().add(officerContent);
            view.setAlignment(Pos.TOP_CENTER);
            view.setPadding(new Insets(20));
        } else {
            view.setAlignment(Pos.CENTER);
            view.setPadding(new Insets(32));
        }
        Node sessionContent = officerContent;
        logout.setOnAction(event -> requestLogout(sessionContent));
        view.getChildren().add(logout);
        getChildren().setAll(view);
    }

    private void showStudentWorkspace(AuthenticatedUser user, Node workspace) {
        Objects.requireNonNull(workspace, "studentFactory result");
        Label identity = new Label("Signed in as " + user.username());
        identity.getStyleClass().add("status-label");
        Button logout = new Button("Log out");
        logout.setOnAction(event -> requestLogout(workspace));
        HBox header = new HBox(16, identity, logout);
        header.setAlignment(Pos.CENTER_RIGHT);
        header.setPadding(new Insets(18, 24, 8, 24));
        BorderPane view = new BorderPane(workspace);
        view.setTop(header);
        getChildren().setAll(view);
    }

    private void requestLogout(Node workspace) {
        if (workspace instanceof SessionView session && session.hasUnsavedText()) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Discard unsaved text?");
            confirmation.setHeaderText("Log out and discard unsaved Claims text?");
            confirmation.setContentText(
                    "Unsaved ownership evidence or decision text will not be retained.");
            ButtonType logout = new ButtonType("Log out", ButtonBar.ButtonData.OK_DONE);
            confirmation.getButtonTypes().setAll(logout, ButtonType.CANCEL);
            if (confirmation.showAndWait().filter(logout::equals).isEmpty()) {
                return;
            }
        }
        if (workspace instanceof SessionView session) {
            session.clearSessionState();
        }
        coordinator.logout();
        showLogin();
    }

    private static Function<AuthenticatedUser, Node> adaptOfficerFactory(
            Supplier<? extends Node> factory) {
        return user -> factory.get();
    }

    private static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("app-title");
        return label;
    }
}
