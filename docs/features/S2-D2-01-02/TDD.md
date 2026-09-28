# S2-D2-01-02 Officer Possible-Match Workflow Technical Design

- Status: Approved
- Draft date: 2026-09-21
- Owner: Developer 2
- Approver: Repository owner
- Approval date: 2026-09-21
- Feature: S2-D2-01 Deterministic Matching Engine and S2-D2-02 Officer Matching Workspace
- Mission Brief: `docs/mission-briefs/S2-D2-01-02-officer-possible-match-workflow.md` (Approved 2026-09-21)
- Decision ledger: `docs/features/S2-D2-01-02/GrillingDecisions.md` (Approved and complete 2026-09-21; GD-009 reconciled by owner direction on 2026-09-21)
- Source PRD: `docs/features/S2-D2-01-02/PRD.md` (Approved 2026-09-21)
- Inspected baseline: `royden/feat-officer-review-queue`, `24d7e9dc08d7646ade140d54cbbacebd86147bdc`
- TDD drafting authorization: Explicit current owner request, including resume request
- Implementation, branch creation, and cross-owner implementation authorization: Not granted
- Requirements-to-tests artifact: Not created; separate next planning stage

## 1. Authority, preflight, and scope

`AGENTS.md`, the current owner instructions, the approved Mission Brief, the completed decision ledger, and the approved PRD govern this design. The PRD fixes product behavior; the proposals below decide its technical realization. The repository owner approved the proposed types and interfaces in this TDD on 2026-09-21. Delivery work remains subject to the later gates recorded below.

Preflight read the root instructions, Mission Brief, complete ledger, then complete PRD in order. All three artifacts record Repository owner approval dated 2026-09-21. The ledger has no unresolved product decision. The owner resolved the one discovered discrepancy: GD-009 now uses every non-Unicode- letter/digit run as a location separator, exactly as the PRD requires. Its dated reconciliation preserves the original approval history. No further product contradiction was found.

Earlier artifact statements that TDD work was not authorized record the state at their approval; the subsequent explicit TDD and resume requests supply that authorization. They do not approve this draft or authorize implementation. The delivery skill's implementation gates are not prerequisites for writing this requested planning artifact.

The resumed worktree had no tracked diff and no partial TDD. The Mission Brief, feature planning directory, and unrelated `evals/` were untracked. Source inspection confirms the delivered officer-review code is present at the exact planning baseline; no dependency on an unmerged absent review implementation remains. `evals/` is outside this work and was not inspected or changed.

The complete vertical feature is canonical reports -> eligibility -> four-rule evaluation -> suggestions -> selected officer comparison -> explicit durable Link -> linked review -> explicit durable Unlink. Reports remain canonical, immutable, and untouched by matching. Links are non-exclusive possible-match judgements, never ownership confirmation. No Student feature, report-store v1, build dependency, report enum, or authentication policy changes.

## 2. Confirmed architecture and integration points

In the tables below, `base` means `io.github.cs32272610mp2xcode.finderskeepers`. Java file paths follow packages under `src/main/java/`; test counterparts live under `src/test/java/`.

| Existing type / file | Confirmed contract and use | Owner |
| --- | --- | --- |
| `base.report.ItemReport` | Final immutable class, not a record; UUID `reportId`, String `reporterId`, type, item name, category, location, LocalDate occurrence date, public description, private identifying detail, status, Instant creation time. Eleven accessors; `create`, `restore`, `withStatus`; redacted `toString`. Restored valid text is preserved exactly. Consume directly; never call `withStatus` from matching. | Developer 1 |
| `base.report.ReportType` | `LOST`, `FOUND`; canonical display labels. | Developer 1 |
| `base.report.ReportStatus` | `SUBMITTED`, `UNDER_REVIEW`; both eligible here. | Developer 1 |
| `base.report.ItemCategory` | STATIONERY, BOOKS, CLOTHING, BAGS, WATER_BOTTLES, ELECTRONICS, SPORTS_EQUIPMENT, PERSONAL_ITEMS, OTHER; exact enum equality. | Developer 1 |
| `base.report.ReportConstraints` | Existing occurrence-date and creation-time display formatters. | Developer 1 |
| `base.report.persistence.ReportRepository` | `loadAll()` returns an unmodifiable insertion-ordered snapshot; `insert`, complete identity-preserving `replace`. Matching uses only `loadAll`. | Developer 2, shared interface |
| `base.report.persistence.JsonReportRepository` | Synchronized instance operations, rereads durable state, strict version-one JSON, 16 MiB bound. No relationship contract. | Developer 2 |
| `ReportStoreJsonCodec`, `ReportStoreFiles`, `NioReportStoreFiles`, `StoreFileFailure`, `ReportStoreException` in `base.report.persistence` | Package-local codec/file seams; bounded reads, sibling staging, forced writes and atomic replacement; fixed checked errors. Reuse the pattern, not inaccessible report-specific helpers or report schema. | Developer 2 |
| `base.FindersKeepersApp` | Creates one `JsonReportRepository(Path.of("data", "reports.json"))`, injects it into Student factory and lazy officer supplier. Current supplier creates `DeskOfficerReviewPane(new DeskOfficerReviewService(reportRepository))`. | Developer 1 shell / shared composition |
| `base.auth.application.AuthenticationCoordinator`, `ApplicationRoute`; `base.auth.ui.AuthenticationPane` | Stored identity supplies STUDENT / DESK_OFFICER routing. Supplier is called only for DESK_OFFICER. Logout clears session and replaces the authenticated subtree with login. | Developer 2 |
| `base.review.application.DeskOfficerReviewService`, `ReviewQueueState`, `ReviewQueueFilter` | Per-view plain-Java service, immutable state, SUBMITTED-only queue, All/Lost/Found filters, selected canonical detail, authoritative Start review recheck. Review legitimately writes status; matching must not. | Developer 2 |
| `base.review.ui.DeskOfficerReviewPane` | Programmatic JavaFX, render guard, custom public cells, selected-only private detail, synchronous repository use, local `review.css`. | Developer 2 |
| `base.report.bootstrap.StudentReportWorkspaceFactory` | Already supports `create(ReportRepository)` and shares the supplied instance. No additional Student wiring needed. | Developer 1 |
| `base.report.application.ReportSubmissionService`, `StudentReportHistoryService`; `base.report.ui.StudentReportHomePane`, form/history controllers and panes | Existing submission and exact authenticated Reporter-ID history. History has ROOT normalization and UUID-string final ordering precedent. Its substring search is not matching policy. Remain unchanged. | Developer 1 |

Existing tests use JUnit Jupiter, synthetic `ItemReport.restore` fixtures, `@TempDir`, real fresh repository instances, and scripted repository/file faults. `DeskOfficerReviewServiceTest` and `DeskOfficerReviewPersistenceTest` establish service-state and durable integration seams. Report persistence has format, storage, security, recovery, replacement, and concurrency suites. No JavaFX test framework is introduced. Existing documentation uses fenced text diagrams; this design follows that convention.

