'use strict'
// Zrzut tooltipow (surowe komponenty z pakietu): po jednym przedmiocie kazdej rzadkosci + czesc setu + unikat + ozdoba.
const { opis, rcon, H } = require('/h/evale/wspolne.js')
const fs = require('fs')
module.exports = async function (r) {
  const nick = 'ItmT' + Date.now() % 10000
  const bot = await H.connect(nick); await H.sleep(3000)
  await rcon(`mmocore admin level set ${nick} main 10`)
  const zebrane = {}, wynik = []
  const RZ = ['Zwykły', 'Magiczny', 'Rzadki', 'Epicki', 'Legendarny']
  for (let proba = 0; proba < 40 && RZ.some(k => !zebrane[k]); proba++) {
    await rcon('minecraft:clear ' + nick); await H.sleep(300)
    for (let i = 0; i < 20; i++) await rcon(`mi generate ${nick} -type:ARMOR -id:PRZ_WOJ_HELM_3 -level:8`)
    await H.sleep(2500)
    for (const it of bot.inventory.items()) {
      const o = opis(it); const k = RZ.find(x => o.lore[0] && o.lore[0].includes(x))
      if (k && !zebrane[k]) { zebrane[k] = true; wynik.push({ id: 'PRZ_WOJ_HELM_3', poz: 8, tier: k, o, surowe: it.components }) }
    }
  }
  for (const [typ, id, n] of [['ARMOR', 'PRZ_SET_CALUN_NOCNEGO_LISA_KLATA', '❖ Kurta'], ['SWORD', 'PRZ_UNI_KLINGA_TYSIACA_BLIZN', 'Klinga'], ['RING', 'PRZ_MAG_RING_3', 'Sygnet']]) {
    await rcon('minecraft:clear ' + nick); await H.sleep(300)
    await rcon(id.startsWith('PRZ_MAG') ? `mi generate ${nick} -type:${typ} -id:${id} -level:6` : `mi give ${typ} ${id} ${nick} 1`); await H.sleep(1500)
    const it = bot.inventory.items().find(i => opis(i).nazwa.includes(n))
    if (it) wynik.push({ id, poz: 6, tier: id.includes('SET') ? 'Zestaw' : id.includes('UNI') ? 'Unikatowy' : '?', o: opis(it), surowe: it.components })
  }
  fs.writeFileSync('/w/tooltipy.json', JSON.stringify(wynik, null, 1))
  r.assert(wynik.length >= 7, `zebrano ${wynik.length} tooltipów`, wynik.map(x => x.tier).join(', '))
  bot.quit()
}
