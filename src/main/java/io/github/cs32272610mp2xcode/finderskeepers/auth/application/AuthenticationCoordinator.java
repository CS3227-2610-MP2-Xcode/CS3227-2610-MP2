package io.github.cs32272610mp2xcode.finderskeepers.auth.application;

import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;

/** Coordinates login, in-memory session state, and role-derived navigation. */
public final class AuthenticationCoordinator {
    private final AuthenticationService authenticationService;

    private AuthenticatedUser currentUser;

    private String message = "";

    /**
     * Creates a coordinator without an authenticated user.
     *
     * @param service credential verifier
     */
    public AuthenticationCoordinator(AuthenticationService service) {
        authenticationService = Objects.requireNonNull(service, "service");
    }

    /**
     * Attempts login and adopts the stored account identity only on success.
     *
     * @param username entered username
     * @param password entered password; cleared by the authentication service
     * @return authentication outcome
     */
    public AuthenticationResult login(String username, char[] password) {
        AuthenticationResult result = authenticationService.authenticate(username, password);
        message = result.message();
        if (result.status() == AuthenticationStatus.SUCCESS) {
            currentUser = result.user().orElseThrow();
        }
        return result;
    }

    /** Clears the current validation message without changing the session. */
    public void clearValidation() {
        message = "";
    }

    /** Clears the authenticated user and validation message. */
    public void logout() {
        currentUser = null;
        message = "";
    }

    /**
     * Derives the current application route from the authenticated identity.
     *
     * @return login or the route belonging to the stored user role
     */
    public ApplicationRoute route() {
        if (currentUser == null) {
            return ApplicationRoute.LOGIN;
        }
        return switch (currentUser.role()) {
            case STUDENT -> ApplicationRoute.STUDENT;
            case DESK_OFFICER -> ApplicationRoute.DESK_OFFICER;
        };
    }

    /**
     * Gets the current authenticated identity.
     *
     * @return current identity, when logged in
     */
    public Optional<AuthenticatedUser> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    /**
     * Gets the current user-facing validation message.
     *
     * @return current validation message
     */
    public String message() {
        return message;
    }
}
