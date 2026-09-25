# Sprint 3 Claims → Sprint 4 Appointment Handover

## 1. Purpose

This handover defines only the Claims-to-Appointment dependency for Developer
1's Sprint 4 Student appointment work. It records what the implemented Sprint
3 Claims feature guarantees, what may safely cross the ownership boundary, and
the smallest missing integration capability.

Sprint 3 verifies ownership. Sprint 4 appointments are a separate downstream
domain:

```text
Report / Match
      ↓
    Claim
      ↓
  APPROVED
      ↓
 Appointment
      ↓
Collection / Handover
```

This document does not decide or implement appointment lifecycle, storage, UI,
slot management, collection completion, or tests.

## 2. Current Sprint 3 implementation contract

The current implementation has a separate, durable Claim domain. A `Claim`
retains exactly:

- `ClaimId claimId`;
- `String claimantUserId`;
- `UUID lostReportId`;
- `UUID foundReportId`;
- immutable ownership evidence;
- `ClaimStatus status`;
- `Instant submittedAt`;
- optional `Instant terminalAt`; and
- optional decision reason.

`ClaimStatus` contains exactly `PENDING_REVIEW`, `APPROVED`, `REJECTED`, and
`WITHDRAWN`. Only `PENDING_REVIEW` is active; all other states are terminal.
The first durable terminal action wins. A terminal Claim cannot transition
again.

`ClaimId` wraps the persisted UUID. Its stable visible reference is derived as
`CLM-` plus the UUID's 32 uppercase hexadecimal digits without hyphens. The
reference is a display identifier, not an authorization credential.

For an `APPROVED` Claim, `terminalAt` is required and is the approval time. The
Claim also retains the claimant's stable authenticated user ID and the
directional LOST and FOUND Report IDs. Approval does not mutate either Item
Report, its `ReportStatus`, or the possible-match link. Approval also does not
mean that collection, handover, or return has occurred.

The public `ClaimRepository` currently provides `loadAll`, atomic `submit`, and
atomic `withdraw`, `approve`, and `reject` commands. `JsonClaimRepository` is
the current implementation. `FindersKeepersApp` creates one shared
application-lifetime Claim repository and passes it to `ClaimWorkspaceFactory`,
which creates fresh authenticated Student and Desk Officer Claim workspaces.
The Student Claims feature is injected into the existing Student workspace as
the third top-level tab.

`StudentClaimsService` is bound to one authenticated Student. Its public
presentation state shows only that Student's Claims, but it is a stateful
Claims UI workflow, not a neutral downstream integration API. Its row handles
are intentionally opaque. Selected Student detail exposes the visible Claim
reference, status, submission/terminal times, the Student's own evidence and
decision reason, and a safe current report summary.

The latest officer-review correction removed the old `Start review` mutation.
The active report-review queue now accepts both `SUBMITTED` and `UNDER_REVIEW`
reports and excludes report endpoints associated with Approved Claims through
`ApprovedClaimReportService.loadApprovedReportIds()`. That service returns only
an immutable set of approved Claim report endpoint UUIDs for report-queue
visibility. It does not identify the Claim, claimant, or approval time and
cannot authorize appointment booking.

Reports excluded from the active officer report-review queue remain canonical
Item Reports. Queue exclusion must not be interpreted as report deletion.

## 3. Appointment eligibility rule

A Student may book a collection appointment **only** when all three conditions
hold at the authoritative booking check:

1. the user is authenticated as a Student;
2. the Claim belongs to that authenticated Student; and
3. the Claim currently has status `APPROVED`.

The approved Claim is the authoritative ownership-verification result for the
appointment workflow.

None of the following grants appointment eligibility:

- a Claim in `PENDING_REVIEW`, `REJECTED`, or `WITHDRAWN`;
- merely owning a LOST report;
- merely having a possible-match link;
- an Item Report having status `SUBMITTED` or `UNDER_REVIEW`;
- an officer having opened or reviewed a report; or
- the former `Start review` action having been used.

Report-review state must not become a hidden prerequisite. The Claims handoff
is `ClaimStatus.APPROVED`, regardless of either report's `ReportStatus`.

## 4. Data available to Sprint 4

### A. Legitimate eligibility and context data

The implemented Claim model is the current source of truth for these facts:

| Fact | Exact current source | Appropriate Sprint 4 use |
| --- | --- | --- |
| Stable Claim identity | `Claim.claimId()` returning `ClaimId`; persisted value from `ClaimId.value()` | Appointment-to-Claim association and authoritative lookup |
| Visible Claim reference | `ClaimId.reference()`; also present in selected `StudentClaimDetail.claimReference()` | Student-facing confirmation or appointment display |
| Claimant user ID | `Claim.claimantUserId()` | Claim-side ownership check against the authenticated Student; it need not be exposed back to the UI |
| Claim status | `Claim.status()` returning `ClaimStatus` | Eligibility requires exactly `APPROVED` |
| LOST Report ID | `Claim.lostReportId()` | Optional downstream context lookup when Sprint 4 has an approved need |
| FOUND Report ID | `Claim.foundReportId()` | Optional downstream context lookup when Sprint 4 has an approved need |
| Approval time | `Claim.terminalAt()` when status is `APPROVED` | Optional booking/case context; it is the generic terminal time, not a separately stored field |
| Existing safe Student context | `StudentClaimsState.StudentClaimDetail` and `SafeMatchSummary` | Reference, status, event times, lost item name, and approved safe FOUND fields when currently available |

Sprint 4 should consume the minimum subset its approved UI and rules require.
For eligibility itself, Claim identity, authenticated ownership, and current
`APPROVED` status are sufficient. Report endpoint IDs, approval time, and safe
display context should cross the boundary only if Sprint 4 planning establishes
a concrete need.

### B. Data Sprint 4 should not consume

Appointment code should not receive or depend on:

- ownership evidence;
- approval or rejection decision reasons;
- another Student's identity or Claim;
- private Item Report descriptions or identifying details;
- Claim lock, closure, resubmission, or repeated-decision mechanics;
- `ClaimLedger` rules;
- Claim-store schema, paths, codecs, temporary-file protocol, or repository
  synchronization details;
- the internal structure of opaque Student or officer handles; or
- the implementation of `JsonClaimRepository` or `claims.json`.

The visible Claim reference alone must never be treated as proof of ownership.

## 5. Data and implementation details Sprint 4 must not depend on

Sprint 4 must not parse or directly edit `claims.json`, copy Claim persistence,
or call broad persistence operations merely to reproduce Claim-side filtering.
It must not infer approval from report state, possible-match state, officer
queue visibility, or a remembered earlier UI result.

Appointment lifecycle remains separate from Claim lifecycle:

- an Approved Claim remains Approved after booking;
- cancellation or rescheduling must not revert Claim status;
- appointment completion must not rewrite Claim evidence, decision reason, or
  event times;
- appointment state must not be added to `ItemReport` or `ReportStatus` for
  convenience; and
- Sprint 4 must not introduce a Claim transition to represent booking,
  cancellation, attendance, completion, or handover.

Claims are upstream eligibility and context. Appointments own all later
appointment state.

## 6. Recommended integration seam

### Current limitation

No current public API cleanly answers, for an authenticated Student, "is this
an Approved Claim owned by this Student?"

- `ClaimRepository.loadAll()` is public but returns every retained Claim,
  including other Students' identities, evidence, and decision data. It is a
  Claim persistence/command boundary and is too broad for appointment code.
- `StudentClaimsService` enforces the right Student filter, but it is a
  stateful submission/tracking UI service. Its Claim handles deliberately have
  no public ID accessor, and its selected detail includes evidence and decision
  data that appointments do not need.
- `ApprovedClaimReportService.loadApprovedReportIds()` is a deliberately narrow
  officer report-queue adapter. It returns report IDs, not Claim ownership or
  Claim identity, so it must not be reused as appointment authorization.

### Smallest required capability

Developer 2 should provide, after separate approval, one narrow Claim-side read
contract bound to the current authenticated Student. It should either resolve
an appointment candidate by stable Claim identity or return that Student's
currently Approved Claim summaries, depending on the Sprint 4 entry flow that
Developer 1 selects during planning. In either shape, the Claim side must:

- reject a non-Student authenticated user;
- enforce `claimantUserId == authenticatedUser.userId()` internally;
- require the current authoritative status to be exactly `APPROVED`;
- fail closed when authoritative Claim data is unavailable; and
- expose only a minimal immutable result.

