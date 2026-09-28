# S3-D2-01 Claims and Verification Technical Design

- Status: Approved
- Draft date: 2026-09-22
- Approver: Repository owner
- Approval date: 2026-09-22
- Owner: Developer 2
- Feature: Claims and Verification
- Workstream: Developer 2
- Mission Brief: `docs/mission-briefs/S3-D2-01-claims-and-verification.md` (Approved 2026-09-22)
- Decision ledger: `docs/features/S3-D2-01/GrillingDecisions.md` (Approved and complete 2026-09-22)
- Source PRD: `docs/features/S3-D2-01/PRD.md` (Approved 2026-09-22)
- Inspected baseline: `royden/feat-officer-review-queue` at `2512a5d761759e177bc88cc75724b1226a9642ea`
- TDD drafting authorization: Explicit current repository-owner request
- Product-owner clarification: My claims, officer pending, and officer history rows use the FOUND report's category; section 9.3 records this source
- Requirements-to-Tests, implementation, tests, branch creation, commits, pushes, pull requests, merges, and cross-owner production edits: Not authorized

## 1. Authority, scope, and preflight outcome

This document decides how to implement the approved product behaviour. It does not change what the feature does. Precedence is `AGENTS.md`, the approved Mission Brief, the approved Grilling Decisions, the approved PRD, verified repository contracts, then this TDD.

The complete governing artifacts were read in that order. The Mission Brief, decision ledger, and PRD are approved. Their older statements that the next artifact was not authorized record the gate state when each file was written. The current request is the later, separate authorization to create only this TDD. No upstream conflict was found. The product owner resolved the one narrow row-projection ambiguity on 2026-09-22: the singular category in My claims, officer pending, and officer history is the FOUND report's category, without assuming that the LOST and FOUND categories are identical. Section 9.3 applies that clarification; no unrelated product decision is reopened.

The verified worktree is the exact planning baseline named by the approved artifacts. It already contains the Sprint 2 officer review and possible-match implementation. It also contains pre-existing tracked and untracked work unrelated to this TDD; none of that work is modified here.

This design adds one separate Claims domain and store. It consumes the existing authentication identity, canonical reports, and durable possible-match links. It never writes reports or links. It does not add a report status, copy a report into a claim, record a deciding officer, add a dependency, or create a generic workflow framework.

## 2. Verified implementation baseline

`base` below means `io.github.cs32272610mp2xcode.finderskeepers`. Java paths are under `src/main/java/` unless stated otherwise.

| Existing interface or file | Verified contract relevant to Claims | Ownership |
| --- | --- | --- |
| `base.report.ItemReport` | Final immutable canonical report. UUID Report ID, String Reporter ID, type, item name, category, location, occurrence date, public description, private identifying detail, status, and millisecond-precision Instant creation time. Redacted `toString`. | Developer 1 |
| `ReportType`, `ItemCategory`, `ReportStatus`, `ReportConstraints` | LOST/FOUND orientation, canonical categories, only SUBMITTED/UNDER_REVIEW statuses, and existing report date/time formatting. Claims must not extend them. | Developer 1 |
| `base.report.persistence.ReportRepository` | `loadAll`, `insert`, and complete `replace`; `loadAll` is sufficient for every Claims read and revalidation. Claims use no write operation. | Developer 2 shared interface |
| `JsonReportRepository` and package-local file helpers | Strict version-one JSON, 16 MiB bound, synchronized instance operations, forced sibling staging and atomic replacement. This is a pattern, not a codec or schema Claims may reuse. | Developer 2 |
| `base.matching.model.PossibleMatchPair` | Canonical unordered pair of distinct Report IDs. | Developer 2 |
| `base.matching.persistence.PossibleMatchRepository` | `loadAll`, `link`, `unlink`; `loadAll` supplies the complete durable link set required for Student discovery and submission revalidation. Claims never call the mutations. | Developer 2 |
| `FilePossibleMatchRepository` | Synchronized, strict version-one pair file at `data/possible-match-links.txt`, with no report copies. | Developer 2 |
| `base.auth.model.AuthenticatedUser`, `UserRole` | Per-login stable `userId`, username, and STUDENT/DESK_OFFICER role. | Developer 2 |
| `AuthenticationCoordinator`, `AuthenticationPane` | In-memory current user and route; logout currently clears immediately and has no unsaved-draft consultation hook. Student factory receives the user; officer content currently uses a no-argument Supplier. | Developer 2 |
| `StudentReportWorkspaceFactory`, `StudentReportHomePane` | The factory privately creates existing report controllers. The pane owns the top-level Student tabs, currently Report an item then My reports. | Developer 1 |
| `DeskOfficerWorkspacePane` | Developer 2-owned top-level officer TabPane, currently Report review then Possible matches. | Developer 2 |
| Existing application services and panes | Stateful per-login plain-Java modules return immutable presentation state. JavaFX panes render state, use render guards, and do not own persistence rules. Unavailable and successful empty states are distinct. | Respective owners |
| `base.FindersKeepersApp` | Developer 1-owned application entry and shared composition. It creates one application-lifetime report repository and link repository, then injects role workspaces. | Developer 1 / shared composition |

Existing tests use JUnit Jupiter, `@TempDir`, synthetic immutable reports, fixed clocks, deterministic UUID suppliers, real local repositories, and package-local scripted file adapters. No JavaFX test framework is present. Repository documentation uses fenced text diagrams, so this TDD follows that convention.

No existing public contract is missing for Claims data access. The required production changes are composition/navigation hooks, not changes to report or possible-match interfaces.

## 3. Architecture and dependency direction

Claims use four modules with small interfaces and substantial behaviour behind them:

```text
JavaFX role panes
      |
      v
StudentClaimsService / OfficerClaimsService
      |              |                 |
      v              v                 v
 Claim domain   ReportRepository   PossibleMatchRepository
      |
      v
ClaimRepository interface
      |
      v
JsonClaimRepository -> strict codec -> package-local filesystem adapter
      |
      v
data/claims.json
```

Dependency rules are:

1. `claim.model` depends only on the JDK. It has no JavaFX, authentication, report, matching, path, or persistence dependency.
2. `claim.persistence` depends on `claim.model` and JDK filesystem classes. It implements the Claim repository interface and owns the single consistency boundary for claim state.
3. `claim.application` coordinates the Claim repository with read-only report and possible-match interfaces and a constructor-bound authenticated user. It creates role-specific projections rather than exposing Claims or reports indiscriminately.
4. `claim.ui` depends on application-facing state and JavaFX. It does not read repositories, inspect paths, derive eligibility, or perform transitions.
5. Existing report and matching domains do not depend on Claims. The only report-owned changes are neutral workspace-composition hooks described in the cross-owner section.

There is no existing neutral navigation/workspace package or interface in the verified repository. Placing shared composition types in `auth.ui` would make Developer 1-owned report UI depend on an authentication-presentation package for a non-authentication concern. The design therefore proposes the neutral `base.workspace` package, owned under Developer 2's package-architecture and role-navigation responsibility. It contains only `WorkspaceFeature` and `SessionView`; it is not a registry, plugin system, or generic feature framework. Developer 1-owned Student UI composition consumes only those small neutral values and imports no Claim model, repository, service, or auth UI.

Primary requirement support: PRD-IC-001–PRD-IC-004, PRD-NF-004, PRD-AC-X05.

## 4. Claim domain model

### 4.1 Types and exact retained fields

`claim.model.ClaimId` is an immutable wrapper around one non-null UUID. It exposes the UUID for persistence/application identity and derives the stable visible reference described in section 14. It has value equality and a redacted `toString`.

`claim.model.ClaimStatus` has exactly:

- `PENDING_REVIEW` / **Pending review**;
- `APPROVED` / **Approved**;
- `REJECTED` / **Rejected**; and
- `WITHDRAWN` / **Withdrawn**.

Stored names are the enum names; display labels are fixed separately.

`claim.model.Claim` is a final immutable class with only:

```text
ClaimId claimId
String claimantUserId
UUID lostReportId
UUID foundReportId
String ownershipEvidence
ClaimStatus status
Instant submittedAt
Optional<Instant> terminalAt
Optional<String> decisionReason
```

There is deliberately no link ID, match score, match reason, copied report field, officer ID, mutable evidence, deletion flag, collection state, return state, or persisted lock record.

Construction paths are:

- `Claim.createPending(...)` for a validated new claim;
- `Claim.restore(...)` for strict persistence reconstruction;
- `approve(Optional<String>, Instant)`, `reject(String, Instant)`, and `withdraw(Instant)` returning complete immutable copies.

