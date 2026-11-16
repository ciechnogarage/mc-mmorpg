# Concept Sketch — dungeon_001

## Layout concepts

Direction boards and Iris zone maps (256×512): [`concepts/`](concepts/) — see [`concepts/CONCEPT_BOARD.md`](concepts/CONCEPT_BOARD.md).

Staging Iris image: `plugins/Iris/packs/dungeon_001/images/dungeon_001_layout.png` (currently **concept B — flooded corridor**).

Legacy reference clones: [`reference/Example.png`](reference/Example.png), [`reference/dungeon_001_layout.png`](reference/dungeon_001_layout.png).

## Live preview for meta

After layout or staging changes, capture repo-visible frames:

```powershell
cd C:\Codex\Minecraft\tools\playtest-bot
npm run capture:dungeon-001-preview
```

Output: [`live-preview/`](live-preview/) — `latest.png`, `latest.json`, timestamped frames. Full runbook: [`VISUAL_CHECK.md`](VISUAL_CHECK.md).

## Route (top-down)

```text
[dunstart] ---- corridor ----> [dunmid] ---- gate ----> [dunend / boss]
     ^                           |                          |
  spawn/platform              wave zone                  Dungeon001Warden
```

## Objective points

| Point | Zone | Notes |
| --- | --- | --- |
| O1 | dunstart | Safe spawn platform; first objective marker |
| O2 | dunmid | Wave trigger (sentinels) |
| O3 | dunend | Boss spawn; finish signal |

## Server paths

| Artifact | Path |
| --- | --- |
| Iris pack | `plugins/Iris/packs/dungeon_001/` |
| MD map | `plugins/MythicDungeons/maps/dungeon_001/` |
| ChunkGenerator | `iris:dungeon_001` (in `config.yml`) |
