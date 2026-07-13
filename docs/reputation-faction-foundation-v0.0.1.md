# Reputation And Faction Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje fundament systemu reputacji i frakcji dla serwera
MCMMORPG. Do tej pory reputacja i frakcje byly wzmiankowane w wielu innych
fundamentach (world content, ekonomia, roadmap, questy, hub), ale bez wspolnego
opisu systemu. Ten dokument je konsoliduje.

Reputacja i frakcje maja dawac dlugoterminowe, horyzontalne cele poza samym
levelem. To filar progresji obok PvE, PvP, craftingu i gildii.

To nie jest finalny system. To wersja v0.0.1. Nie definiujemy tu konkretnych
frakcji, ich nazw, lore ani mapy swiata (to osobny etap, zgodnie z
`world-content-loop`). Nie definiujemy finalnych progow, capow, wartosci ani
plugin configow. Definiujemy system, zasady i counterplay dla abuse.

## Filozofia

Zasady prowadzace caly system:

- Reputacja to progres horyzontalny, nie power creep. Ma dawac tozsamosc i
  opcje, nie liniowa przewage.
- Reputacja jest opt-in. Gracz wybiera, ktore frakcje wspiera.
- Reputacja nie moze dawac obowiazkowego best-in-slot dla kazdego.
- Frakcje wspieraja style gry, ale nie blokuja podstawowych mechanik.
- Gracz solo musi miec sensowna droge reputacji.
- System musi byc odporny na grind altami i farmienie abuse.
- Reputacja nie zastepuje bossow, dungeonow, PvP ani craftingu. Jest rownoleglym
  filarem, nie zamiennikiem.

## Czym jest reputacja

- Reputacja to standing gracza wzgledem konkretnej frakcji.
- Zdobywa sie ja przez aktywnosci powiazane z dana frakcja.
- Progi/tiery reputacji bramkuja dostep do nagrod (opisane jakosciowo, bez
  finalnych wartosci).
- Reputacje mozna tracic, np. przez dzialanie przeciw frakcji albo wspieranie
  frakcji przeciwnej.
- Reputacja jest dlugoterminowa: cel na tygodnie gry, nie na jedna sesje.

## Czym sa frakcje

- Frakcja to grupa o wlasnej tozsamosci, ofercie i stylu nagrod.
- Na serwerze istnieje wiele frakcji.
- Frakcje moga byc w konflikcie. Jako opcja designu: wzrost reputacji u jednej
  frakcji moze kosztowac reputacje u frakcji przeciwnej. Decyzja gracza ma miec
  wage.
- Gracz wybiera, ktore frakcje wspiera, i moze zmieniac kierunek, ale nie bez
  kosztu.
- Frakcje daja tozsamosc w PvP (kto jest sojusznikiem, kto wrogiem) i zasilaja
  faction conflict jako typ world eventu.
- Konkretne nazwy frakcji, lore i przypisanie do regionow sa poza zakresem tej
  wersji.

## Zrodla reputacji

Reputacje zdobywa sie przez aktywnosci, nie przez jeden grind. Zrodla (spojne z
`quest-contract-objective`, `discovery-npc-board-loop-001`, `economy` i
`world-content-loop`):

- reputation quests,
- kontrakty i zlecenia z boardow huba,
- faction events,
- aktywnosci regionalne,
- faction dungeons,
- delivery / caravan tasks,
- world events typu faction conflict.

Roznorodnosc zrodel ma pozwalac graczowi budowac reputacje stylem, ktory lubi,
zamiast jednej powtarzalnej petli.

## Nagrody reputacji

Reputacja moze odblokowywac (lista z `world-content-loop`):

- vendor unlocks,
- crafting recipes,
- dungeon keys,
- cosmetic rewards,
- faction gear bases,
- region access,
- daily / weekly quests,
- PvP identity,
- unique utility items.

Nagrody maja byc atrakcyjne i tozsamosciowe, ale nie obowiazkowe dla kazdego
buildu. Cosmetics, utility i tozsamosc sa zdrowsze jako reward niz czysta
przewaga statystyk.

