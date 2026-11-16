# Nieumarły — plan napraw i wiarygodnej weryfikacji

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Realizować kolejno, zapisując dowody odbioru każdego etapu.

**Goal:** Naprawić wykazane wady modelu gracza oraz testów, nadać postaci wyraźny charakter świadomego nieumarłego i potwierdzić gotowość oddzielnie dla grafiki, integracji i gry.

**Architecture:** Najpierw naprawić narzędzia pomiaru i powiązać dowody z konkretnym projektem. Następnie zamknąć kontrakt stanów i cyklu życia, dopracować wygląd oraz ruch, a na końcu sprawdzić całość w kliencie Minecraft. Kontrola pliku, pomiar w Blockbenchu i test serwera pozostają odrębnymi bramkami.

**Tech Stack:** Python/Pillow, Blockbench desktop + MCP, ModelEngine 4.0.9, CharacterCreator Java, Paper 1.21.11, Nexo.

**Spec:** `output/undead-player-v5/reviews/2026-09-18-independent-review.md` oraz wymagania i ukończone kroki `docs/superpowers/plans/2026-09-18-undead-player-quality-plan.md`. Ten plan zmienia kolejność pozostałej pracy; nie unieważnia wykonanych porównań twarzy ani kopii bezpieczeństwa.

## Global Constraints

- Docelowy identyfikator: `race_nieumarly`.
- Wersje V2, V3 i V4 zachować. Nie nadpisywać ich projektów ani renderów.
- Nie zmieniać twarzy, proporcji i tekstur jednocześnie w jednej iteracji.
- Nie modyfikować postaci istniejących graczy do testów.
- Nie traktować obecności animacji w pliku jako dowodu jej wyzwalania w grze.
- Użytkownik wybrał twarz B podczas wykonania planu. Zachować ją podczas pracy nad pozostałymi elementami.
- Ten dokument jest planem, nie wdrożeniem. Przed pracą w zewnętrznym repozytorium odczytać jego AGENTS.md i zasady środowisk; testować integrację na staging/dev. World1 dopiero po odbiorze.
- Zmiany artystyczne wykonywać transakcyjnie w Blockbenchu. Generator historyczny nie może nadpisać dopracowanego projektu.
- Jeden proces steruje Blockbenchem. Kontrola nie zapisuje korekt do modelu ani historycznych wyników.
- Brak Git w katalogu serwera: wersjonowane snapshoty i SHA256 zamiast fikcyjnych commitów. W repozytorium źródeł stosować jego workflow.
- Feeling nieumarłego wynika przede wszystkim z sylwetki, rytmu, asymetrii i reakcji ciała. Nie opóźniać sterowania, obrażeń ani kamery dla efektu artystycznego.
- Kierunek roboczy: świadomy, wyniszczony nieumarły zdolny do sprawnej walki. Szczegóły stylistyczne poniżej są propozycją do oceny na animowanym porównaniu, nie wcześniej zaakceptowanym wyglądem.

## Co wykazał audyt testów

18.09.2026 uruchomiono obecny `tools/blockbench/validate_undead.py` na oryginałach i zmienionych kopiach w katalogu tymczasowym. Modeli źródłowych nie zmieniono.

| Przypadek | Obecny wynik | Wniosek |
|---|---|---|
| V4, 9 animacji | exit 0 / PASS | Działa w swoim wąskim zakresie |
| V4 bez hurt i death | exit 0 / PASS | Nie kontroluje wymaganego kompletu klipów |
| jump_start: root y=-4 przy t=0,1 s | exit 0 / PASS | Nie kontroluje podparcia skoku |
| Pivot right_shin przesunięty w y o +3 | exit 0 / PASS, te same błędy stóp | Obliczenia ignorują rzeczywisty rig |
| Nieistniejąca tekstura 999 | exit 1 | Ten przypadek wykrywa w zwykłym trybie |
| Ten sam błąd przy python -O | exit 0 / PASS | assert wyłącza bramki walidacji |
| B, legalne odwołanie do tekstury 1 | exit 1: cervical_spine | Fałszywy alarm dla drugiego atlasu; nie dowodzi niezgodności z ModelEngine |

Pozostawić użyteczne kontrole hierarchii, zakresów, skończonych wartości i pętli, ale poprawić ich kontrakt. Obecne pomiary walk 0,0067 px i attack 0,0291 px dotyczą uproszczonego modelu obliczeniowego. Nie dowodzą poprawności całej geometrii ani zachowania w grze.

Dalsze wady: UV i rozdzielczość na sztywno 256; sprawdzana tylko pierwsza tekstura; liniowa interpolacja bez rozpoznania typu; długości nóg i czasy klipów wpisane ręcznie; PASS drukowany przed kontrolą attack. Siatka 20 Hz i wysokość 28,35 są konwencjami tego assetu, nie uniwersalnym dowodem zgodności formatu. Równe końce pętli nie dowodzą płynności prędkości.

