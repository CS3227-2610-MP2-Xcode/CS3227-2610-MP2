# S2-D2-01-02 Officer Possible-Match Workflow

- Status: Approved
- Owner: Developer 2
- Draft date: 2026-09-21
- Approver: Repository owner
- Approval date: 2026-09-21
- Source sprint items: S2-D2-01 Deterministic Matching Engine and S2-D2-02 Officer Matching Workspace
- Proposed combined mission ID: `S2-D2-01-02`
- Integration baseline inspected: `royden/feat-officer-review-queue` at
  `24d7e9dc08d7646ade140d54cbbacebd86147bdc`
- Mission Brief approval: Granted by repository owner, 2026-09-21
- Branch creation authorization: Not granted
- PRD/TDD authorization: Not granted
- Test or production implementation authorization: Not granted
- Governing instructions: `AGENTS.md`

## Mission / goal

Deliver one end-to-end Desk Officer workflow that turns stored lost-and-found
reports into reproducible **possible-match** suggestions and lets an
authenticated Desk Officer inspect, link, and unlink those report pairs without
the software claiming that ownership has been proved.

The two sprint items are one feature:

```text
Canonical Item Reports
        ↓
Deterministic candidate generation and rule evaluation (S2-D2-01)
        ↓
Ordered possible-match suggestions with scores and reasons
        ↓
Authenticated Desk Officer comparison (S2-D2-02)
        ↓
Explicit durable link as a possible match
        ↓
Explicit durable unlink when the relationship is incorrect
```

The matching engine supplies explainable evidence. The officer workspace lets a
human act on that evidence. Neither capability is complete on its own, and
neither confirms ownership.

## User / actor

The primary actor is an authenticated **Desk Officer**.

Students create the canonical LOST and FOUND Item Reports consumed by this
workflow. They do not run matching, see suggestions or matching metadata,
compare other Students' reports, create or remove possible-match links, or gain
access to another report's private identifying detail.

## Problem being solved

Desk Officers need a repeatable way to identify reports that may describe the
same physical item. Manual scanning is inconsistent, while an opaque or
probabilistic recommender would be difficult to explain and could imply more
certainty than the evidence supports.

The system therefore needs to:

- derive the same suggestions, score components, reasons, and order from the
  same report snapshot and rule configuration;
- show the officer why each pair was suggested;
- preserve the distinction between a software suggestion, an officer-created
  possible-match link, and actual ownership verification; and
- preserve explicit link and unlink outcomes across application restarts.

## Current repository baseline

The following are confirmed repository facts rather than proposed design:

- Developer 1's immutable `ItemReport` is the only report representation. It
  contains Report ID, Reporter ID, LOST/FOUND type, item name, category,
  location, occurrence date, public description, private identifying detail,
  SUBMITTED/UNDER_REVIEW status, and creation time.
- `ReportRepository` exposes only `loadAll`, `insert`, and complete
  identity-preserving `replace`. It has no possible-match relationship API.
- `JsonReportRepository` stores strict version-one report JSON, preserves stable
  insertion order, and rejects unknown report or root members. Possible-match
  links cannot be silently added to this schema.
- `FindersKeepersApp` creates one application-lifetime `ReportRepository` at
  `data/reports.json` and shares it across the Student and Desk Officer
  workflows.
- `AuthenticationPane` mounts the Desk Officer destination only after the
  stored role routes to `DESK_OFFICER`. Logout removes the authenticated view.
- The current officer review queue uses a per-view
  `DeskOfficerReviewService` and `DeskOfficerReviewPane`. Its public row omits
  private detail, while a selected authenticated officer details view displays
  all eleven canonical fields with public and private descriptions separated.
- The existing review queue contains only SUBMITTED reports, but that queue's
  membership rule does not define matching eligibility. Starting review changes
  a report to UNDER_REVIEW and removes it from that queue.
- Student history search has locale-stable case normalization and a Report-ID
  final tie-break precedent. Its substring-search rules are not matching-engine
  requirements.
- There is no existing matching engine, match score, possible-match link model,
  link store, matching screen, or S2-D2 planning artifact.
- The current worktree also contains unrelated untracked `evals/` content. It
  is outside this mission and must remain untouched.

## Feature boundary

This mission begins when the authenticated Desk Officer workflow requests the
current canonical report snapshot. It ends when the officer can see deterministic
possible-match suggestions, compare a selected pair, inspect the reasons, and
durably create or remove a relationship explicitly labelled **possible match**.

