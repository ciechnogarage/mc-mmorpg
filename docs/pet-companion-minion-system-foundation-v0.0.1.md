# Pet, Companion & Minion System Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje trzy osobne systemy towarzyszy:

- global pet inspirowany Dungeon Defenders 1,
- beast companion Wladcy Bestii inspirowany MMO pet classes,
- summony/miniony Rytualisty / Warlocka inspirowane WoW/PoE.

Celem jest dac kazdemu graczowi fajnego peta, ale nie zabrac identity Wladcy
Bestii ani Summonera. System musi tez chronic PvP przed summon spamem, bodyblock
abuse, pet pay-to-win, permanent tank petami i pasywnym damage bez counterplayu.

Dokument jest projektowy. Nie definiujemy finalnego AI, plugin configow, statow,
cooldownow, pelnego UI kolekcji petow ani finalnych questow oswajania.

## Core Model

### Global Pet

Global pet:

- jest dostepny potencjalnie dla kazdego gracza,
- dziala bardziej jak aktywny item/equipment slot niz klasa,
- ma rarity, level, upgrade, trait i skin,
- ma lekki efekt albo zachowanie,
- nie tankuje bossow,
- nie jest glownym zrodlem damage,
- nie zabiera identity Wladcy Bestii ani Summonera.

Inspiracja:

- Dungeon Defenders 1: pet jako zywy item z auto-effectem.

### Beast Companion

Beast companion:

- jest identity Wladcy Bestii,
- jest jedna glowna aktywna bestia bojowa,
- ma komendy, role, family traits i bond progression,
- moze byc oswajana albo rozwijana,
- realnie uczestniczy w walce,
- jest killable, CC-able albo disableable.

Inspiracja:

- MMO pet classes typu WoW / 4Story.

### Summoner / Warlock Minions

Miniony:

- sa identity Rytualisty / Warlocka / Summonera,
- sa przywolywane przez skille, rytualy, klatwy, corpse setup, zasob albo
  cooldown,
- moga byc liczniejsze niz beast companion,
- sa slabsze jednostkowo, czasowe albo warunkowe,
- maja limity, koszt i counterplay.

Inspiracja:

- WoW / PoE skill-based summons.

### Skins / Appearances

Skins / appearances:

- sa warstwa wygladu nakladana na global pety, beast companiony albo miniony,
- nie sa osobnym typem towarzysza,
- moga zmieniac wyglad, kolor, animacje, particles albo model w dozwolonym
  zakresie,
- nie zmieniaja statow, AI, hitboxa gameplayowego, roli ani PvP funkcji.

### Mount

Mount:

- jest osobnym przyszlym systemem,
- nie jest combat petem,
- sluzy do movement, travel, prestige, exploration i ewentualnie travel utility,
- nie walczy, nie tankuje i nie bodyblockuje graczy w PvP,
- nie moze omijac dash cooldownow, dungeon hazards, boss mechanics ani PvP
  combat timerow, chyba ze konkretny content jawnie na to pozwala,
- moze miec skiny, rarity, unlocki, animacje i prestige progression.

## Global Pet Rules

Zasady:

- kazdy gracz moze miec maksymalnie jednego aktywnego global peta,
- pet ma byc fajnym dodatkiem, nie osobnym buildem,
- pet power jest niski i capowany,
- combat effects peta sa slabsze niz skille klasowe,
- utility pet effects sa przydatne, ale nie wymagane,
- pet nie bodyblockuje graczy w PvP,
- pet nie tankuje bossow,
- pet nie triggeruje infinite on-hit efektow,
- pet cosmetics nie daja combat advantage,
- pet nie moze byc pay-to-win combat source.

PvP:

- osobne modyfikatory damage,
- brak mocnego CC,
- brak chain-stagger,
- brak mocnych on-hit procow,
- mozliwe wylaczenie albo oslabienie wybranych efektow.

## Global Pet Archetypes

Archetyp peta ma byc konkretnym zachowaniem z limitem, a nie nudnym stat stickiem
typu `+5% wszystkiego`.

### Spark Pet

Efekt:

- co kilka sekund strzela slabym pociskiem w aktualny cel gracza.

Rola:

- lekki damage/QoL,
- poczucie aktywnego peta.

Limity:

- niski damage,
- PvP damage modifier,
- nie triggeruje mocnych on-hit efektow.

### Ward Pet

Efekt:

- po otrzymaniu obrazen daje maly shield/ward proc.

Rola:

- lekka defensywa,
- pomoc w early PvE.

Limity:

- cooldown,
- brak permanent sustainu,
- mocne PvP ograniczenia.

### Pulse Pet

Efekt:

- okresowo wysyla mala aure utility.

Mozliwe warianty:

- slaby reveal,
- pokazanie materialow,
- lekki debuff na moby.

Limity:

- PvP reveal mocno ograniczony,
- debuff nie moze zastapic klasowego controlu.

### Leech Pet

Efekt:

- daje minimalny resource return po killu albo po trafieniu moba.

Rola:

- lekka wygoda w PvE,
- sustain resource flow.

Limity:

- nie dziala jako mocny lifesteal,
- PvP bardzo ograniczone albo disabled.

### Bomb Pet

Efekt:

- co jakis czas zostawia maly ladunek albo wybuch utility.

Rola:

- PvE clear,
- lekki area utility.

Limity:

- brak chain-CC,
- PvP damage/CC ograniczone,
- cooldown.

### Totem Pet

Efekt:

- okresowo stawia krotki mikro-totem albo mala buff zone.

Rola:

- pozycjonowanie,
- lekki support.

Limity:

- efekt maly,
- nie stackuje sie masowo,
- wymaga pozycji.

### Finder Pet

Efekt:

- pomaga wykrywac materialy, skrzynki, slabe sekrety albo loot sparkle.

Rola:

- eksploracja,
- QoL,
- gathering support.

Limity:

- prawie zero combat power,
- nie pokazuje wszystkiego bez kosztu.

### Mender Pet

Efekt:

- po walce albo na cooldownie daje maly heal/regen tick.

Rola:

- recovery,
- wygoda solo PvE.

Limity:

- nie zastapi healera,
- nie dziala jako combat sustain spam,
- PvP mocno ograniczone.

### Trickster Pet

Efekt:

- maly utility proc.

Mozliwe warianty:

- slow na moba,
- mini blind PvE,
- confuse trash mob.

Limity:

- w PvP tylko bardzo ograniczone efekty albo brak CC,
- nie chainuje kontroli.

### Craft Pet

Efekt:

- drobny profession/QoL bonus.

Mozliwe warianty:

- salvage hint,
- material preview,
- crafting convenience,
- minor gathering helper.

Limity:

- nie omija limitow profesji,
- nie daje combat pay-to-win,
- nie generuje darmowych materialow bez aktywnosci.

## Pet Archetype Rules

Zasady:

- kazdy archetyp ma jasny efekt i jasny limit,
- pet nie powinien byc plaskim stat stickiem,
- najlepsze pety zmieniaja wygode albo styl, nie robia jednej mety,
- combat pet effects sa slabsze niz skille klasowe,
- utility pet effects sa uzyteczne, ale nie wymagane,
- PvP moze miec osobne wartosci, cooldowny albo wylaczone efekty,
- rarity moze poprawiac czestotliwosc, kosmetyke albo lekko wartosc efektu,
  ale nie moze tworzyc absurdalnego power gapu.

## Skins / Appearances Rules

Skins / appearances sa kosmetyczna warstwa na istniejacego towarzysza.

Moga dotyczyc:

- global petow,
- bestii Wladcy Bestii,
- minionow Summonera / Warlocka.

Zasady:

- skin moze zmienic wyglad, kolor, animacje, particles albo model w dozwolonym
  zakresie,
- skin nie zmienia statow,
- skin nie zmienia AI,
- skin nie zmienia gameplayowego hitboxa,
- skin nie zmienia roli peta, bestii ani miniona,
- skin nie moze ukrywac telegraphow,
- skin nie moze utrudniac targetowania,
- skin nie moze ukrywac, czy obiekt jest global petem, bestia Wladcy Bestii czy
  minionem,
