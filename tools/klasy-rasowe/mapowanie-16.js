'use strict'
/**
 * Ścieżka gracza: kreator (rasa × archetyp) -> klasa rasowa, dla WSZYSTKICH 16 par
 * (oczekiwane.json z generuj_klasy.py). Pierwsza para dodatkowo: wyjście i powrót do postaci —
 * kreator przy wyborze postaci woła `class select <archetyp>`, skrypt CoreTools ma to anulować.
 * Konsola tylko czyta placeholdery.
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
const OCZ = require('/h/oczekiwane.json')

module.exports = async function (r) {
  const pary = Object.entries(OCZ.mapa)
  const tylko = process.env.PARY ? process.env.PARY.split(',') : null
  let pierwsza = true
  for (const [para, oczek] of pary) {
    if (tylko && !tylko.includes(para)) continue
    const [race, klass] = para.split('/')
    const nick = 'EvM' + race.slice(0, 3) + klass.slice(0, 3) + (Date.now() % 1000)
    const ph = async (...p) => (await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)).trim().split('|')
    let bot = await H.connect(nick)
    try {
      if (!r.assert(!!(await F.enterCreator(bot)), `${para}: kreator otwarty`)) continue
      await F.completeCreator(bot, { race, klass }); await H.sleep(4000)
      const [k, rasa] = await ph('mmocore_class_id', `luckperms_has_permission_race.${race}`)
      r.assert(k.toUpperCase() === oczek, `${para} -> ${oczek}`, `klasa=${k} race.${race}=${rasa}`)
      if (pierwsza) {
        pierwsza = false
        await H.quit(bot); await H.sleep(3000)
        bot = await H.connect(nick)
        const ekran = await F.waitProfileScreen(bot)
        const glowa = ekran && H.windowSlots(ekran).find(s => s.slot < ekran.inventoryStart && s.name === 'player_head')
        if (r.assert(!!glowa, `${para}: po powrocie jest postac do wybrania`)) {
          await H.click(bot, glowa.slot); await H.sleep(7000)
          const [k2] = await ph('mmocore_class_id')
          r.assert(k2.toUpperCase() === oczek, `${para}: po ponownym wejsciu nadal ${oczek}`, k2)
        }
      }
    } catch (e) { r.fail(`${para}: wyjatek`, e.stack) } finally { await H.quit(bot); await H.sleep(1500) }
  }
}
