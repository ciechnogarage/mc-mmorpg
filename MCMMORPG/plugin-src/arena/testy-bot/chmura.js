'use strict';
const { scenariusz } = require('/bot/lib');
const { narzedzia, RING, sleep } = require('./arena-lib');
const { Vec3 } = require('vec3');
scenariusz('arena-chmura', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const a = await polacz({ nick: 'ArenaA' });
  await t.przygotuj();
  if (!(await t.wymiar('ArenaA')).includes('arena')) { await t.wWorld('ArenaA'); await t.wejdz('ArenaA'); }
  await t.tpArena('ArenaA', { ...RING, z: -10.5 }, 0); await sleep(6000);
  console.log(await rcon('minecraft:item replace entity ArenaA container.0 with minecraft:lingering_potion[minecraft:potion_contents={potion:"minecraft:strong_harming"}]'));
  await sleep(600); a.setQuickBarSlot(0); await sleep(400);
  console.log('trzyma:', a.heldItem?.name, '| serwer:', (await rcon('data get entity ArenaA SelectedItem.id')).replace(/.*data: /, ''));
  let rzucone = 0; a.on('entitySpawn', e => { if (/potion|cloud/.test(e.name || '')) { rzucone++; console.log('encja:', e.name); } });
  await a.lookAt(new Vec3(0.5, 64, -6.5), true); a.activateItem(); await sleep(2500);
  console.log('chmury:', await rcon('minecraft:execute in minecraft:arena run minecraft:execute if entity @e[type=minecraft:area_effect_cloud]'), '| zostalo:', (await rcon('data get entity ArenaA SelectedItem.id')).replace(/.*data: /, ''));
  w.sprawdz('rzut', rzucone > 0, 'mikstura pozostajaca zostala rzucona', rzucone);
}, { limitMs: 60000 });
