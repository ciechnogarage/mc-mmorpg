'use strict';
// Licho — trening CHOWAJ jak gracz (konto bez OP): kreator -> Q -> Drużyna -> LICHO -> CHOWAJ (slot 43).
// Gracz jest Lichem, chowa się w snopku przy bramie i zastyga (ścieżka idealna); w łowach bramą wychodzą 2 boty-Zamawiacze
// (Mannequin), chodzą, sprawdzają sprzęty. Potem gracz wychodzi z kryjówki i rusza się na widoku (ścieżka nieidealna):
// bot ma go zauważyć (pogon) i trafić. Psota przy bocie = podejrzenie. Koniec, powrót, sprzątnięte encje (też manekiny).
// RCON tylko do obserwacji (`licho stan`, stan.*).
//   /opt/docker/mc/narzedzia/bot/run.sh <klon> $PWD/testy/chowaj.js
const { scenariusz, rcon, czekaj, sleep, stan, postac, otworzHub, kliknijSlot, tytulOkna, obserwuj, ruchGracza, idz, f } = require('/bot/lib');

const SNOPEK = { x: -6, y: 64, z: 19 };      // najbliższy sprzęt przy bramie (mapa.txt, jak runda.js)
const stanRundy = async () => (await rcon('licho stan')).replace(/\s+/g, ' ');
const faza = s => (s.match(/faza=(\w+)/) || [])[1] || 'BRAK';
const uczestnicy = s => s.split(' | ').slice(1).map(c => {
  const m = c.match(/^(.+?) (LICHO|ZAMAWIACZ) iskry=(\d+) tchu=(\d+) pkt=(\d+) ksztalt=(\S+) zastygly=(\S+)(?: postac=(\S+) pogon=(\w+) podejrzenie=(\S+))?/);
  return m && { nick: m[1], rola: m[2], iskry: +m[3], pkt: +m[5], ksztalt: m[6], zastygly: m[7],
    postac: m[8] ? m[8].split(',').map(Number) : null, pogon: m[9] === 'true', podejrzenie: m[10] };
}).filter(Boolean);
const boty = s => uczestnicy(s).filter(u => / \(bot\)$/.test(u.nick));
const dyst = (a, b) => Math.hypot(a[0] - b[0], a[1] - b[1], a[2] - b[2]);

