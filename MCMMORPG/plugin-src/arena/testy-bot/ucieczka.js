'use strict';
// A/E: wejscie (warunki), punkt powrotu, combat tag, ucieczki komendami i teleportem, wyjscie.
const { scenariusz } = require('/bot/lib');
const { narzedzia, RING, sleep } = require('./arena-lib');
scenariusz('arena-ucieczka', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const a = await polacz({ nick: 'ArenaA' }); const b = await polacz({ nick: 'ArenaB' });
  const czat = { ArenaA: [], ArenaB: [] };
  for (const bot of [a, b]) { bot.on('messagestr', m => czat[bot.username].push(m)); bot.on('actionBar', m => czat[bot.username].push(String(m?.toString?.() ?? m))); }
  await t.przygotuj();
  const wA = async () => (await t.wymiar('ArenaA'));
  // --- warunki wejscia ---
  await t.wWorld('ArenaA', 1.5); await t.wWorld('ArenaB', 4.5);
  await t.tpWorld('ArenaA', { x: 1.5, y: -40, z: 2.5 }); await sleep(250);
  await rcon('arena wejdz ArenaA'); await sleep(1200);
  w.sprawdz('wejscie-spadajac', !(await wA()).includes('arena'), 'w locie/spadajac wejscie odrzucone', await wA());
  await sleep(2500);
  await rcon('minecraft:execute in minecraft:the_nether run minecraft:fill -1 99 -1 1 99 1 minecraft:netherrack');
  await rcon('minecraft:execute in minecraft:the_nether run minecraft:teleport ArenaA 0.5 100 0.5'); await sleep(1500);
  await rcon('arena wejdz ArenaA'); await sleep(1200);
  w.sprawdz('wejscie-z-netheru', (await wA()).includes('nether'), 'z swiata spoza listy wejscie odrzucone', await wA());
  await rcon('minecraft:summon minecraft:zombie 3.5 -60 6.5 {NoAI:1b,Tags:["qa_ar"],PersistenceRequired:1b}');
  await t.tpWorld('ArenaA', { x: 1.5, y: -60, z: 5.5 }); await sleep(6500);
  const hz = async () => (await rcon('minecraft:data get entity @e[tag=qa_ar,limit=1] Health')).replace(/.*data: /, '');
  const hz0 = await hz(); await rcon('minecraft:damage @e[tag=qa_ar,limit=1] 2 minecraft:player_attack by ArenaA'); console.log('zombie HP', hz0, '->', await hz());
  await sleep(300); await rcon('arena wejdz ArenaA'); await sleep(1200);
  w.sprawdz('wejscie-w-walce-pve', !(await wA()).includes('arena'), 'zaraz po ciosie w moba (walka MMOCore) wejscie odrzucone', await wA());
  await rcon('minecraft:kill @e[tag=qa_ar]');
  await t.tpWorld('ArenaA', { x: 1.5, y: -60, z: 2.5 }); await sleep(12000);   // koniec walki MMOCore
  const p0 = await t.poz('ArenaA');
  w.sprawdz('wejscie-ok', await t.wejdz('ArenaA'), 'wejscie z world po walce');
  const sp = await t.poz('ArenaA');
  w.sprawdz('platforma', Math.abs(sp.x - 0.5) < 1 && Math.abs(sp.z + 28.5) < 1, 'laduje na platformie areny', JSON.stringify(sp));
  await rcon('arena wejdz ArenaA'); await sleep(800);
  await t.wejdz('ArenaB');
  // --- walka i tag ---
  await t.tpArena('ArenaA', { ...RING, z: -2.5 }, 0); await t.tpArena('ArenaB', { ...RING, z: -0.5 }, 180); await sleep(6500);
  const d = await t.cios(a, 'ArenaB', 'cios');
  w.sprawdz('cios-w-ringu', d > 0, 'cios w ringu rani', d);
  for (const n of ['ArenaA', 'ArenaB']) czat[n].length = 0;
  await rcon('arena wyjdz ArenaA'); await sleep(800);
  w.sprawdz('tag-wyjdz', (await wA()).includes('arena'), 'w walce wyjscie z menu odrzucone', czat.ArenaA.filter(m => /Walka/.test(m)).slice(0, 1).join());
  await rcon('arena wyjdz ArenaB'); await sleep(800);
  w.sprawdz('tag-ofiara', (await t.wymiar('ArenaB')).includes('arena'), 'zaatakowany tez nie ucieka');
  await t.tpWorld('ArenaA', { x: 1.5, y: -60, z: 2.5 }); await sleep(800);
  w.sprawdz('tag-teleport', (await wA()).includes('arena'), 'teleport innego pluginu/konsoli z areny zablokowany');
  for (const k of ['/spawn', '/cmi spawn', '/back', '/cmi back', '/home', '/warp stolica', '/tpa ArenaB', '/md play loch_001', '/ksiegaportal', '/stolica', '/menu', '/kill', '/suicide', '/cmi suicide']) {
    a.chat(k); await sleep(700);
    const wy = await wA(); if (!wy.includes('arena')) { w.sprawdz('komenda ' + k, false, 'komenda wyprowadzila z areny', wy); await t.wejdz('ArenaA'); }
  }
  w.sprawdz('komendy', (await wA()).includes('arena'), 'zadna komenda gracza nie wyprowadza z areny');
  console.log('czat A po komendach:', JSON.stringify(czat.ArenaA.slice(-8)));
  await sleep(11000);
  // --- wyjscie po walce ---
  await rcon('arena wyjdz ArenaA'); await sleep(1500);
  const p1 = await t.poz('ArenaA');
  w.sprawdz('wyjscie-punkt', (await wA()).includes('overworld') && Math.hypot(p1.x - p0.x, p1.z - p0.z) < 1 && Math.abs(p1.y - p0.y) < 1, 'po walce wyjscie wraca dokladnie do punktu wejscia', `${JSON.stringify(p0)} -> ${JSON.stringify(p1)}`);
  // punkt powrotu zablokowany blokiem
  await t.wejdz('ArenaA');
  await rcon('minecraft:execute in minecraft:overworld run minecraft:fill 1 -60 2 1 -59 2 minecraft:stone');
  await rcon('arena wyjdz ArenaA'); await sleep(1500);
  const p2 = await t.poz('ArenaA');
  w.sprawdz('powrot-zamurowany', !(await wA()).includes('arena') && !(Math.floor(p2.x) === 1 && Math.floor(p2.z) === 2 && p2.y < -58), 'gdy punkt zamurowany: zapasowy spawn, nie w bloku', `${await wA()} ${JSON.stringify(p2)}`);
  await rcon('minecraft:execute in minecraft:overworld run minecraft:fill 1 -60 2 1 -59 2 minecraft:air');
  // bez walki: obcy teleport z areny
  await t.wejdz('ArenaA'); await sleep(3500);
  await t.tpWorld('ArenaA', { x: 1.5, y: -60, z: 2.5 }); await sleep(800);
  w.sprawdz('teleport-bez-walki', (await wA()).includes('overworld'), 'bez walki obcy teleport z areny przepuszczony (portal, podroz)', await wA());
  await rcon('arena wyjdz ArenaA'); await rcon('arena wyjdz ArenaB'); await sleep(1000);
  await rcon('arena wyjdz ArenaA'); await sleep(500);
  console.log('INFO wyjdz gdy nie na arenie: ', JSON.stringify(czat.ArenaA.slice(-2)));
}, { limitMs: 300000 });
