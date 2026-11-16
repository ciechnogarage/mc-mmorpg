'use strict';
// Zwod: Shift w trakcie ladowania PPM przerywa mocny cios bez obrazen i kosztuje ~0,5 L staminy.
//   run.sh <klon> qa/eval-zwod.js [proby]   (klon --z-kreatorem, debug: true -> linie ZWOD/MOCNY w logu klona)
// Warianty na broni: kontrola (PPM 0,7 s bez Shift = cios), zwod (Shift po 0,15 s), za_pozno (Shift po 0,8 s = cios).
// Bot nie sprawdzi gardy po zwodzie: mineflayer nie ponawia uzycia przy trzymanym PPM jak klient vanilla -> klient QA.
const { scenariusz, czekaj, sleep, postac } = require('/bot/lib');
const PROBY = +(process.argv[2] || 5);
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
  const hp = async () => +(/([\d.]+)f/.exec(await rcon('minecraft:data get entity @e[type=husk,tag=zw,limit=1] Health')) || [0, NaN])[1];
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

  for (const [klasa, typ, id, material] of [['BASTION', 'SWORD', 'MIECZ_ZELAZNY', 'iron_sword'],
                                            ['JASTRZAB', 'DAGGER', 'SZTYLET_REKRUTA', 'iron_sword']]) {
    await rcon(`mmocore admin class ${NICK} ${klasa}`);
    await rcon(`minecraft:clear ${NICK}`);
    await rcon(`mi give ${typ} ${id} ${NICK} 1`);
    await sleep(1200);
    if (!w.sprawdz(`${id}-bron`, await trzymaj(material), klasa)) continue;
    await rcon('minecraft:kill @e[type=husk]');
    await czekaj(() => !Object.values(bot.entities).some(e => e.name === 'husk'), 3000, 100);
    await rcon(`${OW}summon husk ${X} ${Y} ${Z + 2.2} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["zw"],attributes:[{id:"minecraft:max_health",base:5000}],Health:5000f}`);
    const cel = await czekaj(() => Object.values(bot.entities).find(e => e.name === 'husk'), 5000, 200);
    await bot.lookAt(cel.position.offset(0, 1.2, 0), true);
    const wyn = { kontrola: [], zwod: [], za_pozno: [], koszt: [] };
    for (let i = 0; i < PROBY; i++) {
      for (const [wariant, shiftPo, puscPo] of [['zwod', 150, 500], ['kontrola', null, 700], ['za_pozno', 800, 1000]]) {
        await rcon(`mmocore admin resource-stamina set ${NICK} 20`);
        // odnowienie mocnego 20 t + podmiana komponentu po Shift; przed zwodem 3,5 s, bo krwawienie sztyletu z
        // poprzedniego ciosu tyka co 20 t przez ~3 s i wpadaloby w pomiar (16.11: 0,9 HP przy zwodzie)
        await sleep(wariant === 'zwod' ? 3500 : 1600);
        const h0 = await hp(), s0 = await stamina();
        bot.activateItem();
        if (shiftPo != null) { await sleep(shiftPo); bot.setControlState('sneak', true); await sleep(puscPo - shiftPo); }
        else await sleep(puscPo);
        bot.deactivateItem();
        await sleep(400);
        const h1 = await hp(), s1 = await stamina();
        bot.setControlState('sneak', false);
        wyn[wariant].push(+(h0 - h1).toFixed(2));
        if (wariant === 'zwod') wyn.koszt.push(+(s0 - s1).toFixed(2));
      }
    }
    const ile = (t, f) => t.filter(f).length;
    w.sprawdz(`${id} kontrola`, ile(wyn.kontrola, d => d > 0) === PROBY, `obrazenia ${wyn.kontrola.join(' ')}`);
    w.sprawdz(`${id} zwod bez ciosu`, ile(wyn.zwod, d => d === 0) === PROBY, `obrazenia ${wyn.zwod.join(' ')}`);
    w.sprawdz(`${id} zwod kosztuje`, ile(wyn.koszt, k => k > 0 && k < 3) === PROBY, `stamina ${wyn.koszt.join(' ')}`);
    w.sprawdz(`${id} za_pozno = cios`, ile(wyn.za_pozno, d => d > 0) === PROBY, `obrazenia ${wyn.za_pozno.join(' ')}`);
  }
  await rcon('minecraft:kill @e[type=husk,tag=zw]');
}, { limitMs: 300000 });
