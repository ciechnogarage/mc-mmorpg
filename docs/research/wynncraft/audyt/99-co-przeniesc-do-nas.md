# 99. Co przenieść do nas: tabela decyzyjna

Stack world1 (sprawdzone `ls plugins/`, tylko odczyt): Paper 1.21.9+/1.21.11, MMOCore, MMOItems, MMOInventory, MMOProfiles, MythicLib, MythicMobs, MythicCrucible, ModelEngine (+MEG-Molang), MythicDungeons, MythicHUD, DeluxeMenus, Nexo, CMI, Iris, Guilds, DecentHolograms, LibsDisguises, CoreTools, WorldGuard, LuckPerms, PlaceholderAPI, ProtocolLib, PacketEvents, voicechat, własne CharacterCreator i MenuKey.

Oznaczenia:
- **Koszt**: S (dni, konfiguracja), M (1–2 tyg., konfiguracja + grafika albo mały kod), L (tygodnie, własny plugin lub dużo treści), XL (miesiące).
- **Priorytet**: P1 (fundament, najpierw), P2 (wyraźnie podnosi jakość), P3 (później lub przy skali).

Elementy kreatora i menu postaci są w `../4-jak-zbudowac-u-nas.md`. Tu nie powtarzam.

**Uwaga [W]:** każdy wiersz „jak u nas” to hipoteza do sprawdzenia w oficjalnej dokumentacji pluginu przed wdrożeniem (reguła `zawsze-sprawdzac-dokumentacje`). Nic z tego nie było testowane w tej rundzie.

---

## A. Walka i postać

| # | Element | Co robi Wynn | Jak u nas | Koszt | Prio |
|---|---|---|---|---|---|
| A1 | Spelle z kombinacji 3 klików | 4 spelle na klasę, wzorce R-L-R, R-R-R, R-L-L, R-R-L (łuk odwrócony), kombinacja w action barze glifami myszy | MMOCore `skill-casting.mode: KEY_COMBOS` (dziś mamy `SKILL_BAR`), glify L/R w Nexo, wyświetlanie przez MythicHUD | M | P1 |
| A2 | Ultimate pod F | 3 ultimate'y na klasę (poz. 90), ładowane meterem z użycia umiejętności | F (SWAP_HANDS) jest dziś wejściem w casting MMOCore. Decyzja: przy A1 F się zwalnia. Meter jako zmienna MythicLib + pasek w MythicHUD | M | P2 |
| A3 | Rosnący koszt przy spamie | koszt many rośnie przy powtórce, reset po 3 s | zmienna per-skill (mechanika MythicMobs `setvariable`, wywoływana ze skilla MMOCore/MythicLib) + formuła kosztu | S | P2 |
| A4 | 5 atrybutów, 2 pkt/poziom, max 200, wymagania na przedmiotach | STR/DEX/INT/DEF/AGI z żywiołami, zwroty maleją, wymagania sumowane z bonusami | MMOCore attributes + MMOItems `required-*`. Darmowy reset w mieście (NPC + komenda) | M | P1 |
| A5 | Drzewko ~90 węzłów, 50 AP, archetypy z progami, blokady | 9 stron, koszt 1–2 AP, gracz bierze ~1/3 | MMOCore skill-trees. Grafika połączeń jak w rundzie 4, §5. Progi archetypu do sprawdzenia w docs | L | P2 |
| A6 | Aspekty z duplikatów | 128 aspektów, tiery za 1/5/30/150 kopii, 5 slotów | MMOItems token + skrypt/MythicLib modyfikujący skill. Źródło: raid (MythicDungeons) | L | P3 |
| A7 | Krzywa XP | +10–15%/poziom, lvl 121 = koszt 1–120 | `exp-curves/*.txt` w MMOCore (`mmocore-profesje-klucze`) | S | P1 |
| A8 | Liczby obrażeń, nameplate z HP i statusami, fazy bossa | glify cyfr i ikon, style nameplate'ów | MythicMobs + ModelEngine nameplate, ikony w Nexo. Liczby obrażeń: MythicLib indicators albo DecentHolograms | M | P2 |
| A9 | 26 debuffów z ikonami | Bleeding, Static, Corroded… | MythicMobs auras + ikona w nazwie/nameplate | M | P3 |
| A10 | Soul Points (kara za śmierć) | −2 SP za śmierć, ryzyko utraty przedmiotów przy 0, odnawianie o świcie | MMOCore resource albo PAPI + CMI/MythicMobs przy śmierci. Decyzja projektowa | M | P3 |

