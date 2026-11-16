# 0. Spichlerz Głodnego Roku — karta szczegółowa Aktu 1
- **Poziom:** 8 (wejście od 7). **Normalny solo:** 16–20 min.
- **Fantazja:** schodzę przez spichlerz, który uratował Stolicę, i decyduję, czy ułatwić sobie walkę chlebem wyhodowanym na Szczepie.
- **Emocja:** głód i wdzięczność → pokusa → wina bez łatwego uniewinnienia.
- **Motyw:** dobrowolny, nieodwracalny w tym przebiegu stos Zaszczepienia kupuje chwilową przewagę; Matka leczy się, kiedy trafi gracza, proporcjonalnie do jego stosów.
- **Prawo Aktu:** identyczny pąk tylko po poległym; każde trafienie go usuwa. Po 5 s (Ł 8/T 4), z 1 s wspólnego pęcznienia, dojrzewa w jeden skutek: **Kłos Pokusy**. Dopiero ciągłe pozostanie w polu Kłosa przez 1 s daje +10% obrażeń przez 10 s i +1 trwały stos Zaszczepienia (limit 5); przez tę sekundę widoczny znak Kłosa napełnia się, a po wyjściu z pola postęp zeruje się. Po zebraniu Kłos znika. Można go minąć bez stosu, by nie karmić Matki przy przyszłym trafieniu.
- Pąk gracza → Cierń i Płatek ochrony za zdeptanie, po osobnym teście; plan B do tego czasu: brak pąka po śmierci gracza. Limit 12 pąków na instancję, nowy wypiera najstarszy (1 s szarzenia + dźwięk); maks. 6 zabójstw/10 s.
- **Bochen nie jest pąkiem:** ma osobny bursztynowy znak i nazwę „Chleb Szczepu”; nie wyrasta z żywego gracza. W Stolicy/Nexusie taki sam bochen leży w widocznym koszu wydawanym mieszkańcom *przed* wejściem do lochu 008.

## 1. Kishōtenketsu motywu
- **Wstęp (Piekarnia):** pierwszy bochen leży obok zwykłego chleba i miski ziarna. Wejście na oznaczone pole bochna i pozostanie 1 s oznacza świadome zjedzenie: jasny bonus i ciemne piętno na ręce. Pole można ominąć bez utraty przejścia lub nagrody; żaden archetyp nie potrzebuje PPM ani zmiany slotu.
- **Rozwinięcie (silosy):** dwie trudne walki i niedobór bezpiecznych osłon czynią leczenie/bonus kuszącym; dwie z czterech receptur losują się na wejściu, więc rachunek zmienia się między przebiegami.
- **Wybór (pszenica/żyto):** dwa zsypy prowadzą do różnych kątów tej samej niższej kondygnacji; wybór określa pierwsze starcie, nie blokuje sekretów.
- **Zwrot (sala worków):** Matka po raz pierwszy wyłania się z zsypu. Po 1 s sygnału dotyka gracza za 1 obrażenie w bezpiecznej demonstracji; przy stosach Zaszczepienia jej workowe ciało pęcznieje o zielony segment, przy 0 stosów pozostaje bez zmiany. Następnie wraca do piwnicy. Nie ma żaren ani stacji zdjęcia stosów; gracz sam przecina zapowiedziany korzeń.
- **Egzamin (komora ziarna):** użycie lub odmowa chleba i Kłosów pozostaje sensowna: przy 0 stosów boss nie dostaje leczenia; przy dodatnich stosach bonus pomaga, lecz trafienie go leczy. Worki są ruchomą osłoną, nie stacją oczyszczania.
- Wskazówki bez tekstu: identyczny bochen w Stolicy, dwubarwna animacja jedzenia, pasek 0–5 nacięć przy dłoni, zielony puls leczenia Matki po trafieniu i błysk pękających korzeni po własnym ciosie. Kłos Pokusy ma złoty rdzeń i nacięcie na kłosie, odrębne od zwykłego zboża; znak napełnia się widocznie przez 1 s stania w polu.

