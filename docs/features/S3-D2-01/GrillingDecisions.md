# S3-D2-01 Claims and Verification Grilling Decisions

- Status: Approved
- Grilling status: Complete
- Decision date: 2026-09-22
- Approver: Repository owner
- Approval date: 2026-09-22
- Feature: Claims and Verification
- Workstream: Developer 2
- Approved mission brief: `docs/mission-briefs/S3-D2-01-claims-and-verification.md`
- Product requirements document: Not created or authorized
- Planning baseline: `royden/feat-officer-review-queue` at
  `2512a5d761759e177bc88cc75724b1226a9642ea`

## Purpose and authority

This ledger records the repository-owner-approved product decisions for Sprint
3 Claims and Verification. It is the completed Grilling Decisions artifact for
mission S3-D2-01.

The repository owner completed the product grilling, approved the complete
decision ledger, and then separately authorized writing only this artifact on
2026-09-22. That authorization does not extend to a PRD, TDD,
Requirements-to-Tests artifact, implementation, tests, documentation, branch,
commit, push, pull request, merge, dependency, build configuration, or other
repository change.

## Inherited mission boundaries

The following constraints are inherited from the approved Mission Brief and
were not reopened during grilling:

- Claims are a separate domain from Item Reports and possible-match links.
- Only an authenticated Student may submit, track, or withdraw that Student's
  claims.
- Only an authenticated Desk Officer may access the claim queue, inspect
  officer-only claim information, or approve or reject a claim.
- The stable authenticated `userId` is the available claimant identity.
- A possible-match suggestion or link is advisory and never proves ownership.
- Claim behavior does not mutate `ItemReport`, `ReportType`, `ItemCategory`,
  `ReportStatus`, report-store version one, or possible-match semantics.
- Reports currently have only `SUBMITTED` and `UNDER_REVIEW` statuses.
- Item collection, handover, return, inventory management, and notifications
  beyond in-application tracking remain outside Sprint 3.
- Students never receive another Student's identity, claim, ownership evidence,
  private report detail, matching score, matching reasons, or officer metadata.
- Tests, documentation, screenshots, logs, demonstrations, and handoffs use
  only synthetic data and do not expose private evidence or report information.
- Technical architecture, persistence schema and path, repository interfaces,
  filesystem protocol, JavaFX hierarchy, dependency injection, concurrency
  mechanism, and test-class organization remain TDD decisions.

## Confirmed claim target and eligibility decisions

- **GD-001 — Claim target:** A claim immutably targets a directional pair from
  the claiming Student's LOST report to a FOUND report. A suggestion or durable
  link is discovery context and is not the claim's identity.
- **GD-002 — Eligible source:** Only a durable Desk Officer-created
  possible-match link may initiate a claim. Algorithmic suggestions remain
  officer-only and cannot be claimed directly.
- **GD-003 — Submission-time eligibility:** At submission, the link must still
  exist, both reports must be available, the authenticated Student must own the
  LOST report, and the other endpoint must be a FOUND report. The pair need not
  still satisfy the automated matching score or rules.
- **GD-041 — Same-reporter eligibility:** A Student may submit a claim even
  when the same Student owns both the LOST and FOUND reports. The normal
  evidence and officer-decision workflow still applies.
- **GD-045 — Target selection:** A new claim may begin only from an actionable
  card in **Available matches**. Students cannot manually enter report IDs or
  browse unlinked found reports.

## Confirmed cardinality, locking, and resubmission decisions

- **GD-005 — Duplicate active pair:** At most one active claim may exist for
  the same Student and directional pair. A repeated attempt creates nothing
  and directs the Student to the existing claim.
- **GD-006 — Found-report lock:** The first successfully submitted claim locks
  its found report. Other claims targeting that report are blocked while the
  claim is Pending review. Rejection or withdrawal releases the report;
  approval keeps it unavailable for future claims.
- **GD-007 — Lost-report lock:** A lost report may have only one active claim.
  Rejection or withdrawal releases it for another eligible claim; approval
  permanently prevents further claims involving that lost report.
- **GD-012 — Same-pair resubmission:** A withdrawn claim remains in history. A
  new claim for the same pair is allowed if the link remains eligible and both
  reports are unlocked. Rejection does not permit resubmission.
- **GD-018 — Approval closure:** Approval permanently closes both the LOST and
  FOUND report endpoints to every future claim. Existing reports, statuses,
  and possible-match links remain unchanged. Approval does not imply
  collection, handover, or return.
- **GD-048 — Rejection scope:** Rejection prevents that Student from claiming
  the same found report through any lost report. Other Students may still
  claim the found report, and the rejected Student may pursue other found
  reports. This broadens the pair-level rejection rule recorded in GD-012.

## Confirmed evidence and validation decisions

- **GD-008 — Required ownership evidence:** Every claim contains one required,
  nonblank ownership-evidence statement of at most 500 characters. The prompt
  asks for owner-specific information such as distinctive marks, contents, or
  accessories.
- **GD-011 — Evidence amendment:** Submitted evidence is immutable. A Student
  cannot replace or append evidence while the claim is pending.
