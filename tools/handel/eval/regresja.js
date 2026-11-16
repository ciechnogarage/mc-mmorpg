'use strict'
/**
 * TEST REGRESJI HANDLU world1 — dom aukcyjny, skup, sklep.
 *
 * Gracze: 3 konta BEZ OP (A sprzedawca, B i C kupcy). Wchodza przez kreator postaci, menu otwieraja
 * KLAWISZEM Q (pakiet do pluginu MenuKey) i wszystko robia klikaniem. Zadnej komendy z czatu.
 * RCON (konsola) sluzy wylacznie do przygotowania danych (przedmioty, salda) i do POMIAROW
 * (saldo przez PAPI, audyt kopii bazy SQLite CoreTools).
 *
 * Kazdy punkt umie OBLAC — kontrole negatywne: EVAL_SABOTAZ=dupe|wygasanie (w tescie)
 * oraz podatek|skup (sabotaz.sh zmienia config przed uruchomieniem). Kazdy sabotaz musi dac FAIL.
 */
const L = require('/t/lib.js')
const { rconClient } = require('/t/rcon.js')
const { DatabaseSync } = require('node:sqlite')
const { execSync } = require('child_process')
const AH = 'mmoitems_example'
const SAB = process.env.EVAL_SABOTAZ || ''
const R = new L.H.Results('handel')
const EPS = 0.001
const blisko = (a, b) => Math.abs(a - b) < EPS
const [nA, nB, nC] = (process.env.EVAL_NICKS || `HA${Date.now() % 10000} HB${Date.now() % 10000} HC${Date.now() % 10000}`).split(' ')

// ---------- baza: kopia pliku (serwer pisze na zywo w trybie WAL) ----------
function db(sql, ...p) {
  execSync('mkdir -p /tmp/q && cp /db/CoreTools.db* /tmp/q/')
  const d = new DatabaseSync('/tmp/q/CoreTools.db'); try { return d.prepare(sql).all(...p) } finally { d.close() }
}
const oferty = nick => db(`select id, amount, original_amount, status, taken, unit_price from coretools_auctionhouse_${AH} where seller_name = ?`, nick)
const podatek = () => { const r = db(`select tax_balance from coretools_auctionhouse where ah_id = ?`, AH)[0]; try { return JSON.parse(r.tax_balance).collected_tax.stolica.dollar || 0 } catch { return 0 } }

// ---------- klawiatura ceny w oknie "Wystaw przedmiot" ----------
const KLAW = { 7: 19, 8: 20, 9: 21, 4: 28, 5: 29, 6: 30, 1: 37, 2: 38, 3: 39, '.': 46, 0: 47 }
// Przedmioty liczy SERWER ('clear <gracz> <predykat> 0' nic nie zabiera, tylko liczy). Lokalny ekwipunek
// bota nie odswieza sie, gdy otwarte jest okno kontenera — pierwszy przebieg testu dal przez to falszywe FAIL-e.
let RC = null
const PRED = { DAB: '*[minecraft:custom_data~{MMOITEMS_ITEM_ID:"PROF_OAK_TIMBER"}]', KSIEZ: '*[minecraft:custom_data~{MMOITEMS_ITEM_ID:"PROF_MOONLEAF"}]',
  KARMA: '*[minecraft:custom_data~{MMOITEMS_ITEM_ID:"PROF_KARMA_MIESNA"}]', DIRT: 'minecraft:dirt', KAMIEN: 'minecraft:stone_pickaxe' }
async function ile(bot, co) {
  const out = L.strip(await RC.cmd(`minecraft:clear ${bot.username} ${PRED[co]} 0`))
  const m = out.match(/Found (\d+)/); return m ? +m[1] : (/No items were found|No player/.test(out) ? 0 : NaN)
}
const DAB = /d[ęe]bowe/; const KSIEZ = /Księżycowy/; const KAMIEN = /Kamienny kilof|Stone Pickaxe/

