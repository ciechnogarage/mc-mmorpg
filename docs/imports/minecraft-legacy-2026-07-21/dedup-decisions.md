# Dedup decisions — minecraft legacy docs import

Scope: 19 canonical collisions from `docs/imports/minecraft-legacy-2026-07-21/import-manifest.tsv`.

This file does not promote, overwrite, delete, or move any active documentation. It only records dedup decisions for the quarantine import.

## Summary

- Total collisions: 19
- Byte-identical duplicates: 11
- Different files: 8
- `skip-duplicate`: 11
- `keep-current`: 7
- `extract-snippet`: 0
- `needs-human-review`: 1

## Decision table

| # | Relative path | Decision | Active target | Legacy quarantine path | Reason | Action |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `AGENTS.md` | `skip-duplicate` | `docs/AGENTS.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/AGENTS.md` | Byte-identical with active doc; no promotion needed. | Leave quarantine copy; do not modify active doc. |
| 2 | `ai/README.md` | `keep-current` | `docs/ai/README.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/README.md` | Active docs should remain authority; legacy README may be reviewed later only for small wording snippets. | Keep active doc unchanged; review legacy only for small extractable snippets in a later ticket. |
| 3 | `ai/agent-workflows.md` | `keep-current` | `docs/ai/agent-workflows.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/agent-workflows.md` | Active file now contains current /home/przemek/projects/MC + dev-<scope> lease standard; legacy differs and must not override it. | Keep active doc unchanged; review legacy only for small extractable snippets in a later ticket. |
| 4 | `ai/anti-patterns.md` | `keep-current` | `docs/ai/anti-patterns.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/anti-patterns.md` | Active anti-pattern docs should remain authority; legacy differs and needs separate human review before any snippet extraction. | Keep active doc unchanged; review legacy only for small extractable snippets in a later ticket. |
| 5 | `ai/code-review.md` | `skip-duplicate` | `docs/ai/code-review.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/code-review.md` | Byte-identical with active doc; no promotion needed. | Leave quarantine copy; do not modify active doc. |
| 6 | `ai/golden-tasks/forbidden-command.md` | `skip-duplicate` | `docs/ai/golden-tasks/forbidden-command.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/golden-tasks/forbidden-command.md` | Byte-identical with active doc; no promotion needed. | Leave quarantine copy; do not modify active doc. |
| 7 | `ai/golden-tasks/refactor-no-api-change.md` | `skip-duplicate` | `docs/ai/golden-tasks/refactor-no-api-change.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/golden-tasks/refactor-no-api-change.md` | Byte-identical with active doc; no promotion needed. | Leave quarantine copy; do not modify active doc. |
| 8 | `ai/learning-log.md` | `keep-current` | `docs/ai/learning-log.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/learning-log.md` | Learning logs are historical and can conflict; keep active log as authority, legacy remains quarantine reference. | Keep active doc unchanged; review legacy only for small extractable snippets in a later ticket. |
| 9 | `ai/playbooks/bugfix-root-cause.md` | `skip-duplicate` | `docs/ai/playbooks/bugfix-root-cause.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/playbooks/bugfix-root-cause.md` | Byte-identical with active doc; no promotion needed. | Leave quarantine copy; do not modify active doc. |
| 10 | `ai/playbooks/dependency-upgrade-safe.md` | `skip-duplicate` | `docs/ai/playbooks/dependency-upgrade-safe.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/playbooks/dependency-upgrade-safe.md` | Byte-identical with active doc; no promotion needed. | Leave quarantine copy; do not modify active doc. |
| 11 | `ai/playbooks/flaky-test-debug.md` | `skip-duplicate` | `docs/ai/playbooks/flaky-test-debug.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/playbooks/flaky-test-debug.md` | Byte-identical with active doc; no promotion needed. | Leave quarantine copy; do not modify active doc. |
| 12 | `ai/repo-cleanup-2026-05-27.md` | `skip-duplicate` | `docs/ai/repo-cleanup-2026-05-27.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/repo-cleanup-2026-05-27.md` | Byte-identical with active doc; no promotion needed. | Leave quarantine copy; do not modify active doc. |
| 13 | `ai/security-review.md` | `skip-duplicate` | `docs/ai/security-review.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/security-review.md` | Byte-identical with active doc; no promotion needed. | Leave quarantine copy; do not modify active doc. |
| 14 | `ai/standard-map.md` | `skip-duplicate` | `docs/ai/standard-map.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/ai/standard-map.md` | Byte-identical with active doc; no promotion needed. | Leave quarantine copy; do not modify active doc. |
| 15 | `assets/blockbench_model_standard.md` | `needs-human-review` | `docs/assets/blockbench_model_standard.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/assets/blockbench_model_standard.md` | Different design/asset standard content; needs model/assets authority review before extraction. | Keep active doc unchanged; review legacy only for small extractable snippets in a later ticket. |
| 16 | `assets/reviews/mcmmo_elite_crypt_warden_review.md` | `skip-duplicate` | `docs/assets/reviews/mcmmo_elite_crypt_warden_review.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/assets/reviews/mcmmo_elite_crypt_warden_review.md` | Byte-identical with active doc; no promotion needed. | Leave quarantine copy; do not modify active doc. |
| 17 | `compatibility_matrix.md` | `keep-current` | `docs/compatibility_matrix.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/compatibility_matrix.md` | Legacy compatibility matrix is tied to old Server — kopia/staging inventory; keep current file. | Keep active doc unchanged; review legacy only for small extractable snippets in a later ticket. |
| 18 | `plugin_manifest.yaml` | `keep-current` | `docs/plugin_manifest.yaml` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/plugin_manifest.yaml` | Legacy manifest describes old Server — kopia/staging plugin inventory; keep current manifest. | Keep active doc unchanged; review legacy only for small extractable snippets in a later ticket. |
| 19 | `server_environment.md` | `keep-current` | `docs/server_environment.md` | `docs/imports/minecraft-legacy-2026-07-21/raw/docs/server_environment.md` | Server/environment docs must follow current dev/staging/lease standard; legacy environment doc remains quarantine reference. | Keep active doc unchanged; review legacy only for small extractable snippets in a later ticket. |

## Different-file notes

### `ai/README.md`

- Decision: `keep-current`
- Reason: Active docs should remain authority; legacy README may be reviewed later only for small wording snippets.
- Diff preview first 120 lines, capped:

```diff
--- active
+++ legacy
@@ -1,39 +1,26 @@
 # AI Project Knowledge Base
 
