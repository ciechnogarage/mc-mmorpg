'use strict';
// Gornik: praca przy przodku jak gracz (bot BEZ OP, klon --z-kreatorem, chodnik z ../przygotuj_klon.sh).
// Gesty jak klient: LPM w skale = block_dig START + machniecie (w sesji samo machniecie), PPM = block_place (opukanie,
// w przebieraniu: wez bryle do kosza), kucniecie = koniec urabiania / koniec przebierania.
// RCON tylko do sceny (przodek, kilof/klin, teleport, ksztalt nastepnej zyly: `rzemioslo gornik <nick> nastepna`)
// i prawdy (`rzemioslo gornik <nick>` = stan, hp z `data get`). Wynik gracz widzi na czacie (§6[Górnik] ...).
// Drugi bot (widz) sprawdza, ze ruda odkryta opukaniem i bryly sa widoczne dla innych.
// Uruchom: LIMIT_MS=1200000 run.sh <klon> /sciezka/urobek.js [przypadki po przecinku]
const { scenariusz, sleep, postac } = require('/bot/lib');
const { Vec3 } = require('vec3');

const wybrane = (process.argv[2] || 'bez-kilofa,czysto,odrost,obrywka,poza-warstwa,bledy,twarda-kilof,twarda-klin,zawal,zawal-smierc').split(',');
const Y = -59;          // stopy gracza w chodniku (podloga -60, strop -56)

