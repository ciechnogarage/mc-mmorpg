'use strict';
// Eval strony trafienia i bloku wielowarstwowego (AnimacjeBroni: strona, tarcza, Wytrzymalosc.blok) na klonie.
//   run.sh <klon> <ten plik> a|b
//   a = gracz bije moba (husk bez AI, 1000 HP) z przodu/boku/tylu: x1,0 / x1,1 / x1,25; sztylet w plecy x2 (nie x2,5)
//   b = gracz z tarcza (TARCZA_DREWNIANA, 70% z przodu, 30% z boku) pod ciosami huska z przodu/boku/tylu,
//       potem z przodu przy 10% staminy (blok x0,4) i bez staminy (garda peka), na koniec belt pillagera (blok x0,5)
//   c = garda mieczem (Shift + PPM, 40%, tylko przod do 60 st.): husk z przodu blokowany, z boku nie
//   d = PvP: drugi bot (SERAFIN, BULAWA_NOWICJUSZA) bije z przodu gracza z OBSIDIAN_SHIELD (85% z przodu)
//       -> blok obciety do limit_bloku_pvp 75%, koszt staminy bloku obuchem x2 (BLOK_STAMINA obuch=true)
// Liczby z logu klona (debug: true w plugins/AnimacjeBroni/config.yml + animacjebroni reload): linie "STRONA" i "BLOK_STAMINA"; bot podaje spadki HP po swojej stronie.
// Gracz stoi w (0,5 -59 0,5) i patrzy na poludnie (+Z); moby stawiane wzgledem tego.
const { scenariusz, czekaj, sleep, postac } = require('/bot/lib');

const TRYB = process.argv[2] || 'a';
const NICK = 'BotStr' + TRYB + (Date.now() % 10000);
const X = 0.5, Y = -59, Z = 0.5;
const OW = 'execute in minecraft:overworld run ';
const mediana = (t) => { const s = [...t].sort((a, b) => a - b); return s.length ? s[Math.floor(s.length / 2)] : NaN; };

