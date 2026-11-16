# Etap 4 — recenzja krzyżowa X (2026-10-03)

**Bramka:** 00-BRIEF §4: choćby jedno kryterium <7 = **BLOKUJĘ**. Podstawą są R1–R5 z `05-synteza-v2-rozstrzygniecia.md`; oceniam projekt na papierze, nie deklaruję testu na serwerze. Numery `L` niżej oznaczają linie danej karty.

## 003 — Pasieka Królowej Barci — BLOKUJĘ

| Kryterium | 0–10 | Dowód |
|---|---:|---|
| Fun walki | 8 | Przekierowanie roju po szarży daje aktywną decyzję zamiast czekania na odsłonę (L72–88). |
| Mechanika i zwrot | 8 | Dym zabiera kwiat, lecz pozostawia znaną kontrę wnęki i późniejszy transfer (L13–19). |
| Czytelność | 7 | Biały kwiat, złota nić i tor są opisane, ale ruchomy cel królowej oraz dwa kwiaty naziemne wymagają próby widoczności (L18, 76, 145). |
| Topologia | 6 | Skrót „po F” prowadzi przez S, lecz mapa łączy S tylko z H, więc przed otwarciem bramy H powrót nie jest rozrysowany (L31–40, 53–54). |
| Piękno i WOW | 9 | Ujęcie śpiącej królowej przez bursztynowe szkło ma miejsce, minutę, kompozycję i wykonanie (L96–104). |
| Sens fabularny | 8 | List transportowy i prośba Hanny wyjaśniają eksperyment oraz cenę zniszczenia ula (L21–25). |
| Powtarzalność | 7 | Zasklepione krawędzie i miejsce podkurzacza zmieniają dojście i pierwszą kontrę, choć warianty grafu nie są rozpisane (L113–116). |
| Nagroda/progres | 7 | Wosk, lokalne mleczko i pełna pula drużynowa są nazwane, choć wartość sprzętu czeka na formułę (L118–122). |
| Wykonalność | 6 | Kluczowa kontra wymaga wspólnego `~onDamaged` dla LPM, zaklęcia, peta i przywołańca; „plan B” pozostaje tym samym nieprzetestowanym hitboxem (L89–94, 132, 150). |
| Różnorodność | 8 | Rój kierowany znamieniem różni się od szarży Tura, choć obaj bossowie dają okno po chybieniu (L17–19, 88; 006 L86). |

**Przebieg solo, Mag 3 / Wojownik 3:**

| Sala | Mag | Wojownik |
|---|---|---|
| A | Widzi tor bez kosztu i wnękę. | Widzi to samo oraz bramę H. |
| B | Strzela w kwiat; test, czy pet nie przejmuje celu. | Trafia kwiat LPM i odchodzi do wnęki. |
| C | Może obejść wosk przez E; ściana musi blokować pocisk. | Rozbija szew lub wybiera E bez czekania 8 s. |
| D | Znamię wymusza odczyt toru; ma dystans do kwiatu. | Blokuje żądło i idzie do kwiatu przez próg z petem. |
| E | Czyta dowód, widzi królową i odpoczywa. | Robi to samo po C/D. |
| G | Dym wymaga osłony lub trafienia podkurzacza. | Dociera do szwu, jeśli drugi próg zostaje otwarty. |
| F | Wnęka chroni podczas dymu, potem pocisk w kwiat. | Wnęka i blok działają; skrót do A nie istnieje na mapie. |
| S | Powrót do ogniska jest obiecany, lecz niedostępny z F. | Ten sam problem po walce F. |
| H | Filar może dać darmowy ostrzał, jeśli rój nie wymusza ruchu. | Sięga naziemnego kwiatu po bloku; pet nie może zepsuć transferu. |

**Poprawki obowiązkowe:**
- Brak drogi skrótu → L31–40, 53: F nie łączy się z S, a H jest za plombami → dorysować i opisać przechodnie F→S→A dostępne przed H; sprawdzić oba porządki C/D.
- R2 pozostaje hipotezą techniczną → L18, 89–94, 132, 150: ten sam ruchomy kwiat ma przyjąć każdą formę ataku, w tym autonomicznego peta → zbudować na klonie hitbox zwykłego moba, zmierzyć każde źródło i synchronizację transferu; jeśli pet nie może celować, przenieść kontrę na naziemny węzeł trafialny także przez jego atak, bez skrócenia 3 s.
- Losowanie może przeciąć ważną drogę → L39–40, 114: „dwie z trzech krawędzi” bez listy wariantów → podać trzy konkretne grafy, dla każdego drogę A→C/D→F→S oraz widoczne oznaczenie w A.

