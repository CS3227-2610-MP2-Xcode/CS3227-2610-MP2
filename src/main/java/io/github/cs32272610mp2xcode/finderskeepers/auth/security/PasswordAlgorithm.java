package io.github.cs32272610mp2xcode.finderskeepers.auth.security;

import java.util.Objects;

/** Password-derivation algorithms supported by credential-store version 1. */
public enum PasswordAlgorithm {
    /** PBKDF2 using HMAC-SHA-256. */
    PBKDF2_HMAC_SHA256("PBKDF2WithHmacSHA256");

    private final String storageValue;

    PasswordAlgorithm(String value) {
        storageValue = value;
    }

    /**
     * Gets the stable algorithm name written to the credential store.
     *
     * @return stored algorithm name
     */
    public String storageValue() {
        return storageValue;
    }

    /**
     * Resolves an algorithm name from the credential store.
     *
     * @param value stored algorithm name
     * @return supported algorithm
     * @throws IllegalArgumentException when the algorithm is unsupported
     */
    public static PasswordAlgorithm fromStorageValue(String value) {
        Objects.requireNonNull(value, "value");
        for (PasswordAlgorithm algorithm : values()) {
            if (algorithm.storageValue.equals(value)) {
                return algorithm;
            }
        }
        throw new IllegalArgumentException("Unsupported password algorithm");
    }
}
