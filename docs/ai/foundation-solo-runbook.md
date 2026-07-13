# Foundation Solo Runbook

Cel: wykonac foundation MVP jako jeden program koordynowany przez Codex, ale
realizowany przez repo-local MC agentow.

## Co Jest Agentem

- `foundation:solo` nie jest agentem. To tylko helper orkiestracyjny.
- Wykonawcami sa repo-local MC agenci uruchamiani przez:

```bash
npm run mc:agent -- <alias> "<task>"
```

- Agentami dla foundations sa:
  - `inventory`
  - `ui`
  - `rpg`
  - `dungeon`
  - `mobs`
  - `economy`
  - `ops`
  - `qa`

## Model Wykonania

- Jeden top-level koordynator: Codex.
- Faktyczne wykonanie idzie przez agentow domenowych `mc:agent`.
- Pierwszy pass jest zawsze `read-only`, nawet dla milestone'ow oznaczonych
  jako `staging-ready`.
- Shared-file writes otwieraja sie dopiero po calym sweepie `M0-M7` i po
  znormalizowaniu wynikow do milestone packetow.

## Kolejnosc Runu

Uruchom helper z `$HOME`:

```bash
npm run foundation:solo
```

Ten helper drukuje kolejke, ale kazdy milestone nadal wykonuje odpowiadajacy
mu agent:

1. `M0` -> `inventory`
2. `M1` -> `ui`
3. `M2` -> `rpg`
4. `M3` -> `dungeon`
5. `M4` -> `mobs`
6. `M5` -> `economy`
7. `M6` -> `ops`
8. `M7` -> `qa`

Filtrowanie helpera:

```bash
npm run foundation:solo -- M3 M4
```

## Milestone Packet

Kazdy wynik agenta trzeba znormalizowac do jednego packetu:

```md
Milestone:
Scope:
Evidence:
Files or areas:
Blockers:
Rejected guesses:
Handoff:
Validation attempted:
Rollback note:
Status update:
```

Wymagane cechy packetu:

- wskazuje lokalne evidence paths
- wskazuje owning plugin/domain agent
- odrzuca zgadywane IDs, placeholders, permission nodes i DB settings
- definiuje smallest proving validation
- zawiera rollback note dla world, economy, permissions, storage i zmian
  player-adjacent

## Fale Wdrozeniowe

Staging work otwieramy tylko w tej kolejnosci:

1. `M1 + M2` - player-path implementation
2. `M3 + M4` - dungeon i encounter slice po potwierdzeniu IDs i rollback notes
3. `M5 + M6` - minimalne economy/permissions gates i runtime hardening
4. `M7` - pass/fail QA evidence

## Gate'y Programu

- `M1` i `M2` moga wejsc do staging dopiero po pelnym `read-only` sweepie.
- `M3` i `M4` moga wejsc do staging dopiero po potwierdzeniu entry, exit,
  reset, mob IDs, model IDs, boss hooks i reward hooks.
- `M5` ma zostac minimalny i wspierac tylko MVP vertical slice.
- `M6` ma potwierdzic, ze znane runtime issues nie pogorszyly sie.
- `M7` zostaje `blocked`, dopoki nie ma pelnego vertical-slice evidence.
