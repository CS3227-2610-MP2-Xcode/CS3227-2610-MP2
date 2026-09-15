# Finders Keepers User Guide

## Current status

This guide describes the initial scaffold only. Finders Keepers opens a placeholder window that confirms the project is ready. It does not yet implement the planned lost-and-found workflows.

## Requirements

Install Java 25 before running the project. Confirm the active version:

```bash
java --version
```

The first line should report Java 25. The included Gradle Wrapper downloads the matching Gradle distribution when first used, so a separate Gradle installation is not required.

## Start the application

On macOS or Linux:

```bash
./gradlew run
```

On Windows PowerShell or Command Prompt:

```text
gradlew.bat run
```

The window should show **Finders Keepers** and **Project scaffold ready**.

## Build and run the packaged application

On macOS or Linux:

```bash
./gradlew release
java -jar release/FindersKeepers.jar
```

On Windows:

```text
gradlew.bat release
java -jar release\FindersKeepers.jar
```

## Test the project

Run `./gradlew test` on macOS/Linux or `gradlew.bat test` on Windows. Run `check` instead of `test` when you also want the code-quality and documentation checks.

## Planned roles (not yet implemented)

### Community Member

This role is intended for people who lose or find items in the community. Planned responsibilities include submitting an item report, reviewing possible matches, and following up on a claim. These are requirements for later sprints, not current features.

### Desk Officer

This role is intended for help-desk staff. Planned responsibilities include reviewing reports, updating item status, and coordinating a safe handover. These are requirements for later sprints, not current features.

## Troubleshooting

- If Gradle cannot start, confirm that Java 25 is installed and that `java --version` reports the expected version.
- If a command is not found, run it from the Xcode project directory and use the platform-specific wrapper command above.
- If a release artifact is missing, run the `release` command again and check `release/FindersKeepers.jar` after it completes.
- If a documented workflow is absent, treat it as unimplemented and check the project status before reporting a defect.
