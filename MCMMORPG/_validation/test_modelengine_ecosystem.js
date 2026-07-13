const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { inspectModel } = require('./check_modelengine_ecosystem');

function fixture(change = () => {}) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'model-ecosystem-'));
  const blueprintPath = path.join(dir, 'fixture.bbmodel');
  const manifestPath = path.join(dir, 'fixture.quality.json');
  const skillPath = path.join(dir, 'skills.yml');
  const mobPath = path.join(dir, 'mobs.yml');
  const blueprint = {
    elements: [
      { uuid: 'cube', name: 'hand', from: [0, 0, 0], to: [2, 2, 2] },
      { uuid: 'zone-cube', name: 'damage_zone_head', from: [0, 0, 0], to: [2, 2, 2] },
    ],
    outliner: [{
      uuid: 'bone',
      name: 'right_hand',
      origin: [0, 0, 0],
      children: ['cube', { uuid: 'zone', name: 'ob_head', visibility: false, children: ['zone-cube'] }],
    }],
    animations: [{ name: 'attack', length: 1, animators: {} }],
  };
  const manifest = {
    schemaVersion: 2,
    interactionBones: ['right_hand'],
    damageZones: [{ bone: 'ob_head', multiplier: 1.5 }],
    damageZoneBinding: {
      skill: 'fixture_damage_zones',
      skillSourceFile: 'skills.yml',
      mobSourceFile: 'mobs.yml',
      requiredHooks: ['onSpawn', 'onLoad'],
    },
    animationContract: [{ animation: 'attack', impactTicks: [10], bones: ['right_hand'] }],
    skillBindings: [{
      skill: 'fixture_attack',
      animation: 'attack',
      sourceFile: 'skills.yml',
      modelParts: ['right_hand'],
      impactTicks: [10],
      requiresGcd: true,
      requiresModelLock: true,
    }],
    integrationFiles: ['skills.yml'],
  };
  const skill = [
    'fixture_attack:',
    '  Skills:',
    '    - gcd{ticks=20}',
    '    - lockmodel{l=true} @self',
    '    - state{s=attack} @self',
    '    - totem{delay=10} @modelpart{pid=right_hand}',
    'fixture_damage_zones:',
    '  Skills:',
    '    - hitboxconfig{model=fixture;part=ob_head;pass=1.5} @self',
    '',
  ].join('\n');
  change({ blueprint, manifest, skillPath });
  fs.writeFileSync(blueprintPath, JSON.stringify(blueprint));
  fs.writeFileSync(manifestPath, JSON.stringify(manifest));
  if (!fs.existsSync(skillPath)) fs.writeFileSync(skillPath, skill);
  fs.writeFileSync(mobPath, [
    'fixture:',
    '  Skills:',
    '    - skill{s=fixture_damage_zones} @self ~onSpawn',
    '    - skill{s=fixture_damage_zones} @self ~onLoad',
    '',
  ].join('\n'));
  return { blueprintPath, manifestPath };
}

const valid = fixture();
const validResult = inspectModel('fixture', valid);
assert.strictEqual(validResult.verdict, 'PASS', validResult.reasons.join(','));

const cases = [
  ['missing interaction bone', ({ manifest }) => { manifest.interactionBones = ['missing']; }, 'INTERACTION_BONE_MISSING'],
  ['impact outside animation', ({ manifest }) => { manifest.animationContract[0].impactTicks = [30]; }, 'IMPACT_TICK_OUTSIDE_ANIMATION'],
  ['missing skill source', ({ manifest }) => { manifest.skillBindings[0].sourceFile = 'missing.yml'; }, 'SKILL_SOURCE_MISSING'],
  ['missing state play', ({ skillPath }) => { fs.writeFileSync(skillPath, 'fixture_attack:\n  Skills:\n    - gcd{ticks=20}\n'); }, 'SKILL_STATE_NOT_PLAYED'],
  ['missing modelpart use', ({ skillPath }) => { fs.writeFileSync(skillPath, 'fixture_attack:\n  Skills:\n    - gcd{ticks=20}\n    - lockmodel{l=true}\n    - state{s=attack}\n    - delay 10\n'); }, 'SKILL_MODELPART_NOT_USED'],
  ['missing impact delay', ({ skillPath }) => { fs.writeFileSync(skillPath, 'fixture_attack:\n  Skills:\n    - gcd{ticks=20}\n    - lockmodel{l=true}\n    - state{s=attack}\n    - totem{delay=2} @modelpart{pid=right_hand}\n'); }, 'SKILL_IMPACT_DELAY_MISSING'],
  ['missing damage zone', ({ manifest }) => { manifest.damageZones[0].bone = 'ob_missing'; }, 'DAMAGE_ZONE_BONE_MISSING'],
  ['wrong damage multiplier', ({ manifest }) => { manifest.damageZones[0].multiplier = 2; }, 'DAMAGE_ZONE_MULTIPLIER_MISMATCH'],
];

for (const [name, change, expected] of cases) {
  const result = inspectModel('fixture', fixture(change));
  assert(result.reasons.some((reason) => reason.startsWith(expected)), `${name}: ${result.reasons.join(',')}`);
}

console.log(`MODELENGINE_ECOSYSTEM_TEST_PASS: ${cases.length} broken ecosystem fixtures rejected.`);
