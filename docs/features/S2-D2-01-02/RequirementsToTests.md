# S2-D2-01-02 Officer Possible-Match Workflow Requirements to Tests

- Status: Approved
- Draft date: 2026-09-21
- Owner: Developer 2
- Approver: Repository owner
- Approval date: 2026-09-21
- Feature: S2-D2-01 Deterministic Matching Engine and S2-D2-02 Officer Matching Workspace
- Mission Brief: `docs/mission-briefs/S2-D2-01-02-officer-possible-match-workflow.md` (Approved 2026-09-21)
- Decision ledger: `docs/features/S2-D2-01-02/GrillingDecisions.md` (Approved and complete 2026-09-21)
- PRD: `docs/features/S2-D2-01-02/PRD.md` (Approved 2026-09-21)
- TDD: `docs/features/S2-D2-01-02/TDD.md` (Approved 2026-09-21)
- Implementation, branch creation, and cross-owner implementation authorization: Not granted

## 1. Traceability sources and preflight

This plan uses, in order, the approved Mission Brief, the completed `GrillingDecisions.md`, the approved PRD, and the approved TDD. `AGENTS.md` governs repository ownership, privacy, temporary-storage use, verification, and handoff. Preflight found all four artifacts present and mutually consistent. Their approval metadata names the Repository owner and 2026-09-21. The ledger records no unresolved product decision. The TDD records the owner's GD-009 reconciliation, explains that earlier authorization fields are historical, and provides a technical realization for every final PRD acceptance criterion. No approved source was modified by this planning step.

The traceability hierarchy is:

```text
Mission scope and functional requirement
    -> approved PRD acceptance criterion
    -> approved TDD technical realization and public seam
    -> planned automated, manual, review, build, or smoke evidence
```

The Mission Brief contains 29 functional requirements and 23 acceptance criteria. The final PRD contains 30 acceptance criteria. The Mission Brief and PRD reuse some `ME-AC-*` and `OW-AC-*` labels while refining their wording. This document therefore qualifies those labels by source, for example "Mission Brief ME-AC-001" and "PRD ME-AC-001". This is not a new identifier; it prevents two approved statements with the same label from being confused. All 82 approved source statements are explicitly mapped below. The 47 `GD-*` decisions are covered by their reconciled PRD criteria and the detailed boundary plans in sections 3 through 10.

## 2. Verification categories and evidence rules

The smallest proportionate evidence type is used:

| Evidence type | Use in this plan |
| --- | --- |
| Unit test | Pure pair identity, matching rules, score/reason derivation, immutability, and deterministic ordering. |
| Integration test | `OfficerMatchingService` coordination with report/link repositories, state transitions, stale data, retry, and privacy-safe projections. |
| Persistence/integration test | Real `FilePossibleMatchRepository` against `@TempDir`, including fresh instances, format validation, atomic replacement, and failures. |
| Existing regression test | Canonical reports, report persistence, authentication, Student workflows, officer review, and project setup. |
| Manual JavaFX verification | Rendering, visible field separation, layout usability, action enablement, lifecycle clearing, and wording. |
| Source/design review | Role-gated composition, restricted row fields, redacted state/error types, absence of report writes, no JavaFX test dependency, and guide consistency. |
| Build/repository check | Focused Gradle tests, complete `check`, diff/status review, and real-data exclusion. |
| Packaged smoke verification | Conditional implementation-phase `release` plus launch/login/workspace/logout smoke because startup composition changes. |

Deterministic domain, service, and persistence behavior receives automated evidence. Manual evidence is limited to JavaFX behavior whose automation would require disproportionate infrastructure. No JavaFX testing dependency is planned or authorized.

## 3. Matching-engine verification plan

### Eligibility, category, text, date, score, and reasons

`DeterministicMatcherTest` will use literal expected values rather than a second matcher implementation.

- Eligibility covers all four allowed status combinations (`SUBMITTED/SUBMITTED`, `SUBMITTED/UNDER_REVIEW`, `UNDER_REVIEW/SUBMITTED`, `UNDER_REVIEW/UNDER_REVIEW`), LOST/FOUND input in either order, same-type pairs, self IDs, unsupported/invalid snapshots, and reverse-duplicate suppression. Status and Reporter ID never add points.
- Category covers exact enum equality, unequal categories, and `OTHER` only matching `OTHER`. Category is both the 40-point component and a mandatory gate.
- Keywords cover `Locale.ROOT` case behavior under a temporarily changed default locale; spaces, tabs, punctuation, and symbols as separators; duplicate separator runs; leading/trailing separators; duplicate tokens; one-code-point tokens discarded; supplementary Unicode letters counted by code point; one and multiple exact overlaps; no overlap; substring, singular/plural, accent, and Unicode-normalization exclusions; sorted shared token detail; and the fixed 20-or-0 result.
- Location covers case normalization; leading/trailing separators; spaces, tabs, punctuation, and symbols as equivalent separator runs; `Hall A`, `Hall-A`, and `Hall★A`; `Library, Level 2` versus `Library - Level 2`; one-character runs retained; separator-only strings normalizing to equal empty strings; complete unequal strings; and rejection of partial, alias, accent-folded, or Unicode-normalized matches.
- Date covers day 0, days 1 and 6, day 7 inclusive, day 8, FOUND before LOST, both input orders, extreme `LocalDate` values, and the absence of clock, time-zone, time-of-day, or creation-time effects.
- Score covers each component independently, 50 below threshold, 70 exactly at threshold, 80, and 100; failed category/date gates despite otherwise high raw points; exact 40/20/30/10 accounting; and no count-based keyword bonus or hidden contribution.
- Reasons cover fixed component order, a positive reason only for each passing component, neutral failure outcomes, sorted exact shared-token details, date gap detail, no raw non-qualifying presentation total, and absence of Reporter ID, public/private descriptions, creation time, or unsafe input repetition.

