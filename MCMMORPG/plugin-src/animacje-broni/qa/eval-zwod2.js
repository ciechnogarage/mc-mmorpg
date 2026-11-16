'use strict';
// Zwod v2: seria (koszt x2), zwrot A/D po zwodzie (szybsze ladowanie), reakcje mobow MM na sygnal ZAMACH.
//   run.sh <klon> qa/eval-zwod2.js <tryb> [proby]   (klon --z-kreatorem, debug: true; liczby z linii ZWOD/MOCNY/SYGNAL w logu klona)
// seria: 3 zwody w ~2 s (koszt rosnie, swist przy kazdym), pauza 4 s, zwod (koszt jak pierwszy), potem bez staminy: ani zwodu, ani ciosu.
// zwrot: ladowanie 0,45 s po zwodzie + A (oczekiwane naladowanie 12 = pelne) vs to samo bez A (9) - obrazenia zwrot > bez.
// pokaz <pary>: zwod i mocny cios na zmiane w huska - do nagrania klientem QA (klient.sh nagraj).
// moby: L001_KORZENIAK (garda: Odpornosc), L001_WILK (odskok: dystans rosnie), L001_STARY_KORZEN (kontra: bot traci HP
//   przed puszczeniem PPM); drugi zamach 1,5 s po pierwszym = odnowienie (brak reakcji).
const { scenariusz, czekaj, sleep, postac } = require('/bot/lib');
const TRYB = process.argv[2] || 'seria';
const PROBY = +(process.argv[3] || 3);
const NICK = 'BotZv' + (Date.now() % 10000);
const X = 0.5, Y = -59, Z = 0.5;
const OW = 'execute in minecraft:overworld run ';

