const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { createCanvas } = require('canvas');

const MODEL_ID = 'level_1_grove_guardian';
const ROOT = __dirname;
const OUTPUT = path.resolve(
  ROOT,
  '..',
  'plugins',
  'ModelEngine',
  'blueprints',
  MODEL_ID,
  `${MODEL_ID}.bbmodel`,
);
const BACKUP_ROOT = path.resolve(ROOT, 'model_backups', MODEL_ID);

let sequence = 1;

function uuid() {
  return `b8200000-0000-4000-8000-${String(sequence++).padStart(12, '0')}`;
}

function sortedBackupDirs() {
  if (!fs.existsSync(BACKUP_ROOT)) return [];
  return fs
    .readdirSync(BACKUP_ROOT)
    .map((name) => path.join(BACKUP_ROOT, name))
    .filter((file) => fs.existsSync(path.join(file, `${MODEL_ID}.bbmodel`)))
    .sort()
    .reverse();
}

function loadTemplate() {
  const [latestBackup] = sortedBackupDirs();
  const backupFile = latestBackup ? path.join(latestBackup, `${MODEL_ID}.bbmodel`) : null;
  const source = backupFile && fs.existsSync(backupFile) ? backupFile : OUTPUT;
  return JSON.parse(fs.readFileSync(source, 'utf8'));
}

function hashFile(file) {
  return crypto.createHash('sha1').update(fs.readFileSync(file)).digest('hex').slice(0, 10);
}

function backupCurrentVersion() {
  if (!fs.existsSync(OUTPUT)) return;
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const hash = hashFile(OUTPUT);
  const dir = path.join(BACKUP_ROOT, `${stamp}-${hash}`);
  fs.mkdirSync(dir, { recursive: true });
  fs.copyFileSync(OUTPUT, path.join(dir, `${MODEL_ID}.bbmodel`));
  fs.copyFileSync(__filename, path.join(dir, 'build_grove_guardian_model.js'));
}

function collectBoneState(model) {
  const groupsByName = new Map((model.groups || []).map((group) => [group.name, group]));
  const outlinerByName = new Map();

  function cloneNode(node) {
    if (!node || typeof node !== 'object' || Array.isArray(node)) return null;
    const clone = {
      name: node.name,
      origin: node.origin,
      rotation: node.rotation,
      visibility: node.visibility,
      uuid: node.uuid,
      children: [],
    };
    outlinerByName.set(clone.name, clone);
    for (const child of node.children || []) {
      if (typeof child === 'object') {
        const nested = cloneNode(child);
        if (nested) clone.children.push(nested);
      }
    }
    return clone;
  }

  const outliner = (model.outliner || [])
    .map(cloneNode)
    .filter(Boolean);

  return { groupsByName, outlinerByName, outliner };
}

