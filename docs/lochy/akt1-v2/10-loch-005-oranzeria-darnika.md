# 0. Oranżeria Darnika — karta szczegółowa Aktu 1
- **Poziom:** 5 (wejście od 4). **Normalny solo:** 15–19 min.
- **Fantazja:** wchodzę do pracowni ogrodnika i podczas walki przeszczepiam żywe narzędzia przeciw temu, kto je wyhodował.
- **Emocja:** ciekawość → niepokój, gdy eksperyment odpowiada → twórcza kontrola.
- **Motyw:** roślina posadzona w jawnym gnieździe natychmiast zmienia pole walki; wroga parę spina jasny zrost, który rozcina dowolne trafienie.
- **Prawo Aktu:** identyczny pąk tylko po poległym; każde trafienie go usuwa. Pozostawiony 5 s (Ł 8/T 4) dojrzewa w jedyny skutek: kapsułę sadzonki jednego z dwóch typów wylosowanych na wejściu; ostatnią sekundę pęcznieje i świeci.
- Kapsuła leży do końca bieżącego starcia; wejście w nią dodaje ładunek do licznika sakwy w HUD, bez przedmiotu w ręce. Sylwetka dojrzałej kapsuły różni się od wspólnego pąka; w jednym starciu może dojrzeć najwyżej 6 pąków.
- Pąk gracza → Cierń/Płatek ochrony, po osobnym teście śmierci; do tego czasu plan B: bez pąka gracza. Limit 12 pąków na instancję, nowy wypiera najstarszy (1 s szarzenia + dźwięk).

## 1. Kishōtenketsu motywu
- **Wstęp (Próg/Kopuła):** za szkłem Dzbanecznik i Doniczkowiec spięte świecącą nitką osłaniają się nawzajem. Z bezpiecznej wnęki dowolne trafienie statycznego węzła przecina nić i znika ich osłona.
- **Rozwinięcie (Paprociarnia/Storczykarnia):** pierwsze gniazdo świeci tym samym znakiem co kapsuła; jedna sadzonka wzięta po krótkim starciu przerywa czytelny atak wroga. Dwa aktywne typy sadzonek zmieniają tę decyzję co przebieg.
- **Wybór (hub):** gracz zdobywa rdzeń Kotłowni i jeden dowolny z Paprociarni, Storczykarni, Rozsadnika; może zostawić gniazdo puste lub posadzić kapsułę, a przeciwnik widocznie próbuje je odbić.
- **Zwrot (Kotłownia):** Starszy Ogrodnik przełącza neutralne gniazdo na wrogie i spina je z Pnączakiem; trzeba przeciąć zrost, zanim można znów wykorzystać gniazdo. To odwraca związek „gniazdo = moja pomoc”.
- **Egzamin (Szczepiarka):** boss rośnie od posadzonych gniazd, lecz linie da się przeciąć *w trakcie walki*, a dwie rośliny można odwrócić przeciw jej konkretnym atakom. Żadne gniazdo nie jest kluczem do drzwi ani quizem.
- Nauka bez tekstu: biały znak oznacza narzędzie gracza, zielony przejęte przez Ilmę; biały znak kapsuły pasuje do wejściowego pola gniazda. Statyczny węzeł przy stopach jednego z pary pokazuje zrost; po trafieniu osłona pęka. Gracz sadzi przez wejście na pole r=1,5, nie przez kliknięcie.

