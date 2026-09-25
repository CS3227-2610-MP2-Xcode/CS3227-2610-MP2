# Finders Keepers Developer Guide

## Scope

This guide describes the implemented project baseline, local authentication,
report persistence, Student report submission and history, Desk Officer report
review, deterministic possible matching, and the Sprint 3 Claims and
Verification feature for a primary-school lost-and-found application. The
application shell opens authentication at startup and routes each authenticated
role to a fresh workspace over shared application-lifetime repositories.

Claims are implemented end to end: Students can submit, track, and withdraw
ownership claims from durable possible-match links, while Desk Officers can
review, approve, reject, and inspect retained claim history. Appointment,
collection, handover, return, and other Sprint 4 workflows are not implemented.

## Development prerequisites

- Java 25
- The included project Gradle Wrapper

## Current design

The current source tree keeps application startup separate from the shared report foundation:

- `Launcher` is the plain Java entry point used by Gradle and the packaged JAR.
- `FindersKeepersApp` owns the JavaFX lifecycle and creates the authentication scene.
- `AppMetadata` is the single source of truth for the application name and version.
- `app.css` keeps presentation rules separate from the Java scene construction.
- `report` contains the canonical immutable `ItemReport`, its persisted enums,
  creation request, validation types, and storage-format constraints.
- `report.persistence` exposes `ReportRepository` and its strict, ordered, versioned JSON implementation.
- `review.application` owns submitted-queue state, filtering, selection,
  transition policy, stale-target reconciliation, and privacy-safe outcomes.
- `review.ui` renders the separate Desk Officer queue and complete read-only
  details view, and contains the tabbed Desk Officer workspace composition.
- `matching.model` owns symmetric pair identity, the fixed four-rule evidence,
  and deterministic suggestion generation.
- `matching.persistence` stores only canonical report-ID pairs in a separate,
  strict versioned file.
- `matching.application` owns authoritative loading, section partitioning,
  comparison state, Link/Unlink rechecks, and privacy-safe outcomes.
- `matching.ui` renders suggestions, linked possible matches, read-only
  comparison, reasons, actions, and explicit empty/unavailable states.
- `claim.model` owns Claim identity, lifecycle, text validation, and the
  derived lock/closure rules enforced by `ClaimLedger`.
- `claim.persistence` exposes atomic Claim commands and a strict, bounded,
  versioned JSON implementation independent of report and link storage.
- `claim.application` contains role-bound Student and Desk Officer workflows,
  immutable role-narrow projections, and the approved-report endpoint adapter.
- `claim.ui` renders the Student and Desk Officer Claims subworkspaces.
- `claim.bootstrap` creates fresh per-login Claim services and views over the
  shared repositories.
- `workspace` supplies neutral authenticated-feature and session-lifecycle
  seams used to compose Claims without coupling authentication to Claim UI.

`Launcher` delegates to `FindersKeepersApp`, which composes the authentication
coordinator for `data/demo-users.json` and one application-lifetime repository
for each durable domain: `JsonReportRepository` at `data/reports.json`,
`FilePossibleMatchRepository` at `data/possible-match-links.txt`, and
`JsonClaimRepository` at `data/claims.json`. `ClaimWorkspaceFactory` receives
all three repositories, a UTC clock, and a Claim UUID supplier. Authentication
constructs only the workspace selected by the stored role; every login gets
fresh stateful services and views over those shared repositories.

The Student workspace keeps **Report an item** and **My reports**, then appends
**Claims**. The Desk Officer workspace keeps **Report review** and **Possible
matches**, then appends **Claims**. `WorkspaceFeature` carries a feature's JavaFX
node, authoritative entry callback, unsaved-text query, and session-clear
callback. `SessionView` lets authentication warn before discarding unsaved
Claim text and clear per-login state on confirmed logout without changing
durable storage.

