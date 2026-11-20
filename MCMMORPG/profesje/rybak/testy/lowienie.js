'use strict';
// Rybak: lowienie jak gracz (bot BEZ OP, klon --z-kreatorem, stawy z MCMMORPG/profesje/rybak/przygotuj_klon.sh).
// Gesty jak klient: PPM wedka = use_item (zarzut / zaciecie / zwiniecie), Shift = zwijanie w holu, kamera = kontra.
// RCON tylko do sceny (tp, wedka, wymus gatunku — losowosc zostaje w ruchu ryby) i prawdy (rzemioslo rybak <nick> = stan).
// Wynik gracz widzi na czacie (§2[Rybak] ...) — test czyta te sama linie.
// Uruchom: LIMIT_MS=900000 run.sh <klon> /sciezka/lowienie.js [przypadki po przecinku]
const { scenariusz, sleep, postac } = require('/bot/lib');
const { Vec3 } = require('vec3');

const wybrane = (process.argv[2] || 'vanilla,za-wczesnie,drobnica,spozniony,czysto,zerwanie,bez-kontry,bierny,dar-wody,okaz,slaba-wedka,schowana').split(',');
const Y = -59;

scenariusz('rybak-lowienie', async ({ w, polacz, rcon }) => {
  const sfx = String(Date.now() % 10000).padStart(4, '0');
  const nick = `Ry${sfx}`;
  const bot = await polacz({ nick, czasMs: 90000 });
  await postac.wejdz(bot, { imie: `Ryb${sfx}` });
  const czat = [], pasek = [];
  bot.on('messagestr', (m, poz) => { (poz === 'game_info' ? pasek : czat).push(m); });
  const st = async () => (await rcon(`rzemioslo rybak ${nick}`)).replace(/^\[Rzemioslo\] /, '').trim();
  const pole = (s, k) => { const m = s.match(new RegExp(`${k}=([-\\w.,]+)`)); return m ? m[1] : null; };
  const patrz = async (x, y, z) => { await bot.lookAt(new Vec3(x, y, z), false); await sleep(160); };
  const ppm = () => bot.activateItem();
  const tp = (x, z, kto = nick) => rcon(`execute in minecraft:overworld run minecraft:teleport ${kto} ${x} ${Y} ${z} -90 20`);
  const wedka = async () => {
    await rcon(`clear ${nick}`);
    await rcon(`rzemioslo narzedzie ${nick} wedka 1`);
    const r = await rcon(`rzemioslo doreki ${nick} PROF_WEDKA_1 5`);
    await sleep(400);
    if (!/doreki/.test(r)) throw Error('wedka: ' + r);
  };
  const czekaj = async (re, ms = 8000) => { const k = Date.now() + ms; let s = ''; while (Date.now() < k) { s = await st(); if (re.test(s)) return s; await sleep(100); } return s; };
  const zwin = async () => { if (!/brak sesji/.test(await st())) { ppm(); await sleep(400); } };
  // zarzut w staw T1 (brzeg x=109, woda od x=110)
  const zarzuc = async (x = 108.5, z = 45.5, cel = [116, -60, 45.5]) => {
    bot.setControlState('sneak', false);
    await tp(x, z); await sleep(700);
    await patrz(...cel);
    ppm();
    return czekaj(/faza=CZEKA/, 5000);
  };
  const wymus = (gat, za = 10) => rcon(`rzemioslo rybak ${nick} wymus ${gat} ${za}`);
  const ostatniaLinia = (od) => czat.slice(od).filter(m => /\[Rybak\]/.test(m)).pop() || '';
  // hol: Shift zwija; przy zrywie kamera w przeciwna strone i puszczony Shift; trzeszczy -> puszcza
  const hol = async ({ kontra = true, popuszczaj = true, bierny = false, maxMs = 60000 } = {}) => {
    const k = Date.now() + maxMs; let s = '', zr = 0, kon = 0;
    while (Date.now() < k) {
      s = await st();
      if (/brak sesji/.test(s)) break;
      const nap = Number(pole(s, 'nap')), kier = Number(pole(s, 'kier'));
      const zryw = /zryw=true/.test(s), zapow = /zapow=true/.test(s);
      const [hx, hy, hz] = (pole(s, 'hak') || '0,0,0').split(',').map(Number);
      const p = bot.entity.position;
      let fx = hx - p.x, fz = hz - p.z; const l = Math.hypot(fx, fz) || 1; fx /= l; fz /= l;
      const rx = -fz, rz = fx;   // prawo gracza patrzacego na splawik
      if (bierny) { if (zryw) zr++; bot.setControlState('sneak', !(nap > 0.62 || zryw)); await sleep(100); continue; }   // kamera stoi
      if ((zryw || zapow) && kontra) { await bot.lookAt(new Vec3(hx - rx * kier * 4, hy, hz - rz * kier * 4), false); if (zryw) kon++; }
      else await bot.lookAt(new Vec3(hx, hy, hz), false);
      if (zryw) zr++;
      let zwijaj = true;
      if (popuszczaj && (nap > 0.62 || (zryw && kontra))) zwijaj = false;
      bot.setControlState('sneak', zwijaj);
      await sleep(100);
    }
    bot.setControlState('sneak', false);
    return { s, zr, kon };
  };

  await rcon('minecraft:difficulty peaceful');
  await rcon(`rzemioslo flaga ${nick} rybak_wprawa 0`);

  // ---------- vanilla: zwykla wedka = stare lowienie, bez sceny, podpowiedz o wedce profesji
  if (wybrane.includes('vanilla')) {
    await rcon(`clear ${nick}`);
    await rcon(`minecraft:item replace entity ${nick} hotbar.5 with minecraft:fishing_rod`);
    bot.setQuickBarSlot(5); await sleep(400);
    const p0 = pasek.length;
    await zarzuc();
    await sleep(1500);
    const s = await st();
    w.sprawdz('vanilla-bez-sceny', /brak sesji/.test(s) && !/ lot/.test(s), s);
    w.sprawdz('vanilla-podpowiedz', pasek.slice(p0).some(m => /wędk/i.test(m)), pasek.slice(p0).join(' | ').slice(0, 200));
    ppm(); await sleep(400);
  }

  await wedka();

  // ---------- za wczesnie: PPM w zapowiedzi (pierwsze drgniecia) = ryba puszcza, 0
  if (wybrane.includes('za-wczesnie')) {
    await zarzuc();
    await wymus('ryba', 5);
    const s = await czekaj(/faza=ZAPOWIEDZ/, 3000);
    await sleep(300);
    const c0 = czat.length, p0 = pasek.length;
    ppm(); await sleep(500);
    const s2 = await st();
    w.sprawdz('za-wczesnie', /ZAPOWIEDZ/.test(s) && /brak sesji/.test(s2) && pasek.slice(p0).some(m => /Za wcześnie/.test(m)), `${s.slice(0, 60)} -> ${s2} | ${pasek.slice(p0).join('|')}`);
    w.sprawdz('za-wczesnie-0', !czat.slice(c0).some(m => /\[Rybak\].*×/.test(m)), czat.slice(c0).join('|'));
  }

  // ---------- drobnica: zaciecie drobnicy = nic; przeczekana = wraca do czekania
  if (wybrane.includes('drobnica')) {
    await zarzuc();
    await wymus('drobnica', 5);
    let s = await czekaj(/wz=DROBNICA/, 3000);
    const s2 = await czekaj(/faza=CZEKA/, 5000);
    w.sprawdz('drobnica-przeczekana', /DROBNICA/.test(s) && /faza=CZEKA/.test(s2), `${s.slice(0, 50)} -> ${s2.slice(0, 40)}`);
    await wymus('drobnica', 5);
    s = await czekaj(/wz=DROBNICA/, 3000);
    await sleep(600);
    const p0 = pasek.length;
    ppm(); await sleep(500);
    w.sprawdz('drobnica-zacieta', /brak sesji/.test(await st()) && pasek.slice(p0).some(m => /drobnic/.test(m)), pasek.slice(p0).join('|'));
  }

  // ---------- spozniony: brak PPM w oknie = przyneta zjedzona
  if (wybrane.includes('spozniony')) {
    await zarzuc();
    await wymus('ryba', 5);
    await czekaj(/faza=OKNO/, 4000);
    const p0 = pasek.length;
    const s = await czekaj(/brak sesji/, 4000);
    w.sprawdz('spozniony', /brak sesji/.test(s) && pasek.slice(p0).some(m => /zdjęła przynętę/.test(m)), pasek.slice(p0).join('|'));
    await sleep(300); ppm(); await sleep(400);   // zwija pusty zestaw
  }

  // ---------- czysto: zaciecie w oknie, hol z kontra i popuszczaniem = ryba (dobre/wyborne)
  const czystyPolow = async (nazwa, gat, opcje) => {
    await zarzuc();
    await wymus(gat, 5);
    const s = await czekaj(/faza=OKNO/, 4000);
    await sleep(150);
    ppm();
    const s1 = await czekaj(/faza=HOL/, 1500);
    const c0 = czat.length;
    const h = await hol(opcje);
    await sleep(500);
    const linia = ostatniaLinia(c0);
    console.log(nazwa, 'okno:', s.slice(0, 60), '| hol zryw', h.zr, 'kontra', h.kon, '|', linia);
    return { s1, linia, h };
  };
  if (wybrane.includes('czysto')) {
    const { s1, linia, h } = await czystyPolow('czysto', 'ryba', {});
    w.sprawdz('czysto-hol', /faza=HOL/.test(s1), s1.slice(0, 60));
    w.sprawdz('czysto-ryba', /\[Rybak\].*1× Świeży połów.*\((wyborne|dobre)\)/.test(linia), `${linia} zrywy=${h.zr}`);
  }

  // ---------- zerwanie: Shift caly czas, bez kontry = trzeszczy, pęka, 0
  if (wybrane.includes('zerwanie')) {
    const p0 = pasek.length;
    const { linia } = await czystyPolow('zerwanie', 'ryba', { kontra: false, popuszczaj: false });
    w.sprawdz('zerwanie-trzeszczy', pasek.slice(p0).some(m => /trzeszczy/.test(m)), pasek.slice(p0).filter(m => /trzeszcz|pękła/.test(m)).join('|'));
    w.sprawdz('zerwanie-0', /nic tym razem/.test(linia) && !/×/.test(linia), linia);
  }

  // ---------- bez kontry: popuszcza, ale kamery nie odwraca = gorsza jakosc
  if (wybrane.includes('bez-kontry')) {
    const { linia, h } = await czystyPolow('bez-kontry', 'ryba', { kontra: false });
    w.sprawdz('bez-kontry-gorzej', h.zr === 0 || /\((słabe|dobre)\)/.test(linia) || /nic tym razem/.test(linia), `${linia} zrywy=${h.zr}`);
  }

  // ---------- bierny: popuszcza, ale kamera stoi = ryba sama odjezdza w bok, to NIE jest kontra (QA 20.11: dawniej 46/47 za darmo)
  if (wybrane.includes('bierny')) {
    const { linia, h } = await czystyPolow('bierny', 'ryba', { bierny: true });
    w.sprawdz('bierny-bez-kontry', h.zr === 0 || /bez kontry/.test(linia) || /nic tym razem/.test(linia), `${linia} zrywy=${h.zr}`);
  }

  // ---------- dar wody: tonie i stoi, ciezki hol bez zrywow = poboczny (malz / perla)
  if (wybrane.includes('dar-wody')) {
    const { linia } = await czystyPolow('dar-wody', 'dar_wody', {});
    w.sprawdz('dar-wody', /\[Rybak\].*1× (Małż|Perła|Esencja)/.test(linia), linia);
  }

  // ---------- okaz: dlugi odjazd, mocny hol z wyskokami = 2 ryby
  if (wybrane.includes('okaz')) {
    const { linia } = await czystyPolow('okaz', 'okaz', { maxMs: 90000 });
    w.sprawdz('okaz', /Okaz! 2× Świeży połów/.test(linia) || /nic tym razem/.test(linia), linia);
    w.sprawdz('okaz-zlowiony', /Okaz! 2×/.test(linia), linia);
  }

  // ---------- slaba wedka: staw ze znacznikiem lowisko_t2, wedka I = brak sceny + komunikat
  if (wybrane.includes('slaba-wedka')) {
    await zwin();
    const p0 = pasek.length;
    bot.setControlState('sneak', false);
    await tp(143.5, 40.5); await sleep(700);
    await patrz(150, -60, 40.5);
    ppm(); await sleep(2500);
    const s = await st();
    w.sprawdz('slaba-wedka', /brak sesji/.test(s) && pasek.slice(p0).some(m => /II/.test(m)), `${s} | ${pasek.slice(p0).join('|').slice(0, 160)}`);
    ppm(); await sleep(400);
  }

  // ---------- schowana wedka w holu: wedki profesji nie ma w rece = sesja konczy bez lupu
  if (wybrane.includes('schowana')) {
    await zarzuc();
    await wymus('ryba', 5);
    await czekaj(/faza=OKNO/, 4000);
    await sleep(150); ppm();
    await czekaj(/faza=HOL/, 1500);
    const c0 = czat.length, p0s = pasek.length;
    // bot na klonie nie zmienia slotu (held_item_slot odrzucany takze bez lowienia, 20.11 — przyczyna niezbadana),
    // wiec wedke z reki zdejmuje RCON: ta sama sciezka kodu (wedka(p) == null), nie gest gracza
    await rcon(`minecraft:item replace entity ${nick} hotbar.5 with minecraft:air`); await sleep(1000);
    const s = await st();
    w.sprawdz('schowana', /brak sesji/.test(s) && pasek.slice(p0s).some(m => /Schowałeś/.test(m)) && !czat.slice(c0).some(m => /\[Rybak\].*×/.test(m)), `${s} | ${ostatniaLinia(c0)}`);
    bot.setQuickBarSlot(5);
  }
  console.log('PASEK:', pasek.filter(m => m.trim()).slice(-25).join(' || ').slice(0, 1500));
  console.log('CZAT:', czat.filter(m => /Rybak/.test(m)).join(' || '));
  try { bot.quit(); } catch {}
}, { limitMs: Number(process.env.LIMIT_MS || 900000) });
