# Test zaklęć z perspektywy gracza

Ten katalog zawiera klon world1, harness Mineflayer, wyniki kolejki oraz zrzuty prawdziwego klienta. Testy uruchamiaj na klonie `zaklecia-gracz`; skrypty nie są przeznaczone do produkcji.

## Klon i scena

`./klon.sh` uruchamia klon Docker z danymi w `data/`. Przy pierwszym uruchomieniu kopiuje konfigurację pluginów. Czekaj na `Done` w `data/logs/latest.log`. `./klon.sh --fresh` usuwa dotychczasowy klon i jego dane testowe, po czym tworzy je od nowa. Do przygotowania płaskiej areny służy `bot/przygotuj.sh`. Zwykłe scenariusze ustawiają arenę samodzielnie przez RCON.

## Bot i kolejka

`bot/run.sh [A-F]` uruchamia grupy A–F; argument jest filtrem liter, np. `bot/run.sh AC`. Wyniki powstają w `bot/wyniki/`, a konsola wypisuje `PASS`, `FAIL`, `BLAD`, `SKIP` i metryki `FEEL`. Bot `MagTest` gra bez OP. RCON służy do przygotowania sceny oraz odczytu stanu serwera.

Gdy Docker jest niedostępny w bieżącym środowisku, utwórz plik `kolejka/<unikalna-nazwa>.req` zawierający litery grup (pusty plik oznacza wszystkie). Działający `kolejka/runner.sh` pobiera pliki po kolei i zapisuje `<nazwa>.out`, `<nazwa>.serwer.log` oraz `<nazwa>.done` z kodem wyjścia. Kod `1` oznacza, że wystąpił `FAIL`; liczbę `BLAD` sprawdź w podsumowaniach `JSON` pliku `.out`. `kolejka/runner.log` pokazuje start i koniec przebiegów. Jeden przebieg ma limit 25 minut.

**Nie uruchamiaj scenariusza przywołaniec + drugi gracz.** Znana rekurencja `pomocSlug` może zatrzymać serwer. Harness oznacza PvP jako `SKIP`; przywołania testuje wyłącznie z huskami.

## Prawdziwy klient

`klient/start.sh` uruchamia klienta vanilla 1.21.11 w Xvfb na sieci klona. Po `source klient/k.sh` funkcja `X` wysyła polecenia `xdotool`, `Z <nazwa>` zapisuje zrzut do `klient/captures/`, a `R` odczytuje RCON klona. Sekwencje testu opisuje `klient-prawdziwy.md`.

Bot widzi pakiety, stan dłoni, HP, manę, encje i logi. Nie oceni obrazu w pierwszej osobie, czytelności menu i BossBara, zasłaniania ekranu przez efekty, lokalnej animacji dłoni ani toastów klienta. Te obserwacje pochodzą ze zrzutów w `klient/captures/` i są opisane w `OCENA.md`.