The repository stores all canonical report fields and preserves insertion order
across reconstruction. It performs bounded strict reads and all-or-nothing
atomic replacement with no unsafe fallback. The application-owned
`data/reports.json` remains plaintext and is excluded from Git by the exact
root-relative ignore rule `/data/reports.json`. The Claim and relationship
stores and their temporary files have equally precise root-relative ignore
rules. See
[S1-D2-02 Report Storage Format](features/S1-D2-02/StorageFormat.md) for the
public boundary, JSON contract, failure behavior, privacy limits, and operating
assumptions.

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
  The JavaFX pane accepts an `AuthenticationCoordinator` plus opaque Student
  and Desk Officer view factories; it does not know concrete report,
  persistence, review, or hashing types.
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

## Desk Officer review components

The review workflow is separated from authentication, the canonical domain,
and persistence:

- `DeskOfficerReviewService` is one stateful, plain-Java application service
  per mounted Desk Officer view. It loads ordered reports through
  `ReportRepository`, retains the active `SUBMITTED` and `UNDER_REVIEW`
  reports, excludes both endpoints of every Approved Claim when Claims are
  integrated, applies the exact All/Lost/Found filters in memory, owns visible
  selection, and returns an immutable `ReviewQueueState`.
- `ReviewQueueFilter` is a non-persisted UI filter. Lost and Found delegate to
  canonical `ReportType` values and display labels; no stored token or report
  enum is duplicated.
- `ReviewQueueState` contains canonical `ItemReport` values rather than a
  second report-shaped DTO. It distinguishes a ready queue from unavailable
  storage and supplies exact empty, feedback, and Retry presentation state.
- `DeskOfficerReviewPane` renders the state, a five-value public queue row, and
  all eleven selected canonical values. The private identifying detail appears
  only in the selected authenticated details section. Review-specific styling
  is scoped to `review.css`.

The current read-only flow is:

```text
authenticated DESK_OFFICER route
        -> lazy DeskOfficerReviewPane and DeskOfficerReviewService
        -> shared ReportRepository.loadAll()
        -> ApprovedClaimReportService.loadApprovedReportIds()
        -> ordered active snapshot minus approved Claim endpoints
        -> active type filter
        -> selected canonical ItemReport details
```

The former **Start review** report-status mutation is no longer part of this
view. Selecting and refreshing are read-only. Approved Claims do not mutate or
delete reports; `ApprovedClaimReportService` exposes only the immutable set of
their LOST and FOUND endpoint IDs so the active report queue can hide them.

Load failure is distinct from an empty queue. A report-store or Claim-store
failure clears report values and selection, disables filters and selection,
and exposes Retry. Retry preserves the active filter. The UI receives only
fixed contextual copy; paths, JSON, persistence reason names, exception text,
and report values are not included in error messages.

`AuthenticationPane` receives an opaque Student view function and a lazy Desk
Officer `Supplier<? extends Node>`. It invokes only the factory for the
authenticated role. Logout clears the coordinator session and replaces the
entire authenticated subtree, so a later Desk Officer login gets a fresh
service/view and a new authoritative queue load over the same shared
repositories used by the Student workspace.

## Officer possible-match components

The S2 matching module consumes canonical `ItemReport` values and never parses
report JSON or creates a competing report model. `DeterministicMatcher` is a
pure concrete policy implementation. `OfficerMatchingService` combines its
evidence with `ReportRepository` and `PossibleMatchRepository`, then returns
immutable `MatchingWorkspaceState` values. Only an explicitly successful Link
or Unlink changes relationship state; no matching operation writes a report or
changes `ReportStatus`.

### Deterministic policy

Candidate identity is one unordered pair of distinct Report IDs containing one
LOST and one FOUND report. Both `SUBMITTED` and `UNDER_REVIEW` are eligible.
The approved rule table is:

| Criterion | Rule | Points | Gate |
| --- | --- | ---: | --- |
| Category | Exact canonical `ItemCategory` equality; `OTHER` is ordinary | 40 | Required |
| Item-name keywords | At least one exact shared normalized token | 20 | No |
| Location | Complete normalized-location equality | 30 | No |
| Occurrence date | FOUND is 0–7 calendar days after LOST, inclusive | 10 | Required |

