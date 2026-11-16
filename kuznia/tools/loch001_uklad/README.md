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
Argumenty bota: `<styl> [trasa względem katalogu skryptu, domyślnie wynik/trasa.json] [dolacz]`.
- Druga kopia lochu (dwie drużyny naraz): `L1R_OFFSET=400,200 python3 blockout_l001.py --out <katalog>`, potem bot z `<katalog>/trasa.json`.
- Drugi bot tej samej drużyny: trzeci argument `dolacz` — nie ustawia sceny, czeka na wrogów pierwszego bota.
- Sprawdzenie `wrogowie` = udane nadanie tagu każdemu wrogowi przy ustawianiu. Liczba po 1,5 s idzie tylko do `POMIAR wrogowie_po_1_5s`, bo pąk sam dojrzewa po 5 s i znika (16.11: 21/22 w obu kopiach przy obciążonym hoście).
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
- MSPT (co 15 s): średnia 2,2–2,6 ms, encje w lochu ≤42. Próg „lagu” = 50 ms (20 TPS); pojedyncze ticki >50 ms rozbite niżej.
- Kotwa (warunek ≤2 Pniaczki w 10 bl., gracz <16 bl.): najwięcej 3 Pniaczki przy Kotwie, 10 husków w całym lochu.
- **Bramka 12–14 min NIE jest spełniona**: bot 4,7–6 min. Brakuje bossa (~2 min), detali i łupu; bot zna trasę i nie rozgląda się. To decyzja projektowa (więcej treści / dłuższe sale albo inna bramka), nie błąd blockoutu.

## Obciążenie i lagi (klon l001-blockout, 16.11, wieczór)
Pomiar: `mspt` przez RCON co ~5 s (okno 10 s, linia 2), `spark tickmonitor --threshold-tick 55` (odstęp między początkami ticków; 50 ms = normalny rytm), spark profiler `--only-ticks-over`.
| Przebieg | Wynik bota | MSPT średnio | Najdłuższy tick (okno 10 s) |
|---|---|---|---|
| A2 pusto, rozgrzany | — | 0,34 ms | 17,3 ms |
| B 1 bot | PASS 5/5, 5,7 min | 2,9 ms | 399 ms (wejście bota) |
| C drużyna 2 boty | PASS / FAIL 4/5 (tagi w `dolacz`, poprawione) | 3,3 ms | 77 ms |
| D dwie kopie po 1 bocie | PASS 5/5 + PASS 5/5 | 3,6 ms | 139 ms |
| D2 powtórka D, z profilerem | FAIL 4/5 + 4/7 (tagi; 2× miecz po zgonie w kopii 2 z resztkami testu A/B) | 6,5 ms* | 208 ms* |

\* D2 z włączonym profilerem i przy większym obciążeniu hosta (PSI cpu ~25%), nieporównywalne z D.

**19 odstępów >75 ms (B, C, D, A/B) według zdarzenia w tej samej sekundzie (parser logu):** GC ≥15 ms w tym ticku — 9; wejście bota — 4 (do 304 ms); masowe `kill` 25 Pniaczków naraz — 2 (82–89 ms, ok. 1,4 ms na śmierć z przywołaniem pąka); bez wyjaśnienia — 4 (96–139 ms).
- Profil długich ticków D2 (`--only-ticks-over 60`): 84% czasu wątku serwera w JVM (safepoint, GC, wolna alokacja G1), kod lochu i pluginów ≤6%.
- **Pniaczek nie jest potwierdzoną przyczyną**: 3 z ok. 125 śmierci z ręki bota przy odstępie >75 ms ≈ przypadek (~1,4% sekund); 30 śmierci bez gracza (`damage`) — żaden odstęp >63 ms. Ścieżki „zabił gracz” w izolacji nie mierzono.
- Klon a produkcja: heap ten sam (2G/1G), różnią się flagi GC (klon domyślnie G1 `MaxGCPauseMillis` 200; world1 Aikar + `MaxGCPauseMillis=50`) i `--cpus 2`. Klon z flagami world1: `JVM_JAK_PROD=1 klon.sh start <n>`.
- Wejście do instancji MD `loch_001` (obecna mapa world1, bot bez OP, 3×): tick 305 ms + 166 ms (zimne), potem 134 ms, 67 ms — dotyczy każdego wejścia do lochu, całego serwera.
- Alokacja ~11 MB/s przy 2 botach (GC co ~10 s). Poza lochem: `zaklecia` `Nosnik` co tick woła `getItemMeta()` (kopia metadanych) dla 9 slotów + obu rąk × gracz (`Nosnik.java:88,93`, gałąź `pasek-eq-naprawa`) — poprawka `ItemStack#getPersistentDataContainer()` bez kopii, do wpięcia po tamtej sesji; `SzybkiParser` (hud-dane) to już optymalizacja MythicHUD, jego alokacji nie zmierzono.
- Klient QA (prawdziwy klient, sam klawisz W): schody J→R (9 stopni, y 29→38) w ~11 s, 0 „moved wrongly”.

## Flagi JVM i tryb jednego świata MD (klon l001-jvm, 16.11, noc)
Ten sam scenariusz D (2 kopie × 1 bot, oba PASS 5/5 w A i B) + 3 wejścia do instancji `loch_001`; liczone bez 30 s wejść botów, po 1 przebiegu na fazę.
| Faza | JVM | Odstępy >75 ms | >100 ms | Najdłuższy | Pauzy GC | Wejście do instancji (max ticku: 1. / 2. / 3.) |
|---|---|---|---|---|---|---|
| A | domyślny G1 klona | 2,0/min | 0,9/min | 155 ms | 23/min, śr. 17 ms | 197 / 100 / 63 ms |
| B | jak world1 (Aikar + `MaxGCPauseMillis=50`) | 1,0/min | 0,4/min | 111 ms | 27/min, śr. 13 ms | 207 / 65 / 68 ms |
| C | jak B + `OneWorldDungeons: true` | — | — | — | — | 948 / 201 / 324 ms |

- Flagi world1 mniej więcej o połowę zmniejszają lagi od GC, ale ich nie usuwają; koszt wejścia do instancji od flag nie zależy.
- `OneWorldDungeons` gorszy, a bez dopełnienia `maps/loch_001/region/r.-1.1.mca` do 4096 B (209256 B, też na world1) wejście w ogóle nie działa (`EOFException`, `Region.java:89`). Nie wdrażać.
- PSI CPU hosta w czasie pomiaru 9–12% (inne klony) — szum; różnica A/B to 1 przebieg, nie pewnik.
