# Etap 4 — recenzja krzyżowa Y (2026-10-03)

**Bramka:** według [00-BRIEF.md §4](00-BRIEF.md) każda ocena <7 oznacza **BLOKUJĘ**. Obowiązują [05-synteza-v2-rozstrzygniecia.md R1–R5](05-synteza-v2-rozstrzygniecia.md). Oceniam projekt, nie wynik nieprzeprowadzonego testu w grze. Symulacja: Mag i Wojownik bez specjalizacji, solo na poziomie lochu; liczby obrażeń są projektowe.

## 001 — Kwietna Polana — **BLOKUJĘ** (minimum 4/10)

| Kryterium | 0–10 | Jednozdaniowy dowód |
|---|---:|---|
| Fun walki | 6 | Dwie kotwy po 60 HP, Korowiec Bramy, Sowiedź i boss 600 HP ustawiają serię szczytów bez odpoczynku między S3E–S6 ([001:70–77](10-loch-001-kwietna-polana.md)). |
| Mechanika i zwrot | 7 | Taran z pąka Korowca otwiera mur i odwraca wcześniejsze deptanie, lecz Siewca rozsadza zasadę jednego pąka ([001:23,70](10-loch-001-kwietna-polana.md)). |
| Czytelność | 6 | Ten sam pąk ma dawać Pniaczka lub Korzeń według wielkości poległego, a rozróżnia go tylko skala ×1,5 ([001:16](10-loch-001-kwietna-polana.md)). |
| Topologia | 7 | Dwie doliny, kładka między nimi i wczesny widok dzwonnicy dają realną pętlę ([001:39–59](10-loch-001-kwietna-polana.md)). |
| Piękno i WOW | 8 | Dzwon i fala płatków mają konkretny kadr, czas i miejsce, choć opisane emitery wymagają pomiaru obciążenia ([001:145–150](10-loch-001-kwietna-polana.md)). |
| Sens fabularny | 8 | Nóż Ogrodnika wyjaśnia szczepienie sadu bez wymogu poprzedniej wiedzy ([001:31–33](10-loch-001-kwietna-polana.md)). |
| Powtarzalność | 6 | Nastrój „Kwitnący Sad” zmienia stałe 5 s pąka na 4 s w Normalnym, więc wariant przeczy prawu aktu ([001:170–176](10-loch-001-kwietna-polana.md)). |
| Nagroda/progres | 6 | Liczba skrzyń zależy od oceny obejmującej sekrety i Łaskę, a dobicie młodych daje materiały zamiast kosmetyku ([001:106–107,178–188](10-loch-001-kwietna-polana.md)). |
| Wykonalność | 4 | Plan B limitu usuwa encję najdalszą, a nie najstarszą; `world.paki` nie opisuje kolejki per instancja ([001:206](10-loch-001-kwietna-polana.md)). |
| Różnorodność | 7 | Wyboru „depcz małe, zostaw duże” nie powtarza inny loch, choć rój Pniaczków wraca w 010 ([001:111–126](10-loch-001-kwietna-polana.md)). |

**Obowiązkowe poprawki:**
1. R1 ma identyczny pąk po jednym poległym → [001:16,70,173](10-loch-001-kwietna-polana.md) różnicuje model, daje Siewcy trzy pąki i skraca czas w Normalnym → jeden pąk tego samego modelu na zgon, stałe 5/8/4 s; trzy przyszłe Pniaczki lub duży Korzeń mogą wynikać dopiero ze skutku dojrzenia, czytelnie oznaczonego na poległym/terenie.
2. Limit 12 jest niewykonalny według zapisanego planu → [001:206](10-loch-001-kwietna-polana.md) wybiera `sort=furthest`, nie najstarszy, i globalny licznik → znacznik czasu/indeks i licznik **instancji**, trzynasty widocznie ususza najstarszy; wykonać test 13 zgonów w dwóch równoległych instancjach.
3. Mag solo kusi się kitować bossa poza 10 blokami, a Wojownik w F3 zależy od trafienia bossa Korzeniem z konaru → [001:128–143](10-loch-001-kwietna-polana.md) nie podaje częstotliwości salwy ani gwarancji toru bossa → w blockoucie zmierzyć oba przebiegi, ustalić maksymalny odstęp salwy i wymusić czytelne wejście bossa w zasięg Korzenia po chybionym ataku; zostawić obrażenia bez pętania możliwe.
4. Moralny wybór i zbieranie sekretów zmieniają siłę łupu → [001:106–107,178–179](10-loch-001-kwietna-polana.md) → bazowy EXP, sprzęt i materiały profesji równe przy obu decyzjach i wszystkich ocenach; ocena daje wygląd/tytuł, a materiały młodych również dostępne bez ich zabijania.

