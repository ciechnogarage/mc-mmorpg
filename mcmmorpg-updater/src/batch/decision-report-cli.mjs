import { resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { createOperatorDecisionReport } from './decision-report.mjs';

const projectRoot = fileURLToPath(new URL('../../', import.meta.url));
const REPORT = /^reports\/batch-discovery-[0-9a-f]{64}\.report\.json$/;

export class DecisionReportCliError extends Error {
  constructor(code, message, { exitCode = 1, cause } = {}) {
    super(message, { cause });
    this.name = 'DecisionReportCliError';
    this.code = code;
    this.exitCode = exitCode;
  }
}

export function parseDecisionReportArguments(argv) {
  if (!Array.isArray(argv) || argv.length !== 6
    || argv[0] !== '--report' || !REPORT.test(argv[1] ?? '')
    || argv[2] !== '--channel' || !['stable', 'snapshot'].includes(argv[3])
    || argv[4] !== '--operator' || typeof argv[5] !== 'string' || argv[5].trim() !== argv[5] || argv[5].length === 0) {
    throw new DecisionReportCliError(
      'batch-decision-cli-arguments-invalid',
      'usage: npm run operator-decision-report -- --report reports/batch-discovery-<sha256>.report.json --channel stable|snapshot --operator <name>',
      { exitCode: 2 },
    );
  }
  return { report: argv[1], channel: argv[3], operator: argv[5] };
}

export async function runDecisionReportCli(argv, { projectRoot: root, runImpl = createOperatorDecisionReport } = {}) {
  const input = parseDecisionReportArguments(argv);
  return runImpl({
    projectRoot: root,
    discoveryReportPath: input.report,
    channel: input.channel,
    operator: input.operator,
  });
}

export async function main({ argv = process.argv.slice(2), stdout = process.stdout, stderr = process.stderr, runImpl = runDecisionReportCli } = {}) {
  try {
    const result = await runImpl(argv, { projectRoot });
    stdout.write(`${JSON.stringify(result)}\n`);
    return 0;
  } catch (error) {
    stderr.write(`${JSON.stringify({
      ok: false,
      error: {
        code: error?.code ?? 'batch-decision-cli-failed',
        message: error?.message ?? 'operator decision report failed',
      },
    })}\n`);
    return Number.isInteger(error?.exitCode) ? error.exitCode : 1;
  }
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  process.exitCode = await main();
}
