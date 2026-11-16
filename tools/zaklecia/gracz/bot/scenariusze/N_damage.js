'use strict';
const {sleep}=require('../lib/klient');
const {until}=require('../lib/menu');
const {Vec3}=require('vec3');
module.exports=async({k,o,scene,w})=>{
  for(const [spell,slot] of [['plomienie',5],['odmrozenie',5]])await w.step(`N2-${spell}-slot${slot}`,async()=>{
    await scene.prepare({target:false});
    await o.cmd(`minecraft:item replace entity ${o.name} hotbar.${slot} with minecraft:stone_sword`);
    k.cyfra(slot+1);
    await until(async()=>k.bot.quickBarSlot===slot,'slot '+slot,1000);
    await sleep(150);
    await o.cmd(`zaklecia przypisz ${o.name} ${spell} lewa`);
    await o.cmd('minecraft:kill @e[type=minecraft:husk,distance=..12]');
    await o.cmd('minecraft:summon minecraft:husk 0 80 5 {NoAI:1b,PersistenceRequired:1b,Tags:["N2Target"]}');
    await sleep(550);
    await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);
    await sleep(250);
    const target='@e[type=minecraft:husk,tag=N2Target,limit=1]';
    const hp0=await o.hp(target),mana0=await o.mana(),selected=await o.selected(),offhand=await o.placeholder('lewa');
    await k.ppmTrzymaj(850);
    await sleep(200);
    const hp1=await o.hp(target),mana1=await o.mana();
    const minDamage=spell==='plomienie'?5:1; // Ogień z bazowego skilla podpala mimo anulowanego uderzenia.
    w.add(`N2-${spell}-slot${slot}`,Number.isFinite(hp1)&&hp0-hp1>minDamage,
      'czar trafia huska, gdy aktywny jest slot >=2',
      {hp0,hp1,hpDelta:hp0-hp1,mana0,mana1,selected:selected.slice(0,100),offhand,slot:k.bot.quickBarSlot},
      {hpDelta:`>${minDamage}`});
  });
};
