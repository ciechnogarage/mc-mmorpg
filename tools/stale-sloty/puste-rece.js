'use strict'
/** Przegląd 2026-09-26: skrypty stałych slotów przy PUSTEJ ręce / pustym slocie.
 *  {item_first_id} na pustym przedmiocie rzuca w CoreTools 1.4.2 NPE — tu wywołujemy
 *  zdarzenia, w których przedmiotu może nie być; licznik NPE czyta run z logu. */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
module.exports = async function (r) {
  const nick = 'EvPus' + Date.now() % 100000
  const bot = await H.connect(nick)
  const surowy = async (sl, b, mode) => {
    const okno = bot.currentWindow || bot.inventory; const orig = okno.acceptClick; okno.acceptClick = () => {}
    try { await bot.clickWindow(sl, b, mode) } finally { okno.acceptClick = orig }
  }
  try {
    await F.waitProfileScreen(bot); await F.enterCreator(bot)
    await F.completeCreator(bot); await H.sleep(4000)
    console.log('ZNACZNIK_START')
    await rcon(`say ZNACZNIK_START ${nick}`)
    // 1. lewy klik w powietrze pustą ręką (LEFT_CLICK_AIR), prawy w blok pustą ręką
    bot.setQuickBarSlot(5); await H.sleep(300)
    for (let i = 0; i < 3; i++) { bot.swingArm('right'); await H.sleep(300) }
    const blok = bot.blockAt(bot.entity.position.offset(0, -1, 0))
    if (blok) { try { await bot.activateBlock(blok) } catch {} await H.sleep(500) }
    // 2. F pustą ręką z przedmiotem w drugiej ręce
    await rcon(`minecraft:item replace entity ${nick} weapon.offhand with minecraft:dirt 1`); await H.sleep(600)
    bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 }); await H.sleep(800)
    // 3. kliki w PUSTE sloty ekwipunku (w tym klawisz 9 nad pustym slotem)
    await bot.clickWindow(25, 0, 0); await H.sleep(500)
    await surowy(26, 8, 2); await H.sleep(800)
    const s8 = await rcon(`minecraft:execute if items entity ${nick} hotbar.8 minecraft:book`)
    r.assert(/passed/i.test(s8), 'klawisz 9 nad pustym slotem nie przenosi dziennika', s8.trim())
    await rcon(`say ZNACZNIK_KONIEC ${nick}`)
    r.assert(true, 'zdarzenia wywolane — NPE policz w logu miedzy znacznikami')
  } finally { await H.quit(bot) }
}
