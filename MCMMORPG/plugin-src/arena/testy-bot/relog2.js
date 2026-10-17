'use strict';
const { scenariusz } = require('/bot/lib');
const { narzedzia, sleep } = require('./arena-lib');
scenariusz('arena-relog2', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const b = await polacz({ nick: 'ArenaB' });
  await t.przygotuj();
  await t.wWorld('ArenaB', 4.5); console.log('T1 po wWorld', await t.wymiar('ArenaB'), JSON.stringify(await t.poz('ArenaB')));
  const r = await rcon('arena wejdz ArenaB'); await sleep(1500); console.log('T2 po wejdz', JSON.stringify(r), await t.wymiar('ArenaB'));
  await sleep(3000); b.quit(); await sleep(3000); console.log('T3 wyszedl');
  const b2 = await polacz({ nick: 'ArenaB' }); await sleep(4000);
  console.log('T4 po relogu', await t.wymiar('ArenaB'), JSON.stringify(await t.poz('ArenaB')));
  await rcon('arena wyjdz ArenaB'); await sleep(1500);
  const pw = await t.poz('ArenaB'); console.log('T5 po wyjdz', await t.wymiar('ArenaB'), JSON.stringify(pw));
  w.sprawdz('relog-wyjscie', Math.abs(pw.x - 4.5) < 1, 'po relogu wyjscie do punktu wejscia', JSON.stringify(pw));
}, { limitMs: 120000 });
