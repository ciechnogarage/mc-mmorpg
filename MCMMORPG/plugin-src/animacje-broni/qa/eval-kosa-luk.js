'use strict';
// Kosa: kazdy lekki cios ma trafic wszystkie cele w luku i zasiegu (deterministycznie), cel poza zasiegiem - nigdy.
//   run.sh <klon> kosa-luk.js [TYP:ID]
const { scenariusz, czekaj, sleep } = require('/bot/lib');
const [TYP, ID] = (process.argv[2] || 'KOSA:KOSA_SPACZONA').split(':');
const NICK = 'BotKl' + (Date.now() % 100000);
const X = 0.5, Y = -59, Z = 0.5;
// srodek 2,3 bl. przed graczem, boki +-1,5 bl. (w luku 180 st. i zasiegu 3,5), daleki 5,5 bl. (poza zasiegiem kosy, w 6,5 MMOItems)
const CELE = { srodek: [0, 2.3], lewy: [1.5, 2.0], prawy: [-1.5, 2.0], daleki: [0, 5.5] };

scenariusz('kosa-luk', async ({ w, polacz, rcon }) => {
  await rcon('minecraft:gamerule spawn_mobs false');
  await rcon(`minecraft:fill ${X - 4} ${Y - 1} ${Z - 4} ${X + 4} ${Y - 1} ${Z + 8} minecraft:stone`);
  const bot = await polacz({ nick: NICK });
  await sleep(1500);
  await rcon(`minecraft:gamemode survival ${NICK}`);
  await rcon(`minecraft:teleport ${NICK} ${X} ${Y} ${Z} 0 0`);
  await rcon(`mmocore admin attribute give ${NICK} bieglosc-kosa 1`);
  await rcon(`mmocore admin attribute give ${NICK} bieglosc-miecz 1`);
  await rcon('minecraft:kill @e[type=husk,tag=kl]');
  for (const [n, [dx, dz]] of Object.entries(CELE))
    await rcon(`minecraft:summon husk ${X + dx} ${Y} ${Z + dz} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["kl","kl_${n}"],attributes:[{id:"minecraft:max_health",base:2000}],Health:2000f}`);
  await rcon(`minecraft:clear ${NICK}`);
  bot.setQuickBarSlot(0); await sleep(300);
  await rcon(`mi give ${TYP} ${ID} ${NICK} 1`);
  await sleep(1500);
  const hp = async (n) => +(/([\d.]+)f/.exec(await rcon(`minecraft:data get entity @e[type=husk,tag=kl_${n},limit=1] Health`)) || [0, NaN])[1];
  const cel = await czekaj(() => Object.values(bot.entities).filter(e => e.name === 'husk')
    .sort((a, b) => a.position.distanceTo(bot.entity.position) - b.position.distanceTo(bot.entity.position))[0], 5000, 200);
  // srodkowy: najblizej osi z
  const srodek = Object.values(bot.entities).filter(e => e.name === 'husk').find(e => Math.abs(e.position.x - X) < 0.3 && e.position.z < Z + 3);
  await bot.lookAt(srodek.position.offset(0, 1.2, 0), true);
  await rcon(`mmocore admin resource-stamina set ${NICK} 999`);
  await sleep(1200);
  const wiersze = [];
  for (let i = 0; i < 5; i++) {
    const przed = {}; for (const n of Object.keys(CELE)) przed[n] = await hp(n);
    bot.attack(srodek);
    await sleep(300);
    const d = {}; for (const n of Object.keys(CELE)) d[n] = +(przed[n] - await hp(n)).toFixed(2);
    wiersze.push(d);
    await rcon(`mmocore admin resource-stamina set ${NICK} 999`);
    await sleep(3500);   // > 60 t krwawienia i pelne naladowanie: kazdy cios w tych samych warunkach
  }
  const opis = wiersze.map((d, i) => `${i + 1}: s${d.srodek} l${d.lewy} p${d.prawy} d${d.daleki}`).join(' | ');
  const boki = wiersze.every(d => d.lewy > 0 && d.prawy > 0);
  const daleki = wiersze.every(d => d.daleki <= 0.01);
  w.sprawdz(`${ID} luk-kazdy-cios`, boki, opis, { wiersze });
  w.sprawdz(`${ID} poza-zasiegiem-nic`, daleki, `daleki (5,5 bl.): ${wiersze.map(d => d.daleki).join('/')}`);
  await rcon('minecraft:kill @e[type=husk,tag=kl]');
}, { limitMs: 180000 });
