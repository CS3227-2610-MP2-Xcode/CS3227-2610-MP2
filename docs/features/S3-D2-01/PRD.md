# S3-D2-01 Claims and Verification PRD

- Status: Approved
- Draft date: 2026-09-22
- Approver: Repository owner
- Approval date: 2026-09-22
- Row-category clarification: Product owner, 2026-09-22
- TDD authorization: Not granted
- Requirements-to-Tests authorization: Not granted
- Implementation authorization: Not granted
- Branch creation authorization: Not granted
- Feature: Claims and Verification
- Workstream: Developer 2
- Approved mission brief: `docs/mission-briefs/S3-D2-01-claims-and-verification.md`
- Approved decision ledger: `docs/features/S3-D2-01/GrillingDecisions.md`
- Planning baseline: `royden/feat-officer-review-queue` at `2512a5d761759e177bc88cc75724b1226a9642ea`

## Authority and precedence

This PRD is governed, in descending order, by:

1. `AGENTS.md`.
2. The approved S3-D2-01 Mission Brief.
3. The approved S3-D2-01 Grilling Decisions.
4. Verified existing repository behaviour only where this PRD describes an inherited contract.
5. This PRD.

The repository owner's 2026-09-22 request authorizes creation of this PRD only. The older "not authorized" metadata in the Mission Brief and decision ledger records the gate state when those artifacts were written; this later request is the separate PRD authorization anticipated by both artifacts.

If an approved upstream artifact conflicts with another, work stops for owner review rather than silently changing either source. Approval of this PRD will authorize only the product requirements in this document. It will not authorize a TDD, Requirements-to-Tests artifact, branch, tests, implementation, cross-owner production edit, documentation delivery, commit, push, pull request, or merge.

## Purpose

Provide one end-to-end Claims and Verification experience in which an authenticated Student can submit and track an ownership claim arising from an eligible Desk Officer-created possible-match link, and an authenticated Desk Officer can inspect the claim and canonical reports before approving or rejecting it.

The feature records a human claim and a human decision. It does not turn a possible-match link into proof of ownership, change a report, complete item collection or return, or redesign an existing report or matching workflow.

## Actors and audience

- **Student:** views currently claimable matches associated with that Student's LOST reports, submits ownership evidence, tracks only that Student's claims, and withdraws an eligible pending claim.
- **Desk Officer:** reviews pending claims, accesses the claim and complete canonical reports within an authenticated selected-detail view, and approves or rejects one claim at a time.
- **Developer 1:** retains ownership of the canonical report domain, existing Student report workflows, initial JavaFX shell, and build/release areas.
- **Developer 2:** owns the complete claim-specific domain, persistence, application behaviour, Student and Desk Officer claim experiences, and later claim-specific tests and documentation, subject to the repository's planning and integration gates.

## Success criteria

The feature succeeds when:

1. A Student can discover only currently eligible durable possible-match links, review a privacy-safe match summary, submit valid ownership evidence after confirmation, and observe a durable Pending review claim.
2. The approved lost-report, found-report, duplicate, rejection, withdrawal, and approval cardinality rules prevent conflicting claims without exposing another Student.
3. A Student can track only that Student's retained claims, see truthful status, event times, and any safe decision reason, and withdraw an eligible claim.
4. A Desk Officer can review pending claims oldest first, inspect permitted evidence and complete current canonical reports, and decide one claim at a time after explicit confirmation.
5. The first successful terminal action is final, survives application restart, and cannot be contradicted by a repeated, stale, or simultaneous action.
6. Empty, unavailable, stale, invalid, and failed-operation states remain distinct, privacy-safe, retryable where approved, and never imply false success.
7. Claims remain separate from reports and possible-match links; existing report, report-status, report-store, matching, authentication, and unrelated workspace behaviour remains unchanged.

## Dependencies and inherited contracts

### Authentication and identity

The existing authenticated session supplies one stable `userId` and one of the two existing roles, `STUDENT` or `DESK_OFFICER`. Claims introduce no role, registration, account-administration, or persistent-login behaviour.

### Canonical reports

Claims consume Developer 1's canonical immutable Item Reports. Each report has a Report ID, Reporter ID, type, item name, category, location, occurrence date, public description, private identifying detail, status, and creation time. Claims do not add a field or status to that model and do not create a duplicate report representation.

### Possible-match links

A possible-match link is the durable, symmetric relationship created by a Desk Officer in the existing matching workflow. It remains advisory, may participate in discovery of a claimable LOST-to-FOUND pair, and is neither a claim nor proof of ownership. Claims do not change its stored meaning or persistence format.

### Persistence

Claim state must be durable independently of the strict version-one report store and the possible-match link store. At minimum, a successfully submitted Pending review claim and every later terminal outcome must survive application restart. Storage representation, path, schema, repository operations, recovery, and write mechanisms are TDD decisions.

## Glossary

- **Directional pair:** the claiming Student's LOST report followed by one FOUND report connected to it by an eligible possible-match link.
- **Available match:** a directional pair that currently satisfies every submission rule and lock rule and can therefore begin a claim.
- **Claim:** the retained claim-specific record of one Student's assertion over one directional pair, including immutable submitted evidence and lifecycle events.
- **Claim reference:** the stable user-visible reference for one claim.
- **Active claim:** a claim in **Pending review**.
- **Terminal claim:** a claim in **Approved**, **Rejected**, or **Withdrawn**.
- **Report lock:** a Claims-only eligibility restriction associated with a LOST or FOUND report. It is not a report status and does not mutate the report.
- **Canonical report unavailable:** the authoritative Item Report cannot be loaded for current verification. Retained claim history remains distinct from current report availability.
- **Terminal event time:** the approval, rejection, or withdrawal time for a terminal claim.

## Included scope

