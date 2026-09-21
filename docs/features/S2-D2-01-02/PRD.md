# S2-D2-01-02 Officer Possible-Match Workflow PRD

- Status: Approved
- Draft date: 2026-09-21
- Post-draft grilling completed: 2026-09-21
- Revision date: 2026-09-21
- Approver: Repository owner
- Approval date: 2026-09-21
- TDD authorization: Not granted
- Implementation authorization: Not granted
- Branch creation authorization: Not granted
- Feature: Officer Possible-Match Workflow
- Workstream: Developer 2
- Source sprint items: S2-D2-01 Deterministic Matching Engine and S2-D2-02 Officer Matching Workspace
- Approved mission brief: `docs/mission-briefs/S2-D2-01-02-officer-possible-match-workflow.md`
- Completed decision ledger: `docs/features/S2-D2-01-02/GrillingDecisions.md`
- Planning baseline: `royden/feat-officer-review-queue` at `24d7e9dc08d7646ade140d54cbbacebd86147bdc`

## Authority and precedence

This PRD is governed, in descending order, by:

1. `AGENTS.md`.
2. The approved S2-D2-01-02 Mission Brief.
3. The current canonical report, persistence, authentication, and Desk Officer
   review contracts at the planning baseline.
4. The completed post-draft S2-D2-01-02 decision ledger.
5. The repository owner's 2026-09-21 request to prepare this combined PRD.
6. This PRD.

The Mission Brief is the scope boundary. This PRD fixes observable product
rules that the Mission Brief deliberately assigned to the PRD. It does not
authorize a TDD, requirements-to-tests artifact, branch, tests, production
code, commits, pushes, a pull request, or changes in Developer 1-owned areas.

### Artifact chronology

The initial PRD draft was written before the required product grilling. The
repository owner subsequently completed `/grill-me`, accepted every recorded
recommendation, confirmed shared understanding, and authorized reconciliation
of this then-unapproved draft. This revision incorporates that completed
decision ledger without claiming that grilling preceded the initial draft.

## Decision provenance

This document uses the following labels to keep facts and decisions distinct:

| Label | Meaning in this PRD |
| --- | --- |
| **Repository fact** | Behavior or a contract confirmed in the planning baseline. |
| **Mission requirement** | Approved scope or behavior inherited unchanged from the Mission Brief. |
| **PRD decision** | Observable product behavior confirmed in the post-draft decision ledger and fixed here because the Mission Brief assigned it to this PRD. |
| **TDD deferral** | An implementation decision that must not change observable behavior fixed here. |
| **Unresolved product decision** | A product choice still requiring owner input. There are none in this approved PRD. |

## Product goal

The product assists an authenticated Desk Officer in identifying reports that
may describe the same physical item through one complete workflow:

```text
Canonical LOST and FOUND reports
        -> deterministic possible-match suggestions
        -> officer selection and read-only comparison
        -> explicit Link as Possible Match
        -> linked-possible-matches review
        -> explicit Unlink Possible Match
```

The workflow supplies reproducible evidence and a durable officer-created
relationship. It does not verify ownership. Ownership verification, collection,
return, claim, and handover remain human workflows outside this feature.

## Actors and authorization

### Authenticated Desk Officer

An authenticated `DESK_OFFICER` may load the matching workspace, view
privacy-safe suggestions, select one pair, compare the two canonical reports,
inspect matching reasons, link the pair as a possible match, inspect linked
pairs, and unlink a relationship.

### Student

A Student must not load or see the matching workspace, suggestions, rule
scores, matching reasons, other Students' reports, possible-match links, or
another report's Reporter ID or private identifying detail. Student submission,
history, and search behavior remain unchanged.

### Logout and later re-entry

Logout removes the complete matching view and clears every visible suggestion,
selection, score, reason, Reporter ID, and private identifying detail. Logout
does not unlink durable relationships. A later authenticated Desk Officer
session performs a new authoritative load of reports and link state; it does
not restore a cached screen.

## Success criteria

The feature succeeds when:

1. Only authenticated Desk Officers can use it.
2. The same report snapshot and approved rules always produce the same unique
   suggestions, components, reasons, scores, and order.
3. Every suggestion contains exactly one eligible LOST report and one eligible
   FOUND report and is based only on category, item-name keywords, location,
   and occurrence date.
4. Every rule point is visible and explainable, with no hidden criterion.
5. Officers can compare both reports read only, explicitly link and unlink a
   possible-match relationship, and see linked pairs separately.
6. Links are non-exclusive and persist across later application sessions.
7. Empty states, unavailable states, stale actions, and failed mutations remain
   truthful and privacy safe.
8. No matching, viewing, linking, or unlinking operation changes an
   `ItemReport` or `ReportStatus`, or claims ownership has been confirmed.

## Confirmed repository facts and inherited contracts

- **Repository fact:** `ItemReport` is the single immutable report model. It
  contains Report ID, Reporter ID, report type, item name, category, location,
  occurrence date, public description, private identifying detail, status, and
  creation time.
- **Repository fact:** `ReportType` contains only `LOST` and `FOUND`.
- **Repository fact:** `ReportStatus` contains only `SUBMITTED` and
  `UNDER_REVIEW`.
- **Repository fact:** `ItemCategory` is the canonical closed category set;
  matching must compare those enum values rather than invent category aliases.
- **Repository fact:** `ReportRepository.loadAll()` supplies the canonical
  report snapshot. The matching workflow must not parse report JSON directly.
