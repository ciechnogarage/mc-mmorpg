'use strict';
// Etap 1 napadow: gracz (bot bez OP) dolacza do trwajacego napadu po stronie obrony i walczy z manekinami.
// Sciezki nieidealne: nieistniejacy napad, cios we wlasna strone (ma byc zablokowany).
// Uruchom: /opt/docker/mc/narzedzia/bot/run.sh <klon> <sciezka>/testy/dolacz.js
const { scenariusz, czekaj, sleep, stan } = require('/bot/lib');
const { pathfinder, goals } = require('mineflayer-pathfinder');

function nazwa(e) {
  const m = e.metadata && e.metadata[2];
  if (!m) return '';
  try { return JSON.stringify(m); } catch { return String(m); }
}

scenariusz('napady-dolacz', async ({ w, polacz, rcon }) => {
  const nick = 'BotNap' + (Date.now() % 100000);
  await rcon('napad stop');
  await rcon('napad limit 1');
  await rcon('minecraft:difficulty easy');   // w peaceful ciosy mobow/manekinow w gracza = 0
  const bot = await polacz({ nick, plugins: [pathfinder] });
  // bez kopania: inaczej pathfinder rozbiera mur i brame areny (24.10: ~40 machniec w bloki)
  const { Movements } = require('mineflayer-pathfinder');
  const mv = new Movements(bot); mv.canDig = false; mv.allow1by1towers = false; bot.pathfinder.setMovements(mv);
  await rcon(`minecraft:give ${nick} minecraft:iron_sword`);
  await rcon(`minecraft:gamemode survival ${nick}`);
  await sleep(1500);

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
  bot.chat(`/napad dolacz ${id} obrona`);
  await sleep(1500);
  const p = await stan.poz(nick);
  w.sprawdz('dolaczenie-teleport', p && Math.abs(p.x - 30.5) < 2 && Math.abs(p.z - 26.5) < 2, JSON.stringify(p));

  const sword = bot.inventory.items().find((i) => i.name === 'iron_sword');
  if (sword) await bot.equip(sword, 'hand');
  const typy = new Set();
  let ciosyDoszly = 0, ciosyWszystkie = 0;
  let ciosy = 0, ciosySwoich = 0, minHp = bot.health, trafione = 0;
  bot.on('health', () => { minHp = Math.min(minHp, bot.health); });
  bot.on('entityHurt', (e) => { if (e.name === 'mannequin' && e.position.distanceTo(bot.entity.position) < 4) trafione++; });
  bot.on('death', () => w.pomin('smierc-bota', 'bot zginal w walce (dopuszczalne)'));
  const koniec = Date.now() + 100000;
  let swojProbowany = false;
  while (Date.now() < koniec) {
    const s = await rcon('napad stan');
    if (!new RegExp(`#${id} `).test(s)) break;
    const mc = /gracze ciosy (\d+)\/(\d+)/.exec(s); if (mc) { ciosyDoszly = +mc[1]; ciosyWszystkie = +mc[2]; }
    const manekiny = Object.values(bot.entities).filter((e) => e.name === 'mannequin' || (e.type && /mannequin/i.test(String(e.type))));
    manekiny.forEach((e) => typy.add(String(e.name)));
    const wrogowie = manekiny.filter((e) => /Naje/.test(nazwa(e)));
    const swoi = manekiny.filter((e) => /Obro/.test(nazwa(e)));
    // raz: cios we wlasnego obronce (ma byc zablokowany)
    if (!swojProbowany && swoi.length) {
      const s0 = swoi.sort((a, b) => a.position.distanceTo(bot.entity.position) - b.position.distanceTo(bot.entity.position))[0];
      if (s0.position.distanceTo(bot.entity.position) < 3) { await bot.lookAt(s0.position.offset(0, 1.5, 0)); bot.attack(s0); ciosySwoich++; swojProbowany = true; await sleep(700); continue; }
      else if (!wrogowie.length) { bot.pathfinder.setGoal(new goals.GoalNear(s0.position.x, s0.position.y, s0.position.z, 2)); await sleep(500); continue; }
    }
    const cel = wrogowie.sort((a, b) => a.position.distanceTo(bot.entity.position) - b.position.distanceTo(bot.entity.position))[0];
    if (!cel) { await sleep(500); continue; }
    const d = cel.position.distanceTo(bot.entity.position);
    if (d < 3) {
      bot.pathfinder.setGoal(null);
      await bot.lookAt(cel.position.offset(0, 1.5, 0)); bot.attack(cel); ciosy++; await sleep(650);
    }
    else { bot.pathfinder.setGoal(new goals.GoalNear(cel.position.x, cel.position.y, cel.position.z, 2)); await sleep(400); }
  }
  w.sprawdz('manekin-znany-botowi', typy.size > 0, `typy: ${[...typy].join(',')}`);
  w.sprawdz('bot-bil-napastnikow', ciosy > 0, `ciosy ${ciosy}, trafione (entityHurt) ${trafione}`);
  w.sprawdz('manekiny-bija-gracza', minHp < 20, `min HP bota ${minHp}`);
  w.sprawdz('ciosy-gracza-doszly', ciosyDoszly > 0, `serwer: doszlo ${ciosyDoszly} z ${ciosyWszystkie} (reszta = zablokowane, m.in. cios w swojego)`);
  w.sprawdz('cios-swojego-sprobowany', ciosySwoich > 0, `proby ${ciosySwoich} (wynik w zapisie: cios-blok)`);
  console.log(`NAPAD ${id} nick ${nick}`);
}, { limitMs: 200000 });
