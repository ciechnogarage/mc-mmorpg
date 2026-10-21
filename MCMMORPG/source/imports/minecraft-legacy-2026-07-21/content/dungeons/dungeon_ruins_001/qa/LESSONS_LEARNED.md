# DR001 Lessons Learned

- Invoke artifacts can report `success` while the worker only reports a sandbox
  blocker; the board must record the difference between dispatch success and
  implementation success.
- Public namespace decisions need to happen before asset generation. DR001/dr001
  is now the active contract; `dungeon_ruins_001_*` is legacy scaffold only.
- Final asset validation must reject candidate or pending assets by default.
- Runtime dungeon proof needs both QA Lead and Ops/Admin evidence, not just YAML
  parse success.
- Standalone MythicMobs boss smoke must not call MythicDungeons `dsignal`
  directly from `~onDeath`; those calls require a proven active dungeon
  player/instance context.
- Boss spawn persistence checks in the staging QA arena need an active chunk
  holder or an explicit platform setup; spawn-command success alone is not
  enough runtime evidence.
