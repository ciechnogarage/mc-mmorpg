const assert = require('assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { inspectBlueprint } = require('./check_modelengine_quality');

function cube(uuid, name, from = [0, 0, 0], to = [4, 4, 4]) {
  return {
    uuid,
    name,
    from,
    to,
    faces: {
      north: { uv: [0, 0, 4, 4], texture: 0 },
    },
  };
}

function writeFixture(mutator) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'model-quality-'));
  const blueprintPath = path.join(dir, 'fixture.bbmodel');
  const data = {
    resolution: { width: 16, height: 16 },
    textures: [{ name: 'fixture.png', source: 'data:image/png;base64,AA==' }],
    elements: [cube('cube-body', 'body'), cube('cube-hitbox', 'hitbox', [-2, 0, -2], [2, 4, 2])],
    outliner: [
      { uuid: 'bone-body', name: 'body', origin: [0, 0, 0], children: ['cube-body'] },
      { uuid: 'bone-hitbox', name: 'hitbox', origin: [0, 4, 0], visibility: false, children: ['cube-hitbox'] },
    ],
    animations: [
      {
        uuid: 'anim-idle',
        name: 'idle',
        length: 1,
        loop: 'loop',
        animators: {
          'bone-body': {
            type: 'bone',
            keyframes: [
              { channel: 'rotation', time: 0, data_points: [{ x: 0, y: 0, z: 0 }] },
              { channel: 'rotation', time: 0.5, data_points: [{ x: 1, y: 0, z: 0 }] },
              { channel: 'rotation', time: 1, data_points: [{ x: 0, y: 0, z: 0 }] },
            ],
          },
        },
      },
    ],
  };
  mutator(data);
  fs.writeFileSync(blueprintPath, JSON.stringify(data));
  return blueprintPath;
}

function inspect(blueprintPath, stateAnimations = []) {
  return inspectBlueprint(
    { modelId: 'fixture', mobId: 'fixture', template: null, stateAnimations },
    { blueprintPath, manifestPath: `${blueprintPath}.quality.json`, strict: false },
  );
}

const cases = [
  ['orphan cube', (data) => data.elements.push(cube('orphan', 'orphan')), 'ORPHAN_ELEMENT'],
  ['duplicate UUID', (data) => { data.elements[1].uuid = 'cube-body'; }, 'DUPLICATE_UUID'],
  ['visible hitbox', (data) => { data.outliner[1].visibility = true; }, 'HITBOX_VISIBLE'],
  ['non-square hitbox', (data) => { data.elements[1].to[2] = 3; }, 'HITBOX_NOT_SQUARE_XZ'],
  ['bad UV', (data) => { data.elements[0].faces.north.uv = [0, 0, 20, 20]; }, 'UV_OUT_OF_BOUNDS'],
  ['missing declared state', () => {}, 'ANIMATION_MISSING:walk', ['walk']],
];

for (const [name, mutate, expected, stateAnimations] of cases) {
  const result = inspect(writeFixture(mutate), stateAnimations);
  assert(
    result.reasons.some((reason) => reason.startsWith(expected)),
    `${name}: expected ${expected}, got ${result.reasons.join(', ')}`,
  );
}

console.log(`MODELENGINE_QUALITY_TEST_PASS: ${cases.length} regression fixtures rejected as expected.`);
