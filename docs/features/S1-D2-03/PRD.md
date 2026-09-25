# S1-D2-03 Build Desk Officer Review Queue PRD

- Status: Approved
- Draft date: 2026-09-20
- Approver: Repository owner
- Approval date: 2026-09-21
- Implementation authorization: Not granted
- Branch creation authorization: Not granted
- Feature: Build Desk Officer Review Queue
- Workstream: Developer 2
- Approved mission brief: `docs/mission-briefs/S1-D2-03-officer-review.md`
- Decision ledger: `docs/features/S1-D2-03/GrillingDecisions.md`
- Reconciled planning baseline: `a1383ce2cbe6386b67cc675608a10df9ea663261`

## Authority and precedence

This PRD is governed, in descending order, by:

1. `AGENTS.md`
2. The repository owner's 2026-09-20 direction to reconcile against and treat
   the current repository state as authoritative
3. The approved S1-D2-03 Mission Brief
4. The current canonical report-domain contract and approved S1-D2-02
   persistence contract
5. The completed S1-D2-03 decision ledger
6. This PRD

The Mission Brief's historical model-shape and branch-point statements do not
override the current canonical domain. Its approved product scope, exclusions,
ownership boundaries, and authorization gates remain controlling. A genuine
product conflict stops work and returns to repository-owner review.

Approval of this PRD does not authorize a TDD, requirements-to-tests mapping,
tests, implementation, branch creation, commits, publishing, or merging.

## Purpose

Give an authenticated Desk Officer a small, persistence-backed workflow for
reviewing newly submitted lost-and-found reports. The Desk Officer can scan the
submitted queue, filter it by Lost or Found, inspect a report's complete details,
and explicitly start review by changing only its status from `SUBMITTED` to
`UNDER_REVIEW`.

The workflow must remain truthful when storage is empty, unavailable, or changes
between viewing and action. It is not a generalized report-management system.

## Actors and audience

- **Desk Officer:** Reviews submitted reports and starts work on one report.
- **Student:** Owns submitted reports through the separately owned submission
  workflow. S1-D2-03 does not change the Student experience.
- **Developer 1:** Owns the canonical report domain, Student submission, and the
  initial JavaFX shell.
- **Developer 2:** Owns the Desk Officer queue, filtering, details, transition
  policy, role navigation, report persistence, storage-failure behaviour, and
  officer-review documentation.

## Success criteria

The feature succeeds when:

1. Only an authenticated Desk Officer can reach the review workflow.
2. The queue displays every and only currently submitted report in stable
   repository insertion order.
3. All, Lost, and Found filtering is predictable and never mutates a report.
4. A selected report exposes every canonical value while keeping private detail
   out of summaries and screenshots.
5. **Start review** permits only `SUBMITTED -> UNDER_REVIEW`, changes only status,
   and reports success only after durable persistence.
6. Empty, stale, invalid, and storage-failure outcomes are distinct, truthful,
   privacy-safe, and recoverable as specified below.
7. Existing authentication and persistence behaviour remains compatible, and
   verification uses only synthetic isolated data.

## Dependencies and inherited contracts

### Canonical report domain

Developer 1's `ItemReport` is the single report representation. It is an
immutable value with these eleven required accessors:

1. Report ID
2. Reporter ID
3. Report type
4. Item name
5. Category
6. Location
7. Occurrence date
8. Public description
9. Private identifying detail
10. Status
11. Created At

The feature accepts every current canonical `ItemCategory`. `ReportType` remains
`LOST` or `FOUND`; `ReportStatus` remains `SUBMITTED` or `UNDER_REVIEW`.
Canonical human-facing display names are used in the UI. The report domain does
not decide whether a status transition is legal.

### Persistence

The approved `ReportRepository` contract:

- loads every report as an unmodifiable snapshot in stable insertion order;
- treats missing storage as a valid empty repository without creating storage;
- replaces one complete report without moving its stored position;
- preserves Report ID, Reporter ID, and Created At at the repository boundary;
- deliberately does not enforce application workflow transitions;
- reports typed, privacy-safe failures; and
- reports mutation success only after safe complete replacement.

