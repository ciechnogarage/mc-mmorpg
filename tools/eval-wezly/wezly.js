'use strict'
/**
 * Eval wezlow zbierackich przeciwko ZYWEMU world1.
 * Sprawdza pelny lancuch: uderzenie w wezel -> nasz surowiec w plecaku -> XP profesji -> wezel znika.
 *
 * Czego sie nauczylismy przy pisaniu (nie zmieniac bez powodu):
 *  - CMI przechwytuje /tp i /kill; z rcona i z czatu trzeba minecraft:<komenda>,
 *    a teleport miedzy swiatami przez 'minecraft:execute in minecraft:overworld run tp'.
 *  - Skladnia wydania przedmiotu to /mi give <TYP> <ID> <gracz> <ilosc> - gracz jest TRZECI.
 *  - Wezly reaguja na UDERZENIE (~onDamaged). Trigger ~onInteract nie odpala na tym
 *    serwerze na zadnym nosniku, a nosnik ARMOR_STAND nie odpala zadnych triggerow.
 */
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')

const NICK = process.env.EVAL_NICK || 'EvWezel'
const X = 150.5, Y = 99, Z = 197.5
const sleep = ms => new Promise(r => setTimeout(r, ms))
const label = i => { try { return H.itemLabel(i) } catch { return (i && i.displayName) || '' } }
const inv = bot => bot.inventory.items().map(i => `${i.count}x ${label(i)}`).join(' | ')
const has = (bot, frag) => bot.inventory.items().some(i => (label(i) || '').toLowerCase().includes(frag.toLowerCase()))

class R {
  constructor() { this.rows = [] }
  assert(c, l, e) { this.rows.push({ pass: !!c, l, e }); return !!c }
  print() {
    for (const x of this.rows) console.log(`${x.pass ? 'PASS' : 'FAIL'} ${x.l}${x.e ? ' — ' + x.e : ''}`)
    const f = this.rows.filter(x => !x.pass).length
    console.log(f ? `\nWYNIK: ${f} NIEPOWODZEN` : '\nWYNIK: wszystko PASS')
    return f
  }
}

async function doWezla(bot, px = X) {
  for (let i = 0; i < 8; i++) {
    bot.chat(`/minecraft:execute in minecraft:overworld run tp ${NICK} ${px} ${Y} ${Z}`)
    await sleep(1800)
    if (bot.entity && Math.abs(bot.entity.position.x - px) < 6 &&
        Math.abs(bot.entity.position.z - Z) < 6) return true
  }
  return false
}

/** Wezel rozpoznajemy po nazwie w metadanych - w poblizu chodza tez prawdziwe moby. */
function znajdzWezel(bot, fragNazwy) {
  return Object.values(bot.entities)
    .filter(e => e.name === 'zombie' && e.position.distanceTo(bot.entity.position) < 8)
    .filter(e => {
      if (!fragNazwy) return true
      // nazwa bywa zwyklym tekstem albo - przy gradiencie - rozbita na litery w polach "text"
      try {
        const j = JSON.stringify(e.metadata || {})
        const f = fragNazwy.toLowerCase()
        if (j.toLowerCase().includes(f)) return true
        const plain = (j.match(/"text":"([^"]*)"/g) || []).map(x => x.slice(8, -1)).join('')
        return plain.toLowerCase().includes(f)
      } catch { return false }
    })
    .sort((a, b) => a.position.distanceTo(bot.entity.position) - b.position.distanceTo(bot.entity.position))[0]
}

async function odczytXp(bot, profesja) {
  bot.evalLog.length = 0
  bot.chat(`/mmocore admin exp check ${NICK} ${profesja}`)
  await sleep(1400)
  const m = bot.evalLog.join(' | ').match(/has\s+([0-9.]+)\s+EXP/i)
  return m ? parseFloat(m[1]) : -1
}

