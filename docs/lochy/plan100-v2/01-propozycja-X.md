# Propozycja X — 100 lochów jako świat, który dziedziczy skutki

Status: projekt do dyskusji, 2026-10-09. Kanon Aktu 1 i zasady `09-korekta-planu-100.md` są wiążące; nazwy i rozwiązania Aktów 2–10 są propozycją. Docelowo 10 × 10 lochów, poziomy aktu `10(k−1)+1…10k`; koszt może wymusić decyzję właściciela o mniejszej liczbie, bez udawania, że zostały zaprojektowane wszystkie karty.

## A. Mocna rzecz

### 1. **Spadek po kimś** — faworytka

- Zdanie dla znajomego: **„Na tym serwerze decyzja, którą podejmiesz w lochu, staje się prawdziwą częścią wyprawy kogoś obcego — a ty dziedziczysz skutki po kimś przed tobą.”**
- Rdzeń: każdy z lochów 011–100 ma jedno unikalne miejsce „spadku” w walce lub trasie oraz jeden unikalny wybór wyjściowy. Przed wejściem grupa widzi dwa możliwe skutki i po wejściu otrzymuje jeden zapis z poprzedniego aktu: np. ocalałe drewno ze statku daje w 021 ruchomą osłonę, a zachowana mapa daje bezpieczne przejście, lecz więcej odkrytych flank. To nie losowy afiks: skutek ma autora, źródło, własny kadr w mapie i kontrę w walce.
- Gracz może spadek **przyjąć lub przepracować** podczas starcia; na końcu decyduje, co sam przekaże dalej. Wszystkie warianty muszą być przechodnie solo dla 4 archetypów, z równym tierem nagród i bez warunku do awansu.
- Łączenie 100: 001–010 pozostają dokładnie kanonem; system może tylko odczytać istniejący zapis wyboru 001/010 po wdrożeniu kart, z neutralnym stanem domyślnym. W 011–020 pojawia się dziedzictwo Granicy; wybory aktu 2 idą do aktu 3 itd. W 100 wynik zasila epilog i powroty na wyspy. Nie ma par `loch N ↔ strefa N`: wyspa aktu może zarówno wystawić, jak i otrzymać spadek z tej samej puli, przy równym postępie.
- Publiczna Mapa Granicy przechowuje ślad pochodzenia i licznik skutków; na wyspie 1–2 sceny publiczne na akt pokazują, jakie spadki dominują. Nie zmienia to kanonu historii, mocy postaci ani dostępu do bossów. Imię autora jest opcjonalne; można ukryć profil.
- Badane analogie: [Dark Souls](https://en.bandainamcoent.eu/dark-souls/dark-souls) i [Elden Ring](https://en.bandainamcoent.eu/elden-ring/news/elden-ring-introduction-the-multiplayer-system) mają wiadomości/ślady po graczach; [Death Stranding](https://blog.playstation.com/2019/11/07/hit-the-ground-running-with-these-day-one-death-stranding-tips/) ma użyteczną infrastrukturę od innych; [Tides of Tomorrow](https://blog.playstation.com/2026/03/03/tides-of-tomorrow-how-players-will-impact-one-anothers-solo-adventures-out-april-22/) wprost opisuje wybory innych wpływające na samotną fabułę. **Uczciwy wniosek:** asynchroniczne skutki już były. W przejrzanych opisach [Wynncraft](https://wynncraft.wiki.gg/wiki/Dungeon), [Hypixel SkyBlock](https://wiki.hypixel.net/Catacombs_Floor_V), [Monumenta](https://monumenta.wiki.gg/wiki/Dungeons) i [Origin Realms](https://originrealms.com/blog/full-release-new-chapter/) nie znalazłem łańcucha *loch → fizyczny skutek w cudzym lochu → kolejny akt* jako osi całej kampanii MC; to ograniczony przegląd, nie dowód, że nikt tego nigdy nie zrobił.
- Wykonanie: mały moduł Paper zapisuje `accountId, akt, źródło, wariant, data` w jednej transakcji i pobiera zatwierdzony rekord; MD wybiera jedną z **dwóch gotowych wersji jednej sali**, MythicMobs obsługuje jej skutek, Nexo/ModelEngine/HUD tylko komunikują. Żadnego swobodnego stawiania bloków przez graczy ani odtwarzania pełnej symulacji świata. Pilot: odczyt z 010 i trzy lochy 011–013, potem decyzja o skali.
- Koszt: 90 dodatkowych wyborów i 180 wariantów sal do zaprojektowania/testu, plus odpowiedniki wyspiarskie; to może być droższe niż same modele bossów. Jeśli pilot nie daje ciekawych opowieści, zatrzymać rozszerzanie, a nie rozsmarować płytki system na 100 numerów.
- Ryzyka i hamulce: jedna ekspozycja na instancję; pula tylko z pierwszego ukończenia na konto w akcie i z limitu tygodniowego (multikonta pozostają problemem); brak wpływu na drop/EXP; serwer nadaje wynik po finalizacji, nigdy klient; rekordy wygasają w puli po 7 dniach, Kronika pozostaje; pustą pulę zastępuje neutralny skrypt. Żaden wybór nie może zablokować trasy ani narzucić PvP. Testy obciążenia równoległych instancji są bramką.
- Test 3 pytań: **decyzje** — „czy zostawić osłonę czy mapę, a cudzy spadek wykorzystać czy zmienić”; **historie** — „wygrałem dzięki śluzie po graczu, którego nigdy nie spotkałem”; **powrót** — chcę zobaczyć, gdzie dotarł mój ślad i jak zmienia się znana arena. Powroty wymagają nowych sensownych wariantów, a nie tylko licznika.

### 2. **Atlas Praw** — odważna alternatywa, wyższy koszt

- Zdanie dla znajomego: **„Na tym serwerze każda kraina ma prawo fizyki, a gracze w finale aktu wspólnie wybierają jedną z dwóch jego bezpiecznych nastaw na następny miesiąc.”**
- Kręgosłup: 10 praw z sekcji C zostaje stałych; 10. loch każdego aktu pokazuje cenę dwóch nastaw, a wszystkie lochy i wyspa krainy dostają dwie wersje jednej istotnej przeszkody. Rozstrzygnięcie nie blokuje historii, a obie nastawy mają identyczny pułap mocy. Akt 1 jest zamrożony, więc głosowanie mogłoby zacząć się dopiero od Aktu 2.
- Badane analogie: [PoE Atlas](https://www.pathofexile.com/forum/view-thread/3008270) pozwala konfigurować mapy, [Hades](https://www.supergiantgames.com/blog/hades-welcome-to-hell-update-patch-notes/) zmienia wyzwania przez Pact of Punishment, [Noita](https://www.noitagame.com/press/index.html) sprzedaje fizykę materiałów jako rdzeń, [FFXIV Ishgardian Restoration](https://na.finalfantasyxiv.com/lodestone/ishgardian_restoration) pokazuje wspólną przemianę miejsca, a [EVE](https://community.eveonline.com/news/news-channels/press-releases/eve-online-appoints-in-world-economist-1/) trwały świat napędzany graczami. Tu nowy byłby wspólny, jawny wybór *wariantu prawa* dla PvE MC; nie twierdzę, że każdy składnik jest nowy.
- Wykonanie: dwie przetestowane konfiguracje każdej sali i wyspy, przełączenie wyłącznie przy restarcie instancji; nie dynamiczna edycja fizyki. Koszt bliski podwojeniu QA 90 lochów, ryzyko dominacji licznej gildii i optymalizacji „jedynej słusznej” nastawy. Ochrona: limit głosu na konto, jawny audyt, remis = stan neutralny, żadnych statystycznych premii.
- Test 3 pytań: **decyzje** — który koszt świata wziąć na siebie; **historie** — kraina przez miesiąc wygląda i gra inaczej dzięki społeczności; **powrót** — drugi wariant zmienia taktykę. Słabość: polityka głosowania może stać się ważniejsza od przygody. Dlatego wybieram **Spadek po kimś**: oddaje sprawczość każdej małej drużynie i pozwala zacząć od trzech lochów.

## B. Łuk gry 1→100

1. **001–010, Zielona Granica:** kanoniczna walka ze Szczepem, Bractwem Ogrodników, Darnikiem i Głodnym Rokiem; statek z Archipelagu jest jedyną zapowiedzią dalej. Darnik zostaje odcięty, nie zabity. Akt nie rozstrzyga, czy Pierwszy Korzeń naprawdę działa.
2. **011–020:** statek okazuje się częścią szlaku transportującego żywe „kotwice” między krainami. Archipelag nie jest źródłem całego zła: jego załogi próbowały uratować plony, a kwarantanna odcięła również żywność. Tu po raz pierwszy materializuje się cudzy spadek.
3. **021–040:** pustynne szlaki i podziemne archiwa pokazują, że lokalne prawa są podtrzymywane przez dawną sieć miar; bilans nosi podpis Mierniczej. Każda kraina płaci za stabilność sąsiadki własnym niedoborem. To odkrycie jest kompletne w lochu i osobno na wyspie; Kronika scala wiedzę, nie moc.
4. **041–060:** mróz chroni od katastrofy, a dziki las opiera się ludziom, którzy chcą go mierzyć jak pole. Gracz widzi, że „naprawienie” jednego miejsca może zranić drugie. Przymierze Latarni i Księżyca wchodzi przez logistykę Solenny i spór o ścieżki Aelyry/Sylvaena, nie jako obowiązkowy wróg.
5. **061–070:** Pakt Krwi i Kości oraz Przymierze spierają się o zarząd nad siecią. Pakt ma rację, że cudze granice odcinają jego osady; Przymierze ma rację, że swobodny przewóz Szczepu zagraża innym. Grywalni orkowie i nieumarli nie są rodziną wrogów; antagonistą jest doktryna trwałego podziału, reprezentowana przez pojedynczego marszałka i jego maszyny.
6. **071–090:** wulkan dostarcza energię stabilizacji, a Podniebie przekazuje rozkazy do wszystkich kotwic. Wojna o sterowanie zmienia się w pytanie, czy można utrzymać każde lokalne prawo bez ukrywania kosztu przed sąsiadami.
7. **091–100:** Kres jest warsztatem połączeń, nie składanką dziewięciu dawnych biomów. Miernicza chce na zawsze zespolić krainy w jedną nieruchomą równowagę, bo boi się następnego Głodnego Roku. Gracz rozrywa przymusowy układ i ustanawia jawne, odwracalne połączenia; finał nie kasuje wcześniejszych światów.
8. **Wiary:** Ordhun/Nysara/Vharun oraz bóstwa ras są interpretacjami i politycznymi językami postaci, nie potwierdzonym „sekretnym kodem” maszyny. Wybrany patron, łaska i rasa nie zmieniają dostępu, nagród ani praw fizyki. W Akcie 7 przysięga Gorratha i Morvheny przed Nysarą ma ciężar fabularny; sprzeciw Vharuna wobec nieumarłych daje konflikt idei, nie karę dla grywalnej rasy.
9. **Wzrost gry:** 1–10 decyzja o pąku; 11–30 czytanie jednego spadku i lokalnego prawa; 31–60 decyzje o pozycjonowaniu i zasobie areny; 61–90 konflikt kilku interesów przy niezmiennym pojedynczym prawie aktu; 91–100 rozumienie połączeń. Nie dokładamy dziewięciu praw naraz do finału.
10. **Dwie drogi:** lochy i wyspa mają równy poziom/łup rodzinny oraz osobne fabuły i bossów; każda może wystawić i otrzymać spadek. Finał aktu ma dwa różne doświadczenia, bez obowiązku ukończenia obu i bez par numerowanych stref.

## C. 10 aktów — kraina, prawo, konflikt i przykłady

### Akt 1, 001–010 — Zielona Granica (KANON, bez zmian)
- Ziemia uprawna i folklor żniwny; prawo „Co tu pada — kiełkuje”: każdy poległy zostawia identyczny pąk, trafienie go usuwa, pozostawiony dojrzewa po stałym czasie w lokalny skutek.
- Szczep, Bractwo Ogrodników, Darnik i Głodny Rok prowadzą do 010 Matecznika; Darnik zostaje odcięty, a sadzonka przypłynęła statkiem z Archipelagu. Lochy 001–010, ich bossowie, liczby L1–L7 i rozstrzygnięcia R1–R5 pozostają kanoniczne.

### Akt 2, 011–020 — Archipelag Słonych Mgieł
- Kraina: pionowe porty, rafy i statki kwarantanny; pełne morze i żegluga należą tylko tutaj.
- **Prawo:** Co 40 s przypływ podnosi wodę o jeden poziom areny, zalewając niskie osłony i odsłaniając nowe dojścia przy odpływie; zapowiedź 5 s, sucha trasa zawsze zostaje.
- Zagrożenie: blokada żywności po wykryciu Szczepu; admirałka Salia wierzy, że poświęcenie odciętych osad ocali wszystkie pozostałe.
- Emocja: nieufność. Wyspa: ruchome pomosty i łodzie między nabrzeżami.
- **011 Nabrzeże Bojowe:** zakotwicz boje; boss Krab Cumownik odcina szczypcem najniższy pomost tuż przed przypływem.
- **015 Dźwigi Ujścia:** przeciągnij ładunek; boss Celnik Pływów używa zawieszonej skrzyni jako własnej osłony.
- **020 Sercownia Śluz:** przestaw kolejność śluz; Salia oddaje suchą drogę cywilom, a sama walczy na zalanym pokładzie.
- Finał: kwarantanna staje się korytarzem kontrolowanym, a manifest statku wskazuje pustynne odbiorniki kotwic; spadek dla Aktu 3 to uratowany ładunek lub czytelny manifest.

### Akt 3, 021–030 — Złote Piaski
- Kraina: karawanseraje, studnie i ruchome wydmy; żadnych mumii, piramid ani pustynnych nieumarłych.
- **Prawo:** Każde głośne uderzenie ściąga suchy piasek w oznaczony punkt, usypując po 3 s niski wał i obniżając sąsiednią krawędź areny; działa na cios gracza i wroga.
- Zagrożenie: zakopane szlaki odcinają wodę; kartografka Nahir usuwa drogi z map, bo wierzy, że w ten sposób odgrodzi zakażone transporty.
- Emocja: pragnienie. Wyspa: karawany na grzbietach wydm, bez automatycznych teleportów przez burzę.
- **021 Brama Echa:** wywołaj uderzenie w pustym kole; boss Bębnica zasypuje miejsce ostatniego hałasu, a odsłania własny grzbiet.
- **025 Zasypany Karawanseraj:** odkop studnię przez przeniesienie piasku; boss Skorpion Miar atakuje spod nowego wału, który gracz sam usypał.
- **030 Atlas Studni:** oznacz prawdziwy szlak; Nahir przestawia mapę areny przy każdym zakopanym znaku.
- Finał: żywe osady wracają na mapę; pod zasypanym szlakiem jest zejście do archiwum, które dokumentowało koszty stabilizacji.

### Akt 4, 031–040 — Głębia
- Kraina: wydrążone hale, windy i geody; czarny kryształ i brak naturalnego nieba należą tylko tu.
- **Prawo:** Na nieoświetlonej powierzchni po 6 s narasta krucha czarna sól; oświetlenie zatrzymuje narost, dowolny cios kruszy go w widoczne odłamki i otwiera przejście.
- Zagrożenie: archiwum jest zamurowane przez sól; mistrz lamp Borun rozdziela światło tylko górnikom, bo wierzy, że ujawnienie bilansu sieci wywoła panikę i zamknie kopalnię.
- Emocja: klaustrofobia. Wyspa: spiralna jaskinia i wagoniki z punktami przesiadki.
- **031 Szyb Czarnej Soli:** rozświetl krawędź; boss Sólmistrz nosi pancerz, który pęka dopiero po wejściu w snop lampy.
- **035 Kondukt Ślepych:** eskortuj wagon z jedną lampą; boss Głuchy Kretek kruszy osłony w miejscach, które grupa zostawiła ciemne.
- **040 Izba Ostatniej Lampy:** rozdziel światło między archiwalne półki; Borun sam gasi lampy, lecz odsłania wtedy rejestr na swoim pancerzu.
- Finał: znaleziony bilans dowodzi przerzucania kosztów między krainami; najstarsze płatności skierowano do Mroźnej Korony.

### Akt 5, 041–050 — Mroźna Korona
- Kraina: lodowe domy, port lodowca i ślady ewakuacji; tylko tu ciepło jest przenoszonym zasobem.
- **Prawo:** Dotknięcie ogrzanego obiektu przenosi jego ciepło na gracza lub odwrotnie; obiekt zmienia stan na zimny/ciepły, co natychmiast zmienia osłony i ataki, bez biernego licznika wychłodzenia.
- Zagrożenie: ciepło z osad płynie do zamkniętej twierdzy; regentka Ynna uważa, że jeden niezamarznięty szpital jest wart opuszczonych wiosek.
- Emocja: troska. Wyspa: sanie ciągnięte po wytyczonych trasach i schroniska jako punkty przesiadki.
- **041 Przystań Kry:** ogrzej zasuwę; boss Szronny Łowca ściga aktualnie najcieplejszy obiekt zamiast zawsze gracza.
- **045 Zegar Topnienia:** odłącz termiczne ciężary; boss Lodownik przewraca ogrzane filary, tworząc przejście i zagrożenie jednocześnie — bez lawiny.
- **050 Pałac Rozejmu:** przekaż ciepło pustym łóżkom; Ynna włącza szpital do areny i musi wybrać między osłoną a jego ogrzaniem.
- Finał: sieć schronisk dostaje jawny bilans ciepła; szlak uciekinierów prowadzi do dzikiej puszczy, której nie wolno traktować jak pola uprawnego.

### Akt 6, 051–060 — Srebrny Las
- Kraina: dzika, pionowa puszcza z koronami nad przepaściami; brak sadzonek, pól, plonów i Drzewa Matki.
- **Prawo:** Co 30 s srebrne korony obracają gotowe kładki o ćwierć obrotu wraz z fazą miejscowego księżyca, zmieniając dojścia i osłony; 5 s wcześniej słychać skrzypienie pni.
- Zagrożenie: szlak uchodźców może zniszczyć mateczniki drapieżników; strażniczka Ilyra zamyka las, bo wierzy, że każda wytyczona droga stanie się traktem wyrębu.
- Emocja: zachwyt. Wyspa: mosty w koronach i nocne platformy, z naziemną drogą alternatywną.
- **051 Próg Koron:** wyczekaj obrót kładki; boss Srebrna Sowa atakuje tylko z aktualnej strony połączenia.
- **056 Ścieżka Białego Jelenia:** podążaj za zmienioną koroną; boss Biały Jeleń rozdziela arenę porożem, lecz obrót otwiera obejście.
- **060 Zgromadzenie Pni:** wytycz odwracalną drogę; Ilyra przenosi walkę na korony, które gracz wcześniej kazał obrócić.
- Finał: leśna rada zgadza się na czasowe, odwracalne przejście; Przymierze chce je utrwalić, Pakt chce zagwarantować sobie dostęp.

### Akt 7, 061–070 — Ziemie Paktu
- Kraina: neutralny pas wałów, sztandarów i machin; tylko tu widać bezpośredni spór obu grywalnych frakcji.
- **Prawo:** Ustawiony sztandar przesuwa po szynach pobliskie osłony w kierunku marszu swojej formacji; zbicie lub przejęcie sztandaru zmienia geometrię pola po 3 s.
- Zagrożenie: automatyczna linia obrony nie przepuszcza cywilów; marszałek Taren wierzy, że trwała granica zapobiegnie wojnie o kotwice, nawet jeśli rozdzieli rodziny.
- Emocja: wstyd. Wyspa: okopy i kolejka polowa; wrogami aktu są machiny, nie orkowie ani nieumarli.
- **061 Wał Bez Barw:** przejmij neutralny sztandar; boss Chorąży-Maszyna ustawia osłony przed własnym szarżującym korpusem.
- **066 Składnicy Rozejmu:** rozepnij ruchome barykady; boss Poborca Żelaza kradnie tylko niepilnowane sztandary, nie ekwipunek.
- **070 Linia Tarena:** złóż sztandary na wspólnym torze; Taren musi wybrać między zamknięciem granicy a osłoną ewakuacji.
- Finał: Pakt i Przymierze podpisują odwracalny dostęp do kotwic; odkrywają, że ich zasilanie pochodzi z Popielnej Korony.

### Akt 8, 071–080 — Popielna Korona
- Kraina: czynne kominy, kanały lawy i tufowe półki; kult ognia oraz kapłani ognia nie wracają z dawnego 073.
- **Prawo:** Po oznaczonym wyrzucie lawa zastyga w twardą półkę na 20 s, po czym kruszeje; walka polega na wykorzystaniu nowej bryły, nie na stale rosnącym ogniu.
- Zagrożenie: energia do kotwic pochodzi z kontrolowanych erupcji; inżynierka Kaera kieruje je ku ubogiemu wybrzeżu, bo uważa, że inaczej pęknie cała kaldera.
- Emocja: wyrzuty sumienia. Wyspa: kolej linowa nad kraterem i mosty tufowe o czytelnym cyklu.
- **071 Ścieżka Tufowa:** wejdź na świeżą półkę; boss Popielnik przecina drogę strumieniem lawy, która za chwilę stanie się osłoną.
- **075 Kocioł Żużlu:** zatamuj kanał; boss Salamandra Pieca chowa ogon w zastygłym tufie, odsłaniając głowę.
- **080 Komora Upustu:** otwórz bezpieczny komin; Kaera poświęca własną platformę, by utrzymać ciśnienie w głównej rurze.
- Finał: erupcje przechodzą na cykl niższego ciśnienia; sygnał sterujący prowadzi do Podniebia.

### Akt 9, 081–090 — Podniebie
- Kraina: kotwice wysp, żagle i otwarte niebo; pełny lot i silny wiatr należą tylko tutaj.
- **Prawo:** Co 25 s zapowiedziany wektor wiatru pcha graczy, wrogów i lekkie osłony, lecz za zakotwiczoną bryłą powstaje nieruchoma strefa zawietrzna; każda arena ma drogę bez skoku w przepaść.
- Zagrożenie: rozkazy kotwic docierają tylko z najwyższej stacji; nawigatorka Iven zamyka szlaki naziemne, wierząc, że tylko miasta na wysokości przeżyją awarię sieci.
- Emocja: zawrót. Wyspa: gryfy przewozowe i windy żaglowe, lot bojowy wyłącznie w przygotowanych przestrzeniach.
- **081 Kotwice Podniebia:** ustaw bryłę nawietrzną; boss Gryf Prądowy traci pułap, gdy wejdzie w jej cień wiatrowy.
- **085 Młyny Gradowe:** obróć żagiel; boss Śmigło Strażnicze używa tego samego podmuchu do rozpędzenia własnego ostrza.
- **090 Stacja Wiatromierzy:** rozkotwicz ster; Iven odwraca sygnał wiatru tylko na własnej platformie, zdradzając ster Kresu.
- Finał: sygnały stają się publiczne; gracz otrzymuje mapę połączeń, nie magiczny klucz wymagany do kolejnych poziomów.

### Akt 10, 091–100 — Kres, Warsztat Połączeń
- Kraina: nieruchome, surowe hale sprzęgieł i pustych przestrzeni; nie odtwarzamy dziewięciu biomów jako „echo” starego planu.
- **Prawo:** Dwa jawnie połączone obiekty przesuwają się o tę samą odległość w przeciwnych kierunkach; gracz może sprzęgnąć lub rozprzęgnąć wyznaczone pary przed ciosem bossa.
- Zagrożenie: Miernicza chce trwale zablokować wszystkie sprzęgła, bo bez zmian nie będzie następnego Głodnego Roku — ale zniknie możliwość ratowania sąsiadów własnym wyborem.
- Emocja: odpowiedzialność. Wyspa: pierścienie warsztatu przesuwane po torach, bez portali „wspomnienia” do innych aktów.
- **091 Węzeł Zero:** sparuj płyty; boss Figura A naciera, a jej bliźniak otwiera drogę po przeciwnej stronie.
- **096 Galeria Długów:** rozdziel ciężary; boss Rachmistrz przepycha osłonę tylko przez widoczną parę, którą da się rozpiąć.
- **100 Sala Ważenia:** wybierz połączenia na żywo; Miernicza walczy ruchem całej areny, a przeciwruch odsłania jej rdzeń.
- Finał: jawna sieć pozwala krainom odmawiać i ponawiać połączenia; decyzje graczy pozostają w Kronice, a epilog wyspy pokazuje ich skutki bez przewagi mechanicznej.

## D. Macierz kontrolna

| Akt | Prawo fizyczne w walce | Dominujący kolor | Pora / pogoda | Żywioł | Rodzina wrogów | Emocja |
|---|---|---|---|---|---|---|
| 1 | Pąk po poległym dojrzewa | złotozielony | żniwne popołudnie / przelotny deszcz | żywa gleba | folklor żniwny i polne stwory | niepokój |
| 2 | Przypływ zmienia poziom wody | turkus | mglisty świt / szkwał | słona woda | skorupiaki i portowe bestie | nieufność |
| 3 | Hałas przemieszcza piasek | ochra | skwarne południe / suche powietrze | piasek | pustynne kopacze i skorpiony | pragnienie |
| 4 | Ciemność osadza kruchą sól | indygo | bezsłoneczna noc / pył geod | czarny kryształ | solne istoty Głębi | klaustrofobia |
| 5 | Dotyk przenosi ciepło | biel | polarna noc / szron | mróz | szronniki i lodowe łowce | troska |
| 6 | Korony obracają kładki | srebro | księżycowa pełnia / cisza | dzikie drewno | srebrne drapieżniki puszczy | zachwyt |
| 7 | Sztandar przesuwa osłony | karmazyn | krwawy poranek / pył bitewny | żelazo | machiny graniczne | wstyd |
| 8 | Lawa zastyga w czasowe półki | pomarańcz popiołu | zmierzch / opad popiołu | magma | popielniki i salamandry | wyrzuty sumienia |
| 9 | Wiatr pcha, kotwica daje zawietrzną | kobalt | jasny ranek / podmuchy | wiatr | gryfy i śmigłowe straże | zawrót |
| 10 | Sprzężone obiekty poruszają się przeciwnie | fiolet | nieruchomy zmierzch / bezwietrze | przestrzeń | figury sprzęgieł | odpowiedzialność |

Kontrola rejestru: przykładowe czasowniki główne i haczyki bossów w C nie powtarzają się; przy kartach 011–100 potrzebny jest osobny rejestr globalny mechanik, haczyków, skutków prawa i kadrów WOW. Każdy pierwszy loch uczy tylko swojego prawa, każdy dziesiąty egzaminuje z prawa i historii; nie wracają K1–K10 jako stałe role środka aktu.

## E. Ryzyka i najsłabsze miejsca

1. **Największa słabość: koszt treści.** 90 pełnych lochów po jakości Aktu 1 i 180 wersji sal spadku może przekroczyć możliwości zespołu; pilot 011–013 musi zweryfikować czas produkcji i realną zabawę przed obietnicą setki.
2. Spadek może być tylko przebranym afiksem. Każda karta musi pokazać zmianę decyzji w walce, fizyczny ślad źródła i późniejsze następstwo; gdy tego nie ma, wyciąć wariant.
3. Akt 1 nie zapisuje dziś wdrożonych wyników, bo żadna karta nie działa na serwerze. Integracja dopiero przy budowie i neutralny stan domyślny; nie przerabiać kanonicznych 001–010 pod system.
4. Pula zapisów przy niskiej liczbie graczy będzie się powtarzać. Potrzebne przygotowane spadki NPC, jawnie oznaczone jako fikcyjne, bez udawania żywej społeczności.
5. Multikonta i farmienie wariantów mogą wypaczyć publiczną mapę. Deduplikacja kont, limity, statystyki anomalii i brak premii mocy ograniczają bodziec, ale nie eliminują go.
6. Wspólne głosowanie z Atlasu Praw grozi dominacją gildii; dlatego alternatywa pozostaje eksperymentem, a nie zależnością kampanii.
7. Prawa 3, 6, 7 i 10 wymagają ruchomych bloków/osłon. W Paper/MD należy użyć skończonych stanów i bezpiecznych obszarów kolizji; nie obiecywać pełnej symulacji Noity.
8. Sól Aktu 4 i korony Aktu 6 mogą wizualnie przypominać „grę światłem”. Rozdział testowy: 4 to oświetlanie i kruszenie narostu, 6 to czasowy obrót gotowej geometrii niezależny od pochodni gracza.
9. Religie mogą przypadkiem wyglądać jak potwierdzona technologia bogów. Dialogi przedstawiają poglądy wiernych i historyków, nie rozwiązują teologii; żaden patron nie daje uprzywilejowanej ścieżki.
10. Równość wyspy i lochów wymaga osobnych historii i bossów, nie tych samych finałów pod inną nazwą; najdroższy test zakresu po pilocie dotyczy właśnie otwartego świata.
