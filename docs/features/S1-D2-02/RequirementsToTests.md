# S1-D2-02 Requirements-to-Tests Mapping

- Status: Implemented - verification passing
- Draft date: 2026-09-19
- Revision date: 2026-09-20
- Previous conditional approver: Repository owner, 2026-09-19
- Final approver: Repository owner
- Final approval date: 2026-09-20
- Implementation authorization: Repository owner, 2026-09-20
- Feature: JSON Report Persistence
- Workstream: Developer 2
- Source PRD: `docs/features/S1-D2-02/PRD.md` (Approved 2026-09-20)
- Source TDD: `docs/features/S1-D2-02/TDD.md` (Approved 2026-09-20)

## Purpose

This checklist maps every revised PRD requirement, acceptance criterion,
scenario, and extension to stable behavior-level evidence. The repository owner
approved the mapping and separately authorized implementation on 2026-09-20.

A `Deferred` or `Pending` row is not complete. The repository owner confirmed
TS-01 through TS-05 on 2026-09-19 and approved the shared canonical model,
TS-06, the 16 MiB limit, the revised planning set, and separate implementation
authorization on 2026-09-20.

## Status vocabulary

- **Planned:** behavior and evidence are defined but have not yet been observed
  passing.
- **Passing:** the approved test or review has been implemented or performed and
  observed passing.
- **Deferred - owner:** required input owned outside the active workstream is
  unavailable. Deferred is not passing.
- **Confirmed:** the repository owner approved the planning seam, but blocked
  prerequisites may still prevent implementation.
- **Pending approval/authorization:** the owner has not yet granted final
  approval or separately authorized implementation.
- **External - owner:** the obligation is intentionally outside S1-D2-02 but is
  traced to the owning workstream.

## Test-seam index

The repository owner confirmed the persistence seams on 2026-09-19. TS-06 is
added for the approved shared canonical record and is part of this final approval
approval.

| Seam | Evidence boundary | Policy | Status |
| --- | --- | --- | --- |
| TS-01 | Public `ReportRepository` exercised through the real `JsonReportRepository` at a caller-supplied `@TempDir` path | Primary seam for load, insert, replace, reconstruction, conflicts, ordering, capacity, and public outcomes | Confirmed - repository owner, 2026-09-19 |
| TS-02 | Version 1 bytes at the configured path as an observable compatibility boundary | Seed independently authored literal UTF-8 fixtures and compare independent literal expected bytes; never derive expected JSON with the production codec | Confirmed - repository owner, 2026-09-19 |
| TS-03 | Package-private deterministic `ReportStoreFiles` fault adapter | Use only for access, staging/force-equivalent, atomic-move, and cleanup outcomes that cannot be induced portably; assert public reasons and target state, never interaction counts | Confirmed - repository owner, 2026-09-19 |
| TS-04 | Concurrent calls through one shared public repository instance | Coordinate calls with barriers/executors and assert only a serial-equivalent persisted result | Confirmed - repository owner, 2026-09-19 |
| TS-05 | Source, documentation, and build evidence | Structural review for canonical-domain reuse, bounded resources, privacy-safe tests, storage docs, and Gradle gates | Confirmed - repository owner, 2026-09-19 |
| TS-06 | Public `ItemReport` constructor, accessors, equality, and redacted `toString()` | Verify only the minimal shared canonical invariants needed by persistence; clock-dependent submission policy remains external | Confirmed - repository owner, 2026-09-20 |

Tests do not call codec internals and do not replace repository-owned logic with
mocks. Normal filesystem behavior always uses the real NIO adapter under an
isolated temporary directory.

## Privacy-safe test policy

- Fixtures contain only synthetic values and live only in test source or
  temporary test storage.
- Parameter display names use safe case IDs, never a parameter value.
- Assertions use fixed descriptions. They do not pass `ItemReport`, report
  fields, or JSON bytes as an assertion message or rely on an assertion that
  prints `toString()` or a byte diff on failure.
- Byte preservation uses `Arrays.equals` on the original byte arrays with a
  fixed message, rather than a digest or a value-bearing failure dump.
- Tests do not log or print report objects, JSON, field values, or generated
  Unicode values.
- No test reads or writes the application's real configured data location.

## Implementation evidence catalog

### Shared canonical report behavior

