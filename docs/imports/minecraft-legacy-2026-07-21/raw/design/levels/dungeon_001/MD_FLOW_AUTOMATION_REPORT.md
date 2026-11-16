# dungeon_001 — automatyzacja flow MD (2026-05-27)

## Werdykt: **PARTIAL+** (graf ładuje się; flow in-game wymaga walidacji)

| Kryterium DONE | Stan |
| --- | --- |
| `functions.yml` niepuste | **PASS** — generator `tools/generate-dungeon-001-functions-yml.py` → ~3.5 KB, 5 funkcji; szablon w [`functions.yml.template`](functions.yml.template) |
| Ładowanie dungeonu | **PASS** — Paper: `Loading dungeon: dungeon_001` bez `not loaded` / `invalid YAML` (po FQCN + `org.bukkit.Location`) |
| Flow w grze (objective/boss po `/md play`) | **UNKNOWN** — playtest QA nie wychwycił komunikatów skilli; wymaga sesji creative/survival lub dłuższego bota |
| Playtest bez void/death | **PASS** — `2026-05-27T07:46:48Z`: `playable but rough`, `death: null`, `instance-active-at-spawn` |
| Instancje CLASSIC | **PASS** — `verify-dungeon-001-instances.py` |
| Dokumentacja auto vs human | ten plik |

## Generator (reverse JAR 2.0.1)

Skrypt: [`tools/generate-dungeon-001-functions-yml.py`](../../tools/generate-dungeon-001-functions-yml.py)

Wymagania schematu (ustalone empirycznie na staging):

1. `==` = **FQCN** klasy (np. `net.playavalon.mythicdungeons.dungeons.triggers.TriggerDungeonStart`), nie samo `TriggerDungeonStart`.
2. `location` = `org.bukkit.Location` z polami `x/y/z/pitch/yaw` (zwykły map → `IllegalArgumentException`).
3. Jedna funkcja na unikalną lokalizację (HashMap MD).
4. `FunctionTargetType`: `{ ==: …FunctionTargetType, value: PLAYER|NONE|PARTY }`.

Graf V1 (5 funkcji):

| Lokalizacja | Funkcja | Trigger |
| --- | --- | --- |
| dunstart | Start Dungeon | Dungeon Start |
| dunstart+1 | Mythic Skill `Dungeon001StartRelicPulse` | Dungeon Start |
| dunstart+2 | Mythic Skill `Dungeon001MidWaveStart` | Signal `dungeon_001_start_objective_complete` |
| dunstart+3 | Mob Spawner `Dungeon001Warden` | Signal `dungeon_001_mid_objective_complete` |
| dunstart+4 | Finish Dungeon | Signal `dungeon_001_end_boss_defeated` |

Regeneracja + staging:

```powershell
cd C:\Codex\Minecraft
python tools\generate-dungeon-001-functions-yml.py
.\tools\start-staging-qa.ps1 -HeapBudget 2G -StopStrayFirst
cd tools\playtest-bot
npm run playtest:dungeon-001
```

## Co zbadano wcześniej

- Wiki [functions-yml.md](../../.github/agent-knowledge/minecraft-server-docs/mythicdungeons/wiki/md/functions-yml.md) — nadal WIP; hand-edit bez generatora **niezalecany**.
- Bot `md-edit-probe.mjs` — nie zastępuje GUI buildera.
- RCON wyłączony.

## Sygnały MythicMobs (bez zmian)

- `dungeon_001_start_objective_complete` — `Dungeon001StartRelicPulse`
- `dungeon_001_mid_objective_complete` — `Dungeon001MidWaveStart`
- `dungeon_001_end_boss_defeated` — `Dungeon001WardenDeathSignal`

## Human editor (opcjonalnie)

Jeśli generator nie odpala flow w grze: nadal [`MD_EDITOR_CHECKLIST.md`](MD_EDITOR_CHECKLIST.md) (`/md edit` → save). Po edycji w GUI **nie** nadpisuj ręcznie pliku z generatora bez diff review.

## Następny krok

1. Sesja in-game: `/md play dungeon_001` → potwierdź chat (start seal → mid wave → Warden → finish).
2. Jeśli brak triggerów: `/md edit` wizualna korekta połączeń, `/md save`, porównaj z `functions.yml.template`.
3. Po PASS flow: przywróć `fluidHeight: 62` + walkthrough stref (`blue_ice`, `polished_blackstone_bricks`).
