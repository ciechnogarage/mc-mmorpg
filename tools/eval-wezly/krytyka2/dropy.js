'use strict'
/** Krytyka 2 / dropy bestiariusza: N zabic przez /damage ... by <gracz> (zabojca = gracz), liczenie lupu w plecaku. */
const L = require('/w/krytyka2/lib.js')
const { sleep } = L
const [NA] = process.env.EVAL_NICKS.split(' ')
const N = +(process.env.EVAL_N || 30)
const MOBY = (process.env.EVAL_TRYB || 'UNDEAD_GRAVE_CRAWLER:zombie,BEAST_CAVE_SPIDERLING:cave_spider,BEAST_MOOR_SHEEP:sheep').split(',')
const P = { x: 150.5, y: 99, z: 200.5 }
const policz = bot => { const o = {}; for (const i of bot.inventory.items()) { const k = `${L.label(i)}[${i.name}]`; o[k] = (o[k] || 0) + i.count } return o }

;(async () => {
  const A = await L.gotowy(NA)
  try {
    A.chat(`/minecraft:forceload add 150 200`); await sleep(500)
    await L.tp(A, 149.5, 99, 198.5)
    for (const ef of ['resistance 1800 4', 'regeneration 1800 4', 'saturation 1800 4']) { A.chat(`/minecraft:effect give ${NA} minecraft:${ef} true`); await sleep(250) }
    A.chat(`/minecraft:gamemode survival ${NA}`); await sleep(500)
    for (const mm of MOBY) {
      const [id, typ] = mm.split(':')
      A.chat(`/minecraft:clear ${NA}`); await sleep(600)
      A.chat(`/minecraft:kill @e[type=item,distance=..12]`); await sleep(400)
      A.chat(`/minecraft:say ZNACZNIK-START dropy-${id}`); await sleep(300)
      let zabite = 0, zywe = 0
      for (let i = 0; i < N; i++) {
        A.chat(`/mm m spawn ${id} 1 world,${P.x},${P.y},${P.z}`); await sleep(900)
        A.chat(`/minecraft:damage @e[type=${typ},x=${P.x},y=${P.y},z=${P.z},distance=..3,limit=1,sort=nearest] 2000 minecraft:player_attack by ${NA}`); await sleep(900)
        const t = await L.cmd(A, `minecraft:execute if entity @e[type=${typ},x=${P.x},y=${P.y},z=${P.z},distance=..1.5,nbt=!{Invulnerable:1b}]`, 700)
        if (process.env.EVAL_DEBUG) console.log('  itemy przy miejscu smierci:', await L.cmd(A, `minecraft:execute if entity @e[type=item,x=${P.x},y=${P.y},z=${P.z},distance=..4]`, 700), '| gracz:', await L.cmd(A, `minecraft:data get entity ${NA} Pos`, 700))
        if (/passed/i.test(t)) { zywe++; A.chat(`/mm m kill ${id}`); await sleep(500) } else zabite++
        A.chat(`/minecraft:execute at ${NA} run tp @e[type=item,distance=..10] ${NA}`); await sleep(900)
      }
      await sleep(1500)
      A.chat(`/minecraft:say ZNACZNIK-KONIEC dropy-${id} zyje=${zywe}`); await sleep(300)
      console.log(`${id}: proby ${N}, zabite (znikly) ${zabite}, przezyly ${zywe}; lup:`, JSON.stringify(policz(A)))
    }
    A.chat(`/minecraft:clear ${NA}`); await sleep(500)
    A.chat(`/minecraft:forceload remove 150 200`); await sleep(500)
  } finally { await L.H.quit(A) }
  process.exit(0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
