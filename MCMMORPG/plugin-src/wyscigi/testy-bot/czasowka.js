'use strict';
// Czasowka: 3 okrazenia autopilotem (gaz + poslizg w zakretach), meta, rekord, powrot, sprzatniecie.
// Wejscie ta sama komenda konsoli, ktora wykonuje klik w menu (sciezke menu sprawdza wejscie-menu.js).
const { scenariusz } = require('/bot/lib');
const { narzedzia, autopilot, sleep, czekaj } = require('./wyscigi-lib');

const GATUNEK = process.argv[2] || 'wilkor';
const POSLIZG = process.argv[3] !== 'bez-poslizgu';

scenariusz('wyscigi-czasowka-' + GATUNEK, async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const nick = 'Czas' + GATUNEK.slice(0, 8);
  const bot = await polacz({ nick });
  await sleep(2500);
  const przed = await t.pozGracza(nick);
  const tytuly = [], czat = [];
  bot.on('title', (tx) => tytuly.push(String(tx)));
  bot._client.on('set_title_subtitle', p => tytuly.push(JSON.stringify(p.text)));
  bot._client.on('set_title_text', p => tytuly.push(JSON.stringify(p.text)));
  bot.on('message', m => czat.push(m.toString()));
  let paskiUsuniete = 0;
  bot.on('bossBarDeleted', () => paskiUsuniete++);

  const odp = await rcon(`wyscigi czasowka ${nick} ${GATUNEK}`);
  w.sprawdz('start', /OK/.test(odp), odp);
  const tor = await t.tor();
  w.sprawdz('tor', tor.pkt.length > 50, `dlugosc ${tor.dlugosc}, punktow ${tor.pkt.length}`);

  const ruszyl = await czekaj(async () => { const p = await t.poz(nick); return p && p.faza === 'JAZDA' ? p : null; }, 15000, 250);
  w.sprawdz('odliczanie-i-start', !!ruszyl, JSON.stringify(ruszyl));
  const t0 = Date.now();
  let maxV = 0, pobocze = 0, probki = 0;
  const wynik = await autopilot(bot, t, nick, tor, {
    poslizg: POSLIZG, limitMs: 300000,
    stop: p => { maxV = Math.max(maxV, p.v); probki++; if (p.pobocze) pobocze++; return p.meta >= 0; },
  });
  const czasJazdy = (Date.now() - t0) / 1000;
  w.sprawdz('meta', wynik.koniec === 'stop', `koniec ${wynik.koniec} po ${czasJazdy.toFixed(1)} s, ost ${JSON.stringify(wynik.ost)}`);
  const ev = await t.zdarzenia(nick);
  const okr = ev.filter(e => / okrazenie \d/.test(e));
  w.sprawdz('trzy-okrazenia', okr.length === 3, okr.join(' | '));
  const czasy = okr.map(e => { const m = e.match(/(\d+):(\d+)\.(\d+)$/); return m ? +m[1] * 60 + +m[2] + +m[3] / 1000 : 0; });
  w.sprawdz('czasy-okrazen-sensowne', czasy.every(c => c > 15 && c < 90), JSON.stringify(czasy));
  const meta = ev.find(e => / meta /.test(e));
  w.sprawdz('meta-zdarzenie', !!meta, meta);
  w.sprawdz('predkosc-max', maxV > 0.45, `max v ${maxV.toFixed(3)} bl/tick (${(maxV * 20).toFixed(1)} bl/s)`);
  w.sprawdz('autopilot-na-torze', pobocze / Math.max(1, probki) < 0.25, `pobocze ${pobocze}/${probki}`);
  if (POSLIZG) {
    const posl = ev.filter(e => / poslizg /.test(e));
    w.sprawdz('poslizg-wchodzi', posl.length > 0, `${posl.length} wejsc`);
    const turbo = ev.filter(e => / turbo[12] /.test(e));
    w.sprawdz('poslizg-daje-turbo', turbo.length > 0, `${turbo.length}: ${turbo.slice(0, 3).join(' | ')}`);
  }
  const pola = ev.filter(e => / pole /.test(e));
  w.sprawdz('pola-przyspieszenia', pola.length > 0, `${pola.length} najazdow`);
  w.sprawdz('powrot-na-tor-zero', !ev.some(e => /powrot-na-tor/.test(e)), ev.filter(e => /powrot/.test(e)).join(' | ') || 'brak');

  await sleep(2500);
  w.sprawdz('tytul-meta', tytuly.some(s => /META/.test(s)), tytuly.slice(-4).join(' / '));
  w.sprawdz('czat-czas', czat.some(c => /Czas: \d+:\d\d\.\d{3}/.test(c)), czat.filter(c => /Wyścigi/.test(c)).slice(-3).join(' / '));
  const rek = await rcon('wyscigi rekordy');
  w.sprawdz('rekord-zapisany', rek.includes(nick), rek.split('\n').slice(0, 3).join(' / '));
  const wrocil = await czekaj(async () => !(await t.wymiar(nick)).includes('wyscigi'), 12000, 500);
  const po = await t.pozGracza(nick);
  w.sprawdz('powrot-po-mecie', !!wrocil && przed && po && Math.hypot(po.x - przed.x, po.z - przed.z) < 1.5, `przed ${JSON.stringify(przed)} po ${JSON.stringify(po)}`);
  w.sprawdz('bez-wierzchowca', (await t.wilki()) === 0, `wilki ${await t.wilki()}`);
  // MythicHUD przepisuje pakiety paskow (bot nie widzi "remove"); znikniecie paska sprawdza klient QA (zrzut w2.png 20.10).
  w.pomin('bossbar-zdjety', `bot: usuniete ${paskiUsuniete}; weryfikacja klientem QA`);
  w.sprawdz('rekordy-w-czacie', czat.some(c => /najlepsze przejazdy/.test(c)), '');
}, { limitMs: 380000 });
