package io.github.cs32272610mp2xcode.finderskeepers.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class Pbkdf2PasswordHasherTest {
    private final PasswordHasher hasher = new Pbkdf2PasswordHasher(
            new SecureRandom(), PasswordCredential.MIN_ITERATIONS);

    @Test
    void rejectsWhitespaceOnlyPasswordWhenHashing() {
        assertThrows(IllegalArgumentException.class,
                () -> hasher.hash(" \t\r\n".toCharArray()));
    }

    @Test
    void verifiesOnlyTheExactPasswordIncludingMeaningfulSurroundingWhitespace() {
        String password = "  Correct Horse  ";
        PasswordCredential credential = hasher.hash(password.toCharArray());

        assertTrue(hasher.verify(password.toCharArray(), credential));
        assertFalse(hasher.verify(password.trim().toCharArray(), credential));
        assertFalse(hasher.verify("  correct Horse  ".toCharArray(), credential));
        assertFalse(hasher.verify("  Wrong Horse  ".toCharArray(), credential));
        assertFalse(hasher.verify(" \t\r\n".toCharArray(), credential));
    }

    @Test
    void createsASeparateRandomSaltForEachCredential() {
        PasswordCredential first = hasher.hash("same-synthetic-password".toCharArray());
        PasswordCredential second = hasher.hash("same-synthetic-password".toCharArray());

        assertFalse(Arrays.equals(first.salt(), second.salt()));
        assertNotEquals(first, second);
    }

    @Test
    void productionHasherCreatesCredentialsWithSixHundredThousandIterations() {
        PasswordHasher productionHasher = new Pbkdf2PasswordHasher();

        PasswordCredential credential = productionHasher.hash("Synthetic-Password-27".toCharArray());

        assertEquals(600_000, Pbkdf2PasswordHasher.DEFAULT_ITERATIONS);
        assertEquals(Pbkdf2PasswordHasher.DEFAULT_ITERATIONS, credential.iterations());
    }

    @Test
    void credentialDefensivelyCopiesSaltAndHashArrays() {
        byte[] salt = new byte[PasswordCredential.SALT_LENGTH_BYTES];
        byte[] hash = new byte[PasswordCredential.HASH_LENGTH_BYTES];
        salt[0] = 1;
        hash[0] = 2;

        PasswordCredential credential = new PasswordCredential(
                PasswordAlgorithm.PBKDF2_HMAC_SHA256,
                PasswordCredential.MIN_ITERATIONS,
                PasswordCredential.KEY_LENGTH_BITS,
                salt,
                hash);
        salt[0] = 3;
        hash[0] = 4;
        byte[] returnedSalt = credential.salt();
        byte[] returnedHash = credential.hash();
        returnedSalt[0] = 5;
        returnedHash[0] = 6;

        assertEquals(1, credential.salt()[0]);
        assertEquals(2, credential.hash()[0]);
    }
}