## Faction currency

- Waluta frakcyjna jest powiazana z reputacja i opisana w `economy-crafting-loot`
  (sekcja Faction Currency).
- Zrodla i zastosowania zostaja zdefiniowane tam, zeby nie duplikowac wartosci.
- W skrocie: zdobywana z aktywnosci frakcyjnych, wydawana na vendor unlocks,
  recipes, cosmetics, faction bases i utility items.

## Zasady balansu

- Brak obowiazkowego best-in-slot z reputacji dla kazdego.
- Mocne nagrody maja dzienny albo tygodniowy limit zdobywania.
- Zawsze istnieje sensowna sciezka solo.
- Reputacja wspiera styl gry, ale nie blokuje podstawowych mechanik.
- Reputacja nie zastepuje bossow, dungeonow, PvP ani craftingu.
- Konflikt frakcji daje wybory z konsekwencjami, nie darmowe maksowanie
  wszystkiego naraz.

## Anti-abuse i anti-grief

- Frakcje, zwlaszcza PvP, musza miec reguly anty-abuse dla farmienia altow.
- Powtarzalne zrodla reputacji maja limity, zeby uniknac bezmyslnego grindu.
- Reputacji nie da sie kupic w sposob omijajacy wysilek (brak pay-to-skip).
- Repeat-farm tych samych celow ma diminishing returns, spojnie z regulami
  rewardu w `economy-crafting-loot`.

## Test Cases

- Gracz solo robi sensowny progres reputacji bez party.
- Mocna nagroda reputacyjna ma limit dzienny/tygodniowy.
- Reputacja nie jest obowiazkowym best-in-slot dla danej klasy.
- Jezeli przyjmiemy konflikt frakcji, wzrost u frakcji A swiadomie kosztuje
  standing u przeciwnej frakcji B.
- Farmienie reputacji altami jest ograniczone regulami anty-abuse.
- Reputacja daje cele na dluzej niz jedna sesja, ale nie zastepuje innych
  filarow gry.

## Assumptions

- To jest wersja v0.0.1, fundament do dalszego dopracowania.
- Reputacja jest filarem horyzontalnym obok PvE, PvP, gildii i craftingu.
- Ten dokument definiuje system reputacji i frakcji, nie konkretne frakcje.
- Nie definiujemy jeszcze finalnych wartosci, progow, capow ani plugin configow.
- Dokument konsoliduje decyzje juz obecne w innych fundamentach i nie tworzy
  sprzecznych regul.

## Integration

Ten dokument musi pozostac spojny z:

- docs/world-content-loop-foundation-v0.0.1.md (Factions And Reputation, world
  events, faction conflict).
- docs/economy-crafting-loot-foundation-v0.0.1.md (Faction Currency).
- docs/player-journey-milestone-roadmap-v0.0.1.md (first faction contact,
  faction reputation loop).
- docs/quest-contract-objective-foundation-v0.0.1.md (reputation quests,
  kontrakty).
- docs/discovery-npc-board-loop-001-foundation-v0.0.1.md (boardy jako zrodlo
  reputacji).
- docs/stolica-wyspy-hub-foundation-v0.0.1.md (vendorzy i uslugi odblokowywane
  reputacja).
- docs/pvp-foundation-v0.0.1.md (faction identity i faction conflict w PvP).
- docs/professions-foundation-v0.0.1.md (region gathering i contested aktywnosci
  frakcyjne).

## Out Of Scope

- Konkretne frakcje, ich nazwy, lore i przypisanie do regionow oraz mapy swiata
  (osobny etap).
- Finalne progi reputacji, capy, wartosci gain/loss i tabele nagrod.
- Plugin configi i mapowanie na konkretne pluginy.
- Dokladna formula zdobywania i tracenia reputacji.
- Szczegolowa reputacja i ranga w gildiach (przyszly guilds-foundation).
- Sezonowe resety albo sezonowe nagrody reputacyjne.
