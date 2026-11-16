# Projektant Y — Zielona Granica: propozycja Aktu 1

**Status:** etap 1, propozycja do sporu zespołowego; nie jest specyfikacją do wdrożenia. Poziomy 1–10, każdy loch otwarty od N−1, Normalny solo 12–20 min każdym archetypem bez specjalizacji. Instancje i wyspa są równorzędnymi, niezależnymi ścieżkami. Skróty w C: `V` = gracz widzi i wybiera wejście, `Z` = zapowiedziana zasadzka, `P` = po przekroczeniu progu; `n/w` = tempo niskie/wysokie. Każda podana zapowiedź trwa co najmniej 0,6 s, ma odrębny dźwięk/pozę, a unikanie oznacza zwykły ruch, blok lub przerwanie — nigdy osobny przycisk uniku. Wszędzie: checkpoint przed bossem, powrót po upadku, nagroda/sekrety dla całej drużyny, maksimum jeden komunikat w walce, 1 typ w lekcji / 2 typy zwykle / 3 typy tylko w teście.

## A. Audyt starego planu 100

1. **Potwierdzam: Akt 1 jest próbnikiem aktów późniejszych.** `plan-100-lochow-10-wysp.md` §1, §3–4 wprost stawia 003 wodne Archiwum obok morskiego Aktu 2, 004 kryształy obok Głębi, 007 mróz obok Aktu 5, 008 żar obok Aktu 8, 009 Anubisa/mumie/obelisk obok pustynnego Aktu 3. To nie tylko podobny kolor: powtarzają się mechaniki, sylwetki i rekwizyty.
2. **Potwierdzam: kolumny produkują formułę.** `plan-100…` §1 przypisuje K2 status, K3 kanał, K4 odbicia, K8 ogień, K9 kość we wszystkich 10 aktach; §4 pokazuje w Mroźnej Koronie „Zimowy Ogień”, w Popielnej Koronie „Czysty Ogień”. Nawet jeśli pojedynczy loch byłby dobry, porządek przewiduje jego problem za gracza. To jest ryzyko projektowe, nie dowód, że wszystkie 100 gotowych lochów są złe — istnieją tylko nazwy i haczyki.
3. **Potwierdzam: „podstawy” nie są prawem.** Tabela `plan-100…` §3 opisuje dla Aktu 1 „unik, blok, przerwanie, osłona”, czyli ogólną walkę, w dodatku „unik” koliduje z kanonem sterowania w `00-BRIEF.md` §1 (brak przycisku uniku). Pozostałe akty mają działającą regułę świata, np. przypływ.
4. **Potwierdzam z zastrzeżeniem: nie ma spoiwa całego aktu.** `plan-100…` §2 ma zasadę „strefa pokazuje skutek, loch przyczynę”, a 002 posiada lokalną historię Weli/Borka/Topielicy (`loch-002…` §1), więc nie jest fabularną pustką. Nie ma jednak przyczyny łączącej 001–010 ani stopniowego odkrycia, a „Brama Pierwszego Aktu” z `dungeon-ladder…` jest kompilacją motywów bez własnego dramatu.
5. **Potwierdzam: pary 1:1 są nieaktualne.** `plan-100…` §1–2, §4 i §5 używa dosłownie „komórki” loch↔strefa, klucza ze strefy N do lochu N i 10 stref w parach. `00-BRIEF.md` §1 zapisuje korektę właściciela z 30.09: wspólne są kraina, zagrożenie, krewniacy i materiały, ale konkretna fabuła i forma nie mają być parami.
6. **Własne znalezisko: 001 nie spełnia obecnej bramki, nawet na papierze.** `loch-001…` §2–3.2 ma 13 starć, 8 typów zwykłych i jedną trasę w pasmach, mimo pobocznych odnóg; §1 obiecuje wiele lekcji naraz, a §2 nazywa arenę płaską r=32. Ocena właściciela 2/10 w `00-BRIEF.md` §1 nie jest więc tylko kwestią oprawy. Zachować można przetestowane GAJ_WILK/KORZENIAK/LUCZNIK i ideę światła, nie obecny przebieg.
7. **Własne znalezisko: 002 ma mocny prototyp, lecz słabą sprawczość i sprzeczność ścieżek.** Śluzy realnie zmieniają studnie bossa (`loch-002…` §1, §3.1), ale Dzwon daje pełną odporność na truciznę, wyłączając główną lekcję; finał odsłania „wejście do Lochu 003”, choć instancje otwierają się z poziomu/menu i nie mogą tworzyć obowiązkowego ciągu. Zachować hydrologię i postaci, zmienić nagrodę na czytelniejszą kontrę oraz wizję finału na wskazówkę, nie bramę.
8. **Własne znalezisko: stara drabina zostawia sensowny szkielet QA.** `dungeon-ladder…` „Zasady” wymaga solo, sygnałów i trudności zmieniających zachowanie; te normy warto przenieść. Nie przenosić zasady „002–010 nie oznaczają 1 loch = 1 poziom”, bo D5 w `00-BRIEF.md` §1 ustaliło nową skalę.
9. **Własne znalezisko: model nie może prowadzić pomysłu.** `akt1-wyspa-i-lochy.md` §2 dobiera kandydatów LittleRoom po nazwie; `projektowanie-bossow/README.md` §1, §8–9 dowodzi na przykładzie Strażnika Gaju, dlaczego najpierw trzeba ustalić funkcję, haczyk i emocję, a dopiero potem sylwetkę. W C podaję rolę bossa; model jest wyłącznie kandydatem po ocenie w kliencie.
10. **Własne znalezisko: natłok instrukcji grozi utratą czytelności.** 001 chce uczyć bloku, parowania, przerwania, LoS, kolejności celów i statusów w pierwszych 10–15 min (`loch-001…` §1–2). Badanie `research/wynncraft/audyt/09-opinie-graczy.md` §2–4 opisuje skargi na przeciążający onboarding i dialog zasłaniający walkę; u nas jeden motyw na loch powinien mieć pierwszeństwo przed katalogiem lekcji.

## B. Akt 1 — kraj, prawo, stawka

**Tożsamość.** Zielona Granica to zamieszkany pas współpracy na styku drogi, mokradeł, sadów, pasiek i młodych lasów: ludzie, elfy, orkowie i nieumarli naprawiają te same przeprawy. Podziw wynika z bliskości życia i pracy, nie z odległego żywiołu. Nie ma morskiego przypływu, pustynnego żaru, podziemnego kryształu, mrozu, księżycowych faz, frontu wojny, erupcji ani podniebnej burzy. Wiary mogą istnieć jako prywatne znaki mieszkańców, ale fabuła aktu nie wymaga panteonu (`panteon.md`; decyzja w briefie §1).

