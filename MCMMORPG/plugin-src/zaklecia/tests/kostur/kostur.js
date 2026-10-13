'use strict';
// Kostur = ognisko zaklec (13.10): kostur w prawej + jedno zaklecie w lewej (PPM), premia MAGIC_DAMAGE tylko z kosturem.
const { scenariusz, sleep } = require('/bot/lib');

scenariusz('kostur', async ({ w, polacz, rcon }) => {
  const nick = 'BotKostur';
  const bot = await polacz({ nick });
  const ok = async (slot, pred) => (await rcon(`execute if items entity ${nick} ${slot} ${pred}`)).includes('passed');
  const id = x => `*[custom_data~{MMOITEMS_ITEM_ID:"${x}"}]`;
  const NOS = '*[custom_data~{PublicBukkitValues:{"zaklecia:nosnik":1b}}]';
  const magia = async () => parseFloat((await rcon(`papi parse ${nick} %mythiclib_stat_magic_damage%`)).replace(',', '.'));
  const hp = async () => { const m = (await rcon('data get entity @e[tag=qa_k,limit=1] Health')).match(/([\d.]+)f/); return m ? +m[1] : null; };
  await rcon(`minecraft:clear ${nick}`);
  await rcon(`mmocore admin level set ${nick} main 30`);
  for (const a of ['bieglosc-kostur', 'bieglosc-kostur-dwureczny']) await rcon(`mmocore admin attribute give ${nick} ${a} 1`);
  const p0 = bot.entity.position.floored();
  await rcon(`minecraft:fill ${p0.x - 3} ${p0.y - 1} ${p0.z - 3} ${p0.x + 8} ${p0.y - 1} ${p0.z + 3} stone`);
  await rcon(`minecraft:fill ${p0.x - 3} ${p0.y} ${p0.z - 3} ${p0.x + 8} ${p0.y + 3} ${p0.z + 3} air`);
  bot.setQuickBarSlot(0); await sleep(500);
  console.log('przypisz:', await rcon(`zaklecia przypisz ${nick} ognisty_pocisk lewa`)); await sleep(800);
  w.sprawdz('nosnik-w-lewej', await ok('weapon.offhand', NOS), 'zaklecie w lewej dloni');

  const strzal = async opis => {
    await rcon('minecraft:kill @e[tag=qa_k]');
    await rcon(`minecraft:teleport ${nick} ${p0.x + 0.5} ${p0.y} ${p0.z + 0.5} -90 0`); await sleep(600);
    await rcon(`minecraft:summon husk ${p0.x + 5.5} ${p0.y} ${p0.z + 0.5} {NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:["qa_k"],attributes:[{id:"minecraft:max_health",base:500d}],Health:500f}`);
    await rcon('minecraft:effect give @e[tag=qa_k] minecraft:fire_resistance 120 0 true');
    await sleep(1200);
    const h = bot.nearestEntity(e => e.name === 'husk'); await bot.lookAt(h.position.offset(0, 1, 0), true); await sleep(500);
    await rcon(`mmocore admin resource-mana set ${nick} 1000`); await sleep(200);
    const a = await hp(); for (let t = 0; t < 700; t += 200) { bot.activateItem(false); bot.activateItem(true); await sleep(200); } bot.deactivateItem(); await sleep(2500);
    const b = await hp(); const d = +(a - b).toFixed(2);
    console.log(opis, 'obrazenia', d, 'MAGIC_DAMAGE', await magia());
    return d;
  };

  const bez = await strzal('pusta prawa');
  await rcon(`mi give GREATSTAFF KOSTUR_ZYWEGO_DREWNA ${nick} 1`); await sleep(800);
  for (let k = 1; k < 36; k++) if (await ok('container.' + k, id('KOSTUR_ZYWEGO_DREWNA'))) {
    await rcon(`minecraft:item replace entity ${nick} container.0 from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`); break;
  }
  bot.setQuickBarSlot(1); await sleep(400); bot.setQuickBarSlot(0); await sleep(1500);
  w.sprawdz('kostur-w-prawej', await ok('container.0', id('KOSTUR_ZYWEGO_DREWNA')), 'kostur w prawej');
  w.sprawdz('nosnik-zostaje-przy-kosturze', await ok('weapon.offhand', NOS), 'Ksiega nie zdejmuje zaklecia z lewej przy dwurecznym kosturze');
  const mk = await magia();
  w.sprawdz('premia-magii', mk >= 50, 'MAGIC_DAMAGE z kosturem >= 50 (premia 50 + magic-damage kostura)', mk);
  const z = await strzal('z kosturem');
  w.sprawdz('kostur-wzmacnia', bez > 0 && z > bez * 1.3, 'pocisk przez kostur wyraznie mocniejszy', `${bez} -> ${z} (x${(z / bez).toFixed(2)})`);
  bot.setQuickBarSlot(4); await sleep(1500);
  const mp = await magia();
  w.sprawdz('premia-znika', mp < 10, 'po zmianie slotu z kostura premia znika', mp);
  await rcon('minecraft:kill @e[tag=qa_k]');
}, { limitMs: 240000 });
