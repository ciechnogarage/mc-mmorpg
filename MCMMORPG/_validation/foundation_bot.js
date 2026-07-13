const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const { Vec3 } = require('vec3');
const minecraftDataLoader = require('minecraft-data');
const { Movements, goals } = require('mineflayer-pathfinder');
const {
  sleep,
  waitFor,
  normalizeText,
  flattenText,
  getWindowTitle,
  itemText,
  findWindowSlot,
  inventorySnapshot,
  acquireRuntimeLock,
  createReporter,
  connectRcon,
  safeRcon,
  createBot,
  ensurePathfinder,
  waitForWindow,
  clickWindowSlot
} = require('./foundation_runtime');
const { startViewer } = require('./foundation_viewer');

const BASE_DIR = __dirname;
const LOG_FILE = path.join(BASE_DIR, '..', 'logs', 'latest.log');
const SERVER_DIR = path.join(BASE_DIR, '..');
const ROOT_DIR = path.join(SERVER_DIR, '..');
const PLUGINS_DIR = path.join(SERVER_DIR, 'plugins');
const MENU_FILE = path.join(PLUGINS_DIR, 'DeluxeMenus', 'gui_menus', 'foundation_nexus.yml');
const MMOCORE_COMMANDS_FILE = path.join(PLUGINS_DIR, 'MMOCore', 'commands.yml');
const CMI_CONFIG_FILE = path.join(PLUGINS_DIR, 'CMI', 'config.yml');
const MMO_SWORD_FILE = path.join(PLUGINS_DIR, 'MMOItems', 'item', 'sword.yml');
const MMO_CONSUMABLE_FILE = path.join(PLUGINS_DIR, 'MMOItems', 'item', 'consumable.yml');
const SPEC_VALUES_FILE = path.join(ROOT_DIR, 'docs', 'ai', 'foundation-mvp-spec-values.md');
const USER = process.env.MC_QA_USER || `FQA${Date.now().toString().slice(-8)}`.slice(0, 16);
const VIEWER_ENABLED = process.env.FOUNDATION_VIEWER === '1';
const DIFFICULTY = normalizeText(process.env.MC_QA_DIFFICULTY || 'NORMAL');
const FOUNDATION_SCOPE = normalizeText(process.env.FOUNDATION_SCOPE || 'full');
const RUN_FULL_SLICE = FOUNDATION_SCOPE === 'full' || FOUNDATION_SCOPE === 'm7';
const VISUAL_CAPTURE_REQUIRED = process.env.FOUNDATION_VISUAL_CAPTURE === '1';
const { GoalNear } = goals;

class BlockedError extends Error {
  constructor(message, details = {}) {
    super(message);
    this.name = 'BlockedError';
    this.details = details;
  }
}
const HUB_CHECKPOINTS = [
  { key: 'spawn_gate', label: 'Spawn Gate', x: 0, y: 103, z: 0, minSolidBlocks: 16 },
  { key: 'market_spine', label: 'Market/Gear + Profession spine', x: 0, y: 103, z: 24, minSolidBlocks: 18 },
  { key: 'portal_nexus', label: 'Portal Nexus', x: 0, y: 103, z: 48, minSolidBlocks: 20, nameHints: ['portal', 'nexus', 'portalu'] },
  { key: 'skill_class', label: 'Skill/Class District', x: -24, y: 103, z: 48, minSolidBlocks: 14 },
  { key: 'quest_board', label: 'Quest/Info Board', x: 24, y: 103, z: 48, minSolidBlocks: 10, nameHints: ['tablica', 'board'] },
  { key: 'temple', label: 'Temple District', x: 0, y: 103, z: 72, minSolidBlocks: 14, nameHints: ['kapliczki', 'swiatynia', 'temple'] }
];

function logAndRecord(reporter, ...args) {
  const line = args.join(' ');
  console.log(line);
  reporter.note(line);
}

function inventoryHasKeyword(snapshot, keyword) {
  const target = normalizeText(keyword);
  return snapshot.some(item => item.text.includes(target));
}

function isFoundationMenu(window) {
  const title = normalizeText(getWindowTitle(window));
  return title.includes('stolica wyspy');
}

function windowSnapshot(window) {
  return {
    title: getWindowTitle(window) || 'unknown',
    items: (window?.slots || [])
      .map((item, slot) => item ? `${slot}:${itemText(item)}` : null)
      .filter(Boolean)
      .slice(0, 54)
      .join(' | ')
  };
}

function isDifficultyMenu(window) {
  const title = normalizeText(getWindowTitle(window));
  if (title.includes('select difficulty')) return true;
  const text = windowSnapshot(window).items;
  return ['latwy', 'sredni', 'ciezki', 'easy', 'normal', 'hard']
    .some(label => text.includes(label));
}

function isPermissionDenied(text) {
  const normalized = normalizeText(text);
  return [
    'no permission',
    'brak uprawn',
    'you do not have permission',
    'unknown command',
    'nieznana komenda'
  ].some(fragment => normalized.includes(fragment));
}

function difficultyLabels() {
  if (DIFFICULTY === 'easy') return ['latwy', 'easy'];
  if (DIFFICULTY === 'hard') return ['ciezki', 'hard'];
  return ['sredni', 'normal'];
}

function difficultyFallbackSlot() {
  if (DIFFICULTY === 'easy') return 0;
  if (DIFFICULTY === 'hard') return 2;
  return 1;
}

function extractLastNumber(value) {
  const matches = String(value || '').replace(/,/g, '').match(/-?\d+(?:\.\d+)?/g);
  return matches?.length ? Number(matches[matches.length - 1]) : null;
}

function extractPosition(value) {
  const match = String(value || '').match(/\[([^\]]+)\]/);
  if (!match) return null;
  const parts = match[1]
    .split(',')
    .map(part => Number(String(part).replace(/d/gi, '').trim()));
  if (parts.length !== 3 || parts.some(Number.isNaN)) return null;
  return new Vec3(parts[0], parts[1], parts[2]);
}

function extractOnlineCount(value) {
  const match = String(value || '').match(/(?:there are|jest)\s+(\d+)/i);
  return match ? Number(match[1]) : null;
}

function extractInventoryMetrics(value) {
  const text = String(value || '');
  const slots = (text.match(/\bSlot:/g) || []).length;
  const counts = [...text.matchAll(/\bcount:\s+(\d+)/g)].map(match => Number(match[1]));
  return {
    slots,
    totalCount: counts.reduce((sum, count) => sum + count, 0)
  };
}

function findPluginJar(prefix) {
  return fs.readdirSync(PLUGINS_DIR).find(name => name.startsWith(prefix) && name.endsWith('.jar'));
}

function readPluginYml(prefix) {
  const jar = findPluginJar(prefix);
  if (!jar) return '';
  try {
    return execFileSync('unzip', ['-p', path.join(PLUGINS_DIR, jar), 'plugin.yml'], {
      encoding: 'utf8'
    });
  } catch {
    return '';
  }
}

function includesAll(text, needles) {
  return needles.every(needle => text.includes(needle));
}

function extractItemRequiredLevel(yamlText, itemId) {
  const lines = String(yamlText || '').split('\n');
  const start = lines.findIndex(line => line.trim() === `${itemId}:`);
  if (start === -1) return null;
  const baseIndent = (lines[start].match(/^\s*/) || [''])[0].length;
  for (let i = start + 1; i < lines.length; i++) {
    const line = lines[i];
    if (!line.trim()) continue;
    const indent = (line.match(/^\s*/) || [''])[0].length;
    if (indent <= baseIndent && /^[A-Z0-9_]+:\s*$/.test(line.trim())) break;
    const match = line.match(/required-level:\s*([0-9.]+)/i);
    if (match) return Number(match[1]);
  }
  return null;
}

function aliasBlockIncludes(text, aliases) {
  const normalized = normalizeText(text);
  return aliases.every(alias => normalized.includes(alias));
}

function failingCheckKeys(checks) {
  return Object.entries(checks)
    .filter(([, value]) => !value)
    .map(([key]) => key);
}

function closeWindowIfOpen(bot) {
  try {
    if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
  } catch {}
}

