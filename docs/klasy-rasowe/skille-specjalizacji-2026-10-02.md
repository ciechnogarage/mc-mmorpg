# Skille specjalizacji — projekt 32 umiejętności (2026-10-02)

## Założenia wspólne

Każda specjalizacja dostaje własny piąty skill na poziomie postaci 10, przypięty domyślnie do slotu 5. Liczby poniżej dotyczą postaci na poziomie 10 i rangi skilla 1; wymagają balansu na poligonie. Cztery skille klasy zostają: wojownik wydaje Szał, ninja Skupienie i buduje Znamiona, mag wydaje manę, akolita Dusze. Dusze pochodzą **wyłącznie z obecnej reguły śmierci**; nowy skill, trafienie lub sługa nie wytwarza Dusz. Wszystkie koszty ustawia parametr MythicLib `mana`.

Obrażenia są podane przed redukcją, zdrowie jako HP, odległości w blokach. Każdy efekt ofensywny rozdziela cele na `isPlayer false` oraz `isPlayer true` + `mmoCanTarget{interaction=OFFENSE_SKILL}`; leczenie graczy sprawdza `SUPPORT_SKILL`. Warunek inline `?…` bada rzucającego, warunek celu należy umieścić w `TargetConditions`. Efekt po trafieniu pocisku czyta stan rzucającego (`caster.*`), ponieważ `skill.*` nie dociera do `onHit`; Oko Burzy przechowuje premię broni osobno na graczu do chwili trafienia. Doskok, jeśli występuje, używa wyłącznie `teleport{spreadh=0;spreadv=1;unsafe=false} @TargetedTarget` i `@Forward{f=..}`. Punktowa strefa zapamiętuje `@Location` z pozycji celu w chwili rzutu; jej opóźnienia i tyki wykonuje gracz, aby zachować kredyt EXP i łupu. Znacznik Oka Burzy wskazuje jedynie pozycję i właściciela dla synergii. Wszyscy słudzy jednego gracza, także bestia i ognik, dzielą limit trzech.

Każda linia premii za broń poniżej jest pojedynczą linią lore; wdrożenie sprawdza typ MMOItems przez `?bronRasowa{typ=..}` i daje premię tylko raz na rzut. Podane leczenie przy kradzieży życia i wampiryzmie oznacza **wynik łączny**: jeśli statystyka silnika już leczy od tego trafienia, skrypt nie może dodać drugiego pełnego leczenia. Statystyka-podpis odpowiada generatorowi. Wyjątek: `opiekun_duchow` ma w tabeli `regen_many`, ale generator zamienia ją na **większą pulę Dusz**. Wszystkie nazwy specjalizacji można zachować, pod warunkiem wdrożenia opisanych mechanik. „Kościany Grot” pozostaje nazwą specjalizacji, nie skilla.

## Projekty umiejętności

### Człowiek — Miecz Korony (wojownik)

**Miecz Korony → Nieugięty | Mur Przysięgi.** Statystyka: obrona. Klimat: `&8&oKorona stoi tam, gdzie jej miecz.` Działanie: 25 Szału, odnowienie 16 s; postawa trwa 5 s, daje odporność na odrzut i Resistance II (40% mniej otrzymywanych obrażeń), a do pięciu wrogów w promieniu 6 prowokuje do ataku na rzucającego. Nie daje osłony HP. `&6◆ Tarcza: postawa trwa 6 s.`

**Miecz Korony → Czempion Areny | Wyzwanie Areny.** Statystyka: obrażenia fizyczne. Klimat: `&8&oNa arenie liczy się jedno ostrze.` Działanie: 35 Szału, odnowienie 12 s, cel do 5; cios zadaje 8 obrażeń fizycznych i po trafieniu daje rzucającemu 4 HP osłony na 4 s. Pchnięcie Korony wprowadza w zasięg, a Rozpłatanie utrzymuje nacisk na jedną ofiarę. Obietnica: ciężki cios rozstrzyga pojedynek. Różnica: skupienie na jednej ofierze zamiast ochrony grupy. Ryzyko: **średnie**, własność znacznika. `&6◆ Broń drzewcowa: zasięg Wyzwania 6 bl.`

### Człowiek — Lis Zaułków (ninja)

