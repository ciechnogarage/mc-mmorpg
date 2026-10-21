'use strict'
const H = require('/e/lib/harness'); const { rcon } = require('/k/rcon.js')
module.exports = async function (r) {
  const nick = 'EvH1' + Date.now() % 10000
  const ok = async (slot, pred) => (await rcon(`execute if items entity ${nick} ${slot} ${pred}`)).includes('passed')
  const id = x => `*[custom_data~{MMOITEMS_ITEM_ID:"${x}"}]`
  const daj = async (x, cel) => { await rcon(`mi give SWORD ${x} ${nick} 1`); await H.sleep(700); for (let k = 0; k < 36; k++) if (await ok('container.' + k, id(x))) { if ('container.' + k !== cel) { await rcon(`minecraft:item replace entity ${nick} ${cel} from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`) } break } await H.sleep(500) }
  const dmg = async s => { const o = await rcon(`data get entity ${nick} ${s}.components."minecraft:custom_data".MMOITEMS_ATTACK_DAMAGE`); const m = o.match(/([\d.]+)d/); return m ? parseFloat(m[1]) : null }
  const hp = async () => { const o = await rcon(`data get entity @e[tag=qa_db,limit=1] Health`); const m = o.match(/([\d.]+)f/); return m ? parseFloat(m[1]) : null }
  const szybkosc = async () => { const o = await rcon(`attribute ${nick} minecraft:attack_speed get`); const m = o.match(/is ([\d.]+)/); return m ? parseFloat(m[1]) : null }
  const ma = async (s, komp) => !(await rcon(`data get entity ${nick} ${s}.components."minecraft:${komp}"`)).includes('Found no')
  let bot = await H.connect(nick); const msgs = []; bot.on('actionBar', m => msgs.push(m.toString())); await H.sleep(3500)
  await rcon(`minecraft:gamerule spawn_mobs false`); await rcon(`mmocore admin attribute give ${nick} bieglosc-miecz 1`); await rcon(`mmocore admin level set ${nick} main 30`)
  bot.setQuickBarSlot(0); await H.sleep(300)
  await daj('MIECZ_KAMIENNY', 'container.0')
  const sz1 = await szybkosc()
  await daj('PRZ_WOJ_MIECZ_2', 'container.20')
  await bot.clickWindow(20, 0, 0); await H.sleep(400); await bot.clickWindow(45, 0, 0); await H.sleep(1500)
  const dP = await dmg('Inventory[{Slot:0b}]'), dL = await dmg('equipment.offhand'), L = dL / dP
  const sz2 = await szybkosc()
  console.log('prawa', dP, 'lewa', dL.toFixed(2), 'L/P', L.toFixed(3), 'szybkosc 1 bron', sz1, '2 bronie', sz2)
  r.assert(await ma('equipment.offhand', 'consumable'), 'lewa uzbrojona (consumable) przy dwoch broniach')
  r.assert(sz2 && sz1 && Math.abs(sz2 / sz1 - 1.33) < 0.02, 'premia szybkosci ataku +33% przy dwoch broniach', [sz1, sz2])
  const cel = async () => {
    await rcon(`minecraft:kill @e[tag=qa_db]`); const p = bot.entity.position
    await rcon(`minecraft:summon husk ${(p.x + 1.6).toFixed(2)} ${p.y} ${p.z.toFixed(2)} {NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:["qa_db"],attributes:[{id:"minecraft:max_health",base:2000d}],Health:2000f}`); await H.sleep(1500)
    const h = bot.nearestEntity(e => e.name === 'husk'); await bot.lookAt(h.position.offset(0, 1, 0), true); await H.sleep(600); return h
  }
  const ppm = async (ms, shift) => { if (shift) bot.setControlState('sneak', true); await H.sleep(150); const a = await hp(); bot.activateItem(true); await H.sleep(ms); bot.deactivateItem(); await H.sleep(900); if (shift) bot.setControlState('sneak', false); await H.sleep(400); return +(a - await hp()).toFixed(2) }
  let h = await cel()
  const lpm = []; for (let i = 0; i < 4; i++) { const a = await hp(); bot.attack(h); await H.sleep(1100); lpm.push(+(a - await hp()).toFixed(2)) }
  console.log('LPM', lpm); const R = lpm[0]
  r.assert(Math.abs(lpm[1] / R - L) < 0.1 * L, 'LPM naprzemiennie, 2. cios = lewa', (lpm[1] / R).toFixed(3))
  const tap = await ppm(80); console.log('PPM stukniecie', tap, '~', (R * L).toFixed(2))
  r.assert(Math.abs(tap - R * L) < 0.1 * R * L, 'PPM stukniecie = zwykly cios lewa', tap)
  const mocny = await ppm(900); console.log('PPM mocny', mocny, '~', (R * L * 2).toFixed(2))
  r.assert(Math.abs(mocny - 2 * R * L) < 0.1 * 2 * R * L, 'PPM przytrzymany = mocny cios lewa x2', mocny)
  const podw = await ppm(900, true); console.log('Shift+PPM podwojny', podw, '~', (R * 1.5 * (2 * L + 1)).toFixed(2))
  r.assert(Math.abs(podw - R * 1.5 * (2 * L + 1)) < 0.1 * R * 1.5 * (2 * L + 1), 'Shift+PPM = podwojny mocny 1,5x(2L+P)', podw)
  // brak staminy: zeruj stamine przed kazdym mocnym (cel od nowa - zachwianie odpycha)
  h = await cel(); const bez = []
  for (let i = 0; i < 2; i++) { await rcon(`mmocore admin resource-stamina set ${nick} 0`); await H.sleep(200); bez.push(await ppm(900)) }
  await rcon(`mmocore admin resource-stamina set ${nick} 0`); await H.sleep(200); const bezPodw = await ppm(900, true)
  console.log('bez staminy: mocny', bez, 'podwojny', bezPodw, 'zwykly lewa ~', (R * L).toFixed(2))
  r.assert(bez.every(x => Math.abs(x - R * L) < 0.1 * R * L) && Math.abs(bezPodw - R * L) < 0.1 * R * L, 'bez staminy mocny i podwojny = zwykly cios lewa', [bez, bezPodw])
  // higiena: lewa do plecaka - bez consumable
  await bot.clickWindow(45, 0, 0); await H.sleep(400); await bot.clickWindow(22, 0, 0); await H.sleep(1500)
  r.assert(await ok('container.22', id('PRZ_WOJ_MIECZ_2')) && !(await ma('Inventory[{Slot:22b}]', 'consumable')), 'bron zdjeta z off-handa traci consumable')
  const sz3 = await szybkosc(); r.assert(Math.abs(sz3 - sz1) < 0.01, 'premia szybkosci znika przy jednej broni', [sz1, sz3])
  await rcon(`minecraft:kill @e[tag=qa_db]`)
  await H.quit(bot)
}
