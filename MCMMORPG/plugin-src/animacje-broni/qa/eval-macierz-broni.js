'use strict';
// Macierz ataku broni (AnimacjeBroni) na klonie: dla kazdej broni 3 lekkie ciosy LPM w huska, mocny PPM (trzymany ~1 s),
// szturchniecie (krotki PPM), garda (Shift+PPM -> komponent blocks_attacks) i komponenty przedmiotu w dloni.
//   run.sh <klon> macierz-broni.js "TYP:ID,TYP:ID,..." [lewa=TYP:ID]
// Obrazenia = spadek Health huska (NoAI, 1000 HP). Debug AnimacjeBroni (linie DBG) wlaczyc na klonie przed biegiem.
const { scenariusz, czekaj, sleep } = require('/bot/lib');

const LISTA = (process.argv[2] || 'SWORD:MIECZ_ZELAZNY').split(',').map(s => s.split(':'));
const LEWA = (process.argv[3] || '').startsWith('lewa=') ? process.argv[3].slice(5).split(':') : null;
const NICK = 'BotAt' + (Date.now() % 100000);
const X = 0.5, Y = -59, Z = 0.5;
const BIEGLOSCI = ['miecz', 'miecz-dwureczny', 'klinga', 'topor', 'drzewcowa', 'wlocznia', 'sztylet', 'lancuch', 'kostur',
  'kostur-dwureczny', 'grimuar', 'totem', 'filakterium', 'bulawa', 'mlot-dwureczny', 'sierp', 'kosa', 'tarcza'];

