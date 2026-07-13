# Profesje fundament v0.0.1

## Cel dokumentu

Ten dokument definiuje system profesji serwera MCMMORPG. Profesje maja byc
osobnym filarem progresji, ekonomii, rzemiosla, lupu i PvP, a nie tylko lista
NPC w miescie.

System ma laczyc:

- Skyrim-like progression: profesja rosnie przez realne uzywanie.
- MMO economy: jedna postac nie wymaksuje wszystkiego do endgame.
- Celowe farmienie: materialy i receptury maja jasne zrodla.
- Handel miedzy graczami: specjalizacje tworza potrzebe rynku.
- PvP sanity check: consumable, enchanty i utility nie moga psuc balansu walki.

Dokument jest projektowy. Nie definiujemy jeszcze finalnych wartosci XP,
szans na wypadniecie, kosztow, cooldownow ani plugin configow.

## Model bazowy

Profesje nie powinny byc wyborem zamykajacym postac na starcie gry. Gracz
powinien moc sprobowac wielu aktywnosci, zobaczyc co mu odpowiada i dopiero na
wyzszych tierach wejsc w realna specjalizacje.

Zasady:

- Profesje leveluja sie przez aktywnosc zwiazana z dana profesja.
- Podstawowe receptury i akcje sa dostepne szeroko.
- Ograniczenia dotycza glownie mocnych receptur, specjalizacji i endgame.
- Gathering jest mniej limitowany niz rzemioslo combat-power.
- Rzemioslo musi wymagac materialow z realnego contentu: dungeonow, bossow,
  regionow, reputacji, contested zones albo PvP objectives.
- Profesje maja wspierac style gry, ale nie moga calkowicie zastapic lupu.
- Jedna postac nie powinna byc samowystarczalna we wszystkim na najwyzszym
  poziomie.

## Profession Categories

Profesje dziela sie na trzy warstwy:

- Gathering: zdobywanie surowcow ze swiata i mobow.
- Rzemioslo: tworzenie itemow, consumable, enchantow i utility.
- Service systems: odzysk, repair, ulepszania i market support.

Service systems nie musza byc osobnymi profesjami. Moga byc wspolnymi
mechanikami, ktore dzialaja lepiej, jesli gracz ma powiazana profesje.

## Gathering Professions

### Gornictwo

Rola:

- rudy,
- metale,
- kamienie,
- krysztaly,
- materialy do kowalstwa, jubilerstwa i zaklinania.

Gameplay:

- eksploracja kopaln, jaskin i dungeon side rooms,
- ryzykowniejsze zloza w contested zones,
- rzadkie krysztaly z bossow, elite mobow albo eventow.

Nie powinno:

- dawac kompletnej sciezki zarobku bez kontaktu z reszta ekonomii,
- byc jedynym zrodlem wszystkich materialow do ekwipunku.

### Zielarstwo

Rola:

- ziola,
- grzyby,
- toksyny,
- skladniki alchemiczne,
- materialy pod potiony, trucizny, cleanse i resist flasks.

Gameplay:

- zbieranie roslin w regionach,
- rzadkie rosliny przy bossach, w dungeonach albo eventach,
- lepsze surowce w bardziej ryzykownych biomach.

Nie powinno:

- samo tworzyc potek bez Alchemii,
- produkowac niekontrolowanych buffow PvP.

### Lowiectwo

Rola:

- skory,
- kosci,
- futra,
- kly,
- pazury,
- czesci bestii i potworow.

Gameplay:

- zabijanie i oprawianie bestii,
- celowe farmienie materialow z konkretnych mobow,
- trofea i rare monster parts z elite mobow.

Nie powinno:

- dublowac pelnej fantazji klasy Lowcy,
- dawac permanentnych petow bojowych bez osobnego systemu klas/skilli.

## Rzemioslo Professions

### Kowalstwo

Rola:

- bron melee,
- ciezka zbroja,
- tarcze,
- fizyczne ulepszenia,
- repair i quality dla metalowego ekwipunku.

Wspiera style gry:

- Wojownik,
- tank,
- bruiser,
- physical melee,
- block/parry style grys.

PvP uwagi:

- quality i durability nie moga tworzyc ekwipunek gapu bez soft capow,
- tarcze i ciezka zbroja musza miec tradeoff w mobilnosci, stamina albo presji.

### Krawiectwo

Rola:

- szaty,
- lekkie i srednie pancerze,
- stealth ekwipunek,
- caster ekwipunek,
- ward/mana cloth items,
- torby albo utility storage, jesli system bedzie tego wymagac.

Wspiera style gry:

- Lotrzyk,
- Mag,
- Akolita,
- evasion,
- ward,
- stealth,
- mobility.

PvP uwagi:

- stealth ekwipunek musi miec counterplay przez detection, AoE, reveal albo armor
  pressure,
- caster ekwipunek nie moze byc tylko slabsza zbroja; musi dawac realny benefit.

### Jubilerstwo

Rola:

- pierscienie,
- amulety,
- talizmany,
- socket materials,
- resisty,
- staty,
- style gry-defining akcesoria.

Wspiera style gry:

- kazda klasa przez staty i resisty,
- elemental style grys,
- crit style grys,
- minion style grys,
- healing/support style grys.

PvP uwagi:

- resist stacking musi miec capy albo diminishing returns,
- akcesoria nie moga byc jedynym zrodlem wszystkich defensyw.

### Alchemia

Rola:

- potiony,
- eliksiry,
- trucizny,
- antidota,
- cleanse,
- resistance flasks,
- krotkie combat buffy.

Wspiera style gry:

- poison,
- sustain,
- burst windows,
- dungeon preparation,
- PvP utility.

PvP uwagi:

- combat potions musza miec cooldowny i limity stackowania,
- trucizny musza miec counterplay przez poison resist, cleanse albo uptime
  ograniczony aplikacja,
- Alchemia nie moze zastapic healera ani defensywnych skilli klasowych.

### Zaklinanie / Runotworstwo

Rola:

- runy,
- enchanty,
- socket effects,
- magiczne modyfikatory,
- ward,
- mana,
- cast speed,
- elemental skalowanie.

Wspiera style gry:

- Mag,
- Akolita,
- elemental damage,
- ward style grys,
- support,
- summon/minion skalowanie przez wybrane runy.

PvP uwagi:

- enchanty nie moga byc czystym best-in-slot bez tradeoffu,
- movement, cooldown reduction, anti-heal i burst modifiers wymagaja osobnych
  PvP sanity checkow,
- runy powinny zmieniac styl gry, nie tylko dokladac wiecej procentow.

### Inzynieria

Rola:

- pulapki,
- bomby,
- utility PvP,
- dungeon tools,
- specjalna amunicja,
- gadzety do kontroli terenu.

Wspiera style gry:

- Sabotazysta,
- Strzelec,
- trap style grys,
- objective PvP,
- dungeon utility.

PvP uwagi:

- bomby i pulapki musza miec limity, telegraphy albo counterplay,
- utility nie moze chain-CC gracza bez mozliwosci reakcji,
- Inzynieria powinna wspierac przygotowanie i pozycjonowanie, nie spam.

### Gotowanie

Rola:

- jedzenie,
- dluzsze pre-combat buffy,
- stamina,
- regen,
- survival,
- party preparation.

Wspiera style gry:

- solo PvE,
- dungeon preparation,
- gathering,
- dluzsze wyprawy,
- party support poza bezposrednia walka.

PvP uwagi:

- buffy z jedzenia powinny byc slabsze lub wolniejsze niz Alchemia,
- jedzenie nie powinno byc spamowalnym healem w walce,
- najlepiej dziala jako preparation buff przed aktywnoscia.

## Progression And Limits

Profesje powinny miec tierowy model progresji.

### Tier 1-2: Basic

Cel:

- tutorial profesji,
- proste receptury,
- brak twardych lockow,
- nauka materialow i rzemioslo stations.

Zasady:

- kazdy moze rozwijac wiele profesji,
- receptury tanie i czytelne,
- brak mocnego combat-power.

### Tier 3: Skilled

Cel:

- pierwsze realne wybory,
- mocniejsze receptury,
- wymagania materialowe,
- pierwsze questy profesji.

Zasady:

- nadal mozna rozwijac kilka profesji,
- receptury zaczynaja wymagac regionow, dungeonow albo reputacji,
- przedmioty zaczynaja miec znaczenie stylow grye, ale nie endgame.

### Tier 4: Specialist

Cel:

- wejscie w specjalizacje,
- wyzsze koszty,
- wybrane receptury style gry-defining.

Zasady:

- gracz nie powinien latwo pchac wszystkich profesji wysoko,
- specjalizacje powinny wymagac decyzji,
- rzemioslo combat-power zaczyna miec wyrazne ograniczenia.

