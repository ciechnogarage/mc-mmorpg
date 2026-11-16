# 009 Dożynki w Malowanej Wsi

## 0. Kontrakt
- Poziom 9 (otwarcie od 8); Normalny solo 16–19 min, twardo ≤20 min; 1–4 graczy bez specjalizacji.
- Fantazja: wchodzę w święto, które świadomie trwa od lat, i uczę się odczytywać przerwy między krokami, żeby przerwać niebezpieczny korowód bez wybicia wsi.
- Emocja: słoneczna gościnność → niesamowita jednomyślność → smutek i ulga wśród ocalałych.
- Motyw: **rytm steruje zagrożeniami, nigdy bramą obrażeń**; na „4” ruch zamiera na tyle długo, by każdy archetyp trafił.
- Pąk R1: powstaje tylko po poległym; identyczny model/dźwięk jak w akcie. LPM, PPM, zaklęcie, pocisk, pet i przywołaniec usuwają go trafieniem. Ostatnia 1 s pęcznieje i świeci; po stałych 5 s (Łatwy 8, Trudny 4) dojrzewa w **Wstęgę**, wbity w ziemię znacznik bezpiecznej drogi.
- Na najbliższą podłogową „4” Wstęga **raz** rozwija 1×5 białych bezpiecznych płyt przez czerwony pas zagrożenia na 15 ticków, potem więdnie. Nie jest osłoną i nie zatrzymuje ataku; przejście wymaga ruchu po płytach. Dojrzewanie nie czeka na takt, czeka tylko rozwinięcie. Po G takt podłogi nadal steruje pasem i Wstęgą, lecz nie gestami wrogów.
- Kolejka per instancja: limit 12; trzynasty ususza **najstarszy według chwili powstania** przez 1 s szarzenia i dźwięk; max 6 zgonów/10 s. Chowaniec i przywołańce depczą przy przejściu. Pąk gracza → Cierń/Płatek dopiero po osobnym teście, plan B bez niego.

## 1. Cztery akty święta
- Wstęp A/B: barwne płyty rozświetlają się 1–2–3–4. „4” to **chwila wyzwolenia czerwonego pasa** i jednocześnie 15-tickowa **pauza tancerza na sąsiedniej białej płycie poza pasem**. Pierwszy cykl w B nie zadaje obrażeń: Wojownik sięga nieruchomego ćwiczebnego znaku z białej zatoki, Mag ma na niego prostą linię z niskiego ganku. Obaj widzą czerwony pas obok, a nie pod celem.
- Rozwinięcie C/D: czerwony pas rozkwita na „4” po wcześniejszym świeceniu i dźwięku; Kozioł zatrzymuje się po trzech krokach, Korowodnik w pauzie odsłania rdzeń na białej płycie. Trafienie podczas widocznej pauzy, bez bramy na konkretny tick.
- Zwrot G: **na każdej trasie** pierwszy cykl przy wejściu pokazuje jeszcze wspólny impuls płyt, po czym Przepiórka zrywa synchronizującą wstęgę i odtąd każdy wróg zapowiada własny gest. Jeśli Bęben uciszono w D/F, wspólny impuls jest już tylko świetlną pozostałością; jeśli ominięto, jego dźwięk trwa do progu G, a potem sam Bęben gra osobny lokalny pas. Żaden wariant nie zachowuje wspólnego metrum w H.
- Egzamin H: Przepiórka i Wieniec mają czytelne indywidualne pauzy; dźwięk pomaga przewidzieć strefę, sylwetka pozwala walczyć bez muzyki.
- Bęben jest opcjonalny: uciszenie wcześniej usuwa jego czerwony pas i daje wcześniejszą próbkę osobnego gestu; ominięcie oszczędza walkę, ale zachowuje pas aż do G. Dwie wstęgi C/D otwierają G; po scenie zwrotu G otwiera się H.
- Tempo „80 BPM” to oprawa słuchowa; logika serwera używa cyklu 60 ticków (15 na ćwierć), a oznaczone okno pauzy trwa 15 ticków. Nie ma `variableequals world.takt` w warunku trafienia/progresu.

