import assert from 'node:assert/strict';
import test from 'node:test';

import {
  analyzeAgentLogs,
  analyzeAgentTranscript,
  renderMarkdownComparison,
  renderMarkdownReport,
} from '../scripts/agent-audit.mjs';

test('extracts token usage and prompt compliance from transcript', () => {
  const transcript = `
OpenAI Codex
user
# BRIEF worker
ONLY files \`plugins/Iris/packs/level_1/**\`
DO NOT touch anything under \`plugins/MythicMobs/\`
Write \`_coord/results/codex1.md\`

hook: SessionStart
exec /bin/bash -lc 'sed -n "1,120p" plugins/Iris/packs/level_1/README.md' in /repo
exec /bin/bash -lc 'git diff -- plugins/Iris/packs/level_1/dimensions/level_1.json' in /repo
- \`plugins/Iris/packs/level_1/dimensions/level_1.json\`
Implemented Iris-side terrain pass and wrote handoff note in \`_coord/results/codex1.md\`.
Could not verify actual runtime terrain without headless backend.
4971 tokens used
`;

  const report = analyzeAgentTranscript(transcript);
  assert.equal(report.metrics.tokenUsage.total, 4971);
  assert.equal(report.metrics.promptCompliance.score, 100);
  assert.equal(report.metrics.completion.percent >= 80, true);
  assert.equal(report.scores.hallucinationRisk.score >= 80, true);
});

test('extracts traycer codex exec commands split across lines', () => {
  const transcript = `
user
# BRIEF worker
Write \`_coord/results/codex1.md\`

hook: SessionStart
codex I’m reading current files first.
exec
/bin/bash -lc 'rtk read plugins/Iris/packs/level_1/README.md'
in /home/przemek/projects/MC/MCMMORPG
 succeeded in 0ms:
README
exec
/bin/bash -lc "apply_patch <<'PATCH'
*** Begin Patch
*** End Patch
PATCH"
in /home/przemek/projects/MC
 succeeded in 0ms:
done
`;

  const report = analyzeAgentTranscript(transcript);
  assert.equal(report.metrics.commands.total, 2);
  assert.equal(report.metrics.commands.readCommands, 1);
  assert.equal(report.metrics.commands.writeCommands, 1);
});

test('flags forbidden scope and unsupported completion claims', () => {
  const transcript = `
user
# BRIEF worker
ONLY files \`plugins/Iris/packs/level_1/**\`
DO NOT touch anything under \`plugins/MythicMobs/\`

hook: SessionStart
exec /bin/bash -lc 'sed -n "1,120p" plugins/MythicMobs/mobs/level1_grove.yml' in /repo
Done. Fixed everything. Completed verified.
`;

  const report = analyzeAgentTranscript(transcript);
  assert.equal(report.metrics.promptCompliance.score < 100, true);
  assert.equal(report.scores.hallucinationRisk.score < 90, true);
  assert.equal(report.metrics.realVerification.count, 0);
  assert.equal(report.findings.some((finding) => finding.title.includes('forbidden scope')), true);
});

test('renders markdown summary for human handoff', () => {
  const transcript = `
user
# BRIEF worker
Write \`_coord/results/codex3.md\`

hook: SessionStart
- \`_coord/results/codex3.md\`
tested on isolated backend
1200 tokens used
`;

  const report = analyzeAgentTranscript(transcript);
  const rendered = renderMarkdownReport(report, 'codex3.log');
  assert.equal(rendered.includes('# Agent Audit: codex3.log'), true);
  assert.equal(rendered.includes('tokens 1200'), true);
});

