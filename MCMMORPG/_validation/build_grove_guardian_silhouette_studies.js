const fs = require('fs');
const path = require('path');
const { createCanvas } = require('canvas');

const ROOT = __dirname;
const CANDIDATE_ROOT = path.join(ROOT, 'candidates');
const BACKUP_ROOT = path.join(ROOT, 'model_backups');

const CANDIDATES = [
  {
    id: 'grove_guardian_silhouette_a_low_mask',
    label: 'A low-mask triangular colossus',
    build(make) {
      const { bone, cube } = make;
      bone('body', [0, 40, 2]);
      bone('chest', [0, 62, 0], 'body', [-4, 0, 0]);
      bone('head', [0, 76, -7], 'chest');
      bone('crown', [0, 88, 2], 'head');
      bone('crown_left', [10, 91, 4], 'crown', [0, 0, -24]);
      bone('crown_mid', [0, 95, 7], 'crown', [-14, 0, 0]);
      bone('crown_right', [-10, 92, 5], 'crown', [0, 0, 28]);
      bone('left_arm', [23, 65, 1], 'chest', [0, 0, -10]);
      bone('left_forearm', [35, 46, 0], 'left_arm', [-4, 0, -8]);
      bone('right_arm', [-27, 67, 2], 'chest', [0, 0, 18]);
      bone('right_forearm', [-42, 45, 1], 'right_arm', [6, 0, 14]);
      bone('left_leg', [12, 39, 3], 'body', [0, 0, -4]);
      bone('left_shin', [13, 22, 0], 'left_leg', [4, 0, 2]);
      bone('right_leg', [-10, 39, 4], 'body', [0, 0, 6]);
      bone('right_shin', [-12, 21, 1], 'right_leg', [3, 0, -2]);
      bone('hitbox', [0, 46, 2]);

      cube('body', 'pelvis', [-16, 34, -7], [16, 48, 12], 'dark');
      cube('body', 'root_skirt', [-19, 39, -3], [17, 49, 14], 'wood', [0, 0, -3], [0, 44, 4]);
      cube('chest', 'trunk_wedge', [-25, 46, -10], [24, 80, 14], 'wood', [-4, 0, 1], [0, 63, 1]);
      cube('chest', 'shoulder_cap_left', [16, 62, -6], [35, 80, 12], 'moss', [0, -12, -18], [23, 69, 1]);
      cube('chest', 'shoulder_cap_right', [-36, 60, -6], [-15, 78, 11], 'stone', [0, 8, 22], [-24, 68, 2]);
      cube('head', 'mask', [-8, 66, -21], [9, 78, -12], 'stone', [0, -3, 0], [0, 72, -14]);
      cube('head', 'core', [-4, 69, -24], [4, 76, -21], 'glow');
      cube('crown_left', 'crown_left', [8, 85, 0], [18, 111, 9], 'wood', [0, 0, -24], [10, 91, 4]);
      cube('crown_mid', 'crown_mid', [-4, 89, 2], [5, 115, 14], 'wood', [-14, 0, 4], [0, 95, 7]);
      cube('crown_right', 'crown_right', [-18, 86, 1], [-8, 109, 10], 'wood', [0, 0, 28], [-10, 92, 5]);
      cube('left_arm', 'left_arm', [16, 43, -5], [35, 67, 11], 'wood');
      cube('left_forearm', 'left_forearm', [27, 18, -5], [42, 49, 10], 'wood', [-6, 0, -10], [35, 46, 0]);
      cube('right_arm', 'right_arm', [-42, 44, -5], [-20, 68, 11], 'stone');
      cube('right_forearm', 'right_forearm', [-53, 16, -4], [-35, 49, 11], 'root', [7, 0, 12], [-42, 45, 1]);
      cube('left_leg', 'left_leg', [5, 20, -3], [17, 42, 9], 'root');
      cube('left_shin', 'left_shin', [6, 2, -5], [19, 24, 8], 'root');
      cube('right_leg', 'right_leg', [-17, 19, -3], [-6, 42, 9], 'root');
      cube('right_shin', 'right_shin', [-20, 1, -5], [-7, 23, 8], 'root');
      cube('hitbox', 'hitbox', [-18, 0, -17], [18, 92, 17], 'stone');
    },
  },
  {
    id: 'grove_guardian_silhouette_b_rooted_lean',
    label: 'B forward-leaning rooted colossus',
    build(make) {
      const { bone, cube } = make;
      bone('body', [0, 38, 8], null, [-6, 0, 0]);
      bone('chest', [0, 60, 2], 'body', [-9, 0, 0]);
      bone('head', [0, 73, -10], 'chest', [8, 0, 0]);
      bone('crown', [0, 84, -1], 'head');
      bone('crown_left', [12, 86, 2], 'crown', [4, 0, -34]);
      bone('crown_back', [0, 89, 9], 'crown', [-20, 0, 0]);
      bone('crown_right', [-10, 86, 4], 'crown', [8, 0, 28]);
      bone('left_arm', [22, 59, 0], 'chest', [10, 0, -18]);
      bone('left_forearm', [31, 35, -4], 'left_arm', [18, 0, -12]);
      bone('right_arm', [-22, 62, 2], 'chest', [22, 0, 18]);
      bone('right_forearm', [-28, 33, -3], 'right_arm', [26, 0, 8]);
      bone('left_leg', [13, 35, 10], 'body', [-10, 0, 8]);
      bone('left_shin', [16, 17, 4], 'left_leg', [16, 0, -4]);
      bone('right_leg', [-11, 35, 10], 'body', [-8, 0, -8]);
      bone('right_shin', [-14, 17, 4], 'right_leg', [18, 0, 5]);
      bone('hitbox', [0, 44, 2]);

      cube('body', 'pelvis', [-18, 31, -2], [17, 47, 16], 'dark');
      cube('body', 'root_base', [-24, 28, 0], [22, 43, 22], 'root', [0, 0, -2], [0, 35, 10]);
      cube('chest', 'leaning_trunk', [-22, 42, -14], [20, 76, 12], 'wood', [-5, 0, 0], [0, 58, -1]);
      cube('chest', 'upper_back', [-15, 58, 8], [12, 82, 22], 'dark', [-12, 0, 0], [0, 70, 12]);
      cube('head', 'mask', [-7, 66, -24], [8, 79, -15], 'stone');
      cube('head', 'core', [-3, 69, -27], [4, 76, -24], 'glow');
      cube('crown_left', 'crown_left', [10, 81, -3], [19, 104, 6], 'wood', [6, 0, -34], [12, 86, 2]);
      cube('crown_back', 'crown_back', [-4, 84, 8], [5, 110, 18], 'wood', [-22, 0, 0], [0, 89, 9]);
      cube('crown_right', 'crown_right', [-17, 82, -1], [-8, 105, 8], 'wood', [8, 0, 26], [-10, 86, 4]);
      cube('left_arm', 'left_arm', [14, 39, -9], [32, 62, 10], 'wood');
      cube('left_forearm', 'left_forearm', [20, 6, -11], [35, 38, 6], 'root', [18, 0, -10], [31, 35, -4]);
      cube('right_arm', 'right_arm', [-31, 42, -8], [-14, 64, 10], 'wood');
      cube('right_forearm', 'right_forearm', [-36, 5, -10], [-21, 35, 5], 'root', [26, 0, 9], [-28, 33, -3]);
      cube('left_leg', 'left_leg', [6, 16, -3], [18, 38, 9], 'root');
      cube('left_shin', 'left_shin', [7, -1, -10], [21, 18, 5], 'root');
      cube('right_leg', 'right_leg', [-18, 16, -3], [-7, 38, 9], 'root');
      cube('right_shin', 'right_shin', [-21, -1, -9], [-8, 18, 6], 'root');
      cube('hitbox', 'hitbox', [-18, 0, -18], [18, 92, 20], 'stone');
    },
  },
  {
    id: 'grove_guardian_silhouette_c_stone_arm',
    label: 'C asymmetric stone-arm sentinel',
    build(make) {
      const { bone, cube } = make;
      bone('body', [0, 39, 2]);
      bone('chest', [0, 61, 1], 'body', [-2, 0, 0]);
      bone('head', [0, 75, -8], 'chest');
      bone('crown', [0, 88, 0], 'head');
      bone('crown_left', [7, 90, 3], 'crown', [0, 0, -16]);
      bone('crown_mid', [0, 94, 8], 'crown', [-12, 0, 0]);
      bone('crown_right', [-14, 91, 4], 'crown', [0, 0, 38]);
      bone('left_arm', [19, 65, 1], 'chest', [0, 0, -8]);
      bone('left_forearm', [29, 44, 0], 'left_arm', [0, 0, -5]);
      bone('right_arm', [-29, 67, 2], 'chest', [0, 0, 24]);
      bone('right_forearm', [-47, 43, 2], 'right_arm', [4, 0, 12]);
      bone('left_leg', [10, 39, 3], 'body', [0, 0, -2]);
      bone('left_shin', [11, 20, 0], 'left_leg', [4, 0, 1]);
      bone('right_leg', [-10, 39, 4], 'body', [0, 0, 4]);
      bone('right_shin', [-11, 20, 1], 'right_leg', [3, 0, -1]);
      bone('hitbox', [0, 46, 2]);

      cube('body', 'pelvis', [-15, 34, -7], [15, 47, 12], 'dark');
      cube('body', 'root_wrap', [-18, 38, -3], [16, 49, 14], 'root', [0, 0, -2], [0, 43, 4]);
      cube('chest', 'torso', [-21, 46, -11], [22, 79, 13], 'wood', [-3, 0, 1], [0, 61, 1]);
      cube('chest', 'left_trunk_rise', [8, 50, -13], [23, 82, 10], 'wood', [0, -6, -8], [15, 65, 0]);
      cube('head', 'mask', [-7, 67, -22], [8, 79, -13], 'stone');
      cube('head', 'core', [-3, 70, -25], [4, 76, -22], 'glow');
      cube('crown_left', 'crown_left', [5, 84, 0], [14, 106, 8], 'wood', [0, 0, -16], [7, 90, 3]);
      cube('crown_mid', 'crown_mid', [-4, 88, 4], [4, 114, 14], 'wood', [-12, 0, 0], [0, 94, 8]);
      cube('crown_right', 'crown_right', [-25, 86, 1], [-12, 112, 11], 'wood', [0, 0, 38], [-14, 91, 4]);
      cube('left_arm', 'left_arm', [13, 42, -5], [28, 66, 10], 'wood');
      cube('left_forearm', 'left_forearm', [19, 16, -5], [32, 45, 9], 'root');
      cube('right_arm', 'right_arm', [-48, 45, -7], [-22, 69, 12], 'stone');
      cube('right_forearm', 'right_forearm', [-62, 13, -7], [-40, 46, 13], 'stone', [6, 0, 10], [-47, 43, 2]);
      cube('left_leg', 'left_leg', [4, 19, -3], [15, 42, 9], 'root');
      cube('left_shin', 'left_shin', [4, 0, -5], [17, 22, 8], 'root');
      cube('right_leg', 'right_leg', [-16, 19, -3], [-5, 42, 9], 'root');
      cube('right_shin', 'right_shin', [-17, 0, -5], [-4, 22, 8], 'root');
      cube('hitbox', 'hitbox', [-18, 0, -17], [18, 92, 17], 'stone');
    },
  },
];

