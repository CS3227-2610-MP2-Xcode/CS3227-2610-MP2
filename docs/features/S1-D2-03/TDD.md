# S1-D2-03 Build Desk Officer Review Queue Technical Design

- Status: Approved
- Draft date: 2026-09-21
- Approver: Repository owner
- Approval date: 2026-09-21
- Feature: Build Desk Officer Review Queue
- Workstream: Developer 2
- Source PRD: `docs/features/S1-D2-03/PRD.md` (Approved 2026-09-21)
- Mission brief: `docs/mission-briefs/S1-D2-03-officer-review.md` (Approved 2026-09-20)
- Decision ledger: `docs/features/S1-D2-03/GrillingDecisions.md` (Complete 2026-09-20)
- Reconciled repository baseline: `a1383ce2cbe6386b67cc675608a10df9ea663261`
- Implementation authorization: Not granted
- Branch creation authorization: Not granted

## Status and authority

This document is the approved Technical Design Document for S1-D2-03. It
defines how to implement the approved product behavior; its approval does not
authorize requirements-to-tests work, tests, production implementation, branch
creation, commits, publishing, or merging.

The governing order is:

1. `AGENTS.md`
2. the repository owner's direction to reconcile against the current repository
3. the approved S1-D2-03 Mission Brief
4. the approved S1-D2-03 PRD
5. the current canonical report and S1-D2-02 persistence contracts
6. this TDD

The PRD is authoritative for observable behavior. This TDD may select concrete
classes, interfaces, state representations, control flow, and test seams, but
it may not change the approved queue fields, filters, messages, transition
semantics, retry behavior, privacy boundary, or acceptance outcomes.

## Decision labels

This document uses these labels to keep evidence and proposals distinct:

- **Existing behavior** — verified in the current repository.
- **Approved requirement** — fixed by the Mission Brief or approved PRD.
- **Proposed technical change** — the design to approve in this TDD.
- **Assumption** — a bounded condition inherited from the current architecture.
- **Unresolved decision** — a choice that still requires owner judgment.

## Continuity and reconciliation

### Current repository state

**Existing behavior:** The repository is currently on
`royden/feat-report-persistence` at `a1383ce`. The S1-D2-03 Mission Brief,
decision ledger, and PRD are untracked planning files. `evals/` is unrelated
and remains outside this work.

**Existing behavior:** No partial
`docs/features/S1-D2-03/TDD.md` existed when this session resumed. This approved
design therefore records the decisions already resolved during the interrupted
TDD work rather than replacing an existing repository document.

**Existing behavior:** The current baseline contains Developer 1's canonical
report domain, Student submission slice, Developer 2's JSON persistence, local
authentication, role routing, and placeholder authenticated destinations. It
contains no officer-review package or queue UI.

### Reconciled stale facts

| Earlier fact or possible assumption | Current repository evidence | Smallest correction in this design |
| --- | --- | --- |
| The Mission Brief described `ItemReport` as a Java record with an all-fields constructor. | `ItemReport` is a final immutable class with `create(...)`, `restore(...)`, eleven accessors, and `withStatus(...)`. | Consume the current class directly and use `withStatus(UNDER_REVIEW)`; do not construct or duplicate a report model. |
| The Mission Brief named `35d5322` as the persistence baseline. | The reconciled PRD and current `HEAD` identify `a1383ce`, after the canonical-domain integration. | Design against `a1383ce`; retain the older hash only as historical Mission Brief context. |
| Developer 2 once had a temporary report model. | Only Developer 1's canonical `finderskeepers.report.ItemReport` exists in production source. | Add no wrapper, DTO, shadow model, or duplicate enum. |
| Report review might require persistence changes. | `ReportRepository` already exposes ordered `loadAll()` and complete `replace(...)`; `JsonReportRepository` already provides safe durable replacement. | Keep query and transition policy in the review application module; do not extend persistence. |
| Role navigation might require a new router or session abstraction. | `AuthenticationCoordinator` already derives `LOGIN`, `STUDENT`, and `DESK_OFFICER`; `AuthenticationPane` already mounts role content and owns logout. | Inject only a lazy Desk Officer destination into the existing pane. |

### Merge-reconciliation amendment (2026-09-21)

Developer 1's subsequently merged Student report-history work made the Student
submission/history workspace reachable from `AuthenticationPane` through
`StudentReportWorkspaceFactory.create(Path)`. The earlier assumption that the
Student destination was still a placeholder and required no integration change
therefore became stale during the merge from `main`.

The repository owner explicitly approved one narrow cross-owner correction for
this merge: add `StudentReportWorkspaceFactory.create(ReportRepository)` and
have the existing path-based overload delegate through it. `FindersKeepersApp`
constructs the single application-lifetime `JsonReportRepository` and passes
that same instance to the Student factory and each freshly created Desk Officer
review service. This is dependency injection of the existing persistence
contract, not a new abstraction or workflow redesign.

The amendment preserves Student-visible submission and history behavior, role
separation, the canonical report domain, and the approved one-shared-repository
architecture. It supersedes only the statements below that describe the
Student route as a placeholder or list Student composition as entirely
unchanged; the approved product requirements and test strategy remain intact.

No approved product requirement contradicts the current code. No assumption in
this design depends on the removed temporary report implementation.

### Planning-worktree constraint

**Existing behavior:** The approved planning artifacts still record branch
creation as unauthorized, and the current branch remains the persistence
branch.

**Proposed technical change:** This approved TDD remains planning-only. Before
any test or production implementation, the repository owner must separately
authorize creation of the S1-D2-03 feature branch from the reconciled baseline.
No S1-D2-03 implementation may be committed as persistence work.