## 2. Historia środowiskiem
- W Głodnym Roku zwykłe zboże skończyło się zimą. Darnik zaszczepił zapasy Bractwa i Spichlerz wykarmił miasto; po latach nie oznaczono, które bochny nadal pochodzą ze Szczepu.
- Na piętrach widać uczciwą pracę i koszt: miarki racji czterech ras, ręcznie naprawiane zsypy, zwykłe worki oddzielone czerwonym sznurem od zaszczepionych; nie ma oskarżenia jednej grywalnej rasy.
- **Samodzielne odkrycie:** list przewozowy przy workach z widocznym symbolem bochna z Nexusu: „Chleb Stolicy do dziś wypiekamy z ziarna Szczepu. Jadłeś go również ty.” Ten fakt nie wymaga 001–007.
- Ocalała rachmistrzyni Mira: „Zwykłe zboże czeka już w składzie za murem. Zakażone worki są tylko z tego spichlerza.”
- Po wyborze Mira: „Zapiszę, czy próbki odcięto, czy zachowano. Zapasy dla miasta pojadą zwykłym szlakiem.”
- Mapa Granicy pokazuje świadectwo per gracz, nie zmienia globalnego głodu, cen ani zasobów innych instancji. Każdy wybór ma lokalny koszt: odcięcie niszczy nasiona do badań, zachowanie wymaga dalszej kwarantanny.

## 3. Mapa ASCII i topologia
- Pięć ciasnych pięter po 20–36 bl. plus piwnica; 40-blokowy szyb jest pionowym kadrem, nie 40-blokowym pustym korytarzem. Każde zejście kończy się osłoną, przeciwnikiem lub wyborem.
```text
                              N ↑     (rzut kondygnacji; poziomy pionowo ↓)
 [F1 PIEKARNIA 28×24 / ognisko]──────[szyb Ø12, widok na kłos]
           │                                ║ winda-skrót ↑
 [F2 POMOST MIAR 30×20]──────────────────────║
           │                                ║
     [ZSYP PSZENNY]──┐  ┌──[ZSYP ŻYTNI]     ║  wybór jednego wejścia
                    ↓  ↓                    ║
           [F3 ROZDZIELNIA 32×28]───────────║──pętla do F2
                    │                       ║
           [F4 SALA WORKÓW 34×30]───────────║──winda do ogniska
                    │                       ║
           [F5 WAGA 24×26 / ognisko]─────────║
                    ↓
           [PIWNICA BOSS 36×32, kłos]
```
- Pętla jest pozioma na F2–F3: oba zsypy spotykają się w Rozdzielni, lecz boczny pomost pozwala wrócić po drugi sekret i zmienić podejście. Winda to skrót F4/F5 do F1, uruchamiany od dołu.
- Pionowość: krótkie zjazdy do siatek amortyzujących 3–5 bl. niżej, z widocznym celem i osłoną po lądowaniu; 40 bl. szybu widzi się przez kraty, nie spada nim w ciemno.
- Brama bossa: ogromny kłos konstrukcyjny na dnie widoczny od F1, rośnie w kadrze przy każdym zejściu; nie jest skutkiem pąka. Na F5 słychać Matkę i widać dwa położenia worków.
- Każde piętro ma pracę dla gracza: miary uczą racji, zsypy dają wybór kąta, Rozdzielnia uczy pokusy Kłosa, worki uczą ciężaru, Waga stawia decyzję o zapasach.

