'use strict';
// Licho nie śpi — ścieżki nieidealne jak gracze (konta bez OP, wszystko z menu Q):
//   1) Licho wychodzi z gry (rozłączenie) w rozproszeniu -> runda przerwana, za mało graczy;
//   2) gracz klika DOŁĄCZ, gdy runda się kończy -> komunikat, zostaje, gdzie był;
//   3) relog wyrzuconego: wraca do świata źródłowego, survival, bez niewidzialności;
//   4) dołączenie w trakcie rundy -> Zamawiacz w Komorze.
//      (24.10: bot Z utworzony ostatni bywa bez okien — serwer wstrzymuje mu pakiety ~40 s; ten krok sprawdzony klientem QA:
//       DOŁĄCZ w ROZPROSZENIU -> ZAMAWIACZ, y=41, bossbar „Komora · Licha się chowają”.)
// RCON tylko do obserwacji (`licho stan`, stan.*).
// Gracz z zewnątrz (C) chodzi w OSOBNYM kontenerze bota: 3 boty w jednym (1 CPU, 512 MB) w Stolicy = keepalive timeout (24.10).
//   run.sh licho <ścieżka>/testy/nieidealne-c.js &  run.sh licho <ścieżka>/testy/nieidealne.js
const { scenariusz, rcon, czekaj, sleep, stan, postac, otworzHub, kliknijSlot, tytulOkna, obserwuj } = require('/bot/lib');

const stanRundy = async () => (await rcon('licho stan')).replace(/\s+/g, ' ');
const faza = s => (s.match(/faza=(\w+)/) || [])[1] || 'BRAK';
const rola = (s, nick) => (s.match(new RegExp(`\\| ${nick} (\\w+)`)) || [])[1];

/** Q -> Drużyna -> LICHO; zostawia otwarte menu Licha (DOŁĄCZ = slot 5). */
async function menuLicha(w, bot, tag) {
  await czekaj(async () => !/creator_limbo/.test(await stan.wymiar(bot.username)) || null, 15000, 500);
  await sleep(1500);
  let hub = null;
  for (let i = 0; i < 3 && !hub; i++) hub = await otworzHub(bot).catch(async () => { await sleep(1500); return null; });
  // Q po kreatorze INNEGO gracza bywa martwe u bota (znalezisko poza Lichem, runda.js) — ta sama komenda co Zaklecia na Q
  if (!hub) { await rcon(`dm open hub_glowne ${bot.username}`); await czekaj(() => bot.currentWindow, 4000, 100); }
  await kliknijSlot(bot, 38);
  const m = await kliknijSlot(bot, 25);
  w.sprawdz(`${tag}-menu`, /licho_1/.test(obserwuj(bot).opis()), `okno ${tytulOkna(m)?.slice(0, 8)}`);
}
/** czarna skrzynka: ostatnie pakiety z czasem; przy rozłączeniu wypisuje, na czym stanął strumień (diagnoza 30 s ciszy, 24.10) */
function skrzynka(bot) {
  const t0 = Date.now(), ost = [], ka = [], T = () => ((Date.now() - t0) / 1000).toFixed(1);
  let dane = 0, maxLag = 0, lagT = 0, prev = Date.now();
  // zegar co 1 s: duże opóźnienie = zamrożony proces bota; cisza w socket.data przy żywym zegarze = serwer/sieć
  const hb = setInterval(() => { const n = Date.now(), lag = n - prev - 1000; prev = n; if (lag > maxLag) { maxLag = lag; lagT = T(); } }, 1000);
  bot._client.socket?.on('data', () => { dane = Date.now(); });
  for (const em of [bot._client, bot]) { const o = em.emit.bind(em); em.emit = (n, ...a) => { const t = Date.now(); const r = o(n, ...a); const d = Date.now() - t; if (d > 300) console.log(`[${bot.username}] WOLNE ${em === bot ? 'bot' : 'client'}.${n} ${d} ms t=${T()}`); return r; }; }
  bot._client.on('packet', (d, m) => { if (m.name === 'ping') return; ost.push(`${T()} ${m.name}`); if (ost.length > 25) ost.shift(); });
  const wr = bot._client.write.bind(bot._client);
  bot._client.write = (n, p) => { if (n === 'keep_alive') { ka.push(T()); if (ka.length > 5) ka.shift(); } return wr(n, p); };
  bot.on('end', () => { clearInterval(hb); console.log(`[${bot.username}] SKRZYNKA koniec t=${T()} maxLag=${maxLag} ms przy t=${lagT} ostatnie_dane=${((dane - t0) / 1000).toFixed(1)} ka_out=${ka.join(',')} | ${ost.slice(-12).join(' | ')}`); });
}
const dolacz = bot => kliknijSlot(bot, 5, { noweOkno: false }).catch(() => null);
const wGrze = async bot => (await stan.wymiar(bot.username)) === 'minecraft:licho';

