# Agent A — NAPRAWY pluginu Zaklecia (nadzór: Claude)

Polecenia wydaje Claude (nadzorca). Robisz DOKŁADNIE zakres rundy, potem raport i STOP. Nie podejmujesz decyzji
właściciela, nie rozszerzasz zakresu, nie „przy okazji” refaktorujesz. Niejasne → pytanie w raporcie, reszta dalej.
Twoją pracę niezależnie recenzuje Agent B (krytyk) i Claude; każda poprawka musi się obronić testem.

## Materiał (przeczytaj najpierw)
- Wyniki testu: `/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/OCENA.md`, `review-kodu.md`, `PLAN.md`, `README.md`,
  zrzuty prawdziwego klienta `klient/captures/` (K01f, K04-pas, K0506, K08b2, K02-10).
- Pamięć projektu: `/home/dev/.claude/projects/-opt-docker-mc-data-world1/memory/` — `zaklecia-skyrim-stan.md`,
  `mythicmobs-zaklecia-pulapki.md`, `zaklecia-test-gracza-2026-10-09.md`, `mythicmobs-mechaniki-api-sync.md`,
  `animacje-broni.md`, `mmocore-pasek-akcji-nadpisuje.md`, `komunikaty-audyt.md`.

## Gdzie pracujesz
- Kod: worktree `/home/dev/Desktop/Przemek/MC/.claude/worktrees/zaklecia-naprawy/MCMMORPG/plugin-src/zaklecia/`
  (gałąź `zaklecia-naprawy` od af49d5b = wersja na world1). NIE commitujesz — Claude commituje po przeglądzie.
- Konfiguracje (MythicMobs/Zaklecia/DeluxeMenus/…): NIE edytujesz klona; kopiujesz plik do nakładki
  `/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/naprawy/plugins-overlay/<ścieżka jak w plugins/>` i tam zmieniasz.
  Oryginały do odczytu: `/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/data/plugins/`.
- Raporty: `/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/naprawy/`.
- `bot/` należy do Agenta B — nie edytujesz. Możesz dodać WŁASNE sondy jako `bot/scenariusze/N_<nazwa>.js`
  (sam plik, wzór jak istniejące grupy; uruchamiasz `test N_<nazwa>`).

## Kolejka (Docker jest poza Twoim sandboxem)
Plik `/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/kolejka/a-<runda>-<nr>.req`, pierwsza linia:
`build` (buduje Twoją gałąź, wgrywa jar + nakładkę na klon, restartuje klon, w .out start i ostrzeżenia Zaklec) albo
`test <filtr>` (bot/run.sh). Wynik: `.out`, `.serwer.log`, `.done`. Czekaj pętlą na `.done`. Kolejka jest wspólna z
Agentem B i idzie po kolei — nie zasypuj jej; pełne A–F trwa ~4 min. Każdy `.out` ma nagłówek `JAR … md5` — sprawdzaj,
że test szedł na Twoim buildzie.

## Lista napraw (N = numer, waga)
- **N1 KRYT** rekurencja `EfektyCzasowe.pomocSlug → wskazCel → celWZasiegu → dozwolonyCel → MythicLib canTarget →
  DamageCheckEvent → pomocSlug` = StackOverflow, crash serwera (cel-gracz + świat z PvP; world1 ma PvP prawie wszędzie).
  Napraw u źródła: pomijaj sztuczne zdarzenia MythicLib (sprawdź w jarze MythicLib klasę DamageCheckEvent / interfejs
  zdarzeń „fake”) we WSZYSTKICH listenerach obrażeń pluginu + osłona przed ponownym wejściem. Przejrzyj cały plugin pod
  ten sam wzór (każde canTarget/canInteract wołane z listenera zdarzeń obrażeń). Raport: lista wszystkich takich miejsc.
