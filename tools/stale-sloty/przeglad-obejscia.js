'use strict'
/**
 * Przegląd 2026-09-25: ścieżki obejścia stałych slotów i filtra sakwy, których nie ma
 * w sloty-eval.js. Każdy wynik czytamy PO ponownym otwarciu okna (stan serwera, nie
 * przewidywanie klienta mineflayer).
 *
 *  A. sakwa: odłożenie kursorem na pusty slot, klawisz numeryczny, przeciąganie (drag),
 *     shift-klik przedmiotu MMOItems innego typu niż MATERIAL,
 *  B. w oknie sakwy: klawisz 9 nad slotem sakwy, klik w przycisk-sakwę (slot 18),
 *  C. w ekwipunku: klawisz F (druga ręka) nad slotem 8 i 18, F z dziennikiem w ręce,
 *     podwójny klik (COLLECT_TO_CURSOR) z książką na kursorze.
 */
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')

const label = it => (it ? H.itemLabel(it) : 'pusto')
const idOf = it => {
  if (!it) return null
  const s = JSON.stringify(it.nbt || it.components || {})
  const m = s.match(/MMOITEMS_ITEM_ID[^A-Z_]*"?([A-Z0-9_]+)/)
  return m ? m[1] : it.name
}

module.exports = async function (r) {
  const nick = 'EvPrz' + Date.now() % 100000
  const bot = await H.connect(nick)
  // klik wysłany bez przewidywania mineflayer (tryby/przyciski, których nie symuluje)
  const surowy = async (sl, b, mode) => {
    const okno = bot.currentWindow || bot.inventory; const orig = okno.acceptClick; okno.acceptClick = () => {}
    try { await bot.clickWindow(sl, b, mode) } finally { okno.acceptClick = orig }
  }
  try {
    await F.waitProfileScreen(bot); await F.enterCreator(bot)
    await F.completeCreator(bot); await H.sleep(4000)
    console.log('  eq po kreatorze:', H.inventoryLabels(bot).join(' '))
    r.assert(bot.inventory.slots[44]?.name === 'book' && bot.inventory.slots[18]?.name === 'leather',
      'przyciski na miejscu po kreatorze', `8=${label(bot.inventory.slots[44])} 18=${label(bot.inventory.slots[18])}`)

    await rcon(`minecraft:clear ${nick} minecraft:dirt`)
    await rcon(`minecraft:give ${nick} minecraft:dirt 16`)
    await rcon(`mi give MATERIAL PROF_OAK_TIMBER ${nick} 3`)
    await rcon(`mi give RING TEST_WARRIOR_RING ${nick} 1`)
    await H.sleep(1500)
    console.log('  eq po give:', H.inventoryLabels(bot).join(' '))

    const otworzSakwe = async () => {
      if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(600) }
      await bot.clickWindow(18, 0, 0)
      return H.waitForWindow(bot, { timeout: 6000, titleIncludes: 'Sakwa' })
    }
    const zawartosc = w => w.slots.slice(0, w.inventoryStart).map((it, i) => it ? `${i}:${it.name}x${it.count}` : null).filter(Boolean)
    const znajdzDol = (w, pred) => w.slots.findIndex((it, i) => i >= w.inventoryStart && it && pred(it))

    // --- A. filtr sakwy -------------------------------------------------------------------
    let w = await otworzSakwe()
    if (!r.assert(!!w, 'sakwa sie otwiera')) return
    const top = w.inventoryStart
    console.log('  sakwa: gora', top, 'slotow; zawartosc na starcie:', zawartosc(w).join(' ') || 'pusto')

    // A1: ziemia kursorem na pusty slot sakwy
    let d = znajdzDol(w, it => it.name === 'dirt')
    await bot.clickWindow(d, 0, 0); await H.sleep(400)        // podnieś
    await bot.clickWindow(0, 0, 0); await H.sleep(700)        // odłóż na slot 0 sakwy
    if (bot.currentWindow?.selectedItem) { await bot.clickWindow(d, 0, 0); await H.sleep(500) }
    w = await otworzSakwe()
    let z = zawartosc(w)
    r.assert(!z.some(s => s.includes('dirt')), 'A1: ziemi nie da sie odlozyc kursorem na pusty slot sakwy', z.join(' ') || 'pusto')

    // A2: klawisz numeryczny — ziemia w hotbarze, kursor nad pustym slotem sakwy
    d = znajdzDol(w, it => it.name === 'dirt')
    const hotStart = w.slots.length - 9
    let hot = d >= hotStart ? d - hotStart : -1
    if (hot < 0) {
      // przenieś ziemię do wolnego slotu hotbara
      const wolny = [0, 1, 2, 3, 4, 5, 6, 7].find(j => !w.slots[hotStart + j])
      if (wolny != null) { await bot.clickWindow(d, 0, 0); await H.sleep(300); await bot.clickWindow(hotStart + wolny, 0, 0); await H.sleep(500); hot = wolny }
    }
    console.log('  A2: ziemia w hotbarze', hot)
    if (hot >= 0) {
      await bot.clickWindow(1, hot, 2); await H.sleep(700)
      w = await otworzSakwe(); z = zawartosc(w)
      r.assert(!z.some(s => s.includes('dirt')), 'A2: klawisz numeryczny nie wklada ziemi do sakwy', z.join(' ') || 'pusto')
    } else r.fail('A2: brak wolnego slotu hotbara na ziemie')

    // A3: przeciąganie (drag) ziemią po slotach sakwy
    d = znajdzDol(w, it => it.name === 'dirt')
    await bot.clickWindow(d, 0, 0); await H.sleep(400)
    { // mineflayer nie symuluje trybu 5 — wysyłamy pakiety bez przewidywania po stronie klienta
      const okno = bot.currentWindow; const orig = okno.acceptClick; okno.acceptClick = () => {}
      try { for (const [sl, b] of [[-999, 0], [2, 1], [3, 1], [-999, 2]]) { await bot.clickWindow(sl, b, 5); await H.sleep(150) } }
      finally { okno.acceptClick = orig }
    }
    await H.sleep(700)
    if (bot.currentWindow?.selectedItem) { const wol = w.slots.findIndex((it, i) => i >= top && !it); if (wol >= 0) { await bot.clickWindow(wol, 0, 0); await H.sleep(500) } }
    w = await otworzSakwe(); z = zawartosc(w)
    r.assert(!z.some(s => s.includes('dirt')), 'A3: przeciaganie nie wklada ziemi do sakwy', z.join(' ') || 'pusto')

    // A4: shift-klik pierścienia MMOItems (typ RING, nie MATERIAL)
    const ring = znajdzDol(w, it => it.name !== 'dirt' && /Pier|Ring|TEST/i.test(label(it)))
    if (r.assert(ring >= 0, 'bot ma pierscien testowy', H.windowSlots(w).map(x => x.slot + ':' + x.label).join(' | '))) {
      await bot.clickWindow(ring, 0, 1); await H.sleep(700)
      w = await otworzSakwe(); z = zawartosc(w)
      r.assert(z.length === 0 || !w.slots.slice(0, top).some(it => it && it.name !== 'dirt' && /Pier|Ring|TEST/i.test(label(it))),
        'A4: shift-klik pierscienia (MMOItems, nie MATERIAL) odrzucony', z.join(' ') || 'pusto')
    }

    // A5: surowiec kursorem na pusty slot (ścieżka pozytywna)
    const drewno = znajdzDol(w, it => /PROF|Tarcica|Drewno|dęb/i.test(label(it)))
    if (drewno >= 0) {
      await bot.clickWindow(drewno, 0, 0); await H.sleep(400)
      await bot.clickWindow(5, 0, 0); await H.sleep(700)
      if (bot.currentWindow?.selectedItem) { await bot.clickWindow(drewno, 0, 0); await H.sleep(500) }
      w = await otworzSakwe(); z = zawartosc(w)
      r.assert(z.some(s => s.startsWith('5:')), 'A5: surowiec kursorem na pusty slot sakwy przyjety', z.join(' ') || 'pusto')
    } else r.fail('A5: brak surowca u bota')

    // --- B. przyciski przy otwartej sakwie ------------------------------------------------
    await bot.clickWindow(8, 8, 2); await H.sleep(700)          // klawisz 9 nad slotem 8 sakwy
    await bot.clickWindow(top + 9, 0, 0); await H.sleep(700)    // klik w przycisk-sakwę (slot 18 gracza)
    if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(800) }
    const s8 = bot.inventory.slots[44], s18 = bot.inventory.slots[18]
    r.assert(s8?.name === 'book' && s18?.name === 'leather' && !bot.inventory.selectedItem,
      'B: w oknie sakwy przyciski nie daja sie ruszyc (klawisz 9, klik)', `8=${label(s8)} 18=${label(s18)} kursor=${label(bot.inventory.selectedItem)}`)

    // --- C. ekwipunek (okno 0) --------------------------------------------------------------
    await surowy(44, 40, 2); await H.sleep(800)   // F nad slotem 8
    if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(800) }
    await surowy(18, 40, 2); await H.sleep(800)   // F nad slotem 18
    if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(800) }
    const off = await rcon(`minecraft:execute if items entity ${nick} weapon.offhand *`)
    r.assert(bot.inventory.slots[44]?.name === 'book' && bot.inventory.slots[18]?.name === 'leather' && /failed/i.test(off),
      'C1: klawisz F w ekwipunku nie przenosi przyciskow do drugiej reki', `8=${label(bot.inventory.slots[44])} 18=${label(bot.inventory.slots[18])} offhand:${off.trim()}`)

    bot.setQuickBarSlot(8); await H.sleep(400)
    bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 })
    await H.sleep(1000)
    const off2 = await rcon(`minecraft:execute if items entity ${nick} weapon.offhand *`)
    r.assert(/failed/i.test(off2), 'C2: F z dziennikiem w rece nie przenosi go do drugiej reki', off2.trim())
    bot.setQuickBarSlot(0); await H.sleep(300)

    // C3: podwójny klik — zwykła książka na kursorze, dziennik (BOOK) w slocie 8
    await rcon(`minecraft:give ${nick} minecraft:book 1`); await H.sleep(1000)
    const ks = bot.inventory.slots.findIndex((it, i) => i !== 44 && it && it.name === 'book')
    if (ks >= 0) {
      await bot.clickWindow(ks, 0, 0); await H.sleep(300)
      await surowy(ks, 0, 6); await H.sleep(800)       // COLLECT_TO_CURSOR
      if (bot.inventory.selectedItem) { await bot.clickWindow(ks, 0, 0); await H.sleep(500) }
      const liczba = await rcon(`minecraft:execute if items entity ${nick} hotbar.8 minecraft:book`)
      r.assert(/passed/i.test(liczba), 'C3: podwojny klik ksiazka nie zabiera dziennika ze slotu 8', liczba.trim())
    } else r.fail('C3: brak ksiazki u bota')

    // --- D. gdzie leży sakwa (per postać?) -----------------------------------------------
    const pid = (await rcon(`papi parse ${nick} %player_uuid%`)).trim()
    console.log('  UUID gracza (MMOProfiles zwraca tu id profilu?):', pid)
  } finally {
    await H.quit(bot)
  }
}