scenariusz('macierz-broni', async ({ w, polacz, rcon }) => {
  await rcon('minecraft:gamerule spawn_mobs false');
  await rcon('minecraft:weather clear');
  await rcon(`minecraft:fill ${X - 4} ${Y - 1} ${Z - 4} ${X + 4} ${Y - 1} ${Z + 6} minecraft:stone`);
  const bot = await polacz({ nick: NICK });
  await sleep(1500);
  await rcon(`minecraft:gamemode survival ${NICK}`);
  await rcon(`minecraft:teleport ${NICK} ${X} ${Y} ${Z} 0 0`);
  for (const b of BIEGLOSCI) await rcon(`mmocore admin attribute give ${NICK} bieglosc-${b} 1`);
  // lup PRZ_*_4/5 wymaga poziomu >= 10 i atrybutow (np. sila 4) - bez nich MMOItems slusznie odmawia (0 obrazen)
  await rcon(`mmocore admin level set ${NICK} main 60`);
  for (const a of ['strength', 'dexterity', 'intelligence', 'wiara']) await rcon(`mmocore admin attribute give ${NICK} ${a} 30`);
  await rcon(`minecraft:effect give ${NICK} minecraft:resistance infinite 4 true`);

  const hp = async () => {
    const m = /([\d.]+)f/.exec(await rcon('minecraft:data get entity @e[type=husk,tag=mb_cel,limit=1] Health'));
    return m ? +m[1] : NaN;
  };
  const komponent = async (k) => {
    const t = await rcon(`minecraft:data get entity ${NICK} SelectedItem.components."minecraft:${k}"`);
    return /has the following entity data: (.*)$/.exec(t)?.[1] ?? '-';
  };
  const stamina = async () => rcon(`mmocore admin resource-stamina set ${NICK} 999`);

  for (const [typ, id] of LISTA) {
    await rcon(`minecraft:kill @e[type=husk,tag=mb_cel]`);
    await rcon(`minecraft:clear ${NICK}`);
    await rcon(`minecraft:teleport ${NICK} ${X} ${Y} ${Z} 0 0`);
    bot.setQuickBarSlot(0);
    await sleep(300);
    if (LEWA) {
      await rcon(`mi give ${LEWA[0]} ${LEWA[1]} ${NICK} 1`);
      await sleep(600);
      await rcon(`minecraft:item replace entity ${NICK} weapon.offhand from entity ${NICK} weapon.mainhand`);
      await rcon(`minecraft:item replace entity ${NICK} weapon.mainhand with minecraft:air`);
      await sleep(400);
    }
    const dano = await rcon(`mi give ${typ} ${id} ${NICK} 1`);
    await sleep(1200);
    let wDloni = (await rcon(`minecraft:data get entity ${NICK} SelectedItem.components."minecraft:custom_data".MMOITEMS_ITEM_ID`)).includes(`"${id}"`);
    if (!wDloni) {
      const inv = await rcon(`minecraft:data get entity ${NICK} Inventory`);
      const m = new RegExp(`\\{Slot: (\\d)b, id: "[^"]+", count: 1, components: \\{[^]*?MMOITEMS_ITEM_ID: "${id}"`).exec(inv);
      if (m) { bot.setQuickBarSlot(+m[1]); await sleep(600); }
      wDloni = (await rcon(`minecraft:data get entity ${NICK} SelectedItem.components."minecraft:custom_data".MMOITEMS_ITEM_ID`)).includes(`"${id}"`);
    }
    const tag = `${typ}:${id}`;
    if (!wDloni) { w.sprawdz(`${tag} w-dloni`, false, `nie trzyma (${dano.trim().slice(0, 80)})`); continue; }
    await sleep(800);   // przeglad dloni AnimacjeBroni (co 10 t)
    const komp = {};
    for (const k of ['swing_animation', 'attack_range', 'minimum_attack_charge', 'consumable', 'kinetic_weapon', 'piercing_weapon', 'item_model'])
      komp[k] = await komponent(k);

    await rcon(`minecraft:summon husk ${X} ${Y} ${Z + 2.3} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f],Tags:["mb_cel"],attributes:[{id:"minecraft:max_health",base:1000}],Health:1000f}`);
    const cel = await czekaj(() => Object.values(bot.entities).find(e => e.name === 'husk'), 5000, 200);
    if (!cel) { w.sprawdz(`${tag} cel`, false, 'brak huska'); continue; }
    const patrz = () => bot.lookAt(cel.position.offset(0, 1.2, 0), true);
    await stamina();

    // 3 lekkie ciosy w odstepie 2 s (pelne naladowanie nawet dla najwolniejszej broni, poza oknem rytmu)
    const lekkie = [];
    for (let i = 0; i < 3; i++) {
      await patrz(); await sleep(150);
      const h0 = await hp(); bot.attack(cel); await sleep(500); lekkie.push(+(h0 - await hp()).toFixed(2));
      await sleep(1500);
    }
    // mocny: PPM trzymany ~1,3 s, bez kierunku
    await stamina(); await patrz(); await sleep(1200);
    let h0 = await hp();
    bot.activateItem(); await sleep(1300); await patrz(); bot.deactivateItem(); await sleep(700);
    const mocny = +(h0 - await hp()).toFixed(2);
    // szturchniecie: krotki PPM
    await sleep(1500); await patrz();
    h0 = await hp();
    bot.activateItem(); await sleep(150); bot.deactivateItem(); await sleep(700);
    const sztur = +(h0 - await hp()).toFixed(2);
    // garda: Shift + PPM -> blocks_attacks na przedmiocie w dloni
    await sleep(1200);
    bot.setControlState('sneak', true); await sleep(500);
    bot.activateItem(); await sleep(600);
    const garda = await komponent('blocks_attacks');
    bot.deactivateItem(); bot.setControlState('sneak', false); await sleep(400);

    const sr = lekkie.reduce((a, b) => a + b, 0) / 3;
    w.sprawdz(`${tag} lekki`, lekkie.every(v => v > 0), `LPM ${lekkie.join('/')}`, { lekkie });
    w.sprawdz(`${tag} mocny`, mocny > sr * 0.9, `mocny ${mocny} vs lekki sr ${sr.toFixed(2)}; szturchniecie ${sztur}`, { mocny, sztur });
    w.sprawdz(`${tag} garda`, garda !== '-', `blocks_attacks w Shift+PPM: ${garda === '-' ? 'BRAK' : 'jest'}`, { garda });
    w.sprawdz(`${tag} komponenty`, true, Object.entries(komp).map(([k, v]) => `${k}=${v.length > 70 ? v.slice(0, 70) + '…' : v}`).join(' | '), komp);
  }
  await rcon('minecraft:kill @e[type=husk,tag=mb_cel]');
}, { limitMs: 30 * 60 * 1000 });
