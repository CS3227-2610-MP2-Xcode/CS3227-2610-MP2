# AI interaction summary: Finders Keepers development

Date: 2026-09-28
Verification status: Pending final student review

## User goal

Develop Finders Keepers as a production-level Java SE 25 and JavaFX desktop
application for a primary school. Students report lost and found items, review
matches, submit claims, and book collection appointments. Desk Officers review
reports, link possible matches, verify claims, manage custody, and complete item
returns.

## Product and engineering decisions

- Selected Finders Keepers after comparing several two-role application ideas.
- Changed the setting to a primary school and renamed the public role to
  Student while retaining Desk Officer as the operational role.
- Used local authentication rather than Keycloak for the assignment scope.
- Organised the work into short sprints with documentation included throughout
  development.
- Required plans before significant implementation and separate commits for
  production code, tests, documentation, and build changes.
- Used lower-cost agents for routine implementation where useful, followed by
  an independent code-quality review in the main Codex session.

## Initial scaffold and application shell

- Bootstrapped the Java 25 Gradle quality scaffold and JavaFX application shell.
- Added project guides, interaction evidence, multi-platform CI configuration,
  and an initial packaged application artifact.
- Adopted descriptive branches and separation-of-concern commits so the Git
  history remained reviewable.

## Report workflow implementation

- Planned one canonical immutable Item Report domain shared by UI and JSON
  persistence work.
- Added report validation, Student lost/found submission, and persistence
  integration guidance.
- Added Student report history and case-insensitive search over item name and
  public description, ordered newest first.
- Connected report features to the authenticated Student workspace and retained
  `data/reports.json` as the shared persistence boundary.
- Reconciled concurrent work to avoid duplicate report-model contracts that a
  green build alone would not have detected.

## Review automation follow-up

- Configured Greptile repository review behavior and added lightweight project
  context.
- Investigated reviews that appeared in Greptile but were skipped or not shown
  as GitHub comments.
- Treated each automated finding as a hypothesis to reproduce or verify before
  changing production code.

## Appointment and custody workflow

- Chose one collection desk, 30-minute slots in `Asia/Singapore`, repeat booking
  after cancellation or no-show, and a dedicated `NO_SHOW` state.
- Implemented Student booking and cancellation together with Desk Officer slot,
  storage, collection, no-show, return, close-case, and audit actions.
- Added role-by-role manual test sequences and automated workflow tests.
- Fixed cross-process booking races and persisted-state defects found during
  verification.
- Preserved custody state when correcting a storage location after collection.
- Kept appointment lists usable at the startup window size and displayed the
  scheduled time in active appointment rows.
- Preserved every booking attempt in Student history, kept the selected claim
  reference visible, and displayed the event's recorded actor role in audit
  rows.

## JavaFX and authentication follow-up

- Enlarged and restyled the application shell without intentionally changing
  authentication, persistence, or domain behavior.
- Improved appointment controls, contextual errors, and preservation of
  unfinished officer input.
- Added production and demo login modes, local Student and Desk Officer account
  creation, and one-click demo-role entry points.
- Made production login the default while bundling demo accounts for marker
  convenience in a fresh packaged application.
- Added a colourful Student-facing theme and mascot, extended the mascot to the
  login screen, and refreshed the affected screenshots.

## Documentation and quality follow-up

- Added role-specific user workflows, developer guidance, sequence diagrams,
  screenshots, in-app appointment help, and project-wide Javadocs.
- Corrected the User Guide so report review is described as read-only rather
  than claiming status controls that do not exist.
- Expanded unit and JavaFX interaction coverage for authentication,
  appointments, multiple accounts, persistence boundaries, and shared found
  item visibility.
- Required testing work to preserve established product rules and stop for a
  human decision if a newly discovered behavior required a semantic change.

## Continuous delivery and release follow-up

- Added builds, tests, smoke tests, temporary artifacts, GitHub Pages, and
  tagged GitHub Release publication to the delivery workflow.
- Kept repository settings and live GitHub workflow observations as explicit
  human verification steps.
- Did not treat source inspection, a local packaged JAR, or a pushed tag as
  interchangeable evidence.

## Verification approach

- Reviewed plans before implementation and manually checked important JavaFX
  workflows after automated tests.
- Verified review comments against the current branch and domain contracts.
- Kept local checks, packaged-artifact checks, signed-in GUI checks, and remote
  GitHub results as separate evidence.
- Used Git history to confirm implemented themes and concern-separated changes
  rather than relying only on conversational claims.

## Remaining verification

- Student must review this summary and correct any inaccurate or incomplete
  statement before submission.
- Confirm the final GitHub Pages URL and tagged release asset through GitHub.
- Complete any outstanding cross-platform or signed-in visual checks that were
  not directly observed in the recorded Codex sessions.
