# S3-D2-01 Claims and Verification

- Status: Approved
- Owner: Developer 2
- Sprint: Sprint 3 — Claims and Verification
- Sprint dates: 2026-09-21 to 2026-09-23
- Authoritative scope direction received: 2026-09-22
- Approver: Repository owner
- Approval date: 2026-09-22
- Mission Brief approval: Granted by repository owner, 2026-09-22
- Grilling Decisions authorization: Not granted
- PRD, TDD, and Requirements-to-Tests authorization: Not granted
- Branch, test, production implementation, and cross-owner change authorization: Not granted
- Governing instructions: `AGENTS.md`

## Mission / goal

Deliver one Claims and Verification feature in which:

1. an authenticated Student can submit and track an ownership claim for a
   potential match; and
2. an authenticated Desk Officer can review the claim and approve or reject it.

The feature includes claim submission, ownership evidence, validation,
tracking, withdrawal, durable persistence, officer review, decision reasons,
and prevention of repeated decisions.

Claim-specific state remains separate from existing Item Reports. This mission
does not redesign the canonical report model, report statuses, report
persistence, possible-match semantics, authentication, or existing Student and
Desk Officer workflows.

## Users and authorization boundary

The canonical application roles remain:

- **Student** — submits and tracks their own ownership claims and may withdraw a
  claim where the later approved lifecycle permits it.
- **Desk Officer** — reviews submitted claims, accesses the claim and relevant
  report information needed for verification, and approves or rejects a claim
  with the required decision reason.

`Community Member` does not introduce a third role and is not used as an
application role name. Stored authentication roles remain `STUDENT` and
`DESK_OFFICER`.

The existing authenticated user ID is the available stable actor identity.
Whether and how Student and officer identities are persisted on claim records
is a later product and technical decision.

## Problem being solved

The current application can record reports, let officers begin report review,
and let officers create possible-match links. It cannot record a Student's
assertion that a potential match is their property, preserve evidence for that
assertion, expose a claim's progress, or record a Desk Officer's approval or
rejection.

Sprint 3 adds that missing end-to-end claim capability without turning reports
or possible-match links into claims and without treating deterministic matching
as proof of ownership.

## Confirmed repository baseline

The following are existing contracts consumed by this mission, not designs to
replace:

- Developer 1 owns the canonical immutable `ItemReport`, `ReportType`,
  `ItemCategory`, `ReportStatus`, report creation and validation, and the
  existing Student report submission and history workflows.
- `ReportStatus` currently contains only `SUBMITTED` and `UNDER_REVIEW`.
- Developer 2 owns `ReportRepository` and strict version-one JSON report
  persistence. Its public operations are `loadAll`, `insert`, and complete
  identity-preserving `replace`.
- Authentication exposes only the `STUDENT` and `DESK_OFFICER` roles and keeps
  the current `AuthenticatedUser` in memory until logout.
- The Student workspace currently contains report submission and personal
  report history.
- The Desk Officer workspace currently contains report review and possible
  matching.
- Officer-created possible-match links are separate from reports, are not
  ownership proof, and do not change report status.
- No claim model, claim persistence contract, claim service, claim status, or
  claim UI exists.

The implementation baseline must be confirmed again before later delivery.
The inspected Sprint 2 branch contains work not yet present on `main`.

## Feature boundary

This mission begins when an authenticated Student enters the Claims feature and
acts on an eligible potential-match context. It ends when:

- the Student can observe the current truthful state of their claim and perform
  any lifecycle action later approved for Students; and
- an authenticated Desk Officer can reach the claim queue, inspect the claim
  and permitted supporting information, and durably approve or reject an
  undecided claim with the required reason.

The exact source of an eligible potential-match context, claim identity and
cardinality, lifecycle states, evidence fields, validation rules, queue rules,
and decision consequences are intentionally unresolved for the later staged
planning process.

## In scope

Developer 2 owns the complete Claims and Verification feature, including:

- claim-specific state and rules kept outside the existing report and
  possible-match contracts;
- authenticated Student claim submission;
- ownership evidence captured for a claim;
- claim validation and privacy-safe validation feedback;
- Student tracking of their own claims;
- Student withdrawal behavior within the approved lifecycle;
- durable claim persistence, including pending claims surviving application
  restart;
