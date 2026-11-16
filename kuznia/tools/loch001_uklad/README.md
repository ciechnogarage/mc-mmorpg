# Loch 001 (remake): układ, blockout, przejście botem

Kroki 3–4 remake'u (encyklopedia, zakładka „Loch 001: remake”).

| Plik | Co robi |
|---|---|
| `uklad_l001.py` | układ sal i korytarzy (rzut, kontrole: kolizje, ramka, długość trasy, odcinki bez walki) |
| `blockout_l001.py` | z układu: datapack `wynik/datapack/l1r_blockout` (fill/setblock, funkcja `l1r_bo:buduj`), `wynik/trasa.json` (trasa, sale, wrogowie, bramy, ogniska), model BFS przejścia |
| `przejscie_l001.js` | bot bez OP (BASTION poz. 2, kamienny miecz) idzie trasą, bije wrogów L1R_*, mierzy czasy sal, zgony, HP, bramy |

## Użycie (tylko klon)
```
python3 kuznia/tools/loch001_uklad/blockout_l001.py
cp -r kuznia/tools/loch001_uklad/wynik/datapack/l1r_blockout/. <klon>/data/world/datapacks/l1r_blockout/
rcon: minecraft:reload ; minecraft:function l1r_bo:buduj      # offset lokalne (0,0,0) -> świat (200,40,200)
LIMIT_MS=1800000 /opt/docker/mc/narzedzia/bot/run.sh <klon> kuznia/tools/loch001_uklad/przejscie_l001.js uwazny
```
Moby: `kuznia/levels/przeciwnicy/Przeciwnicy_L001.yml` + `_skills.yml` (generuje `kuznia/tools/przeciwnicy/przeciwnicy_l001.py`).

## Pułapki (16.11)
- **Każdy stopień +1 to `stone_stairs`** (`schody()` w blockoucie, 169 sztuk). Na pełnych blokach 1:1 serwer cofał bota co tick (80–370 cofnięć na 6 stopniach, `stepHeight` i sprint bez znaczenia); ze schodami 0. Gracz też nie musi skakać 9 razy z rzędu.
- **`bot.pvp.movements = bot.pathfinder.movements`** zaraz po `loadPlugin(pvp)`: mineflayer-pvp 1.3.2 przy każdym `attack()` wstawia własne domyślne Movements (kopanie, skosy). Bez tego bot po pierwszej walce stał (S3E 180 s, S8 2×120 s, przebieg #10).
- Test bloku przez RCON: `execute ... if block X Y Z minecraft:air` → „Test passed/failed” (`unless ... run say` przez RCON nic nie zwraca).

## Wynik kroku 4 (klon l001-blockout, 16.11, bot uważny bez OP)
| Przebieg | Wynik | Czas | Walka / marsz | Zgony | HP stracone | Odblokowania |
|---|---|---|---|---|---|---|
| #11 | PASS 5/5 | 6,0 min | 220 / 141 s | 1 (J→S5) | 110 | 0 |
| #12 | PASS 5/5 | 4,7 min | 207 / 76 s | 0 | 61 | 0 |

- 22/22 wrogów zabitych, 3 bramy otwarte (korzenie jeziorka, mur Bramy, krata O2).
- MSPT (co 15 s): średnia 2,2–2,6 ms, encje w lochu ≤42. Pojedynczy tick 94 ms w #11 tuż po zgonie bota (przyczyna niepotwierdzona); w #12 spark `--only-ticks-over 40` nie złapał żadnego ticku.
- Kotwa (warunek ≤2 Pniaczki w 10 bl., gracz <16 bl.): najwięcej 3 Pniaczki przy Kotwie, 10 husków w całym lochu.
- **Bramka 12–14 min NIE jest spełniona**: bot 4,7–6 min. Brakuje bossa (~2 min), detali i łupu; bot zna trasę i nie rozgląda się. To decyzja projektowa (więcej treści / dłuższe sale albo inna bramka), nie błąd blockoutu.