- Student Claims navigation with Available matches and My claims.
- Submission from an eligible durable possible-match link.
- One required ownership-evidence statement and its validation.
- Review and explicit confirmation before submission.
- Claim identity, lifecycle, locking, duplicate prevention, resubmission, and retention rules.
- Student claim tracking, details, decision reasons, and withdrawal.
- Desk Officer pending queue, history, filters, selected verification details, approval, and rejection.
- Durable claim outcomes and restart persistence.
- Stale, repeated, simultaneous, unavailable, failure, retry, refresh, and logout behaviour.
- Claim-specific authorization, projection, and privacy rules.
- Product integration requirements for the approved Student and Desk Officer navigation placements.

## Excluded scope

- Changes to `ItemReport`, `ReportType`, `ItemCategory`, `ReportStatus`, report creation, validation, or report-store version one.
- Changes to possible-match meaning, matching scores or rules, link persistence, or the existing Desk Officer matching workflow.
- Manual report-ID entry, Student browsing of unlinked FOUND reports, or a Student-facing matching-score or matching-reason interface.
- Automatic claim approval or rejection.
- Evidence amendment after submission.
- Claim deletion, finite retention, draft restoration, background polling, live updates, or notifications.
- Item collection, handover, return, inventory management, report closure, or a new report status.
- A new application role, authentication redesign, generic workflow engine, generic case-management system, or unrelated UI redesign.
- TDD choices such as classes, packages, interfaces, schemas, file paths, codecs, locking mechanisms, identifier algorithms, timestamp storage, dependency injection, exception mapping, JavaFX hierarchy, and test layout.

## Product requirements

### Claim lifecycle, cardinality, and locking

#### PRD-LC-001 — Keep claim identity and state separate

A claim must retain its own stable claim reference, claimant user ID, directional LOST-to-FOUND target, immutable submitted evidence, status, submission time, applicable terminal time, and applicable decision reason. Claim-specific state must remain separate from both Item Reports and possible-match links. Creating or changing a claim must not itself change a report, report status, or link.

**Sources:** Mission SC-FR-002, SH-FR-001, SH-FR-004; GD-001, GD-008, GD-009, GD-016, GD-018, GD-019, GD-020, GD-033, GD-038.

#### PRD-LC-002 — Use the approved lifecycle

The only user-facing claim statuses are **Pending review**, **Approved**, **Rejected**, and **Withdrawn**. Only Pending review is active. Approved, Rejected, and Withdrawn are terminal. Viewing, selecting, refreshing, or opening a claim must not change its lifecycle.

**Sources:** Mission SC-FR-007, DO-FR-004; GD-009.

#### PRD-LC-003 — Prevent duplicate active claims for one pair

At most one active claim may exist for the same Student and directional pair. A repeated submission attempt must create nothing and direct the Student to the existing active claim without exposing any other claimant.

**Sources:** GD-005, GD-035.

#### PRD-LC-004 — Apply the found-report lock

The first successfully and durably submitted Pending review claim locks its FOUND report against every competing claim. Rejection or withdrawal releases that lock. Approval keeps the FOUND report permanently unavailable for future claims. A failed submission acquires no lock.

**Sources:** GD-006, GD-018 and the approved derived consistency consequences.

#### PRD-LC-005 — Apply the lost-report lock

A LOST report may participate in at most one active claim. Rejection or withdrawal releases that lock so the report may be used for another otherwise eligible claim. Approval permanently prevents every later claim involving that LOST report. A failed submission acquires no lock.

**Sources:** GD-007, GD-018 and the approved derived consistency consequences.

#### PRD-LC-006 — Apply rejection closure and resubmission rules

Rejection releases the rejected claim's active LOST and FOUND report locks, but permanently prevents that claimant from claiming the same FOUND report through any LOST report. It does not prevent another Student from claiming that FOUND report, and it does not prevent the rejected Student from pursuing other FOUND reports. Consequently, rejection never permits same-pair resubmission.

**Sources:** GD-012, GD-048 and the approved derived consistency consequences.

#### PRD-LC-007 — Permit resubmission after withdrawal only

A withdrawn claim remains in history. The same Student may create a new claim for the same pair only if the durable link still exists, both canonical reports are available and otherwise eligible at the new submission time, and neither endpoint is then locked or closed. The new claim has its own reference and new submission time.

**Sources:** Mission SC-FR-002, SC-FR-007; GD-003, GD-010, GD-012, GD-014.

#### PRD-LC-008 — Close both endpoints after approval

Approval permanently closes both the target LOST report and target FOUND report to every future claim. It does not delete or change either report, alter report status, remove a possible-match link, or represent collection, handover, or return.

**Sources:** Mission SH-FR-001, SH-FR-004; GD-018 and the approved derived consistency consequences.

#### PRD-LC-009 — Make the first terminal action final

When approval, rejection, and withdrawal compete, the first action that successfully becomes durable determines the final status. Every later, repeated, stale, or simultaneous terminal attempt must change nothing, refresh the affected view, and truthfully report the claim's current terminal state.

**Sources:** Mission DO-FR-006, SH-FR-002; GD-021.

### Student discovery and claim submission

#### PRD-SC-001 — Place Claims in the Student workspace

The authenticated Student workspace must retain **Report an item** and **My reports** in their existing order and append **Claims** as the third tab. The Claims area must contain the fixed sub-tabs **Available matches** and **My claims**, with Available matches selected by default.

**Sources:** Mission SC-FR-001 and the ownership boundary; GD-004, GD-044, GD-050.

#### PRD-SC-002 — Derive Available matches from eligible durable links

An Available match must originate from a durable Desk Officer-created possible-match link. At submission time, the link must still exist, both canonical reports must be available, the authenticated Student must own the LOST report, and the other endpoint must be a FOUND report. The pair need not still satisfy the matching engine's current score or rules. The same Student may own both reports.

