const fs = require('fs');
const path = require('path');
const { execFile } = require('child_process');
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');

const HOST = process.env.MC_HOST || '127.0.0.1';
const PORT = Number(process.env.MC_PORT || 25565);
const RCON_PORT = Number(process.env.MC_RCON_PORT || 25575);
const RCON_PASS = process.env.RCON_PASS || 'valbot2026';
const VERSION = process.env.MC_VERSION || '1.21.11';
const RCON_TRANSPORT = process.env.MC_RCON_TRANSPORT || 'docker-mc';
const DOCKER_MC = process.env.MC_DOCKER_MC || path.resolve(__dirname, '..', 'docker', 'mc');
const DOCKER_DOMAIN = process.env.MC_DOCKER_DOMAIN || 'world';
const RUNS_DIR = path.join(__dirname, 'runs');
const STAMP = Date.now().toString().slice(-6);
const DUEL_A = process.env.PETCORE_QA_DUEL_A || `P4D${STAMP}A`;
const DUEL_B = process.env.PETCORE_QA_DUEL_B || `P4D${STAMP}B`;
const OBS_A = process.env.PETCORE_QA_OBS_A || `P4O${STAMP}A`;
const OBS_B = process.env.PETCORE_QA_OBS_B || `P4O${STAMP}B`;
const LOGIN_GAP_MS = Number(process.env.PETCORE_QA_LOGIN_GAP_MS || 6000);

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function clean(text) {
  return String(text || '')
    .replace(/§[0-9a-fk-orx]/gi, '')
    .replace(/\x1b\[[0-9;]*m/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

async function countTaggedPetCoreGlobalPets(rcon) {
  const output = clean(await rcon.send('minecraft:execute as @e[tag=petcore.global] run data get entity @s UUID'));
  if (!output) return { count: 0, evidence: output };
  return {
    count: (output.match(/has the following entity data:/g) || []).length,
    evidence: output,
  };
}

function createQaBot(username) {
  const bot = mineflayer.createBot({
    host: HOST,
    port: PORT,
    username,
    auth: 'offline',
    version: VERSION,
  });
  bot.messages = [];
  bot.once('resourcePack', () => {
    try {
      bot.acceptResourcePack();
    } catch {}
  });
  bot.on('message', message => {
    const line = clean(message.toString());
    if (line) bot.messages.push(line);
  });
  return bot;
}

function waitForSpawn(bot, timeoutMs = 45000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`${bot.username} spawn timeout`)), timeoutMs);
    bot.once('spawn', () => {
      clearTimeout(timer);
      resolve();
    });
    bot.once('kicked', reason => {
      clearTimeout(timer);
      reject(new Error(`${bot.username} kicked before spawn: ${clean(reason)}`));
    });
    bot.once('error', error => {
      clearTimeout(timer);
      reject(error);
    });
  });
}

function isLoginThrottle(error) {
  return String(error?.message || error).toLowerCase().includes('logging in too fast');
}

async function waitForMessage(bot, predicate, timeoutMs = 20000) {
  const started = Date.now();
  while (Date.now() - started < timeoutMs) {
    const match = bot.messages.find(predicate);
    if (match) return match;
    await sleep(250);
  }
  throw new Error(`${bot.username} message timeout; seen=${JSON.stringify(bot.messages.slice(-14))}`);
}

async function sendChat(bot, command) {
  bot.chat(command);
  await sleep(500);
}

async function waitForWindow(bot, timeoutMs = 12000) {
  if (bot.currentWindow) return bot.currentWindow;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      bot.removeListener('windowOpen', onOpen);
      reject(new Error(`${bot.username} window timeout`));
    }, timeoutMs);
    function onOpen(window) {
      clearTimeout(timer);
      resolve(window);
    }
    bot.once('windowOpen', onOpen);
  });
}

async function clickSlot(bot, slot) {
  if (bot.clickWindow) {
    await bot.clickWindow(slot, 0, 0);
  } else if (bot.simpleClick?.leftMouse) {
    await bot.simpleClick.leftMouse(slot);
  } else {
    throw new Error('mineflayer clickWindow API unavailable');
  }
  await sleep(1000);
}

async function ensureActiveProfile(bot, rcon) {
  await rcon.send(`mmoprofiles open ${bot.username} true`);
  await waitForWindow(bot);
  await clickSlot(bot, 10);
  await sleep(500);
  if (bot.messages.some(line => line.toLowerCase().includes('desired profile name'))) {
    bot.chat(`QA${bot.username.slice(-6)}`);
    await sleep(3000);
  }
  if (bot.currentWindow) {
    await clickSlot(bot, 10);
    await sleep(1500);
  }
}

