'use strict'
/**
 * Czy gracz moze nosic WIECEJ pierscieni, niz przewiduje projekt?
 * Sloty RING sa i pod klawiszem E (vanilla_inventory), i w panelu /eq (custom).
 * Bot zaklada 2 pierscienie pod E i 2 w panelu, mierzac max mane (+15 za sztuke).
 */
const H = require('./lib/harness'); const F = require('./lib/flow')
const NAME = process.env.EQ_BOT || 'EqRingQA'
const RING1_E = 12, RING2_E = 13

const mana = bot => new Promise(res => {
  const h = m => { const t = m.toString(); if (/^MANA=/.test(t)) { bot.removeListener('message', h); res(t.trim()) } }
  bot.on('message', h)
  bot.chat(`/papi parse ${NAME} MANA=%mythiclib_stat_max_mana%`)
  setTimeout(() => { bot.removeListener('message', h); res('brak') }, 4000)
})
const findRingInBag = (bot, from) => bot.inventory.slots.findIndex((it, i) =>
  i >= from && it && /Srebrny sygnet/i.test(H.itemLabel(it) || ''))

;(async () => {
  const r = new H.Results('cztery-pierscienie'); let bot
  try {
    bot = await H.connect(NAME)
    // Ekran wyboru postaci: konto z postacia ma ja w slocie 10, konto puste ma tam
    // przycisk tworzenia. Klikamy slot 10 i rozpoznajemy po tytule, co sie otworzylo.
    const prof = await F.waitProfileScreen(bot, 20000)
    if (prof) {
      await H.click(bot, 10)
      await H.sleep(2500)
      if (F.isCreatorWindow(bot)) { await F.completeCreator(bot); await H.sleep(3000) }
    }
    await H.sleep(5000)
    bot.chat(`/mmocore admin level set ${NAME} main 30`); await H.sleep(1500)
    for (let i = 0; i < 4; i++) { bot.chat(`/mi give RING PROF_SILVER_SIGNET ${NAME}`); await H.sleep(900) }
    await H.sleep(2500)

    // diagnostyka: poziom wybranej postaci i zawartosc plecaka
    const lvl = await new Promise(res => {
      const h = m => { const t = m.toString(); if (/^LVL=/.test(t)) { bot.removeListener('message', h); res(t.trim()) } }
      bot.on('message', h); bot.chat(`/papi parse ${NAME} LVL=%mmocore_level%`)
      setTimeout(() => { bot.removeListener('message', h); res('brak') }, 4000)
    })
    r.ok('poziom wybranej postaci', lvl)
    r.ok('plecak po wydaniu przedmiotow', bot.inventory.slots.map((it,i)=>it&&i>=9?`${i}:${H.itemLabel(it)}`:null).filter(Boolean).join(' | ').slice(0,400) || 'pusty')
    r.ok('mana na start', await mana(bot))
    for (const slot of [RING1_E, RING2_E]) {
      const idx = findRingInBag(bot, 18)
      if (idx < 0) break
      await bot.clickWindow(idx, 0, 0); await H.sleep(600)
      await bot.clickWindow(slot, 0, 0); await H.sleep(1500)
    }
    const afterE = await mana(bot)
    r.ok('po 2 pierscieniach pod E', `${afterE} | sloty: ${H.itemLabel(bot.inventory.slots[RING1_E])} / ${H.itemLabel(bot.inventory.slots[RING2_E])}`)

    bot.chat('/eq'); const panel = await H.waitForWindow(bot, { timeout: 12000 })
    if (r.assert(!!panel, 'panel /eq otwarty')) {
      for (const re of [/Ring 1/i, /Ring 2/i]) {
        const s = H.findSlot(panel, re); if (!s) continue
        const idx = bot.currentWindow.slots.findIndex((it, i) => i >= 54 && it && /Srebrny sygnet/i.test(H.itemLabel(it) || ''))
        if (idx < 0) continue
        await bot.clickWindow(idx, 0, 0); await H.sleep(600)
        await bot.clickWindow(s.slot, 0, 0); await H.sleep(1500)
      }
      const p = bot.currentWindow
      const r1 = H.findSlot(p, /Ring 1|Srebrny sygnet/i)
      r.ok('sloty pierscieni w panelu po probie', H.windowSlots(p).filter(s => /Ring|Srebrny/i.test(s.label || '')).map(s => `${s.slot}:${s.label}`).join(' | '))
      const afterPanel = await mana(bot)
      r.ok('WYNIK', `start -> po E ${afterE} -> po panelu ${afterPanel}`)
      bot.closeWindow(p)
    }
  } catch (e) { r.fail('probe wywrocil sie', String(e && e.message || e)) }
  finally { r.print(); if (bot) await H.quit(bot); process.exit(0) }
})()
