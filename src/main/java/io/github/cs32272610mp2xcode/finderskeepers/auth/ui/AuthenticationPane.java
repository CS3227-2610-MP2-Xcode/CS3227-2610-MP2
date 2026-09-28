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
        login.getStyleClass().add("primary-button");
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
        clear.getStyleClass().add("quiet-button");
        clear.setOnAction(event -> {
            username.clear();
            password.clear();
            coordinator.clearValidation();
            error.setText("");
            username.requestFocus();
        });

        HBox actions = new HBox(10, login, clear);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label formHeading = new Label("Welcome back");
        formHeading.getStyleClass().add("login-heading");
        Label formCopy = new Label("Sign in to continue to your workspace.");
        formCopy.getStyleClass().add("muted-text");
        VBox form = new VBox(14,
                formHeading,
                formCopy,
                field("Username", username),
                field("Password", password),
                error,
                actions);
        form.getStyleClass().add("login-card");
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPrefWidth(430);

        VBox brand = loginBrand();
        HBox shell = new HBox(brand, form);
        shell.getStyleClass().add("login-shell");
        shell.setMaxWidth(940);
        shell.setMaxHeight(560);
        getChildren().setAll(shell);
    }

    private void showAuthenticatedRoute() {
        AuthenticatedUser user = coordinator.currentUser().orElseThrow();
        if (coordinator.route() == ApplicationRoute.STUDENT
                && studentViewFactory != null) {
            showStudentWorkspace(user, studentViewFactory.apply(user));
            return;
        }
        RoutePresentation presentation = RoutePresentation.forRoute(coordinator.route());
        Button logout = new Button("Log out");
        logout.getStyleClass().add("quiet-button");
        VBox emptyState = new VBox(12,
                heading(presentation.title()),
                new Label(presentation.description()));
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(32));
        Node officerContent = null;
        if (coordinator.route() == ApplicationRoute.DESK_OFFICER
                && deskOfficerContentFactory != null) {
            officerContent = Objects.requireNonNull(
                    deskOfficerContentFactory.apply(user),
                    "officerContentFactory result");
        }
        Node sessionContent = officerContent;
        logout.setOnAction(event -> requestLogout(sessionContent));
        BorderPane view = new BorderPane(officerContent == null ? emptyState : officerContent);
        view.getStyleClass().add("workspace");
        view.setTop(sessionHeader(user, presentation.title(), logout));
        getChildren().setAll(view);
    }

    private void showStudentWorkspace(AuthenticatedUser user, Node workspace) {
        Objects.requireNonNull(workspace, "studentFactory result");
        Button logout = new Button("Log out");
        logout.getStyleClass().add("quiet-button");
        logout.setOnAction(event -> requestLogout(workspace));
        BorderPane view = new BorderPane(workspace);
        view.getStyleClass().add("workspace");
        view.setTop(sessionHeader(user, AppMetadata.STUDENT_ROLE, logout));
        getChildren().setAll(view);
    }

    private static VBox loginBrand() {
        Label mark = new Label("F");
        mark.getStyleClass().add("brand-mark");
        Label title = new Label(AppMetadata.NAME);
        title.getStyleClass().add("brand-title");
        Label copy = new Label("A safer, simpler way for your school community\n"
                + "to reunite belongings with their owners.");
        copy.getStyleClass().add("brand-copy");
        Label report = brandFeature("Report lost or found items");
        Label track = brandFeature("Track Claims and collection updates");
        Label protect = brandFeature("Keep ownership evidence private");
        VBox brand = new VBox(18, mark, title, copy, report, track, protect);
        brand.getStyleClass().add("login-brand-panel");
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPrefWidth(510);
        return brand;
    }

    private static Label brandFeature(String text) {
        Label label = new Label("\u2022  " + text);
        label.getStyleClass().add("brand-feature");
        return label;
    }

    private static VBox field(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        return new VBox(6, label, control);
    }

    private static HBox sessionHeader(AuthenticatedUser user, String role, Button logout) {
        Label mark = new Label("F");
        mark.getStyleClass().add("brand-mark-small");
        Label brand = new Label(AppMetadata.NAME);
        brand.getStyleClass().add("top-bar-brand");
        Label route = new Label(role);
        route.getStyleClass().add("top-bar-role");
        VBox titles = new VBox(1, brand, route);
        Label identity = new Label(user.username());
        identity.getStyleClass().add("status-label");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, mark, titles, spacer, identity, logout);
        header.getStyleClass().add("top-bar");
        return header;
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