-This folder stores durable, non-sensitive operating knowledge for AI-assisted
-work in the MC server project. Canonical rules remain in
-[`../../AGENTS.md`](../../AGENTS.md).
+This folder contains durable, non-sensitive operating knowledge for AI-assisted
+work in `C:\Codex`.
 
-Supporting files here should stay implementation-oriented: playbooks,
-checklists, routing maps, status ledgers, and curated learnings.
+The canonical rules remain in [`../../AGENTS.md`](../../AGENTS.md) and
+[`../../STANDARD.md`](../../STANDARD.md). Files here are supporting artifacts:
+playbooks, checklists, evals, retrospectives, and curated learnings.
 
 ## Contents
 
-- `playbooks/` - repeatable workflows for common tasks
-- `learning-log.md` - stable, non-sensitive lessons worth keeping
-- `anti-patterns.md` - repeated failure modes and how to avoid them
-- `golden-tasks/` - eval tasks for agent behavior
-- `security-review.md` - security review checklist and evidence format
-- `code-review.md` - code review checklist and evidence format
-- `agent-workflows.md` - workflow contract, reports, blast radius, handoffs
-- `plugin-agent-roster.md` - routing map from plugin domains to agents
-- `plugin-knowledge-map.md` - trusted source ladder and no-expose boundaries
-- `known-runtime-issues.md` - read-only runbooks for current runtime problems
-- `mechanic-plugin-map.md` - mechanic-level routing
-- `how-to-use-mc-agents.md` - prompt templates and usage modes for MC agents
-- `mc-agent-task-cheatsheet.md` - quick routing guide by task type
-- `foundation-mvp-implementation.md` - staging-first MVP deployment plan
-- `foundation-master-index.md` - canonical routing index for foundation docs and milestones
-- `foundation-mvp-scope.md` - explicit in-scope and out-of-scope boundary for the active MVP slice
-- `foundation-network-architecture.md` - runtime topology, plugin boundaries, and validation path for the active slice
-- `foundation-solo-runbook.md` - Codex-only orchestration runbook for the M0-M7 sweep
-- `foundation-long-run-prompt-pack.md` - long-run implementation prompt pack for the foundation MVP
-- `foundation-long-run-ledger.md` - working ledger for long foundation runs and resumes
-- `foundation-runtime-baseline.md` - active staging baseline, accepted exceptions, and release blockers for foundation QA
```

### `ai/agent-workflows.md`

- Decision: `keep-current`
- Reason: Active file now contains current /home/przemek/projects/MC + dev-<scope> lease standard; legacy differs and must not override it.
- Diff preview first 120 lines, capped:

```diff
--- active
+++ legacy
@@ -1,98 +1,39 @@
 # Agent Workflows
 
