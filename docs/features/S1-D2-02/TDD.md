# S1-D2-02 JSON Report Persistence Technical Design

- Status: Approved
- Draft date: 2026-09-19
- Revision date: 2026-09-20
- Previous conditional approver: Repository owner, 2026-09-19
- Final approver: Repository owner
- Final approval date: 2026-09-20
- Implementation authorization: Repository owner, 2026-09-20
- Feature: JSON Report Persistence
- Workstream: Developer 2
- Source PRD: `docs/features/S1-D2-02/PRD.md` (Approved 2026-09-20)
- Mission brief: `docs/mission-briefs/S1-D2-02-report-persistence.md`
- Test mapping: `docs/features/S1-D2-02/RequirementsToTests.md`

## Approval record

The repository owner conditionally approved the original design and TS-01 through TS-05 on 2026-09-19. On 2026-09-20 the owner selected the temporary cross-owner report contract and numeric storage limit, approved the revised mission brief, PRD, this TDD, and the test mapping, confirmed TS-06, and separately authorized test and production implementation. Developer 1's subsequently delivered canonical package supersedes the temporary model while preserving the approved persistence behavior.

## Authority and precedence

This design is governed, in descending order, by:

1. `AGENTS.md`
2. The approved S1-D2-02 mission brief
3. The completed S1-D2-02 grilling decision ledger
4. The approved S1-D2-02 PRD
5. This approved TDD

If a lower source conflicts with a higher source, work stops until the higher requirement is preserved or its owner approves a correction.

## Design goals

The persistence module must give Student and Desk Officer application services three small operations while hiding strict JSON processing, whole-store validation, ordering, conflict checks, bounded resource use, filesystem transactions, and privacy-safe failure translation.

The design optimizes for:

- exact reconstruction of the shared canonical report values;
- old-or-new observable file state for every mutation;
- a strict first-version format with no implicit repair or migration;
- stable insertion order and identity-preserving replacement;
- deterministic failure and adversarial-input verification; and
- locality: storage mechanics remain inside the persistence package.

## Exclusions

This design does not add or modify report-domain behaviour, clock-dependent submission validation, workflow or status-transition policy, report creation, identifier or time generation, lookup, deletion, filtering, presentation sorting, UI, startup wiring, encryption, ACL management, backups, migration, journaling, multi-process coordination, a JSON dependency, or release/CI changes.

There is no cancellation API. Once a synchronous repository call begins, it either returns its complete result or throws one complete failure. Java thread interruption does not create a separately supported partial or cancellation outcome.

## Current baseline

- The project uses Java 25, JUnit Jupiter 5.14.4, the Gradle Wrapper, Checkstyle, and JaCoCo.
- Production dependencies contain JavaFX only; no JSON library is present.
- The current source tree contains Developer 1's canonical report domain and Student submission slice. This implementation adds only the persistence package and consumes the canonical domain directly.
- Version 1 is the first report-store format; no data migration is required.
- A separate local-auth design demonstrates repository naming, but its non-atomic fallback and its unrelated byte limit do not satisfy this PRD and must not be copied.

## Design comparison

| Option | Decision | Reason |
| --- | --- | --- |
| Explicit `loadAll`, `insert`, and `replace` methods | Selected | It is the smallest honest surface matching the approved operation set and gives each conflict clear semantics. |
| One `apply(ReportChange)` method with sealed insert/replace commands | Rejected | It adds public command types and dispatch for hypothetical future operations without reducing present complexity. |
| Caller-facing `saveAll` or mutable collection | Rejected | It leaks ordering, duplicate, immutable-field, and lost-update responsibilities to callers. |
| `replace(replacement)` with no original identity | Rejected | A replacement report alone cannot reliably distinguish an intended update from Report-ID retargeting. |
| Public codec or filesystem configuration | Rejected | Format, limit, and transaction policy must stay consistent across callers and repository instances. |
| New JSON dependency | Rejected for this mission | Dependency changes are outside scope and owned jointly; the supported schema is small enough for a dedicated strict codec. |
| Checked exception with a typed reason | Selected | Callers must handle storage failure and switch on a stable category without parsing text or seeing report contents. |

## Confirmed public test seam

The repository owner confirmed the following seam on 2026-09-19 and approved its revised shared-model use on 2026-09-20.

