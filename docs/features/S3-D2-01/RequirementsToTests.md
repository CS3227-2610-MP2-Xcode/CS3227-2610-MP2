# S3-D2-01 Claims and Verification Requirements to Tests

- Status: Approved
- Draft date: 2026-09-22
- Owner: Developer 2
- Approver: Repository owner
- Approval date: 2026-09-22
- Feature: Claims and Verification
- Mission Brief: `docs/mission-briefs/S3-D2-01-claims-and-verification.md` (Approved 2026-09-22)
- Decision ledger: `docs/features/S3-D2-01/GrillingDecisions.md` (Approved and complete 2026-09-22)
- PRD: `docs/features/S3-D2-01/PRD.md` (Approved 2026-09-22)
- TDD: `docs/features/S3-D2-01/TDD.md` (Approved 2026-09-22)
- Implementation, test-code, branch, commit, push, pull-request, merge, guide, and cross-owner production-change authorization: Not granted

## 1. Authority, scope, and preflight

This artifact defines how evidence will demonstrate the approved Sprint 3 requirements. Precedence is `AGENTS.md`, the approved Mission Brief, approved Grilling Decisions, approved PRD, approved TDD, verified repository testing conventions, then this document. The older upstream metadata saying that RTT was not yet authorized records the gate state when those artifacts were written; the repository owner's 2026-09-22 request is the separate authorization to create only this artifact.

The complete upstream artifacts were read in order. All are approved, their dates and scope agree, and no unresolved product or technical decision remains. Every PRD requirement has a plausible verification path through an approved TDD seam. No product rule or architecture is changed here.

The repository uses JUnit Jupiter 5.14.4, `@TempDir`, fixed `Clock` values, deterministic UUID fixtures/suppliers, immutable state assertions, small scripted repository adapters, and package-local filesystem fault adapters. Real local repositories are preferred for persistence evidence. No JavaFX test framework exists, and none is proposed. Existing RTTs use detailed evidence plans, traceability matrices, manual JavaFX checklists, regression reuse, and implementation-phase command evidence; this artifact follows that convention.

The traceability chain is:

```text
PRD requirement or acceptance criterion
    -> stable verification ID
    -> approved TDD seam and observable outcome
    -> automated, manual, review, regression, or command evidence
```

This plan contains **43 automated verification case families**, **12 manual JavaFX cases**, and **5 review/command evidence cases**. A family may use parameterized inputs and multiple assertions; it is one conceptual behavior, not an inflated count of trivial variations.

## 2. Evidence categories and rules

| Category | IDs | Use |
| --- | --- | --- |
| Domain unit | `TC-CLM-DOM-*` | Claim identity, invariants, text policy, lifecycle, ledger locks and closures. |
| Persistence/integration | `TC-CLM-PER-*` | Real `JsonClaimRepository` at `@TempDir`, strict bytes/schema, restart, atomicity, and supported shared-instance competition. |
| Student application unit/integration | `TC-CLM-STU-*` | Bound-role discovery, safe projections, submission, tracking, withdrawal, refresh, and failures. |
| Desk Officer application unit/integration | `TC-CLM-OFF-*` | Queue/history, selected detail, decision validation, terminal actions, and failures. |
| Privacy/authorization | `TC-CLM-PRV-*` | Wrong-role/cross-Student denial, structural projection exclusions, and redacted secondary outputs. |
| Failure/recovery | `TC-CLM-FLT-*` | Scripted dependency/store failures, retry, post-commit refresh failure, and draft-preservation contracts observable outside JavaFX. |
| Compatibility/integration | `TC-CLM-INT-*` | Read-only report/link integration, retention, composition contracts, and existing regressions. |
| Manual JavaFX | `MV-CLM-*` | Rendered placement, field visibility, confirmations, controls, draft/logout interaction, and small-window usability. |
| Review/command | `RV-CLM-*` | Source/design boundaries, cross-owner edits, build commands, diff/status, and data-safety audit. |

Automated tests use only synthetic users, reports, claims, evidence, and reasons. Real persistence tests use unique `@TempDir` paths and never resolve the application's `data/claims.json`. Privacy sentinels may be asserted absent but must not be printed in assertion labels, logs, snapshots, or handoffs. Manual evidence uses synthetic stores and pass/fail notes; screenshots are not required.

## 3. Domain and lifecycle automated cases

| ID and name | Upstream requirements | Setup and action | Expected observable outcome and negative assertion | Approved seam |
| --- | --- | --- | --- | --- |
| `TC-CLM-DOM-001` Claim construction and restoration invariants | PRD-LC-001, PRD-LC-002, PRD-PR-002, PRD-NF-001 | Construct/restore Pending, Approved, Rejected, and Withdrawn Claims with fixed UUIDs and millisecond Instants; parameterize null, blank claimant, equal endpoints, missing/forbidden reason/time combinations, non-millisecond or reversed event times. | Exactly the four statuses restore; valid values are preserved; every invalid combination is rejected without a partial Claim. No link, report copy, or officer field exists. | `Claim`, `ClaimId`, `ClaimStatus` |
| `TC-CLM-DOM-002` Stable reference, value behavior, and redaction | PRD-LC-001, PRD-ST-003, PRD-PR-006 | Use known UUIDs, equal/unequal Claims, result values, and handles; call reference rendering, equality/hash, and `toString`. | Reference is exactly `CLM-` plus 32 uppercase hex digits and is one-to-one; equality covers retained fields; Claim, result, review, state, and handle strings omit evidence, reason, claimant, report values, IDs where prohibited, and paths. | `ClaimId`, domain/result/projection values |
| `TC-CLM-DOM-003` Shared text-policy partitions and boundaries | PRD-SC-007, PRD-DC-002, PRD-PR-006 | Parameterized required evidence, mandatory rejection reason, and optional approval reason: null/absent, empty, Unicode whitespace, invisible-only, 1/499/500/501 code points after `strip`, leading/trailing whitespace, punctuation, Unicode, LF, CRLF, lone CR, tab, ISO control, FORMAT code point, malformed surrogate. | Valid text is trimmed once and retained; LF/CRLF, punctuation, and ordinary Unicode pass; blank/invisible, 501, prohibited control, lone CR, and malformed Unicode fail with fixed field-category feedback that never repeats input. Blank optional approval becomes absent. | `ClaimTextPolicy`, `ClaimValidationException` |
| `TC-CLM-DOM-004` Lifecycle transitions and evidence immutability | PRD-LC-002, PRD-ST-004, PRD-ST-005, PRD-ST-006, PRD-DC-001, PRD-DC-005 | From one Pending Claim, approve with absent/valid reason, reject with valid reason, or withdraw at fixed later times; attempt every terminal-to-terminal and terminal-to-Pending path. | Each valid transition creates one immutable copy, retains ID/claimant/endpoints/evidence/submission time, records the right terminal fields, and leaves the original unchanged. Every terminal Claim rejects another transition. | `Claim` transition methods |
| `TC-CLM-DOM-005` Ledger locks, closures, and resubmission | PRD-LC-003–PRD-LC-008, PRD-IC-003 | Build ledgers covering active duplicate pair, shared LOST, shared FOUND, withdrawal, rejection, approval, same/different claimant, and same/different FOUND. Evaluate candidate submissions. | Pending creates pair/LOST/FOUND locks; withdrawal releases both and permits otherwise eligible same-pair resubmission; rejection releases locks but permanently closes claimant-to-FOUND and forbids same-pair resubmission; other Students and other FOUND targets remain eligible; approval permanently closes both endpoints. Own active blockers return only the owner's Claim; other blockers are generic. | `ClaimLedger` |
| `TC-CLM-DOM-006` Store-wide contradiction detection | PRD-LC-003–PRD-LC-009, PRD-NF-002 | Validate complete retained snapshots with duplicate IDs, two active LOST/FOUND locks, approved-endpoint reuse, rejection-closure violation, invalid same-pair history, and permitted rejected/withdrawn endpoint reuse. | Every contradictory snapshot is rejected as a whole; every permitted history is accepted; no entry is silently dropped or repaired. | `ClaimLedger` validation |