Text is lowercased with `Locale.ROOT`. Every maximal run of code points that
are not Unicode letters or digits is a separator. Keyword tokens shorter than
two code points are discarded and duplicates are removed; location keeps
one-code-point runs and rejoins runs with one ordinary space. There is no
stemming, singular/plural conversion, accent folding, Unicode normalization,
substring, fuzzy, probabilistic, ML, or LLM comparison.

The total is exactly the visible 40/20/30/10 component sum. A pair qualifies
only when both gates pass and the total is at least 70. Rule points are evidence
for a possible match, not confidence or ownership proof. Each positive
component produces its corresponding reason, all component outcomes remain
available in the selected comparison, and shared keywords are sorted.

Suggestions use this total order:

```text
rule points descending
-> canonical first Report ID string ascending
-> canonical second Report ID string ascending
```

`PossibleMatchPair` orders the two UUIDs by their canonical lowercase string,
so A-B and B-A have identical value identity. The same pair tuple resolves
every score tie independently of repository, hash, or input iteration order.
Deterministic rules were chosen because every suggestion must be reproducible
and every point attributable to one of the four approved report fields.

### Relationship storage

`FilePossibleMatchRepository` stores links separately from report-store v1 at
`data/possible-match-links.txt`. The file contains no report copy, Reporter ID,
private detail, score, officer identity, or timestamp. Its canonical UTF-8
shape is:

```text
FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS 1
10000000-0000-0000-0000-000000000001 20000000-0000-0000-0000-000000000002
```

Input is bounded to 16 MiB and strictly validates the header, UUID form,
self-pairs, duplicates, and reverse duplicates. Missing storage means an empty
new store; zero bytes or malformed storage is unavailable rather than empty.
Every mutation rereads and validates the complete target, stages canonical
sorted bytes in a sibling temporary file, forces them, and requires atomic
replacement. There is no unsafe non-atomic fallback. A fresh repository
instance observes a successful Link or Unlink. One report may participate in
multiple links, and unknown report IDs remain stored so an officer can remove
a stale relationship.

### Workflow and interaction summary

```text
authenticated DESK_OFFICER route
        -> DeskOfficerWorkspacePane
        -> load canonical reports + possible-match links
        -> deterministic LOST x FOUND evaluation
        -> ordered unlinked suggestions | retained linked relationships
        -> select pair: read-only canonical comparison + four rule outcomes
        -> explicit Link: authoritative report/link recheck -> atomic commit
        -> linked possible matches
        -> explicit Unlink: authoritative relationship recheck -> atomic removal
```

The suggestion and linked sections are independent. A linked pair remains
reachable for Unlink when it stops qualifying or an endpoint disappears; a
missing side is never reconstructed. Link rechecks current report existence,
type, status, gates, and threshold. Repeated or reversed Link/Unlink requests
are idempotent no-ops with truthful feedback. A write failure retains clearly
labelled last-known state and reports no success.

Rows contain only type, item name, category, occurrence date, location, rule
points, and brief positive reasons. Reporter ID and private identifying detail
exist only in the selected authenticated comparison, with public and private
descriptions separated. Whole-workspace load failure clears rows and private
selection, disables mutations, and exposes Retry. Successful empty results
distinguish no eligible pair, no qualifying pair, all qualifiers already
linked, and no linked relationships. Detaching the view on logout clears its
service snapshots and controls without unlinking durable relationships.

`DeskOfficerWorkspacePane` composes the review queue as the default tab, the
possible-match view as the second non-closable tab, and the injected Claims
feature as the third.
`FindersKeepersApp` creates one application-lifetime
`FilePossibleMatchRepository` at `data/possible-match-links.txt` and its lazy
Desk Officer supplier constructs this workspace with the shared repositories.
Claims consume links read-only: submitting or deciding a Claim does not add or
remove a possible-match relationship.

## Claims and Verification

