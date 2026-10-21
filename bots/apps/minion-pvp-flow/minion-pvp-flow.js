'use strict';

const { BotSession, sleep } = require('@mmorpg-bots/bot-core');
const bridge = require('@mmorpg-bots/mmo-bridge');
const { execFileSync } = require('child_process');

function arg(name, fallback) {
  const index = process.argv.indexOf(`--${name}`);
  return index >= 0 && process.argv[index + 1] ? process.argv[index + 1] : fallback;
}

const domain = arg('domain', 'minioncore');
const host = arg('host', 'minioncore.localhost');
const prefix = arg('prefix', 'MPVP').slice(0, 8);
const holdMs = Number(arg('hold-ms', 12000));
const settleMs = Number(arg('settle-ms', 3000));
const statsContainers = (process.env.MC_STATS_CONTAINERS || `mc-dev-${domain}-game-1,mc-dev-${domain}-proxy-1`)
  .split(',')
  .map(value => value.trim())
  .filter(Boolean);

const ownerName = `${prefix}OWN`.slice(0, 16);
const targetName = `${prefix}TGT`.slice(0, 16);
const sessions = [];

async function rcon(command) {
  return String(await bridge.safeRcon(command, { domain }));
}

function dockerStats() {
  try {
    return execFileSync('docker', [
      'stats',
      '--no-stream',
      '--format',
      '{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}',
      ...statsContainers
    ], { encoding: 'utf8' }).trim();
  } catch (error) {
    return `STATS_ERROR ${error.message}`;
  }
}

async function minionCount() {
  const raw = await rcon('execute if entity @e[name="Melee Minion"]');
  const match = raw.match(/Count:\s*(\d+)/i);
  return match ? Number(match[1]) : 0;
}

async function minionDebugNbt() {
  const fields = ['NoAI', 'Target', 'Brain', 'Pos'];
  const entries = {};
  for (const field of fields) {
    entries[field] = (await rcon(`data get entity @e[name="Melee Minion",limit=1] ${field}`).catch(error => String(error.message))).trim();
  }
  return entries;
}

async function waitHealthDrop(bot, baseline, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (bot.health < baseline - 0.25) return { dropped: true, health: bot.health };
    await sleep(250);
  }
  return { dropped: false, health: bot.health };
}

async function waitHealthBelow(bot, threshold, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (bot.health < threshold) return { dropped: true, health: bot.health };
    await sleep(250);
  }
  return { dropped: false, health: bot.health };
}

async function lookAtTarget(ownerBot, targetBot) {
  const target = ownerBot.players[targetBot.username]?.entity || targetBot.entity;
  if (!target) throw new Error(`owner cannot see target ${targetBot.username}`);
  await ownerBot.lookAt(target.position.offset(0, 1.6, 0), true);
}

async function ownerHitsTarget(ownerBot, targetBot) {
  const target = ownerBot.players[targetBot.username]?.entity || targetBot.entity;
  if (!target) throw new Error(`owner cannot see target ${targetBot.username}`);
  await ownerBot.lookAt(target.position.offset(0, 1.6, 0), true);
  const before = targetBot.health;
  for (let attempt = 0; attempt < 3; attempt++) {
    ownerBot.attack(target);
    const result = await waitHealthDrop(targetBot, before, 1500);
    if (result.dropped) return { hit: true, before, after: targetBot.health, attempts: attempt + 1 };
    await sleep(800);
  }
  return { hit: false, before, after: targetBot.health, attempts: 3 };
}

async function preparePlayers(ownerBot, targetBot, x) {
  await rcon(`minecraft:gamemode survival ${ownerBot.username}`);
  await rcon(`minecraft:gamemode survival ${targetBot.username}`);
  await rcon(`minecraft:effect clear ${ownerBot.username}`);
  await rcon(`minecraft:effect clear ${targetBot.username}`);
  await rcon(`minecraft:effect give ${ownerBot.username} minecraft:instant_health 1 10 true`);
  await rcon(`minecraft:effect give ${targetBot.username} minecraft:instant_health 1 10 true`);
  await rcon(`minecraft:tp ${ownerBot.username} ${x} 70 0 0 0`);
  await rcon(`minecraft:tp ${targetBot.username} ${x + 2.5} 70 0 180 0`);
  await sleep(1000);
}

