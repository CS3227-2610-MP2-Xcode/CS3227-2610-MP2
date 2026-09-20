# Finders Keepers Developer Guide

## Scope

This guide describes the implemented project baseline, integrated Developer 2 local-authentication module, and S1-D2-02 report-persistence foundation for a primary-school lost-and-found application. The approved application-shell integration opens authentication at startup; report persistence remains available behind its repository interface until an owning workflow selects its store path and lifetime.

## Development prerequisites

- Java 25
- The included project Gradle Wrapper

## Current design

The current source tree keeps application startup separate from the shared report foundation:

- `Launcher` is the plain Java entry point used by Gradle and the packaged JAR.
- `FindersKeepersApp` owns the JavaFX lifecycle and creates the authentication scene.
- `AppMetadata` is the single source of truth for the application name and version.
- `app.css` keeps presentation rules separate from the Java scene construction.
- `report.model` contains the canonical immutable `ItemReport` and its persisted enums.
- `report.persistence` exposes `ReportRepository` and its strict, ordered, versioned JSON implementation.

`Launcher` delegates to `FindersKeepersApp`, which composes the authentication coordinator for `data/demo-users.json` and displays `AuthenticationPane`. Future features should use simple, age-appropriate language for students and keep shared services independent of the role-specific user interfaces.

Report persistence is deliberately repository-only: application startup does not yet construct or wire a report repository. The repository stores all canonical report fields in a caller-selected file and preserves insertion order across reconstruction. It performs bounded strict reads and all-or-nothing atomic replacement with no unsafe fallback. See [S1-D2-02 Report Storage Format](features/S1-D2-02/StorageFormat.md) for the public boundary, JSON contract, failure behavior, privacy limits, and operating assumptions.

## Local authentication design

Authentication code is grouped by responsibility under `finderskeepers.auth`:

- `application` contains `AuthenticationCoordinator`, `AuthenticationService`,
  authentication results and statuses, and application routes.
- `model` contains account, authenticated-user, and role values.
- `security` contains the password policy, credential value, hashing interface,
  algorithm enum, and PBKDF2 implementation.
- `persistence` contains the repository interface, JSON repository, internal
  JSON codec, and storage exception.
- `provisioning` contains account-provisioning behavior and its command-line
  tool.
- `ui` contains `AuthenticationPane` and its internal route presentation.
- `bootstrap` contains `AuthenticationFactory`, the production composition
  root for this independent module.

The dependencies flow in one direction:

```text
AuthenticationPane -> AuthenticationCoordinator -> AuthenticationService
AuthenticationFactory -> AuthenticationCoordinator
                      -> JsonUserRepository -> UserStoreJsonCodec
                      -> Pbkdf2PasswordHasher
AuthenticationService -> UserRepository
AuthenticationService -> PasswordHasher
```

- `AuthenticationFactory.createCoordinator(Path)` owns production wiring for
  the JSON repository, PBKDF2 hasher, authentication service, and coordinator.
  The JavaFX pane accepts only an `AuthenticationCoordinator` and does not know
  concrete persistence or hashing types.
- `AuthenticationCoordinator` stores only the current authenticated user and a
  validation message. It derives the route from the user's role. A failed login
  preserves an existing authenticated session; logout clears it and returns to
  the login route.
- `JsonUserRepository` receives its storage path from the caller. An absent file
  means that no accounts are configured. Unreadable, malformed,
  unsupported-version, duplicate, incomplete, oversized, or invalid UTF-8 data
  produces a safe storage failure and is never silently replaced.
- Reads are bounded to detect files larger than 1 MiB, and JSON is decoded with
  strict UTF-8 handling. Writes are rejected if their encoded form would exceed
  the same limit. Successful writes add one account through a temporary file
  and atomic move where the filesystem supports it. Existing user IDs and
  case-insensitive usernames are not overwritten. The repository is designed
  for a single writer; cross-process locking is deferred.
- Version 1 credentials must name `PBKDF2WithHmacSHA256`, use 210,000 to
  1,000,000 iterations inclusive, use a 256-bit derived key, decode to exactly
  16 salt bytes, and decode to exactly 32 hash bytes. The repository rejects
  invalid metadata before invoking cryptographic work.
- `Pbkdf2PasswordHasher` creates new credentials with a cryptographically
  random 16-byte salt, 600,000 iterations, and a 256-bit derived key. Existing
  valid 210,000-iteration demo credentials remain readable.
- `AuthenticationService` trims usernames and looks them up case-insensitively.
  Passwords remain case-sensitive and are not trimmed. Empty and entirely
  whitespace passwords are rejected, but meaningful leading or trailing
  whitespace is preserved. Temporary password arrays are cleared after every
  authentication and provisioning attempt.
