# Nieumarły — plan dopracowania modelu gracza

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Pracować w tej sesji, kolejno. Kroki mają checkboxy; ten dokument nie upoważnia do automatycznego wdrażania nieocenionych wariantów.

**Goal:** Uzyskać spójną wizualnie postać nieumarłego z czytelną twarzą, kompletem podstawowych zachowań gracza i potwierdzonym działaniem na world1.

**Architecture:** Najpierw ustalić wygląd w Blockbenchu, następnie dopracować ruch na zaakceptowanej geometrii, a dopiero potem podłączyć zdarzenia gry. Edytowalny projekt jest źródłem prawdy; generator nie nadpisuje ręcznie dopracowanego projektu. Render, walidacja pliku i test Minecraft stanowią trzy oddzielne poziomy kontroli.

**Tech Stack:** Blockbench desktop + lokalny MCP, projekt Generic .bbmodel, Python/Pillow do kontroli i technicznych zestawień renderów, ModelEngine 4.0.9, CharacterCreator, Nexo, Paper 1.21.11.

**Spec:** Wymagania stanowią ostatni przegląd użytkownika i sekcja „Ustalenia projektowe” poniżej. Poprzednie stany: `output/undead-player-v2/README.md`, `output/undead-player-v3/README.md`, `output/undead-player-v4/README.md`.

## Global Constraints

- Docelowy identyfikator: `race_nieumarly`.
- Środowisko: `/opt/docker/mc/data/world1`; wdrożenie dotyczy wyłącznie world1.
- Wersje V2, V3 i V4 zachować. Nie nadpisywać ich projektów ani renderów.
- Obecny wygląd V4 jest identyczny z V3; V4 zawiera dziewięć animacji.
- Zachować paletę granatu, przygaszonej kości, bordo i turkusu oraz asymetrię rąk.
- Decyzja o przywróceniu twarzy V2 nie została jeszcze podjęta przez użytkownika. V2 jest punktem odniesienia i rekomendowanym pierwszym wariantem do oceny.
- Nie zmieniać twarzy, proporcji i tekstur jednocześnie w jednej iteracji.
- Nie modyfikować postaci istniejących graczy do testów.
- Nie uruchamiać dwóch operacji sterujących Blockbenchem jednocześnie; pomiary i nagrania korzystają z tego samego projektu i osi czasu.
- Nie przedstawiać liczby brył, rozdzielczości atlasu ani poprawnego importu jako dowodu jakości artystycznej.
- Nie traktować obecności animacji w pliku jako dowodu jej wyzwalania w grze.
- Repozytorium Git nie zostało wykryte w katalogu serwera. Zamiast zakładać możliwość commitów, zachowywać wersjonowane artefakty i sumy SHA256.

## Ustalenia projektowe

Postać ma być ponura, wyniszczona i czytelna w stylistyce Minecraft. Twarz nie powinna wyglądać na smutną, zdziwioną ani uśmiechniętą. Czaszka i oczy są głównym punktem uwagi; strój ma wspierać ich czytelność. Żebra mają tworzyć rozpoznawalną klatkę piersiową, nie znak na płaskiej kamizelce. Plecy muszą być dopracowane, ponieważ są istotne w widoku F5.

Podstawowy zakres gry obejmuje bezczynność, chodzenie, bieg, skradanie, skok, pływanie, atak, obrażenia, śmierć i pojawienie się modelu. Obsługa łuku i tarczy należy do etapu wyposażenia, o ile te przedmioty są dostępne dla tej postaci. Animacje nie zmieniają mechaniki obrażeń, kolizji ani uprawnień.

## Pliki i odpowiedzialność

- `output/undead-player-v5/race_nieumarly.bbmodel` — kanoniczny projekt roboczy, kopia V4 z kolejnymi ocenionymi zmianami.
- `output/undead-player-v5/face-review/` — porównanie twarzy V2, V4 i wariantu roboczego.
- `output/undead-player-v5/reviews/` — rendery, GIF-y, zapis problemów i wyników oceny etapów.
- `output/undead-player-v5/animation-matrix.md` — nazwy animacji, zdarzenia, priorytety, status implementacji i testów.
- `output/undead-player-v5/integration-audit.md` — odnalezione źródła CharacterCreator, sposób nakładania modelu, cykl życia i obsługa wyposażenia.
- `output/undead-player-v5/verification.md` — dowody importu, testów wizualnych, testów gry i ograniczenia.
- `output/undead-player-v5/README.md` — pliki wynikowe, instalacja i dokładny zakres gotowości.
- `tools/blockbench/render_undead.py`, `render_undead_motion.py` — dostosowanie do wskazanego projektu i listy animacji; bez niejawnego wyboru modelu otwartego w edytorze.
- `tools/blockbench/validate_undead.py` — rozszerzenie kontroli o docelowy zestaw stanów i rzeczywiste transformacje kości.
- `tools/blockbench/build_undead_player.py` — generator historycznej bazy; nie uruchamiać go na kanonicznym projekcie V5.
- `plugins/ModelEngine/blueprints/race_nieumarly.bbmodel` — zastąpić dopiero w etapie wdrożenia.