**„Suche i bez sensu” §4:** kierunki **PASS** (mapa poprawnie −Z=północ); ≤1 komunikat **PASS** (Jaro poza walką); obietnice **FAIL** (R1 i łup zależny od Łaski); nagroda **WARUNKOWO** (wysoka, lecz moralnie nierówna); cel czytelny **WARUNKOWO** (duży/mały pąk); kara za ignorowanie **PASS** (leczenie bossa); kitowanie **WARUNKOWO** (salwa bez tempa). Test solo: Mag wie, że ma deptać małe pąki, lecz może objeżdżać arenę; Wojownik ma wyraźny cios w oknie, lecz F3 wymaga sprawdzenia toru.

**R1:** NIE (liczba/wygląd/czas/limit). **R2:** WARUNKOWO (tabela jest, wynik solo niezmierzony). **R3 001:** TAK co do Taran–Brama i braku latarni; Siewca jest dmuchawcem.

## 004 — Łany Strachów — **BLOKUJĘ** (minimum 5/10)

| Kryterium | 0–10 | Jednozdaniowy dowód |
|---|---:|---|
| Fun walki | 7 | Skradanie i otwarty marsz dają różne wejścia, ale głośna miedza grozi stałym napływem wron ([004:54–58,74](10-loch-004-lany-strachow.md)). |
| Mechanika i zwrot | 8 | Snopowiec używa kryjówki przeciw graczowi, a boss ścina własne oczy, więc motyw rzeczywiście się odwraca ([004:20–23,124](10-loch-004-lany-strachow.md)). |
| Czytelność | 5 | W wariancie Wiatr nieruchoma kępa ma zdradzać Snopowca, choć sam tekst mówi, że wiatr tę wskazówkę odbiera ([004:22,173–174](10-loch-004-lany-strachow.md)). |
| Topologia | 7 | Dwa pola i miedza są trzema wyborami podejścia, lecz oba klucze elit pozostają obowiązkowe ([004:37–60](10-loch-004-lany-strachow.md)). |
| Piękno i WOW | 8 | Jednoczesny obrót 14 głów i błysk nad olbrzymim Strachem to rozpoznawalny kadr ([004:144–150](10-loch-004-lany-strachow.md)). |
| Sens fabularny | 7 | Księga zmian i imiona na Strachach wiążą horror pól z przysięgą Bractwa ([004:25–31](10-loch-004-lany-strachow.md)). |
| Powtarzalność | 7 | Losowe pasy koszenia realnie zmieniają bezpieczne kwatery i drogę ([004:169–175](10-loch-004-lany-strachow.md)). |
| Nagroda/progres | 6 | „Zbierz żniwo” daje ×2 materiały zbożowe, mimo zasady, że moralny wybór nie daje mocy ([004:165–167](10-loch-004-lany-strachow.md)). |
| Wykonalność | 5 | Wykrycie wymaga niepotwierdzonego złożenia FOV, linii wzroku, kucania i bloku Nexo, a plan B nadal potrzebuje rozróżniania stref ([004:199–203](10-loch-004-lany-strachow.md)). |
| Różnorodność | 8 | Widzialność i wycinana kryjówka nie są osią innego lochu; wspólny snop z 009 jest już jednak bliski wizualnie ([004:15](10-loch-004-lany-strachow.md), [009:8](10-loch-009-dozynki-w-malowanej-wsi.md)). |