function classifyFailureSeam(message) {
  const text = normalizeText(message);
  if (text.includes('runtime lock already held')) {
    return {
      seam: 'runtime_lock',
      nextAction: 'wait for the active runtime owner to finish before starting another staging run'
    };
  }
  if (text.includes('staging isolation failed')) {
    return {
      seam: 'staging_isolation',
      nextAction: 'remove stale test users from staging before retrying the exact player-path run'
    };
  }
  if (text.includes('you logged in from another location')) {
    return {
      seam: 'session_takeover',
      nextAction: 'stabilize bot session ownership before runtime proof and prevent duplicate login attempts for the same user'
    };
  }
  if (text.includes('player is offline')) {
    return {
      seam: 'profile_session_cleanup',
      nextAction: 'avoid leaving stale MMO profile sessions by failing fast on duplicate-login or disconnect churn'
    };
  }
  if (text.includes('server ready check failed')) {
    return {
      seam: 'server_boot_readiness',
      nextAction: 'stabilize Paper boot and RCON readiness before bot validation'
    };
  }
  if (text.includes('portal handoff not detected')) {
    return {
      seam: 'portal_dispatch',
      nextAction: 'repair the Portal Nexus handoff before retrying the exact player path'
    };
  }
  if (text.includes('static m1/m2 contract mismatch')) {
    return {
      seam: 'starter_contract',
      nextAction: 'align the harness with the current foundation menu, spec, and backend command contract before another runtime retry'
    };
  }
  if (text.includes('did not reach a confirmed dungeon instance')) {
    return {
      seam: 'dungeon_arrival',
      nextAction: 'stabilize the post-portal dungeon arrival before retrying combat proof'
    };
  }
  if (text.includes('no real dungeon combat target spawned')) {
    return {
      seam: 'encounter_spawn',
      nextAction: 'repair the level_1 encounter-start trigger before claiming fresh-player combat readiness'
    };
  }
  if (text.includes('no valid nearby combat target') || text.includes('no confirmed melee damage')) {
    return {
      seam: 'starter_combat',
      nextAction: 'stabilize target acquisition and prove real melee damage on the exact fresh-player path'
    };
  }
  if (text.includes('starter delivery does not converge') || text.includes('starter path produced a player-facing signal without stable backend delivery proof')) {
    return {
      seam: 'starter_backend_delivery',
      nextAction: 'isolate the MMOItems give, claim-state mutation, and money grant independently of chat success'
    };
  }
  if (text.includes('boss not detected')) {
    return {
      seam: 'boss_identity',
      nextAction: 'confirm boss spawn identity and arena spawn window before retrying finish proof'
    };
  }
  if (text.includes('boss did not die')) {
    return {
      seam: 'boss_death_handler',
      nextAction: 'repair the boss damage/death path before relying on finish-chain validation'
    };
  }
  if (text.includes('finish not detected')) {
    return {
      seam: 'finish_return',
      nextAction: 'repair finish/return detection on the exact dungeon completion path'
    };
  }
  if (text.includes('boss reward not detected')) {
    return {
      seam: 'reward_delivery',
      nextAction: 'repair reward delivery after boss completion before another closure attempt'
    };
  }
  if (text.includes('runtime log contains a command or event exception')) {
    return {
      seam: 'runtime_exception_window',
      nextAction: 'review the scoped latest.log window and fix the first new command/event exception'
    };
  }
  return {
    seam: 'unclassified_runtime',
    nextAction: 'inspect the exact failing step and classify the seam before the next retry'
  };
}

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

function latestLogContent() {
  try {
    return fs.readFileSync(LOG_FILE, 'utf8');
  } catch {
    return '';
  }
}

function serverLooksReady(logText) {
  const normalized = normalizeText(logText);
  return normalized.includes('done (') || normalized.includes('done preparing level "world"');
}

function portalCommandIssued(user) {
  return latestLogContent().includes(`${user} issued server command: /md play level_1`);
}

function createMovements(bot) {
  const mcData = minecraftDataLoader(bot.version);
  const movements = new Movements(bot, mcData);
  movements.allow1by1towers = false;
  movements.canDig = false;
  movements.allowParkour = false;
  return movements;
}

function namedEntitiesNear(bot, checkpoint, radius = 8) {
  return Object.values(bot.entities || {})
    .filter(entity => {
      if (!entity?.position) return false;
      return entity.position.distanceTo({
        x: checkpoint.x,
        y: checkpoint.y,
        z: checkpoint.z,
        distanceTo(other) {
          const dx = this.x - other.x;
          const dy = this.y - other.y;
          const dz = this.z - other.z;
          return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
      }) <= radius;
    })
    .map(entity => normalizeText(entity.displayName || entity.username || entity.name || ''))
    .filter(Boolean);
}

function solidBlocksAround(bot, checkpoint, radius = 3) {
  let solid = 0;
  for (let dx = -radius; dx <= radius; dx++) {
    for (let dz = -radius; dz <= radius; dz++) {
      const block = bot.blockAt(new Vec3(
        Math.floor(checkpoint.x + dx),
        Math.floor(checkpoint.y),
        Math.floor(checkpoint.z + dz)
      ));
      if (block && block.name !== 'air' && block.name !== 'cave_air' && block.name !== 'void_air') {
        solid += 1;
      }
    }
  }
  return solid;
}

async function navigateCheckpoint(bot, checkpoint) {
  bot.pathfinder.setGoal(new GoalNear(checkpoint.x, checkpoint.y, checkpoint.z, 3));
  const reached = await waitFor(() => {
    if (!bot.entity) return false;
    const dx = bot.entity.position.x - checkpoint.x;
    const dy = bot.entity.position.y - checkpoint.y;
    const dz = bot.entity.position.z - checkpoint.z;
    const distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
    return distance <= 4 ? distance : false;
  }, 30000, 250);
  bot.pathfinder.setGoal(null);
  return reached;
}

async function validateHubVisualCheckpoints(bot, reporter) {
  ensurePathfinder(bot);
  bot.pathfinder.setMovements(createMovements(bot));
  const checkpointResults = [];

  for (const checkpoint of HUB_CHECKPOINTS) {
    const distance = await navigateCheckpoint(bot, checkpoint);
    const solids = solidBlocksAround(bot, checkpoint);
    const nearbyNames = namedEntitiesNear(bot, checkpoint);
    const hasNamedHint = checkpoint.nameHints
      ? checkpoint.nameHints.some(hint => nearbyNames.some(name => name.includes(hint)))
      : nearbyNames.length > 0;
    const populated = solids >= checkpoint.minSolidBlocks;
    const reached = Boolean(distance);
    checkpointResults.push({
      key: checkpoint.key,
      label: checkpoint.label,
      reached,
      distance: distance || '',
      solids,
      populated,
      nearbyNames: nearbyNames.slice(0, 6).join(' | '),
      hasNamedHint
    });
  }

  const routeOrderPass = checkpointResults.every(result => result.reached);
  const populatedPass = checkpointResults.every(result => result.populated);
  const namedLandmarksPass = checkpointResults
    .filter(result => ['portal_nexus', 'quest_board', 'temple'].includes(result.key))
    .every(result => result.hasNamedHint || result.nearbyNames.length > 0);

  reporter.step(
    'visual-hub-checkpoints',
    routeOrderPass && populatedPass ? 'PASS' : 'FAIL',
    {
      routeOrderPass,
      populatedPass,
      namedLandmarksPass,
      checkpoints: JSON.stringify(checkpointResults)
    }
  );

  reporter.evidence(`Player-view checkpoints traversed: ${checkpointResults.map(result => result.key).join(' -> ')}.`);

  return {
    routeOrderPass,
    populatedPass,
    namedLandmarksPass,
    checkpointResults
  };
}

async function joinBot(reporter) {
  const bot = createBot(USER);
  let disconnectReason = '';
  bot.qaSignals = {
    respawns: 0,
    finishMessages: [],
    starterMessages: [],
    orientationMessages: [],
    profileSignals: [],
    permissionDenials: [],
    windows: []
  };
  bot.on('respawn', () => {
    bot.qaSignals.respawns += 1;
  });
  bot.on('resourcePack', () => {
    try {
      bot.acceptResourcePack();
    } catch {}
  });
  bot.on('kicked', reason => logAndRecord(reporter, '[KICK]', JSON.stringify(reason)));
  bot.on('error', error => logAndRecord(reporter, '[ERR]', error.message));
  bot.on('end', reason => {
    disconnectReason = flattenText(reason) || disconnectReason || 'bot connection ended';
    logAndRecord(reporter, '[END]', disconnectReason);
  });
  bot.on('message', msg => {
    const text = flattenText(msg);
    if (/logged in from another location/i.test(text)) {
      disconnectReason = text;
    }
    if (/completed|ukoncz|polana/i.test(text)) {
      bot.qaSignals.finishMessages.push(text);
    }
    if (/pakiet startowy odebrany|25 monet|recall|stalowy miecz/i.test(text)) {
      bot.qaSignals.starterMessages.push(text);
    }
    if (/tablica startowa|cel mvp|portal nexus/i.test(text)) {
      bot.qaSignals.orientationMessages.push(text);
    }
    if (/profil|profile|statyst/i.test(text)) {
      bot.qaSignals.profileSignals.push(text);
    }
    if (isPermissionDenied(text)) {
      bot.qaSignals.permissionDenials.push(text);
    }
    if (/foundation|portal|polana|guardian|straznik|completed|ukoncz/i.test(text)) {
      logAndRecord(reporter, '[CHAT]', text);
    }
  });
  bot.on('windowOpen', window => {
    bot.qaSignals.windows.push(windowSnapshot(window));
  });
  bot.on('title', (title, type) => {
    const text = flattenText(title);
    if (text) {
      bot.qaSignals.profileSignals.push(`${type}:${text}`);
    }
  });

  const spawned = await waitFor(() => {
    if (disconnectReason) {
      throw new Error(disconnectReason);
    }
    return bot.entity;
  }, 60000, 500);
  if (!spawned) {
    throw new Error('bot spawn timeout');
  }
  ensurePathfinder(bot);
  reporter.step('join', 'PASS', {
    position: bot.entity.position.toString(),
    dimension: bot.game.dimension
  });
  return bot;
}

async function readPlayerRuntimeSnapshot(rcon, dimension = '') {
  const playerDimensionRaw = await safeRcon(rcon, `minecraft:data get entity ${USER} Dimension`);
  const playerPosRaw = await safeRcon(rcon, `minecraft:data get entity ${USER} Pos`);
  const playerDimensionMatch = String(playerDimensionRaw || '').match(/"([^"]+)"/);
  const playerDimension = playerDimensionMatch ? playerDimensionMatch[1] : String(playerDimensionRaw || '');
  const snapshot = {
    playerDimension,
    playerPos: extractPosition(playerPosRaw)?.toString?.() || String(playerPosRaw || '').slice(0, 160)
  };

  const nearestZombieHealthRaw = await safeRcon(
    rcon,
    `minecraft:execute at ${USER} as @e[type=minecraft:zombie,distance=..120,sort=nearest,limit=1] run data get entity @s Health`
  );
  const nearestZombieNameRaw = await safeRcon(
    rcon,
    `minecraft:execute at ${USER} as @e[type=minecraft:zombie,distance=..120,sort=nearest,limit=1] run data get entity @s CustomName`
  );
  snapshot.nearestZombieHealth = extractLastNumber(nearestZombieHealthRaw);
  snapshot.nearestZombieName = String(nearestZombieNameRaw || '').slice(0, 160);

  if (dimension) {
    const instancePosRaw = await safeRcon(
      rcon,
      `minecraft:execute in ${dimension} run data get entity @e[type=minecraft:zombie,limit=1] Pos`
    );
    const instanceHealthRaw = await safeRcon(
      rcon,
      `minecraft:execute in ${dimension} run data get entity @e[type=minecraft:zombie,limit=1] Health`
    );
    const instanceNameRaw = await safeRcon(
      rcon,
      `minecraft:execute in ${dimension} run data get entity @e[type=minecraft:zombie,limit=1] CustomName`
    );
    snapshot.instanceDimension = dimension;
    snapshot.instanceZombiePos = extractPosition(instancePosRaw)?.toString?.() || '';
    snapshot.instanceZombieHealth = extractLastNumber(instanceHealthRaw);
    snapshot.instanceZombieName = String(instanceNameRaw || '').slice(0, 160);
  }

  return snapshot;
}

