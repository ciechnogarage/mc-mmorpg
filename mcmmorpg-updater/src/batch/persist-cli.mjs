import { lookup } from 'node:dns/promises';
import { resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { createHttpsTransport } from '../downloads/transport.mjs';
import { normalizeReleaseChannel } from './channel.mjs';
import { persistVerifiedModrinthBatch } from './persist.mjs';

const projectRoot = fileURLToPath(new URL('../../', import.meta.url));
const REPORT = /^reports\/batch-discovery-[0-9a-f]{64}\.report\.json$/;

export class BatchPersistCliError extends Error {
  constructor(code, message, { exitCode = 1, cause } = {}) {
    super(message, { cause });
    this.name = 'BatchPersistCliError';
    this.code = code;
    this.exitCode = exitCode;
  }
}

export function parsePersistArguments(argv) {
  if (!Array.isArray(argv)) throw invalidArguments();
  if (argv.length !== 5 && argv.length !== 7) throw invalidArguments();
  if (argv[0] !== '--report' || !REPORT.test(argv[1] ?? '')) throw invalidArguments();
  if (argv[2] !== '--operator' || typeof argv[3] !== 'string' || argv[3].trim() !== argv[3] || argv[3].length === 0) throw invalidArguments();
  if (argv[4] !== '--confirm-download-all') throw invalidArguments();

  let channel = 'stable';
  if (argv.length === 7) {
    if (argv[5] !== '--channel' || typeof argv[6] !== 'string') throw invalidArguments();
    try {
      channel = normalizeReleaseChannel(argv[6]);
    } catch {
      throw invalidArguments();
    }
  }

  return { report: argv[1], operator: argv[3], channel };
}

function invalidArguments() {
  return new BatchPersistCliError(
    'batch-persist-cli-arguments-invalid',
    'usage: npm run persist-verified-batch -- --report reports/batch-discovery-<sha256>.report.json --operator <name> --confirm-download-all [--channel stable|snapshot]',
    { exitCode: 2 },
  );
}

export async function runPersistCli(argv, {
  projectRoot: root,
  lookupImpl = lookup,
  fetchImpl = createHttpsTransport(),
  runImpl = persistVerifiedModrinthBatch,
} = {}) {
  const input = parsePersistArguments(argv);
  const result = await runImpl({
    projectRoot: root,
    reportPath: resolve(root, input.report),
    manifestPath: resolve(root, '..', 'docs', 'plugin_manifest.yaml'),
    targetsPath: resolve(root, 'registry', 'targets.json'),
    pluginsPath: resolve(root, 'registry', 'plugins.json'),
    registrationsPath: resolve(root, 'config', 'batch-sources.json'),
    policyPath: resolve(root, 'config', 'trusted-sources.json'),
    operator: input.operator,
    channel: input.channel,
    confirmDownloadAll: true,
    lookupImpl,
    fetchImpl,
  });
  return {
    schema_version: 1,
    channel: input.channel,
    source_report_sha256: result.source_report_sha256,
    summary: result.summary,
    batch_report_path: result.batch_report_path,
    batch_report_reused: result.batch_report_reused,
    release_manifest_json_path: result.release_manifest_json_path,
    release_manifest_markdown_path: result.release_manifest_markdown_path,
    release_manifest_json_reused: result.release_manifest_json_reused,
    release_manifest_markdown_reused: result.release_manifest_markdown_reused,
    results: result.results.map((entry) => ({
      plugin_name: entry.plugin_name,
      status: entry.status,
      path: entry.candidate?.path ?? null,
      sha256: entry.candidate?.sha256 ?? null,
      report_path: entry.report_path ?? null,
      report_reused: entry.report_reused ?? false,
      error: entry.error,
    })),
  };
}

export async function main({
  argv = process.argv.slice(2),
  stdout = process.stdout,
  stderr = process.stderr,
  runImpl = runPersistCli,
} = {}) {
  try {
    const result = await runImpl(argv, { projectRoot });
    stdout.write(`${JSON.stringify(result)}\n`);
    return 0;
  } catch (error) {
    stderr.write(`${JSON.stringify({
      ok: false,
      error: {
        code: error?.code ?? 'batch-persist-cli-failed',
        message: error?.message ?? 'verified batch persist failed',
      },
    })}\n`);
    return Number.isInteger(error?.exitCode) ? error.exitCode : 1;
  }
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  process.exitCode = await main();
}
