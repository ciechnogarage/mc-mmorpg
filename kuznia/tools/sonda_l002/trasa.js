// Sonda PRZECHODNIOSCI Lochu 002: bot idzie pathfinderem cala trasa (zero teleportow po wejsciu), sam klika
// kolowrot przy tamie, schodzi po schodach do wraku i wraca. Walki nie mierzy (odpornosc z konsoli PRZED wejsciem).
// Uruchom: /opt/docker/mc/narzedzia/bot/run.sh <klon> <repo>/kuznia/tools/sonda_l002/trasa.js
const { scenariusz, postac } = require('/bot/lib');
const { pathfinder, Movements, goals } = require('mineflayer-pathfinder');
const NICK = process.env.NICK || 'Trasa002';
const sleep = ms => new Promise(r => setTimeout(r, ms));

scenariusz('trasa-l002', async ({ w, polacz, rcon }) => {
  const ok = (n, p, i = '') => w.sprawdz(n, p, i);
  const R = { cmd: rcon };
  const bot = await polacz({ nick: NICK });
  bot.loadPlugin(pathfinder);
  // klon --z-kreatorem: pierwsze wejscie konta laduje w scenie kreatora - postac jak gracz (na klonie bez kreatora droga 'bez-sceny')
  const sc = await postac.wejdz(bot, { imie: 'Trasa' + Math.floor(Math.random() * 900 + 100) });
  console.log('postac.wejdz droga=' + sc.droga);
  let kick = null; bot.on('kicked', r => kick = String(r));
  await sleep(1500);
  await R.cmd(`minecraft:effect give ${NICK} minecraft:resistance infinite 4 true`); await R.cmd(`minecraft:effect give ${NICK} minecraft:regeneration infinite 3 true`);
  // KickOfflinePlayersDelay 300: powrot w ciagu 5 min = STARA instancja w miejscu wyjscia (start w -9,51 / 10,-78, 22.10); /leave dziala bez OP, po /md leave bot zostal w instancji
  bot.chat('/leave'); await sleep(2500);
  await R.cmd(`md play loch_002 ${NICK}`);
  let inst = null; for (let i = 0; i < 40 && !inst; i++) { for (let n = 0; n < 8; n++) if ((await R.cmd(`execute in minecraft:loch_002_${n} positioned 0 100 0 if entity @a[name=${NICK},distance=..2000]`)).includes('passed')) inst = `loch_002_${n}`; await sleep(500); }
  await sleep(3000);
  ok('wejscie', !!inst && Math.hypot(bot.entity.position.x + 56, bot.entity.position.z - 100) < 6, `${inst} poz=${bot.entity.position.floored()}`);
  // MD czysci efekty przy wejsciu i po smierci - odpornosc PO wejsciu, odnawiana (sonda nie mierzy walki)
  const ochrona = async () => { await R.cmd(`minecraft:effect give ${NICK} minecraft:resistance infinite 4 true`); await R.cmd(`minecraft:effect give ${NICK} minecraft:regeneration infinite 3 true`); };
  // test TERENU: moby usuwane co 5 s (odrzut od ciosow resetowal pathfinder - walke mierzy harness gracza, nie ta sonda)
  const czysc = async () => { for (const t of ['endermite', 'drowned', 'bogged', 'witch', 'spider', 'cave_spider'])
      await R.cmd(`execute in minecraft:${inst} positioned 0 100 0 run minecraft:kill @e[type=minecraft:${t},distance=..400,tag=!l002_boss]`); };
  await ochrona(); await czysc(); const zegar = setInterval(async () => { try { await ochrona(); await czysc(); } catch {} }, 5000);
  const mv = new Movements(bot); mv.canDig = false; mv.allowParkour = false; mv.allow1by1towers = false; mv.scafoldingBlocks = []; mv.liquidCost = 6;
  // bez scinania naroznikow: pathfinder (movements.js getMoveDiagonal) puszcza skos, gdy wolny jest JEDEN bok, a Paper cofa
  // bota wcisnietego w pojedynczy pelny blok co tick (forcedMove; naroznik muru 15,102,6, otwor 15,102,8, moss_block 20,102,-2;
  // miejsce utyku zmienialo sie miedzy przebiegami 22.10). Gracz slizga sie po krawedzi - to wada bota, nie terenu.
  const skos = mv.getMoveDiagonal.bind(mv);
  mv.getMoveDiagonal = (node, dir, neighbors) => {
    for (const [dx, dz] of [[dir.x, 0], [0, dir.z]]) for (const dy of [0, 1]) if (mv.getBlock(node, dx, dy, dz).physical) return;
    skos(node, dir, neighbors);
  };
  bot.pathfinder.setMovements(mv);
  const zdarz = []; bot.on('death', () => zdarz.push(`SMIERC ${bot.entity.position.floored()}`));
  bot.on('path_update', r => { if (r.status !== 'success' && r.status !== 'partial') zdarz.push(`path ${r.status}`); });
  const idz = async (x, y, z, nazwa, r = 2, ms = 70000) => {
    const t0 = Date.now(); zdarz.length = 0;
    bot.pathfinder.setGoal(new goals.GoalNear(x, y, z, r));
    // odleglosc w poziomie: bot stoi blok nizej niz Y celu, 3D zawyzala odleglosc
    const odl = () => { const p = bot.entity.position; return Math.hypot(p.x - x, p.z - z) + Math.max(0, Math.abs(p.y - y) - 2); };
    // odblokowanie: pathfinder idzie skosem obok pojedynczego pelnego bloku (naroznik muru 15,102,6, moss_block 20,102,-2)
    // i bot stoi wcisniety w rog z gotowa sciezka (diag-wrak/diag-kokon 22.10; gracz slizga sie po krawedzi) - krok wstecz
    // i nowy cel; liczba odblokowan w wyniku, zeby nie maskowac prawdziwego utyku (ten konczy sie FAIL po limicie czasu)
    let odblok = 0, ost = bot.entity.position.clone(), tOst = Date.now();
    while (Date.now() - t0 < ms && odl() > r + 1.5) {
      await sleep(250);
      if (bot.entity.position.distanceTo(ost) > 0.15) { ost = bot.entity.position.clone(); tOst = Date.now(); continue; }
      if (Date.now() - tOst < 2000) continue;
      odblok++; bot.pathfinder.setGoal(null); bot.clearControlStates();
      bot.setControlState('back', true); await sleep(350); bot.setControlState('back', false);
      bot.pathfinder.setGoal(new goals.GoalNear(x, y, z, r)); ost = bot.entity.position.clone(); tOst = Date.now();
    }
    bot.pathfinder.setGoal(null); await sleep(300);
    const d = odl();
    const zd = [...new Set(zdarz)].slice(0, 4).join('; ');
    ok(`trasa_${nazwa}`, d <= r + 1.5, `odl=${d.toFixed(1)} t=${((Date.now() - t0) / 1000).toFixed(0)}s poz=${bot.entity.position.floored()} odblok=${odblok} ${zd}`);
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
  // brama tamy 3 bl.: skos z (2.5,32.5) obok muru blokowal bota w narozniku (diag 21.10, 1 z 3 przebiegow) - podejscie na wprost jak gracz
  await idz(0, Y, 34, 'przed_brama', 1);
  await idz(0, Y, 20, 'most_przez_basen');
  // na wprost otworu (15,102,8): z (15,5) pathfinder szedl skosem przy narozniku muru 15,102,6 i Paper cofal bota co tick
  // (forcedMove co 50 ms, diag-wrak3 22.10) - gracza ten skos nie zatrzymuje, bota tak
  await idz(16, Y, 8, 'szczyt_schodow', 0);
  // otwor 1 bl. (15,102,8) miedzy murem z=7 i z=9: skos z otworu na pierwszy stopien (14,101,9) przy naroznik 15,102,9 = ten sam
  // forcedMove (trasa 22.10 04:56: 23 odblokowania w otworze) - gracz robi krok na spocznik i skreca, bot tez
  await idz(14, Y, 8, 'spocznik', 0);
  await idz(-8, 97, 18, 'dno_basenu_wrak', 2, 60000);
  // na wprost przez otwor (cel 17,8 r=2 zaliczal sie ze spocznika 14,8, a dalej bot klinowal sie w otworze przy 15,102,7)
  await idz(14, Y, 8, 'spocznik_powrot', 0, 60000);
  await idz(16, Y, 8, 'z_powrotem_po_schodach', 0);
  await idz(0, Y, 0, 'rozstaj');
  await idz(34, Y, -6, 'szeptucha');
  await idz(54, Y, -40, 'kokoniarnia', 3);
  await idz(22, Y, -66, 'grobla');
  await idz(8, Y, -80, 'arena', 3);
  ok('bez_kicka', !kick, kick || '');
  clearInterval(zegar);
  bot.chat('/leave'); await sleep(1500);
}, { limitMs: 600000 });
