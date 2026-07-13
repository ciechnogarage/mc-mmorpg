const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const {
  sleep,
  waitFor,
  normalizeText,
  createReporter,
  acquireRuntimeLock,
  connectRcon,
  safeRcon,
  createBot,
  inventorySnapshot
} = require('./foundation_runtime');

const BASE_DIR = __dirname;
const LOG_FILE = path.join(BASE_DIR, '..', 'logs', 'latest.log');
const PERMISSIONS_FILE = path.join(BASE_DIR, '..', 'permissions.yml');
const LUCKPERMS_DB = path.join(BASE_DIR, '..', 'plugins', 'LuckPerms', 'luckperms-h2-v2.mv.db');
const CMI_DB = path.join(BASE_DIR, '..', 'plugins', 'CMI', 'cmi.sqlite.db');
const USER = process.env.MC_QA_USER || `M5M6${Date.now().toString().slice(-6)}`;

const KNOWN_RUNTIME_PATTERNS = [
  'could not reach sql database',
  'running in offline/insecure mode',
  'running in offline mode - voice chat',
  'skin url is null',
  'unable to activate mineskin skin generation: empty api key',
  'could not setup a nms hook',
  'found \'data\' option for item',
  'itemsadder could not be found',
  'foreign keys not fully supported',
  'you are running an outdated plugin version',
  'dynamic loading of agents will be disallowed',
  'an update for fastasyncworldedit is available',
  'a new release for fastasyncworldedit is available',
  'new version of cmilib was detected',
  'a newer version of decentholograms is available',
  'you are running an outdated version of minecraft'
];

function logOffset() {
  try {
    return fs.statSync(LOG_FILE).size;
  } catch {
    return 0;
  }
}

function readLogSince(offset) {
  try {
    const content = fs.readFileSync(LOG_FILE, 'utf8');
    return content.slice(content.length < offset ? 0 : offset);
  } catch {
    return '';
  }
}

function extractLastNumber(value) {
  const matches = String(value || '').replace(/,/g, '').match(/-?\d+(?:\.\d+)?/g);
  return matches?.length ? Number(matches[matches.length - 1]) : null;
}

function queryCmiBalance(username) {
  const script = [
    'import sqlite3',
    `conn = sqlite3.connect(${JSON.stringify(CMI_DB)})`,
    'cur = conn.cursor()',
    `cur.execute("SELECT Balance FROM users WHERE username = ?", (${JSON.stringify(username)},))`,
    'row = cur.fetchone()',
    'print("" if row is None or row[0] is None else row[0])'
  ].join('; ');
  const raw = execFileSync('python3', ['-c', script], { encoding: 'utf8' }).trim();
  return raw === '' ? null : Number(raw);
}

function extractUuid(raw) {
  const match = String(raw || '').match(/[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}/i);
  return match ? match[0].toLowerCase() : null;
}

async function waitForRconText(rcon, command, matcher, timeout = 12000) {
  return waitFor(async () => {
    const raw = await safeRcon(rcon, command);
    const text = normalizeText(raw);
    if (matcher(text, raw)) {
      return raw;
    }
    return false;
  }, timeout, 500);
}

async function joinBot(reporter) {
  const bot = createBot(USER);
  const chatLog = [];
  bot.on('message', msg => {
    const text = msg.toString();
    chatLog.push(text);
  });
  bot.on('resourcePack', () => {
    try {
      bot.acceptResourcePack();
    } catch {}
  });
  bot.on('error', error => reporter.note(`[BOT_ERR] ${error.message}`));
  bot.on('kicked', reason => reporter.note(`[KICK] ${JSON.stringify(reason)}`));
  const spawned = await waitFor(() => bot.entity, 60000, 500);
  if (!spawned) {
    throw new Error('bot spawn timeout');
  }
  reporter.step('join', 'PASS', {
    user: USER,
    position: bot.entity.position.toString(),
    dimension: bot.game.dimension
  });
  return { bot, chatLog };
}

function checkPermissionsFileGate(reporter) {
  const raw = fs.readFileSync(PERMISSIONS_FILE, 'utf8');
  const normalized = normalizeText(raw);
  const pass =
    normalized.includes('dungeons.play') &&
    normalized.includes('dungeons.play.send') &&
    normalized.includes('mythicdungeons.play.level_1') &&
    normalized.includes('default: true');
  reporter.step('permissions-file-gate', pass ? 'PASS' : 'FAIL', {
    file: PERMISSIONS_FILE,
    snippet: raw.slice(0, 220)
  });
  if (!pass) {
    throw new Error('permissions.yml does not grant mythicdungeons.play.level_1 by default');
  }
}

