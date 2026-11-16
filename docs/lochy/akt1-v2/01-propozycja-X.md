# Projektant X — propozycja Aktu 1 (etap 1)

Status: projekt do rozbicia w recenzji, nie specyfikacja wdrożeniowa. Numeracja 001–010 odpowiada poziomom zalecanym 1–10; każdy loch otwiera się od N−1, trwa docelowo 12–20 min solo na Normalnym i nie zakłada specjalizacji ani konkretnego archetypu. „Unik” poniżej zawsze oznacza ruch, blok lub przerwanie, nigdy nowy przycisk.

## A. Audyt starego planu 100

1. **Potwierdzam próbnik, z zastrzeżeniem „≈”, nie tożsamości.** [Plan §1, §3–4](../plan-100-lochow-10-wysp.md) umieszcza w Akcie 1 archiwum pod wodą, kryształy, śnieg, popiół i pustynię kości; akty 2/4/5/8/3 rozwijają dokładnie te obrazy jako własną dominantę. To zużywa pierwsze wrażenie późniejszych krain. Nie każde użycie kryształu lub kości jest zabronione; problemem jest pełny pakiet klimatu, mechaniki i bossa.
2. **Potwierdzam formułę jako ryzyko projektowe, nie udowodnioną reakcję graczy.** [Plan §1 i tabelki aktów 2–9](../plan-100-lochow-10-wysp.md) powtarza K1–K10, w tym ogień K8 nawet w mrozie i krypty K9 nawet w lesie. To dowód struktury, a nie wynik playtestu. [Audyt Wynn §5–6](../../research/wynncraft/audyt/04-swiat-i-tresci.md) dokumentuje zarzut zaniedbanych dungeonów i monotonnych zadań platformowych w raidach; analogia ostrzega, lecz nie dowodzi, że nasza siatka już nudzi.
3. **Potwierdzam brak prawa.** [Plan §3](../plan-100-lochow-10-wysp.md) wpisuje dla Aktu 1 „Podstawy: unik, blok, przerwanie, osłona”, podczas gdy inne akty mają regułę środowiska. [Brief §1](00-BRIEF.md) wyklucza unik jako przycisk. „Podstawy” są umiejętnościami gracza, nie zachowaniem Zielonej Granicy.
4. **Potwierdzam brak spoiwa w planie 100; częściowo obalam dla pojedynczych speców.** [Plan §4](../plan-100-lochow-10-wysp.md) daje dziesięć haseł i „test wszystkiego”, bez wspólnej przyczyny i skutku. [Loch 002 §1](../loch-002-bagna-zgnilych-korzeni.md) ma lokalny ciąg Wela–Borek–śluzy–Topielica oraz zapowiedź 003; to prawdziwa historia, tylko oparta na wycofywanym morskim 003.
5. **Potwierdzam kolizję z aktualnym kanonem.** [Plan §1–2, §5](../plan-100-lochow-10-wysp.md) definiuje „komórkę” loch ↔ strefa, klucz/mapę i wspólny finał pary. [Brief §1](00-BRIEF.md) podaje późniejszą decyzję 30.09: wspólne kraina, zagrożenie, rodziny i materiały, lecz inni wrogowie, bossowie, historie i formy; bez par 1:1. [Akt 1 — wyspa §1–2](../akt1-wyspa-i-lochy.md) zachowuje wcześniejsze pary, więc i ten dokument wymaga korekty, nie służy jako nowszy kanon w tej kwestii.
6. **Własne: rozjazd skali.** [Akt 1 — wyspa §4](../akt1-wyspa-i-lochy.md) zakłada 1–20, [Loch 001 §1](../loch-001-kwietna-polana.md) 1–5, [stara drabina, „Cel” i „Testy”](/home/dev/Desktop/Przemek/MC/docs/dungeon-ladder-002-010-foundation-v0.0.1.md) wprost odrzuca 1 loch = 1 poziom. [Plan, wstęp i §2](../plan-100-lochow-10-wysp.md) oraz [brief §1](00-BRIEF.md) przyjmują nowsze 1–10. Potrzebny rebalance 001/002, nie samo przemianowanie.
7. **Własne: plan 100 obiecuje otwieranie lochów samym poziomem, ale finał sugeruje parę „ten sam boss” oraz wspólny tytuł** ([Plan §2](../plan-100-lochow-10-wysp.md)); to można zachować jako dwa niezależne finały tej samej *sprawy*, bez tej samej postaci, areny ani obowiązkowego drugiego toru.
8. **Własne: modelami nie wolno uzasadniać potworów.** [Akt 1 — wyspa §2–3](../akt1-wyspa-i-lochy.md) dobiera gotowe modele po nazwie, a sam nakazuje zatwierdzenie po QA. [Baza bossów §1–3](../../research/projektowanie-bossow/README.md) wymaga najpierw funkcji, haczyka i czytelnej kontry; proponuję własny model tam, gdzie paczka nie udźwignie motywu.
9. **Własne: §2 planu 100 nadal uzależnia sekret od wyspy i odwrotnie.** Opcjonalność mechaniczna jest zapisana, ale po odrzuceniu par 1:1 taka symetria co numer może być odczytana jako ukryty obowiązek. Łączyć materiały i legendy na poziomie *aktu*, a nie komórki.

## B. Akt 1 — Zielona Granica

**Tożsamość.** To pogranicze sadów, łąk, żywopłotów, torfowisk i drewnianej inżynierii: piękne, użytkowe, zamieszkane. Nie jest „biomem natury”, lecz krainą, która przez pokolenia negocjowała z żywym krajobrazem. Później mają nadejść morze, pustynny żar, głębia, mróz, księżyc, wojna frakcji, erupcja, burza i echo; tutaj niczego z tych spektakli nie trzeba pożyczać ([Plan §3](../plan-100-lochow-10-wysp.md)).

**Zagrożenie.** Dawna Sieć Rozdziału miała utrzymać zbiory po głodzie. Jej opiekunka, **Mistrzyni Przesiewu Iga**, zamknęła obieg zwrotny i skupia sok w jednym „bezpiecznym” sadzie. Ocalałe gospodarstwa korzystają, obrzeża obumierają, a zwierzęta i rośliny bronią przepływu; Iga nie jest kultystką ani uosobieniem zła. Zapiski i układ zaworów pokażą, że system był wspólnym dziełem ludzi, elfów, orków i nieumarłych rzemieślników — żadna rasa nie jest potworną hordą. Wątek pozostaje świecki; latarnie są narzędziami, nie znakiem Solenny ([Panteon](../../wiary/panteon.md), [brief §1](00-BRIEF.md)).

