# Discovery, NPC & Board Loop 001 Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje minimalistyczna warstwe interakcji dla pierwszego loopu
gry: oznaczenia miasta, NPC, boardy, portale, menu, statusy i krotkie
komunikaty. Celem nie jest prowadzenie gracza za reke. Celem jest dac mu
czytelne punkty orientacyjne, zeby mogl sam odkrywac Stolice Wyspy, lochy,
zlecenia, profesje, uslugi huba i wybory klasowe.

Gracz ma czuc, ze eksploruje miasto MMO, a nie idzie liniowym tutorialowym
korytarzem.

Dokument jest projektowy. Nie definiujemy finalnego pluginu menu, GUI slotow,
NPC dialogow, Citizens/DeluxeMenus configs ani cen/cooldownow.

## Core Philosophy

Zasady:

- hub jest miastem MMO, nie samouczkiem krok po kroku,
- NPC istnieja, bo pelnia funkcje w swiecie,
- boardy organizuja content, ale nie zastapuja gry,
- gracz ma prawo troche poszukac i odkryc systemy sam,
- krytyczne systemy musza byc widoczne przez layout, oznaczenia i logiczne
  rozmieszczenie,
- komunikaty maja byc rzadkie, krotkie i mechaniczne,
- brak ciaglego “idz teraz tutaj”,
- brak NPC, ktory po kazdym kroku tlumaczy nastepny.

Minimum wyjasnien jest potrzebne tylko dla:

- wejscia do lochu,
- wyboru klasy na levelu 10,
- repair/salvage/stash/upgrade,
- reward claim,
- waznych ostrzezen: PvP risk, wysoka trudnosc, irreversible choices.

## Required Interaction Points

### City Signposts

Rola:

- oznaczyc glowne dzielnice bez prowadzenia gracza za reke.

Minimalne oznaczenia:

- Portal Nexus,
- Market,
- Professions,
- Class District,
- PvP Arena,
- Temple District,
- Guild District,
- Bank / Stash.

Zasada:

- nazwy dzielnic i czytelne drogi sa lepsze niz NPC tlumaczacy wszystko.

### Optional Help NPC

Rola:

- jeden NPC informacyjny dla gracza, ktory chce zapytac, gdzie co jest.

Powinien:

- wskazac dzielnice,
- powiedziec, gdzie sa portale, klasy, market, profesje i arena,
- nie prowadzic calego procesu krok po kroku,
- nie blokowac wejscia do dungeonu.

### Portal Nexus

Rola:

- wejscie do dungeon islands,
- podstawowy wybor lochu i difficulty.

Pokazuje:

- dungeon name,
- level,
- difficulty,
- solo/party recommendation,
- broad reward types,
- start run,
- locked reason, jesli content jest locked.

Nie pokazuje:

- pelnego drop rate,
- kompletnej strategii przejscia,
- checklisty wszystkich zlecen.

### Quest Board

Rola:

- lekkie zlecenia powiazane z content source.

Pokazuje:

- quest name,
- content source, np. `loch_001`,
- target,
- amount,
- reward type,
- repeat rule, jesli dotyczy.

Zasada:

- gracz moze wejsc do lochu bez pobrania wszystkich zlecen.

### Profession Board

Rola:

- zlecenia gathering/crafting/salvage,
- pokazanie, ze profesje istnieja i maja materialy z contentu.

Pokazuje:

- order type,
- material albo craft target,
- profession link,
- reward type.

Zasada:

- profesje sa odkrywane przez materialy, boardy i crafting stations, nie przez
  obowiazkowy tutorial.

### Market Services

Rola:

- praktyczne uslugi po powrocie z lochu.

Elementy:

- Repair NPC/station,
- Salvage Station,
- Basic Upgrade Station,
- Stash,
- Vendors,
- material storage albo token view, jesli system to wspiera.

Zasada:

- po powrocie z lochu gracz powinien naturalnie trafic blisko uslug, ale system
  nie musi wciskac mu kazdej po kolei.