**Prawo Odzewu.** Wyraźnie oznaczone żywe szwy korzeniowe pod stopami gracza lub wroga gromadzą nacisk; chowaniec nie nabija progu sam, żeby jego AI nie karało Akolity. Po przekroczeniu progu biegnie po nich złota fala, słychać dwa drewniane stuknięcia, a po 2 s oznaczony węzeł wykonuje jeden lokalny skurcz (np. zamyka szczelinę, wychyla osłonę, podnosi płytę). Potem wraca i stygnie 8 s. Skurcz ma taką samą geometrię i karę dla gracza, wroga i chowańca; można go sprowokować, obejść lub wyczekać. Na 001 jedna żyła i jeden bezpieczny pas; na 010 krzyżują się dwie żyły, ale sygnał i reguła nie zmieniają się. To prawo, nie dodatkowy pasek, klawisz ani losowy debuff. Odróżnić stały szew od dekoracyjnych korzeni teksturą, cząsteczką i dźwiękiem; w Łatwym czas 3 s, w Trudnym 1,5 s, bez zmiany znaku. Fallback techniczny: skurcz jako wyświetlana bryła i strefa MM zamiast edycji terenu; próg liczony per węzeł MD/MM, bez premium target conditions.

**Zagrożenie.** Wspólny cech Pogranicznych Naprawiaczy, ze strachu przed zawaleniami dróg, przybił dziewięć węzłów żywego splotu „na stałe”. Mistrzyni mostów **Mira Sucha** ukryła koszty: każdy zablokowany skurcz przepycha siłę do kolejnego siedliska. Nie jest demonem ani rasowym wrogiem; jej początkowa decyzja uratowała ludzi, a potem stała się pułapką. Strażnicy, zwierzęta i pracownicy bronią miejsc z rozpoznawalnych powodów. Gracz nie „zabija lasu”; uczy się oddawać mu miejsce bez porzucania mieszkańców.

**Wątek i zmiana po każdym ukończeniu.** Osobisty atlas w Nexusie (menu/dialog, bez zależności od wyspy) dopisuje jeden rysunek i krótką wiadomość NPC; ścieżka wyspy odkrywa to samo zagrożenie własnymi wydarzeniami. Każdy loch otwiera poziom, nie poprzedni loch; dowolna kolejność pomija zależne ekspozycje i daje samodzielną historię miejsca. Publiczny teren wyspy nie przestawia się od pojedynczego przejścia instancji. Wspólne materiały i krewniacy, odrębni bossowie i fabuły.

| Loch | Co gracz odkrywa | Co zmienia się w jego świecie po lochu |
|---|---|---|
| 001 | Skurcz korzeni bywa obroną ścieżki, nie atakiem. | Atlas pokazuje pierwszy żywy szew; w Nexusie pojawia się wzór dwóch stuknięć. |
| 002 | Zablokowany odpływ przenosi nacisk w bagno. | Rybacy dają atlasowi mapę śluz; w hubie dostępna jest wymiana materiałów bagiennych. |
| 003 | Mapy cechu celowo pomijały starsze przejścia. | Rysunek ukazuje skreślony szlak, a kartografka odczytuje go na głos. |
| 004 | Kredowe znaczniki fałszowały „bezpieczne” obciążenie. | W atlasie pojawia się prawdziwy znak nośności; NPC poprawia tablicę ostrzeżeń. |
| 005 | Pszczoły omijały wbite przez cech kotwy. | Sprzedawca przyjmuje wosk i pokazuje puste miejsce ula; to oferta, nie brama. |
| 006 | Mosty były naprężane na koszt korzeni pod osadą. | W Nexusie pojawia się plan rozpięcia lin; nowy dialog Miry tłumaczy jej motyw. |
| 007 | Sad dawał plon tylko przez odcięcie dzikich odrostów. | Sadzonka wraca do skrzynki atlasu, a zielarz udostępnia recepturę obu ścieżkom. |
| 008 | Dym alarmowy mylono z sygnałem pożaru, tłumiąc ostrzeżenia splotu. | Dwa znaki alarmu trafiają na tablicę w hubie; dialogi mieszkańców rozróżniają je. |
| 009 | Zwierzęta migrowały trasą, której cech nie widział. | Atlas nakłada szlak stad na mapę dróg; Mira przyznaje, co przemilczała. |
| 010 | Rozwiązanie wymaga oddania splotowi rytmu zamiast kolejnej kotwy. | W atlasie sieć znów pulsuje; gracz ma tytuł i wybór świadectwa, a Akt 2 otwiera poziom, nie ten kill. |

**Łuk emocjonalny:** ciekawość i piękno 001 → niepokój 002–003 → podejrzenie wobec własnych map 004–006 → żal po utraconej współpracy 007–008 → empatia dla ludzi i zwierząt 009 → odpowiedzialna ulga 010. Osią jest poznawanie konsekwencji własnych zabezpieczeń, nie eskalacja od „słabych” do „złych” potworów.

## C. Dziesięć lochów — karty etapu 1

Każdy zapis `sala[tempo/min/start/skład lub działanie]` jest skondensowanym beat chartem; sala ma 20–40 bloków, siedem–osiem sal daje 12–20 min. Ognisko stoi w sali startowej i przed bossem; wyraźny skrót wraca do pierwszego po otwarciu od wewnątrz. Losowanie wybiera jeden z dwóch układów *przyczynowych* na wejściu i jest widoczne przed walką. Sekrety mają wskazówki środowiskowe, nie niewidzialne skrzynki. Własne nazwy wrogów są robocze; przy każdej podano rolę, zapowiedź, karę i uniwersalną kontrę. Bossowie dostają sylwetkę i 4–6 ruchów z zapowiedzią w późniejszej specyfikacji; poniżej są zestawy egzaminacyjne i najjaśniejszy punkt słaby. Nagrody zawsze obejmują EXP, rodzinę sprzętu i materiał profesji równy wartości wyspy oraz żeton przeciw pechowi; sekret daje dodatki, nie wymagane statystyki. Za podstawę czytelności przyjmuję kolejność „funkcja → haczyk → wygląd” i anatomię ataku z `research/projektowanie-bossow/README.md` §1–3.

