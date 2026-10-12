'use strict'
const H = require('/e/lib/harness'); const { rcon } = require('/k/rcon.js')
module.exports = async function (r) {
  const t = Date.now() % 10000, nick = 'EvB' + t, ofiara = 'EvO' + t
  const ok = async (who, slot, pred) => (await rcon(`execute if items entity ${who} ${slot} ${pred}`)).includes('passed')
  const id = x => `*[custom_data~{MMOITEMS_ITEM_ID:"${x}"}]`
  const daj = async (x, cel, typ = 'SWORD') => { await rcon(`mi give ${typ} ${x} ${nick} 1`); await H.sleep(700); for (let k = 0; k < 36; k++) if (await ok(nick, 'container.' + k, id(x))) { if ('container.' + k !== cel) { await rcon(`minecraft:item replace entity ${nick} ${cel} from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`) } break } await H.sleep(500) }
  const ma = async (who, s, komp) => !(await rcon(`data get entity ${who} ${s}.components."minecraft:${komp}"`)).includes('Found no')
  const num = async cmd => { const m = (await rcon(cmd)).match(/(-?[\d.]+)[fd]?\s*$/m); return m ? parseFloat(m[1]) : null }
  let bot = await H.connect(nick); const msgs = []; bot.on('actionBar', m => msgs.push(m.toString())); bot.on('message', (m, pos) => { if (pos === 'game_info') msgs.push(m.toString()) }); await H.sleep(3500)
  await rcon(`minecraft:gamerule spawn_mobs false`); await rcon(`mmocore admin attribute give ${nick} bieglosc-miecz 1`); await rcon(`mmocore admin level set ${nick} main 30`)
  const p0 = bot.entity.position.clone()
  await rcon(`minecraft:fill ${Math.floor(p0.x) - 6} ${Math.floor(p0.y) - 1} ${Math.floor(p0.z) - 6} ${Math.floor(p0.x) + 6} ${Math.floor(p0.y) - 1} ${Math.floor(p0.z) + 6} stone`)
  await rcon(`minecraft:fill ${Math.floor(p0.x) - 6} ${Math.floor(p0.y)} ${Math.floor(p0.z) - 6} ${Math.floor(p0.x) + 6} ${Math.floor(p0.y) + 3} ${Math.floor(p0.z) + 6} air`)
  bot.setQuickBarSlot(0); await H.sleep(300)
  await daj('MIECZ_KAMIENNY', 'container.0'); await daj('PRZ_WOJ_MIECZ_2', 'container.20')
  await bot.clickWindow(20, 0, 0); await H.sleep(400); await bot.clickWindow(45, 0, 0); await H.sleep(1500)
  const tp = async (kto, dx, yaw) => rcon(`minecraft:teleport ${kto} ${(p0.x + dx).toFixed(2)} ${p0.y} ${p0.z.toFixed(2)} ${yaw} 10`)
  const mocny = async () => { await rcon(`mmocore admin resource-stamina set ${nick} 1000`); await H.sleep(300); bot.activateItem(true); await H.sleep(900); bot.deactivateItem(); await H.sleep(1300) }
  // T2: odpornosc na odrzut
  for (const kb of [0, 1]) {
    await rcon(`minecraft:kill @e[tag=qa_b]`); await tp(nick, 0, -90); await H.sleep(600)
    await rcon(`minecraft:summon husk ${(p0.x + 1.6).toFixed(2)} ${p0.y} ${p0.z.toFixed(2)} {PersistenceRequired:1b,Silent:1b,Tags:["qa_b"],attributes:[{id:"minecraft:max_health",base:1000d},{id:"minecraft:movement_speed",base:0d},{id:"minecraft:knockback_resistance",base:${kb}d}],Health:1000f}`); await H.sleep(1200)
    const x0 = await num(`data get entity @e[tag=qa_b,limit=1] Pos[0]`); await mocny(); const x1 = await num(`data get entity @e[tag=qa_b,limit=1] Pos[0]`)
    const hp = await num(`data get entity @e[tag=qa_b,limit=1] Health`)
    console.log('odpornosc', kb, 'przesuniecie', (x1 - x0).toFixed(3), 'hp', hp)
    if (kb === 0) r.assert(x1 - x0 > 0.1 && hp < 1000, 'T2 bez odpornosci: mocny trafia i odpycha', [x1 - x0, hp])
    else r.assert(Math.abs(x1 - x0) < 0.01 && hp < 1000, 'T2 odpornosc 1.0: trafia, ale nie odpycha', [x1 - x0, hp])
  }
  await rcon(`minecraft:kill @e[tag=qa_b]`)
  await H.quit(bot)
}
