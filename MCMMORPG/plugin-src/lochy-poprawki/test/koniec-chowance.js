// Koniec lochu Chowancow (24.10): po wygranej z Czarnoborem (sala_mistrzow) / po 10. fali (wieza_chowancow)
// Chowance wola `lochy sygnal`, mapa MD konczy loch (TriggerRemote -> FunctionFinishDungeon leave) i instancja znika.
// Uruchom: run.sh <klon --z-kreatorem> <ten plik> sala_mistrzow|wieza_chowancow  (LIMIT_MS ~ 1500000 dla wiezy)
// Przygotowanie RCON-em (gracz tego nie zrobi szybko): druzyna poz. 100 i odznaki. Wejscie przez menu, walka klawiszami
// okna tury jak gracz. Start walki = ta sama komenda, ktora odpala funkcja mapy przy podejsciu (TriggerDistance bez zmian).
const { scenariusz, Dialogi, postac, sleep } = require('/bot/lib');
const MAPA = process.argv[2] || process.env.MAPA || 'sala_mistrzow';
const SALA = MAPA === 'sala_mistrzow';
// nowy nick na przebieg: przerwany przebieg zostawia gracza w lochu MD (powrot do 300 s), nastepny by w niego wrocil
const NICK = (SALA ? 'KonSala' : 'KonWieza') + String(Date.now() % 100000).padStart(5, '0');
const SLOT = SALA ? 32 : 34;   // chowance_lochy.yml: mistrzowie 32, wieza 34

