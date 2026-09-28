# Reflections

## Evolving from prompting Codex to engineering an agent workflow

My main learning from MP2 was that using an AI coding agent effectively involved much more than writing a detailed prompt and asking it to implement a feature. Compared to MP1, I spent considerably more effort designing the environment in which Codex worked: defining what information it should receive, what decisions it was allowed to make, what artifacts had to exist before implementation, when it had to stop and ask for clarification, and how its work should be verified.

Initially, I tended to put most of these instructions directly into long prompts. This was explicit, but it became increasingly difficult to maintain as the project grew. Instructions concerning repository ownership, documentation, testing, verification, Git operations and feature boundaries were repeatedly included in task prompts. Besides consuming context, there was also a risk that I would omit a rule in a later prompt.

I therefore progressively moved reusable behaviour into a dedicated delivery skill. The purpose of the skill was not to teach Codex how to write Java, but to define how it should perform software engineering in this repository. The resulting workflow was approximately:

Mission Brief → Grilling/Clarification → PRD → TDD → Requirements-to-Tests → Implementation → Verification → Documentation

The skill also contained stop conditions. If an expected artifact was missing, requirements contradicted one another, or a change crossed another developer's ownership boundary, the agent should not silently make a reasonable assumption and continue.

This was one of the largest changes in how I viewed prompting. In MP1, I largely thought about prompts as instructions for producing an output. In MP2, I began treating persistent agent instructions and skills more like an engineering process. The task prompt described what needed to be accomplished, while the skill described how work should be carried out reliably.

### Learning from an early process failure

My authentication implementation was an important example of why this was necessary. The initial authentication work began before I had established the later feature gates involving grilling, an approved PRD and an approved TDD. Rather than retrospectively generating those documents and pretending that the process had occurred, I recorded this as a legacy exception during the authentication cleanup.

The important lesson was not that the authentication code itself was wrong. The problem was that there was no reliable record showing that product decisions, technical decisions and implementation had occurred in the intended order. Once an AI agent can inspect the repository, modify many files and execute commands autonomously, it becomes easy to produce a technically plausible result without being able to reconstruct why particular decisions were made.

I therefore changed the delivery instructions for subsequent work so that Codex had to stop if required artifacts or approvals were missing. If I repeated the project, I would establish this workflow from the beginning instead of introducing it after the first feature.

---

## Using clarification as part of the agent's skill set

One of the most useful additions to my workflow was using `/grill-me` before creating detailed specifications for larger features.

Instead of starting with a prompt such as:

> Implement the Claims feature.

I used the grilling stage to make Codex question underspecified parts of the feature before it was allowed to formalise the requirements or implement anything.

For Claims, seemingly small questions affected large parts of the eventual implementation. Examples included whether there could be more than one active Claim for the same LOST/FOUND pair, what rejection should prevent, whether a withdrawn Claim could be resubmitted, what Students versus Desk Officers were allowed to see, and what should happen to the corresponding reports after a Claim was approved.

These were not primarily programming questions. They were product and domain decisions. Codex could generate a reasonable answer for each of them, but a reasonable answer was not necessarily the intended answer.

This changed my understanding of what an effective AI engineering agent should do when it encounters ambiguity. I initially thought that a highly autonomous agent should be able to fill in small gaps independently. I later found that this could be dangerous because those small assumptions propagate into persistence, UI behaviour, tests and documentation. For important semantic decisions, asking questions was more valuable than immediately generating code.

The planning artifacts also became useful interfaces between myself and the agent. A Mission Brief bounded the task. The grilling decisions resolved ambiguities. The PRD recorded expected behaviour. The TDD translated that behaviour into a technical design, while the requirements-to-tests document forced the implementation and testing strategy to remain traceable to the requirements.

This reduced the amount of product interpretation that had to occur during implementation.

---

## Reviewing AI-generated specifications instead of only reviewing code

Another significant lesson was that AI-generated requirements, design documents and tests required the same critical review as generated production code.

