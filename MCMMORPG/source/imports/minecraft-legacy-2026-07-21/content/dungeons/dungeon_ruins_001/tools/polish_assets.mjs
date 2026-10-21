import { deflateSync, gzipSync } from "node:zlib";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

const root = new URL("..", import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1");

const roomSpecs = [
  { id: "collapsed_gate_start", w: 31, h: 22, l: 25, theme: "start", type: "gate" },
  { id: "flooded_hallway_01", w: 39, h: 15, l: 17, theme: "start", type: "hall" },
  { id: "root_overgrown_courtyard", w: 47, h: 18, l: 47, theme: "start", type: "courtyard" },
  { id: "broken_archive_room", w: 31, h: 16, l: 27, theme: "start", type: "archive" },
  { id: "moss_chapel_small", w: 23, h: 18, l: 23, theme: "start", type: "chapel" },
  { id: "crystal_bridge_gap", w: 41, h: 20, l: 19, theme: "mid", type: "bridge" },
  { id: "reliquary_archive_mid", w: 45, h: 22, l: 39, theme: "mid", type: "archive" },
  { id: "fractured_rune_pylon", w: 21, h: 24, l: 21, theme: "mid", type: "pylon" },
  { id: "suspended_crystal_chamber", w: 43, h: 26, l: 43, theme: "mid", type: "chamber" },
  { id: "arcane_lock_room", w: 29, h: 18, l: 29, theme: "mid", type: "lock" },
  { id: "ashen_throne_entrance", w: 31, h: 20, l: 25, theme: "end", type: "entrance" },
  { id: "burned_column_hall", w: 41, h: 20, l: 27, theme: "end", type: "hall" },
  { id: "ember_trial_ring", w: 45, h: 20, l: 45, theme: "end", type: "ring" },
  { id: "fallen_king_arena", w: 63, h: 26, l: 63, theme: "end", type: "arena" },
  { id: "reward_vault_end", w: 27, h: 18, l: 27, theme: "end", type: "vault" }
];

const palettes = {
  start: [
    "minecraft:air",
    "minecraft:mossy_stone_bricks",
    "minecraft:cracked_stone_bricks",
    "minecraft:mud_bricks",
    "minecraft:rooted_dirt",
    "minecraft:mangrove_roots",
    "minecraft:vine",
    "minecraft:moss_carpet",
    "minecraft:water",
    "minecraft:glow_lichen"
  ],
  mid: [
    "minecraft:air",
    "minecraft:deepslate_tiles",
    "minecraft:polished_deepslate",
    "minecraft:amethyst_block",
    "minecraft:calcite",
    "minecraft:tinted_glass",
    "minecraft:crying_obsidian",
    "minecraft:amethyst_cluster",
    "minecraft:glow_lichen",
    "minecraft:chain"
  ],
  end: [
    "minecraft:air",
    "minecraft:blackstone",
    "minecraft:basalt",
    "minecraft:polished_basalt",
    "minecraft:nether_bricks",
    "minecraft:magma_block",
    "minecraft:gilded_blackstone",
    "minecraft:chain",
    "minecraft:lava",
    "minecraft:fire"
  ]
};

const nexoAssets = {
  block: ["overgrown_rune_stone", "cracked_reliquary_tile", "ashen_throne_brick", "ember_rune_block"],
  furniture: ["broken_root_altar", "crystal_pylon", "fallen_king_throne", "reward_vault_chest"],
  item: ["mossbound_relic", "cracked_crystal_core", "ashen_crown_fragment", "fallen_king_sigil"]
};

const modelEngine = [
  ["dungeon_ruins_001_root_thrall", "root", false],
  ["dungeon_ruins_001_mossbone_sentinel", "root", false],
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
  fill(0, 1, 0, spec.w - 1, 3, 0, 2);
  fill(0, 1, spec.l - 1, spec.w - 1, 3, spec.l - 1, 2);
  fill(0, 1, 0, 0, 3, spec.l - 1, 2);
  fill(spec.w - 1, 1, 0, spec.w - 1, 3, spec.l - 1, 2);

  if (spec.type === "gate") {
    fill(cx - 6, 1, 2, cx + 6, 10, 3, 2);
    fill(cx - 3, 1, 2, cx + 3, 7, 3, 0);
    fill(cx - 9, 1, 4, cx - 8, 12, 5, 5);
    fill(cx + 8, 1, 4, cx + 9, 12, 5, 5);
  } else if (spec.type === "courtyard" || spec.type === "ring" || spec.type === "arena") {
    for (let a = 0; a < 360; a += 4) {
      const r = spec.type === "arena" ? 24 : 16;
      set(cx + Math.round(Math.cos(a) * r), 1, cz + Math.round(Math.sin(a) * r), spec.theme === "end" ? 5 : 3);
      set(cx + Math.round(Math.cos(a) * (r - 5)), 1, cz + Math.round(Math.sin(a) * (r - 5)), spec.theme === "mid" ? 3 : 7);
    }
    for (const [dx, dz] of [[-10, -10], [10, -10], [-10, 10], [10, 10]]) fill(cx + dx - 1, 1, cz + dz - 1, cx + dx + 1, 6, cz + dz + 1, spec.theme === "end" ? 6 : 5);
  } else if (spec.type === "bridge") {
    fill(2, 1, cz - 2, spec.w - 3, 1, cz + 2, 1);
    fill(cx - 4, 1, cz - 2, cx + 4, 1, cz + 2, 0);
    fill(cx - 5, 1, cz - 4, cx - 4, 4, cz + 4, 3);
    fill(cx + 4, 1, cz - 4, cx + 5, 4, cz + 4, 3);
  } else if (spec.type === "pylon" || spec.type === "lock") {
    fill(cx - 2, 1, cz - 2, cx + 2, 10, cz + 2, spec.theme === "mid" ? 3 : 6);
    fill(cx - 4, 1, cz - 4, cx + 4, 1, cz + 4, 4);
  } else if (spec.type === "vault") {
    fill(cx - 5, 1, cz - 5, cx + 5, 6, cz - 4, 6);
    fill(cx - 3, 1, cz - 5, cx + 3, 3, cz - 4, 0);
    fill(cx - 2, 1, cz + 3, cx + 2, 3, cz + 5, 6);
  } else {
    fill(cx - 2, 1, 2, cx + 2, 1, spec.l - 3, spec.theme === "start" ? 8 : spec.theme === "mid" ? 3 : 5);
    for (let z = 4; z < spec.l - 4; z += 7) {
      fill(4, 1, z, 5, 7, z + 1, spec.theme === "end" ? 2 : 4);
      fill(spec.w - 6, 1, z, spec.w - 5, 7, z + 1, spec.theme === "end" ? 2 : 4);
    }
  }

  for (let z = 2; z < spec.l - 2; z++) for (let x = 2; x < spec.w - 2; x++) {
    if ((x * 13 + z * 17 + spec.id.length) % 31 === 0) set(x, 1, z, spec.theme === "start" ? 7 : spec.theme === "mid" ? 8 : 9);
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
      tagString("Author", "Codex authored procedural room pass"),
      tagString("Note", `Designed ${spec.type} room for ${spec.theme} stage; builder review still required before live.`)
    ])
  ]));
}

