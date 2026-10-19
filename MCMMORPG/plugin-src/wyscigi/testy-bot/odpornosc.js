'use strict';
// Sciezki nieidealne: komenda w trakcie jazdy, obrazenia, wierzchowiec wyrzucony za plot (powrot na tor bez utraty postepu),
// gaz wcisniety za wczesnie (splozenie), relog w trakcie jazdy (sprzatniecie + powrot po ponownym wejsciu).
const { scenariusz } = require('/bot/lib');
const { narzedzia, autopilot, wejscie, patrz, sleep, czekaj } = require('./wyscigi-lib');

scenariusz('wyscigi-odpornosc', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const nick = 'WyscigOdp';
  let bot = await polacz({ nick });
  await sleep(2500);
  const przed = await t.pozGracza(nick);
  const czat = []; bot.on('message', m => czat.push(m.toString()));

  // 1. Gaz od poczatku odliczania = za wczesnie
  w.sprawdz('start', /OK/.test(await rcon(`wyscigi czasowka ${nick} puszczorog`)), '');
  bot.physicsEnabled = false;
  const tGaz = Date.now();
  while (Date.now() - tGaz < 7000) {
    wejscie(bot, { forward: true });
    const p = await t.poz(nick);
    if (p && p.faza === 'JAZDA') break;
    await sleep(100);
  }
  const ev1 = await t.zdarzenia(nick);
  w.sprawdz('za-wczesnie-splozenie', ev1.some(e => /start-za-wczesnie/.test(e)), ev1.filter(e => /START|wczes/.test(e)).join(' | '));
  await sleep(500);
  const pKara = await t.poz(nick);
  w.sprawdz('kara-stoi', pKara && pKara.v < 0.05, JSON.stringify(pKara));

  // 2. Komenda w trakcie jazdy
  bot.chat('/spawn');
  await sleep(1200);
  w.sprawdz('komenda-zablokowana', (await t.wymiar(nick)).includes('wyscigi') && czat.some(c => /komendy są wyłączone/.test(c)), czat.slice(-1)[0]);

  // 3. Obrazenia (np. ogien, upadek, mob) — gracz nietykalny
  const hp0 = await t.hp(nick);
  await rcon(`minecraft:damage ${nick} 6`);
  await sleep(500);
  w.sprawdz('nietykalny', (await t.hp(nick)) === hp0, `${hp0} -> ${await t.hp(nick)}`);

  // 4. Jazda kawalek, potem wierzchowiec za plotem -> powrot na tor bez utraty postepu
  const tor = await t.tor();
  await autopilot(bot, t, nick, tor, { stop: p => p.postep > 60 });
  const p1 = await t.poz(nick);
  await rcon(`minecraft:execute in minecraft:wyscigi as @e[type=minecraft:wolf,limit=1] at @s run minecraft:teleport @s ~ 50 ~`);
  const powrot = await czekaj(async () => (await t.zdarzenia(nick)).some(e => /powrot-na-tor/.test(e)), 6000, 200);
  await sleep(1500);
  const p2 = await t.poz(nick);
  w.sprawdz('powrot-na-tor', !!powrot, (await t.zdarzenia(nick)).filter(e => /powrot/.test(e)).join(''));
  w.sprawdz('postep-zachowany', p2 && p1 && p2.postep > p1.postep - 8 && p2.y < 66, `przed ${p1?.postep} po ${p2?.postep}, y ${p2?.y}`);
  w.sprawdz('nadal-siedzi', p2 && p2.kierowca, JSON.stringify(p2));
  const dalej = await autopilot(bot, t, nick, tor, { stop: p => p.postep > p2.postep + 25, limitMs: 20000 });
  w.sprawdz('jedzie-dalej-po-powrocie', dalej.koniec === 'stop', JSON.stringify(dalej.ost));

  // 5. Relog w trakcie jazdy
  bot.quit();
  await sleep(2000);
  w.sprawdz('relog-sprzata-wierzchowca', (await t.wilki()) === 0, `wilki ${await t.wilki()}`);
  w.sprawdz('relog-stan-pusty', /kierowcy: 0/.test(await rcon('wyscigi stan')), '');
  bot = await polacz({ nick });
  const wrocil = await czekaj(async () => !(await t.wymiar(nick)).includes('wyscigi'), 8000, 400);
  const po = await t.pozGracza(nick);
  w.sprawdz('po-relogu-poza-torem', !!wrocil && przed && po && Math.hypot(po.x - przed.x, po.z - przed.z) < 1.5, `przed ${JSON.stringify(przed)} po ${JSON.stringify(po)} ${await t.wymiar(nick)}`);
  // 6. Wejscie z niedozwolonego swiata
  await rcon(`minecraft:execute in minecraft:the_nether run minecraft:teleport ${nick} 0.5 120 0.5`);
  await sleep(800);
  if (!(await t.wymiar(nick)).includes('nether')) { w.pomin('odmowa-z-innego-swiata', 'brak Netheru na klonie'); return; }
  const odm = await rcon(`wyscigi czasowka ${nick} wilkor`);
  w.sprawdz('odmowa-z-innego-swiata', /Odmowa/.test(odm), odm);
  await rcon(`minecraft:execute in minecraft:overworld run minecraft:teleport ${nick} ${przed.x} ${przed.y} ${przed.z}`);
}, { limitMs: 200000 });
