# ZhengHao's Contributions

This summary is organised by the branch used for each contribution. It was
prepared from ZhengHao's interaction logs and the corresponding Git history;
merged pull-request numbers are included where available.

## 1. Initial project scaffold and school context — `master`

ZhengHao created the Java 25 and JavaFX Gradle baseline, quality checks,
cross-platform packaging, CI configuration, smoke-test paths, and initial
project documentation. He also changed the product from a general community
lost-and-found application to a primary-school system with Student and Desk
Officer roles.

## 2. Greptile review integration — `zh/chore-greptile-review-config` (PR #3)

ZhengHao configured Greptile's pull-request review behavior and added
repository-specific project context. He also investigated skipped or
unpublished reviews and established that automated findings must be verified
before code is changed.

## 3. Item Report domain — `zh/feat-item-report-domain` (PR #5)

ZhengHao implemented the immutable `ItemReport` domain, creation and restoration
contracts, report enums, validation rules, privacy-safe summaries, and
persistence-facing parsing methods. He added comprehensive domain tests and
documented the canonical contract used by the UI and JSON repository.

## 4. Student report submission — `zh/feat-student-report-submission` (PR #6)

ZhengHao implemented the Student report-submission service, controller, form
state, authenticated reporter handling, field-specific validation feedback, and
safe storage-failure messages. He added focused submission tests and documented
how the feature should integrate with authentication and persistence.

## 5. Student report history and search — `zh/feat-student-report-history` (PR #10)

ZhengHao implemented a personal report-history view with readable statuses,
newest-first ordering, case-insensitive search over public fields, and distinct
empty-history and empty-search states. He connected the authenticated Student
workspace to the shared JSON report repository and added tests and user guidance.

## 6. Appointment and custody system — `zh/feat-appointment-system` (PR #14)

ZhengHao implemented Student appointment booking and cancellation together with
Desk Officer slot, storage, collection, no-show, return, close-case, audit, and
persistence workflows. He added automated and manual workflow tests and fixed
cross-process races, custody restoration, history, scheduling, selection, and
audit-label defects found during review.

## 7. Appointment UI improvements — `zh/feat-appointment-ui-improvements` (PR #15)

ZhengHao made the appointment screens usable at smaller window sizes and added
context-aware controls, structured slot dialogs, readable claim selections, and
user-facing validation alerts. He also preserved unfinished officer input and
clarified appointment identifiers and workflow guidance.

## 8. Application-wide UI refresh — `zh/feat-app-ui-refresh` (PR #16)

ZhengHao created a cohesive native JavaFX and CSS design system for the login
shell, navigation, cards, forms, lists, buttons, and feedback states. He applied
the theme across reports, appointments, claims, matching, and review screens
without intentionally changing domain or persistence behavior.

## 9. Login modes and registration — `zh/feat-login-registration` (PR #17)

ZhengHao implemented local Student and Desk Officer registration, production and
demo login modes, and one-click demo-role entry points. He made production mode
the default and added authentication interaction tests, Javadocs, and user
documentation.

## 10. User and developer workflow guides — `zh/docs-user-developer-appointment-guides` (PR #18)

ZhengHao added role-specific User Guide and Developer Guide workflows, diagrams,
screenshots, in-app appointment help, and tests for rendered help content and
shared found-item visibility. He also bundled default demo accounts, strengthened
packaged-artifact validation, and corrected the report-review documentation to
describe its read-only behavior.

## 11. Project Javadocs — `zh/doc-project-javadocs`

ZhengHao added concise, human-readable Javadocs across the project's public
production APIs and relevant test support. The completed Javadocs were later
integrated into `master` through the Student cartoon UI branch.

## 12. Student mascot and cartoon UI — `zh/feat-student-cartoon-ui` (PR #20)

ZhengHao added a colourful primary-school theme and original bear mascot to the
Student and login experiences. He regenerated and visually verified the affected
User Guide and README screenshots while keeping core application behavior
unchanged.

## 13. Appointment account and slot coverage — `zh/test-appointment-account-coverage`

ZhengHao added tests for appointment-slot input boundaries, sequential Student
account isolation, and sequential Desk Officer handoff behavior. He documented
the corresponding account and slot rules; this branch remains pending merge.

## 14. GitHub Pages and release delivery — `zh/ci-cd-pages-release` (PR #21)

ZhengHao added continuous delivery for quality checks, smoke tests, temporary
workflow artifacts, GitHub Pages deployment, and tagged GitHub Release
publication. He also documented the human repository settings and remote checks
needed to verify the website and downloadable release JAR.

## 15. AI interaction evidence and reflection — `zh/docs-interaction-logs`

ZhengHao reorganised dated AI interaction records into contributor-owned folders
and added MP1-style summaries of his Codex-assisted development sessions. He
also documented his individual contributions and reflected on Greptile review,
visual verification, chat forking, atomic commits, and structured prompting.
