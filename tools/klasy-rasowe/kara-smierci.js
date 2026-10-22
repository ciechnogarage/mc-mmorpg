'use strict'
/**
 * Kara za śmierć (2026-09-27): ekwipunek ZOSTAJE, utrata ~10% EXP poziomu, "Rany po śmierci" na 3 min
 * (-15% obrażeń broni i umiejętności, -10% maks. HP, ikona Pecha). Konsola: przygotowanie (EXP),
 * wywołanie śmierci (minecraft:kill — przyczyna nie ma znaczenia dla pomiaru) i odczyt.
 * DLUGO=1 czeka też 3 min na wygaśnięcie kary.
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
const ma = async (nick, slot, id) => /passed/i.test(await rcon(`minecraft:execute if items entity ${nick} ${slot} *[minecraft:custom_data~{MMOITEMS_ITEM_ID:"${id}"}]`))

module.exports = async function (r) {
  const nick = 'EvKara' + Date.now() % 100000
  const bot = await H.connect(nick)
  const czat = []; bot.on('messagestr', m => { if (!m.includes('❤')) czat.push(m) })
  const ph = async (...p) => (await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)).trim().split('|')
  const stan = async et => {
    const [lvl, exp, wd, sd, hp] = await ph('mmocore_level', 'mmocore_experience', 'mythiclib_stat_weapon_damage', 'mythiclib_stat_skill_damage', 'mythiclib_stat_max_health')
    const pech = /passed/i.test(await rcon(`minecraft:execute if entity @a[name=${nick},nbt={active_effects:[{id:"minecraft:unluck"}]}]`))
    const s = { lvl: +lvl, exp: +exp, wd: +wd, sd: +sd, hp: +hp, pech }
    console.log(`  [${et}]`, JSON.stringify(s), '|', (await rcon(`minecraft:clear ${nick} * 0`)).trim()); return s
  }
  // liczba przedmiotów na ziemi przy graczu, pewnie (rcon często milczy na 'execute if')
  const ziemia = async () => { await rcon(`minecraft:execute at ${nick} store result storage kr:t n int 1 if entity @e[type=item,distance=..10]`); return (await rcon('minecraft:data get storage kr:t n')).replace(/.*contents: /, '').trim() }
  const zestaw = { 'armor.chest': 'KOLCZUGA_NAPIERSNIK', 'armor.head': 'KOLCZUGA_HELM', 'container.*': 'MIECZ_KAMIENNY' }
  const smierc = async () => {
    const c0 = czat.length
    await rcon(`minecraft:kill ${nick}`); await H.sleep(2500)
    if (bot.health <= 0 || bot.isAlive === false) { try { bot.respawn() } catch {} }
    await H.sleep(3500)
    return czat.slice(c0).join(' / ')
  }
  try {
    await F.enterCreator(bot); await F.completeCreator(bot, { race: 'czlowiek', klass: 'wojownik' }); await H.sleep(5000)
    await rcon(`mmocore admin exp give ${nick} main 150`); await H.sleep(1000)
    await rcon(`minecraft:execute at ${nick} run minecraft:kill @e[type=item,distance=..30]`); await H.sleep(500)
    const ziemia0 = await ziemia()
    const przed = await stan('przed smiercia')
    for (const [sl, id] of Object.entries(zestaw)) r.assert(await ma(nick, sl, id), `przygotowanie: ${id} w ${sl}`)
    const msg = await smierc()
    const po = await stan('po smierci')
    for (const [sl, id] of Object.entries(zestaw)) r.assert(await ma(nick, sl, id), `po smierci ${id} nadal w ${sl} (ekwipunek zostaje)`)
    r.assert(await ma(nick, 'container.8', 'UI_DZIENNIK'), 'po smierci Dziennik w slocie 8')
    const naZiemi = await ziemia()
    if (naZiemi !== ziemia0) console.log('  NA ZIEMI:', (await rcon(`minecraft:execute at ${nick} as @e[type=item,distance=..10] run data get entity @s Item`)).slice(0, 600))
    r.assert(naZiemi === ziemia0, 'nic nie wypadlo na ziemie (brak duplikacji)', `${ziemia0} -> ${naZiemi}`)
    r.assert(/Zginąłeś i straciłeś/.test(msg), 'komunikat MMOCore po polsku', msg.slice(0, 200))
    r.assert(po.lvl === przed.lvl, 'poziom nie spada', `${przed.lvl} -> ${po.lvl}`)
    const strata = przed.exp - po.exp
    r.assert(strata > 0 && Math.abs(strata - przed.exp * 0.10) <= 2, 'EXP spada o ~10% biezacego', `${przed.exp} -> ${po.exp}`)
    r.assert(po.wd <= przed.wd - 14.9 && po.sd <= przed.sd - 14.9, 'kara: -15% obrazen broni i umiejetnosci', `bron ${przed.wd}->${po.wd}, umiej. ${przed.sd}->${po.sd}`)
    r.assert(po.hp < przed.hp && Math.abs(po.hp - przed.hp * 0.9) < 0.6, 'kara: -10% maks. HP', `${przed.hp} -> ${po.hp}`)
    r.assert(po.pech, 'widoczna ikona kary (Pech)')
    r.assert(/Rany po śmierci/.test(msg), 'komunikat o karze', msg.slice(0, 200))
    await smierc()
    const po2 = await stan('po drugiej smierci')
    r.assert(Math.abs(po2.wd - po.wd) < 0.1 && Math.abs(po2.hp - po.hp) < 0.6, 'druga smierc odswieza kare, nie sumuje', `bron ${po.wd}->${po2.wd}, hp ${po.hp}->${po2.hp}`)
    if (process.env.DLUGO === '1') {
      await H.sleep(185000)
      const koniec = await stan('po 3 min')
      r.assert(Math.abs(koniec.wd - przed.wd) < 0.1 && Math.abs(koniec.hp - przed.hp) < 0.6 && !koniec.pech, 'po 3 min kara wygasa', JSON.stringify(koniec))
    }
  } finally { await H.quit(bot) }
}