## 2. Historia środowiskiem
- Kopuła była pracownią Ilwena Darnika; po Głodnym Roku szukał plonów, które wyżywią wszystkich, a uczennica Ilma Szczepiarka nadal karmi jego próbki, choć nie odróżnia już rośliny od osoby.
- Przy wejściu cztery ręcznie podpisane stoły z sadzonkami; w skrzydłach ślady napraw i szkice korzeni prowadzące do stołów, nie „szalony naukowiec” zapisany w ścianie tekstu.
- **Samodzielne odkrycie:** jedna otwarta strona dziennika przy Kotłowni: „Zaszczepię ziemię jeszcze raz. Nikt więcej nie będzie głodny. — I. Darnik”. Wystarcza bez wcześniejszych lochów.
- Ocalała pomocnica Łada (jeden NPC, stojąca poza walką): „Ilma przestała odcinać chore szczepy. Darnik kazał ratować każdy plon.”
- Po bossie Łada: „To była jego pracownia. Pokażę w Stolicy, które sadzonki wciąż można bezpiecznie hodować.”
- Kronika Mapy zapisuje odkrycie pod 005 niezależnie od kolejności; Łada jest lokalnym świadkiem, nie źródłem przewagi bojowej.

## 3. Mapa ASCII i topologia
- Nowy, ciasny hub 36×38 pod kopułą, cztery skrzydła po 20–34 bl. Każdy ma osobną funkcję: źródło kapsuł, gniazdo, zagrożenie, skrót. Cel widoczny od startu: poruszająca się postać Szczepiarki za środkowym szkłem na piętrze.
```text
                              N ↑     [skala: 1 znak ≈ 4 bl.]
                [STORCZYKARNIA 24×28]──────[ROZSADNIK 24×26]
                       │     gniazdo S       │   kapsuły/sekret
                       │                     │
 [PAPROCIARNIA 26×32]──[KOPUŁA 36×38 / ognisko]──[BOSS Ø30, za szkłem]
       gniazdo P        │    ↘ pętla ↑                 │
                       │       ↓                      │
                [KOTŁOWNIA 22×30]────────────[galeria 20×24]
                  zrost/Łada                 skrót do ogniska
```
- Próg 20×22 prowadzi do Kopuły; brama wymaga rdzenia Kotłowni i jednego rdzenia z Paprociarni, Storczykarni albo Rozsadnika. Dodatkowe skrzydła dają narzędzia i sekrety. Dwa okna bramy pokazują osobno rdzeń Kotłowni i wybrany drugi rdzeń; ich wzory są widoczne w salach.
- Pętla: Paprociarnia → górny pomost Kopuły → Storczykarnia; Kotłownia → galeria → ognisko. Dwa krótkie skróty otwierają się od środka po starciu.
- Pionowość: posadzka y=0, pomost serwisowy y=+5, cienisty dół Kotłowni y=−3; wszystkie cele mają rampę/schody i drogę Chowańca, bez skakania po modelach.
- Nie ma pustego szklarnianego placu: pod kopułą gniazda w zasięgu 5–7 bl. od osłon, donice dzielą pole na trzy kąty, ruch między nimi jest decyzją.

## 4. Beat chart
| Sala | Rola | Min | Skład i powód | Start starcia | Tempo | Poza walką |
|---|---|---:|---|---|---|---|
| Próg 20×22 | czytelność | 0–1 | pojedynczy Pnączak za szybą pokazuje skok | widok, bez zagrożenia | niskie | dziennik roślin, weenie |
| Kopuła 36×38 | wstęp | 1–3 | para Dzbanecznik + Doniczkowiec spięta nitką: osłona i przecięcie | gracz widzi nić z góry | wysokie | ognisko, 4 ramiona |
| Paprociarnia 26×32 | wybrane skrzydło lub dodatkowe | 3–6 albo +1,5 | Pnączak + Ogrodnik: ruch z góry kontra gniazdo | roślina daje cień 1 s | średnie | pierwsza kapsuła, rdzeń |
| Storczykarnia 24×28 | wybrane skrzydło lub dodatkowe | 3–6 albo +1,5 | Dzbanecznik + Cierniorost: nić i klin odcinający gniazdo | gracz przygotowuje gniazdo | wysokie | rdzeń, boczny most |
| Rozsadnik 24×26 | wybrane skrzydło lub dodatkowe | 3–6 albo +1 | 1 Ogrodnik pilnuje kapsuł, nie fala | gracz zaskakuje przy stole | niskie | rdzeń, 2 ładunki, sekret |
| Kotłownia 22×30 | obowiązkowy zwrot | 6–9 | Ogrodnik Starszy + Pnączak zajmują gniazdo | widok zajęcia za szybą | wysokie | rdzeń Kotłowni, dziennik, Łada |
| Galeria 20×24 | oddech/WOW | 9–10 | bez wrogów | — | niskie | rozszczepienie światła na pękniętym dachu, skrót |
| Arena Ø30 | egzamin | 10–14 | Szczepiarka + maks. 2 przyczepione rośliny, bez fal | wejście po przygotowaniu | wysokie | gniazda i zrosty; boss ~3 min |
| Kopuła po walce | rozładowanie | 14–16 | bez wrogów | — | niskie | bezpieczne próbki, łup |
- Jedno z trzech skrzydeł daje drugi wymagany rdzeń; dwa dodatkowe skrzydła i sekrety zajmują najwyżej 4 min: pełna trasa ≤20. Jeden typ uczy, dwa są normą, trzy ciała naraz tylko w bossie.

