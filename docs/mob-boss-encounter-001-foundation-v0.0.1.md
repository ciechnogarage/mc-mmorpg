# Mob & Boss Encounter 001 Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje pierwszy projektowy roster mobow, mini-elite i bossa dla
`loch_001` / `Level 1 Dungeon Island`. Celem jest spiac combat lessons, questy,
loot table, profesje, difficulty behavior i boss encounter w konkretne
przeciwniki z czytelnym counterplayem.

Ten dokument nie jest finalna konfiguracja MythicMobs ani finalnym balansem HP,
damage, armor, cooldownow lub drop rate.

## Difficulty Philosophy

Trudnosc ma zmieniac zachowanie encounterow, nie tylko HP i damage.

Docelowy kierunek:

- wymagajaco,
- fair,
- czytelnie,
- bez losowych one-shotow,
- bez chain-CC bez counterplayu,
- bardziej Elden Ring / Terraria pressure niz idle farm.

Poziomy:

- Source of truth: `docs/dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md`.
- `Latwy`: uczy mechanik, solo-friendly, dluzsze telegraphy, mniej add pressure,
  wybacza bledy.
- `Normalny / Klasyczny`: domyslny wariant encountera, pelna podstawowa tozsamosc i standardowe tempo.
- `Trudny`: ten sam encounter, ale dodatkowe zachowania, krotsze telegraphy,
  wieksza presja na pozycjonowanie.
- `Bardzo Trudny`: realne execution check, znajomosc patternow, mocniejsze combo i arena
  pressure, ale nadal czytelnie.

Zasady:

- trudnosc nie moze opierac sie tylko na wiekszym HP/damage,
- wyzsze trudnosci moga dodawac combo, add timing, fake-out, arena pressure albo kare za
  panic dodge/block spam,
- fantasy moba zostaje takie samo na kazdym poziomie,
- counterplay musi pozostac widoczny.

## Mob Roster

### Feral Wolf

Rola:

- szybki melee mob,
- uczy spacingu, dodge i podstawowego targetowania.

Drop identity:

- wolf fang,
- rough hide,
- small bone.

Quest link:

- `Kly z Lochu 001`.

Counterplay:

- spacing,
- dodge,
- block,
- szybkie burst window po leapie.

Difficulty behavior:

- Latwy: prosty leap z dlugim windupem.
- Normalny / Klasyczny: leap po okrazeniu celu, standardowa presja na spacing.
- Trudny: feintuje podejscie i karze cofanie sie w linii prostej, ale leap lock-in
  jest czytelny.

### Rootling

Rola:

- maly plant/root mob,
- uczy movementu i unikania root zone.

Drop identity:

- corrupted root,
- wild herb,
- natural fragment.

Quest link:

- `Korzenie Straznika`.

Counterplay:

- movement,
- szybkie add clear,
- unikanie root zone.

Difficulty behavior:

- Latwy: pojedynczy root zone.
- Normalny / Klasyczny: root zone laczy sie z lekkim slow.
- Trudny: root zone moze synchronizowac sie z innym mobem, ale z czytelnym
  telegraphem.

### Briar Archer

Rola:

- ranged harasser,
- uczy line of sight, dodge albo block.

Drop identity:

- wood fragment,
- rough hide,
- ranged base chance.

Quest link:

- optional kill/order,
- moze wspierac profession order dla Inzynierii albo Lowiectwa.

Counterplay:

- LoS,
- dodge,
- block,
- szybkie podejscie.

Difficulty behavior:

- Latwy: wolne pojedyncze strzaly.
- Normalny / Klasyczny: cofa sie po podejsciu gracza.
- Trudny: zmienia pozycje i wymusza LoS, ale nie ma aimbot burstu.

### Grove Channeler

Rola:

- caster/channeler,
- uczy interruptu i target priority.

Drop identity:

- weak essence,
- cracked crystal,
- weak rune fragment.

Quest link:

- `Przerwany Rytual`.

Counterplay:

- interrupt,
- LoS,
- szybki burst,
- presja na channel.

Difficulty behavior:

- Latwy: dlugi channel z wyraznym efektem.
- Normalny / Klasyczny: shield/heal channel ma standardowe okno przerwania.
- Trudny: moze baitowac interrupt krotkim fake channel, ale prawdziwy channel ma
  jasny efekt i nadal realne interrupt window.

### Shielded Husk

Rola:

- defensywny mob,
- uczy guard break, heavy attack, parry/counter albo ataku od tylu.

Drop identity:

- cracked plate,
- small bone,
- common armor/shield base chance.

Quest link:

- optional bounty,
- material source dla Kowalstwa.

Counterplay:

- guard pressure,
- heavy attack,
- parry/counter,
- reposition za plecy.

Difficulty behavior:

- Latwy: prosta garda, wolny counter.
- Normalny / Klasyczny: obraca sie do gracza i wymaga lepszego reposition.
- Trudny: laczy garde z counter-slamem po spamie light attack.

### Toxic Creeper

Rola:

- poison/root utility mob,
- uczy status pressure w lekkiej formie.