**Lis Zaułków → Truciciel | Jad w Ranie.** Statystyka: szansa krytyczna. Klimat: `&8&oW rękawie czeka ostatnia kropla.` Działanie: 30 Skupienia, odnowienie 10 s, cel do 3; cięcie zadaje 4 obrażenia fizyczne, dokłada Znamię i jad po 1 obrażeniu co sekundę przez 4 s, bez kumulacji. Cięcie bazowe i jad przygotowują Egzekucję. Obietnica: ostrze rzeczywiście zatruwa. Różnica: presja w zwarciu zamiast bełtów z dystansu. Ryzyko: **średnie**, jedna trucizna na cel i rzucającego. `&6◆ Sztylet: jad trwa 5 s.`

**Lis Zaułków → Łowca Nagród | List Gończy.** Statystyka: obrażenia pocisków. Klimat: `&8&oCena zapisana jest na bełcie.` Działanie: 35 Skupienia, odnowienie 12 s, cel do 18; bełt zadaje 6 obrażeń pociskowych i Znamię, a cel mający Znamię już przed trafieniem otrzymuje dodatkowe 3 obrażenia raz. Znamiona otwierają dystansową Egzekucję. Obietnica: oznacza ściganego i dobija salwą. Różnica: dystansowy łowca jednej ofiary zamiast jadu w zwarciu. Ryzyko: **średnie**, statystyka pocisków. `&6◆ Kusza: znacznik trwa 7 s.`

### Człowiek — Magister Wieży (mag)

**Magister Wieży → Władca Żywiołów | Krąg Żywiołów.** Statystyka: obrażenia czarów. Klimat: `&8&oTrzy żywioły czekają na rozkaz.` Działanie: 16 many, odnowienie 12 s, punkt do 16, promień 3; po 0,8 s każdy z maksymalnie trzech wrogów otrzymuje 6 obrażeń magicznych, a cel z własną Pieczęcią Wieży dodatkowe 2 raz. Pieczęć wyznacza cel, Wybuch kończy rotację obszarową. Obietnica: panuje nad żywiołami. Różnica: natychmiastowa ofensywa zamiast manipulacji czasem. Ryzyko: **średnie**, własność Pieczęci. `&6◆ Grimuar: promień kręgu 4 bl.`

**Magister Wieży → Chronomanta | Echo Chwili.** Statystyka: krótsze odnowienie. Klimat: `&8&oWieża pamięta minioną sekundę.` Działanie: 12 many, odnowienie 18 s, cel do 16; spowalnia na 2 s i po 3 s powtarza na tym samym celu 4 obrażenia magiczne, jeśli nadal jest w zasięgu. Pieczęć może oznaczyć ofiarę przed echem, a podpis skraca dalszą rotację. Obietnica: widoczny opóźniony powrót zaklęcia. Różnica: kontrola i planowanie zamiast kręgu obrażeń. Ryzyko: **średnie**, znikający cel i właściciel znacznika. `&6◆ Grimuar: spowolnienie trwa 3 s.`

### Człowiek — Nosiciel Relikwii (akolita)

**Nosiciel Relikwii → Żywa Relikwia | Zasłona Relikwii.** Statystyka: obrona. Klimat: `&8&oŚwięte żelazo bierze cios.` Działanie: 1 Dusza, odnowienie 15 s, promień 4; rzucający i jeden sojusznik dostają po 7 HP osłony na 6 s, a istniejący własny obrońca Straży Relikwii 5 HP osłony. Straż daje obrońcę, Żniwo nadal może go poświęcić. Obietnica: chroni pierwszy szereg. Różnica: osobista obrona zamiast dowodzenia duchami. Ryzyko: **średnie**, wskazanie własnego obrońcy. `&6◆ Tarcza: własna osłona +3 HP.`

**Nosiciel Relikwii → Chorąży Świętych | Chorągiew Warty.** Statystyka: większa pula Dusz. Klimat: `&8&oPod sztandarem zbierają się głosy.` Działanie: 2 Dusze, odnowienie 20 s, promień 7; przez 8 s maksymalnie trzej własni słudzy zyskują po 4 HP osłony i szybkość I, a dwóch sojuszników po 3 HP osłony. Straż Relikwii dostarcza obrońcę, Rozkaz kieruje oddziałem. Obietnica: duchy obrońców słuchają chorągwi. Różnica: wsparcie oddziału zamiast własnej gardy. Ryzyko: **średnie**, wybór dzieci właściciela. `&6◆ Tarcza: osłona sług +2 HP.`