The transition operations accept only Pending review. Calling one on a terminal Claim returns a typed domain conflict rather than a changed object. The repository uses these operations while holding its consistency lock. `equals`/`hashCode` cover all retained fields. `toString` is always `Claim[redacted]`.

### 4.2 Per-record invariants

- Claim ID, claimant ID, both report IDs, evidence, status, and submission time are required.
- LOST and FOUND Report IDs must differ.
- Claimant ID is the already-normalized authenticated `userId`; it must be nonblank and is preserved exactly. It is never a username or display label.
- Evidence is the normalized required text defined in section 4.3 and never changes after creation.
- All stored times use UTC `Instant` values truncated to exactly milliseconds.
- `PENDING_REVIEW`: no terminal time and no decision reason.
- `APPROVED`: terminal time required; decision reason absent or valid optional text.
- `REJECTED`: terminal time and a valid nonblank decision reason required.
- `WITHDRAWN`: terminal time required and decision reason absent.
- A terminal time cannot precede submission time.
- Terminal status cannot transition again or return to Pending review.

### 4.3 Shared text policy

`ClaimTextPolicy` is the single implementation of evidence and decision-reason text rules. It applies `String.strip()` to leading/trailing Unicode whitespace, then counts Unicode code points. The maximum is 500 code points after trimming. A required value needs at least one code point that is not whitespace, an ISO control, or a Unicode FORMAT code point. Ordinary punctuation and valid Unicode are retained.

Internal LF and CRLF line breaks are accepted and preserved; lone CR, tab, other ISO control characters, malformed surrogate pairs, and absent values are rejected. Optional approval input that is absent or becomes blank after trimming becomes `Optional.empty`; otherwise it uses the same validation. Evidence and reasons are never HTML or markup.

`ClaimValidationException` carries only a field category (EVIDENCE or DECISION_REASON) and fixed actionable copy. It never repeats the submitted text. Semantic Student-safety of a decision reason cannot be reliably inferred by code; the officer UI states the constraint and the stored/displayed reason is exactly the validated officer input.

Primary requirement support: PRD-LC-001, PRD-LC-002, PRD-SC-007, PRD-ST-003–PRD-ST-004, PRD-DC-002–PRD-DC-003, PRD-DC-006, PRD-PR-002, PRD-PR-006.

## 5. Lifecycle, locks, closures, and retained history

`ClaimLedger` is an immutable derived view over a complete Claim snapshot. It centralizes submission eligibility and store-wide validation. It exposes no mutation and no report data.

Persisted facts are Claim records and their events. The following are derived on every authoritative load/mutation:

| Derived fact | Derivation |
| --- | --- |
| Active pair | Pending claim with the same claimant, LOST ID, and FOUND ID |
| LOST lock | Any Pending claim with that LOST ID |
| FOUND lock | Any Pending claim with that FOUND ID |
| Approval closure | Any Approved claim closes both of its endpoint IDs permanently |
| Rejection closure | Any Rejected claim closes `(claimantUserId, foundReportId)` permanently |
| Withdrawal release | Withdrawn contributes no active lock or closure |

There is no second lock map or closure file to become inconsistent. Restart reconstructs every consequence from retained Claims.

Submission evaluation occurs in this order without leaking other claimants:

1. If the same claimant has an active claim for the exact pair, return that existing Claim as `OWN_ACTIVE_CLAIM`.
2. If the same claimant has another active Claim whose LOST or FOUND endpoint blocks this target, return that own Claim as `OWN_ACTIVE_CLAIM` so the UI can direct the Student to My claims.
3. If an approval closes either endpoint, a Pending claim locks either endpoint, or a rejection closes claimant-to-FOUND, return generic `BLOCKED`.
4. Otherwise the claim-state portion is eligible.

The application separately proves the current durable link, report availability, LOST/FOUND direction, and LOST ownership before calling the atomic submit operation.

Submission appends a new Pending claim. Withdrawal and rejection replace one Pending record with a terminal record and thereby release both active locks. Approval replaces it with Approved and thereby turns both endpoint locks into permanent closures. Because every mutation is a replacement of the complete Claim document, the lifecycle event and its lock/closure consequence have one commit point.

The store preserves submission insertion order. During load it rejects duplicate Claim IDs, invalid records, a second active LOST or FOUND lock, multiple approvals involving a closed endpoint, an active Claim involving an approved endpoint, a later stored claim violating an earlier rejection closure, and same-pair resubmission unless the preceding same-pair Claim is Withdrawn. Rejected and withdrawn history sharing endpoints in ways permitted by the PRD remains valid. No retained terminal Claim is deleted.

Transient UI facts—selected row, active sub-tab, draft evidence/reason, confirmation visibility, feedback, and last loaded projections—are never Claim fields and never persisted.

Primary requirement support: PRD-LC-003–PRD-LC-009, PRD-ST-005–PRD-ST-006, PRD-DC-005, PRD-IC-003, PRD-NF-001–PRD-NF-002.

## 6. Claim repository interface

The public persistence interface is intentionally workflow-aware only where needed to make claim mutation atomic:

```text
List<Claim> loadAll() throws ClaimStoreException

SubmissionResult submit(Claim pending) throws ClaimStoreException

TerminalResult withdraw(
    ClaimId id, String claimantUserId, Instant terminalAt)
    throws ClaimStoreException

TerminalResult approve(
    ClaimId id, Optional<String> decisionReason, Instant terminalAt)
    throws ClaimStoreException

TerminalResult reject(
    ClaimId id, String decisionReason, Instant terminalAt)
    throws ClaimStoreException
```

`SubmissionResult` and `TerminalResult` are immutable nested result types in `ClaimRepository`; collection/Claim values are defensively copied and their `toString` values are redacted.

`SubmissionOutcome` is:

- `CREATED`: the candidate was committed;
- `ID_COLLISION`: its Claim ID already exists; no other eligibility fact is revealed;
- `OWN_ACTIVE_CLAIM`: no write; includes only the same claimant's blocking Claim;
- `BLOCKED`: no write and no blocking Claim is returned.

`TerminalOutcome` is:

- `CHANGED`: one Pending Claim became the requested terminal state durably;
- `ALREADY_TERMINAL`: no write; includes the authoritative current terminal Claim so stale UI can report its status;
- `NOT_FOUND`: no write;
- `NOT_AUTHORIZED`: withdrawal claimant mismatch; no Claim is returned.

Approve/reject do not accept or store officer identity. Role authorization and current-report revalidation occur in the officer application module before the repository command; claim-state finality remains enforced again atomically in the repository. Withdrawal ownership is also checked inside the repository so a stale or fabricated Student handle cannot cross the claimant boundary.

Every method rereads and validates the complete current file. `submit` derives current locks/closures and checks ID uniqueness while synchronized. Terminal methods locate the authoritative Claim, verify Pending, validate the requested transition, encode the complete replacement, and report success only after commit. Callers never perform `loadAll` plus an unconstrained public `saveAll`. There is no public generic transaction callback, raw snapshot replacement, or lock-management method.

Primary requirement support: PRD-LC-003–PRD-LC-009, PRD-PR-001–PRD-PR-002, PRD-FL-001, PRD-NF-001–PRD-NF-002.

## 7. Dedicated persistence format

### 7.1 Location, bound, and schema

Claims use `data/claims.json`. Report-store v1 remains `data/reports.json`; possible-match storage remains `data/possible-match-links.txt`.

The Claim store is strict UTF-8 JSON with an inclusive 16,777,216-byte input and output bound, matching existing local-store resource policy. A missing file means an empty new store and causes no write. Zero bytes, an empty JSON object, or malformed content is corrupt, not empty.

Canonical shape:

```json
{
  "schemaVersion": 1,
  "claims": [
    {
      "claimId": "00000000-0000-0000-0000-000000000001",
      "claimantUserId": "synthetic-student-1",
      "lostReportId": "10000000-0000-0000-0000-000000000001",
      "foundReportId": "20000000-0000-0000-0000-000000000001",
      "ownershipEvidence": "Synthetic identifying detail.",
      "status": "PENDING_REVIEW",
      "submittedAt": "2026-09-22T01:02:03.456Z",
      "terminalAt": null,
      "decisionReason": null
    }
  ]
}
```

