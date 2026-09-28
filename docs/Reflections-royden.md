# Finders Keepers Reflections

This document records observations about using agentic software engineering while building Finders Keepers for primary schools. Draft entries are marked until a developer verifies them. It will be expanded to cover at least three substantial skills before submission.

## 2026-09-15 — S1-D1-01 project setup (draft for human review)

Codex handled the overall scaffold and validation while two lower-cost Luna workers drafted bounded, routine parts: one handled documentation placeholders and the other handled CI and style configuration. Giving each worker an explicit file boundary avoided overlapping edits and left the main agent responsible for integration.

The first validation run exposed two useful failures: Checkstyle could not resolve its suppression file, and Javadoc rejected the JavaFX application's implicit constructor. A second run then exposed unsupported and stylistic Checkstyle rules. Fixing those issues before handoff showed that generated configuration still requires execution-based verification; XML parsing alone had not proved that Checkstyle could load and apply the configuration.

The final local gate compiled with Java 25, passed JUnit and Checkstyle, generated Javadoc and JaCoCo output, verified the cross-platform JAR contents, and smoke-tested both the Gradle launch path and the packaged release JAR. GitHub-hosted CI remains unverified until the repository is pushed. This entry is intentionally marked as a draft until the developer checks it against the session transcript.

## Reflection prompts

When a milestone is completed, record concise, evidence-based notes under the relevant heading:

- What was attempted?
- What worked, and how was it verified?
- What was difficult or unclear?
- What would be changed in the next iteration?
- Which user or developer feedback affected the design?

## Future entries

Add dated entries as work progresses. Separate observed results from plans, assumptions, and unresolved questions.
