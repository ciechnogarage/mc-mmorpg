'use strict'
/** Bot wchodzi, przechodzi kreator i stoi EVAL_N sekund - do recznego badania rconem. */
const L = require('/w/krytyka2/lib.js')
const [NA] = process.env.EVAL_NICKS.split(' ')
;(async () => {
  const A = await L.gotowy(NA)
  console.log('gotowy', NA, 'okno:', A.currentWindow ? L.H.windowTitle(A.currentWindow) : '-', 'gm', A.game.gameMode)
  const end = Date.now() + (+process.env.EVAL_N || 120) * 1000
  while (Date.now() < end) { await L.sleep(5000); console.log('poz bota', A.entity.position.toString(), 'items', L.inv(A).slice(0, 200)) }
  await L.H.quit(A); process.exit(0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
