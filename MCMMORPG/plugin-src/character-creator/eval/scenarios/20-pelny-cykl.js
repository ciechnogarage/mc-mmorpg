'use strict'
/** Pelny cykl: wejscie -> kreator -> wybor -> zatwierdzenie -> wyjscie w bezpieczne miejsce. */
const H = require('../lib/harness'); const F = require('../lib/flow')
// Musi byc to samo, co 'creator-room.exit' w config.yml. Przeniesione 2026-09-20 ze
// 239.5/63/208.5 — tamto miejsce jest pod woda.
const EXIT = { x: 149.5, y: 99, z: 197.5 }
module.exports = async function (r) {
  const bot = await H.connect('EvCykl' + Date.now() % 100000)
  try {
    const w = await F.enterCreator(bot)
    if (!r.assert(!!w, 'kreator otwiera sie po "Create New Character"')) return
    r.assert(F.inRoom(bot), 'gracz jest w sali kreatora', bot.entity.position.toString())
    r.assert(bot.game.gameMode === 'spectator', 'tryb spectator w kreatorze', bot.game.gameMode)
    const slots = H.windowSlots(w).map(s => s.label)
    r.assert(['Człowiek', 'Ork', 'Elf', 'Nieumarły'].every(x => slots.includes(x)), 'widoczne 4 rasy')
    r.assert(['Wojownik', 'Mag', 'Akolita', 'Ninja'].every(x => slots.includes(x)), 'widoczne 4 klasy')

    await F.completeCreator(bot, { race: 'ork', klass: 'mag' })
    const created = bot.evalLog.some(m => m.includes('Postać utworzona'))
    r.assert(created, 'serwer potwierdza utworzenie postaci',
      bot.evalLog.filter(m => m.includes('Postać')).slice(-1)[0] || 'brak komunikatu')

    const p = bot.entity.position
    const atExit = Math.abs(p.x - EXIT.x) < 6 && Math.abs(p.z - EXIT.z) < 6
    r.assert(atExit, 'wyjscie na skonfigurowane creator-room.exit, nie na surowy spawn', p.toString())
    r.assert(!F.inRoom(bot), 'gracz opuscil sale kreatora')
    r.assert(bot.game.gameMode !== 'spectator', 'tryb gry przywrocony', bot.game.gameMode)

    await H.sleep(10000)
    r.assert(bot.health === undefined || bot.health > 0, 'gracz zyje 10 s po wyjsciu',
      'hp=' + bot.health)
  } finally { await H.quit(bot) }
}
