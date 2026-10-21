'use strict';
// Kadencja ciosow broni z krwawieniem: N ciosow LPM co pelne naladowanie (+zapas), liczy ciosy, ktore nic nie zadaly.
//   run.sh <klon> kadencja.js "TYP:ID:odstep_ms,..."
// Obrazenia ciosu = spadek Health huska w 150 ms po ciosie (takt krwawienia co 1 s rzadko wpada w to okno).
const { scenariusz, czekaj, sleep } = require('/bot/lib');

const LISTA = (process.argv[2] || 'AXE:TOPOR_BOJOWY:1250').split(',').map(s => s.split(':'));
const NICK = 'BotKd' + (Date.now() % 100000);
const X = 0.5, Y = -59, Z = 0.5, N = 10;

scenariusz('kadencja', async ({ w, polacz, rcon }) => {
  await rcon('minecraft:gamerule spawn_mobs false');
  await rcon(`minecraft:fill ${X - 4} ${Y - 1} ${Z - 4} ${X + 4} ${Y - 1} ${Z + 6} minecraft:stone`);
  const bot = await polacz({ nick: NICK });
  await sleep(1500);
  await rcon(`minecraft:gamemode survival ${NICK}`);
  await rcon(`minecraft:teleport ${NICK} ${X} ${Y} ${Z} 0 0`);
  for (const b of ['topor', 'sierp', 'kosa', 'sztylet', 'miecz']) await rcon(`mmocore admin attribute give ${NICK} bieglosc-${b} 1`);
  const hp = async () => {
    const m = /([\d.]+)f/.exec(await rcon('minecraft:data get entity @e[type=husk,tag=kd_cel,limit=1] Health'));
    return m ? +m[1] : NaN;
  };
  for (const [typ, id, odstep] of LISTA) {
    await rcon('minecraft:kill @e[type=husk,tag=kd_cel]');
    await rcon(`minecraft:clear ${NICK}`);
    bot.setQuickBarSlot(0); await sleep(300);
    await rcon(`mi give ${typ} ${id} ${NICK} 1`);
    await sleep(1500);
    await rcon(`minecraft:summon husk ${X} ${Y} ${Z + 2.3} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["kd_cel"],attributes:[{id:"minecraft:max_health",base:2000}],Health:2000f}`);
    const cel = await czekaj(() => Object.values(bot.entities).find(e => e.name === 'husk'), 5000, 200);
    if (!cel) { w.sprawdz(`${id} cel`, false, 'brak huska'); continue; }
    await bot.lookAt(cel.position.offset(0, 1.2, 0), true);
    await rcon(`mmocore admin resource-stamina set ${NICK} 999`);
    await sleep(1500);
    const ciosy = [];
    const start = await hp();
    for (let i = 0; i < N; i++) {
      const t0 = Date.now();
      const h0 = await hp();
      bot.attack(cel);
      await sleep(150);
      ciosy.push(+(h0 - await hp()).toFixed(2));
      if (i % 3 === 2) await rcon(`mmocore admin resource-stamina set ${NICK} 999`);
      await sleep(Math.max(0, +odstep - (Date.now() - t0)));
    }
    await sleep(3500);   // dokrwawienie
    const razem = +(start - await hp()).toFixed(2);
    const zera = ciosy.filter(v => v <= 0.01).length;
    w.sprawdz(`${typ}:${id} co ${odstep} ms`, zera === 0, `ciosy ${ciosy.join('/')}; przepadlo ${zera}/${N}; razem z krwawieniem ${razem}`, { ciosy, razem, zera });
  }
  await rcon('minecraft:kill @e[type=husk,tag=kd_cel]');
}, { limitMs: 10 * 60 * 1000 });
