import test from 'node:test';
import assert from 'node:assert/strict';
import { main, parseBatchArguments, runBatchDiscoveryCli } from '../src/batch/cli.mjs';

test('batch CLI reports default stable channel and accepts explicit snapshot', async () => {
  assert.deepEqual(parseBatchArguments(['--operator', 'alice']), { operator: 'alice', channel: 'stable' });
  assert.deepEqual(parseBatchArguments(['--operator', 'alice', '--channel', 'snapshot']), { operator: 'alice', channel: 'snapshot' });

  for (const argv of [[], ['--operator'], ['--operator', ' alice'], ['--url', 'https://mirror.example.test/plugin.jar'], ['--operator', 'alice', '--channel', 'release']]) {
    assert.throws(
      () => parseBatchArguments(argv),
      (error) => error.code === 'batch-cli-arguments-invalid' && error.exitCode === 2,
    );
  }

  let received;
  const result = await runBatchDiscoveryCli(['--operator', 'alice', '--channel', 'snapshot'], {
    projectRoot: '/workspace/updater',
    runImpl: async (input) => {
      received = input;
      return {
        schema_version: 1,
        report_path: 'reports/batch-discovery-abc.report.json',
        report: {
          coverage: { manifest_plugins: 35, reported_plugins: 35, complete: true },
          summary: {
            status_counts: { qualified: 0, 'source-not-found': 33 },
            candidate_counts: { found: 1, qualified: 0, manual_hash_approval_required: 1, incompatible: 0 },
          },
        },
      };
    },
  });

  assert.equal(received.operator, 'alice');
  assert.equal(received.channel, 'snapshot');
  assert.equal(received.manifestPath, '/workspace/docs/plugin_manifest.yaml');
  assert.equal(received.registrationsPath, '/workspace/updater/config/batch-sources.json');
  assert.deepEqual(result, {
    schema_version: 1,
    channel: 'snapshot',
    report_path: 'reports/batch-discovery-abc.report.json',
    coverage: { manifest_plugins: 35, reported_plugins: 35, complete: true },
    status_counts: { qualified: 0, 'source-not-found': 33 },
    candidate_counts: { found: 1, qualified: 0, manual_hash_approval_required: 1, incompatible: 0 },
  });
});

test('batch CLI entrypoint emits one JSON record and maps argument errors to exit code 2', async () => {
  const stdout = [];
  const stderr = [];
  assert.equal(await main({
    argv: ['--operator', 'alice'],
    stdout: { write: (value) => stdout.push(value) },
    stderr: { write: (value) => stderr.push(value) },
    runImpl: async () => ({ schema_version: 1, channel: 'stable' }),
  }), 0);
  assert.equal(JSON.parse(stdout[0]).channel, 'stable');

  stdout.length = 0;
  stderr.length = 0;
  assert.equal(await main({
    argv: ['--channel', 'stable'],
    stdout: { write: (value) => stdout.push(value) },
    stderr: { write: (value) => stderr.push(value) },
  }), 2);
  assert.equal(JSON.parse(stderr[0]).error.code, 'batch-cli-arguments-invalid');
});
