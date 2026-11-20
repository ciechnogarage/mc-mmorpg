'use strict';
// Mysliwy: lowy jak gracz (bot BEZ OP, klon --z-kreatorem, plac z ../przygotuj_klon.sh).
// Gesty jak klient: kucniecie przy rewirze = start, dojscie do tropu, kucanie/sprint w podchodzie, cios (LPM w zwierze),
// PPM w zwloki = wybor ciecia tam, gdzie patrzysz, wzrok na znaku = prowadzenie noza, PPM dziczyzna w ognisko = wedzenie.
// RCON tylko do sceny (rewir, noz, teleport zamiast marszu — ruch bota po kreatorze serwer odrzuca, wiatr ustawiony
// na sztywno, zeby test byl powtarzalny, podpalenie zwierzecia) i prawdy (rzemioslo mysliwy <nick> = stan).
// Wynik gracz widzi na czacie (§2[Myśliwy] ...) — test czyta te sama linie.
// Uruchom: LIMIT_MS=1200000 run.sh <klon> /sciezka/lowy.js [przypadki po przecinku]
const { scenariusz, sleep, postac } = require('/bot/lib');
const { Vec3 } = require('vec3');

const wybrane = (process.argv[2] || 'brak-noza,zgubiony,wiatr,sprint,wzrok,ranne,czysto,zla-kolejnosc,obrazenia,spalona-zero,zajac,wedzenie,zgaszone').split(',');
const Y = -59;

