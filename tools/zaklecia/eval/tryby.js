'use strict'
setTimeout(() => { console.error('BLAD: limit czasu'); process.exit(2) }, 120000)
const mineflayer = require('mineflayer'); const { Rcon } = require('rcon-client'); const rp = require('./resourcepack')
const HOST = 'zaklecia-proba'; const sleep = ms => new Promise(r => setTimeout(r, ms))
const clean = s => String(s).replace(/§./g, '').replace(/\x1b\[[0-9;]*m/g, '').trim()
;(async () => {
  const rcon = await Rcon.connect({ host: HOST, port: 25575, password: 'zaklecia' }); const cmd = async c => clean(await rcon.send(c))
  const bot = mineflayer.createBot({ host: HOST, port: 25565, username: 'MagTest', version: '1.21.11' }); rp.attach(bot._client)
  let cz = {}; bot._client.on('world_particles', p => { if (!bot.entity) return; const dx = p.x - bot.entity.position.x; const k = p.particle?.type + (dx > 0.1 ? '_L' : dx < -0.1 ? '_P' : '_S'); if (Math.abs(p.z - bot.entity.position.z) > 1.2) cz[k] = (cz[k] || 0) + 1 })
  await new Promise(r => bot.once('spawn', r)); await sleep(2000)
  await cmd('minecraft:tp MagQA 60 -60 60'); await cmd('minecraft:clear MagTest'); await cmd('minecraft:kill @e[type=zombie]'); await cmd('minecraft:tp MagTest 0.5 -60 0.5 0 0')
  bot.setQuickBarSlot(0); await sleep(300)
  for (const [nazwa, prawa, lewa] of [['prawa', 'plomienie', 'nic'], ['podwojne', 'plomienie', 'plomienie'], ['mieszane', 'plomienie', 'odmrozenie']]) {
    await cmd(`zaklecia przypisz MagTest ${prawa} prawa`); await cmd(`zaklecia przypisz MagTest ${lewa} lewa`); await cmd('mmocore admin resource-mana set MagTest 100'); await sleep(600)
    await bot.look(0, 0, true); cz = {}; bot.activateItem(); await sleep(1000); bot.deactivateItem(); await sleep(500)
    console.log(nazwa.padEnd(9), JSON.stringify(cz))
  }
  bot.quit(); await rcon.end(); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(1) })
