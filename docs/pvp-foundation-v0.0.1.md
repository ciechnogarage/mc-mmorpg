# PvP Foundation v0.0.1

## Cel dokumentu

Ten dokument zbiera w jednym miejscu fundament PvP dla serwera MCMMORPG. Do tej
pory zasady PvP byly rozproszone po innych fundamentach (walka, obrazenia,
itemizacja, ekonomia, world content, roadmap), co grozilo niespojnoscia. Ten
dokument je konsoliduje i ustala wspolny jezyk dla projektowania klas, itemow,
stref i endgame.

To nie jest finalny system. To wersja v0.0.1. Nie definiujemy tu finalnych
wartosci soft capow, procentow redukcji leczenia, czasow combat timera,
cooldownow ani plugin configow. Definiujemy zasady, kierunki i counterplay.

PvP jest dla tego serwera kluczowe. Roadmap opiera caly endgame na rankingu,
contested zones i wojnach gildii. Dlatego kazda mocna mechanika musi miec
counterplay, a gracz ma wygrywac przez umiejetnosc, nie przez sam gear score.

## Filozofia PvP

Zasady prowadzace caly system:

- Gracz wygrywa przez timing, pozycjonowanie, zarzadzanie zasobami i znajomosc
  counterplayu, nie przez sama przewage ekwipunku.
- Kazda mocna mechanika (burst, CC, stealth, heal, summon, tank) musi miec
  jasna kontre.
- PvP jest wprowadzane stopniowo, nie zrzucone na gracza od pierwszych minut.
- Nowi i slabsi gracze nie moga byc griefowani przez silniejszych bez ryzyka i
  ograniczen.
- Przewaga gearu istnieje, ale nie kasuje skill expression i nie robi
  natychmiastowego one-shota.
- System balansujemy bazowo pod 1v1 i 3v3, ale musi dzialac tez w open world i
  wojnach gildii.

## Tryby PvP

PvP nie jest jednym trybem, tylko warstwa kilku kontekstow o roznym ryzyku.

### Duel (1v1)

- Dobrowolny pojedynek za zgoda obu stron.
- Niskie ryzyko, brak utraty itemow.
- Sluzy do nauki counterplayu i testowania buildow.
- Wprowadzany wczesnie jako opcjonalny tutorial PvP.

### Arena (1v1 / 3v3)

- Strukturalny, zbalansowany PvP w kontrolowanym srodowisku.
- Glowny punkt odniesienia dla balansu bazowego.
- Moze byc zrodlem PvP currency i rankingu (szczegoly w `economy` i przyszlym
  dokumencie rankingu).

### Open World Flagged

- PvP wlaczane przez flage/stan, nie wymuszane wszedzie.
- Hub i strefy startowe pozostaja safe zones (zgodnie z `stolica-wyspy-hub`).
- Sluzy do contested gatheringu, zasadzek i ryzykownej eksploracji.

### Contested Zones

- Regiony wyzszego ryzyka i lepszych rewardow (definicja w `world-content-loop`
  i `economy-crafting-loot`).
- Aktywny combat timer i anti-logout.
- Zrodlo PvP currency, reputacji i materialow wyzszego tieru.
- Ryzyko jest jasno oznaczone, zanim gracz wejdzie.

### Guild War

- Warstwa endgame dla wojen gildii i kontroli terytoriow.
- Tutaj traktowana jako placeholder. Szczegolowe reguly (objectives, siege,
  scoring) naleza do osobnego, przyszlego `guilds-foundation`.

### Os czasu wprowadzania (z roadmap)

- Early: PvP opcjonalne i niskiego ryzyka (duel, arena tutorial).
- Mid: pierwsze contested zones i PvP objectives.
- Endgame: ranking PvP, wojny gildii, contested world events.

## Balans bazowy PvP

Reguly skonsolidowane z `combat-foundation` i `damage-defense-foundation`.

- Leczenie w PvP ma obnizona skutecznosc, zeby walki nie byly nieskonczone.
- Anti-heal ma limit, zeby healer nie stawal sie bezuzyteczny.
- CC ma diminishing returns. Po kilku efektach kontroli gracz dostaje krotka
  odpornosc.
- Tenacity, Poise i diminishing returns sa podstawa balansu CC.
- Stun jest krotki. Root i silence sa zdrowsze niz dlugi stun. Perma-control
  jest zakazany projektowo.
- AoE damage moze miec soft cap przy trafianiu wielu graczy naraz.
- Summony maja ograniczona liczbe i mniejsza skutecznosc przeciw graczom.
  Ownera summonera mozna nacisnac bezposrednio.
- Tank ma wysoka przezywalnosc, ale nizszy kill pressure. Upgrade tanka nie
  usuwa jego kontr.
- Burst klasy musza miec moment wejscia i moment slabosci po nieudanym wejsciu.
- Gear scaling w PvP ma soft capy ograniczajace one-shotowanie slabszych
  graczy. Przewaga gearu daje przewage, nie natychmiastowa egzekucje.
- Combat timer i anti-logout ograniczaja ucieczke i wylogowanie w walce.
- Friendly fire domyslnie wylaczone, poza specjalnymi trybami (np. tryb wojny
  gildii).

## Counterplay matrix

Kazdy archetyp ma jasna kontre. To rozwiniecie zasady "Attack Design" z
`combat-foundation`: kazdy mocny atak ma przynajmniej jeden counterplay.