**Prawdziwe prawo aktu — „sok nie znika, tylko zmienia odbiorcę”.** Widoczne bursztynowe żyły łączą źródło, zawór i odbiornik. Otwarcie jednego ramienia zasila je kosztem innego: kwiat wyrasta, kładka sztywnieje, osłona bossa słabnie albo przeciwnie, głodna odnoga agresywnie szuka soku. Gracz może odczytać kierunek po świetle płynącym w żyłach i po stanie liści. To *lokalny wybór przyczynowo-skutkowy*, nie licznik zasobu, losowy proc ani ten sam „przekręć zawór” powtarzany dziesięć razy. W 001 widać skutek pojedynczej latarni; do 010 gracz rozumie koszt ratowania jednego miejsca. Element prawa jest w każdym lochu, lecz każda własna mechanika z C używa go inaczej.

**Łącznik.** Każdy loch oddaje jeden fragment mapy Sieci; komplet nie jest kluczem do 010. W menu/atlasie pojawia się po każdym lochu zmieniony rysunek odpływu i krótki obraz skutku w instancji; każdy loch można otworzyć poziomem, nawet bez wcześniejszych. Wyspa ma osobne historie i własne sposoby zdobycia równoważnego postępu; wspólna jest tylko diagnoza kryzysu. Wspólny, trwały „powrót zieleni” w hubie wymaga decyzji o fazowaniu per postać albo sezonowego stanu serwera — bez tej decyzji obiecujemy wyłącznie atlas i finał instancji.

| Po lochu | Odkrycie gracza | Widoczna zmiana (atlas i finał instancji, bez blokady wyspy) |
|---|---|---|
| 001 | Światło kieruje korzeniami, lecz nie tworzy soku. | Zgaszona skarpa zakwita; w atlasie pierwsza żyła. |
| 002 | Odcięte mokradło gnije, bo nie ma odpływu. | Cofnięty muł odsłania groble, nie morskie archiwum. |
| 003 | W zielniku skreślono nazwy dawnych odbiorników. | Kartki pokazują brakującą gałąź Sieci. |
| 004 | Przeszczepy łączą dwa zagrożenia w jedno. | Rozdzielone pnie otwierają długi widok na centralny sad. |
| 005 | Młyn podaje nasiona tylko w jedną stronę. | Jedno koło staje, drugie rusza wstecz. |
| 006 | Zbierane plony przeciążają tarasy. | Tarasy przestają się zapadać, w dole zostaje szkoda. |
| 007 | Alarm nie chroni mieszkańców, tylko transport. | Milkną strażnicze dzwony; gracz widzi wozy do centrum. |
| 008 | Zapylacze głodnieją poza wybranym sadem. | Pojawia się drugi, dziki szlak kwitnienia. |
| 009 | Sieć tworzy zastępcze rośliny bez miejsca dla starych. | Wylęgarnia zatrzymuje cykl; w atlasie widać koszt pełnego wyłączenia. |
| 010 | Iga chciała uniknąć powtórki głodu; jeden „idealny” odbiorca niszczy resztę. | Gracz przywraca obieg rozproszony, akceptując mniejsze zbiory centrum. |

**Łuk emocjonalny.** 001 zachwyt i troska → 002 niesmak → 003 podejrzenie → 004 sprawczość → 005 odkrycie skali → 006 ciężar kompromisu → 007 złość na kontrolę → 008 czułość wobec współzależności → 009 lęk przed własnym „prostym rozwiązaniem” → 010 odpowiedzialność bez triumfu nad „złym narodem”.

## C. Dziesięć lochów

Wspólna rama produkcyjna: każda mapa ma 6–9 zwartych sal po ok. 20–40 bloków, bramę bossa widoczną przed połową trasy, pętlę/skrót do ogniska i wybór celu. Starcie zwykłe: 1 typ przy nauce, 2 standardowo, 3 tylko w teście. Zapowiedzi ataków ≥0,5 s, czytelne także bez dźwięku; kontrą zawsze może być ruch, osłona, blok, PPM lub przerwanie zwykłymi obrażeniami. Każdy boss dostaje 4–6 ruchów z osobną pozą, 2–3 fazy i ograniczenie kitowania przez zmianę osłon/trasy, bez nieskończonych fal. Łatwy wydłuża zapowiedź i upraszcza *jedno* złożenie mechanik; Trudny dokłada złożenie już poznanych wzorców. Dla każdego: 3–5 sekretów z wizualną wskazówką, EXP i równoważny tier łupu dla całej drużyny, żeton przeciw pechowi, materiał profesji tylko-tu, losowanie zmieniające taktykę. Wpisy poniżej są zwartymi kartami koncepcji; przed budową wymagają osobnych beat chartów z czasami, tabel umiejętności i testów czterech archetypów ([brief §3–4](00-BRIEF.md)).

### 001 — Kwietna Polana: Sad Zgasłych Latarni (poz. 1)
- **Fantazja:** wejść do bajkowego sadu i samemu sprawić, by żywe ścieżki znów oddychały.
- **Motyw — wstęp:** na bezpiecznym manekinie latarnia odsłania bursztynową żyłę i cofa korzeń bez walki.
- **Rozwinięcie:** dwie latarnie dają alternatywne osłony; zapalenie jednej przygasza drugą i przestawia korytarz, nie zamyka bramy.
- **ZWROT:** strażnik zasłania światło własną koroną; gracz musi przestawić latarnię z odsłoniętej strony, nie szukać nowego przycisku.
- **Egzamin:** na arenie przełączenie właściwej latarni odsłania jasny rdzeń i chwilowo ucisza korzenie, lecz gasi bezpieczny brzeg.
- **Topologia:** `obóz→łuk drzew→[wieża↔ogród]→szyja→sowiedzisko→brama→arena↩obóz`; 8 stref, pętla przez oba ramiona, skrót z wieży.
- **Beat:** 0–3 min nauka bez presji; 3–8 gracz podchodzi do czuwającego wilka / łucznik ostrzega z góry; 8–12 elita i cisza przy ognisku; 12–16 boss.
- **Wilk gajowy:** skokiem ogłasza linię natarcia, karze cofanie prosto, a boczny krok otwiera jego flankę.
- **Korzeniak:** czerwonym okiem zapowiada pole chwytu, które omija się ruchem lub blokuje na brzegu.
- **Cierniowy łucznik:** naciąga cięciwę nad głazem, zmusza do użycia pnia jako osłony i podejścia bokiem.
- **Muchomorzec:** świecącą główką zapowiada osłonę wilka, przerywaną serią zwykłych ciosów dowolnej klasy.
- **Sowiedź (elita):** najpierw straszy trzaskiem dzioba, potem skacze na zaznaczone pole; po pudle odsłania bok i uczy karania powrotu.
- **Boss — Strażnik Sadu:** nie broni sadu ze złości, tylko trzyma zamknięte ujście; jego cień przesuwa się wraz z koroną, więc słaby rdzeń trzeba odsłonić latarnią.
- **Fazy:** 1 cień + zamach + ciernie; 2 korona obraca cień i zarasta jedną osłonę; 3 rdzeń zostaje odsłonięty na krótsze, wyraźne okna; podchodzenie wymusza korzeniowy pierścień przy krawędzi.
- **WOW:** w 7. minucie, z wieży, gracz zapala lampę i widzi, jak na zboczu setki zgaszonych kwiatów rozświetlają się po kolei aż do ogromnego, martwego drzewa.
- **Paleta:** róż piwonii; liście wiśni, mech, dąb, jasny kamień, glina, miód, przygaszona miedź; zmierzch, ciepłe lampy i trzask kory przechodzący w ptasi śpiew.
- **Poza walką:** trzy sekrety zdradzają pyłki na drodze do lamp; wybór wieża/ogród decyduje, gdzie zostanie osłona w walce, i zmienia ocenę bez blokady łupu.
- **Powtórka/nagroda:** losuje się zasłonięta latarnia, więc zmienia się bezpieczne podejście; Żeton Sadu, żywica i rdzeń do stolarstwa; Trudny miesza łucznika z polem korzeni, nie dodaje HP.
- **Granica/spadek/ryzyko:** zostają latarnie, wilk, Korzeniak, Łucznik, Sowiedź i skrót; znikają puste pasma i boss jako worek z falami; to sad, nie księżycowy las Aktu 6. Cień korony w BlockEditor → plan B: dwie stałe pozycje korony i lamp.