It includes the matching policy's observable results and the durable lifecycle
of officer-created possible-match relationships. It does not include ownership
verification, claiming, collection, return, Student notifications, or changes
to report submission and history.

## In scope

- Deterministic candidate-pair generation from canonical Item Reports.
- Matching based only on:
  - item category;
  - item-name keywords;
  - location; and
  - occurrence-date range.
- Fixed, documented, testable rule components and any aggregate ranking score.
- Criterion-level reasons that account for every score component.
- A deterministic total order for suggestions, including score ties.
- A Desk Officer-only suggestion list and clear no-suggestion and unavailable
  states.
- Read-only side-by-side comparison of the two canonical reports in a selected
  suggestion.
- Explicit durable linking of two reports as a possible match.
- Visibility of current link state and an explicit durable unlink action.
- Automated matching, ordering, linking, unlinking, persistence, failure, and
  privacy tests at existing plain-Java seams.
- Proportionate manual/source-review evidence for JavaFX behavior under the
  repository's current no-JavaFX-test-framework convention.
- All Sprint 2 documentation and interaction deliverables listed below.

## Explicit non-goals

This mission does not implement or introduce:

- Automatic confirmation of ownership.
- Automatic linking or unlinking.
- A claim, collection, return, handover, or ownership-verification workflow.
- Probabilistic, machine-learning, LLM-based, or opaque matching.
- A fuzzy-AI recommendation system.
- A probability, confidence percentage, or assertion that two reports describe
  the same item.
- Matching on Reporter ID, public description, private identifying detail,
  report status, creation time, or any hidden criterion. Report type and status
  determine eligibility as specified below but do not contribute to score.
- Student-facing suggestions, comparison, links, notifications, or access to
  another Student's report.
- A new matching or ownership `ReportStatus`.
- Changes to `ItemReport`, `ReportType`, `ItemCategory`, `ReportStatus`, Student
  submission, Student history/search, or their validation rules.
- Editing or deleting reports from the matching workspace.
- A general relationship graph, generic workflow engine, or generalized search
  and recommendation platform.
- Live/background matching, polling, pagination, user-defined sorting, or
  unrelated officer-workspace redesign.
- In-place extension of the strict report-store v1 schema without a separately
  approved compatibility and migration decision.
- A new dependency, UI-test framework, build/CI change, release change,
  unrelated refactoring, or unnecessary UI polish.

The existing `publicDescription` is safe to show to an authenticated officer
and may help manual comparison, but it is not one of the four Sprint 2 matching
inputs and therefore does not affect candidate generation, score, or reasons in
this mission.

## Functional requirements

### Matching Engine — S2-D2-01

- **ME-FR-001 — Canonical input:** The engine consumes canonical `ItemReport`
  values loaded through `ReportRepository`; it never parses report JSON or
  creates a duplicate report model.
- **ME-FR-002 — Pair identity:** A candidate is an unordered pair of two
  distinct Report IDs. A report cannot match itself, and A-B and B-A cannot
  appear as separate candidates.
- **ME-FR-003 — Eligibility:** Every candidate contains exactly one LOST and one
  FOUND report. Both SUBMITTED and UNDER_REVIEW reports are eligible. Reporter
  identity must not affect eligibility or score.
- **ME-FR-004 — Bounded criteria:** Only category, item-name keywords, location,
  and occurrence-date range may affect candidate qualification, component
  values, and aggregate rank.
- **ME-FR-005 — Fixed policy:** The approved PRD must fix the keyword rules,
  location rules, date window and boundaries, component weights or precedence,
  and any minimum suggestion threshold. The TDD must implement that observable
  policy without changing it. Matching cannot depend on locale defaults,
  wall-clock time, randomness, input iteration order, or mutable global state.
- **ME-FR-006 — Explainable output:** Each possible-match suggestion identifies
  its two reports and exposes the applicable named rule components, their
  values, an aggregate score when used, and concise officer-readable reasons.
- **ME-FR-007 — Score semantics:** A score is deterministic rule points used for
  qualification and/or ordering. It is not a probability, confidence measure,
  ownership decision, or officer decision. A reason cannot claim evidence that
  its corresponding rule did not produce.
- **ME-FR-008 — Deterministic set and order:** The same canonical report values
  and rule configuration produce the same unique candidate set, components,
  reasons, aggregate scores, and total ordering even when input encounter order
  differs.
