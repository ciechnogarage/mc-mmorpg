# Mapa (`plugin-src/mapa`)
Wersja w źródle: `${version}` w `src/main/resources/plugin.yml:3`, podstawiana z `-PmapaRev` (`build.gradle.kts:33,36-38`) -> jar `mapa-0.1.0-<rev>.jar`.
Jar na world1: `/opt/docker/mc/data/world1/plugins/mapa-0.1.0-202610030029.jar`. Zgodność źródła z jarem: zgodna (`plugin.yml` w jarze różni się tylko podstawioną wersją `0.1.0-202610030029`; `config.yml` jar = źródło = world1, `diff -q` bez różnic). Ostatni commit kodu `39cece2` (2026-10-03); późniejszy `2e4d9b5` (2026-10-21) zmienił tylko `build.sh`.
Opis stanu z dnia: 2026-10-25, commit `8c79ff8` (HEAD worktree `docs-pluginy`).

## Po co (co widzi gracz)
Mapa świata w ręce (przedmiot FILLED_MAP, własny MapView na gracza), pełnoekranowa mapa na MapEngine, atlas (okno z kafelkami i zakładkami światów, panel znaczników zamiast ekwipunku), znaczniki serwerowe i własne pinezki gracza oraz prowadzenie do celu (waypoint na pasku lokatora i napis na action barze). Minimapa to osobny plugin NMinimap, Mapa dokłada do niej ikony znaczników. Gracz nie wpisuje komend: PPM mapą = zmiana przybliżenia, Shift+PPM = pełna mapa (`MapaPlugin.java:407-431`).
Historia wersji v1-v12 i decyzje właściciela: `~/.claude/skills/mc-swiat-lochy/mapa-swiata.md:6-18,19-23`. Specyfikacje: `SPEC.md`, `SPEC-v3.md` ... `SPEC-v9.md`, recenzja `RECENZJA-codex-2026-10-02.md` (w katalogu pluginu; nie czytane w całości).

## Komendy i uprawnienia
Parser `plugin.yml`: 1 komenda `mapa`, 1 uprawnienie `mapa.admin` (default op), `plugin.yml:7-15`. Kod sprawdza uprawnienie sam (`MapaPlugin.java:548-551`), więc działa też z konsoli. Wszystkie wywołania w praktyce idą z konsoli/menu (nie sprawdzone: które menu wywołuje `mapa`).

| Komenda | Kto | Co robi | Źródło |
|---|---|---|---|
| `/mapa otworz <gracz>` | OP/konsola | otwiera graczowi atlas | `MapaPlugin.java:552-557` |
| `/mapa pelna <gracz>` | OP/konsola | otwiera pełną mapę w pozycji gracza (wymaga MapEngine) | `MapaPlugin.java:558-564`, `:598-610` |
| `/mapa cel <gracz> <swiat> <x> <z> <nazwa...>` | OP/konsola | ustawia cel prowadzenia (świat musi pasować do `[a-z0-9_.-]+`) | `MapaPlugin.java:565-582` |
| `/mapa cel <gracz> wyczysc` | OP/konsola | wyłącza prowadzenie | `MapaPlugin.java:568-572` |
| `/mapa przeladuj` | OP/konsola | zamyka mapy i atlasy, `loadAll()` (config, kafle, tekstury, rejestr map), podmiana map graczy | `MapaPlugin.java:584-594` |

Rozbieżności: brak (`usage` w `plugin.yml:10` i tekst `MapaPlugin.java:595` wymieniają te same 5 form). Tab-complete: brak (`grep onTabComplete|TabCompleter` w `src/main` bez trafień).

## Konfiguracja
Plik: `/opt/docker/mc/data/world1/plugins/Mapa/config.yml` (identyczny ze źródłem `src/main/resources/config.yml`). Odczyt: `MapaPlugin.loadAll()` `:222-262`. 12 kluczy najwyższego poziomu (parser), wszystkie użyte w kodzie:

| Klucz | Znaczenie, wartość na world1 | Domyślna w kodzie | Źródło |
|---|---|---|---|
| `mapa-id` | wspólny numer map-id przedmiotu "starego" (zamieniany na osobisty): `30000` | 30000 | `MapaPlugin.java:225` |
| `zoom-reka` | dozwolone przybliżenia mapy w ręce: `[0.25, 0.5, 1, 2, 4, 8]` (filtr: >=1 lub 0.5/0.25) | te same | `MapaPlugin.java:226-230` |
| `zoom-reka-domyslny` | `1` (gdy nie na liście: pierwszy z listy) | 1 | `MapaPlugin.java:231-232` |
| `zoom-reka-swiat` | zoom na świat: `stolica_odnowa: 0.5`, `loch_001: 0.5` | domyślny | `MapaPlugin.java:206-207,215-216` |
| `odswiezanie-ms` | odstęp odświeżania renderera mapy w ręce: `250` | 250 | `MapaPlugin.java:233` |
| `pelna` | pełna mapa: `szerokosc 4` (1-12), `wysokosc 3` (1-8), `odleglosc 3.0` (1.0-5.0) | 4 / 3 / 2.2 | `PelnaMapa.java:97-99` |
| `minimapa.promien` | promień (bloki) ikon na minimapie NMinimap: `256` | 256 | `MinimapaZnaczniki.java:21` |
| `wlasne` | `limit` 30 pinezek na postać; `ikony` (10 kluczy -> nazwa w dialogu) | limit 30 | `ZnacznikDialog.java:62,73,123,133`, `PelnaMapa.java:75` |
| `tytul-okna` | glif tytułu atlasu (`䅐`, Nexo) | pusty | `MapaPlugin.java:637` |
| `nazwy-swiatow` | nazwy w UI: world=Kraina, stolica_odnowa=Stolica, loch_001=Loch 001 · Kwietna Polana | nazwa świata | `MapaPlugin.java:683-684` |
| `zakladki` | zakładki atlasu (świat, nazwa, ikona): 3 sztuki | brak | `MapaData.java:108` |
| `znaczniki` | znaczniki serwerowe (świat, x, z, nazwa, typ, ikona, opis) | brak | `MapaData.java:116` |

Parser: `znaczniki` 21 wpisów, `zakladki` 3, `wlasne.ikony` 10. Dodatkowo plugin czyta `plugins/NMinimap/config.yml` klucz `map-id`, żeby nie dać graczowi mapy o tym samym numerze (`MapaPlugin.java:240-243`; pułapka "mapa znika": `mapa-swiata.md:15`).

## Jak działa
- `onEnable` (`MapaPlugin.java:106-138`): config, własne znaczniki, `loadAll()`; PelnaMapa tylko gdy MapEngine aktywny (inaczej ostrzeżenie), panel atlasu tylko z packetevents, `MinimapaZnaczniki` tylko z NMinimap (`:110-126`).
- Zadania okresowe: `refreshPlayerTiles` co 20 ticków (kafel gracza w otwartym atlasie, `:134,927-938`); `refreshOwnerKeys` co 20 ticków (klucz postaci MMOProfiles, `:137-143`); `Prowadzenie.tick` co 10 ticków (`Prowadzenie.java:23,79`); `PelnaMapa.tick` co 1 tick (`PelnaMapa.java:67`), przy braku sesji pełnej mapy kończy się od razu (`PelnaMapa.java:255`).
- Zdarzenia (`MapaPlugin.java`): Join (+40 ticków opis i podmiana map) `:366-373`, Respawn `:375`, ItemHeld `:380`, Interact (PPM mapą) `:407-431`, Join (klucz) `:433`, Quit (sprzątanie map stanu) `:436-448`, InventoryClick/Drag/Close atlasu `:450-545`. Brak priorytetów ustawionych jawnie (domyślne NORMAL; nie sprawdzone w pełni).
- Dane map: kafle atlasu z `kafle.json` (poziomy bpp/kol/wier na świat), kolory z `swiaty/<swiat>.bin|.blk|.json`, tekstury `tekstury.bin` (`MapaData.java:129-194`). Generator poza pluginem (spec: `SPEC.md`).
- Prowadzenie: waypoint packetevents (`WaypointMost.java:16-32`) + napis kierunek/odległość; cel osiągnięty <= 8 bloków (`Prowadzenie.java:100-103`).
- Własne pinezki: dialog Paper (`ZnacznikDialog`), zapis asynchroniczny (`WlasneZnaczniki.java:103-118`).
- Integracje: MapEngine (pełna mapa), NMinimap (ikony `AsyncMarkerRenderEvent`, `MinimapaZnaczniki.java:25-47`), MMOProfiles (`ProfilMost.java:11-18`), MMOCore (action bar przez `MMOCoreMost`), packetevents (panel i waypointy), MythicLib (priorytety paska: commit `39cece2`).

