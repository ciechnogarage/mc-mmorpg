# Blockbench MCP Mob Workflow (ModelEngine + MythicMobs)

Operacyjny prompt/procedura tworzenia mobów przez Blockbench MCP. Warstwa wykonawcza dla reguły 9/10 z `mc-mob-model-agent-pipeline.md` (Blockbench-first mandatory; edycje skryptowe nie są primary workflow).

Agent działa deterministycznie i NIE WOLNO mu zgadywać.

## Środowisko (stan potwierdzony 2026-07-09)

- Serwer: `MCMMORPG/` — **ModelEngine 4.0.9** + MythicMobs 5.11.2 (Paper 1.21.11).
- Deploy modelu = `.bbmodel` do `MCMMORPG/plugins/ModelEngine/blueprints/<mob_id>/<mob_id>.bbmodel` (konwencja projektowa: podkatalog per mob; importy legacy leżą płasko). Resource pack generuje ModelEngine server-side (`pack.zip`) — **NIE ma ręcznego eksportu per-bone JSON**; układ `resource pack/assets/modelengine/models/...` w archiwum referencyjnym to output starego ME, tylko do inspekcji.
- Configi mobów: `MCMMORPG/plugins/MythicMobs/Packs/<pack>/{mobs,skills,droptables}/` (aktywny pack: `level_1`).
- Raporty/artefakty: `MCMMORPG/_validation/model_studies/` i `MCMMORPG/_validation/model_reviews/<mob_id>/` (kontrakt z `mc-mob-model-agent-pipeline.md`).
- Walidatory lokalne: `MCMMORPG/_validation/check_modelengine_quality.js`, `MCMMORPG/_validation/render_bbmodel_review.js` (visual-first: render PNG → przegląd jako obraz).

## Korpus referencyjny

Rozpakowane archiwum: `$HOME/projects/Minecraft/modele/OUTPUT — kopia (2)/`
(gałęzie: `ModelEngine/` — 542 bbmodel + animacje, `MythicMobs/` — mobs/skills/items/packs, `resource pack/` — tekstury i legacy eksporty).

Fakty o korpusie (zmierzone): dominuje `model_format="free"`, `box_uv=false` (per-face UV), geometria cube-only, rozdzielczości projektów najczęściej 64×64 / 128×128 (deploy PNG bywa 256×256 — nie zakładaj, że `resolution` bbmodela = rozmiar finalnego PNG). Wersje bbmodel 3.6–5.0 → **zakaz ręcznej edycji `.bbmodel`** (format wewnętrzny Blockbench, bez stabilnej specyfikacji).

### Domyślna referencja end-to-end: `merloc` (zweryfikowana z pliku)

```yaml
reference_mob:
  mob_id: merloc
  stack: ModelEngine + MythicMobs
  source_bbmodel: ModelEngine/blueprints/merloc.bbmodel   # format free, box_uv false, res 128x128
  configs:
    - MythicMobs/mobs/littleroom/merloc.yml
    - MythicMobs/skills/littleroom/merloc.yml
    - MythicMobs/packs/littleroom/mobs/merloc.yml
    - MythicMobs/packs/littleroom/skills/merloc.yml
    - MythicMobs/items/littleroom/merloc.yml
  legacy_export_do_inspekcji: resource pack/assets/modelengine/models/merloc/
  item_models: resource pack/assets/littleroom_merloc/models/
  textures: [merloc.png, merloc_e.png, merloc_trident.png, merloc_trident_e.png]
  bones: [torso, left_arm, left_forearm, left_hand, left_hand_finger_1, left_hand_finger_2,
          right_arm, right_forearm, right_hand, trident, right_hand_finger_1, right_hand_finger_2,
          left_leg, left_shin, left_foot, right_leg, right_shin, right_foot,
          h_head, h_jaw, tail_1, tail_2, tail_3, tail_4, hitbox]
  animations: [idle, walk, dash, swing1, swing1_2, swing2, screech, deploy, summon, stun, stun_hit, death]
```

Zasady wiązania: nazwy stanów w configach (`state{s=<name>}`) = 1:1 nazwy animacji; `@modelpart{mid=<mid>;pid=<part>}` = realna kość; `h_`-prefiks = helper bones ModelEngine (head-tracking); `hitbox` zachować.

## Nadrzędne zasady