For example, while reviewing the Claims design, I found that a Claim table was expected to display a single `category`, even though a Claim relates a LOST report and a FOUND report. The specification was syntactically complete, but it did not define which report supplied the displayed category. I eventually specified that this referred to the FOUND report category.

This was a useful example because nothing was obviously broken. Codex had produced something that looked complete, but the meaning of one field was still ambiguous. If I had moved directly to implementation, the agent could easily have selected one interpretation and propagated it through the UI and tests.

I encountered a similar issue in the technical design around transient Claim state. The generated design could clear draft state when the Claims view was re-entered or refreshed. Technically, this was a coherent implementation, but from a user's perspective it could cause entered ownership evidence or reasons to disappear unexpectedly. I therefore separated refreshing persisted data from intentionally clearing transient session state.

The requirements-to-tests stage also showed that comprehensiveness does not automatically imply correctness. The generated test plan covered many cases, but I still found semantic problems such as incorrect withdrawal assumptions and behaviours that had been inferred rather than explicitly required.

This taught me that large AI-generated artifacts can create a different review problem. The agent is very good at producing breadth quickly, but the amount of material can make subtle assumptions harder to notice. My role shifted from writing every artifact manually to distinguishing between:

- requirements that I had actually approved;
- reasonable inferences made by the agent;
- behaviours that had been invented because the specification was incomplete.

I therefore learned not to treat a PRD, TDD or test plan as trustworthy merely because it was detailed and internally consistent.

---

## Where Codex significantly improved productivity

A task where the agent worked particularly effectively was report persistence.

One of my prompts was:

> Plan and implement repository-only JSON persistence for canonical lost-and-found reports.

The important part of this prompt was the phrase `repository-only`. I deliberately constrained the work so that persistence was implemented without changing the JavaFX application, authentication workflow or startup wiring.

By this point, the surrounding workflow and planning artifacts had already established additional decisions such as immutable identity fields, supported operations, storage limits and failure behaviour. Therefore, Codex had a relatively narrow decision space in which it could operate autonomously.

Within this boundary, the agent was very effective. It implemented not only the basic insert/load/replace behaviour, but also validation for malformed data, invalid encodings, duplicate identifiers, capacity limits, immutable-field conflicts and failed replacements. It also produced recovery, concurrency, fault-injection, privacy and capacity tests.

The final requirements-to-tests review represented all 17 PRD requirement identifiers, 16 acceptance criteria and five scenarios, together with test and review evidence.

This is an area where the agent clearly improved my productivity. Producing that breadth of defensive tests manually would have taken substantially more time. Once the desired behaviour was sufficiently specified, Codex was able to explore edge cases systematically and repeatedly execute verification commands without the fatigue associated with manual iteration.

However, this example also showed me that agent productivity depends heavily on preparation. A broad prompt such as "implement persistence" would have given Codex much more freedom to decide matters such as deletion support, application wiring or storage semantics itself. The productivity improvement came not only from the model's ability to generate code quickly, but from reducing the number of decisions it had to invent.

I therefore learned that a useful agent is not necessarily one with maximum freedom. For well-defined engineering tasks, carefully bounded autonomy gave me better results.

---

## Repository ownership and controlling agent autonomy

Working in a two-developer project introduced another problem: Codex could technically modify files outside my assigned feature ownership even when doing so was undesirable from a team perspective.

Some of my features eventually needed integration with shared or Developer 1-owned files. Instead of allowing the agent to edit these files automatically, I introduced a cross-owner approval mechanism. When such a change became necessary, Codex was expected to identify the affected file, explain why the change was required, propose the minimum change, and stop for approval.

This initially felt slower than simply letting Codex fix the integration itself. However, I realised that the speed of an AI agent actually makes repository governance more important. A human developer may notice that they are crossing several module boundaries because of the effort involved in making those edits. An agent can modify many files almost immediately. Without explicit ownership rules, a locally convenient solution could create unnecessary coupling or interfere with my teammate's work before I noticed it.

