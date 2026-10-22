'use strict'
/** Krytyka 2 / sloty: platna zmiana, podwojne klikniecie, obejscie cooldownu drugim slotem. */
const L = require('/w/krytyka2/lib.js')
const { sleep } = L
const [NA] = process.env.EVAL_NICKS.split(' ')
const papi = async (A, ph) => (await L.cmd(A, `papi parse ${NA} ${ph}`, 1000)).split(' | ').pop()
const saldo = async A => (await L.cmd(A, `balance`, 1000)).split(' | ').filter(l => /balance|\$/i.test(l)).pop()
const stan = async (A) => `ziel=${await papi(A, '%mmocore_profession_herbalism%')} gorn=${await papi(A, '%mmocore_profession_mining%')} slot1=${await papi(A, '%coretools_var_player_profession_slot_1%')} slot2=${await papi(A, '%coretools_var_player_profession_slot_2%')} cd1=${await papi(A, '%coretools_var_timestamp_player_default_profession_slot_1_cooldown%')} cd2=${await papi(A, '%coretools_var_timestamp_player_default_profession_slot_2_cooldown%')}`
async function otworz(A) {
  A.chat('/profesje'); const w = await L.H.waitForWindow(A, { timeout: 6000, titleIncludes: 'Profesje' })
  await sleep(1500)
  if (w) console.log('   [menu] ' + w.slots.slice(0, 27).map((it, i) => it ? `${i}:${L.label(it)}` : null).filter(Boolean).join(' | '))
  return w
}
async function wybierz(A, glownySlot, podmenuTytul, pozycja) {
  let w = await otworz(A); if (!w) return 'brak menu'
  await A.clickWindow(glownySlot, 0, 0); await sleep(1300)
  w = A.currentWindow; const t = w ? L.H.windowTitle(w) : '-'
  if (w && t.includes(podmenuTytul)) { await A.clickWindow(pozycja, 0, 0); await sleep(1500) }
  try { A.closeWindow(A.currentWindow) } catch {}
  return t
}

const stanA = async (A) => `alch=${await papi(A, '%mmocore_profession_alchemy%')} alch_exp=${await papi(A, '%mmocore_profession_experience_alchemy%')} slot1=${await papi(A, '%coretools_var_player_profession_slot_1%')} cd1=${await papi(A, '%coretools_var_timestamp_player_default_profession_slot_1_cooldown%')} saved=${await papi(A, '%coretools_var_player_profession_saved_alchemy_level%')}/${await papi(A, '%coretools_var_player_profession_saved_alchemy_exp%')}`
;(async () => {
  const A = await L.gotowy(NA)
  try {
    A.chat(`/minecraft:gamemode creative ${NA}`); await sleep(300)
    console.log('core-economy:', (await L.cmd(A, 'core-economy', 1000)).slice(-300))
    for (const c of [`core-economy give ${NA} dollar 1000`, `core-economy set ${NA} dollar 1000`, `core-economy dollar give ${NA} 1000`, `core-economy add dollar ${NA} 1000`]) console.log('eco:', c, '->', (await L.cmd(A, c, 1000)).slice(-160))
    console.log('saldo:', await saldo(A))
    console.log('wybor alchemii:', await wybierz(A, 11, 'slot 1', 22))
    await L.cmd(A, `mmocore admin exp give ${NA} alchemy 4000`, 900)
    console.log('A.', await stanA(A), '| saldo', await saldo(A))
    let w = await otworz(A)
    A.chat('/minecraft:say ZNACZNIK-START platna-podwojna-alch'); await sleep(200)
    if (w) { A.clickWindow(11, 0, 0).catch(() => {}); A.clickWindow(11, 0, 0).catch(() => {}); await sleep(2500) }
    A.chat('/minecraft:say ZNACZNIK-KONIEC platna-podwojna-alch zyje=x'); await sleep(200)
    console.log('   okno po:', A.currentWindow ? L.H.windowTitle(A.currentWindow) : '-', '| czat:', L.czyste(A).slice(-3).join(' / '))
    try { A.closeWindow(A.currentWindow) } catch {}
    console.log('B. po podwojnym kliknieciu:', await stanA(A), '| saldo', await saldo(A))
    console.log('wybor alchemii ponownie:', await wybierz(A, 11, 'slot 1', 22))
    console.log('C. po ponownym wyborze:', await stanA(A))
  } finally { await L.H.quit(A) }
  process.exit(0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
