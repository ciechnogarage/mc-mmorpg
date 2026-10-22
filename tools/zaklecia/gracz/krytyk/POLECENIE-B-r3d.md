# Polecenie Claude dla Agenta B — dokończenie rundy 3 w NOWYM środowisku

Przerwałem Cię, bo właściciel wprowadził twarde zasady (`/home/dev/.claude/CLAUDE.md`, przeczytaj): klon tylko przez
wspólne `klon.sh`, bot tylko przez wspólny `/opt/docker/mc/narzedzia/bot`. Przeniosłem wszystko:
- Klon: `klon-zaklecia` (dane `/opt/docker/mc/klony/zaklecia/data`, log `.../data/logs/latest.log` — możesz czytać).
  Stary `zaklecia-gracz` usunięty. Na klonie jest ostateczna wersja A (Zaklecia b8a73aa, Ksiega 1433dfb, debug:false).
- Kolejka działa jak dotąd (`kolejka/b-3-NN.req`, `test <filtr>`), ale harness biegnie przez wspólnego bota:
  node 20, pakiety z `NODE_PATH=/bot/node_modules` (własnych node_modules NIE instalujesz), katalog `bot/` montowany
  TYLKO DO ODCZYTU. Wyniki JSON i ślady fuzzera zapisują się do `/bot/wyniki/zaklecia` = na hoście
  `/opt/docker/mc/narzedzia/bot/wyniki/zaklecia/` (stare wyniki tam przeniosłem). `/clone-logs` i `/baseline.log` już
  nie istnieją w kontenerze — kontrolę nowych WARN/ERROR Zaklec/Ksiegi robi runner i dopisuje sekcją `BLEDY-LOGU` na końcu
  `.out`; Twój niezmiennik logu traktuj jako wyłączony i czytaj tę sekcję.
- Kontrolny przebieg `c-02` (grupa A) przeszedł łańcuch: kontrola dodatnia PASS, 0 nowych błędów w logu. FAIL-e tam to
  harness: (1) niezmiennik BossBar „brak REMOVE” — MythicHUD przechwytuje REMOVE i wysyła pusty ADD na tym samym ID;
  uznaj pusty tytuł/pusty ADD jako zamknięcie paska; (2) `setSlotPerSec` łapie przerysowanie okna Księgi po `[refresh]`
  (DeluxeMenus wysyła całe okno) — licz osobno sloty okna i sloty ekwipunku gracza. Popraw oba.
- Twoje niewykonane przebiegi z przerwy leżą w `kolejka/wstrzymane/` (FZ4–FZ6 i jeden wcześniejszy) — zleć je od nowa
  z nowymi nazwami. Wyniki `b-3-01`…`b-3-10` w `kolejka/` są ważne (ten sam build A), wykorzystaj je.
- Wydajność: zapisz load przy każdym przebiegu; pomiary tylko przy load < 8.
Zakres reszty bez zmian (`POLECENIE-B-r3.md` + `POLECENIE-B-r3c.md`). Raport `krytyk/raport-r3.md`, -o `koniec-r3.txt`. STOP.