This was also useful when the implementation depended on domain objects owned by Developer 1. Instead of embedding authentication information into another developer's report model or expanding the model opportunistically, I tried to keep authentication data inside the authentication domain and use small integration contracts where required.

My understanding of agent autonomy therefore changed during the project. I no longer think autonomy should mean "make every decision necessary to complete the goal." In a shared software project, useful autonomy has to operate inside technical, product and ownership boundaries.

---

## Recovering from agent context limitations

A practical limitation I encountered was context exhaustion during a long implementation session.

Earlier in the project, my implementation prompts were very long because I attempted to restate most of the relevant rules and feature context in each conversation. During one session, the available context was exhausted before the entire task was finished.

Rather than attempting to reconstruct progress from conversational memory, I started a new session and made the agent inspect the repository itself: Git commits, the dirty working tree, implemented source files, tests and approved artifacts.

This changed how I thought about continuity when working with agents. The conversation should not be the only place where important project state exists. The repository has to remain the durable source of truth.

Incremental commits, specifications and recorded decisions therefore became useful not just for traditional version control, but also as a recovery mechanism for the agent. A new agent session could reconstruct what had already happened without requiring me to restate the entire history.

This experience also motivated moving repeated instructions out of very large prompts and into reusable skills. Large prompts are explicit, but they consume context and become difficult to maintain. A smaller task-specific prompt combined with stable repository instructions made later sessions easier to resume.

If I repeated the project, I would use smaller incremental commits and externalised agent instructions from the first sprint instead of treating conversation history as durable state.

---

## Experimenting with specialised subagents

After establishing a more reliable single-agent workflow, I also experimented with specialised subagents.

For documentation work, I separated responsibilities such as the User Guide, Developer Guide and development logs rather than asking a single agent to update all documentation sequentially. Each worker had a narrower writable scope, while a coordinator established shared context and later reconciled the outputs.

I found that this was useful when tasks could be divided into genuinely independent areas. The benefit was not simply that three agents could write three files at once. Narrower responsibilities also reduced the amount of unrelated context each worker had to process.

I applied a similar idea when improving test coverage. Instead of distributing work arbitrarily by individual classes, the work was separated along behavioural boundaries such as Claims, persistence, and matching/review/authentication. Separate worktrees allowed workers to operate without immediately interfering with one another.

However, this experiment also exposed a new cost: parallel agents created coordination work.

Individually reasonable documentation changes were not always globally consistent. During reconciliation, stale workflow descriptions, screenshots and terminology still had to be identified. Therefore, the output of the subagents could not simply be concatenated or merged without review.

This taught me that multi-agent parallelism is most useful when the decomposition itself is good. If several agents are given overlapping responsibilities or depend heavily on the same evolving assumptions, the resulting reconciliation work can remove much of the time saved through parallel execution.

If I repeated the task, I would define the shared contracts and source-of-truth artifacts more explicitly before spawning workers. I would also continue using subagents only for work with sufficiently independent outputs instead of assuming that more agents automatically produces higher productivity.

---

## Testing and knowing when additional agent work stops being valuable

Another area where I changed my approach from MP1 was test coverage.

It is easy to instruct an agent to continue generating tests until a coverage number becomes extremely high. Codex is particularly suited to this because it can inspect uncovered branches, generate another test and repeat the process quickly.

However, I became less convinced that maximising a coverage percentage was itself a useful goal.

For MP2, I tried to prioritise behaviour connected to actual requirements instead of manufacturing tests simply to exercise every JavaFX rendering branch, boilerplate method or unlikely platform failure. The requirements-to-tests process helped because tests were expected to trace back to intended behaviour rather than existing only to increase JaCoCo statistics.

This required human judgement because the agent could continue producing additional tests almost indefinitely if instructed to do so. At some point, I had to decide whether another test meaningfully reduced risk or merely increased a metric.

I learned that test generation is one of the strongest uses of the agent, but test selection is still an engineering decision. AI made it cheap to produce more tests; it did not make every possible test equally valuable.

---

## Verification and the danger of plausible retrospective evidence

One of the more unexpected lessons came from documentation and logging.

