'use strict'
/**
 * Rozgrywka z perspektywy gracza (bot bez OP). Konsola tylko przygotowuje (klasa, poziom, punkty atrybutow,
 * wydanie przedmiotu) i czyta stan (execute if items, papi). Zakladanie, przeciaganie kamienia, sloty akcesoriow — bot.
 *  A. wymagania: poziom, glowny atrybut, bieglosc wagi
 *  B. set zbroi (2/4) — przyrost statystyk po zalozeniu kolejnych czesci
 *  C. set regaliow (bron + 4 ozdoby w slotach MMOInventory 9-17) — prog 2/3/5
 *  D. jakosc: osełka przeciagnieta na bron -> Jakość +1% i wieksze obrazenia
 */
const { opis, rcon, ph, H } = require('/h/evale/wspolne.js')
const SERW = { head: 'armor.head', torso: 'armor.chest', legs: 'armor.legs', feet: 'armor.feet' }
const SLOTY = [['head', 'HELM', 'PRZ_SET_CALUN_NOCNEGO_LISA_HELM'], ['torso', 'KLATA', 'PRZ_SET_CALUN_NOCNEGO_LISA_KLATA'], ['legs', 'NOGI', 'PRZ_SET_CALUN_NOCNEGO_LISA_NOGI'], ['feet', 'BUTY', 'PRZ_SET_CALUN_NOCNEGO_LISA_BUTY']]

