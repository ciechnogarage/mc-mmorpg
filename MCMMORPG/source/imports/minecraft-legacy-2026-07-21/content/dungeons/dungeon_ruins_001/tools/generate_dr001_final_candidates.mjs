#!/usr/bin/env node
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { deflateSync, gzipSync } from "node:zlib";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const root = path.resolve(path.dirname(__filename), "..");
const intake = path.join(root, "intake");
const archive = path.join(root, "legacy_candidate_archive");
const repoRoot = path.resolve(root, "..", "..", "..");
const rustRaider = path.join(repoRoot, "Server \u2014 kopia", "1.21.11", "plugins", "ModelEngine", "blueprints", "rust_raider.bbmodel");

const schematics = [
  ["gatehouse_broken_01", 43, 25, 35, "start", "gate"],
  ["rooted_courtyard_01", 57, 23, 57, "start", "courtyard"],
  ["varos_gate_arena_01", 61, 27, 51, "start", "boss_gate"],
  ["collapsed_stairwell_01", 37, 29, 41, "start", "stairs"],
  ["cistern_puzzle_01", 49, 23, 49, "mid", "cistern"],
  ["nhalruun_oracle_chamber_01", 57, 31, 57, "mid", "oracle"],
  ["crown_bridge_01", 59, 25, 25, "mid", "bridge"],
  ["void_anchor_hall_01", 47, 27, 43, "end", "anchor"],
  ["void_sanctum_arena_01", 73, 33, 73, "end", "arena"],
  ["loot_shrine_exit_01", 35, 23, 35, "end", "shrine"]
];
const requiredSchematicFiles = new Set(schematics.map(([id]) => `${id}.schem`));

const models = {
  dr001_ruin_thrall: ["idle", "walk", "attack", "hit", "death"],
  dr001_mossbound_archer: ["idle", "walk", "shoot", "root_cast", "death"],
  dr001_rootling: ["idle", "leap", "attack", "death"],
  dr001_animated_rubble: ["idle", "walk", "slam", "crumble", "death"],
  dr001_gatewarden_varos: ["idle", "walk", "attack_slam", "shield", "roar", "death"],
  dr001_drowned_tombguard: ["idle", "walk", "shield_bash", "death"],
  dr001_plague_acolyte: ["idle", "walk", "cast", "heal", "death"],
  dr001_cistern_leech: ["idle", "swim", "bite", "death"],
  dr001_bone_mason: ["idle", "walk", "summon", "cast", "death"],
  dr001_nhalruun: ["idle", "swim_idle", "cast", "flood", "mirror", "death"],
  dr001_voidbound_knight: ["idle", "walk", "cleave", "enrage", "death"],
  dr001_rune_sentinel: ["idle", "beam_cast", "overload", "death"],
  dr001_crown_shade: ["idle", "blink", "stab", "death"],
  dr001_anchor_warden: ["idle", "walk", "shield", "slam", "death"],
  dr001_ruined_king: ["idle", "walk", "cleave", "shockwave", "summon", "phase", "death"]
};

const nexo = {
  block: ["rune_cracked_stone", "ancient_moss_panel", "cursed_water_grate", "void_rune_block", "broken_crown_ornament"],
  furniture: ["void_anchor", "rune_pedestal", "broken_king_statue", "soul_censer", "loot_shrine"],
  item: ["ruined_gate_key", "relic_shard", "cleansing_draught"]
};

function mkdir(file) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
}

function write(file, data) {
  mkdir(file);
  fs.writeFileSync(file, data);
}

function listFiles(dir) {
  if (!fs.existsSync(dir)) return [];
  const out = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) out.push(...listFiles(full));
    else if (entry.name !== ".gitkeep") out.push(full);
  }
  return out;
}

function archiveLegacyIntake() {
  for (const file of listFiles(intake)) {
    const rel = path.relative(intake, file).replaceAll("\\", "/");
    const legacyNamespace = rel.includes("dungeon_ruins_001");
    const extraSchematic = rel.startsWith("schematics/") && !requiredSchematicFiles.has(path.basename(file));
    if (!legacyNamespace && !extraSchematic) continue;
    const target = path.join(archive, rel);
    mkdir(target);
    fs.renameSync(file, target);
  }
}