Drop identity:

- toxin sac,
- corrupted root,
- antidote material.

Quest link:

- optional alchemy order,
- material source dla Alchemii.

Counterplay:

- movement,
- poison resist,
- cleanse, jesli gracz ma narzedzie,
- szybkie zabicie.

Difficulty behavior:

- Latwy: slaby poison, krotki czas.
- Normalny / Klasyczny: zostawia poison puddle.
- Trudny: laczy poison z root pulse, ale duration i cleanse/resist counterplay
  zostaja.

Rule:

- mob nie moze wymagac cleanse na poziomie, gdzie gracz nie ma jeszcze dostepu
  do sensownego cleanse.

## Encounter Placement

`Outer Path`:

- Feral Wolf,
- Rootling,
- niski pressure,
- nauka ruchu i ataku.

`Ranged Bend`:

- Briar Archer,
- LoS/dodge/block lesson.

`Ritual Clearing`:

- Grove Channeler,
- weak mobs,
- interrupt lesson.

`Broken Gate`:

- Shielded Husk,
- guard pressure,
- parry/counter albo reposition.

`Poison Side Area`:

- Toxic Creeper,
- opcjonalny status/material lesson.

`Gatekeeper Arena`:

- Elite Gatekeeper,
- mini-check przed bossem.

`Ancient Grove Arena`:

- GroveGuardian boss fight.

## Elite Gatekeeper

Rola:

- mini-elite przed bossem,
- testuje podstawy walki bez bycia sciana.

Mechaniki:

- `Heavy Slam`: wolny mocny atak do dash/block/parry.
- `Guarded Stance`: chwilowa redukcja obrazen, wymaga reposition albo guard
  pressure.
- `Call Roots`: przywoluje 1-2 Rootlings.
- `Channel Fortify`: interruptable channel dajacy tarcze, jesli nie przerwany.

Difficulty behavior:

- Latwy: dlugie windupy, malo addow, prosty channel.
- Normalny / Klasyczny: addy pojawiaja sie szybciej, Guarded Stance trwa dluzej, channel daje
  mocniejsza tarcze.
- Trudny: stance swap, counter-slam po spamie atakow, add call w trudniejszym
  timingu.

Counterplay:

- dash/block/parry na Heavy Slam,
- interrupt na Channel Fortify,
- add clear,
- reposition za plecy,
- nie spamowac light attack w garde.

Reward identity:

- better material bundle,
- magic item chance,
- token fragment chance.

Solo rule:

- solo `Latwy` musi byc realne,
- `Normalny / Klasyczny`, `Trudny` i `Bardzo Trudny` wymagaja lepszej gry, ale nie konkretnej klasy.

## GroveGuardian Encounter

`GroveGuardian` / `Straznik Gaju` jest pierwszym bossem. Ma byc czytelny,
wolniejszy od pozniejszych bossow i edukacyjny, ale nie banalny.

Boss nie jest:

- DPS race,
- testem posiadania tanka,
- testem posiadania healera,
- testem konkretnej klasy,
- testem losowego one-shotu.

### Phase 1: Awakening

Mechaniki:

- `Root Slam`: frontal slam, counterplay dash/block.
- `Vine Line`: liniowy root telegraph, counterplay movement.
- `Summon Rootlings`: 1-2 addy, counterplay target priority.

Cel fazy:

- nauczyc pozycji,
- nauczyc uniku,
- nauczyc add clear.

Difficulty behavior:

- Latwy: pojedyncze patterny, dlugie windupy.
- Normalny / Klasyczny: slam i root line moga wystapic blizej siebie.
- Trudny: boss moze zmienic kierunek slam przed lock-in, ale lock-in musi byc
  czytelny.

### Phase 2: Grove Ritual

Mechaniki:

- `Ground Bloom`: AoE pola na ziemi, counterplay reposition.
- `Barkskin Channel`: interruptable shield/heal channel.
- `Briar Volley`: wolniejszy projectile pattern, counterplay LoS/dodge/block.

Cel fazy:

- nauczyc interruptu,
- nauczyc LoS,
- nauczyc zarzadzania arena.

Difficulty behavior:

- Latwy: dlugi channel i wolne AoE.
- Normalny / Klasyczny: AoE wymusza standardowe pozycjonowanie.
- - Trudny: channel moze byc chroniony przez addy, ale interrupt nadal jest realny.

### Low HP: Enrage-Lite

Mechaniki:

- `Faster Slam`: szybszy, ale nadal czytelny slam.
- `Root Pulse`: krotka fala root/slow z jasnym telegraphem.
- `Add Pressure`: wiecej Rootlings, ale bez zalania areny.

Cel fazy:

- presja koncowa,
- test zachowania zasobow,
- utrzymanie spokoju pod presja.

Difficulty behavior:

- Latwy: tylko lekko szybsze tempo.
- Normalny / Klasyczny: add pressure plus root pulse.
- Trudny: boss laczy root pulse z arena pressure, ale nie robi chain-root bez
  counterplayu.

## Anti-Bullshit Rules

Zasady:

