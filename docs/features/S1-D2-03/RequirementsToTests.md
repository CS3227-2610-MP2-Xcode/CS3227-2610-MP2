# S1-D2-03 Requirements-to-Tests Plan

- Status: Approved
- Draft date: 2026-09-21
- Approver: Repository owner
- Approval date: 2026-09-21
- Feature: Build Desk Officer Review Queue
- Workstream: Developer 2
- Source Mission Brief: `docs/mission-briefs/S1-D2-03-officer-review.md` (Approved 2026-09-20)
- Source PRD: `docs/features/S1-D2-03/PRD.md` (Approved 2026-09-21)
- Source TDD: `docs/features/S1-D2-03/TDD.md` (Approved 2026-09-21)
- Reconciled repository baseline: `a1383ce2cbe6386b67cc675608a10df9ea663261`
- Branch creation authorization: Not granted
- Test implementation authorization: Not granted
- Production implementation authorization: Not granted

## Purpose and gate

This plan maps every approved S1-D2-03 requirement, acceptance criterion, scenario, and extension to the smallest meaningful set of automated, manual, review, and command evidence. It defines what must be tested and why; it does not add product behavior.

Approval of this plan does not authorize test or production implementation. The required feature branch and implementation remain subject to separate authorization. A `Planned` row is not passing evidence.

## Repository inspection summary

The current repository contains the canonical report domain, Student submission workflow, JSON report persistence, authentication/session logic, and placeholder role destinations. It contains no `review` package, Desk Officer queue, review pane, or S1-D2-03 test.

Existing tests already establish inherited contracts that this feature must reuse rather than redundantly retest:

| Existing evidence | Coverage already supplied | S1-D2-03 use |
| --- | --- | --- |
| `AuthenticationCoordinatorTest.authenticatesBothRolesAndDerivesTheirRoutes` | Stored roles derive distinct Student and Desk Officer routes | Reused for role authority; new evidence is limited to lazy Desk Officer mounting and Student non-mounting |
| `AuthenticationCoordinatorTest.clearValidationPreservesSessionWhileLogoutClearsIt` | Logout clears the authenticated identity and returns the coordinator to `LOGIN` | Reused for session clearing; rendered subtree/private-detail removal remains manual and structural evidence |
| `ItemReportImmutabilityTest.returnsCompleteCopyWithNewStatusWithoutMutatingOriginal` | `withStatus(...)` changes only status and leaves the original unchanged | Reused by the review transition test; the service must still prove it invokes this only for an authoritative `SUBMITTED` report |
| `JsonReportRepositoryTest.missingStoreLoadsEmptyWithoutCreatingFilesystemEntries` | A missing store is a side-effect-free empty repository | Reused in the real-repository empty-queue integration test |
| `JsonReportRepositoryTest.loadedReportsAreAnUnmodifiableStructuralSnapshot` | Repository snapshots are immutable and isolated from later mutations | Reused as the service input contract; review state collection immutability remains new evidence |
| `JsonReportRepositoryTest.insertionAndReplacementPreserveStableOrderAcrossReconstruction` | Insert and replacement preserve repository order across reconstruction | Reused for queue order and durable transition confidence |
| `JsonReportRepositoryTest.missingReplacementReturnsMissingConflictWithoutCreatingOrChangingStorage` | Missing replacement targets are typed and do not mutate storage | Reused below the service's concurrent-stale reconciliation test |
| `JsonReportRepositoryReplacementTest.replacementPersistsEachRepresentableMutableFieldAndPreservesImmutableState` | Complete replacement, including status, survives a fresh repository and preserves immutable fields | Reused for persistence mechanics; the review integration test narrows the approved workflow to status-only replacement |
| `JsonReportRepositoryRecoveryTest.everyOperationRereadsTheAuthoritativeTarget` | Each repository operation observes current authoritative storage | Supports the service's authoritative recheck behavior |
| `JsonReportRepositoryRecoveryTest.failureReasonsAreTypedAndDiagnosticsContainNoReportValues` | All six storage reasons are typed, fixed, and privacy-safe | Reused for persistence privacy; the service must still map every reason to context-safe review copy |
| `JsonReportRepositoryStorageTest` fault and complete-document tests | Failed mutations preserve the prior complete store; successful mutation becomes visible only as a complete document | Reused for inherited atomicity; service tests focus on truthful state before/after the call |
| `JsonReportRepositoryFormatTest.canonicalEnumAndTemporalRepresentationsRoundTripExactly` and `everyCanonicalCategoryStoredNameRoundTrips` | Both types/statuses, every category, and canonical temporal values round-trip | Reused for storage compatibility; review presentation still needs canonical label/formatter evidence |
| `ReportSubmissionServiceTest` and `StudentReportFormControllerTest` | Student submission produces `SUBMITTED` reports and safely handles validation/storage failure | Regression gate only; S1-D2-03 does not duplicate Student tests |

