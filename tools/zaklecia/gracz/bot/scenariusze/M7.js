'use strict';
const {Vec3}=require('vec3');const {sleep}=require('../lib/klient');
module.exports=async({k,o,scene,w})=>{
  await scene.prepare({target:false});await o.cmd(`minecraft:item replace entity ${o.name} hotbar.6 with minecraft:stone_sword`);k.cyfra(7);await sleep(1000);
  for(const [type,noAI] of [['cow',0],['cow',1],['husk',0],['husk',1]]){
    const tag=`@e[type=minecraft:${type},tag=M7Target,limit=1]`;
    await o.cmd('minecraft:execute in minecraft:overworld positioned 0 80 0 run minecraft:kill @e[type=!minecraft:player,distance=..10]');
    await o.cmd(`minecraft:summon minecraft:${type} 0 80 2 {NoAI:${noAI}b,PersistenceRequired:1b,Tags:["M7Target"]}`);
    await sleep(700);await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(700);
    const hp0=await o.hp(tag),t=k.target(),uuid=await o.cmd(`data get entity ${tag} UUID`);
    for(let i=0;i<3;i++){k.lpmKlik();await sleep(850);}const hp1=await o.hp(tag);
    w.add(`M7-${type}-NoAI${noAI}`,hp1<hp0,`${type} NoAI=${noAI}: 3 LPM, HP ${hp0}→${hp1}`,
      {hpDelta:hp0-hp1,target:t.type,distance:t.distance,entityId:t.entity?.id,clientUuid:t.entity?.uuid,serverUuid:uuid});
  }
};