async function otworzAH(bot) {
  const w = await L.otworzHandel(bot); if (!w) return null
  await L.klik(bot, 10); return L.czekajOkno(bot, /Dom aukcyjny|Aukcje/)
}
/** Wystawia wszystkie sztuki przedmiotu `re` po cenie `cena` (string). podwojnie = dwa potwierdzenia w jednym ticku. */
async function wystaw(bot, re, cena, { podwojnie = false, bezCeny = false } = {}) {
  if (!await otworzAH(bot)) return 'brak AH'
  await L.klik(bot, 49); const w = await L.czekajOkno(bot, /Wystaw/); if (!w) return 'brak okna Wystaw'
  const s = w.slots.findIndex((it, i) => i >= w.inventoryStart && it && re.test(L.nazwa(it)))
  if (s < 0) return 'brak przedmiotu w plecaku'
  await L.klik(bot, s); await L.klik(bot, 13)
  if (!bezCeny) for (const c of cena) { await L.klik(bot, KLAW[c]); await L.sleep(200) }
  await L.klik(bot, 42)
  if (podwojnie) { bot.clickWindow(42, 0, 0).catch(() => {}); bot.clickWindow(42, 0, 0).catch(() => {}) }
  else await L.klik(bot, 42)
  await L.sleep(1800)
  return 'ok'
}
async function znajdzOferte(bot, re, sprzedawca) {
  const w = await otworzAH(bot); if (!w) return null
  return L.kafelki(w).find(k => k.slot >= 9 && k.slot <= 44 && re.test(k.nazwa) && (!sprzedawca || k.lore.some(l => l.includes(sprzedawca))))
}
async function przygotujKupno(bot, re, sprzedawca) {
  const k = await znajdzOferte(bot, re, sprzedawca); if (!k) return false
  await L.klik(bot, k.slot); if (!await L.czekajOkno(bot, /Kupno/)) return false
  await L.klik(bot, 40); return true
}
async function mojeOferty(bot, zakladka) {
  if (!await otworzAH(bot)) return null
  await L.klik(bot, 48); let w = await L.czekajOkno(bot, /Moje oferty/); if (!w) return null
  await L.klik(bot, zakladka); await L.sleep(800); return bot.currentWindow
}
async function otworzSkup(bot) { const w = await L.otworzHandel(bot); if (!w) return null; await L.klik(bot, 12); return L.czekajOkno(bot, /Skup/) }
async function otworzSklep(bot) { const w = await L.otworzHandel(bot); if (!w) return null; await L.klik(bot, 14); return L.czekajOkno(bot, /Sklep/) }

/** Ponowne wejscie tego samego konta: wybor istniejacej postaci na ekranie MMOProfiles. */
async function wrocDoGry(nick) {
  const bot = await L.H.connect(nick, { timeout: 90000 })
  const w = await L.F.waitProfileScreen(bot).catch(() => null)
  if (w) {
    const k = L.kafelki(w).find(x => x.lore.some(l => /Left click to select/i.test(l)))
    if (process.env.EVAL_DEBUG) L.zrzut(w, 'wybor postaci')
    if (k) await L.klik(bot, k.slot)
  }
  await L.sleep(4000)
  return bot
}

