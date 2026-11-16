'use strict'
const mineflayer = require('/e/node_modules/mineflayer'); const { attach } = require('/e/lib/resourcepack.js'); const { rcon } = require('/s/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const num = s => { const m = /(-?[\d.]+)\s*$/.exec(s.trim()); return m ? +m[1] : s.trim() }
const wejdz = async (nick) => { const bot = mineflayer.createBot({ host: process.env.MC_HOST, username: nick, auth: 'offline', version: '1.21.11' }); attach(bot._client)
  const czat = []; bot.on('messagestr', m => czat.push(m))
  await new Promise((res, rej) => { bot.once('spawn', res); bot.once('kicked', r => rej(new Error('kick ' + r))) }); await sleep(6000); bot.czat = czat; return bot }
const stan = async (nick, t) => { const r = {}
  for (const [k, p] of Object.entries({ klasa: '%mmocore_class_id%', poziom: '%mmocore_level%', zasob: '%mmocore_mana_name%' })) r[k] = (await rcon(`papi parse ${nick} ${p}`)).trim()
  r.hp = num(await rcon(`attribute ${nick} minecraft:max_health get`)); r.ruch = num(await rcon(`attribute ${nick} minecraft:movement_speed get`))
  r.as = num(await rcon(`attribute ${nick} minecraft:attack_speed get`)); r.swiat = (await rcon(`data get entity ${nick} Dimension`)).replace(/.*data: /, '')
  console.log(t, nick, JSON.stringify(r)); return r }
;(async () => {
  for (const c of ['forceload add 143 191 155 203', 'fill 143 97 191 155 97 203 minecraft:stone']) console.log(c, '=>', (await rcon(c)).trim())
  const PARY = (process.env.PARY || 'ork:wojownik').split(',')
  for (let i = 0; i < PARY.length; i++) {
    const [rasa, arch] = PARY[i].split(':'); const nick = 'Sonda' + i
    let bot = await wejdz(nick); await stan(nick, 'PRZED')
    // zamek 1: gracz (bez OP i z OP) nie moze
    bot.chat(`/cc testpostac ${nick} ${rasa} ${arch}`); await sleep(1500)
    await rcon(`op ${nick}`); await sleep(500); bot.chat(`/cc testpostac ${nick} ${rasa} ${arch}`); await sleep(1500); await rcon(`deop ${nick}`)
    const czatZ = bot.czat.filter(m => /testpostac|Zlecono|Brak|\/cc/.test(m)); console.log('CZAT po probach gracza:', JSON.stringify(czatZ.slice(0, 4)))
    console.log('KONSOLA:', (await rcon(`cc testpostac ${nick} ${rasa} ${arch} Test${i}`)).trim())
    await sleep(6000); await stan(nick, 'PO')
    // bron startowa w reku -> szybkosc ataku z bronia
    for (let s = 0; s < 9; s++) { const it = bot.inventory.slots[36 + s]; if (it && !/book|bundle|writable/.test(it.name)) { bot.setQuickBarSlot(s); await sleep(800); console.log('BRON', it.name, 'slot', s, 'attack_speed =', num(await rcon(`attribute ${nick} minecraft:attack_speed get`)), 'base =', num(await rcon(`attribute ${nick} minecraft:attack_speed base get`))); break } }
    bot.quit(); await sleep(3000); bot = await wejdz(nick); await stan(nick, 'PO-RELOGU'); bot.quit(); await sleep(1500)
  }
  process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(1) })
