# Recenzja krzyżowa — Projektant C (etap 4, 2026-10-03)

Zakres: 002, 005, 008 (X) i 010 (prowadzący) wg briefu §4 i syntezy v2 R1–R5, plus przegląd powtórek w 10 kartach.
Metoda: każdą salę przeszedłem w głowie dwa razy (Mag solo, Wojownik solo) i sprawdziłem wykonalność z pamięcią projektu
(mythicmobs-*, mythicdungeons-funkcje-bez-edytora, md-*, mm-warunki-inline). Zasada briefu: ocena <7 w jakimkolwiek
kryterium oznacza BLOKADĘ.

**Werdykty:**
- 002: BLOKUJĘ (zwrot jest opcjonalny, powtarzalność 6)
- 005: BLOKUJĘ (czytelność 6, wykonalność 6)
- 008: BLOKUJĘ (motyw bez zębów, łańcuch unieruchomień)
- 010: BLOKUJĘ (różnorodność 6: kopie wrogów i trzeci boss, który leczy się błędami gracza)

Każda blokada ma naprawę na jedną–dwie godziny pracy. Ramy i układów nie trzeba przepisywać.

## 0. Problemy przekrojowe (dotyczą także moich kart, nie tylko recenzowanych)
| # | Powtórka / niespójność | Dowód | Naprawa |
|---|---|---|---|
| P1 | **Dojrzały pąk to osłona ze zboża w 3 lochach** | 004 „Snop — kryjówka”, 008 „kłos… osłona przed liniowym ciosem”, 009 „nieruchomy Snop osłonowy” | 004 zostaje (motyw ukrycia). 008: pąk dojrzewa w **Wołka** (chrząszcz zbożowy HP 6, który biegnie do najbliższego worka i go przegryza; worek się rozsypuje, Ziarnojad leczy się na ziarnie). Dwuznaczne: przegryziony worek przy Matce też ją głodzi. 009: pąk dojrzewa w **Dzwonek** (kwiat dźwięczący na „4”; po zabiciu Bębna przywraca rytm w r8 — narzędzie na Ciszę i dla bossa F3). |
| P2 | **Boss leczy się z błędów gracza w 3 lochach** | 001 Pniaczki leczą Strażnika; 008 „leczy ją o 2 HP × stos”; 010 Szczepienie „obrażenia gracza leczą Darnika” i Sękacz | 001 i 008 zostają (to ich motywy). 010: Szczepienie **przekierowuje** 50% twoich obrażeń na najbliższą kończynę, która od tego odrasta szybciej (zasila Odrost, nie HP); Sękacz: zrost gracz–Cielę **przenosi obrażenia Cielęcia na gracza**, bez leczenia. |
| P3 | **Szarża po prostej z dźwiękiem kozy w 4 lochach** | `entity.goat.prepare_ram`: 004 Bocian, 006 Muflon, 009 Kozioł, 010 Muflon. Szarżujących po linii: Truteń 003/010, Dzik 006, Kozioł 009, Cielę 010, Tur | Słownik dźwięków aktu: jeden dźwięk = jedna rodzina. Koza tylko w 006 (Muflon). 004 Bocian: `entity.parrot.imitate.creeper` p0.5 (klekot). 009 Kozioł: zamiast szarży **3 tupnięcia w miejscu** (krąg na 1-2-3, pauza 4). 010: Cielę Tura wypada, za nie Pniak-Taran (§4). |
| P4 | **`block.grindstone.use` 1,0 s** w 004 (Kosiarz, Przodownik, Żniwiarz) i 008 (Młynarczyk) | tabele bestiariuszy | 004 ma pierwszeństwo (kosa). 008 Młynarczyk: `block.wool.hit` + zamach łopatą. |
| P5 | **Garda z przodu i obejście** w 001 (Korowiec) i 010 (Snopowy Strażnik) | 010 §5: „obejście (tył bez gardy), PPM rozbija gardę” | 010: Strażnik **osłania słup**, nie siebie (stoi między graczem a węzłem nici). Kontra: ściągnąć go od słupa albo trafić węzeł z boku. Do tego „PPM rozbija” łamie R2 (Mag ma PPM = zaklęcie), więc ma być „dowolne trafienie”. |
| P6 | **Kadr WOW „z progu na olbrzyma”** w 6 lochach | 003 Królowa z progu, 004 strachy z pagórka, 005 kopuła z galerii, 006 Tur z kosza, 008 szyb z progu, 009 wieś ze wzgórka | Zostawić 3 widoki (004, 008, 009). Pozostałe zamienić w zdarzenie: 003 Królowa BUDZI SIĘ (rój zrywa się przez plaster na gracza; ucieczka 10 s bez walki), 006 Tur **przeskakuje nad Mostem Sznurów**, a pokład się kołysze, 005 dach pęka nad graczem i odłamki spadają (tylko efekt). |
| P7 | **Liczby bez wspólnej skali** | HP bossów: 001 600 / 002 310 / 003 270 / 004 900 / 005 430 / 006 320 / 007 1400 / 008 560. EXP: 002 1100 na poz. 2 (2,75 poziomu) kontra 004 1100 na poz. 4 (1,4) | Prowadzący ustala jedną tabelę: czas walki z bossem 2,5–3,5 min uważnie i EXP ≈1,3 poziomu za Normalny. **Moje 004 i 007 są zawyżone**, 002, 003 i 006 zaniżone (przy 270–320 HP boss pada w 60–90 s, a to „worek treningowy”, [BOSS] §2). |
| P8 | **Pąki nie z poległego** (R1.1 „każdy poległy”) | 002 Skrzek „sadzi identyczne pąki”; 010 Zasiew i Larwy rzucają pąki | 002: Skrzek składa **jaja** (inny model i dźwięk), które pękają chmurą; pąk powstaje dopiero z zabitej Pijawki. 010: dopuszczam jako zwrot finału („Matecznik jest źródłem pąków”), ale pąk Matecznika ma bursztynową poświatę (ten sam kształt i dźwięk) i jest pokazany w Koronie, zanim pojawi się w walce. |
| P9 | **Cała nagroda opisana „roboczo”** | 002/005/008: „rodzina … nazwy robocze, statystyki osobno” | Akceptowalne na tym etapie. Przed budową jedna tabela łupu aktu. |

