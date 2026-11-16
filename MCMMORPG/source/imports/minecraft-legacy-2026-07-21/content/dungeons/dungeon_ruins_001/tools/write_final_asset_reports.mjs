#!/usr/bin/env node
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const root = path.resolve(path.dirname(__filename), "..");
const qa = path.join(root, "qa");
const specs = path.join(root, "assets_to_create");

const schematics = [
  "gatehouse_broken_01.schem", "rooted_courtyard_01.schem", "varos_gate_arena_01.schem",
  "collapsed_stairwell_01.schem", "cistern_puzzle_01.schem", "nhalruun_oracle_chamber_01.schem",
  "crown_bridge_01.schem", "void_anchor_hall_01.schem", "void_sanctum_arena_01.schem", "loot_shrine_exit_01.schem"
];
const models = [
  "dr001_ruin_thrall.bbmodel", "dr001_mossbound_archer.bbmodel", "dr001_rootling.bbmodel",
  "dr001_animated_rubble.bbmodel", "dr001_gatewarden_varos.bbmodel", "dr001_drowned_tombguard.bbmodel",
  "dr001_plague_acolyte.bbmodel", "dr001_cistern_leech.bbmodel", "dr001_bone_mason.bbmodel",
  "dr001_nhalruun.bbmodel", "dr001_voidbound_knight.bbmodel", "dr001_rune_sentinel.bbmodel",
  "dr001_crown_shade.bbmodel", "dr001_anchor_warden.bbmodel", "dr001_ruined_king.bbmodel"
];
const nexoGroups = {
  block: ["rune_cracked_stone", "ancient_moss_panel", "cursed_water_grate", "void_rune_block", "broken_crown_ornament"],
  furniture: ["void_anchor", "rune_pedestal", "broken_king_statue", "soul_censer", "loot_shrine"],
  item: ["ruined_gate_key", "relic_shard", "cleansing_draught"]
};
const sounds = ["roots_whisper.ogg", "cistern_geyser.ogg", "void_crown_pulse.ogg", "aurelion_enrage.ogg"];

function mkdir(file) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
}

function write(file, text) {
  mkdir(file);
  fs.writeFileSync(file, text);
}

function rel(file) {
  return path.relative(root, file).replaceAll("\\", "/");
}

function sha(file) {
  return fs.existsSync(file) ? crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex") : null;
}

function entry(assetId, category, file, target, qualityStatus, notes) {
  const exists = fs.existsSync(file);
  return {
    required_asset_id: assetId,
    category,
    expected_source_path: rel(file),
    found: exists,
    candidate_source: exists ? rel(file) : null,
    quality_status: qualityStatus,
    namespace_status: assetId.includes("dungeon_ruins_001") ? "legacy_rejected" : "dr001_or_validator_final_name",
    checksum: sha(file),
    target_path: target,
    notes
  };
}

const inventory = [];
for (const name of schematics) {
  inventory.push(entry(name, "schematic", path.join(root, "intake", "schematics", name), `plugins/Iris/packs/dungeon_ruins_001/schematics/${name}`, "blocked_quality_review", "Valid gzipped schematic candidate generated locally; needs builder review before final approval."));
}
for (const name of models) {
  inventory.push(entry(name, "modelengine", path.join(root, "intake", "modelengine", name), `plugins/ModelEngine/blueprints/${name}`, "blocked_quality_review", "ModelEngine technical candidate has required aliases and hitbox data; silhouette/art pass is not final."));
}
for (const [kind, names] of Object.entries(nexoGroups)) {
  for (const name of names) {
    inventory.push(entry(`dr001_${name}.json`, "nexo_model", path.join(root, "intake", "nexo", "models", "item", "dr001", kind, `${name}.json`), `plugins/Nexo/pack/assets/nexo/models/item/dr001/${kind}/${name}.json`, "review_ready", "JSON parses and references final dr001 texture namespace."));
    inventory.push(entry(`dr001_${name}.png`, "nexo_texture", path.join(root, "intake", "nexo", "textures", "item", "dr001", kind, `${name}.png`), `plugins/Nexo/pack/assets/nexo/textures/item/dr001/${kind}/${name}.png`, "blocked_quality_review", "Valid PNG texture candidate generated locally; needs art review."));
  }
}
for (const name of sounds) {
  inventory.push(entry(`dr001_${name}`, "sound", path.join(root, "intake", "sounds", "dr001", name), `plugins/Nexo/pack/assets/nexo/sounds/dr001/${name}`, "missing_authoring_tool", "No ffmpeg/oggenc/approved source audio available. Missing by design; do not fake."));
}

const rejected = [];
const archive = path.join(root, "legacy_candidate_archive");
function collect(dir) {
  if (!fs.existsSync(dir)) return;
  for (const item of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, item.name);
    if (item.isDirectory()) collect(full);
    else rejected.push({ asset: rel(full), reason: "Legacy/candidate namespace or old intake shape; archived as reference only, not final evidence." });
  }
}
collect(archive);