### 001 — Kwietna Polana (poziom 1)
Fantazja/emocja/czas: wchodzę pod kwitnącą koronę i uczę się, że gaj ostrzega, zanim zamknie drogę; zachwyt, 14–16 min.
Motyw: **lampy utrzymują przejście otwarte**; wstęp = jedna niegroźna żyła, rozwinięcie = ustawienie lampy po stronie celu, ZWROT = wróg gasi lampę i otwiera sobie drogę, egzamin = odzyskać osłonę, gdy boss gasi światło.
Topologia: zwarta spirala wokół widocznego od startu pnia areny, `wejście→spirala{ogród A↺ / wieża B↺}→brama→pień↺skrót do wejścia`; cele A/B w dowolnej kolejności, nie łąka w pasmach.
Beat: ogród[n/2/V/—] → szew[n/2/V/Korzeniak] → wieża[w/2/Z/Łucznik] → wybór lamp[n/2/V/zagadka].
Beat: cień[w/2/P/Wilk+Korzeniak] → brama[n/1/V/ognisko,skrót] → pień[w/3/P/boss]; naprzemiennie walka i działanie.
Wilk Gajowy (pościg): przycupnięcie i warknięcie 0,8 s zapowiada skok karzący stanie na szwie; krok w bok lub blok zostawia go bezbronnym.
Korzeniak (kontrola): czerwone oko i trzask 0,8 s zapowiadają linię korzeni; zejście z linii pozwala uderzyć przysiadły rdzeń.
Cierniowy Łucznik (dystans): naciąg 0,7 s wskazuje tor, a zignorowany ostrzał spycha ku szwowi; pień, ruch boczny lub doskok kontruje.
Ćma Knotowa (sabotaż): obrót ku lampie i szelest 1 s zapowiada zgaszenie; przechwycenie jej ciałem albo szybkie trafienie chroni lampę.
Elita Pasterz Pędów (strażnik): unosi dwa ramiona 1 s przed zamknięciem połowy bramy; można go obejść, zablokować cios i zapalić boczną lampę.
Boss **Strażnik Przejścia**, dawny żywy znacznik drogi: jego kora chroni ciemną stronę, więc trzeba przestawić światło, nie bić w płaszcz; fazy: czytelny szew → zgaszona lampa i odwrócone bezpieczne pole → dwa naprzemienne szwy, bez fali addów. Ruchy: zamach, szew, zgaszenie, osłona, odrzut; najjaśniejszy sęk jest odsłonięty po skurczu. Kandydat: model własny.
WOW: w 7. minucie z wieży B widać, jak po zapaleniu lampy jeden złoty puls rozchodzi się po całej koronie i otwiera prześwit dokładnie na arenę bossa.
Paleta: mchy, trawa, kwitnące azalie, brzoza, jasny dąb, białe płatki, żółtawe latarnie; południe, cichy stuk kory i owady.
Poza walką: trzy sekrety z tropami (wosk przy lampie, ślady wilka do schowka, płatki prowadzące na skrót); wybór kolejności A/B zmienia osłonę w sali bramy.
Powrót/trudności: losuje się, która lampa gaśnie w cieniu, co zmienia trasę obejścia; Łatwy wydłuża puls, Trudny pozwala Ćmie raz zgasić lampę w walce z elitą.
Nagroda i świat: kwietny wosk, drewno, sprzęt szlaku i pierwszy rysunek atlasu; oryginalne 001 zachowuje trzy sprawdzone moby i ideę lamp, zastępuje puste pasma, 13 paczek i płaską arenę.
Granica aktów/technika: brak księżyca i boskiego światła Aktu 6; lampy przez TriggerInteract/zmienną instancji, plan B: dwa gotowe warianty osłon zamiast BlockEditora.

### 002 — Bagna Zgniłych Korzeni (poziom 2)
Fantazja/emocja/czas: odzyskuję rybackie groble z wody, widząc, jak jeden odpływ ratuje drugie miejsce; niepokój, 16–18 min.
Motyw: **śluzy zmieniają trasę i studnie bossa**; wstęp = sucha pierwsza kładka, rozwinięcie = śluza odsłania dno, ZWROT = odprowadzenie wody budzi szew i zalewa dotychczasowy skrót, egzamin = kontrolować studnie Topielicy bez „odporności na wszystko”.
Topologia: `przystań→tama→rozstaj{zachód śluza / wschód Borek→śluza}→grobla→topiel↺przystań`; dwa obiegi mokradła, boss widoczny jako czarna sadzawka zza tamy.
Beat: pomost[n/2/V/Pijawka] → tama[w/2/Z/Mułowiec] → śluza 1[n/2/V/kołowrót] → odnoga[w/3/V/Kościej+Pijawka].
Beat: kokon[w/2/P/Kokoniara] → grobla[n/2/V/ognisko,skrót] → topiel[w/3/P/boss]; na odnogach niskie tempo i wskazówki.
Pijawka (rój): plusk 0,7 s zapowiada skok z wody, którego seria zatruwa; utrzymanie kładki i szeroki cios rozwiązują problem.
Mułowiec (blokada): bulgot i uniesiony bark 0,8 s zapowiadają ślad spowolnienia; obejście śladu otwiera flankę.
Kościej Bagienny (dystans): naciąg 0,8 s sygnalizuje strzał ku grobli; pień i ruch boczny są dostępne wszystkim.
Szeptucha (wsparcie): zielony kocioł i śpiew 1 s zapowiadają ochronę Mułowca; doskok albo trzy lekkie ciosy przerywają kanał.
Elita Kokoniara (chwyt): dwie przednie nogi i syk 1 s zapowiadają kokon, który po trafieniu można samemu rozbić trzema ciosami; niezłapany gracz ma okno po skoku.
Boss **Topielica, Matka Mułu** straciła tarlisko przez groble: im więcej osuszonych studni, tym mniej miejsc wynurzenia, ale tym silniejszy jeden przewidywalny skurcz; fazy: wynurzenia → jaja przy aktywnych studniach → ostatnia sucha wyspa. Ruchy: włosy, kałuża, nurkowanie, skrzek, skurcz; jasne oko nad ostatnią studnią jest celem po pudle. Kandydat: model własny.
WOW: w 6. minucie z korony tamy woda opada, ukazując łódź wiszącą między korzeniami i trzy linie pulsu biegnące ku czarnej sadzawce.
Paleta: muł, mangrowiec, ciemny dąb, mokry kamień, miedziane kraty, trzciny, zielone glony; matowe zielono-brązowe światło, krople i niski klang kołowrotu.
Poza walką: trzy sekrety z tropami (ślady sieci do wraku, głos Borka do kokonu, dźwięk dzwonu pod śluzą); uratowanie Borka daje korbę, a dzwon jednorazowo usuwa truciznę, lecz żadne nie blokuje bossa.
Powrót/trudności: losuje się, którą opcjonalną śluzę zatkała kłoda do usunięcia, więc plan grobli się zmienia, lecz korba Borka zawsze działa na wschodzie; Łatwy słabszy skurcz, Trudny jaja pojawiają się przy dwóch czynnych studniach.
Nagroda i świat: glina, zioła, perła i mapa śluz; zachować zbudowaną hydrologię, Welę, Borka i Topielicę z `loch-002…`, usunąć pełną odporność na truciznę i obowiązkowe „wejście” do 003.
Granica aktów/technika: to osuszanie bagna, nie cykliczny przypływ Aktu 2; użyć istniejącego generatora/MD, plan B dla poziomu wody: podmiana gotowych basenów, bez płynącej symulacji.

