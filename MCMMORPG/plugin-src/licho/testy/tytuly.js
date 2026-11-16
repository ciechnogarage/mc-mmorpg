'use strict';
// Licho nie śpi — tytuły za progi jak gracze (konta bez OP, wszystko z menu Q):
//   tytuly.min-graczy > 2 (domyślnie 4): runda we dwóch -> „nie liczy się do tytułów”, brak tytułu, menu: „jeszcze żaden”;
//   tytuly.min-graczy = 2 (tylko na klonie): Licho przetrwa (Zamawiacz stoi) -> „Nowy tytuł: «Kryjak»” + ogłoszenie,
//   w podsumowaniu kolejnej rundy «Kryjak» przy nicku, w menu Licha (przycisk Rekordy) tytuł i postęp.
// RCON tylko do obserwacji (`licho stan`, `licho rekordy`).
//   run.sh licho <ścieżka>/testy/tytuly.js
const { scenariusz, rcon, czekaj, sleep, stan, postac, otworzHub, kliknijSlot, obserwuj } = require('/bot/lib');

const stanRundy = async () => (await rcon('licho stan')).replace(/\s+/g, ' ');
const faza = s => (s.match(/faza=(\w+)/) || [])[1] || 'BRAK';
const rola = (s, nick) => (s.match(new RegExp(`\\| ${nick} (\\w+)`)) || [])[1];

/** Q -> Drużyna -> LICHO; zostawia otwarte menu Licha (DOŁĄCZ = slot 5, Rekordy = 25). */
async function menuLicha(bot) {
  await czekaj(async () => !/creator_limbo/.test(await stan.wymiar(bot.username)) || null, 15000, 500);
  await sleep(1500);
  let hub = null;
  for (let i = 0; i < 3 && !hub; i++) hub = await otworzHub(bot).catch(async () => { await sleep(1500); return null; });
  // Q po kreatorze INNEGO gracza bywa martwe u bota (znalezisko poza Lichem, runda.js) — ta sama komenda co Zaklecia na Q
  if (!hub) { await rcon(`dm open hub_glowne ${bot.username}`); await czekaj(() => bot.currentWindow, 4000, 100); }
  await kliknijSlot(bot, 38);
  return kliknijSlot(bot, 25);
}
const lore = (okno, slot) => JSON.stringify(okno?.slots?.[slot]?.components ?? okno?.slots?.[slot]?.nbt ?? '');
const dolacz = bot => kliknijSlot(bot, 5, { noweOkno: false }).catch(() => null);

scenariusz('licho-tytuly', async ({ w, polacz }) => {
  const min = +((await stanRundy()).match(/tytuly-min=(\d+)/) || [])[1];
  w.sprawdz('stan-progi', min > 0, await stanRundy());
  const liczy = min <= 2;
  const sfx = Date.now() % 10000;
  const boty = [], ekrany = {};
  for (const k of ['A', 'B']) {
    const b = await polacz({ nick: `Ti${k}${sfx}` }); ekrany[b.username] = obserwuj(b);
    const r = await postac.wejdz(b, { imie: `Tyt${k.toLowerCase()}${sfx}` });
    w.sprawdz(`${k}-postac`, !!r, `droga ${r && r.droga}`);
    boty.push(b); await sleep(6000);
  }
  const rundy = liczy ? 2 : 1;
  let L1 = null;
  for (let n = 1; n <= rundy; n++) {
    await czekaj(async () => faza(await stanRundy()) === 'BRAK' || null, 30000, 1000);
    for (const b of boty) { await menuLicha(b); await dolacz(b); await sleep(500); }
    const s = await czekaj(async () => { const x = await stanRundy(); return faza(x) === 'ROZPROSZENIE' ? x : null; }, 70000, 1000);
    w.sprawdz(`r${n}-start`, !!s, s || await stanRundy());
    if (!s) return;
    const L = boty.find(b => rola(s, b.username) === 'LICHO');
    if (n === 1) L1 = L;
    const k = await czekaj(async () => faza(await stanRundy()) === 'KONIEC' || null, 400000, 1000);
    w.sprawdz(`r${n}-koniec`, !!k, await stanRundy());
    await sleep(1500);
    const czat = ekrany[L.username].czat.slice(-14).join(' | ');
    if (!liczy) {
      w.sprawdz('nie-liczy-komunikat', /nie liczy się do tytułów/.test(czat), czat.slice(-300));
      w.sprawdz('nie-liczy-bez-tytulu', !/Nowy tytuł/.test(czat), '');
    } else if (n === 1) {
      w.sprawdz('kryjak-komunikat', /Nowy tytuł: «Kryjak»/.test(czat), czat.slice(-300));
      const czatZ = ekrany[boty.find(b => b !== L).username].czat.slice(-14).join(' | ');
      w.sprawdz('kryjak-ogloszenie', new RegExp(`${L.username} zdobywa tytuł «Kryjak»`).test(czatZ), czatZ.slice(-300));
    } else {
      const czat1 = ekrany[L1.username].czat.slice(-16).join(' | ');
      w.sprawdz('podsumowanie-z-tytulem', new RegExp(`${L1.username} «Kryjak»`).test(czat1), czat1.slice(-400));
    }
  }
  // po powrocie: menu Licha -> lore przycisku Rekordy
  await czekaj(async () => (await stan.wymiar(boty[0].username)) !== 'minecraft:licho' || null, 30000, 1000);
  const m = await menuLicha(L1);
  const l = lore(m, 25);
  w.sprawdz('menu-tytul', liczy ? /Kryjak/.test(l) : /jeszcze żaden/.test(l), l.slice(0, 400));
  w.sprawdz('menu-postep', (liczy ? /Domowik \([12]\/5\)/.test(l) : /Kryjak \(0\/1\)/.test(l)) && new RegExp(`od ${min} graczy`).test(l), '');
  w.sprawdz('rekordy-konsola', true, (await rcon('licho rekordy')).replace(/\s+/g, ' ').slice(0, 300));
}, { limitMs: 900000 });
