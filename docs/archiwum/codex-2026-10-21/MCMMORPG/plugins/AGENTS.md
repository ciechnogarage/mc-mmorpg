# Plugin Visual Asset Instructions

These rules apply to authoring sources and plugin integration under
`MCMMORPG/plugins/`. Read
`docs/contracts/visual-asset-domain-handoff-contract.yaml` before changing a
visual asset or its delivery configuration.

## Source and output boundaries

- For a ModelEngine entity, the editable source is its `.bbmodel` under
  `ModelEngine/blueprints/`. Mutate that file only through the approved
  transactional Blockbench MCP adapter; direct JSON generation or patching is
  forbidden.
- Treat ModelEngine `.data/`, generated resource-pack content, exports, ZIPs,
  and caches as outputs. Do not use them as authoring sources or edit them to
  repair the blueprint.
- For a `nexo_static` asset, Nexo source configuration and its referenced
  authoring textures/models are the editable inputs. Treat generated files
  under `Nexo/pack/` as outputs.
- Nexo owns the merged resource pack while `Pack.import.model_engine` is true.
  Do not introduce a second pack owner or publish a ModelEngine pack directly.

## Routing and identifiers

- Start from a declared `asset_id`, `asset_type`, `style_profile`,
  `rig_archetype`, and `delivery_profile`. Use `modelengine_entity` for
  animated entity delivery and `nexo_static` for static item, weapon, cosmetic,
  furniture, or prop delivery unless the canonical contract is revised.
- Keep project-owned asset identifiers lowercase `snake_case` and consistent
  across the AssetSpec, blueprint directory/file, ModelEngine model id, Nexo
  item/model references, MythicMobs binding, and validation evidence. Preserve
  vendor-defined keys and filenames exactly.
- The requesting domain retains gameplay semantics: Domain 05 item stats and
  rarity, Domain 06 world and progression context, Domain 07 encounter role and
  telegraph timing, Domain 12 companion behavior and persistence, and Domain 14
  UI presentation constraints. Plugin placement never transfers that authority.
- Domain 15 owns the visual and delivery contract. Domain 18 may orchestrate
  integration work but does not redefine the brief or issue an art verdict.
  Domain 17 alone evaluates release evidence.

## Verification boundary

- Validate the source and every changed binding/configuration using the
  relevant scripts in `MCMMORPG/_validation/package.json`.
- Report complete static integration evidence as `READY_FOR_INTEGRATION_GATE`.
  Domain 17 alone may turn that evidence into `INTEGRATION_PASS`. Do not infer
  `ART_PASS` from structure or `RUNTIME_PASS` from configuration, cache, export,
  or pack presence.
- Server starts, plugin reloads, pack publication, staging, and live runtime
  checks require explicit approval.
