'use strict';
const {sleep}=require('../lib/klient');
const {until}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  for(const slot of [5,0])await w.step(`N2-melee-slot${slot}`,async()=>{
    await scene.prepare({target:false});
    await o.cmd(`zaklecia przypisz ${o.name} nic lewa`);
    await o.cmd(`zaklecia przypisz ${o.name} nic prawa`);
    await o.cmd(`minecraft:item replace entity ${o.name} hotbar.${slot} with minecraft:stone_sword`);
    k.cyfra(slot+1);
    await until(async()=>k.bot.quickBarSlot===slot,`slot ${slot}`,1000);
    await o.cmd('minecraft:summon minecraft:husk 0 80 2 {NoAI:1b,PersistenceRequired:1b,Tags:["N2Melee"]}');
    await sleep(400);
    const target='@e[type=minecraft:husk,tag=N2Melee,limit=1]';
    const mob=await until(()=>Object.values(k.bot.entities).find(e=>e.name==='husk'&&e.position.distanceTo(k.bot.entity.position)<5),'husk',1500);
    await k.bot.lookAt(mob.position.offset(0,1,0),true);
    const hp0=await o.hp(target);
    for(let i=0;i<2;i++){k.bot.attack(mob);await sleep(1000);}
    const hp1=await o.hp(target);
    w.add(`N2-melee-slot${slot}`,slot===5?hp1===hp0:hp1<hp0,
      'zwykły cios mieczem tylko ze slotu 0/1',{hp0,hp1,slot});
  });
};
