import { createHash } from 'node:crypto';
import { mkdtemp, mkdir, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { canonicalHash } from '../src/contract/canonical.mjs';

const LIFECYCLE = Object.freeze({
  allowed_operations: [
    'inventory',
    'backup',
    'prepare-staging',
    'compose-stop',
    'compose-start',
    'compose-run-test',
    'health-check',
    'deploy',
    'rollback',
  ],
  single_service_only: true,
  no_dependencies: true,
  recreate: false,
  require_current_config_digest: true,
});

function mutationPolicy(path) {
  const policy = {
    schema_version: 1,
    default_class: 'excluded',
    entries: [
      {
        path,
        class: 'rollback',
        backup_method: 'copy',
        restore_method: 'replace-from-backup',
      },
    ],
  };
  return { ...policy, manifest_sha256: canonicalHash(policy) };
}

export async function buildRegistryFixture() {
  const root = await mkdtemp(join(tmpdir(), 'mcmmorpg-registry-'));
  const composeDir = join(root, 'docker');
  const composePath = join(composeDir, 'compose.yml');
  const dataPaths = {
    world: join(composeDir, 'servers', 'world'),
    items: join(composeDir, 'servers', 'items'),
    hub: join(composeDir, 'servers', 'hub'),
    velocity: join(composeDir, 'velocity'),
  };

  const compose = `name: mcmmorpg
services:
  velocity:
    volumes:
      - ./velocity:/server
  srv-world:
    profiles: ["world"]
    volumes:
      - ./servers/world:/data
  srv-items:
    profiles: ["items"]
    volumes:
      - ./servers/items:/data
  srv-hub:
    profiles: ["hub"]
    volumes:
      - ./servers/hub:/data
`;

  await mkdir(composeDir, { recursive: true });
  await writeFile(composePath, compose);

  for (const target of Object.keys(dataPaths)) {
    await mkdir(join(dataPaths[target], 'plugins'), { recursive: true });
  }

  await mkdir(join(dataPaths.world, 'plugins', 'Example'), { recursive: true });
  await mkdir(join(dataPaths.items, 'plugins', 'Example'), { recursive: true });
  await mkdir(join(dataPaths.world, 'world', 'playerdata'), { recursive: true });
  await writeFile(join(dataPaths.world, 'plugins', 'Example-1.0.0.jar'), 'world-jar-v1\n');
  await writeFile(join(dataPaths.items, 'plugins', 'Example-2.0.0.jar'), 'items-jar-v2\n');
  await writeFile(join(dataPaths.world, 'plugins', 'Example', 'config.yml'), 'enabled: true\n');
  await writeFile(join(dataPaths.items, 'plugins', 'Example', 'config.yml'), 'enabled: false\n');
  await writeFile(join(dataPaths.world, '.env'), 'SECRET=do-not-read\n');
  await writeFile(join(dataPaths.world, 'world', 'playerdata', 'player.dat'), 'private-player-data\n');

  const configSha256 = createHash('sha256').update(compose).digest('hex');
  const targets = {
    schema_version: 1,
    compose: {
      path: composePath,
      project: 'mcmmorpg',
      config_sha256: configSha256,
    },
    allowed_host_root: composeDir,
    targets: Object.fromEntries([
      ['world', targetDefinition('srv-world', 'world', dataPaths.world, '/data', true, 'single-backend')],
      ['items', targetDefinition('srv-items', 'items', dataPaths.items, '/data', true, 'single-backend')],
      ['hub', targetDefinition('srv-hub', 'hub', dataPaths.hub, '/data', true, 'single-backend')],
      ['velocity', targetDefinition('velocity', null, dataPaths.velocity, '/server', false, 'global-entrypoint')],
    ]),
  };

  const plugins = {
    schema_version: 1,
    installations: [
      {
        plugin_id: 'example',
        target: 'world',
        installed_version: '1.0.0',
        jar_path: 'plugins/Example-1.0.0.jar',
        config_paths: ['plugins/Example/config.yml'],
        mutation_policy: mutationPolicy('plugins/Example'),
      },
      {
        plugin_id: 'example',
        target: 'items',
        installed_version: '2.0.0',
        jar_path: 'plugins/Example-2.0.0.jar',
        config_paths: ['plugins/Example/config.yml'],
        mutation_policy: mutationPolicy('plugins/Example'),
      },
    ],
  };

  return { root, composePath, dataPaths, targets, plugins };
}

function targetDefinition(service, profile, hostDataPath, containerDataPath, pilotEligible, blastRadius) {
  return {
    compose_project: 'mcmmorpg',
    compose_service: service,
    compose_profile: profile,
    host_data_path: hostDataPath,
    container_data_path: containerDataPath,
    plugin_directory: 'plugins',
    pilot_eligible: pilotEligible,
    blast_radius: blastRadius,
    lifecycle: structuredClone(LIFECYCLE),
  };
}

export function refreshPolicyHash(policy) {
  const { manifest_sha256: ignored, ...content } = policy;
  policy.manifest_sha256 = canonicalHash(content);
  return policy;
}

export function expectedSha256(text) {
  return createHash('sha256').update(text).digest('hex');
}