`check_death_pose.py` miesza pomiar aktywnej zakładki z plikiem V4, sprawdza jedynie minimum bryły w 25 chwilach i zapisuje death-ground.json przed asercją. `render_undead_motion.py` także nie wiąże aktywnego projektu z plikiem i nadpisuje stały katalog wyników. Tych skryptów nie traktować obecnie jako niezależnych bramek wydania.

## Etap 1 — walidator, który wykrywa własne ograniczenia

**Pliki:** zmiana `tools/blockbench/validate_undead.py`; nowe `tools/blockbench/tests/test_validate_undead.py`, `tools/blockbench/tests/fixtures/`; aktualizacja `tools/blockbench/README.md`.

**Kontrakt:** CLI przyjmuje ścieżkę modelu; exit 0 oznacza wyłącznie zaliczone kontrole pliku, exit 1 błąd modelu, exit 2 nieobsługiwaną cechę lub błąd narzędzia. Raport wskazuje hash, zakres i pominięte kontrole. Żadnego ogólnego PASS przed końcem.

- [ ] Zapisać testy regresyjne powyższych przypadków jako kopie fixture, bez mutowania V4/B. Dla kontroli strukturalnej B musi przejść obsługę tekstur; niezależne błędy ruchu nadal mogą blokować pełny odbiór.
- [ ] Zastąpić produkcyjne assert jawnym zbieraniem błędów i niezerowym kodem wyjścia; identyczna decyzja zwykłego Pythona i `-O`.
- [ ] Sprawdzać wszystkie referencje i dekodować każdy użyty atlas; granice UV brać z zadeklarowanej przestrzeni UV właściwej tekstury/modelu, a nie zawsze z fizycznego rozmiaru PNG.
- [ ] Oddzielić poprawność formatu od polityki tego modelu: wymagane dziewięć istniejących klipów, właściwości hitboxu i dozwolone uproszczenia. Rozszerzać listę stanów wraz z zaakceptowaną macierzą.
- [ ] Dodać negatywne fixture: brak kości, duplikat identyfikatora/nazwy animacji, nieistniejąca referencja, NaN, uszkodzona druga tekstura, klatka poza czasem, przerwana pętla. Dodać poprawny drugi atlas i poprawną animację o innej długości.
- [ ] Nie udawać obsługi interpolacji/wyrażeń: obsłużony wariant obliczać zgodnie z formatem; nieobsługiwany zgłaszać jako niezweryfikowany, bez PASS pomiaru ruchu.
- [ ] Uruchomić `python3 -m unittest discover -s tools/blockbench/tests -v`; następnie validator na V4 i B, sprawdzając pełne raporty i kody wyjścia.

Przykład rzeczywistego kontraktu testu CLI, do umieszczenia w unittest po przygotowaniu fixture:

```python
def test_invalid_texture_fails_even_with_optimization(self):
    for flags in ([], ['-O']):
        result = subprocess.run(
            [sys.executable, *flags, 'tools/blockbench/validate_undead.py',
             'tools/blockbench/tests/fixtures/invalid-texture.bbmodel'],
            capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)
        self.assertNotIn('PASS', result.stdout)
```

**Odbiór:** udokumentowane fałszywe alarmy znikają; błędne fixture nie przechodzą; testy nie deklarują weryfikacji runtime.

## Etap 2 — wiarygodny pomiar i porządek wersji

**Pliki:** `render_undead.py`, `render_undead_motion.py`, `check_death_pose.py`, `organize_projects.py` w `tools/blockbench/`; nowe `tools/blockbench/project_identity.py`, `tools/blockbench/tests/test_project_identity.py`; wyniki pod `output/undead-player-v5/reviews/<revision>/`.

- [ ] Wprowadzić jawne argumenty źródła, katalogu wyników i listy klipów. Manifest zapisuje SHA256 pliku, semantyczną tożsamość eksportu otwartego projektu, wersję narzędzia, kamery, czasy i rozdzielczość. Normalizacja ignoruje tylko udokumentowane metadane edytora; nie geometrię, UV, tekstury ani keyframes.
- [ ] Najpierw test: wybrany projekt A przy żądaniu B kończy pracę błędem przed renderem. Różnica wyłącznie w metadanych eksportu nie blokuje, zmiana jednej współrzędnej blokuje. Sprawdzić te przypadki na zapisanych eksportach.
- [ ] Ponownie używać jednoznacznie zidentyfikowanego projektu; nie otwierać nowej zakładki przy każdym renderze. Niejednoznaczność zgłaszać. Snapshot przed edycją; żadnego automatycznego zamykania niezapisanych prac.
- [ ] Oddzielić generowanie korekt death od kontroli death. Kontrola zapisuje wyłącznie nowy raport; nie zmienia death-ground.json ani kluczy. Przywraca w finally zaznaczenie, tryb i czas edytora, o ile projekt nadal istnieje.
- [ ] Mierzyć przetransformowane wierzchołki faktycznego modelu, z pivotami i transformacjami całego łańcucha. Osobno podeszwy, geometrię ciała i jawnie wyłączony hitbox. Uwzględnić rotacje spoczynkowe, skalę i interpolację.
- [ ] Zweryfikować pomiar niezależnymi małymi fixture: sześcian o znanym przesunięciu, obrót wokół przesuniętego pivotu, dwie kości i skala. Potem przesunięcie right_shin o +3 musi zmienić wynik, a root=-4 ujawnić penetrację.
- [ ] Próbkować klatki kluczowe i odcinki pomiędzy nimi, np. co 1/120 s; raportować krok próbkowania, bez deklaracji dowodu ciągłego. Obejmować pełne długości wszystkich klipów.
- [ ] Sprawdzić dwa kolejne uruchomienia: brak nowych zbędnych zakładek, brak nadpisania poprzedniego raportu, zgodne hashe, poprawne odtworzenie stanu edytora po błędzie MCP.