async function zbierz(bot, r, { nazwa, uderzen, surowiec, profesja, fragNazwy, px }) {
  if (!await doWezla(bot, px)) { r.assert(false, `${nazwa}: bot dotarl pod wezel`, 'pozycja: ' + bot.entity.position); return }
  const w = znajdzWezel(bot, fragNazwy)
  if (!r.assert(!!w, `${nazwa}: wezel widoczny`, 'encji zombie w zasiegu: ' +
      Object.values(bot.entities).filter(e => e.name === 'zombie').length)) return
  await bot.lookAt(w.position.offset(0, 1, 0), true); await sleep(300)

  const xpPrzed = await odczytXp(bot, profesja)
  bot.evalLog.length = 0
  // MythicMobs slucha WYLACZNIE PlayerInteractAtEntityEvent, czyli pakietu INTERACT_AT.
  // bot.activateEntity wysyla INTERACT (mouse=0) i trigger nigdy nie odpali - musi byc activateEntityAt.
  for (let i = 1; i <= uderzen; i++) {
    try { await bot.activateEntityAt(w, w.position.offset(0, 0.5, 0)) }
    catch (e) { console.log('  blad interakcji: ' + e.message) }
    await sleep(1300)
  }
  await sleep(4500)   // najdluzszy kanal (jubiler) trwa 3 s

  const zdobyte = surowiec ? has(bot, surowiec) : (has(bot, 'kryształ') || has(bot, 'kamień'))
  r.assert(zdobyte, `${nazwa}: surowiec w plecaku`, inv(bot).slice(0, 200))
  const komunikat = bot.evalLog.some(m => /Pozyskujesz|pada|Zrywasz|Zbierasz|Wykopujesz|obfit|okaz|bogaty|wyłom|pękła|zebrane|received/i.test(m))
  r.assert(komunikat, `${nazwa}: komunikat o zbiorze`,
    bot.evalLog.filter(m => !/❤|♦|⛨/.test(m)).slice(-2).join(' / ') || '(brak)')

  const xp = await odczytXp(bot, profesja)
  r.assert(xp > xpPrzed, `${nazwa}: XP ${profesja} wzroslo`,
    `${xpPrzed} -> ${xp} EXP`)

  await sleep(400)
  r.assert(!Object.values(bot.entities).some(e => e.id === w.id), `${nazwa}: wezel zniknal`)
}

;(async () => {
  const r = new R()
  const bot = await H.connect(NICK, { timeout: 90000 })
  try {
    try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) }
    catch (e) { console.log('(kreator: ' + e.message + ')') }
    await sleep(3000)
    const WEZLY = [
      { nazwa: 'DRWAL dąb',      uderzen: 3, surowiec: 'dęb',       profesja: 'woodworking',   fragNazwy: 'dąb',      px: 146.5 },
      { nazwa: 'DRWAL świerk',   uderzen: 3, surowiec: 'świerk',    profesja: 'woodworking',   fragNazwy: 'świerkowy',px: 148.5 },
      { nazwa: 'DRWAL brzoza',   uderzen: 2, surowiec: 'brzoz',     profesja: 'woodworking',   fragNazwy: 'brzozowy', px: 150.5 },
      { nazwa: 'ZIELARZ zioła',  uderzen: 1, surowiec: 'zioło',     profesja: 'herbalism',     fragNazwy: 'ziół',     px: 152.5 },
      { nazwa: 'ZIELARZ księżyc',uderzen: 1, surowiec: 'księżyc',   profesja: 'herbalism',     fragNazwy: 'księżycowy',px: 154.5 },
      { nazwa: 'ZIELARZ grzyby', uderzen: 1, surowiec: 'kapelusz',  profesja: 'herbalism',     fragNazwy: 'grzybów',  px: 152.5 },
      { nazwa: 'FARMER',         uderzen: 1, surowiec: 'zbiór',     profesja: 'farming',       fragNazwy: 'poletko',  px: 154.5 },
      { nazwa: 'JUBILER',        uderzen: 1, surowiec: null,        profesja: 'jewelcrafting', fragNazwy: 'druza',    px: 151.5 },
      { nazwa: 'BUDOWNICZY',     uderzen: 1, surowiec: 'glin',     profesja: 'building',      fragNazwy: 'glinianka',px: 153.5 },
    ]
    const tylko = (process.env.EVAL_ONLY || '').toLowerCase()
    for (const w of WEZLY) {
      if (tylko && !tylko.split(',').some(t => w.nazwa.toLowerCase().includes(t.trim()))) continue
      await zbierz(bot, r, w)
    }
  } finally { await H.quit(bot) }
  process.exit(r.print() ? 1 : 0)
})().catch(e => { console.log('FAIL przerwane — ' + e.message); process.exit(2) })
