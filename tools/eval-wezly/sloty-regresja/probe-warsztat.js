'use strict'
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const sleep = ms => new Promise(r => setTimeout(r, ms))
;(async () => {
  const bot = await H.connect(process.env.EVAL_NICK, { timeout: 90000 })
  try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) } catch (e) { console.log(e.message) }
  await sleep(2500)
  for (const c of ['/warsztat-cooking', '/profesje']) {
    bot.evalLog.length = 0; bot.chat(c); await sleep(2500)
    console.log(c, '-> okno:', bot.currentWindow ? H.windowTitle(bot.currentWindow) : '-', '| czat:', bot.evalLog.filter(l => !/[❤♦⛨]/.test(l)).join(' / '))
    try { bot.closeWindow(bot.currentWindow) } catch {}
    await sleep(500)
  }
  await H.quit(bot); process.exit(0)
})()
