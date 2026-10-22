'use strict'
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')
const {rconClient} = require('/h/rcon')

async function main() {
  const name = 'EvCykl40232'
  const bot = await H.connect(name, {timeout: 90000})
  const rc = await rconClient()
  try {
    await H.sleep(2500)
    console.log('INITIAL_WINDOW', bot.currentWindow && H.windowTitle(bot.currentWindow))
    console.log('INITIAL_SLOTS', H.windowSlots(bot.currentWindow).filter(x=>x.slot<72).map(x=>`${x.slot}:${x.label}`).join(' | '))
    console.log('INITIAL_MODE', bot.game.gameMode, 'POSITION', JSON.stringify(bot.entity.position))
    if (F.isCreatorWindow(bot)) await F.completeCreator(bot, {race:'czlowiek', klass:'wojownik'})
    console.log('BEFORE_TP', JSON.stringify(bot.entity.position))
    console.log('PROFILE_WINDOW', bot.currentWindow && H.windowTitle(bot.currentWindow))
    console.log('RCON_LIST', await rc.cmd('list'))
    console.log('TP', await rc.cmd(`execute in minecraft:stolica run minecraft:tp ${name} 0.5 101 41.5 180 0`))
    await H.sleep(3000)
    console.log('CITY_POSITION', JSON.stringify(bot.entity.position))
    console.log('CITY_DIMENSION', bot.game.dimension)
    const count = await rc.cmd('execute in minecraft:stolica positioned 0 101 0 if entity @e[type=minecraft:villager,distance=..64]')
    console.log('NPC_COUNT', count)
    console.log('TP_PORTAL', await rc.cmd(`execute in minecraft:stolica run minecraft:tp ${name} 0.5 101 12.5 180 0`))
    await H.sleep(1800)
    const targets = Object.values(bot.entities).filter(e => e.name === 'villager' && e.position.distanceTo(bot.entity.position) < 7)
    console.log('PORTAL_TARGETS', targets.map(e => ({id:e.id,position:e.position})))
    if (!targets.length) throw new Error('No Portal NPC near Nexus')
    targets.sort((a,b) => a.position.distanceTo(bot.entity.position) - b.position.distanceTo(bot.entity.position))
    bot.activateEntity(targets[0])
    const menu = await H.waitForWindow(bot, {timeout:8000, titleIncludes:'Portal Nexus'})
    console.log('PORTAL_MENU', menu && H.windowTitle(menu))
    if (!menu) throw new Error('Portal NPC did not open travel menu')
    console.log('PORTAL_SLOTS', H.windowSlots(menu).filter(x=>x.slot<27).map(x=>`${x.slot}:${x.label}`).join(' | '))
  } finally {
    rc.close()
    await H.quit(bot)
  }
}
main().catch(e => {console.error('FAIL',e.stack||e); process.exitCode=1})
