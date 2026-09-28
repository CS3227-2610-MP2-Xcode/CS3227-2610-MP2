# S1-D2-02 Grilling Decisions

- Status: Complete
- Feature: JSON Report Persistence
- Mission: S1-D2-02
- Owner: Developer 2
- Confirmed by: Repository owner
- Initial confirmation date: 2026-09-19
- Revision confirmation date: 2026-09-20
- Final planning approval: Repository owner, 2026-09-20
- Implementation authorization: Repository owner, 2026-09-20
- Commit authorization: Repository owner, 2026-09-20
- Mission brief: `docs/mission-briefs/S1-D2-02-report-persistence.md`

## Purpose

This ledger records the product decisions settled before drafting the S1-D2-02 PRD. It is descriptive evidence of the completed grilling process, not implementation authorization.

## Canonical-domain integration update

Developer 1's canonical report domain is now available under `io.github.cs32272610mp2xcode.finderskeepers.report`. It supersedes the temporary shared-model exception recorded below without changing the repository operation set or the eleven-field version-one JSON representation. Persistence consumes the canonical types directly, writes enums with `storedName()`, reads them with `fromStoredName(...)`, and reconstructs stored reports with the clockless `ItemReport.restore(...)` factory.

## Confirmed decisions

### Scope and callers

- **GD-001 — Repository-only mission:** S1-D2-02 delivers the report repository, local JSON persistence, failure behaviour, tests, and storage documentation. Student submission, Desk Officer review, JavaFX, and startup wiring remain outside this mission.
- **GD-002 — Canonical domain dependency:** Persistence uses the project's one shared canonical `ItemReport` and must not introduce a shadow or temporary report-domain model.
- **GD-003 — Operation set:** The repository loads all reports, inserts one report, and replaces one complete existing report. Individual lookup and deletion are excluded.
- **GD-004 — Workflow policy boundary:** Status-transition and other application workflow rules belong to the canonical domain or calling services, not to persistence.

### Report state and mutation

- **GD-005 — Complete state:** Persistence round-trips Report ID, Reporter ID, lost/found type, item name, category, location, occurrence date, public description, private identifying detail, status, and creation time.
- **GD-006 — Required values:** All eleven fields are required. Persistence does not accept `null`, trim, normalize, default, or reinterpret any field.
- **GD-007 — Immutable replacement fields:** Report ID, Reporter ID, and Creation Time cannot change during replacement. All other canonical fields may change when the domain accepts the replacement.
- **GD-008 — Identity-preserving replacement:** The TDD must use a canonical identity-preserving update mechanism or receive the original target ID separately so replacement cannot retarget a stored report.
- **GD-009 — Target conflicts:** Duplicate insertion, missing replacement, and representable immutable-field mismatches fail without changing storage.
- **GD-010 — Stable order:** Loading preserves insertion order, insertion appends, and replacement retains the target's position.

### Storage lifecycle and recovery

- **GD-011 — Missing store:** A missing file loads as an empty repository without creating a file or directory. The first successful insertion creates storage.
- **GD-012 — Strict versioned JSON:** The store is strict, versioned UTF-8 JSON. Malformed input, invalid UTF-8, duplicate, unknown, missing required, or wrongly typed members, duplicate report IDs, trailing content, and unsupported versions are rejected.
- **GD-013 — Whole-store failure:** One invalid stored report invalidates the load. The repository never silently skips, repairs, resets, or partially loads records.
- **GD-014 — Preserve and block:** Corrupt, unreadable, unsupported, or oversized storage blocks reads and mutations and remains untouched for explicit external recovery.
- **GD-015 — Safe replacement:** A mutation reports success only after the whole new store safely replaces the old store. If safe replacement is unavailable, the mutation fails and preserves the previous store.
- **GD-016 — Bounded resources:** Whole-store operations use a 16 MiB byte bound, sized with headroom above the conservative approximately 8.5 MB worst case for 1,000 reports under the approved field limits and JSON escaping policy.
- **GD-017 — Writer model:** One running application uses one shared repository instance with serialized operations. Other repository instances and processes writing concurrently are unsupported.
- **GD-018 — App-owned file:** The JSON remains inspectable but is not a supported user or administrator editing interface. No pre-existing report format requires migration.

### Security, privacy, and failures

