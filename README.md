# Finders Keepers

Finders Keepers is a planned lost-and-found desktop application for primary schools. It will help students report missing or found belongings and help school staff manage safe item returns. The project currently provides a JavaFX application with integrated local authentication, a persistence-backed Desk Officer submitted-report review queue, automated checks, cross-platform packaging, and developer documentation.

Application startup opens local login, with role routing and logout available
after authentication. Desk Officers can now filter submitted reports, inspect
complete details, and durably start review. Student route integration and later
collection/return workflows remain planned.

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
routes to visibly separate role destinations. The Desk Officer submitted-report
review workflow is implemented; Student submission routing and later Desk
Officer collection work remain pending.

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

Application startup and the refreshed release JAR show the login interface. Run
them from the repository root so the external `data/demo-users.json` store is
available. The JAR does not embed that store, so distributing a standalone JAR
with usable demo accounts still requires a shared packaging decision.

## Documentation

- [User Guide](docs/UserGuide.md)
- [Developer Guide](docs/DeveloperGuide.md)
- [Reflections](docs/Reflections.md)
- [Human-verified interaction logs](logs/README.md)
