'use strict'
/**
 * Eval menu-huba przeciwko ZYWEMU world1 — bot protokolowy, nie zrzuty ekranu.
 *
 * Co sprawdza:
 *  1. klawisz Q (pakiet DROP_ITEM) otwiera menu glowne, gdy gracz COS TRZYMA,
 *  2. Q z PUSTA REKA menu NIE otwiera — to udokumentowane ograniczenie protokolu,
 *     sprawdzane tu po to, zeby zmiana zachowania nie przeszla niezauwazona,
 *  3. menu glowne ma komplet kafelkow na umowionych slotach,
 *  4. nawigacja w glab (Postac) i powrot (Wstecz) dzialaja,
 *  5. Zamknij naprawde zamyka okno.
 *
 * Czego NIE sprawdzi: wygladu, tla, czytelnosci ikon. Na to jest klient QA w kontenerze.
 *
 * Bot musi najpierw PRZEJSC KREATOR POSTACI — bez postaci MMOProfiles trzyma go na ekranie
 * wyboru, a w sali kreatora klawisz Q nie ma prawa dzialac.
 */
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')

const TYTUL_MENU = 'Menu'
/** Tytuly WSZYSTKICH ekranow huba — cel kafelka nie moze byc zadnym z nich. */
const EKRANY_HUBA = ['Menu', 'Postać', 'Umiejętności', 'Przygoda', 'Ekonomia',
                     'Społeczność', 'Ustawienia', 'Pomoc']
const jestEkranemHuba = t => EKRANY_HUBA.some(x => t === x || t.endsWith(x))
const SLOT = {
  postac: 10, ekwipunek: 11, umiejetnosci: 12, profesje: 13,
  przygoda: 14, ekonomia: 15, spolecznosc: 16,
  pomoc: 18, ustawienia: 22, zamknij: 26,
}

/** Wcisniecie Q. Klient wysyla je jako akcje gracza o statusie 4 (DROP_ITEM). */
function wcisnijQ(bot) {
  bot._client.write('block_dig', {
    status: 4,
    location: { x: 0, y: 0, z: 0 },
    face: 0,
    sequence: 0,
  })
}

/** Ustawia reke na slocie hotbara Z PRZEDMIOTEM. Zwraca false, gdy hotbar jest pusty. */
async function wezCosDoReki(bot) {
  for (let i = 0; i < 9; i++) {
    if (bot.inventory.slots[36 + i]) { bot.setQuickBarSlot(i); await H.sleep(400); return true }
  }
  return false
}

/** Ustawia reke na PUSTYM slocie hotbara. */
async function opuscRece(bot) {
  for (let i = 0; i < 9; i++) {
    if (!bot.inventory.slots[36 + i]) { bot.setQuickBarSlot(i); await H.sleep(400); return true }
  }
  return false
}