module.exports = async function (r) {
  let nick, bot
  const nowy = async () => { if (bot) { bot.quit(); await H.sleep(1500) } nick = 'ItmG' + Date.now() % 100000; bot = await H.connect(nick); await H.sleep(3000) }
  const inv = () => bot.inventory.items().filter(i => !/Sakwa|Dziennik/.test(opis(i).nazwa))
  const znajdz = frag => inv().find(i => { const o = opis(i); return o.id === frag || o.nazwa.includes(frag) })
  const czekaj = async (frag, ms = 6000) => { for (let t = 0; t < ms; t += 300) { const i = znajdz(frag); if (i) return i; await H.sleep(300) } return null }
  const odloz = async () => { if (bot.inventory.cursorItem) { const w = bot.inventory.firstEmptyInventorySlot(); if (w != null) { await bot.clickWindow(w, 0, 0); await H.sleep(400) } } }
  const wyczysc = async () => { await rcon('minecraft:clear ' + nick); await H.sleep(700) }
  const daj = async (typ, id, tier, poz) => { await rcon(`mi generate ${nick} -type:${typ} -id:${id} -level:${poz || 5} -tier:${tier || 'ZWYKLY'}`); await H.sleep(900) }
  const pasek = []; let podpiety = false
  const zaloz = async (it, slot) => {
    if (!it) return 'brak przedmiotu'
    if (!podpiety) { bot._client.on('system_chat', p => { if (p.isActionBar || p.overlay) pasek.push(JSON.stringify(p.content).replace(/\\u00a7./g, '').slice(0, 160)) }); podpiety = true }
    for (let proba = 0; proba < 2; proba++) {
      pasek.length = 0
      try { await bot.equip(it, slot) } catch (e) { }
      await H.sleep(1500); await odloz()
      if (/passed/i.test(await rcon(`minecraft:execute if items entity ${nick} ${SERW[slot]} *`))) return proba ? 'zalozony (2. proba)' : 'zalozony'
      const nowy = inv().find(i => opis(i).nazwa === opis(it).nazwa); if (nowy) it = nowy
    }
    return 'odrzucony' + (pasek.length ? ' [' + pasek.join(' | ') + ']' : '')
  }
  const stat = async k => parseFloat((await ph(nick, 'mythiclib_stat_' + k))[0]) || 0
  const klasa = async (id, poz) => { await rcon(`mmocore class select ${nick} ${id}`); await H.sleep(600); await rcon(`mmocore admin level set ${nick} main ${poz}`); await H.sleep(600) }
  try {
    // --- A. wymagania -------------------------------------------------------------------------
    await nowy()
    await klasa('RYCERZ', 1); await wyczysc()
    await daj('ARMOR', 'PRZ_WOJ_HELM_4', 'ZWYKLY', 7)
    r.assert((await zaloz(await czekaj('PRZ_WOJ_HELM_4'), 'head')).startsWith('odrzucony'), 'A1 Rycerz poz.1: hełm z poziomu 7 odrzucony (za niski poziom)')
    await wyczysc(); await klasa('RYCERZ', 10)
    await daj('ARMOR', 'PRZ_WOJ_HELM_4', 'ZWYKLY', 7)
    r.assert((await zaloz(await czekaj('PRZ_WOJ_HELM_4'), 'head')).startsWith('odrzucony'), 'A2 Rycerz poz.10 bez Siły: hełm wymagający 3 Siły odrzucony')
    await wyczysc(); await rcon(`mmocore admin attribute give ${nick} strength 4`); await H.sleep(600)
    await daj('ARMOR', 'PRZ_WOJ_HELM_4', 'ZWYKLY', 7)
    r.assert((await zaloz(await czekaj('PRZ_WOJ_HELM_4'), 'head')).startsWith('zalozony'), 'A3 Rycerz poz.10 z 4 Siły: hełm średni założony')
    await wyczysc(); await daj('ARMOR', 'PRZ_WOJ_HELM_5', 'ZWYKLY', 10)
    r.assert((await zaloz(znajdz('PRZ_WOJ_HELM_5'), 'head')).startsWith('odrzucony'), 'A4 Rycerz: przyłbica ciężka odrzucona (brak biegłości ciężkiej — tylko specjalizacje obronne)')
    await wyczysc(); await daj('ARMOR', 'PRZ_NIN_HELM_1', 'ZWYKLY', 1)
    r.assert((await zaloz(znajdz('PRZ_NIN_HELM_1'), 'head')).startsWith('zalozony'), 'A5 Rycerz: lekka baza ninji założona (biegłość lekka, bez wymogu atrybutu na poz. 1)')

    // --- B. set zbroi ninji -------------------------------------------------------------------
    await nowy(); await klasa('AGENT', 10); await rcon(`mmocore admin attribute give ${nick} dexterity 4`); await H.sleep(600)
    const u0 = await stat('dodge_rating'), k0 = await stat('critical_strike_chance')
    const pomiary = []
    for (const [slot, cz, nz] of SLOTY) {
      await rcon(`mi give ARMOR PRZ_SET_CALUN_NOCNEGO_LISA_${cz} ${nick} 1`); await H.sleep(900)
      const it = await czekaj(nz)
      const w = await zaloz(it, slot)
      pomiary.push({ cz, w, unik: await stat('dodge_rating'), kryt: await stat('critical_strike_chance') })
    }
    r.assert(pomiary.every(p => p.w.startsWith('zalozony')), 'B1 Ninja: 4 części Całunu Nocnego Lisa założone', JSON.stringify(pomiary.map(p => p.w)))
    r.assert(pomiary[0].unik === u0, 'B2 1 część: brak bonusu setu (unik bez zmian)', `${u0} -> ${pomiary[0].unik}`)
    r.assert(Math.abs(pomiary[1].unik - u0 - 5) < 0.01, 'B3 2 części: +5 uniku', `${u0} -> ${pomiary[1].unik}`)
    r.assert(pomiary[3].kryt - pomiary[2].kryt >= 6 - 0.01, 'B4 czwarta część: bonus setu +6% szansy na krytyk (plus cecha części)', `${pomiary[2].kryt} -> ${pomiary[3].kryt}`)

    // --- C. set regaliow: sztylet + 3 ozdoby w slotach MMOInventory ---------------------------
    await nowy(); await klasa('AGENT', 10); await rcon(`mmocore admin attribute give ${nick} dexterity 4`); await H.sleep(600)
    const ozd = [['NECKLACE', 9, 'Wisior'], ['RING', 10, 'Pierścień'], ['CHARM', 14, 'Znak']]
    const kr0 = await stat('critical_strike_chance'), po0 = await stat('projectile_damage')
    await rcon(`mi give DAGGER PRZ_SET_BRACTWO_BEZIMIENNYCH_BRON ${nick} 1`); await H.sleep(900)
    const kiel = znajdz('PRZ_SET_BRACTWO_BEZIMIENNYCH_BRON')
    if (kiel) { await bot.equip(kiel, 'hand'); await H.sleep(800) }
    const krok = []
    for (const [t, slot, nz] of ozd) {
      await rcon(`mi give ${t} PRZ_SET_BRACTWO_BEZIMIENNYCH_${t} ${nick} 1`); await H.sleep(900)
      const it = inv().find(i => i.slot >= 18 && opis(i).id === 'PRZ_SET_BRACTWO_BEZIMIENNYCH_' + t)
      if (it) { await bot.clickWindow(it.slot, 0, 0); await H.sleep(400); await bot.clickWindow(slot, 0, 0); await H.sleep(900); await odloz() }
      { const k2 = znajdz('PRZ_SET_BRACTWO_BEZIMIENNYCH_BRON'); if (k2 && (!bot.heldItem || opis(bot.heldItem).id !== 'PRZ_SET_BRACTWO_BEZIMIENNYCH_BRON')) { await bot.equip(k2, 'hand'); await H.sleep(900) } }
      krok.push({ t, bron: opis(bot.heldItem || null)?.id, kryt: await stat('critical_strike_chance'), pocisk: await stat('projectile_damage'), wSlocie: !!bot.inventory.slots[slot] })
    }
    console.log('C kroki', JSON.stringify(krok), 'start kryt', kr0)
    r.assert(krok.every(k => k.wSlocie), 'C1 3 ozdoby setu zostają w slotach akcesoriów (9,10,14)', JSON.stringify(krok.map(k => k.wSlocie)))
    r.assert(krok.every(k => k.bron === 'PRZ_SET_BRACTWO_BEZIMIENNYCH_BRON'), 'C2 Kieł Bezimiennych cały czas w ręce', JSON.stringify(krok.map(k => k.bron)))
    r.assert(krok[0].pocisk === po0 && Math.abs(krok[1].pocisk - po0 - 8) < 0.01, 'C3 2 cz.: bez premii pocisków; 3 cz. (broń+naszyjnik+pierścień): +8% obrażeń pocisków', `${po0} -> ${krok[0].pocisk} -> ${krok[1].pocisk}`)

    // --- D. jakosc: oselka na bron ------------------------------------------------------------
    await nowy(); await klasa('RYCERZ', 10)
    console.log('D gen', await rcon(`mi generate ${nick} -type:SWORD -id:PRZ_WOJ_MIECZ_2 -level:5 -tier:ZWYKLY`)); await H.sleep(900)
    console.log('D eq', inv().map(i => opis(i).nazwa).join(', '))
    await (async()=>{})('SWORD', 'PRZ_WOJ_MIECZ_2', 'ZWYKLY', 5); 
    const miecz = () => znajdz('PRZ_WOJ_MIECZ_2'), dmg = it => parseFloat((opis(it).lore.find(l => l.includes('Obrażenia:')) || '').replace(/[^0-9.]/g, ''))
    const d0 = dmg(miecz())
    for (let n = 0; n < 3; n++) {
      await rcon(`mi give CONSUMABLE PRZ_OSELKA ${nick} 1`); await H.sleep(900)
      const kam = znajdz('PRZ_OSELKA'), m = miecz()
      if (!kam || !m) break
      await bot.clickWindow(kam.slot, 0, 0); await H.sleep(300); await bot.clickWindow(m.slot, 0, 0); await H.sleep(900); await odloz()
    }
    const m3 = miecz()
    r.assert(m3 && /Jakość \+3%/.test(opis(m3).nazwa), 'D1 3 osełki przeciągnięte na miecz: nazwa pokazuje Jakość +3%', m3 ? opis(m3).nazwa : 'brak miecza')
    r.assert(m3 && dmg(m3) > d0, 'D2 obrażenia miecza rosną z jakością', `${d0} -> ${m3 ? dmg(m3) : '?'}`)
  } finally { if (bot) bot.quit() }
}
