'use strict'
const L = require('/t/lib.js')
;(async () => {
  const bot = await L.H.connect(process.env.EVAL_NICKS || 'HdA3', { timeout: 90000 })
  const w = await L.F.waitProfileScreen(bot).catch(e => null)
  L.zrzut(w, 'ekran postaci')
  await L.sleep(1000); bot.quit(); process.exit(0)
})()
