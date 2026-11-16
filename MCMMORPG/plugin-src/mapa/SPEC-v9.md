# Plugin Mapa v9 — SKALOWANIE MAPY W RĘCE (poprzednie SPEC obowiązują)

## Zmierzone (spark, 20 botów chodzących z mapą, klon)
`HandMapRenderer.drawTerrain` = 16 384 × `CraftMapCanvas.setPixel` co 250 ms na gracza. Paper w `setPixel` woła
`MapItemSavedData.setColorsDirty(x,y)`, które oznacza piksel u WSZYSTKICH graczy mających TĘ SAMĄ mapę w ekwipunku
(mapa 30000 jest wspólna) -> koszt O(N²) i każdy gracz dostaje pakiety obszarów zmienionych przez innych.

## 1. Własna mapa (MapView) dla każdego gracza
- Rejestr `plugins/Mapa/mapy-graczy.yml`: `<uuid gracza>: <map id>`. Brak wpisu -> `Bukkit.createMap(świat gracza)`,
  zapisz id. Przy starcie/przeładowaniu: dla każdego zapisanego id `Bukkit.getMap(id)` (null -> utwórz nową i nadpisz).
- Każdy taki MapView: usuń wszystkie renderery, dodaj NASZ renderer (jedna instancja może obsługiwać wiele MapView;
  może zostać contextual), `setTrackingPosition(false)`, `setUnlimitedTracking(false)`, `setLocked(true)`.
- „Nasza mapa” = FILLED_MAP z `MAP_ID` == 30000 (stara wspólna, nadal wstawiana przez CoreTools) ALBO == osobistemu
  id tego gracza. Szybki test (komponent `DataComponentTypes.MAP_ID`, bez `getItemMeta`) zostaje.
- Podmiana: przy wejściu (2 s po join), po odrodzeniu, przy zmianie slotu (PlayerItemHeldEvent) i przy
  InventoryClickEvent dotyczącym mapy: każdy przedmiot w ekwipunku gracza z MAP_ID == 30000 dostaje
  `setData(MAP_ID, MapId.mapId(osobiste))` (TYLKO komponent; reszta przedmiotu — nazwa, lore, dane MMOItems — bez zmian).
- Renderer rysuje tylko dla gracza, do którego należy dana mapa (render(view,…,player): jeśli
  view.getId() != osobiste id tego gracza -> return). Mapa 30000 nie ma już renderera rysującego teren —
  zostaw jej renderer pusty (return), żeby nie było kosztu.

## 2. Mapa „stoi”, strzałka chodzi (jak mapa vanilli)
- Środek terenu (cx, cz) zmienia się TYLKO gdy: zmiana świata/zoomu, pierwszy rysunek, albo gracz odszedł od środka
  o więcej niż 40 pikseli mapy w osi x lub z (piksel = zoom bloków; przy zoom < 1 piksel = 1/k bloku). Wtedy nowy
  środek = pozycja gracza, pełne przerysowanie (setPixel tylko dla zmienionych pikseli — porównaj z poprzednim
  buforem gracza trzymanym w stanie; setPixel nic nie robi przy tej samej wartości, ale porównanie u nas jest
  tańsze niż wywołanie).
- Kursor gracza: pozycja względem środka `(x - cx) * 2 / zoom` (zakres -128..127), kierunek jak dotąd.
  Kursory znaczników liczone względem (cx, cz). Pamięć kursorów z v8 (klucz + kierunek) — klucz uwzględnia też
  pozycję kursora gracza (w pikselach mapy).
- `odswiezanie-ms` zostaje jako minimalny odstęp przerysowań.

## 3. Bez zmian
Zoom (PPM), Shift+PPM pełny ekran, dane terenu, wszystko inne.

## Testy JUnit
decyzja o przerysowaniu (40 px przy zoom 0.25/1/4), pozycja kursora gracza względem środka (krawędzie, ujemne),
rejestr map: wczytanie/zapis YAML w pamięci.
