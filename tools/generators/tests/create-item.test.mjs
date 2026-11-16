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
const TARGET_REL = 'MCMMORPG/plugins/MMOItems/item/sword.yml';

function withFixture(callback) {
  const tempRoot = mkdtempSync(path.join(os.tmpdir(), 'create-item-'));
  try {
    cpSync(path.join(REPO_ROOT, 'docs'), path.join(tempRoot, 'docs'), { recursive: true });
    cpSync(path.join(REPO_ROOT, 'registry'), path.join(tempRoot, 'registry'), { recursive: true });
    mkdirSync(path.join(tempRoot, 'MCMMORPG/plugins/MMOItems/item'), { recursive: true });
    writeFileSync(path.join(tempRoot, TARGET_REL), 'LONG_SWORD:\n  base: SWORD\n', 'utf8');
    callback(tempRoot);
  } finally {
    rmSync(tempRoot, { recursive: true, force: true });
  }
}

function runGenerator(root, args = []) {
  return execFileSync('node', [GENERATOR, ...args], {
    cwd: root,
    encoding: 'utf8',
    env: { ...process.env, MC_CONTENT_ROOT: root },
  });
}

test('dry-run emits deterministic planned writes', () => {
  withFixture((root) => {
    const first = runGenerator(root, ['--dry-run']);
    const second = runGenerator(root, ['--dry-run']);
    assert.equal(first, second);
    const payload = JSON.parse(first);
    assert.equal(payload.mode, 'dry-run');
    assert.equal(payload.subject, 'item.weapon.sword.ashen_vanguard');
    assert.equal(payload.writes.some((entry) => entry.path === 'content/items/weapons/ashen_vanguard.yaml'), true);
    assert.equal(payload.writes.some((entry) => entry.path === TARGET_REL), true);
    assert.equal(payload.writes.some((entry) => entry.path.startsWith('docs/production/contracts/')), false);
  });
});

test('write mode creates source, projection, registry updates, and target section', () => {
  withFixture((root) => {
    runGenerator(root);
    const source = readFileSync(path.join(root, 'content/items/weapons/ashen_vanguard.yaml'), 'utf8');
    const projection = readFileSync(path.join(root, 'build/runtime/items/ashen_vanguard.yml'), 'utf8');
    const registry = readFileSync(path.join(root, 'registry/content/items/item.weapon.sword.ashen_vanguard.yaml'), 'utf8');
    const target = readFileSync(path.join(root, TARGET_REL), 'utf8');
    assert.match(source, /id: item\.weapon\.sword\.ashen_vanguard/);
    assert.match(projection, /^ASHEN_VANGUARD:\n/m);
    assert.match(registry, /target_file: MCMMORPG\/plugins\/MMOItems\/item\/sword\.yml/);
    assert.match(target, /^ASHEN_VANGUARD:\n/m);
  });
});
