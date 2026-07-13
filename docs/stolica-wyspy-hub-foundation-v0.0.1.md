# Stolica Wyspy Hub Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje glowny hub serwera MCMMORPG: Stolice Wyspy. Hub ma byc
miastem na wyspie, ktore zawiera najwazniejsze systemy MMO i prowadzi gracza
przez start, klasy, profesje, handel, dungeony, PvP, gildie, swiatynie i
endgame przygotowanie.

Stolica Wyspy nie jest miastem jednej profesji ani osada tematyczna. Kowal,
alchemik, zaklinacz i inni NPC sa czescia miasta, ale nie definiuja jego
tozsamosci. Hub ma byc centralnym operacyjnym miejscem gracza.

Dokument jest projektowy. Nie definiujemy jeszcze finalnego layoutu blok po
bloku, plugin configow, cen, cooldownow, NPC ID ani UI menu.

## Core Role

Stolica Wyspy ma spelniac kilka rol naraz:

- miejsce startu nowej postaci,
- bezpieczna safe zone,
- centrum tutoriali i treningu,
- miejsce wyboru klasy, podklasy i upgrade podklasy,
- centrum profesji, craftingu, repair, salvage i upgrade,
- rynek, bank i stash,
- portal hub do dungeon islands,
- miejsce zbierania party,
- bezpieczna arena PvP i test buildow,
- centrum gildii i przyszlych guild wars,
- miejsce swiatyn, bostw, blessingow i divine questow,
- miejsce powrotu po dungeonach, farmie i PvP.

Hub ma uczyc systemow stopniowo. Nowy gracz nie powinien dostac od razu
dwudziestu menu, ale miasto musi miec przestrzen na systemy od levelu 1 do
endgame.

## Hub Structure

### Spawn Plaza

Rola:

- start gracza,
- pierwsze NPC,
- pierwsze questy,
- podstawowe drogowskazy,
- bezpieczne wyjasnienie serwera.

Zawartosc:

- tutorial NPC,
- tablica kierunkow,
- pierwsze questy ruchu i walki,
- proste wyjasnienie leveli 1-10, level 10 class choice, level 25 subclass,
  level 50 upgrade,
- widoczne wejscia do najwazniejszych dzielnic.

Zasada:

- gracz po spawnie ma wiedziec, gdzie zaczac bez czytania wiki.

### Training Grounds

Rola:

- nauka dynamicznej walki,
- test starter skills,
- test gearu i buildu,
- przygotowanie do dungeonow i PvP.

Zawartosc:

- training dummies,
- tutorial dash,
- tutorial block,
- tutorial parry,
- tutorial counter,
- tutorial interrupt,
- tutorial ward,
- test stealth/backstab,
- test projectile/line of sight,
- test summon/debuff/trap,
- damage i sustain testing.

Zasada:

- training grounds maja uczyc mechanik praktycznie, nie tylko opisem w menu.

### Class District

Rola:

- pokazanie 5 klas bazowych,
- wybor klasy na levelu 10,
- class mentorzy,
- class preview i class trial.

Zawartosc:

- mentor Wojownika,
- mentor Lotrzyka,
- mentor Lowcy,
- mentor Maga,
- mentor Akolity,
- preview roli PvE/PvP,
- rekomendacja na podstawie starter style testers,
- krotki class trial,
- class trainer po wyborze.

Zasada:

- klasy maja byc wyborem stylu gry, nie losowa lista nazw.

### Subclass Hall

Rola:

- preview podklas,
- wybor podklasy na levelu 25,
- upgrade wybranej podklasy na levelu 50.

Zawartosc:

- tablice podklas,
- trial rooms,
- signature skill preview,
- subclass quest NPC,
- upgrade NPC na level 50,
- ostrzezenia o tradeoffach i PvP counterplayu.

Zasada:

- trial ma testowac realny gameplay podklasy, nie tylko wymagac levelu.

### Profession District

Rola:

- centrum profesji,
- crafting stations,
- trenerzy,
- profession quests,
- gathering i crafting onboarding.

Zawartosc:

