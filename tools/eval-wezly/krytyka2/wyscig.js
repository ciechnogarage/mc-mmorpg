'use strict'
/**
 * Krytyka 2 / wyscigi: dwa boty klikaja ten sam wezel.
 * Kazda proba: czyszczenie plecakow, swiezy wezel (konsola bota A), odczyt XP obu,
 * wzorzec klikniec, odczyt plecakow i XP. Wynik: tabela kto ile dostal.
 */
const L = require('/w/krytyka2/lib.js')
const { sleep } = L
const [NA, NB] = process.env.EVAL_NICKS.split(' ')
const NX = 150.5, NY = 99.5, NZ = 199.5
const TRYB = (process.env.EVAL_TRYB || 'all').split(',')

const PROBY = [
  // nazwa, typ, profesja, wzorzec [ [bot, ms od startu], ... ], czekaj ms
  ['ziola-ten-sam-tick', 'NODE_HERB_PATCH', 'herbalism', [['A', 0], ['B', 0]], 2500, 5],
  ['ziola-B+50ms', 'NODE_HERB_PATCH', 'herbalism', [['A', 0], ['B', 50]], 2500, 3],
  ['grzyby-ten-sam-tick', 'NODE_MUSHROOM_RING', 'herbalism', [['A', 0], ['B', 0]], 2500, 3],
  ['przyprawy-ten-sam-tick', 'NODE_SPICE_PATCH', 'cooking', [['A', 0], ['B', 0]], 2500, 3],
  ['poletko-ten-sam-tick', 'NODE_CROP_FIELD', 'farming', [['A', 0], ['B', 0]], 4500, 2],
  ['poletko-B+1200ms', 'NODE_CROP_FIELD', 'farming', [['A', 0], ['B', 1200]], 4500, 2],
  ['poletko-B+2450ms', 'NODE_CROP_FIELD', 'farming', [['A', 0], ['B', 2450]], 4500, 2],
  ['druza-ten-sam-tick', 'NODE_GEM_GEODE', 'jewelcrafting', [['A', 0], ['B', 0]], 5000, 2],
  ['druza-B+2950ms', 'NODE_GEM_GEODE', 'jewelcrafting', [['A', 0], ['B', 2950]], 5500, 2],
  ['glina-ten-sam-tick', 'NODE_CLAY_DEPOSIT', 'building', [['A', 0], ['B', 0]], 4500, 2],
  ['dab-naprzemian', 'NODE_TIMBER_OAK', 'woodworking', [['A', 0], ['B', 1200], ['A', 2400], ['B', 3600]], 2500, 2],
  ['dab-ten-sam-tick-x3', 'NODE_TIMBER_OAK', 'woodworking', [['A', 0], ['B', 0], ['A', 1200], ['B', 1200], ['A', 2400], ['B', 2400]], 2500, 2],
  ['dab-A-szybko-x6', 'NODE_TIMBER_OAK', 'woodworking', [['A', 0], ['A', 300], ['A', 600], ['A', 900], ['A', 1200], ['A', 1500]], 3000, 1],
  ['brzoza-ten-sam-tick-x2', 'NODE_TIMBER_BIRCH', 'woodworking', [['A', 0], ['B', 0], ['A', 1200], ['B', 1200]], 2500, 2],
]

