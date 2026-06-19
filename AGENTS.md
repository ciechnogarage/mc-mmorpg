# Repository Guidelines

## Project Structure & Module Organization

- `MCMMORPG/` contains the runnable Paper Minecraft server copy, including `paper.jar`, world folders, `server.properties`, and plugin configuration.
- `MCMMORPG/plugins/` holds plugin jars and per-plugin config folders such as `MMOCore/`, `CMI/`, `Iris/`, and `ModelEngine/`.
- `MCMMORPG/world*` contains generated world state. Treat these files as data, not hand-edited source.
- `docs/` contains operational documentation. Keep `docs/plugin_manifest.yaml`, `docs/compatibility_matrix.md`, and `docs/server_environment.md` aligned with server changes.
- `docs/ai/` is for durable AI workflows, playbooks, and review checklists.

## Build, Test, and Development Commands

- Start the server locally from the server directory:
  `cd MCMMORPG && java -Xms2G -Xmx6G -jar paper.jar --nogui`
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

- After plugin jar, config, or world-generation changes, boot the server and review `logs/latest.log` for errors, missing dependencies, and warning spikes.
- For gameplay changes, verify the relevant flow in-game on staging before production use.
- Update docs when observed versions, runtime requirements, or known gaps change.

## Commit & Pull Request Guidelines

This path does not expose usable Git history, so no repository-specific commit convention can be inferred. Use concise, imperative commit messages such as `Update MMOCore class config` or `Document Paper 1.21.11 plugin inventory`.

Pull requests should include the affected plugin/config paths, validation performed, log issues found or cleared, and screenshots or short clips for UI, HUD, menu, model, or gameplay-facing changes.

## Security & Configuration Tips

Do not commit secrets, private keys, raw player data, or unnecessary production logs. Be especially careful with plugin keys, database settings, `ops.json`, ban lists, and sync/auth plugin configuration. Back up world and player data before risky plugin or schema changes.
