package io.github.cs32272610mp2xcode.finderskeepers.auth.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2 password hashing with per-account random salts. */
public final class Pbkdf2PasswordHasher implements PasswordHasher {
    /** Work factor used for newly created credentials. */
    public static final int DEFAULT_ITERATIONS = 600_000;

    private final SecureRandom secureRandom;

    private final int iterations;

    /** Creates a production password hasher. */
    public Pbkdf2PasswordHasher() {
        this(new SecureRandom(), DEFAULT_ITERATIONS);
    }

    /**
     * Creates a hasher with an explicit valid work factor, primarily for controlled tests.
     *
     * @param randomSource cryptographically secure salt source
     * @param iterationCount work factor
     */
    public Pbkdf2PasswordHasher(SecureRandom randomSource, int iterationCount) {
        secureRandom = Objects.requireNonNull(randomSource, "randomSource");
        if (iterationCount < PasswordCredential.MIN_ITERATIONS
                || iterationCount > PasswordCredential.MAX_ITERATIONS) {
            throw new IllegalArgumentException("Password iterations are outside supported bounds");
        }
        iterations = iterationCount;
    }

    @Override
    public PasswordCredential hash(char[] password) {
        PasswordPolicy.requireNonBlank(password);
        byte[] salt = new byte[PasswordCredential.SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = derive(password, salt, iterations);
        return new PasswordCredential(PasswordAlgorithm.PBKDF2_HMAC_SHA256,
                iterations, PasswordCredential.KEY_LENGTH_BITS, salt, hash);
    }

    @Override
    public boolean verify(char[] password, PasswordCredential credential) {
        Objects.requireNonNull(credential, "credential");
        if (PasswordPolicy.isBlank(password)) {
            return false;
        }
        byte[] candidate = derive(password, credential.salt(), credential.iterations());
        try {
            return MessageDigest.isEqual(candidate, credential.hash());
        } finally {
            Arrays.fill(candidate, (byte) 0);
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterationCount) {
        PBEKeySpec specification = new PBEKeySpec(password, salt, iterationCount,
                PasswordCredential.KEY_LENGTH_BITS);
        try {
            String algorithm = PasswordAlgorithm.PBKDF2_HMAC_SHA256.storageValue();
            return SecretKeyFactory.getInstance(algorithm)
                    .generateSecret(specification)
                    .getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Configured password hashing is unavailable", exception);
        } finally {
            specification.clearPassword();
        }
    }
}