## Technical objective

**Approved requirement:** Give an authenticated Desk Officer a deterministic,
persistence-backed submitted-report queue with exactly All, Lost, and Found
filters; complete read-only details; and one explicit, durable
`SUBMITTED -> UNDER_REVIEW` action.

**Proposed technical change:** Add one plain-Java, stateful review application
service and one separate JavaFX review pane. The application service will hide
queue derivation, filtering, selection invariants, authoritative transition
rechecks, persistence calls, retry context, and privacy-safe outcome mapping
behind a small interface. The JavaFX pane will render immutable service state
and forward user actions. Authentication will mount a fresh pane lazily only
for the existing `DESK_OFFICER` route.

The design deliberately does not add a generalized workflow engine, transition
framework, repository query interface, second report model, generic router,
background refresh process, or new dependency.

## Goals and exclusions

### Design goals

- Keep all workflow policy local to one Developer 2-owned application module.
- Reuse the canonical `ItemReport`, enum display labels, date/time formatters,
  `ReportRepository`, `JsonReportRepository`, authentication session, and
  current programmatic JavaFX style.
- Make success impossible before `ReportRepository.replace(...)` returns.
- Keep filtering and selection deterministic and testable without JavaFX.
- Keep raw persistence categories, paths, exceptions, and report values out of
  user-facing failure messages.
- Preserve the existing Student destination and all persistence behavior.
- Use one shared production repository instance while creating fresh
  officer-view state after every Desk Officer login.

### Exclusions

This design does not add Student wiring, report creation, report editing,
deletion, management of reports already under review, reverse transitions,
search, sorting controls, pagination, live updates, automatic retry,
multi-process coordination, migration, encryption, a UI testing framework, or
changes to the canonical domain or version-one storage format.

## Relevant existing architecture

### Canonical report domain

**Existing behavior:** `finderskeepers.report.ItemReport` is an immutable final
value with all eleven approved accessors. `withStatus(...)` returns a complete
copy and explicitly leaves transition legality to the calling workflow.
Value equality covers all fields and `toString()` is redacted.

**Existing behavior:** `ReportType`, `ItemCategory`, and `ReportStatus` expose
canonical `displayName()` values. `ReportConstraints` exposes the ISO local-date
formatter and the exact three-digit UTC creation-time formatter. These are the
only formatting sources the review view needs.

**Approved requirement:** The review feature consumes those types directly and
must not modify or duplicate them.

### Persistence module

**Existing behavior:** `ReportRepository` is the public persistence seam:

```java
List<ItemReport> loadAll() throws ReportStoreException;
void insert(ItemReport report) throws ReportStoreException;
void replace(UUID targetId, ItemReport replacement) throws ReportStoreException;
```

`loadAll()` returns an unmodifiable snapshot in stable insertion order.
`replace(...)` rereads the complete store, retains the target's position, and
durably replaces the whole document. The repository protects Report ID,
Reporter ID, and Created At but deliberately does not enforce application
status policy or status-only updates.

**Existing behavior:** A missing store loads as an empty list without creating
storage. Invalid, inaccessible, non-regular, unsafe-to-replace, unencodable, or
over-limit storage produces a typed `ReportStoreException`. The implementation
uses a forced same-directory temporary file and one atomic move with no unsafe
fallback.

**Assumption:** Supported production use is one process and one shared
`JsonReportRepository` instance. Each method is synchronized on that instance,
but `loadAll()` followed by `replace()` is not one conditional atomic operation.
Coordination with other repository instances or processes remains unsupported.

### Authentication and session

**Existing behavior:** `AuthenticationCoordinator` retains one non-secret
`AuthenticatedUser`, derives the current `ApplicationRoute` from its stored
role, and clears the user and validation message on logout.

**Existing behavior:** `AuthenticationPane` authenticates, displays one of the
two role destinations, and replaces the authenticated subtree with the login
form after logout. The Student route now mounts its submission/history
workspace.

**Approved requirement:** Only `DESK_OFFICER` may mount the review workflow.
Student-visible behavior remains unchanged by S1-D2-03. Logout removes the
review view and its selected private detail; the next officer login performs a
fresh load.

### UI and testing patterns

**Existing behavior:** JavaFX views are built programmatically and receive
plain-Java collaborators. The Student form uses an injected controller and an
immutable presentation state, while tests exercise controller/state behavior
without importing JavaFX.

**Existing behavior:** The build contains JUnit Jupiter but no JavaFX UI-test
framework. Existing storage tests use `@TempDir`; deterministic persistence
faults are introduced only at an established seam.

## Proposed module structure and responsibilities

```text
finderskeepers.review
├── application
│   ├── DeskOfficerReviewService.java
│   ├── ReviewQueueFilter.java
│   └── ReviewQueueState.java
└── ui
    └── DeskOfficerReviewPane.java

resources/.../finderskeepers/review/ui
└── review.css
```

### `DeskOfficerReviewService`

**Proposed technical change:** This is the single review application service
and the primary automated test seam. It is a final, plain-Java class constructed
with the existing `ReportRepository`. One service instance belongs to one
mounted Desk Officer view.

Its proposed small interface is:

```java
public final class DeskOfficerReviewService {
    public DeskOfficerReviewService(ReportRepository repository);

    public ReviewQueueState enter();
    public ReviewQueueState retry();
    public ReviewQueueState changeFilter(ReviewQueueFilter filter);
    public ReviewQueueState select(UUID reportId);
    public ReviewQueueState startReview();
}
```

The service owns:

