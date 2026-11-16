'use strict'
/**
 * Szybki eval TRZECH KUZNI SPRZETOWYCH (arcane-forge, mythical-forge,
 * steel-crafting-station). W odroznieniu od 13 warsztatow profesyjnych te stacje
 * maja warunek 'level{level=N}' (poziom POSTACI, nie profesji), a dwie z nich
 * w ogole nie definiuja sekcji 'command', wiec nie da sie ich otworzyc komenda.
 *
 * Sprawdzamy tylko to, co da sie sprawdzic komenda: czy GUI sie otwiera i co jest
 * w slotach. Pelnego craftu tu nie testujemy - skladniki (HUMAN_SOUL, MAGIC_FRAGMENT,
 * STEEL_INGOT) to inny lancuch niz profesje.
 */
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')

const NICK = process.env.EVAL_NICK || 'EvKuznia'
const sleep = ms => new Promise(r => setTimeout(r, ms))
const label = i => { try { return H.itemLabel(i) } catch { return '' } }

const KUZNIE = [
  { plik: 'arcane-forge.yml',            cmd: 'arcaneforge', maQueue: true },
  { plik: 'mythical-forge.yml',          cmd: null,          maQueue: true },
  { plik: 'steel-crafting-station.yml',  cmd: null,          maQueue: true },
]

class R {
  constructor() { this.rows = [] }
  assert(c, l, e) { this.rows.push({ pass: !!c, l, e }); return !!c }
  print() {
    console.log('\n================ WYNIKI KUZNI ================')
    for (const x of this.rows) console.log(`${x.pass ? 'PASS' : 'FAIL'} ${x.l}${x.e ? ' — ' + x.e : ''}`)
    const f = this.rows.filter(x => !x.pass).length
    console.log(f ? `\nWYNIK: ${f} NIEPOWODZEN / ${this.rows.length}` : `\nWYNIK: wszystko PASS (${this.rows.length})`)
    return f
  }
}

;(async () => {
  const r = new R()
  const bot = await H.connect(NICK, { timeout: 90000 })
  try {
    try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) }
    catch (e) { console.log('(kreator: ' + e.message + ')') }
    await sleep(3000)
    bot.chat(`/minecraft:execute in minecraft:overworld run tp ${NICK} 150.5 99 197.5`); await sleep(2500)
    bot.chat(`/mmocore admin level set ${NICK} 30`); await sleep(1000)

    for (const k of KUZNIE) {
      if (!k.cmd) {
        r.assert(false, `KUZNIA ${k.plik}: da sie otworzyc komenda`,
          'plik NIE ma sekcji command.name — stacji nie da sie otworzyc zadna komenda gracza')
        continue
      }
      if (bot.currentWindow) { try { bot.closeWindow(bot.currentWindow) } catch {} await sleep(600) }
      bot.chat('/' + k.cmd)
      const win = await H.waitForWindow(bot, { timeout: 9000 })
      if (!r.assert(!!win, `KUZNIA ${k.plik}: /${k.cmd} otwiera GUI`,
          bot.evalLog.slice(-2).join(' / '))) continue
      const sloty = H.windowSlots(win).map(s => `${s.slot}:${(s.label || s.name || '').slice(0, 30)}`)
      console.log(`  ${k.cmd} okno "${H.windowTitle(win)}": ${sloty.join(' | ')}`)
      r.assert(sloty.some(s => /Craft|Wytw/i.test(s)), `KUZNIA ${k.plik}: sa receptury w GUI`,
        sloty.slice(0, 8).join(' | '))
      if (bot.currentWindow) { try { bot.closeWindow(bot.currentWindow) } catch {} await sleep(600) }
    }
  } finally { await H.quit(bot) }
  process.exit(r.print() ? 1 : 0)
})().catch(e => { console.log('FAIL przerwane — ' + e.message); process.exit(2) })