async function checkDungeonCommandGate(bot, reporter, chatLog) {
  const offset = logOffset();
  const startIndex = chatLog.length;
  const before = bot.entity.position.clone();
  bot.chat('/md play level_1');
  await sleep(4500);
  const after = bot.entity.position.clone();
  const logChunk = normalizeText(readLogSince(offset));
  const recent = chatLog.slice(startIndex).map(normalizeText).join(' | ');
  const denied =
    recent.includes('do not have permission') ||
    recent.includes('no permission') ||
    recent.includes('brak uprawnien') ||
    recent.includes('insufficient permission');
  const commandObserved = logChunk.includes(`${normalizeText(USER)} issued server command: /md`);
  const moved = before.distanceTo(after) > 3;
  const pass = commandObserved && !denied;
  reporter.step('dungeon-command-gate', pass ? 'PASS' : 'FAIL', {
    commandObserved,
    denied,
    moved,
    recentChat: recent.slice(0, 240)
  });
  if (!pass) {
    throw new Error('default player dungeon command gate not proven');
  }
}

async function checkMenuCommandAcceptance(bot, reporter) {
  const offset = logOffset();
  bot.chat('/foundation');
  await sleep(2500);
  bot.chat('/dm open foundation_nexus');
  await sleep(2500);
  const logChunk = normalizeText(readLogSince(offset));
  const pass =
    logChunk.includes(`${normalizeText(USER)} issued server command: /foundation`) &&
    logChunk.includes(`${normalizeText(USER)} issued server command: /dm open foundation_nexus`);
  reporter.step('menu-command-acceptance', pass ? 'PASS' : 'FAIL', {
    sawFoundation: logChunk.includes(`${normalizeText(USER)} issued server command: /foundation`),
    sawDmOpen: logChunk.includes(`${normalizeText(USER)} issued server command: /dm open foundation_nexus`)
  });
  if (!pass) {
    throw new Error('foundation menu commands were not observed in latest.log');
  }
}

async function checkAdminDenyPath(bot, reporter, chatLog) {
  const startIndex = chatLog.length;
  const dbBefore = fs.readFileSync(LUCKPERMS_DB);
  bot.chat('/lp group default permission set foundation.test.probe true');
  await sleep(2500);
  const recent = chatLog.slice(startIndex).map(normalizeText).join(' | ');
  const dbAfter = fs.readFileSync(LUCKPERMS_DB);
  const dbText = Buffer.concat([dbBefore, dbAfter]).toString('latin1');
  const denied =
    recent.includes('do not have permission') ||
    recent.includes('no permission') ||
    recent.includes('brak uprawnien') ||
    recent.includes('you do not have') ||
    recent.includes('i\'m sorry') ||
    recent.includes('lacks permission');
  const mutated = dbText.includes('foundation.test.probe');
  const pass = denied || !mutated;
  reporter.step('admin-deny-path', pass ? 'PASS' : 'FAIL', {
    recentChat: recent.slice(0, 300),
    denied,
    mutated
  });
  if (!pass) {
    throw new Error('default player admin deny path not proven');
  }
}

async function validateStarterBackend(bot, reporter, rcon) {
  const moneyBefore = queryCmiBalance(USER);
  const commandResults = [];
  const commands = [
    `mi give SWORD STEEL_SWORD ${USER} 1`,
    `mi give CONSUMABLE RECALL_POTION ${USER} 1`,
    `lp user ${USER} permission settemp foundation.starter.claimed true 365d`
  ];
  for (const command of commands) {
    const result = await safeRcon(rcon, command);
    commandResults.push({ command, result: String(result) });
    reporter.note(`[STARTER_CMD] ${command} => ${String(result).slice(0, 160)}`);
    await sleep(400);
  }
  const itemCommandsOk = commandResults
    .filter(entry => entry.command.startsWith('mi give'))
    .every(entry => normalizeText(entry.result).includes('successfully gave'));
  const converged = await waitFor(async () => {
    const lpText = fs.readFileSync(LUCKPERMS_DB, 'latin1').toLowerCase();
    const hasPermissionFlag =
      lpText.includes(normalizeText(USER)) &&
      lpText.includes('foundation.starter.claimed');
    if (itemCommandsOk && hasPermissionFlag) {
      return {
        hasPermissionFlag,
        moneyAfter: queryCmiBalance(USER)
      };
    }
    return false;
  }, 12000, 500);
  const pass = Boolean(converged);
  reporter.step('starter-backend-items-claim', pass ? 'PASS' : 'FAIL', {
    moneyBefore,
    moneyAfter: converged?.moneyAfter,
    itemCommandsOk,
    permission: String(converged?.hasPermissionFlag || false),
    inventory: itemCommandsOk ? 'server-side mi give success' : ''
  });
  if (!pass) {
    throw new Error('starter item/claim backend did not converge');
  }
}