## B. Przedmioty i crafting

| # | Element | Co robi Wynn | Jak u nas | Koszt | Prio |
|---|---|---|---|---|---|
| B1 | Losowe ID z zakresu 30–130% | 90 typów ID, 5–7 ID na przedmiot | MMOItems random stats (templates) | M | P1 |
| B2 | Niezidentyfikowany drop + Identifier | opłata za ID, re-ID ×5 | MMOItems „unidentified” + NPC (DeluxeMenus/komenda MMOItems) | S–M | P1 |
| B3 | Tooltip z ✔/✘ wymagań, miernikiem rolla, socketami | nowy wygląd 2.2 | MMOItems lore format + glify Nexo; `tooltip_style` 1.21.2+ | M | P1 |
| B4 | Rzadkości z kolorem ramki i glinta | 6 rzadkości, glint w kolorze | MMOItems tiers + Nexo (glint/tint), definicje modeli `select` | S | P1 |
| B5 | Gniazda (powdery) z kolejnością i „special” | 5×7 powderów, pierwszy decyduje o specjalu | MMOItems gem stones (sloty kolorów). Special: skrypt | M | P2 |
| B6 | Crafting z recepturą i swobodnymi składnikami | 720 receptur po 2–3 poziomy, 6 składników, modyfikatory pozycji | MMOItems crafting stations (stałe receptury). Swobodne składniki: własny plugin | M (stałe) / L (swobodne) | P2 |
| B7 | 12 profesji, 52 surowce co 10 poziomów, 16 787 węzłów | tiery jakości 70/10/1% | MMOCore professions + węzły z Irisa (bloki Nexo, `iris-generuje-bloki-nexo`) | L | P2 |
| B8 | Jeden surowiec, wiele zastosowań | crafting + wierzchowce + housing | projekt ekonomii | S | P1 |
| B9 | Set bonusy | 71 setów | MMOItems item sets | S | P2 |
| B10 | Mythic ze wzmacnianiem (ascension) | 30% szansy, druga kopia = 100% | MMOItems upgrading (upgrade templates z szansą) | M | P3 |

## C. Świat i aktywności

| # | Element | Co robi Wynn | Jak u nas | Koszt | Prio |
|---|---|---|---|---|---|
| C1 | Jaskinie (130) | mini-dungeon bez klucza, skrzynie T3–T4, teleport wyjściowy | Iris + schematy + MythicMobs spawnery + skrzynie (MMOCore loot-chests) | M na sztukę | P1 |
| C2 | World eventy na timerze, instancja per party | 70 eventów, skalowanie do najwyższego poziomu, ekskluzywny loot, klucze | MythicDungeons (instancja) albo region WorldGuard + MythicMobs fale. Timer: CMI schedule | L | P2 |
| C3 | Dungeon za klucz z moba lub eventu, do 4 osób | 11 + corrupted | MythicDungeons (mamy pipeline Iris imagemap + MD, `repo-mc-na-pulpicie`) | L | P1 |
| C4 | Raid: 3 pokoje × 2 warianty, pokój buffów, timer, kolejka dopełniająca | 5 raidów, skalowanie per gracz | MythicDungeons (funkcje, bez edytora: `mythicdungeons-funkcje-bez-edytora`) + MythicMobs. Kolejka: plugin party finder (brak, do wyboru) | XL | P3 |
| C5 | Lootrun proceduralny | beacony w kolorach, zadania (loot, slay, defend, destroy, target), misje i triale, skrzynia pulli | własny plugin nad MythicMobs + punkty zadań na mapie | XL | P3 |
| C6 | Questy z dialogami, cutscenkami, failsafe'ami | ~303 questy i mini, 36 storyline | MMOCore quests (proste) albo dedykowany plugin questów (do wyboru). Dialog: patrz D3 | XL (treść) | P1 |
| C7 | Śledzenie questa: wiązka + dystans + sidebar | zielony romb z wiązką, „Tracked Quest” | MythicHUD (sidebar/kompas) + BlockDisplay wiązka (do sprawdzenia) | M | P1 |
| C8 | „Pakiet miasta” | bank, emerald merchant, blacksmith, identifier, 8 stacji, scroll merchant w każdej osadzie | szablon schematu + NPC (MythicMobs/CMI) | M | P1 |
| C9 | Transport: scrolle, statki z progiem poziomu, szybkie podróże po questach | 61 fast travel, 20 portów | MMOCore waypoints (mamy katalog `waypoints/`) + CMI warps z permisją z questa | S–M | P1 |
| C10 | Wierzchowce z 8 statystykami, karmieniem, lataniem | 3 typy, 24 EB | MythicMobs + ModelEngine mount (latający wyvern: sprawdzić ME mount controllers) | L | P3 |
| C11 | Housing | wyspy 80×80, bloki za materiały | osobny świat + WorldGuard/plugin wysp | L | P3 |
| C12 | Festiwale 4×/rok | minigry, kosmetyki, tła menu | treść + CMI/MythicMobs eventy | M na festiwal | P2 |
| C13 | Muzyka z nut per strefa | noteblocki, 17 instrumentów, vanillowa muzyka wyciszona | NoteBlockAPI (sprawdzić) + instrumenty w Nexo + WorldGuard region music | M | P2 |
| C14 | Tytuły lokacji i napisy 3D | „The Province of Fruma”, „MOUNT ENCLOSURE…” | title przy wejściu w region (WorldGuard + CMI) + `text_display` (DecentHolograms/CMI holo) z fontem Nexo | S | P1 |

