# MC Asset Dataset Pipeline — design spec

## Cel

Zbudować powtarzalny, offline’owy pipeline do analizy 542 źródłowych plików `.bbmodel`, selekcji pilotażowej rodziny stworzeń, renderowania referencyjnego przez Blockbench MCP, generowania deterministycznych captionów oraz przygotowania datasetu pod przyszłe adaptery Minecraft/Blockbench/ModelEngine/MythicMobs.

## Zakres i granice

- Źródło `/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)` jest tylko odczytywane.
- Pilotaż wybiera 100–150 unikalnych modeli, preferując organiczne creature/moby i odrzucając NPC, propsy, itemy, particles, GUI, efekty, duplikaty i uszkodzone pliki.
- Modele bez potwierdzonego provenance mogą wejść do analizy/datasetu, ale dostają `NO_TRAINING`/`training_allowed: false`.
- Renderowanie odbywa się przez Blockbench MCP/CDP; brak aktywnego endpointu kończy zadanie `BLOCKED_TOOLING`, bez pustych obrazów.
- Nie ma deploymentu, treningu ani zapisu do aktywnych katalogów pluginów.

## Architektura

Pipeline jest Node.js/CommonJS, zgodny z istniejącym repozytorium (`Puppeteer`, `canvas`, istniejący worker MCP). Komendy:

`scan → select → render → validate`

Moduły:

- parser/profil `.bbmodel`;
- selektor i raport odrzuceń;
- grupowanie rodzin i deterministyczny split;
- renderer Blockbench MCP z capability discovery, journalem i `--resume`;
- caption generator;
- walidatory obrazów, metadanych, schema, ID, animacji, ModelPart i referencji;
- JSON Schema, fixtures, `dataset.toml`, README i raporty JSONL.

## Renderowanie

POC wymaga 3–5 modeli i sześciu widoków: `front`, `back`, `left`, `right`, `isometric_front`, `isometric_back`. Kamera, światło, tło i rozdzielczość 1024×1024 są stałe; skala/odległość jest dopasowana do bounding boxa. Render nie może zawierać GUI, osi, gizmo, siatki, nazw, zaznaczeń, watermarków ani debug overlayów. Każdy wynik ma manifest, hash wejścia i status wznowienia.

## Dane i podział

Dataset zawiera obrazy z parą `.txt`, profile modeli, `selection-report.json`, `rejected-models.jsonl`, `families.json`, `split-manifest.json`, `render-report.json`, `dataset.toml` i README. Rodziny nie przeciekają między splitami; seed domyślny to 42, a balans uwzględnia liczbę modeli. Caption zaczyna się od `mcmmoasset` i jest generowany regułowo, deterministycznie.

## Kontrakty przyszłych adapterów

Metadane zachowują rozdział między profilem źródłowym a przyszłym `mc_asset_spec`, `mc_mythic_designer`, `mc_config_repair` i `mc_content_qa`. Wersje Paper/pluginów pozostają `unknown`, jeśli nie wynikają bezpośrednio ze źródła. AssetSpec jest centralnym neutralnym kontraktem, a generatory wersjozależnego YAML pozostają późniejszą warstwą.

## Bramy jakości

POC musi przejść: poprawny odczyt profili, selekcję i raport, sześć rzeczywistych renderów na model, 1024×1024, niepuste obrazy, margines, brak GUI, pary captionów, schema validation, deterministyczność i brak przecieku rodzin. Dopiero po tym pipeline może uruchomić pilotaż 100–150 modeli.

## Awaria i wznowienie

Każdy model/widok ma idempotentny rekord stanu. `BLOCKED_TOOLING`, timeout, błąd walidacji lub brak widoku są zapisywane z przyczyną. `--resume` pomija wyłącznie poprawne artefakty o zgodnym hash’u wejścia i konfiguracji. Nigdy nie tworzymy fałszywych placeholderów.