| ID | Stable behavior name | Seam | Evidence | Status |
| --- | --- | --- | --- | --- |
| RT-030 | `canonicalReportRequiresEveryFieldAndPreservesAcceptedValuesExactly` | TS-06 | Each field rejects `null`; bounded text rejects zero and maximum-plus-one code points, accepts one and maximum code points including supplementary scalars, and accessors return the exact unnormalized values | Passing - 2026-09-20 |
| RT-031 | `canonicalReportUsesApprovedIdentityEnumsAndMillisecondPrecision` | TS-06 | UUID and exact Reporter-ID equality, all approved enum constants, accepted millisecond instants, and rejected sub-millisecond instants are covered without a clock-dependent occurrence-date assertion | Passing - 2026-09-20 |
| RT-032 | `canonicalReportStringRepresentationDisclosesNoFieldValues` | TS-06 | A report containing distinct synthetic canaries produces exactly `ItemReport[redacted]` and contains none of them | Passing - 2026-09-20 |

### Public repository and reconstruction behavior

| ID | Stable behavior name | Seam | Evidence | Status |
| --- | --- | --- | --- | --- |
| RT-001 | `missingStoreLoadsEmptyWithoutCreatingFilesystemEntries` | TS-01 | Construct and load with missing parent/target; result is empty and both paths remain absent; a non-directory parent is separately a storage failure | Passing - 2026-09-20 |
| RT-002 | `insertedReportRoundTripsAllElevenValuesThroughFreshRepository` | TS-01 | First insert creates a store; a fresh instance restores field-by-field canonical equivalence without value-bearing assertions | Passing - 2026-09-20 |
| RT-003 | `requiredReportMembersRejectNullAndRoundTripWithoutNormalization` | TS-01, TS-02 | Every report member is tested missing, `null`, and wrong-typed; required values including location and occurrence date reconstruct exactly without normalization | Passing - 2026-09-20 |
| RT-004 | `insertionAndReplacementPreserveStableOrderAcrossReconstruction` | TS-01 | A multi-report fixture proves append order, middle replacement position, unaffected neighbors, and fresh-instance visibility | Passing - 2026-09-20 |
| RT-005 | `loadedReportsAreAnUnmodifiableStructuralSnapshot` | TS-01 | Caller cannot mutate the returned list; a later repository mutation does not alter the earlier list object | Passing - 2026-09-20 |
| RT-006 | `canonicalEnumAndTemporalRepresentationsRoundTripExactly` | TS-01, TS-02 | Every approved enum token, canonical `LocalDate` string, and exact three-digit UTC `createdAt` value reconstructs exactly; alternate offsets and one-, six-, or nine-digit precision are rejected | Passing - 2026-09-20 |
| RT-007 | `replacementPersistsEachRepresentableMutableFieldAndPreservesImmutableState` | TS-01 | Each of the seven currently representable mutable values changes alone and all change together; a fresh repository instance verifies the replacement, Report ID, Reporter ID, Created At, order, and unaffected reports; an identical replacement follows the same successful commit path; category remains the approved singleton `OTHER` until a compatibility-reviewed domain expansion | Passing - 2026-09-20 |
| RT-008 | `duplicateInsertReturnsDuplicateConflictAndPreservesBytes` | TS-01 | Canonically equal UUID is rejected with the exact typed reason and target bytes remain identical | Passing - 2026-09-20 |
| RT-009 | `missingReplacementReturnsMissingConflictWithoutCreatingOrChangingStorage` | TS-01 | Missing target is covered in a valid existing store and with a missing store; no file is created or changed | Passing - 2026-09-20 |
| RT-010 | `immutableMismatchOrRetargetAttemptFailsWithoutMutation` | TS-01 | Reporter-ID and Created-At mismatches plus explicit UUID target-ID mismatch are separate cases | Passing - 2026-09-20 |
| RT-011 | `nullArgumentsFailBeforeFilesystemAccess` | TS-01 | Null constructor path, insert report, replacement target, and replacement report fail as programmer errors without filesystem entries | Passing - 2026-09-20 |

### Strict JSON, invalid storage, and compatibility