- **Repository fact:** the strict version-one report store has no relationship
  field or possible-match API.
- **Repository fact:** the authenticated Desk Officer destination is mounted
  lazily and removed on logout.
- **Repository fact:** the existing review queue keeps Reporter ID and private
  identifying detail out of public rows and shows them only in a selected
  authenticated officer details view.
- **Mission requirement:** both `SUBMITTED` and `UNDER_REVIEW` reports are
  eligible for matching, regardless of the submitted-only review-queue rule.
- **Mission requirement:** matching and possible-match relationships are
  separate from canonical report state.

## Glossary

- **Eligible report:** a canonical report whose type is LOST or FOUND and whose
  status is SUBMITTED or UNDER_REVIEW.
- **Candidate pair:** two distinct eligible reports containing exactly one LOST
  report and one FOUND report. The pair is unordered for identity.
- **Qualifying pair:** a candidate pair that passes both mandatory matching
  gates and reaches the score threshold.
- **Possible-match suggestion:** a qualifying pair not currently linked, shown
  to a Desk Officer for inspection.
- **Rule score:** deterministic integer points produced only by the four rules
  in this PRD. It is not a probability, confidence, likelihood, certainty, or
  ownership assessment.
- **Possible-match link:** a durable, symmetric, officer-created relationship
  stating only that the two reports are considered a possible match.
- **Linked possible match:** a persisted possible-match link presented in the
  separate linked section so it can be reviewed or unlinked.
- **Authoritative refresh:** an explicit reload of current report and link
  state. This feature has no polling or background refresh.

## Included scope

- Deterministic LOST-to-FOUND candidate generation.
- Exact four-criterion matching policy and rule score.
- Deterministic qualification and product-level ordering.
- Desk Officer-only suggestion, comparison, link, linked-list, and unlink flow.
- Non-exclusive durable possible-match links.
- Truthful empty, unavailable, stale, failure, retry, logout, and re-entry
  behavior.
- Documentation implications and acceptance traceability for both sprint
  items.

## Matching product policy

### Eligibility before matching

A candidate pair must satisfy all of the following before the four matching
rules are applied:

1. The Report IDs are different.
2. Exactly one report is `LOST` and exactly one is `FOUND`.
3. Each report is either `SUBMITTED` or `UNDER_REVIEW`.

Report order, Reporter ID, public description, private identifying detail,
creation time, and repository encounter order do not affect eligibility or
score. A-B and B-A are one candidate, never two.

### Compact rule table

| Criterion | Source field(s) | Comparison | Role | Points | Officer-readable reason |
| --- | --- | --- | --- | ---: | --- |
| Category | `category` on both reports | Exact canonical `ItemCategory` equality | Mandatory gate and contributor | 40 | Same canonical category |
| Item-name keywords | `itemName` on both reports | At least one shared normalized unique token | Contributor | 20 | Shared item-name keyword(s) |
| Location | `location` on both reports | Exact equality after case and separator normalization | Contributor | 30 | Same normalized location |
| Occurrence date | LOST and FOUND `occurrenceDate` | FOUND date is 0 to 7 days after LOST date, inclusive | Mandatory gate and contributor | 10 | Dates are in the allowed directional window |

The possible score range is 0 to 100. A suggestion requires:

- the Category gate to pass;
- the Occurrence Date gate to pass; and
- a total score of at least 70.

Therefore a pair that passes both gates must also match either item-name
keywords or location. There is no bonus, penalty, fallback, fuzzy comparison,
or hidden rule.

### Category rule

**PRD decision:** category is both a hard gate and a 40-point contributor.
The two canonical `ItemCategory` values must be identical. Display names,
free-text synonyms, and category hierarchy do not participate.

- Positive: `WATER_BOTTLES` and `WATER_BOTTLES` pass and add 40 points.
- Negative: `BAGS` and `PERSONAL_ITEMS` fail, even if names, locations, and
  dates otherwise match.
- Boundary: `OTHER` matches only `OTHER`; it is not a wildcard.
- Reason: the officer may see the canonical display category, but no report
  identity or private field.

### Item-name keyword rule

**PRD decision:** keyword overlap is an all-or-nothing 20-point component, not
a count or proportion.

Each `itemName` is normalized independently as follows:

1. Convert case using locale-independent lowercase behavior equivalent to
   `Locale.ROOT`.
2. Treat every maximal run of characters that are not Unicode letters or
   digits as one separator. This covers whitespace and punctuation.
3. Split at separators.
4. Discard empty tokens and tokens shorter than two Unicode code points.
5. Remove duplicate tokens within the same item name.
6. Apply no stop-word removal, stemming, singular/plural conversion, accent
   folding, Unicode canonical or compatibility normalization, spelling
   correction, edit distance, substring matching, semantic expansion, or fuzzy
   matching.

Keyword overlap means the two resulting token sets have at least one exact
token in common. One or many shared tokens contribute the same 20 points.
The selected comparison shows shared normalized tokens in deterministic
ascending lexical order. If either side has no valid token, the component
contributes zero points and has no positive reason.

- Positive: `Blue Pencil-Case` and `pencil case` share `pencil` and `case` and
  add 20 points.
- Positive with duplicates: `pen pen case` and `pen pouch` share the one unique
  token `pen`; duplicates do not add points beyond the 20-point component.
- Negative: `notebook` and `book` do not overlap because substring matches do
  not count.
