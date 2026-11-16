'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  await scene.prepare({target:false});
  await o.cmd(`zaklecia przypisz ${o.name} nic prawa`);
  await o.cmd(`zaklecia przypisz ${o.name} nic lewa`);
  await assign(k,o,'odmrozenie','prawa');
  await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);
  const source=await until(()=>{const n=k.bot.inventory.slots.findIndex((x,i)=>i>=9&&i<=44&&x?.name==='stone_sword');return n<0?null:n;},'miecz MMOItems',5000);
  k.e();try{await k.bot.clickWindow(source,0,0);await k.bot.clickWindow(37,0,0);}finally{k.zamknij();}
  k.cyfra(2);await sleep(400);
  const before=await o.selected();k.f();await sleep(700);
  const off=await o.cmd(`minecraft:data get entity ${o.name} Inventory[{Slot:-106b}]`);
  k.cyfra(1);await sleep(250);
  const after=await o.selected();
  w.add('DB3F-miecz-lewa',/bron:s100c?\/stone_sword/.test(off),
    'miecz MMOItems w lewej po F z prawej dłoni',
    {source,before:before.slice(0,180),off:off.slice(0,180),after:after.slice(0,180),slots:k.bot.inventory.slots.map((x,i)=>x&&[i,x.name]).filter(Boolean)});
};
