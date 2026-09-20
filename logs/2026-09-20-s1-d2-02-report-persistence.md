# 2026-09-20 — S1-D2-02 JSON report persistence

- Verifier: Repository owner
- Environment: Windows, Java 25.0.4, Finders Keepers 0.1.0
- Prompt or action: Plan and implement repository-only JSON persistence for
  canonical lost-and-found reports.
- Expected result: Reports can be inserted, loaded through a fresh repository
  instance, and replaced safely without losing ordering or changing immutable
  identity fields. Invalid or inaccessible storage must block operations without
  damaging existing data.
- Observed result: The canonical report model, strict versioned JSON repository,
  bounded storage, safe atomic replacement, typed failures, and automated
  verification were implemented on `royden/feat-report-persistence`.
- Verification: Repository owner reviewed the draft and approved adding this log
  on 2026-09-20.
- Status: pass
- Follow-up: Review the branch and separately authorize push or pull-request
  creation. Application wiring remains a separate feature.

## Decisions

- Deliver persistence as a repository-only feature without changing
  `FindersKeepersApp`, authentication, or startup wiring.
- Create one shared canonical `ItemReport` because the owning domain
  implementation was unavailable and blocking persistence.
- Require all eleven report fields and preserve accepted text without trimming
  or normalization.
- Keep `ItemCategory` limited to `OTHER` until the domain owner adds
  compatibility-reviewed values.
- Provide `loadAll`, `insert`, and whole-report `replace`; exclude deletion and
  individual lookup.
- Treat Report ID, Reporter ID, and Creation Time as immutable during
  replacement.
- Use strict version-one UTF-8 JSON with an inclusive 16 MiB limit.
- Support one application process using one shared repository instance.
- Store private identifying details as plaintext while keeping report contents
  out of diagnostics and documentation examples.

## Work completed

- Added the canonical immutable report model and persisted enums.
- Added ordered JSON loading, insertion, and replacement.
- Added duplicate, missing-target, and immutable-field conflict handling.
- Added strict rejection of malformed JSON, invalid UTF-8, unsupported versions,
  invalid canonical values, unknown or duplicate fields, and duplicate Report
  IDs.
- Added deterministic Unicode and structural-injection protection.
- Added bounded reads and encoding with over-limit preservation.
- Added same-directory temporary staging, forced writes, and one atomic
  replacement attempt without a non-atomic fallback.
- Added recovery, concurrency, fault-injection, privacy, and capacity tests.
- Added the storage-format contract, Developer Guide coverage, and completed
  requirements-to-tests evidence.
- Kept `evals/` untracked and excluded from every commit.

## Verification evidence

The focused report-domain and persistence suite passed:

```text
gradlew.bat test --tests "io.github.cs32272610mp2xcode.finderskeepers.report.model.*" --tests "io.github.cs32272610mp2xcode.finderskeepers.report.persistence.*"
```

The complete repository gate also passed:

```text
gradlew.bat check
```

The full gate included compilation, JUnit, Checkstyle, Javadoc, and JaCoCo
report generation.

Traceability review found:

- all 17 PRD requirement identifiers represented;
- all 16 acceptance criteria represented;
- all five scenarios represented;
- 32 test-evidence definitions;
- seven review-evidence definitions;
- one verification-gate definition; and
- no undefined or duplicate evidence identifiers.

## Known boundaries

- Persistence is not connected to the JavaFX application.
- No user-facing report workflow exists yet.
- Status-transition policy remains outside the repository.
- Multiple repository instances or processes writing concurrently are
  unsupported.
- Encryption, filesystem access-control configuration, migration, backup,
  repair, and journaling are outside this feature.
- The branch has not been pushed or merged.
