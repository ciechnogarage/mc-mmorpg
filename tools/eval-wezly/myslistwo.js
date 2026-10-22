'use strict'
/**
 * Eval MYSLISTWA na ZYWYM world1: nasze moby z Bestiariusza.
 *
 * Lancuch: spawn moba -> bot go ZABIJA (nie konsola!) -> nasz surowiec PROF_* w plecaku
 *          -> XP profesji hunting wzroslo DOKLADNIE o wartosc z ~onDeath.
 *
 * Dlaczego bot musi zabic sam: XP leci z mechaniki
 *   command{c="mmocore admin exp give <trigger.name> hunting N"} @trigger ~onDeath ?~isplayer
 * Warunek ?~isplayer pilnuje, ze wyzwalaczem jest GRACZ. Mob, ktory zginie od ognia
 * (nieumarli plona w dzien!), od upadku albo od /kill, NIE da XP - i to jest poprawne.
 * Dlatego mierzymy XP PO KAZDYM ZABICIU osobno: sumaryczny przyrost mylil, bo
 * pojedynczy spalony zombie robil z poprawnego wyniku falszywy FAIL.
 *
 * Tabela mobow leci z moby.json (generowana parserem z Bestiariusz.yml + droptables),
 * a nie z pamieci - raz juz wpisalem 6 XP tam, gdzie config ma 9, i test sklamal.
 *
 * Czego sie nauczylismy:
 *  - CMI przechwytuje /tp i /kill -> prefiks minecraft:.
 *  - Bez gracza chunki sa wyladowane: przed operacjami na encjach forceload add.
 *  - Osierocone moby usuwa tylko 'mm m kill <TYP>'.
 *  - Bot goly bije za 1 - dajemy Sile, zeby T1 padal w 1-2 ciosy, i Odpornosc,
 *    zeby nie zginal (smierc = utrata plecaka i falszywe FAIL-e).
 *
 * EVAL_SABOTAGE=konsola - moba zabija konsola zamiast bota.
 *   Oczekiwanie: XP hunting NIE ROSNIE (warunek ?~isplayer).
 */
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')
const { odczytXp: czytajXp } = require('/w/xp.js')
const MOBY_CFG = require('/w/moby.json')

const NICK = process.env.EVAL_NICK || 'EvMysliwy'
const SABOTAZ = (process.env.EVAL_SABOTAGE || '').toLowerCase()
const X = 150, Y = 99, Z = 197
const ILE = +(process.env.EVAL_KILLS || 6)
const sleep = ms => new Promise(r => setTimeout(r, ms))

// Domyslny zestaw: po jednym mobie z kazdej rodziny tabel lupow T1 + jeden T2.
const DOMYSLNE = ['BEAST_WILD_BOAR', 'UNDEAD_GRAVE_CRAWLER', 'BEAST_CAVE_SPIDERLING', 'BEAST_MOOR_SHEEP']

const label = i => { try { return H.itemLabel(i) } catch { return (i && i.displayName) || '' } }
const invList = bot => bot.inventory.items().map(i => `${i.count}x ${label(i)}`).join(' | ') || '(pusty)'
const odczytXp = bot => czytajXp(bot, NICK, 'hunting')

class R {
  constructor() { this.rows = [] }
  assert(c, l, e) { this.rows.push({ pass: !!c, l, e }); return !!c }
  print() {
    console.log('\n================ WYNIKI MYSLISTWA ================')
    for (const x of this.rows) console.log(`${x.pass ? 'PASS' : 'FAIL'} ${x.l}${x.e ? ' — ' + x.e : ''}`)
    const f = this.rows.filter(x => !x.pass).length
    console.log(f ? `\nWYNIK: ${f} NIEPOWODZEN / ${this.rows.length}` : `\nWYNIK: wszystko PASS (${this.rows.length})`)
    return f
  }
}

const zywe = (bot, typ) => Object.values(bot.entities)
  .filter(e => e.name === typ && e.position.distanceTo(bot.entity.position) < 24)

