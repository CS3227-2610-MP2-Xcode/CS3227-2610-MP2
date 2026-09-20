package io.github.cs32272610mp2xcode.finderskeepers.auth.application;

import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;

/** Outcome of one local authentication attempt. */
public final class AuthenticationResult {
    /** Generic message for blank or invalid credentials. */
    public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password.";

    /** User-safe message for unavailable account storage. */
    public static final String STORAGE_ERROR_MESSAGE =
            "Local accounts are unavailable. Please contact the Desk Officer.";

    private final AuthenticationStatus status;

    private final AuthenticatedUser user;

    private AuthenticationResult(AuthenticationStatus resultStatus,
            AuthenticatedUser authenticatedUser) {
        status = Objects.requireNonNull(resultStatus, "resultStatus");
        user = authenticatedUser;
    }

    /**
     * Creates a successful result.
     *
     * @param authenticatedUser verified non-secret identity
     * @return successful result for the supplied identity
     */
    public static AuthenticationResult success(AuthenticatedUser authenticatedUser) {
        return new AuthenticationResult(AuthenticationStatus.SUCCESS,
                Objects.requireNonNull(authenticatedUser, "authenticatedUser"));
    }

    /**
     * Creates an invalid-credential result.
     *
     * @return invalid-credential result
     */
    public static AuthenticationResult invalidCredentials() {
        return new AuthenticationResult(AuthenticationStatus.INVALID_CREDENTIALS, null);
    }

    /**
     * Creates a safe storage-error result.
     *
     * @return safe storage-error result
     */
    public static AuthenticationResult storageError() {
        return new AuthenticationResult(AuthenticationStatus.STORAGE_ERROR, null);
    }

    /**
     * Gets the outcome category.
     *
     * @return outcome category
     */
    public AuthenticationStatus status() {
        return status;
    }

    /**
     * Gets the authenticated identity when successful.
     *
     * @return authenticated identity only on success
     */
    public Optional<AuthenticatedUser> user() {
        return Optional.ofNullable(user);
    }

    /**
     * Gets the user-facing result message.
     *
     * @return user-facing error text, or an empty string on success
     */
    public String message() {
        return switch (status) {
            case SUCCESS -> "";
            case INVALID_CREDENTIALS -> INVALID_CREDENTIALS_MESSAGE;
            case STORAGE_ERROR -> STORAGE_ERROR_MESSAGE;
        };
    }
}