- Negative: `bottle` and `bottles` do not overlap because there is no stemming.
- Boundary: `A pen` yields only `pen`; the one-character token `a` is ignored.
- Boundary: names made only of one-character tokens have no valid keywords and
  contribute zero points.

### Location rule

**PRD decision:** location is a 30-point contributor and is not a gate.

Normalize each `location` by:

1. converting case using locale-independent lowercase behavior equivalent to
   `Locale.ROOT`;
2. treating every maximal run of characters that are not Unicode letters or
   digits as a separator; and
3. removing leading/trailing separators and replacing each internal separator
   run with one ordinary space.

The complete normalized strings must then be equal. Whitespace and punctuation
therefore act as separators rather than significant characters. No accent
folding or Unicode canonical/compatibility normalization is applied. Partial,
substring, alias, building-map, geospatial, and fuzzy matches are not allowed.

- Positive: `  School   Library ` and `school library` add 30 points.
- Positive: `Hall A` and `hall a` add 30 points.
- Positive punctuation boundary: `Library, Level 2` and
  `Library - Level 2` both normalize to `library level 2` and add 30 points.
- Negative: `Library` and `Library entrance` do not match.
- Negative: `Block A` and `Block B` do not match.
- Boundary: internal tabs, repeated spaces, and punctuation runs each normalize
  to one space.
- Reason: the officer sees that the normalized locations are the same; the
  reason need not repeat untrusted free text.

### Occurrence-date rule

**PRD decision:** occurrence date is a directional hard gate and a 10-point
contributor.

The LOST report supplies the loss date and the FOUND report supplies the found
date. The pair passes when the FOUND date is the same as or later than the LOST
date and no more than seven calendar days later. Both day 0 and day 7 are
included. The current date, creation time, time zone, and time of day do not
participate.

- Positive: LOST 2026-09-01 and FOUND 2026-09-01 pass at day 0 and add 10
  points.
- Positive boundary: LOST 2026-09-01 and FOUND 2026-09-08 pass at day 7 and add
  10 points.
- Negative boundary: FOUND 2026-09-09 is day 8 and fails the gate.
- Negative direction: FOUND 2026-08-31 is before the loss date and fails the
  gate.
- Reason: the officer sees the non-negative day difference and that it is
  within the inclusive seven-day window.

## Aggregate score and qualification

The score is the exact sum of the four components: 40 + 20 + 30 + 10, as
applicable. All four component outcomes are available in the selected
comparison; every positive component has a corresponding reason. Failed
components do not receive points and must not receive a positive reason.

A numeric total alone never explains a suggestion. The UI must label it as a
rule score or matching-rule points and must state that it is not confidence or
ownership proof. Scores are displayed only in a possible-match context.

Representative outcomes:

| Outcome | Category | Keywords | Location | Date | Total | Qualification |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Strong possible match | 40 | 20 | 30 | 10 | 100 | Suggested |
| Borderline keyword-led possible match | 40 | 20 | 0 | 10 | 70 | Suggested at threshold |
| Location-led possible match | 40 | 0 | 30 | 10 | 80 | Suggested |
| Same category and date but otherwise weak | 40 | 0 | 0 | 10 | 50 | Not suggested |
| Good keyword/location evidence outside date range | 40 | 20 | 30 | 0 | 90 | Not suggested; mandatory date gate failed |
| Different category with otherwise strong evidence | 0 | 20 | 30 | 10 | 60 | Not suggested; mandatory category gate failed |

## Product-level ordering

Unlinked suggestions appear from higher rule score to lower rule score. Ties
must resolve in a deterministic total order that is independent of report or
collection encounter order. The exact final tie-break is a TDD decision and
must not change qualification, component values, reasons, or scores.

Linked pairs with a current qualifying score follow the same score-first
expectation. A linked pair that is no longer qualifying or has an unavailable
report remains reachable for unlinking and appears after currently scored
linked pairs. Ties within the linked section are deterministic; their exact
technical tie-break is also deferred to the TDD.

A linked pair that no longer qualifies shows its failed gate and component
outcomes without an aggregate total that could be mistaken for a qualifying
score.

## Possible-match suggestion list

### Initial load

Entering the authenticated workspace automatically loads the current canonical
reports and current link state, evaluates suggestions, and displays unlinked
qualifying pairs. The system must know link state before labelling a pair
unlinked.

### Suggestion rows

Each row must label the LOST and FOUND sides and show both item names, the
shared category, each occurrence date and location, the rule score, brief
positive reason labels, and possible-match terminology. Exact shared normalized
keyword values and complete component detail remain in the selected comparison.

Ordinary suggestion rows must not contain Report ID, Reporter ID, public
description, private identifying detail, status, creation time, or
ownership-confirming language.

The unlinked suggestion list contains only qualifying pairs that are not
currently linked. A linked pair is shown in the linked-possible-matches section,
not duplicated as an actionable unlinked suggestion.

### Selection

Selecting a suggestion opens its current read-only comparison and reasons.
Selection alone does not link, unlink, mutate, or change status.

### Refresh

An explicit Refresh reloads authoritative reports and link state and recomputes
the two lists. It is not automatic polling. A successful refresh preserves a
selection only if the same pair still exists in the same applicable section;
otherwise it clears the comparison. A failed refresh clears private comparison
content, disables mutating actions, and presents an unavailable state with an
explicit Retry. Old rows must not be presented as current authoritative state.

### Empty and unavailable states

