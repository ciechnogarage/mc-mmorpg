# 04. Świat, questy, aktywności (dungeony, raidy, lootruny, eventy), transport

Źródła:
- API v3 (`/map/*`, `/map/quests`),
- dane Wynntils Static-Storage (`combat_locations.json`, `lootrun_tasks_named_v2.json`, `services_crowdsourced.json`, `seaskipper_destinations.json`, `item_obtain_v2.json`),
- wiki (Content Book, Raids, Dungeons, Lootrunning, World Events, Mounts, Housing, Fast Travel, Version 2.2.0),
- changelog 2.2.4 (`api/article_581.json`),
- nagrania 2026.

Liczby w `dane/wynntils_statystyki.txt`, `dane/wydarzenia_swiata.csv`, `dane/raids.csv`, `dane/camps.csv`, `dane/znaczniki_mapy.csv`.

---

## 1. Mapa i regiony

- **[F]** Prowincje i obszary: Wynn (start, undead), Gavel (fantasy), Corkus (wyspa, industrial), Dark Forest, Silent Expanse, Sky Islands, Molten Heights, Realm of Light, Canyon of the Lost, **Fruma** (2.2.0, poziomy 105–120). Fruma ma 5 regionów, każdy ze swoim „Sovereign”: Auburn Forest 104–108, Roughworks/Industrial District 108–112, Mistwoods 112–115, Highlands, Aelumia Citadel 118–120. Szósty, The Wastelands, dojdzie później (Version 2.2.0; plansze z filmu Olinus10, 04.04.2026, `klatki/…yhRMvBEvkfg`).
- **[F]** Fruma: „roughly the size of the Wynn Plains”. Mobów tam „grounded in reality compared to Wynn's being themed around the undead and Gavel's being themed on fantasy”. Ma ponad 70 nowych utworów muzycznych.
- **[F]** Znaczniki mapy z API: **1 395** (`dane/znaczniki_mapy.csv`). Rozkład:
  - Content_Cave 162, Content_Quest 112, Content_Miniquest 43, Content_GrindSpot 29, Content_BossAltar 15, Content_UltimateDiscovery 12, Content_Dungeon 10, Content_Raid 4,
  - Merchant_* 511 (Emerald 132, Other 139, Tool 66, Potion 50, Scroll 38, Armour 35, Weapon 34, Accessory 11, Horse 5, KeyForge 1),
  - stacje profesji 8×34–38 = 287, NPC_Blacksmith 63, NPC_ItemIdentifier 43, Trade Market 29,
  - Special_FastTravel 22, SeaskipperFastTravel 20, HousingAirBalloon 23, Rune 4.
- **[F]** Usługi zebrane przez społeczność (Wynntils `services_crowdsourced.json`): Emerald Merchant 133, Booth Shop 109, Blacksmith 65, Tool Merchant 65, **Fast Travel 61**, Potion Merchant 49, Item Identifier 43, 8 stacji rzemieślniczych po 34–38, Party Finder 33, Item Upgrader 29, Housing Balloon 25, Seaskipper 20, Dungeon Merchant 18, Trade Market 17, Mount Enclosure 3.
- **[F]** Zasięg znaczników: x od −2 249 do 1 538, z od −5 622 do −242. Obiekty raidów leżą aż do z −6 438. **[W]** Mapa główna ma około 3,8 × 6,2 tys. bloków.
- **[W]** Na każdą osadę przypada komplet: bank, emerald merchant, blacksmith, identifier, stacje i scroll merchant. Gracz nigdy nie musi daleko chodzić do usług. U nas warto zdefiniować „pakiet miasta” jako szablon.

## 2. Questy

- **[F]** `GET /v3/map/quests` → `{"quests": 303}`. **[W]** To questy i mini-questy razem. Wiki Content Book (stan sprzed Fruma) ma 147 questów i 129 mini-questów, a Fruma dodała 21 questów i 5 mini-questów, co daje ~302.
- **[F]** Parser tabeli Content Book (`dane/questy_content_book.csv`, 145 questów przed Fruma):
  - długość: Long 83, Medium 36, Short 25,
  - trudność: Medium 77, Hard 47, Easy 19,
  - **storyline: 36**,
  - poziomy rozłożone równo, 7–21 questów na każde 10 poziomów, z pikiem na 70–79.