async function setGlobalFlags(minionPvpValue) {
  await rcon('rg flag __global__ -w world pvp allow');
  await rcon(`rg flag __global__ -w world minion-pvp ${minionPvpValue}`);
}

async function runScenario({ name, minionPvpValue, expectDamage, x }) {
  const owner = sessions[0].bot;
  const target = sessions[1].bot;
  const messages = [];
  const onMessage = message => messages.push(String(message));
  owner.on('messagestr', onMessage);

  try {
    await rcon(`minion dismiss ${owner.username}`).catch(() => '');
    owner.chat('/minion dismiss');
    await sleep(1000);
    await setGlobalFlags(minionPvpValue);
    await preparePlayers(owner, target, x);

    const before = target.health;
    owner.chat('/minion summon melee 3');
    await sleep(settleMs);
    const spawned = await minionCount();
    const ownerHit = await ownerHitsTarget(owner, target);
    await lookAtTarget(owner, target);
    owner.chat(`/minion attack ${target.username}`);
    await sleep(500);
    const minionDamageThreshold = ownerHit.after - 0.25;
    const outcome = ownerHit.hit
      ? await waitHealthBelow(target, minionDamageThreshold, holdMs)
      : { dropped: false, health: target.health };
    const after = target.health;
    const live = await minionCount();
    const debugNbt = await minionDebugNbt();

    owner.chat('/minion dismiss');
    await sleep(1000);
    const cleanupCount = await minionCount();

    const passed = expectDamage ? outcome.dropped : !outcome.dropped;
    return {
      name,
      minionPvpValue,
      expectDamage,
      passed,
      before,
      after,
      ownerHit,
      spawned,
      live,
      debugNbt,
      cleanupCount,
      messages,
      stats: dockerStats(),
      tps: await bridge.getTps({ domain }).catch(error => ({ error: error.message }))
    };
  } finally {
    owner.off('messagestr', onMessage);
  }
}

async function main() {
  const preList = await rcon('minecraft:list');
  const preMinions = await minionCount();
  if (!/0 .*70 players online/i.test(preList) || preMinions !== 0) {
    throw new Error(`isolation precheck failed: list=${preList.trim()} minions=${preMinions}`);
  }
  await rcon('gamerule naturalRegeneration false');

  const ownerSession = new BotSession({ username: ownerName, host, reconnect: false });
  const targetSession = new BotSession({ username: targetName, host, reconnect: false });
  sessions.push(ownerSession, targetSession);
  await ownerSession.connect();
  await sleep(4000);
  await targetSession.connect();
  await sleep(3000);

  const allow = await runScenario({
    name: 'global_allow',
    minionPvpValue: 'allow',
    expectDamage: true,
    x: 0
  });

  const deny = await runScenario({
    name: 'global_minion_pvp_deny',
    minionPvpValue: 'deny',
    expectDamage: false,
    x: 20
  });

  await setGlobalFlags('deny');
  await rcon('gamerule naturalRegeneration true').catch(() => '');
  await rcon(`minecraft:effect give ${targetName} minecraft:instant_health 1 10 true`).catch(() => '');
  await rcon(`minion dismiss ${ownerName}`).catch(() => '');
  ownerSession.bot.chat('/minion dismiss');
  await sleep(1000);

  const finalList = await rcon('minecraft:list');
  const finalMinions = await minionCount();
  const result = {
    verdict: allow.passed && deny.passed && finalMinions === 0 ? 'PASS' : 'FAIL',
    players: finalList.trim(),
    finalMinions,
    scenarios: [allow, deny],
    finalStats: dockerStats()
  };

  console.log(JSON.stringify(result, null, 2));
  process.exit(result.verdict === 'PASS' ? 0 : 1);
}

main()
  .catch(error => {
    console.error(JSON.stringify({ verdict: 'ERROR', error: error.stack || error.message }, null, 2));
    process.exit(2);
  })
  .finally(async () => {
    try { await rcon('gamerule naturalRegeneration true'); } catch (_) { }
    for (const session of sessions) {
      try { await session.stop(); } catch (_) { }
    }
  });