S1-D2-03 adds no repository operation and does not read JSON directly.

### Authentication and application integration

Authentication already routes stored `DESK_OFFICER` accounts to a distinct
destination and clears access on logout. Application composition must use one
shared report repository at `data/reports.json`. Startup integration is limited
to the smallest approved composition and navigation change; it must not place
review behaviour inside the authentication UI or alter the Student destination.

The queue workflow and transition policy must remain in a separate Developer
2-owned review application service, presented through a separate Desk Officer
view. This boundary is inherited from the Mission Brief; service and view names,
interfaces, and internal collaboration remain TDD decisions.

### Current integration state

The reconciled baseline includes Developer 1's canonical report domain, Student
submission slice, and Developer 2's persistence integration. Student submission
and report persistence are not yet composed into the running application. That
integration fact does not expand S1-D2-03 into Student feature work.

## Glossary

- **Submitted queue:** The ordered set of stored reports whose current status is
  `SUBMITTED`.
- **Active filter:** Exactly one of All, Lost, or Found.
- **Visible report:** A submitted report matching the active filter.
- **Selected report:** One visible report chosen for details or review.
- **Start review:** The explicit attempt to persist the selected report with
  status `UNDER_REVIEW` and every other value unchanged.
- **Globally empty:** No submitted report exists before type filtering.
- **Filtered empty:** At least one submitted report exists, but none matches the
  active Lost or Found filter.
- **Stale target:** A selected report that is missing or no longer `SUBMITTED`
  when Start review rechecks authoritative state.
- **Storage failure:** A corrupt, unsupported, inaccessible, non-regular,
  unsafe-to-replace, unencodable, or over-limit report-store outcome.

### User-facing copy status

The three empty-state sentences in FR-012 are exact owner-approved copy. The
neutral prompt, success confirmation, stale-target message, and two contextual
storage-failure messages below are proposed exact copy for this PRD. Their
meaning and interaction were owner-confirmed during grilling; approval of this
PRD makes the proposed wording binding for S1-D2-03.

## Included scope

- Authenticated Desk Officer access to the submitted queue
- Automatic queue loading on entry
- `SUBMITTED`-only membership across all Reporter IDs
- Stable insertion order
- Exactly All, Lost, and Found filters
- Public queue-row summaries
- Single selection and complete read-only details
- Private-detail separation within the authenticated details view
- Explicit `SUBMITTED -> UNDER_REVIEW` action
- Status-only, durably persisted replacement
- Invalid repeat, reverse, missing, and stale-target rejection
- Global-empty, filtered-empty, and storage-error presentation
- Explicit recovery from initial and transition storage failures
- Selection and active-filter behaviour
- Logout and role-boundary preservation
- One shared repository at `data/reports.json`
- A separate Developer 2-owned review service and Desk Officer view
- A precise ignore rule for `data/reports.json`
- Focused automated evidence, manual JavaFX evidence, and privacy-safe
  documentation/screenshot updates

## Excluded scope

- Student submission, creation, status checking, or route redesign
- A report-seeding UI, committed demonstration report store, or supported manual
  JSON editing
- Managing or listing reports already `UNDER_REVIEW`
- Returning a report to `SUBMITTED`
- Adding or changing canonical statuses, types, categories, display names, or
  persisted enum names
- Editing any report field
- Deletion, matching, claiming, collection, or return workflows
- Search, sorting controls, newest-first sorting, pagination, assignment, or
  live/background updates
- A normal-state manual refresh feature
- A generic workflow engine, transition framework, repository query operation,
  or duplicate report model
- Changes to the version-one report-store schema or persistence-failure contract
- Multi-process or multi-repository-instance coordination
- Migration, repair, backup, encryption, or filesystem access-control features
- New UI-test frameworks or dependencies
- Authentication redesign or changes to the Student destination