**Sources:** Mission SC-FR-001, SC-FR-002; GD-001, GD-002, GD-003, GD-041.

#### PRD-SC-003 — Allow claims to begin only from an actionable card

A new claim may begin only by selecting an actionable card in Available matches. Students must not manually enter Report IDs or browse unlinked FOUND reports. Opening the card must present a privacy-safe LOST-to-FOUND summary and an evidence field without creating a claim.

**Sources:** Mission SC-FR-002; GD-045 and GD-039.

#### PRD-SC-004 — Show only the approved Available-match projection

An Available-match card must show the Student's LOST item name and the FOUND item's name, category, occurrence date, and location. It must not show Report IDs, reporter identities, public descriptions, private identifying details, matching score, matching reasons, officer identity, or link metadata.

**Sources:** Mission SH-FR-003; GD-024.

#### PRD-SC-005 — Group, order, and empty Available matches

Available matches must be grouped under the Student's LOST reports. LOST-report groups are ordered from most recently submitted to least recently submitted; candidates within each group are ordered by most recent FOUND occurrence date first. If no card is available, the exact and only empty-state sentence is:

`No available matches right now.`

**Sources:** GD-036.

#### PRD-SC-006 — Hide blockers without leaking another claim

Available matches must contain only currently claimable targets. A target blocked by another Student's active claim, an approval closure, or the current Student's rejection closure must be omitted without explaining the blocker. When the Student's own active claim is the blocker, the interface must direct that Student to the existing claim rather than expose a new submission path.

**Sources:** Mission SC-FR-006, SH-FR-003; GD-005, GD-006, GD-007, GD-018, GD-035, GD-048.

#### PRD-SC-007 — Validate one ownership-evidence statement

Submission requires exactly one nonblank ownership-evidence statement that asks for owner-specific information such as distinctive marks, contents, or accessories. Leading and trailing boundaries are trimmed before validation and the 500-character maximum is applied after trimming. The trimmed statement must contain at least one visible character. Normal punctuation and line breaks are allowed; non-printing control characters other than the allowed line breaks are rejected. Invalid evidence must produce safe, actionable feedback and must not create a claim.

**Sources:** Mission SC-FR-003, SC-FR-004; GD-008, GD-040.

#### PRD-SC-008 — Review and confirm before submission

Before submission, the Student must review the selected privacy-safe LOST-to-FOUND summary together with the validated evidence and explicitly confirm submission. Cancelling or returning to edit must create nothing. Submission-time eligibility and lock rules must be checked against current authoritative state when confirmation is acted upon.

**Sources:** Mission SC-FR-002, SC-FR-004; GD-003, GD-039 and the approved derived consistency consequences.

#### PRD-SC-009 — Complete a successful or blocked submission truthfully

Success may be shown only after a new Pending review claim and its locks are durable. The claim must survive application restart, open in My claims, and affected Available matches must refresh. If current eligibility, duplicate, or lock checks fail, no claim or lock is created; the view refreshes and gives privacy-safe guidance, including direction to the Student's own existing claim where applicable.

**Sources:** Mission SC-FR-004, SC-FR-005, SH-FR-002; GD-003, GD-005, GD-006, GD-007, GD-035, GD-039 and the approved derived consistency consequences.

### Student tracking and withdrawal

#### PRD-ST-001 — Show only the Student's retained claims

My claims must show every retained claim whose stored claimant user ID matches the authenticated Student and no claim belonging to another Student. Claims are ordered from newest submission time to oldest.

**Sources:** Mission SC-FR-001, SC-FR-006, SH-FR-003; GD-019, GD-026, GD-037.

#### PRD-ST-002 — Use the approved My claims row projection

Each My claims row must show the LOST item name, FOUND item name, the FOUND report's category, current claim status, and submission time. It must not show the claim reference, evidence, decision reason, reporter identity, private report detail, matching information, officer information, or link metadata. If current canonical report data needed for a row is unavailable, the report-derived field must be shown as unavailable rather than reconstructed, and the retained claim must remain selectable. The LOST and FOUND report categories must not be assumed identical.

**Sources:** Mission SC-FR-006, SH-FR-003; GD-025, GD-038, the inherited privacy boundary, and the product-owner row-category clarification of 2026-09-22.

#### PRD-ST-003 — Show the approved Student claim detail

Selecting the Student's claim must show its stable claim reference, immutable submitted evidence, current status, privacy-safe match summary, submission time, applicable terminal event time, and any Student-visible rejection reason or supplied approval reason. The safe match summary must not exceed the Student-visible report projection permitted by PRD-SC-004. It must not expose a deciding officer or another Student.

**Sources:** Mission SC-FR-006, SH-FR-003; GD-017, GD-020, GD-025, GD-038.

#### PRD-ST-004 — Keep submitted evidence immutable

After successful submission, the Student must not replace, append, or otherwise amend the submitted ownership evidence in any status. The Desk Officer reviews the same submitted statement that the Student sees in claim detail.

**Sources:** Mission SC-FR-003; GD-011.

#### PRD-ST-005 — Offer withdrawal only for Pending review

Withdrawal must be available only on a successfully loaded claim that remains Pending review and belongs to the authenticated Student. It remains available when the original possible-match link or either canonical report is no longer available. Withdrawal requires explicit confirmation of the irreversible outcome and requires no reason.

**Sources:** Mission SC-FR-001, SC-FR-007; GD-010, GD-046, GD-049.

#### PRD-ST-006 — Complete withdrawal as a retained terminal outcome

After durable success, withdrawal sets the terminal status to Withdrawn, records the withdrawal time, releases both active report locks, leaves the claim in history, refreshes the Student view, and cannot be undone. Reports, report statuses, and possible-match links remain unchanged. A repeated or stale withdrawal follows PRD-LC-009.