### Master

Cel:

- topowe receptury przed endgame,
- mocna tozsamosc craftera,
- realna wartosc rynkowa.

Rekomendacja limitu:

- maksymalnie 2 profesje rzemiosloowe na poziomie Master na postac,
- gathering moze byc mniej restrykcyjny,
- utility profesje moga miec lzejsze ograniczenia, jesli nie daja duzej sily PvP.

### Grandmaster

Cel:

- endgame identity profesji,
- najrzadsze receptury,
- wysoka wartosc ekonomiczna,
- duze powiazanie z dungeonami, bossami, reputacja i marketem.

Rekomendacja limitu:

- 1 glowna specjalizacja Grandmaster na postac,
- alternatywnie 1 per kategoria, jesli testy pokaza ze jeden globalny limit jest
  zbyt ostry,
- respec mozliwy, ale kosztowny i z cooldownem.

## Leveling By Use

Profesja rosnie przez dzialania, nie przez samo wydawanie punktow.

Przyklady:

- Gornictwo rosnie przez wydobywanie rud i krysztalow.
- Zielarstwo rosnie przez zbieranie roslin i reagentow.
- Lowiectwo rosnie przez oprawianie bestii i potworow.
- Kowalstwo rosnie przez tworzenie i ulepszanie metalowego ekwipunku.
- Krawiectwo rosnie przez tworzenie zbroi lekkich, szat i materialow cloth.
- Jubilerstwo rosnie przez obrobke kamieni, socketow i akcesoriow.
- Alchemia rosnie przez tworzenie potek, trucizn i antidotow.
- Zaklinanie rosnie przez tworzenie run, enchantow i socket effects.
- Inzynieria rosnie przez tworzenie pulapek, bomb i dungeon tools.
- Gotowanie rosnie przez przygotowywanie jedzenia i buffow wyprawowych.

Anti-abuse:

- craftowanie najtanszego itemu w nieskonczonosc powinno miec diminishing returns,
- wyzsze poziomy wymagaja receptur z odpowiedniego tieru,
- XP profesji powinno premiowac pierwsze rzemiosloi, trudniejsze receptury i
  materialy z realnego contentu,
- nie powinno byc oplacalne makro-farmienie jednej taniej akcji.

## Salvaging

Salvaging nie jest osobna profesja. To wspolny system odzysku materialow i item
sink.

Zasady:

- kazdy gracz moze rozbijac podstawowe przedmioty,
- powiazana profesja daje lepszy odzysk,
- wyzsza profesja moze odzyskac rzadsze komponenty,
- salvaging zawsze oddaje mniej wartosci niz koszt stworzenia itemu,
- salvaging usuwa przedmioty z rynku i stabilizuje ekonomie.

Powiazania:

- Kowalstwo lepiej odzyskuje metal, weapon parts i armor plates.
- Krawiectwo lepiej odzyskuje tkaniny, skory i ward cloth.
- Jubilerstwo lepiej odzyskuje kamienie, socket materials i fragmenty
  akcesoriow.
- Zaklinanie lepiej odzyskuje runy, essence i magic dust.
- Alchemia lepiej odzyskuje skladniki z potek, trucizn i reagentow.
- Inzynieria lepiej odzyskuje mechaniczne czesci, zapalniki i trap parts.

## Stolica Wyspy Integration

Stolica Wyspy powinna byc glownym hubem profesji.

Minimalne miejsca:

- rzemioslo stations dla kazdej profesji,
- trenerzy profesji,
- tablica zlecen rzemiosla,
- market / auction house,
- stash i material storage,
- repair NPC,
- odzysk station,
- ulepszania station,
- vendorzy podstawowych reagentow,
- quest NPC profesji,
- wejscia lub portale do regionow i dungeonow z materialami.

Miasto nie powinno byc tematycznie ograniczone do jednej profesji. Kowal jest
tylko jednym z wielu NPC/systemow. Hub ma obslugiwac pelny loop MMO.

## Economy Integration

Profesje powinny tworzyc rynek, a nie niszczyc go samowystarczalnoscia.

Zasady:

- materialy maja jasne zrodla,
- mocne receptury wymagaja materialow z wielu aktywnosci,
- boss/dungeon materials moga byc ograniczone lub przypisane do postaci,
- market tax i rzemioslo costs usuwaja walute,
- odzysk usuwa przedmioty,
- rare recipes moga byc target farmione,
- rzemioslo nie moze calkowicie ominac bossow, dungeonow, PvP ani reputacji.

