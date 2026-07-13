# Repository Guidelines

## Project Structure & Module Organization

- `MCMMORPG/` contains the runnable Paper Minecraft server copy, including `paper.jar`, world folders, `server.properties`, and plugin configuration.
- `MCMMORPG/plugins/` holds plugin jars and per-plugin config folders such as `MMOCore/`, `CMI/`, `Iris/`, and `ModelEngine/`.
- `MCMMORPG/world*` contains generated world state. Treat these files as data, not hand-edited source.
- `docs/` contains operational documentation. Keep `docs/plugin_manifest.yaml`, `docs/compatibility_matrix.md`, and `docs/server_environment.md` aligned with server changes.
- `docs/ai/` is for durable AI workflows, playbooks, and review checklists.

## Build, Test, and Development Commands

- Do not start Paper natively and do not use the disabled `start.sh`. Runtime
  validation uses Docker backends behind Velocity; see
  `MCMMORPG/START_HERE_DOCKER.md`.
- Select the backend by affected area: `world` for dungeons/world/mobs/models,
  `items` for items/equipment/classes/login, and `hub` for social/hub work.
- From `MCMMORPG/docker`, use `./mc up <area>`, `./mc rcon <area> "<command>"`,
  `./mc logs <area>`, and `./mc down <area>`. Players connect through Velocity
  on `localhost:25565`; never validate by connecting directly to a backend.
- Test every affected area on its own backend. Cross-area changes require
  separate evidence from each affected backend.
- Inspect the latest boot/runtime log:
  `tail -n 200 MCMMORPG/logs/latest.log`
- Find plugin or config references quickly:
  `rg "PluginName|setting_key" MCMMORPG/plugins docs`
- Review plugin inventory docs before upgrades:
  `sed -n '1,160p' docs/plugin_manifest.yaml`

There is no application build system in this repository. Validation is primarily server boot, log review, plugin compatibility review, and focused in-game checks on staging.

## Coding Style & Naming Conventions

- Use 2-space indentation for YAML config files unless the existing file uses another style.
- Keep JSON valid and minimally reformatted; avoid churn in generated files.
- Prefer lowercase, descriptive doc filenames with hyphens or underscores, matching existing examples such as `server_environment.md`.
- Preserve plugin vendor filenames and jar versions exactly.

## Testing Guidelines

- After plugin jar, config, or world-generation changes, start only the relevant
  Docker backend and inspect both `./mc logs <area>` and the area-specific runtime
  evidence for errors, missing dependencies, and warning spikes.
- For gameplay changes, verify the relevant flow in-game on staging before production use.
- For `ModelEngine` mobs, do not treat a YAML `Model:` section or an existing `.bbmodel` as proof that the model is active. Before claiming a model exists or works, verify all three: the active MythicMobs YAML reference, the source blueprint in `plugins/ModelEngine/blueprints/`, and compiled `model_id:*` entries in `plugins/ModelEngine/.data/cache.json`.
- When touching boss/mob models, run `node MCMMORPG/_validation/check_modelengine_binding.js` before any art/runtime conclusion. If the script reports `CACHE_MISSING`, the model is not active yet, regardless of reload output or viewer screenshots.
- For boss-quality `ModelEngine` work, a valid import is still not enough. Before calling a model "good", verify that the blueprint has a real segmented `outliner` rig (not one flat bag of cubes), a hidden `hitbox` bone, and explicit state animations. Run `node MCMMORPG/_validation/check_modelengine_quality.js`; if it fails on `RIG_NOT_SEGMENTED`, `HIERARCHY_TOO_FLAT`, or `ANIMATIONS_MISSING`, treat the model as unfinished.
- For every new or materially changed ModelEngine model, follow `docs/ai/modelengine-model-production.md`. A quality claim requires current multi-view renders, a quality manifest whose SHA-256 matches the shipped blueprint, all review scores at least 4/5, no known issues, and live spawn inspection. Import/cache success alone is never visual approval.
- All creature-model final art is Blockbench-first. Edit and inspect the real `.bbmodel` natively in Blockbench while iterating; do not treat guessed numeric edits, script-generated geometry, or render-only review as an acceptable primary art workflow.
- Before overwriting a generated `.bbmodel` or its generator during visual iteration, create a versioned backup containing both files. Never describe a user-created copy as an agent-created backup.
- After two visual rejections of the same model, stop geometry mutations. Return to the best explicitly accepted visual baseline and prepare side-by-side silhouette/material studies; do not substitute a mechanically simplified blockout or treat structural validation as artistic progress.
- Boss manifests use schema version 2 and must bind animation contracts to real MythicMobs skills and model-part bones. Run `npm --prefix MCMMORPG/_validation run modelengine:ecosystem -- --model <model_id>`; unresolved states, helper bones, impact ticks, skill sources, or integration files block completion.
- Update docs when observed versions, runtime requirements, or known gaps change.

## Commit & Pull Request Guidelines

This path does not expose usable Git history, so no repository-specific commit convention can be inferred. Use concise, imperative commit messages such as `Update MMOCore class config` or `Document Paper 1.21.11 plugin inventory`.

Pull requests should include the affected plugin/config paths, validation performed, log issues found or cleared, and screenshots or short clips for UI, HUD, menu, model, or gameplay-facing changes.

## Security & Configuration Tips

Do not commit secrets, private keys, raw player data, or unnecessary production logs. Be especially careful with plugin keys, database settings, `ops.json`, ban lists, and sync/auth plugin configuration. Back up world and player data before risky plugin or schema changes.