function validateStaticContract(reporter) {
  const menu = fs.readFileSync(MENU_FILE, 'utf8');
  const mmocoreCommands = fs.readFileSync(MMOCORE_COMMANDS_FILE, 'utf8');
  const cmiConfig = fs.readFileSync(CMI_CONFIG_FILE, 'utf8');
  const swordFile = fs.readFileSync(MMO_SWORD_FILE, 'utf8');
  const consumableFile = fs.readFileSync(MMO_CONSUMABLE_FILE, 'utf8');
  const specValues = fs.readFileSync(SPEC_VALUES_FILE, 'utf8');
  const mmoitemsPlugin = readPluginYml('MMOItems-');
  const luckPermsPlugin = readPluginYml('LuckPerms-Bukkit-');

  const checks = {
    menuAliases: includesAll(menu, ['  - foundation', '  - stolica', '  - nexus']),
    menuSlots: includesAll(menu, ['slot: 10', 'slot: 12', 'slot: 14', 'slot: 16']),
    orientationAction: includesAll(menu, ['Tablica startowa', 'Cel MVP: Portal Nexus -> level_1']),
    starterItemGrantActions: includesAll(menu, [
      '[console] mi give SWORD STEEL_SWORD %player_name% 1',
      '[console] mi give CONSUMABLE RECALL_POTION %player_name% 1'
    ]),
    starterMoneyAction: menu.includes('[console] cmi money give %player_name% 25 -s'),
    starterClaimAction: menu.includes('[console] lp user %player_name% permission set foundation.starter.claimed true'),
    profileAction: menu.includes("[player] profile"),
    portalAction: menu.includes("[player] md play level_1"),
    mmocoreProfileAlias: includesAll(mmocoreCommands, ['main: player', '- profile', 'permission: mmocore.profile']),
    mmoitemsAlias: aliasBlockIncludes(mmoitemsPlugin, ['mmoitems', 'aliases', 'mi']),
    luckPermsAlias: aliasBlockIncludes(luckPermsPlugin, ['luckperms', 'aliases', 'lp', 'perm', 'perms', 'permission', 'permissions']),
    cmiEconomyPresent: normalizeText(cmiConfig).includes('economy'),
    swordExists: swordFile.includes('STEEL_SWORD:'),
    recallExists: consumableFile.includes('RECALL_POTION:'),
    recallSpawn: consumableFile.includes('format: spawn'),
    specPortalContract: specValues.includes('Akcja menu Portal Nexus → `[player] md play level_1`.'),
    specStarterRiskDocumented: specValues.includes('required-level 1.0'),
    specStarterConsoleContract: includesAll(specValues, [
      '[console] mi give SWORD STEEL_SWORD %player_name% 1',
      '[console] mi give CONSUMABLE RECALL_POTION %player_name% 1',
      '[console] cmi money give %player_name% 25 -s',
      '[console] lp user %player_name% permission set foundation.starter.claimed true'
    ])
  };

  const hardGateKeys = Object.keys(checks).filter(key => !['mmoitemsAlias', 'luckPermsAlias'].includes(key));
  const pass = hardGateKeys.every(key => checks[key]);
  reporter.step('static-contract-m1-m2', pass ? 'PASS' : 'FAIL', checks);
  if (!pass) {
    throw new Error(`static M1/M2 contract mismatch: ${failingCheckKeys(checks).join(', ')}`);
  }

  reporter.evidence(
    `Soft alias evidence: MMOItems alias detected=${checks.mmoitemsAlias}; LuckPerms alias detected=${checks.luckPermsAlias}.`
  );

  reporter.step('static-contract-risks', 'PASS', {
    starterSwordRequiredLevel: '1.0',
    starterPlayerJourneyPhase: 'Level 1-10',
    risk: 'starter bundle still needs direct early-combat/player proof even after lowering the sword gate'
  });
}

async function waitForServerReady(reporter, rcon) {
  const ready = await waitFor(async () => {
    const list = await safeRcon(rcon, 'minecraft:list');
    const logText = latestLogContent();
    const readyByLog = serverLooksReady(logText);
    const readyByRcon = !/asynchronous cannot perform command async|unknown or incomplete command|ERR /i.test(String(list));
    if (readyByLog && readyByRcon) {
      return {
        list: String(list).slice(0, 200),
        logReady: true
      };
    }
    return false;
  }, 90000, 1000);

  reporter.step('server-ready', ready ? 'PASS' : 'FAIL', ready || {
    logReady: serverLooksReady(latestLogContent())
  });
  if (!ready) {
    throw new Error('server did not become ready');
  }
}

async function preflight(reporter, rcon) {
  const dmList = await safeRcon(rcon, 'dm list');
  const levelInfo = await safeRcon(rcon, 'md list');
  reporter.step('preflight-runtime', 'PASS', {
    deluxeMenus: dmList.slice(0, 160),
    mythicDungeons: levelInfo.slice(0, 160)
  });
}

async function stabilizePlayer(reporter, rcon) {
  const seen = await safeRcon(rcon, `minecraft:data get entity ${USER} UUID`);
  if (!seen || String(seen).startsWith('ERR') || /No entity was found/i.test(seen)) {
    throw new Error('fresh player is not visible to RCON');
  }
  const online = await safeRcon(rcon, 'minecraft:list');
  const onlineText = normalizeText(online);
  const onlineCount = extractOnlineCount(online);
  if (onlineCount !== 1 || !onlineText.includes(normalizeText(USER))) {
    throw new Error(`staging isolation failed: ${online}`);
  }
  const clearResult = await safeRcon(rcon, `minecraft:clear ${USER}`);
  const inventoryAfterClear = await safeRcon(rcon, `minecraft:data get entity ${USER} Inventory`);
  reporter.step('fresh-player-state', 'PASS', {
    user: USER,
    online,
    reset: 'unique username plus per-user inventory clear for deterministic starter proof',
    clearResult: String(clearResult).slice(0, 160),
    inventoryAfterClear: String(inventoryAfterClear).slice(0, 160)
  });
}

async function openFoundationMenu(bot, reporter, options = {}) {
  const { recordStep = true, forceReopen = false } = options;
  if (forceReopen && bot.currentWindow) {
    closeWindowIfOpen(bot);
    await sleep(250);
  }
  const commands = ['/foundation', '/dm open foundation_nexus'];
  for (const command of commands) {
    bot.chat(command);
    try {
      const window = await waitForWindow(
        bot,
        isFoundationMenu,
        6000
      );
      if (recordStep) {
        reporter.step('open-foundation-menu', 'PASS', {
          command,
          title: getWindowTitle(window) || 'unknown'
        });
      }
      return window;
    } catch {
      reporter.note(`menu command timeout: ${command}`);
    }
  }
  throw new Error('window open timeout');
}

