// Kosz: termin minal (trash.yml cofniety o 8 dni przy zatrzymanym serwerze) -> przy wejsciu postac znika naprawde.
const fs = require('fs')
const w = require('./lib/w')
const R = new w.Results('kosz-termin')
;(async () => {
  const off = w.logOffset()
  const bot = await w.connect('WyborQA')
  const d = await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 12000)
  const log = w.logSince(off)
  R.assert(/Kosz: termin minal — Elwen/.test(log), 'log: termin minal — Elwen usuwana na zawsze')
  R.assert(/usuwa NA ZAWSZE postac Elwen/.test(log), 'log: removeProfile Elwen')
  R.assert(!!d && /9\/12/.test(w.plain(d.title)), 'miejsca zwolnione: 9/12', d && w.plain(d.title))
  R.assert(!/W koszu/.test(w.text(bot)), 'lista nie wspomina o koszu')
  R.assert(!/name: Elwen/.test(fs.readFileSync('/clone-data/plugins/CharacterCreator/trash.yml', 'utf8')), 'trash.yml bez Elwen')
  // CharacterStore zapisuje z opoznieniem (debounce) — czekamy do 8 s.
  let gone = false
  for (let i = 0; i < 16 && !gone; i++) { gone = !/name: Elwen/.test(fs.readFileSync('/clone-data/plugins/CharacterCreator/characters.yml', 'utf8')); if (!gone) await w.sleep(500) }
  R.assert(gone, 'characters.yml bez Elwen (po zapisie z debounce)')
  const eid = (log.match(/postac Elwen \(profil ([0-9a-f-]{36})\)/) || [])[1]
  await w.sleep(6500)
  const P = '/clone-data/plugins/'
  const sz = ['CoreTools/userdata/{id}.yml', 'MMOCore/userdata/{id}.yml', 'MMOItems/userdata/{id}.yml', 'MMOInventory/userdata/{id}.yml']
  R.assert(!!eid && sz.every(t => !fs.existsSync(P + t.replaceAll('{id}', eid))) &&
    sz.some(t => fs.existsSync(P + 'CharacterCreator/usuniete/' + eid + '/' + t.replaceAll('{id}', eid))),
    'pliki modulow Elwen przeniesione do CharacterCreator/usuniete/<id>/', eid)
  await w.click(bot, /^(★ )?Grommash /)
  await w.sleep(4000)
  await w.quit(bot)
  R.print(); console.log(`WYNIK: ${R.rows.length - R.failed}/${R.rows.length} PASS`); process.exit(R.failed)
})().catch(e => { R.print(); console.error('ERR', e); process.exit(99) })
