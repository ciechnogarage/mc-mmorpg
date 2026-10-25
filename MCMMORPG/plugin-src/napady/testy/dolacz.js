'use strict';
// Napady: gracz (bot bez OP) dolacza do trwajacego napadu po stronie obrony i walczy z cialami napadu
// (moby MM w przebraniu gracza LD - bot widzi wtedy encje `player`). Bez gracza napad liczy sie symulacja,
// dolaczenie najpierw materializuje ciala, potem teleportuje (LOD, testy/lod.js).
// Strony rozpoznaje po helmie (napastnik: kolczy, obronca: zelazny; Equipment w mobs/Napady.yml).
// Sciezki nieidealne: nieistniejacy napad, cios we wlasna strone (zablokowany), smierc w napadzie
// (decyzja wlasciciela 25.10: "gracz jak pada to pada może obserwować"): respawn jako widz, brak ponownego
// dolaczenia, po koncu napadu powrot do trybu i miejsca sprzed dolaczenia.
// Uruchom: /opt/docker/mc/narzedzia/bot/run.sh <klon> <sciezka>/testy/dolacz.js
const { scenariusz, czekaj, sleep, stan } = require('/bot/lib');
const { pathfinder, goals } = require('mineflayer-pathfinder');

function helm(e) {
  const h = e.equipment && e.equipment[5];
  return h ? h.name : '';
}

