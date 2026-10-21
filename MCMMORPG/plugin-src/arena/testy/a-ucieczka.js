'use strict';
const {scenariusz, sleep, assertWorld, pair, pos, attack} = require('./common');
scenariusz('A-ucieczka', async ({w, polacz, rcon}) => {
  const {a,b} = await pair(polacz,rcon);
  await pos(rcon,a,0,65,0); await pos(rcon,b,1,65,0); await sleep(3200);
  const hp=b.health;
  w.sprawdz('trafienie', await attack(a,b) && b.health < hp, `HP B: ${hp} -> ${b.health}`);
  await rcon('arena wyjdz ArenaA'); await sleep(400);
  w.sprawdz('walka-blokuje-wyjscie', assertWorld(a), `świat ${a.game.dimension}`);
  await rcon('minecraft:execute in minecraft:world run minecraft:teleport ArenaA 0 80 0'); await sleep(400);
  w.sprawdz('komenda-teleport-blokowana', assertWorld(a), `świat ${a.game.dimension}`);
  await sleep(11000); await rcon('arena wyjdz ArenaA'); await sleep(500);
  w.sprawdz('wyjscie-po-czasie', !assertWorld(a), `świat ${a.game.dimension}`);
}, {limitMs:120000});
