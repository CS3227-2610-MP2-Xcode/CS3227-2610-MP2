# S1-D2-02 JSON Report Persistence

- Status: Approved
- Owner: Developer 2
- Draft date: 2026-09-19
- Revision date: 2026-09-20
- Previous approval: Repository owner, 2026-09-19
- Reapprover: Repository owner
- Reapproval date: 2026-09-20
- Implementation authorization: Repository owner, 2026-09-20
- Governing instructions: `AGENTS.md`

## Goal

Provide a repository boundary that stores every canonical `ItemReport` in a
local JSON file and restores the same report state after the repository is
reconstructed.

The completed persistence flow is:

```text
Student or Desk Officer workflow
        ↓
Canonical ItemReport
        ↓
ReportRepository
        ↓
Versioned local JSON store
        ↓
Fresh repository instance
        ↓
Equivalent canonical ItemReport values
```

This mission delivers the persistence boundary only. It does not connect the
repository to either role's user interface or to application startup.

## User outcome

Later Student and Desk Officer workflows can rely on submitted and updated
reports remaining available after the application closes and reopens. A
storage problem is reported safely instead of silently losing, resetting, or
partially replacing report data.

## Scope

Developer 2 implements:

- A controlled cross-owner minimum canonical `ItemReport`, `ReportType`,
  `ItemCategory`, and `ReportStatus` contract because Developer 1's
  implementation is unavailable
- The `ReportRepository` interface over that single canonical `ItemReport`
- A local JSON-backed repository at a caller-supplied path
- Loading every stored report in stable insertion order
- Inserting one report without overwriting a duplicate identifier
- Replacing one existing report without moving it in the stored order
- Enforcement of the replacement invariants in this brief
- Strict, versioned UTF-8 encoding and decoding
- Bounded resource use for whole-store operations
- Safe handling of absent, unreadable, corrupt, unsupported, or oversized
  storage
- Safe all-or-nothing file replacement
- Focused persistence, recovery, and adversarial-input tests
- Report-storage design and format documentation

The repository operations are equivalent to:

```text
loadAll()
insert(report)
replace an existing report with a complete replacement
```

The exact Java signatures depend on the agreed canonical report contract and
are fixed in the Technical Design Document (TDD). Replacement must use an
identity-preserving canonical update mechanism when Developer 1 provides one;
otherwise its repository contract must receive the original target ID
separately from the replacement report.

## Non-goals

This task does not implement:

- A temporary, duplicate, or persistence-owned report-domain model
- Categories beyond `OTHER`, report statuses beyond `SUBMITTED` and
  `UNDER_REVIEW`, or broader report-domain behaviour
- Report creation, identifier generation, or creation-time generation
- Clock-dependent occurrence-date validation, submission requests, or
  submission-specific validation owned by Developer 1
- Report-status transition rules
- Student report submission
- Desk Officer queues, filtering, details, or review actions
- Sorting reports for presentation
- Individual report lookup or report deletion
- Application-startup or JavaFX wiring
- A supported manual JSON-editing interface
- Encryption at rest or operating-system file-permission management
- Automatic repair, backup restoration, journaling, or data migration
- Coordination between multiple application processes or repository instances
- Gradle, dependency, release, or CI changes

## Persisted report state

Every canonical report must round-trip all of these semantic values:

- Report ID
- Reporter ID
- Lost or found type
- Item name
- Category
- Location
- Occurrence date
- Public description
- Private identifying detail
- Status
- Creation time

All eleven values are required. Each text value contains at least one code point
and is preserved exactly without trimming, case conversion, Unicode
normalization, defaulting, or other reinterpretation. Location and occurrence
date cannot be absent in version 1.

Public description and private identifying detail remain separate values in
the domain object and JSON schema.

## Canonical domain contract and ownership exception

The repository owner and Developer 1 agreed that a blocked developer may make a
minimal overlapping change. Under that exception, Developer 2 establishes one
shared canonical contract in
`io.github.cs32272610mp2xcode.finderskeepers.report.model`:

- `ItemReport` is an immutable Java record whose public constructor and record
  accessors are the construction and reconstruction interface.
- Report ID is a non-null `UUID`; equality and uniqueness use `UUID.equals`.
- Reporter ID is a required `String`; equality is exact and case-sensitive.
- `ReportType` contains `LOST` and `FOUND`.
- `ItemCategory` initially contains only `OTHER`.
- `ReportStatus` initially contains `SUBMITTED` and `UNDER_REVIEW`.
- Enum names are stable version 1 storage tokens. Constants may be added, but a
  persisted constant cannot be renamed or removed without a compatibility or
  migration decision.
- Location is a required `String` and occurrence date is a required
  `LocalDate`.
- Created At is a required `Instant` with millisecond precision.
- The record validates requiredness, supplied code-point limits, and Created At
  precision without normalizing values. Its `toString()` reveals no field
  values and returns exactly `ItemReport[redacted]`.