## 5. Bestiariusz i liczby względem Gaj v8
- L2 dla poziomu 5: HP i surowe obrażenia mobów = Gaj v8 ×1,48; cel 8–15 s solo, zapowiedź ≥0,5 s, cios w zwarciu r≥3,5 bl. Formuła obrony wymaga poligonu.
| Mob | Rola; vanilla → model kandydat | HP/cios | Zapowiedź | Atak karzący | Kontra; nieuważny / uważny |
|---|---|---|---|---|---|
| Ogrodnik Bractwa | jedyny humanoid trash; husk z sekatorem → model własny | 47 / 9 | `item.axe.scrape` 0,8 s, trzy ruchy nożyc | trzeci cios r=3,5, zamknięty kierunek | blok/ruch na bok; 9–18 / 0–4 HP |
| Dzbanecznik | przyciąganie; shulker bez pocisku → własny | 59 / 10 | `block.bubble_column.bubble_pop` 1,1 s, kielich się otwiera | lej w oznaczonym stożku | obejść kielich; 10–20 / 0–5 HP |
| Pnączak | zasadzka; cave_spider bez jadu → model własny | 38 / 9 | `block.vine.place` 0,8 s, cień na podłodze | skok z sufitu w zamknięty punkt | wyjść z cienia/blok; 9–18 / 0–4 HP |
| Doniczkowiec | osłona; iron_golem przeskalowany → model własny | 65 / 10 | `block.decorated_pot.break` 1,0 s, obrys r=3,5 | okrężne uderzenie gliną | odsunąć się/uderzyć z boku; 10–20 / 0–5 HP |
| Cierniorost | kontrola pola; armor_stand z blokiem liści → model własny | 38 / 7,5 | `block.azalea_leaves.break` 0,9 s, klin cierni | pas kolców przecina dojście do gniazda | zmienić pas/ciąć zrost; 7–15 / 0–3 HP |
- Zrost łączy najwyżej 2 ciała; węzeł HP1 o hitboxie 2×2 stoi w zapisanym punkcie przy stopach jednego z nich, nie teleportuje za mobem. Dowolne trafienie węzła rozcina nić; po cięciu obie osłony znikają na 5 s.
- Zrost jest mechaniką *walki*, nie wspólną pulą HP ani obowiązkową zagadką; brak trafienia przedłuża starcie o ~5 s i zwiększa karę, ale moby nadal można pokonać.