## 4. Beat chart
| Sala | Rola | Min | Skład i powód | Start starcia | Tempo | Poza walką |
|---|---|---:|---|---|---|---|
| F1 Piekarnia 28×24 | wstęp | 0–2 | bez wrogów; bochen nie jest pułapką natychmiastową | — | niskie | bochen i lista racji, ognisko |
| F2 Pomost 30×20 | lekcja | 2–4 | Młynarczyk + Zakwas: ślad mąki i leczony cel | gracz widzi kanał z góry | wysokie | szyba 40-blokowego szybu |
| Zsyp 22×20 | wybór | 4–5 | pszenica: Workowiec; żyto: Ziarnojad — inna pierwsza osłona | widok przed skokiem, lądowanie za workiem | średnie | wybór kąta, dwie wskazówki |
| F3 Rozdzielnia 32×28 | test | 5–7 | Ziarnojad + Zakwas: po poległym Kłos kusi bonusem | gracz inicjuje od zasłony | wysokie | pętla, pierwszy sekret |
| F3 boczna kieszeń 20×20 | elita opcjonalna | +1–2 | Szczurzy Król: szarpie worek w wąskim torze, bez roju | gracz widzi jasny kłos przez kratę | wysokie | relikt i skrót do F4 |
| F4 Sala worków 34×30 | zwrot | 7–10 | Młynarczyk + Workowiec: ruch worka zmienia linię | wróg uderza worek po 1,0 s | wysokie | Matka ze zsypu, 1 obrażenie i widoczny zielony segment tylko przy stosie |
| F5 Waga 24×26 | oddech/etyka | 10–12 | bez fali; 1 Ziarnojad za zamkniętą kratą jako demonstracja | brak natychmiastowego starcia | niskie | Mira, zapasy zwykłe i zaszczepione, ognisko |
| Piwnica 36×32 | egzamin | 12–16 | Matka + maks. 1 Zakwas z istniejącego worka, bez fal | widok bossa i wybór wejścia | wysokie | worki i dwa tory; boss 3–3,5 min |
| Waga po walce | rozładowanie | 16–18 | bez wrogów | — | niskie | decyzja o partii tej instancji, łup |
- Zjazd zmienia kąt starcia, nie wrzuca gracza w niewidzialną paczkę. Drużyna widzi cel przed zsypem; wyjście z instancji nie wymaga cofania z piwnicy. Opcjonalna elita dodaje najwyżej 2 min: pełna trasa ≤20 min.

## 5. Bestiariusz i liczby względem Gaj v8
- L2 dla poziomu 8: HP i obrażenia zwykłych mobów = Gaj v8 ×1,84; cel 8–15 s solo, zapowiedź ≥0,5 s, zamknięty tor, kara w zwarciu r≥3,5 bl. Obrażenia surowe do poligonu po formule obrony.
| Mob | Rola; vanilla → kandydat | HP/cios | Zapowiedź | Kara | Kontra; nieuważny / uważny |
|---|---|---|---|---|---|
| Młynarczyk | jedyny humanoid trash; husk z łopatą → model własny | 59 / 13 | `block.wool.hit` 1,0 s, zamach łopatą, obrys r=3,5 | uderzenie workiem i chmura mąki 3 s | ruch za worek/blok; 13–26 / 0–5 HP |
| Ziarnojad | ciężki; iron_golem z kłosami → model własny | 74 / 15 | `block.grass.break` 1,1 s, pas ziaren | szarża po rozsypanej linii; odzyskuje 5,5 HP na ziarnie | wejść na deski/za osłonę; 15–30 / 0–7 HP |
| Zakwas | wspierający; slime biały → własny | 48 / 9 | `block.bubble_column.bubble_pop` 1,2 s, puchnący garnek | kanał leczy sąsiada 11 HP i rozlewa lepki krąg | dowolne trafienie przerywa; długa walka / 0–4 HP |
| Workowiec | wypchnięcie; husk bez głowy + display worka → własny | 63 / 13 | `block.wool.break` 0,9 s, kurz wzdłuż toru | popycha worek i gracza w oznaczonym pasie | zejść z pasa lub blok; 13–24 / 0–5 HP |
- Kłos Pokusy z dojrzałego pąka nie jest osłoną ani mobem; oznaczony bonus i trwały koszt działają tylko po ciągłym staniu gracza w jego polu przez 1 s. Śmierć tymczasowego peta nie tworzy pąka; Chowaniec i przywołańce usuwają pąki trafieniem lub przejściem obok, nie zbierają Kłosa za gracza.
- Żaden zwykły mob nie kopiuje bagiennej trucizny ani oranżeryjnego zrostu; presja tu wynika z linii worków i decyzji o chlebie.

