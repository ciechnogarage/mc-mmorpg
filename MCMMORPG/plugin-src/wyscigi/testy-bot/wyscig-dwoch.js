'use strict';
// Wyscig dwoch graczy: wspolna poczekalnia (drugi dolacza do pierwszego), ogloszenie dla innych, odliczanie,
// pozycje w trakcie, wolniejszy jedzie z przerwami w gazie; wyniki 1./2. trafiaja do obu.
const { scenariusz } = require('/bot/lib');
const { narzedzia, autopilot, sleep, czekaj } = require('./wyscigi-lib');

scenariusz('wyscigi-dwoch', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const [nA, nB, nW] = ['WyscigA', 'WyscigB', 'Widz'];
  const a = await polacz({ nick: nA }), b = await polacz({ nick: nB }), widz = await polacz({ nick: nW });
  await sleep(2500);
  const czatA = [], czatB = [], czatW = [];
  a.on('message', m => czatA.push(m.toString())); b.on('message', m => czatB.push(m.toString())); widz.on('message', m => czatW.push(m.toString()));

  const odpA = await rcon(`wyscigi wyscig ${nA} pozogar`);
  w.sprawdz('A-start', /OK/.test(odpA), odpA);
  await sleep(1500);
  w.sprawdz('ogloszenie-dla-innych', czatW.some(c => /czeka na wyścig/.test(c)) && !czatA.some(c => /czeka na wyścig/.test(c)), czatW.slice(-1)[0]);
  w.sprawdz('B-dolacza', /OK/.test(await rcon(`wyscigi wyscig ${nB} glazogrzbiet`)), '');
  const stan = await rcon('wyscigi stan');
  w.sprawdz('jeden-wyscig-dwoch', /WYSCIG POCZEKALNIA 2/.test(stan) && (stan.match(/ - /g) || []).length === 1, stan.replace(/\n/g, ' / ').slice(0, 200));
  w.sprawdz('A-widzi-dolaczenie', czatA.some(c => /WyscigB.*dołącza/.test(c)), czatA.filter(c => /dołącza/.test(c))[0]);
  const pA = await t.poz(nA), pB = await t.poz(nB);
  w.sprawdz('rozne-miejsca-startowe', pA && pB && Math.hypot(pA.x - pB.x, pA.z - pB.z) > 2, `${JSON.stringify(pA)} ${JSON.stringify(pB)}`);
  w.sprawdz('stoja-w-poczekalni', pA && pB && pA.v === 0 && pB.v === 0, '');

  const ruszyli = await czekaj(async () => { const p = await t.poz(nA); return p && p.faza === 'JAZDA' ? p : null; }, 45000, 500);
  w.sprawdz('start-po-poczekalni', !!ruszyli, JSON.stringify(ruszyli));
  const tor = await t.tor();
  let pozycjeWidziane = new Set();
  const jazdaA = autopilot(a, t, nA, tor, { poslizg: true, stop: p => { pozycjeWidziane.add(p.pozycja); return p.meta >= 0; } });
  const jazdaB = autopilot(b, t, nB, tor, { gaz: () => (Date.now() / 1000) % 4 > 1.2, stop: p => p.meta >= 0 || p.faza === 'KONIEC' });
  const [rA, rB] = await Promise.all([jazdaA, jazdaB]);
  w.sprawdz('A-meta', rA.koniec === 'stop', JSON.stringify(rA.ost));
  w.sprawdz('pozycja-liczona', pozycjeWidziane.size >= 1 && [...pozycjeWidziane].every(x => x === 1 || x === 2), JSON.stringify([...pozycjeWidziane]));
  const evA = await t.zdarzenia(nA), evB = await t.zdarzenia(nB);
  w.sprawdz('A-pierwsze-miejsce', evA.some(e => / meta .* miejsce 1/.test(e)), evA.filter(e => /meta/.test(e)).join(''));
  w.sprawdz('B-drugie-albo-czas', evB.some(e => / meta .* miejsce 2/.test(e)) || czatB.some(c => /nie ukończył/.test(c)), evB.filter(e => /meta/.test(e)).join('') || 'brak mety B');
  w.sprawdz('B-dostal-komunikat-o-zwyciezcy', czatB.some(c => /WyscigA.*wygrywa/.test(c)), czatB.filter(c => /wygrywa/.test(c))[0]);
  await czekaj(async () => czatA.some(c => /wyniki/.test(c)) && czatB.some(c => /wyniki/.test(c)), 45000, 500);
  w.sprawdz('wyniki-u-obu', czatA.some(c => /wyniki/.test(c)) && czatB.some(c => /wyniki/.test(c)), czatA.filter(c => /^\d\. |—/.test(c)).join(' / '));
  const wrocili = await czekaj(async () => !(await t.wymiar(nA)).includes('wyscigi') && !(await t.wymiar(nB)).includes('wyscigi'), 15000, 500);
  w.sprawdz('obaj-wrocili', !!wrocili, `${await t.wymiar(nA)} ${await t.wymiar(nB)}`);
  w.sprawdz('bez-wierzchowcow', (await t.wilki()) === 0, `wilki ${await t.wilki()}`);
  w.sprawdz('stan-pusty', /Wyścigi: 0, kierowcy: 0/.test(await rcon('wyscigi stan')), '');
}, { limitMs: 420000 });