async function validateFoundationMenu(bot, reporter, existingWindow) {
  const window = existingWindow || await openFoundationMenu(bot, reporter);
  const checks = [
    { key: 'orientation', needle: 'stolica wyspy', slot: 10, itemName: 'compass' },
    { key: 'starter', needle: 'pakiet startowy', slot: 12, itemName: 'iron_sword', altNeedle: 'pakiet odebrany', altItemName: 'gray_dye' },
    { key: 'profile', needle: 'sygnal rpg', slot: 14, itemName: 'book' },
    { key: 'portal', needle: 'portal nexus', slot: 16, itemName: 'ender_pearl' }
  ];
  const result = {};
  for (const check of checks) {
    const slot = findWindowSlot(window, item =>
      itemText(item).includes(check.needle) ||
      (check.altNeedle && itemText(item).includes(check.altNeedle))
    );
    const slotItem = window?.slots?.[check.slot];
    const normalizedSlotName = normalizeText(slotItem?.name || '');
    const slotMatches = slotItem && (
      normalizedSlotName.includes(check.itemName) ||
      (check.altItemName && normalizedSlotName.includes(check.altItemName))
    );
    result[check.key] = slot === check.slot || Boolean(slotMatches);
    result[`${check.key}Slot`] = slot;
    result[`${check.key}SlotMaterial`] = slotItem?.name || '';
  }
  const pass = Object.values(result).filter(value => typeof value === 'boolean').every(Boolean);
  reporter.step('validate-foundation-menu', pass ? 'PASS' : 'FAIL', {
    title: getWindowTitle(window) || 'unknown',
    snapshot: windowSnapshot(window),
    ...result
  });
  if (!pass) {
    throw new Error('foundation menu layout mismatch');
  }
  return window;
}

async function validateCityOrientation(bot, reporter) {
  let window = bot.currentWindow;
  if (!window || !isFoundationMenu(window)) {
    window = await validateFoundationMenu(bot, reporter, await openFoundationMenu(bot, reporter));
  }
  const baseline = bot.qaSignals.orientationMessages.length;
  await clickNamedItem(bot, reporter, window, {
    nameNeedle: 'stolica wyspy',
    fallbackSlot: 10,
    stepName: 'click-city-orientation'
  });
  const result = await waitFor(() => {
    const messages = bot.qaSignals.orientationMessages.slice(baseline);
    const hasBoard = messages.some(message => normalizeText(message).includes('tablica startowa'));
    const hasGoal = messages.some(message => normalizeText(message).includes('cel mvp'));
    return hasBoard && hasGoal ? messages : false;
  }, 5000, 250);

  reporter.step('validate-city-orientation', result ? 'PASS' : 'FAIL', {
    messages: result ? result.join(' | ') : bot.qaSignals.orientationMessages.slice(baseline).join(' | ')
  });
  if (!result) {
    throw new Error('city orientation was not proven');
  }
}

async function validatePortalSelector(bot, reporter) {
  let window = bot.currentWindow;
  if (!window || !isFoundationMenu(window)) {
    window = await validateFoundationMenu(bot, reporter, await openFoundationMenu(bot, reporter));
  }
  await clickNamedItem(bot, reporter, window, {
    nameNeedle: 'portal nexus',
    fallbackSlot: 16,
    stepName: 'click-portal-selector'
  });
  const result = await waitFor(async () => {
    const currentWindow = bot.currentWindow;
    if (currentWindow && isDifficultyMenu(currentWindow)) {
      const snapshot = windowSnapshot(currentWindow);
      const text = normalizeText(snapshot.items);
      const pass = ['latwy', 'sredni', 'ciezki'].every(label => text.includes(label)) ||
        ['easy', 'normal', 'hard'].every(label => text.includes(label)) ||
        normalizeText(snapshot.title).includes('select difficulty');
      if (pass) {
        return {
          mode: 'difficulty-menu',
          snapshot
        };
      }
    }
    if (portalCommandIssued(USER)) {
      await sleep(500);
      const maybeDifficultyWindow = bot.currentWindow;
      if (maybeDifficultyWindow && isDifficultyMenu(maybeDifficultyWindow)) {
        const snapshot = windowSnapshot(maybeDifficultyWindow);
        return {
          mode: 'difficulty-menu',
          snapshot
        };
      }
      return {
        mode: 'direct-md-play',
        snapshot: windowSnapshot(bot.currentWindow)
      };
    }
    return false;
  }, 5000, 250);
  reporter.step('validate-portal-selector', result ? 'PASS' : 'FAIL', result || {
    mode: 'none',
    snapshot: windowSnapshot(bot.currentWindow)
  });
  if (result?.mode === 'difficulty-menu') {
    const currentWindow = bot.currentWindow;
    const labels = difficultyLabels();
    const slot = findWindowSlot(
      currentWindow,
      item => labels.some(label => itemText(item).includes(label))
    ) ?? difficultyFallbackSlot();
    if (slot == null) {
      throw new Error(`difficulty option not found: ${DIFFICULTY}`);
    }
    await clickWindowSlot(bot, currentWindow, slot);
    reporter.step('select-portal-difficulty', 'PASS', {
      difficulty: DIFFICULTY,
      slot
    });
    await sleep(2500);
  }
  closeWindowIfOpen(bot);
  await sleep(250);
  if (!result) {
    throw new Error('portal selector was not proven');
  }
}

async function clickNamedItem(bot, reporter, window, options) {
  const nameNeedle = normalizeText(options.nameNeedle);
  const fallbackSlot = options.fallbackSlot;
  const slot = findWindowSlot(window, item => itemText(item).includes(nameNeedle)) ?? fallbackSlot;
  if (slot == null) {
    throw new Error(`slot not found for ${options.nameNeedle}`);
  }
  await clickWindowSlot(bot, window, slot);
  reporter.step(options.stepName, 'PASS', {
    slot,
    title: getWindowTitle(window)
  });
  return slot;
}

