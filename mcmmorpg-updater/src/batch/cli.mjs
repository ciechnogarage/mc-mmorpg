import { lookup } from 'node:dns/promises';
import { resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { createHttpsTransport } from '../downloads/transport.mjs';
import { normalizeReleaseChannel } from './channel.mjs';
import { runBatchDiscovery } from './runner.mjs';

const projectRoot = fileURLToPath(new URL('../../', import.meta.url));

export class BatchCliError extends Error {
  constructor(code, message, { exitCode = 1, cause } = {}) {
    super(message, { cause });
    this.name = 'BatchCliError';
    this.code = code;
    this.exitCode = exitCode;
  }
}

export function parseBatchArguments(argv) {
  if (!Array.isArray(argv)) throw invalidArguments();

  let operator = null;
  let channel = 'stable';
  let channelSeen = false;

  for (let index = 0; index < argv.length; index += 2) {
    const key = argv[index];
    const value = argv[index + 1];
    if (key === '--operator' && typeof value === 'string' && value.trim() === value && value.length > 0 && operator === null) {
      operator = value;
    } else if (key === '--channel' && typeof value === 'string' && channelSeen === false) {
      try {
        channel = normalizeReleaseChannel(value);
      } catch {
        throw invalidArguments();
      }
      channelSeen = true;
    } else {
      throw invalidArguments();
    }
  }

  if (operator === null) throw invalidArguments();
  return { operator, channel };
}

function invalidArguments() {
  return new BatchCliError(
    'batch-cli-arguments-invalid',
    'usage: npm run discover-candidates -- --operator <name> [--channel stable|snapshot]; source, URL, target, approval, download overrides forbidden',
    { exitCode: 2 },
  );
}

export async function runBatchDiscoveryCli(argv, {
  projectRoot: root,
  lookupImpl = lookup,
  fetchImpl = createHttpsTransport(),
  runImpl = runBatchDiscovery,
} = {}) {
  const input = parseBatchArguments(argv);
  const result = await runImpl({
    projectRoot: root,
    manifestPath: resolve(root, '..', 'docs', 'plugin_manifest.yaml'),
    targetsPath: resolve(root, 'registry', 'targets.json'),
    pluginsPath: resolve(root, 'registry', 'plugins.json'),
    registrationsPath: resolve(root, 'config', 'batch-sources.json'),
    policyPath: resolve(root, 'config', 'trusted-sources.json'),
    operator: input.operator,
    channel: input.channel,
    lookupImpl,
    fetchImpl,
  });
  return {
    schema_version: 1,
    channel: input.channel,
    report_path: result.report_path,
    coverage: result.report.coverage,
    status_counts: result.report.summary.status_counts,
    candidate_counts: result.report.summary.candidate_counts,
  };
}

export async function main({
  argv = process.argv.slice(2),
  stdout = process.stdout,
  stderr = process.stderr,
  runImpl = runBatchDiscoveryCli,
} = {}) {
  try {
    const result = await runImpl(argv, { projectRoot });
    stdout.write(`${JSON.stringify(result)}\n`);
    return 0;
  } catch (error) {
    stderr.write(`${JSON.stringify({
      ok: false,
      error: {
        code: error?.code ?? 'batch-cli-failed',
        message: error?.message ?? 'batch discovery failed',
      },
    })}\n`);
    return Number.isInteger(error?.exitCode) ? error.exitCode : 1;
  }
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  process.exitCode = await main();
}
