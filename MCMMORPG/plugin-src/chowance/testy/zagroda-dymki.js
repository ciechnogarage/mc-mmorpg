'use strict';
// Zagroda młodych + dymki NPC + ciszej „Pierwsze kroki” (zgłoszenie właściciela 16.11).
//   narzedzia/bot/run.sh <klon> MCMMORPG/plugin-src/chowance/testy/zagroda-dymki.js
// Klon --z-kreatorem. Dwa boty z nowymi postaciami (bez chowańca). A przechodzi drogę gracza, B stoi obok i
// sprawdza, że dymki A są tylko dla A. Prawda: encje widziane przez bota (text_display, młode), czat, tytuł, pasek.
const { scenariusz, postac, stan, obserwuj, sleep } = require('/bot/lib');
const { Vec3 } = require('vec3');
const T0 = Date.now();
const log = (...a) => console.log(`[${((Date.now() - T0) / 1000).toFixed(0)}s]`, ...a);

const SW = 'minecraft:stolica_odnowa';
const PLOT = { x0: 35, x1: 51, z0: -59, z1: -44 };          // płot świerkowy (skan regionu 16.11)
const STARE = { x0: 22, x1: 35, z: -53.5 };                  // dawne stanowiska na trawniku
const wPlocie = p => p.x > PLOT.x0 && p.x < PLOT.x1 && p.z > PLOT.z0 && p.z < PLOT.z1;
const nazwa = e => e.name || e.type || '';
const czysc = s => String(s).replace(/§./g, '');