The example is synthetic. The root has exactly `schemaVersion` and `claims`. Every Claim object has exactly the nine members shown. IDs use canonical lowercase UUID text. Times use UTC ISO-8601 with exactly three fractional digits. `terminalAt` and `decisionReason` are either JSON null or a string; all other Claim values are strings. `schemaVersion` accepts only the numeric token `1`.

The array is authoritative stable submission insertion order. A terminal mutation replaces its record in place; submission appends. Encoder member order, two-space indentation, LF line endings, escaping, and a final LF are fixed. Existing valid but non-canonical whitespace/member order is accepted on read and canonicalized only by a later genuine mutation; a read never rewrites.

### 7.2 Strict decoding and validation

The dedicated codec follows the proven report-store pattern without importing or changing report codec classes. It rejects:

- malformed UTF-8, BOM, trailing non-whitespace, duplicate/unknown/missing members, wrong JSON types, unsupported/nonnumeric versions, and invalid string escapes or surrogate pairs;
- noncanonical UUID/time strings and unknown status names;
- duplicate Claim IDs or any per-record invariant failure;
- a store-wide lock, approval-closure, rejection-closure, or same-pair history contradiction described in section 5; and
- input larger than the bound.

The complete document is validated before any Claim is returned. There is no partial recovery, line skipping, guessed field, copied report repair, migration, automatic reset, or silent empty fallback. Unsupported version and corruption leave the bytes untouched and make Claims unavailable.

`ClaimStoreException.Reason` is:

- `CORRUPT_STORE`;
- `UNSUPPORTED_VERSION`;
- `READ_FAILURE`;
- `WRITE_FAILURE`; and
- `RESULT_TOO_LARGE`.

Messages are fixed and privacy-safe. They contain no cause, path, Claim ID, claimant ID, evidence, reason, report value, JSON token, or file excerpt.

Primary requirement support: PRD-LC-001, PRD-IC-001–PRD-IC-003, PRD-NF-001, PRD-PR-006, PRD-FL-001–PRD-FL-002.

## 8. Atomicity, durability, recovery, and supported concurrency

`JsonClaimRepository` normalizes its path to absolute form and synchronizes all public methods on the application-lifetime repository instance. It keeps no durable-state cache between calls.

One mutation is:

1. bounded read of the target;
2. strict decode and full Claim-ledger validation;
3. atomic business check and immutable candidate construction in memory;
4. bounded canonical encoding before touching the target;
5. creation of the parent when needed and one unique sibling `.claim-store-*.tmp`;
6. complete channel write, `force(true)`, close, then `ATOMIC_MOVE` with `REPLACE_EXISTING`; and
7. return of success with no later fallible storage read.

Unsupported atomic move is a write failure; there is no truncation or non-atomic fallback. Before commit, failure leaves the previous target (or absence) authoritative and cleans up only this operation's temporary file on a best-effort basis. Orphan temporary files are ignored and never promoted. After commit, the target alone is authoritative. Reads do not create, repair, or delete files.

Claim and lock/closure state cannot be partially committed because locks and closures are derived from the one Claim document. A failed submission adds no record and therefore acquires no lock. A failed terminal operation leaves the Pending record and its locks unchanged. Success is reported only after the atomic replacement has returned successfully.

The guaranteed consistency boundary is one JVM, one shared `JsonClaimRepository` instance, and its public operations. Concurrent calls on that instance are serial-equivalent. Therefore two competing submissions cannot both obtain a conflicting LOST/FOUND lock, duplicate active pair, or ID; and competing approve/reject/withdraw calls can commit only the first Pending-to-terminal transition. Later calls observe and return the durable terminal Claim.

The application uses synchronous JavaFX actions and one logged-in route at a time. Report/link revalidation and a Claim commit are not a cross-file transaction. The supported application has no competing report deletion API, and all in-process UI actions are serialized; this is sufficient for the local application. Distinct repository instances/processes, external writers, network filesystems, distributed locking, and hostile filesystem races are not guaranteed. A later need for those guarantees would require a separately approved storage design.

Durability matches the existing stores: complete file bytes are forced before atomic replacement and are visible to a fresh application run. There is no directory-entry fsync guarantee and no claim of surviving arbitrary power or storage-controller failure. After an interrupted operation with no returned outcome, the target present on restart is authoritative; no retrospective success is inferred.

| Storage state / fault | Result |
| --- | --- |
| Missing target with accessible ancestors | Successful empty Claim snapshot; no file created |
| Valid version-one store | Complete immutable Claims in insertion order |
| Corrupt, unsupported, duplicate, contradictory, or oversized store | Whole Claim store unavailable; preserve target; no mutation |
| Candidate exceeds output bound | `RESULT_TOO_LARGE`; preserve target |
| Stage/write/force/close/move failure | `WRITE_FAILURE`; no success; old committed target remains authoritative under the supported filesystem model |
| Orphan sibling temp | Ignore; target only is authoritative |

Primary requirement support: PRD-LC-003–PRD-LC-009, PRD-SC-009, PRD-ST-006, PRD-DC-005, PRD-FL-001, PRD-NF-001–PRD-NF-002.

## 9. Student application module and projections

### 9.1 Constructor authority and operations

`StudentClaimsService` is constructed per login with the complete `AuthenticatedUser`, Claim repository, Report repository, Possible Match repository, `Clock`, and `Supplier<UUID>`. Construction rejects any role other than STUDENT. The stable user ID is retained; username is presentation-only.

Its application-facing operations are:

```text
enter()
refreshAvailable() / retryAvailable()
refreshMyClaims() / retryMyClaims()
beginSubmission(AvailableMatchHandle)
reviewSubmission(AvailableMatchHandle, String rawEvidence)
submit(SubmissionReview)
selectMyClaim(StudentClaimHandle)
withdraw(StudentClaimHandle)
clear()
```

`enter` selects Available matches and performs its authoritative load. Entry, re-entry, sub-tab selection, Refresh, Retry, and authoritative selection reconciliation do not clear or overwrite unsaved evidence. The pane, not the service load operation, owns the transient evidence text and its associated opaque target context. If refresh makes that target stale or unavailable, the text remains retained and cannot be submitted against another target; any later submission still requires a fresh valid handle, review, and confirmation. The pane calls the applicable refresh when a Claims sub-tab is entered and on explicit Refresh/Retry. There is no timer, polling, subscription, or notification.

`reviewSubmission` validates/normalizes evidence and returns a read-only `SubmissionReview` containing only the opaque target handle, approved safe summary, and normalized evidence. It creates no Claim and performs no write. The JavaFX pane presents this value in an explicit confirmation dialog. Cancellation returns to the editable field unchanged.

On confirmation, `submit` performs a fresh report/link/claim read, resolves the handle against the original selected directional pair, checks that the durable link still exists, both reports are present, the first is LOST, the second is FOUND, and the LOST Reporter ID equals the bound Student user ID. It does not rerun or require the matching score. It creates a millisecond Instant and tries at most three UUIDs from the injected supplier; an `ID_COLLISION` consumes only another UUID, not another time. Exhaustion is an internal safe failure. The repository then atomically rechecks Claim locks/closures.

On `CREATED`, the committed Claim is selected in My claims, the draft clears, and Available matches is reloaded. A post-commit view-load failure must not rewrite committed success as failure: the committed claim/status is shown, and the affected list becomes unavailable with Retry. `OWN_ACTIVE_CLAIM` opens/directs to that Student's existing Claim. `BLOCKED` reports only that the match is no longer available and refreshes without revealing why. Every non-`CREATED` outcome preserves the evidence text; confirmation cancellation also returns to editing with the text unchanged.

Withdrawal uses only the selected opaque Claim handle, bound Student ID, current Instant, and Claim repository. It deliberately does not load or require reports or links. The repository atomically checks ownership and Pending state. The UI asks for irreversible confirmation before calling it. `CHANGED` shows Withdrawn and refreshes; `ALREADY_TERMINAL` shows the returned current final status; `NOT_AUTHORIZED` reveals no Claim.

### 9.2 Structurally narrow Student projections

`StudentClaimsState` contains independent Available/My-claims availability, typed safe feedback, and these nested immutable projections:

| Projection | Fields exposed |
| --- | --- |
| `AvailableMatchGroup` | LOST item name and ordered available cards / own-active notices; no Report ID or Reporter ID |
| `AvailableMatchCard` | opaque `AvailableMatchHandle`; LOST item name; FOUND item name, category, occurrence date, location |
| `ExistingClaimNotice` | opaque Student Claim handle and privacy-safe direction to My claims; no blocker identity/reason |
| `MyClaimRow` | opaque handle; LOST/FOUND item names or unavailable markers; FOUND report category or unavailable marker; status; submission Instant |
| `StudentClaimDetail` | visible claim reference; own immutable evidence; status; safe report summary with only PRD-SC-004 fields or unavailable markers; submission/terminal Instants; optional Student-visible decision reason |
| `SubmissionReview` | opaque match handle; safe summary; normalized evidence; no IDs or hidden fields |

