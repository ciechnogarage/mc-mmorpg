const fs = require('fs');
const path = require('path');
const { createCanvas } = require('canvas');

const ROOT = path.resolve(__dirname, '..');
const ID = 'level_1_moss_stalker';
const OUTPUT_DIR = path.join(ROOT, 'plugins', 'ModelEngine', 'blueprints', ID);
const OUTPUT = path.join(OUTPUT_DIR, `${ID}.bbmodel`);
const BACKUPS = path.join(__dirname, 'model_backups', ID);

let sequence = 1;
const nextUuid = () => `d1400000-0000-4000-8000-${String(sequence++).padStart(12, '0')}`;

const UV = {
  bark: [0, 0, 16, 16],
  dark: [16, 0, 32, 16],
  moss: [32, 0, 48, 16],
  glow: [48, 0, 64, 16],
};

const faces = (material) =>
  Object.fromEntries(
    ['north', 'east', 'south', 'west', 'up', 'down'].map((face) => [
      face,
      { uv: UV[material], texture: 0 },
    ]),
  );

const bones = new Map();
const elements = [];

function bone(name, origin, parent = null, rotation = [0, 0, 0], visibility = true) {
  const entry = {
    name,
    uuid: nextUuid(),
    origin,
    rotation,
    visibility,
    parent,
    cubes: [],
    children: [],
  };
  bones.set(name, entry);
  if (parent) bones.get(parent).children.push(name);
  return entry;
}

function cube(boneName, name, from, to, material, rotation = [0, 0, 0], origin = null, color = 0) {
  const element = {
    name,
    box_uv: false,
    render_order: 'default',
    locked: false,
    export: true,
    scope: 0,
    allow_mirror_modeling: true,
    from,
    to,
    autouv: 0,
    color,
    rotation,
    origin: origin || from.map((value, index) => (value + to[index]) / 2),
    faces: faces(material),
    type: 'cube',
    uuid: nextUuid(),
  };
  elements.push(element);
  bones.get(boneName).cubes.push(element.uuid);
  return element;
}

function outliner(name) {
  const entry = bones.get(name);
  return {
    name: entry.name,
    uuid: entry.uuid,
    export: true,
    locked: false,
    scope: 0,
    selected: false,
    _static: { properties: {}, temp_data: {} },
    origin: entry.origin,
    rotation: entry.rotation,
    color: 0,
    children: [
      ...entry.cubes,
      ...entry.children.map((child) => outliner(child)),
    ],
    reset: false,
    shade: true,
    mirror_uv: false,
    visibility: entry.visibility,
    autouv: 0,
    isOpen: false,
    primary_selected: false,
  };
}

function texture() {
  const canvas = createCanvas(64, 16);
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#6f4d2f';
  ctx.fillRect(0, 0, 16, 16);
  ctx.fillStyle = '#251812';
  ctx.fillRect(16, 0, 16, 16);
  ctx.fillStyle = '#58703d';
  ctx.fillRect(32, 0, 16, 16);
  ctx.fillStyle = '#6bbf73';
  ctx.fillRect(48, 0, 16, 16);
  return canvas.toDataURL();
}

bone('hitbox', [0, 13, 0], null, [0, 0, 0], false);
bone('root', [0, 7.5, 0]);
bone('chest', [0, 8.5, -6], 'root');
bone('head', [0, 8.1, -17], 'chest');
bone('attack_side', [3.1, 8.6, -16.5], 'head', [0, 0, 18]);
bone('lower_back', [0, 8.3, 6.9], 'root');
bone('tail_base', [0, 7.8, 13.7], 'lower_back');
bone('tail_mid', [0, 7.1, 20.9], 'tail_base');
bone('tail_tip', [0, 6.4, 27.8], 'tail_mid');

bone('lf_upper', [4.6, 8, -8.8], 'chest', [0, 0, -18]);
bone('lf_lower', [5.1, 3.1, -8.2], 'lf_upper', [0, 0, 16]);
bone('lf_foot', [5.4, 0.8, -8.6], 'lf_lower');
bone('rf_upper', [-4.6, 8, -8.8], 'chest', [0, 0, 18]);
bone('rf_lower', [-5.1, 3.1, -8.2], 'rf_upper', [0, 0, -16]);
bone('rf_foot', [-5.4, 0.8, -8.6], 'rf_lower');

