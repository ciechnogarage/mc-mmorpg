# System walki PvE/PvP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Wdrożyć pierwszą konfigurację walki z kosztami staminy, atakiem mocnym, czterema skillami aktywnymi, jednym defensywnym, obrażeniami fizycznymi/żywiołowymi i PvP w regionach WorldGuard.

**Architecture:** MythicLib pozostaje warstwą obliczania obrażeń, zasobów i mitigiacji. MMOItems opisuje statystyki oraz koszty ataków i umiejętności, MMOCore udostępnia pięć slotów klasowych, a MythicMobs przechowuje statusy i wzorce ataków. WorldGuard ogranicza PvP do regionów z `pvp: allow`.

**Tech Stack:** Paper 1.21.11, MythicLib, MMOItems, MMOCore, MythicMobs, WorldGuard, YAML.

**Spec:** `docs/superpowers/specs/2026-09-20-combat-system-design.md`

## Global Constraints

- Obrażenia są fizyczne albo żywiołowe; nie ma osobnej kategorii magicznej.
- Żywioły startowe: ogień, woda, ziemia, powietrze, jasność i ciemność.
- Trucizna jest ziemnym obrażeniem okresowym, a krwawienie fizycznym obrażeniem okresowym.
- Zwykły i mocny atak zużywają staminę; mocny atak działa pod prawym przyciskiem.
- Każda klasa ma 4 sloty aktywne i 1 slot defensywny.
- PvP działa wyłącznie w regionach WorldGuard z `pvp: allow`; party i sojusznicy nie mają friendly fire.

## Review Focus

- Istniejące pliki MMOItems mogą już mieć `stamina-cost`; trzeba nie nadpisać ich przypadkowo.
- Domyślne skille MythicLib mogą używać nazw `magic`; trzeba oznaczyć je jako żywiołowe albo zostawić kompatybilność bez tworzenia nowej kategorii.
- WorldGuard nie tworzy regionu samą konfiguracją świata; trzeba dodać dokumentację komend tworzących arenę.
- Statusy PvP nie mogą trwać dłużej niż 3 s, jeśli pochodzą z kontroli tłumu.
- YAML musi ładować się bez zmiany istniejących przedmiotów i klas.

### Task 1: Ujednolicenie typów obrażeń i zasobów

**Files:**
- Modify: `plugins/MythicLib/config.yml`
- Modify: `plugins/MythicLib/elements.yml`
- Modify: `plugins/MythicLib/stats.yml`

- [ ] Ustawić skille jako fizyczne domyślnie, a obrażenia żywiołowe pozostawić elementom.
- [ ] Ustawić zestaw aktywnych elementów: ogień, woda, ziemia, powietrze, jasność i ciemność.
- [ ] Ustawić limity pancerza, odporności, krytyków, bloku i uniku jako punkty strojenia.
- [ ] Zwiększyć bazową staminę i zachować regenerację zarządzaną przez MMOCore.
- [ ] Sprawdzić składnię YAML parserem dostępnym w środowisku.

### Task 2: Koszt ataków i pięć slotów umiejętności

**Files:**
- Modify: `plugins/MMOItems/item/sword.yml`
- Modify: `plugins/MMOItems/item/axe.yml`
- Modify: `plugins/MMOItems/item/bow.yml`
- Modify: `plugins/MMOItems/item/staff.yml`
- Modify: `plugins/MMOCore/classes/warrior.yml`
- Modify: `plugins/MMOCore/classes/mage/mage.yml`
- Modify: `plugins/MMOCore/classes/rogue.yml`
- Modify: `plugins/MMOCore/classes/marksman.yml`
- Modify: `plugins/MMOCore/classes/paladin.yml`

- [ ] Dodać umiarkowany `stamina-cost` do bazowych broni, bez kasowania istniejących wartości.
- [ ] Dodać umiejętności prawego przycisku z wyższym kosztem staminy dla przykładowych broni.
- [ ] Odblokować cztery sloty aktywne i dodać piąty slot defensywny w każdej klasie.
- [ ] Użyć nazw skillów z nowego pliku MythicMobs zamiast wpisywać mechaniki w klasach.
- [ ] Sprawdzić, że koszt skilla jest odrzucany przy niewystarczającej stamienie.

### Task 3: Statusy i wzorce PvE

**Files:**
- Create: `plugins/MythicMobs/skills/CombatStatusSkills.yml`
- Create: `plugins/MythicMobs/skills/CombatPlayerSkills.yml`
- Create: `plugins/MythicMobs/mobs/CombatTestMobs.yml`

- [ ] Zdefiniować podpalenie i truciznę jako obrażenia żywiołowe.
- [ ] Zdefiniować krwawienie jako obrażenia fizyczne okresowe.
- [ ] Dodać wychłodzenie, osłabienie i krótkie ogłuszenie z czasami z pierwszej wersji.
- [ ] Dodać cztery skille aktywne i jeden defensywny z kosztami staminy/many.
- [ ] Dodać testowego strażnika z zapowiedzią ciężkiego ataku i oknem na kontratak.

### Task 4: PvP WorldGuard i walidacja

**Files:**
- Modify: `plugins/MMOCore/config.yml`
- Create: `docs/combat-pvp-setup.md`

- [ ] Ustawić reguły interakcji MMOCore tak, aby party i sojusznicy nie zadawali sobie obrażeń.
- [ ] Opisać komendy WorldGuard do utworzenia regionu areny z `pvp: allow` oraz bezpiecznego regionu z `pvp: deny`.
- [ ] Opisać test ręczny: PvE, arena PvP, blokada PvP poza areną, party, stamina, 4 skille, defensywa, statusy i odrodzenie.
- [ ] Wykonać walidację składni wszystkich zmienionych YAML-i i sprawdzić log startowy/reload serwera.