-## Workspace / Lease Gate
-
-- Canonical workspace root is `/home/przemek/projects/MC`.
-- `/home/przemek/projects/MCMMORPG` and `/home/przemek/projects/Minecraft` are import/archive candidates, not active roots, unless a dedicated ticket says otherwise.
-- Write work requires a concrete `dev-<scope>` lease from `MCMMORPG/infra/leases/registry.tsv`; without lease the work is read-only/docs-only.
-- `staging` is integration/promotion only. Agents do not use it for parallel edits.
-- `world/items/hub` is legacy-only. Treat old backend references as recovery context or mark them `needs-update`; do not use them as default target selection.
-- Validation channels: RCON for admin/server checks, Mineflayer for player behavior/GUI/items/quests/interactions, companion/plugin API for structured state, logs for diagnostics, real client for visual QA.
-
-Use this contract for MC server agent work.
-
-Default stance: read-only first, least privilege, small iteration limits,
-deterministic checks, and explicit rollback for risky changes.
-
-## Agent Report
+Use this contract before starting T4 orchestrated, automated, or multi-system
+work.
 
 ```md
-Scope:
-lease:
-workspace_root:
-allowed_paths:
-runtime_touched:
-staging_touched:
-Evidence:
-Findings:
-Risks:
-Knowledge:
-Validation attempted:
-validation:
-rollback:
-Smallest step:
-Learning note:
+# Agent Workflow Contract
```

### `ai/anti-patterns.md`

- Decision: `keep-current`
- Reason: Active anti-pattern docs should remain authority; legacy differs and needs separate human review before any snippet extraction.
- Diff preview first 120 lines, capped:

```diff
--- active
+++ legacy
@@ -14,13 +14,3 @@
   visual, browser, screenshot, or playtest gate.
 - Updating shared rules from a one-off observation without proposing it for
   human review.
-- Repeating the same command, config edit, or diagnosis after it failed twice
-  without new evidence or a changed hypothesis.
-- Editing plugin YAML by guesswork instead of checking nearby local examples or
-  the relevant plugin documentation.
-- Claiming a plugin/runtime issue is fixed without re-checking the current
-  `MCMMORPG/logs/latest.log` or an equivalent focused validation.
-- Guessing PlaceholderAPI placeholders, LuckPerms permission nodes, MythicMobs
-  syntax, ModelEngine IDs, or economy command effects from memory alone.
-- Running broad server/playtest validation when a smaller deterministic check
-  would prove the same property.
```

### `ai/learning-log.md`

- Decision: `keep-current`
- Reason: Learning logs are historical and can conflict; keep active log as authority, legacy remains quarantine reference.
- Diff preview first 120 lines, capped:

```diff
--- active
+++ legacy
@@ -19,34 +19,8 @@
 Review date:
 ```
 
-## MC Agent Entry Template
-
-Use this for plugin-agent lessons that should affect future work.
-
-```md
-## YYYY-MM-DD - Short title
-
-Problem:
-
-Failed attempts:
-
-Root cause:
-
-Source used:
-Local config / local docs / Context7 / official docs / web-community / none
-
-Fix:
-
-Future guardrail:
-
-Applies to agent:
-
-Review date:
-```
-
 ## Current Learnings
 
-- The workspace baseline lives in `AGENTS.md` files at the workspace and project
-  levels.
+- The workspace baseline lives in `C:\Codex\AGENTS.md` and `C:\Codex\STANDARD.md`.
 - Project folders may have their own `AGENTS.md`; those rules are additive and
   should be read before project-specific edits.