**Sources:** Mission SC-FR-007, SH-FR-004; GD-010, GD-021, GD-046 and the approved derived consistency consequences.

### Desk Officer review and history

#### PRD-DO-001 — Place Claims in the Desk Officer workspace

The authenticated Desk Officer workspace must add a dedicated **Claims** tab after **Possible matches**. It must contain the fixed sub-tabs **Pending review** and **Claim history**, with Pending review selected by default.

**Sources:** Mission DO-FR-001 and the ownership boundary; GD-027, GD-044.

#### PRD-DO-002 — Build the Pending review queue

The queue must contain every and only Pending review claim, ordered from oldest submission time to newest. A resubmitted claim uses its new submission time. The queue has no filters. A successful empty load must clearly state that no claims await review; it must not be confused with an unavailable load.

**Sources:** Mission DO-FR-002; GD-013, GD-014, GD-015.

#### PRD-DO-003 — Use the approved pending-row projection

Each pending row must show only claim reference, LOST item name, FOUND item name, the FOUND report's category, and submission time. Claimant identity, evidence, report descriptions, location, and private identifying details must require selection. The LOST and FOUND report categories must not be assumed identical.

**Sources:** Mission DO-FR-003, SH-FR-003; GD-023, GD-038; product-owner row-category clarification of 2026-09-22.

#### PRD-DO-004 — Review one selected claim at a time

The pending queue must be single-select. Selecting a claim must display its verification detail but must not decide it. Every approval or rejection must be performed, confirmed, and completed individually; no bulk decision action is available.

**Sources:** Mission DO-FR-004; GD-009, GD-047.

#### PRD-DO-005 — Show authorized selected verification detail

For a selected pending claim, an authenticated Desk Officer must see the claim reference, claimant user ID, current status, submission time, immutable ownership evidence, and both complete current canonical reports. Complete report detail includes Report ID, Reporter ID, type, item name, category, location, occurrence date, public description, private identifying detail, status, and creation time. LOST and FOUND content must be clearly distinguished and read only.

**Sources:** Mission DO-FR-003; GD-019, GD-020, GD-022, GD-023, GD-038; Mission confirmed repository baseline.

#### PRD-DO-006 — Disable decisions when a canonical report is unavailable

If either canonical report is unavailable during pending review, the claim must remain Pending review. Approve and Reject must be disabled, a privacy-safe unavailable state must be shown, and explicit Retry must be available. Retained or copied report data must not substitute for either current canonical report. Removal of the original possible-match link alone must not disable review.

**Sources:** Mission DO-FR-003, SH-FR-002; GD-033, GD-034.

#### PRD-DO-007 — Build read-only Claim history

Claim history must contain every Approved, Rejected, and Withdrawn claim and no Pending review claim. It must offer exactly **All**, **Approved**, **Rejected**, and **Withdrawn** filters and order visible claims by newest terminal event time first. History is read only and remains retained even when current reports or the original link are unavailable.

**Sources:** Mission DO-FR-002; GD-013, GD-015, GD-026, GD-033, GD-037 and the approved derived consistency consequences.

#### PRD-DO-008 — Use the approved history-row projection

Each history row must show only claim reference, LOST item name, FOUND item name, the FOUND report's category, final status, and terminal event time. Claimant identity, evidence, decision reason, report descriptions, location, and private details must require selection. If current canonical report data needed for a row is unavailable, the report-derived field must be shown as unavailable rather than reconstructed, and the retained claim must remain selectable. The LOST and FOUND report categories must not be assumed identical.

**Sources:** Mission DO-FR-003, SH-FR-003; GD-038, GD-042; product-owner row-category clarification of 2026-09-22.

#### PRD-DO-009 — Show selected historical detail safely

Selecting a historical claim must show its reference, claimant user ID, immutable evidence, final status, submission time, terminal event time, and any recorded decision reason. Both complete canonical reports must be shown when currently available under the same selected-detail boundary as PRD-DO-005. If a report is unavailable, the history and retained claim data remain visible, the missing current report is marked unavailable, and no report data is reconstructed or copied into the claim.

**Sources:** Mission DO-FR-003, SH-FR-003; GD-020, GD-022, GD-026, GD-042 and the approved derived consistency consequences.

### Approval and rejection decisions

#### PRD-DC-001 — Expose decisions only for a reviewable pending claim

Approve and Reject may be enabled only for one selected claim that still exists in Pending review and whose two canonical reports are currently available. Viewing, selection, reason entry, cancellation, and retry must not decide the claim.

**Sources:** Mission DO-FR-003, DO-FR-004, DO-FR-006; GD-009, GD-034, GD-047.

#### PRD-DC-002 — Validate decision-reason text

Rejection requires a nonblank reason of at most 500 characters. Approval has an optional reason with the same trimming, visible-character, allowed-line-break, control-character, and post-trimming length rules as ownership evidence. A blank trimmed optional approval reason is treated as no reason. Invalid text must block the decision and produce safe, actionable feedback without changing the claim.

**Sources:** Mission DO-FR-005, SH-FR-002; GD-016, GD-040 and the approved derived consistency consequences.

#### PRD-DC-003 — Keep Student-visible reasons safe

A Student must see the recorded rejection reason and any supplied approval reason. Any reason shared with the Student must be Student-safe and must not reveal another claimant, another Student's claim or evidence, private FOUND report information, officer identity, matching information, or technical data.

**Sources:** Mission SC-FR-006, SH-FR-003; GD-017 and the inherited privacy boundary.

#### PRD-DC-004 — Confirm irreversible decisions

Approval and rejection must each require explicit confirmation of the selected claim and irreversible result. Rejection-reason validation must complete before the rejection confirmation is offered. Any supplied approval reason must also be valid before approval can be confirmed. Cancelling confirmation changes nothing and returns to the editable decision view.

**Sources:** Mission DO-FR-004, DO-FR-005; GD-016, GD-032, GD-040.