## 2. Historia środowiskiem
- Malowane chaty są zadbane, pola pełne, lecz w progach widać stare buty z wielu lat. Wieś zgodziła się na Szczep, aby nigdy nie powtórzył się Głodny Rok; cena to święto, które nie umie się skończyć.
- W jadalni C stoją cztery talerze dla różnych ras i zapis wspólnej decyzji; żaden lud nie jest kozłem ofiarnym. Większość mieszkańców jest nieruchomym tłem za płotami, z wyraźnymi twarzami po finale.
- Odkrycie samodzielne: podpisana uchwała: „Rada Malowanej Wsi przyjęła szczep Ilwena Darnika i coroczne dożynki bez końca; nikt nas do tego nie zmuszał.” Wpis 009 w Mapie Granicy per gracz ma tę treść bez znajomości wcześniejszych lochów.
- Po bossie stojący mieszkańcy odprowadzają ocalałych z placu; nie ma widoku wymarłej osady ani gore.
- Kwestie (3): starsza wsianka przed placem: „Wybraliśmy pełny stół. Nikt nie mówił, kiedy wolno wstać.” Po ciszy: „Słyszę teraz własne kroki.” Dziecko: „Czy jutro też możemy malować?”

## 3. Mapa ASCII
```text
          N↑; 1 znak ≈ 9 bl.; całość 100×94, y=68–75
       [B 24×20]──[C 22×24]──[D 24×20]
          │          ╲    ╱          │
       [A 24×22]──[E 28×24]──[F 24×22]
       ognisko      plac       bęben
          │          │
       [S 20×18]──[G 24×22]══[H 34×32]
         skrót       próg       gaik
```
- Opłotki tworzą zewnętrzny obwód A–B–C–D–F–E–A, ale **jedyną przechodnią drogą do H jest G–H**; brama H widoczna od A jako biały wieniec ponad dachami.
- B jest pierwszą, obowiązkową lekcją: przejście A–E odblokowuje po pokazowym cyklu w B, więc D-first prowadzi B–A–E–F–D. C i D są potem celami w dowolnej kolejności: odsłonięcie dwóch **ceremonialnych wstęg bramnych** otwiera G. Próg G zawsze uruchamia przejście do osobnych gestów i dopiero potem otwiera H. Bęben D/F jest opcjonalnym kosztem taktycznym; wstęgi bramne są odrębne od Wstęgi z pąka.
- Skrót S od G do A otwiera się przed bossem; pętla przez chaty pozwala wrócić do ogniska bez przecinania aktywnego pasa E.
- Pionowość to niskie ganki y=72–75 i daszki jako punkt obserwacji, nie labirynt pięter; każdy próg ma 3–4 bl. szerokości i alternatywną osłonę.
- Za płotami 18 statycznych sylwetek/display w czterech grupach; aktywna walka nigdy nie dotyka cywilów. W H zostaje 6 sylwetek, reszta widoczna za bramą.

