'use strict'
const H = require('/e/lib/harness'); const { rcon } = require('/k/rcon.js')
module.exports = async function (r) {
  const nick = 'EvK' + Date.now() % 10000
  const ok = async (slot, pred) => (await rcon(`execute if items entity ${nick} ${slot} ${pred}`)).includes('passed')
  const id = x => `*[custom_data~{MMOITEMS_ITEM_ID:"${x}"}]`
  const daj = async (x, cel) => { await rcon(`mi give SWORD ${x} ${nick} 1`); await H.sleep(700); for (let k = 0; k < 36; k++) if (await ok('container.' + k, id(x))) { if ('container.' + k !== cel) { await rcon(`minecraft:item replace entity ${nick} ${cel} from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`) } break } await H.sleep(500) }
  const hp = async () => { const o = await rcon(`data get entity @e[tag=qa_k,limit=1] Health`); const m = o.match(/([\d.]+)f/); return m ? parseFloat(m[1]) : null }
  const st = async () => parseFloat((await rcon(`papi parse ${nick} %mmocore_stamina%`)).trim().replace(',', '.'))
  let bot = await H.connect(nick); await H.sleep(3500)
  await rcon(`minecraft:gamerule spawn_mobs false`); await rcon(`mmocore admin attribute give ${nick} bieglosc-miecz 1`); await rcon(`mmocore admin level set ${nick} main 30`)
  await rcon(`minecraft:execute at ${nick} run fill ~-8 ~-1 ~-8 ~8 ~-1 ~8 stone`); await rcon(`minecraft:execute at ${nick} run fill ~-8 ~ ~-8 ~8 ~3 ~8 air`)
  bot.setQuickBarSlot(0); await H.sleep(300)
  await daj('MIECZ_KAMIENNY', 'container.0'); await daj('PRZ_WOJ_MIECZ_2', 'container.20')
  await bot.clickWindow(20, 0, 0); await H.sleep(400); await bot.clickWindow(45, 0, 0); await H.sleep(1500)
  const start = bot.entity.position.clone()
  // ustaw gracza na srodku, patrzy na +X; husk na odleglosci d
  const ustaw = async d => {
    await rcon(`minecraft:kill @e[tag=qa_k]`); await rcon(`minecraft:teleport ${nick} ${start.x.toFixed(2)} ${start.y} ${start.z.toFixed(2)} -90 10`); await H.sleep(800)
    await rcon(`minecraft:summon husk ${(start.x + d).toFixed(2)} ${start.y} ${start.z.toFixed(2)} {NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:["qa_k"],attributes:[{id:"minecraft:max_health",base:1000d}],Health:1000f}`)
    await rcon(`mmocore admin resource-stamina set ${nick} 1000`); await H.sleep(1500)
  }
  const mocny = async klawisz => {
    const a = await hp(); const p0 = bot.entity.position.clone(); const s0 = await st()
    bot.activateItem(true); await H.sleep(800)
    if (klawisz) bot.setControlState(klawisz, true); await H.sleep(100)
    bot.deactivateItem(); await H.sleep(80); if (klawisz) bot.setControlState(klawisz, false); await H.sleep(1200)
    const p1 = bot.entity.position
    return { dmg: +(a - await hp()).toFixed(2), dx: +(p1.x - p0.x).toFixed(2), dz: +(p1.z - p0.z).toFixed(2), stamina: +(s0 - await st()).toFixed(2), s0 }
  }
  await ustaw(1.6); const zwykly = await mocny(null); console.log('mocny bez kierunku', zwykly)
  await ustaw(4.0); const bezW = await mocny(null); console.log('husk 4.0, bez W', bezW)
  await ustaw(4.0); const zW = await mocny('forward'); console.log('husk 4.0, z W (wypad)', zW)
  await ustaw(1.6); const zS = await mocny('back'); console.log('S (odskok)', zS)
  await ustaw(1.6); const zA = await mocny('left'); console.log('A (unik)', zA)
  r.assert(zwykly.dmg > 0, 'mocny trafia', zwykly.dmg)
  r.assert(bezW.dmg === 0, 'husk 4 bloki dalej: bez W poza zasiegiem', bezW.dmg)
  r.assert(zW.dmg > 0 && zW.dx > 0.6, 'W: wypad trafia dalej i doskakuje', [zW.dmg, zW.dx])
  r.assert(zS.dmg > 0 && zS.dx < -0.4, 'S: cios z odskokiem', [zS.dmg, zS.dx])
  r.assert(zA.dmg > 0 && Math.abs(zA.dz) > 0.4, 'A: cios z unikiem w bok', [zA.dmg, zA.dz])
  r.assert(Math.abs(zA.dmg - zwykly.dmg) < 0.05 * zwykly.dmg && Math.abs(zS.dmg - zwykly.dmg) < 0.05 * zwykly.dmg, 'kierunki: te same obrazenia co mocny', [zwykly.dmg, zS.dmg, zA.dmg])
  await rcon(`minecraft:kill @e[tag=qa_k]`)
  await H.quit(bot)
}
