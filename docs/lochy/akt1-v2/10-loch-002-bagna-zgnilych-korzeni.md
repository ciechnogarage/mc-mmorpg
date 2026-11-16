# 0. Bagna Zgniłych Korzeni — karta szczegółowa Aktu 1
- **Poziom:** 2 (wejście od 1). **Normalny solo:** 14–18 min; cel bez sekretów 12–14 min.
- **Fantazja:** otwieram stare śluzy rybaków, odbieram bagnu kryjówki i schodzę po ślad dawnej strażniczki.
- **Emocja:** duszność → koszt pozornej ulgi → oddech po jednym czystym dźwięku.
- **Motyw:** poziom wody zmienia trasę i zestaw ataków Topielicy; osuszenie daje przewidywalność, ale skupia siłę w jednym skurczu.
- **Prawo Aktu:** identyczny pąk tylko po poległym; dowolne trafienie usuwa go. Po 5 s (Ł 8/T 4), z 1 s wspólnego pęcznienia, dojrzewa w jeden skutek: bąbel zgnilizny, chmura trucizny r=2 przez 3 s. Chmura truje też Pijawki, Mułowca i Topielicę; zabity wróg może zostawić użyteczną pułapkę.
- Pąk zmarłego gracza → Cierń i Płatek ochrony za zdeptanie; do czasu osobnego testu śmierci gracza plan B: bez tego pąka, bez karania kolejnej próby.
- Limit 12 pąków/instancję, trzynasty wypiera najstarszy po 1 s szarzenia i dźwięku; w żadnym starciu nie ma >6 zabójstw/10 s.

## 1. Kishōtenketsu motywu
- **Wstęp (Przystań/Kładki):** martwa ryba w zielonej wodzie, wyraźna linia zatrucia, suche deski; trzy Pijawki wskakują na kładkę. Gracz widzi kontrę, zanim musi ją stosować.
- **Rozwinięcie (Tama/Rozstaj):** kołowrót I opuszcza wodę o 3 bloki, odkrywa barkę i dno do przejścia; ryte na murze znaki trzech studni wskazują związek śluza → arena.
- **Wybór:** II jest opcjonalna; Kokoniarnia → uwolnienie Borka → korba → III to obowiązkowa droga do bossa. Na wejściu widać, czy uszkodzona jest II, czy III; można jednak wybrać kolejność II/III.
- **Zwrot (Grobla/Kokoniarnia):** III zawsze uwalnia wodę i zalewa istniejącą Groblę, nawet gdy jej mechanizm zamknięcia studni C jest uszkodzony. Biały znacznik rybaków na dnie prowadzi suchą trasą przez Kokoniarnię do ogniska i bramy bossa.
- **Egzamin (Topiel):** D zawsze czynna; I zamyka A, sprawna II może zamknąć B, sprawna III może zamknąć C. Uszkodzona śluza daje sekret zamiast zamknięcia. Losowany stan zmienia liczbę i układ czynnych studni, a więc rytm wynurzeń i wybór osłon.
- **Prawo Kiełka:** w pierwszym starciu pąk dojrzewa daleko od desek, w Kokoniarni można zwabić Pijawkę w chmurę, na bossie zostawić bąbel przy czynnej studni i zatruć Topielicę albo usunąć go, by zachować suchą trasę.

## 2. Historia środowiskiem
- Rybacy i Bractwo Ogrodników zbudowali trzy śluzy, by zatrzymać Szczep w stojącej wodzie; znaki Bractwa na zamkniętych skrzyniach, kołowrotach i pieczęci barki łączą to miejsce z aktem.
- Topielica była strażniczką śluz: szeroki pas służbowy, klucze wbite w włosy; próbowała utrzymać wszystkie studnie czynne, aż sama zrosła się z odpływem.
- **Samodzielne odkrycie:** otwarta księga przy Dzwonie i Borkowa kwestia pokazują, że śluzy miały *utopić* Szczep, a nie chronić go; Pieczęć Pasieki na barce wskazuje następny ślad, bez wieży Archiwum.
- Wela: „Borek ruszył do śluzy na południe od Kokoniarni. Ślad urywa się przy sieciach.”
- Borek: „Te śluzy miały zdusić Szczep pod wodą. Strażniczka nie pozwoliła ich otworzyć.”
- Po bossie Wela: „Pieczęć Pasieki była na skrzyniach z sadzonkami. Ktoś je stąd wywiózł.”
- Kronika Mapy Granicy zapisuje ten fakt pod numerem 002, niezależnie od kolejności lochów; poziom i łup nie zależą od odkrycia.

