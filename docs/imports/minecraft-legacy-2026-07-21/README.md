# Minecraft Legacy Docs Import — 2026-07-21

## LEGACY-ONLY / QUARANTINE WARNING

Do not use files under `raw/` as active runbooks. This import is an archive/reference quarantine from the old `/home/przemek/projects/Minecraft` workspace.

The current Minecraft workspace standard is `/home/przemek/projects/MC` plus `dev-<scope>` lease for write work. Old Windows/Cursor/staging/`Server — kopia` commands in this import are not active operating instructions.

Use `legacy-labels.md` before reading or extracting anything from `raw/`.

This directory is a quarantine import from `/home/przemek/projects/Minecraft` into `/home/przemek/projects/MC/docs`.

It is not a promotion to canonical documentation. The source may contain stale, duplicated, generated, or low-quality material. Imported files must be reviewed before being merged into active docs.

## Scope imported

- `docs/`
- `wiki/` excluding workspace metadata directories `.git/` and `.codegraph/`
- `design/`
- `decisions/`
- `tasks/`
- root `AGENTS.md`

Root `decision_log.md` was requested by the decision plan but was missing at `/home/przemek/projects/Minecraft/decision_log.md`. The existing `/home/przemek/projects/Minecraft/decisions/decision_log.md` is included under `raw/decisions/`.

## Counts

- Imported files: 170
- Skipped metadata files: 32
- Missing requested sources: 1
- Imported bytes: 3952726

## Rules

- Do not treat anything under `raw/` as canonical without review.
- Do not overwrite `/home/przemek/projects/MC/docs` with legacy material without a separate merge decision.
- Keep `/home/przemek/projects/Minecraft` untouched until archive-old-roots ticket is explicitly approved.
- Do not import runtime, DB, player data, `.tools`, `ops`, or `Server — kopia` through this ticket.

## Manifest

See `import-manifest.tsv` for source path, target path, checksum, collision flag, risk flags, and review status.