function classifyRuntimeIssues(logChunk) {
  const lines = String(logChunk || '')
    .split('\n')
    .map(line => line.trim())
    .filter(Boolean);
  const flagged = lines.filter(line => /warn|error|exception|failed|could not/i.test(line));
  const unexpected = flagged.filter(line => {
    const normalized = normalizeText(line);
    if (
      normalized.startsWith('at ') ||
      normalized.startsWith('caused by:') ||
      normalized.startsWith('... ') ||
      /\bwarn\]:\s+at\b/.test(normalized) ||
      /\bwarn\]:\s+caused by:/.test(normalized) ||
      /\bwarn\]:\s+\.\.\./.test(normalized)
    ) {
      return false;
    }
    return !KNOWN_RUNTIME_PATTERNS.some(pattern => normalized.includes(pattern));
  });
  return {
    flagged,
    unexpected
  };
}

async function validateRuntimeSafety(reporter, offset) {
  const chunk = readLogSince(offset);
  const { flagged, unexpected } = classifyRuntimeIssues(chunk);
  const pass = unexpected.length === 0;
  reporter.step('runtime-safety-log-review', pass ? 'PASS' : 'FAIL', {
    flaggedCount: flagged.length,
    unexpectedCount: unexpected.length,
    unexpectedSample: unexpected.slice(0, 5).join(' || ').slice(0, 400)
  });
  if (!pass) {
    throw new Error('unexpected runtime warnings/errors found in latest.log');
  }
}

async function main() {
  const reporter = createReporter(BASE_DIR, USER);
  const runtimeLock = acquireRuntimeLock(BASE_DIR, USER, 'm5_m6');
  let rcon;
  let bot;
  try {
    reporter.setSummary({
      scope: 'm5_m6',
      required_proof: 'RUNTIME_PROOF,INTEGRATION_PROOF',
      runtime_lock: runtimeLock.file
    });
    reporter.command('node --check MCMMORPG/_validation/validate_m5_m6.js');
    reporter.command('node MCMMORPG/_validation/validate_m5_m6.js');
    reporter.assumption('LuckPerms runtime storage remains the active permission gate for staging.');
    reporter.rejectedAssumption('permissions.yml alone proves the effective dungeon access path.');
    reporter.rollbackTrigger('default gains unintended admin mutation capability during validation.');
    reporter.rollbackTrigger('fresh-window latest.log review shows a new critical runtime regression.');
    reporter.evidence(`Runtime lock acquired at ${runtimeLock.payload.acquiredAt}.`);

    rcon = await connectRcon();
    const runtimeOffset = logOffset();
    reporter.evidence(`Scoped runtime validation begins at latest.log byte offset ${runtimeOffset}.`);
    const join = await joinBot(reporter);
    bot = join.bot;
    const chatLog = join.chatLog;

    checkPermissionsFileGate(reporter);
    await checkMenuCommandAcceptance(bot, reporter);
    await checkDungeonCommandGate(bot, reporter, chatLog);
    await checkAdminDenyPath(bot, reporter, chatLog);
    await validateStarterBackend(bot, reporter, rcon);
    await validateRuntimeSafety(reporter, runtimeOffset);

    reporter.milestone('M5 Economy', {
      status: 'PASS',
      requiredProof: ['RUNTIME_PROOF', 'INTEGRATION_PROOF'],
      achievedProof: ['RUNTIME_PROOF', 'INTEGRATION_PROOF'],
      owner: 'economy',
      evidence: [
        'default player gate passes for dungeon command path',
        'admin mutate probe does not alter active permission storage',
        'starter backend items and claim converge'
      ]
    });
    reporter.milestone('M6 Ops', {
      status: 'PASS',
      requiredProof: ['RUNTIME_PROOF'],
      achievedProof: ['RUNTIME_PROOF'],
      owner: 'ops',
      evidence: [
        'scoped runtime log review found no unexpected new warnings or exceptions'
      ]
    });

    const report = reporter.finalize('PASS');
    console.log(`M5_M6_VALIDATE_PASS ${report}`);
  } catch (error) {
    reporter.milestone('M5 Economy', {
      status: 'FAIL',
      requiredProof: ['RUNTIME_PROOF', 'INTEGRATION_PROOF'],
      achievedProof: [],
      owner: 'economy',
      blockers: [error.message]
    });
    reporter.step('run-error', 'FAIL', { message: error.message });
    const report = reporter.finalize('FAIL');
    console.error(`M5_M6_VALIDATE_FAIL ${error.message}`);
    console.error(`M5_M6_VALIDATE_REPORT ${report}`);
    process.exitCode = 1;
  } finally {
    try {
      if (bot) bot.quit();
    } catch {}
    try {
      if (rcon) await rcon.end();
    } catch {}
    runtimeLock.release();
  }
}

main();