**Odbiór:** można odtworzyć pomiar dokładnie wskazanej rewizji. Globalne minimum death oznacza tylko brak wykrytej penetracji; podparcie i wiarygodność upadku mają osobny odbiór wizualny.

## Etap 3 — kontrakt gry i naprawa cyklu życia

**Pliki:** `output/undead-player-v5/animation-matrix.md`, `integration-audit.md`; źródła w `/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/`: `src/main/java/pl/mcmmorpg/charactercreator/model/ModelEngineBridge.java`, `creator/ConfirmService.java`, `profile/CharacterProfileModule.java`, `CharacterCreatorPlugin.java` pod tym samym prefiksem pakietu.

- [ ] Odświeżyć audyt: hash źródeł i JAR, instrukcje repozytorium, używany build oraz dostępny harness testów. Aktualna zgodność dwóch JAR nie dowodzi ich zgodności ze źródłami. Zmiany Java planować w repozytorium źródeł, nie w plikach serwera.
- [ ] Dla każdego stanu ustalić zdarzenie wejścia/wyjścia, priorytet, kości, loop/hold, blend i właściciela sterowania: automat ModelEngine albo CharacterCreator. Nie dodawać drugiego wyzwalacza bez testu automatyki.
- [ ] Ustalić dostępność łuku, tarczy, jedzenia, jazdy, elytry i ruchu bocznego. Każdy dostępny stan otrzymuje klip, kontrolowaną pozę/fallback lub potwierdzone zachowanie silnika; brak osobnego klipu nie jest automatycznie błędem.
- [ ] Przyjąć spójny kontrakt brakującego modelu: dla rasy wymagającej modelu nie zatwierdzać zmiany rasy/profilu/uprawnień, jeśli modelu nie da się przygotować. Dla rasy jawnie bez modelu usunąć wyłącznie model rasowy i przywrócić bazową widoczność. Obsłużyć także awarię po prechecku i odtworzyć poprzedni stan.
- [ ] Przed poprawką odtworzyć w harnessie/staging: brak blueprintu, błąd tworzenia modelu, dwukrotne confirm/select, zmiana na rasę bez modelu, współistniejący obcy model. Sprawdzać łącznie model, profil, uprawnienia i widoczność.
- [ ] Ograniczyć usuwanie do modelu należącego do systemu ras. Ponowne nałożenie tej samej rewizji ma być idempotentne; inna rewizja tego samego ID wymaga świadomego odświeżenia.
- [ ] Obsłużyć unload/usunięcie profilu, reconnect, death/respawn i wyłączenie pluginu. Zabezpieczyć opóźnione callbacki przed przypięciem modelu do już innego profilu.
- [ ] Po zmianach powtórzyć te same przypadki; sprawdzić, że spawn nie uruchamia się podwójnie i że obcy model pozostaje. Zbudować JAR przez rzeczywisty workflow repozytorium i zapisać powiązanie źródła→build→test.

**Odbiór:** żadnego mieszanego stanu „nowa rasa, stary model”, żadnego nieuzasadnionego remove-all. Szczegółową implementację Java poprzedza odczyt aktualnego kodu; nie zakładać dziś niezweryfikowanych sygnatur API ani istniejącego frameworka testów.

## Etap 4 — wygląd, osobne iteracje

**Pliki:** wersjonowane projekty pod `output/undead-player-v5/`, `face-review/`, `reviews/`; opis wybranej rewizji w README.

- [x] Zamknąć wybór twarzy na istniejącym porównaniu A/B: użytkownik wybrał B. Nie wracać do całego V2.
- [ ] W osobnej iteracji nadać żebrom objętość od mostka ku bokom; ograniczyć płaską ciemną tablicę pod nimi.
- [ ] W kolejnej iteracji rozdzielić postawę miednicy, klatki i szyi; sprawdzić wpływ na wszystkie dotychczasowe klipy.
- [ ] Dopracować bok czaszki, kości i nieregularne zużycie stroju. Zachować paletę, asymetrię i znak pleców; nie zwiększać atlasu bez wykazanego problemu.
- [ ] Każdą zmianę porównać w identycznych kamerach: przód/tył/bok/3⁄4, zbliżenie i postać 100–150 px. Sprawdzić skrajne położenia głowy i obu ramion z wyposażeniem.
- [ ] Ewentualne scalenie atlasów B zrobić dopiero po akceptacji wyglądu; porównać piksele renderów i UV przed/po. Nie traktować tego jako pilnej optymalizacji.

