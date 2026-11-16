'use strict';
// Licho — preferencja roli (Q w zagrodzie -> pauza -> LICHEM 23 / ŁOWIĘ 25 / LOSUJ 41) i podpowiedź PPM Zamawiacza.
// Dwa konta bez OP. Runda 1: A=ŁOWIĘ, B=LICHEM -> A Zamawiacz, B Licho (przy 2 graczach 1 Zamawiacz, więc bez losu).
// Zamawiacz w łowach PPM w zaczepę sprzętu -> pasek „wcielać się może tylko Licho”. Obaj wychodzą z pauzy (OPUŚĆ),
// runda 2 z odwrotnymi preferencjami -> role odwrotne. Na koniec LOSUJ czyści wybór. RCON tylko do obserwacji.
//   /opt/docker/mc/narzedzia/bot/run.sh <klon> $PWD/testy/rola.js
const { scenariusz, rcon, czekaj, sleep, stan, postac, otworzHub, kliknijSlot, obserwuj, ruchGracza, idz } = require('/bot/lib');

const SNOPEK = { x: -6, y: 64, z: 19 };
const stanRundy = async () => (await rcon('licho stan')).replace(/\s+/g, ' ');
const faza = s => (s.match(/faza=(\w+)/) || [])[1] || 'BRAK';
const rola = (s, nick) => (s.match(new RegExp(`\\| ${nick} (\\w+) iskry=`)) || [])[1];
const tekst = k => k ? JSON.stringify(k.components || k.nbt || k) : '';

async function menuLicha(bot) {
  await czekaj(async () => !/creator_limbo/.test(await stan.wymiar(bot.username)) || null, 15000, 500);
  let hub = null;
  for (let i = 0; i < 3 && !hub; i++) hub = await otworzHub(bot).catch(async () => { await sleep(1500); return null; });
  if (!hub) { await rcon(`dm open hub_glowne ${bot.username}`); await czekaj(() => bot.currentWindow, 4000, 100); }
  await kliknijSlot(bot, 38);
  return kliknijSlot(bot, 25);
}

async function wybierz(w, bot, slot, wzor, tag) {
  const pauza = await otworzHub(bot).catch(() => null);
  w.sprawdz(`${tag}-pauza`, pauza && !!pauza.slots[slot], `Q w zagrodzie: slot ${slot} ${tekst(pauza && pauza.slots[slot]).slice(0, 60)}`);
  if (!pauza) return;
  await kliknijSlot(bot, slot, { noweOkno: false });
  await sleep(800);
  const odp = await rcon(`papi parse ${bot.username} %licho_chce%`);
  w.sprawdz(`${tag}-wybor`, wzor.test(odp), `placeholder: ${odp.trim()}`);
}

scenariusz('licho-rola', async ({ w, polacz }) => {
  const sfx = Date.now() % 10000;
  const wejdz = async (nick, imie) => {
    const b = await polacz({ nick }); const e = obserwuj(b);
    const r = await postac.wejdz(b, { imie });
    w.sprawdz(`${nick}-postac`, !!r, `droga ${r && r.droga}`);
    return [b, e];
  };
  const [A, eA] = await wejdz('LrA' + sfx, 'Rola' + sfx);
  const [B, eB] = await wejdz('LrB' + sfx, 'Rolb' + sfx);

  for (const [runda, chceA, chceB] of [[1, 'zamawiacz', 'licho'], [2, 'licho', 'zamawiacz']]) {
    for (const bot of [A, B]) { await menuLicha(bot); await kliknijSlot(bot, 5, { noweOkno: false }); }
    const p = await czekaj(async () => { const s = await stanRundy(); return faza(s) === 'POCZEKALNIA' && rola(s, A.username) && rola(s, B.username) ? s : null; }, 10000, 400);
    w.sprawdz(`r${runda}-poczekalnia`, !!p, p || await stanRundy());
    if (runda === 1) w.sprawdz('czat-role', /Role losuje gra/.test(eA.czat.slice(-6).join(' ')), eA.czat.slice(-2).join(' | '));
    await wybierz(w, A, chceA === 'licho' ? 23 : 25, chceA === 'licho' ? /Licho/ : /Zamawiacz/, `r${runda}-A`);
    await wybierz(w, B, chceB === 'licho' ? 23 : 25, chceB === 'licho' ? /Licho/ : /Zamawiacz/, `r${runda}-B`);
    w.sprawdz(`r${runda}-czat-wybor`, /pierwszeństwo/.test(eA.czat.slice(-4).join(' ')), eA.czat.slice(-1).join(''));
    const s = await czekaj(async () => { const s = await stanRundy(); return faza(s) === 'ROZPROSZENIE' ? s : null; }, 60000, 1000);
    const ra = s && rola(s, A.username), rb = s && rola(s, B.username);
    w.sprawdz(`r${runda}-role`, ra === chceA.toUpperCase() && rb === chceB.toUpperCase(), `A chce ${chceA} -> ${ra}, B chce ${chceB} -> ${rb}`);
    if (!s) return;
    const [Z, eZ] = ra === 'ZAMAWIACZ' ? [A, eA] : [B, eB];
    if (runda === 1) {
      // Zamawiacz w łowach próbuje się wcielić (PPM w zaczepę snopka) -> podpowiedź
      await czekaj(async () => faza(await stanRundy()) === 'LOWY' || null, 40000, 1000);
      ruchGracza(Z);
      const t = await idz(Z, SNOPEK.x + 2, SNOPEK.y, SNOPEK.z, { r: 1, ms: 20000 });
      const z = Object.values(Z.entities).find(e => e.name === 'interaction' && e.position.distanceTo({ x: SNOPEK.x + 0.5, y: SNOPEK.y, z: SNOPEK.z + 0.5 }) < 0.6);
      if (z) { await Z.lookAt(z.position.offset(0, 0.5, 0)); Z.activateEntity(z); }
      const pas = await czekaj(() => /tylko Licho/.test(eZ.actionbar || '') || null, 3000, 100);
      w.sprawdz('ppm-zamawiacz', !!pas, `${t.opis} | zaczepa ${!!z} | pasek: ${eZ.actionbar || ''}`);
    }
    // obaj wychodzą z pauzy (OPUŚĆ)
    for (const bot of [A, B]) { if (await otworzHub(bot).catch(() => null)) await kliknijSlot(bot, 7, { noweOkno: false }); await sleep(1000); }
    const brak = await czekaj(async () => /Brak rundy/.test(await stanRundy()) || null, 10000, 500);
    w.sprawdz(`r${runda}-wyjscie`, !!brak, await stanRundy());
    await sleep(2000);
  }
  // LOSUJ (41) czyści wybór: wejdź do poczekalni, pauza, LOSUJ
  await menuLicha(A); await kliknijSlot(A, 5, { noweOkno: false });
  await czekaj(async () => rola(await stanRundy(), A.username) || null, 8000, 400);
  await wybierz(w, A, 41, /losuje gra/, 'losuj');
  // lore kafla DOŁĄCZ pokazuje wybór (placeholder rozwinięty)
  if (await otworzHub(A).catch(() => null)) await kliknijSlot(A, 7, { noweOkno: false });
  await sleep(1000);
  const m = await menuLicha(A);
  w.sprawdz('lore-dolacz', /Teraz: .*losuje gra/.test(tekst(m.slots[5])) && !/%licho_chce%/.test(tekst(m.slots[5])), tekst(m.slots[5]).slice(0, 300));
}, { limitMs: 360000 });
