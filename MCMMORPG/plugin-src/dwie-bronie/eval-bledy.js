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
  // T2 (odrzut) w bledy2.js - mob z AI i predkoscia 0 (moby bez AI ignoruja odrzut)
  // T1: PvP zablokowane (WorldGuard __global__ pvp deny)
  const b2 = await H.connect(ofiara); await H.sleep(3000)
  console.log(await rcon(`rg flag __global__ -w world pvp deny`))
  await tp(nick, 0, -90); await tp(ofiara, 1.6, 90); await H.sleep(1500)
  const hpO = await num(`data get entity ${ofiara} Health`); const xO = await num(`data get entity ${ofiara} Pos[0]`)
  await mocny()
  const hpO2 = await num(`data get entity ${ofiara} Health`); const xO2 = await num(`data get entity ${ofiara} Pos[0]`)
  const efekty = await rcon(`data get entity ${ofiara} active_effects`)
  console.log('PvP zablokowane: hp', hpO, '->', hpO2, 'x', xO, '->', xO2, 'efekty:', efekty.replace(/\s+/g, ' ').slice(0, 120))
  r.assert(hpO2 === hpO, 'T1 PvP zablokowane: brak obrazen', [hpO, hpO2])
  r.assert(Math.abs(xO2 - xO) < 0.01 && !efekty.includes('slowness'), 'T1 PvP zablokowane: bez odepchniecia i spowolnienia', [xO2 - xO, efekty.includes('slowness')])
  await rcon(`rg flag __global__ -w world pvp`); await H.quit(b2)
  // T3: prawa z umiejetnoscia PPM (LONG_SWORD: FIREBOLT)
  await rcon(`minecraft:item replace entity ${nick} container.30 from entity ${nick} container.0`); await rcon(`minecraft:item replace entity ${nick} container.0 with air`)
  await daj('LONG_SWORD', 'container.0'); await H.sleep(1500)
  console.log('LONG_SWORD w prawej:', await ok(nick, 'container.0', id('LONG_SWORD')), 'ability:', (await rcon(`data get entity ${nick} Inventory[{Slot:0b}].components."minecraft:custom_data".MMOITEMS_ABILITY`)).slice(0, 80))
  r.assert(!(await ma(nick, 'equipment.offhand', 'consumable')), 'T3 prawa z umiejetnoscia PPM: lewa NIE uzbrojona')
  // T4: wymagania lewej
  await rcon(`minecraft:item replace entity ${nick} container.0 from entity ${nick} container.30`)
  await rcon(`mmocore admin level set ${nick} main 1`); await H.sleep(1500)
  r.assert(!(await ma(nick, 'equipment.offhand', 'consumable')), 'T4 lewa ponad wymagania: nie uzbrojona')
  msgs.length = 0; bot.activateItem(false); await H.sleep(300); bot.deactivateItem(); await H.sleep(1200)
  console.log('komunikaty:', msgs.slice(0, 4))
  r.assert(msgs.some(m => m.includes('wymaga')), 'T4 komunikat o wymaganiach po PPM', msgs.slice(0, 3))
  await H.quit(bot)
}