- **ME-FR-009 — Stable ties:** Suggestions have a documented deterministic
  total order. The TDD defines the exact final tie-break without relying on
  incidental repository or collection order.
- **ME-FR-010 — Read only:** Generating, refreshing, selecting, viewing, or
  comparing suggestions does not mutate a report, report status, or link.
- **ME-FR-011 — Safe failure:** Failure to load authoritative reports is an
  unavailable/error outcome, never an empty or no-suggestion result. User-facing
  diagnostics do not expose storage paths, JSON, unsafe report values, or
  private identifying details.
- **ME-FR-012 — Possible-match language:** Every engine result is described as a
  possible match. No output says or implies matched, owned, verified, claimed,
  or confirmed.

### Officer Workspace — S2-D2-02

- **OW-FR-001 — Authorized entry:** Only the authenticated `DESK_OFFICER` route
  mounts and loads the workspace. A Student route does not construct or expose
  it.
- **OW-FR-002 — Suggestions:** The workspace displays possible-match
  suggestions in the engine's deterministic order and visibly distinguishes
  unlinked suggestions from officer-created possible-match links.
- **OW-FR-003 — Empty versus unavailable:** No eligible suggestions and a load
  or evaluation failure are distinct states. A failure offers a safe, explicit
  retry path where appropriate rather than masquerading as an empty result.
- **OW-FR-004 — Side-by-side comparison:** Selecting a suggestion displays the
  two reports simultaneously in a read-only comparison. The PRD determines the
  displayed canonical fields; the comparison preserves public/private
  separation and clearly labels the two report types.
- **OW-FR-005 — Reasons:** The selected comparison displays the criterion-level
  matching reasons and score components that produced the suggestion. No hidden
  factor may be presented or used.
- **OW-FR-006 — Restricted fields:** Reporter ID and private identifying detail,
  if displayed, appear only in the authenticated selected officer comparison.
  Private detail is clearly separated as officer-only verification information.
  Neither field appears in suggestion rows, matching reasons, empty/error
  states, documentation screenshots, logs, or handoff summaries.
- **OW-FR-007 — Explicit actions:** Viewing a suggestion has no side effect.
  Link and Unlink are separate, explicit officer actions with state-appropriate
  enablement. Neither action confirms ownership.
- **OW-FR-008 — Fresh state:** Logout removes suggestion, comparison, reason,
  and private-detail presentation state. A later officer login reloads current
  reports and durable link state rather than reusing a stale view.

### Link / Unlink behavior — S2-D2-02

- **LU-FR-001 — Relationship meaning:** A link records only that a Desk Officer
  considers two distinct reports a possible match. It is a symmetric
  relationship keyed by the two canonical Report IDs.
- **LU-FR-002 — Separate state:** Link state does not change either
  `ItemReport`, report status, Reporter ID, or any other canonical report value.
  It must not be encoded into descriptions or another existing field.
- **LU-FR-003 — Durable link:** Link success is shown only after the relationship
  has been safely persisted. A fresh persistence instance and a later
  application session must observe it.
- **LU-FR-004 — No duplicate relation:** The same unordered pair cannot produce
  duplicate link records, including when the Report IDs are supplied in reverse
  order. Exact repeated-action behavior and feedback are fixed in the PRD.
- **LU-FR-005 — Durable unlink:** Unlink removes only the selected possible-match
  relationship. Success is shown only after durable removal, and a fresh
  persistence instance and later session must observe its absence.
- **LU-FR-006 — Current reports:** Before creating a link, the application
  rechecks that both reports still exist and remain an eligible LOST-to-FOUND
  pair in a current supported status. A stale or ineligible pair is reconciled
  safely and is not reported as linked.
- **LU-FR-007 — Failure truthfulness:** A failed link or unlink retains the last
  known truthful visible state, reports no success, changes no report, and
  exposes only privacy-safe retry/failure information.
- **LU-FR-008 — Link availability:** Linked pairs remain available to an
  authenticated Desk Officer in a linked-possible-matches section so they can
  be reviewed and unlinked.
- **LU-FR-009 — Non-exclusive links:** One report may participate in multiple
  simultaneous possible-match links. Linking one pair must not remove or reject
  a different possible-match relationship involving either report.

## Matching constraints and invariants

- A possible-match suggestion and an officer-created possible-match link are
  different facts. Engine evidence does not create a link; a link does not
  rewrite the engine's evidence.