**Obowiązkowe poprawki:**
1. Główna wskazówka nie jest zawsze prawdziwa → [004:22,89,173–174,209](10-loch-004-lany-strachow.md) twierdzi jednocześnie, że Snopowiec nie faluje i że Wiatr ukrywa tę różnicę → zachować drugą wskazówkę niezależną od pogody (cień/ruch oczu/dźwięk) i pokazać ją bezpiecznie przed pierwszą zasadzką.
2. Głośna droga nie jest równie oceniana → [004:11,58,74,176–177](10-loch-004-lany-strachow.md) szacuje ją na ~17 min, a punkt oceny „szybko” wymaga ≤13 min; brak progów S–C → dać oddzielny, osiągalny warunek osiągnięcia S dla obu dróg i progi ocen, bez odbierania bazowego łupu.
3. Alarm może spiętrzyć zagrożenia i kłóci się z deklaracją pełnoprawnej trasy → [004:56–57,72–75,191](10-loch-004-lany-strachow.md) pokazuje 6 Strachów oraz 3–4 wrony i Kosiarza na alarm bez limitu/cooldownu → jeden aktywny alarm na pole, jawne 40 s odnowienia i limit wron/Kosiarzy; sprawdzić głośną drogę Magiem bez skradania i Wojownikiem bez osłon.
4. Wykrycie i koszenie są szkicem, nie zweryfikowaną realizacją → [004:199–203](10-loch-004-lany-strachow.md) używa `blocktype` dla Nexo i `fill ... replace <łan>` bez wyniku sondy → test na klonie: gracz kuca w łanie, na ściernisku i na granicy; jeśli rozpoznanie Nexo zawiedzie, przyjąć już w karcie wariant znaczników/vanilla wheat i pokazać koszt encji oraz bloków.
5. Decyzja fabularna daje podwojony surowiec → [004:165–167](10-loch-004-lany-strachow.md) → oba wybory dają równy bojowy łup i materiały; różnić świadectwo, wygląd pola i kosmetyk.

**„Suche i bez sensu” §4:** kierunki **PASS** (mapa północ u góry); ≤1 komunikat **PASS** (alarm dźwiękiem); obietnice **FAIL** (wskazówka Wiatr, równe S); nagroda **FAIL** (moralnie nierówna); cel **WARUNKOWO** (dwa klucze widać, ukrycie wymaga testu); ignorowanie **PASS** (alarm); kitowanie **WARUNKOWO** (głośna trasa i 10 s w łanie bez limitu alarmu). Mag może bezkarnie strzelać w oczy z łanu, jeśli wrogowie nie sprawdzają jego pozycji po trafieniu; Wojownik rozumie skradanie, lecz na miedzy może dostać kilka alarmów naraz.

**R1:** NIE w pełni (brak opisu limitu 12, usychania najstarszego, pąka gracza/planu B i deptania przez sługi; skutek/czas są zgodne). **R2:** WARUNKOWO (tabela jest; test alarmu i strzału Maga potrzebny). **R3 004:** WARUNKOWO (głośna droga istnieje i kosztuje, ale jej uczciwość oraz wykrywanie nie są dowiedzione).

## 007 — Grządki Maluczkich — **BLOKUJĘ** (minimum 4/10)

