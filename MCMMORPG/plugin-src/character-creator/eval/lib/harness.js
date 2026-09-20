'use strict'
/**
 * Harness evalow kreatora postaci.
 *
 * Bot wchodzi przez proxy jak zwykly gracz (velocity modern forwarding, online-mode=false),
 * wiec testuje DOKLADNIE te sciezke, ktora chodzi czlowiek. Protokolowo, bez renderowania:
 * widzimy tytul okna, kazdy slot, ekwipunek, pozycje, tryb gry i wiadomosci systemowe.
 *
 * Czego ten harness NIE sprawdzi: tekstur, przezroczystosci, kadru, czytelnosci ikon.
 * Na to jest klient z Xvfb — patrz README.md. Podzial jest celowy: rzeczy sprawdzalne
 * protokolem maja byc sprawdzane protokolem, bo to kosztuje jedna linijke PASS/FAIL
 * zamiast zrzutu ekranu.
 */
const mineflayer = require('mineflayer')
const velocity = require('./velocity')
const resourcepack = require('./resourcepack')

// Domyslnie WPROST na world1 z wlasnym podpisem forwardingu — patrz lib/velocity.js.
const HOST = process.env.MC_HOST || 'world1'
const PORT = +(process.env.MC_PORT || 25565)
const SECRET = process.env.MC_FORWARDING_SECRET || ''
const VERSION = process.env.MC_VERSION || '1.21.11'
const TARGET = process.env.MC_SERVER || 'world1'

const sleep = ms => new Promise(r => setTimeout(r, ms))

class Results {
  constructor(name) { this.name = name; this.rows = [] }
  ok(label, extra) { this.rows.push({ pass: true, label, extra }) }
  fail(label, extra) { this.rows.push({ pass: false, label, extra }) }
  assert(cond, label, extra) { cond ? this.ok(label, extra) : this.fail(label, extra); return cond }
  get failed() { return this.rows.filter(r => !r.pass).length }
  print() {
    for (const r of this.rows) {
      console.log(`${r.pass ? 'PASS' : 'FAIL'} [${this.name}] ${r.label}` +
        (r.extra ? ` — ${r.extra}` : ''))
    }
  }
}

/** Tworzy bota i czeka na spawn. Odrzuca z czytelnym powodem zamiast wisiec. */
async function connect(username, { timeout = 60000 } = {}) {
  const bot = mineflayer.createBot({ host: HOST, port: PORT, username, auth: 'offline', version: VERSION })
  if (SECRET) velocity.attach(bot._client, { secret: SECRET, username })
  resourcepack.attach(bot._client, { declined: process.env.EVAL_DECLINE_PACK === '1' })
  bot.evalLog = []
  bot.on('message', m => bot.evalLog.push(m.toString()))
  await new Promise((resolve, reject) => {
    const t = setTimeout(() => reject(new Error(`${username}: brak spawn w ${timeout} ms`)), timeout)
    bot.once('spawn', () => { clearTimeout(t); resolve() })
    bot.once('kicked', r => { clearTimeout(t); reject(new Error(`${username} kicked: ${JSON.stringify(r).slice(0, 200)}`)) })
    bot.once('error', e => { clearTimeout(t); reject(e) })
  })
  return bot
}

/** Przechodzi na serwer docelowy przez proxy i czeka na ponowny spawn. */
async function switchServer(bot, server = TARGET, { timeout = 60000 } = {}) {
  const before = bot.entity && bot.entity.id
  bot.chat(`/server ${server}`)
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    await sleep(500)
    if (bot.entity && bot.entity.id !== before) return true
  }
  return false
}

/** Czeka, az bot bedzie mial otwarte okno. Zwraca okno albo null. */
async function waitForWindow(bot, { timeout = 15000, titleIncludes = null } = {}) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    const w = bot.currentWindow
    if (w && (!titleIncludes || windowTitle(w).includes(titleIncludes))) return w
    await sleep(250)
  }
  return null
}

/** Czeka, az okno zniknie (albo zmieni sie na inne). */
async function waitForWindowChange(bot, from, { timeout = 15000 } = {}) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    if (bot.currentWindow !== from) return bot.currentWindow
    await sleep(250)
  }
  return from
}

/** Tytul okna jako zwykly tekst — mineflayer podaje go w kilku ksztaltach zaleznie od wersji. */
function windowTitle(win) {
  if (!win) return ''
  const t = win.title
  if (typeof t === 'string') {
    if (t.startsWith('{') || t.startsWith('[')) {
      try { return plainText(JSON.parse(t)) } catch { return t }
    }
    return t
  }
  return plainText(t)
}

function plainText(node) {
  if (node == null) return ''
  if (typeof node === 'string') return node
  if (Array.isArray(node)) return node.map(plainText).join('')
  if (node.value !== undefined && node.type !== undefined) return plainText(node.value)
  let out = node.text != null ? plainText(node.text) : ''
  if (node.translate) out += plainText(node.translate)
  if (node.extra) out += plainText(node.extra)
  return out
}

/** Nazwa wyswietlana itemu (custom name), bez kolorow — albo typ, gdy brak nazwy. */
function itemLabel(item) {
  if (!item) return null
  const custom = item.customName || (item.nbt && item.nbt.value && item.nbt.value.display)
  const name = item.customName ? plainText(safeJson(item.customName)) : null
  return (name || item.displayName || item.name || '').replace(/§./g, '')
}

function safeJson(s) { try { return JSON.parse(s) } catch { return s } }

/** Zajete sloty okna jako [{slot, label, name}] — do diagnostyki i asercji. */
function windowSlots(win) {
  if (!win) return []
  return win.slots.map((it, slot) => it ? { slot, label: itemLabel(it), name: it.name } : null).filter(Boolean)
}

/** Znajduje slot, ktorego etykieta pasuje do wzorca. */
function findSlot(win, re) {
  return windowSlots(win).find(s => re.test(s.label || '') || re.test(s.name || '')) || null
}

async function click(bot, slot) {
  await bot.clickWindow(slot, 0, 0)
  await sleep(600)
}

function inventoryLabels(bot) {
  return bot.inventory.slots.map((it, i) => it ? `${i}:${it.name}x${it.count}` : null).filter(Boolean)
}

async function quit(bot) { try { bot.quit() } catch {} await sleep(400) }

module.exports = {
  connect, switchServer, waitForWindow, waitForWindowChange, windowTitle, windowSlots,
  findSlot, click, inventoryLabels, itemLabel, plainText, quit, sleep, Results, TARGET,
}