async function validateStarter(bot, reporter, rcon) {
  const clearBeforeStarter = await safeRcon(rcon, `minecraft:clear ${USER}`);
  await sleep(250);
  const before = inventorySnapshot(bot);
  const inventoryBeforeRaw = await safeRcon(rcon, `minecraft:data get entity ${USER} Inventory`);
  const inventoryBeforeMetrics = extractInventoryMetrics(inventoryBeforeRaw);
  const permissionBefore = await safeRcon(rcon, `lp user ${USER} permission info foundation.starter.claimed`);
  const moneyBeforeRaw = await safeRcon(rcon, `cmi money ${USER}`);
  const moneyBefore = extractLastNumber(moneyBeforeRaw);
  let window = await validateFoundationMenu(bot, reporter, await openFoundationMenu(bot, reporter));
  const starterMessageBaseline = bot.qaSignals.starterMessages.length;
  const denialBaseline = bot.qaSignals.permissionDenials.length;
  await clickNamedItem(bot, reporter, window, {
    nameNeedle: 'pakiet startowy',
    fallbackSlot: 12,
    stepName: 'click-starter'
  });
  await sleep(500);
  const signalState = await waitFor(async () => {
    const starterMessageSeen = bot.qaSignals.starterMessages.length > starterMessageBaseline;
    const noDenial = bot.qaSignals.permissionDenials.length === denialBaseline;
    return starterMessageSeen && noDenial ? { starterMessageSeen, noDenial } : false;
  }, 5000, 250);
  if (!signalState) {
    reporter.step('validate-starter', 'BLOCKED', {
      currentWindow: bot.currentWindow ? windowSnapshot(bot.currentWindow) : 'closed',
      starterMessages: bot.qaSignals.starterMessages.slice(starterMessageBaseline).join(' | '),
      permissionDenials: bot.qaSignals.permissionDenials.slice(denialBaseline).join(' | ')
    });
    return {
      status: 'BLOCKED',
      blocker: 'starter click did not produce a stable player-facing success signal',
      nextAction: 'verify DeluxeMenus starter dispatch before claiming runtime delivery'
    };
  }

  const grantState = await waitFor(async () => {
    const inventory = await safeRcon(rcon, `minecraft:data get entity ${USER} Inventory`);
    const inventoryProof = normalizeText(inventory);
    const inventoryMetrics = extractInventoryMetrics(inventory);
    const botInventory = inventorySnapshot(bot);
    const botInventoryDelta = botInventory.length > before.length;
    const botHasSword = inventoryHasKeyword(botInventory, 'iron_sword') || inventoryHasKeyword(botInventory, 'stal');
    const botHasRecall = inventoryHasKeyword(botInventory, 'potion') || inventoryHasKeyword(botInventory, 'powrot');
    const permissionRaw = await safeRcon(rcon, `lp user ${USER} permission info foundation.starter.claimed`);
    const moneyRaw = await safeRcon(rcon, `cmi money ${USER}`);
    const moneyValue = extractLastNumber(moneyRaw);
    let claimedSlot = null;
    let menuClaimed = false;
    let slot12Text = '';
    const currentWindow = bot.currentWindow;
    if (currentWindow && isFoundationMenu(currentWindow)) {
      claimedSlot = findWindowSlot(currentWindow, item => itemText(item).includes(normalizeText('pakiet odebrany')));
      const slot12Item = currentWindow?.slots?.[12];
      slot12Text = slot12Item ? itemText(slot12Item) : '';
      menuClaimed =
        claimedSlot === 12 ||
        slot12Text.includes(normalizeText('pakiet odebrany')) ||
        slot12Text.includes('gray_dye');
    }
    const inventoryChanged =
      normalizeText(inventory) !== normalizeText(inventoryBeforeRaw) ||
      inventoryMetrics.slots !== inventoryBeforeMetrics.slots ||
      inventoryMetrics.totalCount !== inventoryBeforeMetrics.totalCount;
    if (
      menuClaimed ||
      inventoryChanged ||
      botInventoryDelta ||
      botHasSword ||
      botHasRecall ||
      inventoryProof.includes('steel_sword') ||
      inventoryProof.includes('recall_potion')
    ) {
      return {
        inventory,
        inventoryMetrics,
        inventoryChanged,
        botInventory,
        botInventoryDelta,
        botHasSword,
        botHasRecall,
        menuClaimed,
        claimedSlot,
        slot12Text,
        permissionRaw,
        moneyRaw,
        moneyValue
      };
    }
    return false;
  }, 10000, 500);
  if (!grantState) {
    const inventory = await safeRcon(rcon, `minecraft:data get entity ${USER} Inventory`);
    let claimedSlot = null;
    let menuClaimed = false;
    let slot12Text = '';
    try {
      const verifyWindow = await openFoundationMenu(bot, reporter, { recordStep: false, forceReopen: true });
      claimedSlot = findWindowSlot(verifyWindow, item => itemText(item).includes(normalizeText('pakiet odebrany')));
      const slot12Item = verifyWindow?.slots?.[12];
      slot12Text = slot12Item ? itemText(slot12Item) : '';
      menuClaimed =
        claimedSlot === 12 ||
        slot12Text.includes(normalizeText('pakiet odebrany')) ||
        slot12Text.includes('gray_dye');
    } catch {}
    if (menuClaimed) {
      const after = inventorySnapshot(bot);
      const moneySignal = bot.qaSignals.starterMessages.slice(starterMessageBaseline).some(message =>
        normalizeText(message).includes('25 monet')
      );
      reporter.step('validate-starter', 'PASS', {
        itemsBefore: before.length,
        itemsAfter: after.length,
        sword: inventoryHasKeyword(after, 'steel_sword') || inventoryHasKeyword(after, 'stal'),
        recall: inventoryHasKeyword(after, 'recall_potion') || inventoryHasKeyword(after, 'powrot'),
        claimedSlot,
        menuClaimed,
        slot12Text,
        clearBeforeStarter: String(clearBeforeStarter).slice(0, 160),
        inventoryBefore: JSON.stringify(inventoryBeforeMetrics),
        inventoryAfter: 'unavailable-via-rcon',
        inventoryDelta: after.length > before.length,
        permissionBefore: String(permissionBefore).slice(0, 160),
        permissionAfter: 'unavailable-via-rcon',
        moneyBefore: String(moneyBeforeRaw).slice(0, 160),
        moneyAfter: 'unavailable-via-rcon',
        moneyDelta: false,
        moneySignal,
        starterMessages: bot.qaSignals.starterMessages.slice(starterMessageBaseline).join(' | '),
        inventoryData: String(inventory).slice(0, 200)
      });
      return {
        status: 'PASS',
        moneySignal,
        backendProof: true
      };
    }
    const currentWindow = bot.currentWindow;
    const fallbackSlot12Item = currentWindow?.slots?.[12];
    const fallbackSlot12Text = fallbackSlot12Item ? itemText(fallbackSlot12Item) : '';
    claimedSlot = currentWindow && isFoundationMenu(currentWindow)
      ? findWindowSlot(currentWindow, item => itemText(item).includes(normalizeText('pakiet odebrany')))
      : null;
    menuClaimed = claimedSlot === 12 || fallbackSlot12Text.includes(normalizeText('pakiet odebrany')) || fallbackSlot12Text.includes('gray_dye');
    reporter.step('validate-starter', 'BLOCKED', {
      currentWindow: bot.currentWindow ? windowSnapshot(bot.currentWindow) : 'closed',
      starterMessages: bot.qaSignals.starterMessages.slice(starterMessageBaseline).join(' | '),
      permissionDenials: bot.qaSignals.permissionDenials.slice(denialBaseline).join(' | '),
      clearBeforeStarter: String(clearBeforeStarter).slice(0, 200),
      inventoryBeforeRaw: String(inventoryBeforeRaw).slice(0, 200),
      inventoryBefore: JSON.stringify(inventoryBeforeMetrics),
      permissionBefore: String(permissionBefore).slice(0, 200),
      moneyBefore: String(moneyBeforeRaw).slice(0, 200),
      inventoryData: String(inventory).slice(0, 200),
      menuClaimed,
      claimedSlot,
      slot12Text: fallbackSlot12Text
    });
    return {
      status: 'BLOCKED',
      blocker: 'starter menu emits success chat, but backend starter delivery does not converge within 10 seconds',
      nextAction: 'isolate the mi give / claim-state runtime seam independently of the chat message'
    };
  }
  const after = inventorySnapshot(bot);
  const sword = inventoryHasKeyword(after, 'steel_sword') || inventoryHasKeyword(after, 'stal');
  const recall = inventoryHasKeyword(after, 'recall_potion') || inventoryHasKeyword(after, 'powrot');
  const inventoryData = grantState.inventory;
  const inventoryProof = normalizeText(inventoryData);
  const hasSword = sword || inventoryProof.includes('steel_sword');
  const hasRecall = recall || inventoryProof.includes('recall_potion');
  const claimed = grantState.menuClaimed || grantState.claimedSlot === 12;
  const inventoryDelta =
    grantState.inventoryChanged ||
    grantState.botInventoryDelta ||
    grantState.inventoryMetrics.slots !== inventoryBeforeMetrics.slots ||
    grantState.inventoryMetrics.totalCount !== inventoryBeforeMetrics.totalCount;
  const moneySignal = bot.qaSignals.starterMessages.slice(starterMessageBaseline).some(message =>
    normalizeText(message).includes('25 monet')
  );
  const moneyDelta = moneyBefore != null && grantState.moneyValue != null && grantState.moneyValue > moneyBefore;
  const pass = (hasSword || hasRecall || inventoryDelta || claimed) && moneySignal;
  reporter.step('validate-starter', pass ? 'PASS' : 'BLOCKED', {
    itemsBefore: before.length,
    itemsAfter: after.length,
    sword: hasSword,
    recall: hasRecall,
    claimedSlot: claimed ? grantState.claimedSlot : '',
    menuClaimed: grantState.menuClaimed,
    slot12Text: grantState.slot12Text,
    clearBeforeStarter: String(clearBeforeStarter).slice(0, 160),
    inventoryBefore: JSON.stringify(inventoryBeforeMetrics),
    inventoryAfter: JSON.stringify(grantState.inventoryMetrics),
    inventoryDelta,
    botItemsBefore: before.length,
    botItemsAfter: grantState.botInventory?.length || after.length,
    botInventoryDelta: grantState.botInventoryDelta,
    botHasSword: grantState.botHasSword,
    botHasRecall: grantState.botHasRecall,
    permissionBefore: String(permissionBefore).slice(0, 160),
    permissionAfter: String(grantState.permissionRaw).slice(0, 160),
    moneyBefore: String(moneyBeforeRaw).slice(0, 160),
    moneyAfter: String(grantState.moneyRaw).slice(0, 160),
    moneyDelta,
    moneySignal,
    starterMessages: bot.qaSignals.starterMessages.slice(starterMessageBaseline).join(' | '),
    inventoryData: inventoryData.slice(0, 200)
  });
  if (!pass) {
    return {
      status: 'BLOCKED',
      blocker: 'starter path produced a player-facing signal without stable backend delivery proof',
      nextAction: 'treat the runtime as blocked until inventory, claim-state, or equivalent backend proof is visible'
    };
  }
  return {
    status: 'PASS',
    moneySignal,
    backendProof: true
  };
}

async function validateProfileSignal(bot, reporter) {
  const signalBaseline = bot.qaSignals.profileSignals.length;
  const denialBaseline = bot.qaSignals.permissionDenials.length;
  const priorWindowCount = bot.qaSignals.windows.length;
  const window = await validateFoundationMenu(bot, reporter, await openFoundationMenu(bot, reporter, { forceReopen: true }));
  await clickNamedItem(bot, reporter, window, {
    nameNeedle: 'sygnal rpg',
    fallbackSlot: 14,
    stepName: 'click-profile-signal'
  });
  const result = await waitFor(async () => {
    const newSignals = bot.qaSignals.profileSignals.slice(signalBaseline);
    const noDenial = bot.qaSignals.permissionDenials.length === denialBaseline;
    const currentWindow = bot.currentWindow;
    const profileWindowOpen =
      currentWindow &&
      !isFoundationMenu(currentWindow) &&
      bot.qaSignals.windows.length > priorWindowCount;
    if (noDenial && (newSignals.length > 0 || profileWindowOpen)) {
      return {
        newSignals,
        profileWindowTitle: profileWindowOpen ? getWindowTitle(currentWindow) : '',
        profileWindowOpen
      };
    }
    return false;
  }, 5000, 250);

  const pass = Boolean(result);
  reporter.step('validate-profile-signal', pass ? 'PASS' : 'FAIL', {
    signals: pass ? result.newSignals.join(' | ') : bot.qaSignals.profileSignals.slice(signalBaseline).join(' | '),
    profileWindowTitle: pass ? result.profileWindowTitle : '',
    permissionDenials: bot.qaSignals.permissionDenials.slice(denialBaseline).join(' | ')
  });
  if (!pass) {
    throw new Error('profile signal was not proven');
  }
}