## Functional requirements

### FR-001 — Enforce Desk Officer access

The review workflow must be reachable only while the authenticated session's
stored role is `DESK_OFFICER`. A Student session must not expose the queue,
details, private identifying detail, or Start review action.

Entering the Desk Officer destination automatically attempts to load the queue.
Logout must clear the session, remove the role-specific view and its selected
private details, and return to login. Stale navigation must not reopen it.

### FR-002 — Build the submitted queue

A successful load must derive the queue from all stored reports by including
every and only report whose current status is `SUBMITTED`. Membership must not
be limited by Reporter ID or assigned Desk Officer.

The queue must retain repository insertion order. Filtering, selecting, viewing,
failed actions, and successful status replacement must not reorder remaining
reports.

An `UNDER_REVIEW` report must never appear in this queue. Reports are not deleted
when they leave the queue.

### FR-003 — Apply the exact type filters

The interface must expose exactly **All**, **Lost**, and **Found**. **All** is
active on first entry.

- **All** shows every submitted report.
- **Lost** shows submitted reports whose canonical type is `LOST`.
- **Found** shows submitted reports whose canonical type is `FOUND`.

Filtering must preserve the reports' relative insertion order and must not
change any report value. The active filter remains selected after a successful
review and after stale-target reconciliation.

### FR-004 — Show the approved queue-row summary

Each visible queue row must show exactly these public summary values:

- Lost or Found type using its canonical display name
- Item name
- Category using its canonical display name
- Occurrence date in documented ISO local-date form
- Location

Rows must not show Report ID, Reporter ID, public description, private
identifying detail, status, or Created At. Visually identical summaries remain
separate selectable reports; full identity is available in details.

### FR-005 — Manage selection predictably

The view starts without a selection and displays:

`Select a report to view details.`

With no selected visible report, **Start review** must remain visible but
disabled.

Selecting a visible row displays its details and enables **Start review**.
Changing filters preserves selection only while that report remains visible. If
the selected report does not match the new filter, selection and details clear,
the neutral prompt returns, and **Start review** becomes disabled.

### FR-006 — Display complete read-only details

The details view must show all eleven canonical values for the selected report:

- Report ID as the complete canonical UUID
- Reporter ID as stored
- Type using its canonical display name
- Item name as stored
- Category using its canonical display name
- Location as stored
- Occurrence date in ISO local-date form
- Public description as stored
- Private identifying detail as stored
- Status using its canonical display name
- Created At as the documented UTC timestamp with millisecond precision

The public description and private identifying detail must have distinct labels
and visibly separate sections. The private section must identify the value as
reserved for Desk Officer verification. No detail field is editable.

### FR-007 — Expose one explicit Start review action

**Start review** is the only status-changing action in this feature. Activating
it for a selected visible report must immediately attempt review without an
additional confirmation dialog.

Before mutation, the workflow must recheck authoritative report state. Viewing,
selection, filtering, retrying a load, or cancelling navigation through logout
must never change status.

### FR-008 — Permit only the valid status transition

The only successful transition is `SUBMITTED -> UNDER_REVIEW`.

A repeat action against `UNDER_REVIEW` or a reverse
`UNDER_REVIEW -> SUBMITTED` request must be rejected without persistence
mutation and without a success result. The reverse transition is not exposed in
the UI. Any future status or transition requires separate product approval.

The successful replacement must preserve the selected report's Report ID,
Reporter ID, type, item name, category, location, occurrence date, public
description, private identifying detail, and Created At exactly. Only status may
change.

### FR-009 — Report success only after durable replacement

The interface may report success only after the repository accepts the complete
replacement. On success:

1. The persisted status is `UNDER_REVIEW` and survives repository
   reconstruction.
2. Every non-status value is unchanged.
3. The report immediately leaves the submitted queue.
4. Selection and details clear and the neutral details prompt returns.
5. **Start review** becomes disabled.
6. The active filter remains selected.
7. The interface shows the non-blocking message `Review started.`

