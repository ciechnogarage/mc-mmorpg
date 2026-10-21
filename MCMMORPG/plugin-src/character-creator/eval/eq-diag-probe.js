'use strict'
const H = require('./lib/harness'); const F = require('./lib/flow')
const NAME = process.env.EQ_BOT || 'EqRingQA'
;(async () => {
  const r = new H.Results('diag'); let bot
  try {
    bot = await H.connect(NAME)
    const prof = await F.waitProfileScreen(bot, 15000)
    if (prof) { await H.sleep(1000) }
    await H.sleep(4000)
    r.ok('sloty 9-17 (pod E)', bot.inventory.slots.slice(9,18).map((it,i)=>`${9+i}:${it?H.itemLabel(it):'-'}`).join(' | '))
    r.ok('plecak 18-44', bot.inventory.slots.slice(18,45).map((it,i)=>it?`${18+i}:${H.itemLabel(it)}`:null).filter(Boolean).join(' | ') || 'pusty')
    const idx = bot.inventory.slots.findIndex((it,i)=> i>=18 && it && /Srebrny sygnet/i.test(H.itemLabel(it)||''))
    r.ok('pierscien w plecaku', 'slot ' + idx)
    if (idx > 0) {
      const before = bot.evalLog.length
      await bot.clickWindow(idx, 0, 0); await H.sleep(800)
      r.ok('kursor trzyma', H.itemLabel(bot.inventory.slots[idx]) || 'slot zrodlowy pusty (podniesiony)')
      await bot.clickWindow(12, 0, 0); await H.sleep(2000)
      r.ok('slot 12 po wlozeniu', H.itemLabel(bot.inventory.slots[12]) || '-')
      r.ok('slot zrodlowy po wlozeniu', H.itemLabel(bot.inventory.slots[idx]) || '-')
      r.ok('wiadomosci serwera po kliknieciu', bot.evalLog.slice(before).join(' // ').slice(0,300) || 'brak')
    }
  } catch (e) { r.fail('diag', String(e && e.message || e)) }
  finally { r.print(); if (bot) await H.quit(bot); process.exit(0) }
})()