## 1. 002 Bagna Zgniłych Korzeni (X) — BLOKUJĘ
| Kryterium | Ocena | Dowód |
|---|---|---|
| Fun walki | 7 | Pijawki, Bąk, Mułowiec i Szeptucha mają zamknięte cele i kontry; Kokoniara daje dobrą „ochronę cywila”. |
| Mechanika i zwrot | **6** | Zwrot (zalanie Grobli) dzieje się tylko po śluzie III, a §1 mówi „obie boczne ścieżki są opcjonalne”, więc gracz idący prosto nie przeżywa zwrotu. |
| Czytelność | 8 | Linia zatrucia, suche deski, znaki studni na murze. |
| Topologia | 8 | Rozstaj z dwoma odnogami, pętla dnem, weenie „ramię bagna”. |
| WOW | 8 | Opadająca woda odsłania barkę, kadr ma 3 plany. |
| Sens | 8 | Śluzy miały utopić Szczep; świadectwo działa samodzielnie. |
| Powtarzalność | **6** | Losowane są tylko „porządek sygnałów B/C” i „jeden z dwóch suchych szlaków”, czyli ustawienie, nie taktyka. |
| Nagroda | 7 | 5 sekretów, premie 3/4/5; EXP do skalowania (P7). |
| Wykonalność | 7 | Baza v0.2 z sondą 31/31; zalanie Grobli przez BlockEditor jest realne. |
| Różnorodność | 7 | Kokon (4 s unieruchomienia) i pętla „Włosy przyciągają” mają krewnych w 005 (Dzbanecznik wciąga) — dopuszczalne. |

**Poprawki obowiązkowe:**
1. **Zwrot musi być na trasie obowiązkowej.** · Dowód: §1 i §4, ścieżka III opcjonalna. · Naprawa: III wymagana (Kokoniarnia → Borek → korba → III), II opcjonalna. Kokoniara i Borek to najlepsza treść lochu, więc nie powinny być pomijalne. Zalanie Grobli następuje zawsze.
2. **Powtarzalność zmieniająca taktykę.** · Naprawa (z X etap 1, sprawdzone w jego własnej propozycji): przy wejściu losowo **uszkodzona jest śluza II albo III**. Uszkodzona nie zamyka swojej studni, tylko daje inny sekret. Gracz zawsze ma inny zestaw czynnych studni, więc Topielica zmienia rytm wynurzeń. Do tego Kokoniara losuje stronę kokonu Borka (wąska izba / szeroki pomost).
3. **Pąk dwuznaczny** (R1: „nie wyłącznie obowiązek sprzątania”). · Naprawa: chmura bąbla truje WSZYSTKO, w tym Pijawki i Mułowca. Zabity wróg przy grupie = pąk, który można zostawić jako broń.
4. **Skrzek nie sadzi pąków** (P8).
5. **HP Topielicy** wg P7. Przy 310 HP Mag zabija ją w ~70 s, zanim zobaczy fazę 3.

