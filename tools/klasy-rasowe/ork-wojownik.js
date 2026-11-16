'use strict'
/**
 * Prototyp klas rasowych: Ork × Wojownik = Wódz Wojny -> (10 poz.) Tyran / Krwioburca.
 * Mierzy (PASS/FAIL + wartości):
 *  1. biegłości zbroi Wodza Wojny (atrybuty z klasy): lekka 1, średnia 1, ciężka 0,
 *  2. co robi MMOItems, gdy wymaganie biegłości NIE jest spełnione (ciężka zbroja),
 *  3. wybór specjalizacji w oknie /class na 10 poziomie — czy poziom zostaje,
 *  4. Tyran ma ciężką biegłość i ciężka zbroja działa,
 *  5. zmiana specjalizacji (class force) — poziom i co się dzieje z założoną ciężką zbroją.
 * Konsola tylko do przygotowania (poziomy, przedmioty) i odczytu placeholderów.
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')

module.exports = async function (r) {
  const nick = 'EvOrk' + Date.now() % 100000
  const bot = await H.connect(nick)
  const czat = []
  bot.on('messagestr', m => { czat.push(m); console.log('  czat:', m) })
  const ph = async (...p) => {
    const out = await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)
    return out.trim().split('|')
  }
  const stan = async etykieta => {
    const [k, lvl, l, s, c, arm] = await ph('mmocore_class_id', 'mmocore_level',
      'mmocore_attribute_bieglosc_lekka', 'mmocore_attribute_bieglosc_srednia',
      'mmocore_attribute_bieglosc_ciezka', 'mythiclib_stat_armor')
    console.log(`  [${etykieta}] klasa=${k} poziom=${lvl} bieglosci L/S/C=${l}/${s}/${c} pancerz=${arm}`)
    return { k, lvl: +lvl, l: +l, s: +s, c: +c, arm: +arm }
  }
  const klata = () => bot.inventory.slots[6]
  const zaloz = async id => {
    const it = bot.inventory.items().find(i => JSON.stringify(i.nbt || {}).includes(id) || (H.itemLabel(i) || '').includes(id))
    if (!it) return 'brak przedmiotu'
    try { await bot.equip(it, 'torso') } catch (e) { return 'equip: ' + e.message }
    await H.sleep(2000)
    return klata() ? H.itemLabel(klata()) : 'pusto'
  }
  const zdejmij = async () => { if (klata()) { try { await bot.unequip('torso') } catch {} await H.sleep(1200) } }
  try {
    await F.waitProfileScreen(bot); await F.enterCreator(bot)
    await F.completeCreator(bot, { race: 'ork', klass: 'wojownik' }); await H.sleep(4000)

    // --- 1. Wódz Wojny -----------------------------------------------------------------------
    await rcon(`mmocore class select ${nick} WODZ_WOJNY`); await H.sleep(1500)
    // od 2026-09-26 klasa dostaje zestaw startowy z ZAŁOŻONĄ kolczugą — test zbroi zaczyna bez niej
    for (const sl of ['head', 'chest', 'legs', 'feet']) await rcon(`minecraft:item replace entity ${nick} armor.${sl} with minecraft:air`)
    await H.sleep(800)
    let s = await stan('po nadaniu klasy')
    r.assert(s.k.toUpperCase() === 'WODZ_WOJNY', 'klasa to Wodz Wojny', s.k)
    r.assert(s.l === 1 && s.s === 1 && s.c === 0, 'Wodz Wojny: bieglosci lekka 1, srednia 1, ciezka 0', `${s.l}/${s.s}/${s.c}`)

    // --- 2. zbroje ---------------------------------------------------------------------------
    for (const id of ['TEST_ZBROJA_SREDNIA', 'TEST_ZBROJA_CIEZKA']) await rcon(`mi give ARMOR ${id} ${nick} 1`)
    await H.sleep(1500)
    const bez = (await stan('bez zbroi')).arm
    console.log('  zakladam srednia ->', await zaloz('Kolczuga'))
    const zSrednia = (await stan('w sredniej')).arm
    r.assert(zSrednia > bez, 'srednia zbroja (spelnione wymaganie) daje pancerz', `${bez} -> ${zSrednia}`)
    await zdejmij()
    const przed = czat.length
    const wynik = await zaloz('płytowy')
    // odrzucony przedmiot zostaje botowi na kursorze — odkładamy go do plecaka
    if (bot.inventory.cursorItem) { const wolny = bot.inventory.firstEmptyInventorySlot(); if (wolny != null) { await bot.clickWindow(wolny, 0, 0); await H.sleep(600) } }
    const zCiezka = (await stan('proba ciezkiej')).arm
    const wPlecaku = bot.inventory.items().filter(i => i.name === 'iron_chestplate').length
    const naZiemi = await rcon('minecraft:execute if entity @e[type=item,nbt={Item:{id:"minecraft:iron_chestplate"}}]')
    const naSerwerze = await rcon(`minecraft:clear ${nick} minecraft:iron_chestplate 0`)
    console.log('  SERWER (clear ... 0 = tylko liczy):', naSerwerze.trim())
    const naKlacie = await rcon(`minecraft:execute if items entity ${nick} armor.chest minecraft:iron_chestplate`)
    console.log('  SERWER slot klaty:', naKlacie.trim())
    r.assert(/failed/i.test(naKlacie), 'odrzucony napiersnik NIE siedzi w slocie klaty (serwer)', naKlacie.trim())
    r.assert(/Found [1-9]/.test(naSerwerze), 'odrzucony napiersnik zostaje u gracza po stronie serwera', naSerwerze.trim())
    console.log(`  GDZIE JEST ODRZUCONY NAPIERSNIK: plecak=${wPlecaku} kursor=${bot.inventory.cursorItem ? bot.inventory.cursorItem.name : '-'} ziemia=${naZiemi.trim()} wolnych=${bot.inventory.emptySlotCount()}`)
    console.log('  NIESPELNIONE WYMAGANIE: w slocie klaty =', wynik, '| pancerz', bez, '->', zCiezka,
      '| czat:', czat.slice(przed).join(' / ') || '(nic)')
    r.assert(true, `zmierzone: ciezka bez bieglosci -> slot="${wynik}", pancerz ${bez}->${zCiezka}`)
    await zdejmij()

    // --- 3. poziom 10 i wybór specjalizacji w oknie ----------------------------------------
    await rcon(`mmocore admin level give ${nick} main 9`)
    // punkt klasy NIE jest już dawany z konsoli — ma przyjść z tabeli nagród za 10 poziom
    await H.sleep(1500)
    s = await stan('po awansie')
    // nagrody za poziomy 2..10 (exp-tables/default_exp_tables.yml -> class_exp_table)
    const [pAtr, pDrz, pKl, pUm] = await ph('mmocore_attribute_points', 'mmocore_skill_tree_points_global',
      'mmocore_class_points', 'mmocore_skill_points')
    console.log(`  PUNKTY po 10 poz.: atrybuty=${pAtr} drzewko=${pDrz} klasa=${pKl} umiejetnosci=${pUm}`)
    r.assert(pAtr === '9', 'punkt atrybutu co poziom (2..10 = 9)', pAtr)
    r.assert(pDrz === '5', 'punkt drzewka co 2 poziomy (2,4,6,8,10 = 5)', pDrz)
    r.assert(pKl === '1', 'punkt klasy raz, na 10 poziomie', pKl)
    r.assert(s.lvl >= 10, 'poziom 10', s.lvl)
    bot.chat('/class')
    let w = await H.waitForWindow(bot, { timeout: 8000 })
    const tytul = w ? H.windowTitle(w) : 'brak okna'
    r.assert(!!w && /specjalizac/i.test(tytul), '/class na 10 poz. otwiera wybor specjalizacji', tytul)
    if (w) {
      console.log('  sloty:', H.windowSlots(w).filter(x => x.slot < 27).map(x => `${x.slot}:${x.label}`).join(' | '))
      const tyran = H.findSlot(w, /Tyran/)
      if (r.assert(!!tyran, 'kafelek Tyrana jest w oknie')) {
        await H.click(bot, tyran.slot)
        const pot = await H.waitForWindowChange(bot, w, { timeout: 6000 })
        const pt = pot ? H.windowTitle(pot) : 'brak'
        console.log('  potwierdzenie:', pt, H.windowSlots(pot).filter(x => x.slot < 27).map(x => `${x.slot}:${x.label}`).join(' | '))
        const tak = pot && H.findSlot(pot, /Select|Wybierz|Tyran/)
        if (tak) { await H.click(bot, tak.slot); await H.sleep(2000) }
      }
      if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(600) }
    }
    s = await stan('po wyborze')
    const [pktPo] = await ph('mmocore_class_points')
    r.assert(pktPo === '0', 'wybor specjalizacji zuzywa punkt klasy (1 -> 0)', pktPo)
    r.assert(s.k.toUpperCase() === 'TYRAN', 'klasa po wyborze to Tyran', s.k)
    r.assert(s.lvl >= 10, 'wybor specjalizacji NIE zeruje poziomu', s.lvl)
    r.assert(s.c === 1, 'Tyran ma bieglosc ciezka', s.c)

    // --- 4. Tyran w ciężkiej zbroi ---------------------------------------------------------
    await rcon(`mi give ARMOR TEST_ZBROJA_CIEZKA ${nick} 1`); await H.sleep(1500)
    const bez2 = (await stan('Tyran bez zbroi')).arm
    console.log('  zakladam ciezka ->', await zaloz('płytowy'))
    const zCiezka2 = (await stan('Tyran w ciezkiej')).arm
    r.assert(zCiezka2 > bez2, 'Tyran: ciezka zbroja daje pancerz', `${bez2} -> ${zCiezka2}`)

    // --- 5. zmiana specjalizacji z ciężką zbroją na sobie ------------------------------------
    await rcon(`mmocore class force ${nick} KRWIOBURCA`); await H.sleep(2500)
    s = await stan('po zmianie na Krwioburce')
    r.assert(s.k.toUpperCase() === 'KRWIOBURCA', 'zmiana specjalizacji dziala', s.k)
    r.assert(s.lvl >= 10, 'zmiana specjalizacji NIE zeruje poziomu', s.lvl)
    console.log('  ZALOZONA CIEZKA PO UTRACIE BIEGLOSCI: slot =', klata() ? H.itemLabel(klata()) : 'pusto', '| pancerz', s.arm)
    r.assert(true, `zmierzone: ciezka po utracie bieglosci -> slot="${klata() ? H.itemLabel(klata()) : 'pusto'}", pancerz ${s.arm}`)
  } finally {
    await H.quit(bot)
  }
}
