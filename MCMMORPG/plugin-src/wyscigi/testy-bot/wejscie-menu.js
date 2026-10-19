'use strict';
// Gracz bez OP: Q -> Spolecznosc -> Wyscigi -> Czasowka -> Wilkor; siedzi na wierzchowcu w swiecie wyscigow,
// widzi BossBar; potem przytrzymuje Shift na postoju i wraca tam, skad przyszedl, bez wierzchowca.
const { scenariusz } = require('/bot/lib');
const { narzedzia, wejscie, otworzHub, kliknijIOtworz, tytul, sleep, czekaj } = require('./wyscigi-lib');

scenariusz('wyscigi-wejscie-menu', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const nick = 'WyscigMenu';
  const bot = await polacz({ nick });
  await sleep(2500);
  const przed = await t.pozGracza(nick);
  const wymiarPrzed = await t.wymiar(nick);
  w.sprawdz('start-poza-torem', !wymiarPrzed.includes('wyscigi'), `wymiar ${wymiarPrzed}, poz ${JSON.stringify(przed)}`);

  const paski = [];
  bot.on('bossBarCreated', b => paski.push(b)); bot.on('bossBarUpdated', b => paski.push(b));
  const czat = [];
  bot.on('message', m => czat.push(m.toString()));

  const { okno: hub, droga } = await otworzHub(bot);
  w.sprawdz('hub-otwarty', !!hub, `droga ${droga}, sloty ${hub?.slots?.length}`);
  if (!hub) return;
  const spol = await kliknijIOtworz(bot, 38);
  w.sprawdz('spolecznosc-kafel', !!spol && !!spol.slots[43], `kafel 43: ${spol?.slots?.[43]?.name} ${JSON.stringify(spol?.slots?.[43]?.customName ?? '')}`);
  const menu = await kliknijIOtworz(bot, 43);
  w.sprawdz('menu-wyscigow', /Wyścigi Chowańców/.test(tytul(menu)), `tytul "${tytul(menu)}"`);
  const wybor = await kliknijIOtworz(bot, 10);
  w.sprawdz('wybor-wierzchowca', /Czasówka/.test(tytul(wybor)), `tytul "${tytul(wybor)}"`);
  await bot.clickWindow(10, 0, 0);

  const siedzi = await czekaj(async () => { const p = await t.poz(nick); return p && p.kierowca ? p : null; }, 8000, 300);
  w.sprawdz('dosiadl', !!siedzi, JSON.stringify(siedzi || await t.poz(nick)));
  w.sprawdz('swiat-wyscigow', (await t.wymiar(nick)).includes('wyscigi'), await t.wymiar(nick));
  w.sprawdz('wierzchowiec-1', (await t.wilki()) === 1, `wilki ${await t.wilki()}`);
  await sleep(1500);
  const pasek = paski.map(b => plainBar(b)).find(s => /Czasówka|Okrążenie|Gotowi|Tor/.test(s));
  w.sprawdz('bossbar', !!pasek, `paski: ${JSON.stringify([...new Set(paski.map(plainBar))].slice(0, 4))}`);
  w.sprawdz('instrukcja-w-czacie', czat.some(c => /Shift w zakręcie/.test(c)), czat.slice(-3).join(' / '));

  // Wyjscie: Shift na postoju (odliczanie trzyma wierzchowca w miejscu)
  bot.physicsEnabled = false;
  const t0 = Date.now();
  while (Date.now() - t0 < 3500) { wejscie(bot, { shift: true }); await sleep(100); }
  wejscie(bot, {});
  const wrocil = await czekaj(async () => !(await t.wymiar(nick)).includes('wyscigi'), 5000, 300);
  const po = await t.pozGracza(nick);
  w.sprawdz('wyjscie-shift', !!wrocil, `wymiar ${await t.wymiar(nick)}`);
  w.sprawdz('powrot-w-to-samo-miejsce', przed && po && Math.hypot(po.x - przed.x, po.z - przed.z) < 1.5, `przed ${JSON.stringify(przed)} po ${JSON.stringify(po)}`);
  w.sprawdz('bez-wierzchowca', (await t.wilki()) === 0, `wilki ${await t.wilki()}`);
  w.sprawdz('nie-kierowca', (await t.poz(nick)) === null, 'poz BRAK');
}, { limitMs: 90000 });

function plainBar(b) {
  try { const tt = b.title; return typeof tt === 'string' ? tt : (tt?.toString?.() ?? JSON.stringify(tt)); } catch { return ''; }
}
