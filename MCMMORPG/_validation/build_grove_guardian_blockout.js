const fs = require('fs');
const path = require('path');
const { createCanvas } = require('canvas');

const ID = 'level_1_grove_guardian_blockout';
const OUTPUT = path.join(__dirname, 'candidates', ID, `${ID}.bbmodel`);
const BACKUPS = path.join(__dirname, 'model_backups', ID);
let sequence = 1;
const uuid = () => `c9300000-0000-4000-8000-${String(sequence++).padStart(12, '0')}`;
const bones = new Map();
const elements = [];

function bone(name, origin, parent = null, rotation = [0, 0, 0]) {
  const entry = { name, origin, parent, rotation, uuid: uuid(), cubes: [], children: [] };
  bones.set(name, entry);
  if (parent) bones.get(parent).children.push(name);
  return entry;
}

const UV = {
  wood: [0, 0, 32, 32],
  dark: [32, 0, 64, 32],
  stone: [0, 32, 32, 64],
  moss: [32, 32, 64, 64],
};

function faces(material) {
  return Object.fromEntries(
    ['north', 'east', 'south', 'west', 'up', 'down']
      .map((face) => [face, { uv: UV[material], texture: 0 }]),
  );
}

function cube(boneName, name, from, to, material, rotation = [0, 0, 0], origin = null) {
  const element = {
    name,
    type: 'cube',
    box_uv: false,
    from,
    to,
    origin: origin || from.map((value, index) => (value + to[index]) / 2),
    rotation,
    faces: faces(material),
    uuid: uuid(),
  };
  elements.push(element);
  bones.get(boneName).cubes.push(element.uuid);
}

bone('body', [0, 40, 3]);
bone('chest', [0, 60, 1], 'body');
bone('h_head', [2, 77, -8], 'chest');
bone('crown', [3, 87, 0], 'h_head');
bone('crown_left', [12, 89, 1], 'crown', [0, -12, -18]);
bone('crown_center', [3, 92, 4], 'crown', [-8, 0, 3]);
bone('crown_right', [-8, 91, 3], 'crown', [0, 16, 24]);
bone('left_arm', [25, 66, 2], 'chest', [0, -5, -15]);
bone('left_forearm', [39, 48, 0], 'left_arm', [0, 0, 8]);
bone('left_hand', [44, 25, -4], 'left_forearm');
bone('right_arm', [-23, 64, 2], 'chest', [0, 7, 18]);
bone('right_forearm', [-35, 47, 0], 'right_arm', [0, 0, -14]);
bone('right_hand', [-40, 25, -5], 'right_forearm');
bone('left_leg', [11, 39, 3], 'body', [0, 0, -5]);
bone('left_shin', [13, 22, 0], 'left_leg');
bone('left_foot', [14, 7, -5], 'left_shin');
bone('right_leg', [-11, 39, 3], 'body', [0, 0, 6]);
bone('right_shin', [-13, 22, 0], 'right_leg');
bone('right_foot', [-14, 7, -5], 'right_shin');
bone('hitbox', [0, 44, 2]);

// Dominant trunk: broad upper mass, narrow waist, short rooted legs.
cube('body', 'pelvis', [-16, 35, -7], [16, 48, 11], 'dark', [0, 0, 2]);
cube('body', 'lower_trunk', [-13, 44, -8], [14, 62, 11], 'wood', [-3, 0, 1]);
cube('chest', 'main_chest', [-22, 56, -10], [22, 75, 12], 'wood', [-3, 0, 1]);
cube('chest', 'left_shoulder', [15, 61, -7], [35, 78, 12], 'moss', [0, -10, -15], [24, 68, 2]);
cube('chest', 'right_shoulder', [-34, 60, -7], [-15, 75, 10], 'stone', [0, 12, 18], [-22, 67, 2]);

// Clearly separated mask and jaw.
cube('h_head', 'mask', [-9, 72, -22], [12, 87, -12], 'stone', [0, -4, 1], [2, 79, -15]);
cube('h_head', 'eye_socket', [-3, 76, -24], [6, 83, -21], 'dark');
cube('h_head', 'jaw', [-7, 67, -18], [9, 73, -10], 'dark', [7, 0, 0], [2, 71, -12]);

