# Foundation Runtime Baseline

Purpose: keep the staging MVP baseline explicit so runtime QA can separate
accepted staging compromises from new regressions.

## Active Staging Baseline

- `online-mode=false` is the current local MVP baseline.
- `_validation/.runtime.lock` is mandatory for any runtime-touching foundation run.
- `plugins/Nexo/settings.yml` uses non-blocking pre-join pack dispatch for bot QA:
  `send_pre_join: false`, `mandatory: false`.
- HuskSync stays disabled on staging until a real database baseline exists.
- MCPets YAML fallback is accepted on staging only while pets are outside the
  first vertical slice.

## Accepted Staging Noise

- Offline-bot `ModelEngine` skin warnings are accepted noise if login/spawn still succeeds.
- Known MCPets SQL fallback warnings are accepted only when pet persistence is
  not part of the tested slice.

## Release Blockers

- New `Command exception`, `Could not pass event`, or `Unhandled exception`
  lines inside the scoped `latest.log` window.
- Dungeon player-path failure on the exact route:
  `foundation -> starter -> Portal Nexus -> level_1 -> boss -> reward -> return`.
- Any proof claim that relies on config presence, chat-only success, or admin
  shortcuts instead of the exact player path.

## Next Update Trigger

Update this file whenever staging exceptions change, a disabled blocker returns
to the active stack, or a new accepted warning is introduced intentionally.
