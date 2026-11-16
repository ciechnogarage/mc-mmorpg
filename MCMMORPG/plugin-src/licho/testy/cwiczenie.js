'use strict';
// Licho — ćwiczenie solo z botami-Lichami, jak gracz (konto bez OP): kreator -> Q -> Drużyna -> LICHO -> TRENING,
// boty w stanie rundy, łowy, pozorny blok bota, trafienie -> ucieczka na inne miejsce, pogoń aż do złapania
// (wszystkich botów, jeśli zdąży), koniec, powrót, brak encji. Rekordy (plik) sprawdza wywołujący z hosta.
// RCON tylko do obserwacji (`licho stan`, stan.*).
//   /opt/docker/mc/narzedzia/bot/run.sh <klon> $PWD/testy/cwiczenie.js
const { scenariusz, rcon, czekaj, sleep, stan, postac, otworzHub, kliknijSlot, tytulOkna, obserwuj, ruchGracza, idz } = require('/bot/lib');

const stanRundy = async () => (await rcon('licho stan')).replace(/\s+/g, ' ');
const uczestnicy = s => s.split(' | ').slice(1).map(c => {
  const m = c.match(/^(.+) (LICHO|ZAMAWIACZ) iskry=(\d+) tchu=(\d+) pkt=(\d+) ksztalt=(\S+) zastygly=(\S+)/);
  return m && { nick: m[1], rola: m[2], iskry: +m[3], tchu: +m[4], pkt: +m[5], ksztalt: m[6], zastygly: m[7] };
}).filter(Boolean);
const boty = s => uczestnicy(s).filter(u => / \(bot\)$/.test(u.nick));
const faza = s => (s.match(/faza=(\w+)/) || [])[1] || 'BRAK';

async function trening(w, bot, tag) {
  await czekaj(async () => !/creator_limbo/.test(await stan.wymiar(bot.username)) || null, 15000, 500);
  await sleep(1500);
  let hub = null;
  for (let i = 0; i < 3 && !hub; i++) hub = await otworzHub(bot).catch(async () => { await sleep(1500); return null; });
  w.sprawdz(`${tag}-q`, !!hub, 'Q otwiera hub');
  if (!hub) return false;
  const dr = await kliknijSlot(bot, 38);
  w.sprawdz(`${tag}-druzyna`, !!dr.slots[25], `Drużyna: kafel LICHO w slocie 25 (${tytulOkna(dr)?.slice(0, 40)})`);
  const menu = await kliknijSlot(bot, 25);
  const k = menu.slots[41];
  const nazwa = k && JSON.stringify(k.components || k.nbt || k).match(/Ćwiczenie z botami/);
  w.sprawdz(`${tag}-kafel`, !!nazwa, `slot 41: ${k ? k.name : 'pusty'} ${nazwa ? 'Ćwiczenie z botami' : ''}`);
  await kliknijSlot(bot, 41, { noweOkno: false });
  return true;
}

