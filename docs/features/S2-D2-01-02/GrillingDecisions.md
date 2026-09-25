# S2-D2-01-02 Officer Possible-Match Workflow Grilling Decisions

- Status: Approved
- Grilling status: Complete
- Decision date: 2026-09-21
- Approver: Repository owner
- Approval date: 2026-09-21
- Feature: Officer Possible-Match Workflow
- Workstream: Developer 2
- Source sprint items: S2-D2-01 Deterministic Matching Engine and S2-D2-02 Officer Matching Workspace
- Approved mission brief: `docs/mission-briefs/S2-D2-01-02-officer-possible-match-workflow.md`
- Product requirements document: `docs/features/S2-D2-01-02/PRD.md`
- Planning baseline: `royden/feat-officer-review-queue` at `24d7e9dc08d7646ade140d54cbbacebd86147bdc`

## Purpose and truthful chronology

This ledger records the inherited and owner-confirmed product decisions for
S2-D2-01-02. It is approved evidence of completed product grilling. Its
approval does not authorize a TDD, requirements-to-tests artifact, branch,
tests, implementation, commits, publishing, or merging.

The artifact sequence is:

1. The Mission Brief was approved on 2026-09-21.
2. An initial combined PRD draft was written on 2026-09-21.
3. The repository owner then requested and completed a `/grill-me` review of
   that existing draft on 2026-09-21.
4. The owner accepted each recommended product decision and explicitly
   confirmed shared understanding.
5. This ledger was finalized and the then-unapproved PRD was reconciled to it.

This ledger does not claim that grilling preceded the initial PRD draft.

## Authority and inherited decisions

The following decisions were already approved by the Mission Brief and were not
reopened during grilling:

- every candidate contains exactly one distinct LOST report and one distinct
  FOUND report;
- SUBMITTED and UNDER_REVIEW reports are eligible;
- only category, item-name keywords, location, and occurrence date affect
  matching;
- results are deterministic, explainable, and always possible matches;
- matching and viewing do not mutate an `ItemReport` or `ReportStatus`;
- links are explicit, durable, symmetric, non-exclusive possible-match
  relationships and do not confirm ownership;
- one report may participate in multiple links;
- linked pairs remain Desk Officer-visible for explicit unlinking;
- Reporter ID and private identifying detail may appear only in the selected
  authenticated officer comparison;
- Students receive no matching, comparison, score, reason, or link interface;
- automatic Link/Unlink, ownership verification, claims, returns, ML, LLM,
  embeddings, confidence output, and hidden matching inputs remain excluded;
  and
- exact technical tie-breaking, persistence, architecture, JavaFX structure,
  atomicity, recovery, dependency injection, and exception taxonomy belong to
  the TDD.

Repository evidence also confirms the canonical immutable eleven-field
`ItemReport`, the closed `ItemCategory` set, the two report types, the two
statuses, the report repository boundary, lazy authenticated Desk Officer
routing, logout removal of officer content, and the existing selected-detail
privacy pattern.

## Confirmed matching decisions

### Eligibility and category

- **GD-001 — Pair eligibility:** A candidate is one unordered pair of distinct
  Report IDs with exactly one LOST and one FOUND report. Each report is
  SUBMITTED or UNDER_REVIEW. A-B and B-A are one pair.
- **GD-002 — Exact category gate:** Canonical `ItemCategory` equality is a hard
  qualification gate and contributes 40 points. Cross-category pairs do not
  qualify regardless of other evidence.
- **GD-003 — `OTHER` is not a wildcard:** `OTHER` matches only `OTHER` under the
  same exact-equality rule.

### Item-name keywords

- **GD-004 — Keyword normalization:** Each `itemName` is lowercased using
  locale-independent `Locale.ROOT` behavior. Every maximal run of characters
  that are not Unicode letters or digits is a separator.
- **GD-005 — Token validity:** Empty tokens and tokens shorter than two Unicode
  code points are discarded. Duplicate tokens within one item name are
  removed.
- **GD-006 — No linguistic expansion:** There is no stop-word removal,
  stemming, singular/plural conversion, accent folding, Unicode canonical or
  compatibility normalization, spelling correction, substring matching, edit
  distance, semantic expansion, or fuzzy matching.
- **GD-007 — Exact overlap:** At least one exact shared normalized token
  contributes 20 points. The number or proportion of shared tokens does not
  change the component value.
- **GD-008 — Keyword reasons:** The selected comparison identifies the shared
  normalized token values in deterministic order. A compact suggestion row may
  use only a brief positive keyword-reason label.

### Location