There is no current automated JavaFX test seam. The approved TDD explicitly rejects adding one, so rendered controls, exact field inclusion/exclusion, read-only behavior, layout, and visible logout privacy use manual evidence plus source review. Business behavior remains automated at the plain-Java service and real-repository seams.

## Approved test seams

| Seam | Boundary | Use in this plan |
| --- | --- | --- |
| TS-01 | Public `DeskOfficerReviewService` operations observed through immutable `ReviewQueueState` | Primary unit/component seam for queue policy, filters, selection, messages, transition guards, stale reconciliation, Retry context, and deterministic state |
| TS-02 | TS-01 backed by a real `JsonReportRepository` under `@TempDir` | Integration evidence for missing storage and fresh-instance durable status-only replacement |
| TS-03 | Existing `ReportRepository` with a small scripted test adapter | Only deterministic faults and target races that cannot be induced portably; assertions concern public state and logical persisted outcome, not private call choreography |
| TS-04 | Existing authentication coordinator route/session seam plus source review of lazy destination mounting | Role access, logout, Student non-mounting, fresh per-login view, and no pre-authentication report load |
| TS-05 | Manual JavaFX workflow | Mounted controls, five-value rows, eleven-value details, privacy separation, enablement, empty/error rendering, logout clearing, and screenshot crop |
| TS-06 | Source, documentation, Git, and command evidence | Shared path, ignore rule, ownership, canonical reuse, privacy, documentation, regression, release, smoke, and clean-scope evidence |

## Test design rules

- Use only synthetic reports and users. Real or project-local report data is never read or written.
- Every real persistence test uses `@TempDir`; no test touches `data/reports.json`.
- Parameterized case names use fixed safe case IDs. Assertion messages do not include reports, descriptions, Reporter IDs, JSON, paths, exceptions, or unsafe input.
- Tests assert public state and persisted outcomes, not private method calls. The scripted repository may record whether a mutation occurred only where absence of mutation is itself an approved behavior.
- Existing persistence tests remain the authority for JSON grammar, byte bounds, atomic replacement, and storage fault mechanics. S1-D2-03 tests do not repeat those matrices.
- Exact approved user-facing messages are independent test literals.
- Reverse transition is tested as an approved absence: the public review service accepts no caller-selected status, and an authoritative `UNDER_REVIEW` report never reaches `replace(...)`. The production interface must not be widened merely to make a reverse call possible.

### Exact copy oracle

Tests and manual evidence use these owner-approved literals without deriving expected values from production constants:

| Context | Exact copy |
| --- | --- |
| No selected report | `Select a report to view details.` |
| Successful transition | `Review started.` |
| Stale or missing target | `This report is no longer available for review.` |
| Initial, Retry, or reconciliation load failure | `Reports are unavailable. Please try again.` |
| Start review storage failure | `Review could not be started because reports are unavailable. Please try again.` |
| Globally empty | `No submitted reports.` |
| Lost-filter empty | `No submitted reports match the Lost filter.` |
| Found-filter empty | `No submitted reports match the Found filter.` |

## Planned automated evidence catalog

Unless stated otherwise, these tests belong in `DeskOfficerReviewServiceTest`. Real-repository evidence belongs in `DeskOfficerReviewPersistenceTest`. The classes may be combined if doing so improves readability without changing the public seams.