function createTexture() {
  const canvas = createCanvas(128, 128);
  const context = canvas.getContext('2d');
  const regions = [
    ['bark', '#4a2d1d', '#6f4629', '#25160f', 0, 0],
    ['darkBark', '#2a1a11', '#51311f', '#150d09', 32, 0],
    ['moss', '#425d31', '#678649', '#20301a', 64, 0],
    ['stone', '#666c68', '#8c948f', '#353a37', 96, 0],
    ['heart', '#97cf48', '#ccf170', '#416a27', 0, 32],
    ['root', '#3a2418', '#613c25', '#1c110b', 32, 32],
    ['heartwood', '#754728', '#a86735', '#3b2315', 64, 32],
    ['crackedStone', '#4f5551', '#727a74', '#252a27', 96, 32],
  ];

  for (const [name, base, light, dark, x, y] of regions) {
    context.fillStyle = base;
    context.fillRect(x, y, 32, 32);

    if (name === 'bark' || name === 'darkBark' || name === 'root' || name === 'heartwood') {
      context.fillStyle = light;
      for (let stripe = 6; stripe < 26; stripe += 10) {
        context.fillRect(x + stripe, y + 4 + ((stripe / 3) % 4), 3, 20);
      }
      context.fillStyle = dark;
      context.fillRect(x + 13, y + 7, 2, 13);
    }

    if (name === 'moss') {
      context.fillStyle = light;
      context.fillRect(x + 3, y + 4, 18, 9);
      context.fillRect(x + 12, y + 16, 14, 9);
      context.fillStyle = dark;
      context.fillRect(x + 7, y + 19, 7, 5);
      context.fillRect(x + 20, y + 7, 5, 5);
    }

    if (name === 'stone' || name === 'crackedStone') {
      context.fillStyle = light;
      context.fillRect(x + 4, y + 4, 23, 3);
      context.fillRect(x + 8, y + 17, 14, 2);
      context.fillStyle = dark;
      context.fillRect(x + 15, y + 7, 2, 10);
      context.fillRect(x + 21, y + 16, 2, 9);
    }

    if (name === 'heart') {
      context.fillStyle = '#ecffb3';
      context.fillRect(x + 11, y + 7, 10, 16);
      context.fillStyle = '#b9ea61';
      context.fillRect(x + 8, y + 10, 16, 10);
      context.fillStyle = '#416a27';
      context.fillRect(x + 14, y + 4, 4, 22);
    }
  }

  return canvas.toDataURL();
}

const UV = {
  bark: [0, 0, 32, 32],
  darkBark: [32, 0, 64, 32],
  moss: [64, 0, 96, 32],
  stone: [96, 0, 128, 32],
  heart: [0, 32, 32, 64],
  root: [32, 32, 64, 64],
  heartwood: [64, 32, 96, 64],
  crackedStone: [96, 32, 128, 64],
};

function faces(material) {
  return Object.fromEntries(
    ['north', 'east', 'south', 'west', 'up', 'down'].map((face) => [
      face,
      { uv: UV[material], texture: 0 },
    ]),
  );
}