**„Suche i bez sensu” (§4):** wskazówki **OK** (L107–110, 147); ≤1 komunikat **OK w projekcie** (L147); obietnice **WARUNKOWO** (działający skrót nie wynika z mapy); nagroda **WARUNKOWO** (brak statystyk); cel gracza **OK** (plomby C/D); kara za ignorowanie **OK** (rój 2×5, L63); skitowanie **WARUNKOWO** (wnęki muszą mieć drogę wyjścia i zasięg bossa, L87, 141). Mag solo: z osłony rozumie kwiat, lecz bez testu trafienia zaklęciem nie wiadomo, czy egzamin działa; Wojownik solo: blokuje szarżę i sięga naziemnego kwiatu, o ile próg nie zamknie powrotu.

**R1:** projektowo tak, śmierć gracza ma dopuszczony plan B (L8–11). **R2:** tabela jest, wykonanie warunkowe do testu wszystkich źródeł. **R3:** tak — Woszczarka zasklepia przejście, nie spowalnia (L62).

## 006 — Wąwozy Tura — BLOKUJĘ

| Kryterium | 0–10 | Dowód |
|---|---:|---|
| Fun walki | 8 | Gracz wybiera murek lub przesmyk, a odłam zmienia kolejne gesty Tura (L76–93). |
| Mechanika i zwrot | 8 | Tur niszczy znaną osłonę, po czym klinuje się w naturalnej szczelinie (L13–18). |
| Czytelność | 7 | Ślady, zamknięta linia i biała podstawa rogu są jawne, lecz ścieżka drugiego spotkania jest niejasna (L40, 55, 79). |
| Topologia | 6 | C i D mają być równoprawnym pierwszym wyborem, ale karta nie wyznacza stanu dwóch przynęt/odłamów dla obu tras i prowadzi F bezpośrednio z D (L31–42, 51–57, 142). |
| Piękno i WOW | 8 | Most daje dwa punkty widzenia na kwitnące poroże i stado w konkretnej minucie (L101–109). |
| Sens fabularny | 8 | Ostrzeżenie Darnika przy wypuszczonym Turze i otwarcie zagrody spinają przyczynę oraz skutek (L22–26, 78, 93). |
| Powtarzalność | 7 | Dwie trasy i jeden murek zmieniają przygotowanie, jeśli zostaną rozpisane bez kolizji bramy (L118–121). |
| Nagroda/progres | 7 | Poroże i wrzos są lokalne, pełny łup nie zależy od kolejności, lecz liczby EXP czekają na krzywą (L123–126). |
| Wykonalność | 7 | Dwa oznaczone wyloty i statyczny most mają wykonalne plany B, ale kolizję dużego moba trzeba sprawdzić w blockoucie (L133–144). |
| Różnorodność | 8 | Trop i wybór geometrii tworzą inną decyzję niż znamię 003, mimo wspólnego wzoru „szarża → pudło → bij” (L7, 86; 003 L81). |

**Przebieg solo, Mag 6 / Wojownik 6:**

| Sala | Mag | Wojownik |
|---|---|---|
| A | Czyta trop bez walki. | Próbuje murku i widzi bramę H. |
| B | Ostrzeliwuje z góry; półka nie może być bezkarna. | Uczy się zejścia z szarży i osłony od półki. |
| C | Pokonuje wilki i kładzie przynętę; przy D-first nie zna numeru spotkania. | Rozdziela parę w przesmyku; ta sama niejasność odłamu. |
| M | Z góry czyta trasę, z dołu zyskuje osłonę. | Wybiera informację albo bezpieczeństwo; upadek kosztuje czas. |
| D | Przecina sieć pociskiem, lecz przynęta wymaga jasnego stanu. | Przecina sieć LPM; D-first musi dać pierwszy odłam. |
| E | Zwiastun może być pusty przy trasie południowej. | Sprawdza rysę, nie powinien czekać na spóźnionego Tura. |
| F | Klinuje Tura, ale półka może dać darmowy ostrzał. | Ucieka z rozbitego murku do szczeliny i bije po utknięciu. |
| G | Odczytuje dwa skoki elity z półki. | Blokuje pierwszy, obchodzi drugi, wraca przez S. |
| S | Zjazd do A kosztuje 12 s, bez softlocka. | Wraca do ogniska bez ponownej walki. |
| H | Dystans pozwala karać utknięcie; testować bezpieczną półkę. | Blok i cios w klin działają, jeśli Tur dociera do obu szczelin. |