Mag solo: Kładki są trudniejsze (Pijawki skaczą bliżej niż zasięg zaklęć), ale da się je przejść. Wojownik solo: Bąk na
osobnej wysepce zmusza do biegu przez linię kolców, co jest dobre. Kitowanie: Włosy przy >6 bl., więc nie przejdzie.

## 2. 005 Oranżeria Darnika (X) — BLOKUJĘ
| Kryterium | Ocena | Dowód |
|---|---|---|
| Fun | 7 | Sadzonki działają w walce (Rosiczka łapie szarżę, Purchawa przerywa Szew). |
| Mechanika i zwrot | 8 | Zwrot „przejęte gniazdo” odwraca narzędzie, a boss wchłania posadzone rośliny. |
| Czytelność | **6** | Na poziomie 5 jest naraz 5 systemów: kapsuły z pąków (podnieś w 12 s), 2 z 4 typów, gniazda, nici zrostu, przejęcia. Boss ma 7 zdolności, w tym 4 zależne od tego, co posadzono. Bez tekstu gracz nie odróżni „mojej” Rosiczki od „jej”. |
| Topologia | **6** | Hub z 4 skrzydłami, ale „do bossa potrzebne dwa rdzenie z dowolnych dwóch skrzydeł” (§3). Wybór Paprociarnia + Storczykarnia pomija Kotłownię, czyli ZWROT. To ten sam błąd co w 002. |
| WOW | 8 | Kopuła z drzewem przebijającym dach, kadr z ramą (P6: zamienić na zdarzenie). |
| Sens | 8 | Dziennik „nikt więcej nie będzie głodny”, Ilma i Łada. |
| Powtarzalność | 9 | Losowane 2 z 4 sadzonek zmieniają walkę z bossem. |
| Nagroda | 7 | Robocza. |
| Wykonalność | **6** | §13: „PPM wywołuje PlayerInteractEvent/funkcję MD, zużywa KeyItem”. U nas PPM = lewa ręka zaklęć (Zaklęcia v4.1) albo ładowany cios, a KeyItem MD wymaga przedmiotu w ręce, czyli zmiany slotu w walce. Nić między dwoma ruchomymi mobami z hitboxem „na środku” = teleport encji co tick (pamięć optymalizacja-walk-chowancow: onTeleportEvent co tick, koszt). |
| Różnorodność | 7 | Zrost jest tu nauczany, a w 010 egzaminowany. To świadoma oś, dopuszczam. |

**Poprawki obowiązkowe:**
1. **Kotłownia (zwrot) na trasie obowiązkowej.** Naprawa: boss wymaga rdzenia z Kotłowni i jednego dowolnego z pozostałych trzech skrzydeł.
2. **Sadzenie bez klikania.** · Sadzonka trafia do „sakwy” (licznik w HUD MythicHUD, nie przedmiot w ręce) przez wejście w kapsułę. · Sadzenie odbywa się przez wejście na gniazdo (MD TriggerDistance r1,5 + warunek posiadania), bez PPM i bez KeyItem w ręce. · Działa dla każdego archetypu i nie koliduje z Księgą ani z blokiem.
3. **Odchudzić systemy.** · Kapsuła z pąka leży na ziemi do końca starcia (nie 12 s), więc gracz nie musi gonić zegara w walce. · Nić tylko statyczna: węzeł przy stopach JEDNEGO z pary, jak plan B samej karty, ale jako plan A. · Boss: jego wersje zdolności mają kolor zielony, wersje gracza biały (jeden język kolorów na cały loch, opisany w §1).
4. **HP 430 przy poz. 5** wg P7.

Mag solo: w Kopule nić 2×2 da się trafić zaklęciem. W bossie Mag stoi z dala od gniazd i musi do nich podbiegać, a po
poprawce 2 wystarczy wejść. Wojownik solo: OK. Kitowanie: Szew i szyna — OK.