- the last successfully loaded ordered submitted snapshot;
- the active filter;
- the selected visible Report ID;
- load availability, safe feedback, and stale-reconciliation retry context;
- in-memory filtering and selection validation;
- the authoritative pre-mutation recheck;
- enforcement of `SUBMITTED -> UNDER_REVIEW` only;
- construction of the status-only replacement;
- repository error translation; and
- construction of immutable `ReviewQueueState` outputs.

The service has no JavaFX dependency and no user, credential, path, JSON, or
filesystem knowledge.

### `ReviewQueueFilter`

**Proposed technical change:** This non-persisted enum contains exactly `ALL`,
`LOST`, and `FOUND`. `LOST` and `FOUND` delegate matching and display labels to
the corresponding canonical `ReportType`; the enum does not duplicate stored
tokens or report types.

It preserves order by applying a predicate to the ordered submitted snapshot.
It performs no sorting or mutation.

### `ReviewQueueState`

**Proposed technical change:** This immutable presentation state contains or
derives:

- ready versus load-unavailable availability;
- the active `ReviewQueueFilter`;
- an unmodifiable ordered list of visible canonical `ItemReport` values;
- an optional selected canonical `ItemReport`;
- the exact inline queue/empty message, when applicable;
- the exact neutral or load-error details message;
- an optional typed feedback message for success, stale target, or transition
  storage failure;
- whether **Start review** is enabled; and
- whether **Retry** is visible.

The implementation may retain the complete submitted snapshot privately to
distinguish global-empty from filtered-empty state. It must not expose a mutable
collection.

`ReviewQueueState` is presentation state, not a report DTO: it contains the
canonical reports and defines no duplicate report fields, enum tokens, or
persistence representation.

State invariants are:

1. Every retained queue report has status `SUBMITTED`.
2. Visible reports are the stable-order filter of that retained snapshot.
3. A selected report is present only when it is visible.
4. Load-unavailable state contains no reports or selection, disables Start
   review, and exposes Retry.
5. Empty state contains no selection and disables Start review.
6. No message contains exception text, a path, JSON, or any report value.

### `DeskOfficerReviewPane`

**Proposed technical change:** This is the only new JavaFX view. It receives a
fresh `DeskOfficerReviewService`, calls `enter()` once when constructed, renders
the returned state, and forwards filter, selection, Start review, and Retry
events back to the service.

The pane owns control construction and formatting only. It does not read JSON,
call the repository, decide transition legality, cache a second report list, or
inspect persistence reasons.

During construction, the pane loads `review.css` from its own class-relative
resource URL and attaches it to the pane's stylesheet list. The application
shell does not load or know about the review stylesheet.

### Authentication destination factory

**Proposed technical change:** Extend `AuthenticationPane` with a lazy
`Supplier<? extends Node>` for Desk Officer content alongside the merged Student
view function. The pane invokes the officer supplier only after successful
authentication when `coordinator.route() == DESK_OFFICER`. It keeps its
existing identity/title and logout chrome and preserves the Student workspace
behavior unchanged.

This uses a JDK functional interface rather than introducing a generalized
route registry or application router. `AuthenticationPane` remains unaware of
report, persistence, and review classes.

### Production composition

**Proposed technical change:** `FindersKeepersApp` creates exactly one:

```text
JsonReportRepository(Path.of("data", "reports.json"))
```

It passes the shared repository to
`StudentReportWorkspaceFactory.create(ReportRepository)` and passes a lazy Desk
Officer factory to `AuthenticationPane`. Each officer-factory invocation
constructs a new `DeskOfficerReviewService` over that same repository and a new
`DeskOfficerReviewPane`. The repository therefore has application lifetime,
while role-specific view state has one authenticated-view lifetime.

Repository construction performs no I/O. The report store is accessed only by
an authenticated role's explicit submission, history, or review operation.

## Components and files likely affected

### New Developer 2-owned files

| File | Responsibility |
| --- | --- |
| `src/main/java/.../finderskeepers/review/application/DeskOfficerReviewService.java` | Queue workflow, state changes, transition validation, persistence mapping |
| `src/main/java/.../finderskeepers/review/application/ReviewQueueFilter.java` | Exact All/Lost/Found predicate |
| `src/main/java/.../finderskeepers/review/application/ReviewQueueState.java` | Immutable, presentation-ready review state and invariants |
| `src/main/java/.../finderskeepers/review/ui/DeskOfficerReviewPane.java` | Queue, filters, details, actions, and rendering |
| `src/main/resources/.../finderskeepers/review/ui/review.css` | Review-scoped visual rules only |
| `src/test/java/.../finderskeepers/review/application/DeskOfficerReviewServiceTest.java` | Plain-Java queue, filter, selection, stale, retry, and error behavior |
| `src/test/java/.../finderskeepers/review/application/DeskOfficerReviewPersistenceTest.java` | Real `JsonReportRepository` and `@TempDir` durability evidence |
| `docs/images/desk-officer-review-queue.png` | Cropped privacy-safe synthetic queue screenshot |

The exact test-class split may combine the two test files if that keeps each
behavior easier to follow. It must not change the agreed public seams.

### Existing files with proposed changes