**Poprawki obowiązkowe:**
- Brama i kolejność są niedookreślone → L15–19, 40–42, 51–57, 142: pierwszy wybór może być D, ale F jest już na tej gałęzi, a dwa odłamy mają wymagać dwóch różnych przynęt → rozpisać stan C-first i D-first krok po kroku (aktywacja, spotkanie, odłam, odblokowanie F/H, reset po śmierci) i dopasować beat chart obu tras.
- Pozorna zmiana trudności → L56, 131: Wilczyca jest w G już na Normalnym, a lista Trudnego ponownie ją wymienia → wskazać dodatkowego Muflona i jego osobną, czytelną zapowiedź jako rzeczywistą zmianę albo wybrać inną.
- Przesmyk może stać się bezkarną stałą pozycją → L80, 86, 92, 144: dwie skalne kontry zostają zawsze, a trasy są stałe → w blockoucie pokazać zasięg Rogów/Ryku i tor dojścia do obu szczelin; udokumentować próbę Mag/Wojownik bez trafień Tura przez całe H i poprawić bezpieczną półkę, jeśli taka istnieje.

**„Suche i bez sensu” (§4):** wskazówki **OK** (L112–115, 143); ≤1 komunikat **OK w projekcie** (L151); obietnice **WARUNKOWO** (dwa odłamy w obu trasach); nagroda **WARUNKOWO** (brak liczb EXP i efektów sprzętu); cel gracza **NIE** w alternatywnym porządku przynęt; kara za ignorowanie **OK** (9+odrzut, L86); skitowanie **DO TESTU** (szczeliny, L80, 92). Mag solo: z mostu wie, gdzie stanąć, lecz może bezkarnie ostrzeliwać z półki; Wojownik solo: blok/zejście z linii są jasne, ale kolejność drugiego odłamu nie jest.

**R1:** tak, lokalna przynęta dojrzewa po stałych 5 s, pąk gracza ma plan B (L8–11). **R2:** tabela elity i bossa jest, bez bramy na źródło trafienia (L94–99). **R3:** terenowy zwrot, dwie losowane trasy, Most i gwarantowany kontakt są; sekwencja tras wymaga doprecyzowania (L40–43, 58).

## 009 — Dożynki w Malowanej Wsi — BLOKUJĘ

| Kryterium | 0–10 | Dowód |
|---|---:|---|
| Fun walki | 8 | Pasy, zatoki i trafienie tancerza tworzą ruchową walkę także bez muzyki (L71–87). |
| Mechanika i zwrot | 6 | Uciszenie Bębna ma być zwrotem F, lecz gracz może go ominąć, a losowanie może umieścić go wcześniej w D (L15–18, 50–53, 115). |
| Czytelność | 7 | Oświetlone płyty i poza działają bez dźwięku, ale „4” bywa jednocześnie pauzą i chwilą uderzenia pasa (L13–14, 145). |
| Topologia | 8 | Dwa cele C/D, obwód chat i skrót S dają wybór drogi bez wymuszenia bębna (L27–42). |
| Piękno i WOW | 9 | Nieruchomy tłum obracający głowy ma czytelny kadr, minutę i oszczędny koszt encji (L96–104). |
| Sens fabularny | 9 | Uchwała wsi, talerze ras i żywi ocalali pokazują sprawczość mieszkańców bez wybielania skutku (L20–25, 88). |
| Powtarzalność | 7 | Położenie pierwszego pasa zmienia ganek, lecz wariant bębna podważa kolejność nauki (L113–116). |
| Nagroda/progres | 7 | Materiał i rodzina łupu są lokalne, bez kary za decyzję moralną, ale sprzęt nie ma jeszcze wartości liczbowej (L118–122). |
| Wykonalność | 6 | Osobny ruchomy hitbox tancerza i zegar fazowy są nieprzetestowane; plan B automatycznej odsłony zmienia egzamin z trafienia w pauzie (L18, 72, 131–132). |
| Różnorodność | 6 | Snop z pąka jako osłona przed pasem powtarza pąk-osłonę 004 i kłos-osłonę 008, a Kozioł znów szarżuje po zapowiedzi (L8–9, 64; 004 L15, 008 L6). |

**Przebieg solo, Mag 9 / Wojownik 9:**

