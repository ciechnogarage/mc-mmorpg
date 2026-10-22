'use strict';
const {sleep}=require('../lib/klient');const {assign}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  await scene.prepare({target:false});
  await assign(k,o,'leczenie','lewa');
  for(let i=1;i<=3;i++)await w.step(`HP-przygotowanie-${i}`,async()=>{
    const healed=await o.cmd(`minecraft:effect give ${o.name} minecraft:instant_health 1 10 true`);
    await sleep(600);
    const h0=await o.hp(),reply=await o.cmd(`minecraft:damage ${o.name} 6`),h1=await o.hp();
    await sleep(350);const h2=await o.hp();
    w.add(`HP-przygotowanie-${i}`,h1<h0-1,`HP ${h0}→${h1}→${h2}; /damage: ${reply}; /heal: ${healed}`,{before:h0,immediate:h1,later:h2});
    if(h1<h0-1){await k.ppmTrzymaj(850);const h3=await o.hp();w.add(`HP-leczenie-${i}`,h3>h1+0.3,`HP ${h1}→${h3}`,{healed:h3-h1});}
  });
};
