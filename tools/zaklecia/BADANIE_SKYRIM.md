## Typy w Skyrimie

*Koszty poniżej są bazowe: przed obniżkami za umiejętność, perki i wyposażenie. Typ rzucania i sposób dostarczenia to dwie niezależne cechy zaklęcia.* [Creation Kit: Magic Effect](https://ck.uesp.net/wiki/Magic_Effect), [UESP: Magic Overview](https://en.uesp.net/wiki/Skyrim:Magic_Overview)

| Typ / dostarczenie | Zasada i przykłady |
|---|---|
| **Concentration** / Aimed, Self, Target Actor | Działa podczas trzymania, pobiera manę na sekundę. **Flames:** 14 many/s, 8 obrażeń/s; **Healing:** 12/s, 10 zdrowia/s; **Healing Hands:** 25/s, 10 zdrowia/s innemu celowi. [UESP: Healing](https://pt.uesp.net/wiki/Skyrim:Healing), [zestawienie zaklęć](https://www.carlsguides.com/skyrim/magic/) |
| **Fire and Forget** / Aimed | Ładowanie, wypuszczenie, jednorazowy koszt; efekt może potem trwać. **Firebolt:** 41 many, 25 obrażeń; **Fireball:** 133 many, 40 obrażeń w promieniu 15 stóp. Typowy czas przygotowania wynosi około **0,5 s**; nie jest to swobodne „dokarmianie” zaklęcia maną dla większej mocy. [Creation Kit: Magic Effect](https://ck.uesp.net/wiki/Magic_Effect), [UESP: Destruction](https://en.uesp.net/wiki/Skyrim:Destruction), [Fireball](https://skyrim.fandom.com/wiki/Fireball) |
| **Fire and Forget** / Target Location | Efekt trafia wskazaną powierzchnię lub miejsce. **Fire Rune:** 234 many, wybuch 50 obrażeń po zbliżeniu wroga; w podstawowym Skyrimie aktywna jest jedna runa naraz. **Conjure Flame Atronach:** 150 many, przywołanie na 60 s. [Fire Rune](https://skyrimcodex.com/spell/fire-rune), [UESP: Conjuration](https://pt.m.uesp.net/wiki/Skyrim:Conjuration) |
| **Fire and Forget** / Self | Natychmiastowy lub czasowy efekt po rzuceniu. **Oakflesh:** 103 many, +40 pancerza na 60 s; **Candlelight:** 21 many, światło na 60 s; **Transmute Mineral Ore:** 88 many, przemiana jednej rudy. **Flame Cloak:** 289 many, 8 obrażeń/s pobliskim wrogom przez 60 s. [UESP: Alteration](https://en.uesp.net/wiki/Skyrim:Alteration), [UESP: Destruction](https://en.uesp.net/wiki/Skyrim:Destruction), [zestawienie zaklęć](https://www.carlsguides.com/skyrim/magic/) |
| **Concentration** / Self, defensywne | **Lesser Ward:** 34 many/s, +40 pancerza i osłona o sile 40 przeciw magii. Osłona narasta po podniesieniu, słabnie od trafień i może pęknąć. [UESP: Restoration](https://en.uesp.net/wiki/Skyrim:Restoration), [Creation Kit: Magic Effect](https://ck.uesp.net/wiki/Magic_Effect) |
| **Fire and Forget** / Aimed, kontrola | **Fury:** 67 many, istoty do poziomu 6 atakują otoczenie przez 30 s. Poziom celu jest warunkiem skuteczności, nie skalą zadawanych obrażeń. [UESP: Illusion](https://en.uesp.net/wiki/Skyrim:Illusion), [zestawienie zaklęć](https://www.carlsguides.com/skyrim/magic/) |
| **Master / Ritual** / różne | Osobna kategoria zaawansowanych zaklęć, zwykle rytuał oburęczny trwający około **3 s**; nie jest zwykłym rzutem z perkiem Dual Casting. **Fire Storm:** 1426 many, wybuch wokół maga do 100 obrażeń, silniejszy blisko środka. [UESP: Spells](https://en.uesp.net/wiki/Skyrim:Spells), [Fire Storm](https://skyrim.fandom.com/wiki/Fire_Storm) |
| **Constant Effect** / zwykle Self | Efekt stale aktywny zdolności lub wyposażenia, bez przycisku rzucania. **Contact/Touch** w terminologii Creation Kit oznacza trafienie bronią; nie jest zwykłym sposobem celowania zaklęciem z dłoni. [Creation Kit: Magic Effect](https://ck.uesp.net/wiki/Magic_Effect) |

**Oburącz:** perk danej szkoły łączy dwa egzemplarze tego samego zaklęcia w jeden rzut: zwykle około **2,2× efektu za 2,8× kosztu** jednej ręki. Wzmocnienie dotyczy obrażeń, czasu działania lub pułapu poziomu celu zależnie od efektu. *Impact* pozwala wielu oburęcznym zaklęciom Destruction zachwiać przeciwnikiem. Przywołanie trwa dłużej, ale oburącz nie zwiększa limitu stworzeń: standardowo **1**, z *Twin Souls* **2**. Koszt bazowy mnoży się m.in. przez `1 − (poziom umiejętności / 400)^0,65`, redukcje perków i sprzętu oraz mnożnik oburęczny. Obecne serwerowe **2,5× many** jest świadomym odstępstwem od Skyrima. [UESP: Magic Overview](https://en.uesp.net/wiki/Skyrim:Magic_Overview), [UESP: Conjuration](https://pt.m.uesp.net/wiki/Skyrim:Conjuration)

**Przerwania i oprawa:** brak many blokuje przygotowanie albo kończy koncentrację; zachwianie może przerwać ładowanie lub rytuał. Skyrim rozdziela wygląd i dźwięk dłoni przy przygotowaniu, sygnał gotowości, wypuszczenie oraz trafienie; koncentracja ma dźwięk zapętlony. [Creation Kit: Magic Effect](https://ck.uesp.net/wiki/Magic_Effect), [UESP: Magic Overview](https://en.uesp.net/wiki/Skyrim:Magic_Overview)

## Mapowanie na nasze sterowanie

| Typ u nas | Wejście i oburącz | Odpowiedzialność i ograniczenie |
|---|---|---|
| `koncentracja` | PPM trzymany; LPM+PPM uruchamia to samo zaklęcie oburącz; sam LPM może nadal przełączać lewą. | Plugin mierzy czas, manę i cele; MythicMobs wykonuje efekt oraz dźwięki, Nexo/cząsteczki pokazują dłonie. Obecne próbki obrażeń co 0,5 s są przybliżeniem ciągłego działania. |
| `pocisk` | LPM-klik dla szybkiego rzutu lewą; PPM prawa może dawać krótki, stały czas przygotowania. LPM podczas PPM uzbraja rzut oburęczny, wypuszczenie PPM wystrzeliwuje. | Plugin rozstrzyga ręce, koszt i przerwania; MythicMobs prowadzi pocisk, kolizję i żywioł. Firebolt na sam LPM to **serwerowe uproszczenie**: w Skyrim Fire and Forget wymaga przygotowania. |
| `ladowany` | PPM trzymany, wystrzał po puszczeniu; LPM+PPM scala obie ręce. | Plugin egzekwuje minimalny czas i stan „gotowe”; MythicMobs pokazuje narastanie w dłoni, potem pocisk/wybuch. Trzymanie dłużej nie powinno samo zwiększać mocy, chyba że projekt świadomie doda taką zasadę. |
| `runa` | PPM przygotowuje, puszczenie umieszcza w punkcie wskazanym celownikiem; oburącz jedna mocniejsza runa. | Plugin wybiera bezpieczną powierzchnię, właściciela, limit i czas życia; MythicMobs obsługuje wyzwolenie i wybuch; BlockDisplay/Nexo rysują znak. LPM-klik może być wariantem szybkiego stawiania. |
| `plaszcz`, `na_siebie` | PPM–puszczenie dla czasowego buffa; LPM-klik jako szybki wariant. Oburącz wydłuża czas albo wzmacnia parametr wskazany w definicji. | Plugin trzyma czas, wyłączność i koszt; MythicMobs nakłada efekt; cząsteczki/ModelEngine pokazują aurę. Nie tworzyć drugiego płaszcza przez dwa równoległe rzuty. |
| `tarcza` | PPM trzymany; LPM+PPM podnosi mocniejszą osłonę. | Plugin musi przechwycić obrażenia i kierunek trafienia, prowadzić siłę osłony oraz manę; MythicMobs/Nexo rysują ward. Sama osłona wizualna z MythicMobs nie zapewni wiernego blokowania różnych źródeł obrażeń. |
| `przywolanie` | PPM–puszczenie w wybranym miejscu; oburącz wydłuża życie przywołania. | Plugin pilnuje limitu 1/2, właściciela i sprzątania; MythicMobs tworzy i steruje mobem, ModelEngine daje model. Target Location wymaga kontroli podłoża i przestrzeni. |
| `iluzja`, `leczenie_innych` | PPM–puszczenie dla Fury; PPM trzymany dla Healing Hands. | Plugin wybiera cel z linii wzroku, sprawdza poziom, sojusz i odporność; MythicMobs nakłada AI/leczenie. Target Actor można przybliżyć ray castem; bez pewnego celu rzut nie powinien pobierać pełnej many. |
| `rytual`, `staly_efekt` | Rytuał: PPM przez około 3 s, rezerwuje obie ręce, puszczenie po gotowości. Stały efekt: pasywny, bez LPM/PPM. | Plugin wymusza bezruch/przerwania; MythicMobs realizuje finał, efekty dłoni i dźwięki. Rytuał nie korzysta z mnożnika Dual Casting. Stały efekt należy do systemu pasywów/ekwipunku, nie do pętli rzucania. |

## Architektura

Obecnie [`Ksiega.java`](/home/dev/Desktop/Przemek/MC/.claude/worktrees/zaklecia/MCMMORPG/plugin-src/zaklecia/src/main/java/pl/mcmmorpg/zaklecia/Ksiega.java) przechowuje `mana-za-tik`, a [`Rzucanie.java`](/home/dev/Desktop/Przemek/MC/.claude/worktrees/zaklecia/MCMMORPG/plugin-src/zaklecia/src/main/java/pl/mcmmorpg/zaklecia/Rzucanie.java) prowadzi jeden automat strumienia. Proponuję rozszerzyć definicję o niezależne pola `typ`, `dostarczenie`, `reka`, `czas-ladowania-t`, `koszt-jednorazowy`, `koszt-na-sekunde`, `czas-trwania-t`, `zasieg`, `promien`, `limit-aktywnych`, `cel` oraz `oburacz: {koszt, parametr, mnoznik, wymaga-perka}`. Przykład: `fireball: {typ: ladowany, dostarczenie: aimed, czas-ladowania-t: 10, koszt-jednorazowy: 133, oburacz: {koszt: 2.8, parametr: obrazenia, mnoznik: 2.2}}`.

Plugin powinien mieć wspólny automat `gotowe → ladowanie/aktywne → wystrzal/stop → odnowienie`, z osobnym stanem każdej dłoni i transakcją many **przed** efektem. Dispatcher typu wybiera cel i wywołuje konfigurowalne skille MythicMobs: `przygotuj`, `gotowe`, `wypusc`, `tik`, `traf`, `pekla`, `zakoncz`, `brak_many`, `przerwano`; warianty `_P`, `_L`, `_Obie`. Dzięki temu po napisaniu obsługi **mechanik typów** nowe zaklęcia powstają z definicji i skilli, bez dopisywania Java dla każdego z nich. MythicMobs odpowiada za obrażenia/AI/FX, MythicLib/MMOCore za zasoby i statystyki, Nexo/ModelEngine/Display za wygląd; plugin zachowuje autorytet nad wejściem, kosztami, celami, limitami i cyklem życia.

## Pierwsza partia

| Kolejność | Zaklęcie | Zakres pierwszego wdrożenia |
|---|---|---|
| 1 | **Firebolt** | Szybki pocisk w pojedynczy cel: test kolizji, żywiołu, LPM oraz wariantu oburęcznego. |
| 2 | **Healing** | Koncentracja Self: wykorzystuje istniejącą pętlę PPM i testuje leczenie zamiast obrażeń. |
| 3 | **Fireball** | Przygotowanie PPM przez około 0,5 s, wystrzał po puszczeniu, obrażenia obszarowe. |
| 4 | **Lesser Ward** | Obrona trzymana PPM: test realnego przechwytywania magii, siły osłony i przerwania. |
| 5 | **Fire Rune** | Wskazana powierzchnia, jedna aktywna runa, wyzwolenie po wejściu przeciwnika. |

## Ryzyka

- **LPM nie ma sygnału puszczenia.** Nie da się wiernie ładować lewą dłonią ani kończyć jej koncentracji przez puszczenie LPM. Dla ładowania lewej potrzebny jest PPM jako klawisz prowadzący; przy zajętej prawej ręce można dodać jawny modyfikator, np. Shift+PPM, po sprawdzeniu kolizji ze skradaniem.
- **LPM pełni dziś funkcję przełącznika lewej.** Rzut po kliknięciu trzeba rozstrzygać według typu zaklęcia i krótkiego okna LPM+PPM; w przeciwnym razie szybki pocisk może przypadkowo włączyć strumień albo odwrotnie.
- **PPM przychodzi co 4 ticki.** Wypuszczenie i gotowość mają opóźnienie pomiarowe do około 0,2 s; komunikat „gotowe” i koszt należy liczyć po stronie serwera. Utrata celu, zmiana przedmiotu, brak many i stagger muszą kończyć stan bez podwójnego wystrzału.
- **Wardy, AI iluzji i przywołania wymagają kontroli pluginu.** Efekt wizualny nie dowodzi ochrony; poziom potwora MMOCore/MythicMobs trzeba zmapować na próg iluzji. Runy i moby muszą znikać po logout, śmierci, zmianie świata i restarcie.
- **Liczby Skyrima są punktem odniesienia, nie skalą serwera.** Koszty, obrażenia i sekundy trzeba przeliczyć na pulę many oraz tempo walki MC. Bezpośredni odczyt części stron UESP był blokowany przez 403, więc liczby sprawdziłem także w dostępnych zestawieniach; przed implementacją warto zatwierdzić jedną wersję danych gry.

## Źródła

[Creation Kit: Magic Effect — typy, dostarczenie, czasy, dźwięki](https://ck.uesp.net/wiki/Magic_Effect) · [UESP: Magic Overview — koszt i Dual Casting](https://en.uesp.net/wiki/Skyrim:Magic_Overview) · [UESP: Spells](https://en.uesp.net/wiki/Skyrim:Spells) · [UESP: Destruction](https://en.uesp.net/wiki/Skyrim:Destruction) · [UESP: Restoration](https://en.uesp.net/wiki/Skyrim:Restoration) · [UESP: Alteration](https://en.uesp.net/wiki/Skyrim:Alteration) · [UESP: Conjuration](https://pt.m.uesp.net/wiki/Skyrim:Conjuration) · [UESP: Illusion](https://en.uesp.net/wiki/Skyrim:Illusion) · [UESP: Healing](https://pt.uesp.net/wiki/Skyrim:Healing) · [zestawienie kosztów i efektów](https://www.carlsguides.com/skyrim/magic/).