'use strict';
const {Vec3}=require('vec3');const {sleep}=require('../lib/klient');const {assign}=require('../lib/menu');const {lore}=require('../lib/macierz');
module.exports=async({k,o,scene,w})=>{
  const cases=[['zasieg-wewnatrz',5.5,0,true],['zasieg-poza',6.5,0,false],['kat-wewnatrz',5.5,16,true],['kat-na-hitboxie',5.5,22,true],['kat-po-granicy',5.5,25,false],['kat-poza',5.5,26,false]];
  for(const id of ['plomienie','odmrozenie']){
    await scene.prepare({target:false});await assign(k,o,id,'lewa');
    // Zasięg mierzymy w otwartym polu; scena domyślna stawia ścianę na z=7.
    await o.cmd('minecraft:fill 0 80 7 3 83 7 minecraft:air');
    const l=lore(id);w.add(`RANGE-lore-${id}`,l.range===6,`${id}: lore zasięg ${l.range}, config stożek 6 bl./półkąt 20°`);
    for(const [caseId,distance,angle,inside] of cases)for(let rep=1;rep<=5;rep++)await w.step(`RANGE-${id}-${caseId}-${rep}`,async()=>{
      await scene.clearArena();await o.cmd(`mmocore admin resource-mana set ${o.name} 20`);
      const rad=angle*Math.PI/180,x=0.5+Math.sin(rad)*distance,z=0.5+Math.cos(rad)*distance;
      await o.cmd(`minecraft:summon minecraft:husk ${x.toFixed(3)} 80 ${z.toFixed(3)} {NoAI:1b,PersistenceRequired:1b,Tags:["RangeTarget"]}`);
      await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);await sleep(220);
      const hp0=await o.hp('@e[type=minecraft:husk,tag=RangeTarget,limit=1]'),m0=await o.mana(),s=o.snapshot();
      await k.ppmTrzymaj(850);
      const hp1=await o.hp('@e[type=minecraft:husk,tag=RangeTarget,limit=1]'),m1=await o.mana(),damage=hp0-hp1;
      w.add(`RANGE-${id}-${caseId}-${rep}`,inside?damage>0.1:Math.abs(damage)<0.1,
        `${id}: ${caseId}, d=${distance}, kąt=${angle}°, HP ${hp0}→${hp1}, mana ${m0}→${m1}`,
        {...o.metrics(s),distance,angle,damage,manaDelta:m0-m1,loreRange:l.range,expectedHit:inside});
    });
  }
};