| File | Smallest proposed change | Ownership note |
| --- | --- | --- |
| `src/main/java/.../auth/ui/AuthenticationPane.java` | Accept and lazily mount opaque Desk Officer content; preserve Student route and existing logout | Developer 2 owns role navigation |
| `src/main/java/.../FindersKeepersApp.java` | Construct one shared report repository and supply the fresh Desk Officer view factory | Developer 1-owned shell; this exact minimal composition change is pre-approved by the Mission Brief |
| `src/main/java/.../report/bootstrap/StudentReportWorkspaceFactory.java` | Add a repository-accepting overload while preserving and delegating the path-based API | Developer 1-owned composition; explicitly approved during merge reconciliation |
| `.gitignore` | Add the precise root-relative rule `/data/reports.json` | Explicit S1-D2-03 deliverable; do not ignore all of `data/` |
| `docs/UserGuide.md` | Add delivered officer steps, states, recovery, and screenshot | Developer 2 documentation |
| `docs/DeveloperGuide.md` | Document review modules, flows, transition policy, path, privacy, and integration | Developer 2 documentation |
| `README.md` | Make only the minimal current-status correction | No broader rewrite |

### Files explicitly not changed

- `ItemReport`, `ReportType`, `ItemCategory`, `ReportStatus`, and
  `ReportConstraints`
- `ReportRepository`, `JsonReportRepository`, its codec, or storage adapters
- Student behavior or composition beyond the approved repository-injection overload
- `AuthenticationCoordinator`, session records, or authentication persistence
- `build.gradle`, dependencies, CI, release configuration, or `Launcher`
- scene dimensions, smoke-test behavior, or application lifecycle
- shared `app.css`; review styling remains in the review-owned stylesheet

Any newly discovered need to change one of these files requires an ownership
and scope checkpoint before implementation continues.

## Application state model

The application service has two top-level availability states.

### Ready

Ready state contains an ordered submitted snapshot, active filter, optional
visible selection, and optional feedback. It derives:

- visible rows from the filter;
- global versus filtered empty copy;
- selected full details; and
- Start review enablement.

### Load unavailable

Load-unavailable state contains no report values or selection, retains the
filter required for the next Retry, disables Start review, and exposes Retry.
The service privately retains whether a successful retry must restore the stale
target message.

The state does not include a loading phase because repository calls are
synchronous and JavaFX event handling is serial. No background task or automatic
state change is introduced.

### Feedback lifecycle

**Proposed technical change:** Feedback is deterministic rather than timer
driven:

- successful review produces `Review started.`;
- stale reconciliation produces
  `This report is no longer available for review.`;
- a Start review storage failure produces
  `Review could not be started because reports are unavailable. Please try again.`;
- a later selection or filter interaction clears prior success, stale, or
  transition-failure feedback;
- another Start review attempt replaces the previous action feedback; and
- logout discards the whole service and state.

No timer, background refresh, or automatic dismissal changes the view.

## Data flow

### Enter the Desk Officer destination

1. `AuthenticationCoordinator` authenticates the account and derives
   `DESK_OFFICER` from the stored role.
2. `AuthenticationPane` invokes the Desk Officer content supplier.
3. The supplier creates a fresh review service and pane over the shared
   repository.
4. The pane calls `service.enter()`.
5. `enter()` resets filter to All, clears selection and feedback, and calls
   `ReportRepository.loadAll()`.
6. On success, the service retains only reports with status `SUBMITTED`, in the
   exact returned order, and returns ready state.
7. On failure, it returns load-unavailable state with
   `Reports are unavailable. Please try again.` and Retry.

A missing store is a successful empty load, not a failure and not a reason to
create storage.

### Apply All, Lost, or Found

1. The view sends the selected `ReviewQueueFilter` to `changeFilter(...)`.
2. The service filters the already loaded submitted snapshot in memory.
3. Relative order is unchanged.
4. If the selected Report ID remains visible, selection and details remain.
5. Otherwise selection clears and the neutral prompt returns.
6. Filtering performs no repository call and cannot change report status.

Filter controls remain visible but are disabled while load is unavailable.

### Select and inspect a report

1. The view passes the clicked visible report's canonical UUID to
   `select(...)`.
2. The service resolves that ID only within the current visible snapshot.
3. A visible match becomes the sole selection and enables Start review.
4. A non-visible ID cannot create a hidden selection; the service clears or
   retains no selection and performs no persistence call.
5. The state returns the selected canonical `ItemReport` for full rendering.

Selection, display, and scrolling are read-only and perform no storage call.

### Start review successfully

1. The pane calls `startReview()` only while state reports an enabled selected
   visible report.
2. The service captures the selected Report ID and calls `loadAll()` to obtain
   the current authoritative snapshot.
3. It locates that UUID using canonical equality.
4. It requires the authoritative report's status to be exactly `SUBMITTED`.
5. It creates `authoritative.withStatus(ReportStatus.UNDER_REVIEW)`.
6. It calls `repository.replace(reportId, replacement)`.
7. Only after `replace(...)` returns does it produce success.
8. The post-success submitted snapshot is derived from the authoritative
   pre-replacement order with the transitioned report excluded. No second read
   is required.
9. The active filter stays selected, selection and details clear, Start review
   disables, and `Review started.` is returned alongside the resulting queue or
   empty state.

Using the authoritative report rather than the earlier selected snapshot means
the replacement preserves the ten current non-status values even if the store
changed before the recheck.

### Reconcile a stale or missing target

If the authoritative recheck cannot find the selected UUID or finds a status
other than `SUBMITTED`:

1. no replacement is attempted;
2. the same successful recheck snapshot becomes the reconciled queue source;
3. the active filter is preserved;
4. selection and details clear;
5. `This report is no longer available for review.` is returned; and
6. the resulting global or filtered empty state is shown when applicable.

If `replace(...)` reports `REPLACEMENT_TARGET_NOT_FOUND` after a successful
recheck, the service performs one new `loadAll()` reconciliation:

- a successful reload produces the same stale-target state; or
- a failed reload produces load-unavailable state, clears all report values,
  preserves the active filter, and exposes Retry.

When Retry later succeeds after that failed stale reconciliation, the service
restores the stale-target message with the reloaded queue. Initial-entry Retry
does not add a stale message.

### Logout and later login

1. AuthenticationPane's existing logout action calls
   `AuthenticationCoordinator.logout()`.
2. It replaces the entire authenticated subtree with the login form.
3. The removed review pane and its per-view service are no longer reachable
   through navigation; selected labels and private detail are absent from the
   visible interface.
4. A later Desk Officer login invokes the supplier again, creates fresh state,
   selects All, and performs a new authoritative load.

There is no cached review Node and no review-owned copy of session identity.

## Status-transition design

### Legal transition

The review application service owns the one legal rule:

```text
authoritative status == SUBMITTED
        + explicit Start review action
        + successful complete replacement
        -> UNDER_REVIEW success
```

The rule is checked before `withStatus(...)` and before persistence mutation.
The domain copy method and repository remain policy-free.

### Invalid transitions

- An authoritative `UNDER_REVIEW` report is stale/unavailable for this queue;
  the service does not call `replace(...)` and does not report success.
- A repeated click after success cannot transition again because success clears
  selection and removes the report from the submitted snapshot.
- A no-selection Start review call is a no-op with no repository access and no
  success. The JavaFX button is also disabled in that state.
- `UNDER_REVIEW -> SUBMITTED` is unrepresentable: the service accepts no target
  status and exposes no reverse or generic transition operation.
- Future statuses do not become valid implicitly; anything other than
  `SUBMITTED` fails the same authoritative guard.

This design intentionally avoids a public `transition(reportId, targetStatus)`
method because it would widen the interface solely to express forbidden or
hypothetical workflows.

### Status-only preservation

The replacement is always produced by calling `withStatus(UNDER_REVIEW)` on the
authoritative current report. The service never reconstructs the report field
by field. This reuses the canonical invariant and guarantees that Report ID,
Reporter ID, type, item name, category, location, occurrence date, both
descriptions, and Created At are unchanged by the service.

## Persistence interactions and durability

### Operation use

- `loadAll()` is used for initial entry, Retry, every Start review recheck, and
  reconciliation after a replacement target disappears.
- `replace(...)` is used only after the authoritative status guard passes.
- `insert(...)` is never used by this feature.
- JSON, paths, codecs, store-size rules, and temporary files remain hidden
  behind `ReportRepository`.

### Write ordering

1. Load and validate the complete authoritative store.
2. Locate the selected target.
3. Validate current status.
4. Create the canonical status-only copy.
5. Invoke complete repository replacement.
6. Update visible service state only after replacement returns.

There is no optimistic UI success and no post-success action that can convert a
durable commit into a reported failure.

### Atomicity threshold

The inherited repository guarantee is the feature's durability threshold: once
`replace(...)` returns, a fresh repository instance can observe one complete
version-one document containing the replacement while the operating system and
storage remain operational. The review service adds no second transaction,
backup, or recovery file.

### Load-plus-replace race assumption

**Assumption:** The JavaFX view issues review actions serially through the one
shared repository instance, and no supported second writer changes the store
between the service's recheck and replacement. The two calls are therefore an
adequate workflow guard for the approved deployment shape, but they are not a
compare-and-set transaction.

If multi-instance or multi-process writers become supported later, the
repository would need a separately approved conditional replacement operation.
This feature must not pre-emptively add one.

## Error handling

The service maps technical storage outcomes by operation context, never by
displaying `ReportStoreException.getMessage()`.

| Technical condition | Service outcome | Visible behavior |
| --- | --- | --- |
| Any `loadAll()` failure during entry or Retry | Load unavailable | No rows/details; Start review disabled; Retry visible; `Reports are unavailable. Please try again.` |
| `loadAll()` failure during Start review before the target can be classified | Transition storage failure | Preserve current row, selection, details, and filter; Start review remains enabled; contextual review-failure copy |
| Recheck target missing or not `SUBMITTED` | Stale target | No mutation; reconcile from the successful snapshot; clear selection; preserve filter; stale-target copy |
| `REPLACEMENT_TARGET_NOT_FOUND` from `replace(...)` | Concurrent stale target | Perform one authoritative reload; show stale state on success or load-unavailable state on reload failure |
| Corrupt/unsupported, I/O/safe-replacement, unencodable/over-limit, or another unexpected checked replacement reason | Transition storage failure | No success; retain current row, selection, details, filter, and enabled action |
| `IMMUTABLE_FIELD_MISMATCH` while using an authoritative `withStatus` copy | Defensive transition storage failure | Treat as an unavailable replacement, expose no technical detail, and retain current state; this is unreachable in the supported writer model |
| Null collaborator or impossible null UI command | Programmer error | Reject through normal argument validation; not a user-facing storage state |

`DUPLICATE_REPORT_ID` is not produced by the replacement path. If a scripted or
future adapter nevertheless returns it, it follows the generic transition
storage-failure mapping rather than leaking a persistence category.

No error path logs, prints, embeds, or returns report values, JSON, file paths,
causes, schema details, or technical failure names.

## Empty-state handling

Ready-state copy is derived in this order:

1. If the complete submitted snapshot is empty, show
   `No submitted reports.`
2. Otherwise, if the visible Lost or Found list is empty, show the applicable
   exact filter-specific sentence.
3. Otherwise, show the ordered rows and no empty message.

