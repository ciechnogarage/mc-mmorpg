'use strict'
// Sonda: czy klucz statystyki w pliku klasy ZASTĘPUJE wartość domyślną MMOCore, czy się do niej dodaje.
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
module.exports = async function (r) {
  const nick = 'EvSonda' + Date.now() % 100000
  const bot = await H.connect(nick)
  try {
    await F.enterCreator(bot); await F.completeCreator(bot, { race: 'czlowiek', klass: 'ninja' }); await H.sleep(4000)
    const st = ['max_mana', 'mana_regeneration', 'health_regeneration', 'max_health', 'movement_speed', 'attack_damage', 'attack_speed']
    for (const k of (process.env.KLASY || 'AGENT,SWIETY_AKOLITA,WYROCZNIA,OPIEKUN_DUCHOW,HIEROFANT,RYCERZ,MAG_AKADEMII,TROPICIEL').split(',')) {
      await rcon(`mmocore class select ${nick} ${k}`); await H.sleep(700)
      const w = (await rcon(`papi parse ${nick} ${st.map(s => '%mythiclib_stat_' + s + '%').join('|')}`)).trim().split('|')
      console.log('  ' + k.padEnd(16), st.map((s, i) => `${s}=${w[i]}`).join(' '))
    }
    r.assert(true, 'sonda')
  } finally { await H.quit(bot) }
}
