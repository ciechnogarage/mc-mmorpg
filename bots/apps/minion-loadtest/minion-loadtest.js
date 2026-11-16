'use strict';

// Staging-only MinionCore client fleet. Clients join through Velocity and use
// real player commands; RCON is used only for observation and isolated test
// setup/cleanup, never to bypass MinionCore summon logic.

const { BotSession, sleep } = require('@mmorpg-bots/bot-core');
const bridge = require('@mmorpg-bots/mmo-bridge');

function arg(name, fallback) {
  const index = process.argv.indexOf(`--${name}`);
  return index >= 0 && process.argv[index + 1] ? process.argv[index + 1] : fallback;
}

const count = Number(arg('count', 10));
const prefix = arg('prefix', 'MINION').slice(0, 10);
const joinDelay = Number(arg('join-delay', 4000));
const settleMs = Number(arg('settle-ms', 5000));
const holdMs = Number(arg('hold-ms', 70000));
const domain = arg('domain', 'world');
const host = arg('host', 'world.localhost');
const batchSummon = process.argv.includes('--batch-summon');
const summonAmount = Number(arg('amount', batchSummon ? 1 : 3));
const summonType = arg('type', 'melee');
const safeArena = !process.argv.includes('--no-safe-arena');
const arenaX = Number(arg('arena-x', 0));
const arenaY = Number(arg('arena-y', 71));
const arenaZ = Number(arg('arena-z', 0));
const sessions = [];
const results = [];
const connectedBots = [];

async function prepareSafeArena() {
  if (!safeArena) {
    return;
  }
  await bridge.safeRcon(
    `execute in minecraft:overworld run forceload add ${arenaX - 32} ${arenaZ - 32} ${arenaX + 32} ${arenaZ + 32}`,
    { domain }
  );
  await bridge.safeRcon(
    `execute in minecraft:overworld run fill ${arenaX - 24} ${arenaY - 1} ${arenaZ - 24} ${arenaX + 24} ${arenaY - 1} ${arenaZ + 24} minecraft:stone`,
    { domain }
  );
  await bridge.safeRcon(
    `execute in minecraft:overworld run fill ${arenaX - 24} ${arenaY} ${arenaZ - 24} ${arenaX + 24} ${arenaY + 5} ${arenaZ + 24} minecraft:air`,
    { domain }
  );
}

async function moveToSafeArena(username, index) {
  if (!safeArena) {
    return;
  }
  const column = (index - 1) % 10;
  const row = Math.floor((index - 1) / 10);
  const x = arenaX - 18 + column * 4;
  const z = arenaZ - 18 + row * 4;
  await bridge.safeRcon(`execute in minecraft:overworld run tp ${username} ${x + 0.5} ${arenaY} ${z + 0.5}`, { domain });
  await bridge.safeRcon(`minecraft:effect clear ${username}`, { domain });
  await bridge.safeRcon(`minecraft:gamemode adventure ${username}`, { domain });
}

async function main() {
  await prepareSafeArena();

  for (let index = 1; index <= count; index++) {
    const username = `${prefix}${String(index).padStart(3, '0')}`.slice(0, 16);
    const session = new BotSession({ username, host, reconnect: false });
    try {
      const bot = await session.connect();
      const messages = [];
      bot.on('messagestr', message => messages.push(message));
      connectedBots.push({ bot, messages, username });
      await moveToSafeArena(username, index);
      if (!batchSummon) {
        bot.chat(`/minion summon ${summonType} ${summonAmount}`);
      }
      await sleep(500);
      results.push({
        username,
        joined: true,
        position: String(bot.entity?.position ?? ''),
        messages,
      });
      sessions.push(session);
      console.log(
        `[${username}] joined${batchSummon ? ' (batch summon pending)' : ` and summoned ${summonType} x${summonAmount}`}`
      );
    } catch (error) {
      results.push({ username, joined: false, error: String(error.message || error) });
      await session.stop().catch(() => {});
      console.error(`[${username}] join failed: ${error.message || error}`);
    }

    if (index < count) {
      await sleep(joinDelay);
    }
  }

  if (batchSummon) {
    for (const entry of connectedBots) {
      entry.bot.chat(`/minion summon ${summonType} ${summonAmount}`);
    }
    await sleep(1500);
  }

  await sleep(settleMs);
  const list = await bridge.safeRcon('minecraft:list', { domain });
  const mobs = await bridge.safeRcon('mm mobs list', { domain });
  const minionCount = await bridge.safeRcon('execute if entity @e[name="Melee Minion"]', { domain });

  console.log(
    JSON.stringify(
      {
        count,
        joined: results.filter(result => result.joined).length,
        summonAmount,
        expectedMinions: results.filter(result => result.joined).length * summonAmount,
        results,
        list: String(list),
        mobs: String(mobs),
        minionCount: String(minionCount),
      },
      null,
      2
    )
  );

  await sleep(holdMs);

  for (const session of sessions) {
    await session.stop().catch(() => {});
  }
  console.log(JSON.stringify({ verdict: 'OBSERVED', disconnected: sessions.length }));
}

main().catch(error => {
  console.error('[minion-loadtest] fatal:', error);
  process.exitCode = 1;
});
