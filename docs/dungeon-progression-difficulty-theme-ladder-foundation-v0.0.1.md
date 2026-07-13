# Dungeon Progression, Difficulty & Theme Ladder Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje poprawiony model progresji lochow, trudnosci i drabinki
klimatycznej dungeon islands. Lochy sa glowna osia PvE i levelowania postaci,
ale nie dzialaja stale jako `1 loch = 1 level postaci`.

Wczesny onboarding moze dawac szybki progress po pierwszych lochach, ale w
dluzszej perspektywie level postaci rosnie przez clear/progress dungeonow,
bossow, objective'ow, zlecen i trudniejszych wariantow. Kazdy loch ma miec
wlasna tozsamosc klimatu, mobow, mechanik i rewardow.

Dokument jest projektowy. Nie definiujemy finalnych XP numbers, drop rate,
lockoutow, plugin configow ani pelnej listy 100 lochow.

## Core Rules

Zasady:

- Lochy sa glowna osia progresji PvE.
- `Dungeon tier` oznacza poziom/content tier lochu, nie gwarantowany level
  postaci.
- Nie obowiazuje stala zasada `1 loch = 1 level postaci`.
- Pierwszy loch moze dac bardzo duzy progress, bo uczy petli gry.
- Pozniejsze levele wymagaja kilku runow, bossow, objective'ow, zlecen,
  trudniejszych wariantow albo target progression.
- Mob grind poza lochami nie jest glowna forma levelowania.
- Mob grind w lochu jest naturalna czescia runu, ale nie powinien przebijac
  clear/objective/boss progress.
- Powtorki lochu maja sens dla lootu, materialow, tokenow, boss dropow,
  practice, skinow, pet/mount cosmetics i difficulty challenge.

## Difficulty Model

Poprawiony model trudnosci ma cztery poziomy:

- `Latwy`.
- `Normalny / Klasyczny`.
- `Trudny`.
- `Bardzo Trudny`.

### Latwy

Rola:

- tryb dostepnosci dla casuali,
- dla graczy, ktorzy nie daja rady na klasycznym poziomie,
- dobre miejsce do nauki mechanik.

Zasady:

- dluzsze telegraphy,
- mniej addow,
- mniej pulapek,
- slabsze rewardy,
- nadal uczy mechanik,
- nie powinien byc najlepsza farma.

### Normalny / Klasyczny

Rola:

- domyslny tryb gry,
- klasyczny balans serwera,
- pelna podstawowa wersja lochu.

Zasady:

- standardowe mechaniki,
- standardowy loot/progress,
- domyslna rekomendacja dla gracza, ktory rozumie podstawy.

### Trudny

Rola:

- wyzsza presja,
- lepsze rewardy,
- challenge dla graczy, ktorzy opanowali normalny poziom.

Moze dodawac:

- nowe mob variants,
- dodatkowe patrole,
- pulapki,
- side objective,
- mocniejsze zachowania bossa,
- krotsze telegraphy.

### Bardzo Trudny

Rola:

- challenge tier,
- najlepsze rewardy danego lochu,
- mocny execution check.

Moze dodawac:

- dodatkowa faze bossa,
- elite modifiers,
- mocniejsze arena hazards,
- trudniejsze add timings,
- wyzszy token/material/rare chance.

Zasada:

- `Bardzo Trudny` moze byc wymagajacy, ale nie moze byc niesprawiedliwy.

## Difficulty Design Rules

Zasady:

- trudnosc nie moze byc tylko HP/damage scalingiem,
- wyzsze trudnosci moga dodawac zachowania, moby, pulapki, objective i fazy,
- wyzsze trudnosci nie moga zmieniac lochu w losowy chaos,
- kazda nowa mechanika musi miec czytelny counterplay,
- `Trudny` i `Bardzo Trudny` nie sa wymagane do casual progresji,
- `Trudny` i `Bardzo Trudny` moga przyspieszac loot/progress dobrym graczom,
- reward quality rosnie z trudnoscia, ale reward identity zostaje spojne z
  klimatem lochu.

## Dungeon Progression Rules

Progresja postaci:

- pierwsze levele moga byc szybkie i onboardingowe,
- pozniej poziom postaci wymaga wiecej niz jednego clearu,
- level 10 nadal odblokowuje wybor klasy,
- level 25 i 50 powinny wymagac szerszego zestawu aktywnosci,
- endgame 50+ opiera sie na target farmingu, bossach, difficulty, PvP, gildiach,
  profesjach i prestige.

Runy:

- first clear daje mocny progress,
- repeat clear daje glownie loot, materialy, tokeny, practice i difficulty
  rewards,
- boss/major objective jest glownym sygnalem completion,
- partial clear moze dac czesciowy progress, ale nie powinien zastapic full
  clearu,
- party i solo clear sa valid, ale leech/AFK credit musi byc ograniczony.

## Theme Ladder 001-100

Lochy 001-100 maja tworzyc drabinke klimatyczna, nie losowa liste map.

Zasady:

- kazdy loch ma wlasna tozsamosc,
- `loch_002` nie moze byc kopia `loch_001` z innymi statami,
- kazdy loch powinien miec wlasny klimat, enemy families, boss identity,
  materialy, hazards i reward hooks,
- w ramach jednego regionu mozna miec kilka lochow spokrewnionych klimatem,
  ale kazdy musi miec inny hook,
