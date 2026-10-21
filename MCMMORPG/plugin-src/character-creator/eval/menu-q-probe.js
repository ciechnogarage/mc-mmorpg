'use strict'
/** Menu glowne pod klawiszem Q (PlayerDropItemEvent) + kafelek ze statami postaci.
 *  Bot "wciska Q" tak jak gracz: wyrzuca przedmiot z reki. */
const H = require('./lib/harness'); const F = require('./lib/flow')
const NAME = process.env.EQ_BOT || 'EqPanQA'

function loreOf(item) {
  if (!item) return null
  const c = item.components && item.components.find && item.components.find(c => /lore/i.test(c.type || ''))
  if (c) return JSON.stringify(c.data).slice(0, 500)
  if (item.customLore) return [].concat(item.customLore).join(' | ').slice(0, 500)
  const nbt = item.nbt && item.nbt.value && item.nbt.value.display
  return nbt ? JSON.stringify(nbt).slice(0, 500) : '(brak lore w danych bota)'
}

;(async () => {
  const r = new H.Results('menu-Q'); let bot
  try {
    bot = await H.connect(NAME)
    const prof = await F.waitProfileScreen(bot, 20000)
    if (prof) { await H.click(bot, 10); await H.sleep(2500); if (F.isCreatorWindow(bot)) { await F.completeCreator(bot); await H.sleep(3000) } }
    for (let i = 0; i < 30 && !bot.inventory.slots[9]; i++) await H.sleep(1000)

    bot.chat(`/mi give MATERIAL PROF_ROUGH_STONE ${NAME}`); await H.sleep(2000)
    const held = bot.inventory.slots.slice(36, 45).find(Boolean)
    if (!r.assert(!!held, 'bot ma cos w hotbarze do wyrzucenia')) return
    await bot.setQuickBarSlot(bot.inventory.slots.indexOf(held) - 36); await H.sleep(500)

    await bot.tossStack(held).catch(() => {})
    await H.sleep(2500)
    const w = bot.currentWindow
    if (r.assert(!!w, 'Q (wyrzucenie przedmiotu) otworzylo menu', w ? H.windowTitle(w) : '-')) {
      const tile = w.slots[11]
      r.assert(!!tile, 'kafelek w slocie 11 istnieje', H.itemLabel(tile))
      const raw = loreOf(tile) || ''
      const txt = (JSON.stringify(tile) .match(/"value":"([^"]*)"/g) || []).map(m => m.slice(9, -1))
        .filter(v => v && !/^(white|gray|gold|yellow|green|dark_\w+|red|aqua|blue|text|string|compound|list|byte)$/.test(v)).join(' ')
      r.ok('lore kafelka - tekst', txt.replace(/\s+/g, ' ').slice(0, 300))
      r.assert(!/%\w+%/.test(txt), 'placeholdery statow podstawily sie (brak surowych %...%)',
        (txt.match(/%\w+%/g) || []).join(', ') || 'brak surowych')
      const all = H.windowSlots(w).filter(s => s.slot < 27).map(s => `${s.slot}:${s.label}`).join(' | ')
      r.ok('cale menu glowne', all.slice(0, 400))
      bot.closeWindow(w)
    }
  } catch (e) { r.fail('probe', String(e && e.message || e)) }
  finally { r.print(); if (bot) await H.quit(bot); process.exit(0) }
})()