| ID | Stable behavior name | Seam | Evidence | Status |
| --- | --- | --- | --- | --- |
| RT-012 | `invalidStoredDocumentBlocksEveryOperationAndPreservesExactBytes` | TS-01, TS-02 | Invalid-form by load/insert/replace matrix returns no partial result, uses the corrupt-store reason, and preserves exact target bytes | Passing - 2026-09-20 |
| RT-013 | `unsupportedSchemaVersionBlocksEveryOperationWithoutMigration` | TS-01, TS-02 | The exact integer token `1` is contrasted with lower/higher integers and alternate numeric spellings including decimal and exponent forms; every unsupported form returns `CORRUPT_OR_UNSUPPORTED_STORE` and preserves bytes | Passing - 2026-09-20 |
| RT-014 | `unreadableOrNonRegularStoreBlocksEveryOperationWithoutAlteration` | TS-01, TS-03 | A real directory/non-regular target and a portable injected access failure cover load/insert/replace; the path is not followed, truncated, or replaced | Passing - 2026-09-20 |
| RT-015 | `versionOneLiteralFixturesLoadAndSuccessfulWritesUseCanonicalBytes` | TS-01, TS-02 | Independently authored empty/populated fixtures cover permitted leading/trailing whitespace, alternate root and report-member order, solidus escape, uppercase escape hex, and a valid surrogate pair; successful output matches fixed order, escaping, required members, UTF-8, indentation, and final-newline policy | Passing - 2026-09-20 |
| RT-016 | `duplicateCanonicalIdsInvalidateTheWholeStore` | TS-01, TS-02 | Two syntactically valid report objects with the same canonical UUID cause whole-store failure and block both mutations | Passing - 2026-09-20 |

`RT-012` covers each invalid form individually, with safe case-number display
names:

- empty or truncated JSON;
- invalid UTF-8;
- malformed syntax, invalid escape, raw control character, or unpaired escaped
  surrogate;
- excessive or unexpected nesting beyond the fixed version 1 schema;
- duplicate decoded member names expressed through different escape spellings;
- trailing non-whitespace content, a UTF-8 BOM, or non-JSON whitespace;
- wrong root, collection, or scalar type;
- duplicate top-level or report-object member;
- unknown top-level or report-object member;
- missing required top-level or report-object member;
- wrong type or disallowed `null` for any member;
- invalid enum, date, time, identifier, or other canonical value; and
- a report rejected by the canonical reconstruction API.

Duplicate canonical IDs are isolated in `RT-016` so identity equality receives
direct evidence rather than being hidden inside the grammar matrix.

### Adversarial text and Unicode

| ID | Stable behavior name | Seam | Evidence | Status |
| --- | --- | --- | --- | --- |
| RT-017 | `fixedAttackCorpusRemainsOneValueInEveryCanonicalTextField` | TS-01 | Field-by-attack-class matrix covers JSON punctuation, delimiters, standard escaped controls, backslashes, and JSON/path-looking fragments accepted by the domain; fresh load restores the exact attacked value and preserves count, identities, status, neighbors, and configured path | Passing - 2026-09-20 |
| RT-018 | `deterministicallyGeneratedUnicodeRoundTripsInEveryCanonicalTextField` | TS-01 | A fixed-seed generator produces 256 valid Unicode-scalar cases within each canonical field limit, including BMP, combining, and supplementary characters; fresh loads restore exact values with report count, identity, status, and neighboring fields unchanged; generated values are never printed | Passing - 2026-09-20 |
| RT-019 | `unencodableTextFailsBeforeInsertOrReplacementMutation` | TS-01 | Unpaired high and low Java UTF-16 surrogates are separate insert/replace cases; first insert leaves target absent and replacement leaves existing bytes identical | Passing - 2026-09-20 |
| RT-020 | `adversarialReportDataCannotInfluenceTheConfiguredPath` | TS-01 | Every accepted path-looking text case changes only the configured target; no report-derived sibling or directory is created | Passing - 2026-09-20 |

The corpus and generator use the approved code-point limits. Invalid Unicode is
tested at the repository seam because Java strings can contain unpaired UTF-16
surrogates even though they cannot be emitted as valid JSON UTF-8.

### Capacity, filesystem transaction, and recovery

