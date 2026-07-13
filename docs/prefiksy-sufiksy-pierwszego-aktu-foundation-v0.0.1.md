# Prefiksy i sufiksy pierwszego aktu fundament v0.0.1

## Cel dokumentu

Ten dokument definiuje system prefiksow i sufiksow dla ekwipunku pierwszego
aktu `loch_001-010`. Nie traktujemy "afiksu" jako jednego worka. Afiks to
ogolna nazwa modyfikatora przedmiotu, a kazdy modyfikator powinien byc
prefiksem albo sufiksem.

Podzial na prefiksy i sufiksy ma dawac sensowna konkurencje miejsc na
przedmiocie. Przedmiot nie moze miec wszystkiego naraz, wiec dobry ekwipunek
wynika z polaczenia: baza przedmiotu, rzadkosc, pasujace prefiksy, pasujace
sufiksy, ewentualne rzemioslo i ulepszanie.

Dokument jest projektowy. Nie definiuje finalnych wartosci liczbowych,
konfiguracji pluginow ani pelnej puli endgame.

## Definicje

Afiks:

- dowolny modyfikator przedmiotu,
- moze byc prefiksem albo sufiksem,
- nie powinien istniec jako trzecia, nieokreslona kategoria na zwyklych,
  magicznych i rzadkich przedmiotach.

Prefiks:

- glowna moc przedmiotu,
- najczesciej obrazenia, defensywa, zasoby albo baza mocy,
- odpowiada na pytanie: "co ten przedmiot robi?".

Sufiks:

- dopasowanie i jakosc dzialania przedmiotu,
- najczesciej odporności, atrybuty, tempo, kryt, regeneracja albo uzytecznosc,
- odpowiada na pytanie: "jak dobrze ten przedmiot pasuje do postaci?".

Unikat:

- moze lamac zwykly podzial prefiks/sufiks,
- ma wlasny efekt specjalny,
- musi miec osobna kontrole PvP.

## Limity rzadkosci

`Zwykly`:

- brak prefiksow i sufiksow,
- tylko bazowe statystyki przedmiotu.

`Magiczny`:

- maksymalnie 1 prefiks,
- maksymalnie 1 sufiks,
- ma uczyc gracza, ze przedmiot ma modyfikatory.

`Rzadki`:

- maksymalnie 3 prefiksy,
- maksymalnie 3 sufiksy,
- glowny trzon progresji ekwipunku.

`Epicki`:

- moze miec mocniejszy wariant zwyklego prefiksu albo sufiksu,
- moze miec jeden efekt warunkowy,
- nadal nie powinien byc zawsze lepszy od dobrze dobranego rzadkiego przedmiotu.

`Unikatowy`:

- ma efekt specjalny,
- moze miec nietypowe zasady,
- nie musi trzymac limitu 3 prefiksy / 3 sufiksy,
- nie moze byc zawsze najlepszym wyborem.

## Prefiksy pierwszego aktu

Prefiksy sa glowna moca przedmiotu. W pierwszym akcie powinny byc czytelne i
bezpieczne dla PvP.

### Obrazenia

- obrazenia fizyczne,
- obrazenia od ognia,
- obrazenia od zimna,
- obrazenia od blyskawic,
- obrazenia chaosu,
- obrazenia trucizny,
- obrazenia wrecz,
- obrazenia dystansowe,
- obrazenia zaklec,
- obrazenia minionow,
- obrazenia pulapek,
- obrazenia w czasie.

Zasada: przedmiot nie powinien miec zbyt wielu zrodel mnozenia obrazen naraz w
pierwszym akcie.

### Defensywa

- zdrowie,
- pancerz,
- unik,
- ward,
- sila bloku,
- garda,
- poise.

Zasada: defensywa moze byc mocna, ale nie moze tworzyc niesmiertelnego tanka
bez kosztu w mobilnosci, zasobach albo obrazeniach.

### Zasoby

- mana,
- stamina,
- maksymalny zasob pod konkretny styl gry,
- sila leczenia.

Zasada: zasoby maja wspierac rotacje, nie usuwac kosztow umiejetnosci.

