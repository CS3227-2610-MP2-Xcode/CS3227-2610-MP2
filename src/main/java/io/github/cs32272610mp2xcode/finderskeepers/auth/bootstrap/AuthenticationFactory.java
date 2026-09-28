package io.github.cs32272610mp2xcode.finderskeepers.auth.bootstrap;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationCoordinator;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationService;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.RegistrationService;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.BundledUserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.JsonUserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.Pbkdf2PasswordHasher;

/** Composition factory for local login and self-registration. */
public final class AuthenticationFactory {
    private AuthenticationFactory() {
    }

    /**
     * Creates a coordinator backed by the supplied local credential store.
     * Login and registration share one repository and PBKDF2 hasher so an
     * account created through the coordinator is immediately available to
     * later login attempts.
     *
     * @param userStorePath local JSON credential-store path
     * @return fully composed login and registration coordinator
     * @throws NullPointerException when the store path is absent
     */
    public static AuthenticationCoordinator createCoordinator(Path userStorePath) {
        Path store = Objects.requireNonNull(userStorePath, "userStorePath");
        return createCoordinator(new JsonUserRepository(store));
    }

    /**
     * Creates the coordinator for the read-only demonstration accounts bundled in the app.
     *
     * @return demonstration login coordinator independent of the working directory
     */
    public static AuthenticationCoordinator createDemoCoordinator() {
        return createCoordinator(new BundledUserRepository());
    }

    private static AuthenticationCoordinator createCoordinator(UserRepository repository) {
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        AuthenticationService service = new AuthenticationService(repository, hasher);
        RegistrationService registration = new RegistrationService(
                repository, hasher, UUID::randomUUID);
        return new AuthenticationCoordinator(service, registration);
    }
}
