'use strict';
// Licho — ćwiczenie, ścieżki nieidealne (2 konta bez OP):
// 1) B sam w zwykłej poczekalni (DOŁĄCZ), A klika TRENING -> odmowa „trwa runda z graczami”, A zostaje poza Zagrodą;
// 2) B dostaje podpowiedź „Sam? … TRENING”, wychodzi Q -> Opuść zagrodę i klika TRENING -> ćwiczenie z botami;
// 3) A klika DOŁĄCZ w trakcie ćwiczenia -> wchodzi jako Zamawiacz;
// 4) B wychodzi z serwera -> ćwiczenie trwa dla A; 5) A wychodzi -> runda się kończy, encje znikają.
//   /opt/docker/mc/narzedzia/bot/run.sh <klon> $PWD/testy/cwiczenie-nieidealne.js
const { scenariusz, rcon, czekaj, sleep, stan, postac, otworzHub, kliknijSlot, obserwuj } = require('/bot/lib');

const stanRundy = async () => (await rcon('licho stan')).replace(/\s+/g, ' ');
const faza = s => (s.match(/faza=(\w+)/) || [])[1] || 'BRAK';
const rola = (s, nick) => (s.match(new RegExp(`\\| ${nick} (\\w+) `)) || [])[1];
const ileBotow = s => (s.match(/ \(bot\) LICHO/g) || []).length;

async function menuLicha(bot) {
  await czekaj(async () => !/creator_limbo/.test(await stan.wymiar(bot.username)) || null, 15000, 500);
  await sleep(1500);
  let hub = null;
  for (let i = 0; i < 3 && !hub; i++) hub = await otworzHub(bot).catch(async () => { await sleep(1500); return null; });
  if (!hub) { await rcon(`dm open hub_glowne ${bot.username}`); await czekaj(() => bot.currentWindow, 4000, 100); }
  await kliknijSlot(bot, 38);
  return kliknijSlot(bot, 25);
}

scenariusz('licho-cwiczenie-nieidealne', async ({ w, polacz }) => {
  const sfx = Date.now() % 10000;
  const wejdz = async (nick, imie) => {
    const b = await polacz({ nick }); const e = obserwuj(b);
    const r = await postac.wejdz(b, { imie });
    w.sprawdz(`${nick}-postac`, !!r, `droga ${r && r.droga}`);
    return [b, e];
  };
  const [B, eB] = await wejdz('LnB' + sfx, 'Lnbb' + sfx);
  await sleep(30000);   // kreatory osobno (obciążenie klona)
  const [A, eA] = await wejdz('LnA' + sfx, 'Lnaa' + sfx);
  const skadA = await stan.wymiar(A.username);

  // 1) B w zwykłej poczekalni
  await menuLicha(B); await kliknijSlot(B, 5, { noweOkno: false });
  const p1 = await czekaj(async () => { const s = await stanRundy(); return faza(s) === 'POCZEKALNIA' && rola(s, B.username) ? s : null; }, 8000, 400);
  w.sprawdz('b-poczekalnia', !!p1, p1 || await stanRundy());

  // A klika TRENING -> odmowa
  await menuLicha(A); await kliknijSlot(A, 41, { noweOkno: false });
  await sleep(1500);
  const sA = await stanRundy();
  w.sprawdz('a-odmowa', /trwa runda z graczami/.test(eA.czat.slice(-5).join(' ')) && !rola(sA, A.username) && ileBotow(sA) === 0,
    `${eA.czat.slice(-2).join(' | ')} || ${sA}`);
  w.sprawdz('a-poza', (await stan.wymiar(A.username)) === skadA, await stan.wymiar(A.username));

  // 2) B (sam w poczekalni): podpowiedź, Q w zagrodzie = pauza -> Opuść (slot 7), potem TRENING z menu Licha
  w.sprawdz('b-podpowiedz', /Sam\? .*SZUKAJ.*CHOWAJ/.test(eB.czat.slice(-8).join(' ')), eB.czat.slice(-2).join(' | '));
  const pauza = await otworzHub(B).catch(() => null);
  w.sprawdz('b-pauza', pauza && !!pauza.slots[7], 'Q w zagrodzie otwiera pauzę');
  await kliknijSlot(B, 7, { noweOkno: false });
  const wyszedl = await czekaj(async () => (await stan.wymiar(B.username)) !== 'minecraft:licho' || null, 8000, 400);
  w.sprawdz('b-opuscil', !!wyszedl && /Brak rundy/.test(await stanRundy()), `${await stan.wymiar(B.username)} / ${await stanRundy()}`);
  await menuLicha(B); await kliknijSlot(B, 41, { noweOkno: false });
  const p2 = await czekaj(async () => { const s = await stanRundy(); return ileBotow(s) === 3 ? s : null; }, 6000, 400);
  w.sprawdz('b-trening', p2 && rola(p2, B.username) === 'ZAMAWIACZ', p2 || await stanRundy());

  // 3) A dołącza w trakcie (DOŁĄCZ) -> Zamawiacz
  await menuLicha(A); await kliknijSlot(A, 5, { noweOkno: false });
  const p3 = await czekaj(async () => { const s = await stanRundy(); return rola(s, A.username) ? s : null; }, 8000, 400);
  w.sprawdz('a-dolacza', p3 && rola(p3, A.username) === 'ZAMAWIACZ' && (await stan.wymiar(A.username)) === 'minecraft:licho', p3 || await stanRundy());
  w.sprawdz('a-czat-cwiczenie', /nie liczy się do rekordów/.test(eA.czat.slice(-6).join(' ')), eA.czat.slice(-3).join(' | '));

  // 4) B wychodzi -> trwa dalej
  B.quit(); await sleep(3000);
  const p4 = await stanRundy();
  w.sprawdz('b-wyszedl-trwa', faza(p4) !== 'BRAK' && !rola(p4, B.username) && ileBotow(p4) === 3, p4);

  // 5) A wychodzi -> koniec, encje sprzątnięte
  A.quit(); await sleep(3000);
  const p5 = await stanRundy();
  w.sprawdz('a-wyszedl-koniec', faza(p5) === 'BRAK' || /brak rundy|Brak rundy/i.test(p5), p5);
  const ile = await rcon('minecraft:execute in minecraft:licho positioned 0 64 0 if entity @e[type=minecraft:block_display,distance=..80]');
  const ile2 = await rcon('minecraft:execute in minecraft:licho positioned 0 64 0 if entity @e[type=minecraft:interaction,distance=..80]');
  w.sprawdz('encje-sprzatniete', !/passed/i.test(ile) && !/passed/i.test(ile2), `${ile} / ${ile2}`);
}, { limitMs: 300000 });
