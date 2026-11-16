'use strict'
/** Przegląd 2026-09-25: stara postać ma WŁASNY przedmiot w slocie 8 / 18 (skrypt wtedy
 *  przycisku nie wstawia). Czy gracz może ten przedmiot normalnie przenieść?
 *  Przygotowanie konsolą: przyciski zamienione na ziemię. */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const { rcon } = require('/h/rcon.js')
module.exports = async function (r) {
  const nick = 'EvCud' + Date.now() % 100000
  const bot = await H.connect(nick)
  try {
    await F.waitProfileScreen(bot); await F.enterCreator(bot)
    await F.completeCreator(bot); await H.sleep(4000)
    await rcon(`minecraft:item replace entity ${nick} hotbar.8 with minecraft:dirt 5`)
    await rcon(`minecraft:item replace entity ${nick} container.18 with minecraft:cobblestone 5`)
    await H.sleep(1200)
    const serwer = async (slot, id) => /passed/i.test(await rcon(`minecraft:execute if items entity ${nick} ${slot} minecraft:${id}`))
    const zamknij = async () => { if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await H.sleep(700) } }

    // 1. shift-klik własnej ziemi ze slotu 8 do plecaka
    await bot.clickWindow(44, 0, 1); await H.sleep(1000); await zamknij()
    r.assert(!(await serwer('hotbar.8', 'dirt')), 'shift-klik wlasnego przedmiotu ze slotu 8 go przenosi')
    // 2. shift-klik własnego bruku ze slotu 18
    await bot.clickWindow(18, 0, 1); await H.sleep(1000); await zamknij()
    r.assert(!(await serwer('container.18', 'cobblestone')), 'shift-klik wlasnego przedmiotu ze slotu 18 go przenosi')
    // 3. klawisz 9 nad pustym slotem plecaka, gdy w slocie 8 jest cudzy przedmiot
    await rcon(`minecraft:item replace entity ${nick} hotbar.8 with minecraft:dirt 5`); await H.sleep(800)
    const wolny = [19, 20, 21, 22, 23, 24, 25, 26].find(i => !bot.inventory.slots[i])
    await bot.clickWindow(wolny, 8, 2); await H.sleep(1000); await zamknij()
    r.assert(!(await serwer('hotbar.8', 'dirt')), 'klawisz 9 przenosi wlasny przedmiot ze slotu 8', `slot docelowy ${wolny}`)
    // 4. po zwolnieniu slotów przyciski nadal wracają przy odrodzeniu
    await rcon(`minecraft:item replace entity ${nick} hotbar.8 with minecraft:air`)
    await rcon(`minecraft:item replace entity ${nick} container.18 with minecraft:air`)
    await rcon(`minecraft:kill ${nick}`); await H.sleep(1500)
    try { bot.respawn() } catch {}
    await H.sleep(4000)
    r.assert(await serwer('hotbar.8', 'book') && await serwer('container.18', 'leather'), 'po odrodzeniu przyciski wstawione w wolne sloty')
    // 5. przycisk nadal zablokowany (regresja poprawki)
    await bot.clickWindow(44, 0, 1); await H.sleep(1000); await zamknij()
    await bot.clickWindow(18, 0, 1); await H.sleep(1000); await zamknij()
    await bot.clickWindow(21, 8, 2); await H.sleep(1000); await zamknij()
    r.assert(await serwer('hotbar.8', 'book') && await serwer('container.18', 'leather'), 'przyciski nadal nie daja sie ruszyc (shift-klik, klawisz 9)')
  } finally { await H.quit(bot) }
}
