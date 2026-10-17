'use strict';
const { scenariusz } = require('/bot/lib');
const { narzedzia, RING, sleep } = require('./arena-lib');
scenariusz('arena-respawn', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const b = await polacz({ nick: 'ArenaB' });
  let spawny = 0; b.on('death', () => console.log('bot: death')); b.on('spawn', () => { spawny++; console.log('bot: spawn', b.game.dimension, b.entity.position); });
  await t.przygotuj();
  if (!(await t.wymiar('ArenaB')).includes('arena')) { await t.wWorld('ArenaB'); await t.wejdz('ArenaB'); }
  await rcon('minecraft:clear ArenaB minecraft:diamond'); await rcon('minecraft:give ArenaB minecraft:diamond 3');
  await t.tpArena('ArenaB', RING); await sleep(6500);
  const ile = async () => (await rcon('execute if items entity ArenaB container.* minecraft:diamond'));
  console.log('przed:', await ile());
  await rcon('minecraft:damage ArenaB 500 minecraft:generic'); 
  for (let i = 0; i < 10; i++) { await sleep(1000); console.log(i, 'hp', await t.hp('ArenaB'), 'bot.health', b.health, 'isAlive', b.isAlive, await t.wymiar('ArenaB'), JSON.stringify(await t.poz('ArenaB'))); }
  console.log('po:', await ile());
  w.sprawdz('respawn', spawny > 0, 'bot sie odrodzil', spawny);
  const pp = await t.poz('ArenaB');
  w.sprawdz('respawn-platforma', (await t.wymiar('ArenaB')).includes('arena') && Math.abs(pp.z + 28.5) < 2 && Math.abs(pp.x - 0.5) < 2, 'odradza sie na platformie, nie w ringu', JSON.stringify(pp));
  await rcon('minecraft:clear ArenaB minecraft:diamond');
}, { limitMs: 90000 });