Opaque handle classes have no public ID accessor and a redacted `toString`. Only the service can resolve them to report/Claim IDs. This prevents a JavaFX cell from accidentally rendering an internal Report ID or using a visible claim reference as authorization.

Available loading reads reports, links, and Claims. It orients each unordered durable link only when one endpoint is the bound Student's LOST report and the other is FOUND; same-reporter FOUND reports are allowed. Missing/misdirected links are omitted. The Claim ledger emits cards only for available targets, own-active notices for that Student's blockers, and nothing for every other blocker.

LOST groups sort by report `createdAt` descending, then LOST UUID string ascending as a reproducible internal tie-break. Cards sort by FOUND occurrence date descending, then FOUND UUID string ascending. The only empty sentence is exactly `No available matches right now.`

My claims filters stored claimant ID before projection and sorts submission time descending, then Claim ID string ascending for an internal tie. A successful report-store load that lacks one referenced report produces unavailable report-derived fields while retaining/selecting the Claim. A whole report-store or Claim-store load failure clears stale rows/detail and shows Unavailable plus Retry; it is not an empty state. These authoritative view changes do not clear the pane's unsaved evidence.

### 9.3 Resolved row category projection source

PRD-ST-002, PRD-DO-003, and PRD-DO-008 incorporate the product-owner clarification that the singular `category` in `MyClaimRow`, `PendingClaimRow`, and `HistoryClaimRow` is the FOUND report's category. The projection reads it from the current canonical FOUND report and shows the approved unavailable marker when that report-derived field is unavailable. It does not compare, reconcile, or assume equality with the LOST report's category, and it does not add a second category field.

This is consistent with `AvailableMatchCard` and the Student safe match summary, for which PRD-SC-004 already identifies the FOUND item's category. No other approved projection is reopened.

Primary requirement support: PRD-SC-001–PRD-SC-009, PRD-ST-001–PRD-ST-006, PRD-PR-001–PRD-PR-003, PRD-FL-001–PRD-FL-004, PRD-NF-003.

## 10. Desk Officer application module and projections

### 10.1 Constructor authority and operations

`OfficerClaimsService` is per-login and requires a DESK_OFFICER `AuthenticatedUser`, Claim repository, read-only Report repository, and Clock. It never retains or sends the officer user ID to Claim persistence.

Operations are:

```text
enter()
refreshPending() / retryPending()
refreshHistory() / retryHistory()
changeHistoryFilter(ALL | APPROVED | REJECTED | WITHDRAWN)
selectPending(OfficerClaimHandle)
selectHistory(OfficerClaimHandle)
reviewDecision(OfficerClaimHandle, APPROVE | REJECT, String rawReason)
approve(DecisionReview) / reject(DecisionReview)
clear()
```

Entry selects Pending review and loads current Claims/reports. Entry, re-entry, inner-tab selection, Refresh, Retry, and authoritative selection reconciliation do not clear or overwrite unsaved decision text. The pane owns that transient text and associates it with the selected opaque Claim handle so it is never silently transferred to another Claim. Pending and history have separate availability, rows, selection, and Retry state. A full store load failure clears affected rows/private detail/actions as required by PRD-FL-002, but preserves the unsaved decision text; a later action still requires a current reviewable Claim and fresh confirmation. A successful load with a missing referenced Item Report keeps the row and retained Claim but marks the report-derived field unavailable.

Selection is read-only. `reviewDecision` validates optional approval or mandatory rejection text before the pane offers confirmation. `DecisionReview` contains an opaque handle, action, claim reference, and normalized optional or required reason; it is redacted.

Immediately before approve/reject, the service reloads Claims and canonical reports, verifies the selected Claim still exists as Pending, and verifies both report IDs are present. The original possible-match link is irrelevant and is not loaded. Missing reports disable and block both decisions while the Claim remains Pending. When current reports exist, the repository transition is the final atomic Pending check. `CHANGED` clears pending selection/detail, shows final-status success, removes the row, and makes the committed Claim available in history. Only this durable successful decision clears its associated reason draft. `ALREADY_TERMINAL` changes nothing, preserves unsaved text, and refreshes/reports the returned current status. Persistence failure preserves reason text and Pending presentation, reports no success, and permits explicit retry.

### 10.2 Structurally narrow officer projections

`OfficerClaimsState` exposes:

| Projection | Fields exposed |
| --- | --- |
| `PendingClaimRow` | opaque handle; claim reference; LOST/FOUND item names or unavailable; FOUND report category or unavailable marker; submission Instant |
| `HistoryClaimRow` | opaque handle; claim reference; LOST/FOUND item names or unavailable; FOUND report category or unavailable marker; terminal status; terminal Instant |
| `OfficerClaimDetail` | reference; claimant user ID; immutable evidence; status; submission/terminal Instants; optional reason; separate optional current LOST and FOUND `ItemReport` values |
| `DecisionReview` | opaque handle; reference; decision kind; normalized reason; no officer identity |

Pending rows contain every and only Pending Claim, sorted submission time ascending then Claim ID string ascending. There is no pending filter. History contains every and only terminal Claim, filtered by exactly All/Approved/ Rejected/Withdrawn, sorted terminal time descending then Claim ID string ascending. Tie-breaks are deterministic implementation details, not additional visible priority.

Full canonical `ItemReport` values appear only inside selected `OfficerClaimDetail`. The queue/history records cannot expose Reporter ID, descriptions, private detail, evidence, claimant ID, or decision reason because those fields do not exist in those projection types. Detail and all nested values have redacted `toString` implementations.

Primary requirement support: PRD-DO-001–PRD-DO-009, PRD-DC-001–PRD-DC-006, PRD-PR-001, PRD-PR-004, PRD-FL-001–PRD-FL-004, PRD-NF-003.

## 11. Report, possible-match, and authentication integration

### Reports

Claims call only `ReportRepository.loadAll`. Application code indexes the complete list by Report ID and rejects a duplicate-ID snapshot as unavailable rather than choosing one. It never calls `insert`, `replace`, or `ItemReport.withStatus`.

At new submission the current reports establish direction, ownership, and availability. After submission, the stored directional IDs remain the Claim target. Current reports are loaded only for projections and officer decision availability; they are never reconstructed from Claim data. A successfully loaded report snapshot missing an ID is distinct from a report-store load failure.

Both existing ReportStatus values remain acceptable because the approved PRD does not restrict Claim eligibility by report review status. Status never becomes a Claim lock or closure.

### Possible-match links

Student discovery and confirmation call only `PossibleMatchRepository.loadAll` and compare `PossibleMatchPair.of(lostId, foundId)`. Matching score/evaluation is never called. Link removal after Claim commit changes only future submission discovery/revalidation. Withdrawal, officer review, decisions, and history do not require a link. No Claim action calls `link` or `unlink`.

### Authentication

Each Claims application module receives the full `AuthenticatedUser` from the existing authenticated-route factory and rejects the wrong role at construction. Student operations use only the bound stable user ID for report ownership, Claim filtering, and withdrawal. Officer operations retain only the fact that the per-login module was constructed for DESK_OFFICER; no deciding identity enters a command or record.

The UI constructs Claims content only inside the matching authenticated route. Logout removes and clears the per-login service/pane. A later login constructs fresh services and reloads durable state. There is no persistent session or new auth role.

Primary requirement support: PRD-SC-002–PRD-SC-003, PRD-ST-002–PRD-ST-005, PRD-DO-005–PRD-DO-006, PRD-DC-001, PRD-PR-001–PRD-PR-004, PRD-IC-001–PRD-IC-002, PRD-NF-004.

## 12. JavaFX design

### 12.1 Student Claims

`StudentClaimsPane` is a BorderPane with fixed inner tabs **Available matches** and **My claims**, Available selected by default.