## 4. Persistence and competing-action automated cases

Every real-filesystem case below uses a unique `@TempDir` target and fresh repository instances where restart is part of the behavior.

| ID and name | Upstream requirements | Setup and action | Expected observable outcome and negative assertion | Approved seam |
| --- | --- | --- | --- | --- |
| `TC-CLM-PER-001` Missing, empty, and canonical round trip | PRD-LC-001, PRD-IC-003, PRD-NF-001 | Load a missing target/parent; then submit synthetic Claims and reopen with a fresh repository. | Missing means empty and creates no file. Committed Pending and all terminal variants round-trip in submission insertion order with exact identity, evidence, claimant, status, times, and reason. | `JsonClaimRepository`, `@TempDir` |
| `TC-CLM-PER-002` Strict schema and canonical encoding | PRD-LC-001, PRD-PR-006, PRD-NF-001 | Read valid noncanonical whitespace/member order and parameterized invalid bytes: zero/empty object, malformed UTF-8/JSON, BOM, trailing data, duplicate/unknown/missing members, wrong types, bad escapes/surrogates, noncanonical UUID/time, unknown status, invalid nullability. Perform a genuine mutation after a valid noncanonical read. | Valid input loads without read-time rewrite; later mutation emits exact two-space/LF/final-LF canonical bytes. Every invalid document fails whole-store with fixed safe reason and unchanged bytes; no partial success or excerpt/path is exposed. | `ClaimStoreJsonCodec`, `JsonClaimRepository` |
| `TC-CLM-PER-003` Version and size boundaries | PRD-FL-002, PRD-NF-001 | Exercise numeric version 1, nonnumeric and unsupported versions, input at/over 16,777,216 bytes, and candidate output at/over the configured bound. | Only version 1 and inclusive-size input are accepted; unsupported version is distinguished from corruption; oversized input/output is rejected before partial publication/write, preserving the target. | Real repository plus bounded constructor seam |
| `TC-CLM-PER-004` Restart reconstructs locks and closures | PRD-LC-004–PRD-LC-008, PRD-NF-001 | Commit Pending, Withdrawn, Rejected, and Approved histories; create a fresh repository/ledger and attempt affected submissions. | Active locks, withdrawal release, rejection claimant-to-FOUND closure, and approval endpoint closures are reconstructed solely from retained Claims and behave identically after restart; no lock sidecar exists. | `JsonClaimRepository`, `ClaimLedger` |
| `TC-CLM-PER-005` Atomic mutation failure preserves prior commit | PRD-FL-001, PRD-NF-001 | Through package-local `ClaimStoreFiles`, fail staging/parent creation, partial write, force, close, atomic move, and unsupported atomic move before commit; cover absent and existing targets. For each applicable pre-commit failure, exercise successful and failed best-effort temporary-file cleanup. | Every stage/write/force/close/move failure before commit returns the correct safe failure, never success, and preserves the previous target or absence. Cleanup after that failure is best effort: cleanup failure may leave an orphan temporary file, but does not redefine the commit boundary. Orphan sibling temps are ignored and never promoted, and there is no non-atomic fallback. | `ClaimStoreFiles` scripted adapter plus real NIO tests |
| `TC-CLM-PER-006` Mutation outcomes and authoritative terminal state | PRD-LC-003, PRD-LC-009, PRD-PR-002, PRD-FL-001 | Exercise submit `CREATED`, ID collision, own active pair/endpoint, generic blocker; withdraw owner mismatch/not found; terminal changed/already terminal/not found. | Typed outcome matches durable state; no rejected operation writes. Unauthorized withdrawal returns no Claim. Repeated/stale terminal result returns only the authoritative terminal Claim and changes nothing. | `ClaimRepository` public commands |
| `TC-CLM-PER-007` Shared-instance competing submissions | PRD-LC-003–PRD-LC-005, PRD-NF-002 | Coordinate simultaneous calls on one repository instance for duplicate pair, shared LOST, shared FOUND, and distinct eligible pairs. | At most one conflicting submission commits; later callers receive the approved nonleaking outcome. Distinct eligible claims may both commit. Fresh load contains no violated invariant. No multi-process guarantee is asserted. | Shared `JsonClaimRepository` instance |
| `TC-CLM-PER-008` Shared-instance competing terminal actions | PRD-LC-009, PRD-NF-002 | Coordinate approve/reject/withdraw calls on one Pending Claim through one shared repository instance. | Exactly one `CHANGED` result commits; every later action returns `ALREADY_TERMINAL` with the same durable final Claim; fresh load contains one terminal state and no partial reason/time combination. | Shared `JsonClaimRepository` instance |

## 5. Student application automated cases

`StudentClaimsService` tests use fixed clocks and deterministic UUID suppliers. Real repositories are used for end-to-end durability; small scripted report, link, and Claim repositories are used only for deterministic staleness and failure outcomes.

