package io.github.cs32272610mp2xcode.finderskeepers.auth.model;

import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordCredential;

/**
 * A local account containing identity, role, and password-verification material.
 *
 * @param userId stable user identifier
 * @param username login username
 * @param role stored application role
 * @param credential password-verification material
 */
public record UserAccount(
        String userId,
        String username,
        UserRole role,
        PasswordCredential credential) {

    /** Validates a local account. */
    public UserAccount {
        userId = requireText(userId, "userId");
        username = requireText(username, "username");
        role = Objects.requireNonNull(role, "role");
        credential = Objects.requireNonNull(credential, "credential");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}
