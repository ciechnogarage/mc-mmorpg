'use strict'
// Arena Bagien (uogolnione forge/tools/przeciwnicy/arena_bot.js): bot NIEUWAZNY (stoi i bije) vs UWAZNY (reaguje na
// dzwiek zapowiedzi z zapowiedzi.json, generowanego przez tools/bagna/mm_bagna.py: {MOB: [{dzwiek, pitch, reakcja, ms}]}).
// env: MOB=BAGNA_MULOWIEC TRYB=nieuwazny|uwazny POWT=3 MC_HOST=bagna-test RCON_PASS=bagnatest
//      ARENA=x,y,z (srodek, domyslnie 300.5,-59,300.5) ZAPOWIEDZI=/b/mm/zapowiedzi.json MOBS_YML=/b/mm/Bagna_mobs.yml
//      BRON=stone_sword ILE=1 (ile mobow naraz - roj) DEBUG=1
// Typ encji bazowej czytany z MOBS_YML (Type:). Jesli mineflayer nie widzi bazy pod modelem ME, zycie moba sprawdzamy RCON-em.
const fs = require('fs')
const mineflayer = require('/g/node_modules/mineflayer'); const { pathfinder, Movements } = require('/g/node_modules/mineflayer-pathfinder')
const { rcon: rconRaw } = require('/h/rcon.js')
// rcon z limitem 4 s (pomiar 03.10: petla walki wisiala na odpowiedzi RCON)
const rcon = c => Promise.race([rconRaw(c).catch(e => { if (E.DEBUG) console.log('  rcon BLAD', c.slice(0, 40), e.message); return '' }), new Promise(r => setTimeout(() => r('TIMEOUT'), 4000))])
const E = process.env
const MOB = E.MOB, TRYB = E.TRYB || 'nieuwazny', POWT = +(E.POWT || 3), ILE = +(E.ILE || 1), HOST = E.MC_HOST || 'bagna-test'
const [AX, AY, AZ] = (E.ARENA || '0.5,-60,0.5').split(',').map(Number)
// START = pozycja gracza, SPAWN = pozycja moba (domyslnie 7 bl. przed/za srodkiem); LIMIT_S = limit walki (elity 120)
const START = (E.START || `${AX},${AY},${AZ - 7}`).split(',').map(Number), SPAWN = (E.SPAWN || `${AX},${AY},${AZ + 6}`).split(',').map(Number)
const LIMIT = 1000 * +(E.LIMIT_S || 60)
// STOJ_S: przez pierwsze N s bot nie podchodzi (pod ostrzalem dystansowca, jak w lochu za woda) - potem dobiega i bije
const STOJ = 1000 * +(E.STOJ_S || 0)
// uwazny reaguje na zapowiedzi CALEJ rodziny (Krol wzywa Rechotniki, Kokoniara - mlode); wlasne zapowiedzi moba najpierw
const _ZJ = JSON.parse(fs.readFileSync(E.ZAPOWIEDZI || '/b/mm/zapowiedzi.json', 'utf8'))
const ZAP_MOBA = _ZJ[MOB] || []
const ZAP = [...ZAP_MOBA, ...Object.entries(_ZJ).filter(([k]) => k !== MOB).flatMap(([, v]) => v)]
const yml = fs.readFileSync(E.MOBS_YML || '/b/mm/Bagna_mobs.yml', 'utf8')
const blok = (yml.split(/\n(?=[A-Z0-9_]+:\n)/).find(b => b.startsWith(MOB + ':')) || '')
const TYP = E.TYP || ((blok.match(/\n  Type: ([A-Z_]+)/) || [])[1] || '').toLowerCase()
if (!TYP) { console.error('brak moba', MOB, 'w', E.MOBS_YML); process.exit(2) }
const nick = (TRYB === 'uwazny' ? 'Uw' : 'Nu') + (Date.now() % 10000)
const sleep = ms => new Promise(r => setTimeout(r, ms))
const OBSZAR = `x=${AX - 14},y=${AY - 3},z=${AZ - 14},dx=28,dy=10,dz=28`
const bot = mineflayer.createBot({ host: HOST, port: 25565, username: nick, auth: 'offline' })
bot.loadPlugin(pathfinder)
// Nexo wysyla pack w fazie CONFIGURATION i czeka na status (pamiec eval-botem-mineflayer) - bez tego bot wisi po UUID
bot._client.on('add_resource_pack', p => { bot._client.write('resource_pack_receive', { uuid: p.uuid, result: 3 }); bot._client.write('resource_pack_receive', { uuid: p.uuid, result: 0 }) })
bot.on('kicked', r => { console.log('KICK', JSON.stringify(r)); process.exit(3) }); bot.on('error', e => console.log('BLAD', e.message))
let reakcja = 0, reakcje = 0, zgon = false, hpStrata = 0, hpPrev = 20, slyszane = {}, trafienia = [], ostatni = '', tStart = 0
bot.on('health', () => { if (bot.health < hpPrev) { hpStrata += hpPrev - bot.health; if (tStart) { const m = bot.nearestEntity(e => e.name === TYP); trafienia.push(`${((Date.now() - tStart) / 1000).toFixed(1)}s -${(hpPrev - bot.health).toFixed(1)} po:${ostatni} odl:${m ? m.position.distanceTo(bot.entity.position).toFixed(1) : '?'} ctl:${['back','left','right','forward'].filter(k => bot.controlState[k]).join('+')}`) } } hpPrev = bot.health })
bot.on('death', () => { if (tStart) zgon = true })
// CMI odradza na spawnie world1 (149/99/197 = powietrze na plaskim klonie) -> natychmiast z powrotem na arene, zanim spadnie
let poZgonie = false
bot.on('death', () => { poZgonie = true })
bot.on('spawn', () => { if (!poZgonie) return; poZgonie = false
  for (const ms of [50, 400, 1200]) setTimeout(() => rcon(`minecraft:tp ${nick} ${START[0]} ${START[1]} ${START[2]}`).catch(() => {}), ms) })
