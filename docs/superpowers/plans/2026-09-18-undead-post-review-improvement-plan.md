# Nieumarły — plan poprawy po całościowym review

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Odhaczać kroki dopiero po zapisaniu dowodów. Ten dokument przygotowuje pracę; nie uruchamia implementacji ani wdrożenia.

**Goal:** Doprowadzić nieumarłego do spójnego wyglądu i ruchu oraz kompletnej, sprawdzonej obsługi jako rasy gracza na world3.

**Architecture:** Rozwijać aktualnego kandydata, zachowując już wykonane naprawy. Najpierw ustalić kontrakt stanów i wyposażenia oraz odebrać poprawiony bridge, następnie dopracować model i animacje, a na końcu zweryfikować cały zestaw na graczu. Plik modelu, plugin i dostarczona paczka tworzą jedno wydanie identyfikowane hashami.

**Tech Stack:** Blockbench 5.1.6/MCP, Python, CharacterCreator/Java, Paper 1.21.11, ModelEngine 4.0.9, MythicMobs 5.11.2, Nexo 1.20 — wersje zastane, do ponownego potwierdzenia przy wykonaniu.

**Spec:** `output/undead-player-v5/reviews/whole-independent-2026-09-18/review.md` oraz karta kierunku `output/undead-player-v5/undead-feeling.md`. Ten plan aktualizuje kolejność pozostałych prac z wcześniejszych planów; nie unieważnia zaakceptowanej twarzy B.

## Założenia i decyzje

- Charakter: świadomy, wyniszczony nieumarły, nadal sprawny w walce. Bez komediowego kuśtykania, ciągnięcia nogi i opóźniania sterowania.
- Twarz B pozostaje zaakceptowaną bazą. Opinia recenzenta o sympatycznym szkielecie nie unieważnia decyzji użytkownika. Ewentualny wariant twarzy jest osobną propozycją, nie warunkiem napraw technicznych.
- Zachować strój, paletę i czytelną asymetrię. Główny obszar poprawy wyglądu: przestrzenność klatki, postawa i czytelność w ruchu.
- Bazą jest `gait-rework-01`, SHA256 `bc70eacc7981b41d3436fa5bf2b969bd55f68bc775a65ab4ed304e6452ca9634`. To prototyp, nie zaakceptowany finalny model.
- `output/undead-player-v5/race_nieumarly.bbmodel` i blueprint world1 są starszym V4. Nie używać ich jako źródła kolejnej iteracji.
- Docelowy serwer: **world3**. Wzmianki o końcowym wdrażaniu world1 w starszych planach są nieaktualne dla tej pracy.
- Poprawiony bridge w `integration-repair/source-work/` jest kandydatem. Nie przepisywać go od nowa, lecz sprawdzić, scalić ze źródłami i uzupełnić brakujące zachowania.
- Nie powtarzać napraw walidatora i identyfikacji projektu, które przeszły obecne testy. Rozszerzać je tylko o wykazane luki.
- Nie zmieniać istniejących postaci graczy do testów. Historyczne modele i dowody pozostają nienaruszone.
- Nie dodawać systemu zwłok, nowego systemu walki, VFX ani audio do tego zakresu.
- Brak Git w katalogu serwera: snapshoty i hashe. Zewnętrzne źródła Java podlegają instrukcjom ich repozytorium i uprawnieniom środowiska.
- Brak klienta gry oznacza brak odbioru runtime; brak MCP oznacza brak świeżego odbioru edytora. Offline pozostaje diagnostyką pomocniczą.

## Wybór podejścia

Rekomendacja: **kontynuacja kandydata z odbiorem etapowym**. Zachowuje poprawiony skok, śmierć, twarz B i użyteczne narzędzia. Pozwala osobno odrzucić nieudany ruch lub wygląd bez cofania integracji.

