'use strict'
/**
 * KAŻDY slot ekwipunku z perspektywy gracza (bot bez OP; konsola: klasa, poziom, przedmioty, odczyt).
 *  A. 4 sloty zbroi × biegłość: Rycerz (L+Ś) — średnia wchodzi, ciężka odrzucona; Mag Akademii
 *     (tkanina) — lekka odrzucona. Stan slotu sprawdza SERWER (execute if items).
 *  B. druga ręka: tarcza (pancerz) i grimuar (obr. magiczne) — czy statystyka działa bez/z biegłością.
 *  C. łuk: czy strzała wylatuje bez biegłości (Rycerz) i z nią (Agent).
 *  D. 9 slotów akcesoriów MMOInventory (9..17): przedmiot zostaje w slocie i dodaje statystyki — bez
 *     blokady klasy (akcesoria są wspólne).
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
const SLOT = { head: 5, torso: 6, legs: 7, feet: 8 }
const SERW = { head: 'armor.head', torso: 'armor.chest', legs: 'armor.legs', feet: 'armor.feet' }
const CZESC = { head: 'HELM', torso: null, legs: 'NOGI', feet: 'BUTY' }

module.exports = async function (r) {
  const nick = 'EvEq' + Date.now() % 100000
  const bot = await H.connect(nick)
  const czat = []; bot.on('messagestr', m => { if (!m.includes('❤')) czat.push(m) })
  const ph = async (...p) => (await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)).trim().split('|')
  const odloz = async () => { if (bot.inventory.cursorItem) { const w = bot.inventory.firstEmptyInventorySlot(); if (w != null) { await bot.clickWindow(w, 0, 0); await H.sleep(400) } } }
  const klasa = async id => { await rcon(`mmocore class select ${nick} ${id}`); await H.sleep(800); await rcon(`mmocore admin level give ${nick} main 40`); await H.sleep(800) }
  const wyczysc = async () => { await rcon(`minecraft:clear ${nick}`); await H.sleep(800) }
  const dajZbroje = async (waga, slot) => {
    const id = CZESC[slot] ? `TEST_ZBROJA_${waga}_${CZESC[slot]}` : `TEST_ZBROJA_${waga}`
    await rcon(`mi give ARMOR ${id} ${nick} 1`); await H.sleep(1000)
    return bot.inventory.items().find(i => (H.itemLabel(i) || '').includes('[TEST]'))
  }
  const zaloz = async (waga, slot) => {
    const it = await dajZbroje(waga, slot)
    if (!it) return 'brak przedmiotu'
    try { await bot.equip(it, slot) } catch (e) { /* odrzucenie bywa wyjątkiem */ }
    await H.sleep(1500); await odloz()
    const s = await rcon(`minecraft:execute if items entity ${nick} ${SERW[slot]} *`)
    return /passed/i.test(s) ? 'zalozony' : 'odrzucony'
  }
  try {
    await F.enterCreator(bot); await F.completeCreator(bot, { race: 'czlowiek', klass: 'wojownik' }); await H.sleep(4000)

    // --- A. zbroja ------------------------------------------------------------------------------
    await klasa('RYCERZ'); await wyczysc()
    for (const slot of Object.keys(SLOT)) {
      const c = await zaloz('CIEZKA', slot); await wyczysc()
      const s = await zaloz('SREDNIA', slot); await wyczysc()
      r.assert(c === 'odrzucony', `Rycerz ${slot}: ciezka (brak bieglosci) odrzucona`, c)
      r.assert(s === 'zalozony', `Rycerz ${slot}: srednia (ma bieglosc) zalozona`, s)
    }
    await klasa('MAG_AKADEMII')
    for (const slot of Object.keys(SLOT)) {
      const l = await zaloz('LEKKA', slot); await wyczysc()
      r.assert(l === 'odrzucony', `Mag Akademii ${slot}: lekka (tylko tkanina) odrzucona`, l)
    }

    // --- B. druga ręka -------------------------------------------------------------------------
    const drugaReka = async (typ, id, statKlucz, etykieta) => {
      await wyczysc(); const [przed] = await ph(statKlucz)
      await rcon(`mi give ${typ} ${id} ${nick} 1`); await H.sleep(1000)
      const it = bot.inventory.items().find(i => !/air/.test(i.name))
      if (!it) return { blad: 'brak przedmiotu' }
      try { await bot.equip(it, 'off-hand') } catch (e) { return { blad: e.message } }
      await H.sleep(1500); await odloz()
      const wRece = /passed/i.test(await rcon(`minecraft:execute if items entity ${nick} weapon.offhand *`))
      const [po] = await ph(statKlucz)
      console.log(`  [${etykieta}] ${id} w drugiej rece=${wRece} ${statKlucz}: ${przed} -> ${po}`)
      return { wRece, przed: +przed, po: +po }
    }
    await klasa('RYCERZ')
    const t0 = await drugaReka('SHIELD', 'PALADIN_SHIELD', 'mythiclib_stat_armor', 'Rycerz bez tarczy')
    await klasa('BASTION')
    const t1 = await drugaReka('SHIELD', 'PALADIN_SHIELD', 'mythiclib_stat_armor', 'Bastion z tarcza')
    r.assert(!t0.blad && t0.po === t0.przed, 'tarcza bez bieglosci: pancerz sie NIE dodaje', t0.blad || `${t0.przed}->${t0.po} w rece=${t0.wRece}`)
    r.assert(!t1.blad && t1.po > t1.przed, 'tarcza z bieglosci: pancerz sie dodaje', t1.blad || `${t1.przed}->${t1.po}`)
    await klasa('RYCERZ')
    const g0 = await drugaReka('TOME', 'AMPLIFICATION_TOME', 'mythiclib_stat_magic_damage', 'Rycerz grimuar')
    await klasa('MAG_AKADEMII')
    const g1 = await drugaReka('TOME', 'AMPLIFICATION_TOME', 'mythiclib_stat_magic_damage', 'Mag Akademii grimuar')
    r.assert(!g0.blad && g0.po === g0.przed, 'grimuar bez bieglosci: obr. magiczne sie NIE dodaja', g0.blad || `${g0.przed}->${g0.po}`)
    r.assert(!g1.blad && g1.po > g1.przed, 'grimuar z bieglosci: obr. magiczne sie dodaja', g1.blad || `${g1.przed}->${g1.po}`)

    // --- C. łuk ----------------------------------------------------------------------------------
    const strzal = async etykieta => {
      await wyczysc()
      await rcon(`mi give BOW COMPOSITE_BOW ${nick} 1`); await rcon(`minecraft:give ${nick} minecraft:arrow 16`); await H.sleep(1200)
      const luk = bot.inventory.items().find(i => i.name === 'bow')
      if (!luk) return 'brak luku'
      await bot.equip(luk, 'hand'); await H.sleep(800)
      await rcon(`minecraft:execute at ${nick} run minecraft:kill @e[type=arrow,distance=..30]`)
      const c0 = czat.length
      bot.activateItem(); await H.sleep(1300); bot.deactivateItem(); await H.sleep(700)
      const lot = await rcon(`minecraft:execute at ${nick} if entity @e[type=arrow,distance=..40]`)
      const wynik = /passed/i.test(lot) ? 'strzala wyleciala' : 'brak strzaly'
      console.log(`  [${etykieta}] ${wynik} | czat: ${czat.slice(c0).join(' / ') || '-'}`)
      return wynik
    }
    await klasa('RYCERZ'); const l0 = await strzal('Rycerz')
    await klasa('AGENT'); const l1 = await strzal('Agent')
    r.assert(l0 === 'brak strzaly', 'luk bez bieglosci: nie strzela', l0)
    r.assert(l1 === 'strzala wyleciala', 'luk z bieglosci: strzela', l1)

    // --- D. akcesoria ------------------------------------------------------------------------------
    await klasa('RYCERZ'); await wyczysc()
    const akc = [[9, 'NECKLACE', 'TEST_WARRIOR_NECKLACE'], [10, 'RING', 'TEST_WARRIOR_RING'],
      [11, 'RING', 'TEST_PALADIN_RING'], [12, 'GLOVES', 'TEST_WARRIOR_GLOVES'],
      [13, 'BELT', 'TEST_WARRIOR_BELT'], [14, 'CHARM', 'TEST_WARRIOR_CHARM']]
    const stat = ['mythiclib_stat_max_health', 'mythiclib_stat_attack_damage', 'mythiclib_stat_max_mana', 'mythiclib_stat_armor', 'mythiclib_stat_critical_strike_chance', 'mythiclib_stat_magic_damage', 'mythiclib_stat_movement_speed', 'mythiclib_stat_defense']
    for (const [slot, typ, id] of akc) {
      const przed = (await ph(...stat)).map(Number)
      await rcon(`mi give ${typ} ${id} ${nick} 1`); await H.sleep(1000)
      const it = bot.inventory.items().find(i => (H.itemLabel(i) || '').includes('[TEST]') && i.slot !== undefined && (i.slot < 9 || i.slot > 17))
      if (!it) { r.fail(`akcesorium slot ${slot} (${typ})`, 'brak przedmiotu'); continue }
      await bot.clickWindow(it.slot, 0, 0); await H.sleep(400)
      await bot.clickWindow(slot, 0, 0); await H.sleep(1500); await odloz()
      const jest = /passed/i.test(await rcon(`minecraft:execute if items entity ${nick} container.${slot} *`))
      const po = (await ph(...stat)).map(Number)
      const zm = stat.map((s, i) => po[i] !== przed[i] ? `${s.replace('mythiclib_stat_', '')} ${przed[i]}->${po[i]}` : null).filter(Boolean)
      r.assert(jest && zm.length > 0, `akcesorium slot ${slot} (${typ}): zostaje i dziala`, `w slocie=${jest} ${zm.join(', ') || 'bez zmian statystyk'}`)
    }
  } finally { await rcon(`minecraft:clear ${nick}`).catch(() => {}); await H.quit(bot) }
}