async function connectQaRcon() {
  if (RCON_TRANSPORT !== 'docker-mc') {
    return Rcon.connect({ host: HOST, port: RCON_PORT, password: RCON_PASS });
  }
  return {
    send(command) {
      return new Promise((resolve, reject) => {
        execFile(
          'bash',
          [DOCKER_MC, 'rcon', DOCKER_DOMAIN, command],
          { cwd: path.dirname(DOCKER_MC), maxBuffer: 1024 * 1024 },
          (error, stdout, stderr) => {
            if (error) {
              reject(new Error(clean(stderr || stdout || error.message)));
              return;
            }
            resolve(stdout || stderr || '');
          }
        );
      });
    },
    end() {},
  };
}

async function connectSequentially(users) {
  const bots = [];
  for (const user of users) {
    let bot = createQaBot(user);
    try {
      await waitForSpawn(bot);
    } catch (error) {
      try {
        bot.quit();
      } catch {}
      if (!isLoginThrottle(error)) throw error;
      await sleep(LOGIN_GAP_MS * 2);
      bot = createQaBot(user);
      await waitForSpawn(bot);
    }
    bots.push(bot);
    await sleep(LOGIN_GAP_MS);
  }
  return bots;
}

function distance(a, b) {
  return a.entity.position.distanceTo(b.entity.position);
}

function hasDeathMessage(bot) {
  return bot.messages.some(line => /slain|died|zgin|umar/i.test(line));
}

