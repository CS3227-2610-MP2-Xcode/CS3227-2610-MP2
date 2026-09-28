# Disclaimer
This reflection was polished by an AI, but the core content, engineering
decisions, and reflections are entirely my own (ZhengHao).

# Reflections on AI-Assisted Software Engineering

This document captures how I customised and used AI agents while building
Finders Keepers for CS3227. I did not treat the agent as a code generator that
could be trusted without supervision. I used different tools and prompting
techniques for review, visual design, context management, Git hygiene, and
feature planning, then verified the outputs against the application and my own
product decisions.

## Example 1: Using Greptile as an AI code reviewer

### How I used it

As the codebase and number of pull requests grew, slow manual peer review became
a bottleneck. I configured Greptile to review pull requests and provide another
pass over changes before they were merged. I also gave it lightweight project
context so that its comments could be grounded in our domain and repository
conventions rather than generic Java advice.

I used Greptile as a first-pass reviewer, not as the final authority. When it
raised an issue, I passed the comment to Codex with instructions to inspect the
existing branch, verify whether the issue was real, and implement a fix only if
the behavior could be reproduced or established from the contract. This saved
time because I could focus my manual attention on suspicious or high-risk code
instead of rereading every changed line with no starting point.

### What worked well

This workflow found subtle problems in the appointment feature. Examples
included a storage-location correction reversing custody, Student appointment
lists collapsing at the startup window size, booking times disappearing from
active rows, earlier appointment attempts being omitted from history, and audit
rows assigning the wrong actor role. These were more useful than style-only
comments because they affected actual user workflows.

The combination of Greptile and Codex also improved testing efficiency. I asked
Codex to turn a valid review finding into a focused regression test or a
repeatable manual scenario. The resulting evidence made it easier to understand
the bug and to check that the proposed fix did not alter unrelated behavior.

### Where additional guidance was needed

Greptile occasionally marked reviews as skipped or counted a review without
publishing a useful GitHub comment. More importantly, a confident review comment
was still only a hypothesis. It could misunderstand the domain, point at code
that was already protected elsewhere, or propose a fix with a larger behavioral
impact than the original problem.

I therefore still needed to trace the relevant service, persistence, and UI
flow. For stateful features such as booking, custody, and item return, a
single-line comment did not explain the complete sequence or all affected
states. Blindly accepting every comment would have created extra work and could
have introduced regressions.

### What I learnt and would change

AI review is most useful as a prioritisation tool. It saves review time by
pointing a flashlight at risky areas, but engineering judgement remains with the
developer. If I repeated the project, I would require every review finding to
include a reproducible scenario, the expected contract, and a suggested
regression test. I would also check review publication on a small pull request
before depending on it for the rest of the sprint.

## Example 2: Combining UI prompting with visual verification skills

### How I prompted the UI work

For the UI sessions, I did not simply ask the agent to "make it look nicer." I
specified the users, the affected screens, the desired interaction, and the
logic that had to remain unchanged. For example, I described when appointment
actions should appear, how slot creation should use `+` and `-` controls, how
duplicate or invalid times should produce user-facing alerts, and how approved
claims should use readable item information instead of long identifiers.

I also supplied visual direction. The general application should use a cohesive
native JavaFX design, while Student-facing screens should be more colourful and
appropriate for primary-school users. I asked the agent to preserve
authentication, persistence, and domain behavior, and to update only the
screenshots that genuinely changed.

### Skills and tools selected by the agent

The agent matched different skills to different parts of the UI task:

- It used JavaFX and CSS implementation skills for the reusable application
  shell, cards, navigation, controls, and feature-specific styling.
- It first attempted a live desktop visual audit. When the desktop-control layer
  could not attach to the unbundled JavaFX process, it created a temporary,
  test-only JavaFX snapshot harness instead.
- It rendered the real JavaFX nodes to PNG and inspected the images instead of
  assuming that correct source code implied a correct visual result.
- It used the `imagegen` skill to create one original bear mascot, then added it
  sparingly to the Student and login experiences.