- If there is not at least one eligible LOST and one eligible FOUND report,
  the workspace states that there are no eligible LOST-to-FOUND pairs.
- If eligible candidate pairs exist but none qualify under the approved rules,
  the workspace states that there are no qualifying possible matches.
- Loading or evaluation failure is an unavailable state, never either empty
  state, and offers explicit Retry.

## Officer side-by-side comparison

Selecting an available suggestion or linked pair displays LOST and FOUND in
clearly separate labelled columns or groups. The comparison is read only and
shows, for both reports:

1. Report ID.
2. Reporter ID.
3. LOST or FOUND type.
4. Item name.
5. Category.
6. Location.
7. Occurrence date.
8. Public description.
9. Private identifying detail.
10. Status.
11. Creation time.

Reporter ID and private identifying detail are officer-only information. They
appear only after an authenticated Desk Officer selects a pair. Private detail
must be visibly separated from public description and labelled for officer
verification. It must not appear in rows, reasons, errors, logs, screenshots,
Student views, or handoff summaries.

The comparison also shows:

- the current rule score, when the pair currently qualifies;
- all four component outcomes and their points;
- the exact shared normalized keyword values, when the keyword component
  passes;
- positive officer-readable reasons; and
- whether the pair is unlinked, linked, no longer qualifying, or partly
  unavailable.

Opening or viewing the comparison changes nothing.

## Matching reasons

Reasons explain rule outcomes rather than make conclusions. They map one to
one to the four criteria:

- same canonical category, +40;
- shared normalized item-name keyword or keywords, +20;
- same normalized location, +30; and
- FOUND date is 0 to 7 days after LOST date, +10.

The comparison may also show failed components as neutral facts, such as no
shared keyword or date outside the window, but failed components receive no
positive wording or points. Reasons never use Reporter ID, public description,
private identifying detail, creation time, or a hidden factor. Wording may be
refined in the UI or guide only if meaning, points, privacy, and possible-match
terminology remain unchanged.

## Link as Possible Match

A successful **Link as Possible Match** records only this statement:

> A Desk Officer considers these two reports a possible match.

It does not confirm ownership, claim an item, close a case, complete a handover,
change report status, edit either report, or prevent other relationships.

### Valid link

The action is available only for a selected unlinked suggestion. Before
reporting success, the workflow rechecks authoritative state: both reports must
still exist, remain distinct, form one LOST-to-FOUND pair, have a supported
status, pass both mandatory gates, reach 70 points, and remain unlinked.

Success is shown only after the relationship is durably stored. The pair then
moves out of unlinked suggestions into linked possible matches, remains
selected with a linked state, and offers Unlink. The reports and their statuses
retain all canonical values unchanged.

### Already-linked and reverse-order duplicate

Linking A-B when A-B or B-A is already linked is an idempotent no-op. It creates
no duplicate, keeps the existing linked state, and tells the officer the pair
is already linked as a possible match. It is not presented as a new successful
link.

### Self-link

A self-link is invalid even if reached outside the normal UI. It creates no
relationship, changes no report, and produces privacy-safe invalid-action
feedback rather than success.

### Stale, missing, or newly ineligible pair

If either report is missing or the pair no longer meets current eligibility and
qualification when Link is activated, no link is created. The workspace
reconciles to current report/link state, removes the stale actionable
suggestion, clears private detail if the selection is no longer valid, and
explains that the pair is no longer available for linking without exposing
technical or private data.

### Link failure

If durable link creation fails, the pair remains visibly unlinked in the last
known truthful state, no success is shown, neither report changes, and the
officer receives privacy-safe failure information and may explicitly retry.

## Non-exclusive link cardinality

Possible-match links are non-exclusive. One report may participate in zero,
one, or multiple simultaneous possible-match links. Linking one pair does not
remove, reject, downgrade, or imply that another pair involving either report
is wrong. The product imposes no one-to-one ownership cardinality because a
possible-match link is not an ownership decision.

## Linked possible matches

The authenticated workspace contains a distinct linked-possible-matches
section. Each relationship appears once regardless of report order. Its row is
privacy safe and identifies the LOST and FOUND sides, linked state, and current
rule score/reason summary when available.

Selecting a current linked pair opens the same read-only comparison and current
rule evaluation, but exposes Unlink rather than Link. A durable link remains in
this section even if later report values would no longer qualify; it is labelled
as no longer qualifying and is never automatically unlinked.

If a linked relationship references a report that is no longer available, the
relationship remains listed in a privacy-safe unavailable form so an officer
can remove the relationship. Available report data may be shown under the
normal privacy boundary; missing report data must not be reconstructed or
invented. When ordinary row fields cannot identify the missing side, its
canonical Report ID may be shown solely to distinguish and unlink that stale
relationship. Report ID is not Reporter ID; Reporter ID and private detail
remain prohibited outside an available selected comparison. The row and
selection distinguish this from a load failure affecting the whole workspace.

If no relationships exist, the section states that there are no linked possible
matches. That state is independent of whether the unlinked suggestion list is
empty.

## Unlink Possible Match

Unlink removes only the selected officer-created possible-match relationship.
It does not delete a report, change any report field or status, assert that the
reports describe different items, or block the pair from becoming a future
suggestion if it still qualifies.

### Valid unlink

Success is shown only after durable removal. The relationship disappears from
the linked section. If both reports still exist and currently qualify, the pair
returns to the unlinked suggestion list, remains selected, and exposes Link. If
it no longer qualifies or a report is missing, it does not become a suggestion
and the selection clears.

