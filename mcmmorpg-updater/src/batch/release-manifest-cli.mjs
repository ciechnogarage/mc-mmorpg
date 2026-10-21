import { resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { createOperatorReleaseManifest } from './release-manifest.mjs';

const projectRoot = fileURLToPath(new URL('../../', import.meta.url));
const AUDIT = /^reports\/batch-download-[0-9a-f]{64}\.report\.json$/;

export class ReleaseManifestCliError extends Error {
  constructor(code, message, { exitCode = 1, cause } = {}) {
    super(message, { cause });
    this.name = 'ReleaseManifestCliError';
    this.code = code;
    this.exitCode = exitCode;
  }
}

export function parseReleaseManifestArguments(argv) {
  if (!Array.isArray(argv) || argv.length !== 2 || argv[0] !== '--batch-audit' || !AUDIT.test(argv[1] ?? '')) {
    throw new ReleaseManifestCliError(
      'batch-release-cli-arguments-invalid',
      'usage: npm run release-manifest -- --batch-audit reports/batch-download-<sha256>.report.json',
      { exitCode: 2 },
    );
  }
  return { batchAudit: argv[1] };
}

export async function runReleaseManifestCli(argv, {
  projectRoot: root,
  runImpl = createOperatorReleaseManifest,
} = {}) {
  const input = parseReleaseManifestArguments(argv);
  const result = await runImpl({ projectRoot: root, batchAuditPath: resolve(root, input.batchAudit) });
  return {
    schema_version: 1,
    summary: result.manifest.summary,
    json_path: result.json_path,
    markdown_path: result.markdown_path,
    json_reused: result.json_reused,
    markdown_reused: result.markdown_reused,
  };
}

export async function main({
  argv = process.argv.slice(2),
  stdout = process.stdout,
  stderr = process.stderr,
  runImpl = runReleaseManifestCli,
} = {}) {
  try {
    const result = await runImpl(argv, { projectRoot });
    stdout.write(`${JSON.stringify(result)}\n`);
    return 0;
  } catch (error) {
    stderr.write(`${JSON.stringify({
      ok: false,
      error: { code: error?.code ?? 'batch-release-cli-failed', message: error?.message ?? 'release manifest failed' },
    })}\n`);
    return Number.isInteger(error?.exitCode) ? error.exitCode : 1;
  }
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  process.exitCode = await main();
}
