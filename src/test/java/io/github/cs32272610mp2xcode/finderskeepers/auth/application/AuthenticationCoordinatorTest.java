package io.github.cs32272610mp2xcode.finderskeepers.auth.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.JsonUserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordAlgorithm;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordCredential;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordHasher;

class AuthenticationCoordinatorTest {
    private static final String STUDENT_PASSWORD = "student passphrase";

    private static final String OFFICER_PASSWORD = "officer passphrase";

    private static final String SURROUNDED_PASSWORD = "  meaningful whitespace\t";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void authenticatesBothRolesAndDerivesTheirRoutes() {
        PasswordCredential studentCredential = credential((byte) 1);
        PasswordCredential officerCredential = credential((byte) 2);
        ExactPasswordHasher hasher = new ExactPasswordHasher(Map.of(
                studentCredential, STUDENT_PASSWORD.toCharArray(),
                officerCredential, OFFICER_PASSWORD.toCharArray()));
        AuthenticationCoordinator coordinator = coordinator(
                List.of(
                        account("student-1", "student.user", UserRole.STUDENT,
                                studentCredential),
                        account("officer-1", "officer.user", UserRole.DESK_OFFICER,
                                officerCredential)),
                hasher);

        char[] studentPassword = STUDENT_PASSWORD.toCharArray();
        AuthenticationResult studentResult = coordinator.login(
                "  STUDENT.USER  ", studentPassword);

        assertAll(
                () -> assertEquals(AuthenticationStatus.SUCCESS, studentResult.status()),
                () -> assertEquals(UserRole.STUDENT,
                        coordinator.currentUser().orElseThrow().role()),
                () -> assertEquals(ApplicationRoute.STUDENT, coordinator.route()),
                () -> assertArrayEquals(new char[studentPassword.length], studentPassword));

        char[] officerPassword = OFFICER_PASSWORD.toCharArray();
        AuthenticationResult officerResult = coordinator.login("officer.user", officerPassword);

        assertAll(
                () -> assertEquals(AuthenticationStatus.SUCCESS, officerResult.status()),
                () -> assertEquals(UserRole.DESK_OFFICER,
                        coordinator.currentUser().orElseThrow().role()),
                () -> assertEquals(ApplicationRoute.DESK_OFFICER, coordinator.route()),
                () -> assertArrayEquals(new char[officerPassword.length], officerPassword));
    }

