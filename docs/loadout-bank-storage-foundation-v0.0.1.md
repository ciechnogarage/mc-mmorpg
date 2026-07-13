# Loadout Bank And Storage Foundation v0.0.1

## Cel dokumentu

Ten dokument zbiera w jednym miejscu fundament loadoutow, banku i storage dla
serwera MCMMORPG. Do tej pory te systemy byly opisane rozproszone: loadouty w
`progression-respec`, bank i stash w `stolica-wyspy-hub`, stash menu w
`discovery-npc-board`, material storage w `professions`, a guild bank w
`guilds`. Nigdzie nie bylo wspolnego opisu, jak gracz przechowuje rzeczy i
zarzadza przygotowanymi setupami. Ten dokument to konsoliduje.

Loadout, bank i storage to warstwa wygody i przygotowania, nie warstwa mocy
bojowej. Maja pozwalac graczowi szybko sie przygotowac, schowac dorobek i wrocic
do gry, bez dawania przewagi statystyk.

To nie jest finalny system. To wersja v0.0.1. Nie definiujemy tu liczby slotow
banku, czasow combat lockout, limitow stackow ani plugin configow. Definiujemy
zasady i counterplay dla abuse. Guild bank pozostaje osobnym systemem opisanym w
`guilds-foundation`.

## Filozofia

Zasady prowadzace caly system:

- Storage i loadouty to warstwa wygody i przygotowania, nie zrodlo mocy bojowej.
- Przechowywanie rzeczy nie daje przewagi statystyk; daje porzadek i komfort.
- Loadout to narzedzie wygody, nie instant counter-pick w trakcie walki.
- Zmiana setupu i dostep do banku odbywaja sie w safe zone, nie w srodku walki.
- Te systemy wspieraja petle gry (wyprawa, powrot do huba, przygotowanie), nie
  zastepuja progresji.

## Loadouty