## 3. Proposed modules and ownership

All additions below belong to Developer 2. There is one pure matching module, one relationship persistence module, one application service with immutable state, and thin JavaFX presentation. There is no matcher interface, generic rule framework, separate controller/mapper layer, event bus, or shadow report.

| Proposed type | Responsibility, inputs / outputs, dependencies | Deliberately does not own |
| --- | --- | --- |
| `base.matching.model.PossibleMatchPair` | Immutable normalized pair of UUIDs. Factory `of(UUID, UUID)`, `firstId`, `secondId`, value equality/hash and pair comparator. JDK only. Shared by matching and links so symmetry has one definition. | Report data, LOST/FOUND orientation, score, status, officer identity, timestamps |
| `base.matching.model.MatchEvaluation` | Immutable eligible-pair evidence: pair, LOST ID, FOUND ID, four outcomes, shared tokens, day difference; derived points, qualification and reasons. Nested `Criterion` / `CriterionResult` types keep the fixed four-rule vocabulary local. | Persistence, JavaFX, full reports |
| `base.matching.model.DeterministicMatcher` | Concrete pure `generate(List<ItemReport>)` and `evaluate(ItemReport, ItemReport)`; fixed policy and score-first comparator. Returns immutable nested `Generation` plus qualifying evaluations. Owns all normalization and eligibility. | JSON, links, clocks, randomness, report writes, UI controls |
| `base.matching.persistence.PossibleMatchRepository` | Three operations: load pairs, add pair if absent, remove pair if present. Reports checked storage errors. | Canonical-report loading or matching policy |
| `base.matching.persistence.FilePossibleMatchRepository` | Implements the repository with a strict versioned pair file, bounded reads and atomic complete replacement. Constructor takes Path; no startup I/O. | Report JSON or cross-file transactions |
| `base.matching.persistence.PossibleMatchStoreException` | Checked category-only storage failure, following existing redacted error pattern. | Raw exception causes, paths, report values |
| `base.matching.persistence.PossibleMatchStoreFiles`, `NioPossibleMatchStoreFiles` | Package-local actual filesystem seam: bounded read and atomic replacement. The NIO implementation owns staging, force, move, cleanup. | Codec/policy, public test hooks, generalized storage framework |
| `base.matching.application.OfficerMatchingService` | Constructor receives existing ReportRepository, PossibleMatchRepository and concrete matcher. Owns loading, partitioning, selection, Link/Unlink rechecks, immutable state, safe outcomes and retry. | Authentication decisions, JavaFX, report mutation |
| `base.matching.application.MatchingWorkspaceState` | Immutable presentation state; nested row, selection, section, availability and feedback value types. Rows are restricted projections; selected details reference canonical ItemReports. | Independent report model, rule recalculation, storage |
| `base.matching.ui.OfficerMatchingPane` | Renders state, forwards actions, builds read-only comparison and clears detached content. Receives one service. | Rule logic, persistence calls or paths, role checks |
| `base.review.ui.DeskOfficerWorkspacePane` | Composes existing review pane and new matching pane in non-closable tabs. Constructor receives shared repositories; constructs fresh services/panes. | New router, login, ownership policy or review redesign |

The pair itself is the entire persisted relationship value; no separate `PossibleMatchLink` wrapper is needed. Presence in the repository supplies the fact that the pair is linked. No rule version, score snapshot, creation timestamp, link creator, or report copy is required by the approved workflow.

## 4. Pure matching contract

`generate(List<ItemReport>) -> Generation` accepts a defensive snapshot of canonical objects. Null list/member or duplicate Report ID is a fixed-message invalid-input exception, not a partial result; even identical duplicate inputs are rejected because the repository contract promises unique IDs. The service maps such a contract violation to evaluation unavailable. It must never choose an encounter-order winner among conflicting reports.

`Generation` contains `boolean hasEligiblePairs` and `List<MatchEvaluation> qualifyingPairs`, copied immutably and sorted as in section 6. It knows nothing about link state. Its qualifying pairs become unlinked suggestions only after the application service subtracts stored links.

Partition by exact LOST / FOUND type with explicit status allowlist SUBMITTED or UNDER_REVIEW. Evaluate the LOST x FOUND Cartesian product once, skipping equal IDs defensively. The two partitions and unique Report IDs prevent self-pairs and reversed duplicates. There is no reporter-identity restriction. `hasEligiblePairs` is true if both partitions are non-empty, before category or date gates. Retain only qualifying evaluations, sort the result explicitly. Normalization can be computed once per report within this invocation; any local cache is keyed by ID and discarded afterwards. No global cache or index. Time is O(L*F plus result sorting), with no extra product size limit or silent candidate truncation. This is proportionate to the existing local desktop dataset and synchronous workflow; large-data responsiveness is a limitation.

`evaluate(ItemReport a, ItemReport b) -> Optional<MatchEvaluation>` accepts either order. Self, same-type or unsupported-status pairs return empty, meaning ineligible, not storage failure. Otherwise it orients LOST / FOUND from current canonical types and returns all four outcomes even if category/date fail or the total is below threshold. This same operation supports Link rechecks and currently non-qualifying linked comparisons. Missing reports are handled by the service, not fabricated engine inputs.

### Exact rule implementation

Lowercase complete strings with `Locale.ROOT`, then iterate Unicode code points. A retained code point satisfies `Character.isLetterOrDigit(int)`; every maximal run of other code points separates tokens. This includes symbols, punctuation, whitespace and combining marks. No Unicode normalization, accent folding, stop words, stemming, aliases, substring/fuzzy matching or expansion. This helper is private to the matcher and shared by its two text rules.

| Criterion | Calculation | Outcome / points |
| --- | --- | --- |
| CATEGORY | `lost.category() == found.category()`; OTHER is ordinary equality | Pass 40, fail 0; mandatory gate |
| KEYWORDS | Split lowercased item names, discard empty and fewer-than-two-code-point tokens, deduplicate, intersect exact strings | Non-empty intersection 20, otherwise 0; no weighting by count |
| LOCATION | Join all non-empty lowercased letter/digit runs with one U+0020, retaining one-code-point runs | Complete normalized equality 30, otherwise 0 |
| DATE | `ChronoUnit.DAYS.between(lost.occurrenceDate(), found.occurrenceDate())`, kept as long | `0 <= days && days <= 7` gives 10, otherwise 0; mandatory gate |

Location has no token-length filter. `Hall A`, `Hall-A`, `Hall★A` all become `hall a`. Two separator-only valid stored locations both normalize to the empty string and compare equal: this follows the approved exact equality rule; do not add an unapproved non-empty gate. Keywords with no valid tokens still contribute zero. Day subtraction avoids overflow from adding seven days to a maximum LocalDate. No wall-clock date validation is performed.

