package io.github.cs32272610mp2xcode.finderskeepers.auth.bootstrap;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.cs32272610mp2xcode.finderskeepers.auth.application.ApplicationRoute;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationCoordinator;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationResult;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationStatus;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.RegistrationResult;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.RegistrationStatus;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.JsonUserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordCredential;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.Pbkdf2PasswordHasher;

class AuthenticationFactoryTest {
    private static final String PASSWORD = "  factory passphrase  ";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void composesJsonStorageAndPbkdf2Verification() throws UserStoreException {
        Path store = temporaryDirectory.resolve("isolated").resolve("users.json");
        JsonUserRepository repository = new JsonUserRepository(store);
        Pbkdf2PasswordHasher setupHasher = new Pbkdf2PasswordHasher(
                new SecureRandom(), PasswordCredential.MIN_ITERATIONS);
        char[] setupPassword = PASSWORD.toCharArray();
        try {
            repository.add(new UserAccount(
                    "student-1",
                    "factory.user",
                    UserRole.STUDENT,
                    setupHasher.hash(setupPassword)));
        } finally {
            Arrays.fill(setupPassword, '\0');
        }
        AuthenticationCoordinator coordinator = AuthenticationFactory.createCoordinator(store);
        char[] loginPassword = PASSWORD.toCharArray();

        AuthenticationResult result = coordinator.login("FACTORY.USER", loginPassword);

        assertAll(
                () -> assertEquals(AuthenticationStatus.SUCCESS, result.status()),
                () -> assertEquals(UserRole.STUDENT,
                        coordinator.currentUser().orElseThrow().role()),
                () -> assertEquals(ApplicationRoute.STUDENT, coordinator.route()),
                () -> assertEquals("", coordinator.message()),
                () -> assertArrayEquals(new char[loginPassword.length], loginPassword),
                () -> assertTrue(Files.isRegularFile(store)));
    }

    @Test
    void composesRegistrationAndLoginOverTheSameStore() throws UserStoreException {
        Path store = temporaryDirectory.resolve("registered-users.json");
        AuthenticationCoordinator coordinator = AuthenticationFactory.createCoordinator(store);
        char[] password = "factory-created officer".toCharArray();
        char[] confirmation = "factory-created officer".toCharArray();

        RegistrationResult registration = coordinator.register(
                "factory.officer", password, confirmation, UserRole.DESK_OFFICER);
        coordinator.logout();
        char[] loginPassword = "factory-created officer".toCharArray();
        AuthenticationResult login = coordinator.login("FACTORY.OFFICER", loginPassword);

        JsonUserRepository repository = new JsonUserRepository(store);
        assertAll(
                () -> assertEquals(RegistrationStatus.SUCCESS, registration.status()),
                () -> assertEquals(AuthenticationStatus.SUCCESS, login.status()),
                () -> assertEquals(ApplicationRoute.DESK_OFFICER, coordinator.route()),
                () -> assertEquals(UserRole.DESK_OFFICER,
                        repository.findByUsername("factory.officer").orElseThrow().role()),
                () -> assertArrayEquals(new char[password.length], password),
                () -> assertArrayEquals(new char[confirmation.length], confirmation),
                () -> assertArrayEquals(new char[loginPassword.length], loginPassword));
    }
}
