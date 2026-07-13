# Model lupu z lochow fundament v0.0.1

## Cel dokumentu

Ten dokument definiuje, co wypada w lochach i po co gracz ma je powtarzac.
Loch nie moze dawac samych materialow, bo wtedy walka z potworami i bossami
traci emocje. Loch nie moze tez dawac samych gotowych przedmiotow, bo wtedy
profesje, rzemioslo, odzysk, handel i ulepszanie traca sens.

Poprawny model to mieszanka:

- ekwipunku wypadajacego z przeciwnikow,
- materialow profesyjnych,
- baz przedmiotow pod rzemioslo i ulepszanie,
- zetonow,
- fragmentow receptur,
- materialow z bossow,
- odzysku ze slabych przedmiotow.

Dokument jest projektowy. Nie definiuje finalnych szans na wypadniecie, puli
afiksow, statystyk przedmiotow, cen, konfiguracji pluginow ani pelnych tabel
kazdego lochu.

## Slownik obowiazkowy

W dokumentach projektowych uzywamy polskiego jezyka. Angielskie nazwy zostaja
tylko jako techniczne identyfikatory plikow, komend, pluginow albo stale ID typu
`loch_001`.

Zasady nazewnictwa:

- `loot` piszemy jako `lup`.
- `reward` piszemy jako `nagroda`.
- `gear` i `equipment` piszemy jako `ekwipunek`.
- `equipment drops` piszemy opisowo: `ekwipunek wypadajacy z przeciwnikow`.
- `boss rewards` piszemy jako `nagrody za pokonanie bossa`.
- `boss material` piszemy jako `material z bossa`.
- `crafting` piszemy jako `rzemioslo` albo `wytwarzanie`.
- `crafting base` piszemy jako `baza przedmiotu` albo `baza pod rzemioslo`.
- `salvage` piszemy jako `odzysk`.
- `token` piszemy jako `zeton`.
- `pity system` piszemy jako `zabezpieczenie przed pechem`.
- `objective` piszemy jako `cel`.
- `quest` piszemy jako `zlecenie`.
- `mob` piszemy jako `potwor` albo `przeciwnik`.
- `elite mob` piszemy jako `elitarny przeciwnik`.
- `drop rate` piszemy jako `szansa na wypadniecie`.
- `rare chance` piszemy jako `szansa na rzadki przedmiot`.
- `target farming` piszemy jako `celowe farmienie`.
- `build` piszemy jako `styl gry` albo `konfiguracja postaci`.

Nie uzywamy kalk typu `lupy ekwipunku` albo `nagrody bossow`.
Poprawnie: `ekwipunek wypadajacy z przeciwnikow`, `nagrody za pokonanie bossa`,
`lup z bossa`, `material z bossa`.

## Warstwy lupu z lochu

### Ekwipunek

Lochy musza dawac ekwipunek:

- bronie,
- zbroje,
- akcesoria,
- tarcze,
- katalizatory,
- przedmioty do drugiej reki,
- bazy przedmiotow pod ulepszanie.

Ekwipunek ma czasem byc uzywalny od razu. Gracz po zabiciu bossa albo elity
powinien miec szanse na przedmiot, ktory faktycznie chce zalozyc, sprzedac,
ulepszyc albo zachowac jako dobra baze.

### Materialy profesyjne

Materialy profesyjne sa paliwem ekonomii:

- ziola,
- grzyby,
- rudy,
- skory,
- kosci,
- futra,
- krysztaly,
- tkaniny,
- esencje,
- czesci potworow,
- mechaniczne komponenty,
- fragmenty runiczne.

Material nie jest nagroda sam w sobie, jesli nie ma zastosowania. Kazdy wazny
material musi miec jasne zrodlo i pozniejsze uzycie w rzemiosle, ulepszaniu,
recepturach, handlu albo odzysku.

### Bazy przedmiotow

Baza przedmiotu to drop, ktory moze nie byc idealny od razu, ale ma wartosc,
bo nadaje sie do dalszego rozwoju.

Przyklady wartosciowej bazy:

- dobry typ broni,
- dobra zbroja pod konkretny styl gry,
- miejsce na rune,
- sensowny wbudowany stat,
- dobry poziom przedmiotu,
- rzadki typ akcesorium,
- baza pasujaca do receptury.

Najlepszy przedmiot czesto powinien wynikac z polaczenia: dobra baza z lochu,
materialy profesyjne, ulepszanie, zeton albo material z bossa.