### 002 — Bagna Zgniłych Korzeni (poz. 2)
- **Fantazja:** osuszyć uwięzioną osadę i zobaczyć, co woda ukrywała pod własnym domem.
- **Motyw — wstęp:** przezroczysta śluza na pustym basenie pokazuje, jak bursztynowa żyła zasila lub odcina odpływ.
- **Rozwinięcie:** otwarta śluza osusza przejście i jedną studnię bossa, ale odsłania gniazdo pijawek na dnie; gracz wybiera kolejność dwóch odnóg.
- **ZWROT:** Topielica przełamuje groblę; osuszony wcześniej basen staje się jedyną suchą drogą odwrotu.
- **Egzamin:** gracz decyduje przed bossem, ile studni zamknąć, a w walce używa suchej grobli i bąbli, by przewidzieć wynurzenie.
- **Topologia:** `pomost→kładki→tama→hub{zachód/gniazdo→wschód}→grobla→topiel↩tama`; 8 stref, dwa cele w różnej kolejności.
- **Beat:** 0–3 pokaz trucizny bez ataku; 3–8 pijawki z zasadzki i widoczny strzelec; 8–12 wybór śluz i elita; 12–17 boss.
- **Pijawka:** marszczy wodę przed wyskokiem, karze stanie w basenie, po czym daje się zepchnąć z kładki.
- **Mułowiec:** zostawia spowalniający ślad z wyraźnym bulgotem, więc gracz odsuwa się od niego przed ciosem.
- **Kościej bagienny:** jest żywym ptasim szkieletem oblepionym trzciną, ostrzega naciągiem łuku i przegrywa z osłoną pnia.
- **Szeptucha:** to roślinny grzybnik w kukle, nie wyznawczyni wiary; kanał osłony pęka po zwykłych ciosach.
- **Kokoniara (elita):** owija widoczną nitką kładkę, którą gracz przecina ciosami przed skokiem, a jej chybiony skok odsłania kokon Borka.
- **Boss — Topielica, Matka Mułu:** szuka ujścia przez działające studnie, więc wykonane prace zmieniają przewidywalność jej ataku, a nie tylko jej HP.
- **Fazy:** 1 włosy, kałuża, wynurzenie; 2 jaja przy czynnych studniach; 3 pęknięta grobla zmienia suchą trasę; włosy ściągają z krawędzi przeciw kitowaniu.
- **WOW:** w 9. minucie z korony tamy gracz obraca kołowrót i widzi, jak czarne lustro zapada się na trzech poziomach, odsłaniając drewniany dach całej zatopionej osady.
- **Paleta:** ciemna oliwka; mangrowiec, błoto, trzcina, ciemny dąb, bruk, zielona terakota, bursztyn; mętne południe, światło spod wody i trzask żab przechodzący w stuk kołowrotu.
- **Poza walką:** Borek, korba i Dzwon zostają zależnością; wrak, ślady Borka i kocioł wskazują sekrety, lecz Dzwon daje skrócenie trucizny, nie pełną odporność kasującą motyw.
- **Powtórka/nagroda:** losuje się uszkodzona śluza B albo C, co zmienia aktywne studnie; Żeton Grobli, torf i włókno; Trudny łączy odwrót z jednym jajem, Łatwy daje dłuższy bulgot.
- **Granica/spadek/ryzyko:** zachowuję gotową topologię, śluzy, Welę/Borka i Topielicę ([spec 002](../loch-002-bagna-zgnilych-korzeni.md)); usuwam zapowiedź morskiego 003 i nadmierny rozmiar pustych dojść. To torfowisko lądowe, nie przypływ Aktu 2. Zmiana poziomu wody → plan B: dyskretne warianty basenu, bez płynnej animacji.

