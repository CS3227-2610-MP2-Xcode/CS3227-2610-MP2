# Finders Keepers

Finders Keepers is a planned lost-and-found desktop application for primary schools. It will help students report missing or found belongings and help school staff manage safe item returns. The project currently provides a JavaFX shell, an independent local-authentication module, automated checks, cross-platform packaging, and developer documentation.

Local login, role routing, and logout are implemented but are not yet connected to the Developer 1-owned application shell. The Student and Desk Officer feature workflows remain planned.

## Requirements

- Java 25 must be the active Java version (`java --version`).
- No separate Gradle installation is needed; the Gradle Wrapper is included.

## Commands

On macOS or Linux:

```bash
./gradlew run       # Start the application
./gradlew test      # Run automated tests
./gradlew check     # Run tests, Checkstyle, Javadoc, and reports
./gradlew release   # Build the cross-platform release JAR
```

On Windows PowerShell or Command Prompt:

```text
gradlew.bat run
gradlew.bat test
gradlew.bat check
gradlew.bat release
```

After building the release, run it with:

```bash
java -jar release/FindersKeepers.jar
```

## Planned roles

- **Student** — a primary-school student who will report a lost or found item and check for updates.
- **Desk Officer** — a school staff member who will review reports, manage item status, and coordinate safe collection.

These descriptions define the intended responsibility boundary. Authentication
can route to both role destinations after shell integration, but neither role's
lost-and-found workflow is implemented yet.

## Local authentication status

Authentication is separated by responsibility under `finderskeepers.auth`:
`application`, `model`, `security`, `persistence`, `provisioning`, `ui`, and
`bootstrap`. `AuthenticationCoordinator` owns login/session behavior, while
`AuthenticationFactory.createCoordinator(Path)` is the production composition
point. The JavaFX pane depends only on the coordinator.

The version 1 JSON credential-store schema remains unchanged. It accepts only
`PBKDF2WithHmacSHA256` credentials with 210,000 to 1,000,000 iterations, a
256-bit key, a 16-byte salt, and a 32-byte hash. Newly provisioned credentials
use 600,000 iterations. Stores are decoded as strict UTF-8 and are limited to
1 MiB. The repository assumes a single writer.

Passwords are not trimmed: meaningful surrounding spaces remain part of the
password, while empty and whitespace-only passwords are rejected. Temporary
password arrays are cleared after use. The masked password field remains
populated after a failed login and is cleared after success or **Clear**.

After compiling classes, local accounts can be provisioned through
`io.github.cs32272610mp2xcode.finderskeepers.auth.provisioning.AccountProvisioningTool`.
See the [Developer Guide](docs/DeveloperGuide.md) for the full command and
safety constraints.

Application-shell wiring, release-JAR refresh, and demo-store packaging remain
pending shared integration work. The current startup and committed release JAR
continue to show the placeholder shell.

## Documentation

- [User Guide](docs/UserGuide.md)
- [Developer Guide](docs/DeveloperGuide.md)
- [Reflections](docs/Reflections.md)
- [Human-verified interaction logs](logs/README.md)
