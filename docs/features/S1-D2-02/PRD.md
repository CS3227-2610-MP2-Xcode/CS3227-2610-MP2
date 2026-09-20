# S1-D2-02 JSON Report Persistence PRD

- Status: Approved
- Draft date: 2026-09-19
- Revision date: 2026-09-20
- Previous approval: Repository owner, 2026-09-19
- Reapprover: Repository owner
- Reapproval date: 2026-09-20
- Implementation authorization: Repository owner, 2026-09-20
- Feature: JSON Report Persistence
- Workstream: Developer 2
- Approved mission brief: `docs/mission-briefs/S1-D2-02-report-persistence.md`
- Decision ledger: `docs/features/S1-D2-02/GrillingDecisions.md`

## Authority and precedence

This PRD is governed, in descending order of authority, by:

1. `AGENTS.md`
2. The approved S1-D2-02 mission brief
3. The completed S1-D2-02 grilling decision ledger
4. This PRD

If these sources conflict, work stops until the higher-authority requirement is
preserved or the owning party explicitly approves a correction. Approval of the
PRD alone does not authorize test or production implementation.

## Purpose

Provide dependable local persistence for every canonical lost-or-found report so
later Student and Desk Officer workflows can close and reopen the application
without losing submitted or updated report state.

The feature exposes storage behaviour to application services, not directly to a
human-facing UI. It must fail safely and truthfully when the local store cannot be
read or changed.

## Actors and audience

- **Student application service:** will insert canonical reports created by the
  separately owned Student submission workflow.
- **Desk Officer application service:** will load and replace canonical reports
  through separately planned review workflows.
- **Developer 1:** owns and extends the canonical report types, Student
  submission, and clock-dependent occurrence-date validation.
- **Developer 2:** consumes the canonical report contract and owns the
  repository contract, JSON persistence, storage failure behaviour, and storage
  documentation.

## Success criteria

The feature succeeds when:

1. Every canonical report value survives repository reconstruction.
2. Insert and replacement preserve stable report ordering and reject conflicts
   without changing existing data.
3. Missing storage behaves as an empty repository, while invalid storage is
   preserved and blocks unsafe operations.
4. Successful mutations never expose a partial JSON document.
5. Accepted adversarial text cannot alter JSON structure, another field, or
   another report.
6. Failures are distinguishable without exposing report contents.
7. Verification uses only synthetic reports and isolated temporary storage.

## Dependencies

### Planning dependency

The approved revised S1-D2-02 mission brief and completed decision ledger define
this PRD's scope and product decisions. The repository owner reapproved them
together with this PRD on 2026-09-20.

### Canonical report-domain dependency

Developer 1's canonical domain is available under
`io.github.cs32272610mp2xcode.finderskeepers.report`. The earlier controlled
shared-model exception is superseded. Persistence imports the canonical
`ItemReport` and enums directly, reconstructs stored records through the
clockless `ItemReport.restore(...)`, and must not retain a wrapper, duplicate,
or compatibility model.

### Operational assumptions

- No legacy report store exists and no migration is required.
- One application process uses one shared report repository.
- The repository storage location is supplied by application composition rather
  than derived from report data.
- The local filesystem is expected to support the safe replacement guarantee in
  this PRD; an unsupported filesystem produces a truthful failure.

## Glossary

- **Canonical report:** The project's single approved shared `ItemReport` value.
- **Report store:** The versioned local JSON document containing all reports.
- **Repository reconstruction:** Creating a fresh repository instance at the same
  storage path, representing persistence across application restarts.
- **Stable insertion order:** Reports load in insertion order; replacement does
  not move a report.
- **Conflict:** A duplicate insert, missing replacement target, or representable
  immutable-field mismatch.
- **Safe replacement:** A mutation outcome in which callers observe either the
  complete previous store or the complete new store, never a partial document.
- **Corrupt storage:** A document that violates the accepted encoding, structure,
  schema, identity, or canonical report requirements.

## Included scope

- Consuming the canonical `ItemReport`, `ReportType`, `ItemCategory`, and
  `ReportStatus` supplied by Developer 1
- Preserving canonical requiredness, code-point limits, Created At precision,
  identity, and privacy-safe report representation during reconstruction
- Loading all canonical reports
- Inserting one canonical report
- Replacing one complete existing canonical report
- Stable insertion ordering
- Versioned local JSON storage
- Missing, invalid, unsupported, oversized, and inaccessible-store behaviour
- Conflict detection and safe mutation outcomes
- JSON-structure injection defense
- Privacy-safe diagnostics
- Automated persistence and recovery verification
- Storage format, limits, and limitations documentation

