# 2026-09-29 — PR #23 whole-application test coverage retrospective reconstruction

- Verifier: Pending repository-owner review of this reconstruction and its session-only evidence.
- Environment: FindersKeepers, `royden/test-coverage`; the recorded local verification used Windows, Java 25 and Gradle 9.6.1. PR CI used Java 25 on macOS, Windows and Ubuntu.
- Prompt or action: After approving the coverage-gap analysis, the owner asked Codex to add meaningful tests in waves until aggregate production **line** coverage reached at least 90%. Production source and all documentation were read-only. The owner later requested logical test-only commits on `royden/test-coverage`, then a push.
- Expected result: A stable whole-application suite covering appointment boundaries, account and role isolation, Claims, matching, reports, reviews and JavaFX workflows, with a fresh JaCoCo aggregate line result of at least 90%.
- Observed result: [PR #23](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/pull/23) merged 31 changed files, all under `src/test/java/`, from base `95175ab` through head `c90d4a8`. The patch contains 62 added `@Test` annotations and expands assertions in existing tests. The recorded final local result was 513 passed, none failed or skipped, and 6,385/7,083 production lines covered (90.15%). PR CI reported success on all three operating systems.
- Verification: Historical local results and CI evidence are separated below. No tests were run while preparing this log. No manual JavaFX verification record was found.
- Status: needs follow-up — the owner has not yet verified this retrospective summary or supplied a manual UI observation.
- Follow-up: Compare the historical local result with the original Codex session if an independently retained transcript is required. Record any actual human JavaFX walkthrough separately, with date, environment, actions and observations.

## Evidence and provenance

- The owner's recorded instructions in the Codex conversation are the final whole-application coverage request, the Phase 0–3 implementation approval, the instruction to split completed work into commits, and the later request to push. The implementation approval supplied the 475-test, 65.89%-line baseline after merge commit `d1d746e`.
- [PR #23](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/pull/23) identifies base `95175ab`, head `c90d4a8`, eight commits, 31 changed files, 3,411 additions and three deletions; it was merged on 2026-09-29 Singapore time. Its [head commit](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/commit/c90d4a8bed45041384b0130ca0a0b6f9a91943b9) and [CI run 108](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/actions/runs/36459960578) are the remote evidence used here.
- The commit sequence is [matching and authentication](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/commit/c4e06e4), [persistence](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/commit/8f7814d), [Claim behavior](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/commit/25aa959), the [test-coverage merge](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/commit/d1d746e), [appointments](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/commit/52564a1), [Claims UI](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/commit/6ad9d55), [matching/review/report UI](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/commit/4199790), and [workspace composition](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/commit/c90d4a8).
- The 62-test count is a count of added `@Test` annotations in the PR patch: 24 in the three earlier test commits and 38 in the four later test commits. It is not a count of every new assertion or parameterized case.

## Tests present in the PR

| Area | Test evidence in the PR diff |
| --- | --- |
| Appointment integration and boundaries | `AppointmentLifecycleIntegrationTest`, `AppointmentWorkspaceFactoryIntegrationTest`, `AppointmentAuthorizationBoundaryTest`, `AppointmentSlotBoundaryTest`, `AppointmentStoreFailureBoundaryTest` and `AppointmentTransitionRejectionTest` cover sequential students competing for a slot, cancellation/rescheduling, no-show and custody persistence, role and ownership rejection, exact Singapore midnight/23:30 and half-hour boundaries, duplicate/adjacent/disabled slots, failed storage and rejected transitions without partial writes or audit events. |
| Authentication and session | New `AuthenticationModelValidationTest` checks account/session identity validation; expanded `JsonUserRepositoryTest` checks escaped Unicode identity after restart; expanded `Pbkdf2PasswordHasherTest` rejects unsupported credential metadata. Appointment authorization and workspace tests also exercise role-bound and per-session behavior. |
| Claims | New `ClaimApplicationStateTest` plus expanded Student/Officer service, model and JSON repository tests cover selection state, ownership, competing claims, stale evidence, decision rules, terminal state, Unicode/format handling and safe failures. `StudentClaimsPaneWorkflowTest` and `DeskOfficerClaimsPaneWorkflowTest` cover submit/withdraw, approval/rejection, pending/history, unavailable/retry states and private-detail boundaries. |
| Matching | New `MatchEvaluationTest` checks rule outcomes, points and deterministic reasons; expanded `FilePossibleMatchRepositoryFormatTest` checks invalid UTF-8 and bare carriage returns. `OfficerMatchingPaneWorkflowTest` covers suggestion selection, durable link/unlink, presentation and recovery from invalid relationship storage. |
| Reports and reviews | Expanded `DeskOfficerReviewServiceTest` checks retry after an unavailable approved-claim endpoint source. `StudentReportPaneWorkflowTest` covers validation, persistence failure, own-history search and account isolation. `DeskOfficerReviewPaneWorkflowTest` covers filtering, selection, approved-Claim endpoint hiding, retry and privacy of rendered rows. |
| JavaFX appointment UI | `OfficerAppointmentPaneTest` and `StudentAppointmentPaneWorkflowTest` exercise rendered slot/case/audit values, missing/current-time feedback, booking/rescheduling/cancellation, no-show/custody actions, session-draft clearing, empty states and corrupt-store retry. The Claims, matching, report and review pane tests above are also automated JavaFX workflow tests. |
| Workspace and helpers | `WorkspaceCompositionTest` checks Student and Desk Officer tab switching, refresh and feature lifecycle/session cleanup. `ClaimsUiFixture`, `FxTestDialogs` and `FxTestNodes` provide test setup, dialog handling and rendered-node lookup. |

The PR diff changes test source only. It does not show production-source, documentation or coverage-configuration edits. These are tests **present in the patch**; their presence alone is not evidence that a person used the live UI.

## Historical local commands and results recorded during the coverage work

The implementation request recorded the post-merge baseline as **475 passed, zero failed, zero skipped; 4,667/7,083 lines (65.89%); 60.04% branches**. The Codex session then recorded full-suite/JaCoCo wave checkpoints, rather than a separately preserved command transcript for every intermediate wave:

| Recorded checkpoint | Tests passed | Aggregate line coverage | Branch coverage |
| --- | ---: | ---: | ---: |
| Approved baseline at `d1d746e` | 475 | 4,667/7,083 (65.89%) | 60.04% |
| Appointment wave | 484 | 5,040/7,083 (71.16%) | 64.19% |
| Claims UI wave | 489 | 5,561/7,083 (78.51%) | 68.34% |
| Matching/review/report UI wave | 496 | 6,040/7,083 (85.27%) | 70.68% |
| Final targeted waves | 513 | 6,385/7,083 (90.15%) | 2,091/2,773 (75.41%) |

The recorded final full-suite and JaCoCo command used this cached Gradle executable from the repository working directory:

```powershell
& 'C:\Users\royde\Desktop\NUS\Sem 5\CS3227\MP2\CS3227-2610-MP2-cov-claims\.gradle-user\wrapper\dists\gradle-9.6.1-bin\4ticwg1pgcbps2hj28r8so764\gradle-9.6.1\bin\gradle.bat' test jacocoTestReport --rerun-tasks --offline --gradle-user-home 'C:\Users\royde\.gradle' --quiet
```

The session recorded **513 passed, zero failed, zero skipped**. JaCoCo recorded 31,679/34,580 instructions (91.61%), 6,385/7,083 lines (90.15%), 2,091/2,773 branches (75.41%), 1,266/1,351 methods (93.71%), and 214/218 classes (98.17%). The line gain over the approved baseline was 1,718 covered production lines. The recorded normal project gate also passed using the same executable:

```powershell
& 'C:\Users\royde\Desktop\NUS\Sem 5\CS3227\MP2\CS3227-2610-MP2-cov-claims\.gradle-user\wrapper\dists\gradle-9.6.1-bin\4ticwg1pgcbps2hj28r8so764\gradle-9.6.1\bin\gradle.bat' check --offline --gradle-user-home 'C:\Users\royde\.gradle' --quiet
```

These local commands and percentages are **historical Codex-session results**, not reruns for this log. A historical command/result for each of the three 2026-09-23 test commits was not found in the available session record; none is inferred from their presence in the PR.

## CI visible for PR #23

[CI run 108](https://github.com/CS3227-2610-MP2-Xcode/CS3227-2610-MP2/actions/runs/36459960578), associated with head `c90d4a8`, concluded **success**. Its `Quality (macos-15)`, `Quality (windows-latest)` and `Quality (ubuntu-latest)` jobs each concluded success. The job logs show `clean check release`, including `test`, Checkstyle and `jacocoTestReport`, followed by a release-artifact smoke test. Each job log lists 513 `PASSED` test entries and no `FAILED` or `SKIPPED` test entries. CI proves those checks ran successfully on the PR; its visible logs do not print a JaCoCo aggregate percentage, so the 90.15% figure above remains the recorded local JaCoCo result.

## New verification for this reconstruction — 2026-09-29

No tests or JavaFX application run were performed while writing this log. Read-only inspection compared `95175ab` with `c90d4a8` using `git log --format="%h %p %ad %s" --date=short 95175ab72b5ac543327d59ff2d30e82e387a1493..c90d4a8bed45041384b0130ca0a0b6f9a91943b9`, `git diff --name-status 95175ab72b5ac543327d59ff2d30e82e387a1493 c90d4a8bed45041384b0130ca0a0b6f9a91943b9`, and `git diff --stat 95175ab72b5ac543327d59ff2d30e82e387a1493 c90d4a8bed45041384b0130ca0a0b6f9a91943b9`. Results: eight commits in the ancestry, 31 changed `src/test/java/` files, and 3,411 insertions/three deletions. The remote PR patch and CI job logs were inspected separately.

## Owner review, correction and manual UI evidence

The recorded owner actions were approval of the analysis and implementation waves, clarification that pre-existing local files were out of scope, the request for logical test-only commits on `royden/test-coverage`, and authorization to push. PR comments show an owner request for a Greptile review, but the bot replied that review credits were exhausted; it provided no substantive test review. No recorded owner correction of an individual test assertion or human JavaFX observation was found. Automated JavaFX tests and CI smoke tests are not a manual walkthrough. Manual UI verification is **pending**, not passed.
