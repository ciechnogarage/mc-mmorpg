import { deflateSync, gzipSync } from "node:zlib";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

const root = new URL("..", import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, "$1");

const schematics = [
  ["collapsed_gate_start", 31, 20, 24, "start"],
  ["flooded_hallway_01", 37, 14, 17, "start"],
  ["root_overgrown_courtyard", 45, 16, 45, "start"],
  ["broken_archive_room", 29, 14, 25, "start"],
  ["moss_chapel_small", 21, 16, 21, "start"],
  ["crystal_bridge_gap", 39, 18, 17, "mid"],
  ["reliquary_archive_mid", 43, 20, 37, "mid"],
  ["fractured_rune_pylon", 19, 22, 19, "mid"],
  ["suspended_crystal_chamber", 41, 24, 41, "mid"],
  ["arcane_lock_room", 27, 16, 27, "mid"],
  ["ashen_throne_entrance", 29, 18, 23, "end"],
  ["burned_column_hall", 39, 18, 25, "end"],
  ["ember_trial_ring", 43, 18, 43, "end"],
  ["fallen_king_arena", 61, 24, 61, "end"],
  ["reward_vault_end", 25, 16, 25, "end"]
];

const nexoModels = {
  block: [
    "overgrown_rune_stone",
    "cracked_reliquary_tile",
    "ashen_throne_brick",
    "ember_rune_block"
  ],
  furniture: [
    "broken_root_altar",
    "crystal_pylon",
    "fallen_king_throne",
    "reward_vault_chest"
  ],
  item: [
    "mossbound_relic",
    "cracked_crystal_core",
    "ashen_crown_fragment",
    "fallen_king_sigil"
  ]
};

const modelEngineModels = [
  ["dungeon_ruins_001_root_thrall", "root"],
  ["dungeon_ruins_001_mossbone_sentinel", "root"],
  ["dungeon_ruins_001_swamp_spore_caster", "root"],
  ["dungeon_ruins_001_crystal_acolyte", "crystal"],
  ["dungeon_ruins_001_reliquary_guardian", "crystal"],
  ["dungeon_ruins_001_arcane_shardling", "crystal"],
  ["dungeon_ruins_001_ashen_guard", "ash"],
  ["dungeon_ruins_001_ember_wraith", "ash"],
  ["dungeon_ruins_001_thronebound_knight", "ash"],
  ["dungeon_ruins_001_crystal_warden", "crystal_boss"],
  ["dungeon_ruins_001_fallen_root_king", "ash_boss"]
];

function ensure(path) {
  mkdirSync(dirname(path), { recursive: true });
}

function write(path, data) {
  ensure(path);
  writeFileSync(path, data);
}

function crc32(buffer) {
  let table = crc32.table;
  if (!table) {
    table = crc32.table = new Uint32Array(256);
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      table[n] = c >>> 0;
    }
  }
  let c = 0xffffffff;
  for (const byte of buffer) c = table[(c ^ byte) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data = Buffer.alloc(0)) {
  const typeBuf = Buffer.from(type);
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
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
      raw[i] = r;
      raw[i + 1] = g;
      raw[i + 2] = b;
      raw[i + 3] = a;
    }
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;
  ihdr[9] = 6;
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk("IHDR", ihdr),
    chunk("IDAT", deflateSync(raw)),
    chunk("IEND")
  ]);
}

function nbtString(name, value) {
  const n = Buffer.from(name);
  const v = Buffer.from(value);
  const out = Buffer.alloc(1 + 2 + n.length + 2 + v.length);
  let o = 0;
  out[o++] = 8;
  out.writeUInt16BE(n.length, o); o += 2; n.copy(out, o); o += n.length;
  out.writeUInt16BE(v.length, o); o += 2; v.copy(out, o);
  return out;
}

function nbtShort(name, value) {
  const n = Buffer.from(name);
  const out = Buffer.alloc(1 + 2 + n.length + 2);
  let o = 0;
  out[o++] = 2;
  out.writeUInt16BE(n.length, o); o += 2; n.copy(out, o); o += n.length;
  out.writeInt16BE(value, o);
  return out;
}

function nbtInt(name, value) {
  const n = Buffer.from(name);
  const out = Buffer.alloc(1 + 2 + n.length + 4);
  let o = 0;
  out[o++] = 3;
  out.writeUInt16BE(n.length, o); o += 2; n.copy(out, o); o += n.length;
  out.writeInt32BE(value, o);
  return out;
}

