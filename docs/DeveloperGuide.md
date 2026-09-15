# Finders Keepers Developer Guide

## Scope

This guide describes the implemented S1-D1-01 project baseline. Feature architecture will be added as the Community Member and Desk Officer workflows are implemented.

## Development prerequisites

- Java 25
- The included project Gradle Wrapper

## Current design

The current source tree is intentionally small:

- `Launcher` is the plain Java entry point used by Gradle and the packaged JAR.
- `FindersKeepersApp` owns the JavaFX lifecycle and creates the placeholder scene.
- `AppMetadata` is the single source of truth for the application name and version.
- `app.css` keeps presentation rules separate from the Java scene construction.

`Launcher` delegates to `FindersKeepersApp`; there is no role, domain, storage, or authentication layer yet. Future features should keep shared services independent of the role-specific user interfaces.

## Useful commands

On macOS or Linux:

```bash
./gradlew run
./gradlew test
./gradlew check
./gradlew release
```

On Windows:

```text
gradlew.bat run
gradlew.bat test
gradlew.bat check
gradlew.bat release
```

`check` compiles the project, runs JUnit, Checkstyle, Javadoc checks, and generates the JaCoCo report. `release` produces `release/FindersKeepers.jar`.

## Testing and quality gates

- JUnit 5 provides automated tests. The baseline tests verify the application identity and Java 25 runtime.
- Java compilation enables all lint warnings and treats warnings as errors.
- Checkstyle runs against production and test sources.
- Javadoc warnings fail the build.
- JaCoCo writes HTML and XML coverage reports; a numerical coverage threshold will be introduced when testable feature logic exists.

## Release packaging

The Gradle `fatJar` task packages application classes, CSS, JavaFX, and its native libraries. `verifyUniversalJar` checks for the Windows x64, Linux x64, and macOS ARM64 JavaFX launchers and native Glass libraries before `release` copies the JAR into `release/`.

The JAR also supports `--smoke-test`, which opens the application and exits automatically. This argument is for automated verification rather than normal use.

## Continuous integration

`.github/workflows/ci.yml` runs for pushes and pull requests on Ubuntu, macOS, and Windows. Each job installs Java 25, validates the Gradle Wrapper, runs `clean check release`, smoke-tests the exact release JAR, and uploads it as a workflow artifact. The workflow can only be confirmed on GitHub after the first push; its equivalent build and smoke checks can be run locally beforehand.

## Planned areas

- Community Member workflows — to be designed and implemented.
- Desk Officer workflows — to be designed and implemented.
- Shared domain and storage services — to be designed before both role features depend on them.
- Role-specific JavaFX views and navigation — to be documented with the feature implementation.

## Contribution notes

Keep future entries grounded in the current source tree. Add tests for observable behaviour, update both guides with each feature, and record design decisions when they become verifiable.

## Branch and pull-request workflow

Create a branch for every change that is not a tiny correction to the current
baseline. Branch names use this format:

```text
<name>/<feat|bug|doc>-<feature-name>
```

Examples:

```text
alex/feat-community-item-report
mei/bug-duplicate-claim-check
alex/doc-testing-guide
```

Use a short kebab-case feature name. The first component identifies the
developer, and the second component states the change type:

- `feat` adds user-facing behaviour.
- `bug` fixes incorrect behaviour.
- `doc` changes documentation or project records.

Open a pull request into `main` when the branch is ready. Every pull request
must complete these gates before merging:

1. Greptile review has run and its actionable findings are addressed or
   explicitly discussed.
2. A teammate has performed and approved a manual review.
3. A different teammate (not the pull-request author) merges the approved pull
   request.

The pull-request description should state the user or developer-facing change,
the tests and checks run, and any known limitations. Keep unrelated changes out
of the pull request so that both automated and manual review remain focused.

## Commit convention

Make each commit one coherent, reversible unit of work. Prefer the format:

```text
<type>(<scope>): <imperative summary>
```

Use these types consistently:

- `feat` — one user-facing feature or vertical slice.
- `fix` — one defect correction.
- `docs` — guides, logs, reflections, or other documentation.
- `test` — tests without production behaviour changes.
- `build` — Gradle, dependencies, packaging, or local tooling.
- `ci` — GitHub Actions and other pipeline changes.
- `refactor` — behaviour-preserving code restructuring.
- `chore` — narrowly scoped maintenance.

Keep commits small enough that another developer can understand and review the
change from its message and diff. Do not combine an unrelated feature, a broad
formatting pass, and a release artifact in one commit. Generated build output
under `build/` is not committed; the distributable JAR under `release/` is
committed separately when a release is intentionally updated.

For this initial scaffold, the local history is intentionally separated as:

1. `build: add Java 25 Gradle and quality scaffold`
2. `feat: add initial JavaFX application shell`
3. `docs: add project guides and interaction log`
4. `ci: add multi-platform build and release workflow`
5. `build: add initial Finders Keepers release artifact`

Future work should follow the same separation: implement one feature, add its
tests, document it, and update the release artifact only in the release commit
when the team has agreed that the version is ready.

## Acknowledgements

The repository layout and Gradle quality/release approach were informed by the team's earlier CS3227 MP1 project, as requested for structural reference. The Finders Keepers application code and documentation were written afresh for MP2; no MP1 feature implementation was reused. The build uses Gradle, OpenJFX, JUnit 5, Checkstyle, and JaCoCo.