### 003 — Papiernia Zatajonych Ścieżek (poziom 3)
Fantazja/emocja/czas: odczytuję mapę granicy z ruchomej taśmy papieru i odkrywam wykreśloną drogę; podejrzenie, 15–17 min.
Motyw: **odwracanie biegu taśmy przestawia mostki i kolejność patroli**; wstęp = pusta taśma, rozwinięcie = dwa kierunki dla dwóch celów, ZWROT = ktoś wyrywa taśmę, więc trzeba przejść po jej nieruchomym szkielecie, egzamin = boss zmienia kierunek po zapowiedzi.
Topologia: pionowa pętla wokół wału `hala dół↗drukarnia↘magazyn↗galeria boss↺winda`; dwa odczyty mapy można zebrać w dowolnej kolejności, brama bossa widoczna nad głową.
Beat: wejście[n/2/V/taśma] → wał[w/2/V/Skryba] → dwa stoły[n/2/V/zagadka] → magazyn[w/2/Z/Nożycowiec+Skryba].
Beat: zerwanie[w/2/P/Drukarz] → winda[n/2/V/ognisko,skrót] → galeria[w/3/P/boss]; kierunek taśmy zdradza dźwięk koła.
Skryba Pyłu (dystans): biały obłok i szelest 0,7 s zapowiadają linię papierowych cięć; zejście z linii lub osłona wału kontruje.
Nożycowiec (zwarcie): otwiera dwa ostrza 0,8 s, a ignorowany tnie przy krawędzi taśmy; blok i obejście po skoku dają okno.
Drukarz (kontrola): ciężki stempel unosi się 1 s przed odciskiem na kratce; ruch do pustej kratki albo przerwanie dźwignią działa dla wszystkich.
Strażniczka Korekty (wsparcie): wskazuje palcem mapę 0,8 s przed zmianą patrolu; dojście i uderzenie przerywa znak.
Elita Zecerka (pościg): słyszalne przekładanie czcionek 1 s oznacza, który most zamknie; gracz może przeczekać na galerii lub wymusić jej wejście na szew.
Boss **Archiwistka Przejść** wycina drogi, by uniknąć paniki mieszkańców: jej osłona zależy od kierunku taśmy, a po zerwaniu ujawnia ręcznie ukryty fragment; fazy: czytelny obrót → zerwanie i piesza pogoń → ręczne przewijanie. Ruchy: stempel, papierowy stożek, odwrót, skurcz, cięcie; biała ręka jest słabym punktem po zmianie kierunku. Kandydat: model własny.
WOW: w 5. minucie z górnej galerii widać, jak gigantyczna taśma wypisuje na ścianie mapę całego gaju, a jeden trakt zostaje czarną farbą wykreślony na oczach gracza.
Paleta: brzozowe deski, białe płótno, czarny betonowy atrament, beżowy papier, miedź, ciemny kamień; blade światło okien, stuk drewnianych pras.
Poza walką: sekrety mają tropy w błędach drukarskich, strzępach na belce i odcisku dziecięcej stopy; wybór kolejności odczytów zmienia drogę powrotną.
Powrót/trudności: losowy pierwszy kierunek taśmy przestawia bezpieczny korytarz; Łatwy zatrzymuje wał dłużej, Trudny Zecerka raz cofnie taśmę podczas walki.
Nagroda i świat: włókno, tusz, sprzęt kartografa, skreślona droga w atlasie; nie zachować zatopionego Archiwum 003 ze starej drabiny.
Granica aktów/technika: bez ruin morskich Aktu 2; ruchoma taśma jako trzy stany BlockEditora, plan B: statyczne mostki aktywowane dźwigniami i animacja item_display.

### 004 — Kredowy Jar (poziom 4)
Fantazja/emocja/czas: przechodzę po białych półkach, które cech oznaczył jako bezpieczne, i sprawdzam, komu służą znaki; zwątpienie, 14–16 min.
Motyw: **sprawdzanie nośności półki przed wejściem**: użycie próbnego kołka przy brzegu wznieca pył z pęknięcia i wskazuje fałszywy znacznik; wstęp = bezpieczna próbka, rozwinięcie = wybór półek, ZWROT = stabilna półka pod bossem pęka, a „fałszywa” staje się drogą ratunku, egzamin = czytać rysy, nie farbę.
Topologia: tarasy wokół otwartego jaru `góra→(lewy taras / prawa szczelina)→dno→winda→krawędź boss↺góra`; droga pionowa, brama bossa widać przez jar.
Beat: próg[n/2/V/próba] → taras[w/2/V/Kredziak] → wybór[n/2/V/znaki] → szczelina[w/2/Z/Strącarz+Kredziak].
Beat: dno[n/2/V/ślad ekipy] → winda[w/2/P/Dozorca] → krawędź[n/1/V/ognisko] → boss[w/3/P]; fala odzewu wskazuje rysę.
Kredziak (zwarcie): biały obłok z pięści 0,7 s przed uderzeniem spycha z wąskiej półki; blok albo ruch do ściany kontruje.
Strącarz (dystans): wysunięcie dłoni 0,8 s zapowiada pocisk kredy na oznaczony skraj; filar i ruch do środka są bezpieczne.
Rysownik (sabotowanie): stuk pędzla 1 s poprzedza przemalowanie znaku; obserwacja pęknięcia lub przerwanie ciosami ujawnia prawdę.
Półkownik (tank): szeroki zamach 0,9 s rozkrusza płytę pod sobą; sprowokowanie zamachu z daleka otwiera mu plecy.
Elita Dozorca Jarów (kontrola): dwa metaliczne uderzenia 1 s zwiastują osuwisko po oznaczonej linii; zejście z linii i powrót po osuwisku dają kontrę.
Boss **Mistrz Białych Znaków** maskował rysy, by transport nie stanął: maluje bezpieczne pole tylko dla siebie, a gracz odsłania prawdziwą podporę; fazy: fałszywe znaki → pęknięcie środka → walka na odsłoniętych podporach. Ruchy: pędzel, pył, strącenie, osuwisko, skurcz; jasna puszka farby otwiera się po trafieniu w podporę. Kandydat: model własny.
WOW: w 8. minucie z windy widać całe zbocze jak biały rysunek; skurcz odsłania pod farbą ciąg czarnych pęknięć prowadzących do areny.
Paleta: kalcyt, biała terakota, dioryt, andezyt, jasny dąb, czarny żwir; ostre dzienne światło, suchy chrzęst.
Poza walką: tropy sekretów to odciski kamyków na fałszywej półce, ukryty znak pod deską i sznur pomiarowy; wybór tras ustala pozycję startową elity.
Powrót/trudności: jedna z dwóch półek ma nośność odwróconą na wejściu, widoczną w rysach; Łatwy wolniejsze osuwisko, Trudny Rysownik przemalowuje tylko raz pod presją.
Nagroda i świat: kreda, wapno, sprzęt mierniczy, prawdziwy znak nośności; stare Kryształowe Jaskinie 004 oddać Głębi.
Granica aktów/technika: żadnych magicznych kryształów/ciemności Aktu 4; półki w dwóch gotowych stanach i bezpieczny teleport po upadku, plan B: niezmienna geometria z zapowiadaną strefą zagrożenia.