### Repeated unlink

An unlink request for an already-absent relationship is an idempotent no-op. It
does not alter reports or other links, reports no new removal success, reconciles
the view to the relationship's absence, and tells the officer it is already
unlinked.

### Missing report and stale relationship

A missing report does not block removal of the relationship. An officer may
unlink the stale relationship from its privacy-safe linked entry. No report
content is inferred or restored.

### Unlink failure

If durable removal fails, the pair remains visibly linked in the last known
truthful state, no success is shown, neither report nor another relationship
changes, and the officer receives privacy-safe failure information and may
explicitly retry.

## Empty, error, and retry states

The product distinguishes all of the following:

| State | Observable result | Retry/action behavior |
| --- | --- | --- |
| No eligible reports | No eligible LOST-to-FOUND pair exists | Refresh remains available |
| Eligible reports, no qualifying pair | Candidate pairs exist but none pass gates and threshold | Refresh remains available |
| No linked possible matches | Linked section is empty | Suggestions remain independently usable |
| Report-loading failure | Workspace cannot establish authoritative reports | No report rows/details; Link/Unlink disabled; explicit Retry |
| Link-state loading failure | Workspace cannot truthfully separate linked and unlinked pairs | Workspace unavailable; no Link/Unlink; explicit Retry |
| Matching/evaluation failure | Reports loaded but suggestions cannot be evaluated safely | Not shown as empty; private comparison cleared; explicit Retry |
| Link failure | Selected pair remains last-known unlinked | No success; explicit action retry remains available |
| Unlink failure | Selected pair remains last-known linked | No success; explicit action retry remains available |

Retry performs a new authoritative attempt for the failed operation or load.
There is no automatic/background retry. Failure copy must not expose paths,
JSON, credentials, unsafe input, private report values, stack traces, exception
text, or implementation internals.

## Logout and re-entry

Logging out while viewing rows or a comparison immediately removes the matching
workspace and all visible sensitive state. No selection, Reporter ID, private
detail, score, or reason remains visible on the login screen. Logout performs
no Link or Unlink action.

Later Desk Officer login constructs a fresh view and loads current authoritative
reports and links. A previous selection is not restored. Durable links remain
visible if they still exist.

## Explicit non-goals

This PRD does not introduce:

- ownership confirmation or an owner-found result;
- claim, collection, return, notification, or handover workflows;
- automatic linking or automatic unlinking;
- ML, LLM, embeddings, fuzzy AI recommendation, edit distance, NLP, semantic
  similarity, probability, confidence, likelihood, or certainty output;
- matching on Reporter ID, public description, private identifying detail,
  creation time, or hidden criteria;
- Student-facing matching, comparison, score, reason, or link interfaces;
- a matching-related `ReportStatus` or a change to an existing status;
- an `ItemReport` change made to support matching;
- changes to Student submission, history, or search;
- a review-queue redesign, generic relationship graph, generic workflow
  engine, pagination, live polling, user-defined sorting, or unrelated
  refactoring;
- unnecessary UI polish, new dependencies, build/CI/release changes, or a new
  UI-test framework.

## Main scenarios and extensions

### SC-001 — Inspect and link a suggestion

1. An authenticated Desk Officer enters the workspace.
2. Current reports and link state load successfully.
3. Qualifying unlinked pairs appear in score-first deterministic order.
4. The officer selects one pair and sees both reports, all rule components,
   the total, and possible-match language.
5. The officer activates Link as Possible Match.
6. Current eligibility and qualification are rechecked.
7. Durable storage succeeds.
8. The pair moves to linked possible matches, remains selected in linked state,
   exposes Unlink, and changes no report.

### SC-002 — Inspect and unlink an existing relationship

1. The officer selects a linked possible match.
2. The current read-only comparison and reasons appear where available.
3. The officer activates Unlink Possible Match.
4. Durable removal succeeds.
5. The linked entry disappears; the pair returns to suggestions only if it
   currently qualifies, in which case it remains selected and exposes Link.
   Otherwise selection clears.
6. No report or other relationship changes.

### SC-003 — Empty versus unavailable

1. A successful authoritative load yields either no eligible pair, no
   qualifying pair, or no linked relationship.
2. The applicable empty message appears.
3. If loading or evaluation instead fails, an unavailable state and explicit
   Retry appear; no empty claim is made.

### SC-004 — Stale link attempt

1. The officer selects an unlinked suggestion.
2. Authoritative state changes before Link.
3. Link recheck finds a missing or ineligible report or non-qualifying pair.
4. No relationship is created and no report changes.
5. The view reconciles to current state and provides privacy-safe feedback.

### SC-005 — Persistence action fails

1. The officer activates Link or Unlink.
2. Durable mutation fails.
3. No success is shown and no report changes.
4. The selected relationship retains its last known truthful linked/unlinked
   state.
5. The officer may explicitly retry.

### SC-006 — Logout clears private state

1. The officer selects a comparison containing officer-only fields.
2. The officer logs out.
3. The matching UI and all visible report data are removed.
4. A later officer login starts from a fresh authoritative load with no restored
   selection.

## Acceptance criteria

### S2-D2-01 — Deterministic Matching Engine

- **ME-AC-001 — LOST-to-FOUND eligibility:** Every candidate contains exactly
  one distinct LOST and one distinct FOUND report; self-pairs, same-type pairs,
  and reversed duplicates are absent.