| ID | Stable behavior name | Seam | Evidence | Status |
| --- | --- | --- | --- | --- |
| RT-021 | `storedDocumentByteBoundaryIsEnforcedWithoutPartialLoadOrMutation` | TS-01, TS-02 | Valid literal stores below and exactly at the 16 MiB production read limit load; one byte above fails and blocks both mutations without changing bytes | Passing - 2026-09-20 |
| RT-022 | `encodedMutationByteBoundaryIsEnforcedBeforeFilesystemMutation` | TS-01 | The same valid candidate succeeds when the injected bound equals its independent byte length and fails when the bound is one byte less; public-policy cases cover valid below-bound and over-bound insertion/replacement while preserving existing bytes or target absence | Passing - 2026-09-20 |
| RT-023 | `filesystemMutationFaultsPreserveOldStoreOrAbsenceAndNeverReportSuccess` | TS-01, TS-03 | Deterministic cases cover parent creation, temporary creation, temporary write, force/close, unsupported atomic move, atomic move failure, and cleanup after a primary failure; the old target remains complete/readable or absent, while an empty newly created parent is an explicitly permitted first-insert residue | Passing - 2026-09-20 |
| RT-024 | `orphanTemporaryArtifactsNeverBecomeAuthoritativeStorage` | TS-01, TS-02 | Complete and partial orphan siblings beside a valid or missing target are neither loaded, promoted, nor automatically deleted | Passing - 2026-09-20 |
| RT-025 | `successfulMutationIsVisibleOnlyAsACompleteDocument` | TS-01, TS-03 | Controlled observations before and after commit see only the complete old or complete new target; a fresh repository reads the new document after success | Passing - 2026-09-20 |

### Failure privacy and supported concurrency

| ID | Stable behavior name | Seam | Evidence | Status |
| --- | --- | --- | --- | --- |
| RT-026 | `failureReasonsAreTypedAndDiagnosticsContainNoReportValues` | TS-01, TS-03 | All six public reasons are produced by representative cases; fixed message, cause/suppressed graph, and captured process output omit report values; structural review confirms the module has no logging path | Passing - 2026-09-20 |
| RT-027 | `overlappingSharedRepositoryOperationsAreEquivalentToSerialExecution` | TS-04 | Controlled distinct inserts, same-ID inserts, load/mutation, and insert/replacement overlaps produce one permitted serial outcome with no lost report, duplicate effect, or partial document | Passing - 2026-09-20 |
| RT-028 | `everyOperationRereadsTheAuthoritativeTarget` | TS-01, TS-02 | After one repository instance has loaded successfully, external test setup replaces the target first with corrupt bytes and then with valid recovery bytes; later load/insert/replace calls observe the current target rather than cached state | Passing - 2026-09-20 |
| RT-029 | `failurePrecedenceIsDeterministicAndPreservesStorage` | TS-01, TS-02 | Combination cases prove corrupt store before conflict, missing target before immutable/candidate failure, duplicate before candidate failure, and immutable mismatch before candidate encoding/capacity failure; every case preserves bytes or absence | Passing - 2026-09-20 |

### Review and command evidence

| ID | Stable evidence name | Seam | Evidence | Status |
| --- | --- | --- | --- | --- |
| RV-001 | `storageDocumentationStatesSchemaLimitRecoveryPrivacyAndConcurrency` | TS-05 | Review final storage format and operational documentation for version, 16 MiB bound, recovery, plaintext/orphan limitation, durability threshold, and unsupported writer model | Passed - 2026-09-20 |
| RV-002 | `repositoryUsesCanonicalDomainTypesAndDefinesNoShadowReportModel` | TS-05 | Inspect public signatures/imports and search production source for exactly one shared `ItemReport` definition and explicit UUID replacement target | Passed - 2026-09-20 |
| RV-003 | `persistenceTestsUseOnlySyntheticReportsTemporaryPathsAndPrivacySafeAssertions` | TS-05 | Review all fixture factories, parameter display names, assertion helpers, captured output, and target paths | Passed - 2026-09-20 |
| RV-004 | `resourceUseIsBoundedDuringReadDecodeAndEncode` | TS-05 | Review overflow-safe maximum-plus-one reads, schema-bounded parser depth/allocations, bounded encoder, the documented 1,000-report sizing calculation, and no unbounded whole-document fallback | Passed - 2026-09-20 |
| RV-005 | `changeSetStaysWithinApprovedOwnershipAndScope` | TS-05 | Diff review finds the approved minimal shared model, persistence, tests, documentation, and separately authorized delivery-skill alignment; no dependency, status-transition policy, workflow, UI, startup, release, or CI change | Passed - 2026-09-20 |
| RV-006 | `nioAdapterUsesTheApprovedSafeReplacementProtocol` | TS-05 | Inspect public-constructor delegation to production policy and the NIO adapter for final-target no-follow handling, a same-directory random temp, complete write, `force(true)`, close, one `ATOMIC_MOVE` attempt, no target pre-delete or non-atomic fallback, safe cleanup, and no report-derived path | Passed - 2026-09-20 |
| RV-007 | `documentationScreenshotsAndHandoffContainNoReportValues` | TS-05 | Review changed documentation, produced screenshots if any, command output retained for handoff, and the final handoff summary for report field values or private identifying detail | Passed - 2026-09-20; no screenshots produced |
| VG-001 | `focusedPersistenceTestsAndRepositoryCheckPass` | TS-05 | Run the focused persistence suite followed by `gradlew.bat check`; no required test is skipped or quarantined | Passing - 2026-09-20 |

