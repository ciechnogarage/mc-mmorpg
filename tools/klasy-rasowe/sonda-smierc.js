'use strict'
// Sonda: kiedy znika ekwipunek przy śmierci (keep_inventory true)? przed / martwy (ekran śmierci) / po odrodzeniu.
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
module.exports = async function (r) {
  const gr = async () => { await rcon(`minecraft:execute in minecraft:overworld store result storage kr:t g int 1 run gamerule keep_inventory`); return (await rcon('minecraft:data get storage kr:t g')).replace(/.*contents: /,'').trim() }
  const nick = 'EvSm' + Date.now() % 100000
  const bot = await H.connect(nick, {}); bot.settings && (bot._client.autoRespawn = false)
  const naZiemi = async () => { await rcon(`minecraft:execute at ${nick} store result storage kr:t n int 1 if entity @e[type=item,distance=..8]`); return (await rcon('minecraft:data get storage kr:t n')).replace(/.*contents: /,'').trim() }
  const ile = async et => { console.log(`  [${et}] przedmiotow na ziemi (<=8): ${await naZiemi()}, gamerule: ${await gr()}`); const o = await rcon(`minecraft:clear ${nick} * 0`); const p = await rcon(`minecraft:execute at ${nick} if entity @e[type=item,distance=..10]`); console.log(`  [${et}] ${o.trim()} | ziemia: ${p.trim()}`); console.log('    vanilla:', (await rcon(`minecraft:clear ${nick} minecraft:dirt 0`)).trim(), '/', (await rcon(`minecraft:clear ${nick} minecraft:diamond 0`)).trim()); console.log('    klata:', (await rcon(`minecraft:data get entity ${nick} equipment.chest`)).slice(0, 700)); console.log('    sloty:', (await rcon(`minecraft:data get entity ${nick} Inventory`)).replace(/\{components:\{[^]*?\}\}/g,'').slice(0, 900)) }
  try {
    await F.enterCreator(bot); await F.completeCreator(bot, { race: 'czlowiek', klass: 'wojownik' }); await H.sleep(5000)
    await rcon(`minecraft:execute at ${nick} run minecraft:kill @e[type=item,distance=..30]`); await H.sleep(500)
    if (process.env.GDZIE) { await rcon(`minecraft:execute in ${process.env.GDZIE.split(' ')[0]} run minecraft:tp ${nick} ${process.env.GDZIE.split(' ').slice(1).join(' ')}`); await H.sleep(3000); console.log('  swiat:', (await rcon(`papi parse ${nick} %player_world% %player_x% %player_y% %player_z%`)).trim()) }
    await rcon(`minecraft:give ${nick} minecraft:dirt 5`); await rcon(`minecraft:give ${nick} minecraft:diamond 3`); await H.sleep(800)
    await ile('przed')
    bot.removeAllListeners('death'); bot._client.removeAllListeners('death_combat_event')
    await rcon(`minecraft:kill ${nick}`); await H.sleep(1500)
    await ile('martwy, przed respawnem')
    bot.respawn(); await H.sleep(4000)
    await ile('po respawnie')
    r.assert(true, 'sonda')
  } finally { await H.quit(bot) }
}
