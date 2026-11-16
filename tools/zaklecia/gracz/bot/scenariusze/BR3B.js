'use strict';
const {Vec3}=require('vec3');
const {sleep}=require('../lib/klient');
const {until}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  const before=await o.cmd(`mmocore admin attribute check ${o.name} bieglosc-luk`);
  const points=Number(before.match(/(\d+)\s+(?:points|punkt)/i)?.[1]||0),grant=Math.max(0,1-points);
  if(grant)await o.cmd(`mmocore admin attribute give ${o.name} bieglosc-luk ${grant}`);
  try{for(const slot of [0,5])await w.step(`BR3B-luk-slot${slot}`,async()=>{
    await scene.prepare({target:false});
    await o.cmd(`zaklecia przypisz ${o.name} nic prawa`);
    await o.cmd(`zaklecia przypisz ${o.name} nic lewa`);
    await o.cmd(`minecraft:clear ${o.name} minecraft:bow`);
    await o.cmd(`mi give BOW LUK_MYSLIWSKI ${o.name} 1`);
    const source=await until(()=>{const n=k.bot.inventory.slots.findIndex((x,i)=>i>=9&&i<=44&&x?.name==='bow');return n<0?null:n;},'łuk MMOItems',5000);
    const dest=36+slot;
    if(source!==dest){k.e();try{await k.bot.clickWindow(source,0,0);await k.bot.clickWindow(dest,0,0);}finally{k.zamknij();}}
    k.cyfra(slot+1);await sleep(400);
    const selected=await o.selected(),held=/id: "minecraft:bow"/.test(selected);
    if(!held)throw Error(`łuk nie jest wybrany w slocie ${slot}: ${selected.slice(0,150)}`);
    await o.cmd(`minecraft:give ${o.name} minecraft:arrow 64`);
    await o.cmd('minecraft:summon minecraft:husk 0 80 5 {NoAI:1b,PersistenceRequired:1b,Tags:["BR3BTarget"]}');
    await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);await sleep(350);
    const target='@e[type=minecraft:husk,tag=BR3BTarget,limit=1]',hp0=await o.hp(target);
    await k.ppmTrzymaj(1300);await sleep(1300);
    const hp1=await o.hp(target),damage=hp0-hp1;
    w.add(`BR3B-luk-slot${slot}`,slot===0?damage>0:Math.abs(damage)<0.01,
      `MMOItems łuk slot ${slot}: ${hp0}→${hp1} HP`,{slot,source,selected:selected.slice(0,180),hp0,hp1,damage,held});
  });}finally{if(grant)await o.cmd(`mmocore admin attribute take ${o.name} bieglosc-luk ${grant}`);}
};
