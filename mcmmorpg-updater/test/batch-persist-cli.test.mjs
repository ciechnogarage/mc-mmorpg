import test from 'node:test';
import assert from 'node:assert/strict';
import { main, parsePersistArguments, runPersistCli } from '../src/batch/persist-cli.mjs';

const REPORT = `reports/batch-discovery-${'a'.repeat(64)}.report.json`;

test('persist CLI keeps stable default and accepts explicit channel', async () => {
  assert.deepEqual(parsePersistArguments([
    '--report', REPORT,
    '--operator', 'alice',
    '--confirm-download-all',
  ]), { report: REPORT, operator: 'alice', channel: 'stable' });

  assert.deepEqual(parsePersistArguments([
    '--report', REPORT,
    '--operator', 'alice',
    '--confirm-download-all',
    '--channel', 'snapshot',
  ]), { report: REPORT, operator: 'alice', channel: 'snapshot' });

  for (const argv of [
    [],
    ['--report', REPORT, '--operator', 'alice'],
    ['--report', 'reports/not-content-addressed.json', '--operator', 'alice', '--confirm-download-all'],
    ['--report', REPORT, '--operator', ' alice', '--confirm-download-all'],
    ['--report', REPORT, '--operator', 'alice', '--confirm-download-all', '--channel', 'release'],
    ['--report', REPORT, '--operator', 'alice', '--confirm-download-all', '--shell', 'rm'],
  ]) {
    assert.throws(
      () => parsePersistArguments(argv),
      (error) => error.code === 'batch-persist-cli-arguments-invalid' && error.exitCode === 2,
    );
  }

  let received;
  const output = await runPersistCli([
    '--report', REPORT,
    '--operator', 'alice',
    '--confirm-download-all',
    '--channel', 'snapshot',
  ], {
    projectRoot: '/workspace/updater',
    runImpl: async (input) => {
      received = input;
      return {
        schema_version: 1,
        summary: { selected: 1, persisted: 1, already_present: 0, failed: 0 },
        batch_report_path: 'reports/batch-download-a.report.json',
        results: [{
          plugin_name: 'Example',
          status: 'persisted',
          candidate: { path: 'downloads/example/a.jar', sha256: 'a'.repeat(64) },
          report_path: 'reports/batch-download-plugin-a.report.json',
          report_reused: false,
          error: null,
        }],
      };
    },
  });

  assert.equal(received.reportPath, `/workspace/updater/${REPORT}`);
  assert.equal(received.confirmDownloadAll, true);
  assert.equal(received.operator, 'alice');
  assert.equal(received.channel, 'snapshot');
  assert.equal(output.channel, 'snapshot');
  assert.deepEqual(output.results[0], {
    plugin_name: 'Example',
    status: 'persisted',
    path: 'downloads/example/a.jar',
    sha256: 'a'.repeat(64),
    report_path: 'reports/batch-download-plugin-a.report.json',
    report_reused: false,
    error: null,
  });
});

test('persist CLI main emits one JSON record and maps argument errors to exit code 2', async () => {
  const stdout = [];
  const stderr = [];
  assert.equal(await main({
    argv: ['--report', REPORT, '--operator', 'alice', '--confirm-download-all'],
    stdout: { write: (value) => stdout.push(value) },
    stderr: { write: (value) => stderr.push(value) },
    runImpl: async () => ({ schema_version: 1, channel: 'stable', summary: { selected: 0 }, results: [] }),
  }), 0);
  assert.equal(JSON.parse(stdout[0]).channel, 'stable');

  stdout.length = 0;
  stderr.length = 0;
  assert.equal(await main({
    argv: ['--report', REPORT, '--operator', 'alice'],
    stdout: { write: (value) => stdout.push(value) },
    stderr: { write: (value) => stderr.push(value) },
  }), 2);
  assert.equal(JSON.parse(stderr[0]).error.code, 'batch-persist-cli-arguments-invalid');
});
