# Royden's Contributions

This summary is organised by contribution and branch. It was prepared from
Royden's interaction logs, pull-request diffs, and Git history. The branches
for PRs #2, #12, and #13 were stacked on earlier work; each feature is
described once under the branch that introduced it. Closed persistence PRs #4
and #7 were superseded by the merged PR #9.

## 1. Developer 2 agent workflow — `royden/feat-dev2-agent-workflow` (PR #1)

Royden added the repository's Developer 2 delivery skill, shared `AGENTS.md`
ownership and data-safety rules, and the Sprint 1 authentication mission brief.
The workflow establishes cross-owner integration boundaries, synthetic-data
handling, verification, and handoff requirements for subsequent Developer 2
work; its planning and approval gates were tightened during the authentication
cleanup.

## 2. Local authentication and role navigation — `royden/feat-local-auth` (PR #2)

Royden implemented local username/password authentication with a versioned JSON
account store, salted PBKDF2 password verification, synthetic demo-account
provisioning, and an in-memory authenticated session. He added login validation,
role-based Student and Desk Officer landing routes, logout, the authentication
JavaFX pane, and focused service, storage, provisioning, and security tests.
This branch also recorded an authentication cleanup that separated the module
by responsibility and tightened its delivery gates. Its interaction log records
automated checks and both role destinations, while several detailed manual UI
checks remained pending.

## 3. JSON report persistence — `royden/feat-report-persistence-review3` (PR #9)

Royden implemented the `ReportRepository` contract and strict, versioned JSON
storage for ordered report loading, insertion, and whole-report replacement. The
repository checks conflicts and malformed or oversized data, reports typed
storage failures, and stages atomic file replacement without a non-atomic
fallback. He added format, recovery, concurrency, security, and replacement
tests plus a storage-format contract and developer documentation. He adapted the
persistence code and tests to Developer 1's canonical `ItemReport`, report
enums, constraints, and restoration API; he did not deliver the canonical
report domain. This PR was repository-only and did not wire report storage into
the application shell.

## 4. Desk Officer report review — `royden/feat-officer-review-queue` (PR #11)

Royden built the Desk Officer report queue service and JavaFX workspace over
the shared report repository, with status filters, report selection and detail,
refresh and retry states, and tests for persistence and failure handling. He
added the review mission and planning documents, workflow guidance, and a queue
screenshot. The initial queue included a start-review transition; the later
Claims integration made report review read-only and excluded reports attached
to approved Claims. The retrospective interaction log does not verify a human
run of the rendered JavaFX review workflow.

## 5. Possible-match workflow — `royden/feat-officer-review-queue` (PR #11)

On the same branch, Royden implemented deterministic lost-to-found suggestions
from canonical report fields, a separate durable store for linked report-ID
pairs, and Desk Officer services and JavaFX views for comparison, reasons,
linking, and unlinking. He added matching policy and storage tests, the Sprint 2
mission and planning documents, and guide coverage. The retrospective log
records source and history evidence but leaves manual JavaFX matching checks
pending.

## 6. Claims and Verification — `royden/feat-claim` (PR #12)

Royden implemented the Claim domain and independent JSON claim repository,
including validation, durable lifecycle decisions, and tests. He built Student
submission, evidence, tracking, and withdrawal workflows and Desk Officer
pending-review, approval, rejection, decision-reason, and history workflows,
with role-specific JavaFX panes. He connected Claims to the report and
possible-match repositories through session-aware workspace navigation and
privacy-limited views, and added the approved-Claim endpoint used by report
review. This branch also contains the Sprint 3 planning documents and an
appointment handover contract; it did not implement appointments. The Claims
retrospective leaves human verification of the rendered workflow pending.

## 7. Guides and interaction records — `royden/doc-guides-logs` (PR #13)

Royden reconciled the User Guide and Developer Guide with the implemented
Developer 2 architecture and role workflows through Claims. He added three
retrospective interaction summaries for officer review, possible matching,
and Claims, explicitly marking their original session evidence and manual
verification as pending. He also added scoped documentation-agent
configurations for the guides and logs.

## 8. Cross-feature test coverage — `royden/test-coverage` (PR #23)

Royden expanded automated tests across authentication, Student report UI,
matching, Desk Officer review, Claims, and workspace session composition. He
added JavaFX workflow test support and tests for role boundaries, state changes,
storage failures, and persistence reconstruction. The PR also adds appointment
service, storage-boundary, integration, and JavaFX workflow tests; these are
test contributions to ZhengHao's appointment feature, not an appointment
implementation claim.