scenariusz('chowance-zagroda-dymki', async ({ w, polacz, rcon }) => {
  const nowy = async pref => {
    const b = await polacz({ nick: pref + (Date.now() % 100000) });
    b._e = obserwuj(b, { czatLinii: 200 });
    await postac.wejdz(b, { imie: pref + (Date.now() % 10000), rasa: 0, klasa: 0 });
    await sleep(2500);
    return b;
  };
  const A = await nowy('ZgA'); log('A w grze');
  await sleep(30000); // kreatory osobno (≥30 s, pułapka 8 skilla hosta)
  const B = await nowy('ZgB'); log('B w grze');
  await stan.tp(A.username, { x: 31.5, y: 101, z: -51.5 }, { swiat: SW, yaw: -90 });
  await stan.tp(B.username, { x: 31.5, y: 101, z: -53.5 }, { swiat: SW, yaw: -90 });
  await sleep(6000); // młode stawia pilnuj() co 5 s

  // ---- 1. młode stoją w płocie, nie na trawniku (pozycje z serwera po PDC chowance:mlode — ciała ModelEngine bot widzi niejasno)
  const GAT = ['KUDLACZ', 'ISKRZYK', 'MUSZLIK', 'PEDZIK', 'UPIOREK'];
  const pozSerwer = async g => {
    const o = await rcon(`minecraft:execute in ${SW} run minecraft:data get entity @e[limit=1,nbt={BukkitValues:{"chowance:mlode":"${g}"}}] Pos`);
    const m = o.match(/\[(-?[\d.]+)d, (-?[\d.]+)d, (-?[\d.]+)d\]/); return m ? new Vec3(+m[1], +m[2], +m[3]) : null;
  };
  const serw = {}; for (const g of GAT) serw[g] = await pozSerwer(g);
  log('pozycje', JSON.stringify(serw));
  const wszystkie = GAT.every(g => serw[g] && wPlocie(serw[g]));
  w.sprawdz('mlode-w-plocie', wszystkie, '5/5 młodych w płocie (x 35–51, z −59…−44)', GAT.map(g => `${g}@${serw[g] ? serw[g].x + ',' + serw[g].y + ',' + serw[g].z : 'brak'}`).join(' '));
  const ile = {}; for (const g of GAT) ile[g] = +((await rcon(`minecraft:execute in ${SW} run minecraft:execute if entity @e[nbt={BukkitValues:{"chowance:mlode":"${g}"}}]`)).match(/Count: (\d+)/) || [0, 0])[1];
  w.sprawdz('jedno-cialo-na-stanowisko', GAT.every(g => ile[g] === 1), 'dokładnie 1 encja młodego na gatunek (bez sierot po rozładowaniu chunku)', JSON.stringify(ile));
  const naTrawniku = Object.values(A.entities).filter(e => e.position && Math.abs(e.position.z - STARE.z) < 0.6 && e.position.x > STARE.x0 && e.position.x < STARE.x1
    && /text_display/.test(nazwa(e)));
  w.sprawdz('trawnik-pusty', naTrawniku.length === 0, 'brak etykiet młodych na dawnym trawniku', `${naTrawniku.length}`);
  // encja bota najbliższa pozycji młodego (do kliknięcia), bez napisów i chmur
  // jak gracz: klik w hitbox ModelEngine (`interaction` w miejscu młodego); sam wilk przychodzi do bota bez typu
  const encjaMlodego = (bot, g) => Object.values(bot.entities).filter(e => serw[g] && nazwa(e) === 'interaction' && e.position.distanceTo(serw[g]) < 0.6)[0];
  const mlode = () => GAT.map(g => encjaMlodego(A, g)).filter(e => e && e.position.distanceTo(serw[GAT[0]]) < 12);

  const strazniczka = Object.values(A.entities).find(e => /mannequin/.test(nazwa(e)) && e.position.distanceTo(new Vec3(32.5, 101, -48.5)) < 6);
  w.sprawdz('strazniczka-widoczna', !!strazniczka, 'Mannequin Strażniczki przy furtce', strazniczka ? strazniczka.position.toString() : 'brak');
  if (!strazniczka) return;
  const dymkiNad = (bot, poz, r = 1.6) => Object.values(bot.entities).filter(e => /text_display/.test(nazwa(e)) && e.position.distanceTo(poz) < r);
  const glowa = strazniczka.position.offset(0, 2.4, 0);
  const czatNpc = b => b._e.czat.filter(l => /Strażniczka Ogrodu:|Na start umie|Pieczęć już masz|Nowa twarz/.test(czysc(l)));

  // ---- 2. klik w młode bez pieczęci: opis w dymku zamiast 3 linii czatu, etykieta schowana
  const cel = encjaMlodego(A, 'MUSZLIK');
  log('cel', cel?.id, nazwa(cel), cel?.type, cel?.position?.toString());
  // treść napisu z metadanych encji (TextDisplay: komponent tekstu); szukamy po treści, nie liczbie wyświetlaczy
  const tekst = e => { try { return JSON.stringify(e.metadata || []); } catch { return ''; } };
  const zTekstem = (bot, re) => Object.values(bot.entities).filter(e => /text_display/.test(nazwa(e)) && re.test(tekst(e)));
  const etykietaA = zTekstem(A, /Muszlik/).filter(e => e.position.distanceTo(cel.position) < 2.5).map(e => e.id);
  await A.lookAt(cel.position.offset(0, 0.5, 0), true); A.activateEntity(cel); await sleep(1200);
  const dymA = zTekstem(A, /Na start umie/), dymB = zTekstem(B, /Na start umie/);
  const etykietaWciazA = etykietaA.filter(id => A.entities[id]);
  w.sprawdz('opis-mlodego-w-dymku', dymA.length === 1 && dymB.length === 0 && czatNpc(A).length === 0,
    'A widzi 1 dymek z opisem, B żadnego, na czacie brak opisu', `A ${dymA.length} B ${dymB.length}; czat: ${czatNpc(A).join(' | ')}`);
  w.sprawdz('etykieta-schowana', etykietaA.length > 0 && etykietaWciazA.length === 0, 'stała etykieta młodego schowana u A na czas dymku',
    `etykiet przed ${etykietaA.length}, nadal widocznych ${etykietaWciazA.length}`);
  w.sprawdz('pasek-bez-pieczeci', /Pieczęć Opiekuna da ci Strażniczka/.test(JSON.stringify(A._e.historia)), 'pasek mówi, skąd pieczęć (historia paska — inne komunikaty mogą go zaraz nadpisać)', A._e.actionbar);

  log('krok 3');
  // ---- 3. rozmowa ze Strażniczką: dymek nad nią tylko u A, nic na czacie, pieczęć po ~4 s
  const przedB = dymkiNad(B, glowa).length;
  const przedA = dymkiNad(A, glowa).length;
  log('strazniczka', strazniczka.id, nazwa(strazniczka), 'A@', A.entity.position.toString());
  A.activateEntity(strazniczka); await sleep(1500);
  log('dymki A przy Strażniczce', zTekstem(A, /Strażniczka|Pieczęć|twarz/).map(e => e.id + '@' + e.position));
  w.sprawdz('dymek-strazniczki-A', dymkiNad(A, glowa).length === przedA + 1, 'A widzi nowy dymek nad Strażniczką', `${przedA} → ${dymkiNad(A, glowa).length}`);
  w.sprawdz('dymek-niewidoczny-dla-B', dymkiNad(B, glowa).length === przedB, 'B nie widzi dymka A', `${przedB} → ${dymkiNad(B, glowa).length}`);
  await sleep(3500);
  w.sprawdz('pieczec-dana', /Otrzymano: Pieczęć Opiekuna/.test(A._e.actionbar) || A.inventory.items().some(i => /paper/.test(i.name)),
    'pieczęć w ekwipunku / pasek „Otrzymano”', A._e.actionbar);
  w.sprawdz('czat-bez-npc', czatNpc(A).length === 0 && czatNpc(B).length === 0, 'kwestie Strażniczki nie lecą na czat (A i B)', czatNpc(A).concat(czatNpc(B)).join(' | '));
  await sleep(11000);
  w.sprawdz('dymek-znika', dymkiNad(A, glowa).length === przedA, 'dymek sam znika po czasie czytania', `${dymkiNad(A, glowa).length}`);

  log('krok 4');
  // ---- 4. jak gracz: furtka, wejście, klik młodego z pieczęcią → związanie
  const furtka = A.blockAt(new Vec3(34, 102, -51));
  w.sprawdz('furtka', /fence_gate/.test(furtka?.name || ''), 'furtka w 34/102/-51', furtka?.name);
  await A.lookAt(furtka.position.offset(0.5, 0.5, 0.5), true); await A.activateBlock(furtka); await sleep(600);
  A.setControlState('forward', true); A.setControlState('jump', true);
  const t0 = Date.now();
  while (Date.now() - t0 < 5000 && A.entity.position.x < 35.6) { await A.lookAt(new Vec3(38, 102.6, -51.5), true); await sleep(100); }
  A.setControlState('forward', false); A.setControlState('jump', false); await sleep(500);
  const wszedl = wPlocie(A.entity.position);
  w.sprawdz('wejscie-przez-furtke', wszedl, 'gracz wchodzi furtką do zagrody', A.entity.position.toString());
  if (!wszedl) await stan.tp(A.username, { x: 36.5, y: 102, z: -51.5 }, { swiat: SW, yaw: -90 });
  const bliski = mlode().sort((a, b) => a.position.distanceTo(A.entity.position) - b.position.distanceTo(A.entity.position))[0];
  await A.lookAt(bliski.position.offset(0, 0.5, 0), true); A.activateEntity(bliski); await sleep(3500);
  const st = (await stan.papi(A.username, '%chowance_kroki_postep%')).trim();
  w.sprawdz('zwiazano', /Związano/.test(czysc(A._e.title)) || /[0-4]\/4/.test(st), 'związanie: tytuł „Związano!”', `title="${czysc(A._e.title)}" kroki=${st}`);
  await sleep(2000);

  log('krok 5');
  // ---- 5. krok 1/4 (przywołanie): bez tytułu na środku ekranu, pasek + 1 linia czatu
  const tytulT = A._e.titleT; const czatN = A._e.czat.length;
  A.chat('/chw przywolaj 1'); await sleep(2600);
  const noweCzat = A._e.czat.slice(czatN).map(czysc);
  w.sprawdz('krok-bez-tytulu', A._e.titleT === tytulT, 'zaliczenie kroku nie pokazuje tytułu', `title="${czysc(A._e.title)}"`);
  w.sprawdz('krok-pasek', /Krok 1\/4/.test(A._e.actionbar), 'pasek „Krok 1/4 ✔ · Dalej”', A._e.actionbar);
  w.sprawdz('krok-czat-1-linia', noweCzat.filter(l => /Pierwsze kroki/.test(l)).length <= 1, 'najwyżej 1 linia „Pierwsze kroki” na czacie', noweCzat.join(' | '));
  console.log(A._e.opis({ okno: false, czat: 12 }));
}, { limitMs: 480000 });
