'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');

module.exports=async({k,o,scene,w})=>{
  const attr=await o.cmd(`mmocore admin attribute check ${o.name} bieglosc-miecz`);
  const points=Number(attr.match(/(\d+)\s+(?:points|punkt)/i)?.[1]||0),grant=Math.max(0,1-points);
  if(grant)await o.cmd(`mmocore admin attribute give ${o.name} bieglosc-miecz ${grant}`);
  try{
    for(const orientation of ['bron-prawa','bron-lewa'])await w.step(`DB3R-${orientation}`,async()=>{
      await scene.prepare({target:false});
      await o.cmd(`zaklecia przypisz ${o.name} nic prawa`);
      await o.cmd(`zaklecia przypisz ${o.name} nic lewa`);
      const hand=orientation==='bron-prawa'?'lewa':'prawa';
      await assign(k,o,'odmrozenie',hand);
      const given=await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);
      const source=await until(()=>{
        const n=k.bot.inventory.slots.findIndex((x,i)=>i>=9&&i<=44&&x?.name==='stone_sword');
        return n<0?null:n;
      },'miecz MMOItems',5000);
      const dest=orientation==='bron-prawa'?36:45;
      k.e();try{await k.bot.clickWindow(source,0,0);await k.bot.clickWindow(dest,0,0);}finally{k.zamknij();}
      k.cyfra(orientation==='bron-prawa'?1:2);await sleep(650);
      const selected=await o.selected();
      const off=await o.cmd(`minecraft:data get entity ${o.name} Inventory[{Slot:-106b}]`);
      const inventory=k.bot.inventory.slots.map((x,i)=>x&&[i,x.name,x.displayName]).filter(Boolean);
      const weaponOk=orientation==='bron-prawa'?selected.includes('bron:s100/stone_sword'):off.includes('bron:s100/stone_sword');
      w.add(`DB3R-${orientation}`,weaponOk,`${orientation}: miecz ${weaponOk?'w dłoni':'nie jest w dłoni'}`,
        {orientation,given,source,dest,selected:selected.slice(0,250),off:off.slice(0,250),inventory});
    });
  }finally{if(grant)await o.cmd(`mmocore admin attribute take ${o.name} bieglosc-miecz ${grant}`);}
};
