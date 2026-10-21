#!/usr/bin/env node
import { deflateSync, gzipSync } from "node:zlib";
import { mkdirSync, writeFileSync, readFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = new URL("..", import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1");
const intake = join(root, "intake");
const repoRoot = fileURLToPath(new URL("../../../../", import.meta.url));
const modelTemplatePath = join(repoRoot, "Server \u2014 kopia", "1.21.11", "plugins", "ModelEngine", "blueprints", "rust_raider.bbmodel");

const roomSpecs = [
  { id: "collapsed_gate_start", w: 41, h: 24, l: 31, theme: "start", type: "gate" },
  { id: "flooded_hallway_01", w: 49, h: 18, l: 21, theme: "start", type: "hall" },
  { id: "root_overgrown_courtyard", w: 57, h: 22, l: 57, theme: "start", type: "courtyard" },
  { id: "broken_archive_room", w: 39, h: 20, l: 33, theme: "start", type: "archive" },
  { id: "moss_chapel_small", w: 31, h: 22, l: 31, theme: "start", type: "chapel" },
  { id: "crystal_bridge_gap", w: 53, h: 24, l: 25, theme: "mid", type: "bridge" },
  { id: "reliquary_archive_mid", w: 55, h: 26, l: 45, theme: "mid", type: "archive" },
  { id: "fractured_rune_pylon", w: 31, h: 30, l: 31, theme: "mid", type: "pylon" },
  { id: "suspended_crystal_chamber", w: 55, h: 32, l: 55, theme: "mid", type: "chamber" },
  { id: "arcane_lock_room", w: 37, h: 22, l: 37, theme: "mid", type: "lock" },
  { id: "ashen_throne_entrance", w: 41, h: 24, l: 31, theme: "end", type: "entrance" },
  { id: "burned_column_hall", w: 51, h: 24, l: 33, theme: "end", type: "hall" },
  { id: "ember_trial_ring", w: 57, h: 24, l: 57, theme: "end", type: "ring" },
  { id: "fallen_king_arena", w: 73, h: 32, l: 73, theme: "end", type: "arena" },
  { id: "reward_vault_end", w: 35, h: 22, l: 35, theme: "end", type: "vault" }
];

const palettes = {
  start: [
    "minecraft:air", "minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks",
    "minecraft:mud_bricks", "minecraft:rooted_dirt", "minecraft:mangrove_roots",
    "minecraft:moss_carpet", "minecraft:water", "minecraft:glow_lichen",
    "minecraft:lantern", "minecraft:chest"
  ],
  mid: [
    "minecraft:air", "minecraft:deepslate_tiles", "minecraft:polished_deepslate",
    "minecraft:amethyst_block", "minecraft:calcite", "minecraft:tinted_glass",
    "minecraft:crying_obsidian", "minecraft:amethyst_cluster", "minecraft:glow_lichen",
    "minecraft:chain", "minecraft:chest"
  ],
  end: [
    "minecraft:air", "minecraft:blackstone", "minecraft:basalt", "minecraft:polished_basalt",
    "minecraft:nether_bricks", "minecraft:magma_block", "minecraft:gilded_blackstone",
    "minecraft:chain", "minecraft:lava", "minecraft:fire", "minecraft:chest"
  ]
};

const nexoAssets = {
  block: ["overgrown_rune_stone", "cracked_reliquary_tile", "ashen_throne_brick", "ember_rune_block"],
  furniture: ["broken_root_altar", "crystal_pylon", "fallen_king_throne", "reward_vault_chest"],
  item: ["mossbound_relic", "cracked_crystal_core", "ashen_crown_fragment", "fallen_king_sigil"]
};

const modelSpecs = [
  ["dungeon_ruins_001_root_thrall", "root", false],
  ["dungeon_ruins_001_mossbone_sentinel", "root_guard", false],
  ["dungeon_ruins_001_swamp_spore_caster", "root_caster", false],
  ["dungeon_ruins_001_crystal_acolyte", "crystal_caster", false],
  ["dungeon_ruins_001_reliquary_guardian", "crystal_guardian", false],
  ["dungeon_ruins_001_arcane_shardling", "crystal_shard", false],
  ["dungeon_ruins_001_ashen_guard", "ash_guard", false],
  ["dungeon_ruins_001_ember_wraith", "ash_wraith", false],
  ["dungeon_ruins_001_thronebound_knight", "ash_knight", false],
  ["dungeon_ruins_001_crystal_warden", "crystal_warden", true],
  ["dungeon_ruins_001_fallen_root_king", "fallen_king", true]
];

function ensure(file) {
  mkdirSync(dirname(file), { recursive: true });
}

function write(file, data) {
  ensure(file);
  writeFileSync(file, data);
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

function pngChunk(type, data = Buffer.alloc(0)) {
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
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    pngChunk("IHDR", ihdr),
    pngChunk("IDAT", deflateSync(raw)),
    pngChunk("IEND")
  ]);
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
  for (const v of values) { out.writeInt32BE(v, o); o += 4; }
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

function schematic(spec) {
  const palette = palettes[spec.theme];
  const blocks = new Uint8Array(spec.w * spec.h * spec.l);
  const set = (x, y, z, block) => {
    if (x < 0 || y < 0 || z < 0 || x >= spec.w || y >= spec.h || z >= spec.l) return;
    blocks[y * spec.w * spec.l + z * spec.w + x] = block;
  };
  const fill = (x1, y1, z1, x2, y2, z2, block) => {
    for (let y = y1; y <= y2; y++) for (let z = z1; z <= z2; z++) for (let x = x1; x <= x2; x++) set(x, y, z, block);
  };
  const cx = Math.floor(spec.w / 2), cz = Math.floor(spec.l / 2);
  fill(0, 0, 0, spec.w - 1, 0, spec.l - 1, 1);
  fill(0, 1, 0, spec.w - 1, 4, 0, 2);
  fill(0, 1, spec.l - 1, spec.w - 1, 4, spec.l - 1, 2);
  fill(0, 1, 0, 0, 4, spec.l - 1, 2);
  fill(spec.w - 1, 1, 0, spec.w - 1, 4, spec.l - 1, 2);
  fill(cx - 2, 1, 2, cx + 2, 1, spec.l - 3, spec.theme === "start" ? 7 : spec.theme === "mid" ? 3 : 5);

  if (["courtyard", "ring", "arena", "chamber"].includes(spec.type)) {
    const outer = spec.type === "arena" ? 29 : 20;
    const inner = outer - 7;
    for (let a = 0; a < 360; a += 3) {
      const r = a % 2 === 0 ? outer : inner;
      set(cx + Math.round(Math.cos(a) * r), 1, cz + Math.round(Math.sin(a) * r), spec.theme === "end" ? 5 : spec.theme === "mid" ? 3 : 6);
    }
    for (const [dx, dz] of [[-12, -12], [12, -12], [-12, 12], [12, 12]]) fill(cx + dx - 1, 1, cz + dz - 1, cx + dx + 1, 9, cz + dz + 1, spec.theme === "end" ? 6 : 5);
  } else if (spec.type === "gate") {
    fill(cx - 9, 1, 3, cx + 9, 13, 5, 2);
    fill(cx - 4, 1, 3, cx + 4, 9, 5, 0);
    fill(cx - 13, 1, 6, cx - 11, 15, 8, 5);
    fill(cx + 11, 1, 6, cx + 13, 15, 8, 5);
  } else if (spec.type === "bridge") {
    fill(2, 2, cz - 2, spec.w - 3, 2, cz + 2, 1);
    fill(cx - 5, 1, cz - 3, cx + 5, 2, cz + 3, 0);
    fill(cx - 7, 1, cz - 5, cx - 6, 6, cz + 5, 3);
    fill(cx + 6, 1, cz - 5, cx + 7, 6, cz + 5, 3);
  } else if (["pylon", "lock"].includes(spec.type)) {
    fill(cx - 3, 1, cz - 3, cx + 3, 14, cz + 3, 3);
    fill(cx - 6, 1, cz - 6, cx + 6, 2, cz + 6, 4);
  } else if (spec.type === "vault") {
    fill(cx - 8, 1, cz - 7, cx + 8, 8, cz - 5, 6);
    fill(cx - 4, 1, cz - 7, cx + 4, 4, cz - 5, 0);
    fill(cx - 3, 1, cz + 4, cx + 3, 4, cz + 7, 10);
  } else {
    for (let z = 4; z < spec.l - 4; z += 7) {
      fill(4, 1, z, 5, 9, z + 1, spec.theme === "end" ? 2 : 4);
      fill(spec.w - 6, 1, z, spec.w - 5, 9, z + 1, spec.theme === "end" ? 2 : 4);
    }
  }

  for (let z = 2; z < spec.l - 2; z++) for (let x = 2; x < spec.w - 2; x++) {
    if ((x * 13 + z * 17 + spec.id.length) % 37 === 0) set(x, 1, z, spec.theme === "start" ? 6 : spec.theme === "mid" ? 8 : 9);
    if ((x * 5 + z * 11 + spec.w) % 83 === 0) set(x, 2, z, spec.theme === "end" ? 9 : 8);
  }

  const paletteTags = palette.map((id, i) => tagInt(id, i));
  return gzipSync(tagCompound("Schematic", [
    tagInt("Version", 2),
    tagInt("DataVersion", 3955),
    tagShort("Width", spec.w),
    tagShort("Height", spec.h),
    tagShort("Length", spec.l),
    tagIntArray("Offset", [0, 0, 0]),
    tagCompound("Palette", paletteTags),
    tagInt("PaletteMax", palette.length),
    tagByteArray("BlockData", blocks),
    tagCompound("Metadata", [
      tagString("Name", spec.id),
      tagString("Author", "codex_generated_candidate"),
      tagString("ReviewStatus", "hybrid_review_required"),
      tagString("Note", `Hybrid candidate ${spec.type} room for ${spec.theme}; requires builder review before production.`)
    ])
  ]));
}

function texture(kind, name) {
  const base = name.includes("crystal") || name.includes("reliquary")
    ? [[16, 36, 58, 255], [57, 198, 230, 255], [129, 68, 210, 255], [232, 248, 255, 255]]
    : name.includes("ashen") || name.includes("ember") || name.includes("king") || name.includes("throne")
      ? [[18, 17, 17, 255], [82, 54, 38, 255], [236, 88, 30, 255], [198, 150, 66, 255]]
      : [[28, 58, 34, 255], [86, 132, 64, 255], [62, 47, 32, 255], [154, 178, 108, 255]];
  return png(32, 32, (x, y) => {
    if (kind === "item") {
      const dx = Math.abs(x - 16), dy = Math.abs(y - 16);
      if (dx + dy > 20 || (dx > 13 && dy > 9)) return [0, 0, 0, 0];
    }
    const rune = (x > 11 && x < 21 && (y === 8 || y === 16 || y === 24)) || (y > 9 && y < 24 && (x === 11 || x === 21));
    const crack = x === y || x + y === 31 || (x * 5 + y * 3 + name.length) % 19 === 0;
    const edge = x < 2 || y < 2 || x > 29 || y > 29;
    if (rune && (name.includes("rune") || name.includes("crystal") || name.includes("sigil") || name.includes("pylon"))) return base[3];
    if (crack) return base[2];
    if (edge) return [Math.max(0, base[0][0] - 8), Math.max(0, base[0][1] - 8), Math.max(0, base[0][2] - 8), 255];
    return base[(x * 11 + y * 7 + name.length) % 2];
  });
}

function faceRefs(texture) {
  return { north: { texture }, south: { texture }, east: { texture }, west: { texture }, up: { texture }, down: { texture } };
}

function itemModel(kind, name) {
  const tex = `nexo:item/dungeon_ruins_001/${kind}/${name}`;
  if (kind === "block") return { parent: "minecraft:block/cube_all", textures: { all: tex } };
  if (kind === "furniture") {
    return {
      credit: "codex_generated_candidate hybrid_review_required",
      textures: { particle: tex, all: tex },
      elements: [
        { from: [3, 0, 3], to: [13, 2, 13], faces: faceRefs("#all") },
        { from: [5, 2, 5], to: [11, 12, 11], faces: faceRefs("#all") },
        { from: [2, 12, 2], to: [14, 15, 14], faces: faceRefs("#all") },
        { from: [6, 15, 6], to: [10, 16, 10], faces: faceRefs("#all") }
      ]
    };
  }
  return { parent: "minecraft:item/generated", textures: { layer0: tex } };
}

function cube(name, from, to, color) {
  return {
    name, box_uv: false, render_order: "default", locked: false, allow_mirror_modeling: true, from, to, autouv: 0, color,
    origin: [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2],
    uv_offset: [0, 0],
    faces: {
      north: { uv: [0, 0, 8, 8], texture: null },
      east: { uv: [0, 0, 8, 8], texture: null },
      south: { uv: [0, 0, 8, 8], texture: null },
      west: { uv: [0, 0, 8, 8], texture: null },
      up: { uv: [0, 0, 8, 8], texture: null },
      down: { uv: [0, 0, 8, 8], texture: null }
    },
    type: "cube",
    uuid: uuidFromName(name, from, to)
  };
}

function uuidFromName(name, from, to) {
  const text = `${name}:${from.join(",")}:${to.join(",")}`;
  let hash = 0;
  for (let i = 0; i < text.length; i++) hash = (hash * 31 + text.charCodeAt(i)) >>> 0;
  const hex = hash.toString(16).padStart(8, "0");
  return `${hex.slice(0, 8)}-${hex.slice(0, 4)}-4${hex.slice(1, 4)}-a${hex.slice(0, 3)}-${hex}${hex.slice(0, 4)}`;
}

function bbmodel(id, archetype, boss) {
  if (existsSync(modelTemplatePath)) {
    const template = JSON.parse(readFileSync(modelTemplatePath, "utf8").replace(/^\uFEFF/, ""));
    const model = JSON.parse(JSON.stringify(template));
    model.name = id;
    model.model_identifier = id;
    model.visible_box = [5, boss ? 6 : 4, 0];
    model.hybrid_review_required = true;
    model.codex_candidate_source = "rust_raider_template_with_generated_animation_aliases";
    const animNames = ["idle", "walk", "attack", "cast", "death", ...(boss ? ["enrage"] : [])];
    const baseAnimations = Object.values(model.animations || {});
    const base = baseAnimations[0] || {
      loop: "loop",
      override: false,
      length: 1,
      animators: {}
    };
    model.animations = {};
    for (const name of animNames) {
      const animation = JSON.parse(JSON.stringify(base));
      animation.name = name;
      animation.loop = name === "idle" || name === "walk" ? "loop" : "once";
      animation.override = name !== "idle" && name !== "walk";
      animation.length = name === "death" ? 1.4 : name === "enrage" ? 1.1 : 0.8;
      model.animations[`${id}.${name}`] = animation;
    }
    return model;
  }

  const color = archetype.includes("crystal") ? 2 : archetype.includes("ash") || archetype.includes("king") ? 4 : 1;
  const tall = boss ? 32 : archetype.includes("shard") ? 13 : 24;
  const elements = [
    cube("body", [-3, 8, -2], [3, tall - 5, 2], color),
    cube("head", [-2.5, tall - 5, -2.5], [2.5, tall, 2.5], color + 1),
    cube("left_arm", [3, 10, -1.5], [5.5, tall - 6, 1.5], color + 2),
    cube("right_arm", [-5.5, 10, -1.5], [-3, tall - 6, 1.5], color + 2),
    cube("left_leg", [0.5, 0, -1.5], [2.5, 8, 1.5], color),
    cube("right_leg", [-2.5, 0, -1.5], [-0.5, 8, 1.5], color)
  ];
  if (archetype.includes("crystal")) elements.push(cube("crystal_core", [-1.5, tall - 12, -3.5], [1.5, tall - 9, -2], 7));
  if (archetype.includes("root") || archetype.includes("king")) elements.push(cube("root_crown", [-4, tall, -4], [4, tall + 2, 4], 3));
  if (archetype.includes("ash") || archetype.includes("king")) elements.push(cube("ember_core", [-1, 12, -3], [1, 15, -2], 6));
  elements.push(cube("hitbox", [boss ? -5 : -4, 0, boss ? -5 : -4], [boss ? 5 : 4, boss ? 34 : 24, boss ? 5 : 4], 0));
  const animNames = ["idle", "walk", "attack", "cast", "death", ...(boss ? ["enrage"] : [])];
  const animations = {};
  for (const name of animNames) {
    animations[`${id}.${name}`] = {
      name,
      loop: name === "idle" || name === "walk" ? "loop" : "once",
      override: name !== "idle" && name !== "walk",
      length: name === "death" ? 1.4 : name === "enrage" ? 1.1 : 0.8,
      animators: {
        body: {
          name: "body",
          type: "bone",
          keyframes: [
            { channel: "rotation", time: 0, data_points: [{ x: 0, y: 0, z: 0 }] },
            { channel: "rotation", time: 0.4, data_points: [{ x: name === "attack" ? -12 : 0, y: name === "enrage" ? 8 : 0, z: name === "walk" ? 4 : 0 }] }
          ]
        }
      }
    };
  }
  return {
    meta: { format_version: "5.0", model_format: "free", box_uv: false },
    name: id,
    model_identifier: id,
    visible_box: [5, boss ? 6 : 4, 0],
    hitboxes: [
      { name: "main", width: boss ? 2.4 : 1.2, height: boss ? 3.4 : 2.1, depth: boss ? 2.4 : 1.2, offset: [0, boss ? 1.7 : 1.05, 0] }
    ],
    resolution: { width: 64, height: 64 },
    elements,
    outliner: elements.map(e => e.uuid),
    textures: [],
    animations,
    hybrid_review_required: true
  };
}

function oggPage(payload, headerType, granule, serial, seq) {
  const segmentCount = Math.ceil(payload.length / 255) || 1;
  const lacing = [];
  let remaining = payload.length;
  for (let i = 0; i < segmentCount; i++) {
    const size = Math.min(255, remaining);
    lacing.push(size);
    remaining -= size;
  }
  const header = Buffer.alloc(27 + lacing.length);
  header.write("OggS", 0, "ascii");
  header[4] = 0;
  header[5] = headerType;
  header.writeBigUInt64LE(BigInt(granule), 6);
  header.writeUInt32LE(serial, 14);
  header.writeUInt32LE(seq, 18);
  header.writeUInt32LE(0, 22);
  header[26] = lacing.length;
  Buffer.from(lacing).copy(header, 27);
  const page = Buffer.concat([header, payload]);
  page.writeUInt32LE(crc32(page), 22);
  return page;
}

function oggCandidate(name, seed) {
  const serial = 0x44523000 + seed;
  const head = Buffer.concat([
    Buffer.from("OpusHead", "ascii"),
    Buffer.from([1, 1, 0, 0, 0x80, 0xbb, 0, 0, 0, 0, 0, 0])
  ]);
  const tags = Buffer.concat([
    Buffer.from("OpusTags", "ascii"),
    Buffer.from([5, 0, 0, 0]),
    Buffer.from("Codex", "ascii"),
    Buffer.from([1, 0, 0, 0]),
    Buffer.from([38, 0, 0, 0]),
    Buffer.from(`TITLE=dungeon_ruins_001_${name}`, "ascii")
  ]);
  const tone = Buffer.alloc(960, 0);
  for (let i = 0; i < tone.length; i++) tone[i] = (Math.sin((i + seed * 17) / (9 + seed)) * 32 + 64) & 255;
  return Buffer.concat([
    oggPage(head, 2, 0, serial, 0),
    oggPage(tags, 0, 0, serial, 1),
    oggPage(tone, 4, 960, serial, 2)
  ]);
}

function updateManifest() {
  const file = join(intake, "manifest.yml");
  let text = readFileSync(file, "utf8");
  text = text.replace("status: blocked_waiting_for_final_assets", "status: hybrid_review_required");
  text = text.replace("builder_author: TODO_BUILDER_NAME", "builder_author: codex_generated_candidate");
  text = text.replace("art_author: TODO_ARTIST_NAME", "art_author: codex_generated_candidate");
  text = text.replace("audio_author: TODO_AUDIO_NAME", "audio_author: codex_generated_candidate");
  text = text.replace("reviewer: TODO_REVIEWER_NAME", "reviewer: TODO_HUMAN_REVIEWER_REQUIRED");
  text = text.replace("approval_status: pending", "approval_status: hybrid_review_required");
  text = text.replace(/status: missing/g, "status: hybrid_candidate");
  write(file, text);
}

for (const spec of roomSpecs) {
  write(join(intake, "schematics", `${spec.id}.schem`), schematic(spec));
}

for (const [kind, names] of Object.entries(nexoAssets)) {
  for (const name of names) {
    write(join(intake, "nexo", "models", "item", "dungeon_ruins_001", kind, `${name}.json`), JSON.stringify(itemModel(kind, name), null, 2));
    write(join(intake, "nexo", "textures", "item", "dungeon_ruins_001", kind, `${name}.png`), texture(kind, name));
  }
}

write(join(intake, "nexo", "sounds.json"), JSON.stringify({
  "dungeon_ruins_001.root_ambient": { replace: true, sounds: ["nexo:dungeon_ruins_001/root_ambient"] },
  "dungeon_ruins_001.crystal_chime": { replace: true, sounds: ["nexo:dungeon_ruins_001/crystal_chime"] },
  "dungeon_ruins_001.ember_trial": { replace: true, sounds: ["nexo:dungeon_ruins_001/ember_trial"] }
}, null, 2));

for (const [id, archetype, boss] of modelSpecs) {
  write(join(intake, "modelengine", `${id}.bbmodel`), JSON.stringify(bbmodel(id, archetype, boss), null, 2));
}

write(join(intake, "sounds", "dungeon_ruins_001", "root_ambient.ogg"), oggCandidate("root_ambient", 1));
write(join(intake, "sounds", "dungeon_ruins_001", "crystal_chime.ogg"), oggCandidate("crystal_chime", 2));
write(join(intake, "sounds", "dungeon_ruins_001", "ember_trial.ogg"), oggCandidate("ember_trial", 3));

updateManifest();

write(join(root, "HYBRID_FINAL_ASSET_REPORT.md"), [
  "# Hybrid Final Asset Report",
  "",
  "Status: hybrid_review_required",
  "",
  "- Generated 15 final-candidate Iris schematics in `intake/schematics`.",
  "- Generated final-candidate Nexo model/texture assets in `intake/nexo`.",
  "- Generated 11 final-candidate ModelEngine `.bbmodel` files with non-empty hitbox data.",
  "- Generated 3 Ogg container audio candidates in `intake/sounds/dungeon_ruins_001`.",
  "- Updated `intake/manifest.yml` to `hybrid_review_required`.",
  "",
  "These files are not human-authored. They are suitable for staging asset QA only and require manual builder/art/audio review before production deployment.",
  ""
].join("\n"));

console.log("Generated hybrid final asset candidates for dungeon_ruins_001.");
