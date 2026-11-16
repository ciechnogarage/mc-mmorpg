# Kuźnia lochów

Generatory terenu i lochów (Iris, .nbt, MythicDungeons, MythicMobs) oraz ich wyniki. Do 21.10.2026 leżały
w `/opt/docker/mc/forge` i działały na osobnym kontenerze `mc-forge`. Teraz kuźnia to zwykły klon world1 z `klon.sh`.

- `tools/` – skrypty. Ścieżki i kontener biorą z `tools/kuznia_sciezki.py` (Python) i `tools/kuznia.sh` (bash):
  `KUZNIA` = nazwa klonu (domyślnie `kuznia`), czyli kontener `klon-$KUZNIA` i dane `/opt/docker/mc/klony/$KUZNIA/data`;
  `KUZNIA_LEVELS` = katalog wyników (domyślnie `kuznia/levels`).
- `levels/` – wyniki i źródła: mapy MD + pliki MM (`loch_001_md`, `loch_002_md`, `poziom_01`), spec/podglądy,
  `przeciwnicy/`, `review/`, `packi/` (definicje packów Irisa bez `objects`, np. `pustka_002`, podkład Lochu 002).

## Cykl pracy (Loch 002)
```
df -h /; free -g; /opt/docker/mc/narzedzia/klon.sh lista
/opt/docker/mc/narzedzia/klon.sh start kuznia --z-iris
kuznia/tools/przygotuj_klon.sh && /opt/docker/mc/narzedzia/klon.sh restart kuznia
python3 kuznia/tools/make_loch002.py          # .nbt -> klon, spec + podgląd -> levels
python3 kuznia/tools/piec002.py               # świat-pustka + kafelki, kontrola parserem; wypisuje SWIAT=...
python3 kuznia/tools/build_loch002_md.py <SWIAT>   # -> levels/loch_002_md
kuznia/tools/wgraj_klon.sh loch_002 && /opt/docker/mc/narzedzia/klon.sh restart kuznia
/opt/docker/mc/narzedzia/bot/run.sh kuznia kuznia/tools/sonda_l002/sonda.js   # mechaniki (tp z RCON)
/opt/docker/mc/narzedzia/bot/run.sh kuznia kuznia/tools/sonda_l002/trasa.js   # przejście pathfinderem
/opt/docker/mc/narzedzia/klon.sh usun kuznia
```
Wdrożenie na world1: `tools/wdrozenie_l002/wdroz.sh` (kopia w `backups/`, cofnięcie `wycofaj.sh`), restart world1 tylko za zgodą.

## Historia, nie uruchamiać
`e2e_poziom.js`, `seedbot.js`, `przeciwnicy/arena_bot.js` to boty starego harnessu `forge/gracz` (własne `node_modules`,
zakazane od 12.10). Przed użyciem trzeba je przepisać na `/opt/docker/mc/narzedzia/bot` (jak `sonda_l002/`).
Usunięte przy przenosinach: `client_enter.sh`, `shoot.sh` (własny klient, dziś `narzedzia/klient-qa/klient.sh`),
`sonda_l002/uruchom.sh` (dziś `narzedzia/bot/run.sh`). Kontener `mc-forge` usunięty 21.10.
