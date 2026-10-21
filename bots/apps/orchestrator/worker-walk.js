'use strict';

// Worker load-testu: "dumb AI" (plan F) — chodzenie na dystans w losowym
// kierunku bez pathfindingu (niskie CPU), test ładowania chunków pod Iris.

const { BotSession, sleep } = require('@mmorpg-bots/bot-core');
const { installProcessGuards } = require('@mmorpg-bots/safe-protocol');

installProcessGuards(console);

const USER = (process.env.BOT_USERNAME || `LOAD${process.pid}`).slice(0, 16);

async function main() {
  const session = new BotSession({ username: USER, maxReconnects: 10 });
  const bot = await session.connect();
  console.log(`spawn ok @ ${bot.entity.position}`);

  const yaw = Math.random() * Math.PI * 2;
  await bot.look(yaw, 0, true);
  bot.setControlState('forward', true);
  bot.setControlState('sprint', true);
  bot.setControlState('jump', true);

  let lastPos = bot.entity.position.clone();
  setInterval(async () => {
    const pos = bot.entity?.position;
    if (!pos) return;
    const moved = pos.distanceTo(lastPos);
    console.log(`pos=${pos.floored()} moved=${moved.toFixed(1)} chunks=${Object.keys(bot.world?.async?.columns || {}).length || '?'}`);
    if (moved < 2) {
      // anti-stuck: nowy kierunek
      await bot.look(Math.random() * Math.PI * 2, 0, true).catch(() => {});
    }
    lastPos = pos.clone();
  }, 10000);
}

main().catch(err => {
  console.error('worker fatal:', err.message);
  process.exit(1);
});