Sprint 3 implements Claims as a separate durable domain under
`finderskeepers.claim`. A Claim records a human ownership assertion and a human
decision; it is not a report status, possible-match link, collection record, or
appointment.

### Domain and lifecycle

`Claim` is immutable and retains exactly these values:

```text
ClaimId claimId()
String claimantUserId()
UUID lostReportId()
UUID foundReportId()
String ownershipEvidence()
ClaimStatus status()
Instant submittedAt()
Optional<Instant> terminalAt()
Optional<String> decisionReason()
```

`ClaimId` wraps a UUID and derives the visible reference `CLM-` followed by the
UUID's 32 uppercase hexadecimal digits. `ClaimStatus` has exactly
`PENDING_REVIEW`, `APPROVED`, `REJECTED`, and `WITHDRAWN`. Only Pending review
is active. The other statuses are terminal, and a terminal Claim cannot
transition again. Pending Claims have no terminal fields; Approved Claims
require a terminal time and may have a reason; Rejected Claims require both;
Withdrawn Claims require a terminal time and no reason. Event times use
millisecond precision.

Ownership evidence and decision reasons are normalized through
`ClaimTextPolicy`. Required text is stripped, must contain a visible code point,
may contain approved line breaks, rejects control/format characters, and is
limited to 500 Unicode code points. Approval reasons are optional; rejection
reasons are required. Claim and result `toString()` methods are redacted.

`ClaimLedger` validates each complete retained snapshot and derives the Claim
locks and closures:

- at most one pending Claim may use a LOST endpoint or FOUND endpoint;
- a pending or Approved Claim blocks either endpoint for every competing Claim;
- approval permanently closes both endpoints;
- rejection permanently closes that FOUND endpoint only to the same claimant;
- withdrawal releases its active locks; and
- the same claimant and pair can be submitted again only after the previous
  Claim was Withdrawn and current reports/link remain eligible.

These are Claim-domain restrictions only. No Claim command mutates an
`ItemReport`, `ReportStatus`, or `PossibleMatchPair`.

### Claim persistence

`ClaimRepository` is the atomic boundary for `loadAll`, `submit`, `withdraw`,
`approve`, and `reject`. Commands return typed outcomes rather than relying on
exception text. Submission distinguishes created, Claim-ID collision, the
same Student's active blocker, and a non-disclosable competing block. Terminal
commands distinguish changed, already terminal, missing, and unauthorized.
The repository returns no Claim value for a blocked submission or unauthorized
withdrawal, preventing disclosure of another Student's data.

`JsonClaimRepository` stores the complete retained ledger in stable submission
order at `data/claims.json`. The strict UTF-8 version-one document contains
`schemaVersion` and a `claims` array. Each Claim object has exactly
`claimId`, `claimantUserId`, `lostReportId`, `foundReportId`,
`ownershipEvidence`, `status`, `submittedAt`, `terminalAt`, and
`decisionReason`. UUIDs, status names, timestamps, members, lifecycle
invariants, and ledger consistency are validated on every read. Missing storage
means an empty ledger; malformed, oversized, unsupported-version, or
contradictory storage is unavailable rather than silently replaced.

Reads and generated documents are bounded to 16 MiB. Each synchronized mutation
rereads authoritative storage, applies its conditional transition, validates
the complete candidate ledger, and atomically replaces the target through a
sibling temporary file. There is no unsafe non-atomic fallback. A failed write
does not report success or replace the target. Synchronization serializes
commands issued through the shared repository instance; cross-process locking
is not provided.

### Student Claims service and projection

`StudentClaimsService` is constructed for exactly one authenticated `STUDENT`
and receives the shared Claim, report, and possible-match repositories. Its
two independently loaded views are **Available matches** and **My claims**:

- Available matches begin only from durable Desk Officer-created links. The
  service orients each link LOST-to-FOUND, requires the authenticated Student
  to own the LOST report, and evaluates current Claim locks and closures.