1. Nie zgaduj. Nieznana wartość = literalnie `TODO_UNKNOWN_FIELD`.
2. Zakaz ręcznego pisania/edycji `.bbmodel` — tylko przez Blockbench MCP.
3. Nie wymyślaj pól YAML — tylko pola potwierdzone referencją lub schematem repo (`docs/ai/plugin-knowledge-map.md`).
4. Inspekcja → asset plan → preflight → wykonanie → walidacja → raport. Bez skrótów.
5. Spójność nazw: `mob_id` = `mid` = nazwa pliku bbmodel = katalog blueprints = nazwy w YAML. `snake_case` dla nowych assetów.
6. Wariant istniejącej referencji > struktura od zera.
7. Konflikt między referencjami zgłaszaj jawnie, nie uśredniaj.
8. Brak części/animacji wymaganej przez config = błąd walidacji, nie „domyślne obejście".
9. Nie kończ bez `render_preview`/renderu i walidacji.

## MCP: wykrycie i sposób pracy

Skonfigurowane w `~/projects/MC/.mcp.json` (wymagany uruchomiony Blockbench desktop z załadowanym pluginem):

| Serwer | Endpoint | Rola |
|---|---|---|
| `blockbench-ashfox` | `http://127.0.0.1:8788/mcp` | **primary** — deterministyczne low-level tools, revision guard |
| `blockbench-jj` | `http://localhost:3000/bb-mcp` | fallback (jasonjgardner) — zweryfikowany działający 2026-07-09 |

**UWAGA port**: domyślny port ashfox **8787 koliduje z proxy Headroom** na tej maszynie. Po załadowaniu pluginu ustaw w Blockbench Settings → `ashfox: Server` → MCP Port = **8788** i przeładuj plugin.

Najpierw sprawdź, który endpoint odpowiada (`tools/list`). Nie zakładaj sygnatur narzędzi z pamięci — odczytaj je z `tools/list`.

### Wariant ashfox (primary)

1. `tools/list` → `list_capabilities`
2. `ensure_project` / `get_project_state`
3. Zmiany tylko wykrytymi narzędziami: `add_bone`, `add_cube` (mesh tylko gdy referencja wymaga), `assign_texture`, `paint_faces`, `read_texture`, `create_animation_clip`, `set_frame_pose`, `set_trigger_keyframes`
4. Narzędzia mutujące: przekazuj aktualny `ifRevision`; po revision-mismatch → `get_project_state` i retry z nowym revision
5. Po większej zmianie `get_project_state`; przed eksportem **obowiązkowo** `validate` → `render_preview` → `export`

### Wariant jasonjgardner (fallback)

1. Załaduj skill `blockbench-use` jeśli dostępny, potem listuj narzędzia
2. Ten sam logiczny workflow: inspekcja → plan → modelowanie → teksturowanie → animacja → walidacja → preview → eksport

## Etapy

### A — Inspekcja
Zidentyfikuj referencyjne bbmodel/tekstury/animacje/configi; wskaż najmocniejszą referencję end-to-end; potwierdź target (domyślnie ModelEngine + MythicMobs — jeśli niejednoznaczny, przyjmij i odnotuj).

### B — Asset plan (przed jakąkolwiek zmianą)

```yaml
asset_plan:
  target_stack: ModelEngine 4.0.9 + MythicMobs 5.11.2
  chosen_reference: <mob>
  new_mob_id: <snake_case>
  output_root: MCMMORPG/plugins/... (patrz Środowisko)
  geometry_strategy: reuse_reference_bone_scheme | simplified_variant | new_topology
  required_animations: [idle, walk, attack, death]   # minimum, gdy nie podano inaczej
  unresolved_items: [TODO_UNKNOWN_FIELD ...]
```

### C — Preflight (wszystko `true` albo stop)

```yaml
preflight_checklist:
  reference_model_loaded: true|false
  target_plugin_confirmed: true|false
  output_structure_confirmed: true|false
  texture_paths_resolved: true|false
  animation_names_confirmed: true|false
  part_ids_confirmed: true|false
  scale_confirmed_or_defaulted: true|false   # brak danych → s=1 + odnotuj
  no_manual_bbmodel_editing: true|false
```

