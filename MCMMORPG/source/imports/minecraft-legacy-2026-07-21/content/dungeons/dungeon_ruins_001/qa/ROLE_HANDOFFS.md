# DR001 Role Handoffs

Status: release-prep handoff, waiting-for-live-approval

| Role | Checked | Sources | Blockers | Handoff | Lesson |
| --- | --- | --- | --- | --- | --- |
| Product/Design | Concept identity, stage flow, boss roster, rewards | `C:\Codex\dungeon_ruins_001_codex_concept.md` | Runtime proof missing | Combat, World, Economy | Lock namespace early. |
| Plugin Admin | MythicDungeons command syntax, no live execution | local MythicDungeons commands docs | Exact MMOCore command hooks need staging proof | Ops/Admin, QA | Docs syntax is command-level, objective wiring remains editor-owned. |
| Combat | DR001 mobs, bosses, skills, drops | Concept and local MythicMobs style | Boss tuning untested | QA Lead | Telegraphs need client observation. |
| World/Iris/Nexo/ModelEngine | Biome, structure, Nexo, model contracts | Concept asset sections | Final assets not supplied | Builder/Artist | Legacy scaffold is not final. |
| Economy | DR001 currencies, gear, weekly reward | Concept rewards/economy | Drop rates need economy review | QA Lead | Weekly reward must be lockout-tested. |
| QA Lead | Static validators and player QA script | `qa/STAGING_ASSET_PLAYTEST.md` | Live approval missing | Ops/Admin | Final state is waiting-for-live-approval. |
| Ops/Admin | Live runner, rollback, log markers | `qa/FINAL_ASSET_LIVE_RUNNER.md` | Staging runtime not approved | Operator | No server command was run in implementation. |