| ID | Stable behavior name | Level / seam | Scenarios and reason | Existing coverage reused | Status |
| --- | --- | --- | --- | --- | --- |
| RT-001 | `enterBuildsSubmittedQueueInRepositoryOrderAcrossReporters` | Unit/component, TS-01 | Valid mixed Lost/Found and Reporter IDs; excludes every `UNDER_REVIEW`; one and several submitted reports; proves no implicit sort and immutable visible list | Ordered/complete snapshots and reconstruction are already covered by `JsonReportRepositoryTest` | Planned |
| RT-002 | `filtersAreExactlyAllLostAndFoundAndPreserveRelativeOrder` | Unit/component, TS-01 | All is default; Lost and Found valid partitions; interleaved types preserve relative order; filter operations do not persist or change report values; enum has exactly three constants | Canonical type parsing is covered by `ReportEnumParsingTest` | Planned |
| RT-003 | `globalAndFilteredEmptyStatesUseExactDistinctCopy` | Unit plus integration, TS-01/TS-02 | Empty repository and repository containing only `UNDER_REVIEW` are globally empty; Lost-empty and Found-empty are both represented; missing store is globally empty and remains absent; all cases clear selection and disable Start review | Missing-store side effects already covered by `JsonReportRepositoryTest` | Planned |
| RT-004 | `selectionTracksOnlyVisibleReportsAcrossFilterChanges` | Unit/component, TS-01 | Initial/no-selection boundary; valid visible selection; selection retained by including filter; cleared by excluding filter; unknown/non-visible ID cannot create hidden selection; neutral prompt and action enablement follow selection | None at review seam | Planned |
| RT-005 | `stateIsImmutableAndDeterministicForEquivalentInputs` | Unit/component, TS-01 | Returned lists cannot be structurally changed; equivalent ordered snapshot/filter/selection yields equal observable state; selection/filter/viewing makes no repository mutation or refresh | Repository snapshot immutability is existing coverage, but service state is new | Planned |
| RT-006 | `startReviewPersistsOnlyStatusFromAuthoritativeReport` | Integration, TS-02 | Selected submitted report is changed in storage between selection and action without changing status; service rechecks, preserves the authoritative ten non-status values, writes `UNDER_REVIEW`, reports success only after replacement, removes the row, clears details, disables action, and preserves active filter; fresh repository reconstructs the result | `withStatus(...)`, complete replacement, order, and fresh reconstruction are existing lower-level coverage | Planned |
| RT-007 | `successfulReviewShowsApplicableRemainingOrEmptyState` | Unit/component, TS-01 | Parameterized boundary outcomes: remaining visible rows; last global submitted report; and last report matching active Lost/Found filter while another submitted type remains; each retains order/filter and shows `Review started.` with the correct queue/empty state | None at review seam | Planned |
| RT-008 | `invalidRepeatReverseAndNoSelectionCannotMutate` | Unit/structural, TS-01 | No-selection call is a no-op without repository access; authoritative `UNDER_REVIEW` target is stale and never replaced; a second action after success cannot repeat because selection cleared; reflection/source assertion proves no public operation accepts `ReportStatus` or another target status | Repository deliberately permits status replacement; `ItemReport.withStatus` is policy-free, so review-specific evidence is required | Planned |
| RT-009 | `missingOrNonSubmittedTargetReconcilesUnderSameFilter` | Unit/component, TS-01 | Parameterized missing and already-under-review precheck; no success/mutation; exact stale message; authoritative submitted queue reload; selection/details clear; filter retained; resulting non-empty, global-empty, and filtered-empty boundaries | Missing repository replacement behavior exists, but precheck reconciliation is new | Planned |
| RT-010 | `replacementTargetRaceReconcilesOrExposesRetryContext` | Unit/component, TS-03 | `replace(...)` reports target missing after valid recheck; successful reconciliation load yields stale state; failed reconciliation load yields load-unavailable with no report values and retained filter; later Retry restores the stale notice and current queue | Lower-level missing-target preservation is existing coverage | Planned |
| RT-011 | `initialLoadFailureRequiresExplicitRetry` | Unit/component, TS-03 | Entry failure for each safe reason family produces exact unavailable copy, no rows/details, disabled Start review, visible Retry, default All, and no empty copy; state changes only after explicit Retry; successful Retry shows queue or true empty state. Assertions prove state/copy omit exception text, reason names, paths, JSON, and report values | Typed reasons/privacy are existing persistence coverage | Planned |
| RT-012 | `startReviewStorageFailuresRetainRetryableSelectionWithoutSuccess` | Unit/component, TS-03 | Precheck load failure and replacement failures other than missing target map to contextual review-failure copy; row, selected canonical report, details, filter, and enabled action remain; no success appears; logical persisted report stays submitted. Parameterized reasons include corrupt/unsupported, I/O/safe replacement, unencodable/over-limit, immutable mismatch, and defensive duplicate ID; assertions prove no technical or report value leaks | Atomic failure preservation and typed reasons are existing lower-level coverage | Planned |
| RT-013 | `selectedStateExposesCanonicalReportForCompleteRendering` | Unit/component, TS-01 | For each canonical category across both types, selecting a visible report returns that exact canonical instance/value set for rendering; identifiers and temporal values remain complete; no duplicate details DTO exists | Every category and temporal value already round-trips in persistence tests | Planned |