No success dialog is shown.

If removal leaves no submitted report globally, the success confirmation is
shown with the global-empty state. If submitted reports remain but none matches
the active Lost or Found filter, it is shown with the corresponding
filtered-empty state.

### FR-010 — Reconcile a stale or missing target

If authoritative state shows that the selected report is missing or no longer
`SUBMITTED`, the workflow must not report or persist a successful transition.
It must:

1. show `This report is no longer available for review.`;
2. reload the authoritative submitted queue;
3. preserve the active filter;
4. clear selection and details and restore the neutral details prompt; and
5. disable **Start review** until another visible report is selected.

If the reconciliation load itself fails, FR-011's load-failure state applies.
Its Retry preserves the same active filter. A successful reconciliation reload
may show the global-empty or applicable filtered-empty state while retaining the
stale-target message. This action-triggered reload is not a live-update feature.

### FR-011 — Handle storage failure truthfully

A missing report store is a valid empty repository and follows FR-012. Corrupt,
unsupported, inaccessible, non-regular, unsafe-to-replace, unencodable, or
over-limit storage must never be presented as empty or successful.

If initial or reconciliation loading fails, the view must:

- show `Reports are unavailable. Please try again.`;
- show an explicit **Retry** action;
- show no queue entries or report details; and
- keep **Start review** disabled.

**Retry** must explicitly attempt another authoritative load. No automatic or
background retry is permitted. Initial-entry Retry uses the default **All**
filter. Retry after a failed stale-target reconciliation preserves the filter
that was active when reconciliation began. While loading is unavailable, the
storage-error message replaces the neutral details prompt and no report value is
shown.

If replacement fails, the view must:

- show
  `Review could not be started because reports are unavailable. Please try again.`;
- report no success;
- leave the selected report `SUBMITTED` in the complete authoritative store;
- retain the row, selection, complete details, and active filter; and
- leave **Start review** available for an explicit retry.

All storage-failure reasons listed above share these context-appropriate user
messages. The view must not expose exception text, file paths, report contents,
unsafe input, schema internals, or technical failure categories.

### FR-012 — Distinguish both empty conditions

If no submitted report exists before type filtering, the queue must show:

`No submitted reports.`

If submitted reports exist but the active type filter has no match, it must show
the applicable message:

- `No submitted reports match the Lost filter.`
- `No submitted reports match the Found filter.`

Empty states are inline, not modal. They show the neutral details prompt, show no
report values, clear selection, and disable **Start review**. Switching to a
filter with matching reports must show those reports without changing their
order.

### FR-013 — Preserve storage and session boundaries

The running application must use one shared report repository whose store is
`data/reports.json`, resolved from the application working directory. The store
remains plaintext, application-owned, and excluded from Git by a precise ignore
rule. No store contents or demonstration report store may be committed.

Logout must remove all selected report values, including private detail, from
the visible interface. A later Desk Officer login performs a fresh queue load.

### FR-014 — Keep documentation truthful

The User Guide must document Desk Officer entry, queue fields, filters, details,
Start review, success, both empty states, and storage-error recovery. It must
embed a cropped queue screenshot using safe synthetic reports and no private or
credential information.

The Developer Guide must document role integration, queue membership/order,
transition policy, storage path, privacy boundary, and the division between the
canonical domain, persistence, review workflow, and UI. The README receives only
the minimal current-status correction required by the implemented feature.

## Non-functional requirements

### NFR-001 — Privacy and authorization

Private identifying detail may appear only in the authenticated selected-report
details view and within the inherited report-persistence boundary: the
authoritative plaintext store and its private temporary replacement file. It
must not appear in queue rows, empty/error/success messages, screenshots, logs,
diagnostics, generated documentation examples, test output, or handoff
summaries.

Real student, staff, school, credential, or report data must never be used.

### NFR-002 — Truthful and safe mutation