## 6. Elita i boss
- **Szczurzy Król — funkcja:** sprawdza pozycję przy zasypywanym wyjściu i konsekwencję worków; to jedyny szczurzy przeciwnik w lochu, bez roju trashu.
- **Haczyk:** ciągnie pojedynczy worek do szczeliny i otwiera z niego tor pędu; gracz zamyka tor przesuniętym workiem albo świadomie zatrzymuje się na 1 s w Kłosie Pokusy po poległym.
- **Historia/wygląd:** stare zwierzę spichlerza połączone z lnianymi sznurkami; masywny tułów, jeden jasny kłos na grzbiecie jako punkt skupienia. Vanilla ravager zmniejszony → model własny; `rotter` istnieje, ale nie pasuje do szczura bez przebudowy.
- **Parametry:** 313 HP (25% z 1250 po zaokrągleniu), ugryzienie 15; `entity.ravager.roar` 1,0 s zapowiada linię pędu w zamknięty punkt, po uderzeniu w worek 2 s odsłony. Elita opcjonalna w F3; pełna trasa ≤20 min.
- **Matka Zasiewu — funkcja:** test budżetu ryzyka: świadomy bonus kontra leczenie bossa za własne błędy, a nie kara za odmowę chleba.
- **Haczyk:** każde skuteczne trafienie gracza leczy ją o **1,5% maks. HP × stos** (0–5): przy 1250 HP to 18,75–93,75 HP za trafienie. Kłos Pokusy lub chleb podnosi stos; przesunięcie worka zmienia osłonę i trasę Matki.
- **Historia/wygląd:** dawna gospodyni, która rozdzielała racje wszystkim czterem rasom i sama jadła ostatnia; Szczep wypełnił jej worek ciała ziarnem. Sylwetka szeroka jak snop z jedną ogromną dłonią do wydawania, biały znak miary na brzuchu to najjaśniejszy słaby punkt. Vanilla iron_golem z workami → model własny; żadna nazwa paczki nie udaje gotowej Matki.
- **8 taktów emocji:** wdzięczność → podejrzenie → odczytanie miary → pokusa → rachunek za chleb → skupienie → opróżnienie worka → żal. Po porażce ziarno wysypuje się, widać starą miarkę zamiast krwawej śmierci.
- **Fazy:** 100–65% Wydawanie (linia worka, miara); 65–30% Głód (ruchome osłony i leczenie za trafienie); <30% Przesyt (konstrukcyjny kłos na środku pęka, pasy ziarna się skracają, okno jasnego brzucha rośnie).
- **HP startowe:** 1250 solo Normalny (L2: 6,5 efektywnego DPS × ok. 192 s), brak stałej regeneracji i pełnej nietykalności. Maks. 1 Zakwas z oznaczonego worka; zabity zostawia pąk, po 5 s Kłos Pokusy zamiast kolejnego Zakwasu.
| Zdolność | Zapowiedź, czas | Cel zamknięty? | Kara surowa | Kontra / odsłona |
|---|---|---|---:|---|
| Wydanie racji | `block.barrel.open` 0,9 s, biała linia miary | tak, punkt i linia | 9 | zejść z linii/blok; 1,5 s dłoń nisko |
| Rzut workiem | `block.wool.place` 1,1 s, cień prostokąta | tak, pole 3×3 | 10 + przesunięcie worka | zejść z pola; worek zostaje osłoną, 2 s powrotu |
| Zsyp ziarna | `block.sand.fall` 1,2 s, pas z góry | tak, pas | 7 + wolny marsz 2 s | zmienić pas; 1 s jasny brzuch |
| Głodny chwyt | `entity.iron_golem.attack` 0,8 s, ramiona r=3,5 | środek boss | 9 i leczenie 1,5% max HP × stos | wyjść poza r/blok; 1,6 s odsłony |
| Przesunięcie ciężaru | `block.piston.extend` 1,0 s, strzałka na torze | wybrany worek | 7 przy torze | ruch za drugi worek; po ruchu 1,5 s bez ataku |
| Korzeń Zaszczepienia | `block.rooted_dirt.place` 1,0 s, czerwone pęknięcia przy nogach | na gracza z ≥3 stosami | 4 + spowolnienie i przytrzymanie ≤1 s | własne dowolne trafienie w widoczny korzeń (2 trafienia); przez 2 s po korzeniu Matka nie celuje w tego gracza |
| Pęknięcie kłosa | `block.grass.break` 1,3 s, dwa jasne pasy | tak, dwa pasy | 9 | stanąć w szczelinie/blok; 2 s brzuch jasny |
- **Arena 36×32:** dwa worki na torach po 6 bl.; dowolne trafienie worka przez gracza przesuwa go do sąsiedniego ogranicznika, atak Matki robi to samo. W obu położeniach zostaje bezpieczna ścieżka ≥3 bl.; żaden worek nie blokuje wyjścia ani bramy.
- Po przesunięciu worka zmieniają się dwie osłony i trasa Matki: bliższa linia Wydania racji albo dłuższa droga do gracza. To wybór w ruchu, bez przełączania maszyn.
- **Anty-kitowanie:** jeśli gracz utrzymuje >8 bl. przez 4 s, Matka zapowiada Rzut workiem na zamknięty punkt, potem idzie krótszą otwartą drogą; gracz dostaje po chybieniu 2 s na trafienie, więc ruch nie jest karany automatycznie.
- **Chleb — 2 z 4 na wejściu, maks. 5 stosów:** Siły +15% obrażeń/60 s za 1 stos; Żywota +8 surowego HP natychmiast za 1; Pędu +12% ruchu/60 s za 2; Tarczy +8 osłony/60 s za 2. Każdy typ ma dość porcji, by wraz z Kłosami dało się osiągnąć 5 stosów przy dowolnej parze. Nadmiar ponad 5 nie daje bonusu ani nie zużywa porcji; stosy trwają do wyjścia i nie zmywają się przy śmierci.
- Przy 3+ stosach Korzeń co 12 s tylko po zapowiedzi, do przecięcia własnym atakiem; przy 5 co 8 s. To osobny skutek Zaszczepienia, bez pąków na żywym graczu. Jedzenie jest dobrowolne; na 0 stosach ruch Korzenia nie występuje.
| R2 solo bez specjalizacji | Szczurzy Król | Matka i korzeń |
|---|---|---|
| Wojownik | blok pędu, cios po chybieniu; Kłos wedle ryzyka | blok chwytu, ruch za worek; własny cios rozcina korzeń; bez stosów pełen dostęp do brzucha |
| Ninja | zejście z linii, seria po chybieniu; Kłos opcjonalny | porusza worek, trafia po rzucie; własny cios rozcina korzeń bez specjalnego uniku |
| Mag | zaklęcie w odsłonięty grzbiet, ruch za worek | zaklęcie przesuwa worek i tnie korzeń; brzuch dostępny z zasięgu, PPM nie jest wymagane |
| Akolita | własny cios lub przywołaniec w grzbiet, blok pędu | własny cios/zaklęcie/przywołaniec tnie korzeń; Chowaniec przesuwa worek trafieniem |
- **Budżet bez chleba i Kłosów, plan poligonu 4×solo:** 0 stosów = 0 leczenia, 0 Korzeni, 1250 HP. L1 daje 6,5 efektywnego DPS na poziomie 8, czyli ~192 s uważnej walki; testować osobno Wojownika, Ninję, Maga i Akolitę, także dostęp do worków/odsłon bez specjalizacji. Uczta z 5 stosami kupuje DPS, ale każde trafienie odnawia Matce 93,75 HP (~14,4 s dodatkowej pracy przy DPS_ref). To hipoteza, nie zaliczony test.

