# Codex Team Board

**Rule 1:** `success` is only accepted after a real invoke artifact. Cursor meta
does not implement features, and raw historical run JSON files are preserved as
audit evidence. See [`ops/codex-team/dispatch-contract.md`](../ops/codex-team/dispatch-contract.md).

| packet_id | parent_task | role_id | channel | run_artifact | board_status | deliverable |
|-----------|-------------|---------|---------|--------------|--------------|-------------|
| WP-dungeon-001-design | DUNGEON-001 | dungeon_designer | single_shot | tasks/codex-team-runs/WP-dungeon-001-design.json | invalid_legacy_meta | Pre-enforcement Cursor-meta implementation; redispatch if needed. |
| WP-dungeon-001-iris | DUNGEON-001 | config_codex_worker | single_shot | tasks/codex-team-runs/WP-dungeon-001-iris.json | invalid_legacy_meta | Pre-enforcement Cursor-meta implementation; redispatch if needed. |
| WP-dungeon-001-encounter | DUNGEON-001 | mob_boss_designer | single_shot | tasks/codex-team-runs/WP-dungeon-001-encounter.json | invalid_legacy_meta | Pre-enforcement Cursor-meta implementation; redispatch if needed. |
| WP-dungeon-001-qa | DUNGEON-001 | qa_lead | single_shot | tasks/codex-team-runs/WP-dungeon-001-qa.json | blocked | Live QA blocked until server/runtime is verified. |
| WP-mc-graph-v2 | MC-GRAPH-V2 | minecraft_server_orchestrator | langgraph_minecraft | LangChain tests: 37 passed | done-phase-a-e1 | Graph v2 DoD completed outside repo board artifact. |
| run-dungeon-001-meta | DUNGEON-001 | minecraft_server_orchestrator | langgraph_minecraft | tasks/orchestrator-runs/run-dungeon-001-meta.json | stale | No deliverable; preserve as historical context only. |
| WP-auto-20260526-100132-plus | AUTO | qa_lead | single_shot | tasks/codex-team-runs/WP-auto-20260526-100132-plus.json | not_satisfied | Read-only/ACK-only result; no repo deliverable. |
| WP-auto-20260526-100132-pro | AUTO | config_codex_worker | single_shot | tasks/codex-team-runs/WP-auto-20260526-100132-pro.json | success | Real invoke updated bootstrap docs. |
| WP-auto-20260526-114246-plus | AUTO | qa_lead | single_shot | tasks/codex-team-runs/WP-auto-20260526-114246-plus.json | not_satisfied | Read-only/ACK-only result; no repo deliverable. |
| WP-auto-20260526-114246-pro | AUTO | config_codex_worker | single_shot | tasks/codex-team-runs/WP-auto-20260526-114246-pro.json | success | Real invoke updated `AGENTS.md` learned facts. |
| WP-mc-graph-smoke | MC-GRAPH-SMOKE | minecraft_server_orchestrator | langgraph_minecraft | tasks/codex-team-runs/WP-mc-graph-smoke.json | success | Real graph smoke artifact. |
| WP-dungeon-001-recovery-audit | DUNGEON-001 | config_codex_worker | single_shot | tasks/codex-team-runs/WP-dungeon-001-recovery-audit.json | not_satisfied | ACK-only role setup; no recovery audit deliverable. |
| WP-dungeon-001-visual-redesign | DUNGEON-001 | config_codex_worker | single_shot | tasks/codex-team-runs/WP-dungeon-001-visual-redesign.json | not_satisfied | ACK-only role setup; no visual redesign deliverable. |
| WP-dungeon-001-functions-yaml | DUNGEON-001-MD-FLOW | config_codex_worker | single_shot | tasks/codex-team-runs/WP-dungeon-001-functions-yaml.json | success | Real invoke updated stale task state. |
| STD-001-repo-cleanup | STD-001 | qa_lead | direct_docs_cleanup | docs/ai/repo-cleanup-2026-05-27.md | success | Standard cleanup aligned QA/tooling to canonical `dungeon_ruins_01`; no live commands. |
| STD-004-standard-sweep | STD-004 | qa_lead | direct_docs_cleanup | docs/ai/repo-cleanup-2026-05-27.md | success | Non-destructive standard sweep run; active stale references checked, historical evidence preserved. |
| STD-005-standard-generator-cleanup | STD-005 | qa_lead | direct_docs_cleanup | .github/agent-knowledge/server-delivery-pipeline/_tools/new-sprint-cycle.ps1 | success | Sprint marker scans now exclude vendor/runtime noise; historical intake cleaned. |
| STD-006-generated-artifact-scan-cleanup | STD-006 | qa_lead | direct_docs_cleanup | .github/agent-knowledge/server-delivery-pipeline/_tools/new-sprint-cycle.ps1 | success | Sprint marker scans no longer scan generated delivery artifacts; bootstrap intake wording cleaned. |

## Follow-ups

- Docs mirror cache is intentionally out of scope for this ops cleanup.
- Historical JSON run artifacts are not rewritten here; board verdicts are the current audit layer.
| WP-dungeon-ruins-001-final-assets | DUNGEON-RUINS-001 | config_codex_worker | direct_repair_after_failed_invoke | C:\Codex\Minecraft\tasks\codex-team-runs\WP-dungeon-ruins-001-final-assets.json | repaired_direct | Final intake gate repaired in-session: normal validation blocks `hybrid_review_required`, `-UseFinalIntake` aborts before staging writes, scaffold dry-run still works. |
| WP-dungeon-ruins-001-dr001-release-prep | DUNGEON-RUINS-001 | config_codex_worker | direct_repair_after_failed_invoke | C:\Codex\Minecraft\tasks\codex-team-runs\WP-dungeon-ruins-001-dr001-release-prep.json | repaired_direct_waiting_for_live_approval | Invoke worker hit sandbox failure; active source pack migrated to DR001/dr001, validators and release-prep gates added, runtime QA awaits approval. |
| WP-dungeon-ruins-001-dr001-release-prep | - | from-packet | codex-team | C:\Codex\Minecraft\tasks\codex-team-runs\WP-dungeon-ruins-001-dr001-release-prep.json | dispatched |
