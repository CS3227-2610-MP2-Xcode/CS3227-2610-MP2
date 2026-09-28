package io.github.cs32272610mp2xcode.finderskeepers.auth.application;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordCredential;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordHasher;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordPolicy;

/**
 * Creates local accounts through the same hashed credential store used for
 * login.
 *
 * <p>Registration validates before hashing, refuses case-insensitive username
 * reuse, assigns a generated identifier, and returns only a non-secret identity.
 * Validation and storage failures leave the caller unauthenticated and do not
 * replace an existing account.</p>
 */
public final class RegistrationService {
    private final UserRepository repository;

    private final PasswordHasher passwordHasher;

    private final Supplier<UUID> userIdSupplier;

    /**
     * Creates a local registration service.
     *
     * @param userRepository account store
     * @param hasher password hashing implementation
     * @param idSupplier unique account identifier source
     */
    public RegistrationService(UserRepository userRepository, PasswordHasher hasher,
            Supplier<UUID> idSupplier) {
        repository = Objects.requireNonNull(userRepository, "userRepository");
        passwordHasher = Objects.requireNonNull(hasher, "hasher");
        userIdSupplier = Objects.requireNonNull(idSupplier, "idSupplier");
    }

    /**
     * Validates and stores a new account, clearing both password arrays before returning.
     *
     * @param username requested login name
     * @param password requested password; cleared before this method returns
     * @param confirmation repeated password; cleared before this method returns
     * @param role requested application role
     * @return registration outcome and identity on success
     */
    public RegistrationResult register(String username, char[] password,
            char[] confirmation, UserRole role) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return RegistrationResult.failure(RegistrationStatus.INVALID_USERNAME);
            }
            if (PasswordPolicy.isBlank(password)) {
                return RegistrationResult.failure(RegistrationStatus.INVALID_PASSWORD);
            }
            if (!Arrays.equals(password, confirmation)) {
                return RegistrationResult.failure(RegistrationStatus.PASSWORD_MISMATCH);
            }
            if (role == null) {
                return RegistrationResult.failure(RegistrationStatus.INVALID_ROLE);
            }

            String normalizedUsername = username.trim();
            if (repository.findByUsername(normalizedUsername).isPresent()) {
                return RegistrationResult.failure(RegistrationStatus.USERNAME_TAKEN);
            }
            String userId = Objects.requireNonNull(
                    userIdSupplier.get(), "userIdSupplier result").toString();
            PasswordCredential credential = passwordHasher.hash(password);
            UserAccount account = new UserAccount(
                    userId, normalizedUsername, role, credential);
            repository.add(account);
            return RegistrationResult.success(new AuthenticatedUser(
                    account.userId(), account.username(), account.role()));
        } catch (UserStoreException exception) {
            return RegistrationResult.failure(RegistrationStatus.STORAGE_ERROR);
        } finally {
            clear(password);
            clear(confirmation);
        }
    }

    private static void clear(char[] secret) {
        if (secret != null) {
            Arrays.fill(secret, '\0');
        }
    }
}
