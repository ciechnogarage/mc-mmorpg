'use strict';
// D: granica ringu. Wrecz i luk w obie strony przez line; kontrola: obaj w ringu.
const { scenariusz } = require('/bot/lib');
const { narzedzia, RING, TRYBUNY, W_RINGU, TARAS, sleep } = require('./arena-lib');
scenariusz('arena-granica', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const a = await polacz({ nick: 'ArenaA' }); const b = await polacz({ nick: 'ArenaB' });
  await t.przygotuj();
  for (const n of ['ArenaA', 'ArenaB']) { await rcon(`minecraft:item replace entity ${n} container.0 with minecraft:air`); await rcon(`minecraft:clear ${n} minecraft:arrow`); }
  for (const n of ['ArenaA', 'ArenaB']) { await t.wWorld(n, n === 'ArenaA' ? 1.5 : 4.5); w.sprawdz('wejscie-' + n, await t.wejdz(n), 'wejscie z menu (arena wejdz)'); }
  const ustaw = async (pa, pb, ya, yb) => { await t.tpArena('ArenaA', pa, ya); await t.tpArena('ArenaB', pb, yb); await sleep(6500); await t.uleczy('ArenaB'); await t.uleczy('ArenaA'); };
  // wrecz
  await ustaw({ ...RING, z: -15.5 }, W_RINGU, 180, 0);
  w.sprawdz('wrecz-ring-ring', (await t.cios(a, 'ArenaB', 'ring->ring')) > 0, 'obaj w ringu: cios rani');
  await sleep(11000);
  await ustaw(W_RINGU, TRYBUNY, 180, 0);
  w.sprawdz('wrecz-ring-trybuny', (await t.cios(a, 'ArenaB', 'ring->trybuny')) === 0, 'z ringu na widza za lina: zero');
  await ustaw(TRYBUNY, W_RINGU, 0, 180);
  w.sprawdz('wrecz-trybuny-ring', (await t.cios(a, 'ArenaB', 'trybuny->ring')) === 0, 'widz za lina na walczacego: zero');
  // luk
  for (const bot of [a, b]) { const n = bot.username; await rcon(`mmocore admin attribute give ${n} bieglosc-luk 1`); await rcon(`minecraft:item replace entity ${n} container.0 with minecraft:air`); await rcon(`mi give BOW LUK_MYSLIWSKI ${n} 1`); await sleep(300); await rcon(`minecraft:give ${n} minecraft:arrow 32`); console.log(n, 'slot0:', (await rcon(`data get entity ${n} Inventory[{Slot:0b}].id`)).replace(/.*data: /, '')); }
  await sleep(800);
  const strzal = async (strzelec, cel, opis) => {
    strzelec.setQuickBarSlot(0); await sleep(300);
    const { Vec3 } = require('vec3'); const pc = await t.poz(cel), ps = await t.poz(strzelec.username);
    const e = new Vec3(pc.x, pc.y, pc.z); const d = new Vec3(ps.x, ps.y, ps.z).distanceTo(e);
    await strzelec.lookAt(e.offset(0, 1.1 + d * 0.01, 0), true); await sleep(300);
    const h0 = await t.hp(cel); strzelec.activateItem(); await sleep(1200); strzelec.deactivateItem(); await sleep(1500); const h1 = await t.hp(cel);
    console.log(opis, '| odl', d.toFixed(1), '| HP', h0, '->', h1); return +(h0 - h1).toFixed(2);
  };
  await ustaw({ ...RING, z: -3.5 }, { ...RING, z: 2.5 }, 0, 180);
  w.sprawdz('luk-ring-ring', (await strzal(a, 'ArenaB', 'luk ring->ring')) > 0, 'luk w ringu rani (kontrola)');
  await sleep(11000);
  await ustaw(TARAS, { ...RING, z: -12.5 }, 0, 180);
  w.sprawdz('luk-taras-ring', (await strzal(a, 'ArenaB', 'luk taras->ring')) === 0, 'strzala z trybun do ringu: zero');
  await ustaw({ ...RING, z: -12.5 }, TARAS, 180, 0);
  w.sprawdz('luk-ring-taras', (await strzal(a, 'ArenaB', 'luk ring->taras')) === 0, 'strzala z ringu w widza: zero');
  // strzal z ringu, cel wchodzi... strzala wypuszczona z trybun, cel w ringu -> zero nawet gdy strzelec wejdzie do ringu
  await ustaw(TRYBUNY, { ...RING, z: -10.5 }, 0, 180);
  a.setQuickBarSlot(0);
  { const { Vec3 } = require('vec3'); const pc = await t.poz('ArenaB'); await a.lookAt(new Vec3(pc.x, pc.y + 1.2, pc.z), true); }
  const h0 = await t.hp('ArenaB'); a.activateItem(); await sleep(1100); a.deactivateItem(); await sleep(200); await t.tpArena('ArenaA', W_RINGU, 0); await sleep(1500);
  const h1 = await t.hp('ArenaB'); console.log('wypuszczona zza liny | HP', h0, '->', h1);
  w.sprawdz('luk-wypuszczona-poza', h0 === h1, 'strzala wypuszczona zza liny nie rani, nawet gdy strzelec wskoczy do ringu', `${h0} -> ${h1}`);
  for (const n of ['ArenaA', 'ArenaB']) { await rcon(`minecraft:item replace entity ${n} container.0 with minecraft:air`); await rcon(`minecraft:clear ${n} minecraft:arrow`); }
  await sleep(11000); // bez wylogowania w walce (forfeit 60 s psuje nastepny przebieg)
}, { limitMs: 260000 });