function texture() {
  const canvas = createCanvas(64, 64);
  const context = canvas.getContext('2d');
  const palette = {
    wood: '#4a2d1d',
    dark: '#26170f',
    root: '#392417',
    stone: '#636964',
    moss: '#4d6836',
    glow: '#9cd74a',
  };
  const layout = [
    ['wood', 0, 0],
    ['dark', 32, 0],
    ['stone', 0, 32],
    ['moss', 32, 32],
  ];
  for (const [name, x, y] of layout) {
    context.fillStyle = palette[name];
    context.fillRect(x, y, 32, 32);
  }
  context.fillStyle = palette.root;
  context.fillRect(16, 32, 16, 32);
  context.fillStyle = palette.glow;
  context.fillRect(48, 32, 16, 32);
  return canvas.toDataURL();
}

function buildModel(candidate) {
  let sequence = 1;
  const bones = new Map();
  const elements = [];
  const uuid = () => `d1500000-0000-4000-8000-${String(sequence++).padStart(12, '0')}`;
  const UV = {
    wood: [0, 0, 32, 32],
    dark: [32, 0, 64, 32],
    stone: [0, 32, 32, 64],
    moss: [32, 32, 48, 64],
    root: [16, 32, 32, 64],
    glow: [48, 32, 64, 64],
  };

  function faces(material) {
    return Object.fromEntries(
      ['north', 'east', 'south', 'west', 'up', 'down'].map((face) => [
        face,
        { uv: UV[material], texture: 0 },
      ]),
    );
  }

  function bone(name, origin, parent = null, rotation = [0, 0, 0]) {
    const entry = { name, origin, parent, rotation, uuid: uuid(), cubes: [], children: [], visibility: true };
    bones.set(name, entry);
    if (parent) bones.get(parent).children.push(name);
    return entry;
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
    return element;
  }

  candidate.build({ bone, cube });
  if (bones.has('hitbox')) bones.get('hitbox').visibility = false;

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

  return {
    meta: { format_version: '5.0', model_format: 'free', box_uv: false },
    name: candidate.id,
    resolution: { width: 64, height: 64 },
    elements,
    groups: [...bones.values()].map(({ name, origin, rotation, uuid: id }) => ({
      name,
      origin,
      rotation,
      uuid: id,
    })),
    outliner: [...bones.values()].filter((entry) => !entry.parent).map((entry) => outliner(entry.name)),
    animations: [],
    textures: [
      {
        path: `${candidate.id}.png`,
        name: `${candidate.id}.png`,
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
        relative_path: `${candidate.id}.png`,
        source: texture(),
      },
    ],
  };
}

function backupIfExists(targetFile, backupDir) {
  if (!fs.existsSync(targetFile)) return;
  fs.mkdirSync(backupDir, { recursive: true });
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  fs.copyFileSync(targetFile, path.join(backupDir, `${stamp}-${path.basename(targetFile)}`));
}

function writeCandidate(candidate) {
  const model = buildModel(candidate);
  const dir = path.join(CANDIDATE_ROOT, candidate.id);
  const output = path.join(dir, `${candidate.id}.bbmodel`);
  backupIfExists(output, path.join(BACKUP_ROOT, candidate.id));
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(output, `${JSON.stringify(model, null, 2)}\n`);
  return output;
}

function main() {
  const written = CANDIDATES.map(writeCandidate);
  for (const file of written) console.log(`WROTE: ${file}`);
}

if (require.main === module) main();
