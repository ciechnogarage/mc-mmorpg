'use strict'
/**
 * REGRESJA zgloszona z gry: "przy probie wywalalo non stop ekran wybrania profilu,
 * chociaz tworzylem postac". Straznik wyboru profilu nie moze walczyc z kreatorem.
 */
const H = require('../lib/harness'); const F = require('../lib/flow')
module.exports = async function (r) {
  const bot = await H.connect('EvStrz' + Date.now() % 100000)
  try {
    const w = await F.enterCreator(bot)
    if (!r.assert(!!w, 'kreator otwarty')) return
    let stolen = 0
    for (let i = 0; i < 20; i++) {           // 40 s: dwa pelne okresy recheck (200 tickow)
      await H.sleep(2000)
      if (F.isProfileWindow(bot)) stolen++
    }
    r.assert(stolen === 0, 'ekran wyboru profilu NIE podmienia kreatora przez 40 s',
      stolen ? `podmieniony ${stolen}x` : 'ani razu')
    r.assert(F.isCreatorWindow(bot), 'kreator nadal otwarty po 40 s',
      H.windowTitle(bot.currentWindow))
    r.assert(!bot.evalLog.some(m => m.includes('Jesteś już w kreatorze')),
      'nie doszlo do proby drugiej sesji kreatora')
  } finally { await H.quit(bot) }
}
