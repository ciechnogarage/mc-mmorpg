# Plugin Mapa v5 — etap 1: pełna mapa z menu + znaczniki na minimapie (dodatek do SPEC..SPEC-v4)

## 1. Wejście do mapy
- PPM mapą w ręce (bez Shifta) otwiera teraz MAPĘ NA CAŁY EKRAN (dotąd atlas). Shift+PPM = zoom bez zmian.
- `/mapa otworz <gracz>` nadal otwiera atlas (używa go CoreTools: klik w slot 7 w ekwipunku).
- Na górnym pasku pełnej mapy, przed „✕”, dodaj przycisk tekstowy „Atlas” (szer. ~40 px): LPM -> zamknij pełną
  mapę i otwórz atlas na tym samym świecie i środku widoku. `PelnaMath.przycisk` zwraca dla niego -3 (test JUnit).
- Jeśli MapEngine niedostępny — PPM otwiera atlas jak dotąd.

## 2. Znaczniki na minimapie NMinimap (softdepend: NMinimap)
- Zależność kompilacji: `compileOnly(files("libs/NMinimap-1.0.9.jar"))` (plik już leży w `libs/`).
- Klasy NMinimap używaj TYLKO w osobnej klasie `MinimapaZnaczniki implements Listener`, rejestrowanej w onEnable
  wyłącznie gdy `Bukkit.getPluginManager().isPluginEnabled("NMinimap")` (inaczej NoClassDefFoundError).
- `@EventHandler onMarkers(su.nezushin.nminimap.api.events.AsyncMarkerRenderEvent e)` (wątek asynchroniczny!):
  gracz = `e.getPlayer().getPlayer()`; dla każdego znacznika `plugin.data().markers(świat gracza)` z niepustą
  ikoną, w promieniu `minimapa.promien` bloków (config, domyślnie 256) od gracza:
  `e.getMarkers().add(new su.nezushin.nminimap.markers.impl.LocationMarker(ikona, new Location(świat, x+0.5, y, z+0.5)))`
  (y = wysokość gracza). Tylko odczyt niezmiennych danych — bez wywołań Bukkit API wymagających wątku głównego
  poza `getWorld()/getLocation()` gracza (te są bezpieczne do odczytu).
- Nazwa ikony = pole `ikona` znacznika (te same pliki PNG co `plugins/Mapa/ikony`, generator kopiuje je też do
  `plugins/NMinimap/markers/`).

## Config
```yaml
minimapa:
  promien: 256
```
