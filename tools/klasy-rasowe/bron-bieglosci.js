'use strict'
/**
 * Biegłości broni z perspektywy gracza: bot BEZ OP bije huska (NoAI, 200 HP; zombie płonie w dzień i psuje pomiar) bronią
 * dozwoloną i zabronioną dla swojej klasy. Mierzymy utratę HP celu (konsola tylko czyta
 * `data get` i przygotowuje: klasa, przedmioty, cel). Rycerz: halabarda TAK, klinga i sierp NIE;
 * Wyrocznia: sierp TAK, halabarda NIE.
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')

module.exports = async function (r) {
  const nick = 'EvBron' + Date.now() % 100000
  const tag = 'evb' + Date.now() % 100000
  const bot = await H.connect(nick)
  const czat = []
  bot.on('messagestr', (m, pos) => { czat.push(m); console.log('  czat[' + pos + ']:', m) })
  const hp = async () => { const o = await rcon(`minecraft:data get entity @e[tag=${tag},limit=1] Health`); const m = o.match(/([\d.]+)f/); return m ? parseFloat(m[1]) : NaN }
  let miejsce = null   // pozycja przywołanego celu; naturalne zombie przy spawnie też bywają w zasięgu
  const cel = () => Object.values(bot.entities).filter(e => e.name === 'husk' && miejsce && e.position.distanceTo(miejsce) < 0.6)[0]
  const uderz = async (szukaj, etykieta) => {
    const it = bot.inventory.items().find(i => (H.itemLabel(i) || '').toLowerCase().includes(szukaj))
    if (!it) return { blad: 'brak przedmiotu ' + szukaj }
    await bot.equip(it, 'hand'); await H.sleep(1500)
    const z = cel(); if (!z) return { blad: 'brak zombie w zasiegu' }
    await bot.lookAt(z.position.offset(0, 1.6, 0), true)
    const przed = await hp(); const c0 = czat.length
    bot.attack(z); await H.sleep(1200)
    const po = await hp()
    console.log(`  [${etykieta}] ${H.itemLabel(it)}: HP ${przed} -> ${po} | czat: ${czat.slice(c0).join(' / ') || '-'}`)
    return { przed, po, strata: przed - po, czat: czat.slice(c0).filter(m => !m.includes('❤')).join(' / ') }
  }
  try {
    await F.enterCreator(bot); await F.completeCreator(bot, { race: 'czlowiek', klass: 'wojownik' }); await H.sleep(4000)
    await rcon(`mmocore class select ${nick} RYCERZ`)
    for (const [t, id] of [['HALBERD', 'BRAVE_HALBERD'], ['KATANA', 'FLOWING_KATANA'], ['SIERP', 'SIERP_KSIEZYCOWY']]) await rcon(`mi give ${t} ${id} ${nick} 1`)
    await rcon(`mmocore admin level give ${nick} main 40`)
    await rcon(`minecraft:execute at ${nick} run kill @e[type=#minecraft:zombies,distance=..40]`)
    await H.sleep(500)
    miejsce = bot.entity.position.floored().offset(2.5, 0, 0.5)
    await rcon(`minecraft:summon minecraft:husk ${miejsce.x} ${miejsce.y} ${miejsce.z} {NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:["${tag}"],attributes:[{id:"minecraft:max_health",base:200d}],Health:200f}`)
    await H.sleep(2000)
    console.log('  przedmioty:', bot.inventory.items().map(i => H.itemLabel(i)).join(' | '))
    const h1 = await uderz('halberd', 'Rycerz')
    const k1 = await uderz('katana', 'Rycerz')
    const s1 = await uderz('sierp', 'Rycerz')
    r.assert(!h1.blad && h1.strata > 0, 'Rycerz: halabarda (bieglosc drzewcowa) zadaje obrazenia', h1.blad || h1.strata)
    r.assert(!k1.blad && k1.strata === 0 && /elfie klingi/.test(k1.czat), 'Rycerz: elfia klinga (brak bieglosci) nie zadaje obrazen, komunikat o bieglosci', k1.blad || `${k1.strata} | ${k1.czat}`)
    r.assert(!s1.blad && s1.strata === 0 && /sierpy/.test(s1.czat), 'Rycerz: sierp (brak bieglosci) nie zadaje obrazen, komunikat o bieglosci', s1.blad || `${s1.strata} | ${s1.czat}`)
    await rcon(`mmocore class select ${nick} WYROCZNIA`); await H.sleep(1000)
    await rcon(`mmocore admin level give ${nick} main 40`); await H.sleep(1000)
    const s2 = await uderz('sierp', 'Wyrocznia')
    const h2 = await uderz('halberd', 'Wyrocznia')
    r.assert(!s2.blad && s2.strata > 0, 'Wyrocznia: sierp (nowy typ SIERP) zadaje obrazenia', s2.blad || s2.strata)
    r.assert(!h2.blad && h2.strata === 0, 'Wyrocznia: halabarda (brak bieglosci) nie zadaje obrazen', h2.blad || h2.strata)
  } finally {
    await rcon(`minecraft:kill @e[tag=${tag}]`).catch(() => {})
    await H.quit(bot)
  }
}