## 3. 008 Spichlerz Głodnego Roku (X) — BLOKUJĘ
| Kryterium | Ocena | Dowód |
|---|---|---|
| Fun | 7 | Worki jako ruchoma osłona dobrze robią „ciężar w ruchu”. |
| Mechanika i zwrot | **6** | Kara za chleb nie ma zębów. Przy 5 stosach Matka leczy się o 10 HP na trafienie, czyli 1,8% z 560. 5 trafień w walce to ~9% HP, a Chleb Siły daje +15% obrażeń przez 60 s. Pokusa jest więc darmowa. Zwrot „Matka demonstruje na cieniu gracza” (§1) nie mówi, co gracz WIDZI. |
| Czytelność | 7 | Pasek nacięć 0–5 przy dłoni, zielony puls leczenia. Wewnętrzna sprzeczność: haczyk mówi „stos 0–10”, a §6 „maks. 5 stosów”. |
| Topologia | 8 | Pięć pięter, zsypy z wyborem, winda-skrót, szyb jako kadr. |
| WOW | 8 | Szyb 40 bl. z kłosem. |
| Sens | 9 | Bochen widoczny wcześniej w Nexusie, list przewozowy, Mira; konsekwencja uczciwie lokalna. |
| Powtarzalność | 7 | 2 z 4 chlebów zmienia rachunek, ale S tylko „bez chleba” sprawia, że gracz polujący na ocenę nie ma pokusy. |
| Nagroda | 7 | Robocza. |
| Wykonalność | 7 | „Lokalna flaga gracza 0–5”: zmienne `caster.*` w MM nie działają (pamięć mythicmobs-cicha-walidacja). Użyć tagów lub `scoreboard` przez `command{}`, a stos sprawdzać w TargetConditions `score`/`hastag`. |
| Różnorodność | 7 | P1 (kłos), P4 (szlifierka). |

**Poprawki obowiązkowe:**
1. **Kara za chleb proporcjonalna do mocy.** · Leczenie = **1,5% max HP Matki × stos za trafienie** (5 stosów = 7,5% na trafienie, czyli 42 HP z 560). · Korzeń Zaszczepienia **nie unieruchamia**, tylko daje spowolnienie i przypina na 1 s. Przez 2 s po korzeniu żaden atak Matki nie celuje w tego gracza. · Obecnie unieruchomienie 3 s + Rzut workiem 1,1 s w zamknięty punkt = trafienie bez kontry (brief: brak łańcucha unieruchomień).
2. **Zwrot jako obraz.** Na F4 Matka po raz pierwszy wyłania się z zsypu, trafia gracza z chlebem (scenariusz bezpieczny, 1 obrażenie) i jej worek-ciało widocznie PĘCZNIEJE o zielony segment. Gracz bez chleba widzi, że cios go nie karmi.
3. **Dwie drogi do S.** „Post” (0 chleba) albo „Uczta” (5 stosów i boss <90 s), żeby pokusa żyła przy powtórkach.
4. **Stos 0–5 wszędzie**; licznik na scoreboardzie lub tagach (nie `caster.*`).
5. **P1, P4, P7.**

Mag solo: Korzeń na Magu przy 5 stosach co 8 s dziś oznacza stały spętany cel, a po poprawce 1 da się grać. Wojownik
solo: przesuwanie worka LPM jest dobre. Kitowanie: Rzut workiem przy >8 bl. — OK.

## 4. 010 Matecznik (prowadzący) — BLOKUJĘ
| Kryterium | Ocena | Dowód |
|---|---|---|
| Fun | 8 | Rytm „tnij, depcz, bij”, Szarża w najdalszego, trzy kończyny w kieszeniach. |
| Mechanika i zwrot | 8 | Odrost (pąk przy przeciętej nici ją zszywa) to prawdziwy egzamin z R1; zwrot „drzewo zrzuca gracza” jest fizyczny. |
| Czytelność | 7 | Dwa skutki pąka zależne od miejsca. Sam to oznaczasz jako ryzyko, a mój 001 ma ten sam problem (mały/duży). |
| Topologia | 7 | Pień jest unikalny, ale Podnóże z trzema konarami w dowolnej kolejności to trzeci hub z ramionami (002 Rozstaj, 005 Kopuła). Ratuje go upadek do środka. |
| WOW | 9 | Dziewięć miniatur gaśnie nić po nici, potem zsuw z płatkami w górę. |
| Sens | 9 | Grób Dary, „przytnij — ja nie umiem”; odpowiedzialność za plony (Hanna). |
| Powtarzalność | 7 | Dziki konar i kolejność F2 zmieniają priorytety. |
| Nagroda | 7 | „≈1 poziom” i rodzina dla każdego archetypu; brak liczb. |
| Wykonalność | 7 | Węzły statyczne (dobrze), upadek z forceload. Truteń zastępczo „bee ×2”: vanillowa pszczoła ginie po użądleniu i ma własne AI, więc trzeba ją wyłączyć albo wziąć inny byt. |
| Różnorodność | **6** | Snopowy Strażnik = Korowiec z 001 (garda z przodu, P5). Muflon Matecznika = Muflon z 006 (ten sam dźwięk i spychanie, P3). Cielę Tura = szarża Tura w miniaturze. Szczepienie + Sękacz to trzeci boss „leczący się z gracza” (P2). Czas 18–22 min przekracza brief (12–20). |