scenariusz('licho-cwiczenie', async ({ w, polacz }) => {
  const sfx = Date.now() % 10000;
  const A = await polacz({ nick: 'LiC' + sfx }); const eA = obserwuj(A);
  const r = await postac.wejdz(A, { imie: 'Licc' + sfx });
  w.sprawdz('postac', !!r, `droga ${r && r.droga}`);
  const skad = await stan.wymiar(A.username);
  await trening(w, A, 'A');
  const wym = await czekaj(async () => (await stan.wymiar(A.username)) === 'minecraft:licho' || null, 8000, 400);
  w.sprawdz('wejscie', !!wym, `wymiar ${await stan.wymiar(A.username)}`);

  const s1 = await czekaj(async () => { const s = await stanRundy(); return boty(s).length ? s : null; }, 5000, 500);
  w.sprawdz('boty', s1 && boty(s1).length === 3 && boty(s1).every(b => b.rola === 'LICHO' && b.zastygly !== '-'), s1 || await stanRundy());
  const ja = s1 && uczestnicy(s1).find(u => u.nick === A.username);
  w.sprawdz('rola-gracza', ja && ja.rola === 'ZAMAWIACZ', JSON.stringify(ja));
  w.sprawdz('czat-start', /ćwiczenie|Ćwiczenie/.test(eA.czat.slice(-10).join(' | ')), eA.czat.slice(-4).join(' | '));
  if (!s1) return;

  const s2 = await czekaj(async () => { const s = await stanRundy(); return faza(s) === 'LOWY' ? s : null; }, 20000, 1000);
  w.sprawdz('lowy', !!s2, (s2 || await stanRundy()).slice(0, 60));
  if (!s2) return;
  const V = A.entity.position.constructor;
  ruchGracza(A);

  // pogoń: zawsze za najbliższym botem; po trafieniu bot ucieka, więc czytamy jego nowe miejsce ze stanu
  let pierwszeTrafienie = null, zlapane = 0, pozorny = null, pudla = 0;
  const trasy = [], miejsca = {};
  const koniecPogoni = Date.now() + 140000;
  while (Date.now() < koniecPogoni) {
    const s = await stanRundy();
    if (faza(s) !== 'LOWY') break;
    const lista = boty(s).filter(b => b.zastygly !== '-');
    for (const b of boty(s)) (miejsca[b.nick] = miejsca[b.nick] || new Set()).add(b.zastygly);
    if (!lista.length) { await sleep(400); continue; }
    const p = A.entity.position;
    const b = lista.map(b => { const [x, y, z] = b.zastygly.split(',').map(Number); return { ...b, x, y, z, d: p.distanceTo(new V(x + 0.5, y, z + 0.5)) }; })
      .sort((a, c) => a.d - c.d)[0];
    if (b.d > 3) { const t = await idz(A, b.x + 0.5, b.y, b.z + 0.5, { r: 1, ms: 12000 }); trasy.push(`${b.nick}@${b.zastygly}: ${t.opis}`); continue; }
    if (pozorny == null) { const bl = A.blockAt(new V(b.x, b.y, b.z)); pozorny = { widzi: bl && bl.name, ma: b.ksztalt }; }
    const cel = Object.values(A.entities).find(e => e.name === 'interaction' && e.position.distanceTo(new V(b.x + 0.5, b.y, b.z + 0.5)) < 0.6);
    if (!cel) { pudla++; await sleep(500); if (pudla > 40) break; continue; }
    await A.lookAt(cel.position.offset(0, 0.5, 0)); A.attack(cel);
    await sleep(500);
    const po = boty(await stanRundy()).find(x => x.nick === b.nick);
    if (!po) zlapane++;
    else if (!pierwszeTrafienie && po.iskry < b.iskry) {
      const pz = await czekaj(async () => { const q = boty(await stanRundy()).find(x => x.nick === b.nick); return q && q.zastygly !== '-' && q.zastygly !== b.zastygly ? q : null; }, 6000, 300);
      pierwszeTrafienie = { przed: b.zastygly, iskry: `${b.iskry}->${po.iskry}`, po: pz ? pz.zastygly : 'nie przeniósł się' };
    }
  }
  w.sprawdz('diag', true, `miejsca ${JSON.stringify(Object.fromEntries(Object.entries(miejsca).map(([k, v]) => [k, [...v]])))} | trasy(${trasy.length}) ${trasy.slice(0, 3).join(' ; ')} ... ${trasy.slice(-2).join(' ; ')}`);
  w.sprawdz('pozorny-blok', pozorny && pozorny.widzi === pozorny.ma, JSON.stringify(pozorny));
  w.sprawdz('trafienie-ucieczka', pierwszeTrafienie && pierwszeTrafienie.po !== 'nie przeniósł się', JSON.stringify(pierwszeTrafienie));
  w.sprawdz('zlapane', zlapane >= 1, `złapane boty: ${zlapane}/3, pudła(brak zaczepy) ${pudla}`);
  const s3 = await stanRundy();
  w.sprawdz('tchu', (uczestnicy(s3).find(u => u.nick === A.username) || { tchu: 0 }).tchu >= 0, s3.slice(0, 200));

  // koniec (złapane wszystkie albo czas) -> podsumowanie z dopiskiem o ćwiczeniu -> powrót
  const kon = await czekaj(async () => ['PODSUMOWANIE', 'KONIEC', 'BRAK'].includes(faza(await stanRundy())) || null, 40000, 1000);
  w.sprawdz('koniec', !!kon, await stanRundy());
  await czekaj(() => /nie liczy się do rekordów/.test(eA.czat.slice(-12).join(' ')) || null, 5000, 300);
  w.sprawdz('czat-bez-rekordow', /nie liczy się do rekordów/.test(eA.czat.slice(-12).join(' ')), eA.czat.slice(-6).join(' | '));
  const wrot = await czekaj(async () => (await stan.wymiar(A.username)) === skad || null, 25000, 1000);
  w.sprawdz('powrot', !!wrot, `${await stan.wymiar(A.username)} (skąd ${skad}), tryb ${await stan.trybGry(A.username)}`);
  w.sprawdz('tryb-po', (await stan.trybGry(A.username)) === 'survival', await stan.trybGry(A.username));
  await sleep(1000);
  const ile = await rcon('minecraft:execute in minecraft:licho positioned 0 64 0 if entity @e[type=minecraft:interaction,distance=..80]');
  const ile2 = await rcon('minecraft:execute in minecraft:licho positioned 0 64 0 if entity @e[type=minecraft:block_display,distance=..80]');
  w.sprawdz('encje-sprzatniete', !/passed/i.test(ile2) && !/passed/i.test(ile), `${ile} / ${ile2}`);
  w.info && w.info('gracz', A.username);
}, { limitMs: 330000 });