scenariusz('napady-dolacz', async ({ w, polacz, rcon }) => {
  const nick = 'BotNap' + (Date.now() % 100000);
  await rcon('napad stop');
  await rcon('napad limit 1');
  await rcon('napad lod auto');          // ciala wstaja dopiero, gdy gracz dolacza (LOD)
  await rcon('napad przebrania true');
  await rcon('minecraft:difficulty easy');   // w peaceful ciosy mobow w gracza = 0, a MM zombie/szkielety znikaja
  const bot = await polacz({ nick, plugins: [pathfinder] });
  // mineflayer liczy grawitacje takze w trybie widza i odsyla serwerowi opadanie (widz w grze wisi; 25.10:
  // serwer stawia widza na y -49, bot spadal do -55,6 przed odczytem i do ziemi po 3 s) -> fizyka jak u widza
  bot.on('game', () => { bot.physicsEnabled = bot.game.gameMode !== 'spectator'; });
  // bez kopania: inaczej pathfinder rozbiera mur i brame areny (24.10: ~40 machniec w bloki)
  const { Movements } = require('mineflayer-pathfinder');
  const mv = new Movements(bot); mv.canDig = false; mv.allow1by1towers = false; bot.pathfinder.setMovements(mv);
  await rcon(`minecraft:give ${nick} minecraft:iron_sword`);
  await rcon(`minecraft:gamemode survival ${nick}`);
  await sleep(1500);
  const start = await stan.poz(nick), startSwiat = await stan.wymiar(nick);

  // nieidealna: nie ma takiego napadu
  const czat = [];
  bot.on('messagestr', (m) => czat.push(m));
  bot.chat('/napad dolacz 999 obrona');
  const brak = await czekaj(() => czat.find((m) => /Nie ma takiego napadu/.test(m)), 5000, 200);
  w.sprawdz('zly-napad', !!brak, brak || `czat: ${czat.slice(-3).join(' | ')}`);

  await rcon('napad start 1');
  const id = await czekaj(async () => { const s = await rcon('napad stan'); const m = s.match(/#(\d+) /); return m && m[1]; }, 15000, 500);
  w.sprawdz('napad-ruszyl', !!id, `id ${id}`);
  if (!id) return;
  const lodPrzed = ((await rcon('napad stan')).match(/\| lod (\S+) ciala/) || [])[1];
  const t0 = Date.now();
  bot.chat(`/napad dolacz ${id} obrona`);
  const blisko = (q) => q && Math.abs(q.x - 30.5) < 2 && Math.abs(q.z - 26.5) < 2;
  const p = await czekaj(async () => { const q = await stan.poz(nick); return blisko(q) && q; }, 20000, 300);
  w.sprawdz('dolaczenie-teleport', !!p, `${JSON.stringify(p || await stan.poz(nick))} po ${Date.now() - t0} ms (lod przed: ${lodPrzed})`);
  const sCial = await rcon('napad stan');
  const cialo = (sCial.match(/\| lod (\S+) ciala (\d+)/) || []).slice(1).join(' ');
  w.sprawdz('dolaczenie-ciala', /^CIALA [1-9]/.test(cialo), cialo);

  const sword = bot.inventory.items().find((i) => i.name === 'iron_sword');
  if (sword) await bot.equip(sword, 'hand');
  const typy = new Set();
  let ciosyDoszly = 0, ciosyWszystkie = 0;
  let ciosy = 0, ciosySwoich = 0, minHp = bot.health, trafione = 0, zginal = false;
  bot.on('health', () => { minHp = Math.min(minHp, bot.health); });
  const ciala = () => Object.values(bot.entities).filter((e) => e !== bot.entity && e.username !== nick && /helmet/.test(helm(e)));
  bot.on('entityHurt', (e) => { if (/helmet/.test(helm(e)) && e.position.distanceTo(bot.entity.position) < 4) trafione++; });
  bot.on('death', () => { zginal = true; });
  const koniec = Date.now() + 100000;
  let swojProbowany = false;
  while (Date.now() < koniec && !zginal) {
    const s = await rcon('napad stan');
    if (!new RegExp(`#${id} `).test(s)) break;
    const mc = /gracze ciosy (\d+)\/(\d+)/.exec(s); if (mc) { ciosyDoszly = +mc[1]; ciosyWszystkie = +mc[2]; }
    const cc = ciala();
    cc.forEach((e) => typy.add(String(e.name)));
    const wrogowie = cc.filter((e) => helm(e) === 'chainmail_helmet');
    const swoi = cc.filter((e) => helm(e) === 'iron_helmet');
    const odl = (e) => e.position.distanceTo(bot.entity.position);
    // raz: cios we wlasnego obronce (ma byc zablokowany)
    if (!swojProbowany && swoi.length) {
      const s0 = swoi.sort((a, b) => odl(a) - odl(b))[0];
      if (odl(s0) < 3) { await bot.lookAt(s0.position.offset(0, 1.5, 0)); bot.attack(s0); ciosySwoich++; swojProbowany = true; await sleep(700); continue; }
      else if (!wrogowie.length) { bot.pathfinder.setGoal(new goals.GoalNear(s0.position.x, s0.position.y, s0.position.z, 2)); await sleep(500); continue; }
    }
    const cel = wrogowie.sort((a, b) => odl(a) - odl(b))[0];
    if (!cel) { await sleep(500); continue; }
    if (odl(cel) < 3) {
      bot.pathfinder.setGoal(null);
      await bot.lookAt(cel.position.offset(0, 1.5, 0)); bot.attack(cel); ciosy++; await sleep(650);
    }
    else { bot.pathfinder.setGoal(new goals.GoalNear(cel.position.x, cel.position.y, cel.position.z, 2)); await sleep(400); }
  }
  bot.pathfinder.setGoal(null);
  w.sprawdz('ciala-znane-botowi', typy.size > 0, `cialo ${cialo}, typy encji u bota: ${[...typy].join(',')}`);
  w.sprawdz('bot-bil-napastnikow', ciosy > 0, `ciosy ${ciosy}, trafione (entityHurt) ${trafione}`);
  w.sprawdz('ciala-bija-gracza', minHp < 20, `min HP bota ${minHp}`);
  w.sprawdz('ciosy-gracza-doszly', ciosyDoszly > 0, `serwer: doszlo ${ciosyDoszly} z ${ciosyWszystkie} (reszta = zablokowane, m.in. cios w swojego)`);
  w.sprawdz('cios-swojego-sprobowany', ciosySwoich > 0, `proby ${ciosySwoich} (wynik w zapisie: cios-blok)`);

  if (!zginal) {
    w.pomin('smierc-widz', 'bot przezyl napad - sciezki smierci nie sprawdzono w tym przebiegu');
  } else {
    // mineflayer odradza sam (respawn: true); serwer ustawia widza tik po odrodzeniu
    const tryb = await czekaj(async () => (await stan.trybGry(nick)) === 'spectator' && 'spectator', 10000, 300);
    w.sprawdz('smierc-widz', !!tryb, `tryb po odrodzeniu: ${await stan.trybGry(nick)}`);
    const pw = await stan.poz(nick);
    w.sprawdz('smierc-punkt-widza', pw && Math.abs(pw.x - 14.5) < 6 && Math.abs(pw.z - 20.5) < 6 && pw.y > -55, JSON.stringify(pw));
    const ekw = bot.inventory.items().some((i) => i.name === 'iron_sword');
    w.sprawdz('smierc-bez-utraty-rzeczy', ekw, `miecz w EQ po smierci: ${ekw}`);
    czat.length = 0;
    bot.chat(`/napad dolacz ${id} obrona`);
    const odm = await czekaj(() => czat.find((m) => /Polegles/.test(m)), 5000, 200);
    w.sprawdz('polegly-nie-wraca-do-walki', !!odm, odm || `czat: ${czat.slice(-3).join(' | ')}`);
    // koniec napadu (najwyzej limit czasu) -> powrot
    await czekaj(async () => !new RegExp(`#${id} `).test(await rcon('napad stan')), 330000, 2000);
    const powrot = await czekaj(async () => (await stan.trybGry(nick)) === 'survival' && 'survival', 10000, 300);
    const pk = await stan.poz(nick), sk = await stan.wymiar(nick);
    w.sprawdz('po-napadzie-powrot', !!powrot && sk === startSwiat && pk && start && Math.hypot(pk.x - start.x, pk.z - start.z) < 3,
      `tryb ${await stan.trybGry(nick)}, swiat ${sk} (byl ${startSwiat}), poz ${JSON.stringify(pk)} (byla ${JSON.stringify(start)})`);
  }
  console.log(`NAPAD ${id} nick ${nick} cialo ${cialo}`);
}, { limitMs: 520000 });