Aggregate points are exactly the sum of the four outcomes, 0..100. `qualifies = categoryPassed && datePassed && total >= 70`, with eligible orientation already enforced. Qualifying scores are 70, 80, 100. A raw 90 with failed date is not qualifying. Reporter ID, descriptions, creation time, and report status beyond eligibility are never consulted for score or reasons.

## 5. Evidence and reason representation

`MatchEvaluation` is an immutable value with a controlled construction path used by the matcher; callers cannot supply inconsistent totals or reason copy. Its fields are pair, lostId, foundId, category outcome, keyword outcome, location outcome, date outcome, sorted unique sharedTokens and signed dayGap. `CriterionResult` identifies the criterion and pass/fail; points and gate status derive from the fixed criterion, never arbitrary supplied weights. `components()` returns CATEGORY, KEYWORDS, LOCATION, DATE in that order. `totalPoints()` and `qualifies()` are derived, not separately mutable fields.

Reason methods on the evaluation derive immutable outputs directly from these results. `positiveReasonLabels()` omits failed criteria and uses the PRD's four concise labels. `componentReasons()` returns all four outcomes, points and neutral failure explanations. The positive keyword detail includes sharedTokens in ascending Java `String.compareTo` order; it is never emitted in row summaries. Date detail uses the stored dayGap and inclusive window; negative or over-seven values receive only the failure explanation. Category and location reasons can use fixed text without repeating untrusted values. Numbers use stable decimal conversion rather than locale-dependent formatting.

No UI rule evaluation or independent reason reconstruction is permitted. Rule points are always labelled as such and accompanied by the explanation that they do not establish ownership. `MatchEvaluation` may retain a raw total for pure calculation tests, but service presentation exposes a total only when currently qualifying. Missing/ineligible comparisons have unavailable outcomes, not invented zero scores or false gate failures.

Collections use defensive `List.copyOf` / `Set.copyOf`; nested values are immutable. Types containing report-derived strings or selected reports use redacted `toString`, including row/state records, so automatic record strings cannot become accidental diagnostics. No evaluation or report content is logged.

## 6. Pair identity and exact deterministic order

`PossibleMatchPair.of(a,b)` rejects null IDs and equal IDs before any I/O. Order IDs by `UUID.toString().compareTo(...)`, using canonical lowercase 36-character UUID strings. Store the lesser ID in `firstId`, the greater in `secondId`. All construction paths enforce this invariant. Equality and hash use both canonical UUID fields. Thus A-B equals B-A while A-C remains distinct. LOST/FOUND orientation is separate from pair identity and may change with canonical report values; it is never stored in the relationship.

The exact suggestion comparator is:

```text
1. totalPoints descending
2. pair.firstId.toString() ascending (String.compareTo)
3. pair.secondId.toString() ascending (String.compareTo)
```

Linked order is:

```text
1. currently qualifying pairs first; all other links second
2. within qualifying pairs: totalPoints descending
3. within either group: canonical first ID ascending, then second ID ascending
```

No extra precedence is imposed between non-qualifying and missing-report links. UUID strings are fixed representations of stable identity, not display copy. The two-ID tuple uniquely distinguishes pairs, so equal scores cannot leave an unresolved tie. It works even when either report is missing. Do not use hash order, repository order, category label, item name, createdAt, token count or natural UUID signed-long comparison as an additional ordering rule. Sorting shared tokens and using fixed component order also fixes reason order.

## 7. Relationship repository interface

The public interface is deliberately three operations:

```text
Set<PossibleMatchPair> loadAll() throws PossibleMatchStoreException
boolean link(PossibleMatchPair pair) throws PossibleMatchStoreException
boolean unlink(PossibleMatchPair pair) throws PossibleMatchStoreException
```

Each call reads the current target file, validates the complete store, and returns immutable values or performs one complete replacement. `link` returns true only after adding an absent pair durably; false means already present and performs no write. `unlink` returns true only after removing a present pair durably; false means already absent and performs no write. No cached snapshot survives calls. A fresh repository pointed to the same path sees the same set. Returned set encounter order is not a contract; presentation always sorts.

The repository validates pair structure and uniqueness. The application service validates references and current qualification against `ReportRepository` before creating a new link. Reads deliberately retain structurally valid pairs with unknown report IDs; otherwise missing-report links could not be unlinked. The repository never loads reports, decides eligibility or cascades deletion. Normal production mutations are reachable only through the officer service.

## 8. Storage representation and versioning

Use `data/possible-match-links.txt`, a sibling of `data/reports.json`. A small ASCII-compatible UTF-8 pair format avoids a second general JSON parser and new dependencies. Existing report JSON remains untouched. The file contains only canonical UUID pairs, with no private information or duplicated report values.

Canonical bytes are:

```text
FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS 1
10000000-0000-0000-0000-000000000001 20000000-0000-0000-0000-000000000002
```

The example IDs are synthetic. Exact grammar:

```text
document = header LF (uuid SP uuid LF)*
header   = "FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS 1"
uuid     = lowercase hexadecimal 8-4-4-4-12 UUID form
SP       = one ASCII space; LF = one byte 0x0A
```

Encoder always emits LF and a final newline, no BOM, no blank lines, and pairs sorted by their canonical ID tuple. Decoder accepts LF or CRLF line endings and an optional final newline; no other whitespace relaxation, comments, unknown columns or blank lines. Every UUID must pass both the exact textual grammar and UUID parsing. Uppercase/short UUID forms are rejected rather than silently altered. Pair columns may be reversed and records may be unsorted on read: normalize each pair, then detect duplicates before returning any result. A later genuine mutation writes canonical order. Reading alone never rewrites.

A header with the exact magic and a numeric version other than 1 yields UNSUPPORTED_VERSION. Missing/malformed magic, nonnumeric version, invalid UUID, self-pair, duplicate or reverse duplicate yields CORRUPT_STORE. A valid header with no pair lines is the only existing-file empty representation. A missing file is new/empty and does not create directories on read; zero bytes or whitespace-only content is corrupt, not empty.

Bound existing input and proposed output at 16,777,216 bytes, matching the existing report-store resource guard. Read at most bound plus one; over-limit input is CORRUPT_STORE and over-limit output is RESULT_TOO_LARGE before staging. This is a storage protection limit, not a rule about exclusive links. No partial decoding, silent line skipping, auto-migration, truncation or repair.

## 9. Atomicity, durability, recovery, and concurrency

`FilePossibleMatchRepository` normalizes the supplied path to absolute form, uses synchronized methods, and holds no lasting report or link cache. Its package-local filesystem seam has `readBounded(Path,int) -> Optional<byte[]>` and `replaceAtomically(Path,byte[])`; checked failures carry only a category. NIO catches IOException and SecurityException without propagating raw causes. The codec can be private helpers within the repository; no public codec API.

Mutation sequence follows the inspected NIO report-store pattern:

1. Read and fully validate the current store. Compute a set copy with exactly the requested pair added/removed. Return false immediately for a no-op.
2. Encode and enforce the output bound before touching the target.
3. Create the parent if necessary. Reject a target that is a symlink, directory, inaccessible path or other non-regular file. Missing checks use NOFOLLOW_LINKS and verify ancestor accessibility as in report persistence.
4. Create a unique sibling `.possible-match-links-*.tmp`; write all bytes, looping for partial channel writes, force(true), and close the channel.
5. Replace the target using ATOMIC_MOVE and REPLACE_EXISTING. Unsupported atomic replacement is failure; never fall back to truncation or a non-atomic copy. Successful move is the commit point.
6. Return true without another fallible read/write. Before commit, clean up only the temporary file created by this operation, best effort. Cleanup failure does not mask the original failure or alter the target.

No read writes or recovers from a temp file. A terminated process before commit leaves the old target (or absence); after commit a fresh instance reads the new target. Orphan temps are ignored, never promoted or used to infer success. No backup/journal or second transaction file is needed because only the link file changes. Manual repair of a corrupt store is outside this UI and requires separate authorization; Retry only rereads it, never resets it.

The supported model is one local application process, one application-lifetime instance of each repository, serialized synchronous JavaFX actions, and a local filesystem with reliable atomic replacement. No concurrent external writer, multi-instance concurrent writer, network filesystem guarantee or malicious filesystem race protection is claimed. Fresh sequential instances are supported. Report recheck and link commit are not a cross-file transaction; within this model no Student/review action can interleave between them. A changed report observed on a later call is reconciled normally. No report lock/API change is needed. Temp-name randomness is an I/O detail and never affects matching.

Durability means forced file bytes and successful atomic replacement survive normal process restart under this filesystem model. The existing platform pattern does not promise directory-entry fsync or survival of arbitrary power loss/controller failure. After a crash with no returned outcome, the target is authoritative; the application makes no retrospective success claim.

| Condition | Repository result / application behavior |
| --- | --- |
| Missing target with accessible ancestors | Empty new store; read creates nothing |
| Header-only valid store | True empty links |
| Zero bytes, malformed content, duplicate/reverse duplicate, self-link, invalid UUID, excess size | Whole store unavailable; no partial rows, no mutation, no silent empty |
| Unknown canonical report ID in a valid pair | Retain pair; service marks missing side and enables explicit Unlink |
| Unsupported version | Unavailable; preserve bytes; no migration |
| Unreadable target or inaccessible ancestor | READ_FAILURE, not missing/empty |
| Encode bound exceeded | RESULT_TOO_LARGE; old target unchanged |
| Stage/write/force/close/move failure, including unsupported atomic move | WRITE_FAILURE; no success; old committed target retained under supported model |
| Orphan temporary file | Ignore; target alone defines durable state |

## 10. Application service and immutable presentation state

`OfficerMatchingService` has no I/O in its constructor. Its public operations return `MatchingWorkspaceState`: `enter()`, `refresh()`, `retry()`, `select(Section, UUID a, UUID b)`, `link(UUID a, UUID b)`, `unlink(UUID a, UUID b)`, `clear()`. `Section` is SUGGESTIONS or LINKED. The UI dispatches action IDs from the service-owned selected comparison; it cannot submit scores or reports. Explicit command identity lets the service safely handle a repeated request even after the first Unlink cleared selection. Each command canonicalizes its IDs and rechecks durable state; it never depends on a stale row's claimed link status. Reverse IDs normalize to the same pair. A self/null selection or command maps to INVALID_PAIR and clears selection before any mutation. Pair factory and repository invariants also reject self-link outside the UI.

State comprises availability (NOT_LOADED, READY, UNAVAILABLE), ordered suggestion and linked rows, suggestion-empty reason, linked-empty flag, optional selected comparison, optional feedback/failure category, and derived Link/Unlink/Retry enablement. Availability controls empty semantics: unavailable never means empty. All collection snapshots are immutable.

Nested `PairRow` holds an internal pair key, optional `ReportSummary` for each canonical endpoint, presentation kind (SUGGESTED, LINKED_QUALIFYING, LINKED_NON_QUALIFYING, LINKED_REPORT_UNAVAILABLE), optional qualifying score and brief positive reason labels. `ReportSummary` contains only report type, item name, category, occurrence date, and location. It is a presentation projection, not a replacement for ItemReport; no persistence or matching accepts it. Internal IDs are necessary for selection but are not rendered in ordinary rows. Rows contain no Reporter ID, descriptions, status, creation time, exact shared tokens, or ItemReport reference.

Nested `SelectedComparison` contains the pair, section, optional canonical reports for first/second endpoints, and optional current MatchEvaluation. Rendering orients available LOST/FOUND reports from their actual types. Its presentation score is present only for a qualifying evaluation. When both reports exist but are ineligible, show current fields and ineligibility, no aggregate and no invented rule results. If types now coincide, label each actual type rather than pretending one is FOUND. When one/both reports are missing, show only available canonical data and unavailable placeholders. Do not infer a missing side's type from ID order. Its Report ID may identify that unavailable side; never substitute Reporter ID. A linked selection always retains Unlink while the workspace is ready.

Full report snapshots are retained only inside the per-officer service and selected comparison; other UI state contains public projections. These are references to canonical immutable objects, not copied report-shaped models.

### Enter, refresh, selection and retry

Enter clears prior state and loads reports, then relationships, then invokes the matcher. Build ID lookup and both sections locally; publish READY only after all work succeeds. Subtract linked pairs from qualifying evaluations. Evaluate linked pairs not already evaluated with the same matcher; keep every stored pair even if missing/ineligible/non-qualifying. Sort both lists by section 6. Selection is initially absent.

Refresh repeats that pipeline. Preserve a selection only if the same pair remains in the same section, rebuilding its comparison from the fresh snapshot. If it changes section or disappears, clear it. Initial/refresh failure discards rows and canonical snapshots, clears selected/private content, disables both mutations, and offers explicit Retry. `retry()` in UNAVAILABLE reruns the load without restoring discarded selection. There is no timer, polling or auto retry.

Selection is an in-memory lookup of a row from the last successful authoritative load, not a separate storage write or implicit refresh. It displays that snapshot's canonical values and evaluation. Missing row clears comparison; unavailable state ignores selection. Link always rechecks current data.

Empty derivation is independent of linked membership: no eligible pair is distinct from eligible pairs but no qualifying pair. If qualifying pairs exist but all are linked, the suggestion list simply has no unlinked suggestions; do not incorrectly say none qualify. The linked section's empty flag reflects the actual loaded relationship set independently.

## 11. Link sequence and stale action handling

The UI dispatches Link only for a selected unlinked suggestion; without a selection it dispatches nothing. The service rejects commands while unavailable. A repeated command for a linked pair cannot create a new link, including after selection moves or clears; the UI disables Link in linked state. For a command:

