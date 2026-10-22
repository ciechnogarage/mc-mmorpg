'use strict';
const {Vec3}=require('vec3');const {sleep}=require('../lib/klient');const {assign}=require('../lib/menu');
module.exports=async({k,o,scene,w,connect})=>{
  const peer=await connect('MagCel');
  try{
    await scene.prepare({target:false});await o.cmd('rg flag -w world __global__ pvp deny');
    await assign(k,o,'duch_wilka','prawa');
    for(let i=1;i<=12;i++)await w.step(`WF-sojusznik-${i}`,async()=>{
      await scene.clearArena();await o.cmd('minecraft:tp MagCel 0 80 5 180 0');
      await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');await sleep(180);
      await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');await sleep(350);
      await o.cmd(`mmocore admin resource-mana set ${o.name} 20`);
      await k.bot.lookAt(new Vec3(-1,81,5),true);await sleep(120);
      const h0=await o.hp('MagCel'),m0=await o.mana();k.lpmKlik();await sleep(950);
      const h1=await o.hp('MagCel'),m1=await o.mana();
      const reply=await o.cmd('minecraft:execute positioned 0 80 0 if entity @e[type=minecraft:wolf,distance=..12]');
      const summoned=/Test passed|Found [1-9]|Count: [1-9]/i.test(reply);
      w.add(`WF-sojusznik-${i}`,summoned&&h1>=h0-0.1,`MagCel ${h0}→${h1}, mana ${m0}→${m1}, wilk ${summoned}`,{hpDelta:h0-h1,manaDelta:m0-m1,summoned});
    });
  }finally{peer.quit();await o.cmd('rg flag -w world __global__ pvp deny').catch(()=>{});}
};