- Available matches: header with Refresh/Retry and safe feedback; a scrollable group list; approved cards; own-active notices linking to My claims; exact empty sentence; and no IDs/tooltips/accessibility text containing excluded data.
- Submission: opening a card shows the safe directional summary and a multi-line evidence field. Review calls the service validator; a confirmation dialog shows the same safe summary and trimmed evidence. Cancel returns to editing. Durable success clears the field and selects the new My claims detail. Failure retains the field. Leaving/re-entering Claims, changing Claims sub-tabs, Refresh, Retry, unavailable state, and selection reconciliation also retain the field and its target context.
- My claims: Refresh/Retry, newest-first single-select rows, and a scrollable selected detail. Withdrawal appears only for a loaded owned Pending Claim and uses an irreversible confirmation dialog with no reason field.
- Unavailable clears rows/detail and disables actions. A missing individual report uses an unavailable placeholder without hiding retained Claim data.

The existing Report an item and My reports panes are not redesigned.

### 12.2 Desk Officer Claims

`DeskOfficerClaimsPane` is a BorderPane with fixed inner tabs **Pending review** and **Claim history**, Pending review selected by default.

- Pending: oldest-first single-select ListView, no filters, selected-detail ScrollPane, reason TextArea, separate Approve and Reject buttons, Refresh, Retry, feedback, and a clear successful-empty state.
- Detail: Claim facts first, then clearly separated complete read-only LOST and FOUND report groups. Evidence and private report data never appear in row cells. If either report is absent, its group says unavailable, both decision buttons disable, and Retry remains available.
- Decision: rejection validates a required reason before confirmation; approval validates optional text before confirmation. Each dialog identifies the visible claim reference and irreversible final status. Cancel preserves editable text. Success clears selection/detail/reason and makes the Claim immediately reachable in history. Outer/inner tab changes, entry/re-entry, Refresh, Retry, unavailable state, stale outcomes, and selection reconciliation do not clear decision text or apply it to another Claim.
- History: exactly four filter toggles, newest-terminal-first rows, read-only selected detail, and current-report unavailable placeholders without copied report reconstruction.

Both panes follow current programmatic JavaFX conventions: render guards while replacing lists/restoring selection, custom cells cleared on reuse, plain text Labels/TextAreas, scoped `claims.css`, synchronous service calls, and no background thread or listener surviving logout.

Primary requirement support: PRD-SC-001, PRD-SC-003–PRD-SC-009, PRD-ST-001–PRD-ST-006, PRD-DO-001–PRD-DO-009, PRD-DC-001–PRD-DC-005, PRD-FL-002–PRD-FL-004.

## 13. Composition, refresh, and logout lifecycle

`ClaimWorkspaceFactory` is the production composition module. One instance is created with the application-lifetime report, link, and Claim repositories, `Clock.systemUTC()`, and `UUID::randomUUID`. It creates fresh per-login Student or Desk Officer services/panes and returns a neutral `WorkspaceFeature` value:

```text
Node content
Runnable onEnter
BooleanSupplier hasUnsavedText
Runnable clearSessionState
```

`WorkspaceFeature` belongs to the neutral `base.workspace` package. Developer 2 owns that package placement through package architecture and role navigation. It contains no authentication or Claim type. The package has only this value and `SessionView`; it is a small real seam used by both role workspaces, not a feature framework.

The Student report workspace receives the feature, appends its Node as the third non-closable **Claims** tab, and calls `onEnter` whenever that tab becomes selected. The officer workspace appends the feature after **Possible matches** and does the same. Claims panes call the relevant subview refresh on inner-tab entry and explicit Refresh/Retry. Every entry callback is authoritative data refresh only. It does not own, reset, or replace evidence/reason controls or their transient target association.

`SessionView` is a two-method neutral interface implemented by the two top-level role workspaces:

```text
boolean hasUnsavedText()
void clearSessionState()
```

They delegate only to the Claims feature. `AuthenticationPane` consults this interface before logout. If false, it clears and logs out normally. If true, it shows one confirmation warning. Cancel performs neither clear nor logout and leaves the same view/text. Confirm calls `clearSessionState` before coordinator logout and login rendering.

Claims `clear` empties lists, selections, private report details, confirmation state, feedback, and text fields, and releases service snapshots. It performs no storage call and is invoked only after confirmed logout. Draft evidence/reason exists only in JavaFX controls and is never restored after a later login. Durable Claims are unaffected. Outside confirmed logout, only a durably successful submission may clear its evidence and only a durably successful officer decision may clear its associated reason. Confirmation cancellation, navigation, entry/re-entry, refresh/retry, load failure, unavailable state, stale outcomes, and scene attachment/detachment do not clear draft text. Any additional discard action would require an upstream product decision and is not designed here.

Primary requirement support: PRD-SC-001, PRD-DO-001, PRD-FL-003–PRD-FL-005, PRD-IC-004, PRD-AC-X06.

## 14. Claim IDs, references, clocks, and UTC display

Internal identity is a random UUID wrapped by `ClaimId`, generated from an injected `Supplier<UUID>`. The repository detects duplicate Claim IDs before any eligibility write. The Student submission module retries with at most three supplied UUIDs for the same confirmed action and same submission time; exhaustion produces safe internal failure and no Claim. Deterministic suppliers make this behaviour testable.

The user-visible reference is a pure one-to-one rendering of the persisted Claim ID:

```text
CLM- + 32 uppercase hexadecimal UUID digits without hyphens
```

For example, UUID `00000000-0000-0000-0000-000000000001` displays as `CLM-00000000000000000000000000000001`. It is not separately stored and cannot drift or collide independently. It appears only in approved details and officer rows; it is never an authorization credential.

Both services receive a `Clock`. Each state-changing command samples once and truncates to milliseconds. Persistence uses exact `Instant` strings with three fractional digits. UI formatting occurs only in `ClaimTimeFormatter`, using a fixed UTC-zone formatter such as `uuuu-MM-dd HH:mm:ss.SSS UTC`; it never uses the machine default zone or a relative label. Projections retain `Instant`, so formatting does not alter stored facts.

Primary requirement support: PRD-LC-001, PRD-LC-007, PRD-PR-005, PRD-NF-001, PRD-NF-003.

## 15. Failure taxonomy and privacy-safe UI mapping

Application state uses typed outcomes, never exception messages.

| Category | Meaning | UI handling | Classification |
| --- | --- | --- | --- |
| Successful empty | Authoritative load succeeded with no applicable rows | Approved empty copy; no Retry | Normal |
| Validation | Evidence/reason violates section 4.3 | Field-specific fixed feedback; preserve text; no confirmation/write | User-correctable |
| Ineligible/stale submission | Link/report/direction/ownership or Claim eligibility changed | No write; refresh; generic unavailable guidance or own-Claim direction | User-correctable only by choosing current state |
| Authorization | Wrong route/role or withdrawal claimant mismatch | Deny/clear unsafe selection; no other Claim data | Terminal for that action; internal route issue if reachable |
| Stale terminal action | Claim already Approved/Rejected/Withdrawn | No write; report returned status and refresh | Terminal for that action |
| Missing canonical report | Successful report load lacks a referenced ID | Retain history; unavailable derived fields; disable officer decisions; withdrawal still allowed | Retryable if report is restored; otherwise retained condition |
| Claim/report/link load failure | Store could not be read or was corrupt/unsupported | Clear affected stale rows/detail; preserve unsaved evidence/reason and target association; Unavailable + Retry; never empty | Retryable after external recovery |
| Claim write failure/size limit | Candidate did not commit | Preserve last confirmed state and draft; safe failure + explicit retry; no success | Retryable except persistent capacity/configuration fault |
| Exhausted ID collisions / invalid internal snapshot | Contract/runtime invariant could not be satisfied | Fixed generic failure, no mutation; no internals | Internal/log-only category without sensitive logging |

Logs, exceptions, object strings, screenshots, tests, docs, and handoffs must not contain evidence, decision reasons, private report fields, another Student's identity/Claim, unsafe input, credentials, paths, JSON excerpts, or raw causes. No Claim code logs domain values. Manual/test data is synthetic.

Primary requirement support: PRD-SC-006–PRD-SC-009, PRD-ST-002–PRD-ST-006, PRD-DO-002–PRD-DO-009, PRD-DC-001–PRD-DC-005, PRD-PR-003–PRD-PR-006, PRD-FL-001–PRD-FL-003, PRD-AC-X03–PRD-AC-X04.

## 16. State and sequence design

### 16.1 Claim lifecycle

```text
                         approve(valid optional reason, current reports)
                       +-----------------------------------------------> Approved
                       |
Pending review --------+ reject(required valid reason, current reports) -> Rejected
                       |
                       + withdraw(own Claim; reports/link not required) -> Withdrawn

Approved / Rejected / Withdrawn -- any terminal action --> unchanged current status

Only the successful durable replacement crosses an arrow.
Viewing, selection, refresh, retry, confirmation cancellation, and failed writes
do not cross an arrow.
```