## 4. Beat chart
| Sala | Rola/min | Skład z powodem | Start | Tempo | Poza walką |
|---|---|---|---|---|---|
| A | orientacja 0–1 | 0; słyszalny bęben i widok Wieńca | bez walki | niskie | ognisko, dwie ścieżki |
| B | nauka 1–3 | jeden Kozioł i nieruchomy znak ćwiczebny; pierwszy cykl bez obrażeń | Mag widzi znak z ganku, Wojownik sięga z białej zatoki | wysokie | czerwony pas i osobna biała pozycja trafienia |
| C | wybór I 3–5 | Korowodnik + Wianek; pauza daje cel, wianek pas | gracz obserwuje z ganku | wysokie | pierwsza wstęga, uchwała |
| D | wybór II 5–7 | Kozioł; Bęben-Purchawa tylko w wylosowanym wariancie D | wróg widoczny zza chaty | wysokie | druga wstęga, sekret w izbie |
| E | WOW/oddech 7–8 | 0 aktywnych; korowód sceniczny | gracz stoi na wzgórku | niskie | wybór wejścia na rynek |
| F | wybór Bębna 8–10 | Wianek + opcjonalny Bęben; gracz ucisza lub obchodzi | pas zapowiedziany przed progiem | wysokie | koszt/moment ciszy, skrót |
| G | obowiązkowy zwrot 10–13 | elita Przepiórka + jeden Korowodnik | wspólny impuls płyt → Przepiórka rozrywa wstęgę → osobne gesty | wysokie | ognisko przez S, próg H |
| H | egzamin 13–16,5 | Wieniec + pas sceniczny; bez fal mobów | wejście z ganku i pełnym widokiem | wysokie | ocalała wieś, wyjście do minuty 19 |
- Maks. dwa aktywne typy w jednym starciu; rozstawieni wrogowie czekają na mapie od startu, pułapka F uruchamia się po wejściu.
- Przy D-first C/D zamieniają minuty i role wyboru; E może nastąpić przed D, lecz po obowiązkowej lekcji B. F można minąć bez walki z Bębnem; łączny cel nadal ≤19 min.

## 5. Bestiariusz i liczby
- L1/L2: poziom 9 daje DPS_ref 7,0 i mnożnik Gaj v8 ×1,96 dla HP i obrażeń. Wartości zaokrąglone; punkt startu do próby czterech niespecjalizowanych klas.
| Mob | Rola; vanilla → model | HP / surowe | Zapowiedź i kara | Kontra; nieuważny → uważny |
|---|---|---|---|---|
| Korowodnik | jedyny humanoid, zaszczepiony wieśniak występujący parami; husk z kwiatem → własny | 78 / 6 | `block.note_block.hat` 1,0 s, obrót w zamknięty łuk r=3,5, 12 | odejść za płot/blok; na widocznej „4” 15 ticków rdzeń na białej płycie podatny na dowolne trafienie; stoi: 2×12, reaguje: 0 |
| Bęben-Purchawa | emiter rytmu; mushroom cow/armor stand → własny pękaty grzyb | 59 / 4 | `block.note_block.basedrum` 1,2 s, pas ziemi błyszczy, po 4 uderzenie 10 | zejść z pasa lub uderzyć grzyb; ignoruje: 10, reaguje: 0; śmierć wyłącza jego pas, nie blokuje G |
| Wianek | lotny kontroler; allay → własny krąg siana | 55 / 4 | `block.note_block.bell` 1,0 s, czerwony pierścień na ziemi, na „4” 12 | ruch poza pierścień albo Wstęgą po białej drodze; stoi: 12, reaguje: 0 i 1 s odsłony |
| Kozioł Dożynkowy | szarża; goat → własna słomiana koza | 82 / 8 | `entity.goat.prepare_ram` 1,1 s, 3 krótkie linie w ten sam zamknięty sektor, 14 | bok/za płot, potem 15 ticków pauzy; stoi: 14, reaguje: 0 + odsłona |
- Dojrzały pąk daje **Wstęgę**: raz rozwija 1×5 bezpiecznych płyt na „4”, nie tworzy Snopa ani osłony. W jednym cyklu zagrożeń aktywny najwyżej jeden duży czerwony pas.

