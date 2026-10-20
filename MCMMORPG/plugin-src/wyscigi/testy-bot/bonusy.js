'use strict';
// Wyscig z innymi: skrzynki na torze, zbieranie, F = uzycie bonusu (bez zamiany rak), LPM = cios w sasiada,
// trafienie zwalnia; po wyscigu skrzynki i strzalki znikaja.
const { scenariusz } = require('/bot/lib');
const { narzedzia, autopilot, klawiszF, lpm, sleep, czekaj } = require('./wyscigi-lib');

scenariusz('wyscigi-bonusy', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const [nA, nB] = ['BonusA', 'BonusB'];
  const a = await polacz({ nick: nA }), b = await polacz({ nick: nB });
  await sleep(2500);
  w.sprawdz('start', /OK/.test(await rcon(`wyscigi wyscig ${nA} wilkor`)) && /OK/.test(await rcon(`wyscigi wyscig ${nB} pozogar`)), '');
  const ruszyli = await czekaj(async () => (await t.poz(nA))?.faza === 'JAZDA', 45000, 400);
  w.sprawdz('jazda', !!ruszyli, '');
  w.sprawdz('skrzynki-na-torze', (await t.skrzynki()) === 12, `skrzynki ${await t.skrzynki()}`);
  const tor = await t.tor();
  // B macha LPM, gdy jest blisko A (pierwsze 8 s), oba co sekunde probuja F
  let stop = false;
  const machanie = (async () => { const t0 = Date.now(); while (!stop && Date.now() - t0 < 8000) { lpm(b); await sleep(250); } })();
  const fki = (async () => { while (!stop) { klawiszF(a); klawiszF(b); await sleep(1000); } })();
  const jA = autopilot(a, t, nA, tor, { poslizg: true, stop: p => p.meta >= 0 || p.postep > 2 * tor.dlugosc || p.faza === 'KONIEC' });
  const jB = autopilot(b, t, nB, tor, { poslizg: true, przod: 3, stop: p => p.meta >= 0 || p.postep > 2 * tor.dlugosc || p.faza === 'KONIEC' });
  await Promise.all([jA, jB]); stop = true; await machanie; await fki;
  const ev = [...await t.zdarzenia(nA), ...await t.zdarzenia(nB)];
  const zebrane = ev.filter(e => /bonus-zebrany/.test(e)), uzyte = ev.filter(e => /bonus-uzyty/.test(e));
  w.sprawdz('bonus-zebrany', zebrane.length >= 2, zebrane.slice(0, 4).join(' | '));
  w.sprawdz('bonus-uzyty-F', uzyte.length >= 1, uzyte.slice(0, 4).join(' | '));
  const trafienia = ev.filter(e => /zachwianie|tarcza-zatrzymala/.test(e));
  w.sprawdz('cios-lub-bonus-trafia', trafienia.length >= 1, trafienia.slice(0, 4).join(' | '));
  w.sprawdz('cios-LPM', ev.some(e => /zachwianie cios:BonusB|tarcza-zatrzymala cios/.test(e)), ev.filter(e => /cios/.test(e)).join(' | ') || 'brak');
  await rcon(`wyscigi wyjdz ${nA}`); await rcon(`wyscigi wyjdz ${nB}`);
  await sleep(2500);
  w.sprawdz('po-wyscigu-czysto', (await t.skrzynki()) === 0 && (await t.strzalki()) === 0 && (await t.wilki()) === 0,
    `skrzynki ${await t.skrzynki()} strzalki ${await t.strzalki()} wilki ${await t.wilki()}`);
}, { limitMs: 300000 });
