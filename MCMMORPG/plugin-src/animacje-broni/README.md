# AnimacjeBroni — feeling broni MMOItems (2026-10-01)

Vanilla 1.21.11 ma komponent przedmiotu `swing_animation` (`whack` = cios, `stab` = pchnięcie, `none`, plus długość
w tickach). Działa w 1. i 3. osobie, u wszystkich patrzących, bez modów. MMOItems 6.10 go nie ustawia, więc robi to
ten plugin, wg typu MMOItems przedmiotu. Drugi element to **model trzymania**: własny model w packu Nexo
(`bron:<skala>/<material>`), który dziedziczy vanillowy model materiału i zmienia tylko rozmiar w dłoni — rękojeść
zostaje w dłoni (przesunięcie liczone w `generuj.py`). Ikona w ekwipunku i przedmiot na ziemi są bez zmian.

Feeling broni opiera się na rodzaju obrażeń: **cięte** (ostrze tnie szeroko), **kłute** (grot przebija w linii),
**obuchowe** (tępa broń miażdży) i na wadze broni (długość zamachu, minimalne naładowanie ciosu, dźwięk).

| Broń (typ) | Obrażenia | Zamach (t) | Naładowanie | Zasięg | PPM | Trzymanie |
|---|---|---|---|---|---|---|
| Miecz (SWORD / LONG_SWORD) | cięte / kłute zależnie od ciosu | cięcie 6 / 7, pchnięcie przy ciężkim z przodu lub wykończeniu | — | 3 | garda −40% | vanilla |
| Klinga elfów (KATANA) | cięte | cios 5 | — | 3 | garda −30%, lżejsza | vanilla |
| Topór (AXE) | cięte | cios 8 | 60% | 3 | — | vanilla |
| Miecz dwuręczny (GREATSWORD) | cięte, ciężka | cios 10 | 80% | 3,3 | **garda oburącz** −55% (ostrze poziomo przed ciałem), wolna | ×1,30 |
| Broń drzewcowa (HALBERD) | **kłute** (było obuchowe) | pchnięcie 8 | 100% | **2–4,5**, przebija w linii | PPM = blok oburącz (garda −35%); sprint + PPM = szarża (vanilla, obrażenia przy zderzeniu zależne od prędkości); kontra po bloku jak dotąd: garda ≥0,5 s, puść PPM i w 0,5 s LPM = wypad (krok + pchnięcie przez linię 4,5 bl. za 200%, odnowienie 3 s tylko wypadu) | ×1,30 |
| Włócznia (SPEAR) | kłute | vanilla | vanilla | 2–4,5 | szarża (vanilla) | vanilla |
| Sztylet (DAGGER) | kłute | pchnięcie 4 | — | **2,6** | — | ×0,80 |
| Kosa (KOSA) | cięte, ciężka | cios 10 | 80% | **3,5** | — | ×1,40 |
| Sierp (SIERP) | cięte | cios 5 | — | 2,8 | — | vanilla |
| Buława (HAMMER) | obuchowe | cios 8 | 60% | 3 | — | vanilla |
| Młot dwuręczny (GREATHAMMER) | obuchowe, ciężka | cios 12 | **90%** | 3 | uniesienie i uderzenie obszarowe | ×1,30 |
| Kostur dwuręczny (GREATSTAFF) | obuchowe | cios 9 | 60% | 3,3 | — | ×1,40 |
| Różdżka / kostur (WAND / STAFF) | magia (LPM pocisk MMOItems) | pchnięcie 5 / 7 | — | 3 | — | ×0,90 / ×1,25 |
| Grimuar, totem, filakterium | — | pchnięcie 5 | — | 3 | — | vanilla |
| Tarcza (SHIELD) | obuchowe | cios 7 | — | 3 | blok (vanilla) | vanilla |
| Łańcuch (WHIP) | cięte | cios 7 | — | LPM bicz MMOItems | — | vanilla |

Garda oburącz (halabarda, miecz dwuręczny): PPM w pozie ładowania kuszy (obie ręce z przodu), a definicja modelu
`bron:s130g/<materiał>` podczas PPM (`using_item`) obraca broń poziomo w poprzek ciała, w 1. osobie w poprzek ekranu
(kąty z `qa/pozy.sh`: 3. osoba [90,0,-45], 1. osoba [-30,0,-45]). Trzymanie oburącz BEZ wciśniętego PPM jest w vanilli
niemożliwe (poza oburęczną ma tylko naładowana kusza, przypięta do samego przedmiotu kuszy).
Halabarda podczas sprintu używa zwykłego modelu `bron:s130/<materiał>` i vanillowej animacji szarży włócznią;
nie blokuje ciosów ani nie przyjmuje pozy ładowania kuszy. Po zwolnieniu sprintu PPM znów uruchamia gardę.
Zmiana sprintu podczas trzymania PPM podmienia przedmiot i może przerwać jego używanie po stronie klienta;
szarżę zaczyna się, biegnąc przed wciśnięciem PPM.
Zablokowany cios: iskry w miejscu styku + dźwięk bloku tarczy i metalu (tylko cios z przodu, w kącie gardy).

