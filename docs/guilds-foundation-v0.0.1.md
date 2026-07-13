# Guilds Foundation v0.0.1

## Cel dokumentu

Ten dokument zbiera w jednym miejscu fundament gildii dla serwera MCMMORPG. Do
tej pory gildie byly wzmiankowane w wielu fundamentach (world content, ekonomia,
hub, PvP, roadmap), ale bez wspolnego opisu systemu. Wiele docow odsylalo do
przyszlego `guilds-foundation`: PvP odlozyl tu reguly wojen gildii (objectives,
siege, scoring), a reputacja odlozyla tu reputacje i range w gildii. Ten
dokument je konsoliduje.

Gildie sa dla serwera filarem spolecznym i grupowym endgame. Roadmap opiera czesc
endgame na wojnach gildii i kontroli terytoriow, wiec gildie musza dawac wspolne
cele bez zamykania serwera dla graczy solo i slabszych grup.

To nie jest finalny system. To wersja v0.0.1. Nie definiujemy tu finalnych
wartosci bonusow terytorialnych, czasow siege windows, formuly scoringu wojny ani
plugin configow. Definiujemy zasady, role i counterplay.

## Filozofia gildii

Zasady prowadzace caly system:

- Gildia daje tozsamosc i wspolne cele, nie obowiazkowa przewage statystyk.
- Gildie sa filarem grupowym i endgame, ale nie zamykaja serwera dla graczy solo
  ani slabszych gildii.
- Walka gildii toczy sie o objective, nie o sam kill count.
- Wojny maja siege windows, nie calodobowy grief.
- Rewardy gildii sa horyzontalne i prestizowe (kosmetyka, ekonomia, wspolne
  projekty), nie broken combat power.
- Dominujaca gildia trafia na soft anti-snowball limity, zeby jedna grupa nie
  zamrazala serwera.
- Solo gracz zawsze ma sensowna sciezke podstawowej progresji bez gildii.

## Czym jest gildia

- Gildia to zorganizowana grupa graczy o wspolnej tozsamosci, banku i celach.
- Gildie zaklada sie przez guild creation NPC w Guild District huba
  (`stolica-wyspy-hub`).
- Gildia ma strukture rang i uprawnien (kto zaprasza, kto zarzadza bankiem, kto
  startuje wojny i projekty). Konkretne nazwy rang i tabela uprawnien sa poza
  zakresem tej wersji.
- Czlonkostwo jest dobrowolne. Gracz moze grac solo i dolaczyc do gildii pozniej.
- Reputacja i ranga wewnatrz gildii (odlozone z `reputation-faction-foundation`)
  naleza do tego systemu: opisane jakosciowo, bez finalnych progow.

## Progresja i projekty gildii

- Gildia rozwija sie przez wspolne projekty, a nie przez rozdawanie przewagi
  bojowej czlonkom.
- Guild projects moga pochlaniac duze ilosci materialow (guild materials,
  `economy-crafting-loot`) bez dawania broken combat power. To celowy sink
  ekonomiczny i wspolny cel, nie power creep.
- Guild bank przechowuje wspolne zasoby; dostep jest bramkowany uprawnieniami
  rang.
- Guild upgrades, guild crafting i guild cosmetics sa nagrodami prestizowymi i
  funkcjonalnymi (np. uslugi, kosmetyka, wygoda), nie obowiazkowym best-in-slot.
- Wklad czlonkow (member contributions) zasila bank i projekty, co daje slabszym
  czlonkom sensowny sposob udzialu bez wymogu top PvP.

## Guild District (hub)

