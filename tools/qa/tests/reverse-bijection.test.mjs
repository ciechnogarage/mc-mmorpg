import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';

import { validateReverseBijection } from '../validate-reverse-bijection.mjs';

function withFixture({ sources, registry }, callback) {
  const rootDir = mkdtempSync(path.join(os.tmpdir(), 'reverse-bijection-'));

  try {
    mkdirSync(path.join(rootDir, 'registry/content/items'), { recursive: true });

    for (const source of sources) {
      writeFileSync(path.join(rootDir, source.path), source.contents, 'utf8');
    }

    writeFileSync(path.join(rootDir, 'registry/content-registry.yaml'), registry, 'utf8');
    return callback(rootDir);
  } finally {
    rmSync(rootDir, { recursive: true, force: true });
  }
}

function sourceRecord({
  id,
  targetTuple = `MMOItems:MCMMORPG/plugins/MMOItems/item/WEAPON.yml#${id.split('.').at(-1).toUpperCase()}`,
  targetFile = 'MCMMORPG/plugins/MMOItems/item/WEAPON.yml',
  sectionKey = id.split('.').at(-1).toUpperCase(),
  stage = 'runtime_loaded',
  deprecated = false,
  plugin = 'MMOItems',
} = {}) {
  const runtimeLines = [
    'runtime_identity:',
    `  plugin: ${plugin}`,
  ];

  if (targetFile !== null) {
    runtimeLines.push(`  target_file: ${targetFile}`);
  }
  if (sectionKey !== null) {
    runtimeLines.push(`  section_key: ${sectionKey}`);
  }
  if (targetTuple !== null) {
    runtimeLines.push(`  target_tuple: ${targetTuple}`);
  }
  runtimeLines.push('  cardinality: one_subject_per_target');

  return [
    'subject:',
    `  id: ${id}`,
    'display_name: Test Item',
    'content_type: item',
    ...runtimeLines,
    'lifecycle:',
    `  stage: ${stage}`,
    `deprecated: ${deprecated ? 'true' : 'false'}`,
    '',
  ].join('\n');
}

function registryRecord({ subjectId, targetTuple, lifecycleStage = 'runtime_loaded' }) {
  return [
    '  - subject_id: ' + subjectId,
    '    registry_path: registry/content/items/' + subjectId + '.yaml',
    '    content_type: item',
    '    template_id: item.weapon.sword',
    '    runtime_plugin: MMOItems',
    '    target_tuple: ' + targetTuple,
    '    lifecycle_stage: ' + lifecycleStage,
    '    runtime_status: loaded',
  ].join('\n');
}

function registryFile(records) {
  const recordBlock = records.length > 0 ? records.join('\n') : '  []';
  return [
    'schema_version: 1',
    'registry_id: content-registry',
    'generated_from:',
    '  - registry/content/items',
    'records:',
    recordBlock,
    '',
  ].join('\n');
}

test('accepts clean MMOItems pilot occupancy', () => {
  const tuple = 'MMOItems:MCMMORPG/plugins/MMOItems/item/WEAPON.yml#ASHEN_VANGUARD';

  withFixture(
    {
      sources: [
        {
          path: 'registry/content/items/item.weapon.sword.ashen_vanguard.yaml',
          contents: sourceRecord({ id: 'item.weapon.sword.ashen_vanguard', targetTuple: tuple, sectionKey: 'ASHEN_VANGUARD' }),
        },
      ],
      registry: registryFile([registryRecord({ subjectId: 'item.weapon.sword.ashen_vanguard', targetTuple: tuple })]),
    },
    (rootDir) => {
      const result = validateReverseBijection(rootDir);
      assert.equal(result.ok, true);
      assert.equal(result.active_subjects, 1);
      assert.equal(result.occupied_targets, 1);
    },
  );
});

test('rejects duplicate active subjects on one target tuple', () => {
  const tuple = 'MMOItems:MCMMORPG/plugins/MMOItems/item/WEAPON.yml#COLLISION';

  withFixture(
    {
      sources: [
        {
          path: 'registry/content/items/item.weapon.sword.alpha.yaml',
          contents: sourceRecord({ id: 'item.weapon.sword.alpha', targetTuple: tuple, sectionKey: 'COLLISION' }),
        },
        {
          path: 'registry/content/items/item.weapon.sword.beta.yaml',
          contents: sourceRecord({ id: 'item.weapon.sword.beta', targetTuple: tuple, sectionKey: 'COLLISION' }),
        },
      ],
      registry: registryFile([
        registryRecord({ subjectId: 'item.weapon.sword.alpha', targetTuple: tuple }),
        registryRecord({ subjectId: 'item.weapon.sword.beta', targetTuple: tuple }),
      ]),
    },
    (rootDir) => {
      assert.throws(() => validateReverseBijection(rootDir), /DUPLICATE_TARGET/);
    },
  );
});

test('rejects orphan registry occupancy', () => {
  const tuple = 'MMOItems:MCMMORPG/plugins/MMOItems/item/WEAPON.yml#ORPHANED';

  withFixture(
    {
      sources: [
        {
          path: 'registry/content/items/item.weapon.sword.alpha.yaml',
          contents: sourceRecord({ id: 'item.weapon.sword.alpha', targetTuple: tuple, sectionKey: 'ORPHANED' }),
        },
      ],
      registry: registryFile([registryRecord({ subjectId: 'item.weapon.sword.legacy', targetTuple: tuple })]),
    },
    (rootDir) => {
      assert.throws(() => validateReverseBijection(rootDir), /ORPHAN_TARGET/);
    },
  );
});

test('rejects runtime-loaded records without target occupancy metadata', () => {
  withFixture(
    {
      sources: [
        {
          path: 'registry/content/items/item.weapon.sword.alpha.yaml',
          contents: sourceRecord({
            id: 'item.weapon.sword.alpha',
            targetTuple: null,
            targetFile: 'MCMMORPG/plugins/MMOItems/item/WEAPON.yml',
            sectionKey: null,
          }),
        },
      ],
      registry: registryFile([]),
    },
    (rootDir) => {
      assert.throws(() => validateReverseBijection(rootDir), /MISSING_RUNTIME_TARGET/);
    },
  );
});
