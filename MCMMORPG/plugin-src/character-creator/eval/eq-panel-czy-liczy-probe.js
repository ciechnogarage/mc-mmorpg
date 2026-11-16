'use strict'
/**
 * Czy panel /eq (custom inventory) NADAL nadaje staty, gdy wlaczony jest tez ekwipunek
 * pod klawiszem E (vanilla inventory)? Pas daje +10 max health. Zakladamy go TYLKO
 * w panelu i patrzymy, czy zycie rosnie.
 */
const H = require('./lib/harness'); const F = require('./lib/flow')
const NAME = process.env.EQ_BOT || 'EqPanQA'
const hp = bot => new Promise(res => {
  const h = m => { const t = m.toString(); if (/^HP=/.test(t)) { bot.removeListener('message', h); res(t.trim()) } }
  bot.on('message', h); bot.chat(`/papi parse ${NAME} HP=%mythiclib_stat_max_health%`)
  setTimeout(() => { bot.removeListener('message', h); res('brak') }, 4000)
})
;(async () => {
  const r = new H.Results('panel-czy-liczy'); let bot
  try {
    bot = await H.connect(NAME)
    const prof = await F.waitProfileScreen(bot, 20000)
    if (prof) { await H.click(bot, 10); await H.sleep(2500); if (F.isCreatorWindow(bot)) { await F.completeCreator(bot); await H.sleep(3000) } }
    await H.sleep(5000)
    bot.chat(`/mi give BELT PROF_TRAVELER_BELT ${NAME}`); await H.sleep(1500)
    bot.chat(`/mmocore admin level set ${NAME} main 30`); await H.sleep(1500)
    const base = await hp(bot); r.ok('zycie na start', base)

    bot.chat('/eq'); const panel = await H.waitForWindow(bot, { timeout: 12000 })
    if (!r.assert(!!panel, 'panel otwarty')) return
    const belt = H.findSlot(panel, /^Pas$/i)
    const inBag = panel.slots.findIndex((it, i) => i >= 54 && it && /Pas podroznika/i.test(H.itemLabel(it) || ''))
    r.ok('slot Pas w panelu / pas w plecaku', `slot=${belt && belt.slot} pas=${inBag}`)
    if (belt && inBag > 0) {
      await bot.clickWindow(inBag, 0, 0); await H.sleep(700)
      await bot.clickWindow(belt.slot, 0, 0); await H.sleep(2500)
      const label = H.itemLabel(bot.currentWindow && bot.currentWindow.slots[belt.slot])
      const after = await hp(bot)
      r.ok('slot Pas po wlozeniu', label)
      r.assert(after !== base, 'panel /eq NADAL nadaje staty (zycie sie zmienilo)', `${base} -> ${after}`)
      // i czy to samo widac pod E (slot 16 = Pas w vanilla_inventory)
      bot.closeWindow(bot.currentWindow); await H.sleep(1500)
      r.ok('slot 16 pod E po zalozeniu w panelu', H.itemLabel(bot.inventory.slots[16]) || '-')

      // DRUGI pas — tym razem do slotu pod klawiszem E. Jesli staty sie sumuja,
      // zycie urosnie o kolejne +10, czyli gracz nosi dwa pasy naraz.
      bot.chat(`/mi give BELT PROF_TRAVELER_BELT ${NAME}`); await H.sleep(2000)
      const idx2 = bot.inventory.slots.findIndex((it, i) => i >= 18 && it && /Pas podroznika/i.test(H.itemLabel(it) || ''))
      r.ok('drugi pas w plecaku', 'slot ' + idx2)
      if (idx2 > 0) {
        await bot.clickWindow(idx2, 0, 0); await H.sleep(700)
        await bot.clickWindow(16, 0, 0); await H.sleep(2500)
        const label16 = H.itemLabel(bot.inventory.slots[16])
        const after2 = await hp(bot)
        r.ok('slot 16 pod E po wlozeniu drugiego pasa', label16)
        r.ok('WYNIK ZYCIA', `start ${base} -> pas w panelu ${after} -> drugi pas pod E ${after2}`)
      }
    }
  } catch (e) { r.fail('probe', String(e && e.message || e)) }
  finally { r.print(); if (bot) await H.quit(bot); process.exit(0) }
})()