| Sala | Mag | Wojownik |
|---|---|---|
| A | Widzi dwa cele i puls płyt. | Rozpoznaje wieniec i bezpieczny obwód. |
| B | Strzela po trzecim kroku; nauka „4” musi odróżnić pas. | Trafia Kozła w pauzie bez ciosu z czerwonego pasa. |
| C | Ganek daje linię na rdzeń i wianek. | Musi dojść do Korowodnika bez aktywnego pasa. |
| D | Chata osłania od Kozła/Bębna, losowanie zmienia moment ciszy. | Tnie drugą wstęgę; nie może ominąć nauki gestów. |
| E | Kadr i wybór wejścia są odpoczynkiem. | Widzi osłony i przyszłą arenę. |
| F | Może ominąć Bęben i cały zwrot. | Może go uciszyć lub legalnie ominąć; obie trasy muszą uczyć ciszy. |
| G | Bez metrum czyta cień Przepiórki; z Bębnem traci lekcję. | Blokuje skok, trafia dziób i korzysta z S. |
| S | Bezpieczny powrót do A jest czytelny. | To samo, bez ponownej walki. |
| H | Ganek może dać skit zza chaty; sprawdzić pas śledzący. | Musi dosięgnąć tancerza w 15 ticków bez wejścia w pas na „4”. |

**Poprawki obowiązkowe:**
- Zwrot może wcale nie nastąpić → L15–18, 50–53, 111, 115: ominięcie Bębna zachowuje wspólne metrum, a losowanie przesuwa go do D → zapewnić w G/H obowiązkowe przejście od wspólnego taktu do osobnych gestów niezależnie od wyboru; Bęben niech zmienia koszt i moment, a nie istnienie zwrotu.
- Niejednoznaczne „4” → L13–14, 61–65, 81: czwarty krok jest jednocześnie bezpiecznym oknem ciosu i wyzwalaczem zagrożeń → pokazać odrębne pasy na ziemi i osobny punkt tancerza, dodać jedną próbną scenę, w której Wojownik i Mag widzą bezpieczną pozycję trafienia.
- Kopia osłony z pąka → L8–9, 84: Snop osłania przed pasem jak Snop 004 przed wzrokiem/kosą i kłos 008 przed linią → nadać dojrzałemu pąkowi działanie związane z pauzą (np. jednokrotne zatrzymanie najbliższego pasa i okno przejścia), bez tworzenia kolejnej statycznej zasłony; zachować 5 s R1.
- Techniczna kontra wymaga dowodu → L72, 78, 132: 15 ticków i ruchomy hitbox muszą przyjąć LPM, zaklęcie, pocisk i pet → zmierzyć trafienia na klonie, rozdzielić zegar wizualny od obrażeń; gdy test padnie, stały naziemny znak aktywny przez ≥15 ticków ma dawać tę samą świadomą odsłonę, zamiast automatu.

**„Suche i bez sensu” (§4):** wskazówki **OK** (L107–110, 145); ≤1 komunikat **OK w projekcie** (L148); obietnice **WARUNKOWO** (zwrot na każdej trasie); nagroda **WARUNKOWO** (brak liczb/efektów); cel gracza **OK** (dwie wstęgi, L39); kara za ignorowanie **OK** (pasy 5–9, L61–64); skitowanie **DO TESTU** (chaty i ganki wobec pasa śledzącego, L75, 87). Mag solo: ganku używa do odczytu, lecz hitbox/15 ticków czekają na pomiar; Wojownik solo: zatoka i blok działają, jeśli znak tancerza nie stoi w aktywnym pasie.

**R1:** tak, o ile Snop zachowa 5 s i identyczny pąk (L8–10). **R2:** koncepcyjnie tak, tabela obejmuje elitę i bossa, wdrożenie wymaga testu hitboxu (L89–94). **R3:** skład i ocalali tak; wymóg pauzy 15 ticków i braku bramy `world.takt` zapisany (L18, 72, 138), lecz zwrot wymaga poprawy.

## 010 — Matecznik — BLOKUJĘ (bez taryfy ulgowej)

