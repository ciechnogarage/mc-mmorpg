'use strict';
// Arena remake'u Lochu 001 (kuznia/tools/przeciwnicy/README.md "Loch 001 remake"). Klon world1 BEZ --z-kreatorem
// (swiat 'world' plaski), paczka L1R_* + GAJ_* skills wgrana do plugins/MythicMobs.
//   run.sh <klon> arena_l001.js walka <MOB> nieuwazny|uwazny [POWT=3]   bot BASTION poz. 2, Kamienny miecz (MMOItems)
//   run.sh <klon> arena_l001.js pak                                     mechanika R1: pak, deptanie, dojrzewanie, Korzen
//   run.sh <klon> arena_l001.js garda                                   Korowiec: cios z przodu vs z tylu (garda -80%)
// Strata HP = scoreboard minecraft.custom:damage_taken (/10), nie bot.health (regen MMOCore). Linie POMIAR {...}.
// Nieuwazny: stoi i bije najblizszego wroga, pakow nie rusza. Uwazny: unik na dzwiek zapowiedzi (po glosnosci/wysokosci -
// mineflayer 4.39 myli nazwy dzwiekow 1.21.11), depcze paki, najpierw cel priorytetowy (Siewca, Kotwa).
const { scenariusz, sleep } = require('/bot/lib');
const { pathfinder, Movements } = require('mineflayer-pathfinder');
const pvp = require('mineflayer-pvp').plugin;

const [TRYB = 'walka', MOB = 'L1R_WILK', STYL = 'nieuwazny', POWT_S = '3'] = process.argv.slice(2);
const POWT = +POWT_S;
const S = { x: 300.5, y: -59, z: 300.5 };          // srodek areny (plaski swiat klona)
const BOX = 'x=286,y=-62,z=286,dx=28,dy=12,dz=28';
const r1 = x => +(+x).toFixed(1);

// zapowiedzi: klucz "glosnosc/wysokosc" (1 miejsce po przecinku) -> reakcja
const ZAPOWIEDZI = {
  '1.6/0.5': ['wilk-skok', 'bok', 700], '1.6/0.8': ['lucznik-naciag', 'bok', 900],
  '2.0/0.5': ['korzeniak', 'tyl', 1100], '1.8/0.7': ['siewca-puch', 'bok', 800],
  '1.8/0.6': ['korowiec-cios', 'tyl', 900], '1.8/0.8': ['sowiedz-skok', 'bok', 900],
  '1.8/0.5': ['sowiedz-lapa', 'tyl', 800],
};

