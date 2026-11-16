'use strict'
/** Przegląd 2026-09-25: postać "sprzed stałych slotów" (bez przycisków) — czy po ponownym
 *  wejściu i wyborze postaci (MMOProfiles) przyciski się pojawiają? Przyciski zdejmujemy
 *  konsolą (przygotowanie), wybór postaci robi bot jak gracz. */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
module.exports = async function (r) {
  const nick = 'EvRel' + Date.now() % 100000
  let bot = await H.connect(nick)
  try {
    await F.waitProfileScreen(bot); await F.enterCreator(bot)
    await F.completeCreator(bot); await H.sleep(4000)
    await rcon(`minecraft:item replace entity ${nick} hotbar.8 with minecraft:air`)
    await rcon(`minecraft:item replace entity ${nick} container.18 with minecraft:air`)
    await H.sleep(1000)
    r.assert(!bot.inventory.slots[44] && !bot.inventory.slots[18], 'przygotowanie: przyciski zdjete', H.inventoryLabels(bot).join(' '))
    await H.quit(bot); await H.sleep(3000)
    bot = await H.connect(nick)
    const w = await F.waitProfileScreen(bot)
    if (!r.assert(!!w, 'ekran wyboru postaci po ponownym wejsciu')) return
    const sloty = H.windowSlots(w).filter(s => s.slot < w.inventoryStart)
    console.log('  ekran postaci:', sloty.map(s => `${s.slot}:${s.name}:${s.label}`).join(' | '))
    const postac = sloty.find(s => s.name === 'player_head') || sloty.find(s => !/Stw|Create|Nowa|pane|barrier/i.test(s.label + s.name))
    if (!r.assert(!!postac, 'jest kafelek istniejacej postaci')) return
    await H.click(bot, postac.slot)
    await H.sleep(1500)
    if (bot.currentWindow) console.log('  po kliknieciu okno:', H.windowTitle(bot.currentWindow), H.windowSlots(bot.currentWindow).filter(s => s.slot < bot.currentWindow.inventoryStart).map(s => `${s.slot}:${s.label}`).join(' | '))
    await H.sleep(5000)
    if (bot.currentWindow) { console.log('  nadal okno:', H.windowTitle(bot.currentWindow)) }
    const s8 = await rcon(`minecraft:execute if items entity ${nick} hotbar.8 minecraft:book`)
    const s18 = await rcon(`minecraft:execute if items entity ${nick} container.18 minecraft:leather`)
    console.log('  eq:', H.inventoryLabels(bot).join(' '))
    r.assert(/passed/i.test(s8) && /passed/i.test(s18), 'po wyborze postaci przyciski wracaja (PlayerDataLoadedEvent)', `8:${s8.trim()} 18:${s18.trim()}`)
  } finally { await H.quit(bot) }
}