;(async () => {
  const rc = await rconClient(); RC = rc
  const saldo = async n => parseFloat(L.strip(await rc.cmd(`papi parse ${n} %coretools_eco_dollar%`)))
  const ustaw = async (n, v) => rc.cmd(`core-economy force-set dollar ${v} ${n}`)
  const daj = async (n, id, x) => rc.cmd(`mi give MATERIAL ${id} ${n} ${x}`)
  console.log(`konta testowe: A=${nA} B=${nB} C=${nC}  sabotaz=${SAB || '-'}`)
  for (const n of [nA, nB, nC]) await rc.cmd(`deop ${n}`)
  let A = await L.wejdz(nA); const B = await L.wejdz(nB); const C = await L.wejdz(nC)
  for (const n of [nA, nB, nC]) { await rc.cmd(`minecraft:clear ${n}`); await ustaw(n, 100) }
  await daj(nA, 'PROF_OAK_TIMBER', 20); await daj(nA, 'PROF_MOONLEAF', 10)
  await L.sleep(1500)

  // ============ 1. wystawienie przez menu, oplata za wystawienie ============
  const tax0 = podatek()
  let r = await wystaw(A, DAB, '2')
  let o = oferty(nA).filter(x => x.status === 'active')
  R.assert(r === 'ok' && o.length === 1 && o[0].amount === 20 && blisko(o[0].unit_price, 2), '1a: A wystawil 20x drewno po 2 $ klikajac w menu', `${r}; oferty=${JSON.stringify(o)}`)
  let sA = await saldo(nA)
  R.assert(blisko(sA, 99), '1b: pobrano oplate za wystawienie 20*2*2,5% = 1,00 $', `saldo A ${sA}`)
  R.assert((await ile(A, 'DAB')) === 0, '1c: przedmiot zniknal z plecaka sprzedawcy', `A ma ${(await ile(A, 'DAB'))}`)
  const idDrewna = o[0] && o[0].id

  // ============ 2. kategorie: oferta widoczna w Surowcach, nie w Sprzecie ============
  let w = await otworzAH(B)
  await L.klik(B, 1); w = await L.czekajOkno(B, /Surowce/)
  const wSur = w && L.kafelki(w).some(k => DAB.test(k.nazwa) && k.lore.some(l => l.includes(nA)) && k.lore.some(l => /Cena: 2(\.0)? \$/.test(l)))
  await L.klik(B, 4); w = await L.czekajOkno(B, /Sprzęt/)
  const wSprz = w && L.kafelki(w).some(k => DAB.test(k.nazwa))
  R.assert(wSur && !wSprz, '2: zakladka Surowce pokazuje oferte z cena i sprzedawca; Sprzet jej nie pokazuje', `surowce=${wSur} sprzet=${wSprz}`)

  // ============ 3. kupno: salda, podatek 6%, przedmiot ============
  const przed = { a: await saldo(nA), b: await saldo(nB) }
  const ok3 = await przygotujKupno(B, DAB, nA)
  await L.klik(B, 40); await L.sleep(1800)
  if (SAB === 'dupe') await daj(nB, 'PROF_OAK_TIMBER', 1)       // KONTROLA NEGATYWNA: przedmiot znikad
  const po = { a: await saldo(nA), b: await saldo(nB) }
  R.assert(ok3 && blisko(po.b, przed.b - 40), '3a: B zaplacil 20 * 2 = 40 $', `${przed.b} -> ${po.b}`)
  R.assert(blisko(po.a, przed.a + 20 * (2 - 0.12)), '3b: A dostal 20 * (2 - 6%) = 37,60 $ od razu (online)', `${przed.a} -> ${po.a}`)
  R.assert((await ile(B, 'DAB')) === 20, '3c: B ma dokladnie 20 sztuk (zadna nie powstala ani nie zginela)', `B ma ${(await ile(B, 'DAB'))}`)
  const tax1 = podatek()
  R.assert(blisko(tax1 - tax0, 1 + 2.4), '3d: do kasy regionu (odplyw z obiegu) trafilo 1,00 oplaty + 2,40 podatku', `${tax0} -> ${tax1}`)
  R.assert((oferty(nA).find(x => x.id === idDrewna) || {}).status === 'sold', '3e: oferta ma status sold', JSON.stringify(oferty(nA).find(x => x.id === idDrewna)))

  // ============ 4. przedmioty zablokowane: vanilla i karma Chowancow nie wejda do slotu ============
  await rc.cmd(`minecraft:give ${nA} minecraft:dirt 5`); await daj(nA, 'PROF_KARMA_MIESNA', 2); await L.sleep(800)
  const n0 = oferty(nA).length
  r = await wystaw(A, /Dirt|Ziemia/, '1')
  const r2 = await wystaw(A, /Mięsna Karma/, '1')
  await L.sleep(500)
  R.assert(oferty(nA).length === n0 && (await ile(A, 'DIRT')) === 5 && (await ile(A, 'KARMA')) === 2, '4: ziemi (vanilla) i karmy Chowancow nie da sie wystawic; zostaja w plecaku', `ofert ${n0}->${oferty(nA).length}, dirt=${(await ile(A, 'DIRT'))}, karma=${(await ile(A, 'KARMA'))} (${r}/${r2})`)

  // ============ 5. brak ceny / zero: potwierdzenie nic nie robi, przedmiot wraca po zamknieciu ============
  const s5 = await saldo(nA)
  r = await wystaw(A, KSIEZ, '', { bezCeny: true })
  await L.klik(A, KLAW[0]); await L.klik(A, 42); await L.klik(A, 42)        // "0" jako pierwsza cyfra jest ignorowane
  await L.klik(A, 53); await L.sleep(1000)
  R.assert(oferty(nA).length === n0 && (await ile(A, 'KSIEZ')) === 10 && blisko(await saldo(nA), s5), '5: bez ceny (i z "0" na poczatku) nic sie nie wystawia, oplaty brak, liscie wracaja do plecaka', `ofert=${oferty(nA).length} lisci=${(await ile(A, 'KSIEZ'))}`)

  // ============ 6. podwojne klikniecie KUP w jednym ticku (ten sam kupiec) ============
  r = await wystaw(A, KSIEZ, '5')
  const b6 = await saldo(nB)
  const ok6 = await przygotujKupno(B, KSIEZ, nA)
  B.clickWindow(40, 0, 0).catch(() => {}); B.clickWindow(40, 0, 0).catch(() => {})
  await L.sleep(2500)
  R.assert(ok6 && blisko(await saldo(nB), b6 - 50) && (await ile(B, 'KSIEZ')) === 10, '6: dwa klikniecia KUP w jednym ticku = jedno kupno (50 $, 10 lisci)', `saldo ${b6}->${await saldo(nB)}, lisci=${(await ile(B, 'KSIEZ'))}`)

  // ============ 7. dwoch kupcow tej samej oferty w jednym ticku ============
  await daj(nA, 'PROF_OAK_TIMBER', 10); await L.sleep(800)
  r = await wystaw(A, DAB, '1')
  const b7 = { b: await saldo(nB), c: await saldo(nC) }; const d7 = { b: (await ile(B, 'DAB')), c: (await ile(C, 'DAB')) }
  const ok7 = (await przygotujKupno(B, DAB, nA)) && (await przygotujKupno(C, DAB, nA))
  B.clickWindow(40, 0, 0).catch(() => {}); C.clickWindow(40, 0, 0).catch(() => {})
  await L.sleep(2500)
  const a7 = { b: await saldo(nB), c: await saldo(nC) }; const e7 = { b: (await ile(B, 'DAB')) - d7.b, c: (await ile(C, 'DAB')) - d7.c }
  const jedenKupil = (e7.b === 10 && e7.c === 0 && blisko(a7.b, b7.b - 10) && blisko(a7.c, b7.c)) || (e7.c === 10 && e7.b === 0 && blisko(a7.c, b7.c - 10) && blisko(a7.b, b7.b))
  R.assert(ok7 && jedenKupil, '7: dwoch kupcow w jednym ticku — dokladnie jeden dostal 10 szt. i zaplacil 10 $, drugi nic', `przedmioty +${JSON.stringify(e7)} salda ${JSON.stringify(b7)}->${JSON.stringify(a7)}`)

  // ============ 8. podwojne potwierdzenie WYSTAWIENIA w jednym ticku (3 proby) ============
  let zle8 = []
  for (let i = 0; i < 3; i++) {
    await daj(nA, 'PROF_OAK_TIMBER', 4); await L.sleep(700)
    const przed8 = oferty(nA).filter(x => x.status === 'active').length; const s8 = await saldo(nA)
    await wystaw(A, DAB, '3', { podwojnie: true })
    const act = oferty(nA).filter(x => x.status === 'active')
    const nowe = act.length - przed8; const suma = act.slice(-nowe).reduce((a, x) => a + x.amount, 0)
    if (nowe !== 1 || suma !== 4 || (await ile(A, 'DAB')) !== 0 || !blisko(await saldo(nA), s8 - 0.3)) zle8.push(`proba ${i + 1}: nowych ofert ${nowe}, sztuk ${suma}, w plecaku ${(await ile(A, 'DAB'))}, saldo ${s8}->${await saldo(nA)}`)
  }
  R.assert(zle8.length === 0, '8: dwa potwierdzenia wystawienia w jednym ticku = jedna oferta, jedna oplata (3 proby)', zle8.join('; '))

  // ============ 9. wygasniecie i PODWOJNY odbior w jednym ticku ============
  const doWyg = oferty(nA).filter(x => x.status === 'active').pop()
  if (SAB !== 'wygasanie') await rc.cmd(`ct admin auctionhouse ${AH} expired ${doWyg && doWyg.id}`)
  await L.sleep(1500)
  const przed9 = (await ile(A, 'DAB'))
  w = await mojeOferty(A, 2)
  const kaf = w && L.kafelki(w).find(k => k.slot >= 9 && k.slot <= 44 && DAB.test(k.nazwa))
  if (kaf) { A.clickWindow(kaf.slot, 0, 0).catch(() => {}); A.clickWindow(kaf.slot, 0, 0).catch(() => {}) }
  await L.sleep(2500)
  const po9 = oferty(nA).find(x => x.id === (doWyg && doWyg.id)) || {}
  R.assert(!!kaf && (await ile(A, 'DAB')) - przed9 === 4 && po9.status === 'expired' && po9.taken === 1, '9: wygasla oferta jest w "Do odbioru"; dwa klikniecia odbioru w jednym ticku oddaja 4 szt., nie 8', `kafelek=${!!kaf} +${(await ile(A, 'DAB')) - przed9} szt., status=${po9.status} taken=${po9.taken}`)

  // ============ 10. pelny plecak przy zakupie: brak pobrania, oferta zostaje ============
  await rc.cmd(`minecraft:give ${nC} minecraft:cobblestone 2304`); await L.sleep(800)
  const c10 = await saldo(nC)
  const ofertaA = oferty(nA).filter(x => x.status === 'active').pop()
  const ok10 = await przygotujKupno(C, DAB, nA); await L.klik(C, 40); await L.sleep(2000)
  const po10 = oferty(nA).find(x => x.id === (ofertaA && ofertaA.id)) || {}
  R.assert(ok10 && blisko(await saldo(nC), c10) && po10.status === 'active', '10: pelny plecak — zakup odrzucony, pieniadze nie pobrane, oferta dalej aktywna', `saldo C ${c10}->${await saldo(nC)}, oferta ${po10.status}`)
  await rc.cmd(`minecraft:clear ${nC} minecraft:cobblestone`)

  // ============ 11. sprzedaz, gdy sprzedawca jest offline ============
  const sAoff = await saldo(nA); const ofertaOff = oferty(nA).filter(x => x.status === 'active').pop()
  A.quit(); await L.sleep(3000)
  const ok11 = await przygotujKupno(B, DAB, nA); await L.klik(B, 40); await L.sleep(2000)
  A = await wrocDoGry(nA)
  await L.sleep(3000)
  const sAon = await saldo(nA)
  const oczek = ofertaOff ? ofertaOff.amount * (ofertaOff.unit_price * 0.94) : NaN
  const wiad = A.evalLog.some(m => /Gdy Cię nie było/.test(m))
  R.assert(ok11 && blisko(sAon - sAoff, oczek) && wiad, '11: sprzedaz pod nieobecnosc — pieniadze i komunikat po powrocie', `+${(sAon - sAoff).toFixed(4)} (oczek. ${oczek}), komunikat=${wiad}`)

  // ============ 12. wylogowanie z przedmiotem w slocie wystawiania ============
  await daj(nA, 'PROF_MOONLEAF', 3); await L.sleep(800)
  await otworzAH(A); await L.klik(A, 49); w = await L.czekajOkno(A, /Wystaw/)
  const s12 = w && w.slots.findIndex((it, i) => i >= w.inventoryStart && it && KSIEZ.test(L.nazwa(it)))
  if (s12 > 0) { await L.klik(A, s12); await L.klik(A, 13) }
  const wSlocie = A.currentWindow && A.currentWindow.slots[13] && A.currentWindow.slots[13].count
  A.quit(); await L.sleep(3000)
  A = await wrocDoGry(nA); await L.sleep(2000)
  R.assert(wSlocie === 3 && (await ile(A, 'KSIEZ')) === 3, '12: rozlaczenie z przedmiotem w slocie wystawiania — przedmiot wraca do plecaka', `w slocie ${wSlocie}, po powrocie ${(await ile(A, 'KSIEZ'))}`)

  // ============ 13. skup: 1 sztuka, wszystkie, podwojny shift w jednym ticku ============
  await rc.cmd(`minecraft:clear ${nA} minecraft:oak_log`); await daj(nA, 'PROF_OAK_TIMBER', 10); await L.sleep(800)
  const k13 = 0.65
  let s13 = await saldo(nA)
  w = await otworzSkup(A)
  const t13 = w && L.kafelki(w).find(k => DAB.test(k.nazwa))
  const cenaWLore = t13 && t13.lore.some(l => l.includes(`${k13} $`))
  if (t13) await L.klik(A, t13.slot)                                           // LPM = 1 sztuka
  const po1 = await saldo(nA); const zostalo = (await ile(A, 'DAB'))
  w = await L.czekajOkno(A, /Skup/)
  const t13b = w && L.kafelki(w).find(k => DAB.test(k.nazwa))
  if (t13b) { A.clickWindow(t13b.slot, 0, 1).catch(() => {}); A.clickWindow(t13b.slot, 0, 1).catch(() => {}) }   // 2x shift w jednym ticku
  await L.sleep(2000)
  const po2 = await saldo(nA)
  R.assert(cenaWLore && blisko(po1 - s13, k13) && zostalo === 9, '13a: skup LPM — sprzedal 1 szt. za 0,65 $ (cena z modelu widoczna w opisie)', `+${(po1 - s13).toFixed(4)}, zostalo ${zostalo}, cena w lore=${cenaWLore}`)
  R.assert(blisko(po2 - po1, 9 * k13) && (await ile(A, 'DAB')) === 0, '13b: skup SHIFT+LPM x2 w jednym ticku — 9 szt. zaplacone raz (5,85 $), plecak pusty', `+${(po2 - po1).toFixed(4)}, zostalo ${(await ile(A, 'DAB'))}`)

  // ============ 14. sklep: kupno z pieniedzmi i bez ============
  const s14 = await saldo(nA)
  w = await otworzSklep(A)
  const kil = w && L.kafelki(w).find(k => KAMIEN.test(k.nazwa))
  if (kil) await L.klik(A, kil.slot)
  await L.sleep(800)
  const ok14a = blisko(await saldo(nA), s14 - 15) && (await ile(A, 'KAMIEN')) === 1
  await ustaw(nC, 0)
  w = await otworzSklep(C); const kilC = w && L.kafelki(w).find(k => KAMIEN.test(k.nazwa))
  if (kilC) await L.klik(C, kilC.slot)
  await L.sleep(800)
  R.assert(ok14a && (await ile(C, 'KAMIEN')) === 0 && blisko(await saldo(nC), 0), '14: sklep — kilof za 15 $; bez pieniedzy nic nie wydaje i nie schodzi na minus', `A kilof=${(await ile(A, 'KAMIEN'))}, C kilof=${(await ile(C, 'KAMIEN'))} saldo C=${await saldo(nC)}`)

  // ============ 15. petla kup-u-NPC -> sprzedaj-NPC: skup nie przyjmuje kilofa ze sklepu ============
  const s15 = await saldo(nA)
  w = await otworzSkup(A)
  let kilofWSkupie = false
  for (const zak of [0, 1, 2, 3, 4, 5, 6, 7, 8]) { await L.klik(A, zak); const ww = A.currentWindow; if (ww && L.kafelki(ww).some(k => KAMIEN.test(k.nazwa) || /pickaxe/i.test(k.name))) kilofWSkupie = true }
  R.assert(!kilofWSkupie && (await ile(A, 'KAMIEN')) === 1 && blisko(await saldo(nA), s15), '15: zadna zakladka skupu nie przyjmuje kilofa ze sklepu (brak petli NPC->NPC)', `kilof w skupie=${kilofWSkupie}`)

  // ============ audyt bazy: zadna oferta testowa nie ma sprzecznego stanu ============
  const wszystkie = [nA].flatMap(n => oferty(n))
  const sprzeczne = wszystkie.filter(x => (x.status === 'active' && x.taken) || (x.status === 'sold' && x.amount < 0) || x.amount > x.original_amount)
  R.assert(sprzeczne.length === 0, 'audyt: brak ofert w sprzecznym stanie (active+taken, ujemna ilosc, wiecej niz wystawiono)', JSON.stringify(sprzeczne))

  R.print()
  console.log(`WYNIK: ${R.failed ? 'FAIL' : 'PASS'} (${R.rows.length - R.failed}/${R.rows.length})`)
  for (const b of [A, B, C]) try { b.quit() } catch {}
  rc.close(); process.exit(R.failed ? 1 : 0)
})().catch(e => { console.log('BLAD', e); R.print(); process.exit(2) })
