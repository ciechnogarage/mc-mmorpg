'use strict';
// Zwod: Shift w trakcie ladowania PPM przerywa mocny cios bez obrazen i kosztuje ~0,5 L staminy.
//   run.sh <klon> qa/eval-zwod.js [proby] [tryb] [cel]   (klon --z-kreatorem, debug: true -> linie ZWOD/MOCNY w logu klona)
// tryb mob (domyslny): miecz, sztylet (pelne 12 t), miecz dwureczny (pelne 24 t); warianty: kontrola (PPM bez Shift = cios),
//   zwod (Shift po 0,15 s), zwod_pozny (tylko 24 t: Shift po 0,8 s = 16 t, po pelnym lekkiej, przed pelnym ciezkiej),
//   za_pozno (Shift 0,2 s po pelnym = cios).
// tryb dwie: dwa miecze (DwieBronie, PPM = lewa reka) -> Shift w trakcie PPM NIE jest zwodem (obrazenia > 0 jak kontrola).
// tryb pvp <nick>: cel = gracz <nick> (klient QA) zamiast huska; na klonie rg flag -w world __global__ pvp allow.
// Bot nie sprawdzi gardy po zwodzie: mineflayer nie ponawia uzycia przy trzymanym PPM jak klient vanilla -> klient QA.
const { scenariusz, czekaj, sleep, postac } = require('/bot/lib');
const PROBY = +(process.argv[2] || 5);
const TRYB = process.argv[3] || 'mob';
const CEL = process.argv[4] || null;
const NICK = 'BotZw' + (Date.now() % 10000);
const X = 0.5, Y = -59, Z = 0.5;
const OW = 'execute in minecraft:overworld run ';