### Elf — Klinga Gaju (wojownik)

**Klinga Gaju → Różany Rycerz | Korona Cierni.** Statystyka: obrona. Klimat: `&8&oRóża broni miejsca, gdzie rośnie.` Działanie: 30 Szału, odnowienie 16 s; kolce trwają 6 s, a rzucający otrzymuje Resistance I. Wdrożenie używa impulsów co 1 s w promieniu 3 wokół rzucającego: każdy wróg w zasięgu otrzymuje 3 obrażenia fizyczne i spowolnienie na 1 s. To celowe uproszczenie aury reaktywnej, które działa też bez trafienia wręcz. `&6◆ Tarcza: impuls zadaje 4 obrażenia.`

**Klinga Gaju → Tancerz Ostrzy | Piruet Klingi.** Statystyka: uniki. Klimat: `&8&oKlinga mija cień liścia.` Działanie: 25 Szału, odnowienie 10 s, cel do 4; cięcie zadaje 6 obrażeń fizycznych, daje szybkość I na 3 s i 15 Szału tylko po trafieniu. Przelot Gaju ustawia za ofiarą i może wzmocnić następny cios. Obietnica: szybki, nieuchwytny szermierz. Różnica: ruchomy pojedynek zamiast obrony punktu. Ryzyko: **średnie**, sprawdzenie `onAttack` Przelotu. `&6◆ Katana: szybkość trwa 4 s.`

### Elf — Szept Kniei (ninja)

**Szept Kniei → Tkacz Sideł | Sidła Korzeni.** Statystyka: szansa krytyczna. Klimat: `&8&oŚcieżka pamięta krok intruza.` Działanie: 35 Skupienia, odnowienie 14 s, punkt do 12; jedno widoczne sidło o promieniu 2 trwa 8 s, a pierwszy wróg, który wejdzie w obszar, otrzymuje 3 obrażenia fizyczne, spowolnienie na 3 s i Znamię. Strzała z Korony oraz Egzekucja wykorzystują unieruchomioną ofiarę. Obietnica: stawia prawdziwą pułapkę. Różnica: przygotowanie terenu zamiast snajperki. Ryzyko: **wysokie**, wykrycie wejścia i jednorazowość. `&6◆ Łuk długi: sidło trwa 10 s.`

**Szept Kniei → Srebrna Cięciwa | Deszcz Srebra.** Statystyka: obrażenia pocisków. Klimat: `&8&oKsiężyc prowadzi grot przez liście.` Działanie: 40 Skupienia, odnowienie 11 s; punkt do 22 bloków, promień 3. Po zapowiedzi 0,6 s następują trzy fale w odstępach 0,5 s. Każda fala zadaje 3 obrażenia typu PROJECTILE i nakłada jedno Znamię na cel. `&6◆ Łuk długi: promień 4 bl.`

### Elf — Tkacz Arkanów (mag)

**Tkacz Arkanów → Pieśniarz Burz | Refren Pioruna.** Statystyka: obrażenia czarów. Klimat: `&8&oGaj podchwytuje pieśń burzy.` Działanie: 14 many, odnowienie 11 s, linia do 18; piorun trafia do trzech wrogów po 6 obrażeń magicznych i spowalnia ich na 2 s. Nić uderza w ustawioną w linii grupę przed refrenem, Wybuch domyka obszar. Obietnica: śpiew przywołuje piorun. Różnica: liniowa ofensywa zamiast obrony przed magią. Ryzyko: **średnie**, znacznik własnej Nici. `&6◆ Kostur dwuręczny: linia szersza o 0,5 bl.`

**Tkacz Arkanów → Władca Zwierciadeł | Zwierciadło Gaju.** Statystyka: redukcja obrażeń magicznych. Klimat: `&8&oW tafli liścia gaśnie obcy czar.` Działanie: 12 many, odnowienie 18 s, na siebie; przez 5 s osłona pochłania do 8 obrażeń magicznych, a po pierwszym pochłoniętym trafieniu oddaje napastnikowi 4 obrażenia magiczne, raz. Bariera Many osłania kolejny cios, Nić trzyma wroga dalej. Obietnica: odbija część czaru. Różnica: reakcja obronna zamiast pioruna. Ryzyko: **wysokie**, filtr typu obrażeń i źródło trafienia. `&6◆ Kostur dwuręczny: odbicie +2 obrażenia.`