- Invalid credentials share one generic message. The masked JavaFX password
  field remains populated after a failed login and is cleared only after a
  successful login or the explicit **Clear** action.
- Authenticated state contains only user ID, username, and role. Passwords,
  hashes, and salts never enter it.

## Student report-submission components

The Student submission slice is implemented as a UI/application boundary, but
it is not yet wired into the running application. Its components are:

- `report.application.ReportSubmitter` is the application boundary for creating
  and saving one report. It accepts a `ReportCreationRequest` and
  returns the saved `ItemReport`.
- `report.application.ReportSubmissionService` implements that boundary. It
  generates the report ID, creates and validates the domain object, and only
  then calls its injected `Consumer<ItemReport>` storage adapter. This keeps
  invalid input away from persistence without duplicating Developer 2's
  `ReportRepository` contract.
- `report.application.ReportSubmissionException` is the safe boundary for a
  persistence or integration failure. Its technical cause is retained for
  diagnostics, but it is not shown to the Student.
- `report.ui.ReportFormInput` is an immutable snapshot of the editable form
  values: lost/found type, item name, category, location, occurrence date,
  public description, and private identifying detail. It adds the reporter ID
  supplied by the authenticated session when creating a domain request.
- `report.ui.ReportFormField` is the stable mapping between domain validation
  field names and the editable controls.
- `report.ui.SubmissionViewState` is the presentation result. A successful
  state contains the saved report ID; an invalid state contains field-specific
  messages; a storage-failure state contains only a safe retry/help message.
  Failed states never contain a report ID, and field-error maps are immutable.
- `report.ui.StudentReportFormController` binds one authenticated reporter to
  a `ReportSubmitter`, converts input into a domain request, maps
  `ReportValidationException` errors to `ReportFormField`, and translates
  `ReportSubmissionException` into a generic storage-failure state.
- `report.ui.StudentReportForm` is the JavaFX view. It presents the editable
  controls, renders field errors and feedback, clears inputs after success or
  on **Clear**, and retains input after a storage failure so the Student can
  retry.

The authenticated identity is not an editable form field. Integration must
obtain the current `AuthenticatedUser` from the authentication session and pass
`AuthenticatedUser.userId()` to `StudentReportFormController`; the controller
rejects a missing or blank identity. The username is display-only. The public
description is intended for matching, while the private identifying detail is
reserved for staff verification and is not included in confirmations.

The submission flow is:

```text
AuthenticatedUser.userId()
        -> StudentReportFormController
        -> ReportFormInput.toCreationRequest(reporterId)
        -> ReportSubmissionService.submit(request)
        -> injected report-storage adapter
        -> saved ItemReport / validation state / storage-failure state
        -> StudentReportForm feedback and field messages
```

`ReportRepository`, concrete JSON persistence, and the adapter from its save
operation into `ReportSubmissionService` are Developer 2/shared integration
dependencies. That adapter must translate storage failures into
`ReportSubmissionException`. Authentication route wiring (including the
Student route in `AuthenticationPane`) is also still required. Until those
dependencies are connected, the form is not reachable from the current
application startup.

The version 1 credential store has this shape; values below are descriptive placeholders, not credentials:

```json
{
  "version": 1,
  "accounts": [
    {
      "userId": "synthetic-id",
      "username": "synthetic.username",
      "role": "STUDENT",
      "algorithm": "PBKDF2WithHmacSHA256",
      "iterations": 210000,
      "keyLength": 256,
      "salt": "base64-encoded random bytes",
      "passwordHash": "base64-encoded derived bytes"
    }
  ]
}
```

### Provisioning local accounts

Compile the project, then run the provisioning utility from the repository root. It accepts the store path, user ID, username, and canonical role as arguments and securely prompts for the password:

```text
gradlew.bat classes
java -cp build\classes\java\main io.github.cs32272610mp2xcode.finderskeepers.auth.provisioning.AccountProvisioningTool data\users.json student-001 student.name STUDENT
```

Use `DESK_OFFICER` for a Desk Officer. The utility permits only the two canonical roles, masks password entry, rejects empty and whitespace-only passwords without trimming valid passwords, clears its temporary password array, does not print credential material, and refuses duplicate identifiers or usernames. It creates credentials using the 600,000-iteration default and does not offer replacement; account administration is outside this feature.

`data/demo-users.json` retains the version 1 schema and contains the two explicitly synthetic accounts listed in the User Guide. Never use that public store or those demonstration passwords for real users. Application startup resolves this external file relative to the working directory. It is not embedded in the release JAR, so launch from the repository root until a shared distribution layout is approved.

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