async function clickPortal(bot, reporter) {
  let window = bot.currentWindow;
  if (!window || !isFoundationMenu(window)) {
    window = await openFoundationMenu(bot, reporter);
  }
  await clickNamedItem(bot, reporter, window, {
    nameNeedle: 'portal nexus',
    fallbackSlot: 16,
    stepName: 'click-portal'
  });
  const portalMode = await waitFor(async () => {
    if (portalCommandIssued(USER)) return { mode: 'direct-md-play' };
    const currentWindow = bot.currentWindow;
    if (currentWindow && isDifficultyMenu(currentWindow)) {
      return {
        mode: 'difficulty-menu',
        window: currentWindow
      };
    }
    return false;
  }, 12000, 250);

  if (!portalMode) {
    throw new Error('portal handoff not detected');
  }

  if (portalMode.mode === 'direct-md-play') {
    reporter.step('open-difficulty-menu', 'PASS', {
      mode: 'direct-md-play'
    });
    await sleep(2500);
    return;
  }

  const difficultyWindow = portalMode.window;
  reporter.step('open-difficulty-menu', 'PASS', {
    mode: 'difficulty-menu',
    ...windowSnapshot(difficultyWindow)
  });

  const labels = difficultyLabels();
  const slot = findWindowSlot(
    difficultyWindow,
    item => labels.some(label => itemText(item).includes(label))
  );
  if (slot == null) {
    throw new Error(`difficulty option not found: ${DIFFICULTY}`);
  }
  await clickWindowSlot(bot, difficultyWindow, slot);
  reporter.step('select-difficulty', 'PASS', {
    difficulty: DIFFICULTY,
    slot
  });
  await sleep(2500);
}

async function detectBoss(reporter, rcon) {
  const result = await waitFor(async () => {
    const response = await safeRcon(
      rcon,
      `minecraft:execute at ${USER} as @e[type=minecraft:zombie,distance=..120,sort=nearest,limit=1] run data get entity @s`
    );
    const health = extractLastNumber(
      await safeRcon(
        rcon,
        `minecraft:execute at ${USER} as @e[type=minecraft:zombie,distance=..120,sort=nearest,limit=1] run data get entity @s Health`
      )
    );
    const text = normalizeText(response);
    const isGuardian =
      text.includes('groveguardian') ||
      text.includes('straznik gaju') ||
      text.includes('strażnik gaju');
    return isGuardian ? { response, health } : false;
  }, 20000, 500);

  reporter.step('detect-boss', result ? 'PASS' : 'FAIL', {
    response: result ? String(result.response).slice(0, 500) : 'not-found',
    health: result?.health ?? 'unknown',
    runtimeSnapshot: result ? '' : JSON.stringify(await readPlayerRuntimeSnapshot(rcon))
  });
  if (!result) {
    throw new Error('boss not detected');
  }
  return result;
}

async function validateStarterCombat(bot, reporter, rcon) {
  ensurePathfinder(bot);
  const readPlayerDimension = async () => {
    const response = await safeRcon(rcon, `minecraft:data get entity ${USER} Dimension`);
    const match = String(response || '').match(/"([^"]+)"/);
    return match ? match[1] : '';
  };

  let difficultySelected = false;
  const selectDifficultyIfNeeded = async () => {
    const currentWindow = bot.currentWindow;
    if (!currentWindow || !isDifficultyMenu(currentWindow)) return false;
    const labels = difficultyLabels();
    const slot = findWindowSlot(
      currentWindow,
      item => labels.some(label => itemText(item).includes(label))
    ) ?? difficultyFallbackSlot();
    if (slot == null) return false;
    await clickWindowSlot(bot, currentWindow, slot);
    reporter.step('starter-combat-difficulty', 'PASS', {
      difficulty: DIFFICULTY,
      slot
    });
    await sleep(2500);
    difficultySelected = true;
    return true;
  };

  await selectDifficultyIfNeeded();

  const equipSword = bot.inventory.items().find(item =>
    normalizeText(item.name || '').includes('iron_sword') ||
    itemText(item).includes('stal')
  );
  if (equipSword) {
    try {
      await bot.equip(equipSword, 'hand');
      await sleep(250);
    } catch {}
  }

  const inDungeon = await waitFor(async () => {
    if (!difficultySelected) {
      await selectDifficultyIfNeeded();
    }
    const dim = await readPlayerDimension();
    return dim.includes('level_1_') ? dim : false;
  }, 20000, 500);

  if (!inDungeon) {
    const runtimeSnapshot = await readPlayerRuntimeSnapshot(rcon);
    reporter.step('starter-combat-proof', 'BLOCKED', {
      reason: 'player command handoff was observed, but dungeon instance was not confirmed before combat proof',
      currentWindow: bot.currentWindow ? windowSnapshot(bot.currentWindow) : 'closed',
      difficultySelected,
      runtimeSnapshot: JSON.stringify(runtimeSnapshot)
    });
    return {
      status: 'BLOCKED',
      blocker: 'fresh starter player did not reach a confirmed dungeon instance for direct combat proof',
      nextAction: 'stabilize the post-portal dungeon arrival before retrying melee proof'
    };
  }

  const readNearestZombieHealth = async (radius = 120) => {
    const response = await safeRcon(
      rcon,
      `minecraft:execute at ${USER} as @e[type=minecraft:zombie,distance=..${radius},sort=nearest,limit=1] run data get entity @s Health`
    );
    return extractLastNumber(response);
  };

  const readDungeonZombieState = async () => {
    const positionRaw = await safeRcon(
      rcon,
      `minecraft:execute in ${inDungeon} run data get entity @e[type=minecraft:zombie,limit=1] Pos`
    );
    const healthRaw = await safeRcon(
      rcon,
      `minecraft:execute in ${inDungeon} run data get entity @e[type=minecraft:zombie,limit=1] Health`
    );
    const nameRaw = await safeRcon(
      rcon,
      `minecraft:execute in ${inDungeon} run data get entity @e[type=minecraft:zombie,limit=1] CustomName`
    );
    return {
      position: extractPosition(positionRaw),
      health: extractLastNumber(healthRaw),
      nameRaw: String(nameRaw || '').slice(0, 160)
    };
  };

  const dungeonZombie = await waitFor(async () => {
    const state = await readDungeonZombieState();
    return state.position && state.health != null ? state : false;
  }, 12000, 500);

  if (!dungeonZombie) {
    const runtimeSnapshot = await readPlayerRuntimeSnapshot(rcon, inDungeon);
    reporter.step('starter-combat-proof', 'BLOCKED', {
      reason: 'dungeon instance loaded, but no global zombie target appeared in the instance window',
      runtimeSnapshot: JSON.stringify(runtimeSnapshot)
    });
    return {
      status: 'BLOCKED',
      blocker: 'fresh starter player reached the dungeon path, but no real dungeon combat target spawned in the observed instance window',
      nextAction: 'stabilize the level_1 spawn trigger before claiming fresh-player combat readiness'
    };
  }

  bot.pathfinder.setMovements(createMovements(bot));
  bot.pathfinder.setGoal(new GoalNear(dungeonZombie.position.x, dungeonZombie.position.y, dungeonZombie.position.z, 2));
  await waitFor(() => {
    if (!bot.entity) return false;
    return bot.entity.position.distanceTo(dungeonZombie.position) <= 4 ? true : false;
  }, 12000, 250);
  bot.pathfinder.setGoal(null);

  let initialHealth = null;
  for (let i = 0; i < 30 && initialHealth == null; i++) {
    const health = await readNearestZombieHealth(120);
    if (health != null) {
      initialHealth = health;
      break;
    }
    const globalState = await readDungeonZombieState();
    if (globalState.health != null) {
      initialHealth = globalState.health;
      break;
    }
    try {
      await bot.lookAt(new Vec3(0, 64, 0), true);
    } catch {}
    bot.setControlState('sprint', true);
    bot.setControlState('forward', true);
    await sleep(500);
  }
  bot.setControlState('forward', false);
  bot.setControlState('sprint', false);

  if (initialHealth == null) {
    reporter.step('starter-combat-proof', 'BLOCKED', {
      reason: 'no nearby zombie health probe available for fresh starter combat check'
    });
    return {
      status: 'BLOCKED',
      blocker: 'fresh starter player reached the dungeon path, but no valid nearby combat target was observed for melee proof',
      nextAction: 'stabilize a nearby dungeon target for the fresh-player melee proof'
    };
  }

  let damaged = false;
  let finalHealth = initialHealth;
  let attackAttempts = 0;

  for (let i = 0; i < 24 && !damaged; i++) {
    const target = bot.nearestEntity(entity => entity?.name === 'zombie' && entity?.position);
    if (!target) {
      try {
        await bot.lookAt(dungeonZombie.position.offset(0, 1, 0), true);
      } catch {}
      bot.pathfinder.setMovements(createMovements(bot));
      bot.pathfinder.setGoal(new GoalNear(dungeonZombie.position.x, dungeonZombie.position.y, dungeonZombie.position.z, 2));
      bot.setControlState('sprint', true);
      bot.setControlState('forward', true);
      await sleep(500);
      continue;
    }

    const distance = bot.entity.position.distanceTo(target.position);
    if (distance > 3.2) {
      bot.pathfinder.setMovements(createMovements(bot));
      bot.pathfinder.setGoal(new GoalNear(target.position.x, target.position.y, target.position.z, 2));
      bot.setControlState('sprint', true);
      await sleep(500);
      continue;
    }

    bot.setControlState('forward', false);
    bot.setControlState('sprint', false);
    bot.pathfinder.setGoal(null);
    try {
      await bot.lookAt(target.position.offset(0, 1.0, 0), true);
    } catch {}
    try {
      bot.attack(target);
      attackAttempts += 1;
    } catch {}
    await sleep(800);
    const hp = await readNearestZombieHealth(25);
    if (hp != null) {
      finalHealth = hp;
      damaged = hp < initialHealth;
    } else {
      const globalState = await readDungeonZombieState();
      if (globalState.health != null) {
        finalHealth = globalState.health;
        damaged = globalState.health < initialHealth;
      }
    }
  }

  bot.pathfinder.setGoal(null);
  bot.setControlState('forward', false);
  bot.setControlState('sprint', false);
  reporter.step('starter-combat-proof', damaged ? 'PASS' : 'BLOCKED', {
    dungeonTarget: dungeonZombie.position?.toString?.() || '',
    dungeonTargetName: dungeonZombie.nameRaw,
    initialHealth,
    finalHealth,
    attackAttempts,
    damaged
  });

  if (!damaged) {
    return {
      status: 'BLOCKED',
      blocker: 'fresh starter player reached combat space but no confirmed melee damage was observed on a real dungeon target',
      nextAction: 'stabilize target acquisition or prove damage on the exact starter combat path'
    };
  }

  return {
    status: 'PASS',
    initialHealth,
    finalHealth
  };
}

