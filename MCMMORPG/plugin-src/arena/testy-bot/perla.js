'use strict';
// Luka 5 (czesc botowa): perla poza granice areny / z ringu na trybuny w walce, mikstura pozostajaca z trybun w ring.
const { scenariusz } = require('/bot/lib');
const { narzedzia, RING, TRYBUNY, W_RINGU, TARAS, sleep } = require('./arena-lib');
const { Vec3 } = require('vec3');
scenariusz('arena-perla', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const a = await polacz({ nick: 'ArenaA' }); const b = await polacz({ nick: 'ArenaB' });
  await t.przygotuj();
  for (const n of ['ArenaA', 'ArenaB']) if (!(await t.wymiar(n)).includes('arena')) { await t.wWorld(n, n === 'ArenaA' ? 1.5 : 4.5); await t.wejdz(n); }
  const daj = async (n, item) => { await rcon(`minecraft:item replace entity ${n} container.0 with ${item}`); await sleep(500); };
  const rzucPerle = async (cel) => { await daj('ArenaA', 'minecraft:ender_pearl 4'); a.setQuickBarSlot(0); await a.lookAt(cel, true); await sleep(200); a.activateItem(); await sleep(3500); };
  // 1. perla z platformy w pustke za granica (r>64 albo y<45)
  await t.tpArena('ArenaA', { x: 0.5, y: 65, z: -34.5 }, 180); await sleep(6000);
  await rzucPerle(new Vec3(0.5, 70, -80));
  const p1 = await t.poz('ArenaA'); console.log('perla w pustke ->', await t.wymiar('ArenaA'), JSON.stringify(p1));
  w.sprawdz('perla-pustka', (await t.wymiar('ArenaA')).includes('arena') && p1.y >= 60 && Math.hypot(p1.x, p1.z) < 64, 'perla w pustke: gracz zostaje na arenie (platforma), nie spada', JSON.stringify(p1));
  // 2. w walce: perla z ringu na trybuny - walka trwa (tag), nie da sie wyjsc z menu
  await t.tpArena('ArenaA', { ...RING, z: -2.5 }, 0); await t.tpArena('ArenaB', { ...RING, z: -0.5 }, 180); await sleep(6500);
  await t.cios(a, 'ArenaB', 'cios (tag)');
  await rzucPerle(new Vec3(0.5, 66, -24.5));
  const p2 = await t.poz('ArenaA'); console.log('perla na trybuny ->', JSON.stringify(p2));
  await rcon('arena wyjdz ArenaA'); await sleep(800);
  w.sprawdz('perla-tag', (await t.wymiar('ArenaA')).includes('arena'), 'po perle z ringu tag dalej trzyma (wyjscie menu odmowione)');
  await sleep(11000);
  // 3. mikstura pozostajaca krzywdy z trybun w ring
  await t.tpArena('ArenaA', TRYBUNY, 180); await t.tpArena('ArenaB', { ...RING, z: -14.5 }, 0); await sleep(6500); await t.uleczy('ArenaB');
  await daj('ArenaA', 'minecraft:lingering_potion[minecraft:potion_contents={potion:"minecraft:strong_harming"}]'); a.setQuickBarSlot(0);
  { const pc = await t.poz('ArenaB'); await a.lookAt(new Vec3(pc.x, pc.y + 0.2, pc.z), true); }
  const h0 = await t.hp('ArenaB'); a.activateItem(); await sleep(4000); const h1 = await t.hp('ArenaB');
  console.log('chmura z trybun HP', h0, '->', h1, '| chmury:', await rcon('minecraft:execute in minecraft:arena run minecraft:execute if entity @e[type=minecraft:area_effect_cloud]'));
  w.sprawdz('chmura-z-trybun', h0 === h1, 'chmura krzywdy rzucona z trybun nie rani walczacego', `${h0} -> ${h1}`);
  // kontrola: chmura w ringu
  await sleep(2000); await t.tpArena('ArenaA', { ...RING, z: -10.5 }, 0); await sleep(6500); await t.uleczy('ArenaB');
  await daj('ArenaA', 'minecraft:lingering_potion[minecraft:potion_contents={potion:"minecraft:strong_harming"}]'); a.setQuickBarSlot(0);
  { const pc = await t.poz('ArenaB'); await a.lookAt(new Vec3(pc.x, pc.y + 0.2, pc.z), true); }
  const k0 = await t.hp('ArenaB'); a.activateItem(); await sleep(4000); const k1 = await t.hp('ArenaB');
  console.log('chmura w ringu HP', k0, '->', k1);
  w.sprawdz('chmura-ring', k1 < k0, 'kontrola: chmura krzywdy w ringu rani', `${k0} -> ${k1}`);
  await rcon('minecraft:execute in minecraft:arena run minecraft:kill @e[type=minecraft:area_effect_cloud]');
  await daj('ArenaA', 'minecraft:air'); await sleep(11000);
}, { limitMs: 220000 });