- Determinism covers the candidate set, rule components, reason text, aggregate
  score, and final order, not merely the numeric total.
- The engine is independent of repository encounter order and applies the
  deterministic total tie-break specified in the TDD.
- Every aggregate score is fully accounted for by visible, documented
  components. There is no hidden bonus, penalty, or rule.
- Score and reason copy uses only permitted matching fields. It does not expose
  Reporter ID, private identifying detail, or other private report-identifying
  data.
- Date comparison is based on stored `LocalDate` values and the approved
  inclusive boundary. It does not use the current clock.
- Text matching uses an explicitly documented, locale-stable normalization and
  keyword/location rule. Valid restored text may preserve surrounding
  whitespace, so matching cannot assume stored values are pre-normalized.
- Suggestion, comparison, and link operations never transition report status.
- No result, score, reason, link, button label, success message, or guide text
  may imply ownership confirmation.

## Score and reason semantics

The approved matching policy must define a small named component for each of
the four Sprint 2 criteria. For every component it must state:

- the exact input field or fields;
- normalization and comparison rules;
- valid, invalid, and boundary examples;
- whether the component is an eligibility gate, contributes points, or both;
- the points or deterministic precedence it contributes; and
- the exact information from that component that may appear in an officer
  reason.

If a numeric total is used, it is the exact sum of the documented component
points and uses a documented range. If no numeric total is ultimately used,
the deterministic component precedence and ordering rules must still be
explained. In either case, the interface must explain **why suggested**, not
just present an unexplained number.

The sprint specification fixes the four permitted criteria but does not supply
their observable rule values. The PRD must therefore decide and obtain approval
for the exact keyword rules, location rules, date window, weights or precedence,
and threshold. These values are not prerequisites for Mission Brief approval;
the later TDD must implement the approved PRD policy without changing it.

## Link persistence constraints

The current repository has no persistence contract for relationships, and the
strict version-one report JSON accepts only canonical report data. This mission
therefore requires durable possible-match link/unlink behavior without changing
or duplicating `ItemReport` or silently extending report-store v1.

The TDD will define the persistence design. This Mission Brief constrains only
the observable outcome: a successful link or unlink survives a fresh
persistence instance and later application session; a failed operation reports
no success, exposes no private or technical data, and does not change either
canonical report.

## Authorization and privacy boundaries

- The existing stored role and `DESK_OFFICER` route remain the authorization
  boundary. Matching is mounted lazily inside that authenticated destination.
- Logout removes the complete matching UI subtree and any selected private
  details. Durable links remain persisted because logout is not unlink.
- Candidate generation uses only the approved four non-private criteria.
- Reporter ID and private identifying detail, if displayed, are restricted to
  the authenticated selected officer comparison and never appear in suggestion
  rows or matching reasons.
- Private identifying detail follows the existing officer-detail boundary and
  remains visually separate from public descriptions.
- Student history remains restricted to the exact authenticated Reporter ID and
  receives no link or matching metadata.
- Tests, screenshots, examples, and manuals use only synthetic users and
  reports. No real student, staff, school, credential, or report data is used.
- Errors, logs, documentation screenshots, test output, and handoff summaries
  contain no private report values, unsafe report contents, paths, JSON, salts,
  hashes, passwords, or other credential material.

## Dependencies and existing contracts

### Developer 1 and shared contracts

- `io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport` supplies the
  canonical immutable data and accessors used for matching and comparison.
- `ReportType`, `ItemCategory`, and `ReportStatus` supply the current canonical
  enums and display names.
- Student submission supplies the persisted reports; S2-D1-01 Student history
  and search already share the report repository and remain unchanged.
- Developer 1 owns those report types, Student workflows, Gradle/build/release
  configuration, and the initial JavaFX shell. This mission neither requires
  nor authorizes changing them.

### Developer 2 contracts

- `ReportRepository.loadAll()` is the source of the canonical report snapshot.
  Matching does not read `data/reports.json` directly.
- `JsonReportRepository` continues to own report persistence only. Its stable
  order is inherited storage behavior, not the suggestion tie-break.
- `AuthenticationCoordinator`, `AuthenticationPane`, and the existing lazy
  Desk Officer destination preserve role routing and logout behavior.
- `DeskOfficerReviewService`, `ReviewQueueState`, and
  `DeskOfficerReviewPane` establish current officer service/state/view and
  privacy patterns. The matching workflow may compose with them but must not
  silently redesign the delivered review queue.