| Kryterium | 0–10 | Dowód |
|---|---:|---|
| Fun walki | 6 | F1 blokuje obrażenia do dwóch cięć, a F3 trzy nici odejmują łącznie 36% HP przy starcie poniżej 35%, więc finał kończy się samymi cięciami (L98–100). |
| Mechanika i zwrot | 7 | Spadek z Korony odwraca wspinaczkę, ale pąki z nici wracają do znanego cięcia z 005 (L14–17; 005 L14–15). |
| Czytelność | 5 | Identyczny pąk daje Odrost albo Pniaczka zależnie od nieokreślonego „przy nici”, a boss sieje pąki o innym czasie (L8–9, 80, 99, 104, 108). |
| Topologia | 8 | Trzy konary w dowolnej kolejności, zjazdy do ogniska i spadek do wnętrza tworzą wyraźny kształt (L31–52). |
| Piękno i WOW | 7 | Miniatury dziewięciu wysp i zjazd są obrazowe, lecz opis obiecuje obrót horyzontu, który sam uznaje za niemożliwy (L121–131). |
| Sens fabularny | 8 | Grób Dary i obie drogi opieki nad plonami domykają motywację Darnika (L20–29, 144–146). |
| Powtarzalność | 7 | Dziki konar i szybsza kończyna zmieniają kolejność, choć druga elita zwiększa powtarzanie znanego zrostu (L148–152). |
| Nagroda/progres | 8 | Prawie poziom, rodzina dla klas i osobisty łup są konkretne względem wcześniejszych kart (L154–158). |
| Wykonalność | 5 | Siedem systemów naraz obejmuje śmierć/pąki, węzły, promień do 140 bloków, spadek 100 bloków i `fill`; plany B nie rozwiązują reguły 5 s ani testu każdego źródła (L98–110, 123–131, 165–173). |
| Różnorodność | 5 | Cięcie zrostu/osłony kopiuje egzamin 005, Truteń i Cielę kopiują szarże 003/006, a Pniaczek z 001 wraca bez nowej roli (L59–80; 005 L75–81). |

**Przebieg solo, Mag 10 / Wojownik 10:**

| Sala | Mag | Wojownik |
|---|---|---|
| 1 | Trafia węzeł, obserwuje odrost i uczy się pąka. | LPM tnie i depcze; trzecia próba jest powtórką. |
| 2 | Pniaczki nie są nową lekcją, wybiera konar. | To samo, ma ognisko i widok na serce. |
| 3 | Z dystansu przecina nić Trutni; lot nie może omijać osłon. | Blokuje szarżę, tnie węzeł, walczy jak w 003. |
| 4 | Zboże zasłania Słup: ryzyko szukania hitboxu pod ostrzałem. | Okrąża gardę, szuka Słupa i walczy z czterema ciałami. |
| 5 | Z góry może zabić Muflona przed zapadnięciem mostu. | Upadek kosztuje 30% HP, potrzebuje kontry węzła u stóp. |
| 6 | Widzi miniatury i spada bez obrotu horyzontu. | Sterowanie w locie i lądowanie wymagają próby. |
| 7 | Odczytuje grób i odzyskuje siły. | Odczytuje grób, przygotowuje blok. |
| 8 | Strzela w węzły i serce; F3 trzy cięcia kończą egzamin. | Biega do węzłów, ale F3 nie wymaga ciosu w bossa. |

**Poprawki obowiązkowe:**
- Złamana jedna reguła R1 → L8–9, 80, 99, 104, 108: F2 skraca dojrzewanie do 4 s na Normalnym, a „Larwy-pąki”/„3 pąki” nie mają tej samej zapowiedzi i skutku → wszystkie prawdziwe pąki: ten sam model, 5/8/4 s, ostatnia 1 s świeci, trafienie zawsze usuwa; zasiew bossa nazwać innym obiektem lub podporządkować mu identyczną regułę, z jawną granicą „przy nici”.
- Egzamin kończy się przed walką → L98–100: trzy cięcia po 12% pokrywają całe F3, ostatnie automatycznie kończy walkę → niech cięcia zrywają ochronę/zmieniają arenę, a finał wymaga zadania obrażeń sercu w wyuczonych oknach; osobno policzyć HP i czas dla Mag/Wojownik solo.
- Brak R2 elity i ryzyko samodzielnego cięcia → L76, 113–119: tabela dotyczy tylko bossa, Sękacz wiąże gracza z węzłem u jego stóp → dodać cztery konkretne kontry elity, w tym samodzielny cios Wojownika i zaklęcie Maga w węzeł bez trafiania własnej postaci; testować wszystkie źródła na klonie.
- Niedomknięta logika nici → L16–17, 93–100, 121–126: trzy Główne Nici pękają na Koronie, a później trzy znów trzeba przeciąć w arenie → rozróżnić górne i korzeniowe przewody w obrazie i nazwach albo pokazać ich odrost przed bossem.
- Nieprawdziwa obietnica WOW → L126–131: „horyzont przechyla się o 15°”, po czym karta przyznaje brak obrotu kamery → opisać rzeczywisty widok zapadania Korony i płatków bez obiecywania obrotu; miniatury sprawdzić na docelowym dystansie renderowania.
- Zły zastępnik i brak rzetelnej próby wejścia → L70, 171–172: silverfish jako Pniaczek oraz założone `allow-flight` kolidują z zapisanymi próbami → użyć małego huska, zamrożonego terenu i testu wejścia/upadku prawdziwym klientem na klonie; plan B teleportu ma zachować odkrycie Gardzieli.

