# S1-D2-03 Desk Officer Review Queue Grilling Decisions

- Status: Complete
- Decision date: 2026-09-20
- Feature: Build Desk Officer Review Queue
- Workstream: Developer 2
- Approved mission brief: `docs/mission-briefs/S1-D2-03-officer-review.md`
- Product requirements document: `docs/features/S1-D2-03/PRD.md`
- Reconciled repository baseline: `a1383ce2cbe6386b67cc675608a10df9ea663261`

## Purpose

This ledger records the inherited and owner-confirmed product decisions used by
the S1-D2-03 PRD. It is descriptive evidence of completed product grilling. It
does not authorize the TDD, tests, implementation, branch creation, commits,
publishing, or merging.

## Authority and reconciliation

The approved Mission Brief supplies the feature scope and original product
decisions. On 2026-09-20, the repository owner directed the PRD to treat the
current repository state as authoritative after Developer 1's canonical report
domain was merged and Developer 2's temporary report model was removed.

The reconciliation established that:

- the canonical `ItemReport` remains an immutable eleven-field value;
- its field names, accessor types, privacy meanings, and identity semantics are
  unchanged;
- `ReportType` remains `LOST` or `FOUND`;
- `ReportStatus` remains `SUBMITTED` or `UNDER_REVIEW`;
- all nine current canonical categories must be accepted and displayed;
- canonical enum display names, rather than storage tokens, are intended for
  user interfaces;
- `ItemReport.withStatus(...)` creates a complete status-changed copy but does
  not decide whether a transition is legal; and
- persistence ordering, replacement, durability, and failure behaviour remain
  unchanged.

The Mission Brief's references to an immutable Java record, an all-fields public
constructor, the old `35d5322` branch point, and the old main revision are stale
technical or planning facts. They do not invalidate an approved product
decision.

## Confirmed decisions

### Scope, audience, and visibility

- **GD-001 — Authorized audience:** Only the authenticated Desk Officer route
  exposes this workflow. Logout clears the session and removes access.
- **GD-002 — Queue membership:** The queue contains every stored report whose
  current status is `SUBMITTED`, regardless of Reporter ID. Reports already
  `UNDER_REVIEW` are excluded.
- **GD-003 — Queue order:** Queue order is repository insertion order. Filtering
  preserves relative order; no other sorting is added.
- **GD-004 — Filters:** The filters are exactly **All**, **Lost**, and **Found**.
  **All** is selected when the Desk Officer first enters the view.
- **GD-005 — Scope restraint:** Search, sorting controls, pagination, live
  updates, assignment to particular officers, and management of reports already
  under review remain excluded.

### Queue, selection, and details

- **GD-006 — Queue-row summary:** Each row shows only the canonical display
  values for Lost/Found type, item name, category, occurrence date, and
  location. Report ID, Reporter ID, public description, private identifying
  detail, status, and Created At are omitted from the row.
- **GD-007 — Selection across filters:** Changing filters preserves a selection
  while the selected report remains visible. If the selected report is filtered
  out, selection and details clear.
- **GD-008 — No-selection state:** Initial entry and any cleared selection
  outside a storage-error state show a neutral instruction to select a report.
  A contextual unavailable outcome replaces that instruction during a
  storage-error state. **Start review** remains visible but disabled.
- **GD-009 — Complete read-only details:** Selecting a row displays all eleven
  canonical values without editing. The private identifying detail is clearly
  separated from the public description.
- **GD-010 — Canonical presentation:** Lost/Found type, category, and status use
  the canonical human-facing display labels. Occurrence date uses the documented
  ISO local-date form, and Created At uses the documented UTC millisecond
  timestamp form.

### Empty states

- **GD-011 — Globally empty queue:** If no submitted report exists, the queue
  shows `No submitted reports.`
- **GD-012 — Filtered-empty queue:** If submitted reports exist but none match
  the active Lost or Found filter, the queue shows
  `No submitted reports match the Lost filter.` or
  `No submitted reports match the Found filter.`
