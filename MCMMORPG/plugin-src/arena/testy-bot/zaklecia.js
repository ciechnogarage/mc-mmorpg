'use strict';
// H: zaklecia na graczy: pocisk i kula ognia w ringu (rani), przez line (zero), przywolaniec (zero).
const { scenariusz } = require('/bot/lib');
const { narzedzia, RING, TRYBUNY, W_RINGU, sleep } = require('./arena-lib');
const { Vec3 } = require('vec3');
scenariusz('arena-zaklecia', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const a = await polacz({ nick: 'ArenaMag' }); const b = await polacz({ nick: 'ArenaB' });
  await t.przygotuj();
  for (const n of ['ArenaMag', 'ArenaB']) if (!(await t.wymiar(n)).includes('arena')) { await t.wWorld(n, n === 'ArenaMag' ? 1.5 : 4.5); await t.wejdz(n); }
  await rcon('minecraft:item replace entity ArenaMag container.0 with minecraft:air');
  await rcon('mmocore admin class ArenaMag MAG_AKADEMII'); await rcon('mmocore admin level set ArenaMag main 30'); await sleep(800); console.log('klasa:', await rcon('papi parse ArenaMag %mmocore_class% %mmocore_mana%/%mmocore_max_mana%'));
  const rzuc = async (zakl, opis, pa, pb, yaw = 0) => {
    console.log('przypisz', zakl, (await rcon(`zaklecia przypisz ArenaMag ${zakl} lewa`)).trim().slice(0, 80));
    await t.tpArena('ArenaMag', pa, yaw); await t.tpArena('ArenaB', pb, 180); await sleep(6500); await t.uleczy('ArenaB');
    a.setQuickBarSlot(0); const pc = await t.poz('ArenaB'); await a.lookAt(new Vec3(pc.x, pc.y + 1.0, pc.z), true); await sleep(300);
    await rcon('mmocore admin resource-mana set ArenaMag 1000');
    const h0 = await t.hp('ArenaB');
    for (let k = 0; k < 6; k++) { a.activateItem(false); a.activateItem(true); await sleep(200); } a.deactivateItem(); await sleep(2500);
    const h1 = await t.hp('ArenaB'); console.log(opis, 'HP', h0, '->', h1); return +(h0 - h1).toFixed(2);
  };
  /*KULA*/  w.sprawdz('kula-ring', (await rzuc('kula_ognia', 'kula ring->ring', { ...RING, z: -4.5 }, { ...RING, z: 1.5 })) > 0, 'kula ognia rani w ringu');
  await sleep(11000);
  // kula wybucha przy linie (cel w ringu przy krawedzi), widz tuz za lina
  const dw = await (async () => {
    await t.tpArena('ArenaMag', { ...RING, z: -10.5 }, 180); await t.tpArena('ArenaB', TRYBUNY, 180); await sleep(6500); await t.uleczy('ArenaB');
    a.setQuickBarSlot(0); await a.lookAt(new Vec3(0.5, 64.2, -19.4), true); await rcon('mmocore admin resource-mana set ArenaMag 1000');
    const h0 = await t.hp('ArenaB'); for (let k = 0; k < 6; k++) { a.activateItem(false); a.activateItem(true); await sleep(200); } a.deactivateItem(); await sleep(2500);
    const h1 = await t.hp('ArenaB'); console.log('kula w line, widz za lina HP', h0, '->', h1); return h0 - h1;
  })();
  w.sprawdz('kula-odprysk-za-line', dw === 0, 'wybuch kuli przy linie nie rani widza', dw);
  await sleep(11000);
  w.sprawdz('pocisk-ring', (await rzuc('ognisty_pocisk', 'pocisk ring->ring', { ...RING, z: -4.5 }, { ...RING, z: 1.5 })) > 0, 'ognisty pocisk rani w ringu');
  await sleep(11000);
  w.sprawdz('pocisk-ring-trybuny', (await rzuc('ognisty_pocisk', 'pocisk ring->trybuny', { ...RING, z: -14.5 }, TRYBUNY, 180)) === 0, 'pocisk z ringu w widza: zero');
  w.sprawdz('pocisk-trybuny-ring', (await rzuc('ognisty_pocisk', 'pocisk trybuny->ring', TRYBUNY, { ...RING, z: -12.5 })) === 0, 'pocisk z trybun w ring: zero');
  await sleep(11000);
  // przywolaniec
  console.log('przypisz', (await rcon('zaklecia przypisz ArenaMag duch_wilka lewa')).trim().slice(0, 80));
  await t.tpArena('ArenaMag', { ...RING, z: -4.5 }, 0); await t.tpArena('ArenaB', { ...RING, z: 1.5 }, 180); await sleep(6500); await t.uleczy('ArenaB');
  a.setQuickBarSlot(0); const pc = await t.poz('ArenaB'); await a.lookAt(new Vec3(pc.x, pc.y + 1, pc.z), true); await rcon('mmocore admin resource-mana set ArenaMag 1000');
  const h0 = await t.hp('ArenaB'); for (let k = 0; k < 6; k++) { a.activateItem(false); a.activateItem(true); await sleep(200); } a.deactivateItem();
  const e = a.players.ArenaB?.entity; if (e) { await a.lookAt(e.position.offset(0, 1.4, 0), true); a.attack(e); }
  await sleep(9000); const h1 = await t.hp('ArenaB');
  const wilki = await rcon('minecraft:execute in minecraft:arena run minecraft:execute if entity @e[type=!minecraft:player,type=!minecraft:iron_golem,type=!minecraft:item_display,type=!minecraft:text_display,type=!minecraft:interaction,x=0,y=64,z=0,distance=..20]');
  console.log('przywolaniec: HP B', h0, '->', h1, '| byty w ringu:', wilki);
  console.log('INFO przywolaniec w ringu (obaj w ringu = PvP wlasciciela):', h0, '->', h1);
  await rcon('zaklecia przypisz ArenaMag nic lewa'); await sleep(11000);
}, { limitMs: 300000 });