### 005 — Pasieka Pod Korą (poziom 5)
Fantazja/emocja/czas: przechodzę przez olbrzymi ul i przywracam pszczołom trasę bez niszczenia kolonii; czułość pod presją, 15–17 min.
Motyw: **zapach wskazuje cel roju**; wstęp = dwa pojemniki z woskiem bez wrogów, rozwinięcie = oznaczanie jednej z dwóch tras, ZWROT = boss znakuje gracza i trzeba przenieść zapach na pusty plaster, egzamin = świadomie przełączyć pościg.
Topologia: sześciokątne plastry połączone mostami `próg→plaster centralny↔(żłób A / miodnia B)→matecznik↺próg`; wybór kolejności, boss przez półprzezroczysty wosk.
Beat: komora[n/2/V/zapach] → żłób[w/2/V/Pszczoła] → dwa plastry[n/2/V/wybór] → miodnia[w/3/Z/Woskownik+Pszczoła].
Beat: powrót[n/2/V/przełączenie] → matecznik[w/2/P/Opiekun] → ognisko[n/1/V] → boss[w/3/P]; zapach pokazuje jasny pył, nie tekst.
Pszczoła Strażnicza (pościg): lot w miejscu i brzęk 0,7 s zapowiadają żądło karzące nosiciela zapachu; zmiana plastra albo blok przecina szarżę.
Woskownik (tank): nabrzmienie pancerza 0,8 s poprzedza zalepienie przejścia; zejście z osi i atak w miękki bok kontruje.
Nosiciel Pyłku (wsparcie): wir pyłu 0,8 s oznacza nowy znacznik zapachu na podłożu; cios lub przestawienie pojemnika przerywa przekazanie.
Ćma Miodowa (dystans): matowy błysk skrzydeł 0,8 s zapowiada klejący pył; osłona plastra albo ruch poza stożek chroni.
Elita Stary Opiekun (kontrola): podnosi dymnicę 1 s przed ogłuszeniem roju i gracza; osłona wosku i przerwanie uderzeniem działają dla wszystkich.
Boss **Matka Ominiętego Ula** broni pustych komór po wbiciu kotew: nie goni najwyższych obrażeń, tylko najsilniejszy zapach; fazy: śledzenie pojemnika → znacznik na graczu → oddanie znacznika pustemu plastrowi. Ruchy: żądło, szum, woskowa ściana, znak, skurcz; jasne żądło odsłania się po błędnym pościgu. Kandydat: model własny.
WOW: w 6. minucie ze środkowego plastra widać podświetlony od tyłu ul wielkości domu, a rój układa się w kształt dawnej drogi, którą przecięła żelazna kotwa.
Paleta: miodowe bloki, żółta terakota, ciemny dąb, woskowane deski, bursztynowe szkło, mech; ciepły bursztyn, głęboki bas brzęczenia.
Poza walką: trzy sekrety wskazuje porzucony plaster, smugę pyłku i mały ul z odmiennym brzękiem; decyzja, który żłób zasilić, zmienia miejsce roju przy bossie.
Powrót/trudności: losowany czynny żłób przestawia drogę zwabienia, nie kolor; Łatwy znacznik krótszy, Trudny Opiekun potrafi raz przerzucić zapach.
Nagroda i świat: wosk, pyłek, sprzęt pszczelarza, pusty ul w atlasie; zastępuje mechaniczną Kopalnię Nadzorcy 005, której ciężki metal lepiej pasuje do Głębi.
Granica aktów/technika: to ekologia, nie przemysł Aktu 4; znacznik przez zmienną instancji i efekty cząstek, plan B: wabik jako dwa stałe cele z dźwignią.

### 006 — Most Sznurów (poziom 6)
Fantazja/emocja/czas: rozplatam bezpieczny dla ludzi, lecz duszący korzenie most nad gęstym zagajnikiem; odpowiedzialność, 15–18 min.
Motyw: **naciąg lin wybiera drogę i osłonę**; wstęp = luźna lina bez ryzyka, rozwinięcie = dwa punkty naciągu, ZWROT = poluzowanie głównej liny daje korzeniom skurcz i odbiera prosty most, egzamin = rozłożyć ciężar podczas walki.
Topologia: dwa wiszące pokłady na różnych wysokościach `przyczółek→(górny pomost / dolny kosz)→kołowrót→brama boss↺zjazd do przyczółka`; decyzja o naciągu zmienia skrót.
Beat: kotwa[n/2/V/lina] → górny pomost[w/2/V/Szarpacz] → dolny kosz[n/2/V/wybór] → kołowrót[w/3/Z/Łowca+Szarpacz].
Beat: luzowanie[n/2/V/most] → zjazd[w/2/P/Dozorca] → ognisko[n/1/V] → boss[w/3/P]; brak przepaści bez ratunku.
Szarpacz Lin (zwarcie): napina sznur 0,8 s przed pchnięciem do krawędzi; blok albo krok ku słupowi zostawia go odsłoniętego.
Łowca Kotew (dystans): metaliczny klik 0,8 s zapowiada lot haka i przyciągnięcie; słup przecina linię, a ruch boczny powoduje pudło.
Plecionek (kontrola): trzy drgania 1 s poprzedzają związanie dwóch desek; odejście ze szwu albo uderzenie w węzeł kontruje.
Noszowy (wsparcie): opuszcza kosz z krzykiem 1 s przed wzmocnieniem sojusznika; przecięcie drogi kosza ciałem lub cios zatrzymuje dostawę.
Elita Dozorca Przeprawy (tank): ciężar przenosi na jedną linę po głębokim skrzypnięciu 1 s; przejście na drugi pokład lub blok ciężkiego ciosu daje flankę.
Boss **Węźlarz**, dawny zastępca Miry, wierzy, że każdy most musi być sztywny: osłania się naprężoną liną, ale sam dociąża tę samą kotwę; fazy: sztywna droga → luźny most → naprzemienne podparcie. Ruchy: hak, ciężar, zaciąg, cięcie liny, skurcz; jasny węzeł na piersi odsłania się po przełożeniu naciągu. Kandydat: model własny.
WOW: w 7. minucie z dolnego kosza widzimy całą konstrukcję od spodu: po poluzowaniu sznurów most ugina się, a uwolnione zielone pędy niosą go przez sekundę zamiast pali.
Paleta: liny, świerk, ciemny dąb, mech, miedziane haki, beżowe płótno, zielone liście; mgła poranna, skrzypienie i krótki gong kołowrotu.
Poza walką: trzy sekrety zdradzają przecięty węzeł, stary szkic pod pomostem i zwisająca torba; wybór pokładu zmienia punkt naciągu przed bossem.
Powrót/trudności: losuje się zużyta lina A albo B, więc wartość górnej/dolnej trasy się odwraca; Łatwy szeroki pomost, Trudny jedno dodatkowe przełożenie ciężaru w fazie 3.
Nagroda i świat: włókno, żywica, sprzęt przewoźnika, plan rozpięcia lin; zastępuje Złamaną Fortecę 006, która powtarza forteczne akty wojenne.
Granica aktów/technika: nie ma frakcyjnej wojny Aktu 7; dwa warianty mostu BlockEditor, plan B: stała konstrukcja i zmieniające się kolizje/osłony na pokładach.