#### PRD-DC-005 — Complete a successful officer decision

Success may be shown only after the Approved or Rejected terminal state, applicable reason, and terminal event time are durable. The claim must then leave the pending queue, the pending selection and detail must clear, a final-status success message must appear, and the record must be immediately available in Claim history. Its locks and future eligibility must follow PRD-LC-004 through PRD-LC-008.

**Sources:** Mission DO-FR-004, DO-FR-005, SH-FR-002; GD-006, GD-007, GD-016, GD-018, GD-028, GD-048.

#### PRD-DC-006 — Retain no deciding-officer identity

The claim must not retain or display the deciding Desk Officer's identity. Decision authorization still depends on the current authenticated Desk Officer session, but neither role may see deciding-officer identity as claim history or metadata.

**Sources:** Mission DO-FR-001; GD-019.

### Authorization, privacy, identity, and time

#### PRD-PR-001 — Enforce role authorization

Only an authenticated Student may enter the Student Claims area, submit a claim, view that Student's claims, or request withdrawal. Only an authenticated Desk Officer may enter the officer Claims area, view the pending queue or claim history, inspect officer-only selected detail, or approve or reject a claim. Direct or stale navigation must not bypass these boundaries.

**Sources:** Mission SC-FR-001, DO-FR-001; approved inherited mission boundaries.

#### PRD-PR-002 — Authorize every claim by stable claimant identity

Every claim must retain the claimant's stable authenticated user ID. Student authorization and ownership checks must use that identity, including ownership of the target LOST report and access to My claims or withdrawal. Display names or a later session's incidental UI state must not grant claim access.

**Sources:** Mission users and authorization boundary, SC-FR-001; GD-003, GD-019.

#### PRD-PR-003 — Preserve Student projection exclusions

No Student-facing list, card, detail, feedback, failure, or redirect may expose another Student's identity, claim, ownership evidence, or private report detail; any matching score or matching reason; or officer, deciding-officer, or possible-match-link metadata. A Student may see only that Student's own evidence and the explicitly approved safe report fields and decision reasons.

**Sources:** Mission SC-FR-006, SH-FR-003; GD-017, GD-024, GD-025, GD-035, GD-038 and the inherited privacy boundary.

#### PRD-PR-004 — Confine officer-sensitive data to selected detail

Claimant identity, ownership evidence, report descriptions, Reporter IDs, private identifying details, and decision reasons must not appear in Desk Officer queue or history rows. They may appear only after an authenticated Desk Officer selects the applicable claim, as specified by PRD-DO-005 and PRD-DO-009.

**Sources:** Mission DO-FR-003, SH-FR-003; GD-022, GD-023, GD-042.

#### PRD-PR-005 — Display every claim event time as UTC

Students and Desk Officers must see the submission time and, when applicable, the decision or withdrawal time. Every visible claim event time must be an absolute timestamp explicitly labelled UTC. This requirement does not prescribe the stored timestamp representation or UI formatting beyond those guarantees.

**Sources:** GD-020, GD-051.

#### PRD-PR-006 — Prevent sensitive secondary disclosure

Errors, logs, diagnostics, screenshots, demonstrations, generated documentation examples, test output, and handoff summaries must not reproduce ownership evidence, private report information, another Student's identity or claim, credentials, unsafe input, storage content, paths, or implementation internals. All later tests, examples, and demonstrations must use synthetic users, reports, claims, evidence, reasons, and school data.

**Sources:** Mission SH-FR-003 and privacy/safety boundaries; approved inherited mission boundaries.

### Loading, failure, refresh, and logout

#### PRD-FL-001 — Preserve confirmed state on failed mutations

A failed submission, withdrawal, approval, or rejection must report no success, leave the last confirmed claim and lock state unchanged, preserve entered evidence or reason text, display privacy-safe failure feedback, and permit an explicit retry. No partial or uncertain result may be presented as success.

**Sources:** Mission SH-FR-002; GD-029.

#### PRD-FL-002 — Distinguish claim-load failure from empty state

When data required to load any Claims view cannot be loaded, the affected view must show an explicit unavailable state, hide claim rows and details, disable claim actions, and offer explicit Retry. A failed load must never be presented as any corresponding successful empty state and must not leave old rows presented as current.

**Sources:** Mission SH-FR-002; GD-030.

#### PRD-FL-003 — Refresh from authoritative state

Each Claims view must reload when the user enters it, provide an explicit Refresh action, and refresh after a completed state-changing action. A stale or repeated terminal attempt must also refresh to the current terminal state. A refresh or retry that fails follows PRD-FL-002.

**Sources:** Mission SH-FR-002; GD-021, GD-030, GD-043.

#### PRD-FL-004 — Provide no background update channel

Sprint 3 Claims must not poll, live-update, or send notifications. A user may observe external changes only through view entry, explicit Refresh or Retry, or the refresh that follows an action.

**Sources:** GD-043.

#### PRD-FL-005 — Warn about and clear unsaved state on logout

Logout must warn when unsaved ownership evidence or decision-reason text exists. If logout is cancelled, the current Claims view and text remain. Confirmed logout must clear claim lists, selected detail, private report content, and unsaved text before returning to login. A later login performs a fresh load and does not restore drafts or selection.

**Sources:** Mission authentication boundary and SH-FR-003; GD-031, GD-043.

### Compatibility and product integration

#### PRD-IC-001 — Preserve canonical report behaviour

Claim submission, withdrawal, approval, rejection, loading, and history must not add claim state to or otherwise mutate Item Reports, ReportType, ItemCategory, ReportStatus, report-store version one, report creation, validation, Student report submission/history, or Desk Officer report review.

**Sources:** Mission SH-FR-001, SH-FR-004, explicit non-goals and ownership boundaries; approved inherited mission boundaries.

