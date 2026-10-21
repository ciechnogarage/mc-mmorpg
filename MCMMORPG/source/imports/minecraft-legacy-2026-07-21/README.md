# Minecraft legacy source/assets/content/scripts import

Source: `/home/przemek/projects/Minecraft`

Target: `/home/przemek/projects/MC/MCMMORPG/source/imports/minecraft-legacy-2026-07-21`

This is an additive import snapshot. It does not delete, move, archive, or modify the legacy source root. It does not import runtime, database, player data, ops, `.tools`, or server-copy material.

Operational status: quarantine/source-import. Contents are not promoted to active runtime or staging by this import. Future promotion requires a separate ticket, `dev-<scope>` lease for write/runtime work, and validation evidence.

## Imported groups

| Group | Target layer | Status | Files | Bytes |
| --- | --- | --- | ---: | ---: |
| `assets` | `assets` | `imported` | 16 | 121628 |
| `content` | `content` | `imported` | 304 | 19536689 |
| `scripts` | `source` | `imported` | 1 | 6653 |

## Manifests

- `import-manifest.tsv` records source path, destination path, size, and SHA-256 for every imported file.
- `script-legacy-dependencies.tsv` records legacy path/platform markers found in imported scripts.
- `skipped-manifest.tsv` records source files intentionally not imported by this ticket.
- `promotion-decisions.md` records the non-executing promotion/cleanup decision map for imported groups.
