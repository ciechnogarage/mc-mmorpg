'use strict'
/**
 * Harness evali Chowańców (Agent A). Bot mineflayer wchodzi na serwer jak gracz; wynik = PASS/FAIL.
 * Wzór: plugin-src/character-creator/eval/lib/harness.js (memory eval-botem-mineflayer).
 * Serwer testowy chw-test nie ma velocity ani resource packa, więc łączymy się wprost.
 * Okna Papera (dialogi): pakiet show_dialog -> NBT -> przyciski; klik = custom_click_action.
 */
const mineflayer = require('mineflayer')
const nbt = require('prismarine-nbt')
const { execSync } = require('child_process')

const HOST = process.env.MC_HOST || 'chw-test'
const PORT = +(process.env.MC_PORT || 25565)
const VERSION = process.env.MC_VERSION || '1.21.11'
const RCON = process.env.RCON_CMD || '' // wołane z hosta — w kontenerze używamy pliku-kolejki
const sleep = ms => new Promise(r => setTimeout(r, ms))

class Results {
  constructor(name) { this.name = name; this.rows = [] }
  ok(l, x) { this.rows.push({ pass: true, l, x }) }
  fail(l, x) { this.rows.push({ pass: false, l, x }) }
  assert(c, l, x) { c ? this.ok(l, x) : this.fail(l, x); return c }
  get failed() { return this.rows.filter(r => !r.pass).length }
  print() { for (const r of this.rows) console.log(`${r.pass ? 'PASS' : 'FAIL'} [${this.name}] ${r.l}${r.x ? ' — ' + r.x : ''}`) }
}

function plain(node) {
  if (node == null) return ''
  if (typeof node === 'string') return node
  if (Array.isArray(node)) return node.map(plain).join('')
  if (typeof node !== 'object') return String(node)
  if (node.type !== undefined && node.value !== undefined && node.text === undefined) return plain(node.value)
  let out = node.text != null ? plain(node.text) : (node[''] != null ? plain(node['']) : '')
  if (node.translate) out += plain(node.translate)
  if (node.extra) out += plain(node.extra)
  return out
}

async function connect(username, { timeout = 60000 } = {}) {
  const bot = mineflayer.createBot({ host: HOST, port: PORT, username, auth: 'offline', version: VERSION })
  // Serwer z Nexo (chw-proba) wysyła pack w fazie konfiguracji i czeka na status (memory eval-botem-mineflayer).
  require('./resourcepack').attach(bot._client, { declined: false })
  bot.log = []
  bot.dialog = null
  bot.dialogs = 0
  const szum = t => /❤ \d+\/\d+ \| ★/.test(t)
  bot.on('message', m => { const t = m.toString(); if (!szum(t)) bot.log.push(t) })
  bot.on('actionBar', m => { const t = m.toString(); if (!szum(t)) bot.log.push('[AB] ' + t) })
  bot._client.on('show_dialog', p => {
    try {
      const d = p.dialog && p.dialog.data ? nbt.simplify(p.dialog.data) : p.dialog
      bot.dialog = d
      bot.dialogs++
    } catch (e) { bot.dialog = { blad: String(e) } }
  })
  bot._client.on('clear_dialog', () => { bot.dialog = null })
  await new Promise((resolve, reject) => {
    const t = setTimeout(() => reject(new Error(`${username}: brak spawn w ${timeout} ms`)), timeout)
    bot.once('spawn', () => { clearTimeout(t); resolve() })
    bot.once('kicked', r => { clearTimeout(t); reject(new Error(`${username} kicked: ${JSON.stringify(r).slice(0, 300)}`)) })
    bot.once('error', e => { clearTimeout(t); reject(e) })
  })
  return bot
}

/** Przyciski aktualnego okna: [{label, id, payload}] (multi_action: actions + exit_action). */
function dialogButtons(d) {
  if (!d) return []
  const out = []
  const push = b => {
    if (!b) return
    const a = b.action || {}
    out.push({ label: plain(b.label), id: a.id, payload: a.payload ?? a.additions, raw: a })
  }
  for (const b of (d.actions || [])) push(b)
  if (d.exit_action) push(d.exit_action)
  if (d.action && d.action.label) push(d.action)
  if (d.yes) push(d.yes)
  if (d.no) push(d.no)
  return out
}