scenariusz('koniec-' + MAPA, async ({ w, polacz, rcon }) => {
  const ok = (n, p, i = '') => w.sprawdz(n, p, i);
  // tworzenie instancji (kopia swiata ~9 s) wstrzymuje watek serwera i RCON nie odpowiada - ponawiaj
  const rc0 = rcon; rcon = async c => { for (let i = 0; ; i++) { try { return await rc0(c); } catch (e) { if (i >= 5) throw e; await sleep(2000); } } };
  const jest = async n => /time is/i.test(await rcon(`execute in minecraft:${MAPA}_${n} run minecraft:time query gametime`));
  const wInstancji = async n => (await rcon(`execute in minecraft:${MAPA}_${n} positioned 0 100 0 if entity @a[name=${NICK},distance=..2000]`)).includes('passed');
  const info = async () => rcon(`chw admin info ${NICK}`);
  // stan.wymiar/papi z lib ida golym RCON (bez ponawiania) - tu przez rcon z ponawianiem
  const wymiar = async n => { const m = (await rcon(`data get entity ${n} Dimension`)).match(/data:\s*"?([^"\s]+)"?\s*$/); return m ? m[1] : null; };
  const papi = async (n, ph) => (await rcon(`papi parse ${n} ${ph}`)).trim();

  const bot = await polacz({ nick: NICK, czasMs: 60000 });
  const d = bot._dialogi || (bot._dialogi = new Dialogi(bot));
  const czat = []; bot.on('messagestr', m => { czat.push(m); if (czat.length > 60) czat.shift(); });
  const droga = await postac.wejdz(bot, { imie: NICK });
  ok('postac', !!droga, JSON.stringify(droga));
  await sleep(3000);

  // przygotowanie: druzyna i odznaki (sala: 3 odznaki = wymog Czarnoboru; wieza: 4 = limit poz. 100)
  for (const g of ['GROMOWLADCA', 'ZELAZNOROG', 'WILKOR']) await rcon(`chw admin daj ${NICK} ${g} 100`);
  for (const o of SALA ? ['ZARU', 'FALI', 'KORONY'] : ['ZARU', 'FALI', 'KORONY', 'CIENIA']) await rcon(`chw admin odznaka ${NICK} ${o}`);
  await rcon(`chw admin lecz ${NICK}`);
  const i0 = await info();
  ok('przygotowanie', /druzyna=3/.test(i0), i0.slice(0, 160));

  // sygnal poza lochem = nic (gracz w swiecie glownym)
  const w0 = await wymiar(NICK);
  const c0 = czat.length;
  await rcon(`lochy sygnal ${NICK} ${SALA ? 'sala_mistrzow_koniec' : 'wieza_fala_10'}`); await sleep(2000);
  ok('sygnal_poza_lochem_nic', (await wymiar(NICK)) === w0 && !czat.slice(c0).some(m => /complete|ukończ/i.test(m)), `${w0} -> ${await wymiar(NICK)}`);

  // wejscie jak gracz: menu lochow Chowancow (Q -> Menazeria -> Lochy; tu otwarte konsola) -> klik kafla
  await rcon(`dm open chowance_lochy ${NICK}`);
  for (let i = 0; i < 20 && !bot.currentWindow; i++) await sleep(250);
  ok('menu_lochow', !!bot.currentWindow, bot.currentWindow ? 'okno otwarte' : 'brak okna');
  if (bot.currentWindow) await bot.clickWindow(SLOT, 0, 0).catch(() => {});
  let inst = null, wym = null;
  for (let i = 0; i < 40 && inst === null; i++) {
    wym = await wymiar(NICK); const m = new RegExp(`${MAPA}_(\\d+)`).exec(wym || '');
    if (m) inst = +m[1]; else await sleep(1500);
  }
  ok('wejscie_menu', inst !== null, `wymiar ${wym}; czat: ${czat.slice(-5).join(' | ')}`);
  if (inst === null) return;
  await sleep(4000);

  // zly sygnal w lochu = nic (sala nie slucha wiezy i odwrotnie; wieza nie konczy po fali 9)
  await rcon(`lochy sygnal ${NICK} ${SALA ? 'wieza_fala_10' : 'wieza_fala_9'}`); await sleep(3000);
  ok('obcy_sygnal_nie_konczy', await wInstancji(inst), 'gracz dalej w lochu');

  // walka: ta sama komenda co skill mapy (sala_mistrzow_wyzwanie_czarnobor / wieza_chowancow_arena)
  const odp = await rcon(SALA ? `chw admin walka-npc ${NICK} MISTRZ_CZARNOBOR` : `chw admin wieza ${NICK} WIEZA_CHOWANCOW`);
  ok('start_walki', /rozpocz|fala 1/i.test(odp), odp);
  czat.length = 0;
  // ruch jak gracz: klawisz 1 (slot 0) na "Twoj ruch!" (Chowance czyta PlayerItemHeldEvent i wraca slot na 8)
  // ruch na odnowieniu nie zuzywa tury (WalkiTurowe.klawisz) -> nastepny klawisz 2..4
  let mojRuch = false, ruch = 0; bot.on('messagestr', m => { if (/Twój ruch/.test(m)) { mojRuch = true; ruch = 0; } else if (/odnawia się/.test(m) && ruch < 3) { ruch++; mojRuch = true; } });
  const rekord = async () => +((await papi(NICK, '%chowance_wieza_WIEZA_CHOWANCOW_rekord%')).match(/\d+/) || [0])[0];
  let klik = 0, wygrana = null, wyjscie = null, koniecMs = SALA ? 900000 : 1500000, ostRekord = 0, fale = [];
  const t0 = Date.now();
  while (Date.now() - t0 < koniecMs) {
    if (mojRuch) { mojRuch = false; await sleep(400); bot.setQuickBarSlot(ruch); klik++; await sleep(300); bot.setQuickBarSlot(8); }
    else if (d.biezacy) { try { d.kliknij(0); } catch (e) { /* okno bez przyciskow (podsumowanie) */ } }
    await sleep(1200);
    if (!SALA) {
      const r = await rekord();
      // bez `chw admin lecz` po fali: wstrzymywalo start nastepnej fali (24.10, aktywny=- po fali 4)
      if (r > ostRekord) { ostRekord = r; fale.push({ fala: r, wLochu: await wInstancji(inst) }); }
      if (r >= 10 && !wygrana) wygrana = Date.now();
    } else if (!wygrana && /CIENIA/.test(await info())) wygrana = Date.now();
    if (wygrana && !(await wInstancji(inst))) { wyjscie = Date.now(); break; }
    if (wygrana && Date.now() - wygrana > 30000) break;
    if (!wygrana && czat.some(m => /przegr|porażk|poddajesz/i.test(m))) break;
  }
  ok('wygrana', !!wygrana, `${SALA ? 'odznaka CIENIA' : 'rekord ' + ostRekord}, klikniec ${klik}, ${((Date.now() - t0) / 1000).toFixed(0)} s, czat: ${czat.slice(-4).join(' | ')}`);
  if (!SALA) ok('fale_bez_konca', fale.filter(f => f.fala < 10).every(f => f.wLochu) && fale.length >= 1, JSON.stringify(fale));
  if (!SALA && !wygrana) {
    // bot (klawisze 1-4 po kolei) nie przechodzi 10 fal: porazka nie konczy lochu, potem ta sama komenda, ktora
    // Chowance wysyla po fali 10 (WalkiTurowe.java:1054 FALA_WIEZY dla kazdej fali -> zdarzenia.yml)
    await sleep(5000);
    ok('porazka_nie_konczy', await wInstancji(inst), 'gracz dalej w lochu po porazce');
    await rcon(`lochy sygnal ${NICK} wieza_fala_10 100`); wygrana = Date.now();
    for (let i = 0; i < 20 && !wyjscie; i++) { await sleep(1000); if (!(await wInstancji(inst))) wyjscie = Date.now(); }
  }
  const po = wyjscie && wygrana ? (wyjscie - wygrana) / 1000 : null;
  ok('wyjscie_po_wygranej', po !== null && po >= 3 && po <= 15, po !== null ? `po ${po.toFixed(1)} s (sygnal 100 tickow)` : 'gracz zostal w lochu');
  wym = await wymiar(NICK);
  ok('poza_lochem', !new RegExp(MAPA).test(wym || ''), wym);
  let zamknieta = null;
  for (let i = 0; i < 20; i++) { if (!(await jest(inst))) { zamknieta = i * 2; break; } await sleep(2000); }
  ok('instancja_zamknieta', zamknieta !== null, zamknieta !== null ? `swiat ${MAPA}_${inst} zniknal po ~${zamknieta} s` : 'swiat dalej jest');
  ok('komunikat_konca', czat.some(m => /ukończ|complete|finish|Ukończ/i.test(m)), czat.slice(-6).join(' | '));
}, { limitMs: +(process.env.LIMIT_MS || 1700000) });