### Elf — Wyrocznia (akolita)

**Wyrocznia → Ostrze Księżyca | Łuk Pełni.** Statystyka: szansa krytyczna. Klimat: `&8&oPełnia ostrzy srebrny sierp.` Działanie: 1 Dusza, odnowienie 10 s, łuk 3; każdy z maksymalnie trzech wrogów dostaje 5 obrażeń fizycznych, a istniejący własny ognik leczy rzucającego o 2 HP raz. Sierp Księżyca sprowadza ognik, Rozkaz trzyma go przy froncie. Obietnica: walczy sierpem w zwarciu. Różnica: osobista ofensywa zamiast opieki. Ryzyko: **średnie**, tylko jedno leczenie od ognika. `&6◆ Sierp: cięcie +2 obrażenia.`

**Wyrocznia → Przewodnik Ogników | Ścieżka Ogników.** Statystyka: większa pula Dusz, zgodnie z wynikiem generatora. Klimat: `&8&oOgniki prowadzą rannych do świtu.` Działanie: 2 Dusze, odnowienie 16 s, promień 6; każdy z maksymalnie dwóch sojuszników otrzymuje 2 HP leczenia co 2 s przez 6 s, a jeden własny ognik zyskuje 4 HP osłony na 6 s. Sierp daje ognik, Opieka pomaga jednemu rannemu. Obietnica: prowadzi ogniki ku leczeniu. Różnica: utrzymuje drużynę zamiast ciąć. Ryzyko: **średnie**, osłona tylko jednego ognika. `&6◆ Sierp: pierwsze leczenie +2 HP.`

### Ork — Wódz Wojny (wojownik)

**Wódz Wojny → Tyran | Żelazny Rozkaz.** Statystyka: obrona. Klimat: `&8&oWódz rzuca wyzwanie całej hordzie.` Działanie: 30 Szału, odnowienie 17 s; prowokuje do pięciu wrogów w promieniu 6, a rzucający otrzymuje Resistance I na 6 s. Przez 6 s każde otrzymane trafienie daje 5 Szału, najwyżej 25. Bez osłony sojuszników. `&6◆ Tarcza: okno Szału trwa 7 s.`

**Wódz Wojny → Krwioburca | Uderzenie Krwi.** Statystyka: szansa krytyczna. Klimat: `&8&oKrew podsyca huk topora.` Działanie: 40 Szału, odnowienie 10 s, cel do 4; cios zadaje 8 obrażeń fizycznych, a przy zdrowiu rzucającego poniżej 50% dodaje 4 obrażenia i zwraca 10 Szału po trafieniu. Natarcie Wodza ustawia cel, Gniew może zużyć odzyskany Szał. Obietnica: ranny ork bije mocniej. Różnica: ryzykowna ofensywa zamiast osłony. Ryzyko: **średnie**, próg HP bada rzucającego. `&6◆ Topór: premia poniżej 50% HP +2 obrażenia.`

### Ork — Kieł Stepu (ninja)

**Kieł Stepu → Zaklinacz Bestii | Gwizd Stepowego Wilka.** Statystyka: większe zdrowie. Klimat: `&8&oStep odpowiada na krótki gwizd.` Działanie: 40 Skupienia, odnowienie 22 s, cel do 12; przy rzucającym pojawia się jeden wilk na 10 s, z 18 HP i ciosami po 3 obrażenia, który ściga cel oznaczony własnym Śladem Kła. Wilk jest dzieckiem właściciela, wymaga wolnego miejsca w limicie trzech i nie nakłada Znamion. Obietnica: bestia słucha gwizdu i tropu. Różnica: wytrzymały łowca z towarzyszem zamiast miotacza. Ryzyko: **wysokie**, AI i limit dzieci. `&6◆ Włócznia: wilk ma +4 HP.`

