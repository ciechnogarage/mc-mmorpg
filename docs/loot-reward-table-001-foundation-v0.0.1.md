# Tabela lupu 001 fundament v0.0.1

## Cel dokumentu

Ten dokument opisuje projektowa tabele lupu dla `loch_001` / `Level 1 Dungeon
Island`. Ma spiac pierwszy loch z ekwipunkiem, materialami, zleceniami,
profesjami, odzyskiem, rzemioslem, zetonami, nagroda za pokonanie bossa i
kontrola balansu PvP.

To nie jest finalna konfiguracja. Nie ustalamy jeszcze procentow, ilosci,
statystyk przedmiotow, cen, skladni pluginow ani pelnych receptur.

## Zasady

- Kazdy wazny material ma jasne zrodlo.
- Kazdy przedmiot zlecenia wskazuje konkretnego przeciwnika albo cel lochu.
- Zwykli przeciwnicy daja zloto, smieciowy lup, podstawowe materialy i mala
  szanse na zwykly albo magiczny ekwipunek.
- Silniejsi przeciwnicy daja lepsze materialy i wieksza szanse na dobra baze
  przedmiotu.
- Elita daje paczke materialow, szanse na rzadki ekwipunek i fragment zetonu.
- Boss daje podstawowa gwarantowana nagrode, material z bossa, zeton oraz
  szanse na rzadki przedmiot albo fragment receptury.
- Odzysk oddaje mniej wartosci niz rzemioslo zuzywa.
- Lup ma prowadzic gracza z powrotem do Stolicy Wyspy: sprzedaz, naprawa,
  odzysk, schowek, rzemioslo, ulepszanie.
- Wczesny lup nie moze tworzyc one-shot PvP, nieskonczonego leczenia, stalej
  kontroli tlumu ani obowiazkowego spamu mikstur.

## Zrodla lupu

| Zrodlo | Podstawowy lup | Rzadszy lup | Powiazanie | Uzycie | Ryzyko PvP |
| --- | --- | --- | --- | --- | --- |
| Slaby roj przeciwnikow | drobne zloto, smieciowy lup, male kosci | zwykla baza zbroi, drobny material do odzysku | `Oczysc Sciezke` | odzysk, Kowalstwo, Lowiectwo | niskie |
| Wilki i bestie | wilczy kiel, surowa skora | pazur, lepsza skora, zwykla baza lekkiej zbroi | `Kly z Lochu 001` | Lowiectwo, Krawiectwo, Alchemia | niskie |
| Korzenie i roslinne potwory | uszkodzony korzen, dzikie ziolo | naturalny fragment, material do oczyszczenia statusu | `Korzenie Straznika` | Zielarstwo, Alchemia, Zaklinanie | srednie, jesli statusy sa za mocne |
| Zaklinacz / kanalizujacy przeciwnik | slaba esencja, pekniety krysztal | slaby fragment runy, magiczny pyl | `Przerwany Rytual` | Zaklinanie, Jubilerstwo | srednie, jesli runy za szybko wzmacniaja PvP |
| Opancerzony przeciwnik | peknieta plytka, uszkodzona tarcza | baza tarczy albo pancerza | zlecenie poboczne | Kowalstwo, odzysk | srednie |
| Elitarny straznik bramy | paczka materialow, zloto | rzadki ekwipunek, fragment zetonu | cel przed bossem | wiele profesji | srednie |
| Boczne zasoby | dzikie ziola, pekniete krysztaly, naturalne fragmenty | mala skrzynka materialow | eksploracja | profesje i rzemioslo | niskie |
| Cel lochu | doswiadczenie, zloto, mala skrzynka materialow | fragment zetonu, fragment receptury | aktywuj / obron / przerwij | postep i rzemioslo | niskie |
| `GroveGuardian` / `Straznik Gaju` | zeton gaju, naturalne fragmenty | rdzen straznika, rzadki przedmiot, fragment receptury | glowny boss | material z bossa, ulepszanie, zabezpieczenie przed pechem | srednie; wymaga limitow |

## Katalog materialow

### wilczy kiel

Zrodlo: wilki i bestie w `loch_001`.

Uzycie:

- zlecenie `Kly z Lochu 001`,
- Lowiectwo,
- proste receptury alchemiczne,
- pozniejsze bronie lekkie albo ozdoby.

Zasada: nie moze stac sie drogim bottleneckiem rynku.

### surowa skora

Zrodlo: bestie i przeciwnicy zwierzecy.

Uzycie:

- Krawiectwo,
- lekka zbroja,
- torby albo prosta uzytkowa odziez,
- odzysk i rzemioslo.

Zasada: bezpieczny material wprowadzajacy gracza w ekwipunek lekki.

### mala kosc

Zrodlo: slabe potwory, bestie, smieciowy lup.

Uzycie:

- Lowiectwo,
- Alchemia,
- Inzynieria,
- proste akcesoria albo skladniki.

Zasada: material powszechny, bez mocnego wplywu na PvP.

### uszkodzony korzen

Zrodlo: roslinne potwory i korzenie w lochu.

Uzycie:

- Zielarstwo,
- Alchemia,
- Zaklinanie,
- zlecenie `Korzenie Straznika`.

Zasada: material organiczny, nie "trucizna jako nagroda".

### dzikie ziolo

Zrodlo: boczne zasoby i roslinne fragmenty mapy.

