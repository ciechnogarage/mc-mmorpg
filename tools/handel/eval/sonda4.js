'use strict'
const L = require('/t/lib.js'); const { rconClient } = require('/t/rcon.js')
;(async () => {
  const rc = await rconClient(); const nick = process.env.EVAL_NICKS || 'HdA4'
  const bot = await L.H.connect(nick, { timeout: 90000 })
  const w0 = await L.F.waitProfileScreen(bot).catch(() => null)
  if (w0) { const k = L.kafelki(w0).find(x => x.lore.some(l => /Left click to select/i.test(l))); if (k) await L.klik(bot, k.slot) }
  await L.sleep(4000)
  await rc.cmd(`mi give MATERIAL PROF_MOONLEAF ${nick} 3`); await L.sleep(800)
  const ile = async () => L.strip(await rc.cmd(`minecraft:clear ${nick} *[minecraft:custom_data~{MMOITEMS_ITEM_ID:"PROF_MOONLEAF"}] 0`))
  console.log('start', await ile())
  await L.otworzHandel(bot); await L.klik(bot, 10); await L.czekajOkno(bot, /Dom aukcyjny/); await L.klik(bot, 49)
  const w = await L.czekajOkno(bot, /Wystaw/)
  const s = w.slots.findIndex((it, i) => i >= w.inventoryStart && it && /Księżycowy/.test(L.nazwa(it)))
  await L.klik(bot, s); await L.klik(bot, 13); console.log('w slocie', bot.currentWindow.slots[13] && bot.currentWindow.slots[13].count, 'plecak', await ile())
  await L.klik(bot, 53); await L.sleep(2000)
  console.log('po Zamknij: okno=', bot.currentWindow ? L.tytul(bot.currentWindow) : 'brak', 'plecak', await ile())
  bot.quit(); rc.close(); process.exit(0)
})()
