'use strict'
// Nakladka na wspolny zestaw /bot/lib (CLAUDE.md: boty tylko przez /opt/docker/mc/narzedzia/bot) — to samo API,
// ktorego uzywa gra.js: Results, connect, okna, etykiety.
const L = require('/bot/lib')
const sleep = ms => new Promise(r => setTimeout(r, ms))
function plainText(n) {
  if (n == null) return ''
  if (typeof n === 'string') return n
  if (Array.isArray(n)) return n.map(plainText).join('')
  if (n.value !== undefined && n.type !== undefined) return plainText(n.value)
  let o = n.text != null ? plainText(n.text) : ''
  if (n.translate) o += plainText(n.translate)
  if (n.extra) o += plainText(n.extra)
  return o
}
const safeJson = s => { try { return JSON.parse(s) } catch { return s } }
function itemLabel(i) { if (!i) return null; const n = i.customName ? plainText(safeJson(i.customName)) : null; return (n || i.displayName || i.name || '').replace(/§./g, '') }
function windowTitle(w) { if (!w) return ''; const t = w.title; return typeof t === 'string' ? (/^[{[]/.test(t) ? plainText(safeJson(t)) : t) : plainText(t) }
function windowSlots(w) { return w ? w.slots.map((it, slot) => it ? { slot, label: itemLabel(it), name: it.name } : null).filter(Boolean) : [] }
function findSlot(w, re) { return windowSlots(w).find(s => re.test(s.label || '') || re.test(s.name || '')) || null }
async function click(bot, slot) { await bot.clickWindow(slot, 0, 0); await sleep(600) }
function inventoryLabels(bot) { return bot.inventory.slots.map((it, i) => it ? `${i}:${it.name}x${it.count}` : null).filter(Boolean) }
async function waitForWindow(bot, { timeout = 15000 } = {}) {
  if (bot.currentWindow) return bot.currentWindow
  return new Promise((res, rej) => { const t = setTimeout(() => rej(new Error('brak okna')), timeout); bot.once('windowOpen', w => { clearTimeout(t); res(w) }) })
}
async function connect(nick) {
  const bot = await L.polacz({ nick })
  bot.evalLog = []
  bot.on('message', m => bot.evalLog.push(m.toString()))
  return bot
}
async function quit(bot) { try { bot.quit() } catch {} await sleep(400) }
class Results {
  constructor(name) { this.name = name; this.rows = [] }
  ok(l, e) { this.rows.push({ pass: true, l, e }) }
  fail(l, e) { this.rows.push({ pass: false, l, e }) }
  assert(c, l, e) { c ? this.ok(l, e) : this.fail(l, e); return c }
  get failed() { return this.rows.filter(r => !r.pass).length }
  print() { for (const r of this.rows) console.log(`${r.pass ? 'PASS' : 'FAIL'} [${this.name}] ${r.l}${r.e ? ' — ' + r.e : ''}`) }
}
class Rcon { async connect() {} async send(c) { return L.rcon(c) } close() { L.zamknijRcon() } }
module.exports = { Results, connect, quit, itemLabel, windowTitle, windowSlots, findSlot, click, inventoryLabels, waitForWindow, Rcon, sleep }
