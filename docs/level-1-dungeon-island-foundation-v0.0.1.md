# Level 1 Dungeon Island Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje pierwszy grywalny dungeon island jako vertical slice
serwera MCMMORPG. Ma pokazac, jak systemy z fundamentow zaczynaja dzialac razem:
Stolica Wyspy, Portal Nexus, dynamiczna walka, starter skills, loot, profesje,
boss, powrot do huba i progres do wyboru klasy na levelu 10.

Petla:

- Stolica Wyspy.
- Training Grounds.
- Portal Nexus.
- Level 1 Dungeon Island.
- boss, loot, materialy, token/progress.
- powrot do huba.
- repair, salvage, stash, crafting, upgrade.
- progres do levelu 10 i wyboru klasy.

Dokument jest projektowy. Nie definiujemy jeszcze finalnych HP, damage, XP,
drop rate, plugin configow, MythicMobs YAML, loot table ani mapy blok po bloku.

## Core Role

Level 1 Dungeon Island ma byc pierwszym wzorcem dla pozniejszych dungeon islands.

Cele:

- nauczyc gracza podstaw walki bez sciany tekstu,
- pokazac pierwsza pelna petle dungeon -> hub,
- pokazac, ze dungeon daje loot i materialy do dalszej progresji,
- pokazac podstawy profesji i salvagingu,
- przygotowac gracza do levelu 10 i wyboru klasy,
- dac pierwszy boss encounter z czytelnym counterplayem,
- stworzyc model, ktory da sie skalowac na kolejne dungeon islands.

Nie jest celem:

- wymaganie klasy,
- wymaganie party,
- robienie endgame difficulty,
- wrzucenie pelnej ekonomii,
- finalizowanie lore calego regionu.

## Player Flow

### 1. Start w Stolicy Wyspy

Gracz zaczyna w hubie i dostaje podstawowe prowadzenie:

- gdzie sa Training Grounds,
- czym jest Portal Nexus,
- czym sa starter skills,
- czym sa dungeony jako glowna petla progresji,
- ze wybor klasy pojawi sie na levelu 10.

### 2. Training Grounds

Przed pierwszym dungeonem gracz powinien zobaczyc podstawy:

- movement,
- attack,
- dash,
- block,
- parry/counter,
- interrupt,
- ward albo podstawowa defensywa,
- starter skill usage.

Nie trzeba wymuszac perfekcji. Trening ma dac kontekst, a dungeon ma utrwalic
mechaniki w praktyce.

### 3. Portal Nexus

Portal Nexus pokazuje wejscie do `Level 1 Dungeon Island`.

Minimalne informacje:

- dungeon level,
- recommended player count,
- difficulty,
- reward preview,
- quest/progress status,
- ostrzezenie, ze dungeon testuje podstawy walki.

Na start domyslny tryb to `Normalny / Klasyczny`; `Latwy` jest fallbackiem dla casuali.

### 4. Dungeon Run

Dungeon prowadzi gracza przez:

- arrival camp,
- outer path,
- combat trial clearing,
- resource side area,
- elite gatekeeper,
- boss arena,
- reward exit.

### 5. Powrot do huba

Po runie gracz wraca do Stolicy Wyspy i uczy sie:

- sprzedazy smieci,
- repair,
- salvage,
- stash,
- basic crafting,
- basic upgrade,
- przygotowania do kolejnego runu.

### 6. Progres do levelu 10

Kilka runow, lekkie questy/zlecenia albo progres dungeonowy prowadza gracza do
levelu 10.
Na tym etapie gracz powinien juz rozumiec, czy bardziej odpowiada mu melee,
defense, projectile, mobility, stealth, spell, support, debuff, summon albo trap.

## Dungeon Structure

### Arrival Camp

Rola:

- bezpieczny mini-start instancji,
- briefing NPC,
- checkpoint,
- wyjasnienie celu.

Zawartosc:

- entrance NPC albo prompt,
- krotki objective,
- widoczna sciezka do pierwszego encountera,
- opcjonalny portal wyjscia.

Zasada:

- nie zabijac gracza natychmiast po wejsciu.

### Outer Path

