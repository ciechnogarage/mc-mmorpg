'use strict';
// Luka 1: ta sama druzyna MMOCore / ta sama gildia MMOCore w ringu - czy moga sie bic. Druzyna i gildia przez GUI jak gracz.
const { scenariusz } = require('/bot/lib');
const { narzedzia, RING, sleep, czekaj } = require('./arena-lib');
scenariusz('arena-druzyna', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const a = await polacz({ nick: 'ArenaA' }); const b = await polacz({ nick: 'ArenaB' });
  const czat = { ArenaA: [], ArenaB: [] };
  for (const bot of [a, b]) bot.on('messagestr', m => czat[bot.username].push(m));
  await t.przygotuj();
  const ost = n => JSON.stringify(czat[n].filter(m => m.trim()).slice(-3));
  const stan = async () => (await rcon('papi parse ArenaB party=%mmocore_party_count% gildia=%mmocore_guild_name%')).trim();
  const okno = async (bot, komenda) => { const p = new Promise(r => bot.once('windowOpen', r)); bot.chat(komenda); return Promise.race([p, sleep(4000).then(() => null)]); };
  const klik = async (bot, slot) => { await bot.clickWindow(slot, 0, 0).catch(e => console.log('klik', e.message)); await sleep(1000); };
  const ring = async opis => {
    for (const n of ['ArenaA', 'ArenaB']) if (!(await t.wymiar(n)).includes('arena')) { await t.wWorld(n, n === 'ArenaA' ? 1.5 : 4.5); await t.wejdz(n); }
    await t.tpArena('ArenaA', { ...RING, z: -2.5 }, 0); await t.tpArena('ArenaB', { ...RING, z: -0.5 }, 180); await sleep(6500); await t.uleczy('ArenaB');
    return t.cios(a, 'ArenaB', opis);
  };
  // kontrola bez druzyny
  w.sprawdz('kontrola', (await ring('bez druzyny')) > 0, 'kontrola: obcy bija sie w ringu');
  await sleep(11000);
  // druzyna
  console.log('okno party:', !!(await okno(a, '/party'))); await klik(a, 23);
  console.log('A po create:', ost('ArenaA'), '| okno view:', a.currentWindow?.title?.slice?.(0, 20));
  if (!a.currentWindow) await okno(a, '/party');
  await klik(a, 27); a.chat('ArenaB'); await sleep(1500);
  console.log('A:', ost('ArenaA'), '| B:', ost('ArenaB'));
  b.chat('/party accept'); await sleep(1500);
  console.log('stan:', await stan());
  w.sprawdz('w-druzynie', /party=2/.test(await stan()), 'A i B w jednej druzynie', await stan());
  const dd = await ring('ta sama druzyna');
  w.sprawdz('druzyna-ring', dd > 0, 'ta sama druzyna: cios w ringu rani (pojedynek przyjaciol)', dd);
  a.closeWindow?.(a.currentWindow); b.chat('/party'); await sleep(1500); if (b.currentWindow) await klik(b, 29); await sleep(11000);
  console.log('stan po wyjsciu:', await stan());
  // gildia
  console.log('okno guild:', !!(await okno(a, '/guild'))); await klik(a, 23);
  console.log('A po create:', ost('ArenaA')); a.chat('TST'); await sleep(1200); console.log('A:', ost('ArenaA')); a.chat('Testowa'); await sleep(1500); console.log('A:', ost('ArenaA'));
  if (!a.currentWindow) await okno(a, '/guild');
  await klik(a, 27); a.chat('ArenaB'); await sleep(1500); console.log('A:', ost('ArenaA'), '| B:', ost('ArenaB'));
  b.chat('/guild accept'); await sleep(1500);
  console.log('stan:', await stan());
  w.sprawdz('w-gildii', !/gildia=(None|$|%)/.test(await stan()), 'B w gildii A', await stan());
  const dg = await ring('ta sama gildia');
  w.sprawdz('gildia-ring', dg > 0, 'ta sama gildia: cios w ringu rani', dg);
  await sleep(11000);
}, { limitMs: 260000 });