- **ME-AC-002 — Allowed statuses:** SUBMITTED and UNDER_REVIEW participate in
  every allowed cross-status combination; status changes score neither up nor
  down.
- **ME-AC-003 — Four permitted criteria only:** Category, item-name keywords,
  location, and occurrence date are the only matching inputs. Changing any
  excluded field cannot change qualification, components, reasons, score, or
  order.
- **ME-AC-004 — Exact category policy:** Equal canonical categories pass the
  mandatory gate and contribute 40 points; unequal categories and `OTHER` as a
  wildcard do not.
- **ME-AC-005 — Exact keyword policy:** Locale-independent case normalization,
  non-letter/digit separators, two-code-point minimum tokens, duplicate removal,
  no stop words, and exact unique-token overlap produce either 20 or 0 points
  exactly as specified.
- **ME-AC-006 — Exact location policy:** Locale-independent case normalization
  with every non-letter/digit run treated as a separator produces exact
  normalized equality for 30 points; partial and alias matches do not.
- **ME-AC-007 — Exact date policy:** FOUND on LOST day through day 7 inclusive
  passes the mandatory directional gate and contributes 10 points; FOUND before
  LOST and day 8 or later fail.
- **ME-AC-008 — Qualification and threshold:** Only pairs passing category and
  date gates and scoring at least 70 are suggestions; 70 qualifies and 50 does
  not.
- **ME-AC-009 — Deterministic score and components:** The score is exactly the
  visible 40/20/30/10 sum, ranges from 0 to 100, and contains no bonus, penalty,
  randomness, clock, locale-default, or iteration-order effect.
- **ME-AC-010 — Reason correctness:** Every positive component has one accurate
  officer-readable reason, failed components have no positive claim, and no
  reason uses a private or excluded field.
- **ME-AC-011 — Deterministic ordering:** Higher scores precede lower scores;
  equal scores have one TDD-defined deterministic total order independent of
  input encounter order.
- **ME-AC-012 — Reproducibility:** Repeated evaluation and shuffled input of the
  same canonical values produce the same pairs, components, normalized reason
  information, scores, and final order.
- **ME-AC-013 — Read only:** Generating, refreshing, selecting, and comparing
  suggestions mutate no report, status, or relationship.
- **ME-AC-014 — Empty versus failure:** No eligible pair and no qualifying pair
  are successful empty outcomes distinct from loading/evaluation failure.
- **ME-AC-015 — Explainability and language:** Documentation and presentation
  explain all rules and use only possible-match and rule-point language, never
  confidence, probability, ownership, verification, claim, or confirmation.

### S2-D2-02 — Officer Matching Workspace

- **OW-AC-001 — Authorization:** Only an authenticated Desk Officer can load
  the workspace; a Student cannot see suggestions, scores, reasons, links,
  comparisons, or another report's restricted fields.
- **OW-AC-002 — Suggestion list:** Initial load and Refresh show unlinked
  qualifying pairs in deterministic score-first order with privacy-safe rows,
  the approved exact row fields, score, brief reasons, and possible-match
  terminology.
- **OW-AC-003 — Side-by-side comparison:** Selecting an available pair shows all
  eleven canonical fields for LOST and FOUND in distinct read-only groups,
  plus score and all component outcomes.
- **OW-AC-004 — Private-information boundary:** Reporter ID and private
  identifying detail appear only in the selected authenticated officer
  comparison and never in rows, reasons, errors, logs, screenshots, Student
  results, or handoff summaries.
- **OW-AC-005 — Explicit Link:** Viewing has no side effect. A valid explicit
  Link persists one symmetric relationship, reports success only after durable
  storage, moves the pair to the linked section, and mutates no report/status.
- **OW-AC-006 — Repeated and reverse Link:** Already-linked and reverse-order
  requests are idempotent no-ops with already-linked feedback and no duplicate;
  self-link is rejected without mutation.
- **OW-AC-007 — Stale Link:** Missing, newly ineligible, or newly
  non-qualifying reports prevent Link, reconcile the view, report no success,
  and expose no private or technical data.
- **OW-AC-008 — Non-exclusive relationships:** One report can be linked to
  multiple other reports; creating one link does not remove or reject another.
- **OW-AC-009 — Linked section:** Each relationship appears once in a distinct
  linked-possible-matches section and remains reachable for review and unlink
  even when no longer qualifying or partly unavailable. Currently qualifying
  links are score ordered before stale/unavailable links; a no-longer-qualifying
  link shows failed components but no aggregate total. A missing side's Report
  ID may identify an otherwise ambiguous stale relationship without exposing
  Reporter ID or private detail.
- **OW-AC-010 — Explicit Unlink:** Valid Unlink durably removes only the
  selected relationship, reports success only after removal, mutates no report,
  and returns the pair to suggestions with selection preserved only if it
  currently qualifies; otherwise selection clears.
- **OW-AC-011 — Repeated and stale Unlink:** Repeated Unlink is an idempotent
  already-absent no-op; a missing report does not prevent removing its stale
  relationship.
- **OW-AC-012 — Failure truthfulness:** Link failure retains unlinked state;
  Unlink failure retains linked state; neither reports success or changes a
  report, and both allow explicit retry.
- **OW-AC-013 — Distinct states:** No eligible reports, no qualifying pairs, no
  linked relationships, report-load failure, link-state-load failure,
  evaluation failure, Link failure, and Unlink failure are observably distinct.
