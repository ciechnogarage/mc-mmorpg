# Foundation MVP — Tablica zadań (delegowalna)

Cel: atomowe zadania wykonawcze. Każde ma: domenę odpowiedzialną,
pliki docelowe, wejście (sekcja w `foundation-mvp-spec-values.md`), zależności i
**twarde kryterium akceptacji**. Integrator (Codex/Claude) scala shared-file writes.

Źródła prawdy: `foundation-mvp-spec-values.md` (wartości), `foundation-implementation-status.md`
(stan) i `foundation-long-run-ledger.md` (run).

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

---

## FAZA B — Stolica Wyspy (pełne miasto, M1) · po A1

### B1 — build świata 7 dystryktów — owner `dungeon`/`ui` (FAWE)
- Pliki: `world/` (przez FAWE/struktury, nie blok-po-bloku), `plugins/Multiverse-Core/worlds.yml`.
- Wejście: spec §1 (koordynaty, region `hub_stolica`).
- Akceptacja: gracz spawnuje w Spawn Gate `0,103,0` i dochodzi pieszo do każdego z 6 dystryktów;
  Portal Nexus widoczny z drogi; obszary nie kolidują z dungeonem.

### B2 — 17 NPC + Portal Nexus + board — owner `mobs` (+`ui`)
- Pliki: `plugins/MythicMobs/mobs/hub_npcs.yml`, region/interakcja portalu → menu `foundation_nexus`.
- Wejście: spec §2 (roster, koord, dialog), §3 (portal/board).
- Zależność: B1 (świat istnieje).
- Akceptacja: 17 NPC stoi na koordach z nazwami (AI off); klik Portal Nexus / `PortalKeeper` otwiera
  `foundation_nexus`; board kieruje do Portal Nexus, nie startuje lochu.

### B3 — orientacja spawn — owner `ui`
- Pliki: holo/signs (DecentHolograms), ścieżki.
- Wejście: spec §1, §3.
- Zależność: B1, B2.
- Akceptacja: nowy gracz bez pomocy admina trafia spawn → board → Portal Nexus.

---

## FAZA C — pętla RPG+loch (domknięcie istniejącego, M2–M4) · po A1

### C1 — starter RPG signal — owner `rpg`
- Pliki: `plugins/MMOCore/classes/human.yml`, `plugins/MMOItems/item/`, `foundation_nexus.yml`.
- Wejście: spec §6. **Sprawdzić required-level 6.0 STEEL_SWORD vs nowy gracz lvl 1.**
- Akceptacja: nowy gracz po „pakiet startowy" realnie dostaje gear+skill i zadaje obrażenia (in-game/bot).

### C2 — boss death→finish + difficulty — owner `mobs`
- Pliki: `plugins/MythicMobs/{mobs,skills,droptables}/level1_grove.yml`, `maps/level_1/functions.yml`.
- Wejście: spec §4, §5.
- Akceptacja: realny kill GroveGuardian (bot z A2) odpala finish; droptable daje reward wg §5.

### C3 — entry/exit/reset/return — owner `dungeon`
- Pliki: `plugins/MythicDungeons/maps/level_1/{config,functions}.yml`.
- Wejście: spec §7.
- Akceptacja: pełna pętla wejście→walka→finish→reward→powrót do hubu, bez pollucji template/duplikatów.

---

## FAZA D — gates (M5–M6) · po C

### D1 — permission/economy — owner `economy`
- Wejście: spec §8. Akceptacja: default gracz przechodzi slice, admin-komendy zamknięte.

### D2 — runtime safety — owner `ops`
- Akceptacja: brak nowych critical errors na boot/`latest.log`; znane wyjątki udokumentowane.

---

## FAZA E — QA pełny E2E (M7) · po B, C, D, A2

- owner `qa`, harnessem z A2.
- Akceptacja: `FINAL: PASS` z evidence (log + `_validation/foundation_mvp_qa_<data>.md`),
  zaktualizowany ledger i status.