## Etap 1: Punkt odniesienia i audyt integracji

**Wynik:** Niezmieniony projekt bazowy i jawna lista potwierdzonych oraz niepotwierdzonych zachowań.

- [x] Skopiować V4 do katalogu V5; zapisać SHA256 pliku źródłowego i aktualnego blueprintu serwera.
- [x] Spisać istniejące animacje: idle, walk, jump_start, jump, jump_end, attack, hurt, death, spawn.
- [x] Odczytać konfigurację CharacterCreator i odnaleźć jego źródła przez `rg --files /opt/docker/mc -g '*.java' -g '*.kt' -g 'pom.xml' -g 'build.gradle*'`.
- [x] Ustalić z kodu sposób nakładania/usuwania modelu, widoczność bazowej postaci, odrodzenie i zmianę profilu. Same nazwy metod w JAR nie są wystarczającym dowodem zachowania.
- [x] Jeżeli źródeł nie ma, zapisać brak dostępu jako ograniczenie integracji. Kontynuować grafikę i animacje; nie tworzyć w ciemno drugiego pluginu ani nie nadpisywać istniejącego JAR.
- [x] W macierzy oznaczyć każdy stan osobno: „jest w projekcie”, „zaimportowany”, „ma wyzwalacz”, „przetestowany na graczu”.

**Odbiór:** Można wskazać, co aktualnie istnieje i czego nie wiemy. Nie ma domyślnych założeń, że hurt, attack lub śmierć gracza działają automatycznie.

## Etap 2: Twarz — osobna decyzja wizualna

**Wynik:** Jedna zaakceptowana twarz przy niezmienionym stroju V3/V4.

- [x] Zrobić identycznie oświetlone zbliżenia V2 i V4: przód, trzy czwarte, bok.
- [x] Przygotować pierwszy wariant z geometrią i rysunkiem twarzy V2 na obecnym stroju. Nie zmieniać przy tym pochylenia tułowia.
- [x] Porównać zamknięcie ust, szerokość żuchwy, wielkość oczu, brwi i długość widocznej szyi.
- [ ] Jeżeli potrzebne są poprawki, zmieniać jeden element naraz. Zachować poprzedni render obok nowego.
- [x] Pokazać twarz zarówno w zbliżeniu, jak i na postaci o wysokości około 100–150 pikseli, aby ocenić czytelność z dystansu.
- [x] Uzyskać wybór użytkownika przed uznaniem twarzy za zamknięty etap: B, potwierdzone 18.09.2026.

**Odbiór:** Oczy pozostają punktem uwagi; usta nie dają przypadkowego grymasu; wyraz twarzy jest zaakceptowany. Bez tej decyzji nie zmieniać ponownie twarzy podczas pracy nad strojem.

## Etap 3: Klatka piersiowa i postawa

**Wynik:** Czytelna odsłonięta anatomia i postawa widoczna również z boku.

- [ ] Otworzyć zaakceptowany projekt w Blockbenchu i sprawdzić go w neutralnej pozie, bez animacji idle.
- [ ] Przeprojektować żebra jako połączone części biegnące od mostka ku bokom; usunąć wygląd gałązek i odłączonych ozdób.
- [ ] Ograniczyć widoczność płaskiej ciemnej powierzchni pod żebrami poprzez zmianę jej kształtu i głębokości, a nie samo zwiększanie kontrastu.
- [ ] Rozdzielić postawę na położenie miednicy, klatki i szyi. Nie uzyskiwać całego garbienia przez obrót jednego sztywnego tułowia.
- [ ] Sprawdzić zakres obrotu głowy oraz ramion z płaszczem: patrzenie w górę/dół i na boki, zamach obu rąk.
- [ ] Wykonać przód, tył, bok, trzy czwarte oraz czarną sylwetkę na jasnym tle. Porównać bez zmiany kadru z bazą etapu.

**Odbiór:** Żebra są czytelne także bez zbliżenia. Z boku szyja i barki tworzą celową postawę. Głowa i ręce nie mają oczywistych kolizji ze strojem w ocenionych pozach.

## Etap 4: Tekstury i wykończenie stroju

**Wynik:** Spójny poziom dopracowania twarzy, przodu, pleców i kończyn.

