import test from 'node:test';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import {
  RegistryValidationError,
  loadRegistry,
  validateRegistry,
} from '../src/registry/index.mjs';
import { buildRegistryFixture, refreshPolicyHash } from './registry-fixtures.mjs';

const TARGETS_PATH = fileURLToPath(new URL('../registry/targets.json', import.meta.url));
const PLUGINS_PATH = fileURLToPath(new URL('../registry/plugins.json', import.meta.url));

function rejectsWithCode(promise, code) {
  return assert.rejects(promise, (error) => {
    assert.ok(error instanceof RegistryValidationError);
    assert.equal(error.code, code);
    return true;
  });
}

test('shipped registry exactly matches the four Compose targets and current config digest', async () => {
  const registry = await loadRegistry({ targetsPath: TARGETS_PATH, pluginsPath: PLUGINS_PATH });

  assert.deepEqual(Object.keys(registry.targets.targets).sort(), ['hub', 'items', 'velocity', 'world']);
  assert.deepEqual(
    Object.fromEntries(Object.entries(registry.targets.targets).map(([id, target]) => [id, {
      service: target.compose_service,
      profile: target.compose_profile,
      container: target.container_data_path,
    }])),
    {
      world: { service: 'srv-world', profile: 'world', container: '/data' },
      items: { service: 'srv-items', profile: 'items', container: '/data' },
      hub: { service: 'srv-hub', profile: 'hub', container: '/data' },
      velocity: { service: 'velocity', profile: null, container: '/server' },
    },
  );
  assert.equal(registry.targets.targets.velocity.pilot_eligible, false);
  for (const id of ['world', 'items', 'hub']) {
    const target = registry.targets.targets[id];
    assert.equal(target.blast_radius, 'single-backend');
    assert.equal(target.lifecycle.single_service_only, true);
    assert.equal(target.lifecycle.no_dependencies, true);
    assert.equal(target.lifecycle.recreate, false);
    assert.equal(target.lifecycle.require_current_config_digest, true);
  }
});

test('same plugin may have divergent versions and configuration per target', async () => {
  const fixture = await buildRegistryFixture();
  const registry = await validateRegistry({ targets: fixture.targets, plugins: fixture.plugins });
  const example = registry.plugins.installations.filter((entry) => entry.plugin_id === 'example');

  assert.deepEqual(example.map((entry) => [entry.target, entry.installed_version]), [
    ['world', '1.0.0'],
    ['items', '2.0.0'],
  ]);
  assert.notEqual(example[0].jar_path, example[1].jar_path);
});

test('unknown Compose service and profile mismatch fail closed', async () => {
  const fixture = await buildRegistryFixture();
  const badService = structuredClone(fixture.targets);
  badService.targets.world.compose_service = 'srv-does-not-exist';
  await rejectsWithCode(validateRegistry({ targets: badService, plugins: fixture.plugins }), 'compose-service-missing');

  const badProfile = structuredClone(fixture.targets);
  badProfile.targets.world.compose_profile = 'items';
  await rejectsWithCode(validateRegistry({ targets: badProfile, plugins: fixture.plugins }), 'compose-profile-mismatch');
});

test('stale Compose config digest is rejected', async () => {
  const fixture = await buildRegistryFixture();
  const targets = structuredClone(fixture.targets);
  targets.compose.config_sha256 = '0'.repeat(64);
  await rejectsWithCode(validateRegistry({ targets, plugins: fixture.plugins }), 'compose-config-digest-mismatch');
});

test('lifecycle cannot expand beyond one digest-checked service', async () => {
  const fixture = await buildRegistryFixture();
  const targets = structuredClone(fixture.targets);
  targets.targets.world.lifecycle.no_dependencies = false;
  await rejectsWithCode(
    validateRegistry({ targets, plugins: fixture.plugins }),
    'lifecycle-restrictions-invalid',
  );
});

test('target paths reject traversal and locations outside the allowed root', async () => {
  const fixture = await buildRegistryFixture();
  const traversing = structuredClone(fixture.targets);
  traversing.targets.world.host_data_path = `${fixture.dataPaths.world}/../items`;
  await rejectsWithCode(validateRegistry({ targets: traversing, plugins: fixture.plugins }), 'path-noncanonical');

  const outside = structuredClone(fixture.targets);
  outside.targets.world.host_data_path = fixture.root;
  await rejectsWithCode(validateRegistry({ targets: outside, plugins: fixture.plugins }), 'path-outside-root');
});

test('mutation policies require a version, manifest hash, closed classes, and canonical in-root paths', async (t) => {
  const cases = [
    ['missing policy', 'mutation-policy-missing', (plugins) => delete plugins.installations[0].mutation_policy],
    ['unsupported class', 'mutation-class-invalid', (plugins) => {
      plugins.installations[0].mutation_policy.entries[0].class = 'mutable';
      refreshPolicyHash(plugins.installations[0].mutation_policy);
    }],
    ['noncanonical path', 'path-noncanonical', (plugins) => {
      plugins.installations[0].mutation_policy.entries[0].path = 'plugins/Example/../Other';
      refreshPolicyHash(plugins.installations[0].mutation_policy);
    }],
    ['outside-root path', 'path-outside-root', (plugins) => {
      plugins.installations[0].mutation_policy.entries[0].path = '/tmp/outside';
      refreshPolicyHash(plugins.installations[0].mutation_policy);
    }],
    ['wrong manifest hash', 'mutation-manifest-hash-mismatch', (plugins) => {
      plugins.installations[0].mutation_policy.manifest_sha256 = 'f'.repeat(64);
    }],
  ];

  for (const [name, code, mutate] of cases) {
    await t.test(name, async () => {
      const fixture = await buildRegistryFixture();
      const plugins = structuredClone(fixture.plugins);
      mutate(plugins);
      await rejectsWithCode(validateRegistry({ targets: fixture.targets, plugins }), code);
    });
  }
});

test('world and player-data paths can never be classified as rollback', async (t) => {
  for (const protectedPath of ['world', 'world_nether/region', 'plugins/Auth/playerdata/user.dat']) {
    await t.test(protectedPath, async () => {
      const fixture = await buildRegistryFixture();
      const plugins = structuredClone(fixture.plugins);
      plugins.installations[0].mutation_policy.entries[0].path = protectedPath;
      refreshPolicyHash(plugins.installations[0].mutation_policy);
      await rejectsWithCode(
        validateRegistry({ targets: fixture.targets, plugins }),
        'protected-path-rollback',
      );
    });
  }
});