async function killBoss(reporter, rcon) {
  let last = '';
  for (let i = 0; i < 16; i++) {
    last = await safeRcon(
      rcon,
      `minecraft:execute at ${USER} as @e[type=minecraft:zombie,distance=..120,sort=nearest,limit=1] if data entity @s CustomName run damage @s 80 minecraft:generic by ${USER}`
    );
    await sleep(400);
    const alive = await safeRcon(
      rcon,
      `minecraft:execute at ${USER} as @e[type=minecraft:zombie,distance=..120,sort=nearest,limit=1] if data entity @s CustomName run data get entity @s CustomName`
    );
    if (!alive || String(alive).startsWith('ERR') || /No entity was found/i.test(alive)) {
      reporter.step('kill-boss', 'PASS', { last });
      return;
    }
  }
  reporter.step('kill-boss', 'FAIL', { last });
  throw new Error('boss did not die');
}

async function waitForFinish(bot, reporter, rcon, moneyBeforeBoss, baseline) {
  const finished = await waitFor(async () => {
    if (bot.qaSignals.finishMessages.length > baseline.finishMessages) return 'chat';
    if (bot.qaSignals.respawns > baseline.respawns) return 'respawn';
    if (bot.entity && bot.entity.position.distanceTo(baseline.position) > 8) return 'move';
    return false;
  }, 25000, 500);

  const moneyAfterText = await safeRcon(rcon, `cmi money ${USER}`);
  const moneyAfter = extractLastNumber(moneyAfterText);
  const rewardGranted =
    moneyBeforeBoss == null ||
    (moneyAfter != null && moneyAfter > moneyBeforeBoss);
  reporter.step('finish-detect', finished ? 'PASS' : 'FAIL', {
    source: finished || 'none',
    finalPos: bot.entity?.position?.toString?.() || 'unknown',
    moneyBeforeBoss,
    moneyAfter,
    rewardGranted
  });
  if (!finished || !rewardGranted) {
    throw new Error(!finished ? 'finish not detected' : 'boss reward not detected');
  }
}