| Kryterium | 0–10 | Jednozdaniowy dowód |
|---|---:|---|
| Fun walki | 7 | Przymusowa zmiana stanu przez Ćmę jest mocnym zwrotem, ale Mały w S4 musi stawić czoło pięciu Mrówkom i Kosarzowi przy −50% obrażeń ([007:79,98–105](10-loch-007-grzadki-maluczkich.md)). |
| Mechanika i zwrot | 8 | Dmuchawiec/rosa, wymuszony Pyłek i Język bossa tworzą pełny egzamin z dwóch stanów ([007:16–26,122](10-loch-007-grzadki-maluczkich.md)). |
| Czytelność | 5 | Plan B nie zmniejsza gracza, a Ziutka obiecuje „zrobisz się mały”, notatka Darnika mówi „zmniejszony pyłkiem” ([007:5–6,34–40](10-loch-007-grzadki-maluczkich.md)). |
| Topologia | 5 | Jeden z dwóch otwartych tuneli wystarcza do S6, a beat chart opisuje tylko jeden; brak wyboru dwóch celów lub użytecznej alternatywy w tej próbie ([007:49,70,80](10-loch-007-grzadki-maluczkich.md)). |
| Piękno i WOW | 9 | Widok spod kapusty i kropla rosy jak głaz mają konkretną perspektywę w obu wariantach ([007:143–148](10-loch-007-grzadki-maluczkich.md)). |
| Sens fabularny | 6 | Wariant B przeczy wyjaśnieniu Darnika i kwestii Ziutki, czyli samodzielny dowód historii staje się fałszywy ([007:34–40](10-loch-007-grzadki-maluczkich.md)). |
| Powtarzalność | 7 | Dwa z trzech tuneli i losowe F2 zmieniają przeciwników i stan potrzebny do walki ([007:165–168](10-loch-007-grzadki-maluczkich.md)). |
| Nagroda/progres | 6 | Zbieranie kijanek daje ×2 materiały, a ich pozostawienie tylko Łaskę, więc wybór fabularny zwiększa moc ([007:162–163](10-loch-007-grzadki-maluczkich.md)). |
| Wykonalność | 4 | Wariant A zależy od ośmiu prób skali, a wariant B wymaga osobnej ogromnej geometrii i nie gwarantuje działania bram oraz narracji ([007:191–213](10-loch-007-grzadki-maluczkich.md)). |
| Różnorodność | 8 | Ogród z perspektywy żuka jest osobny, choć Kret wykonuje znaną z kilku lochów zasadzkę z cienia ([007:92,143–148](10-loch-007-grzadki-maluczkich.md)). |

**Obowiązkowe poprawki:**
1. Dwa warianty mają sprzeczną opowieść → [007:5–6,34–40](10-loch-007-grzadki-maluczkich.md) w B nie zachodzi zmniejszenie, mimo kwestii i notatki → zapisać wersje dialogu/odkrycia zależne od A/B, albo wybrać jeden docelowy wariant po próbie; nie obiecywać graczowi przemiany, której nie ma.
2. Wybór trasy nie ma konsekwencji w jednym przebiegu → [007:49,70,80,160](10-loch-007-grzadki-maluczkich.md) mówi „2 z 3”, po czym prowadzi jednym tunelem, a sekret trzeciego każe zdeptać „zatyczkę” zamiast pąka → dwa otwarte tunele połączyć pętlą z odmiennym kosztem/łupem, trzeci udostępnić jasno opisanym sekretem niezależnym od R1.
3. R1 jest niekompletne → [007:18,188,191–213](10-loch-007-grzadki-maluczkich.md) nie podaje jednolitego pąka po każdej śmierci, limitu 12, widocznego wyparcia, pąka gracza/planu B i automatycznego deptania przez sługi → dopisać jeden wspólny protokół R1 i przetestować 13 pąków, śmierć gracza oraz wszystkie źródła trafienia.
4. S4 ma wymuszony stan słaby przeciw rojowi → [007:16,51,79,90,93](10-loch-007-grzadki-maluczkich.md) wymaga Małego pod liśćmi, gdzie Kosarz i Mrówki na niego polują → dać dostęp do rosy lub płyty Ciężkiego w sali, wyznaczyć bezpieczny odwrót i zmierzyć przejście Magiem/Wojownikiem solo bez zaklęć obszarowych.
5. Próg wykonalności A/B i łup → [007:191–213,162–163](10-loch-007-grzadki-maluczkich.md) nie ma wyniku prób ani równych materiałów → prowadzący wykonuje 8 prób A na klonie przed zamknięciem projektu, B osobno waliduje geometrię/AI; obie decyzje o kijankach dają te same materiały bojowe, różnią kosmetyk.

