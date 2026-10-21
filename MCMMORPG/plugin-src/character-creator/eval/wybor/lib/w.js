'use strict'
/**
 * Harness evali WYBORU POSTACI (agent WYBOR, 2026-09-30). Na bazie lib/h.js (Chowance).
 * Bot wchodzi na klon cc-nap-wybor jak gracz: klawisze sceny idą pakietem player_input (jak u klienta
 * w spectatorze), LPM = atak w encję sceny, PPM = interakcja, okna Papera = show_dialog + custom_click_action.
 * Pola okien (checkboxy, tekst) klient dokleja do ładunku kliknięcia — tu robimy to samo.
 */
const h = require('./h')
const { sleep } = h

const HOST = process.env.MC_HOST || 'cc-nap-wybor'
const RCON = { host: HOST, port: 25575, pass: 'wybor' }
const rcon = cmd => h.rconSend(cmd, RCON)

async function connect(name) {
  const bot = await h.connect(name)
  bot.on('error', () => {})
  // mineflayer nie emituje 'actionBar' dla paska w formie NBT (komponent z 'extra') — czytamy pakiet sami.
  const nbt = require('prismarine-nbt')
  bot._client.on('action_bar', p => {
    try {
      const t = h.plain(p.text && p.text.type ? nbt.simplify(p.text) : p.text)
      if (t) bot.log.push('[AB] ' + t)
    } catch {}
  })
  return bot
}

/** Krótkie wciśnięcie klawisza ruchu (zbocze narastające) — jak klient w spectatorze. */
async function press(bot, key) {
  const inputs = {}
  inputs[key] = true
  bot._client.write('player_input', { inputs })
  await sleep(150)
  bot._client.write('player_input', { inputs: {} })
  await sleep(350)
}

function sceneEntity(bot) {
  // Tarcza klików (interaction) przed kamerą — widoczna tylko dla gracza w scenie.
  const all = Object.values(bot.entities).filter(e => e !== bot.entity)
  const inter = all.filter(e => e.name === 'interaction')
  // Najnowsza tarcza (najwyzsze id): po ponownym otwarciu sceny stara encja moze wisiec w bot.entities,
  // a pozycja bota w spectatorze bywa nieaktualna — odleglosc wskazywala zla encje.
  const pick = (inter.length ? inter : all)
  pick.sort((a, b) => b.id - a.id)
  return pick[0]
}

async function lpm(bot) {
  const e = sceneEntity(bot)
  if (!e) return false
  bot.attack(e)
  await sleep(600)
  return true
}

async function ppm(bot) {
  const e = sceneEntity(bot)
  if (!e) return false
  // Klient przy interakcji z encją wysyła INTERACT_AT i INTERACT (notatka mythicmobs-cicha-walidacja).
  bot._client.write('use_entity', { target: e.id, mouse: 2, x: 0, y: 1, z: 0, hand: 0, sneaking: false })
  bot._client.write('use_entity', { target: e.id, mouse: 0, hand: 0, sneaking: false })
  await sleep(700)
  return true
}

function buttons(bot) { return h.dialogButtons(bot.dialog) }
function text(bot) { return h.dialogText(bot.dialog) }

/** Pola okna: [{key, type, label}] (bool/text). */
function inputs(d) {
  if (!d || !Array.isArray(d.inputs)) return []
  return d.inputs.map(i => ({ key: i.key, type: String(i.type || ''), label: h.plain(i.label) }))
}

/**
 * Klik w przycisk okna z wartościami pól. values: {key: true|false|'tekst'}.
 * Pakiet składany ręcznie jak w h.clickDialog (serwer czyta lengthPrefixed NBT).
 */
async function click(bot, re, values = {}) {
  const b = buttons(bot).find(x => typeof re === 'string' ? x.label === re : re.test(x.label))
  if (!b || !b.id) return false
  const payload = Object.assign({}, b.payload && typeof b.payload === 'object' ? b.payload : {})
  for (const i of inputs(bot.dialog)) {
    if (i.key in values) payload[i.key] = values[i.key]
    else if (/bool/.test(i.type)) payload[i.key] = false
    else if (/text/.test(i.type)) payload[i.key] = ''
  }
  bot.dialog = null
  const nbtBuf = encodeRoot(payload)
  const id = Buffer.from(b.id, 'utf8')
  bot._client.writeRaw(Buffer.concat([varint(0x41), varint(id.length), id, varint(nbtBuf.length), nbtBuf]))
  await sleep(900)
  return true
}

function varint(n) {
  const out = []
  let v = n >>> 0
  while (v > 0x7f) { out.push((v & 0x7f) | 0x80); v >>>= 7 }
  out.push(v)
  return Buffer.from(out)
}
function str(s) { const b = Buffer.from(s, 'utf8'); const l = Buffer.alloc(2); l.writeUInt16BE(b.length); return Buffer.concat([l, b]) }
function tag(v) {
  if (typeof v === 'boolean') return [0x01, Buffer.from([v ? 1 : 0])]
  if (typeof v === 'string') return [0x08, str(v)]
  if (typeof v === 'number' && Number.isInteger(v)) { const b = Buffer.alloc(4); b.writeInt32BE(v); return [0x03, b] }
  if (typeof v === 'number') { const b = Buffer.alloc(8); b.writeDoubleBE(v); return [0x06, b] }
  if (v && typeof v === 'object') return [0x0a, body(v)]
  return [0x01, Buffer.from([0])]
}
function body(o) {
  const parts = []
  for (const k of Object.keys(o)) { const [t, b] = tag(o[k]); parts.push(Buffer.from([t]), str(k), b) }
  parts.push(Buffer.from([0]))
  return Buffer.concat(parts)
}
function encodeRoot(o) { return Buffer.concat([Buffer.from([0x0a]), body(o)]) }

/** Ładunek przycisku przychodzi jako SNBT-string albo obiekt — ujednolicenie. */
function normalizePayloads(bot) {
  for (const b of buttons(bot)) {
    if (typeof b.payload === 'string') {
      const m = b.payload.match(/n:(\d+).*b:(\d+)/)
      if (m) b.payload = { n: +m[1], b: +m[2] }
    }
  }
}

async function waitDialog(bot, pred = () => true, timeout = 10000) {
  return h.waitDialog(bot, d => pred(d), timeout)
}

/** Linie logu serwera klonu od znacznika (liczymy parserem, nie grepem po nicku). */
const fs = require('fs')
const LOG = process.env.CLONE_LOG || '/clone/logs/latest.log'
function logSince(offset) {
  // Przesuniecie w BAJTACH (statSync.size) — tnij bufor, nie napis (polskie znaki = 2 bajty).
  return fs.readFileSync(LOG).subarray(offset).toString('utf8')
}
function logOffset() { return fs.statSync(LOG).size }

module.exports = { connect, press, lpm, ppm, buttons, text, inputs, click, waitDialog, rcon, sleep, logSince, logOffset, plain: h.plain, quit: h.quit, Results: h.Results, normalizePayloads }