- Cards expose only the Student's LOST item name and the FOUND item's name,
  category, occurrence date, and location. Report IDs, reporter identities,
  descriptions, private identifying details, matching scores/reasons, and
  blocker identities are absent. Opaque handles have no public ID accessors.
- Another Student's blocker or a closure silently removes the card. The current
  Student's own active blocker yields only a safe direction to that Claim.
- Submission validates evidence, creates a read-only confirmation value, then
  rereads reports, links, and Claims before committing. It verifies current
  report existence, types, LOST ownership, and link existence. Claim UUID
  collisions are retried at most three times.
- My claims filters by exact authenticated claimant ID, orders newest first,
  and exposes restricted rows. Selected detail adds the visible Claim
  reference, the Student's immutable evidence, status, event times, decision
  reason, and only the approved safe current-report summary.
- Withdrawal is owner-bound and available only for Pending review. It does not
  require the original reports or link because the durable Claim is the
  authorization target.

The Student state distinguishes `NOT_LOADED`, `READY`, and `UNAVAILABLE` per
subview. Entry, Refresh, and Retry perform authoritative loads. A dependency
failure clears the affected rows instead of presenting a false empty result.
Submission and withdrawal success are shown only after the atomic Claim command
returns. If the post-commit refresh fails, the committed Claim and truthful
success remain visible through a minimal retained projection so a successful
operation is not misreported as failed.

### Desk Officer Claims service and projection

`OfficerClaimsService` accepts only an authenticated `DESK_OFFICER`. It exposes
**Pending review** and read-only **Claim history** subviews:

- Pending Claims are ordered oldest first. Rows contain an opaque handle,
  visible reference, current LOST/FOUND names when available, FOUND category,
  and submission time.
- History contains terminal Claims ordered by newest terminal time first and
  supports All, Approved, Rejected, and Withdrawn filters.
- Selected detail is the only officer projection that contains claimant ID,
  submitted evidence, decision data, and complete current canonical reports.
  Missing reports remain explicit optionals and disable a decision.
- Approval permits an optional normalized reason. Rejection requires one.
  Both actions require a validated confirmation value and reread current Claim
  and report state before issuing the atomic repository command.
- The first durable approval, rejection, or withdrawal wins. Repeated or stale
  decisions return the authoritative terminal Claim and make no second write.

Pending and history availability are independent. Load failures clear only the
affected view and offer Retry. A decision read/write failure retains truthful
state and reports no success; a stale selection, missing Claim, missing report,
or already-terminal Claim receives a distinct privacy-safe outcome.

### Privacy and composition boundaries

Student and Desk Officer rows, details, handles, and feedback are different
types rather than one broad DTO. Student-facing state and JavaFX never receive
another claimant's Claim or a complete report. Desk Officer queue rows do not
receive evidence or private report detail; those values appear only after
authenticated selection. Fixed feedback enums keep paths, JSON, exception text,
and hidden Claim data out of JavaFX messages.

`ClaimWorkspaceFactory` creates fresh per-login role-bound services and panes
over one application-lifetime Claim repository. It supplies the Student Claim
feature to `StudentReportWorkspaceFactory` and a `DeskOfficerClaimFeature` to
`DeskOfficerWorkspacePane`. The latter bundles the Claims workspace with
`ApprovedClaimReportService`, whose only output is the set of report endpoints
closed by Approved Claims. The report-review service uses that narrow adapter
to hide those reports without depending on Claim rows, evidence, claimant
identity, or persistence details.

Claims UI state and unsaved evidence/decision text are cleared on confirmed
logout; cancelled logout preserves them. Durable Claims are never deleted.
An Approved Claim is the upstream ownership-verification result that a future
Sprint 4 workflow may use when determining appointment eligibility; it does not
itself create an appointment or make appointment behavior available.
The implemented feature does not provide appointment eligibility, booking,
collection, handover, return, report closure, notifications, background
polling, or an appointment-specific Approved-Claim API. The proposed Sprint 4
handoff is documented separately in
[Sprint 3 Claims to Sprint 4 Appointment Handover](features/S3-D2-01/Sprint4AppointmentHandover.md).