## 3. Mapa i zbudowana geometria
- **Stan:** v0.2 z klonu `mc-l002`, sonda 31/31 dwukrotnie; projekt nie przenosi sal, kładek, tamy, trzech śluz, czterech studni, Borka, Weli ani ognisk. World1 jeszcze nie ma wdrożenia.
- Układ ma ~180×250 bl., y=102 ląd/kładki, y=100 woda, trasa główna ~260 bl.; współrzędne poniżej są z istniejącej specyfikacji, nie są nowym blockoutem.
```text
                         N ↑  (skala: odległości z koordynatów, nie kratka 1:1)
       [TOPIEL r22 (0,-92), D zawsze]  ← weenie: ramię bagna widać z Grobli
                    │
       [GROBLA (22,-66) ognisko] ← zalew po III; dno ↙ Kokoniarnia
                    │                                  │
   [ŚLUZA II (-32,-6)] ← [ROZSTAJ (0,0)] → [CHATA (34,-8)]
            │               │                         ↓
     zachodni basen          │               [ŚLUZA III (66,-8)]
            └───────────────┼───────────────[KOKONIARNIA (54,-44), Borek]
                            │                            │
                  [TAMA (0,38), I] ──────────────────────┘ (pętla dnem)
                            │
             [WYSEPKA (0,52); Bąk (14,68)]
                            │
                    [KŁADKI (-28,62)]
                            │
              [PRZYSTAŃ (-56,78) ognisko/Wela]
```
- Sala to odcinek taktyczny 20–40 bl. w większym wypieczonym terenie; wolne połacie bagna są nieprzechodnim widokiem przez trzciny i wodę, nie marszem bez celu.
- Pionowość jest już w poziomie lustra: kładka y=102, odsłonięte dna po obniżeniu wody ok. 3 bl.; zejścia prowadzą po istniejących brzegach, bez skoku w pustkę.
- Skrót po II: suche dno zachodniego basenu łączy Rozstaj z Tamą. Przejście Tama → Kokoniarnia jest zawsze dostępne; wyjście na III blokuje korba Borka. Po III dno przy Kokoniarni prowadzi do ogniska na Grobli i obowiązkowej bramy. Jeśli kolizja wypieku obali połączenie, dołożyć krótki most na tym brzegu.
- Weenie od początku trasy: ramię bagna nad studniami widać przez prześwit martwych drzew na Wysepce; z Grobli widać układ czterech studni, bez wejścia na arenę.

