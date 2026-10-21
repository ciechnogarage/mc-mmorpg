'use strict'
/**
 * Skorka postaci: czy po zatwierdzeniu gracz faktycznie DOSTAJE teksture swojej pary
 * rasa x klasa, czy ma ja juz PRZY WEJSCIU (przed ekranem wyboru), czy trzyma sie jej po
 * relogu i czy zmienia sie razem z przelaczeniem postaci.
 *
 * Bot nie renderuje, wiec nie oglada skorki — czyta wartosc wlasnosci 'textures' z pakietu
 * listy graczy, czyli dokladnie to, co serwer wysyla klientowi. Wzorzec bierzemy z
 * skins.yml (montowany do /res przez run.sh), zeby test porownywal z zamierzona skorka,
 * a nie sam ze soba.
 */
const fs = require('fs')
const H = require('../lib/harness'); const F = require('../lib/flow')

const SKINS_YML = '/res/skins.yml'

/** Minimalny czytnik skins.yml — pelnego YAML-a nie ma po co ciagnac do kontenera. */
function expectedSkins() {
  if (!fs.existsSync(SKINS_YML)) return null
  const out = {}
  let current = null
  for (const line of fs.readFileSync(SKINS_YML, 'utf8').split('\n')) {
    const head = line.match(/^ {2}([a-z0-9_]+):\s*$/)
    if (head) { current = head[1]; out[current] = {}; continue }
    const value = line.match(/^ {4}value:\s*'(.+)'\s*$/)
    if (value && current) out[current].value = value[1]
  }
  return out
}

/**
 * Sledzi wlasnosc 'textures' WLASNEGO wpisu gracza w tab-liscie.
 *
 * Paper przy podmianie profilu usuwa i dodaje wpis na nowo, wiec kazda zmiana skorki
 * przychodzi tu jako kolejny 'player_info' z pelnymi wlasnosciami. Nazwy pol roznia sie
 * miedzy wersjami protokolu (key/name), stad oba warianty.
 */
function watchTextures(bot) {
  const byPlayer = {}
  bot._client.on('player_info', (packet) => {
    const rows = packet.data || packet.players || []
    for (const row of Array.isArray(rows) ? rows : [rows]) {
      const player = row.player || row
      const name = player.name || row.name
      const props = player.properties || row.properties
      if (!name || !props) continue
      for (const prop of props) {
        if ((prop.key || prop.name) === 'textures' && prop.value) byPlayer[name] = prop.value
      }
    }
  })
  return {
    of: (who) => byPlayer[who] || null,
    own: () => byPlayer[bot.username] || null,
    seen: () => Object.keys(byPlayer),
  }
}

const shorten = v => (v ? v.slice(0, 24) + '…' : String(v))

