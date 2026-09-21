# S1-D2-03 Desk Officer Submitted-Report Review

- Status: Approved
- Owner: Developer 2
- Draft date: 2026-09-20
- Approver: Repository owner
- Approval date: 2026-09-20
- Implementation authorization: Not granted
- Branch creation authorization: Not granted
- Integration baseline: `royden/feat-report-persistence` at
  `35d5322a971cb3ba5496de9e270d25eda3870bcb`
- Governing instructions: `AGENTS.md`

## Goal

Give an authenticated Desk Officer a small, persistence-backed workflow to:

1. View the queue of submitted reports.
2. Filter it by lost or found type.
3. Inspect the complete details of a selected report.
4. Explicitly start reviewing it, changing its status from `SUBMITTED` to
   `UNDER_REVIEW`.
5. Receive a clear empty state when no submitted reports, or no reports matching
   the active filter, exist.

The feature reuses the existing report model, repository, authentication
session, and JavaFX patterns. It does not introduce a generalized
review-management system.

## Current baseline and relevant existing state

- The persistence branch contains the latest authentication integration and all
  S1-D2-02 persistence work. Current `main` at `06808cd` is its ancestor and
  does not contain the persistence-side commits.
- The tracked worktree was clean when this brief was prepared. Unrelated
  untracked files under `evals/mp2-dev2-delivery/` must remain excluded.
- The baseline `gradlew.bat check` completed successfully on 2026-09-20.
- `ItemReport` is an immutable record containing eleven required fields:
  Report ID, Reporter ID, report type, item name, category, location,
  occurrence date, public description, private identifying detail, status, and
  Created At.
- `ReportStatus` contains only `SUBMITTED` and `UNDER_REVIEW`.
- `ReportRepository` exposes `loadAll`, `insert`, and complete,
  identity-preserving `replace` operations.
- Persistence preserves stable insertion order and deliberately does not enforce
  workflow transitions. A calling workflow must reject an invalid transition
  before invoking replacement.
- A missing report store loads as an empty repository. Corrupt, inaccessible,
  unsupported, or oversized storage produces a typed failure and must not be
  presented as an empty queue.
- Authentication retains an in-memory user with a stored role and routes to
  separate Student and Desk Officer destinations.
- `AuthenticationPane` currently renders placeholder role homes. Its approved
  boundary prohibits placing report-review behaviour directly in the login UI.
- `FindersKeepersApp` composes authentication only. No report repository path or
  lifetime is currently wired.
- No report store, demonstration reports, review UI, screenshot asset, or
  S1-D2-03 feature-planning set existed before this brief.
- The User Guide, Developer Guide, and README still describe Desk Officer review
  as planned.

## In scope

- A Desk Officer-only submitted-report queue.
- Queue membership limited to reports whose status is `SUBMITTED`.
- A default **All** view with exactly **All / Lost / Found** filters.
- Stable repository insertion order, with no additional presentation sorting.
- Clear handling of both:
  - no submitted reports; and
  - no reports matching the active filter.
- Selection of one queue entry and display of all eleven canonical fields.
- Private identifying detail visible only within the authenticated Desk Officer
  details view and clearly separated from the public description.
- An explicit **Start review** action.
- Validation and persistence of the single allowed transition,
  `SUBMITTED -> UNDER_REVIEW`.
- Immediate removal of a successfully transitioned report from the submitted
  queue and clearing of its selection.
- Safe, distinct presentation of report-storage failure.
- One application-owned shared report repository at `data/reports.json`.
- A precise Git ignore rule for `data/reports.json`; no report-store contents
  committed.
- A separate Developer 2-owned review service and Desk Officer view.
- The smallest required startup and navigation integration.
- User Guide officer-review steps and a queue screenshot.
- Developer Guide workflow, transition, storage-path, and architecture updates.
- A minimal README current-status correction.
- Automated coverage of valid and invalid status changes, with proportionate
  queue, filter, and empty-state coverage.

## Non-goals

- Student submission or report creation.
- A report-seeding UI, committed demo report store, or supported manual JSON
  editing.
- Managing reports already under review.
- Returning a report to `SUBMITTED`.
- Additional statuses, categories, or changes to persisted enum names.
- Editing report fields, deletion, matching, claiming, collection, or return
  workflows.