- brak losowego one-shotu,
- brak chain-root bez counterplayu,
- brak wymogu cleanse, jesli gracz nie ma jeszcze narzedzia cleanse,
- brak wymogu konkretnej klasy/skilla,
- ataki musza miec czytelny windup,
- addy nie moga rosnac nieskonczenie,
- interruptable channel musi byc widoczny,
- arena musi miec miejsce na ruch,
- solo `Latwy` musi byc realne,
- `Trudny` i `Bardzo Trudny` moga byc wymagajace, ale nie niesprawiedliwe.

## Pet / Companion Note

Ten dokument nie projektuje pelnego systemu petow, ale ustala granice, zeby
przyszle systemy sie nie dublowaly.

Warstwy:

- `Global Pet`: potencjalnie dostepny dla kazdego gracza, lekki utility/passive
  albo cosmetic, nie pelny combat build.
- `Wladca Bestii`: podklasa Lowcy, ktora moze oswajac albo kontrolowac
  dodatkowa bestie jako realny combat companion.
- `Summoner / Rytualista / Warlock`: miniony przywolywane skillami, czesto
  tymczasowe, zalezne od zasobow, rytualu, cooldownu, corpse/curse setupu albo
  owner pressure.

Zasady:

- global pet nie moze zabrac identity Wladcy Bestii,
- Wladca Bestii nie powinien miec armii minionow jak Summoner,
- Summoner nie powinien miec stalego tanka-peta bez kosztu i counterplayu,
- pety/miniony musza byc killable albo miec inne jasne counterplay,
- PvP wymaga osobnych modifierow na pet/minion damage,
- owner nadal musi byc realnym celem,
- unikac bodyblock abuse.

## Integration

Ten dokument musi pozostac spojny z:

- docs/level-1-dungeon-island-foundation-v0.0.1.md.
- docs/dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md.
- docs/loot-reward-table-001-foundation-v0.0.1.md.
- docs/quest-contract-objective-foundation-v0.0.1.md.
- docs/combat-foundation-v0.0.1.md.
- docs/starter-skills-class-progression-foundation-v0.0.1.md.
- docs/class-subclass-foundation-v0.0.1.md.
- docs/class-skill-kits-foundation-v0.0.1.md.
- docs/pet-companion-minion-system-foundation-v0.0.1.md.

W szczegolnosci:

- `level-1-dungeon-island` okresla segmenty mapy i miejsce bossa.
- `loot-reward-table-001` okresla drop identity kazdego moba.
- `quest-contract-objective` okresla questy i objective uzywajace tych mobow.
- `combat` okresla dash, block, parry, counter, interrupt, LoS, add priority i
  resource management.
- `starter skills` okresla style testowane przed wyborem klasy.
- `class-subclass` i `class-skill-kits` okreslaja roznice miedzy Wladca Bestii
  companion a Summoner/Warlock minions.
- `pet-companion-minion-system` okresla global pet, Wladca Bestii companion i
  Summoner/Warlock minions jako trzy osobne warstwy.

## Out Of Scope

Nie robimy jeszcze:

- finalnych MythicMobs YAML,
- finalnych HP/damage/armor/cooldownow,
- exact drop rate,
- modelu 3D bossa,
- finalnej mapy areny blok po bloku,
- pelnego systemu petow,
- pelnego systemu oswajania,
- pelnego systemu summonow,
- wymogu party albo konkretnej klasy.

## Test Cases

Mob & Boss Encounter 001 v0.0.1 powinien przejsc ponizsze scenariusze:

- Kazdy mob ma jasna lekcje i counterplay.
- Difficulty zmienia zachowanie, nie tylko HP/damage.
- Latwy jest solo-friendly.
- Normalny / Klasyczny jest domyslnym baseline.
- Trudny i Bardzo Trudny wymagaja lepszej gry, ale zostaja fair.
- Feral Wolf wspiera quest/drop `wolf fang`.
- Grove Channeler wspiera interrupt objective i weak essence drop.
- Shielded Husk uczy obejscia defensywy.
- Toxic Creeper pokazuje poison/status bez karania gracza za brak cleanse.
- Elite Gatekeeper testuje heavy attack, add clear i interrupt bez bycia sciana.
- GroveGuardian jest do przejscia solo na `Latwy`.
- Boss ma czytelne fazy i brak bullshit one-shotow.
- Drop identity zgadza sie z loot-reward-table-001.
- Global pet, Wladca Bestii companion i Summoner minions sa rozdzielone
  systemowo.

## Assumptions

- `loch_001` / `Level 1 Dungeon Island` jest pierwszym dungeonem.
- Gracz jest przed wyborem klasy, wiec encountery nie moga wymagac class kit.
- `GroveGuardian` / `Straznik Gaju` jest roboczym pierwszym bossem.
- Trudnosc ma isc w strone fair challenge, nie casual idle farmy ani
  niesprawiedliwego one-shot designu.
- Pelny system petow/companionow/minionow bedzie osobnym dokumentem.
- Dokument jest projektowy i nie zmienia konfiguracji serwera.
