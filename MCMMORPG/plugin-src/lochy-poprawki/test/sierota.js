// Osierocona instancja MD (24.10): gracz klika kafel lochu i rozlacza sie, zanim swiat instancji sie wczyta.
// MD (DungeonClassic.prepInstance) dopisuje go po wczytaniu bez sprawdzenia isOnline -> w instancji gracz offline
// bez trackera wyrzucenia; po powrocie MD uwaza go za "w lochu" (nie teleportuje) i instancja wisi bez konca.
// Zamiatacz LochyPoprawki (co 30 s): offline bez trackera -> po KickOfflinePlayersDelay (300 s) liczonym od
// pierwszego przejscia po rozlaczeniu wyrzuca przy kolejnym przejsciu (razem max 300 + 2x30 = 360 s od rozlaczenia);
// online poza swiatem instancji 2 przejscia z rzedu -> wypisuje; pusta instancja -> dispose.
// Uruchom: run.sh <klon --z-kreatorem --z-iris> <ten plik> powrot|bez_powrotu   (bez_powrotu: LIMIT_MS ~ 700000)
const { scenariusz, postac, sleep } = require('/bot/lib');
const TRYB = process.argv[2] || 'powrot';
const MAPA = 'sala_mistrzow', SLOT = 32;   // chowance_lochy.yml: Sala Mistrzow Wiezi
const NICK = 'Sierota' + String(Date.now() % 100000).padStart(5, '0');

scenariusz('sierota-' + TRYB, async ({ w, polacz, rcon }) => {
  const ok = (n, p, i = '') => w.sprawdz(n, p, i);
  const rc0 = rcon; rcon = async c => { for (let i = 0; ; i++) { try { return await rc0(c); } catch (e) { if (i >= 5) throw e; await sleep(2000); } } };
  const stan = async () => (await rcon('lochy stan')).replace(/§.|\s+$/g, '');
  const wpis = s => (s.match(new RegExp(`(${MAPA}_\\d+):[^|]*${NICK}=(\\S+)`)) || []);
  const wymiar = async () => { const m = (await rcon(`data get entity ${NICK} Dimension`)).match(/data:\s*"?([^"\s]+)"?\s*$/); return m ? m[1] : null; };
  const jest = async s => /time is/i.test(await rcon(`execute in minecraft:${s} run minecraft:time query gametime`));
  const kafel = async bot => {
    await rcon(`dm open chowance_lochy ${NICK}`);
    for (let i = 0; i < 20 && !bot.currentWindow; i++) await sleep(250);
    if (!bot.currentWindow) return false;
    await bot.clickWindow(SLOT, 0, 0).catch(() => {});
    return true;
  };

  let bot = await polacz({ nick: NICK, czasMs: 60000 });
  ok('postac', true, JSON.stringify(await postac.wejdz(bot, { imie: NICK })));
  await sleep(2000);
  // gracz klika kafel i od razu wychodzi (zamkniety klient, zerwane laczenie) - swiat instancji jeszcze sie kopiuje
  ok('kafel', await kafel(bot), 'menu lochow otwarte, klik kafla');
  bot.quit(); bot = null;
  let swiat = null, s = '';
  for (let i = 0; i < 15 && !swiat; i++) { await sleep(1000); s = await stan(); swiat = wpis(s)[1] || null; }
  const stanGracza = wpis(s)[2];
  // bez poprawki MD: gracz w instancji jako offline BEZ trackera (to jest blad, ktory test odtwarza)
  ok('sierota_powstala', !!swiat && stanGracza === 'offline', s);
  if (!swiat) return;

  if (TRYB === 'powrot') {
    await sleep(6000);
    bot = await polacz({ nick: NICK, czasMs: 60000 });
    const dr = await postac.wejdz(bot, { imie: NICK });
    ok('powrot', true, `${JSON.stringify(dr)} wymiar ${await wymiar()}; ${await stan()}`);
    // zamiatacz: 2 przejscia co 30 s z graczem online poza swiatem instancji -> wypisanie, potem dispose
    let t = null; const t0 = Date.now();
    for (let i = 0; i < 50 && t === null; i++) { await sleep(2000); if (!(await jest(swiat))) t = (Date.now() - t0) / 1000; }
    ok('instancja_zamknieta', t !== null && t <= 95, t !== null ? `${swiat} zniknal po ${t.toFixed(0)} s od powrotu` : `${swiat} dalej jest; ${await stan()}`);
  } else {
    // offline bez trackera: 300 s + do 2 przejsc zamiatacza po 30 s; t0 liczy sie juz po rozlaczeniu, +10 s na odpytywanie
    let t = null; const t0 = Date.now();
    for (let i = 0; i < 220 && t === null; i++) { await sleep(2000); if (!(await jest(swiat))) t = (Date.now() - t0) / 1000; }
    ok('instancja_zamknieta', t !== null && t <= 370, t !== null ? `${swiat} zniknal po ${t.toFixed(0)} s offline` : `${swiat} dalej jest; ${await stan()}`);
    bot = await polacz({ nick: NICK, czasMs: 60000 });
    ok('powrot', true, JSON.stringify(await postac.wejdz(bot, { imie: NICK })));
  }
  ok('brak_wpisu', !wpis(await stan())[1], await stan());

  // skutek dla gracza: znow wchodzi do lochu z menu (bez poprawki MD uwaza go za "w lochu" i nic sie nie dzieje)
  await sleep(2000);
  ok('menu_po_powrocie', await kafel(bot), 'menu lochow otwarte');
  let wym = null;
  for (let i = 0; i < 20 && !new RegExp(MAPA).test(wym || ''); i++) { await sleep(1500); wym = await wymiar(); }
  ok('ponowne_wejscie', new RegExp(`${MAPA}_\\d+`).test(wym || ''), `wymiar ${wym}`);
  await rcon(`md leave ${NICK}`).catch(() => {});
}, { limitMs: +(process.env.LIMIT_MS || (TRYB === 'powrot' ? 400000 : 800000)) });