- **N2 WYS** broń w prawej + zaklęcie w lewej: czasem mana schodzi, strumień trafia, 0 obrażeń (K08b2; C01 w e3-18/19
  raz trafia, raz nie). Najpierw instrumentacja: `debug: true` w config Zaklec → linie `ZAKDBG` w logu dla każdego tiku
  trafienia (kandydaci stożka, LOS, wynik mmoCanTarget, cel przekazany do MM, obrażenia przed/po). Domyślnie wyłączone.
  Potem przyczyna i poprawka. Uwaga: MythicLib `mmodamage` bierze metadane ataku z GŁÓWNEJ dłoni.
- **N3 ŚR** zwykły przedmiot (tarcza, łuk, blok, mikstura, kilof) wybrany obok zaklęcia w lewej → po PPM w
  `SelectedItem` znów `debug_stick` (C-tarcza/łuk/blok). Nośnik nigdy nie może nadpisać ani wypchnąć cudzego przedmiotu.
- **N4 ŚR** pasek Shift+Q: ~290 pakietów BossBar, 20 komunikatów, 15 dźwięków na 5 akcji. BossBar tylko przy zmianie
  treści, dźwięk/komunikat max 1 na akcję gracza.
- **N5 ŚR** ładowane „Uzdrowienie innego” nie leczy MagCel (B-uzdrowienie-innego-cel), ciągłe leczy. Przyczyna + poprawka.
- **N6 WYS** Księga: po LPM/PPM na zaklęciu panel „Twoje dłonie” zostaje pusty, potwierdzenie pod krawędzią okna (K01f).
  Panel ma pokazać nowy stan od razu; potwierdzenie widoczne w oknie albo dźwięk + odświeżenie.
- **N7 WYS** strumień Odmrożenia zasłania ~pół ekranu w 1. osobie (K04-pas). Rzucający ma widzieć cel. Zaproponuj
  i zrób najprostsze skuteczne (np. osobny, węższy/krótszy/półprzezroczysty obiekt tylko dla rzucającego, a pełny dla
  innych) — bez psucia widoku dla obserwatorów. Ocena tylko prawdziwym klientem (Claude zrobi zrzuty).
- **N8 ŚR** „You are now in combat!” po angielsku — znajdź plugin/klucz i spolszcz w nakładce (zgodnie z audytem
  komunikatów: pasek MythicLib priorytety).
- **N9 NIS** z review: `Nosnik.java:171–186` błędne id usuwa zaklęcie przed walidacją; `Wybor.java:180–197` opóźniony
  LPM paska może przypisać pozycję z nowej sesji; `Wybor.java:214–215` BossBar paska po śmierci; `Rzucanie.java:273–288`
  i `RzutyOdrzutowe.java:199–231` zmiana świata; `RzutyOdrzutowe.java:185–195` mana pobrana bez efektu.

## NIE ruszasz (decyzje właściciela)
Mapowanie PPM/LPM/Shift+Q; zachowanie prawej po puszczeniu PPM w trybie „obie”; koszty many; Sakwa/stałe sloty;
biegłości klas; menu huba poza Księgą zaklęć.

## Runda 1 (teraz): N1, N2 (instrumentacja + diagnoza; poprawka jeśli pewna), N3, N4.
Każda poprawka: minimalna zmiana, komentarz zgodny ze stylem pliku, `build` przez kolejkę, regresja `test ABCDEF`
(+ własna sonda N_* dla tej poprawki: najpierw pokazuje FAIL na starym zachowaniu, potem PASS). Dla N1 sonda: dwa boty,
przywołany wilk, cios gracz→gracz w świecie z PvP — na klonie wolno (klon może paść; po padnięciu zleć `build`, który
go podniesie, i zapisz to w raporcie).
Raport rundy: `/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/naprawy/raport-r1.md` (to też plik -o), PO POLSKU,
max 40 linii: dla każdego N: przyczyna (plik:linia), co zmienione (pliki), dowód (nazwy przebiegów kolejki + wynik),
ryzyko regresji. Lista zmienionych plików. Bez kodu i diffów. Potem STOP i czekasz na polecenia Claude.
