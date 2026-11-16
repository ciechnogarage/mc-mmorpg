# DUNGEON-001 — recovery audit (2026-05-26)

## Werdykt

**Nie jest dobrze zaczęty jako deliverable.** Jest **klon lochu + dokumentacja + jeden playtest layoutu**. Brakuje walidowanych runów Codex, flow MD i dowodu instancji CLASSIC.

## Co jest realne (staging `Server — kopia/1.21.11`)

| Element | Stan | Dowód |
|--------|------|--------|
| Iris pack `dungeon_001` | istnieje | `plugins/Iris/packs/dungeon_001/` + biomes `dunstart`, `dunmid`, `dunend` |
| MD map `dungeon_001` | istnieje, CLASSIC | `plugins/MythicDungeons/maps/dungeon_001/config.yml` |
| ChunkGenerator | `iris:dungeon_001` | config.yml (setup script) |
| Mythic mobs/skills | pliki istnieją | `Dungeon001DungeonMobs.yml`, `Dungeon001DungeonSkills.yml` |
| MD function graph | **pusty** | `functions.yml` → `Functions: []`; skan całego Codex — brak wzorca do skopiowania |
| Design docs | w repo | `design/levels/dungeon_001/*.md` |

## Co jest „na papierze” (nie liczy się jako done)

| Artifact | Problem |
|----------|---------|
| `WP-dungeon-001-design/iris/encounter` runs | `invalid_legacy_meta` — zrobione w Cursor meta, nie `invoke_role` |
| `WP-dungeon-001-qa` | `mvp-pass` tylko na playtest layoutu, nie pełny MVP |
| `run-dungeon-001-meta` | `dispatched` bez sensownego deliverable |
| Praca nad `langgraph_minecraft` v2 | infrastruktura hubu, **nie** domknięcie DUNGEON-001 |

## Playtest (2026-05-26)

- `dungeon-001-qa`: **playable but rough** — spawn mossy_stone, `/md play` działał
- Instance A/B: problemy z **permissions** lub brak ruchu po `/md play` (raporty instance smoke)
- Acceptance „2 party → 2 instance id” — **PASS** w logach/folderach (`verify-dungeon-001-instances.py`, 2026-05-27); bot raportuje ten sam prefiks Iris world string

## Blockery (kolejność)

1. **MD-FLOW-001 (human)** — `/md edit dungeon_001` → wire flow → `/md save` ([`MD_EDITOR_CHECKLIST.md`](MD_EDITOR_CHECKLIST.md))
2. ~~**QA-INSTANCE-001**~~ — zamknięte 2026-05-27 (`verify-dungeon-001-instances.py` PASS)
3. **Permissions** — boty QA: `grant-dungeon-001-qa.py` + restart Paper po pierwszym join
4. **Re-dispatch Codex** — iris verify, encounter tune (prawdziwy `invoke`, nie meta)

## Następne kroki (minimalny MVP)

```text
1. Human: MD editor save (blocker #1)
2. Pro: config_codex_worker — permissions + biome audit (WP-dungeon-001-recovery-audit retry)
3. QA: instance smoke po #1–2
4. Meta: update team-board tylko po run JSON z finished_at + pliki
```

## Komendy

```powershell
cd C:\Codex\Minecraft
.\tools\prepare-dungeon-001-md-editor.ps1   # przed sesją human
.\tools\start-staging-qa.ps1 -HeapBudget 2G
python tools\grant-dungeon-001-qa.py
.\tools\run-dungeon-001-instance-smoke.ps1
```