- a Desk Officer claim queue;
- Desk Officer claim details and access to relevant canonical reports within
  the approved privacy boundary;
- explicit approval and rejection actions;
- decision reasons;
- prevention of a repeated or conflicting decision;
- role-authorization boundaries for claim views;
- claim-specific automated, manual, privacy, and regression evidence; and
- claim-specific user and developer documentation after implementation is
  authorized and delivered.

## Explicit non-goals

This mission does not authorize or introduce:

- changes to `ItemReport`, `ReportType`, `ItemCategory`, `ReportStatus`, report
  creation, or report validation;
- changes to report-store version one or encoding claim data inside a report;
- changes to the meaning or persistence format of possible-match links;
- a duplicate or Claims-owned report model;
- authentication redesign, a new application role, registration, account
  administration, or persistent login;
- redesign of Student report submission/history, Desk Officer report review,
  or officer matching;
- automatic ownership approval or rejection based on matching scores;
- treating a possible-match suggestion or link as verified ownership;
- item collection, handover, return, inventory management, or notifications
  beyond claim tracking unless a later approved artifact explicitly brings a
  narrowly defined behavior into this mission;
- a generic workflow engine, generic case-management platform, or unrelated UI
  redesign;
- new dependencies, build/CI changes, release changes, or migrations without
  separate authorization; or
- production changes to Developer 1-owned or shared integration files merely
  because Developer 2 owns the Claims feature.

## Mission-level functional requirements

### Student claim capability

- **SC-FR-001 — Authorized access:** Only an authenticated Student may submit,
  track, or request withdrawal of that Student's claims.
- **SC-FR-002 — Potential-match context:** A submitted claim identifies the
  approved potential-match target without changing the target reports or the
  meaning of a possible-match suggestion or link.
- **SC-FR-003 — Ownership evidence:** Submission captures the approved evidence
  required for a Desk Officer to assess ownership. The exact evidence fields,
  limits, and validation belong to Grilling Decisions and the PRD.
- **SC-FR-004 — Validation:** Invalid or incomplete claim input is rejected
  before persistence with safe, actionable feedback and no false success.
- **SC-FR-005 — Durable submission:** A submitted pending claim survives
  application restart. Exact storage guarantees and mechanics belong to the
  TDD.
- **SC-FR-006 — Personal tracking:** A Student sees only their own claims and a
  truthful current state. The exact visible fields and decision information are
  later product decisions.
- **SC-FR-007 — Withdrawal:** The feature provides withdrawal behavior. The
  allowed source states, resulting state, repeated action behavior, and effect
  on officer review are unresolved for Grilling Decisions.

### Desk Officer verification capability

- **DO-FR-001 — Authorized access:** Only an authenticated Desk Officer may
  enter the officer claim queue, inspect officer-only claim details, or decide
  a claim.
- **DO-FR-002 — Claim queue:** The officer has a truthful queue of claims that
  are eligible for review under the later approved lifecycle rules.
- **DO-FR-003 — Relevant information:** The officer can inspect the claim,
  ownership evidence, and permitted canonical report information needed for a
  decision. Student-visible and officer-only information remain separated.
- **DO-FR-004 — Explicit decision:** Approval and rejection are explicit Desk
  Officer actions. Viewing or selecting a claim does not decide it.
- **DO-FR-005 — Decision reason:** Each decision records the reason required by
  the later approved product rules.
- **DO-FR-006 — Repeated-decision prevention:** A decided claim cannot receive a
  second or conflicting decision. Exact conflict and simultaneous-action rules
  remain for Grilling Decisions and the PRD.

### Shared claim behavior

- **SH-FR-001 — Existing-contract boundary:** Claim-specific state must not be
  encoded into `ItemReport`, `ReportStatus`, report-store version one, or the
  meaning of a possible-match relationship without later explicit approval.
- **SH-FR-002 — Truthful failure:** A failed claim operation is not reported as
  successful. Exact failure, recovery, and retry behavior belongs to later
  approved requirements and design.
- **SH-FR-003 — Privacy:** Claim evidence and private report information appear
  only to the actors and in the contexts approved by the later requirements.
  Errors, logs, screenshots, test output, and handoffs expose neither private
  evidence nor unsafe report data.