**Kieł Stepu → Kościany Grot | Salwa Kości.** Statystyka: obrażenia pocisków. Klimat: `&8&oKości przodków lecą przed łowcą.` Działanie: 35 Skupienia, odnowienie 11 s; wachlarz trzech grotów pod kątami około -15°, 0°, +15°, zasięg 16 bloków. Każdy trafiony grot zadaje 4 obrażenia typu PROJECTILE i Znamię; jeden cel może otrzymać najwyżej dwa groty z salwy. `&6◆ Włócznia: czwarty grot.`

### Ork — Szaman (mag)

**Szaman → Burzownik | Grom z Totemu.** Statystyka: obrażenia czarów. Klimat: `&8&oNad totemem pęka niebo.` Działanie: 12 many, odnowienie 12 s, punkt do 16, promień 3; po 0,8 s każdy z maksymalnie trzech wrogów otrzymuje 6 obrażeń magicznych, a stojący we własnym Oku Burzy po 2 dodatkowe raz. Oko wyznacza strefę, Wybuch pozostaje większym wydatkiem many. Obietnica: przyzywa piorun nad polem bitwy. Różnica: obrażenia obszarowe zamiast rytmu wsparcia. Ryzyko: **średnie**, własność punktu burzy. `&6◆ Totem: promień gromu 4 bl.`

**Szaman → Głos Bębna | Rytm Przodków.** Statystyka: krótsze odnowienie. Klimat: `&8&oBęben niesie krok przez step.` Działanie: 10 many, odnowienie 16 s, promień 6; rzucający i do dwóch sojuszników zyskują szybkość I na 4 s i po 4 HP osłony, co pozwala utrzymać własne Oko Burzy i walczyć wokół niego. Podpis pozwala częściej stawiać Oko bez wzmacniania pojedynczego ciosu. Obietnica: wojenny rytm prowadzi sprzymierzeńców i duchy burzy. Różnica: mobilność i wsparcie zamiast gromu. Ryzyko: **średnie**, selekcja sojuszników. `&6◆ Totem: osłona sojuszników +2 HP.`

### Ork — Kapłan Krwi (akolita)

**Kapłan Krwi → Awatar Wojny | Ofiara Młota.** Statystyka: obrażenia fizyczne. Klimat: `&8&oBóg bitwy przemawia żelazem.` Działanie: 1 Dusza, odnowienie 11 s, cel do 4, promień 2; młot uderza do trzech wrogów po 6 obrażeń fizycznych, a jeden istniejący własny przodek otrzymuje szybkość I na 4 s. Przodek Młota daje sługę, Żniwo kończy jego udział. Obietnica: awatar osobiście walczy na froncie. Różnica: własny młot zamiast dowodzenia duchami. Ryzyko: **średnie**, jeden buff dla własnego sługi. `&6◆ Młot dwuręczny: promień uderzenia 3 bl.`

**Kapłan Krwi → Wołający Przodków | Zew Pobojowiska.** Statystyka: większa pula Dusz. Klimat: `&8&oImiona poległych wracają w bębnie.` Działanie: 2 Dusze, odnowienie 18 s, cel do 12; przy celu wzywa jednego przodka na 12 s, z 18 HP i ciosami po 3 obrażenia, a każdy z maksymalnie dwóch istniejących własnych sług dostaje 3 HP osłony. Wspólny limit trzech obowiązuje; Rozkaz kieruje oddziałem. Obietnica: dawni wojownicy rzeczywiście przychodzą. Różnica: oddział zamiast osobistego młota. Ryzyko: **wysokie**, AI i limit dzieci. `&6◆ Młot dwuręczny: przodek ma +4 HP.`

### Nieumarły — Rycerz Śmierci (wojownik)

**Rycerz Śmierci → Kustosz Krypt | Próg Krypty.** Statystyka: obrona. Klimat: `&8&oMartwy strażnik zamyka przejście.` Działanie: 30 Szału, odnowienie 16 s; punkt dwa bloki przed rzucającym tworzy na 6 s strefę o promieniu 3. Pierwszy impuls działa od razu; kolejne sprawdzają strefę co 0,5 s. Wrogowie w strefie są odpychani od punktu i spowalniani; rzucający wewnątrz ma Resistance I (20% mniej obrażeń). Nie daje osłony HP. `&6◆ Tarcza: strefa trwa 7 s.`