### Determinism, excluded fields, and read-only behavior

Automated evidence will evaluate the same snapshot repeatedly and across several explicit shuffles. Assertions compare candidate pair identity, component outcomes, shared tokens, reason text, scores, and complete ordering. Score ties use UUIDs chosen so canonical UUID-string order differs from natural signed-UUID order, proving the TDD comparator: score descending, then canonical first ID and second ID ascending. Linked ordering receives the corresponding qualifying-first and canonical-pair tie checks in service tests.

Parameterized fixture copies will change only Reporter ID, public description, private identifying detail, creation time, and each other non-matching field. Status changes remain within the two eligible values. Qualification, score, reasons, and order must remain identical. Report references and all eleven values are snapshotted before generation, selection, refresh, Link, and Unlink; matching-only operations must leave report values, status, link repository contents, and report-store bytes unchanged.

### Empty and failure behavior

Matcher tests distinguish an empty report list, only LOST reports, only FOUND reports, and eligible pairs with no qualifier. Service integration tests map a report-load failure, link-store-load failure, invalid duplicate-ID snapshot, and evaluation failure to typed unavailable states. No failure may appear as "no eligible pair", "no qualifying possible match", or "no linked possible matches". Safe feedback is asserted without paths, JSON, exception text, IDs, or private report values.

## 4. Possible-match relationship verification plan

`PossibleMatchPairTest` and repository/service tests cover:

- canonical symmetric identity (`A-B == B-A`) and matching equality/hash;
- null and self-pair rejection before I/O;
- canonical UUID-string ordering and independence from LOST/FOUND orientation;
- set-level duplicate and reverse-duplicate prevention;
- non-exclusive `A-B` and `A-C` relationships with neither displaced;
- storage of canonical Report IDs only, with no `ItemReport`, Reporter ID, report field, score, timestamp, or report copy in the relationship file;
- Link and Unlink changing only the selected pair set, never a report value or status; and
- unknown report IDs retained as structurally valid relationships so stale links remain removable.

## 5. Persistence verification plan

All persistence tests use a unique `@TempDir` path. They must assert that no application `data/` path or user store is read or written.

`FilePossibleMatchRepositoryTest` covers missing target and parent (empty without creating storage), header-only valid empty store, valid pairs, LF and CRLF input, optional final newline, reversed/unsorted input canonicalization, exact canonical output bytes, fresh-instance reconstruction, Link durability, Unlink durability, repeated Link/Unlink no-ops, reverse duplicate prevention, non-exclusive links, unknown IDs retained, and deterministic output ordering.

`FilePossibleMatchRepositoryFormatTest` covers zero bytes, whitespace-only, bad magic, nonnumeric version, unsupported numeric version, blank/extra lines, unknown columns, invalid/uppercase/short UUIDs, self-pairs, exact duplicates, reverse duplicates, malformed separators, BOM, oversized input, and oversized result. Every invalid store rejects the whole read; it never yields a partial set or an empty success and is never silently rewritten.

`FilePossibleMatchRepositoryRecoveryTest` exercises the package-local file seam through repository operations. Scripted read, parent creation, staging, partial-write, force, close, atomic-move, unsupported-atomic-move, and cleanup failures verify typed failures, no false success, no non-atomic fallback, and the old target (or prior absence) remains authoritative before the commit point. Orphan sibling temporary files are ignored. A successful atomic move is the commit point and is not followed by a fallible reload. Tests cover regular target checks, symlink/directory/inaccessible targets where the platform permits deterministic setup, and the 16,777,216-byte bounds. No test claims multi-process, network-filesystem, directory-fsync, or arbitrary power-loss guarantees beyond the approved TDD.

## 6. Link operation verification plan

`OfficerMatchingServiceTest` and `OfficerMatchingPersistenceTest` cover the complete Link flow:

- a selected qualifying unlinked pair is authoritatively reloaded and reevaluated before the write;
- a committed Link appears only after repository `link` returns true, moves the pair from suggestions to linked possible matches, preserves selection, enables Unlink, and survives a fresh link-repository/service instance;
- a report missing before Link, same-type change, category/date gate failure, or score below 70 produces `STALE_PAIR`, no write, no success, and cleared invalid private selection; both currently approved statuses remain eligible;
- an existing pair, including a reverse-order request, yields `ALREADY_LINKED`, remains linked, and creates no duplicate; self/null input yields `INVALID_PAIR` before storage mutation;
- report-load, relationship-load, evaluation, and relationship-write failures retain the last-known unlinked state, label it as last-known, expose safe retry, and never publish optimistic linked state;
- one report can be linked with multiple reports; and
- no Link path calls report `insert`, `replace`, or `withStatus`, changes report bytes, confirms ownership, or creates any status transition.

## 7. Unlink operation verification plan

The same service/persistence suites cover:

- removal of one existing relationship only, with durable absence through a fresh persistence instance;
- other links, including links sharing either endpoint, remaining intact;
- an absent pair returning `ALREADY_UNLINKED` as an idempotent no-op with no new-removal success;
- write failure retaining last-known linked state and retry without false success;
- a still-qualifying pair returning to suggestions, remaining selected, and exposing Link after committed removal;
- a non-qualifying, ineligible, or missing-report pair creating no suggestion and clearing selection after committed removal;
- missing one or both canonical reports never blocking removal of the stored relationship; and
- every report field/status and every non-target relationship remaining unchanged.

## 8. Officer service and state verification plan

Plain-Java integration tests are primary. They cover `enter`, `refresh`, `retry`, `select`, `link`, `unlink`, and `clear` through returned immutable `MatchingWorkspaceState` values.

Evidence includes load order (reports, then links, then evaluation); publication of READY only after the whole snapshot succeeds; separation of qualifying unlinked suggestions and every stored linked relationship; exact score-first and canonical-pair ordering; all-qualifying-pairs-linked versus no-qualifying distinction; independent linked-empty state; selection in either section; selection preservation only when the same pair remains in the same section; refresh clearing moved/disappeared selection; retry starting a fresh load; stale/non-qualifying/missing linked rows; action enablement; and immutable collections/state.

Row assertions enumerate the permitted projection fields and prove absence of Report ID rendering, Reporter ID, descriptions, status, creation time, exact tokens, and `ItemReport` references. Selected comparison assertions permit canonical reports only in an authenticated officer service instance, keep public and private detail distinct, show actual type for stale same-type pairs, and never reconstruct a missing side. `clear()` releases rows, snapshots, selection, reasons, feedback, and restricted data without writing either repository.

## 9. Authorization and privacy verification plan

Existing `AuthenticationCoordinatorTest` supplies role derivation and logout session evidence. Source review of `AuthenticationPane` verifies that the officer supplier is invoked only on `DESK_OFFICER`, the Student route receives only the Student workspace, and logout replaces the authenticated subtree. After the separately approved composition change, source review and packaged smoke verification confirm lazy matching construction, Student exclusion, fresh officer objects on later login, and no stale selection restoration.

Automated state tests use synthetic sentinel strings for Reporter ID, public description, and private detail and assert that the restricted sentinels are absent from row/state `toString`, reasons, empty/error/feedback text, and exception messages. Source review verifies no object-dump logging and no restricted data in accessibility text or tooltips. Manual evidence uses only synthetic reports and must not capture Reporter ID, private detail, signed-in identity, credentials, storage paths, or raw errors. No screenshot is required by the approved artifacts.

Reporter ID and private identifying detail may appear only after an authenticated Desk Officer selects an available comparison. They must never appear in suggestion/linked rows, reasons, empty/error copy, logs, screenshots, test output, handoff summaries, or Student UI. Report ID is permitted only in the selected comparison and, for an otherwise ambiguous missing endpoint, the stale linked entry allowed by the PRD.

## 10. Concise manual JavaFX verification

Use a temporary copied application data directory or an explicitly synthetic demo store; never use real user data. Record pass/fail notes, not screenshots.

1. Log in as a synthetic Student and confirm no possible-match tab, rows, scores, reasons, links, comparison, or other report data is available.
2. Log in as a synthetic Desk Officer and confirm the existing review tab is preserved and the possible-match workspace is available.
3. Confirm unlinked suggestions appear in the automated expected order with LOST/FOUND labels, exact approved row fields, rule score, brief positive reasons, possible-match wording, and no restricted fields.
4. Confirm linked pairs appear once in a distinct section; qualifying links precede stale/unavailable links; no-suggestions, no-qualifying, no-links, and unavailable states are visually distinct.
5. Select a pair and confirm side-by-side read-only LOST/FOUND groups show all eleven fields, public/private descriptions are visibly separated, private detail is labelled officer-only, all four component outcomes are shown, and no edit control exists.
6. Confirm Link is enabled only for a current unlinked suggestion, durable success moves the pair to linked state without ownership language, and Unlink is then enabled. Confirm Unlink removes only that relationship and produces the approved qualifying/non-qualifying post-state.
7. Trigger each safe synthetic unavailable/action-failure fixture available in the implementation smoke setup; confirm old rows are not presented as current after load failure, action failure retains a clearly last-known state, and explicit Retry behaves as approved.
8. With a stale missing-side link, confirm only available data and the allowed Report ID placeholder appear, Unlink remains available, and no missing content is invented.
9. Use maximum-length valid synthetic names, locations, and descriptions at a small supported window size; confirm scrolling keeps comparison, reasons, Link/Unlink, and officer-only separation usable.
10. Log out while private detail is visible; confirm all officer content is
    removed. Log in again as a Desk Officer; confirm a fresh authoritative
    load, durable links preserved, and no restored selection.

## 11. Existing regression coverage reused

The following existing suites must remain green and are reused rather than duplicated:

| Existing suite(s) | Existing evidence retained |
| --- | --- |
| `ItemReportCreationTest`, `ItemReportValidationTest`, `ItemReportImmutabilityTest`, `ReportEnumParsingTest` | Canonical eleven-field model, enum contracts, immutable values, privacy-safe `toString`, and Unicode field boundaries. |
| `JsonReportRepositoryTest`, `JsonReportRepositoryStorageTest`, `JsonReportRepositoryFormatTest`, `JsonReportRepositoryRecoveryTest`, `JsonReportRepositoryReplacementTest`, `JsonReportRepositorySecurityTest`, `JsonReportRepositoryConcurrencyTest` | `ReportRepository`/JSON durability, format, bounded/safe I/O, recovery, replacement, security, and existing synchronization behavior. |
| `AuthenticationCoordinatorTest`, `AuthenticationFactoryTest`, `JsonUserRepositoryTest`, `Pbkdf2PasswordHasherTest`, `AccountProvisionerTest` | Authentication, both role routes, logout, safe failures, credential privacy, and storage composition. |
| `ReportSubmissionServiceTest`, `StudentReportFormControllerTest`, `SubmissionViewStateTest`, `StudentReportWorkspaceFactoryTest` | Student submission and canonical shared repository composition. |
| `StudentReportHistoryServiceTest`, `StudentReportHistoryPersistenceTest`, `StudentReportHistoryControllerTest`, `ReportHistoryViewStateTest` | Student Reporter-ID isolation, history/search, persistence, and safe failure presentation. |
| `DeskOfficerReviewServiceTest`, `DeskOfficerReviewPersistenceTest` | Existing review queue, selection, private-detail boundary, authoritative status mutation, retry, and persistence behavior. |
| `ProjectSetupTest` | Application metadata and Java 25 baseline. |

These suites are regression evidence only where they already prove an inherited contract. They do not replace new matcher, link-persistence, matching-service, or JavaFX manual evidence.

## 12. Cross-owner integration verification

No cross-owner edit is authorized by this artifact.

| Affected component | Separately approved smallest integration | Required verification after that edit | Evidence | Existing Developer 1 behavior preserved |
| --- | --- | --- | --- | --- |
| `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/FindersKeepersApp.java` | Construct one application-lifetime `FilePossibleMatchRepository` at `data/possible-match-links.txt`; change only the lazy officer supplier to create `DeskOfficerWorkspacePane` with the shared repositories. Developer 1 may make the edit. | Student factory receives the same report repository; report path, scene, stage lifecycle, launcher, and smoke logic are unchanged; officer content is still lazy and role-gated; fresh login gets fresh workspace while links persist. | Source/design review, existing Student/auth/review regression suites, `check`, `release`, packaged Desk Officer/Student smoke. | Student submission/history, canonical report storage, initial JavaFX shell, scene dimensions, and release configuration remain unchanged. |
| Developer 1-owned canonical report types | No edit planned. | Matching imports and reads `ItemReport`, `ReportType`, `ItemCategory`, and `ReportStatus` directly; no shadow DTO/model and no report status/schema change. | Source review plus canonical-model regressions and report-byte invariance integration tests. | All validation, enums, factory methods, and Student consumers remain unchanged. |
| Application startup/data integration | Add only the Developer 2 link store and exact `.gitignore` entries after implementation authorization. | Missing link store causes no startup write; report-store v1 is untouched; packaged application creates/updates only link storage on explicit Link/Unlink. | Persistence integration, source review, packaged smoke, `git diff`, `git status --short`. | Existing report/user stores and startup behavior remain intact. |

## 13. Requirements-to-tests matrix

### Mission Brief functional requirements