**Odbiór:** jawna ocena twarzy, żeber, sylwetki i materiałów; brak regresji schowanej pod jednoczesnymi zmianami. Test liczbowy nie zastępuje ART_PASS.

## Etap 5 — ruch, przejścia i wyposażenie

**Pliki:** wybrany projekt V5, `animation-matrix.md`, raporty i nagrania `reviews/<revision>/`.

- [ ] Naprawić jump_start i jump_end przez skoordynowanie ugięcia nóg z root. Dla płaskiego podłoża przyjąć roboczą tolerancję penetracji 0,04 px; odróżnić stopę podpierającą od uniesionej. Zmierzyć pełną geometrię, nie wzór ze stałymi długościami.
- [ ] Przebudować death: utrata podparcia, ugięcie kolan/miednicy, opóźniony ruch tułowia i ramion, osiadanie. Ponownie wyznaczyć kontakt dla zaakceptowanej głowy, bez kopiowania korekt V4.
- [ ] Dopracować walk przez przenoszenie ciężaru i subtelną asymetrię; zachować dobre podparcie. Tempo sprawdzić względem rzeczywistej prędkości, także podczas ruchu bocznego i wstecz.
- [ ] Uzupełnić run, crouch w miejscu/ruchu, swim oraz zachowania sprzętu ustalone w etapie 3. Hurt zachować krótki; attack rozstrzygnąć jako nakładkę górnej części ciała lub pełny ruch z jawną polityką chodzenia.
- [ ] Nagrać pełne sekwencje: idle↔walk↔run; idle/walk→skok→lądowanie; attack+walk; hurt+walk; hurt+attack; dwa hurt; death przerywający każdy z nich; spawn→idle. Obserwować przeskoki, brak powrotu i clipping.
- [ ] Zweryfikować obie dłonie, zmianę slotu, tarczę i łuk, czas wymachu względem rzeczywistego trafienia. Nie zmieniać mechaniki obrażeń dla dopasowania animacji.

**Odbiór:** poprawione znane zanurzenia, wiarygodny upadek, brak wykrytych regresji w przejściach. Izolowany GIF każdego klipu jest materiałem pomocniczym, nie końcowym testem.

## Etap 6 — paczka, klient i wydajność

**Pliki:** `output/undead-player-v5/verification.md`, README, manifest finalnej rewizji; blueprint i paczka wyłącznie w kontrolowanym środowisku testowym przed wdrożeniem.

- [ ] Zaimportować dokładnie zaakceptowaną rewizję, także jej wszystkie tekstury. Sprawdzić wygenerowane części ModelEngine i finalny ZIP Nexo, błędy importu, ścieżki oraz treść atlasów.
- [ ] Sprawdzić na kliencie otrzymanie aktualnej paczki; nagrać FPP, F5 przód/tył i widok drugiego gracza. Zweryfikować przedmioty, kamerę, widoczność bazowego gracza i skrajne kąty głowy.
- [ ] Odtworzyć macierz stanów na płaskim terenie, schodach, krawędziach i w wodzie; anulowane obrażenia nie powinny udawać trafienia. Sprawdzić death hold, respawn, zmianę profilu, reconnect i obserwatora dołączającego później.
- [ ] Porównać tę samą scenę bez modeli i z 1, 10 oraz 30 modelami: MSPT/TPS, liczba encji, FPS/frame time klienta, stabilność po kolejnych przełączeniach. Zapisać sprzęt, dystans renderowania i wyposażenie; sama liczba kostek nie rozstrzyga kosztu.
- [ ] Przed pomiarem ustalić budżet docelowej liczby widocznych graczy i sprzętu. Bez niego raportować pomiary, nie wystawiać „MMO-ready”.
- [ ] Wystawić osobno ART_PASS, INTEGRATION_PASS i RUNTIME_PASS, każdy z hashem i dowodem. Nieprzetestowane przypadki oznaczać jawnie. Przygotować poprzedni blueprint/paczkę i procedurę rollback przed wdrożeniem world1.

**Odbiór:** brak otwartych P1; znane P2 poprawione lub jawnie zaakceptowane jako ograniczenia, nigdy przemilczane. Wdrożenie nie wynika automatycznie z zielonego walidatora.

## Pokrycie świeżego review

| Grupa problemów | Etap |
|---|---|
| Brak blueprintu, ignorowany wynik, remove-all, podwójne nakładanie, cleanup | 3 |
| Brak stanów/wyzwalaczy i polityki wyposażenia | 3, 5, 6 |
| Fałszywe PASS/FAIL, stałe długości, brak kontroli skoku | 1, 2, 5 |
| Niewłaściwa zakładka, nadpisywanie dowodów, korekty death | 2 |
| Twarz, żebra, postawa, materiały, drugi atlas | 4 |
| Sztywna śmierć, chód, blending, clipping | 4, 5 |
| Kamera, paczka klienta, lifecycle runtime, wydajność | 6 |
| Nieaktualne dokumenty i powiązanie źródeł/JAR | 2, 3, 6 |