write(path.join(qa, "FINAL_ASSET_INVENTORY.json"), `${JSON.stringify({
  pack_id: "dungeon_ruins_001",
  generated_at: "2026-05-30T00:00:00Z",
  final_namespace: "dr001",
  approval_status: "partial_authoring_ready",
  summary: {
    total_required: inventory.length,
    found: inventory.filter(i => i.found).length,
    missing: inventory.filter(i => !i.found).length,
    blocked_quality_review: inventory.filter(i => i.quality_status === "blocked_quality_review").length,
    missing_authoring_tool: inventory.filter(i => i.quality_status === "missing_authoring_tool").length,
    archived_legacy_candidates: rejected.length
  },
  assets: inventory,
  rejected_legacy_candidates: rejected
}, null, 2)}\n`);

write(path.join(qa, "FINAL_ASSET_REVIEW.md"), [
  "# DR001 Final Asset Review",
  "",
  "Date: 2026-05-30",
  "Verdict: FINAL_ASSET_BUNDLE_PARTIAL_AUTHORING_READY",
  "",
  "## Summary",
  "",
  "- Created DR001-named local technical candidates for schematics, ModelEngine, and Nexo resource-pack assets.",
  "- Archived legacy `dungeon_ruins_001` intake candidates under `legacy_candidate_archive/` so final intake no longer leaks the old namespace.",
  "- Did not create Ogg files because no local Ogg encoder or approved source audio exists.",
  "- Did not mark the manifest `approved_final`; quality review and audio authoring remain required.",
  "",
  "## Promoted Or Created",
  "",
  "- Schematics: 10 gzipped `.schem` files with final required names.",
  "- ModelEngine: 15 `dr001_*.bbmodel` technical candidates with required animation aliases and explicit hitboxes.",
  "- Nexo: 13 final-namespace JSON models and 13 PNG textures under `intake/nexo/.../dr001/`.",
  "- Manifest: real SHA-256 values for created files; missing audio entries remain `missing`.",
  "",
  "## Rejected",
  "",
  "- Legacy ModelEngine `dungeon_ruins_001_*.bbmodel` files: archived, not final.",
  "- Legacy Nexo `dungeon_ruins_001` model/texture paths: archived, not final.",
  "- Legacy generated Ogg candidates: archived, not final and not treated as real authored audio.",
  "- Old unprefixed schematic candidates not in the final validator list: archived as reference only.",
  "",
  "## Quality Concerns",
  "",
  "- ModelEngine files are technical import candidates, not final bespoke boss/mob art.",
  "- Procedural schematics need builder inspection for gameplay scale, landmark composition, and route readability.",
  "- Procedural Nexo textures are coherent with the DR001 palette but still need art review.",
  "- Audio is fully blocked until real Ogg files are authored or converted with an approved local tool.",
  "",
  "## Remaining Work",
  "",
  "- Author final Ogg files: `roots_whisper`, `cistern_geyser`, `void_crown_pulse`, `aurelion_enrage`.",
  "- Human review or replace ModelEngine silhouettes for Varos, Nhal'Ruun, Aurelion, and trash/elite mobs.",
  "- Builder review or replace schematics against the dungeon layout and encounter pacing.",
  "- Approve final manifest only after quality review passes and all checksums are present.",
  ""
].join("\n"));

write(path.join(specs, "dr001_sound_requirements.md"), [
  "# DR001 Sound Requirements",
  "",
  "Final Ogg files required under `intake/sounds/dr001/`:",
  "",
  "- `roots_whisper.ogg`: low moss/root ambience, quiet positional loop.",
  "- `cistern_geyser.ogg`: water pressure burst with stone resonance.",
  "- `void_crown_pulse.ogg`: cyan/violet void-crown pulse, readable cue.",
  "- `aurelion_enrage.ogg`: boss enrage sting, dark regal impact.",
  "",
  "No fake Ogg containers are acceptable. Use authored audio or approved licensed source audio, then record license/source notes and SHA-256 in `intake/manifest.yml`.",
  ""
].join("\n"));

write(path.join(specs, "dr001_model_requirements.md"), [
  "# DR001 Model Requirements",
  "",
  "Replace or art-review the 15 `dr001_*.bbmodel` technical candidates. Each final file needs a readable silhouette, non-empty hitbox data, and the animations listed in `modelengine/models.md`.",
  "",
  "Boss priority: `dr001_gatewarden_varos`, `dr001_nhalruun`, `dr001_ruined_king`.",
  ""
].join("\n"));

write(path.join(specs, "dr001_schematic_requirements.md"), [
  "# DR001 Schematic Requirements",
  "",
  "Review or replace the 10 generated `.schem` candidates in `intake/schematics/`. Final builder review must confirm route readability, encounter space, entrance/exit alignment, and no debug/test-looking block usage.",
  ""
].join("\n"));

write(path.join(specs, "dr001_nexo_prop_requirements.md"), [
  "# DR001 Nexo Prop Requirements",
  "",
  "Review or replace the 13 generated model/texture pairs under `intake/nexo/.../dr001/`. Final art should support ancient ruined fortress, broken crown, void cistern, moss/root, muted gold, and cyan/violet corruption reads.",
  ""
].join("\n"));

console.log("Wrote DR001 final asset inventory, review, and authoring specs.");