### 003 — Zielnik Wymazanych Nazw (poz. 3)
- **Fantazja:** odkryć, kto wykreślił z rejestru miejsca, którym nie wolno już rosnąć.
- **Motyw — wstęp:** odcisk liścia na stole i na drzwiach uczy czytania trzech kształtów żył, bez kary.
- **Rozwinięcie:** trafienie w liść o właściwej żyle odcina osłonę archiwalnego owada; fałszywy egzemplarz zdradza się pustym przebiegiem soku.
- **ZWROT:** w sali katalogu oznaczenia na drzwiach są poprawne, lecz gracz widzi żyłę od tyłu; musi odwrócić kierunek odczytu, nie zapamiętywać koloru.
- **Egzamin:** boss przestawia karty zielnika na arenie; trafienie w kartę z dopływem odsłania go, karta bez dopływu go wzmacnia.
- **Topologia:** `przedsionek→stół→pierścień 3 gablot→galeria→katalog→arena↩stół`; 7 sal, pętla wokół gablot i wybór dwóch próbek.
- **Beat:** 0–3 spokojny odcisk; 3–7 wróg pokazuje prawdziwy znak; 7–11 dwa możliwe katalogi i cisza; 11–16 boss.
- **Kornik skryba:** ryje ślad na podłodze przed szarżą, a gracz schodzi z linii i uderza w nieruchomy odwłok.
- **Ćma kart:** szeleszcząc rozrzuca zasłony papieru, które znikają po jednym mocnym ciosie lub obejściu.
- **Liściarz:** osłania się kartą z żyłą, lecz strona bez żyły jest bezbronna dla każdego rodzaju obrażeń.
- **Sekatornik:** głośno zatrzaskuje nożyce przed cięciem, pozwalając zablokować cios i ukarać powrót.
- **Archiwar (elita):** obraca wielką kartę w stronę gracza przed szarżą; uderzenie w stronę z żyłą rozrywa papierowy pancerz.
- **Boss — Kustosz Zapisów:** mechaniczny człowiek z roślinnym grzbietem broni sfałszowanego rejestru; jasny znaczek na piersi pojawia się tylko po poprawnym odczycie przepływu.
- **Fazy:** 1 jedna karta + cięcie; 2 dwie karty i zmyłka odwrotnego odczytu; 3 wyrwane karty tworzą osłony, lecz żadna nie wymaga pamięci poza obserwacją żył.
- **WOW:** w 8. minucie z górnej galerii gracz otwiera księgę wielkości podłogi; z pustych stron wyrastają trójwymiarowe kontury wymazanych sadów.
- **Paleta:** pergaminowa kość słoniowa; brzoza, papierowe mapy, biała terakota, biblioteczki, miedź, liście azalii, jasny mech; słońce przez świetliki, szelest i pojedynczy stuk stempla.
- **Poza walką:** trzy dopasowania liści wskazane tłoczeniami prowadzą do tajnej strony z podpisami budowniczych Sieci; wybór próbki zmienia bossa, nie prawdę fabularną.
- **Powtórka/nagroda:** losuje się położenie dwóch z trzech dopływów; Żeton Zielnika, papier z włókien; Trudny odwraca jedną gablotę, Łatwy zaznacza strzałki.
- **Granica/ryzyko:** czytanie żył służy lądowej botanice, nie morskim ruinom ani runom Aktu 4. Wykrywanie orientacji → plan B: dwa ręcznie zbudowane warianty pomieszczenia, bez obrotu bloków w locie.

### 004 — Przeszczepiona Aleja (poz. 4)
- **Fantazja:** rozdzielić drzewa zszyte w jednego, agresywnego olbrzyma, zanim pochłoną drogę.
- **Motyw — wstęp:** dwa spokojne pnie połączone jasnym zrostem pokazują, że przecięcie zrostu rozdziela dopływ.
- **Rozwinięcie:** pary wrogów wymieniają zdolności przez zrost; gracz może przerwać połączenie zwykłymi ciosami albo wykorzystać wspólny moment odsłonięcia.
- **ZWROT:** sztuczny zrost na gracza przekazuje jego obrażenia pobliskiemu korzeniakowi; szybka pogoń staje się błędem, trzeba przeciąć widoczną nić.
- **Egzamin:** boss łączy dwie kończyny z różnymi atakami; rozcięcie wybranego zrostu zmienia repertuar kolejnej fazy.
- **Topologia:** `brama→dwie aleje↔szkółka→krzyż zrostów→taras→arena↩brama`; 7 stref, pętla wokół szkółki, wybór kolejności uwolnienia pni.
- **Beat:** 0–3 wolny pokaz nici; 3–7 gracz rozdziela parę, która go widzi; 7–11 nić na graczu i odpoczynek; 11–16 boss.
- **Szewnik:** podnosi ostre gałęzie przed połączeniem z sojusznikiem, a przerwanie nici odbiera mu osłonę.
- **Dwaśnik:** bije dwiema łapami w rytmie partnera, więc rozdzielenie pary otwiera bezpieczny bok.
- **Pędlak:** rzuca pędem, który można zablokować, lecz połączony z Dwaśnikiem ma dłuższy zasięg.
- **Korzeniak przeszczepiony:** znane pole spowolnienia obejmuje tylko stronę zasilaną bursztynową nicią.
- **Matecznik zrostu (elita):** zapowiada spięcie trzech pni jednym mocnym pulsem, który gracz przerywa przecięciem dowolnej jasnej nici.
- **Boss — Sadzarka Dwóch Pni:** miała ratować chore drzewa, lecz Sieć zszyła je bez końca; gracz wybiera, którą jasną nić przeciąć i którą rękę osłabić.
- **Fazy:** 1 długa ręka i zrost; 2 dwie ręce tworzą różne sektory; 3 pozostająca ręka ma mocniejszy, wolniejszy zamach z czytelnym powrotem; korzenie blokują bezkarne oddalenie.
- **WOW:** w 7. minucie z tarasu gracz przecina pień, a sześć drzew połączonych jedną koroną rozsuwa się jak kurtyna, odsłaniając centralny sad daleko za doliną.
- **Paleta:** miedziany pomarańcz; modrzew, odarta kora, pomarańczowa terakota, mech, grzybnia, kalcyt, miedź; ostre poranne światło, odgłos pękającego włókna.
- **Poza walką:** blizny na pniach wskazują trzy sekrety; decyzja, który pień zachować, zmienia boczną osłonę i opis atlasu.
- **Powtórka/nagroda:** losuje się para połączonych ról, więc priorytet w walce jest inny; Żeton Zrostu i włókno szczepu; Trudny łączy strzelca z bruiserem, Łatwy opóźnia nić.
- **Granica/ryzyko:** organiczna chirurgia drzew, bez kryształowych odbić Aktu 4. Link obrażeń między encjami może być kruchy → plan B: link daje tylko osłonę/atak, z czytelną liną cząsteczek.