scenariusz('mysliwy-lowy', async ({ w, polacz, rcon }) => {
  const sfx = String(Date.now() % 10000).padStart(4, '0');
  const nick = `My${sfx}`;
  const bot = await polacz({ nick, czasMs: 90000 });
  await postac.wejdz(bot, { imie: `Mys${sfx}` });
  const czat = [], pasek = [];
  bot.on('messagestr', (m, poz) => { (poz === 'game_info' ? pasek : czat).push(m); });
  const st = async () => (await rcon(`rzemioslo mysliwy ${nick}`)).replace(/^\[Rzemioslo\] /, '').trim();
  const pole = (s, k) => { const m = s.match(new RegExp(`${k}=([-\\w.,;@+]+)`)); return m ? m[1] : null; };
  const xyz = (t) => t.split(',').map(Number);
  const patrz = async (x, y, z) => { await bot.lookAt(new Vec3(x, y, z), false); await sleep(160); };
  // PPM w Interaction BEZ obracania glowy: bot.activateEntity robi najpierw lookAt(encja + 1 blok)
  // (mineflayer lib/plugins/inventory.js:250) i psuje celowanie w znak ciecia -> pakiet use_entity recznie
  const uzyj = (it) => bot._client.write('use_entity', { target: it.id, mouse: 0, sneaking: false, hand: 0, location: new Vec3(0, 0, 0) });
  // przy zwlokach kat liczony od oka, w ktore teleport postawil bota (pozycja bota po stronie klienta bywala
  // nieodswiezona: 3 kliki z tym samym katem obok celu, przebieg 9) -> bot.look z katem jak lookAt w mineflayer
  let oko = null;
  // mineflayer wysyla 'look' tylko przy ZMIANIE kata: pierwszy pakiet po teleporcie serwer gubil (przebieg 10: 3 kliki
  // z katem sprzed teleportu), a ponowny look w ten sam kat nic nie wysyla -> drgniecie o 3 st. i sprawdzenie Rotation na serwerze
  const patrzOd = async (x, y, z) => {
    const dx = x - oko[0], dy = y - oko[1], dz = z - oko[2];
    const yaw = Math.atan2(-dx, -dz), pitch = Math.atan2(dy, Math.hypot(dx, dz));
    const mcY = ((180 - yaw * 180 / Math.PI) % 360 + 360) % 360, mcP = -pitch * 180 / Math.PI;
    for (let k = 0; k < 4; k++) {
      await bot.look(yaw + 0.05, pitch, false); await sleep(120);
      await bot.look(yaw, pitch, false); await sleep(200);
      const m = (await rcon(`minecraft:data get entity ${nick} Rotation`)).match(/\[(-?[\d.]+)f, (-?[\d.]+)f\]/);
      if (!m) return;
      const dy2 = Math.abs((((+m[1] - mcY) % 360) + 540) % 360 - 180);
      if (dy2 < 1.5 && Math.abs(+m[2] - mcP) < 1.5) return;
    }
  };
  const celuj = async (x, y, z) => { if (oko) return patrzOd(x, y, z); await patrz(x, y, z); await sleep(150); await patrz(x, y, z); };   // duzy obrot po teleporcie dochodzi z opoznieniem
  const tp = (x, y, z, kto = nick) => { if (kto === nick) oko = null; return rcon(`execute in minecraft:overworld run minecraft:teleport ${kto} ${x} ${y} ${z}`); };
  const czekaj = async (re, ms = 8000) => { const k = Date.now() + ms; let s = ''; while (Date.now() < k) { s = await st(); if (re.test(s)) return s; await sleep(150); } return s; };
  const pasekOd = (n) => pasek.slice(n).join(' | ');
  const linia = (od) => czat.slice(od).filter(m => /\[Myśliwy\]/.test(m)).pop() || '';
  const noz = async () => {
    await rcon(`clear ${nick}`);
    await rcon(`rzemioslo narzedzie ${nick} noz 1`);
    const r = await rcon(`rzemioslo doreki ${nick} PROF_NOZ_1 5`);
    await sleep(400);
    if (!/doreki/.test(r)) throw Error('noz: ' + r);
  };
  let rx = 200;   // kazdy przypadek dostaje swoj rewir (rewir po uzyciu odpoczywa 20 s)
  const rewir = async (mob = 'BEAST_WILD_BOAR') => {
    rx += 12; if (rx > 290) rx = 212;
    const z = 120;
    const r = await rcon(`rzemioslo rewir world ${rx} ${Y} ${z} ${mob}`);
    if (!/->/.test(r)) throw Error('rewir: ' + r);
    return [rx + 0.5, Y, z + 0.5];
  };
  const kucnij = async () => { bot.setControlState('sneak', false); await sleep(200); bot.setControlState('sneak', true); await sleep(400); };
  // start przy rewirze i przejscie po tropach (teleport na kazdy trop = dojscie), konczy w fazie PODCHOD
  const tropem = async (mob) => {
    await czekaj(/^(?!.*faza=)/, 5000);   // poprzedni low musi sie skonczyc (ucieczka trwa 1,5 s)
    const [x, y, z] = await rewir(mob);
    bot.setControlState('sneak', false);
    await tp(x + 1, y, z); await sleep(600);
    await kucnij();
    let s = await czekaj(/faza=TROP/, 3000);
    if (!/faza=TROP/.test(s)) return { s, ok: false };
    await rcon(`rzemioslo mysliwy ${nick} wiatr 0`);   // powtarzalnosc: wiatr na +X (gracz wie z pasku i czasteczek)
    for (let i = 0; i < 8; i++) {
      s = await st();
      if (!/faza=TROP/.test(s)) break;
      const [tx, ty, tz] = xyz(pole(s, 'tropXYZ'));
      await tp(tx, ty, tz); await sleep(500);
    }
    s = await czekaj(/faza=PODCHOD/, 3000);
    return { s, ok: /faza=PODCHOD/.test(s) };
  };
  const zwierze = (s) => { const t = pole(s, 'zwierzeXYZ'); return t ? xyz(t) : null; };
  const encja = (nazwa, p) => bot.nearestEntity(e => e.name === nazwa && e.position.distanceTo(new Vec3(...p)) < 3);
  // ustawia gracza z wiatrem (zapach nie leci do zwierzecia): wiatr 0 = wieje na +X, gracz po stronie +X
  const podejdz = async (s, d = 3, wiatr = 0) => {
    await rcon(`rzemioslo mysliwy ${nick} wiatr ${wiatr}`);
    const [ax, ay, az] = zwierze(s);
    const r = wiatr * Math.PI / 180;
    bot.setControlState('sneak', true);
    await tp(ax + Math.cos(r) * d, ay, az + Math.sin(r) * d); await sleep(500);
    await patrz(ax, ay + 0.4, az);
    return [ax, ay, az];
  };
  const zabij = async (s, nazwa = 'pig') => {
    const p = await podejdz(s);
    const e = encja(nazwa, p);
    if (!e) return 'brak encji ' + nazwa;
    await bot.attack(e); await sleep(600);
    return await st();
  };
  // ciecie i-te: patrz na znak, PPM w zwloki (Interaction), trzymaj wzrok PROWADZENIE tickow
  const tnij = async (i, { odwroc = false } = {}) => {
    const s = await st();
    const c = (pole(s, 'ciecia') || '').split(';').filter(Boolean)[i];
    if (!c) return 'brak ciecia ' + i + ' w ' + s;
    const [px, py, pz] = xyz(c.split('@')[1]);
    await celuj(px, py, pz);
    const zl = xyz(pole(s, 'zwlokiXYZ'));
    const it = bot.nearestEntity(e => e.name === 'interaction' && e.position.distanceTo(new Vec3(...zl)) < 1.5);
    if (!it) return 'brak interaction';
    uzyj(it);
    await sleep(250);
    // pudlo (PPM obok znaku = tylko podpowiedz "Celuj w czerwony znak") -> jak gracz: popraw wzrok i kliknij jeszcze raz
    for (let k = 0; k < 2 && /aktywne=-1/.test(await st()) && !stanCiec(await st())[i]; k++) { await celuj(px, py, pz); uzyj(it); await sleep(250); }
    if (odwroc) { await patrz(px + 3, py + 2, pz + 3); await sleep(200); await patrz(px, py, pz); }
    await sleep(900);
    return await st();
  };
  const stanCiec = (s) => (pole(s, 'ciecia') || '').split(';').filter(Boolean).map(c => c[1] === '+');
  const doZwlok = async (s) => {
    const t = pole(s, 'zwlokiXYZ');
    if (!t) throw Error('brak zwlok: ' + s.slice(0, 120));
    const zl = xyz(t);
    bot.setControlState('sneak', false);
    await tp(zl[0] + 1.6, zl[1], zl[2]); await sleep(700);   // wyprostowany: oko 1,62 (serwer i bot licza to samo)
    oko = [zl[0] + 1.6, zl[1] + 1.62, zl[2]];
    return zl;
  };

  await rcon('minecraft:difficulty peaceful');
  await rcon(`rzemioslo flaga ${nick} mysliwy_wprawa 0`);
  await rcon('rzemioslo mysliwy pomiar on');

  // ---------- bez noza: kucniecie przy rewirze nic nie zaczyna, komunikat jak w silniku wezlow
  if (wybrane.includes('brak-noza')) {
    await rcon(`clear ${nick}`);
    const [x, y, z] = await rewir();
    await tp(x + 1, y, z); await sleep(500);
    const p0 = pasek.length;
    await kucnij();
    const s = await st();
    w.sprawdz('brak-noza', !/faza=/.test(s) && /noża|nóż/i.test(pasekOd(p0)), `${s.slice(0, 60)} | ${pasekOd(p0).slice(0, 160)}`);
  }
  await noz();

  // ---------- zgubiony trop: odejscie > 40 blokow od tropu
  if (wybrane.includes('zgubiony')) {
    const [x, y, z] = await rewir();
    bot.setControlState('sneak', false);
    await tp(x + 1, y, z); await sleep(500);
    await kucnij();
    const s = await czekaj(/faza=TROP/, 3000);
    const p0 = pasek.length;
    await tp(x + 60, y, z + 40); await sleep(800);
    const s2 = await st();
    w.sprawdz('trop-start', /faza=TROP/.test(s) && /tropXYZ=/.test(s), s.slice(0, 140));
    w.sprawdz('zgubiony', !/faza=/.test(s2) && /Zgubiłeś trop/.test(pasekOd(p0)), `${s2.slice(0, 50)} | ${pasekOd(p0).slice(0, 120)}`);
  }

  // ---------- splosznie: wiatr niesie zapach do zwierzecia (gracz pod wiatr zly)
  if (wybrane.includes('wiatr')) {
    const r = await tropem();
    w.sprawdz('tropy-dojscie', r.ok && !!zwierze(r.s), r.s.slice(0, 160));
    if (r.ok) {
      const p0 = pasek.length;
      const [ax, ay, az] = zwierze(r.s);
      await rcon(`rzemioslo mysliwy ${nick} wiatr 180`);   // wieje na -X; gracz po stronie +X = zapach leci na zwierze
      bot.setControlState('sneak', true);
      await tp(ax + 10, ay, az); await sleep(1800);
      const s = await st();
      w.sprawdz('zwietrzylo', !/faza=PODCHOD/.test(s) && /zwietrzyło/.test(pasekOd(p0)), `${s.slice(0, 60)} | ${pasekOd(p0).slice(0, 160)}`);
    }
  }

  // ---------- sploszenie: sprint w zasiegu sluchu (wiatr dobry)
  if (wybrane.includes('sprint')) {
    const r = await tropem();
    if (r.ok) {
      const p0 = pasek.length;
      await rcon(`rzemioslo mysliwy ${nick} wiatr 0`);
      const [ax, ay, az] = zwierze(r.s);
      bot.setControlState('sneak', false);
      await tp(ax + 18, ay, az); await sleep(400);
      await patrz(ax, ay, az);
      bot.setControlState('sprint', true); bot.setControlState('forward', true);
      await sleep(2500);
      bot.setControlState('forward', false); bot.setControlState('sprint', false);
      const s = await st();
      const ok = /Usłyszało/.test(pasekOd(p0));
      w.sprawdz('sprint-sploszenie', !/faza=PODCHOD/.test(s) && ok, `${s.slice(0, 60)} | ${pasekOd(p0).slice(0, 160)}`);
    } else w.pomin('sprint-sploszenie', 'brak podchodu: ' + r.s.slice(0, 80));
  }

  // ---------- sploszenie: wyprostowany za blisko (wiatr dobry)
  if (wybrane.includes('wzrok')) {
    const r = await tropem();
    if (r.ok) {
      const p0 = pasek.length;
      await rcon(`rzemioslo mysliwy ${nick} wiatr 0`);
      const [ax, ay, az] = zwierze(r.s);
      bot.setControlState('sneak', false); await sleep(200);
      await tp(ax + 4, ay, az); await sleep(900);
      const s = await st();
      w.sprawdz('zobaczylo', !/faza=PODCHOD/.test(s) && /Zobaczyło/.test(pasekOd(p0)), `${s.slice(0, 60)} | ${pasekOd(p0).slice(0, 160)}`);
    } else w.pomin('zobaczylo', 'brak podchodu: ' + r.s.slice(0, 80));
  }

  // ---------- ranne: cios bez kucania (nie z podchodu) = zwierze ucieka, brak zwlok
  if (wybrane.includes('ranne')) {
    const r = await tropem();
    if (r.ok) {
      const p = await podejdz(r.s, 3);
      bot.setControlState('sneak', false); await sleep(150);
      const e = encja('pig', p);
      const p0 = pasek.length;
      if (e) { await bot.attack(e); }
      await sleep(2200);
      const s = await st();
      w.sprawdz('ranne-ucieka', !!e && !/faza=/.test(s) && !/zwloki typ/.test(s) && /uciekło|Zobaczyło/.test(pasekOd(p0)), `${s.slice(0, 80)} | ${pasekOd(p0).slice(0, 160)}`);
    } else w.pomin('ranne-ucieka', 'brak podchodu');
  }

  // ---------- czysto: dzik, podchod z wiatrem, cios z kucania, ciecia po kolei = wyborna skora i dziczyzna
  let mamDziczyzne = null;
  if (wybrane.includes('czysto')) {
    const r = await tropem();
    if (r.ok) {
      const s = await zabij(r.s);
      w.sprawdz('cios-z-podchodu', /zwloki typ=PIG/.test(s) && /spalona=false/.test(s), s.slice(0, 160));
      await doZwlok(s);
      const c0 = czat.length;
      let last = '';
      for (let i = 0; i < 5; i++) last = await tnij(i);
      await sleep(600);
      const l = linia(c0);
      w.sprawdz('czysto-wyborne', /wyborne/.test(l) && /Dziczyzna/.test(l) && !/—/.test(l), l || last.slice(0, 200));
      if (/Dziczyzna/.test(l)) mamDziczyzne = /Dziczyzna §7\(§6wyborne|Dziczyzna \(wyborne/.test(l) || /wyborne/.test(l.split('Dziczyzna')[1] || '') ? 'PROF_DZICZYZNA_WYBORNE' : 'PROF_DZICZYZNA';
    } else w.pomin('czysto-wyborne', 'brak podchodu');
  }

  // ---------- zla kolejnosc: brzuch przed odbytem = tresc jelit na miesie (mieso slabsze, uwaga w linii)
  if (wybrane.includes('zla-kolejnosc')) {
    const r = await tropem();
    if (r.ok) {
      const s = await zabij(r.s);
      await doZwlok(s);
      const c0 = czat.length;
      for (const i of [1, 0, 2, 3, 4]) await tnij(i);
      await sleep(600);
      const l = linia(c0);
      w.sprawdz('zla-kolejnosc', /treść na mięsie/.test(l) && /Dziczyzna/.test(l) && !/Dziczyzna[^,]*wyborne/.test(l), l || s.slice(0, 120));
    } else w.pomin('zla-kolejnosc', 'brak podchodu');
  }

  // ---------- obrazenia w trakcie ciecia: scena przerwana, postep zostaje, dokonczenie po ponownym PPM
  if (wybrane.includes('obrazenia')) {
    await rcon('minecraft:difficulty easy');
    await rcon('minecraft:gamerule spawn_monsters false');
    const r = await tropem();
    if (r.ok) {
      const s = await zabij(r.s);
      await doZwlok(s);
      await tnij(0);
      // start ciecia 1 i cios w trakcie prowadzenia
      const s1 = await st();
      const c = (pole(s1, 'ciecia') || '').split(';').filter(Boolean)[1];
      const [px, py, pz] = xyz(c.split('@')[1]);
      await celuj(px, py, pz);
      const zl = xyz(pole(s1, 'zwlokiXYZ'));
      const it = bot.nearestEntity(e => e.name === 'interaction' && e.position.distanceTo(new Vec3(...zl)) < 1.5);
      const p0 = pasek.length;
      uzyj(it); await sleep(250);
      await rcon(`minecraft:damage ${nick} 2 minecraft:generic`);
      await sleep(400);
      const s2 = await st();
      w.sprawdz('obrazenia-przerwa', /scena=false/.test(s2) && /ciecia=0\+/.test(s2) && /1-@/.test(s2), `${s2.slice(0, 200)} | ${pasekOd(p0).slice(0, 100)}`);
      const c0 = czat.length;
      for (let i = 1; i < 5; i++) await tnij(i);
      await sleep(600);
      const l = linia(c0);
      w.sprawdz('obrazenia-dokonczone', /drgnęła ręka/.test(l) && /Dziczyzna/.test(l), l || 'brak linii');
    } else w.pomin('obrazenia-przerwa', 'brak podchodu');
    await rcon('minecraft:difficulty peaceful');
  }

  // ---------- spalona + zla kolejnosc = 0 skory i 0 miesa (proba moze dac 0)
  if (wybrane.includes('spalona-zero')) {
    const r = await tropem();
    if (r.ok) {
      const u = pole(r.s, 'zwierzeUUID');
      const [ax, , az] = zwierze(r.s);
      await rcon(`execute in minecraft:overworld run minecraft:setblock ${Math.floor(ax)} ${Y} ${Math.floor(az)} minecraft:fire`);   // zwierze wchodzi w ogien
      await sleep(700);
      void u;
      const s = await zabij(r.s);
      w.sprawdz('spalona-zwloki', /spalona=true/.test(s), s.slice(0, 160));
      await doZwlok(s);
      const c0 = czat.length;
      for (const i of [4, 3, 2, 1, 0]) await tnij(i);
      await sleep(600);
      const l = czat.slice(c0).filter(m => /\[Myśliwy\]/.test(m)).join(' / ');
      w.sprawdz('spalona-zero', /skóra spalona/.test(l) && /mięso zepsute/.test(l), l || 'brak linii');
    } else w.pomin('spalona-zero', 'brak podchodu');
  }

  // ---------- zajac: inna kolejnosc ciec (lapy -> skora -> patroszenie)
  if (wybrane.includes('zajac')) {
    const r = await tropem('BEAST_FOREST_HARE');
    if (r.ok) {
      const s = await zabij(r.s, 'rabbit');
      w.sprawdz('zajac-zwloki', /zwloki typ=RABBIT/.test(s) && /ciecia=0-@[^;]+;1-@[^;]+;2-@[^;]+; /.test(s), s.slice(0, 200));
      await doZwlok(s);
      const c0 = czat.length;
      for (let i = 0; i < 3; i++) await tnij(i);
      await sleep(600);
      const l = linia(c0);
      w.sprawdz('zajac-wynik', /Łup z bestii/.test(l) && /Dziczyzna/.test(l), l || 'brak linii');
    } else w.pomin('zajac-wynik', 'brak podchodu');
  }

  // ---------- wedzenie: dziczyzna na zapalonym ognisku -> suszona dziczyzna
  const wedz = async (x, z) => {
    const id = mamDziczyzne || 'PROF_DZICZYZNA';
    if (!mamDziczyzne) { await rcon(`mi give MATERIAL PROF_DZICZYZNA ${nick} 1`); await sleep(300); }   // przypadek niezalezny od lowu
    const r = await rcon(`rzemioslo doreki ${nick} ${id} 6`); mamDziczyzne = null;
    await sleep(300);
    await tp(x + 0.5, Y, z - 1.5); await sleep(400);
    await patrz(x + 0.5, Y + 0.4, z + 0.5);
    const b = bot.blockAt(new Vec3(x, Y, z));
    if (b) await bot.activateBlock(b).catch(() => {});
    await sleep(500);
    return r;
  };
  if (wybrane.includes('wedzenie')) {
    const r = await wedz(200, 190);
    const s = await st();
    w.sprawdz('wedzenie-start', /wedzi@200,-59,190/.test(s), `${r} | ${s.slice(-120)}`);
    await sleep(11000);   // >= 2 probki co 5 s przy zapalonym ogniu
    await rcon(`rzemioslo mysliwy ${nick} wedzenie-gotowe`);
    const c0 = czat.length;
    const b = bot.blockAt(new Vec3(200, Y, 190));
    await bot.activateBlock(b).catch(() => {});
    await sleep(600);
    const l = linia(c0);
    w.sprawdz('wedzenie-suszona', /Suszona dziczyzna/.test(l), l || (await st()).slice(-120));
  }
  if (wybrane.includes('zgaszone')) {
    const p0 = pasek.length;
    await wedz(208, 190);   // zgaszone od poczatku: odmowa
    w.sprawdz('zgaszone-odmowa', /zgaszone/.test(pasekOd(p0)) && !/wedzi@208/.test(await st()), pasekOd(p0).slice(0, 120));
    await wedz(204, 190);
    await rcon('execute in minecraft:overworld run minecraft:setblock 204 -59 190 minecraft:campfire[lit=false]');
    await sleep(11000);
    await rcon(`rzemioslo mysliwy ${nick} wedzenie-gotowe`);
    const c0 = czat.length;
    await bot.activateBlock(bot.blockAt(new Vec3(204, Y, 190))).catch(() => {});
    await sleep(600);
    const l = linia(c0);
    w.sprawdz('zgaszone-zepsute', /nie doschło/.test(l), l || (await st()).slice(-120));
    await rcon('execute in minecraft:overworld run minecraft:setblock 204 -59 190 minecraft:campfire[lit=true]');
  }
  const wp = await rcon(`rzemioslo flaga ${nick} mysliwy_wprawa`);
  console.log('wprawa:', wp, '| czat:', czat.filter(m => /Myśliwy/.test(m)).join(' / ').slice(0, 1500));
  await rcon('rzemioslo mysliwy pomiar off');
  bot.quit();
}, { limitMs: Number(process.env.LIMIT_MS || 1200000) });