Sama kosmetyka nie zamknie braków stanów gracza i wyposażenia. Przebudowa od zera zwiększyłaby ryzyko regresji i ponownego rozstrzygania zaakceptowanych elementów. Żaden z tych wariantów nie jest teraz uzasadniony.

## 1. Zamrożenie bazy i przywrócenie wiarygodnego podglądu

**Pliki:** `output/undead-player-v5/revision-index.json`, `tools/blockbench/README.md`; nowy `output/undead-player-v5/improvement-baseline/manifest.json`. Dowody kolejnych etapów w nowych katalogach `reviews/improvement-<etap>-<rewizja>/`.

- [x] Potwierdzić hash kandydata oraz aktualny stan world3 i źródeł/JAR CharacterCreator. Zapisać różnice od review; nie zakładać, że środowisko pozostało niezmienione.
- [x] W manifeście zapisać źródło, hash, poprzednią rewizję, wersję narzędzi i status `PROTOTYPE_NOT_ACCEPTED`.
- [x] Przywrócić działający Blockbench/MCP zgodnie z lokalnym README; sprawdzić tożsamość aktywnego projektu przed pomiarem. Nie osłabiać kontroli tożsamości w celu obejścia błędu.
- [x] Uruchomić istniejące kontrole:

```bash
python3 -m unittest discover -s tools/blockbench/tests -v
python3 tools/blockbench/validate_undead.py output/undead-player-v5/gait-rework-01/race_nieumarly.bbmodel
python3 tools/blockbench/inspect_review_project.py --source output/undead-player-v5/gait-rework-01/race_nieumarly.bbmodel
```

- [ ] Utrwalić świeży front/bok/tył/3⁄4 i sekwencję idle–walk–stop–attack–hurt–death. Zapis źródła, kamery i czasu musi umożliwiać późniejsze porównanie.

**Odbiór:** jednoznacznie wskazana baza i działający podgląd; obecne 51 testów stanowi punkt odniesienia, nie sztywną docelową liczbę. Awaria podglądu nie blokuje pracy nad kontraktem Java, ale blokuje odbiór zmian wizualnych.

## 2. Kontrakt zachowania gracza i wyposażenia

**Pliki:** uaktualnić `output/undead-player-v5/animation-matrix.md`, `integration-audit.md` i `integration-repair/implementation-contract.md`.

- [ ] Sprawdzić dostępne mechaniki i przedmioty serwera. Dla każdej pozycji poniżej zapisać wejście, wyjście, przerwanie, priorytet, zakres kości, blend oraz właściciela: ModelEngine albo CharacterCreator.
- [x] Potwierdzić automatyczne zachowania w zainstalowanym ModelEngine, zanim powstaną dodatkowe wyzwalacze. Jeden właściciel uruchamiania danego stanu.
- [ ] Przyjąć następujący zakres funkcjonalny:

| Zachowanie | Planowana obsługa |
|---|---|
| Stanie/chód/sprint | idle, walk, run; tempo zgodne z transportem gracza |
| Skradanie | crouch_idle i crouch_walk; poprawna wysokość i powrót |
| Skok | jump_start → jump → jump_end; również skok w ruchu i ponowny skok |
| Woda | swim/swim_idle dla właściwej pozy gracza; samo wejście do wody nie oznacza pływania |
| Atak | Wyzwalacz właściwej akcji; działanie także w ruchu i locie |
| Obrażenia | hurt po skutecznym trafieniu; brak reakcji na anulowane obrażenia |
| Łuk i tarcza | draw/hold/block, jeśli dostępne; puszczenie, zmiana slotu i śmierć kończą stan |
| Śmierć/odrodzenie | death przerywa pozostałe stany; jawny czas istnienia modelu; spawn raz na właściwe zdarzenie |
| Jedzenie/jazda/elytra | Dla każdej dostępnej mechaniki sprawdzona poza lub fallback; nie wyłączać mechaniki tylko z powodu braku klipu |
| Główna/druga ręka | Przedmiot, dominująca ręka i zmiana slotu zgodne z graczem |

