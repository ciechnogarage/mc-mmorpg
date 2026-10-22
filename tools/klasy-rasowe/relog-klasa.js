'use strict'
/**
 * Czy klasa postaci przeżywa ponowne wejście? Kreator (ConfirmService.applyToRealPlayer)
 * przy KAŻDYM wyborze postaci woła `mmocore class select <gracz> <mmocore-id archetypu>`.
 * Scenariusz: Ork Wojownik -> WODZ_WOJNY -> 10 poz. -> Tyran z okna /class ->
 * wyjście -> wejście -> wybór postaci -> jaka klasa i poziom?
 * Od 2026-09-26 WODZ_WOJNY nadaje skrypt CoreTools klasy-rasowe.yml, a nie konsola.
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
module.exports = async function (r) {
  const nick = 'EvRelK' + Date.now() % 100000
  const ph = async (...p) => (await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)).trim().split('|')
  let bot = await H.connect(nick)
  try {
    await F.waitProfileScreen(bot); await F.enterCreator(bot)
    await F.completeCreator(bot, { race: 'ork', klass: 'wojownik' }); await H.sleep(5000)
    const [k0, rasa] = await ph('mmocore_class_id', 'luckperms_check_permission_race.ork')
    console.log('  po kreatorze klasa:', k0, '| race.ork:', rasa)
    // klasę rasową nadaje skrypt CoreTools (Scripts/klasy-rasowe.yml), nie konsola
    r.assert(k0.toUpperCase() === 'WODZ_WOJNY', 'Ork + Wojownik po kreatorze to Wodz Wojny (bez konsoli)', k0)
    await rcon(`mmocore admin level give ${nick} main 9`); await H.sleep(1500)
    bot.chat('/class')
    const w = await H.waitForWindow(bot, { timeout: 8000 })
    const t = w && H.findSlot(w, /Tyran/)
    if (t) {
      await H.click(bot, t.slot)
      const p = await H.waitForWindowChange(bot, w, { timeout: 6000 })
      const tak = p && H.findSlot(p, /Select|Wybierz/)
      if (tak) { await H.click(bot, tak.slot); await H.sleep(2000) }
    }
    if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(500) }
    const [k1, l1] = await ph('mmocore_class_id', 'mmocore_level')
    r.assert(k1.toUpperCase() === 'TYRAN', 'przed wyjsciem: Tyran', `${k1} ${l1}`)
    await H.quit(bot); await H.sleep(3000)
    bot = await H.connect(nick)
    const ekran = await F.waitProfileScreen(bot)
    if (!r.assert(!!ekran, 'ekran wyboru postaci po ponownym wejsciu')) return
    const post = H.windowSlots(ekran).filter(s => s.slot < ekran.inventoryStart).find(s => s.name === 'player_head')
    if (!r.assert(!!post, 'kafelek postaci')) return
    await H.click(bot, post.slot); await H.sleep(7000)
    if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(500) }
    const [k2, l2, c2] = await ph('mmocore_class_id', 'mmocore_level', 'mmocore_attribute_bieglosc_ciezka')
    console.log(`  PO PONOWNYM WEJSCIU: klasa=${k2} poziom=${l2} bieglosc ciezka=${c2}`)
    r.assert(k2.toUpperCase() === 'TYRAN', 'po ponownym wejsciu nadal Tyran', k2)
    r.assert(l2 === '10', 'po ponownym wejsciu nadal 10 poziom', l2)
  } finally { await H.quit(bot) }
}