The catalog intentionally does not add separate tests for each field copied by `withStatus`, each JSON failure grammar, or each filesystem fault stage. Those behaviors already have stronger lower-level coverage. RT-006 supplies the one feature-level durable composition test needed to prove the workflow uses those contracts correctly.

## Manual, review, and command evidence catalog

| ID | Evidence | Level / seam | Required observation | Status |
| --- | --- | --- | --- | --- |
| MV-001 | `deskOfficerAccessQueueFiltersSelectionAndDetails` | Manual UI/system, TS-05 | Desk Officer login mounts and automatically loads the view; Student login never exposes it; exactly All/Lost/Found appear; rows show exactly the five approved public values; visually identical rows remain separately selectable; all eleven read-only details use canonical labels/formatters; public/private sections are visibly distinct; selection/action behavior matches RT-004 | Planned |
| MV-002 | `reviewEmptyFailureRetryAndLogoutWorkflow` | Manual UI/system, TS-05 | Immediate Start review has no confirmation; success removes the row and shows correct feedback/empty state; global and both filtered-empty copy render inline; load failure is not shown as empty and exposes Retry; transition failure retains details/action; logout removes the view and private values; later Desk Officer login starts fresh at All and reloads | Planned |
| MV-003 | `reviewLayoutAndScreenshotPrivacy` | Manual UI/system, TS-05 | View remains usable at existing scene/minimum sizes; long read-only text wraps/scrolls; screenshot is cropped to queue, uses synthetic rows, and excludes identity, details, private values, credentials, unsafe input, paths, and technical errors | Planned |
| RV-001 | `authenticationMountingIsLazyAndRoleBound` | Source review, TS-04 | Desk Officer supplier is invoked only after successful `DESK_OFFICER` routing; Student path remains unchanged; no review Node is cached; logout replaces the subtree; every later officer login constructs a fresh service/view; report storage is not loaded before officer authentication | Planned |
| RV-002 | `presentationUsesOnlyApprovedCanonicalValues` | Source review, TS-05/TS-06 | Queue cell uses exactly type display name, item name, category display name, ISO occurrence date, and location; details use all eleven canonical accessors and approved formatters; `ItemReport.toString()` is not display copy; controls are read-only and private detail is selected-only | Planned |
| RV-003 | `compositionUsesOneSharedIgnoredReportStore` | Source/Git review, TS-06 | One application-lifetime `JsonReportRepository(Path.of("data", "reports.json"))`; fresh per-login service/view over it; exact root-relative `/data/reports.json` ignore rule; target is ignored and untracked; no store contents committed | Planned |
| RV-004 | `changeSetPreservesCanonicalPrivacyAndOwnershipBoundaries` | Source/diff review, TS-06 | One canonical `ItemReport`; no wrapper/DTO/duplicate enum/status; no repository/schema/domain/Student/dependency/build/CI/release change; review policy stays in service and presentation in separate pane; only pre-approved shell composition edit; no logging or diagnostic path leaks report values | Planned |
| RV-005 | `documentationMatchesDeliveredReviewWorkflow` | Documentation/screenshot review, TS-06 | User Guide, Developer Guide, minimal README correction, and screenshot satisfy FR-014 without describing undelivered behavior or exposing private data | Planned |
| VG-001 | `focusedReviewAndInheritedRegressionTestsPass` | Automated command, TS-06 | Run focused review tests, then existing canonical-domain, persistence, authentication/session, and Student suites; no test skipped, quarantined, or run against project-local storage | Planned |
| VG-002 | `repositoryCheckReleaseAndPackagedSmokePass` | Build/system command, TS-06 | `gradlew.bat check`, `gradlew.bat release`, and packaged `--smoke-test` launch pass because startup/demonstration behavior changes | Planned |
| VG-003 | `finalDiffStatusAndDataSafetyAuditPass` | Git/filesystem review, TS-06 | Diff contains only approved scope; status records intended files; exact report store is ignored/untracked; tests and demonstration touched no real or project-local report data | Planned |