function nbtIntArray(name, values) {
  const n = Buffer.from(name);
  const out = Buffer.alloc(1 + 2 + n.length + 4 + values.length * 4);
  let o = 0;
  out[o++] = 11;
  out.writeUInt16BE(n.length, o); o += 2; n.copy(out, o); o += n.length;
  out.writeInt32BE(values.length, o); o += 4;
  for (const value of values) {
    out.writeInt32BE(value, o);
    o += 4;
  }
  return out;
}

function nbtByteArray(name, values) {
  const n = Buffer.from(name);
  const out = Buffer.alloc(1 + 2 + n.length + 4 + values.length);
  let o = 0;
  out[o++] = 7;
  out.writeUInt16BE(n.length, o); o += 2; n.copy(out, o); o += n.length;
  out.writeInt32BE(values.length, o); o += 4;
  Buffer.from(values).copy(out, o);
  return out;
}

function nbtCompound(name, children) {
  const n = Buffer.from(name);
  return Buffer.concat([
    Buffer.from([10, n.length >> 8, n.length & 255]),
    n,
    ...children,
    Buffer.from([0])
  ]);
}

function makeSchematic(width, height, length, theme) {
  const palette = {
    start: ["minecraft:air", "minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:water", "minecraft:mangrove_roots"],
    mid: ["minecraft:air", "minecraft:deepslate_tiles", "minecraft:amethyst_block", "minecraft:calcite", "minecraft:tinted_glass"],
    end: ["minecraft:air", "minecraft:blackstone", "minecraft:basalt", "minecraft:magma_block", "minecraft:gilded_blackstone"]
  }[theme];
  const data = new Uint8Array(width * height * length);
  for (let y = 0; y < height; y++) {
    for (let z = 0; z < length; z++) {
      for (let x = 0; x < width; x++) {
        const i = y * width * length + z * width + x;
        const edge = x === 0 || z === 0 || x === width - 1 || z === length - 1;
        const centerLine = Math.abs(x - width / 2) < 1 || Math.abs(z - length / 2) < 1;
        if (y === 0) data[i] = edge ? 2 : 1;
        else if (y < 4 && edge) data[i] = 2;
        else if (y === 1 && centerLine) data[i] = theme === "start" ? 3 : theme === "mid" ? 2 : 3;
        else if (y === 2 && !edge && (x + z) % 19 === 0) data[i] = 4;
      }
    }
  }
  const paletteChildren = palette.map((id, index) => nbtInt(id, index));
  return gzipSync(nbtCompound("Schematic", [
    nbtInt("Version", 2),
    nbtInt("DataVersion", 3955),
    nbtShort("Width", width),
    nbtShort("Height", height),
    nbtShort("Length", length),
    nbtIntArray("Offset", [0, 0, 0]),
    nbtCompound("Palette", paletteChildren),
    nbtInt("PaletteMax", palette.length),
    nbtByteArray("BlockData", data),
    nbtCompound("Metadata", [
      nbtString("Name", "dungeon_ruins_001 generated source schematic"),
      nbtString("Author", "Codex generated source asset"),
      nbtString("Note", "Review and replace with builder-authored production schematic before live deployment.")
    ])
  ]));
}

function textureFor(kind, name) {
  const palette = name.includes("crystal")
    ? [[32, 62, 82, 255], [92, 220, 255, 255], [156, 92, 255, 255]]
    : name.includes("ashen") || name.includes("ember") || name.includes("king") || name.includes("throne")
      ? [[30, 24, 22, 255], [225, 92, 35, 255], [181, 139, 64, 255]]
      : [[39, 67, 40, 255], [102, 135, 70, 255], [80, 58, 43, 255]];
  return png(16, 16, (x, y) => {
    const idx = (x * 7 + y * 11 + name.length) % 3;
    if (kind === "item" && (x < 3 || y < 3 || x > 12 || y > 12)) return [0, 0, 0, 0];
    if (kind === "furniture" && (x + y) % 9 === 0) return [240, 220, 160, 255];
    return palette[idx];
  });
}

function minecraftModel(kind, name) {
  const texture = `nexo:item/dungeon_ruins_001/${kind}/${name}`;
  if (kind === "block") {
    return {
      parent: "minecraft:block/cube_all",
      textures: { all: texture }
    };
  }
  return {
    parent: "minecraft:item/generated",
    textures: { layer0: texture }
  };
}