- The clock-dependent rule that an occurrence date is today or earlier belongs
  to Developer 1's later submission-validation workflow.

This is the project's canonical model, not a persistence-owned substitute.
Developer 1 must extend or jointly revise it instead of introducing competing
types.

## Repository behaviour

### Loading

- A missing report-store file is an empty repository.
- Loading a missing store returns an empty collection without creating a file
  or parent directory.
- `loadAll` returns every report exactly once in stable insertion order.
- Replacement does not change a report's position.
- The returned reports preserve every canonical value and Created At precision.

### Insertion

- Inserting into a missing store creates the required parent directory and the
  first versioned JSON document.
- A report whose ID is already present is rejected as a duplicate according to
  the canonical Report-ID equality rules.
- Duplicate insertion leaves the existing file byte-for-byte unchanged.
- Successful insertion appends the report to the stored order.

### Replacement

- The replacement contract must make changing the target Report ID
  unrepresentable or compare the replacement against a separately supplied
  original target ID.
- Replacing a target that does not exist is rejected without creating or
  changing a file. Replacement never renames or retargets a stored report.
- Report ID, Reporter ID, and Creation Time are immutable during replacement.
- When an immutable mismatch is representable at the repository boundary, it is
  rejected and leaves the existing file byte-for-byte unchanged.
- Lost or found type, item name, category, location, occurrence date, public
  description, private identifying detail, and status may change when the
  canonical domain model accepts the replacement.
- Persistence does not decide whether a status transition or other mutable
  field change is permitted by an application workflow.

## JSON and storage behaviour

- The store is app-owned, human-inspectable JSON rather than a supported
  hand-editing interface.
- The document has an explicit schema version. Version 1 accepts only the
  fields and JSON value types defined by the approved TDD.
- Input is decoded as strict UTF-8. Invalid UTF-8, invalid escapes, malformed
  JSON, duplicate members, unknown members, missing required members, wrong
  value types, duplicate report IDs, trailing content, and unsupported schema
  versions are rejected.
- Any invalid stored report makes the store corrupt; the repository does not
  silently skip or partially load records.
- A corrupt, unreadable, unsupported, or oversized store blocks loading,
  insertion, and replacement. Its bytes are not repaired, reset, or
  overwritten automatically.
- Whole-store reads and writes use a fixed 16 MiB byte limit, sized from the
  supplied field limits, worst-case JSON escaping, and an expected retained
  volume of 1,000 reports.
- Encoding completes and is validated before the destination file is changed.
- A mutation is successful only when the complete new document safely replaces
  the previous document. If the filesystem cannot provide the required safe
  replacement, the operation fails and preserves the previous store.
- One running application uses one shared repository instance. Its operations
  are serialized. Concurrent writers through other instances or processes are
  unsupported.

No version 0 or other legacy report store exists, so this mission does not
provide import or migration behaviour.

## Adversarial-input behaviour

Every report value is data, never JSON syntax or a filesystem path component.
The encoder must correctly escape and preserve canonical text containing:

- Quotation marks and backslashes
- Braces, brackets, commas, and colons
- Newlines, tabs, and other permitted control characters
- Unicode accepted by the canonical domain model
- Text that resembles JSON members, objects, arrays, or additional reports

For example, JSON-looking text inside a description must remain one description
value. It must not add a property, add a report, change a status, or alter any
other stored value.

Text that cannot be represented as valid UTF-8 and JSON, including an unpaired
UTF-16 surrogate, is rejected before mutation. The previous store remains
byte-for-byte unchanged.

Report data must never influence the repository's storage path. Structural
input attacks and excessive input are rejected within the documented resource
bound rather than being partially accepted.

## Privacy requirements

The private identifying detail is stored as plaintext in the local JSON file.
Encryption and platform-specific file access controls are outside this mission.

The repository returns complete canonical reports to trusted application
services. Later service and UI layers are responsible for deciding which users
may see private details.

No report field value may be emitted in persistence errors, logs, documentation
examples, screenshots, test output, or handoff summaries. Synthetic report
values may appear only in test source fixtures and isolated temporary store
files required to verify persistence; tests must not print them. Real student,
staff, or school data must never be used. Error messages identify the failure
category without echoing report contents or unsafe input.

## Error contract

Callers must be able to distinguish these failure categories without inspecting
message text or report contents:

- Duplicate report insertion
- Missing replacement target
- Immutable-field mismatch
- Corrupt or unsupported storage
- Storage I/O or safe-replacement failure
- Invalid, unencodable, or over-limit persisted data

The exact Java exception or result types are decided in the TDD. A failure must
not expose report values, return a partial success, or leave a partially written
JSON document.

## Architecture boundary

The design separates callers from the file format:

```text
Student or Desk Officer application service
        ↓
ReportRepository
        ↓
JSON repository and codec
        ↓
Caller-supplied local path
```

