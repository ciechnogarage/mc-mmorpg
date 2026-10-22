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
;(async () => {
  const A = await L.gotowy(NA)
  try {
    A.chat(`/minecraft:gamemode creative ${NA}`); await sleep(300)
    for (const c of [`eco give ${NA} dollar 1000`, `eco add ${NA} dollar 1000`, `eco ${NA} give dollar 1000`]) console.log('eco:', c, '->', (await L.cmd(A, c, 1000)).slice(-160))
    console.log('saldo:', await saldo(A))
    // zielarstwo w slocie 1 i poziom >= 10 (sciezka platna)
    console.log('wybor:', await wybierz(A, 11, 'slot 1', 12))
    await L.cmd(A, `mmocore admin exp give ${NA} herbalism 4000`, 900)
    console.log('A.', await stan(A), '| saldo', await saldo(A))
    // podwojne klikniecie platnej zmiany w jednym ticku
    let w = await otworz(A)
    const it = w && w.slots[11]; console.log('   slot 11:', it ? L.label(it) : '-')
    A.chat('/minecraft:say ZNACZNIK-START platna-podwojna'); await sleep(200)
    if (w) { A.clickWindow(11, 0, 0).catch(() => {}); A.clickWindow(11, 0, 0).catch(() => {}); await sleep(2500) }
    A.chat('/minecraft:say ZNACZNIK-KONIEC platna-podwojna zyje=x'); await sleep(200)
    try { A.closeWindow(A.currentWindow) } catch {}
    console.log('B. po podwojnym kliknieciu:', await stan(A), '| saldo', await saldo(A))
    // gornik do slotu 1 od razu (cooldown blokuje tylko ZWALNIANIE slotu)
    console.log('wybor gornik slot1:', await wybierz(A, 11, 'slot 1', 10))
    console.log('C. gornik w slocie 1:', await stan(A))
    // proba ponownej zmiany slotu 1 w trakcie cooldownu
    w = await otworz(A); if (w) { await A.clickWindow(11, 0, 0); await sleep(1300) }
    console.log('D. zmiana slotu 1 w cooldownie ->', L.czyste(A).slice(-2).join(' / '), '| okno', A.currentWindow ? L.H.windowTitle(A.currentWindow) : '-')
    try { A.closeWindow(A.currentWindow) } catch {}
    // obejscie: zielarstwo do slotu 2
    console.log('wybor ziel slot2:', await wybierz(A, 15, 'slot 2', 12))
    console.log('E. zielarstwo w slocie 2 (obejscie):', await stan(A), '| saldo', await saldo(A))
  } finally { await L.H.quit(A) }
  process.exit(0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
