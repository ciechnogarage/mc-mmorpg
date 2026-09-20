'use strict'
/** Rozlaczenie w trakcie kreatora: rzeczy maja wrocic przy nastepnym wejsciu. */
const H = require('../lib/harness'); const F = require('../lib/flow')
module.exports = async function (r) {
  const name = 'EvRozl' + Date.now() % 100000
  let bot = await H.connect(name)
  let before = 0
  try {
    await F.waitProfileScreen(bot)
    await H.sleep(3000)
    before = F.totalItems(bot)
    const w = await F.enterCreator(bot)
    if (!r.assert(!!w, 'kreator otwarty')) return
    await H.sleep(4000)
  } finally { await H.quit(bot) }

  await H.sleep(4000)
  bot = await H.connect(name)
  try {
    await H.sleep(8000)   // restoreAfterJoin ma 40 tickow opoznienia
    r.assert(!F.inRoom(bot), 'po powrocie gracz nie zostal uwieziony w sali kreatora',
      bot.entity.position.toString())
    r.assert(bot.game.gameMode !== 'spectator', 'tryb gry nie zostal spectator',
      bot.game.gameMode)
    const after = F.totalItems(bot)
    r.assert(after >= before, 'ekwipunek wrocil po rozlaczeniu', `przed=${before} po=${after}`)
  } finally { await H.quit(bot) }
}
