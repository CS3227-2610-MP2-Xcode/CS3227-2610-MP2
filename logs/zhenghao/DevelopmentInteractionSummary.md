# Development Interaction Summary

## Purpose

This document summarizes how ZhengHao used Codex while developing Finders Keepers. It records the main development themes, human decisions, verification boundaries, and repository outcomes. It is not a raw transcript and does not expose model reasoning.

Verification status: pending ZhengHao's comparison with the original sessions.

## Product definition and delivery planning

Codex was first used as a product-planning partner. Several two-role application ideas were compared before Finders Keepers was selected. The human developer fixed the core roles, changed the setting to a primary school, renamed the public role to Student, and required the work to be organized into short sprints with documentation included throughout.

The interaction established conventions that continued through the project: plan before implementation, keep branch names descriptive, separate commits by concern, review delegated work, and distinguish automated checks from human GUI verification.

## Build scaffold and application shell

The first implementation slice created the Java 25 Gradle quality scaffold, JavaFX shell, project guides, interaction logs, multi-platform CI, and an initial packaged JAR. Codex helped translate the approved plan into bounded implementation tasks and verification commands. Git history records the scaffold, shell, documentation, CI, and release artifact as separate commits on 15 September.

## Report domain and Student workflows

Codex helped plan the shared Item Report contract so Developer 1's domain work and Developer 2's persistence work could converge on one model. The resulting work added an immutable report domain, validation tests, Student submission, persistence integration guidance, and a report-history/search view.

A later integration check identified that a green build would not by itself prevent competing report models. The human developer therefore required the canonical contract to be reconciled before continuing. Search behavior was explicitly decided to be case-insensitive, newest-first, and backed by the shared JSON report store.

Git evidence includes separate feature, test, and documentation commits for the domain and submission slices, followed by fixes for restoring stored text, Student-role workspace routing, and report-history integration.

## Review automation

Codex guided Greptile setup and investigated reviews that were counted by the service but skipped or not visible on GitHub. A lightweight project context and review configuration were added. The interaction also established an important practice: an automated review finding had to be reproduced or checked against the actual contract before a code change was made.

## Appointment and custody lifecycle

The appointment feature was developed from an explicit handoff plan. The human developer chose 30-minute Singapore-time slots, one collection desk, repeat booking after cancellation or no-show, a `NO_SHOW` state, visible actor identity in the officer audit view, and a high coverage target.

Codex was used to implement or review booking, cancellation, no-show, collection, storage, return, close-case, audit, and persistence behavior. It also produced manual role-by-role test sequences. Review-driven corrections covered cross-process booking races, malformed or incomplete persisted state, custody being reversed by a storage-location correction, appointment lists collapsing in a small window, collection time disappearing from active rows, earlier attempts missing from history, selected claim references becoming unreadable, and audit rows showing the wrong actor role.

The repository history shows production, test, and documentation commits for the base workflow, followed by narrowly scoped fixes and regression documentation. The appointment system was merged through pull request #14, and its UI improvements through pull request #15.

## JavaFX usability and authentication

Codex helped improve the application shell and feature workspaces without intentionally changing domain behavior. Appointment actions were redesigned to remain discoverable, with invalid operations explained through user-facing alerts. Input preservation and small-window behavior were reviewed separately from service logic.

Authentication work added production and demo modes, local Student and Desk Officer registration, and one-click demo entry points. The human developer chose production mode as the default and requested bundled demo accounts for marker convenience. Tests covered authentication-pane interactions, and documentation explained account creation and mode switching.

A later visual pass introduced a colourful Student-facing theme and mascot, added the mascot to the login screen, and refreshed workflow and README screenshots. These changes were kept separate from core authentication, persistence, and report behavior.

## Documentation, Javadocs, and assignment audit

Codex helped create role-specific user workflows, developer guidance, sequence diagrams, screenshots, in-app appointment help, and a concise project README. Review feedback corrected the User Guide so the report-review page was described as read-only rather than claiming controls that did not exist. Project-wide Javadocs were added as a separate documentation task.

The final assignment audit was deliberately read-only and separated confirmed repository evidence from runtime, visual, and remote checks. This avoided treating source inspection as proof that the packaged application or hosted services worked.

## Tests and continuous delivery

Codex was used to plan higher unit and interaction coverage for appointment boundaries and multiple-account scenarios while preserving the existing product rules. The developer required work to stop if testing exposed a behavior that needed a product decision.

Continuous delivery work added GitHub Pages and tagged-release paths alongside builds, tests, smoke tests, and temporary workflow artifacts. The interaction explicitly identified that GitHub repository settings and remote workflow runs require human or live-service verification. Pushing a tag was not treated as sufficient proof that a GitHub Release contained the latest JAR; the tag target, workflow result, and release asset must all be checked.

## How Codex was used

- Planning: turn assignment requirements and product decisions into bounded implementation plans.
- Implementation: generate routine code and documentation after plan approval, sometimes through a cheaper model.
- Review: inspect delegated changes, verify automated-review claims, and propose focused corrections.
- Testing: add unit and JavaFX interaction tests, interpret failures, and provide manual role-based test procedures.
- Documentation: maintain user, developer, Javadoc, README, workflow, and interaction evidence.
- Git workflow: prepare concern-separated commits and pull requests when explicitly requested.

## Human oversight and verification boundary

The human developer retained decisions about product semantics, merge timing, UI acceptance, repository settings, and publication. Codex output was reviewed before commits or pushes where requested. Local tests, packaged-JAR checks, signed-in GUI checks, and GitHub-hosted workflow results were reported as separate forms of evidence.

This summary was reconstructed from the available Codex project history and Git history on 28 September 2026. It may omit conversations held outside that project or sessions deleted before preparation. The dated files in this folder provide more detailed evidence for the early development slices.
