'use strict';
const {sleep}=require('../lib/klient');
const {until}=require('../lib/menu');
const {Vec3}=require('vec3');

module.exports=async({k,o,scene,w})=>{
  const hp=tag=>o.hp(`@e[type=minecraft:husk,tag=${tag},limit=1]`);
  for(const [id,charge] of [['ognisty_pocisk',1000],['kula_ognia',1450]]){
    await w.step(`N_pociski-${id}`,async()=>{
      await scene.prepare({target:false});
      await o.cmd('minecraft:summon minecraft:husk 0 80 2.5 {NoAI:1b,PersistenceRequired:1b,Tags:["NPFront"]}');
      await o.cmd(`minecraft:summon minecraft:husk 0 80 ${id==='kula_ognia'?4:5} {NoAI:1b,PersistenceRequired:1b,Tags:["NPRear"]}`);
      await o.cmd(`zaklecia przypisz ${o.name} ${id} lewa`);
      await until(async()=>await o.placeholder('lewa')!=='pusta',`${id} w lewej`,2500);
      await sleep(400);await k.bot.lookAt(new Vec3(0.5,80.95,5.5),true);await sleep(120);
      const front0=await hp('NPFront'),rear0=await hp('NPRear'),mana0=await o.mana();
      await k.ppmTrzymaj(charge);await sleep(450);
      const front1=await hp('NPFront'),rear1=await hp('NPRear'),mana1=await o.mana();
      w.add(`N_pociski-${id}`,mana1<mana0&&front1<front0,
        `${id}: pierwszy ${front0}→${front1}, drugi ${rear0}→${rear1}`,
        {front0,front1,rear0,rear1,mana0,mana1});
    });
  }
};