- Search, user-selected sorting, newest-first sorting, pagination, or live
  updates.
- A generic workflow engine, state-machine framework, repository query API, or
  duplicate report model.
- Changes to the version-one JSON schema or persistence failure contract.
- Multi-process or multi-repository-instance coordination.
- New UI testing frameworks or dependencies.
- Authentication redesign or changes to the Student destination.
- Encryption, access-control configuration, migration, backup, or repair.

## Ownership boundaries

Developer 2 owns:

- The review application logic.
- Queue selection and filtering.
- Transition validation.
- The Desk Officer view.
- Role-navigation integration within the existing authentication flow.
- Report-store composition on the review side.
- Officer-review documentation and verification.

Developer 1 retains ownership of:

- `ItemReport`, `ReportType`, `ItemCategory`, and `ReportStatus`.
- Student submission and creation validation.
- The initial JavaFX shell and entry point.
- Build, dependency, release, and CI configuration.

The repository owner approved the smallest cross-owner change to
`FindersKeepersApp` as part of this mission: compose one shared report
repository at the approved path and provide it to the separate Desk Officer
workflow. No report behaviour may be added to the shell, and no shared report
type may be modified. This scope approval does not authorize implementation.

## Dependencies and existing contracts

- The canonical `ItemReport` constructor and accessors are the only report
  representation.
- Queue logic must use `ReportRepository.loadAll()` rather than reading JSON
  directly.
- Transition persistence must use the existing complete `replace` operation.
- A replacement must retain Report ID, Reporter ID, Created At, and every other
  field except status.
- The repository's stable insertion order supplies queue order.
- The application must use one shared repository instance.
- Missing storage means a valid empty queue; typed repository failures mean an
  error state.
- Only the authenticated `DESK_OFFICER` route exposes the workflow.
- Logout continues to clear the session and remove access to the officer view.
- No dependency change is needed or authorized.
- Earlier S1-D2-02 wording that left transition validation to an external
  application workflow is resolved here: S1-D2-03's Developer 2 review service
  owns this officer transition while persistence remains policy-free.

## Constraints

- The report store is `data/reports.json`, resolved from the application working
  directory.
- The store remains plaintext and must be ignored by Git.
- Tests must use isolated temporary storage and synthetic reports.
- Real student, staff, school, or report data must never be used.
- Queue rows expose only public summary information.
- The required screenshot must:
  - use synthetic reports;
  - show the queue rather than private details;
  - exclude the signed-in username and all credential information; and
  - exclude unsafe input and private identifying detail.
- Exact UI copy, layout, result types, and class signatures remain later PRD or
  TDD decisions.
- No production implementation begins without approved planning artefacts and
  separate implementation authorization.

## Conceptual implementation approach

At a high level:

1. Application startup creates one existing `JsonReportRepository` for
   `data/reports.json`.
2. The authenticated Desk Officer route displays a separate review view; the
   Student route remains unchanged.
3. A small review application service loads reports through `ReportRepository`.
4. It selects `SUBMITTED` reports and applies the chosen `ReportType` filter in
   memory, retaining insertion order.
5. Selecting a report displays its canonical values through existing accessors.
6. **Start review** rechecks that the selected report is still `SUBMITTED`,
   creates a complete replacement differing only in status, and calls
   `replace`.
7. Only after durable success does the view remove the report and clear the
   selection.
8. Empty results and storage failures become different UI states.

This approach requires no new public repository operation, status constant,
model helper, or generalized transition abstraction.

## Status-transition rules currently implied or confirmed

- The only valid transition is `SUBMITTED -> UNDER_REVIEW`.
- Transition occurs only through an explicit Desk Officer action.
- Viewing, selecting, or filtering a report never changes status.
- Repeating the action against an `UNDER_REVIEW` report is an invalid transition,
  not an idempotent success.
- `UNDER_REVIEW -> SUBMITTED` is invalid and is not exposed.
- An invalid transition performs no persistence mutation.
- A successful transition changes only `status`.
- Success is reported only after repository replacement succeeds.
- Missing targets, stale state, or storage failure must not be reported as a
  successful transition.
- Future statuses or transitions require separate shared-contract approval.

## Expected deliverables

