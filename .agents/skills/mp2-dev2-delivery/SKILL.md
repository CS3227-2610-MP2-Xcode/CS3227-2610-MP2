---
name: mp2-dev2-delivery
description: Deliver an approved Developer 2 feature in the Finders Keepers CS3227 MP2 repository, including authentication, role navigation, persistence, or Desk Officer review. Use only for implementation tasks that deliver Dev 2-owned MP2 changes; do not use for explanation or code-reading requests, Dev 1 implementation, unrelated Java work, or non-delivery requests.
metadata:
  short-description: Deliver approved MP2 Dev 2 features
---

# MP2 Developer 2 Delivery

Deliver only the approved Developer 2 feature scope.

## Establish scope

Before implementation:

1. Read and obey the repository-root `AGENTS.md`.
2. Locate and read the relevant mission brief under `docs/mission-briefs/`.
3. If the applicable mission brief is missing, ambiguous, or not approved for implementation, stop and request clarification or approval.
4. Inspect the relevant existing code and tests.

Begin the response with a concise preflight summary covering:

- requested outcome;
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