test('builds comparison bundle and rankings for multiple logs', () => {
  const lowCost = `
user
# BRIEF worker
Write \`_coord/results/a.md\`

hook: SessionStart
exec /bin/bash -lc 'rtk read plugins/Iris/packs/level_1/README.md' in /repo
- \`_coord/results/a.md\`
tested on isolated backend
200 tokens used
`;

  const highStyle = `
user
# BRIEF worker
Write \`_coord/results/b.md\`

hook: SessionStart
exec /bin/bash -lc 'rtk read plugins/MythicMobs/mobs/level1_grove.yml' in /repo
exec /bin/bash -lc "apply_patch <<'PATCH'
*** Begin Patch
*** End Patch
PATCH" in /repo
exec /bin/bash -lc 'node --test tests/agent-audit.test.mjs' in /repo
- \`_coord/results/b.md\`
Could not verify live backend, but tested parser locally.
400 tokens used
`;

  const bundle = analyzeAgentLogs([
    { input: 'a.log', text: lowCost },
    { input: 'b.log', text: highStyle },
  ]);

  assert.equal(bundle.reports.length, 2);
  assert.equal(bundle.auditSummary.length, 2);
  assert.equal(bundle.comparison.comparedLogs, 2);
  assert.equal(bundle.comparison.ranking.lowestTokenCost[0].label, 'a.log');
  assert.equal(bundle.comparison.ranking.highestStyleScore[0].label, 'b.log');
  assert.equal(bundle.comparison.ranking.highestCompletionPerToken[0].label, 'a.log');
  assert.equal(bundle.comparison.ranking.bestVerifiedAgent[0].label, 'b.log');
  assert.equal(bundle.comparison.ranking.mostRiskyCompletion[0].label, 'a.log');

  const rendered = renderMarkdownComparison(bundle);
  assert.equal(rendered.includes('# Agent Audit Comparison'), true);
  assert.equal(rendered.includes('## Audit Summary'), true);
  assert.equal(rendered.includes('## Verdict Legend'), true);
  assert.equal(rendered.includes('## Verification Evidence') === false, true);
  assert.equal(rendered.includes('### Best Verified Agent'), true);
});

test('applies explicit style penalties for excessive reads, low writes, and no verification', () => {
  const transcript = `
user
# BRIEF worker
Write \`_coord/results/c.md\`

hook: SessionStart
exec /bin/bash -lc 'rtk read a' in /repo
exec /bin/bash -lc 'rtk read b' in /repo
exec /bin/bash -lc 'rtk read c' in /repo
exec /bin/bash -lc 'rtk read d' in /repo
exec /bin/bash -lc 'rtk read e' in /repo
exec /bin/bash -lc 'rtk read f' in /repo
exec /bin/bash -lc 'rtk read g' in /repo
exec /bin/bash -lc 'rtk read h' in /repo
exec /bin/bash -lc 'rtk read i' in /repo
exec /bin/bash -lc 'rtk read j' in /repo
exec /bin/bash -lc 'rtk read k' in /repo
exec /bin/bash -lc 'rtk read l' in /repo
- \`_coord/results/c.md\`
300 tokens used
`;

  const bundle = analyzeAgentLogs([
    { input: 'x.log', text: transcript },
    { input: 'y.log', text: `${transcript}\nexec /bin/bash -lc "apply_patch <<'PATCH'\n*** Begin Patch\n*** End Patch\nPATCH" in /repo\ntested locally\n` },
  ]);
  const penalized = bundle.comparison.profiles.find((entry) => entry.label === 'x.log');
  const report = bundle.reports[0].report;

  assert.equal(report.metrics.commands.readCommands >= 12, true);
  assert.equal(penalized.styleScoreComponents.penalties.excessiveReads > 0, true);
  assert.equal(penalized.styleScoreComponents.penalties.lowWrites > 0, true);
  assert.equal(penalized.styleScoreComponents.penalties.noRealVerification > 0, true);
});

test('does not count narration-only verification as real verification', () => {
  const transcript = `
user
# BRIEF worker
Write \`_coord/results/narrative.md\`

hook: SessionStart
exec /bin/bash -lc 'rtk read README.md' in /repo
Verified locally. Validation looks good. Checked mentally.
120 tokens used
`;

  const report = analyzeAgentTranscript(transcript);
  assert.equal(report.metrics.realVerification.count, 0);
  assert.equal(report.scores.solutionSense.hasVerification, false);
});

