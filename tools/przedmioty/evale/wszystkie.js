'use strict'
// Kazdy szablon PRZ_ (bazy + czesci setow) daje sie zbudowac w grze: mi generate do bota partiami, liczymy co dotarlo.
const { opis, rcon, H } = require('/h/evale/wspolne.js')
const fs = require('fs')
module.exports = async function (r) {
  const kat = JSON.parse(fs.readFileSync('/w/katalog.json', 'utf8'))
  const lista = kat.przedmioty.map(x => [x.typ, x.id, x.nazwa]).concat(kat.sety.flatMap(s => s.czesci.map(c => [c.typ, c.id, c.nazwa])))
  const nick = 'ItmW' + Date.now() % 10000
  const bot = await H.connect(nick); await H.sleep(3000)
  await rcon(`mmocore admin level set ${nick} main 10`)
  const brak = []
  for (let i = 0; i < lista.length; i += 18) {
    const partia = lista.slice(i, i + 18)
    await rcon('minecraft:clear ' + nick); await H.sleep(400)
    for (const [t, id] of partia) await rcon(`mi generate ${nick} -type:${t} -id:${id} -level:10 -tier:${id.startsWith('PRZ_SET_') ? 'ZESTAW' : 'MAGICZNY'}`)
    let its = []
    for (let w = 0; w < 40; w++) { await H.sleep(300); its = bot.inventory.items().map(opis).filter(o => o.id && o.id.startsWith('PRZ_')); if (its.length >= partia.length) break }
    const ids = new Set(its.map(o => o.id))
    for (const [t, id, n] of partia) if (!ids.has(id)) brak.push(`${t}:${id} (${n})`)
  }
  const pierwsza = brak.length, naprawde = []
  for (const w of brak) {
    const [t, id] = w.split(' ')[0].split(':'); const n = w.replace(/^.*?\((.*)\)$/, '$1')
    await rcon('minecraft:clear ' + nick); await H.sleep(300)
    await rcon(`mi generate ${nick} -type:${t} -id:${id} -level:10 -tier:${id.startsWith('PRZ_SET_') ? 'ZESTAW' : 'MAGICZNY'}`)
    let ok = false
    for (let k = 0; k < 20 && !ok; k++) { await H.sleep(300); ok = bot.inventory.items().some(i => opis(i).id === id) }
    if (!ok) naprawde.push(w)
  }
  console.log('pierwsza runda brakow:', pierwsza, 'po ponowieniu:', naprawde.length)
  brak.length = 0; brak.push(...naprawde)
  r.assert(brak.length === 0, `wszystkie ${lista.length} szablony budują się w grze`, brak.join(', ').slice(0, 1500))
  bot.quit()
}
