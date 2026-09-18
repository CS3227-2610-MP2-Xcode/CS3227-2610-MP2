# S1-D2-01 Authentication Cleanup — 2026-09-18

## Goal

Clean up the first local-authentication implementation before application-shell
integration. The work separates packages by responsibility, replaces the
controller/session split with a coordinator, hardens version 1 credential-store
validation, and aligns the project documentation with the resulting design.

## Process record

The original authentication implementation began without completing the future
feature gates for grilling, an approved product requirements document, and an
approved technical design document. This cleanup records that deviation
honestly. No retrospective artifacts were created to suggest those gates had
already occurred.

This remediation is the sole named legacy exception. It is authorized by the
approved S1-D2-01 mission brief, the approved cleanup plan, and the explicit
implementation request. Future Developer 2 feature delivery must follow the
repository delivery skill and stop when a required artifact or approval is
missing or contradictory.

## Boundaries

- Keep credential-store schema version 1 and the existing demo store unchanged.
- Leave the Developer 1-owned startup shell, Gradle and CI configuration,
  committed release JAR, and demo-store packaging unchanged.
- Do not commit, push, or perform cross-owner integration as part of the cleanup.
- Do not record public demo passwords or credential material in this log.

## Verification

- Verifier: automated repository checks completed; human UI review pending
- Verification: startup and packaged-JAR smoke checks passed; manual UI checks pending
- Status: integrated; needs manual UI follow-up
- Automated implementation checks: 54 focused authentication test invocations,
  production and test Checkstyle, and Javadoc passed on 2026-09-18. The final
  clean repository check and delivery-skill validation are reported in the
  implementation handoff; they do not replace the pending human review.
- Shell integration, the full repository check, release packaging, and the
  packaged-JAR smoke launch passed on 2026-09-18. Coordinator tests are not UI
  coverage.
- Manual UI checks remain: verify password masking, **Clear**, failed-password
  retention, both role route titles and descriptions, and logout rendering.
- Follow-up: complete and record the human UI review, then decide how the
  external demo credential store is supplied with a distributable release.
