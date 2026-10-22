'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');
const {Vec3}=require('vec3');

module.exports=async({k,o,scene,w})=>{
  const sword=/stone_sword/i;
  const beforeSkill=await o.cmd(`mmocore admin attribute check ${o.name} bieglosc-miecz`);
  const initial=Number(beforeSkill.match(/(\d+)\s+(?:points|punkt)/i)?.[1]||0),grant=Math.max(0,1-initial);
  if(grant)await o.cmd(`mmocore admin attribute give ${o.name} bieglosc-miecz ${grant}`);
  const equip=async orientation=>{
    await scene.prepare({target:false});
    await o.cmd(`zaklecia przypisz ${o.name} nic lewa`);
    await o.cmd(`zaklecia przypisz ${o.name} nic prawa`);
    for(let i=0;i<3;i++){
      await o.cmd(`minecraft:clear ${o.name} minecraft:stone_sword`);await sleep(300);
      if(!k.bot.inventory.slots.some((x,n)=>n>=9&&n<=45&&n!==13&&n!==37&&sword.test(x?.name||'')))break;
    }
    await assign(k,o,'odmrozenie',orientation==='bron-prawa'?'lewa':'prawa');
    await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);
    const slot=await until(()=>{const i=k.bot.inventory.slots.findIndex((x,n)=>n>=9&&n<=44&&n!==13&&n!==37&&x&&sword.test(`${x.name} ${x.displayName}`));return i<0?null:i;},'miecz MMOItems',2500);
    if(orientation==='bron-lewa'){
      k.e();try{await k.bot.clickWindow(slot,0,0);await k.bot.clickWindow(45,0,0);}finally{k.zamknij();}
      k.cyfra(1);
    }else{
      k.e();try{await k.bot.clickWindow(slot,0,0);await k.bot.clickWindow(36,0,0);}finally{k.zamknij();}
      k.cyfra(1);
    }
    await sleep(900);
    const selected=await o.selected(),offData=await o.cmd(`minecraft:execute if items entity ${o.name} weapon.offhand minecraft:stone_sword`);
    const ready=orientation==='bron-prawa'?/bron:s100c?\/stone_sword/.test(selected):/Test passed/i.test(offData);
    if(!ready)throw Error(`brak MMOItems w ${orientation}: src=${slot}, srcItem=${k.bot.inventory.slots[slot]?.name}, offClient=${k.item('off')?.name}, cursor=${k.bot.inventory.cursorItem?.name}, selected=${selected.slice(0,130)} off=${offData.slice(0,130)}`);
    return {slot,selected,offData,off:k.item('off')?.name||null,main:k.item()?.name||null};
  };
  let leftBlocked=false;
  try{for(const orientation of ['bron-lewa','bron-prawa'])for(const action of ['ppm','lpm','shiftq','f','przelacz','smierc']){
    if(orientation==='bron-lewa'&&leftBlocked){w.skip(`DB3-${orientation}-${action}`,'wyposażenie miecza w lewej dłoni z aktywnym nośnikiem jest zablokowane');continue;}
    await w.step(`DB3-${orientation}-${action}`,async()=>{
      let before;
      try{before=await equip(orientation);}catch(e){if(orientation==='bron-lewa'){leftBlocked=true;w.add('DB3-bron-lewa-equip',false,String(e));return;}throw e;}
      const hpTarget='@e[type=minecraft:husk,tag=DB3Target,limit=1]';
      const known=new Set(Object.values(k.bot.entities).filter(e=>e.name==='husk').map(e=>e.id));
      await o.cmd('minecraft:summon minecraft:husk 0 80 2 {NoAI:1b,PersistenceRequired:1b,Tags:["DB3Target"]}');
      const mob=await until(()=>Object.values(k.bot.entities).find(e=>e.name==='husk'&&!known.has(e.id)&&e.position.distanceTo(k.bot.entity.position)<5),'nowy husk DB3',2000);
      await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(150);
      const h0=await o.hp(hpTarget),m0=await o.mana();
      if(action==='ppm')await k.ppmTrzymaj(850);
      else if(action==='lpm'){
        if(orientation==='bron-prawa'){
          k.bot.attack(mob);await sleep(1000);k.bot.attack(mob);
        }else k.lpmKlik();
        await sleep(850);if(orientation==='bron-lewa')k.lpmKlik();
      }
      else if(action==='shiftq'){await k.shiftQ();await sleep(250);k.q();}
      else if(action==='f'){k.f();await sleep(900);}
      else if(action==='przelacz'){k.cyfra(6);await sleep(200);k.cyfra(1);await sleep(900);}
      else{
        const previous=await o.cmd('minecraft:gamerule keep_inventory');
        try{
          await o.cmd('minecraft:gamerule keep_inventory true');await o.cmd('minecraft:setblock 0 111 0 minecraft:stone');
          await o.cmd(`minecraft:tp ${o.name} 0.5 112 0.5 0 0`);
          await until(()=>k.bot.entity.position.y>110,'wieża DB3',4000);
          let dead=false;k.bot.once('death',()=>{dead=true;});k.w(true);
          await until(()=>k.bot.entity.position.z>1.6,'krawędź DB3',5000).catch(()=>{});k.w(false);
          await until(()=>dead,'śmierć DB3',8000);await sleep(900);
        }finally{await o.cmd('minecraft:setblock 0 111 0 minecraft:air');await o.cmd(`minecraft:gamerule keep_inventory ${/true/i.test(previous)?'true':'false'}`);}
      }
      const h1=action==='smierc'?null:await o.hp(hpTarget),m1=await o.mana(),selected=await o.selected();
      const weaponAfter=orientation==='bron-lewa'||action==='f'
        ?/stone_sword/.test(k.item('off')?.name||'')
        :/bron:s100c?\/stone_sword/.test(selected);
      const data={orientation,action,before,after:{selected:selected.slice(0,120),off:k.item('off')?.name||null,main:k.item()?.name||null},weaponAfter,hpDelta:h0-h1,manaDelta:m0-m1};
      const cast=orientation==='bron-prawa'&&action==='ppm'||orientation==='bron-lewa'&&action==='lpm';
      const melee=orientation==='bron-prawa'&&action==='lpm';
      w.add(`DB3-${orientation}-${action}`,weaponAfter&&(cast?m1<m0&&h1<h0:melee?h1<h0:true),`${orientation} ${action}: HP ${h0}→${h1}, mana ${m0}→${m1}`,data);
    });
  }}finally{if(grant)await o.cmd(`mmocore admin attribute take ${o.name} bieglosc-miecz ${grant}`);}
};
