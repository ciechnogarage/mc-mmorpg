'use strict'
/** Obciazenie: K botow stoi w siatce wezlow i klika najblizszy wezel co ~1.1 s przez EVAL_N sekund. */
const L = require('/w/krytyka2/lib.js')
const { sleep } = L
const NICKS = process.env.EVAL_NICKS.split(' ')
const CZAS = (+process.env.EVAL_N || 240) * 1000
const MIEJSCA = [[146.5, 191.5], [151.5, 191.5], [146.5, 195.5], [151.5, 195.5], [146.5, 199.5], [151.5, 199.5], [148.5, 193.5], [153.5, 197.5]]
;(async () => {
  const bots = []
  for (const n of NICKS) { bots.push(await L.gotowy(n)); await sleep(500) }
  let kliki = 0
  await Promise.all(bots.map(async (b, i) => {
    const [x, z] = MIEJSCA[i % MIEJSCA.length]
    b.chat(`/minecraft:gamemode creative ${b.nick}`); await sleep(300)
    await L.tp(b, x, 100, z)
    await sleep(2000)
    const koniec = Date.now() + CZAS
    let ostCzysc = Date.now()
    while (Date.now() < koniec) {
      const cel = Object.values(b.entities).filter(e => e.name === 'zombie' && e.position.y > 100.5 && e.position.distanceTo(b.entity.position) < 5)
        .sort((p, q) => p.position.distanceTo(b.entity.position) - q.position.distanceTo(b.entity.position))[0]
      if (cel) { L.klik(b, cel); kliki++ }
      await sleep(1100)
      if (Date.now() - ostCzysc > 30000) { b.chat(`/minecraft:clear ${b.nick}`); ostCzysc = Date.now() }
    }
  }))
  console.log('klikniec wyslanych:', kliki)
  for (const b of bots) await L.H.quit(b)
  process.exit(0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