- **OW-AC-014 — Logout and re-entry:** Logout removes all matching and private
  presentation state; later officer login starts a fresh authoritative load
  while durable links remain persisted.
- **OW-AC-015 — No ownership confirmation:** No row, comparison, reason, score,
  action, success message, or guide text says or implies that ownership is
  confirmed.

## Product examples

All examples are synthetic. Each assumes distinct reports in an allowed status.

### EX-001 — Strong possible match

| Field | LOST | FOUND |
| --- | --- | --- |
| Item name | Blue Pencil-Case | pencil case, blue |
| Category | Stationery | Stationery |
| Location | ` School  Library ` | `school library` |
| Occurrence date | 2026-09-01 | 2026-09-03 |

Components: category +40, shared keywords `blue`, `case`, and `pencil` +20,
same normalized location +30, found two days after loss +10. Total: 100.
Result: shown as a possible-match suggestion. Officer reason: same category,
shared item-name keywords, same normalized location, and dates within two days.

### EX-002 — Location-led possible match

| Field | LOST | FOUND |
| --- | --- | --- |
| Item name | Spectacles | Black frame |
| Category | Personal items | Personal items |
| Location | Hall A | ` hall   a ` |
| Occurrence date | 2026-09-05 | 2026-09-12 |

Components: category +40, keywords +0, location +30, day-7 boundary +10.
Total: 80. Result: shown as a possible-match suggestion.

### EX-003 — Keyword overlap without location

| Field | LOST | FOUND |
| --- | --- | --- |
| Item name | Green water bottle | bottle with green cap |
| Category | Water bottles | Water bottles |
| Location | Classroom 2A | General office |
| Occurrence date | 2026-09-10 | 2026-09-10 |

Components: category +40, shared `bottle` and `green` +20, location +0,
same-day date +10. Total: 70. Result: shown at the inclusive score threshold.

### EX-004 — Keyword boundary does not qualify

| Field | LOST | FOUND |
| --- | --- | --- |
| Item name | Notebook | Book |
| Category | Books | Books |
| Location | Library | Canteen |
| Occurrence date | 2026-09-14 | 2026-09-15 |

`notebook` and `book` are different whole tokens, so keyword points are 0.
Components: 40 + 0 + 0 + 10 = 50. Result: not suggested.

### EX-005 — Location punctuation normalization

| Field | LOST | FOUND |
| --- | --- | --- |
| Item name | Sports bag | Bag |
| Category | Bags | Bags |
| Location | Library, Level 2 | Library - Level 2 |
| Occurrence date | 2026-09-16 | 2026-09-17 |

The punctuation runs act as separators, so both locations normalize to
`library level 2`. The shared token `bag` also matches. Components:
40 + 20 + 30 + 10 = 100. Result: suggested with all four reasons.

### EX-006 — Good text/location evidence outside the date window

| Field | LOST | FOUND |
| --- | --- | --- |
| Item name | Red jacket | Jacket red |
| Category | Clothing | Clothing |
| Location | Canteen | canteen |
| Occurrence date | 2026-09-01 | 2026-09-09 |

Components would account for 40 + 20 + 30 + 0 = 90, but day 8 fails the
mandatory date gate. Result: not suggested.

### EX-007 — Category gate

| Field | LOST | FOUND |
| --- | --- | --- |
| Item name | Blue pouch | Blue pouch |
| Category | Bags | Stationery |
| Location | Classroom 1B | Classroom 1B |
| Occurrence date | 2026-09-20 | 2026-09-20 |

Components would account for 0 + 20 + 30 + 10 = 60, but category equality is
mandatory. Result: not suggested.

## Documentation implications

Completed implementation will require, without pre-writing those guides here:

- Developer Guide explanation of eligibility, exact normalization, all four
  rule components, gates, weights, threshold, reason mapping, deterministic
  ordering, and the TDD-defined final tie-break.
- Developer Guide rationale for deterministic rule matching rather than ML,
  LLM, embeddings, probability, or fuzzy matching.
- Developer Guide workflow and interaction diagram from report load through
  evaluation, comparison, durable Link, linked review, and durable Unlink.
- Officer/User Guide instructions for suggestions, score interpretation,
  comparison, private-detail purpose, Link, linked section, Unlink, Refresh,
  empty states, failure/retry, logout, and the fact that ownership is not
  confirmed.
- An interaction summary aligned with the final UI and privacy-safe synthetic
  evidence. Screenshots, if later required, must not show Reporter ID, private
  detail, signed-in identity, credentials, unsafe report content, paths, JSON,
  or technical errors.

## Product risks and assumptions

| Risk or assumption | Product control in this PRD |
| --- | --- |
| Rules are too permissive | Exact category and directional seven-day gates plus a 70-point threshold constrain suggestions. |
| Rules are too restrictive | Either keyword overlap or exact normalized location can satisfy the threshold, and officers may link one report to multiple candidates. |
| Officers read score as confidence | Score is labelled rule points, every component is visible, and confidence/probability language is prohibited. |
| A link is mistaken for ownership confirmation | Possible-match wording is mandatory and Link has no status, claim, or handover effect. |
| Private data spreads beyond comparison | Restricted fields are allowed only in the selected authenticated comparison and are banned from rows, reasons, errors, logs, screenshots, and Student views. |
| Linked state becomes confused with current qualification | Linked pairs have a separate section and may be labelled no longer qualifying without automatic Unlink. |
| Stored text has surrounding or repeated whitespace | Matching always applies the specified normalization and never assumes storage is already normalized. |
| Occurrence dates are honest report data | The product uses stored dates only; this feature does not validate real-world truth or use the current clock. |