Trade rules:

- podstawowe materialy zwykle tradeable,
- bardzo mocne materialy moga byc przypisane do postaci albo partially przypisane do postaci,
- crafted ekwipunek moze byc tradeable przed equipem,
- wybrane high-tier rzemiosloi moga wymagac crafter identity albo station
  unlock.

## PvP Rules

Profesje moga dawac przewage przygotowania, ale nie moga wygrywac walki same.

Wymagaja sanity checkow:

- healing potions,
- resistance flasks,
- poison consumables,
- bombs,
- traps,
- movement gadgets,
- anti-heal,
- cooldown reduction enchanty,
- burst damage runy,
- stealth/detection ekwipunek,
- cleanse tools.

Zasady:

- consumable powinny miec cooldowny i limity stackowania,
- utility powinno miec counterplay,
- najlepsze PvP przedmioty profesji powinny kosztowac materialy z ryzykownego
  contentu,
- ekwipunek advantage ma istniec, ale soft capy musza chronic przed one-shot meta,
- profesje nie moga zastapic skilla gracza w dynamicznej walce.

## Respec Professions

Respec profesji ma naprawiac bledy i pozwalac zmienic kierunek, ale nie moze
pozwalac codziennie omijac limitow rynku.

Zasady:

- niskie tiery mozna resetowac latwo,
- Master/Grandmaster respec powinien kosztowac walute, materialy albo token,
- respec powinien miec cooldown,
- gracz nie powinien tracic calej historii progresji brutalnie bez ostrzezenia,
- mozna rozwazyc system refundu czesci XP albo zachowania odblokowanych
  receptur jako inactive knowledge.

## Test Cases

Professions v0.0.1 powinny przejsc ponizsze scenariusze:

- Nowy gracz moze sprobowac kilku profesji bez popsucia postaci.
- Gracz leveluje profesje przez realne uzywanie, nie przez pasywne menu.
- Crafter potrzebuje materialow z contentu, a nie tylko vendorow.
- Rynek ma sens, bo jedna postac nie wymaksuje wszystkiego do endgame.
- Gathering nadal jest oplacalny dla casuala.
- Alchemia nie tworzy nieskonczonego sustainu w PvP.
- Inzynieria nie tworzy spamowalnego CC albo bomb meta.
- Zaklinanie i Jubilerstwo nie robia czystych best-in-slot bez tradeoffu.
- Salvaging usuwa przedmioty z rynku i nie drukuje materialow.
- Respec profesji pomaga naprawic blad, ale nie pozwala codziennie swapowac
  pod market.
- Stolica Wyspy pelni role centralnego huba profesji bez zawlaszczenia motywu
  przez jedna profesje.

## Powiazania z fundamentami

Ten dokument musi pozostac spojny z:

- docs/combat-foundation-v0.0.1.md.
- docs/damage-defense-foundation-v0.0.1.md.
- docs/skill-ability-system-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.
- docs/economy-crafting-loot-foundation-v0.0.1.md.
- docs/progression-respec-foundation-v0.0.1.md.
- docs/world-content-loop-foundation-v0.0.1.md.
- docs/player-journey-milestone-roadmap-v0.0.1.md.
- docs/stolica-wyspy-hub-foundation-v0.0.1.md.
- docs/model-lupu-z-lochow-foundation-v0.0.1.md.
- docs/loot-reward-table-001-foundation-v0.0.1.md.
- docs/pet-companion-minion-system-foundation-v0.0.1.md.

W szczegolnosci:

- damage types zostaja: Physical, Fire, Cold, Lightning, Chaos, Poison.
- staty zostaja: Strength, Dexterity, Intelligence, Faith, Vitality.
- profesje wspieraja style gry przez przedmioty, materialy i utility, nie przez
  zastapienie klas.
- pierwsze materialy profesji z `loch_001` sa opisane w loot-reward-table-001.
- Lowiectwo, Zaklinanie i Alchemia moga wspierac pet/companion/minion systems,
  ale nie moga zabrac identity Wladcy Bestii ani Warlocka.
- PvP musi miec counterplay dla kazdego mocnego rzemiosla.
- endgame rzemioslo musi byc powiazany z dungeonami, bossami, reputacja,
  ekonomia i ryzykownym contentem.