| Requirement / AC | Behavior being verified | Evidence type | Planned test/evidence location | Scenario(s) | Expected result |
| --- | --- | --- | --- | --- | --- |
| ME-FR-001 | Matcher consumes canonical `ItemReport` snapshots only. | Unit + source review | `DeterministicMatcherTest`; matching package review | Restored canonical reports; inspect dependencies | Direct objects used; no JSON parsing or duplicate report model. |
| ME-FR-002 | Unordered distinct pair identity. | Unit | `PossibleMatchPairTest`; `DeterministicMatcherTest` | self, A-B, B-A | Self rejected; A-B equals B-A and appears once. |
| ME-FR-003 | LOST/FOUND and both allowed statuses; reporter ignored. | Unit | `DeterministicMatcherTest` | all status combinations, same type, reporter changes | Only distinct LOST/FOUND candidates; reporter has no effect. |
| ME-FR-004 | Only four criteria affect results. | Unit | `DeterministicMatcherTest` | mutate each included/excluded field | Only category/name/location/date can alter components or rank. |
| ME-FR-005 | Exact fixed locale/clock/order-independent policy. | Unit | `DeterministicMatcherTest` | changed default locale, shuffled input, repeated run | Same fixed results without clock/random/global-state input. |
| ME-FR-006 | Output exposes pair, components, score, reasons. | Unit + integration | `DeterministicMatcherTest`; `OfficerMatchingServiceTest` | 70/80/100 qualifiers | Complete internally consistent evidence returned. |
| ME-FR-007 | Rule points and reasons are truthful, not confidence. | Unit + manual/review | Matcher/service tests; manual checklist; guide review | pass/fail each component | Exact rule points; no false or ownership claim. |
| ME-FR-008 | Candidate set, evidence, and order are deterministic. | Unit | `DeterministicMatcherTest` | repeats and explicit shuffles | Identical pairs, components, reasons, totals, order. |
| ME-FR-009 | Stable total tie-break. | Unit | `DeterministicMatcherTest` | equal scores; UUID string/natural order disagreement | Canonical two-ID string tuple resolves every tie. |
| ME-FR-010 | Matching/viewing is read only. | Unit + integration | Matcher/service tests; report byte snapshot | generate, refresh, select, compare | No report, status, or link mutation. |
| ME-FR-011 | Report-load failure is unavailable and privacy safe. | Integration | `OfficerMatchingServiceTest` | scripted report read failure | UNAVAILABLE, Retry, no empty claim or unsafe content. |
| ME-FR-012 | Possible-match language only. | Source/design + manual | UI/guide review; checklist | rows, reasons, scores, actions, errors | No matched/owned/verified/claimed/confirmed implication. |
| OW-FR-001 | Only authenticated Desk Officer mounts workspace. | Existing regression + source/manual | `AuthenticationCoordinatorTest`; `AuthenticationPane`/composition review | Student login; officer login | Student excluded; officer supplier lazy and available. |
| OW-FR-002 | Ordered suggestions separated from links. | Integration + manual | `OfficerMatchingServiceTest`; checklist | mixed linked/unlinked qualifying pairs | Correct order and mutually exclusive sections. |
| OW-FR-003 | Empty and unavailable states differ with retry. | Integration + manual | `OfficerMatchingServiceTest`; checklist | no eligible, no qualifying, load/evaluation fail | Exact distinct state; Retry only where approved. |
| OW-FR-004 | Read-only side-by-side canonical comparison. | Integration + manual | Service test; checklist | select available pair | Eleven fields per side, labelled type, no edits. |
| OW-FR-005 | Visible reasons exactly match matcher evidence. | Unit + integration + manual | Matcher/service tests; checklist | component combinations | No UI recalculation or hidden factor. |
| OW-FR-006 | Restricted fields confined to selected officer comparison. | Integration + source/manual | Service privacy tests; UI/source review; checklist | rows, selection, errors, logout | Restricted fields only in selected authenticated comparison. |
| OW-FR-007 | Explicit state-appropriate Link/Unlink only. | Integration + manual | `OfficerMatchingServiceTest`; checklist | view/select/link/unlink | View has no side effect; correct action enabled; no ownership claim. |
| OW-FR-008 | Logout clears state; re-entry reloads current stores. | Integration + existing regression + manual | Service `clear` test; auth regression; packaged smoke | select private detail, logout, later login | No visible/selected carryover; durable links reload. |
| LU-FR-001 | Link means symmetric possible-match judgment only. | Unit + persistence + review | Pair/repository tests; copy review | Link A-B/reverse | One symmetric pair; possible-match wording. |
| LU-FR-002 | Relationship state is separate from reports. | Integration + source review | Service/report-byte tests | Link and Unlink | No canonical report field/status changes or encoding abuse. |
| LU-FR-003 | Link success follows durable commit. | Persistence/integration | Repository/service persistence tests | successful Link; fresh instance | Success only after true; pair survives reconstruction. |
| LU-FR-004 | No duplicate; repeated semantics exact. | Unit + persistence + integration | Pair/repository/service tests | repeat and reverse Link | No write/duplicate; ALREADY_LINKED, not new success. |
| LU-FR-005 | Unlink removes only target durably. | Persistence/integration | Repository/service persistence tests | unlink among multiple links; fresh instance | Target absent, other pairs retained, success after commit. |
| LU-FR-006 | Link rechecks current reports and qualification. | Integration | `OfficerMatchingServiceTest` | missing, same-type, gate/threshold/status change | No Link; reconciled STALE_PAIR and safe feedback. |
| LU-FR-007 | Mutation failures remain truthful. | Integration + persistence fault | Service/recovery tests | read/write failures | No success or report change; last-known relation state retained. |
| LU-FR-008 | Linked pairs remain available for Unlink. | Integration + manual | Service test; checklist | qualifying, stale, missing-side links | Each stored pair remains in linked section with Unlink. |
| LU-FR-009 | Links are non-exclusive. | Unit + persistence + integration | Pair/repository/service tests | A-B and A-C | Both coexist; neither displaced or rejected. |

### Mission Brief acceptance-criteria crosswalk

