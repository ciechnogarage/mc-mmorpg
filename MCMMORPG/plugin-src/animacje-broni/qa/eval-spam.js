'use strict';
// Spam LPM 8 CPS przez 3 s w powietrze: ile staminy schodzi (cel: niedoladowane klikniecia nie zjadaja paska w 2-3 s).
const { scenariusz, sleep } = require('/bot/lib');
const NICK = 'BotSp' + (Date.now() % 100000);
scenariusz('spam', async ({ w, polacz, rcon }) => {
  const bot = await polacz({ nick: NICK });
  await sleep(1500);
  await rcon(`minecraft:gamemode survival ${NICK}`);
  await rcon(`mmocore admin attribute give ${NICK} bieglosc-miecz 1`);
  await rcon(`minecraft:clear ${NICK}`); bot.setQuickBarSlot(0); await sleep(300);
  await rcon(`mi give SWORD MIECZ_ZELAZNY ${NICK} 1`); await sleep(1500);
  const stan = async () => { const m = /lekki=([\d.]+) stamina=([\d.]+)\/(\d+)/.exec(await rcon(`animacjebroni stamina gracz ${NICK}`)); return { lekki: +m[1], st: +m[2], max: +m[3] }; };
  await rcon(`mmocore admin resource-stamina set ${NICK} 999`); await sleep(1500);
  const s0 = await stan();
  for (let i = 0; i < 24; i++) { bot.swingArm('right'); await sleep(125); }
  const s1 = await stan();
  w.sprawdz('spam-8cps-3s', s0.st - s1.st < 0.5 * s0.max, `pasek ${s0.st.toFixed(1)} -> ${s1.st.toFixed(1)} / ${s0.max} (24 klikniecia, lekki ${s0.lekki})`);
  await sleep(2500);
  const s2 = await stan();
  for (let i = 0; i < 4; i++) { bot.swingArm('right'); await sleep(800); }
  const s3 = await stan();
  w.sprawdz('pelne-ciosy-pelny-koszt', Math.abs((s2.st - s3.st) - 4 * s0.lekki) < 0.6, `4 pelne machniecia: ${(s2.st - s3.st).toFixed(2)}, oczekiwane ~${(4 * s0.lekki).toFixed(2)}`);
}, { limitMs: 120000 });