### 16.2 Successful Student submission

```text
Student UI      Student service     Report repo   Link repo    Claim repo / file
    | confirm review  |                  |            |               |
    |---------------->| loadAll -------->|            |               |
    |                 |<-- reports ------|            |               |
    |                 | loadAll --------------------->|               |
    |                 |<-- durable links -------------|               |
    |                 | load current Claims ------------------------->|
    |                 |<-- snapshot / ledger -------------------------|
    |                 | validate ownership, direction, link, text; generate ID/time
    |                 | submit(Pending Claim) ----------------------->|
    |                 |           synchronized read/check/append/encode/stage/force/move
    |                 |<-- CREATED only after commit -----------------|
    |<-- success; open My claims; clear draft; refresh Available ------|

Any stale/blocked/read/write failure creates no Claim and no derived lock.
```

### 16.3 Student withdrawal

```text
Student UI      Student service                    Claim repo / file
    | confirm          |                                  |
    |----------------->| withdraw(id, bound user, time) -->|
    |                  |        synchronized current read  |
    |                  |        owner + Pending check      |
    |                  |        replace with Withdrawn     |
    |                  |        stage / force / atomic move|
    |                  |<-- CHANGED + committed Claim ------|
    |<-- Withdrawn; locks now derive as released; refresh --|

No report or link read is required. A competing terminal result returns the
already durable current terminal Claim and changes nothing.
```

### 16.4 Desk Officer approval or rejection

```text
Officer UI      Officer service       Report repo        Claim repo / file
    | confirm          |                   |                    |
    |----------------->| loadAll ---------->|                    |
    |                  |<-- current reports-|                    |
    |                  | require both endpoints present          |
    |                  | approve/reject(id, reason, time) ------>|
    |                  |              synchronized Pending check |
    |                  |              immutable terminal copy    |
    |                  |              stage / force / atomic move|
    |                  |<-- CHANGED only after commit ------------|
    |<-- clear pending detail; success; show in history ----------|

Missing current report stops before Claim mutation. Link state is not read.
```

### 16.5 Competing or stale terminal actions

```text
Action A             shared Claim repository             Action B
   | approve/reject/withdraw  |                              |
   |------------------------->| synchronized current read    |
   |                          | Pending -> terminal + commit |
   |<-------------------------| CHANGED                      |
   |                          |<--------- terminal command ---|
   |                          | synchronized current read     |
   |                          | sees terminal; no write       |
   |                          |---------- ALREADY_TERMINAL --->|

Exactly one terminal replacement commits. Both callers can report the same
authoritative final status; the later caller never claims success.
```

### 16.6 Persistence boundary

```text
repository command
   -> bounded read target
   -> strict decode + full ledger validation
   -> conditional immutable candidate
   -> bounded canonical encode
   -> write unique sibling temp
   -> force file bytes + close
   -> atomic replace target                 [commit point]
   -> return success

Before commit: old target/absence and its derived locks remain authoritative.
After commit: new target and its derived locks/closures remain authoritative.
```

## 17. Testability design (not Requirements-to-Tests)

The proposed public seams are part of this design for owner review. No test file, test inventory, test ID, or Requirements-to-Tests mapping is created now.

| Seam | Why it is a real seam |
| --- | --- |
| Claim / ClaimId / ClaimLedger public domain interfaces | Lifecycle, invariant, reference, and derived lock/closure behaviour can be observed without JavaFX or storage internals. |
| ClaimRepository with real `JsonClaimRepository` at `@TempDir` | Complete durable submission/transition/restart behaviour and shared-instance competition are observable through the same interface used by production. |
| Package-local `ClaimStoreFiles` adapter | Filesystem behaviour genuinely varies and deterministic stage/force/move/read faults cannot be produced portably through normal NIO. Real filesystem tests remain the default. |
| StudentClaimsService returned role-specific state | Authorization, discovery, safe projections, revalidation, ordering, failure/refresh, submission, and withdrawal are observable without inspecting private implementation. |
| OfficerClaimsService returned role-specific state | Queue/history projections, selected verification, report availability, reasons, first-terminal-wins, and safe recovery are observable without JavaFX internals. |
| Injected `Clock` and `Supplier<UUID>` | Time and randomness genuinely vary; fixed inputs make exact UTC times, collisions, and references deterministic. |
| Existing repository interfaces with small scripted adapters | Deterministic report/link disappearance and safe checked failures are true cross-module seams. No production deletion or fault toggle is added. |
| JavaFX/manual/source review | Exact tab placement, confirmations, draft preservation across entry/re-entry/sub-tab/refresh/retry/failure, wrapping, selected-only private content, long text, small-window usability, and logout warning are presentation facts not requiring a new JavaFX dependency. |

Tests later approved by Requirements-to-Tests must use behaviour through these interfaces, real temporary Claim storage where practical, independent literal expected values/bytes, and synthetic data. They must not mock Claim internals, inspect private fields, add a raw `saveAll`, or read application `data/`. Implementation should proceed in vertical behaviour slices rather than writing a horizontal imagined suite, but exact tests and evidence belong to the next planning stage.

## 18. Consequential technical decisions

| Decision | Alternatives considered | Why this fits the repository | Consequences / trade-offs |
| --- | --- | --- | --- |
| One strict versioned `claims.json` whole-store document | Put Claims in report JSON; extend link file; database; event log | Matches existing local persistence and avoids forbidden contract changes/dependencies | Simple restart and atomic replacement; fixed bound eventually limits new writes but never deletes history |
| Derive locks/closures from retained Claims | Persist separate lock table or booleans on reports/claims | One source of truth gives atomic lifecycle plus eligibility consequence | O(number of Claims) scans, proportionate to local data; no independently queryable lock store |
| Explicit atomic repository commands | `loadAll` + public `saveAll`; generic transaction callback; generic workflow engine | Small deep interface hides full-file RMW and makes first-writer-wins enforceable | Repository knows Claim transition vocabulary; deliberately not reusable as a generic store |
| Immutable Claim copies with transition methods | Mutable entity setters; status-only writes | Matches immutable ItemReport style and prevents partial lifecycle state | Complete record replacement per transition |
| UUID identity and derived full reference | Sequential counter; separate short random reference; hash | Existing project uses UUID and injected suppliers; full derivation has no second collision domain | Visible reference is long but stable and deterministic; no counter file |
| Millisecond `Instant`, fixed UTC display | LocalDateTime; default-zone display; relative time | Matches existing persisted time precision and approved UTC requirement | UI is explicit and deterministic; no localization in Sprint 3 |
| Two role-specific application modules/projections | One broad Claims service/state; UI filters a full domain object | Structurally enforces privacy and follows per-view service conventions | Some orchestration duplication, intentionally preferable to a broad leaky interface |
| Opaque presentation handles | Expose Report/Claim IDs in every row; use row index or visible reference | Lets UI select without gaining fields it must not render | Handles are snapshot-local and must be revalidated on every mutation |
| Strict dedicated hand-written codec pattern | New JSON dependency; reuse inaccessible report codec; Java serialization | No dependency authorization and schema is small; consistent with existing stores | More implementation code; format/recovery verification remains important |
| Shared-instance `synchronized` consistency | Distributed/file locks; database transaction; optimistic version file | Smallest mechanism matching one-process JavaFX app and existing pattern | No multi-process/external-writer guarantee; limitation is explicit |
| Neutral `base.workspace` lifecycle seam | Types under `auth.ui`; Claims-specific imports in report UI; parallel application shell; recursive Node inspection | No existing neutral package exists; a two-type package supports both role tabs/logout without coupling report UI to auth presentation or Claims | Three exact cross-owner integration edits remain approval-gated; this is not a generic feature framework |

## 19. Exact proposed file plan

Paths are repository-relative. No file in this section is created or modified by this TDD except this TDD itself.

### A. Developer 2-owned new production files

