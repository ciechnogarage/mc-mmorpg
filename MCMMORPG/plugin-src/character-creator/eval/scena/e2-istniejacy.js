'use strict'
// E2: gracz z postacia. PPM w kreatorze pyta (Nie = zostaje, Tak = wraca do wyboru), Spacja = siatka
// 16 klas i skok do klasy, druga postac, potem DWIE zmiany postaci w jednej sesji (przycisk
// "Zmien postac" z menu Q = konsola `cc wybor <gracz>`, dokladnie jak hub_postac).
const s = require('./lib/scena')
const { h, sleep } = s
const ARCH = /^(Human|Warrior|Mage|Acolyte|Ninja|Rogue|Paladin)\b/i
async function utworz(bot, name, r, etyk) {
  const d = await s.waitDialog(bot, /Nowa postać/)
  r.assert(!!d, `${etyk}: ksiega "Nowa postac"`)
  const imie = (s.inputs(d).find(x => x.key === 'imie') || {}).initial
  await s.clickWith(bot, /Stwórz/, { imie, kolor: 'niebieski' })
  let st
  for (let t = 0; t < 60; t++) { await sleep(500); st = await s.stan(name); if (st.gm === '0' && /149\.5d.*197\.5d/.test(st.pos || '')) break }
  r.assert(st.gm === '0' && /149\.5d.*197\.5d/.test(st.pos || ''), `${etyk}: wyladowal na starcie`, JSON.stringify(st))
  await sleep(4000)
  return imie
}
;(async () => {
  const r = new h.Results('E2 istniejacy')
  const name = 'NapE2_' + (process.env.SUF || Date.now() % 10000)
  let bot
  try {
    bot = await s.join(name)
    await s.waitScene(bot)
    await s.lpm(bot)
    const imie1 = await utworz(bot, name, r, 'postac 1')
    // --- zmiana postaci -> scena wyboru -> ksiega postaci -> + Nowa postac
    await s.rcon(`cc wybor ${name}`)
    r.assert(await s.waitScene(bot), 'scena wyboru po "Zmien postac"')
    await s.ppm(bot)
    let d = await s.waitDialog(bot, /Księga postaci/)
    r.assert(!!d, 'PPM w wyborze: Ksiega postaci')
    await h.clickDialog(bot, /Nowa postać/)
    r.assert(await s.waitScene(bot), 'kreator po "+ Nowa postac"')
    // --- PPM w kreatorze u gracza z postacia: pytanie
    await s.press(bot, 'D', 2500) // ork
    await s.ppm(bot)
    d = await s.waitDialog(bot, /Przerwać tworzenie/)
    r.assert(!!d, 'PPM w kreatorze: pytanie "Przerwac tworzenie postaci?"')
    await h.clickDialog(bot, /Nie, tworzę dalej/)
    await sleep(800)
    r.assert(!!s.shield(bot), '"Nie": dalej w kreatorze')
    // --- Spacja: siatka, skok do Lisza (nieumarly mag)
    await s.press(bot, 'SPACJA', 600)
    d = await s.waitDialog(bot, /Wszystkie klasy/)
    const przyciski = h.dialogButtons(d).map(b => b.label)
    r.assert(!!d && przyciski.length === 21, 'Spacja: siatka 4 naglowki ras + 16 klas + Wroc', String(przyciski.length))
    await h.clickDialog(bot, /^Lisz$/)
    await sleep(2500)
    await s.lpm(bot)
    d = await s.waitDialog(bot, /Nowa postać/)
    r.assert(!!d && /Lisz/.test(h.dialogText(d)) && /Nieumarły/.test(h.dialogText(d)), 'skok z siatki: ksiega pokazuje Lisza (nieumarly mag)', d ? h.dialogText(d).split('\n')[1] : '')
    // "Wroc" z ksiegi: wybor zostaje
    await s.clickWith(bot, /Wróć/, {})
    await s.lpm(bot)
    d = await s.waitDialog(bot, /Nowa postać/)
    r.assert(!!d && /Lisz/.test(h.dialogText(d)), 'Wroc z ksiegi zachowuje rase i klase')
    // wulgarne imie
    await s.clickWith(bot, /Stwórz/, { imie: 'Kuuurw4', kolor: 'szary' })
    d = await s.waitDialog(bot, /niedozwolone/)
    r.assert(!!d, 'wulgarne imie: odmowa "niedozwolone"')
    // losuj
    const przed = (s.inputs(bot.dialog).find(x => x.key === 'imie') || {}).initial
    await s.clickWith(bot, /Losuj/, { imie: 'Kuuurw4', kolor: 'szary' })
    d = await s.waitDialog(bot, /Nowa postać/)
    const po = (s.inputs(d).find(x => x.key === 'imie') || {}).initial
    r.assert(po && po !== 'Kuuurw4' && po.length >= 3, 'Losuj imie podmienia imie', `${przed} -> ${po}`)
    await s.clickWith(bot, /Wróć/, {})
    // PPM -> Tak: wraca do wyboru postaci
    await s.ppm(bot)
    d = await s.waitDialog(bot, /Przerwać tworzenie/)
    await h.clickDialog(bot, /Tak, przerwij/)
    await sleep(3000)
    r.assert(!!s.shield(bot), '"Tak": scena wyboru postaci (dalej w scenie)')
    await s.ppm(bot)
    d = await s.waitDialog(bot, /Księga postaci/)
    r.assert(!!d, 'po przerwaniu: wybor postaci z ksiega')
    await h.clickDialog(bot, /Nowa postać/)
    await s.waitScene(bot)
    await s.press(bot, 'D', 2500); await s.press(bot, 'D', 2500) // elf
    await s.lpm(bot)
    const imie2 = await utworz(bot, name, r, 'postac 2')
    const k2 = (await s.rcon(`papi parse ${name} %mmocore_class%`)).trim()
    // --- dwie zmiany postaci w sesji
    for (let i = 1; i <= 3; i++) {
      await s.rcon(`cc wybor ${name}`)
      await s.waitScene(bot)
      await s.press(bot, 'D', 2500)
      await s.lpm(bot, 500)
      let st
      for (let t = 0; t < 30; t++) { await sleep(500); st = await s.stan(name); if (st.gm === '0') break }
      await sleep(4000)
      const k = (await s.rcon(`papi parse ${name} %mmocore_class%`)).trim()
      r.assert(st.gm === '0' && k && !ARCH.test(k), `zmiana postaci nr ${i}: w grze, klasa rasowa`, `${k} (${JSON.stringify(st)})`)
    }
    console.log('POSTACIE', imie1, imie2, k2)
  } catch (e) { r.fail('wyjatek', String(e && e.stack || e).slice(0, 300)) }
  if (bot) await h.quit(bot)
  r.print(); process.exit(r.failed ? 1 : 0)
})()