## Requirement traceability and scenario partitions

Each row identifies the approved requirement, observable behavior, appropriate level, representative valid/invalid/boundary/empty scenarios, existing coverage, new evidence, and the governing TDD component or interaction.

| Requirement | Observable behavior | Test level | Valid / invalid / boundary / empty scenarios | Existing coverage | Additional evidence | TDD component / interaction |
| --- | --- | --- | --- | --- | --- | --- |
| FR-001 Desk Officer access | Only an authenticated Desk Officer mounts and loads review; Student cannot; logout removes view/private details; later login is fresh | Integration seam + manual UI + source | Valid officer; invalid Student/stale access; pre-auth and post-logout boundaries; empty store still loads only after authorization | Auth role derivation and coordinator logout tests | MV-001, MV-002, RV-001 | Authentication destination factory, logout/later-login flow, TS-04 |
| FR-002 submitted queue | Every and only `SUBMITTED` report appears in repository order across Reporter IDs; remaining reports never reorder or delete | Unit/component + integration | Mixed statuses/types/reporters; one/many; `UNDER_REVIEW` excluded; empty and only-under-review snapshots | Repository ordered load/replacement and immutable snapshot tests | RT-001, RT-006, RT-007 | Service `enter()`, retained submitted snapshot, `loadAll()` |
| FR-003 exact filters | Exactly All/Lost/Found; All default; predicates preserve relative order and values; active filter survives success/stale handling | Unit/component + manual | Each filter; interleaved types; repeated/no-op filter; success/stale boundaries; no match | Canonical ReportType contract | RT-002, RT-004, RT-006, RT-007, RT-009, RT-010, MV-001 | `ReviewQueueFilter`, `changeFilter(...)`, state rendering |
| FR-004 queue-row summary | Each row renders exactly five approved public values and no excluded value; equal-looking rows retain identity | Manual UI + source review | Both types; every category; identical summaries/different UUIDs; long valid text; no rows | Canonical label/accessor and category storage coverage | RT-013, MV-001, RV-002, MV-003 | Custom `ListCell<ItemReport>`, UUID-backed selection |
| FR-005 selection | Starts clear; visible selection shows details/enables action; including filter retains; excluding/invalid selection clears and restores prompt | Unit/component + manual | Valid visible ID; invalid/unknown/non-visible ID; initial and post-action boundaries; empty/load-unavailable states | None | RT-003, RT-004, RT-007, RT-009, MV-001 | Service selection invariant, derived enablement, pane rendering guard |
| FR-006 complete details | Selected report shows all eleven complete, read-only canonical values with public/private separation | Unit/component + manual + source | Both types/status display; every category; complete UUID/time; long valid descriptions; no selection/error/empty clears values | Domain accessors, equality, enum/temporal persistence coverage | RT-013, MV-001, MV-003, RV-002 | Selected canonical report in state; details renderer and formatters |
| FR-007 explicit Start review | One immediate action performs authoritative recheck; viewing/filtering/retry/logout never mutates | Unit/component + manual | Valid selected action; invalid no-selection; repeated click boundary; non-action interactions | Repository fresh-read contract | RT-002, RT-005, RT-006, RT-008, MV-002 | `startReview()`, authoritative `loadAll()`, UI action |
| FR-008 only valid transition | Only `SUBMITTED -> UNDER_REVIEW` succeeds; repeat/reverse/non-submitted reject without mutation; only status changes | Unit/structural + integration | Valid transition; invalid under-review/repeat/reverse/no selection | `withStatus` status-only copy; repository policy-free replacement | RT-006, RT-008, RT-009 | Service status guard, fixed `UNDER_REVIEW`, no generic transition API |
| FR-009 durable success | Success only after replacement; fresh instance observes status-only result; row/details clear; filter and exact feedback remain; correct resulting empty state | Integration + unit + manual | Remaining rows; last global row; last filtered row; replacement success/failure boundary | Fresh reconstruction, atomic complete replacement, stable order | RT-006, RT-007, RT-012, MV-002 | `withStatus`, `replace`, post-success state derived without reload |
| FR-010 stale/missing target | Missing or non-submitted recheck yields no mutation/success, exact stale copy, authoritative reconciliation, retained filter, cleared selection; failed reconciliation is retryable | Unit/component with scripted faults | Missing; already under review; replacement race; reload succeeds/fails; Retry succeeds; resulting non-empty/global/filtered empty | Missing target typed persistence and authoritative reread tests | RT-009, RT-010 | Stale precheck flow, target-not-found reload, stale Retry context |
| FR-011 storage failure | Missing store is empty; other failures never look empty/successful; initial/reconciliation error hides data and offers explicit Retry; transition error retains retryable selection; no technical leakage | Unit + integration + manual | Initial, Retry, precheck, replacement, reconciliation contexts; all reason families; Retry success/failure; missing store boundary | Full persistence fault/reason/privacy suites | RT-003, RT-010 through RT-012, MV-002 | Explicit availability state and context error mapping |
| FR-012 empty conditions | Exact global, Lost-empty, and Found-empty copy; inline, no values/selection, neutral prompt, disabled action; switching to match restores ordered rows | Unit/component + integration + manual | Missing/zero; only under review; submitted opposite type; success/stale leads empty; switch out of empty | Missing-store persistence test | RT-003, RT-007, RT-009, MV-002 | Ready-state empty derivation order and renderer |
| FR-013 storage/session boundaries | One shared working-directory store, precisely ignored; logout removes selected values; later officer login reloads fresh | Source/Git + manual + regression | Officer/Student/pre-auth; logout with private details; relogin after storage change; missing store; ignored/untracked path | Coordinator logout and repository constructor behavior | MV-002, RV-001, RV-003, VG-003 | Production composition, lazy factory, authenticated subtree lifecycle |
| FR-014 truthful documentation | Guides and screenshot describe only delivered behavior and preserve privacy | Review | All delivered states; no undelivered actions; screenshot boundary excludes identity/private/technical data; no applicable empty input | None | MV-003, RV-005 | Documentation and screenshot deliverables |
| NFR-001 privacy/authorization | Private detail remains only in selected authenticated details and approved persistence; no real data or leaks elsewhere | Unit privacy + manual + source/review | Selected officer details; Student/no-selection/logout/error/screenshot boundaries; failure paths; empty states | Redacted `toString`, persistence diagnostic privacy, auth roles | RT-011, RT-012, MV-001 through MV-003, RV-001 through RV-005, VG-003 | Fixed messages, selected-only rendering, lazy role mount, privacy policy |
| NFR-002 truthful/safe mutation | No optimistic success; invalid/failing actions preserve logical/persisted state | Unit + integration | Valid durable replace; precheck/replace failure; stale/invalid/no selection; empty after success | Repository atomic failure/success coverage | RT-006 through RT-012 | Write ordering and error handling |
| NFR-003 canonical compatibility | Direct canonical types, all categories and labels, no duplicate model/token/status | Unit + source/review | Every category; both types/statuses; canonical date/time; structural absence of duplicates; empty not applicable | Enum/domain/format persistence suites | RT-013, RV-002, RV-004 | Canonical imports/accessors/formatters and `withStatus` |
| NFR-004 deterministic presentation | Same snapshot/filter/selection produces same state; no sort, timer, polling, implicit refresh/retry | Unit + manual/source | Equivalent inputs; repeated filters/selections; explicit Retry only; empty/error states | Ordered repository snapshots | RT-002, RT-005, RT-011, MV-001, RV-004 | Immutable state, synchronous calls, feedback lifecycle |
| NFR-005 test/demo isolation | Only synthetic data and isolated temp storage; project-local store untouched | Review/command | All persistence-affecting automated cases; manual demo/screenshot; no invalid case writes real path; empty not applicable | Existing persistence tests use `@TempDir` | VG-001, VG-003, RV-005 | Test privacy policy, TS-02/TS-03/TS-06 |
| NFR-006 compatibility verification | Existing suites, full check, release, and packaged smoke remain green | Regression/system commands | Focused then full; no skips; packaging and startup boundary; empty not applicable | Current repository suites listed above | VG-001, VG-002 | Build/release gates |
| NFR-007 scope/ownership | Separate review service/view; minimal approved shell edit; no unapproved domain, Student, repository, dependency, release, or CI change | Source/diff review | Intended file set; absence of cross-owner expansion; empty not applicable | Current package boundaries | RV-004, VG-003 | Module structure and affected-file decisions |

