# Design Brief — Dungeon-001 (`dungeon_001`)

## Identity

- **Product name:** Dungeon-001
- **Level id:** `level_001`
- **Dungeon id:** `dungeon_001`
- **Pipeline:** first Iris + MythicDungeons vertical slice (template after `loch`)

## Player promise

A cold flooded ruin: safe mossy entry, escalating blue-ice mid pressure, blackstone boss finale. Each party gets a **private CLASSIC instance** (`/md play dungeon_001`).

## Fantasy and pacing

| Zone | Job | Emotion |
| --- | --- | --- |
| Start (`dunstart`) | Orient, first objective | Curiosity, safety |
| Mid (`dunmid`) | Wave / pressure | Tension |
| End (`dunend`) | Boss arena | Payoff |

## Constraints

- PNG source: `reference/Example.png` (cloned from loch test layout).
- Zone colors: `#000040` / `#000080` / `#0000C0`; outside map = air only.
- MythicDungeons flow in editor only (no hand-written `functions.yml` until schema verified).
- Staging server: `Server — kopia/1.21.11`.

## References

- Playbooks: `MajnKarto/.github/agent-knowledge/agent-playbooks/dungeon-level-production-pipeline.md`
- Loch QA contract: `loch-dungeon-qa-architect.md` (adapted signals for `dungeon_001`)
