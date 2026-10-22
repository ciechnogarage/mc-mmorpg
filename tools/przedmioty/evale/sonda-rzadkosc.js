'use strict'
// Sonda mechanizmu "rzadkosc jako modyfikator": rozklad rzadkosci, liczba affiksow per rzadkosc, efekt legendarny.
const { opis, rcon, H } = require('/h/evale/wspolne.js')
const fs = require('fs')
const [TYP, ID] = (process.env.ARG || 'SWORD:TEST_RZ_MIECZ').split(':')
module.exports = async function (r) {
  const N = +(process.env.N || 200), nick = 'ItmQ' + Date.now() % 10000
  const bot = await H.connect(nick); await H.sleep(3000)
  await rcon(`mmocore admin level set ${nick} main 10`)
  const staty = {}
  for (let n = 0; n < N; n += 20) {
    await rcon('minecraft:clear ' + nick); await H.sleep(300)
    for (let i = 0; i < 20; i++) await rcon(`mi generate ${nick} -type:${TYP} -id:${ID} -level:8`)
    let its = []
    for (let w = 0; w < 40; w++) { await H.sleep(300); its = bot.inventory.items().map(opis).filter(o => o.lore.some(l => l.includes('Rzadkość') || l.includes('Poziom'))); if (its.length >= 20) break }
    for (const o of its) {
      const rz = ((o.lore.find(l => l.includes('Rzadkość')) || 'Rzadkość: brak').replace(/.*Rzadkość:\s*/, '')).trim()
      const aff = o.lore.filter(l => l.includes('◆')).length, leg = o.lore.some(l => l.includes('✦'))
      const k = staty[rz] = staty[rz] || { n: 0, aff: {}, leg: 0, przyklad: o.nazwa }
      k.n++; k.aff[aff] = (k.aff[aff] || 0) + 1; if (leg) k.leg++
    }
  }
  console.log('ROZKLAD', JSON.stringify(staty))
  fs.writeFileSync('/w/sonda-rzadkosc.json', JSON.stringify(staty, null, 1))
  bot.quit()
}