1. Retain prior state for failure handling and use the canonical pair key.
2. Load current canonical reports and current relationships. Compute current evaluations/rows before attempting any write. A read/evaluation failure reports the appropriate safe action failure with no mutation, retaining last-known state explicitly as such; an explicit Refresh failure instead follows the full unavailable-clearing behavior above.
3. If the pair is already linked, publish reconciled linked state with ALREADY_LINKED, not new-Link success. This check precedes the new-link eligibility rejection: an existing stale link is still a link.
4. Otherwise require both reports to exist and `evaluate` to be present and qualifying: distinct LOST/FOUND, supported statuses, category/date gates, at least 70 points. A stale/missing/ineligible/non-qualifying pair causes no write, publishes the newly loaded sections with STALE_PAIR, removes the stale suggestion and clears invalid selected/private content.
5. Prepare the success state from this report snapshot and link set plus pair; call repository `link(pair)`. True means committed new Link; publish that prepared state, retain the pair selected in LINKED, expose Unlink and give LINKED feedback. There is no post-commit reload that could mask success.
6. False is ALREADY_LINKED, never new success. In the supported single-writer model this means the pair is present; reconcile that pair to LINKED using the already loaded reports. Reverse requests cannot produce duplicates.
7. On checked write failure publish no success, retain last-known unlinked relationship state and comparison, flag it as last-known, and allow explicit action retry. Do not optimistically move the pair. Retry calls Link again and repeats the complete authoritative precheck.

No Link path invokes report insert/replace/withStatus. Adding A-B does not remove A-C or impose per-report exclusivity.

## 12. Unlink sequence and lifecycle

Unlink targets the canonical command pair. The normal UI enables it only for a selected LINKED row and dispatches those IDs. A repeated command checks current absence even if selection has since cleared. The service rejects commands while unavailable; an absent UI selection dispatches no command.

1. Read current canonical reports and relationships and derive current evaluations/sections before writing. Failure retains last-known linked state, no success, and explicit action retry. A successfully loaded report snapshot that lacks an endpoint is not a load failure and does not block removal.
2. If pair absent, publish absence with ALREADY_UNLINKED; preserve selection only if it now qualifies in suggestions, otherwise clear. No write.
3. Prepare state using current reports and links minus this pair. Call repository `unlink(pair)` without eligibility or qualification requirements.
4. True reports UNLINKED only after durable removal. Publish prepared state; if the pair currently qualifies, move it to SUGGESTIONS, keep selection and enable Link. Otherwise clear selection/private content and create no row.
5. False reports ALREADY_UNLINKED and reconciles the same absence; never claim a new removal. Failure leaves last-known linked state and permits retry. No post-commit storage reload is performed.

| Current fact / event | Service lifecycle |
| --- | --- |
| Qualifying, unlinked | One suggestion with score and reasons |
| Successful Link | Remove suggestion, add one linked row, preserve selected pair |
| Already-linked qualifying pair | Linked section only, current score and reasons |
| Linked but rule gates/threshold later fail | Retain linked row; show component failures, suppress total; Unlink remains |
| Linked but type/status becomes ineligible | Retain link; actual canonical fields and ineligible notice; no fabricated evaluation |
| Linked endpoint missing | Retain link, missing-side placeholder/Report ID; available side only; Unlink remains |
| Successful/repeated Unlink | Remove only pair; return to suggestions and preserve selection iff currently qualifies |
| Explicit Refresh | Rebuild from both current stores; preserve selection only in same section |
| Logout | Drop presentation and service snapshots; leave durable links intact |

## 13. Error model and privacy

`PossibleMatchStoreException.Reason` is CORRUPT_STORE, UNSUPPORTED_VERSION, READ_FAILURE, WRITE_FAILURE, or RESULT_TOO_LARGE. Messages are fixed and contain no raw cause, suppressed exception, path, file bytes, UUIDs or report values. Structural null/self-pair programmer errors are rejected before I/O with fixed messages; the service maps user-reachable invalid selections to INVALID_PAIR.

Service feedback uses nested typed categories rather than copied exception text:

| Category | State / action |
| --- | --- |
| REPORT_LOAD_FAILED | Enter/refresh unavailable; action precheck failed with last-known state |
| RELATIONSHIP_LOAD_FAILED | Same, never infer unlinked from unknown link state |
| EVALUATION_FAILED | Invalid snapshot / unexpected rule evaluation failure; no partial new state or mutation |
| LINK_FAILED / UNLINK_FAILED | Durable write failed; last-known state, no success, explicit retry |
| STALE_PAIR / INVALID_PAIR | Safe rejection and selection reconciliation, no mutation |
| LINKED / UNLINKED | Only after true committed mutation |
| ALREADY_LINKED / ALREADY_UNLINKED | Idempotent feedback, not new success |

Catch report-store checked failures at the service seam. Catch invalid-input and runtime evaluation exceptions only around snapshot evaluation/derivation, mapping to EVALUATION_FAILED without raw diagnostics; do not catch VM Errors or turn programming failures after commit into a purported rollback. There is no user-configurable matcher, so no separate configuration error is needed. All potentially failing evaluation happens before mutation.

Keep copy mapping in the plain-Java state/service so the UI cannot leak raw exceptions. Copy must distinguish the listed outcomes, preserve the PRD's meaning and possible-match terminology, and may be refined during UI delivery. Action failures label retained data as last-known and a retry repeats the whole operation; unknown current state never enables a write without recheck.

Reporter ID and private identifying detail are accessible for rendering only through the selected authenticated comparison. Public/private description controls are separate. Row strings, tooltips, reasons, accessibility text, errors, logs, screenshots and handoff artifacts must not include restricted values. Display report strings as plain JavaFX text, never HTML/markup. Clear reused cells on empty/reassignment. No object dump logging; tests use synthetic fixtures and safe assertion labels instead of printing sensitive fields.

## 14. Composition, authorization, and JavaFX

The only required Developer 1 edit is `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/FindersKeepersApp.java`. The smallest change constructs one application-lifetime `FilePossibleMatchRepository(Path.of("data", "possible-match-links.txt"))` alongside the existing shared report repository and changes only the lazy officer supplier to construct `DeskOfficerWorkspacePane(reportRepository, possibleMatchRepository)`. Imports change accordingly. Preserve Student factory wiring, report path, scene dimensions, stage lifecycle and smoke logic. Developer 1 can make this edit instead. Separate explicit cross-owner approval is required before implementation; approval of this TDD alone is not permission.

Constructing a path-only repository does not load matching data at startup. Only the existing authenticated DESK_OFFICER supplier creates services and panes and calls `enter`. `AuthenticationPane` and coordinator need no changes. The Student factory never receives the link repository or matcher. No second entry point, role enum, login flow, report model or authorization system.

`DeskOfficerWorkspacePane` contains two non-closable tabs: existing report review (default, preserving the current destination) and Possible matches. Both are constructed only inside the officer route; matching performs its initial load during that construction. Tab switching is presentation only, not automatic refresh. Officers use explicit Refresh after review changes; both supported statuses are eligible anyway. Existing review service/pane behavior remains intact.

