'use strict'
/** Czy ~onInteract odpala na NIEWIDZIALNYM ZOMBIE (dokladnie taki nosnik jak nasze wezly)? */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const NICK = process.env.EVAL_NICK || 'EvInt2'
const sleep = ms => new Promise(r => setTimeout(r, ms))
;(async () => {
  const bot = await H.connect(NICK, { timeout: 90000 })
  let fired=false, tried=0
  try {
    try { await F.waitProfileScreen(bot); const w=await F.enterCreator(bot); if(w) await F.completeCreator(bot) } catch(e){}
    await sleep(4000)
    bot.chat(`/mm mobs spawn ZZTESTNODE 1 world,149,99,197`); await sleep(3000)
    const zs = Object.values(bot.entities)
      .filter(e => e.name==='zombie' && e.position.distanceTo(bot.entity.position) < 8)
      .sort((a,b)=>a.position.distanceTo(bot.entity.position)-b.position.distanceTo(bot.entity.position))
    console.log('niewidzialnych zombie w zasiegu: '+zs.length)
    for (const e of zs) {
      tried++
      await bot.lookAt(e.position.offset(0,1,0), true); await sleep(300)
      bot.evalLog.length = 0
      await bot.activateEntityAt(e, e.position.offset(0,1,0)); await sleep(1600)
      if (bot.evalLog.some(x=>(x||'').includes('ZZNODE_INTERACT_FIRED'))) { fired=true; break }
    }
  } catch(err){ console.log('BLAD: '+err.message) }
  finally {
    console.log(`${fired?'PASS':'FAIL'} ~onInteract na NIEWIDZIALNYM ZOMBIE (nosnik wezla) — sprawdzono ${tried} encji`)
    await H.quit(bot)
  }
  process.exit(0)
})().catch(e=>{console.log('przerwane: '+e.message);process.exit(2)})