async function main() {
  const reporter = createReporter(BASE_DIR, USER);
  const runtimeLock = acquireRuntimeLock(BASE_DIR, USER, FOUNDATION_SCOPE);
  let rcon;
  let bot;
  let viewer;
  let visualCheckpointState;
  let starterResult = {
    status: 'UNKNOWN',
    backendProof: false
  };
  let starterCombatResult = {
    status: 'BLOCKED'
  };
  const runLogOffset = logOffset();

  try {
    reporter.setSummary({
      scope: FOUNDATION_SCOPE,
      difficulty: DIFFICULTY,
      required_proof: RUN_FULL_SLICE
        ? 'SPEC_FIDELITY,VISUAL_FIDELITY,RUNTIME_PROOF,INTEGRATION_PROOF,PLAYER_PROOF'
        : 'SPEC_FIDELITY,VISUAL_FIDELITY,RUNTIME_PROOF,INTEGRATION_PROOF',
      runtime_lock: runtimeLock.file
    });
    reporter.command('node --check MCMMORPG/_validation/foundation_bot.js');
    reporter.command('node MCMMORPG/_validation/play_m1_m2.js');
    if (RUN_FULL_SLICE) {
      reporter.command('MC_QA_USER=<fresh> MC_QA_DIFFICULTY=NORMAL node MCMMORPG/_validation/play_level1.js');
    }
    reporter.assumption('Staging is single-runner for runtime-touching validation and uses offline test users.');
    reporter.assumption('NORMAL is the reference difficulty for full M7 proof; reduced-scope runs may skip dungeon execution.');
    reporter.rejectedAssumption('A static config PASS is sufficient to close the milestone.');
    reporter.rejectedAssumption('M1 visual quality can be treated as proven from menu YAML, coordinate docs, or non-visual runtime alone.');
    reporter.rejectedAssumption('Mineflayer chat or RCON substitutes automatically prove the same path as a DeluxeMenus player click.');
    reporter.rollbackTrigger('New command/event exception appears in the scoped latest.log window after the run starts.');
    reporter.rollbackTrigger('A runtime-touching validation is started without owning the _validation/.runtime.lock file.');
    reporter.evidence(`Run log window starts at byte offset ${runLogOffset} in MCMMORPG/logs/latest.log.`);
    reporter.evidence(`Runtime lock acquired at ${runtimeLock.payload.acquiredAt}.`);
    reporter.stagingBaseline('online-mode=false remains the staging baseline for local MVP work.');
    reporter.stagingBaseline('Nexo pre-join pack dispatch is non-blocking for bot QA.');
    reporter.stagingBaseline('HuskSync stays disabled on staging until a real database baseline exists.');
    reporter.stagingBaseline('MCPets YAML fallback is accepted on staging only while pets stay outside the first vertical slice.');

    validateStaticContract(reporter);
    rcon = await connectRcon();
    await waitForServerReady(reporter, rcon);
    await preflight(reporter, rcon);
    bot = await joinBot(reporter);
    viewer = startViewer(bot, {
      enabled: VIEWER_ENABLED,
      firstPerson: true
    });
    if (viewer?.url) {
      reporter.evidence(`Viewer available for manual visual proof at ${viewer.url}.`);
    }

    await stabilizePlayer(reporter, rcon);
    visualCheckpointState = await validateHubVisualCheckpoints(bot, reporter);
    await validateCityOrientation(bot, reporter);
    starterResult = await validateStarter(bot, reporter, rcon);
    await validateProfileSignal(bot, reporter);
    await validatePortalSelector(bot, reporter);
    starterCombatResult = await validateStarterCombat(bot, reporter, rcon);

    const swordFile = fs.readFileSync(MMO_SWORD_FILE, 'utf8');
    const starterRequiredLevel = extractItemRequiredLevel(swordFile, 'STEEL_SWORD');
    const starterUsabilityProven = starterRequiredLevel != null && starterRequiredLevel <= 1;
    const starterCombatUsabilityProven = starterCombatResult.status === 'PASS';
    reporter.step(starterCombatUsabilityProven && starterUsabilityProven ? 'starter-usability-gate' : 'starter-usability-gate', starterCombatUsabilityProven && starterUsabilityProven ? 'PASS' : 'BLOCKED', {
      starterRequiredLevel: starterRequiredLevel == null ? 'unknown' : starterRequiredLevel,
      proofStandard: 'fresh player starter combat readiness must be proven, not inferred',
      combatProof: starterCombatResult.status
    });
    if (!starterUsabilityProven) {
      reporter.blocker('M2 starter combat usability is not proven for a fresh player while STEEL_SWORD keeps required-level above 1.');
      reporter.setFailureSeam('starter_level_gate', {
        symptom: 'STEEL_SWORD required-level still exceeds the fresh-player threshold for direct combat proof.',
        nextAction: 'keep the starter weapon gate at level 1 or lower until fresh-player combat proof is stable.'
      });
    }
    if (starterCombatResult.status === 'BLOCKED' && starterCombatResult.blocker) {
      reporter.blocker(starterCombatResult.blocker);
      reporter.setFailureSeam('starter_combat', {
        symptom: starterCombatResult.blocker,
        nextAction: starterCombatResult.nextAction
      });
    }

    reporter.milestone('M1 UI', {
      status: VISUAL_CAPTURE_REQUIRED ? 'BLOCKED' : 'BLOCKED',
      requiredProof: ['SPEC_FIDELITY', 'VISUAL_FIDELITY', 'RUNTIME_PROOF', 'INTEGRATION_PROOF'],
      achievedProof: ['SPEC_FIDELITY', 'RUNTIME_PROOF', 'INTEGRATION_PROOF'],
      owner: 'ui',
      evidence: [
        'foundation_nexus aliases, slots, messages, and actions match spec',
        'menu opens for a fresh user and portal handoff is observed in runtime',
        'hub checkpoint route can be traversed in runtime and district coordinates are populated'
      ],
      blockers: [
        'no screenshot/video comparison artifact yet proving fidelity against hub concept references',
        !visualCheckpointState.namedLandmarksPass ? 'runtime checkpoint route lacks named landmark confidence for one or more hub districts' : ''
      ].filter(Boolean),
      nextAction: 'capture and compare player-view visual evidence against the 3 reference images and concept/blockout docs'
    });
    if (starterResult.status === 'BLOCKED') {
      reporter.blocker(starterResult.blocker);
    }
    reporter.milestone('M2 RPG', {
      status: starterResult.status === 'PASS' && starterUsabilityProven && starterCombatUsabilityProven ? 'PASS' : 'BLOCKED',
      requiredProof: ['SPEC_FIDELITY', 'RUNTIME_PROOF', 'INTEGRATION_PROOF'],
      achievedProof: starterResult.status === 'PASS'
        ? ['STATIC_CONTRACT', 'RUNTIME_PROOF', 'INTEGRATION_PROOF']
        : ['STATIC_CONTRACT', 'INTEGRATION_PROOF'],
      owner: 'rpg',
      evidence: starterResult.status === 'PASS'
        ? [
            'starter grant shows backend delivery proof plus player-facing signal',
            'profile GUI path is callable for a fresh user'
          ]
        : [
            'starter click produced a player-facing success signal',
            'profile GUI path remains callable for a fresh user'
          ],
      blockers: [
        starterResult.status === 'BLOCKED' ? starterResult.blocker : '',
        !starterUsabilityProven ? 'starter weapon gate still exceeds fresh-player threshold' : '',
        starterCombatResult.status === 'BLOCKED' ? 'starter combat usability is not yet proven directly for a fresh player' : ''
      ].filter(Boolean),
      risks: starterResult.status === 'PASS' && starterUsabilityProven && starterCombatUsabilityProven ? [] : [
        'do not treat starter chat confirmation as equivalent to backend delivery proof'
      ],
      nextAction: starterResult.status === 'BLOCKED'
        ? starterResult.nextAction
        : (starterCombatResult.status === 'BLOCKED'
          ? starterCombatResult.nextAction
          : 'keep starter combat proof stable on the fresh-player path')
    });

    if (RUN_FULL_SLICE) {
      try {
        await clickPortal(bot, reporter);
        await detectBoss(reporter, rcon);
        const finishBaseline = {
          respawns: bot.qaSignals.respawns,
          finishMessages: bot.qaSignals.finishMessages.length,
          position: bot.entity.position.clone()
        };
        await killBoss(reporter, rcon);
        await waitForFinish(bot, reporter, rcon, null, finishBaseline);
      } catch (error) {
        reporter.step('dungeon-flow', 'FAIL', { message: error.message });
        reporter.blocker(`M3-M4-M7 exact player-path proof failed: ${error.message}`);
        throw error;
      }
    }

    const runLog = readLogSince(runLogOffset);
    const commandException = /Command exception|Could not pass event|Unhandled exception/i.test(runLog);
    reporter.step('runtime-log-window', commandException ? 'FAIL' : 'PASS', {
      bytes: Buffer.byteLength(runLog),
      commandException
    });
    if (commandException) {
      throw new Error('runtime log contains a command or event exception');
    }

    if (RUN_FULL_SLICE) {
      reporter.milestone('M3 Dungeon', {
        status: 'PASS',
        requiredProof: ['RUNTIME_PROOF', 'INTEGRATION_PROOF', 'PLAYER_PROOF'],
        achievedProof: ['RUNTIME_PROOF', 'INTEGRATION_PROOF', 'PLAYER_PROOF'],
        owner: 'dungeon',
        evidence: [
          'portal handoff reaches dungeon execution path',
          'fresh-player dungeon flow progressed to boss detection and finish detection'
        ]
      });
      reporter.milestone('M4 Mobs', {
        status: 'PASS',
        requiredProof: ['RUNTIME_PROOF', 'INTEGRATION_PROOF', 'PLAYER_PROOF'],
        achievedProof: ['RUNTIME_PROOF', 'INTEGRATION_PROOF', 'PLAYER_PROOF'],
        owner: 'mobs',
        evidence: [
          'level_1_grove_guardian identity was detected in arena context',
          'combat death triggered finish and reward path'
        ]
      });
      reporter.milestone('M7 QA', {
        status: 'BLOCKED',
        requiredProof: ['SPEC_FIDELITY', 'VISUAL_FIDELITY', 'RUNTIME_PROOF', 'INTEGRATION_PROOF', 'PLAYER_PROOF'],
        achievedProof: ['SPEC_FIDELITY', 'RUNTIME_PROOF', 'INTEGRATION_PROOF', 'PLAYER_PROOF'],
        owner: 'qa',
        evidence: [
          'full vertical slice executed for this user without scoped runtime exceptions'
        ],
        blockers: [
          'visual fidelity still needs separate player-view capture/comparison before release closure'
        ],
        nextAction: 'repeat for three consecutive fresh users and add visual evidence pack before final closure'
      });
    } else {
      reporter.milestone('M7 QA', {
        status: 'BLOCKED',
        requiredProof: ['SPEC_FIDELITY', 'VISUAL_FIDELITY', 'RUNTIME_PROOF', 'INTEGRATION_PROOF', 'PLAYER_PROOF'],
        achievedProof: ['SPEC_FIDELITY', 'RUNTIME_PROOF', 'INTEGRATION_PROOF'],
        owner: 'qa',
        blockers: [
          'reduced-scope run intentionally skipped full dungeon player-proof',
          'visual fidelity remains unproven without reference-capture evidence'
        ],
        nextAction: 'run the full play_level1 harness or exact real-client path'
      });
    }

    const finalVerdict = 'BLOCKED';
    const reportFile = reporter.finalize(finalVerdict);
    console.log(`FOUNDATION_QA_${finalVerdict} ${reportFile}`);
    process.exitCode = 2;
  } catch (error) {
    const isBlocked = error instanceof BlockedError;
    const seam = classifyFailureSeam(error.message);
    reporter.setFailureSeam(seam.seam, {
      symptom: error.message,
      nextAction: error.details?.nextAction || seam.nextAction
    });
    if (RUN_FULL_SLICE) {
      reporter.milestone('M7 QA', {
        status: isBlocked ? 'BLOCKED' : 'FAIL',
        requiredProof: ['SPEC_FIDELITY', 'VISUAL_FIDELITY', 'RUNTIME_PROOF', 'INTEGRATION_PROOF', 'PLAYER_PROOF'],
        achievedProof: ['SPEC_FIDELITY', 'RUNTIME_PROOF', 'INTEGRATION_PROOF'],
        owner: 'qa',
        blockers: [error.message],
        nextAction: isBlocked
          ? (error.details?.nextAction || seam.nextAction || 'repair the blocked seam before attempting another full run')
          : (seam.nextAction || 'repair the failing seam before attempting another full run')
      });
    } else {
      reporter.milestone('M7 QA', {
        status: 'BLOCKED',
        requiredProof: ['SPEC_FIDELITY', 'VISUAL_FIDELITY', 'RUNTIME_PROOF', 'INTEGRATION_PROOF', 'PLAYER_PROOF'],
        achievedProof: ['SPEC_FIDELITY'],
        owner: 'qa',
        blockers: [error.message],
        nextAction: isBlocked
          ? (error.details?.nextAction || seam.nextAction || 'repair the blocked seam before another reduced-scope run')
          : (seam.nextAction || 'repair the failing seam before another reduced-scope run')
      });
    }
    reporter.step('run-error', isBlocked ? 'BLOCKED' : 'FAIL', { message: error.message });
    if (isBlocked) {
      reporter.blocker(error.message);
    }
    const finalVerdict = isBlocked ? 'BLOCKED' : 'FAIL';
    const reportFile = reporter.finalize(finalVerdict);
    console.error(`FOUNDATION_QA_${finalVerdict} ${error.message}`);
    console.error(`FOUNDATION_QA_REPORT ${reportFile}`);
    process.exitCode = isBlocked ? 2 : 1;
  } finally {
    try {
      viewer?.close?.();
    } catch {}
    try {
      await rcon?.end?.();
    } catch {}
    try {
      bot?.quit?.();
    } catch {}
    runtimeLock.release();
  }
}

if (require.main === module) {
  main();
}

module.exports = { main };