scenariusz('strona-' + TRYB, async ({ w, polacz, rcon }) => {
  const bot = await polacz({ nick: NICK });
  const wejscie = await postac.wejdz(bot, {});   // klon --z-kreatorem: nowa postac jak gracz
  w.sprawdz('postac', !!wejscie, 'droga ' + (wejscie && wejscie.droga));
  await sleep(1500);
  await rcon(`minecraft:gamemode survival ${NICK}`);
  await rcon(`${OW}teleport ${NICK} ${X} ${Y} ${Z} 0 0`);
  await rcon(`minecraft:attribute ${NICK} minecraft:knockback_resistance base set 1`);
  const trzymaj = async (material) => {
    for (let proba = 0; proba < 6; proba++) {
      if ((await rcon(`minecraft:data get entity ${NICK} SelectedItem.id`)).includes(material)) return true;
      const inv = await rcon(`minecraft:data get entity ${NICK} Inventory`);
      const m = new RegExp(`\\{Slot: (\\d)b, id: "minecraft:${material}"`).exec(inv);
      if (m) { bot.setQuickBarSlot((+m[1] + 1) % 9); await sleep(200); bot.setQuickBarSlot(+m[1]); }
      await sleep(400);
    }
    return false;
  };
  const hpHuska = async () => {
    const m = /([\d.]+)f/.exec(await rcon('minecraft:data get entity @e[type=husk,tag=str_cel,limit=1] Health'));
    return m ? +m[1] : NaN;
  };

  if (TRYB === 'a') {
    // yaw huska: 180 = patrzy na gracza (przod), 90 = bokiem, 0 = plecami
    const seria = async (klasa, typ, id, material, etykieta) => {
      await rcon(`mmocore admin class ${NICK} ${klasa}`);
      await rcon(`minecraft:clear ${NICK}`);
      await rcon(`mi give ${typ} ${id} ${NICK} 1`);
      await sleep(1200);
      const ok = await trzymaj(material);
      w.sprawdz(`${etykieta}-bron`, ok, `${klasa} trzyma ${id}`);
      if (!ok) return null;
      const wynik = {};
      for (const [strona, yaw] of [['przod', 180], ['bok', 90], ['tyl', 0]]) {
        await rcon('minecraft:kill @e[type=husk]');
        await czekaj(() => !Object.values(bot.entities).some(e => e.name === 'husk'), 3000, 100);
        await rcon(`${OW}summon husk ${X} ${Y} ${Z + 2} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["str_cel"],Rotation:[${yaw}f,0f],attributes:[{id:"minecraft:max_health",base:1000}],Health:1000f}`);
        const cel = await czekaj(() => Object.values(bot.entities).find(e => e.name === 'husk'), 5000, 200);
        if (!cel) { w.sprawdz(`${etykieta}-${strona}`, false, 'brak huska'); continue; }
        await bot.lookAt(cel.position.offset(0, 1.6, 0), true);
        const spadki = [];
        for (let i = 0; i < 4; i++) {
          await sleep(1300);   // pelne naladowanie
          const h0 = await hpHuska(); bot.attack(cel); await sleep(500); const h1 = await hpHuska();
          if (h0 - h1 > 0) spadki.push(h0 - h1);
        }
        wynik[strona] = mediana(spadki);
        w.sprawdz(`${etykieta}-${strona}`, spadki.length >= 3, `spadki HP ${spadki.map(v => v.toFixed(2)).join(', ')} -> mediana ${wynik[strona].toFixed(2)}`);
      }
      return wynik;
    };
    const m = await seria('BASTION', 'SWORD', 'MIECZ_ZELAZNY', 'iron_sword', 'miecz');
    if (m) {
      const bok = m.bok / m.przod, tyl = m.tyl / m.przod;
      w.sprawdz('miecz-bok-x1.1', Math.abs(bok - 1.1) < 0.06, `bok/przod = ${bok.toFixed(3)}`);
      w.sprawdz('miecz-tyl-x1.25', Math.abs(tyl - 1.25) < 0.06, `tyl/przod = ${tyl.toFixed(3)}`);
    }
    const s = await seria('JASTRZAB', 'DAGGER', 'SZTYLET_REKRUTA', 'iron_sword', 'sztylet');
    if (s) {
      const tyl = s.tyl / s.przod;
      w.sprawdz('sztylet-plecy-x2-nie-x2.5', Math.abs(tyl - 2.0) < 0.1, `tyl/przod = ${tyl.toFixed(3)} (x2,5 = mnozniki nalozone)`);
    }
    await rcon('minecraft:kill @e[type=husk]');
  }

  if (TRYB === 'c') {
    await rcon(`mmocore admin class ${NICK} BASTION`);
    await rcon(`minecraft:clear ${NICK}`);
    await rcon(`mi give SWORD MIECZ_ZELAZNY ${NICK} 1`);
    await sleep(1200);
    w.sprawdz('miecz', await trzymaj('iron_sword'), 'BASTION trzyma MIECZ_ZELAZNY');
    for (const [etykieta, dx, dz] of [['przod', 0, 1.4], ['bok', 1.4, 0]]) {
      await rcon('minecraft:kill @e[type=husk]');
      await rcon(`minecraft:effect give ${NICK} minecraft:instant_health 1 5 true`);
      await sleep(1200);
      bot.look(Math.PI, 0, true);
      bot.setControlState('sneak', true); await sleep(300);
      bot.activateItem(); await sleep(500);
      await rcon(`${OW}summon husk ${X + dx} ${Y} ${Z + dz} {Silent:1b,PersistenceRequired:1b,Tags:["str_bije"],attributes:[{id:"minecraft:attack_damage",base:10},{id:"minecraft:movement_speed",base:0},{id:"minecraft:knockback_resistance",base:1}]}`);
      await sleep(3000);
      bot.deactivateItem(); bot.setControlState('sneak', false);
      w.sprawdz(`garda-${etykieta}`, true, 'liczby w logu: STRONA ... blok=');
    }
    await rcon('minecraft:kill @e[type=husk]');
  }

  if (TRYB === 'd') {
    const NICK2 = 'BotStrB' + (Date.now() % 10000);
    await rcon(`mmocore admin class ${NICK} BASTION`);
    await rcon(`minecraft:clear ${NICK}`);
    await rcon(`mi give SHIELD OBSIDIAN_SHIELD ${NICK} 1`);
    await sleep(1200);
    await rcon(`minecraft:item replace entity ${NICK} weapon.offhand from entity ${NICK} weapon.mainhand`);
    await rcon(`minecraft:item replace entity ${NICK} weapon.mainhand with minecraft:air`);
    await sleep(8000);   // drugi bot nie zaraz po pierwszym (kreator ciezki, MythicLib "Ghost session")
    const b2 = await polacz({ nick: NICK2 });
    const wej2 = await postac.wejdz(b2, {});
    w.sprawdz('bot2-postac', !!wej2, 'droga ' + (wej2 && wej2.droga));
    await sleep(1500);
    await rcon(`minecraft:gamemode survival ${NICK2}`);
    await rcon(`mmocore admin class ${NICK2} SERAFIN`);
    await rcon(`minecraft:clear ${NICK2}`);
    await rcon(`mi give HAMMER BULAWA_NOWICJUSZA ${NICK2} 1`);
    await rcon(`${OW}teleport ${NICK2} ${X} ${Y} ${Z + 2.2} 180 0`);
    await sleep(1500);
    let ok2 = false;
    for (let proba = 0; proba < 6 && !ok2; proba++) {
      const sel = await rcon(`minecraft:data get entity ${NICK2} SelectedItem.id`);
      ok2 = !/no elements|has no/.test(sel) && /minecraft:/.test(sel);
      if (!ok2) {
        const m = /\{Slot: (\d)b, id: "minecraft:([a-z_]+)"/.exec(await rcon(`minecraft:data get entity ${NICK2} Inventory`));
        if (m) { b2.setQuickBarSlot((+m[1] + 1) % 9); await sleep(200); b2.setQuickBarSlot(+m[1]); }
        await sleep(400);
      }
    }
    w.sprawdz('bot2-bulawa', ok2, 'trzyma: ' + (await rcon(`minecraft:data get entity ${NICK2} SelectedItem.id`)));
    await rcon(`minecraft:effect give ${NICK} minecraft:instant_health 1 5 true`);
    bot.look(Math.PI, 0, true);
    bot.activateItem(true); await sleep(600);
    // UWAGA: world ma w WorldGuard __global__ pvp: deny -> przed trybem d na klonie
    //   rg flag -w world __global__ pvp allow   (po tescie: ... pvp deny)
    const ofiara = bot.entities ? Object.values(b2.entities).find(e => e.username === NICK) : null;
    // bot.health (MMOCore odnawia HP) ani entityHurt (czesciowy blok tarcza nie wysyla damage_event) nie mierza
    // -> statystyki serwera w dziesiatych czesciach: damage_taken / damage_blocked_by_shield
    await rcon('minecraft:scoreboard objectives add str_dt minecraft.custom:minecraft.damage_taken');
    await rcon('minecraft:scoreboard objectives add str_db minecraft.custom:minecraft.damage_blocked_by_shield');
    const wynik = async (o) => +((/has (\d+)/.exec(await rcon(`minecraft:scoreboard players get ${NICK} ${o}`)) || [0, 0])[1]);
    const dt0 = await wynik('str_dt'), db0 = await wynik('str_db');
    for (let i = 0; i < 4 && ofiara; i++) {
      await b2.lookAt(ofiara.position.offset(0, 1.5, 0), true);
      await sleep(1400); b2.attack(ofiara);
    }
    await sleep(600);
    bot.deactivateItem();
    const dt = (await wynik('str_dt')) - dt0, db = (await wynik('str_db')) - db0;
    // ciosy przechodza przez tarcze (pvp deny -> oba 0). damage_blocked_by_shield liczy od obrazen sprzed
    // MythicLib (25.10: 18,8 przy 4 ciosach po 4,1) -> proporcje 75% sprawdzaj w logu: STRONA final/dmg = 0,25
    w.sprawdz('pvp-ciosy', !!ofiara && db > 0 && dt > 0, `przyjete ${dt / 10}, blok ${db / 10}; log: STRONA pvp=true final/dmg, BLOK_STAMINA obuch=true`);
    b2.quit();
  }

  if (TRYB === 'b') {
    await rcon(`mmocore admin class ${NICK} BASTION`);
    await rcon(`minecraft:clear ${NICK}`);
    await rcon(`mi give SHIELD TARCZA_DREWNIANA ${NICK} 1`);
    await sleep(1200);
    await rcon(`minecraft:item replace entity ${NICK} weapon.offhand from entity ${NICK} weapon.mainhand`);
    await rcon(`minecraft:item replace entity ${NICK} weapon.mainhand with minecraft:air`);
    await sleep(800);
    const warstwa = async (n) => (await rcon(`minecraft:data get entity ${NICK} equipment.offhand.components."minecraft:blocks_attacks".damage_reductions[${n}]`)).replace(/.*data: /, '');
    const w0 = await warstwa(0), w1 = await warstwa(1);
    w.sprawdz('tarcza-warstwy', /horizontal_blocking_angle: 60\.0f/.test(w0) && /factor: 0\.39/.test(w0)
      && /horizontal_blocking_angle: 120\.0f/.test(w1) && /factor: 0\.3[0f]/.test(w1), `przod ${w0} | bok ${w1}`);
    const maks = +(/\/(\d+)/.exec(await rcon(`animacjebroni stamina gracz ${NICK}`)) || [0, 0])[1];
    let ciosy = [];
    bot.on('health', () => { ciosy.push(bot.health); });

    const pod = async (etykieta, dx, dz, staminaUlamek, ms = 4500) => {
      await rcon('minecraft:kill @e[type=husk]');
      await rcon(`mmocore admin resource-stamina set ${NICK} ${Math.round(maks * staminaUlamek * 100) / 100}`);
      await rcon(`minecraft:effect give ${NICK} minecraft:instant_health 1 5 true`);
      await sleep(1200);
      bot.look(Math.PI, 0, true);   // mineflayer: yaw PI = patrzy na +Z
      bot.activateItem(true); await sleep(400);
      ciosy = [];
      await rcon(`${OW}summon husk ${X + dx} ${Y} ${Z + dz} {Silent:1b,PersistenceRequired:1b,Tags:["str_bije"],attributes:[{id:"minecraft:attack_damage",base:10},{id:"minecraft:movement_speed",base:0},{id:"minecraft:knockback_resistance",base:1}]}`);
      await sleep(ms);
      bot.deactivateItem();
      await rcon('minecraft:kill @e[type=husk]');
      w.sprawdz(`blok-${etykieta}`, ciosy.length > 0, `HP po ciosach: ${ciosy.map(v => v.toFixed(1)).join(', ')} (log: STRONA/BLOK_STAMINA)`);
      await sleep(1500);
    };
    await pod('przod', 0, 1.4, 1);
    await pod('bok', 1.4, 0, 1);
    await pod('tyl', 0, -1.4, 1, 2200);
    await pod('przod-stamina10', 0, 1.4, 0.1, 2500);
    await pod('przod-stamina0', 0, 1.4, 0, 2500);
    // belt: pillager z kusza 6 blokow z przodu
    await rcon(`mmocore admin resource-stamina set ${NICK} ${maks}`);
    await rcon(`minecraft:effect give ${NICK} minecraft:instant_health 1 5 true`);
    await sleep(2500);   // garda po pekniecu wraca (cooldown)
    bot.look(Math.PI, 0, true);
    bot.activateItem(true); await sleep(400);
    ciosy = [];
    await rcon(`${OW}summon pillager ${X} ${Y} ${Z + 7} {Silent:1b,PersistenceRequired:1b,Tags:["str_bije"],Rotation:[180f,0f],HandItems:[{id:"minecraft:crossbow",count:1}],equipment:{mainhand:{id:"minecraft:crossbow",count:1}},attributes:[{id:"minecraft:movement_speed",base:0}]}`);
    await sleep(8000);
    bot.deactivateItem();
    await rcon('minecraft:kill @e[type=pillager]');
    await rcon('minecraft:kill @e[type=arrow]');
    w.sprawdz('blok-belt', ciosy.length > 0, `HP po beltach: ${ciosy.map(v => v.toFixed(1)).join(', ')}`);
  }
}, { limitMs: 240000 });
