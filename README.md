# Finders Keepers

Finders Keepers is a planned lost-and-found desktop application for primary schools. It will help students report missing or found belongings and help school staff manage safe item returns. This initial milestone provides a working JavaFX shell, automated checks, cross-platform packaging, and documentation so both developers can begin feature work from the same baseline.

The Student and Desk Officer workflows are planned but not implemented yet.

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

These descriptions define the intended responsibility boundary only; neither role is available in this scaffold.

## Documentation

- [User Guide](docs/UserGuide.md)
- [Developer Guide](docs/DeveloperGuide.md)
- [Reflections](docs/Reflections.md)
- [Human-verified interaction logs](logs/README.md)
