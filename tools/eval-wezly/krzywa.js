'use strict'
/**
 * Pomiar REALNEJ krzywej doswiadczenia profesji.
 * Configi deklaruja wzor '150 + {level}*75 + {level}^2*12' (i tanszy dla
 * cooking/building). Sprawdzamy, co serwer NAPRAWDE robi: kazdej profesji
 * nadajemy inna dawke XP i odczytujemy poziom placeholderem.
 * Sabotaz: EVAL_SABOTAGE=brakxp - bez nadania XP wszystko ma zostac na 1.
 */
const H=require('/e/lib/harness'), F=require('/e/lib/flow')
const {oczysc}=require('/w/xp.js')
const NICK=process.env.EVAL_NICK||'EvKrz'
const SAB=(process.env.EVAL_SABOTAGE||'').toLowerCase()
const sleep=ms=>new Promise(r=>setTimeout(r,ms))
const czyste=bot=>bot.evalLog.map(oczysc).filter(l=>!/[❤♦⛨]/.test(l))
async function cmd(bot,c,w=800){bot.evalLog.length=0;bot.chat('/'+c);await sleep(w);return czyste(bot).join(' | ')}
async function poziom(bot,p){
  const t=await cmd(bot,`papi parse ${NICK} %mmocore_profession_${p}%`,1400)
  const m=t.match(/(-?[0-9]+)\s*$/m)||t.match(/:\s*(-?[0-9]+)/)
  if(!m)console.log(`  [poziom] ${p}: ${JSON.stringify(t.slice(-150))}`)
  return m?parseInt(m[1]):NaN
}
// wzory z plikow MMOCore/professions/*.yml
const wzorStd=L=>150+L*75+L*L*12
const wzorTani=L=>100+L*55+L*L*8
const poziomZWzoru=(xp,f)=>{let L=1,s=0;while(s+f(L)<=xp&&L<200){s+=f(L);L++}return L}

;(async()=>{
  const bot=await H.connect(NICK,{timeout:90000})
  try{
    try{await F.waitProfileScreen(bot);const w=await F.enterCreator(bot);if(w)await F.completeCreator(bot)}
    catch(e){console.log('(kreator: '+e.message+')')}
    await sleep(3000)
    const PLAN=[
      ['alchemy',100],['cooking',500],['building',500],['mining',1000],
      ['woodworking',2000],['herbalism',4000],['hunting',8000],['fishing',16000],
      ['farming',32000],['smithing',64000],['tailoring',128000],
      ['jewelcrafting',256000],['enchanting',584325],
    ]
    for(const [p,n] of PLAN){ if(SAB!=='brakxp') await cmd(bot,`mmocore admin exp give ${NICK} ${p} ${n}`,700) }
    await sleep(1500)
    console.log('\nprofesja        XP nadane   poziom REALNY   poziom wg wzoru z configu')
    console.log('-'.repeat(78))
    for(const [p,n] of PLAN){
      const L=await poziom(bot,p)
      const tani=(p==='cooking'||p==='building')
      const wz=poziomZWzoru(n,tani?wzorTani:wzorStd)
      console.log(`${p.padEnd(15)} ${String(n).padStart(8)}   ${String(L).padStart(11)}   ${String(wz).padStart(22)}${tani?'  (tansza krzywa w configu)':''}`)
    }
  } finally { await H.quit(bot) }
  process.exit(0)
})().catch(e=>{console.log('FAIL przerwane — '+e.message);process.exit(2)})