## 6. Elita i boss
- **Starszy Ogrodnik — funkcja:** test przejęcia gniazda pod presją.
- **Haczyk:** przejmuje jedno gniazdo, a przecięcie nici do Pnączaka wyłącza jego cierniową gardę na 4 s.
- **Historia/wygląd:** uczeń Ilmy, który nosi jedną przerośniętą rękę do wiązania szczepów; zielona ręka jest największą bryłą, biały węzeł nici najjaśniejszym słabym punktem. Vanilla husk → model własny.
- **Parametry:** 225 HP (25% z 900), cios 12 surowych; zapowiedź przejęcia `block.beacon.activate` 1,2 s, szeroki zamach `item.axe.scrape` 0,9 s, odsłona 2 s po zerwaniu statycznego węzła. Nie przyzywa dodatkowych pracowników.
- **Szczepiarka — funkcja:** egzamin z wyboru sadzonki, cięcia zrostu i obsługi gniazda bez pauzowania walki.
- **Haczyk:** wchłania to, co gracz posadził w dwóch gniazdach; kiedy nitkę przetnie dowolne trafienie, roślina na 4 s działa przeciw jej następnemu gestowi.
- **Historia:** Ilma chciała uratować dzieło mistrza i zszywała wszystkie rośliny w jeden organizm; każda zdolność to narzędzie uprawy doprowadzone do przesady, nie zestaw czarów znikąd.
- **Wygląd:** dominanta sylwetki to wielkie asymetryczne nożyce z łodyg; cienkie ciało z jedną ręką w rękawie szklarniowym. Węzeł na mostku jest najjaśniejszy i odsłania się po cięciu. Vanilla witch bez mikstur → model własny; istniejące `lrd_witch` tylko kandydat proporcji, nie gotowy boss.
- **8 taktów emocji:** ciekawość → respekt → odczytanie narzędzi → napięcie → zdrada gniazda → opanowanie → rozcięcie → ulga. Ilma klęka po porażce, Łada zabiera ją z areny.
- **Fazy:** 100–65% Pielęgnacja (uczy gestów); 65–30% Szczepienie (pochłania po jednym aktywnym gnieździe); <30% Zrośnięcie (częstszy Szew, najwyżej jedno przejęte gniazdo naraz; zapowiedź nie krótsza niż 0,7 s).
- **HP startowe:** 900 solo Normalny (L2: 5 efektywnego DPS ×180 s); miękka garda 30% redukcji działa tylko podczas aktywnego Szwu, bez stałej nietykalności. Przecięcie węzła usuwa nić na 4 s i daje 2 s odsłony; dowolne źródło obrażeń wywołuje to samo.
| Zdolność / sadzonka | Zapowiedź, czas | Cel zamknięty? | Kara surowa | Kontra i odsłona |
|---|---|---|---:|---|
| Cięcie sekatorem | `item.axe.scrape`, 0,8 s; srebrny łuk | kierunek | 9 r=3,5 | wyjść za plecy/blok; 1,2 s powrotu |
| Szew między gniazdami | `block.beacon.ambient`, 1,2 s, jasna nić | linia gniazdo→boss | 7 + garda | trafić statyczny węzeł (każde źródło); 2 s węzeł jasny |
| Pnącze (jedna z 4 sadzonek) | `block.vine.place`, 1,0 s | pas od gniazda | 7 i rozdzielenie pola | posadzone przez gracza tworzy rampę/osłonę w walce; odcięte zwiąże bossowi nogę 2 s |
| Purchawa | `entity.puffer_fish.blow_up`, 0,9 s | koło r=3 | 6 + odrzut | własna Purchawa przerywa Szew; odcięta eksploduje pod bossem, 2 s odsłony |
| Ciernisko | `block.azalea_leaves.break`, 1,1 s | klin od gniazda | 7 | własne stawia kłującą ścianę 5 s; odcięte rani bossowi bark |
| Rosiczka | `block.slime_block.place`, 1,0 s | krąg r=2,5 | 6 + chwyt 2 s | własna przechwytuje Cięcie sekatorem i blokuje nożyce na 3 s; odcięta łapie samą Ilmę |
| Przejęcie gniazda | `block.beacon.deactivate`, 1,3 s, kolor biały→zielony | konkretne gniazdo | kolejny atak +2 | cios w statyczny węzeł lub własna sadzonka w drugim gnieździe; 2 s powrotu |
- **Arena Ø30:** dwa gniazda przy przeciwległych donicach, cztery niskie osłony, środkowy pasek światła; w fazie 2 jedno szkło pęka i odsłania boczną ścieżkę, w fazie 3 opada rama, ale pozostają dwa dojścia do każdego gniazda.
- **Anty-kitowanie:** po >7 bl. Szew przecina środkową linię areny i boss przechodzi po szynie ogrodniczej na drugi koniec; punkt jest pokazany 1,2 s wcześniej. Nie teleportuje na gracza i nie śledzi go po sygnale.
- **Sadzonki:** aktywne dokładnie 2 z 4, pokazane przy wejściu i w Rozsadniku. Kapsuła po wejściu w nią trafia do licznika sakwy HUD; przy każdym gnieździe są dwa oznaczone pola r=1,5, po jednym dla typu. Wejście z ładunkiem sadzi właściwy typ automatycznie i zużywa 1 ładunek. Każdy typ ma co najmniej 2 kapsuły na obowiązkowej trasie (Kopuła/Kotłownia) i odrasta z pąków; boss przejmuje najwyżej jedno gniazdo naraz.
| R2 solo bez specjalizacji | Starszy Ogrodnik | Szczepiarka |
|---|---|---|
| Wojownik | LPM w statyczny węzeł; blok szerokiego zamachu | dowolne trafienie węzła, wejście na pole gniazda, blok cięcia |
| Ninja | trafienie węzła po cieniu Pnączaka; ruch w bok | przecina węzeł z obu ścieżek, odsłonę bije serią, sadzi przez wejście |
| Mag | podstawowe zaklęcie w węzeł; zejście z klina | zaklęcie/pocisk przecina węzeł; do sadzenia tylko wchodzi na pole, bez PPM ani zmiany slotu |
| Akolita | własny atak/przywołaniec/Chowaniec przecina węzeł | każde źródło tnie węzeł; przywołańce depczą pąki, gracz sadzi wejściem na pole |
- To kontrakty wejścia do prób, nie wynik testu; żaden archetyp nie potrzebuje specjalizacji ani trafienia w okno liczone po pingu.