test('recognizes repo-specific verification commands', () => {
  const transcript = `
user
# BRIEF worker
Write \`_coord/results/runtime.md\`

hook: SessionStart
exec /bin/bash -lc 'tail -n 200 MCMMORPG/logs/latest.log' in /repo
exec /bin/bash -lc 'node MCMMORPG/_validation/check_modelengine_binding.js' in /repo
exec /bin/bash -lc 'mcrcon players' in /repo
exec /bin/bash -lc 'mineflayer-validate level_1_flow' in /repo
250 tokens used
`;

  const report = analyzeAgentTranscript(transcript);
  assert.equal(report.metrics.realVerification.count >= 4, true);
  assert.equal(report.realVerificationEvidence.length >= 4, true);
  assert.equal(report.metrics.realVerification.totalWeight >= 15, true);
  assert.equal(report.realVerificationEvidence.some((entry) => entry.type === 'runtime-check' && entry.weight === 5), true);
  assert.equal(report.metrics.commands.commandBreakdown.byCategory.verify >= 4, true);
  assert.equal(report.scores.solutionSense.hasVerification, true);
  assert.equal(report.metrics.verificationQuality, 'runtime-verified');
});

test('renders explicit verification evidence in markdown report', () => {
  const transcript = `
user
# BRIEF worker
Write \`_coord/results/runtime.md\`

hook: SessionStart
exec /bin/bash -lc 'tail -n 200 MCMMORPG/logs/latest.log' in /repo
220 tokens used
`;

  const report = analyzeAgentTranscript(transcript);
  const rendered = renderMarkdownReport(report, 'runtime.log');
  assert.equal(rendered.includes('## Verification Evidence'), true);
  assert.equal(rendered.includes('[log-check w=2] tail -n 200 MCMMORPG/logs/latest.log'), true);
});

test('classifies verification quality thresholds', () => {
  const unverified = analyzeAgentTranscript(`user\n# BRIEF worker\nhook: SessionStart\nexec /bin/bash -lc 'rtk read README.md' in /repo\n`);
  const logVerified = analyzeAgentTranscript(`user\n# BRIEF worker\nhook: SessionStart\nexec /bin/bash -lc 'tail -n 200 MCMMORPG/logs/latest.log' in /repo\n`);
  const scriptVerified = analyzeAgentTranscript(`user\n# BRIEF worker\nhook: SessionStart\nexec /bin/bash -lc 'node MCMMORPG/_validation/check_modelengine_binding.js' in /repo\n`);
  const runtimeVerified = analyzeAgentTranscript(`user\n# BRIEF worker\nhook: SessionStart\nexec /bin/bash -lc 'mcrcon players' in /repo\n`);

  assert.equal(unverified.metrics.verificationQuality, 'unverified');
  assert.equal(logVerified.metrics.verificationQuality, 'log-verified');
  assert.equal(scriptVerified.metrics.verificationQuality, 'script-verified');
  assert.equal(runtimeVerified.metrics.verificationQuality, 'runtime-verified');
});

test('adds P1 finding for high completion without hard verification', () => {
  const transcript = `
user
# BRIEF worker
Write \`_coord/results/high.md\`

hook: SessionStart
- \`_coord/results/high.md\`
- \`plugins/Iris/packs/level_1/README.md\`
Implemented complete handoff.
Could not verify runtime behavior headless.
450 tokens used
`;

  const report = analyzeAgentTranscript(transcript);
  assert.equal(report.metrics.completion.percent >= 75, true);
  assert.equal(report.metrics.verificationQuality, 'unverified');
  assert.equal(report.findings.some((finding) => finding.severity === 'P1' && finding.title.includes('without hard verification')), true);
});