function crc32(buffer) {
  if (!crc32.table) {
    crc32.table = new Uint32Array(256);
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      crc32.table[n] = c >>> 0;
    }
  }
  let c = 0xffffffff;
  for (const byte of buffer) c = crc32.table[(c ^ byte) & 255] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data = Buffer.alloc(0)) {
  const typeBuf = Buffer.from(type);
  const len = Buffer.alloc(4);
  const crc = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  crc.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])));
  return Buffer.concat([len, typeBuf, data, crc]);
}

function png(width, height, pixel) {
  const raw = Buffer.alloc((width * 4 + 1) * height);
  for (let y = 0; y < height; y++) {
    const row = y * (width * 4 + 1);
    raw[row] = 0;
    for (let x = 0; x < width; x++) {
      const [r, g, b, a] = pixel(x, y);
      const i = row + 1 + x * 4;
      raw[i] = r; raw[i + 1] = g; raw[i + 2] = b; raw[i + 3] = a;
    }
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;
  ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk("IHDR", ihdr), chunk("IDAT", deflateSync(raw)), chunk("IEND")]);
}

function tagString(name, value) {
  const n = Buffer.from(name), v = Buffer.from(value);
  const out = Buffer.alloc(1 + 2 + n.length + 2 + v.length);
  let o = 0; out[o++] = 8; out.writeUInt16BE(n.length, o); o += 2; n.copy(out, o); o += n.length; out.writeUInt16BE(v.length, o); o += 2; v.copy(out, o);
  return out;
}

function tagShort(name, value) {
  const n = Buffer.from(name);
  const out = Buffer.alloc(1 + 2 + n.length + 2);
  let o = 0; out[o++] = 2; out.writeUInt16BE(n.length, o); o += 2; n.copy(out, o); o += n.length; out.writeInt16BE(value, o);
  return out;
}

function tagInt(name, value) {
  const n = Buffer.from(name);
  const out = Buffer.alloc(1 + 2 + n.length + 4);
  let o = 0; out[o++] = 3; out.writeUInt16BE(n.length, o); o += 2; n.copy(out, o); o += n.length; out.writeInt32BE(value, o);
  return out;
}

function tagIntArray(name, values) {
  const n = Buffer.from(name);
  const out = Buffer.alloc(1 + 2 + n.length + 4 + values.length * 4);
  let o = 0; out[o++] = 11; out.writeUInt16BE(n.length, o); o += 2; n.copy(out, o); o += n.length; out.writeInt32BE(values.length, o); o += 4;
  for (const value of values) { out.writeInt32BE(value, o); o += 4; }
  return out;
}

function tagByteArray(name, values) {
  const n = Buffer.from(name), body = Buffer.from(values);
  const out = Buffer.alloc(1 + 2 + n.length + 4 + body.length);
  let o = 0; out[o++] = 7; out.writeUInt16BE(n.length, o); o += 2; n.copy(out, o); o += n.length; out.writeInt32BE(body.length, o); o += 4; body.copy(out, o);
  return out;
}

function tagCompound(name, children) {
  const n = Buffer.from(name);
  return Buffer.concat([Buffer.from([10, n.length >> 8, n.length & 255]), n, ...children, Buffer.from([0])]);
}

