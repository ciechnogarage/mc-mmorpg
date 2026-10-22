'use strict'
/**
 * Rasa jest zapisana jako uprawnienie LuckPerms na KONTO (race.<rasa>), a nie na postać.
 * Konto tworzy Orka Wojownika, potem drugą postać — Elfa Wojownika (ma dostać
 * Ostrze Gaju, nie klasę orka), potem wraca do Orka (ma zostać WODZ_WOJNY).
 */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
module.exports = async function (r) {
  const nick = 'EvDwie' + Date.now() % 100000
  const ph = async (...p) => (await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)).trim().split('|')
  const wejdz = async () => { const b = await H.connect(nick); const e = await F.waitProfileScreen(b); return [b, e] }
  let [bot] = await wejdz()
  try {
    await F.enterCreator(bot); await F.completeCreator(bot, { race: 'ork', klass: 'wojownik' }); await H.sleep(5000)
    const [k1] = await ph('mmocore_class_id')
    r.assert(k1.toUpperCase() === 'WODZ_WOJNY', 'postac 1: Ork Wojownik -> Wodz Wojny', k1)
    await H.quit(bot); await H.sleep(3000)
    let ekran; [bot, ekran] = await wejdz()
    console.log('  ekran:', H.windowSlots(ekran).filter(s => s.slot < ekran.inventoryStart).map(s => `${s.slot}:${s.name}`).join(' '))
    // po pierwszej postaci slot 10 zajmuje jej głowa — "utwórz" to pierwszy szmaragd
    const nowa = H.windowSlots(ekran).find(s => s.slot < ekran.inventoryStart && s.name === 'emerald')
    if (nowa) await H.click(bot, nowa.slot)
    let kreator = null
    for (let i = 0; i < 60 && !kreator; i++) { if (F.isCreatorWindow(bot)) kreator = bot.currentWindow; else await H.sleep(300) }
    if (!r.assert(!!kreator, 'kreator drugiej postaci otwarty')) return
    await F.completeCreator(bot, { race: 'elf', klass: 'wojownik' }); await H.sleep(5000)
    const [k2, ork2, elf2] = await ph('mmocore_class_id', 'luckperms_has_permission_race.ork', 'luckperms_has_permission_race.elf')
    console.log(`  postac 2: klasa=${k2} race.ork=${ork2} race.elf=${elf2}`)
    r.assert(k2.toUpperCase() === 'OSTRZE_GAJU', 'postac 2: Elf Wojownik -> Ostrze Gaju (rasa z aktywnej postaci)', k2)
    await H.quit(bot); await H.sleep(3000)
    ;[bot, ekran] = await wejdz()
    const glowy = H.windowSlots(ekran).filter(s => s.slot < ekran.inventoryStart && s.name === 'player_head')
    console.log('  postacie:', glowy.map(s => `${s.slot}:${s.label}`).join(' | '))
    // postać orka = pierwsza utworzona; etykiety to imiona — bierzemy tę, która NIE jest ostatnio użyta
    let wynik = null
    for (const g of glowy) {
      await H.click(bot, g.slot); await H.sleep(7000)
      if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(500) }
      const [k, o] = await ph('mmocore_class_id', 'luckperms_has_permission_race.ork')
      console.log(`  wybrana ${g.label}: klasa=${k} race.ork=${o}`)
      if (o === 'yes') { wynik = k; break }
      await H.quit(bot); await H.sleep(3000); [bot, ekran] = await wejdz()
    }
    r.assert(wynik && wynik.toUpperCase() === 'WODZ_WOJNY', 'powrot do postaci orka: nadal Wodz Wojny', wynik || 'nie znaleziono postaci orka')
  } finally { await H.quit(bot) }
}
