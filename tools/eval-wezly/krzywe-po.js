'use strict'
/**
 * DOWOD PO ZMIANIE: krzywa profesji jest NIEZALEZNA od poziomu klasy.
 *
 * 1) Ten sam zastrzyk XP przy RÓZNYCH poziomach klasy ma dac IDENTYCZNY
 *    poziom profesji. Przed zmiana (formula z {level}=poziom KLASY) klasa 1
 *    i klasa 40 dawaly wyniki rozniace sie ~143x.
 * 2) Zmierzone progi musza zgadzac sie co do jednostki z plikiem
 *    exp-curves/zbieractwo.txt. To osobny, konieczny dowod: MMOCore przy
 *    nieznanej nazwie krzywej NIE zglasza bledu, tylko po cichu traktuje
 *    napis jako formule (ExperienceCurve.fromConfig -> catch -> Formula...).
 *    Zgodnosc liczb jest jedynym dowodem, ze wczytala sie krzywa z pliku.
 */
const H=require('/e/lib/harness'), F=require('/e/lib/flow')
const {oczysc}=require('/w/xp.js')
const NICK=process.env.EVAL_NICK||'EvKp'
const P=process.env.EVAL_PROF||'mining'
const sleep=ms=>new Promise(r=>setTimeout(r,ms))
const czyste=bot=>bot.evalLog.map(oczysc).filter(l=>!/[❤♦⛨]/.test(l))
async function cmd(bot,c,w=700){bot.evalLog.length=0;bot.chat('/'+c);await sleep(w);return czyste(bot).join(' | ')}
async function papi(bot,ph,w=1100){
  const t=await cmd(bot,`papi parse ${NICK} ${ph}`,w)
  const m=t.match(/(-?[0-9]+(?:[.,][0-9]+)?)\s*$/m)
  return m?parseFloat(m[1].replace(',','.')):NaN
}
// kumulatywne XP -> oczekiwany poziom, prosto z exp-curves/zbieractwo.txt
const PROBKI=[[81,2],[208,3],[587,5],[2272,10],[5200,15],[9746,20],[26000,30]]
const PROGI=[[1,81],[2,127],[3,169],[4,210]]
;(async()=>{
  const bot=await H.connect(NICK,{timeout:90000})
  const wyniki=[]
  try{
    try{await F.waitProfileScreen(bot);const w=await F.enterCreator(bot);if(w)await F.completeCreator(bot)}
    catch(e){console.log('(kreator: '+e.message+')')}
    await sleep(3000)

    console.log('=== TEST 1: ten sam zastrzyk XP przy klasie 1 i 40 ===')
    console.log('klasa |   XP  | oczek. poziom | zmierzony | reszta XP')
    console.log('-'.repeat(60))
    for(const K of [1,40]){
      await cmd(bot,`mmocore admin level set ${NICK} main ${K}`,900)
      const kl=await papi(bot,'%mmocore_level%')
      for(const [xp,oczek] of PROBKI){
        await cmd(bot,`mmocore admin level set ${NICK} ${P} 1`,700)
        await cmd(bot,`mmocore admin exp set ${NICK} ${P} 0`,700)
        await cmd(bot,`mmocore admin exp give ${NICK} ${P} ${xp}`,800)
        const L=await papi(bot,`%mmocore_profession_${P}%`)
        const R=await papi(bot,`%mmocore_profession_experience_${P}%`)
        const ok=(L===oczek)
        wyniki.push({klasa:kl,xp,oczek,L,R,ok})
        console.log(`${String(kl).padStart(5)} | ${String(xp).padStart(5)} | ${String(oczek).padStart(13)} | ${String(L).padStart(9)} | ${String(R).padStart(9)} ${ok?'OK':'ZLE'}`)
      }
    }

    console.log('\n=== TEST 2: progi pojedynczych poziomow vs plik ===')
    console.log('poziom | plik | zmierzony(o 1 XP mniej / dokladnie)')
    console.log('-'.repeat(60))
    await cmd(bot,`mmocore admin level set ${NICK} main 40`,900)
    for(const [L0,prog] of PROGI){
      // o 1 XP za malo -> zostajemy; dokladnie prog -> awans
      await cmd(bot,`mmocore admin level set ${NICK} ${P} ${L0}`,700)
      await cmd(bot,`mmocore admin exp set ${NICK} ${P} 0`,700)
      await cmd(bot,`mmocore admin exp give ${NICK} ${P} ${prog-1}`,800)
      const aMniej=await papi(bot,`%mmocore_profession_${P}%`)
      await cmd(bot,`mmocore admin exp give ${NICK} ${P} 1`,800)
      const aDokl=await papi(bot,`%mmocore_profession_${P}%`)
      const ok=(aMniej===L0 && aDokl===L0+1)
      wyniki.push({prog:true,ok})
      console.log(`${String(L0+'->'+(L0+1)).padStart(6)} | ${String(prog).padStart(4)} | ${aMniej} / ${aDokl} ${ok?'OK':'ZLE'}`)
    }

    // porownanie miedzy klasami
    const k1=wyniki.filter(w=>w.klasa===1&&!w.prog).map(w=>w.L).join(',')
    const k40=wyniki.filter(w=>w.klasa===40&&!w.prog).map(w=>w.L).join(',')
    console.log(`\npoziomy przy klasie 1:  ${k1}`)
    console.log(`poziomy przy klasie 40: ${k40}`)
    const zgodne=(k1===k40 && k1.length>0)
    const wszystko=wyniki.every(w=>w.ok) && zgodne
    console.log(`\nidentyczne miedzy klasami: ${zgodne?'TAK':'NIE'}`)
    console.log(wszystko?'PASS — krzywa niezalezna od klasy i zgodna z plikiem':'FAIL')
  } finally { await H.quit(bot) }
  process.exit(0)
})().catch(e=>{console.log('FAIL przerwane — '+e.message);process.exit(2)})
