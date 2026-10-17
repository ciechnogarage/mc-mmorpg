'use strict';
// Eval modelu staminy (AnimacjeBroni Wytrzymalosc + DwieBronie) na klonie.
//   run.sh stamina <ten plik> a|b|c
//   a = prawa reka: model, lekki, oddech, regeneracja, mocny, zadyszka, garda (bez regeneracji, koszt, pekniecie)
//   b = ciagla walka dla zestawow 14/10/7 s
//   c = DwieBronie: mocny lewa w jednostkach
// Kazdy tryb na swiezym graczu. Slot z mieczem bierzemy z serwera (Inventory) i ustawiamy wprost: przewijanie
// hotbara otwiera pasek wyboru Zaklec, a wtedy kazdy PPM jest przejmowany (DENY) - zmierzone na klonie 17.10.
const { scenariusz, czekaj, sleep } = require('/bot/lib');

const TRYB = process.argv[2] || 'a';
const NICK = 'BotSt' + TRYB + (Date.now() % 10000);
const X = 0.5, Y = -59, Z = 0.5;

scenariusz('stamina-' + TRYB, async ({ w, polacz, rcon }) => {
  const bot = await polacz({ nick: NICK });
  await sleep(1500);
  let cooldown = false;
  bot._client.on('set_cooldown', () => { cooldown = true; });

  const stan = async () => {
    const t = await rcon(`animacjebroni stamina gracz ${NICK}`);
    const m = /zestaw=(\d+)s L=([\d.]+) pkt wsp_broni=([\d.]+) lekki=([\d.]+) stamina=([\d.]+)\/(\d+)/.exec(t);
    if (!m) throw new Error('nieczytelny stan: ' + t);
    return { zestaw: +m[1], L: +m[2], wsp: +m[3], lekki: +m[4], st: +m[5], max: +m[6] };
  };
  const ustaw = async (v) => rcon(`mmocore admin resource-stamina set ${NICK} ${v}`);
  const blisko = (a, b, tol) => Math.abs(a - b) <= tol;
  const trzymajMiecz = async () => {
    for (let proba = 0; proba < 5; proba++) {
      if ((await rcon(`minecraft:data get entity ${NICK} SelectedItem.id`)).includes('iron_sword')) return true;
      const inv = await rcon(`minecraft:data get entity ${NICK} Inventory`);
      const m = /\{Slot: (\d)b, id: "minecraft:iron_sword"/.exec(inv) || /id: "minecraft:iron_sword"[^}]*?Slot: (\d)b/.exec(inv);
      if (m) bot.setQuickBarSlot(+m[1]);
      await sleep(400);
    }
    return false;
  };

  const kl = await rcon(`mmocore admin class ${NICK} BASTION`);   // bieglosc miecz + tarcza
  await rcon(`minecraft:gamemode survival ${NICK}`);
  await rcon(`minecraft:teleport ${NICK} ${X} ${Y} ${Z} 0 0`);
  await rcon(`mi give SWORD MIECZ_ZELAZNY ${NICK} 1`);
  await sleep(1500);
  const trzyma = await trzymajMiecz();
  w.sprawdz('przygotowanie', trzyma, `${kl.trim().slice(0, 50)}; serwer trzyma miecz=${trzyma}`);
  if (!trzyma) return;
  await sleep(800);
  let s = await stan();

  if (TRYB === 'a') {
    w.sprawdz('model-zestaw-10', s.zestaw === 10 && blisko(s.L, s.max / 16, 0.01),
      `zestaw=${s.zestaw} L=${s.L} (= ${s.max}/(10*1,6)) wsp=${s.wsp} lekki=${s.lekki}`);

    // lekki: koszt przy kazdym machnieciu (tez w powietrze), bez regeneracji miedzy ciosami < 1 s
    await ustaw(s.max); await sleep(1200);
    const przed = (await stan()).st;
    for (let i = 0; i < 5; i++) { bot.swingArm('right'); await sleep(600); }
    const po = (await stan()).st;
    w.sprawdz('lekki-5-machniec', blisko(przed - po, 5 * s.lekki, 0.15),
      `spadek ${(przed - po).toFixed(2)}, oczekiwany 5 x ${s.lekki} = ${(5 * s.lekki).toFixed(2)}`);

    // oddech i tempo regeneracji
    const a = (await stan()).st; const b = (await stan()).st;
    w.sprawdz('oddech-bez-regeneracji', b <= a + 0.05, `zaraz po ciosach ${a.toFixed(2)} -> ${b.toFixed(2)}`);
    await ustaw(4); await sleep(1500);   // nisko: pasek nie dobije do maksimum w oknie pomiaru
    const t1 = Date.now(); const r1 = (await stan()).st;
    await sleep(3000);
    const t2 = Date.now(); const r2 = (await stan()).st;
    const tempo = (r2 - r1) / ((t2 - t1) / 1000);
    w.sprawdz('regeneracja-tempo', blisko(tempo, 1.6 * s.L, 0.35 * 1.6 * s.L),
      `${tempo.toFixed(2)} pkt/s, oczekiwane ${(1.6 * s.L).toFixed(2)} (1,6 L/s) [${r1.toFixed(2)} -> ${r2.toFixed(2)}]`);

    // mocny (PPM przytrzymany, bez kierunku = znad glowy): 2,5 x lekki
    await trzymajMiecz();
    await ustaw(s.max); await sleep(1200);
    const m0 = (await stan()).st;
    bot.activateItem(); await sleep(800); bot.deactivateItem(); await sleep(400);
    const m1 = (await stan()).st;
    w.sprawdz('mocny-2.5L', blisko(m0 - m1, 2.5 * s.lekki, 0.2),
      `spadek ${(m0 - m1).toFixed(2)}, oczekiwany 2,5 x ${s.lekki} = ${(2.5 * s.lekki).toFixed(2)}`);

    // zadyszka: pusty pasek = cios dalej wchodzi, ~60% obrazen
    await trzymajMiecz();
    await rcon(`minecraft:summon husk ${X} ${Y} ${Z + 2} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["st_cel"],attributes:[{id:"minecraft:max_health",base:1000}],Health:1000f}`);
    const cel = await czekaj(() => Object.values(bot.entities).find(e => e.name === 'husk'), 5000, 200);
    const hp = async () => {
      const m = /([\d.]+)f/.exec(await rcon('minecraft:data get entity @e[type=husk,tag=st_cel,limit=1] Health'));
      return m ? +m[1] : NaN;
    };
    const cios = async (st) => {
      await ustaw(st); await sleep(1600);   // > okno rytmu 24 t: kazdy cios pierwszy w serii (bez x1,4)
      if (st === 0) await ustaw(0);
      await bot.lookAt(cel.position.offset(0, 1.4, 0), true);
      const h0 = await hp(); bot.attack(cel); await sleep(400); return h0 - await hp();
    };
    const pelne = [], puste = [];
    if (cel) { for (let i = 0; i < 4; i++) pelne.push(await cios(s.max)); for (let i = 0; i < 4; i++) puste.push(await cios(0)); }
    const sr = (t) => t.reduce((x, y) => x + y, 0) / Math.max(1, t.length);
    const stosunek = sr(puste) / Math.max(1e-6, sr(pelne));
    w.sprawdz('zadyszka-60proc', !!cel && sr(pelne) > 0 && blisko(stosunek, 0.6, 0.12),
      `pelny pasek ${pelne.map(v => v.toFixed(1))}, pusty ${puste.map(v => v.toFixed(1))}, stosunek ${stosunek.toFixed(2)}`);
    await rcon('minecraft:kill @e[type=husk,tag=st_cel]');

    // garda mieczem (Shift + PPM): bez regeneracji; zablokowany cios kosztuje (wzor: log BLOK_STAMINA); bez staminy peka
    await trzymajMiecz();
    await ustaw(s.max / 2); await sleep(1200);
    bot.setControlState('sneak', true); await sleep(300);
    bot.activateItem(); await sleep(1500);
    const g0 = (await stan()).st; await sleep(3500); const g1 = (await stan()).st;
    w.sprawdz('garda-bez-regeneracji', blisko(g1, g0, 0.05),
      `w gardzie bez ciosow ~3,5 s: ${g0.toFixed(2)} -> ${g1.toFixed(2)}`);
    await rcon(`minecraft:summon husk ${X} ${Y} ${Z + 2} {Silent:1b,PersistenceRequired:1b,Tags:["st_bije"]}`);
    const napastnik = await czekaj(() => Object.values(bot.entities).find(e => e.name === 'husk'), 5000, 200);
    const k0 = (await stan()).st;
    const tStart = Date.now();
    while (Date.now() - tStart < 3000) { if (napastnik) await bot.lookAt(napastnik.position.offset(0, 1.4, 0), true); await sleep(250); }
    const k1 = (await stan()).st;
    w.sprawdz('blok-garda-koszt', k0 - k1 > 0, `w gardzie pod ciosami 3 s: ${k0.toFixed(2)} -> ${k1.toFixed(2)} (koszt na cios: log BLOK_STAMINA)`);
    const tP = Date.now();
    while (!cooldown && Date.now() - tP < 12000) { if (napastnik) await bot.lookAt(napastnik.position.offset(0, 1.4, 0), true); await sleep(300); }
    w.sprawdz('garda-peka', cooldown, cooldown ? 'set_cooldown broni po ciosie w garde bez staminy' : 'brak set_cooldown w 12 s');
    bot.deactivateItem(); bot.setControlState('sneak', false);
    await rcon('minecraft:kill @e[type=husk,tag=st_bije]');
  }

  if (TRYB === 'b') {
    for (const z of [14, 10, 7]) {
      await rcon(`animacjebroni stamina ${z}`);
      await trzymajMiecz();
      s = await stan();
      await ustaw(s.max); await sleep(300);
      const szybkosc = 1.6 / s.wsp;
      const start = Date.now();
      let przy70 = null;
      while ((Date.now() - start) / 1000 < z * 1.1) {
        bot.swingArm('right');
        await sleep(1000 / szybkosc);
        if (przy70 === null && (Date.now() - start) / 1000 >= z * 0.7) przy70 = (await stan()).st;
      }
      const koniec = (await stan()).st;
      w.sprawdz(`ciagla-walka-${z}s`, przy70 > s.lekki && koniec < s.lekki,
        `po ${(0.7 * z).toFixed(1)} s: ${przy70?.toFixed(2)}, po ${(1.1 * z).toFixed(1)} s: ${koniec.toFixed(2)} (lekki ${s.lekki}, ${szybkosc.toFixed(2)} ciosu/s)`);
      await ustaw(s.max); await sleep(500);
    }
    await rcon('animacjebroni stamina 10');
  }

  if (TRYB === 'c') {
    await rcon(`minecraft:item replace entity ${NICK} weapon.offhand from entity ${NICK} weapon.mainhand`);
    await sleep(500);
    await rcon(`mi give SWORD MIECZ_ZELAZNY ${NICK} 1`);
    await sleep(2000);
    await trzymajMiecz();
    await sleep(1500);
    const lewa = (await rcon(`minecraft:data get entity ${NICK} equipment.offhand.id`)).includes('iron_sword');
    s = await stan();
    await ustaw(s.max); await sleep(1300);
    const d0 = (await stan()).st;
    bot.activateItem(true); await sleep(900); bot.deactivateItem(); await sleep(400);
    const d1 = (await stan()).st;
    w.sprawdz('dwie-bronie-mocny-lewa', lewa && blisko(d0 - d1, 2.5 * s.lekki, 0.25),
      `lewa=${lewa}; spadek ${(d0 - d1).toFixed(2)}, oczekiwany 2,5 x ${s.lekki} = ${(2.5 * s.lekki).toFixed(2)} (wsp ${s.wsp}, z premia szybkosci dwoch broni)`);
  }
}, { limitMs: 200000 });
