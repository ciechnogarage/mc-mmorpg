'use strict';
// PvP tylko w ringu na CALYM serwerze: world bez flagi WG (sama wtyczka). Kontrole: mob rani, leczenie innego dziala.
const { scenariusz } = require('/bot/lib');
const { narzedzia, RING, sleep } = require('./arena-lib');
const { Vec3 } = require('vec3');
scenariusz('arena-polityka', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const a = await polacz({ nick: 'ArenaA' }); const b = await polacz({ nick: 'ArenaB' });
  await t.przygotuj();
  console.log('WG world:', (await rcon('rg info __global__ -w world')).replace(/\n/g, ' ').slice(0, 120));
  for (const n of ['ArenaA', 'ArenaB']) await t.wWorld(n, n === 'ArenaA' ? 1.5 : 3.5);
  await t.tpWorld('ArenaA', { x: 1.5, y: -60, z: 2.5 }, -90); await t.tpWorld('ArenaB', { x: 3.5, y: -60, z: 2.5 }, 90); await sleep(6500);
  await rcon('minecraft:effect give ArenaB minecraft:instant_health 1 5 true'); await sleep(500);
  w.sprawdz('world-wrecz', (await t.cios(a, 'ArenaB', 'world wrecz')) === 0, 'w world (bez WG) cios gracz->gracz zablokowany');
  // luk
  await rcon('mmocore admin attribute give ArenaA bieglosc-luk 1'); await rcon('minecraft:item replace entity ArenaA container.0 with minecraft:air'); await rcon('mi give BOW LUK_MYSLIWSKI ArenaA 1'); await rcon('minecraft:give ArenaA minecraft:arrow 8'); await sleep(500);
  await t.tpWorld('ArenaA', { x: 1.5, y: -60, z: -2.5 }, 0); await sleep(6500);
  a.setQuickBarSlot(0); { const p = await t.poz('ArenaB'); await a.lookAt(new Vec3(p.x, p.y + 1.2, p.z), true); }
  let h0 = await t.hp('ArenaB'); a.activateItem(); await sleep(1300); a.deactivateItem(); await sleep(1500);
  w.sprawdz('world-luk', (await t.hp('ArenaB')) === h0, 'strzala w world nie rani gracza', `${h0} -> ${await t.hp('ArenaB')}`);
  // mikstura krzywdy (szkodliwa) i leczenia (pomocna)
  await rcon('minecraft:item replace entity ArenaA container.0 with minecraft:splash_potion[minecraft:potion_contents={potion:"minecraft:harming"}]'); await sleep(400);
  { const p = await t.poz('ArenaB'); await a.lookAt(new Vec3(p.x, p.y + 0.3, p.z), true); }
  h0 = await t.hp('ArenaB'); a.activateItem(); await sleep(1800);
  w.sprawdz('world-mikstura', (await t.hp('ArenaB')) === h0, 'mikstura krzywdy w world nie rani gracza', `${h0} -> ${await t.hp('ArenaB')}`);
  await rcon('minecraft:damage ArenaB 8 minecraft:generic'); await sleep(500);
  await rcon('minecraft:item replace entity ArenaA container.0 with minecraft:splash_potion[minecraft:potion_contents={potion:"minecraft:healing"}]'); await sleep(400);
  { const p = await t.poz('ArenaB'); await a.lookAt(new Vec3(p.x, p.y + 0.3, p.z), true); }
  h0 = await t.hp('ArenaB'); a.activateItem(); await sleep(1800);
  w.sprawdz('world-leczenie', (await t.hp('ArenaB')) > h0, 'mikstura leczenia na innym graczu dziala', `${h0} -> ${await t.hp('ArenaB')}`);
  // oswojony wilk gracza A
  await rcon('minecraft:summon minecraft:wolf 3.5 -60 4.5 {Tags:["qa_w"],Owner:[I;0,0,0,0]}');
  const uuidA = (await rcon('data get entity ArenaA UUID')).replace(/.*data: /, '').trim();
  await rcon(`minecraft:data modify entity @e[tag=qa_w,limit=1] Owner set value ${uuidA}`);
  await rcon('minecraft:effect give ArenaB minecraft:instant_health 1 5 true'); await sleep(500);
  h0 = await t.hp('ArenaB'); console.log(await rcon('minecraft:damage ArenaB 4 minecraft:mob_attack by @e[tag=qa_w,limit=1]')); await sleep(500);
  w.sprawdz('world-wilk', (await t.hp('ArenaB')) === h0, 'wilk gracza A nie rani gracza B w world', `${h0} -> ${await t.hp('ArenaB')}`);
  // mob dalej rani
  await rcon('minecraft:summon minecraft:zombie 3.5 -60 6.5 {NoAI:1b,Tags:["qa_w"]}');
  h0 = await t.hp('ArenaB'); await rcon('minecraft:damage ArenaB 4 minecraft:mob_attack by @e[type=minecraft:zombie,tag=qa_w,limit=1]'); await sleep(500);
  w.sprawdz('world-mob', (await t.hp('ArenaB')) < h0, 'zombie dalej rani gracza (kontrola)', `${h0} -> ${await t.hp('ArenaB')}`);
  await rcon('minecraft:kill @e[tag=qa_w]');
  // ring dalej dziala (po ciosie zombie B jest w walce MMOCore 10 s - wejscie odmowione)
  await sleep(11000);
  for (const n of ['ArenaA', 'ArenaB']) await t.wejdz(n);
  await t.tpArena('ArenaA', { ...RING, z: -2.5 }, 0); await t.tpArena('ArenaB', { ...RING, z: -0.5 }, 180); await sleep(6500);
  await rcon('minecraft:item replace entity ArenaA container.0 with minecraft:air');
  w.sprawdz('ring-wrecz', (await t.cios(a, 'ArenaB', 'ring wrecz')) > 0, 'w ringu PvP dziala');
  await rcon('minecraft:clear ArenaA minecraft:arrow'); await sleep(11000);
}, { limitMs: 200000 });