**Rycerz Śmierci → Żniwiarz | Cięcie Żniwiarza.** Statystyka: kradzież życia. Klimat: `&8&oKażde ostrze zbiera oddech.` Działanie: 40 Szału, odnowienie 11 s, cel do 4; cios zadaje 9 obrażeń fizycznych i leczy rzucającego o 25% rzeczywiście zadanych obrażeń, najwyżej 5 HP. Marsz z Krypty i Gniew podtrzymują ofensywną rotację. Obietnica: zbiera życie własnym cięciem. Różnica: samoleczenie zamiast obrony progu. Ryzyko: **średnie**, leczenie po redukcji obrażeń. `&6◆ Miecz dwuręczny: pułap leczenia 7 HP.`

### Nieumarły — Upiór (ninja)

**Upiór → Dusiciel | Zacisk Pętli.** Statystyka: szansa krytyczna. Klimat: `&8&oŁańcuch ucisza ostatni oddech.` Działanie: 35 Skupienia, odnowienie 11 s; pierwszy cel w linii do 6 bloków zostaje unieruchomiony na 1,5 s (Slowness 255 i blokada skoku) oraz otrzymuje 2 obrażenia bazowe. Każde istniejące Znamię dodaje 2 obrażenia, maksymalnie za pięć Znamion; skill zużywa Znamiona jak Egzekucja. `&6◆ Łańcuch: unieruchomienie trwa 2 s.`

**Upiór → Łucznik Ciszy | Strzała Bez Głosu.** Statystyka: obrażenia pocisków. Klimat: `&8&oMrok wypuszcza grot bez szelestu.` Działanie: 35 Skupienia, odnowienie 10 s, cel do 18; przywołany bezgłośny grot zadaje 7 obrażeń pociskowych i Znamię, a trafiony z co najmniej 10 bl. cel dostaje oślepienie na 2 s. Łańcuch pozostaje bronią Upiora, lecz Strzała, Rzut i dystansowa Egzekucja prowadzą tę rotację; Pętla zatrzymuje pościg. Obietnica: strzela z mroku bez dźwięku. Różnica: utrzymuje dystans zamiast zaciskać łańcuch. Ryzyko: **średnie**, bezgłośny efekt i statystyka pocisków. `&6◆ Łańcuch: oślepienie trwa 3 s.`

### Nieumarły — Lisz (mag)

**Lisz → Duszożerca | Głód Filakterium.** Statystyka: wampiryzm czarów. Klimat: `&8&oFilakterium pije cudzy oddech.` Działanie: 12 many, odnowienie 12 s, cel do 15; więź zadaje 3 obrażenia magiczne od razu i po 1 co sekundę przez 4 s, lecząc rzucającego o 50% rzeczywiście zadanych obrażeń, łącznie najwyżej 4 HP. Splot Duszy otwiera drugi kanał wysysania, Bariera chroni w zasięgu celu. Obietnica: czary karmią lisza. Różnica: pojedyncza ofiara zamiast zarazy w obszarze. Ryzyko: **średnie**, pułap leczenia i własność więzi. `&6◆ Filakterium: pułap leczenia 5 HP.`

**Lisz → Zwiastun Zarazy | Zaraza Kurhanu.** Statystyka: większa pula many. Klimat: `&8&oTam, gdzie staje, gnije ziemia.` Działanie: 18 many, odnowienie 17 s, punkt do 14, promień 3; strefa trwa 6 s i co 2 s zadaje do trzech wrogów po 2 obrażenia magiczne oraz spowolnienie na 1 s, bez kumulacji tej samej zarazy. Splot Duszy utrzymuje ofiarę w polu, większa pula many finansuje nękanie. Obietnica: zostawia rzeczywistą strefę rozkładu. Różnica: obszarowa presja zamiast więzi. Ryzyko: **wysokie**, liczba impulsów i własność znacznika. `&6◆ Filakterium: strefa trwa 8 s.`

### Nieumarły — Pasterz Ciszy (akolita)

