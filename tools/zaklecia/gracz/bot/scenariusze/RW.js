'use strict';
const {Vec3}=require('vec3');const {sleep}=require('../lib/klient');const {assign}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  const summary={};
  for(const id of ['plomienie','odmrozenie'])for(const wall of [true,false]){
    await scene.prepare({target:false});
    if(!wall)await o.cmd('minecraft:fill 0 80 7 3 83 7 minecraft:air');
    await assign(k,o,id,'lewa');summary[`${id}-${wall}`]=[];
    for(let rep=1;rep<=5;rep++)await w.step(`RW-${id}-${wall?'sciana':'otwarte'}-${rep}`,async()=>{
      await scene.clearArena();await o.cmd(`mmocore admin resource-mana set ${o.name} 20`);
      await o.cmd('minecraft:summon minecraft:husk 0 80 5.5 {NoAI:1b,PersistenceRequired:1b,Tags:["RWTarget"]}');
      await k.bot.lookAt(new Vec3(0.5,80.8,6),true);await sleep(220);
      const target='@e[type=minecraft:husk,tag=RWTarget,limit=1]',h0=await o.hp(target),m0=await o.mana();
      await k.ppmTrzymaj(850);const h1=await o.hp(target),m1=await o.mana(),damage=h0-h1;
      summary[`${id}-${wall}`].push(damage);
      w.add(`RW-${id}-${wall?'sciana':'otwarte'}-${rep}`,m1<m0,`${id} ściana=${wall} HP ${h0}→${h1}, mana ${m0}→${m1}`,{damage,manaDelta:m0-m1});
    });
  }
  for(const id of ['plomienie','odmrozenie'])w.add(`RW-kontrast-${id}`,true,`5 prób ze ścianą i bez: ${summary[`${id}-true`]} / ${summary[`${id}-false`]}`);
};