**„Suche i bez sensu” §4:** kierunki **WARUNKOWO** (−Z=północ poprawne, ale strzałki dopiero do sprawdzenia); ≤1 komunikat **PASS**; obietnice **FAIL** (Ziutka w B); nagroda **FAIL** (kijanki); cel **WARUNKOWO** (wejście jasne, drugi tunel nie ma roli); kara za ignorowanie **PASS** (Mrówki/Język); kitowanie **WARUNKOWO** (Wylew co 6 s, ale w wariancie B arena 40×40 i liście y+4). Mag solo w S4 może utknąć z −50% obrażeń przeciw rojowi; Wojownik ma kontrę Dużego, ale wejście pod liście ją odbiera.

**R1:** NIE (braki protokołu). **R2:** WARUNKOWO (tabela jest, lecz solo S4 i pet/skalowanie nieprzetestowane). **R3 007:** WARUNKOWO (A ma listę prób, B istnieje, losowanie F2 i Kosarz są; próba nie ma wyniku, topologia i narracja B wymagają naprawy).

## 010 — Matecznik — **BLOKUJĘ** (minimum 4/10; bez taryfy ulgowej)

| Kryterium | 0–10 | Jednozdaniowy dowód |
|---|---:|---|
| Fun walki | 6 | F1 wymaga dwóch cięć przed jakimkolwiek obrażeniem bossa, a F3 kończy się trzema obowiązkowymi cięciami zamiast walką ([010:98–100](10-loch-010-matecznik.md)). |
| Mechanika i zwrot | 4 | Traf węzeł, zgaś osłonę i bij w odsłonie jest osią 005, a tu dostaje tylko obowiązkowe deptanie po cięciu ([010:14–17,78–85](10-loch-010-matecznik.md), [005:5,75–81](10-loch-005-oranzeria-darnika.md)). |
| Czytelność | 5 | Pąk po śmierci i pąk z żywej nici mają ten sam sygnał, lecz różne skutki i wyjątkowe czasy ([010:8–9,78–80,98–109](10-loch-010-matecznik.md)). |
| Topologia | 8 | Trzy konary i zjazdy do ogniska prowadzą do jednorazowego zejścia przez pień ([010:31–52](10-loch-010-matecznik.md)). |
| Piękno i WOW | 9 | Panorama dziewięciu miniwysp i zapadająca Korona tworzą precyzyjny finał wizualny ([010:121–131](10-loch-010-matecznik.md)). |
| Sens fabularny | 8 | Grób Dary tłumaczy czyn Darnika bez ekspozycji, lecz opieka nad plonami jest wskazana tylko dla jednej decyzji ([010:24–29,144–146](10-loch-010-matecznik.md)). |
| Powtarzalność | 6 | Losuje się kolejność F2 i konar z dodatkową elitą, ale rdzeń każdej sali nadal każe ciąć ten sam statyczny węzeł ([010:59–64,149–151](10-loch-010-matecznik.md)). |
| Nagroda/progres | 7 | Sprzęt dla czterech archetypów i wspólna nagroda są podane, lecz progi S–C nie mają przypisanych nagród ([010:152–158](10-loch-010-matecznik.md)). |
| Wykonalność | 4 | Pąki rodzą się przy cięciu żywej nici, a F2 wymaga 4 s w Normalnym wbrew R1; brak planu dla pąka gracza i kolejki 12 ([010:78–80,99,165–173](10-loch-010-matecznik.md)). |
| Różnorodność | 4 | Szczepiec, nici, osłona, statyczny węzeł i egzamin bossa powtarzają Oranżerię 005 w tym samym akcie ([010:15,71,78–85](10-loch-010-matecznik.md), [005:5,67–81](10-loch-005-oranzeria-darnika.md)). |

