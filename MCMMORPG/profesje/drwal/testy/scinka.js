'use strict';
// Drwal: scinka jak gracz (bot BEZ OP, klon --z-kreatorem, las z MCMMORPG/profesje/drwal/przygotuj_klon.sh).
// RCON tylko do sceny (tp na start przypadku, siekiera, difficulty) i prawdy (rzemioslo drwal <nick> = stan sesji, hp).
// Gesty jak klient: LPM w klode = block_dig START + machniecie; LPM w sesji = machniecie; trzymany PPM = block_place co 200 ms.
// Lezacego pnia bot nie widzi (BlockDisplay), wiec punkt celowania bierze z `rzemioslo drwal <nick> cel <nr>` — gracz widzi go okiem.
// Uruchom: LIMIT_MS=900000 run.sh <klon> /sciezka/scinka.js [przypadki po przecinku]
const { scenariusz, sleep, postac, stan, ruchGracza, idz } = require('/bot/lib');
const { Vec3 } = require('vec3');

const wybrane = (process.argv[2] || 'bez-siekiery,czysto,sprochnialy,zawis,gdzie-chce,w-pasie,zywica').split(',');
const Y = -59;

scenariusz('drwal-scinka', async ({ w, polacz, rcon }) => {
  const sfx = String(Date.now() % 10000).padStart(4, '0');
  const nick = `Dr${sfx}`, nick2 = `Dw${sfx}`;
  const bot = await polacz({ nick, czasMs: 90000 });
  await postac.wejdz(bot, { imie: `Drw${sfx}` });
  ruchGracza(bot);
  let seq = 1;
  const tp = (x, z, yaw = 0, kto = nick) => rcon(`execute in minecraft:overworld run minecraft:teleport ${kto} ${x} ${Y} ${z} ${yaw} 0`);
  const st = async () => (await rcon(`rzemioslo drwal ${nick}`)).replace(/^\[Rzemioslo\] /, '').trim();
  // lookAt(..., true) w mineflayer 4.39 NIE wysyla zmiany samego pitchu (serwer zostaje przy starym) — obrot bez force, jak mysz
  const patrz = async (x, y, z) => { await bot.lookAt(new Vec3(x, y, z), false); await sleep(160); };
  const lpmBlok = async () => {   // LPM w blok: start kopania + machniecie (jak klient), zaraz anulowane
    const b = bot.blockAtCursor(5);
    if (!b) return false;
    bot._client.write('block_dig', { status: 0, location: b.position, face: 1, sequence: seq++ });
    bot.swingArm();
    await sleep(60);
    bot._client.write('block_dig', { status: 1, location: b.position, face: 1, sequence: seq++ });
    return true;
  };
  const ppmRaz = () => {
    const b = bot.blockAtCursor(5);
    const loc = b ? b.position : bot.entity.position.floored();
    bot._client.write('block_place', { location: loc, direction: 1, hand: 0, cursorX: 0.5, cursorY: 0.5, cursorZ: 0.5,
      insideBlock: false, sequence: seq++, worldBorderHit: false });
    bot.swingArm();   // klient macha po PPM siekiera w klode (przewidziane korowanie) — plugin ma to odroznic od ciosu
  };
  const trzymajPpm = async (ms) => { const k = Date.now() + ms; while (Date.now() < k) { ppmRaz(); await sleep(200); } };
  const ciosy = async (n, x, y, z) => { for (let i = 0; i < n; i++) { await patrz(x, y, z); await lpmBlok(); await sleep(450); } };
  const siekiera = async () => {
    await rcon(`clear ${nick}`);
    await rcon(`rzemioslo narzedzie ${nick} siekiera 1`);
    const r = await rcon(`rzemioslo doreki ${nick} PROF_SIEKIERA_1 5`);
    await sleep(400);
    if (!/doreki/.test(r)) throw Error('siekiera: ' + r);
  };
  const czekajFaza = async (re, ms = 8000) => { const k = Date.now() + ms; let s = ''; while (Date.now() < k) { s = await st(); if (re.test(s)) return s; await sleep(250); } return s; };
  // lezacy pien: okrzesanie korony i ciecie na klody (cel z serwera, gracz widzi displaye)
  const obrobka = async (drzazgi = 0) => {
    const s0 = await st();
    const seg = Number((s0.match(/segmenty=(\d+)/) || [])[1] || 0);
    for (let g = 0; g < seg; g++) {
      const c = (await rcon(`rzemioslo drwal ${nick} cel ${g}`)).match(/cel=([-\d.]+),([-\d.]+),([-\d.]+) bok=([-\d.]+),([-\d.]+)/);
      if (!c) { w.sprawdz(`cel-${g}`, false, 'brak celu'); return; }
      const [x, y, z, bx, bz] = c.slice(1).map(Number);
      const r = await idz(bot, x + bx * 2, Y, z + bz * 2, { r: 0, ms: 15000 });
      if (!r.ok) console.log('trasa do cel', g, r.opis);
      const n = g === 0 ? 3 : 2;
      const c2 = (await rcon(`rzemioslo drwal ${nick} cel ${g}`)).match(/cel=([-\d.]+),([-\d.]+),([-\d.]+)/);   // z miejsca, gdzie stanal
      const [ax, ay, az] = c2 ? c2.slice(1).map(Number) : [x, y, z];
      for (let i = 0; i < n; i++) { await patrz(ax, ay, az); bot.swingArm(); await sleep(450); }
      if (g === 1 && drzazgi) {   // kilka ciosow w srodek klody — drzazgi
        const d = (await rcon(`rzemioslo drwal ${nick} cel 100`)).match(/cel=([-\d.]+),([-\d.]+),([-\d.]+)/);
        for (let i = 0; i < drzazgi; i++) { await patrz(+d[1], +d[2], +d[3]); bot.swingArm(); await sleep(450); }
      }
    }
  };
  const wynik = async (re = /brak sesji/, ms = 6000) => czekajFaza(re, ms);

  await rcon('minecraft:difficulty normal');

  // ---------- bez siekiery: komunikat, brak sesji
  if (wybrane.includes('bez-siekiery')) {
    await rcon(`clear ${nick}`);
    await tp(127.3, 40.5, 90);
    await sleep(800);
    await patrz(125.5, Y + 0.6, 40.5);
    await lpmBlok(); await sleep(600);
    const s = await st();
    w.sprawdz('bez-siekiery', /brak sesji/.test(s), s);
  }

  // ---------- czysto (dab 125,40 na wschod) + puszczenie za wczesnie + drzazgi + drugi gracz w pasie upadku
  if (wybrane.includes('czysto')) {
    await siekiera();
    await tp(127.3, 40.5, 90);
    await sleep(800);
    await ciosy(6, 125.5, Y + 0.55, 40.5);
    let s = await st();
    w.sprawdz('wrab-6', /ciosy=6/.test(s), s);
    const r1 = await idz(bot, 123.6, Y, 40.5, { r: 0, ms: 10000 });
    await patrz(125.5, Y + 0.85, 40.5);
    await trzymajPpm(2300);
    await sleep(600);
    s = await st();
    w.sprawdz('za-wczesnie-stoi', /faza=RZAZ/.test(s), `${s} trasa ${r1.opis}`);
    const b2 = await polacz({ nick: nick2, czasMs: 90000 });
    await postac.wejdz(b2, { imie: `Dwi${sfx}` });
    await tp(128.5, 40.6, 90, nick2);
    await sleep(1500);
    const hp2 = await stan.hp(nick2);
    await patrz(125.5, Y + 0.85, 40.5);
    await trzymajPpm(900);   // postep ~0,54 + ~0,26 = ~0,8: pasmo trzeszczenia
    s = await czekajFaza(/faza=(OSTRZEZENIE|PADA|LEZY)/, 3000);
    w.sprawdz('upadek-na-wschod', Number((s.match(/kier=([-\d.]+),/) || [])[1]) > 0.8, s);
    s = await czekajFaza(/faza=LEZY/, 6000);
    w.sprawdz('lezy', /faza=LEZY/.test(s), s);
    await sleep(500);
    const hp2po = await stan.hp(nick2);
    w.sprawdz('widz-przygnieciony', hp2po < hp2, `hp drugiego ${hp2} -> ${hp2po}`);
    await obrobka(3);
    s = await wynik();
    w.sprawdz('czysto-koniec', /brak sesji/.test(s), s);
    try { b2.quit(); } catch {}
  }

  // ---------- sprochnialy (dab 125,52): gluchy pien, 0 drewna
  if (wybrane.includes('sprochnialy')) {
    await siekiera();
    await rcon('rzemioslo drzewo sprochnialy world 125 -59 52');
    await tp(127.3, 52.5, 90);
    await sleep(800);
    await ciosy(6, 125.5, Y + 0.55, 52.5);
    await idz(bot, 123.6, Y, 52.5, { r: 0, ms: 10000 });
    await patrz(125.5, Y + 0.85, 52.5);
    await trzymajPpm(3300);
    const s = await wynik(/brak sesji/, 9000);
    w.sprawdz('sprochnialy-koniec', /brak sesji/.test(s), s);
  }

  // ---------- zawis (dab 140,52 na wschod, sasiad 144,52) + klin od zachodu
  if (wybrane.includes('zawis')) {
    await siekiera();
    await tp(142.0, 52.5, 90);
    await sleep(800);
    await ciosy(6, 140.5, Y + 0.55, 52.5);
    await idz(bot, 138.6, Y, 52.5, { r: 0, ms: 10000 });
    await patrz(140.5, Y + 0.85, 52.5);
    await trzymajPpm(3300);
    let s = await czekajFaza(/faza=(WISI|LEZY)/, 6000);
    w.sprawdz('zawisl', /faza=WISI/.test(s), s);
    if (/WISI/.test(s)) {
      await ciosy(5, 140.5, Y + 0.5, 52.5);   // klin w pniak od zachodu (strona przeciwna upadkowi)
      s = await czekajFaza(/faza=LEZY/, 6000);
      w.sprawdz('klin-spada', /faza=LEZY/.test(s), s);
      await obrobka(0);
    }
    s = await wynik();
    w.sprawdz('zawis-koniec', /brak sesji/.test(s), s);
  }

  // ---------- gdzie chce (dab 110,40): bez wrebu, sam rzaz z zachodu
  if (wybrane.includes('gdzie-chce')) {
    await siekiera();
    await tp(108.6, 40.5, 270);
    await sleep(800);
    await patrz(110.5, Y + 0.85, 40.5);
    await ppmRaz(); await sleep(300);
    await trzymajPpm(3800);
    let s = await czekajFaza(/faza=(OSTRZEZENIE|PADA|LEZY|WISI)/, 3000);
    w.sprawdz('bez-wrebu-pada', /faza=(OSTRZEZENIE|PADA|LEZY|WISI)/.test(s), s);
    s = await czekajFaza(/faza=(LEZY|WISI)/, 6000);
    if (/LEZY/.test(s)) await obrobka(0);
    else if (/WISI/.test(s)) await idz(bot, 100, Y, 40.5, { r: 1, ms: 10000 });   // odchodzi od zawieszonego
    s = await wynik(/brak sesji/, 15000);
    w.sprawdz('gdzie-chce-koniec', /brak sesji/.test(s), s);
  }

  // ---------- w pasie upadku (dab 140,40): czysto, ale drwal wchodzi tam, gdzie pada
  if (wybrane.includes('w-pasie')) {
    await siekiera();
    await stan.lecz(nick);
    await tp(142.0, 40.5, 90);
    await sleep(800);
    await ciosy(6, 140.5, Y + 0.55, 40.5);
    await idz(bot, 138.6, Y, 40.5, { r: 0, ms: 10000 });
    await patrz(140.5, Y + 0.85, 40.5);
    const hp = await stan.hp(nick);
    await trzymajPpm(3300);
    await idz(bot, 142.5, Y, 40.5, { r: 0, ms: 3000 });
    let s = await czekajFaza(/faza=LEZY|brak sesji/, 6000);
    await sleep(400);
    const hpPo = await stan.hp(nick);
    w.sprawdz('drwal-przygnieciony', hpPo < hp || bot.health === 0, `hp ${hp} -> ${hpPo} ${s}`);
    if (bot.health === 0) { bot.respawn && bot.respawn(); await sleep(2000); }
    s = await wynik(/brak sesji/, 4000);
    if (!/brak sesji/.test(s)) { await idz(bot, 150, Y, 46, { r: 1, ms: 10000 }); s = await wynik(/brak sesji/, 20000); }
  }

  // ---------- zywica (swierk 125,64): spała wyzej, potem scinka po kilku minutach = drewno smolne (czesciowo)
  if (wybrane.includes('zywica')) {
    await siekiera();
    await tp(127.3, 64.5, 90);
    await sleep(800);
    await ciosy(3, 125.5, Y + 1.7, 64.5);
    let s = await st();
    w.sprawdz('spala', /brak sesji/.test(s), s);
    await sleep(1200);
    await ciosy(6, 125.5, Y + 0.55, 64.5);
    await idz(bot, 123.6, Y, 64.5, { r: 0, ms: 10000 });
    await patrz(125.5, Y + 0.85, 64.5);
    await trzymajPpm(3300);
    s = await czekajFaza(/faza=(LEZY|WISI)/, 7000);
    if (/LEZY/.test(s)) await obrobka(0);
    s = await wynik(/brak sesji/, 8000);
    w.sprawdz('zywica-koniec', /brak sesji/.test(s), s);
  }
}, { limitMs: Number(process.env.LIMIT_MS || 900000) });