| Path | Layer | Responsibility / major dependencies |
| --- | --- | --- |
| `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/claim/model/ClaimId.java` | Domain | UUID value, exact reference rendering, redacted string; JDK only |
| `.../claim/model/ClaimStatus.java` | Domain | Four stored/display statuses and terminal predicate |
| `.../claim/model/ClaimValidationException.java` | Domain | Safe field-specific text/invariant validation failure |
| `.../claim/model/ClaimTextPolicy.java` | Domain | One 500-code-point trimming/control/line-break policy |
| `.../claim/model/Claim.java` | Domain | Immutable retained facts, restore, and three terminal transitions |
| `.../claim/model/ClaimLedger.java` | Domain | Derived locks/closures, submission outcome, store-wide validation |
| `.../claim/persistence/ClaimRepository.java` | Persistence seam | Load plus explicit atomic submit/withdraw/approve/reject contracts and nested typed results |
| `.../claim/persistence/ClaimStoreException.java` | Persistence | Fixed privacy-safe checked failure reasons |
| `.../claim/persistence/JsonClaimRepository.java` | Persistence | Synchronized read/check/write operations for one Claim path |
| `.../claim/persistence/ClaimStoreJsonCodec.java` | Persistence | Package-local strict version-one codec and bound-aware encoding |
| `.../claim/persistence/ClaimStoreFiles.java` | Persistence internal seam | Package-local bounded read and atomic-replace interface |
| `.../claim/persistence/NioClaimStoreFiles.java` | Persistence adapter | NOFOLLOW bounded read, forced sibling write, atomic replace, cleanup |
| `.../claim/persistence/StoreFileFailure.java` | Persistence internal | Package-local ACCESS / OVER_LIMIT filesystem failure |
| `.../claim/application/StudentClaimsState.java` | Application/projection | Student availability, feedback, opaque handles, and the three Student projection families |
| `.../claim/application/StudentClaimsService.java` | Application | Bound-Student discovery, review, submission, tracking, withdrawal, refresh |
| `.../claim/application/OfficerClaimsState.java` | Application/projection | Officer availability, filters, feedback, opaque handles, rows and selected detail |
| `.../claim/application/OfficerClaimsService.java` | Application | Bound-officer queue/history, selected report retrieval, decisions, refresh |
| `.../claim/bootstrap/ClaimWorkspaceFactory.java` | Composition | Application-lifetime dependencies to fresh per-login role features |
| `.../claim/ui/StudentClaimsPane.java` | UI | Student sub-tabs, cards, draft/review/confirmation, detail, withdrawal |
| `.../claim/ui/DeskOfficerClaimsPane.java` | UI | Pending/history lists, verification detail, reason/confirmation/actions |
| `.../claim/ui/ClaimTimeFormatter.java` | UI | One fixed explicitly-labelled UTC event formatter |
| `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/workspace/WorkspaceFeature.java` | Neutral workspace composition seam | Node/authoritative-entry/draft/clear callbacks for one injected role feature; no auth UI or Claim dependency |
| `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/workspace/SessionView.java` | Neutral workspace session seam | Logout draft/clear interface for role roots; no auth UI or Claim dependency |
| `src/main/resources/io/github/cs32272610mp2xcode/finderskeepers/claim/ui/claims.css` | UI resource | Claims-scoped selectors only |

### B. Developer 2-owned existing files expected to change later

| Path | Reason | Smallest expected change |
| --- | --- | --- |
| `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/auth/ui/AuthenticationPane.java` | Officer factory needs authenticated identity; logout must warn/clear Claim drafts | Store officer content as `Function<AuthenticatedUser, Node>` while preserving Supplier overloads; centralize logout to consult `SessionView`, confirm if needed, clear, then call coordinator logout |
| `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/review/ui/DeskOfficerWorkspacePane.java` | Add approved third officer tab and session lifecycle delegation | Preserve first two tabs/order; accept one neutral Claims `WorkspaceFeature`; append Claims; call onEnter on selection; implement `SessionView` by delegation |

### C. Shared / Developer 1-owned files requiring later explicit authorization

| Path | Owner | Required smallest change |
| --- | --- | --- |
| `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/FindersKeepersApp.java` | Developer 1 / shared composition | Construct one `JsonClaimRepository(Path.of("data", "claims.json"))` and one Claim workspace factory with existing repositories, UTC clock and UUID supplier; pass authenticated Claim features into the existing Student/officer workspace factories. Preserve paths, scene/stage, smoke test, and unrelated wiring. |
| `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/report/bootstrap/StudentReportWorkspaceFactory.java` | Developer 1 | Add an overload accepting `Function<AuthenticatedUser, WorkspaceFeature>`; keep existing overloads; create the feature after validating Student identity and pass it to the home pane. Do not change report service/controller composition. |
| `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/report/ui/StudentReportHomePane.java` | Developer 1 | Add an overload accepting the neutral feature; retain Report an item/My reports unchanged; append non-closable Claims, call its entry callback when selected, and implement `SessionView` by delegating draft/clear only. |
| `.gitignore` | Shared/repository-level; `AGENTS.md` assigns no explicit Developer owner | Add only `/data/claims.json` and `/data/.claim-store-*.tmp`; keep this repository-level edit behind later implementation authorization |

No exact test files are fixed here because Requirements-to-Tests is the next separately authorized artifact. Later Claim tests belong under corresponding `src/test/java/.../claim/...` packages. Later guide files are also excluded from this authorization.

## 20. Cross-Owner Integration Requests

These are design requests for later owner approval, not authorized edits.

### Request 1 — application composition

1. **Exact file:** `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/FindersKeepersApp.java`
2. **Current owner:** Developer 1 / shared application composition.
3. **Why required:** only the entry point creates application-lifetime local repositories and injects both authenticated role destinations. One shared Claim repository instance is required for the supported synchronization boundary.
4. **Smallest sufficient change:** the construction/wiring described in file plan C; no scene, stage, auth, report, matching, smoke, or release change.
5. **Why Developer 2 code cannot avoid it cleanly:** constructing repositories inside panes would create multiple unsynchronized instances and hide runtime paths; a parallel shell would violate the mission.
6. **Could Developer 1 make it instead?** Yes, and may do so from the approved factory interface.

### Request 2 — Student workspace factory extension

1. **Exact file:** `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/report/bootstrap/StudentReportWorkspaceFactory.java`
2. **Current owner:** Developer 1.
3. **Why required:** it privately owns correct report-controller composition and is the existing authenticated Student destination factory.
4. **Smallest sufficient change:** one backwards-compatible overload accepting the neutral Claims feature factory and forwarding that feature to the home pane; existing overloads/behaviour remain.
5. **Why Developer 2 code cannot avoid it cleanly:** duplicating its private composition would shadow the Student report workflow; wrapping its two-tab result would not produce the approved three peer tabs.
6. **Could Developer 1 make it instead?** Yes; this is preferred if ownership is kept strict.

### Request 3 — Student third tab and logout delegation

1. **Exact file:** `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/report/ui/StudentReportHomePane.java`
2. **Current owner:** Developer 1.
3. **Why required:** it owns the existing top-level Student TabPane and exact ordering.
4. **Smallest sufficient change:** backwards-compatible constructor/overload, append Claims after My reports, invoke the neutral entry callback, and delegate the neutral session lifecycle. Do not alter existing tab content or refresh behaviour.
5. **Why Developer 2 code cannot avoid it cleanly:** nesting or replacing this pane would change the approved top-level navigation or duplicate report UI.
6. **Could Developer 1 make it instead?** Yes; the neutral feature value means no Claim domain knowledge is required.

The Developer 2-owned `AuthenticationPane` and `DeskOfficerWorkspacePane` changes do not require cross-owner approval, but still require later production implementation authorization. `AGENTS.md` does not assign `.gitignore` to Developer 2; its two proposed Claim runtime exclusions are therefore classified as a shared/repository-level edit and also remain behind later implementation authorization. The new neutral `base.workspace` package is within Developer 2's package-architecture and role-navigation ownership, while each import/edit inside a Developer 1 file remains covered by Requests 2 and 3. No report model, report status, report persistence, matching model, or possible-match persistence change is requested.

## 21. Delivery order after later gates

After this TDD approval, the remaining gates are a separate Requirements-to-Tests approval, implementation authorization, and each cross-owner authorization. Only after those later gates:

1. implement Claim identity/text/lifecycle and ledger behind their public interfaces;
2. implement the real temporary-file Claim repository and one atomic Pending submission path, then terminal replacements and fault handling;
3. implement Student projections/load path, then confirmed submission, then tracking/withdrawal;
4. implement officer queue/history projections, then selected verification, approval and rejection;
5. add thin role panes, scoped styling, UTC rendering, confirmations, and logout lifecycle;
6. perform authorized workspace/application composition last, without broadening cross-owner edits; and
7. after separately authorized documentation delivery, update only the Claim-relevant guide sections and interaction evidence.