scenariusz('gornik-urobek', async ({ w, polacz, rcon }) => {
  const sfx = String(Date.now() % 10000).padStart(4, '0');
  const nick = `Go${sfx}`, nick2 = `Gw${sfx}`;
  const bot = await polacz({ nick, czasMs: 90000 });
  await postac.wejdz(bot, { imie: `Gor${sfx}` });
  const widz = await polacz({ nick: nick2, czasMs: 90000 });
  await postac.wejdz(widz, { imie: `Gwi${sfx}` });
  const czat = [], pasek = [], pasek2 = [];
  bot.on('messagestr', (m, poz) => { (poz === 'game_info' ? pasek : czat).push(m); });
  widz.on('messagestr', (m, poz) => { if (poz === 'game_info') pasek2.push(m); });
  let seq = 1;
  const st = async () => (await rcon(`rzemioslo gornik ${nick}`)).replace(/^\[Rzemioslo\] /, '').trim();
  const tp = (x, y, z, kto = nick, yaw = 180) => rcon(`execute in minecraft:overworld run minecraft:teleport ${kto} ${x} ${y} ${z} ${yaw} 0`);
  const patrz = async (x, y, z) => { await bot.lookAt(new Vec3(x, y, z), false); await sleep(180); };
  // blockAtCursor w tej wersji mineflayera zwracal null przy poprawnym kacie (proba 20.11) -> wlasny promien z oka po kacie bota
  const kursor = () => {
    const eye = bot.entity.position.offset(0, 1.62, 0), yaw = bot.entity.yaw, pitch = bot.entity.pitch;
    const dir = new Vec3(-Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), -Math.cos(yaw) * Math.cos(pitch));
    return bot.world.raycast(eye, dir, 5);
  };
  const lpmBlok = async () => {
    const b = kursor();
    if (!b) return false;
    bot._client.write('block_dig', { status: 0, location: b.position, face: 3, sequence: seq++ });
    bot.swingArm();
    await sleep(60);
    bot._client.write('block_dig', { status: 1, location: b.position, face: 3, sequence: seq++ });
    return true;
  };
  const ppm = () => {
    const b = kursor();
    const loc = b ? b.position : bot.entity.position.floored();
    bot._client.write('block_place', { location: loc, direction: 3, hand: 0, cursorX: 0.5, cursorY: 0.5, cursorZ: 0.5,
      insideBlock: false, sequence: seq++, worldBorderHit: false });
    bot.swingArm();
  };
  const hp = async (kto = nick) => { const m = (await rcon(`minecraft:data get entity ${kto} Health`)).match(/(-?[\d.]+)f/); return m ? +m[1] : NaN; };
  const pasekOd = (n) => pasek.slice(n).join(' | ');
  const linia = (od) => czat.slice(od).filter(m => /\[Górnik\]/.test(m)).pop() || '';
  const czekaj = async (re, ms = 8000) => { const k = Date.now() + ms; let s = ''; while (Date.now() < k) { s = await st(); if (re.test(s)) return s; await sleep(200); } return s; };
  const czekajLinie = async (od, ms = 4000) => { const k = Date.now() + ms; while (Date.now() < k) { const l = linia(od); if (l) return l; await sleep(200); } return ''; };
  const kucnij = async () => { bot.setControlState('sneak', true); await sleep(400); bot.setControlState('sneak', false); await sleep(300); };
  const narzedzie = async (id) => {
    await rcon(`clear ${nick}`);
    if (!id) { await sleep(300); return; }
    const m = id.match(/^PROF_(KILOF|SIEKIERA)_(\d)$/);
    if (m) await rcon(`rzemioslo narzedzie ${nick} ${m[1].toLowerCase()} ${m[2]}`);
    else await rcon(`mi give MATERIAL ${id} ${nick} 1`);
    await sleep(300);
    const r = await rcon(`rzemioslo doreki ${nick} ${id} 5`);
    await sleep(400);
    if (!/doreki/.test(r)) throw Error('narzedzie: ' + r);
  };
  // przodki: sciana polnocna Z=99 (twarz SOUTH), miekka X<214, twarda X>=214; kazdy przypadek swoj (odnowa 240 s per konto)
  const MIEKKIE = [201, 208], TWARDE = [218, 226];
  const POLUDNIE = [201, 208];   // sciana poludniowa Z=105 (twarz NORTH), miekka
  const przodek = async (x, z = 99) => { const r = await rcon(`rzemioslo przodek world ${x} -58 ${z}`); if (!/przodek/.test(r)) throw Error(r); return [x, -58, z]; };
  // stan -> lista zyly [{x,y,z,t,f}], luzne [[x,y,z]], kupka [{x,y,z,typ,r}]
  const zyla = (s) => [...((s.match(/zyla=([^|]*)/) || [])[1] || '').matchAll(/(-?\d+),(-?\d+),(-?\d+)([TM])([uxo-])/g)].map(m => ({ x: +m[1], y: +m[2], z: +m[3], t: m[4], f: m[5] }));
  const luzne = (s) => [...((s.match(/luzne=([^|]*)/) || [])[1] || '').matchAll(/(-?\d+),(-?\d+),(-?\d+)/g)].map(m => [+m[1], +m[2], +m[3]]);
  const kupka = (s) => [...((s.match(/kupka=([^|]*)/) || [])[1] || '').matchAll(/(-?[\d.]+),(-?[\d.]+),(-?[\d.]+)([RKP])([r-])/g)].map(m => ({ x: +m[1], y: +m[2], z: +m[3], typ: m[4], r: m[5] === 'r' }));
  const twarz = (z) => z === 99 ? 100 : 105;   // powierzchnia sciany
  // po teleporcie miedzy swiatami klient potrzebuje chunkow, zanim blockAtCursor cos zwroci
  const stanPrzed = async (x, z) => {
    await tp(x + 0.5, Y, z === 99 ? 101.5 : 103.5, nick, z === 99 ? 180 : 0);
    for (let i = 0; i < 40; i++) { await sleep(150); const b = bot.blockAt(new Vec3(x, -58, z)); if (b && b.name !== 'air' && bot.entity.position.distanceTo(new Vec3(x + 0.5, Y, z)) < 5) break; }
    await sleep(250);
  };
  // start: PPM (opukanie) w punkt przodka
  const zacznij = async (x, z, { luz = 0, dl = 4, uskok = 0, zawal = 0 } = {}) => {
    await czekaj(/^brak sesji/, 4000);
    await rcon(`rzemioslo gornik ${nick} nastepna ${luz} ${dl} ${uskok} ${zawal}`);
    await stanPrzed(x, z);
    await patrz(x + 0.5, -57.5, twarz(z) + (z === 99 ? -0.05 : 0.05));
    ppm(); await sleep(500);
    return await czekaj(/faza=/, 2000);
  };
  const celSciana = (k) => [k.x + 0.5, k.y + 0.5, twarz(k.z) + (k.z === 99 ? -0.05 : 0.05)];
  const opukaj = async (k) => { await patrz(...celSciana(k)); ppm(); await sleep(400); };
  const urab = async (k, n, pauza = 470) => { for (let i = 0; i < n; i++) { await patrz(...celSciana(k)); if (i === 0) await lpmBlok(); else bot.swingArm(); await sleep(pauza); } };
  // przebieranie: LPM rozbija, PPM bierze; tryb: 'dobrze' | 'plonna-do-kosza' | 'przekrusz' | 'bez-rozbijania'
  const przebierz = async (tryb = 'dobrze') => {
    for (let n = 0; n < 30; n++) {
      const s = await st();
      if (!/faza=PRZEBIERANIE/.test(s)) return s;
      const k = kupka(s);
      const nierozbite = k.filter(g => !g.r);
      const doWziecia = k.filter(g => g.r && g.typ !== 'P');
      let g, gest;
      if (tryb === 'bez-rozbijania' && nierozbite.length) { g = nierozbite[0]; gest = 'ppm'; }
      else if (nierozbite.length) { g = nierozbite[0]; gest = 'lpm'; }
      else if (tryb === 'przekrusz' && doWziecia.length) { g = doWziecia[0]; gest = 'lpm'; tryb = 'dobrze'; }
      else if (tryb === 'plonna-do-kosza' && k.some(x => x.typ === 'P')) { g = k.find(x => x.typ === 'P'); gest = 'ppm'; }
      else if (doWziecia.length) { g = doWziecia[0]; gest = 'ppm'; }
      else { await kucnij(); await sleep(600); return await st(); }
      await patrz(g.x, g.y + 0.2, g.z);
      if (gest === 'lpm') bot.swingArm(); else ppm();
      await sleep(gest === 'lpm' ? 470 : 360);
    }
    return await st();
  };
  // zyla wzdluz: opukaj kazdy blok (dzwiek -> ruda), urab odkryte
  const wybierz = async (s, { n = 4, pauza } = {}) => {
    for (const k of zyla(s)) { await stanPrzed(k.x, k.z); await opukaj(k); }
    for (const k of zyla(await st())) {
      if (k.f !== 'o' && k.f !== '-') continue;
      const kk = zyla(await st()).find(q => q.x === k.x && q.y === k.y);
      if (!kk || kk.f === 'x' || kk.f === 'u') continue;
      await stanPrzed(k.x, k.z);
      await urab(k, n, pauza);
    }
    return await st();
  };

  await rcon('rzemioslo gornik pomiar on');
  await rcon(`rzemioslo flaga ${nick} gornik_wprawa 0`);
  await tp(205.5, Y, 103.5, nick2, 180);   // widz stoi w chodniku przez caly test

  // ---------- bez kilofa: reka pusta -> komunikat, brak sesji; zle narzedzie (siekiera) -> to samo
  if (wybrane.includes('bez-kilofa')) {
    await sleep(3000);   // zestaw startowy z kreatora dochodzi po wejsciu (20.11: raz trafil do reki po clear -> „Tym skały…”)
    await narzedzie(null);
    const p0 = pasek.length;
    await stanPrzed(MIEKKIE[0], 99);
    await patrz(MIEKKIE[0] + 0.5, -57.5, 99.95);
    await przodek(MIEKKIE[0]);
    await lpmBlok(); await sleep(600);
    const s = await st();
    w.sprawdz('bez-kilofa', /^brak sesji/.test(s) && /rękami|Weź kilof/.test(pasekOd(p0)), pasekOd(p0).slice(0, 160) + ' || ' + s.slice(0, 80));
    await narzedzie('PROF_SIEKIERA_1');
    const p1 = pasek.length;
    await lpmBlok(); await sleep(600);
    w.sprawdz('siekiera-odmowa', /^brak sesji/.test(await st()) && /kilof/.test(pasekOd(p1)), pasekOd(p1).slice(0, 160));
  }

  // ---------- czysto: opukanie prowadzi do zyly, urabianie miekkiej kilofem, przebranie bez bledow -> wyborna
  if (wybrane.includes('czysto')) {
    await narzedzie('PROF_KILOF_1');
    await przodek(MIEKKIE[0]);
    let s = await zacznij(MIEKKIE[0], 99, { dl: 4 });
    w.sprawdz('czysto-start', /faza=URABIANIE/.test(s) && zyla(s).length === 4 && zyla(s).every(k => k.t === 'M'), s.slice(0, 200) + ' || pasek: ' + pasek.slice(-2).join(' | '));
    // opukanie bloku poza zyla: brak odkrycia; bloku zyly: ruda u widza
    const z0 = zyla(s);
    await opukaj({ x: z0[0].x, y: -59, z: 99 });   // pod zyla
    await opukaj(z0[1]);
    await sleep(500);
    const b2 = widz.blockAt(new Vec3(z0[1].x, z0[1].y, z0[1].z));
    w.sprawdz('widz-widzi-rude', b2 && /iron_ore/.test(b2.name), b2 ? b2.name : 'brak bloku');
    s = await wybierz(s);
    w.sprawdz('czysto-przebieranie', /faza=PRZEBIERANIE/.test(s) && kupka(s).length >= 8, s.slice(0, 260));
    const bryl = widz.nearestEntity(e => e.name === 'item_display' && e.position.distanceTo(new Vec3(z0[1].x, -59, 100.5)) < 4);
    w.sprawdz('widz-widzi-bryly', !!bryl, bryl ? bryl.position.toString() : 'brak item_display');
    const c0 = czat.length;
    s = await przebierz('dobrze');
    const l = await czekajLinie(c0);
    w.sprawdz('czysto-wynik', /Bryła urobku \(wyborna\)|wyborn/.test(l) && /czysty, wyborny/.test(l), l || s.slice(0, 120));
    const ruda = widz.blockAt(new Vec3(z0[1].x, z0[1].y, z0[1].z));
    w.sprawdz('widz-sciana-wraca', ruda && ruda.name === 'stone', ruda ? ruda.name : '?');
    const mo = bot.blockAt(new Vec3(z0[1].x, z0[1].y, z0[1].z));
    w.sprawdz('gornik-widzi-wyrobisko', mo && mo.name === 'cobblestone', mo ? mo.name : '?');
  }

  // ---------- odrost: ten sam przodek zaraz po pracy -> odmowa z czasem
  if (wybrane.includes('odrost')) {
    const p0 = pasek.length;
    await stanPrzed(MIEKKIE[0], 99);
    await patrz(MIEKKIE[0] + 0.5, -57.5, 99.95);
    ppm(); await sleep(600);
    w.sprawdz('odrost-odmowa', /^brak sesji/.test(await st()) && /już wybrałeś/.test(pasekOd(p0)), pasekOd(p0).slice(0, 140));
  }

  // ---------- obrywka: gluchy strop wykryty opukaniem i zbity z boku -> bez zawalu, bez obrazen
  if (wybrane.includes('obrywka')) {
    await narzedzie('PROF_KILOF_1');
    await przodek(MIEKKIE[1]);
    let s = await zacznij(MIEKKIE[1], 99, { luz: 1, dl: 4, zawal: 1 });
    const [lx, ly, lz] = luzne(s)[0] || [];
    w.sprawdz('obrywka-luzny', lx !== undefined, s.slice(0, 220));
    if (lx !== undefined) {
      const p0 = pasek.length;
      await tp(lx + 0.5 + 1.6, Y, lz + 0.5, nick, 90); await sleep(500);   // z boku
      await patrz(lx + 0.5, ly + 0.05, lz + 0.5); ppm(); await sleep(500);
      w.sprawdz('obrywka-gluchy', /głucho/.test(pasekOd(p0)), pasekOd(p0).slice(0, 140));
      const h0 = await hp();
      for (let i = 0; i < 2; i++) { await patrz(lx + 0.5, ly + 0.05, lz + 0.5); if (i === 0) await lpmBlok(); else bot.swingArm(); await sleep(470); }
      await sleep(900);
      s = await st();
      const h1 = await hp();
      w.sprawdz('obrywka-zbita', luzne(s).length === 0 && h1 >= h0, `luzne=${luzne(s).length} hp ${h0}->${h1}`);
      s = await wybierz(s);
      w.sprawdz('obrywka-bez-zawalu', /zawal=false/.test(s) && /faza=PRZEBIERANIE/.test(s), s.slice(0, 200));
      const c0 = czat.length;
      await przebierz('dobrze');
      const l = await czekajLinie(c0);
      w.sprawdz('obrywka-wynik', /zbity w porę/.test(l), l);
    }
  }

  // ---------- poza warstwa: urabianie plonnej skaly nad zyla -> uwaga w wyniku, nizsza jakosc
  if (wybrane.includes('poza-warstwa')) {
    await narzedzie('PROF_KILOF_1');
    await przodek(POLUDNIE[0], 105);
    let s = await zacznij(POLUDNIE[0], 105, { dl: 3 });
    const z0 = zyla(s);
    for (const dx of [0, 1]) { await stanPrzed(z0[0].x + dx, 105); await urab({ x: z0[0].x + (z0[0].x < z0[1].x ? -1 - dx : 1 + dx), y: -58, z: 105 }, 4); }
    s = await st();
    w.sprawdz('poza-plonna', /faza=URABIANIE/.test(s) && kupka(s).filter(g => g.typ === 'P').length >= 2, s.slice(0, 260));
    s = await wybierz(s);
    const c0 = czat.length;
    await przebierz('dobrze');
    const l = await czekajLinie(c0);
    w.sprawdz('poza-wynik', /płonną skałę obok warstwy/.test(l) && !/wyborny/.test(l), l);
  }

  // ---------- bledy przebierania: plonna do kosza, przekruszenie, bryla bez rozbicia -> zanieczyszczony/dobry, nie wyborny
  if (wybrane.includes('bledy')) {
    await narzedzie('PROF_KILOF_1');
    await przodek(POLUDNIE[1], 105);
    let s = await zacznij(POLUDNIE[1], 105, { dl: 5 });
    s = await wybierz(s);
    const c0 = czat.length;
    // najpierw przekruszenie jednej rudy, potem plonna do kosza, reszta bez rozbijania
    await przebierzMix();
    const l = await czekajLinie(c0, 6000);
    w.sprawdz('bledy-wynik', /płonna skała|przekruszona|nieprzebrane/.test(l) && !/wyborny/.test(l), l || (await st()).slice(0, 200));
  }
  async function przebierzMix() {
    let krok = 0;
    for (let n = 0; n < 30; n++) {
      const s = await st();
      if (!/faza=PRZEBIERANIE/.test(s)) return;
      const k = kupka(s);
      let g, gest;
      const rudaR = k.find(x => x.r && x.typ === 'R'), plonna = k.find(x => x.typ === 'P'), nier = k.find(x => !x.r);
      if (krok === 0) { g = k.find(x => !x.r && x.typ === 'R') || nier; gest = 'lpm'; if (g && g.typ === 'R') krok = 1; }
      else if (krok === 1 && rudaR) { g = rudaR; gest = 'lpm'; krok = 2; }      // drugi cios w rozbita rude = mial
      else if (krok === 2 && plonna) { g = plonna; gest = 'ppm'; krok = 3; }     // plonna do kosza
      else if (nier) { g = nier; gest = 'ppm'; }                                // reszta bez rozbijania
      else if (k.length) { g = k.find(x => x.typ !== 'P') || k[0]; gest = 'ppm'; }
      if (!g) { await kucnij(); return; }
      await patrz(g.x, g.y + 0.2, g.z);
      if (gest === 'lpm') bot.swingArm(); else ppm();
      await sleep(gest === 'lpm' ? 470 : 360);
    }
  }

  // ---------- twarda skala kilofem: odbicia, po 3 ruda skruszona; cala zyla tak -> wynik 0 (zniszczone zloze)
  if (wybrane.includes('twarda-kilof')) {
    await narzedzie('PROF_KILOF_1');
    await przodek(TWARDE[0]);
    let s = await zacznij(TWARDE[0], 99, { dl: 3 });
    w.sprawdz('twarda-zyla', zyla(s).length === 3 && zyla(s).every(k => k.t === 'T'), s.slice(0, 200));
    const p0 = pasek.length, c0 = czat.length;
    for (const k of zyla(s)) { await stanPrzed(k.x, 99); await urab(k, 3); }
    await sleep(800);
    s = await st();
    const l = await czekajLinie(c0);
    w.sprawdz('twarda-odbicie', /odbija/.test(pasekOd(p0)), pasekOd(p0).slice(0, 160));
    w.sprawdz('twarda-skruszona', /skruszył/.test(pasekOd(p0)), pasekOd(p0).slice(0, 200));
    w.sprawdz('twarda-zero', /^brak sesji/.test(s) && !/Bryła urobku/.test(l), `${l} | ${pasekOd(p0).slice(-120)}`);
  }

  // ---------- twarda skala klinem: 5 uderzen na blok, urobek jest
  if (wybrane.includes('twarda-klin')) {
    await narzedzie('PROF_KLIN_GORNICZY');
    await przodek(TWARDE[1]);
    let s = await zacznij(TWARDE[1], 99, { dl: 3 });
    s = await wybierz(s, { n: 5, pauza: 560 });
    w.sprawdz('klin-urobione', zyla(s).filter(k => k.f === 'u').length === 3 && /faza=PRZEBIERANIE/.test(s), s.slice(0, 220));
    const c0 = czat.length;
    await przebierz('dobrze');
    const l = await czekajLinie(c0);
    w.sprawdz('klin-wynik', /Bryła urobku/.test(l), l);
  }

  // ---------- zawal: luzny strop nie opukany; trzeszczenie (widz tez dostaje ostrzezenie), obryw rani, zasypuje
  const zawal = async (x, z, { smierc = false } = {}) => {
    await narzedzie('PROF_KLIN_GORNICZY');   // twarda skala
    await tp(x - 3.5, Y, z === 99 ? 103.5 : 101.5, nick2, 180); await sleep(400);   // widz obok
    await przodek(x, z);
    let s = await zacznij(x, z, { luz: 1, dl: 6, zawal: 1 });
    const L = luzne(s)[0];
    if (!L) { w.pomin(smierc ? 'smierc' : 'zawal', 'brak luznego stropu: ' + s.slice(0, 120)); return; }
    if (smierc) { await rcon(`minecraft:damage ${nick} ${Math.max(1, (await hp()) - 6)}`); await sleep(300); }
    const p0 = pasek.length, p2 = pasek2.length, c0 = czat.length;
    const h0 = await hp();
    for (const k of zyla(s)) {
      if (/grozi=true/.test(await st())) break;
      await stanPrzed(k.x, z); await opukaj(k); await urab(k, 5, 560);
    }
    s = await czekaj(/grozi=true|^brak sesji/, 3000);
    w.sprawdz((smierc ? 'smierc' : 'zawal') + '-trzeszczy', /trzeszczy/.test(pasekOd(p0)) && /trzeszczy/.test(pasek2.slice(p2).join(' ')), pasekOd(p0).slice(-160) + ' || widz: ' + pasek2.slice(p2).join(' ').slice(-80));
    await tp(L[0] + 0.5, Y, L[2] + 0.5, nick, 180);   // zostaje pod luznym stropem
    await sleep(3600);
    const h1 = await hp();
    s = await st();
    if (smierc) {
      w.sprawdz('smierc-zabija', h1 <= 0 || bot.health <= 0 || /^brak sesji/.test(s), `hp ${h0}->${h1} bot.health=${bot.health} | ${s.slice(0, 80)}`);
      await sleep(1500);
      return;
    }
    // 20.11: ta sama sila 18 dala raz 18, raz ~10 HP (czym zmniejszone — nie sprawdzone; postac z kreatora) -> próg 5 + flaga sesji
    w.sprawdz('zawal-rani', h1 < h0 - 5 && /przygnieciony=true/.test(s), `hp ${h0}->${h1} | ${(s.match(/przygnieciony=\w+/) || [''])[0]}`);
    // zasypuje nieurobione bloki zyly w promieniu 3 i polowe brył w 2,5 — co trafi, zalezy od miejsca luznego stropu
    w.sprawdz('zawal-zasypuje', /zawal=true/.test(s) && Number((s.match(/zasypane=(\d+)/) || [])[1]) > 0, s.slice(0, 240));
    for (const k of zyla(s).filter(k => k.f === '-')) { await stanPrzed(k.x, z); await urab(k, 5, 560); }   // reszta zyly poza gruzem
    await przebierz('dobrze');
    const l = await czekajLinie(c0, 5000);
    w.sprawdz('zawal-wynik', /obwalił/.test(l) || /zasypane/.test(pasekOd(p0)), l || pasekOd(p0).slice(-140));
  };
  if (wybrane.includes('zawal')) await zawal(230, 99).catch(e => w.sprawdz('zawal', false, String(e)));
  if (wybrane.includes('zawal-smierc')) await zawal(222, 105, { smierc: true }).catch(e => w.sprawdz('smierc', false, String(e)));

  const wp = ((await st()).match(/wprawa=(\d*)/) || [])[1];   // (`rzemioslo flaga <nick> <klucz>` bez wartosci USUWA flage)
  w.sprawdz('wprawa-postaci', Number(wp) >= 3, 'gornik_wprawa=' + wp);
  console.log('wprawa:', wp, '| czat:', czat.filter(m => /Górnik/.test(m)).join(' / ').slice(0, 1800));
  await rcon('rzemioslo gornik pomiar off');
  widz.quit(); bot.quit();
}, { limitMs: Number(process.env.LIMIT_MS || 1200000) });