## 4. Beat chart
| Sala/odcinek (wymiar taktyczny) | Rola | Min | Skład i powód | Start starcia | Tempo | Poza walką |
|---|---|---:|---|---|---|---|
| Przystań 24×24 | nauka | 0–1 | 3 Pijawki od beczek: ruch z wody ku deską | wróg widoczny, gracz inicjuje | niskie | Wela, ognisko, znaki śluz |
| Kładki 5 szer., 30 dł. | test | 1–2 | 2 Pijawki spod obu boków: trzymać suchy środek | wróg zaskakuje po plusku 0,7 s | wysokie | ślad chorej ryby |
| Wysepka 28×26 | osłona | 2–3 | Bąk z wysepki (14,68): pnie przecinają tor kolca | widok i dojście osłoną | niskie | trop barki pod wodą |
| Tama 20×20 | WOW/test | 3–4 | Mułowiec + 2 Pijawki: ślad zabiera brzeg | gracz widzi parę przed kołowrotem | wysokie | I, wrak, opadanie wody |
| Rozstaj 24×24 | wybór/oddech | 4–5 | bez fali; oznaczenie uszkodzonej śluzy | brak | niskie | ognisko; II opcjonalna |
| Chata 20×24 | opcjonalny priorytet | +1 | Szeptucha + Mułowiec: wywar chroni ślad | gracz zaskakuje kanał | wysokie | kocioł sekretu |
| Zachodni basen 28×30 | opcjonalny test | +1 | Bąk + 3 Pijawki: kolce nad wodą | wróg widzi z kępy | wysokie | II, skrzynia, skrót |
| Kokoniarnia 30×32 | obowiązkowa elita | 5–8 | Kokoniara + 2 kokony Pijawek: ochrona Borka | wejście w widoczną sieć | wysokie | Borek, korba, losowana strona kokonu |
| Wschodnia śluza 24×24 | obowiązkowy zwrot | 8–10 | Mułowiec + Szeptucha: odbicie poznanej pary | gracz zaskakuje z suchego brzegu | średnie | III, Dzwon, zalanie Grobli |
| Dno / Grobla 30×26 | droga zwrotna | 10–12 | 1 Bąk przy znaku dna: presja orientacji | ptak zapowiada z bezpiecznej półki | niskie | ścieżka do ogniska, brama |
| Topiel Ø44 | egzamin | 12–15 | Topielica + 1 Pijawka obecna od początku; jej śmierć daje jedyny pąk starcia, Skrzek składa jaja bez fal | widok + próg bez powrotu do końca próby | wysokie | wybór kąta, woda/studnie; boss ~2,5 min |
| Barka po walce 20×18 | rozładowanie | 15–17 | bez wrogów | — | niskie | Pieczęć Pasieki, łup |
- Trasa z obiema odnogami i sekretami trwa najwyżej 20 min; Kokoniarni, III i zalania Grobli nie da się ominąć.

## 5. Bestiariusz i liczby do poligonu
- L2 dla poziomu 2: HP i obrażenia zwykłych mobów = Gaj v8 ×1,12; cel zwykłego starcia 8–15 s. Surowe obrażenia wymagają poligonu po ustaleniu formuły obrony MythicLib.
- Każde atakujące pole po zapowiedzi zamyka punkt/tor; w zwarciu zasięg wokół moba wynosi min. 3,5 bl. Celem QA jest kara ≥3,5 bl. i wyraźna różnica nieuważny/uważny.
| Mob | Rola; vanilla → kandydat | HP; cios surowy | Zapowiedź ≥0,5 s | Kara za ignorowanie | Kontra; nieuważny / uważny |
|---|---|---|---|---|---|
| Pijawka | rój; baby husk z oddechem pod wodą → własny | 14; 3,5 | `entity.slime.squish` 0,6 s, bąble | skok na zamknięty punkt i trucizna 2 s | krok na deskę/cios obszarowy; 2 ukąszenia / 0–1 |
| Mułowiec | napór; drowned → własny | 45; 7 | `block.mud.hit` 1,0 s, ciemny łuk r=3,5 | przysiad i uderzenie, ślad spowalnia 2 s | ruch po łuku/blok; 7–14 HP / 0–3 |
| Bąk | dystans; kurczak z ukrytym ciałem/display → własny ptak | 29; 8+4,5 | `entity.parrot.ambient` 1,1 s, dziób na punkt | dwa kolce w ten sam zamknięty punkt | pień/wyjście z linii; 8–13 HP / 0–3 |
| Szeptucha | zaklinacz, jedyny humanoid trash; witch → `lrd_witch` do przeróbki | 36; 5,5 | `block.brewing_stand.brew` 1,2 s, zielona fiolka | Wywar osłania sojusznika na 4 s, rzut w ziemię truje | dowolne trafienie przerywa kanał; dłuższe starcie / 0–4 HP |
- Bąk zastępuje Kościeja/Trzcinnika; nie kodować go jako szkieletu ani przedstawiciela grywalnych Nieumarłych.
- Pijawki w v0.2 miały 1 obrażenie; 3,5 jest propozycją L2 do poligonu, nie twierdzeniem o stanie klonu. Geometria pozostaje ta sama.