Some feature interaction records had not been captured completely at the time the work was performed. Later, I used the agent to reconstruct retrospective logs by inspecting planning artifacts, source files, tests and Git history.

For example, one retrospective prompt asked the agent to inspect the approved Claims planning artifacts, implementation, tests and delivery commits.

The important requirement was that repository evidence could establish what had been implemented, but it could not establish an observation that had never been recorded.

The reconstructed Claims log could show that the Claim domain, persistence, Student and Desk Officer workflows, tests and navigation code existed. It could not truthfully claim that a developer had manually exercised every rendered JavaFX workflow. For that reason, the verification remained marked as `Pending`.

I applied the same distinction to the possible-match and Desk Officer review retrospectives.

I found this important because LLMs are extremely good at producing plausible histories. If I simply asked Codex to "write the development log", it could easily produce a clean narrative containing commands or observations that would sound completely believable. That is not the same as evidence.

I therefore constrained the log-writing process so that retrospective reconstruction could cite repository state while explicitly refusing to claim unrecorded test execution or manual UI observations.

This changed my understanding of verification. There are multiple kinds of evidence:

- source code can establish that an implementation exists;
- automated test results can establish that particular checks passed;
- Git history can establish when recorded changes occurred;
- a screenshot can establish a specific visual state;
- manual verification can establish that a human actually exercised a workflow.

One type should not silently be substituted for another.

If I repeated the project, I would record manual verification, screenshots and development logs immediately after each feature instead of reconstructing them later.

---

## What I would change

If I repeated MP2, I would make several changes to the agent and its skill set.

First, I would establish the complete delivery workflow and ownership rules before implementing the first feature. Authentication showed that retrofitting governance after implementation creates unnecessary cleanup.

Second, I would keep stable behaviour in skills and use much shorter task prompts. The task prompt should mostly explain the current goal and unusual constraints; general repository behaviour should not need to be repeated every time.

Third, I would add a more explicit verification skill or checklist separating automated verification, repository evidence and manual UI verification. This would make it harder for either myself or the agent to confuse "tests passed" with "the feature has been fully verified."

Fourth, I would preserve progress through smaller commits and feature artifacts more consistently so that a fresh agent session can recover from context loss without requiring a large continuity prompt.

Fifth, I would maintain the User Guide, Developer Guide, logs and screenshots closer to the point when each feature is implemented. Delaying documentation made later reconciliation more expensive.

Finally, I would be more selective about workflow depth. The full Mission Brief → Grilling → PRD → TDD → Requirements-to-Tests pipeline was valuable for behaviourally complex features such as Claims. For a very small and low-risk change, forcing every artifact to the same level of detail could create more overhead than value. A future version of the skill could provide different workflows depending on the risk and ambiguity of the task.

---

## Overall learning

The most important thing I learned from MP2 was that designing an effective AI agent for software engineering is not mainly about finding a perfect prompt.

The agent was already capable of generating code, reading a repository, writing tests, running commands and producing documentation. The difficult part was engineering the surrounding process so that those capabilities were used in a controlled and reviewable way.

My role increasingly became that of an owner and orchestrator rather than the person typing every implementation detail. I had to decide what the agent was allowed to infer, where it had to ask questions, what artifacts acted as contracts, how repository ownership should constrain edits, what evidence was sufficient for verification, and when additional agent work no longer added meaningful value.

This also changed how I interpret agent autonomy. More autonomy did not reduce the need for human engineering judgement. In several cases it increased it, because Codex could implement a plausible interpretation much faster than I could notice that the interpretation itself was wrong.

The most effective workflow I found was therefore not to supervise every individual line of code, but to establish strong boundaries before implementation and review the decisions and artifacts that mattered most. When the problem was well specified, Codex was highly productive at implementation, edge-case exploration and testing. When product semantics, architecture or evidence were ambiguous, human review remained necessary.

My biggest improvement from MP1 to MP2 was therefore not that I learned how to make Codex write more code. I learned how to make its work more observable, bounded, recoverable and verifiable.