scenariusz('licho-nieidealne', async ({ w, polacz }) => {
  const sfx = Date.now() % 10000;
  const imiona = { A: 'Nida', B: 'Nidb' };
  const boty = {}, ekrany = {};
  for (const k of ['A', 'B']) {
    const nick = `Li${k}n${sfx}`;
    boty[k] = await polacz({ nick }); ekrany[k] = obserwuj(boty[k]); skrzynka(boty[k]);
    const r = await postac.wejdz(boty[k], { imie: imiona[k] + sfx });
    w.sprawdz(`${k}-postac`, !!r, `droga ${r && r.droga}`);
    await sleep(8000);   // scena kreatora jest ciężka: na klonie --cpus 2 trzy naraz = keepalive timeout (24.10)
  }
  const { A, B } = boty;
  const skad = await stan.wymiar(A.username);

  // A i B wchodzą; C (osobny proces) czeka z otwartym menu Licha
  for (const [k, b] of [['A', A], ['B', B]]) { await menuLicha(w, b, k); await dolacz(b); }
  const s1 = await czekaj(async () => { const s = await stanRundy(); return faza(s) === 'ROZPROSZENIE' ? s : null; }, 60000, 1000);
  w.sprawdz('rozproszenie', !!s1, s1 || await stanRundy());
  if (!s1) return;
  const [L, Z] = rola(s1, A.username) === 'LICHO' ? [A, B] : [B, A];
  const nickL = L.username;
  w.sprawdz('licho-niewidzialne', /invisibility/.test(JSON.stringify(await stan.efekty(nickL))), JSON.stringify(await stan.efekty(nickL)));

  // 1) Licho rozłącza się w rozproszeniu -> zostaje 1 gracz -> runda przerwana
  L.quit(); await sleep(1500);
  const s2 = await czekaj(async () => { const s = await stanRundy(); return faza(s) === 'KONIEC' ? s : null; }, 5000, 300);
  w.sprawdz('wyjscie-licha-konczy', !!s2, s2 || await stanRundy());
  const eZ = ekrany[Z === A ? 'A' : 'B'];   // powód przerwania: napis pod tytułem końca rundy
  const ok = await czekaj(() => /Za mało graczy|przerwana/i.test(eZ.opis()) || null, 3000, 200);
  w.sprawdz('komunikat-przerwana', !!ok, eZ.opis().split('\n').filter(l => /TYTUL|BOSSBAR/.test(l)).join(' / '));

  // 2) C klika DOŁĄCZ w podsumowaniu -> sprawdza nieidealne-c.js

  // 3) Licho wraca (relog >= 5 s): odesłane tam, skąd przyszło, survival, bez niewidzialności
  await sleep(5000);
  const L2 = await polacz({ nick: nickL }); skrzynka(L2);
  await postac.wejdz(L2, { imie: imiona[nickL[2]] + sfx });
  const wrot = await czekaj(async () => (await stan.wymiar(nickL)) === skad || null, 20000, 1000);
  w.sprawdz('relog-powrot', !!wrot, `${await stan.wymiar(nickL)} (skąd ${skad})`);
  w.sprawdz('relog-tryb', (await stan.trybGry(nickL)) === 'survival', await stan.trybGry(nickL));
  w.sprawdz('relog-bez-efektow', !/invisibility|blindness/.test(JSON.stringify(await stan.efekty(nickL))), JSON.stringify(await stan.efekty(nickL)));

  // 4) nowa runda: C (już w poczekalni) i L2, Z dołącza w trakcie -> Zamawiacz w Komorze
  await czekaj(async () => /POCZEKALNIA/.test(await stanRundy()) || null, 30000, 1000);
  await menuLicha(w, L2, 'L2'); await dolacz(L2);
  await czekaj(async () => (await stan.wymiar(Z.username)) === skad || null, 20000, 1000);
  await menuLicha(w, Z, 'Z');
  const s3 = await czekaj(async () => { const s = await stanRundy(); return faza(s) === 'ROZPROSZENIE' ? s : null; }, 60000, 1000);
  w.sprawdz('druga-runda', !!s3, s3 || await stanRundy());
  if (!s3) return;
  await dolacz(Z);
  await czekaj(async () => (await wGrze(Z)) || null, 8000, 400);
  const s4 = await stanRundy();
  const pz = await stan.poz(Z.username);
  w.sprawdz('dolacz-w-trakcie', rola(s4, Z.username) === 'ZAMAWIACZ' && pz.y < 50, `rola ${rola(s4, Z.username)}, y=${pz.y.toFixed(1)}`);

  // sprzątanie konsolą (poza testem; wyjście z pauzy sprawdzone klientem QA)
  for (const b of [L2, Z]) await rcon(`licho wyjdz ${b.username}`);
}, { limitMs: 420000 });
