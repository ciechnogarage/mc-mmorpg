'use strict'
/**
 * Czy sloty pod klawiszem E i panel /eq to TEN SAM ekwipunek, czy dwa osobne?
 *
 * Test mierzy, nie zaklada: bot zaklada koszule (+6 max health) w slocie pod E, potem
 * DRUGA taka sama w slocie SHIRT panelu /eq. Jesli to jeden zbior danych — druga koszula
 * nie wejdzie albo podmieni pierwsza, a zycie wzrosnie raz. Jesli dwa — zycie wzrosnie dwa razy.
 */
const H = require('./lib/harness')
const F = require('./lib/flow')

const NAME = process.env.EQ_BOT || 'EqDblQA'
const SHIRT_SLOT_E = 15

const hp = bot => new Promise(res => {
  const h = m => { const t = m.toString(); if (/^HP=/.test(t)) { bot.removeListener('message', h); res(t) } }
  bot.on('message', h)
  bot.chat(`/papi parse ${NAME} HP=%mythiclib_stat_max_health%`)
  setTimeout(() => { bot.removeListener('message', h); res('brak odpowiedzi') }, 4000)
})

;(async () => {
  const r = new H.Results('eq-podwojne')
  let bot
  try {
    bot = await H.connect(NAME)
    const prof = await F.waitProfileScreen(bot, 20000)
    if (prof) { const w = await F.enterCreator(bot, 20000); if (w) { await F.completeCreator(bot); await H.sleep(3000) } }
    await H.sleep(3000)

    bot.chat(`/mi give SHIRT PROF_LINEN_SHIRT ${NAME}`); await H.sleep(1000)
    bot.chat(`/mi give SHIRT PROF_LINEN_SHIRT ${NAME}`); await H.sleep(1500)
    const base = await hp(bot)
    r.ok('zycie na start (bez ekwipunku)', base)

    // 1) koszula w slocie pod klawiszem E
    let idx = bot.inventory.slots.findIndex((it, i) => i >= 18 && it && /Lniana koszula/i.test(H.itemLabel(it) || ''))
    await bot.clickWindow(idx, 0, 0); await H.sleep(600)
    await bot.clickWindow(SHIRT_SLOT_E, 0, 0); await H.sleep(2000)
    const afterE = await hp(bot)
    r.assert(/Lniana koszula/i.test(H.itemLabel(bot.inventory.slots[SHIRT_SLOT_E]) || ''),
      'koszula #1 w slocie pod E', 'zycie: ' + afterE)

    // 2) druga koszula w slocie SHIRT panelu /eq
    bot.chat('/eq')
    const panel = await H.waitForWindow(bot, { timeout: 12000 })
    if (r.assert(!!panel, 'panel /eq otwarty')) {
      const shirtSlot = H.findSlot(panel, /Koszula/i)
      r.ok('slot Koszula w panelu', shirtSlot ? `nr ${shirtSlot.slot}, zawartosc: ${shirtSlot.label}` : 'NIE ZNALEZIONO')
      // czy panel POKAZUJE juz zalozona koszule (jeden zbior) czy pusta ikone (dwa zbiory)?
      r.ok('panel pokazuje w tym slocie', shirtSlot ? shirtSlot.label : '-')
      const inPanelInv = panel.slots.findIndex((it, i) => i >= 54 && it && /Lniana koszula/i.test(H.itemLabel(it) || ''))
      if (shirtSlot && inPanelInv > 0) {
        await bot.clickWindow(inPanelInv, 0, 0); await H.sleep(600)
        await bot.clickWindow(shirtSlot.slot, 0, 0); await H.sleep(2000)
        const nowLabel = H.itemLabel(bot.currentWindow && bot.currentWindow.slots[shirtSlot.slot])
        const afterPanel = await hp(bot)
        r.ok('po wlozeniu koszuli #2 do panelu', `slot panelu: ${nowLabel}, zycie: ${afterPanel}`)
        r.ok('PODSUMOWANIE zycia', `start ${base} -> po E ${afterE} -> po panelu ${afterPanel}`)
      } else {
        r.fail('nie udalo sie wlozyc koszuli #2 w panelu', `slotPanelu=${!!shirtSlot} koszulaWPlecaku=${inPanelInv}`)
      }
      bot.closeWindow(panel)
    }
  } catch (e) { r.fail('probe wywrocil sie', String(e && e.message || e)) }
  finally { r.print(); if (bot) await H.quit(bot); process.exit(0) }
})()
