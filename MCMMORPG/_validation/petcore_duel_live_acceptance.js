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
const USER_A = process.env.PETCORE_QA_USER_A || `PQA${Date.now().toString().slice(-6)}A`;
const USER_B = process.env.PETCORE_QA_USER_B || `PQA${Date.now().toString().slice(-6)}B`;
const RUNS_DIR = path.join(__dirname, 'runs');

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

async function waitForMessage(bot, predicate, timeoutMs = 20000) {
  const started = Date.now();
  while (Date.now() - started < timeoutMs) {
    const match = bot.messages.find(predicate);
    if (match) return match;
    await sleep(250);
  }
  throw new Error(`${bot.username} message timeout; seen=${JSON.stringify(bot.messages.slice(-12))}`);
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
  const window = await waitForWindow(bot);
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
          DOCKER_MC,
          ['rcon', DOCKER_DOMAIN, command],
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

async function main() {
  fs.mkdirSync(RUNS_DIR, { recursive: true });
  const startedAt = new Date().toISOString();
  const report = {
    startedAt,
    users: [USER_A, USER_B],
    steps: [],
    verdict: 'UNKNOWN',
  };
  let rcon;
  let botA;
  let botB;
  try {
    rcon = await connectQaRcon();
    const plugins = clean(await rcon.send('plugins'));
    if (!plugins.includes('PetCore') || !plugins.includes('MMOProfiles') || !plugins.includes('MythicMobs')) {
      throw new Error(`required plugins missing: ${plugins}`);
    }
    report.steps.push({ name: 'plugins', status: 'PASS', evidence: plugins });

    botA = createQaBot(USER_A);
    await waitForSpawn(botA);
    await sleep(3500);
    botB = createQaBot(USER_B);
    await waitForSpawn(botB);
    report.steps.push({ name: 'bot_spawn', status: 'PASS' });

    await rcon.send(`op ${USER_A}`);
    await rcon.send(`op ${USER_B}`);
    report.steps.push({ name: 'op_test_users', status: 'PASS' });

    await ensureActiveProfile(botA, rcon);
    await ensureActiveProfile(botB, rcon);
    report.steps.push({ name: 'select_mmoprofiles_profile', status: 'PASS' });

    await sendChat(botA, '/petcore summon');
    await sendChat(botB, '/petcore summon');
    const summonA = await waitForMessage(botA, line =>
      line.includes('Global Pet summoned.') || line.includes('Global Pet summon denied:')
    );
    const summonB = await waitForMessage(botB, line =>
      line.includes('Global Pet summoned.') || line.includes('Global Pet summon denied:')
    );
    if (!summonA.includes('Global Pet summoned.') || !summonB.includes('Global Pet summoned.')) {
      throw new Error(`summon failed: ${summonA} | ${summonB}`);
    }
    report.steps.push({ name: 'summon', status: 'PASS', evidence: [summonA, summonB] });

    await sendChat(botA, `/petcore duel request ${USER_B}`);
    const requestResult = await waitForMessage(botA, line => line.includes('Pet Duel request:'));
    const requestNotice = await waitForMessage(botB, line => line.includes('Pet Duel request received.'));
    if (!requestResult.includes('ACCEPTED')) {
      throw new Error(`duel request not accepted: ${requestResult}`);
    }
    report.steps.push({ name: 'duel_request', status: 'PASS', evidence: [requestResult, requestNotice] });

    await sendChat(botB, '/petcore duel accept');
    const acceptResult = await waitForMessage(botB, line => line.includes('Pet Duel accept:'));
    if (!acceptResult.includes('ACCEPTED')) {
      throw new Error(`duel accept failed: ${acceptResult}`);
    }
    report.steps.push({ name: 'duel_accept', status: 'PASS', evidence: acceptResult });

    await sendChat(botA, '/petcore duel skill PetCore_GlobalPetPulse');
    const firstSkillResult = await waitForMessage(botA, line => line.includes('Pet Duel skill'));
    if (!firstSkillResult.includes('invoked') && !firstSkillResult.includes('denied')) {
      throw new Error(`unexpected duel skill response: ${firstSkillResult}`);
    }
    if (firstSkillResult.includes('denied')) {
      await sleep(1500);
      await sendChat(botA, '/petcore duel skill PetCore_GlobalPetPulse');
    }
    const skillResult = firstSkillResult.includes('invoked')
      ? firstSkillResult
      : await waitForMessage(botA, line => line.includes('Pet Duel skill invoked.'));
    if (!skillResult.includes('invoked')) {
      throw new Error(`duel skill failed: ${skillResult}`);
    }
    report.steps.push({ name: 'duel_skill', status: 'PASS', evidence: [firstSkillResult, skillResult] });

    await sendChat(botA, '/petcore duel end');
    const endResult = await waitForMessage(botA, line => line.includes('Pet Duel ended.'));
    report.steps.push({ name: 'duel_end', status: 'PASS', evidence: endResult });

    const active = clean(await rcon.send('mm mobs listactive'));
    report.steps.push({ name: 'mythic_active_snapshot', status: 'PASS', evidence: active.slice(0, 500) });
    report.verdict = 'PASS';
  } catch (error) {
    report.verdict = 'FAIL';
    report.error = error.stack || error.message;
    process.exitCode = 1;
  } finally {
    try {
      if (botA) await sendChat(botA, '/petcore recall');
      if (botB) await sendChat(botB, '/petcore recall');
    } catch {}
    try {
      if (rcon) {
        await rcon.send(`deop ${USER_A}`);
        await rcon.send(`deop ${USER_B}`);
      }
    } catch {}
    try {
      if (botA) botA.quit();
      if (botB) botB.quit();
    } catch {}
    try {
      if (rcon) rcon.end();
    } catch {}
    const file = path.join(RUNS_DIR, `petcore_duel_live_${startedAt.replace(/[:.]/g, '-')}.json`);
    fs.writeFileSync(file, JSON.stringify(report, null, 2) + '\n');
    console.log(`${report.verdict} ${file}`);
  }
}

main();