## 6. Elita i boss
- Elita **Przepiórka Ostatniego Snopa**: funkcja uczy czytania pauzy bez wspólnego bębna; haczyk: skok chowa ją za snopem, a własny cień pokazuje miejsce lądowania.
- Historia: przy żniwach zostawiano ostatni snop dla przyszłego zasiewu; Szczep uczynił z niego strażniczkę placu. Sylwetka trójkątna, niska, z trzema źdźbłami; biały dziób to jasny punkt odsłony.
- Elita **333 HP** (≈25% HP bossa)/10 surowe; `entity.parrot.fly` 1,0 s → zamknięty punkt lądowania 7; po lądowaniu 20 ticków odsłony białego dzioba, każdy typ trafienia działa. Płot przecina tor, brak nieuchwytności poza fazą lotu.
- Boss **Wieniec Dożynkowy**: funkcja egzaminu sprawdza zapowiedź pasa, pauzę Korowodnika i indywidualny gest po zerwaniu wspólnego pulsu w G.
- Haczyk: korowód podtrzymuje Wieniec, lecz jego znak tancerza staje na **białej płycie poza czerwonym pasem** przez 15 ticków; trafienie dowolnym źródłem zrzuca Wieniec na 5 s. Po G okno wyzwala osobny gest tancerza, nie wspólny bęben.
- Historia: nie jest opętanym tłumem; żywy Szczep splótł publiczną obietnicę obfitości w jeden organizm. Przerywamy pętlę, żeby mieszkańcy mogli zdecydować na nowo.
- Wygląd: ogromny jasny pierścień ze słomy na czterech korzeniach, w środku jedno białe serce nasienne, reszta kobaltowe/karminowe wstęgi. Model własny; brak potwierdzonego pasującego modelu LittleRoom.
- Arena H 34×32, trzy niskie ganki, 4 malowane chaty, 2 stałe białe zatoki; czerwone radialne pasy mają osobne białe pola celów poza ich zasięgiem. Aktywny najwyżej jeden duży pas. Żadna brama postępu nie zależy od taktu.
- HP **1330** (L2: DPS_ref 7,0 × 190 s ≈ 3 min 10 s uważnie); osłona redukuje 50% obrażeń, nigdy nie zeruje ich. Trafienie znaku tancerza w pauzie zrzuca osłonę 5 s; chybienie nie więzi walki.
- F1 100–65%: po G tylko osobne gesty, z wizualnym wspomnieniem czterech płyt; F2 65–30%: chata traci dekoracyjny płot i odsłania boczną trasę; F3 30–0%: gesty pozostają czytelne przy cichszej muzyce, bez konieczności ciszy.
- Główną kontrą jest **stały naziemny znak tancerza** na białej płycie, 1 HP, aktywny 15 ticków po jego pozie; ruchoma sylwetka jest wskazówką. Trafienie daje świadomą odsłonę, a boss zawsze przyjmuje co najmniej 50% obrażeń. Akolita może użyć własnego ciosu/zaklęcia.
| Zdolność | Zapowiedź / cel | Kara | Powrót / kontra |
|---|---|---|---|
| Krok korowodu | `block.note_block.hat` 1,0 s, łuk r=3,5 zamknięty | 7 | 15 ticków pauzy na „4”; bok/blok i dowolne trafienie tancerza |
| Opad wstęgi | `block.note_block.bell` 1,2 s, pas świeci | 6 + odepchnięcie | 1,5 s; zejść do zatoki lub za chatę |
| Zrzut wieńca | `entity.goat.prepare_ram` 1,1 s, cień w punkcie zamkniętym | 9 | 2 s na ziemi, pełne obrażenia; odejść z cienia |
| Przecięcie placu | `block.grass.place` 1,0 s, korzeń wskazuje czerwony pas | 5 przy wejściu w pas | 2 s; zejść do białej zatoki lub przejść Wstęgą przez pas |
| Przyśpiewka | `block.note_block.flute` 1,3 s, dwie kolejne czerwone linie zamknięte przed ruchem | 2×5 w te same punkty | 2 s; ruch w bok raz, nie wracać w linię |
| Samotny obrót | cichsza muzyka + biała poza przez 1,2 s w F3 | 8 w łuku r=3,5 | 20 ticków gestu odpoczynku; bok/blok, trafienie znaku tancerza |
- Anty-kitowanie: po 6 s bez trafienia korowód przenosi jeden jawny pas do ostatniego **zamkniętego** miejsca gracza, po czym wraca na środek; żadnego ataku śledzącego unik.
- Ósmy takt finału: wieniec opada na ziemię, bęben milknie, widać twarze sześciu ludzi za bramą; po zwycięstwie opuszczają ręce i odchodzą z placu, nie giną.
| Solo R2 | Elita Przepiórka | Wieniec / tancerz |
|---|---|---|
| Wojownik | blok skoku, LPM w biały dziób po lądowaniu | blok kroku, LPM w stały znak podczas 15 ticków; mocny cios w 5 s odsłony |
| Ninja | zejść z cienia, seria po lądowaniu | ruch przez zatokę; LPM albo pocisk w stały znak, seria po opadnięciu |
| Mag | zaklęcie w dziób po lądowaniu z ganku | zaklęcie w stały znak z ganku lub własny pocisk; białe pole leży poza czerwonym pasem |
| Akolita | własny cios/zaklęcie lub pet w 20-tickowym oknie | własne trafienie w stały znak; pet może go wybrać po pozytywnym teście AI, potem bije odsłoniętego bossa |

