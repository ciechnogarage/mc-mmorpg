'use strict';
// Miecz startowy Miecza Korony: czy klasa (additional-bieglosc-miecz) wystarcza, czy trzeba punktu bieglosci.
const { scenariusz, sleep } = require('/bot/lib');

scenariusz('miecz-startowy', async ({ w, polacz, rcon }) => {
  const nick = 'BotMiecz';
  const bot = await polacz({ nick });
  for (const g of ['fall_damage false', 'keep_inventory true', 'spawn_mobs false', 'spawn_monsters false']) await rcon(`minecraft:gamerule ${g}`);
  const p0 = bot.entity.position.floored();
  await rcon(`minecraft:fill ${p0.x - 3} ${p0.y - 1} ${p0.z - 3} ${p0.x + 5} ${p0.y - 1} ${p0.z + 3} stone`);
  await rcon(`minecraft:fill ${p0.x - 3} ${p0.y} ${p0.z - 3} ${p0.x + 5} ${p0.y + 3} ${p0.z + 3} air`);
  await rcon(`minecraft:clear ${nick}`);
  console.log(await rcon(`mmocore admin class ${nick} RYCERZ`));
  await rcon(`mmocore admin level set ${nick} main 1`); await sleep(800);
  const czat = []; bot.on('messagestr', m => czat.push(m)); bot.on('actionBar', m => czat.push(String(m)));
  await rcon(`mi give SWORD MIECZ_KAMIENNY ${nick} 1`); await sleep(800);
  bot.setQuickBarSlot(1); await sleep(300); bot.setQuickBarSlot(0); await sleep(800);
  console.log('w rece:', (await rcon(`data get entity ${nick} SelectedItem.components."minecraft:custom_data".MMOITEMS_ITEM_ID`)).replace(/.*data: /, ''));
  const stan = async () => (await rcon(`papi parse ${nick} %mmocore_class% bieglosc=%mmocore_attribute_bieglosc-miecz%`)).trim();
  const cios = async opis => {
    await rcon('minecraft:kill @e[tag=qa_m]');
    await rcon(`minecraft:teleport ${nick} ${p0.x + 0.5} ${p0.y} ${p0.z + 0.5} -90 0`); await sleep(700);
    await rcon(`minecraft:summon husk ${p0.x + 2.1} ${p0.y} ${p0.z + 0.5} {NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:["qa_m"],attributes:[{id:"minecraft:max_health",base:200d}],Health:200f}`);
    await sleep(1200);
    const h = bot.nearestEntity(e => e.name === 'husk');
    await bot.lookAt(h.position.offset(0, 1, 0), true); await sleep(900);
    czat.length = 0;
    const hp = async () => { const m = (await rcon('data get entity @e[tag=qa_m,limit=1] Health')).match(/([\d.]+)f/); return m ? +m[1] : null; };
    const a = await hp(); bot.attack(h); await sleep(1200); const b = await hp();
    const odm = czat.filter(m => /możesz|wymaga|spełnia/i.test(m));
    console.log(opis, '|', await stan(), '| cios', a, '->', b, '| odmowy', JSON.stringify(odm.slice(0, 2)));
    return { d: a - b, odm: odm.length };
  };
  const bez = await cios('klasa bez punktu');
  await rcon(`mmocore admin attribute give ${nick} bieglosc-miecz 1`); await sleep(800);
  const z = await cios('klasa + punkt');
  w.sprawdz('cios-dochodzi', z.d > 0, 'kontrola: z punktem bieglosci cios trafia', z.d.toFixed(2));
  w.sprawdz('klasa-wystarcza', bez.odm === 0 && Math.abs(bez.d - z.d) < 0.5, 'Miecz Korony bez recznego punktu bije tak samo i bez odmowy', `bez=${bez.d.toFixed(2)} (odmowy ${bez.odm}) z=${z.d.toFixed(2)}`);
  await rcon('minecraft:kill @e[tag=qa_m]');
}, { limitMs: 180000 });