No UI state may imply a successful transition before durable repository success.
Invalid or failed actions must not alter persisted report state. A replacement
failure must leave the previously authoritative report store complete according
to the inherited persistence contract.

### NFR-003 — Canonical compatibility

The feature must consume Developer 1's canonical `ItemReport`, `ReportType`,
`ItemCategory`, and `ReportStatus` directly. It must support all current
canonical categories and use canonical display labels. It must not add a
wrapper, duplicate model, duplicate enum, new status, or new persisted token.

### NFR-004 — Deterministic presentation

Given the same ordered repository snapshot, status set, active filter, and
selection, the externally visible queue, empty state, details, and enabled
actions must be deterministic. No implicit sorting, automatic retry, or
background refresh may change the view.

### NFR-005 — Test and demonstration isolation

Automated persistence-affecting evidence must use synthetic reports and isolated
temporary storage. Manual demonstration must use synthetic data and must confirm
that no real or project-local user report store was read or modified.

### NFR-006 — Compatibility verification

Existing canonical-domain, report-persistence, authentication, session, and
Student-submission checks must remain green. Implementation completion also
requires the repository's full `check`, release packaging, and packaged smoke
launch because startup and demonstration behaviour change.

### NFR-007 — Scope and ownership

Implementation must keep workflow policy in the separate Developer 2-owned
review application service and presentation in the separate Desk Officer view.
It must remain inside Developer 2's approved review ownership plus the already
approved minimal startup composition change. Any additional change to Developer
1-owned domain, Student workflow, initial shell, build, dependency, release, or
CI behaviour requires separate approval.

## Main scenarios and extensions

### SC-001 — Enter and inspect the submitted queue

1. An authenticated Desk Officer enters the role destination.
2. The application loads the report store.
3. The **All** filter displays submitted reports in insertion order.
4. Rows show only the five approved public summary values.
5. The Desk Officer selects one report.
6. All eleven read-only values appear, with private detail separated.
7. **Start review** becomes enabled.

Extensions:

- A Student session cannot reach the workflow.
- A missing store follows the globally empty scenario.
- A failed load follows SC-006.

### SC-002 — Filter and preserve valid selection

1. Submitted Lost and Found reports are visible under **All**.
2. The Desk Officer selects a Lost report.
3. Switching to **Lost** retains the selection and details.
4. Switching to **Found** clears them and disables **Start review**.
5. Each filtered list retains relative insertion order.

Extension:

- If the filter has no match but another submitted type exists, the applicable
  filtered-empty message appears.

### SC-003 — Start review successfully

1. The Desk Officer selects a visible submitted report.
2. They activate **Start review** without a confirmation dialog.
3. Authoritative state still contains the submitted report.
4. Complete status-only replacement succeeds durably.
5. The report leaves the queue, selection clears, and the neutral details prompt
   returns.
6. The active filter remains selected.
7. `Review started.` appears.
8. A later repository reconstruction observes `UNDER_REVIEW` with all other
   values unchanged.

Extensions:

- Reviewing the last globally submitted report shows the success confirmation
  with `No submitted reports.`
- Under Lost or Found, if submitted reports remain but none matches the active
  filter, the success confirmation appears with the applicable filtered-empty
  state.

### SC-004 — Queue is empty

1. Loading succeeds with no submitted reports, including when storage is absent.
2. The queue shows `No submitted reports.`
3. Selection is clear, the neutral details prompt is shown, and **Start review**
   is disabled.

Extension:

- When submitted reports exist only outside the active Lost or Found filter,
  the applicable filter-specific empty message appears instead.

### SC-005 — Selected target becomes stale

1. The Desk Officer selects a submitted report.
2. The action recheck finds it missing or no longer submitted.
3. No transition or success is reported.
4. The unavailable message appears.
5. The queue reloads under the same active filter, selection clears, and the
   neutral details prompt returns.

Extensions:

- If reload produces no globally submitted report or no match for the active
  Lost or Found filter, the corresponding empty state appears alongside the
  unavailable message.