**Obowiązkowe poprawki:**
1. Mechanika łamie R1 na poziomie definicji → [010:8–9,14,16,78–80,104,108](10-loch-010-matecznik.md) tworzy „pąki” z żywych nici, larw i nasion, a [010:99](10-loch-010-matecznik.md) daje im 4 s w Normalnym → pąk R1 wyłącznie po poległym, identyczny model/5 s (Ł 8/T 4), jeden skutek lokalny; przy nici dać osobny, inaczej nazwany znak odrastania, z inną cząsteczką i czasem.
2. Finał powtarza 005 → [005:5,67,75–81](10-loch-005-oranzeria-darnika.md) i [010:15,78–85,98–100](10-loch-010-matecznik.md) każą ciąć nić/węzeł, by osłabić osłonę → przenieść motyw 010 na **przycinanie konarów jako zmianę przestrzeni i repertuaru ataków Darnika**, a nie trafianie statycznych węzłów; pąki poległych mają w tym pomagać/szkodzić, nie resetować cięcia.
3. R3 wymaga walki jako kulminacji → [010:98–100](10-loch-010-matecznik.md) czyni Darnika nietykalnym w F1 i automatycznie kończy F3 po trzech niciach → usunąć twardą nietykalność i warunek „ostatnia = koniec”; cięcia dają okna, osłony lub obrażenia, lecz zwycięstwo wymaga pokonania walczącego Darnika.
4. R2 i solo są niepewne → [010:76,109,116–119](10-loch-010-matecznik.md) każe samemu ciąć węzeł przy stopach, a Akolicie przypisuje tylko deptanie przez AI; żadna próba nie dowodzi trafienia petem/przywołańcem węzła → węzeł przenieść przed gracza w widoczny, duży hitbox, albo dać kontrę własnym ciosem/ruchem dla każdej klasy; sprawdzić Magiem i Wojownikiem bez specjalizacji oraz petem na klonie.
5. Brakuje obowiązkowego protokołu i domknięcia wyboru → [010:165–173,144–146](10-loch-010-matecznik.md) nie opisuje limitu 12, wyparcia, pąka gracza/planu B ani opieki nad plonami przy pozostawieniu Darnika → dopisać per-instancyjną kolejkę i bezpieczny plan B dla śmierci, a Hannę wskazać jako opiekunkę zbiorów w obu zakończeniach.

**„Suche i bez sensu” §4:** kierunki **PASS** (północ u góry); ≤1 komunikat **PASS** (kwestie poza walką); obietnice **FAIL** (nazwa pąka dla innych źródeł, finał „walka”); nagroda **WARUNKOWO** (duża, progi ocen nieopisane); cel **WARUNKOWO** (węzły widoczne, sens dwóch pąków nie); kara za ignorowanie **PASS** (zrost leczy bossa); kitowanie **WARUNKOWO** (szarża w najdalszego, bez limitu/frekwencji). Mag solo ma łatwy dystans do statycznych węzłów i może ominąć walkę w F3; Wojownik biega po rampach do trzech węzłów, gdy boss zajmuje środek.

**R1:** NIE. **R2:** WARUNKOWO (tabela jest, lecz własny węzeł/pet nie dowiedzione). **R3 010:** NIE (walka wyparta bramkami, zrost 005 skopiowany; Darnik przeżywa, odpowiedzialność za plony tylko w jednej gałęzi).

## Powtórki w całej dziesiątce — przegląd pozostałych kart