// w wodzie (basen Pijawek) plyn w gore - inaczej bot tonie goniac odrzuconego moba (pomiar 03.10: 'drowned')
bot.on('physicsTick', () => { if (bot.entity && bot.entity.isInWater) bot.setControlState('jump', true) })   // mineflayer odradza sam (respawn: true)
// unik w bok w strone SRODKA areny (losowy bok wbijal bota w mur/filar - Kosciej v2: trafienia przy scianie)
function bokKuSrodkowi() {
  const y = bot.entity.yaw, p = bot.entity.position
  const lewo = { x: -Math.cos(y), z: Math.sin(y) }
  const dl = (p.x + 3 * lewo.x - AX) ** 2 + (p.z + 3 * lewo.z - AZ) ** 2, dp = (p.x - 3 * lewo.x - AX) ** 2 + (p.z - 3 * lewo.z - AZ) ** 2
  return dl < dp ? 'left' : 'right'
}
async function unik(kierunek, ms) {
  // nowa zapowiedz w trakcie uniku (roj) przedluza unik, jesli od poczatku poprzedniego minelo >= 400 ms
  if (Date.now() < reakcja && Date.now() - (globalThis.startUniku || 0) < 400) return
  globalThis.startUniku = Date.now(); reakcja = Date.now() + ms; reakcje++
  bot.clearControlStates(); bot.setControlState('sprint', kierunek !== 'back')
  bot.setControlState(kierunek, true)
  if (kierunek === 'back') bot.setControlState(bokKuSrodkowi(), true)
  await sleep(ms); bot.clearControlStates()
}
bot.on('soundEffectHeard', (nazwa, poz, vol, pitch) => {
  const n = String(nazwa).replace(/^minecraft:/, '')
  slyszane[n] = (slyszane[n] || 0) + 1
  if (!/step|ambient|krok/.test(n)) ostatni = `${n}@${((Date.now() - tStart) / 1000).toFixed(1)}`
  if (TRYB !== 'uwazny') return
  for (const z of ZAP) {
    if (n !== z.dzwiek || Math.abs(pitch - z.pitch) > 0.04) continue
    if (z.reakcja === 'bok') unik(bokKuSrodkowi(), Math.min(z.ms, 900))
    else if (z.reakcja === 'tyl') unik('back', Math.min(z.ms, 2600))
    else if (z.reakcja === 'naprzod') unik('forward', 500)
    // 'bij' (kanal do przerwania): uwazny nie odchodzi, bije dalej
  }
})
let ostAtak = 0, ostPoz = null, utkniecie = 0
async function walcz(mob) {
  const cel = mob.position.offset(0, (mob.height || 1) * 0.6, 0)
  bot.lookAt(cel, true).catch(() => {})
  const d = mob.position.distanceTo(bot.entity.position)
  if (d > 2.6) {
    bot.setControlState('forward', true); bot.setControlState('sprint', d > 5)
    if (ostPoz && ostPoz.distanceTo(bot.entity.position) < 0.05) { if (++utkniecie > 3) { bot.setControlState('jump', true); utkniecie = 0 } } else bot.setControlState('jump', false)
    ostPoz = bot.entity.position.clone()
  } else { bot.setControlState('forward', false); bot.setControlState('sprint', false); bot.setControlState('jump', false) }
  if (d <= 3.2 && Date.now() - ostAtak >= 650) { bot.attack(mob); ostAtak = Date.now() }   // miecz: pelny cios co ~0.625 s
}
async function zywy() {
  const r = await rcon(`minecraft:execute if entity @e[type=minecraft:${TYP},${OBSZAR}]`)
  if (r === 'TIMEOUT' || r === '') return 1   // brak odpowiedzi != brak moba
  const m = r.match(/count: (\d+)/i); return m ? +m[1] : 0
}
bot.once('spawn', async () => {
  const wyn = []
  bot.pathfinder.setMovements(new Movements(bot))
  // po zgonie odradzal sie wysoko (spawn swiata z world1) i ginal od upadku w nastepnej rundzie (pomiar 03.10)
  await rcon(`minecraft:spawnpoint ${nick} ${START[0]} ${START[1]} ${START[2]}`)
  await sleep(2000)
  console.log('wszedl', bot.game.dimension, JSON.stringify(bot.entity.position))
  for (let i = 0; i < POWT; i++) {
    // po zgonie w poprzedniej rundzie: odrodzenie i odczekanie (pomiar 03.10: druga runda 'ginela' po 1.2 s)
    tStart = 0
    for (let k = 0; k < 40 && !(bot.isAlive && bot.health > 0); k++) await sleep(250)
    // CMI przenosi odrodzonego na spawn world1 (powietrze na plaskim klonie) - pilnuj 6 s, az bot stoi na arenie
    for (let k = 0; k < 20; k++) {
      if (bot.entity && Math.abs(bot.entity.position.y - START[1]) > 1.5) await rcon(`minecraft:tp ${nick} ${START[0]} ${START[1]} ${START[2]}`)
      await sleep(300)
    }
    await sleep(2500); await rcon(`effect clear ${nick}`)
    await rcon(`minecraft:kill @e[type=!player,${OBSZAR}]`)
    await rcon(`gamemode survival ${nick}`); await rcon(`effect clear ${nick}`)
    await rcon(`minecraft:item replace entity ${nick} weapon.mainhand with minecraft:${E.BRON || 'stone_sword'}`)
    const tpOdp = await rcon(`minecraft:tp ${nick} ${START[0]} ${START[1]} ${START[2]} 0 0`); if (E.DEBUG) console.log("  tp:", tpOdp); await sleep(1200)
    await rcon(`effect give ${nick} instant_health 1 5 true`); await sleep(600)
    hpStrata = 0; hpPrev = bot.health; zgon = false; reakcje = 0; slyszane = {}; trafienia = []; tStart = Date.now()
    await rcon(`mm mobs spawn ${MOB} ${ILE} world,${SPAWN[0]},${SPAWN[1]},${SPAWN[2]}`); await sleep(800)
    const t0 = Date.now(); let zabity = false, widac = 0
    while (Date.now() - t0 < LIMIT && !zgon) {
      // z modelem ME baza jest ukryta - bot widzi hitbox modelu jako encje 'interaction' (03.10); bicie jej trafia moba
      const wArenie = e => Math.abs(e.position.x - AX) < 14 && Math.abs(e.position.z - AZ) < 14
      const mob = bot.nearestEntity(e => e.name === TYP && wArenie(e)) || bot.nearestEntity(e => e.name === 'interaction' && wArenie(e))
      // uwazny najpierw rozbija kokon na sobie (BAGNA_KOKON = niewidzialny zombie przy graczu), jak zrobilby gracz
      const kokon = TRYB === 'uwazny' && TYP !== 'zombie' ? bot.nearestEntity(e => e.name === 'zombie' && e.position.distanceTo(bot.entity.position) < 2.5) : null
      if (kokon) { bot.lookAt(kokon.position.offset(0, 1, 0), true).catch(() => {}); if (Date.now() - ostAtak >= 650) { bot.attack(kokon); ostAtak = Date.now() } await sleep(100); continue }
      if (mob) widac++
      // duch: mineflayer bywa trzyma encje po smierci moba (pomiar 03.10: 60 s bicia w nieruchomy cel) -> co 1 s pytamy serwer
      if (Date.now() - (globalThis.ostSpr || 0) > 1000) { globalThis.ostSpr = Date.now(); if (await zywy() === 0) { zabity = true; break } }
      if (!mob) { const z = await zywy(); if (E.DEBUG) console.log('  brak moba, zywy=', z); if (z === 0) { zabity = true; break } await sleep(300); continue }
      if (Date.now() >= reakcja && Date.now() - t0 >= STOJ) await walcz(mob)
      else if (Date.now() - t0 < STOJ) bot.lookAt(mob.position.offset(0, 1.5, 0), true).catch(() => {})
      if (E.DEBUG && (Date.now() - t0) % 2000 < 110) console.log('  hpmoba', await rcon(`minecraft:data get entity @e[type=minecraft:${TYP},${OBSZAR},limit=1] Health`), await rcon(`minecraft:data get entity @e[type=minecraft:${TYP},${OBSZAR},limit=1] AbsorptionAmount`), 'cel', mob.name, mob.id)
      if (E.DEBUG && (Date.now() - t0) % 2000 < 110) console.log('  dbg', ((Date.now() - t0) / 1000).toFixed(1), 'bot', bot.entity.position.toString(), 'mob', mob.position.toString(), 'odl', mob.position.distanceTo(bot.entity.position).toFixed(1), 'hp', bot.health)
      await sleep(100)
    }
    bot.clearControlStates()
    const r = { mob: MOB, tryb: TRYB, ile: ILE, czas_s: +((Date.now() - t0) / 1000).toFixed(1), hp_strata: +hpStrata.toFixed(1), hp_proc: Math.round(hpStrata * 5), zgon, zabity, reakcje,
      baza_widoczna: widac > 0, trafienia, zapowiedzi_slyszane: ZAP_MOBA.map(z => `${z.dzwiek}:${slyszane[z.dzwiek] || 0}`) }
    wyn.push(r); console.log(JSON.stringify(r)); await sleep(1500)
  }
  console.log('WYNIKI ' + JSON.stringify(wyn)); await rcon(`minecraft:kill @e[type=!player,${OBSZAR}]`); process.exit(0)
})
