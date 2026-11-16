'use strict'
/** Czy bot w ogole potrafi zlowic rybe? Uzywa wbudowanego bot.fish(), nie recznych klikniec. */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const NICK = process.env.EVAL_NICK || 'EvRyby'
const sleep = ms => new Promise(r => setTimeout(r, ms))
const label = i => { try { return H.itemLabel(i) } catch { return i.displayName } }
;(async () => {
  const bot = await H.connect(NICK, { timeout: 90000 })
  try {
    try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) } catch (e) {}
    await sleep(3000)
    bot.chat(`/minecraft:execute in minecraft:overworld run tp ${NICK} 159 64 195`)
    await sleep(2500)
    bot.chat(`/mi give MATERIAL PROF_RAW_CATCH ${NICK} 1`)   // marker, zeby widziec ze komendy dzialaja
    bot.chat(`/minecraft:give ${NICK} minecraft:fishing_rod`)
    await sleep(2000)
    const rod = bot.inventory.items().find(i => i.name === 'fishing_rod')
    if (!rod) { console.log('BRAK WEDKI'); return }
    await bot.equip(rod, 'hand'); await sleep(500)
    console.log('pozycja: ' + bot.entity.position)
    let zlowione = 0
    for (let i = 1; i <= 4; i++) {
      try {
        await Promise.race([bot.fish(), new Promise((_, rej) => setTimeout(() => rej(new Error('timeout 45s')), 45000))])
        zlowione++
        console.log(`  polow ${i}: OK`)
      } catch (e) { console.log(`  polow ${i}: ${e.message}`) }
      await sleep(1200)
    }
    console.log('ZLOWIONO (bot.fish): ' + zlowione + '/4')
    console.log('PLECAK: ' + bot.inventory.items().map(i => `${i.count}x ${label(i)}`).join(' | '))
  } finally { await H.quit(bot) }
  process.exit(0)
})().catch(e => { console.log('przerwane: ' + e.message); process.exit(2) })