`OfficerMatchingPane` uses the current programmatic JavaFX pattern:

- BorderPane header: possible-match title, explanatory rule-points notice, Refresh button and feedback/error region.
- Resizable split content: two distinctly labelled lists (suggestions and linked possible matches) and one comparison ScrollPane. Each list uses custom public row cells with the exact PRD fields; only one pair can be selected across both lists.
- Comparison: two read-only labelled groups side by side. Each available report shows all eleven canonical fields via direct accessors and existing enum/date/time formatters. Public description and officer-only private detail have distinct sections. At small window widths allow scrolling; do not hide required data or require a shell sizing change.
- Below comparison: four component outcomes/reasons, qualifying total only, and separate Link as Possible Match / Unlink Possible Match controls. Enablement comes solely from state. Missing sides show placeholders; linked rows never lose their Unlink action merely for lost qualification.
- Empty labels distinguish no eligible pair, no qualifying pair, no unlinked suggestion, and no links as derived by the service. Unavailable hides old lists/details and shows Retry. Action failures retain last-known state.
- A rendering guard suppresses selection callbacks while replacing lists and restoring the state-owned selection. Row selection dispatches IDs, never report objects or a UI-computed score.

Feature CSS lives in `src/main/resources/io/github/cs32272610mp2xcode/finderskeepers/matching/ui/matching.css` with matching-specific selectors loaded by the matching pane. No shared CSS or UI dependency change. Calls remain synchronous as in officer review, with no background job or subscription that can outlive logout.

Logout's existing subtree replacement removes all visible officer content. The workspace installs a view-local scene-detachment listener: once mounted, detachment clears matching lists/detail controls and calls service `clear()` to release report snapshots, evaluation data, selection and feedback. It writes no storage. No application/static cache retains a pane/service/state, and there are no global listeners to unregister. Fresh login creates fresh objects, loads both stores and restores no selection. This is lifecycle cleanup, not a new authorization policy or a guarantee of secure JVM heap erasure.

## 15. Technical sequence diagrams

Text diagrams follow the repository's current documentation convention. Arrows returning state carry immutable values, never persistence handles.

### Refresh / generate

```text
Officer UI       Matching service       ReportRepository    Link repository     Matcher
    | Refresh           |                     |                   |               |
    |------------------>| loadAll             |                   |               |
    |                   |-------------------->|                   |               |
    |                   |<-- canonical reports|                   |               |
    |                   | loadAll -------------------------------->|               |
    |                   |<-------------------- current pairs ------|               |
    |                   | generate(reports), evaluate stale links ----------------->|
    |                   |<---------------- immutable evidence / ordered qualifiers -|
    |                   | partition; keep every stored link; sort; reconcile selection
    |<-- READY state ---|                                                         |
    |                   |                                                         |
    |  Any load/evaluation failure: clear snapshots/private comparison,             |
    |<-- UNAVAILABLE; actions disabled; explicit Retry -----------------------------|
```

### Link

```text
Officer UI       Matching service       ReportRepository    Link repository     Matcher
    | Link selection    |                     |                   |               |
    |------------------>| loadAll ------------>|                   |               |
    |                   |<-- current reports --|                   |               |
    |                   | loadAll -------------------------------->|               |
    |                   |<---------------------- current pairs -----|               |
    |                   | evaluate current pair / derive state -------------------->|
    |                   |<----------------------- gates, points, qualification -----|
    |                   | already linked -> reconcile + ALREADY_LINKED, no write    |
    |                   | stale/unqualified -> reconcile + STALE_PAIR, no write     |
    |                   | valid new pair: prepare success state                    |
    |                   | link(pair) ----------------------------->|               |
    |                   |                     |            stage / force / atomic move
    |                   |<---------------- committed true ---------|               |
    |<-- selected LINKED state; Unlink enabled |                   |               |
    |                   | write failure -> retain last-known unlinked; retry        |
    |                   | no report write and no post-commit reload                |
```

### Unlink

```text
Officer UI       Matching service       ReportRepository    Link repository     Matcher
    | Unlink selection  |                     |                   |               |
    |------------------>| loadAll ------------>|                   |               |
    |                   |<-- reports (missing endpoint is allowed)  |               |
    |                   | loadAll -------------------------------->|               |
    |                   |<---------------------- current pairs -----|               |
    |                   | evaluate available pair / prepare next state ----------->|
    |                   |<----------------------------- current evidence -----------|
    |                   | absent -> ALREADY_UNLINKED, no write                      |
    |                   | present: unlink(pair) ------------------>|               |
    |                   |                     |            stage / force / atomic move
    |                   |<---------------- committed true ---------|               |
    |<-- UNLINKED; selected suggestion iff qualifying, else clear --|               |
    |                   | write failure -> retain last-known linked; retry          |
    |                   | no report write; other pairs untouched                    |
```

## 16. Focused data and dependency diagram

```text
FindersKeepersApp (existing; cross-owner change requires approval)
   | shared ReportRepository                    | PossibleMatchRepository
   |                                            | implemented by
   |                                            v
   |                                    FilePossibleMatchRepository
   |                                        | private codec helpers
   |                                        +--> PossibleMatchStoreFiles --> NIO
   |                                                      |
   |                                          possible-match-links.txt
   v
AuthenticationPane -- DESK_OFFICER supplier --> DeskOfficerWorkspacePane
                                                  |                 |
                                    existing ReviewPane       OfficerMatchingPane
                                                                    |
                                                          OfficerMatchingService
                                                           /         |         \
                                          ReportRepository     Matcher      Link repository
                                              |                   |
                                        canonical ItemReport      +--> Generation
                                        enums / UUIDs                   |
                                                               MatchEvaluation
                                                               /             \
                                                    PossibleMatchPair    four criterion results
                                                     (UUID, UUID)        points/reasons/tokens

Service output: MatchingWorkspaceState
   +-- ordered public PairRows (pair key + restricted summaries)
   +-- selected comparison only --> canonical ItemReports + evaluation
   +-- availability / feedback / derived action enablement

Persistence stores only PossibleMatchPair; neither it nor Matcher depends on JavaFX.
Only the service reads both stores; it never writes ReportRepository.
```

## 17. Proposed public test seams

These seams are proposed for owner approval with this TDD, not claimed already approved. No tests or RequirementsToTests document are created at this stage.

