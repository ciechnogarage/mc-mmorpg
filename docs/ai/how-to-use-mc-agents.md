# MC Agents

Repo-local MC agents sa warstwa wykonawcza dla domen pluginow i mechanik. Nie sa background services. Top-level Codex lub Claude uzywa ich jako task-agent tools, a finalne scalenie trzyma orchestrator.

## Co Tu Jest Agentem

- `inventory`, `ui`, `art`, `rpg`, `dungeon`, `mobs`, `economy`, `ops`, `qa` sa domenowymi agentami.
- `foundation:solo` nie jest agentem. To helper koordynacyjny.

## Helper Command

Run z `/home/przemek`:

```bash
npm run mc:agent -- <alias> [--write-policy read-only|workspace-write] [--role primary|secondary|validation] "<task>"
```

Domyslnie agent jest `read-only`, chyba ze task zawiera wyrazny sygnal wykonawczy albo podasz `--write-policy workspace-write`.

Prompt agenta buduje registry w `/home/przemek/scripts/collab-agent-registry.js`. Registry okresla:

- `allowed_paths`
- `forbidden_paths`
- `required_inputs`
- `required_evidence`
- `done_criteria`
- `rollback_required_for`
- `default_validation_commands`

## Aliasy

- `inventory`: plugin inventory, ownership, drift
- `rpg`, `items`: classes, items, stats, loot, crafting, progression
- `mobs`, `models`: MythicMobs, ModelEngine, pets, resource-pack assets
- `dungeon`, `world`: MythicDungeons, maps, regions, pregeneration
- `ui`, `hud`, `menus`: DeluxeMenus, MythicHUD, holograms, placeholders
- `art`, `assets`, `visual`: art direction, modele i tekstury broni/itemow, bloki, furniture, skiny, kosmetyki, ikony resource pack
- `perms`, `permissions`, `economy`: LuckPerms, Vault, CMI, Guilds
- `ops`, `sync`, `logs`: runtime health, SQL/storage, packets, voice
- `qa`, `release`: readiness, bot/runtime evidence, pass/fail validation

## Contract

Kazdy agent zwraca:

```md
scope:
evidence:
files_or_areas:
proposed_or_applied_changes:
validation:
risks:
blockers:
next_owner:
```

Raport mozna sprawdzic:

```bash
npm run collab:validate-output -- --agent qa --file /path/to/report.md
npm run collab:validate-output -- --agent art --file /path/to/report.md --json
```

## Examples

```bash
npm run mc:agent -- ops --write-policy read-only --role primary "read-only latest.log storage startup errors"
npm run mc:agent -- mobs --write-policy read-only --role secondary "trace GroveGuardian MythicMobs ModelEngine IDs"
npm run mc:agent -- dungeon --write-policy workspace-write --role primary "staging implementation requested: level_1 flow"
npm run mc:agent -- qa --write-policy read-only --role validation "validate foundation vertical slice evidence"
```

## Foundation Routing

- `foundation starter menu`: primary `ui`, secondary `rpg`, validation `qa`
- `level_1 dungeon`: primary `dungeon`, secondary `mobs`, validation `qa`
- `boss/model/reward hook`: primary `mobs`, secondary `dungeon`, validation `qa`
- `weapon/furniture/texture/skin/resource pack`: primary `art`, odpowiedni agent gameplay jako secondary, validation `qa`
- `runtime/log/storage`: primary `ops`, validation `qa`

Realne taski artystyczne na backendach `world/items/hub` sa opisane w [mc-art-server-playbook.md](/home/przemek/projects/MC/docs/ai/mc-art-server-playbook.md).
Nowy proces dla mobow i modeli (brief -> art -> mobs -> optional dungeon/rpg -> qa) jest opisany w [mc-mob-model-agent-pipeline.md](/home/przemek/projects/MC/docs/ai/mc-mob-model-agent-pipeline.md).
Warstwa nauki i krytyki dla agentow jest opisana w:
- [mc-model-mob-learning-spec.md](/home/przemek/projects/MC/docs/ai/mc-model-mob-learning-spec.md)
- [mc-model-mob-critique-rubric.md](/home/przemek/projects/MC/docs/ai/mc-model-mob-critique-rubric.md)
- [mc-model-mob-anti-pattern-catalog.md](/home/przemek/projects/MC/docs/ai/mc-model-mob-anti-pattern-catalog.md)

Jesli ktos nie rozumie relacji `Codex/Claude -> agent role -> skill -> corpus -> artifact gates`, czytaj najpierw:
- [mc-agent-operating-model.md](/home/przemek/projects/MC/docs/ai/mc-agent-operating-model.md)

## Safety

- Do not inspect secrets, DB contents, player data, generated world data, or plugin caches unless they are explicitly in scope.
- Ryzykowne zmiany economy, permissions, world, DB/storage, i player-adjacent wymagaja rollback notes.
- QA musi uzyc real evidence: logs, commands, bot/runtime checks, explicit PASS/FAIL artifacts. Documentation-only approval nie wystarcza.
- QA verdict musi byc jednym z: `PASS`, `FAIL`, `BLOCKED`, `INSUFFICIENT_EVIDENCE`.
- Runtime tests uzywaja `MCMMORPG/docker/mc` i backendow `world`, `items`, `hub`.
- Client flows wchodza przez Velocity na `localhost:25565`.
- Native `Paper` lub `start.sh` to niewazne evidence.
- Testuj tylko backend dotkniety przez zmiane.
