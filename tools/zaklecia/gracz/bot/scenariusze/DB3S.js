'use strict';
const {Vec3}=require('vec3');
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');
const TYPES=require('./B').TYPES;
module.exports=async({k,o,scene,w})=>{
  const attr=await o.cmd(`mmocore admin attribute check ${o.name} bieglosc-miecz`);
  const points=Number(attr.match(/(\d+)\s+(?:points|punkt)/i)?.[1]||0),grant=Math.max(0,1-points);
  if(grant)await o.cmd(`mmocore admin attribute give ${o.name} bieglosc-miecz ${grant}`);
  try{for(const [id,type] of Object.entries(TYPES))for(const orientation of ['bron-prawa','bron-lewa'])
    await w.step(`DB3S-${id}-${orientation}`,async()=>{
      await scene.prepare({target:false});
      await o.cmd(`zaklecia przypisz ${o.name} nic prawa`);
      await o.cmd(`zaklecia przypisz ${o.name} nic lewa`);
      await o.cmd(`minecraft:clear ${o.name} minecraft:stone_sword`);
      await assign(k,o,id,orientation==='bron-prawa'?'lewa':'prawa');
      await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);
      const src=await until(()=>{const n=k.bot.inventory.slots.findIndex((x,i)=>i>=9&&i<=44&&x?.name==='stone_sword');return n<0?null:n;},'miecz MMOItems',5000);
      const dest=orientation==='bron-prawa'?36:45;
      if(src!==dest){k.e();try{await k.bot.clickWindow(src,0,0);await k.bot.clickWindow(dest,0,0);}finally{k.zamknij();}}
      if(orientation==='bron-prawa')k.cyfra(1);
      await sleep(500);
      const armedSelected=await o.selected(),armedOff=await o.cmd(`minecraft:data get entity ${o.name} Inventory[{Slot:-106b}]`);
      const weaponReady=orientation==='bron-prawa'?armedSelected.includes('bron:s100/stone_sword'):armedOff.includes('bron:s100/stone_sword');
      if(!weaponReady)throw Error(`miecz nie jest w ${orientation}: selected=${armedSelected.slice(0,130)} off=${armedOff.slice(0,130)}`);
      if(id==='runa_ognia')await k.bot.lookAt(new Vec3(0.5,79.8,3.5),true);
      else if(['leczenie','oslona','zasklepienie_ran','wielkie_uzdrowienie','przywolany_miecz'].includes(id))await k.bot.look(0,1.3,true);
      else await k.bot.lookAt(new Vec3(0.5,80.8,id==='uspokojenie'?2.5:5.5),true);
      if(['leczenie','zasklepienie_ran','wielkie_uzdrowienie'].includes(id))await o.cmd(`minecraft:damage ${o.name} 6`);
      else if(id==='uspokojenie')await o.cmd('minecraft:summon minecraft:husk 0 80 2 {NoAI:1b,PersistenceRequired:1b}');
      else if(!['leczenie_dloni','uzdrowienie_innego','oslona','przywolany_miecz'].includes(id))
        await o.cmd('minecraft:summon minecraft:husk 0 80 5 {NoAI:1b,PersistenceRequired:1b}');
      const mana0=await o.mana(),hp0=await o.hp(o.name),held0=await o.selected();
      const duration=type==='koncentracja'||type==='tarcza'?850:id==='wielkie_uzdrowienie'?1700:1350;
      if(orientation==='bron-prawa')await k.ppmTrzymaj(duration);
      else{k.lpmKlik();await sleep(duration);if(type==='koncentracja'||type==='tarcza')k.lpmKlik();}
      await sleep(250);
      const mana1=await o.mana(),hp1=await o.hp(o.name),held1=await o.selected(),off1=await o.cmd(`minecraft:data get entity ${o.name} Inventory[{Slot:-106b}]`);
      const swordPresent=orientation==='bron-prawa'?held1.includes('bron:s100/stone_sword'):off1.includes('bron:s100/stone_sword');
      w.add(`DB3S-${id}-${orientation}`,swordPresent,
        `${id} ${orientation}: mana ${mana0}→${mana1}, HP ${hp0}→${hp1}, broń ${swordPresent?'obecna':'utracona'}`,
        {id,type,orientation,mana0,mana1,hp0,hp1,armedSelected:armedSelected.slice(0,140),armedOff:armedOff.slice(0,140),held0:held0.slice(0,90),held1:held1.slice(0,90),off1:off1.slice(0,140),swordPresent});
    });
  }finally{if(grant)await o.cmd(`mmocore admin attribute take ${o.name} bieglosc-miecz ${grant}`);}
};