### 005 — Młyn Jednostronnych Nasion (poz. 5)
- **Fantazja:** zatrzymać młyn, który miele przyszłe plony peryferii, by nakarmić tylko centrum.
- **Motyw — wstęp:** wolny przenośnik przesuwa worek i pokazuje, jak wrócić bocznym pomostem.
- **Rozwinięcie:** taśma przenosi także wrogów i ziarna osłon; przekierowanie podajnika daje graczowi osłonę kosztem krótszej drogi wroga.
- **ZWROT:** młyn rusza wstecz i bezpieczne dotąd osłony jadą w stronę ostrzy; gracz musi zmienić trasę, nie biec szybciej.
- **Egzamin:** boss obraca dwa segmenty taśmy w przeciwne strony; trafienie w przełącznik w oknie po zamachu tworzy korytarz do jego rdzenia.
- **Topologia:** `rampa→taśma A→sortownia↔mostek↔taśma B→sterownia→arena↩rampa`; 7 sal, serpentyna z poprzecznym skrótem i wyborem dwóch przekładni.
- **Beat:** 0–3 nauka taśmy; 3–8 wrogowie spływają pojedynczo z widocznego podajnika; 8–12 odwrotka, przerwa przy sterowni; 12–17 boss.
- **Mącznik:** wyskakuje z worka po widocznym drżeniu płótna, a odsunięcie worka na taśmę ujawnia go przed atakiem.
- **Wózkowy:** sygnalizuje prostą szarżę stukiem kół, z której schodzi się w boczny pomost.
- **Przesiewacz:** zasłania się sitem, lecz odsłania się podczas wyrzutu ziarna i można mu przerwać serię.
- **Szpulnik:** przywiązuje gracza do taśmy widoczną linką, którą przecina dowolny cios albo wyjście za słup.
- **Młynarz ziarna (elita):** przestawia bieg po trzech wyraźnych stukach, a gracz może wykorzystać tę zapowiedź do wejścia za jego sito.
- **Boss — Dozorca Młyna:** wykonał rozkaz ratowania zbiorów, więc w walce broni kierunku transportu, a nie własnej skóry.
- **Fazy:** 1 taśma i sito; 2 przeciwne biegi + szarża; 3 zerwane worki tworzą chwilowe osłony; jego koło dociąga gracza z krańca, bez teleportu.
- **WOW:** w 6. minucie z kładki nad halą gracz widzi setki nasion na taśmach, a po odwróceniu koła jasny strumień po raz pierwszy płynie ku ciemnym oknom domów na obrzeżu.
- **Paleta:** zbożowe złoto; dębowe deski, siano, kamień młyński, miedź, żółta terakota, ciemny dąb, worki z Nexo; smugowe południe, rytmiczny stuk zębatek.
- **Poza walką:** rozsypane ziarno wskazuje schowki i pochodzenie transportu; wybór przekładni zmienia trasę dostępu do bossa i jedną osłonę.
- **Powtórka/nagroda:** losowany kierunek pierwszej taśmy zmienia stronę wejścia starć; Żeton Młyna, mączka nasienna; Trudny zmienia bieg w środku starcia, Łatwy po nim.
- **Granica/ryzyko:** to wiejski młyn z żywą Siecią, nie głębinowa fabryka Aktu 4. Płynny ruch taśm może być nieczytelny/ciężki → plan B: stałe segmenty z przenoszeniem co kilka taktów i oznaczonym kierunkiem.

### 006 — Tarasy Ciężkich Plonów (poz. 6)
- **Fantazja:** przejść wiszące tarasy sadu, na których każdy kosz może uratować albo pogrzebać drogę.
- **Motyw — wstęp:** pusty kosz na huśtającym się pomoście uczy równowagi bez wroga.
- **Rozwinięcie:** platformy mają dwie stabilne pozycje zależne od ciężaru zbioru; gracz przenosi jeden kosz, by podnieść osłonę lub otworzyć skrót.
- **ZWROT:** elita strąca kosz i zmienia pozycję tarasu podczas walki; bezpieczna wysokość staje się linią ciosu.
- **Egzamin:** boss przetacza wielki owoc między szalami areny; gracz wybiera, gdzie stanie osłona, a gdzie odsłoni się jasny rdzeń.
- **Topologia:** `podnóże→dół↗dwa tarasy↘ważnia→most→arena↩podnóże`; 7 sal, pionowa ósemka i skrót windą, wybór kosza.
- **Beat:** 0–3 równowaga; 3–8 gracz zaskakuje strażnika z wyższego tarasu; 8–12 zrzut kosza i spokojne ważenie; 12–17 boss.
- **Zbieracz:** podnosi kosz wysoko przed ciosem, więc blokuje się zamach i wykorzystuje ciężki powrót.
- **Szkodnik tarasu:** podgryza linę z widocznym pyłem, a uderzenie go od strony liny zatrzymuje uszkodzenie.
- **Gruszak:** turla owoc po oznaczonej rynnie, której można użyć przeciw niemu po zejściu z toru.
- **Strażniczka kosza:** trzyma tarczę z przodu, ale ciężar opuszcza jej bok podczas zwrotu platformy.
- **Łamacz podpór (elita):** po długim uniesieniu młota uderza w oznaczoną podporę, a gracz przerzuca kosz, by zmienić cel ciosu.
- **Boss — Ważniczy Obfitości:** wierzy, że równy bilans na wadze znaczy sprawiedliwość; jego pancerz odpada, gdy gracz celowo przechyli szalę.
- **Fazy:** 1 przerzut owocu i ciężki zamach; 2 tarasy zamieniają wysokość; 3 jeden ciężar pęka, tworząc nowy poziom; lina bossa napina się przy próbie stania daleko.
- **WOW:** w 8. minucie, po wejściu na najwyższy taras, gracz widzi złote sady unoszące się kaskadą nad pustą doliną; po przestawieniu kosza jedna kaskada opada z hukiem na właściwe podpory.
- **Paleta:** śliwkowy fiolet; ciemna śliwa z Nexo, purpurowa terakota, drewno akacji, łańcuchy, miedź, mech, kosze; zachód, światło spod liści, skrzyp lin.
- **Poza walką:** zarys odciśniętego kosza wskazuje trzy tajne półki; wybór ciężaru ujawnia zapis strat z dalszej doliny.
- **Powtórka/nagroda:** losuje się taras z ciężarem startowym, co zmienia flankę; Żeton Tarasów i suszone owoce; Trudny strąca kosz przy drugim ataku, Łatwy dopiero po starciu.
- **Granica/ryzyko:** to fizyczny ciężar zbiorów, nie mroźny teren ani forteczne tarcze. Ruch platformy w MD → plan B: dwie stałe wysokości z bezpiecznym przeniesieniem graczy i wyraźnym odliczaniem.