    @Test
    void unknownUsernameAndIncorrectPasswordUseTheSameGenericFailure() {
        PasswordCredential credential = credential((byte) 3);
        ExactPasswordHasher hasher = new ExactPasswordHasher(
                Map.of(credential, STUDENT_PASSWORD.toCharArray()));
        AuthenticationCoordinator coordinator = coordinator(
                List.of(account("student-1", "student.user", UserRole.STUDENT, credential)),
                hasher);
        char[] unknownPassword = "unknown input".toCharArray();
        char[] incorrectPassword = "incorrect input".toCharArray();

        AuthenticationResult unknown = coordinator.login("missing.user", unknownPassword);
        AuthenticationResult incorrect = coordinator.login("student.user", incorrectPassword);

        assertAll(
                () -> assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, unknown.status()),
                () -> assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, incorrect.status()),
                () -> assertEquals(AuthenticationResult.INVALID_CREDENTIALS_MESSAGE,
                        unknown.message()),
                () -> assertEquals(unknown.message(), incorrect.message()),
                () -> assertTrue(coordinator.currentUser().isEmpty()),
                () -> assertEquals(ApplicationRoute.LOGIN, coordinator.route()),
                () -> assertArrayEquals(new char[unknownPassword.length], unknownPassword),
                () -> assertArrayEquals(new char[incorrectPassword.length], incorrectPassword));
    }

    @Test
    void rejectsBlankUsernameAndBlankPasswordsWithoutInvokingHasher() {
        PasswordCredential credential = credential((byte) 4);
        ExactPasswordHasher hasher = new ExactPasswordHasher(
                Map.of(credential, STUDENT_PASSWORD.toCharArray()));
        AuthenticationCoordinator coordinator = coordinator(
                List.of(account("student-1", "student.user", UserRole.STUDENT, credential)),
                hasher);
        char[] empty = new char[0];
        char[] whitespace = {' ', '\t', '\n', '\u2003'};
        char[] blankUsernamePassword = STUDENT_PASSWORD.toCharArray();

        AuthenticationResult nullPassword = coordinator.login("student.user", null);
        AuthenticationResult emptyPassword = coordinator.login("student.user", empty);
        AuthenticationResult whitespacePassword = coordinator.login("student.user", whitespace);
        AuthenticationResult blankUsername = coordinator.login(" \t ", blankUsernamePassword);

        assertAll(
                () -> assertEquals(AuthenticationStatus.INVALID_CREDENTIALS,
                        nullPassword.status()),
                () -> assertEquals(AuthenticationStatus.INVALID_CREDENTIALS,
                        emptyPassword.status()),
                () -> assertEquals(AuthenticationStatus.INVALID_CREDENTIALS,
                        whitespacePassword.status()),
                () -> assertEquals(AuthenticationStatus.INVALID_CREDENTIALS,
                        blankUsername.status()),
                () -> assertEquals(0, hasher.verificationCount()),
                () -> assertArrayEquals(new char[0], empty),
                () -> assertArrayEquals(new char[whitespace.length], whitespace),
                () -> assertArrayEquals(
                        new char[blankUsernamePassword.length], blankUsernamePassword),
                () -> assertTrue(coordinator.currentUser().isEmpty()),
                () -> assertEquals(ApplicationRoute.LOGIN, coordinator.route()));
    }

    @Test
    void preservesMeaningfulSurroundingPasswordWhitespace() {
        PasswordCredential credential = credential((byte) 5);
        ExactPasswordHasher hasher = new ExactPasswordHasher(
                Map.of(credential, SURROUNDED_PASSWORD.toCharArray()));
        AuthenticationCoordinator coordinator = coordinator(
                List.of(account("student-1", "student.user", UserRole.STUDENT, credential)),
                hasher);
        char[] password = SURROUNDED_PASSWORD.toCharArray();

        AuthenticationResult result = coordinator.login("student.user", password);

        assertAll(
                () -> assertEquals(AuthenticationStatus.SUCCESS, result.status()),
                () -> assertEquals(ApplicationRoute.STUDENT, coordinator.route()),
                () -> assertArrayEquals(new char[password.length], password));
    }

    @Test
    void failedLoginFromEmptyStateCreatesNoSession() {
        PasswordCredential credential = credential((byte) 6);
        AuthenticationCoordinator coordinator = coordinator(
                List.of(account("student-1", "student.user", UserRole.STUDENT, credential)),
                new ExactPasswordHasher(Map.of(credential, STUDENT_PASSWORD.toCharArray())));

        AuthenticationResult result = coordinator.login(
                "student.user", "wrong password".toCharArray());

        assertAll(
                () -> assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, result.status()),
                () -> assertTrue(coordinator.currentUser().isEmpty()),
                () -> assertEquals(ApplicationRoute.LOGIN, coordinator.route()),
                () -> assertEquals(AuthenticationResult.INVALID_CREDENTIALS_MESSAGE,
                        coordinator.message()));
    }

    @Test
    void failedLoginPreservesExistingIdentityAndDerivedRoute() {
        PasswordCredential studentCredential = credential((byte) 7);
        PasswordCredential officerCredential = credential((byte) 8);
        AuthenticationCoordinator coordinator = coordinator(
                List.of(
                        account("student-1", "student.user", UserRole.STUDENT,
                                studentCredential),
                        account("officer-1", "officer.user", UserRole.DESK_OFFICER,
                                officerCredential)),
                new ExactPasswordHasher(Map.of(
                        studentCredential, STUDENT_PASSWORD.toCharArray(),
                        officerCredential, OFFICER_PASSWORD.toCharArray())));
        coordinator.login("student.user", STUDENT_PASSWORD.toCharArray());
        AuthenticatedUser originalUser = coordinator.currentUser().orElseThrow();

        AuthenticationResult result = coordinator.login(
                "officer.user", "wrong password".toCharArray());

        assertAll(
                () -> assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, result.status()),
                () -> assertEquals(originalUser, coordinator.currentUser().orElseThrow()),
                () -> assertEquals(ApplicationRoute.STUDENT, coordinator.route()),
                () -> assertEquals(AuthenticationResult.INVALID_CREDENTIALS_MESSAGE,
                        coordinator.message()));
    }

    @Test
    void storageFailurePreservesExistingIdentityAndDerivedRoute() {
        PasswordCredential credential = credential((byte) 10);
        UserAccount account = account(
                "student-1", "student.user", UserRole.STUDENT, credential);
        SwitchableUserRepository repository = new SwitchableUserRepository(account);
        ExactPasswordHasher hasher = new ExactPasswordHasher(
                Map.of(credential, STUDENT_PASSWORD.toCharArray()));
        AuthenticationCoordinator coordinator = coordinator(repository, hasher);
        coordinator.login("student.user", STUDENT_PASSWORD.toCharArray());
        AuthenticatedUser originalUser = coordinator.currentUser().orElseThrow();
        repository.failReads();
        char[] password = STUDENT_PASSWORD.toCharArray();

        AuthenticationResult result = coordinator.login("student.user", password);

        assertAll(
                () -> assertEquals(AuthenticationStatus.STORAGE_ERROR, result.status()),
                () -> assertEquals(originalUser, coordinator.currentUser().orElseThrow()),
                () -> assertEquals(ApplicationRoute.STUDENT, coordinator.route()),
                () -> assertEquals(AuthenticationResult.STORAGE_ERROR_MESSAGE,
                        coordinator.message()),
                () -> assertEquals(1, hasher.verificationCount()),
                () -> assertArrayEquals(new char[password.length], password));
    }

    @Test
    void clearValidationPreservesSessionWhileLogoutClearsIt() {
        PasswordCredential credential = credential((byte) 9);
        AuthenticationCoordinator coordinator = coordinator(
                List.of(account("officer-1", "officer.user", UserRole.DESK_OFFICER, credential)),
                new ExactPasswordHasher(Map.of(credential, OFFICER_PASSWORD.toCharArray())));
        coordinator.login("officer.user", OFFICER_PASSWORD.toCharArray());
        coordinator.login("officer.user", "wrong password".toCharArray());

        coordinator.clearValidation();

        assertAll(
                () -> assertEquals("", coordinator.message()),
                () -> assertEquals(UserRole.DESK_OFFICER,
                        coordinator.currentUser().orElseThrow().role()),
                () -> assertEquals(ApplicationRoute.DESK_OFFICER, coordinator.route()));

        coordinator.logout();

        assertAll(
                () -> assertTrue(coordinator.currentUser().isEmpty()),
                () -> assertEquals(ApplicationRoute.LOGIN, coordinator.route()),
                () -> assertEquals("", coordinator.message()));
    }

    @Test
    void corruptCredentialMetadataBecomesStorageErrorBeforeHashing() throws IOException {
        Path store = temporaryDirectory.resolve("corrupt-users.json");
        String salt = java.util.Base64.getEncoder().encodeToString(new byte[16]);
        String hash = java.util.Base64.getEncoder().encodeToString(new byte[32]);
        String corruptJson = """
                {
                  "version": 1,
                  "accounts": [
                    {
                      "userId": "student-1",
                      "username": "student.user",
                      "role": "STUDENT",
                      "algorithm": "unsupported",
                      "iterations": 210000,
                      "keyLength": 256,
                      "salt": "%s",
                      "passwordHash": "%s"
                    }
                  ]
                }
                """.formatted(salt, hash);
        Files.writeString(store, corruptJson, StandardCharsets.UTF_8);
        CountingPasswordHasher hasher = new CountingPasswordHasher();
        AuthenticationCoordinator coordinator = coordinator(
                new JsonUserRepository(store), hasher);
        char[] password = STUDENT_PASSWORD.toCharArray();

        AuthenticationResult result = coordinator.login("student.user", password);

        assertAll(
                () -> assertEquals(AuthenticationStatus.STORAGE_ERROR, result.status()),
                () -> assertEquals(AuthenticationResult.STORAGE_ERROR_MESSAGE,
                        coordinator.message()),
                () -> assertTrue(coordinator.currentUser().isEmpty()),
                () -> assertEquals(ApplicationRoute.LOGIN, coordinator.route()),
                () -> assertEquals(0, hasher.verificationCount()),
                () -> assertArrayEquals(new char[password.length], password),
                () -> assertEquals(corruptJson,
                        Files.readString(store, StandardCharsets.UTF_8)));
    }

    private static AuthenticationCoordinator coordinator(List<UserAccount> accounts,
            PasswordHasher hasher) {
        return coordinator(new InMemoryUserRepository(accounts), hasher);
    }

    private static AuthenticationCoordinator coordinator(UserRepository repository,
            PasswordHasher hasher) {
        AuthenticationService service = new AuthenticationService(repository, hasher);
        RegistrationService registration = new RegistrationService(
                repository, hasher, UUID::randomUUID);
        return new AuthenticationCoordinator(service, registration);
    }

    private static UserAccount account(String userId, String username, UserRole role,
            PasswordCredential credential) {
        return new UserAccount(userId, username, role, credential);
    }

    private static PasswordCredential credential(byte marker) {
        byte[] salt = new byte[PasswordCredential.SALT_LENGTH_BYTES];
        byte[] hash = new byte[PasswordCredential.HASH_LENGTH_BYTES];
        Arrays.fill(salt, marker);
        Arrays.fill(hash, marker);
        return new PasswordCredential(
                PasswordAlgorithm.PBKDF2_HMAC_SHA256,
                PasswordCredential.MIN_ITERATIONS,
                PasswordCredential.KEY_LENGTH_BITS,
                salt,
                hash);
    }

    private static final class InMemoryUserRepository implements UserRepository {
        private final List<UserAccount> accounts;

        InMemoryUserRepository(List<UserAccount> initialAccounts) {
            accounts = List.copyOf(initialAccounts);
        }

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return accounts.stream()
                    .filter(account -> account.username().equalsIgnoreCase(username))
                    .findFirst();
        }

        @Override
        public void add(UserAccount account) {
            throw new UnsupportedOperationException("Authentication tests do not add accounts");
        }
    }

    private static final class ExactPasswordHasher implements PasswordHasher {
        private final Map<PasswordCredential, char[]> expectedPasswords;

        private int verificationCount;

        ExactPasswordHasher(Map<PasswordCredential, char[]> passwordByCredential) {
            expectedPasswords = Map.copyOf(passwordByCredential);
        }

        @Override
        public PasswordCredential hash(char[] password) {
            throw new UnsupportedOperationException("Authentication tests do not create hashes");
        }

        @Override
        public boolean verify(char[] password, PasswordCredential credential) {
            verificationCount++;
            char[] expected = expectedPasswords.get(credential);
            return expected != null && Arrays.equals(expected, password);
        }

        int verificationCount() {
            return verificationCount;
        }
    }

    private static final class SwitchableUserRepository implements UserRepository {
        private final UserAccount account;

        private boolean readsFail;

        SwitchableUserRepository(UserAccount storedAccount) {
            account = storedAccount;
        }

        @Override
        public Optional<UserAccount> findByUsername(String username) throws UserStoreException {
            if (readsFail) {
                throw new UserStoreException("synthetic storage failure");
            }
            return account.username().equalsIgnoreCase(username)
                    ? Optional.of(account) : Optional.empty();
        }

        @Override
        public void add(UserAccount accountToAdd) {
            throw new UnsupportedOperationException("Authentication tests do not add accounts");
        }

        void failReads() {
            readsFail = true;
        }
    }

    private static final class CountingPasswordHasher implements PasswordHasher {
        private int verificationCount;

        @Override
        public PasswordCredential hash(char[] password) {
            throw new UnsupportedOperationException("Authentication tests do not create hashes");
        }

        @Override
        public boolean verify(char[] password, PasswordCredential credential) {
            verificationCount++;
            return false;
        }

        int verificationCount() {
            return verificationCount;
        }
    }
}
