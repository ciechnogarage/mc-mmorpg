# Agent Workflows

Use this contract for MC server agent work.

Default stance: read-only first, least privilege, small iteration limits,
deterministic checks, and explicit rollback for risky changes.

## Agent Report

```md
Scope:
Evidence:
Findings:
Risks:
Knowledge:
Validation attempted:
Smallest step:
Learning note:
```

For foundation MVP work, also include:

```md
Milestone:
Handoff:
Rollback note:
Status update:
```

## Blast Radius

Classify proposed edits:

- `docs-only`: docs, prompts, routing maps.
- `config`: plugin YAML/JSON/config without direct player-data risk.
- `gameplay`: classes, mobs, drops, dungeons, items, UI behavior.
- `economy`: currency, rewards, shops, fees, grants.
- `permissions`: LuckPerms, command access, rank gates, admin exposure.
- `world`: maps, generated terrain, regions, instances, rollback-sensitive data.
- `DB-storage`: sync, persistence, SQL, migrations, backups.
- `secrets-player-data`: credentials, tokens, databases, player inventories,
  private state.

Economy, permissions, world, DB-storage, and secrets-player-data work needs a
rollback note and explicit approval before destructive live-risk changes.

## Source Ladder

For unknown plugin syntax or runtime behavior, use this order and name the level
used in the report:

1. Local installed config or nearby working example.
2. Local project docs or cached references.
3. Context7, if available.
4. Official vendor documentation.
5. Trusted community/web source, only if higher-trust sources do not answer.

Do not guess PlaceholderAPI placeholders, permission nodes, MythicMobs skills,
ModelEngine IDs, Nexo/resource-pack paths, database settings, or economy command
effects.

## Continuous Improvement

- Record a learning note when the task exposes a reusable lesson, missing
  source, repeated failure, unsafe assumption, or plugin-specific trap.
- After two failed attempts in the same class of problem, do not repeat the same
  command, edit pattern, or diagnosis without new evidence; change hypothesis.
- After three failed attempts, stop implementation, name the blocker, reduce the
  problem to a smaller test, or change strategy before continuing.
- Prefer the smallest proving test that actually validates the change.
- Never store secrets, private player data, tokens, database dumps, or raw
  sensitive logs in learning notes, prompts, docs, or examples.

## Handoff

During read-only dry runs, agents do not spawn nested agents. The coordinator
routes cross-domain work using:

- `docs/ai/mechanic-plugin-map.md`
- `docs/ai/plugin-agent-roster.md`
- `docs/ai/plugin-knowledge-map.md`
- `docs/ai/foundation-mvp-implementation.md`