## D. UI i UX

| # | Element | Co robi Wynn | Jak u nas | Koszt | Prio |
|---|---|---|---|---|---|
| D1 | Okno = obiekt ze świata | stragan, stół, tablica, sejf, kufer | glify Nexo w tytule DeluxeMenus (już to robimy), osobny glif tła i napisu | M (grafika) | P1 |
| D2 | HUD z glifów | paski HP/many, level w emblemacie, hotbar, meter ultimate | MythicHUD (ma nexo-hook i shader tekstu) | M | P1 |
| D3 | Dialog NPC z portretem i stylem frakcji | ramka nad hotbarem, SHIFT, wybory scrollem, historia | action bar z glifami (MythicHUD) albo dialog Papera (poza walką). **Nie zasłaniać HUD-u w walce, pomijanie od razu** | L | P2 |
| D4 | Sidebar celu | Tracked Quest, Daily Objective, Guild Obj., party HP | MythicHUD / scoreboard (CMI) z PAPI | S | P1 |
| D5 | Hologramy „Click to…” z ikoną myszy i ✔/✘ wymagań | przy stacjach i węzłach | DecentHolograms + glify Nexo + PAPI warunki | S | P1 |
| D6 | Tytuły ekranowe | YOU DIED, Warping in 3, SLAY!, Complete! | CMI/MythicMobs `title` z glifem tła | S | P1 |
| D7 | Plakaty nowości i festiwali przy wejściu | okno-pergamin raz po aktualizacji | DeluxeMenus + flaga LuckPerms meta | S | P2 |
| D8 | Sezonowe logo i tła menu | warianty logo w selektorze | glify Nexo per event, przełączane configiem | S | P3 |
| D9 | Content Book jako hub treści z filtrami i śledzeniem | ~1 290 pozycji „content” | DeluxeMenus z PAPI (MMOCore quests) + filtr. Uwaga na przytłoczenie: tryb „polecane” | L | P2 |
| D10 | Powiadomienia „NEW!” i kafelki klawiszy | tile/notify, swap F | glify Nexo | S | P2 |

## E. Społeczność, ekonomia, monetyzacja

