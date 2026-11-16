'use strict'
// Bot pokazowy: tryb z argv (podwojne|miecz), rzuca 5 s w kukle; klient QA patrzy z boku.
setTimeout(() => process.exit(2), 60000)
const mineflayer = require('mineflayer'); const { Rcon } = require('rcon-client'); const rp = require('./resourcepack')
const HOST = 'zaklecia-proba'; const sleep = ms => new Promise(r => setTimeout(r, ms)); const tryb = process.argv[2]
;(async () => {
  const rcon = await Rcon.connect({ host: HOST, port: 25575, password: 'zaklecia' }); const cmd = c => rcon.send(c)
  const bot = mineflayer.createBot({ host: HOST, port: 25565, username: 'MagTest', version: '1.21.11' }); rp.attach(bot._client)
  await new Promise(r => bot.once('spawn', r)); await sleep(1500)
  await cmd('minecraft:clear MagTest'); await cmd('minecraft:kill @e[type=zombie]'); await cmd('minecraft:tp MagTest 0.5 -60 0.5 0 0')
  await cmd('mm mobs spawn ZAK_KUKLA 1 world,0.5,-60,4.5'); bot.setQuickBarSlot(0); await sleep(300)
  if (tryb === 'podwojne') { await cmd('zaklecia przypisz MagTest plomienie prawa'); await cmd('zaklecia przypisz MagTest plomienie lewa') }
  else { await cmd('zaklecia przypisz MagTest odmrozenie lewa'); await cmd('minecraft:item replace entity MagTest hotbar.0 with minecraft:iron_sword') }
  await sleep(800); const z = Object.values(bot.entities).find(e => e.name === 'zombie'); if (z) await bot.lookAt(z.position.offset(0, 1.2, 0), true)
  await cmd('mmocore admin resource-mana set MagTest 100'); console.log('RZUCAM')
  bot.activateItem(tryb !== 'podwojne'); for (let i = 0; i < 10; i++) { await sleep(500); await cmd('mmocore admin resource-mana set MagTest 100') }
  bot.deactivateItem(); await sleep(500); bot.quit(); await rcon.end(); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(1) })