Rola:

- proste wejscie w walke,
- nauka spacingu,
- nauka podstawowego ataku i dodge.

Encountery:

- 2-3 weak mobs,
- jeden ranged harasser,
- latwy telegraph do unikniecia.

Zasada:

- pierwszy odcinek ma budowac pewnosc, nie frustracje.

### Combat Trial Clearing

Rola:

- pierwsze miejsce testujace konkretny combat mechanic.

Lekcje:

- block przeciw czytelnemu uderzeniu,
- parry/counter przeciw wolnemu heavy attack,
- interrupt przeciw channelowi,
- line of sight przeciw ranged mobowi.

Zasada:

- gracz moze przejsc bez perfekcyjnego wykonania, ale poprawne wykonanie
  powinno wyraznie pomagac.

### Resource Side Area

Rola:

- pokazanie, ze dungeon daje materialy nie tylko gear.

Zawartosc:

- low-tier ziola,
- natural fragments,
- skory/kosci z mobow,
- kamien albo maly krysztal,
- opcjonalny profession prompt.

Zasada:

- gathering jest opcjonalny, ale widocznie oplacalny.

### Elite Gatekeeper

Rola:

- mini-elite przed bossem,
- sprawdzenie 2-3 lekcji naraz.

Mechaniki:

- heavy attack do dash/block,
- krotki channel do interruptu,
- add albo hazard do target priority.

Zasada:

- mini-elite ma przygotowac do bossa, nie byc sciana trudnosci.

### Boss Arena

Rola:

- pierwszy pelny boss encounter,
- test podstawowego combat loopu.

Boss:

- roboczo `GroveGuardian` / `Straznik Gaju`.

Zasada:

- boss ma byc czytelny, wolniejszy i edukacyjny.

### Reward Exit

Rola:

- nagroda,
- domkniecie runu,
- powrot do huba.

Zawartosc:

- chest,
- token/progress,
- material reward,
- portal powrotny do Stolicy Wyspy.

Zasada:

- gracz po nagrodzie ma naturalnie wiedziec, ze wraca do huba i korzysta z
  repair/salvage/crafting/stash.

## Combat Lessons

Dungeon ma uczyc walki przez encountery.

Lekcje:

- basic attack timing,
- dash jako reakcja na telegraph,
- block jako kontrola incoming damage,
- parry/counter jako nagroda za timing,
- interrupt jako odpowiedz na channel/cast,
- AoE telegraph jako nauka pozycji,
- add priority jako nauka targetowania,
- resource pressure bez wymagania healera,
- starter skill usage bez wyboru klasy,
- party basics bez wymogu party.

Wazne:

- solo gracz musi miec realna droge przejscia,
- party ma miec latwiej przez podzial rol,
- dungeon nie moze wymagac tanka, healera ani konkretnej klasy.

## Enemy Roles

### Weak Swarm Mob

Rola:

- latwy cel,
- nauka cleave,
- nauka spacingu,
- pierwszy test starter skilli.

Counterplay:

- movement,
- light AoE,
- basic attack timing.

### Ranged Harasser

Rola:

- uczy line of sight,
- uczy dodge,
- pokazuje, ze nie kazdego moba bije sie frontalnie.

Counterplay:

- LoS,
- dash,
- block,
- szybkie podejscie.

### Shielded Brute

Rola:

- uczy guard pressure,
- uczy ataku z pozycji,
- uczy parry/counter.

Counterplay:

- guard break,
- atak od tylu,
- parry,
- heavy attack.

### Caster / Channeler

Rola:

- uczy interruptu,
- uczy target priority.

Counterplay:

- interrupt,
- burst,
- line of sight,
- stun/pressure, jesli skill na to pozwala.

### Poison / Root Mob

Rola:

- lekki test cleanse/resist/movement,
- pokazanie status effects bez karania nowych graczy.

Counterplay:

- movement,
- cleanse,
- poison resist,
- szybkie zabicie.

### Mini-Elite Gatekeeper

Rola:

- laczy kilka lekcji,
- przygotowuje do bossa.

Counterplay:

- dash/block,
- interrupt,
- resource management,
- target priority.