- It regenerated the affected User Guide and README screenshots and checked
  their dimensions and visible layout before committing them.
- It still ran the normal Gradle quality gate and packaged JavaFX smoke test so
  visual work did not bypass code-level verification.

### What worked well

The snapshot workflow caught a real CSS cascade problem: an application-wide
label rule overrode the intended white text on the green login panel. The code
compiled and the stylesheet looked reasonable when read as text, but the
rendered screenshot exposed the contrast error immediately. Rerendering after
the selector fix provided direct evidence that the screen had improved.

The same method helped verify that the mascot did not crowd the login form and
that the 1120 by 760 README screenshot represented the real application. This
was much stronger than accepting a generated design because it looked plausible
in code.

### Where additional guidance was needed

My first appointment UI requirements mixed several ideas: hiding irrelevant
actions, disabling time-gated actions, and explaining unavailable actions using
hover text. After trying the interaction, I changed my mind and preferred
clickable actions with clear popup errors. The agent could implement either
design, but it could not decide which experience I actually preferred.

Visual verification also required extra setup when the normal desktop-control
tool could not connect to JavaFX. The temporary snapshot harness was useful, but
it added work and could only show the states that the harness deliberately
constructed. I still needed human review of the complete signed-in workflows.

### What I learnt and would change

An effective UI agent needs both implementation skills and a rendered feedback
loop. Next time, I would define a small visual acceptance matrix before coding:
screen, user role, window size, state, expected actions, expected message, and
required screenshot. I would also separate interaction rules from visual style
so that changing a color or mascot never silently changes business behavior.

## Example 3: Forking chat sessions to preserve context

### How I used it

Finders Keepers contained several related but separable jobs. I used forked chat
sessions when a new task depended on the same product context but needed a
different focus. For example, appointment planning could be continued in an
implementation session, while later UI, documentation, and review work could
start from similar context without turning one chat into an unmanageable
transcript.

Forking preserved useful context such as the Student and Desk Officer roles, the
primary-school setting, established terminology, and earlier product decisions.
At the same time, each fork could have a narrow objective and its own completion
criteria.

### What worked well

This improved productivity because I did not need to explain the complete domain
again for every follow-up. It also reduced accidental mixing between unrelated
work. A UI-focused session could concentrate on JavaFX and screenshots, while a
review-focused session could concentrate on a particular pull request and its
tests.

Forking was especially useful before delegating routine implementation to a
lower-cost model. I could prepare a detailed plan in one session, continue from
that context, and use the main session to review the implementation.

### Risks and corrections

Preserved context can become stale. A fork may remember an earlier branch,
contract, or file layout even after another pull request has been merged. I saw
this risk when concurrent report and persistence work had to be reconciled
around one canonical model. A forked session that trusts old context without
checking Git can confidently work against the wrong state.

### What I learnt and would change

Forking is a context-management skill, not a substitute for repository checks.
For every fork, I would start with a compact handoff containing the objective,
current branch, base commit or pull request, files in scope, decisions that must
remain true, and verification commands. The agent should then confirm the live
Git state before editing. This preserves the useful reasoning context without
treating it as proof of the current codebase.

## Example 4: Asking the AI to commit by separation of concern

### Why I made this explicit

Working code is not the only measure of a good software-engineering workflow.
Large mixed commits make review, rollback, and debugging difficult. I therefore
repeatedly asked the agent to commit by separation of concern rather than
placing implementation, tests, screenshots, and documentation into one commit.

Examples from this project included separating the appointment feature from its
tests and workflow documentation, separating the reusable application shell
from feature-workspace styling, and separating the login mascot code from the
refreshed README screenshot.

### What worked well

Focused commits made the pull requests easier to understand. A reviewer could
inspect a production change, then see the tests that established its behavior,
then review the documentation update. For visual work, keeping the UI code and
generated screenshots separate made it obvious which files changed application
behavior and which files only recorded the new appearance.

This also improved traceability. When a review finding referred to one
behavior, I could identify the relevant fix and regression test without
searching through a large unrelated commit.

### Where guidance was needed