## Student report-submission components

The Student submission slice is implemented as a UI/application boundary and
is reachable through the authenticated Student workspace. Its components are:

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

`StudentReportWorkspaceFactory` owns Student composition. Its path-based API
constructs a repository for standalone callers, while its repository-based API
accepts the application-lifetime `ReportRepository`. Both APIs adapt `insert`
failures into `ReportSubmissionException` and pass one repository to submission
and history services. `AuthenticationPane` receives a Student-view factory so
authentication remains responsible only for session state, role routing, and
logout. The authenticated `userId()` is passed to both controllers; the
username is display-only.

## Student report-history and search components

S2-D1-01 implements a personal history use case without expanding the shared
`ReportRepository` interface:

- `report.application.StudentReportHistoryService` calls
  `ReportRepository.loadAll()`, selects the exact authenticated `reporterId`,
  searches item names and public descriptions, and returns newest-first rows.
- Search strips surrounding query whitespace, lowercases with `Locale.ROOT`,
  and performs case-insensitive substring matching. Either the item name or
  public description may match. A blank query returns the full personal list.
- `report.application.ReportHistoryEntry` is the privacy-safe projection passed
  toward JavaFX. It deliberately contains no reporter identity, location, or
  private identifying detail.
- `report.application.ReportHistorySearchResult` distinguishes a Student with
  no reports from a Student whose current search has no matches.
- `report.application.ReportHistoryException` retains the repository failure
  for diagnostics while keeping it outside presentation text.
- `report.ui.StudentReportHistoryController` binds one authenticated Student to
  the use case and maps results into normal, no-history, no-match, or safe
  load-failure presentation states.
- `report.ui.ReportHistoryViewState` owns those immutable presentation states.
- `report.ui.StudentReportHistoryPane` renders the search controls, readable
  report statuses, public report summaries, and empty/failure feedback.
- `report.ui.StudentReportHomePane` places submission and personal history in
  fixed **Report an item** and **My reports** tabs. Selecting **My reports**
  refreshes storage before showing the current history.

The history flow is:

```text
AuthenticatedUser.userId()
        -> StudentReportHistoryController.search(query)
        -> StudentReportHistoryService.search(reporterId, query)
        -> ReportRepository.loadAll()
        -> exact reporter filtering
        -> item-name OR public-description matching
        -> newest-first ReportHistoryEntry rows
        -> ReportHistoryViewState
        -> StudentReportHistoryPane
```

Search never reads `privateIdentifyingDetail`, and the JavaFX layer never
receives that field. `ReportStoreException` becomes a safe load-failure state;
it is not presented as an empty history. The service reloads the repository on
each search or clear action so newly submitted reports and persisted status
changes can appear without restarting the application.

The startup path now constructs one `JsonReportRepository` at
`data/reports.json`, adapts `repository.insert(report)` into the submission
service while translating `ReportStoreException` to
`ReportSubmissionException`, and passes the same repository to
`StudentReportHistoryService`. `AuthenticationPane` receives the injected
Student-view factory and passes `AuthenticatedUser.userId()` to both Student
controllers and the display-only username to `StudentReportHomePane`.

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
- Desk Officer review tests cover active `SUBMITTED`/`UNDER_REVIEW` membership
  and order, exact filters, global and filtered empty states, selection,
  immutable state, Approved-Claim endpoint
  exclusion, Claim-store failure, Retry context, and read-only behavior. Real
  persistence evidence uses JUnit temporary directories.
- Possible-match tests cover symmetric pair identity, exact category/keyword/
  location/date rules, score thresholds, reasons, Unicode and locale boundaries,
  repeated and shuffled determinism, stable UUID tie-breaking, strict link-file
  parsing, fresh-instance Link/Unlink, non-exclusive links, storage failures,
  stale/missing pairs, privacy-safe state, and unchanged canonical report bytes.
  Every real matching persistence test uses a JUnit temporary directory.