- **GD-040 — Text validation:** Evidence and decision-reason text is trimmed at
  its leading and trailing boundaries. A mandatory field must contain at least
  one visible character. Normal punctuation and line breaks are allowed,
  non-printing control characters are rejected, and the 500-character limit
  is applied after trimming. Optional approval text follows the same rules when
  supplied.

## Confirmed lifecycle and Student-action decisions

- **GD-009 — Status vocabulary:** The user-facing statuses are **Pending
  review**, **Approved**, **Rejected**, and **Withdrawn**. Only Pending review
  is active; the others are terminal. Viewing or selecting a claim has no
  lifecycle effect.
- **GD-010 — Withdrawal:** A Student may withdraw only a Pending review claim.
  Withdrawal requires explicit irreversible-action confirmation, produces the
  terminal Withdrawn status, releases both report locks, remains in history,
  and cannot be undone.
- **GD-046 — Withdrawal reason:** Withdrawal requires no reason. The claim
  records its Withdrawn status and withdrawal time.
- **GD-049 — Withdrawal with unavailable context:** A Student may withdraw a
  claim that successfully loads and remains Pending review even if its link or
  either report is unavailable.
- **GD-039 — Submission confirmation:** Before submission, the Student reviews
  the selected LOST-to-FOUND summary and entered evidence and explicitly
  confirms. After durable success, the new claim opens in **My claims** and
  affected available matches refresh.

## Confirmed decision behavior

- **GD-016 — Decision reasons:** Rejection requires a nonblank reason of at
  most 500 characters. An approval reason is optional and, when supplied,
  follows the same limit and validation.
- **GD-017 — Student-visible reasons:** A Student sees the rejection reason or
  supplied approval reason. The shared reason must be Student-safe and must not
  reveal another claimant or private found-report information.
- **GD-032 — Decision confirmation:** Approval and rejection both require
  explicit confirmation of the irreversible outcome. Rejection-reason
  validation occurs before confirmation.
- **GD-047 — Individual review:** The queue is single-select. Every claim is
  inspected, confirmed, and decided individually; there are no bulk decisions.
- **GD-021 — Repeated, stale, and simultaneous actions:** The first successful
  approval, rejection, or withdrawal becomes final. Every later or stale
  terminal action changes nothing, refreshes the view, and truthfully reports
  the current terminal state.
- **GD-028 — Successful officer decision:** After a successful decision, the
  claim leaves the pending queue, selection and detail clear, a final-status
  success message appears, and the record is immediately available in history.

## Confirmed post-submission context behavior

- **GD-033 — Link removal:** Removing a possible-match link after submission
  does not change the claim. A pending claim remains reviewable and a terminal
  claim remains historical. Link state affects only future submissions.
- **GD-034 — Unavailable canonical report:** If either report is unavailable
  during officer review, the claim remains Pending review, Approve and Reject
  are disabled, a privacy-safe unavailable message appears, and Retry is
  available. Copied report data does not substitute for the canonical reports.

## Confirmed Student experience decisions

- **GD-004 — Student Claims area:** The Student workspace has a dedicated
  **Claims** tab containing **Available matches** and **My claims**.
- **GD-024 — Available-match projection:** An available-match card shows the
  Student's lost item name and the found item's name, category, occurrence
  date, and location. It excludes report IDs, reporter identities,
  descriptions, private identifying details, matching score and reasons, and
  officer or link metadata.
- **GD-025 — Personal tracking projection:** A **My claims** row shows lost and
  found item names, category, status, and submission time. Selecting the claim
  shows its reference, submitted evidence, safe match summary, relevant event
  times, and any Student-visible decision reason.
- **GD-035 — Hidden blockers:** **Available matches** contains only targets the
  Student can currently claim. Targets blocked by another Student, approval,
  or rejection are omitted without revealing why. When the Student's own
  active claim is the blocker, the interface directs that Student to the
  existing claim.
- **GD-036 — Available-match grouping and order:** Matches are grouped under
  the Student's lost reports. The most recently submitted lost reports appear
  first, and candidates within each group use the most recent found occurrence
  date first. The empty state says only `No available matches right now.`
- **GD-037 — Student-history order:** **My claims** is ordered by newest
  submission first.
- **GD-038 — Student-visible claim reference:** A stable claim reference
  appears in Student claim details but not in available-match cards or Student
  summary rows.
- **GD-044 — Student Claims sub-tabs:** **Available matches** and **My claims**
  are fixed sub-tabs. Available matches is selected by default.
- **GD-050 — Student navigation placement:** The Student tabs remain **Report
  an item**, **My reports**, then **Claims**. Claims is appended as the third
  tab so existing report behavior and ordering remain unchanged.

## Confirmed Desk Officer experience decisions

- **GD-013 — Queue and history membership:** The primary queue contains only
  Pending review claims. A separate read-only history contains Approved,
  Rejected, and Withdrawn claims. The pending empty state clearly states that
  no claims await review.
- **GD-014 — Pending order:** Pending claims are ordered from oldest submission
  to newest. A new claim submitted after withdrawal receives a new submission
  time and enters at the corresponding position.
