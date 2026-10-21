'use strict'
const H = require('./lib/harness')
const F = require('./lib/flow')
const NAME = process.env.EQ_BOT || 'OcenaEqQA'

const ACC = { 9:'PAULDRONS',10:'NECKLACE',11:'EARRING',12:'LEFT_RING',13:'RIGHT_RING',
              14:'GLOVES',15:'SHIRT',16:'BELT',17:'CHARM' }

function row(bot){ return Object.keys(ACC).map(i=>`${i}:${bot.inventory.slots[i]?H.itemLabel(bot.inventory.slots[i]):'PUSTY'}`).join(' | ') }

;(async()=>{
  const r = new H.Results('ocena-eq'); let bot
  try{
    bot = await H.connect(NAME)
    const prof = await F.waitProfileScreen(bot, 20000)
    if(prof){ const w = await F.enterCreator(bot,20000); if(w){ await F.completeCreator(bot); await H.sleep(4000) } }
    await H.sleep(4000)

    // --- 1) ILE CZEKAMY NA IKONY ---
    let t0=Date.now(), seen=-1
    for(let i=0;i<40;i++){
      const n = Object.keys(ACC).filter(s=>bot.inventory.slots[s]).length
      if(n===9){ seen=Date.now()-t0; break }
      await H.sleep(1000)
    }
    r.assert(seen>=0, `9 ikon slotow pojawilo sie w plecaku`, seen>=0?`po ${(seen/1000).toFixed(0)} s od spawnu`:`po 40 s wciaz brak: ${row(bot)}`)
    console.log('GORNY RZAD PLECAKA: '+row(bot))

    bot.chat(`/mmocore admin level set ${NAME} main 30`)
    await H.sleep(1500)

    // --- 2) CZY DA SIE WYRZUCIC IKONE SLOTU (mode 4 = drop) ---
    const before = H.itemLabel(bot.inventory.slots[16])
    try{ await bot.clickWindow(16, 1, 4) }catch(e){ console.log('drop-throw: '+e.message) }
    await H.sleep(1500)
    const afterDrop = H.itemLabel(bot.inventory.slots[16])
    r.assert(afterDrop===before, 'ikona slotu BELT przezyla probe wyrzucenia (ctrl+Q)', `przed: ${before} / po: ${afterDrop}`)
    const ground = Object.values(bot.entities).filter(e=>e.name==='item'||e.objectType==='Item').length
    console.log('ENCJE ITEM WOKOL PO DROPIE: '+ground)

    // --- 3) CZY DA SIE PRZENIESC IKONE W INNE MIEJSCE PLECAKA ---
    try{ await bot.clickWindow(16,0,0); await H.sleep(600); await bot.clickWindow(30,0,0) }catch(e){ console.log('move-throw: '+e.message) }
    await H.sleep(1500)
    r.assert(H.itemLabel(bot.inventory.slots[16])===before && !bot.inventory.slots[30],
      'ikona slotu BELT nie da sie przeniesc do plecaka', `slot16=${H.itemLabel(bot.inventory.slots[16])} slot30=${H.itemLabel(bot.inventory.slots[30])}`)

    // --- 4) SHIFT-CLICK: czy przedmiot sam trafia do wlasciwego slotu ---
    bot.chat(`/mi give BELT PROF_TRAVELER_BELT ${NAME}`); await H.sleep(1800)
    let src = bot.inventory.slots.findIndex((it,i)=>i>=18 && it && /Pas podroznika/i.test(H.itemLabel(it)||''))
    if(src<0) src = bot.inventory.slots.findIndex((it,i)=>i>=36 && it && /Pas podroznika/i.test(H.itemLabel(it)||''))
    console.log('PAS wyladowal w slocie: '+src+' ('+(src>=0?H.itemLabel(bot.inventory.slots[src]):'-')+')')
    if(src>=0){
      try{ await bot.clickWindow(src,0,1) }catch(e){ console.log('shift-throw: '+e.message) }
      await H.sleep(1500)
      r.assert(/Pas podroznika/i.test(H.itemLabel(bot.inventory.slots[16])||''),
        'SHIFT-CLICK zaklada pas w slocie BELT', `slot16 = ${H.itemLabel(bot.inventory.slots[16])}`)
    }

    // --- 5) recznie: podnies i wloz ---
    src = bot.inventory.slots.findIndex((it,i)=>i>=18 && it && /Pas podroznika/i.test(H.itemLabel(it)||''))
    if(src>=0){
      await bot.clickWindow(src,0,0); await H.sleep(600); await bot.clickWindow(16,0,0); await H.sleep(1800)
      r.assert(/Pas podroznika/i.test(H.itemLabel(bot.inventory.slots[16])||''),
        'reczne wlozenie pasa do slotu BELT dziala', `slot16 = ${H.itemLabel(bot.inventory.slots[16])}`)
    }
    console.log('GORNY RZAD PO PROBACH: '+row(bot))

    // --- 6) WARSZTATY: czy receptury akcesoriow sa w ogole widoczne ---
    for(const [cmd,label] of [['/warsztat-jewelcrafting','Jubiler'],['/warsztat-tailoring','Krawiec'],['/warsztat-smithing','Kowal']]){
      bot.chat(cmd)
      const w = await H.waitForWindow(bot,{timeout:8000})
      if(!w){ r.fail(`warsztat ${label} otworzyl sie`); continue }
      const names = H.windowSlots(w).map(s=>s.label).filter(Boolean)
      console.log(`WARSZTAT ${label} (${H.windowTitle(w)}): `+JSON.stringify(names))
      bot.closeWindow(w); await H.sleep(1200)
    }
  }catch(e){ r.fail('probe wywrocil sie', String(e&&e.message||e)) }
  finally{ r.print(); if(bot) await H.quit(bot); process.exit(0) }
})()