## Observed implementation evidence

Evidence was collected on 2026-09-20:

- The focused domain and persistence suite passed with
  `gradlew.bat test --tests "io.github.cs32272610mp2xcode.finderskeepers.report.model.*" --tests "io.github.cs32272610mp2xcode.finderskeepers.report.persistence.*"`.
- The repository-wide `gradlew.bat check` gate passed, including compilation,
  JUnit, Checkstyle, Javadoc, and JaCoCo report generation.
- Production source contains one canonical `ItemReport` definition, and both
  public repository implementations import that shared type with an explicit
  UUID replacement target.
- Persistence tests use synthetic values with `@TempDir` paths or in-memory
  package-private filesystem fakes. Parameter names and assertions do not emit
  report values.
- Source review confirmed the maximum-plus-one read probe, schema-bounded
  parser, bounded encoder, same-directory random staging file, `force(true)`,
  one atomic replacement attempt, no target pre-delete, and no non-atomic
  fallback.
- Diff review from the branch base found only the approved report model,
  persistence, tests, planning/operational documentation, and separately
  authorized Dev 2 delivery-skill alignment. No dependency, workflow, UI,
  startup, release, or CI file changed.
- Documentation contains no complete report example or synthetic/real report
  content values beyond required schema names and allowed tokens, and no
  screenshots were produced.

## Equivalence partitions and boundary representatives

| Dimension | Representatives | Evidence |
| --- | --- | --- |
| Path state | Missing parent/target; valid regular v1 file; corrupt file; unsupported file; oversized file; directory/non-regular path; inaccessible path | RT-001, RT-002, RT-012 through RT-014, RT-021 |
| Report count | Zero, one, and three or more | RT-001, RT-002, RT-004, RT-015 |
| Identity state | New ID; canonically equal duplicate ID; existing target; missing target; explicit target-ID mismatch where representable | RT-002, RT-008 through RT-010, RT-016 |
| Replacement invariants | All immutables equal; Reporter-ID mismatch; Created-At mismatch; Report-ID retarget attempt | RT-007, RT-010, RV-002 |
| Required values | Present value; missing member; explicit `null`; empty bounded text; maximum and maximum-plus-one code points | RT-003, RT-012, RT-030 |
| Mutable fields | Each of seven currently representable mutable fields changed independently; all changed together; none changed; category is the approved singleton `OTHER` | RT-007 |
| Enum and temporal values | Every enum constant; approved date/time boundary values; precision, offset, and timezone representatives | RT-006, RT-015 |
| JSON grammar | Valid canonical; valid alternate member order; lexical/syntax failure; schema failure; canonical reconstruction failure | RT-012, RT-015 |
| Schema version | Integer 1; lower and higher integers; wrong type/non-integer treated as corrupt | RT-012, RT-013, RT-015 |
| Text | Ordinary; fixed structural/path-looking attacks; permitted controls; deterministic valid Unicode scalars; unpaired high/low surrogate | RT-017 through RT-020 |
| Stored byte size | Below, exactly at, and one byte above the configured production limit | RT-021 |
| Encoded next-state size | Exact equality and one byte over at an injected deterministic bound; below and over at the production policy for first insertion and replacement | RT-022 |
| Transaction stage | Before filesystem action; parent/temp creation; partial write; force/close; atomic-move capability; move; committed target; cleanup | RT-023, RT-025 |
| Recovery state | Authoritative target absent/valid plus complete/partial orphan sibling | RT-024 |
| Concurrency | Distinct inserts; same-ID inserts; read/mutation; insert/replacement | RT-027 |
| Failure family | Duplicate; missing; immutable; corrupt/unsupported; access/safe replacement; unencodable/over-limit | RT-026 |
| Cached versus authoritative state | Previously loaded state; externally corrupted target; externally recovered valid target | RT-028 |
| Failure precedence | Invalid store plus conflict; missing plus mismatch; conflict/mismatch plus invalid or oversized candidate | RT-029 |