- [ ] Zdefiniować wynik błędu zastosowania modelu: brak częściowo zatwierdzonej rasy/profilu/uprawnień; przywrócenie poprzedniego stanu.
- [x] Zdefiniować cleanup: zmiana profilu/rasy, wyjście, unload pluginu, respawn i zmiana świata; usuwanie wyłącznie własnego modelu.

**Odbiór:** każda dostępna czynność ma określone zachowanie. Osobny klip nie jest wymagany, jeżeli sprawdzony fallback daje poprawny efekt. Brak wpisu nie jest fallbackiem.

## 3. Odbiór i domknięcie integracji

**Źródła:** `/home/dev/Desktop/Przemek/MC/MCMMORPG/plugin-src/character-creator/`. Względem tego katalogu: `src/main/java/pl/mcmmorpg/charactercreator/model/ModelEngineBridge.java`, `creator/ConfirmService.java`, `profile/CharacterProfileModule.java` i `CharacterCreatorPlugin.java` pod tym samym prefiksem pakietu. Punktem porównania jest kandydat z `integration-repair/source-work/`.

**Testy:** istniejący `src/test/java/pl/mcmmorpg/charactercreator/model/ModelEngineBridgeTest.java`; rozszerzenie o scenariusze potwierdzania profilu i cyklu życia przy integracji ze źródłami.

- [x] Odczytać instrukcje repozytorium, porównać źródła z kandydatem i ustalić odtwarzalny build. Nie kopiować całego snapshotu na nowszy kod.
- [x] Uruchomić istniejące testy kandydata. Harness `tools/TestModelBridge.java` ma zależne od katalogu ścieżki bibliotek — ustalić prawidłowy classpath przed uruchomieniem; błąd środowiska nie jest wynikiem testu funkcji.
- [ ] Dla każdej brakującej gwarancji najpierw odtworzyć błąd w teście, następnie poprawić kod i ponowić właściwy test:

| Scenariusz | Oczekiwany wynik |
|---|---|
| Dwukrotne apply/confirm/select tej samej rasy | Jeden własny model; brak ponownego spawn |
| Obcy model obok rasowego | Obcy model przetrwa apply i clear |
| Rasa bez modelu | Usunięty tylko model rasowy; właściwa widoczność bazowej encji |
| Brak blueprintu/błąd tworzenia po prechecku | Brak częściowego zapisu profilu lub uprawnień; poprzedni wygląd zachowany/przywrócony |
| Respawn/reconnect/zmiana świata/profilu/unload | Brak osieroconych modeli i zadań; jeden właściwy model po odtworzeniu |
| Nowy obserwator | Poprawny wygląd bez ponownego spawn u istniejących obserwatorów |

- [ ] Podłączyć brakujące wyzwalacze zgodnie z macierzą; sprawdzić anulowane obrażenia, serię trafień, atak w ruchu i śmierć przerywającą atak.
- [ ] Zweryfikować synchronizację przedmiotów przez silnik; kod uzupełniać wyłącznie tam, gdzie istniejąca obsługa nie realizuje kontraktu.
- [x] Zbudować oznaczony hashem JAR i zapisać zgodność źródło → build → kandydat. Testy bridge nie przyznają odbioru klienta.

**Odbiór:** przechodzą testy własności, idempotencji, błędów i cleanup; istnieje konkretna mapa wyzwalaczy. Wynik: kandydat integracji gotowy do testów na środowisku docelowym.

## 4. Klatka piersiowa, sylwetka i czytelność

**Pliki:** nowa rewizja w `output/undead-player-v5/chest-depth-01/`; `undead-feeling.md`; aktualizacja indeksu po zapisaniu wariantu. Edycja przez Blockbench, bez nadpisywania bazy.

