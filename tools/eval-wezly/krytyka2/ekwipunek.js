'use strict'
/** Krytyka 2 / ekwipunek: pelny plecak, stackowanie, zrodla tego samego id. */
const L = require('/w/krytyka2/lib.js')
const { sleep } = L
const [NA] = process.env.EVAL_NICKS.split(' ')
const sloty = bot => bot.inventory.slots.map((it, i) => it ? `${i}:${it.count}x${L.label(it)}[${it.name}]` : null).filter(Boolean).join(' ')
const przedmiotyObok = async (bot) => L.cmd(bot, `minecraft:execute at ${bot.nick} if entity @e[type=item,distance=..4]`, 900)

;(async () => {
  const A = await L.gotowy(NA)
  try {
    A.chat(`/minecraft:forceload add 150 199`); await sleep(500)
    await L.tp(A, 149.5, 99, 198.5)
    for (const ef of ['resistance 900 4', 'regeneration 900 4', 'saturation 900 4', 'fire_resistance 900 0']) { A.chat(`/minecraft:effect give ${NA} minecraft:${ef} true`); await sleep(250) }
    A.chat(`/minecraft:gamemode survival ${NA}`); await sleep(500)
    A.chat(`/minecraft:clear ${NA}`); await sleep(800)
    console.log('PO CLEAR sloty:', sloty(A))

    // --- 1. pelny plecak + mi give ---
    A.chat(`/minecraft:kill @e[type=item,distance=..10]`); await sleep(500)
    A.chat(`/minecraft:give ${NA} minecraft:cobblestone 2304`); await sleep(1500)
    const wolne = A.inventory.slots.slice(9, 45).filter(s => !s).length
    console.log('wolne sloty plecaka po zapchaniu:', wolne, '| empty slots:', A.inventory.emptySlotCount())
    A.chat(`/minecraft:say ZNACZNIK-START pelny-mi-give`); await sleep(300)
    const odp = await L.cmd(A, `mi give MATERIAL PROF_MEDICINAL_HERB ${NA} 5`, 1500)
    console.log('odpowiedz mi give przy pelnym:', odp.slice(-300))
    console.log('item na ziemi obok gracza?', await przedmiotyObok(A))
    const ents = Object.values(A.entities).filter(e => e.name === 'item' && e.position.distanceTo(A.entity.position) < 5)
    console.log('encje item obok (widok bota):', ents.length, ents.map(e => { try { const it = e.getDroppedItem(); return it && (it.count + 'x ' + L.label(it)) } catch { return '?' } }).join(', '))
    A.chat(`/minecraft:say ZNACZNIK-KONIEC pelny-mi-give zyje=-`); await sleep(300)

    // --- 2. pelny plecak + wezel ziol ---
    A.chat(`/minecraft:kill @e[type=item,distance=..10]`); await sleep(600)
    const w = await L.spawnWezel(A, 'NODE_HERB_PATCH', 150.5, 99.5, 199.5)
    await sleep(1000)
    A.chat(`/minecraft:say ZNACZNIK-START pelny-wezel`); await sleep(300)
    if (w) { L.klik(A, w); await sleep(2000) } else console.log('brak wezla')
    console.log('pelny+wezel: item na ziemi?', await przedmiotyObok(A))
    const ents2 = Object.values(A.entities).filter(e => e.name === 'item' && e.position.distanceTo(A.entity.position) < 5)
    console.log('encje item obok:', ents2.map(e => { try { const it = e.getDroppedItem(); return it && (it.count + 'x ' + L.label(it)) } catch { return '?' } }).join(', '))
    A.chat(`/minecraft:say ZNACZNIK-KONIEC pelny-wezel zyje=-`); await sleep(300)

    // --- 3. stackowanie: mi give 1 + 1 ---
    A.chat(`/minecraft:clear ${NA}`); await sleep(800)
    A.chat(`/minecraft:kill @e[type=item,distance=..10]`); await sleep(500)
    for (let i = 0; i < 2; i++) { A.chat(`/mi give MATERIAL PROF_MEDICINAL_HERB ${NA} 1`); await sleep(700) }
    A.chat(`/mi give MATERIAL PROF_RAW_FLEECE ${NA} 1`); await sleep(700)
    A.chat(`/mi give MATERIAL PROF_MEDICINAL_HERB ${NA} 70`); await sleep(900)
    console.log('STACK mi give 1+1+70 ziol, 1 runo:', sloty(A))

    // --- 4. runo z dropu owcy vs runo z mi give ---
    let proby = 0
    while (proby++ < 12) {
      const przed = A.inventory.items().filter(i => /runo/i.test(L.label(i))).reduce((a, i) => a + i.count, 0)
      A.chat(`/mm m spawn BEAST_MOOR_SHEEP 1 world,150.5,99,199.5`); await sleep(1200)
      A.chat(`/minecraft:damage @e[type=sheep,x=150.5,y=99,z=199.5,distance=..2,limit=1] 1000 minecraft:player_attack by ${NA}`); await sleep(1200)
      A.chat(`/minecraft:execute at ${NA} run tp @e[type=item,distance=..8] ${NA}`); await sleep(1500)
      const po = A.inventory.items().filter(i => /runo/i.test(L.label(i))).reduce((a, i) => a + i.count, 0)
      if (po > przed) break
    }
    console.log('STACK runo (mi give + drop owcy):', sloty(A))
    const runa = A.inventory.slots.map((it, i) => it && /runo/i.test(L.label(it)) ? it : null).filter(Boolean)
    for (const r of runa) console.log('  runo slot', r.slot, 'count', r.count, 'components:', JSON.stringify(r.components || r.nbt || {}).slice(0, 600))
    A.chat(`/minecraft:clear ${NA}`); await sleep(600)
    A.chat(`/minecraft:forceload remove 150 199`); await sleep(500)
  } finally { await L.H.quit(A) }
  process.exit(0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
