# Nexo Packet: mcmmo_elite_crypt_warden

Status: prepared

Entity model:
- The boss model is expected to ship through ModelEngine's generated resource pack.
- Local Nexo documentation says Nexo can auto-import ModelEngine's resource pack and merge it into its own pack.
- No separate Nexo entity item id is required for the boss model at this stage.

Drop/token draft:
- `mcmmo_elite_crypt_shard` is provided as a Nexo item stub for a future boss drop/token.
- This item is optional for the model smoke test; it is included so the asset packet has a resource-pack-facing token ready for loot design.

Manual/runtime pending:
- Confirm Nexo is configured to import ModelEngine pack content on staging.
- Run `/nexo reload pack` only on staging after ModelEngine import.
- Verify the shard with `/nexo iteminfo mcmmo_elite_crypt_shard` if the item file is copied into the staging Nexo items folder.