module.exports = async function (r) {
  const skins = expectedSkins()
  if (!r.assert(!!skins, 'skins.yml jest widoczny dla evala', 'brak /res/skins.yml')) return
  // Komplet jest osobna asercja, zeby brak JEDNEJ skorki nie blokowal calego scenariusza —
  // wtedy widac i to, ze czegos brakuje, i to, czy reszta mechanizmu dziala.
  r.assert(Object.keys(skins).length >= 16, 'skins.yml ma komplet 4 rasy x 4 klasy',
    Object.keys(skins).length + ' wpisow')
  if (!r.assert(!!(skins.elf_akolita && skins.ork_wojownik),
    'sa skorki uzywane w tym scenariuszu (elf_akolita, ork_wojownik)')) return

  const name = 'EvSkin' + Date.now() % 100000
  let firstCharacter = null

  // --- postac 1: elf + ninja
  let bot = await H.connect(name)
  let textures = watchTextures(bot)
  try {
    const w = await F.enterCreator(bot)
    if (!r.assert(!!w, 'kreator otwiera sie')) return
    await F.completeCreator(bot, { race: 'elf', klass: 'akolita' })
    firstCharacter = (bot.evalLog.find(m => m.includes('Postać utworzona')) || '')
      .replace(/^.*Postać utworzona:\s*/, '').trim()
    r.assert(!!firstCharacter, 'postac utworzona', firstCharacter || 'brak komunikatu')

    // Skorka idzie w tym samym opoznionym zadaniu co model rasy (10 tickow po teleporcie).
    await H.sleep(3000)
    r.assert(textures.own() === skins.elf_akolita.value,
      'po zatwierdzeniu gracz ma skorke elf_akolita',
      'dostal ' + shorten(textures.own()) + ', oczekiwano ' + shorten(skins.elf_akolita.value))
  } finally { await H.quit(bot) }
  if (!firstCharacter) return

  // --- postac 2 na TYM SAMYM koncie: skorka ma isc za postacia, nie za kontem.
  // Ekran wyboru otwiera sie sam po wejsciu, wiec druga postac robimy po relogu —
  // '/mmoprofiles open' wymagaloby permisji OP, ktorej bot nie ma.
  // Chwila przerwy miedzy sesjami: MMOProfiles zapisuje i zwalnia profil asynchronicznie,
  // a wejscie w tej samej sekundzie potrafi trafic w to okno i nie doczekac sie ekranu wyboru.
  await H.sleep(5000)
  bot = await H.connect(name)
  textures = watchTextures(bot)
  try {
    const list = await F.waitProfileScreen(bot, 45000)
    // Skorka ma byc na graczu JUZ TERAZ, przed kliknieciem czegokolwiek: plugin naklada
    // skorke ostatnio granej postaci przy samym wejsciu (skins.apply-on-join). Bez tej
    // asercji regresja tej sciezki jest niewidoczna — dalsze kroki i tak wybieraja postac.
    await H.sleep(2500)
    r.assert(textures.own() === skins.elf_akolita.value,
      'przy wejsciu, PRZED wyborem, gracz ma juz skorke ostatnio granej postaci',
      'dostal ' + shorten(textures.own()) + ', oczekiwano ' + shorten(skins.elf_akolita.value))
    if (!list) {
      // Nie ma ekranu = profil zostal wybrany bez pytania. Druga postac odpada, ale to, co
      // w tym momencie naprawde chcemy wiedziec, sprawdzic mozna: czy skorka wrocila.
      await H.sleep(5000)
      r.assert(textures.own() === skins.elf_akolita.value,
        'bez ekranu wyboru: po relogu skorka wraca sama (druga postac pominieta)',
        'dostal ' + shorten(textures.own()))
    }
    if (r.assert(!!list, 'po relogu wraca ekran wyboru postaci')) {
      const create = H.findSlot(bot.currentWindow, /Create New Character/i)
      if (r.assert(!!create, 'jest slot "Create New Character"')) {
        await H.click(bot, create.slot)
        await H.sleep(2500)
        if (r.assert(F.isCreatorWindow(bot), 'kreator otwiera sie dla drugiej postaci')) {
          await F.completeCreator(bot, { race: 'ork', klass: 'wojownik' })
          // Osobna asercja na SAM FAKT utworzenia: bez niej zgubione klikniecie w kreatorze
          // (zdarza sie) raportowalo sie jako "brak skorki", czyli mylilo o przyczynie.
          const second = bot.evalLog.some(m => m.includes('Postać utworzona'))
          if (r.assert(second, 'druga postac powstala',
            bot.evalLog.slice(-3).join(' / ') || 'brak komunikatow')) {
            await H.sleep(3000)
            r.assert(textures.own() === skins.ork_wojownik.value,
              'druga postac dostaje SWOJA skorke (ork_wojownik)',
              'dostal ' + shorten(textures.own()))
          }
        }
      }
    }
  } finally { await H.quit(bot) }

  // --- powrot na postac 1: skorka ma wrocic razem z nia (relog + przelaczenie profilu)
  await H.sleep(5000)
  bot = await H.connect(name)
  textures = watchTextures(bot)
  try {
    const list = await F.waitProfileScreen(bot, 45000)
    if (r.assert(!!list, 'ekran wyboru postaci po drugim relogu')) {
      // Uwaga: na liscie MMOProfiles widnieje JEGO nazwa profilu ("Profile n1"), a nie imie
      // postaci z kreatora — te dwie nazwy nie sa ze soba zwiazane.
      const slot = H.findSlot(bot.currentWindow, /^\[1\]|Profile n1/i)
      if (r.assert(!!slot, 'pierwsza postac (slot 1) jest na liscie',
        H.windowSlots(bot.currentWindow).map(s => s.label).join(' | '))) {
        await H.click(bot, slot.slot)
        await H.sleep(5000)
        r.assert(textures.own() === skins.elf_akolita.value,
          'wybor pierwszej postaci przywraca JEJ skorke, nie ostatnio uzywana',
          'dostal ' + shorten(textures.own()))
      }
    }
  } finally { await H.quit(bot) }
}
