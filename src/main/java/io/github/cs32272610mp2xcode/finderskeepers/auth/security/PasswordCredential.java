package io.github.cs32272610mp2xcode.finderskeepers.auth.security;

import java.util.Arrays;
import java.util.Objects;

/** Password-verification material stored for a local account. */
public final class PasswordCredential {
    /** Lowest work factor accepted from a version 1 credential store. */
    public static final int MIN_ITERATIONS = 210_000;

    /** Highest work factor accepted from a version 1 credential store. */
    public static final int MAX_ITERATIONS = 1_000_000;

    /** Required PBKDF2 derived-key length in bits. */
    public static final int KEY_LENGTH_BITS = 256;

    /** Required random-salt length in bytes. */
    public static final int SALT_LENGTH_BYTES = 16;

    /** Required derived password length in bytes. */
    public static final int HASH_LENGTH_BYTES = KEY_LENGTH_BITS / Byte.SIZE;

    private final PasswordAlgorithm algorithm;

    private final int iterations;

    private final int keyLength;

    private final byte[] salt;

    private final byte[] hash;

    /**
     * Creates validated version 1 password-verification material.
     *
     * @param algorithmValue password derivation algorithm
     * @param iterationCount work factor
     * @param derivedKeyLength derived key length in bits
     * @param saltBytes unique account salt
     * @param hashBytes derived password value
     */
    public PasswordCredential(PasswordAlgorithm algorithmValue, int iterationCount,
            int derivedKeyLength, byte[] saltBytes, byte[] hashBytes) {
        algorithm = Objects.requireNonNull(algorithmValue, "algorithmValue");
        if (iterationCount < MIN_ITERATIONS || iterationCount > MAX_ITERATIONS) {
            throw new IllegalArgumentException("Password iterations are outside supported bounds");
        }
        if (derivedKeyLength != KEY_LENGTH_BITS) {
            throw new IllegalArgumentException("Password key length is unsupported");
        }
        iterations = iterationCount;
        keyLength = derivedKeyLength;
        salt = requireLength(saltBytes, SALT_LENGTH_BYTES, "salt");
        hash = requireLength(hashBytes, HASH_LENGTH_BYTES, "hash");
    }

    /**
     * Gets the password derivation algorithm.
     *
     * @return password derivation algorithm
     */
    public PasswordAlgorithm algorithm() {
        return algorithm;
    }

    /**
     * Gets the password work factor.
     *
     * @return work factor
     */
    public int iterations() {
        return iterations;
    }

    /**
     * Gets the derived key length.
     *
     * @return derived key length in bits
     */
    public int keyLength() {
        return keyLength;
    }

    /**
     * Gets the unique account salt.
     *
     * @return defensive copy of the salt
     */
    public byte[] salt() {
        return salt.clone();
    }

    /**
     * Gets the derived password value.
     *
     * @return defensive copy of the derived password value
     */
    public byte[] hash() {
        return hash.clone();
    }

    private static byte[] requireLength(byte[] value, int expectedLength, String name) {
        Objects.requireNonNull(value, name);
        if (value.length != expectedLength) {
            throw new IllegalArgumentException(name + " has an unsupported length");
        }
        return value.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PasswordCredential that)) {
            return false;
        }
        return iterations == that.iterations
                && keyLength == that.keyLength
                && algorithm == that.algorithm
                && Arrays.equals(salt, that.salt)
                && Arrays.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(algorithm, iterations, keyLength);
        result = 31 * result + Arrays.hashCode(salt);
        result = 31 * result + Arrays.hashCode(hash);
        return result;
    }
}
