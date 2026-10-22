# Projektant Y — propozycja niezależna, plan 100 lochów v2

Status: koncepcja do recenzji, nie decyzja właściciela. Akt 1 v2 i jego liczby/rejestry pozostają zamrożone. Zakładam 10 × 10 lochów (poziomy aktu k: 10k−9…10k); zmniejszenie liczby po Akcie 3 jest osobną decyzją właściciela. Wyspa i lochy dają równy progres, innych bossów i samodzielne opowieści. „Szew” oznacza odkrywaną relację między miejscami, nie drugie prawo aktu ani obowiązkowy portal.

## A. Mocna rzecz — trzy kandydatury

### 1. **Atlas Szwów** — faworyt
- **Zdanie dla znajomego:** „Na tym serwerze sto lochów składa się w jedną ukrytą mapę: trop z późnego aktu potrafi zmienić drogę przez miejsce, które znasz od miesięcy”.
- **Jak działa:** Każdy z 100 lochów ma jeden *fakt topologiczny* do odkrycia (skąd przyszło coś, dokąd prowadzi przewód, kto zamknął przejście). Gracz przypina go w Atlasie; pary lub trójki faktów ujawniają szew między krainami. Około 20–30 szwów to przygotowane przez autorów alternatywne wejścia, krótkie przejścia albo widoki na sąsiednią krainę; reszta to połączenia wiedzy i opowieści. Ścieżka główna, poziom i łup nie wymagają Atlasu. Wyspy dostarczają równorzędnych tropów do tych samych połączeń.
- **Akt 1 bez retconu:** Nie dopisujemy nic do kart 001–010. Już istniejące ślady (statek z sadzonką, Kronika na Mapie Granicy, los Darnika) stają się pierwszymi węzłami Atlasu *poza* instancjami; pierwszy nowy fizyczny szew może wejść dopiero w Akcie 2.
- **Research i uczciwa nowość:** [*Outer Wilds*](https://store.steampowered.com/app/753640/Outer_Wilds/) opiera postęp na wiedzy o zmieniających się miejscach; [*Path of Exile*](https://webcdn.pathofexile.com/forum/view-thread/3008270) ma Atlas map i regionalne modyfikatory; [Monumenta](https://monumenta.wiki.gg/wiki/Point_of_Interest) łączy POI i wejścia do lochów. [Wynncraft](https://wynncraft.com/fruma) oferuje fabułę i rajdy, [Hypixel SkyBlock](https://wiki.hypixel.net/Catacombs_Floor_V) — piętra Catacombs z nagrodami. Każdy składnik ma poprzednika. Nie znalazłem w tych źródłach przykładu *stu ręcznie napisanych instancji MMO na MC*, których fakty budują dwukierunkową, odkrywaną topologię całej kampanii; to propozycja połączenia, nie twierdzenie o światowym pierwszeństwie.
- **Kręgosłup:** 001–010 ustanawiają zagadkę Szczepu; akty 2–9 odkrywają po jednej warstwie sieci i 2–4 szwy; 091–100 wymagają *zrozumienia* jej pochodzenia dla pełnego zakończenia, lecz podstawowy finał jest zawsze dostępny. Pierwszy loch aktu uczy wyłącznie miejscowego prawa, ostatni sprawdza prawo i historię; Atlas nie jest schematem K1–K10 ani drugą walutą mocy.
- **Wykonanie/koszt:** Rejestr 100 faktów i relacji w prostych danych, GUI mapy/Kroniki, warunkowe teleporterowe wyjścia w MythicDungeons/Paper, przygotowane warianty geometrii i test powrotu do punktu wejścia. Najdroższe są poziomy i QA: 20–30 dodatkowych małych odcinków, kontrola spoilerów, poziomów i stanu party. Start: 1 szew w Akcie 2, decyzja o skali po teście. Bez generowania map na żywo.
- **Ryzyka:** Wiedza z wiki może „rozwiązać” mapę; kontrą jest przyjemność znalezienia śladu i użycia skrótu, nie tajne hasło. Alt/teleport nie może ominąć poziomu ani wymusić długiej podróży. Jeśli parser/stan instancji zawodzą, szew jest wskazówką i teleportem z hubu, nie przejściem w walce. Dodatkowy koszt może zjeść jakość samych bossów.
- **Test 3 pytań:** decyzje — tak, gracz wybiera trasę i które tropy śledzi; opowieści — tak, „znaleźliśmy drogę z portu do kopalni przez dawny spichlerz”; powrót miesiącami — tak, kolejne akty odkrywają nowe relacje starych miejsc, ale tylko jeśli regularnie powstaje treść.

### 2. **Kronika Świadków** — świat zapisuje sprzeczne relacje graczy
- **Zdanie dla znajomego:** „Na tym serwerze to, co ocaliłeś w lochu, staje się jednym z widocznych świadectw historii twojej wyspy”.
- **Jak działa/kręgosłup:** Każdy loch i strefa wyspy dają jeden materialny dowód dotyczący konfliktu aktu; po 10 rozdziałach postać składa własną wersję zdarzeń. Hub krainy pokazuje obok siebie sprawdzone, sprzeczne zeznania i dwie odblokowywane przez społeczność instalacje, bez większościowego kasowania mniejszości. Finał aktu reaguje w dialogu i scenerii, nie w sile łupu; po 10 aktach powstaje archiwum sporów.
- **Research:** [*Dwarf Fortress* Legends](https://dwarffortresswiki.org/index.php/Legends) zapisuje zdarzenia świata, [FFXIV Ishgardian Restoration](https://na.finalfantasyxiv.com/lodestone/ishgardian_restoration) pokazuje wkład serwera w zmianę miejsca, a [WoW Warfronts](https://worldofwarcraft.blizzard.com/en-us/news/22461364/warfronts-are-rolling-in-will-you-weather-the-storm) miały publiczne fazy wkładu. Nowe byłoby połączenie **sprzecznych, dowodowych relacji** z każdym z 100 lochów; nie sama „zmiana świata”.
- **Wykonanie/koszt/ryzyka:** 200+ krótkich tekstów i wariantów ekspozycji, baza per postać + agregaty; treści autorskie, bez wolnego tekstu graczy. Ryzyko farmy altów i presji frakcyjnej: limit wpływu raz na postać/akt, obie relacje pozostają widoczne, brak bonusów bojowych. Koszt scenariusza i moderacji jest bardzo wysoki.
- **Test 3 pytań:** decyzje — tak, co ujawnić; historie — tak, „nasza frakcja ocaliła dowód”; miesiące — umiarkowanie, jeśli nowe świadectwa faktycznie zmieniają ekspozycję. Słabsze od Atlasu, bo łatwo zmienia się w panel głosowania.

### 3. **Dziedziczone Wyprawy** — asynchroniczne ślady w instancjach
- **Zdanie dla znajomego:** „Na tym serwerze w lochu widzisz użyteczną decyzję poprzedniej drużyny i zostawiasz jedną następnym”.
- **Jak działa/kręgosłup:** W każdym ze 100 lochów drużyna może zostawić jeden z 2–3 autorskich znaków (np. ostrzeżenie o poziomie wody, wskazanie bezpiecznego przejścia, koszt ratunku); jedna następna instancja losuje znak, nigdy obowiązkowy i nigdy w arenie bossa. Wyspa ma analogiczne ślady terenowe. Akty uczą odczytywać coraz bardziej niejednoznaczne wskazówki.
- **Research:** [*Elden Ring*](https://en.bandainamcoent.eu/elden-ring/news/elden-ring-introduction-the-multiplayer-system) ma wiadomości, duchy i plamy krwi innych graczy; [Monumenta](https://monumenta.wiki.gg/wiki/Point_of_Interest) resetuje POI. Nie udajemy, że asynchroniczne ślady są nowe. Nowa byłaby kontrolowana, fabularna sukcesja *przez wszystkie instancje* w MMO na MC.
- **Wykonanie/koszt/ryzyka:** Plugin Paper trzyma 1 enum na instancję + czas życia; trzy gotowe obiekty per loch, filtrowanie bez tekstu. Ryzyka: grief przez fałszywą poradę, spam bazy, niejasne autorstwo; ślady mogą wyłącznie pomagać, rotują i nie zdradzają finału. Test 3 pytań: decyzje — trochę; opowieści — tak; miesiące — słabo bez nowej treści. Za mało jako samodzielna oś.

**Wybór Y:** Atlas Szwów ma największy wpływ na strukturę 1→100 przy najmniejszej presji społecznej. Jego koszt jest realny: jeśli nie powstanie choć kilka sprawdzonych przejść między aktami, zostanie tylko ozdobną mapą i propozycję trzeba odrzucić.

## B. Łuk gry 1→100

1. **001–010, Zielona Granica:** kanoniczny Szczep, Bractwo Ogrodników, Darnik i Głodny Rok; sadzonka przybyła statkiem z Archipelagu. Gracz uczy się, że jedną lokalną regułę można zrozumieć i wykorzystać, a decyzja o plonach ma koszt. Akt 1 nie potwierdza żadnej wiary.
2. **011–020, Archipelag:** śledztwo wskazuje ładunek ze Szczepem, ale nie sprawcę. Wyspy i lochy opowiadają różne części tej samej migracji; pierwsza odnaleziona trasa pokazuje, że morze dawniej łączyło odległe krainy bez zwykłego rejsu.
3. **021–030, pustynia:** próba odczytania dzienników przewozu odkrywa, że karawany przewoziły żywe próbki, aby ocalić regiony przed katastrofą; pierwsi autorzy sieci mieli racjonalny cel. Atlas zaczyna odróżniać *pochodzenie* od *winy*.
4. **031–040, Głębia:** kopalnie ujawniają fizyczne kotwice szwów. Nie zbudowali ich współcześni bogowie ani jedna rasa; legendarne interpretacje Ordhuna i Nysary pozostają sporami, nie wykładem metafizyki.
5. **041–050, Korona:** uszkodzona kotwica może dać schronienie jednej osadzie kosztem izolacji drugiej. Decyzje wysp i lochów pokazują realny koszt ratowania wszystkiego naraz. Rosnąca złożoność: prawo lokalne + wybór trasy + ocena skutków.
6. **051–060, Srebrny Las:** Przymierze Latarni i Księżyca chroni dawne przejścia; tradycje Aelyry/Sylvaena i Solarnej Latarni opisują je inaczej. Las pozostaje dziki, nie jest powtórką upraw Aktu 1; noc i głosy zwierząt są tropami kulturowymi, nie kolejnym prawem świata.
7. **061–070, Ziemie Paktu:** Pakt Krwi i Kości żąda otwarcia sieci dla swoich osad, Przymierze boi się najazdu. Gracz obu frakcji poznaje własne racje; orkowie i nieumarli pozostają ludami grywalnymi, nie rodzinami mobów. Przysięga Nysary i spór Vharuna z Morvheną wyostrzają stawkę polityczną, ale nie zmieniają łask patronów.
8. **071–080, Popielna Korona:** sieć grozi przenoszeniem katastrof między krainami. Zwolennicy zamknięcia potrafią wskazać konkretne ofiary; zwolennicy otwarcia konkretne uratowane miasta. Atlas pokazuje przyczynę, nie daje moralnego wyniku punktowego.
9. **081–090, Podniebie:** ujawnia się plan Głównej Wiązaczki: spiąć wszystkie wyspy jedną stabilną geometrią, by nie było już głodu i odciętych szlaków. Skutkiem ubocznym byłoby zatarcie różnych praw i siedlisk, czyli tożsamości krain. Frakcje i wyznania dzielą się *wewnętrznie*, bez automatycznego „dobrzy/źli”.
10. **091–100, Kres:** gracz przechodzi centrum sieci i decyduje, czy utrzymać lokalne prawa oraz ograniczone szwy, czy otworzyć wielką drogę z bezpiecznikami. Oba zakończenia mają jawny koszt, tę samą moc i dostępność; wiedza z Atlasu daje pełniejszy epilog oraz dodatkową trasę, nigdy nie blokuje finału. Dziesiąte prawo jest własne, bez remiksu dziewięciu poprzednich.

**Tempo:** każdy akt 10 poziomów i jedno nowe prawo fizyczne. Pierwszy loch uczy go w walce, środek używa odrębnych czasowników i haczyków, dziesiąty egzaminuje z prawa i historii. Złożoność rośnie przez mapę przyczyn i konsekwencji, a nie przez dokładanie globalnych liczników/statusów. Poziom i łup nie zależą od kolejności, frakcji, wiary ani zapełnienia Atlasu; Kronika zostaje bonusem wiedzy. Skala HP/EXP po 010 wymaga osobnego poligonu czterech archetypów; nie ekstrapoluję L1–L4 liniowo do 100.

## C. Dziesięć aktów (nazwy 011–100 robocze)

### 1. Zielona Granica · 001–010 — kanon, bez zmian
- Uprawna kraina żniwnego folkloru, Szczep, Bractwo Ogrodników, Darnik i Głodny Rok; wyspa oraz dziesięć kart 001–010 pozostają dokładnie jak w akt1-v2, bez leśnego, morskiego lub religijnego retconu.
- Prawo: **„Co tu pada — kiełkuje”**: poległy zostawia pąk, który można zdeptać dowolnym trafieniem albo dopuścić do dojrzewania; finał 010 to Matecznik, Darnik odcięty, nie zabity, a statek z sadzonką wskazuje Akt 2.

### 2. Archipelag Słonych Mgieł · 011–020
- Kraina: porty na palach, mokre wraki i płycizny; jedyny akt o morzu jako pionowej, zmiennej podłodze — bez Topielców/Utopców i powtórki Topielicy z 002.
- Prawo: **„Woda bierze i oddaje grunt”** — czytelny przypływ co 90 s zalewa zaznaczone pasy, zmieniając osłony, przejścia i zasięgi obu stron walki.
- Antagonistka: kapitan Avara zamyka szlaki, bo widziała, jak obcy ładunek zabił załogę; zagrożenie: blokada transportu, emocja **podejrzenie**.
- Transport: płaskodenne łodzie między wyspami; w lochach mokre rampy, pokłady i odsłaniane suche koryta.
- **011 Suchy Dok:** *mierz* znaki przypływu; bosman blokuje grodzie własną kotwicą, którą można uwięzić w odpływie.
- **016 Magazyn Pieczęci:** *przeważ* skrzynie dla osłony; inspektorka pływa między regałami, ale ciężki ładunek zamyka jej tor.
- **020 Ładownia Bez Portu:** *osądź* dokument przewozu; Avara odcina wodę pod sobą, ujawniając pod kilem przejście ku pustyni.
- Finał: dziennik potwierdza podróż sadzonki, nie winę kapitan; pierwszy szew pokazuje, że statek korzystał ze starszej sieci.

### 3. Złote Piaski · 021–030
- Kraina: karawany, gliniane miasta i wydmy szkliwa; jedyny akt otwartego słońca i cienia, bez mumii, faraonów i grobowców jako głównej rodziny.
- Prawo: **„Odkryte słońce nagrzewa ciało i metal”** — jawny żar narasta wyłącznie w nasłonecznionych polach, cień stałych przesłon go wygasza także u wrogów.
- Antagonista: rachmistrz Sefir chroni wodę i informacje o starych szlakach, bo otwarcie przejść może opróżnić oazy; zagrożenie: prywatyzacja cienia, emocja **pragnienie**.
- Transport: karawany na wyschniętych tarasach; gracz rozpoznaje ruchomy cień z dużych żagli.
- **021 Brama Żagli:** *przesuń* płótna; nadzorca ustawia żagle przeciw graczowi, sam zostając na odsłoniętym placu.
- **026 Studnia Miar:** *odmierz* wodę na chłodzenie; poborca traci ochronę, gdy jego miara napełni się kondensatem.
- **030 Obserwatorium Białej Godziny:** *zasłoń* soczewkę; Sefir zawiesza własny cień nad areną, lecz sam wtedy nagrzewa się na podwyższeniu.
- Finał: księgi dowodzą, że przewozy były akcją ratunkową; Atlas rozdziela trasę, intencję i wynik.

### 4. Głębia · 031–040
- Kraina: geody, szyby i podziemne mosty; jedyny akt, gdzie widzialność jest zasobem walki, nie dekoracją.
- Prawo: **„Kamień odpowiada tylko w świetle”** — oświetlone żyły otwierają bezpieczną drogę lub słaby punkt; mrok zasłania oba, bez wzrostu statystyk wrogów.
- Antagonistka: miernicza Velka gasi lampy, by ukryć kotwice szwów przed rabunkiem; zagrożenie: uwięzienie górników, emocja **klaustrofobia**.
- Transport: wagoniki na szynach i windy przeciwciężarowe; komory wydrążone pionowo, nie dziesięć podobnych grot.
- **031 Szyb Pierwszej Lampy:** *doświetl* żyłę; Kretacz chowa pancerz po zgaszeniu trzech jawnych lamp.
- **035 Kuźnia Geod:** *rozszczep* wiązkę w pryzmacie; odlewnik zaślepia własny automat, gdy przechodzi przez kolorowy snop.
- **040 Kotwa Bez Dna:** *połącz* reflektory; Velka przestawia źródła, lecz jej cień zdradza prawdziwy przewód.
- Finał: istnieją realne kotwice sieci; pochodzenie religijne pozostaje interpretacją, a Velka daje dostęp do planów po zabezpieczeniu górników.

### 5. Mroźna Korona · 041–050
- Kraina: wysokie lodowe półki i schrony w skale; jedyny akt o ciężarze na kruchej powierzchni, bez lawiny 045 i powtórki ciepło/zimno z Aktu 3.
- Prawo: **„Lód pęka pod skumulowanym ciężarem”** — każda jawnie spękana tafla utrzymuje określoną liczbę przejść/uderzeń, po czym odsłania niższą, przeżywalną trasę.
- Antagonista: opiekun schronów Torg zamyka przejście, bo otwarcie go pozbawi jedną osadę osłony; zagrożenie: izolacja, emocja **samotność**.
- Transport: sanie na linach między półkami; pionowe zejścia zawsze mają widoczny punkt lądowania.
- **041 Próg Kry:** *wyważ* kroki; strażnik w ciężkim pancerzu sam łamie most, jeśli gracz oznaczy bezpieczny brzeg.
- **046 Schron Dwóch Dolin:** *rozdziel* zapasy; gospodarz muru przesuwa ciężkie drzwi, przez co pęka jego własna osłona.
- **050 Most Ocalałych:** *przeprowadź* karawanę; Torg wybiera bezpieczniejszą trasę dla swoich ludzi i odsłania własną flankę.
- Finał: dowód pokazuje, że sieć może ocalić albo odciąć; konflikt nie kończy się karą za troskę o swoich.

### 6. Srebrny Las · 051–060
- Kraina: dzika, srebrna puszcza, korony nad przepaścią; jedyny akt o tropie dźwiękowym, bez pól uprawnych, Jelenia 056 i Drzewa Matki 058.
- Prawo: **„Las słyszy każdy hałas”** — uderzenie, bieg i krzyk tworzą widoczną falę dźwięku, która naprowadza faunę; cisza tłumi trop, ale nie wyłącza walki.
- Antagonistka: zwiadowczyni Elen pilnuje ścieżek Przymierza, bo obawia się osiedlania w ostoi; zagrożenie: utrata siedlisk, emocja **zachwyt z lękiem**.
- Transport: zawieszone pomosty i ścieżki na koronach; księżyc wskazuje drogę w świecie, nie przełącza prawa co fazę.
- **051 Nasłuch Koron:** *zwab* wartę od przejścia; boss rysuje falę dźwięku rogiem, zdradzając własną pozycję.
- **055 Nory Milczących:** *stłum* dzwonki przy szlaku; Łowczyni słyszy każdy strzał, lecz można ją skierować w pustą koronę.
- **060 Sala Szeptów:** *odsłuchaj* korę; Elen naśladuje kroki gracza, lecz prawdziwa fala odbija się od jej stanowiska.
- Finał: Przymierze dopuszcza ograniczony szew, kiedy gracz udowodni, że przejście omija gniazda; wiara Aelyry i Sylvaena objaśnia spór, nie zastępuje obserwacji.

### 7. Ziemie Paktu · 061–070
- Kraina: pogranicze fortyfikacji i ziemi niczyjej; jedyny akt o osłonach tworzonych przez zniszczenie, bez orków i nieumarłych jako domyślnej rodziny przeciwników.
- Prawo: **„Uderzenie zostawia osłonę”** — oznaczone działa/masywne ciosy rzeźbią trwały do końca starcia krater lub gruz; obie strony mogą użyć nowej geometrii.
- Antagonista: dowódca Sarn (człowiek) broni blokady z obawy przed wojną przez szwy; zagrożenie: eskalacja pogranicza, emocja **gniew**.
- Transport: pancerne promy rzeczne i zasłonięte trakty; frakcje mają odrębne obozy, te same poziomy i fakty.
- **061 Rów Graniczny:** *wykop* osłonę ciosami działa; boss rozbija mur, za którym sam chowa sygnał natarcia.
- **065 Pole Milczących Armat:** *przetocz* działo; artylerzysta burzy własny punkt obserwacyjny, jeśli gracz wymusi zmianę osi.
- **070 Sala Rozejmu:** *ochroń* posłów za świeżym gruzem; Sarn musi wyjść z osłony, aby zatrzymać oba sztandary.
- Finał: ujawniony dokument wskazuje wspólną manipulację szlakami; Pakt i Przymierze pozostają stronami rokowań, nie łupem do wybicia.

### 8. Popielna Korona · 071–080
- Kraina: kaldery, obsydian i dymne tarasy; jedyny akt o przemianie rozgrzanej skały, bez kapłanów ognia 073 i „ognia w każdym ósmym lochu”.
- Prawo: **„Żar rozmiękcza, chłód utrwala”** — wyraźnie oznaczone płyty można na krótko odkształcić w walce, po ostygnięciu stają się nową przeszkodą lub mostem.
- Antagonistka: hutniczka Mera chce zatopić centralną kotwicę, bo przeszły przez nią niszczycielskie wstrząsy; zagrożenie: odcięcie drogi ratunku, emocja **trwoga**.
- Transport: ciężkie gondole na łańcuchach nad kalderą; bez jazdy po lawie jako obowiązkowej bramy zręcznościowej.
- **071 Próg Żużlu:** *uformuj* miękką płytę; nadzorca odkuwa barierę, która po stygnięciu blokuje mu odwrót.
- **076 Piec Łańcuchów:** *zaklinuj* koło; mechaniczny smokołaz topi własne stopnie, otwierając drogę pod brzuch.
- **080 Studnia Wygaszenia:** *schłódź* kanał; Mera osłania serce pieca, lecz utrwalony most zmusza ją do pojedynku na jednym poziomie.
- Finał: pokazany jest rzeczywisty koszt otwartej sieci; Mera zgadza się na bezpiecznik zamiast ślepego połączenia.

### 9. Podniebie · 081–090
- Kraina: podwieszone miasta i łańcuchy w chmurach; jedyny akt, w którym wektor wiatru zmienia lot i ruch obu stron, bez „odbijaj pocisk” jako powtórki.
- Prawo: **„Wiatr niesie wszystko w jednym kierunku”** — widoczny strumień przesuwa skoki, upadki i pociski, także wrogie; zawsze jest osłonięta droga dla walki wręcz.
- Antagonista: lotnik Ivo chce otworzyć wszystkie szwy, bo jego miasto umiera od odcięcia; zagrożenie: przeciążenie sieci, emocja **nadzieja**.
- Transport: kolejki na łańcuchach i sterowalne żagle, bez wymaganego swobodnego lotu na gryfie.
- **081 Kotwica Nieba:** *ustaw* żagiel; bosman chybia szarżę, gdy sam przeleci przez zmianę strumienia.
- **086 Szkoła Lotników:** *przechwyć* niesioną przesyłkę; trenerka oddaje rozkaz przez wiatr, zdradzając okno bez osłony.
- **090 Węzeł Burzowy:** *przeciąż* śmigła strumieniem; Ivo odwraca wiatr, by ratować załogę, i odsłania rdzeń maszyny.
- Finał: poznajemy plan Głównej Wiązaczki i jego uczciwą obietnicę; katastrofa wynika ze skali, nie z prostego szaleństwa.

### 10. Kres · 091–100
- Kraina: odsłonięte styki świata i niewykończone krawędzie wysp; jedyny akt o sąsiadowaniu przestrzeni, bez kopiowania praw 1–9 i ich bossów.
- Prawo: **„Rysa styka odległe miejsca”** — oznaczone pary szczelin przepuszczają gracza, wroga lub cios na drugą stronę; połączenie jest stałe w danym starciu i czytelne dla wszystkich archetypów.
- Antagonistka: Główna Wiązaczka chce jednej bezpiecznej drogi dla każdego, nawet kosztem lokalnych praw; zagrożenie: ujednolicenie krain, emocja **odpowiedzialność**.
- Transport: krótkie przejścia przez szwy; przestrzeń tworzy sąsiedztwa, nie losowe teleporty.
- **091 Próba Węzła:** *przejdź* szczelinę; strażnik broni frontu, a gracz może wrócić za jego plecy tą samą rysą.
- **096 Archiwum Tras:** *zestaw* dwa obrazy; kustosz wysyła cios w niewłaściwą szczelinę po zmianie pozycji celu.
- **100 Serce Szwu:** *wybierz* konfigurację bezpieczników; Wiązaczka broni przejść, lecz własne skróty otwierają drogę do rdzenia.
- Finał: podstawowe zakończenie bez Atlasu; pełna wiedza pozwala zrozumieć koszt obu rozwiązań i zobaczyć dodatkowy epilog. Brak premii do statystyk lub zablokowanego bossa.

## D. Macierz kontrolna 10 aktów

| Akt | Prawo fizyczne w walce | Dominujący kolor | Pora/pogoda | Żywioł / medium | Rodzina wrogów | Emocja |
|---|---|---|---|---|---|---|
| 1 | śmierć → pąk | złoto zboża | późne lato, przed burzą | ziemia uprawna | plonowe istoty i folklor pól | troska |
| 2 | cykl zalewu | turkus | świt, słona mgła | woda morska | rafowa fauna i konstrukty portu | podejrzenie |
| 3 | jawny żar słońca/cienia | ochra | południe, bezchmurnie | słońce | pustynne drapieżniki i gliniane straże | pragnienie |
| 4 | światło ujawnia żyłę | indygo | bez pory, pył geod | minerał | kryształowe ślepce i górnicze automaty | klaustrofobia |
| 5 | ciężar łamie taflę | lodowy błękit | polarna noc, suchy śnieg | lód | lodowe drapieżniki i straże schronów | samotność |
| 6 | hałas wyznacza trop | srebro | błękitna godzina, rosa | dźwięk | dzika fauna koron | zachwyt z lękiem |
| 7 | uderzenie tworzy osłonę | rdzawa czerwień | zmierzch, kurz | gruz | machiny, sępy i żelazne pasożyty | gniew |
| 8 | gorąca skała mięknie i stygnie | pomarańcz obsydianu | czarny dzień, deszcz popiołu | magma/skała | żużlowe golemy i salamandry | trwoga |
| 9 | wektor wiatru niesie ruch | biel chmur | jasny poranek, silny wiatr | powietrze | podniebne drapieżniki i śmigłowe konstrukty | nadzieja |
| 10 | rysa styka dwa miejsca | ametyst | bezczas, bez opadów | przestrzeń | bezpostaciowe węzły i strażnicy geometrii | odpowiedzialność |

Kolory, pory, żywioły, rodziny i emocje mają po jednym dominującym właścicielu. Lokalni antagoniści ludzkich/elfich/orkowych/nieumarłych kultur mogą wystąpić jako osoby, lecz żadna grywalna rasa nie staje się rodziną mobów. Nazwy, czasowniki i haczyki 011–100 są roboczym rejestrem do kontroli przy projektowaniu kart; przykłady nie przywracają stałych kolumn K2–K9.

## E. Ryzyka i najsłabsze miejsca

1. **Najsłabsze:** 100 wysokiej jakości lochów plus 20–30 szwów to ogromny koszt. [Origin Realms](https://originrealms.com/blog/full-release-new-chapter/) publicznie wycofał plan dużych skryptowanych lochów z powodu tempa produkcji; nie wolno obiecać całego Atlasu przed pionowym testem Aktu 2.
2. Akt 1 nie ma projektowanego Atlasu: retroaktywne interpretacje istniejących śladów mogą wydać się doklejone. Test narracyjny na graczach po 010, bez zmiany kart.
3. Szwy między instancjami mogą zepsuć grupy, kolejki i zapisy powrotu. Pierwszy prototyp wyłącznie poza walką, z powrotem do hubu i logiem każdej zmiany stanu.
4. Prawo Aktu 4 wymaga czytelnego światła bez przewagi monitorów/gamma; test na słabym kliencie i z trybem dostępności.
5. Prawo Aktu 6 może karać Magów/Akolitów częściej niż ciche klasy; fala dźwięku i limity reakcji fauny muszą obejmować cztery archetypy solo.
6. Akt 7 ma ryzyko gryfingu frakcyjnego i skojarzenia Paktu z mobami; wszyscy gracze dostają te same fakty, nagrody i dostęp do rozejmu.
7. Akty 2, 5, 8 zmieniają teren, ale *innymi przyczynami*: cykl wody, skumulowany ciężar, przemiana cieplna. Jeśli ich testy bojowe brzmią tak samo, przepisać prawo 5 lub 8.
8. Finałowy wybór nie może wymusić trwałego rozszczepienia serwera ani zniszczyć starego kontentu; różnice zostają w osobistym epilogu i bezpiecznych wariantach publicznej scenografii.
9. Nazwy i pojedyncze bossowe haczyki to próbka, nie dowód unikalności wszystkich 100; przed kartami 011+ potrzebny globalny rejestr motywów, czasowników, bossów i kadrów WOW, analogiczny do L5–L7.