Uzycie:

- Zielarstwo,
- Alchemia,
- podstawowe mikstury przygotowawcze,
- proste receptury leczenia albo oczyszczenia.

Zasada: mikstury bojowe wymagaja limitow czasu odnowienia i stackowania.

### naturalny fragment

Zrodlo: roslinne potwory, skrzynki materialow, boss.

Uzycie:

- pierwsze ulepszanie,
- Krawiectwo,
- Kowalstwo,
- Zaklinanie.

Zasada: dobry pierwszy material do nauki ulepszania, nie glowny koszt calej gry.

### pekniety krysztal

Zrodlo: kanalizujacy przeciwnicy, boczne zasoby.

Uzycie:

- Jubilerstwo,
- Zaklinanie,
- podstawowe runy,
- proste akcesoria.

Zasada: wprowadza magiczne rzemioslo bez silnego skoku mocy.

### slaba esencja

Zrodlo: zaklinacze, kanalizujacy przeciwnicy, cele rytualne.

Uzycie:

- Zaklinanie,
- Runotworstwo,
- pierwsze efekty uzytkowe.

Zasada: nie moze szybko dawac mocnego skracania czasu odnowienia albo burstu.

### peknieta plytka

Zrodlo: opancerzeni przeciwnicy i odzysk.

Uzycie:

- Kowalstwo,
- naprawa pancerza,
- tarcze,
- defensywne bazy przedmiotow.

Zasada: wspiera obronny styl gry, ale nie tworzy niesmiertelnego tanka.

### slaby fragment runy

Zrodlo: zaklinacze, cele rytualne, rzadziej skrzynki.

Uzycie:

- Zaklinanie,
- Runotworstwo,
- proste modyfikatory uzytkowe.

Zasada: runy na tym etapie maja byc lekkim dodatkiem, nie fundamentem przewagi.

### zeton gaju

Zrodlo: `GroveGuardian`, cele lochu, rzadziej fragmenty z elit.

Uzycie:

- zabezpieczenie przed pechem,
- zakup materialu albo bazy,
- przyszly sprzedawca albo receptura.

Zasada: zeton nie kupuje natychmiast najlepszego przedmiotu.

### rdzen straznika

Zrodlo: `GroveGuardian`.

Uzycie:

- material z bossa,
- ulepszanie,
- receptury zwiazane z pierwszym aktem,
- rzadki skladnik do przedmiotow o naturalnym motywie.

Zasada: nie moze samodzielnie tworzyc dominujacego przedmiotu PvP.

## Ekwipunek z `loch_001`

`loch_001` moze dawac:

- zwykle miecze, luki, rozdzki, tarcze i lekkie pancerze,
- magiczne wersje prostych broni,
- pierwsze akcesoria z malymi statystykami,
- bazy przedmiotow pod odzysk albo ulepszanie,
- rzadki przedmiot z bossa, ale bez gwarancji najlepszego wyboru.

Zasady:

- zwykli przeciwnicy rzadko daja dobry ekwipunek,
- elita ma wyzsza szanse na dobra baze,
- boss ma najwieksza szanse na rzadki przedmiot,
- rzemioslo moze poprawic baze, ale nie omija zabijania bossa i elit.

## Skalowanie trudnosci

`Latwy`:

- mniej materialow,
- mniejsza szansa na rzadki przedmiot,
- nadal realny postep dla gracza uczacego sie mechanik.

`Normalny / Klasyczny`:

- bazowa wartosc lupu,
- domyslna szansa na ekwipunek i materialy,
- punkt odniesienia dla balansu.

`Trudny`:

- wiecej materialow,
- lepsza szansa na rzadki przedmiot,
- wiecej fragmentow zetonu,
- mozliwa dodatkowa skrzynka za cel poboczny.

`Bardzo Trudny`:

- najwyzsza wartosc farmienia `loch_001`,
- lepsza szansa na material z bossa,
- lepsza szansa na rzadki albo epicki przedmiot,
- nadal brak gwarantowanego najlepszego przedmiotu.

## Integracja

Ten dokument musi pozostac spojny z:

- docs/model-lupu-z-lochow-foundation-v0.0.1.md.
- docs/level-1-dungeon-island-foundation-v0.0.1.md.
- docs/mob-boss-encounter-001-foundation-v0.0.1.md.
- docs/quest-contract-objective-foundation-v0.0.1.md.
- docs/professions-foundation-v0.0.1.md.
- docs/economy-crafting-loot-foundation-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.

## Testy

- `wolf fang` / `wilczy kiel` ma jasne zrodlo.
- Kazdy early material ma przynajmniej jedno zastosowanie.
- Boss daje emocjonujacy lup, ale nie psuje PvP.
- Odzysk nadaje wartosc slabym przedmiotom.
- Zeton pomaga przy pechu, ale nie kupuje natychmiast najlepszego przedmiotu.
- Lup wspiera progres do poziomu 10 bez farmienia jednego potwora w kolko.

## Zalozenia

- `loch_001` jest pierwszym content source.
- `GroveGuardian` / `Straznik Gaju` jest pierwszym bossem.
- Szanse na wypadniecie i ilosci pozostaja placeholderami projektowymi.
- PvP sanity ma pierwszenstwo przed fantazja nagrod.
- Dokument jest design-only i nie zmienia konfiguracji serwera.
