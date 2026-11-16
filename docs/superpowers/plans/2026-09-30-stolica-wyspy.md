# Stolica Wyspy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Postawić na world1 bezpieczną Stolicę Wyspy i skierować tam gracza po kreatorze.

**Architecture:** Osobny świat VoidGen chroni istniejącą mapę. Odtwarzalny generator bloków tworzy wyspę i miasto. MythicMobs obsługuje NPC, a obecne menu i komendy obsługują usługi. Przełączenie punktów następuje po weryfikacji.

**Tech Stack:** Paper 1.21.11, Multiverse, VoidGen, MythicMobs, DeluxeMenus, WorldGuard, CharacterCreator, MMOProfiles.

**Spec:** `docs/superpowers/specs/2026-09-30-stolica-wyspy-design.md`

## Global Constraints

- Istniejący świat `world` i kreator pozostają bez zmian poza trzema uzgodnionymi punktami docelowymi.
- Nie tworzyć działającego teleportu do niewdrożonej wyspy otwartego świata.
- Nie udawać wyboru klasy: klasa jest już wybierana w kreatorze.
- Wszystkie aktywne usługi opierać na istniejących komendach.

## Review Focus

- Brak świata `stolica` przy starcie: punkty nie mogą zostać przełączone.
- Utrata NPC po restarcie: spawnery muszą je odtworzyć.
- Profil bez postaci: wybór profilu musi odbywać się w bezpiecznym miejscu.
- Niedostępny serwer wyspy: interakcja ma wyjaśniać stan, bez teleportu.
- Wyjście z lochu: gracz wraca do Stolicy, zachowując profil i ekwipunek.

---

### Task 1: Osobny świat i wyspa

**Files:** `tools/stolica/build.py`, `world/datapacks/stolica/`, nowy świat `stolica/`.

- [x] Wygenerować odtwarzalny teren, ulice, Nexus i dzielnice.
- [x] Utworzyć świat VoidGen i wstawić budowle.
- [x] Sprawdzić punkty lądowania, dojścia i brzeg wyspy.

### Task 2: Usługi miasta

**Files:** `plugins/MythicMobs/mobs/Stolica.yml`, `plugins/MythicMobs/skills/Stolica.yml`, `plugins/MythicMobs/spawners/`, `plugins/DeluxeMenus/gui_menus/stolica_*.yml`.

- [x] Podłączyć NPC do istniejących usług i Portalu Nexus.
- [x] Oznaczyć przyszłe przejścia jako niedostępne.
- [ ] Sprawdzić interakcje oraz odtworzenie po restarcie. Spawnery i 11 NPC potwierdzone na żywo; kliknięcia i restart wymagają testu klientem z aktywnym profilem.

### Task 3: Ochrona i przełączenie

**Files:** `plugins/WorldGuard/worlds/stolica/regions.yml`, `plugins/CharacterCreator/config.yml`, `plugins/MMOProfiles/config.yml`.

- [x] Włączyć safe zone i zweryfikować flagi.
- [x] Dopiero po potwierdzeniu świata i NPC zmienić wszystkie trzy punkty.
- [ ] Sprawdzić pełny cykl nowej postaci i wejście/wyjście z Lochu 001. Bot testowy zatrzymuje się na scenie wyboru profilu, bez widocznego okna Minecraft; test tą metodą nie potwierdza tego cyklu.