Centrum gildii w hubie, spojne z `stolica-wyspy-hub` (### Guild District):

- guild creation NPC,
- guild bank,
- guild quest board,
- guild war board,
- territory info,
- guild vendor.

Guild District jest punktem startowym dla zakladania gildii, zarzadzania bankiem,
przyjmowania celow gildii i koordynacji wojen. Hub pozostaje safe zone; PvP w
hubie jest dobrowolne i bez griefu, takze dla czlonkow gildii (`pvp-foundation`,
`stolica-wyspy-hub`).

## Guild content

- Guild quest board daje wspolne cele PvE niezalezne od wojny: questy gildyjne i
  zlecenia dla grupy.
- Guild bosses (`economy-crafting-loot`) sa grupowa trescia powiazana z gildia i
  jej ekonomia.
- Dostep do tresci gildii (guild content access) wprowadzany jest w srodkowej i
  poznej czesci podrozy gracza (`player-journey-milestone-roadmap`), nie na
  starcie.
- Tresc gildii ma wspierac wspolprace, ale nie byc jedyna droga progresji dla
  graczy bez gildii.

## Terytoria i wojny gildii

Konsolidacja `world-content-loop` (Guild Territory, Guild Wars). Tu domykamy dlug
z `pvp-foundation` (objectives, siege, scoring).

Zasady terytoriow i wojen:

- Guild territory to region powiazany z kontrola gildii.
- Walka toczy sie o objective, nie o samo zabijanie.
- Wojny maja siege windows zamiast permanentnego, calodobowego griefu.
- Terytoria moga dawac bonusy, ale nie moga zamknac serwera dla slabszych gildii.
- Defender advantage istnieje, ale atakujacy musi miec realna szanse. Obrona nie
  moze byc nie do przejscia.
- Rewardy gildii istnieja, ale z limitami snowballu, i wspieraja gildie, crafting,
  ekonomie oraz prestiz.
- Dominujaca gildia ma soft anti-snowball limity.
- Solo gracze nie moga byc calkowicie odcieci od podstawowej progresji przez
  kontrole terytoriow.

Mozliwe objective wojny (z `world-content-loop`):

- Capture core.
- Hold point.
- Destroy gate.
- Escort siege engine.
- Control resource node.
- Relic extraction.
- Boss kill race w contested guild territory.

Role w wojnie gildii (z `world-content-loop`, spojne z rolami z
`combat-foundation` i `class-subclass-foundation`):

- Frontline tank.
- Bruiser.
- Ranged pressure.
- Caster AoE.
- Healer/support.
- Scout/stealth.
- Siege utility.
- Anti-stealth/detection.
- Objective runner.

Wojna gildii jest warstwa PvP zdefiniowana w `pvp-foundation` (tryb Guild War):
friendly fire moze byc wlaczone w trybie wojny gildii, mimo ze domyslnie jest
wylaczone. Formula scoringu i matchmaking sa poza zakresem tej wersji.

## Ekonomia gildii

Konsolidacja `economy-crafting-loot` (### Guild Materials, ## Guild Economy), bez
powielania finalnych wartosci.

- Guild economy wspiera wojny i wspolne cele.
- Zrodla: territory resources, guild war wins, guild events, guild bosses, member
  contributions, contested objectives.
- Wydawanie: siege equipment, territory upgrades, defense structures, guild
  crafting, guild bank upgrades, cosmetics, event activation.
- Guild materials sa osobnym torem materialow zasilajacym guild projects i
  upgrades.
- Lup z wojny gildii ma osobne zasady: najczesciej koszt napraw i siege
  resources, a nie pelna utrata ekwipunku (`economy-crafting-loot`, Lup risk).
- Uprawnienia (guild permission) kontroluja dostep do banku, projektow i wydatkow
  gildii.

## Anti-snowball i fairness

- Terytoria daja bonusy, ale projektowo nie zamykaja serwera dla slabszych
  gildii.
- Dominujaca gildia trafia na soft anti-snowball limity, zeby kontrola nie
  betonowala mapy.
- Siege windows ograniczaja wojne do okreslonych okien, eliminujac calodobowy
  grief.
- Defender ma przewage, ale atakujacy realna szanse: zaden uklad nie jest nie do
  ruszenia.
- Solo gracz i mala gildia maja zawsze dostep do podstawowej progresji poza
  wojna o terytoria.
- Nagrody za objective waza wiecej niz nagrody za sam kill, co odciaga wojne od
  griefu slabszych.

## Onboarding gildii

- Wprowadzenie do gildii (guild introduction) i guild onboarding quest naleza do
  poznej czesci onboardingu, nie do pierwszej sesji (`onboarding-tutorial`,
  `player-journey-milestone-roadmap`).
- Ciezsze systemy gildii (wojny, terytoria) maja wlasny, pozniejszy onboarding, a
  nie sa zrzucane na nowego gracza naraz.
- Gracz najpierw poznaje hub, walke i pierwsze dungeony, a dopiero pozniej gildie
  jako warstwe grupowa.

## Test Cases

- Gracz solo robi sensowny progres podstawowy bez nalezenia do gildii.
- Kontrola terytoriow przez silna gildie nie odcina slabszych graczy od
  podstawowej progresji.
- Wojna gildii toczy sie o objective w siege window, nie jako calodobowy grief.
- Defender ma przewage, ale atakujaca gildia ma realna szanse zdobycia objective.
- Dominujaca gildia trafia na soft anti-snowball limity.
- Guild projects pochlaniaja duze ilosci materialow bez dawania broken combat
  power.
- Lup z wojny gildii to koszt napraw i siege resources, nie pelna utrata
  ekwipunku.
- Dostep do banku i projektow gildii jest bramkowany uprawnieniami rang.

## Assumptions

- To jest wersja v0.0.1, fundament do dalszego dopracowania.
- Gildie sa filarem spolecznym i grupowym endgame obok PvE, PvP, reputacji i
  craftingu.
- Wojna gildii jest warstwa PvP zdefiniowana w `pvp-foundation` (tryb Guild War).
- Nie definiujemy jeszcze finalnych wartosci, czasow, formul ani plugin configow.
- Ten dokument konsoliduje decyzje juz obecne w innych fundamentach i nie tworzy
  sprzecznych regul.

## Integration

Ten dokument musi pozostac spojny z:

- docs/world-content-loop-foundation-v0.0.1.md (Guild Territory, Guild Wars,
  objectives, role).
- docs/economy-crafting-loot-foundation-v0.0.1.md (Guild Materials, Guild
  Economy, guild war lup risk).
- docs/stolica-wyspy-hub-foundation-v0.0.1.md (Guild District, guild bank, boardy,
  safe zone huba).
- docs/pvp-foundation-v0.0.1.md (tryb Guild War, friendly fire w wojnie, siege
  jako warstwa PvP).
- docs/reputation-faction-foundation-v0.0.1.md (reputacja i ranga w gildii).
- docs/player-journey-milestone-roadmap-v0.0.1.md (guild introduction, guild
  onboarding quest, dostep do tresci gildii).
- docs/onboarding-tutorial-foundation-v0.0.1.md (pozniejszy onboarding gildii).
- docs/combat-foundation-v0.0.1.md (role bojowe wykorzystywane w wojnie gildii).
- docs/class-subclass-foundation-v0.0.1.md (role klas i podklas w wojnie).

## Out Of Scope

- Konkretne nazwy rang gildii i pelna tabela uprawnien.
- Finalne czasy siege windows i kalendarz wojen.
- Mapa terytoriow i przypisanie regionow do gildii.
- Wartosci bonusow terytorialnych, nagrod i kosztow projektow.
- Formula scoringu wojny gildii i matchmaking.
- Plugin configi i mapowanie na konkretny guild plugin.
- Sezonowe rankingi i resety gildii (przyszly dokument sezonow/prestige).