async function main() {
  fs.mkdirSync(RUNS_DIR, { recursive: true });
  const startedAt = new Date().toISOString();
  const users = [DUEL_A, DUEL_B, OBS_A, OBS_B];
  const report = {
    startedAt,
    users,
    duelists: [DUEL_A, DUEL_B],
    observers: [OBS_A, OBS_B],
    steps: [],
    verdict: 'UNKNOWN',
  };
  let rcon;
  let bots = [];
  try {
    rcon = await connectQaRcon();
    const plugins = clean(await rcon.send('plugins'));
    if (!plugins.includes('PetCore') || !plugins.includes('MMOProfiles') || !plugins.includes('MythicMobs')) {
      throw new Error(`required plugins missing: ${plugins}`);
    }
    report.steps.push({ name: 'plugins', status: 'PASS', evidence: plugins });

    bots = await connectSequentially(users);
    const [duelistA, duelistB, observerA, observerB] = bots;
    report.steps.push({ name: 'four_bot_spawn', status: 'PASS' });

    for (const user of users) await rcon.send(`op ${user}`);
    report.steps.push({ name: 'op_test_users', status: 'PASS' });

    for (const bot of bots) await ensureActiveProfile(bot, rcon);
    report.steps.push({ name: 'select_mmoprofiles_profiles', status: 'PASS' });

    await rcon.send('fill 236 62 205 246 62 215 minecraft:stone replace');
    await rcon.send('fill 236 63 205 246 66 215 minecraft:air replace');
    await rcon.send(`tp ${DUEL_A} 239 63 208`);
    await rcon.send(`tp ${DUEL_B} 243 63 208`);
    await rcon.send(`tp ${OBS_A} 239 63 212`);
    await rcon.send(`tp ${OBS_B} 243 63 212`);
    await sleep(2500);
    const observerDistances = [
      Math.min(distance(observerA, duelistA), distance(observerA, duelistB)),
      Math.min(distance(observerB, duelistA), distance(observerB, duelistB)),
    ];
    if (observerDistances.some(value => value > 5)) {
      throw new Error(`observers not near duelists: ${observerDistances.map(v => v.toFixed(2)).join(', ')}`);
    }
    report.steps.push({
      name: 'observers_positioned_near_duel',
      status: 'PASS',
      evidence: observerDistances.map(value => Number(value.toFixed(2))),
    });

    await sendChat(duelistA, '/petcore summon');
    await sendChat(duelistB, '/petcore summon');
    const summonA = await waitForMessage(duelistA, line =>
      line.includes('Global Pet summoned.') || line.includes('Global Pet summon denied:')
    );
    const summonB = await waitForMessage(duelistB, line =>
      line.includes('Global Pet summoned.') || line.includes('Global Pet summon denied:')
    );
    if (!summonA.includes('Global Pet summoned.') || !summonB.includes('Global Pet summoned.')) {
      throw new Error(`summon failed: ${summonA} | ${summonB}`);
    }
    report.steps.push({ name: 'two_pet_summon_only', status: 'PASS', evidence: [summonA, summonB] });

    await sendChat(duelistA, `/petcore duel request ${DUEL_B}`);
    const requestResult = await waitForMessage(duelistA, line => line.includes('Pet Duel request:'));
    const requestNotice = await waitForMessage(duelistB, line => line.includes('Pet Duel request received.'));
    if (!requestResult.includes('ACCEPTED')) throw new Error(`duel request not accepted: ${requestResult}`);

    await sendChat(duelistB, '/petcore duel accept');
    const acceptResult = await waitForMessage(duelistB, line => line.includes('Pet Duel accept:'));
    if (!acceptResult.includes('ACCEPTED')) throw new Error(`duel accept failed: ${acceptResult}`);
    report.steps.push({
      name: 'duel_request_accept',
      status: 'PASS',
      evidence: [requestResult, requestNotice, acceptResult],
    });

    const activeDuringDuel = await countTaggedPetCoreGlobalPets(rcon);
    if (activeDuringDuel.count !== 2) {
      throw new Error(`expected exactly two tagged PetCore global pets during duel: ${activeDuringDuel.evidence}`);
    }
    report.steps.push({
      name: 'exactly_two_petcore_mobs_during_four_bot_duel',
      status: 'PASS',
      evidence: { count: activeDuringDuel.count, output: activeDuringDuel.evidence.slice(0, 700) },
    });

    const observerHealthBefore = [observerA.health, observerB.health];
    await sleep(8000);
    const observerHealthAfter = [observerA.health, observerB.health];
    if (hasDeathMessage(observerA) || hasDeathMessage(observerB)) {
      throw new Error(`observer death message seen: ${observerA.messages.slice(-8).join(' | ')} :: ${observerB.messages.slice(-8).join(' | ')}`);
    }
    if (observerHealthAfter[0] < observerHealthBefore[0] || observerHealthAfter[1] < observerHealthBefore[1]) {
      throw new Error(`observer damage detected: before=${observerHealthBefore.join(',')} after=${observerHealthAfter.join(',')}`);
    }
    if (hasDeathMessage(duelistA) || hasDeathMessage(duelistB)) {
      throw new Error(`duelist owner death message seen: ${duelistA.messages.slice(-8).join(' | ')} :: ${duelistB.messages.slice(-8).join(' | ')}`);
    }
    report.steps.push({
      name: 'spectators_safe_while_pets_fight',
      status: 'PASS',
      evidence: { before: observerHealthBefore, after: observerHealthAfter },
    });

    await sendChat(duelistA, '/petcore duel end');
    const endResult = await waitForMessage(duelistA, line => line.includes('Pet Duel ended.'));
    await sendChat(duelistA, '/petcore recall');
    await sendChat(duelistB, '/petcore recall');
    await sleep(1500);
    const activeAfterCleanup = await countTaggedPetCoreGlobalPets(rcon);
    if (activeAfterCleanup.count !== 0) {
      throw new Error(`cleanup left tagged PetCore global pets: ${activeAfterCleanup.evidence}`);
    }
    report.steps.push({ name: 'duel_end_cleanup', status: 'PASS', evidence: [endResult, activeAfterCleanup] });

    report.verdict = 'PASS';
  } catch (error) {
    report.verdict = 'FAIL';
    report.error = error.stack || error.message;
    process.exitCode = 1;
  } finally {
    const [duelistA, duelistB] = bots;
    try {
      if (duelistA) await sendChat(duelistA, '/petcore recall');
      if (duelistB) await sendChat(duelistB, '/petcore recall');
    } catch {}
    try {
      if (rcon) for (const user of users) await rcon.send(`deop ${user}`);
    } catch {}
    try {
      for (const bot of bots) bot.quit();
    } catch {}
    try {
      if (rcon) rcon.end();
    } catch {}
    const file = path.join(RUNS_DIR, `petcore_duel_four_bot_${startedAt.replace(/[:.]/g, '-')}.json`);
    fs.writeFileSync(file, JSON.stringify(report, null, 2) + '\n');
    console.log(`${report.verdict} ${file}`);
  }
}

main();
