'use strict'
// WYCOFANY 2026-09-26: badał required-class zestawów TEST_<ARCHETYP>_* na klasach archetypów.
// Po wdrożeniu 16 klas rasowych nikt nie ma klasy archetypu (skrypt CoreTools anuluje class force WARRIOR),
// a required-class zdjęto ze wszystkich przedmiotów (latacz_broni.py). Zostawiony jako zapis pomiaru.
/**
 * Przegląd 2026-09-25: czy 'required-class' w zestawach TEST_{KLASA}_* działa.
 * MMOItems porównuje NAZWĘ WYŚWIETLANĄ klasy MMOCore (PlayerClass.getName(), bajtkod
 * MMOCoreModule.getClass) — sprawdzamy to zakładaniem napierśnika na żywym serwerze.
 * Konsola tylko do przygotowania (klasa, przedmioty) i odczytu placeholderów.
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')

module.exports = async function (r) {
  const nick = 'EvZes' + Date.now() % 100000
  const bot = await H.connect(nick)
  const ph = async (...p) => (await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)).trim().split('|')
  const klata = () => bot.inventory.slots[6]
  const zaloz = async fragment => {
    const it = bot.inventory.items().find(i => (H.itemLabel(i) || '').includes(fragment))
    if (!it) return 'brak przedmiotu'
    try { await bot.equip(it, 'torso') } catch (e) { return 'equip: ' + e.message }
    await H.sleep(2000)
    const serwer = await rcon(`minecraft:execute if items entity ${nick} armor.chest *`)
    return /passed/i.test(serwer) ? 'zalozony' : 'odrzucony'
  }
  const zdejmij = async () => {
    try { await bot.unequip('torso') } catch {}
    await H.sleep(1000)
    if (bot.inventory.selectedItem) { const w = bot.inventory.firstEmptyInventorySlot(); if (w != null) await bot.clickWindow(w, 0, 0) }
    await rcon(`minecraft:item replace entity ${nick} armor.chest with minecraft:air`)
    await rcon(`minecraft:clear ${nick} minecraft:iron_chestplate`); await rcon(`minecraft:clear ${nick} minecraft:leather_chestplate`)
    await rcon(`minecraft:clear ${nick} minecraft:golden_chestplate`); await rcon(`minecraft:clear ${nick} minecraft:chainmail_chestplate`)
    await H.sleep(800)
  }
  try {
    await F.waitProfileScreen(bot); await F.enterCreator(bot)
    await F.completeCreator(bot, { race: 'czlowiek', klass: 'wojownik' }); await H.sleep(4000)
    // Czlowiek, nie Ork: od 2026-09-26 Ork Wojownik dostaje WODZ_WOJNY (Scripts/klasy-rasowe.yml),
    // a przejscie klasy rasowej z powrotem na WARRIOR skrypt celowo anuluje.

    for (const [klasa, swoj, cudzy, swojId, cudzyId] of [
      ['WARRIOR', 'Wojownika', 'Maga', 'TEST_WARRIOR_CHESTPLATE', 'TEST_MAGE_CHESTPLATE'],
      ['PALADIN', 'Akolity', 'Wojownika', 'TEST_PALADIN_CHESTPLATE', 'TEST_WARRIOR_CHESTPLATE'],
      ['ROGUE', 'Ninjy', 'Wojownika', 'TEST_ROGUE_CHESTPLATE', 'TEST_WARRIOR_CHESTPLATE'],
      ['WODZ_WOJNY', null, 'Wojownika', null, 'TEST_WARRIOR_CHESTPLATE'],
    ]) {
      await rcon(`mmocore class force ${nick} ${klasa}`); await H.sleep(1500)
      const [id, nazwa] = await ph('mmocore_class_id', 'mmocore_class')
      console.log(`  klasa ${id} (${nazwa})`)
      if (swojId) {
        await rcon(`mi give ARMOR ${swojId} ${nick} 1`); await H.sleep(1200)
        const wynik = await zaloz('Napiersnik ' + swoj)
        r.assert(wynik === 'zalozony', `${klasa} (${nazwa}): wlasny napiersnik ${swojId} zakladany`, wynik)
        await zdejmij()
      }
      await rcon(`mi give ARMOR ${cudzyId} ${nick} 1`); await H.sleep(1200)
      const wynik2 = await zaloz('Napiersnik ' + cudzy)
      r.assert(wynik2 === 'odrzucony', `${klasa} (${nazwa}): cudzy napiersnik ${cudzyId} odrzucony`, wynik2)
      await zdejmij()
    }
  } finally {
    await H.quit(bot)
  }
}
