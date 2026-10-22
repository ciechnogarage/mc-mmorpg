'use strict';
const {Vec3}=require('vec3');
const {sleep}=require('../lib/klient');
const {assign}=require('../lib/menu');

module.exports=async({k,o,scene,w})=>{
  await scene.prepare({target:false});
  await o.cmd('minecraft:effect give MagTest minecraft:instant_health 1 255 true');
  await sleep(300);
  const h0=await o.hp('MagTest');
  await o.cmd('minecraft:damage MagTest 4 minecraft:generic');await sleep(150);
  const h1=await o.hp('MagTest');
  await o.cmd('minecraft:effect give MagTest minecraft:instant_health 1 255 true');await sleep(400);
  await assign(k,o,'oslona','lewa');
  const mana0=await o.mana();
  for(let i=0;i<7;i++){k.ppmWcisnij();await sleep(180);}
  const mana1=await o.mana(),before=await o.hp('MagTest');
  await o.cmd('minecraft:damage MagTest 4 minecraft:generic');await sleep(150);
  const after=await o.hp('MagTest');k.ppmPusc();
  w.add('N_effects-oslona',h0-h1>3.8&&before-after<h0-h1-0.4,
    `Osłona: bez ${h0-h1} HP, z ${before-after} HP; mana ${mana0}→${mana1}`,
    {h0,h1,before,after,mana0,mana1});
  await scene.prepare({target:false});
  await o.cmd('minecraft:effect give MagTest minecraft:instant_health 1 255 true');
  await o.cmd('minecraft:summon minecraft:husk 0 80 1.5 {PersistenceRequired:1b,Tags:["NCalmControl"]}');
  const control0=await o.hp('MagTest');await sleep(2400);const control1=await o.hp('MagTest');
  await o.cmd('minecraft:kill @e[type=minecraft:husk,tag=NCalmControl]');
  await o.cmd('minecraft:effect give MagTest minecraft:instant_health 1 255 true');
  await o.cmd('minecraft:summon minecraft:husk 0 80 4 {NoAI:0b,PersistenceRequired:1b,Tags:["NCalm"]}');
  await o.cmd('minecraft:effect give @e[type=minecraft:husk,tag=NCalm] minecraft:slowness 20 255 true');
  await assign(k,o,'uspokojenie','lewa');
  await k.bot.lookAt(new Vec3(0.5,80.8,4.5),true);await sleep(200);
  const aimed=(()=>{try{const t=k.target();return `${t.type}:${t.entity?.name||''}:${t.distance}`;}catch(e){return String(e);}})();
  const m0=await o.mana(),ai0=await o.cmd('minecraft:data get entity @e[type=minecraft:husk,tag=NCalm,limit=1] NoAI');
  await k.ppmTrzymaj(1300);await sleep(400);
  const m1=await o.mana(),ai1=await o.cmd('minecraft:data get entity @e[type=minecraft:husk,tag=NCalm,limit=1] NoAI');
  await o.cmd('minecraft:effect clear @e[type=minecraft:husk,tag=NCalm] minecraft:slowness');
  await o.cmd('minecraft:tp @e[type=minecraft:husk,tag=NCalm] 0 80 1.5');
  const calm0=await o.hp('MagTest');await sleep(2400);const calm1=await o.hp('MagTest');
  w.add('N_effects-uspokojenie',m1<m0&&control1<control0&&calm1>=calm0-0.1,
    `Uspokojenie: mana ${m0}→${m1}, kontrola ${control0}→${control1} HP, po czarze ${calm0}→${calm1} HP`,
    {m0,m1,ai0,ai1,aimed,control0,control1,calm0,calm1});
};
