/**
 * Eval wyboru postaci (agent WYBOR). Konto WyborQA (bez OP, rank.mvp = 12 miejsc, 11 postaci z setup.js).
 * Kazdy krok to to, co robi gracz: PPM, klik w oknie, A/D, LPM; konsola klonu tylko do OBSERWACJI
 * (papi parse, pliki danych) i do "menu Q" (DeluxeMenus woła dokladnie 'cc wybor <gracz> lista' z konsoli).
 */
const fs = require('fs')
const w = require('./lib/w')
const R = new w.Results('wybor')
const NAME = 'WyborQA'
const DATA = '/clone-data'
const trashFile = () => { try { return fs.readFileSync(DATA + '/plugins/CharacterCreator/trash.yml', 'utf8') } catch { return '' } }
const trashCount = () => (trashFile().match(/^  [0-9a-f-]{36}:/mg) || []).length
const papi = async ph => (await w.rcon(`papi parse ${NAME} ${ph}`)).replace(/§./g, '').trim()
const tiles = bot => w.buttons(bot).map(b => b.label).filter(l => !/Nowa postać|Brak wolnych|Zarządzaj|Wróć|Zamknij|^Kosz \(/.test(l))
const tileName = l => l.replace(/^[★▶] /, '').split('  ')[0]
const tooltips = bot => (bot.dialog && bot.dialog.actions || []).map(a => w.plain(a.tooltip))
const hint = bot => { const ab = bot.log.filter(l => l.startsWith('[AB] ') && /\[(LPM|A\/D)/.test(l)); return ab.length ? ab[ab.length - 1] : '' }

async function enterScene(bot) {
  // Po wejsciu: 11 postaci >= lista-od (6) — lista otwiera sie sama.
  return w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 12000)
}
async function openList(bot) {
  bot.dialog = null
  for (let t = 0; t < 4; t++) {
    await w.ppm(bot)
    const d = await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 3000)
    if (d) return d
  }
  return null
}
async function waitPlaying(bot, ms = 15000) {
  const end = Date.now() + ms
  while (Date.now() < end) { if (bot.game.gameMode !== 'spectator') return true; await w.sleep(300) }
  return false
}
async function reconnect(bot) { await w.quit(bot); await w.sleep(5000); const b = await w.connect(NAME); b.on('message', m => CZAT.push(m.toString())); return b }
const CZAT = []
const START = w.logOffset()