- [x] Przygotować jedną wyraźną propozycję klatki: mniejsza dominacja czarnego panelu, głębia między ubraniem a kośćmi, nieregularne brzegi i celowe prześwity. Nie zwiększać jedynie kontrastu albo grubości żeber.
- [x] Zachować twarz B, paletę i animacje podczas porównania klatki. Sprawdzić prawdziwą przestrzenność z boku i 3⁄4.
- [ ] Ocenić postawę spoczynkową i tylną sylwetkę. Osobną kość szyi/barku dodawać tylko po wykazaniu, że obecny rig uniemożliwia potrzebny ruch; wtedy przeliczyć wszystkie klipy.
- [x] Zestawić przed/po w identycznych kamerach: pełny model, sylwetka bez tekstur, ekranowa wysokość około 140 px i większe zbliżenie.
- [ ] Sprawdzić UV, szyję, ramiona i klatkę w skrajnych pozach ataku, skłonu i obrotu głowy. Uprościć detale stroju, jeśli odbierają czytelność kościom.
- [ ] Przed przeniesieniem wariantu dalej uzyskać ocenę wyglądu. Krytyka twarzy pozostaje opcjonalnym osobnym eksperymentem.

**Odbiór:** klatka czyta się przestrzennie, a charakter pozostaje widoczny z dystansu; brak nowych widocznych kolizji. Nowa geometria staje się bazą końcowej animacji dopiero po odbiorze.

## 5. Ruch: atak, chód i przejścia

**Pliki:** nowe rewizje `attack-support-01/`, `gait-calibrated-01/` i `motion-coherent-01/` w `output/undead-player-v5/`; właściwe skrypty w `tools/blockbench/` tylko gdy wymagają zmiany. Dowody obejmują hash geometrii po etapie 4.

- [ ] Naprawić atak z rzeczywistym mieczem: preferować zakotwiczone stopy. Jeśli potrzebny krok, wyraźnie unieść stopę i przenieść ciężar; usunąć rozjeżdżanie obu podeszw po ziemi.
- [x] Ustalić wspólną neutralną pozycję rąk i korpusu dla końców attack/spawn/jump_end oraz wejścia idle. Nie wymuszać identycznych końców wszystkich klipów — faza ruchu może wymagać blenda.
- [ ] Zmierzyć tempo ruchu gracza i rzeczywiste skalowanie czasu klipu. Dopasować długość oraz częstotliwość kroku; obecne 0,75 bloku/s przy 1× jest punktem diagnostycznym, nie docelowym tempem.
- [ ] Dopracować wyraźną, ale funkcjonalną asymetrię; nie wracać do niemal nierozróżnialnych wariantów mikrodrgań. W idle 60 s i walk 60 s ocenić natrętne powtórzenia oraz czytelność z tyłu.
- [x] Zachować poprawione podparcie jump_start/jump_end. W death ocenić ciężar i osiadanie, nie tylko globalne minimum Y; nie przebudowywać zaakceptowanych fragmentów bez wskazanej wady.
- [x] W pomiarze próbkować pełne klipy przy 120 Hz i w kluczach. Minimum poniżej −0,04 jednostki modelu na płaskim podłożu jest alarmem istniejącego narzędzia; brak alarmu nie potwierdza wiarygodnego podparcia.
- [x] Dodać raport przemieszczenia kontaktującej stopy w przestrzeni świata dla faz podparcia. Najpierw przypadki kontrolne: nieruchoma stopa, celowy poślizg, uniesiona stopa. Nie traktować ruchu stopy względem root podczas normalnego chodu jako błędu.
- [ ] Sprawdzić wejścia/przerwania w 0%, 25%, 50% i 75% cyklu: idle↔walk/run, skok w miejscu/ruchu, attack+walk, hurt+attack, dwa hurt, death z każdego stanu. Zapisać rzeczywisty blend i pozę po zakończeniu.

**Odbiór:** brak widocznego ślizgu w ataku, chód zgodny z ruchem świata, brak widocznych przeskoków i pozostawionych póz w sprawdzonych przejściach. Sama różnica wierzchołków dwóch póz nie jest dowodem błędu runtime.