| Requirement / AC | Behavior being verified | Evidence type | Planned test/evidence location | Scenario(s) | Expected result |
| --- | --- | --- | --- | --- | --- |
| Mission Brief ME-AC-001 | Repeated/shuffled snapshots are identical. | Unit | `DeterministicMatcherTest` | repeats and permutations | Same set, components, reasons, scores, order. |
| Mission Brief ME-AC-002 | No self or reversed duplicate. | Unit | Pair/matcher tests | self and reverse inputs | No invalid/duplicate candidate. |
| Mission Brief ME-AC-003 | LOST/FOUND with both statuses. | Unit | Matcher test | four cross-status combinations | All allowed pairs participate identically. |
| Mission Brief ME-AC-004 | Four criteria independently, combined, boundaries. | Unit | Matcher test | rule boundary table in section 3 | Exact approved component/gate results. |
| Mission Brief ME-AC-005 | Excluded fields cannot affect results. | Unit | Matcher test | change each excluded field | Qualification, score, reasons, order unchanged. |
| Mission Brief ME-AC-006 | Scores and reasons fully accounted. | Unit | Matcher test | 50/70/80/100 and gate failures | Exact sum and outcome-corresponding reasons. |
| Mission Brief ME-AC-007 | Ties use documented total order. | Unit | Matcher test | equal scores with adversarial UUIDs | Canonical string tuple, never encounter order. |
| Mission Brief ME-AC-008 | Empty outcomes differ from load failure. | Unit + integration | Matcher/service tests | empty/no qualifier/read failure | Successful empties remain distinct from UNAVAILABLE. |
| Mission Brief ME-AC-009 | Matching has no persistence/report mutation. | Unit + integration | Matcher/service tests | generate/refresh/select | Stores and canonical values unchanged. |
| Mission Brief ME-AC-010 | Possible-match and rule-point language. | Review + manual | UI/docs review; checklist | all visible copy | No confidence/probability/ownership wording. |
| Mission Brief ME-AC-011 | Rules/docs/evidence agree. | Source/design review | Developer Guide, User Guide, tests, TDD cross-check | implementation handoff audit | Rule table, sequence, boundaries, tie-break align. |
| Mission Brief OW-AC-001 | Officer access, Student exclusion, logout clear. | Existing + source/manual | Auth tests; composition review; smoke | Student/officer/logout | Correct role gating and state removal. |
| Mission Brief OW-AC-002 | Ordered rows and linked state shown. | Integration + manual | Service test; checklist | mixed pair states | Engine order and explicit possible-match/link state. |
| Mission Brief OW-AC-003 | Complete read-only selected comparison. | Integration + manual | Service test; checklist | select available pair | Eleven fields, separation, reasons, no mutation. |
| Mission Brief OW-AC-004 | Non-mutating view/refresh/retry actions. | Integration | Service test | view/select/refresh/retry | No report/status/link mutation. |
| Mission Brief OW-AC-005 | Symmetric durable Link and later session. | Persistence/integration + smoke | Repository/service persistence; login smoke | Link then reconstruct/relogin | Exactly one durable link. |
| Mission Brief OW-AC-006 | Durable targeted Unlink and later session. | Persistence/integration + smoke | Repository/service persistence; login smoke | Unlink then reconstruct/relogin | Target remains absent; others remain. |
| Mission Brief OW-AC-007 | Link/Unlink never changes report or confirms ownership. | Integration + review | Service bytes/value test; copy review | both mutations | Reports unchanged; possible-match language only. |
| Mission Brief OW-AC-008 | Reverse/repeat/self/stale/failure semantics. | Unit + integration + persistence | Pair/repository/service tests | all exceptional actions | Exact no-op/error semantics; truthful durable state. |
| Mission Brief OW-AC-009 | Empty, no-link, unavailable distinct. | Integration + manual | Service tests; checklist | each state | Privacy-safe distinct copy and approved Retry. |
| Mission Brief OW-AC-010 | Restricted fields stay selected-officer-only. | Integration + source/manual | Privacy sentinel tests; review/checklist | rows/reasons/errors/logs/screens | No restricted leakage. |
| Mission Brief OW-AC-011 | Non-exclusive linked section supports Unlink. | Persistence/integration + manual | Repository/service tests; checklist | A-B/A-C; stale links | All links reachable independently. |
| Mission Brief OW-AC-012 | Docs, diagrams, tests, evidence agree. | Source/design + build/repository | Documentation audit; `check`; diff/status | final implementation handoff | Delivered behavior and artifacts align with synthetic evidence. |

### Final PRD acceptance criteria