Plan nie przyznaje modelowi żadnego nowego PASS. W tej sesji wykonano audyt i eksperymenty na kopiach tymczasowych; nie poprawiano assetu, kodu walidatora ani integracji.

## Rozszerzenie — jakość produktu i scenariusze pomijane przez dotychczasowe testy

Poniższe punkty rozszerzają odbiór. Są wymaganiami i hipotezami do sprawdzenia, a nie listą dodatkowych potwierdzonych usterek modelu.

### A. Hierarchia odbioru wizualnego

- [ ] Przed detalowaniem zapisać kartę kierunku: ponury, wyniszczony nieumarły; zamknięta/spokojna żuchwa; oczy jako pierwszy punkt uwagi; asymetria podporządkowana anatomii; zachowana minecraftowa bryła. B traktować jako kandydata twarzy, nie finalny projekt.
- [ ] Oceniać kolejno: sylwetka → proporcje → twarz → anatomia → materiały → drobne detale. Odrzucić etap, jeśli wymaga przykrycia problemu wcześniejszego etapu większą liczbą szczegółów.
- [ ] Dla każdej rewizji zrobić te same obrazy postaci o wysokości około 64, 128 i 256 px. Przy 64 px oceniać rozpoznawalność sylwetki, przy 128 px twarz/żebra/materiały, przy 256 px wykonanie. To obrazy porównawcze, nie deklaracja konkretnej odległości w grze.
- [ ] Obejrzeć w neutralnym świetle edytora, następnie w grze w dzień, cieniu i słabym oświetleniu. Sprawdzić, czy kości nie zlewają się ze strojem, a turkus nie zasłania oczu. Nie deklarować emisji światła na podstawie samego jasnego koloru tekstury.
- [ ] Sprawdzić szwy atlasu, gęstość tekseli, rozciąganie na cienkich powierzchniach, niewidoczne powierzchnie i nakładające się płaszczyzny powodujące migotanie. Celowo współdzielone UV dopuszczać; automatyczne odrzucanie każdego overlapu byłoby błędem testu.

### B. Ruch — czego sam kontakt z ziemią nie dowodzi

- [ ] W fazie podparcia mierzyć także przesunięcie stopy w poziomie względem świata. Sam stały poziom Y nie wykrywa ślizgania. W Blockbenchu odróżnić animację w miejscu od ruchu encji; końcowy wynik sprawdzić przy rzeczywistych prędkościach gracza.
- [ ] Sprawdzić obie nogi na całym cyklu, momenty zmiany podparcia i odwrócenie kierunku ruchu. Nie zakładać, że faza podparcia zawsze trwa pierwszą połowę klipu.
- [ ] Sprawdzić ciągłość pozycji i widocznych prędkości przy zapętleniu; dopuszczać zamierzone uderzenia/akcenty. Nie narzucać identycznej gładkości każdej animacji.
- [ ] Zbudować atlas póz z identycznymi kamerami: neutralna, skrajne spojrzenia, pełny zamach obu rąk, crouch, swim, naciągnięty łuk, tarcza, końcowa poza death. Oglądać szyję, pachy, kolana, dłonie i poły.
- [ ] Przy death ustalić zamierzony kontakt ciała z ziemią w kolejnych fazach i w pozie końcowej. Kontakt jednego skrawka płaszcza nie wystarcza, jeśli reszta ciała przypadkowo wisi. Nie wymagać, aby wszystkie części ciała jednocześnie dotykały gruntu.
- [ ] Określić końce reakcji: hurt nie może zostawić skręconego tułowia; przerwany atak nie może zostawić uniesionej ręki; wyjście z crouch nie może zachować obniżonego root; respawn nie może dziedziczyć death hold.

### C. Scenariusze integracyjne o wysokiej wartości

| Scenariusz | Oczekiwany dowód |
|---|---|
| Atak → obrażenia → śmierć w krótkim odstępie | Death przejmuje sterowanie; poprzednie callbacki nie przywracają ataku/idle |
| Śmierć → szybki respawn | Nowa żywa postać bez starej pozy i bez podwójnego modelu |
| Profil A → B → A przed zakończeniem zadań | Finalny model i dane należą do A; zdarzenia B nie zmieniają A |
| Zmiana świata/teleport/chunk unload i ponowny tracking | Obserwator widzi jeden aktualny model; brak pozostawionych części |
| Obserwator oddala się i wraca lub dołącza podczas crouch/death | Otrzymuje bieżący stan, nie wyłącznie domyślne idle |
| Trzymanie łuku → zmiana slotu/upuszczenie przedmiotu | Kończy się poza użycia; ręce odpowiadają aktualnemu wyposażeniu |
| Brak lub odrzucenie paczki klienta | Jawne zachowanie zgodne z polityką serwera; brak cichej, nieocenionej niewidzialności |
| Zmiana rasy przy błędzie tworzenia modelu | Spójny rollback wyglądu, profilu, klasy i uprawnień objętych transakcją |
| Wyłączenie integracji z aktywnymi modelami testowymi | Zadania wygasają; własne zasoby są posprzątane; obce modele pozostają |
| Kolejne cykle nałóż→zdejmij po rozgrzaniu | Liczba encji i zadań wraca do poziomu bazowego; brak narastającego wycieku |

