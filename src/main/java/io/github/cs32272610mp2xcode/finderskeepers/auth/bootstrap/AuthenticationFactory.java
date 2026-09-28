package io.github.cs32272610mp2xcode.finderskeepers.auth.bootstrap;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationCoordinator;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationService;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.RegistrationService;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.JsonUserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.Pbkdf2PasswordHasher;

/** Production composition factory for local authentication. */
public final class AuthenticationFactory {
    private AuthenticationFactory() {
    }

    /**
     * Creates a coordinator backed by the supplied local credential store.
     *
     * @param userStorePath local JSON credential-store path
     * @return fully composed authentication coordinator
     */
    public static AuthenticationCoordinator createCoordinator(Path userStorePath) {
        Path store = Objects.requireNonNull(userStorePath, "userStorePath");
        JsonUserRepository repository = new JsonUserRepository(store);
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        AuthenticationService service = new AuthenticationService(repository, hasher);
        RegistrationService registration = new RegistrationService(
                repository, hasher, UUID::randomUUID);
        return new AuthenticationCoordinator(service, registration);
    }
}
