# Prompt Summary

## Metadata

- Project: Finders Keepers — CS3227 MP2
- Developer represented: ZhengHao
- AI tool: OpenAI Codex desktop application
- Period represented: 15–28 September 2026
- Prepared from: user-authored messages in the Codex project `cs3227 mp2`, existing dated interaction logs, and matching Git history
- Verification: pending ZhengHao's comparison with the original sessions

## Scope and method

This is a concise, chronological paraphrase of ZhengHao's development prompts. Repeated retries, pasted copies of the same request, credentials, and internal model reasoning are omitted. Closely related follow-ups are combined, while decisions that changed product behavior, scope, validation, or Git workflow are retained. The source conversations remain the authoritative transcripts.

## Chronological prompt summary

### 15 September — product direction, planning, and scaffold

1. Brainstorm production-level application ideas with two clearly separated user roles and select an idea suitable for the assignment.
2. Develop the selected Finders Keepers concept around a Community Member and Desk Officer, then rewrite the proposal into a human-readable one-page discussion document.
3. Break the project into three-day sprints through 28 September, explain the task identifiers, and include documentation work in each sprint rather than leaving it to the end.
4. Clarify shared ownership of the report repository and evaluate whether Keycloak was necessary; retain local authentication for the project scope.
5. Plan the S1-D1-01 scaffold before implementation, use a cheaper model for routine implementation where appropriate, and stop for manual review before committing or pushing.
6. Establish branch naming and separation-of-concern commit conventions.
7. Change the product context to a primary school and rename the Community Member role to Student.

### 19–20 September — review tooling and report features

8. Configure Greptile automatic pull-request review and diagnose reviews that appeared in Greptile but were skipped or not published as GitHub comments.
9. Add lightweight repository-specific review context, review the result, and commit the configuration by concern.
10. Plan the immutable Item Report domain while accounting for Developer 2's persistence dependency and existing pull requests.
11. Implement the report domain with tests and documentation, run the Gradle quality gate manually, and keep production, tests, and documentation in separate commits.
12. Reconcile the Item Report contract with concurrent work so both developers used one canonical domain model rather than duplicate types.
13. Implement Student lost/found report submission, add feature-level tests and documentation, explain how to exercise the feature manually, and prepare the pull request.
14. Plan Student report history and search using case-insensitive text matching, newest-first ordering, the shared `data/reports.json` store, and the existing status model.
15. Connect the report-history UI to the authenticated Student workspace, rebase it on the latest shared branch, review the result, and provide exact manual verification steps.

### 25–26 September — appointment and custody workflow

16. Inspect only feature-adjacent files and design a detailed appointment handoff for a cheaper implementation model.
17. Decide on one collection counter, 30-minute slots in `Asia/Singapore`, a `NO_SHOW` state, repeat booking after cancellation or no-show, visible officer identity in audit records, and a 90% coverage target.
18. Implement the appointment flow, review the delegated code for quality and Javadocs, add tests where practical, and commit by concern.
19. Request step-by-step manual testing for Student and Desk Officer actions, then diagnose missing claims, appointment creation errors, time-gated actions, and custody-state failures.
20. Add a pull-request workflow diagram distinguishing Student and Desk Officer actions.
21. Validate review findings before fixing them, including cross-process booking races and JSON persistence behavior.
22. Fix verified defects involving custody after storage correction, unusable lists at the startup window size, missing collection times, incomplete appointment-attempt history, unreadable selected claim references, and incorrect audit actor labels.
23. Provide the exact positive-case sequence for an end-to-end appointment and item-return test.

### 28 September — usability, authentication, documentation, and release work

24. Improve appointment screens at smaller window sizes and show actions according to workflow context without changing core appointment logic.
25. Keep time-gated actions clickable but report invalid prerequisites through clear alerts; preserve unsubmitted input when screens refresh.
26. Refresh the wider JavaFX application shell and feature styling while preserving authentication, persistence, and domain behavior.
27. Add production and demo login modes, local Student/Officer registration, and two one-click demo-role entry points; make production mode the default.
28. Add authentication Javadocs and interaction tests, and verify review findings before changing Linux CI behavior.
29. Produce assignment-oriented user and developer guides with workflow diagrams, screenshots, role-specific instructions, and a concise README patterned after the MP1 project.
30. Correct documentation that described nonexistent report-status controls and guarantee that bundled demo accounts remain available in a fresh packaged application.
31. Add concise, human-readable Javadocs across the project.
32. Make Student-facing screens more colourful and age-appropriate, add a mascot to Student and login screens, and refresh the affected screenshots.
33. Increase test coverage without changing core logic, emphasizing appointment input boundaries and multiple-account cases; stop for a product decision if a behavioral defect is discovered.
34. Audit the assignment requirements and report confirmed gaps separately from items requiring runtime, visual, or remote verification.
35. Add continuous delivery for builds, tests, smoke tests, temporary artifacts, GitHub Pages, and tagged GitHub Releases; identify required human repository settings.
36. Ask for exact GitHub Pages activation steps, then verify whether pushing `v1.0.0` alone proved that the latest release JAR had been published.
37. Consolidate the interaction evidence into contributor-owned log folders and create MP1-style prompt and development summaries.

## Recurring human decisions and controls

- Plans were requested before significant implementation so architecture and product choices could be reviewed first.
- Repository inspection was repeatedly constrained to feature-relevant files to control token use.
- Delegated implementation was followed by a separate quality review rather than accepted automatically.
- Review comments were treated as hypotheses until reproduced or verified against the code.
- Production, tests, documentation, and build changes were normally committed as separate concerns.
- Manual GUI, packaged-JAR, GitHub Pages, and GitHub Release checks were kept distinct from local source and unit-test results.
- No prompt summary is evidence that a workflow passed; outcomes must be supported by tests, observed UI behavior, Git history, or remote workflow results.