```java
public interface ReportRepository {
    List<ItemReport> loadAll() throws ReportStoreException;

    void insert(ItemReport report) throws ReportStoreException;

    void replace(UUID targetId, ItemReport replacement)
            throws ReportStoreException;
}
```

The interface imports `java.util.UUID` and the exact shared `ItemReport`. It must not use `Object`, a persistence wrapper, a duplicate DTO, or a second identifier type.

The production adapter is constructed with one caller-owned location:

```java
ReportRepository repository = new JsonReportRepository(reportStorePath);
```

Construction rejects a null path, captures `toAbsolutePath().normalize()`, and performs no filesystem I/O. The schema version, byte limit, codec, and file transaction policy are fixed module policy rather than caller configuration.

The public adapter is final. Its public constructor delegates to this exact package-private test/integration constructor:

```java
JsonReportRepository(
        Path reportStorePath,
        ReportStoreFiles storeFiles,
        int maximumStoreBytes)
```

The package-private constructor rejects null collaborators and a non-positive or overflow-unsafe bound, captures the same normalized path, and performs no I/O. The public constructor supplies `new NioReportStoreFiles()` and `MAX_STORE_BYTES`. It does not permit application callers to select a different format or storage policy.

### `loadAll` contract

- A missing target returns `List.of()` and creates neither the target nor its parent directory.
- A valid target returns every canonical report exactly once in array order.
- The result is an unmodifiable structural snapshot, not a live repository collection.
- No report is filtered or redacted; trusted callers receive complete canonical reports.
- Any invalid report invalidates the whole load. No partial list is returned.

### `insert` contract

- The existing store is read and completely validated first.
- A canonically equal Report ID produces `DUPLICATE_REPORT_ID` and no write.
- A successful insertion appends the report and becomes visible through a fresh repository instance before the call returns.
- Candidate Unicode and size are validated before a directory, temporary file, or target is created.

### `replace` contract

- The original target identity is supplied separately as a `UUID`.
- The existing store is read and completely validated first.
- Target lookup uses `UUID.equals` without persistence-owned normalization.
- A missing target is reported before examining representable immutable mismatches.
- The replacement Report ID must equal the explicit target ID. Its Reporter ID and Created At values must equal the stored target under canonical equality.
- A successful replacement occupies the target's existing array position.
- Every other canonical field is persisted as supplied. The repository does not decide whether a status transition or field edit is permitted.
- An identical replacement follows the normal validated atomic-replacement path; it is not a special no-I/O success.

### Shared operation invariants

- Null public arguments are programmer errors and throw `NullPointerException` before filesystem access.
- All public operations on one `JsonReportRepository` instance are serialized and linearizable.
- Every operation rereads the target; there is no cache that could hide external corruption or overwrite externally recovered bytes.
- Distinct repository instances and other processes writing concurrently are unsupported.
- Whole-store time and memory use are finite and proportional to the approved byte bound.
- A successful method has no partial result. A failed mutation never reports success.
- Retrying a read is side-effect free. Retrying an insertion after its first success returns the duplicate reason. Retrying the same valid replacement is a new successful atomic replacement with the same logical result. A failed storage operation may be retried after external recovery because the next call rereads the authoritative target.

## Public failure contract

One checked, final exception exposes one typed reason:

```java
public final class ReportStoreException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum Reason {
        DUPLICATE_REPORT_ID,
        REPLACEMENT_TARGET_NOT_FOUND,
        IMMUTABLE_FIELD_MISMATCH,
        CORRUPT_OR_UNSUPPORTED_STORE,
        STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE,
        UNENCODABLE_OR_OVER_LIMIT_RESULT
    }

    private final Reason reason;

    public ReportStoreException(Reason reason) {
        super(messageFor(reason), null, false, true);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    // messageFor is the exhaustive fixed mapping in the table below.
}
```

The constructor accepts only a `Reason` and selects the exact fixed message in the table below. It calls the four-argument `Throwable` constructor with a null cause and suppression disabled, so the public exception has no raw parser, domain, filesystem, or report-bearing cause or suppressed exception. Callers branch on `reason()`, never message text.

