import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const composePath = path.join(ROOT, 'MCMMORPG/docker/compose.yml');
const envExamplePath = path.join(ROOT, 'MCMMORPG/docker/.env.example');
const baselinePath = path.join(ROOT, 'docs/database-staging-baseline.md');

test('database staging baseline stays internal, health-gated, and secretless', () => {
  const compose = fs.readFileSync(composePath, 'utf8');
  const envExample = fs.readFileSync(envExamplePath, 'utf8');
  const baseline = fs.readFileSync(baselinePath, 'utf8');

  assert.match(compose, /^  mariadb:\n/m);
  assert.match(compose, /image: mariadb:11\.8/);
  assert.match(compose, /MARIADB_DATABASE: "\$\{MARIADB_DATABASE:-mcmmorpg\}"/);
  assert.match(compose, /mariadb-admin ping/);
  assert.doesNotMatch(compose, /3306:3306/);
  assert.match(compose, /condition: service_healthy/);

  assert.match(envExample, /^MARIADB_ROOT_PASSWORD=change-me$/m);
  assert.match(envExample, /^MARIADB_PASSWORD=change-me$/m);
  assert.match(baseline, /MCMMORPG\/docker\/\.env/);
  assert.match(baseline, /not tracked by Git/i);
});