| ID and name | Upstream requirements | Setup and action | Expected observable outcome and negative assertion | Approved seam |
| --- | --- | --- | --- | --- |
| `TC-CLM-STU-001` Bound role, entry, and discovery source | PRD-SC-002, PRD-PR-001, PRD-PR-002 | Construct with Student and wrong-role users. Enter with durable links, unlinked matching candidates, LOST not owned by user, reversed/misdirected endpoints, missing reports, both report statuses, and same-reporter pair. | Wrong role is rejected. Only durable links that orient from the bound Student's LOST report to an available FOUND report become candidates; matching score is never required; same-reporter pair is allowed. | `StudentClaimsService`, scripted/real existing repositories |
| `TC-CLM-STU-002` Available projection, blockers, grouping, and order | PRD-SC-004–PRD-SC-006, PRD-NF-003 | Mix multiple LOST groups and FOUND candidates, tie values, another Student's lock, approval closure, rejection closure, and own active blockers. | Groups sort LOST creation newest-first then approved UUID tie; cards sort FOUND occurrence newest-first then tie. Cards expose only approved fields. Other blockers vanish without reason; own blocker produces only a safe existing-Claim notice/handle. Successful empty uses exactly `No available matches right now.` | `StudentClaimsState` from service |
| `TC-CLM-STU-003` Open/review creates nothing | PRD-SC-003, PRD-SC-007, PRD-SC-008 | Begin from a current opaque card, review valid/invalid evidence, inspect summary, and abandon before submit. | Begin/review performs no Claim write or lock. Review returns the same safe summary and normalized evidence; invalid input returns fixed actionable feedback and no review object. No manual ID-entry operation exists. | `beginSubmission`, `reviewSubmission` |
| `TC-CLM-STU-004` Confirmed durable submission and UUID collision retry | PRD-SC-008, PRD-SC-009, PRD-LC-001, PRD-NF-001 | Confirm a review with fixed Clock and supplier yielding zero, one, two, or three collisions; use real Claim storage. | Eligibility is reloaded; success appears only after commit, creates one Pending Claim with one sampled millisecond time, retries at most three IDs, opens/selects My claims, clears only the committed draft, refreshes Available matches, and survives a fresh repository/service instance. Exhaustion creates nothing and is safely reported. | `StudentClaimsService`, real `JsonClaimRepository` |
| `TC-CLM-STU-005` Submission-time stale and blocked matrix | PRD-SC-002, PRD-SC-006, PRD-SC-008, PRD-SC-009, PRD-FL-001 | Between review and submit remove link/report, reverse/change type, change LOST owner, create own duplicate/endpoint blocker, other blocker, rejection closure, or approval closure. | No stale/blocked case creates a Claim or lock or reports success. View refreshes; own active blocker directs to own Claim; all other cases use privacy-safe generic guidance and preserve evidence. | Service with scripted snapshots and repository outcomes |
| `TC-CLM-STU-006` My claims filtering, order, rows, and detail | PRD-ST-001–PRD-ST-004, PRD-PR-003, PRD-PR-005, PRD-NF-003 | Load claims for two Students across all statuses/times; make one/both reports absent and use different LOST/FOUND categories. Select the bound Student's rows. | Only bound Student Claims appear newest submission first with deterministic tie. Row category comes from FOUND, missing fields are unavailable, and Claim stays selectable. Reference/evidence/reason appear only in detail; UTC Instants and safe fields are present; evidence has no edit path. Another Student and prohibited fields are structurally absent. | `StudentClaimsService` / `StudentClaimsState` |
| `TC-CLM-STU-007` Withdrawal authorization and unavailable context | PRD-ST-005, PRD-ST-006, PRD-PR-002 | Withdraw an owned Pending Claim with reports/link present, then absent; attempt another Student's Claim, an unknown Claim, and the authenticated Student's own terminal Claim. | Owned Pending withdrawal needs no report/link read or reason, becomes durable Withdrawn at fixed time, releases locks, remains retained, and refreshes. Another Student's Claim and an unknown Claim expose no Claim and perform no mutation. The authenticated Student's own terminal Claim performs no mutation and may return or refresh to its authoritative terminal status; TC-CLM-STU-008 remains the focused stale-terminal case. | `StudentClaimsService`, `ClaimRepository` |
| `TC-CLM-STU-008` Stale withdrawal and failed withdrawal | PRD-LC-009, PRD-FL-001, PRD-FL-003 | Cause a competing decision before withdrawal; separately inject read/write failure. | Stale action reports/refreshes to authoritative final status without a write. Failure preserves last confirmed Pending presentation and any unrelated draft, gives safe retry, and never claims success. | Student service plus scripted Claim repository |
| `TC-CLM-STU-009` Student load, Retry, Refresh, and entry behavior | PRD-FL-002–PRD-FL-004 | Script Claim/report/link load failures then recovery; call enter, subview refresh/retry, explicit Refresh, and post-action refresh while tracking repository calls. | Failure clears stale rows/detail/actions and shows Unavailable plus Retry, never the empty sentence. Retry/entry/Refresh each perform a new authoritative load; post-action reconciles; there is no timer, listener, polling, or notification API. | Student service state/call recording |
| `TC-CLM-STU-010` Report/link read-only integration and restart visibility | PRD-IC-001, PRD-IC-002, PRD-NF-004 | Snapshot every Item Report value, report-store bytes, link set/bytes; perform discovery, submit, withdraw, and restart using real temporary report/link/Claim stores where practical. | Claims persist and eligibility changes as approved; reports, statuses, report-store v1 bytes, and possible-match links/bytes remain unchanged. Link removal after submission affects later discovery only and does not alter/erase the Claim. | Application/persistence integration |

## 6. Desk Officer application automated cases

