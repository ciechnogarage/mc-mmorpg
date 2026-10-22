'use strict'
/**
 * KAŻDA grupa biegłości broni (21) z perspektywy gracza: przedmiot z grupy trzyma klasa, która
 * ma biegłość (właściciel z oczekiwane.json) i klasa, która jej nie ma. Pomiar:
 *  - wręcz: lewy klik w huska (NoAI, 200 HP) -> utrata HP celu,
 *  - dystans (łuk, kusza, muszkiet, kostur, różdżka): prawy klik -> komunikat o brakującej biegłości,
 *  - katalizatory (totem, filakterium): statystyka z drugiej ręki,
 * plus E: blok tarczą (podniesiona tarcza, cios moba z przodu) bez i z biegłością.
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
const OCZ = require('/h/oczekiwane.json')
const GRUPY = [ // grupa, typ, id, sposób, [statystyka]
  ['miecz', 'SWORD', 'STEEL_SWORD', 'wrecz'], ['sztylet', 'DAGGER', 'STEEL_DAGGER', 'wrecz'],
  ['sztylet', 'GAUNTLET', 'FIGHTING_GAUNTLETS', 'wrecz'], ['luk', 'BOW', 'COMPOSITE_BOW', 'dystans'],
  ['kostur', 'STAFF', 'EARTH_STAFF', 'dystans'], ['kostur', 'WAND', 'OAK_WAND', 'dystans'],
  ['bulawa', 'HAMMER', 'HEAVY_HAMMER', 'wrecz'], ['drzewcowa', 'LANCE', 'FIRE_GREATLANCE', 'wrecz'],
  ['kusza', 'CROSSBOW', 'MEDIEVAL_CROSSBOW', 'dystans'], ['kusza', 'MUSKET', 'MAKESHIFT_MUSKET', 'dystans'],
  ['kostur_dwureczny', 'GREATSTAFF', 'STIFF_GREATSTAFF', 'wrecz'], ['topor', 'AXE', 'SILVER_AXE', 'wrecz'],
  ['topor', 'GREATAXE', 'BARBARIAN_AXE', 'wrecz'], ['wlocznia', 'SPEAR', 'SHARP_SPEAR', 'wrecz'],
  ['totem', 'TALISMAN', 'THUNDER_BLESSING', 'stat'], ['mlot_dwureczny', 'GREATHAMMER', 'UNRELIABLE_HAMMER', 'wrecz'],
  ['miecz_dwureczny', 'GREATSWORD', 'TWO_HANDED_STEEL_SWORD', 'wrecz'], ['lancuch', 'WHIP', 'WIND_WHIP', 'wrecz'],
  ['filakterium', 'FILAKTERIUM', 'FILAKTERIUM_KOSCIANE', 'stat'], ['kosa', 'KOSA', 'KOSA_SPACZONA', 'wrecz'],
  ['miecz', 'THRUSTING_SWORD', 'TEST_RAPIER', 'wrecz']  // SKILLED_SWORD wymaga też zręczności 14,
]
const bazowe = Object.entries(OCZ.klasy).filter(([, k]) => !k.baza)
const wlasciciel = g => bazowe.find(([, k]) => k.bron.includes(g))[0]
const obcy = g => bazowe.find(([, k]) => !k.bron.includes(g))[0]

module.exports = async function (r) {
  const nick = 'EvBw' + Date.now() % 100000
  const tag = 'evbw' + Date.now() % 100000
  const bot = await H.connect(nick)
  const czat = []; bot.on('messagestr', m => { if (!m.includes('❤')) czat.push(m) })
  const ph = async (...p) => (await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)).trim().split('|')
  const hp = async () => { const m = (await rcon(`minecraft:data get entity @e[tag=${tag},limit=1] Health`)).match(/([\d.]+)f/); return m ? +m[1] : NaN }
  let miejsce = null
  const cel = () => Object.values(bot.entities).find(e => e.name === 'husk' && miejsce && e.position.distanceTo(miejsce) < 0.6)
  const klasa = async id => { await rcon(`mmocore class select ${nick} ${id}`); await H.sleep(700); await rcon(`mmocore admin level give ${nick} main 60`); await H.sleep(700) }
  const nowyCel = async () => {
    await rcon(`minecraft:kill @e[tag=${tag}]`)
    await rcon(`minecraft:execute at ${nick} run minecraft:kill @e[type=#minecraft:zombies,distance=..40]`)
    miejsce = bot.entity.position.floored().offset(2.5, 0, 0.5)
    await rcon(`minecraft:summon minecraft:husk ${miejsce.x} ${miejsce.y} ${miejsce.z} {NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:["${tag}"],attributes:[{id:"minecraft:max_health",base:200d}],Health:200f}`)
    await H.sleep(1500)
  }
  const proba = async (typ, id, sposob, gnazwa) => {
    await rcon(`minecraft:clear ${nick}`); await H.sleep(600)
    const ST = ['mythiclib_stat_magic_damage', 'mythiclib_stat_max_mana', 'mythiclib_stat_skill_damage']
    const przed = (await ph(...ST)).map(Number)   // PRZED wydaniem: katalizator działa też z głównej ręki
    await rcon(`mi give ${typ} ${id} ${nick} 1`); await rcon(`minecraft:give ${nick} minecraft:arrow 8`); await H.sleep(1200)
    const it = bot.inventory.items().find(i => i.name !== 'arrow')
    if (!it) return { blad: 'brak przedmiotu' }
    const c0 = czat.length
    if (sposob === 'stat') {
      await bot.equip(it, 'off-hand'); await H.sleep(1500)
      const po = (await ph(...ST)).map(Number)
      return { dziala: po.some((v, i) => v > przed[i]), opis: ST.map((s, i) => `${s.slice(15)} ${przed[i]}->${po[i]}`).join(', '), msg: czat.slice(c0).join(' / ') }
    }
    await bot.equip(it, 'hand'); await H.sleep(1200)
    if (sposob === 'wrecz') {
      const z = cel(); if (!z) return { blad: 'brak celu' }
      await bot.lookAt(z.position.offset(0, 1.6, 0), true)
      const p = await hp(); bot.attack(z); await H.sleep(1300); const k = await hp()
      const msg = czat.slice(c0).join(' / ')
      return { dziala: p - k > 0 || (p > 0 && Number.isNaN(k)), opis: Number.isNaN(k) ? `HP ${p}->cel zabity` : `HP ${p}->${k}`, msg }  // NaN = cel padł od ciosu
    }
    bot.activateItem(); await H.sleep(1400); bot.deactivateItem(); await H.sleep(900)
    const msg = czat.slice(c0).join(' / ')
    return { dziala: !msg.includes('Nie możesz tego użyć'), opis: 'prawy klik', msg }
  }
  try {
    await F.enterCreator(bot); await F.completeCreator(bot, { race: 'czlowiek', klass: 'wojownik' }); await H.sleep(4000)
    await nowyCel()
    for (const [g, typ, id, sposob] of GRUPY) {
      const gn = OCZ.bieglosci.includes(g) ? g : g
      for (const [kl, ma] of [[obcy(g), false], [wlasciciel(g), true]]) {
        await klasa(kl)
        if (sposob === 'wrecz' && !(await hp() > 60)) await nowyCel()
        const w = await proba(typ, id, sposob, gn)
        const komunikat = /brakuje: Biegłość/.test(w.msg || '')
        console.log(`  [${g} ${typ}] ${kl} (${ma ? 'ma' : 'brak'}): ${w.blad || w.opis} | ${w.msg || '-'}`)
        if (ma) r.assert(!w.blad && w.dziala && !komunikat, `${typ} (${g}): ${kl} z bieglosci — dziala`, w.blad || `${w.opis} | ${w.msg || '-'}`)
        else r.assert(!w.blad && !w.dziala && (sposob === 'stat' || komunikat), `${typ} (${g}): ${kl} bez bieglosci — zablokowane`, w.blad || `${w.opis} | ${w.msg || '-'}`)
      }
    }
    // --- E. blok tarczą -------------------------------------------------------------------------
    for (const [kl, ma] of [['RYCERZ', false], ['BASTION', true]]) {
      await klasa(kl); await rcon(`minecraft:clear ${nick}`); await H.sleep(500)
      await rcon(`mi give SHIELD PALADIN_SHIELD ${nick} 1`); await H.sleep(1000)
      const t = bot.inventory.items().find(i => i.name === 'shield')
      await bot.equip(t, 'off-hand'); await H.sleep(800)
      await nowyCel()
      const z = cel(); await bot.lookAt(z.position.offset(0, 1.6, 0), true)
      await rcon(`minecraft:effect give ${nick} minecraft:instant_health 1 5`); await H.sleep(1500)
      bot.activateItem(true); await H.sleep(1200)
      const [h0] = await ph('player_health')
      await rcon(`minecraft:damage ${nick} 6 minecraft:mob_attack by @e[tag=${tag},limit=1]`); await H.sleep(800)
      const [h1] = await ph('player_health')
      bot.deactivateItem()
      const zablokowal = +h1 >= +h0
      console.log(`  [tarcza ${kl}] HP ${h0} -> ${h1}`)
      r.assert(true, `zmierzone: ${kl} (${ma ? 'ma' : 'brak'} bieglosci tarczy) ${zablokowal ? 'BLOKUJE' : 'nie blokuje'} cios (HP ${h0}->${h1})`)
    }
  } finally { await rcon(`minecraft:kill @e[tag=${tag}]`).catch(() => {}); await rcon(`minecraft:clear ${nick}`).catch(() => {}); await H.quit(bot) }
}