Efekt trafienia (plugin, każdy cios): cięte = łuk cięcia + świst; kłute = iskry krytyka w linii ciosu + stuk grotu;
obuchowe = pył spod celu + głuche uderzenie (ciężka broń: uderzenie buzdyganu w ziemię). Do tego efekty MMOItems
(rozprysk obrażeń co 3 s: cięte stożek 60°, kłute 30°, obuchowe wokół celu).

## Feeling jak w Skyrimie

Reakcje zwykłego ciosu działają przy naładowaniu co najmniej 90%. Źródłem wartości jest `TYPY` w `generuj.py`.

Szybko zadane ciosy budują rytm/kombinację, a cierpliwie wyczekany cios staje się ciężki. Ciężki cios wymaga
pełnego naładowania broni (`ceil(20 / attack_speed)` ticków) i jeszcze `ciezkie.czekanie` ticków bez zamachu
(domyślnie 10). Każdy zamach, także w powietrze, i każde trafienie zaczynają liczenie od nowa. Gotowość jest
sygnalizowana raz na ładowanie cichym dźwiękiem słyszanym tylko przez gracza i kilkoma iskrami przy dłoni.
Ciężki cios kosztuje dodatkowo 12% maksymalnej staminy MMOCore: sprint, W, A/D, S są sprawdzane w tej kolejności;
bez kierunku używany jest wariant przedni. Bez przypisanego wariantu albo przy zbyt małej staminie trafienie pozostaje zwykłe.
Ciężki cios przerywa kombinację. W konfiguracji `ciezkie_warianty` są mnożniki, siła odrzutu i kąt łuku; halabarda
i włócznia zachowują własne mechaniki bez ciężkich ciosów. Bez MMOCore dodatkowe koszty są zerowe (jedno ostrzeżenie
przy uruchomieniu); zwykłe koszty MMOItems nadal działają.

Kombinacja obejmuje trzy zwykłe trafienia bronią w walce wręcz (bez halabardy i włóczni). Pierwszy cios jest zwykły;
od drugiego każdy cios serii kosztuje dodatkowo 2% maksymalnej staminy. Kolejny cios musi mieć co najmniej 90% naładowania i trafić najpóźniej 8 ticków po pełnym
odnowieniu broni (`20 / attack_speed` ticków od poprzedniego trafienia). Trzeci cios zadaje ×1,4 obrażeń i efekt rodzaju
broni. Po drugim trafieniu SWORD / LONG_SWORD szykuje pchnięcie na wykończenie. Znacznik PDC z końcowym tickiem
chroni ten zamach przed okresowym `dopasuj()`, a po wykończeniu lub przerwaniu serii animacja wraca.
Pozostałe bronie zachowują swój zwykły zamach również przy wykończeniu.

## Miecz: cięcie i pchnięcie

SWORD i LONG_SWORD tną zwykłym ciosem (`whack`). Gotowy ciężki cios z trzymanym W lub bez kierunku
zmienia zamach w dłoni na pchnięcie (`stab`) jeszcze przed kliknięciem. Tak samo pchnięciem jest trzecie
trafienie kombinacji. Ciężki cios z A/D pozostaje bocznym cięciem, z S odpycha, a podczas biegu
jest cięciem z rozbiegu. Gracz i obserwatorzy widzą zamach przypisany do przedmiotu w chwili kliknięcia.
Pchnięcie przebija do dwóch kolejnych celów w linii za pierwszym (każdy za ×0,6 obrażeń), ignoruje
30% redukcji pancerza i ma smugę krytyka oraz stuk grotu. Cięcie zachowuje dotychczasowe efekty.
Parowanie i riposta działają jak wcześniej. Warianty pochodzą z `atak_pchniecie` w `TYPY`.

Młot dwuręczny ma osobny model `s130u` podczas `using_item`, wstępnie ustawiony pionowo nad głową. Przy naładowanym
zamachu pojawia się dwutickowa smuga widoczna w świecie; kształt i cząsteczki zależą od rodzaju broni.