- **GD-013 — Empty-state actions:** Either empty state clears selection and
  disables **Start review**. Empty states are inline rather than modal.

### Start-review workflow

- **GD-014 — Explicit immediate action:** **Start review** is available only for
  a selected visible submitted report. Activating it immediately attempts the
  transition; there is no additional confirmation dialog.
- **GD-015 — Only valid transition:** The only valid transition is
  `SUBMITTED -> UNDER_REVIEW`. Repeat and reverse transitions are invalid rather
  than idempotent successes and perform no persistence mutation.
- **GD-016 — Status-only outcome:** A successful transition changes only status.
  Report ID, Reporter ID, type, item name, category, location, occurrence date,
  both descriptions, and Created At remain exact.
- **GD-017 — Truthful durability:** Success is reported only after the complete
  replacement is durably accepted by the repository.
- **GD-018 — Successful UI result:** After durable success, the report
  immediately leaves the submitted queue, selection and details clear, the
  active filter remains selected, and a brief non-blocking confirmation is
  shown.

### Invalid, stale, and failed actions

- **GD-019 — Missing or stale target:** If the selected report is missing or no
  longer `SUBMITTED` when the action rechecks it, no success is reported and no
  transition is performed. The view shows a safe unavailable outcome, reloads
  the authoritative submitted queue, preserves the active filter, clears
  selection, and returns to the neutral no-selection state. If that reload
  fails, its explicit Retry also preserves the active filter.
- **GD-020 — Initial storage failure:** A load failure is never presented as an
  empty queue. The view shows a privacy-safe unavailable outcome with an
  explicit **Retry** action and no report details.
- **GD-021 — Transition storage failure:** A failed replacement shows a
  contextual, privacy-safe failure outcome. It retains the row, selection,
  details, and active filter because no successful transition occurred.
  **Start review** remains available for an explicit retry.
- **GD-022 — Error-detail boundary:** Corrupt or unsupported storage, access or
  safe-replacement failure, and size or encoding failure share the privacy-safe
  user-facing storage-error category. Technical categories remain available to
  trusted application code but are not displayed to the Desk Officer.

### Privacy and acceptance

- **GD-023 — Queue privacy:** Queue rows, empty/error states, success messages,
  screenshots, logs, and diagnostics never expose private identifying detail or
  report contents outside their approved view.
- **GD-024 — Details privacy:** Private identifying detail is visible only after
  selection within the authenticated Desk Officer details view.
- **GD-025 — Synthetic evidence:** Automated and manual evidence uses only
  synthetic reports and isolated temporary storage. The required queue
  screenshot excludes private detail, unsafe text, the signed-in username, and
  credential information.
- **GD-026 — Observable acceptance:** Acceptance evidence covers authorization,
  queue membership and order, all filters, both empty states, selection and
  details, durable success, status-only preservation, invalid repeat and reverse
  actions, stale or missing targets, storage failures and retries, logout, and
  privacy.

## Copy provenance

The three empty-state sentences in GD-011 and GD-012 were explicitly approved
during grilling. For the neutral prompt, success confirmation, stale-target
outcome, and the two storage-failure contexts, the owner confirmed the required
meaning and interaction rather than exact wording. The PRD therefore labels its
exact wording for those outcomes as proposed copy; approving the PRD approves
that copy without treating it as an earlier grilling decision.

## Intentional technical deferrals

The TDD will decide module and class boundaries, result types, exception mapping,
dependency seams, JavaFX layout, styling, internal reload mechanics, and test
class organization. Those decisions must preserve this ledger's observable
behaviour and may not introduce a second report model, new status, new filter,
or new repository operation without returning to PRD review.

## Completion

No product decision or owner response remains unresolved. The PRD is ready for
repository-owner review. Approval of either document does not authorize later
planning or delivery stages.