| Requirement / AC | Behavior being verified | Evidence type | Planned test/evidence location | Scenario(s) | Expected result |
| --- | --- | --- | --- | --- | --- |
| PRD ME-AC-001 | Exact LOST/FOUND eligibility and unique pairs. | Unit | Pair/matcher tests | self, same type, reverse | One distinct cross-type candidate only. |
| PRD ME-AC-002 | Both statuses in all combinations; no points. | Unit | Matcher test | four status combinations | Same components/scores for equivalent fields. |
| PRD ME-AC-003 | Four permitted inputs only. | Unit | Matcher test | included/excluded field mutations | Excluded fields cannot alter any result dimension. |
| PRD ME-AC-004 | Exact category gate and 40 points. | Unit | Matcher test | equal, unequal, OTHER cases | Equality +40; inequality fails gate; no wildcard. |
| PRD ME-AC-005 | Exact keyword normalization and 20/0. | Unit | Matcher test | case, separators, code-point length, duplicates, exclusions | Exact unique-token overlap only. |
| PRD ME-AC-006 | Exact location normalization and 30/0. | Unit | Matcher test | spaces/punctuation/symbols/partial/empty normalized | Complete normalized equality only. |
| PRD ME-AC-007 | Directional inclusive date gate and 10/0. | Unit | Matcher test | before, day 0, 7, 8, reversed arguments | Only FOUND 0..7 days after LOST passes. |
| PRD ME-AC-008 | Mandatory gates plus inclusive 70 threshold. | Unit | Matcher test | raw 50/60/70/80/90/100 combinations | Only gate-passing totals >=70 qualify. |
| PRD ME-AC-009 | Exact deterministic component sum. | Unit | Matcher test | every combination, locale/clock/order probes | 0..100 from 40/20/30/10 only. |
| PRD ME-AC-010 | Reasons correspond to outcomes and remain private. | Unit + integration | Matcher/service privacy tests | pass/fail each rule with sentinels | Positive reasons only for passes; no excluded/private values. |
| PRD ME-AC-011 | Score-first deterministic ordering. | Unit | Matcher test | mixed scores and tied UUIDs | Descending score then canonical pair tuple. |
| PRD ME-AC-012 | Full reproducibility under repeat/shuffle. | Unit | Matcher test | repeated snapshots and permutations | All outputs bit-for-bit/value-equal. |
| PRD ME-AC-013 | Generate/refresh/select/compare read only. | Unit + integration | Matcher/service tests | all non-mutating operations | No report/status/relation mutation. |
| PRD ME-AC-014 | No eligible/no qualifying differ from failures. | Unit + integration | Matcher/service tests | both empties and all failure categories | Exact successful-empty versus UNAVAILABLE state. |
| PRD ME-AC-015 | Explainable possible-match language. | Review + manual | UI/docs source review; checklist | rules, rows, actions, guide | Complete explanation; prohibited certainty language absent. |
| PRD OW-AC-001 | Authenticated officer only. | Existing + source/manual | Auth tests; composition review; smoke | Student/officer routes | Student cannot construct or observe matching. |
| PRD OW-AC-002 | Exact privacy-safe suggestion rows and order. | Integration + manual | Service tests; checklist | enter/refresh with mixed scores | Exact fields/order/reasons/terminology; no restricted fields. |
| PRD OW-AC-003 | All eleven fields in read-only selected comparison. | Integration + manual | Service test; checklist | select qualifying pair | Two labelled groups, components and total, no edit. |
| PRD OW-AC-004 | Restricted information boundary. | Integration + source/manual | Sentinel tests; UI/log review; checklist | all presentation/error paths | Reporter/private data only in selected officer comparison. |
| PRD OW-AC-005 | Explicit durable Link transition. | Persistence/integration + manual | Service persistence tests; checklist | valid Link | Commit, move section, preserve selection, no report change. |
| PRD OW-AC-006 | Repeat/reverse Link no-op; self rejection. | Unit + integration | Pair/repository/service tests | repeat, reverse, self | No duplicate/write/false success. |
| PRD OW-AC-007 | Stale Link reconciles safely. | Integration | Service test | missing/ineligible/non-qualifying before Link | No relation; stale row/private selection removed safely. |
| PRD OW-AC-008 | Non-exclusive relationships. | Persistence/integration | Repository/service tests | A-B and A-C | Both links coexist and remain actionable. |
| PRD OW-AC-009 | Linked section retains qualifying/stale/missing links. | Integration + manual | Service test; checklist | current, non-qualifying, same-type, missing endpoint | Each once; exact order/state; no misleading total; Unlink remains. |
| PRD OW-AC-010 | Durable targeted Unlink and post-state. | Persistence/integration + manual | Service persistence tests; checklist | qualifying and stale Unlink | Only target removed; selection preserved iff suggestion returns. |
| PRD OW-AC-011 | Repeated/stale Unlink semantics. | Persistence/integration | Repository/service tests | absent pair; missing endpoint | ALREADY_UNLINKED no-op; stale relation removable. |
| PRD OW-AC-012 | Link/Unlink failure truthfulness and retry. | Integration + persistence fault | Service/recovery tests | read/write failures then retry | Last-known state, no false success/report change, retry works. |
| PRD OW-AC-013 | Every empty/failure state is distinct. | Integration + manual | Service tests; checklist | eight approved state classes | Typed state and privacy-safe distinct presentation. |
| PRD OW-AC-014 | Logout clears; later login reloads durable truth. | Integration + existing + manual/smoke | Service clear; auth tests; packaged smoke | selected private detail then logout/login | No stale presentation/selection; current links/reports reload. |
| PRD OW-AC-015 | No ownership confirmation anywhere. | Source/design + manual | Code/copy/docs review; checklist | all rows/actions/messages/guides | Possible-match judgment only. |

## 14. Planned test structure

No test file is created by this plan.

| Likely test location | Responsibility | Requirements covered | Level |
| --- | --- | --- | --- |
| `src/test/java/.../matching/model/PossibleMatchPairTest.java` | Pair canonicalization, symmetry, identity, comparator, self/null rejection, shared endpoints. | ME-FR-002; LU-FR-001/004/009; PRD ME-AC-001; OW-AC-006/008 | Unit |
| `src/test/java/.../matching/model/DeterministicMatcherTest.java` | Eligibility; four exact rules; Unicode/locale/date boundaries; scores; reasons; determinism; exclusions; immutable output. | ME-FR-001–010; PRD ME-AC-001–014 | Unit |
| `src/test/java/.../matching/persistence/FilePossibleMatchRepositoryTest.java` | Missing/empty/valid stores, canonical bytes, fresh instances, Link/Unlink, no-ops, non-exclusivity, unknown IDs. | LU-FR-001–005/008/009; PRD OW-AC-005/006/008–011 | Persistence/integration |
| `src/test/java/.../matching/persistence/FilePossibleMatchRepositoryFormatTest.java` | Strict grammar, corrupt/duplicate/self/unsupported/size handling. | LU-FR-004/007; PRD OW-AC-006/009/012/013 | Persistence/integration |
| `src/test/java/.../matching/persistence/FilePossibleMatchRepositoryRecoveryTest.java` | Read and atomic-replacement fault injection, old-target preservation, orphan handling. | LU-FR-003/005/007; PRD OW-AC-005/010/012/013 | Persistence/integration |
| `src/test/java/.../matching/application/OfficerMatchingServiceTest.java` | Loading/partitioning, state, selection, refresh/retry, stale actions, privacy, failures, clear, exact action feedback. | OW-FR-002–008; LU-FR-006–009; PRD OW-AC-002–015; PRD ME-AC-010/013/014 | Integration |
| `src/test/java/.../matching/application/OfficerMatchingPersistenceTest.java` | Real report and link repositories, report-byte invariance, fresh service/repository Link/Unlink durability. | ME-FR-010; LU-FR-002/003/005/007; PRD OW-AC-005/010/012/014 | Persistence/integration |
| Existing suites listed in section 11 | Preserve canonical, persistence, auth, Student, review, and setup contracts. | Inherited contracts and regression obligations | Existing regression |
| JavaFX checklist in section 10 | Rendered fields, layout, wording, enablement, lifecycle, role visibility. | OW-FR-001–008; PRD OW-AC-001–005/009/010/013–015 | Manual JavaFX |