- [ ] Dla każdego przypadku zapisać identyfikator rewizji, stan początkowy, sekwencję zdarzeń, oczekiwany wynik, wynik rzeczywisty i nagranie/log. Test z mockami potwierdza logikę modułu; nie zastępuje sprawdzenia ModelEngine na serwerze.
- [ ] Sprawdzić mechanikę trafień i kamerę niezależnie od bryły renderowanej: asset nie może przypadkowo zmieniać zasięgu, kolizji ani dawać innego wyniku trafienia niż przyjęta mechanika gracza. Rozmiar modelu nie jest dowodem rozmiaru hitboxu runtime.
- [ ] Odczytać zasady zbroi, niewidzialności, glow, ręki dominującej i ewentualnych kosmetyków. Testować współistnienie tylko funkcji faktycznie dostępnych; niedostępność udokumentować zamiast tworzyć niepotrzebne klipy.

### D. Testowanie samych narzędzi

- [ ] Rozdzielić testy regresji konkretnego assetu od testów ogólnych formatu. Poprawny model z dwoma atlasami, inną długością klipu lub poprawną skalą nie może polec z powodu założeń jednej wersji nieumarłego.
- [ ] Dodać test błędu MCP, pustej odpowiedzi, brakującej animacji, obciętego eksportu i przerwania renderowania. Żaden z nich nie może pozostawić raportu oznaczonego jako kompletny.
- [ ] Sprawdzić invariants: zmiana samej nazwy projektu nie zmienia pomiaru; przesunięcie całej geometrii wraz z referencją podłoża nie zmienia błędu kontaktu; obrót kamery nie zmienia pomiaru fizycznej pozy; usunięcie renderu nie oznacza automatycznie negatywnego wyniku assetu.
- [ ] Kontrolować, czy obrazy rzeczywiście przedstawiają różne klatki. Identyczne screenshoty całej ruchomej animacji zgłaszać do kontroli, ale statycznego hold nie odrzucać automatycznie.
- [ ] Nie używać jednego dużego testu „wszystko PASS”. Raport rozdziela strukturę, tekstury, rig, klipy, kontakt, przejścia, paczkę i runtime. Zaliczenie jednej sekcji nie maskuje braku innej.
- [ ] Tam, gdzie test korzysta z tego samego wzoru co generator, dodać niezależny pomiar lub ręcznie policzony prosty fixture. Dwa identyczne algorytmy mogą potwierdzić ten sam błąd.

### E. Pakiet przekazania i kryterium zakończenia

Każda oceniana rewizja dostarcza: edytowalny projekt, tekstury, krótki changelog, manifest hashy, zestaw stałych widoków, atlas póz, nagrania sekwencji i raport z otwartymi problemami. Manifest wskazuje oddzielnie wersję roboczą, kandydata do odbioru i wersję wdrożoną; nazwa zakładki nie pełni tej roli.

Kolejność nakładu: najpierw blokujące spójność rasy i wiarygodność weryfikacji; następnie duże problemy wizualne i ruchowe; później materiały i porządkowanie atlasu. Nie obiecywać czasu całości przed uruchomieniem sprawdzalnego pipeline i zamknięciem zakresu dostępnych stanów gry.

Odbiór końcowy wymaga przejścia macierzy obowiązkowych przypadków, zaakceptowanej grafiki, udokumentowanego budżetu wydajności i odtwarzalnego rollbacku. Niewykonany test pozostaje niewykonany — nie „prawdopodobnie zaliczony”.

## Etap artystyczny F — feeling świadomego nieumarłego

Ten etap rozszerza etapy 4–5, a jego odbiór w grze należy do etapu 6. Przygotować go po naprawie narzędzi i podstawowego rigu; nie dopisywać charakteru do ruchu z potwierdzonymi błędami kontaktu. Nie usuwa żadnego wcześniejszego zadania naprawczego.

**Pliki:** nowe `output/undead-player-v5/undead-feeling.md`; wersjonowane projekty i porównania w `output/undead-player-v5/reviews/feeling/`; aktualizacje `animation-matrix.md`, `verification.md` i README. Projekt roboczy wskazywać manifestem; nie nadpisywać kandydata B ani V4.

### F1. Ustalenie języka ruchu

Rozpatrzone kierunki:

| Kierunek | Zaleta | Koszt/ryzyko |
|---|---|---|
| Świadomy, wyniszczony nieumarły — rekomendowany | Pasuje do grywalnej rasy i wielu klas; łączy sprawność z obcą motoryką | Potrzebuje subtelnej pracy timingiem, inaczej wygląda jak człowiek w skórce |
| Ciężki, rozpadający się trup | Bardzo czytelny charakter | Silne kulenie i bezwład mogą sugerować ociężałe sterowanie oraz wyglądać komicznie w biegu |
| Drapieżny ghul | Wyraźna energia, dobry kierunek dla agresywnego moba | Silniej narzuca klasę i charakter; trudniej połączyć z łukiem, tarczą i spokojnym idle |

Plan przyjmuje pierwszy kierunek. Dwa pozostałe stanowią porównanie projektowe, nie zlecenie trzech pełnych modeli.

- [ ] Zapisać w `undead-feeling.md` cztery zasady: oszczędność ruchu, niesymetryczna sztywność, krótka bezwładność segmentów, nagłe odzyskiwanie kontroli. Przykłady poniżej wiążą każdą zasadę z konkretnym klipem.
- [ ] Zdefiniować cechy stałe: lekko zapadnięta klatka, nierówne barki, jeden bardziej sztywny łokieć i oszczędny ruch szyi. Zachować zakresy pozwalające poprawnie używać obu dłoni i patrzeć zgodnie z kierunkiem gracza.
- [ ] Ustalić, że zwykłe idle nie ma wyraźnego rytmicznego oddychania żywej postaci. Dopuszczalna jest subtelna zmiana napięcia/postawy, bez automatycznego kołysania całego ciała.

**Odbiór:** feeling można opisać przez widoczne cechy ruchu, a nie samą nazwę rasy, świecące oczy lub dźwięk.

### F2. Krótkie porównanie charakteru przed przerabianiem wszystkich klipów

- [ ] Na identycznej geometrii i teksturze przygotować trzy krótkie próbki: obecny ruch jako kontrola, subtelny feeling, mocniejszy feeling. Zmieniać wyłącznie ruch; nie łączyć porównania z nową twarzą lub strojem.
- [ ] Każda próbka pokazuje tę samą sekwencję: 4 s idle → dwa cykle walk → zatrzymanie → attack → hurt → powrót do idle. Uruchomienie attack i hurt oddzielić tak, by było widać charakter obu reakcji.
- [ ] Dla wersji subtelnej zacząć od przesunięcia akcentu barku/ramienia o około 0,05 s; dla mocniejszej około 0,10 s. To parametry prototypu, nie globalne opóźnienie każdej kości. Dłonie z przedmiotami zachowują zgodność akcji z trafieniem/użyciem.
- [ ] Nagrać bok, trzy czwarte i tył F5 przy tej samej prędkości i kamerze; dodatkowo pokazać sylwetkę bez tekstury, aby ocenić sam język ruchu.
- [ ] Wybrać natężenie na podstawie porównania przed przeniesieniem go na cały zestaw animacji. Zapisać co zachowano, co odrzucono i dlaczego. Nie wybierać mocniejszego wariantu tylko dlatego, że zmiana jest bardziej widoczna.

**Odbiór:** postać wygląda na nieumarłą, nie na przypadkowo uszkodzony rig. Ruch pozostaje przyjemny podczas dłuższego patrzenia od tyłu.

### F3. Przeniesienie feelingu na zachowania

| Stan | Zaplanowany charakter | Granica, której nie przekraczać |
|---|---|---|
| Idle | Długie spokojne utrzymanie pozy; oszczędna korekta barku lub głowy; lekko różne napięcie rąk | Brak ciągłego drżenia, kiwania i ostentacyjnego oddechu; brak samowolnego odwracania głowy od celu gracza |
| Walk | Nierówny zakres pracy ramion, sztywniejszy jeden łokieć, drobne opóźnienie klatki względem bioder | Kontakt stóp i prędkość nadal prawidłowe; asymetria nie oznacza automatycznie kulenia |
| Run | Bardziej zebrana sylwetka i zdecydowany napęd nóg, sztywność barków pozostaje rozpoznawalna | Bez chwiejnego sprintu, ciągnięcia stopy i utraty kontroli nad bronią |
| Start/stop/zwrot | Krótkie osiadanie tułowia przy zatrzymaniu; głowa wskazuje kierunek, klatka domyka ruch | Żadnego opóźnienia kamery lub kierunku trafienia; efekt skrętu dodać tylko przy poprawnym sterowaniu runtime |
| Crouch | Złożenie bioder i kolan, klatka lekko schowana, nierówne barki zachowane | Bez zapadania w ziemię, blokowania widoku i kolizji żeber z udami |
| Skok/lądowanie | Zwarta poza w locie; po lądowaniu krótko opóźnione osiadanie barków/głowy | Nogi amortyzują prawidłowo; efekt nie opóźnia kolejnego ruchu gracza |
| Attack | Krótkie zebranie, zdecydowany wymach, lekko sztywny powrót ręki i barku | Animacja nie przesuwa faktycznego momentu obrażeń ani nie obraca dłoni niezgodnie z bronią |
| Hurt | Głównie mechaniczna utrata ustawienia barku/tułowia, potem krótki powrót przypominający nastawienie stawu | Reakcja nadal czytelna; brak długiego oszołomienia lub blokady sterowania |
| Death | Stopniowa utrata napięcia i podparcia; nogi puszczają, tułów opada z bezwładem | Bez obrotu całej sztywnej figury; po śmierci postać nie wraca samoczynnie do życia |
| Spawn | Krótkie odzyskanie napięcia: barki, szyja, ustalenie spojrzenia, gotowość | Nie udawać wstawania z ziemi, jeśli początkowa poza jest stojąca; nie powtarzać przy każdym przeładowaniu widoczności |
| Łuk/tarcza | Charakter głównie w barkach i klatce; dłonie stabilnie wykonują funkcję | Priorytet czytelnego celowania, naciągu i bloku |
| Swim | Oszczędny, nieco sztywny ruch z zachowaną orientacją postaci | Bez nieczytelnego szamotania i niekontrolowanego obracania ciała |

