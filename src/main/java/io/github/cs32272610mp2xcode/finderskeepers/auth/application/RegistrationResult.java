package io.github.cs32272610mp2xcode.finderskeepers.auth.application;

import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;

/** Outcome of one local self-registration attempt. */
public final class RegistrationResult {
    private final RegistrationStatus status;

    private final AuthenticatedUser user;

    private RegistrationResult(RegistrationStatus resultStatus,
            AuthenticatedUser authenticatedUser) {
        status = Objects.requireNonNull(resultStatus, "resultStatus");
        user = authenticatedUser;
    }

    /**
     * Creates a successful result.
     *
     * @param authenticatedUser newly created identity
     * @return successful result
     * @throws NullPointerException when the identity is absent
     */
    public static RegistrationResult success(AuthenticatedUser authenticatedUser) {
        return new RegistrationResult(RegistrationStatus.SUCCESS,
                Objects.requireNonNull(authenticatedUser, "authenticatedUser"));
    }

    /**
     * Creates a failed result without an authenticated identity.
     *
     * @param failureStatus non-success result category
     * @return failed result
     * @throws NullPointerException when the status is absent
     * @throws IllegalArgumentException when the status is {@link RegistrationStatus#SUCCESS}
     */
    public static RegistrationResult failure(RegistrationStatus failureStatus) {
        RegistrationStatus checked = Objects.requireNonNull(failureStatus, "failureStatus");
        if (checked == RegistrationStatus.SUCCESS) {
            throw new IllegalArgumentException("A successful registration requires a user");
        }
        return new RegistrationResult(checked, null);
    }

    /**
     * Gets the outcome category.
     *
     * @return registration status
     */
    public RegistrationStatus status() {
        return status;
    }

    /**
     * Gets the newly authenticated identity when registration succeeds.
     *
     * @return created identity only on success
     */
    public Optional<AuthenticatedUser> user() {
        return Optional.ofNullable(user);
    }

    /**
     * Gets user-facing feedback, or an empty string on success.
     *
     * @return safe registration feedback
     */
    public String message() {
        return switch (status) {
            case SUCCESS -> "";
            case INVALID_USERNAME -> "Enter a username.";
            case INVALID_PASSWORD -> "Enter a password that is not blank.";
            case PASSWORD_MISMATCH -> "Passwords do not match.";
            case INVALID_ROLE -> "Choose Student or Desk Officer.";
            case USERNAME_TAKEN -> "That username is already in use.";
            case STORAGE_ERROR ->
                "Local accounts are unavailable. Please contact the Desk Officer.";
        };
    }
}
