'use strict'
/** Przegląd 2026-09-25: Q (MenuKey) przy dzienniku w ręce — skrypt CoreTools anuluje
 *  wyrzucenie; czy menu pod Q nadal się otwiera (regresja MenuKey)? */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
module.exports = async function (r) {
  const bot = await H.connect('EvQ' + Date.now() % 100000)
  try {
    await F.waitProfileScreen(bot); await F.enterCreator(bot)
    await F.completeCreator(bot); await H.sleep(4000)
    for (const slot of [8, 4]) {
      bot.setQuickBarSlot(slot); await H.sleep(500)
      const przed = bot.inventory.slots[36 + slot]
      bot._client.write('block_dig', { status: 4, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 })
      const w = await H.waitForWindow(bot, { timeout: 5000, titleIncludes: 'Menu' })
      r.assert(!!w, `Q przy slocie ${slot} (${przed ? H.itemLabel(przed) : 'pusto'}) otwiera menu`, w ? H.windowTitle(w) : 'brak okna')
      if (w) { bot.closeWindow(w); await H.sleep(800) }
    }
    r.assert(bot.inventory.slots[44]?.name === 'book', 'dziennik zostal w slocie 8', H.itemLabel(bot.inventory.slots[44]))
  } finally { await H.quit(bot) }
}