scenariusz('zwod2-' + TRYB, async ({ w, polacz, rcon }) => {
  const bot = await polacz({ nick: NICK });
  const wejscie = await postac.wejdz(bot, {});
  w.sprawdz('postac', !!wejscie, 'droga ' + (wejscie && wejscie.droga));
  await sleep(1500);
  await rcon(`minecraft:gamemode survival ${NICK}`);
  await rcon(`${OW}fill ${X - 6} ${Y - 1} ${Z - 6} ${X + 6} ${Y - 1} ${Z + 8} minecraft:stone`);
  await rcon(`${OW}teleport ${NICK} ${X} ${Y} ${Z} 0 0`);
  await rcon(`minecraft:attribute ${NICK} minecraft:knockback_resistance base set 1`);
  const stamina = async () => parseFloat((await rcon(`papi parse ${NICK} %mmocore_stamina%`)).replace(',', '.'));
  const zdrowie = async (sel) => +(/([\d.]+)f/.exec(await rcon(`minecraft:data get entity ${sel} Health`)) || [0, NaN])[1];
  const trzymaj = async (material) => {
    for (let p = 0; p < 6; p++) {
      if ((await rcon(`minecraft:data get entity ${NICK} SelectedItem.id`)).includes(material)) return true;
      const inv = await rcon(`minecraft:data get entity ${NICK} Inventory`);
      const m = new RegExp(`\\{Slot: (\\d)b, id: "minecraft:${material}"`).exec(inv);
      if (m) { bot.setQuickBarSlot((+m[1] + 1) % 9); await sleep(200); bot.setQuickBarSlot(+m[1]); }
      await sleep(400);
    }
    return false;
  };
  await rcon(`mmocore admin class ${NICK} BASTION`);
  await sleep(1500);   // pasek umiejetnosci MMOCore ~1 s po zmianie klasy (16.11)
  await rcon(`minecraft:clear ${NICK}`);
  await rcon(`mi give SWORD MIECZ_ZELAZNY ${NICK} 1`);
  await sleep(1200);
  if (!w.sprawdz('miecz w rece', await trzymaj('iron_sword'), 'BASTION')) return;

  // Jeden zwod: PPM, Shift po 150 ms, puszczenie PPM po 500 ms, Shift w gore.
  const zwod = async () => {
    bot.activateItem(); await sleep(150);
    bot.setControlState('sneak', true); await sleep(350);
    bot.deactivateItem(); await sleep(100);
    bot.setControlState('sneak', false); await sleep(100);
  };

  if (TRYB === 'pokaz') {   // do nagrania klientem QA (widz z boku): na zmiane zwod i pelny mocny cios w huska, PROBY par co ~5 s
    await rcon(`${OW}summon husk ${X} ${Y} ${Z + 2.2} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["zv"],attributes:[{id:"minecraft:max_health",base:5000}],Health:5000f}`);
    const cel = await czekaj(() => Object.values(bot.entities).find(e => e.name === 'husk'), 5000, 200);
    await bot.lookAt(cel.position.offset(0, 1.2, 0), true);
    await rcon(`say POKAZ_START ${NICK}`);
    for (let i = 0; i < PROBY; i++) {
      await rcon(`mmocore admin resource-stamina set ${NICK} 20`);
      await sleep(2000); await zwod();
      await sleep(2500); bot.activateItem(); await sleep(750); bot.deactivateItem();
    }
    await sleep(1000);
    await rcon('minecraft:kill @e[type=husk,tag=zv]');
  }

  if (TRYB === 'seria') {
    for (let i = 0; i < PROBY; i++) {
      await rcon(`mmocore admin resource-stamina set ${NICK} 20`);
      await sleep(4000);   // poza oknem serii poprzedniej proby
      const s0 = await stamina();
      // swist zwodu: ATTACK_NODAMAGE v=0,45 p=1,35 (AnimacjeBroni); po v/p, nazwy dzwiekow mineflayer 4.39 != 1.21.11
      let swisty = 0; const sluch = (n, pos, v, p) => { if (Math.abs(v - 0.45) < 0.02 && Math.abs(p - 1.35) < 0.04) swisty++; };
      bot.on('soundEffectHeard', sluch);
      for (let k = 0; k < 3; k++) await zwod();   // ~0,7 s na zwod -> wszystkie w 60 t
      bot.removeListener('soundEffectHeard', sluch);
      w.sprawdz(`seria ${i}: swist przy kazdym zwodzie`, swisty === 3, `swisty ${swisty}/3`);
      const s1 = await stamina();
      w.sprawdz(`seria ${i}: 3 zwody`, s0 - s1 > 3 * 0.5, `stamina ${s0} -> ${s1}`);   // bez serii 3 x ~0,7; z seria ~0,7+1,4+2,8
      await sleep(4000);
      const s2 = await stamina();
      await zwod();
      const s3 = await stamina();
      w.sprawdz(`seria ${i}: po pauzie koszt od nowa`, s2 - s3 > 0 && s2 - s3 < (s0 - s1) / 3, `stamina ${s2} -> ${s3}`);
    }
    // bez staminy: zwod odmowiony, ladowanie trwa -> cios w huska
    await rcon(`${OW}summon husk ${X} ${Y} ${Z + 2.2} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["zv"],attributes:[{id:"minecraft:max_health",base:5000}],Health:5000f}`);
    const cel = await czekaj(() => Object.values(bot.entities).find(e => e.name === 'husk'), 5000, 200);
    await bot.lookAt(cel.position.offset(0, 1.2, 0), true);
    await sleep(2000);
    await rcon(`mmocore admin resource-stamina set ${NICK} 0.2`);
    const h0 = await zdrowie('@e[type=husk,tag=zv,limit=1]');
    bot.activateItem(); await sleep(150); bot.setControlState('sneak', true); await sleep(550);
    bot.deactivateItem(); await sleep(400); bot.setControlState('sneak', false);
    const h1 = await zdrowie('@e[type=husk,tag=zv,limit=1]');
    // mocny (12 L) drozszy od zwodu -> bez staminy na zwod nie ma tez ciosu; rozroznienie tylko w logu: ZWOD wynik=brak_staminy, MOCNY wariant=brak_staminy (16.11)
    w.sprawdz('bez staminy: ani zwodu, ani ciosu', h0 - h1 === 0, `husk ${h0} -> ${h1} (log: ZWOD wynik=brak_staminy)`);
    await rcon('minecraft:kill @e[type=husk,tag=zv]');
  }

  if (TRYB === 'zwrot') {
    await rcon(`${OW}summon husk ${X} ${Y} ${Z + 2.2} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["zv"],attributes:[{id:"minecraft:max_health",base:5000}],Health:5000f}`);
    const cel = await czekaj(() => Object.values(bot.entities).find(e => e.name === 'husk'), 5000, 200);
    await bot.lookAt(cel.position.offset(0, 1.2, 0), true);
    const wyn = { zwrot: [], bez: [] };
    for (let i = 0; i < PROBY; i++) {
      for (const wariant of ['zwrot', 'bez']) {
        await rcon(`mmocore admin resource-stamina set ${NICK} 20`);
        await rcon(`${OW}teleport ${NICK} ${X} ${Y} ${Z} 0 0`);
        await bot.lookAt(cel.position.offset(0, 1.2, 0), true);
        await sleep(4500);   // krwawienie poprzedniego ciosu + okno serii
        const h0 = await zdrowie('@e[type=husk,tag=zv,limit=1]');
        bot.activateItem(); await sleep(150);
        bot.setControlState('sneak', true); await sleep(100);
        bot.deactivateItem();
        bot.setControlState('sneak', false);
        // mineflayer 4.39 wysyla w player_input tylko shift (physics.js) -> klawisz A pakietem recznie
        if (wariant === 'zwrot') { bot._client.write('player_input', { inputs: { left: true } }); await sleep(150);
          bot._client.write('player_input', { inputs: {} }); }
        else await sleep(150);
        await sleep(100);
        bot.activateItem(); await sleep(450);   // 9 t: bez zwrotu postep 0,5, ze zwrotem 12 t = pelne
        bot.deactivateItem();
        await sleep(500);
        const h1 = await zdrowie('@e[type=husk,tag=zv,limit=1]');
        wyn[wariant].push(+(h0 - h1).toFixed(2));
      }
    }
    const sr = t => t.reduce((a, b) => a + b, 0) / t.length;
    w.sprawdz('zwrot: cios mocniejszy niz bez zwrotu', wyn.zwrot.every(d => d > 0) && sr(wyn.zwrot) > sr(wyn.bez) * 1.05,
      `zwrot ${wyn.zwrot.join(' ')} | bez ${wyn.bez.join(' ')}`);
    await rcon('minecraft:kill @e[type=husk,tag=zv]');
  }

  if (TRYB === 'moby') {
    await rcon('minecraft:gamerule spawn_monsters true');
    // boss i wilk bija bota zwyklymi ciosami; smierc = boss gubi cel i nastepne sygnaly ida w pustke (16.11: 2/3 FAIL kontry)
    await rcon(`minecraft:effect give ${NICK} minecraft:resistance infinite 2 true`);
    // odrzut kontry = pakiet predkosci od serwera dla bota (max poziomej, bloki/tick); pozycja bota liczona klientem bywa myląca
    let vMax = 0;
    bot._client.on('entity_velocity', (p) => { if (bot.entity && p.entityId === bot.entity.id) {
      const v = p.velocity || p; const h = Math.hypot(v.x, v.z); const hn = h > 50 ? h / 8000 : h; if (hn > vMax) vMax = hn; } });
    // cios kontry rozpoznawany po dzwieku (ravager.attack v=1.2 p=1.1): boss ma wlasny SLAM z throw co 50 t i jego odrzut
    // myli pomiar predkosci (16.11: v2=0.6 bez kontry); nazwy dzwiekow w mineflayer 4.39 nie pasuja do 1.21.11 -> po v/p
    let kontry = 0;
    bot.on('soundEffectHeard', (n, pos, v, p) => { if (Math.abs(v - 1.2) < 0.02 && Math.abs(p - 1.1) < 0.04) kontry++; });
    const sprzataj = async () => { await rcon('mm mobs killall'); await rcon('minecraft:kill @e[type=zombie]'); await rcon('minecraft:kill @e[type=wolf]'); };
    // obrazenia bota tabela damage_taken (dziesiate czesci HP): bot.health stoi przez regen MMOCore (16.11: kontra z dzwiekiem, hp=0)
    await rcon('minecraft:scoreboard objectives add zw_dmg minecraft.custom:minecraft.damage_taken');
    const obrazenia = async () => +((/has (\d+) \[/.exec(await rcon(`minecraft:scoreboard players get ${NICK} zw_dmg`)) || [0, 0])[1]) / 10;
    const MOBY = [['L001_KORZENIAK', 'zombie', 'garda'], ['L001_WILK', 'wolf', 'odskok'], ['L001_STARY_KORZEN', 'zombie', 'kontra']];
    for (const [id, typ, reakcja] of MOBY) {
      await sprzataj();   // samo killall zostawialo moby z poprzedniego przebiegu (16.11: sygnal do 4 mobow)
      await sleep(800);
      await rcon(`${OW}teleport ${NICK} ${X} ${Y} ${Z} 0 0`);
      await rcon(`mm mobs spawn ${id} 1 world,${X},${Y},${Z + 2.5}`);
      const mob = await czekaj(() => Object.values(bot.entities).find(e => e.name === typ), 5000, 200);
      if (!w.sprawdz(`${id} jest`, !!mob, typ)) continue;
      const SEL = `@e[type=${typ},limit=1,sort=nearest,x=${X},y=${Y},z=${Z}]`;
      // lekki cios do namierzenia + mocne ciosy zabijaly Korzeniaka (24 HP) przed 3. proba (16.11: 3. proba same zera)
      // wilk wraca do 8 HP sam (vanilla przelicza max_health dzikiego wilka, 16.11 recznie: 1000 -> 8) -> Odpornosc V
      if (typ === 'wolf') await rcon(`minecraft:effect give ${SEL} minecraft:resistance infinite 4 true`);
      else { await rcon(`minecraft:attribute ${SEL} minecraft:max_health base set 1000`);
        await rcon(`minecraft:data modify entity ${SEL} Health set value 1000f`); }
      const wyniki = [];
      for (let i = 0; i < PROBY; i++) {
        await rcon(`mmocore admin resource-stamina set ${NICK} 20`);
        await rcon(`minecraft:effect give ${NICK} minecraft:instant_health 1 5 true`);
        await rcon('minecraft:kill @e[type=slime]');   // slime'y rodza sie mimo spawn_monsters false (spawn_mobs true na klonie; 16.11 napastnik=SLIME, bot zabity) -> na klonie tez mv gamerule set minecraft:spawn_mobs false world
        await sleep(5500);   // odnowienie reakcji 4-5 s
        await rcon(`${OW}teleport ${NICK} ${X} ${Y} ${Z} 0 0`);
        await rcon(`${OW}teleport ${SEL} ${X} ${Y} ${Z + 2.2} 180 0`);
        await sleep(300);
        await bot.lookAt(mob.position.offset(0, 0.6, 0), true);
        // sygnal idzie tylko do moba, ktory ma gracza za cel (swiezy mob bez celu nie reaguje - 16.11: 8/11, brak linii SYGNAL);
        // lekki cios LPM = mob namierza atakujacego (AITargetSelectors attacker)
        bot.attack(mob); await sleep(900);
        await rcon(`${OW}teleport ${SEL} ${X} ${Y} ${Z + 2.2} 180 0`); await sleep(200);
        await bot.lookAt(mob.position.offset(0, 0.6, 0), true);
        const d0 = bot.entity.position.distanceTo(mob.position), b0 = bot.health;
        await rcon(`minecraft:scoreboard players set ${NICK} zw_dmg 0`);
        vMax = 0; kontry = 0;
        // najwieksze oddalenie w oknie 600 ms co 50 ms: jeden odczyt po 250 ms trafial na powrot/skok wilka (16.11: d=-2,16 przy SYGNAL)
        let dMax = 0; const probki = setInterval(() => { dMax = Math.max(dMax, bot.entity.position.distanceTo(mob.position) - d0); }, 50);
        bot.activateItem();
        await sleep(250);
        const d1 = bot.entity.position.distanceTo(mob.position);
        const efekt = reakcja === 'garda' ? await rcon(`minecraft:data get entity ${SEL} active_effects`) : '';
        await sleep(350);
        const b1 = bot.health, dk = bot.entity.position.distanceTo(mob.position), v1 = +vMax.toFixed(2), k1 = kontry;
        clearInterval(probki);
        bot.deactivateItem();
        const dmg1 = await obrazenia();
        await sleep(300);
        // drugi zamach od razu: reakcja na odnowieniu
        await bot.lookAt(mob.position.offset(0, 0.6, 0), true);
        const e0 = bot.entity.position.distanceTo(mob.position), c0 = bot.health;
        vMax = 0; kontry = 0; bot.activateItem(); await sleep(250);
        const e1 = bot.entity.position.distanceTo(mob.position);
        const efekt2 = reakcja === 'garda' ? await rcon(`minecraft:data get entity ${SEL} active_effects`) : '';
        await sleep(350); const v2 = +vMax.toFixed(2), k2 = kontry; const c1 = bot.health, ek = bot.entity.position.distanceTo(mob.position); bot.deactivateItem();
        wyniki.push({ d: +(d1 - d0).toFixed(2), dk: +(dk - d0).toFixed(2), dk2: +(ek - e0).toFixed(2), hp: +(b0 - b1).toFixed(1), ef: /resistance/.test(efekt),
                      d2: +(e1 - e0).toFixed(2), hp2: +(c0 - c1).toFixed(1), ef2: /resistance/.test(efekt2), v1, v2, k1, k2, dMax: +dMax.toFixed(2), dmg1 });
        await sleep(300);
      }
      const opis = JSON.stringify(wyniki);
      if (reakcja === 'garda') w.sprawdz(`${id} garda`, wyniki.every(r => r.ef), opis);
      if (reakcja === 'odskok') w.sprawdz(`${id} odskok`, wyniki.every(r => r.dMax > 1), opis);
      if (reakcja === 'kontra') w.sprawdz(`${id} kontra (cios + odrzut)`, wyniki.every(r => r.k1 > 0 && r.dmg1 > 0 && r.v1 > 0.3), opis);   // odrzut = throw kontry (zwykly cios moba bez odrzutu: knockback_resistance 1)
      w.sprawdz(`${id} odnowienie (2. zamach bez reakcji)`, wyniki.every(r =>
        reakcja === 'odskok' ? r.d2 < 1 : reakcja === 'kontra' ? r.k2 === 0 : true), opis);
    }
    await sprzataj();
    await rcon(`minecraft:effect clear ${NICK}`);
    await rcon('minecraft:gamerule spawn_monsters false');
    await rcon('minecraft:scoreboard objectives remove zw_dmg');
  }
}, { limitMs: 420000 });