## 7. WOW
- **Minuta 1:** z bezpiecznego progu F1 gracz patrzy przez Ø12 szybu 40 bl. w dół: spiralne pomosty i setki worków obrastają konstrukcyjny kłos wielkości drzewa, światło wpada pięcioma szczelinami i zatrzymuje się na pyle mąki. To głębia pionowa z L6, odrębna od Kłosa Pokusy.
- Ten sam kłos jest małą jasną kreską przy pierwszym spojrzeniu i ogromnym, pulsującym zagrożeniem na F5; kadr opowiada zejście bez pustego marszu.
- Bryła i worki wypieczone offline, `place template` dla stałych pomostów, oszczędne item_display tylko dla pyłu blisko progu; odgłos zsypu `block.sand.fall` przenosi się z dołu.

## 8. Paleta
- Dominanty: dark_oak_planks, spruce_trapdoor, barrel, hay_block, wheat, white_wool, stripped_spruce_log, calcite.
- Sepia z białą mąką i czerwoną nicią tylko na zakażonych workach; późne popołudnie, boczne snopy światła, pył nie zasłania telegraphów.
- Skrzypienie zsypów, miarowe przesypywanie ziarna, dalekie koło żaren jako dźwięk historii; żarna nie są interaktywną stacją oczyszczania.

