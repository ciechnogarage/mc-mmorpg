'use strict'
/** Kontrola: jak mineflayer numeruje efekty na tym serwerze + czy potrawy nadaja buffy.
 *  Najpierw KONTROLA ze znanym efektem (minecraft:speed), potem potrawy. */
const H=require('/e/lib/harness'), F=require('/e/lib/flow')
const NICK=process.env.EVAL_NICK||'EvEf'
const sleep=ms=>new Promise(r=>setTimeout(r,ms))
const label=i=>{try{return H.itemLabel(i)}catch{return (i&&i.displayName)||''}}
async function cmd(bot,c,w=900){bot.chat('/'+c);await sleep(w)}
const dump=bot=>JSON.stringify(bot.entity.effects||{})
async function wyczysc(bot){await cmd(bot,`minecraft:effect clear ${NICK}`,1200)}
;(async()=>{
  const bot=await H.connect(NICK,{timeout:90000})
  try{
    try{await F.waitProfileScreen(bot);const w=await F.enterCreator(bot);if(w)await F.completeCreator(bot)}catch(e){}
    await sleep(3000)
    await wyczysc(bot)
    console.log('po wyczyszczeniu:', dump(bot))
    console.log('\n=== KONTROLA: znane efekty z komendy ===')
    for(const e of ['speed','strength','resistance','absorption','regeneration','water_breathing']){
      await wyczysc(bot)
      await cmd(bot,`minecraft:effect give ${NICK} minecraft:${e} 200 0 true`,1600)
      console.log(`  minecraft:${e.padEnd(16)} -> ${dump(bot)}`)
    }
    console.log('\n=== POTRAWY ===')
    for(const [id,nazwa,oczek] of [
      ['PROF_TRAIL_RATION','Racja podróżna',1],
      ['PROF_HUNTER_ROAST','Pieczeń myśliwego',1],
      ['PROF_MASTER_BANQUET','Mistrzowska uczta',4]]){
      await wyczysc(bot)
      await cmd(bot,`minecraft:clear ${NICK}`,900)
      await cmd(bot,`mi give CONSUMABLE ${id} ${NICK} 1`,1400)
      const it=bot.inventory.items().find(i=>(label(i)||'').includes(nazwa))
      if(!it){console.log(`  ${id}: BRAK PRZEDMIOTU`);continue}
      await bot.equip(it,'hand');await sleep(700)
      try{await bot.activateItem()}catch(e){}
      await sleep(3000)
      try{bot.deactivateItem()}catch{}
      await sleep(2000)
      const ef=bot.entity.effects||{}
      const n=Object.keys(ef).length
      console.log(`  ${id.padEnd(20)} efektow=${n} (oczekiwano ${oczek}) ${n>=oczek?'OK':'ZLE'}  ${JSON.stringify(ef)}`)
    }
  } finally { await H.quit(bot) }
  process.exit(0)
})().catch(e=>{console.log('FAIL — '+e.message);process.exit(2)})
