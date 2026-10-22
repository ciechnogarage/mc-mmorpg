'use strict'
/**
 * Eval SYSTEMU SLOTOW PROFESJI.
 * Pytanie: czy wybor profesji w slotach cokolwiek ogranicza?
 * Metoda: swiezy gracz (oba sloty 'none'), nadajemy XP profesjom,
 * ktorych NIE wybral, i patrzymy czy poziom rosnie.
 * Sabotaz: EVAL_SABOTAGE=brakxp - nie nadaje XP, test ma oblac.
 */
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')
const { odczytXp, oczysc } = require('/w/xp.js')
const NICK = process.env.EVAL_NICK || 'EvSlot'
const SAB = (process.env.EVAL_SABOTAGE || '').toLowerCase()
const sleep = ms => new Promise(r => setTimeout(r, ms))

class R { constructor(){this.rows=[]}
  a(c,l,e){this.rows.push({p:!!c,l,e});return !!c}
  print(){console.log('\n========= WYNIKI SLOTOW =========')
    for(const x of this.rows) console.log(`${x.p?'PASS':'FAIL'} ${x.l}${x.e?' — '+x.e:''}`)
    const f=this.rows.filter(x=>!x.p).length
    console.log(f?`\nWYNIK: ${f} NIEPOWODZEN / ${this.rows.length}`:`\nWYNIK: wszystko PASS (${this.rows.length})`)
    return f}}

// HUD CoreTools zalewa czat paskiem zycia - bez tego filtra kazdy odczyt
// lapie "24/24" z paska zamiast odpowiedzi komendy (raz juz na tym poleglo).
const czysteLinie = bot => bot.evalLog.map(oczysc).filter(l => !/[\u2764\u2666\u26e8]/.test(l))
async function cmd(bot,c,w=900){bot.evalLog.length=0;bot.chat('/'+c);await sleep(w);return czysteLinie(bot).join(' | ')}

// MMOCore nie ma 'level check' (sprawdzone: /mmocore admin -> tylko give/take/set),
// wiec poziom czytamy placeholderem. NaN przy bledzie, nigdy 0.
async function poziom(bot,prof){
  const t = await cmd(bot,`papi parse ${NICK} %mmocore_profession_${prof}%`,1500)
  const m = t.match(/(-?[0-9]+)\s*$/m) || t.match(/:\s*(-?[0-9]+)/)
  if(!m) console.log(`  [poziom] nie sparsowano ${prof}: ${JSON.stringify(t.slice(-200))}`)
  return m?parseInt(m[1]):NaN
}

;(async()=>{
  const r=new R()
  const bot=await H.connect(NICK,{timeout:90000})
  try{
    try{await F.waitProfileScreen(bot);const w=await F.enterCreator(bot);if(w)await F.completeCreator(bot)}
    catch(e){console.log('(kreator: '+e.message+')')}
    await sleep(3000)

    // 1. stan wyjsciowy slotow
    const s1=await cmd(bot,`core-variables get profession_slot_1 ${NICK}`,1200)
    const s2=await cmd(bot,`core-variables get profession_slot_2 ${NICK}`,1200)
    console.log('slot_1 ->',JSON.stringify(s1.slice(-160)))
    console.log('slot_2 ->',JSON.stringify(s2.slice(-160)))
    const pusty = /none/i.test(s1) && /none/i.test(s2)
    r.a(pusty,'swiezy gracz ma oba sloty puste (none)',`${s1.slice(-60)} / ${s2.slice(-60)}`)

    // 2. XP do profesji spoza slotow
    const TEST=['mining','fishing','smithing','cooking','building']
    for(const p of TEST){
      const przed=await poziom(bot,p)
      if(SAB!=='brakxp') await cmd(bot,`mmocore admin exp give ${NICK} ${p} 5000`,1200)
      const po=await poziom(bot,p)
      r.a(Number.isFinite(po)&&po>przed,
        `${p}: poziom ROSNIE mimo pustych slotow (system nieegzekwowany)`,
        `poziom ${przed} -> ${po}`)
    }

    // 3. zajmij slot_1 mining, sprawdz czy inne profesje nadal rosna
    await cmd(bot,`core-variables set profession_slot_1 mining ${NICK}`,1200)
    const s1b=await cmd(bot,`core-variables get profession_slot_1 ${NICK}`,1200)
    r.a(/mining/i.test(s1b),'slot_1 ustawiony na mining',s1b.slice(-60))
    const przed=await poziom(bot,'herbalism')
    await cmd(bot,`mmocore admin exp give ${NICK} herbalism 5000`,1200)
    const po=await poziom(bot,'herbalism')
    r.a(po>przed,'herbalism rosnie mimo ze slot_1=mining a slot_2=none',`${przed} -> ${po}`)

    // 4. ile profesji naraz da sie miec na poziomie > 0
    const WSZYSTKIE=['mining','woodworking','herbalism','hunting','fishing','farming','smithing','tailoring','jewelcrafting','enchanting','alchemy','cooking','building']
    let ile=0
    for(const p of WSZYSTKIE){ if(await poziom(bot,p)>1) ile++ }
    r.a(ile>2,`gracz ma ${ile}/13 profesji powyzej poziomu 1 przy 2 slotach`,`limit slotow = 2`)

  } finally { await H.quit(bot) }
  process.exit(r.print()?1:0)
})().catch(e=>{console.log('FAIL przerwane — '+e.message);process.exit(2)})
