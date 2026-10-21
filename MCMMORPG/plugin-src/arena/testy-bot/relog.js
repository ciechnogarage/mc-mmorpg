'use strict';
// J: wylogowanie w walce = poddanie (platforma, 60 s bez ringu); relog bez walki; gdzie laduje po relogu.
const { scenariusz, polacz: _p } = require('/bot/lib');
const { narzedzia, RING, sleep, czekaj } = require('./arena-lib');
scenariusz('arena-relog', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  let a = await polacz({ nick: 'ArenaA' }); const b = await polacz({ nick: 'ArenaB' });
  const czatB = []; b.on('messagestr', m => czatB.push(m));
  await t.przygotuj();
  for (const n of ['ArenaA', 'ArenaB']) if (!(await t.wymiar(n)).includes('arena')) { await t.wWorld(n, n === 'ArenaA' ? 1.5 : 4.5); await t.wejdz(n); }
  await t.tpArena('ArenaA', { ...RING, z: -2.5 }, 0); await t.tpArena('ArenaB', { ...RING, z: -0.5 }, 180); await sleep(6500);
  w.sprawdz('cios', (await t.cios(a, 'ArenaB', 'cios przed wylogowaniem')) > 0, 'walka trwa (tag)');
  a.quit(); await sleep(2500);
  w.sprawdz('komunikat-poddania', czatB.some(m => /poddał walkę/.test(m)), 'przeciwnik dostaje komunikat o poddaniu', JSON.stringify(czatB.slice(-2)));
  a = await polacz({ nick: 'ArenaA' }); const czatA = []; a.on('messagestr', m => czatA.push(m)); a.on('actionBar', m => czatA.push(String(m?.toString?.() ?? m)));
  await sleep(3000);
  const p = await t.poz('ArenaA');
  console.log('A po relogu w walce:', await t.wymiar('ArenaA'), JSON.stringify(p));
  w.sprawdz('relog-poza-arena', (await t.wymiar('ArenaA')).includes('overworld') && Math.abs(p.x - 1.5) < 1 && Math.abs(p.z - 2.5) < 1, 'po wylogowaniu w walce wraca do punktu wejscia (wyniesiony przy wyjsciu)', `${await t.wymiar('ArenaA')} ${JSON.stringify(p)}`);
  await sleep(11000);
  w.sprawdz('wejscie-po-poddaniu', await t.wejdz('ArenaA'), 'moze znow wejsc na arene');
  await t.tpArena('ArenaA', RING); await sleep(2500);
  const p2 = await t.poz('ArenaA');
  w.sprawdz('zakaz-ringu', Math.hypot(p2.x, p2.z) > 19.5, 'do 60 s od poddania nie wejdzie do ringu', JSON.stringify(p2));
  await rcon('arena wyjdz ArenaA'); await sleep(1500);
  // relog bez walki na trybunach
  await sleep(11000);
  await rcon('arena wyjdz ArenaB'); await sleep(1500);
  await t.wWorld('ArenaB', 4.5); await t.wejdz('ArenaB'); await sleep(3500);
  b.quit(); await sleep(2000);
  const b2 = await polacz({ nick: 'ArenaB' }); const czatB2 = []; b2.on('messagestr', m => czatB2.push(m)); b2.on('actionBar', m => czatB2.push(String(m?.toString?.() ?? m)));
  await sleep(4000);
  console.log('B po relogu bez walki:', await t.wymiar('ArenaB'), JSON.stringify(await t.poz('ArenaB')), 'komunikaty:', JSON.stringify(czatB2.filter(m => m.trim()).slice(-4)));
  const pw = await t.poz('ArenaB');
  w.sprawdz('relog-wyjscie', (await t.wymiar('ArenaB')).includes('overworld'), 'relog bez walki: gracz jest w punkcie wejscia, nie na arenie', `${await t.wymiar('ArenaB')} ${JSON.stringify(pw)}`);
  console.log('komunikaty A:', JSON.stringify(czatA.filter(m => m.trim()).slice(-5)));
}, { limitMs: 200000 });
