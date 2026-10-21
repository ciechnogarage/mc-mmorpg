'use strict'
/**
 * Przycisk "WYJDZ": jedno klikniecie wyprowadza z kreatora BEZ tworzenia postaci.
 *
 * Do 0.2.0-d5a7bdd wyjscia nie bylo wcale — teleport, komendy i czat sa w kreatorze
 * zablokowane, a zamkniete okno straznik otwiera z powrotem. Ten scenariusz pilnuje
 * calej drogi wyjscia, lacznie z tym, ze zawieszony ProfileCreateEvent zostaje porzucony
 * CZYSTO: drugie wejscie do kreatora po rezygnacji musi dzialac.
 */
const H = require('../lib/harness'); const F = require('../lib/flow')
const SLOT_EXIT = 54

module.exports = async function (r) {
  const bot = await H.connect('EvWyj' + Date.now() % 100000)
  try {
    // Liczba przedmiotow SPRZED wejscia: w kreatorze plecak gracza jest schowany, a w jego
    // slotach siedza ikony menu — mierzenie tam sprawdzaloby liczbe przyciskow, nie zwrot.
    if (!await F.waitProfileScreen(bot)) return r.fail('brak ekranu wyboru postaci')
    const itemsBefore = F.totalItems(bot)

    const w = await F.enterCreator(bot)
    if (!r.assert(!!w, 'kreator otwiera sie po "Create New Character"')) return

    const exitSlot = H.windowSlots(w).find(s => s.slot === SLOT_EXIT)
    r.assert(!!exitSlot && /WYJD/i.test(exitSlot.label || ''), 'przycisk WYJDZ jest w slocie 54',
      exitSlot ? exitSlot.label : 'slot pusty')

    await H.click(bot, SLOT_EXIT)
    await H.sleep(2500)

    r.assert(!F.inRoom(bot), 'gracz opuscil sale kreatora', bot.entity.position.toString())
    r.assert(bot.game.gameMode !== 'spectator', 'tryb gry przywrocony', bot.game.gameMode)
    // ">=", a nie "==": kit startowy dosypuje sie ASYNCHRONICZNIE, czesto juz w trakcie
    // sesji kreatora (sweeper odklada go wtedy do stashu). Rezygnacja ma go ODDAC, wiec
    // gracz moze wyjsc z wieksza liczba przedmiotow niz wszedl — czego nie wolno, to wyjsc
    // z mniejsza.
    r.assert(F.totalItems(bot) >= itemsBefore, 'kreator nie zjadl ekwipunku',
      `po wyjsciu ${F.totalItems(bot)}, przed wejsciem ${itemsBefore}`)

    const back = await H.waitForWindow(bot, { timeout: 12000, titleIncludes: F.PROFILE_TITLE })
    r.assert(!!back, 'po wyjsciu otwiera sie ekran wyboru postaci',
      H.windowTitle(bot.currentWindow))

    // Rezygnacja nie moze zostawic MMOProfiles z niczym zawieszonym — sprawdzamy to
    // jedynym wiarygodnym sposobem: powtornym wejsciem.
    const again = await F.enterCreator(bot)
    r.assert(!!again, 'kreator otwiera sie PONOWNIE po rezygnacji')
    if (again) {
      await H.click(bot, SLOT_EXIT)
      await H.sleep(2000)
    }
  } finally { await H.quit(bot) }
}