- If reload fails, the load-storage-error state replaces the queue; its Retry
  preserves the active filter.

### SC-006 — Initial load fails

1. Entering the Desk Officer destination cannot safely load the store.
2. The storage-unavailable message appears instead of an empty queue.
3. No row or details are shown and Start review is disabled.
4. The Desk Officer selects **Retry**.
5. A successful retry presents the authoritative queue or true empty state.

Extension:

- When this load error follows stale-target reconciliation instead of initial
  entry, Retry preserves the filter that was active when reconciliation began.

### SC-007 — Transition persistence fails

1. The Desk Officer selects a submitted report and activates **Start review**.
2. Replacement fails before durable success.
3. No success is shown; no transition is committed, and the authoritative report
   remains `SUBMITTED`.
4. The row, selection, details, filter, and enabled action remain.
5. The contextual storage message appears.
6. The Desk Officer may explicitly retry **Start review**.

### SC-008 — Logout clears access and private details

1. A Desk Officer has selected a report with private detail visible.
2. They log out.
3. The authenticated session and role view are removed.
4. The login view contains no report details.
5. Stale navigation cannot reopen the queue.

## Acceptance criteria

- **AC-001:** A Desk Officer login automatically reaches a loadable review view;
  a Student cannot access it, and logout removes the view and selected details.
- **AC-002:** For a mixed stored snapshot, the queue contains every and only
  `SUBMITTED` report in repository insertion order and excludes every
  `UNDER_REVIEW` report.
- **AC-003:** All is the default; Lost and Found include only their canonical
  type while preserving relative insertion order.
- **AC-004:** Every queue row shows exactly type, item name, category,
  occurrence date, and location with canonical display labels where applicable,
  and exposes none of the six excluded detail values.
- **AC-005:** Selection is initially empty, remains when a filter still includes
  the report, and clears when the report is filtered out. Start review is
  disabled whenever no visible report is selected. Every cleared-selection
  state other than the defined storage-error state shows the neutral details
  prompt.
- **AC-006:** Selecting a report displays all eleven canonical values read-only,
  including complete identifiers and time values, with public and private
  descriptions visibly separated.
- **AC-007:** One immediate Start review action changes a submitted report to
  under review only after durable replacement, removes it from the queue, clears
  selection, preserves the active filter, and shows `Review started.` The
  resulting queue or applicable global/filtered empty state is also shown.
- **AC-008:** Fresh-instance persistence evidence proves successful review
  changes only status and preserves all ten other values exactly.
- **AC-009:** Repeat, reverse, and no-selection actions cannot produce a
  successful persistence mutation. The UI never exposes the reverse action or
  an enabled no-selection action.
- **AC-010:** A missing or already-under-review target produces the unavailable
  result, performs no new transition, reloads the queue under the same filter,
  clears selection, restores the neutral details prompt, and shows any resulting
  global/filtered empty state.
- **AC-011:** Missing storage produces the globally empty state without creating
  storage; every storage-failure condition listed in FR-011 produces the
  storage-error state instead.
- **AC-012:** Initial storage failure offers Retry with no queue or details;
  Retry after failed stale-target reconciliation preserves the active filter;
  transition failure leaves the report submitted, retains the current row and
  selection, and permits explicit Start review retry. None exposes internal
  failure details.
- **AC-013:** Global and filtered empty conditions display their exact approved
  inline messages, clear selection, show the neutral details prompt, and disable
  Start review.
- **AC-014:** Queue and details correctly render every canonical category and
  the canonical type, category, and status display labels rather than raw stored
  tokens.
- **AC-015:** The final queue screenshot uses only safe synthetic data and shows
  no private identifying detail, unsafe input, signed-in username, credential,
  or technical storage information.
- **AC-016:** Focused automated evidence covers queue membership/order, filters,
  both empty cases, selection, durable status-only transition, invalid repeat
  and reverse attempts, stale/missing target, and storage failure/retry.