- **GD-015 — Filters:** The pending queue has no filters. Claim history has
  exactly **All**, **Approved**, **Rejected**, and **Withdrawn** filters.
- **GD-022 — Selected verification detail:** Selecting a claim gives an
  authenticated Desk Officer the claim evidence and both complete canonical
  reports, including reporter IDs and private identifying details.
- **GD-023 — Pending queue-row projection:** A row shows only claim reference,
  lost and found item names, category, and submission time. Claimant identity,
  evidence, descriptions, location, and private details require selection.
- **GD-027 — Officer navigation placement:** The Desk Officer workspace has a
  dedicated **Claims** tab after **Possible matches**.
- **GD-037 — Officer-history order:** Claim history is ordered by newest
  terminal event first.
- **GD-038 — Officer-visible claim reference:** The stable claim reference
  appears in the pending queue and selected pending or historical detail.
- **GD-042 — Officer history-row projection:** A history row shows only claim
  reference, lost and found item names, category, final status, and terminal
  event time. Identity, evidence, reasons, and complete reports require
  selection.
- **GD-044 — Officer Claims sub-tabs:** **Pending review** and **Claim history**
  are fixed sub-tabs. Pending review is selected by default.

## Confirmed identity, time, retention, and privacy decisions

- **GD-019 — Stored actor identity:** A claim retains the claimant's stable
  authenticated user ID. It does not retain or display the deciding officer's
  identity.
- **GD-020 — Visible event times:** Students and Desk Officers see the
  submission time and, when applicable, the decision or withdrawal time.
- **GD-051 — Time presentation:** Every claim event time is displayed as an
  absolute, explicitly labelled UTC timestamp to both roles. Storage
  representation remains a TDD decision.
- **GD-026 — Retention and deletion:** All claims, including rejected and
  withdrawn claims, are retained indefinitely for Sprint 3. Neither role has a
  claim-deletion action.
- **GD-031 — Logout and drafts:** Logout warns when unsaved evidence or
  decision text exists. Confirmed logout clears claim lists, selected details,
  and unsaved text. Drafts are not restored after a later login.

## Confirmed loading, failure, and refresh decisions

- **GD-029 — Failed state-changing operation:** Failed submission, withdrawal,
  approval, or rejection leaves the last confirmed state unchanged, preserves
  entered evidence or reason text, displays privacy-safe failure feedback, and
  permits explicit retry. It never reports false success.
- **GD-030 — Claim-load failure:** A load failure produces an explicit
  unavailable state, hides claim rows and details, disables claim actions, and
  offers Retry. It is never presented as an empty queue or empty history.
- **GD-043 — Refresh behavior:** A Claims view reloads when the user enters it,
  provides explicit Refresh, and refreshes after that user completes an action.
  Sprint 3 has no background polling, live updates, or notifications.

## Derived consistency consequences

The following consequences follow directly from the approved decisions and do
not add new product behavior:

- A claim lock begins only after successful durable submission. If concurrent
  submissions compete for a lost or found report, the first successful one
  obtains the lock and later attempts fail without exposing another claimant.
- Rejection releases the rejected claim's two active locks, closes the
  claimant-to-found relationship described in GD-048, and leaves reports and
  possible-match links unchanged.
- Withdrawal releases both active locks and leaves reports and possible-match
  links unchanged. Resubmission still requires the link and reports to satisfy
  GD-003.
- Approval closes both report endpoints within Claims but does not create a
  report status, remove a possible-match link, or represent item return.
- A blank optional approval reason produces no Student-visible reason. Status
  and event time remain visible.
- Terminal history remains available even when current report data cannot be
  loaded; unavailable canonical report content is neither reconstructed nor
  copied into the claim.

## Copy provenance

Except for the explicitly approved available-match empty sentence in GD-036,
the repository owner approved the required meaning, fields, privacy boundary,
and interaction behavior rather than final interface sentences. A later PRD
may propose concise implementation-neutral copy only if separately authorized;
that copy must preserve this ledger.

## Intentional technical deferrals

A later, separately authorized TDD may decide claim classes and packages,
repository operations, JSON or other storage schema, storage path, codecs,
durability and recovery mechanisms, concurrency enforcement, identifier
generation, time representation, application-service boundaries, JavaFX class
hierarchy, dependency injection, exception mapping, and test organization.

Those choices must preserve this ledger's observable behavior, privacy rules,
locking semantics, retained history, report and matching boundaries, and
truthful failure outcomes. They may not add statuses, report mutations,
possible-match mutations, drafts, deletion, collection, return, notifications,
or another role without returning to approved product planning.

## Completion and authorization boundary

No product decision or owner response assigned to Grilling Decisions remains
unresolved. The ledger was explicitly approved by the repository owner on
2026-09-22 and is sufficient for a later PRD request.

This approval and the authorization to write this file do **not** authorize a
PRD, TDD, Requirements-to-Tests artifact, other documentation, implementation,
tests, production changes, cross-owner edits, branch creation, commits,
publishing, or merging.
