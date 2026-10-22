'use strict'
/**
 * Dokladny pomiar progow poziomow profesji.
 * Krok 0: weryfikacja, ze %mmocore_profession_<p>% to NAPRAWDE poziom
 *         (ustawiamy znany poziom i czytamy) - inaczej caly pomiar klamie.
 * Krok 1: reset do poziomu 1 / 0 XP, potem porcje po 100 XP i odczyt
 *         poziomu oraz XP po kazdej porcji.
 */
const H=require('/e/lib/harness'), F=require('/e/lib/flow')
const {oczysc}=require('/w/xp.js')
const NICK=process.env.EVAL_NICK||'EvProg'
const P=process.env.EVAL_PROF||'mining'
const KROK=parseInt(process.env.EVAL_KROK||'100')
const ILE=parseInt(process.env.EVAL_ILE||'30')
const sleep=ms=>new Promise(r=>setTimeout(r,ms))
const czyste=bot=>bot.evalLog.map(oczysc).filter(l=>!/[❤♦⛨]/.test(l))
async function cmd(bot,c,w=700){bot.evalLog.length=0;bot.chat('/'+c);await sleep(w);return czyste(bot).join(' | ')}
async function papi(bot,ph,w=1200){
  const t=await cmd(bot,`papi parse ${NICK} ${ph}`,w)
  const m=t.match(/(-?[0-9]+(?:[.,][0-9]+)?)\s*$/m)
  return m?parseFloat(m[1].replace(',','.')):NaN
}
;(async()=>{
  const bot=await H.connect(NICK,{timeout:90000})
  try{
    try{await F.waitProfileScreen(bot);const w=await F.enterCreator(bot);if(w)await F.completeCreator(bot)}
    catch(e){console.log('(kreator: '+e.message+')')}
    await sleep(3000)

    console.log('=== krok 0: czy placeholder to poziom? ===')
    for(const L of [7,23,41]){
      await cmd(bot,`mmocore admin level set ${NICK} ${P} ${L}`,800)
      const v=await papi(bot,`%mmocore_profession_${P}%`)
      console.log(`  ustawiono poziom ${L} -> placeholder zwraca ${v}  ${v===L?'OK':'!!! NIEZGODNE'}`)
    }

    console.log(`\n=== krok 1: progi dla ${P}, porcje po ${KROK} XP ===`)
    await cmd(bot,`mmocore admin level set ${NICK} ${P} 1`,800)
    await cmd(bot,`mmocore admin exp set ${NICK} ${P} 0`,800)
    let suma=0, poprz=await papi(bot,`%mmocore_profession_${P}%`)
    console.log(`  start: poziom ${poprz}`)
    console.log('  nadane_lacznie  poziom  XP_w_poziomie   koszt_poziomu')
    for(let i=0;i<ILE;i++){
      await cmd(bot,`mmocore admin exp give ${NICK} ${P} ${KROK}`,650)
      suma+=KROK
      const L=await papi(bot,`%mmocore_profession_${P}%`)
      const e=await papi(bot,`%mmocore_profession_experience_${P}%`)
      const awans=(L>poprz)?`  <-- AWANS na ${L}`:''
      console.log(`  ${String(suma).padStart(13)}  ${String(L).padStart(6)}  ${String(e).padStart(13)}${awans}`)
      poprz=L
      if(L>=50) break
    }
    console.log('\n  wzor z configu: 150 + {level}*75 + {level}^2*12')
    console.log('  gdyby dzialal: awans 1->2 po 237 XP, 2->3 po 348 XP, 3->4 po 483 XP')
    console.log('  gdyby wzor NIE dzialal (fallback z jara): 100 XP na kazdy poziom')
  } finally { await H.quit(bot) }
  process.exit(0)
})().catch(e=>{console.log('FAIL przerwane — '+e.message);process.exit(2)})