scenariusz(`arena-l001-${TRYB}-${MOB}-${STYL}`, async ({ w, polacz, rcon }) => {
  const ile = async sel => { const m = /count: (\d+)|(\d+)$/i.exec((await rcon(`minecraft:execute in minecraft:overworld if entity ${sel}`)).trim()); return m ? +(m[1] || m[2]) : 0; };
  const hpScore = async nick => { const m = /has (\d+)/.exec(await rcon(`minecraft:scoreboard players get ${nick} l1r_dmg`)); return m ? +m[1] / 10 : 0; };
  const czysc = async () => { for (let i = 0; i < 3; i++) { await rcon(`minecraft:kill @e[type=!player,${BOX}]`); await sleep(200); } };
  const pomiar = (id, v) => console.log(`POMIAR ${id} ${JSON.stringify(v)}`);

  // --- scena
  await rcon('minecraft:execute in minecraft:overworld run minecraft:forceload add 280 280 320 320');
  await rcon('minecraft:gamerule spawn_monsters false');
  await rcon('minecraft:scoreboard objectives add l1r_dmg minecraft.custom:minecraft.damage_taken');
  for (const f of ['286 -59 286 314 -57 286', '286 -59 314 314 -57 314', '286 -59 286 286 -57 314', '314 -59 286 314 -57 314'])
    await rcon(`minecraft:execute in minecraft:overworld run minecraft:fill ${f} minecraft:mossy_cobblestone`);
  await czysc();

  const nick = (STYL === 'uwazny' ? 'Uw' : 'Nu') + (Date.now() % 100000);
  let bot = await polacz({ nick }); await sleep(2500);
  await rcon(`mmocore admin class ${nick} BASTION`);
  await rcon(`mmocore admin level set ${nick} main 2`);
  await rcon(`mmocore admin attribute give ${nick} bieglosc-miecz 1`);
  bot.quit(); await sleep(8000);                       // relog po zmianie klasy (weryf.js: inaczej zamrozenie pakietow)
  bot = await polacz({ nick }); await sleep(2500);
  bot.loadPlugin(pathfinder); bot.loadPlugin(pvp);
  const mv = new Movements(bot); mv.canDig = false; mv.allow1by1towers = false; mv.allowParkour = false;
  bot.pathfinder.setMovements(mv);
  await rcon(`minecraft:gamemode survival ${nick}`); await rcon(`minecraft:clear ${nick}`);
  await rcon(`mi give SWORD MIECZ_KAMIENNY ${nick} 1`); await sleep(800);
  const inv = await rcon(`minecraft:data get entity ${nick} Inventory`);
  const sl = /\{Slot: (\d)b, id: "minecraft:stone_sword/.exec(inv); if (sl) bot.setQuickBarSlot(+sl[1]);
  w.sprawdz('miecz', !!sl, `slot ${sl && sl[1]}`);

  let reakcjaDo = 0, zgon = false; const reakcje = {}; const nieznane = {};
  bot.on('death', () => { zgon = true; setTimeout(() => { try { bot.respawn(); } catch {} }, 300); });
  async function unik(kier, ms) {
    if (Date.now() < reakcjaDo) return; reakcjaDo = Date.now() + ms;
    bot.pvp.forceStop(); bot.clearControlStates(); bot.setControlState('sprint', true);
    if (kier === 'bok') bot.setControlState(Math.random() < 0.5 ? 'left' : 'right', true); else bot.setControlState('back', true);
    await sleep(ms); bot.clearControlStates();
  }
  bot.on('soundEffectHeard', (nazwa, poz, vol, pitch) => {
    const k = `${(+vol).toFixed(1)}/${(+pitch).toFixed(1)}`;
    const z = ZAPOWIEDZI[k];
    if (!z) { if (vol >= 1.4) nieznane[`${nazwa} ${k}`] = (nieznane[`${nazwa} ${k}`] || 0) + 1; return; }
    reakcje[z[0]] = (reakcje[z[0]] || 0) + 1;
    if (STYL !== 'uwazny') return;
    if (z[0] === 'korzeniak' && poz && poz.distanceTo(bot.entity.position) > 6) return;   // Wyrwa daleko od bota
    unik(z[1], z[2]);
  });

  const wArenie = e => e && e.position && Math.abs(e.position.x - S.x) < 14 && Math.abs(e.position.z - S.z) < 14;
  // nazwa z metadanych (custom_name, indeks 2) przychodzi razem ze spawnem; wyposazenie paka - osobnym pakietem pozniej
  const nazwaE = e => { const m = e.metadata && e.metadata[2]; return (m && m.value && m.value.text && m.value.text.value) || ''; };
  const jestPak = e => e.name === 'zombie' && (/^(Pąk|Duży pąk)/.test(nazwaE(e)) || (e.equipment || []).some(i => i && /azalea|dandelion/.test(i.name)));
  const TYPY_WROGA = new Set(['wolf', 'zombie', 'husk', 'skeleton', 'polar_bear']);   // text_display (paski MM) i stojaki odpadaja
  const wrog = e => TYPY_WROGA.has(e.name) && nazwaE(e) !== '' && !jestPak(e);
  const PRIOR = new Set(['husk', 'zombie']);   // Siewca (dorosly husk bez ruchu) / Kotwa (zombie bez AI) - rozpoznanie ponizej
  const cel = () => {
    const me = bot.entity.position;
    const wrogowie = Object.values(bot.entities).filter(e => wArenie(e) && wrog(e));
    if (STYL === 'uwazny') {
      const pak = Object.values(bot.entities).filter(e => wArenie(e) && jestPak(e) && e.position.distanceTo(me) < 7)
        .sort((a, b) => a.position.distanceTo(me) - b.position.distanceTo(me))[0];
      if (pak) return pak;
      const stoj = wrogowie.filter(e => PRIOR.has(e.name) && e._stoi);
      if (stoj.length) return stoj[0];
    }
    return wrogowie.sort((a, b) => a.position.distanceTo(me) - b.position.distanceTo(me))[0];
  };
  // "stoi" = brak ruchu > 2 s (Siewca, Kotwa)
  const ruch = {};
  const sledz = () => { for (const e of Object.values(bot.entities)) { if (!PRIOR.has(e.name)) continue;
    const p = ruch[e.id]; if (!p || p.pos.distanceTo(e.position) > 0.3) ruch[e.id] = { pos: e.position.clone(), t: Date.now() };
    e._stoi = Date.now() - ruch[e.id].t > 2000; } };

  const start = async () => {
    await czysc();
    await rcon(`minecraft:effect clear ${nick}`);
    await rcon(`minecraft:tp ${nick} ${S.x} ${S.y} ${S.z - 7} 0 0`); await sleep(1200);
    await rcon(`minecraft:effect give ${nick} minecraft:instant_health 1 10 true`);
    await rcon(`mmocore admin resource-stamina set ${nick} 1000`).catch(() => {});
    await sleep(800);
    await rcon(`minecraft:scoreboard players set ${nick} l1r_dmg 0`);
    zgon = false; for (const k in reakcje) delete reakcje[k];
  };
  const walcz = async (limitMs, koniec) => {
    const t0 = Date.now(); let pakiZdeptane = 0, ostatniCel = null;
    while (Date.now() - t0 < limitMs && !zgon) {
      sledz();
      const c = cel();
      if (!c) { if (await koniec()) break; await sleep(300); continue; }
      if (Date.now() >= reakcjaDo) {
        if (jestPak(c)) { if (ostatniCel !== c.id) pakiZdeptane++; bot.pvp.forceStop(); await bot.lookAt(c.position.offset(0, 0.5, 0), true);
          if (c.position.distanceTo(bot.entity.position) > 2.8) { bot.setControlState('forward', true); await sleep(250); bot.clearControlStates(); } else bot.attack(c); }
        else if (bot.pvp.target !== c) bot.pvp.attack(c);
        if (process.argv[6] === "debug" && ostatniCel !== c.id) console.log("CEL", c.name, jestPak(c), r1(c.position.distanceTo(bot.entity.position)));
        ostatniCel = c.id;
      }
      await sleep(150);
    }
    bot.pvp.forceStop(); bot.clearControlStates();
    return { czas_s: r1((Date.now() - t0) / 1000), pakiZdeptane };
  };

  if (TRYB === 'walka') {
    const wyn = [];
    for (let i = 0; i < POWT; i++) {
      await start();
      await rcon(`mm mobs spawn ${MOB} 1 world,${S.x},${S.y},${S.z + 6}`); await sleep(1000);
      // koniec: brak wrogow i pakow w arenie przez 6 s (paki moga jeszcze dojrzec)
      let pusto = 0;
      const r = await walcz(MOB === 'L1R_SOWIEDZ' ? 120000 : 75000, async () => (++pusto > 8 && await ile(`@e[type=!player,type=!item,type=!armor_stand,${BOX}]`) === 0));
      const pozostale = await ile(`@e[type=!player,type=!item,type=!armor_stand,tag=!l1r_pak,tag=!l1r_pak_duzy,${BOX}]`);
      const x = { mob: MOB, styl: STYL, ...r, hp_strata: await hpScore(nick), zgon, zabity: !zgon && pozostale === 0, reakcje: { ...reakcje } };
      wyn.push(x); pomiar(`walka-${i}`, x);
      if (zgon) { await sleep(2500); }
    }
    const sr = k => r1(wyn.reduce((s, x) => s + x[k], 0) / wyn.length);
    pomiar('srednia', { mob: MOB, styl: STYL, n: wyn.length, czas_s: sr('czas_s'), hp_strata: sr('hp_strata'), zgony: wyn.filter(x => x.zgon).length });
    w.sprawdz('walki', wyn.length === POWT, `${wyn.length}/${POWT}`);
    if (Object.keys(nieznane).length) console.log('NIEZNANE DZWIEKI', JSON.stringify(nieznane));
  }

  if (TRYB === 'pak') {
    // nikt nie walczy: bot stoi za murem obserwacji (spectator nie: paki MM moga liczyc tylko survival)
    const poz = `${S.x},${S.y},${S.z + 4}`;
    const pakiM = `@e[tag=l1r_pak,${BOX}]`, pakiD = `@e[tag=l1r_pak_duzy,${BOX}]`;
    const pniaczki = `@e[type=husk,nbt={IsBaby:1b},${BOX}]`, korzen = `@e[type=armor_stand,nbt={Marker:1b},${BOX}]`;
    const zabij = async typ => { await rcon(`mm mobs spawn ${typ} 1 world,${poz}`); await sleep(600); await rcon(`minecraft:kill @e[type=!player,type=!armor_stand,tag=!l1r_pak,tag=!l1r_pak_duzy,${BOX}]`); await sleep(500); };
    await start(); await rcon(`minecraft:tp ${nick} ${S.x} ${S.y} ${S.z - 10}`);
    // 1. maly polegly -> 1 pak; nietkniety po 5 s -> Pniaczek, paka nie ma
    await zabij('L1R_WILK');
    const p1 = await ile(pakiM); await sleep(6000);
    const p1po = await ile(pakiM), n1 = await ile(pniaczki);
    w.sprawdz('pak-po-smierci', p1 === 1, `paki ${p1}`);
    w.sprawdz('pak-dojrzewa-pniaczek', p1po === 0 && n1 === 1, `paki po 6 s ${p1po}, pniaczki ${n1}`);
    // 2. zdeptany pak -> znika, nic nie wyrasta (czy opozniony lancuch umiera z pakiem)
    await czysc(); await zabij('L1R_WILK');
    await rcon(`minecraft:tp ${nick} ${S.x} ${S.y} ${S.z + 2} 0 0`); await sleep(800);
    const pak = Object.values(bot.entities).find(e => jestPak(e) && wArenie(e));
    if (pak) { await bot.lookAt(pak.position.offset(0, 0.4, 0), true); bot.attack(pak); await sleep(400); }
    const p2 = await ile(pakiM); await sleep(6000); const n2 = await ile(pniaczki);
    w.sprawdz('pak-zdeptany', !!pak && p2 === 0 && n2 === 0, `widzial ${!!pak}, paki po ciosie ${p2}, pniaczki po 6 s ${n2}`);
    // 3. pak Siewcy -> 3 Pniaczki
    await czysc(); await rcon(`minecraft:tp ${nick} ${S.x} ${S.y} ${S.z - 10}`); await zabij('L1R_SIEWCA'); await sleep(6000);
    const n3 = await ile(pniaczki);
    w.sprawdz('pak-siewcy-3', n3 === 3, `pniaczki ${n3}`);
    // 4. duzy pak -> Korzen (marker), spowalnia bota stojacego w nim
    await czysc(); await zabij('L1R_KORZENIAK');
    const d4 = await ile(pakiD); await rcon(`minecraft:tp ${nick} ${S.x} ${S.y} ${S.z + 4.5}`); await sleep(5600);
    const k4 = await ile(korzen); await sleep(800);
    const wolny = /Slowness|slowness/.test(await rcon(`minecraft:data get entity ${nick} active_effects`));
    w.sprawdz('pak-duzy-korzen', d4 === 1 && k4 === 1 && wolny, `duze paki ${d4}, korzen ${k4}, spowolniony ${wolny}`);
    await sleep(8500); const k4po = await ile(korzen);
    w.sprawdz('korzen-8s', k4po === 0, `korzen po ~9 s: ${k4po}`);
    // 5. Kotwa budzi Pniaczka co 10 s
    await czysc(); await rcon(`minecraft:tp ${nick} ${S.x} ${S.y} ${S.z - 10}`);
    await rcon(`mm mobs spawn L1R_KOTWA 1 world,${poz}`); await sleep(10800);
    const n5 = await ile(pniaczki);
    w.sprawdz('kotwa-budzi', n5 === 1, `pniaczki po 10,8 s: ${n5}`);
  }

  if (TRYB === 'garda') {
    // Korowiec z NoAI-podobnym unieruchomieniem (slowness 255) - cios z przodu i z tylu, ubytek HP z data get
    const hpK = async () => +(/([\d.]+)f/.exec(await rcon(`minecraft:execute in minecraft:overworld run minecraft:data get entity @e[type=zombie,limit=1,sort=nearest,x=300.5,y=-59,z=300.5,distance=..2] Health`)) || [0, NaN])[1];
    const wynik = {};
    for (const [strona, dz] of [['przod', 2.2], ['tyl', -2.2]]) {
      await start();
      await rcon(`mm mobs spawn L1R_KOROWIEC 1 world,${S.x},${S.y},${S.z}`); await sleep(800);
      await rcon(`minecraft:execute in minecraft:overworld as @e[type=zombie,limit=1,${BOX}] run minecraft:data merge entity @s {NoAI:1b,Rotation:[0f,0f]}`);
      await rcon(`minecraft:tp ${nick} ${S.x} ${S.y} ${S.z + dz}`); await sleep(800);   // Rotation 0 = patrzy na +Z
      const ciosy = [];
      for (let i = 0; i < 4; i++) {
        const mob = Object.values(bot.entities).find(e => e.name === 'zombie' && wArenie(e));
        const a = await hpK(); await bot.lookAt(mob.position.offset(0, 1.4, 0), true); bot.attack(mob); await sleep(900);
        ciosy.push(r1(a - await hpK()));
      }
      wynik[strona] = ciosy;
    }
    pomiar('garda', wynik);
    const sp = x => x.reduce((s, v) => s + v, 0) / x.length;
    // z przodu Korowiec i tak zadaje CIOS (bot w zasiegu 4,5) -> 1,5 s odslony: wtedy pelny cios (zamierzone)
    const tyl = sp(wynik.tyl), zGarda = wynik.przod.filter(v => v <= 0.3 * tyl), odsl = wynik.przod.filter(v => v >= 0.6 * tyl);
    w.sprawdz('garda-przod-20%', zGarda.length >= 2 && zGarda.every(v => v > 0), `ciosy w garde ${JSON.stringify(zGarda)} vs tyl sr. ${r1(tyl)}`);
    // odslona celowana: czekaj na zapowiedz CIOSU (1.8/0.6), 1,2 s (zamach 1 s) i bij z przodu
    await start(); await rcon(`mm mobs spawn L1R_KOROWIEC 1 world,${S.x},${S.y},${S.z}`); await sleep(800);
    await rcon(`minecraft:execute in minecraft:overworld as @e[type=zombie,limit=1,${BOX}] run minecraft:data merge entity @s {NoAI:1b,Rotation:[0f,0f]}`);
    await rcon(`minecraft:tp ${nick} ${S.x} ${S.y} ${S.z + 2.2}`); await sleep(800);
    const odsl2 = [];
    for (let i = 0; i < 3; i++) {
      const t = await new Promise(res => { const f = (n, p, v, pi) => { if ((+v).toFixed(1) === '1.8' && (+pi).toFixed(1) === '0.6') { bot.off('soundEffectHeard', f); res(true); } };
        bot.on('soundEffectHeard', f); setTimeout(() => { bot.off('soundEffectHeard', f); res(false); }, 9000); });
      if (!t) continue; await sleep(1200);
      const mob = Object.values(bot.entities).find(e => e.name === 'zombie' && wArenie(e));
      const a = await hpK(); await bot.lookAt(mob.position.offset(0, 1.4, 0), true); bot.attack(mob); await sleep(500);
      odsl2.push(r1(a - await hpK()));
    }
    pomiar('odslona', odsl2);
    w.sprawdz('odslona-po-ciosie', odsl2.length >= 2 && odsl2.every(v => v >= 0.6 * tyl), `ciosy z przodu 1,2 s po zapowiedzi ${JSON.stringify(odsl2)} vs tyl ${r1(tyl)}`);
  }
  await czysc();
}, { limitMs: Number(process.env.LIMIT_MS || 900000) });