## 6. Elita i boss
- **Kokoniara — funkcja:** sprawdza odzyskanie ruchu i ochronę cywila w ciasnej izbie.
- **Haczyk:** każdy kokon, który zostawi, staje się osłoną dla niej i przeszkodą dla gracza; dowolne 3 trafienia go rozrywają, każde źródło liczy się jednakowo.
- **Historia/wygląd:** pająk, który wyściełał rybacką sieć włóknem Szczepu; szeroki odwłok z jednym białym szwem jako najjaśniejszy słaby punkt. Vanilla cave_spider powiększony → model własny; model `voras` istnieje, ale pajęczy motyw grobu nie pasuje bez przebudowy.
- **Parametry:** 131 HP (25% z 525 po zaokrągleniu), skok 9 surowych, kokon unieruchamia najwyżej 4 s; po 50% przestawia dwa kokony zamiast przyzywać falę. Kokon Borka losuje wąską izbę albo szeroki pomost i zmienia drogę ratunku. Zapowiedź `entity.spider.ambient` 1,0 s; skok w zapisany punkt, 2 s odsłony po chybieniu.
- **Topielica — funkcja:** egzamin decyzji o wodzie, ruchu po suchym i kolejności celów.
- **Haczyk:** każda zamknięta studnia odbiera jedno wynurzenie, lecz wzmacnia czytelny, pojedynczy skurcz pozostałej wody.
- **Historia/wygląd:** dawna Strażniczka Śluz zaplątana w przepust; dominanta to trójkąt włosów spływających w studnię, nie czaszka potwora. Białe mosiężne klucze na piersi są najjaśniejszym punktem i oknem trafienia. Vanilla drowned ×2,4 → model własny; `lrd_witch` istnieje tylko jako kandydat bryły, nie gotowa Topielica.
- **8 taktów emocji:** ślad → wstręt → odczytanie → kontrola → utrata brzegu → skupienie → uwolnienie → oddech; finał pęka klamrą śluzy, nie egzekucją grywalnej rasy.
- **Fazy:** 100–60% „Straż”; 60–25% „Skrzek” — jaja o innym modelu i dźwięku pękają w chmurę przy czynnej studni, bez pąka; <25% „Ostatni skurcz”, zmiana pasa suchego przez gotowe stany BlockEditor.
- **HP startowe do prób:** 525 solo Normalny (L2; 3,5 efektywnego DPS ×150 s); bez stałej regeneracji. Obrona: przez 1,5 s po trafieniu chowa korpus za przepust, zostawia odsłonięte klucze; każde źródło obrażeń może je trafić.
| Zdolność | Zapowiedź i czas | Cel zamknięty? | Kara surowa | Kontra / odsłona |
|---|---|---|---:|---|
| Włosy Topieli | `block.hanging_roots.break`, linia 1,0 s | tak, kierunek | 7 + przyciągnięcie | krok za osłonę; włosy zostają 1,5 s |
| Kałuża | `entity.witch.throw`, krąg 1,2 s | tak, środek | 5 + trucizna 3 s | zejść z koła; 1 s wolnego powrotu |
| Zanurzenie | `entity.drowned.ambient_water`, bąble 1,5 s | studnia wybrana przy starcie | 8 w r=3 | odejść od bulgoczącej studni; 2 s klucze odkryte |
| Skurcz odpływu | `block.bubble_column.whirlpool_inside`, pas 1,4 s | tak, pas od studni | 6 + odrzut | wyjść z pasa lub blok; 1,5 s odsłony |
| Skrzek z jaj | `entity.turtle.egg_crack`, 1,0 s, brunatna kiść | jedna czynna studnia wskazana przed złożeniem | kwaśna chmura r=2 przez 3 s, max raz na 20 s | odejść od kiści; jaja nie są pąkiem i nie dojrzewają w bąbel |
| Ostatnie tąpnięcie | `block.mud.break`, 1,3 s, pierścień r=3,5 | środek boss | 9 | wyjść poza promień/blok; 2 s klucze jasne |
- **Koszt osuszania:** I zamyka A; II zamyka B tylko jeśli sprawna, III zamyka C tylko jeśli sprawna. Czynne 3/2/1 studnie → wynurzenie co 10/12/14 s; skurcz ma odpowiednio 7/8/9 surowych i pas szerokości 2/3/3 bl. D zostaje czynna. Uszkodzona III nadal zalewa Groblę; II może być pominięta. Nie przyspieszać jedynej studni.
- **Arena Ø44:** cztery istniejące osłony, środkowa sadzawka r=6 i cztery studnie; suche pasy po progach faz stanowią zmianę, wizualnie oznaczoną grzybieniem, bez zalania całej areny.
- **Anty-kitowanie:** po >6 bl. Włosy wybierają ostatnią pozycję gracza, a boss zanurza się do najbliższej czynnej studni; po chybieniu daje pełne okno ataku, bez teleportu na gracza.
- **Dzwon:** znaleziony po III niezależnie od uszkodzenia; jedno uderzenie w całym przebiegu czyści bieżącą truciznę drużyny i daje 8 s odporności, z dźwiękiem niosącym się do areny.
| R2 solo bez specjalizacji | Kokoniara: przerwanie/ucieczka | Topielica: klucze, skurcz i pąk |
|---|---|---|
| Wojownik | LPM w kokon, blok skoku, wyjście za słup | LPM/PPM w klucze po wynurzeniu; blok skurczu lub ruch; cios w pąk |
| Ninja | LPM w kokon, krótki ruch po boku | trafienie dowolne po bulgocie; przejście po suchej krawędzi; LPM pąk |
| Mag | podstawowe zaklęcie w kokon, przerwanie Wywaru | zaklęcie w klucze/pąk z brzegu; zejście z pasa bez uniku przyciskowego |
| Akolita | własny cios, przywołaniec lub Chowaniec rozcina kokon | każdy z tych ciosów trafia klucze/pąk; przywołańce depczą pąki przy przejściu |
- Tabela opisuje dostępne kontry, nie zaliczony pomiar 4 klas; docelowe QA: każda wygrywa solo, zwykły wróg 8–15 s, uważny traci znacząco mniej.

