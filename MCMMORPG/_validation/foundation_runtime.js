const fs = require('fs');
const path = require('path');
const { execFile } = require('child_process');
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const { pathfinder } = require('mineflayer-pathfinder');

const HOST = process.env.MC_HOST || '127.0.0.1';
const PORT = Number(process.env.MC_PORT || 25565);
const RPORT = Number(process.env.MC_RCON_PORT || 25575);
const RPASS = process.env.RCON_PASS;
const VERSION = process.env.MC_VERSION || '1.21.11';
const RCON_TRANSPORT = process.env.MC_RCON_TRANSPORT || 'tcp';
const DOCKER_DOMAIN = process.env.MC_DOCKER_DOMAIN || 'items';
const DOCKER_MC = process.env.MC_DOCKER_MC || path.resolve(__dirname, '..', 'docker', 'mc');
const PROOF_LEVELS = [
  'SPEC_FIDELITY',
  'VISUAL_FIDELITY',
  'STATIC_CONTRACT',
  'RUNTIME_PROOF',
  'INTEGRATION_PROOF',
  'PLAYER_PROOF'
];
const REPORT_VERDICTS = ['UNKNOWN', 'PASS', 'FAIL', 'BLOCKED', 'INSUFFICIENT_EVIDENCE'];

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function waitFor(fn, timeout = 30000, step = 300) {
  const start = Date.now();
  while (Date.now() - start < timeout) {
    const value = await fn();
    if (value) return value;
    await sleep(step);
  }
  return false;
}