## 7. WOW
- Minuta 7, wzgórek E: białe chaty malowane w kwiaty stoją wokół rynku. Korowód unosi Wieniec szeroki jak stodoła na korzeniach; rynek wiruje jak tarcza zegara, a wstęgi rozwijają nad nim barwny dach i przesuwają kolorowe cienie po białych ścianach.
- Ruch narasta przy wejściu, muzyka trwa; nikt nie obraca głowy ku graczowi. W pierwszym planie czerwony pas przecina biały **wzór drogi na płytach**, zapowiedź działania Wstęgi, w głębi jasne serce Wieńca. Kadr nie wymaga pąka pozostawionego przez gracza.
- Budowa: calcite/white_terracotta, malowane wzory Nexo na 8 fasadach, 6 segmentów wstęg jako display, obrotowe płyty wzoru pod rynkiem bez ruchu bloków kolizyjnych; tłum pozostaje statyczny, bez AI.

## 8. Paleta
- Dominujące: calcite, white_terracotta, blue_terracotta, hay_block, oak_planks, red_wool, yellow_wool, flower_pot; wzory Nexo na maks. 8 fasadach.
- Bezchmurne popołudnie i miękkie światło; białe ściany odbijają słońce, cień Wieńca jest jedyną chłodną plamą. Bez krwi, kości, ognia i nocy.
- Tło: `note_block` bęben, skrzypki i śpiew; ataki mają osobne dźwięki, a po G muzyka cichnie pod indywidualnymi gestami bez pełnej ciszy.

## 9. Sekrety i konsekwencja
- S1: malowany kogut na ścianie B wskazuje trzecią płytę rytmu; za nią izba z książką pieśni, bez obowiązkowego quizu.
- S2: cztery talerze w C mają cztery różne motywy kwiatów; ten sam motyw za piecem kryje uchwałę i wzór rzemieślniczy.
- S3: wstęga z D zaczepia się o dach F; wejście z ganku prowadzi do szkatuły ze starym listem mieszkańców.
- S4: przerwany dźwięk dzwonka za G prowadzi do furtki S i pamiątkowej maski Przepiórki.
- Wybór taktyczny: zostawić Bęben-Purchawę, zachować jego pas i wspólną prognozę **do progu G**, albo uciszyć go wcześniej, usunąć pas i wcześniej zobaczyć indywidualny gest. Wybór po walce: zostawić wieniec jako pustą dekorację lub go rozpleść; Mapa/NPC reagują, moc i łup nie.

## 10. Powtarzalność i ocena
- Losuje się, czy pierwszy pas na placu idzie północ–południe czy wschód–zachód; chorągiew w A i próba w B pokazują wariant. Zmienia wybór ganku i miejsce, w którym Wstęga tworzy bezpieczną drogę.
- Losuje się położenie Bębna: D albo F; oba cele C/D są dostępne, a **dwie ceremonialne wstęgi bramne** otwierają G, potem obowiązkowy zwrot otwiera H. Wstęgi bramne są dekoracją/kluczem, odrębną od skutku pąka. Losowanie zmienia moment, kiedy opłaca się uciszyć Bęben.
- S: boss, ≥3 sekrety, ≤1 trafienie pasa i ocaleni w kadrze; A: boss + dwie wstęgi; B: boss; C: boss po ≥2 powrotach. Ocena nie warunkuje EXP ani łupu.

