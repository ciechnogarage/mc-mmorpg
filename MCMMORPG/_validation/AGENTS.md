# Visual Asset Validation Instructions

These rules apply to validators, fixtures, manifests, and evidence under
`MCMMORPG/_validation/`.

## Manifest and evidence contract

- Route visual-asset validation through
  `docs/contracts/visual-asset-domain-handoff-contract.yaml` and select checks
  from this directory's `package.json`; do not invent a repository-wide test
  command.
- New profile-aware quality manifests must declare `asset_id`, `asset_type`,
  `style_profile`, `rig_archetype`, and `delivery_profile`. Select rig and
  animation requirements from the declared profile rather than assuming a
  humanoid model.
- Bind every verdict to the exact source SHA-256 and every reviewed render to
  its SHA-256. Any source or render change invalidates the corresponding
  verdict. Builder output cannot award itself `ART_PASS`.
- Preserve existing fixtures until profile-specific replacements prove the
  same rejection behavior. A manifest/schema migration must include both valid
  and deliberately invalid regression fixtures.

## Gate separation

- `ART_PASS` requires a fresh visual review of the declared brief, style
  profile, and hash-bound render set.
- `INTEGRATION_PASS` may be issued from static source, binding, export, and
  pack-contract checks. It does not prove gameplay, visual quality, plugin
  activation, or client delivery.
- `RUNTIME_PASS` requires separately approved runtime evidence for the exact
  source/configuration/pack hashes. Never synthesize it from a passing static
  check, generated cache, log fragment, or existing screenshot.
- Missing, stale, or mismatched hashes fail closed. Keep failures explicit;
  do not downgrade them to warnings to advance a gate.

## Operational boundary

Validation work here is read-only with respect to server state unless the user
separately approves runtime work. Do not install Blockbench integrations,
start or reload servers, publish packs, or modify worlds, player data, economy,
permissions, or secrets as part of a static validation task.