- The current review work on `royden/feat-officer-review-queue` must be merged
  or otherwise present in the verified S2 integration baseline before
  implementation begins.

### Missing contract required by this mission

No current class or interface stores possible-match relationships. The TDD must
design the smallest Developer 2-owned persistence capability that satisfies the
durable link/unlink requirements without inventing a shadow report model or
changing a Developer 1-owned type.

### Cross-owner integration

The current `AuthenticationPane` accepts one opaque Desk Officer node, and
`FindersKeepersApp` currently supplies `DeskOfficerReviewPane`. Integrating one
cohesive officer workspace and its link persistence will likely require a small
composition change in Developer 1's shell-owned
`FindersKeepersApp`.

Before that implementation change:

1. identify the exact file and verified baseline;
2. explain why composition requires the change;
3. propose the smallest sufficient edit;
4. state whether Developer 1 can make it instead; and
5. obtain explicit approval.

No second application entry point, duplicate report type, or parallel officer
login flow may be created to avoid this dependency.

## Conceptual workflow

```text
Authenticated DESK_OFFICER route
        ↓
Load canonical reports through ReportRepository
        ├── failure → privacy-safe unavailable state / explicit retry
        ↓
Keep exactly LOST-to-FOUND pairs in SUBMITTED or UNDER_REVIEW
        ↓
Generate each unique unordered candidate pair once
        ↓
Evaluate category + item-name keywords + location + date range
        ↓
Create visible components, reasons, and aggregate rank
        ↓
Sort by approved rank and the deterministic TDD tie-break
        ↓
Show possible-match suggestions
        ├── none → true no-suggestions state
        └── select → read-only side-by-side comparison + reasons
                           ↓
                    explicit Link action
                           ↓
                    authoritative report recheck
                           ↓
                    durably store possible-match relationship
                           ↓
                    visibly linked and available for Unlink
                           ↓
                    explicit Unlink action
                           ↓
                    durably remove only that relationship
```

Changing selection, refreshing suggestions, comparing reports, and viewing a
link never changes a report or a link. Only explicit successful Link and Unlink
actions mutate relationship state.

## Acceptance criteria

### S2-D2-01 Matching Engine acceptance criteria

- **ME-AC-001:** Given a fixed canonical report snapshot and fixed approved
  rules, repeated runs and different input iteration orders produce identical
  candidate pairs, component values, reason text, aggregate scores, and order.
- **ME-AC-002:** The engine emits no self-pair and no reversed duplicate pair.
- **ME-AC-003:** Every candidate contains exactly one LOST and one FOUND report;
  both SUBMITTED and UNDER_REVIEW reports participate.
- **ME-AC-004:** Category, item-name keywords, location, and occurrence-date
  range are each covered independently, in combination, and at their approved
  boundaries.
- **ME-AC-005:** Changing Reporter ID, public description, private identifying
  detail, created-at time, or another non-eligibility/non-matching field cannot
  change qualification, score, reasons, or order.
- **ME-AC-006:** Every displayed score is exactly accounted for by its visible
  components, and every reason corresponds to the rule outcome that produced
  it.
- **ME-AC-007:** Equal-ranked suggestions resolve through the documented total
  tie-break and never through incidental repository or collection order.
- **ME-AC-008:** Empty eligible input or no qualifying pair produces a true
  no-suggestions result, distinct from report-load failure.
- **ME-AC-009:** Matching performs no persistence mutation and changes no
  canonical report value or status.
- **ME-AC-010:** Engine types, docs, and presentation call results only possible
  matches and never describe a score as probability, confidence, or ownership
  proof.
- **ME-AC-011:** Matching rules, rationale, score/reason semantics, sequence,
  boundaries, and deterministic tie behavior are documented and agree with the
  automated evidence.

### S2-D2-02 Officer Workspace acceptance criteria

- **OW-AC-001:** An authenticated Desk Officer can enter the workspace; a
  Student cannot see or load it; logout clears its visible and selected state.
- **OW-AC-002:** Suggestions appear in deterministic engine order with their
  possible-match label and current linked/unlinked state.
- **OW-AC-003:** Selecting a suggestion shows the PRD-approved canonical fields
  for both reports side by side, read only, with types, public/private
  separation, and the exact matching reasons visible.
- **OW-AC-004:** Viewing, selecting, comparing, refreshing, and retrying perform
  no report, status, or link mutation.
