'use strict'
const L = require('/t/lib.js')
;(async () => {
  const nick = process.env.EVAL_NICKS || 'HdA4'
  const bot = await L.H.connect(nick, { timeout: 90000 })
  const w0 = await L.F.waitProfileScreen(bot).catch(() => null)
  if (w0) { const k = L.kafelki(w0).find(x => x.lore.some(l => /Left click to select/i.test(l))); if (k) await L.klik(bot, k.slot) }
  await L.sleep(4000)
  const test = async (etyk, sciezka, slot) => {
    await L.otworzHandel(bot); for (const [s, re] of sciezka) { await L.klik(bot, s); await L.czekajOkno(bot, re) }
    await L.klik(bot, slot); await L.sleep(1500)
    console.log(etyk, '->', bot.currentWindow ? L.tytul(bot.currentWindow) : 'ZAMKNIETE')
  }
  await test('AH glowne: Wstecz(45)', [[10, /Dom aukcyjny/]], 45)
  await test('AH glowne: Zamknij(53)', [[10, /Dom aukcyjny/]], 53)
  await test('Moje oferty: Zamknij(53)', [[10, /Dom aukcyjny/], [48, /Moje oferty/]], 53)
  await test('Moje oferty: Wroc(45)', [[10, /Dom aukcyjny/], [48, /Moje oferty/]], 45)
  await test('Wystaw: Zamknij(53)', [[10, /Dom aukcyjny/], [49, /Wystaw/]], 53)
  await test('Skup: Wstecz(45)', [[12, /Skup/]], 45)
  await test('Skup: Zamknij(53)', [[12, /Skup/]], 53)
  await test('Sklep: Wstecz(45)', [[14, /Sklep/]], 45)
  bot.quit(); process.exit(0)
})()