**Pasterz Ciszy → Awatar Otchłani | Głód Otchłani.** Statystyka: kradzież życia. Klimat: `&8&oKosa karmi pustkę i nosiciela.` Działanie: 1 Dusza, odnowienie 11 s, cel do 4; **własny cios** kosą zadaje 7 obrażeń fizycznych i leczy rzucającego o 30% rzeczywiście zadanych obrażeń, najwyżej 4 HP, a jeden własny sługa przy celu dostaje 3 HP osłony. Kosiarz Ciszy i Rozkaz doprowadzają sługę, ale leczenie działa także bez niego. Obietnica: skradziona cisza karmi ostrze i rany. Różnica: osobista agresja zamiast orszaku. Ryzyko: **średnie**, fizyczne obrażenia i pułap leczenia. `&6◆ Kosa: pułap leczenia 6 HP.`

**Pasterz Ciszy → Pan Kurhanów | Głosy Kurhanów.** Statystyka: większa pula Dusz, zgodnie z wynikiem generatora. Klimat: `&8&oZ kurhanów wstają cudze głosy.` Działanie: 2 Dusze, odnowienie 18 s, cel do 12; przy celu wzywa jednego sługę na 12 s, z 16 HP i ciosami po 3 obrażenia, a każdy z maksymalnie dwóch istniejących własnych sług dostaje 4 HP osłony. Wspólny limit trzech obowiązuje; Rozkaz wybiera ofiarę, Żniwo poświęca orszak. Obietnica: umarli idą za szeptem. Różnica: zarządzanie sługami zamiast osobistej kradzieży życia. Ryzyko: **wysokie**, AI, limit i obciążenie. `&6◆ Kosa: sługa kurhanu ma +4 HP.`

## Zmiany w generatorze potrzebne do podpięcia

Zmiany wdrożone w partii 10. Dodać `SPEC_SKILLE: spec_id -> skill_id` z 32 wpisami:

| Klasa bazowa | Dwie specjalizacje → `skill_id` |
|---|---|
| `rycerz` | `bastion` → `SPEC_BASTION_MUR`; `czempion` → `SPEC_CZEMPION_WYZWANIE` |
| `agent` | `widmo` → `SPEC_WIDMO_JAD`; `jastrzab` → `SPEC_JASTRZAB_LIST` |
| `mag_akademii` | `elementalista` → `SPEC_ELEMENTALISTA_KRAG`; `przywolywacz` → `SPEC_PRZYWOLYWACZ_ECHO` |
| `swiety_akolita` | `serafin` → `SPEC_SERAFIN_ZASLONA`; `hierofant` → `SPEC_HIEROFANT_CHORAGIEW` |
| `ostrze_gaju` | `straznik_cierni` → `SPEC_STRAZNIK_CIERNI_MUR`; `tancerz_ostrzy` → `SPEC_TANCERZ_CIECIE` |
| `straznik_kniei` | `cien_lisci` → `SPEC_CIEN_LISCI_SIDLA`; `sokol` → `SPEC_SOKOL_DESZCZ` |
| `tkacz_arkanow` | `tkacz_zywiolow` → `SPEC_TKACZ_ZYWIOLOW_REFREN`; `straznik_zywiolow` → `SPEC_STRAZNIK_ZYWIOLOW_ZWIERCIADLO` |
| `wyrocznia` | `ostrze_ksiezyca` → `SPEC_OSTRZE_KSIEZYCA_LUK`; `opiekun_duchow` → `SPEC_OPIEKUN_DUCHOW_SWIT` |
| `wodz_wojny` | `tyran` → `SPEC_TYRAN_ROZKAZ`; `krwioburca` → `SPEC_KRWIOBURCA_SZAL` |
| `tropiciel` | `lowca_bestii` → `SPEC_LOWCA_BESTII_WILK`; `lowca_kosci` → `SPEC_LOWCA_KOSCI_SALWA` |
| `szaman` | `burzownik` → `SPEC_BURZOWNIK_GROM`; `totemista` → `SPEC_TOTEMISTA_RYTM` |
| `kaplan_krwi` | `awatar_wojny` → `SPEC_AWATAR_WOJNY_MLOT`; `szept_przodkow` → `SPEC_SZEPT_PRZODKOW_WEZWANIE` |
| `rycerz_smierci` | `straznik_grobu` → `SPEC_STRAZNIK_GROBU_PROG`; `zniwiarz` → `SPEC_ZNIWIARZ_CIECIE` |
| `widmo_grobu` | `cien` → `SPEC_CIEN_PETLA`; `strzelec_grobu` → `SPEC_STRZELEC_GROBU_STRZALA` |
| `mag_grobu` | `duszozerca` → `SPEC_DUSZOZERCA_WIEZ`; `nekromanta` → `SPEC_NEKROMANTA_ZARAZA` |
| `spaczony_akolita` | `awatar_otchlani` → `SPEC_AWATAR_OTCHLANI_GLOD`; `pasterz_umarlych` → `SPEC_PASTERZ_UMARLYCH_GLOS` |

