# Iris imagemap — przepis tworzenia poziomu dungeonu

Zweryfikowane botem (mineflayer) 2026-06-21: pack `empty` generuje maskę stref
1:1 wg obrazu — granice START/MID na Z≈−50, MID/END na Z≈150 (centered, 1px=1blok).

## Jak działa (skrót)
- `dimensions/empty.json` → `regionStyle.imageMap` czyta kanał **BLUE** obrazu
  `images/Example.png` (200×500), `interpolationMethod: NONE` (ostre krawędzie),
  `centered: true` (środek obrazu = świat 0,0), `tiled: false`.
- Wartość BLUE piksela → region → biom (1:1):
  | BLUE   | region/biom | blok wierzchni | strefa |
  |--------|-------------|----------------|--------|
  | `#000040` | dunstart | grass_block | start |
  | `#000080` | dunmid   | ice         | mid    |
  | `#0000C0` | dunend   | granite     | boss   |
  | poza obrazem | empty | deepslate_tiles | void/tło |
- Generator FLAT, powierzchnia Y≈58–60 (`generators/flat.json`, biomy w `biomes/`).

## Nowy poziom (1 dungeon = 1 level)
1. Skopiuj pack: `cp -r packs/empty packs/level_<n>`.
2. Zmień nazwę dimensionu w `packs/level_<n>/dimensions/empty.json` (pole `name`)
   ew. przenieś plik na `dimensions/level_<n>.json` (nazwa pliku = id dimensionu).
3. Namaluj `images/<obraz>.png` kanałem BLUE wg tabeli stref (1px=1blok).
   Tło (poza wyspą) zostaw bez wartości docelowych → trafi do `empty`.
   Podmień referencję `image` w dimensionie jeśli inna nazwa pliku.
4. Wygeneruj świat: w grze (op) lub konsoli `iris create level_<n> type=level_<n>`.
5. (Później) warianty seed: dodaj do biomów warstwy/obiekty losowane seedem →
   `iris create level_<n>_v2 type=level_<n> seed=<n>` da inne dekoracje, ten sam kształt.
6. Oddaj świat do MythicDungeons jako templatkę (instancja per party, limity, trudności).

## Walidacja botem
`_validation/validate.js` (mineflayer + RCON) — generuje świat, przelatuje footprint,
odczytuje bloki i rysuje mapę stref. Wymaga tymczasowo `online-mode=false` +
Nexo `dispatch.send_pre_join/mandatory: false` (inaczej bot wisi na resource packu).
Patrz `_validation/README` poniżej w repo.
