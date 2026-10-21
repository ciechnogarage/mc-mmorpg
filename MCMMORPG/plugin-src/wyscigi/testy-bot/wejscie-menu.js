'use strict';
// Gracz bez OP: Q -> DRUZYNA -> kafel WYSCIGI -> Czasowka (kafel NA CZAS) -> Wilkor; siedzi na wierzchowcu, widzi pasek;
// Q w wyscigu otwiera pauze (nie hub), "Opusc" wraca tam, skad przyszedl; F nie zamienia broni.
const { scenariusz } = require('/bot/lib');
const { narzedzia, otworzHub, kliknijIOtworz, nazwaSlotu, klawiszQ, klawiszF, sleep, czekaj } = require('./wyscigi-lib');

scenariusz('wyscigi-wejscie-menu', async ({ w, polacz, rcon }) => {
  const t = narzedzia(rcon);
  const nick = 'WyscigMenu';
  const bot = await polacz({ nick });
  await sleep(2500);
  const przed = await t.pozGracza(nick);
  w.sprawdz('start-poza-torem', !(await t.wymiar(nick)).includes('wyscigi'), JSON.stringify(przed));
  const paski = []; bot.on('bossBarCreated', b => paski.push(String(b.title ?? '')));
  const czat = []; bot.on('message', m => czat.push(m.toString()));

  const { okno: hub, droga } = await otworzHub(bot);
  w.sprawdz('hub-otwarty', !!hub, `droga ${droga}`);
  if (!hub) return;
  const druzyna = await kliknijIOtworz(bot, 38);
  w.sprawdz('druzyna-kafel-wyscigi', /Wyścigi Chowańców/.test(nazwaSlotu(druzyna, 43)), nazwaSlotu(druzyna, 43).slice(0, 120));
  w.sprawdz('druzyna-opis', /Drużyna/.test(nazwaSlotu(druzyna, 0)) && !/Społeczność/.test(nazwaSlotu(druzyna, 0)), nazwaSlotu(druzyna, 0).slice(0, 80));
  const menu = await kliknijIOtworz(bot, 43);
  w.sprawdz('menu-wyscigow', /Czasówka/.test(nazwaSlotu(menu, 5)) && /Wyścig/.test(nazwaSlotu(menu, 7)), nazwaSlotu(menu, 5).slice(0, 80));
  const wybor = await kliknijIOtworz(bot, 5);
  w.sprawdz('wybor-wierzchowca', /Wilkor/.test(nazwaSlotu(wybor, 1)) && /Żelaznoróg/.test(nazwaSlotu(wybor, 41)), nazwaSlotu(wybor, 1).slice(0, 80));
  await bot.clickWindow(1, 0, 0);

  const siedzi = await czekaj(async () => { const p = await t.poz(nick); return p && p.kierowca ? p : null; }, 8000, 300);
  w.sprawdz('dosiadl', !!siedzi, JSON.stringify(siedzi));
  w.sprawdz('wierzchowiec-1', (await t.wilki()) === 1, `wilki ${await t.wilki()}`);
  w.sprawdz('strzalki-pol', (await t.strzalki()) >= 27, `strzalki ${await t.strzalki()}`);
  w.sprawdz('czasowka-bez-skrzynek', (await t.skrzynki()) === 0, `skrzynki ${await t.skrzynki()}`);
  await sleep(1500);
  w.sprawdz('bossbar', paski.length > 0, JSON.stringify(paski.slice(0, 2)).slice(0, 120));
  w.sprawdz('instrukcja-A/D', czat.some(c => /A\/D/.test(c)), czat.filter(c => /Wyścigi/.test(c)).slice(-1)[0]);

  // F na siodle: zadnej zamiany rak (Ksiega), tylko komunikat o braku bonusu
  const lewaPrzed = JSON.stringify(bot.inventory.slots[45] ?? null), prawaPrzed = JSON.stringify(bot.heldItem ?? null);
  klawiszF(bot); await sleep(800);
  w.sprawdz('F-bez-zamiany-rak', JSON.stringify(bot.inventory.slots[45] ?? null) === lewaPrzed && JSON.stringify(bot.heldItem ?? null) === prawaPrzed, '');

  // Q = pauza (nie hub)
  const pauzaOkno = new Promise(res => bot.once('windowOpen', res));
  klawiszQ(bot);
  const pauza = await Promise.race([pauzaOkno, sleep(3000).then(() => null)]);
  w.sprawdz('Q-pauza', !!pauza && /Opuść wyścig/.test(nazwaSlotu(pauza, 24)), nazwaSlotu(pauza, 24).slice(0, 80) || 'brak okna');
  if (pauza) await bot.clickWindow(24, 0, 0);
  const wrocil = await czekaj(async () => !(await t.wymiar(nick)).includes('wyscigi'), 6000, 300);
  const po = await t.pozGracza(nick);
  w.sprawdz('opusc-z-pauzy', !!wrocil, await t.wymiar(nick));
  w.sprawdz('powrot-w-to-samo-miejsce', przed && po && Math.hypot(po.x - przed.x, po.z - przed.z) < 1.5, `przed ${JSON.stringify(przed)} po ${JSON.stringify(po)}`);
  w.sprawdz('bez-wierzchowca', (await t.wilki()) === 0, `wilki ${await t.wilki()}`);
  await sleep(1500);
  w.sprawdz('dekoracje-zdjete', (await t.strzalki()) === 0, `strzalki ${await t.strzalki()}`);
}, { limitMs: 90000 });
