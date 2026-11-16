'use strict'
/** Po usunieciu slotow akcesoriow z panelu: czy zostal tylko jeden komplet slotow
 *  i czy przedmiot osierocony w usunietym slocie przestal dawac staty. */
const H = require('./lib/harness'); const F = require('./lib/flow')
const NAME = process.env.EQ_BOT || 'EqPanQA'
const hp = bot => new Promise(res => {
  const h = m => { const t = m.toString(); if (/^HP=/.test(t)) { bot.removeListener('message', h); res(t.trim()) } }
  bot.on('message', h); bot.chat(`/papi parse ${NAME} HP=%mythiclib_stat_max_health%`)
  setTimeout(() => { bot.removeListener('message', h); res('brak') }, 4000)
})
;(async () => {
  const r = new H.Results('po-ciecu'); let bot
  try {
    bot = await H.connect(NAME)
    const prof = await F.waitProfileScreen(bot, 20000)
    if (prof) { await H.click(bot, 10); await H.sleep(2500); if (F.isCreatorWindow(bot)) { await F.completeCreator(bot); await H.sleep(3000) } }
    await H.sleep(5000)
    r.ok('zycie teraz', await hp(bot))
    r.ok('sloty pod E (9-17)', bot.inventory.slots.slice(9,18).map((it,i)=>`${9+i}:${it?H.itemLabel(it):'-'}`).join(' | '))
    bot.chat('/eq'); const p = await H.waitForWindow(bot, { timeout: 12000 })
    if (r.assert(!!p, 'panel otwarty')) {
      const panelOnly = H.windowSlots(p).filter(s => s.slot < 54)
      r.ok('zawartosc samego panelu (sloty 0-53)', panelOnly.map(s=>`${s.slot}:${s.label}`).join(' | ').slice(0,400))
      const acc = panelOnly.filter(s => /Naszyjnik|Kolczyk|Ring|Pas|Charm|Koszula|Dlonie|Ramiona/i.test(s.label||''))
      r.assert(acc.length === 0, 'panel NIE ma juz slotow akcesoriow (koniec duplikatu)',
        acc.length ? acc.map(s=>s.label).join(', ') : 'zero')
      bot.closeWindow(p)
    }
  } catch (e) { r.fail('probe', String(e && e.message || e)) }
  finally { r.print(); if (bot) await H.quit(bot); process.exit(0) }
})()
