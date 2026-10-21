import test from 'node:test';
import assert from 'node:assert/strict';
import { buildBatchReport, discoverBatchOutcomes } from '../src/batch/index.mjs';

const PUBLIC_DNS = async () => [{ address: '93.184.216.34', family: 4 }];

function inventoryFor(plugin) {
  return {
    schema_version: 1,
    scanned_at: '2026-07-21T12:00:00.000Z',
    minecraft_version: '1.21.11',
    manifest: { sha256: 'a'.repeat(64), plugin_count: 1 },
    installation_count: 1,
    targets: [{ target: 'world', installation_count: 1 }],
    plugins: [{
      manifest: {
        name: plugin.name,
        current_version: plugin.currentVersion,
        jar: `${plugin.name}-${plugin.currentVersion}.jar`,
        license_status: 'unknown',
      },
      installations: [{
        target: 'world',
        installed_version: plugin.currentVersion,
        jar: { path: `plugins/${plugin.name}-${plugin.currentVersion}.jar`, sha256: 'b'.repeat(64) },
      }],
    }],
  };
}

function registrationFor({ name, pluginId, adapter, url }) {
  return {
    schema_version: 1,
    registrations: [{
      manifest_name: name,
      plugin_id: pluginId,
      kind: 'premium-or-login',
      adapter,
      official_url: url,
      reason: `${adapter} official source is authoritative; do not infer updates from Modrinth search.`,
    }],
    modrinth_search: {
      adapter: 'modrinth-search-v2',
      max_results: 10,
      search: {
        host: 'api.modrinth.com',
        project_path: '/v2',
        path_prefix: '/v2/search',
        max_bytes: 1024,
      },
      project: {
        host: 'api.modrinth.com',
        project_path: '/v2',
        path_prefix: '/v2/project/',
        max_bytes: 1024,
      },
      artifact: {
        host: 'cdn.modrinth.com',
        max_bytes: 1024,
      },
      redirects: { max: 0, same_host_only: true },
    },
  };
}

async function discoverVendorFixture({ plugin, registration, html, channel = 'stable' }) {
  const requests = [];
  const outcomes = await discoverBatchOutcomes({
    inventory: inventoryFor(plugin),
    registrations: registrationFor(registration),
    registry: { plugins: { installations: [] } },
    registryInventory: { targets: [] },
    policy: { schema_version: 1, sources: [] },
    projectRoot: '/fixtures',
    operator: 'codex',
    channel,
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async ({ url }) => {
      requests.push(url);
      if (url === registration.url) {
        return { statusCode: 200, headers: {}, body: Buffer.from(html) };
      }
      throw new Error(`unexpected URL: ${url}`);
    },
  });

  return {
    requests,
    outcome: outcomes.get(plugin.name),
    report: buildBatchReport({
      inventory: inventoryFor(plugin),
      registrations: registrationFor(registration),
      outcomes,
      generatedAt: '2026-07-21T12:30:00.000Z',
      channel,
    }),
  };
}

test('mythiccraft vendor adapter reports stable metadata without Modrinth search or candidate', async () => {
  const registration = {
    name: 'MythicDungeons',
    pluginId: 'mythicdungeons',
    adapter: 'mythiccraft-resource',
    url: 'https://mythiccraft.io/index.php?resources/mythic-dungeons.869/',
  };
  const result = await discoverVendorFixture({
    plugin: { name: 'MythicDungeons', currentVersion: '2.0.1-SNAPSHOT' },
    registration,
    html: '<title>Mythic Dungeons 2.1.0</title><meta name="description" content="Dungeon builder for Paper 1.21.11"><h1>Version 2.1.0</h1>',
  });

  assert.deepEqual(result.requests, [registration.url]);
  assert.equal(result.outcome.kind, 'vendor-metadata');
  assert.equal(result.outcome.compatibility.compatible, true);
  assert.equal(result.outcome.vendor_metadata.download_authorized, false);
  assert.match(result.outcome.vendor_metadata.content_sha256, /^[0-9a-f]{64}$/);
  assert.equal(result.outcome.available_versions[0].channel, 'stable');

  const plugin = result.report.plugins[0];
  assert.equal(plugin.status, 'premium-or-login');
  assert.equal(plugin.candidate, null);
  assert.equal(plugin.vendor_metadata.adapter, 'mythiccraft-resource');
  assert.equal(plugin.compatibility.compatible, true);
});

test('phoenixdevt vendor adapter reports dev channel metadata without stable fallback', async () => {
  const registration = {
    name: 'MMOCore',
    pluginId: 'mmocore',
    adapter: 'phoenixdevt-devbuilds',
    url: 'https://phoenixdevt.fr/devbuilds',
  };
  const result = await discoverVendorFixture({
    plugin: { name: 'MMOCore', currentVersion: '1.13.1' },
    registration,
    html: '<title>Phoenix Development Dev Builds</title><p>MMOCore build 1.14.0-dev supports Paper 1.21.11.</p>',
  });

  assert.deepEqual(result.requests, [registration.url]);
  assert.equal(result.outcome.available_versions[0].channel, 'dev');
  assert.equal(result.outcome.available_versions[0].version_type, 'alpha');
  assert.equal(result.outcome.vendor_metadata.channel_policy, 'No fallback between stable, snapshot, and dev channels.');
  assert.ok(result.report.plugins[0].vendor_metadata.risk_notes.includes('phoenixdevt-devbuild-account-auth-required'));
});

test('voxel shop adapter records token auth boundary and unknown compatibility when page lacks Paper evidence', async () => {
  const registration = {
    name: 'Vouchers',
    pluginId: 'vouchers',
    adapter: 'voxel-shop-updates',
    url: 'https://updates.voxelplugins.com/vouchers',
  };
  const result = await discoverVendorFixture({
    plugin: { name: 'Vouchers', currentVersion: '3.8.0' },
    registration,
    html: '<title>Vouchers 3.9.0</title><p>Latest premium release 3.9.0.</p>',
  });

  assert.deepEqual(result.requests, [registration.url]);
  assert.equal(result.outcome.compatibility.compatible, null);
  assert.match(result.outcome.vendor_metadata.auth_boundary, /Official token flow is required/);
  assert.ok(result.report.plugins[0].vendor_metadata.risk_notes.includes('voxel-shop-token-auth-required'));
  assert.equal(result.report.plugins[0].candidate, null);
});

test('vendor authority registration prevents misleading Modrinth exact-name candidate', async () => {
  const registration = {
    name: 'MythicDungeons',
    pluginId: 'mythicdungeons',
    adapter: 'mythiccraft-resource',
    url: 'https://mythiccraft.io/index.php?resources/mythic-dungeons.869/',
  };
  const result = await discoverVendorFixture({
    plugin: { name: 'MythicDungeons', currentVersion: '2.0.1-SNAPSHOT' },
    registration,
    html: '<title>Mythic Dungeons 2.1.0</title><p>Supports Paper 1.21.11.</p>',
  });

  assert.deepEqual(result.requests, [registration.url]);
  assert.equal(result.report.plugins[0].status, 'premium-or-login');
  assert.equal(result.report.plugins[0].candidate, null);
});