## 7. WOW
- **Minuta 3–4:** z bezpiecznej kładki nad basenem I, po obrocie kołowrotu kamera zostaje graczowi; woda opada o 3 bl. w 5 s, odsłania barkę Bractwa z kwitnącymi pod wodą skrzyniami i pieczęcią Pasieki.
- Kadr: ciemna rama dwóch martwych mangrowców, pionowa ściana mokrego błota, jasna barka na dnie; trzy warstwy głębi, bez pustej tafli.
- Budowa: wypieczony kadłub `place template` i trzy wcześniej przygotowane stany wody/roślin BlockEditor; bąble i dźwięk `block.bubble_column.whirlpool_inside` są akcentem, nie zasłoną ekranu.

## 8. Paleta
- Dominanty: mangrove_planks, mangrove_roots, mud, packed_mud, mud_bricks, mossy_cobblestone, lily_pad, brown_mushroom_block.
- Chłodne teal w wodzie, ciemny brąz drewna, jeden mosiężny akcent przy kluczach; pochmurny dzień, mgła tylko na dalekim planie, kładki czytelne.
- Tło: rechot, kapanie, skrzyp kołowrotu; po Dzwonie na 2 s cisza i pojedynczy ton. W walce najwyżej jeden komunikat tekstowy naraz.