## Excluded scope

- Changing the canonical category or status sets owned by Developer 1
- Clock-dependent occurrence-date validation and other submission validation
- Report creation, ID generation, or creation-time generation
- Student or Desk Officer UI and application workflows
- Status-transition policy
- Presentation sorting, filtering, or individual lookup
- Report deletion
- Startup or JavaFX wiring
- Manual JSON editing support
- Encryption or filesystem ACL management
- Automatic repair, backup restoration, journaling, or migration
- Concurrent writers through multiple repository instances or processes
- Dependency, Gradle, release, or CI changes without the applicable
  `AGENTS.md` cross-owner proposal and explicit authorization

## Functional requirements

### FR-001 — Preserve complete canonical report state

The repository must store and restore these values for every report:

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
- Created At

All eleven values are required. Persistence must reject absent values and must
not trim, normalize, default, or otherwise reinterpret a canonical value.

The shared canonical report accepts `UUID` Report IDs, exact case-sensitive
Reporter IDs, `LOST` or `FOUND`, every canonical `ItemCategory` stored name,
`SUBMITTED` or `UNDER_REVIEW`, a `LocalDate`, and a millisecond-precision
`Instant`. Text uses inclusive Unicode code-point bounds: Reporter ID 1–128,
item name 1–100, location 1–120, public description 1–500, and private
identifying detail 1–500. Clockless restoration does not enforce the
today-or-earlier occurrence-date rule; the Student submission workflow owns
that rule.

### FR-002 — Load missing storage as empty

When the configured report store and its parent directory do not exist, loading
must succeed with an empty ordered collection. Loading alone must not create the
directory or file.

### FR-003 — Load stored reports deterministically

Loading a valid store must return every stored report exactly once in stable
insertion order. Loading through a fresh repository instance at the same path
must return values equivalent to those successfully persisted earlier.

### FR-004 — Insert without overwriting

A successful insertion must:

1. Create the parent directory and initial store when they are absent.
2. Preserve all existing reports and their order.
3. Append the new report to the stored order.
4. Be visible to a fresh repository instance after success is reported.

An insertion whose Report ID already exists under the canonical identity rules
must return a duplicate conflict and leave the previous store byte-for-byte
unchanged.

### FR-005 — Replace one complete existing report

Replacement must update one existing target without moving it in the stored
order. The replacement contract must either make Report-ID retargeting
unrepresentable or compare the replacement with a separately supplied original
target identity.

Report ID, Reporter ID, and Created At are immutable. A missing target or any
representable immutable-field mismatch must return the corresponding conflict
and leave the store byte-for-byte unchanged.

Lost or found type, item name, category, location, occurrence date, public
description, private identifying detail, and status may change when accepted by
the canonical domain. Persistence must not enforce application status-transition
or field-editing policy.

### FR-006 — Reject and preserve invalid or inaccessible storage

Malformed, invalidly encoded, structurally invalid, unsupported, internally
duplicated, non-canonical, or over-limit storage must fail as a whole. The
repository must not:

- Return a partial list
- Skip an invalid report
- Reset the store to empty
- Repair or rewrite the store automatically
- Permit insertion or replacement over the invalid store

The original bytes remain available for explicit recovery outside this feature.

When the configured path exists but is not a readable regular file, loading,
insertion, and replacement must all report a storage-access failure. The
repository must not create, replace, truncate, or otherwise alter that path.

### FR-007 — Report truthful mutation outcomes

A mutation may report success only when the complete new store has safely
replaced the previous store. Encoding, validation, capacity, or replacement
failure must return a failure and preserve the complete previous store.

The feature must never report success while leaving a partial document or while
only some requested state is durable.

### FR-008 — Enforce the supported JSON contract

The report store must be strict, versioned UTF-8 JSON. The repository must reject:

- Invalid UTF-8 or JSON escapes
- Malformed JSON or trailing content
- Duplicate, unknown, missing required, or wrongly typed members
- Duplicate canonical Report IDs
- Unsupported schema versions
- Values that cannot reconstruct a canonical report

The JSON remains inspectable, but manual editing is unsupported. The exact member
names, value encodings, and version 1 grammar are TDD decisions constrained by
these observable outcomes.

### FR-009 — Preserve accepted adversarial text as data

Every canonical text value accepted by the domain must remain data rather than
JSON syntax or a filesystem path component. JSON-significant punctuation,
permitted control characters, accepted Unicode, and text resembling JSON
structure must round-trip exactly without:

- Adding, removing, or changing a report
- Adding or changing a JSON member
- Changing status or immutable identity
- Changing a neighbouring field
- Influencing the configured storage path