function bbmodel(id, theme) {
  const isBoss = theme.endsWith("_boss");
  const accent = theme.startsWith("crystal") ? 1 : theme.startsWith("ash") ? 2 : 0;
  const animations = {};
  const names = ["idle", "walk", "attack", "cast", "death", ...(isBoss ? ["enrage"] : [])];
  for (const name of names) {
    animations[`${id}.${name}`] = {
      name,
      loop: name === "idle" || name === "walk" ? "loop" : "once",
      override: false,
      length: name === "death" ? 1.2 : 0.8,
      animators: {}
    };
  }
  return {
    meta: { format_version: "5.0", model_format: "free", box_uv: false },
    name: id,
    model_identifier: id,
    visible_box: [4, isBoss ? 5 : 4, 0],
    resolution: { width: 64, height: 64 },
    elements: [
      cube("body", [-3, 8, -2], [3, isBoss ? 24 : 20, 2], accent),
      cube("head", [-2.5, isBoss ? 24 : 20, -2.5], [2.5, isBoss ? 29 : 25, 2.5], accent + 1),
      cube("left_arm", [3, 10, -1.5], [5, isBoss ? 22 : 19, 1.5], accent + 2),
      cube("right_arm", [-5, 10, -1.5], [-3, isBoss ? 22 : 19, 1.5], accent + 2),
      cube("left_leg", [0.5, 0, -1.5], [2.5, 8, 1.5], accent),
      cube("right_leg", [-2.5, 0, -1.5], [-0.5, 8, 1.5], accent)
    ],
    outliner: ["body", "head", "left_arm", "right_arm", "left_leg", "right_leg"],
    textures: [],
    animations
  };
}

function cube(name, from, to, color) {
  return {
    name,
    box_uv: false,
    render_order: "default",
    locked: false,
    from,
    to,
    autouv: 0,
    color,
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

function generate() {
  for (const [name, w, h, l, theme] of schematics) {
    write(join(root, "iris", "schematics", `${name}.schem`), makeSchematic(w, h, l, theme));
  }

  write(join(root, "iris", "pack", "images", "dungeon_ruins_001_layout.png"), png(192, 192, (x, y) => {
    const d = Math.hypot(x - 96, y - 96);
    if (x < 68) return [49, 89, 58, 255];
    if (x < 130) return [58, 100, 130, 255];
    if (d < 54 || x >= 130) return [74, 31, 23, 255];
    return [0, 0, 0, 255];
  }));

  write(join(root, "resourcepack", "pack.mcmeta"), JSON.stringify({
    pack: {
      pack_format: 46,
      description: "dungeon_ruins_001 source resource pack placeholders"
    }
  }, null, 2));

  for (const [kind, names] of Object.entries(nexoModels)) {
    for (const name of names) {
      write(
        join(root, "resourcepack", "assets", "nexo", "models", "item", "dungeon_ruins_001", kind, `${name}.json`),
        JSON.stringify(minecraftModel(kind, name), null, 2)
      );
      write(
        join(root, "resourcepack", "assets", "nexo", "textures", "item", "dungeon_ruins_001", kind, `${name}.png`),
        textureFor(kind, name)
      );
    }
  }

  write(join(root, "resourcepack", "assets", "nexo", "sounds.json"), JSON.stringify({
    "dungeon_ruins_001.root_ambient": { replace: false, sounds: ["minecraft:ambient/cave/cave1"] },
    "dungeon_ruins_001.crystal_chime": { replace: false, sounds: ["minecraft:block/amethyst_block/chime"] },
    "dungeon_ruins_001.ember_trial": { replace: false, sounds: ["minecraft:block/fire/ambient"] }
  }, null, 2));

  write(join(root, "resourcepack", "PARTICLES_AND_SOUNDS.md"), [
    "# Particles And Sounds",
    "",
    "No custom `.ogg` files were generated because no local audio encoder is available.",
    "The source pack includes `assets/nexo/sounds.json` entries mapped to vanilla sounds.",
    "",
    "- START: `happyVillager`, `composter`, cave ambience.",
    "- MID: `endRod`, cyan `reddust`, amethyst chimes.",
    "- END: `flame`, `lava`, fire ambience.",
    ""
  ].join("\n"));

  for (const [id, theme] of modelEngineModels) {
    write(join(root, "modelengine", "blueprints", `${id}.bbmodel`), JSON.stringify(bbmodel(id, theme), null, 2));
  }
}

generate();
