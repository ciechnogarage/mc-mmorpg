'use strict';
const {sleep}=require('../lib/klient');const {until}=require('../lib/menu');const {Vec3}=require('vec3');
module.exports=async({k,o,scene,w})=>{
  const target='@e[type=minecraft:husk,tag=B6Edge,limit=1]';
  const samples=[];
  for(const [x,wall] of [[0.5,true],[0.95,true],[0.95,false],[0.975,true],[1.0,true],[1.0,false],[1.0,true],[1.1,true],[1.1,false],[1.3,true]]){
    await o.cmd(`zaklecia przypisz ${o.name} nic lewa`);k.ppmPusc();await sleep(700);
    await scene.prepare({target:false});await o.cmd(`minecraft:summon minecraft:husk ${x} 80 5.5 {NoAI:1b,PersistenceRequired:1b,Tags:["B6Edge"]}`);
    if(wall)await o.cmd('minecraft:fill 0 80 4 0 81 4 minecraft:stone');
    const block=await o.cmd('minecraft:execute if block 0 81 4 minecraft:stone');
    const player=await o.cmd(`minecraft:data get entity ${o.name} Pos`);
    await o.cmd(`zaklecia przypisz ${o.name} plomienie lewa`);await until(async()=>await o.placeholder('lewa')==='Płomienie','lewy czar',3000);
    await k.bot.lookAt(new Vec3(x+0.24,81.55,5.5),true);await sleep(400);
    const before=await o.hp(target);k.ppmWcisnij();for(let i=0;i<4;i++){await sleep(210);k.ppmWcisnij();}k.ppmPusc();await sleep(250);const after=await o.hp(target);
    samples.push({x,wall,before,after,damage:before-after,block:block.slice(0,100),player:player.slice(0,140)});
  }
  const damage=(x,wall)=>samples.find(s=>s.x===x&&s.wall===wall)?.damage;
  const detail=samples.map(s=>`${s.x}/${s.wall?'mur':'otwarte'}:${s.damage}`).join(' ');
  w.add('B6Edge-prog',damage(0.5,true)<0.1&&damage(0.95,true)>1&&damage(0.975,true)>1,
    detail,{samples});
  w.add('B6Edge-powtorzenie',samples[4].damage>1&&samples[6].damage>1&&damage(1.0,false)>1,
    detail,{samples});
};
