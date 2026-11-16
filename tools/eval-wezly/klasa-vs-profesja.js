'use strict'
/**
 * DOWOD: w exp-curve profesji {level} to poziom KLASY gracza, nie poziom profesji.
 * (bajtkod: FormulaExperienceCurve$LevelPlaceholder.parse -> PlayerData.getLevel())
 * Metoda: dla kilku poziomow KLASY mierzymy, ile XP kosztuje jeden poziom profesji.
 * Oczekiwanie przy wzorze '150 + {level}*75 + {level}^2*12':
 *   klasa 1  -> 237 XP/poziom      klasa 20 -> 6450       klasa 50 -> 33900
 */
const H=require('/e/lib/harness'), F=require('/e/lib/flow')
const {oczysc}=require('/w/xp.js')
const NICK=process.env.EVAL_NICK||'EvKl'
const P='mining'
const sleep=ms=>new Promise(r=>setTimeout(r,ms))
const czyste=bot=>bot.evalLog.map(oczysc).filter(l=>!/[❤♦⛨]/.test(l))
async function cmd(bot,c,w=700){bot.evalLog.length=0;bot.chat('/'+c);await sleep(w);return czyste(bot).join(' | ')}
async function papi(bot,ph,w=1100){
  const t=await cmd(bot,`papi parse ${NICK} ${ph}`,w)
  const m=t.match(/(-?[0-9]+(?:[.,][0-9]+)?)\s*$/m)
  return m?parseFloat(m[1].replace(',','.')):NaN
}
const wzor=L=>150+L*75+L*L*12
;(async()=>{
  const bot=await H.connect(NICK,{timeout:90000})
  try{
    try{await F.waitProfileScreen(bot);const w=await F.enterCreator(bot);if(w)await F.completeCreator(bot)}
    catch(e){console.log('(kreator: '+e.message+')')}
    await sleep(3000)
    console.log('klasa   koszt_1_poziomu_profesji_ZMIERZONY   wzor_dla_{level}=klasa   XP_do_profesji_50')
    console.log('-'.repeat(92))
    for(const K of [1,10,25,50]){
      await cmd(bot,`mmocore admin level set ${NICK} main ${K}`,900)
      const kl=await papi(bot,'%mmocore_level%')
      await cmd(bot,`mmocore admin level set ${NICK} ${P} 1`,800)
      await cmd(bot,`mmocore admin exp set ${NICK} ${P} 0`,800)
      // szukamy progu: dosypujemy male porcje az do awansu
      const porcja=Math.max(50,Math.round(wzor(K)/12))
      let suma=0, L=1, limit=40
      while(L<2 && limit-->0){
        await cmd(bot,`mmocore admin exp give ${NICK} ${P} ${porcja}`,600)
        suma+=porcja
        L=await papi(bot,`%mmocore_profession_${P}%`)
      }
      const reszta=await papi(bot,`%mmocore_profession_experience_${P}%`)
      const koszt=(L>=2)?(suma-reszta):NaN
      console.log(`${String(kl).padStart(5)}   ${String(koszt).padStart(35)}   ${String(wzor(K)).padStart(22)}   ${String(Number.isFinite(koszt)?Math.round(koszt*49):NaN).padStart(17)}`)
    }
    console.log('\nJesli kolumna ZMIERZONA sledzi kolumne wzoru -> {level} to poziom KLASY.')
  } finally { await H.quit(bot) }
  process.exit(0)
})().catch(e=>{console.log('FAIL przerwane — '+e.message);process.exit(2)})