| Co się powtarza | Dowód i ocena | Naprawa dla recenzowanych |
|---|---|---|
| **Mechanika: 005↔010** | 005 tnie jasny zrost dowolnym trafieniem, usuwa gardę i odsłania bossa ([005:5,67,75–81](10-loch-005-oranzeria-darnika.md)); 010 robi to samo przy Słupie i Darniku ([010:15,78–100](10-loch-010-matecznik.md)); **powtórka blokująca**. | Przebudować 010 według poprawki 2. |
| **Wróg: 001↔010** | Pniaczek i frontalny obrońca wracają jako „jak 001” i Snopowy Strażnik z gardą ([001:88–90](10-loch-001-kwietna-polana.md), [010:70–75](10-loch-010-matecznik.md)); finał może używać krewniaków, lecz wymaga nowej funkcji. | W 010 Pniaczki zostawić wyłącznie jako skutek śmierci, a obrońcę zastąpić przeciwnikiem pracującym na zmianach terenu. |
| **Boss: 003/006/007/010** | Chybiona szarża/skok → krótka odsłona powraca u Królowej, Tura, Ropuchy i kończyn Darnika ([003:88](10-loch-003-pasieka-krolowej-barci.md), [006:77](10-loch-006-wawozy-tura.md), [007:122–123](10-loch-007-grzadki-maluczkich.md), [010:106](10-loch-010-matecznik.md)); sama kontra jest poprawna, lecz jako główny haczyk bossa stałaby się kopią. | Zostawić Ropuchę przy zmianie stanu, a 010 oprzeć na trwałej przemianie pola, nie kolejnym oknie po ataku. |
| **Kadr WOW: 001/004/010** | 001 pokazuje dzwonnicę z obozu, 004 pole z pagórka, 010 wyspy z Korony ([001:145–150](10-loch-001-kwietna-polana.md), [004:144–150](10-loch-004-lany-strachow.md), [010:121–131](10-loch-010-matecznik.md)); punkty widokowe są podobnym startem kadru, ale obrazy i przemiany są różne. | Zachować kadry; w 010 utrzymać zapadanie Korony jako zdarzenie widziane podczas gry. |
| **Start starć: 001/003/004/005/006/007/008/009/010** | W większości pierwsze walki zaczynają się od bezpiecznego widoku i inicjatywy gracza; zaskoczenie występuje też w 001 S3W, 004 drugim polu, 007 S3/S6 i 010 konarze ([001:68–75](10-loch-001-kwietna-polana.md), [004:70–78](10-loch-004-lany-strachow.md), [007:78–83](10-loch-007-grzadki-maluczkich.md), [010:57–64](10-loch-010-matecznik.md)). | W 010 rozdzielić trzy konary na inne początki niż kolejny „wróg zaskakuje / gracz zaskakuje / most się zamyka”; w 001 i 004 zmierzyć serię wysokiego tempa. |
| **Skutek pąka: 004↔009** | Oba dojrzewają w Snop osłonowy ([004:15](10-loch-004-lany-strachow.md), [009:8](10-loch-009-dozynki-w-malowanej-wsi.md)), chociaż 004 używa widzialności, a 009 rytmu; nie jest to powtórka głównej mechaniki, ale słaby wyróżnik prawa aktu. | W 004 nazwać i pokazać snop jako ruchomą kryjówkę tylko dla wzroku, odróżnioną bryłą od nieruchomej osłony 009. |

## Weryfikacja stosu i granice oceny

- Lokalne pomiary: [mythicmobs-cicha-walidacja.md](/home/dev/.claude/projects/-opt-docker-mc-data-world1/memory/mythicmobs-cicha-walidacja.md), [mm-warunki-inline-to-rzucajacy.md](/home/dev/.claude/projects/-opt-docker-mc-data-world1/memory/mm-warunki-inline-to-rzucajacy.md), [mythicdungeons-funkcje-bez-edytora.md](/home/dev/.claude/projects/-opt-docker-mc-data-world1/memory/mythicdungeons-funkcje-bez-edytora.md), [md-wejscie-kick-floating.md](/home/dev/.claude/projects/-opt-docker-mc-data-world1/memory/md-wejscie-kick-floating.md): MM 5.11.2 potrafi milczeć przy błędnym warunku/mechanice, warunek inline bada rzucającego, MD nie podtrzymuje `delay` w swoim tymczasowym casterze, a generowanie mapy przy wejściu grozi kickiem; stąd wymagane próby skutku w klonie i wypieczony teren.
- Oficjalna [wiki MythicMobs o warunkach celu inline](https://git.mythiccraft.io/mythiccraft/MythicMobs/-/wikis/skills/conditions/in-linetargetconditions?version_id=4ec33679c456387103d5504218554dae838fb3e5) oznacza tę składnię jako Premium; darmowy wariant używa `TargetConditions` metaskilla. W 004 samo wskazanie `TargetConditions` jest właściwym kierunkiem, ale nie dowodzi działania `blocktype` na Nexo ani całej koniunkcji.
- Nie uruchamiałem świata ani pluginów (brief zabrania dotykania serwera); oceny wykonalności odnoszą się do dowodów w kartach i pamięci, a nie do przeprowadzonej tu próby. W szczególności A w 007, wykrycie w 004 oraz celowanie AI/petów w 010 wymagają pomiaru, zanim autor oznaczy je jako „działa”.
