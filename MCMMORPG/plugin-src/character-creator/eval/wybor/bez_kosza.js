/**
 * Zgloszenie 01.10 (konto LimitQA: 5 postaci, bez rangi = limit 5):
 *  - pelny limit widac pod A/D ("+ Nowa postac (limit)" -> "[LPM] Usun postac"), LPM tam = Zarzadzaj z licznikiem,
 *  - usuwanie bez kosza: od razu na zawsze, zadnego slowa "kosz",
 *  - po usunieciu WSZYSTKICH scena od razu pokazuje "+ Nowa postac" pod A/D (wczesniej dopiero pod PPM),
 *  - kamera: pakiet camera dochodzi PO pojawieniu sie encji-kamery (inaczej klient go ignoruje = wolna kamera, martwy LPM).
 */
const w = require('./lib/w')
const R = new w.Results('bez-kosza')
const NAME = process.env.QA || 'LimitQA'
const btn = bot => w.buttons(bot).map(b => b.label)
const hint = bot => { const ab = bot.log.filter(l => l.startsWith('[AB] ') && /\[(LPM|A\/D)/.test(l)); return ab.length ? ab[ab.length - 1] : '' }
const noKosz = s => !/kosz/i.test(s)

function watchCamera(bot) {
  const spawned = new Map() // id -> czas
  const cams = [] // {id, t, known}
  bot._client.on('spawn_entity', p => spawned.set(p.entityId, Date.now()))
  bot._client.on('camera', p => cams.push({ id: p.cameraId, t: Date.now(), known: spawned.has(p.cameraId) }))
  return { cams, spawned }
}
const camOk = c => c.cams.some(x => x.known)

;(async () => {
  let bot = await w.connect(NAME)
  const cam = watchCamera(bot)
  await w.sleep(6000)
  R.assert(camOk(cam), 'kamera: pakiet camera po spawnie encji-kamery (wejscie)', JSON.stringify(cam.cams))

  // Karuzela: 5 postaci + Nowa (limit) + Usun. Idziemy w lewo o 2 (z ostatnio granej) az do "Usun postac".
  let found = false
  for (let i = 0; i < 8 && !found; i++) {
    await w.press(bot, 'right'); await w.sleep(400)
    found = /\[LPM\] Usuń postać/.test(hint(bot)) && !/Usuń postacie/.test(hint(bot))
    if (/\[LPM\] Usuń postać/.test(hint(bot))) found = true
  }
  R.assert(found, 'A/D przy pelnym limicie: miejsce "Nowa postac" pokazuje [LPM] Usun postac', hint(bot))
  // LPM na miejscu "Nowa (limit)" albo "Usun" — oba prowadza do Zarzadzaj.
  await w.lpm(bot)
  const m = await w.waitDialog(bot, d => /Zarządzaj/.test(w.plain(d.title)), 6000)
  R.assert(!!m, 'LPM przy limicie -> okno Zarzadzaj')
  R.assert(/5\/5/.test(w.text(bot)), 'Zarzadzaj mowi o limicie 5/5', w.text(bot).slice(0, 160))
  R.assert(noKosz(w.text(bot) + btn(bot).join('|')), 'Zarzadzaj bez slowa "kosz"', btn(bot).join(' | '))

  for (const n of ['Lima', 'Limb', 'Limc', 'Limd', 'Lime']) {
    await w.click(bot, new RegExp('^☐ ' + n)); await w.waitDialog(bot, d => /Zarządzaj/.test(w.plain(d.title)), 5000)
  }
  await w.click(bot, /^Usuń zaznaczone \(5\)/)
  const c = await w.waitDialog(bot, d => /Usunąć na zawsze/.test(w.plain(d.title)), 5000)
  R.assert(!!c && noKosz(w.text(bot) + btn(bot).join('|')), 'potwierdzenie: "Usunac na zawsze?", bez kosza', c ? w.text(bot).slice(0, 120) : 'brak okna')
  await w.click(bot, /Usuń na zawsze/)
  const g = await w.waitDialog(bot, d => /Twoje postacie/.test(w.plain(d.title)), 6000)
  R.assert(!!g && /0\/5/.test(w.plain(g.title)), 'po usunieciu wszystkich: lista 0/5', g ? w.plain(g.title) : 'brak')
  R.assert(btn(bot).some(l => /\+ Nowa postać/.test(l)) && !btn(bot).some(l => /Zarządzaj/.test(l)), 'lista: + Nowa postac, bez Zarzadzaj', btn(bot).join(' | '))
  await w.click(bot, /Wróć do sceny/)
  await w.sleep(2500)
  R.assert(/\[LPM\] Stwórz postać/.test(hint(bot)), 'scena od razu: [LPM] Stworz postac (bez A/D, bez PPM)', hint(bot))
  await w.press(bot, 'right'); await w.sleep(800)
  R.assert(/\[LPM\] Stwórz postać/.test(hint(bot)), 'A/D nie gubi "Nowa postac"', hint(bot))
  const off = w.logOffset()
  await w.lpm(bot)
  await w.sleep(4000)
  R.assert(/Scena otwarta: LimitQA \(PickStep\)/.test(w.logSince(off)), 'LPM -> kreator (PickStep)')
  // kamera po przejsciu do kreatora (zmiana kontrolera, kadr) — pakiet dalej trafia w znana encje
  const last = cam.cams[cam.cams.length - 1]
  R.assert(last && last.known, 'kamera w kreatorze: ostatni pakiet camera wskazuje znana encje', JSON.stringify(last))
  await w.quit(bot)

  const log = w.logSince(0)
  R.assert(/usuwa NA ZAWSZE postac Lime/.test(log) && !/przenosi do kosza/.test(log.split('\n').filter(l => l.includes(NAME)).join('\n')), 'log: usuniete na zawsze, nic do kosza')
  R.print(); console.log(`WYNIK: ${R.rows.length - R.failed}/${R.rows.length} PASS`); process.exit(R.failed)
})().catch(e => { R.print(); console.error('ERR', e); process.exit(99) })
