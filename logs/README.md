# AI interaction summaries

This directory records dated summaries of prompts and interactions used while developing Finders Keepers. An AI agent may create a draft, but a developer must compare it with the original session and mark it verified before submission. These notes are process evidence, not a substitute for automated tests.

## Entry format

Create one Markdown file per session, using a date-based filename such as `2026-09-15-sample.md`. Include:

```text
# YYYY-MM-DD — Short interaction title

- Verifier: name or role
- Environment: OS, Java version, and app/build version when relevant
- Prompt or action: what was entered or done
- Expected result: what should happen
- Observed result: what actually happened
- Verification: pending, or the verifier and date
- Status: pass, fail, or needs follow-up
- Follow-up: issue, next check, or `None`
```

Keep summaries concise and factual. Record only interactions that were human-verified, and distinguish observed behavior from assumptions.

## Security

Never record passwords, access tokens, API keys, personal secrets, private user data, or other credentials. Redact sensitive values before saving a log.
