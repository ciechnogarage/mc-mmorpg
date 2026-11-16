'use strict'
setTimeout(() => process.exit(2), 90000)
const mineflayer = require('mineflayer'); const { Rcon } = require('rcon-client'); const rp = require('./resourcepack')
const HOST = 'zaklecia-proba'; const sleep = ms => new Promise(r => setTimeout(r, ms)); const clean = s => String(s).replace(/§./g, '').trim()
;(async () => {
  const rcon = await Rcon.connect({ host: HOST, port: 25575, password: 'zaklecia' }); const cmd = async c => clean(await rcon.send(c))
  const mag = mineflayer.createBot({ host: HOST, port: 25565, username: 'MagTest', version: '1.21.11' }); rp.attach(mag._client)
  await new Promise(r => mag.once('spawn', r)); await sleep(4500)
  const obs = mineflayer.createBot({ host: HOST, port: 25565, username: 'CelTest', version: '1.21.11' }); rp.attach(obs._client)
  await new Promise(r => obs.once('spawn', r)); await sleep(2000)
  const cz = { mag: {}, obs: {} }; let on = false
  for (const [k, b] of [['mag', mag], ['obs', obs]]) b._client.on('world_particles', p => { if (!on) return; const t = p.particle?.type; cz[k][t] = (cz[k][t] || 0) + 1 })
  await cmd('minecraft:clear MagTest'); await cmd('minecraft:tp MagTest 0.5 -60 0.5 0 0'); await cmd('minecraft:tp CelTest 3.5 -60 2.5 90 0'); await cmd('minecraft:kill @e[type=zombie]')
  mag.setQuickBarSlot(0); await sleep(400); await cmd('zaklecia przypisz MagTest odmrozenie prawa'); await cmd('zaklecia przypisz MagTest odmrozenie lewa'); await cmd('mmocore admin resource-mana set MagTest 100'); await sleep(700)
  on = true; mag.activateItem(); await sleep(1200); mag.deactivateItem(); await sleep(800); on = false
  console.log('rzucajacy dostal:', JSON.stringify(cz.mag)); console.log('obserwator dostal:', JSON.stringify(cz.obs))
  mag.quit(); obs.quit(); await rcon.end(); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(1) })
