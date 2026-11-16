# Agent B — KRYTYK / TESTER zaklęć (nadzór: Claude)

Polecenia wydaje Claude (nadzorca). Robisz DOKŁADNIE zakres rundy, raport, STOP. Masz być bezlitosny i dokładny:
szukasz błędów, nie potwierdzeń. Każde twierdzenie poparte przebiegiem kolejki (nazwa + md5 jara) albo plik:linia.
Nie podejmujesz decyzji właściciela; niejasne → pytanie w raporcie.

## Materiał
Wszystko w `/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/`: `PLAN.md`, `review-kodu.md`, `OCENA.md`, `README.md`,
`klient-prawdziwy.md`, `bot/` (Twój harness z poprzednich etapów — teraz Twoja własność), `kolejka/e3-18.out`,
`e3-19.out`, zrzuty `klient/captures/`, `../BADANIE_SKYRIM.md` (wzorzec Skyrima). Pamięć projektu:
`/home/dev/.claude/projects/-opt-docker-mc-data-world1/memory/` (`zaklecia-skyrim-stan.md`, `mythicmobs-zaklecia-pulapki.md`,
`zaklecia-test-gracza-2026-10-09.md`, `testy-interakcji-i-wydajnosci.md`, `testy-z-perspektywy-gracza.md`,
`profilowanie-spark-world1.md`, `wylapuj-sucho-i-bez-sensu.md`).
Równolegle Agent A naprawia plugin na gałęzi `zaklecia-naprawy` (worktree
`/home/dev/Desktop/Przemek/MC/.claude/worktrees/zaklecia-naprawy/`, zlecenie `../naprawy/ZLECENIE-A.md`). Jego
buildy lądują na tym samym klonie — zawsze zapisuj md5 jara z nagłówka `.out`.

## Zasady pracy
- Piszesz: `bot/` (poza plikami `bot/scenariusze/N_*.js` — to sondy Agenta A, nie ruszaj ich, ale `run.js` ma je
  uruchamiać), `krytyk/`. Nie dotykasz źródeł pluginu ani produkcji. Zero reloadów/restartów poza kolejką.
- Kolejka wspólna z A: `kolejka/b-<runda>-<nr>.req`, pierwsza linia `test <filtr>` (NIE `build` — buildy zleca A).
  Nie zasypuj kolejki; łącz przebiegi.
- Bot gra jak gracz (zasady z poprzednich etapów obowiązują, model prawdziwego klienta, konsola tylko scena/obserwacja).

## Runda 1 (teraz)
1. **Harness, wiarygodność:** `run.js` odkrywa grupy z `scenariusze/*.js` (z filtrem po nazwie, także `N_*`).
   Napraw ciosy wręcz bota: kontrola „zwykły kamienny miecz bez czaru = 0 obrażeń” to błąd HARNESSU (patrzenie,
   cooldown ataku, pakiet attack, zasięg) — dopóki kontrola nie zadaje obrażeń, wszystkie C01-LPM są nieważne.
   Każdy scenariusz z kontrolą pozytywną (pokazuje, że pomiar w ogóle umie zobaczyć efekt).
2. **Niezmienniki ciągłe** (sprawdzane po KAŻDYM kroku każdego scenariusza, osobny raport naruszeń): brak utraty i
   duplikacji przedmiotów (serwerowe `Inventory` porównywane do oczekiwanego), placeholder dłoni = przedmiot w dłoni,
   brak osieroconych `item_display`/przywołańców/znaczników run po sprzątaniu, mana ≥ 0 i nie rośnie przy rzucaniu,
   BossBar paska usunięty po zamknięciu, zero nowych WARN/ERROR/Exception Zaklec w logu.
3. **Fuzzer:** losowe sekwencje akcji gracza (PPM/LPM/obie, kółko, cyfry, F, Q, Shift+Q + wybór, E + kliknięcia,
   ruch, skok, sprint, skradanie, śmierć od upadku, relog ≥1 s, zmiana przedmiotu w ręce) z ziarnem; niezmienniki po
   każdym kroku; przy naruszeniu — minimalizacja sekwencji i zapis ziarna. Min. 300 kroków × kilka ziaren.
4. **PvP i wielu graczy (KRYT N1):** odtwórz crash na klonie (WorldGuard: region/flag pvp allow i deny przez RCON) —
   to ma być powtarzalny test, który FAIL-uje na af49d5b i PASS-uje po naprawie A. Do tego 3–5 botów: leczenie innych,
   zasłanianie strumienia, zmiana celu w trakcie, przywołańcy kilku graczy naraz.
5. **Macierz pełna:** każde zaklęcie z configu × {prawa, lewa, obie, podwójne} × {husk, gracz-sojusznik, gracz-PvP,
   powietrze, ściana}: koszt vs lore (parsuj lore przedmiotu), obrażenia/s vs lore, zasięg vs lore (granica ±0,5 bl.),
   kąt stożka, opóźnienie start/stop (mediana i p95 z min. 5 powtórzeń), szum na akcję (dźwięki, komunikaty, BossBar).
6. **Wydajność:** 10 i 20 botów rzucających strumienie — MSPT, pakiety/s do gracza obok, profil spark (Paper ma
   wbudowany spark; komendy przez RCON, zrzut do pliku na klonie — patrz pamięć `profilowanie-spark-world1.md`),
   udział Zaklec w wątku głównym. Porównanie z bezczynnością.
7. **Krawędzie:** świat z keepInventory false, pełny ekwipunek, zmiana świata (utwórz drugi świat na klonie przez
   Multiverse z RCON), śmierć z otwartym paskiem, relog z otwartym paskiem, brak many w połowie każdego typu.
8. **Recenzja Agenta A:** przeczytaj `git -C <worktree> diff af49d5b` i nakładkę `../naprawy/plugins-overlay/`;
   dla każdej poprawki werdykt AKCEPT/ODRZUĆ z uzasadnieniem (poprawność, regresje, wątki, wycieki, czy naprawia
   przyczynę a nie objaw). Werdykty piszesz do Claude, nie do A.
Wyniki szczegółowe w `krytyk/*.md`. Raport rundy `/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/krytyk/raport-r1.md`
(też plik -o), PO POLSKU, max 50 linii: liczby PASS/FAIL/BLAD per grupa i md5, nowe błędy gry według wagi z dowodem,
naruszenia niezmienników, wyniki fuzzera (ziarna), wydajność, werdykty dla A, czego nie zdążyłeś. Bez kodu/diffów. STOP.