**„Suche i bez sensu” (§4):** wskazówki **WARUNKOWO** (granica „przy nici” i trzy nici, L8, 16, 93); ≤1 komunikat **OK w tekście** (L175–179); obietnice **NIE** (obrót horyzontu); nagroda **OK** (L154–158); cel gracza **WARUNKOWO** (F3 auto-koniec); kara za ignorowanie **OK** (Odrost, leczenie, L80, 109); skitowanie **DO TESTU** (rampy i trzy kieszenie, L91–111). Mag solo: z ziemi dosięga górnych węzłów, ale bez potwierdzonego hitboxu/peta i przy F3 walka znika; Wojownik solo: wspinanie jest czytelne, lecz węzeł u własnych stóp i automatyczne −36% odbierają finałowi decyzję o obrażeniach.

**R1:** nie — Normalny F2 ma 4 s i inne „pąki” bez wspólnej reguły. **R2:** nie — brak tabeli elity i sprawdzonej kontry własnego zrostu. **R3:** częściowo — Darnik żyje i wskazano opiekę nad plonami (L144–146), ale walka przegrywa z łamigłówką w F3.

## Powtórki między dziesięcioma lochami i granice wniosków

- **Mechanika — realny duplikat:** 005 L11–15, 75–81 i 010 L14–17, 78–100 uczą „traf w węzeł → zdejmij osłonę → bij”; finał 010 powinien testować decyzje z całego aktu, ale potrzebuje nowego skutku cięcia w walce, nie większej liczby nici.
- **Mechanika osłony z pąka:** 004 L15 tworzy Snop-kryjówkę, 008 L6 niski kłos przeciw linii, 009 L8–9 Snop przeciw pasowi; różnią się detalem, lecz decyzja „zostaw pąk dla osłony” powtarza się trzeci raz — obowiązkowo zmienić 009.
- **Wrogowie i zdolności:** 003/010 używają Trutnia z zapowiedzianą szarżą (003 L61; 010 L72), 006/010 Muflona i młodego Tura (006 L65–66; 010 L74–75), 001/010 Pniaczka (001 L88; 010 L70), a 006/009 Kozioł z `entity.goat.prepare_ram` karze boczną linią (006 L66; 009 L64); krewniacy są dozwoleni, ale 010 musi nadać im nową rolę we współpracy, nie samą nić-osłonę.
- **Bossowie/start:** 003 L81/88 i 006 L86 opierają odsłonę na chybionej szarży; 001 L77, 002 L67, 004 L78, 007 L83, 009 L54 i 010 L64 zaczynają finał widokiem oraz odcięciem odwrotu — użyteczne, lecz zbyt jednolity pierwszy beat; 010 powinien zacząć od decyzji w istniejącej arenie przed zamknięciem Gardzieli.
- **Kadr WOW:** 003 L97, 006 L102, 009 L97 i 010 L123 pokazują ogromny obiekt/bossa z progu, ale sylwetka, pora i ruch są różne; nie blokuję samych kadrów, jedynie niemożliwy obrót w 010.
- **Technicznie:** pamięć `mythicmobs-cicha-walidacja.md` zabrania traktować ciszę `mm reload` jako dowód; `mm-warunki-inline-to-rzucajacy.md` rozdziela warunek rzucającego i celu; `mythicmobs-mechaniki-api-sync.md` wymaga synchronizacji własnych mechanik; `mythicdungeons-funkcje-bez-edytora.md` wskazuje `summon level=1`, bez `delay` z funkcji MD; `mythicmobs-rozstawione-moby.md` odrzuca silverfish i wymaga stałych spawnów. Żadna z czterech kart nie ma jeszcze testu LPM/PPM/zaklęcie/pocisk/pet/przywołaniec na klonie, więc wykonalność 7+ jest wyłącznie oceną projektu, nie certyfikatem wdrożenia.
