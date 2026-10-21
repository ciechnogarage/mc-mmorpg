'use strict';
// Reguly rak Ksiegi (12.10): macierz typow + tarcza Nosiciela + przelozenie zlego ukladu + miecz startowy Miecza Korony.
const { scenariusz, sleep } = require('/bot/lib');

const DWU = ['BOW', 'CROSSBOW', 'MUSKET', 'GREATBOW', 'GREATSWORD', 'GREATAXE', 'GREATHAMMER', 'HALBERD', 'LANCE', 'KOSA',
  'GREATSTAFF', 'STAFF', 'SPEAR'];
const LEWA = ['OFF_CATALYST'];
const PROBKI = require('fs').readFileSync(__dirname + '/probki.txt', 'utf8').trim().split('\n').map(l => l.trim().split(/\s+/));

scenariusz('reguly-rak', async ({ w, polacz, rcon }) => {
  const nick = 'BotReguly';
  const bot = await polacz({ nick });
  const ok = async (slot, pred) => (await rcon(`execute if items entity ${nick} ${slot} ${pred}`)).includes('passed');
  const id = x => `*[custom_data~{MMOITEMS_ITEM_ID:"${x}"}]`;
  const gdzie = async x => {
    const o = [];
    if (await ok('weapon.offhand', id(x))) o.push('LEWA');
    if (await ok('container.0', id(x))) o.push('PRAWA');
    if (await ok('player.cursor', id(x))) o.push('kursor');
    for (let k = 1; k < 36; k++) if (await ok('container.' + k, id(x))) o.push(k < 9 ? 'pasek' + k : 'plecak');
    return o.join(',') || 'BRAK';
  };
  const wPlecak = async (typ, x) => {
    await rcon(`mi give ${typ} ${x} ${nick} 1`); await sleep(500);
    for (let k = 0; k < 36; k++) if (await ok('container.' + k, id(x))) {
      if (k !== 27) { await rcon(`minecraft:item replace entity ${nick} container.27 from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`); }
      return true;
    }
    return false;
  };
  const czysc = async () => { await rcon(`minecraft:clear ${nick}`); await sleep(700); bot.setQuickBarSlot(0); await sleep(300); };
  const zamknij = () => { try { bot.closeWindow(bot.inventory); } catch (e) { /* brak okna */ } };
  // srodowisko: bez smierci (klon ma plaski swiat w powietrzu i nocne moby - bot ginal i gubil przedmioty)
  for (const g of ['fall_damage false', 'keep_inventory true', 'spawn_mobs false', 'spawn_monsters false']) await rcon(`minecraft:gamerule ${g}`);
  await rcon('minecraft:time set noon');
  const p0 = bot.entity.position.floored();
  await rcon(`minecraft:fill ${p0.x - 4} ${p0.y - 1} ${p0.z - 4} ${p0.x + 6} ${p0.y - 1} ${p0.z + 4} stone`);
  await rcon(`minecraft:fill ${p0.x - 4} ${p0.y} ${p0.z - 4} ${p0.x + 6} ${p0.y + 3} ${p0.z + 4} air`);
  await rcon(`minecraft:teleport ${nick} ${p0.x + 0.5} ${p0.y} ${p0.z + 0.5}`); await sleep(800);
  await rcon(`mmocore admin level set ${nick} main 100`);

  // 1. macierz: kazdy typ do prawej (sztylet w lewej) i do lewej (sztylet w prawej)
  for (const [typ, jedno, dwu] of PROBKI) {
    for (const [x, flaga] of [[jedno, false], [dwu, true]]) {
      if (x === '-' || x === 'STEEL_DAGGER') continue;
      const dwureczna = typ !== 'SHIELD' && (DWU.includes(typ) || flaga), tylkoLewa = LEWA.includes(typ);
      for (const reka of ['P', 'L']) {
        await czysc();
        await wPlecak('DAGGER', 'STEEL_DAGGER'); await bot.clickWindow(27, 0, 0); await sleep(300); await bot.clickWindow(reka === 'P' ? 45 : 36, 0, 0); await sleep(600);
        if (!(await wPlecak(typ, x))) { w.pomin(`${typ}-${x}-${reka}`, 'brak przedmiotu'); continue; }
        await bot.clickWindow(27, 0, 0); await sleep(300); await bot.clickWindow(reka === 'P' ? 36 : 45, 0, 0); await sleep(800);
        const g = await gdzie(x), s = await gdzie('STEEL_DAGGER');
        if (await ok('player.cursor', '*')) { await bot.clickWindow(27, 0, 0); await sleep(300); }
        zamknij(); await sleep(200);
        const ocz = reka === 'P' ? (tylkoLewa ? 'odrzucony' : 'PRAWA') : (dwureczna ? 'odrzucony' : 'LEWA');
        const jest = reka === 'P' ? (g.includes('PRAWA') ? 'PRAWA' : 'odrzucony') : (g.includes('LEWA') ? 'LEWA' : 'odrzucony');
        const sztOcz = reka === 'P' ? (dwureczna ? 'plecak' : 'LEWA') : 'PRAWA';
        w.sprawdz(`${typ}-${x}-${reka}`, jest === ocz && g !== 'BRAK' && s.includes(sztOcz), `oczekiwano ${ocz}`, `przedmiot=${g} sztylet=${s}`);
      }
    }
  }

  // 2. Nosiciel Relikwii: tarcza jako jedyna bron - w prawej przy pustej lewej
  await czysc(); await wPlecak('SHIELD', 'PALADIN_SHIELD'); await bot.clickWindow(27, 0, 0); await sleep(300); await bot.clickWindow(36, 0, 0); await sleep(800); zamknij();
  w.sprawdz('tarcza-prawa-sama', await ok('container.0', id('PALADIN_SHIELD')), 'tarcza zostaje w prawej', await gdzie('PALADIN_SHIELD'));
  await czysc(); await wPlecak('SHIELD', 'ABSORBING_SHIELD'); await bot.clickWindow(27, 0, 0); await sleep(300); await bot.clickWindow(45, 0, 0); await sleep(800); zamknij();
  w.sprawdz('tarcza-z-flaga-2r-do-lewej', await ok('weapon.offhand', id('ABSORBING_SHIELD')), 'tarcza z flaga TWO_HANDED nadal jednoreczna', await gdzie('ABSORBING_SHIELD'));

  // 3. zly uklad wstawiony komenda: wlocznia w lewej przy pustej prawej -> przechodzi do prawej
  await czysc(); await rcon(`mi give SPEAR SHARP_SPEAR ${nick} 1`); await sleep(600);
  for (let k = 0; k < 36; k++) if (await ok('container.' + k, id('SHARP_SPEAR'))) { await rcon(`minecraft:item replace entity ${nick} weapon.offhand from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`); break; }
  bot.setQuickBarSlot(3); await sleep(400); bot.setQuickBarSlot(0); await sleep(1200);
  w.sprawdz('wlocznia-z-lewej-do-prawej', await ok('container.0', id('SHARP_SPEAR')), 'przelozona do pustej prawej', await gdzie('SHARP_SPEAR'));
  // ... a przy zajetej prawej -> do plecaka, bez utraty
  await czysc(); await wPlecak('DAGGER', 'STEEL_DAGGER'); await bot.clickWindow(27, 0, 0); await sleep(300); await bot.clickWindow(36, 0, 0); await sleep(600); zamknij();
  await rcon(`mi give SPEAR SHARP_SPEAR ${nick} 1`); await sleep(600);
  for (let k = 1; k < 36; k++) if (await ok('container.' + k, id('SHARP_SPEAR'))) { await rcon(`minecraft:item replace entity ${nick} weapon.offhand from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`); break; }
  bot.setQuickBarSlot(3); await sleep(400); bot.setQuickBarSlot(0); await sleep(1200);
  const gw = await gdzie('SHARP_SPEAR');
  w.sprawdz('wlocznia-z-lewej-do-plecaka', gw !== 'BRAK' && !gw.includes('LEWA') && await ok('container.0', id('STEEL_DAGGER')), 'prawa zajeta: wlocznia do plecaka, sztylet zostaje', gw);

  // 4. miecz startowy: klasa Miecz Korony (rycerz) z Kamiennym mieczem, bez recznie dodanej bieglosci
  await czysc();
  for (const c of ['RYCERZ', 'rycerz', 'CZLOWIEK_RYCERZ']) { const o = await rcon(`mmocore admin class ${nick} ${c}`); console.log('class', c, '->', o.slice(0, 80)); if (!/not|nie|invalid|Could/i.test(o)) break; }
  await rcon(`mmocore admin level set ${nick} main 1`); await sleep(1000);
  console.log('klasa:', await rcon(`papi parse ${nick} %mmocore_class% bieglosc-miecz=%mmocore_attribute_bieglosc-miecz%`));
  const czat = []; bot.on('messagestr', m => czat.push(m)); bot.on('actionBar', m => czat.push(String(m)));
  await rcon(`mi give SWORD MIECZ_KAMIENNY ${nick} 1`); await sleep(800);
  bot.setQuickBarSlot(0); await sleep(600);
  await rcon(`minecraft:teleport ${nick} ${p0.x + 0.5} ${p0.y} ${p0.z + 0.5} -90 0`); await sleep(800);
  const p = bot.entity.position;
  await rcon(`minecraft:summon husk ${(p.x + 1.6).toFixed(2)} ${p.y} ${p.z.toFixed(2)} {NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:["qa_r"],attributes:[{id:"minecraft:max_health",base:200d}],Health:200f}`); await sleep(1500);
  const h = bot.nearestEntity(e => e.name === 'husk'); await bot.lookAt(h.position.offset(0, 1, 0), true); await sleep(600);
  const hp = async () => { const m = (await rcon(`data get entity @e[tag=qa_r,limit=1] Health`)).match(/([\d.]+)f/); return m ? +m[1] : null; };
  const a = await hp(); bot.attack(h); await sleep(1200); const b = await hp();
  const odmowa = czat.filter(m => /Nie możesz tego użyć|nie spełnia/i.test(m));
  console.log('cios kamiennym mieczem:', a, '->', b, '| komunikaty:', JSON.stringify(odmowa.slice(0, 2)));
  w.sprawdz('miecz-startowy-uzywalny', odmowa.length === 0 && a - b > 1.5, 'Miecz Korony uzywa miecza startowego bez odmowy', `obrazenia=${(a - b).toFixed(2)} odmowy=${odmowa.length}`);
  await rcon('minecraft:kill @e[tag=qa_r]');
}, { limitMs: 1500000 });
