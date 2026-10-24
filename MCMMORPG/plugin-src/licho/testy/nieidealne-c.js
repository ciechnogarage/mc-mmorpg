'use strict';
// Licho nie śpi — gracz z zewnątrz (C) do nieidealne.js, w osobnym kontenerze bota (3 boty w jednym = keepalive timeout, 24.10):
//   2) C klika DOŁĄCZ, gdy runda się kończy (KONIEC) -> komunikat odmowy, zostaje, gdzie był;
//   4) po końcu rundy C dołącza pierwszy (nowa poczekalnia), dostaje rolę w drugiej rundzie.
// Start PRZED nieidealne.js (kreator C nie nakłada się na kreatory A/B):
//   run.sh licho <ścieżka>/testy/nieidealne-c.js &   # po „C-postac” w logu:  run.sh licho <ścieżka>/testy/nieidealne.js
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

scenariusz('licho-nieidealne-c', async ({ w, polacz }) => {
  const sfx = Date.now() % 10000;
  const C = await polacz({ nick: `LiCn${sfx}` }); const e = obserwuj(C); skrzynka(C);
  const r = await postac.wejdz(C, { imie: 'Nidc' + sfx });
  w.sprawdz('C-postac', !!r, `droga ${r && r.droga}`);
  const skad = await stan.wymiar(C.username);

  // 2) menu otwarte w poczekalni A/B, klik DOŁĄCZ dopiero, gdy runda przerwana (KONIEC)
  await czekaj(async () => /POCZEKALNIA/.test(await stanRundy()) || null, 240000, 1000);
  await menuLicha(w, C, 'C');
  const k = await czekaj(async () => faza(await stanRundy()) === 'KONIEC' || null, 120000, 250);
  w.sprawdz('C-koniec-widziany', !!k, await stanRundy());
  if (!k) return;
  await dolacz(C); await sleep(1500);
  w.sprawdz('C-odmowa-komunikat', /kończy|spróbuj za chwilę/i.test(e.czat.slice(-6).join(' | ')), e.czat.slice(-3).join(' | '));
  w.sprawdz('C-odmowa-zostaje', (await stan.wymiar(C.username)) === skad, `${await stan.wymiar(C.username)} (skąd ${skad})`);

  // 4) po końcu rundy C dołącza jako pierwszy do nowej poczekalni
  await czekaj(async () => { const s = await stanRundy(); return faza(s) === 'BRAK' || /Brak rundy/.test(s) || null; }, 20000, 500);
  await menuLicha(w, C, 'C2'); await dolacz(C);
  const wg = await czekaj(async () => (await stan.wymiar(C.username)) === 'minecraft:licho' || null, 8000, 400);
  w.sprawdz('C-poczekalnia', !!wg, `${await stan.wymiar(C.username)} / ${await stanRundy()}`);
  const s3 = await czekaj(async () => { const s = await stanRundy(); return faza(s) === 'ROZPROSZENIE' ? s : null; }, 150000, 1000);
  w.sprawdz('C-rola', !!s3 && /LICHO|ZAMAWIACZ/.test(rola(s3, C.username) || ''), s3 ? `rola ${rola(s3, C.username)}` : await stanRundy());

  // koniec: nieidealne.js wyprowadza L2 i Z -> runda za mała -> C wraca skąd przyszedł, w swoim trybie
  const wrot = await czekaj(async () => (await stan.wymiar(C.username)) === skad || null, 180000, 1000);
  w.sprawdz('C-powrot', !!wrot, `${await stan.wymiar(C.username)}, tryb ${await stan.trybGry(C.username)}`);
  if (!wrot) await rcon(`licho wyjdz ${C.username}`);
}, { limitMs: 600000 });
