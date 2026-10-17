'use strict';
// Luk w ringu: 3 strzaly A->B z 5 blokow (diagnostyka: kto anuluje strzale).
const { scenariusz } = require('/bot/lib');
const { narzedzia, RING, sleep } = require('./arena-lib');
const { Vec3 } = require('vec3');
scenariusz('arena-luk', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const a = await polacz({ nick: 'ArenaA' }); const b = await polacz({ nick: 'ArenaB' });
  await t.przygotuj();
  for (const n of ['ArenaA', 'ArenaB']) { if (!(await t.wymiar(n)).includes('arena')) { await t.wWorld(n); await t.wejdz(n); } }
  await rcon('mmocore admin attribute give ArenaA bieglosc-luk 1');
  await rcon('minecraft:item replace entity ArenaA container.0 with minecraft:air'); await rcon('mi give BOW LUK_MYSLIWSKI ArenaA 1'); await sleep(300);
  await rcon('minecraft:clear ArenaA minecraft:arrow'); await rcon('minecraft:give ArenaA minecraft:arrow 16');
  await t.tpArena('ArenaA', { ...RING, z: -2.5 }, 0); await t.tpArena('ArenaB', { ...RING, z: 2.5 }, 180); await sleep(6500);
  let suma = 0;
  for (let i = 0; i < 3; i++) {
    a.setQuickBarSlot(0); const pc = await t.poz('ArenaB'); await a.lookAt(new Vec3(pc.x, pc.y + 1.2, pc.z), true); await sleep(300);
    const h0 = await t.hp('ArenaB'); a.activateItem(); await sleep(1300); a.deactivateItem(); await sleep(1500); const h1 = await t.hp('ArenaB');
    console.log('strzal', i, 'HP', h0, '->', h1); suma += h0 - h1; await t.uleczy('ArenaB');
  }
  w.sprawdz('luk-ring', suma > 0, 'luk rani w ringu', suma);
  await rcon('minecraft:item replace entity ArenaA container.0 with minecraft:air'); await rcon('minecraft:clear ArenaA minecraft:arrow');
  await sleep(11000);
}, { limitMs: 170000 });
