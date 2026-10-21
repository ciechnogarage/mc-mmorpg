'use strict'
// Pomiar przeciwnika na arenie: bot NIEUWAZNY (tylko bije) vs UWAZNY (reaguje na dzwiek zapowiedzi).
// env: MOB=GAJ_WILK, TRYB=nieuwazny|uwazny, POWT=3. Wynik: czas walki, utracone HP, smierci -> JSON na koncu.
const mineflayer = require('/g/node_modules/mineflayer'); const { pathfinder, Movements } = require('/g/node_modules/mineflayer-pathfinder')
const pvp = require('/g/node_modules/mineflayer-pvp').plugin; const { rcon } = require('/h/rcon.js')
const MOB = process.env.MOB, TRYB = process.env.TRYB || 'nieuwazny', POWT = +(process.env.POWT || 3)
const nick = (TRYB === 'uwazny' ? 'Uw' : 'Nu') + (Date.now() % 10000)
const sleep = ms => new Promise(r => setTimeout(r, ms))
const bot = mineflayer.createBot({ host: process.env.MC_HOST || 'klon-kuznia', port: 25565, username: nick, auth: 'offline' })
bot.loadPlugin(pathfinder); bot.loadPlugin(pvp)
let reakcja = 0, reakcje = 0, zgon = false, hpStrata = 0, hpPrev = 20
bot.on('health', () => { if (bot.health < hpPrev) hpStrata += hpPrev - bot.health; hpPrev = bot.health })
bot.on('death', () => { zgon = true; setTimeout(() => { try { bot.respawn() } catch {} }, 300) })
async function unik(kierunek, ms) {
  if (Date.now() < reakcja) return; reakcja = Date.now() + ms; reakcje++
  bot.pvp.forceStop(); bot.clearControlStates(); bot.setControlState('sprint', true); bot.setControlState(kierunek, true)
  await sleep(ms); bot.clearControlStates()
}
bot.on('soundEffectHeard', (nazwa, poz) => {
  if (TRYB !== 'uwazny') return
  const n = String(nazwa)
  if (/wolf\.growl/.test(n)) unik(Math.random() < 0.5 ? 'left' : 'right', 700)
  else if (/crossbow\.loading_start/.test(n)) unik(Math.random() < 0.5 ? 'left' : 'right', 900)
  else if (/hanging_roots\.break/.test(n)) unik('back', 1100)
  else if (/rooted_dirt\.break/.test(n) && poz && poz.distanceTo(bot.entity.position) < 3) {
    bot.lookAt(poz, true).then(() => unik('back', 900))
  }
})
bot.once('spawn', async () => {
  const wyn = []
  bot.pathfinder.setMovements(new Movements(bot))
  await sleep(2000)
  for (let i = 0; i < POWT; i++) {
    await rcon(`minecraft:kill @e[type=!player,x=286,y=-62,z=286,dx=28,dy=10,dz=28]`)
    await rcon(`gamemode survival ${nick}`); await rcon(`effect clear ${nick}`)
    await rcon(`minecraft:item replace entity ${nick} weapon.mainhand with minecraft:stone_sword`)
    await rcon(`minecraft:tp ${nick} 300.5 -59 293.5 0 0`); await sleep(1200)
    await rcon(`effect give ${nick} instant_health 1 5 true`); await sleep(600)
    hpStrata = 0; hpPrev = bot.health; zgon = false; reakcje = 0
    await rcon(`mm mobs spawn ${MOB} 1 world,300.5,-59,306.5`); await sleep(800)
    const t0 = Date.now(); let zabity = false
    while (Date.now() - t0 < 60000 && !zgon) {
      const mob = bot.nearestEntity(e => ['wolf', 'zombie', 'skeleton'].includes(e.name) && Math.abs(e.position.x - 300.5) < 14 && Math.abs(e.position.z - 300.5) < 14)
      if (!mob) { zabity = true; break }
      if (Date.now() >= reakcja && bot.pvp.target !== mob) bot.pvp.attack(mob)
      if (process.env.DEBUG && (Date.now() - t0) % 2000 < 160) console.log('  dbg', ((Date.now()-t0)/1000).toFixed(1), mob.name, mob.type, 'odl', mob.position.distanceTo(bot.entity.position).toFixed(1), 'bot', bot.entity.position.floored().toString(), 'mob', mob.position.floored().toString(), 'cel', bot.pvp.target && bot.pvp.target.id === mob.id, 'hp', bot.health)
      await sleep(150)
    }
    bot.pvp.forceStop()
    const r = { mob: MOB, tryb: TRYB, czas_s: +((Date.now() - t0) / 1000).toFixed(1), hp_strata: +hpStrata.toFixed(1), zgon, zabity, reakcje }
    wyn.push(r); console.log(JSON.stringify(r)); await sleep(1500)
  }
  console.log('WYNIKI ' + JSON.stringify(wyn)); await rcon(`minecraft:kill @e[type=!player,x=286,y=-62,z=286,dx=28,dy=10,dz=28]`); process.exit(0)
})