#### PRD-IC-002 — Preserve possible-match behaviour

Claims must consume existing durable possible-match links without changing their semantics or persistence format. Link removal after claim submission must not change the submitted claim, its reviewability, or its retained history; it affects only future submission eligibility. A claim decision must not create, remove, or relabel a link.

**Sources:** Mission SC-FR-002, SH-FR-004; GD-002, GD-018, GD-033.

#### PRD-IC-003 — Retain claims indefinitely without deletion

All claims in every status must be retained indefinitely for Sprint 3. Neither Student nor Desk Officer has a claim-deletion action. Removing a report or link must not delete retained claim history.

**Sources:** GD-026, GD-033 and the approved derived consistency consequences.

#### PRD-IC-004 — Treat navigation placement as a gated integration surface

The product must satisfy the exact Student and Desk Officer tab placements in PRD-SC-001 and PRD-DO-001 while preserving all existing tab ordering and behaviour. If delivery requires a production edit to Developer 1-owned initial shell or another shared composition file, the later TDD and implementation handoff must identify the exact file, reason, smallest sufficient edit, and whether Developer 1 can make it instead. That edit requires separate explicit authorization before implementation.

**Sources:** Mission ownership and integration boundaries; GD-027, GD-050.

## Non-functional product requirements

### PRD-NF-001 — Durable restart truthfulness

After any submission or terminal action reports success, a fresh application run must show the same claim, status, immutable evidence, claimant identity, times, applicable reason, and eligibility consequences. A result that is not durable must not be reported as successful.

**Sources:** Mission SC-FR-005, SH-FR-002 and persistence constraints; GD-029.

### PRD-NF-002 — Enforce competing outcomes consistently

When submissions compete for a LOST or FOUND report, no observable outcome may contain two claims that both succeeded in violation of PRD-LC-003 through PRD-LC-005. When terminal actions compete, no observable outcome may contain more than the one final status required by PRD-LC-009. The TDD decides the technical enforcement mechanism.

**Sources:** Mission DO-FR-006, SH-FR-002; GD-005, GD-006, GD-007, GD-021 and the approved derived consistency consequences.

### PRD-NF-003 — Keep ordered views reproducible

Given the same authoritative claim and report data, Available matches, My claims, Pending review, and Claim history must satisfy their approved ordering rules. This PRD does not impose an additional user-visible priority between entries whose approved ordering values are equal.

**Sources:** GD-014, GD-036, GD-037.

### PRD-NF-004 — Preserve scope, compatibility, and ownership

The feature must reuse the existing authentication, canonical report, report persistence, and possible-match contracts without shadow types or silent contract changes. Any future observable behaviour change returns to product review; any cross-owner production change follows PRD-IC-004 and the repository approval gate.

**Sources:** Mission confirmed baseline, explicit non-goals, ownership and integration boundaries; approved inherited mission boundaries.

## Main scenarios and extensions

### SC-001 — View and open an Available match

1. An authenticated Student enters Claims; Available matches is selected.
2. The view reloads current reports, durable links, claims, locks, and closures.
3. Only currently claimable cards appear under the Student's LOST reports in the approved order and projection.
4. The Student opens one card and sees a safe LOST-to-FOUND summary plus the evidence field.

Extensions:

- If there is no card, the exact approved empty sentence appears.
- If loading fails, the unavailable state and Retry appear instead of the empty sentence.
- A hidden blocker reveals no other claimant; the Student's own active blocker directs to that existing claim.

### SC-002 — Submit a valid claim

1. The Student enters evidence that passes PRD-SC-007.
2. The Student reviews the safe match summary and trimmed evidence.
3. The Student explicitly confirms submission.
4. Current link, reports, ownership, direction, duplicate, and lock conditions still pass.
5. Durable submission succeeds and the claim becomes Pending review.
6. My claims opens on the new claim and Available matches refreshes.
7. A later application run shows the same pending claim.

Extensions:

- Cancelling confirmation returns to editing and creates nothing.
- Invalid evidence remains editable and creates nothing.
- A stale link, unavailable report, duplicate, or lock conflict creates nothing, refreshes current state, and gives privacy-safe guidance.
- A persistence failure preserves evidence, reports no success, changes no confirmed lock state, and permits retry.

### SC-003 — Track and withdraw a claim

1. The authenticated Student opens My claims and sees only that Student's claims, newest submission first.
2. Selecting a claim shows the approved safe detail and UTC-labelled event times.
3. For a Pending review claim, the Student chooses withdrawal and confirms the irreversible outcome without entering a reason.
4. Durable withdrawal succeeds, releases both active locks, records Withdrawn and its event time, and refreshes the view.

Extensions:

- Missing reports or link do not prevent withdrawal of a loaded pending claim.
- A terminal or concurrently decided claim changes nothing and refreshes to its current terminal status.
- A failure preserves Pending review and permits explicit retry.

### SC-004 — Review the pending queue

1. An authenticated Desk Officer enters Claims; Pending review is selected.
2. The queue loads Pending review claims oldest first.
3. The officer selects one row.
4. The officer sees the claim evidence, claimant user ID, and both complete canonical reports in read-only verification detail.

Extensions:

- A successful empty load communicates that no claims await review.
- A load failure shows unavailable and Retry, not the empty state.
- If either report is unavailable, the claim remains pending, both decisions are disabled, and Retry is offered without substituting copied report data.
- Removing the original link does not make the pending claim unreviewable.

### SC-005 — Approve a claim

1. The officer selects a reviewable Pending review claim.
2. The officer leaves the approval reason blank or supplies a valid optional reason.
3. The officer explicitly confirms approval.
4. The claim is still pending and both canonical reports are available.
5. Durable approval succeeds.
6. The claim leaves the pending queue, detail clears, final-status success is shown, and the claim appears immediately in history.
7. Both report endpoints are permanently closed to future claims, while reports and links remain unchanged.

