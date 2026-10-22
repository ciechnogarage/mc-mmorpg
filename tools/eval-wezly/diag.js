'use strict'
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const NICK = process.env.EVAL_NICK || 'EvWezel'
const sleep = ms => new Promise(r => setTimeout(r, ms))
const label = i => { try { return H.itemLabel(i) } catch { return i.displayName } }
;(async () => {
  const bot = await H.connect(NICK, { timeout: 90000 })
  try {
    try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) } catch (e) {}
    await sleep(3000)
    for (const cmd of [`/mi give MATERIAL PROF_OAK_TIMBER ${NICK} 3`, `/mi give MATERIAL PROF_OAK_TIMBER ${NICK}`]) {
      bot.evalLog.length = 0
      console.log('> ' + cmd); bot.chat(cmd); await sleep(2500)
      console.log('  odpowiedz: ' + (bot.evalLog.filter(m => !/❤|♦|⛨/.test(m)).slice(-2).join(' | ') || '(cisza)'))
      const ma = bot.inventory.items().filter(i => (label(i) || '').toLowerCase().includes('dęb'))
      console.log('  drewno: ' + (ma.length ? ma.map(i => i.count + 'x ' + label(i)).join(', ') : 'BRAK'))
      if (ma.length) break
    }
  } finally { await H.quit(bot) }
  process.exit(0)
})().catch(e => { console.log('przerwane: ' + e.message); process.exit(2) })