- Student report-history tests cover exact ownership filtering, newest-first
  ordering, deterministic ties, blank queries, item-name and public-description
  matches, case and whitespace normalization, empty histories, empty searches,
  private-detail exclusion, storage failures, immutable presentation state,
  and reconstruction through a real temporary JSON repository.
- Student workspace-composition tests verify that submission and history share
  one canonical repository and that write failures cross the composition
  boundary as `ReportSubmissionException` without real user data.
- Claim model and ledger tests cover lifecycle invariants, text normalization,
  redaction, active locks, approval and rejection closures, withdrawal release,
  duplicate/contradictory ledgers, and first-terminal-action behavior.
- Claim persistence tests cover strict version-one JSON, canonical UUIDs and
  millisecond UTC timestamps, Unicode handling, size bounds, atomic replacement
  failures, fresh-instance reconstruction, conditional command outcomes, and
  shared-instance concurrent submissions and terminal actions. They use only
  JUnit temporary directories.
- Student Claims tests cover durable-link discovery, role and ownership checks,
  safe projections, grouping and ordering, authoritative submission rechecks,
  UUID collision retries, own-Claim redirection, blocker privacy, tracking,
  withdrawal, unavailable/retry states, and post-commit refresh failures.
- Desk Officer Claims tests cover pending/history ordering and filters,
  selected-only complete report detail, reason validation, missing-report
  decision blocking, approval/rejection durability, repeated-decision
  prevention, stale selections, failure mapping, and retry behavior.
- Claim privacy and composition tests verify structurally narrow role
  projections, absent private values in rows and feedback, fresh per-login
  workspaces over shared repositories, Approved-Claim report-queue exclusion,
  and session-state clearing without durable deletion.
- Java compilation enables all lint warnings and treats warnings as errors.
- Checkstyle runs against production and test sources.
- Javadoc warnings fail the build.
- JaCoCo writes HTML and XML coverage reports. A project-wide numerical
  coverage threshold is not yet configured.

## Release packaging

The Gradle `fatJar` task packages application classes, CSS, JavaFX, and its native libraries. `verifyUniversalJar` checks for the Windows x64, Linux x64, and macOS ARM64 JavaFX launchers and native Glass libraries before `release` copies the JAR into `release/`.

The JAR also supports `--smoke-test`, which opens the application and exits automatically. This argument is for automated verification rather than normal use.

## Continuous integration

`.github/workflows/ci.yml` runs for pushes and pull requests on Ubuntu, macOS, and Windows. Each job installs Java 25, validates the Gradle Wrapper, runs `clean check release`, smoke-tests the exact release JAR, and uploads it as a workflow artifact. The workflow can only be confirmed on GitHub after the first push; its equivalent build and smoke checks can be run locally beforehand.

## Planned areas

- Student report submission and personal report history/status display —
  implemented and reachable through the shared Student workspace composition.
- Desk Officer deterministic possible matching, durable relationships, and
  application-shell composition — implemented.
- Claims and Verification — implemented for Student submission, tracking, and
  withdrawal; Desk Officer review, approval, rejection, and history; durable
  atomic storage; role-narrow privacy projections; and active report-queue
  exclusion after approval.
- Appointment booking, collection, handover, return, notifications, and report
  closure remain future work. No Sprint 4 behavior should be inferred from an
  Approved Claim beyond the separately documented handoff contract.
- Repository construction and startup wiring — implemented through the
  approved report, possible-match, and Claim paths with one
  application-lifetime repository instance per durable domain.
- Richer category and status vocabularies, submission validation, and report creation — to be extended through the shared canonical model without introducing a competing report type.
- Additional role-specific JavaFX workflows beyond the delivered Student,
  review, possible-match, and Claims modules remain future work.
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

The repository imports these canonical types directly from the `report`
package. It declares no parallel report-domain type or duplicate enum. The JSON
decoder passes the eleven decoded values to the clockless
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
