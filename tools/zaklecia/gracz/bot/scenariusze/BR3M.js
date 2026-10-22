'use strict';
const {Vec3}=require('vec3');
const {sleep}=require('../lib/klient');
const {until}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  const before=await o.cmd(`mmocore admin attribute check ${o.name} bieglosc-miecz`);
  const initial=Number(before.match(/(\d+)\s+(?:points|punkt)/i)?.[1]||0),grant=Math.max(0,1-initial);
  if(grant)await o.cmd(`mmocore admin attribute give ${o.name} bieglosc-miecz ${grant}`);
  try{
    for(const slot of [0,5])await w.step(`BR3M-mmiecz-slot${slot}`,async()=>{
      await scene.prepare({target:false});
      await o.cmd(`minecraft:clear ${o.name} minecraft:stone_sword`);
      await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);
      const source=await until(()=>{const n=k.bot.inventory.slots.findIndex((x,i)=>i>=9&&i<=44&&x?.name==='stone_sword');return n<0?null:n;},'miecz MMOItems',5000);
      const dest=36+slot;
      if(source!==dest){k.e();try{await k.bot.clickWindow(source,0,0);await k.bot.clickWindow(dest,0,0);}finally{k.zamknij();}}
      k.cyfra(slot+1);await sleep(350);
      await o.cmd('minecraft:summon minecraft:husk 0 80 2 {NoAI:1b,PersistenceRequired:1b,Tags:["BR3MTarget"]}');
      await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(350);
      const target='@e[type=minecraft:husk,tag=BR3MTarget,limit=1]',hp0=await o.hp(target),selected=await o.selected();
      const mob=await until(()=>Object.values(k.bot.entities).find(e=>e.name==='husk'&&e.position.distanceTo(k.bot.entity.position)<5),'husk BR3M',2000);
      for(let i=0;i<2;i++){k.bot.attack(mob);await sleep(1000);}
      const hp1=await o.hp(target),damage=hp0-hp1,held=/bron:s100c?\/stone_sword/.test(selected);
      w.add(`BR3M-mmiecz-slot${slot}`,held&&(slot===0?damage>0:Math.abs(damage)<0.01),
        `MMOItems miecz slot ${slot}: ${hp0}→${hp1} HP`,{slot,hp0,hp1,damage,held,source,selected:selected.slice(0,160)});
    });
  }finally{if(grant)await o.cmd(`mmocore admin attribute take ${o.name} bieglosc-miecz ${grant}`);}
};
