'use strict'
/**
 * Czy sloty ekwipunku PRZYJMUJA nasze przedmioty — protokolem, nie mysza.
 *
 * Sprawdza dwie drogi naraz:
 *  1) sloty w EKWIPUNKU VANILLA (to, co gracz widzi pod klawiszem E) — MMOInventory
 *     'vanilla_inventory.yml' sadzi je w gornym rzedzie plecaka, czyli w slotach 9-17
 *     okna 0 (numeracja Bukkita i protokolu pokrywaja sie dla plecaka),
 *  2) panel /eq (custom inventory 'mmoinventory1_legacy') — osobne okno.
 *
 * Bot musi byc OP (nadaje sie go z rcon przed startem), bo sam sobie wydaje przedmioty
 * i ustawia poziom — inaczej itemy z required-level nie wchodza do slotow.
 */
const H = require('./lib/harness')
const F = require('./lib/flow')

const NAME = process.env.EQ_BOT || 'EqSlotQA'
const SHIRT_SLOT = 15   // vanilla_inventory.yml: SHIRT
const BELT_SLOT = 16    // vanilla_inventory.yml: BELT

;(async () => {
  const r = new H.Results('eq-sloty')
  let bot
  try {
    bot = await H.connect(NAME)
    const prof = await F.waitProfileScreen(bot, 20000)
    if (prof) {
      const w = await F.enterCreator(bot, 20000)
      if (w) { await F.completeCreator(bot); await H.sleep(3000) }
    }
    await H.sleep(3000)

    bot.chat(`/mmocore admin level set ${NAME} main 30`)
    await H.sleep(1200)
    for (const [type, id] of [['SHIRT', 'PROF_LINEN_SHIRT'], ['BELT', 'PROF_TRAVELER_BELT'],
                              ['RING', 'PROF_SILVER_SIGNET'], ['CHARM', 'PROF_LUCK_CHARM']]) {
      bot.chat(`/mi give ${type} ${id} ${NAME}`)
      await H.sleep(900)
    }
    await H.sleep(2500)

    // --- 1) sloty pod klawiszem E ---
    const slot9to17 = bot.inventory.slots.slice(9, 18)
      .map((it, i) => `${9 + i}:${it ? H.itemLabel(it) : 'pusty'}`)
    r.assert(slot9to17.filter(s => !s.endsWith('pusty')).length === 9,
      'w plecaku (klawisz E) stoi 9 ikon slotow akcesoriow', slot9to17.join(' | '))

    // znajdz koszule w hotbarze/plecaku
    const shirt = bot.inventory.slots.findIndex((it, i) =>
      i >= 18 && it && /Lniana koszula/i.test(H.itemLabel(it) || ''))
    if (r.assert(shirt > 0, 'koszula jest w ekwipunku bota', 'slot ' + shirt)) {
      await bot.clickWindow(shirt, 0, 0); await H.sleep(700)
      await bot.clickWindow(SHIRT_SLOT, 0, 0); await H.sleep(1500)
      const inSlot = H.itemLabel(bot.inventory.slots[SHIRT_SLOT])
      r.assert(/Lniana koszula/i.test(inSlot || ''),
        'koszula ZALOZONA w slocie SHIRT pod klawiszem E', 'slot 15: ' + inSlot)
    }

    // --- 2) panel /eq ---
    bot.chat('/eq')
    const panel = await H.waitForWindow(bot, { timeout: 12000 })
    if (r.assert(!!panel, 'panel /eq otworzyl sie botowi (gracz bez OP tez ma prawo)')) {
      const slots = H.windowSlots(panel)
      r.assert(slots.length >= 13, 'panel ma komplet slotow/ikon', 'zajetych pol: ' + slots.length)
      const names = slots.map(s => s.label).filter(Boolean).slice(0, 20)
      r.assert(names.some(n => /Naszyjnik|Ring|Pas|Charm|Glowa|Klata/i.test(n)),
        'ikony slotow maja polskie nazwy', names.join(', ').slice(0, 220))
      bot.closeWindow(panel)
    }
  } catch (e) {
    r.fail('probe wywrocil sie', String(e && e.message || e))
  } finally {
    r.print()
    if (bot) await H.quit(bot)
    process.exit(r.failed ? 1 : 0)
  }
})()
