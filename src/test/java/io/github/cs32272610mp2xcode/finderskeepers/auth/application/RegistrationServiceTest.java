package io.github.cs32272610mp2xcode.finderskeepers.auth.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordAlgorithm;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordCredential;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordHasher;

class RegistrationServiceTest {
    private static final UUID USER_ID = UUID.fromString(
            "be0b7e30-0bc6-4c65-a9ed-776c417c1d8e");

    private static final String PASSWORD = "new account password";

    @Test
    void registersStudentWithNormalizedUsernameAndClearsPasswords() {
        RecordingRepository repository = new RecordingRepository();
        RecordingHasher hasher = new RecordingHasher();
        RegistrationService service = service(repository, hasher);
        char[] password = PASSWORD.toCharArray();
        char[] confirmation = PASSWORD.toCharArray();

        RegistrationResult result = service.register(
                "  new.student  ", password, confirmation, UserRole.STUDENT);

        UserAccount stored = repository.accounts().getFirst();
        assertAll(
                () -> assertEquals(RegistrationStatus.SUCCESS, result.status()),
                () -> assertEquals("", result.message()),
                () -> assertEquals(USER_ID.toString(), stored.userId()),
                () -> assertEquals("new.student", stored.username()),
                () -> assertEquals(UserRole.STUDENT, stored.role()),
                () -> assertEquals(stored.credential(), hasher.credential()),
                () -> assertEquals(stored.userId(), result.user().orElseThrow().userId()),
                () -> assertArrayEquals(PASSWORD.toCharArray(), hasher.hashedPassword()),
                () -> assertArrayEquals(new char[password.length], password),
                () -> assertArrayEquals(new char[confirmation.length], confirmation));
    }

    @Test
    void registersDeskOfficerAndCoordinatorStartsOfficerSession() {
        RecordingRepository repository = new RecordingRepository();
        RecordingHasher hasher = new RecordingHasher();
        RegistrationService registration = service(repository, hasher);
        AuthenticationCoordinator coordinator = new AuthenticationCoordinator(
                new AuthenticationService(repository, hasher), registration);

        RegistrationResult result = coordinator.register(
                "new.officer", PASSWORD.toCharArray(), PASSWORD.toCharArray(),
                UserRole.DESK_OFFICER);

        assertAll(
                () -> assertEquals(RegistrationStatus.SUCCESS, result.status()),
                () -> assertEquals(UserRole.DESK_OFFICER,
                        coordinator.currentUser().orElseThrow().role()),
                () -> assertEquals(ApplicationRoute.DESK_OFFICER, coordinator.route()),
                () -> assertEquals("", coordinator.message()));
    }

    @Test
    void refusesCaseInsensitiveDuplicateBeforeHashing() {
        RecordingRepository repository = new RecordingRepository();
        repository.accounts().add(account("existing-id", "Existing.User", UserRole.STUDENT));
        RecordingHasher hasher = new RecordingHasher();
        RegistrationService service = service(repository, hasher);
        char[] password = PASSWORD.toCharArray();
        char[] confirmation = PASSWORD.toCharArray();

        RegistrationResult result = service.register(
                " existing.user ", password, confirmation, UserRole.DESK_OFFICER);

        assertAll(
                () -> assertEquals(RegistrationStatus.USERNAME_TAKEN, result.status()),
                () -> assertEquals("That username is already in use.", result.message()),
                () -> assertTrue(result.user().isEmpty()),
                () -> assertFalse(hasher.wasCalled()),
                () -> assertEquals(1, repository.accounts().size()),
                () -> assertArrayEquals(new char[password.length], password),
                () -> assertArrayEquals(new char[confirmation.length], confirmation));
    }

