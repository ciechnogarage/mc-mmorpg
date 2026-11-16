'use strict';
const {sleep}=require('../lib/klient');
const {until}=require('../lib/menu');
const {Vec3}=require('vec3');
module.exports=async({k,o,scene,w,connect})=>{
  const husk='@e[type=minecraft:husk,tag=B6Rear,limit=1]';
  const spell=async()=>{await o.cmd(`zaklecia przypisz ${o.name} plomienie lewa`);await until(async()=>await o.placeholder('lewa')==='Płomienie','lewy czar',2500);await sleep(250);};
  const cast=async(target=husk)=>{const before=await o.hp(target);k.ppmWcisnij();for(let i=0;i<4;i++){await sleep(210);k.ppmWcisnij();}k.ppmPusc();await sleep(180);const after=await o.hp(target);return {before,after,damage:before-after};};
  const aim=async(x,y,z=5.5)=>{await k.bot.lookAt(new Vec3(x,y,z),true);await sleep(180);};
  const spawn=async(x=0.5)=>{await o.cmd(`minecraft:summon minecraft:husk ${x} 80 5.5 {NoAI:1b,PersistenceRequired:1b,Tags:["B6Rear"]}`);await spell();};
  await w.step('B6Occ-wilk',async()=>{
    await scene.prepare({target:false});await o.cmd(`zaklecia przypisz ${o.name} duch_wilka prawa`);await sleep(350);await aim(0.5,80.2,3.5);k.lpmKlik();
    await until(async()=>/Test passed/.test(await o.cmd('minecraft:execute positioned 0 80 0 if entity @e[type=minecraft:wolf,distance=..12]')),'wilk',3500);
    await o.cmd('minecraft:execute positioned 0 80 0 run minecraft:tp @e[type=minecraft:wolf,limit=1,sort=nearest] 0.5 80.6 2.5');
    await o.cmd('minecraft:execute positioned 0 80 0 run minecraft:data merge entity @e[type=minecraft:wolf,limit=1,sort=nearest] {NoAI:1b,NoGravity:1b}');
    await o.cmd(`zaklecia przypisz ${o.name} nic prawa`);await spawn();await aim(0.5,80.95);const center=await cast();
    await aim(0.5,82.0);const head=await cast();
    w.add('B6Occ-wilk-srodek-glowa',center.damage<0.1&&head.damage>1,`środek=${center.damage}, głowa=${head.damage}`,{center,head});
  });
  let front,rear;
  try{
    front=await connect('MagPrzod');rear=await connect('MagCel');
    await w.step('B6Occ-PvP-dwaj',async()=>{
      await scene.prepare({target:false});await o.cmd('rg flag -w world __global__ pvp allow');
      await o.cmd('minecraft:tp MagPrzod 0.5 80 2.5 180 0');await o.cmd('minecraft:tp MagCel 0.5 80 5.5 180 0');
      await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');
      await o.cmd('minecraft:effect give MagPrzod minecraft:resistance 15 255 true');await o.cmd('minecraft:effect give MagCel minecraft:resistance 1 255 true');await sleep(1200);
      await spell();await aim(0.5,80.95);const blocked=await cast('MagCel');
      await o.cmd('minecraft:tp MagPrzod 3.5 80 2.5 180 0');await sleep(300);const open=await cast('MagCel');
      w.add('B6Occ-PvP-dwaj',blocked.damage<0.1&&open.damage>1,`za graczem=${blocked.damage}, po odsunięciu=${open.damage}`,{blocked,open});
    });
  }finally{await o.cmd('rg flag -w world __global__ pvp deny');front?.quit();rear?.quit();await sleep(300);}
  await w.step('B6Occ-pol-muru',async()=>{
    const samples=[];
    for(const x of [0.5,0.75,0.8125,0.875,0.9,0.925,1.0,1.1,1.3]){
      await scene.prepare({target:false});await spawn(x);await o.cmd('minecraft:fill 0 80 4 0 81 4 minecraft:stone');
      await aim(x+0.24,81.55);const result=await cast();samples.push({x,...result});
    }
    w.add('B6Occ-pol-muru',samples[0].damage<0.1&&samples.some(v=>v.x>0.5&&v.damage>1),
      samples.map(v=>`${v.x}:${v.damage}`).join(' '),{samples});
  });
};