- **GD-019 — JSON-structure injection defense:** Every accepted textual value is encoded as data. JSON-significant characters, control characters, accepted Unicode, and JSON-looking fragments round-trip exactly without changing the document structure or another field.
- **GD-020 — Invalid text:** Text that cannot be represented as valid UTF-8 and JSON is rejected before mutation, leaving the previous bytes unchanged.
- **GD-021 — Adversarial evidence:** Verification uses a fixed attack corpus and deterministic generated valid-Unicode strings across canonical text fields.
- **GD-022 — Plaintext at rest:** Reports, including private identifying details, are stored in plaintext JSON. Encryption and operating-system ACL management are excluded.
- **GD-023 — Visibility boundary:** The repository returns complete reports to trusted application services. Later callers enforce which users may see private details.
- **GD-024 — No value disclosure:** Persistence errors, logs, documentation examples, screenshots, test output, and handoff summaries never emit report field values. Only synthetic values in test source fixtures and isolated temporary stores are permitted.
- **GD-025 — Distinguishable failures:** Callers can distinguish conflicts, corrupt or unsupported storage, and I/O or safe-replacement failures without parsing message text or receiving report contents.

### Acceptance and planning gates

- **GD-026 — Restart evidence:** Persistence across restarts is demonstrated by writing through one repository instance and loading through a fresh instance at the same temporary path. Full application restart belongs to later integration.
- **GD-027 — Controlled ownership overlap:** The repository owner originally approved a minimal shared model while Developer 1's implementation was unavailable. Developer 1's delivered canonical domain now supersedes that exception, and the temporary model must be removed rather than retained as a compatibility layer.
- **GD-028 — Separate authorization:** Mission, PRD, TDD, and requirements-to-tests approval do not authorize implementation, commits, pushing, merging, or publishing.
- **GD-029 — Enum storage contract:** Version 1 uses the canonical enums' `storedName()` values. The current categories are `STATIONERY`, `BOOKS`, `CLOTHING`, `BAGS`, `WATER_BOTTLES`, `ELECTRONICS`, `SPORTS_EQUIPMENT`, `PERSONAL_ITEMS`, and `OTHER`; persisted names require compatibility review before rename or removal.
- **GD-030 — Canonical Java shape:** `ItemReport` is an immutable final value object with a `UUID` Report ID, exact case-sensitive `String` Reporter ID, required text, required `LocalDate` occurrence date, and required millisecond-precision `Instant` Created At value. The clockless `ItemReport.restore(...)` factory is the persistence reconstruction seam.
- **GD-031 — Validation split:** The canonical value object enforces requiredness, supplied code-point limits, Created At precision, and privacy-safe `toString()` output. Developer 1's later submission workflow enforces the clock-dependent occurrence-date rule.
- **GD-032 — Storage naming:** The version 1 JSON member is `createdAt`; the old design placeholder `creationTime` is not part of the format.

## Inherited constraints

- Developer 1 owns `ItemReport`, `ReportType`, `ItemCategory`, `ReportStatus`, creation requests, submission validation, and Student submission. Developer 2 consumes those canonical types without wrappers or duplicates.
- Developer 2 owns `ReportRepository`, JSON persistence, storage failure, and recovery behaviour.
- Shared interfaces, status contracts, dependencies, startup, and end-to-end integration require cross-owner coordination.
- Tests use synthetic reports in isolated temporary directories and never touch real user storage.
- Adding a JSON dependency or changing Gradle, release, CI, JavaFX, or application startup requires separate approval.

## Intentional technical deferrals

These are assigned TDD decisions rather than unresolved product decisions:

| Decision | Owner/input | Acceptance obligation |
| --- | --- | --- |
| Exact Java repository signatures and identity-preserving replacement seam | Developer 2, using the approved shared contract | Fixed before TDD approval and shown to enforce GD-007 to GD-009 |
| JSON member names, enum encodings, date/time format, and schema layout | Developer 2, using the approved shared value semantics | Strict version 1 grammar documented before TDD approval |
| Concrete exception or result types | Developer 2 | Preserve the observable categories in GD-025 |
| Store byte limit | Resolved: Developer 2 and repository owner selected 16 MiB for 1,000 expected retained reports | Calculation documented and boundary-tested before implementation completion |
| JSON codec and filesystem abstraction strategy | Developer 2; dependency changes require the `AGENTS.md` cross-owner proposal and explicit authorization | Must satisfy the approved PRD without changing product behaviour |

No product decision is unassigned. The affected mission brief, PRD, TDD, and requirements-to-tests mapping return to owner approval before implementation.
