// Przygotowanie termin.js: Elwen (poz. 15) do kosza droga gracza.
const w = require('./lib/w')
;(async () => {
  const bot = await w.connect('WyborQA')
  await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 12000)
  await w.click(bot, /Zarządzaj postaciami/); await w.waitDialog(bot, d => /Zarządzaj/.test(w.plain(d.title)), 5000)
  await w.click(bot, /^☐ Elwen /); await w.waitDialog(bot, d => /Zarządzaj/.test(w.plain(d.title)), 5000)
  await w.click(bot, /^Usuń zaznaczone \(1\)/); await w.waitDialog(bot, d => /Przenieść/.test(w.plain(d.title)), 5000)
  await w.click(bot, /Przenieś do kosza/, { p0: 'Elwen' }); await w.sleep(1500)
  console.log(w.text(bot).slice(0, 100)); await w.quit(bot); process.exit(0)
})().catch(e => { console.error('ERR', e); process.exit(1) })