### Zetony i zabezpieczenie przed pechem

Zetony chronia gracza przed skrajnym pechem. Gracz, ktory powtarza loch i nie
dostaje dobrego przedmiotu, nadal robi postep.

Zasady:

- zeton nie kupuje natychmiast najlepszego przedmiotu,
- zeton moze kupic material, baze, fragment receptury albo element systemu
  ulepszania,
- najcenniejsze zetony moga byc przypisane do postaci albo ograniczone handlem,
- zeton ma pomagac, a nie usuwac sens walki z bossami.

### Fragmenty receptur

Fragment receptury to sposob na dlugoterminowa progresje rzemiosla.

Zasady:

- fragment receptury moze wypadac z bossow, elit, celow pobocznych albo skrzyn
  za ukonczenie lochu,
- receptura nie powinna calkowicie omijac lochow i bossow,
- mocne receptury powinny wymagac kilku typow zasobow,
- fragment receptury jest nagroda ekonomiczna, nie prosty wzrost obrazen.

### Odzysk

Odzysk sprawia, ze slaby ekwipunek nadal ma wartosc.

Zasady:

- slabe przedmioty mozna rozebrac na materialy,
- odzysk oddaje mniej wartosci niz koszt wytworzenia albo ulepszenia,
- odzysk nie zastepuje celowego farmienia,
- odzysk pomaga usuwac nadmiar przedmiotow z rynku.

## Zrodla lupu

### Zwykli przeciwnicy

Zwykli przeciwnicy daja:

- zloto,
- smieciowy lup,
- podstawowe materialy,
- niska szanse na zwykly albo magiczny ekwipunek,
- czasem czesc potwora powiazana z jego typem.

Rola: utrzymuja tempo runu i daja male, stale nagrody.

### Silniejsi przeciwnicy

Silniejsi przeciwnicy daja:

- lepsze materialy,
- wieksza szanse na magiczny ekwipunek,
- czasem dobra baze przedmiotu,
- czasem fragment zetonu albo material powiazany z motywem lochu.

Rola: sa krokiem miedzy zwyklym potworem a elita.

### Elitarni przeciwnicy

Elitarni przeciwnicy daja:

- paczke materialow,
- wieksza szanse na rzadki ekwipunek,
- szanse na dobra baze przedmiotu,
- fragment zetonu,
- przedmiot albo material powiazany z motywem lochu.

Rola: tworza moment napiecia przed bossem i nagradzaja lepsza gre.

### Cele poboczne

Cele poboczne daja:

- skrzynki materialow,
- fragmenty receptur,
- zasoby profesyjne,
- dodatkowy zeton,
- nagrody uzytkowe.

Rola: nagradzaja eksploracje i wykonanie mechaniki lochu, a nie samo zabicie
wszystkiego po drodze.

### Boss

Boss daje:

- gwarantowana podstawowa nagrode,
- szanse na rzadki, epicki albo unikatowy ekwipunek,
- material z bossa,
- zeton,
- fragment receptury,
- dobra baze przedmiotu.

Boss nie daje gwarantowanego najlepszego przedmiotu w grze. Boss ma byc waznym
zrodlem lupu, ale nie jedyna droga rozwoju calego stylu gry.

## Trudnosc a lup

`Latwy`:

- mniejsza wartosc farmienia,
- mniej materialow,
- nizsza szansa na rzadki przedmiot,
- nadal realna szansa na podstawowy ekwipunek i postep.

`Normalny / Klasyczny`:

- punkt odniesienia dla balansu lupu,
- standardowa ilosc materialow,
- standardowa szansa na ekwipunek,
- domyslna wartosc farmienia.

`Trudny`:

- wiecej materialow,
- lepsza szansa na rzadki przedmiot,
- wiecej fragmentow zetonu,
- szansa na dodatkowy fragment receptury albo lepsza baze.

`Bardzo Trudny`:

- najwyzsza wartosc farmienia danego lochu,
- lepsza szansa na epicki albo unikatowy przedmiot,
- lepsze paczki materialow,
- wieksza szansa na material z bossa,
- nadal bez gwarancji najlepszego przedmiotu.

Wyzsza trudnosc zwieksza jakosc, ilosc i szanse, ale nie zmienia tozsamosci
lupu. Loch bagienny nie staje sie nagle zrodlem fortecznych tarcz tylko dlatego,
ze jest na wyzszej trudnosci.

## Relacja dropow i rzemiosla

Rzemioslo nie zastepuje dropow. Dropy nie zastepuja rzemiosla.