function dialogText(d) {
  if (!d) return ''
  const body = Array.isArray(d.body) ? d.body : (d.body ? [d.body] : [])
  return plain(d.title) + '\n' + body.map(b => plain(b.contents)).join('\n')
}

async function waitDialog(bot, pred = () => true, timeout = 10000) {
  const end = Date.now() + timeout
  while (Date.now() < end) {
    if (bot.dialog && pred(bot.dialog)) return bot.dialog
    await sleep(200)
  }
  return null
}

/**
 * Klika przycisk okna, którego etykieta pasuje do wzorca.
 * UWAGA: minecraft-data 1.21.11 opisuje custom_click_action jako `option + anonymousNbt`, a serwer
 * (ServerboundCustomClickActionPacket, bajtkod paper-1.21.11) czyta `lengthPrefixed(65536)` z NBT.
 * Dlatego pakiet składamy ręcznie i wysyłamy writeRaw (inaczej serwer rozłącza bota).
 */
async function clickDialog(bot, re) {
  const b = dialogButtons(bot.dialog).find(x => typeof re === 'string' ? x.label === re : re.test(x.label))
  if (!b || !b.id) return false
  bot.dialog = null
  const nbtBuf = b.payload === undefined ? Buffer.from([0x00]) : encodeNbtRoot(b.payload)
  const id = Buffer.from(b.id, 'utf8')
  const pkt = Buffer.concat([varint(0x41), varint(id.length), id, varint(nbtBuf.length), nbtBuf])
  bot._client.writeRaw(pkt)
  await sleep(700)
  return true
}

function varint(n) {
  const out = []
  let v = n >>> 0
  while (v > 0x7f) { out.push((v & 0x7f) | 0x80); v >>>= 7 }
  out.push(v)
  return Buffer.from(out)
}
function nbtStr(s) { const b = Buffer.from(s, 'utf8'); const l = Buffer.alloc(2); l.writeUInt16BE(b.length); return Buffer.concat([l, b]) }
function nbtPayload(v) {
  if (typeof v === 'string') return [0x08, nbtStr(v)]
  if (typeof v === 'number' && Number.isInteger(v)) { const b = Buffer.alloc(4); b.writeInt32BE(v); return [0x03, b] }
  if (typeof v === 'number') { const b = Buffer.alloc(8); b.writeDoubleBE(v); return [0x06, b] }
  if (Array.isArray(v)) { const b = Buffer.alloc(4 + 4 * v.length); b.writeInt32BE(v.length); v.forEach((x, i) => b.writeInt32BE(x, 4 + 4 * i)); return [0x0b, b] }
  if (v && typeof v === 'object') return [0x0a, compoundBody(v)]
  return [0x01, Buffer.from([0])]
}
function compoundBody(o) {
  const parts = []
  for (const k of Object.keys(o)) { const [t, b] = nbtPayload(o[k]); parts.push(Buffer.from([t]), nbtStr(k), b) }
  parts.push(Buffer.from([0x00]))
  return Buffer.concat(parts)
}
/** Sieciowe NBT (bez nazwy korzenia): 0x0a + ciało compound. */
function encodeNbtRoot(o) { return Buffer.concat([Buffer.from([0x0a]), compoundBody(o)]) }

function rcon(cmd) {
  // Evale uruchamiane w kontenerze node — komendy serwera idą przez plik kolejki czytany przez run.sh na hoście.
  // Prościej: sam kontener eval ma dostęp do docker.sock? Nie — używamy RCON protokołu bezpośrednio.
  throw new Error('użyj rconSend')
}