- **OW-AC-005:** An explicit Link action creates at most one symmetric
  relationship for the pair, reports success only after durable persistence,
  and is visible through a fresh persistence instance and later login.
- **OW-AC-006:** An explicit Unlink action removes only the selected
  relationship, reports success only after durable persistence, and remains
  absent through a fresh persistence instance and later login.
- **OW-AC-007:** Linking or unlinking changes no field of either canonical
  report and never confirms ownership or performs a status transition.
- **OW-AC-008:** Reverse-duplicate, repeated, self, stale, ineligible, missing,
  and persistence-failure cases follow the approved no-op/error semantics,
  report no false success, and preserve truthful durable state.
- **OW-AC-009:** No-suggestion, no-link, and persistence-unavailable states are
  visibly distinct and privacy safe, with explicit retry where applicable.
- **OW-AC-010:** Reporter ID and private identifying detail, if displayed,
  appear only in the authenticated selected officer comparison. Neither appears
  in candidate rows, reasons, failures, logs, screenshots, or documentation.
- **OW-AC-011:** Possible-match links are non-exclusive. Linked pairs remain
  reachable in the linked-possible-matches section for review and unlinking.
- **OW-AC-012:** Officer instructions, the workflow diagram, interaction
  summary, linking/unlinking tests, and privacy-safe synthetic evidence agree
  with delivered behavior.

## Testing expectations

The later requirements-to-tests plan must map each acceptance criterion to
automated, manual, source-review, or command evidence. It must cover behavior,
not implementation choreography, and must not design a test-only production
API.

### Automated matching evidence

- LOST-to-FOUND eligibility across both current statuses and every approved rule
  independently.
- Keyword, location, and date normalization/boundary cases fixed by the
  approved PRD.
- Combined components, threshold edges, component-to-total accounting, and
  reason consistency.
- Same inputs repeated and shuffled, proving identical pairs, scores, reasons,
  and order.
- Score ties resolved by the documented deterministic TDD tie-break.
- No self or reverse-duplicate candidates.
- Excluded fields unable to affect matching.
- Empty input, no candidate, and safe report-load failure.
- Immutable/read-only result behavior where exposed by the chosen interface.

### Automated link/unlink evidence

- Valid link and fresh-instance reconstruction.
- Valid unlink and fresh-instance reconstruction of absence.
- Symmetric pair identity and reverse-duplicate prevention.
- Self-link rejection.
- Repeated link and repeated unlink semantics approved in the PRD.
- Multiple links involving the same report and linked-section availability for
  later unlinking.
- Authoritative recheck of missing or newly ineligible reports.
- Safe link and unlink failures with no optimistic success or partial mutation.
- Preservation of all eleven values and statuses of both reports.
- Persistence failures leave truthful link state and report no false success.
- Only synthetic reports and isolated temporary directories; no test reads or
  writes project-local or real user data.

### Authorization, UI, and regression evidence

- Existing authentication tests plus structural/manual evidence that matching
  loads only for `DESK_OFFICER`, Student routing does not expose it, logout
  removes it, and later login creates fresh view state.
- Manual/source-review evidence for deterministic rows, true empty versus error,
  side-by-side read-only comparison, visible reasons, public/private separation,
  state-appropriate Link/Unlink actions, retry, long valid text, and logout.
- Existing canonical-domain, report-persistence, authentication, Student, and
  officer-review suites remain green.
- No JavaFX test dependency is added merely for this mission; rendered behavior
  may use recorded manual evidence unless a separate dependency change is
  approved.
- Focused tests, the repository `check`, and, because startup and demonstration
  behavior will change, the required release/smoke verification are planned for
  implementation handoff, not run during Mission Brief preparation.

## Documentation deliverables

The complete feature must provide:

- An updated Developer Guide explanation of the matching algorithm.
- A Developer Guide rationale for deterministic rather than probabilistic,
  ML-based, or LLM-based matching.
- A Developer Guide explanation of component scores, aggregate score or
  precedence, reasons, thresholds, date boundaries, and final tie-breaking.
- A Developer Guide matching sequence aligned with the implemented service and
  persistence flow.
- Desk Officer matching instructions, placed in the User Guide when they are
  user-facing and cross-referenced from the Developer Guide where useful.
- A matching workflow diagram covering reports, deterministic suggestions,
  comparison, durable link, and durable unlink.