function stripColors(value) {
  return String(value || '')
    .replace(/[\u00A7&][0-9A-FK-ORX]/gi, '')
    .replace(/<[^>]+>/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function normalizeText(value) {
  return stripColors(value).toLowerCase();
}

function flattenText(value) {
  if (value == null) return '';
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  if (Array.isArray(value)) {
    return value.map(flattenText).filter(Boolean).join(' ');
  }
  if (typeof value === 'object') {
    const preferred = [
      value.text,
      value.translate,
      value.value,
      value.extra,
      value.with,
      value.color
    ]
      .map(flattenText)
      .filter(Boolean);
    if (preferred.length) return preferred.join(' ');
    try {
      return JSON.stringify(value);
    } catch {
      return String(value);
    }
  }
  return String(value);
}

function getWindowTitle(window) {
  return flattenText(
    window?.title ||
    window?.inventoryTitle ||
    window?.name ||
    window?.displayName ||
    ''
  );
}

function itemText(item) {
  if (!item) return '';
  const parts = [
    item.name,
    item.displayName,
    item.customName,
    flattenText(item?.nbt),
    flattenText(item?.nbt?.value?.display?.value?.Name?.value)
  ].filter(Boolean);
  return normalizeText(parts.join(' '));
}

function findWindowSlot(window, matcher) {
  if (!window?.slots) return null;
  for (let i = 0; i < window.slots.length; i++) {
    const item = window.slots[i];
    if (!item) continue;
    if (matcher(item, i)) return i;
  }
  return null;
}

function inventorySnapshot(bot) {
  const items = bot.inventory?.items?.() || [];
  return items.map(item => ({
    name: item.name,
    displayName: item.displayName,
    count: item.count,
    slot: item.slot,
    text: itemText(item)
  }));
}

function makeRunId() {
  return new Date().toISOString().replace(/[:.]/g, '-');
}

function ensureRunsDir(baseDir) {
  const runsDir = path.join(baseDir, 'runs');
  fs.mkdirSync(runsDir, { recursive: true });
  return runsDir;
}

function acquireRuntimeLock(baseDir, user, scope = 'foundation-runtime') {
  const lockFile = path.join(baseDir, '.runtime.lock');
  const payload = {
    scope,
    user,
    pid: process.pid,
    acquiredAt: new Date().toISOString()
  };
  try {
    const fd = fs.openSync(lockFile, 'wx');
    fs.writeFileSync(fd, JSON.stringify(payload, null, 2) + '\n', 'utf8');
    fs.closeSync(fd);
  } catch (error) {
    if (error.code !== 'EEXIST') throw error;
    let existing = '';
    try {
      existing = fs.readFileSync(lockFile, 'utf8').trim();
    } catch {}
    throw new Error(`runtime lock already held: ${existing || lockFile}`);
  }

  return {
    file: lockFile,
    payload,
    release() {
      try {
        fs.unlinkSync(lockFile);
      } catch (error) {
        if (error.code !== 'ENOENT') throw error;
      }
    }
  };
}

function createReporter(baseDir, user) {
  const runId = makeRunId();
  const runsDir = ensureRunsDir(baseDir);
  const file = path.join(runsDir, `foundation_bot_${runId}_${user}.md`);
  const state = {
    runId,
    user,
    startedAt: new Date().toISOString(),
    steps: [],
    notes: [],
    verdict: 'UNKNOWN',
    summary: {},
    commands: [],
    assumptions: [],
    rejectedAssumptions: [],
    risks: [],
    blockers: [],
    rollbackTriggers: [],
    milestones: {},
    evidence: [],
    stagingBaseline: [],
    failureSeam: null
  };

  function note(line) {
    state.notes.push(line);
  }

  function setSummary(summary = {}) {
    state.summary = {
      ...state.summary,
      ...summary
    };
  }

  function command(value) {
    if (value) state.commands.push(value);
  }

  function assumption(value) {
    if (value) state.assumptions.push(value);
  }

  function rejectedAssumption(value) {
    if (value) state.rejectedAssumptions.push(value);
  }

  function risk(value) {
    if (value) state.risks.push(value);
  }

  function blocker(value) {
    if (value) state.blockers.push(value);
  }

  function rollbackTrigger(value) {
    if (value) state.rollbackTriggers.push(value);
  }

  function evidence(value) {
    if (value) state.evidence.push(value);
  }

  function stagingBaseline(value) {
    if (value) state.stagingBaseline.push(value);
  }

  function setFailureSeam(seam, details = {}) {
    if (!seam) return;
    state.failureSeam = {
      seam,
      symptom: details.symptom || '',
      nextAction: details.nextAction || ''
    };
  }

  function milestone(name, details = {}) {
    const requiredProof = (details.requiredProof || []).filter(level => PROOF_LEVELS.includes(level));
    const achievedProof = (details.achievedProof || []).filter(level => PROOF_LEVELS.includes(level));
    state.milestones[name] = {
      status: details.status || 'UNKNOWN',
      requiredProof,
      achievedProof,
      evidence: details.evidence || [],
      blockers: details.blockers || [],
      risks: details.risks || [],
      owner: details.owner || '',
      nextAction: details.nextAction || ''
    };
  }

  function step(name, status, details = {}) {
    state.steps.push({
      name,
      status,
      at: new Date().toISOString(),
      details
    });
  }

  function milestoneMissingProof(entry) {
    return (entry.requiredProof || []).filter(level => !(entry.achievedProof || []).includes(level));
  }

  function validateConsistency(verdict) {
    if (!REPORT_VERDICTS.includes(verdict)) {
      throw new Error(`invalid foundation report verdict: ${verdict}`);
    }

    const milestoneEntries = Object.entries(state.milestones || {});
    const blockedMilestones = milestoneEntries.filter(([, entry]) => entry.status === 'BLOCKED');
    const failedMilestones = milestoneEntries.filter(([, entry]) => entry.status === 'FAIL');
    const insufficientMilestones = milestoneEntries.filter(([, entry]) => entry.status === 'INSUFFICIENT_EVIDENCE');
    const passWithMissingProof = milestoneEntries.filter(([, entry]) =>
      entry.status === 'PASS' && milestoneMissingProof(entry).length > 0
    );
    const passWithBlockers = milestoneEntries.filter(([, entry]) =>
      entry.status === 'PASS' && (entry.blockers || []).length > 0
    );

    if (passWithMissingProof.length) {
      const names = passWithMissingProof.map(([name]) => name).join(', ');
      throw new Error(`foundation report inconsistent: PASS milestone missing required proof: ${names}`);
    }

    if (passWithBlockers.length) {
      const names = passWithBlockers.map(([name]) => name).join(', ');
      throw new Error(`foundation report inconsistent: PASS milestone carries blockers: ${names}`);
    }

    if (verdict === 'PASS') {
      if (state.blockers.length || blockedMilestones.length || failedMilestones.length || insufficientMilestones.length) {
        throw new Error('foundation report inconsistent: PASS verdict with blockers or non-pass milestones');
      }
    }

    if (verdict === 'BLOCKED') {
      if (!state.blockers.length && !blockedMilestones.length) {
        throw new Error('foundation report inconsistent: BLOCKED verdict without blocker evidence');
      }
      if (failedMilestones.length) {
        throw new Error('foundation report inconsistent: BLOCKED verdict with FAIL milestone');
      }
    }

    if (verdict === 'FAIL' && !failedMilestones.length) {
      const runError = state.steps.some(step => step.name === 'run-error' && step.status === 'FAIL');
      if (!runError) {
        throw new Error('foundation report inconsistent: FAIL verdict without FAIL milestone or run-error step');
      }
    }

    if (verdict === 'INSUFFICIENT_EVIDENCE') {
      if (!insufficientMilestones.length && !state.blockers.length) {
        throw new Error('foundation report inconsistent: INSUFFICIENT_EVIDENCE without missing-evidence marker');
      }
    }
  }

  function write() {
    const lines = [];
    lines.push(`# Foundation QA Bot Run ${runId}`);
    lines.push('');
    lines.push(`- user: \`${state.user}\``);
    lines.push(`- started_at: \`${state.startedAt}\``);
    lines.push(`- verdict: \`${state.verdict}\``);
    const summaryKeys = Object.keys(state.summary || {});
    if (summaryKeys.length) {
      lines.push('');
      lines.push('## Summary');
      lines.push('');
      for (const key of summaryKeys) {
        lines.push(`- ${key}: \`${flattenText(state.summary[key])}\``);
      }
    }
    const milestoneNames = Object.keys(state.milestones || {});
    if (milestoneNames.length) {
      lines.push('');
      lines.push('## Milestones');
      lines.push('');
      for (const name of milestoneNames) {
        const entry = state.milestones[name];
        lines.push(`- \`${name}\` ${entry.status}`);
        lines.push(`  - required_proof: \`${entry.requiredProof.join(', ') || 'none'}\``);
        lines.push(`  - achieved_proof: \`${entry.achievedProof.join(', ') || 'none'}\``);
        if (entry.owner) lines.push(`  - owner: \`${entry.owner}\``);
        if (entry.evidence.length) lines.push(`  - evidence: \`${entry.evidence.join(' | ')}\``);
        if (entry.risks.length) lines.push(`  - risks: \`${entry.risks.join(' | ')}\``);
        if (entry.blockers.length) lines.push(`  - blockers: \`${entry.blockers.join(' | ')}\``);
        if (entry.nextAction) lines.push(`  - next_action: \`${entry.nextAction}\``);
      }
    }
    if (state.commands.length) {
      lines.push('');
      lines.push('## Validation Commands');
      lines.push('');
      for (const value of state.commands) {
        lines.push(`- \`${value}\``);
      }
    }
    if (state.evidence.length) {
      lines.push('');
      lines.push('## Evidence');
      lines.push('');
      for (const value of state.evidence) {
        lines.push(`- ${value}`);
      }
    }
    if (state.stagingBaseline.length) {
      lines.push('');
      lines.push('## Staging Baseline');
      lines.push('');
      for (const value of state.stagingBaseline) {
        lines.push(`- ${value}`);
      }
    }
    if (state.failureSeam) {
      lines.push('');
      lines.push('## Failure Seam');
      lines.push('');
      lines.push(`- seam: \`${state.failureSeam.seam}\``);
      if (state.failureSeam.symptom) {
        lines.push(`- symptom: \`${flattenText(state.failureSeam.symptom)}\``);
      }
      if (state.failureSeam.nextAction) {
        lines.push(`- next_action: \`${flattenText(state.failureSeam.nextAction)}\``);
      }
    }
    lines.push('');
    lines.push('## Steps');
    lines.push('');
    for (const entry of state.steps) {
      lines.push(`- \`${entry.status}\` ${entry.name}`);
      const detailKeys = Object.keys(entry.details || {});
      for (const key of detailKeys) {
        lines.push(`  - ${key}: \`${flattenText(entry.details[key])}\``);
      }
    }
    if (state.risks.length) {
      lines.push('');
      lines.push('## Risks');
      lines.push('');
      for (const value of state.risks) {
        lines.push(`- ${value}`);
      }
    }
    if (state.blockers.length) {
      lines.push('');
      lines.push('## Blockers');
      lines.push('');
      for (const value of state.blockers) {
        lines.push(`- ${value}`);
      }
    }
    if (state.rejectedAssumptions.length) {
      lines.push('');
      lines.push('## Rejected Assumptions');
      lines.push('');
      for (const value of state.rejectedAssumptions) {
        lines.push(`- ${value}`);
      }
    }
    if (state.assumptions.length) {
      lines.push('');
      lines.push('## Assumptions');
      lines.push('');
      for (const value of state.assumptions) {
        lines.push(`- ${value}`);
      }
    }
    if (state.rollbackTriggers.length) {
      lines.push('');
      lines.push('## Rollback Triggers');
      lines.push('');
      for (const value of state.rollbackTriggers) {
        lines.push(`- ${value}`);
      }
    }
    lines.push('');
    lines.push('## Notes');
    lines.push('');
    for (const line of state.notes) {
      lines.push(`- ${line}`);
    }
    fs.writeFileSync(file, lines.join('\n') + '\n', 'utf8');
    return file;
  }

  return {
    state,
    note,
    setSummary,
    command,
    assumption,
    rejectedAssumption,
    risk,
    blocker,
    rollbackTrigger,
    evidence,
    stagingBaseline,
    setFailureSeam,
    milestone,
    step,
    finalize(verdict) {
      validateConsistency(verdict);
      state.verdict = verdict;
      return write();
    }
  };
}

async function connectRcon() {
  if (RCON_TRANSPORT === 'docker-mc') {
    return {
      async send(command) {
        return await new Promise((resolve, reject) => {
          execFile(
            DOCKER_MC,
            ['rcon', DOCKER_DOMAIN, command],
            { cwd: path.dirname(DOCKER_MC), maxBuffer: 1024 * 1024 },
            (error, stdout, stderr) => {
              if (error) {
                reject(new Error((stderr || stdout || error.message).trim()));
                return;
              }
              resolve((stdout || stderr || '').trim());
            }
          );
        });
      },
      async end() {
        return undefined;
      }
    };
  }
  return Rcon.connect({
    host: HOST,
    port: RPORT,
    password: RPASS
  });
}

async function safeRcon(rcon, command) {
  try {
    return await rcon.send(command);
  } catch (error) {
    return `ERR ${error.message}`;
  }
}

function createBot(user) {
  const bot = mineflayer.createBot({
    host: HOST,
    port: PORT,
    username: user,
    auth: 'offline',
    version: VERSION
  });
  return bot;
}

function ensurePathfinder(bot) {
  if (!bot?.pathfinder) {
    bot.loadPlugin(pathfinder);
  }
  return bot.pathfinder;
}

async function waitForWindow(bot, matcher, timeout = 10000) {
  const initial = bot.currentWindow;
  if (initial && matcher(initial)) return initial;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      clearInterval(poller);
      bot.removeListener('windowOpen', onOpen);
      reject(new Error('window open timeout'));
    }, timeout);
    const poller = setInterval(() => {
      const current = bot.currentWindow;
      if (!current || !matcher(current)) return;
      clearTimeout(timer);
      clearInterval(poller);
      bot.removeListener('windowOpen', onOpen);
      resolve(current);
    }, 200);
    function onOpen(window) {
      if (!matcher(window)) return;
      clearTimeout(timer);
      clearInterval(poller);
      bot.removeListener('windowOpen', onOpen);
      resolve(window);
    }
    bot.on('windowOpen', onOpen);
  });
}

async function clickWindowSlot(bot, window, slot) {
  if (bot.clickWindow) {
    await bot.clickWindow(slot, 0, 0);
  } else if (bot.simpleClick?.leftMouse) {
    await bot.simpleClick.leftMouse(slot);
  }
  await sleep(350);
  return bot.currentWindow || window;
}

module.exports = {
  HOST,
  PORT,
  RPORT,
  RPASS,
  VERSION,
  sleep,
  waitFor,
  stripColors,
  normalizeText,
  flattenText,
  getWindowTitle,
  itemText,
  findWindowSlot,
  inventorySnapshot,
  PROOF_LEVELS,
  REPORT_VERDICTS,
  acquireRuntimeLock,
  createReporter,
  connectRcon,
  safeRcon,
  createBot,
  ensurePathfinder,
  waitForWindow,
  clickWindowSlot
};