### D — Modelowanie
Otwórz referencję, zlistuj outliner/kości/tekstury/animacje. Wariant = modyfikacja referencji, kompatybilne kości i pivoty. Cube-based, per-face UV. Zachowaj `hitbox` i helper bones.

### E — Tekstury
Rozmiar wg referencji (start: 64×64 lub 128×128). Konwencja emissive `_e` jeśli występuje. Spójne nazwy.

### F — Animacje
Minimum `idle, walk, attack, death` + wymagane przez skille. Nazwy dokładnie takie, jak będą w `state{s=...}`.

### G — Deploy (układ MCMMORPG, nie legacy z archiwum)

```text
MCMMORPG/plugins/ModelEngine/blueprints/<mob_id>/<mob_id>.bbmodel
MCMMORPG/plugins/MythicMobs/Packs/<pack>/mobs/<mob_id>.mob.yml
MCMMORPG/plugins/MythicMobs/Packs/<pack>/skills/<mob_id>.skill.yml
MCMMORPG/plugins/MythicMobs/Packs/<pack>/droptables/<mob_id>.droptable.yml   # gdy rewards w scope
MCMMORPG/_validation/model_studies/<mob_id>_*.md
MCMMORPG/_validation/model_reviews/<mob_id>/
```

Pack generuje ME po restarcie serwera (`docker/mc up`, patrz `mc-art-server-playbook.md`). Weryfikacja in-game: spawn przez RCON, pomiary server-side (RCON + forceload, nie botem-na-oko), death-trigger tylko realnym killem.

### H — Walidacja (obowiązkowa)

Kody błędów: `V001_TARGET_UNSPECIFIED`, `V002_REFERENCE_MISSING`, `V003_TEXTURE_MISSING`, `V004_ANIMATION_MISSING`, `V005_PART_ID_MISSING`, `V006_MODEL_ID_MISMATCH`, `V007_EXPORT_STRUCTURE_INVALID`, `V008_GUESSED_FIELD_FORBIDDEN`, `V009_MANUAL_BBMODEL_FORBIDDEN`, `V010_VALIDATION_FAILED`.

```yaml
validation_result:
  status: PASS|WARN|FAIL
  errors: []
  warnings: []
  checked: {model_ids: bool, texture_links: bool, animation_names: bool,
            part_ids: bool, export_paths: bool, config_keys_known: bool}
  outputs: {previews_generated: N, exported_files: N, changed_files: [...]}
```

Uzupełniająco: `node MCMMORPG/_validation/render_bbmodel_review.js` + przegląd renderów jako obraz (validator sam w sobie nie wystarcza) oraz `check_modelengine_quality.js`.

### I — Raport końcowy (dokładnie w tej kolejności)
1. `DETECTED_STACK` 2. `CHOSEN_REFERENCE` 3. `ASSET_PLAN` 4. `OUTPUTS` (pliki, co deployowalne, gdzie `TODO_UNKNOWN_FIELD`) 5. `VALIDATION_RESULT`

## Instalacja pluginów (runtime, jednorazowo per maszyna)

- ashfox: Blockbench → File → Plugins → Load Plugin from URL → `https://github.com/sigee-min/ashfox/releases/latest/download/ashfox.js` (lokalna kopia: `~/projects/Minecraft/tools/ashfox-plugin/ashfox.js`; źródła: `~/projects/Minecraft/tools/ashfox/`). Potem **zmień port na 8788** (patrz UWAGA wyżej). Status popup: `ashfox MCP inline|sidecar: 127.0.0.1:8788/mcp`.
- jasonjgardner: Load Plugin from URL → `https://jasonjgardner.github.io/blockbench-mcp-plugin/mcp.js`; port/endpoint w Settings → General (`3000` / `bb-mcp`). Stan 2026-07-09: załadowany i działający.
- Blockbench jest snapem (`blockbench-snapcraft`) — przy problemach z portem/plikami sprawdź confinement; fallback: AppImage/deb.
- Check (endpoint wymaga handshake MCP initialize → Mcp-Session-Id; goły `tools/list` bez sesji zwróci błąd): `curl -s http://127.0.0.1:8788/mcp -H "Content-Type: application/json" -H "Accept: application/json, text/event-stream" -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"probe","version":"1.0"}}}'`
- Pułapka `risky_eval` (jj): wymaga otwartego projektu; odrzuca kod zawierający `//` (także w URL-ach) i `console.`.