Every empty state clears selection, shows
`Select a report to view details.`, and disables Start review. A success or
stale feedback message may appear separately alongside the resulting empty
state as required by the PRD.

Load-unavailable state is not an empty state and must never show empty copy.

## UI and presentation design

### Layout

**Proposed technical change:** `DeskOfficerReviewPane` uses existing JavaFX
controls without a new framework:

- a heading and inline feedback area;
- one `ToggleGroup` containing exactly All, Lost, and Found;
- a `ListView<ItemReport>` with a custom cell for the queue;
- a responsive split or border layout with queue and scrollable details;
- a complete read-only details section;
- an always-present Start review button; and
- a Retry button visible only for load-unavailable state.

The pane must fit the existing scene and minimum window sizes by using managed
layout and scrolling. `FindersKeepersApp` scene dimensions are not changed.

A rendering guard prevents programmatic list/filter updates from recursively
firing user-action handlers. Rendering always replaces controls from one
complete immutable state rather than incrementally guessing at changes.

### Queue row

The custom list cell renders exactly:

1. `reportType().displayName()`
2. `itemName()`
3. `category().displayName()`
4. occurrence date through `ReportConstraints.OCCURRENCE_DATE_FORMAT`
5. `location()`

It does not render Report ID, Reporter ID, either description, status, or
Created At. `ItemReport.toString()` is not used as display copy. Visually
identical rows remain distinct because selection is backed by Report ID.

### Details

The selected canonical report renders all eleven values:

- full `reportId().toString()`;
- stored Reporter ID;
- type display name;
- stored item name;
- category display name;
- stored location;
- formatted ISO local date;
- stored public description;
- stored private identifying detail;
- status display name; and
- `ReportConstraints.formatCreationTime(createdAt())`.

Labels and text containers are read-only and wrap long text. Public description
and private identifying detail use separate labelled sections. The private
section is visibly marked as reserved for Desk Officer verification.

When selection clears or storage becomes unavailable, every value-bearing
control is cleared or removed before the neutral/error state is shown.

### Actions and focus

- Start review is visible at all times and enabled only for a selected visible
  report in ready state.
- Retry is visible and managed only in load-unavailable state.
- Filter toggles reflect the service's active filter after every render.
- A retained selected Report ID is reselected after a filter change that still
  includes it.
- No confirmation dialog, success dialog, automatic retry, or background
  refresh is added.

### Styling and screenshot privacy

Review-specific selectors live in `review.css`, which
`DeskOfficerReviewPane` loads from its class-relative resource during
construction. The selectors do not alter existing login or Student rules, and
no stylesheet change is added to `FindersKeepersApp`. The screenshot is cropped
to the queue area, uses only synthetic rows, and excludes the signed-in
identity, details panel, private identifying detail, credentials, unsafe input,
paths, and technical errors.

## Authentication and session integration

The authentication coordinator remains the single source of route authority.
The review service does not accept a role parameter or duplicate session state.

The lazy content supplier provides three useful guarantees:

1. report storage is not read before Desk Officer authentication;
2. Student login cannot construct or display the review pane; and
3. every later Desk Officer login gets a new view/service and fresh queue load.

AuthenticationPane continues to own username display and logout. The review
pane does not need or display the username. This also lets the required queue
screenshot exclude identity without altering authentication behavior.

## Ownership boundaries

### Developer 2

Developer 2 owns the new review package, state and transition policy, Desk
Officer pane, role-destination mounting, report-repository composition for this
workflow, report-store ignore rule, focused tests, officer documentation, and
manual review evidence.

### Developer 1 and shared contracts

Developer 1 continues to own the canonical report types, Student workflow,
initial shell, build, release, and CI configuration. This design requires no
change to those report types and no Student integration.

The only Developer 1-owned production edit is the already approved minimal
`FindersKeepersApp` composition change. It is limited to repository creation
and the Desk Officer factory argument. It must not alter scene sizing,
lifecycle, smoke-test logic, startup role behavior, or any other shell concern.

Any additional cross-owner need stops at the smallest proposed change and
requires explicit approval.

## Dependencies and integration points

The feature depends only on:

- canonical report domain accessors, `withStatus`, display labels, and
  formatters;
- the existing `ReportRepository` and `JsonReportRepository`;
- existing `ReportStoreException` typed outcomes;
- `AuthenticationCoordinator`, `ApplicationRoute`, and `AuthenticationPane`;
- JavaFX controls already present in the build; and
- JUnit Jupiter and `@TempDir` already present for tests.

No dependency, Gradle, module, schema, or configuration change is proposed.

## Proposed public test seams and testing implications

This approval confirms these seams for the later requirements-to-tests stage.
No test has been written; tests remain unauthorized.

| Seam | Boundary | Evidence approach |
| --- | --- | --- |
| TS-01 | Public `DeskOfficerReviewService` operations observed through immutable `ReviewQueueState` | Primary behavior seam for membership, order, filters, selection, exact messages, transition guard, stale reconciliation, and retry state |
| TS-02 | TS-01 backed by a real `JsonReportRepository` at an `@TempDir` path | Prove missing-store behavior and fresh-instance durable status-only replacement without mocking persistence internals |
| TS-03 | Existing `ReportRepository` seam with a small scripted test adapter | Use only for deterministic target disappearance and load/replace fault sequences that cannot be induced portably; assert service state and persisted logical outcome, not call counts |
| TS-04 | Existing authentication coordinator route/session seam plus source review of lazy destination mounting | Reuse role/logout tests; verify the supplier is reachable only on `DESK_OFFICER` without adding JavaFX test infrastructure |
| TS-05 | Manual JavaFX workflow | Verify mounted controls, exact five-value rows, all eleven details, privacy separation, action enablement, empty/error states, logout clearing, and screenshot crop |
| TS-06 | Source, documentation, Git, and command evidence | Verify one shared path, precise ignore rule, ownership, absence of shadow models/dependencies, docs, full checks, release, and smoke launch |