- Stealth / opener z ukrycia: reveal, tracking, AoE wyrywajace ze stealth,
  aktywny DoT, light level, combat timer.
- Healer / sustain: anti-heal, silence, presja burstem w oknie braku zasobow.
- Summoner: presja bezposrednio na ownera, zabicie summonow, limity summonow.
- Caster / cast time: interrupt, line of sight, baitowanie wardu, presja melee.
- Tank / turtle za tarcza: guard break, anti-heal, flankowanie, ataki od tylu,
  DoT i chaos damage.
- Burst / assassin: baitowanie wejscia i karanie po cooldownach, dash bait,
  Poise/Tenacity przeciw chain CC.
- Ranged / kiting: line of sight, przeszkody, mobilnosc, gap closer.

Zasada: jezeli jakas strategia nie ma counterplayu, jest do poprawy, a nie do
zostawienia.

## Anti-grief i ochrona nowych graczy

- Hub i strefy startowe sa safe zones bez otwartego PvP.
- Nowi i nisko-poziomowi gracze nie moga byc wolnym celem dla wysoko-poziomowych
  bez ryzyka i ograniczen.
- Repeat-kill tego samego gracza ma diminishing returns na reward (spojne z
  `economy-crafting-loot`).
- Open world PvP wymaga kontekstu (flaga, contested zone), nie jest globalne i
  bezwarunkowe.
- PvP economy nie moze opierac sie na griefowaniu slabszych graczy.

## Ryzyko i reward

Reguly spojne z `economy-crafting-loot` i `world-content-loop` (bez powielania
finalnych liczb i tabel).

- Contested zones daja lepsze materialy i PvP currency w zamian za realne
  ryzyko.
- W contested zones mozliwa jest ograniczona utrata resource bag, nie pelnego
  ekwipunku.
- PvP currency pochodzi z arena, contested objectives i rankingu.
- PvP currency nie powinna dawac niekontrolowanej przewagi w PvE, a PvP nie moze
  stawac sie obowiazkowym grindem PvE.
- Unique itemy, enchanty, runy, sockety i consumable wymagaja PvP sanity check,
  zeby nie lamac balansu (zgodnie z `itemization-foundation`).

## Realia Minecraft i latency

- To jest serwer Minecraft, nie singleplayer. Parry, dodge i interrupt musza
  miec wybaczalne okna, bo istnieje opoznienie sieciowe.
- Decyzje krytyczne dla balansu (CC, leczenie, obrazenia) licza sie po stronie
  serwera.
- Mechaniki nie moga wymagac perfekcji na poziomie klatek, bo to byloby
  niesprawiedliwe przy roznym pingu.

## Test Cases

- Gracz z niskim gearem nie ginie od jednego ciosu gracza z wysokim gearem.
- Chain CC na jednym graczu szybko wpada w diminishing returns.
- Healer pozostaje uzyteczny mimo anti-heal w PvP.
- Caster ma realna szanse, ale jego dlugi cast mozna przerwac interruptem.
- Stealth opener mozna skontrowac revealem, trackingiem albo dobrym
  pozycjonowaniem.
- Nowy gracz w strefie startowej nie moze byc zabity przez wysoko-poziomowego.
- Wielokrotne zabicie tego samego gracza daje coraz mniejszy reward.
- Wejscie w contested zone daje lepszy reward, ale realne ryzyko i combat timer.

## Assumptions

- To jest wersja v0.0.1, fundament do dalszego dopracowania.
- PvP jest kluczowe, wiec kazda mocna mechanika musi miec counterplay.
- Balans bazowy projektujemy pod 1v1/3v3, ale system musi dzialac w open world i
  wojnach gildii.
- Nie definiujemy jeszcze finalnych wartosci ani plugin configow.
- Ten dokument konsoliduje decyzje juz obecne w innych fundamentach i nie tworzy
  sprzecznych regul.

## Integration

Ten dokument musi pozostac spojny z:

- docs/combat-foundation-v0.0.1.md (rdzen walki, PvP Rules, counterplay).
- docs/damage-defense-foundation-v0.0.1.md (typy obrazen, Tenacity/Poise, CC).
- docs/itemization-foundation-v0.0.1.md (PvP sanity check ekwipunku).
- docs/economy-crafting-loot-foundation-v0.0.1.md (PvP currency, utrata bag,
  diminishing returns rewardu).
- docs/world-content-loop-foundation-v0.0.1.md (contested zones, combat timer,
  anti-logout).
- docs/player-journey-milestone-roadmap-v0.0.1.md (stopniowe wprowadzanie PvP).
- docs/class-subclass-foundation-v0.0.1.md (role PvE/PvP klas i podklas).
- docs/skill-trees-passives-upgrades-foundation-v0.0.1.md (passive i upgrade
  wplywajace na PvP).
- docs/stolica-wyspy-hub-foundation-v0.0.1.md (safe zones w hubie).
- docs/professions-foundation-v0.0.1.md (contested gathering).

## Out Of Scope

- Finalne wartosci soft capow, procentow redukcji leczenia, czasow combat
  timera i cooldownow.
- Plugin configi i mapowanie na konkretne pluginy.
- Wybor i konfiguracja anti-cheata.
- Szczegolowe reguly wojen gildii i siege (osobny przyszly `guilds-foundation`).
- Formula rankingu PvP i matchmaking.
- Sezonowy PvP ladder i nagrody sezonowe.
