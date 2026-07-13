# World Events Foundation v0.0.1

## Cel dokumentu

Ten dokument zbiera w jednym miejscu fundament world events dla serwera MCMMORPG.
Do tej pory world events byly opisane rozproszone: lista typow i zasad w
`world-content-loop` (## World Events), Event Board w `stolica-wyspy-hub`,
faction conflict jako typ world eventu w `reputation-faction`, event rewards i
sinki w `economy-crafting-loot`, seasonal events w `seasons-prestige-cosmetics`,
a oznaczanie PvP eventow w `pvp-foundation`. Nigdzie nie bylo wspolnego opisu
systemu. Ten dokument to konsoliduje.

World events sa warstwa, ktora ozywia swiat i daje powody do ruchu po mapie.
Maja przyciagac graczy do wspolnych aktywnosci, ale nie zastepowac dungeonow,
PvP ani profesji jako kluczowej progresji.

To nie jest finalny system. To wersja v0.0.1. Nie definiujemy tu czasow eventow,
cooldownow spawnow, tabel nagrod ani plugin configow. Definiujemy zasady i
counterplay dla abuse.

## Filozofia

Zasady prowadzace caly system:

- World events robia swiat zywszy i daja powody do ruchu po mapie.
- Event nie jest obowiazkowa farma ani jedynym zrodlem kluczowego progressu.
- Rewardy eventu sa proporcjonalne do ryzyka, nie rozdawane za sama obecnosc.
- Event wspiera rozne role, nie jest tylko zergiem DPS.
- PvP eventy sa jasno oznaczone, zeby nie byly griefem przez zaskoczenie.

## Typy world eventow

Konsolidacja `world-content-loop` (## World Events). Typy eventow, opisane
jakosciowo:

- Invasion — fala wrogow nacierajaca na region.
- Caravan — ruchomy cel do eskorty albo przechwycenia.
- Resource rush — okresowy wzrost dostepnosci surowcow w rejonie.
- Relic capture — walka o zdobycie i utrzymanie obiektu.
- Rare elite spawn — pojawienie sie rzadkiego, mocnego przeciwnika.
- World boss — duzy boss w otwartym swiecie, czesto w contested zone.
- Faction conflict — starcie frakcji zasilane systemem reputacji.
- Corrupted zone — czasowo skazony region o podwyzszonym ryzyku i nagrodzie.
- Dungeon surge — wzmozona aktywnosc albo modyfikatory wokol dungeonow.

Lista typow jest fundamentem, nie zamknieta wyliczanka finalnych eventow.

## Zasady eventu

Konsolidacja `world-content-loop`. Kazdy world event ma trzymac sie zasad:

- Event ma jasny cel, zeby gracz od razu wiedzial, co robic.
- Event ma limit czasu, zeby nie trwal w nieskonczonosc.
- Rewardy sa proporcjonalne do ryzyka.
- PvP eventy musza byc oznaczone.
- Eventy nie sa jedynym zrodlem kluczowego progressu.
- Eventy wspieraja rozne role: damage, tank, healer, scout, controller,
  crafter/logistics.

## Event board

Konsolidacja `stolica-wyspy-hub` (### Event Board). Event board w hubie informuje
graczy o tym, co dzieje sie w swiecie.

Rola:

- informowanie o aktywnosciach swiata.

Zawartosc:

- world events,
- dungeon surges,
- contested zone alerts,
- PvP objectives,
- seasonal events,
- boss timers (w przyszlosci).

Zasada:

- event board ma generowac ruch w swiecie, ale nie robic jednej obowiazkowej
  farmy.

## PvP i faction eventy

- PvP eventy musza byc oznaczone i zgodne z regulami z `pvp-foundation`, zeby
  gracz wchodzil w PvP swiadomie, nie przez zaskoczenie.
- Faction conflict to typ world eventu zasilany frakcjami z
  `reputation-faction`: starcia frakcji sa zrodlem reputacji i tozsamosci w PvP.
- Siege i guild eventy lacza sie z systemem gildii (`guilds-foundation`,
  `economy-crafting-loot`) i podlegaja regulom wojen gildii (siege windows,
  walka o objective, anti-snowball).

## Role w evencie

- Event ma dawac sensowny udzial roznym rolom: damage, tank, healer, scout,
  controller, crafter/logistics (`world-content-loop`).
- Role bojowe i wsparcia pochodza z `combat-foundation` i
  `class-subclass-foundation`; event nie moze nagradzac wylacznie czystego DPS.
- Role nie-bojowe (scout, crafter/logistics) maja miec realny wklad, zeby event
  nie byl tylko zergiem.

## Rewardy i ekonomia eventow

- Eventy sa zrodlem nagrod i sinkow ekonomicznych (`economy-crafting-loot`):
  PvP events, faction events, siege events i guild events zasilaja odpowiednie
  waluty i tory nagrod.
- Rewardy sa proporcjonalne do ryzyka i nie sa obowiazkowym best-in-slot;
  najlepszy dorobek wymaga roznych aktywnosci, nie samej farmy eventow.
- Seasonal events daja sezonowe nagrody i kosmetyke (`seasons-prestige-
  cosmetics`) jako warstwe tozsamosciowa, bez power creep.

## Fairness i anti-abuse

- Event nie jest jedynym zrodlem kluczowego progressu; gracz, ktory go pominie,
  nie zostaje odciety od rozwoju.
- Event board generuje ruch, ale nie wymusza jednej obowiazkowej farmy.
- Solo gracz i mala grupa maja sensowny udzial w evencie, nie tylko duze zergi.
- PvP eventy sa oznaczone, zeby nie byly griefem przez zaskoczenie.
- Anti-snowball z `pvp-foundation` i `guilds-foundation` przenosi sie na eventy
  PvP i siege, zeby jedna grupa nie betonowala kazdego eventu.

## Test Cases

- Kazdy world event ma jasny cel i limit czasu.
- Rewardy eventu skaluja sie z ryzykiem, nie z sama obecnoscia.
- PvP event jest oznaczony, zanim gracz w niego wejdzie.
- Event nie jest jedynym zrodlem kluczowego progressu gracza.
- Rozne role (w tym scout i crafter/logistics) maja sensowny udzial w evencie.
- Event board pokazuje aktywne eventy i nie wymusza jednej obowiazkowej farmy.
- Seasonal event daje sezonowe nagrody i kosmetyke bez power creep.

## Assumptions

- To jest wersja v0.0.1, fundament do dalszego dopracowania.
- World events to warstwa ozywiajaca swiat obok dungeonow, PvP i profesji.
- Faction conflict jest zdefiniowany przez frakcje z `reputation-faction`.
- Seasonal events sa czescia warstwy sezonowej z `seasons-prestige-cosmetics`.
- Nie definiujemy jeszcze czasow eventow, cooldownow spawnow, tabel nagrod ani
  plugin configow.
- Ten dokument konsoliduje decyzje juz obecne w innych fundamentach i nie tworzy
  sprzecznych regul.

## Integration

Ten dokument musi pozostac spojny z:

- docs/world-content-loop-foundation-v0.0.1.md (## World Events, typy i zasady,
  ruch po mapie).
- docs/stolica-wyspy-hub-foundation-v0.0.1.md (### Event Board, zawartosc i
  zasada ruchu).
- docs/reputation-faction-foundation-v0.0.1.md (faction conflict jako world
  event, faction events jako zrodlo reputacji).
- docs/economy-crafting-loot-foundation-v0.0.1.md (event rewards i sinki, waluty
  PvP/faction/siege/guild).
- docs/seasons-prestige-cosmetics-foundation-v0.0.1.md (seasonal events i
  sezonowe nagrody bez power creep).
- docs/pvp-foundation-v0.0.1.md (oznaczanie PvP eventow, reguly PvP w evencie).
- docs/guilds-foundation-v0.0.1.md (siege i guild eventy, anti-snowball).
- docs/combat-foundation-v0.0.1.md (role bojowe w evencie).
- docs/class-subclass-foundation-v0.0.1.md (role klas i podklas w evencie).

## Out Of Scope

- Finalne czasy eventow, cooldowny spawnow i czestotliwosc.
- Tabele nagrod i konkretne wartosci.
- Mapowanie eventow na konkretne regiony i mape.
- Boss timers jako konkretne wartosci czasowe.
- System matchmakingu i instancjonowania eventow.
- Plugin configi i mapowanie na konkretny event albo spawn plugin.
