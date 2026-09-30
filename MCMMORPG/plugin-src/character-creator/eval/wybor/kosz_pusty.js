/**
 * Wszystkie postacie w koszu (konto KoszQA, 2 postacie z setup.js): gracz nie moze utknac —
 * po wejsciu lista sama, "+ Nowa postac" i "Kosz (n)"; LPM = kreator; przywrocenie z kosza dziala.
 */
const w = require('./lib/w')
const R = new w.Results('kosz-pusty')
const NAME = 'KoszQA'
const btn = bot => w.buttons(bot).map(b => b.label)
const hint = bot => { const ab = bot.log.filter(l => l.startsWith('[AB] ') && /\[(LPM|A\/D)/.test(l)); return ab.length ? ab[ab.length - 1] : '' }
async function lista(bot) {
  bot.dialog = null
  for (let t = 0; t < 4; t++) { await w.ppm(bot); if (await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 3000)) return true }
  return false
}
;(async () => {
  let bot = await w.connect(NAME)
  await w.sleep(5000)
  R.assert(await lista(bot), 'PPM -> lista (2 postacie)')
  await w.click(bot, /Zarządzaj postaciami/)
  await w.waitDialog(bot, d => /Zarządzaj/.test(w.plain(d.title)), 5000)
  for (const re of [/^☐ /, /^☐ /]) { await w.click(bot, re); await w.waitDialog(bot, d => /Zarządzaj/.test(w.plain(d.title)), 5000) }
  await w.click(bot, /^Usuń zaznaczone \(2\)/)
  await w.waitDialog(bot, d => /Przenieść do kosza/.test(w.plain(d.title)), 5000)
  await w.click(bot, /Przenieś do kosza/)
  await w.waitDialog(bot, d => /Zarządzaj/.test(w.plain(d.title)), 5000)
  R.assert(/Nie masz aktywnych postaci/.test(w.text(bot)) && /^Kosz \(2\)/.test(btn(bot)[0]), 'obie w koszu: "Nie masz aktywnych postaci", Kosz (2) na gorze', btn(bot).join(' | '))
  await w.quit(bot); await w.sleep(5000)

  bot = await w.connect(NAME)
  const d = await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 12000)
  R.assert(!!d, 'wejscie z pustym kontem: lista otwiera sie sama')
  const b = btn(bot)
  R.assert(b.some(l => /\+ Nowa postać/.test(l)) && b.some(l => /^Kosz \(2\)/.test(l)) && b.some(l => /Zarządzaj/.test(l)), 'lista: + Nowa postac, Zarzadzaj, Kosz (2)', b.join(' | '))
  R.assert(/Nie masz aktywnych postaci\. Przywróć je z kosza \(2 postacie\) albo stwórz nową/.test(w.text(bot)), 'lista mowi, co zrobic (kosz albo nowa)', w.text(bot))
  await w.click(bot, /Wróć do sceny/)
  await w.sleep(2000)
  R.assert(/\[LPM\] Nowa postać/.test(hint(bot)), 'scena bez postaci: podpowiedz "[LPM] Nowa postac"', hint(bot))
  // LPM = nowa postac (kreator), PPM w kreatorze = rezygnacja -> z powrotem wybor.
  await w.lpm(bot)
  await w.sleep(4000)
  const log = w.logSince(0).split('\n').filter(l => l.includes(NAME)).slice(-6).join('\n')
  R.assert(/Scena otwarta: KoszQA \(PickStep\)/.test(log), 'LPM w pustej scenie -> kreator (PickStep)')
  await w.ppm(bot)
  const d2 = await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 12000)
  R.assert(!!d2, 'rezygnacja z kreatora -> z powrotem lista z koszem')
  await w.click(bot, /^Kosz \(2\)/)
  await w.waitDialog(bot, d => /^Kosz/.test(w.plain(d.title)), 5000)
  await w.click(bot, /^☐ /)
  await w.waitDialog(bot, d => /^Kosz/.test(w.plain(d.title)), 5000)
  await w.click(bot, /^Przywróć zaznaczone \(1\)/)
  await w.waitDialog(bot, d => /^Kosz/.test(w.plain(d.title)), 5000)
  R.assert(/Przywrócono: /.test(w.text(bot)), 'przywrocono jedna z kosza', w.text(bot).slice(0, 80))
  await w.quit(bot); await w.sleep(5000)
  bot = await w.connect(NAME)
  await w.sleep(6000)
  R.assert(/\[LPM\/Spacja\] Graj/.test(hint(bot)), 'po relogu: scena z przywrocona postacia (LPM/Spacja Graj)', hint(bot))
  await w.lpm(bot)
  await w.sleep(4000)
  R.assert(bot.game.gameMode !== 'spectator', 'gra przywrocona postacia')
  await w.quit(bot)
  R.print(); console.log(`WYNIK: ${R.rows.length - R.failed}/${R.rows.length} PASS`); process.exit(R.failed)
})().catch(e => { R.print(); console.error('ERR', e); process.exit(99) })
