# Blocker example - how to report a hard stop

Situation: FinishDungeon trigger held in MD's in-memory template referenced a
stale mob name after a pack refactor; disk edits were overwritten by autosave.

Correct blocker report:

```md
scope: level_1 completion wiring (FinishDungeon on boss death)
evidence: functions.yml.bak.diag diff vs autosaved file; md reload NPE log;
  alias workaround proven for spawn path but finish trigger lives in template
  memory, not pack
files_or_areas: plugins/MythicDungeons/maps/level_1/functions.yml (MD-owned)
proposed_or_applied_changes: none applied - disk edit is not durable while an
  edit session is open
validation: real-kill probe shows dungeon does not finish
risks: closing the session may autosave stale state over template
blockers: need a write path MD will not overwrite: builder-GUI inside the edit
  session, or server stopped + session closed + disk edit + start
next_owner: coordinator decision (server restart window)
```

Rules illustrated: after 2 failed attempts at the same sub-goal, STOP and report
understanding (not effort); expensive cycles (server restart, full E2E) max 1
before consulting the source; never claim done without a real-kill validation.