- **AC-017:** Existing domain, persistence, authentication/session, and Student
  submission checks plus the full repository check remain green; release and
  packaged smoke verification pass before implementation completion.
- **AC-018:** Verification and demonstration use only synthetic reports and
  isolated temporary storage and confirm that no real or project-local report
  data was touched.
- **AC-019:** User Guide, Developer Guide, and README changes accurately describe
  only delivered behaviour and preserve the documented privacy boundary.
- **AC-020:** Composition evidence confirms one shared repository at
  `data/reports.json`, resolved from the application working directory. Final
  diff and version-control status evidence confirms that exact store is ignored
  and untracked and that no report-store contents are committed.
- **AC-021:** Delivered structure keeps queue and transition policy in a separate
  Developer 2-owned review application service, presents it in a separate Desk
  Officer view, and does not place review behaviour in the authentication UI.

## Requirement index

| Requirement | Observable outcome | Acceptance evidence |
| --- | --- | --- |
| FR-001 | Only authenticated Desk Officers reach the workflow; logout removes access | AC-001, AC-015 |
| FR-002 | Queue membership and order are correct | AC-002 |
| FR-003 | Exact filters preserve order and context | AC-003, AC-005, AC-007, AC-010, AC-012 |
| FR-004 | Rows contain the approved public summary only | AC-004, AC-014, AC-015 |
| FR-005 | Selection and action enablement are predictable | AC-005, AC-009 |
| FR-006 | All eleven details are visible read-only with privacy separation | AC-006, AC-015 |
| FR-007 | Start review is explicit, immediate, and side-effect free before action | AC-007, AC-009 |
| FR-008 | Only the approved transition can succeed and only status changes | AC-008, AC-009 |
| FR-009 | Durable success drives removal, feedback, and preserved filter | AC-007, AC-008 |
| FR-010 | Stale and missing targets reconcile without false success | AC-010 |
| FR-011 | Storage errors remain distinct, safe, and recoverable | AC-011, AC-012 |
| FR-012 | Global and filtered empty states are distinct | AC-011, AC-013 |
| FR-013 | Store/session boundaries and logout privacy are preserved | AC-001, AC-015, AC-018, AC-020 |
| FR-014 | Documentation and screenshot match delivered behaviour | AC-015, AC-019 |
| NFR-001 | Private data remains confined to approved boundaries | AC-001, AC-004, AC-006, AC-012, AC-015, AC-018, AC-020 |
| NFR-002 | Invalid and failed actions cannot imply or cause partial success | AC-007 through AC-012 |
| NFR-003 | The feature consumes every current canonical report value directly | AC-008, AC-014 |
| NFR-004 | Presentation is deterministic without hidden refresh or retry | AC-002, AC-003, AC-005, AC-012 |
| NFR-005 | Tests and demonstrations are isolated and synthetic | AC-015, AC-018 |
| NFR-006 | Existing and release verification remains green | AC-016, AC-017 |
| NFR-007 | Scope and ownership boundaries remain intact | AC-017, AC-019, AC-021 |

## Intentional TDD decisions

The following are explicitly deferred because they do not change the approved
observable behaviour:

- review-service and view class names and interfaces;
- result and error types used between service and UI;
- how authoritative rechecks and reloads share data internally;
- JavaFX control selection, layout, styling, focus behaviour, and responsive
  sizing;
- repository dependency injection and deterministic failure seams;
- package structure and test class organization; and
- the vertical red-green implementation sequence.

The TDD must not change queue fields, filters, selection rules, messages,
transition semantics, retry behaviour, privacy boundaries, or acceptance
outcomes without returning to this PRD for owner review.

## Approval and next gate

This complete PRD was approved by the repository owner on 2026-09-21. No product
decision remains unresolved.

The Technical Design Document stage has not been authorized. PRD approval alone
does not authorize requirements-to-tests planning, tests, implementation, branch
creation, commits, publishing, or merging.