## 9. Sekrety i wybór
| Sekret | Wskazówka w świecie | Skutek |
|---|---|---|
| Wrak Rybaków | lina z beczki znika pod wodą przy Tamie | materiały/EXP po I |
| Skrzynia Topielca | trzy białe nacięcia na zachodnim palu | materiały/EXP po II |
| Dziennik Borka | imię nacięte na rybackiej sieci prowadzącej do ukrytej skrzyni w Kokoniarni | zapis o pierwszym otwarciu śluzy, materiały/EXP; samo uwolnienie Borka jest obowiązkowe |
| Dzwon | piktogram dzwonu na kole III i łańcuch schodzący w basen | jednorazowe oczyszczenie/EXP |
| Wywar Szeptuchy | zielone ślady od chaty do kotła | 4 serca osłony dla obecnej drużyny |
- **Wybór:** III obowiązkowa; II można uruchomić lub pominąć. Sprawna dodatkowa śluza zmniejsza liczbę wynurzeń, ale wzmacnia skurcz. Uszkodzona II daje „Skrzynię mechanika”, uszkodzona III „Zapis o odpływie”; każda wskazówka leży przy swoim kole. Decyzja pozostaje w instancji.

## 10. Powtarzalność i ocena
- Przy wejściu losowo uszkodzona jest II albo III, pokazana złamanym kołem na Rozstaju. Uszkodzona nie zamyka swojej studni, lecz jej użycie daje sekret. Gracz planuje inną kolejność i inne osłony wobec czynnych B/C; bąble zawsze uprzedzają wynurzenie.
- Kokoniara losuje kokon Borka w wąskiej izbie albo na szerokim pomoście; osłona i kierunek skoku elity zmieniają trasę ratunku. Dwa gotowe układy, bez losowania niewidocznego w starciu.
- S: boss bez śmierci, ≤2 trafienia skurczem, ≥3 sekrety; A: bez śmierci, dowolne sekrety; B: ukończenie z jedną śmiercią; C: więcej śmierci. Ocena nie blokuje EXP.

## 11. Nagroda
- Normalny pierwsze przejście: 240 EXP na postać (L3: 96 moby/elita, 96 boss, 48 sekrety); powtórka 100. Łatwy ×0,7, Trudny ×1,3; sekretowa pula pozostaje dostępna także gdy II jest uszkodzona.
- Rodzina: **Rybactwo Starej Śluzy** — hak, woskowana linka, buty na muł (nazwy robocze); statystyki do osobnej tabeli łupu.
- Materiały profesji: `PROF_VENOM_SAC`, `PROF_CLAY_LUMP`, `PROF_MEDICINAL_HERB`, `PROF_WATER_ESSENCE`; tylko tu Pieczęć Pasieki i Dzwonowy Odłamek.
- Sekrety 3/4/5 dają rosnący pakiet materiałów, rzadką perłę dopiero za 5/5; cała drużyna otrzymuje EXP i nagrodę końcową, odkrycie per gracz.
- Po pierwszym ukończeniu Mapa Granicy dopisuje historię śluz; wskazuje Pasiekę 003, nie wymaga ukończenia jej po kolei.

## 12. Trudności
- Łatwy: pąk 8 s; skurcz pas węższy o 1 bl.; 2 s odporności po wyjściu z wody, dłuższa zapowiedź Kokoniary 1,3 s.
- Normalny: liczby tabel; pąk 5 s; Dzwon 8 s; dwie pomyłki przy studniach są do przeżycia przy pełnym zdrowiu.
- Trudny: pąk 4 s; po skurczu przez 3 s jedna osłona jest mokra i niepewna, oznaczona zielonym kapanie; zapowiedzi nie spadają poniżej 0,7 s.
- Żaden tryb nie dodaje fali bez powodu ani nie zmienia prawa zdeptania pąka.

