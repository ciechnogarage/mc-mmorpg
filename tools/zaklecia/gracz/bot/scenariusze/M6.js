'use strict';
const {Vec3}=require('vec3');const {sleep}=require('../lib/klient');
module.exports=async({k,o,scene,w})=>{
  const tag='@e[type=minecraft:cow,tag=M6Target,limit=1]';
  await scene.prepare({target:false});await o.cmd(`minecraft:item replace entity ${o.name} hotbar.6 with minecraft:stone_sword`);k.cyfra(7);
  const fresh=async()=>{await o.cmd(`minecraft:kill @e[type=minecraft:cow,tag=M6Target]`);await o.cmd('minecraft:summon minecraft:cow 0 80 2 {NoAI:1b,PersistenceRequired:1b,Tags:["M6Target"]}');await sleep(1500);await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(400);};
  await fresh();const hp0=await o.hp(tag),hurt0=await o.cmd(`data get entity ${tag} HurtTime`),by=await o.cmd(`minecraft:damage ${tag} 2 minecraft:player_attack by ${o.name}`),hp1=await o.hp(tag);
  w.add('M6-swieza-krowa-damage-player',hp1<hp0,`nietknięta krowa /damage player_attack ${hp0}→${hp1}; ${by}`,{hurt0,hpDelta:hp0-hp1});
  await fresh();const hp2=await o.hp(tag),before=await o.cmd(`data get entity ${tag} HurtTime`);k.lpmKlik();await sleep(100);const hurt=await o.cmd(`data get entity ${tag} HurtTime`),hp3=await o.hp(tag);
  await sleep(1200);const hurtAfter=await o.cmd(`data get entity ${tag} HurtTime`),after=await o.cmd(`minecraft:damage ${tag} 2 minecraft:player_attack by ${o.name}`),hp4=await o.hp(tag);
  w.add('M6-LPM-hurt',hp3<hp2||/\b[1-9]\d*b\b/.test(hurt),`LPM ${hp2}→${hp3}; HurtTime ${before}→${hurt}→${hurtAfter}; po 1,2s: ${after} ${hp3}→${hp4}`,
    {hpDelta:hp2-hp3,hurt,before,hurtAfter,commandDelta:hp3-hp4});
};