Text that cannot be represented as valid UTF-8 and JSON must fail before the
store changes.

### FR-010 — Return privacy-safe failure categories

Callers must be able to distinguish, without parsing message text:

- Duplicate insertion
- Missing replacement target
- Representable immutable-field mismatch
- Corrupt or unsupported storage
- Storage I/O or safe-replacement failure
- Invalid, unencodable, or over-limit persisted data

A failure must not return partial success or include any report field value in
its message or diagnostic information.

### FR-011 — Protect report values outside storage

The report store contains plaintext canonical values, including private
identifying details. The repository returns complete reports to trusted
application services; later services and UIs own visibility decisions.

Persistence errors, logs, documentation examples, screenshots, test output, and
handoff summaries must not emit report field values. Only synthetic values in
test source fixtures and isolated temporary report stores are permitted.

## Non-functional requirements

### NFR-001 — Bounded resource use

Whole-store reads and writes use a fixed inclusive limit of 16 MiB
(`16,777,216` bytes). It is sized with headroom above the conservative
approximately 8.5 MB worst-case document for 1,000 reports under the approved
field limits, JSON escaping, fixed scalar encodings, and framing. Observable
boundary behaviour must match FR-006, FR-007, and FR-010. Removing or changing
the bound or its failure behaviour requires renewed PRD approval.

### NFR-002 — Supported writer model

Operations through the one shared repository in one application process must be
serialized. Correctness under concurrent writers using other repository instances
or processes is not promised and must be documented as unsupported.

### NFR-003 — Test isolation

Every automated persistence test must use synthetic reports and an isolated
temporary location. Tests must not read or modify real user storage and must not
print report values.

### NFR-004 — Ownership and maintainability

The persistence feature and later workflows share Developer 1's one canonical
report contract. No second report model may be created. Persistence uses
`storedName()` and `fromStoredName(...)` so enum storage names remain owned in
one place; requiredness, identity, or temporal semantics cannot change silently.

### NFR-005 — Compatibility boundary

Version 1 is the first report-store format. Unsupported versions fail safely;
forward or backward migration is not part of this feature. Any future schema
change requires an explicit compatibility or migration decision.

### NFR-006 — Verification gate

Implementation completion requires the focused automated evidence derived from
this PRD and a passing repository `check` task. Failed, skipped, or unavailable
checks must be reported accurately and prevent an unqualified completion claim.

## Main scenarios and extensions

### SC-001 — First report survives reconstruction

1. The report path does not exist.
2. Loading returns an empty collection without creating storage.
3. A canonical report is inserted successfully.
4. A fresh repository instance loads the report with every value preserved.

Extensions:

- If parent-directory or store creation fails, insertion reports an I/O failure
  and no successful persistence is claimed.
- If encoding or capacity validation fails, insertion reports the appropriate
  failure and leaves no partial store.

### SC-002 — Updated report survives reconstruction

1. A valid store contains two or more reports.
2. One target is replaced with allowed changed values.
3. A fresh repository instance loads the replacement in the original position.
4. The target's three immutable values and every unaffected report remain
   unchanged.

Extensions:

- A missing target returns a conflict and leaves the file unchanged.
- A representable immutable-field mismatch returns a conflict and leaves the
  file unchanged.
- An application-invalid status transition is rejected before persistence by the
  owning domain or caller.

### SC-003 — Invalid store blocks unsafe work

1. The configured path contains a store that violates the supported contract.
2. Loading reports a corrupt or unsupported-storage failure and returns no
   reports.
3. Insert and replacement also fail.
4. The original bytes remain unchanged for explicit external recovery.

### SC-004 — Adversarial text remains one value

1. A canonical report contains accepted text with JSON-significant characters,
   control characters, Unicode, or JSON-looking fragments.
2. Insertion or replacement succeeds.
3. A fresh repository instance restores the exact text.
4. Report count, identities, statuses, neighbouring fields, and storage path are
   unchanged except for the requested operation.

### SC-005 — Safe replacement is unavailable

1. A valid store exists.
2. A mutation reaches a filesystem that cannot provide the required safe
   replacement.
3. The mutation reports a storage failure.
4. The previous store remains complete and readable.

## Acceptance criteria

- **AC-001:** A missing store loads as empty without a filesystem side effect.
- **AC-002:** Inserted and replaced reports round-trip all eleven values through a
  fresh repository instance.
- **AC-003:** Every field is required; missing or `null` report members are
  rejected, while accepted text is preserved without normalization.
