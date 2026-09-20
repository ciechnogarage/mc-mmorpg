'use strict'
/** Rekonesans: pokazuje realny uklad okien na sciezce nowego gracza. Nic nie zaklada. */
const H = require('../lib/harness')

const dump = (tag, win) => {
  if (!win) return console.log(`${tag}: BRAK OKNA`)
  console.log(`${tag}: type=${win.type} id=${win.id} slots=${win.slots.length} title="${H.windowTitle(win)}"`)
  for (const s of H.windowSlots(win)) console.log(`   slot ${String(s.slot).padStart(2)} ${s.name} "${s.label}"`)
}

;(async () => {
  const user = process.env.EVAL_USER || ('Eval' + Math.floor(Math.random() * 9000 + 1000))
  console.log('# gracz: ' + user)
  const bot = await H.connect(user)
  console.log(`lobby: ${bot.game.gameMode} @ ${bot.entity.position}`)

  await H.switchServer(bot, H.TARGET)
  await H.sleep(3000)
  console.log(`${H.TARGET}: ${bot.game.gameMode} @ ${bot.entity.position}`)

  const first = await H.waitForWindow(bot, { timeout: 20000 })
  dump('okno po wejsciu', first)

  if (first) {
    const create = H.findSlot(first, /create|nowa|nowy|\+/i)
    console.log('kandydat "stworz postac": ' + JSON.stringify(create))
    if (create) {
      await H.click(bot, create.slot)
      await H.sleep(3000)
      dump('okno po kliknieciu', bot.currentWindow)
      console.log(`pozycja=${bot.entity.position} tryb=${bot.game.gameMode} swiat=${bot.game.dimension}`)
    }
  }
  console.log('--- ostatnie wiadomosci ---')
  for (const m of bot.evalLog.slice(-8)) console.log('  ' + m)
  await H.quit(bot)
  process.exit(0)
})().catch(e => { console.log('BLAD: ' + e.message); process.exit(1) })
