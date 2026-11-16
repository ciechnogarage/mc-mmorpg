'use strict';
// C/B: smierc na arenie bez kary i bez nagrody; kontrola: smierc w world nadal karana. + teleport bez walki.
const { scenariusz } = require('/bot/lib');
const { narzedzia, RING, sleep, czekaj } = require('./arena-lib');
scenariusz('arena-smierc', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const a = await polacz({ nick: 'ArenaA' }); const b = await polacz({ nick: 'ArenaB' });
  const czat = { ArenaA: [], ArenaB: [] };
  for (const bot of [a, b]) bot.on('messagestr', m => czat[bot.username].push(m));
  await t.przygotuj();
  const diamenty = async n => ((await rcon(`execute if items entity ${n} container.* minecraft:diamond`)).match(/count: (\d+)/i) || [0, 0])[1];
  const pech = async n => /unluck/.test(await t.efekty(n));
  const expUstaw = async n => { await rcon(`mmocore admin level set ${n} main 5`); await rcon(`mmocore admin exp set ${n} main 50`); };
  for (const n of ['ArenaA', 'ArenaB']) { await t.wWorld(n, n === 'ArenaA' ? 1.5 : 4.5); await expUstaw(n); await rcon(`minecraft:effect clear ${n}`); }
  await rcon('minecraft:clear ArenaB minecraft:diamond'); await rcon('minecraft:give ArenaB minecraft:diamond 3');
  // teleport konsoli BEZ walki (stoi na platformie): zablokowany?
  await t.wejdz('ArenaA'); await sleep(1000);
  console.log('A po wejsciu:', await t.wymiar('ArenaA'), JSON.stringify(await t.poz('ArenaA')));
  await t.tpWorld('ArenaA', { x: 1.5, y: -60, z: 2.5 }); await sleep(800);
  console.log('INFO teleport konsoli bez walki ->', await t.wymiar('ArenaA'));
  if (!(await t.wymiar('ArenaA')).includes('arena')) await t.wejdz('ArenaA');
  await t.wejdz('ArenaB');
  const e0 = { A: await t.exp('ArenaA'), B: await t.exp('ArenaB') };
  await t.tpArena('ArenaA', { ...RING, z: -2.5 }, 0); await t.tpArena('ArenaB', { ...RING, z: -0.5 }, 180); await sleep(6500);
  for (const n of ['ArenaA', 'ArenaB']) czat[n].length = 0;
  await t.cios(a, 'ArenaB', 'pierwszy cios (tag)');
  await rcon('minecraft:damage ArenaB 500 minecraft:player_attack by ArenaA'); await sleep(2500);
  const umarl = await czekaj(async () => (await t.hp('ArenaB')) > 0 ? true : null, 8000, 500);
  await sleep(1500);
  const pB = await t.poz('ArenaB');
  console.log('B po smierci:', await t.wymiar('ArenaB'), JSON.stringify(pB), 'czat A:', JSON.stringify(czat.ArenaA.slice(-3)), 'czat B:', JSON.stringify(czat.ArenaB.slice(-3)));
  w.sprawdz('respawn-arena', (await t.wymiar('ArenaB')).includes('arena') && pB && Math.abs(pB.z + 28.5) < 2, 'odradza sie na platformie areny', JSON.stringify(pB));
  w.sprawdz('ekwipunek', (await diamenty('ArenaB')) === '3', 'ekwipunek zostaje', await diamenty('ArenaB'));
  w.sprawdz('bez-pecha', !(await pech('ArenaB')), 'bez debuffa kary smierci');
  const e1 = { A: await t.exp('ArenaA'), B: await t.exp('ArenaB') };
  w.sprawdz('exp-ofiary', e1.B === e0.B, 'EXP ofiary bez straty', `${e0.B} -> ${e1.B}`);
  w.sprawdz('exp-zabojcy', e1.A === e0.A, 'zabojca nie dostaje EXP (farma altem)', `${e0.A} -> ${e1.A}`);
  w.sprawdz('komunikat', czat.ArenaA.some(m => /pokonał ArenaB/.test(m)), 'ogloszenie zwyciestwa na arenie', JSON.stringify(czat.ArenaA.slice(-3)));
  const lup = await rcon('minecraft:execute in minecraft:arena run minecraft:execute if entity @e[type=minecraft:item,x=0,y=64,z=0,distance=..30]');
  w.sprawdz('bez-lupu', !/passed/.test(lup), 'nic nie wypada na ring', lup);
  // ochrona po odrodzeniu: 3 s
  await sleep(11000);
  // kontrola: smierc w world nadal karana
  await rcon('arena wyjdz ArenaB'); await sleep(1500); await t.wWorld('ArenaB', 4.5);
  await expUstaw('ArenaB'); const ew0 = await t.exp('ArenaB');
  await rcon('minecraft:kill ArenaB'); await sleep(4000);
  const ew1 = await t.exp('ArenaB');
  console.log('smierc w world: exp', ew0, '->', ew1, 'pech', await pech('ArenaB'));
  w.sprawdz('kara-poza-arena', (await pech('ArenaB')) || ew1 !== ew0, 'poza arena kara smierci nadal dziala (pech lub strata EXP)', `${ew0} -> ${ew1}`);
  await rcon('minecraft:effect clear ArenaB'); await rcon('minecraft:clear ArenaB minecraft:diamond');
  await rcon('arena wyjdz ArenaA'); await sleep(11000);
}, { limitMs: 240000 });