const net = require('net')
/** Minimalny klient RCON (Source RCON) — serwer chw-test:25575, hasło z env. */
function rconSend(cmd, { host = HOST, port = 25575, pass = process.env.RCON_PASS || 'chwtest' } = {}) {
  return new Promise((resolve, reject) => {
    const s = net.connect(port, host)
    let buf = Buffer.alloc(0), authed = false
    const pkt = (id, type, body) => {
      const b = Buffer.from(body, 'utf8')
      const p = Buffer.alloc(14 + b.length)
      p.writeInt32LE(10 + b.length, 0); p.writeInt32LE(id, 4); p.writeInt32LE(type, 8); b.copy(p, 12); p.writeInt16LE(0, 12 + b.length)
      return p
    }
    const t = setTimeout(() => { s.destroy(); reject(new Error('rcon timeout: ' + cmd)) }, 8000)
    s.on('data', d => {
      buf = Buffer.concat([buf, d])
      while (buf.length >= 4) {
        const len = buf.readInt32LE(0)
        if (buf.length < len + 4) break
        const id = buf.readInt32LE(4)
        const body = buf.slice(12, 4 + len - 2).toString('utf8')
        buf = buf.slice(4 + len)
        if (!authed) {
          if (id === -1) { clearTimeout(t); s.destroy(); return reject(new Error('rcon auth')) }
          authed = true
          s.write(pkt(2, 2, cmd))
        } else { clearTimeout(t); s.end(); return resolve(body) }
      }
    })
    s.on('error', e => { clearTimeout(t); reject(e) })
    s.write(pkt(1, 3, pass))
  })
}

/** Wybór/utworzenie profilu MMOProfiles w oknie wyboru (profile_names wyłączone). */
const SLUZBOWE = /Create New|VIP|MVP|Next Page|Previous|Removal|Log Out/i
async function wybierzProfil(bot, { nowy = false } = {}) {
  const end = Date.now() + 20000
  while (Date.now() < end && !bot.currentWindow) await sleep(300)
  const lista = () => { const w = bot.currentWindow; return w ? w.slots.map((it, i) => it && i < w.inventoryStart ? { i, name: it.name, label: label(it) } : null).filter(Boolean) : [] }
  if (!bot.currentWindow) return { ok: false, powod: 'brak okna profili' }
  let sloty = lista()
  if (nowy) {
    const c = sloty.find(s => /Create New/i.test(s.label))
    if (!c) return { ok: false, powod: 'brak „Create New Character”: ' + sloty.map(s => s.label).join('|') }
    const przed = sloty.filter(s => !SLUZBOWE.test(s.label)).map(s => s.label)
    await bot.clickWindow(c.i, 0, 0)
    await sleep(2000)
    if (!bot.currentWindow) return { ok: true, sloty, auto: true } // utworzenie od razu wybrało profil
    sloty = lista()
    const nowe = sloty.filter(s => !SLUZBOWE.test(s.label) && !przed.includes(s.label))
    const cel = nowe[0] || sloty.filter(s => !SLUZBOWE.test(s.label)).pop()
    if (!cel) return { ok: false, powod: 'po utworzeniu brak profilu: ' + sloty.map(s => s.label).join('|') }
    await bot.clickWindow(cel.i, 0, 0)
  } else {
    const cel = sloty.find(s => !SLUZBOWE.test(s.label))
    if (!cel) return { ok: false, powod: 'brak profilu do wyboru: ' + sloty.map(s => s.label).join('|') }
    await bot.clickWindow(cel.i, 0, 0)
  }
  await sleep(2000)
  return { ok: !bot.currentWindow, sloty, powod: bot.currentWindow ? 'okno nadal otwarte: ' + lista().map(s => s.label).join('|') : '' }
}

function label(item) {
  if (!item) return ''
  const comp = (item.components || []).find(c => c.type === 'custom_name' || c.type === 'item_name')
  if (comp && comp.data) {
    try { return plain(comp.data.type ? nbt.simplify(comp.data) : comp.data).replace(/§./g, '') } catch {}
  }
  const cn = item.customName
  if (cn && typeof cn === 'object') return plain(cn).replace(/§./g, '')
  if (typeof cn === 'string') { try { return plain(JSON.parse(cn)).replace(/§./g, '') } catch { return cn.replace(/§./g, '') } }
  return String(item.displayName || item.name || '').replace(/§./g, '')
}

async function quit(bot) { try { bot.quit() } catch {} await sleep(500) }

module.exports = { connect, dialogButtons, dialogText, waitDialog, clickDialog, rconSend, wybierzProfil, label, plain, quit, sleep, Results }