| ID and name | Upstream requirements | Setup and action | Expected observable outcome and negative assertion | Approved seam |
| --- | --- | --- | --- | --- |
| `TC-CLM-OFF-001` Bound role and pending queue | PRD-DO-002–PRD-DO-004, PRD-PR-001, PRD-NF-003 | Construct with Desk Officer and wrong role; load mixed-status claims/times with equal-time ties. | Wrong role is rejected. Queue contains every and only Pending Claim oldest-first with deterministic tie, no pending filter, one opaque selection, and no bulk-decision operation. Selection alone performs no write. | `OfficerClaimsService` / state |
| `TC-CLM-OFF-002` Pending row and selected-detail boundary | PRD-DO-003–PRD-DO-005, PRD-PR-004 | Use reports with different categories and synthetic sensitive sentinels; inspect rows before and detail after selection. | Row has only reference, item names, FOUND category, and submission time. Selected detail alone has claimant, evidence, and two complete current canonical read-only reports clearly separated. Row/state strings omit sensitive sentinels. | Officer projections |
| `TC-CLM-OFF-003` Missing canonical report and removed link | PRD-DO-006, PRD-DC-001 | Select Pending Claim with either/both reports absent; separately remove original link while reports remain. | Missing report retains Pending row/Claim, marks current report unavailable, disables both decisions, substitutes no copied data, and exposes Retry. Link removal alone does not affect review or enablement. | Officer service with scripted repositories |
| `TC-CLM-OFF-004` History membership, filters, order, rows, and detail | PRD-DO-007–PRD-DO-009, PRD-IC-003, PRD-NF-003 | Load all statuses with terminal-time ties; apply All/Approved/Rejected/Withdrawn; remove reports/link; select history. | History contains only terminal Claims, exactly four filters, newest terminal first plus deterministic tie. Row uses FOUND category/current unavailable marker and restricted fields; retained selected detail remains read-only with Claim facts and no reconstructed report data. | Officer service/history projections |
| `TC-CLM-OFF-005` Decision-reason review partitions | PRD-DC-002–PRD-DC-004 | Review approve/reject with the text partitions from DOM-003, cancellation represented by no command, and known references. | Rejection requires valid nonblank reason; approval accepts absent/blank-as-absent or valid text. Invalid input creates no confirmation value/write and returns fixed safe feedback. Review value contains normalized reason/reference only and is redacted. | `reviewDecision`, `DecisionReview` |
| `TC-CLM-OFF-006` Durable approval and rejection | PRD-DC-001, PRD-DC-005, PRD-DC-006, PRD-LC-004–PRD-LC-008, PRD-NF-001 | With fixed Clock and real Claim store, approve with absent/valid reason and reject with valid reason; reopen fresh service/repository. | Success only after durable transition; terminal time/reason is exact, no officer identity is accepted/stored, queue removes/clears selection, history immediately contains record, and restart preserves approval closures or rejection release/claimant-to-FOUND closure. Reports/links remain unchanged. | Officer service plus real repository |
| `TC-CLM-OFF-007` Stale/repeated/competing officer decisions | PRD-LC-009, PRD-DC-005, PRD-FL-003 | Decide through one service then repeat/stale from another; combine with PER-008 competition. | First commit wins; later service changes nothing, preserves retained history, clears no unrelated draft, reports authoritative final status, and refreshes pending/history. | Officer service, shared repository |
| `TC-CLM-OFF-008` Officer load/mutation failure and recovery | PRD-FL-001–PRD-FL-004 | Script Claim/report load failure, decision write failure, Retry, explicit Refresh, view entry, and post-action reload failure. | Load failure clears stale rows/private detail/actions and is distinct from successful empty. Mutation failure preserves Pending and reason text, reports no success, and permits retry. A committed decision remains success even if later refresh fails; affected list becomes Unavailable. No background channel exists. | Officer service scripted dependencies |

## 7. Privacy, authorization, failure, and compatibility cases

| ID and name | Upstream requirements | Setup and action | Expected observable outcome and negative assertion | Approved seam |
| --- | --- | --- | --- | --- |
| `TC-CLM-PRV-001` Cross-role and cross-Student denial | PRD-PR-001, PRD-PR-002 | Attempt wrong-role service construction, forged or stale handles, and selection/withdrawal of another Student's Claim. | Wrong-role construction is rejected; forged or stale handles fail safely; another Student's Claim is neither disclosed nor mutated. A visible reference or incidental display state never grants authority. Session clear/logout/fresh-login lifecycle evidence belongs to TC-CLM-FLT-004 and MV-CLM-011; this case does not require a cleared service object to become permanently invalid. | Both role services and repository withdrawal check |
| `TC-CLM-PRV-002` Structural projection allowlists | PRD-SC-004, PRD-ST-002–PRD-ST-003, PRD-DO-003, PRD-DO-005, PRD-DO-008–PRD-DO-009, PRD-PR-003–PRD-PR-004 | Reflection/accessor allowlist plus behavioral sentinel assertions for every card, notice, row, review, and detail projection. | Student surfaces cannot expose another identity/Claim/evidence, private detail, descriptions beyond approved fields, matching score/reasons, officer/link metadata, or internal IDs. Officer rows cannot expose selected-detail-only fields. Full reports exist only in selected officer detail. | Narrow record types and returned state |
| `TC-CLM-PRV-003` Safe errors, diagnostics, and secondary values | PRD-PR-006 | Trigger every validation, store, stale, blocked, authorization, and internal-collision failure; inspect exception messages/causes, feedback, result/state/handle `toString`, and any Claim logging calls. | No evidence, reason, unsafe input, claimant/other Student, private report data, credentials, storage contents/path, raw cause, or implementation internals appear. Fixtures remain synthetic. | Exceptions, typed feedback, redacted values, source review companion |
| `TC-CLM-FLT-001` Dependency-load failure matrix | PRD-FL-002, PRD-FL-003 | Make Claim/report/link adapters fail independently for each Student/Officer subview, then recover on Retry. | Each affected view hides stale rows/detail and disables actions; unavailable is never empty; Retry makes a fresh load and can recover without leaking dependency details. | Scripted existing repository interfaces |
| `TC-CLM-FLT-002` Failed mutation preserves confirmed state and draft contract | PRD-FL-001 | Inject submit/withdraw/approve/reject failures before commit and compare durable bytes/state plus service outcomes. | Prior committed state/locks remain exact, no partial success is reported, evidence/reason remains available to the pane, and explicit retry is possible. | Service/repository fault seams |
| `TC-CLM-FLT-003` Post-commit refresh failure stays truthful | PRD-FL-001–PRD-FL-003, PRD-NF-001 | Allow commit, then fail the next authoritative list/report load. | The committed Claim/status is still reported as success; no retry of the mutation occurs; affected list becomes unavailable with Retry and restart proves the commit. | Scripted read-after-commit failure plus real store |
| `TC-CLM-FLT-004` Clear and fresh-login service lifecycle | PRD-FL-005 | Populate lists/selections/private detail/feedback, call service `clear`, then create fresh per-login services against durable stores. | In-memory rows, selections, snapshots, private detail, and feedback are removed without storage writes. Fresh service restores durable Claims only, not transient selection or draft state. JavaFX text-warning/cancel behavior remains manual. | Both services and `ClaimWorkspaceFactory` |
| `TC-CLM-INT-001` Canonical report contract preservation | PRD-IC-001, PRD-NF-004 | Run Claim flows beside canonical reports in both statuses and compare objects/store bytes; inspect dependencies for shadow report types or writes. | Claims consume `ItemReport`/existing enums directly, accept both report statuses, never call report mutation methods, add no status/field/schema, and leave existing report behavior/bytes unchanged. | Integration test plus source review |
| `TC-CLM-INT-002` Possible-match contract preservation | PRD-IC-002, PRD-NF-004 | Snapshot pair set/file bytes; run discovery/submission/withdrawal/review/decision/history and remove a link after submission. | Claim code calls link repository reads only; no Claim action creates/removes/relabels a link or invokes matching score. Removed link blocks future submission but not retained review/history/withdrawal. | Integration test plus source review |
| `TC-CLM-INT-003` Retention and absence of deletion | PRD-IC-003 | Accumulate Claims in every status, restart, remove referenced report/link, and enumerate public service/repository operations. | Every Claim remains retained/selectable as approved; missing context uses unavailable markers; neither role nor repository exposes a Claim-deletion operation. | Real repository and API/source review |
| `TC-CLM-INT-004` Shared application-lifetime repository composition | PRD-IC-004, PRD-NF-002, PRD-NF-004 | After separately authorized composition, construct Student and Officer features from the application factory and trace repository identity/lifecycle. | Both roles receive services backed by one application-lifetime Claim repository instance; each login gets fresh panes/services; existing repositories and workspace behaviors remain intact. No edit is authorized by this plan. | Composition test/source review after gated edits |

