'use strict';
const {Vec3}=require('vec3');
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');

module.exports=async({k,o,scene,w})=>{
  const target='@e[type=minecraft:husk,tag=BR3Target,limit=1]';
  for(const kind of ['miecz','luk'])for(const slot of [0,5])await w.step(`BR3-${kind}-slot${slot}`,async()=>{
    await scene.prepare({target:false});
    await o.cmd(`minecraft:item replace entity ${o.name} hotbar.${slot} with minecraft:${kind==='miecz'?'stone_sword':'bow'}`);
    if(kind==='luk')await o.cmd(`minecraft:give ${o.name} minecraft:arrow 64`);
    await o.cmd(`minecraft:summon minecraft:husk 0 80 ${kind==='miecz'?2:5} {NoAI:1b,PersistenceRequired:1b,Tags:["BR3Target"]}`);
    k.cyfra(slot+1);await until(async()=>/minecraft:(stone_sword|bow)/.test(await o.selected()),'wybór broni',1500);
    await k.bot.lookAt(new Vec3(0.5,80.8,kind==='miecz'?2.5:5.5),true);await sleep(350);
    const hp0=await o.hp(target),selected=await o.selected();
    if(kind==='miecz'){k.lpmKlik();await sleep(500);}else{k.ppmWcisnij();await sleep(1050);k.ppmPusc();await sleep(1100);}
    const hp1=await o.hp(target),damage=hp0-hp1;
    w.add(`BR3-${kind}-slot${slot}`,slot===0?damage>0:Math.abs(damage)<0.01,
      `${kind}, slot ${slot}: ${hp0}→${hp1} HP`,{kind,slot,selected:selected.slice(0,90),damage});
  });
  for(const id of ['ognisty_pocisk','kula_ognia'])await w.step(`BR3-${id}-slot5`,async()=>{
    await scene.prepare({target:false});
    await assign(k,o,id,'lewa');
    await o.cmd(`minecraft:item replace entity ${o.name} hotbar.5 with minecraft:stone_sword`);
    k.cyfra(6);await until(async()=>/minecraft:stone_sword/.test(await o.selected()),'miecz w slocie 5',1500);
    await o.cmd('minecraft:summon minecraft:husk 0 80 5 {NoAI:1b,PersistenceRequired:1b,Tags:["BR3Target"]}');
    await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);await sleep(220);
    const hp0=await o.hp(target),mana0=await o.mana(),selected=await o.selected();
    await k.ppmTrzymaj(1350);await sleep(500);
    const hp1=await o.hp(target),mana1=await o.mana();
    w.add(`BR3-${id}-slot5`,mana1<mana0&&(hp1<hp0||Number.isNaN(hp1)),
      `${id}: ${hp0}→${hp1} HP, mana ${mana0}→${mana1}`,{id,hp0,hp1,mana0,mana1,selected:selected.slice(0,100)});
  });
};