- trener Gornictwa,
- trener Zielarstwa,
- trener Lowiectwa,
- trener Kowalstwa,
- trener Krawiectwa,
- trener Jubilerstwa,
- trener Alchemii,
- trener Zaklinania / Runotworstwa,
- trener Inzynierii,
- trener Gotowania,
- crafting stations,
- recipe NPC,
- gathering board,
- profession order board.

Zasada:

- profesje leveluja sie przez uzywanie, ale miasto daje onboarding, stacje i
  rynek.

### Market District

Rola:

- handel,
- material economy,
- vendorzy,
- repair,
- salvage,
- upgrade.

Zawartosc:

- auction house,
- direct trade area,
- material market,
- basic vendors,
- repair NPC,
- salvage station,
- upgrade station,
- vendorzy reagentow podstawowych,
- market tax i listing fee jako sinki.

Zasada:

- market ma wspierac handel miedzy graczami, nie robic pelnej
  samowystarczalnosci jednej postaci.

### Bank / Stash

Rola:

- przechowywanie itemow,
- przechowywanie materialow,
- waluty i token overview,
- przygotowanie do dungeonow.

Zawartosc:

- stash,
- material storage,
- currency/token overview,
- loadout preparation area,
- opcjonalnie guild bank access w pozniejszym etapie.

Zasada:

- bank musi byc blisko marketu i portali, bo gracz bedzie wracal tu po
  dungeonach.

### Portal Nexus

Rola:

- glowna brama do dungeon islands,
- party entry,
- difficulty selector,
- jasny status progresji.

Zawartosc:

- portal do level_1 dungeon island,
- future portals do kolejnych dungeon islands,
- difficulty selector: Latwy, Normalny / Klasyczny, Trudny, Bardzo Trudny,
- party size info,
- rewards preview,
- lockout/status board,
- dungeon unlock requirements,
- boss/token info.

Zasada:

- portal musi jasno pokazywac level, difficulty, wymagania, party size i typ
  rewardow.

### Quest Board

Rola:

- centralny system kierowania gracza do aktywnosci.

Zawartosc:

- main quests,
- dungeon unlock quests,
- daily/weekly,
- profession orders,
- bounty,
- event notices,
- class/subclass pointers,
- faction hooks w przyszlosci.

Zasada:

- quest board nie ma byc lista chore'ow; ma wskazywac sensowne aktywnosci.

### PvP Arena

Rola:

- bezpieczne PvP,
- test buildow,
- sparingi,
- ranking/trening w przyszlosci.

Zawartosc:

- duel area,
- unranked practice,
- ranked practice w pozniejszym etapie,
- no-loot PvP,
- spectator area,
- build test rules,
- PvP dummy albo NPC sparring.

Zasada:

- PvP w hubie jest dobrowolne, oznaczone i bez griefu nowych graczy.

### Guild District

Rola:

- centrum gildii,
- przyszle guild wars,
- wspolne cele i ekonomia gildyjna.

Zawartosc:

- guild creation NPC,
- guild bank,
- guild quest board,
- guild war board,
- territory info,
- guild vendor,
- siege/resource hooks w przyszlosci.

Zasada:

- guild district ma przygotowac endgame social loop, ale nie blokowac solo
  progresji.

### Temple District

Rola:

- swiatynie bostw,
- blessing,
- deity reputation,
- offerings,
- divine quests,
- przyszle relic/item/skill hooks.

Zawartosc:

- kilka swiatyn albo kaplic,
- shrine interaction,
- offering altar,
- deity reputation board,
- divine quest NPC,
- blessing preview,
- ostrzezenie o konsekwencjach zmiany bostwa.

Zasada:

- bostwa sa dodatkowa warstwa decyzji buildowo-tozsamosciowej, nie klasa i nie
  darmowy buff do rotowania.

### Respec Shrine / Build Lab

Rola:

- naprawa bledow buildowych,
- testowanie postaci,
- kontrolowana zmiana kierunku.

Zawartosc:

- respec statow,
- respec skilli,
- respec pasywek,
- respec profesji,
- training loadout test,
- koszt i cooldown respecow wysokiego tieru.

Zasada:

- respec ma pomagac, ale nie pozwalac omijac decyzji buildowych przed kazda
  walka.

### Event Board

Rola:

- informowanie o aktywnosciach swiata.

Zawartosc:

- world events,
- dungeon surges,
- contested zone alerts,
- PvP objectives,
- seasonal events,
- boss timers w przyszlosci.

Zasada:

- event board ma generowac ruch w swiecie, ale nie robic jednej obowiazkowej
  farmy.

## Temple / Deity System

Swiatynie maja byc systemem z potencjalem na pozniejsza glebie: buffy, itemy,
questy, reputacje, relikty, a w przyszlosci moze divine skille albo pasywki.
Na etapie huba definiujemy miejsce i zasady, nie finalny panteon.

Zasady:

- bostwa nie moga byc prosto przypisane do klas typu bostwo wojownika, maga albo
  healera,
- kazde bostwo powinno dawac stylowy tradeoff, ktory ma sens dla kilku klas i
  buildow,
- blessing nie moze byc darmowym stale aktywnym power creepem,
- gracz powinien miec tylko jedno glowne aktywne blogoslawienstwo naraz,
- blessing powinien wymagac kosztu, cooldownu, ofiary, reputacji, questu albo
  aktywnosci,
- zmiana bostwa jest mozliwa, ale ma byc rzadka i bolesna,
- zmiana bostwa nie moze byc taktycznym swapem przed dungeonem albo PvP,
- PvP musi miec osobne modyfikatory dla blessingow.

Konsekwencje zmiany bostwa moga obejmowac:

- utrate reputacji u poprzedniego bostwa,
- blokade blessingow na okres pokuty,
- quest pokutny,
- koszt ofiary,
- czasowy debuff,
- utrate dostepu do czesci divine nagrod,
- oslabienie reliktu albo itemu powiazanego z poprzednim bostwem.

## Example Deity Roles

Przyklady sa robocze. Nie finalizuja lore ani nazw panteonu.

### Bostwo Przysiegi

Styl:

- konsekwencja,
- ochrona,
- trzymanie roli,
- nagradzanie utrzymania obietnicy albo wybranego stylu.

Moze wspierac:

- tankow,
- supportow,
- bruiserow,
- objective players,
- solo graczy grajacych stabilnie.

Tradeoff:

- slabsze bonusy przy chaotycznej zmianie taktyki,
- kara za porzucenie celu albo sojusznika,
- mniejszy burst.

### Bostwo Proby

Styl:

- ryzyko,
- walka z silniejszym celem,
- elite mobs,
- boss push,
- trudniejsze PvP.

Moze wspierac:

- PvE push,
- boss hunters,
- PvP graczy bioracych ryzykowne walki,
- glass cannon i bruiser buildy.

Tradeoff:

- slabsze w farmieniu latwych mobow,
- wymaga realnego ryzyka,
- nie powinno nagradzac griefowania slabszych graczy.

### Bostwo Glebii

Styl:

- sustain,
- ward,
- odpornosc,
- dlugie walki,
- stabilnosc.

Moze wspierac:

- casterow,
- tankow,
- warlockow,
- healerow,
- solo graczy.

Tradeoff:

- nizszy burst,
- slabsza mobilnosc albo pressure,
- PvP healing/ward modifiers.

### Bostwo Zmierzchu

Styl:

- stealth,
- reposition,
- debuff,
- wejscie i wyjscie z walki,
- taktyka.

Moze wspierac:

- rogue,
- ranger,
- control mage,
- warlock,
- trap builds.

Tradeoff:

- detection i reveal counterplay,
- slabsze w dlugim frontalnym starciu,
- ograniczone chain-stealth w PvP.

### Bostwo Echa

Styl:

- rotacja,
- combo,
- rytm walki,
- cooldown discipline,
- nagradzanie poprawnej sekwencji.

Moze wspierac:

- kazda klase z jasna rotacja,
- combo melee,
- spell rotations,
- trap/setup builds,
- support timing.

Tradeoff:

- kara za panic spam,
- kara za przerwanie rytmu,
- slabsze, gdy gracz musi czesto zmieniac plan.

### Bostwo Plagi

Styl:

- DoT,
- poison,
- curse,
- anti-heal,
- attrition.

Moze wspierac:

- poison builds,
- warlock,
- trap builds,
- control builds,
- PvE boss attrition.

Tradeoff:

- cleanse i resist counterplay,
- slabsze w szybkim burście,
- PvP anti-heal capy.

### Bostwo Zaru

Styl:

- agresja,
- presja,
- walka na ryzyku,
- mocniejsze okna ofensywne.

Moze wspierac:

- berserk,
- mage,
- assassin,
- glass cannon,
- offensive ranger.

Tradeoff:

- koszt defensywy,
- koszt sustainu,
- ryzyko przy niskim HP,
- cooldowny na burst windows.

### Bostwo Ladu

Styl:

- kontrola pola,
- block,
- poise,
- anti-CC,
- objective play.

Moze wspierac:

- areny,
- guild wars,
- tankow,
- supportow,
- zone control casterow.

Tradeoff:

- nizszy damage,
- mniejsza mobilnosc,
- slabsze w gonieniu celu.

### Bostwo Rzemiosla

Styl:

- crafting,
- quality,
- salvage,
- gathering,
- material efficiency.

Moze wspierac:

- crafterow,
- gathererow,
- ekonomie,
- graczy grajacych marketem.

Tradeoff:

- nie moze omijac limitow profesji,
- nie moze tworzyc combat pay-to-win,
- bonusy powinny byc ekonomiczne albo utility, nie czysty power.

### Bostwo Handlu

Styl:

- trading,
- market,
- merchant utility,
- reputacja,
- kontrakty.

Moze wspierac:

- handlarzy,
- crafterow,
- gildie,
- graczy farmiacych rynek.

Tradeoff:

- nie drukuje waluty,
- nie omija market tax calkowicie,
- nie daje combat power za ekonomie.

## Progressive Unlocks

### Level 1

Dostep:

- Spawn Plaza,
- basic vendors,
- Training Grounds,
- starter skills,
- basic professions,
- Temple District jako lore/tutorial bez mocnego power,
- pierwsze wskazanie Portal Nexus.

### Level 5

Dostep:

- szersze starter testing,
- basic crafting,
- first salvage/repair,
- first dungeon preparation,
- pierwsze slabe czasowe blessing.

### Level 10

Dostep:

- class mentors,
- class trial,
- class choice,
- class trainer,
- pierwsze sensowne deity blessing dopasowane do stylu gry, nie klasy.

### Level 25

Dostep:

- Subclass Hall,
- subclass trials,
- signature skill,
- deeper profession/crafting access,
- deity quests zwiazane ze specjalizacja i reputacja.

### Level 50

Dostep:

- subclass upgrade services,
- endgame dungeon board,
- advanced crafting/upgrading,
- guild/PvP/endgame boards,
- mocniejsze blessing i deity item hooks.

### Level 50+

Dostep:

- endgame target farming,
- high-tier profession services,
- guild war logistics,
- advanced market,
- prestige systems,
- deity reputation/endgame quests.

## Design Rules

Zasady huba:

- Stolica Wyspy jest centralnym operacyjnym hubem MMO.
- Kazda dzielnica musi miec gameplayowy powod istnienia.
- Nowy gracz musi rozumiec pierwsze kroki bez wiki.
- Hub ma uczyc systemow stopniowo.
- Najwazniejsze uslugi musza byc blisko spawnu albo jasno oznaczone.
- PvP w hubie jest bezpieczne i dobrowolne.
- Market i profesje maja wspierac ekonomie, nie samowystarczalnosc.
- Portal Nexus jasno pokazuje dungeon level, difficulty, party size, rewards i
  wymagania.
- Temple District daje dodatkowa warstwe wyboru, ale nie zastapi klas, itemow
  ani profesji.
- Bostwa tworza decyzje z konsekwencjami, nie darmowy buff do rotowania.
- Hub musi byc przygotowany na przyszle rozszerzenia bez przebudowy calego
  miasta.

## Integration

Ten dokument musi pozostac spojny z:

- docs/player-journey-milestone-roadmap-v0.0.1.md.
- docs/world-content-loop-foundation-v0.0.1.md.
- docs/level-1-dungeon-island-foundation-v0.0.1.md.
- docs/dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md.
- docs/dungeon-ladder-002-010-foundation-v0.0.1.md.
- docs/quest-contract-objective-foundation-v0.0.1.md.
- docs/discovery-npc-board-loop-001-foundation-v0.0.1.md.
- docs/professions-foundation-v0.0.1.md.
- docs/economy-crafting-loot-foundation-v0.0.1.md.
- docs/class-subclass-foundation-v0.0.1.md.
- docs/class-skill-kits-foundation-v0.0.1.md.
- docs/combat-foundation-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.

W szczegolnosci:

- `player-journey` okresla unlocki level 1-10, 10, 25, 50 i 50+.
- `professions` okresla profesje, salvage, crafting stations i limity
  specjalizacji.
- `economy` okresla market, tax, repair, salvage, upgrade i currency sinks.
- `classes` okreslaja mentorow, triale, class/subclass wybor i upgrade.
- `combat` okresla training grounds, PvP arena, dummy i tutorial mechanik.
- `world-content` okresla Portal Nexus, dungeon islands, eventy i contested
  zones.
- `level-1-dungeon-island` okresla pierwszy wzorzec wejscia przez Portal Nexus,
  dungeon runu, boss rewardow i powrotu do huba.
- `quest-contract-objective` okresla Quest Board, zlecenia, dungeon objective,
  bounty, profession orders i roznice miedzy questem a achievementem.
- `discovery-npc-board-loop-001` okresla minimalistyczne NPC, boardy, menu i
  komunikaty bez prowadzenia gracza za reke.
- `itemization` okresla gear testing, upgrade, salvage, affixy, runy i PvP
  sanity checks.

## Out Of Scope

Nie robimy jeszcze:

- finalnego layoutu blok po bloku,
- plugin configow,
- NPC ID,
- WorldGuard regionow,
- dungeon portalow,
- finalnych cen i cooldownow,
- finalnego UI menu,
- pelnego lore miasta,
- finalnego panteonu bostw,
- finalnych divine skilli,
- wszystkich dungeon islands.

## Test Cases

Stolica Wyspy v0.0.1 powinna przejsc ponizsze scenariusze:

- Nowy gracz po spawnie wie, gdzie zaczac tutorial i gdzie isc dalej.
- Gracz level 1-10 moze testowac style walki i podstawowe profesje bez wyboru
  klasy.
- Gracz level 10 rozumie wybor klasy i trafia do mentorow.
- Gracz level 25 widzi podklasy, triale i wymagania wyboru.
- Gracz level 50 rozumie upgrade podklasy i endgame services.
- Gracz po dungeonie wraca do huba i wie, gdzie sprzedac, naprawic,
  salvageowac, ulepszyc albo schowac itemy.
- Crafter wie, gdzie rozwijac profesje, kupic podstawowe reagenty i korzystac z
  marketu.
- PvP gracz moze testowac build bez griefowania nowych graczy.
- Party moze zebrac sie w hubie i wejsc do dungeon island przez Portal Nexus.
- Gracz rozumie, ze swiatynie daja opcjonalne blessing/questy, a nie
  obowiazkowa klase.
- Blessing bostwa nie robi jednej oczywistej mety dla jednej klasy.
- Zmiana bostwa ma wyrazne konsekwencje i nie oplaca sie jako szybki swap pod
  walke.
- Hub nie wymusza PvP ani handlu, ale naturalnie pokazuje te systemy.
- Dokument nie uzywa `miasto-kowale` jako nazwy ani motywu.

## Assumptions

- Nazwa robocza huba to `Stolica Wyspy`.
- Hub jest safe zone.
- PvP dziala tylko w wyznaczonych arenach/test zones.
- Dungeon islands sa glowna forma progresji PvE.
- Portal Nexus jest centralnym wejsciem do dungeon islands.
- `loch_002-010` stanowia pierwszy akt dungeon islands po `loch_001`.
- Swiatynie i bostwa beda osobnym przyszlym systemem, ale hub musi miec na nie
  miejsce.
- Zmiana bostwa jest dozwolona, ale kosztowna, rzadka i obciazona
  konsekwencjami.
- Ten dokument porzadkuje funkcje miasta przed implementacja mapy/pluginow.