async function testMoba(bot, r, m) {
  const P = `MYSLISTWO ${m.id}`
  console.log(`\n---------- ${P} (${m.bukkit}, ${m.hp} HP, ${m.xpHunting} XP/szt.) ----------`)

  bot.chat(`/minecraft:clear ${NICK}`); await sleep(1000)
  for (const ef of ['strength 600 9', 'resistance 600 4', 'regeneration 600 4', 'fire_resistance 600 0', 'saturation 600 4']) {
    bot.chat(`/minecraft:effect give ${NICK} minecraft:${ef} true`); await sleep(350)
  }

  let xp = await odczytXp(bot)
  if (!r.assert(Number.isFinite(xp), `${P}: odczyt XP hunting dziala`, `zwrocono ${xp}`)) return

  const przyrosty = []          // przyrost XP po KAZDYM zabiciu z osobna
  let zabite = 0
  for (let i = 0; i < ILE; i++) {
    bot.chat(`/mm m spawn ${m.id} 1 world,${X + (i % 3) - 1},${Y},${Z + 2}`)
    await sleep(1700)
    const cel = zywe(bot, m.bukkit).sort((a, b) =>
      a.position.distanceTo(bot.entity.position) - b.position.distanceTo(bot.entity.position))[0]
    if (!cel) { console.log(`  ${i + 1}: mob sie nie pojawil`); continue }

    if (SABOTAZ === 'konsola') {
      bot.chat(`/mm m kill ${m.id}`); await sleep(1800)
    } else {
      for (let c = 0; c < 14 && bot.entities[cel.id]; c++) {
        try { await bot.lookAt(cel.position.offset(0, 1, 0), true) } catch {}
        bot.attack(cel); await sleep(650)
      }
    }
    if (bot.entities[cel.id]) { console.log(`  ${i + 1}: mob przezyl`); continue }
    zabite++

    bot.chat(`/minecraft:execute at ${NICK} run tp @e[type=item,distance=..16] ${NICK}`)
    await sleep(1300)
    const xpTeraz = await odczytXp(bot)
    const d = xpTeraz - xp
    przyrosty.push(d)
    console.log(`  ${i + 1}: zabity, XP +${d}`)
    xp = xpTeraz
  }

  r.assert(zabite > 0, `${P}: bot zabil przynajmniej jednego moba`, `zabitych ${zabite}/${ILE}`)

  // --- lup ---
  const lupy = bot.inventory.items().filter(i => m.lup.includes((label(i) || '').trim()))
  const ile = lupy.reduce((a, i) => a + i.count, 0)
  r.assert(ile > 0, `${P}: nasz surowiec PROF_* wypadl`,
    `${lupy.map(i => i.count + 'x ' + label(i)).join(', ') || 'NIC z [' + m.lup.join(' / ') + ']'}; plecak: ${invList(bot).slice(0, 200)}`)

  // --- XP: kazde zabicie przez GRACZA ma dac dokladnie xpHunting ---
  const zXp = przyrosty.filter(d => d > 0)
  const bezXp = przyrosty.filter(d => d === 0).length
  if (SABOTAZ === 'konsola') {
    r.assert(zXp.length === 0, `${P}: SABOTAZ konsola — XP NIE rosnie (warunek ?~isplayer dziala)`,
      `przyrosty: [${przyrosty.join(', ')}]`)
    return
  }
  r.assert(zXp.length > 0, `${P}: zabicie przez gracza nalicza XP hunting`,
    `przyrosty po kolejnych zabiciach: [${przyrosty.join(', ')}]`)
  const zle = zXp.filter(d => Math.abs(d - m.xpHunting) > 0.001)
  r.assert(zle.length === 0, `${P}: kazde naliczenie == ${m.xpHunting} XP (z ~onDeath)`,
    `niezgodne: [${zle.join(', ')}] ze wszystkich [${przyrosty.join(', ')}]`)
  if (bezXp) console.log(`  UWAGA: ${bezXp} z ${zabite} zabic nie dalo XP — najpewniej mob zginal od ognia/upadku, nie od bota (?~isplayer)`)

  bot.chat(`/mm m kill ${m.id}`); await sleep(1200)
}

;(async () => {
  const r = new R()
  if (SABOTAZ) console.log(`### TRYB SABOTAZU: ${SABOTAZ} ###`)
  const bot = await H.connect(NICK, { timeout: 90000 })
  try {
    let rozlaczony = null
    bot.on('end', re => { rozlaczony = 'polaczenie zamkniete: ' + re })
    bot.on('kicked', re => { rozlaczony = 'kick: ' + JSON.stringify(re).slice(0, 120) })
    try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) }
    catch (e) { console.log('(kreator: ' + e.message + ')') }
    await sleep(3000)
    bot.chat(`/minecraft:execute in minecraft:overworld run tp ${NICK} ${X}.5 ${Y} ${Z}.5`); await sleep(2500)
    bot.chat(`/minecraft:gamemode survival ${NICK}`); await sleep(1000)
    bot.chat(`/minecraft:forceload add ${X} ${Z}`); await sleep(800)
    console.log('pozycja bota: ' + bot.entity.position + ' gamemode=' + bot.game.gameMode)

    const tylko = (process.env.EVAL_ONLY || '').toLowerCase()
    const lista = MOBY_CFG.filter(m => tylko
      ? tylko.split(',').some(t => m.id.toLowerCase().includes(t.trim()))
      : DOMYSLNE.includes(m.id))
    console.log('testowane moby: ' + lista.map(m => m.id).join(', '))
    for (const m of lista) {
      if (rozlaczony) { console.log(`PRZERWANO na ${m.id}: ${rozlaczony}`); break }
      try { await testMoba(bot, r, m) }
      catch (e) { r.assert(false, `MYSLISTWO ${m.id}: przypadek przerwany`, e.message) }
    }
    for (const m of lista) { bot.chat(`/mm m kill ${m.id}`); await sleep(600) }
    bot.chat(`/minecraft:forceload remove ${X} ${Z}`); await sleep(800)
  } finally { await H.quit(bot) }
  process.exit(r.print() ? 1 : 0)
})().catch(e => { console.log('FAIL przerwane — ' + e.message); process.exit(2) })