The ellipsis in paths expands to `io/github/cs32272610mp2xcode/finderskeepers`. Cohesive behavior suites are preferred over one class per requirement.

## 15. Test data strategy

All fixtures are synthetic and deterministic. A shared test fixture helper may construct canonical reports but must not duplicate matcher logic.

- Use fixed UUIDs with recognizable final segments and at least one pair whose canonical string ordering differs from `UUID.compareTo` signed-long order.
- Use fixed `LocalDate` values around 2026-09-01 for day -1, 0, 1, 7, and 8, plus extreme-date cases; use fixed millisecond-precision `Instant` values.
- Include LOST and FOUND reports in every allowed status combination and all canonical categories needed for equal, unequal, and `OTHER` boundaries.
- Use item names such as `Blue Pencil-Case`, duplicate-token cases, only one-code-point tokens, supplementary Unicode letters, substring and singular/plural exclusions, punctuation/symbol runs, and no-overlap names.
- Use locations covering repeated whitespace, tabs, commas, hyphens, `★`, one-character runs, separator-only values, exact normalized equality, partial mismatch, and accent/normalization non-equivalence.
- Construct component totals 50, 70, 80, and 100, plus raw high totals with a failed mandatory gate and equal-score tied suggestions.
- Create `A-B` and `A-C` to prove non-exclusive links, as well as stored links with a missing endpoint and a current non-qualifying pair.
- Use explicit private sentinels solely for negative assertions. Never print them in assertion labels, logs, snapshots, or handoff output.
- Use `@TempDir` for every real report/link store and scripted in-memory repository doubles only for deterministic failure/staleness. Never point at `data/`, a home directory, or real application/user files.

## 16. Failure-injection strategy

- Report-load failure: a small scripted `ReportRepository` implements the existing interface and throws each `ReportStoreException.Reason` on the next `loadAll`; no production API changes.
- Relationship-load/write failure: a scripted `PossibleMatchRepository` returns a fixed set or throws the approved checked reason on `loadAll`, `link`, or `unlink`. It records attempted operations without report content.
- Evaluation failure: supply the approved duplicate-ID/invalid snapshot at the matcher boundary or a repository snapshot that triggers the documented contract failure; do not add a public matcher failure toggle.
- Stale/missing reports: change the scripted repository snapshot between `select` and Link/Unlink, or use fresh real repository instances where the existing API can express the change. Do not add report deletion or future status APIs solely for tests.
- Filesystem failures: inject the TDD's package-local `PossibleMatchStoreFiles` seam and fail read, staging, partial write, force, close, atomic move, and cleanup in turn. Complement with real `@TempDir` integration. No production test switch or raw-cause exposure.

## 17. Implementation-phase verification commands

These commands are recorded, not run for this documentation-only transition. On Windows, from the repository root:

```text
gradlew.bat test --tests "io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPairTest" --tests "io.github.cs32272610mp2xcode.finderskeepers.matching.model.DeterministicMatcherTest"
gradlew.bat test --tests "io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.*" --tests "io.github.cs32272610mp2xcode.finderskeepers.matching.application.*"
gradlew.bat test
gradlew.bat check
gradlew.bat release
git diff --check
git diff
git status --short
```

`release` and packaged smoke are required when the approved application composition/startup edit is implemented. The smoke launches the packaged artifact with isolated synthetic stores and verifies Student routing, Desk Officer entry, matching load, Link/Unlink, logout, and fresh login. Before handoff, confirm that tests did not create or modify real user data and that only intended source, test, documentation, resource, and exact ignore-file changes appear.

## 18. Coverage audit and approval gate

- Every Mission Brief completion obligation is represented by the 29 functional-requirement rows, 23 Mission Brief AC rows, detailed plans, and regression/cross-owner sections.
- Every final PRD acceptance criterion has a dedicated matrix row and concrete evidence location; every `GD-001` through `GD-047` decision is represented through its reconciled criterion and boundary scenario.
- Every TDD obligation requiring verification has evidence: pair invariants, exact matcher policy, reason derivation, comparator, strict storage grammar, fresh-instance durability, atomic failure behavior, service state machine, privacy, JavaFX lifecycle, composition, and documentation/build checks.
- Every rule boundary is automated, including non-letter/digit separators, code-point token length, separator-only location equality, day 0/7/8 and negative direction, gates versus totals, threshold 70, and deterministic UUID tie-breaking.
- Every persistence mutation has success, no-op, fresh-instance, and failure evidence. All real I/O is isolated under `@TempDir`.
- Authorization and privacy have automated, existing-regression, source-review, and manual evidence. No restricted value is required in screenshots or handoff summaries.
- The sole cross-owner shell edit is identified but not authorized or performed. Existing Developer 1 behavior has explicit regression evidence.
- No planned test changes approved behavior, relies on an unapproved field or status, adds JavaFX automation, or introduces a test-only production API.
- No requirement is mapped to vague evidence where a plain-Java seam exists.
- No unresolved traceability blocker remains.

The repository owner explicitly approved this complete requirements-to-tests plan on 2026-09-21. This approval completes the Mission Brief, product, technical-design, and traceability planning gates. It does not authorize branch creation, production code, tests, the cross-owner `FindersKeepersApp` edit, dependencies, commits, pushes, pull requests, release changes, or merging; those require separate authorization.