## 11. Nagroda
- EXP L3: pierwsze przejście Normalny **1080** na osobę (moby/elita 432, boss 432, sekrety 216), powtórka **450**; Łatwy ×0,7, Trudny ×1,3. Pełny osobisty EXP i łup bez dzielenia puli.
- Rodzina robocza: Buty Korowodu (+5% prędkości po białych płytach), Sierp Dożynkowy (+5% obrażeń podczas odsłony), Chusta Malowanej Wsi (+4% redukcji obrażeń pasów); wartości startowe do poligonu. Materiał profesji Barwiona Słoma.
- Tylko-tu: Wstęga Ostatniego Tańca do kosmetycznego rzemiosła. Sekrety dają motywy malarskie, nie przewagę bojową.
- Mapa Granicy dostaje świadectwo świadomego wyboru wsi i szkic dojścia do Matecznika; 010 otwiera poziom, więc 009 nie jest obowiązkowym kluczem.

## 12. Trudności
- Łatwy: pąk 8 s, pas zapowiedziany 1,6 s, pauza tancerza 20 ticków, zatoki podświetlone.
- Normalny: pąk 5 s, pas 1,2 s, pauza 15 ticków, opisany skład.
- Trudny: pąk 4 s, pas 1,0 s, Przepiórka ma drugi skok po 3 s; pauza tancerza nadal 15 ticków, żadnej bramy na ping.

## 13. Wykonanie i plan B
- Teren i chaty wypieczone offline przez `place template`; MD zapisuje wstęgi, skrót, wybór Bębna i checkpoint. BlockEditor zmienia tylko płot F2 i dekorację finału.
- Rytm: jeden zegar instancji uruchamia widoczne czerwone pasy; obrażenia sprawdza kolizja z czerwonym polem **z wyłączeniem 1×5 białych płyt Wstęgi**, bez `variableequals world.takt` na trafieniu. Zegar obrazu i zegar obrażeń mają jeden znacznik startu oraz test zgodności na klonie; po G każdy wróg ma osobną zapowiedź niezależną od Bębna.
- Pauza tancerza: **stały naziemny mob-hitbox 1 HP** na białej płycie, aktywny przez ≥15 ticków; `~onDamaged` od dowolnego źródła zrzuca osłonę na 5 s i usuwa hitbox przez despawn, bez śmierci i bez pąka. Na klonie zmierzyć LPM, PPM, zaklęcie, pocisk, pet, przywołańca oraz różnicę klatek obrazu i obrażeń; wynik ma wskazać jedną świadomą odsłonę bez automatu. Jeśli ruchomy cel albo zegar zawiedzie, ten stały znak jest obowiązkowym planem B, z tym samym oknem.
- Tłum: statyczne display bez AI, 18 sylwetek bez animacji głów; 6 po finale. Plan B: 8 sylwetek + banery rzucające cienie; ruch rynku pozostaje.
- MM 5.11.2: `Despawn: persistent`, bez `randomstroll`, spawn `level=1;os=true`, warunki na celu wyłącznie w `TargetConditions` synchronizowanym z poziomu wyżej; nowe warunki potwierdzić skutkiem na klonie.
- Nexo: wzory i glify ≤256 px, `minecraft:default` dla fontu, `nexo:` dla modeli itemów. Szacunek: zwykle 4–6 aktywnych mobów, max 8 + boss + 12 pąków; 18 statycznych display, bez AI.

