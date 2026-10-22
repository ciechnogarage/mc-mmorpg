'use strict'
// Rozklad rzadkosci i affiksow w grze (od 30.09 rzadkosc losuje modyfikator; mi generate bez -tier).
// Sprawdza: zakres liczby affiksow na rzadkosc, brak duplikatow rodzin, efekt legendarny (✦) tylko u Legendarnych,
// udzialy rzadkosci blisko tabel (tresc.RARITY).
const { opis, rcon, H } = require('/h/evale/wspolne.js')
const N = +(process.env.N || 400)
const BAZY = [['ARMOR', 'PRZ_WOJ_KLATA_3'], ['SWORD', 'PRZ_WOJ_MIECZ_4'], ['RING', 'PRZ_MAG_RING_2'], ['DAGGER', 'PRZ_NIN_SZTYLET_5'], ['BELT', 'PRZ_AKO_BELT_3']]
const ZAKRES = { 'Zwykły': [0, 0], 'Magiczny': [1, 2], 'Rzadki': [3, 4], 'Epicki': [4, 5], 'Legendarny': [5, 6] }
const UDZIAL = { 'Zwykły': 0.60, 'Magiczny': 0.28 }
module.exports = async function (r) {
  const nick = 'ItmR' + Date.now() % 10000
  const bot = await H.connect(nick); await H.sleep(3000)
  await rcon(`mmocore admin level set ${nick} main 10`)
  const st = {}; let dup = 0, legZle = 0, suma = 0
  for (let n = 0; n < N; n += 20) {
    await rcon('minecraft:clear ' + nick); await H.sleep(300)
    for (let i = 0; i < 20; i++) { const [t, id] = BAZY[(n + i) % BAZY.length]; await rcon(`mi generate ${nick} -type:${t} -id:${id} -level:8`) }
    let its = []
    for (let w = 0; w < 40; w++) { await H.sleep(300); its = bot.inventory.items().map(opis).filter(o => o.lore.some(l => l.includes('Rzadkość') || l.includes('Poziom przedmiotu'))); if (its.length >= 20) break }
    for (const o of its) {
      const rz = Object.keys(ZAKRES).find(k => o.lore.some(l => l.includes(k))) || '?'
      const aff = o.lore.filter(l => l.includes('◆')).map(l => l.replace(/[◆]/g, '').replace(/\[T\d\]/, '').trim())
      if (new Set(aff).size !== aff.length) dup++
      const leg = o.lore.some(l => l.includes('✦'))
      if (leg) legZle++
      const k = st[rz] = st[rz] || { n: 0, liczby: {} }
      k.n++; k.liczby[aff.length] = (k.liczby[aff.length] || 0) + 1; suma++
    }
  }
  console.log('ROZKLAD', JSON.stringify(st))
  r.assert(suma >= N * 0.95, `wygenerowano ${suma}/${N}`)
  r.assert(dup === 0, 'brak duplikatów rodzin affiksów', `duplikaty ${dup}`)
  r.assert(legZle === 0, '✦ (umiejętność) nie pojawia się na losowych przedmiotach — tylko unikaty', `niezgodnych ${legZle}`)
  for (const [rz, [a, b]] of Object.entries(ZAKRES)) {
    const k = st[rz]; if (!k) { r.ok(`${rz}: nie wypadł w tej próbie (rzadki; sprawdzany sondą na większej próbie)`); continue }
    const poza = Object.entries(k.liczby).filter(([l]) => +l < a || +l > b).reduce((s, [, v]) => s + v, 0)
    r.assert(poza === 0, `${rz}: liczba affiksów ${a}–${b}`, JSON.stringify(k.liczby))
  }
  for (const [rz, u] of Object.entries(UDZIAL)) {
    const f = (st[rz] ? st[rz].n : 0) / suma
    r.assert(Math.abs(f - u) <= 0.07, `${rz}: udział ${(f * 100).toFixed(1)}% (tabela ${Math.round(u * 100)}%)`)
  }
  bot.quit()
}
