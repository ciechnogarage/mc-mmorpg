# Backlog - MCMMORPG

Bootstrap items from orchestrator spec section 40.

| ID | Title | Owner | Priority | Risk | Status |
| --- | --- | --- | --- | --- | --- |
| OPS-001 | Plugin manifest - fill versions from server | technical_director_minecraft | critical | R0 | done (staging scan captured 2026-05-26) |
| OPS-002 | Compatibility matrix for current stack | plugin_architect | critical | R0 | open |
| DESIGN-001 | Core loop MCMMORPG | product_owner | critical | R1 | draft |
| DESIGN-002 | Vertical Slice Scope | producer_scope_manager | critical | R1 | open |
| QA-001 | Smoke test checklist execution on staging | qa_lead | high | R1 | open |
| TECH-001 | Staging server readiness | devops_sre | critical | R2 | open |
| INT-001 | LangGraph orchestrator smoke run | technical_director_minecraft | medium | R1 | ready - use `tools/run-orchestrator-graph.ps1` |
| STD-001 | Align repo tool registry and QA docs with canonical `dungeon_ruins_01` gate | qa_lead | high | R2 | done (2026-05-27) |
| STD-002 | Add automated guard/playtest profile for `dungeon_ruins_01` after live runner approval | qa_lead | high | R2 | open |
| STD-003 | Review legacy `dungeon_001` docs and decide archive vs retained vertical-slice evidence | dungeon_designer | medium | R1 | open |
| STD-004 | Run repo-standard cleanup sweep and record non-destructive verdict | qa_lead | high | R1 | done (2026-05-27) |
| STD-005 | Exclude vendor/runtime noise from sprint marker scans | qa_lead | medium | R1 | done (2026-05-27) |
| STD-006 | Exclude generated delivery artifacts from their own sprint marker scans | qa_lead | medium | R1 | done (2026-05-27) |

## Vertical slice candidates (not scheduled)

- Hub/spawn + WorldGuard
- 1-3 classes MMOCore
- 5-10 mobs + 1 boss
- 10-20 items
- 1 mini dungeon
- Main menu + holograms
