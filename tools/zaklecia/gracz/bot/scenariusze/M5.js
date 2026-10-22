'use strict';
const {Vec3}=require('vec3');const {sleep}=require('../lib/klient');
module.exports=async({k,o,scene,w})=>{
  await scene.prepare({target:false});await o.cmd(`minecraft:item replace entity ${o.name} hotbar.6 with minecraft:stone_sword`);k.cyfra(7);
  const tag='@e[type=minecraft:cow,tag=M5Target,limit=1]';
  for(const x of [0,17,30]){
    await o.cmd(`minecraft:fill ${x-2} 79 -2 ${x+2} 79 4 minecraft:stone`);
    await o.cmd(`minecraft:fill ${x-2} 80 -2 ${x+2} 84 4 minecraft:air`);
    await o.cmd('minecraft:kill @e[type=minecraft:cow,tag=M5Target]');
    await o.cmd(`minecraft:tp ${o.name} ${x}.5 80 0.5 0 0`);
    await o.cmd(`minecraft:summon minecraft:cow ${x} 80 2 {NoAI:1b,PersistenceRequired:1b,Tags:["M5Target"]}`);
    await sleep(800);await k.bot.lookAt(new Vec3(x+0.5,80.8,2.5),true);await sleep(850);
    const hp0=await o.hp(tag),invul=await o.cmd(`data get entity ${tag} Invulnerable`),t=k.target();
    k.lpmKlik();await sleep(900);const hp1=await o.hp(tag);
    const cmd=await o.cmd(`minecraft:damage ${tag} 2 minecraft:player_attack by ${o.name}`),hp2=await o.hp(tag);
    w.add(`M5-x${x}`,hp1<hp0||hp2<hp1,`x=${x} LPM ${hp0}→${hp1}; /damage player_attack ${hp1}→${hp2}; ${cmd}`,
      {clientDelta:hp0-hp1,commandDelta:hp1-hp2,invul,target:t.type,distance:t.distance,position:await o.cmd(`data get entity ${o.name} Pos`)});
  }
  await o.cmd('minecraft:kill @e[type=minecraft:cow,tag=M5Target]');
  for(const x of [17,30])await o.cmd(`minecraft:fill ${x-2} 79 -2 ${x+2} 79 4 minecraft:air`);
};
