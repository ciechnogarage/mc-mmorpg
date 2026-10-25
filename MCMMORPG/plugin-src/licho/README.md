# Licho — minigra „Licho nie śpi” (prop hunt)

**WDROŻONE na world1 25.10 01:24 UTC** z gałęzi `licho` (f3b986f), jar `licho-0.1.0-202610250042.jar`
md5 `25c0c63ed05f7a98fe0b5bfa9b92e342`. Cofnięcie: `/opt/docker/mc/data/world1/backups/licho-20261025-012313/cofnij.sh`.

Projekt gry (role, punkty, tytuły, koszt serwera): `PROJEKT.md`. Ten plik opisuje plugin.

## Wejście gracza
Q → Drużyna (slot 38) → kafel LICHO (25) → menu `licho` (DOŁĄCZ slot 5, Rekordy 25/26/34/35). Gracz nie wpisuje komend.
Menu i kafel: `wdrozenie/menu/` (DeluxeMenus), wpinane przez `wdrozenie/latka.py`.

## Konfiguracja (`plugins/Licho/config.yml`, komentarze w pliku)
- `swiat` (licho, VoidGen), `swiaty-wejscia`, `swiaty-awaryjne` — skąd wolno dołączyć i dokąd odesłać.
- `gracze.min/max/na-zamawiacza`, `czasy.*` (poczekalnia, rozproszenie 30, łowy 180, podsumowanie 8 s).
- `licho.*`, `zamawiacz.*`, `punkty.*` — zasady rundy; `przebrania` — sprzęty, w które wciela się Licho.
- `tytuly.min-graczy` (4) i progi `tytuly.licho` / `tytuly.zamawiacz` (liczba → nazwa). Tytuł wynika z progów,
  więc zmiana progów działa wstecz. Liczą się tylko rundy dokończone przy co najmniej `min-graczy` graczach.
- Po zmianie: `licho przeladuj` (na klonie; na world1 restart lub to samo z konsoli przy 0 graczach).

## Dane
`plugins/Licho/rekordy.yml`: `gracze.<uuid>` → `nick`, `najlepszy`, `data`, `rundy`, `przetrwane`,
`tytul-przetrwane`, `tytul-zlapani` (te dwa liczą tylko rundy do tytułów).

## Komendy (konsola, `licho.console` = OP)
`licho dolacz|wyjdz <gracz>` (menu woła je przez konsolę), `rekordy [gracz]`, `zbuduj` (Zagroda z `mapa.txt`),
`stan` (faza, gracze, forceload, encje, progi tytułów), `start [s]`, `obserwuj <gracz>`, `przeladuj`.

## PlaceholderAPI (softdepend)
`%licho_tytul%`, `%licho_postep_licho%`, `%licho_postep_zamawiacz%` („Domowik (2/5)”), `%licho_min_graczy%`.

## Budowa, testy, wdrożenie
- `bash build.sh` → `build/libs/` (ma być jeden jar).
- Klon: `wdrozenie/klon-przygotuj.sh <klon>` (pełne), `wdrozenie/klon-jar.sh <klon>` (podmiana jara + restart).
- Boty: `narzedzia/bot/run.sh <klon> $PWD/testy/<scenariusz>.js` — `runda.js`, `nieidealne.js` (+ `nieidealne-c.js`), `tytuly.js`.
- World1: `wdrozenie/wdroz.sh --sucho`, potem `wdrozenie/wdroz.sh` (tylko przy 0 graczach; kopia świata, restart, budowa Zagrody).
