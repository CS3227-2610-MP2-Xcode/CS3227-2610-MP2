package io.github.cs32272610mp2xcode.finderskeepers.auth.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.cs32272610mp2xcode.finderskeepers.auth.application.ApplicationRoute;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationCoordinator;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationService;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.RegistrationService;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordAlgorithm;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordCredential;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordHasher;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;

class AuthenticationPaneTest {
    private static final int FX_TIMEOUT_SECONDS = 10;

    private static final String STUDENT_PASSWORD = "Student-Demo-27!";

    private static final String OFFICER_PASSWORD = "Officer-Demo-42!";

    @BeforeAll
    static void startJavaFxToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        Platform.setImplicitExit(false);
    }

    @AfterAll
    static void stopJavaFxToolkit() {
        Platform.exit();
    }

    @Test
    void exercisesLoginModesRegistrationRoleRoutingAndLogout() throws Exception {
        runOnFxThread(() -> {
            TestPasswordHasher demoHasher = new TestPasswordHasher();
            MutableUserRepository demoRepository = new MutableUserRepository(List.of(
                    account("demo-student-id", "demo.student", UserRole.STUDENT,
                            STUDENT_PASSWORD, demoHasher),
                    account("demo-officer-id", "demo.officer", UserRole.DESK_OFFICER,
                            OFFICER_PASSWORD, demoHasher)));
            AuthenticationCoordinator demoCoordinator = coordinator(
                    demoRepository, demoHasher, new UUID(0L, 1L));

            TestPasswordHasher productionHasher = new TestPasswordHasher();
            MutableUserRepository productionRepository = new MutableUserRepository(List.of());
            AuthenticationCoordinator productionCoordinator = coordinator(
                    productionRepository, productionHasher, new UUID(0L, 2L));

            AuthenticationPane pane = new AuthenticationPane(
                    demoCoordinator,
                    productionCoordinator,
                    user -> new Label("Student workspace: " + user.username()),
                    user -> new Label("Officer workspace: " + user.username()));
            Stage stage = new Stage();
            try {
                stage.setScene(new Scene(pane, 1120, 760));
                stage.show();
                pane.applyCss();
                pane.layout();

                assertProductionLoginIsDefault(pane);
                switchMode(pane, "Demo mode");
                assertDemoLoginIsVisible(pane);

                button(pane, "Log in as demo Student").fire();
                assertEquals(ApplicationRoute.STUDENT, demoCoordinator.route());
                assertTrue(productionCoordinator.currentUser().isEmpty());
                assertTrue(hasLabel(pane, "Student workspace: demo.student"));
                button(pane, "Log out").fire();
                assertTrue(demoCoordinator.currentUser().isEmpty());
                assertTrue(hasLabel(pane, "Demo mode"));

                button(pane, "Log in as demo Desk Officer").fire();
                assertEquals(ApplicationRoute.DESK_OFFICER, demoCoordinator.route());
                assertTrue(hasLabel(pane, "Officer workspace: demo.officer"));
                button(pane, "Log out").fire();

                switchMode(pane, "Production mode");
                button(pane, "Create account").fire();
                assertTrue(hasLabel(pane, "Create your account"));

                input(pane, "Choose a username").setText("new.officer");
                input(pane, "Choose a password").setText("new officer password");
                input(pane, "Repeat your password").setText("does not match");
                roleSelector(pane).setValue("Desk Officer");
                button(pane, "Create and log in").fire();

                assertTrue(hasLabel(pane, "Passwords do not match."));
                assertTrue(productionCoordinator.currentUser().isEmpty());
                assertTrue(productionRepository.accounts().isEmpty());

                input(pane, "Repeat your password").setText("new officer password");
                button(pane, "Create and log in").fire();
                assertEquals(ApplicationRoute.DESK_OFFICER, productionCoordinator.route());
                assertTrue(demoCoordinator.currentUser().isEmpty());
                assertTrue(hasLabel(pane, "Officer workspace: new.officer"));
                assertEquals(1, productionRepository.accounts().size());

                button(pane, "Log out").fire();
                assertTrue(productionCoordinator.currentUser().isEmpty());
                assertProductionLoginIsDefault(pane);

                input(pane, "Username").setText("NEW.OFFICER");
                input(pane, "Password").setText("new officer password");
                button(pane, "Log in").fire();
                assertEquals(ApplicationRoute.DESK_OFFICER, productionCoordinator.route());
                assertTrue(hasLabel(pane, "Officer workspace: new.officer"));
                button(pane, "Log out").fire();

                input(pane, "Username").setText("temporary");
                input(pane, "Password").setText("temporary");
                button(pane, "Clear").fire();
                assertEquals("", input(pane, "Username").getText());
                assertEquals("", input(pane, "Password").getText());
            } finally {
                stage.close();
            }
            return null;
        });
    }

    private static void assertProductionLoginIsDefault(AuthenticationPane pane) {
        assertTrue(hasLabel(pane, "Production mode"));
        assertTrue(hasButton(pane, "Log in"));
        assertTrue(hasButton(pane, "Create account"));
        assertFalse(hasButton(pane, "Log in as demo Student"));
        Button modeSwitch = button(pane, "\u21c4");
        assertEquals("Switch login mode", modeSwitch.getAccessibleText());
        assertEquals("Switch to Demo mode", modeSwitch.getTooltip().getText());
    }

    private static void assertDemoLoginIsVisible(AuthenticationPane pane) {
        assertTrue(hasLabel(pane, "Demo mode"));
        assertTrue(hasButton(pane, "Log in as demo Student"));
        assertTrue(hasButton(pane, "Log in as demo Desk Officer"));
        assertFalse(hasButton(pane, "Create account"));
        assertEquals("Switch to Production mode",
                button(pane, "\u21c4").getTooltip().getText());
    }

    private static void switchMode(AuthenticationPane pane, String expectedMode) {
        button(pane, "\u21c4").fire();
        assertTrue(hasLabel(pane, expectedMode));
    }

    private static AuthenticationCoordinator coordinator(MutableUserRepository repository,
            TestPasswordHasher hasher, UUID generatedId) {
        return new AuthenticationCoordinator(
                new AuthenticationService(repository, hasher),
                new RegistrationService(repository, hasher, () -> generatedId));
    }

    private static UserAccount account(String userId, String username, UserRole role,
            String password, TestPasswordHasher hasher) {
        return new UserAccount(userId, username, role, hasher.hash(password.toCharArray()));
    }

    private static Button button(Node root, String text) {
        return find(root, Button.class, candidate -> text.equals(candidate.getText()));
    }

    private static boolean hasButton(Node root, String text) {
        return descendants(root)
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .anyMatch(candidate -> text.equals(candidate.getText()));
    }

    private static boolean hasLabel(Node root, String text) {
        return descendants(root)
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .anyMatch(candidate -> text.equals(candidate.getText()));
    }

    private static TextInputControl input(Node root, String prompt) {
        return find(root, TextInputControl.class,
                candidate -> prompt.equals(candidate.getPromptText()));
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> roleSelector(Node root) {
        Node selector = descendants(root)
                .filter(ComboBox.class::isInstance)
                .findFirst()
                .orElseThrow();
        return (ComboBox<String>) selector;
    }

    private static <T extends Node> T find(Node root, Class<T> type,
            Predicate<T> predicate) {
        return descendants(root)
                .filter(type::isInstance)
                .map(type::cast)
                .filter(predicate)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected " + type.getSimpleName() + " was not rendered"));
    }

    private static Stream<Node> descendants(Node root) {
        Stream<Node> current = Stream.of(root);
        if (root instanceof Parent parent) {
            Stream<Node> children = parent.getChildrenUnmodifiable().stream()
                    .flatMap(AuthenticationPaneTest::descendants);
            return Stream.concat(current, children);
        }
        return current;
    }

    private static <T> T runOnFxThread(java.util.concurrent.Callable<T> task)
            throws InterruptedException, ExecutionException, TimeoutException {
        FutureTask<T> future = new FutureTask<>(task);
        Platform.runLater(future);
        return future.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static final class MutableUserRepository implements UserRepository {
        private final List<UserAccount> accounts;

        MutableUserRepository(List<UserAccount> initialAccounts) {
            accounts = new ArrayList<>(initialAccounts);
        }

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return accounts.stream()
                    .filter(account -> account.username().equalsIgnoreCase(username))
                    .findFirst();
        }

        @Override
        public void add(UserAccount account) throws UserStoreException {
            if (findByUsername(account.username()).isPresent()) {
                throw new UserStoreException("duplicate test username");
            }
            accounts.add(account);
        }

        List<UserAccount> accounts() {
            return accounts;
        }
    }

    private static final class TestPasswordHasher implements PasswordHasher {
        private final Map<PasswordCredential, char[]> passwords = new IdentityHashMap<>();

        private byte marker = 1;

        @Override
        public PasswordCredential hash(char[] password) {
            byte[] salt = new byte[PasswordCredential.SALT_LENGTH_BYTES];
            byte[] hash = new byte[PasswordCredential.HASH_LENGTH_BYTES];
            Arrays.fill(salt, marker);
            Arrays.fill(hash, marker);
            marker++;
            PasswordCredential credential = new PasswordCredential(
                    PasswordAlgorithm.PBKDF2_HMAC_SHA256,
                    PasswordCredential.MIN_ITERATIONS,
                    PasswordCredential.KEY_LENGTH_BITS,
                    salt,
                    hash);
            passwords.put(credential, password.clone());
            return credential;
        }

        @Override
        public boolean verify(char[] suppliedPassword, PasswordCredential storedCredential) {
            return Arrays.equals(passwords.get(storedCredential), suppliedPassword);
        }
    }
}