// Three connected crown limbs, not a spray of independent sticks.
cube('crown', 'crown_root', [-12, 82, -2], [16, 92, 10], 'dark', [-5, 0, 2], [3, 87, 1]);
cube('crown_left', 'left_branch', [7, 84, -3], [19, 108, 8], 'wood', [0, 0, -25], [12, 89, 1]);
cube('crown_center', 'center_branch', [-2, 88, 2], [9, 116, 13], 'dark', [-10, 0, 3], [3, 92, 4]);
cube('crown_right', 'right_branch', [-16, 87, -1], [-5, 111, 9], 'wood', [0, 0, 30], [-8, 91, 3]);

// Unequal arms: stone maul on one side, root fist on the other.
cube('left_arm', 'left_upper_arm', [17, 44, -5], [37, 70, 12], 'dark', [0, -3, -17], [25, 66, 2]);
cube('left_forearm', 'left_forearm', [30, 23, -6], [49, 53, 12], 'wood', [0, 0, 9], [39, 48, 0]);
cube('left_hand', 'stone_maul', [33, 10, -12], [55, 33, 11], 'stone', [0, 0, 4], [44, 25, -4]);
cube('right_arm', 'right_upper_arm', [-34, 43, -5], [-15, 68, 12], 'wood', [0, 4, 20], [-23, 64, 2]);
cube('right_forearm', 'right_forearm', [-47, 21, -6], [-27, 51, 11], 'dark', [0, 0, -14], [-35, 47, 0]);
cube('right_hand', 'root_fist', [-51, 9, -13], [-31, 32, 10], 'wood', [0, 0, -7], [-40, 25, -5]);

cube('left_leg', 'left_thigh', [4, 22, -4], [19, 46, 10], 'wood', [0, 0, -4], [11, 39, 3]);
cube('left_shin', 'left_shin', [7, 7, -5], [20, 27, 9], 'dark', [0, 0, 4], [13, 22, 0]);
cube('left_foot', 'left_root_foot', [3, 0, -15], [25, 10, 9], 'wood', [-2, -4, 0], [14, 7, -5]);
cube('right_leg', 'right_thigh', [-19, 22, -4], [-4, 46, 10], 'wood', [0, 0, 5], [-11, 39, 3]);
cube('right_shin', 'right_shin', [-20, 7, -5], [-7, 27, 9], 'dark', [0, 0, -5], [-13, 22, 0]);
cube('right_foot', 'right_root_foot', [-25, 0, -15], [-3, 10, 9], 'wood', [-2, 4, 0], [-14, 7, -5]);
cube('hitbox', 'hitbox', [-17, 0, -17], [17, 92, 17], 'stone');
bones.get('hitbox').visibility = false;

function outliner(name) {
  const entry = bones.get(name);
  return {
    name,
    origin: entry.origin,
    rotation: entry.rotation,
    visibility: entry.visibility,
    uuid: entry.uuid,
    children: [...entry.cubes, ...entry.children.map(outliner)],
  };
}

function texture() {
  const canvas = createCanvas(64, 64);
  const context = canvas.getContext('2d');
  context.fillStyle = '#724526';
  context.fillRect(0, 0, 32, 32);
  context.fillStyle = '#2d1e16';
  context.fillRect(32, 0, 32, 32);
  context.fillStyle = '#68716d';
  context.fillRect(0, 32, 32, 32);
  context.fillStyle = '#486534';
  context.fillRect(32, 32, 32, 32);
  return canvas.toDataURL();
}

const model = {
  meta: { format_version: '5.0', model_format: 'free', box_uv: false },
  name: ID,
  resolution: { width: 64, height: 64 },
  elements,
  groups: [...bones.values()].map(({ name, origin, rotation, uuid: id }) => ({
    name, origin, rotation, uuid: id,
  })),
  outliner: [...bones.values()].filter((entry) => !entry.parent).map((entry) => outliner(entry.name)),
  animations: [],
  textures: [{
    path: '',
    name: `${ID}.png`,
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
    relative_path: `${ID}.png`,
    source: texture(),
  }],
};

if (fs.existsSync(OUTPUT)) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const directory = path.join(BACKUPS, timestamp);
  fs.mkdirSync(directory, { recursive: true });
  fs.copyFileSync(OUTPUT, path.join(directory, `${ID}.bbmodel`));
  fs.copyFileSync(__filename, path.join(directory, path.basename(__filename)));
}
fs.mkdirSync(path.dirname(OUTPUT), { recursive: true });
fs.writeFileSync(OUTPUT, `${JSON.stringify(model, null, 2)}\n`);
console.log(`BLOCKOUT_BUILT ${OUTPUT}: ${elements.length} elements, ${bones.size} bones`);
