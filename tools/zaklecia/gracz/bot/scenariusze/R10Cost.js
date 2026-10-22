'use strict';
const {sleep}=require('../lib/klient');
const {until}=require('../lib/menu');
const {Vec3}=require('vec3');

module.exports=async({k,o,scene,w})=>{
  for(const count of [1,5,10]){
    await scene.prepare({target:false});
    for(let i=0;i<count;i++){
      const x=0.5+(i-(count-1)/2)*0.34;
      await o.cmd(`minecraft:summon minecraft:husk ${x} 80 5.5 {NoAI:1b,PersistenceRequired:1b,Tags:["R10Cost"]}`);
    }
    await o.cmd('minecraft:effect give @e[type=minecraft:husk,tag=R10Cost] minecraft:resistance 60 255 true');
    await o.cmd(`zaklecia przypisz ${o.name} plomienie lewa`);
    await until(async()=>await o.placeholder('lewa')==='Płomienie','lewy czar',3000);
    await k.bot.lookAt(new Vec3(0.5,80.95,5.5),true);
    await sleep(250);
    await o.cmd(`minecraft:say R10COST-${count}-START`);
    k.ppmWcisnij();
    const presses=count===1?60:count===5?18:12;
    for(let i=0;i<presses;i++){await sleep(210);k.ppmWcisnij();}
    k.ppmPusc();await sleep(300);
    await o.cmd(`minecraft:say R10COST-${count}-END`);
    w.add(`R10Cost-${count}`,true,`${count} kandydatów, ${presses} odświeżeń PPM`);
  }
};