## 13. Wykonanie i plany B
- Baza v0.2 z klonu: proceduralny wypiek, MD, MM, przechodniość, 31/31 sond; przy projektowaniu R3 **nie** uruchamiać serwera ani nie zmieniać tych plików.
- Trudność 1 — zalanie Grobli po III: gotowe warianty BlockEditor uruchamiane przez interakcję z kołowrotem; plan B: dwa wypieczone szablony pomostu i suchego dna przełączane `place template` na aktywnym chunku po kontroli kolizji.
- Trudność 2 — skurcz zależny od 1–3 studni: trzy gotowe skille MM wybrane flagami MD, bez arytmetyki `variableadd`; plan B: trzy gotowe konfiguracje areny/emitera.
- Trudność 3 — wspólny pąk i śmierć gracza: `~onDeath` → summon PĄK HP1, `delay` we własnym skillu pąka, limit/więdnięcie przez kontroler instancji; plan B dla śmierci gracza opisany w §0. Testować skutek, bo nieznany warunek MM może milczeć.
- Warunki trucizny na graczu w `TargetConditions` metaskilla, nie inline na emiterze; wywołanie aury synchronicznie. Populacja `Despawn: persistent`, bez `randomstroll`, `summon level=1`; `TriggerDistance`, nie opóźnienie z funkcji MD.
- Nexo tylko dla ikony Pieczęci; tekstura ≤256 px i `item_model` w `nexo:`. Szacunek szczytu: 1 boss + 1 Pijawka + 6 pąków + 4 emitery + 4 display = 16 encji, limit twardy 24.
- Nowa mapa MD wymaga restartu przy przyszłym wdrożeniu; karta nie jest wdrożeniem. Modele z paczki są tylko sprawdzonymi kandydatami nazw, nie zatwierdzonym wyglądem.

## 14. Samokontrola
- §4 briefu: brak pustych przestrzeni; Kokoniarnia i zwrot na trasie; brak fal bossa znikąd (Skrzek to jaja, nie pąki); sekrety mają wskazówki; uszkodzona śluza i położenie Borka zmieniają taktykę.
- Kierunki: północ to −Z, południe +Z; wskazówka Borka z v0.2 („południe od Kokoniarni”) pozostaje prawidłowa: śluza III (66,−8) leży na **południe** od (54,−44).
- Czytelność: jedna zapowiedź = jeden zamknięty cel; dźwięk i znak ziemi przed karą; kara ma możliwą kontrę ruch/blok; Chowaniec/Akolita depczą pąki.
- Samoocena 0–10 do poligonu: fun 8; motyw/zwrot 8; czytelność 8; topologia 8; WOW 8; historia 8; powtarzalność 8; nagroda 7; wykonalność 7; różnorodność 9.
- **Ryzyko do bramki:** powrót dnem po III wymaga sprawdzenia kolizji na klonie, a liczby surowych obrażeń i cztery archetypy wymagają poligonu; bez tego karta opisuje cel, nie udaje zaliczonego testu.

## 15. Odpowiedź na recenzję (etap 5)
| Poprawka | Co zmieniono (sekcja) / odrzucono + dowód |
|---|---|
| C 002.1, przekrojowe P8/L7 | Kokoniarnia, Borek, korba i III są obowiązkowe; Grobla zalewa się zawsze; Skrzek składa odrębne jaja (§1, §3–4, §6). |
| C 002.2 | Los uszkadza II albo III, co zmienia czynne studnie i sekrety; Kokoniara losuje stronę Borka (§1, §6, §9–10). |
| C 002.3 | Bąbel truje także wrogów; początkowa Pijawka na arenie daje pąk do świadomego użycia (§0–1, §4). |
| C 002.5, P7 i L1–L4 | Topielica 525 HP, Kokoniara 131 HP, moby ×1,12; 240 EXP pierwsze/100 powtórka, boss ~2,5 min, pełna trasa ≤20 (§4–6, §11). |
| R1/R2, L5–L6 | Jeden bąbel po poległym, stały czas, każde trafienie usuwa, limit FIFO; tabela czterech klas i WOW opadającej wody pozostają (§0, §6–7, §14). |
| Odrzucone | Brak; poprawki obowiązkowe przyjęto. |