## 6. Uzupełnienie dostępnych stanów i wyposażenia

**Pliki:** `tools/blockbench/author_player_states.py`, `tools/blockbench/tests/test_player_states.py`; nowe rewizje modelu; `animation-matrix.md` i kod wyzwalaczy z etapu 3.

- [x] Traktować istniejące pięć studiów pozy jako prototypy. Nie uznawać ich za obecne w bazie ani odebrane w grze.
- [ ] Ukończyć run, crouch_idle/crouch_walk oraz swim/swim_idle zgodnie z macierzą. Bieg ma własny rytm i podparcie; samo skrócenie czasu walk nie kończy pracy.
- [ ] Dopracować bow_draw/bow_hold/block na rzeczywistym wyposażeniu. Sprawdzić obie dominujące ręce, zmianę slotu, puszczenie, celowanie pionowe i użycie podczas ruchu.
- [ ] Potwierdzić widoczność zbroi i brak podwójnych przedmiotów. Jeśli silnik nie zapewnia potrzebnej obsługi, zapisać konkretną politykę wyglądu zamiast obiecywać niezweryfikowaną funkcję.
- [ ] Sprawdzić przejścia woda–ląd, kucanie–stanie przy przeszkodzie, run–attack–hurt, przerwanie łuku/tarczy i zachowania fallback z macierzy.
- [x] Rozszerzyć wymagany komplet klipów w walidacji zgodnie z macierzą; test usunięcia wymaganego klipu ma kończyć się błędem, brak klipu zastąpionego zatwierdzonym fallbackiem nie może powodować fałszywego alarmu.

**Odbiór:** każda dostępna czynność ma działający stan lub sprawdzony fallback. Przedmioty pozostają funkcjonalne; feeling nie opóźnia trafienia, celowania ani kamery.

## 7. Odbiór na world3 i przygotowanie wydania

**Pliki:** nowy `output/undead-player-v5/release-candidate-01/` z modelem i manifestem, `verification.md`, instrukcja odtworzenia i rollback. Zmiany katalogów world3 wyłącznie w ramach uzgodnionego wykonania i wymaganych uprawnień.

Pierwsze cztery kroki poniżej stanowią również przygotowanie środowiska do prób w etapach 5–6: wykonać je po etapie 3 dla jawnie oznaczonej rewizji testowej. Końcowe wydanie otrzymuje osobny manifest i pełną regresję po zakończeniu zmian. Wczesna instalacja testowa nie jest publikacją ukończonej rasy.

- [x] Przygotować snapshot faktycznego stanu world3: pluginy, konfiguracje, paczka. Brak wcześniejszego nieumarłego oznacza rollback przez odtworzenie tej nieobecności, nie instalację V4.
- [x] Przygotować kontrolowaną instalację testową modelu i zgodnego JAR. Zweryfikować zależności CharacterCreator, zamiast kopiować całe konfiguracje świata world1.
- [ ] Wygenerować ModelEngine i końcowy ZIP Nexo; potwierdzić import wszystkich tekstur, części i klipów. Zapisać hashe blueprintu/JAR/ZIP i brak błędów importu.
- [ ] Potwierdzić, że klient otrzymał właściwą paczkę. Nagrać FPP, F5 przód/tył oraz drugiego obserwatora; to nie są widoki edytora.
- [ ] Przejść pełną macierz na płaskim terenie, schodach, krawędziach, przy ścianie i w wodzie. Sprawdzić głowę, hitbox, kamerę, broń, zbroję, śmierć, respawn, zmianę profilu i późne dołączenie obserwatora.
- [ ] Porównać scenę bez modeli i z 1/10/30 widocznymi modelami: MSPT/TPS, liczba encji oraz czas klatki/FPS klienta. Zapisać sprzęt i ustawienia. Docelowy budżet wydajności ustalić przed pomiarem dla rzeczywistego celu serwera; bez niego raportować wynik bez deklaracji gotowości MMO.
- [x] Sprawdzić rollback w środowisku testowym. Przygotować konkretny zestaw do wdrożenia wraz z wynikami i pozostałymi ograniczeniami.