bone('lb_upper', [4.9, 7.6, 8.4], 'lower_back', [0, 0, -8]);
bone('lb_lower', [5.4, 3.7, 10.5], 'lb_upper', [0, 0, 10]);
bone('lb_foot', [5.8, 1.1, 11.1], 'lb_lower');
bone('rb_upper', [-4.9, 7.6, 8.4], 'lower_back', [0, 0, 8]);
bone('rb_lower', [-5.4, 3.7, 10.5], 'rb_upper', [0, 0, -10]);
bone('rb_foot', [-5.8, 1.1, 11.1], 'rb_lower');

cube('root', 'body_core', [-3.9, 6.9, -0.8], [3.9, 9.5, 8.8], 'dark', [-5, 0, 0], [0, 8, 2.8], 4);
cube('root', 'rib_taper', [-3, 6.8, 1.4], [3, 8.8, 11.8], 'bark', [-1, 0, 0], [0, 7.8, 6.6], 8);
cube('chest', 'shoulder_mass', [-6.3, 6.9, -15.4], [6.3, 10, -4.3], 'dark', [7, 0, 0], [0, 8, -6.8], 4);
cube('chest', 'forequarter_plate', [-4.1, 7.1, -20.1], [4.1, 9.8, -12.9], 'moss', [12, 0, 0], [0, 8, -15.3], 9);
cube('chest', 'spine_bridge', [-2.3, 7.6, -1.9], [2.3, 8.9, 6.4], 'dark', [-4, 0, 0], [0, 8, 2.6], 4);
cube('head', 'root_mask', [-2.2, 7.2, -25.6], [2.2, 9.5, -14.6], 'dark', [9, 0, 0], [0, 8.1, -18.9], 7);
cube('head', 'jaw_wedge', [-1.8, 6.3, -25.1], [1.8, 7.6, -13.3], 'bark', [15, 0, 0], [0, 6.8, -18.8], 8);
cube('head', 'brow_plate', [-1.3, 8.7, -22.8], [1.3, 9.8, -16.8], 'moss', [7, 0, 0], [0, 9.1, -19], 5);
cube('attack_side', 'thorn_cheek', [0.8, 7.1, -23.2], [5.6, 9.1, -16], 'glow', [0, 0, 33], [2, 8, -18.9], 3);
cube('attack_side', 'lash_stub', [4.8, 7.5, -28.3], [11.8, 8.7, -22.1], 'bark', [0, 18, 34], [5.8, 8.1, -24.4], 0);
cube('lower_back', 'hip_mass', [-3.6, 7, 6.4], [3.6, 10.2, 13.6], 'glow', [-10, 0, 0], [0, 8.6, 10.1], 3);
cube('lower_back', 'rear_counterweight', [-2.4, 6.8, 12.9], [2.4, 8.5, 18.2], 'bark', [10, 0, 0], [0, 7.8, 15.7], 8);
cube('tail_base', 'root_tail_base', [-1.6, 6.8, 14.5], [1.6, 8, 21.5], 'bark', [12, 0, 0], [0, 7.5, 17.2], 0);
cube('tail_mid', 'root_tail_mid', [-1.3, 6.3, 20.9], [1.3, 7.4, 27.1], 'moss', [9, 0, 0], [0, 6.9, 23.7], 0);
cube('tail_tip', 'drag_tail_tip', [-1.5, 5.8, 26.4], [1.5, 6.7, 33.8], 'dark', [6, 0, 0], [0, 6.2, 30], 8);

