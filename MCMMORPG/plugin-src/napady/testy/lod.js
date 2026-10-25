'use strict';
// Napady LOD (decyzja wlasciciela 25.10: "2. a jak to dopracować i zrobić jak najlepiej"): bez gracza w poblizu
// napad liczy sie symulacja (0 encji), obserwacja/dolaczenie materializuje ciala MM, po wyjsciu i zwloce
// ciala znikaja, a napad leci dalej symulacja. Stan (zywi po stronach, brama, faza) przechodzi miedzy trybami.
// Sciezki nieidealne: obserwuj bez id, dolaczenie z obserwacji, wyjscie i ponowna materializacja.
// Uruchom: /opt/docker/mc/narzedzia/bot/run.sh <klon> <sciezka>/testy/lod.js
const { scenariusz, czekaj, sleep, stan } = require('/bot/lib');

// "#1 SZTURM t=12s atak 10 obrona 8 brama 0 lup 0 ... | lod CIALA ciala 18/lib 6 materializacji 1 | ziarno 7"
function napad(s, id) {
  const linia = s.split('\n').find((l) => l.includes(`#${id} `));
  if (!linia) return null;
  const m = linia.match(/#\d+ (\S+) t=(\d+)s atak (\d+) obrona (\d+) brama (-?\d+).*\| lod (\S+) ciala (\d+)\/lib \d+ materializacji (\d+)/);
  return m && { faza: m[1], t: +m[2], atak: +m[3], obrona: +m[4], brama: +m[5], lod: m[6], ciala: +m[7], mat: +m[8], zywi: +m[3] + +m[4] };
}

scenariusz('napady-lod', async ({ w, polacz, rcon }) => {
  const nick = 'BotLod' + (Date.now() % 100000);
  await rcon('napad stop');
  await rcon('napad limit 1');
  await rcon('napad lod auto');
  await rcon('minecraft:difficulty easy');
  const bot = await polacz({ nick });
  await rcon(`minecraft:gamemode survival ${nick}`);
  await sleep(1500);
  const start = await stan.poz(nick), startSwiat = await stan.wymiar(nick);
  const czat = [];
  bot.on('messagestr', (m) => czat.push(m));
  const st = async (id) => napad(await rcon('napad stan'), id);

  await rcon('napad start 1 7');
  const id = await czekaj(async () => { const m = (await rcon('napad stan')).match(/#(\d+) /); return m && m[1]; }, 15000, 500);
  w.sprawdz('napad-ruszyl', !!id, `id ${id}`);
  if (!id) return;

  // 1. nikogo w poblizu: symulacja, zero cial, a napad idzie dalej
  await sleep(3000);
  const s1 = await st(id);
  w.sprawdz('bez-gracza-symulacja', s1 && s1.lod === 'SYM' && s1.ciala === 0, JSON.stringify(s1));
  await sleep(15000);
  const s2 = await st(id);
  const ruch = s2 && (s2.t > s1.t) && (s2.faza !== s1.faza || s2.zywi !== s1.zywi || s2.brama !== s1.brama);
  w.sprawdz('symulacja-postepuje', !!ruch, `${JSON.stringify(s1)} -> ${JSON.stringify(s2)}`);

  // nieidealna: obserwuj bez numeru
  bot.chat('/napad obserwuj');
  const uz = await czekaj(() => czat.find((m) => /\/napad obserwuj <id>/.test(m)), 5000, 200);
  w.sprawdz('obserwuj-bez-id', !!uz, uz || czat.slice(-3).join(' | '));

  // 2. obserwacja: materializacja, potem widz w punkcie widza; kazdy zywy w modelu ma cialo
  const przed = await st(id);
  const t0 = Date.now();
  bot.chat(`/napad obserwuj ${id}`);
  const widz = await czekaj(async () => (await stan.trybGry(nick)) === 'spectator' && 'spectator', 20000, 250);
  const msMat = Date.now() - t0;
  const pw = await stan.poz(nick);
  w.sprawdz('obserwuj-widz', !!widz && pw && Math.abs(pw.x - 14.5) < 6 && Math.abs(pw.z - 20.5) < 6, `${await stan.trybGry(nick)} ${JSON.stringify(pw)} po ${msMat} ms`);
  const po = await st(id);
  w.sprawdz('materializacja-kompletna', po && po.lod === 'CIALA' && po.ciala === po.zywi && po.mat === 1, JSON.stringify(po));
  // tik napadu stoi w czasie przejscia, wiec liczby moga spasc tylko o polegnych w ciagu ~1 s po materializacji
  w.sprawdz('stan-do-cial', przed && po && po.zywi <= przed.zywi && po.zywi >= przed.zywi - 2 && po.brama <= przed.brama,
    `${JSON.stringify(przed)} -> ${JSON.stringify(po)}`);

  // 3. nieidealna: z obserwacji do walki (strona obrony) - tryb survival, punkt dolaczenia
  bot.chat(`/napad dolacz ${id} obrona`);
  const walka = await czekaj(async () => { const q = await stan.poz(nick); return (await stan.trybGry(nick)) === 'survival' && q && Math.abs(q.x - 30.5) < 2 && Math.abs(q.z - 26.5) < 2 && q; }, 10000, 250);
  w.sprawdz('obserwacja-do-walki', !!walka, `${await stan.trybGry(nick)} ${JSON.stringify(await stan.poz(nick))}`);
  await sleep(4000);

  // 4. wyjscie: powrot gracza od razu, ciala znikaja po zwloce (10 s), stan przechodzi do symulacji
  bot.chat('/napad opusc');
  const wrocil = await czekaj(async () => (await stan.wymiar(nick)) === startSwiat && (await stan.trybGry(nick)) === 'survival' && 'ok', 10000, 300);
  w.sprawdz('opusc-powrot', !!wrocil, `${await stan.wymiar(nick)} ${await stan.trybGry(nick)} (byl ${startSwiat} ${JSON.stringify(start)})`);
  let ostatniCiala = null, pierwszySym = null;
  const tWyj = Date.now();
  await czekaj(async () => {
    const s = await st(id);
    if (!s) return 'koniec';
    if (s.lod === 'CIALA') ostatniCiala = s;
    if (s.lod === 'SYM') { pierwszySym = s; return 'sym'; }
    return false;
  }, 40000, 400);
  const msDemat = Date.now() - tWyj;
  w.sprawdz('dematerializacja', pierwszySym && pierwszySym.ciala === 0, `${JSON.stringify(pierwszySym)} po ${msDemat} ms`);
  w.sprawdz('dematerializacja-zwloka', msDemat >= 8000, `${msDemat} ms (zwloka-s 10, kontrola co 0,5 s)`);
  w.sprawdz('stan-do-symulacji', ostatniCiala && pierwszySym && pierwszySym.zywi <= ostatniCiala.zywi && pierwszySym.zywi >= ostatniCiala.zywi - 2,
    `${JSON.stringify(ostatniCiala)} -> ${JSON.stringify(pierwszySym)}`);

  // 5. ponowna materializacja tego samego napadu (arena juz zbudowana, cieplo)
  const t1 = Date.now();
  bot.chat(`/napad obserwuj ${id}`);
  const widz2 = await czekaj(async () => (await stan.trybGry(nick)) === 'spectator' && 'ok', 20000, 250);
  const ponownie = await st(id);
  w.sprawdz('ponowna-materializacja', !!widz2 && ponownie && ponownie.lod === 'CIALA' && ponownie.mat === 2 && ponownie.ciala === ponownie.zywi,
    `${JSON.stringify(ponownie)} po ${Date.now() - t1} ms`);

  // 6. stop w trakcie obserwacji: gracz wraca, ciala i strzaly sprzatniete
  await rcon(`napad stop ${id}`);
  const wrocil2 = await czekaj(async () => (await stan.trybGry(nick)) === 'survival' && (await stan.wymiar(nick)) === startSwiat && 'ok', 15000, 300);
  w.sprawdz('stop-powrot', !!wrocil2, `${await stan.trybGry(nick)} ${await stan.wymiar(nick)}`);
  await sleep(3000);
  const zostalo = await rcon('execute in minecraft:napady if entity @e[type=!player,type=!item,type=!experience_orb,type=!text_display,x=-200,y=-64,z=-200,dx=1400,dy=400,dz=1400]');
  w.sprawdz('po-stopie-czysto', /failed|0$/i.test(zostalo.trim()), zostalo.trim());
  console.log(`LOD napad ${id}: materializacja ${msMat} ms, dematerializacja ${msDemat} ms po wyjsciu`);
});
