// Koniec lochu Chowancow (24.10): po wygranej z Czarnoborem (sala_mistrzow) / po 10. fali (wieza_chowancow)
// Chowance wola `lochy sygnal`, mapa MD konczy loch (TriggerRemote -> FunctionFinishDungeon leave) i instancja znika.
// Uruchom: run.sh <klon --z-kreatorem> <ten plik> sala_mistrzow|sala_powtorka|wieza_chowancow  (LIMIT_MS ~ 1500000 dla wiezy)
// sala_powtorka (24.10): 1. wygrana z Czarnoborem poza lochem (odznaka + nagroda), 2. w lochu - bez nagrody, loch i tak sie konczy
// Przygotowanie RCON-em (gracz tego nie zrobi szybko): druzyna poz. 100 i odznaki. Wejscie przez menu, walka klawiszami
// okna tury jak gracz. Start walki = ta sama komenda, ktora odpala funkcja mapy przy podejsciu (TriggerDistance bez zmian).
const { scenariusz, Dialogi, postac, przedmioty, sleep } = require('/bot/lib');
const TRYB = process.argv[2] || process.env.MAPA || 'sala_mistrzow';
const POWTORKA = TRYB === 'sala_powtorka';
const MAPA = POWTORKA ? 'sala_mistrzow' : TRYB;
const SALA = MAPA === 'sala_mistrzow';
// nowy nick na przebieg: przerwany przebieg zostawia gracza w lochu MD (powrot do 300 s), nastepny by w niego wrocil
const NICK = (SALA ? 'KonSala' : 'KonWieza') + String(Date.now() % 100000).padStart(5, '0');
const SLOT = SALA ? 32 : 34;   // chowance_lochy.yml: mistrzowie 32, wieza 34