## GroveGuardian Encounter

`GroveGuardian` / `Straznik Gaju` jest roboczym pierwszym bossem. Ma byc
czytelny i wolniejszy niz pozniejsze bossy, bo jego glowna funkcja to nauka
mechanik.

### Phase 1

Mechaniki:

- melee slam,
- root telegraph,
- proste addy.

Lekcja:

- dash albo block przeciw slam,
- movement przeciw root field,
- add priority.

### Phase 2

Mechaniki:

- ground AoE,
- summon roots/adds,
- channel heal/shield.

Lekcja:

- pozycjonowanie,
- interrupt,
- target priority,
- timing starter skilli.

### Low HP Enrage-Lite

Mechaniki:

- szybsze telegraphy,
- wieksza presja addow,
- mocniejszy slam, ale bez one-shotu.

Lekcja:

- zachowanie zasobow,
- nie panikowac,
- wykorzystac poznane mechaniki.

### Boss Rules

- Solo gracz moze wygrac przez poprawne uniki i zarzadzanie zasobami.
- Party ma latwiej przez podzial rol, ale boss nie wymaga party.
- Boss nie wymaga klasy, podklasy ani konkretnego buildu.
- Dropy bossa nie moga dawac early one-shot ani niekontrolowanego sustainu.
- Boss ma byc pierwszym target farming hookiem, nie zrodlem best-in-slot.

## Loot, Economy And Professions

Loot ma uczyc itemizacji i powrotu do huba.

### Item Rewards

Rewardy:

- common gear,
- magic gear,
- niski rare chance,
- starter weapon/armor bases,
- basic accessories chance,
- pierwszy boss token albo pity progress.

Zasady:

- loot ma pomagac early progression,
- loot nie jest best-in-slot,
- itemy powinny pokazywac rarity, affixy i stat relevance.

### Materials

Materialy:

- natural fragments,
- korzenie,
- low-tier herbs,
- grzyby,
- skory,
- kosci,
- low-tier kamien,
- maly krysztal,
- magic dust albo essence niskiego tieru.

Powiazania:

- Zielarstwo: ziola/grzyby.
- Lowiectwo: skory/kosci.
- Gornictwo: kamien/krysztal.
- Alchemia: potiony, antidota, poison/cleanse basics.
- Krawiectwo: skory/tkaniny.
- Jubilerstwo: krysztaly.
- Zaklinanie: essence/magic dust.
- Kowalstwo: proste metal/stone components, jesli drop table to wspiera.

### Hub Return Loop

Po runie gracz powinien miec powod, zeby uzyc:

- repair,
- salvage,
- stash,
- basic crafting,
- basic upgrade,
- vendor,
- market later.

Salvage:

- pokazuje item sink,
- oddaje mniej niz item kosztuje,
- moze dac material do pierwszego upgrade.

## Difficulty Model

Source of truth: `docs/dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md`.

### Latwy

Rola:

- baseline MVP,
- solo-friendly,
- tutorialowy.

Zasady:

- najpierw implementowalny wariant,
- najwazniejsze mechaniki czytelne,
- niska kara za blad.

### Normalny / Klasyczny

Rola:

- ten sam teren,
- mocniejsze moby,
- wiecej pressure,
- lepszy rare chance,
- wiecej materialow.

Zasady:

- nie musi zmieniac calej mapy,
- powinien premiowac lepsze execution albo male party.

### Trudny

Rola:

- ten sam teren,
- wiecej mechanik albo affixow,
- lepszy loot,
- realne wymaganie dobrego execution.

Zasady:

- nie powinien byc wymagany do levelowania 1-10,
- nie powinien dawac early PvP broken itemow,
- moze byc pierwszym testem party coordination.

### Bardzo Trudny

Rola:

- challenge tier dla `loch_001`,
- dodatkowe mechaniki, pulapki albo boss behavior,
- najlepsze rewardy tego lochu bez psucia early PvP.

Zasady:

- nie moze byc tylko HP/damage scalingiem,
- moze dodawac moby, pulapki, objective, arena hazards albo affix-style pressure,
- nadal nie wymaga konkretnej klasy, podklasy albo party.

