# Onboarding And Tutorial Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje fundament onboardingu i tutoriali dla serwera MCMMORPG:
jak nowy gracz wchodzi w serwer w pierwszych godzinach. Do tej pory tutoriale,
training grounds i nauka podstaw byly wzmiankowane w wielu fundamentach (hub,
discovery, level 1, roadmap), ale bez wspolnego opisu. Ten dokument je
konsoliduje.

To nie jest finalny system. To wersja v0.0.1. Nie definiujemy tu konkretnych
tekstow dialogow, finalnej listy questow ani plugin configow. Definiujemy
filozofie, flow pierwszej sesji i mapowanie: czego uczymy i gdzie.

Cel onboardingu: gracz po wejsciu rozumie pierwsze kroki bez wiki, uczy sie
podstaw walki w praktyce, i naturalnie dochodzi do pierwszego runu dungeonu oraz
do swiadomego wyboru klasy na Level 10.

## Filozofia onboardingu

Zasady prowadzace caly onboarding:

- Nauka przez granie, nie przez sciane tekstu.
- Gracz ma rozumiec pierwsze kroki bez koniecznosci czytania wiki.
- Tutoriale sa opt-in i kontekstowe, nie wymuszonym liniowym korytarzem.
- Systemy odslaniaja sie stopniowo. Nowy gracz nie dostaje wszystkiego naraz.
- Onboarding ma prowadzic do realnej gry (dungeon, hub loop), a nie byc osobnym
  zamknietym samouczkiem.

## Discovery kontra guided

To rozstrzygniecie napiecia miedzy dwoma fundamentami: `stolica-wyspy-hub`
opisuje hub jako centrum tutoriali i treningu, a `discovery-npc-board-loop-001`
mowi, ze hub to miejsce do odkrywania, nie liniowy tutorial.

Rozstrzygniecie:

- Hub jest eksploracyjny. Gracz czuje, ze zwiedza miasto MMO, nie idzie
  korytarzem samouczka.
- Tutoriale istnieja, ale sa rozproszone i opcjonalne: training grounds,
  tutorial NPC, dummy do bicia, stacje profesji.
- Nie ma obowiazkowego liniowego tutoriala blokujacego gre.
- Jednoczesnie gracz po spawnie zawsze ma jasny sygnal, gdzie zaczac i gdzie isc
  dalej, zeby brak liniowosci nie zmienil sie w zagubienie.
- `Latwy` poziom dungeonu jest dostepnym fallbackiem dla graczy, ktorzy chca
  uczyc sie spokojniej.

## First session flow

Domyslny, ale nie wymuszony, przeplyw pierwszej sesji:

1. Spawn w hubie (`stolica-wyspy-hub`).
2. Orientacja: gracz widzi, gdzie sa training grounds, boardy i Portal Nexus.
3. Training grounds: podstawy walki w praktyce na dummy.
4. Pierwszy board albo kontrakt: pierwszy cel poza tutorialem
   (`discovery-npc-board-loop-001`, `quest-contract-objective`).
5. Pierwszy run dungeonu level_1: nauka walki w realnej grze, bez sciany tekstu
   (`level-1-dungeon-island`).
6. Powrot do huba: repair, loadout, upgrade, kolejny cel.

Ten przeplyw jest sugerowany przez UI i NPC, ale gracz moze isc wlasna sciezka i
uczyc sie przez granie.

## Czego uczymy i gdzie

Mapowanie mechanika do miejsca w hubie i grze (bez finalnych wartosci):

- Podstawy walki: training grounds i dummy. Dash, block, parry, counter,
  interrupt, ward (zgodnie z `combat-foundation`).
- Loadout i skille: pierwszy loadout albo jego tutorial w hubie.
- Profesje: onboarding gatheringu i craftingu przy stacjach profesji
  (`professions-foundation`), pokazany jako opcja, nie obowiazek.
- Respec: tutorial respec dla wczesnych pomylek przy buildzie
  (`progression-respec-foundation`).
- PvP: opcjonalny duel/arena tutorial (`pvp-foundation`), wprowadzany pozniej i
  niskiego ryzyka.
- Wybor stylu/klasy: starter skills pozwalaja testowac style przed wyborem na
  Level 10 (`starter-skills-class-progression`).

## Mosty do milestone'ow

- Onboarding prowadzi do tego, ze na Level 10 gracz rozumie, ktora klase chce
  wybrac i dlaczego (`player-journey-milestone-roadmap`).
- Ciezsze systemy (PvP, gildie, crafting endgame) maja wlasne, pozniejsze
  onboardingi, a nie sa wrzucane na nowego gracza naraz.
- Pierwsze kilka runow level_1 plus boardy i kontrakty naturalnie prowadza do
  dalszej progresji.

## Czego unikamy

- Sciany tekstu zamiast nauki przez granie.
- Wymuszonego liniowego tutoriala blokujacego wejscie do gry.
- Gatingu podstawowych mechanik za dlugim samouczkiem.
- Zalewania nowego gracza wszystkimi systemami naraz.
- Sytuacji, w ktorej brak liniowego tutoriala zmienia sie w brak jakiegokolwiek
  kierunku.

## Test Cases

- Nowy gracz po spawnie wie, gdzie zaczac, bez czytania wiki.
- Gracz uczy sie dash, block i parry w praktyce, nie z tekstu.
- Gracz moze pominac formalny tutorial i uczyc sie przez granie, nie tracac
  kierunku.
- Pierwszy run level_1 jest zrozumialy dla gracza solo.
- Profesje i PvP sa pokazane jako opcje, nie obowiazkowy tutorial.
- Na Level 10 gracz rozumie, ktora klase chce wybrac i dlaczego.

## Assumptions

- To jest wersja v0.0.1, fundament do dalszego dopracowania.
- `Stolica Wyspy` jest glownym punktem startowym i centrum treningu.
- Level_1 jest pierwszym dungeonem ucz-przez-granie.
- Nie definiujemy jeszcze finalnych tekstow, listy questow ani plugin configow.
- Dokument konsoliduje decyzje juz obecne w innych fundamentach i nie tworzy
  sprzecznych regul.

## Integration

Ten dokument musi pozostac spojny z:

- docs/stolica-wyspy-hub-foundation-v0.0.1.md (training grounds, tutorial NPC,
  stopniowe uczenie systemow).
- docs/discovery-npc-board-loop-001-foundation-v0.0.1.md (eksploracja zamiast
  liniowego tutoriala, boardy).
- docs/level-1-dungeon-island-foundation-v0.0.1.md (nauka walki przez granie).
- docs/player-journey-milestone-roadmap-v0.0.1.md (Level 1-10 starter, wybor
  klasy na Level 10).
- docs/combat-foundation-v0.0.1.md (czego uczy tutorial walki).
- docs/starter-skills-class-progression-foundation-v0.0.1.md (testowanie stylow
  przed wyborem klasy).
- docs/progression-respec-foundation-v0.0.1.md (respec tutorial).
- docs/professions-foundation-v0.0.1.md (onboarding profesji).
- docs/pvp-foundation-v0.0.1.md (opcjonalny duel/arena tutorial).

## Out Of Scope

- Konkretne teksty dialogow i questow tutorialowych.
- Achievement UI i pelne layouty UI/HUD.
- Plugin configi (silnik NPC, silnik questow).
- Finalna lista i kolejnosc questow onboardingowych.
- Lokalizacja i tlumaczenia.
- Onboarding endgame systemow (guild wars, seasonal) poza wzmianka o ich
  istnieniu.
