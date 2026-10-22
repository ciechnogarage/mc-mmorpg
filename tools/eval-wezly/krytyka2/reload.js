'use strict'
/** Krytyka 2 / mm reload w trakcie zbierania: licznik ciec, kanal, rejestr wezlow, spawner. */
const L = require('/w/krytyka2/lib.js')
const { sleep } = L
const [NA] = process.env.EVAL_NICKS.split(' ')
const Z = (x, y, z, d = 0.8) => `x=${x},y=${y},z=${z},distance=..${d}`
const jest = async (A, x, y, z) => /passed/i.test(await L.cmd(A, `minecraft:execute if entity @e[type=zombie,${Z(x, y, z)}]`, 700))
;(async () => {
  const A = await L.gotowy(NA)
  try {
    A.chat('/minecraft:forceload add 149 197'); await sleep(400)
    A.chat(`/minecraft:gamemode creative ${NA}`); await sleep(300)
    await L.tp(A, 149.5, 99, 197.5)
    const H = await L.spawnWezel(A, 'NODE_HERB_PATCH', 151.5, 99.5, 198.5)
    const O = await L.spawnWezel(A, 'NODE_TIMBER_OAK', 147.5, 99.5, 198.5)
    const C = await L.spawnWezel(A, 'NODE_CROP_FIELD', 150.5, 99.5, 195.5)
    console.log('wezly:', !!H, !!O, !!C)
    console.log('mobow przy spawnerze demo (przed):', await L.cmd(A, `minecraft:execute if entity @e[type=zombie,${Z(149, 99, 197, 1.5)}]`, 700))
    A.chat('/minecraft:say ZNACZNIK-START reload'); await sleep(300)
    L.klik(A, O); await sleep(1300); L.klik(A, O); await sleep(1300)
    console.log('dab po 2 cieciach:', L.czyste(A).filter(l => /Cięcie/.test(l)).join(' / '))
    L.klik(A, C); await sleep(400)
    A.chat('/mm reload'); await sleep(9000)
    console.log('kanal poletka po reloadzie: poletko', (await jest(A, 150.5, 99.5, 195.5)) ? 'ZYJE' : 'usuniete')
    A.evalLog.length = 0
    L.klik(A, O); await sleep(1500)
    console.log('dab 3. ciecie po reloadzie:', L.czyste(A).filter(l => /Cięcie|pada/.test(l)).join(' / ') || '(brak reakcji)', '| dab', (await jest(A, 147.5, 99.5, 198.5)) ? 'ZYJE' : 'usuniety')
    A.evalLog.length = 0
    L.klik(A, H); await sleep(1500)
    console.log('ziola po reloadzie:', L.czyste(A).filter(l => /Zrywasz|obfit|okaz/.test(l)).join(' / ') || '(brak reakcji)', '| kepa', (await jest(A, 151.5, 99.5, 198.5)) ? 'ZYJE' : 'usunieta')
    console.log('mobow przy spawnerze demo (po):', await L.cmd(A, `minecraft:execute if entity @e[type=zombie,${Z(149, 99, 197, 1.5)}]`, 700))
    console.log('mm m list? killall:', (await L.cmd(A, 'mm m killall', 1200)).slice(-200))
    await sleep(500)
    for (const [x, y, z] of [[151.5, 99.5, 198.5], [147.5, 99.5, 198.5], [150.5, 99.5, 195.5]])
      console.log(`po killall encja ${x},${z}:`, (await jest(A, x, y, z)) ? 'NADAL JEST (sierota)' : 'brak')
    console.log('spawner demo po killall:', await L.cmd(A, `minecraft:execute if entity @e[type=zombie,${Z(149, 99, 197, 1.5)}]`, 700))
    A.chat('/minecraft:say ZNACZNIK-KONIEC reload zyje=x'); await sleep(300)
  } finally { await L.H.quit(A) }
  process.exit(0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