| Condition | Reason | Exact exception message | Target outcome |
| --- | --- | --- | --- |
| Insert ID already exists | `DUPLICATE_REPORT_ID` | `A report with that identifier already exists.` | Existing bytes unchanged |
| Replacement target is absent | `REPLACEMENT_TARGET_NOT_FOUND` | `The report to replace does not exist.` | Existing bytes unchanged; missing target remains missing |
| Replacement changes Report ID, Reporter ID, or Created At | `IMMUTABLE_FIELD_MISMATCH` | `The replacement changes immutable report identity.` | Existing bytes unchanged |
| Existing bytes are malformed, invalid UTF-8, structurally invalid, non-canonical, duplicated, unsupported, or oversized | `CORRUPT_OR_UNSUPPORTED_STORE` | `The report store is corrupt or uses an unsupported format.` | Existing bytes unchanged; all operations blocked |
| Existing path is unreadable/non-regular, or directory creation, staging, forcing, cleanup of a failed transaction, or atomic replacement fails | `STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE` | `The report store could not be accessed or safely replaced.` | No success reported; previous target preserved |
| Proposed canonical data contains invalid Unicode or the complete encoded result exceeds the bound | `UNENCODABLE_OR_OVER_LIMIT_RESULT` | `The requested report state is not encodable or exceeds the supported store limit.` | No destination mutation; first target remains absent |

Failure precedence for a mutation is:

1. null/programmer misuse;
2. existing-path access, bound, decode, schema, and canonical validation;
3. duplicate or missing-target conflict;
4. immutable-field comparison;
5. candidate Unicode, encoding, and capacity validation; and
6. filesystem staging and atomic commit.

This order ensures invalid existing storage can never be overwritten by a seemingly valid mutation.

## Module structure and ownership

Developer 1's canonical report domain is:

```text
io.github.cs32272610mp2xcode.finderskeepers.report
├── ItemReport.java                       public immutable final value object
├── ReportType.java                       LOST, FOUND
├── ItemCategory.java                     nine canonical categories
├── ReportStatus.java                     SUBMITTED, UNDER_REVIEW
└── ReportConstraints.java                stable size and temporal formats
```

`ItemReport` exposes eleven accessors and the following persistence restoration factory:

```java
public static ItemReport restore(
        UUID reportId,
        String reporterId,
        ReportType reportType,
        String itemName,
        ItemCategory category,
        String location,
        LocalDate occurrenceDate,
        String publicDescription,
        String privateIdentifyingDetail,
        ReportStatus status,
        Instant createdAt)
```

Clockless restoration rejects null or blank fields, enforces the canonical inclusive code-point bounds, and rejects an `Instant` whose nanoseconds are not divisible by `1_000_000`. It preserves every accepted string exactly and does not apply the clock-dependent occurrence-date rule. Value equality covers all fields; repository identity and uniqueness use `reportId`, while immutable Reporter ID comparison uses exact case-sensitive `String.equals`. `toString()` returns a fixed redacted form with no field values: exactly `ItemReport[redacted]`.

The persistence package is:

```text
io.github.cs32272610mp2xcode.finderskeepers.report.persistence
├── ReportRepository.java                 public seam
├── JsonReportRepository.java             public production adapter
├── ReportStoreException.java             public checked failure
├── ReportStoreJsonCodec.java             package-private strict v1 codec
├── ReportStoreFiles.java                 package-private filesystem boundary
└── NioReportStoreFiles.java              package-private production filesystem adapter
```

The package is a deep module: three caller operations hide parsing, canonical reconstruction, store-wide invariants, conflict logic, ordering, bounded encoding, staging, forcing, atomic movement, cleanup, and failure sanitization.

`ReportStoreFiles` is the only substitutable internal boundary because local filesystem failure genuinely varies across platforms. Its conceptual contract is limited to a bounded read and an atomic whole-document replacement:

```java
interface ReportStoreFiles {
    Optional<byte[]> readBounded(Path target, int maximumBytes)
            throws StoreFileFailure;

    void replaceAtomically(Path target, byte[] completeDocument)
            throws StoreFileFailure;
}
```

These names illustrate the boundary, not a separately approved public API. Normal tests use `NioReportStoreFiles` and `@TempDir`. A package-private deterministic fault adapter implements the same boundary only for read, pre-commit staging, atomic-move, and cleanup outcomes that cannot be induced portably. Tests observe repository outcomes and target bytes, not collaborator call counts.

