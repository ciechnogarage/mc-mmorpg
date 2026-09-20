'use strict'
/** Dwoch graczy naraz w kreatorze — kazdy ma miec wlasna sesje i wlasne okno. */
const H = require('../lib/harness'); const F = require('../lib/flow')
module.exports = async function (r) {
  const a = await H.connect('EvDwA' + Date.now() % 100000)
  let b = null
  try {
    b = await H.connect('EvDwB' + Date.now() % 100000)
    const [wa, wb] = [await F.enterCreator(a), await F.enterCreator(b)]
    r.assert(!!wa && !!wb, 'obaj gracze weszli do kreatora')
    await H.sleep(5000)
    r.assert(F.isCreatorWindow(a) && F.isCreatorWindow(b), 'obaj nadal maja otwarty kreator')
    const refused = [...a.evalLog, ...b.evalLog].some(m => m.includes('Jesteś już w kreatorze'))
    r.assert(!refused, 'zadnemu nie odmowiono sesji')
    r.assert(F.inRoom(a) && F.inRoom(b), 'obaj sa w sali kreatora')
  } finally { await H.quit(a); if (b) await H.quit(b) }
}
