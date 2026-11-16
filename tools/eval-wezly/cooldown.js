'use strict'
/**
 * Czy 7-dniowy cooldown zmiany profesji w ogole da sie "przeczekac"?
 * Menu wymaga, by placeholder timestampu byl DOSLOWNIE 'Expired'.
 * Sprawdzamy jego wartosc: u swiezego gracza (default 0) i po ustawieniu 7day.
 */
const H=require('/e/lib/harness'), F=require('/e/lib/flow')
const {oczysc}=require('/w/xp.js')
const NICK=process.env.EVAL_NICK||'EvCd'
const sleep=ms=>new Promise(r=>setTimeout(r,ms))
const czyste=b=>b.evalLog.map(oczysc).filter(l=>!/[❤♦⛨]/.test(l))
async function cmd(b,c,w=1000){b.evalLog.length=0;b.chat('/'+c);await sleep(w);return czyste(b).join(' | ')}
;(async()=>{
  const bot=await H.connect(NICK,{timeout:90000})
  try{
    try{await F.waitProfileScreen(bot);const w=await F.enterCreator(bot);if(w)await F.completeCreator(bot)}
    catch(e){console.log('(kreator: '+e.message+')')}
    await sleep(3000)
    const PH=['%coretools_var_timestamp_player_default_profession_slot_1_cooldown%',
              '%coretools_var_player_profession_slot_1%',
              '%coretools_var_player_profession_free_change_used%',
              '%coretools_var_player_profession_saved_mining_level%']
    console.log('=== swiezy gracz (wartosci domyslne) ===')
    for(const p of PH) console.log(`  ${p}\n     -> ${JSON.stringify((await cmd(bot,`papi parse ${NICK} ${p}`,1300)).slice(-120))}`)
    console.log('\n=== po wyborze mining i wyczyszczeniu slotu (ustawia cooldown 7day) ===')
    await cmd(bot,`core-variables set profession_slot_1 mining ${NICK}`,1000)
    await cmd(bot,`mmocore admin level set ${NICK} mining 23`,900)
    await cmd(bot,`corecast profession_clear_slot_1_mining ${NICK}`,1600)
    for(const p of PH) console.log(`  ${p}\n     -> ${JSON.stringify((await cmd(bot,`papi parse ${NICK} ${p}`,1300)).slice(-120))}`)
    console.log('\n  Menu przepusci platna zmiane TYLKO gdy pierwszy placeholder == "Expired".')
  } finally { await H.quit(bot) }
  process.exit(0)
})().catch(e=>{console.log('FAIL przerwane — '+e.message);process.exit(2)})
