'use strict'
/**
 * Krytyka 2 / dropy bestiariusza, licznik PO STRONIE SERWERA.
 * Zabicie: /damage <mob> 2000 player_attack by <gracz> (zabojca = gracz, odpala ~onDeath i XP).
 * Po kazdym zabiciu: dla kazdego id z tabeli 'execute if entity @e[type=item,...,nbt={..MMOITEMS_ITEM_ID..}]'
 * (0/1 na zabicie), potem kasujemy itemy przy miejscu smierci. Bot NIE podnosi niczego -
 * pierwszy pomiar liczyl lup w plecaku bota i zanizal wynik.
 */
const L = require('/w/krytyka2/lib.js')
const { sleep } = L
const [NA] = process.env.EVAL_NICKS.split(' ')
const N = +(process.env.EVAL_N || 30)
const MOBY = {
  UNDEAD_GRAVE_CRAWLER: ['zombie', ['PROF_BEAST_BONE', 'HUMAN_SOUL']],
  BEAST_CAVE_SPIDERLING: ['cave_spider', ['PROF_VENOM_SAC', 'PROF_BEAST_SCRAP']],
  BEAST_MOOR_SHEEP: ['sheep', ['PROF_RAW_FLEECE', 'PROF_BEAST_SCRAP']],
  BEAST_WILD_BOAR: ['pig', ['PROF_BEAST_SCRAP', 'PROF_BEAST_BONE']],
}
const wybrane = (process.env.EVAL_TRYB || Object.keys(MOBY).join(',')).split(',')
const P = { x: 152.5, y: 99, z: 194.5 }
const sel = (extra = '') => `x=${P.x},y=${P.y},z=${P.z},distance=..4${extra}`

;(async () => {
  const A = await L.gotowy(NA)
  try {
    A.chat(`/minecraft:forceload add 152 194`); await sleep(500)
    const GM = process.env.EVAL_GM || 'survival'
    A.chat(`/minecraft:gamemode ${GM} ${NA}`); await sleep(400)
    for (const ef of ['resistance 1800 4', 'regeneration 1800 4', 'saturation 1800 4']) { A.chat(`/minecraft:effect give ${NA} minecraft:${ef} true`); await sleep(250) }
    console.log('tryb gry gracza:', GM)
    // pozycja gracza nie ma znaczenia dla licznika; warunek tabel to playerwithin{d=60}
    console.log('gracz:', await L.cmd(A, `minecraft:data get entity ${NA} Pos`, 800))
    const kontrola = await L.cmd(A, `mi give MATERIAL PROF_RAW_FLEECE ${NA} 1`, 600)
    A.chat(`/minecraft:kill @e[type=item,${sel()}]`); await sleep(400)
    console.log('kalibracja mi drop:', await L.cmd(A, `mi drop MATERIAL PROF_RAW_FLEECE world ${P.x} ${P.y} ${P.z} 100 1-1 0`, 800))
    console.log('kalibracja licznika (oczekiwane Count: 1):', await L.cmd(A, `minecraft:execute if entity @e[type=item,${sel()},nbt={Item:{components:{"minecraft:custom_data":{MMOITEMS_ITEM_ID:"PROF_RAW_FLEECE"}}}}]`, 600))
    for (const id of wybrane) {
      const [typ, lup] = MOBY[id]
      A.chat(`/minecraft:kill @e[type=item,${sel()}]`); await sleep(400)
      const wynik = Object.fromEntries(lup.map(l => [l, 0])); let zabite = 0, xpLinie = 0
      for (let i = 0; i < N; i++) {
        A.chat(`/mm m spawn ${id} 1 world,${P.x},${P.y},${P.z}`); await sleep(700)
        const d = await L.cmd(A, `minecraft:damage @e[type=${typ},${sel()},limit=1,sort=nearest] 2000 minecraft:player_attack by ${NA}`, 700)
        if (/Applied/i.test(d)) zabite++
        for (const l of lup) {
          const t = await L.cmd(A, `minecraft:execute if entity @e[type=item,${sel()},nbt={Item:{components:{"minecraft:custom_data":{MMOITEMS_ITEM_ID:"${l}"}}}}]`, 450)
          const m = t.match(/Count:\s*(\d+)/); if (m) wynik[l] += +m[1]
        }
        A.chat(`/minecraft:kill @e[type=item,${sel()}]`); await sleep(250)
      }
      console.log(`${id}: zabite ${zabite}/${N}; zabic z dropem danego id:`, JSON.stringify(wynik))
    }
    A.chat(`/minecraft:clear ${NA}`); await sleep(400)
    A.chat(`/minecraft:forceload remove 152 194`); await sleep(400)
  } finally { await L.H.quit(A) }
  process.exit(0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