function schematic([id, w, h, l, theme, type]) {
  const palettes = {
    start: ["minecraft:air", "minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:rooted_dirt", "minecraft:mangrove_roots", "minecraft:moss_carpet", "minecraft:water", "minecraft:glow_lichen", "minecraft:chain", "minecraft:lantern", "minecraft:chest"],
    mid: ["minecraft:air", "minecraft:deepslate_tiles", "minecraft:polished_deepslate", "minecraft:amethyst_block", "minecraft:calcite", "minecraft:tinted_glass", "minecraft:crying_obsidian", "minecraft:water", "minecraft:sea_lantern", "minecraft:chain", "minecraft:chest"],
    end: ["minecraft:air", "minecraft:blackstone", "minecraft:polished_blackstone_bricks", "minecraft:basalt", "minecraft:gilded_blackstone", "minecraft:crying_obsidian", "minecraft:purple_stained_glass", "minecraft:magma_block", "minecraft:chain", "minecraft:soul_lantern", "minecraft:chest"]
  };
  const palette = palettes[theme];
  const data = new Uint8Array(w * h * l);
  const set = (x, y, z, b) => { if (x >= 0 && y >= 0 && z >= 0 && x < w && y < h && z < l) data[y * w * l + z * w + x] = b; };
  const fill = (x1, y1, z1, x2, y2, z2, b) => { for (let y = y1; y <= y2; y++) for (let z = z1; z <= z2; z++) for (let x = x1; x <= x2; x++) set(x, y, z, b); };
  const cx = Math.floor(w / 2), cz = Math.floor(l / 2);
  fill(0, 0, 0, w - 1, 0, l - 1, 1);
  fill(0, 1, 0, w - 1, 4, 0, 2); fill(0, 1, l - 1, w - 1, 4, l - 1, 2);
  fill(0, 1, 0, 0, 4, l - 1, 2); fill(w - 1, 1, 0, w - 1, 4, l - 1, 2);
  fill(cx - 3, 1, 2, cx + 3, 1, l - 3, theme === "start" ? 6 : theme === "mid" ? 7 : 5);
  if (type.includes("arena") || type.includes("courtyard") || type.includes("oracle")) {
    const r = Math.min(cx, cz) - 7;
    for (let a = 0; a < 360; a += 2) {
      const rad = a % 12 === 0 ? r - 4 : r;
      set(cx + Math.round(Math.cos(a) * rad), 1, cz + Math.round(Math.sin(a) * rad), theme === "end" ? 4 : 3);
    }
    for (const [dx, dz] of [[-15, -15], [15, -15], [-15, 15], [15, 15]]) fill(cx + dx - 1, 1, cz + dz - 1, cx + dx + 1, 10, cz + dz + 1, theme === "end" ? 5 : 4);
  } else if (type === "bridge") {
    fill(2, 3, cz - 2, w - 3, 3, cz + 2, 1); fill(cx - 6, 1, cz - 5, cx + 6, 2, cz + 5, 0);
    fill(cx - 10, 1, cz - 6, cx - 8, 9, cz + 6, 3); fill(cx + 8, 1, cz - 6, cx + 10, 9, cz + 6, 3);
  } else if (type === "gate" || type === "boss_gate") {
    fill(cx - 12, 1, 4, cx + 12, 14, 6, 2); fill(cx - 5, 1, 4, cx + 5, 9, 6, 0);
    fill(cx - 16, 1, 7, cx - 13, 17, 10, 4); fill(cx + 13, 1, 7, cx + 16, 17, 10, 4);
  } else if (type === "anchor") {
    fill(cx - 4, 1, cz - 4, cx + 4, 15, cz + 4, 5); fill(cx - 9, 1, cz - 9, cx + 9, 2, cz + 9, 6);
  } else if (type === "shrine") {
    fill(cx - 8, 1, cz - 6, cx + 8, 8, cz - 4, 4); fill(cx - 3, 1, cz + 4, cx + 3, 5, cz + 7, 10);
  } else {
    for (let z = 4; z < l - 4; z += 7) {
      fill(4, 1, z, 5, 9, z + 1, 4); fill(w - 6, 1, z, w - 5, 9, z + 1, 4);
    }
  }
  for (let z = 2; z < l - 2; z++) for (let x = 2; x < w - 2; x++) {
    if ((x * 13 + z * 17 + id.length) % 43 === 0) set(x, 1, z, theme === "start" ? 5 : theme === "mid" ? 8 : 9);
    if ((x * 7 + z * 11 + w) % 97 === 0) set(x, 2, z, theme === "end" ? 7 : 8);
  }
  return gzipSync(tagCompound("Schematic", [
    tagInt("Version", 2), tagInt("DataVersion", 3955), tagShort("Width", w), tagShort("Height", h), tagShort("Length", l),
    tagIntArray("Offset", [0, 0, 0]), tagCompound("Palette", palette.map((block, i) => tagInt(block, i))), tagInt("PaletteMax", palette.length), tagByteArray("BlockData", data),
    tagCompound("Metadata", [tagString("Name", `dr001_${id}`), tagString("Author", "codex_local_procedural_candidate"), tagString("ReviewStatus", "blocked_quality_review")])
  ]));
}