## Scenario and extension mapping

| Scenario | Evidence |
| --- | --- |
| SC-001 enter and inspect queue, including Student denial, missing store, and failed load | RT-001, RT-003, RT-011, RT-013, MV-001, RV-001, RV-002 |
| SC-002 filter and preserve/clear selection, including filtered empty | RT-002 through RT-004, MV-001 |
| SC-003 successful review, including global and filtered empty outcomes | RT-006, RT-007, MV-002 |
| SC-004 globally or filter empty | RT-003, RT-007, MV-002 |
| SC-005 stale target, empty outcomes, failed reconciliation, and filter-preserving Retry | RT-009, RT-010 |
| SC-006 initial load failure and explicit Retry | RT-011, MV-002 |
| SC-007 transition persistence failure and explicit action retry | RT-012, MV-002 |
| SC-008 logout clears access and private details | Existing coordinator logout test, MV-002, RV-001 |

## Acceptance-criterion mapping

| Criterion | Evidence |
| --- | --- |
| AC-001 | Existing authentication role/logout tests, MV-001, MV-002, RV-001 |
| AC-002 | RT-001, RT-006 |
| AC-003 | RT-002, RT-004, RT-006, RT-009 |
| AC-004 | RT-013, MV-001, RV-002 |
| AC-005 | RT-003, RT-004, RT-007, RT-009, RT-011 |
| AC-006 | RT-013, MV-001, RV-002 |
| AC-007 | RT-006, RT-007, MV-002 |
| AC-008 | RT-006 plus existing `ItemReportImmutabilityTest` and repository replacement tests |
| AC-009 | RT-008, MV-001 |
| AC-010 | RT-009, RT-010 |
| AC-011 | RT-003, RT-011 |
| AC-012 | RT-010 through RT-012, MV-002 |
| AC-013 | RT-003, RT-007, RT-009, MV-002 |
| AC-014 | RT-013, MV-001, RV-002 |
| AC-015 | MV-003, RV-005 |
| AC-016 | RT-001 through RT-012 |
| AC-017 | VG-001, VG-002 |
| AC-018 | RV-005, VG-001, VG-003 |
| AC-019 | RV-005 |
| AC-020 | RV-003, VG-003 |
| AC-021 | RV-004 |