## 7. WOW
- **Minuta 9–10:** po Kotłowni gracz wychodzi na galerię +5 pod kopułą 40 bl.; nad nim rozlega się trzask i pęka pas dachu wzdłuż gałęzi drzewa. Światło dzieli się na cztery smugi w zraszanej mgle, a kilka odłamków spada obok bez obrażeń.
- Kadr prowadzi wzrok od pękniętej architektury i światła do przygotowanych gniazd i Ilmy z nożycami; WOW to zdarzenie w obrębie kopuły, bez olbrzyma oglądanego z progu.
- Pieczona bryła drzewa i szkła, dwa gotowe stany dachu, odłamki jako krótkie cząsteczki i dźwięk; nieruchome display tylko w głębi. Bez ruchomych tysięcy encji.

## 8. Paleta
- Dominanty: white_stained_glass, glass_pane, calcite, quartz_pillar, moss_block, big_dripleaf, azalea_leaves, copper_grate.
- Przedpołudnie po deszczu, złamane białe światło i szmaragd liści; kapsuły i pola gracza świecą biało, przejęte przez Ilmę zielono. Nić ma biały węzeł przed przejęciem i zielony po nim.
- Dźwięk: krople w Kotłowni, powolne skrzypienie ram, pojedynczy trzask szkła przy cięciu; bez muzyki zagłuszającej sygnał ataku.

