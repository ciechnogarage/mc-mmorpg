'use strict'
/** Czy rozbicie NASZEGO bloku Nexo faktycznie daje XP gornictwa (mechanika 'custom' event BREAK)? */
const H = require('/e/lib/harness'); const F = require('/e/lib/flow')
const NICK = process.env.EVAL_NICK || 'EvNexo'
const sleep = ms => new Promise(r => setTimeout(r, ms))

async function xp(bot){ bot.evalLog.length=0; bot.chat(`/mmocore admin exp check ${NICK} mining`); await sleep(1500)
  const m = bot.evalLog.join(' | ').match(/has\s+([0-9.]+)\s+EXP/i); return m?parseFloat(m[1]):-1 }
;(async()=>{
  const bot = await H.connect(NICK,{timeout:90000}); const rows=[]
  const rec=(p,l,e)=>{rows.push({p:!!p,l,e});return !!p}
  try{
    try{ await F.waitProfileScreen(bot); const w=await F.enterCreator(bot); if(w) await F.completeCreator(bot)}catch(e){}
    await sleep(4000)
    bot.chat(`/minecraft:give ${NICK} minecraft:diamond_pickaxe 1`); await sleep(1200)
    bot.evalLog.length=0
    bot.chat(`/nexo give ${NICK} ruda_urobku 3`); await sleep(2500)
    console.log('  odpowiedz na /nexo give: '+(bot.evalLog.filter(m=>!/❤|♦|⛨/.test(m)).slice(-3).join(' | ')||'(cisza)'))
    console.log('  plecak: '+bot.inventory.items().map(i=>i.count+'x '+i.name).join(', '))
    const blk = bot.inventory.items().find(i=>i.name==='note_block')
    rec(!!blk,'blok Nexo w plecaku', blk?`${blk.count}x ${blk.name}`:bot.inventory.items().map(i=>i.name).join(','))
    if(!blk) throw new Error('brak bloku')
    const pick = bot.inventory.items().find(i=>i.name==='diamond_pickaxe')
    // znajdz wolne miejsce obok
    const base = bot.entity.position.floored()
    let ref=null, face=null
    for (const d of [[1,-1,0],[-1,-1,0],[0,-1,1],[0,-1,-1],[2,-1,0],[0,-1,2]]) {
      const p = base.offset(d[0],d[1],d[2]); const b = bot.blockAt(p)
      const above = bot.blockAt(p.offset(0,1,0))
      if (b && b.boundingBox==='block' && above && above.name==='air'){ ref=b; face=p.offset(0,1,0).minus(p); break }
    }
    if(!rec(!!ref,'znaleziono miejsce na blok', ref?JSON.stringify(ref.position):'brak')) throw new Error('brak miejsca')
    await bot.equip(blk,'hand'); await sleep(500)
    await bot.lookAt(ref.position.offset(0.5,1.5,0.5),true); await sleep(400)
    await bot.placeBlock(ref, face); await sleep(1500)
    const placed = bot.blockAt(ref.position.offset(0,1,0))
    rec(placed && placed.name==='note_block','blok postawiony', placed?placed.name:'?')

    const before = await xp(bot)
    await bot.equip(pick,'hand'); await sleep(500)
    await bot.dig(bot.blockAt(ref.position.offset(0,1,0))); await sleep(2500)
    const after = await xp(bot)
    rec(after>before, 'XP gornictwa wzroslo po rozbiciu bloku Nexo', `${before} -> ${after} EXP`)
    const drop = bot.inventory.items().some(i=>/urob|bryl|ruda/i.test((i.customName||i.displayName||'')+JSON.stringify(i.nbt||{})))
    console.log('  plecak po: '+bot.inventory.items().map(i=>i.count+'x '+i.name).join(', '))
  }catch(e){console.log('BLAD: '+e.message)}
  finally{ for(const x of rows) console.log(`${x.p?'PASS':'FAIL'} ${x.l}${x.e?' — '+x.e:''}`); await H.quit(bot) }
  process.exit(0)
})().catch(e=>{console.log('przerwane: '+e.message);process.exit(2)})
