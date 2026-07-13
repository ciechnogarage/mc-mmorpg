# Foundation MVP — Tablica zadań (delegowalna)

Cel: atomowe, delegowalne zadania dla repo-local `mc:agent`. Każde ma: owner-agenta,
pliki docelowe, wejście (sekcja w `foundation-mvp-spec-values.md`), zależności i
**twarde kryterium akceptacji**. Integrator (Codex/Claude) scala shared-file writes.

Źródła prawdy: `foundation-mvp-spec-values.md` (wartości), `foundation-implementation-status.md`
(stan), `foundation-long-run-ledger.md` (run). Reguły wykonania: `foundation-long-run-prompt-pack.md`.

DAG: `A1 ∥ A2` → `B1→B2→B3 ∥ C1,C2,C3` → `D1,D2` → `E`. A2 musi być zielone, by E miało sens.

---

## FAZA A — fundament

### A1 — spec twardych wartości — ✅ DONE (integrator)
Deliverable: `docs/ai/foundation-mvp-spec-values.md`. Zero pól TBD dla zadań B–E.

### A2 — harness walidacji: realna walka — owner `qa`
- Pliki: `MCMMORPG/_validation/foundation_bot.js` (+ ewent. helper combat).
- Wejście: spec §4 (próg death, HP/difficulty), §7 (dungeon), §6 (gear/required-level).
- Zadanie: zastąpić RCON-kill realnym `bot.attack(GroveGuardian)` z pathfindingiem; pętla aż
  `TriggerMythicMobDeath` odpali `FunctionFinishDungeon`; wykryć finish+reward+return z logu/stanu.
- Bramka kosztu: test na **EASY**; jeśli bot ginie/flaky → 1 cykl restartu, potem zmiana metody
  (np. obniżyć required-level broni z §6, albo tymczasowo HP bossa). NIE 3. ślepy strzał.
- Akceptacja: jeden zielony `node MCMMORPG/_validation/play_level1.js` z dowodem w logu
  `TriggerMythicMobDeath: GroveGuardian` → `FunctionFinishDungeon`.
```bash
npm run mc:agent -- qa "A2: przerob MCMMORPG/_validation/foundation_bot.js tak by mineflayer realnie atakowal GroveGuardian (pathfinding + bot.attack, nie RCON /kill) az TriggerMythicMobDeath odpali FunctionFinishDungeon. Uzyj wartosci z docs/ai/foundation-mvp-spec-values.md sekcje 4,6,7. Test na difficulty EASY. Bramka kosztu: max 1 restart serwera przy flaky, potem zmien metode. Akceptacja: zielony 'node MCMMORPG/_validation/play_level1.js' z dowodem w logu TriggerMythicMobDeath->FunctionFinishDungeon. Zwroc touched files, wynik, rollback, blockery."
```

---

## FAZA B — Stolica Wyspy (pełne miasto, M1) · po A1

### B1 — build świata 7 dystryktów — owner `dungeon`/`ui` (FAWE)
- Pliki: `world/` (przez FAWE/struktury, nie blok-po-bloku), `plugins/Multiverse-Core/worlds.yml`.
- Wejście: spec §1 (koordynaty, region `hub_stolica`).
- Akceptacja: gracz spawnuje w Spawn Gate `0,103,0` i dochodzi pieszo do każdego z 6 dystryktów;
  Portal Nexus widoczny z drogi; obszary nie kolidują z dungeonem.
```bash
npm run mc:agent -- dungeon "B1: zbuduj blockout Stolicy Wyspy w swiecie 'world' wg docs/ai/foundation-mvp-spec-values.md sekcja 1 (koordynaty 6 dystryktow + Spawn Gate, os +Z). Uzyj FAWE/struktur, nie reka. Zaloz region WorldGuard hub_stolica z flagami z sekcji 1. Akceptacja: spawn 0,103,0 -> pieszo do kazdego dystryktu, Portal Nexus widoczny z drogi. Zwroc touched files, wynik, rollback, blockery."
```

### B2 — 17 NPC + Portal Nexus + board — owner `mobs` (+`ui`)
- Pliki: `plugins/MythicMobs/mobs/hub_npcs.yml`, region/interakcja portalu → menu `foundation_nexus`.
- Wejście: spec §2 (roster, koord, dialog), §3 (portal/board).
- Zależność: B1 (świat istnieje).
- Akceptacja: 17 NPC stoi na koordach z nazwami (AI off); klik Portal Nexus / `PortalKeeper` otwiera
  `foundation_nexus`; board kieruje do Portal Nexus, nie startuje lochu.
```bash
npm run mc:agent -- mobs "B2: zdefiniuj 17 NPC huba w plugins/MythicMobs/mobs/hub_npcs.yml wg docs/ai/foundation-mvp-spec-values.md sekcja 2 (MythicMobs, NoAI, Persistent, koordynaty, display, dialog). Podlacz fizyczny Portal Nexus/PortalKeeper (sekcja 3) by otwieral istniejace menu DeluxeMenus foundation_nexus. Akceptacja: NPC na koordach z nazwami, klik Portalu otwiera menu. Zwroc touched files, wynik, rollback, blockery."
```