function replaceGeometry(model) {
  const { groupsByName, outlinerByName, outliner } = collectBoneState(model);
  const elements = [];

  for (const node of outlinerByName.values()) {
    node.children = node.children.filter((child) => typeof child === 'object');
  }

  function attachCube(boneName, element) {
    elements.push(element);
    const node = outlinerByName.get(boneName);
    if (!node) throw new Error(`Bone not found in template outliner: ${boneName}`);
    node.children.unshift(element.uuid);
  }

  function cube(boneName, name, from, to, material, rotation = [0, 0, 0], origin = null) {
    attachCube(boneName, {
      name,
      box_uv: false,
      type: 'cube',
      from,
      to,
      origin: origin || from.map((value, index) => (value + to[index]) / 2),
      rotation,
      faces: faces(material),
      uuid: uuid(),
    });
  }

  model.groups = [...groupsByName.values()].map((group) => ({
    name: group.name,
    origin: group.origin,
    rotation: group.rotation,
    uuid: group.uuid,
  }));
  model.outliner = outliner;
  model.elements = elements;

  // Beast-first posture: forward-driven chest, buried hips, short planted legs.
  cube('body', 'pelvis_root_block', [-18, 35, -4], [16, 47, 16], 'darkBark', [0, 0, 2], [0, 41, 5]);
  cube('body', 'root_skirt', [-23, 36, 2], [21, 49, 22], 'root', [0, 0, -2], [0, 42, 12]);
  cube('body', 'lower_heartwood', [-9, 43, -6], [7, 50, 9], 'heartwood', [-5, 0, 0], [-1, 46, 1]);

  cube('chest', 'beast_chest', [-22, 49, -18], [18, 69, 13], 'bark', [-11, 0, 0], [-1, 59, -2]);
  cube('chest', 'shoulder_mantle', [-33, 58, -14], [20, 79, 15], 'darkBark', [-8, 0, 0], [-2, 70, 0]);
  cube('chest', 'left_front_flank', [8, 53, -16], [20, 74, 7], 'bark', [0, -16, -6], [13, 64, -3]);
  cube('chest', 'right_front_flank', [-24, 53, -16], [-10, 74, 8], 'bark', [0, 12, 6], [-15, 64, -3]);
  cube('chest', 'left_root_hump', [5, 54, -18], [24, 84, 11], 'root', [-4, -10, -10], [14, 67, -2]);
  cube('chest', 'back_spine_mass', [-15, 58, 8], [11, 84, 26], 'root', [-16, 0, 0], [0, 72, 14]);
  cube('chest', 'chest_jawline', [-13, 58, -17], [8, 66, -8], 'heartwood', [-5, 0, 0], [-2, 62, -12]);
  cube('chest', 'left_moss_patch', [15, 66, -5], [28, 76, 9], 'moss', [0, -8, -10], [20, 71, 2]);

  cube('h_head', 'snout_mask', [-10, 61, -28], [6, 73, -16], 'stone', [4, -3, 0], [-2, 67, -21]);
  cube('h_head', 'brow_plate', [-11, 65, -24], [6, 70, -17], 'crackedStone', [2, -2, 0], [-3, 67, -20]);
  cube('h_head', 'core_glow', [-4, 65, -29], [3, 72, -26], 'heart');
  cube('h_jaw', 'jaw_root', [-9, 58, -20], [8, 64, -11], 'darkBark', [10, 0, 0], [-1, 61, -14]);

  cube('crown', 'mane_root', [-10, 80, 1], [10, 88, 12], 'darkBark', [-12, 0, 0], [2, 84, 5]);
  cube('crown', 'mane_bridge', [-6, 76, 4], [6, 86, 15], 'root', [-20, 0, 0], [1, 81, 8]);
  cube('left_crown_1', 'left_mane_spire', [6, 80, 1], [15, 101, 9], 'root', [-14, 0, -22], [12, 88, 0]);
  cube('left_crown_2', 'left_mane_tip', [15, 93, 5], [23, 109, 11], 'root', [-18, 0, -10], [25, 99, 0]);
  cube('right_crown_1', 'right_mane_spire', [-15, 82, 2], [-5, 101, 10], 'root', [-10, 0, 18], [-7, 91, 2]);
  cube('back_crown', 'back_mane_spire', [0, 83, 8], [8, 108, 16], 'root', [-26, 0, 4], [7, 92, 8]);

  cube('left_arm', 'left_forelimb_upper', [11, 45, -8], [25, 67, 10], 'bark', [10, -4, -18], [24, 68, 2]);
  cube('left_forearm', 'left_forelimb_lower', [16, 18, -10], [30, 47, 8], 'root', [20, 0, -10], [38, 52, 0]);
  cube('left_hand', 'left_paw_mass', [15, 2, -13], [31, 21, 8], 'root', [8, 0, -2], [43, 31, -3]);

  cube('right_arm', 'right_strike_upper', [-48, 45, -9], [-19, 71, 15], 'stone', [6, 0, 20], [-20, 65, 3]);
  cube('right_forearm', 'right_strike_lower', [-60, 16, -11], [-36, 49, 15], 'stone', [10, 0, 12], [-32, 49, 0]);
  cube('right_hand', 'right_fist_mass', [-62, 6, -13], [-38, 22, 14], 'root', [6, 0, 4], [-38, 31, -2]);

  cube('left_leg', 'left_hind_upper', [4, 22, 2], [15, 40, 14], 'root', [-8, 0, -2], [11, 41, 4]);
  cube('left_shin', 'left_hind_lower', [5, 8, -1], [17, 24, 11], 'root', [6, 0, 4], [13, 25, 1]);
  cube('left_foot', 'left_hoof_mass', [1, 1, -13], [21, 8, 12], 'root', [-3, -2, 0], [14, 8, -1]);
  cube('left_root_toes', 'left_toe_root', [4, 0, -18], [19, 4, -7], 'root', [-3, -4, 0], [14, 4, -8]);

  cube('right_leg', 'right_hind_upper', [-15, 22, 2], [-4, 40, 14], 'root', [-8, 0, 4], [-10, 41, 4]);
  cube('right_shin', 'right_hind_lower', [-17, 8, -1], [-5, 24, 11], 'root', [6, 0, -4], [-13, 25, 1]);
  cube('right_foot', 'right_hoof_mass', [-21, 1, -13], [-1, 8, 12], 'root', [-3, -2, 0], [-14, 8, -1]);
  cube('right_root_toes', 'right_toe_root', [-19, 0, -18], [-4, 4, -7], 'root', [-3, -4, 0], [-14, 4, -8]);

  cube('left_vine_1', 'left_root_strap', [14, 38, 10], [19, 58, 17], 'moss', [10, 0, 4], [17, 52, 8]);
  cube('right_vine_1', 'right_root_strap', [-24, 38, 11], [-18, 56, 18], 'moss', [12, 0, -3], [-17, 57, 10]);

  // Technical volumes stay explicit and hidden in beauty renders.
  cube('ob_head', 'damage_zone_head', [-12, 61, -29], [9, 75, -15], 'heart');
  cube('ob_torso', 'damage_zone_torso', [-21, 46, -18], [18, 80, 15], 'heart');
  cube('ob_core_weakpoint', 'damage_zone_core_weakpoint', [-4, 65, -30], [4, 73, -24], 'heart');
  cube('ob_left_arm', 'damage_zone_left_arm', [10, 44, -9], [27, 68, 12], 'heart');
  cube('ob_left_forearm', 'damage_zone_left_forearm', [15, 17, -11], [31, 49, 9], 'heart');
  cube('ob_right_arm', 'damage_zone_right_arm', [-48, 44, -10], [-18, 72, 16], 'heart');
  cube('ob_right_forearm', 'damage_zone_right_forearm', [-61, 15, -12], [-35, 50, 16], 'heart');
  cube('ob_left_leg', 'damage_zone_left_leg', [3, 21, 1], [16, 41, 15], 'heart');
  cube('ob_left_shin', 'damage_zone_left_shin', [4, 7, -2], [18, 25, 12], 'heart');
  cube('ob_left_foot', 'damage_zone_left_foot', [1, 0, -14], [22, 9, 13], 'heart');
  cube('ob_right_leg', 'damage_zone_right_leg', [-16, 21, 1], [-3, 41, 15], 'heart');
  cube('ob_right_shin', 'damage_zone_right_shin', [-18, 7, -2], [-4, 25, 12], 'heart');
  cube('ob_right_foot', 'damage_zone_right_foot', [-22, 0, -14], [0, 9, 13], 'heart');
  cube('hitbox', 'hitbox', [-18, 0, -18], [18, 92, 19], 'stone');

  return model;
}

function main() {
  const model = loadTemplate();
  backupCurrentVersion();
  replaceGeometry(model);
  model.name = MODEL_ID;
  model.model_identifier = MODEL_ID;
  model.resolution = { width: 128, height: 128 };
  model.textures = [
    {
      path: '',
      name: `${MODEL_ID}.png`,
      folder: '',
      namespace: '',
      id: '0',
      particle: false,
      render_mode: 'default',
      render_sides: 'auto',
      frame_time: 1,
      frame_order_type: 'loop',
      frame_order: '',
      frame_interpolate: false,
      visible: true,
      internal: true,
      saved: false,
      uuid: uuid(),
      relative_path: `${MODEL_ID}.png`,
      source: createTexture(),
    },
  ];
  fs.mkdirSync(path.dirname(OUTPUT), { recursive: true });
  fs.writeFileSync(OUTPUT, `${JSON.stringify(model, null, 2)}\n`);
  console.log(`WROTE: ${OUTPUT}`);
}

if (require.main === module) {
  main();
}