The codec parses directly into local scalar values and immediately invokes the clockless `ItemReport.restore(...)` factory. Parsed maps, token objects, and local variables are implementation details, not a second report model.

### Canonical report invariants

| Field | Java type | Canonical rule |
| --- | --- | --- |
| `reportId` | `UUID` | Required; standard UUID equality |
| `reporterId` | `String` | Required; 1–128 Unicode code points; exact case-sensitive equality |
| `reportType` | `ReportType` | Required; `LOST` or `FOUND` |
| `itemName` | `String` | Required; 1–100 Unicode code points |
| `category` | `ItemCategory` | Required; any canonical category |
| `location` | `String` | Required; 1–120 Unicode code points |
| `occurrenceDate` | `LocalDate` | Required; clock-dependent today-or-earlier validation is external |
| `publicDescription` | `String` | Required; 1–500 Unicode code points |
| `privateIdentifyingDetail` | `String` | Required; 1–500 Unicode code points |
| `status` | `ReportStatus` | Required; initially `SUBMITTED` or `UNDER_REVIEW` |
| `createdAt` | `Instant` | Required; nanoseconds divisible by 1,000,000 |

An enum constant name is a compatibility token once persisted. Adding a constant is allowed, while renaming or removing a persisted constant requires a version or migration decision. The value object does not generate IDs or time values and exposes no status-transition policy.

## Version 1 JSON contract

### Document grammar

The complete UTF-8 document is one JSON object with exactly these members:

| Member | JSON type | Rule |
| --- | --- | --- |
| `schemaVersion` | integer | Required; the only supported lexical and numeric value is `1` |
| `reports` | array | Required; array order is repository insertion order |

Each `reports` element is one object with exactly these members:

| Member | Presence | JSON representation |
| --- | --- | --- |
| `reportId` | Required store member | Lowercase canonical 36-character UUID string; other spellings are rejected |
| `reporterId` | Required store member | Exact JSON string satisfying the canonical code-point bound |
| `reportType` | Required store member | Exact stable enum token `LOST` or `FOUND` |
| `itemName` | Required store member | Exact JSON string satisfying the canonical code-point bound |
| `category` | Required store member | Exact canonical `ItemCategory.storedName()` token |
| `location` | Required store member | Exact JSON string satisfying the canonical code-point bound |
| `occurrenceDate` | Required store member | Canonical `LocalDate.toString()` ISO-8601 string that round-trips unchanged through `LocalDate.parse` |
| `publicDescription` | Required store member | Exact JSON string satisfying the canonical code-point bound |
| `privateIdentifyingDetail` | Required store member | Exact JSON string satisfying the canonical code-point bound |
| `status` | Required store member | Exact stable enum token `SUBMITTED` or `UNDER_REVIEW` |
| `createdAt` | Required store member | UTC instant with exactly three fractional digits and terminal `Z`; parsed value must have millisecond precision |

All eleven member names are always emitted as a storage-format rule. Every value is required and represented as a JSON string; JSON `null` is always rejected. Decoding does not trim, Unicode-normalize, default, change case, or change temporal precision.

Object member order is irrelevant on input. Array order is significant. The decoder rejects duplicate, unknown, or missing members at both object levels; wrong types; invalid escapes; raw control characters in strings; invalid UTF-8; unpaired UTF-16 surrogate escapes; duplicate canonical Report IDs; non-canonical reports; non-JSON whitespace; a UTF-8 BOM; and any non-whitespace trailing content.

The decoder is schema-directed rather than a recursive general-purpose object mapper. It accepts only the fixed nesting required by the root object, report array, and report objects; excessive or unexpected nesting is rejected without recursive descent proportional to attacker-controlled depth.

### Accepted lexical subset

Except for the schema restrictions below, tokenization follows RFC 8259 JSON:

- Input is strict UTF-8 without a BOM.
- Leading, inter-token, and trailing JSON whitespace may contain only space, horizontal tab, line feed, or carriage return. After optional trailing JSON whitespace, any further byte is trailing content and is rejected.
- Strings are double-quoted. Unescaped quotation mark, reverse solidus, and code points `U+0000` through `U+001F` are rejected inside a string.
- The accepted escapes are quotation mark, reverse solidus, solidus, backspace, form feed, line feed, carriage return, horizontal tab, and `\u` followed by exactly four case-insensitive hexadecimal digits.
- A `\u` high-surrogate escape must be immediately followed by a `\u` low-surrogate escape. The pair decodes to one supplementary scalar. A lone, reversed, or otherwise unpaired surrogate is rejected. Equivalent escaped and unescaped valid scalar values reconstruct the same canonical text.
- Member-name equality and duplicate detection occur after escape decoding. Escaped spellings of an approved member name are accepted, and two spellings that decode to the same name are a duplicate.
- `schemaVersion` accepts only the exact ASCII number token `1`. Tokens such as a decimal or exponent spelling of the same numeric value are rejected, as are a sign, leading zero, or any other number.
- JSON `null`, booleans, arrays, objects, and numeric values are rejected for every report member.

Object member order and permitted whitespace do not affect meaning. The codec does not accept comments, single-quoted strings, trailing commas, non-finite numbers, JavaScript escapes, or any other extension to the subset above.

### Canonical emitted bytes

The encoder emits:

- UTF-8 without a BOM;
- two-space indentation;
- line feed (`U+000A`) line endings and one final line feed;
- top-level members in the table order;
- report members in the table order;
- reports in stable insertion order;
- lowercase JSON literals;
- the shortest required escapes for quotation mark, reverse solidus, and the standard escaped control characters, with remaining control code points represented by lowercase four-hex-digit `\u` escapes; and
- accepted non-control Unicode scalar values directly as UTF-8.

The encoder walks Unicode by code point, preserves valid supplementary characters, and rejects an unpaired Java UTF-16 surrogate before filesystem mutation. Text resembling JSON remains string data and cannot create a member or report.

An independent literal version 1 fixture, written only in test source with synthetic data, verifies the decoder and canonical emitted bytes. Production encoding code is never used to manufacture the expected fixture.

## Bounded resource policy

`MAX_STORE_BYTES` is the fixed inclusive module policy `16,777,216` bytes (16 MiB). The maximum-plus-one probe therefore cannot overflow.

The sizing basis is 1,000 expected retained reports. The five bounded text fields contain at most 1,348 code points per report. The longest supported JSON escape is six UTF-8 bytes, so their conservative combined maximum is 8,088 bytes per report. UUID, enum, date, instant, member-name, quote, comma, indentation, newline, root, and array framing are conservatively bounded below 512 bytes per report plus fixed document framing. The resulting 1,000-report estimate is below 8.6 MB. The 16 MiB policy leaves more than seven megabytes of headroom while still providing a finite resource ceiling. The byte limit, rather than a report count, is authoritative, so more than 1,000 small reports may fit and a smaller number of large reports may reach the bound.

The final implementation must:

- read at most `MAX_STORE_BYTES + 1` bytes to distinguish at-limit from over-limit input;
- reject an existing store larger than the limit before decoding it;
- decode with a strict UTF-8 `CharsetDecoder` configured to report malformed and unmappable input;
- keep parser collections and text bounded by the accepted input bytes and canonical domain maxima;
- encode through a bounded UTF-8 sink that stops after `MAX_STORE_BYTES + 1`, rather than first building an unbounded document;
- accept a valid complete document whose encoded length is at or below the limit; and
- reject a candidate whose complete encoded length is above the limit before any destination-side filesystem action.

The package-private constructor makes exact boundary tests small and deterministic. A valid candidate is independently encoded, then the same candidate is tested with its byte length as the bound and with one byte less. This proves the inclusive comparator path even if no canonical candidate can be constructed at exactly the production constant. Separate tests exercise valid stores below, at, and above the production read bound, and structural review verifies the public constructor's approved constant and sizing calculation.

## Read flow

```text
synchronized repository method
        ↓
bounded read of the configured target
        ├── target missing → empty ordered state
        ├── access/non-regular failure → privacy-safe storage failure
        └── bytes present
                ↓
strict UTF-8 decode and version 1 parse
                ↓
reconstruct every canonical ItemReport
                ↓
validate canonical Report-ID uniqueness
                ↓
return an unmodifiable ordered snapshot
```

No read creates a directory, target, temporary file, backup, or recovery file. Only a genuinely absent final target is the empty-store case. A parent component that is not a directory, an indeterminate existence result, or any access error is a storage failure rather than an empty repository.

## Mutation flow

Both mutations use this order while holding the instance monitor:

1. Validate non-null arguments.
2. Read and validate the complete current store. Missing means an empty store.
3. Apply insert or replacement conflict and immutable-field rules to an in-memory ordered list.
4. Strictly encode the complete candidate and enforce the byte limit.
5. Only after successful encoding, create missing parent directories if needed.
6. Create a randomly named temporary sibling in the target directory. No report value contributes to its name.
7. Write every candidate byte through a `FileChannel`, force the temporary file with `force(true)`, and close it.
8. Invoke `Files.move` once with `ATOMIC_MOVE` and `REPLACE_EXISTING`. If the provider does not support atomically replacing that target, report failure; never delete the old target first and never retry with a non-atomic move.
9. Return success only after the atomic move returns successfully.
10. Best-effort delete an uncommitted temporary file. A cleanup problem must not
    hide the primary fixed failure reason or expose a path/report value.

After a successful commit there is no later fallible action that can convert the committed mutation into a reported failure. Cleanup after success is unnecessary because the moved temporary path no longer exists.

If first-insertion staging created previously absent parent directories and a later I/O step fails, empty directories may remain. The report target remains absent and no partial document is authoritative; removing newly created parents is deliberately avoided because their ownership can no longer be established safely.

`NioReportStoreFiles` checks the final target without following a final-component symbolic link and opens reads with `NOFOLLOW_LINKS` where the provider supports it. An observed final-component symlink is rejected as non-regular. The target is never opened for truncating writes. Concurrent replacement of path components by another process is outside the supported one-process writer model; this design does not claim a portable race-free filesystem sandbox.

## Transaction states and recovery

| State at interruption or failure | Target state | Temporary state | Next repository call |
| --- | --- | --- | --- |
| Before candidate encoding completes | Previous target or absent | None | Reads the previous state; invalid candidate had no filesystem effect |
| Parent creation or temporary creation fails | Previous target or absent | None or an empty uncommitted sibling | Reports storage failure; best-effort cleanup; target is not rewritten |
| Temporary write fails | Previous target or absent | Partial uncommitted sibling possible | Reports storage failure; best-effort cleanup; target remains authoritative |
| Temporary force or close fails | Previous target or absent | Complete or partial uncommitted sibling possible | Reports storage failure; best-effort cleanup; target remains authoritative |
| Atomic move is unsupported or fails | Previous target or absent | Complete forced sibling possible | Reports storage failure; no non-atomic fallback; best-effort cleanup |
| Atomic move succeeds | Complete new target | No separate sibling | Reports success; fresh instances read the new state |
| Application process terminates before atomic move | Previous target or absent | Orphan sibling possible | Target remains authoritative; orphan is ignored, not auto-recovered |
| Application process terminates after atomic move returns while the OS/storage remain operational | Complete new target | No separate sibling | Target is parsed normally by a fresh process |
| Machine, operating system, or storage device fails suddenly | Filesystem/provider-dependent; no module guarantee | Filesystem/provider-dependent | On recovery, strictly evaluate the target that exists; never infer success from a sibling or auto-repair |

An orphan temporary sibling is never treated as a report store, backup, or recovery candidate. Automatic cleanup on startup is excluded because deleting an unrecognized file could be unsafe. A hard stop can therefore leave plaintext report data in an orphan sibling until an operator removes it.

The durability threshold promised by this feature is process-visible: after the forced temporary file has been atomically moved and the call returns, later repository instances observe one complete document while the operating system and storage remain operational. Portable directory-entry forcing and guarantees across sudden machine, storage-device, or operating-system failure are not available from this module and are explicitly outside the guarantee. The module still never intentionally uses a non-atomic replacement.

## Storage-state decision table

| Current state | `loadAll` | `insert` / `replace` | Automatic action |
| --- | --- | --- | --- |
| Target and parent absent | Empty success, no side effect | Validate candidate, then create storage atomically | None on read |
| Valid supported store | Complete ordered success | Apply requested mutation atomically | None |
| Empty file or malformed JSON | Corrupt/unsupported failure | Same failure before conflict checks | Preserve bytes |
| Invalid UTF-8 or invalid JSON string | Corrupt/unsupported failure | Same failure before conflict checks | Preserve bytes |
| Structurally invalid or non-canonical report | Corrupt/unsupported failure | Same failure before conflict checks | Preserve bytes |
| Unsupported `schemaVersion` | Corrupt/unsupported failure | Same failure before conflict checks | Preserve bytes; no migration |
| Existing bytes exceed limit | Corrupt/unsupported failure | Same failure before conflict checks | Preserve bytes |
| Target is unreadable, a directory, symlink, or other non-regular path | Storage failure | Storage failure | Do not follow, replace, or alter it |
| Unrelated orphan temporary sibling exists | Ignore it and evaluate target only | Ignore it and evaluate target only | No automatic deletion or recovery |