- This approved Mission Brief under `docs/mission-briefs/`.
- Developer 2-owned review application logic and Desk Officer JavaFX view.
- The minimal approved `FindersKeepersApp` composition change.
- A `data/reports.json` ignore rule.
- Focused automated tests for review behaviour.
- An updated User Guide with officer-review steps.
- A cropped, privacy-safe synthetic queue screenshot embedded in the User Guide.
- An updated Developer Guide covering transition rules and integration.
- A minimal README current-status correction.
- A factual delivery and verification record and final handoff.

## Verification expectations

- Focused automated evidence covers queue membership and filtering, both empty
  conditions, successful durable transition, rejection of invalid repeat and
  reverse changes, and preservation of every non-status field.
- Persistence failure is verified as distinct from an empty queue.
- Existing report persistence and authentication/session tests remain green.
- Manual JavaFX verification covers Desk Officer login, filters, selection, full
  details, successful removal after transition, empty states, logout, and
  screenshot privacy.
- Run the repository's focused tests, `gradlew.bat check`, and, because startup
  and demonstration behaviour change, `gradlew.bat release` plus the packaged
  smoke launch.
- Inspect the final diff and status, confirm `data/reports.json` is untracked and
  ignored, and confirm no test or manual check touched real data.
- No UI-test dependency is expected; rendered behaviour may use recorded manual
  evidence where automated coverage would require an unapproved framework.

## Agentic workflow and checkpoints

1. **Mission Brief approval:** complete on 2026-09-20.
2. **Branch authorization:** create the S1-D2-03 branch from the verified
   baseline before writing further repository artefacts.
3. **Decision record and PRD:** record the accepted grilling decisions, draft
   the PRD, and stop for explicit approval.
4. **TDD:** design the concrete seams and integration, then stop for explicit
   approval.
5. **Requirements-to-tests planning:** map every approved behaviour to evidence
   and stop for explicit approval.
6. **Implementation authorization:** separately authorize the exact approved
   scope.
7. **Vertical delivery:** implement small observable slices with focused
   verification and reviewable diffs.
8. **Cross-owner checkpoint:** make only the approved startup composition
   change; escalate any additional shared-file need.
9. **Completion verification:** run focused tests, the full check,
   release/smoke verification, manual UI evidence, screenshot review, and a
   diff/status audit.
10. **Handoff:** report exact changes, evidence, limitations, and outstanding
    integration.
11. **Repository operations:** committing, pushing, opening a pull request,
    refreshing the committed release JAR, and merging each require their
    applicable separate authorization and review gates.

## Open decisions or blockers requiring human confirmation

No product decision remains after the approved grilling recommendations.

Current gates and blockers are:

- No S1-D2-03 PRD, TDD, or requirements-to-tests mapping exists yet.
- Implementation and feature-branch creation have not been authorized.
- Committing a refreshed release JAR remains a later release decision; release
  and smoke verification are still required.
- Commit, push, pull-request, and merge authorization have not been granted.
- Exact UI copy, concrete interfaces, and error/result types are intentionally
  deferred to the later approved planning stages rather than treated as implied
  contracts.

## Branching strategy for this sub-sprint

- Do not implement S1-D2-03 on `royden/feat-report-persistence`.
- This Mission Brief is the sole S1-D2-03 planning-file exception recorded on
  that branch under the repository owner's explicit instruction on 2026-09-20.
- The current correct feature-branch point is
  `35d5322a971cb3ba5496de9e270d25eda3870bcb`.
- The proposed branch name is `royden/feat-officer-review`.
- Current `main` at `06808cd` is not a valid branch point because it lacks
  S1-D2-02.
- If persistence is merged before branch creation, use updated `main` only after
  verifying it contains `35d5322` as an ancestor.
- Otherwise, create the feature branch directly from `35d5322` as a stacked
  branch. Its eventual pull request into `main` must wait until persistence
  lands or be rebased or updated so earlier persistence commits are not
  presented as S1-D2-03 work.
- Preserve and exclude the unrelated untracked `evals/` files.
- No feature branch has been created yet.

## Planning and authorization gates

Approval of this Mission Brief does not authorize PRD, TDD,
requirements-to-tests, production, test, screenshot, branch, commit, push,
pull-request, release-artifact, or merge work. Each later gate remains separate
and must be recorded truthfully before Developer 2 delivery begins.