    @Test
    void rejectsInvalidInputsBeforeStorageOrHashing() {
        RecordingRepository repository = new RecordingRepository();
        RecordingHasher hasher = new RecordingHasher();
        RegistrationService service = service(repository, hasher);

        RegistrationResult username = service.register(
                "\t", PASSWORD.toCharArray(), PASSWORD.toCharArray(), UserRole.STUDENT);
        RegistrationResult password = service.register(
                "new.student", "   ".toCharArray(), "   ".toCharArray(), UserRole.STUDENT);
        RegistrationResult mismatch = service.register(
                "new.student", PASSWORD.toCharArray(), "different".toCharArray(),
                UserRole.STUDENT);
        RegistrationResult role = service.register(
                "new.student", PASSWORD.toCharArray(), PASSWORD.toCharArray(), null);

        assertAll(
                () -> assertEquals(RegistrationStatus.INVALID_USERNAME, username.status()),
                () -> assertEquals("Enter a username.", username.message()),
                () -> assertEquals(RegistrationStatus.INVALID_PASSWORD, password.status()),
                () -> assertEquals("Enter a password that is not blank.", password.message()),
                () -> assertEquals(RegistrationStatus.PASSWORD_MISMATCH, mismatch.status()),
                () -> assertEquals("Passwords do not match.", mismatch.message()),
                () -> assertEquals(RegistrationStatus.INVALID_ROLE, role.status()),
                () -> assertEquals("Choose Student or Desk Officer.", role.message()),
                () -> assertTrue(repository.accounts().isEmpty()),
                () -> assertFalse(hasher.wasCalled()));
    }

    @Test
    void reportsStorageFailureAndPreservesAnExistingCoordinatorSession() {
        RecordingRepository repository = new RecordingRepository();
        RecordingHasher hasher = new RecordingHasher();
        AuthenticationCoordinator coordinator = new AuthenticationCoordinator(
                new AuthenticationService(repository, hasher), service(repository, hasher));
        RegistrationResult initial = coordinator.register(
                "first.student", PASSWORD.toCharArray(), PASSWORD.toCharArray(),
                UserRole.STUDENT);
        repository.failReads();
        char[] password = PASSWORD.toCharArray();
        char[] confirmation = PASSWORD.toCharArray();

        RegistrationResult failed = coordinator.register(
                "second.student", password, confirmation, UserRole.STUDENT);

        assertAll(
                () -> assertEquals(RegistrationStatus.SUCCESS, initial.status()),
                () -> assertEquals(RegistrationStatus.STORAGE_ERROR, failed.status()),
                () -> assertEquals(
                        "Local accounts are unavailable. Please contact the Desk Officer.",
                        coordinator.message()),
                () -> assertEquals("first.student",
                        coordinator.currentUser().orElseThrow().username()),
                () -> assertEquals(ApplicationRoute.STUDENT, coordinator.route()),
                () -> assertArrayEquals(new char[password.length], password),
                () -> assertArrayEquals(new char[confirmation.length], confirmation));
    }

    @Test
    void resultFactoryRejectsSuccessWithoutAUser() {
        assertThrows(IllegalArgumentException.class,
                () -> RegistrationResult.failure(RegistrationStatus.SUCCESS));
    }

    private static RegistrationService service(UserRepository repository,
            PasswordHasher hasher) {
        return new RegistrationService(repository, hasher, () -> USER_ID);
    }

    private static UserAccount account(String id, String username, UserRole role) {
        return new UserAccount(id, username, role, credential());
    }

    private static PasswordCredential credential() {
        return new PasswordCredential(
                PasswordAlgorithm.PBKDF2_HMAC_SHA256,
                PasswordCredential.MIN_ITERATIONS,
                PasswordCredential.KEY_LENGTH_BITS,
                new byte[PasswordCredential.SALT_LENGTH_BYTES],
                new byte[PasswordCredential.HASH_LENGTH_BYTES]);
    }

    private static final class RecordingRepository implements UserRepository {
        private final List<UserAccount> accounts = new ArrayList<>();

        private boolean readsFail;

        @Override
        public Optional<UserAccount> findByUsername(String username) throws UserStoreException {
            if (readsFail) {
                throw new UserStoreException("synthetic read failure");
            }
            return accounts.stream()
                    .filter(account -> account.username().equalsIgnoreCase(username))
                    .findFirst();
        }

        @Override
        public void add(UserAccount account) {
            accounts.add(account);
        }

        List<UserAccount> accounts() {
            return accounts;
        }

        void failReads() {
            readsFail = true;
        }
    }

    private static final class RecordingHasher implements PasswordHasher {
        private char[] password;

        private final PasswordCredential credential = RegistrationServiceTest.credential();

        @Override
        public PasswordCredential hash(char[] passwordToHash) {
            password = passwordToHash.clone();
            return credential;
        }

        @Override
        public boolean verify(char[] suppliedPassword, PasswordCredential storedCredential) {
            return Arrays.equals(password, suppliedPassword)
                    && credential.equals(storedCredential);
        }

        boolean wasCalled() {
            return password != null;
        }

        char[] hashedPassword() {
            return password.clone();
        }

        PasswordCredential credential() {
            return credential;
        }
    }
}
