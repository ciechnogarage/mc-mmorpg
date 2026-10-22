'use strict'
// POLIGON KLAS v3 (akolita z Duszami: przed walką 3 sługi, w walce Wezwanie + Małe Leczenie z paska). Na bazie v2.
// POLIGON KLAS v2 (bot rzuca umiejętności z paska MMOCore, mag strzela różdżką z dystansu). Na bazie v1.
// POLIGON KLAS v1 (etap 1 przebudowy klas, 2026-09-30). Tylko klon (CC_TEST_WEJSCIE=1).
// Arena i przeciwnicy z sesji Lochu 001 (forge/tools/przeciwnicy: arena_v1.nbt, Przeciwnicy_Gaj*.yml),
// uniki bota uważnego jak w ich arena_bot.js. Bot dostaje PRAWDZIWĄ postać (/cc testpostac) i bije bronią
// startową klasy. Ograniczenia v1: bez umiejętności (to wciąż demo), tylko walka wręcz — łucznicy i magowie
// orientacyjnie. env: PARY=rasa:arch,...  MOBY=GAJ_WILK,...  TRYBY=nieuwazny,uwazny  POWT=1  POZIOM=1
// Montowanie: /e = harness kreatora (resource pack), /g = forge/gracz (mineflayer-pvp), /s = katalog z rcon.js.
const mineflayer = require('/g/node_modules/mineflayer'); const { pathfinder, Movements } = require('/g/node_modules/mineflayer-pathfinder')
const pvp = require('/g/node_modules/mineflayer-pvp').plugin; const { attach } = require('/e/lib/resourcepack.js'); const { rcon } = require('/s/rcon.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const num = s => { const m = /(-?[\d.]+)[a-z]?\s*$/.exec(String(s).trim()); return m ? +m[1] : NaN }
const PARY = (process.env.PARY || 'czlowiek:wojownik').split(','), MOBY = (process.env.MOBY || 'GAJ_WILK,GAJ_KORZENIAK,GAJ_LUCZNIK').split(',')
const TRYBY = (process.env.TRYBY || 'nieuwazny,uwazny').split(','), POWT = +(process.env.POWT || 1), POZIOM = +(process.env.POZIOM || 5)
// Skille bojowe szkieletów dostępne do 5. poz. (HUMAN_SHIELD pomijany — cel to sojusznik).
const SKILLE = { wojownik: ['DEEP_WOUND', 'WEAKEN', 'EMPOWERED_ATTACK'], ninja: ['DEEP_WOUND', 'FURTIVE_STRIKE', 'WEAKEN', 'EMPOWERED_ATTACK'],
  mag: ['FIRE_STORM', 'FIREBALL', 'POWER_MARK'], akolita: ['WEAKEN', 'MINOR_HEALINGS'] }
// Przeciwnik po UUID pobranym z serwera (selektor po nazwie moba) — słudzy akolity to też szkielety i wilki (v3).
const NAZWY_WROGOW = { GAJ_WILK: 'Wilk Gajowy', GAJ_KORZENIAK: 'Korzeniak', GAJ_LUCZNIK: 'Cierniowy Łucznik' }
const uuidZ = s => { const m = /\[I;\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+)\]/.exec(s); if (!m) return null
  const h = m.slice(1).map(x => (Number(x) >>> 0).toString(16).padStart(8, '0')).join(''); return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}` }
let WROG = null
const wrog = e => WROG && e.uuid === WROG
const F = bot => bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 })
const CX = 300.5, CZ = 300.5, Y = -60
const czysc = () => rcon(`minecraft:kill @e[type=!player,type=!armor_stand,x=286,y=-63,z=286,dx=28,dy=12,dz=28]`)

async function przygotujArene () {
  await rcon('forceload add 280 280 320 320')
  const o = await rcon('execute in minecraft:overworld run place template przeciwnicy:arena_v1 286 -61 286')
  console.log('arena:', o.trim())
}

async function klasa (rasa, arch, idx) {
  const nick = ('A' + idx + rasa.slice(0, 3) + arch.slice(0, 3)).slice(0, 16)
  const bot = mineflayer.createBot({ host: process.env.MC_HOST, username: nick, auth: 'offline', version: '1.21.11' })
  attach(bot._client); bot.loadPlugin(pathfinder); bot.loadPlugin(pvp)
  await new Promise((res, rej) => { bot.once('spawn', res); bot.once('kicked', r => rej(new Error('kick ' + JSON.stringify(r)))) })
  await sleep(5000)
  await rcon(`cc testpostac ${nick} ${rasa} ${arch}`); await sleep(12000)
  await rcon(`mmocore admin level set ${nick} main ${POZIOM}`); await sleep(1500)
  const info = {
    rasa, arch, klasa: (await rcon(`papi parse ${nick} %mmocore_class_id%`)).trim(),
    archetyp: (await rcon(`papi parse ${nick} %coretools_var_player_klasa_archetyp%`)).trim(),
    hp_max: num(await rcon(`attribute ${nick} minecraft:max_health get`)),
    pancerz: num(await rcon(`attribute ${nick} minecraft:armor get`)),
    obrona: num(await rcon(`papi parse ${nick} %mythiclib_stat_defense%`)),
  }
  let bron = null
  for (let s = 0; s < 9; s++) { const it = bot.inventory.slots[36 + s]; if (it && !/book|bundle|arrow|writable/.test(it.name)) { bot.setQuickBarSlot(s); bron = it.name; break } }
  info.bron = bron
  // akolita: pasek ustawia się sam (Wezwanie, Małe Leczenie, Żniwo) — bez ręcznego przypisywania; Żniwo nie jest rzucane
  const skille = arch === 'akolita' ? ['AKO_WEZWANIE', 'MINOR_HEALINGS'] : SKILLE[arch]
  if (arch !== 'akolita') for (let i = 0; i < skille.length; i++) await rcon(`mmocore admin slot bind ${nick} ${i + 1} ${skille[i]}`)
  info.skille = skille; console.log('KLASA ' + JSON.stringify(info))
  const bronSlot = bot.quickBarSlot
  // Skill k (0..) jest pod k-tym klawiszem z pominięciem trzymanego slotu (use-lowest-keybinds, zmierzone 30.09).
  const klawisz = k => { const wolne = [...Array(9).keys()].filter(s => s !== bronSlot); return wolne[k] }
  let rzuty = 0, kolej = 0, ostatni = 0
  const rzuc = async celE => {
    if (Date.now() - ostatni < 2500) return; ostatni = Date.now()
    await bot.lookAt(celE.position.offset(0, 0.5, 0), true)
    F(bot); await sleep(250); bot.setQuickBarSlot(klawisz(kolej % skille.length)); kolej++; rzuty++; await sleep(250); F(bot); await sleep(150)
    if (bot.quickBarSlot !== bronSlot) bot.setQuickBarSlot(bronSlot)
  }
  bot.pathfinder.setMovements(new Movements(bot))

  let reakcja = 0, zgon = false, strata = 0, prev = bot.health, TRYB = 'nieuwazny'
  bot.on('health', () => { if (bot.health < prev) strata += prev - bot.health; prev = bot.health })
  bot.on('death', () => { zgon = true; setTimeout(() => { try { bot.respawn() } catch {} }, 300) })
  const unik = async (k, ms) => { if (Date.now() < reakcja) return; reakcja = Date.now() + ms; bot.pvp.forceStop(); bot.clearControlStates(); bot.setControlState('sprint', true); bot.setControlState(k, true); await sleep(ms); bot.clearControlStates() }
  bot.on('soundEffectHeard', (n, poz) => {
    if (TRYB !== 'uwazny') return; n = String(n)
    if (/wolf\.growl/.test(n)) unik(Math.random() < 0.5 ? 'left' : 'right', 700)
    else if (/crossbow\.loading_start/.test(n)) unik(Math.random() < 0.5 ? 'left' : 'right', 900)
    else if (/hanging_roots\.break/.test(n)) unik('back', 1100)
    else if (/rooted_dirt\.break/.test(n) && poz && poz.distanceTo(bot.entity.position) < 3) bot.lookAt(poz, true).then(() => unik('back', 900))
  })

  const wyniki = []
  for (const mob of MOBY) for (const tryb of TRYBY) for (let i = 0; i < POWT; i++) {
    TRYB = tryb; await czysc()
    await rcon(`minecraft:tp ${nick} ${CX} ${Y + 1} ${CZ - 7} 0 0`); await sleep(1200)
    await rcon(`effect clear ${nick}`); await rcon(`mmocore admin resource-health set ${nick} 1000`); await sleep(800)
    if (arch === 'akolita') {  // Dusze na start walki i 3 sługi (limit 3), jak gracz przed starciem
      await rcon(`mmocore admin resource-mana set ${nick} 10`)
      for (let s = 0; s < 3; s++) { F(bot); await sleep(250); bot.setQuickBarSlot(klawisz(0)); await sleep(250); F(bot); await sleep(200); bot.setQuickBarSlot(bronSlot); await sleep(3300) }
    }
    strata = 0; prev = bot.health; zgon = false; rzuty = 0
    await rcon(`mm mobs spawn ${mob} 1 world,${CX},${Y + 1},${CZ + 6}`); await sleep(800)
    WROG = uuidZ(await rcon(`data get entity @e[name="${NAZWY_WROGOW[mob]}",limit=1] UUID`)); if (!WROG) console.log('BRAK UUID wroga', mob)
    const t0 = Date.now(); let zabity = false
    while (Date.now() - t0 < 45000 && !zgon) {
      const cel = bot.nearestEntity(e => e.type !== 'player' && wrog(e) && Math.abs(e.position.x - CX) < 14 && Math.abs(e.position.z - CZ) < 14)
      if (!cel) { const r = await rcon(`execute if entity @e[name="${NAZWY_WROGOW[mob]}"]`); if (!/passed/.test(r)) { zabity = true; break } await sleep(300); continue }
      if (Date.now() >= reakcja) {
        if (arch === 'mag') {
          bot.pvp.forceStop(); const d = cel.position.distanceTo(bot.entity.position)
          bot.clearControlStates(); if (d < 4) bot.setControlState('back', true); else if (d > 7) bot.setControlState('forward', true)
          await bot.lookAt(cel.position.offset(0, 0.5, 0), true); bot.swingArm('right')
        } else if (bot.pvp.target !== cel) bot.pvp.attack(cel)
        if (cel.position.distanceTo(bot.entity.position) < 8) await rzuc(cel)
      }
      await sleep(arch === 'mag' ? 700 : 150)
    }
    bot.pvp.forceStop(); bot.clearControlStates()
    const r = { klasa: info.klasa, mob, tryb, czas_s: +((Date.now() - t0) / 1000).toFixed(1), hp_strata: +strata.toFixed(1),
      proc_hp: +(100 * strata / info.hp_max).toFixed(0), zgon, zabity, rzuty }
    wyniki.push(r); console.log('WALKA ' + JSON.stringify(r)); await sleep(1500)
  }
  await czysc(); bot.quit(); await sleep(1500)
  return { info, wyniki }
}

;(async () => {
  await przygotujArene()
  const wszystko = []
  for (let i = 0; i < PARY.length; i++) {
    const [rasa, arch] = PARY[i].split(':')
    try { wszystko.push(await klasa(rasa, arch, i)) } catch (e) { console.log('BLAD ' + PARY[i] + ' ' + e.message) }
  }
  console.log('WYNIKI ' + JSON.stringify(wszystko)); process.exit(0)
})().catch(e => { console.error('BLAD', e); process.exit(2) })