1. Przy generowaniu **wyłącznie pliku specjalizacji** dopisać jej skill do `skills:` z `level: 10` i `max-level: 10`. Bazowa klasa go nie dostaje, sygnatura i trzy skille archetypu zostają. Definicje `SPEC_*` i skrypty MythicMobs znajdują się w osobnych plikach `Specjalizacje*.yml`.
2. Dodać `SPEC_SKILLE.values()` do `na_pasku` i weryfikacji unikalnego słowa nazwy. W generowaniu paska dodać gałąź dla dokładnego `spec_id`, która na poziomie 10 wiąże **slot 5**, bez szukania pierwszego wolnego slotu 1–4. Dotychczasowa pętla 1–4 obsługuje wspólne skille i nie może pobrać skilla siostrzanej specjalizacji.
3. Przy przejściu między specjalizacjami zastępować stary skill w slocie 5 nowym, również przy wczytaniu profilu; nie nadpisywać slotów 1–4. W lore klasy zamienić „Umiejętności specjalizacji: w przygotowaniu” na nazwę skilla i poziom 10. Perk Dusz w kartach ma odpowiadać wynikowi generatora.
4. Sprawdzić: 32 klucze `SPEC_SKILLE` równe 32 `sid` w `KLASY`, 32 unikalne `skill_id`, każdy zdefiniowany w MythicLib, żaden obecny w klasie bazowej, tylko właściwa specjalizacja może związać go w slocie 5. Generator i bramki statyczne są uruchamiane po wdrożeniu; test serwerowy pozostaje do wykonania osobno.

## Trzy największe ryzyka

1. **Stan i właściciel przy wielu graczach.** Pieczęć i Oko Burzy wymagają stanu przypisanego do właściciela; strefy Sidła, Próg i Zaraza działają z kontekstu gracza. Pociski nie przenoszą `skill.*` do `onHit`; wspólny `caster.*` może zostać nadpisany przez równoczesne rzuty.
2. **Cele, słudzy i obciążenie.** Obszary wymagają osobnych ścieżek mob/gracz; bestia i przodkowie muszą mieć AI, czas życia, właściwego właściciela i wspólny limit trzech. Wiele stref i sług wymaga próby grupowej oraz pomiaru TPS.
3. **Zgodność liczb z silnikiem.** `projectile-damage`, `lifesteal`, `spell-vampirism`, odbicie obrażeń magicznych i osłony trzeba zmierzyć dla typów użytych w skillach. Pula many Szamana i brak regeneracji Dusz mogą blokować rotację mimo poprawnej składni.

## Uwagi wdrożeniowe partii 10

- Wariant Korony Cierni to jawne impulsy co sekundę, gdyż aura oparta na trafieniu wręcz nie została potwierdzona w tej konfiguracji. Jednorazowe Sidło, Próg i Deszcz wykonują opóźnione tyki jako metaskille gracza wokół zapamiętanego punktu.
- Osłony HP są wykonywane przez mechanikę MythicMobs `shield{amount=...;maxabsorb=100}`. Resistance II daje 40% redukcji, Resistance I daje 20%.
- Pozostałe efekty z projektu zachowują temat i koszty, lecz część sprzężeń wymaga pomiaru na serwerze. Zwłaszcza rzeczywiste leczenie zależne od obrażeń po redukcji, filtrowanie wyłącznie obrażeń magicznych Zwierciadła i śledzenie jednego właściciela przy równoczesnych znacznikach nie mają jeszcze dowodu z testu serwerowego. Lore nie powinno obiecywać tych niezbadanych szczegółów.
- Slugi specjalizacji współdzielą limit `children{a=<3}` z bazowymi sługami. Strefy nie przywołują mobów, więc nie zajmują miejsca sługi.