### 007 — Dzwony Cichego Traktu (poz. 7)
- **Fantazja:** prześlizgnąć się przez trakt chroniony alarmami, nie zostawiając z tyłu bezbronnych mieszkańców.
- **Motyw — wstęp:** pusty dzwon i widoczny promień czujnika uczą, co go uruchamia; nie ma wymogu skradania klasowego.
- **Rozwinięcie:** gracz może wybrać krótką, głośną drogę albo dłuższe odcięcie przewodu; alarm przywołuje patrol z istniejącej trasy, nie generuje fal bez końca.
- **ZWROT:** Iga kieruje alarm na transport, nie na gracza; najbezpieczniejsza dla gracza droga naraża woźnicę, więc trzeba odwrócić uwagę własnym ruchem.
- **Egzamin:** boss sam jest dzwonnikiem; jego uderzenie otwiera chwilowo plecy, ale budzi czujniki w sektorze, który gracz wcześniej zostawił zasilony.
- **Topologia:** `brama→trakt z dwiema równoległymi drogami↔posterunek→wozy→dzwonnica→arena↩brama`; 8 sal, pętla patrolowa, skrót po odcięciu kabla.
- **Beat:** 0–3 test czujnika; 3–8 widoczny patrol, gracz może go zaskoczyć; 8–12 alarm przy wozie i cisza w schowku; 12–17 boss.
- **Gwizdak:** krótkim gwizdem sygnalizuje wezwanie patrolu, przerywane ciosami albo osłoną za wozem.
- **Sękowy tropiciel:** śledzi odcisk gracza na ziemi, lecz gubi go po przekroczeniu korzeniowej bariery.
- **Woźnik automatu:** szarżuje w linii toru po stukocie kół, a zejście z drogi otwiera plecy.
- **Dzwonnik mały:** podnosi młotek przed stożkiem dźwięku; blok tłumi cios, a ominięcie stożka daje kontrę.
- **Nadzorca wozów (elita):** gwizdkiem kieruje patrol na oznaczony sektor, więc gracz może zwabić go pod własny wóz i przerwać rozkaz.
- **Boss — Wielki Dzwonnik:** strażniczy mechanizm przerobiono z alarmu ratunkowego na ochronę ładunku, dlatego jego jasny dzwon odsłania serce po każdym własnym sygnale.
- **Fazy:** 1 pojedynczy sektor dzwonów; 2 fałszywy sygnał przed prawdziwym; 3 pęknięty dzwon skraca sektor, ale przyspiesza rytm; nie można stać poza areną, bo dźwięk idzie po ścianie.
- **WOW:** w 10. minucie z okna dzwonnicy gracz widzi cały trakt zapalający się bursztynowymi punktami alarmu; po odcięciu przewodu światła gasną od najdalszej wioski ku niemu.
- **Paleta:** granat atramentowy; ciemny dąb, łupek, mosiężna miedź, bruk, liście świerku, płótno, latarnie; noc bez magii księżyca, dźwięk dzwonu i oddech koni.
- **Poza walką:** ślady kół i zerwane sznurki wskazują skrytki; wybór ochrony woźnicy zmienia ocenę i dialog końca, nie moc łupu.
- **Powtórka/nagroda:** losuje się czynny przewód północny/południowy, zmieniając trasę patrolu; Żeton Traktu i mosiężny język; Trudny opóźnia wyłączenie alarmu, Łatwy pokazuje obszar na ziemi.
- **Granica/ryzyko:** cywilny trakt, bez wojny frakcji Aktu 7 i bez wymogu Ninji. AI patrolu i alarmy mogą przeciążyć instancję → plan B: dwa stałe patrole oraz jeden stan alarmu bez dynamicznego pathfindingu.

### 008 — Dom Głodnych Zapylaczy (poz. 8)
- **Fantazja:** przywrócić drogę owadom, które zapomniały wszystkie kwiaty poza jednym sadem.
- **Motyw — wstęp:** spokojny owad przenosi widoczny pyłek między dwiema donicami; gracz blokuje mu drogę własnym ciałem i widzi zmianę.
- **Rozwinięcie:** zapylacze lecą prostą, oznaczoną smugą; przestawienie donicy zmienia cel lotu i to, która roślina daje osłonę lub zagrożenie.
- **ZWROT:** boss znacznikiem pyłku wyznacza *gracza* jako kwiat; ucieczka w narożnik ściąga owady, więc trzeba poprowadzić je przez pustą donicę.
- **Egzamin:** przestawiane donice tworzą trzy możliwe trasy lotu; tylko jedna zapyla kwiat odsłaniający bossa, ale pozostałe pozostają użytecznymi osłonami.
- **Topologia:** `próg→szklarnia→hub z 3 grządkami↔górny pomost→matecznik→arena↩próg`; 8 sal, hub i wybór dwóch zapyleń.
- **Beat:** 0–3 obserwacja lotu; 3–8 gracz prowokuje spokojny rój i poznaje linię; 8–12 pyłek na graczu, odpoczynek nad matecznikiem; 12–17 boss.
- **Pszczoła przewodnia:** przed prostym lotem unosi odwłok i smugę, którą gracz może przeciąć osłoną.
- **Trzmiel tarczowy:** osłania małe owady, lecz po przelocie ma długi, odsłonięty powrót.
- **Modliszka kwiatowa:** udaje donicę, ale ruch pyłku zdradza ją przed chwytem, kontrowanym bocznym krokiem.
- **Pyłkowiec:** zostawia na graczu żółty znacznik po zapowiedzi kichnięciem; znacznik zmywa się przy pustej donicy.
- **Opiekun ula (elita):** zamyka wlot widocznym wiekiem, które otwiera po locie trzmiela; wtedy każdy archetyp ma krótkie okno na jasny zawias.
- **Boss — Królowa Jednego Kwiatu:** Sieć nauczyła ją jednego adresu pożytku; wybór trasy owadów zmienia, czy broni serca, czy odsłania je przy karmieniu.
- **Fazy:** 1 lot po jednej linii; 2 pyłek na graczu i dwa tory; 3 rozkwit bocznych donic daje krótkie okna na jasny odwłok; pyłkowa smuga dociera też do obrzeży areny.
- **WOW:** w 9. minucie, z górnego pomostu, gracz przekłada ostatnią donicę: przez szklany dach widać wielki pas kwiatów rozkwitający wzdłuż nowej trasy owadów aż poza horyzont instancji.
- **Paleta:** mleczny turkus; szkło, prismarine tylko jako barwa szkła, jasny dąb, azalia, mech, biała terakota, ule, kalcyt; południe, rozproszone światło, brzęk skrzydeł zamiast muzyki ognia.
- **Poza walką:** wzory pyłku na parapetach wskazują cztery ukryte donice; wybór drugiego zapylenia ujawnia list od pszczelarzy do Igi.
- **Powtórka/nagroda:** losuje się układ dwóch czynnych donic, co zmienia bezpieczny tor; Żeton Pyłku i wosk do alchemii; Trudny nakłada dwa zapylacze, Łatwy jeden.
- **Granica/ryzyko:** szklarnia jest rolnicza, bez erupcji Aktu 8 i bez kosmicznych świateł. Lot owadów po torach → plan B: niewidzialne stałe punkty trasy i wyraźne smugi, bez swobodnego AI roju.

