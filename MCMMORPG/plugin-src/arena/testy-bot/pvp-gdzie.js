'use strict';
// Gdzie dziala PvP: world (oczekiwane: NIE, WG deny) vs ring areny (oczekiwane: TAK). A bije B, HP B z serwera.
const { scenariusz, sleep, czekaj } = require('/bot/lib');
scenariusz('pvp-gdzie', async ({ w, polacz, rcon }) => {
  const a = await polacz({ nick: 'PvpA' }); const b = await polacz({ nick: 'PvpB' });
  for (const g of ['fall_damage false','spawn_monsters false','spawn_mobs false','advance_time false']) await rcon(`minecraft:gamerule ${g}`);
  await rcon('minecraft:time set 6000'); await rcon('minecraft:kill @e[type=!player,type=!item_display,type=!text_display,type=!interaction,type=!iron_golem]');
  if (!a.entity || !b.entity) throw new Error('brak encji');
  const hp = async n => { const m = (await rcon(`data get entity ${n} Health`)).match(/([\d.]+)f/); return m ? +m[1] : null; };
  const wymiar = async n => (await rcon(`data get entity ${n} Dimension`)).replace(/.*data: /, '');
  const cios = async opis => {
    await rcon('minecraft:effect give PvpB minecraft:instant_health 1 5 true'); await sleep(800);
    const e = await czekaj(() => a.players.PvpB?.entity || null, 5000, 200);
    if (!e) { console.log(opis, 'B niewidoczny dla A'); return null; }
    await a.lookAt(e.position.offset(0, 1.4, 0), true); await sleep(900);
    const h0 = await hp('PvpB'); a.attack(e); await sleep(800); const h1 = await hp('PvpB');
    console.log(opis, '| HP B', h0, '->', h1, '| wymiar A', await wymiar('PvpA'), '| A', a.entity.position.floored(), 'B', b.entity.position.floored());
    return h0 - h1;
  };
  // world: platforma na stalych wspolrzednych, teleport, 6,5 s na nietykalnosc CMI po teleporcie
  for (const n of ['PvpA', 'PvpB']) console.log('wyjdz', n, await rcon(`arena wyjdz ${n}`)); await sleep(2000);
  console.log(await rcon('minecraft:execute in minecraft:overworld run minecraft:fill 0 -61 0 6 -61 4 minecraft:stone'));
  console.log(await rcon('minecraft:execute in minecraft:overworld run minecraft:teleport PvpA 1.5 -60 2.5 -90 0'));
  console.log(await rcon('minecraft:execute in minecraft:overworld run minecraft:teleport PvpB 3.5 -60 2.5 90 0'));
  await sleep(6500);
  const dw = await cios('world');
  for (const n of ['PvpA', 'PvpB']) console.log('wejdz', n, await rcon(`arena wejdz ${n}`)); await sleep(4000);
  console.log(await rcon('minecraft:execute in minecraft:arena run minecraft:teleport PvpA 0.5 64 0.5 -90 0'));
  console.log(await rcon('minecraft:execute in minecraft:arena run minecraft:teleport PvpB 2.5 64 0.5 90 0'));
  await sleep(6500);
  const wa = await wymiar('PvpA'), wb = await wymiar('PvpB');
  w.sprawdz('oba-w-arenie', /arena/.test(wa) && /arena/.test(wb), 'po wejsciu obaj w swiecie arena', wa + ' ' + wb);
  const dr = await cios('ring');
  w.sprawdz('world-bez-pvp', dw === 0, 'w world PvP zablokowane (WG pvp deny)', dw);
  w.sprawdz('ring-pvp', dr > 0, 'w ringu areny PvP dziala', dr);
}, { limitMs: 120000 });