- JUnit 5 provides automated tests. In addition to the baseline tests,
  authentication tests cover the coordinator's login/session behavior,
  credential whitespace and array clearing, both role routes, logout, factory
  wiring, PBKDF2 verification and salt uniqueness, provisioning validation,
  bounded JSON persistence, missing storage, corrupt metadata, and plaintext
  exclusion.
- Report domain and persistence tests cover report invariants, reconstruction,
  ordering, replacement conflicts, strict JSON and Unicode handling, resource
  bounds, atomic-write failures, recovery behavior, and supported
  shared-instance concurrency. Persistence tests use JUnit temporary
  directories only.
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

- Student report submission UI/controller components — implemented in an
  isolated slice; repository/service composition and authenticated route wiring
  remain to be integrated.
- Student status features — to be designed and implemented.
- Desk Officer workflows for reviewing reports and coordinating collection — to be designed and implemented.
- Repository construction and startup wiring — to be integrated once the owning application workflow selects its store path and lifetime.
- Richer category and status vocabularies, submission validation, and report creation — to be extended through the shared canonical model without introducing a competing report type.
- Role-specific JavaFX views and navigation — to be documented with the feature implementation.
- Deciding how the external demo credential store is supplied with a
  distributable release — pending shared integration approval.

## Report domain contract (S1-D1-02)

The report domain is independent of JavaFX and persistence. It lives under
`io.github.cs32272610mp2xcode.finderskeepers.report` and provides the shared
contract used by Student submission and Desk Officer storage/review features.

`ItemReport` is an immutable final value object with these eleven fields and
accessors:

```text
UUID reportId()
String reporterId()
ReportType reportType()
String itemName()
ItemCategory category()
String location()
LocalDate occurrenceDate()
String publicDescription()
String privateIdentifyingDetail()
ReportStatus status()
Instant createdAt()
```

`ReportCreationRequest` contains the eight user-supplied values: reporter ID,
report type, item name, category, location, occurrence date, public
description, and private identifying detail. Report ID, status, and creation
time are controlled by the domain.

New reports are created with `ItemReport.create(UUID, ReportCreationRequest,
Clock)`. The factory validates the request, always assigns `SUBMITTED`, and
uses the supplied clock for `createdAt`, truncated to milliseconds. Storage
reconstruction uses the clockless `ItemReport.restore(...)`, which validates a
complete stored record while preserving its status, creation time, and text
exactly. The clock-taking overload is for importing external records when a
future-date check is also required. `withStatus(...)` returns a complete
updated copy and never mutates the original. Equality is value-based across all
eleven fields.

### Validation and persistence-facing values

All eleven fields are required. Null and blank text are separate validation
cases. Creation strips surrounding whitespace before measuring text in Unicode
code points. Restoration preserves surrounding whitespace, rejects entirely
blank values, and measures the exact stored value so persistence can round-trip
data without changing it. Reporter ID is limited to 128, item name to 100,
location to 120, and each of the two descriptions to 500. Future occurrence
dates are rejected when creating a report using the provided clock.
`occurrenceDate` is a `LocalDate` stored strictly as
`yyyy-MM-dd`. `createdAt` is an `Instant` stored in UTC with millisecond
precision, such as `2026-09-19T07:15:30.123Z`.

`reportId` is a UUID serialized using its canonical string form and compared by
UUID value. `reporterId` is an opaque, case-sensitive string; it is not
lowercased or treated as a display name. The persistence property names are
`reportId`, `reporterId`, `reportType`, `itemName`, `category`, `location`,
`occurrenceDate`, `publicDescription`, `privateIdentifyingDetail`, `status`,
and `createdAt`.

The enums have these exact stored names. Persistence must call `storedName()`
when writing and `fromStoredName(...)` when reading rather than maintaining a
second switch or duplicate enum:

```text
ReportType: LOST, FOUND
ItemCategory: STATIONERY, BOOKS, CLOTHING, BAGS, WATER_BOTTLES,
              ELECTRONICS, SPORTS_EQUIPMENT, PERSONAL_ITEMS, OTHER
ReportStatus: SUBMITTED, UNDER_REVIEW
```

Enum parsing is strict and case-sensitive. Unknown, null, or blank stored names
are invalid. The public and private descriptions are intentionally separate;
`ItemReport.toString()` is always `ItemReport[redacted]`, and
`ReportCreationRequest.toString()` exposes neither description.

The repository must import these canonical types from the `report` package. It
must not declare a parallel `report.model.ItemReport` or duplicate enums. The
JSON decoder should pass the eleven decoded values to the clockless
`ItemReport.restore(...)`; this keeps domain validation in one place and allows
all supported categories to round-trip.

The domain reports field-specific, readable validation errors through an
immutable validation-error collection. JSON/file handling remains a storage
responsibility and must not be added to this domain package.

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
alex/feat-student-item-report
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