Extensions:

- Invalid optional text blocks confirmation and changes nothing.
- A failed action preserves text and pending state and permits retry.
- A stale or competing terminal action changes nothing and refreshes to the current final status.

### SC-006 — Reject a claim

1. The officer selects a reviewable Pending review claim.
2. The officer enters a valid mandatory Student-safe rejection reason.
3. Validation succeeds before explicit rejection confirmation.
4. Durable rejection succeeds.
5. The claim leaves the pending queue, detail clears, final-status success is shown, and the claim appears immediately in history.
6. Active report locks release, while the claimant-to-FOUND rejection closure prevents that Student from claiming the same FOUND report again.

Extensions:

- Missing, blank, over-limit, or control-character-containing text blocks the decision and remains editable.
- Failure, stale state, and competing terminal action follow the same truthful behaviour as SC-005.

### SC-007 — Inspect claim history

1. The Desk Officer opens Claim history.
2. Approved, Rejected, and Withdrawn claims appear newest terminal event first.
3. The officer may apply exactly one of the four approved filters.
4. Selecting a row shows retained claim detail, reason when applicable, and complete current canonical reports when available.

Extensions:

- If a current report is unavailable, retained claim history remains visible and the report is marked unavailable without reconstruction.
- A load failure replaces rows and detail with unavailable and Retry rather than an empty history.

## Acceptance criteria

### Student acceptance

- **PRD-AC-S01 — Available matches:** Entry loads only currently eligible durable-link targets, uses the approved projection, grouping, ordering, and exact empty sentence, and exposes no prohibited data.
- **PRD-AC-S02 — Open an eligible match:** A Student can begin only from an actionable card and sees a safe directional summary and evidence field with no claim created by opening it.
- **PRD-AC-S03 — Evidence validation:** Blank, over-500-character, and prohibited-control-character evidence is rejected after approved trimming; valid punctuation and line breaks are accepted; invalid input creates no claim.
- **PRD-AC-S04 — Review and confirmation:** The Student reviews the selected summary and evidence, may cancel without mutation, and must explicitly confirm before submission.
- **PRD-AC-S05 — Successful submission:** A valid confirmed submission creates one durable Pending review claim, applies both locks, opens it in My claims, refreshes Available matches, and survives restart.
- **PRD-AC-S06 — Invalid or blocked submission:** A stale link, unavailable or misdirected report, non-owned LOST report, duplicate, or locked endpoint creates nothing, changes no lock, refreshes state, and reveals no other claimant.
- **PRD-AC-S07 — Personal tracking:** My claims shows only the authenticated Student's retained claims newest first, with the approved row and detail projections and stable reference only in detail.
- **PRD-AC-S08 — Terminal decisions:** Approved and Rejected detail shows the final status, UTC-labelled event time, and applicable Student-safe decision reason; no officer identity or prohibited report/matching data appears.
- **PRD-AC-S09 — Withdrawal:** A loaded owned Pending review claim can be withdrawn after confirmation without a reason even if its reports or link are unavailable; it becomes durable Withdrawn history and releases both locks.
- **PRD-AC-S10 — Terminal withdrawal attempt:** A terminal or concurrently decided claim cannot be withdrawn again; the attempt changes nothing and refreshes to the truthful final status.

### Desk Officer acceptance

- **PRD-AC-D01 — Pending queue:** A successful load shows every and only Pending review claim oldest first with no filters and the approved row fields; empty and unavailable states are distinct.
- **PRD-AC-D02 — Selected verification:** Selecting one claim shows authorized claim data, immutable evidence, claimant user ID, and both complete current canonical reports without deciding the claim or exposing sensitive data in the row.
- **PRD-AC-D03 — Unavailable report:** When either canonical report is unavailable, the claim stays Pending review, Approve and Reject are disabled, copied report data is not substituted, and Retry is available.
- **PRD-AC-D04 — Approval:** A confirmed approval with absent or valid optional reason durably produces Approved, removes the claim from Pending review, clears selection, shows final-status success, places it in history, and permanently closes both endpoints.
- **PRD-AC-D05 — Rejection:** A confirmed rejection with a valid mandatory safe reason durably produces Rejected, releases active locks, enforces the claimant-to-FOUND closure, and moves the claim from queue to history.
- **PRD-AC-D06 — Reason validation:** Rejection cannot reach confirmation with a missing or invalid reason; invalid supplied approval text likewise blocks approval. No invalid reason changes the claim.
- **PRD-AC-D07 — Stale and repeated decisions:** After the first terminal action succeeds, every repeated, stale, or simultaneous action changes nothing, clears no retained history, and refreshes to the current final status.
- **PRD-AC-D08 — Claim history:** History shows only terminal claims newest terminal event first, supports exactly the approved filters and row projection, and provides read-only selected detail.
- **PRD-AC-D09 — Individual review:** The queue is single-select and exposes no bulk approval or rejection path; every decision is separately inspected and confirmed.

### Shared acceptance

- **PRD-AC-X01 — Locking and cardinality:** Competing submissions cannot both succeed when they would violate the pair, LOST, or FOUND rules; rejection, withdrawal, and approval produce exactly their approved release or closure consequences.
- **PRD-AC-X02 — Restart persistence and retention:** Pending and terminal claims, evidence, claimant identity, status, times, reason, and eligibility consequences survive restart; every claim remains retained and neither role can delete one.
- **PRD-AC-X03 — Privacy and authorization:** Cross-role and cross-Student access is denied, each list/detail projection contains only approved data, and no sensitive claim or report content appears in secondary outputs.
- **PRD-AC-X04 — Truthful failure and recovery:** Failed mutations preserve the last confirmed state and unsaved text, report no success, and permit retry; failed loads hide stale rows and show unavailable plus Retry rather than an empty state.
- **PRD-AC-X05 — Existing-contract preservation:** Claim operations leave every Item Report, ReportStatus, report-store-v1 document, and possible-match link unchanged and do not imply collection, handover, return, or automatic ownership proof.
- **PRD-AC-X06 — Refresh and logout:** Claims reload on entry, Refresh is explicit, post-action views reconcile, no polling or notifications occur, and confirmed logout clears lists, selection, private content, and drafts after warning about unsaved text.

