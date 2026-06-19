# AI Project Knowledge Base

This folder contains durable, non-sensitive operating knowledge for AI-assisted
work in `C:\Codex`.

The canonical rules remain in [`../../AGENTS.md`](../../AGENTS.md) and
[`../../STANDARD.md`](../../STANDARD.md). Files here are supporting artifacts:
playbooks, checklists, evals, retrospectives, and curated learnings.

## Contents

- `playbooks/` - repeatable workflows for common tasks.
- `learning-log.md` - stable, non-sensitive lessons worth preserving.
- `anti-patterns.md` - repeated failure modes and how to prevent them.
- `golden-tasks/` - eval tasks for testing agent behavior.
- `security-review.md` - security review checklist and evidence format.
- `code-review.md` - code review checklist and evidence format.
- `agent-workflows.md` - workflow contract template for orchestration.

## Hygiene

- Do not store secrets, tokens, PII, customer data, raw production logs, or
  private keys here.
- Prefer tests, hooks, CI, and scanners for hard requirements.
- Keep entries short, current, and linked to real files or commands when useful.
- Remove stale learnings instead of layering conflicting rules.