The minimal result should contain the stable Claim ID/reference and only the
approved optional context needed by the appointment feature, such as approval
time or LOST/FOUND Report IDs. It should not expose a raw `Claim`, evidence,
decision reason, all-Claim snapshot, Claim repository, or persistence detail.

The booking operation must perform or invoke this authoritative check at the
time of booking; a previously rendered Approved label is not sufficient by
itself.

This is a proposed capability, not an implemented signature. Sprint 4 planning
must first choose whether booking begins from a selected Approved Claim or from
an appointment-owned list of eligible Claims; that choice determines whether a
single-Claim resolver or an owned-approved-Claims query is the smaller API.

## 7. Ownership boundary

Developer 2 owns the Claim-side adapter/read service because it interprets
Claim identity, claimant ownership, status, storage failure, and projection
privacy. Developer 1 should consume that approved public contract and own the
Appointment domain, slot selection, booking UI, appointment persistence, and
appointment lifecycle.

The cross-owner integration request is therefore:

1. Developer 1 defines the smallest appointment entry-flow need during Sprint
   4 planning: resolve one selected Claim or list the authenticated Student's
   Approved Claims.
2. Developer 2 reviews and implements the minimal Claim-side authenticated read
   adapter after explicit authorization.
3. Shared application composition injects that adapter into Developer 1's
   Appointment feature. Any edit to `FindersKeepersApp` or another shared or
   Developer 1-owned composition surface requires the normal cross-owner
   approval.

Developer 1 can consume the existing `ClaimId`, `ClaimStatus`, and safe
projection semantics as documented here, but should not consume
`ClaimRepository.loadAll()` directly or instantiate `StudentClaimsService` as
an appointment dependency.

## 8. Sprint 4 decisions still unresolved

Sprint 3 does not decide the following. They must be resolved separately in
Sprint 4 planning:

- Can one Approved Claim have only one active appointment at a time?
- May the Student book another appointment after cancellation?
- What happens after a missed appointment?
- When does an appointment become completed?
- Does appointment completion affect a later collection or case state?
- How are collection slots represented, generated, and owned?
- Can two Students book the same slot, and what consistency rule prevents it?
- What does rescheduling do to the previous booking?
- What information from the LOST and FOUND reports should the appointment UI
  show?
- What happens if an Approved Claim exists but one or both canonical reports
  later become unavailable?
- Must eligibility be rechecked for rescheduling or only for initial booking?
- Which appointment or case views include cancelled, missed, or completed
  records, and for how long?

This handover deliberately makes no decision on those questions.

## 9. Suggested integration verification

Later Sprint 4 tests should verify at least:

- a `PENDING_REVIEW` Claim cannot book;
- a `REJECTED` Claim cannot book;
- a `WITHDRAWN` Claim cannot book;
- an Approved Claim belonging to another Student cannot book;
- the authenticated owner of an Approved Claim can reach booking;
- appointment booking, cancellation, rescheduling, and completion do not
  mutate Claim status, evidence, decision reason, or Claim event times;
- report status alone cannot grant booking eligibility;
- a possible-match link alone cannot grant booking eligibility;
- report-review queue visibility and the former `Start review` workflow cannot
  grant booking eligibility; and
- Claim-store unavailability fails eligibility closed without exposing private
  Claim data.

These are integration behaviors for later Sprint 4 planning, not a Sprint 4
Requirements-to-Tests artifact and not test authorization.

## 10. Known limitations / current API gaps

- There is no appointment-specific or neutral authenticated Approved-Claim
  lookup today.
- The current narrow `ApprovedClaimReportService` serves officer report-queue
  visibility only and loses Claim identity and ownership context.
- Student-facing Claim handles are intentionally opaque and cannot currently
  carry a stable Claim ID into a separate Appointment feature.
- The current Claim model stores a generic terminal time; approval time is
  obtained from `terminalAt` only after confirming status `APPROVED`.
- Existing safe Student report context is designed for the Claims UI. Reuse by
  appointments requires an explicit minimal projection decision rather than
  importing the Claims UI state wholesale.
- Shared application wiring for the Appointment feature does not yet exist and
  remains a separately approved cross-owner integration change.
