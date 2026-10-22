'use strict'
/** Diagnostyka: co NAPRAWDE robi kazda komenda, ktora wola kafelek menu.
 *  Bot tworzy postac, po czym wpisuje komendy po kolei i notuje: czy otworzylo sie okno
 *  i co przyszlo na czat. To odpowiada na pytanie "kafelek nie dziala czy nie ma tresci". */
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')
const KOMENDY = ['player','attributes','class','skills','skilltrees','cast','quests',
                 'waypoints','ah','core-playervault','deposit','withdraw','party','friends',
                 'guild','pvpmode']
module.exports = async function (r) {
  const bot = await H.connect('EvDiag' + Date.now() % 100000)
  try {
    if (!r.assert(!!(await F.waitProfileScreen(bot)), 'ekran wyboru')) return
    if (!r.assert(!!(await F.enterCreator(bot)), 'kreator')) return
    await F.completeCreator(bot); await H.sleep(3000)
    for (const cmd of KOMENDY) {
      const przed = bot.evalLog.length
      bot.chat('/' + cmd)
      await H.sleep(2500)
      const okno = bot.currentWindow
      const tytul = okno ? H.windowTitle(okno) : ''
      const czat = bot.evalLog.slice(przed).map(m => m.replace(/\s+/g,' ').trim())
                     .filter(Boolean).join(' | ').slice(0, 150)
      console.log(`  /${cmd.padEnd(18)} okno=${tytul ? '"'+tytul+'"' : '-'}  czat: ${czat || '-'}`)
      if (okno) { bot.closeWindow(okno); await H.sleep(1000) }
    }
    r.ok('diagnostyka wykonana')
  } finally { await H.quit(bot) }
}
