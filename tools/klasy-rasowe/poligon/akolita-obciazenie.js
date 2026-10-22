'use strict'
// Obciążenie: 4 akolitów × 3 sługi walczy z 8 wilkami; MSPT przed i w trakcie (Paper /mspt).
const mineflayer = require('/g/node_modules/mineflayer'); const { attach } = require('/e/lib/resourcepack.js'); const { rcon } = require('/s/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const F = bot => bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 })
const mspt = async () => (await rcon('mspt')).replace(/\s+/g, ' ').trim()
;(async () => {
  await rcon('forceload add 280 280 320 320'); await rcon('minecraft:kill @e[type=!player,x=286,y=-63,z=286,dx=28,dy=12,dz=28]')
  console.log('MSPT spoczynek:', await mspt())
  const boty = []
  for (const [i, rasa] of ['nieumarly', 'ork', 'elf', 'czlowiek'].entries()) {
    const nick = 'Ob' + rasa.slice(0, 4) + i
    const bot = mineflayer.createBot({ host: process.env.MC_HOST, username: nick, auth: 'offline', version: '1.21.11' }); attach(bot._client)
    await new Promise(r => bot.once('spawn', r)); await sleep(4500)
    await rcon(`cc testpostac ${nick} ${rasa} akolita`); await sleep(15000)
    await rcon(`minecraft:tp ${nick} ${296 + i * 3}.5 -59 296.5 0 0`); await sleep(1200)
    const bron = bot.quickBarSlot; const k = [...Array(9).keys()].filter(s => s !== bron)[0]
    for (let n = 0; n < 3; n++) { await rcon(`mmocore admin resource-mana set ${nick} 10`); F(bot); await sleep(300); bot.setQuickBarSlot(k); await sleep(300); F(bot); await sleep(300); bot.setQuickBarSlot(bron); await sleep(3200) }
    boty.push(bot)
  }
  const sl = (await rcon('execute if entity @e[type=!player,type=!wolf,x=286,y=-63,z=286,dx=28,dy=12,dz=28]')).trim()
  console.log('sług na arenie:', sl, '| MSPT ze sługami (bez walki):', await mspt())
  for (let w = 0; w < 8; w++) await rcon(`mm mobs spawn GAJ_WILK 1 world,${294 + w * 2}.5,-59,306.5`)
  await sleep(800)
  for (const b of boty) { const w = b.nearestEntity(e => e.name === 'wolf' && e.position.z > 304); if (w) try { await b.lookAt(w.position.offset(0, .4, 0), true); b.attack(w) } catch {} }
  const pomiary = []; for (let s = 0; s < 6; s++) { await sleep(2500); pomiary.push(await mspt()) }
  console.log('MSPT w walce (co 2,5 s):'); pomiary.forEach(p => console.log('  ', p))
  console.log('wilków zostało:', (await rcon('execute if entity @e[type=wolf,name="Wilk Gajowy"]')).trim())
  for (const b of boty) b.quit(); await sleep(500); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(2) })