Zasady:

- loch daje przedmioty, ktore mozna zalozyc od razu,
- loch daje bazy, ktore warto ulepszac,
- loch daje materialy do profesji,
- rzemioslo pozwala kierunkowac, ulepszac i stabilizowac progres,
- najlepszy progres powinien czesto wymagac kilku systemow naraz.

Przyklad poprawnej petli:

- gracz znajduje dobra baze miecza,
- rozbiera slabe przedmioty przez odzysk,
- zdobywa material z elity,
- zabija bossa dla zetonu i materialu z bossa,
- uzywa rzemiosla do ulepszenia miecza,
- nadal musi uwazac na limity PvP i sensowny counterplay.

## Zastosowanie w pierwszym akcie

`loch_001-003`:

- zwykly i magiczny ekwipunek,
- pierwsze rzadkie bazy,
- proste akcesoria,
- podstawowe materialy,
- pierwsze zetony,
- proste fragmenty receptur.

`loch_004-006`:

- wiecej rzadkiego ekwipunku,
- lepsze bazy,
- pierwsze sensowne miejsca na runy,
- mocniejsze materialy profesyjne,
- wieksza rola elitarnych przeciwnikow.

`loch_007-009`:

- wieksza szansa na rzadkie i epickie przedmioty,
- materialy z bossow,
- fragmenty receptur,
- lepsze bazy pod konkretne style gry,
- mocniejsze zabezpieczenie przed pechem przez zetony.

`loch_010`:

- boss konczacy pierwszy akt,
- wieksza szansa na epicki albo unikatowy przedmiot,
- zeton aktu,
- fragmenty receptur,
- dobre bazy przedmiotow,
- brak gwarantowanego najlepszego przedmiotu.

## Zasady PvP

Lup z lochow nie moze psuc PvP.

Zakazane skutki:

- unikat dajacy one-shot bez counterplayu,
- nieskonczone leczenie,
- stala kontrola tlumu,
- spam mikstur bez kosztu i limitu,
- obowiazkowa farma jednego bossa,
- przedmiot, ktory usuwa sens calej klasy albo stylu gry.

Mocny przedmiot moze istniec, ale musi miec koszt, wymogi, ograniczenia albo
jasny counterplay.

## Integracja

Ten dokument musi pozostac spojny z:

- docs/itemization-foundation-v0.0.1.md.
- docs/ekwipunek-pierwszego-aktu-foundation-v0.0.1.md.
- docs/economy-crafting-loot-foundation-v0.0.1.md.
- docs/professions-foundation-v0.0.1.md.
- docs/loot-reward-table-001-foundation-v0.0.1.md.
- docs/dungeon-ladder-002-010-foundation-v0.0.1.md.
- docs/dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md.

W szczegolnosci:

- itemizacja okresla typy przedmiotow, rzadkosc, afiksy i ograniczenia PvP,
- ekonomia okresla handel, sinki, zetony i celowe farmienie,
- profesje okreslaja, do czego sluza materialy,
- tabela lupu `loch_001` jest pierwszym przykladem konkretnej tabeli,
- ekwipunek pierwszego aktu okresla wspolna pule przedmiotow i akcenty lupu,
- drabinka `loch_002-010` okresla klimaty i zrodla lupu pierwszego aktu.

## Testy

Model lupu z lochow powinien przejsc ponizsze scenariusze:

- Loch daje ekwipunek, materialy, zeton, fragment receptury i nagrode za bossa.
- Slaby przedmiot nadal ma wartosc przez odzysk.
- Material ma jasne zrodlo i zastosowanie.
- Boss jest ekscytujacy, ale nie gwarantuje najlepszego przedmiotu.
- Zeton pomaga przy pechu, ale nie usuwa sensu farmienia.
- Rzemioslo uzywa dropow i materialow, ale nie omija lochow.
- Wyzsza trudnosc zwieksza wartosc lupu bez zmiany tozsamosci lochu.
- PvP nie psuje sie przez jeden unikat, miksture, leczenie albo kontrole tlumu.

## Zalozenia

- Lochy sa glownym zrodlem progresji ekwipunku PvE.
- Profesje i rzemioslo wspieraja oraz kierunkuja progresje, ale nie zastepuja
  dropow.
- Materialy z lochow sa czescia ekonomii, nie cala ekonomia.
- Finalne szanse, statystyki, ceny, receptury i konfiguracje pluginow zostaja
  na pozniejszy etap balansu.