function texture(kind, name) {
  const base = name.includes("crystal") || name.includes("reliquary")
    ? [[20, 44, 62, 255], [64, 218, 240, 255], [139, 82, 214, 255], [225, 245, 255, 255]]
    : name.includes("ashen") || name.includes("ember") || name.includes("king") || name.includes("throne")
      ? [[22, 20, 20, 255], [82, 60, 48, 255], [225, 87, 32, 255], [190, 142, 62, 255]]
      : [[31, 61, 37, 255], [91, 128, 67, 255], [65, 48, 34, 255], [150, 170, 110, 255]];
  return png(32, 32, (x, y) => {
    if (kind === "item") {
      const dx = Math.abs(x - 16), dy = Math.abs(y - 16);
      if (dx + dy > 20 || (dx > 12 && dy > 10)) return [0, 0, 0, 0];
    }
    const crack = (x === y || x + y === 31 || (x * 5 + y * 3 + name.length) % 23 === 0);
    const rune = (x > 12 && x < 20 && (y === 8 || y === 16 || y === 24)) || (y > 10 && y < 23 && (x === 12 || x === 20));
    if (rune && (name.includes("rune") || name.includes("crystal") || name.includes("sigil"))) return base[3];
    if (crack) return base[2];
    return base[(x * 11 + y * 7 + name.length) % 2];
  });
}

function model(kind, name) {
  const tex = `nexo:item/dungeon_ruins_001/${kind}/${name}`;
  if (kind === "block") return { parent: "minecraft:block/cube_all", textures: { all: tex } };
  if (kind === "furniture") {
    return {
      credit: "Codex source scaffold",
      textures: { particle: tex, all: tex },
      elements: [
        { from: [3, 0, 3], to: [13, 2, 13], faces: faceRefs("#all") },
        { from: [5, 2, 5], to: [11, 12, 11], faces: faceRefs("#all") },
        { from: [2, 12, 2], to: [14, 15, 14], faces: faceRefs("#all") }
      ]
    };
  }
  return { parent: "minecraft:item/generated", textures: { layer0: tex } };
}

function faceRefs(texture) {
  return {
    north: { texture }, south: { texture }, east: { texture }, west: { texture }, up: { texture }, down: { texture }
  };
}

function bbmodel(id, archetype, boss) {
  const color = archetype.includes("crystal") ? 2 : archetype.includes("ash") || archetype.includes("king") ? 4 : 1;
  const tall = boss ? 30 : archetype.includes("shard") ? 12 : 24;
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
            { channel: "rotation", time: 0.4, data_points: [{ x: name === "attack" ? -12 : 0, y: 0, z: name === "walk" ? 4 : 0 }] }
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
    resolution: { width: 64, height: 64 },
    elements,
    outliner: elements.map(e => e.name),
    textures: [],
    animations
  };
}