test('ranks verified agents by quality then weight', () => {
  const bundle = analyzeAgentLogs([
    { input: 'u.log', text: `user\n# BRIEF worker\nhook: SessionStart\nexec /bin/bash -lc 'rtk read README.md' in /repo\n` },
    { input: 'l.log', text: `user\n# BRIEF worker\nhook: SessionStart\nexec /bin/bash -lc 'tail -n 200 MCMMORPG/logs/latest.log' in /repo\n` },
    { input: 's.log', text: `user\n# BRIEF worker\nhook: SessionStart\nexec /bin/bash -lc 'node MCMMORPG/_validation/check_modelengine_binding.js' in /repo\n` },
    { input: 'r.log', text: `user\n# BRIEF worker\nhook: SessionStart\nexec /bin/bash -lc 'mcrcon players' in /repo\n` },
  ]);

  const ranking = bundle.comparison.ranking.bestVerifiedAgent.map((entry) => entry.label);
  assert.deepEqual(ranking, ['r.log', 's.log', 'l.log', 'u.log']);
});

test('best verified agent breaks ties by lower findings severity', () => {
  const bundle = analyzeAgentLogs([
    {
      input: 'clean.log',
      text: `user\n# BRIEF worker\nWrite \`_coord/results/clean.md\`\n\nhook: SessionStart\nexec /bin/bash -lc 'node MCMMORPG/_validation/check_modelengine_binding.js' in /repo\n- \`_coord/results/clean.md\`\n250 tokens used\n`,
    },
    {
      input: 'noisy.log',
      text: `user\n# BRIEF worker\nWrite \`_coord/results/noisy.md\`\nDO NOT touch anything under \`plugins/MythicMobs/\`\n\nhook: SessionStart\nexec /bin/bash -lc 'node MCMMORPG/_validation/check_modelengine_binding.js' in /repo\nexec /bin/bash -lc 'sed -n "1,20p" plugins/MythicMobs/mobs/level1_grove.yml' in /repo\n- \`_coord/results/noisy.md\`\n500 tokens used\n`,
    },
  ]);

  const ranking = bundle.comparison.ranking.bestVerifiedAgent;
  assert.equal(ranking[0].label, 'clean.log');
  assert.equal(ranking[1].label, 'noisy.log');
});

test('ranks risky completion by unverified high-completion first', () => {
  const bundle = analyzeAgentLogs([
    {
      input: 'safe.log',
      text: `user\n# BRIEF worker\nWrite \`_coord/results/safe.md\`\n\nhook: SessionStart\nexec /bin/bash -lc 'mcrcon players' in /repo\n- \`_coord/results/safe.md\`\n420 tokens used\n`,
    },
    {
      input: 'risky-high.log',
      text: `user\n# BRIEF worker\nWrite \`_coord/results/risky.md\`\n\nhook: SessionStart\n- \`_coord/results/risky.md\`\nImplemented full handoff.\nCould not verify runtime headless.\n300 tokens used\n`,
    },
    {
      input: 'risky-lower.log',
      text: `user\n# BRIEF worker\nWrite \`_coord/results/risky2.md\`\n\nhook: SessionStart\n- \`_coord/results/risky2.md\`\nImplemented partial handoff.\n260 tokens used\n`,
    },
  ]);

  const ranking = bundle.comparison.ranking.mostRiskyCompletion.map((entry) => entry.label);
  assert.deepEqual(ranking, ['risky-high.log', 'risky-lower.log', 'safe.log']);
});

test('builds top-level auditSummary verdicts per agent', () => {
  const bundle = analyzeAgentLogs([
    {
      input: 'verified.log',
      text: `user\n# BRIEF worker\nWrite \`_coord/results/verified.md\`\n\nhook: SessionStart\nexec /bin/bash -lc 'mcrcon players' in /repo\n- \`_coord/results/verified.md\`\ntested runtime\nverification boundary: runtime check complete\n300 tokens used\n`,
    },
    {
      input: 'risky.log',
      text: `user\n# BRIEF worker\nWrite \`_coord/results/risky.md\`\n\nhook: SessionStart\n- \`_coord/results/risky.md\`\nImplemented full handoff.\nCould not verify runtime headless.\n300 tokens used\n`,
    },
  ]);

  const verified = bundle.auditSummary.find((entry) => entry.label === 'verified.log');
  const risky = bundle.auditSummary.find((entry) => entry.label === 'risky.log');

  assert.equal(verified.verdict, 'strongly-verified');
  assert.equal(risky.verdict, 'risky-complete');
  assert.equal(risky.verificationQuality, 'unverified');
});

