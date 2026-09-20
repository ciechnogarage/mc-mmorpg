'use strict'
/** Nikt nie gra bez postaci: ekran wyboru ma sie pojawic i wracac po zamknieciu. */
const H = require('../lib/harness'); const F = require('../lib/flow')
module.exports = async function (r) {
  const bot = await H.connect('EvWym' + Date.now() % 100000)
  try {
    r.assert(!!(await F.waitProfileScreen(bot)), 'ekran wyboru postaci otwiera sie po wejsciu')
    bot.closeWindow(bot.currentWindow)
    await H.sleep(1000)
    r.assert(!F.isProfileWindow(bot), 'ekran da sie zamknac (gracz probuje uciec)')
    const back = await F.waitProfileScreen(bot, 20000)
    r.assert(!!back, 'straznik otwiera go z powrotem w 20 s')
  } finally { await H.quit(bot) }
}