### Rzemioslo i baza przedmiotu

- jakosc bazy przedmiotu,
- dodatkowe miejsce na rune,
- lepszy wynik odzysku,
- wieksza wartosc bazy pod ulepszanie.

Zasada: prefiksy rzemieslnicze nie moga generowac ekonomii bez limitu.

## Sufiksy pierwszego aktu

Sufiksy dopasowuja przedmiot do postaci. W pierwszym akcie powinny glownie
uzupelniac braki i dawac kierunek stylu gry.

### Atrybuty

- sila,
- zrecznosc,
- inteligencja,
- wiara,
- zywotnosc.

Zasada: atrybuty pomagaja spelnic wymagania i wzmacniaja kierunek postaci, ale
nie powinny same zastapic punktow statystyk.

### Odporności

- odpornosc na ogien,
- odpornosc na zimno,
- odpornosc na blyskawice,
- odpornosc na chaos,
- odpornosc na trucizne.

Zasada: odporności sa wazne, ale pierwszy akt nie powinien dawac pelnej
odporności na typ obrazen.

### Tempo

- szybkosc ataku,
- szybkosc rzucania,
- szybkosc ruchu,
- regeneracja many,
- regeneracja staminy.

Zasada: tempo jest mocne w PvP, wiec stale bonusy musza byc ostrozne.

### Trafienia krytyczne

- szansa na trafienie krytyczne,
- obrazenia krytyczne.

Zasada: kryt ma wspierac burst, ale nie moze sam tworzyc one-shota.

### Kontrola i odpornosc na presje

- tenacity,
- poise,
- krotsze spowolnienia,
- slabszy efekt klatwy,
- mocniejsze oczyszczenie.

Zasada: te sufiksy zmniejszaja presje, ale nie usuwaja counterplayu przeciwnika.

### Uzytecznosc

- wykrywanie skradania,
- cichsze skradanie,
- nizszy koszt naprawy,
- lepszy odzysk materialow,
- mniejsza kara po przerwaniu kanalowania.

Zasada: uzytecznosc ma byc dobra, ale nie moze zastepowac profesji ani klas.

## Ograniczenia slotow

### Bron

Moze losowac:

- prefiksy obrazen,
- prefiksy obrazen pod tagi,
- sufiksy szybkosci ataku,
- sufiksy trafien krytycznych,
- sufiksy atrybutow.

Nie powinna losowac:

- duzych odporności,
- mocnych bonusow profesyjnych,
- pelnej defensywy.

### Tarcza

Moze losowac:

- prefiksy bloku,
- prefiksy gardy,
- prefiksy pancerza,
- prefiksy zdrowia,
- sufiksy odporności,
- sufiksy poise,
- sufiksy tenacity.

Nie powinna losowac:

- wysokich obrazen,
- mocnych bonusow krytycznych.

### Katalizator i relikwia

Moga losowac:

- prefiksy many,
- prefiksy wardu,
- prefiksy obrazen zaklec,
- prefiksy minionow,
- prefiksy leczenia,
- sufiksy atrybutow,
- sufiksy regeneracji,
- sufiksy odporności.

Nie powinny losowac:

- silnych bonusow do bloku,
- czysto wojskowych afiksow tarczy.

### Zbroja

Moze losowac:

- prefiksy zdrowia,
- prefiksy pancerza,
- prefiksy uniku,
- prefiksy wardu,
- sufiksy odporności,
- sufiksy atrybutow,
- sufiksy poise,
- sufiksy tenacity.

Nie powinna losowac:

- wysokich obrazen,
- mocnych efektow rzemieslniczych.

### Buty

Moga losowac:

- prefiksy uniku,
- prefiksy staminy,
- sufiksy szybkosci ruchu,
- sufiksy krotszego spowolnienia,
- sufiksy odporności,
- sufiksy regeneracji staminy.

Nie powinny losowac:

- duzych obrazen,
- mocnych bonusow do bossowych materialow.

### Rekawice

Moga losowac:

- prefiksy obrazen broni,
- prefiksy obrazen zaklec,
- sufiksy szybkosci ataku,
- sufiksy szybkosci rzucania,
- sufiksy trafien krytycznych,
- sufiksy atrybutow.

Nie powinny losowac:

- pelnej defensywy zbroi,
- mocnych odporności na wiele typow obrazen naraz.

### Bizuteria

Moze losowac:

- szeroka pule sufiksow,
- odporności,
- atrybuty,
- zasoby,
- trafienia krytyczne,
- regeneracje,
- wykrywanie,
- efekty uzytkowe.

Zasada: bizuteria jest elastyczna, ale nie powinna miec najlepszych wersji
wszystkich afiksow naraz.

### Pas

Moze losowac:

- zdrowie,
- stamina,
- odporności,
- nizszy koszt naprawy,
- lepszy odzysk,
- uzytecznosc mikstur,
- nośnosc albo ekonomiczne drobiazgi.

Zasada: pas wspiera przygotowanie i ekonomie, ale nie powinien byc najlepszym
slotem ofensywnym.

## Efekty warunkowe

Efekty warunkowe powinny byc rzadkie w pierwszym akcie.

Dopuszczalne kierunki:

- premia po udanym bloku,
- premia po parowaniu,
- premia po uniku,
- premia po przerwaniu kanalowania,
- premia po trafieniu w plecy,
- premia przeciw elitom,
- premia przy niskiej manie albo staminie.

Zasada: efekt warunkowy musi miec okno, limit albo czas odnowienia. Nie powinien
byc stale aktywny.

## Zakazane w pierwszym akcie

- Duze skracanie wszystkich czasow odnowienia.
- Pelna odpornosc na typ obrazen.
- Stale ogluszenie.
- Staly root.
- Stale silence.
- Leczenie bez limitu.
- Duza stala predkosc ruchu w PvP.
- Ignorowanie pancerza, uniku albo wardu bez kontry.
- Generowanie duzej ilosci materialow ekonomicznych.
- Afiksy robiace z jednego lochu obowiazkowe zrodlo mocy.
- Afiksy, ktore zastepuja cala klase albo profesje.

## Przyklady przedmiotow

Rzadki miecz:

- prefiks: obrazenia fizyczne,
- prefiks: obrazenia wrecz,
- prefiks: zdrowie,
- sufiks: szybkosc ataku,
- sufiks: sila,
- sufiks: odpornosc na ogien.

Rzadka szata:

- prefiks: ward,
- prefiks: mana,
- prefiks: obrazenia zaklec,
- sufiks: inteligencja,
- sufiks: szybkosc rzucania,
- sufiks: odpornosc na chaos.

Magiczne buty:

- prefiks: unik,
- sufiks: krotsze spowolnienia.

Epicka tarcza:

- prefiks: sila bloku,
- prefiks: zdrowie,
- sufiks: tenacity,
- efekt warunkowy: po udanym bloku krotko odzyskujesz troche staminy.

## Integracja

Ten dokument musi pozostac spojny z:

- docs/itemization-foundation-v0.0.1.md.
- docs/ekwipunek-pierwszego-aktu-foundation-v0.0.1.md.
- docs/model-lupu-z-lochow-foundation-v0.0.1.md.
- docs/damage-defense-foundation-v0.0.1.md.
- docs/skill-ability-system-v0.0.1.md.

## Testy

- Kazdy modyfikator jest prefiksem albo sufiksem.
- Prefiksy i sufiksy konkuruja o ograniczone miejsca.
- Magiczny, rzadki i epicki przedmiot maja jasne limity.
- Sloty nie losuja statow sprzecznych z ich rola.
- Mocne efekty warunkowe maja limit albo czas odnowienia.
- PvP nie psuje sie przez perma-CC, nieskonczone leczenie, stala predkosc albo
  brak kontry.

## Zalozenia

- Dokladne wartosci liczbowe zostaja na pozniejszy balans.
- Pierwszy akt uczy dzialania prefiksow i sufiksow.
- Zwykle, magiczne i rzadkie afiksy sa glownie pozytywne.
- Kompromisy zostaja glownie dla unikatow i bardzo mocnych efektow specjalnych.