### 007 — Sad Zakazanych Odrostów (poziom 7)
Fantazja/emocja/czas: przywracam sad, w którym obfity plon kupiono kosztem dzikich gałęzi; żal, 14–16 min.
Motyw: **cięcie jednej gałęzi przekierowuje wzrost drugiej**; wstęp = dwie sadzonki na tablicy, rozwinięcie = wybrać żywy korytarz i plon, ZWROT = ścięta gałąź ratuje gracza jako most, a „ocalona” zasłania wyjście, egzamin = wybrać cięcie z kosztem podczas bossa.
Topologia: koncentryczne rzędy drzew `brama→zewnętrzny krąg↔dwa sady→środek boss↺kanał zbiorów`; wybór kolejności zbioru, brama bossa widoczna między drzewami.
Beat: sadzonki[n/2/V/cięcie] → rząd[w/2/V/Żniwiarz] → rozstaje[n/2/V/wybór] → sad A[w/2/Z/Zbieracz+Żniwiarz].
Beat: sad B[n/2/V/odrost] → kanał[w/2/P/Sadownik] → ognisko[n/1/V] → środek[w/3/P/boss]; owoce oznaczają zasób, nie loot do farmienia.
Żniwiarz Cierni (zwarcie): sekator unosi się 0,8 s przed cięciem po łuku; blok lub krok za pień daje atak w plecy.
Zbieracz (rój): szelest kosza 0,7 s zapowiada skok z drzewa i utratę pozycji; ruch między rzędami i szeroki cios kontrują.
Sadzonnik (wsparcie): zielony pęd rośnie 1 s przed zasłonięciem przejścia; szybki cios w pęd albo obejście drugą alejką działa zawsze.
Owocnik (dystans): owoc jaśnieje 0,8 s przed kwaśnym rzutem; pień lub ruch w poprzek osi chroni.
Elita Sadownik (kontrola): pokazuje sekator 1 s przed odcięciem podpory wybranego drzewa; wyjście z cienia korony lub przerwanie ręki pozwala uniknąć pułapki.
Boss **Szczepione Serce** było pielęgnowane dla plonu i samo zagłuszyło dzikie odrosty: jego osłonę przenosi się cięciem, ale każde cięcie zmienia układ bezpiecznych alejek; fazy: słodki plon → zarośnięty środek → uratowanie przejścia dzikim pędem. Ruchy: owoc, sekator, pęd, osłona, skurcz; biały zrost pnia świeci po cięciu. Kandydat: model własny.
WOW: w 8. minucie z kanału zbiorów widać białe kwiaty ponad sadami; po pierwszym cięciu jeden cały rząd zakwita, a drugi natychmiast gubi płatki na ziemię.
Paleta: różowe płatki, kwitnące azalie, jabłkowe liście, jasny dąb, gliniane donice, żółta słoma; łagodne popołudnie, pszczoły daleko, nożyce blisko.
Poza walką: trzy sekrety wskazują ślady beczek, odrzucone szczepy i ptasie gniazdo; wybór plonu daje inny materiał, równą moc.
Powrót/trudności: losuje się sad z chorym szczepem, zmieniając użyteczny korytarz; Łatwy odrost jest wolniejszy, Trudny boss przenosi osłonę dwukrotnie.
Nagroda i świat: sadzonka, zioła, sprzęt ogrodnika, receptura w atlasie; stara Mroźna Przełęcz 007 należy do Mroźnej Korony.
Granica aktów/technika: żadnego mrozu, wiatru ani sezonowego licznika; warianty gałęzi jako gotowe szablony, plan B: podnoszone płoty korzeniowe przy stałych drzewach.

### 008 — Dom Dymnych Sygnałów (poziom 8)
Fantazja/emocja/czas: rozróżniam dym alarmowy od dymu tłumiącego ostrzeżenia korzeni i ratuję sygnalistów; napięcie, 15–17 min.
Motyw: **ustawienie kominów wybiera, co widać**; wstęp = widoczna komora dymna, rozwinięcie = osłonięcie celu albo odsłonięcie szwu, ZWROT = boss zamyka komin i dym ukrywa jego pozycję, ale zdradza prąd po cząstkach, egzamin = czytać przepływ zamiast sylwetki.
Topologia: zamknięta ósemka wokół dwóch kominów `wejście→piec A↔galeria↔piec B→hala boss↺kanał wentylacyjny`; dwa kominy w dowolnej kolejności, arena widoczna przez kratę.
Beat: próba[n/2/V/dym] → piec A[w/2/V/Sadzownik] → galeria[n/2/V/komin] → piec B[w/3/Z/Kominiarz+Sadzownik].
Beat: zamknięcie[w/2/P/Wartownik] → kanał[n/2/V/ognisko,skrót] → hala[w/3/P/boss]; w dymie zawsze świecą krawędzie bezpiecznej drogi.
Sadzownik (zwarcie): czarne ręce zbierają się 0,8 s przed ciosem z dymu; dźwięk butów i blok albo ruch za kratę kontrują.
Kominiarz (dystans): trzy stuknięcia 0,8 s oznaczają tor sadzy, który zasłoni sygnał; komin jako osłona lub doskok chroni.
Dmuchacz (wsparcie): mieszek pęcznieje 1 s przed zmianą przepływu; przerwanie uderzeniem albo przejście na górną galerię daje kontrę.
Wartownik Kraty (tank): czerwona krata rozżarza się 0,9 s przed zamknięciem przejścia; ominięcie kominem B lub blok ataku otwiera jego bok.
Elita Głuchy Sygnalista (kontrola): podnosi dzwon 1 s przed fałszywym alarmem odwracającym patroli; czytanie dymu i przerwanie ręki rozwiązuje zmyłkę.
Boss **Główna Sygnalistka** tłumiła stuk korzeni, by ludzie nie uciekali przy każdym pulsie: walka nagradza zobaczenie prądu dymu, który zdradza drogę bossa; fazy: dwa kominy → jedno zamknięcie → odkryta hala. Ruchy: dmuchnięcie, sadza, krata, dzwon, skurcz; jasny mosiężny dzwon pozostaje widoczny w mgle. Kandydat: model własny.
WOW: w 5. minucie z górnej galerii komin wyrzuca szarą smugę nad cały zielony jar; po otwarciu drugiego komina smuga rozdziela się i odsłania dotąd niewidoczny szlak stad.
Paleta: szary tynk, węgiel drzewny, przygaszona czerwień cegły, dębowe belki, mosiądz, czarne szkło; późne popołudnie, świst i dwie różne sekwencje dzwonu.
Poza walką: sekrety wskazują czysty ślad dłoni, poprawny rytm dzwonu i gołębie pióra przy kanale; wybór otwartego komina ustala osłonę na arenie.
Powrót/trudności: losuje się zatkany komin, co zmienia bezpieczną pętlę; Łatwy rzadszy dym, Trudny Sygnalista daje jeden fałszywy dzwon przed prawdziwym.
Nagroda i świat: węgiel drzewny, mosiężny znacznik, sprzęt sygnalisty, rozróżnienie alarmów; Popielne Sanktuarium 008 zostawić wulkanicznemu Aktowi 8.
Granica aktów/technika: dym jest informacją i osłoną, bez lawy, erupcji i ognistych obrażeń; cząstki plus fog w ustalonych strefach, plan B: półprzezroczyste zasłony blokowe z kierunkową cząstką.

