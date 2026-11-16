'use strict'
/** Eksperyment: czy ~onInteract odpala, gdy bot wysle INTERACT_AT (mouse=2) zamiast INTERACT (mouse=0). */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const NICK = process.env.EVAL_NICK || 'EvInt'
const X = 150.5, Y = 99, Z = 215.5
const sleep = ms => new Promise(r => setTimeout(r, ms))
const saw = (bot, m) => bot.evalLog.some(x => (x || '').includes(m))

;(async () => {
  const bot = await H.connect(NICK, { timeout: 90000 })
  const rows = []
  const rec = (p, l, e) => { rows.push({p:!!p,l,e}); return !!p }
  try {
    try { await F.waitProfileScreen(bot); const w = await F.enterCreator(bot); if (w) await F.completeCreator(bot) } catch (e) {}
    await sleep(3000)
    await sleep(1500)
    bot.chat(`/mm mobs spawn ZZTESTINTERACT 1 world,149,99,197`); await sleep(3000)

    const find = () => Object.values(bot.entities)
      .filter(e => e.name === 'cow' && e.position.distanceTo(bot.entity.position) < 10)
      .sort((a,b)=>a.position.distanceTo(bot.entity.position)-b.position.distanceTo(bot.entity.position))[0]
    let e = find()
    if (!rec(!!e,'mob testowy (COW) znaleziony','krow w zasiegu: '+Object.values(bot.entities).filter(x=>x.name==='cow').length)) throw new Error('brak moba')

    await bot.lookAt(e.position.offset(0,1,0), true); await sleep(500)

    // A: INTERACT (mouse=0) - to robил dotychczasowy test i bot.openEntity (handel z wiesniakiem)
    bot.evalLog.length = 0
    await bot.activateEntity(e); await sleep(2500)
    const a = saw(bot,'ZZMARK_INTERACT_FIRED')
    rec(!a, 'A) activateEntity mouse=0 (INTERACT): trigger MILCZY (oczekiwane)', a?'ODPALIL':'cisza')

    // B: INTERACT_AT (mouse=2) - to wysyla prawdziwy klient vanilla
    bot.evalLog.length = 0
    await bot.activateEntityAt(e, e.position.offset(0,1,0)); await sleep(2500)
    const b = saw(bot,'ZZMARK_INTERACT_FIRED')
    rec(b, 'B) activateEntityAt mouse=2 (INTERACT_AT): trigger ODPALA', b?'ZZMARK_INTERACT_FIRED':'cisza — '+bot.evalLog.slice(-3).join(' / '))

    // C: kontrola - uderzenie
    bot.evalLog.length = 0
    bot.attack(e); await sleep(2500)
    const c = saw(bot,'ZZMARK_DAMAGED_FIRED')
    rec(c, 'C) kontrola: ~onDamaged odpala', c?'ok':'cisza')
  } catch (err) { console.log('BLAD: '+err.message) }
  finally {
    for (const x of rows) console.log(`${x.p?'PASS':'FAIL'} ${x.l}${x.e?' — '+x.e:''}`)
    console.log(rows.filter(x=>!x.p).length ? 'WYNIK: sa FAIL' : 'WYNIK: wszystko PASS')
    await H.quit(bot)
  }
  process.exit(0)
})().catch(e=>{console.log('przerwane: '+e.message);process.exit(2)})