| # | Element | Co robi Wynn | Jak u nas | Koszt | Prio |
|---|---|---|---|---|---|
| E1 | Party z podziałem XP 20% i party finder z kolejką | typy party, do 10/16 osób | MMOCore party (XP share w configu). Finder: DeluxeMenus + komendy albo plugin | M | P2 |
| E2 | Gildie od niskiego poziomu, wojny od poziomu gildii | 4 EB, poz. 5; wojny od poz. gildii 50 | plugin Guilds (mamy). Terytoria i wojny nie istnieją: osobny projekt | S (gildie) / XL (terytoria) | P2 / P3 |
| E3 | Sezony z nagrodami progowymi | 30 progów SR, odznaki | zależne od E2 | L | P3 |
| E4 | Rynek z ofertami kupna/sprzedaży i price-checkiem | 5% opłaty, 2-tyg. średnia, Booth shops | brak pluginu rynku w stacku: wybrać (do researchu) | M | P2 |
| E5 | Upusty emeraldów | bank pages do 1,5 mln E, ID, teleporty, naprawy, mounty | ceny w configach MMOCore/MMOItems/CMI | S | P1 |
| E6 | Emeraldy w jednostkach 1/64/4096 | fizyczne przedmioty + sakiewka | CMI economy (wirtualna) albo przedmioty MMOItems. Decyzja projektowa | S | P1 |
| E7 | Bomby globalne | XP/loot/profesje ×2 dla całego świata, podziękowanie publiczne | przedmiot MMOItems/CMI → globalny booster MMOCore + broadcast | S | P2 |
| E8 | Waluta premium wymienialna na walutę gry | shares 1 USD, na rynku za emeraldy | druga waluta (np. PlayerPoints, do sprawdzenia) + rynek z E4 | M | P3 |
| E9 | Rangi kosmetyczno-wygodowe | sloty, magazyn, totemy, tła, kolory | LuckPerms + MMOProfiles sloty. **Bez pożyczonych AP** | S | P2 |
| E10 | Statystyki postaci jak w API Wynna | logins, blocksWalked, deaths, quests[], raidStats… | PAPI + MMOProfiles/MMOCore stats, eksport do WWW | M | P2 |
| E11 | Zasady i lista modów | makra 1 spell/klik, zakaz AFK, lista dozwolonych modów | regulamin (CMI /rules), mody dozwolone: voicechat, minimapy bez radaru | S | P1 |

## F. Technika

| # | Element | Co robi Wynn | Jak u nas | Koszt | Prio |
|---|---|---|---|---|---|
| F1 | Definicje modeli 1.21.4 z `range_dispatch`/`select` | cały katalog przedmiotów na `potion` + `leather_*` z CMD | Nexo `item_model` / definicje z tintami (runda 4, §4) | S–M | P1 |
| F2 | Dwa wyglądy modelu (GUI vs świat) | shader perspectiveModel (alfa 252/253) | `select` po `display_context` (bez shadera) | S | P2 |
| F3 | VFX spelli jako sekwencje modeli na `item_display` | Judrajim 106 klatek, sprite'y 128–256 px | MythicMobs + Nexo/ModelEngine, na start 3–6 klatek + `.mcmeta` | M–L | P2 |
| F4 | Świecące części modeli | alfa 254 = emissive (shader) | od 1.21.? `light_emission` w elementach modelu (sprawdzić wersję) albo shader | S | P3 |
| F5 | Muzyka z nut | 0 MB w packu | patrz C13 | M | P2 |
| F6 | Wiele małych światów + proxy z transferem | 52 światy, mediana 13 graczy | nie teraz (zakres: tylko world1) | – | P3 |
| F7 | Aktualizacje falami bez restartu globalnego | „available on all worlds in 8 hours” | przy jednym świecie: okno serwisowe + zapowiedź | – | P3 |
| F8 | Publiczne API statystyk | 40 endpointów, OAuth2, prywatność per gracz | później: eksport PAPI → WWW | L | P3 |

---

## Proponowana kolejność [W]

1. **Fundament (P1)**:
   - A1 (kombinacje) i A4 (atrybuty),
   - B1–B4 (przedmioty z rollami i tooltipem),
   - C1, C3, C6 w małej skali (jaskinie, dungeon, questy),
   - C7–C9, C14 (prowadzenie gracza, miasta, transport, tytuły),
   - D1, D2, D4–D6 (okna, HUD, sidebar, hologramy, tytuły),
   - E5, E6, E11 (ekonomia i zasady),
   - F1.
2. **Jakość (P2)**:
   - A2, A3, A5, A8 (ultimate, koszt, drzewko, nameplate),
   - B5–B7, B9,
   - C2 (world eventy), C12, C13,
   - D3 (dialogi), D7, D9, D10,
   - E1, E4, E7, E9, E10,
   - F2, F3, F5.
3. **Endgame i skala (P3)**:
   - A6 (aspekty), C4 (raidy), C5 (lootruny), C10 (wierzchowce), C11 (housing),
   - E2 z terytoriami, E3, E8,
   - F4, F6–F8.
