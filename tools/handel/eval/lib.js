'use strict'
/** Wspolne kroki evali handlu: wejscie postaci, klawisz Q, klikanie po menu, odczyt okien. */
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')
const nbt = require('/e/node_modules/prismarine-nbt')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const strip = s => String(s == null ? '' : s).replace(/§./g, '').trim()
function txt(n) {
  if (n == null) return ''
  if (typeof n === 'string') { try { if (n.startsWith('{') || n.startsWith('[')) return txt(JSON.parse(n)) } catch {} return n }
  if (Array.isArray(n)) return n.map(txt).join('')
  if (typeof n === 'object' && n.type && 'value' in n) { try { return txt(nbt.simplify(n)) } catch { return txt(n.value) } }
  let o = ''
  if (n.text != null) o += txt(n.text)
  if (n[''] != null) o += txt(n[''])
  if (n.extra) o += txt(n.extra)
  return o
}
const lore = it => { const l = it && it.customLore; return Array.isArray(l) ? l.map(x => strip(txt(x))) : [] }
const nazwa = it => strip(H.itemLabel(it) || '')
const tytul = w => w ? strip(H.windowTitle(w)).replace(/[\ue000-\uf8ff\u4000-\u4fff]/g, '') : ''
const gorne = w => w ? w.slots.slice(0, w.inventoryStart ?? (w.slots.length - 36)) : []
const kafelki = w => gorne(w).map((it, slot) => it ? { slot, nazwa: nazwa(it), lore: lore(it), name: it.name, count: it.count } : null).filter(Boolean)
function wcisnijQ(bot) { bot._client.write('block_dig', { status: 4, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 }) }
async function wejdz(nick, { klasa = 'wojownik' } = {}) {
  const bot = await H.connect(nick, { timeout: 90000 })
  bot.on('messagestr', m => { if (!/[❤♦⛨]/.test(m) && process.env.EVAL_DEBUG) console.log(`  [${nick} czat] ${m}`) })
  try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot, { klass: klasa }) }
  catch (e) { console.log(`(${nick} kreator: ${e.message})`) }
  await sleep(2500)
  return bot
}
async function czekajOkno(bot, re, timeout = 8000) {
  const t0 = Date.now()
  while (Date.now() - t0 < timeout) {
    if (bot.currentWindow && re.test(tytul(bot.currentWindow))) { await sleep(400); return bot.currentWindow }
    await sleep(100)
  }
  return null
}
async function klik(bot, slot, btn = 0, mode = 0) {
  await bot.clickWindow(slot, btn, mode).catch(e => console.log('  (klik blad', e.message, ')'))
  await sleep(700)
}
/** Q -> Menu -> Handel. Zwraca okno Handlu albo null. */
async function otworzHandel(bot) {
  try { if (bot.currentWindow) bot.closeWindow(bot.currentWindow) } catch {}
  await sleep(500)
  wcisnijQ(bot)
  const m = await czekajOkno(bot, /Menu$/)
  if (!m) return null
  await klik(bot, 15)
  return czekajOkno(bot, /Handel$/)
}
function zrzut(w, etykieta) {
  console.log(`--- ${etykieta}: "${tytul(w)}" (${w ? w.type : '-'})`)
  for (const k of kafelki(w)) console.log(`   [${k.slot}] ${k.name}x${k.count} "${k.nazwa}" | ${k.lore.filter(Boolean).join(' / ').slice(0, 160)}`)
}
module.exports = { H, F, sleep, strip, lore, nazwa, tytul, kafelki, wcisnijQ, wejdz, czekajOkno, klik, otworzHandel, zrzut }