### 009 — Wylęgarnia Nasion Zastępczych (poz. 9)
- **Fantazja:** zatrzymać idealne sadzonki, które zajmują miejsce wszystkiego, co rośnie krzywo.
- **Motyw — wstęp:** pusty kokon pęka po czasie; uderzenie go przed wykluciem zachowuje naturalną roślinę obok.
- **Rozwinięcie:** pokonani wrogowie zostawiają nasiona; trzeba wybrać, które zgnieść najpierw, a które wykorzystać do otwarcia żywej bramy.
- **ZWROT:** zgniecenie *wszystkich* nasion odcina dopływ do wyjścia i osłony, więc potrzebne jest jedno kontrolowane kiełkowanie.
- **Egzamin:** boss tworzy trzy nasiona o różnych oznaczeniach; gracz chroni jedno, niszczy dwa, a właściwy kiełek odsłania jego rdzeń.
- **Topologia:** `izba→spirala 3 komór↔inkubator→ogród prób→arena↩izba`; 7 sal, spirala z wyborem kolejności komór i skrótem przez kiełek.
- **Beat:** 0–3 pierwszy kokon bez zagrożenia; 3–8 przeciwnik zostawia nasiono pod presją; 8–12 zwrot i cicha izba prób; 12–18 boss.
- **Łupnik:** osłania dojrzewające nasiono pancerzem, który pęka po naładowanym ciosie lub obejściu.
- **Nasionożer:** biegnie do nasiona po widocznym skręcie głowy; przecięcie trasy chroni wybrane kiełkowanie.
- **Kiełkownik:** wyrasta tylko ze świecącego kokonu, więc gracz może zniszczyć go przed narodzinami.
- **Zastępca:** kopiuje ruch poprzedniego wroga z opóźnioną zapowiedzią; blok albo odsunięcie karze jego przewidywalność.
- **Ogrodnik próbny (elita):** trzyma dwa kokony w dłoniach i wskazuje wcześniej ten, który wypuści; trafienie w wskazaną dłoń przerywa wylęg.
- **Boss — Matka Zastępcza:** powstała, by uzupełniać straty, lecz zastępuje również żywe rośliny; każda decyzja o nasionie zmienia jej ochronę.
- **Fazy:** 1 pojedynczy kokon; 2 trzy nasiona, każde z innym kierunkiem żyły; 3 ocalały kiełek tworzy osłonę i lukę na jasny rdzeń; boss wraca ku aktywnemu inkubatorowi, więc nie da się go wyciągnąć na pusty korytarz.
- **WOW:** w 11. minucie, zza szyby izby prób, gracz widzi tysiące identycznych sadzonek układających się w równy las; po ocaleniu jednego krzywego nasiona linia symetrii pęka na całej hali.
- **Paleta:** kredowa biel; kalcyt, biała terakota, brzoza, jasny mech, bone block jako wapno, szkło, suche liście; sterylne światło, miarowe pękanie łupin.
- **Poza walką:** wadliwe, krzywe sadzonki wskazują trzy sekrety i pokazują, że „błąd” bywa zdrowy; wybór ocalałego gatunku zmienia obraz finału.
- **Powtórka/nagroda:** losuje się żywotny gatunek nasiona, z odczytem po żyle, więc priorytet celu się zmienia; Żeton Wylęgarni i łupiny do zielarstwa; Trudny skraca czas kiełkowania, Łatwy go wydłuża.
- **Granica/ryzyko:** żywe rośliny, nie nekromancja, mumie ani obeliski Aktu 3/7. Liczba aktywnych kokonów może dławić TPS → plan B: maksymalnie trzy encje i reszta jako bloki/efekty.

### 010 — Serce Sieci Rozdziału (poz. 10)
- **Fantazja:** stanąć przed osobą, która naprawdę uratowała wyspę przed głodem, i odmówić jej „jednego doskonałego sadu”.
- **Motyw — wstęp:** na modelu trzech miniaturowych dolin gracz widzi, że jeden otwarty dopływ osusza dwa inne.
- **Rozwinięcie:** trzy ramiona Sieci dają różne narzędzia walki: osłonę, krótką drogę albo okno obrażeń; nie można zasilić wszystkich naraz.
- **ZWROT:** Iga odłącza centralne źródło; gracz musi zamknąć wcześniej wybrane udogodnienie, by obieg wrócił boczną gałęzią.
- **Egzamin:** wybór rozproszonego obiegu w trakcie walki ratuje dwa obrzeża i osłabia centrum; boss testuje odczyt dopływu, konsekwencję wyboru i ruch pod presją.
- **Topologia:** `widok bramy→hub 3 gałęzi↔galeria kosztów→ognisko→serce→arena↩hub`; 9 stref, trzy cele w dowolnej kolejności i skrót do ogniska.
- **Beat:** 0–4 cichy model; 4–9 trzy gałęzie z innymi inicjacjami starć; 9–12 decyzja i pełna cisza; 12–20 Iga + finał bez dodatkowej fali.
- **Strażnik przewodu:** staje na aktywnej żyle i osłania ją, ale frontalny zamach zdradza wolny bok.
- **Liściowy rachmistrz:** zaznacza przyszłe pole cięcia, więc zejście poza wzór otwiera przerwanie.
- **Dostawca soku:** przenosi ładunek między ramionami i pokazuje trasę, którą można mu przeciąć zwykłym ciosem.
- **Korzeń zwrotny:** wyrasta za graczem po widocznym pulsie, zmuszając go do przemieszczenia bez zamknięcia areny.
- **Przełożony zaworów (elita):** zamyka najbliższą żyłę po bursztynowym sygnale, lecz mocny cios w jasny klucz otwiera ją na krótko.
- **Boss — Iga, Mistrzyni Przesiewu:** zamyka przewód, który gracz ostatnio wybrał, więc każda faza każe oddać wygodę, którą sam zbudował; jasny punkt to ręczny klucz rozdziału, nie jej twarz.
- **Fazy:** 1 jedna gałąź i ciężki klucz; 2 Iga odwraca przepływ, zmieniając osłony; 3 rozproszony obieg tworzy trzy krótkie okna ataku; jej klucz ciągnie się po aktywnej żyle, karząc długie kite'owanie.
- **WOW:** w 16. minucie ze środka areny gracz otwiera boczny obieg: przez trzy okna widzi trzy odległe doliny zapalające się kolejno zielenią, podczas gdy idealny sad nad nim blednie do zwyczajnej barwy.
- **Paleta:** bursztynowy miód; gładki dąb, miedź, kamienne cegły, żywe liście, mech, złota terakota, szkło, ciemna ziemia; światło tętni zgodnie z przepływem, dźwięk pompy staje się oddechem.
- **Poza walką:** listy o dawnym głodzie, plan bez podpisu autora i ręczny klucz tworzą trzy wskazane sekrety; wybór gałęzi zmienia ułatwienie w walce i finałową ilustrację, nie zamyka aktu.
- **Powtórka/nagroda:** losuje się gałąź, którą Iga zamyka jako pierwszą; Żeton Obiegu, Rdzeń Sieci do receptur Aktu 1 i EXP; Trudny zamyka drugie ramię po znanej zapowiedzi, Łatwy jedno.
- **Granica/ryzyko:** finał tego krajobrazu, bez kolumnowego miksu lodu/ognia/kości i bez panteonu. Trzy stany obiegu w MD/BlockEditor → plan B: trzy stałe warianty areny wybierane przy wejściu, z tym samym wyborem taktycznym.

## D. Macierz różnorodności

