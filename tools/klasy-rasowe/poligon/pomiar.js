'use strict'
const mineflayer = require('/e/node_modules/mineflayer')
const { attach } = require('/e/lib/resourcepack.js')
const { rcon } = require('/s/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const KLASY = process.env.KLASY.split(',')
const num = s => { const m = /(-?[\d.]+)\s*$/.exec(s.trim()); return m ? +m[1] : s.trim() }
;(async () => {
  const bot = mineflayer.createBot({ host: process.env.MC_HOST, username: 'Sonda', auth: 'offline', version: '1.21.11' })
  attach(bot._client)
  await new Promise((res, rej) => { bot.once('spawn', res); bot.once('kicked', r => rej(new Error('kick ' + JSON.stringify(r)))); bot.once('error', rej) })
  await sleep(4000)
  const wyn = []
  for (const k of KLASY) {
    await rcon(`mmocore class select Sonda ${k}`); await sleep(1500)
    for (const poz of [1, 10]) {
      await rcon(`mmocore admin level set Sonda main ${poz}`); await sleep(1200)
      bot.setQuickBarSlot(8); await sleep(400)  // slot 8 = Dziennik/pusty — "ręka bez broni"
      const asPusta = num(await rcon('attribute Sonda minecraft:attack_speed get'))
      let bron = null, asBron = null
      for (let s = 0; s < 8; s++) { const it = bot.inventory.slots[36 + s]; if (it) { bot.setQuickBarSlot(s); bron = it.name; break } }
      await sleep(500); if (bron) asBron = num(await rcon('attribute Sonda minecraft:attack_speed get'))
      const r = {
        klasa: k, poz,
        klasaTeraz: (await rcon('papi parse Sonda %mmocore_class_id%')).trim(),
        hp: num(await rcon('attribute Sonda minecraft:max_health get')),
        ruch: num(await rcon('attribute Sonda minecraft:movement_speed get')),
        asPusta, bron, asBron,
        zasob: (await rcon('papi parse Sonda %mmocore_mana%/%mmocore_max_mana%')).trim(),
        nazwaZasobu: (await rcon('papi parse Sonda %mmocore_mana_name%')).trim(),
        regen: (await rcon('papi parse Sonda %mythiclib_stat_mana_regeneration%')).trim(),
      }
      wyn.push(r); console.log(JSON.stringify(r))
    }
  }
  // Wiara: atrybut istnieje i daje bonus
  console.log('WIARA give:', (await rcon('mmocore admin attribute give Sonda wiara 4')).trim())
  console.log('WIARA check:', (await rcon('mmocore admin attribute check Sonda wiara')).trim())
  console.log('SKILL_DAMAGE:', (await rcon('papi parse Sonda %mythiclib_stat_skill_damage%')).trim())
  console.log('MAX_MANA:', (await rcon('papi parse Sonda %mmocore_max_mana%')).trim())
  bot.quit(); await sleep(500); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(1) })