| Seam | Planned behavioral evidence |
| --- | --- |
| PossibleMatchPair public value contract | Canonical order, equality/hash, reversed identity, null/self rejection, independent pairs sharing endpoints |
| DeterministicMatcher generate/evaluate and immutable evaluations | Eligibility across four status combinations, each rule and Unicode boundaries, 0/7/8 and negative day gaps, gates versus raw totals, 50/70/80/100, exact reasons and token order, repeated and shuffled snapshots, stable UUID tie-break, excluded-field independence, duplicate-ID input rejection, immutable results |
| PossibleMatchRepository with real FilePossibleMatchRepository at @TempDir | Missing/header-only/zero-byte distinction, exact bytes, reverse canonicalization, corrupt/unsupported/duplicate/self cases, unknown IDs retained, fresh-instance Link/Unlink, repeated no-ops, non-exclusivity, bounded reads/writes and target preservation |
| OfficerMatchingService through returned state, with real report/link repositories | End-to-end suggestion partition, selection, durable Link/Unlink, qualifying/stale/missing links, selection migration, refresh retention, empty classifications, action enablement, unchanged report values and bytes |
| Existing report repository and new relationship repository seams with scripted fault adapters | Deterministic disappearance, type change, non-qualification, load/write failures, safe retry and no false success. No mocking matcher internals or UI controls. |
| Package-local filesystem seam, exercised through repository operations | Stage/force/close/move/unsupported-atomic/cleanup failures; old target remains authoritative, orphan ignored, no failure rewritten to empty. Real filesystem integration complements injected faults. |
| Existing authentication coordinator plus source/manual JavaFX evidence | DESK_OFFICER-only lazy construction, Student exclusion, logout detach/clear, fresh login, exact row fields, selected private boundaries, all eleven fields, reasons, long text, small window, correct buttons and retry |

Use independent literal expected values and synthetic IDs, not a second matcher inside tests. Test Unicode code-point length with supplementary letters, ROOT case behavior under a changed default locale (restore locale after the test), symbols in locations, one-character location tokens, separator-only equality, duplicate name tokens, singular/plural and substring exclusions. ID ties need IDs where String order differs from natural signed UUID comparison. Test all four components, including failures in retained links, without exposing a raw non-qualifying total in presentation state.

Service fixtures may change reports through the existing persistence contract between calls to simulate staleness; the matching service itself never writes them. Missing reports can be supplied via a scripted read seam; do not add a production delete method or future enum solely for tests. Existing two statuses are both eligible, so same-type changes exercise current ineligibility.

No public failure toggles, raw-state setters or test-only APIs. Package-local filesystem dependency injection follows the existing real I/O seam. Every persistence test uses isolated temporary storage, never application data. Regression suites cover canonical domain, report persistence, authentication, Student submission/history and officer review. Automated assertions use safe case labels and avoid dumping private values or full file contents.

## 18. File ownership and later integration

Paths below are relative to the repository root. Java paths use `src/main/java/io/github/cs32272610mp2xcode/finderskeepers/` as prefix.

| Proposed file/type | Owner | Purpose | Change | Separate cross-owner approval? |
| --- | --- | --- | --- | --- |
| `matching/model/PossibleMatchPair.java` | Developer 2 | One symmetric pair invariant | New | No |
| `matching/model/MatchEvaluation.java` | Developer 2 | Immutable components/reasons; nested criterion types | New | No |
| `matching/model/DeterministicMatcher.java` | Developer 2 | Pure policy, generation, ordering; nested Generation | New | No |
| `matching/persistence/PossibleMatchRepository.java` | Developer 2 | Three-operation durable pair interface | New | No |
| `matching/persistence/FilePossibleMatchRepository.java` | Developer 2 | Strict versioned codec and set mutation | New | No |
| `matching/persistence/PossibleMatchStoreException.java` | Developer 2 | Safe checked storage outcomes | New | No |
| `matching/persistence/PossibleMatchStoreFiles.java` | Developer 2 | Package-local filesystem seam; nested safe failure type | New | No |
| `matching/persistence/NioPossibleMatchStoreFiles.java` | Developer 2 | Bounded read and safe atomic replacement | New | No |
| `matching/application/OfficerMatchingService.java` | Developer 2 | Workflow and current-state reconciliation | New | No |
| `matching/application/MatchingWorkspaceState.java` | Developer 2 | Immutable state and nested presentation values | New | No |
| `matching/ui/OfficerMatchingPane.java` | Developer 2 | Thin comparison/list/actions presentation | New | No |
| `review/ui/DeskOfficerWorkspacePane.java` | Developer 2 | Compose existing review with matching | New | No |
| Matching UI resource `matching/ui/matching.css` under corresponding `src/main/resources` prefix | Developer 2 | Scoped layout/style | New | No |
| `FindersKeepersApp.java` | Developer 1 / shared composition | Construct link repository; replace officer supplier only | Modified later | **Yes; Developer 1 may make it** |
| `.gitignore` | Developer 2 storage integration | Add exact `/data/possible-match-links.txt` and `/data/.possible-match-links-*.tmp` exclusions | Modified later | No; no build/CI changes |
| Tests under corresponding matching packages | Developer 2 | Behavioral evidence at approved seams | New later | No; test plan and implementation authorization still required |
| `docs/DeveloperGuide.md`, officer sections of `docs/UserGuide.md` | Developer 2 | Algorithm, storage, diagrams, officer instructions and interaction summary | Modified later | No; do not edit Student-owned sections |
| `docs/features/S2-D2-01-02/RequirementsToTests.md` | Developer 2 | Separate acceptance-evidence plan | New later | Separate planning request, not created here |
| `docs/features/S2-D2-01-02/TDD.md` | Developer 2 | This combined design | New now | No |
| `docs/features/S2-D2-01-02/GrillingDecisions.md` | Developer 2 planning | Owner-authorized GD-009 reconciliation and dated record | Reconciled in this conversation | Explicitly authorized by owner |

All canonical report files, Student files, report-store implementation/schema, AuthenticationPane/coordinator, existing review files, shared stylesheet, Gradle/CI/release files remain unchanged by the proposal. Do not broaden the shell edit under the earlier S1 integration approval: S2 requires its own explicit authorization. The only planned shared-file edit beyond shell is the Developer 2-owned data ignore entry, not a new storage path for reports.

## 19. Dependency-aware delivery plan

After TDD approval, separately prepare and approve RequirementsToTests, then obtain branch, implementation and shell-integration authorization. Recheck the baseline contains existing review and Student integration before delivery.

1. Pair identity and first pure matching slice: one behavior test, minimum implementation; extend through eligibility, four rules, reasons, gates and total ordering. Do not batch all imagined tests before implementing.
2. Durable pair slice: real temporary repository, first Link and fresh-instance load; extend through Unlink, idempotence, shared endpoints, format and faults.
3. Service read slice: real reports plus links -> two ordered sections, empty versus unavailable, selected canonical comparison and privacy-safe rows.
4. Service mutation slices: current Link recheck and selected-state movement; then Unlink, stale/missing cases, failures and retry, preserving report bytes.
5. Thin JavaFX slice: lists, comparison, reasons, actions, safe copy and scoped style; manual/source review for rendering and session lifetime.
6. Compose the officer tabs and, only after separate approval, integrate the minimal FindersKeepersApp supplier/storage edit. Verify Student isolation, review regression, initial matching load and fresh login.
7. Deliver Developer Guide algorithm/rationale/ordering/storage/sequence, officer User Guide instructions, workflow diagram and interaction summary. Document deterministic rules rather than probability/ML because evidence must be reproducible and attributable to the approved four inputs. Change existing future-work wording only after behavior is delivered and verified.
8. Run focused tests, `gradlew.bat check`, `gradlew.bat release`, and packaged smoke/manual workflow verification because composition/demo behavior changes. Check privacy-safe evidence, data ignore rules, full diff and status. Record failures or unverified manual steps truthfully; do not claim release-ready until required evidence passes. Commits/publishing remain separately gated.

