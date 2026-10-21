'use strict'
const H = require('/e/lib/harness'); const { rcon } = require('/k/rcon.js')
module.exports = async function (r) {
  const nick = 'EvR' + Date.now() % 10000
  const ok = async (slot, pred) => (await rcon(`execute if items entity ${nick} ${slot} ${pred}`)).includes('passed')
  const id = x => `*[custom_data~{MMOITEMS_ITEM_ID:"${x}"}]`
  const gdzie = async x => { for (let k = 0; k < 36; k++) if (await ok('container.' + k, id(x))) return k; return -1 }
  const daj = async (x, cel, typ = 'SWORD') => { await rcon(`mi give ${typ} ${x} ${nick} 1`); await H.sleep(700); const k = await gdzie(x); if (k >= 0 && 'container.' + k !== cel) { await rcon(`minecraft:item replace entity ${nick} ${cel} from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`) } await H.sleep(500) }
  const ma = async (s, komp) => !(await rcon(`data get entity ${nick} ${s}.components."minecraft:${komp}"`)).includes('Found no')
  const szyb = async () => { const m = (await rcon(`attribute ${nick} minecraft:attack_speed get`)).match(/is ([\d.]+)/); return m ? +m[1] : null }
  let bot = await H.connect(nick); await H.sleep(3500)
  for (const a of ['bieglosc-miecz', 'bieglosc-luk', 'bieglosc-tarcza']) await rcon(`mmocore admin attribute give ${nick} ${a} 1`)
  await rcon(`mmocore admin level set ${nick} main 30`)
  bot.setQuickBarSlot(0); await H.sleep(300)
  await daj('MIECZ_KAMIENNY', 'container.0')
  for (const [typ, x] of [['BOW', 'COMPOSITE_BOW'], ['SHIELD', 'PALADIN_SHIELD']]) {
    await rcon(`minecraft:item replace entity ${nick} weapon.offhand with air`); await daj(x, 'weapon.offhand', typ); await H.sleep(1200)
    const mod = await rcon(`attribute ${nick} minecraft:attack_speed modifier value get dwiebronie:premia_szybkosci`)
    r.assert(await ok('weapon.offhand', id(x)) && !(await ma('equipment.offhand', 'consumable')) && mod.includes('no modifier'), `${typ} w lewej: nie uzbrojony, bez premii`, null)
  }
  await rcon(`minecraft:item replace entity ${nick} weapon.offhand with air`)
  await daj('PRZ_WOJ_MIECZ_2', 'weapon.offhand'); await H.sleep(1200)
  r.assert(await ma('equipment.offhand', 'consumable'), 'miecz w lewej: uzbrojony')
  // slot 2 Ksiegi: uzbrojona bron trafia do prawej - consumable zdjety od razu
  bot.setQuickBarSlot(1); await H.sleep(250)
  r.assert(await ok('container.1', id('PRZ_WOJ_MIECZ_2')) && !(await ma('Inventory[{Slot:1b}]', 'consumable')), 'slot 2: bron w prawej bez consumable po 5 tickach')
  bot.setQuickBarSlot(0); await H.sleep(1200)
  // kursor: uzbrojona lewa zdjeta na kursor traci consumable od razu (wiec do skrzyni trafi juz bez niego)
  await bot.clickWindow(45, 0, 0); await H.sleep(300)
  r.assert(await ok('player.cursor', id('PRZ_WOJ_MIECZ_2')) && await ok('player.cursor', '*[!minecraft:consumable]'), 'bron zdjeta na kursor: bez consumable po 6 tickach')
  await bot.clickWindow(45, 0, 0); await H.sleep(1200)
  // wyrzucenie z off-handu: zaloz, uzbroj, wyrzuc (Q na off-handzie w oknie E)
  await daj('PRZ_WOJ_MIECZ_2', 'weapon.offhand'); await H.sleep(1200)
  r.assert(await ma('equipment.offhand', 'consumable'), 'ponownie uzbrojony')
  await bot.clickWindow(45, 0, 4); await H.sleep(1500)  // mode 4 = Q
  r.assert(await ok('weapon.offhand', id('PRZ_WOJ_MIECZ_2')), 'Q na lewej w oknie E: Ksiega blokuje wyrzucenie (bron zostaje)')
  await bot.clickWindow(45, 0, 0); await H.sleep(300); await bot.clickWindow(-999, 0, 0); await H.sleep(1500)
  // serwer nie pozwala wyrzucic broni z kursora poza okno - wraca do ekwipunku (bez consumable)
  let k2 = -1; for (let k = 0; k < 36; k++) if (await ok('container.' + k, id('PRZ_WOJ_MIECZ_2'))) k2 = k
  const naZiemi = (await rcon(`execute if entity @e[type=item]`)).includes('passed')
  r.assert(!naZiemi && (k2 >= 0 || await ok('weapon.offhand', id('PRZ_WOJ_MIECZ_2'))), 'klik poza oknem: bron nie ginie i nie lezy na ziemi', k2)
  r.assert(k2 < 0 || await ok('container.' + k2, '*[!minecraft:consumable]'), 'bron po powrocie do ekwipunku bez consumable', k2)
  await rcon(`minecraft:kill @e[type=item]`)
  await H.quit(bot)
}