- **AC-004:** Insertion order is deterministic and replacement retains position.
- **AC-005:** Duplicate insertion, missing replacement, and every representable
  immutable mismatch preserve the prior store byte-for-byte. Replacement either
  makes Report-ID retargeting unrepresentable or rejects an explicit target-ID
  mismatch without mutation, permits changes to every mutable field, and
  separately rejects Reporter-ID and Created-At changes without mutation.
- **AC-006:** Each enumerated invalid or unsupported form causes loading to fail
  without a partial or empty substitute, blocks insertion and replacement, and
  preserves the original bytes. An unreadable or non-regular configured path
  likewise blocks all three operations and is not altered.
- **AC-007:** Accepted adversarial strings pass both a fixed attack corpus and
  deterministic generated-Unicode verification with exact round-trip and
  unchanged structure.
- **AC-008:** Failure categories are distinguishable without report values in
  diagnostics or output.
- **AC-009:** Safe-replacement failure preserves the complete previous store.
- **AC-010:** All persistence verification uses temporary synthetic storage and
  leaves real application data untouched.
- **AC-011:** The repository documentation states the schema version, storage
  bound, recovery behaviour, plaintext-at-rest limitation, and unsupported
  concurrency model.
- **AC-012:** Focused automated checks and `gradlew.bat check` or `./gradlew check`
  pass before implementation is declared complete.
- **AC-013:** Overlapping operations through the one supported shared repository
  produce a result equivalent to some serial execution, with no lost report,
  duplicate effect, or partial document.
- **AC-014:** Structural review confirms that the public repository seam and
  persistence implementation use the approved shared canonical report types
  directly and define no duplicate report-domain type.
- **AC-015:** Insertion and replacement of a canonical object containing text
  that cannot be encoded as valid UTF-8 and JSON fail before mutation, return the
  appropriate privacy-safe category, and preserve an existing store
  byte-for-byte.
- **AC-016:** Once the store bound is configured, mutation results at or below the
  supported boundary persist successfully, while an insertion or replacement
  whose encoded result exceeds it fails before mutation. An existing store
  remains byte-for-byte unchanged, and an over-limit first insertion leaves the
  store absent.

## Requirement index

| Requirement | Outcome | Acceptance evidence |
| --- | --- | --- |
| FR-001 | Complete required canonical state is preserved without normalization | AC-002, AC-003 |
| FR-002 | Missing storage is an empty, side-effect-free read | AC-001 |
| FR-003 | Valid reports load once in stable order after reconstruction | AC-002, AC-004 |
| FR-004 | Insert appends without overwriting duplicates | AC-002, AC-004, AC-005 |
| FR-005 | Replacement preserves identity, immutables, and position | AC-002, AC-004, AC-005 |
| FR-006 | Invalid or inaccessible storage is preserved and blocks unsafe work | AC-006 |
| FR-007 | Mutation outcomes are all-or-nothing and truthful | AC-005, AC-006, AC-009, AC-015, AC-016 |
| FR-008 | Only supported strict, versioned UTF-8 JSON is accepted | AC-006, AC-011 |
| FR-009 | Accepted adversarial text remains data and unencodable text is rejected | AC-007, AC-015 |
| FR-010 | Failures are distinguishable and privacy-safe | AC-008, AC-015 |
| FR-011 | Report values remain confined to trusted storage and services | AC-008, AC-010 |
| NFR-001 | Whole-store resource use is bounded | AC-006, AC-011, AC-016 |
| NFR-002 | The supported writer model is explicit and serialized | AC-011, AC-013 |
| NFR-003 | Tests cannot modify real storage | AC-010 |
| NFR-004 | Persistence reuses the canonical report contract | AC-014 |
| NFR-005 | Unsupported versions fail without implicit migration | AC-006, AC-011 |
| NFR-006 | Required verification passes before completion | AC-012 |

## Intentional TDD decisions

The product outcome constrains, but does not prescribe, these later
technical decisions:

- Exact repository method signatures and identity-preserving replacement seam
- Concrete error/result types
- Version 1 JSON member names and value encodings
- Encoding of enums, dates, and Created At values
- Codec/library strategy and any deterministic filesystem-failure seam
- Safe-replacement write sequence and internal temporary-file handling
- Package structure, composition, and test class organization

Every item must be resolved before TDD approval. Any resolution that changes an
observable requirement in this PRD returns to PRD review and approval first.

## Approval and next gate

The repository owner approved the original PRD on 2026-09-19 and reapproved the
complete persistence design on 2026-09-20, including required-field semantics
and the 16 MiB bound. Developer 1's subsequently delivered canonical domain
supersedes the temporary cross-owner model exception while leaving the approved
repository behavior and eleven-field storage shape unchanged.