## Integration

Ten dokument musi pozostac spojny z:

- docs/stolica-wyspy-hub-foundation-v0.0.1.md.
- docs/dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md.
- docs/quest-contract-objective-foundation-v0.0.1.md.
- docs/loot-reward-table-001-foundation-v0.0.1.md.
- docs/mob-boss-encounter-001-foundation-v0.0.1.md.
- docs/discovery-npc-board-loop-001-foundation-v0.0.1.md.
- docs/combat-foundation-v0.0.1.md.
- docs/starter-skills-class-progression-foundation-v0.0.1.md.
- docs/professions-foundation-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.
- docs/economy-crafting-loot-foundation-v0.0.1.md.
- docs/player-journey-milestone-roadmap-v0.0.1.md.
- docs/world-content-loop-foundation-v0.0.1.md.

W szczegolnosci:

- `Stolica Wyspy` okresla Portal Nexus i hub return loop.
- `quest-contract-objective` okresla zlecenia, drop questy, dungeon objective,
  bounty i reward wrappers dla tego contentu.
- `loot-reward-table-001` okresla pierwsze dropy, materialy, tokeny, boss
  rewardy i PvP sanity check dla `loch_001`.
- `mob-boss-encounter-001` okresla roster mobow, behavior scaling trudnosci,
  Elite Gatekeeper i GroveGuardian encounter.
- `discovery-npc-board-loop-001` okresla, jak gracz odkrywa Portal Nexus,
  boardy, uslugi huba i powrot z lochu bez liniowego tutorialu.
- `combat` okresla dash, block, parry, counter, interrupt, ward i resource
  pressure.
- `starter skills` okreslaja style testowane przed level 10.
- `professions` okresla gathering, crafting i salvaging.
- `itemization` okresla rarity, affixy, materialy i PvP sanity checks.
- `economy` okresla tokeny, pity, sinks i market return loop.
- `player journey` okresla progres level 1-10 i wybor klasy na level 10.
- `world-content` okresla role dungeonow jako glownej petli PvE.

## Out Of Scope

Nie robimy jeszcze:

- kolejnych dungeon islands,
- finalnych plugin configow,
- MythicMobs YAML,
- loot table,
- boss script,
- finalnych wartosci HP/damage/XP/drop rate,
- PvPvE dungeonow,
- finalnego lore regionu,
- wymogu party,
- wymogu klasy albo podklasy.

## Test Cases

Level 1 Dungeon Island v0.0.1 powinien przejsc ponizsze scenariusze:

- Nowy gracz rozumie, jak wejsc do dungeonu przez Portal Nexus.
- Solo gracz moze przejsc `Latwy` po nauczeniu sie podstaw walki.
- Party ma korzysci z podzialu rol, ale nie jest wymagane.
- Dungeon uczy dash, block, parry, counter i interrupt bez sciany tekstu.
- Boss jest czytelny i ma counterplay dla kazdej glownej mechaniki.
- Loot po runie naturalnie prowadzi do repair, salvage, stash, crafting albo
  upgrade w hubie.
- Gathering/material side area pokazuje profesje bez robienia z nich obowiazku.
- Kilka runow, zlecenia albo progress dungeonowy pomaga dojsc do levelu 10 i
  wyboru klasy.
- Dropy nie psuja PvP early game.
- `Normalny / Klasyczny`, `Trudny` i `Bardzo Trudny` sa opisane jako skalowanie
  tego samego dungeon island.

## Assumptions

- `Stolica Wyspy` jest glownym hubem startowym.
- `Portal Nexus` jest domyslna brama do dungeon islands.
- `Level 1 Dungeon Island` jest pierwszym wzorcem dla pozniejszych dungeonow.
- `GroveGuardian` / `Straznik Gaju` jest roboczym pierwszym bossem.
- Pierwsza implementacja moze skupic sie na `Latwy` i `Normalny / Klasyczny`.
- `Trudny` i `Bardzo Trudny` sa modelem docelowym, nie wymaganiem pierwszego
  wdrozenia.
- Dokument jest projektowy i nie zmienia konfiguracji serwera.