- **[F]** Fruma: **13-częściowa fabuła w 5 rozdziałach** (Queen's Recruit 103 → Apotheosis 119) i 8 pobocznych (Version 2.2.0). Gracz o Fruma: 21 questów na obszar „1,5–1,7× Corkusa, który ma 5 questów” (Reddit 1qztfi8).
- **[F]** Typy celów w questach (wiki Quests): zbieranie, zabijanie, podróż, zagadki, instancje. Są etapy do ~30 („as many as thirty”). Questy „eventowe” zostają w grze na stałe.
- **[F]** Na nagraniach 2026 (`klatki/V28_…`, `V16_…`) widać:
  - wielkie tytuły etapu: „SLAY! / Kill mobs to continue”, „Complete!”,
  - wybory dialogowe (np. Knock / Attack / Wait / Leave),
  - instancje z krzyżówką na podłodze (Olinus10 4:50),
  - teleporty „Warping in 3 / DO NOT MOVE”.
- **[F]** Cutscenki da się pominąć klawiszem zamiany rąk: „PRESS [F] SWAP HANDS TO SKIP 1/4” u góry ekranu. Licznik „1/4” oznacza ujęcia, a pomijanie działa **ujęcie po ujęciu** (`klatki/V04_…`).
- **[F]** Stan 2.2.4: wiele poprawek to „failsafe'y” questów, żeby nie dało się ich zablokować („Added a failsafe to Memory Paranoia to prevent … getting softlocked”, `api/article_581.json`). **[W]** Questy ze skryptami łatwo zablokować. Każdy etap potrzebuje wyjścia awaryjnego (`/kill` i powrót).
- **[F]** Ocena graczy: jakość questów rośnie z poziomem. „The first area in Gavel… my least favorite”, „when you enter Fruma the game literally gets 2-3x as good” (Reddit 1vpd6ij).

## 3. Jaskinie, odkrycia, grind spoty

- **[F]** Jaskinie: **130** (Wynntils `combat_locations`) i 162 znaczniki Content_Cave w API. Typy: Normal, Cave Complex, Boss Cave, Tower, Island. Każda ma skrzynie tier III–IV i teleport wyjściowy (wiki Caves). Fruma dodała 23 jaskinie.
- **[F]** Odkrycia: terytorialne (776 810 XP łącznie), światowe (8,37 mln XP) i **sekretne (98 szt., 38,9 mln XP)** (wiki Experience Points, Content Book).
- **[F]** Grind spoty: 31. Rune Shrines: 2.
- **[W]** Jaskinia to mini-dungeon bez klucza, z nagrodą i wyjściem. Tania treść na dużą skalę. Iris + schematy + MythicMobs spawnery wystarczą.

## 4. World Events (wydarzenia w świecie)

- **[F]** **70 world eventów** (`dane/wydarzenia_swiata.csv`). Poziomy od 3 do 120, najwięcej na 90–119 (27). Trudność: Hard 31, Medium 30, Easy 9. Długość: Medium 32, Long 21, Short 17. Wymagania: poziom (68) i quest (26).
- **[F]** Wydarzenia startują **według timera**. API zwraca w polu `schedule` czas najbliższego startu: w chwili pobrania 13 eventów miało start w ciągu 5 minut. Gracz i jego party dostają **własną instancję ze skalowaniem do najwyższego poziomu w drużynie**. Wygrana, jeśli ktoś przeżyje (wiki World Events).
- **[F]** Nagrody zależą od poziomu gracza („rewardPerLevel”): przedmioty, składniki, przedmiot ekskluzywny, **klucze do dungeonów**. Bomba World Event od gracza uruchamia wszystkie naraz.
- **[F]** **Major World Event: Prelude to Annihilation** — „roughly once every 3 days but it's randomly offset”. Nagrodą są Corrupted Caches z 5 ekskluzywnymi mythikami. Gracze narzekają na lag i godziny: „1. Lag out 2. Crash 3. 2:30 AM” (Reddit 1t3kq50, 321 pkt).
- **[F]** Źródła przedmiotów według Wynntils `item_obtain_v2.json` (853 przedmioty z opisanym źródłem): **worldEvent 377**, merchant 276, unavailable 92, quest 87, event 51, raid 47, gathering 30, tinkering 28, specificMobDrop 21, mobDropRegion 20, caveCompletion 18, dungeon/dungeonMerchant 24, miniboss 7, forgeryChest 7, lootChest 6.
- **[W]** World eventy to główny kanał ekskluzywnego lootu. U nas: MythicMobs + MythicDungeons instancja albo region WorldGuard z falami. Timer z CMI schedule albo MM spawners.

## 5. Dungeony i Forgery

- **[F]** 11 dungeonów (Decrepit Sewers, Infested Pit, Timelost Sanctum, Underworld Crypt, Sand-Swept Tomb, Ice Barrows, Undergrowth Ruins, Galleon's Graveyard, Fallen Factory, Eldritch Outlook, The Forgery) plus wersje **Corrupted** (Wynntils `combat_locations`).
- **[F]** Wejście za **kluczem**, który wpuszcza do 4 graczy. Klucz wypada z rzadkich Key Guardianów w strefie o podobnym poziomie albo z world eventu. Klucze Corrupted są „złamane”, a naprawia się je runami u Key Forge Merchanta w Forgery. Nagrody: emeraldy przy wejściu, fragmenty i Boss Token wymieniany u Dungeon Merchanta. „Second Chance” cofa do startu albo pod bossa (wiki Dungeons).
- **[F]** Krytyka: „dungeons/the forgery really just need a major overhaul because they haven't been touched in so long” (Reddit 1wajxrx).
- **[W]** Model „klucz z moba lub eventu → instancja” pasuje do MythicDungeons (mamy pipeline, `repo-mc-na-pulpicie`).

## 6. Raidy

**[F]** 5 raidów (`dane/raids.csv`, API `/map/raids`):

| Raid | Poziom | Długość | Nagród w puli | W tym mythic / fabled / legendary |
|---|---|---|---|---|
| Nest of the Grootslangs | 54 | Medium | 54 | 3 / 22 / 10 |
| Orphion's Nexus of Light | 79 | Long | 60 | 9 / 22 / 8 |
| The Canyon Colossus | 95 | Medium | 62 | 9 / 23 / 14 |
| The Nameless Anomaly | 103 | Medium | 65 | 9 / 19 / 15 |
| The Wartorn Palace (Fruma) | 119 | Long | 60 | 9 / 20 / 10 |

Struktura (wiki Raids, Wynntils `TheWartornPalaceRaid.java`, klatki):
1. Wymagana **runa** w ekwipunku. Party gotowe do 4 osób, a brakujące miejsca **dopełnia kolejka** (party tymczasowo się łączą). Party złożone z 4 członków jednej gildii może odpalić **Guild Raid** (trudniejszy, np. „Orphion will now receive an additional 25% HP buff”, 2.2.4).
2. **3 pokoje wyzwań, każdy w 2 losowych wariantach**, np. TWP: Grand Aisles / Regal Ballroom, Statuary Hall / Intruding Rifts, The Spire's Shadow. Przed pokojem jest plansza instrukcji z celem (`klatki/V03_…`). Kto zginie, ogląda do końca pokoju jako spectator.
3. Po każdym pokoju jest **pokój buffów**: każdy gracz wybiera 1 z 2 skrzyń (3 od rangi raidowej Rookie III) z losowym z 5 buffów raidu, „Hold to Choose” (`klatki/V15_…`). W tym samym pokoju stoi Blacksmith.
4. Interlude, potem boss z fazami. Raid ma **timer**: start z zapasem, +1 min za każdy pokój, koniec czasu to porażka.
5. **Skalowanie per gracz**: mob ma inne HP dla gracza na 60, a inne na 100 poziomie.
6. **Gambity** (17 typów, np. Anemic's, Glutton's, Hemophiliac's): dobrowolne utrudnienia za lepsze nagrody. Nagrodą są aspekty, tomy, emeraldy i pulle w skrzyni nagród z rerollami.

- **[F]** Zmiany z 2.2.4 pokazują filozofię projektowania: „The room will now end automatically after 90 seconds if at most 1 player is stuck” oraz „Golems now receive a speed multiplier depending on their current HP… IF YOU CAN THINK OF IT? WE MADE IT FASTER” (`api/article_581.json`). **[W]** Twórcy wyrównują czasy pokojów między klasami i tną martwe sekundy.
- **[F]** Gracze z Destiny 2: raidy są „a fair bit simpler”, a mechanika sprowadza się do „X person stands on a platform”. Chcą więcej współpracy (Reddit 1wajxrx).

## 7. Lootruny (solo endgame)

- **[F]** 7 obozów: Canyon of the Lost Excursion (South) 85, Corkus Traversal 90, Molten Heights Hike 95, Sky Islands Exploration 96, Silent Expanse Expedition 101, Fruma Foray West 110 i East 115 (`dane/camps.csv`).
- **[F]** Wymaga wstąpienia do „Lootrun Division” Silverbull Trading Company (poziomy dywizji, XP za run max 500, a z bonusem dziennym 700) oraz ukończenia questów i większości jaskiń regionu (wiki Lootrunning).
- **[F]** Przebieg: gracz wybiera **beacon** (kolorowa wiązka w świecie, 13 kolorów w Wynntils `LootrunBeaconKind`: zielony, żółty, niebieski, fioletowy, szary, pomarańczowy, czerwony, ciemnoszary, biały, aqua, różowy, crimson, rainbow). Każdy kolor ma inny efekt, np. „+1 Boon”, „+1 Reward Reroll”, „+Time & No Mob Buffs”, różowy „+1 Beacon Reroll” (film Olinus10 2023, Version 2.2.0).
- **[F]** Zadania: **137 nazwanych** miejsc na 7 obozów: loot 64, slay 23, defend 19, destroy 16, target 15 (Wynntils `lootrun_tasks_named_v2.json`). Do tego **29 typów misji** (Cleansing Greed, Hoarder, Jester's Trick…), **13 typów triali** (Adrenaline Junkie, Hubris, Lights Out…), klątwy i booni.
- **[F]** Start: tooltip „Start a Lootrun! … temporarily lock your equipment and potions. Daily Bonuses… Min. Challenges: 4, Max. Challenges: 100, Starting Challenges: 12” (`klatki/V29_…`). Okno „Inspect” śledzi stan runu (`klatki/V26_…`).
- **[F]** Nagroda: skrzynia z liczbą „pulls” i rerolli. Pula mythików zmienia się co tydzień.
- **[F]** Run bywa bardzo długi: „casting spell manually as a battle monk for 3h”. Pauza działa przez przelogowanie na inną postać (Reddit 1sm416j, 140 pkt).
- **[W]** Lootrun to proceduralny endgame na istniejącej mapie: te same jaskinie i obszary, nowe zadania i mnożniki. Najlepszy stosunek treści do kosztu. U nas: plugin lub skrypty nad MythicMobs (zadania: zabij N, broń punktu, niszcz obiekt) z punktami na Irisowej mapie.

## 8. Bossy, wyzwania

- **[F]** **15 Boss Altarów** (Bovine Barn, Rotten Passage, Prison of Souls… Deserter's Refuge z Fruma). Boss przyzywany na ołtarzu (wiki, Wynntils).
- **[F]** Legendary Island: 10 „zmechanizowanych” bossów z historii serwera. Śmierć cofa na start. Nagrody w „Legendary Rewards Market” (tier Bronze/Silver/Gold/Diamond akcesoriów).
- **[F]** The Qira Hive, Tower of Ascension: wyzwania-questy z XP skalowanym do poziomu.

## 9. Transport

| Środek | Fakty |
|---|---|
| Teleport scrolls | do miast, kosztują 1 Soul Point (wiki Soul Points); Scroll Merchant w 36 miejscach |
| V.S.S. Seaskipper | statek między 20 portami z progami poziomu 15–105 (Bear Zoo Island 15 … Fruma Gate 105) (Wynntils) |
| Fast travel | 61 punktów: tunele, wozy, windy w Mesie, Nexus (po queście), C.S.S. Sunrise (Relos ↔ Legendary Island) (wiki Fast Travel) |
| Wierzchowce (2.2.0) | 3 typy: Horse (poz. 9, szybki, słaba akceleracja), **Wyvern (poz. 84, lata do limitu wysokości)**, Adasaur (poz. 115, skok, bojowy). 8 statystyk + Training (Speed, Acceleration, Altitude, Energy, Handling, Toughness, Boost, Powerup); karmienie materiałami profesji; hodowla; energia; power-upy w świecie. Kupno 24 EB, trzymane w Enclosure (2–5 slotów według rangi), przywoływane przedmiotem (wiki Mounts, `klatki/V08_…`, `V24_…`) |
| Balon housingu | 23–25 miejsc |
| „Warping in 3, DO NOT MOVE” | odliczanie przed teleportem, ruch przerywa (`klatki/V16_…`) |

- **[F]** Gracze chcieli latać: „As a paladin main I can't wait for flying mounts” (Reddit 1qztfi8). Recenzent nazwał jednak wierzchowce słabym punktem: „MOUNTS SUCK”, „MOUNT GRIND” (plansze w filmie Olinus10).

## 10. Housing i festiwale

- **[F]** Housing: wyspa 80×80, budowa od y 96 do 256. Bloki kupuje się **za materiały profesji**. Są NPC-e do postawienia, Build Stand (zapis buildu) i manekin DPS. Liczba wysp według rangi: 1–5, limity regionów i NPC-ów też według rangi. Są wyspy gildii (wiki Housing, Ranks).
- **[F]** 4 festiwale (Heroes, Bonfire, Spirits, Blizzard). Każdy ma minigrę (np. Blizzard Brawl, Banish the Beyond, Battle for the Bonfire), czasowe skrzynki, kosmetyki i tła menu postaci (plakaty w packu, `klatki/P_m_interface_b.png`).

## 11. Muzyka i dźwięk

- **[F]** „music, composed of noteblocks, can be heard in certain areas” (wiki Music). Oficjalny pack 2.1 **wycisza całą vanillową muzykę** (wszystkie `music.*` → `other/blank`) i dodaje **17 instrumentów** `wynn.instrument.*` (bass, bell, brass, drum, flute, guitar, harp, hat, kora, marimba, piano, pizz, snare, tamborine, tom, vibraphone, woodblock) (`pack/x73144/assets/minecraft/sounds.json`).
- **[W]** Muzykę gra serwer, wysyłając pakiety dźwięku nuta po nucie. Dzięki temu utwór zmienia się w zależności od strefy bez pobierania plików audio, a 70+ utworów Fruma nie powiększa packa. U nas: NoteBlockAPI albo wbudowany odtwarzacz (do sprawdzenia) z własnymi instrumentami w Nexo.

## 12. Wnioski dla nas [W]

1. **Kolejność kosztów treści**: jaskinie i world eventy (tanie, dużo) → questy (drogie, najwięcej chwalone) → dungeony (instancje) → raidy (najdroższe) → lootruny (proceduralne, na istniejącej mapie).
2. **Każda aktywność daje unikalny loot** (world eventy: 377 z 853 opisanych źródeł). Bez tego aktywność umiera.
3. **Instancja per party ze skalowaniem do najwyższego poziomu** eliminuje kradzież moba i trolling. MythicDungeons to umie. World eventy też trzeba instancjonować.
4. **Raid = 3 pokoje × 2 warianty + wybór buffa + timer**: gotowy szablon dla MythicDungeons.
5. **Cutscenki pomijalne ujęciami i failsafe'y questów**: od pierwszego dnia.
