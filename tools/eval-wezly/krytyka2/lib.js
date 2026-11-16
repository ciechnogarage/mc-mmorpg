'use strict'
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')
const { oczysc } = require('/w/xp.js')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const label = i => { try { return H.itemLabel(i) } catch { return (i && i.displayName) || '' } }
const czyste = bot => bot.evalLog.map(oczysc).filter(l => !/[\u2764\u2666\u26e8]/.test(l))

async function gotowy(nick) {
  const bot = await H.connect(nick, { timeout: 90000 })
  bot.nick = nick
  try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) }
  catch (e) { console.log(`(${nick} kreator: ${e.message})`) }
  await sleep(2500)
  return bot
}
async function cmd(bot, c, w = 900) { bot.evalLog.length = 0; bot.chat('/' + c); await sleep(w); return czyste(bot).join(' | ') }
async function xp(bot, prof, nick = bot.nick) {
  const t = await cmd(bot, `mmocore admin exp check ${nick} ${prof}`, 1500)
  const m = t.match(/has\s+([0-9]+(?:[.,][0-9]+)?)\s+EXP/i)
  return m ? parseFloat(m[1].replace(',', '.')) : NaN
}
async function tp(bot, x, y, z) {
  // UWAGA: 'execute in minecraft:overworld run tp' z czatu bota NIE przenosi go (zmierzone: brak pakietu pozycji,
  // Pos na serwerze bez zmian). Dziala zwykle 'minecraft:tp' + reczne ustawienie pozycji po stronie bota.
  bot.chat(`/minecraft:tp ${bot.nick} ${x} ${y} ${z}`)
  await sleep(1200)
  bot.entity.position.set(x, y, z)
  await sleep(800)
  return true
}
const SUROWIEC = /Zioło|Liść|kapelusz|ziele|zbiór|kryształ|kamień|glin|drewno|Kość|runo|Łup|Worek|Trofeum|Dusza|Fragment/i
const suma = bot => bot.inventory.items().filter(i => SUROWIEC.test(label(i) || '')).reduce((a, i) => a + i.count, 0)
const inv = bot => bot.inventory.items().map(i => `${i.count}x ${label(i)}`).join(' | ') || '(pusty)'
const zombie = (bot, pos, r = 1.5) => Object.values(bot.entities)
  .filter(e => e.name === 'zombie' && e.position.distanceTo(pos) < r)
async function spawnWezel(bot, typ, x, y, z) {
  const przed = new Set(Object.keys(bot.entities))
  bot.chat(`/mm m spawn ${typ} 1 world,${x},${y},${z}`)
  for (let i = 0; i < 20; i++) {
    await sleep(250)
    const e = Object.values(bot.entities).find(e => e.name === 'zombie' && !przed.has(String(e.id)) &&
      Math.abs(e.position.x - x) < 1 && Math.abs(e.position.z - z) < 1)
    if (e) return e
  }
  return null
}
// INTERACT_AT wprost pakietem - bez czekania na obrot glowy, zeby dwa boty trafily w ten sam tick
function klik(bot, e) {
  bot._client.write('use_entity', { target: e.id, mouse: 2, sneaking: false, hand: 0,
    x: 0, y: 0.5, z: 0, location: { x: 0, y: 0.5, z: 0 } })
}
module.exports = { klik, SUROWIEC, H, F, sleep, label, czyste, gotowy, cmd, xp, tp, suma, inv, zombie, spawnWezel }
