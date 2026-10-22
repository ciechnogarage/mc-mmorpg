'use strict'
/**
 * Ścieżka WŁAŚCICIELA (konto z OP) — tego nie łapały evale bez OP (2026-09-26): placeholder
 * luckperms_check_permission pyta hasPermission, więc OP "miał" wszystkie 4 rasy -> Człowiek Mag
 * dostał 4 klasy po kolei (skończył jako Mag Grobu) i 4 zestawy startowe. Skrypt czyta teraz
 * luckperms_has_permission (sam wpis w LuckPerms).
 * Mierzy: klasę rasową, zestaw startowy wydany RAZ (liczony po stronie serwera przez clear ... 0).
 * OP nadawane konsolą, bo to stan konta właściciela; sama ścieżka to kliki gracza.
 * RASA/KLASA (domyślnie czlowiek/mag), OP=0 — to samo bez OP.
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')

// broń archetypu z zestawu startowego — dokładnie jedna sztuka na postać
const BRON_ARCH = { wojownik: 'MIECZ_KAMIENNY', ninja: 'SZTYLET_REKRUTA', mag: 'ROZDZKA_UCZNIA', akolita: 'BULAWA_NOWICJUSZA' }

module.exports = async function (r) {
  const race = process.env.RASA || 'czlowiek'; const klass = process.env.KLASA || 'mag'
  const OP = process.env.OP !== '0'
  const oczek = require('/h/oczekiwane.json').mapa[`${race}/${klass}`]
  const nick = 'EvOp' + (Date.now() % 100000)
  const ph = async (...p) => (await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)).trim().split('|')
  const ile = async id => {
    const t = await rcon(`minecraft:clear ${nick} *[minecraft:custom_data~{MMOITEMS_ITEM_ID:"${id}"}] 0`)
    const m = t.match(/Found (\d+)/); return m ? +m[1] : 0
  }
  const bot = await H.connect(nick)
  try {
    if (OP) await rcon(`op ${nick}`)
    if (!r.assert(!!(await F.enterCreator(bot)), 'kreator otwarty')) return
    await F.completeCreator(bot, { race, klass }); await H.sleep(6000)
    const [k, xyz] = await ph('mmocore_class_id', 'luckperms_has_permission_race.xyz')
    r.assert(k.toUpperCase() === oczek, `${OP ? 'OP ' : ''}${race}/${klass} -> ${oczek}`, `klasa=${k} (race.xyz=${xyz})`)
    const n = await ile(BRON_ARCH[klass])
    r.assert(n === 1, `zestaw startowy wydany RAZ (${BRON_ARCH[klass]})`, `sztuk: ${n}`)
    const inne = await Promise.all(Object.values(BRON_ARCH).filter(b => b !== BRON_ARCH[klass]).map(ile))
    r.assert(inne.every(x => x === 0), 'brak broni innych archetypów (zestawy innych ras/klas)', inne.join(','))
  } catch (e) { r.fail('wyjatek', e.stack) } finally {
    if (OP) await rcon(`deop ${nick}`).catch(() => {})
    await H.quit(bot)
  }
}
