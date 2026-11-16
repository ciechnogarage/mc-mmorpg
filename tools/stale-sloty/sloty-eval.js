'use strict'
/**
 * Eval stałych slotów ekwipunku przeciwko ŻYWEMU world1 (Scripts/stale-sloty.yml).
 *
 *  1. po kreatorze postaci: dziennik w slocie 8, sakwa w slocie 18,
 *  2. PPM dziennikiem w ręce otwiera menu główne,
 *  3. klik w dziennik / sakwę w oknie ekwipunku: dziennik -> menu, sakwa -> okno sakwy,
 *  4. przycisku nie da się podnieść ani przenieść klawiszem 9, ani wyrzucić (Q),
 *  5. sakwa przyjmuje surowiec MMOItems (MATERIAL), odrzuca zwykły przedmiot,
 *  6. po śmierci przyciski nie leżą na ziemi, a po odrodzeniu wracają na miejsce.
 */
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')

const { rcon } = require('/h/rcon.js')

const HOTBAR = i => 36 + i          // numeracja okna 0 (ekwipunek gracza) w mineflayer
const slotItem = (bot, i) => bot.inventory.slots[i]
const label = it => (it ? H.itemLabel(it) : 'pusto')

module.exports = async function (r) {
  const nick = 'EvSlot' + Date.now() % 100000
  const bot = await H.connect(nick)
  try {
    const ekran = await F.waitProfileScreen(bot)
    if (!r.assert(!!ekran, 'ekran wyboru postaci sie pokazal')) return
    if (!r.assert(!!await F.enterCreator(bot), 'kreator postaci otwarty')) return
    await F.completeCreator(bot)
    await H.sleep(4000)

    // --- 1. przyciski na miejscu -----------------------------------------------------------
    const d = slotItem(bot, HOTBAR(8)); const s = slotItem(bot, 18)
    r.assert(d && d.name === 'book' && /Dziennik/.test(label(d)), 'slot 8: dziennik', label(d))
    r.assert(s && s.name === 'leather' && /Sakwa/.test(label(s)), 'slot 18: sakwa', label(s))

    // --- 2. PPM dziennikiem ----------------------------------------------------------------
    bot.setQuickBarSlot(8); await H.sleep(500)
    bot.activateItem(); await H.sleep(200); bot.deactivateItem()
    let w = await H.waitForWindow(bot, { timeout: 6000, titleIncludes: 'Menu' })
    r.assert(!!w, 'PPM dziennikiem otwiera menu', w ? H.windowTitle(w) : 'brak okna')
    if (w) { bot.closeWindow(w); await H.sleep(800) }

    // --- 3. klik w oknie ekwipunku (okno 0) ------------------------------------------------
    await bot.clickWindow(HOTBAR(8), 0, 0)
    w = await H.waitForWindow(bot, { timeout: 6000, titleIncludes: 'Menu' })
    r.assert(!!w, 'klik w dziennik w ekwipunku otwiera menu', w ? H.windowTitle(w) : 'brak okna')
    if (w) { bot.closeWindow(w); await H.sleep(800) }
    r.assert(!bot.inventory.cursorItem && slotItem(bot, HOTBAR(8))?.name === 'book',
      'dziennik nie wisi na kursorze i zostal w slocie 8', label(bot.inventory.cursorItem))

    // --- 4. przenoszenie i wyrzucanie ------------------------------------------------------
    await bot.clickWindow(20, 8, 2)          // klawisz 9 nad slotem 20
    await H.sleep(800)
    r.assert(slotItem(bot, HOTBAR(8))?.name === 'book' && slotItem(bot, 20)?.name !== 'book',
      'klawisz 9 nie przenosi dziennika', `8=${label(slotItem(bot, HOTBAR(8)))} 20=${label(slotItem(bot, 20))}`)
    await bot.clickWindow(18, 0, 1)          // shift-klik w sakwe
    await H.sleep(800)
    w = bot.currentWindow
    if (w) { bot.closeWindow(w); await H.sleep(800) }
    r.assert(slotItem(bot, 18)?.name === 'leather', 'shift-klik nie rusza sakwy', label(slotItem(bot, 18)))
    bot.setQuickBarSlot(8); await H.sleep(400)
    bot._client.write('block_dig', { status: 4, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 })
    await H.sleep(1500)
    if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(600) }
    r.assert(slotItem(bot, HOTBAR(8))?.name === 'book', 'Q nie wyrzuca dziennika', label(slotItem(bot, HOTBAR(8))))
    const naZiemi = await rcon('minecraft:execute if entity @e[type=item,nbt={Item:{id:"minecraft:book"}}]')
    r.assert(!/passed/i.test(naZiemi), 'na ziemi nie lezy zaden dziennik', naZiemi)

    // --- 5. sakwa --------------------------------------------------------------------------
    await rcon(`mi give MATERIAL PROF_OAK_TIMBER ${nick} 3`)
    await rcon(`minecraft:give ${nick} minecraft:dirt 2`)
    await H.sleep(1500)
    await bot.clickWindow(18, 0, 0)
    w = await H.waitForWindow(bot, { timeout: 6000, titleIncludes: 'Sakwa' })
    if (r.assert(!!w, 'klik w sakwe otwiera okno sakwy', w ? H.windowTitle(w) : 'brak okna')) {
      const top = w.inventoryStart
      const znajdz = n => w.slots.findIndex((it, i) => i >= top && it && n(it))
      const drewno = znajdz(it => /PROF|Tarcica|dębow|Drewno/i.test(label(it)) || (it.nbt && JSON.stringify(it.nbt).includes('PROF_OAK_TIMBER')))
      const ziemia = znajdz(it => it.name === 'dirt')
      if (r.assert(drewno >= 0 && ziemia >= 0, 'bot ma surowiec i ziemie', H.windowSlots(w).map(x => x.slot + ':' + x.name).join(' '))) {
        await bot.clickWindow(ziemia, 0, 1); await H.sleep(900)
        const ziemiaWSakwie = w.slots.slice(0, top).some(it => it && it.name === 'dirt')
        r.assert(!ziemiaWSakwie, 'sakwa odrzuca zwykly przedmiot (ziemia)')
        await bot.clickWindow(drewno, 0, 1); await H.sleep(900)
        const surowiecWSakwie = w.slots.slice(0, top).some(it => it && it.name !== 'dirt')
        r.assert(surowiecWSakwie, 'sakwa przyjmuje surowiec MMOItems',
          w.slots.slice(0, top).filter(Boolean).map(it => label(it)).join(', ') || 'sakwa pusta')
      }
      bot.closeWindow(w); await H.sleep(800)
    }

    // --- 6. śmierć i odrodzenie ------------------------------------------------------------
    await rcon(`minecraft:kill ${nick}`)
    await H.sleep(1500)
    const zNim = await rcon('minecraft:execute if entity @e[type=item,nbt={Item:{id:"minecraft:book"}}]')
    const zNim2 = await rcon('minecraft:execute if entity @e[type=item,nbt={Item:{id:"minecraft:leather"}}]')
    r.assert(!/passed/i.test(zNim) && !/passed/i.test(zNim2), 'po smierci przyciski nie leza na ziemi', zNim + ' | ' + zNim2)
    try { bot.respawn() } catch {}
    await H.sleep(4000)
    r.assert(slotItem(bot, HOTBAR(8))?.name === 'book' && slotItem(bot, 18)?.name === 'leather',
      'po odrodzeniu przyciski wracaja', `8=${label(slotItem(bot, HOTBAR(8)))} 18=${label(slotItem(bot, 18))}`)
  } finally {
    await H.quit(bot)
  }
}