## 14. Samokontrola
- Skład aktywny to dokładnie cztery typy z ramy C oraz elita Przepiórka; żaden słomiany Snopowiec z 004 nie wraca tu jako wróg.
- Na każdej ścieżce do H B daje bezpieczną demonstrację czerwonego pasa i białego pola celu; G wymusza utratę wspólnego pulsu, a H testuje już tylko osobne gesty.
- 80 BPM jest oprawą do G; po zwrocie każdy wróg pokazuje własną pozę i pas. Zegar hazardu nadal ma widoczny znacznik, także bez dźwięku.
- Dwa pasy z Przyśpiewki są zamknięte przed pierwszym ciosem; druga linia nie aktualizuje się po ruchu gracza, zgodnie z Gaj v8.
- Jeśli gracz uderza Bęben w środku cyklu, aktywny pas kończy bieżącą zapowiedź i dopiero potem gasną wspólne sygnały; nie ma nagłego niewidzialnego trafienia.
- Ocalałych w finale nie liczy się jako wrogów ani cele; interakcja z dzieckiem następuje dopiero po wyłączeniu hazardów.
- Pąk po bossie dojrzewa nadal według R1 we Wstęgę; przy wygaszonych pasach jej jednorazowe białe płyty są śladem walki, nie pułapką przy łupie.
- W wariancie bez dźwięku rytm czyta się z czterech kolejno rozświetlanych płytek; w wariancie bez cząsteczek z modeli pasów i wyraźnej pozy tancerza.
- Blockout musi sprawdzić, czy z każdego ganku widać białe serce Wieńca, czerwony pas i białą zatokę; czy Wojownik dosięga znaku bez wejścia w czerwony pas; i czy Mag nie może bezkarnie strzelać przez całą H zza chaty. Zasłonięty widok lub bezkarna pozycja oznacza przesunięcie chaty/pasa przed wdrożeniem.
- Brak walki z tłumem cywilów, gore i fal przy bossie. A/B uczą, E daje kadr, S skrót; pozostałe sale zmieniają taktykę. Sekrety mają prawdziwe wskazówki w świecie, a uchwała tłumaczy motywację wsi.
- Maks. jeden komunikat w walce: ikona aktywnego pasa; dźwięk i kolor pokazują resztę. Muzyka nie jest jedynym sygnałem. 15 ticków pauzy dotyczy bonusu, boss stale przyjmuje część obrażeń.
- Kontry są ruchem, blokiem lub dowolnym trafieniem, bez osobnego przycisku uniku; nieuważny dostaje 2×6–9, uważny ma zatoki i odsłony. Test solo czterech archetypów wymagany przed wdrożeniem.
- Własna ocena 0–10: fun 8; motyw/zwrot 8; czytelność 8; topologia 8; piękno/WOW 9; fabuła 9; powtarzalność 7; nagroda 7; wykonalność 7; różnorodność 9.
- Ryzyko dla recenzenta X: zgodność zegara obrazu i obrażeń oraz trafienia sześciu źródeł wymagają próby na klonie; stały znak zachowuje świadomą kontrę bez dźwięku.

## 15. Odpowiedź na recenzję (etap 5)
| Poprawka obowiązkowa | Co zmieniono (sekcja) |
|---|---|
| X: zwrot pomijany przez Bęben | Obowiązkowa zmiana wspólnego impulsu na osobne gesty w G przy obu wyborach; H tylko przez G, §§1, 3–4, 13. |
| X: dwuznaczna „4” | Czerwony pas i osobna biała płyta celu; bezpieczna próba Mag/Wojownik w B, §§1, 4, 6. |
| X/P2/L5: kopia Snopa | Dojrzały pąk daje jednorazową Wstęgę, drogę 1×5 na „4”, bez osłony, §§0, 5–6, 10, 14. |
| X: ruchomy hitbox/15 ticków | Stały znak 1 HP, ≥15 ticków, ręczne trafienie i plan pomiarów sześciu źródeł na klonie, §§6, 13; pomiar nie jest deklarowany jako wykonany. |
| P1/L6: powtórka WOW | Korowód unosi Wieniec, ruch rynku i barwny dach; bez obrotu głów i ciszy, §§7–8. |
| P7/L1–L4: liczby i czas | HP 1330/333, moby ×1,96, EXP 1080/450, boss ~3:10 i wyjście ≤19 min, §§4–6, 11. |
| R1/R2: reguły aktu | Pąk po poległym, stały czas, trafienie, kolejka i tabela 4 klas, §§0, 6, 13. |