## 9. Sekrety i wybór
| Sekret | Wskazówka w świecie | Skutek |
|---|---|---|
| Miary czterech domów | cztery znaki ras na miarce F2 | zapis o wspólnym racjonowaniu, materiał |
| Suchy zsyp | drugi tor jest oznaczony wysypaną mąką od F2 | skrót do schowka, EXP |
| Zwykłe ziarno | czerwony sznur rozdziela dwa składy na F4 | dowód zapasów zastępczych, kosmetyka |
| Stara łyżka Matki | trzykrotny stuk łyżki słychać pod podłogą Wagi | tylko-tu relikt do profesji |
- **Wybór po bossie:** odciąć skażoną partię tej instancji (próbki giną, Mira wpisuje „kwarantanna zakończona”) albo zachować ją pod plombą (Mira wpisuje „wymaga dozoru”). Zwykłe zboże już zabezpieczono; żadna opcja nie powoduje globalnego głodu ani nie daje lepszego bojowego łupu.
- Główny wybór bojowy to ilość zjedzonego chleba; decyzja po bossie jest świadectwem, nie testem moralnym premiowanym mocą.

## 10. Powtarzalność i ocena
- Losowanie 2 z 4 bochenków zmienia rachunek ryzyka: para Siła/Żywot wspiera krótką walkę, para Pęd/Tarcza ułatwia ruch za workami, inne pary wymagają innego wykorzystania okien. Wszystkie warianty można przejść bez chleba.
- Jedno z dwóch położeń początkowych worków jest losowane i pokazane z F5; zmienia bezpieczną stronę pierwszego chwytu Matki, nie wymaga zgadywania.
- S: bez śmierci, ≥2 sekrety i jedna z dróg: **Post** (0 stosów, boss bez leczenia; przebiegnięcie przez Kłos bez zatrzymania na 1 s zachowuje Post) albo **Uczta** (5 stosów, boss ≤240 s). A: bez śmierci; B: jedna śmierć; C: więcej. Bazowe L1/L2 to ~192 s; limit Uczty zostawia 48 s na Korzenie nawet przy parze chlebów bez bonusu obrażeń. S daje wygląd worka, nie moc.