- [ ] Utrzymać szerokie poły oraz asymetrię osłon nóg; nie dodawać kolejnych detali bez wpływu na czytelność.
- [ ] Opracować osobne reguły materiałów: kość — duże płaszczyzny i pęknięcia; tkanina — załamania od szwów i pasa; skóra — otarcia na brzegach; metal — oszczędne jasne krawędzie.
- [ ] Przerysować dolne przetarcia tak, aby nie tworzyły regularnego ornamentu. Większe rozdarcia powinny wpływać na obrys geometrii.
- [ ] Dopracować czaszkę z boku i z tyłu, przedramiona i kości goleni do poziomu pleców.
- [ ] Sprawdzić UV i granice wysp; nie podnosić rozdzielczości atlasu bez wykazanej potrzeby.
- [ ] Wykonać porównanie przed/po na neutralnym tle i przy jednakowym świetle. Przedstawić użytkownikowi komplet wyglądu do oceny przed animowaniem.

**Odbiór:** Materiały odróżniają się kształtem plam i krawędzi, nie tylko kolorem. Tył nie jest uboższy od przodu, a strój nie odbiera twarzy roli głównego punktu uwagi.

## Etap 5: Podstawowy ruch i reakcje

**Wynik:** Ocenione sekwencje z naturalnymi przejściami na finalnej geometrii.

- [ ] Poprawić jump_start: ugiąć nogi razem z opuszczeniem miednicy, aby stopy nie wchodziły w ziemię.
- [ ] Sprawdzić połączenia idle → walk → idle oraz jump_start → jump → jump_end → idle. Oceniać rzeczywiste mieszanie stanów, a nie wyłącznie osobne klipy.
- [ ] Dopracować chód: podparcie, przeniesienie ciężaru, miednicę, barki i opóźnienie ręki. Tempo dopasować później do zmierzonej szybkości gracza.
- [ ] Dopracować attack z rzeczywistym typem broni testowej: przygotowanie, moment kontaktu, wyhamowanie i powrót. Zweryfikować możliwość ataku w ruchu.
- [ ] Dopracować hurt jako krótką reakcję bez przesuwania fizycznej pozycji gracza; sprawdzić serię trafień i trafienie podczas ataku.
- [ ] Przebudować death: utrata podparcia, ugięcie kolan, ruch miednicy, upadek i osiadanie kończyn. Nie ograniczać jej do obrotu całej postaci wokół stóp.
- [ ] Sprawdzić kontakt całej geometrii z podłożem w śmierci, a nie tylko dolny punkt w kilku klatkach. Po zmianie głowy przeliczyć stare korekty wysokości.
- [ ] Nagrywać pełne klipy oraz przejścia z widoczną płaszczyzną ziemi, z przodu i z boku. Zapewnić odpowiedni kadr również dla leżącej postaci.

**Odbiór:** Brak widocznych skoków pozy między stanami, zapadania stóp i lewitowania ciała w sprawdzonych przebiegach. Śmierć ma fazy utraty równowagi i osiadania. Testy liczbowe uzupełniają ocenę ruchu.

## Etap 6: Pozostałe zachowania gracza

**Wynik:** Nazwane klipy i opis ich uruchamiania.

| Klip | Zdarzenie | Zasada |
|---|---|---|
| run | Ruch przy włączonym sprincie | Zastępuje walk; nie tylko przyspieszenie pracy rąk |
| crouch_idle | Skradanie bez ruchu | Niższa postawa z poprawnym podparciem |
| crouch_walk | Skradanie w ruchu | Zachowuje niższą postawę |
| swim | Faktyczny stan pływania | Pozioma pozycja i cykliczny ruch kończyn |
| swim_idle | Brak ruchu w odpowiednim stanie wodnym | Zachowanie dobrać do pozy gracza, nie do samego kontaktu z wodą |
| bow_draw / bow_hold | Używanie łuku | Prawidłowe ręce, kierunek celowania i przerwanie po puszczeniu |
| block | Używanie tarczy | Tarcza osłania przód; stan kończy się po zaprzestaniu używania |

- [ ] Ustalić na podstawie wyposażenia i reguł serwera, czy łuk i tarcza są dostępne; wpisać wynik do macierzy.
- [ ] Wykonać run, crouch_idle, crouch_walk i swim/swim_idle oraz przejścia do pozy stojącej.
- [ ] Wykonać bow_draw, bow_hold i block dla dostępnego wyposażenia; niedostępne stany jawnie oznaczyć jako poza zakresem tej rasy.
- [ ] Jawnie określić zachowanie podczas jedzenia, jazdy i elytry: osobny klip, zachowanie bazowe albo niedostępna mechanika. Nie pozostawiać aktywnej pozy ataku lub pływania po zmianie stanu.

**Odbiór:** Nie ma nieopisanej podstawowej czynności dostępnej na serwerze. Dla każdej pozy istnieje warunek wejścia, wyjścia i sposób przerwania.

