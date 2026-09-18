package io.github.cs32272610mp2xcode.finderskeepers.auth.model;

import java.util.Objects;

/**
 * Minimum non-secret identity retained for an authenticated session.
 *
 * @param userId stable user identifier
 * @param username display and login username
 * @param role stored application role
 */
public record AuthenticatedUser(String userId, String username, UserRole role) {
    /** Validates session identity. */
    public AuthenticatedUser {
        userId = requireText(userId, "userId");
        username = requireText(username, "username");
        role = Objects.requireNonNull(role, "role");
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
