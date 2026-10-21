'use strict';

// QA: scenariusze, asercje, raporty PASS/FAIL/BLOCKED (plan sekcje E/I).
// Raport ląduje w bots/runs/ jako markdown + json.

const fs = require('fs');
const path = require('path');
const { validateContract } = require('./schema-check');

const VERDICTS = ['PASS', 'FAIL', 'BLOCKED'];

function createReporter({ name, runsDir }) {
  const dir = runsDir || path.join(__dirname, '..', '..', 'runs');
  fs.mkdirSync(dir, { recursive: true });
  const runId = new Date().toISOString().replace(/[:.]/g, '-');
  const state = { name, runId, startedAt: new Date().toISOString(), steps: [], blockers: [], verdict: 'UNKNOWN' };

  return {
    state,
    step(stepName, status, details = {}) {
      state.steps.push({ name: stepName, status, at: new Date().toISOString(), details });
      (status === 'FAIL' ? console.error : console.log)(`  [${status}] ${stepName}`, Object.keys(details).length ? JSON.stringify(details).slice(0, 200) : '');
    },
    blocker(text) {
      state.blockers.push(text);
    },
    finalize(verdict) {
      if (!VERDICTS.includes(verdict)) throw new Error(`zły werdykt: ${verdict}`);
      if (verdict === 'PASS' && (state.blockers.length || state.steps.some(s => s.status === 'FAIL'))) {
        throw new Error('raport niespójny: PASS z blockerami/failami');
      }
      state.verdict = verdict;
      state.finishedAt = new Date().toISOString();
      const base = path.join(dir, `${name}_${runId}`);
      fs.writeFileSync(`${base}.json`, JSON.stringify(state, null, 2) + '\n');
      const md = [
        `# ${name} — ${verdict}`,
        '',
        `- run: ${runId}`,
        `- start: ${state.startedAt}`,
        `- koniec: ${state.finishedAt}`,
        '',
        '## Kroki',
        '',
        ...state.steps.map(s => `- \`${s.status}\` ${s.name}${Object.keys(s.details).length ? ` — \`${JSON.stringify(s.details).slice(0, 300)}\`` : ''}`),
        ...(state.blockers.length ? ['', '## Blockery', '', ...state.blockers.map(b => `- ${b}`)] : []),
        ''
      ].join('\n');
      fs.writeFileSync(`${base}.md`, md);
      return `${base}.md`;
    }
  };
}

// Runner scenariusza: kroki wykonują się sekwencyjnie; wyjątek = FAIL kroku,
// kolejne kroki lecą dalej chyba że krok ma critical:true.
//
// Reguła korelacji z protokołem: krok może zadeklarować `relevantPackets`;
// scenariusz dostaje `interference(startMs, endMs, packets)` (wpięty w
// safe-protocol.recoverableInWindow). Recoverable drop pakietu istotnego dla
// asercji w jej oknie => INCONCLUSIVE => werdykt BLOCKED, nie PASS — bo
// dropniętego pakietu nie da się odróżnić od "brak zdarzenia".
async function runScenario({ name, steps, runsDir, interference }) {
  const reporter = createReporter({ name, runsDir });
  let failed = false;
  let blocked = false;
  let inconclusive = false;
  for (const { name: stepName, run, critical = false, relevantPackets = null } of steps) {
    if (blocked) { reporter.step(stepName, 'SKIP', { reason: 'wcześniejszy krok krytyczny padł' }); continue; }
    const startMs = Date.now();
    try {
      const details = await run(reporter);
      const hits = (interference && relevantPackets !== undefined)
        ? interference(startMs, Date.now(), relevantPackets) || []
        : [];
      if (relevantPackets && hits.length) {
        inconclusive = true;
        reporter.step(stepName, 'INCONCLUSIVE', { ...details, droppedPackets: hits.slice(0, 10) });
        reporter.blocker(`interferencja protokołu w oknie asercji: ${stepName} (${hits.length} recoverable)`);
      } else {
        reporter.step(stepName, 'OK', details || {});
      }
    } catch (error) {
      failed = true;
      reporter.step(stepName, 'FAIL', { error: error.message });
      if (critical) {
        blocked = true;
        reporter.blocker(`krok krytyczny padł: ${stepName} — ${error.message}`);
      }
    }
  }
  const verdict = (blocked || inconclusive) ? 'BLOCKED' : failed ? 'FAIL' : 'PASS';
  const file = reporter.finalize(verdict);
  return { verdict, file, state: reporter.state };
}

function assertEquals(actual, expected, label = 'wartość') {
  if (actual !== expected) throw new Error(`${label}: oczekiwano ${JSON.stringify(expected)}, jest ${JSON.stringify(actual)}`);
}

function assertTruthy(value, label = 'wartość') {
  if (!value) throw new Error(`${label}: pusta/falsy`);
  return value;
}

module.exports = { createReporter, runScenario, assertEquals, assertTruthy, validateContract, VERDICTS };
