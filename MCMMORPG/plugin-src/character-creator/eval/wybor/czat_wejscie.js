'use strict';
// Cisza na czacie od wejscia do konca sceny (wlasciciel 17.10: "wylacz czat przy kreatorze postaci bo syfi").
// Nowy gracz wchodzi, a RCON co 0,4 s rozsyla `say` i `tellraw @a`. Liczymy pakiety czatu (bez paska akcji):
//   przed-scena  od logowania do SPECTATOR sceny  -> 0 (tu byla dziura: scena czeka na MMOProfiles)
//   w-scenie     w scenie                          -> 0
//   po-scenie    po `cc scena <gracz> zamknij`     -> >0 (czat wraca)
// Uruchom: /opt/docker/mc/narzedzia/bot/run.sh <klon> czat_wejscie.js   (klon z --z-kreatorem)
const { scenariusz, rcon, sleep, czekaj, plain } = require('/bot/lib');

scenariusz('czat-wejscie', async ({ w, polacz }) => {
  const nick = 'CzatQA' + Math.floor(Math.random() * 900 + 100);
  const log = [];
  let t0 = Date.now();
  let faza = 'przed-scena';
  let spam = true;
  let n = 0;
  const spammer = (async () => {
    while (spam) {
      n++;
      try {
        await rcon(`say SPAM-${n}`);
        await rcon(`tellraw @a {"text":"TELL-${n}"}`);
      } catch (e) { /* RCON chwilowo zajety */ }
      await sleep(400);
    }
  })();
  const lap = bot => {
    const c = bot._client;
    const rec = (typ, tekst) => log.push({ t: Date.now() - t0, faza, typ, tekst: String(tekst).slice(0, 120) });
    c.on('system_chat', p => { if (!p.isActionBar) rec('system', plain(p.content)); });
    c.on('player_chat', p => rec('player', p.plainMessage));
    c.on('profileless_chat', p => rec('profileless', plain(p.message)));
  };

  await sleep(1500);
  t0 = Date.now();
  const bot = await polacz({ nick, plugins: [lap] });
  const wScenie = await czekaj(() => bot.game.gameMode === 'spectator', 30000, 100);
  w.sprawdz('scena', !!wScenie, `SPECTATOR po ${Date.now() - t0} ms`);
  faza = 'w-scenie';
  // Straznik wyboru otwiera scene od nowa co 10 s od wejscia — zamykamy wczesnie, zeby zostalo okno na czat.
  await sleep(4000);
  faza = 'zamykanie';
  await rcon(`cc scena ${nick} zamknij`);
  await czekaj(() => bot.game.gameMode !== 'spectator', 5000, 100);
  await sleep(500);
  faza = 'po-scenie';
  await sleep(3000);
  spam = false;
  await spammer;

  const ile = f => log.filter(m => m.faza === f);
  w.sprawdz('przed-scena', ile('przed-scena').length === 0,
    `${ile('przed-scena').length} wiadomosci miedzy logowaniem a scena`, ile('przed-scena'));
  w.sprawdz('w-scenie', ile('w-scenie').length === 0, `${ile('w-scenie').length} wiadomosci w scenie`, ile('w-scenie'));
  const po = [...ile('zamykanie'), ...ile('po-scenie')];
  w.sprawdz('po-scenie', po.some(m => /SPAM|TELL/.test(m.tekst)),
    `${po.length} wiadomosci od zamkniecia sceny (czat wraca)`, po.slice(0, 5));
  console.log(log.map(m => `${String(m.t).padStart(6)} ${m.faza.padEnd(11)} ${m.typ.padEnd(11)} ${m.tekst}`).join('\n'));
}, { limitMs: 120000 });