## Etap 7: Integracja z graczem

**Pliki:** Źródła integracji wskazane w `integration-audit.md`; dokładna lokalizacja musi wynikać z etapu 1. Nie edytować samego JAR jako substytutu źródeł.

- [ ] Dla każdej zmiany kodu dodać test konkretnego przejścia stanu przed implementacją, w istniejącym środowisku testowym tego pluginu.
- [ ] Podłączyć hurt do skutecznie zadanych obrażeń; anulowane zdarzenie nie uruchamia reakcji.
- [ ] Podłączyć attack do właściwego działania gracza, z uwzględnieniem przerwania i powtarzania.
- [ ] Podłączyć ruch i używanie przedmiotów do stanów z macierzy. Zweryfikować priorytety w zainstalowanej wersji ModelEngine; nie opierać się wyłącznie na flagach override.
- [ ] Zapewnić, że death przerywa pozostałe stany. Zweryfikować czas zachowania modelu po śmierci gracza; nie dodawać systemu zwłok bez osobnej decyzji projektowej.
- [ ] Synchronizować główną i drugą rękę, zmianę slotu i dominującą rękę. Sprawdzić, czy standardowe ręce lub przedmioty nie są widoczne podwójnie.
- [ ] Sprawdzić założenie/zdjęcie modelu, respawn, zmianę świata i profilu, ponowne wejście oraz odbiorcę dołączającego później.

**Przypadki obowiązkowe:** Trafienie w bezruchu, trafienie w biegu, dwa szybkie trafienia, anulowane obrażenia, atak w biegu, śmierć podczas ataku, odrodzenie, zmiana przedmiotu podczas pozy użycia.

**Odbiór:** Stany działają z rzeczywistych zdarzeń. W razie braku źródeł etapu nie oznaczać jako ukończonego i nie nazywać projektu pełną obsługą gracza.

## Etap 8: Test w Minecraft i wdrożenie

- [ ] Zachować kopię działającego blueprintu i integracji przed wdrożeniem kandydata.
- [ ] Uruchomić walidator wskazując projekt V5; sprawdzić też eksport z edytora. Uwzględnić wszystkie finalne animacje i transformacje spoczynkowe kości.
- [ ] Zaimportować kandydata w ModelEngine, sprawdzić logi oraz komplet wygenerowanych części i tekstury.
- [ ] Przebudować Nexo, jeśli zmieniły się zasoby, i potwierdzić obecność nowych danych w finalnej paczce.
- [ ] W uzgodnionej sesji testowej ocenić pierwszą osobę, F5 i widok drugiego gracza. Nie zastępować tego zrzutami Blockbencha.
- [ ] Powtórzyć czynności z macierzy na płaskim podłożu, schodach, przy ścianie i w wodzie. Sprawdzić miecz, łuk i tarczę, jeśli są dostępne.
- [ ] Sprawdzić wydajność względem V4 przy tej samej liczbie widocznych modeli. Bez takiego pomiaru nie deklarować gotowości na dużą liczbę graczy.
- [ ] Zapisać wyniki, pozostałe usterki i ograniczenia w verification.md; dołączyć czytelne nagrania i porównanie wyglądu z V2/V4.
- [ ] W przypadku regresji przywrócić kopię blueprintu/integracji i odbudować zmienioną paczkę.

**Odbiór końcowy:** Zaakceptowany wygląd, przetestowane dostępne zachowania gracza, poprawne wyposażenie i kamera, brak widocznych poważnych kolizji oraz brak nowych błędów importu/integracji. Każdy niewykonany test pozostaje jawnie oznaczony.

## Kolejność realizacji i przeglądy

1. Audyt i twarz — pierwszy przegląd wizualny.
2. Anatomia, postawa i tekstury — drugi przegląd całego modelu.
3. Podstawowy ruch i brakujące stany — trzeci przegląd pełnych klipów.
4. Integracja, test gracza i wdrożenie — odbiór końcowy.

Nie przeskakiwać z niezaakceptowanego wariantu twarzy do publikacji na serwerze. W trakcie oczekiwania na ocenę wyglądu można prowadzić audyt integracji i przygotować macierz stanów, ponieważ te prace nie zależą od wyboru twarzy.

## Postęp wykonania — 18.09.2026

Etap 1 zakończony: źródła odnaleziono poza /opt/docker/mc, w /home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator. Zapisano audyt i macierz stanów. Etap 2: przygotowano porównanie A/B, sprawdzono niezmienność ciała i animacji; oczekuje na wybór użytkownika. Etapy 3–8 nie rozpoczęły się. W tej iteracji nie wykonywano mutacji runtime. Wykryto niezależną zmianę JAR CharacterCreator; audyt wymaga odświeżenia przed zmianami kodu, co zapisano w integration-audit.md.