This ordering describes dependency-aware vertical capability slices, not a Requirements-to-Tests checklist or an authorization to write tests/code.

## 22. Risks and controls

| Risk | Control / accepted limitation |
| --- | --- |
| Claim state leaks into reports or links | Dedicated domain/file; Claim services use report/link reads only; regression evidence later checks unchanged values/bytes |
| Duplicate/conflicting Claims | One synchronized conditional repository interface; derived locks/closures checked inside mutation |
| Lifecycle and lock state diverge | No persisted locks; both derive from the one committed Claim record |
| Stale UI decides twice | Repository rechecks current Pending state; first durable terminal action wins; later result returns current terminal Claim |
| Link disappears after submission | Link used only for new-submission revalidation; no link dependency in withdrawal/review/history |
| Missing reports cause copied history | Claim stores IDs only; projections show unavailable; decisions disable; history remains |
| Privacy leakage from broad state or object dumps | Role-specific narrow records, opaque handles, selected-only full reports, redacted strings, fixed errors |
| Post-commit refresh fails | Mutation result remains authoritative success; affected view becomes unavailable/retryable instead of reversing success |
| Fixed store bound meets indefinite retention | No deletion or truncation; capacity failure is truthful and preserves all Claims. A later scale requirement would need a separately approved store revision. |
| Multi-process writer loses updates | Explicitly outside guarantee; one shared application-lifetime repository instance is mandatory |
| Atomic move/platform durability differs | No fallback; safe failure and existing target preservation; no directory-fsync/power-loss overclaim |
| Student integration expands report ownership | Neutral feature seam and three exact gated files; no report domain/service change |
| Navigation or refresh silently loses a draft | Authoritative entry/refresh never owns text; panes retain target-bound drafts; only durable corresponding success or completed confirmed logout clears them |
| Logout leaves private content | Central warning, cancellation semantics, confirmed session clear, fresh per-login modules; no unconfirmed detach clear |
| Manual codec complexity | Small fixed schema, strict whole-document validation, existing proven pattern, later real-byte/fault evidence |

No new dependency, unresolved technical choice, or unresolved product projection remains. The row-category source is fixed by section 9.3. The main delivery risk is obtaining the three explicit cross-owner integration approvals; this is not a reason to weaken approved behaviour.

## 23. Architectural requirements traceability

This is architectural traceability only. It does not assign test IDs or create the later Requirements-to-Tests evidence map.

| PRD group | Technical realization |
| --- | --- |
| PRD-LC-001–PRD-LC-002 | Sections 4 and 7: exact immutable Claim fields, four states, strict persisted grammar |
| PRD-LC-003–PRD-LC-009 | Sections 5–6 and 8: derived ledger rules plus atomic conditional submit/terminal commands |
| PRD-SC-001 | Sections 12–13 and cross-owner requests 1–3: exact third tab and default inner tab |
| PRD-SC-002–PRD-SC-006 | Sections 9 and 11: durable-link orientation, current report ownership, narrow cards, grouping/order, hidden blockers/own redirect |
| PRD-SC-007–PRD-SC-009 | Sections 4.3, 9, and 16.2: one text policy, review/confirmation, authoritative recheck, durable outcome |
| PRD-ST-001–PRD-ST-004 | Sections 9.2–9.3: claimant filtering, newest order, restricted row/detail, immutable evidence; My-claims row category comes from the FOUND report |
| PRD-ST-005–PRD-ST-006 | Sections 6, 9.1, and 16.3: bound-owner atomic withdrawal independent of reports/links |
| PRD-DO-001–PRD-DO-004 | Sections 9.3, 10, and 12–13: exact tab placement, pending-only oldest queue, restricted rows, single selection; pending-row category comes from the FOUND report |
| PRD-DO-005–PRD-DO-006 | Sections 10–11: selected-only complete current reports and missing-report decision disablement |
| PRD-DO-007–PRD-DO-009 | Sections 9.3, 10.2, and 12.2: terminal-only filtered history, restricted rows, retained detail with current-report optionals; history-row category comes from the FOUND report |
| PRD-DC-001–PRD-DC-004 | Sections 4.3, 10.1, and 12.2: reviewable-current-report gate, reason validation/safety, explicit confirmation |
| PRD-DC-005–PRD-DC-006 | Sections 6, 10.1, and 16.4–16.5: durable final transition and no officer identity field/command |
| PRD-PR-001–PRD-PR-002 | Sections 9–11 and 13: role-validated per-login modules and stable claimant ID enforcement |
| PRD-PR-003–PRD-PR-004 | Sections 9.2, 10.2, and 15: structurally separate role/list/detail projections |
| PRD-PR-005 | Section 14: Instant storage and fixed explicitly-labelled UTC display |
| PRD-PR-006 | Sections 4, 7, 9–10, and 15: redaction, fixed errors, synthetic-only secondary evidence |
| PRD-FL-001–PRD-FL-002 | Sections 7–10 and 15: atomic target preservation, typed failures, independent unavailable/empty state |
| PRD-FL-003–PRD-FL-004 | Sections 9–13: entry/explicit/post-action refresh and no background channel |
| PRD-FL-005 | Sections 9–10 and 13: entry/refresh draft preservation, warning, logout-cancel preservation, confirmed session clear, no later-login draft restoration |
| PRD-IC-001–PRD-IC-003 | Sections 3, 7, and 11: dedicated store, read-only integrations, retained no-delete Claims |
| PRD-IC-004 | Sections 13, 19–20: exact minimal integration surfaces and explicit later gates |
| PRD-NF-001 | Sections 7–8 and 14: complete durable document, stable IDs/times, restart reconstruction |
| PRD-NF-002 | Sections 5–6, 8, and 16.5: shared-instance serialized conditional mutations |
| PRD-NF-003 | Sections 9.2 and 10.2: explicit primary comparators and stable internal UUID tie-breaks |
| PRD-NF-004 | Sections 2–3, 11, and 19–20: reuse without shadow types or silent contract changes |

Acceptance-level coverage is plausible for PRD-AC-S01–PRD-AC-S10 through sections 4, 9, 12, 13, 15, and 16; PRD-AC-D01–PRD-AC-D09 through sections 4, 6, 9.3, 10, 12, 15, and 16; and PRD-AC-X01–PRD-AC-X06 through sections 3, 5–8, 11, 13–17. Exact evidence and test IDs remain deliberately deferred to RTT.

## 24. Completeness audit and approval gate

The design was rechecked against the complete approved PRD:

- every requirement group has a concrete module/interface realization;
- the four observable statuses and every transition remain unchanged;
- active locks, approval closures, rejection closure, withdrawal release, and same-pair resubmission are enforceable from durable retained facts;
- duplicate/competing submissions and terminal actions serialize at one real repository consistency boundary;
- success is not returned before atomic Claim-store commit;
- report-store v1 and possible-match format/semantics are untouched;
- no report, link, score, or private report field is copied into a Claim;
- link removal affects only later submission eligibility;
- current-report absence is distinct from Claim history and store-load failure;
- Student and officer row/detail projections are structurally separate;
- no deciding-officer identity is accepted or retained;
- event times persist and display as explicitly labelled UTC;
- entry/re-entry, sub-tab navigation, refresh/retry, unavailable/load failure, stale outcomes, and scene attachment changes do not clear unsaved evidence or decision text;
- only durable corresponding action success or completed confirmed logout clears its approved transient state; logout cancellation preserves the view;
- the My claims, pending-row, and history-row category is consistently sourced from the FOUND report without assuming LOST/FOUND category equality;
- the neutral workspace seam no longer places Developer 1 report UI under an authentication-UI dependency;
- `.gitignore` is classified as shared/repository-level because no explicit Developer 2 ownership is recorded;
- all required cross-owner files and smallest changes are explicit and remain unauthorized;
- no test suite, Requirements-to-Tests artifact, production code, dependency, guide, branch, commit, push, pull request, or merge is included.

No upstream contradiction, infeasible requirement, or unresolved upstream ambiguity remains. The category-source clarification is reflected consistently in the PRD and section 9.3. The repository owner approved this TDD on 2026-09-22; all other approved architecture, persistence, locking, concurrency, privacy, lifecycle, and integration decisions remain unchanged. This approval covers only the technical design and proposed public seams. It does not authorize Requirements-to-Tests, implementation, tests, cross-owner edits, documentation delivery, branch creation, commits, pushes, pull requests, release changes, or merging.