function cube(name, from, to, color) {
  return {
    name, box_uv: false, render_order: "default", locked: false, from, to, autouv: 0, color,
    origin: [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2],
    uv_offset: [0, 0],
    faces: {
      north: { uv: [0, 0, 8, 8], texture: null },
      east: { uv: [0, 0, 8, 8], texture: null },
      south: { uv: [0, 0, 8, 8], texture: null },
      west: { uv: [0, 0, 8, 8], texture: null },
      up: { uv: [0, 0, 8, 8], texture: null },
      down: { uv: [0, 0, 8, 8], texture: null }
    }
  };
}

for (const spec of roomSpecs) write(join(root, "iris", "schematics", `${spec.id}.schem`), schematic(spec));

write(join(root, "iris", "pack", "images", "dungeon_ruins_001_layout.png"), png(256, 256, (x, y) => {
  const path = Math.abs(y - (128 + Math.sin(x / 22) * 24));
  if (x < 82 && path < 34) return [44, 92, 52, 255];
  if (x >= 82 && x < 172 && path < 30) return [52, 108, 136, 255];
  if (x >= 172 && path < 42) return [96, 42, 26, 255];
  if ((x - 42) ** 2 + (y - 128) ** 2 < 900) return [62, 105, 62, 255];
  if ((x - 128) ** 2 + (y - 128) ** 2 < 1100) return [66, 92, 132, 255];
  if ((x - 216) ** 2 + (y - 128) ** 2 < 1400) return [105, 48, 30, 255];
  return [0, 0, 0, 255];
}));

for (const [kind, names] of Object.entries(nexoAssets)) {
  for (const name of names) {
    write(join(root, "resourcepack", "assets", "nexo", "models", "item", "dungeon_ruins_001", kind, `${name}.json`), JSON.stringify(model(kind, name), null, 2));
    write(join(root, "resourcepack", "assets", "nexo", "textures", "item", "dungeon_ruins_001", kind, `${name}.png`), texture(kind, name));
  }
}

write(join(root, "resourcepack", "assets", "nexo", "sounds.json"), JSON.stringify({
  "dungeon_ruins_001.root_ambient": { replace: false, sounds: ["minecraft:ambient/cave/cave1", "minecraft:block/mangrove_roots/break"] },
  "dungeon_ruins_001.crystal_chime": { replace: false, sounds: ["minecraft:block/amethyst_block/chime", "minecraft:block/amethyst_cluster/place"] },
  "dungeon_ruins_001.ember_trial": { replace: false, sounds: ["minecraft:block/fire/ambient", "minecraft:block/lava/pop"] }
}, null, 2));

write(join(root, "resourcepack", "CUSTOM_AUDIO_REQUIRED.md"), [
  "# Custom Audio Required",
  "",
  "No real custom `.ogg` files were generated in this environment.",
  "",
  "Reason: the workspace exposes Node and Java but no Vorbis/Ogg encoder such as ffmpeg, oggenc, or an installed Node audio package. Writing fake `.ogg` files would break the resource pack, so this pack uses vanilla sound mappings in `assets/nexo/sounds.json` until audio is produced by an audio tool.",
  "",
  "Required final files if custom audio is desired:",
  "",
  "- `assets/nexo/sounds/dungeon_ruins_001/root_ambient.ogg`",
  "- `assets/nexo/sounds/dungeon_ruins_001/crystal_chime.ogg`",
  "- `assets/nexo/sounds/dungeon_ruins_001/ember_trial.ogg`",
  "",
  "Acceptance: each file must be valid Ogg Vorbis, under 10 seconds for ambient stingers, and referenced from `assets/nexo/sounds.json`.",
  ""
].join("\n"));

for (const [id, archetype, boss] of modelEngine) write(join(root, "modelengine", "blueprints", `${id}.bbmodel`), JSON.stringify(bbmodel(id, archetype, boss), null, 2));

write(join(root, "ASSET_POLISH_REPORT.md"), [
  "# Asset Polish Report",
  "",
  "- Replaced first-pass massing schematics with more detailed authored procedural rooms.",
  "- Increased layout image to 256x256 with distinct START/MID/END route zones.",
  "- Rebuilt Nexo textures at 32x32 with stronger rune/crack/readability patterns.",
  "- Rebuilt furniture item models as shaped JSON elements instead of flat generated items.",
  "- Rebuilt ModelEngine `.bbmodel` files with archetype-specific attachments and non-empty animation keyframes.",
  "- Kept custom audio as a documented blocker instead of generating invalid `.ogg` files.",
  "",
  "These assets are still source scaffolds and require builder/artist review before live deployment.",
  ""
].join("\n"));
