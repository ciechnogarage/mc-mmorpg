'use strict'
/**
 * Pelna sciezka kucharza od zera, przeciwko ZYWEMU world1:
 *   wezel przyprawowy -> surowiec + XP gotowania -> warsztat -> POTRAWA -> ZJEDZENIE.
 *
 * Pulapki juz oplacone krwia (nie zmieniac bez powodu):
 *  - MythicMobs slucha WYLACZNIE PlayerInteractAtEntityEvent -> activateEntityAt.
 *  - CMI przechwytuje /tp, /kill, /clear -> prefiks minecraft:.
 *  - /mi give <TYP> <ID> <gracz> <ilosc> - gracz jest TRZECI.
 *  - HUD CoreTools zalewa czat -> filtrujemy linie z ❤ ♦ ⛨.
 */
const H = require('/e/lib/harness')
const F = require('/e/lib/flow')
const NICK = process.env.EVAL_NICK || 'EvKuch'
const X = 150.5, Y = 99, Z = 197.5
const SX = 152.5                       // gdzie stawiamy kepe przypraw
const sleep = ms => new Promise(r => setTimeout(r, ms))
const label = i => { try { return H.itemLabel(i) } catch { return (i && i.displayName) || '' } }
const inv = bot => bot.inventory.items().map(i => `${i.count}x ${label(i)}`).join(' | ')
const ile = (bot, frag) => bot.inventory.items()
  .filter(i => (label(i) || '').toLowerCase().includes(frag.toLowerCase()))
  .reduce((a, i) => a + i.count, 0)
const czysty = bot => bot.evalLog.filter(m => !/[❤♦⛨]/.test(m))

class R {
  constructor(){this.rows=[]}
  assert(c,l,e){this.rows.push({pass:!!c,l,e});console.log(`${c?'PASS':'FAIL'} ${l}${e?' — '+e:''}`);return !!c}
  print(){const f=this.rows.filter(x=>!x.pass).length
    console.log(f?`\nWYNIK: ${f} NIEPOWODZEN / ${this.rows.length}`:`\nWYNIK: wszystko PASS (${this.rows.length})`);return f}
}
async function cmd(bot,c,w=800){bot.evalLog.length=0;bot.chat('/'+c);await sleep(w);return czysty(bot).join(' | ')}
async function xpCook(bot){
  const t = await cmd(bot, `mmocore admin exp check ${NICK} cooking`, 1400)
  const m = t.match(/has\s+([0-9.]+)\s+EXP/i); return m ? parseFloat(m[1]) : -1
}
async function papi(bot,ph){
  const t=await cmd(bot,`papi parse ${NICK} ${ph}`,1200)
  const m=t.match(/(-?[0-9]+(?:[.,][0-9]+)?)\s*$/m); return m?parseFloat(m[1].replace(',','.')):NaN
}
async function tp(bot,x){
  for(let i=0;i<8;i++){
    bot.chat(`/minecraft:execute in minecraft:overworld run tp ${NICK} ${x} ${Y} ${Z}`); await sleep(1700)
    if(bot.entity&&Math.abs(bot.entity.position.x-x)<6&&Math.abs(bot.entity.position.z-Z)<6)return true
  } return false
}
function znajdz(bot,frag){
  return Object.values(bot.entities)
    .filter(e=>e.name==='zombie'&&e.position.distanceTo(bot.entity.position)<8)
    .filter(e=>{try{const j=JSON.stringify(e.metadata||{}).toLowerCase()
      if(j.includes(frag.toLowerCase()))return true
      const p=(j.match(/"text":"([^"]*)"/g)||[]).map(x=>x.slice(8,-1)).join('')
      return p.includes(frag.toLowerCase())}catch{return false}})
    .sort((a,b)=>a.position.distanceTo(bot.entity.position)-b.position.distanceTo(bot.entity.position))[0]
}