## Scenario and extension mapping

| Scenario or extension | Evidence | Status |
| --- | --- | --- |
| SC-001 main path | RT-001, RT-002 | Passing - 2026-09-20 |
| SC-001 parent-directory or store creation failure | RT-023 | Passing - 2026-09-20 |
| SC-001 encoding failure | RT-019 | Passing - 2026-09-20 |
| SC-001 capacity failure | RT-022 | Passing - 2026-09-20 |
| SC-002 main replacement path | RT-004, RT-007 | Passing - 2026-09-20 |
| SC-002 missing target | RT-009 | Passing - 2026-09-20 |
| SC-002 immutable mismatch | RT-010 | Passing - 2026-09-20 |
| SC-002 application-invalid status transition | XW-001 | External - Developer 1/application workflow |
| SC-003 invalid store | RT-012 through RT-014, RT-016, RT-021, RT-028 | Passing - 2026-09-20 |
| SC-004 adversarial text | RT-017 through RT-020 | Passing - 2026-09-20 |
| SC-005 safe replacement unavailable | RT-023 through RT-025 | Passing - 2026-09-20 |

## Functional and non-functional requirement mapping

| Requirement | Evidence | Coverage intent |
| --- | --- | --- |
| FR-001 | RT-002, RT-003, RT-006, RT-007, RT-030, RT-031 | All eleven required values and exact temporal/text semantics |
| FR-002 | RT-001 | Side-effect-free missing read |
| FR-003 | RT-002, RT-004, RT-015, RT-028 | Complete, once-only, ordered and authoritative fresh/current-instance load |
| FR-004 | RT-002, RT-004, RT-008, RT-022, RT-023 | First creation, append, duplicate preservation, bounded and truthful insertion |
| FR-005 | RT-004, RT-007, RT-009, RT-010 | Complete replacement, order, mutable fields, target/immutable conflicts |
| FR-006 | RT-012 through RT-014, RT-016, RT-021, RT-028, RT-029 | Whole-store rejection and preservation for invalid/inaccessible storage |
| FR-007 | RT-008 through RT-014, RT-019, RT-021 through RT-025, RT-028, RT-029, RV-006 | No false or partial mutation success and verified production replacement protocol |
| FR-008 | RT-012, RT-013, RT-015, RT-016 | Strict v1 UTF-8 JSON and duplicate-ID rejection |
| FR-009 | RT-017 through RT-020 | Structural-injection resistance, exact Unicode, invalid-Unicode rejection, path isolation |
| FR-010 | RT-008 through RT-014, RT-019, RT-021 through RT-023, RT-026, RT-029 | Typed, privacy-safe caller failures with deterministic precedence |
| FR-011 | RT-026, RV-001, RV-003, RV-007 | Plaintext boundary and non-disclosure across tests, docs, screenshots, and handoff |
| NFR-001 | RT-021, RT-022, RV-001, RV-004 | Fixed store bound and boundary behavior |
| NFR-002 | RT-027, RV-001 | Serialized supported instance and documented unsupported writers |
| NFR-003 | RV-003 | Synthetic, isolated tests with no real storage |
| NFR-004 | RT-030 through RT-032, RV-002, RV-005 | Single canonical contract, minimal validation, privacy-safe representation, absence of workflow policy, and ownership discipline |
| NFR-005 | RT-013, RT-015, RV-001 | Version 1 boundary and no implicit migration |
| NFR-006 | VG-001 | Focused and full verification gates |

## Acceptance-criterion mapping

| Criterion | Evidence | Status |
| --- | --- | --- |
| AC-001 | RT-001 | Passing - 2026-09-20 |
| AC-002 | RT-002, RT-004, RT-007 | Passing - 2026-09-20 |
| AC-003 | RT-003, RT-030 | Passing - 2026-09-20 |
| AC-004 | RT-004 | Passing - 2026-09-20 |
| AC-005 | RT-007 through RT-010, RV-002 | Passing - 2026-09-20 |
| AC-006 | RT-012 through RT-014, RT-016, RT-021, RT-028, RT-029 | Passing - 2026-09-20 |
| AC-007 | RT-017, RT-018 | Passing - 2026-09-20 |
| AC-008 | RT-026, RT-032, RV-007 | Passing - 2026-09-20 |
| AC-009 | RT-023 through RT-025, RV-006 | Passing - 2026-09-20 |
| AC-010 | RV-003 | Passing - 2026-09-20 |
| AC-011 | RV-001 | Passing - 2026-09-20 |
| AC-012 | VG-001 | Passing - 2026-09-20 |
| AC-013 | RT-027 | Passing - 2026-09-20 |
| AC-014 | RT-030 through RT-032, RV-002, RV-005 | Passing - 2026-09-20 |
| AC-015 | RT-019, RT-026 | Passing - 2026-09-20 |
| AC-016 | RT-021, RT-022 | Passing - 2026-09-20 |

