import assert from 'node:assert/strict';
import { cpSync, mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import os from 'node:os';
import path from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../..');
const GENERATOR = path.join(REPO_ROOT, 'tools/generators/create-item');

function parseYaml(filePath) {
  const output = execFileSync(
    'python3',
    [
      '-c',
      [
        'import json, sys, yaml',
        'with open(sys.argv[1], "r", encoding="utf-8") as handle:',
        '    data = yaml.safe_load(handle)',
        'print(json.dumps(data if data is not None else {}))',
      ].join('\n'),
      filePath,
    ],
    { encoding: 'utf8' },
  );

  return JSON.parse(output);
}

function withFixture(callback) {
  const tempRoot = mkdtempSync(path.join(os.tmpdir(), 'create-item-registry-'));
  try {
    cpSync(path.join(REPO_ROOT, 'docs'), path.join(tempRoot, 'docs'), { recursive: true });
    cpSync(path.join(REPO_ROOT, 'registry'), path.join(tempRoot, 'registry'), { recursive: true });
    mkdirSync(path.join(tempRoot, 'MCMMORPG/plugins/MMOItems/item'), { recursive: true });
    writeFileSync(path.join(tempRoot, 'MCMMORPG/plugins/MMOItems/item/sword.yml'), 'LONG_SWORD:\n  base: SWORD\n', 'utf8');
    callback(tempRoot);
  } finally {
    rmSync(tempRoot, { recursive: true, force: true });
  }
}

test('generator writes a parseable content registry aggregate with entries', () =>
  withFixture((root) => {
    execFileSync('node', [GENERATOR], {
      cwd: root,
      encoding: 'utf8',
      env: { ...process.env, MC_CONTENT_ROOT: root },
    });

    const registry = parseYaml(path.join(root, 'registry/content-registry.yaml'));
    assert.ok(Array.isArray(registry.entries));
    assert.equal(registry.entries.length, 1);
    assert.equal(registry.entries[0].subject_id, 'item.weapon.sword.ashen_vanguard');
    assert.equal(registry.entries[0].content_type, 'item');
  }));
