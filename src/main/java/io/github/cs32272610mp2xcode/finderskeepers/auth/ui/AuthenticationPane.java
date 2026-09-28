package io.github.cs32272610mp2xcode.finderskeepers.auth.ui;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.cs32272610mp2xcode.finderskeepers.AppMetadata;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.ApplicationRoute;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationCoordinator;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationResult;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationStatus;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.RegistrationResult;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.RegistrationStatus;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.workspace.SessionView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Login, registration, and role-navigation pane for the application shell.
 *
 * <p>The pane opens in Production mode. Its top-right mode control switches to
 * one-click demonstration login. Successful authentication routes by the
 * stored account role, while logout returns to the mode used for that
 * session.</p>
 */
public final class AuthenticationPane extends StackPane {
    private static final String DEMO_STUDENT_USERNAME = "demo.student";

    private static final String DEMO_STUDENT_PASSWORD = "Student-Demo-27!";

    private static final String DEMO_OFFICER_USERNAME = "demo.officer";

    private static final String DEMO_OFFICER_PASSWORD = "Officer-Demo-42!";

    private final AuthenticationCoordinator demoCoordinator;

    private final AuthenticationCoordinator productionCoordinator;

    private final Function<AuthenticatedUser, Node> studentViewFactory;

    private final Function<AuthenticatedUser, Node> deskOfficerContentFactory;

    private LoginMode loginMode = LoginMode.PRODUCTION;

    /**
     * Creates the login interface using one coordinator for both modes.
     * This convenience overload is intended for compositions that do not need
     * separate demo and production stores.
     *
     * @param authenticationCoordinator authentication and navigation module
     */
    public AuthenticationPane(AuthenticationCoordinator authenticationCoordinator) {
        this(authenticationCoordinator, null,
                (Function<AuthenticatedUser, Node>) null);
    }

    /**
     * Creates authentication with an injected Student workspace and one
     * coordinator shared by both login modes.
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
     * Creates authentication with injected Desk Officer content and one
     * coordinator shared by both login modes.
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
     * Creates authentication with injected role destinations and one
     * coordinator shared by both login modes.
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
     * Creates authentication with user-aware role destinations and one
     * coordinator shared by both login modes.
     *
     * @param authenticationCoordinator authentication and navigation module
     * @param studentFactory factory for the authenticated Student workspace
     * @param officerContentFactory factory for the authenticated officer workspace
     */
    public AuthenticationPane(AuthenticationCoordinator authenticationCoordinator,
            Function<AuthenticatedUser, Node> studentFactory,
            Function<AuthenticatedUser, Node> officerContentFactory) {
        this(authenticationCoordinator, authenticationCoordinator,
                studentFactory, officerContentFactory);
    }

    /**
     * Creates authentication with separate demo and production account stores.
     * The initial screen uses the production coordinator; the mode control
     * selects the demonstration coordinator for one-click demo login.
     *
     * @param demonstrationCoordinator demo credential verifier
     * @param localCoordinator production login and registration coordinator
     * @param studentFactory factory for the authenticated Student workspace
     * @param officerContentFactory factory for the authenticated officer workspace
     */
    public AuthenticationPane(AuthenticationCoordinator demonstrationCoordinator,
            AuthenticationCoordinator localCoordinator,
            Function<AuthenticatedUser, Node> studentFactory,
            Function<AuthenticatedUser, Node> officerContentFactory) {
        demoCoordinator = Objects.requireNonNull(
                demonstrationCoordinator, "demonstrationCoordinator");
        productionCoordinator = Objects.requireNonNull(
                localCoordinator, "localCoordinator");
        studentViewFactory = studentFactory;
        deskOfficerContentFactory = officerContentFactory;
        getStyleClass().add("app-root");
        showLogin();
    }

    private void showLogin() {
        VBox form = loginMode == LoginMode.DEMO
                ? demoLoginForm() : productionLoginForm();
        showLoginScreen(form);
    }

    private VBox demoLoginForm() {
        Label error = loginFeedback();
        Button studentLogin = new Button("Log in as demo Student");
        studentLogin.getStyleClass().add("primary-button");
        studentLogin.setMaxWidth(Double.MAX_VALUE);
        studentLogin.setDefaultButton(true);
        studentLogin.setOnAction(event -> loginDemo(
                DEMO_STUDENT_USERNAME, DEMO_STUDENT_PASSWORD, error));

        Button officerLogin = new Button("Log in as demo Desk Officer");
        officerLogin.setMaxWidth(Double.MAX_VALUE);
        officerLogin.setOnAction(event -> loginDemo(
                DEMO_OFFICER_USERNAME, DEMO_OFFICER_PASSWORD, error));

        Label formHeading = new Label("Try the demonstration");
        formHeading.getStyleClass().add("login-heading");
        Label formCopy = new Label(
                "Choose a role. The public demo credentials are filled in for you.");
        formCopy.getStyleClass().add("muted-text");
        formCopy.setWrapText(true);
        VBox form = new VBox(14, formHeading, formCopy,
                studentLogin, officerLogin, error);
        styleLoginCard(form);
        return form;
    }

