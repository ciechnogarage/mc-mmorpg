'use strict'
/**
 * Stajnia podgladu: zmiana rasy/klasy NIE tworzy u klienta nowej encji.
 *
 * To jest regresja z gry, zglaszana trzy razy pod rzad jako "na chwile pojawia sie jakis
 * randomowy model". Przyczyna nie byla ani w modelach, ani w pobieraniu tekstur: kazde
 * klikniecie kasowalo encje podgladu i tworzylo nowa, a klient do czasu rozwiazania skorki
 * rysuje DOMYSLNA, inna dla kazdego UUID profilu. Dlatego bot pilnuje tu mechanizmu,
 * a nie wygladu: po wejsciu ma stanac komplet nosicieli (po jednym na skorke) i od tego
 * momentu zadne klikniecie nie ma prawa wywolac spawn_entity ani entity_destroy.
 */
const H = require('../lib/harness'); const F = require('../lib/flow')
const PLAYER_TYPE = 155   // id encji 'player' w 1.21.11 — sprawdzone w minecraft-data

module.exports = async function (r) {
  const bot = await H.connect('EvStaj' + Date.now() % 100000)
  const spawnedAtEntry = new Set()
  let spawnsAfter = 0
  let destroysAfter = 0
  let watching = false
  try {
    bot._client.on('spawn_entity', p => {
      if ((p.type ?? p.entityType) !== PLAYER_TYPE) return
      if (watching) spawnsAfter++; else spawnedAtEntry.add(p.entityId)
    })
    bot._client.on('entity_destroy', p => { if (watching) destroysAfter += (p.entityIds || []).length })

    if (!await F.waitProfileScreen(bot)) return r.fail('brak ekranu wyboru postaci')
    await H.click(bot, F.SLOT_CREATE_PROFILE)
    await H.sleep(3000)

    // 16 skorek + nosiciel awaryjny (para bez skorki). ">= 16", zeby test nie pekal przy
    // dolozeniu rasy albo klasy do configu.
    r.assert(spawnedAtEntry.size >= 16, 'po wejsciu stoi komplet nosicieli',
      `encji typu player: ${spawnedAtEntry.size}`)

    watching = true
    for (const slot of [F.RACE_SLOTS.ork, F.CLASS_SLOTS.ninja, F.RACE_SLOTS.elf,
                        F.CLASS_SLOTS.mag, F.RACE_SLOTS.nieumarly]) {
      await H.click(bot, slot)
      await H.sleep(600)
    }
    r.assert(spawnsAfter === 0, 'zmiana rasy/klasy nie tworzy nowej encji podgladu',
      `spawnow po wejsciu: ${spawnsAfter}`)
    r.assert(destroysAfter === 0, 'zmiana rasy/klasy nie kasuje encji podgladu',
      `destroy po wejsciu: ${destroysAfter}`)
    r.assert(F.isCreatorWindow(bot), 'kreator dziala po serii przelaczen',
      H.windowTitle(bot.currentWindow))
  } finally { await H.quit(bot) }
}