- Matching unit tests and their documented scope.
- Linking and unlinking tests, including fresh-instance durability.
- An interaction summary covering the suggestion list, comparison, Link,
  Unlink, empty, failure/retry, privacy, and logout behavior.
- Privacy-safe synthetic screenshots or other manual evidence only if required
  by the approved PRD; no private detail, signed-in identity, credentials,
  unsafe report text, path, JSON, or technical failure detail may appear.
- A final handoff that identifies changed files, checks and results, data-safety
  evidence, limitations, unresolved integration, and unverified requirements.

Documentation must replace the Developer Guide's statement that matching is
future work only after the feature is actually delivered and verified.

## Risks and controls

- **Unspecified matching policy:** Different reasonable rules produce different
  suggestions. Control: approve an exact observable rule table with examples in
  the PRD before TDD approval.
- **False certainty:** Scores or links may be mistaken for ownership proof.
  Control: use possible-match language everywhere and define scores as rule
  points only.
- **Unstable ordering:** Repository or hash iteration could leak into ties.
  Control: require input-order independence and a documented deterministic
  total tie-break, with its exact design fixed in the TDD.
- **Link durability gap:** No current persistence contract stores
  relationships. Control: plan a bounded Developer 2-owned association
  boundary; do not modify `ItemReport` or report-store v1 implicitly.
- **Privacy expansion:** Pair comparison could spread private detail into lists,
  reasons, screenshots, or logs. Control: keep it selected-officer-only and use
  public matching fields everywhere else.
- **Cross-owner drift:** Startup composition or shared report changes could
  expand scope. Control: use the existing opaque officer destination and request
  separate approval for the smallest necessary shell edit.
- **Stale baseline:** S1-D2-03 is not yet on the inspected `origin/main`.
  Control: verify the implementation baseline contains the approved review
  workflow before branch creation or TDD finalization.

## Definition of done

This combined mission is finished and merge-ready only when:

- The accepted LOST-to-FOUND eligibility and non-exclusive link lifecycle are
  recorded in the decision ledger, and the PRD contains an approved matching
  rule table for the four permitted criteria.
- The repository owner has approved this Mission Brief, one combined PRD, one
  combined TDD, and one requirements-to-tests plan with consistent metadata.
- Separate implementation and any cross-owner integration authorization have
  been granted.
- The matching engine satisfies every S2-D2-01 acceptance criterion with the
  approved deterministic rule table, visible reasons, and total ordering.
- The officer workspace satisfies every S2-D2-02 acceptance criterion,
  including side-by-side comparison and truthful officer-only behavior.
- Link and unlink are durable across fresh persistence instances and later
  application sessions and never mutate a canonical report or imply ownership.
- Matching and relationship failures are distinct from empty states and expose
  no private or technical data.
- All planned automated and manual evidence passes using only synthetic data
  and isolated storage.
- Existing domain, persistence, authentication, Student, and officer-review
  behavior remains green.
- Required Developer Guide, User Guide/officer instructions, workflow diagram,
  test documentation, and interaction summary are complete and accurate.
- `gradlew.bat check` or `./gradlew check` passes; required release and packaged
  smoke verification pass because startup/demo behavior changes.
- The final diff remains inside the approved feature and ownership boundary;
  no unrelated `evals/`, local report store, private data, or generated build
  output is included.
- Remaining limitations and every unverified requirement are reported
  truthfully in the handoff.

## Planning and authorization gates

Approval of this Mission Brief does not authorize PRD/TDD work, branch
creation, tests, production code, documentation delivery, screenshots,
dependency changes, commits, pushes, pull requests, release artifacts, or
merging.

The intended sequence is:

1. Repository owner approves the Mission Brief and approval metadata is
   recorded.
2. Create `docs/features/S2-D2-01-02/GrillingDecisions.md`, recording the
   accepted Mission Brief decisions and assigning the exact four-criterion rule
   table to the PRD.
3. Draft one combined PRD, decide the exact observable matching policy, and
   obtain repository-owner approval.
4. Resolve and approve the technical design and requirements-to-tests plan,
   including the public matching and relationship seams.
5. Separately authorize the exact implementation and cross-owner composition
   scope.
6. Implement and verify the single vertical feature.

## Open Decisions

No Mission-Brief-level product decision remains unresolved.

The exact keyword rules, location rules, date window, weights or precedence,
and suggestion threshold are intentionally assigned to the combined PRD. The
exact deterministic tie-break and persistence design are intentionally assigned
to the TDD.