## Koszt dla serwera
Stałe: dwa timery co 20 ticków (pętla po graczach online / atlasach), jeden co 10 ticków (tylko gracze z celem), jeden co tick w PelnaMapa. Renderer mapy w ręce odświeża co `odswiezanie-ms` (250 ms) per gracz z mapą (`HandMapRenderer.java:82`, `MapaPlugin.java:219`; klasy nie czytałem w całości). Pamięć: `swiaty/*.bin|.blk` ~10,5 MB na dysku, ładowane przy starcie (`MapaData.java:152-194`). Zapis na dysk: pinezki asynchronicznym wątkiem `Mapa-zapis-znacznikow` (`WlasneZnaczniki.java:29-33`), `mapy-graczy.yml` przez plik tymczasowy (`MapaPlugin.java:316`). Pomiar MSPT/100 graczy: nie mierzone w tym opisie (wymóg właściciela: `mapa-swiata.md:23`).

## Dane
- `plugins/Mapa/gracze/<uuid>.yml` (4 pliki w dniu opisu): własne pinezki, klucz = UUID PROFILU postaci z MMOProfiles (`ProfilMost.key`, `WlasneZnaczniki.java:41-50`), a bez MMOProfiles UUID gracza. Znaczy: pinezki są per postać (to jest poprawne wobec reguły "MMOProfiles daje jeden UUID").
- `plugins/Mapa/mapy-graczy.yml`: `UUID gracza -> numer map-id` (`MapyGraczy.java:20`); tu klucz = UUID konta, nie postaci (osobny MapView na konto).
- `kafle.json`, `swiaty/` (3 światy: world, stolica_odnowa, loch_001), `tekstury.bin`, `ikony/*.png` (ikony pełnej mapy): dane wejściowe generatora, tylko odczyt (`MapaData.java`, `PelnaMapa.java:72-84`).

## Znane pułapki i decyzje
- Pułapki zmierzone (MapPalette 248 kolorów, ramki MapEngine, oversized_in_gui): `~/.claude/skills/mc-swiat-lochy/mapa-swiata.md:19-27`.
- "Mapa znika": numer map-id NMinimap = pierwsza osobista mapa (v10): `mapa-swiata.md:15`; w kodzie `reserved`, `MapaPlugin.java:65-67,240-243`.
- Podejrzane w kodzie (nie poprawiane): `pelna.odleglosc` na world1 = 3.0, domyślna w kodzie 2.2 (`PelnaMapa.java:99`, świadoma zmiana nieudokumentowana); plugin.yml `description` mówi "Otwiera lub przeładowuje mapę" a komenda robi też cele; nie sprawdzono, kto poza menu daje graczowi przedmiot mapy.

## Budowanie i wdrożenie
`./build.sh` w katalogu pluginu (Gradle w kontenerze `build-mapa-$$`, `--memory 2g`; zależności przypięte w `build.gradle.kts`: paper-api 1.21.11, mapengine-api 1.8.12, MMOProfiles, MythicLib, MMOCore, packetevents; `libs/` NMinimap-1.0.9, AnvilORM-1.0.2). Testy jednostkowe JUnit 5 w `src/test/java/pl/mcmmorpg/mapa/` (7 plików: MapaData, MapaMath, MapyGraczy, PanelMath, PelnaMath, Prowadzenie, WlasneZnaczniki). Wdrożenia v1-v12 robiły skrypty z `backups/mapa*/cofnij.sh` (`mapa-swiata.md:6-18`).