| Loch | Mechanika motywu | Topologia | Start starć dominujący | Haczyk bossa | Dominujący kolor | Emocja |
|---|---|---|---|---|---|---|
| 001 | latarnie i cień korony | dwa ramiona z tyrolką | gracz widzi łucznika z dołu | odsłoń rdzeń przesuwaną lampą | róż piwonii | zachwyt |
| 002 | osuszanie studni | hub śluz z kładkami | zasadzka z wody | liczba studni zależy od prac | ciemna oliwka | niesmak |
| 003 | odczyt kierunku żył na kartach | pierścień gablot | gracz wybiera okaz do aktywacji | prawdziwa karta otwiera pancerz | kość słoniowa | podejrzenie |
| 004 | przecinanie przeszczepów | skrzyżowane aleje | połączona para ujawnia się dźwiękiem | odcięta ręka zmienia repertuar | miedziany pomarańcz | sprawczość |
| 005 | odwracane podajniki | serpentyna przenośników ze skrótem | przeciwnik nadjeżdża z podajnika | zmiana biegu otwiera rdzeń | zbożowe złoto | oszołomienie skalą |
| 006 | bilans ciężarów | pionowa ósemka tarasów | gracz zaskakuje z góry | przechylona szala gubi pancerz | śliwkowy fiolet | ciężar decyzji |
| 007 | przekierowanie alarmu | równoległy trakt i pętla patrolu | patrol mija kryjówkę gracza | własny dzwon odsłania plecy | granat atramentowy | gniew |
| 008 | trasy zapylenia | hub grządek z górnym pomostem | gracz prowokuje neutralny rój | karmienie odsłania odwłok | mleczny turkus | czułość |
| 009 | kontrolowane kiełkowanie | spirala komór | wróg zostawia nasiono po śmierci | ocalony kiełek łamie osłonę | kredowa biel | lęk |
| 010 | koszt rozdziału obiegu | trzy gałęzie wokół serca | wybrany przewód budzi strażnika | boss zamyka wybrane ułatwienie | bursztynowy miód | odpowiedzialność |

Autokontrola: w każdej kolumnie jest dziesięć odrębnych nazw i odrębny skutek taktyczny. W 002 i 010 widać przepływ, ale 002 zarządza *poziomem wody i kryjówkami*, 010 *rezygnacją z własnego ułatwienia*; 001 i 003 używają światła/odczytu, lecz pierwsze przestawia lampę, drugie rozpoznaje kierunek żyły. Te pary wymagają osobnego playtestu, bo różnica w tekście nie gwarantuje różnicy w rękach gracza.

## E. Korekta struktury całej gry 1–100

1. Zachować dziesięć aktów i zakresy poziomów z [Planu §3](../plan-100-lochow-10-wysp.md); to czytelna nawigacja, nie obowiązek dziesięciu ról K1–K10.
2. Każdy akt ma jedno prawo środowiska i konflikt wynikający z życia jego krainy; każdy loch inną mechanikę motywu, topologię oraz emocję. Prawo powraca jako zasada przyczynowa, nie identyczna interakcja.
3. Przy projektowaniu aktu najpierw rozpisać dziesięć obrazów i dziesięć pytań dramatycznych, potem macierz mechanika × topologia × start walki × boss × paleta; skreślić duplikaty przed listą modeli.
4. Ułożyć rytm nauki w obrębie aktu: wejście, komplikacje, dwie zmiany perspektywy, punkt bez odwrotu, konsekwencja. Numer nie przypisuje tematu „status/ogień/kość”.
5. Między aktami powracać do *umiejętności gracza* (osłona, ruch, przerwanie), lecz zmieniać powód, przestrzeń i koszt decyzji. Akt 10 może przetwarzać wcześniejsze prawa, ale nie musi kopiować dziesięciu kolumn.
6. Wyspa i lochy dzielą krainę, zagrożenie, rodziny wrogów oraz materiały; nie parować numerów, bossów, fabuł, sekretów ani warunków wejścia. Równe tiery łupu i niezależny EXP zostają ([brief §1](00-BRIEF.md)).
7. Każde dwa akty wykonać audyt motywów: jeśli „śnieg”, „kopalnia”, „mumia” lub „meteor” pojawiają się przed własnym aktem w pełnej skali, przesunąć je albo zmienić na lokalny szczegół.
8. Przyszły szablon to *bramka jakości* i karta beatów, nie szablon fabuły. Jeden loch budować i mierzyć naraz; owner playtest decyduje, czy następny jest gotowy do produkcji ([Plan §7](../plan-100-lochow-10-wysp.md)).
9. Do powtarzalności użyć 2–3 wariantów zmieniających taktykę, nie generowania 100 pustych map. Liczyć koszt budowy i utrzymania: dziesięć wyjątkowych bossów na akt to ogromny dług animacji, telemetrii i QA.

## F. Pytania do zespołu i ryzyka

1. **Blokuję** zachowanie odsyłacza z 002 do „zatopionego archiwum” 003; naprawa: odsłonić zatopioną osadę i ślad Sieci, bo morze należy do Aktu 2.
2. **Blokuję** automatyczne przeniesienie 001 v7; naprawa: ciasny blockout z osłonami, pojedynczy motyw latarni, pomiar 12–20 min i ponowna ocena właściciela. Jego 2/10 z briefu jest wystarczającym sygnałem, że sama kosmetyka nie wystarczy.
3. **Najsłabszy 003:** odczyt kierunku żyły może przypominać quiz zamiast walki; warunek przejścia bramki: 8 z 10 testerów rozumie zwrot bez tekstu po jednym pokazie, a błędna karta daje naprawialny koszt.
4. **Najsłabszy 006:** ciężary i tarasy mogą być technicznie ciężkie oraz nużące solo; warunek: wariant dwóch stałych wysokości daje tę samą decyzję i nie wydłuża przebiegu poza 18 min.
5. **Najsłabszy 007:** alarm łatwo staje się wymuszonym stealth, trudnym dla Maga/Akolity; naprawa: głośna trasa jest pełnoprawna, z limitowanym patrolem i tą samą jakością łupu.
6. Czy akceptujemy Igę jako świecką, ludzką opiekunkę systemu, czy lepiej, by finał miał bossa niehumanoidalnego? W obu wariantach odpowiedzialność za zamknięty obieg musi być czytelna.
7. Czy wspólna zmiana hubu jest w ogóle w zakresie Aktu 1? Bez bezpiecznego fazowania zostaje atlas i lokalne finały; nie obiecywać globalnej zieleni po indywidualnym przejściu.
8. Czy materiał „tylko-tu” ma być surowcem każdego lochu, jeśli wyspa musi mieć *równe rodzaje i tiery*? Proponuję różny wygląd/efekt użytkowy, bez przewagi mocy ani przepisu wymagającego obu torów.
9. Każdego bossa trzeba obejrzeć w jednym kadrze z 4–10 bloków, z jednym jasnym punktem słabym i odrębną zapowiedzią; lista modeli LittleRoom jest pulą kandydatów, nie dowodem wykonalności ([Baza bossów §3–5](../../research/projektowanie-bossow/README.md)).