scenariusz('koniec-' + TRYB, async ({ w, polacz, rcon }) => {
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
  // czas wygranej z chwili komunikatu, nie z obrotu petli walki (petla z RCON-em spozniala sie o >5 s, 24.10)
  const WYGRANA = /Zdobywasz odznakę|nagroda mistrza jest jednorazowa/;
  // odstep wygrana -> koniec lochu z dwoch komunikatow jednym polaczeniem: opoznienie czatu bota (do ~4 s, 24.10) sie znosi
  const KONIEC = /ukończ|complete|finish/i;
  let tWygranej = 0, tKonca = 0;
  const czat = []; bot.on('messagestr', m => { czat.push(m); if (czat.length > 60) czat.shift();
    if (!tWygranej && WYGRANA.test(m)) tWygranej = Date.now(); if (!tKonca && KONIEC.test(m)) tKonca = Date.now(); });
  const droga = await postac.wejdz(bot, { imie: NICK });
  ok('postac', !!droga, JSON.stringify(droga));
  await sleep(3000);

  // przygotowanie: druzyna i odznaki (sala: 3 odznaki = wymog Czarnoboru; wieza: 4 = limit poz. 100)
  for (const g of ['GROMOWLADCA', 'ZELAZNOROG', 'WILKOR']) await rcon(`chw admin daj ${NICK} ${g} 100`);
  for (const o of SALA ? ['ZARU', 'FALI', 'KORONY'] : ['ZARU', 'FALI', 'KORONY', 'CIENIA']) await rcon(`chw admin odznaka ${NICK} ${o}`);
  await rcon(`chw admin lecz ${NICK}`);
  const i0 = await info();
  ok('przygotowanie', /druzyna=3/.test(i0), i0.slice(0, 160));

  // ruch jak gracz: klawisz 1 (slot 0) na "Twoj ruch!" (Chowance czyta PlayerItemHeldEvent i wraca slot na 8)
  // ruch na odnowieniu nie zuzywa tury (WalkiTurowe.klawisz) -> nastepny klawisz 2..4
  let mojRuch = false, ruch = 0; bot.on('messagestr', m => { if (/Twój ruch/.test(m)) { mojRuch = true; ruch = 0; } else if (/odnawia się/.test(m) && ruch < 3) { ruch++; mojRuch = true; } });
  const rekord = async () => +((await papi(NICK, '%chowance_wieza_WIEZA_CHOWANCOW_rekord%')).match(/\d+/) || [0])[0];
  // nagroda Czarnobora: kamien przemiany MROK w ekwipunku (serwer, `execute if items` - `data get Inventory` RCON ucina) + exp poskramiacza
  const nagroda = async () => ({ kamien: await przedmioty.ile(NICK, 'KAMIEN_PRZEMIANY_MROK'),
    exp: (await papi(NICK, '%mmocore_profession_experience_poskramiacz%')) });
  let inst = null, wym = null;
  let klik = 0, wygrana = null, wyjscie = null, ostRekord = 0, fale = [], t0 = 0;
  // walka: ta sama komenda co skill mapy (sala_mistrzow_wyzwanie_czarnobor / wieza_chowancow_arena)
  // wynik walki z mistrzem jest losowy (bot bije klawiszami po kolei, druzyna na limicie 80): po porazce gracz
  // leczy sie i probuje znowu - tu do 3 prob, bo test sprawdza nagrode i koniec lochu, nie sile bota
  const walcz = async (wLochu, nazwa) => {
    for (let p = 1; p <= (SALA ? 3 : 1); p++) {
      if (p > 1) { await rcon(`chw admin lecz ${NICK}`); await sleep(3000); }
      await walcz1(wLochu, p === 1 ? nazwa : `${nazwa}_proba${p}`);
      if (wygrana) return;
    }
  };
  const walcz1 = async (wLochu, nazwa) => {
    const odp = await rcon(SALA ? `chw admin walka-npc ${NICK} MISTRZ_CZARNOBOR` : `chw admin wieza ${NICK} WIEZA_CHOWANCOW`);
    ok(nazwa, /rozpocz|fala 1/i.test(odp), odp);
    czat.length = 0; mojRuch = false; wygrana = null; wyjscie = null; t0 = Date.now(); tWygranej = 0;
    const koniecMs = SALA ? 900000 : 1500000;
    while (Date.now() - t0 < koniecMs) {
      if (mojRuch) { mojRuch = false; await sleep(400); bot.setQuickBarSlot(ruch); klik++; await sleep(300); bot.setQuickBarSlot(8); }
      else if (d.biezacy) { try { d.kliknij(0); } catch (e) { /* okno bez przyciskow (podsumowanie) */ } }
      await sleep(1200);
      if (!SALA) {
        const r = await rekord();
        // bez `chw admin lecz` po fali: wstrzymywalo start nastepnej fali (24.10, aktywny=- po fali 4)
        if (r > ostRekord) { ostRekord = r; fale.push({ fala: r, wLochu: await wInstancji(inst) }); }
        if (r >= 10 && !wygrana) wygrana = Date.now();
      // wygrana z mistrzem: odznaka (1. raz) albo komunikat jednorazowej nagrody (powtorka, Chowance 24.10)
      } else if (!wygrana && tWygranej) wygrana = tWygranej;
      if (wygrana && !wLochu) { await sleep(3000); break; }
      if (wygrana && !(await wInstancji(inst))) { wyjscie = Date.now(); break; }
      if (wygrana && Date.now() - wygrana > 30000) break;
      if (!wygrana && czat.some(m => /przegr|porażk|poddajesz/i.test(m))) break;
    }
  };

  let n1 = null;
  if (POWTORKA) {
    // 1. wygrana poza lochem: odznaka CIENIA + nagroda
    const n0 = await nagroda();
    await walcz(false, 'start_walki_1');
    n1 = await nagroda();
    ok('wygrana_1_z_nagroda', !!wygrana && n1.kamien > n0.kamien && n1.exp !== n0.exp, `${JSON.stringify(n0)} -> ${JSON.stringify(n1)}, czat: ${czat.slice(-4).join(' | ')}`);
    if (!wygrana) return;
    await rcon(`chw admin lecz ${NICK}`); await sleep(3000);
  }

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

  await walcz(true, 'start_walki');
  if (POWTORKA) { const n2 = await nagroda(); ok('wygrana_2_bez_nagrody', !!wygrana && n2.kamien === n1.kamien && n2.exp === n1.exp && czat.some(m => /jednorazowa/.test(m)), `${JSON.stringify(n1)} -> ${JSON.stringify(n2)}`); }
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
  for (let i = 0; i < 10 && SALA && !tKonca; i++) await sleep(1000);
  const po = SALA ? (tKonca && tWygranej ? (tKonca - tWygranej) / 1000 : null) : (wyjscie && wygrana ? (wyjscie - wygrana) / 1000 : null);
  // sala: komunikat odznaki dochodzi do bota ~4 s pozniej niz koniec lochu wzgledem logu serwera (nagroda 02:07:33,
  // sygnal 02:07:38 = 100 tickow, 24.10) - dolny prog liczy tu tylko kolejnosc; odstep sprawdza log serwera
  ok('wyjscie_po_wygranej', po !== null && po >= (SALA ? 0 : 3) && po <= 15, po !== null ? `po ${po.toFixed(1)} s (sygnal 100 tickow)` : 'gracz zostal w lochu');
  wym = await wymiar(NICK);
  ok('poza_lochem', !new RegExp(MAPA).test(wym || ''), wym);
  let zamknieta = null;
  for (let i = 0; i < 20; i++) { if (!(await jest(inst))) { zamknieta = i * 2; break; } await sleep(2000); }
  ok('instancja_zamknieta', zamknieta !== null, zamknieta !== null ? `swiat ${MAPA}_${inst} zniknal po ~${zamknieta} s` : 'swiat dalej jest');
  for (let i = 0; i < 10 && !czat.some(m => KONIEC.test(m)); i++) await sleep(1000);
  ok('komunikat_konca', czat.some(m => KONIEC.test(m)), czat.slice(-6).join(' | '));
}, { limitMs: +(process.env.LIMIT_MS || 1700000) });
