'use strict'
/**
 * Ekwipunek ma wrocic w calosci. Dwa osobne ryzyka naraz:
 *  - kreator chowa plecak na czas sesji (i musi go oddac),
 *  - "Newbie kit" dosypuje przedmioty JUZ PO wejsciu do kreatora, a sweeper je zabiera —
 *    dawniej kasowal, teraz ma odkladac do stashu.
 */
const H = require('../lib/harness'); const F = require('../lib/flow')
module.exports = async function (r) {
  const bot = await H.connect('EvEkw' + Date.now() % 100000)
  try {
    await F.waitProfileScreen(bot)
    await H.sleep(3000)
    const before = F.totalItems(bot)
    const w = await F.enterCreator(bot)
    if (!r.assert(!!w, 'kreator otwarty')) return

    await H.sleep(6000)   // czas na kit i na dwa przebiegi sweepera
    const kit = bot.evalLog.some(m => m.toLowerCase().includes('kit'))
    const inCreator = bot.inventory.items().filter(i => i.slot < 36).length
    r.assert(inCreator <= 5, 'plecak schowany na czas kreatora (zostaja tylko ikony UI)',
      'zajetych slotow: ' + inCreator)

    await F.completeCreator(bot)
    await H.sleep(3000)
    const after = F.totalItems(bot)
    r.assert(after >= before, 'po wyjsciu nie ubylo przedmiotow',
      `przed=${before} po=${after}` + (kit ? ' (w trakcie przyszedl kit)' : ''))
    if (kit) {
      r.assert(after > 0, 'przedmioty z kitu przyznanego W KREATORZE nie przepadly',
        'po wyjsciu: ' + after)
    }
  } finally { await H.quit(bot) }
}
