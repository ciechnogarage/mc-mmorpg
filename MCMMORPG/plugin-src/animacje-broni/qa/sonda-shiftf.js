'use strict';
// Sonda Shift+F (16.11): zamiana kompletu Ksiegi (KsiegaPostaci Bron.swap: anulowany PlayerSwapHandItemsEvent + setItem
// slotow 0/40 <-> 11/12) -> czy PPM od razu laduje mocny cios bronia z zapasu.
//   run.sh <klon> qa/sonda-shiftf.js [proby] [zwloka_ms] [garda]   (klon --z-kreatorem, debug: true; licz MOCNY/SONDA_PPM po kazdej linii SONDA)
// Zapas = drugi miecz z `mi give` przelozony z plecaka do slotu 11 (nigdy w rece: bez consumable i bez znacznika stanu).
// Zamiana Shift+F wysyla Ksiega przez setItem, a AnimacjeBroni slucha zamiany rak z ignoreCancelled -> bez
// PlayerInventorySlotChangeEvent komponenty czekaly na zegar co 10 t.
const { scenariusz, sleep, postac, czekaj } = require('/bot/lib');
const PROBY = +(process.argv[2] || 6);
const ZWLOKA = +(process.argv[3] || 60);   // ms od puszczenia Shift do PPM (gracz: 0-150)
const GARDA = process.argv[4] === 'garda';   // Shift trzymany dalej -> PPM = garda (licz SONDA_PPM+1 uniesiona=true)
const NICK = 'BotSf' + (Date.now() % 10000);
scenariusz('sonda-shiftf', async ({ w, polacz, rcon }) => {
  const bot = await polacz({ nick: NICK });
  w.sprawdz('postac', !!(await postac.wejdz(bot, {})), NICK);
  await sleep(1500);
  await rcon(`minecraft:gamemode survival ${NICK}`);
  await rcon(`execute in minecraft:overworld run teleport ${NICK} 0.5 -59 0.5 0 0`);
  await rcon(`mmocore admin class ${NICK} BASTION`); await sleep(2500);
  const zamien = () => bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 });
  const ppmW11 = async () => /consume_seconds|animation/.test(
    await rcon(`minecraft:data get entity ${NICK} Inventory[{Slot:11b}].components."minecraft:consumable"`));
  let zamiany = 0;
  for (let i = 0; i < PROBY; i++) {
    await rcon(`minecraft:clear ${NICK}`); await sleep(300);
    await rcon(`mi give SWORD MIECZ_ZELAZNY ${NICK} 1`);
    await czekaj(() => bot.heldItem && bot.heldItem.name === 'iron_sword', 3000, 50);
    await sleep(1200);   // aktywny miecz dopasowany zegarem
    // drugi miecz prosto z plecaka do slotu 11 - nigdy w rece, wiec bez komponentow i bez znacznika stanu (PDC "stan");
    // kopia z reki (item replace from weapon.mainhand) niosla znacznik i dopasuj() ja pomijal (16.11, 0/6 tez z poprawka)
    await rcon(`mi give SWORD MIECZ_ZELAZNY ${NICK} 1`); await sleep(1200);   // Ksiega odklada kartki paska 1-4
    let m = null;   // pelny Inventory RCON ucina (~4 KB) -> pytanie o kazdy slot osobno
    for (const x of [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35]) {
      if ((await rcon(`minecraft:data get entity ${NICK} Inventory[{Slot:${x}b}].id`)).includes('iron_sword')) { m = x; break; }
    }
    w.sprawdz(`proba ${i}: drugi miecz`, m != null, 'slot ' + m);
    await rcon(`minecraft:item replace entity ${NICK} container.11 from entity ${NICK} container.${m}`);
    await rcon(`minecraft:item replace entity ${NICK} container.${m} with minecraft:air`);
    await rcon(`minecraft:item modify entity ${NICK} container.11 {function:"minecraft:set_name",name:"ZAPAS"}`);
    w.sprawdz(`proba ${i}: zapas bez PPM`, !(await ppmW11()), 'slot 11 bez consumable');
    await rcon(`mmocore admin resource-stamina set ${NICK} 20`);
    await rcon(`say SONDA ${NICK} proba=${i}`);
    bot.setControlState('sneak', true); await sleep(150);
    zamien(); await sleep(60);
    if (!GARDA) bot.setControlState('sneak', false);
    await sleep(ZWLOKA);
    bot.activateItem(); await sleep(1400); bot.deactivateItem(); await sleep(600);
    bot.setControlState('sneak', false);
    const wReku = await rcon(`minecraft:data get entity ${NICK} SelectedItem.components."minecraft:custom_name"`);
    if (wReku.includes('ZAPAS')) zamiany++;   // w rece bron z zapasu = Shift+F zadzialal
    else w.sprawdz(`proba ${i}: zamiana`, false, wReku.slice(0, 120));
  }
  w.sprawdz('Shift+F zamienil komplet', zamiany === PROBY, `${zamiany}/${PROBY}`);
  w.sprawdz('koniec', true, 'wynik w logu klona: MOCNY po kazdej linii SONDA');
}, { limitMs: 240000 });
