'use strict'
// Wpiecie lupu (nie walka): kukla PRZ_TEST_KUKLA (TYLKO klon, 1 HP) z PRAWDZIWA tabela DT_BEST_BOSS_WARDEN.
// Konsola zadaje cios Z BOTA jako sprawca (kredyt zabojstwa = gracz). Lup czytany z ziemi po stronie serwera
// (lore kazdej encji przedmiotu), bez podnoszenia. Sprawdza: 2 nasze przedmioty z bossa, ilvl = poziom gracza +-2,
// rzadkosc z naszych tierow. Walke sprawdzaja evale lochow.
const { rcon, H, opis } = require('/h/evale/wspolne.js')
const PROB = +(process.env.ARG || 6)
module.exports = async function (r) {
  const nick = 'ItmL' + Date.now() % 10000
  const bot = await H.connect(nick); await H.sleep(3000)
  await rcon(`mmocore class select ${nick} RYCERZ`); await rcon(`mmocore admin level set ${nick} main 6`); await rcon('minecraft:time set day'); await rcon('minecraft:kill @e[type=zombie]'); await rcon(`minecraft:effect give ${nick} minecraft:resistance 600 4 true`); await H.sleep(800)
  const wszystko = []; let dwa = 0, zabite = 0
  for (let k = 0; k < PROB; k++) {
    await rcon('minecraft:kill @e[type=item]'); await rcon('minecraft:clear ' + nick); await H.sleep(500)
    const p = bot.entity.position
    await rcon(`mm mobs spawn PRZ_TEST_KUKLA 1 world,${(p.x + 7).toFixed(1)},${p.y.toFixed(1)},${p.z.toFixed(1)}`); await H.sleep(1200)
    const odp = await rcon(`minecraft:execute at ${nick} run damage @e[name=KuklaLupu,distance=..16,limit=1] 100000 minecraft:player_attack by ${nick}`)
    await H.sleep(3500)
    const nasze = []; let naZiemi = 0
    const NN = `@e[type=item,distance=..32,limit=1,sort=nearest]`
    const klucz = async k => { const o = await rcon(`minecraft:execute at ${nick} run data get entity ${NN} Item.components."minecraft:custom_data".${k}`); const m = o.match(/data: "?([^"]*)"?$/); return m ? m[1].trim() : '' }
    for (let n = 0; n < 40; n++) {
      if (!/passed/i.test(await rcon(`minecraft:execute at ${nick} if entity @e[type=item,distance=..32]`))) break
      naZiemi++
      const id = await klucz('MMOITEMS_ITEM_ID')
      if (id.startsWith('PRZ_')) nasze.push({ n: +((await rcon(`minecraft:execute at ${nick} run data get entity ${NN} Item.count`)).replace(/[^0-9]/g, '') || 1), id, rz: await klucz('MMOITEMS_TIER'), ilvl: +(await klucz('MMOITEMS_ITEM_LEVEL')).replace(/[^0-9.]/g, '') })
      await rcon(`minecraft:execute at ${nick} run minecraft:kill ${NN}`)
    }
    const bloki = { length: naZiemi }
    const zyje = /passed/i.test(await rcon(`minecraft:execute at ${nick} if entity @e[name=KuklaLupu,distance=..16]`))
    if (zyje) console.log('kukla przezyla!')
    const wEq = bot.inventory.items().map(opis).filter(o => o.lore.some(l => l.includes('Poziom przedmiotu'))).map(o => o.nazwa)
    if (wEq.length) console.log(`kukla ${k + 1}: bot podniósł ${wEq.length}: ${wEq.join(', ')}`)
    const bazy = nasze.filter(x => !x.id.startsWith('PRZ_SET_')).flatMap(x => Array(x.n || 1).fill(x))
    const trafiony = /Applied/.test(odp); if (trafiony) zabite++
    if (trafiony && bazy.length >= 2) dwa++
    wszystko.push(...bazy)
    console.log(`kukla ${k + 1}: ${odp.slice(0, 50)} | przedmiotow na ziemi ${bloki.length}, naszych ${nasze.length}: ${JSON.stringify(nasze)}`)
  }
  r.assert(zabite >= PROB - 2, `cios gracza zabija kukłę (${zabite}/${PROB})`)
  r.assert(dwa === zabite, 'każda zabita kukła z tabelą bossa daje 2 bazy (części setów osobno)', `${dwa}/${zabite}`)
  r.assert(wszystko.length && wszystko.every(x => x.ilvl >= 4 && x.ilvl <= 8), 'poziom przedmiotu = poziom gracza 6 ±2', JSON.stringify(wszystko.map(x => x.ilvl)))
  r.assert(wszystko.length && wszystko.every(x => /ZWYKLY|MAGICZNY|RZADKI/.test(x.rz)), 'rzadkość z naszych tierów', JSON.stringify(wszystko.map(x => x.rz)))
  bot.quit()
}
