# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

This is **not a code project** — there is nothing to build, lint, or unit-test. It contains:

- `MCMMORPG/` — a live/staging **Paper Minecraft server** (MC 1.21.11, Java 21) running an MMO-RPG plugin stack. "Work" here means editing plugin **YAML config** and content files under `MCMMORPG/plugins/<Plugin>/`.
- `docs/` — a non-sensitive knowledge base (plugin inventory, environment notes, AI playbooks, checklists).

There is no git repository in this checkout (`git` commands fail with exit 128).

## Running the server

Paper jar lives at `MCMMORPG/paper.jar`. Start it from the `MCMMORPG/` directory:

```bash
cd MCMMORPG && java -Xms4G -Xmx6G -jar paper.jar nogui
```

`online-mode=true` and `whitelist`/`ops`/`banned-*` are empty — this is a working server instance, not a template. Live changes affect real state.

There is **no automated config validator in this checkout**. To validate a YAML edit, the only real check is a server (re)start and a scan of `MCMMORPG/logs/latest.log` for plugin enable errors/warnings. Prefer a YAML syntax check (e.g. `python3 -c "import yaml,sys; yaml.safe_load(open(sys.argv[1]))" <file>`) before restarting.

## Plugin architecture (the big picture)

The RPG experience is built from interdependent plugins; load order and shared libraries matter. Inventory + observed versions are the source of truth in `docs/plugin_manifest.yaml` and `docs/compatibility_matrix.md`. Key relationships:

- **MythicLib** is the shared foundation for **MMOCore** (classes/stats/progression), **MMOItems** (items/gear/stats), and **MMOInventory**. Editing stats in one of these often requires matching definitions in the others.
- **MythicMobs** (mobs/bosses/skills/loot) → **MythicCrucible** (Mythic-defined items) and **MythicDungeons** (instanced content) build on it. **ModelEngine** (+ `MEG-Molang`) supplies custom mob models; **LibsDisguises** disguises.
- **Packet/API deps:** `ProtocolLib`, `packetevents`, `PixelLibs`, `PlaceholderAPI` are required by multiple plugins — do not remove or downgrade casually.
- **Infrastructure:** `LuckPerms` (permissions — *critical*), `Vault`/`CMI`(+`CMILib`) (economy/admin bridge), `HuskSync` (cross-server player-data sync), `Multiverse-Core`/`Iris`/`VoidGen` (worlds/generation), `WorldGuard`+`FastAsyncWorldEdit` (regions/editing), `Guilds`, `MCPets`, `voicechat`.

Content lives in well-known subtrees: MythicMobs in `mobs/ skills/ items/ droptables/ spawners/`; MMOItems in `item/ item-types.yml item-tiers.yml`; MMOCore in `classes/ attributes/ exp-*`; MythicDungeons in `maps/ loottables.yml`. When changing content, keep cross-references (item IDs, skill names, drop-table keys) consistent across these files.

## The `docs/` knowledge base — read with care

`docs/` was inherited from a larger Windows workspace (`C:\Codex\...`). Several files reference paths and tooling that **do not exist in this checkout**: `.github/agent-knowledge/`, `.cursor/rules/`, `.agents/`, `tasks/`, `decisions/`, `builds/`, `STANDARD.md`, `Server — kopia/1.21.11`, and the PowerShell `verify-*.ps1`/`validate-*.ps1` scripts. Treat those references as **stale/aspirational, not actionable here**. The docs that describe *this* checkout accurately are:

- `docs/plugin_manifest.yaml`, `docs/compatibility_matrix.md`, `docs/server_environment.md` — keep these in sync if you change the plugin set.
- `docs/ai/` playbooks and checklists are advisory.

## Safety boundaries (from docs/ai/standard-map.md)

These map to risk tiers; honor the gates even though the enforcement scripts aren't present here:

- **Do not run live server commands, restart, or deploy without explicit user approval.**
- Permissions (LuckPerms), economy (Vault/CMI), staff/admin, and player-data (HuskSync) changes are high-risk — plan, note rollback, and get approval before applying.
- Plugin syntax must come from installed/generated config or verified official docs, not guesses.
- Never commit or store secrets, tokens, player data, or raw logs in `docs/`.

## Working with the local Codex (from /home/przemek/CLAUDE.md)

Claude is lead; Codex is for second opinions / heavy handoffs. Route with `npm run collab:route -- --task "<task>"`; cheap review via `npm run consult:codex -- "<question>"`. These npm scripts live in the user's home workspace, not in this repo.