The agent did not automatically know the right commit boundaries. If I asked it
only to finish a feature, it could optimise for speed and group everything
together. Conversely, splitting too aggressively could create commits that did
not compile or had no meaningful purpose on their own.

I still had to decide whether a test belonged with its production fix or as a
separate reviewable concern, and whether a documentation update described the
same motive or a genuinely separate task.

### What I learnt and would change

Next time, I would include the proposed commit plan in the implementation plan
before any files are changed. Each commit should have one coherent motive, pass
the relevant checks, and leave the repository in an understandable state. I
would review `git diff --cached` and the commit message before authorising each
commit instead of asking for cleanup only at the end.

## Example 5: Writing a draft prompt before each feature

### My prompting technique

For separate features or jobs, I usually wrote a draft checklist before asking
the agent to work. The checklist combined the user problem, concrete UI
examples, edge cases, and a request to clarify anything uncertain. One
appointment UI draft included requirements such as:

```text
- Show actions according to the current workflow and completed inputs.
- Replace the create-slot form with adjacent + and - controls and a time dialog.
- Show popup alerts for duplicate slots and invalid time input.
- Use readable item details instead of a long claim identifier.
- Clarify uncertain design decisions before implementation.
```

I used checkboxes because they made the prompt function as both a design draft
and a completion checklist. Screenshots attached to the draft gave the agent a
concrete reference for the controls or layout that I was discussing.

### What worked well

Drafting the prompt forced me to think through the feature before code was
written. It exposed edge cases such as duplicate slots, invalid times, small
windows, missing storage locations, and actions attempted before the collection
time. It also gave the agent more useful acceptance criteria than a broad
request such as "improve appointment UX."

This technique helped me choose the appropriate skill set. Logic-heavy items
needed contract inspection and tests. Visual items needed JavaFX rendering and
screenshot inspection. Review items needed reproduction before fixing. Git
items needed an explicit branch and commit plan. The task description therefore
guided the agent toward the right tools instead of relying on it to guess.

### Where the draft created extra work

A detailed prompt can still contain contradictions or premature solutions. My
first draft suggested both hiding some buttons and showing a disabled collection
button with hover guidance. Later, after seeing the workflow, I preferred
clickable buttons that displayed popup errors. The checklist was useful, but
marking an item as complete did not prove that the interaction felt right.

The draft also risked prescribing a widget before the underlying workflow was
fully understood. The agent still needed to inspect the current state machine
and ask whether the proposed UI preserved its rules.

### What I learnt and would change

I would structure future draft prompts into five sections:

1. User problem and affected role.
2. Existing behavior and evidence, including a screenshot where useful.
3. Non-negotiable product rules and files or logic that must remain unchanged.
4. Acceptance criteria and edge cases.
5. Open questions, verification steps, and proposed commit boundaries.

This format would distinguish requirements from examples and leave uncertain
choices open for discussion. I would ask the agent to restate the acceptance
criteria and identify contradictions before producing an implementation plan.

## Overall reflection

The most effective single-agent workflow was not based on one large, general
prompt. It came from selecting a bounded skill set for each task and defining
how success would be observed. Greptile accelerated review, but findings still
needed reproduction. UI generation accelerated styling, but screenshots and
manual inspection were necessary. Forked sessions preserved context, but Git
state had to be refreshed. Atomic commits improved reviewability, but their
boundaries required human judgement. Detailed draft prompts reduced ambiguity,
but they still needed refinement after interacting with the real application.

The tasks handled most effectively by AI were structured planning, routine
implementation, targeted test generation, repetitive documentation, and
first-pass review. The agent required the most guidance for product semantics,
visual preference, workflow trade-offs, current branch state, and deciding
whether an automated finding was actually valid.

If I repeated the project, I would give every task a short contract containing
scope, invariants, evidence, acceptance criteria, verification methods, and
commit boundaries. I would also keep visual snapshot support as a reusable test
tool instead of rebuilding a temporary harness for each UI session. These
changes would make the agent faster without weakening the human review points
that protect code quality and product intent.