## Requirement index

| Requirement group | Requirement IDs | Primary acceptance evidence |
| --- | --- | --- |
| Lifecycle, cardinality, and locking | PRD-LC-001–PRD-LC-009 | PRD-AC-S05, PRD-AC-S06, PRD-AC-S09, PRD-AC-S10, PRD-AC-D04, PRD-AC-D05, PRD-AC-D07, PRD-AC-X01, PRD-AC-X02, PRD-AC-X05 |
| Student discovery and submission | PRD-SC-001–PRD-SC-009 | PRD-AC-S01–PRD-AC-S06, PRD-AC-X03, PRD-AC-X04, PRD-AC-X06 |
| Student tracking and withdrawal | PRD-ST-001–PRD-ST-006 | PRD-AC-S07–PRD-AC-S10, PRD-AC-X02, PRD-AC-X03, PRD-AC-X06 |
| Desk Officer review and history | PRD-DO-001–PRD-DO-009 | PRD-AC-D01–PRD-AC-D03, PRD-AC-D08, PRD-AC-D09, PRD-AC-X03, PRD-AC-X04 |
| Approval and rejection | PRD-DC-001–PRD-DC-006 | PRD-AC-D04–PRD-AC-D07, PRD-AC-D09, PRD-AC-X01, PRD-AC-X04 |
| Authorization, privacy, identity, and time | PRD-PR-001–PRD-PR-006 | PRD-AC-S01, PRD-AC-S07, PRD-AC-S08, PRD-AC-D02, PRD-AC-D08, PRD-AC-X03 |
| Loading, failure, refresh, and logout | PRD-FL-001–PRD-FL-005 | PRD-AC-S06, PRD-AC-S10, PRD-AC-D01, PRD-AC-D03, PRD-AC-D07, PRD-AC-X04, PRD-AC-X06 |
| Compatibility and product integration | PRD-IC-001–PRD-IC-004 | PRD-AC-X02, PRD-AC-X05, PRD-AC-X06; later integration approval evidence for PRD-IC-004 |
| Non-functional product guarantees | PRD-NF-001–PRD-NF-004 | PRD-AC-S05, PRD-AC-D04, PRD-AC-D05, PRD-AC-X01–PRD-AC-X05 |

Each substantive requirement records its Mission Brief and/or Grilling Decision source immediately below the requirement. This index maps every requirement group to later acceptance evidence without choosing test classes, seams, or implementation mechanisms.

## Product integration requirements

The feature has two approved integration outcomes:

1. Append Student Claims after My reports without changing the existing Report an item or My reports behaviour.
2. Add Desk Officer Claims after Possible matches without changing the existing report-review or matching behaviour.

These outcomes may require shared or Developer 1-owned shell/composition work. No such production edit is authorized at this stage. The later TDD must identify the smallest exact integration surfaces, and later implementation requires the repository owner's explicit authorization for each cross-owner file change or assignment of that edit to Developer 1.

## Intentional TDD decisions

The following remain for the separately authorized TDD and must not change an observable requirement in this PRD:

- claim classes, records, packages, and service boundaries;
- repository interfaces, operations, result types, and method signatures;
- claim storage schema, format, version, path, encoding, and codecs;
- atomicity, file-locking, write, recovery, and concurrency mechanisms;
- claim-reference generation and internal identity representation;
- timestamp storage representation and clock seams;
- JavaFX classes, controls, hierarchy, styling, and dependency injection;
- exception taxonomy and UI error mapping;
- automated-test organization, deterministic fault seams, and red-green order;
- exact internal ordering tie-breaks that do not add a user-visible priority beyond PRD-NF-003; and
- exact production files and smallest changes for the gated navigation integration surfaces.

Any TDD proposal that changes eligibility, projections, lifecycle, lock or closure consequences, ordering priorities, privacy, confirmation, failure, refresh, retention, or integration outcomes must return to PRD review first.

## Requirements review outcome

This draft was reviewed against the complete approved Mission Brief and all approved Grilling Decisions GD-001 through GD-051, including the duplicate GD-037, GD-038, and GD-044 entries that intentionally govern both role-specific experiences.

The review confirmed that:

- every approved decision is represented by a requirement, scope boundary, or approved derived consequence;
- a possible-match link remains distinct from a claim, and current report data remains distinct from retained historical claim data;
- Pending review remains the only active state and each terminal status has its approved lock, closure, history, and repeated-action behaviour;
- rejection closure is broader than pair-level resubmission and applies to the claimant-to-FOUND relationship exactly as approved;
- Student projections exclude identities, private details, matching data, and officer/link metadata, while officer-sensitive data remains selected-detail only;
- no exact interface sentence other than `No available matches right now.` has been made binding;
- no storage, class, interface, algorithm, JavaFX, exception, or test-design decision has leaked into the product requirements; and
- the only identified cross-owner integration surfaces are the approved Student and Desk Officer navigation placements.

No contradiction or unresolved product decision was found. Equal-value ordering ties receive no additional user-visible priority because none was approved; the later TDD may choose a reproducible internal tie-break without changing the approved primary ordering.

## Approval and next gate

The repository owner explicitly approved this complete PRD on 2026-09-22. Approval authorizes only the product requirements in this document.

No TDD, Requirements-to-Tests artifact, branch, test, implementation, User Guide, Developer Guide, diagram, log, reflection, commit, push, pull request, or merge is authorized by this approval.