## 20. Risks, controls and focused architectural self-review

| Risk / review question | Resolved control |
| --- | --- |
| Unnecessary abstraction | One concrete matcher, one stateful service and one repository interface; nested fixed value types; no rule registry, generic workflow or extra coordinator/controller |
| Duplicated matching logic | One evaluate operation drives generation, linked reevaluation and Link revalidation; reasons derive from immutable results |
| Shadow reports / report-store coupling | Consume ItemReport directly; row summaries are UI-only projections; pair store has UUIDs only; never extend report v1 |
| Hidden mutation | No report writes in matching; plain-Java integration verifies complete report equality and unchanged bytes |
| Nondeterministic ties/reasons | Explicit score plus normalized two-UUID string tuple; fixed component order; sorted unique tokens |
| Reverse links / exclusivity drift | One canonical pair factory for engine, service, codec and repository; set uniqueness; no endpoint uniqueness constraint |
| Stale state / qualification drift | Current reads and matching before every Link; never trust UI evidence; refresh reconciliation and selected-state rules explicit |
| Missing report / relationship divergence | Preserve stored pair and expose stale unlink; no automatic cleanup, inferred report or cached missing-side data |
| Corrupt store mistaken for empty | Only missing or valid header-only means empty; all structural failures reject whole store and block mutations |
| Partial writes / false success | Full candidate staged and forced; atomic replace only; no post-commit fallible reload; retained last-known state on failure |
| Private-data leakage | Public row projection, selected canonical references only, redacted value strings, fixed errors and local cleanup; no logs of model values |
| JavaFX test dependence | All policy/state operations available through plain Java seams; manual/source evidence limited to actual rendered behavior |
| Hidden shell ownership change | Exact FindersKeepersApp edit identified and separately gated; Developer 1 can implement it |
| Unbounded feature growth | No new dependencies, background work, generalized storage framework, claims or report status; local synchronous scale is recorded |

The focused self-review resolved technical edge cases directly: normalized empty location equality is preserved; unknown IDs remain removable; stale linked type changes are not mislabelled LOST/FOUND; all-qualifying-pairs-linked is not confused with no qualifying pair; success does not depend on a second read after commit; the latest owner request supersedes historical TDD drafting authorization metadata without rewriting approvals. No product change is introduced and no unresolved technical choice remains for the implementation agent. Filesystem/runtime limitations are explicit assumptions, not guarantees of multi-writer or arbitrary power-loss safety.

## 21. Completeness and design traceability

This table maps approved PRD acceptance criteria to technical realization, without starting the separate RequirementsToTests artifact. Mission Brief IDs have their own numbering; the rows below refer specifically to the PRD.

| PRD acceptance criteria | Technical realization |
| --- | --- |
| ME-AC-001, ME-AC-002 | Sections 4 and 6: UUID uniqueness, LOST x FOUND generation, explicit status allowlist |
| ME-AC-003, ME-AC-004, ME-AC-005, ME-AC-006, ME-AC-007, ME-AC-008 | Section 4: only four inputs, exact Unicode/ROOT policy, enum/date gates, weights and threshold |
| ME-AC-009, ME-AC-010 | Sections 4–5: derived points and reasons, immutable component accounting |
| ME-AC-011, ME-AC-012 | Section 6: total tuple comparator and deterministic token/component order |
| ME-AC-013 | Sections 4, 7, 10–12: read-only matching; relationship-only mutations |
| ME-AC-014 | Sections 9–10, 13: explicit empty facts versus unavailable/failure categories |
| ME-AC-015 | Sections 5, 13–14, 19: possible-match/rule-point semantics and later guide evidence |
| OW-AC-001 | Section 14: existing route authority, lazy officer construction, unchanged Student factory |
| OW-AC-002 | Sections 6, 10, 14: authoritative partition/order and exact public row projection |
| OW-AC-003, OW-AC-004 | Sections 5, 10, 13–14: selected canonical eleven-field comparison, evidence and private separation |
| OW-AC-005, OW-AC-006, OW-AC-007 | Sections 6–11: explicit symmetric Link, durable success, no-op repeats and authoritative stale rejection |
| OW-AC-008 | Sections 6–7, 11: pair uniqueness without report exclusivity |
| OW-AC-009 | Sections 6, 10, 12: linked retention/order, hidden non-qualifying total, missing IDs and Unlink access |
| OW-AC-010, OW-AC-011 | Sections 7–9, 12: durable removal, repeated absence, stale Unlink and selected-state rules |
| OW-AC-012, OW-AC-013 | Sections 9–13: atomicity, last-known failure state, independent empties, typed errors and explicit retry |
| OW-AC-014 | Section 14: subtree removal, local clear, fresh per-login load; durable links survive |
| OW-AC-015 | Sections 1, 5, 11–14, 19: no ownership implication in actions, results or later guides |

Ledger coverage: GD-001–GD-021 map to sections 4–6; GD-022–GD-025 to sections 10, 13–14; GD-026–GD-039 to sections 7–12; GD-040–GD-044 to sections 10, 13–14; GD-045–GD-047 to sections 1, 5 and 19. The Mission Brief's read-only, durable, authenticated, non-exclusive workflow and documentation obligations are preserved. SC-001/SC-004 follow Link; SC-002 follows Unlink; SC-003/SC-005 follow empty/failure paths; SC-006 follows session cleanup.

Completeness review: current actual contracts inspected; eligibility and all rules fixed; evidence, total ordering, storage grammar, pair identity, atomicity, recovery, errors, session lifetime, UI composition, seams and ownership decided; three operation sequences and a focused dependency diagram included. There is no unresolved product or technical blocker to owner review. Later shell approval is an implementation gate, not an unowned design gap.

## 22. Approval record

The repository owner explicitly approved this complete TDD on 2026-09-21. This approval confirms the matching, relationship persistence, ordering, service, JavaFX, test-seam, ownership, and cross-owner integration design recorded here. It does not authorize `RequirementsToTests.md`, branch creation, tests, production implementation, the Developer 1-owned shell edit, dependencies, commits, pushes, pull requests, release changes, or merging. A requested change to observable product behavior returns to the approved PRD first.

This TDD does not claim implemented or verified feature behavior. No production, test, CI, release or guide-delivery changes were made for this design; no build or tests were run for this planning-only transition. `RequirementsToTests.md` is the next planning artifact once the owner requests that stage.