cube('lf_upper', 'lf_upper_leg', [3.6, 3.8, -11], [6.2, 8.5, -7.3], 'dark', [0, 0, -18], [4.6, 8, -8.8], 5);
cube('lf_lower', 'lf_lower_leg', [4.1, 0.7, -10], [5.9, 4.2, -7.3], 'bark', [0, 0, 16], [5.1, 3.1, -8.2], 8);
cube('lf_foot', 'lf_root_foot', [2.6, -0.2, -11.8], [7.4, 1.1, -7.4], 'moss', [0, 0, 0], [5.4, 0.8, -8.6], 7);
cube('rf_upper', 'rf_upper_leg', [-6.2, 3.8, -11], [-3.6, 8.5, -7.3], 'moss', [0, 0, 18], [-4.6, 8, -8.8], 9);
cube('rf_lower', 'rf_lower_leg', [-5.9, 0.7, -10], [-4.1, 4.2, -7.3], 'moss', [0, 0, -16], [-5.1, 3.1, -8.2], 9);
cube('rf_foot', 'rf_root_foot', [-7.4, -0.2, -11.8], [-2.6, 1.1, -7.4], 'moss', [0, 0, 0], [-5.4, 0.8, -8.6], 9);
cube('lb_upper', 'lb_upper_leg', [3.9, 4.3, 7.1], [6.7, 8.2, 10.5], 'dark', [0, 0, -8], [4.9, 7.6, 8.4], 5);
cube('lb_lower', 'lb_lower_leg', [4.6, 1.5, 9.4], [6.4, 4.6, 11.8], 'bark', [0, 0, 10], [5.4, 3.7, 10.5], 6);
cube('lb_foot', 'lb_root_foot', [3.1, 0.2, 9.6], [7.9, 1.3, 13.2], 'moss', [0, 0, 0], [5.8, 1.1, 11.1], 7);
cube('rb_upper', 'rb_upper_leg', [-6.7, 4.3, 7.1], [-3.9, 8.2, 10.5], 'bark', [0, 0, 8], [-4.9, 7.6, 8.4], 8);
cube('rb_lower', 'rb_lower_leg', [-6.4, 1.5, 9.4], [-4.6, 4.6, 11.8], 'dark', [0, 0, -10], [-5.4, 3.7, 10.5], 1);
cube('rb_foot', 'rb_root_foot', [-7.9, 0.2, 9.6], [-3.1, 1.3, 13.2], 'glow', [0, 0, 0], [-5.8, 1.1, 11.1], 3);
cube('hitbox', 'hitbox_cube', [-8, 0, -21], [8, 14, 14], 'glow', [0, 0, 0], [0, 7, -3.5], 5);

const model = {
  meta: { format_version: '5.0', model_format: 'free', box_uv: false },
  name: ID,
  model_identifier: ID,
  visible_box: [0, 1, 0],
  variable_placeholders: '',
  multi_file_ruleset: '',
  variable_placeholder_buttons: [],
  timeline_setups: [],
  unhandled_root_fields: {},
  resolution: { width: 64, height: 64 },
  elements,
  groups: Array.from(bones.values()).map((entry) => ({
    name: entry.name,
    uuid: entry.uuid,
    export: true,
    locked: false,
    scope: 0,
    selected: false,
    _static: { properties: {}, temp_data: {} },
    origin: entry.origin,
    rotation: entry.rotation,
    color: 0,
    children: [],
    reset: false,
    shade: true,
    mirror_uv: false,
    visibility: entry.visibility,
    autouv: 0,
    isOpen: false,
    primary_selected: false,
  })),
  outliner: [outliner('hitbox'), outliner('root')],
  textures: [
    {
      path: '',
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
      uuid: nextUuid(),
      relative_path: `${ID}.png`,
      name: `${ID}.png`,
      width: 64,
      height: 64,
      uv_width: 64,
      uv_height: 64,
      use_as_default: false,
      layers_enabled: false,
      sync_to_project: '',
      file_format: 'png',
      wrap_mode: 'limited',
      pbr_channel: 'color',
      fps: 7,
      source: texture(),
    },
  ],
};

fs.mkdirSync(OUTPUT_DIR, { recursive: true });
if (fs.existsSync(OUTPUT)) {
  const stamp = new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14);
  const backupDir = path.join(BACKUPS, stamp);
  fs.mkdirSync(backupDir, { recursive: true });
  fs.copyFileSync(OUTPUT, path.join(backupDir, `${ID}.bbmodel`));
}

fs.writeFileSync(OUTPUT, JSON.stringify(model, null, 2));
console.log(`wrote ${OUTPUT}`);
