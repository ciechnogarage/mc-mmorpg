'use strict'
// Prototyp etapu 2, część 2: limit sług, słudzy walczą sami, Dusza za zabójstwo sługi.
const mineflayer = require('/g/node_modules/mineflayer'); const { attach } = require('/e/lib/resourcepack.js'); const { rcon } = require('/s/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const RASA = process.env.RASA || 'ork', TYP = process.env.TYP || 'zombified_piglin', nick = 'Ak7' + RASA.slice(0, 4)
const V = async p => (await rcon(`papi parse ${nick} ${p}`)).trim()
const F = bot => bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 })
const ile = async t => { const r = (await rcon(`execute if entity @e[type=${t},x=286,y=-63,z=286,dx=28,dy=12,dz=28]`)).trim(); const m = /Count: (\d+)/.exec(r); return m ? +m[1] : 0 }
;(async () => {
  await rcon('forceload add 280 280 320 320'); await rcon('minecraft:kill @e[type=!player,x=286,y=-63,z=286,dx=28,dy=12,dz=28]')
  const bot = mineflayer.createBot({ host: process.env.MC_HOST, username: nick, auth: 'offline', version: '1.21.11' }); attach(bot._client)
  const czat = []; bot.on('messagestr', m => { if (/dbg|Dusz/i.test(m)) czat.push(m.slice(0,60)) })
  await new Promise(r => bot.once('spawn', r)); await sleep(5000)
  await rcon(`cc testpostac ${nick} ${RASA} akolita`); await sleep(14000)
  await rcon(`mmocore admin slot bind ${nick} 1 AKO_WEZWANIE`); await rcon(`minecraft:tp ${nick} 300.5 -59 296.5 0 0`); await sleep(1500)
  await rcon(`mmocore admin resource-mana set ${nick} 10`); if (process.env.TAG) await rcon(`tag ${nick} add ${process.env.TAG}`)
  const bron = bot.quickBarSlot; const wolne = [...Array(9).keys()].filter(s => s !== bron)
  const rzuc = async k => { F(bot); await sleep(300); bot.setQuickBarSlot(wolne[k]); await sleep(300); F(bot); await sleep(300); if (bot.quickBarSlot !== bron) bot.setQuickBarSlot(bron) }
  for (let i = 0; i < 4; i++) { await rzuc(0); await sleep(3300); console.log(`wezwanie ${i + 1}: slug`, await ile(TYP), '| dusze', await V('%mmocore_mana%')) }
  await rcon('mm mobs spawn GAJ_WILK 1 world,300.5,-59,303.5'); await sleep(1000)
  const w = bot.nearestEntity(e => e.name === 'wolf'); await bot.lookAt(w.position.offset(0, .4, 0), true); bot.attack(w); const d0 = +(await V('%mmocore_mana%'))
  const t0 = Date.now(); while (Date.now() - t0 < 30000 && await ile('wolf') > 0) await sleep(1000)
  console.log('wilk po jednym ciosie gracza:', (await ile('wolf')) ? 'ZYJE po 30 s' : `padl po ${((Date.now() - t0) / 1000).toFixed(0)} s`, '| dusze', d0, '->', await V('%mmocore_mana%'))
  console.log('CZAT:', JSON.stringify(czat.slice(-10)))
  bot.quit(); await sleep(300); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(2) })
