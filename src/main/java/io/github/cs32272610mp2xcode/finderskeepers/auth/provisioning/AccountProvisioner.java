package io.github.cs32272610mp2xcode.finderskeepers.auth.provisioning;

import java.util.Arrays;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordCredential;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordHasher;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordPolicy;

/** One-time service for adding local accounts without storing plaintext passwords. */
public final class AccountProvisioner {
    private final UserRepository repository;

    private final PasswordHasher passwordHasher;

    /**
     * Creates an account provisioner.
     *
     * @param userRepository target local account store
     * @param hasher password hashing implementation
     */
    public AccountProvisioner(UserRepository userRepository, PasswordHasher hasher) {
        repository = Objects.requireNonNull(userRepository, "userRepository");
        passwordHasher = Objects.requireNonNull(hasher, "hasher");
    }

    /**
     * Adds one supported local account and refuses duplicate identity data.
     *
     * @param userId stable identifier
     * @param username login username
     * @param roleText canonical role name
     * @param password password characters; cleared before this method returns
     * @throws UserStoreException when storage cannot be read or updated
     */
    public void provision(String userId, String username, String roleText, char[] password)
            throws UserStoreException {
        try {
            String normalizedId = requireText(userId, "user ID");
            String normalizedUsername = requireText(username, "username");
            String normalizedRole = requireText(roleText, "role");
            PasswordPolicy.requireNonBlank(password);
            UserRole role;
            try {
                role = UserRole.valueOf(normalizedRole);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "role must be STUDENT or DESK_OFFICER", exception);
            }
            PasswordCredential credential = passwordHasher.hash(password);
            repository.add(new UserAccount(normalizedId, normalizedUsername, role, credential));
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