function texture(kind, name) {
  const palette = name.includes("void") || name.includes("cursed") || name.includes("relic")
    ? [[18, 22, 31, 255], [37, 79, 92, 255], [80, 46, 130, 255], [115, 214, 226, 255], [195, 154, 73, 255]]
    : name.includes("crown") || name.includes("king") || name.includes("shrine")
      ? [[24, 23, 22, 255], [74, 65, 50, 255], [152, 116, 51, 255], [94, 46, 117, 255], [221, 170, 74, 255]]
      : [[31, 43, 34, 255], [72, 91, 65, 255], [95, 119, 67, 255], [42, 98, 112, 255], [150, 126, 69, 255]];
  return png(32, 32, (x, y) => {
    if (kind === "item" && Math.abs(x - 16) + Math.abs(y - 16) > 20) return [0, 0, 0, 0];
    const rune = (x > 10 && x < 22 && (y === 8 || y === 16 || y === 24)) || (y > 9 && y < 24 && (x === 11 || x === 21));
    const crack = x === y || x + y === 31 || (x * 5 + y * 3 + name.length) % 23 === 0;
    if (rune && /rune|void|relic|key|anchor|pedestal/.test(name)) return palette[3];
    if (crack) return palette[2];
    if (x < 2 || y < 2 || x > 29 || y > 29) return palette[0];
    return palette[(x * 11 + y * 7 + name.length) % 2 + 1];
  });
}

function faceRefs(texture) {
  return { north: { texture }, south: { texture }, east: { texture }, west: { texture }, up: { texture }, down: { texture } };
}

function nexoModel(kind, name) {
  const tex = `nexo:item/dr001/${kind}/${name}`;
  if (kind === "block") return { parent: "minecraft:block/cube_all", textures: { all: tex } };
  if (kind === "furniture") {
    return {
      credit: "DR001 local procedural candidate, blocked pending art review",
      textures: { particle: tex, all: tex },
      elements: [
        { from: [3, 0, 3], to: [13, 2, 13], faces: faceRefs("#all") },
        { from: [5, 2, 5], to: [11, 11, 11], faces: faceRefs("#all") },
        { from: [2, 11, 2], to: [14, 14, 14], faces: faceRefs("#all") },
        { from: [6, 14, 6], to: [10, 16, 10], faces: faceRefs("#all") }
      ]
    };
  }
  return { parent: "minecraft:item/generated", textures: { layer0: tex } };
}