;(async () => {
  const only = process.env.SCEN || ''
  let bot = await w.connect(NAME)
  const czat = CZAT
  const zbieraj = b => b.on('message', m => CZAT.push(m.toString()))
  zbieraj(bot)

  // --- 1. Po wejsciu: lista sama, stala kolejnosc, gwiazdka przy ostatnio granej ---------------------
  let d = await enterScene(bot)
  R.assert(!!d, 'po wejsciu lista otwiera sie sama (11 postaci >= lista-od 6)')
  const order1 = tiles(bot).map(tileName)
  R.assert(order1.length === 11, 'lista ma 11 kafelkow', order1.join(','))
  R.assert(/11\/12/.test(w.plain(bot.dialog && bot.dialog.title)), 'tytul pokazuje zajete/wszystkie miejsca', w.plain(bot.dialog && bot.dialog.title))
  const starred = tiles(bot).filter(l => l.startsWith('★ ')).map(tileName)
  R.assert(starred.length === 1, 'dokladnie jedna gwiazdka (ostatnio grana)', starred.join(','))
  const tt = tooltips(bot).join('\n')
  R.assert(/Poziom: 12/.test(tt) && /Czas gry:/.test(tt) && /Ostatnio:/.test(tt), 'karta w podpowiedzi: poziom, ostatnio, czas gry')
  R.assert(/Miejsce: /.test(tt), 'karta pokazuje miejsce', (tt.match(/Miejsce: .*/g) || []).slice(0, 3).join(' | '))

  // --- 2. Klik w kafelek = graj (jeden klik) ---------------------------------------------------------
  await w.click(bot, /^(★ )?Cedric /)
  R.assert(await waitPlaying(bot), 'klik "Cedric" w liscie -> gra (bez dodatkowych krokow)')
  await w.sleep(2500)
  R.assert(await papi('%mmocore_level%') === '4', 'gra postacia Cedric (poziom MMOCore 4)', await papi('%mmocore_level%'))

  // --- 3. Relog: ta sama kolejnosc, gwiazdka przeniesiona na Cedrica, ale Cedric NIE przeskakuje -----
  bot = await reconnect(bot)
  d = await enterScene(bot)
  const order2 = tiles(bot).map(tileName)
  R.assert(JSON.stringify(order1) === JSON.stringify(order2), 'po relogu kolejnosc identyczna', order2.join(','))
  R.assert(tiles(bot).some(l => l.startsWith('★ Cedric')), 'gwiazdka przy Cedricu (ostatnio grana)')
  // Scena pod lista stoi na Cedricu: podpowiedz mowi "Postac 5/11" (Cedric jest 5. w stalej kolejnosci).
  await w.click(bot, /Wróć do sceny/)
  await w.sleep(2500)
  const ci = order1.indexOf('Cedric') + 1
  R.assert(new RegExp('Postać ' + ci + '/11').test(hint(bot)), 'scena zaznacza ostatnio grana (Postac ' + ci + '/11)', hint(bot))
  R.assert(/LPM\/Spacja\] Graj/.test(hint(bot)) && /PPM\] Lista i zarządzanie/.test(hint(bot)), 'podpowiedz mowi o spacji i liscie', hint(bot))

  // --- 4. A/D przeglada; spacja = graj ----------------------------------------------------------------
  await w.press(bot, 'right')
  await w.sleep(1600)
  R.assert(new RegExp('Postać ' + (ci + 1) + '/11').test(hint(bot)), 'D -> nastepna postac', hint(bot))
  await w.press(bot, 'left')
  await w.sleep(1600)
  R.assert(new RegExp('Postać ' + ci + '/11').test(hint(bot)), 'A -> z powrotem', hint(bot))
  await w.press(bot, 'jump')
  R.assert(await waitPlaying(bot), 'spacja w scenie = graj zaznaczona postacia')
  await w.sleep(2000)

  // --- 5. Menu Q: lista nad gra, zmiana postaci bez sceny ---------------------------------------------
  let sawSpectator = false
  const gm = () => { if (bot.game.gameMode === 'spectator') sawSpectator = true }
  bot.on('game', gm)
  bot.dialog = null
  await w.rcon(`cc wybor ${NAME} lista`)
  d = await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 5000)
  R.assert(!!d, 'menu Q (cc wybor <g> lista) otwiera liste nad gra')
  R.assert(tiles(bot).some(l => l.startsWith('▶ Cedric')), 'lista w grze oznacza postac, ktora grasz (▶)')
  await w.click(bot, /^Aldor /)
  await w.sleep(4000)
  R.assert(await papi('%mmocore_level%') === '12', 'klik "Aldor" w liscie z gry -> gra Aldorem (poziom 12)', await papi('%mmocore_level%'))
  R.assert(!sawSpectator && bot.game.gameMode !== 'spectator', 'zmiana z gry bez sceny (ani razu spectator)')
  R.assert(bot.log.some(l => /Grasz teraz postacią Aldor\./.test(l)), 'czat: "Grasz teraz postacia Aldor."')
  bot.removeListener('game', gm)
  // Klik w postac, ktora juz grasz — nic sie nie dzieje.
  bot.dialog = null
  await w.rcon(`cc wybor ${NAME} lista`)
  await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 5000)
  const clicked = await w.click(bot, /^▶ Aldor/)
  await w.sleep(1500)
  console.log('DBG klik ▶ Aldor', clicked, bot.log.filter(l => /grasz/.test(l)))
  R.assert(bot.log.some(l => /Już grasz postacią Aldor/.test(l)), 'klik w postac, ktora grasz: "Juz grasz postacia Aldor"')

  // --- 6. Zarzadzanie: siatka przelacznikow, 3 naraz do kosza, imie od poziomu 10 ---------------------
  bot.dialog = null
  await w.rcon(`cc wybor ${NAME} lista`)
  await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 5000)
  await w.click(bot, /Zarządzaj postaciami/)
  d = await w.waitDialog(bot, d => /Zarządzaj postaciami/.test(w.plain(d.title)), 15000)
  R.assert(!!d, '"Zarzadzaj postaciami" z gry -> scena z oknem zarzadzania')
  const btn = () => w.buttons(bot).map(b => b.label)
  R.assert(w.inputs(bot.dialog).length === 0, 'bez pol wyboru Papera (zygzak) — same przyciski')
  R.assert(/^Usuń zaznaczone \(0\)/.test(btn()[0]) && /^Kosz \(0\)/.test(btn()[1]), 'akcje w PIERWSZYM rzedzie (widoczne bez przewijania)', btn().slice(0, 2).join(' | '))
  const toggles = () => btn().filter(l => /^[☐☑] /.test(l))
  R.assert(toggles().length === 11 && toggles().every(l => l.startsWith('☐ ')), '11 przelacznikow ☐', toggles().join(' | '))
  const hintBefore = hint(bot)
  for (const n of ['Brakk', 'Morth', 'Elwen']) {
    await w.click(bot, new RegExp('^☐ ' + n + ' '))
    await w.waitDialog(bot, d => /Zarządzaj postaciami/.test(w.plain(d.title)), 5000)
  }
  R.assert(toggles().filter(l => l.startsWith('☑ ')).map(tileName).map(l => l.slice(2)).join(',') === 'Brakk,Elwen,Morth'
    || toggles().filter(l => l.startsWith('☑ ')).length === 3, 'klik przelacza: 3 zaznaczone ☑', toggles().filter(l => l.startsWith('☑')).join(' | '))
  R.assert(/^Usuń zaznaczone \(3\)/.test(btn()[0]), 'licznik w akcji: "Usun zaznaczone (3)"', btn()[0])
  await w.click(bot, /^Usuń zaznaczone/)
  d = await w.waitDialog(bot, d => /Przenieść do kosza/.test(w.plain(d.title)), 5000)
  R.assert(!!d, 'potwierdzenie przeniesienia do kosza')
  const cin = w.inputs(bot.dialog)
  R.assert(cin.length === 1 && /Elwen/.test(cin[0].label), 'pole na imie tylko dla postaci >= 10 poz. (Elwen 15)', cin.map(i => i.label).join('|'))
  await w.click(bot, /Przenieś do kosza/, { p0: 'Elwe' })
  d = await w.waitDialog(bot, d => /Przenieść do kosza/.test(w.plain(d.title)), 5000)
  R.assert(!!d && /się nie zgadza/.test(w.text(bot)), 'zle imie -> odmowa, okno wraca z bledem')
  R.assert(trashCount() === 0, 'zle imie -> kosz pusty', String(trashCount()))
  await w.click(bot, /Przenieś do kosza/, { p0: 'elwen' })
  d = await w.waitDialog(bot, d => /Zarządzaj postaciami/.test(w.plain(d.title)), 5000)
  R.assert(!!d && /W koszu: .*Brakk/.test(w.text(bot)), 'po usunieciu wraca okno zarzadzania z komunikatem', w.text(bot).slice(0, 160))
  R.assert(trashCount() === 3, 'trash.yml ma 3 wpisy', String(trashCount()))
  R.assert(toggles().length === 8 && toggles().every(l => l.startsWith('☐ ')), 'zostalo 8 postaci, zaznaczenie wyczyszczone')
  await w.sleep(1500)
  R.assert(hint(bot) !== '' && /Postać 1\/8/.test(hint(bot)), 'scena zostaje na tej samej postaci (Aldor 1/8) — bez resetu', hint(bot) + ' (przed: ' + hintBefore + ')')
  R.assert(/^Kosz \(3\)/.test(btn()[1]), 'przycisk "Kosz (3)"')

  // --- 7. Kosz: przywroc jedna, usun na zawsze druga ---------------------------------------------------
  await w.click(bot, /^Kosz \(3\)/)
  d = await w.waitDialog(bot, d => /^Kosz/.test(w.plain(d.title)), 5000)
  R.assert(!!d && /zajmują miejsca/.test(w.text(bot)), 'kosz mowi, ze postacie zajmuja miejsca')
  R.assert(/^Przywróć zaznaczone \(0\)/.test(btn()[0]) && /^Usuń na zawsze \(0\)/.test(btn()[1]), 'kosz: akcje w pierwszym rzedzie', btn().slice(0, 2).join(' | '))
  R.assert(toggles().length === 3 && toggles().every(l => /· 7 dni$/.test(l)), 'kazda postac w koszu: "· 7 dni"', toggles().join(' | '))
  await w.click(bot, /^☐ Brakk /)
  await w.waitDialog(bot, d => /^Kosz/.test(w.plain(d.title)), 5000)
  await w.click(bot, /^Przywróć zaznaczone \(1\)/)
  d = await w.waitDialog(bot, d => /^Kosz/.test(w.plain(d.title)), 5000)
  R.assert(!!d && /Przywrócono: Brakk/.test(w.text(bot)), 'przywrocono Brakka')
  R.assert(trashCount() === 2, 'trash.yml: 2 wpisy', String(trashCount()))
  const off = w.logOffset()
  await w.click(bot, /^☐ Morth /)
  await w.waitDialog(bot, d => /^Kosz/.test(w.plain(d.title)), 5000)
  await w.click(bot, /^Usuń na zawsze \(1\)/)
  d = await w.waitDialog(bot, d => /Usunąć na zawsze/.test(w.plain(d.title)), 5000)
  R.assert(!!d && /NA ZAWSZE/.test(w.text(bot)) && w.inputs(bot.dialog).length === 0, 'potwierdzenie na zawsze (Morth poz. 1 — bez imienia)')
  await w.click(bot, /^Usuń na zawsze$/)
  d = await w.waitDialog(bot, d => /^Kosz/.test(w.plain(d.title)), 5000)
  R.assert(!!d && /Usunięto na zawsze: Morth/.test(w.text(bot)), 'Morth usuniety na zawsze, zostaje okno kosza', w.text(bot).slice(0, 160))
  R.assert(trashCount() === 1, 'trash.yml: 1 wpis (Elwen)', String(trashCount()))
  await w.sleep(7000)
  const logDel = w.logSince(off)
  R.assert(/usuwa NA ZAWSZE postac Morth/.test(logDel), 'log: usuniecie na zawsze')
  const hang = (logDel.match(/future nie domkniety.*/) || [''])[0]
  console.log('DIAG removeProfile:', hang || 'future domkniety w 5 s')
  const chars = fs.readFileSync(DATA + '/plugins/CharacterCreator/characters.yml', 'utf8')
  R.assert(!/name: Morth/.test(chars), 'rekord Mortha zniknal z characters.yml (po 7 s — zapis z debounce)')
  // Pliki modulow usunietej postaci: przeniesione do archiwum, nie zostaja w userdata.
  const mid = (logDel.match(/postac Morth \(profil ([0-9a-f-]{36})\)/) || [])[1]
  const plik = rel => fs.existsSync(DATA + '/plugins/' + rel.replaceAll('{id}', mid))
  const szablony = ['CoreTools/userdata/{id}.yml', 'MMOCore/userdata/{id}.yml', 'MMOItems/userdata/{id}.yml', 'MMOInventory/userdata/{id}.yml']
  R.assert(!!mid && szablony.every(t => !plik(t)), 'po usunieciu: brak plikow Mortha w CoreTools/MMOCore/MMOItems/MMOInventory userdata', mid)
  const wArch = szablony.filter(t => plik('CharacterCreator/usuniete/{id}/' + t))
  R.assert(wArch.length >= 1 && wArch.some(t => t.startsWith('CoreTools')), 'pliki w CharacterCreator/usuniete/<id>/ (w tym CoreTools)', wArch.join(', '))
  console.log('ARCH log:', (logDel.match(/pliki modulow przeniesione.*/) || [''])[0])

  // --- 8. Relog: Brakk wrocil na swoje miejsce, Morth i Elwen nie na liscie ---------------------------
  await w.click(bot, /Wróć/)
  bot = await reconnect(bot)
  await enterScene(bot)
  const order3 = tiles(bot).map(tileName)
  const expect = order1.filter(n => n !== 'Morth' && n !== 'Elwen')
  R.assert(JSON.stringify(order3) === JSON.stringify(expect), 'po relogu: Brakk na swoim miejscu, bez Mortha i Elwen', order3.join(','))
  R.assert(/10\/12/.test(w.plain(bot.dialog && bot.dialog.title)), 'miejsca: 10/12 (Elwen w koszu nadal liczony)', w.plain(bot.dialog && bot.dialog.title))
  R.assert(/W koszu: 1 postać/.test(w.text(bot)), 'lista mowi o koszu', w.text(bot))

  await w.click(bot, /^(★ )?Aldor /)
  await waitPlaying(bot)
  await w.quit(bot)
  await w.sleep(5000)

  // --- 9. Nowy gracz bez postaci: od razu kreator, bez listy ------------------------------------------
  const nowy = await w.connect('NowyWybor' + (Date.now() % 1000))
  await w.sleep(6000)
  R.assert(nowy.game.gameMode === 'spectator' && !nowy.dialog, 'nowy gracz: od razu scena kreatora, bez listy')
  await w.lpm(nowy)
  const kd = await w.waitDialog(nowy, d => /Nowa postać/.test(w.plain(d.title)), 5000)
  R.assert(!!kd, 'nowy gracz: LPM w kreatorze -> ksiega "Nowa postac"')
  await w.quit(nowy)

  // Czat calego przebiegu (wszystkie sesje bota WyborQA sa w logu serwera; bot.log ma ostatnia) — bez
  // angielskich skryptow MMOProfiles; log klonu bez 'Invalid hud holder' przy wyjsciach w scenie.
  const logAll = w.logSince(START)
  const eng = (logAll.match(/Profile (created|selected|deleted)|Welcome! Please choose|successfully left your profile/g) || [])
  R.assert(eng.length === 0 && !czat.some(l => /Profile (created|selected|deleted)|Welcome! Please|successfully left/.test(l)),
    'czat bez angielskich komunikatow MMOProfiles', eng.concat(czat.filter(l => /Profile|Welcome|successfully/.test(l))).join(' | '))
  R.assert(!/Invalid hud holder/.test(logAll), 'log bez "Invalid hud holder specified" (wyjscia w scenie)')
  R.print()
  console.log(`WYNIK: ${R.rows.length - R.failed}/${R.rows.length} PASS`)
  process.exit(R.failed)
})().catch(e => { R.print(); console.error('ERR', e); process.exit(99) })