- skin nie moze dawac combat advantage ani pay-to-win.

Zrodla skinow moga byc:

- bossy,
- reputacje,
- eventy,
- achievementy,
- crafting,
- dropy,
- sklep kosmetyczny, jesli kiedykolwiek powstanie.

## Beast Companion System

Beast Companion jest identity Wladcy Bestii.

Zasady:

- Wladca Bestii ma jedna glowna aktywna bestie bojowa,
- bestia jest oswajana z wybranych rodzin mobow,
- bestia rozwija sie przez bond/mastery,
- bestia ma komendy,
- bestia realnie uczestniczy w walce,
- bestia jest killable, CC-able albo disableable,
- owner nadal jest realnym celem,
- companion damage, tankiness, healing i CC maja PvP modyfikatory,
- bestia nie jest armia minionow.

Komendy:

- attack,
- return,
- hold,
- protect,
- special command.

Role:

- harass,
- light tank,
- peel,
- bleed,
- poison,
- scout,
- interrupt,
- tracking.

## Beast Families

`Wolf`:

- chase,
- harass,
- bleed,
- target pressure.

`Bear / Boar`:

- light tank,
- peel,
- stagger,
- frontline pressure.

`Bird`:

- scout,
- mark,
- mobility,
- anti-stealth utility.

`Spider / Serpent`:

- poison,
- slow,
- control,
- attrition.

`Cat / Panther`:

- flank,
- crit window,
- mobility pressure.

`Spirit Beast`:

- utility,
- ward,
- detection,
- rare unlock.

Taming restrictions:

- bossy nie sa tameable,
- elite nie sa tameable bez specjalnego late-game unlocku,
- tamed beast zachowuje role/family identity, nie dokladne boss mechanics.

## Summoner / Warlock Minions

Miniony sa identity Rytualisty / Warlocka / Summonera.

Zasady:

- miniony sa przywolywane przez skille, rytualy, klatwy, corpse setup, zasob
  albo cooldown,
- miniony moga byc liczniejsze niz beast companion,
- miniony sa slabsze jednostkowo albo czasowe,
- miniony maja count cap, duration, cost, cooldown albo summon condition,
- miniony sa killable, dispellable, interruptable albo zalezne od owner
  pressure,
- owner musi byc realnym celem.

Przykladowe typy:

- melee thrall,
- ranged spirit,
- curse familiar,
- temporary demon,
- sacrifice minion.

Warlock loop:

- curse,
- drain,
- summon,
- sacrifice,
- owner pressure,
- cleanse/interrupt/LoS jako counterplay.

## PvP Rules

Global pet:

- niski damage,
- brak bodyblock,
- brak tankowania,
- brak mocnego CC.

Beast Companion:

- realna presja,
- killable/CC-able,
- damage modifier vs players,
- owner pressure dziala.

Minions:

- count cap,
- duration/cooldown,
- PvP damage modifier,
- killable/dispellable.

Wspolne zasady:

- pety/miniony nie triggeruja infinite on-hit efektow,
- healing petow/minionow w PvP ma twarde capy,
- stealth + pet/minion pressure wymaga reveal/detection counterplay,
- collision/bodyblock graczy musi byc wylaczone albo mocno ograniczone,
- boss/dungeon PvE nie moze byc trivializowany przez pet tanking.

## PvE Rules

Zasady:

- global pet daje flavor/QoL, nie zastepuje class kit,
- Beast Companion pomaga solo Wladcy Bestii, ale nie gra za gracza,
- miniony pomagaja Warlockowi/Summonerowi zarzadzac presja, addami i setup
  windows,
- bossy moga pressure'owac ownera i summony osobno,
- czesc boss attacks moze cleave/splash pets/minions,
- summons powinny respektowac dungeon hazards,
- companion/minion AI nie moze pullowac calego dungeonu przypadkiem.

## Progression

Global pet:

- rarity,
- pet level,
- upgrade,
- skin,
- trait,
- QoL unlock.

Beast Companion:

- tame family,
- bond,
- role upgrade,
- command upgrade,
- subclass scaling.

Minions:

- new minion types,
- count cap,
- duration,
- curse synergy,
- ritual upgrades.

Gear support:

- global pet gear tylko cosmetic/QoL albo bardzo niski cap,
- Beast Companion skaluje sie z Wladca Bestii stats/mastery,
- miniony skaluja sie z summon/minion damage, Chaos/Faith/Intelligence i skill
  tags.

Profession support:

- Lowiectwo daje beast materials, taming bait, companion care,
- Zaklinanie daje minion/rune modifiers,
- Alchemia daje temporary pet/minion utility consumables z PvP capami.

## Integration

Ten dokument musi pozostac spojny z:

- docs/class-subclass-foundation-v0.0.1.md.
- docs/class-skill-kits-foundation-v0.0.1.md.
- docs/mob-boss-encounter-001-foundation-v0.0.1.md.
- docs/combat-foundation-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.
- docs/professions-foundation-v0.0.1.md.
- docs/skill-ability-system-v0.0.1.md.

W szczegolnosci:

- Wladca Bestii zachowuje unikalna bestie,
- Lowca companion commands i Warlock minion skills nie moga sie dublowac,
- pet/minion granice i PvP sanity pozostaja zgodne z encounterami,
- pet/minion gameplay musi miec LoS, target priority, interrupt, owner pressure
  i counterplay,
- pet/minion staty wymagaja osobnych affixow i capow,
- Lowiectwo, Zaklinanie i Alchemia wspieraja system bez zabierania identity
  klasom.

## Out Of Scope

Nie robimy jeszcze:

- finalnego AI,
- plugin configow,
- finalnych statow, HP, damage, cooldownow,
- pelnego pet collection UI,
- finalnych taming questow,
- finalnego mount systemu,
- monetizacji/cosmetic shop rules poza zasada no combat pay-to-win,
- finalnej listy wszystkich bestii i minionow,
- finalnej listy skinow / appearances.

## Test Cases

Pet, Companion & Minion v0.0.1 powinien przejsc ponizsze scenariusze:

- Global pet jest fajny, ale nie obowiazkowy do combat buildu.
- Pet archetypes maja konkretne efekty, nie sa nudnymi stat stickami.
- Wladca Bestii czuje sie unikalny przez jedna realna bestie.
- Warlock/Summoner czuje sie unikalny przez minion/ritual loop.
- Global pet, beast companion i miniony sa mechanicznie rozne.
- PvP gracz moze kontrowac companion/minion przez kill, CC, dispel, LoS albo
  pressure ownera.
- Pety/miniony nie bodyblockuja graczy.
- Bossy nie sa trivializowane przez pet tanking.
- Gear/profesje wspieraja pet/minion buildy, ale nie robia ich mandatory dla
  kazdej klasy.
- Taming nie pozwala kazdej klasie miec combat companiona jak Wladca Bestii.
- Nie istnieje osobna kategoria `Cosmetic Pet`; kosmetyka jest warstwa skinow.
- Skin nie daje statow, AI advantage, mniejszego hitboxa ani PvP przewagi.
- PvP przeciwnik dalej rozpoznaje, czy widzi global peta, bestie czy miniona.
- Mount jest osobnym przyszlym systemem, nie combat petem.
- Mount nie walczy, nie tankuje i nie bodyblockuje graczy w PvP.

## Assumptions

- Global pet jest dla kazdego, ale low-power.
- Wladca Bestii zostaje pod Lowca.
- Summoner/Rytualista/Warlock zostaje pod Akolita.
- Dungeon Defenders 1 inspiruje global pet jako equipment-like companion.
- WoW/4Story inspiruja Beast Companion jako MMO pet class.
- WoW/PoE inspiruja Summoner/Warlock minions jako skill-based summons.
- Kosmetyka jest warstwa wygladu dla petow, bestii i minionow, nie osobnym
  typem towarzysza.
- Mount jest osobnym przyszlym systemem movement/travel/prestige.
- Dokument jest design-only i nie zmienia konfiguracji serwera.
