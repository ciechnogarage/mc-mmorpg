'use strict'
/**
 * Co DOKLADNIE dostaje klient przy podmianie skorki wlasnego gracza.
 *
 * Vanilla przyjmuje wlasnosci profilu TYLKO przy DODAWANIU wpisu do listy graczy
 * (ClientboundPlayerInfoUpdatePacket#ADD_PLAYER robi computeIfAbsent) — sama aktualizacja
 * istniejacego wpisu jest ignorowana. Zeby skorka zmienila sie u obserwatora, serwer musi
 * wyslac player_remove, a dopiero potem player_info z wlasnoscia 'textures'. Ten skrypt
 * drukuje surowa kolejnosc tych pakietow dla WLASNEGO wpisu gracza plus respawny.
 */
const fs = require('fs')
const H = require('./lib/harness')
const F = require('./lib/flow')

const USER = process.env.EVAL_USER
const WATCH_MS = +(process.env.EVAL_WATCH_MS || 30000)
const t0 = Date.now()
const stamp = () => ((Date.now() - t0) / 1000).toFixed(1).padStart(5) + 's'

function skinIndex() {
  const out = {}; let current = null
  for (const line of fs.readFileSync('/res/skins.yml', 'utf8').split('\n')) {
    const head = line.match(/^ {2}([a-z0-9_]+):\s*$/); if (head) { current = head[1]; continue }
    const v = line.match(/^ {4}value:\s*'(.+)'\s*$/); if (v && current) out[v[1]] = current
  }
  return out
}
const shortTex = (known, value) => known[value] ? 'skins.yml:' + known[value] : 'OBCA:' + (() => {
  try { return JSON.parse(Buffer.from(value, 'base64').toString()).textures.SKIN.url.split('/').pop().slice(0, 12) } catch { return '?' }
})()

;(async () => {
  const known = skinIndex()
  const bot = await H.connect(USER)
  const me = bot.username
  let uuid = null
  console.log(`${stamp()} wszedl jako ${me}`)

  bot._client.on('player_info', (p) => {
    const rows = p.data || p.players || []
    for (const row of (Array.isArray(rows) ? rows : [rows])) {
      const player = row.player || row
      const name = player.name || row.name
      const id = row.uuid || row.UUID || player.uuid
      if (name === me && id) uuid = id
      const isMe = name === me || (uuid && id === uuid)
      if (!isMe) continue
      const props = player.properties || row.properties || []
      const tex = props.find(x => (x.key || x.name) === 'textures')
      console.log(`${stamp()} player_info action=${p.action} name=${name || '(brak)'} uuid=${id || '(brak)'}` +
        ` textures=${tex ? shortTex(known, tex.value) : 'BRAK'}`)
    }
  })
  bot._client.on('player_remove', (p) => {
    for (const id of p.players || []) if (!uuid || id === uuid) console.log(`${stamp()} player_remove uuid=${id}${id === uuid ? ' (TO JA)' : ''}`)
  })
  bot._client.on('respawn', () => console.log(`${stamp()} respawn (przeladowanie wlasnej encji)`))

  try {
    if (process.env.EVAL_VIA_PROXY === '1') {
      // Wejscie jak u czlowieka: na proxy, potem /server world1. Ta sciezka dokłada
      // Velocity miedzy serwer a klienta — pytanie brzmi, czy przepuszcza remove+add.
      console.log(`${stamp()} /server ${H.TARGET}`)
      const moved = await H.switchServer(bot)
      console.log(`${stamp()} przeniesiony: ${moved}`)
    }
    const list = await F.waitProfileScreen(bot, 30000)
    if (list) {
      // Czlowiek siedzi na ekranie wyboru kilkanascie sekund. Bot klikajacy w 0.5 s nie
      // zobaczy nadpisania, ktore przychodzi pozniej — stad ta pauza.
      const wait = +(process.env.EVAL_DELAY_MS || 0)
      if (wait) { console.log(`${stamp()} czekam ${wait} ms na ekranie wyboru`); await H.sleep(wait) }
      const wanted = process.env.EVAL_SLOT
        ? H.windowSlots(bot.currentWindow).find(s => s.slot === +process.env.EVAL_SLOT)
        : H.findSlot(bot.currentWindow, /^\[1\]|Profile n1/i)
      if (wanted) { console.log(`${stamp()} klikam slot ${wanted.slot} (${wanted.label})`); await H.click(bot, wanted.slot) }
    }
    const deadline = Date.now() + WATCH_MS
    while (Date.now() < deadline) await H.sleep(500)
  } finally { await H.quit(bot) }
})().catch(e => { console.error('BLAD:', e.message); process.exit(1) })