## Compatibility and migration

- Version 1 is the only accepted version.
- The decoder does not infer a version, accept aliases, ignore new members, or coerce old representations.
- Unsupported versions remain untouched and block mutations.
- A future format change requires a separately approved compatibility or migration decision; it cannot silently broaden the version 1 decoder.
- A change to storage-relevant canonical requiredness, validation, identifier equality, enum storage names, or date/time representation is also a compatibility change. Developer 1 and Developer 2 must jointly decide whether it requires a new schema version or migration before that domain change can make an existing version 1 store unreadable.
- Deterministic canonical output does not make manual JSON editing supported.
- A valid version 1 document with alternate permitted whitespace or member order is accepted. The next successful mutation, including an identical replacement, rewrites it in canonical emitted form; rejected mutations preserve its exact original bytes.

## Privacy and security controls

- The configured path is the only path input. Report fields never participate in path resolution, file naming, or error text.
- JSON strings are encoded as data and decoded strictly; structural punctuation inside them cannot escape the value.
- Private identifying detail remains a distinct plaintext member. Encryption and operating-system permissions are non-goals.
- Exceptions expose only a reason and fixed generic message, with no report value, JSON excerpt, parser token, path derived from a report, raw cause, or report `toString()` output.
- This package emits no report-content logs.
- Tests compare reports and bytes with fixed, value-free assertion messages so a failing assertion does not print synthetic private detail. Synthetic values exist only in test source and isolated temporary files.
- Documentation, screenshots, test output, and handoff summaries contain no example report field values.

## Test seams

| Seam | Status | Evidence strategy |
| --- | --- | --- |
| TS-01: Public `ReportRepository` backed by `JsonReportRepository(Path)` | Confirmed - repository owner, 2026-09-19 | Use the real adapter and `@TempDir`; reconstruct repositories for persistence evidence |
| TS-02: Version 1 byte compatibility at the configured target | Confirmed - repository owner, 2026-09-19 | Seed independent literal bytes, call only public repository operations, and compare canonical output with independent literal bytes |
| TS-03: Package-private `ReportStoreFiles` fault adapter | Confirmed - repository owner, 2026-09-19 | Inject only non-portable read, staging, force-equivalent, atomic-move, and cleanup outcomes; assert public reason and target state, never call counts |
| TS-04: Shared-instance concurrency | Confirmed - repository owner, 2026-09-19 | Coordinate public calls with barriers/executors and assert a serial-equivalent persisted result |
| TS-05: Structural and command evidence | Confirmed - repository owner, 2026-09-19 | Review imports/types/docs and the real NIO same-directory, force, single-atomic-move, no-fallback sequence; then run focused tests and `gradlew.bat check` |
| TS-06: Canonical `ItemReport.restore(...)`, accessors, equality, and redacted `toString()` | Confirmed - repository owner, 2026-09-20 | Reuse Developer 1's domain tests and verify persistence reconstruction, exact preservation, enum storage APIs, millisecond precision, and privacy-safe representation without applying clock-dependent submission policy |

The complete stable test catalog and traceability are in `RequirementsToTests.md`. No persistence test may use real application storage.

## Vertical red-green implementation slices

After separate implementation authorization, work proceeds one observable behavior at a time. Each slice begins with one failing behavior test and adds only the implementation needed to make it green. Review and any refactoring are a separate step before the next failing behavior is introduced, with the green suite preserved.

