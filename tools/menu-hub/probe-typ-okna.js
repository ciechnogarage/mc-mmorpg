'use strict'
/** Jakiego TYPU okno daje DeluxeMenus dla hub_glowne — chest 9x3 czy shulker?
 *  Od tego zalezy, czy da sie uzyc przezroczystej tekstury shulkera z packa Nexo. */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
module.exports = async function (r) {
  const bot = await H.connect('EvTyp' + Date.now() % 100000)
  try {
    if (!r.assert(!!(await F.waitProfileScreen(bot)), 'ekran wyboru')) return
    if (!r.assert(!!(await F.enterCreator(bot)), 'kreator')) return
    await F.completeCreator(bot); await H.sleep(3000)
    bot._client.write('block_dig', { status: 4, location: {x:0,y:0,z:0}, face: 0, sequence: 0 })
    const w = await H.waitForWindow(bot, { timeout: 12000, titleIncludes: 'Menu' })
    if (!r.assert(!!w, 'menu otwarte')) return
    console.log('   TYP OKNA: ' + JSON.stringify(w.type) + '  id=' + w.id +
                '  slotow=' + (w.slots.length - 36) + '  tytul=' + H.windowTitle(w))
    r.ok('typ okna odczytany')
  } finally { await H.quit(bot) }
}
