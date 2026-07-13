# Walidacja serwera botem (mineflayer)

Obiektywna weryfikacja zmian in-game: bot wchodzi, generuje świat, czyta bloki.

## Pliki
- `validate.js` — pełna walidacja Iris imagemap: tworzy świat `dungeon_test` z packa
  `empty`, przelatuje footprint 200×500, odczytuje bloki powierzchni, rysuje mapę stref
  + werdykt (START/MID/END + proporcje).
- `probe.js` / `probe2.js` — minimalne testy logowania (diagnostyka spawnu/resource packa).
- `rcon-validate.js` — szkic fallbacku przez RCON (niepełny; `execute in` nie celuje
  w światy spoza overworld/nether/end — preferuj bota).

## Wymagania (tymczasowe, na czas testu!)
Bot nie zaloguje się przy domyślnej konfiguracji — trzeba PRZYWRÓCIĆ po teście:
1. `server.properties`: `online-mode=false`, `enable-rcon=true`, `enforce-secure-profile=false`,
   `rcon.password=<hasło>` (skrypty używają `RCON_PASS`).
2. `plugins/Nexo/settings.yml` → `dispatch`: `send_pre_join: false`, `mandatory: false`
   (inaczej bot wisi w fazie configuration na obowiązkowym resource packu).

Backupy oryginałów: `server.properties.bak.preval`, `plugins/Nexo/settings.yml.bak.preval`.

## Uruchomienie
```bash
cd _validation && npm install
node validate.js | tee validate.out
node validate_foundation_report.js runs/<foundation_report>.md
```
Sukces = mapa z trzema ostrymi strefami (S/M/E) + ramką `.` (deepslate poza obrazem).

## Foundation Runtime Rules

- Runtime-touching foundation validation is serialized through `_validation/.runtime.lock`.
- Foundation reports now separate `STATIC_CONTRACT`, `RUNTIME_PROOF`, `INTEGRATION_PROOF`, and `PLAYER_PROOF`.
- A config/file PASS is not enough to close the full vertical slice.
- For ModelEngine mobs, treat `MythicMobs YAML -> .bbmodel source -> .data/cache.json compiled entries` as one required chain.
- Run `node check_modelengine_binding.js` before claiming that a boss/mob model exists, loads, or only needs visual polish.
- Run `node check_modelengine_quality.js` before claiming that a boss model is production-worthy. A passing pipeline is not enough; the blueprint must have a segmented `outliner` rig, a hidden `hitbox` bone, and declared state animations.
- The full production standard is `../../docs/ai/modelengine-model-production.md`.
- Run `npm run modelengine:render -- --model <model_id>` after every material geometry or animation change. Its multi-view, silhouette, player-scale, hitbox, and animation frames are the visual evidence source.
- Boss models require `_validation/model_quality/<model_id>.quality.json`. Never place review JSON in `plugins/ModelEngine/blueprints/`, because ModelEngine attempts to import it as a model. The manifest SHA-256 must match, every review score must be at least 4/5, `knownIssues` must be empty, and every referenced evidence file must exist.
- Run `npm run modelengine:quality:test` after changing quality rules. The fixtures prove that orphan cubes, duplicate UUIDs, visible or malformed hitboxes, invalid UVs, and missing animations are rejected.
- Prismarine Viewer screenshots are not valid ModelEngine visual evidence in this environment because it does not resolve generated resource-pack item models. Use the `.bbmodel` renderer for appearance and the Mineflayer probe for live registration/spawn/render-part delivery.
- Run `npm run modelengine:corpus -- --corpus "<reference-root>" --audio-metadata` to rebuild the complete reference inventory, relationship graph, blueprint benchmarks, image metadata, and audio metadata.
- Corpus generation also rebuilds `modelengine-design-router.json`, `modelengine-negative-patterns.json`, and the human routing guide. These route independent references by design subsystem and expose evidence-backed limitations; they are not templates for copying a source model.
- Record model-specific hypotheses and verified outcomes in `active_runtime_reviews/model_design_learning.json`. Corpus generation never overwrites this append-only journal.
- Run `npm run modelengine:ecosystem -- --model <model_id>` for schema-v2 manifests. This checks animation contracts, gameplay helper bones, skill definitions, model-part usage, impact timing, GCD/model locks, and integration files.
- Run `npm run modelengine:ecosystem:test` after changing ecosystem rules.
- Run `npm run modelengine:boss-reviews` to regenerate the runtime-aware boss pipeline, active runtime review templates, and draft scaffolds together.
- Completed active-runtime strict reviews live in `active_runtime_reviews/completed_runtime_boss_reviews.json`; run `npm run modelengine:runtime-reviews:test` to validate their claims, hashes, animation coverage, and runtime-evidence boundaries.
- The strict-review process is documented in `STRICT_REVIEW_WORKFLOW.md`.