- **SH-FR-004 — Existing-contract preservation:** Claim operations do not
  silently mutate reports, report statuses, or possible-match links.

## Ownership and integration boundaries

### Developer 2 ownership

Developer 2 owns all claim-specific behavior, persistence, Student and Desk
Officer experiences, tests, and documentation approved for this mission.

This ownership is feature ownership. It does not provide advance authorization
to change files belonging to another developer or to bypass a shared contract.

### Developer 1 ownership retained

Developer 1 retains ownership of:

- `ItemReport`, `ReportType`, `ItemCategory`, and `ReportStatus`;
- report creation requests and report validation;
- the existing Student report-submission and report-history behavior;
- Student report field and validation documentation;
- the application entry point and initial JavaFX shell; and
- Gradle, dependency, release, and CI configuration.

Claims must consume the existing report domain through approved public
contracts. A shadow report type or claim-specific copy of the report domain is
not an acceptable integration workaround.

### Cross-owner production changes

Cross-owner production changes are not authorized by this Mission Brief or by
Developer 2's feature ownership. Before any such change, the later approved
design and delivery request must:

1. identify the exact file and verified baseline;
2. explain why the Claims feature requires the change;
3. propose the smallest sufficient edit;
4. state whether Developer 1 can make the edit instead; and
5. obtain explicit authorization for that exact change.

Independent Developer 2-owned work may proceed only after all applicable
planning and implementation gates are complete.

## Persistence and consistency constraints

- Pending claims must survive application restart. The exact repository
  interface, schema, storage format, path, guarantees, and recovery behavior
  belong to the TDD after product requirements are approved.
- Claim data must not be added to strict report-store version one or the
  possible-match link format without a separately approved compatibility
  decision.
- Automated persistence tests use synthetic claims and isolated temporary
  storage. They do not read or write project-local runtime stores.

## Privacy and safety boundaries

- Use only synthetic users, reports, claims, evidence, reasons, and school data
  in tests, documentation, screenshots, logs, and demonstrations.
- A Student must not gain access to another Student's claim, reporter identity,
  private report detail, or ownership evidence.
- Officer-only evidence and private report information must not be exposed
  outside the authorized contexts later approved by Grilling Decisions and the
  PRD.
- Claim-domain string representations and diagnostics must not reproduce
  evidence, private report data, storage contents, or paths.
- Existing authentication remains the role boundary. Claim-specific session
  and logout presentation behavior remains for Grilling Decisions and the PRD.

## Acceptance criteria

This mission is complete only when later approved requirements demonstrate
that:

- an authenticated Student can submit a valid ownership claim for an approved
  potential-match context;
- invalid claim input cannot create a claim or display false success;
- a submitted pending claim remains available after application restart;
- the Student can track only their own claims and use the approved withdrawal
  behavior;
- an authenticated Desk Officer can load the approved claim queue and inspect
  the permitted claim, evidence, and relevant report information;
- the officer can explicitly approve or reject an eligible claim with the
  required reason;
- repeated, conflicting, unauthorized, or failed decisions cannot produce a
  second decision or false success under the later approved rules;
- reports, report statuses, report-store v1, and possible-match relationships
  remain unchanged unless a later exact cross-owner change is separately
  approved;
- existing authentication, report, Student, review, and matching behavior
  remains green; and
- required claim documentation and interaction evidence truthfully match the
  delivered behavior.

## Testing expectations

The later Requirements-to-Tests artifact must map every approved requirement
to proportionate automated, manual, review, or command evidence. It must cover
the approved Student and Desk Officer claim behavior, pending-claim restart
persistence, authorization and privacy boundaries, preservation of existing
report and possible-match contracts, and relevant regression behavior. Exact
test scenarios and seams depend on the approved Grilling Decisions, PRD, and
TDD.

## Documentation and process deliverables

After their respective gates are explicitly authorized, the mission requires:

- one completed and approved Grilling Decisions record;
- one approved Claims and Verification PRD;
- one approved TDD;
- one approved Requirements-to-Tests mapping;
- claim-specific User Guide and Developer Guide updates after implementation;
- factual human-verified interaction evidence following `logs/README.md`;
- reflection updates required by the project submission process;
- a final handoff listing changed files, verification, privacy/data-safety
  evidence, limitations, cross-owner changes, and unverified requirements.

