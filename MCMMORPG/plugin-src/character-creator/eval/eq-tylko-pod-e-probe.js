'use strict'
/** Stan docelowy: caly ekwipunek pod klawiszem E, panel /eq nie istnieje. */
const H = require('./lib/harness'); const F = require('./lib/flow')
const NAME = process.env.EQ_BOT || 'EqPanQA'
const hp = bot => new Promise(res => {
  const h = m => { const t = m.toString(); if (/^HP=/.test(t)) { bot.removeListener('message', h); res(t.trim()) } }
  bot.on('message', h); bot.chat(`/papi parse ${NAME} HP=%mythiclib_stat_max_health%`)
  setTimeout(() => { bot.removeListener('message', h); res('brak') }, 4000)
})
;(async () => {
  const r = new H.Results('tylko-pod-E'); let bot
  try {
    bot = await H.connect(NAME)
    const prof = await F.waitProfileScreen(bot, 20000)
    if (prof) { await H.click(bot, 10); await H.sleep(2500); if (F.isCreatorWindow(bot)) { await F.completeCreator(bot); await H.sleep(3000) } }
    // sloty MMOInventory wjezdzaja z opoznieniem po wczytaniu profilu — czekamy na nie,
    // zamiast mierzyc na slepo (poprzedni przebieg zlapal pusty plecak i falszywy FAIL)
    for (let i = 0; i < 30 && !bot.inventory.slots[9]; i++) await H.sleep(1000)
    r.ok('otwarte okno w chwili pomiaru', bot.currentWindow ? H.windowTitle(bot.currentWindow) : 'brak (gracz w swiecie)')

    const row = bot.inventory.slots.slice(9, 19).map((it, i) => `${9 + i}:${it ? H.itemLabel(it) : '-'}`)
    r.assert(bot.inventory.slots.slice(9, 18).every(Boolean),
      '9 slotow akcesoriow pod E', row.slice(0, 9).join(' | '))
    const base = await hp(bot)
    r.ok('zycie przed zalozeniem', base)

    // realne zalozenie: koszula +6 HP do slotu 15 pod E
    bot.chat(`/mi give SHIRT PROF_LINEN_SHIRT ${NAME}`); await H.sleep(2000)
    const idx = bot.inventory.slots.findIndex((it, i) => i >= 19 && it && /Lniana koszula/i.test(H.itemLabel(it) || ''))
    if (r.assert(idx > 0, 'koszula w plecaku', 'slot ' + idx)) {
      await bot.clickWindow(idx, 0, 0); await H.sleep(700)
      await bot.clickWindow(15, 0, 0); await H.sleep(2500)
      const after = await hp(bot)
      r.assert(/Lniana koszula/i.test(H.itemLabel(bot.inventory.slots[15]) || ''),
        'koszula zalozona w slocie pod E', 'slot 15: ' + H.itemLabel(bot.inventory.slots[15]))
      r.assert(after !== base, 'staty policzone (raz)', `${base} -> ${after}`)
    }

    const before = bot.evalLog.length
    bot.chat('/eq'); await H.sleep(2500)
    const win = bot.currentWindow
    const msgs = bot.evalLog.slice(before).join(' // ')
    r.assert(!win, 'komenda /eq NIE otwiera juz zadnego okna', msgs.slice(0, 160) || '(brak odpowiedzi)')
    bot.chat('/custominv'); await H.sleep(2500)
    r.assert(!bot.currentWindow, 'komenda /custominv rowniez martwa')
  } catch (e) { r.fail('probe', String(e && e.message || e)) }
  finally { r.print(); if (bot) await H.quit(bot); process.exit(0) }
})()
