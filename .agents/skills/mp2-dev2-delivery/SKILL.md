---
name: mp2-dev2-delivery
description: Deliver a fully planned, approved, and separately authorized Developer 2 feature in the Finders Keepers CS3227 MP2 repository, including authentication, role navigation, persistence, or Desk Officer review. Use only for implementation tasks that deliver Dev 2-owned MP2 changes; do not use for planning, explanation, code-reading requests, Dev 1 implementation, unrelated Java work, or non-delivery requests.
metadata:
  short-description: Deliver fully approved MP2 Dev 2 features
---

# MP2 Developer 2 Delivery

Deliver only the approved Developer 2 feature scope.

## Verify delivery gates

Before changing implementation files:

1. Read and obey the repository-root `AGENTS.md`.
2. Identify the mission ID and read its mission brief under
   `docs/mission-briefs/`.
3. Read these feature-planning artifacts under `docs/features/<mission-id>/`:
   - `GrillingDecisions.md`
   - `PRD.md`
   - `TDD.md`, meaning Technical Design Document
   - `RequirementsToTests.md`
4. Verify every gate in this exact order:
   1. the mission brief is approved;
   2. `GrillingDecisions.md` is complete and contains no unresolved decision;
   3. `PRD.md` is approved and records its approver and approval date;
   4. `TDD.md` is approved;
   5. `RequirementsToTests.md` is approved and completely traces the approved
      requirements to automated or explicitly manual verification; and
   6. the repository owner has separately authorized implementation.
5. Check the artifacts for contradictions with each other, `AGENTS.md`, and the
   current implementation request.
6. Inspect the relevant existing code and tests only after the gates pass.

Approval of a planning artifact is not implementation authorization. Do not
infer approval from a draft, file presence, mission-brief approval, or a prior
implementation request for different scope.

If any artifact is missing, incomplete, unapproved, unresolved, ambiguous, or
contradictory, stop before implementation. Report the exact failed gate and ask
the owner to complete or reconcile it. Do not create, backfill, reinterpret, or
invent planning artifacts or approvals while running this delivery skill.

### Named transition exception

Only the S1-D2-01 authentication cleanup recorded in
`logs/2026-09-18-s1-d2-01-auth-cleanup.md` may proceed without retrospective
grilling, PRD, TDD, or requirements-to-tests artifacts. It is grandfathered
only when all of these are present:

- the approved `S1-D2-01-auth-login.md` mission brief;
- the owner-approved Authentication Cleanup and Delivery-Gate Hardening plan;
- an explicit request to implement that cleanup; and
- the dated cleanup log that truthfully records the original gate deviation.

Do not fabricate retrospective artifacts for this exception. The exception
does not authorize later authentication features, shell or release integration,
or any other Developer 2 delivery.

Begin the response with a concise preflight summary covering:

- requested outcome;
- the status of every required delivery gate or the named exception;
- Developer 2-owned implementation scope;
- possible ownership-boundary conflicts;
- planned tests;
- planned documentation.

Treat authentication, role navigation, persistence, storage recovery, and Desk Officer review according to the ownership boundaries in `AGENTS.md` and the selected mission brief.

## Respect ownership and authorization

Implement only the approved feature scope.

Before modifying a Developer 1-owned area or another cross-owner boundary:

1. identify the affected file;
2. explain why the integration change is required;
3. propose the smallest sufficient change;
4. state whether Developer 1 could make it instead; and
5. stop and request explicit approval for that modification.

Continue independent, approved Developer 2 work when possible. Do not duplicate shared or Developer 1-owned types to bypass an ownership boundary.

Never commit, push, merge, publish, add or change dependencies, perform destructive actions, or alter release or CI configuration unless explicitly authorized.

## Protect data

Use only synthetic users, credentials, reports, and private details in implementation, tests, documentation, screenshots, and examples.

Never reveal or record real credentials, secrets, password material, private report-identifying details, or real student, staff, or school data. Run persistence tests only against isolated temporary storage, never real user storage.

## Implement and verify

Keep code, tests, and documentation within the approved mission scope.

After implementation:

1. run focused tests for the changed behavior;
2. run the repository checks required by `AGENTS.md`, including any conditional checks applicable to the change;
3. report required checks that could not run exactly and do not substitute unverified claims;
4. confirm tests did not modify real user data;
5. inspect `git diff`; and
6. inspect `git status --short`.

Do not report completion while required verification is failing or an ownership-boundary change remains unapproved.

## Handoff

Provide a concise handoff listing:

- files changed;
- tests and repository checks run;
- results, including failures, skips, or unverified requirements;
- remaining risks, limitations, or cross-owner integration work.