Historical Sprint 1 and Sprint 2 mission briefs, planning artifacts, decision
records, and logs remain historical records and are not rewritten solely
because Claims are in Sprint 3 scope.

## Risks and controls

- **Report-domain leakage:** Claim state could be placed into `ItemReport` or
  `ReportStatus` for convenience. Control: keep Claims separate and require an
  exact cross-owner approval before any report-contract change.
- **Possible match mistaken for ownership:** A deterministic suggestion or link
  could be treated as proof. Control: preserve existing possible-match semantics
  and require an explicit human claim decision.
- **Student privacy expansion:** Claim tracking or potential-match presentation
  could expose another Student or private report data. Control: resolve exact
  projections during requirements work and test every role boundary.
- **Conflicting decisions:** Stale views or repeated actions could approve and
  reject the same claim. Control: resolve the observable conflict rules in
  Grilling Decisions and the PRD before selecting a technical enforcement
  design.
- **Cross-owner drift:** Student navigation or shell composition could expand
  into report redesign. Control: use the smallest integration seam and obtain
  exact authorization before editing another owner's production file.
- **Baseline ambiguity:** Sprint 2 work is present on the inspected feature
  branch but not `main`. Control: record the exact verified implementation
  baseline before branch creation or technical design.

## Unresolved product decisions for Grilling Decisions

The following are deliberately unresolved here and must not be inferred from
historical sprint plans or current implementation convenience:

1. What exact object a claim targets and how it relates to a matching
   suggestion, an officer-created possible-match link, and the two reports.
2. Which potential matches are eligible for Student claim submission and how a
   Student reaches them without receiving officer-only matching metadata.
3. Claim cardinality and duplicate/competing-claim rules for each Student,
   report, pair, or potential match.
4. The ownership-evidence fields, requiredness, limits, validation, amendment
   behavior, and visibility.
5. The complete claim lifecycle and user-facing status vocabulary.
6. The states in which withdrawal is allowed, its effect, and repeated
   withdrawal behavior.
7. The Desk Officer queue membership, ordering, filtering, selection, and
   empty-state behavior.
8. Approval/rejection reason requirements and which decision information is
   visible to the Student.
9. The effects of approval, rejection, or withdrawal on later claims,
   possible-match links, report review, collection, return, and any future
   workflow.
10. Whether officer identity and decision timestamps are product-visible or
    retained for audit.
11. Conflict, stale-state, and simultaneous-action outcomes observable to each
    role.
12. Claim retention and whether any deletion or resubmission behavior exists.
13. Exact privacy projections for Student lists, officer queue rows, claim
    details, report details, documentation, and manual evidence.
14. Exact Student and Desk Officer navigation placement and the corresponding
    smallest cross-owner integration request.

Technical choices such as classes, packages, repository signatures, storage
schema, file path, codec, filesystem protocol, and JavaFX hierarchy are not
Grilling Decisions. They remain for the TDD after the PRD fixes observable
product behavior.

## Planning and authorization gates

This Mission Brief establishes the proposed authoritative Sprint 3 scope and
ownership boundary. It does not authorize a downstream artifact or delivery.

Work proceeds only through these gates in order:

1. The repository owner approves this Mission Brief and its metadata is updated.
2. On a separate explicit request, create and approve
   `GrillingDecisions.md`, resolving the product decisions assigned above.
3. On a later explicit request, create and approve the PRD using only the
   approved Mission Brief and Grilling Decisions.
4. On a later explicit request, create and approve the TDD using only the
   approved earlier artifacts and verified repository contracts.
5. On a later explicit request, create and approve Requirements-to-Tests.
6. Separately authorize the exact implementation scope, branch, tests, and each
   required cross-owner production change.
7. Implement and verify only that approved scope.

Mission Brief approval alone does not authorize Grilling Decisions, a PRD, a
TDD, Requirements-to-Tests, branch creation, production code, tests,
documentation delivery, screenshots, dependencies, release changes, commits,
pushes, pull requests, or merging.
