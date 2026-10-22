'use strict'
/** Krytyka 2 / naduzycia: zasieg klikniecia, ucieczka w trakcie kanalu, wylogowanie w trakcie kanalu. */
const L = require('/w/krytyka2/lib.js')
const { sleep } = L
const [NA, NB] = process.env.EVAL_NICKS.split(' ')
const znacznik = (bot, t) => { bot.chat(`/minecraft:say ZNACZNIK-START ${t}`) }
const koniec = (bot, t) => { bot.chat(`/minecraft:say ZNACZNIK-KONIEC ${t} zyje=-`) }
;(async () => {
  const [A, B] = await Promise.all([L.gotowy(NA), L.gotowy(NB)])
  try {
    A.chat('/minecraft:forceload add 149 197'); await sleep(400)
    A.chat('/minecraft:forceload add 160 197'); await sleep(400)
    for (const b of [A, B]) { b.chat(`/minecraft:gamemode ${process.env.EVAL_GM || "creative"} ${b.nick}`); await sleep(300) }
    await L.tp(A, 149.5, 99, 197.5); await L.tp(B, 149.5, 99, 195.5)
    console.log('A:', await L.cmd(A, `minecraft:data get entity ${NA} Pos`, 700))
    // --- zasieg: wezly coraz dalej wzdluz +x, klik bez patrzenia ---
    for (const d of (process.env.EVAL_GM === 'survival' ? [2.5, 3.5, 4, 4.5, 5, 5.5, 6.5] : [2.5, 3.5, 4.5, 5.5, 7, 10])) {
      const x = 149.5 + d
      const w = await L.spawnWezel(A, 'NODE_HERB_PATCH', x, 99.5, 197.5)
      if (!w) { console.log('zasieg', d, 'brak wezla'); continue }
      znacznik(A, `zasieg-${d}`); await sleep(300)
      L.klik(A, w); await sleep(1500)
      const zyje = /passed/i.test(await L.cmd(A, `minecraft:execute if entity @e[type=zombie,x=${x},y=99.5,z=197.5,distance=..0.8]`, 800))
      console.log(`zasieg ${d} bl.: wezel ${zyje ? 'NIE zebrany' : 'ZEBRANY'}`)
      koniec(A, `zasieg-${d}`); await sleep(300)
      if (zyje) { A.chat(`/minecraft:execute as @e[type=zombie,x=${x},y=99.5,z=197.5,distance=..0.8] run minecraft:tp @s ~ -300 ~`); await sleep(600) }
    }
    // --- ucieczka w trakcie kanalu poletka: klik i tp 60 blokow ---
    let w = await L.spawnWezel(A, 'NODE_CROP_FIELD', 150.5, 99.5, 198.5)
    await sleep(800)
    znacznik(A, 'kanal-ucieczka'); await sleep(300)
    L.klik(A, w); await sleep(300)
    A.chat(`/minecraft:tp ${NA} 209.5 120 197.5`); await sleep(4000)
    console.log('ucieczka: pozycja A', await L.cmd(A, `minecraft:data get entity ${NA} Pos`, 700))
    koniec(A, 'kanal-ucieczka'); await sleep(300)
    await L.tp(A, 149.5, 99, 197.5)
    // --- wylogowanie w trakcie kanalu druzy (B klika i wychodzi) ---
    w = await L.spawnWezel(A, 'NODE_GEM_GEODE', 150.5, 99.5, 196.5)
    await sleep(800)
    const eB = B.entities[w.id]
    znacznik(A, 'kanal-wylogowanie'); await sleep(300)
    L.klik(B, eB); await sleep(500)
    await L.H.quit(B)
    await sleep(4500)
    const zyje = /passed/i.test(await L.cmd(A, `minecraft:execute if entity @e[type=zombie,x=150.5,y=99.5,z=196.5,distance=..0.8]`, 800))
    console.log('wylogowanie: wezel po kanale', zyje ? 'ZYJE' : 'usuniety')
    koniec(A, 'kanal-wylogowanie'); await sleep(500)
    A.chat('/minecraft:forceload remove 149 197'); await sleep(300)
    A.chat('/minecraft:forceload remove 160 197'); await sleep(300)
  } finally { await L.H.quit(A); try { await L.H.quit(B) } catch {} }
  process.exit(0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
