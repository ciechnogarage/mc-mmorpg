# Wdrożenie Bagien na world1 — pakiet (NIE uruchamiać bez zgody właściciela)

Zakres (właściciel 03.10): Pijawka, Mułowiec, Kościej, Szeptucha, Kokoniara + Kokoniątko (tylko Wylęg). Rechotnik, Ślimak,
Ognik, Król Ropuch ODŁOŻONE — definicje w `output/lochy/bagna/mm/odlozone/`, modele zostają w `modele/`, nie wchodzą do pakietu.

Co wchodzi (bez restartu, same reloady):
- `plugins/ModelEngine/blueprints/bagna/bagna_<id>.bbmodel` ×6 — kopie z `output/lochy/bagna/modele/` przepuszczone przez
  `klon/normalizuj_bbmodel.py` (animator skryptów pod kluczem `effects`).
- `plugins/MythicMobs/mobs/Bagna_mobs.yml`, `skills/Bagna_skills.yml` — z `output/lochy/bagna/mm/` (NIGDY `arena_baza_widoczna/`).
- `plugins/Nexo/pack/assets/bagna/` (.ogg z LittleRoom tylko dla aktywnych mobów) + wpisy `bagna:*` w `plugins/Nexo/sounds.yml` (między znacznikami `# BAGNA`).
- `plugins/PaskiMobow/config.yml`: `wzorce += 'BAGNA_*'`, `pomin += 'BAGNA_P_*','BAGNA_T_*','BAGNA_KOKON'`, poziomy/rzadkości w `moby:`
  (PaskiMobow rysuje pasek tylko dla frakcji z listy albo id z `wzorce`; niewidzialny mob = pusty pasek).

Tylko DEFINICJE: Loch 002 i wyspa Mokradeł nie istnieją na world1 — nic się samo nie pojawia. Spawn ręczny do oglądu:
`mm mobs spawn BAGNA_RECHOTNIK 1 world,x,y,z`.

## Użycie
```
tools/bagna/wdrozenie/wdroz.sh              # na sucho: bramki + lista kroków
tools/bagna/wdrozenie/wdroz.sh --wykonaj    # czeka na 0 graczy (CZEKAJ_NA_ZERO=1), kopia w backups/bagna-<data>
tools/bagna/wdrozenie/wycofaj.sh --wykonaj  # z backups/... (ostatnia_kopia.txt): usuwa nowe ścieżki, przywraca 2 configi, reloady
```
Kolejność reloadów: `meg reload` → czekaj na „ModelEngine ResourcePack is ready” → `nexo reload all` (dopiero to składa
sounds.json i pack z modelami; `nexo reload pack` nie czyta sounds.yml) → `mm reload` → `paskimobow przeladuj`.
Nexo wysyła graczom nowy pack — dlatego czekamy na 0 graczy.

## Kontrola po wdrożeniu (skrypt robi sam)
- `klon/log.py` porównuje rodzaje WARN/ERROR z logiem sprzed wdrożenia → `backups/bagna-*/kontrola.txt` ma mieć „NOWE: 0”.
- `pack.zip` zawiera `assets/modelengine/models/bagna_*`  i `assets/bagna/sounds.json`.
- Ręcznie: spawn jednego moba, oko na pasek z nazwą, `mm debug` czysty.

## Test na klonie (03.10, świeży klon z world1)
`rm -rf /opt/docker/mc/bagna-test; klon/przygotuj.sh; klon/start.sh` →
`CEL=/opt/docker/mc/bagna-test/data KONTENER=bagna-test BK=/opt/docker/mc/bagna-test/kopia-wdrozenia wdroz.sh --wykonaj`
→ NOWE WARN/ERROR: 0, 173 pliki modeli, sounds.json jest; `wycofaj.sh --wykonaj` → NOWE: 0, PaskiMobow/config.yml i
Nexo/sounds.yml identyczne z world1, brak plików Bagien.
Uwaga na świeżym serwerze: po `meg reload` Nexo loguje tylko „ResourcePack is ready” (import robi `nexo reload all`).

## Ryzyka
- Ciosy gracza w hitbox modelu ME: bot mineflayer trafiał tylko raz na kilka sekund (ME `interaction`). Przed otwarciem
  lochu sprawdzić żywym graczem, że każdy cios wchodzi.
- Kościej: strzał na flagę świata `world.bagna_kos_strzal` — dwa Kościeje strzelające w tej samej chwili trafią oba punkty.
