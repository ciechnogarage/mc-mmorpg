import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const REGISTRY = path.join(ROOT, 'docs', 'contracts', 'character-scope-registry.yaml');
const CONTRACT = path.join(ROOT, 'docs', 'contracts', 'account-character-contract.yaml');

function runRegistry(args) {
  return spawnSync('python3', ['scripts/check-character-scope-registry.py', ...args], {
    cwd: ROOT,
    encoding: 'utf8',
  });
}

function runContracts(args) {
  return spawnSync('python3', ['scripts/check-contracts-v2.py', ...args], {
    cwd: ROOT,
    encoding: 'utf8',
  });
}

function runMutatedCopy(sourcePath, name, mutate, runner) {
  const source = readFileSync(sourcePath, 'utf8');
  const mutated = mutate(source);
  assert.notEqual(mutated, source, `mutation ${name} did not change source`);
  const dir = mkdtempSync(path.join(tmpdir(), 'd04-d02-'));
  const target = path.join(dir, path.basename(sourcePath));
  writeFileSync(target, mutated);
  return runner([target]);
}

test('character scope registry validator accepts canonical D04 persisted-state entries', () => {
  const result = runRegistry(['docs/contracts/character-scope-registry.yaml']);
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /character-scope-registry: static declarations ok/);
});

test('character scope registry validator rejects missing D04 owner annotation', () => {
  const result = runMutatedCopy(
    REGISTRY,
    'remove-semantic-owner-domain',
    (source) =>
      source.replace(
        /(key: character\.identity\.race_id[\s\S]*?semantic_owner_domain: )"04"/,
        '$1"99"',
      ),
    runRegistry,
  );
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /semantic_owner_domain must be '04'/);
});

test('contracts v2 validator accepts canonical D04 character state bucket', () => {
  const result = runContracts(['docs/contracts/account-character-contract.yaml']);
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /account-character-contract\.yaml: ok/);
});

test('contracts v2 validator rejects missing D04 character state bucket', () => {
  const result = runMutatedCopy(
    CONTRACT,
    'remove-domain04-character-state',
    (source) => source.replace('      domain04_character_state:', '      domain04_character_state_removed:'),
    runContracts,
  );
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /domain04_character_state/);
});
