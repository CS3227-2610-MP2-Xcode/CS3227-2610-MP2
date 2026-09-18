# Finders Keepers User Guide

## Current status

Finders Keepers is intended for a primary school's lost-and-found desk. Application startup now opens the local login, with Student and Desk Officer routing and logout available after authentication.

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

Both `gradlew run` and the current release JAR open the **Finders Keepers** login interface when launched from the repository root.

## Synthetic demonstration accounts

The project-local `data/demo-users.json` store contains two public, synthetic demonstration accounts:

| Role | Username | Password |
| --- | --- | --- |
| Student | `demo.student` | `Student-Demo-27!` |
| Desk Officer | `demo.officer` | `Officer-Demo-42!` |

These accounts and passwords are clearly labelled public synthetic test data. Do not reuse them for a real person, school, or system. The credential store contains salted password hashes rather than these plaintext passwords. Application startup reads the project-local store from `data/demo-users.json`. The store is not embedded in the JAR, so launch the JAR from the repository root unless a different store layout is configured later.

## Login behavior

Enter one of the usernames and passwords above and select **Log in**. Usernames ignore capitalization and surrounding spaces. Passwords are case-sensitive and are not trimmed: meaningful leading or trailing spaces are part of the password, while an empty or entirely whitespace password is rejected. Temporary password arrays used by the authentication module are cleared after each attempt. The stored account role determines which interface opens; there is no role selector.

Blank or incorrect details show `Invalid username or password.` without identifying which entry was wrong. After a failed login, the masked password field remains populated so the user can correct the attempt. A successful login clears it. **Clear** removes both fields and the message. **Log out** clears the in-memory session and returns to login.

The local store keeps its version 1 format. For safety, it accepts only
`PBKDF2WithHmacSHA256` credentials with 210,000 to 1,000,000 iterations, a
256-bit key, a 16-byte salt, and a 32-byte hash. Newly provisioned accounts use
600,000 iterations. The store must be valid UTF-8 and no larger than 1 MiB, and
it assumes one writer at a time. If any of these checks fail, authentication
reports that local accounts are unavailable rather than replacing the file.

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

## Role destinations

### Student

Successful Student login reaches a distinct Student home. Report submission and status features remain planned for later sprints.

### Desk Officer

Successful Desk Officer login reaches a distinct Desk Officer home. Report review and collection features remain planned for later sprints.

## Troubleshooting

- If Gradle cannot start, confirm that Java 25 is installed and that `java --version` reports the expected version.
- If a command is not found, run it from the Xcode project directory and use the platform-specific wrapper command above.
- If a release artifact is missing, run the `release` command again and check `release/FindersKeepers.jar` after it completes.
- If a documented workflow is absent, treat it as unimplemented and check the project status before reporting a defect.
- If login reports that local accounts are unavailable, do not edit or replace the credential store. Ask the project team to inspect or restore it.