- **GD-009 — Location normalization:** Each `location` is lowercased using
  locale-independent `Locale.ROOT` behavior. Every maximal run of characters
  that are not Unicode letters or digits acts as a separator. Leading and
  trailing separators are removed, and every internal separator run is
  replaced by one ordinary space.
- **GD-010 — Exact normalized location:** Locations contribute 30 points only
  when the complete normalized strings are equal. There is no partial,
  substring, alias, map, geospatial, accent-folding, Unicode-normalization, or
  fuzzy match.
- **GD-011 — Punctuation is a separator:** Harmless punctuation variation does
  not block a match. For example, `Library, Level 2` and
  `Library - Level 2` both normalize to `library level 2`.

### Occurrence date

- **GD-012 — Directional date gate:** The FOUND occurrence date must be the
  same as or later than the LOST occurrence date and no more than seven
  calendar days later. This is a hard gate and contributes 10 points.
- **GD-013 — Inclusive boundaries:** Day 0 and day 7 qualify. A FOUND date
  before the LOST date or on day 8 or later fails the gate.
- **GD-014 — Stored dates only:** The comparison uses stored `LocalDate` values
  and does not use current time, creation time, time zone, or time of day.

### Score, threshold, reasons, and ordering

- **GD-015 — Exact component weights:** Category contributes 40 points,
  item-name keyword overlap 20, normalized location equality 30, and the date
  window 10. The total range is 0 to 100 with no hidden bonus or penalty.
- **GD-016 — Inclusive threshold:** A suggestion must pass both mandatory gates
  and score at least 70. The qualifying totals are 70, 80, and 100.
- **GD-017 — Evidence precedence:** Exact normalized location ranks above a
  single or multiple shared item-name keywords because location contributes 30
  and keyword overlap contributes 20.
- **GD-018 — Rule-point semantics:** A score is deterministic rule points, not
  probability, confidence, likelihood, certainty, or ownership strength.
- **GD-019 — Visible accounting:** Every positive component has a corresponding
  officer-readable reason. Failed components receive zero and no positive
  claim. The selected comparison shows all four outcomes and their points.
- **GD-020 — Failed-gate presentation:** Unlinked non-qualifying pairs are not
  shown. A linked pair that no longer qualifies shows its failed gate and
  component outcomes with a clear no-longer-qualifying state, but no potentially
  misleading aggregate score.
- **GD-021 — Product-level order:** Qualifying suggestions and currently
  qualifying linked pairs are ordered by descending score. No-longer-qualifying
  or unavailable linked relationships follow scored linked pairs. All ties are
  deterministic; the exact final tie-break is a TDD decision.

## Confirmed workspace decisions

### Suggestion rows and selected comparison

- **GD-022 — Suggestion-row fields:** Each unlinked row shows LOST and FOUND
  labels, both item names, the shared category, each occurrence date and
  location, the rule score, and brief positive reason labels.
- **GD-023 — Row exclusions:** Report ID, Reporter ID, public description,
  private identifying detail, status, and creation time are excluded from
  ordinary suggestion rows. Exact shared normalized keyword values and complete
  component detail appear only after selection.
- **GD-024 — Complete selected comparison:** An available selected pair shows
  all eleven canonical fields for both reports in separate read-only LOST and
  FOUND groups, together with all component outcomes and reasons.
- **GD-025 — Private-detail boundary:** Reporter ID and private identifying
  detail appear only in that selected authenticated comparison. Private detail
  is visibly separate from public description and identified as officer-only
  verification information.

### Link behavior

- **GD-026 — Explicit Link meaning:** Link records only that a Desk Officer
  considers the two reports a possible match. Selection and viewing have no
  side effect.
- **GD-027 — Link-time revalidation:** Before reporting success, Link rechecks
  that both reports exist, remain distinct, form a supported LOST-to-FOUND
  pair, have allowed statuses, pass category/date gates, meet the 70-point
  threshold, and remain unlinked.
- **GD-028 — Stale Link:** A missing, newly ineligible, or newly
  non-qualifying pair is not linked. The workspace reconciles current state,
  clears invalid private comparison content, and provides privacy-safe feedback.
- **GD-029 — Repeated and reverse Link:** Linking an already-linked unordered
  pair is an idempotent no-op with already-linked feedback. It creates no
  duplicate and is not reported as a new successful Link. Self-link is invalid.
- **GD-030 — Successful Link presentation:** Durable success moves the pair
  from unlinked suggestions to linked possible matches, preserves the selected
  comparison, changes its state to linked, and exposes Unlink. No report changes.

### Linked lifecycle and Unlink behavior

