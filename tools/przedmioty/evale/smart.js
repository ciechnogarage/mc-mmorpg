'use strict'
// Lup pod klase: ninja (AGENT) zabija 20 kukiel z tabela bossa; udzial baz ninji w lupie (oczekiwane ~0,75 + 0,25*0,25 ≈ 0,81).
const { rcon, ph, H } = require('/h/evale/wspolne.js')
module.exports = async function (r) {
  const nick = 'ItmS' + Date.now() % 10000
  const bot = await H.connect(nick); await H.sleep(3000)
  await rcon(`mmocore class select ${nick} AGENT`); await rcon(`mmocore admin level set ${nick} main 6`); await H.sleep(4000)
  const arch = (await ph(nick, 'coretools_var_player_klasa_archetyp'))[0]
  r.assert(arch === 'nin', 'placeholder archetypu dla Agenta = nin', arch)
  await rcon('minecraft:time set day'); await rcon(`minecraft:effect give ${nick} minecraft:resistance 600 4 true`)
  const NN = `@e[type=item,distance=..32,limit=1,sort=nearest]`
  const ids = []
  for (let k = 0; k < 20; k++) {
    await rcon('minecraft:kill @e[type=item]'); await H.sleep(300)
    const p = bot.entity.position
    await rcon(`mm mobs spawn PRZ_TEST_KUKLA 1 world,${(p.x + 7).toFixed(1)},${p.y.toFixed(1)},${p.z.toFixed(1)}`); await H.sleep(1000)
    await rcon(`minecraft:execute at ${nick} run damage @e[name=KuklaLupu,distance=..16,limit=1] 100000 minecraft:player_attack by ${nick}`)
    await H.sleep(3000)
    for (let n = 0; n < 30; n++) {
      if (!/passed/i.test(await rcon(`minecraft:execute at ${nick} if entity @e[type=item,distance=..32]`))) break
      const o = await rcon(`minecraft:execute at ${nick} run data get entity ${NN} Item.components."minecraft:custom_data".MMOITEMS_ITEM_ID`)
      const m = o.match(/data: "([^"]*)"/); if (m && /^PRZ_(WOJ|NIN|MAG|AKO)_/.test(m[1])) ids.push(m[1])
      await rcon(`minecraft:execute at ${nick} run minecraft:kill ${NN}`)
    }
  }
  const nin = ids.filter(x => x.startsWith('PRZ_NIN_')).length, udz = nin / Math.max(1, ids.length)
  console.log('SMART', ids.length, 'baz, ninja', nin)
  r.assert(ids.length >= 30, `z 20 kukieł wypadło ${ids.length} baz`)
  r.assert(udz >= 0.65, `udział baz ninji ${(udz * 100).toFixed(0)}% (oczekiwane ~80%, bez łupu pod klasę 25%)`)
  bot.quit()
}
