package io.github.cs32272610mp2xcode.finderskeepers.auth.provisioning;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.JsonUserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordAlgorithm;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordCredential;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordHasher;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.Pbkdf2PasswordHasher;

class AccountProvisionerTest {
    private static final String STUDENT_PASSWORD = "Synthetic Student Password 27!";

    private static final String OFFICER_PASSWORD = "Synthetic Officer Password 42!";

    @TempDir
    private Path temporaryDirectory;

    private RecordingRepository repository;

    private RecordingPasswordHasher hasher;

    private AccountProvisioner provisioner;

    @BeforeEach
    void setUp() {
        repository = new RecordingRepository();
        hasher = new RecordingPasswordHasher();
        provisioner = new AccountProvisioner(repository, hasher);
    }

    @Test
    void rejectsWhitespaceOnlyPasswordAndClearsItsArray() {
        char[] password = " \t\r\n".toCharArray();

        assertThrows(IllegalArgumentException.class, () -> provisioner.provision(
                "student-1", "student.user", "STUDENT", password));

        assertArrayEquals(new char[password.length], password);
        assertFalse(hasher.wasCalled());
        assertNull(repository.addedAccount());
    }

    @Test
    void preservesMeaningfulSurroundingWhitespaceWhileHashingAndClearsTheSourceArray()
            throws Exception {
        char[] password = "  Synthetic Password  ".toCharArray();
        char[] expectedPassword = password.clone();

        provisioner.provision("student-1", "student.user", "STUDENT", password);

        assertArrayEquals(expectedPassword, hasher.hashedPassword());
        assertArrayEquals(new char[password.length], password);
        assertTrue(hasher.wasCalled());
        UserAccount account = repository.addedAccount();
        assertNotNull(account);
        assertEquals("student-1", account.userId());
        assertEquals("student.user", account.username());
        assertEquals(UserRole.STUDENT, account.role());
    }

    @Test
    void provisionsBothRolesWithProductionParametersAndNoPlaintext()
            throws IOException, UserStoreException {
        Path store = temporaryDirectory.resolve("accounts.json");
        JsonUserRepository jsonRepository = new JsonUserRepository(store);
        AccountProvisioner productionProvisioner = new AccountProvisioner(
                jsonRepository, new Pbkdf2PasswordHasher());

        productionProvisioner.provision(
                "student-1", "student.user", "STUDENT", STUDENT_PASSWORD.toCharArray());
        productionProvisioner.provision(
                "officer-1", "officer.user", "DESK_OFFICER", OFFICER_PASSWORD.toCharArray());

        UserAccount student = jsonRepository.findByUsername("student.user").orElseThrow();
        UserAccount officer = jsonRepository.findByUsername("officer.user").orElseThrow();
        String persisted = Files.readString(store, StandardCharsets.UTF_8);
        assertEquals(UserRole.STUDENT, student.role());
        assertEquals(UserRole.DESK_OFFICER, officer.role());
        assertEquals(Pbkdf2PasswordHasher.DEFAULT_ITERATIONS,
                student.credential().iterations());
        assertEquals(Pbkdf2PasswordHasher.DEFAULT_ITERATIONS,
                officer.credential().iterations());
        assertFalse(persisted.contains(STUDENT_PASSWORD));
        assertFalse(persisted.contains(OFFICER_PASSWORD));
    }

    @Test
    void rejectsBlankIdentityFieldsAndInvalidRoleBeforeHashing() {
        char[] blankIdPassword = STUDENT_PASSWORD.toCharArray();
        char[] blankUsernamePassword = STUDENT_PASSWORD.toCharArray();
        char[] invalidRolePassword = STUDENT_PASSWORD.toCharArray();

        assertThrows(IllegalArgumentException.class, () -> provisioner.provision(
                " ", "student.user", "STUDENT", blankIdPassword));
        assertThrows(IllegalArgumentException.class, () -> provisioner.provision(
                "student-1", "\t", "STUDENT", blankUsernamePassword));
        assertThrows(IllegalArgumentException.class, () -> provisioner.provision(
                "student-1", "student.user", "COMMUNITY_MEMBER", invalidRolePassword));
        assertThrows(IllegalArgumentException.class, () -> provisioner.provision(
                "student-1", "student.user", "STUDENT", null));

        assertArrayEquals(new char[blankIdPassword.length], blankIdPassword);
        assertArrayEquals(new char[blankUsernamePassword.length], blankUsernamePassword);
        assertArrayEquals(new char[invalidRolePassword.length], invalidRolePassword);
        assertFalse(hasher.wasCalled());
        assertNull(repository.addedAccount());
    }

    @Test
    void refusesDuplicateIdentifiersAndUsernamesWithoutReplacingTheAccount()
            throws IOException, UserStoreException {
        Path store = temporaryDirectory.resolve("duplicates.json");
        JsonUserRepository jsonRepository = new JsonUserRepository(store);
        AccountProvisioner jsonProvisioner = new AccountProvisioner(
                jsonRepository, new RecordingPasswordHasher());
        jsonProvisioner.provision(
                "student-1", "student.user", "STUDENT", STUDENT_PASSWORD.toCharArray());
        byte[] original = Files.readAllBytes(store);

        assertThrows(UserStoreException.class, () -> jsonProvisioner.provision(
                "STUDENT-1", "different.user", "STUDENT", OFFICER_PASSWORD.toCharArray()));
        assertThrows(UserStoreException.class, () -> jsonProvisioner.provision(
                "student-2", "STUDENT.USER", "STUDENT", OFFICER_PASSWORD.toCharArray()));

        assertArrayEquals(original, Files.readAllBytes(store));
        assertTrue(jsonRepository.findByUsername("student.user").isPresent());
        assertTrue(jsonRepository.findByUsername("different.user").isEmpty());
    }

    @Test
    void clearsPasswordWhenPersistenceFails() {
        UserRepository failingRepository = new UserRepository() {
            @Override
            public Optional<UserAccount> findByUsername(String username) {
                return Optional.empty();
            }

            @Override
            public void add(UserAccount account) throws UserStoreException {
                throw new UserStoreException("synthetic persistence failure");
            }
        };
        AccountProvisioner failingProvisioner = new AccountProvisioner(
                failingRepository, new RecordingPasswordHasher());
        char[] password = STUDENT_PASSWORD.toCharArray();

        assertThrows(UserStoreException.class, () -> failingProvisioner.provision(
                "student-1", "student.user", "STUDENT", password));

        assertArrayEquals(new char[password.length], password);
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
        private UserAccount account;

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return Optional.empty();
        }

        @Override
        public void add(UserAccount accountToAdd) {
            account = accountToAdd;
        }

        UserAccount addedAccount() {
            return account;
        }
    }

    private static final class RecordingPasswordHasher implements PasswordHasher {
        private boolean called;

        private char[] password;

        @Override
        public PasswordCredential hash(char[] passwordToHash) {
            called = true;
            password = passwordToHash.clone();
            return credential();
        }

        @Override
        public boolean verify(char[] suppliedPassword, PasswordCredential storedCredential) {
            throw new UnsupportedOperationException("Verification is not used while provisioning");
        }

        boolean wasCalled() {
            return called;
        }

        char[] hashedPassword() {
            return password.clone();
        }
    }
}