Konsolidacja `progression-respec` (## Loadouts). Loadout pozwala zapisac kilka
przygotowanych setupow, zeby gracz nie musial recznie przekladac wszystkiego
przed kazda wyprawa.

Loadout moze zawierac:

- skill bar,
- weapon set,
- armor set,
- akcesoria,
- quick items,
- ewentualnie preset pasywek, jesli system pasywek na to pozwoli.

Zasady:

- Zmiana loadoutu tylko poza walka albo w safe zone.
- W PvP obowiazuje combat lockout: brak zmiany przez krotki czas po otrzymaniu
  lub zadaniu obrazen.
- Loadout nie omija wymagan statow, mastery, itemow ani cooldownow.
- Loadout nie resetuje cooldownow.
- Loadout jest narzedziem wygody, nie instant counter-pickiem.

## Bank i stash

Konsolidacja `stolica-wyspy-hub` (### Bank / Stash). Bank i stash to glowne
miejsce przechowywania w hubie.

Rola:

- przechowywanie itemow,
- przechowywanie materialow,
- overview waluty i tokenow,
- przygotowanie do dungeonow.

Placement:

- bank jest blisko marketu i portali, bo gracz wraca tu po dungeonach,
- bank, market i loadout preparation area tworza jeden wygodny wezel powrotu.

Bank dziala w safe zone huba (`stolica-wyspy-hub`), wiec dostep do dorobku nie
jest zagrozony griefem.

## Material storage

- Material storage to osobny tor przechowywania zasilajacy profesje i projekty
  (spojnie z `professions` i `economy-crafting-loot`).
- Materialy trzymane sa oddzielnie od zwyklego stash, zeby crafting i sinki
  ekonomiczne mialy czytelne zaplecze.
- Torby albo utility storage moga dojsc, jesli system bedzie tego wymagac
  (`professions`); na tym etapie odlozone jakosciowo, bez finalnych pojemnosci.

## Stash menu i UX

Konsolidacja `discovery-npc-board` (### Stash Menu). Menu stash ma byc proste i
czytelne.

Minimalne pola:

- item storage,
- material storage,
- token/currency view.

UX i petla powrotu:

- system informuje o stanie `inventory full`, zeby gracz nie tracil lupu po
  cichu,
- po powrocie z lochu repair, salvage, stash i upgrade sa logicznie dostepne w
  jednym rejonie huba (`discovery-npc-board`),
- stash jest punktem, w ktorym gracz odklada dorobek przed kolejna wyprawa.

## Loadout preparation area

- Loadout preparation area to strefa w hubie do przygotowania setupow przed
  dungeonem (`stolica-wyspy-hub`).
- Jest czescia petli powrotu do huba: repair, loadout, upgrade, kolejny cel
  (`onboarding-tutorial`).
- Pierwszy loadout albo jego tutorial gracz poznaje w hubie podczas onboardingu
  (`onboarding-tutorial`), nie jest rzucany na gleboka wode od razu.

## Guild bank (odsylacz)

- Guild bank to osobny system opisany w `guilds-foundation` (Guild District).
- Dostep do guild bank jest bramkowany uprawnieniami rang gildii.
- Ten dokument tylko wskazuje guild bank jako odrebny system i nie powiela jego
  regul; bank osobisty i guild bank to dwie rozne rzeczy.

## Fairness i anti-abuse

- Loadout nie omija cooldownow, combat timerow ani wymagan buildowych
  (`progression-respec`).
- PvP combat lockout zapobiega instant counter-pickowi w trakcie walki: setupu
  nie zmienia sie tuz po obrazeniach.
- Storage (bank, stash, material storage) nie daje combat power; to porzadek i
  wygoda, nie statystyki.
- Zmiana loadoutu i dostep do banku odbywaja sie w safe zone, wiec nie sa
  narzedziem griefu ani ucieczki z walki.

## Test Cases

- Zmiana loadoutu jest zablokowana w trakcie walki i dziala w safe zone.
- Loadout nie resetuje cooldownow ani nie omija wymagan statow, mastery i itemow.
- PvP combat lockout blokuje zmiane loadoutu krotko po otrzymaniu lub zadaniu
  obrazen.
- Bank, stash i material storage nie daja graczowi przewagi bojowej.
- Po powrocie z lochu repair, salvage, stash i upgrade sa dostepne w hubie.
- System sygnalizuje `inventory full`, gdy ekwipunek jest pelny.
- Guild bank pozostaje osobnym systemem bramkowanym uprawnieniami rang gildii.

## Assumptions

- To jest wersja v0.0.1, fundament do dalszego dopracowania.
- Loadout, bank i storage to warstwa wygody i przygotowania, nie progresji mocy.
- Guild bank jest zdefiniowany osobno w `guilds-foundation`.
- Nie definiujemy jeszcze liczby slotow, limitow stackow, czasow combat lockout
  ani plugin configow.
- Ten dokument konsoliduje decyzje juz obecne w innych fundamentach i nie tworzy
  sprzecznych regul.

## Integration

Ten dokument musi pozostac spojny z:

- docs/progression-respec-foundation-v0.0.1.md (## Loadouts, zasady zmiany i
  combat lockout).
- docs/stolica-wyspy-hub-foundation-v0.0.1.md (### Bank / Stash, placement,
  loadout preparation area, safe zone).
- docs/discovery-npc-board-loop-001-foundation-v0.0.1.md (### Stash Menu,
  inventory full, petla powrotu repair/salvage/stash/upgrade).
- docs/professions-foundation-v0.0.1.md (material storage, torby/utility storage).
- docs/economy-crafting-loot-foundation-v0.0.1.md (waluty, tokeny i materialy
  trzymane w storage).
- docs/guilds-foundation-v0.0.1.md (guild bank jako osobny, bramkowany system).
- docs/onboarding-tutorial-foundation-v0.0.1.md (pierwszy loadout, petla powrotu
  do huba).

## Out Of Scope

- Liczba slotow banku i stash oraz limity stackow.
- Czasy combat lockout i finalne wartosci.
- Plugin configi i mapowanie na konkretny storage albo loadout plugin.
- Guild bank (osobny system w `guilds-foundation`).
- System torb i utility storage, jesli powstanie.
- Konkretne layouty UI menu banku, stash i loadoutow.
