'use strict'
// Obciazenie strumienia: obserwator liczy pakiety (czasteczki, ruch/metadane encji) w trakcie rzutu drugiego bota.
setTimeout(() => { console.error('BLAD: limit czasu'); process.exit(2) }, 150000)
const mineflayer = require('mineflayer'); const { Rcon } = require('rcon-client'); const rp = require('./resourcepack')
const HOST = process.env.MC_HOST || 'zaklecia-proba'; const sleep = ms => new Promise(r => setTimeout(r, ms))
const clean = s => String(s).replace(/§./g, '').replace(/\x1b\[[0-9;]*m/g, '').trim()
;(async () => {
  const rcon = await Rcon.connect({ host: HOST, port: 25575, password: 'zaklecia' }); const cmd = async c => clean(await rcon.send(c))
  const mag = mineflayer.createBot({ host: HOST, port: 25565, username: 'MagTest', version: '1.21.11' }); rp.attach(mag._client)
  await new Promise(r => mag.once('spawn', r)); await sleep(4500)
  const obs = mineflayer.createBot({ host: HOST, port: 25565, username: 'CelTest', version: '1.21.11' }); rp.attach(obs._client)
  await new Promise(r => obs.once('spawn', r)); await sleep(2000)
  const licz = {}; let zbieraj = false
  obs._client.on('packet', (d, meta) => { if (zbieraj) licz[meta.name] = (licz[meta.name] || 0) + 1 })
  await cmd('minecraft:clear MagTest'); await cmd('minecraft:kill @e[type=zombie]'); await cmd('minecraft:tp MagTest 0.5 -60 0.5 0 0'); await cmd('minecraft:tp CelTest 3.5 -60 2.5 90 0')
  await cmd('mm mobs spawn ZAK_KUKLA 1 world,0.5,-60,5.5'); mag.setQuickBarSlot(0); await sleep(1500)
  for (const [tryb, p, l] of [['prawa', 'plomienie', 'nic'], ['podwojne', 'plomienie', 'plomienie'], ['mieszane', 'plomienie', 'odmrozenie']]) {
    await cmd(`zaklecia przypisz MagTest ${p} prawa`); await cmd(`zaklecia przypisz MagTest ${l} lewa`); await cmd('mmocore admin resource-mana set MagTest 100'); await sleep(700)
    const z = Object.values(mag.entities).find(e => e.name === 'zombie'); if (z) await mag.lookAt(z.position.offset(0, 1.2, 0), true)
    for (const k in licz) delete licz[k]; zbieraj = true; mag.activateItem(); await sleep(3000); mag.deactivateItem(); zbieraj = false; await sleep(800)
    const ile = n => ((licz[n] || 0) / 3).toFixed(1)
    const encje = ['entity_teleport', 'sync_entity_position', 'entity_position_sync', 'rel_entity_move', 'entity_move_look', 'entity_metadata', 'spawn_entity', 'entity_destroy'].map(n => licz[n] ? `${n}=${ile(n)}` : '').filter(Boolean).join(' ')
    console.log(`${tryb.padEnd(9)} czasteczki/s=${ile('world_particles')}  ${encje}  wszystkie/s=${(Object.values(licz).reduce((a, b) => a + b, 0) / 3).toFixed(0)}`)
  }
  console.log('istniejace item_display po rzutach:', await cmd('minecraft:execute if entity @e[type=item_display]'))
  mag.quit(); obs.quit(); await rcon.end(); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(1) })