    private VBox productionLoginForm() {
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Label error = loginFeedback();

        Button login = new Button("Log in");
        login.getStyleClass().add("primary-button");
        login.setDefaultButton(true);
        login.setOnAction(event -> {
            AuthenticationResult result = productionCoordinator.login(
                    username.getText(), password.getText().toCharArray());
            error.setText(productionCoordinator.message());
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
            productionCoordinator.clearValidation();
            error.setText("");
            username.requestFocus();
        });

        Button createAccount = new Button("Create account");
        createAccount.getStyleClass().add("quiet-button");
        createAccount.setOnAction(event -> showRegistration());

        HBox actions = new HBox(10, login, clear, createAccount);
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
        styleLoginCard(form);
        return form;
    }

    private void showRegistration() {
        TextField username = new TextField();
        username.setPromptText("Choose a username");
        PasswordField password = new PasswordField();
        password.setPromptText("Choose a password");
        PasswordField confirmation = new PasswordField();
        confirmation.setPromptText("Repeat your password");
        ComboBox<String> role = new ComboBox<>();
        role.getItems().setAll("Student", "Desk Officer");
        role.setValue("Student");
        Label error = loginFeedback();

        Button create = new Button("Create and log in");
        create.getStyleClass().add("primary-button");
        create.setDefaultButton(true);
        create.setOnAction(event -> {
            RegistrationResult result = productionCoordinator.register(
                            username.getText(), password.getText().toCharArray(),
                            confirmation.getText().toCharArray(), roleFromLabel(role.getValue()));
            error.setText(result.message());
            if (result.status() == RegistrationStatus.SUCCESS) {
                password.clear();
                confirmation.clear();
                showAuthenticatedRoute();
            }
        });

        Button back = new Button("Back to login");
        back.getStyleClass().add("quiet-button");
        back.setOnAction(event -> {
            productionCoordinator.clearValidation();
            showLogin();
        });

        Label formHeading = new Label("Create your account");
        formHeading.getStyleClass().add("login-heading");
        Label formCopy = new Label(
                "For this assignment, you may create a Student or Desk Officer account.");
        formCopy.getStyleClass().add("muted-text");
        formCopy.setWrapText(true);
        VBox form = new VBox(12,
                formHeading,
                formCopy,
                field("Username", username),
                field("Password", password),
                field("Confirm password", confirmation),
                field("Role", role),
                error,
                new HBox(10, create, back));
        styleLoginCard(form);
        showLoginScreen(form);
    }

    private void showLoginScreen(VBox form) {
        VBox brand = loginBrand();
        HBox shell = new HBox(brand, form);
        shell.getStyleClass().add("login-shell");
        shell.setMaxWidth(940);
        shell.setMaxHeight(560);

        Label mode = new Label(loginMode.label());
        mode.getStyleClass().add("login-mode-label");
        Button switchMode = new Button("\u21c4");
        switchMode.getStyleClass().addAll("icon-button", "mode-switch-button");
        switchMode.setAccessibleText("Switch login mode");
        switchMode.setTooltip(new Tooltip("Switch to " + loginMode.other().label()));
        switchMode.setOnAction(event -> {
            activeCoordinator().clearValidation();
            loginMode = loginMode.other();
            showLogin();
        });
        HBox modeBar = new HBox(8, mode, switchMode);
        modeBar.getStyleClass().add("login-mode-bar");
        modeBar.setAlignment(Pos.CENTER_RIGHT);

        BorderPane screen = new BorderPane();
        screen.setTop(modeBar);
        screen.setCenter(shell);
        getChildren().setAll(screen);
    }

    private void loginDemo(String username, String password, Label error) {
        AuthenticationResult result = demoCoordinator.login(
                username, password.toCharArray());
        error.setText(demoCoordinator.message());
        if (result.status() == AuthenticationStatus.SUCCESS) {
            showAuthenticatedRoute();
        }
    }

    private static Label loginFeedback() {
        Label feedback = new Label();
        feedback.getStyleClass().add("login-error");
        feedback.setWrapText(true);
        return feedback;
    }

    private static void styleLoginCard(VBox form) {
        form.getStyleClass().add("login-card");
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPrefWidth(430);
    }

    private static UserRole roleFromLabel(String role) {
        if ("Desk Officer".equals(role)) {
            return UserRole.DESK_OFFICER;
        }
        if ("Student".equals(role)) {
            return UserRole.STUDENT;
        }
        return null;
    }

    private AuthenticationCoordinator activeCoordinator() {
        return loginMode == LoginMode.DEMO ? demoCoordinator : productionCoordinator;
    }

    private void showAuthenticatedRoute() {
        AuthenticationCoordinator coordinator = activeCoordinator();
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
        activeCoordinator().logout();
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

    private enum LoginMode {
        DEMO("Demo mode"),
        PRODUCTION("Production mode");

        private final String label;

        LoginMode(String displayLabel) {
            label = displayLabel;
        }

        String label() {
            return label;
        }

        LoginMode other() {
            return this == DEMO ? PRODUCTION : DEMO;
        }
    }
}