### Class District

Rola:

- mentorzy klas,
- class preview,
- wybor klasy na levelu 10.

Zasada:

- przed level 10 wybory sa locked i jasno mowia dlaczego,
- na levelu 10 gracz moze znalezc mentorow i zdecydowac,
- system moze sugerowac klase na podstawie starter styles, ale tylko jako
  opcjonalna podpowiedz.

### Dungeon Return Point

Rola:

- punkt powrotu z `loch_001`,
- blisko market/stash/repair/salvage/upgrade.

Zasada:

- po clearze gracz wraca w logiczne miejsce, ale bez wymuszonego touru po
  kazdym NPC.

## Menu Requirements

### Portal Nexus Menu

Minimalne pola:

- dungeon name,
- dungeon level,
- difficulty,
- party recommendation,
- broad reward types,
- start,
- locked reason.

Przyklad:

- `loch_001`
- Tier 1
- Normalny / Klasyczny
- Solo-friendly
- Rewards: early gear, wolf fang, rough hide, weak essence, grove token chance

`Latwy` jest widoczny jako fallback dla graczy, ktorzy chca spokojniej nauczyc
sie mechanik, ale nie jest domyslnym trybem.

### Quest Board Menu

Minimalne pola:

- quest name,
- content source,
- target,
- amount,
- reward type,
- repeat rule.

Przyklad:

- `Kly z Lochu 001`
- Source: `loch_001`
- Target: wolf-type mobs
- Amount: 10 wolf fang
- Reward: XP, gold, material cache

### Profession Board Menu

Minimalne pola:

- order type,
- material/craft target,
- linked profession,
- reward type.

Przyklad:

- Gather: corrupted root
- Source: `loch_001`
- Profession: Alchemia / Zaklinanie
- Reward: profession XP, material cache

### Salvage Menu

Minimalne pola:

- selected item,
- expected material category,
- confirmation.

Wazne:

- menu moze ostrzec, ze salvage oddaje mniej wartosci niz crafting zuzywa,
  ale nie powinno spamowac gracza przy kazdym smieciowym itemie.

### Repair Menu

Minimalne pola:

- damaged item,
- cost,
- confirm.

### Basic Upgrade Menu

Minimalne pola:

- item,
- required material,
- broad result preview,
- confirm.

### Stash Menu

Minimalne pola:

- item storage,
- material storage,
- token/currency view.

### Class Mentor Menu

Minimalne pola:

- class name,
- short style preview,
- requirements,
- locked/unlocked state.

Zasada:

- nie wciskac graczowi jednej “najlepszej” klasy.

## Message Rules

Komunikaty maja byc rzadkie, konkretne i mechaniczne.

Typy:

- `Accepted`: zlecenie przyjete.
- `Progress`: tylko gdy ma sens, np. `Wolf Fang 3/10`.
- `Complete`: cel gotowy do oddania.
- `Locked`: brak levelu, materialu albo odblokowania.
- `Warning`: inventory full, wysoka trudnosc, PvP risk, wazny wybor.
- `Reward`: nagroda odebrana.

Zakazy:

- brak stalego prowadzenia po hubie,
- brak instrukcji po kazdym kroku,
- brak narratora tlumaczacego oczywistosci,
- brak wymuszania jednej trasy przez miasto.

## Discovery Flow 1-10

### Level 1

Gracz:

- pojawia sie w Stolicy Wyspy,
- widzi glowny plac i oznaczenia dzielnic,
- moze znalezc Training Grounds, Quest Board albo Portal Nexus.

### Level 1-3

Gracz:

- testuje podstawy walki,
- znajduje pierwsze zlecenia albo od razu idzie do `loch_001`,
- uczy sie, ze boardy wskazuja content source.

### Level 3-5

Gracz:

- przez loot i materialy odkrywa repair, salvage, stash i basic upgrade,
- moze skorzystac z Profession Board,
- zaczyna rozumiec, ze materialy maja zastosowania.

### Level 5-9

Gracz:

- wraca do `loch_001`,
- testuje zlecenia, dropy, boss progress i difficulty preview,
- moze zaczac interesowac sie buildem przed level 10.

### Level 10

Gracz:

- odblokowuje realny wybor klasy,
- moze isc do Class District,
- dostaje opcjonalna sugestie na podstawie starter styles,
- wybiera klase bez automatycznego prowadzenia za reke.

## Design Rules

Zasady:

- minimalna liczba NPC potrzebna do funkcji,
- NPC nie istnieje tylko po to, zeby powtarzac oczywistosci,
- menu ma byc czytelne, ale nie musi tlumaczyc calej gry,
- gracz moze wejsc do lochu bez wszystkich zlecen,
- reward preview pokazuje typy nagrod, nie pelne drop rate,
- locked content wyjasnia wymaganie jednym zdaniem,
- discovery jest czescia funu,
- krytyczne systemy nie moga byc ukryte bez sensu,
- achievement UI, endgame UI i pelne tutoriale sa poza zakresem.

## Integration

Ten dokument musi pozostac spojny z:

- docs/stolica-wyspy-hub-foundation-v0.0.1.md.
- docs/level-1-dungeon-island-foundation-v0.0.1.md.
- docs/dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md.
- docs/quest-contract-objective-foundation-v0.0.1.md.
- docs/loot-reward-table-001-foundation-v0.0.1.md.
- docs/professions-foundation-v0.0.1.md.
- docs/economy-crafting-loot-foundation-v0.0.1.md.
- docs/player-journey-milestone-roadmap-v0.0.1.md.

W szczegolnosci:

- `Stolica Wyspy` okresla dzielnice i funkcje huba.
- `level-1-dungeon-island` okresla pierwszy dungeon dostepny przez Portal
  Nexus.
- `quest-contract-objective` okresla typy zlecen, objective i achievementow.
- `loot-reward-table-001` okresla materialy i reward types dla `loch_001`.
- `professions` okresla Profession Board, salvage, upgrade i material usage.
- `economy` okresla repair, salvage, upgrade i reward claim jako sinki.
- `player journey` okresla level 1-10 i class choice na levelu 10.

## Out Of Scope

Nie robimy jeszcze:

- finalnego pluginu menu,
- GUI slot po slocie,
- dlugich NPC dialogow,
- NPC prowadzacego gracza przez kazdy krok,
- achievement UI,
- pelnego endgame UI,
- finalnych cen,
- finalnych drop rate,
- finalnych repair/upgrade kosztow.

## Test Cases

Discovery, NPC & Board Loop 001 v0.0.1 powinien przejsc ponizsze scenariusze:

- Gracz po spawnie rozumie, ze jest w miescie-hubie i widzi glowne dzielnice.
- Gracz moze sam znalezc Portal Nexus bez prowadzenia krok po kroku.
- Gracz widzi, ze `loch_001` jest pierwszym dostepnym lochem.
- Quest `Kly z Lochu 001` pokazuje source, target, ilosc i reward type.
- Gracz moze wejsc do lochu bez pobierania wszystkich zlecen.
- Po powrocie z lochu repair, salvage, stash i upgrade sa logicznie dostepne,
  ale nie wciskane na sile.
- Class choice przed level 10 jest locked i jasno mowi dlaczego.
- Na levelu 10 gracz moze znalezc mentorow i wybrac klase bez automatycznego
  prowadzenia za reke.
- Menu nie spamuje gracza komunikatami.
- Gracz ma poczucie eksploracji, nie checklisty.

## Assumptions

- `Stolica Wyspy` jest hubem do odkrywania, nie liniowym tutorialem.
- `loch_001` / `Level 1 Dungeon Island` jest pierwszym lochem.
- NPC i boardy pelnia funkcje, nie zastepuja ciekawosci gracza.
- Minimalne wyjasnienia sa potrzebne tylko dla krytycznych systemow.
- Dokument jest projektowy i nie zmienia konfiguracji serwera.