;(async()=>{
  const r=new R()
  const bot=await H.connect(NICK,{timeout:90000})
  try{
    try{await F.waitProfileScreen(bot);const w=await F.enterCreator(bot);if(w)await F.completeCreator(bot)}
    catch(e){console.log('(kreator: '+e.message+')')}
    await sleep(3000)
    await cmd(bot,`minecraft:clear ${NICK}`,900)
    await cmd(bot,`mmocore admin level set ${NICK} main 40`,900)   // wysoka KLASA - nie moze nic zmieniac
    await cmd(bot,`mmocore admin level set ${NICK} cooking 1`,800)
    await cmd(bot,`mmocore admin exp set ${NICK} cooking 0`,800)

    // ---------- 1. WEZEL PRZYPRAWOWY ----------
    console.log('\n---------- 1. wezel przyprawowy ----------')
    await tp(bot,X)
    bot.chat(`/mm mobs spawn NODE_SPICE_PATCH 1 world,${SX},${Y},${Z}`); await sleep(2500)
    await tp(bot,SX)
    const w=znajdz(bot,'przyprawowych')
    if(r.assert(!!w,'wezel przyprawowy widoczny','zombie w zasiegu: '+
        Object.values(bot.entities).filter(e=>e.name==='zombie').length)){
      await bot.lookAt(w.position.offset(0,1,0),true); await sleep(400)
      const xp0=await xpCook(bot)
      bot.evalLog.length=0
      try{await bot.activateEntityAt(w,w.position.offset(0,0.5,0))}catch(e){console.log('  blad: '+e.message)}
      await sleep(2500)
      const komunikat=czysty(bot).some(m=>/Zrywasz|obfit|przyprawowe/i.test(m))
      r.assert(komunikat,'komunikat o zbiorze',czysty(bot).slice(-2).join(' / ')||'(brak)')
      r.assert(ile(bot,'przyprawowe')>0,'PROF_WILD_SPICE w plecaku',inv(bot).slice(0,180))
      const xp1=await xpCook(bot)
      r.assert(xp1>xp0,'XP gotowania wzroslo z WEZLA',`${xp0} -> ${xp1} EXP`)
    }

    // ---------- 2. KRZYWA GOTOWANIA ----------
    console.log('\n---------- 2. krzywa gotowania (plik zbieractwo.txt) ----------')
    await cmd(bot,`mmocore admin level set ${NICK} cooking 1`,800)
    await cmd(bot,`mmocore admin exp set ${NICK} cooking 0`,800)
    await cmd(bot,`mmocore admin exp give ${NICK} cooking 5200`,900)
    const lvlC=await papi(bot,'%mmocore_profession_cooking%')
    r.assert(lvlC===15,'5200 XP -> poziom gotowania 15 (jak w pliku)',`zmierzono ${lvlC}`)

    // ---------- 3. WARSZTAT -> POTRAWA ----------
    console.log('\n---------- 3. warsztat kucharza ----------')
    await cmd(bot,`minecraft:clear ${NICK}`,900)
    await cmd(bot,`mi give MATERIAL PROF_WILD_SPICE ${NICK} 4`,1000)
    r.assert(ile(bot,'przyprawowe')>=2,'skladniki na racje podrozna',inv(bot).slice(0,180))
    bot.chat('/warsztat-cooking')
    const win=await H.waitForWindow(bot,{timeout:9000})
    if(r.assert(!!win,'warsztat kucharza otwiera sie',czysty(bot).slice(-2).join(' / '))){
      const przed=H.windowSlots(win).map(s=>`${s.slot}:${(s.label||'').slice(0,30)}`).join(' | ')
      console.log('  okno PRZED: '+przed)
      const it=win.slots[10]
      const lab=it?(label(it)||''):''
      if(r.assert(!!it&&!/Brak receptury/i.test(lab),'receptura racji w slocie 10',`"${lab}"`)){
        await bot.clickWindow(10,0,0); await sleep(4000)
        // Wyrob laduje w KOLEJCE (crafting-time) i dopiero klikniecie go odbiera.
        // Kolejka bywa jeszcze "niegotowa" przy pierwszym podejsciu - stad petla.
        for(let proba=1;proba<=4;proba++){
          if(!bot.currentWindow)break
          if(ile(bot,'Racja podróżna')>0)break
          for(const s of H.windowSlots(bot.currentWindow)){
            if([10,11,12,13,14].includes(s.slot))continue
            if(/Kolejka pusta/i.test(s.label||''))continue
            if(!/Racja|Kolejka/i.test(s.label||''))continue
            console.log(`  proba ${proba}, odbior ze slotu ${s.slot}: "${s.label}"`)
            await bot.clickWindow(s.slot,0,0); await sleep(1800)
          }
          await sleep(1500)
        }
      }
      try{bot.closeWindow(bot.currentWindow)}catch{}
      await sleep(1500)
    }
    r.assert(ile(bot,'Racja podróżna')>0,'POTRAWA "Racja podróżna" w plecaku',inv(bot).slice(0,220))

    // ---------- 4. CZY DA SIE ZJESC ----------
    console.log('\n---------- 4. jadalnosc potrawy ----------')
    await cmd(bot,`minecraft:clear ${NICK}`,900)
    await cmd(bot,`mi give CONSUMABLE PROF_TRAIL_RATION ${NICK} 1`,1200)
    const maPotr=ile(bot,'Racja podróżna')>0
    r.assert(maPotr,'potrawa wydana jako CONSUMABLE',inv(bot).slice(0,180))
    if(maPotr){
      // zglodz bota, zeby restore-food mialo co odbudowac
      // Najpierw trzeba spalic SYTOSC (saturation) - dopiero potem spada pasek glodu.
      await cmd(bot,`minecraft:effect give ${NICK} minecraft:hunger 20 40 true`,2000)
      for(let i=0;i<14 && bot.food>12;i++) await sleep(1500)
      await cmd(bot,`minecraft:effect clear ${NICK} minecraft:hunger`,1200)
      const food0=bot.food
      console.log(`  glod przed jedzeniem: ${food0}/20`)
      const it=bot.inventory.items().find(i=>(label(i)||'').includes('Racja podróżna'))
      await bot.equip(it,'hand'); await sleep(800)
      bot.evalLog.length=0
      try{await bot.activateItem()}catch(e){console.log('  activateItem: '+e.message)}
      await sleep(3000)
      try{bot.deactivateItem()}catch{}
      await sleep(2500)
      const food1=bot.food
      const zjedzona=ile(bot,'Racja podróżna')===0
      r.assert(zjedzona,'potrawa zniknela z reki (zostala zjedzona)',`w plecaku: ${inv(bot).slice(0,150)}`)
      r.assert(food1>food0,'pasek glodu odbudowany',`${food0} -> ${food1}`)
      const eff=Object.values(bot.entity.effects||{})
      const nazwy=eff.map(e=>e.id!==undefined?e.id:JSON.stringify(e)).join(',')
      // SPEED ma na tym serwerze id 0 (zmierzone kontrola w efekty.js)
      const maSpeed=eff.some(e=>e.id===0)
      r.assert(maSpeed,'efekt SPEED z potrawy dziala',`efekty gracza: [${nazwy}]`)
    }
  } finally {
    try{bot.chat('/mm m kill NODE_SPICE_PATCH')}catch{}
    await sleep(1200)
    await H.quit(bot)
  }
  process.exit(r.print()?1:0)
})().catch(e=>{console.log('FAIL przerwane — '+e.message);process.exit(2)})
