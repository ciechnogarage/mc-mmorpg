// Sonda PRZECHODNIOSCI Lochu 002: bot idzie pathfinderem cala trasa (zero teleportow po wejsciu), sam klika
// kolowrot przy tamie, schodzi po schodach do wraku i wraca. Walki nie mierzy (odpornosc z konsoli PRZED wejsciem).
// Uruchom: /opt/docker/mc/narzedzia/bot/run.sh <klon> <repo>/kuznia/tools/sonda_l002/trasa.js
const { scenariusz } = require('/bot/lib');
const { pathfinder, Movements, goals } = require('mineflayer-pathfinder');
const NICK = process.env.NICK || 'Trasa002';
const sleep = ms => new Promise(r => setTimeout(r, ms));

scenariusz('trasa-l002', async ({ w, polacz, rcon }) => {
  const ok = (n, p, i = '') => w.sprawdz(n, p, i);
  const R = { cmd: rcon };
  const bot = await polacz({ nick: NICK });
  bot.loadPlugin(pathfinder);
  let kick = null; bot.on('kicked', r => kick = String(r));
  await sleep(1500);
  await R.cmd(`effect give ${NICK} minecraft:resistance infinite 4 true`); await R.cmd(`effect give ${NICK} minecraft:regeneration infinite 3 true`);
  await R.cmd(`md play loch_002 ${NICK}`);
  let inst = null; for (let i = 0; i < 40 && !inst; i++) { for (let n = 0; n < 8; n++) if ((await R.cmd(`execute in minecraft:loch_002_${n} positioned 0 100 0 if entity @a[name=${NICK},distance=..2000]`)).includes('passed')) inst = `loch_002_${n}`; await sleep(500); }
  ok('wejscie', !!inst, inst);
  await sleep(3000);
  // MD czysci efekty przy wejsciu i po smierci - odpornosc PO wejsciu, odnawiana (sonda nie mierzy walki)
  const ochrona = async () => { await R.cmd(`effect give ${NICK} minecraft:resistance infinite 4 true`); await R.cmd(`effect give ${NICK} minecraft:regeneration infinite 3 true`); };
  // test TERENU: moby usuwane co 5 s (odrzut od ciosow resetowal pathfinder - walke mierzy harness gracza, nie ta sonda)
  const czysc = async () => { for (const t of ['endermite', 'drowned', 'bogged', 'witch', 'spider', 'cave_spider'])
      await R.cmd(`execute in minecraft:${inst} positioned 0 100 0 run minecraft:kill @e[type=minecraft:${t},distance=..400,tag=!l002_boss]`); };
  await ochrona(); await czysc(); const zegar = setInterval(async () => { try { await ochrona(); await czysc(); } catch {} }, 5000);
  const mv = new Movements(bot); mv.canDig = false; mv.allowParkour = false; mv.allow1by1towers = false; mv.scafoldingBlocks = []; mv.liquidCost = 6;
  bot.pathfinder.setMovements(mv);
  const zdarz = []; bot.on('death', () => zdarz.push(`SMIERC ${bot.entity.position.floored()}`));
  bot.on('path_update', r => { if (r.status !== 'success' && r.status !== 'partial') zdarz.push(`path ${r.status}`); });
  const idz = async (x, y, z, nazwa, r = 2, ms = 70000) => {
    const t0 = Date.now(); zdarz.length = 0;
    bot.pathfinder.setGoal(new goals.GoalNear(x, y, z, r));
    // odleglosc w poziomie: bot stoi blok nizej niz Y celu, 3D zawyzala odleglosc
    const odl = () => { const p = bot.entity.position; return Math.hypot(p.x - x, p.z - z) + Math.max(0, Math.abs(p.y - y) - 2); };
    while (Date.now() - t0 < ms && odl() > r + 1.5) await sleep(250);
    bot.pathfinder.setGoal(null); await sleep(300);
    const d = odl();
    const zd = [...new Set(zdarz)].slice(0, 4).join('; ');
    ok(`trasa_${nazwa}`, d <= r + 1.5, `odl=${d.toFixed(1)} t=${((Date.now() - t0) / 1000).toFixed(0)}s poz=${bot.entity.position.floored()} ${zd}`);
    return d <= r + 1.5;
  };
  const Y = 102;
  await idz(-42, Y, 70, 'przystan_kladki');
  await idz(-28, Y, 62, 'kladki');
  await idz(0, Y, 52, 'wysepka');
  await idz(4, Y, 36, 'kolowrot1');
  await sleep(2500);
  const korba = Object.values(bot.entities).find(e => e.name === 'zombie' && e.position.distanceTo(bot.entity.position) < 5);
  ok('korba_widoczna', !!korba);
  if (korba) { await bot.lookAt(korba.position.offset(0, 1.5, 0)); try { bot.activateEntityAt(korba, korba.position.offset(0, 1, 0)); } catch {} await sleep(300); try { bot.activateEntity(korba); } catch {} }
  await sleep(9000);
  await idz(0, Y, 20, 'most_przez_basen');
  await idz(17, Y, 8, 'szczyt_schodow');
  await idz(-8, 97, 18, 'dno_basenu_wrak', 2, 60000);
  await idz(17, Y, 8, 'z_powrotem_po_schodach', 2, 60000);
  await idz(0, Y, 0, 'rozstaj');
  await idz(34, Y, -6, 'szeptucha');
  await idz(54, Y, -40, 'kokoniarnia', 3);
  await idz(22, Y, -66, 'grobla');
  await idz(8, Y, -80, 'arena', 3);
  ok('bez_kicka', !kick, kick || '');
  clearInterval(zegar);
}, { limitMs: 600000 });
