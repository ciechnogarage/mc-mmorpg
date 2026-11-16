'use strict'
const H = require('/e/lib/harness'); const { rcon } = require('/k/rcon.js')
module.exports = async function (r) {
  const nick = 'EvDb' + Date.now() % 10000
  const ok = async (slot, pred) => (await rcon(`execute if items entity ${nick} ${slot} ${pred}`)).includes('passed')
  const id = x => `*[custom_data~{MMOITEMS_ITEM_ID:"${x}"}]`
  const daj = async (x, cel) => { await rcon(`mi give SWORD ${x} ${nick} 1`); await H.sleep(700); for (let k = 0; k < 36; k++) if (await ok('container.' + k, id(x))) { if ('container.' + k !== cel) { await rcon(`minecraft:item replace entity ${nick} ${cel} from entity ${nick} container.${k}`); await rcon(`minecraft:item replace entity ${nick} container.${k} with air`) } break } await H.sleep(500) }
  const dmg = async sciezka => { const o = await rcon(`data get entity ${nick} ${sciezka}.components."minecraft:custom_data".MMOITEMS_ATTACK_DAMAGE`); const m = o.match(/([\d.]+)d/); return m ? parseFloat(m[1]) : null }
  const hp = async () => { const o = await rcon(`data get entity @e[tag=qa_db,limit=1] Health`); const m = o.match(/([\d.]+)f/); return m ? parseFloat(m[1]) : null }
  let bot = await H.connect(nick); await H.sleep(3500)
  await rcon(`minecraft:gamerule spawn_mobs false`); await rcon(`mmocore admin attribute give ${nick} bieglosc-miecz 1`); await rcon(`mmocore admin level set ${nick} main 30`)
  bot.setQuickBarSlot(0); await H.sleep(300)
  await daj('MIECZ_KAMIENNY', 'container.0'); await daj('PRZ_WOJ_MIECZ_2', 'container.20')
  await bot.clickWindow(20, 0, 0); await H.sleep(400); await bot.clickWindow(45, 0, 0); await H.sleep(1200)
  const dP = await dmg('Inventory[{Slot:0b}]'), dL = await dmg('equipment.offhand')
  console.log('ATTACK_DAMAGE prawa', dP, 'lewa', dL, 'proporcja', (dL / dP).toFixed(3))
  r.assert(dP && dL && dL !== dP, 'bronie maja rozne obrazenia', [dP, dL])
  const cel = async () => {
    await rcon(`minecraft:kill @e[tag=qa_db]`); const p = bot.entity.position
    await rcon(`minecraft:summon husk ${(p.x + 1.6).toFixed(2)} ${p.y} ${p.z.toFixed(2)} {NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:["qa_db"],attributes:[{id:"minecraft:max_health",base:500d}],Health:500f}`); await H.sleep(1500)
    const h = bot.nearestEntity(e => e.name === 'husk'); await bot.lookAt(h.position.offset(0, 1, 0), true); await H.sleep(600); return h
  }
  const seria = async (h, n, jak) => { const out = []; for (let i = 0; i < n; i++) { const a = await hp(); if (jak === 'ppm') bot.activateItem(); else if (jak === 'ppmIstota') bot.activateEntity(h); else bot.attack(h); await H.sleep(250); if (jak !== 'lpm') bot.deactivateItem(); await H.sleep(1150); out.push(+(a - await hp()).toFixed(2)) } return out }
  // 1: dwie bronie, LPM x4
  let h = await cel(); const lpm = await seria(h, 4, 'lpm'); console.log('LPM dwie bronie', lpm)
  r.assert(lpm.every(x => x > 0), 'LPM dwie bronie: kazdy cios zadaje obrazenia', lpm)
  r.assert(Math.abs(lpm[1] / lpm[0] - dL / dP) < 0.15 * (dL / dP), 'LPM 2. cios = lewa (proporcja ~ lewa/prawa)', (lpm[1] / lpm[0]).toFixed(3))
  // 2: PPM w powietrze i w istote
  h = await cel(); const ppm = await seria(h, 2, 'ppm'); console.log('PPM (use_item) dwie bronie', ppm)
  const ppmE = await seria(h, 2, 'ppmIstota'); console.log('PPM (klik w istote) dwie bronie', ppmE)
  r.assert(ppm.every(x => x > 0) && ppmE.every(x => x > 0), 'PPM zadaje cios', [ppm, ppmE])
  // 3: kontrola - pusta lewa
  await bot.clickWindow(45, 0, 0); await H.sleep(400); await bot.clickWindow(22, 0, 0); await H.sleep(1200)
  h = await cel(); const jedna = await seria(h, 3, 'lpm'); const ppm1 = await seria(h, 1, 'ppm'); console.log('jedna bron LPM', jedna, 'PPM', ppm1)
  r.assert(jedna.every(x => x > 0), 'jedna bron: LPM dziala', jedna)
  // 4: lewa z powrotem, slot 2 (klon w off-handzie)
  await bot.clickWindow(22, 0, 0); await H.sleep(400); await bot.clickWindow(45, 0, 0); await H.sleep(1200)
  bot.setQuickBarSlot(1); await H.sleep(1200)
  h = await cel(); const s2 = await seria(h, 3, 'lpm'); const s2p = await seria(h, 1, 'ppm'); console.log('slot 2 LPM', s2, 'PPM', s2p)
  r.assert(s2.every(x => x > 0), 'slot 2: LPM dziala', s2)
  r.assert(s2p[0] === 0, 'slot 2: PPM bez ciosu (klon w lewej)', s2p)
  // 5: lewa ponad wymagania (poziom 1, Miecz zolnierski wymaga 30)
  bot.setQuickBarSlot(0); await H.sleep(1200); await rcon(`mmocore admin level set ${nick} main 1`); await H.sleep(1000)
  h = await cel(); const wym = await seria(h, 3, 'lpm'); const wymP = await seria(h, 1, 'ppm'); console.log('poziom 1, lewa wymaga 30: LPM', wym, 'PPM', wymP)
  r.assert(wym.every(x => Math.abs(x - wym[0]) < 0.01) && wym[0] < 10, 'lewa bez wymagan: LPM bije tylko prawa', wym)
  r.assert(wymP[0] < 10, 'lewa bez wymagan: PPM nie bije lewa', wymP)
  await rcon(`minecraft:kill @e[tag=qa_db]`)
  await H.quit(bot)
}