### Automated behavior implications

Later automated evidence should cover, through the seams above:

- mixed submitted/under-review membership and stable order across Reporter IDs;
- default All plus Lost and Found filtering with relative order;
- globally empty versus filtered-empty exact copy;
- initial selection, retained selection, cleared filtered-out selection, and
  Start review enablement;
- selected canonical report availability for all detail rendering;
- successful status-only replacement and fresh-instance reconstruction;
- success when the result is globally or filter-empty;
- a selected target becoming `UNDER_REVIEW` before action;
- an automated public-interface check that no review-service operation accepts
  `ReportStatus` or another caller-selected target status;
- a selected target disappearing before action;
- replacement-target disappearance followed by successful and failed
  reconciliation;
- initial load failure and explicit Retry;
- stale-reconciliation Retry preserving its filter and stale notice;
- precheck and replacement failure retaining row, selection, details, filter,
  and retryable action;
- a no-selection action performing no repository mutation;
- every persistence reason mapping to safe context copy without leaked values;
  and
- immutable returned collections and deterministic states.

The forbidden reverse transition is verified by two automated pieces of
evidence: a narrow structural test of the public service surface proves that no
operation accepts `ReportStatus` or another caller-selected target status, and
a behavior test proves that an authoritative `UNDER_REVIEW` report remains
unchanged without a replacement call. This structural assertion verifies an
approved absence at the public seam rather than a private implementation
detail. The production interface must not be widened to a generic status
transition merely to create a reverse-transition test call.

### Test isolation and privacy

- Real persistence tests use only `@TempDir` and synthetic reports.
- A scripted repository is limited to the already real persistence seam and
  deterministic faults; internal review methods are not mocked or exposed.
- Expected messages are independent approved literals.
- Assertions and display names must not print descriptions, reporter IDs, JSON,
  or paths on failure. `ItemReport.toString()` remains redacted.
- No test reads or writes project-local `data/reports.json`.
- Existing domain, persistence, authentication/session, and Student tests remain
  compatibility gates.

### Manual and completion implications

Because no UI-test framework is approved, manual JavaFX evidence covers the
rendered workflow. Completion later requires focused tests, `gradlew.bat check`,
`gradlew.bat release`, a packaged smoke launch, screenshot privacy review, and
final diff/status/ignore audits. Those commands are not run for this docs-only
approval update.

The separate requirements-to-tests artifact remains the next planning gate and
is intentionally not created by this resumed TDD stage.

## Vertical implementation sequence

After requirements-to-tests approval, branch authorization, and separate
implementation authorization, implementation should proceed in these bounded
vertical slices. Each automated slice begins with one failing behavior test and
adds only enough production behavior to pass it.

1. **Initial queue slice:** service entry through the real repository, submitted
   membership, stable order, missing-store global empty, and immutable state.
2. **Filter and selection slice:** exact filter enum, relative order, both empty
   cases, selection retention/clearing, and action enablement.
3. **Details presentation slice:** canonical selected report, exact formatters,
   five-value row cell, all-eleven-value read-only details, and privacy
   separation.
4. **Durable transition slice:** authoritative recheck, status guard,
   `withStatus`, complete replacement, fresh-instance preservation, success
   feedback, removal, selection clear, and preserved filter.
5. **Stale and failure slice:** non-submitted/missing target, replacement race,
   initial/reconciliation Retry context, and transition-failure retention.
6. **Authenticated mounting slice:** lazy Desk Officer supplier, fresh per-login
   service/view, logout removal, one shared repository path, and precise ignore
   rule.
7. **Documentation and manual evidence slice:** scoped styling, User Guide,
   Developer Guide, minimal README correction, safe screenshot, focused/full
   verification, release, and smoke evidence.

Student integration, additional statuses, refactoring, and release-artifact
commit decisions are not folded into these slices.

## Meaningful rejected alternatives

| Alternative | Decision | Reason |
| --- | --- | --- |
| Add repository queries such as `loadSubmitted()` or `findById()` | Rejected | `loadAll()` already supplies the complete ordered snapshot; a query API would duplicate workflow policy and expand persistence. |
| Add a conditional status-transition operation to the repository now | Rejected | The approved one-instance writer model does not require a new persistence contract; multi-writer compare-and-set is out of scope. |
| Put transition rules in `ItemReport.withStatus(...)` or `ReportStatus` | Rejected | The canonical domain explicitly leaves workflow legality to callers and is Developer 1-owned. |
| Reconstruct an eleven-field replacement in the service | Rejected | `withStatus(...)` already produces the exact complete copy with less error surface. |
| Add a queue-row/report-details DTO | Rejected | Canonical `ItemReport` plus a custom JavaFX cell and read-only details already suffice; a second report-shaped type risks drift. |
| Read or edit `reports.json` directly from the review module | Rejected | It bypasses the approved repository, strict schema, recovery, ordering, and safe replacement. |
| Add separate service, controller, mapper, and view-model layers | Rejected | One plain-Java application service plus immutable state gives the required test seam and keeps the interface deep. |
| Add a generic workflow/state-machine framework | Rejected | There is one transition and no approved future transition set. |
| Add a generalized application router or route registry | Rejected | One lazy JDK supplier extends the existing role-navigation pane without new infrastructure. |
| Construct or cache the officer pane at application startup | Rejected | It could load before authorization and would retain stale selection/filter state across logout. |
| Reload after successful replacement | Rejected | The successful authoritative snapshot already determines the new queue; a second failing read could obscure a mutation that already committed. |
| Add background loading, polling, or automatic Retry | Rejected | The PRD forbids live updates and automatic retry, and concurrency would add complexity without an approved requirement. |
| Add TestFX or another UI dependency | Rejected | Plain-Java service/state tests plus focused manual JavaFX evidence are proportionate and preserve the current build. |
| Integrate the existing Student form while touching routing | Rejected | Student composition is a separate owner/scope obligation and is explicitly excluded from S1-D2-03. |