;(async () => {
  const [A, B] = await Promise.all([L.gotowy(NA), L.gotowy(NB)])
  const bots = { A, B }
  try {
    A.chat(`/minecraft:forceload add ${Math.floor(NX)} ${Math.floor(NZ)}`); await sleep(600)
    await Promise.all([L.tp(A, 149.5, 99, 198.5), L.tp(B, 151.5, 99, 198.5)])
    for (const b of [A, B]) { b.chat(`/minecraft:gamemode creative ${b.nick}`); await sleep(300) }
    console.log('pozycje serwer', await L.cmd(A, `minecraft:data get entity ${NA} Pos`, 900), await L.cmd(A, `minecraft:data get entity ${NB} Pos`, 900))
    const wyniki = []
    for (const [nazwa, typ, prof, wz, czekaj, n] of PROBY) {
      if (!TRYB.includes('all') && !TRYB.some(t => nazwa.includes(t))) continue
      for (let k = 0; k < n; k++) {
        for (const b of [A, B]) { b.chat(`/minecraft:clear ${b.nick}`); await sleep(250) }
        await sleep(500)
        A.chat(`/minecraft:say ZNACZNIK-START ${nazwa}#${k}`); await sleep(300)
        const xa0 = 0, xb0 = 0
        const w = await L.spawnWezel(A, typ, NX, NY, NZ)
        if (!w) { console.log(`${nazwa}#${k}: wezel sie nie pojawil`); continue }
        await sleep(1200)
        for (const b of [A, B]) { const e = b.entities[w.id]; if (e) { try { await b.lookAt(e.position.offset(0, 0.5, 0), true) } catch {} } }
        await sleep(300)
        A.evalLog.length = 0; B.evalLog.length = 0
        // klikniecia z tym samym ms wysylane synchronicznie w jednej petli JS
        const grupy = {}
        for (const [kto, ms] of wz) (grupy[ms] = grupy[ms] || []).push(kto)
        const t0 = Date.now()
        for (const ms of Object.keys(grupy).map(Number).sort((a, b) => a - b)) {
          const d = t0 + ms - Date.now(); if (d > 0) await sleep(d)
          for (const kto of grupy[ms]) {
            const bot = bots[kto]; const e = bot.entities[w.id]
            if (!e) { console.log(`  ${kto}@${ms}: wezla juz nie ma u bota`); continue }
            L.klik(bot, e)
          }
        }
        await sleep(czekaj)
        const msgA = L.czyste(A).filter(l => /Pozyskujesz|pada|Zrywasz|Zbierasz|Wykopujesz|obfit|okaz|bogaty|wyłom|pękła|zebrane|Cięcie|Ostrożnie|Nabierasz/i.test(l))
        const msgB = L.czyste(B).filter(l => /Pozyskujesz|pada|Zrywasz|Zbierasz|Wykopujesz|obfit|okaz|bogaty|wyłom|pękła|zebrane|Cięcie|Ostrożnie|Nabierasz/i.test(l))
        const ia = L.suma(A), ib = L.suma(B)
        const xa1 = 0, xb1 = 0
        const zyje = /passed/i.test(await L.cmd(A, `minecraft:execute if entity @e[type=zombie,x=${NX},y=${NY},z=${NZ},distance=..1]`, 900))
        A.chat(`/minecraft:say ZNACZNIK-KONIEC ${nazwa}#${k} zyje=${zyje}`); await sleep(300)
        const row = { nazwa, k, itemsA: ia, itemsB: ib, xpA: +(xa1 - xa0).toFixed(2), xpB: +(xb1 - xb0).toFixed(2), wezelZyje: zyje,
          invA: L.inv(A), invB: L.inv(B), msgA: msgA.join(' / ').slice(0, 200), msgB: msgB.join(' / ').slice(0, 200) }
        wyniki.push(row)
        console.log(JSON.stringify(row))
        if (zyje) { A.chat(`/minecraft:execute as @e[type=zombie,x=${NX},y=${NY},z=${NZ},distance=..1] run minecraft:tp @s ~ -300 ~`); await sleep(800) }
      }
    }
    console.log('\n===== PODSUMOWANIE =====')
    for (const r of wyniki) console.log(`${r.nazwa}#${r.k}: A ${r.itemsA} szt/${r.xpA} XP | B ${r.itemsB} szt/${r.xpB} XP | wezel po: ${r.wezelZyje ? 'ZYJE' : 'usuniety'}`)
    A.chat(`/minecraft:forceload remove ${Math.floor(NX)} ${Math.floor(NZ)}`); await sleep(600)
  } finally { await L.H.quit(A); await L.H.quit(B) }
  process.exit(0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