- [ ] Przenosić wybrane cechy po jednym klipie, po każdym uruchamiając kontrolę kontaktu, skrajnych póz i właściwych przejść.
- [ ] Sprawdzić idle oraz walk przez co najmniej 60 s, ponieważ powtarzana krótka sztuczka może stać się irytującym tikiem. Nie dodawać rzadkich losowych gestów przed sprawdzeniem bazowego idle i sposobu ich przerywania.
- [ ] Jeśli runtime nie pozwala bezpiecznie oddzielić patrzenia, skrętu i przedmiotów, zachować poprawne sterowanie i ograniczyć feeling do obsługiwanych kości. Ograniczenie zapisać, bez ukrytego zastępowania sterowania animacją.

### F4. Dźwięk i efekty — opcjonalna warstwa po odbiorze ruchu

Bazowy zakres feelingu obejmuje geometrię i animację. Dźwięki/VFX są osobnym, opcjonalnym rozszerzeniem; ich brak nie blokuje oceny ruchu.

- [ ] Po odbiorze ruchu ocenić krótkie suche odgłosy stawu/tkaniny przy spawn, mocnym hurt lub death. Unikać chrupania przy każdym kroku i ciągłego stękania.
- [ ] Przed dodaniem audio ustalić osobno głośność właściciela i obserwatora, zasięg, częstotliwość oraz zachowanie kilku postaci naraz. Używać wyłącznie zasobów z ustalonym prawem użycia.
- [ ] Efekty oczu lub krótką emisję cząstek rozważyć dopiero po ocenie czytelności. Nie dodawać stałej chmury zakrywającej postać lub walkę. Światło, emisja tekstury i kolor oka to odrębne możliwości techniczne wymagające sprawdzenia w stacku.

### F5. Odbiór feelingu

- [ ] W porównaniu bez podpisów opisać różnice obecnej i nowej wersji: sztywność, ciężar, asymetria, sposób odzyskiwania kontroli. Jeśli różnicę daje wyłącznie twarz/tekstura, praca nad motoryką nie osiągnęła celu.
- [ ] Ocenę zapisać osobno dla: charakteru nieumarłego, czytelności akcji, responsywności, komfortu F5 i powtarzalności. Żaden wzrost charakteru nie usprawiedliwia pogorszenia czytelności trafień czy sterowania.
- [ ] Potwierdzić feeling z bronią i bez niej, w ruchu i bezruchu. Pozy dłoni nie mogą wyglądać poprawnie wyłącznie przy pustych rękach.
- [ ] W FPP sprawdzić to, co rzeczywiście widzi właściciel: ręce, przedmioty i ewentualne audio. Nie obiecywać, że animacja trzecioosobowego modelu automatycznie zmieni ręce pierwszoosobowe. Jeśli FPP pozostaje vanilla, opisać to jawnie jako granicę zakresu.
- [ ] Do ART_PASS dołączyć zaakceptowane porównanie feelingu; do RUNTIME_PASS nagranie tej samej rewizji w grze. Sam opis pożądanego charakteru nie jest dowodem wykonania.

## Ostateczna kolejność realizacji po dodaniu feelingu

1. Etapy 1–2: wiarygodne kontrole, pomiar rzeczywistego rigu i identyfikacja wersji.
2. Etap 3: kontrakt stanów, spójność rasy i cyklu życia.
3. Etap 4 + F1: wybór twarzy, poprawa sylwetki/anatomii i zasady charakteru.
4. Etap 5: naprawa podparcia, bazowego upadku i przejść.
5. F2: porównanie natężenia feelingu na krótkiej sekwencji.
6. F3 + pozostała część etapu 5: pełny zestaw zachowań z zaakceptowanym charakterem, sprzętem i przejściami.
7. Etap 6 + F5: odbiór paczki, klienta, wydajności oraz feelingu. F4 tylko jako świadomie wybrane rozszerzenie.

Plan przygotowuje pracę; samo dopisanie feelingu nie zmienia modelu ani serwera.
