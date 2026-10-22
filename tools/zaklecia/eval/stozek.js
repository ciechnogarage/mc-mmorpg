'use strict'
setTimeout(() => process.exit(2), 90000)
const mineflayer = require('mineflayer'); const { Rcon } = require('rcon-client'); const rp = require('./resourcepack')
const HOST = 'zaklecia-proba'; const sleep = ms => new Promise(r => setTimeout(r, ms))
const clean = s => String(s).replace(/§./g, '').replace(/\x1b\[[0-9;]*m/g, '').trim()
;(async () => {
  const rcon = await Rcon.connect({ host: HOST, port: 25575, password: 'zaklecia' }); const cmd = async c => clean(await rcon.send(c))
  const bot = mineflayer.createBot({ host: HOST, port: 25565, username: 'MagTest', version: '1.21.11' }); rp.attach(bot._client)
  await new Promise(r => bot.once('spawn', r)); await sleep(1500)
  await cmd('minecraft:clear MagTest'); await cmd('minecraft:kill @e[type=husk]'); await cmd('minecraft:tp MagTest 0.5 -60 0.5 0 0')
  const katy = [-30, -20, -15, -10, -5, 0, 5, 10, 15, 20, 30]
  for (const tryb of ['pojedyncze', 'podwojne']) {
    await cmd('minecraft:kill @e[type=husk]')
    for (const k of katy) { const r = k * Math.PI / 180; const x = 0.5 - 3 * Math.sin(r), z = 0.5 + 3 * Math.cos(r)
      await cmd(`minecraft:summon husk ${x.toFixed(2)} -60 ${z.toFixed(2)} {NoAI:1b,Silent:1b,Tags:["k${k < 0 ? 'm' + (-k) : k}"],Health:40f,attributes:[{id:"minecraft:max_health",base:40}]}`) }
    await cmd('zaklecia przypisz MagTest plomienie prawa'); await cmd(`zaklecia przypisz MagTest ${tryb === 'podwojne' ? 'plomienie' : 'nic'} lewa`)
    bot.setQuickBarSlot(0); await sleep(700); await bot.look(0, 0, true)   // mineflayer yaw 0 = polnoc; odwracamy tp
    await cmd('minecraft:tp MagTest 0.5 -60 0.5 0 0'); await sleep(500); await cmd('mmocore admin resource-mana set MagTest 100')
    const czat = []; bot.on('messagestr', m => { if (m.includes('[dbg]')) czat.push(m) }); bot.activateItem(); await sleep(400); const u = {}; for (const k of katy) u[(await cmd(`data get entity @e[tag=k${k < 0 ? 'm' + (-k) : k},limit=1] UUID`)).slice(-40)] = k; console.log('uuid->kat', JSON.stringify(u)); console.log('obrot serwer:', await cmd('data get entity MagTest Rotation'), '| bot yaw', bot.entity.yaw.toFixed(2)); await sleep(700); bot.deactivateItem(); await sleep(600)
    const wynik = []
    for (const k of katy) { const h = (await cmd(`data get entity @e[tag=k${k < 0 ? 'm' + (-k) : k},limit=1] Health`)).match(/([\d.]+)f/)?.[1]; wynik.push(`${k}°:${+h < 40 ? 'TRAF' : '-'}`) }
    console.log(tryb.padEnd(10), wynik.join('  ')); console.log(czat.slice(0, 14).join('\n'))
  }
  await cmd('minecraft:kill @e[type=husk]'); bot.quit(); await rcon.end(); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(1) })