**Poprawki obowiązkowe:**
1. **Krewniacy z nowym zwrotem, nie kopie** (brief §3.6). · Snopowy Strażnik osłania słup (P5). · Muflon wypada, za niego Barciowy Rój (krewniak 003): jedna chmura pszczół krąży wokół słupa, znamię nie istnieje, ale rój atakuje każdego w promieniu nici, więc nić trzeba ciąć z dystansu albo po przejściu chmury. · Cielę zastąpić **Pniakiem-Taranem** (krewniak Korzenia-Tarana z 001): wbija się w ścianę konaru i tworzy most; zabity daje pąk, który przy nici ją zszywa.
2. **Szczepienie bez leczenia** (P2): przekierowuje obrażenia na kończynę i przyspiesza jej Odrost.
3. **Czas ≤20 min.** Konary po 3 min zamiast 4 (sala 5 bez mostu, który się zapada, bo „wejście bez powrotu” już było w sali 8), Korona 1 min.
4. **Pąk Matecznika z bursztynową poświatą** (P8); pokazać w Koronie, zanim padnie w walce.
5. **R2.** „PPM rozbija gardę” (Snopowy) i „PPM ładowany w okno” zastąpić „dowolne trafienie”; łuk Ninjy nie jest gwarantowany w kanonie, więc zapisać „dowolny atak”.

Mag solo: węzły na 1,5 bl. trafia, Główne Nici z ziemi też. W F2 Sekator „blok 3. cięcia = ogłuszenie” daje Magowi mniej
(ma blok?), więc niech okno daje też chybienie 3. cięcia. Wojownik solo: Główne Nici na wysokich korzeniach wymagają
wejścia po rampie pod Opadem. Dobre napięcie, zostawić.

## 5. „Suche i bez sensu” (brief §4) — zbiorczo
- Wskazówki: 002 (Borek „na południe od Kokoniarni”) sprawdzone przez X na współrzędnych ✓. 010 „ślad małych butów” i „smuga pszczół” są wizualne i dobre ✓.
- Komunikaty w walce: wszystkie 4 karty mają kwestie poza walką ✓. 010 kwestia 2 przed walką — jedna ✓.
- Obietnice: 010 miniatury nie są portalami ✓. 008 Mira mówi o „zwykłym zbożu za murem” — sekret „Zwykłe ziarno” to pokazuje ✓.
- Nagroda warta wysiłku: wszędzie robocza (P9).
- Kitowanie: zamknięte we wszystkich 4 ✓.
- „Boss + fale”: brak ✓.
- Losowość kosmetyczna: 002 ✗ (poprawka 2).

## 6. R1 / R2 / R3 — kontrola
| | R1 (pąk) | R2 (4 archetypy) | R3 (rozstrzygnięcia) |
|---|---|---|---|
| 002 | ✗ Skrzek sadzi pąki; pąk wyłącznie zły | ✓ tabela | ✓ Dzwon jednorazowy, Bąk, koszt osuszania; ✗ zwrot opcjonalny |
| 005 | ✓ kapsuła po dojrzeniu | ✗ PPM/KeyItem (Mag) | ✓ zrost, sadzonki w walce, tabliczki tylko sekret; ✗ Kotłownia pomijalna |
| 008 | ✓ (P1 do zmiany) | ✓ ale korzeń 3 s przy Magu | ✓ nieodwracalne, korzenie cięte, świadectwo lokalne; ✗ kara bez zębów |
| 010 | ~ pąk Matecznika (P8) | ✗ „PPM rozbija gardę” | ✓ Darnik odcięty, walka jako kulminacja, odpowiedzialność za plony |

## 7. Moje własne karty po tym przeglądzie (żeby nie było taryfy dla siebie)
- 004: Kosiarz ma pierwszeństwo do szlifierki (P4); Bocian zmienia dźwięk (P3); HP Żniwiarza 900 do skalowania (P7).
- 007: HP Ropuchy 1400 do skalowania (P7).
- 001: dwa skutki pąka zależne od wielkości ciała mają to samo ryzyko co 010. Wspólny test w kliencie QA: jeden blockout, oba warianty.