scenariusz('licho-chowaj', async ({ w, polacz }) => {
  const sfx = Date.now() % 10000;
  const A = await polacz({ nick: 'LiH' + sfx }); const eA = obserwuj(A);
  const r = await postac.wejdz(A, { imie: 'Lich' + sfx });
  w.sprawdz('postac', !!r, `droga ${r && r.droga}`);
  const skad = await stan.wymiar(A.username);
  await czekaj(async () => !/creator_limbo/.test(await stan.wymiar(A.username)) || null, 15000, 500);
  await sleep(1500);
  let hub = null;
  for (let i = 0; i < 3 && !hub; i++) hub = await otworzHub(A).catch(async () => { await sleep(1500); return null; });
  w.sprawdz('q', !!hub, 'Q otwiera hub');
  if (!hub) return;
  await kliknijSlot(A, 38);
  const menu = await kliknijSlot(A, 25);
  const tekst = k => k ? JSON.stringify(k.components || k.nbt || k) : '';
  w.sprawdz('kafel-szukaj', /Trening: szukasz botów/.test(tekst(menu.slots[41])), tekst(menu.slots[41]).slice(0, 80));
  w.sprawdz('kafel-chowaj', /Trening: chowasz się/.test(tekst(menu.slots[43])), tekst(menu.slots[43]).slice(0, 80));
  await kliknijSlot(A, 43, { noweOkno: false });
  const wym = await czekaj(async () => (await stan.wymiar(A.username)) === 'minecraft:licho' || null, 8000, 400);
  w.sprawdz('wejscie', !!wym, `wymiar ${await stan.wymiar(A.username)}`);

  const s1 = await czekaj(async () => { const s = await stanRundy(); return boty(s).length ? s : null; }, 5000, 400);
  const ja = s1 && uczestnicy(s1).find(u => u.nick === A.username);
  w.sprawdz('rola-licho', ja && ja.rola === 'LICHO' && faza(s1) === 'ROZPROSZENIE', s1 || await stanRundy());
  w.sprawdz('boty-zamawiacze', s1 && boty(s1).length === 2 && boty(s1).every(b => b.rola === 'ZAMAWIACZ'), s1 && boty(s1).map(b => b.nick + ':' + b.rola).join(', '));
  w.sprawdz('tytul', /Lichem/.test(eA.opis()), eA.opis().split('\n').filter(l => /TYTUL/.test(l)).join(' / '));
  w.sprawdz('czat-start', /CHOWAJ/.test(eA.czat.slice(-6).join(' ')), eA.czat.slice(-2).join(' | '));
  w.sprawdz('tryb', (await stan.trybGry(A.username)) === 'adventure', await stan.trybGry(A.username));
  if (!s1) return;

  // ścieżka idealna: snopek przy bramie, PPM w zaczepę, stoi = zastyga
  ruchGracza(A);
  const t1 = await idz(A, SNOPEK.x + 2, SNOPEK.y, SNOPEK.z, { r: 1, ms: 20000 });
  w.sprawdz('chodzi', t1.ok, t1.opis);
  const zaczepa = Object.values(A.entities).find(e => e.name === 'interaction' && e.position.distanceTo({ x: SNOPEK.x + 0.5, y: SNOPEK.y, z: SNOPEK.z + 0.5 }) < 0.6);
  if (zaczepa) { await A.lookAt(zaczepa.position.offset(0, 0.5, 0)); A.activateEntity(zaczepa); }
  A.clearControlStates();
  const ja2 = () => stanRundy().then(s => uczestnicy(s).find(u => u.nick === A.username));
  const zs = await czekaj(async () => { const g = await ja2(); return g && g.ksztalt === 'hay_block' && g.zastygly !== '-' ? g : null; }, 6000, 300);
  w.sprawdz('wcielenie-zastygniecie', !!zs, JSON.stringify(await ja2()));

  // łowy: boty wychodzą bramą (manekiny w świecie), ruszają się
  const s2 = await czekaj(async () => { const s = await stanRundy(); return faza(s) === 'LOWY' ? s : null; }, 30000, 1000);
  w.sprawdz('lowy', !!s2, (s2 || await stanRundy()).slice(0, 80));
  if (!s2) return;
  await sleep(500);
  const man = await rcon('minecraft:execute in minecraft:licho positioned 0 64 0 if entity @e[type=minecraft:mannequin,distance=..80]');
  const p0 = boty(await stanRundy());
  w.sprawdz('manekiny', /Count: 2|passed/i.test(man) && p0.every(b => b.postac), `${man} / ${p0.map(b => b.postac && b.postac.join(',')).join(' ; ')}`);
  w.sprawdz('manekiny-widac', Object.values(A.entities).filter(e => e.name === 'mannequin').length === 2,
    `encje u gracza: ${[...new Set(Object.values(A.entities).map(e => e.name))].join(',')}`);
  await sleep(6000);
  const p1 = boty(await stanRundy());
  const ruch = p1.map(b => { const a = p0.find(x => x.nick === b.nick); return a && a.postac && b.postac ? dyst(a.postac, b.postac) : 0; });
  w.sprawdz('boty-chodza', ruch.some(d => d > 1.5), `przesunięcie w 6 s: ${ruch.map(d => d.toFixed(1)).join(', ')}`);

  // 25 s w kryjówce: obserwacja (boty mogą znaleźć Licho przy bramie — to też poprawny wynik)
  const obs = { podejrzenia: 0, maxDo: 99, iskryPrzed: zs ? zs.iskry : 3 };
  for (let i = 0; i < 25; i++) {
    const s = await stanRundy(); if (faza(s) !== 'LOWY') break;
    const g = uczestnicy(s).find(u => u.nick === A.username); if (!g) break;
    for (const b of boty(s)) { if (b.podejrzenie !== '-') obs.podejrzenia++; if (b.postac) obs.maxDo = Math.min(obs.maxDo, dyst(b.postac, [SNOPEK.x + 0.5, SNOPEK.y, SNOPEK.z + 0.5])); }
    obs.iskry = g.iskry;
    await sleep(1000);
  }
  w.sprawdz('diag-kryjowka', true, JSON.stringify(obs) + ' | pasek ' + (eA.actionbar || ''));

  // psota (F) — boty w zasięgu słuchu mają podejrzenie
  let s3 = await stanRundy();
  if (faza(s3) === 'LOWY') {
    const blisko = boty(s3).filter(b => b.postac && dyst(b.postac, [SNOPEK.x + 0.5, SNOPEK.y, SNOPEK.z + 0.5]) <= 11);
    f(A);
    await sleep(700);
    const po = boty(await stanRundy());
    w.sprawdz('psota-slychac', !blisko.length || blisko.every(b => { const q = po.find(x => x.nick === b.nick); return q && (q.podejrzenie !== '-' || q.pogon); }),
      `w zasięgu ${blisko.map(b => b.nick).join(',') || 'nikt'} -> ${po.map(b => b.nick + ' pod=' + b.podejrzenie).join(' ; ')} | ${eA.actionbar || ''}`);
  }

  // ścieżka nieidealna: Licho wychodzi i biega przed botem -> pościg i trafienie
  s3 = await stanRundy();
  let pogon = false, trafiony = false, ostatnie = '';
  const iskry0 = (uczestnicy(s3).find(u => u.nick === A.username) || {}).iskry;
  const koniecTestu = Date.now() + 40000;
  while (Date.now() < koniecTestu && faza(s3) === 'LOWY') {
    const b = boty(s3).filter(b => b.postac).sort((a, c) => dyst(a.postac, [A.entity.position.x, A.entity.position.y, A.entity.position.z]) - dyst(c.postac, [A.entity.position.x, A.entity.position.y, A.entity.position.z]))[0];
    if (!b) break;
    const [bx, by, bz] = b.postac;
    // w stronę bota, zatrzymuje się 3 bl. przed nim (widać go, rusza się)
    await idz(A, bx + 3 * Math.sign(A.entity.position.x - bx || 1), by, bz, { r: 1.5, ms: 4000 });
    s3 = await stanRundy();
    const g = uczestnicy(s3).find(u => u.nick === A.username);
    if (boty(s3).some(x => x.pogon)) pogon = true;
    if (!g || g.rola !== 'LICHO' || g.iskry < iskry0) { trafiony = true; break; }
    ostatnie = boty(s3).map(x => `${x.nick} ${x.postac} pogon=${x.pogon}`).join(' ; ');
  }
  w.sprawdz('pogon', pogon || trafiony, ostatnie + ' | ' + (eA.actionbar || ''));
  w.sprawdz('trafienie', trafiony, `${JSON.stringify(uczestnicy(await stanRundy()).find(u => u.nick === A.username))} | ${eA.opis().split('\n').filter(l => /TYTUL/.test(l)).join(' / ')}`);
  A.clearControlStates();

  // koniec (złapane albo świt) -> podsumowanie -> powrót, encje i manekiny sprzątnięte
  const kon = await czekaj(async () => ['PODSUMOWANIE', 'KONIEC', 'BRAK'].includes(faza(await stanRundy())) || null, 170000, 1000);
  w.sprawdz('koniec', !!kon, await stanRundy());
  w.sprawdz('tytul-koniec', /Złapane|Świt/.test(eA.opis()), eA.opis().split('\n').filter(l => /TYTUL|BOSS/.test(l)).join(' / '));
  await czekaj(() => /nie liczy się do rekordów/.test(eA.czat.slice(-12).join(' ')) || null, 5000, 300);
  w.sprawdz('czat-bez-rekordow', /nie liczy się do rekordów/.test(eA.czat.slice(-12).join(' ')), eA.czat.slice(-6).join(' | '));
  const wrot = await czekaj(async () => (await stan.wymiar(A.username)) === skad || null, 25000, 1000);
  w.sprawdz('powrot', !!wrot, `${await stan.wymiar(A.username)} (skąd ${skad}), tryb ${await stan.trybGry(A.username)}`);
  await sleep(1000);
  const ile = [];
  for (const t of ['interaction', 'block_display', 'mannequin'])
    ile.push(await rcon(`minecraft:execute in minecraft:licho positioned 0 64 0 if entity @e[type=minecraft:${t},distance=..80]`));
  w.sprawdz('encje-sprzatniete', ile.every(x => !/passed/i.test(x)), ile.join(' / '));
}, { limitMs: 420000 });
