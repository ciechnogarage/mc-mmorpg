'use strict'
/**
 * Konfiguracja 48 klas tak, jak ją LICZY serwer (MMOCore -> MythicLib), nie jak stoi w plikach:
 * każda biegłość (3 zbroje + 21 broni) = 1 dokładnie tam, gdzie oczekiwane.json, statystyka-podpis
 * specjalizacji > 0, delty ras. Konsola: nadanie klasy (przygotowanie) i odczyt placeholderów.
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
const OCZ = require('/h/oczekiwane.json')
const stat = k => 'mythiclib_stat_' + k.replace(/-/g, '_')

module.exports = async function (r) {
  const nick = 'EvKonf' + Date.now() % 100000
  const ph = async p => (await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)).trim().split('|')
  const bot = await H.connect(nick)
  const zm = {}
  try {
    await F.enterCreator(bot); await F.completeCreator(bot, { race: 'czlowiek', klass: 'wojownik' }); await H.sleep(4000)
    const biegl = OCZ.bieglosci
    const extra = ['movement_speed', 'max_health', 'max_mana', 'lifesteal', 'spell_vampirism', 'cooldown_reduction', 'physical_damage']
    for (const [id, k] of Object.entries(OCZ.klasy)) {
      await rcon(`mmocore class select ${nick} ${id}`); await H.sleep(700)
      const pola = ['mmocore_class_id', ...biegl.map(b => 'mmocore_attribute_bieglosc_' + b), ...extra.map(e => 'mythiclib_stat_' + e)]
      if (k.stat) pola.push(stat(k.stat))
      const w = await ph(pola)
      const klasa = w[0]
      const ma = biegl.filter((b, i) => w[1 + i] === '1')
      const zle = biegl.filter((b, i) => !['0', '1'].includes(w[1 + i]))
      const oczek = [...k.zbroja, ...k.bron]
      const ok = klasa.toUpperCase() === id && zle.length === 0 &&
        ma.length === oczek.length && oczek.every(b => ma.includes(b))
      r.assert(ok, `${id}: klasa i bieglosci`, `klasa=${klasa} ma=[${ma}] oczek=[${oczek}]${zle.length ? ' NIEZNANE=' + zle : ''}`)
      zm[id] = Object.fromEntries(extra.map((e, i) => [e, parseFloat(w[1 + biegl.length + i])]))
      if (k.stat) {
        const v = parseFloat(w[w.length - 1])
        r.assert(v > 0, `${id}: statystyka specjalizacji ${k.stat} > 0`, w[w.length - 1])
      }
    }
    const z = zm
    console.log('  RASY (Rycerz/Ostrze Gaju/Wodz Wojny/Rycerz Smierci):', JSON.stringify(['RYCERZ', 'OSTRZE_GAJU', 'WODZ_WOJNY', 'RYCERZ_SMIERCI'].map(i => z[i])))
    r.assert(z.OSTRZE_GAJU.movement_speed > z.RYCERZ.movement_speed && z.RYCERZ_SMIERCI.movement_speed < z.RYCERZ.movement_speed, 'elf szybszy, nieumarly wolniejszy od czlowieka',
      `${z.OSTRZE_GAJU.movement_speed} / ${z.RYCERZ.movement_speed} / ${z.RYCERZ_SMIERCI.movement_speed}`)
    r.assert(z.WODZ_WOJNY.max_health > z.RYCERZ.max_health && z.OSTRZE_GAJU.max_health < z.RYCERZ.max_health, 'ork wiecej HP, elf mniej',
      `${z.WODZ_WOJNY.max_health} / ${z.RYCERZ.max_health} / ${z.OSTRZE_GAJU.max_health}`)
    r.assert(z.RYCERZ.cooldown_reduction >= 5, 'czlowiek: redukcja odnowienia', z.RYCERZ.cooldown_reduction)
    r.assert(z.RYCERZ_SMIERCI.lifesteal >= 3 && z.MAG_GROBU.spell_vampirism >= 3, 'nieumarly: kradziez zycia / wampiryzm czarow',
      `${z.RYCERZ_SMIERCI.lifesteal} / ${z.MAG_GROBU.spell_vampirism}`)
    r.assert(z.TROPICIEL.max_mana < z.AGENT.max_mana && z.SZAMAN.max_mana < z.MAG_AKADEMII.max_mana, 'ork: mniej many (ninja i mag)',
      `${z.TROPICIEL.max_mana}<${z.AGENT.max_mana}, ${z.SZAMAN.max_mana}<${z.MAG_AKADEMII.max_mana}`)
    r.assert(z.WODZ_WOJNY.physical_damage >= 10, 'ork: +obrazenia fizyczne', z.WODZ_WOJNY.physical_damage)
  } finally { await H.quit(bot) }
}