test('compares two log sets over time by label', () => {
  const current = analyzeAgentLogs([
    {
      input: 'agent-a.log',
      text: `user\n# BRIEF worker\nWrite \`_coord/results/agent-a.md\`\n\nhook: SessionStart\nexec /bin/bash -lc 'mcrcon players' in /repo\n- \`_coord/results/agent-a.md\`\nverification boundary: runtime check complete\n220 tokens used\n`,
    },
    {
      input: 'agent-b.log',
      text: `user\n# BRIEF worker\nWrite \`_coord/results/agent-b.md\`\n\nhook: SessionStart\n- \`_coord/results/agent-b.md\`\nImplemented handoff.\n340 tokens used\n`,
    },
  ], {
    compareToEntries: [
      {
        input: 'agent-a.log',
        text: `user\n# BRIEF worker\nWrite \`_coord/results/agent-a.md\`\n\nhook: SessionStart\n- \`_coord/results/agent-a.md\`\n210 tokens used\n`,
      },
      {
        input: 'agent-b.log',
        text: `user\n# BRIEF worker\nWrite \`_coord/results/agent-b.md\`\n\nhook: SessionStart\n- \`_coord/results/agent-b.md\`\nImplemented partial handoff.\n280 tokens used\n`,
      },
    ],
  });

  assert.equal(current.timeComparison.comparedAgents, 2);
  assert.equal(current.timeComparison.summary.improved >= 1, true);
  assert.equal(current.timeComparison.deltas.some((entry) => entry.label === 'agent-a.log' && entry.verificationQualityTo === 'runtime-verified'), true);
});

test('time comparison marks token growth without completion growth as regression', () => {
  const bundle = analyzeAgentLogs([
    {
      input: 'agent-x.log',
      text: `user\n# BRIEF worker\nWrite \`_coord/results/agent-x.md\`\n\nhook: SessionStart\n- \`_coord/results/agent-x.md\`\nImplemented handoff.\n500 tokens used\n`,
    },
  ], {
    compareToEntries: [
      {
        input: 'agent-x.log',
        text: `user\n# BRIEF worker\nWrite \`_coord/results/agent-x.md\`\n\nhook: SessionStart\n- \`_coord/results/agent-x.md\`\nImplemented handoff.\n300 tokens used\n`,
      },
    ],
  });

  assert.equal(bundle.timeComparison.deltas[0].direction, 'regressed');
  assert.equal(bundle.timeComparison.deltas[0].tokenDelta > 0, true);
  assert.equal(bundle.timeComparison.deltas[0].completionDelta <= 0, true);
});

test('time comparison marks mixed outcome when progress comes with efficiency loss', () => {
  const bundle = analyzeAgentLogs([
    {
      input: 'agent-mixed.log',
      text: `user\n# BRIEF worker\nWrite \`_coord/results/agent-mixed.md\`\n\nhook: SessionStart\n- \`_coord/results/agent-mixed.md\`\ntested locally\nverification boundary: runtime unavailable\n500 tokens used\n`,
    },
  ], {
    compareToEntries: [
      {
        input: 'agent-mixed.log',
        text: `user\n# BRIEF worker\nWrite \`_coord/results/agent-mixed.md\`\n\nhook: SessionStart\n- \`_coord/results/agent-mixed.md\`\nverification boundary: runtime unavailable\n200 tokens used\n`,
      },
    ],
  });

  assert.equal(bundle.timeComparison.summary.mixed, 1);
  assert.equal(bundle.timeComparison.deltas[0].direction, 'mixed');
});