### 009 — Przesmyk Stad (poziom 9)
Fantazja/emocja/czas: otwieram trasę zwierzętom, których migrację cech uznał za zagrożenie dla osad; empatia, 16–18 min.
Motyw: **prowadzenie stada przez wybór dwóch bram, bez eskorty HP**; wstęp = ślady na piasku leśnym, rozwinięcie = otworzyć bramę przed falą, ZWROT = stado zawraca ku graczowi, bo „bezpieczna” brama pachnie kotwą, egzamin = otworzyć naturalny przesmyk i przeżyć wspólny szew.
Topologia: rozcięty pierścień `punkt obserwacyjny→(brama A / brama B)→przesmyk→urwisko boss↺ścieżka zwierząt`; boss widoczny jako sylwetka na urwisku, wybór bram zmienia trasę stada.
Beat: ślady[n/2/V/odczyt] → brama A[w/2/V/Czujka] → rozwidlenie[n/2/V/wybór] → brama B[w/3/Z/Łapacz+Czujka].
Beat: nawrót[w/2/P/Przewodnik] → naturalny szlak[n/2/V/ognisko,skrót] → urwisko[w/3/P/boss]; żadna fala stada nie zadaje nieczytelnych obrażeń.
Czujka (dystans): gwizd 0,8 s zapowiada strzałę prowadzącą, która kieruje stado w złą bramę; filar i szybki doskok kontrują.
Łapacz (zwarcie): sieć rozciąga się 0,8 s przed rzutem, który zatrzymuje gracza na szwie; blok lub zejście z linii rozwiązuje chwyt.
Płoszak (kontrola): trąbka rozbrzmiewa 1 s przed zawróceniem stada; przerwanie albo ustawienie się za osłoną pozwala utrzymać trasę.
Łania Szlakowa (neutralna przeszkoda): tupnięcie i jasny ślad 0,8 s poprzedzają galop przez oznaczony pas; odejście z pasa i niewalka są prawidłową kontrą.
Elita Przewodnik Kotew (tank): wbija pal 1 s przed zamknięciem bramy; przerwanie ciosami lub obejście drugim ramieniem otwiera szlak.
Boss **Dozorca Przesmyku** uważa, że chroni osadę, zamykając migrację: jego pancerz jest z dwóch bram, a osłonę traci, gdy stado przejdzie naturalnym kanałem; fazy: kontrola bram → nawrót → przejście stada. Ruchy: pal, sieć, gwizd, szarża, skurcz; jasny dzwonek przy bramie wskazuje okno. Kandydat: model własny.
WOW: w 10. minucie z punktu obserwacyjnego dolina pod graczem wypełnia się setkami świecących śladów, po czym przez otwarty przesmyk przechodzi kilka wielkich sylwetek, nie armia mobów.
Paleta: rdzawa glina, brunatne liście, świerk, piaskowiec leśny, płowa wełna, żółty mech; złota godzina, tupot i dalekie rogi.
Poza walką: tropy do sekretów to stare poroże, brakujące szczeble płotu i ślady młodego; gracz wybiera, która brama stanie się trwałym skrótem w instancji.
Powrót/trudności: losuje się kierunek wejścia stada, więc prioritet bram się odwraca; Łatwy dłuższe okno przed nawrotem, Trudny Płoszak raz zmienia kierunek.
Nagroda i świat: skóra, włókna, sprzęt przewodnika, szlak migracji w atlasie; Pustkowia Kości i Anubisa 009 oddać Złotym Piaskom, bez nieumarłych jako zła.
Granica aktów/technika: to migracja w lesie, nie pustynna nekropolia Aktu 3; stado jako sceniczne displaye poza kolizją, plan B: 6–8 modeli i ślady cząstek zamiast setek encji.

### 010 — Próg Żywej Granicy (poziom 10)
Fantazja/emocja/czas: wchodzę pod pierwszą drogę cechu, by uwolnić splot bez odebrania ludziom przejścia; odpowiedzialna ulga, 18–20 min.
Motyw: **pożyczyć drogę splotowi i oddać ją we właściwym momencie**; wstęp = widoczne dwa szwy i bezpieczna próba, rozwinięcie = w trzech komorach przełożyć nacisk na drogę zapasową, ZWROT = Mira sama przejmuje ciężar i nie wolno jej „pokonać” przez zadanie obrażeń, egzamin = zsynchronizować dwa czytelne skurcze, by uratować ją i most.
Topologia: przeplatany diament `stara droga→(osada A↑ / gaj B↓)→skrzyżowanie węzła→hala boss↺tunel powrotu`; oba cele dowolnie, brama bossa widoczna od wejścia przez korzenie.
Beat: próbny szew[n/2/V/nauka] → osada[w/2/V/Robotnik] → gaj[n/2/V/wybór] → węzeł[w/3/Z/Łącznik+Robotnik].
Beat: próg[w/2/P/Skurcznik] → tunel[n/2/V/ognisko,skrót] → hala[w/5/P/boss]; 18 min obejmuje obserwację, nie sztuczny timer DPS.
Robotnik Kotwicy (zwarcie): młot unosi się 0,8 s przed przybiciem deski blokującej szew; blok lub cios w deskę przerywa.
Łącznik (dystans): dwie miedziane iskry 0,8 s zwiastują linię między węzłami; zejście z linii albo pień jako osłona kontruje.
Skurcznik (kontrola): kora napina się 1 s przed skurczem w poprzek ścieżki; przesunięcie na drogę zapasową lub wymuszenie skurczu na wrogu działa zawsze.
Strażniczka Drogi (wsparcie): latarnia błyska 0,8 s przed utrwaleniem kotwy; doskok/przerwanie albo przełożenie nacisku na drugi szew udaremnia ją.
Elita Ostatni Palowy (tank): wbija pal po trzech stuknięciach 1 s, co zamyka jedną z dwóch dróg; obejście drugą i ciężki cios po wbiciu otwierają słaby bok.
Boss **Mira Sucha / Węzeł Próg**: Mira trzyma kotwę, a żywy splot osłania ją przed graczem — zwycięża się przez odciążenie dwóch stron, nie egzekucję. Fazy: Mira pokazuje regułę → splot zwraca skurcze i odsłania koszt → gracz przeprowadza dwa pulsy, Mira puszcza kotwę. Ruchy: młot, pal, dwie linie szwów, korowa osłona, podmuch, rozpięcie; najjaśniejszy jest pęknięty sworzeń, nie głowa Miry. Kandydat: model własny.
WOW: w 15. minucie z kładki nad halą gracz widzi pod sobą cały przekrój granicy jak koronę drzewa: po zwolnieniu sworznia złote impulsy wracają w dziewięciu kierunkach, a most pozostaje na nowym, żywym podparciu.
Paleta: dąb, korzenie mangrowca, miedziane kotwy, biały kamień drogi, zielony mech, złote żyły; świt po deszczu, najpierw metronomiczne uderzenia, potem wielogłos lasu.
Poza walką: sekrety zdradzają szkic pierwszego mostu, dziecięcy znak Miry i dawny dźwięk szwu; wybór kolejności A/B zmienia położenie osłony, finałowe świadectwo dla mieszkańców jest kosmetyczne.
Powrót/trudności: losuje się, który szew ma wyższy próg, więc kolejność pulsu zmienia plan; Łatwy dłuższe okna, Trudny Mira raz przechwytuje nacisk, lecz nadal jest solo.
Nagroda i świat: rdzeń żywego drewna, rodzina sprzętu drogi, żeton, tytuł i ukończony atlas; Brama Pierwszego Aktu zostaje jedynie funkcją finału, nie kolażem ognia, kości i kryształu.
Granica aktów/technika: finał kończy lokalny spór, nie zdradza praw Aktów 2–10; dwa liczniki szwów w zmiennych instancji, plan B: sekwencja dwóch dźwigni z tym samym widocznym sygnałem i bez wymogu idealnych ticków.

## D. Macierz różnorodności i kontrola powtórek

| # | Mechanika motywu | Topologia | Start starć dominujący | Haczyk bossa | Dominujący kolor | Emocja |
|---|---|---|---|---|---|---|
| 001 | kotwiczenie lampą | spirala z kieszeniami | gracz widzi i wybiera flankę | osłona po ciemnej stronie | jasnozielony | zachwyt |
| 002 | osuszanie studni | dwa obiegi mokradła | zasadzka z wody | mniej kryjówek = mocniejszy znany skurcz | brunatna zieleń | niepokój |
| 003 | odwracanie taśmy | pionowa pętla wału | patrol nad głową | kierunek taśmy osłania | kość słoniowa | podejrzenie |
| 004 | badanie nośności | tarasy w jarze | strzał z przeciwnej półki | farba ukrywa prawdziwą podporę | kredowa biel | zwątpienie |
| 005 | przenoszenie zapachu | plaster z ramionami | gracz wabi rój | pościg za znacznikiem | bursztyn | czułość |
| 006 | przełożenie naciągu | dwa wiszące pokłady | hak wciąga na pokład | boss dociąża własną kotwę | lniany beż | odpowiedzialność |
| 007 | cięcie i odrost | koncentryczny sad | wróg spada z korony | każde cięcie przenosi osłonę | płatkowy róż | żal |
| 008 | kierowanie dymem | wentylacyjna ósemka | przeciwnik zdradzony prądem | pozycję wskazuje dym | grafit | napięcie |
| 009 | wybór bram migracji | rozcięty pierścień | fala stad przed walką | stado rozbraja pancerz bram | rdzawa miedź | empatia |
| 010 | oddanie pożyczonej drogi | przeplatany diament z tunelem | wróg zamyka drogę zapasową | uwolnienie Miry przez dwa pulsy | złoto | ulga |