**Odbiór końcowy:** osobne `ART_PASS`, `INTEGRATION_PASS`, `RUNTIME_PASS` odnoszące się do tego samego wydania. Brak otwartych P1; pozostałe P2 poprawione albo jawnie zaakceptowane. Brak testu pozostaje `NOT_RUN`, nigdy PASS.

## Kolejność i punkty kontroli

1. Etapy 1–2: pewna baza i kompletna macierz zachowań.
2. Etap 3: gotowy do próby kandydat integracji. Etap 4 może być przygotowywany niezależnie, ale końcowy ruch zależy od przyjętej geometrii.
3. Etap 4: odbiór przestrzenności klatki i postawy, z zachowaniem twarzy B.
4. Etapy 5–6: odbiór pełnego ruchu, przejść i wyposażenia; pierwsze testy runtime wykonywać tu, nie odkładać całej kalibracji do końca.
5. Etap 7: pełna regresja jednego wydania i przygotowanie wdrożenia.

## Pokrycie 11 ustaleń review

| Ustalenie | Etap |
|---|---|
| Brak produktu na world3 | 1, 7 |
| Brak stanów i wyzwalaczy | 2, 3, 6 |
| Własność modeli/idempotencja/błędy | 3 |
| Wadliwy skok starszego blueprintu | 1, 5, 7 — nie cofać istniejącej naprawy |
| Kontrakt przejść | 2, 5, 6 |
| Ślizg ataku | 5 |
| Kalibracja i charakter chodu | 5 |
| Odbiór twarzy/sylwetki | 4, 5 — twarz B zachowana |
| Żebra jak emblemat | 4 |
| Wyposażenie i perspektywa | 2, 6, 7 |
| Ograniczenia narzędzi i reprodukcji | 1, 5, 7 |

**Pierwszy pakiet do wykonania:** zamrożona baza, przywrócony podgląd i uzupełniona macierz stanów. Dopiero z tym kompletem rozpoczynać zmiany wyglądu oraz domykanie kodu integracji.

## Stan wykonania — 18.09.2026

Szczegóły i granice: `output/undead-player-v5/improvement-execution/model-result.md`. Aktualny model: player-motion-03, 21 klipów. Niezależne scoped review klatki, korekty ruchu i narzędzi zakończone. Odbiór wyposażenia i runtime pozostaje otwarty; nieodhaczone kroki nie są automatycznie ukończone przez obecność prototypu. Integracja Java przechodzi osobny przegląd i nie została wdrożona.

### Aktualizacja po instalacji testowej

Finalny raport: `output/undead-player-v5/improvement-execution/result.md`. Integracja: 107 testów PASS, R1/R2/R3 zamknięte w niezależnym rereview. Kandydat zainstalowano na world3, utworzono bezpieczny creator_limbo, potwierdzono start/import oraz generację i udostępniane bajty ZIP Nexo. Wydanie jest związane hashami w release-candidate-01. 21 klipów potwierdzono w identycznym blueprintcie; nie utożsamiać tego z odczytem obiektów animacji żywego silnika. Odbiór klienta, kalibracja, wydajność i faktyczny rollback pozostają NOT_RUN. Gwarancja atomowości między pluginami pozostaje ograniczona API — patrz raport integracji; fail-closed nie jest pełnym rollbackiem.

Próba rollbacku plików/usługi/paczki na pustym world3 wykonana: wcześniejsze bajty i nieobecność payloadu odtworzone, start bez CharacterCreator, udostępniana paczka identyczna z wcześniejszą. Pusty pokój pozostawiono nieużywany; danych postaci nie cofano. Następnie przywrócono kandydata testowego. Dowód world3-rollback-result.json.
