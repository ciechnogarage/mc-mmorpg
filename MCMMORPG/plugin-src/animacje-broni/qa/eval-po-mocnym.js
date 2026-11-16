'use strict';
// LPM zaraz po trafionym mocnym (PPM ~1,3 s): czy cios wchodzi (kosa/topor dwureczny maja minimum_attack_charge 0,8).
//   run.sh <klon> po-mocnym.js "TYP:ID,..."
const { scenariusz, czekaj, sleep } = require('/bot/lib');
const LISTA = (process.argv[2] || 'KOSA:KOSA_SPACZONA').split(',').map(s => s.split(':'));
const NICK = 'BotPm' + (Date.now() % 100000);
const X = 0.5, Y = -59, Z = 0.5;

scenariusz('po-mocnym', async ({ w, polacz, rcon }) => {
  await rcon('minecraft:gamerule spawn_mobs false');
  await rcon(`minecraft:fill ${X - 4} ${Y - 1} ${Z - 4} ${X + 4} ${Y - 1} ${Z + 6} minecraft:stone`);
  const bot = await polacz({ nick: NICK });
  await sleep(1500);
  await rcon(`minecraft:gamemode survival ${NICK}`);
  for (const b of ['kosa', 'topor', 'miecz', 'mlot-dwureczny', 'bulawa']) await rcon(`mmocore admin attribute give ${NICK} bieglosc-${b} 1`);
  const hp = async () => +(/([\d.]+)f/.exec(await rcon('minecraft:data get entity @e[type=husk,tag=pm,limit=1] Health')) || [0, NaN])[1];
  for (const [typ, id] of LISTA) {
    await rcon('minecraft:kill @e[type=husk,tag=pm]');
    await rcon(`minecraft:clear ${NICK}`);
    await rcon(`minecraft:teleport ${NICK} ${X} ${Y} ${Z} 0 0`);
    bot.setQuickBarSlot(0); await sleep(300);
    await rcon(`mi give ${typ} ${id} ${NICK} 1`);
    await sleep(1500);
    await rcon(`minecraft:summon husk ${X} ${Y} ${Z + 2.3} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["pm"],attributes:[{id:"minecraft:max_health",base:2000}],Health:2000f}`);
    const cel = await czekaj(() => Object.values(bot.entities).find(e => e.name === 'husk'), 5000, 200);
    await bot.lookAt(cel.position.offset(0, 1.2, 0), true);
    await rcon(`mmocore admin resource-stamina set ${NICK} 999`);
    await sleep(2000);
    const h0 = await hp();
    bot.activateItem(); await sleep(1300); bot.deactivateItem(); await sleep(250);
    const h1 = await hp();
    bot.attack(cel); await sleep(300);
    const h2 = await hp();
    w.sprawdz(`${id} LPM-po-mocnym`, h0 - h1 > 0 && h1 - h2 > 0,
      `mocny ${(h0 - h1).toFixed(2)}, LPM 0,55 s pozniej ${(h1 - h2).toFixed(2)}`);
  }
  await rcon('minecraft:kill @e[type=husk,tag=pm]');
}, { limitMs: 200000 });