## Out-of-scope technical decisions deferred to the TDD

The TDD must decide the following without changing this PRD's observable
behavior:

- class and interface structure;
- service composition and module seams;
- persistence API and repository implementation for links;
- link storage format, version, and path;
- atomic write and recovery strategy;
- supported concurrency assumptions;
- exact technical final tie-break after score;
- JavaFX component hierarchy, layout mechanics, and styling;
- dependency injection and application composition details;
- failure exception taxonomy and internal error mapping;
- automated test classes, fixtures, fault seams, and red-green sequence;
- whether and how a stale linked relationship is represented internally; and
- the smallest cross-owner shell composition edit, which still requires
  separate approval before implementation.

The TDD may refine implementation-neutral UI wording but may not change rule
inputs, normalization, gates, points, threshold, privacy boundaries, action
semantics, empty/error distinctions, or link cardinality.

## Requirement index

| Product area | Primary requirements | Acceptance evidence destination |
| --- | --- | --- |
| Authorization and privacy | Actors, comparison boundary, logout | OW-AC-001, OW-AC-004, OW-AC-014 |
| Candidate eligibility | Eligibility before matching | ME-AC-001, ME-AC-002 |
| Four-rule policy | Matching product policy | ME-AC-003 through ME-AC-008 |
| Scoring, reasons, determinism | Aggregate score, reasons, ordering | ME-AC-009 through ME-AC-012, ME-AC-015 |
| Read-only behavior | Suggestion and comparison behavior | ME-AC-013, OW-AC-003 |
| Empty/failure behavior | Empty, error, and retry states | ME-AC-014, OW-AC-012, OW-AC-013 |
| Link semantics | Link as Possible Match | OW-AC-005 through OW-AC-007 |
| Non-exclusive links | Link cardinality | OW-AC-008 |
| Linked list and Unlink | Linked section and Unlink behavior | OW-AC-009 through OW-AC-011 |
| Ownership-safe language | Goal, glossary, non-goals | ME-AC-015, OW-AC-015 |

### Grilling-decision traceability

| Decision-ledger range | Reconciled PRD sections |
| --- | --- |
| GD-001–GD-003 | Eligibility before matching; Category rule; ME-AC-001, ME-AC-004 |
| GD-004–GD-008 | Item-name keyword rule; Matching reasons; ME-AC-005, ME-AC-010 |
| GD-009–GD-011 | Location rule; ME-AC-006; EX-005 |
| GD-012–GD-014 | Occurrence-date rule; ME-AC-007; EX-002 and EX-006 |
| GD-015–GD-021 | Aggregate score; Product-level ordering; ME-AC-008–ME-AC-012 |
| GD-022–GD-025 | Suggestion rows; Officer comparison; OW-AC-002–OW-AC-004 |
| GD-026–GD-030 | Link as Possible Match; SC-001; OW-AC-005–OW-AC-007 |
| GD-031–GD-039 | Non-exclusive links; Linked possible matches; Unlink; SC-002 and SC-005; OW-AC-008–OW-AC-012 |
| GD-040–GD-044 | Empty/error/retry; Refresh; Logout and re-entry; OW-AC-013–OW-AC-014 |
| GD-045–GD-047 | Product goal; Glossary; Explicit non-goals; ME-AC-015 and OW-AC-015 |

## Post-draft grilling and self-review outcome

An initial PRD draft existed before this review. The repository owner then
completed the required `/grill-me` rounds, accepted every recommendation, and
confirmed shared understanding. This revision was pressure-tested against the
resulting `GrillingDecisions.md`, the approved Mission Brief, both sprint items,
the canonical report model, report persistence boundary, authentication route,
and existing officer privacy pattern.

The review resolved the following potential ambiguities in this PRD:

- category and date are mandatory gates as well as visible point components;
- the date comparison is directional and includes days 0 and 7;
- keyword tokenization, minimum length, duplicates, punctuation, and stop-word
  behavior are exact;
- location treats whitespace and punctuation as separators and forbids partial
  matching;
- exact normalized location contributes 30 points and keyword overlap 20;
- score 70 is inclusive and a high raw total cannot override a failed gate;
- ordinary suggestion-row fields and selected-comparison detail are exact;
- linked pairs are separate from actionable unlinked suggestions;
- repeated Link and Unlink are idempotent no-ops with truthful feedback;
- links are non-exclusive and are never removed automatically;
- stale linked relationships remain removable and may use the missing side's
  Report ID solely for identification;
- successful Link/Unlink selection behavior is fixed;
- all empty and failure outcomes are distinct; and
- Reporter ID/private detail remain confined to the selected authenticated
  comparison.

No rule contradicts the Mission Brief, no hidden matching input remains, and no
technical architecture choice required for the TDD has been prescribed.

## Unresolved product decisions

None. Owner approval confirms that this PRD contains no placeholder, ambiguous
product rule, or question that the TDD would need to answer as product policy.

## Approval and next gate

The repository owner explicitly approved this complete PRD on 2026-09-21.
Approval authorizes only these product requirements. TDD and
requirements-to-tests creation, branch creation, tests, implementation,
commits, pushes, and pull requests remain unauthorized until separately
requested.
