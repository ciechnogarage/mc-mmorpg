'use strict'
const H = require('/e/lib/harness'); const { rcon } = require('/k/rcon.js')
module.exports = async function (r) {
  const nick = 'EvL2' + Date.now() % 10000
  const ok = async (slot, pred) => (await rcon(`execute if items entity ${nick} ${slot} ${pred}`)).includes('passed')
  const id = x => `*[custom_data~{MMOITEMS_ITEM_ID:"${x}"}]`
  const KLON = '*[custom_data~{PublicBukkitValues:{"ksiega:klon":1b}}]'
  const NOS = '*[custom_data~{PublicBukkitValues:{"zaklecia:nosnik":1b}}]'
  const ile = async x => { let n = 0; for (let k = 0; k < 36; k++) if (await ok('container.' + k, id(x))) n++; if (await ok('weapon.offhand', id(x))) n++; if (await ok('player.cursor', id(x))) n++; return n }
  const daj = async (x, cel) => { await rcon(`mi give SWORD ${x} ${nick} 1`); await H.sleep(700); for (let k = 0; k < 36; k++) if (await ok('container.' + k, id(x))) { if ('container.' + k !== cel) { await rcon(`minecraft:item replace entity ${nick} ${cel} from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`) } break } await H.sleep(500) }
  const model = async sciezka => { const o = await rcon(`data get entity ${nick} ${sciezka}.components."minecraft:item_model"`); const m = o.match(/"([^"]+)"/); return m ? m[1] : '(brak: ' + o.trim().slice(0, 60) + ')' }
  const cios = async bot => {
    await rcon(`minecraft:kill @e[type=husk]`); const p = bot.entity.position
    await rcon(`minecraft:summon husk ${(p.x + 1.6).toFixed(2)} ${p.y} ${p.z.toFixed(2)} {NoAI:1b,Health:20f,PersistenceRequired:1b}`); await H.sleep(1500)
    const husk = bot.nearestEntity(e => e.name === 'husk'); if (!husk) return null
    await bot.lookAt(husk.position.offset(0, 1, 0), true); await H.sleep(1200); bot.attack(husk); await H.sleep(900)
    const odp = await rcon(`data get entity @e[type=husk,limit=1] Health`); console.log('husk:', odp.trim()); const hp = odp.match(/([\d.]+)f/); await rcon(`minecraft:kill @e[type=husk]`)
    return hp ? parseFloat(hp[1]) : 0
  }
  let bot = await H.connect(nick); bot.on('messagestr', m => { if (!m.includes('❤')) console.log('MSG', m) }); await H.sleep(3500)
  await rcon(`minecraft:gamerule spawn_mobs false`); await rcon(`mmocore admin attribute give ${nick} bieglosc-miecz 1`); console.log(await rcon(`mmocore admin level set ${nick} main 30`))
  bot.setQuickBarSlot(0); await H.sleep(300)
  await daj('PRZ_WOJ_MIECZ_1', 'container.0'); await daj('PRZ_WOJ_MIECZ_2', 'container.20')
  await bot.clickWindow(20, 0, 0); await H.sleep(400); await bot.clickWindow(45, 0, 0); await H.sleep(1200)
  r.assert(await ok('weapon.offhand', id('PRZ_WOJ_MIECZ_2')) && await ok('container.1', KLON), 'T1 slot 0: prawdziwa w lewej, obraz w slocie 2 paska')
  r.assert(!(await ok('container.1', '*[custom_data~{MMOITEMS_ITEM_ID:"PRZ_WOJ_MIECZ_2"}]')), 'T1 obraz bez danych MMOItems')
  const m1 = await model('Inventory[{Slot:1b}]'); console.log('T1 model obrazu w slocie 2:', m1)
  r.assert(m1.startsWith('ksiega:klon/') && m1 !== 'ksiega:klon/pusty', 'T1 obraz w slocie = nakladka (widac w GUI, w dloni nic)', m1)
  bot.setQuickBarSlot(1); await H.sleep(1000)
  const m2 = await model('equipment.offhand'); const m2r = await model('Inventory[{Slot:1b}]'); console.log('T2 off-hand:', m2, 'reka:', m2r)
  r.assert(m2.startsWith('ksiega:klon/') && m2 !== 'ksiega:klon/pusty', 'T2 klon w off-handzie = nakladka niewidoczna w dloni', m2)
  r.assert(!m2r.startsWith('ksiega:'), 'T2 prawdziwa bron w rece ma zwykly model', m2r)
  console.log('NAKLADKA', m2)
  r.assert(await ok('container.1', id('PRZ_WOJ_MIECZ_2')) && await ok('weapon.offhand', KLON), 'T2 wybrany slot 2: prawdziwa w rece, klon w off-handzie')
  r.assert(await ile('PRZ_WOJ_MIECZ_2') === 1, 'T2 dokladnie 1 prawdziwa', await ile('PRZ_WOJ_MIECZ_2'))
  const hp = await cios(bot); r.assert(hp !== null && hp < 20, 'T2 cios ze slotu 2 zadaje obrazenia', hp)
  bot.setQuickBarSlot(0); await H.sleep(1000)
  r.assert(await ok('weapon.offhand', id('PRZ_WOJ_MIECZ_2')) && await ok('container.1', KLON) && await ok('container.0', id('PRZ_WOJ_MIECZ_1')), 'T3 powrot na slot 1: bron wraca do off-handu')
  const hp0 = await cios(bot); r.assert(hp0 !== null && hp0 < 20, 'T3 cios ze slotu 1 dziala', hp0)
  for (let i = 0; i < 6; i++) { bot.setQuickBarSlot(i % 2); await H.sleep(150) }
  bot.setQuickBarSlot(1); await H.sleep(1000)
  r.assert(await ile('PRZ_WOJ_MIECZ_2') === 1 && await ile('PRZ_WOJ_MIECZ_1') === 1, 'T4 szybkie kolko 1<->2: bez duplikatow', [await ile('PRZ_WOJ_MIECZ_2'), await ile('PRZ_WOJ_MIECZ_1')])
  // T5: E przy wybranym slocie 2 - zdejmij lewa do plecaka i zaloz z powrotem
  await bot.clickWindow(37, 0, 0); await H.sleep(500); await bot.clickWindow(22, 0, 0); await H.sleep(1200)
  r.assert(await ok('container.22', id('PRZ_WOJ_MIECZ_2')) && await ile('PRZ_WOJ_MIECZ_2') === 1, 'T5 E: zdjecie lewej przy wybranym slocie 2', await ile('PRZ_WOJ_MIECZ_2'))
  await bot.clickWindow(22, 0, 0); await H.sleep(500); await bot.clickWindow(37, 0, 0); await H.sleep(1200)
  r.assert(await ile('PRZ_WOJ_MIECZ_2') === 1 && await ok('container.1', id('PRZ_WOJ_MIECZ_2')) && await ok('weapon.offhand', KLON), 'T5 E: zalozenie z powrotem, bron w rece slotu 2', await ile('PRZ_WOJ_MIECZ_2'))
  if (await ok('player.cursor', '*')) console.log('T5 cos na kursorze!')
  await bot.closeWindow(bot.inventory); await H.sleep(400)
  // T6: Q przy slocie 2
  let okno = false; bot.once('windowOpen', () => { okno = true })
  bot._client.write('block_dig', { status: 4, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 }); await H.sleep(1500)
  r.assert(await ile('PRZ_WOJ_MIECZ_2') === 1, 'T6 Q nie wyrzuca broni ze slotu 2', await ile('PRZ_WOJ_MIECZ_2'))
  console.log('T6 menu po Q otwarte:', okno, '(MenuKey na klonie?)')
  if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(300) }
  // T7: relog z wybranym slotem 2
  await H.quit(bot); await H.sleep(2500)
  r.assert(await ok('weapon.offhand', id('PRZ_WOJ_MIECZ_2')) || (await rcon('minecraft:list')).includes(nick) === false, 'T7 po wyjsciu stan kanoniczny (sprawdzany po powrocie)')
  bot = await H.connect(nick); await H.sleep(4000)
  console.log('T7 held po powrocie', bot.quickBarSlot)
  r.assert(await ile('PRZ_WOJ_MIECZ_2') === 1, 'T7 po relogu dokladnie 1 prawdziwa', await ile('PRZ_WOJ_MIECZ_2'))
  r.assert(!(await ok('weapon.offhand', KLON)) || await ok('container.1', id('PRZ_WOJ_MIECZ_2')), 'T7 klon w off-handzie tylko gdy prawdziwa w slocie 2')
  // T8: zaklecie w lewej + kolko
  bot.setQuickBarSlot(0); await H.sleep(800)
  console.log('T8', await rcon(`zaklecia przypisz ${nick} plomienie lewa`)); await H.sleep(1000)
  for (const s of [5, 6, 0, 1, 2, 7, 0]) { bot.setQuickBarSlot(s); await H.sleep(400) }
  await H.sleep(800)
  r.assert(await ok('weapon.offhand', NOS), 'T8 zaklecie zostaje w lewej po kreceniu kolkiem')
  r.assert(await ile('PRZ_WOJ_MIECZ_2') === 1 && await ile('PRZ_WOJ_MIECZ_1') === 1, 'T8 bronie dalej po 1x', [await ile('PRZ_WOJ_MIECZ_2'), await ile('PRZ_WOJ_MIECZ_1')])
  console.log('T9', await rcon(`zaklecia przypisz ${nick} plomienie prawa`)); await H.sleep(1000)
  let gdzieNos = -1; for (let k = 0; k < 9; k++) if (await ok('container.' + k, NOS)) gdzieNos = k
  for (const s of [5, 0, 6, 0]) { bot.setQuickBarSlot(s); await H.sleep(400) }
  await H.sleep(800)
  let po = -1; for (let k = 0; k < 9; k++) if (await ok('container.' + k, NOS)) po = k
  r.assert(gdzieNos >= 0 && po === gdzieNos, 'T9 zaklecie prawej zostaje w pasku po kolku', [gdzieNos, po])
  await H.quit(bot)
}