| Broń | Rodzaj | Reakcja trafienia | PPM |
|---|---|---|---|
| Miecz, długi miecz, katana, miecz dwuręczny, miecz kłujący | cięte / kłute | Krytyki pozostają w MMOItems; parowanie otwiera ripostę x1,5 na 30 ticków | Garda: pełne parowanie przez pierwsze 6 ticków, potem zwykły blok |
| Topór, topór dwuręczny, kosa | cięte | Krwawienie: 25% obrażeń ciosu przez 3 s, odświeżane | — |
| Sierp | cięte | Krwawienie do 3 stosów po 25% | — |
| Buława, młot dwuręczny, kostur dwuręczny | obuchowe | Osłabienie redukcji pancerza o 30% | Młot dwuręczny: uniesienie, po 16 tickach uderzenie wokół gracza x1,6 w promieniu 3,5; odnowienie 120 ticków |
| Sztylet | kłute | Cios w plecy x2; ze skradania, gdy cel nie ściga gracza, x3 | — |

Parowanie i uderzenie młota zachwieją cel: spowolnienie na 20 ticków, animacja obrażeń i lekki odrzut.
Uderzenie młota wymaga pustej lewej ręki i braku umiejętności MMOItems na PPM. Krótsze przytrzymanie
oraz puszczenie PPM podczas odnowienia nie wywołują uderzenia.

**PPM (garda, szarża, uderzenie młota) tylko przy pustej lewej ręce i bez umiejętności MMOItems na PPM** — inaczej PPM należy do
zaklęcia/tarczy w lewej ręce albo do umiejętności; plugin zdejmuje i przywraca te komponenty sam (przegląd co 10 ticków).
Zmierzone na klonie 30.09 (`qa/mechanika.sh`): garda miecza 6 → 4,0 HP bez / 2,4 HP z gardą; halabarda nie trafia
celu na 1 bloku, trafia dwa w linii na 3 i 4 blokach; młot dwuręczny nie wyprowadza ciosu 0,6 s po poprzednim
(miecz tak); klient nie wysyła LPM, dopóki trzymany jest PPM, więc kontra to „puść prawy, kliknij lewy”; szarża halabardą trafiała w poprzednim układzie komponentów; garda halabardy 6 → 4,0 / 2,5 HP, miecza dwuręcznego 3,9 / 1,7 HP (4 powtórzenia); kontra (qa/mechanika.sh G): samo puszczenie PPM nic nie robi, blok+LPM = wypad, blok działa zaraz po wypadzie, dwie kontry w 2,9 s = jeden wypad; garda gaśnie z pochodnią w lewej ręce i wraca po jej zdjęciu. Nowy podział gardy i szarży wymaga ponownego sprawdzenia w grze.

Poza pluginem celowo: zamach i PPM **SPEAR** (materiały `*_SPEAR` mają vanillowe pchnięcie i szarżę; plugin daje im tylko efekt trafienia), **BOW / CROSSBOW /
GREATBOW / MUSKET** (animacje naciągania). Model trzymania dostaje tylko przedmiot z modelem domyślnym materiału —
model z Nexo/MMOItems (`model:`) zostaje. Materiały bez modelu „narzędzia” (END_ROD, książki, łańcuch) są pomijane.


## Jak działa
- `generuj.py` — źródło prawdy (tabela `TYPY`): pisze `wynik/config.yml` i `wynik/pack/assets/bron/` (materiały
  zbiera z `MMOItems/item/<typ>.yml` serwera, transformacje rąk z client.jar).
- Plugin poprawia przedmiot, gdy trafia do ręki (wejście, zmiana slotu, zamiana rąk, klik/przeciąganie, podniesienie)
  i przeglądem obu rąk co 10 ticków. `ItemBuildEvent` MMOItems 6.10 jest martwy (nikt go nie wywołuje).
- `/animacjebroni sprawdz <gracz>` — typ, zamach i model z głównej ręki, `zamachTeraz`, `nastepnyAtak` oraz `ciezkiGotowy` i `ciezkiZa` (ticki do gotowości, −1 bez wariantu); `/animacjebroni reload`.

## Zmiana / test / wdrożenie
    python3 generuj.py && cp wynik/config.yml src/main/resources/ && ./build.sh
    qa/klon.sh <katalog> --fresh        # klon world1 ab-test (+ klient: docker run ... mc-client-test qa/launch.sh)
    qa/scena.sh GREATSWORD HARDENED_STEEL_CLAYMORE   # dane komponentów + zrzuty 1. osoba / zamach / przód
    qa/mechanika.sh A|A2|B0|C|D|E|G|P|U|S|K|W|B|T|Z|N|C3|SW1|SW2|SW3   # pomiary mechanik, w tym miecza i kombinacji
    qa/efekty.sh SWORD PRZ_WOJ_MIECZ_1 miecz   # zrzuty trafienia i pozy PPM
    ./wdroz.sh                          # world1: kopia + cofnij.sh, tylko przy 0 graczy, RESTART

26.3: `swing_animation` zastąpią `attack_animation` + `interact_animation` — przy aktualizacji wersji zmienić plugin.