```

### `assets/blockbench_model_standard.md`

- Decision: `needs-human-review`
- Reason: Different design/asset standard content; needs model/assets authority review before extraction.
- Diff preview first 120 lines, capped:

```diff
--- active
+++ legacy
@@ -3,6 +3,14 @@
 Status: standard repozytoryjny  
 Zakres: Blockbench -> ModelEngine v4 -> MythicMobs -> Nexo/resource pack -> Paper/Purpur 1.21.x  
 Klasyfikacja pracy: T2 dla standardu i asset pipeline; T3, gdy zmiana dotyka staging/live plugin config, economy, permissions, resource pack wdrozonego na graczy albo migracji ID.
+
+Powiazane dokumenty:
+
+- `docs/assets/blockbench_model_specialist_playbook.md` - sciezka mastery,
+  quality gates, failure drills i evidence loop dla agentow oraz reviewerow.
+- `docs/assets/templates/blockbench_model_readme_template.md` - wymagany punkt
+  startowy dla README nowego modelu.
+- `docs/assets/reviews/TEMPLATE.md` - szablon review z klasyfikacja porazek.
 
 ## 1. Cel standardu
 
@@ -16,6 +24,10 @@
 - brak konfliktow ID i nazw w resource packu;
 - profesjonalny wyglad bez niszczenia FPS/TPS;
 - assety praktyczne gameplayowo, nie tylko ladne w podgladzie Blockbench.
+
+Ten dokument definiuje baseline. Dla pracy specjalistycznej nad nowym modelem
+albo review kandydata trzeba rowniez przejsc playbook specjalistyczny i
+uzupelnic evidence loop.
 
 ## 2. Typy modeli
 
@@ -106,15 +118,3 @@
 - `cube1`
 - `cube2`
 - `bone`
-- `bone2`
-- `test`
-- `nowy_model`
-- `final_final`
-- `boss_poprawiony`
-- `bez_nazwy`
-
-## 5. Standard skali
```

### `compatibility_matrix.md`

- Decision: `keep-current`
- Reason: Legacy compatibility matrix is tied to old Server — kopia/staging inventory; keep current file.
- Diff preview first 120 lines, capped:

```diff
--- active
+++ legacy
@@ -1,6 +1,6 @@
 # Compatibility Matrix
 
-Status: staging inventory refreshed on 2026-06-24 from `MCMMORPG`.
+Status: staging inventory captured on 2026-05-26 from `Server — kopia/1.21.11`.
 
 ## Server Target
 
@@ -24,31 +24,28 @@
 | ProtocolLib | 5.4.1-SNAPSHOT-f606cc9 | observed on 1.21.11 staging | Packet deps | staged-observed |
 | PacketEvents | 2.11.2 | observed on 1.21.11 staging | Packet deps | staged-observed |
 | ModelEngine | 4.0.9 | observed on 1.21.11 staging | Models + performance | staged-observed |
-| HuskSync | 3.8.8-0772f09+mc.1.21.11 | observed on 1.21.11 staging | Player sync | disabled for staging MVP until DB baseline exists |
+| HuskSync | 3.8.8-0772f09+mc.1.21.11 | observed on 1.21.11 staging | Player sync | staged-observed |
 | PixelLibs | 1.1.1 | observed on 1.21.11 staging | Library dependency | staged-observed |
 
 ## Observed Evidence
 
-- `MCMMORPG/version_history.json` reports `1.21.11-127-bd74bf6 (MC: 1.21.11)`.
-- `MCMMORPG/plugins/` inventory contains the full MMO/Mythic/Iris stack plus `MythicHUD`.
-- `MCMMORPG/logs/latest.log` exists and must be reviewed for current startup warnings/errors before release.
+- `version_history.json` reports `1.21.11-127-bd74bf6 (MC: 1.21.11)`.
+- `codex-start-last.out.log` reports Paper boot on Java 21.
+- `plugins/` inventory contains the full MMO/Mythic/Iris stack plus `MythicHUD`.
 
 ## Known Gaps
 
 - Dedicated quest plugin is still not present in the observed stack.
 - Anti-cheat is still not present in the observed stack.
 - Auction/shop path is still unresolved beyond Vault/CMI bridge assumptions.