### B3 — orientacja spawn — owner `ui`
- Pliki: holo/signs (DecentHolograms), ścieżki.
- Wejście: spec §1, §3.
- Zależność: B1, B2.
- Akceptacja: nowy gracz bez pomocy admina trafia spawn → board → Portal Nexus.
```bash
npm run mc:agent -- ui "B3: dodaj orientacje spawn->board->Portal Nexus (holo/signs/sciezki) wg docs/ai/foundation-mvp-spec-values.md sekcje 1,3. Akceptacja: nowy gracz bez admina trafia od spawnu do Portal Nexus. Zwroc touched files, wynik, rollback, blockery."
```

---

## FAZA C — pętla RPG+loch (domknięcie istniejącego, M2–M4) · po A1

### C1 — starter RPG signal — owner `rpg`
- Pliki: `plugins/MMOCore/classes/human.yml`, `plugins/MMOItems/item/`, `foundation_nexus.yml`.
- Wejście: spec §6. **Sprawdzić required-level 6.0 STEEL_SWORD vs nowy gracz lvl 1.**
- Akceptacja: nowy gracz po „pakiet startowy" realnie dostaje gear+skill i zadaje obrażenia (in-game/bot).
```bash
npm run mc:agent -- rpg "C1: potwierdz i napraw starter RPG signal wg docs/ai/foundation-mvp-spec-values.md sekcja 6. Kluczowe: STEEL_SWORD ma required-level 6.0 a nowy gracz to lvl 1 - sprawdz czy zadaje pelne obrazenia, jak nie to obniz required-level startowej broni. Akceptacja: nowy gracz dostaje gear+skill i bije bossa. Zwroc touched files, wynik, rollback, blockery."
```

### C2 — boss death→finish + difficulty — owner `mobs`
- Pliki: `plugins/MythicMobs/{mobs,skills,droptables}/level1_grove.yml`, `maps/level_1/functions.yml`.
- Wejście: spec §4, §5.
- Akceptacja: realny kill GroveGuardian (bot z A2) odpala finish; droptable daje reward wg §5.
```bash
npm run mc:agent -- mobs "C2: potwierdz lancuch death->finish GroveGuardian (TriggerMythicMobDeath->FunctionFinishDungeon) i difficulty scaling wg docs/ai/foundation-mvp-spec-values.md sekcje 4,5. UWAGA: testuj realnym killem, /kill nie matchuje triggera. Akceptacja: realny kill odpala finish + reward. Zwroc touched files, wynik, rollback, blockery."
```

### C3 — entry/exit/reset/return — owner `dungeon`
- Pliki: `plugins/MythicDungeons/maps/level_1/{config,functions}.yml`.
- Wejście: spec §7.
- Akceptacja: pełna pętla wejście→walka→finish→reward→powrót do hubu, bez pollucji template/duplikatów.
```bash
npm run mc:agent -- dungeon "C3: potwierdz flow level_1 entry/exit/reset/return wg docs/ai/foundation-mvp-spec-values.md sekcja 7. Akceptacja: wejscie z Portal Nexus -> walka -> finish -> reward -> powrot do hubu, bez duplikatow persisted mobow. Zwroc touched files, wynik, rollback, blockery."
```

---

## FAZA D — gates (M5–M6) · po C

### D1 — permission/economy — owner `economy`
- Wejście: spec §8. Akceptacja: default gracz przechodzi slice, admin-komendy zamknięte.
```bash
npm run mc:agent -- economy "D1: minimalne permission/economy gates dla slice wg docs/ai/foundation-mvp-spec-values.md sekcja 8 (mythicdungeons.play.level_1, menu, vendor/reward przez CMI/Vault). Admin-komendy zamkniete dla default. Akceptacja: default gracz przechodzi slice. Zwroc touched files, wynik, rollback, blockery."
```

### D2 — runtime safety — owner `ops`
- Akceptacja: brak nowych critical errors na boot/`latest.log`; znane wyjątki udokumentowane.
```bash
npm run mc:agent -- ops "D2: potwierdz brak nowych critical errors na boot po zmianach B/C/D. Znane wyjatki (HuskSync off, MCPets YAML) udokumentuj. Akceptacja: czysty boot window w latest.log. Zwroc touched files, wynik, rollback, blockery."
```

---

## FAZA E — QA pełny E2E (M7) · po B, C, D, A2

- owner `qa`, harnessem z A2.
- Akceptacja: `FINAL: PASS` z evidence (log + `_validation/foundation_mvp_qa_<data>.md`),
  zaktualizowany ledger i status.
```bash
npm run mc:agent -- qa "E: pelny E2E vertical slice harnessem z A2: spawn Stolica Wyspy -> orientacja -> board -> Portal Nexus -> level_1 -> moby+boss -> realny kill -> reward -> return. Akceptacja: FINAL: PASS z evidence (log + _validation/foundation_mvp_qa_<data>.md). Zaktualizuj foundation-long-run-ledger.md i foundation-implementation-status.md. Zwroc pass/fail, evidence, blockery."
```