## 9. Sekrety i wybór
| Sekret | Prawdziwa wskazówka | Skutek |
|---|---|---|
| Dach Darnika | pnącze na ścianie widać z galerii i Paprociarni; schody serwisowe są zawsze dostępne | notatka o pierwszej próbie, materiał |
| Ziemia spod szkła | brudne ślady wiodą do luźnej donicy | rzadki substrat profesji |
| Cztery podpisy | tabliczki wskazują tylko kolejność podlewania czterech próbek | ukryta fotografia Bractwa, kosmetyczny tytuł |
| Oddech Kotła | para dmucha w jedną kratę co 5 s | zestaw sadzonek do nagrody, nie do progresu |
- **Wybór:** Kotłownia jest obowiązkowa; drugi rdzeń można zdobyć w dowolnym z trzech pozostałych skrzydeł. Gracz wybiera typ w polu gniazda albo zostawia je puste; sekwencja tabliczek jest tylko sekretem.

## 10. Powtarzalność i ocena
- Instancja losuje 2 z 4 sadzonek przy wejściu; oba typy mają kapsuły w Kopule i Kotłowni na obowiązkowej trasie, a wybrane skrzydło daje dodatkowy ładunek swojego typu. Para zmienia kontrowanie Szwu i klina na bossie, nie kolor modeli.
- Losuje się również zajęcie północnego lub południowego gniazda przez Starszego; obie wersje są czytelne przez jasną nić już z progu.
- S: bez śmierci, przecięte ≥3 zrosty podczas ataków, użyte obie sadzonki w walce; A: bez śmierci; B: jedna śmierć; C: więcej. Sekret tabliczek nie podnosi mocy.

## 11. Nagroda
- Normalny pierwsze przejście: 600 EXP na postać (L3: 240 moby/elita, 240 boss, 120 sekrety); powtórka 250. Łatwy ×0,7, Trudny ×1,3; wybór skrzydeł nie zmienia bazowej puli.
- Rodzina **Narzędzia Szczepiarni**: Sekator Łady, Rękaw Zraszacza, Obręcz Szklanika — nazwy robocze, trzy różne style statystyk po tabeli klas.
- Materiały profesji: Szmaragdowy Substrat, Żywa Nić, Oszronione Szkło; tylko-tu Żywa Nić (rzadki surowiec przeszczepu) i fotografia pracowni do Kroniki.
- Sekrety dają materiały i kosmetyczny tytuł, nie obowiązkową przewagę bojową; EXP i nagroda bossa dla każdego obecnego gracza, sadzonki w trakcie walki są wspólne dla drużyny.
- Mapa Granicy dopisuje wątek „nikt więcej nie będzie głodny” i lokalnie pokazuje Ładę jako świadkinię; nie otwiera lochu 006 ponad wymóg poziomu.

## 12. Trudności
- Łatwy: pąk 8 s; rośliny gracza trwają 7 s; nić ma hitbox 2,5×2,5; po przejęciu gniazda boss czeka 2 s przed następnym ruchem.
- Normalny: liczby tabel, 5 s pąka, 5 s osłony Cierniska, 4 s odwrócenia zdolności po cięciu.
- Trudny: pąk 4 s; boss może przeszyć drugie gniazdo po 6 s, ale nigdy oba w tej samej 1,2 s zapowiedzi; czytelność i szerokość przejść stała.