## 11. Nagroda
- Normalny pierwsze przejście: 960 EXP na postać (L3: 384 moby/elita, 384 boss, 192 sekrety); powtórka 400. Łatwy ×0,7, Trudny ×1,3; identyczna pula i bojowy łup dla 0–5 stosów oraz obu decyzji.
- Rodzina **Racje Głodnego Roku**: Pas Wagi, Drewniana Miarka, Płaszcz Mąki (nazwy robocze), z różnymi profilami obrony/ruchu bez premii za zjedzenie chleba.
- Materiały profesji: Ziarno Szczepu pod plombą, Lniana Taśma, Suchy Zakwas; tylko-tu Stara Łyżka Matki i świadectwo partii w Mapie Granicy.
- Sekrety: materiał + tytuł „Rachmistrz Racji”; cała drużyna dostaje EXP i nagrodę bossa, osobiste świadectwo zapisuje się każdemu według jego wyboru.
- Mapa ujawnia, że chleb w Nexusie ma pochodzenie ze Szczepu; nie wymaga ukończenia innych lochów, nie steruje cenami ani głodem świata.

## 12. Trudności
- Łatwy: pąk 8 s; Korzeń przy 3+ stosach co 16 s, hitbox 2,5 bl.; leczenie 1,5% max HP × stos; worki bez odrzutu.
- Normalny: pąk 5 s; Korzeń 12 s/8 s dla 3–4/5 stosów; leczenie 1,5% max HP × stos; liczby tabel.
- Trudny: pąk 4 s; Korzeń 10 s/7 s; leczenie 1,5% max HP × stos, zapowiedź ≥1 s; drugi worek startuje dalej od bezpiecznego kąta. Bez stosów pełna kontra i droga Post do S.

## 13. Wykonanie i plany B
- **Trudność 1 — licznik Zaszczepienia:** scoreboard per gracz w instancji, wartości 0–5; jedzenie bochna lub ciągłe stanie 1 s w Kłosie dodaje stos raz, reset licznika dopiero po wyjściu z instancji. Postęp napełniania Kłosa zeruje się natychmiast po opuszczeniu jego pola i nie dodaje stosu. Warunki odczytują `score` na celu; nie używać `caster.*`. Plan B: pięć tagów z osobnymi skillami i czyszczeniem przy wyjściu; sprawdzić trwałość po śmierci.
- **Trudność 2 — leczenie Matki proporcjonalne do stosów:** synchroniczny skill po skutecznym trafieniu, warianty 0/18,75/37,5/56,25/75/93,75 HP przy 1250 max HP; warunek `score` trafionego gracza w `TargetConditions`. Plan B: pluginowy mechanizm z logowanym wynikiem; nigdy async aura z `mmoCanTarget`.
- **Trudność 3 — dwa ruchome worki:** pełne bloki na dwóch ograniczonych torach, cztery gotowe stany BlockEditor/MD. Plan B: 2 warianty areny pieczone offline, zmiana w przerwie 1,0 s ataku Matki, widoczna strzałka; gracz przesuwa dowolnym trafieniem.
- Pąki: `~onDeath` → HP1, własny `delay`, pęcznienie i usychanie; limit instancji 12. `TriggerDistance` wykrywa obecność gracza w polu Kłosa, a ciągły licznik 1 s uruchamia zebranie; `summon level=1`, `Despawn: persistent`; żadnych opóźnień w funkcji MD. Elementy ciężaru `place template` ze znanym rogiem i wczytanymi chunkami.
- Nexo: znaki bochnów i lista racji z ikoną ≤256 px, `item_model` w `nexo:` i glif z fontem `minecraft:default` jeśli trafi do GUI. ModelEngine tylko po akceptacji sylwetki; vanilla działa jako zastępczy.
- Szczyt: boss 1 + Zakwas 1 + worki z hitboxami 4 + Kłosy/pąki 8 + 4 display + kontroler 2 = 20 encji; limit 24. Wypieczone worki są blokami.