scenariusz('zwod', async ({ w, polacz, rcon }) => {
  const bot = await polacz({ nick: NICK });
  const wejscie = await postac.wejdz(bot, {});
  w.sprawdz('postac', !!wejscie, 'droga ' + (wejscie && wejscie.droga));
  await sleep(1500);
  await rcon(`minecraft:gamemode survival ${NICK}`);
  await rcon(`${OW}fill ${X - 4} ${Y - 1} ${Z - 4} ${X + 4} ${Y - 1} ${Z + 6} minecraft:stone`);
  await rcon(`${OW}teleport ${NICK} ${X} ${Y} ${Z} 0 0`);
  await rcon(`minecraft:attribute ${NICK} minecraft:knockback_resistance base set 1`);
  const SEL = TRYB === 'pvp' ? CEL : '@e[type=husk,tag=zw,limit=1]';
  const hp = async () => +(/([\d.]+)f/.exec(await rcon(`minecraft:data get entity ${SEL} Health`)) || [0, NaN])[1];
  const stamina = async () => parseFloat((await rcon(`papi parse ${NICK} %mmocore_stamina%`)).replace(',', '.'));
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

  const BRONIE = {
    mob: [['BASTION', 'SWORD', 'MIECZ_ZELAZNY', 'iron_sword', 12], ['JASTRZAB', 'DAGGER', 'SZTYLET_REKRUTA', 'iron_sword', 12],
          ['RYCERZ_SMIERCI', 'GREATSWORD', 'PRZ_WOJ_MIECZ_DWURECZNY_1', 'iron_sword', 24]],
    dwie: [['BASTION', 'SWORD', 'MIECZ_ZELAZNY', 'iron_sword', 12]],
    pvp: [['BASTION', 'SWORD', 'MIECZ_ZELAZNY', 'iron_sword', 12]],
  }[TRYB];
  for (const [klasa, typ, id, material, pelne] of BRONIE) {
    await rcon(`mmocore admin class ${NICK} ${klasa}`);
    await sleep(1500);   // zapas po zmianie klasy (tagi arch_* i pasek MMOCore przychodza 0-2 s pozniej); PPM to nie blokuje - patrz qa/sonda-klasa.js
    await rcon(`minecraft:clear ${NICK}`);
    await rcon(`mi give ${typ} ${id} ${NICK} 1`);
    await sleep(1200);   // >= 10 t: bron z `mi give` dostaje komponenty (consumable = PPM) dopiero z zegara dopasujDlonie co 10 t
    if (!w.sprawdz(`${id}-bron`, await trzymaj(material), klasa)) continue;
    if (TRYB === 'dwie') {   // druga bron do lewej reki; DwieBronie uzbraja ja przy PPM
      // kopia broni z prawej po stronie serwera: mi give + klikanie okna psulo kursor (pasek 1-4 = umiejetnosci MMOCore, 16.11 3 przebiegi FAIL)
      await rcon(`minecraft:item replace entity ${NICK} weapon.offhand from entity ${NICK} weapon.mainhand`);
      await sleep(1500);
      const wLewej = async () => (await rcon(`minecraft:data get entity ${NICK} equipment.offhand.id`)).includes('iron_sword');
      if (!w.sprawdz('lewa-bron', await wLewej(), 'off-hand')) continue;
    }
    await rcon('minecraft:kill @e[type=husk]');
    await czekaj(() => !Object.values(bot.entities).some(e => e.name === 'husk'), 3000, 100);
    let cel;
    if (TRYB === 'pvp') {
      await rcon(`${OW}teleport ${CEL} ${X} ${Y} ${Z + 2.2} 180 0`);
      await rcon(`minecraft:effect give ${CEL} minecraft:resistance infinite 2 true`);  // -60%: 5 prob x 3 warianty bez smierci
      cel = await czekaj(() => bot.players[CEL] && bot.players[CEL].entity, 5000, 200);
    } else {
      await rcon(`${OW}summon husk ${X} ${Y} ${Z + 2.2} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["zw"],attributes:[{id:"minecraft:max_health",base:5000}],Health:5000f}`);
      cel = await czekaj(() => Object.values(bot.entities).find(e => e.name === 'husk'), 5000, 200);
    }
    await bot.lookAt(cel.position.offset(0, 1.2, 0), true);
    const pelneMs = pelne * 50;
    const warianty = [['zwod', 150, 500], ['kontrola', null, pelneMs + 100], ['za_pozno', pelneMs + 200, pelneMs + 400]];
    if (pelne > 12) warianty.splice(1, 0, ['zwod_pozny', 800, 1000]);
    const wyn = { kontrola: [], zwod: [], zwod_pozny: [], za_pozno: [], koszt: [] };
    for (let i = 0; i < PROBY; i++) {
      for (const [wariant, shiftPo, puscPo] of warianty) {
        await rcon(`mmocore admin resource-stamina set ${NICK} 20`);
        if (TRYB === 'pvp') {   // odrzut poprzedniego ciosu przesuwa gracza (16.11: Z 2,2 -> 6,8, potem pudlo) -> na miejsce przed kazda proba
          await rcon(`${OW}teleport ${CEL} ${X} ${Y} ${Z + 2.2} 180 0`);
          await rcon(`minecraft:effect give ${CEL} minecraft:instant_health 1 5 true`);
          await sleep(500);
        }
        // odnowienie mocnego 20 t + podmiana komponentu po Shift; przed zwodem 3,5 s, bo krwawienie sztyletu z
        // poprzedniego ciosu tyka co 20 t przez ~3 s i wpadaloby w pomiar (16.11: 0,9 HP przy zwodzie)
        await sleep(wariant === 'zwod' ? 3500 : 1600);
        const h0 = await hp(), s0 = await stamina();
        bot.activateItem(TRYB === 'dwie');   // dwie bronie: klient uzywa off-handu (glowna reka bez uzycia), bot musi wskazac sam
        if (shiftPo != null) { await sleep(shiftPo); bot.setControlState('sneak', true); await sleep(puscPo - shiftPo); }
        else await sleep(puscPo);
        bot.deactivateItem();
        await sleep(400);
        const h1 = await hp(), s1 = await stamina();
        bot.setControlState('sneak', false);
        wyn[wariant].push(+(h0 - h1).toFixed(2));
        if (wariant.startsWith('zwod')) wyn.koszt.push(+(s0 - s1).toFixed(2));
      }
    }
    const ile = (t, f) => t.filter(f).length;
    w.sprawdz(`${id} kontrola`, ile(wyn.kontrola, d => d > 0) === PROBY, `obrazenia ${wyn.kontrola.join(' ')}`);
    if (TRYB === 'dwie') {   // dwie bronie: Shift przy trzymanym PPM to podwojny mocny DwieBronie, nie zwod
      w.sprawdz(`${id} dwie: Shift w PPM = cios`, ile(wyn.zwod, d => d > 0) === PROBY, `obrazenia ${wyn.zwod.join(' ')}`);
    } else {
      w.sprawdz(`${id} zwod bez ciosu`, ile(wyn.zwod, d => d === 0) === PROBY, `obrazenia ${wyn.zwod.join(' ')}`);
      if (pelne > 12) w.sprawdz(`${id} zwod po 16 t bez ciosu`, ile(wyn.zwod_pozny, d => d === 0) === PROBY, `obrazenia ${wyn.zwod_pozny.join(' ')}`);
      w.sprawdz(`${id} zwod kosztuje`, ile(wyn.koszt, k => k > 0 && k < 3) === wyn.koszt.length, `stamina ${wyn.koszt.join(' ')}`);
    }
    w.sprawdz(`${id} za_pozno = cios`, ile(wyn.za_pozno, d => d > 0) === PROBY, `obrazenia ${wyn.za_pozno.join(' ')}`);
  }
  await rcon('minecraft:kill @e[type=husk,tag=zw]');
  if (CEL) await rcon(`minecraft:effect clear ${CEL}`);
}, { limitMs: 300000 });