## 8. Manual JavaFX verification

Run with an isolated synthetic copied data directory or temporary launch fixture. Record pass/fail notes only. Do not capture credentials, evidence, private report details, identities, paths, or other sensitive content in screenshots or handoffs.

| ID and name | Requirements | Procedure and expected evidence |
| --- | --- | --- |
| `MV-CLM-001` Student navigation and defaults | PRD-SC-001, PRD-IC-004 | Log in as a synthetic Student. Confirm top tabs remain **Report an item**, **My reports**, **Claims**; Claims is third; fixed sub-tabs are **Available matches**, **My claims** with Available selected. Existing report tabs still behave unchanged. |
| `MV-CLM-002` Available cards, grouping, copy, and privacy | PRD-SC-003–PRD-SC-006 | Confirm grouping/order and exact approved card fields; open a card and confirm safe directional summary/evidence field with no mutation. Confirm the sole empty sentence is exactly `No available matches right now.` and hidden blockers reveal no identity/reason. |
| `MV-CLM-003` Submission review and confirmation | PRD-SC-007–PRD-SC-009 | Enter multiline/boundary synthetic evidence; confirm review shows trimmed evidence and safe summary. Cancel returns to editable text and creates nothing. Confirmed durable success opens/selects My claims, clears the committed draft, and refreshes matches. |
| `MV-CLM-004` Student rows, detail, UTC, and unavailable markers | PRD-ST-001–PRD-ST-004, PRD-PR-005 | Confirm row/detail field placement, reference only in detail, FOUND category source, status, explicitly labelled absolute UTC times, safe reason, immutable evidence, and usable unavailable markers without invented report content. |
| `MV-CLM-005` Withdrawal confirmation and stale result | PRD-ST-005–PRD-ST-006 | Pending owned Claim alone shows Withdraw. Confirm irreversible dialog has no reason field; cancellation changes nothing; success shows Withdrawn. A stale/terminal attempt refreshes to the final status without a second success. |
| `MV-CLM-006` Officer navigation, queue, and single selection | PRD-DO-001–PRD-DO-004 | Confirm Claims appears after **Possible matches**; fixed **Pending review**/**Claim history** sub-tabs default to Pending. Queue is oldest-first, has no filters/bulk controls, permits one selection, and selection alone changes nothing. Existing officer tabs remain unchanged. |
| `MV-CLM-007` Selected-only officer verification detail | PRD-DO-003–PRD-DO-006, PRD-PR-004 | Before selection, verify rows omit claimant/evidence/descriptions/location/private detail/reason. After selection, verify Claim facts plus clearly separated complete read-only LOST/FOUND reports; missing report disables both decisions and shows Retry. Removed link alone does not disable them. |
| `MV-CLM-008` Approval and rejection confirmation | PRD-DC-001–PRD-DC-005 | Confirm the decision UI clearly tells the Desk Officer that every Student-visible reason must be safe for the Student and must not reveal another claimant or private FOUND-report information; this is interaction guidance, not automated semantic moderation. Confirm invalid rejection/approval text blocks confirmation; valid rejection and absent/valid approval reason reach distinct irreversible dialogs naming reference/final status. Cancel preserves text. Success clears pending detail/reason, shows final status, and makes history reachable. |
| `MV-CLM-009` Officer history presentation | PRD-DO-007–PRD-DO-009 | Confirm exactly All/Approved/Rejected/Withdrawn filters, newest-terminal-first rows, approved row fields, reference, read-only detail, UTC times, and retained detail with unavailable current reports and no reconstruction. |
| `MV-CLM-010` Empty, unavailable, Retry, Refresh, and no live channel | PRD-FL-002–PRD-FL-004 | For each role, distinguish successful empty from Unavailable. Unavailable hides stale/private content and disables actions while Retry remains. Verify entry, explicit Refresh, and post-action refresh; leave views idle and confirm no polling, live update, or notification behavior. |
| `MV-CLM-011` Draft preservation and logout lifecycle | PRD-FL-001, PRD-FL-005 | With unsaved evidence and separately unsaved reason, navigate/re-enter/change sub-tab/Refresh/Retry/trigger synthetic failure and confirm text stays bound to its target. Logout warns; cancel preserves same view/text; confirm clears lists, selection, private detail, and text. Later login restores no draft/selection. |
| `MV-CLM-012` Long-text and small-window usability | PRD-SC-007, PRD-DC-002, PRD-NF-004 | Use 500-code-point multiline synthetic evidence/reason and the project's small supported window. Confirm wrapping/scrolling keeps summaries, fields, details, confirmations, Retry, and actions usable without exposing hidden data in tooltip/accessibility text. |

## 9. Review, command, regression, and cross-owner evidence

### Review and command cases

| ID | Evidence and expected result |
| --- | --- |
| `RV-CLM-001` | Source/design review confirms Claim has only the nine approved retained fields; no copied report/link/match/officer/deletion/lock field; role-specific projections and opaque handles match TDD allowlists; all relevant strings/errors are redacted. Covers PRD-LC-001, PRD-DC-006, PRD-PR-003–PRD-PR-006, PRD-NF-004. |
| `RV-CLM-002` | Source review confirms report and link integrations call `loadAll` only, matching score is not consulted, no shadow canonical type exists, no polling/background listener exists, and no public raw `saveAll`, transaction callback, test toggle, or delete API was added. Covers PRD-FL-004, PRD-IC-001–PRD-IC-003, PRD-NF-004. |
| `RV-CLM-003` | After separate authorization, review exact cross-owner diffs: Student Claims third after My reports; Officer Claims after Possible matches; existing tabs unchanged; `FindersKeepersApp` creates one shared Claim repository; logout uses neutral `SessionView`; only exact Claim ignore entries are added. Covers PRD-SC-001, PRD-DO-001, PRD-FL-005, PRD-IC-004. |
| `RV-CLM-004` | Implementation-phase commands: focused Claim suites, `gradlew.bat test`, `gradlew.bat check`, and—because startup/composition is affected—`gradlew.bat release` plus packaged synthetic smoke. All must pass or be reported exactly; no invented code-coverage threshold applies. |
| `RV-CLM-005` | Final `git diff --check`, complete `git diff`, and `git status --short` confirm only authorized implementation/test/resource/docs changes, no real `data/` or user file changed, all fixtures are synthetic, and cross-owner edits have explicit authorization. |

### Existing regression suites reused rather than duplicated

| Existing suite(s) | Existing evidence retained |
| --- | --- |
| `AuthenticationCoordinatorTest`, `AuthenticationFactoryTest`, `JsonUserRepositoryTest`, `Pbkdf2PasswordHasherTest`, `AccountProvisionerTest` | Role routing, logout/session behavior, authentication persistence, and credential privacy. |
| `ItemReportCreationTest`, `ItemReportValidationTest`, `ItemReportImmutabilityTest`, `ReportEnumParsingTest` | Canonical report fields/enums, immutability, validation, and redacted report strings. |
| `JsonReportRepositoryTest`, `JsonReportRepositoryStorageTest`, `JsonReportRepositoryFormatTest`, `JsonReportRepositoryRecoveryTest`, `JsonReportRepositoryReplacementTest`, `JsonReportRepositorySecurityTest`, `JsonReportRepositoryConcurrencyTest` | Strict report-store v1 durability, bytes, bounds, atomic recovery, security, and synchronization. |
| `ReportSubmissionServiceTest`, `StudentReportFormControllerTest`, `SubmissionViewStateTest`, `StudentReportWorkspaceFactoryTest` | Existing Student submission and workspace composition. |
| `StudentReportHistoryServiceTest`, `StudentReportHistoryPersistenceTest`, `StudentReportHistoryControllerTest`, `ReportHistoryViewStateTest` | Existing Student Reporter-ID isolation, report history, persistence, and safe failure states. |
| `DeskOfficerReviewServiceTest`, `DeskOfficerReviewPersistenceTest` | Existing Desk Officer report queue, selected private-detail boundary, status mutation, retry, and persistence. |
| `PossibleMatchPairTest`, `DeterministicMatcherTest`, `FilePossibleMatchRepositoryTest`, `FilePossibleMatchRepositoryFormatTest`, `FilePossibleMatchRepositoryRecoveryTest`, `OfficerMatchingServiceTest`, `OfficerMatchingPersistenceTest` | Pair/link semantics, matching, possible-match persistence, officer matching, privacy, and link invariance. |
| `ProjectSetupTest` | Application metadata and Java 25 baseline. |

These suites prove inherited contracts only. They do not replace Claim domain, Claim persistence, Claim service, privacy, or manual UI evidence.

### Cross-owner integration evidence (not implementation authorization)

| Gated surface | Required later evidence |
| --- | --- |
| `FindersKeepersApp.java` | `TC-CLM-INT-004`, `RV-CLM-003`, auth/report/matching regressions, full checks, release, and packaged synthetic Student/Officer/login/logout/restart smoke prove one application-lifetime repository and unchanged shell/startup behavior. |
| `StudentReportWorkspaceFactory.java` and `StudentReportHomePane.java` | `MV-CLM-001`, `MV-CLM-011`, `RV-CLM-003`, Student report regressions, and composition tests prove the third tab, entry callback, session delegation, and unchanged existing Student tabs. |
| `AuthenticationPane.java` and `DeskOfficerWorkspacePane.java` | `MV-CLM-006`, `MV-CLM-011`, `RV-CLM-003`, auth/officer regressions, and packaged smoke prove authenticated identity injection, officer tab placement, warning/cancel/clear behavior, and fresh-login state. |
| `.gitignore` | Diff review confirms only `/data/claims.json` and `/data/.claim-store-*.tmp` are added after separate authorization. |

## 10. Requirement-to-evidence matrix

Every substantive PRD requirement appears exactly once below as a row; reused evidence is intentional.

| Requirement | Verification IDs |
| --- | --- |
| PRD-LC-001 | DOM-001, DOM-002, PER-001, PER-002, STU-004, RV-CLM-001 |
| PRD-LC-002 | DOM-001, DOM-004 |
| PRD-LC-003 | DOM-005, DOM-006, PER-006, PER-007, STU-005 |
| PRD-LC-004 | DOM-005, PER-004, PER-007, OFF-006 |
| PRD-LC-005 | DOM-005, PER-004, PER-007, OFF-006 |
| PRD-LC-006 | DOM-005, PER-004, OFF-006 |
| PRD-LC-007 | DOM-005, PER-004, STU-007 |
| PRD-LC-008 | DOM-005, PER-004, OFF-006 |
| PRD-LC-009 | DOM-004, PER-008, STU-008, OFF-007 |
| PRD-SC-001 | MV-CLM-001, RV-CLM-003 |
| PRD-SC-002 | STU-001, STU-005 |
| PRD-SC-003 | STU-003, MV-CLM-002 |
| PRD-SC-004 | STU-002, PRV-002, MV-CLM-002 |
| PRD-SC-005 | STU-002, MV-CLM-002 |
| PRD-SC-006 | DOM-005, STU-002, STU-005 |
| PRD-SC-007 | DOM-003, STU-003, MV-CLM-003 |
| PRD-SC-008 | STU-003–STU-005, MV-CLM-003 |
| PRD-SC-009 | PER-001, STU-004, STU-005, MV-CLM-003 |
| PRD-ST-001 | STU-006 |
| PRD-ST-002 | STU-006, PRV-002, MV-CLM-004 |
| PRD-ST-003 | DOM-002, STU-006, PRV-002, MV-CLM-004 |
| PRD-ST-004 | DOM-004, STU-006, MV-CLM-004 |
| PRD-ST-005 | DOM-004, STU-007, MV-CLM-005 |
| PRD-ST-006 | DOM-004, STU-007, STU-008, MV-CLM-005 |
| PRD-DO-001 | MV-CLM-006, RV-CLM-003 |
| PRD-DO-002 | OFF-001, OFF-008, MV-CLM-006, MV-CLM-010 |
| PRD-DO-003 | OFF-002, PRV-002, MV-CLM-007 |
| PRD-DO-004 | OFF-001, MV-CLM-006 |
| PRD-DO-005 | OFF-002, PRV-002, MV-CLM-007 |
| PRD-DO-006 | OFF-003, MV-CLM-007 |
| PRD-DO-007 | OFF-004, INT-003, MV-CLM-009 |
| PRD-DO-008 | OFF-004, PRV-002, MV-CLM-009 |
| PRD-DO-009 | OFF-004, PRV-002, MV-CLM-009 |
| PRD-DC-001 | DOM-004, OFF-003, OFF-006, MV-CLM-008 |
| PRD-DC-002 | DOM-003, OFF-005, MV-CLM-008 |
| PRD-DC-003 | DOM-003, STU-006, PRV-002, MV-CLM-008 |
| PRD-DC-004 | OFF-005, MV-CLM-008 |
| PRD-DC-005 | DOM-004, OFF-006, OFF-007, MV-CLM-008 |
| PRD-DC-006 | DOM-001, OFF-006, RV-CLM-001 |
| PRD-PR-001 | STU-001, OFF-001, PRV-001 |
| PRD-PR-002 | DOM-001, STU-001, STU-007, PRV-001 |
| PRD-PR-003 | STU-002, STU-006, PRV-002, PRV-003 |
| PRD-PR-004 | OFF-002, OFF-004, PRV-002, MV-CLM-007 |
| PRD-PR-005 | STU-006, MV-CLM-004, MV-CLM-009 |
| PRD-PR-006 | DOM-002, DOM-003, PER-002, PRV-003, RV-CLM-001 |
| PRD-FL-001 | PER-005, STU-005, STU-008, OFF-008, FLT-002, FLT-003, MV-CLM-011 |
| PRD-FL-002 | PER-003, STU-009, OFF-008, FLT-001, MV-CLM-010 |
| PRD-FL-003 | STU-008, STU-009, OFF-007, OFF-008, FLT-001, FLT-003, MV-CLM-010 |
| PRD-FL-004 | STU-009, OFF-008, MV-CLM-010, RV-CLM-002 |
| PRD-FL-005 | FLT-004, MV-CLM-011, RV-CLM-003 |
| PRD-IC-001 | STU-010, INT-001, RV-CLM-002 |
| PRD-IC-002 | STU-010, INT-002, RV-CLM-002 |
| PRD-IC-003 | DOM-005, PER-001, OFF-004, INT-003, RV-CLM-002 |
| PRD-IC-004 | INT-004, MV-CLM-001, MV-CLM-006, RV-CLM-003 |
| PRD-NF-001 | DOM-001, PER-001–PER-005, STU-004, OFF-006, FLT-003 |
| PRD-NF-002 | DOM-006, PER-007, PER-008, INT-004 |
| PRD-NF-003 | STU-002, STU-006, OFF-001, OFF-004 |
| PRD-NF-004 | STU-010, INT-001–INT-004, MV-CLM-012, RV-CLM-001–RV-CLM-003 |

In this matrix, abbreviated automated IDs such as `DOM-001` mean `TC-CLM-DOM-001`; the full stable IDs are defined in sections 3–7.

## 11. Acceptance-criterion-to-evidence matrix

| Acceptance criterion | Verification IDs |
| --- | --- |
| PRD-AC-S01 | TC-CLM-STU-001, TC-CLM-STU-002, TC-CLM-PRV-002, MV-CLM-002, MV-CLM-010 |
| PRD-AC-S02 | TC-CLM-STU-003, MV-CLM-002 |
| PRD-AC-S03 | TC-CLM-DOM-003, TC-CLM-STU-003, MV-CLM-003 |
| PRD-AC-S04 | TC-CLM-STU-003, TC-CLM-STU-004, MV-CLM-003 |
| PRD-AC-S05 | TC-CLM-PER-001, TC-CLM-PER-004, TC-CLM-STU-004, TC-CLM-STU-010, MV-CLM-003 |
| PRD-AC-S06 | TC-CLM-STU-005, TC-CLM-PRV-003 |
| PRD-AC-S07 | TC-CLM-STU-006, TC-CLM-PRV-002, MV-CLM-004 |
| PRD-AC-S08 | TC-CLM-STU-006, TC-CLM-PRV-002, MV-CLM-004 |
| PRD-AC-S09 | TC-CLM-DOM-005, TC-CLM-PER-004, TC-CLM-STU-007, MV-CLM-005 |
| PRD-AC-S10 | TC-CLM-PER-008, TC-CLM-STU-008, MV-CLM-005 |
| PRD-AC-D01 | TC-CLM-OFF-001, TC-CLM-OFF-002, TC-CLM-OFF-008, MV-CLM-006, MV-CLM-010 |
| PRD-AC-D02 | TC-CLM-OFF-002, TC-CLM-PRV-002, MV-CLM-007 |
| PRD-AC-D03 | TC-CLM-OFF-003, MV-CLM-007 |
| PRD-AC-D04 | TC-CLM-DOM-003, TC-CLM-PER-004, TC-CLM-OFF-005, TC-CLM-OFF-006, MV-CLM-008 |
| PRD-AC-D05 | TC-CLM-DOM-005, TC-CLM-PER-004, TC-CLM-OFF-005, TC-CLM-OFF-006, MV-CLM-008 |
| PRD-AC-D06 | TC-CLM-DOM-003, TC-CLM-OFF-005, MV-CLM-008 |
| PRD-AC-D07 | TC-CLM-PER-008, TC-CLM-OFF-007 |
| PRD-AC-D08 | TC-CLM-OFF-004, TC-CLM-PRV-002, MV-CLM-009 |
| PRD-AC-D09 | TC-CLM-OFF-001, MV-CLM-006, MV-CLM-008 |
| PRD-AC-X01 | TC-CLM-DOM-005, TC-CLM-PER-007, TC-CLM-PER-008, TC-CLM-OFF-006 |
| PRD-AC-X02 | TC-CLM-PER-001, TC-CLM-PER-004, TC-CLM-STU-004, TC-CLM-OFF-006, TC-CLM-INT-003 |
| PRD-AC-X03 | TC-CLM-PRV-001–TC-CLM-PRV-003, MV-CLM-002, MV-CLM-004, MV-CLM-007, MV-CLM-009, RV-CLM-001 |
| PRD-AC-X04 | TC-CLM-PER-005, TC-CLM-STU-008–TC-CLM-STU-009, TC-CLM-OFF-008, TC-CLM-FLT-001–TC-CLM-FLT-003, MV-CLM-010–MV-CLM-011 |
| PRD-AC-X05 | TC-CLM-STU-010, TC-CLM-INT-001–TC-CLM-INT-002, RV-CLM-002 |
| PRD-AC-X06 | TC-CLM-STU-009, TC-CLM-OFF-008, TC-CLM-FLT-004, MV-CLM-010–MV-CLM-011, RV-CLM-003 |

## 12. Reverse traceability: verification case to requirements

Each automated case already states its requirement IDs in sections 3–7. This compact index makes orphan evidence obvious without duplicating the case text.

| Case family | Requirement groups covered |
| --- | --- |
| `TC-CLM-DOM-001–006` | LC-001–009; SC-007; ST-003–006; DC-001–003/005; PR-002/006; IC-003; NF-001/002 |
| `TC-CLM-PER-001–008` | LC-001/003–009; PR-002/006; FL-001/002; IC-003; NF-001/002 |
| `TC-CLM-STU-001–010` | LC-001/003/009; SC-002–009; ST-001–006; PR-001–005; FL-001–004; IC-001/002; NF-001/003/004 |
| `TC-CLM-OFF-001–008` | LC-004–009; DO-002–009; DC-001–006; PR-001/004; FL-001–004; IC-003; NF-001/003 |
| `TC-CLM-PRV-001–003` | SC-004; ST-002/003; DO-003/005/008/009; PR-001–004/006 |
| `TC-CLM-FLT-001–004` | FL-001–005; NF-001 |
| `TC-CLM-INT-001–004` | IC-001–004; NF-002/004 |
| `MV-CLM-001–012` | Presentation/interaction portions of SC-001/003–009, ST-001–006, DO-001–009, DC-001–005, PR-004/005, FL-001–005, IC-004, NF-004 |
| `RV-CLM-001–005` | LC-001; SC-001; DO-001; DC-006; PR-003–006; FL-004/005; IC-001–004; NF-004; repository handoff obligations |

No case lacks an approved requirement. Manual evidence supplements but does not replace automated domain, application, persistence, concurrency, privacy, or failure assertions.

## 13. Planned test structure and fixtures

No test file is created by this artifact. Likely cohesive implementation-time locations are:

| Likely test location | Cases |
| --- | --- |
| `src/test/java/.../claim/model/ClaimTest.java` | DOM-001, DOM-002, DOM-004 |
| `src/test/java/.../claim/model/ClaimTextPolicyTest.java` | DOM-003 |
| `src/test/java/.../claim/model/ClaimLedgerTest.java` | DOM-005, DOM-006 |
| `src/test/java/.../claim/persistence/JsonClaimRepositoryTest.java` | PER-001, PER-004, PER-006 |
| `src/test/java/.../claim/persistence/JsonClaimRepositoryFormatTest.java` | PER-002, PER-003 |
| `src/test/java/.../claim/persistence/JsonClaimRepositoryRecoveryTest.java` | PER-005 |
| `src/test/java/.../claim/persistence/JsonClaimRepositoryConcurrencyTest.java` | PER-007, PER-008 |
| `src/test/java/.../claim/application/StudentClaimsServiceTest.java` | STU-001–009, PRV-001–003, FLT-001/002 |
| `src/test/java/.../claim/application/StudentClaimsPersistenceTest.java` | STU-004, STU-010, FLT-003 |
| `src/test/java/.../claim/application/OfficerClaimsServiceTest.java` | OFF-001–005, OFF-007/008, PRV-001–003, FLT-001/002 |
| `src/test/java/.../claim/application/OfficerClaimsPersistenceTest.java` | OFF-006, FLT-003 |
| `src/test/java/.../claim/bootstrap/ClaimWorkspaceFactoryTest.java` | FLT-004, INT-004 |
| Existing suites in section 9 | INT-001–003 and inherited regression evidence |

The ellipsis expands to `io/github/cs32272610mp2xcode/finderskeepers`. Tests should use JUnit parameterized families for text/schema partitions, fixed millisecond Instants, recognizable UUIDs, a supplier that scripts collisions, report pairs with different categories, and sentinel strings used only for negative privacy assertions. Scripted repositories should record calls and support snapshot changes/fail-next behavior without adding production fault toggles. Real filesystem behavior remains primary wherever practical.

## 14. Suggested later red-green order

This ordering is a planning aid only and does not authorize tests or code:

1. Claim identity, text, lifecycle, and ledger invariants (`DOM-*`).
2. Strict real Claim storage and atomic workflow commands (`PER-*`).
3. Student discovery/projections, then durable submission and withdrawal (`STU-*`, relevant `PRV-*`/`FLT-*`).
4. Officer queue/history projections, then durable decisions (`OFF-*`, remaining `PRV-*`/`FLT-*`).
5. Thin JavaFX panes and manual interaction evidence (`MV-*`).
6. Separately authorized composition, compatibility regressions, release smoke, and final repository audit (`INT-*`, `RV-*`).

## 15. Implementation-phase commands

These commands are recorded, not run during this documentation-only stage:

```text
gradlew.bat test --tests "io.github.cs32272610mp2xcode.finderskeepers.claim.model.*"
gradlew.bat test --tests "io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.*"
gradlew.bat test --tests "io.github.cs32272610mp2xcode.finderskeepers.claim.application.*"
gradlew.bat test
gradlew.bat check
gradlew.bat release
git diff --check
git diff
git status --short
```

`release` and packaged synthetic smoke are required only after the separately authorized startup/composition changes. Before handoff, confirm no test read or wrote the application's normal `data/` store and no real user data appears in outputs.

## 16. Coverage audit and approval gate

- All **58** substantive PRD requirements are present in section 10 and map to at least one concrete evidence ID.
- All **25** PRD acceptance criteria are present in section 11 and map to concrete evidence.
- Reverse references in sections 3–8 and 12 leave no orphan test/evidence family.
- Domain, text, persistence, restart, atomicity, competition, stale action, service projection, authorization, and privacy rules use automated seams.
- Manual evidence is limited to rendered JavaFX placement, field visibility, confirmation, draft/logout interaction, and usability.
- Privacy requirements have structural allowlists and explicit negative sentinel assertions, not only visual review.
- Persistence covers missing/round-trip/restart/strict format/version/size, corrupt or contradictory stores, failed write preservation, and no partial success.
- Supported competing actions are deterministic on one shared repository instance; this plan makes no multi-process guarantee.
- Report-store v1 and possible-match byte/semantic preservation use new invariance checks plus existing regression suites rather than duplicating those suites.
- The three Developer 1/shared production files, the Developer 2 auth/officer files, and `.gitignore` integration evidence are identified but no edit is authorized or performed by this artifact.
- No JavaFX testing dependency, public test-only API, raw `saveAll`, Claim deletion, copied report data, deciding-officer identity, polling, or code coverage threshold is introduced.

Requirements that deliberately use more than one evidence type are the UI integration/projection and lifecycle interactions: PRD-SC-001, PRD-SC-003–PRD-SC-009, PRD-ST-002–PRD-ST-006, PRD-DO-001–PRD-DO-009, PRD-DC-001–PRD-DC-005, PRD-PR-003–PRD-PR-005, PRD-FL-001–PRD-FL-005, PRD-IC-004, and PRD-NF-004. Their business rules are automated while rendered interaction is manual and composition/privacy boundaries receive source or regression review.

No upstream contradiction, unverifiable requirement, or traceability gap was found. The repository owner approved this Requirements-to-Tests artifact on 2026-09-22, completing the planning traceability gate. This approval does not authorize test or production implementation, cross-owner edits, guides, branches, commits, pushes, pull requests, release changes, or merging.
