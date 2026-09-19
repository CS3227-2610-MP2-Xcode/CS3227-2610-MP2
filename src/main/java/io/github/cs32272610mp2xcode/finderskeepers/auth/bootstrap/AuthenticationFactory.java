package io.github.cs32272610mp2xcode.finderskeepers.auth.bootstrap;

import java.nio.file.Path;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationCoordinator;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationService;
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
        AuthenticationService service = new AuthenticationService(
                repository, new Pbkdf2PasswordHasher());
        return new AuthenticationCoordinator(service);
    }
}
