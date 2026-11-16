'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');

module.exports=async({k,o,scene,w})=>{
  const names=['stone_sword','netherite_sword','bow'];
  const real=()=>{
    const found={};for(const n of names)found[n]=0;
    for(const s of [...Array(36)].map((_,i)=>i+9).concat(45)){
      if([13,37].includes(s))continue;
      const item=k.bot.inventory.slots[s];if(item&&item.name in found)found[item.name]+=item.count;
    }
    if(k.bot.inventory.cursorItem?.name in found)found[k.bot.inventory.cursorItem.name]+=k.bot.inventory.cursorItem.count;
    return found;
  };
  const server=async()=>{const x=await w.checker.inventory();return x;};
  const reset=async()=>{await scene.prepare({target:false});await o.cmd(`zaklecia przypisz ${o.name} nic prawa`);await o.cmd(`zaklecia przypisz ${o.name} nic lewa`);await sleep(250);};
  const give=async(type,id,name,exclude=[])=>{
    await o.cmd(`mi give ${type} ${id} ${o.name} 1`);
    let slot=await until(()=>{const n=k.bot.inventory.slots.findIndex((v,i)=>i>=9&&i<=44&&!exclude.includes(i)&&v?.name===name);return n<0?null:n;},id,3000);
    if(slot===36||slot===45){const empty=k.bot.inventory.slots.findIndex((v,i)=>i>=20&&i<=35&&!v);if(empty<0)throw Error('brak plecaka');
      k.e();try{await k.bot.clickWindow(slot,0,0);await k.bot.clickWindow(empty,0,0);}finally{k.zamknij();}slot=empty;}
    return slot;
  };
  const click=async(a,b,button=0,mode=0)=>{k.e();try{await k.bot.clickWindow(a,button,mode);if(b!==null)await k.bot.clickWindow(b,0,0);}finally{k.zamknij();}await sleep(650);};
  const check=async(id,before,expectLeft,expectSpell)=>{
    if(k.bot.currentWindow)k.zamknij();await sleep(500);
    const after=real(),left=k.item('off')?.name||null,spell=await o.placeholder('lewa'),inv=await server();
    const ok=JSON.stringify(before)===JSON.stringify(after)&&left===expectLeft&&((spell==='Płomienie')===expectSpell)&&inv.bad.length===0&&inv.errors.length===0;
    w.add(id,ok,`real=${JSON.stringify(before)}→${JSON.stringify(after)} off=${left} czar=${spell} server/client=${inv.bad.length}/${inv.errors.length}`,{before,after,left,spell,inv});
  };
  const armed=async()=>{await reset();await assign(k,o,'plomienie','lewa');await sleep(350);};
  await w.step('B6Safety-przeciagniecie',async()=>{await armed();const slot=await give('SWORD','MIECZ_KAMIENNY','stone_sword'),before=real();await click(slot,45);await check('B6Safety-przeciagniecie',before,'stone_sword',false);});
  await w.step('B6Safety-F',async()=>{await armed();const slot=await give('SWORD','MIECZ_KAMIENNY','stone_sword');await click(slot,36);k.cyfra(1);await sleep(250);const before=real();k.f();await check('B6Safety-F',before,'stone_sword',false);
    for(let i=0;i<8;i++){k.f();await sleep(125);}await check('B6Safety-F-powtorki',before,'stone_sword',false);});
  await w.step('B6Safety-shift',async()=>{await armed();let slot=await give('SWORD','MIECZ_KAMIENNY','stone_sword');if(slot<14||slot>35){await click(slot,23);slot=23;}const before=real();await click(slot,null,0,1);await check('B6Safety-shift',before,'stone_sword',false);});
  await w.step('B6Safety-numer',async()=>{await armed();const slot=await give('SWORD','MIECZ_KAMIENNY','stone_sword');await click(slot,41);const before=real();await click(45,null,5,2);await check('B6Safety-numer',before,'stone_sword',false);});
  await w.step('B6Safety-zapas',async()=>{await armed();const slot=await give('SWORD','MIECZ_KAMIENNY','stone_sword');await click(slot,12);const before=real();await click(12,null,1,0);await check('B6Safety-zapas',before,'stone_sword',false);});
  await w.step('B6Safety-zestaw',async()=>{await armed();let slot=await give('SWORD','MIECZ_KAMIENNY','stone_sword');const first=slot;await click(slot,12);
    const afterFirst={source:first,spareRight:k.bot.inventory.slots[11]?.name,spareLeft:k.bot.inventory.slots[12]?.name};
    slot=await give('SWORD','MIECZ_KAMIENNY','stone_sword',[12]);const second=slot;await click(slot,11);const before=real();
    console.log('B6SETUP zestaw '+JSON.stringify({first:afterFirst,second,right:k.bot.inventory.slots[36]?.name,spareRight:k.bot.inventory.slots[11]?.name,spareLeft:k.bot.inventory.slots[12]?.name,off:k.item('off')?.name}));
    k.cyfra(1);await sleep(180);k.shift(true);await sleep(250);k.f();await sleep(300);k.shift(false);await check('B6Safety-zestaw',before,'stone_sword',false);});
  for(const [type,id,name] of [['GREATSWORD','MIECZ_RUNICZNY','netherite_sword'],['BOW','LUK_MYSLIWSKI','bow']]){
    await w.step(`B6Safety-odmowa-${name}`,async()=>{await armed();const slot=await give(type,id,name),before=real();await click(slot,45);await check(`B6Safety-odmowa-${name}`,before,'debug_stick',true);});
  }
};
