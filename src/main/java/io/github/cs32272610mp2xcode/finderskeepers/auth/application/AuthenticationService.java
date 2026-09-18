package io.github.cs32272610mp2xcode.finderskeepers.auth.application;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordHasher;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordPolicy;

/** Verifies local credentials without exposing account lookup details. */
public final class AuthenticationService {
    private final UserRepository repository;

    private final PasswordHasher passwordHasher;

    /**
     * Creates the authentication service.
     *
     * @param userRepository local account source
     * @param hasher password verifier
     */
    public AuthenticationService(UserRepository userRepository, PasswordHasher hasher) {
        repository = Objects.requireNonNull(userRepository, "userRepository");
        passwordHasher = Objects.requireNonNull(hasher, "hasher");
    }

    /**
     * Authenticates a username and password.
     *
     * @param username entered username
     * @param password entered password; cleared before this method returns
     * @return generic invalid result, safe storage error, or authenticated identity
     */
    public AuthenticationResult authenticate(String username, char[] password) {
        try {
            if (username == null || username.trim().isEmpty()
                    || PasswordPolicy.isBlank(password)) {
                return AuthenticationResult.invalidCredentials();
            }
            Optional<UserAccount> account = repository.findByUsername(username.trim());
            if (account.isEmpty()
                    || !passwordHasher.verify(password, account.orElseThrow().credential())) {
                return AuthenticationResult.invalidCredentials();
            }
            UserAccount verified = account.orElseThrow();
            return AuthenticationResult.success(new AuthenticatedUser(
                    verified.userId(), verified.username(), verified.role()));
        } catch (UserStoreException exception) {
            return AuthenticationResult.storageError();
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
