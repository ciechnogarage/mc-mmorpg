'use strict';
const {sleep}=require('../lib/klient');
const {until}=require('../lib/menu');
module.exports=async ctx=>{
  const {k,o,scene,w}=ctx;
  await require('./PERF2')(ctx,'20');
  const cmd='minecraft:execute in minecraft:overworld positioned 0 80 0 as @e[type=!minecraft:player,distance=..24] run minecraft:data get entity @s';
  const countCmd='minecraft:execute in minecraft:overworld positioned 0 80 0 if entity @e[type=!minecraft:player,distance=..24]';
  const count=async()=>{const s=await o.cmd(countCmd);return /Test failed/i.test(s)?0:Number(s.match(/Count:\s*(\d+)/i)?.[1]||0);};
  const activeBefore={prawa:await o.placeholder('prawa'),lewa:await o.placeholder('lewa'),entities:await count()};
  k.ppmPusc();await o.cmd(`zaklecia przypisz ${o.name} nic prawa`);await o.cmd(`zaklecia przypisz ${o.name} nic lewa`);
  await until(async()=>{const prawa=await o.placeholder('prawa'),lewa=await o.placeholder('lewa');return prawa==='pusta'&&lewa==='pusta';},'obie dłonie bez zaklęcia',2500);
  await scene.clearArena();
  const first=await o.cmd(cmd),count0=await count();
  await sleep(1000);
  const second=await o.cmd(cmd),count1=await count();
  console.log('N_ENTITIES '+JSON.stringify({activeBefore,count0,count1,first:first.slice(0,2500),second:second.slice(0,2500)}));
  w.add('N_entities-identyfikacja',count0===0&&count1===0,
    `po cleanup ${count0} encji, po 1 s ${count1}: ${second.slice(0,180)}`,{activeBefore,count0,count1,first:first.slice(0,2500),second:second.slice(0,2500)});
};
