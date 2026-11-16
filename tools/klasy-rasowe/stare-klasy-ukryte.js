'use strict'
/**
 * 2026-09-26: stare klasy (warrior/mage/paladin/rogue) mają display:false. Gracz (od wdrożenia 16 klas: Rycerz)
 * na 10 poziomie dostaje punkt klasy z tabeli nagród — sprawdzamy, że w /class NIE może
 * nim wybrać innej klasy bazowej (okno bez kafelków klas albo brak okna).
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
module.exports = async function (r) {
  const nick = 'EvStare' + Date.now() % 100000
  const bot = await H.connect(nick)
  try {
    await F.waitProfileScreen(bot); await F.enterCreator(bot)
    await F.completeCreator(bot, { race: 'czlowiek', klass: 'wojownik' }); await H.sleep(4000)
    await rcon(`mmocore admin level give ${nick} main 9`); await H.sleep(1500)
    const [k, pk] = (await rcon(`papi parse ${nick} %mmocore_class_id%|%mmocore_class_points%`)).trim().split('|')
    r.assert(k.toUpperCase() === 'RYCERZ' && pk === '1', 'Rycerz (Czlowiek Wojownik) na 10 poz. ma 1 punkt klasy', `${k} ${pk}`)
    bot.chat('/class')
    const w = await H.waitForWindow(bot, { timeout: 6000 })
    const kafle = w ? H.windowSlots(w).filter(x => x.slot < w.inventoryStart) : []
    console.log('  okno:', w ? H.windowTitle(w) : 'brak', '|', kafle.map(x => `${x.slot}:${x.label}`).join(' | '))
    const klasy = kafle.filter(x => /Ninja|Mage|Wojownik|Akolita|Warrior|Paladin|Rogue/i.test(x.label || ''))
    r.assert(klasy.length === 0, 'w /class nie ma kafelkow starych klas', klasy.map(x => x.label).join(', ') || 'brak')
    // próba realnej zmiany: klik w pierwszy kafel klasy i potwierdzenie
    if (w && klasy.length) {
      await H.click(bot, klasy[0].slot)
      const pot = await H.waitForWindowChange(bot, w, { timeout: 5000 })
      console.log('  po kliknieciu:', pot ? H.windowTitle(pot) : 'brak', H.windowSlots(pot).filter(x => x.slot < 27).map(x => `${x.slot}:${x.label}`).join(' | '))
      const tak = pot && H.findSlot(pot, /Select|Wybierz/)
      if (tak) { await H.click(bot, tak.slot); await H.sleep(2000) }
    }
    if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(600) }
    const [k2] = (await rcon(`papi parse ${nick} %mmocore_class_id%`)).trim().split('|')
    r.assert(k2.toUpperCase() === 'RYCERZ', 'klasa bez zmian', k2)
  } finally { await H.quit(bot) }
}
