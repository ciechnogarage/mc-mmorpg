'use strict';
// Napady: gracz (bot bez OP) dolacza do trwajacego napadu po stronie obrony i walczy z cialami napadu
// (moby MM w przebraniu gracza LD - bot widzi wtedy encje `player`). Bez gracza napad liczy sie symulacja,
// dolaczenie najpierw materializuje ciala, potem teleportuje (LOD, testy/lod.js).
// Strony rozpoznaje po helmie: obronca zelazny, napastnik kolczy albo skorzany (config wyposazenie, przedmioty MMOItems).
// Gracz nosi przedmioty MMOItems jak kazdy gracz (zestaw TEST_PALADIN: pancerz 14, obrona 16) - test liczy, czy cios
// ciala w gracza = trudnosc easy -> obrona MythicLib -> pancerz vanilla (Wyposazenie.java, MythicLib config.yml:34).
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
const obronca = (e) => helm(e) === 'iron_helmet';
const napastnik = (e) => /helmet/.test(helm(e)) && !obronca(e);

// cios ciala w gracza: easy (vanilla Player.hurt: min(d/2+1, d)) -> obrona ML (d*50/(50+obr)) -> pancerz 1.21
function oczekiwany(atak, pancerz, obrona, easy = true) {
  let d = easy ? Math.min(atak / 2 + 1, atak) : atak;
  if (obrona > 0) d = d * 50 / (50 + obrona);
  const g = Math.max(Math.min(pancerz - d / 2, 20), pancerz * 0.2);
  return d * (1 - g / 25);
}
const ZESTAW = ['HELMET', 'CHESTPLATE', 'LEGGINGS', 'BOOTS'];

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
  // TEST_PALADIN wymaga biegłości średniej 1 (MMOItems required-bieglosc-srednia); świeża postać (Human) jej nie ma,
  // więc MMOItems nie da założyć - postać z klasą, która ją daje (czlowiek/rycerz.yml additional-bieglosc-srednia 1)
  console.log('klasa', await rcon(`mmocore admin force-class ${nick} RYCERZ`));
  for (const c of ZESTAW) await rcon(`mi give ARMOR TEST_PALADIN_${c} ${nick}`);
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
  const odczytPancerza = async () => parseFloat(((await rcon(`minecraft:attribute ${nick} minecraft:armor get`)).match(/is (-?[\d.]+)/) || [])[1]);
  let pancerz = 0;
  for (let proba = 0; proba < 4 && pancerz < 13.9; proba++) {
    // PPM zbroja w rece = zalozenie (jak gracz); klik w slot zbroi okna 0 serwer odrzuca (E = Ksiega, 25.10 sonda)
    for (const c of ['golden_helmet', 'golden_chestplate', 'golden_leggings', 'golden_boots']) {
      const it = bot.inventory.items().find((i) => i.name === c);
      if (!it) continue;
      await bot.equip(it, 'hand').catch(() => {}); await sleep(300);
      bot.activateItem(); await sleep(200); bot.deactivateItem(); await sleep(400);
    }
    await sleep(800);
    pancerz = await odczytPancerza();
  }
  // obrona 4 na sztuke TEST_PALADIN (pancerz 3,5 na sztuke) - liczona z tego, co faktycznie zalozone
  const obrona = Math.round(pancerz / 3.5) * 4;
  w.sprawdz('gracz-w-zbroi-mmoitems', pancerz >= 13.9, `pancerz gracza ${pancerz}, obrona ${obrona} (TEST_PALADIN x4 = 14 / 16)`);
  // kazdy spadek HP: obok strzala = cios z luku, inaczej wrecz
  const spadki = [];
  let hpOst = bot.health;
  bot.on('health', () => {
    const d = hpOst - bot.health; hpOst = bot.health;
    if (d <= 0.01 || bot.health <= 0) return;
    const strzala = Object.values(bot.entities).some((e) => e.name === 'arrow' && e.position.distanceTo(bot.entity.position) < 3);
    spadki.push({ d: Math.round(d * 100) / 100, strzala });
  });
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
    const wrogowie = cc.filter(napastnik);
    const swoi = cc.filter(obronca);
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
  // topor 7 / luk 6 (ATAK_*, config wyposazenie); luk w grze 5-7 (predkosc strzaly). Czesc ciosow trafia w klatki
  // nietykalnosci (vanilla odejmuje tylko nadwyzke nad poprzednim ciosem) - stad prog 70% zgodnych
  const wrecz = spadki.filter((s) => !s.strzala).map((s) => s.d), luk = spadki.filter((s) => s.strzala).map((s) => s.d);
  const oW = oczekiwany(7, pancerz, obrona), oBezObr = oczekiwany(7, pancerz, 0), oBezEasy = oczekiwany(7, pancerz, obrona, false);
  const rowne = (x, o) => Math.abs(x - o) < 0.06;
  const zgodne = wrecz.filter((x) => rowne(x, oW)).length;
  w.sprawdz('cios-wrecz-wg-wzoru', wrecz.length > 0 && zgodne >= wrecz.length * 0.7,
    `wrecz ${JSON.stringify(wrecz)} oczek. ${oW.toFixed(2)} (bez obrony ${oBezObr.toFixed(2)}, bez easy ${oBezEasy.toFixed(2)}), zgodnych ${zgodne}/${wrecz.length}`);
  const oL = [5, 6, 7].map((a) => oczekiwany(a, pancerz, obrona));
  w.sprawdz('strzala-wg-wzoru', luk.length === 0 || luk.every((x) => oL.some((o) => rowne(x, o))),
    `luk ${JSON.stringify(luk)} oczek. ${oL.map((o) => o.toFixed(2)).join('/')} (atak 5/6/7)`);
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
