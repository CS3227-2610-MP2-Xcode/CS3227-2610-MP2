package io.github.cs32272610mp2xcode.finderskeepers.auth.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordAlgorithm;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordCredential;

class AuthenticationModelValidationTest {
    @Test
    void accountAndSessionIdentityNormalizeTextAndRejectMissingIdentityParts() {
        PasswordCredential credential = credential();

        UserAccount account = new UserAccount(" student-1 ", " student.user ",
                UserRole.STUDENT, credential);
        AuthenticatedUser session = new AuthenticatedUser(" officer-1 ", " officer.user ",
                UserRole.DESK_OFFICER);

        assertEquals("student-1", account.userId());
        assertEquals("student.user", account.username());
        assertEquals("officer-1", session.userId());
        assertEquals("officer.user", session.username());
        assertThrows(IllegalArgumentException.class,
                () -> new UserAccount(" ", "student.user", UserRole.STUDENT, credential));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticatedUser("officer-1", "\t", UserRole.DESK_OFFICER));
        assertThrows(NullPointerException.class,
                () -> new UserAccount(null, "student.user", UserRole.STUDENT, credential));
        assertThrows(NullPointerException.class,
                () -> new AuthenticatedUser("officer-1", "officer.user", null));
    }

    private static PasswordCredential credential() {
        return new PasswordCredential(PasswordAlgorithm.PBKDF2_HMAC_SHA256,
                PasswordCredential.MIN_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                new byte[PasswordCredential.SALT_LENGTH_BYTES],
                new byte[PasswordCredential.HASH_LENGTH_BYTES]);
    }
}
