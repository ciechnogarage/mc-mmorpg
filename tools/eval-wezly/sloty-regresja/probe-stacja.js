'use strict'
const H = require('/e/lib/harness'); const F = require('/e/lib/flow'); const { rconClient } = require('/t/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms)); const N = process.env.EVAL_NICK
;(async () => {
  const rc = await rconClient()
  const bot = await H.connect(N, { timeout: 90000 })
  try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) } catch (e) { console.log(e.message) }
  await sleep(2500)
  for (const st of (process.env.STACJE || 'profession-jewelcrafting profession-mining').split(' ')) {
    bot.evalLog.length = 0
    const out = await rc.cmd(`mi stations open ${st} ${N}`); await sleep(2000)
    console.log(st, '| rcon:', JSON.stringify(out), '| okno:', bot.currentWindow ? H.windowTitle(bot.currentWindow) : '-', '| czat:', bot.evalLog.filter(l => !/[\u2764\u2666\u26e8]/.test(l)).join(' / '))
    try { bot.closeWindow(bot.currentWindow) } catch {}; await sleep(500)
  }
  await H.quit(bot); rc.close(); process.exit(0)
})()