## 13. Wykonanie i plany B
- **Trudność 1 — kapsuła/gniazdo:** wejście w kapsułę zwiększa licznik typu w HUD MythicHUD; wejście na oznaczone pole gniazda r=1,5 uruchamia MD `TriggerDistance` tylko przy dodatnim liczniku i atomowo zużywa ładunek. Plan B: dwa jawne pola naciskowe na każde gniazdo z tym samym licznikiem; żadnego KeyItem, PPM ani zmiany slotu.
- **Trudność 2 — dynamiczna arena:** BlockEditor lub `place template` ustawia 4 wycinki gniazd, bez zmiany podłogi pod graczem; plan B: dwa wypieczone warianty areny dla rodzin sadzonek (kontrola i osłona), wybierane na wejściu, 2 typy sadzonek nadal rzeczywiście działają w walce.
- **Trudność 3 — statyczny zrost:** jawny węzeł HP1 z hitboxem 2×2 powstaje w zapisanym punkcie przy stopach jednego moba; display nici może wskazać parę, lecz nie podąża co tick. `~onDamaged` rozłącza oba ciała; plan B: sam węzeł bez display. Testować dowolny atak, zaklęcie, pocisk, Chowańca i przywołańca.
- Pąk `~onDeath` → osobna encja HP1 z własnym `delay`; z funkcji MD bez `delay`, `TriggerDistance` po pojawieniu gracza, `summon level=1`. Populacja `Despawn: persistent`, bez `randomstroll`; warunki na cel w `TargetConditions`, własne mechaniki synchroniczne.
- `place template` czyta z `generated/<ns>/structures/`, stawia róg szablonu, chunki trzeba załadować; ID struktury wersjonować. Nexo: ikony kapsuł ≤256 px, `item_model` w `nexo:`; klient musi pokazać prawidłowy glif.
- Szacunek w szczycie: boss 1 + rośliny 2 + nici/hitboxy 2 + pąki do 6 + display 6 + kontroler 2 = 19 encji, limit 24. ModelEngine 4 jest opcją po akceptacji brył, vanilla jest pełnym planem B.

## 14. Samokontrola
- §4 briefu: każda sala ma funkcję i zmianę decyzji; nie ma obowiązkowego quizu; nie ma fali „boss + 3 paczki”; sekrety mają trop; losowanie 2/4 zmienia kontrę w walce.
- R1: pąk tylko po poległym, stały czas, każde trafienie usuwa, jeden skutek, kolejka wypiera najstarszy; R2: węzeł i odsłona przyjmują każde źródło, sadzenie jest wejściem; R3: Kotłownia obowiązkowa, zrost i sadzonki działają w walce, tabliczki są sekretem.
- Samoocena 0–10: fun 8; motyw/zwrot 9; czytelność 8; topologia 8; WOW 9; historia 8; powtarzalność 9; nagroda 7; wykonalność 7; różnorodność 9.
- **Ryzyko do bramki:** działanie nici i czterech sposobów trafienia wymaga poligonu; warianty areny zachowają taktykę, ale nie pełną liczbę wizualnych kombinacji. Nie przypisywać im zaliczonego testu bez wyniku.

## 15. Odpowiedź na recenzję (etap 5)
| Poprawka | Co zmieniono (sekcja) / odrzucono + dowód |
|---|---|
| C 005.1 | Kotłownia i jej zwrot dają obowiązkowy rdzeń; drugi pochodzi z jednego z trzech skrzydeł (§1, §3–4, §9). |
| C 005.2 | Kapsuła wpada do licznika HUD po wejściu; wejście na oznaczone pole gniazda automatycznie sadzi typ, bez PPM/KeyItem (§0–1, §6, §13). |
| C 005.3 | Kapsuła trwa do końca starcia; jeden statyczny węzeł przy stopach zamiast śledzącej nici; biały to gracz, zielony Ilma (§0–1, §5–6, §8, §13). |
| C 005.4, P7 i L1–L4 | Ilma 900 HP, Starszy 225 HP, moby ×1,48; 600 EXP pierwsze/250 powtórka; boss ~3 min, pełna trasa ≤20 (§4–6, §11). |
| C przekrojowe P6, L6 | Pęknięcie dachu i rozszczepienie światła tworzą zdarzenie w kopule; typ WOW pozostaje architekturą i światłem (§7). |
| R1/R2, L5 | Kapsuła jest jednym skutkiem pąka po poległym; stały czas, trafienie usuwa, kolejka wypiera najstarszy; tabela czterech klas (§0, §6, §14). |
| Odrzucone | Brak; poprawki obowiązkowe przyjęto. |
| Recenzja 2 C — uwaga do §6 | W tabeli bossa trafia się statyczny węzeł, nie wizualną nić; oba opisy kontr ujednolicono z §5 i §13 (§6). |
