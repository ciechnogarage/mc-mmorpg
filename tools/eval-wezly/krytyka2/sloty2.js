'use strict'
/** Krytyka 2 / sloty profesji: rozjazd CoreTools <-> MMOCore, format EXP, podwojne klikniecie platnej zmiany. */
const L = require('/w/krytyka2/lib.js')
const { sleep } = L
const [NA] = process.env.EVAL_NICKS.split(' ')
const papi = async (A, ph) => { const t = await L.cmd(A, `papi parse ${NA} ${ph}`, 1000); return t.split(' | ').pop() }
const stan = async (A, p) => `poziom=${await papi(A, `%mmocore_profession_${p}%`)} exp=${await papi(A, `%mmocore_profession_experience_${p}%`)} slot1=${await papi(A, '%coretools_var_player_profession_slot_1%')} slot2=${await papi(A, '%coretools_var_player_profession_slot_2%')} saved_lvl=${await papi(A, `%coretools_var_player_profession_saved_${p}_level%`)} saved_exp=${await papi(A, `%coretools_var_player_profession_saved_${p}_exp%`)}`
async function menu(A, cmd, tytul) {
  A.chat('/' + cmd)
  const w = await L.H.waitForWindow(A, { timeout: 6000, titleIncludes: tytul })
  return w
}
;(async () => {
  const A = await L.gotowy(NA)
  try {
    A.chat(`/minecraft:gamemode creative ${NA}`); await sleep(300)
    console.log('0. start:', await stan(A, 'herbalism'))
    // 1. XP zielarstwa BEZ wybranej profesji (sloty nie blokuja XP - znane z sloty.js)
    await L.cmd(A, `mmocore admin exp give ${NA} herbalism 400`, 900)
    console.log('1. po 400 XP bez slotu:', await stan(A, 'herbalism'))
    // 2. wybor zielarstwa w slocie 1 przez menu -> przywrocenie 'saved' (nieustawionego)
    let w = await menu(A, 'profesje', 'Profesje')
    console.log('   menu glowne:', w ? L.H.windowTitle(w) : 'BRAK')
    A.chat(`/minecraft:say ZNACZNIK-START slot-wybor`); await sleep(200)
    if (w) { await A.clickWindow(11, 0, 0); await sleep(1200) }
    w = A.currentWindow; console.log('   okno po kliknieciu 11:', w ? L.H.windowTitle(w) : 'BRAK')
    if (w) { await A.clickWindow(12, 0, 0); await sleep(1500) }
    A.chat(`/minecraft:say ZNACZNIK-KONIEC slot-wybor zyje=x`); await sleep(200)
    try { A.closeWindow(A.currentWindow) } catch {}
    console.log('2. po wyborze zielarstwa w slocie 1:', await stan(A, 'herbalism'))
    // 3. format EXP przy duzej wartosci
    await L.cmd(A, `mmocore admin exp give ${NA} herbalism 3000`, 900)
    await L.cmd(A, `mmocore admin exp set ${NA} herbalism 1234.5`, 900)
    console.log('3. format exp po set 1234.5:', await stan(A, 'herbalism'))
    // 4. platna zmiana: pieniadze + podwojne klikniecie w tym samym ticku
    console.log('   saldo przed:', await papi(A, '%vault_eco_balance%'), await L.cmd(A, `cmi money set ${NA} 1000`, 900))
    console.log('   saldo po ustawieniu:', await papi(A, '%vault_eco_balance%'))
    w = await menu(A, 'profesje', 'Profesje')
    const it = w && w.slots[11]; console.log('   slot 11 menu:', it ? L.label(it) : '-', '| lore:', it ? JSON.stringify(it.components || '').slice(0, 0) : '')
    A.chat(`/minecraft:say ZNACZNIK-START slot-platna-podwojna`); await sleep(200)
    if (w) { A.clickWindow(11, 0, 0).catch(() => {}); A.clickWindow(11, 0, 0).catch(() => {}); await sleep(2000) }
    A.chat(`/minecraft:say ZNACZNIK-KONIEC slot-platna-podwojna zyje=x`); await sleep(200)
    try { A.closeWindow(A.currentWindow) } catch {}
    console.log('4. po podwojnym kliknieciu platnej zmiany: saldo', await papi(A, '%vault_eco_balance%'), '|', await stan(A, 'herbalism'))
    console.log('   cooldown slot1:', await papi(A, '%coretools_var_timestamp_player_default_profession_slot_1_cooldown%'))
    // 5. ponowny wybor zielarstwa - czy wraca poziom i exp
    w = await menu(A, 'profesje', 'Profesje')
    if (w) { await A.clickWindow(11, 0, 0); await sleep(1200) }
    w = A.currentWindow; if (w && /slot 1/.test(L.H.windowTitle(w))) { await A.clickWindow(12, 0, 0); await sleep(1500) }
    try { A.closeWindow(A.currentWindow) } catch {}
    console.log('5. po ponownym wyborze zielarstwa:', await stan(A, 'herbalism'))
  } finally { await L.H.quit(A) }
  process.exit(0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
