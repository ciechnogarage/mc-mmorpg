# AI Anti-Patterns

Repeated mistakes should be captured here only after they are confirmed and
general enough to help future work.

## Current Anti-Patterns

- Treating a successful tool invocation or ACK-only run as implementation
  success. A completed implementation needs real file changes and/or
  deterministic verification evidence.
- Copying the full workspace standard into every project file. Prefer one
  canonical root standard with concise project-specific additions.
- Claiming UI/game work is done after a build only. Use the relevant project
  visual, browser, screenshot, or playtest gate.
- Updating shared rules from a one-off observation without proposing it for
  human review.
