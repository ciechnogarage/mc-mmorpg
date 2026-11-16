'use strict'
/**
 * Okno specjalizacji przy 32 kafelkach: Rycerz (Człowiek × Wojownik) na 10 poz. widzi TYLKO
 * Bastion i Czempion, wybiera Bastion w oknie /class (jak gracz) -> dostaje ciężką zbroję i tarczę.
 * Konsola: poziomy (przygotowanie) i odczyt placeholderów.
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')

module.exports = async function (r) {
  const nick = 'EvSpec' + Date.now() % 100000
  const ph = async (...p) => (await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)).trim().split('|')
  const bot = await H.connect(nick)
  try {
    await F.enterCreator(bot); await F.completeCreator(bot, { race: 'czlowiek', klass: 'wojownik' }); await H.sleep(4000)
    const [k0] = await ph('mmocore_class_id')
    r.assert(k0.toUpperCase() === 'RYCERZ', 'Czlowiek Wojownik -> Rycerz', k0)
    await rcon(`mmocore admin level give ${nick} main 9`); await H.sleep(1500)
    const [lvl, pkt] = await ph('mmocore_level', 'mmocore_class_points')
    r.assert(+lvl >= 10 && pkt === '1', 'poziom 10 i punkt klasy', `${lvl} / ${pkt}`)
    bot.chat('/class')
    const w = await H.waitForWindow(bot, { timeout: 8000 })
    if (!r.assert(!!w && /specjalizac/i.test(H.windowTitle(w)), '/class otwiera wybor specjalizacji', w ? H.windowTitle(w) : 'brak')) return
    const kafelki = H.windowSlots(w).filter(x => x.slot < 26 && x.label).map(x => `${x.slot}:${x.label}`)
    console.log('  kafelki:', kafelki.join(' | '))
    r.assert(kafelki.length === 2 && /11:.*Bastion/.test(kafelki.join()) && /15:.*Czempion/.test(kafelki.join()),
      'widac tylko Bastion (11) i Czempion (15)', kafelki.join(' | '))
    const b = H.findSlot(w, /Bastion/)
    if (b) {
      await H.click(bot, b.slot)
      const pot = await H.waitForWindowChange(bot, w, { timeout: 6000 })
      const tak = pot && H.findSlot(pot, /Select|Wybierz|Bastion/)
      if (tak) { await H.click(bot, tak.slot); await H.sleep(2000) }
      if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(600) }
    }
    const [k, l2, c, t, p2] = await ph('mmocore_class_id', 'mmocore_level', 'mmocore_attribute_bieglosc_ciezka', 'mmocore_attribute_bieglosc_tarcza', 'mmocore_class_points')
    r.assert(k.toUpperCase() === 'BASTION', 'po wyborze klasa to Bastion', k)
    r.assert(+l2 >= 10 && p2 === '0', 'poziom zostaje, punkt klasy zuzyty', `${l2} / ${p2}`)
    r.assert(c === '1' && t === '1', 'Bastion: bieglosc ciezka zbroja i tarcza', `${c} / ${t}`)
  } finally { await H.quit(bot) }
}
