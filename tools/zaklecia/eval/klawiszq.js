'use strict'
setTimeout(() => process.exit(2), 60000)
const mineflayer = require('mineflayer'); const { Rcon } = require('rcon-client'); const rp = require('./resourcepack')
const HOST = 'zaklecia-proba'; const sleep = ms => new Promise(r => setTimeout(r, ms))
;(async () => {
  const rcon = await Rcon.connect({ host: HOST, port: 25575, password: 'zaklecia' })
  const bot = mineflayer.createBot({ host: HOST, port: 25565, username: 'MagTest', version: '1.21.11' }); rp.attach(bot._client)
  await new Promise(r => bot.once('spawn', r)); await sleep(2500); await rcon.send('minecraft:clear MagTest')
  let okno = ''; bot.on('windowOpen', w => { okno = JSON.stringify(w.title) })
  const q = async shift => { okno = ''; bot._client.write('player_input', { inputs: { shift } }); await sleep(400)
    bot._client.write('block_dig', { status: 4, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 }); await sleep(1500)
    bot._client.write('player_input', { inputs: {} }); const t = okno; if (bot.currentWindow) bot.closeWindow(bot.currentWindow); await sleep(600); return t }
  const a = await q(false), b = await q(true)
  console.log(/Menu/.test(a) && !/Ulubione/.test(a) ? 'PASS' : 'FAIL', 'Q (pusta reka) = hub', a.slice(0, 90))
  console.log(/Ulubione/.test(b) ? 'PASS' : 'FAIL', 'Shift+Q = ulubione', b.slice(0, 90))
  bot.quit(); await rcon.end(); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(1) })