- **GD-031 — Non-exclusive links:** One report may participate in multiple
  simultaneous links. Creating one does not remove, reject, or downgrade
  another.
- **GD-032 — No automatic unlink:** A link remains in the linked section until
  an officer explicitly unlinks it, even if the pair later stops qualifying or
  one report becomes unavailable.
- **GD-033 — Stale linked entries:** A stale linked entry is labelled no longer
  qualifying or report unavailable, displays only existing report data, and
  keeps Unlink available. Missing report content is never reconstructed.
- **GD-034 — Stale-entry identity:** When ordinary row fields cannot identify a
  missing side, its canonical Report ID may appear solely to distinguish and
  unlink that stale relationship. Report ID is not Reporter ID; no Reporter ID
  or private detail is exposed.
- **GD-035 — Explicit Unlink meaning:** Unlink durably removes only the selected
  possible-match relationship. It changes no report, makes no assertion that
  the items differ, and does not affect another link.
- **GD-036 — Successful Unlink presentation:** If the pair still exists and
  qualifies, it returns to unlinked suggestions, remains selected, and exposes
  Link. Otherwise selection clears and no suggestion is created.
- **GD-037 — Repeated Unlink:** Unlinking an already-absent relationship is an
  idempotent no-op with already-unlinked feedback, not a new removal success.
- **GD-038 — Missing-report Unlink:** A missing report does not prevent removal
  of the stale relationship.
- **GD-039 — Action failure truthfulness:** Link failure retains last-known
  unlinked state; Unlink failure retains last-known linked state. Neither
  reports success or changes reports, and each permits explicit retry.

### Empty, unavailable, refresh, and session behavior

- **GD-040 — Three independent empty outcomes:** The workspace distinguishes
  no eligible LOST-to-FOUND pair, eligible pairs but no qualifying suggestion,
  and no linked possible matches.
- **GD-041 — Failure is not empty:** Report-load, relationship-state-load, and
  matching-evaluation failures are unavailable outcomes rather than an empty
  result.
- **GD-042 — Unavailable presentation:** Initial load or Refresh failure clears
  private comparison content, disables Link/Unlink, avoids presenting old rows
  as current, and offers explicit Retry. There is no automatic retry or polling.
- **GD-043 — Successful Refresh:** Refresh reloads current reports and links and
  recomputes both sections. It preserves a selection only when the same pair
  remains in the same applicable section.
- **GD-044 — Logout and re-entry:** Logout removes all visible matching and
  private state without unlinking. Later Desk Officer entry loads current
  authoritative report/link state and restores no prior selection.

## Distinction among suggestion, link, and ownership

- **GD-045 — Suggestion:** A suggestion is deterministic engine evidence for a
  currently qualifying unlinked pair. It does not create a relationship.
- **GD-046 — Link:** A link is an explicit durable Desk Officer judgement that
  the pair is a possible match. It does not rewrite the score or prove the
  suggestion correct.
- **GD-047 — Ownership:** Ownership confirmation, claim, collection, return,
  and handover remain outside this feature. No score, reason, row, action,
  success message, or guide text may imply otherwise.

## Copy provenance

The owner confirmed the required meaning and interaction of suggestion, Link,
Unlink, repeated-action, stale, empty, unavailable, success, and retry outcomes,
not exact final sentences. The PRD may use implementation-neutral wording. Any
later copy refinement must preserve the confirmed semantics, privacy boundary,
and possible-match terminology.

## Intentional technical deferrals

The TDD owns class/interface design, service composition, persistence API,
storage format and path, atomicity and recovery, concurrency assumptions, exact
technical tie-breaking, JavaFX hierarchy, dependency injection, repository
implementation, exception taxonomy, automated-test structure, and the internal
representation of stale links. Each deferral must preserve this ledger's
observable behavior and acceptance obligations.

## Completion

### Owner-authorized reconciliation — 2026-09-21

After the original approval, the TDD preflight identified that GD-009 named
only whitespace and punctuation whereas the approved PRD includes all
non-letter/digit characters. The repository owner explicitly confirmed the
PRD rule as authoritative and authorized reconciling GD-009: `Hall A`,
`Hall-A`, and `Hall★A` normalize equivalently. This records the already
intended rule, not a scope change or a new original approval. The original
approval metadata and chronology remain unchanged. The owner also separately
authorized resuming TDD planning; implementation remains unauthorized.

No product decision or owner response remains unresolved. The repository owner
explicitly approved this completed ledger and the reconciled PRD on 2026-09-21.
Approval does not authorize the TDD, requirements-to-tests work, or
implementation.
