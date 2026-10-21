import test from 'node:test';
import assert from 'node:assert/strict';
import { discoverVendorMetadata } from '../src/batch/vendor-adapters.mjs';

test('Voxel batch adapter lists Nexo resource 6901 versions from official updates API', async () => {
  const outcome = await discoverVendorMetadata({
    entry: {
      manifest: {
        name: 'nexo',
        current_version: '1.20.0',
        license_status: 'unknown',
      },
    },
    registration: {
      manifest_name: 'nexo',
      plugin_id: 'nexo',
      kind: 'premium-or-login',
      adapter: 'voxel-shop-updates',
      resource_id: 6901,
      official_url: 'https://voxel.shop/product/6901/nexo/updates',
      reason: 'Voxel account-gated product updates page is authoritative.',
    },
    minecraftVersion: '1.21.11',
    channel: 'stable',
    voxelClient: {
      getResourceUpdates: async ({ resource_id }) => {
        assert.equal(resource_id, 6901);
        return {
          response: {
            success: true,
            resource: { id: '6901' },
          updates: [
            {
              id: 'nexo-1.26',
              version: '1.26',
              title: 'Nexo 1.26',
              description: 'Supports Paper 1.26.',
              game_versions: ['1.26'],
              time: '1781908716',
              url: 'https://voxel.shop/product/6901/nexo/update/nexo-1.26',
              snapshot: '0',
              beta: '0',
            },
            {
              id: 'nexo-1.25',
              version: '1.25',
              title: 'Nexo 1.25',
              description: 'Release with exact Paper support.',
              game_versions: ['1.21.11'],
              time: '1781808716',
              url: 'https://voxel.shop/product/6901/nexo/update/nexo-1.25',
              snapshot: '0',
              beta: '0',
            },
            {
              id: 'nexo-1.24.0',
              version: '1.24.0',
              title: 'Nexo 1.24.0',
              description: 'Release with exact Paper support.',
              game_versions: ['1.21.11'],
              time: '1781708716',
              url: 'https://voxel.shop/product/6901/nexo/update/nexo-1.24.0',
              snapshot: '0',
              beta: '0',
            },
          ],
          },
        };
      },
    },
  });

  assert.equal(outcome.kind, 'vendor-metadata');
  assert.equal(outcome.vendor_metadata.resource_id, 6901);
  assert.equal(outcome.vendor_metadata.api_url, 'https://api.voxel.shop/v1/getResourceUpdates');
  const byVersion = new Map(outcome.available_versions.map((version) => [version.version, version]));
  assert.equal(byVersion.get('1.24.0').compatible, true);
  assert.equal(byVersion.get('1.24.0').compatibility.class, 'exact-listed');
  assert.match(byVersion.get('1.24.0').compatibility.reason, /1\.21\.11/);
  assert.equal(byVersion.get('1.25').compatible, true);
  assert.equal(byVersion.get('1.25').compatibility.class, 'exact-listed');
  assert.equal(byVersion.get('1.25').compatibility.source, 'voxel-getResourceUpdates');
  assert.deepEqual(byVersion.get('1.25').compatibility.matched_versions, ['1.21.11']);
  assert.equal(byVersion.get('1.26').compatible, false);
  assert.ok(['negative-listed', 'unknown'].includes(byVersion.get('1.26').compatibility.class));
  assert.match(byVersion.get('1.26').compatibility.reason, /1\.21\.11/);
  assert.equal(outcome.compatibility.compatible, true);
  assert.ok(outcome.vendor_metadata.changelog_urls.includes('https://voxel.shop/product/6901/nexo/update/nexo-1.24.0'));
});

test('Voxel batch adapter reports auth_required when official updates API is gated', async () => {
  const outcome = await discoverVendorMetadata({
    entry: {
      manifest: {
        name: 'nexo',
        current_version: '1.20.0',
        license_status: 'unknown',
      },
    },
    registration: {
      manifest_name: 'nexo',
      plugin_id: 'nexo',
      kind: 'premium-or-login',
      adapter: 'voxel-shop-updates',
      resource_id: 6901,
      official_url: 'https://voxel.shop/product/6901/nexo/updates',
      reason: 'Voxel account-gated product updates page is authoritative.',
    },
    minecraftVersion: '1.21.11',
    channel: 'stable',
    voxelClient: {
      getResourceUpdates: async () => ({
        response: { success: false, message: 'AUTH_REQUIRED' },
      }),
    },
  });

  assert.equal(outcome.vendor_metadata.fetched, false);
  assert.deepEqual(outcome.rejections, [{
    stage: 'vendor-metadata',
    project_id: '6901',
    reason: 'auth_required',
  }]);
  assert.equal(outcome.available_versions[0].version, '1.20.0');
});
