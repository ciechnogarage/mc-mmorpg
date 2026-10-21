'use strict'
// E1: nowy gracz tworzy postac kazdej rasy (inna klasa na rase) — tylko klawisze i ksiega.
const s = require('./lib/scena')
const { h, sleep } = s
const RASY = [['czlowiek', 0], ['ork', 1], ['elf', 2], ['nieumarly', 3]]
const KLASY = ['wojownik', 'mag', 'akolita', 'ninja']
;(async () => {
  const r = new h.Results('E1 rasy')
  const suf = process.env.SUF || String(Date.now() % 10000)
  for (const [rasa, i] of RASY) {
    const name = `NapR${i}_${suf}`
    let bot
    try {
      bot = await s.join(name)
      r.assert(await s.waitScene(bot), `${name}: scena kreatora po wejsciu`)
      for (let k = 0; k < i; k++) await s.press(bot, 'D', 2200)
      for (let k = 0; k < i; k++) await s.press(bot, 'S', 500)
      await s.lpm(bot)
      const d = await s.waitDialog(bot, /Nowa postać/)
      r.assert(!!d, `${name}: ksiega "Nowa postac" po LPM`)
      const tekst = h.dialogText(d)
      const imie = (s.inputs(d).find(x => x.key === 'imie') || {}).initial || ''
      r.assert(imie.length >= 3 && imie !== name, `${name}: imie startowe losowe, nie nick`, imie)
      r.assert(/Rola:/.test(tekst) && !/Hardcore|wkrótce/i.test(tekst), `${name}: podsumowanie z rola, bez "Hardcore: wkrotce"`, tekst.replace(/\n/g, ' | ').slice(0, 160))
      r.assert(!/[\u{10000}-\u{10FFFF}]/u.test(JSON.stringify(d)), `${name}: brak znakow spoza BMP w oknie`)
      await s.clickWith(bot, /Stwórz/, { imie, kolor: 'zielony' })
      let st, maxY = 0, gms = new Set()
      const przyWyjsciu = x => { const p = (x.pos || '').split(',').map(parseFloat); return Math.abs(p[0] - 149.5) < 3 && Math.abs(p[2] - 197.5) < 3 }
      for (let t = 0; t < 60; t++) {
        await sleep(500); st = await s.stan(name); gms.add(st.gm)
        if (st.gm === '0' && przyWyjsciu(st)) break
      }
      r.assert(st.gm === '0' && /overworld/.test(st.dim || '') && przyWyjsciu(st), `${name}: po przelocie na starcie (149/197), survival`, JSON.stringify(st))
      await sleep(4000) // mapowanie archetyp -> klasa rasowa (CoreTools, ~1-2 s)
      const klasa = await s.rcon(`papi parse ${name} %mmocore_class%`)
      r.assert(klasa && !/^(Human|Warrior|Mage|Acolyte|Ninja|Rogue|Paladin)\b/i.test(klasa.trim()), `${name}: klasa rasowa po utworzeniu`, klasa.trim())
      console.log(`POSTAC ${name} ${imie} ${rasa} ${KLASY[i]}`)
    } catch (e) { r.fail(`${name}: wyjatek`, String(e && e.message || e)) }
    if (bot) await h.quit(bot)
  }
  r.print(); process.exit(r.failed ? 1 : 0)
})()
