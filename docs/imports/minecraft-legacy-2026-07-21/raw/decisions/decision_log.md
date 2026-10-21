# Decision Log

Material project decisions. Chat-only decisions do not count.

---

## DEC-001 - Plugin-First MVP

```yaml
decision:
  id: DEC-001
  date: 2026-05-22
  decision: "The MCMMORPG MVP is plugin-first with no custom core plugin while the plugin stack is sufficient."
  reason: "Faster vertical-slice delivery and lower technical risk."
  alternatives:
    - "Write a custom core plugin from the beginning."
    - "Add a ready-made quest plugin immediately."
  impact:
    - "Lower starting cost."
    - "Greater dependency on plugin compatibility."
    - "Requires plugin_manifest and compatibility_matrix."
  approved_by:
    - owner_serwera
    - technical_director_minecraft
  requires_user_approval: true
```

---

## DEC-002 - Orchestrator And LangChain Wrapper Integration

```yaml
decision:
  id: DEC-002
  date: 2026-05-22
  decision: "Plan MCMMORPG work through the minecraft-mcmmorpg-orchestrator skill; optional multi-step runtime uses langchain_codex.minecraft via Codex CLI, not OpenAI API."
  reason: "Separates planning from config execution and reuses the existing LangChain/Codex wrapper."
  alternatives:
    - "One monolithic prompt without repo artifacts."
    - "Only Cursor skill with no LangGraph runtime."
  impact:
    - "docs/tasks/ops artifacts remain the source of truth."
    - "Delivery pipeline remains the gate for release and live QA."
  approved_by:
    - owner_serwera
  requires_user_approval: false
```

---

## DEC-003 - Orchestrator Graph Runner Script

```yaml
decision:
  id: DEC-003
  date: 2026-05-22
  decision: "Add tools/run-orchestrator-graph.ps1 to write JSON into tasks/orchestrator-runs/; graph defaults to no interrupts; knowledge context is trimmed for the Codex CLI limit."
  reason: "Makes LangGraph execution repeatable without manually copying commands."
  impact:
    - "Requires codex login and an installed langchain-codex environment."
    - "Long prompts may require further kb_context trimming."
  approved_by:
    - technical_director_minecraft
  requires_user_approval: false
```

---

## DEC-004 - Cursor Meta-Orchestrator And Single-Account Codex Dispatch

```yaml
decision:
  id: DEC-004
  date: 2026-05-27
  decision: "Cursor is the meta-orchestrator; implementation dispatch follows the workspace single_account policy through invoke-codex-team.ps1 on active login primary. secondary is legacy/manual fallback only."
  reason: "Aligns Minecraft with the audited workspace standard and prevents ACK-only or Cursor-meta implementation from being counted as success."
  impact:
    - "tasks/team-board.md is the current audit verdict layer."
    - "tasks/codex-team-runs/ keeps raw historical evidence."
    - "LangGraph only runs when explicitly useful and not duplicating a full Cursor plan."
  approved_by:
    - owner_serwera
  requires_user_approval: false
```

---

## DEC-005 - Canonical First Dungeon Runtime Evidence

```yaml
decision:
  id: DEC-005
  date: 2026-05-27
  decision: "Repository QA and tool registry treat dungeon_ruins_01 as the only canonical first-dungeon release target; loch and dungeon_001 are retained as legacy/prototype evidence."
  reason: "Prevents prototype or wrong-engine playtests from being counted as release evidence for the current progression path."
  alternatives:
    - "Delete legacy loch/dungeon_001 artifacts immediately."
    - "Keep all dungeon playtest tools equally valid."
  impact:
    - "ops/tools/registry.yaml exposes live_runner_dungeon_ruins_01 as the canonical runtime gate."
    - "qa/smoke_tests.md points release QA to the dungeon_ruins_01 live runner."
    - "tools/playtest-bot remains available for legacy regression only."
  approved_by:
    - qa_lead
    - dungeon_director
  requires_user_approval: false
```