1. **Canonical report:** requiredness and one text boundary, then remaining code-point bounds, exact preservation, equality, enums, millisecond precision, and redacted string representation through TS-06.
2. **Missing read:** public seam, path capture, and side-effect-free empty load.
3. **First reconstruction:** minimal approved canonical report, version 1 codec, first atomic insertion, and fresh-instance load.
4. **Complete state and order:** all eleven required values, multiple appends, immutable snapshots, and canonical bytes.
5. **Insert conflict:** canonical ID equality and byte-preserving duplicate failure.
6. **Replacement:** explicit target, position retention, mutable fields, then the missing and immutable conflict cases one at a time.
7. **Strict input:** each invalid encoding/grammar/schema/canonical-state partition added separately, always proving whole-store blocking.
8. **Adversarial text:** fixed corpus, valid supplementary Unicode, generated Unicode, structure invariants, and invalid-surrogate rejection.
9. **Capacity:** existing-store and candidate below/at/above boundaries.
10. **Filesystem faults:** access, pre-commit staging/force, atomic-move, and cleanup outcomes with previous-target preservation.
11. **Concurrency and closure:** overlapping shared-instance operations,
    structural ownership review, storage documentation, focused suite, and full
    `check`.

No horizontal batch of test skeletons is written ahead of its active slice.

## Blocking decisions

Every row must be resolved in this document and in `RequirementsToTests.md` before TDD approval.

| Blocker | Owner | Required resolution | Status |
| --- | --- | --- | --- |
| TDD-B01 | Canonical domain integration | Developer 1's `report` package; clockless `ItemReport.restore(...)` and accessors | Resolved by Developer 1 contract and integration review |
| TDD-B02 | Canonical domain integration | `UUID` identity and exact case-sensitive Reporter ID semantics | Resolved by Developer 1 contract |
| TDD-B03 | Canonical domain integration | Immutable value object and explicit `UUID` replacement target | Resolved by Developer 1 contract |
| TDD-B04 | Canonical domain integration | All fields required with exact Java types and no normalization | Resolved by Developer 1 contract |
| TDD-B05 | Canonical domain integration | Stable `storedName()`/`fromStoredName(...)` tokens for every canonical enum constant | Resolved by Developer 1 contract and integration review |
| TDD-B06 | Canonical domain integration | Required `LocalDate` and canonical round-tripping ISO-8601 string | Resolved by Developer 1 contract |
| TDD-B07 | Canonical domain integration | Required millisecond-precision `Instant`; exact three-digit UTC `Z` representation | Resolved by Developer 1 contract |
| TDD-B08 | Canonical domain integration | Requiredness, code-point bounds, precision, redacted `toString()`; clock rule remains in submission | Resolved by Developer 1 contract |
| TDD-B09 | Developer 2 with repository owner | 1,000-report sizing calculation and `MAX_STORE_BYTES = 16,777,216` | Resolved 2026-09-20 |
| TDD-B10 | Repository owner | Confirmation of TS-01 through TS-05 and the proposed public error/interface seam | Resolved 2026-09-19 |

No blocker remains. The repository owner approved the revised planning artifacts and separately authorized implementation on 2026-09-20.

## Cross-workstream handoff

Developer 1 need not implement persistence. Developer 2 consumes Developer 1's canonical model directly and removes the superseded temporary model. A competing `ItemReport` or enum set is prohibited. Future changes to persisted requiredness, enum names, identity, or temporal representations require joint compatibility review.

Adding a JSON dependency is not proposed. If the team later prefers one, Developer 2 must identify `build.gradle`, explain the smallest dependency change, ask the owning developer to make it or obtain explicit authorization, and return this TDD for review.

Application startup and role workflows remain deferred to their separately approved work. They must compose one shared repository instance and translate `ReportStoreException.Reason` without reading the JSON file directly.

## Verification and completion gate

Implementation is not complete until:

1. this TDD and the test mapping are approved with dates;
2. no blocker or deferred traceability row remains other than explicitly traced later-workflow obligations;
3. tests are implemented vertically and every mapped automated test passes;
4. test storage is synthetic and isolated under temporary directories;
5. focused persistence tests pass;
6. `gradlew.bat check` passes on Windows, or the repository-approved equivalent passes on the executing platform;
7. the diff contains persistence, persistence tests, and documentation with no duplicate model, canonical-domain modification, dependency, startup, JavaFX, release, or CI change;
8. storage schema, bound, recovery, plaintext, orphan-temp, durability, and unsupported-concurrency limitations are documented; and
9. the repository owner separately authorizes and then accepts implementation.

The repository owner approved this decision-complete technical design and separately authorized production implementation and incremental commits on 2026-09-20. Pushing, merging, and publishing remain unauthorized.