- przejscia klimatyczne powinny byc stopniowe i logiczne,
- nowy klimat powinien wprowadzac nowe moby, materialy, statusy, pulapki albo
  boss patterny.

Przykladowa logika przejsc:

- gaj / las,
- korzenie / bagna,
- stare ruiny,
- jaskinie / krysztaly,
- forteca / zbrojni przeciwnicy,
- mroz,
- ogien / popiol,
- pustkowia,
- mrok / chaos,
- endgame corrupted zones.

To jest kierunek klimatyczny, nie finalna lista 100 lochow.

## Portal Nexus Requirements

Portal Nexus powinien pokazywac:

- dungeon tier,
- klimat/region,
- difficulty,
- rekomendowany poziom,
- reward type,
- status odblokowania,
- czy `Normalny/Klasyczny` jest domyslnym trybem,
- czy `Latwy` ma slabsze rewardy,
- czy wyzsze trudnosci dodaja nowe elementy encountera.

Portal Nexus nie musi pokazywac pelnych drop rate.

## Example: Loch 001

`loch_001` pozostaje pierwszym wzorcem.

Klimat:

- gaj,
- korzenie,
- dzika natura,
- GroveGuardian / Straznik Gaju.

Difficulty:

- `Latwy`: wolniejsze telegraphy, mniej Rootlings, slabsze poison/root pressure.
- `Normalny/Klasyczny`: pelny standardowy GroveGuardian encounter.
- `Trudny`: dodatkowe Rootling timing, Grove Channeler add, mocniejsza presja
  interrupt window.
- `Bardzo Trudny`: dodatkowy arena hazard, mocniejsze phase combo, wyzszy
  guardian core/token chance.

Reward identity:

- natural/grove-themed materials,
- wolf fang,
- corrupted root,
- weak essence,
- grove token,
- guardian core.

Zasada:

- `loch_001` jest tutorialowym wzorcem, ale wyzsze trudnosci moga dac realny
  challenge.

## Integration

Ten dokument musi pozostac spojny z:

- docs/level-1-dungeon-island-foundation-v0.0.1.md.
- docs/dungeon-ladder-002-010-foundation-v0.0.1.md.
- docs/mob-boss-encounter-001-foundation-v0.0.1.md.
- docs/loot-reward-table-001-foundation-v0.0.1.md.
- docs/discovery-npc-board-loop-001-foundation-v0.0.1.md.
- docs/player-journey-milestone-roadmap-v0.0.1.md.
- docs/world-content-loop-foundation-v0.0.1.md.
- docs/economy-crafting-loot-foundation-v0.0.1.md.
- docs/professions-foundation-v0.0.1.md.

W szczegolnosci:

- `level-1-dungeon-island` opisuje pierwszy wzorzec, ale nie cala progresje.
- `dungeon-ladder-002-010` opisuje pierwszy akt po `loch_001`, bez zamykania
  pelnej listy 100 lochow.
- `mob-boss-encounter-001` opisuje behavior scaling, ktory ten dokument
  rozszerza do czterech poziomow trudnosci.
- `loot-reward-table-001` musi rozroznic nagrody wedlug czterech trudnosci.
- `discovery-npc-board-loop` musi pokazywac difficulty bez prowadzenia gracza za
  reke.
- `player-journey` musi traktowac dungeony jako glowna os progresji, ale nie
  jako sztywne `1 loch = 1 level`.
- `world-content-loop` musi utrzymac dungeon ladder jako glowna os PvE i target
  farmingu.
- `economy/professions` musza dawac inne materialy i recipe hooks dla kolejnych
  klimatow.

## Out Of Scope

Nie robimy jeszcze:

- finalnej listy 100 lochow,
- finalnych XP numbers,
- finalnych drop rate,
- plugin configow,
- finalnych bossow 002-100,
- finalnej mapy klimatycznej blok po bloku,
- lockoutow,
- matchmakingu,
- pelnej tabeli materialow dla wszystkich klimatow.

## Test Cases

Dungeon Progression, Difficulty & Theme Ladder v0.0.1 powinien przejsc
ponizsze scenariusze:

- Dokument nie twierdzi, ze `1 loch = 1 level` dziala przez cala gre.
- `Normalny/Klasyczny` jest domyslnym poziomem trudnosci.
- `Latwy` jest opcja dla casuali, ale nie najlepsza farma.
- `Trudny` i `Bardzo Trudny` dodaja zachowania, moby, pulapki albo fazy, nie
  tylko staty.
- `loch_002` nie moze byc kopia `loch_001`.
- Kazdy dungeon tier ma wlasna tozsamosc klimatu, mobow i rewardow.
- Powtorki lochu maja sens dla lootu/materialow/tokenow, ale trash mob grind nie
  jest glownym levelingiem.
- Wyzsze trudnosci przyspieszaja dobrym graczom rewardy/progress, ale nie sa
  wymagane dla casual class progression.
- Portal Nexus jasno pokazuje difficulty, klimat, reward type i status
  odblokowania.

## Assumptions

- Lochy sa glowna osia progresji PvE.
- `loch_001` pozostaje pierwszym wzorcem.
- `Latwy`, `Normalny/Klasyczny`, `Trudny`, `Bardzo Trudny` sa aktualnym modelem
  trudnosci lochow.
- Dungeon ladder moze docelowo miec okolice 100 lochow, ale beda projektowane
  etapami.
- Dokument jest design-only i nie zmienia konfiguracji serwera.