Role-specific UI and application services must not read or write the JSON file
directly. The persistence implementation must not:

- Define any report-domain type other than the approved shared canonical model
- Generate or change report identifiers or creation times
- Normalize report fields
- Enforce status transitions
- Filter private fields according to the current user
- Sort reports for a particular screen
- Derive a file or directory name from report contents

## Ownership and integration

This task primarily stays within Developer 2's persistence and storage-recovery
ownership. The minimal shared report model is an explicitly approved exception
to the Developer 1 ownership recorded in `AGENTS.md`.

The following require Developer 1 coordination or separate approval:

- Expanding or changing the minimal canonical report contract after this mission
- Renaming or removing stable report enum storage names
- Changing shared report-status behaviour
- Adding or changing a JSON dependency in `build.gradle`
- Wiring the repository into application startup or JavaFX screens
- Changing release or CI configuration

Before one of those changes, identify the affected file, explain why it is
required, propose the smallest sufficient change, and state whether Developer 1
can make it instead. Do not duplicate shared types or construct a second
application entry point to bypass integration.

## Required tests

Automated tests must use synthetic reports and isolated temporary directories.
They must cover:

- Loading a missing store as empty without creating a file or directory
- Inserting a report and loading it through a fresh repository instance
- Exact round-trip of all eleven report values
- Required location and occurrence-date values, including rejection of `null`
- Stable insertion order across a fresh repository instance
- Replacement through one instance and verification through a fresh instance
- Replacement retaining the target's stored position
- Allowed changes to every mutable report field
- Rejection of duplicate insertion
- Rejection of a missing replacement target
- Proof that replacement cannot retarget a Report ID, either through the
  canonical type/update contract or through runtime mismatch rejection
- Rejection of a missing target and of representable changes to Reporter ID or
  Creation Time
- Byte-for-byte preservation after every rejected mutation
- Strict handling of corrupt, unsupported, invalid-UTF-8, and over-limit stores
- Strict handling of duplicate, missing, unknown, and wrongly typed JSON members
- Rejection of unencodable text without changing an existing store
- Safe failure when all-or-nothing replacement is unavailable
- A fixed corpus of quotes, escapes, delimiters, control characters, and
  JSON-looking payloads in every canonical text field
- Deterministically generated valid Unicode strings in canonical text fields
- Exact adversarial-string round-trip with report count, status, identities,
  and neighbouring field values unchanged
- Safe failure categories whose diagnostics do not contain report contents
- Tests leaving real application and user storage untouched

Repository reconstruction in tests is the acceptance evidence for persistence
across restarts. Full application restart testing belongs to later startup and
workflow integration.

## Definition of done

This task is complete when:

- The implementation uses the approved shared canonical report contract without
  a duplicate domain model.
- Inserted and replaced reports survive repository reconstruction with every
  value preserved.
- Duplicate and invalid replacement operations preserve the previous store.
- Missing and invalid storage follow the safe behaviour in this brief.
- Adversarial field contents cannot alter the JSON structure or another report.
- Report values do not appear in diagnostics, logs, documentation examples,
  screenshots, test output, or handoff summaries. Only synthetic values in
  test source fixtures and isolated temporary stores are permitted.
- Every required automated test passes.
- `gradlew.bat check` or `./gradlew check` passes.
- The final diff remains within the approved ownership boundary.
- Storage format, limits, failure behaviour, and known limitations are
  documented.

## Planning and approval gates

Approval of this mission brief alone does not authorize production
implementation. Work proceeds through these gates in order:

1. The repository owner explicitly approves this mission brief and its approval
   metadata is recorded.
2. `docs/features/S1-D2-02/GrillingDecisions.md` records the completed decision
   ledger with no unresolved product decision.
3. `docs/features/S1-D2-02/PRD.md` is approved with its approver and approval
   date.
4. The repository owner approves the controlled cross-owner canonical report
   contract described above and records Developer 1 review as a later handoff.
5. `docs/features/S1-D2-02/TDD.md` and
   `docs/features/S1-D2-02/RequirementsToTests.md` are complete, record approved
   status and approval dates consistent with the other planning artifacts, and
   trace every approved requirement to automated or explicitly manual evidence.
6. The repository owner separately authorizes implementation.

Approval of this mission brief is not PRD or TDD approval and is not
implementation authorization.

All six gates were completed by the repository owner on 2026-09-20. Developer
1 review remains a later integration handoff and does not block the approved
shared-model exception.

## Handoff evidence

The final implementation handoff must report:

- The canonical report contract used
- The repository interface and JSON schema version
- The configured storage bound and safe-replacement strategy
- Files changed
- Automated checks performed and their results
- Evidence that tests used only temporary synthetic storage
- Security, privacy, concurrency, and recovery limitations
- Cross-owner integration changes requested from or approved by Developer 1
- Requirements that remain unverified