function uuid(text) {
  const h = crypto.createHash("sha1").update(text).digest("hex");
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`;
}

function cube(name, from, to, color) {
  return {
    name, box_uv: false, render_order: "default", locked: false, from, to, autouv: 0, color,
    origin: [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2],
    faces: { north: { uv: [0, 0, 8, 8], texture: null }, east: { uv: [0, 0, 8, 8], texture: null }, south: { uv: [0, 0, 8, 8], texture: null }, west: { uv: [0, 0, 8, 8], texture: null }, up: { uv: [0, 0, 8, 8], texture: null }, down: { uv: [0, 0, 8, 8], texture: null } },
    type: "cube",
    uuid: uuid(`${name}:${from.join(",")}:${to.join(",")}`)
  };
}

function model(id, animationNames) {
  let base = null;
  if (fs.existsSync(rustRaider)) {
    base = JSON.parse(fs.readFileSync(rustRaider, "utf8").replace(/^\uFEFF/, ""));
    base.name = id;
    base.model_identifier = id;
    base.visible_box = [5, id.includes("king") || id.includes("varos") || id.includes("nhalruun") ? 6 : 4, 0];
    base.codex_quality_status = "blocked_quality_review";
    base.codex_source_note = "Derived from local rust_raider.bbmodel template for technical import testing; needs final art pass.";
    const boss = id.includes("king") || id.includes("varos") || id.includes("nhalruun");
    base.hitboxes = [{ name: "main", width: boss ? 2.4 : 1.2, height: boss ? 3.4 : 2.1, depth: boss ? 2.4 : 1.2, offset: [0, boss ? 1.7 : 1.05, 0] }];
  } else {
    const boss = id.includes("king") || id.includes("varos") || id.includes("nhalruun");
    const tall = boss ? 34 : id.includes("rootling") ? 12 : 24;
    const elements = [cube("body", [-3, 8, -2], [3, tall - 5, 2], 1), cube("head", [-2.5, tall - 5, -2.5], [2.5, tall, 2.5], 2), cube("left_arm", [3, 9, -1.5], [5.5, tall - 6, 1.5], 3), cube("right_arm", [-5.5, 9, -1.5], [-3, tall - 6, 1.5], 3), cube("left_leg", [0.5, 0, -1.5], [2.5, 8, 1.5], 1), cube("right_leg", [-2.5, 0, -1.5], [-0.5, 8, 1.5], 1), cube("hitbox", [boss ? -5 : -4, 0, boss ? -5 : -4], [boss ? 5 : 4, boss ? 34 : 24, boss ? 5 : 4], 0)];
    base = { meta: { format_version: "5.0", model_format: "free", box_uv: false }, name: id, model_identifier: id, visible_box: [5, boss ? 6 : 4, 0], hitboxes: [{ name: "main", width: boss ? 2.4 : 1.2, height: boss ? 3.4 : 2.1, depth: boss ? 2.4 : 1.2, offset: [0, boss ? 1.7 : 1.05, 0] }], resolution: { width: 64, height: 64 }, elements, outliner: elements.map(e => e.uuid), textures: [], codex_quality_status: "blocked_quality_review" };
  }
  const first = Object.values(base.animations || {})[0] || { loop: "loop", override: false, length: 1, animators: {} };
  base.animations = {};
  for (const name of animationNames) {
    const animation = JSON.parse(JSON.stringify(first));
    animation.name = name;
    animation.loop = name === "idle" || name === "walk" || name === "swim_idle" ? "loop" : "once";
    animation.override = !["idle", "walk", "swim_idle"].includes(name);
    animation.length = name === "death" ? 1.4 : 0.8;
    base.animations[`${id}.${name}`] = animation;
  }
  return base;
}

function sha(file) {
  return crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
}

function generateAssets() {
  archiveLegacyIntake();
  for (const spec of schematics) write(path.join(intake, "schematics", `${spec[0]}.schem`), schematic(spec));
  for (const [id, animations] of Object.entries(models)) write(path.join(intake, "modelengine", `${id}.bbmodel`), JSON.stringify(model(id, animations), null, 2));
  for (const [kind, names] of Object.entries(nexo)) {
    for (const name of names) {
      write(path.join(intake, "nexo", "models", "item", "dr001", kind, `${name}.json`), JSON.stringify(nexoModel(kind, name), null, 2));
      write(path.join(intake, "nexo", "textures", "item", "dr001", kind, `${name}.png`), texture(kind, name));
    }
  }
  write(path.join(intake, "nexo", "sounds.json"), JSON.stringify({
    "dr001.roots_whisper": { replace: true, sounds: ["nexo:dr001/roots_whisper"] },
    "dr001.cistern_geyser": { replace: true, sounds: ["nexo:dr001/cistern_geyser"] },
    "dr001.void_crown_pulse": { replace: true, sounds: ["nexo:dr001/void_crown_pulse"] },
    "dr001.aurelion_enrage": { replace: true, sounds: ["nexo:dr001/aurelion_enrage"] }
  }, null, 2));
}

function manifestEntry(file, status, source, target, note) {
  const rel = path.relative(root, file).replaceAll("\\", "/");
  return [
    `    - path: ${rel}`,
    `      sha256: ${sha(file)}`,
    `      status: ${status}`,
    `      source: ${source}`,
    `      target: ${target}`,
    `      notes: "${note}"`
  ].join("\n");
}

function writeManifest() {
  const schematicEntries = schematics.map(([id]) => manifestEntry(path.join(intake, "schematics", `${id}.schem`), "blocked_quality_review", "local procedural schematic authoring", `plugins/Iris/packs/dungeon_ruins_001/schematics/${id}.schem`, "Valid gzipped WorldEdit schematic candidate; requires builder review before final approval."));
  const modelEntries = Object.keys(models).map(id => manifestEntry(path.join(intake, "modelengine", `${id}.bbmodel`), "blocked_quality_review", "local ModelEngine template-derived candidate", `plugins/ModelEngine/blueprints/${id}.bbmodel`, "Technical import candidate with required animation aliases and hitbox data; needs final ModelEngine art pass."));
  const nexoEntries = [];
  for (const [kind, names] of Object.entries(nexo)) for (const name of names) {
    nexoEntries.push(manifestEntry(path.join(intake, "nexo", "models", "item", "dr001", kind, `${name}.json`), "review_ready", "local procedural JSON model", `plugins/Nexo/pack/assets/nexo/models/item/dr001/${kind}/${name}.json`, "JSON references final dr001 texture path."));
    nexoEntries.push(manifestEntry(path.join(intake, "nexo", "textures", "item", "dr001", kind, `${name}.png`), "blocked_quality_review", "local procedural texture", `plugins/Nexo/pack/assets/nexo/textures/item/dr001/${kind}/${name}.png`, "Valid PNG texture candidate; needs art review."));
  }
  const sounds = ["roots_whisper", "cistern_geyser", "void_crown_pulse", "aurelion_enrage"].map(name => [
    `    - path: intake/sounds/dr001/${name}.ogg`,
    "      sha256: missing",
    "      status: missing_authoring_tool",
    "      source: blocked - no local Ogg encoder or approved audio source",
    `      target: plugins/Nexo/pack/assets/nexo/sounds/dr001/${name}.ogg`,
    "      notes: \"Do not fake. Requires authored Ogg audio before approved_final.\""
  ].join("\n"));
  write(path.join(intake, "manifest.yml"), [
    "pack_id: dungeon_ruins_001",
    "display_name: \"Ruiny Zlamanej Korony\"",
    "status: partial_authoring_ready",
    "source_of_truth: content/dungeons/dungeon_ruins_001/intake",
    "review:",
    "  builder_author: codex_local_procedural_candidate",
    "  art_author: codex_local_procedural_candidate",
    "  audio_author: missing_authoring_tool",
    "  reviewer: human_review_required",
    "  reviewed_at: 2026-05-30T00:00:00Z",
    "  approval_status: partial_authoring_ready",
    "checksum_policy:",
    "  algorithm: sha256",
    "  required_before_staging_import: true",
    "license_source_notes:",
    "  local_generated: \"Generated locally by repository script; no external assets downloaded.\"",
    "  template_source: \"ModelEngine candidates may derive from local staging rust_raider.bbmodel where available; requires license/art review before final approval.\"",
    "  audio: \"No final Ogg files generated because local ffmpeg/oggenc/approved audio source is unavailable.\"",
    "schematics:",
    "  required_count: 10",
    "  files:",
    ...schematicEntries,
    "modelengine:",
    "  required_count: 15",
    "  requires_hitboxes: true",
    "  namespace: dr001_",
    "  files:",
    ...modelEntries,
    "nexo:",
    "  required_namespace: dr001",
    "  files:",
    ...nexoEntries,
    "sounds:",
    "  required: true",
    "  files:",
    ...sounds,
    "rejected_legacy_candidates:",
    "  archive: content/dungeons/dungeon_ruins_001/legacy_candidate_archive",
    "  reason: \"dungeon_ruins_001 namespace is legacy scaffold/reference only and not final dr001 intake.\"",
    ""
  ].join("\n"));
}

generateAssets();
writeManifest();
console.log("Generated DR001 final-candidate source bundle. Approval remains blocked pending quality/audio review.");