## Assumptions, risks, blockers, and unresolved decisions

### Assumptions

1. The application remains a single process using one shared report repository
   instance and JavaFX-serialized review actions.
2. Synchronous bounded repository calls on the JavaFX thread are acceptable for
   this sprint; no responsiveness requirement or asynchronous architecture is
   approved.
3. Holding complete reports in the authenticated per-view service is within the
   inherited plaintext persistence/privacy boundary. Only selected details are
   rendered, and no report values are logged.
4. Removing the authenticated subtree satisfies the approved visible logout
   privacy requirement; there is no background task or retained navigation
   handle to the removed pane.
5. Existing enum display labels and `ReportConstraints` formatters are the
   canonical presentation sources.

### Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Unsupported external writer changes the target between recheck and replace | Record the one-writer assumption; use the authoritative report; handle missing replacement with reconciliation; do not claim compare-and-set safety. |
| JavaFX list refresh accidentally fires selection/filter handlers | Render from one state under a local rendering guard and reselect only the state-owned ID. |
| Private detail leaks through summary, message, screenshot, or diagnostic | Custom five-field cell; fixed copy; separate selected-only details; redacted domain string; safe screenshot crop and review. |
| A storage failure is mistaken for an empty queue | Availability is explicit and load-unavailable state cannot derive empty copy. |
| A successful commit is followed by an ambiguous failing reload | Do not reload after successful replace; derive state from the already authoritative ordered snapshot. |
| Review styling changes existing screens | Use a review-owned stylesheet with scoped selectors; do not edit shared rules. |
| Work is accidentally committed as persistence scope | Require separately authorized S1-D2-03 branch creation before implementation or commit activity. |

### Blockers and gates

- The requirements-to-tests stage has not begun and remains a separate next
  gate.
- Branch creation is not authorized.
- Test and production implementation are not authorized.
- Commit, push, pull-request, release-artifact, and merge actions remain
  unauthorized.

### Unresolved technical decisions

**Unresolved decision:** None.

Repository evidence, approved requirements, and ownership boundaries determine
the service seam, transition placement, persistence flow, route integration,
error mapping, and verification approach. No `/grill-me` question remains.

## PRD-to-design traceability summary

This table preserves design-level traceability without starting the separate
requirements-to-tests mapping stage.

| Approved requirement area | TDD decision |
| --- | --- |
| FR-001, FR-013 | Existing route/session authority; lazy Desk Officer-only factory; fresh per-login state; subtree removal on logout; one shared repository path |
| FR-002 | `loadAll()` plus in-memory `SUBMITTED` filter in repository order |
| FR-003 | Exact non-persisted filter enum; stable-order predicates; active-filter retention |
| FR-004 | Custom `ListCell<ItemReport>` using exactly five approved canonical values |
| FR-005 | Service-owned visible selection invariant and derived action enablement |
| FR-006 | Selected canonical report rendered read-only with canonical labels/formatters and separated private section |
| FR-007, FR-008 | One service-owned Start review operation; authoritative status guard; fixed target status; reverse unrepresentable |
| FR-009 | `withStatus` on authoritative report; complete `replace`; success only after return; no post-success reload |
| FR-010 | Recheck snapshot reconciliation; missing replacement reload; preserved filter and stale retry context |
| FR-011 | Explicit availability and action-context error mapping; safe fixed copy; manual Retry only |
| FR-012 | State derives global versus filtered empty before rendering |
| FR-014 | Planned User Guide, Developer Guide, minimal README, and privacy-safe screenshot changes |
| NFR-001 | Desk Officer-only mounting, five-field rows, selected-only private detail, fixed messages, synthetic isolated evidence |
| NFR-002 | Validate before mutation; preserve state on failure; success only after durable replacement |
| NFR-003 | Direct canonical types, accessors, display labels, formatters, and `withStatus`; no duplicate model |
| NFR-004 | Immutable state, synchronous explicit actions, stable order, no timers/polling/implicit retry |
| NFR-005 | `@TempDir`, synthetic reports, scripted faults only at the repository seam, safe manual data |
| NFR-006 | Existing suites plus focused tests, full check, release, and packaged smoke gate |
| NFR-007 | New Developer 2 review package and role wiring; only the pre-approved minimal shell composition edit |

## Approval record

The repository owner approved this TDD on 2026-09-21. This approval confirms:

- the single stateful review-service seam and immutable state contract;
- the exact authoritative recheck and status-only replacement flow;
- the context-sensitive failure and stale-retry design;
- the lazy Desk Officer destination integration;
- the 2026-09-21 merge-reconciliation amendment for the explicitly approved
  Student workspace repository-injection overload;
- the proposed public test seams; and
- the listed ownership boundaries and rejected alternatives.

This approval does not authorize requirements-to-tests work, branch creation,
tests, implementation, commits, release changes, or publication. Any requested
change to observable product behavior returns first to the approved PRD.
