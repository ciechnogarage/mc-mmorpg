/**
 * Spacja dziala RAZ (scalenie a48b556: auto-merge wolal onJump dwa razy).
 *  A) kreator (nowy gracz): jedno wcisniecie -> DOKLADNIE jedno okno "Wszystkie klasy", ktore zostaje otwarte
 *     (zadne drugie okno ani zamkniecie, zadna ksiega "Nowa postac"), gracz dalej w scenie kreatora.
 *  B) scena wyboru (WyborQA): jedno wcisniecie -> DOKLADNIE jeden wybor profilu. Licznik = dzwiek
 *     entity.player.levelup, ktory MMOProfiles gra przy KAZDYM wyborze (skrypt profile-select po pakiecie),
 *     plus log serwera: jedna para "Profile Unloading"/zadna podwojna zmiana.
 */
const w = require('./lib/w')
const R = new w.Results('spacja')
const nbt = require('prismarine-nbt')
const title = p => { try { return w.plain(nbt.simplify(p.dialog.data).title) } catch { return '?' } }
;(async () => {
  // --- A: kreator ---
  const nowy = 'Spacja' + (Date.now() % 10000)
  let bot = await w.connect(nowy)
  const dl = []
  bot._client.on('show_dialog', p => dl.push({ t: Date.now(), title: title(p) }))
  let cleared = 0
  bot._client.on('clear_dialog', () => cleared++)
  await w.sleep(7000)
  R.assert(bot.game.gameMode === 'spectator', 'A: nowy gracz w scenie kreatora')
  const przed = dl.length
  await w.press(bot, 'jump')
  await w.sleep(3000)
  const nowe = dl.slice(przed)
  R.assert(nowe.length === 1 && /Wszystkie klasy/.test(nowe[0].title), 'A: jedna Spacja -> dokladnie JEDNO okno "Wszystkie klasy"', nowe.map(x => x.title).join(' | '))
  R.assert(cleared === 0 && bot.dialog && /Wszystkie klasy/.test(w.plain(bot.dialog.title)), 'A: okno zostaje otwarte (nie zamkniete, nie podmienione)')
  R.assert(!nowe.some(x => /Nowa postać/.test(x.title)) && bot.game.gameMode === 'spectator', 'A: zadnej drugiej akcji (ksiega/wyjscie) — dalej kreator')
  await w.quit(bot)
  await w.sleep(5000)

  // --- B: scena wyboru ---
  bot = await w.connect('WyborQA')
  // Dzwiek wyboru profilu (MMOProfiles profile-select: sound ENTITY_PLAYER_LEVELUP) przychodzi jako
  // sound_effect z id rejestru 1256, kategoria master — mineflayer 4.39 zle go nazywa (stara tabela 1.21.11),
  // wiec liczymy po id pakietu.
  const lvl = []
  bot._client.on('sound_effect', p => { if (p.sound && p.sound.soundId === 1256 && p.soundCategory === 'master') lvl.push(Date.now()) })
  await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 12000)
  await w.click(bot, /Wróć do sceny/)
  await w.sleep(2000)
  const off = w.logOffset()
  const lv0 = lvl.length
  await w.press(bot, 'jump')
  await w.sleep(7000)
  const log = w.logSince(off)
  R.assert(bot.game.gameMode !== 'spectator', 'B: Spacja w scenie wyboru -> gra')
  R.assert(lvl.length - lv0 === 1, 'B: DOKLADNIE jeden wybor profilu (dzwiek wyboru MMOProfiles x1)', String(lvl.length - lv0))
  const zalad = (log.match(/Session validated for/g) || []).length
  R.assert(zalad === 1, 'B: log serwera: profil wczytany RAZ (CoreTools "Session validated" x1)', String(zalad))
  const scen = (log.match(/Scena otwarta: WyborQA/g) || []).length
  R.assert(scen === 0, 'B: scena nie otwiera sie ponownie po Spacji', String(scen))
  console.log('DIAG B log: Session validated x' + zalad)
  await w.quit(bot)
  await w.sleep(6000)

  // --- C: LPM "Graj" z bronia ostatniej postaci w rece — bez komunikatu MMOItems o bieglosci ---
  bot = await w.connect('WyborQA')
  await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 12000)
  await w.click(bot, /Wróć do sceny/); await w.sleep(1500)
  const n0 = bot.log.length
  await w.lpm(bot); await w.sleep(5000)
  const bieg = bot.log.slice(n0).filter(l => /Biegłość/.test(l) && !l.startsWith('[AB]'))
  R.assert(bot.game.gameMode !== 'spectator' && bieg.length === 0, 'C: LPM "Graj" -> gra, bez "Nie mozesz tego uzyc — brakuje: Bieglosc"', bieg.join(' | '))
  await w.quit(bot)
  R.print(); console.log(`WYNIK: ${R.rows.length - R.failed}/${R.rows.length} PASS`); process.exit(R.failed)
})().catch(e => { R.print(); console.error('ERR', e); process.exit(99) })