## Red-green implementation order after authorization

1. RT-001 and RT-003: initial queue, immutable state, and missing-store global empty through the real repository.
2. RT-002, RT-004, and RT-005: filters, selection, both filtered-empty cases, enablement, and deterministic state.
3. RT-013 plus RV-002/MV-001 preparation: canonical details and presentation inputs without adding a second report model.
4. RT-006 through RT-008: durable status-only success and invalid-transition absence.
5. RT-009 through RT-012: stale targets, races, contextual failures, Retry, and privacy-safe mapping.
6. RV-001/RV-003 and MV-001/MV-002: authenticated mounting, shared repository, logout, and manual workflow.
7. MV-003, RV-004/RV-005, and VG-001 through VG-003: documentation, screenshot, regressions, release/smoke, and final audits.

Each automated slice begins with one failing behavior test. Existing tests are not rewritten merely to claim S1-D2-03 coverage.

## Ambiguities and blockers

No approved requirement is untestable and no contradiction was found among the Mission Brief, PRD, TDD, current canonical domain, persistence contract, and authentication contract.

Rendered JavaFX behavior is intentionally manual because the approved scope forbids a new UI-test framework. This is a deliberate evidence allocation, not an untestable requirement: service behavior is automated and the remaining rendering/access observations are explicit in MV-001 through MV-003 and RV-001/RV-002.

Current gates remain:

- separate branch creation authorization; and
- separate test and production implementation authorization.

Commit, push, pull-request, release-artifact commit, and merge authorization also remain outside this plan.

## Approval record

The repository owner approved this Requirements-to-Tests plan on 2026-09-21. This approval confirms the planned public test seams, automated evidence catalog, manual evidence allocation, regression gates, and traceability mappings above. It does not authorize branch creation, test implementation, or production implementation.
