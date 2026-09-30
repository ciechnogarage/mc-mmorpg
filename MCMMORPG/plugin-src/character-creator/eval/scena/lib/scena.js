'use strict'
/**
 * Bot w scenie kreatora (CharacterCreator, naprawa 2026-09-30). Robi to, co gracz klawiszami:
 *  - A/D/W/S/Spacja = pakiet player_input (wcisniecie + puszczenie),
 *  - LPM = use_entity ATTACK w tarcze (encja interaction przed kamera) — w spectatorze serwer
 *    zamienia to na "zacznij ogladac", scena to anuluje i traktuje jak klik,
 *  - PPM = use_entity INTERACT w te sama tarcze,
 *  - dialogi Papera = custom_click_action skladany recznie (h.js), z polami ksiegi (imie, kolor).
 * Konsola (RCON) tylko do OBSERWACJI stanu, poza jednym wyjatkiem: `cc wybor <gracz>` —
 * dokladnie ta komenda, ktora wola przycisk "Zmien postac" w menu Q (DeluxeMenus hub_postac).
 */
const h = require('./h')
const { sleep } = h

const HOST = process.env.MC_HOST || 'cc-nap-kreator'
const rcon = cmd => h.rconSend(cmd, { host: HOST, pass: process.env.RCON_PASS || 'napkreator' })

async function join(name) {
  const bot = await h.connect(name)
  bot.titles = []
  bot._client.on('set_title_text', p => bot.titles.push({ t: 'title', text: txt(p.text), at: Date.now() }))
  bot._client.on('set_title_subtitle', p => bot.titles.push({ t: 'sub', text: txt(p.text), at: Date.now() }))
  bot.gm = null
  bot._client.on('game_state_change', p => { if (p.reason === 3) bot.gm = p.gameMode })
  return bot
}

function txt(nbtOrJson) {
  try {
    if (nbtOrJson && nbtOrJson.type) return h.plain(require('prismarine-nbt').simplify(nbtOrJson))
    if (typeof nbtOrJson === 'string') return h.plain(JSON.parse(nbtOrJson))
    return h.plain(nbtOrJson)
  } catch { return String(nbtOrJson) }
}

/** Tarcza klikow: najblizsza encja interaction. */
function shield(bot) {
  let best = null
  for (const e of Object.values(bot.entities)) {
    if (e.name !== 'interaction' || !bot.entity) continue
    const d = e.position.distanceTo(bot.entity.position)
    if (!best || d < best.d) best = { e, d }
  }
  return best && best.d < 6 ? best.e : null
}

async function waitScene(bot, timeout = 30000) {
  const end = Date.now() + timeout
  while (Date.now() < end) {
    if (shield(bot)) { await sleep(2500); return true } // + czern przy otwarciu
    await sleep(250)
  }
  return false
}

const FLAGS = ['forward', 'backward', 'left', 'right', 'jump', 'shift', 'sprint']
const KEY = { A: 'left', D: 'right', W: 'forward', S: 'backward', SPACJA: 'jump' }
async function press(bot, key, after = 400) {
  const on = {}; for (const f of FLAGS) on[f] = f === KEY[key]
  const off = {}; for (const f of FLAGS) off[f] = false
  bot._client.write('player_input', { inputs: on })
  await sleep(150)
  bot._client.write('player_input', { inputs: off })
  await sleep(after)
}

async function lpm(bot, after = 800) {
  const s = shield(bot); if (!s) throw new Error('brak tarczy (LPM)')
  bot._client.write('use_entity', { target: s.id, mouse: 1, sneaking: false })
  await sleep(after)
}

async function ppm(bot, after = 800) {
  const s = shield(bot); if (!s) throw new Error('brak tarczy (PPM)')
  bot._client.write('use_entity', { target: s.id, mouse: 0, hand: 0, sneaking: false })
  await sleep(after)
}

/** Pole tekstowe/wyboru okna: {key, initial}. */
function inputs(d) {
  return (d && d.inputs ? d.inputs : []).map(i => ({ key: i.key, initial: i.initial, options: i.options }))
}

/** Klik z polami okna (ksiega "Nowa postac"): payload przycisku + wartosci pol. */
async function clickWith(bot, re, fields) {
  const b = h.dialogButtons(bot.dialog).find(x => re.test(x.label))
  if (!b || !b.id) return false
  const payload = Object.assign({}, b.payload || {}, fields)
  bot.dialog = null
  const nbtBuf = encode(payload)
  const id = Buffer.from(b.id, 'utf8')
  bot._client.writeRaw(Buffer.concat([varint(0x41), varint(id.length), id, varint(nbtBuf.length), nbtBuf]))
  await sleep(900)
  return true
}
function varint(n) { const o = []; let v = n >>> 0; while (v > 0x7f) { o.push((v & 0x7f) | 0x80); v >>>= 7 } o.push(v); return Buffer.from(o) }
function str(s) { const b = Buffer.from(s, 'utf8'); const l = Buffer.alloc(2); l.writeUInt16BE(b.length); return Buffer.concat([l, b]) }
function encode(o) {
  const parts = [Buffer.from([0x0a])]
  for (const k of Object.keys(o)) {
    const v = o[k]
    if (typeof v === 'number') { const b = Buffer.alloc(4); b.writeInt32BE(v); parts.push(Buffer.from([0x03]), str(k), b) } else parts.push(Buffer.from([0x08]), str(k), str(String(v)))
  }
  parts.push(Buffer.from([0x00]))
  return Buffer.concat(parts)
}

async function waitDialog(bot, re, timeout = 8000) {
  return h.waitDialog(bot, d => re.test(h.dialogText(d)), timeout)
}

/** Stan gracza z serwera: tryb gry i swiat (konsola tylko obserwuje). */
async function stan(name) {
  const gm = await rcon(`minecraft:data get entity ${name} playerGameType`)
  const dim = await rcon(`minecraft:data get entity ${name} Dimension`)
  const pos = await rcon(`minecraft:data get entity ${name} Pos`)
  return { gm: (gm.match(/: (\d)/) || [])[1], dim: (dim.match(/"([^"]+)"/) || [])[1], pos: (pos.match(/\[(.*)\]/) || [])[1] }
}

module.exports = { join, waitScene, press, lpm, ppm, inputs, clickWith, waitDialog, stan, rcon, shield, sleep, h }