**Autokontrola:** mechaniki, topologie, dominujące starty, haczyki, dominanty i emocje są rozłączne jako *główne* doświadczenia; wspólne Prawo Odzewu jest gramatyką świata, nie powtórzeniem motywu. Uwaga do bramki: 002 i 006 zawierają kołowroty, ale pierwszy reguluje wodę/studnie, drugi nośność dwóch pokładów; 009 i 010 mają rozgałęzienie, lecz 009 jest trasą stad z jednym rozcięciem, a 010 dwoma poziomami krzyżującymi się przed bossem. W blockoucie trzeba sprawdzić, czy różnica jest odczuwalna bez podpisów.

## E. Korekta struktury całej gry 1–100

1. Zachować **10 aktów × około 10 lochów** jako czytelną skalę poziomów, rodzin łupu i prawa krainy, lecz usunąć stałe K1–K10 jako typy lochów. Numery mówią o zalecanym poziomie, nie o obowiązkowym motywie.
2. Każdy akt ma **jedno prawo świata** uczone w pierwszym lochu i rozwijane we wszystkich pozostałych; tematy lochów wynikają z mieszkańców, pracy, historii i krajobrazu konkretnej krainy. Akt 1: Odzew; Akt 2 może mieć przypływ, ale bez kopiowania śluz 002 jako głównej mechaniki.
3. Projekt aktu zaczyna się od dziesięciu emocji, dziesięciu obrazów WOW i dziesięciu różnych topologii, dopiero potem przydziela się poziomy i bosów. Macierz jak w D jest obowiązkową kontrolą, nie szablonem do wypełniania kolumn.
4. Zastąpić „komórkę” dwiema równorzędnymi ścieżkami w tej samej krainie: wspólne materiały/rodziny zagrożeń/tier, inne konkretne historie i bossowie; bez klucza ze strefy N wymagającego lochu N. Łączniki są sekretami i skrótami wiedzy, nie bramami progresu.
5. Rozdzielić progres **mocy** (poziom, równoważny sprzęt, żeton przeciw pechowi) od progresu **wiedzy** (atlas, świadectwa, opcjonalne dialogi). Dowolny loch może być pierwszą instancją odblokowaną poziomem; nie wymaga ukończenia poprzednika.
6. Ułożyć rytm w obrębie aktu jako trzy różne napięcia: 1–3 poznanie prawa i ludzi, 4–7 koszt i sprzeczne interesy, 8–10 decyzja i konsekwencje. To dramaturgia, nie powtarzany zestaw „status/pocisk/ogień/kość”.
7. W kolejnych aktach użyć **budżetu obietnic**: jeśli Akt 5 obiecuje mróz, nie zużywać mroźnej przełęczy w Akcie 1; jeśli Akt 3 obiecuje pustynne grobowce, nie stawiać Anubisa na startowej wyspie. Epizodyczne zapowiedzi innych krain są możliwe, pełny motyw trzeba zachować.
8. Przed zaakceptowaniem każdego aktu sprawdzić arkusz 10×10 dla konfliktów: rdzeń mechaniki, topologia, inicjatywa starcia, sylwetka bossa, dominanta palety, emocja, unikalny łup, udział czynności poza walką, koszt techniczny, sposób solo. Powtarza się tylko to, co jest świadomym motywem przewodnim aktu.
9. Jeden loch dopracować na ciasnym blockoucie i arenie testowej, potem przenieść wnioski do kolejnego; `00-BRIEF.md` §1 i `research/projektowanie-bossow/README.md` §8 wymagają dowodu czytelności przed modelem. Unikać mnożenia 100 prefabów z jednego szablonu.
10. Raz na akt przegląd krzyżowy: co nowy akt zabiera poprzednim, co wnosi jako *nowe* prawo, czy boss finałowy jest własną historią, a nie quizem z dziesięciu żywiołów. Ocena poniżej 7 w dowolnym kryterium briefu blokuje produkcję, z konkretną poprawką.

## F. Pytania do zespołu i ryzyka

1. **BLOKUJĘ** utrzymanie „unik” jako przycisku lub wymogu w opisach 001/007: kanon ma ruch, blok i przerwanie (`00-BRIEF.md` §1). Naprawa: wszystkie kontry przepisać na istniejące wejścia i przetestować czterema archetypami.
2. **BLOKUJĘ** finał 010 jako „boss + fale / kolaż tematów”: nie egzaminuje Prawa Odzewu ani decyzji Miry. Naprawa: dwa czytelne szwy i uwolnienie kotwy jako warunek, HP służy fazom, nie jedynemu celowi.
3. Czy Mira może pozostać przy życiu i być stałą postacią dalszej gry? Proponuję tak; porażka ma być poznawcza, nie egzekucją antagonisty.
4. Najsłabszy jest **003**: taśma może wyglądać jak mechaniczna kopalnia albo stać się kłopotliwą animacją bez wpływu na decyzje. Przed akceptacją wymagam blockoutu dwóch realnie różnych tras i fallbacku z trzema stanami.
5. Drugi najsłabszy jest **008**: dym w Minecrafcie może ograniczać czytelność i FPS, a skojarzenie z Popielną Koroną jest bliskie. Przed akceptacją wymagam testu na GUI scale 3–4, odróżnienia dwóch dzwonów i dymu jako informacji, bez ognistych obrażeń.
6. Ryzyko wspólnego prawa: 10 szwów może zmęczyć jak status globalny. W QA każdy loch musi mieć inny użytek z pulsu, a gracz uważny powinien potrafić go użyć przeciw wrogowi przynajmniej raz.
7. Ryzyko 002: istniejący klon ma problem z pathfindingiem przed Kokoniarnią (`loch-002…` §7); blokować pełną specyfikację, dopóki człowiek i bot nie przejdą skrótu i obu odnóg bez grzęźnięcia.
8. Ryzyko kosztu i performansu: 10 własnych sylwetek bossów, sceniczne stado 009 i zmiany terenu 003/006/010 wymagają najpierw blockoutu/QA; modele LittleRoom wolno dopuścić po porównaniu sylwetek, a plany B są w kartach, bez setek aktywnych mobów.
9. Pytanie o łup: potwierdzić przed implementacją, czy token i materiał „tylko tu” da się zbalansować względem wyspy bez receptury czyniącej drugą ścieżkę obowiązkową.
10. Najostrzejsza bramka przed produkcją: osobne 7+ za czytelność, zabawę solo, WOW i różnorodność na *grywalnym* blockoucie; samo ładne lore lub tabela nie jest zaliczeniem.
