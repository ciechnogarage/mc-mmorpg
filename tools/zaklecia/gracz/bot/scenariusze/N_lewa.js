'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');
const crypto=require('node:crypto');
module.exports=async({k,o,scene,w})=>{
  const sword=i=>i&&/stone_sword/i.test(`${i.name} ${i.displayName}`);
  const snapshot=()=>{
    const slots=[...Array(36)].map((_,i)=>i+9).concat([5,6,7,8,45]);
    const items=slots.map(slot=>{const i=k.bot.inventory.slots[slot];return {slot,name:i?.name||null,count:i?.count||0,id:i?.type||0,nbt:i?.nbt||null};});
    return {items,sha:crypto.createHash('sha256').update(JSON.stringify(items)).digest('hex'),swords:items.filter(i=>i.slot!==13&&i.slot!==37&&/stone_sword/.test(i.name||'')).reduce((n,i)=>n+i.count,0)};
  };
  const give=async(exclude=[13,37])=>{
    await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);
    return until(()=>{const i=k.bot.inventory.slots.findIndex((x,n)=>n>=9&&n<=44&&!exclude.includes(n)&&sword(x));return i<0?null:i;},'miecz',3000);
  };
  const cleanSwords=async()=>{
    for(let i=0;i<4;i++){
      await o.cmd(`minecraft:clear ${o.name} minecraft:stone_sword`);await sleep(350);
      if(snapshot().swords===0)return;
    }
    throw Error('miecze z poprzedniej sondy pozostaly w ekwipunku');
  };
  const setup=async()=>{await scene.prepare({target:false});await cleanSwords();await assign(k,o,'plomienie','lewa');await sleep(400);};
  const check=async(name,before,start,source)=>{
    await sleep(850);const after=snapshot(),left=await o.placeholder('lewa');
    const serverOff=await o.cmd(`minecraft:execute if items entity ${o.name} weapon.offhand minecraft:stone_sword`);
    const messages=o.events.slice(start).filter(e=>e.packet==='system_chat'&&/Odłożono zaklęcie z lewej ręki/.test(JSON.stringify(e.data)));
    const changed=before.items.filter((item,i)=>JSON.stringify(item)!==JSON.stringify(after.items[i])).map(item=>item.slot);
    const unexpected=changed.filter(slot=>![13,36,37,45,source].includes(slot));
    w.add(name,after.swords===before.swords&&sword(k.item('off'))&&/Test passed/i.test(serverOff)
      &&left!=='Płomienie'&&messages.length===1&&unexpected.length===0,
      'bron w lewej, zaklecie odlozone, jeden komunikat i bez straty miecza',
      {before,after,left,messages:messages.length,off:k.item('off')?.name,serverOff,changed,unexpected});
  };
  await w.step('N_lewa-przeciagniecie',async()=>{
    await setup();const slot=await give(),before=snapshot(),start=o.events.length;
    k.e();try{await k.bot.clickWindow(slot,0,0);await k.bot.clickWindow(45,0,0);}finally{k.zamknij();}
    await check('N_lewa-przeciagniecie',before,start,slot);
  });
  await w.step('N_lewa-F',async()=>{
    await setup();const slot=await give();
    k.e();try{await k.bot.clickWindow(slot,0,0);await k.bot.clickWindow(36,0,0);}finally{k.zamknij();}
    await sleep(250);const before=snapshot(),start=o.events.length;k.f();
    await check('N_lewa-F',before,start,36);
    const stable=snapshot(),repeatStart=o.events.length;
    for(let i=0;i<6;i++){k.f();await sleep(110);}
    await sleep(350);
    const repeated=snapshot(),extra=o.events.slice(repeatStart).filter(e=>e.packet==='system_chat'
      &&/Odłożono zaklęcie z lewej ręki/.test(JSON.stringify(e.data))).length;
    w.add('N_lewa-F-powtorki',repeated.swords===stable.swords&&extra===0,
      'szybkie F tam i z powrotem nie dubluje ani nie gubi broni i nie powtarza komunikatu',
      {stable,repeated,extra});
  });
  await w.step('N_lewa-shift',async()=>{
    await setup();let slot=await give();
    k.e();try{await k.bot.clickWindow(slot,0,0);await k.bot.clickWindow(36,0,0);}finally{k.zamknij();}
    await sleep(200);slot=await give([13,36,37]);
    // Shift na hotbarze przenosi do plecaka; źródło musi leżeć w rzędzie plecaka.
    if(slot>=36){k.e();try{await k.bot.clickWindow(slot,0,0);await k.bot.clickWindow(23,0,0);}finally{k.zamknij();}slot=23;await sleep(200);}
    const before=snapshot(),start=o.events.length;
    k.e();try{await k.bot.clickWindow(slot,0,1);}finally{k.zamknij();}
    await check('N_lewa-shift',before,start,slot);
  });
  await w.step('N_lewa-zapas',async()=>{
    await setup();const slot=await give();
    k.e();try{await k.bot.clickWindow(slot,0,0);await k.bot.clickWindow(12,0,0);}finally{k.zamknij();}
    await sleep(250);const before=snapshot(),start=o.events.length;
    k.e();try{await k.bot.clickWindow(12,1,0);}finally{k.zamknij();}
    await check('N_lewa-zapas',before,start,12);
  });
  await w.step('N_lewa-prawy-czar-lewa-bron',async()=>{
    await scene.prepare({target:false});await cleanSwords();await assign(k,o,'plomienie','prawa');await sleep(350);
    const slot=await give(),before=snapshot();
    k.e();try{await k.bot.clickWindow(slot,0,0);await k.bot.clickWindow(45,0,0);}finally{k.zamknij();}
    await sleep(850);const after=snapshot(),right=await o.placeholder('prawa');
    const serverOff=await o.cmd(`minecraft:execute if items entity ${o.name} weapon.offhand minecraft:stone_sword`);
    w.add('N_lewa-prawy-czar-lewa-bron',sword(k.item('off'))&&/Test passed/i.test(serverOff)
      &&after.swords===before.swords&&right==='Płomienie',
      'broń w lewej nie usuwa zaklęcia z prawej',{before,after,right,off:k.item('off')?.name,serverOff});
  });
};
