'use strict'
// Wspolne narzedzia evali przedmiotow: tekst z komponentow 1.21.5+, generowanie z konsoli, odczyt statystyk.
const H = require('/e/lib/harness')
const { rcon } = require('/k/rcon.js')

function tekst(n) {           // komponent tekstowy (NBT/JSON) -> zwykly tekst
  if (n == null) return ''
  if (typeof n === 'number' || typeof n === 'boolean') return String(n)
  if (typeof n === 'string') { if (!/^[\[{]/.test(n)) return n; try { return tekst(JSON.parse(n)) } catch { return n } }
  if (Array.isArray(n)) return n.map(tekst).join('')
  if (n.type && n.value !== undefined) return tekst(n.value)
  let s = n.text !== undefined ? tekst(n.text) : ''
  if (n[''] !== undefined) s += tekst(n[''])
  if (n.extra) s += tekst(n.extra)
  return s
}
function komponent(it, typ) {
  const c = (it && it.components) || []
  const k = c.find(x => x.type === typ)
  return k ? k.data : undefined
}
function opis(it) {
  if (!it) return null
  const nazwa = tekst(komponent(it, 'custom_name')) || H.itemLabel(it)
  const lore = (komponent(it, 'lore') || []).map(tekst)
  const cd = komponent(it, 'custom_data'), cv = cd && cd.value ? cd.value : {}
  const id = cv.MMOITEMS_ITEM_ID ? String(cv.MMOITEMS_ITEM_ID.value) : null
  return { id, nazwa: nazwa.replace(/§./g, ''), lore: lore.map(l => l.replace(/§./g, '')), mat: it.name, slot: it.slot,
           dye: komponent(it, 'dyed_color'), trim: komponent(it, 'trim') }
}
async function generuj(nick, typ, id, poziom, tier) {
  const t = tier ? ` -tier:${tier}` : ''
  return rcon(`mi generate ${nick} -type:${typ} -id:${id} -level:${poziom}${t}`)
}
const ph = async (nick, ...p) => (await rcon(`papi parse ${nick} ${p.map(x => '%' + x + '%').join('|')}`)).trim().split('|')
module.exports = { tekst, komponent, opis, generuj, ph, rcon, H }