-- Latest checked startup log summary reported HuskSync/MCPets SQL errors and a large warning count; re-check current logs before release decisions.
-- HuskSync has been moved out of active plugins for the staging MVP; MCPets YAML fallback is accepted only for this staging slice.
-- CMI economy is enabled as the current staging Vault economy provider.
-- `online-mode=false` is the requested staging baseline for local MVP work.
+- Startup log review is still needed for plugin enable warnings/errors beyond presence and version detection.
 
 ## Verification Procedure
 
```

### `plugin_manifest.yaml`

- Decision: `keep-current`
- Reason: Legacy manifest describes old Server — kopia/staging plugin inventory; keep current manifest.
- Diff preview first 120 lines, capped:

```diff
--- active
+++ legacy
@@ -5,13 +5,13 @@
   target_minecraft_version: 1.21.11
   runtime: Paper 1.21.11-127-main@bd74bf6
   environment: staging
-  server_folder: MCMMORPG
-  last_scanned: 2026-06-24
+  server_folder: Server — kopia/1.21.11
+  last_scanned: 2026-05-26
   scan_source:
-    - MCMMORPG/plugins
-    - MCMMORPG/logs/latest.log
-    - MCMMORPG/version_history.json
-  scan_note: Versions are sourced from local jar filenames and existing runtime evidence; startup warnings/errors still require review before release.
+    - Server — kopia/1.21.11/plugins
+    - Server — kopia/1.21.11/codex-start-last.out.log
+    - Server — kopia/1.21.11/version_history.json
+  scan_note: Versions are sourced from staged jar filenames plus observed Paper boot lines.
 
 plugins:
   - name: Chunky-Bukkit
@@ -40,16 +40,6 @@
     license_status: unknown
     update_policy: staging_required
     criticality: high
-
-  - name: CharacterStage
-    jar: CharacterStage-0.1.0-SNAPSHOT.jar
-    current_version: 0.1.0-SNAPSHOT
-    role: character selection/profile integration bridge
-    owner: ui_ux_menu_designer
-    license_status: internal
-    update_policy: staging_required
-    criticality: high
-    source_path: MCMMORPG/plugin-src/character-stage
 
   - name: CoreTools
     jar: CoreTools-1.4.1.jar
@@ -104,9 +94,6 @@
```

### `server_environment.md`

- Decision: `keep-current`
- Reason: Server/environment docs must follow current dev/staging/lease standard; legacy environment doc remains quarantine reference.
- Diff preview first 120 lines, capped:

```diff
--- active
+++ legacy
@@ -15,23 +15,25 @@
 | Environment | Purpose | Notes |
 | --- | --- | --- |
 | DEV | Fast config experiments | May reset. |
-| STAGING | Compatibility and player tests | `MCMMORPG`; required before production changes. |
+| STAGING | Compatibility and player tests | `Server — kopia/1.21.11`; required before production changes. |
 | PRODUCTION | Live players | Backup, rollback, and approval are required for R3/R4. |
 
 ## Server Folders In Repo
 
 | Folder | Role |
 | --- | --- |
-| `MCMMORPG` | Local staging copy with Paper runtime and plugin inventory. |
+| `Server — kopia/1.21.11` | Local staging copy with Paper runtime and plugin inventory. |
+| `Server` | Present in repo, but no plugin inventory was detected during the latest pass. |
 
 ## Evidence Used
 
-1. `MCMMORPG/version_history.json`
-2. `MCMMORPG/plugins/`
-3. `MCMMORPG/logs/latest.log`
+1. `Server — kopia/1.21.11/version_history.json`
+2. `Server — kopia/1.21.11/codex-start-last.out.log`
+3. `Server — kopia/1.21.11/plugins/`
+4. `Server — kopia/1.21.11/logs/latest.log`
 
 ## Remaining Verification Steps
 
 1. Capture exact startup version lines for ProtocolLib and PixelLibs when they matter for an upcoming update.
-2. Review staging boot warnings before the next player-facing QA session; latest checked summary reported HuskSync/MCPets SQL errors.
+2. Review staging boot warnings before the next player-facing QA session.
 3. Keep this file and `docs/plugin_manifest.yaml` in sync with the staging copy after plugin changes.
```

## Verification expectations

- Parser should confirm 19 table rows.
- No active docs are modified by this decision file.
- Future promotion must happen through explicit `extract-snippet` or curated design lesson tickets, not by copying raw legacy files over active docs.