## 14. Samokontrola
- §4 briefu: każda kondygnacja ma widoczne działanie; 40-blokowy szyb to kadr; brak obowiązkowego chleba i stacji oczyszczania; brak „boss + 3 fale”; sekrety mają trop; losowanie 2/4 zmienia walkę.
- R1: pąk tylko po poległym, stały czas, każde trafienie usuwa, jeden skutek Kłos Pokusy, kolejka wypiera najstarszy; Zaszczepienie ma własny sygnał. R2: bez stosów cztery kontry i budżet L1; R3: nieodwracalne stosy, samodzielne cięcie korzenia, leczenie procentowe, worki, świadectwo per gracz.
- Samoocena 0–10: fun 8; motyw/zwrot 9; czytelność 8; topologia 8; WOW 8; historia 9; powtarzalność 8; nagroda 8; wykonalność 7; różnorodność 9.
- **Ryzyko do bramki:** efekty chleba i leczenie po trafieniu trzeba zmierzyć na czterech solowych klasach przy ustalonej formule obrony; tabela w §6 jest planem pomiaru, nie certyfikatem. Brak tej próby wstrzymuje wdrożenie, nie ukończenie projektu karty.

## 15. Odpowiedź na recenzję (etap 5)
| Poprawka | Co zmieniono (sekcja) / odrzucono + dowód |
|---|---|
| C 008.1 | Leczenie 1,5% maks. HP × stos (0–5); Korzeń spowalnia, trzyma ≤1 s, przez 2 s Matka nie celuje w gracza (§6, §12–13). |
| C 008.2 | Matka wychodzi z zsypu w F4; bezpieczne trafienie za 1 obrażenie pokazuje zielony segment tylko przy stosie (§1, §4). |
| C 008.3 | Dwie drogi do S: Post lub Uczta z 5 stosami i limitem 240 s (§10). |
| C 008.4 | Stosy 0–5 w całej karcie; licznik scoreboard lub tagi, bez `caster.*` (§0, §6, §12–13). |
| C przekrojowe P1/P4, L5 | Pąk dojrzewa w Kłos Pokusy: +10% obrażeń na 10 s i +1 stos po 1 s ciągłego stania w polu, bez osłony; Młynarczyk używa `block.wool.hit` (§0, §4–6). |
| C przekrojowe P7, P7 prowadzącego, L1–L4 | Matka 1250 HP, elita 313 HP, moby ×1,84; 960 EXP pierwsze/400 powtórka; boss 3–3,5 min, pełna trasa ≤20 (§4–6, §11). |
| R1/R2, L6–L7 | Pąk tylko po poległym, stały czas, trafienie usuwa, jeden skutek, najstarszy wypierany; tabela czterech klas; WOW = głębia szybu; leczenie bossa pozostaje zgodnie z L7 (§0, §6–7, §14). |
| Odrzucono C P1: Wołek z pąka | Wiążący późniejszy rejestr L5 przypisuje 008 Kłos Pokusy, kategorię pokusy, a nie moba Wołka; osłona-kłos również została usunięta (§0, §5–6). |
| Odrzucono C 008.3: boss <90 s przy Uczcie | L1 daje 6,5 efektywnego DPS na poz. 8, L2 wymaga 1250 HP: 1250/6,5 ≈192 s bez leczenia. Nawet pięć porcji Siły (+75% DPS) daje ~110 s; Kłos +10% tylko na 10 s oszczędza <1 s, więc 90 s jest sprzeczne ze skalą L1/L2. Próg 240 s zachowuje drogę Uczty (§6, §10). |
| Recenzja 2 C — W8 | Kłos daje bonus i stos dopiero po 1 s ciągłego stania w polu; znak widocznie się napełnia, wyjście zeruje postęp. Samo przebiegnięcie zachowuje 0 stosów i drogę Post do S (§0–1, §5–6, §10, §13). |
