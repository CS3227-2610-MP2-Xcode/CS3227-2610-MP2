# AI interaction summary: Finders Keepers final audit and delivery session

Date: 2026-09-28
Verification status: Pending final student review

## User goals

- Audit the project against the supplied CS3227 MP2 requirements without
  reading unrelated repository content.
- Add continuous delivery for builds, tests, smoke tests, temporary artifacts,
  GitHub Pages, and tagged GitHub Releases.
- Identify human actions required to activate GitHub Pages and publish a
  release.
- Reconstruct the student's Codex usage as concise, submission-ready
  interaction summaries.
- Organise interaction evidence by developer under `logs/zhenghao/` and
  `logs/royden/`.

## Prompting and planning approach

- Explicitly restricted repository inspection to feature-relevant files and
  excluded mission briefs, logs, agent configuration, and Greptile context
  unless the current task directly required them.
- Supplied the assignment description and requested a gap audit before asking
  for delivery changes.
- Required confirmed gaps to be separated from runtime, visual, and remote
  checks that still needed human verification.
- Requested a new branch from `master` where needed and retained manual control
  over GitHub repository settings.

## Assignment audit summary

- Compared the assignment requirements with the relevant feature code, tests,
  build configuration, user guide, developer guide, reflection, and README.
- Kept source inspection distinct from launching the packaged application,
  visually checking JavaFX screens, and observing remote GitHub workflows.
- Identified continuous delivery and a deployed project website as remaining
  delivery work rather than treating the existing CI build as sufficient.

## Continuous-delivery follow-up

- Added a workflow path for quality checks, smoke testing, temporary workflow
  artifacts, GitHub Pages deployment, and tagged GitHub Release publication.
- Added the project website content and documented the repository settings that
  a maintainer must enable in GitHub Pages.
- Explained that the Pages source must be set to GitHub Actions after the
  workflow reaches the default branch.
- Explained that a release tag must point to the intended commit and that a
  successful release workflow must attach the packaged JAR before publication
  can be considered verified.

## Tag and release verification boundary

- The student updated `master`, created annotated tag `v1.0.0`, and pushed the
  tag to GitHub.
- The interaction did not treat the tag push alone as proof that the latest JAR
  appeared in a GitHub Release.
- Final verification requires checking the tag target, the corresponding
  GitHub Actions run, the release entry, and the downloaded release asset.

## Interaction-log reconstruction

- Reviewed the user-authored messages available in the Codex project
  `cs3227 mp2` and matched the main development themes against Git history.
- Summarised decisions and outcomes instead of copying raw transcripts or
  exposing internal model reasoning.
- Used Git authorship to place five existing ZhengHao records under
  `logs/zhenghao/` and five Royden records under `logs/royden/`.
- Retained the shared log README at `logs/README.md`.
- Followed the MP1 convention of dated, human-reviewable session summaries.

## Verification completed

- Confirmed that the ten moved historical files are byte-identical to their
  previous versions.
- Checked the Markdown changes with `git diff --check`.
- Kept the interaction-log work on branch `zh/docs-interaction-logs` without
  committing, pushing, or opening a pull request.

## Remaining work and student review

- Review this summary against the original Codex sessions and correct any
  statement that does not match the intended development record.
- Confirm the GitHub Pages deployment from the repository's Pages settings and
  public site URL.
- Confirm the tagged release workflow completed and that its downloadable JAR
  came from the intended `v1.0.0` commit.
- Commit and push the log reorganisation only after the student approves it.