## Deferred blockers and external obligations

| ID | Status | Owner | Required resolution | Effect |
| --- | --- | --- | --- | --- |
| D-001 | Resolved 2026-09-20 | Repository owner under shared-integration exception | Canonical package/API, requiredness, types, identity, enums, temporal representations, validation split, immutability, and field limits | Exact contract recorded in the TDD; Developer 1 review remains a non-blocking later handoff |
| D-002 | Resolved 2026-09-20 | Developer 2 and repository owner | Bound resource use for 1,000 expected reports | `MAX_STORE_BYTES = 16,777,216`; calculation and boundary evidence recorded |
| D-003 | Resolved 2026-09-20 | Repository owner | Confirm TS-01 through TS-06 and the repository/error seam | TS-01 through TS-05 were confirmed on 2026-09-19; TS-06 and the complete revised seam were approved on 2026-09-20 |
| D-004 | Resolved 2026-09-20 | Repository owner | Authorize test and production implementation after planning approval | Test and production implementation were separately authorized on 2026-09-20 |
| XW-001 | External - Developer 1/application workflow | Owning domain/workflow | Reject application-invalid status transitions before calling persistence | Explicit PRD non-goal; repository must not duplicate this policy |

## Mechanical traceability audit

Before TDD approval and again before implementation completion:

1. Extract every `FR-*` and `NFR-*` identifier from the approved PRD and compare
   it with the requirement table.
2. Extract every `AC-*` identifier and compare it with the acceptance table.
3. Extract every `SC-*` main path and extension and compare it with the scenario
   table.
4. Extract every `RT-*`, `RV-*`, `VG-*`, and `XW-*` reference from this document
   and verify that exactly one catalog or obligation row defines it.
5. Treat any missing identifier, undefined evidence ID, `Deferred` row, skipped
   test, or failed review as incomplete.

The completion audit on 2026-09-20 found all 17 PRD requirement IDs, all 16
acceptance criteria, and all five scenarios represented. It found 41 evidence
definitions (32 RT, seven RV, one VG, and one XW), with no undefined reference
or duplicate definition.

## Completion gate

S1-D2-02 cannot be declared complete until:

1. D-001 and D-002 remain recorded as resolved, TS-06 and the complete revised
   planning set receive final owner approval, and D-004 implementation
   authorization has been given.
2. Every `RT-*` row is `Passing`; none is skipped, disabled, quarantined, or
   silently weakened.
3. RV-001 through RV-007 are recorded as passed reviews.
4. Every PRD functional requirement, non-functional requirement, acceptance
   criterion, scenario, and extension maps to defined evidence with no undefined
   IDs.
5. All conflict, invalid-input, capacity, and fault tests verify exact prior
   bytes or target absence without printing those bytes.
6. The focused persistence suite passes, followed by `gradlew.bat check`.
7. Tests use only isolated temporary directories and synthetic canonical values,
   and their display/assertion/output paths cannot emit report contents.
8. Storage documentation records the final v1 grammar, byte limit, recovery
   behavior, atomicity/durability threshold, plaintext and orphan-temp
   limitations, and unsupported multi-instance/process concurrency.
9. Structural review confirms exactly one approved shared `ItemReport`, no shadow
   model, the approved NIO replacement sequence, no persistence-owned transition
   policy, no unapproved dependency/configuration change, and no scope expansion.
10. The implementation handoff reports exact command evidence and unresolved
    external integration accurately, and its retained output and summary contain
    no report field values.

XW-001 remains traced but must not be misrepresented as repository behavior. It
does not block S1-D2-02 completion; it remains the responsibility of the owning
domain or later application workflow, while structural review verifies that the
repository does not enforce transition policy.

Completion result: satisfied on 2026-09-20 for the approved repository-only
scope. Application wiring and XW-001 remain intentionally outside S1-D2-02.
