'use strict'
/**
 * Rekonesans, nie scenariusz: drukuje OS CZASU zmian wlasnosci 'textures' dla konta
 * podanego w EVAL_USER. Kazda zmiana jest tlumaczona na id skorki z skins.yml (albo na
 * sam hash tekstury, gdy to nie jest skorka postaci) — dzieki temu widac nie tylko CO
 * gracz dostal, ale tez ILE RAZY i KIEDY ktos to nadpisal.
 *
 * Uzycie: EVAL_USER=<nick> [EVAL_SLOT=<slot>] node tools-skin-timeline.js
 */
const fs = require('fs')
const H = require('./lib/harness')
const F = require('./lib/flow')

const USER = process.env.EVAL_USER
const WATCH_MS = +(process.env.EVAL_WATCH_MS || 30000)

function skinIndex() {
  const out = {}
  let current = null
  for (const line of fs.readFileSync('/res/skins.yml', 'utf8').split('\n')) {
    const head = line.match(/^ {2}([a-z0-9_]+):\s*$/)
    if (head) { current = head[1]; continue }
    const value = line.match(/^ {4}value:\s*'(.+)'\s*$/)
    if (value && current) out[value[1]] = current
  }
  return out
}

/** Hash tekstury z zakodowanej wartosci — to on identyfikuje PLIK skorki u Mojanga. */
function hashOf(value) {
  try {
    const json = JSON.parse(Buffer.from(value, 'base64').toString('utf8'))
    const url = json.textures && json.textures.SKIN && json.textures.SKIN.url
    return url ? url.split('/').pop() : '(brak SKIN)'
  } catch { return '(nieczytelne)' }
}

const t0 = Date.now()
const stamp = () => ((Date.now() - t0) / 1000).toFixed(1).padStart(5) + 's'

function watch(bot, known, label) {
  let last = null
  bot._client.on('player_info', (packet) => {
    const rows = packet.data || packet.players || []
    for (const row of Array.isArray(rows) ? rows : [rows]) {
      const player = row.player || row
      const name = player.name || row.name
      const props = player.properties || row.properties
      if (name !== bot.username || !props) continue
      for (const prop of props) {
        if ((prop.key || prop.name) !== 'textures' || !prop.value) continue
        if (prop.value === last) continue
        last = prop.value
        const id = known[prop.value]
        console.log(`${stamp()} ${label} skorka -> ${id ? 'skins.yml:' + id : 'OBCA ' + hashOf(prop.value)}`)
      }
    }
  })
  return { last: () => last }
}

;(async () => {
  const known = skinIndex()
  console.log('znanych skorek postaci w skins.yml:', Object.keys(known).length)
  const bot = await H.connect(USER)
  console.log(`${stamp()} wszedl jako ${USER}`)
  const w = watch(bot, known, USER)
  try {
    const list = await F.waitProfileScreen(bot, 30000)
    if (list) {
      const slots = H.windowSlots(bot.currentWindow).map(s => `${s.slot}:${s.label}`)
      console.log(`${stamp()} ekran wyboru postaci: ${slots.join(' | ')}`)
      const wanted = process.env.EVAL_SLOT
        ? H.windowSlots(bot.currentWindow).find(s => s.slot === +process.env.EVAL_SLOT)
        : H.findSlot(bot.currentWindow, /^\[1\]|Profile n1/i)
      if (wanted) {
        console.log(`${stamp()} klikam slot ${wanted.slot} (${wanted.label})`)
        await H.click(bot, wanted.slot)
      } else {
        console.log(`${stamp()} BRAK pasujacego slotu — tylko obserwuje`)
      }
    } else {
      console.log(`${stamp()} ekran wyboru sie nie pojawil — tylko obserwuje`)
    }
    const deadline = Date.now() + WATCH_MS
    while (Date.now() < deadline) await H.sleep(500)
    console.log(`${stamp()} koniec obserwacji; ostatnia skorka: ` +
      (w.last() ? (known[w.last()] ? 'skins.yml:' + known[w.last()] : 'OBCA ' + hashOf(w.last())) : 'ZADNA'))
  } finally { await H.quit(bot) }
})().catch(e => { console.error('BLAD:', e.message); process.exit(1) })