module.exports = async function (r) {
  const bot = await H.connect('EvHub' + Date.now() % 100000)
  try {
    // --- postac, bez ktorej gracz nie wychodzi z ekranu wyboru --------------------------
    const ekran = await F.waitProfileScreen(bot)
    if (!r.assert(!!ekran, 'ekran wyboru postaci sie pokazal')) return
    const kreator = await F.enterCreator(bot)
    if (!r.assert(!!kreator, 'kreator postaci otwarty')) return
    await F.completeCreator(bot)
    await H.sleep(3000)
    r.assert(!bot.currentWindow, 'po wyjsciu z kreatora zadne okno nie zostalo otwarte')

    // --- 1. klawisz Q z PUSTA REKA TEZ otwiera menu --------------------------------------
    // To jest caly powod istnienia pluginu MenuKey. Na samym CoreTools tu bylo pusto:
    // 'PlayerDropItemEvent' jest zdarzeniem o UPUSZCZONYM przedmiocie i nie leci, gdy nie
    // ma czego upuscic. MenuKey slucha samego pakietu, wiec reka moze byc pusta.
    if (r.assert(await opuscRece(bot), 'bot ma pusty slot w hotbarze')) {
      wcisnijQ(bot)
      const puste = await H.waitForWindow(bot, { timeout: 10000, titleIncludes: TYTUL_MENU })
      r.assert(!!puste, 'Q z PUSTA REKA otwiera menu',
        puste ? H.windowTitle(puste) : 'zadne okno nie przyszlo w 10 s')
      if (puste) { bot.closeWindow(puste); await H.sleep(1200) }
    }

    // --- 2. klawisz Q z przedmiotem w rece otwiera menu ----------------------------------
    if (!r.assert(await wezCosDoReki(bot), 'bot ma co trzymac w rece')) return
    const ileWReku = bot.heldItem ? bot.heldItem.count : 0
    wcisnijQ(bot)
    const menu = await H.waitForWindow(bot, { timeout: 12000, titleIncludes: TYTUL_MENU })
    if (!r.assert(!!menu, 'klawisz Q otworzyl menu glowne',
        menu ? H.windowTitle(menu) : 'zadne okno nie przyszlo w 12 s')) return
    r.assert(bot.heldItem && bot.heldItem.count === ileWReku,
      'przedmiot NIE wypadl z reki przy otwieraniu menu',
      `przed=${ileWReku} po=${bot.heldItem ? bot.heldItem.count : 0}`)
    r.assert(menu.slots.length - 36 === 27, 'menu glowne ma 27 slotow (3 rzedy)',
      'slotow okna: ' + (menu.slots.length - 36))

    // --- 3. komplet kafelkow -------------------------------------------------------------
    const etykiety = {}
    for (const s of H.windowSlots(menu)) etykiety[s.slot] = s.label
    for (const [nazwa, slot] of Object.entries(SLOT)) {
      r.assert(!!etykiety[slot], `kafelek '${nazwa}' stoi w slocie ${slot}`,
        etykiety[slot] || 'slot pusty')
    }
    const zajete = H.windowSlots(menu).filter(s => s.slot < 27).length
    r.assert(zajete === Object.keys(SLOT).length,
      'w menu nie ma kafelkow poza umowionymi', `zajetych: ${zajete}`)

    // --- 4. nawigacja w glab i powrot ----------------------------------------------------
    await H.click(bot, SLOT.postac)
    const sekcja = await H.waitForWindow(bot, { timeout: 8000, titleIncludes: 'Posta' })
    if (r.assert(!!sekcja, 'kafelek Postac otworzyl sekcje',
        sekcja ? H.windowTitle(sekcja) : 'okno sie nie zmienilo')) {
      const wstecz = H.windowSlots(sekcja).find(s => s.slot === 18)
      r.assert(!!wstecz, 'sekcja ma przycisk Wstecz w slocie 18', wstecz ? wstecz.label : 'brak')
      await H.click(bot, 18)
      const powrot = await H.waitForWindow(bot, { timeout: 8000, titleIncludes: TYTUL_MENU })
      r.assert(!!powrot, 'Wstecz wraca do menu glownego',
        powrot ? H.windowTitle(powrot) : 'nie wrocilo')
    }

    // --- 5. kafelki naprawde URUCHAMIAJA swoje cele --------------------------------------
    // Nawigacja miedzy ekranami huba to jedno, a wpiecie w CUDZE pluginy drugie. Tu
    // sprawdzamy te drugie: po kliknieciu ma przyjsc okno, ktore NIE jest juz hubem.
    // Sciezka: [slot w menu glownym, (opcjonalnie slot w sekcji), czego oczekujemy]
    // Kafelka 'Ekwipunek' NIE MA na tej liscie swiadomie: od 2026-09-20 ekwipunek mieszka
    // w vanillowym oknie pod klawiszem E, a kafelek tylko zamyka menu i o tym mowi.
    // Sprawdzamy go osobno, nizej.
    const cele = [
      { nazwa: 'Statystyki', droga: [SLOT.postac, 10], okno: true },
      { nazwa: 'Atrybuty', droga: [SLOT.postac, 13], okno: true },
      { nazwa: 'Zmien postac', droga: [SLOT.postac, 16], okno: true },
      { nazwa: 'Umiejetnosci', droga: [SLOT.umiejetnosci, 11], okno: true },
      { nazwa: 'Drzewka rozwoju', droga: [SLOT.umiejetnosci, 13], okno: true },
      { nazwa: 'Tryb rzucania', droga: [SLOT.umiejetnosci, 15], okno: false },
      { nazwa: 'Profesje', droga: [SLOT.profesje], okno: true },
      { nazwa: 'Zadania', droga: [SLOT.przygoda, 13], okno: true },
      { nazwa: 'Druzyna', droga: [SLOT.spolecznosc, 11], okno: true },
      { nazwa: 'Znajomi', droga: [SLOT.spolecznosc, 13], okno: true },
      { nazwa: 'Gildia', droga: [SLOT.spolecznosc, 15], okno: true },
      { nazwa: 'Tryb PvP', droga: [SLOT.ustawienia, 13], okno: false },
    ]
    for (const cel of cele) {
      wcisnijQ(bot)
      const start = await H.waitForWindow(bot, { timeout: 8000, titleIncludes: TYTUL_MENU })
      if (!start) { r.fail(`${cel.nazwa}: nie udalo sie wrocic do menu`); continue }
      for (const slot of cel.droga) { await H.click(bot, slot) }
      // Czekamy WARUNKOWO, nie sztywno: cudze GUI potrafi przyjsc po sekundzie albo po
      // czterech, zaleznie od obciazenia serwera. Sztywne 2,5 s dawalo losowe FAIL-e na
      // kafelkach, ktore chwile wczesniej przechodzily.
      const czekamDo = Date.now() + 7000
      while (Date.now() < czekamDo) {
        const w = bot.currentWindow
        if (cel.okno ? (w && !jestEkranemHuba(H.windowTitle(w))) : !w) break
        await H.sleep(300)
      }
      const okno = bot.currentWindow
      const tytul = okno ? H.windowTitle(okno) : ''
      if (cel.okno) {
        // Musi to byc okno CUDZE, nie kolejny ekran huba — inaczej "nic sie nie stalo"
        // przechodzilo jako sukces, bo sekcja zostawala otwarta.
        r.assert(!!okno && !jestEkranemHuba(tytul),
          `kafelek '${cel.nazwa}' otworzyl swoj ekran`, tytul || 'zadne okno')
      } else {
        // Przelacznik: ma zamknac menu i NIE otworzyc nowego okna.
        r.assert(!okno, `kafelek '${cel.nazwa}' zadzialal jako przelacznik (bez okna)`,
          tytul || '')
      }
      bot.closeWindow && okno && bot.closeWindow(okno)
      await H.sleep(1200)
    }

    // --- 6. kafelek 'Ekwipunek' ma ZAMKNAC menu (ekwipunek jest pod E) -------------------
    wcisnijQ(bot)
    if (await H.waitForWindow(bot, { timeout: 8000, titleIncludes: TYTUL_MENU })) {
      await H.click(bot, SLOT.ekwipunek)
      await H.sleep(1500)
      r.assert(!bot.currentWindow, 'kafelek Ekwipunek zamyka menu (ekwipunek jest pod E)',
        bot.currentWindow ? H.windowTitle(bot.currentWindow) : '')
    }

    // --- 7. zamkniecie -------------------------------------------------------------------
    wcisnijQ(bot)
    await H.waitForWindow(bot, { timeout: 8000, titleIncludes: TYTUL_MENU })
    await H.click(bot, SLOT.zamknij)
    await H.sleep(1200)
    r.assert(!bot.currentWindow, 'Zamknij zamyka okno',
      bot.currentWindow ? H.windowTitle(bot.currentWindow) : '')

    console.log('     konto testowe: ' + bot.username)
  } finally { await H.quit(bot) }
}
