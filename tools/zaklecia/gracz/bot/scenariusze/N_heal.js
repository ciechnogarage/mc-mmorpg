'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');
const {Vec3}=require('vec3');

module.exports=async({k,o,scene,w,connect})=>{
  let target;
  try{
    target=await connect('MagCel');
    await o.cmd('rg flag -w world __global__ pvp allow');
    for(let i=1;i<=3;i++)await w.step('N5-uzdrowienie-'+i,async()=>{
      await scene.prepare({target:false});
      await scene.targetBot();
      await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');
      await sleep(700);
      await o.cmd('minecraft:damage MagCel 6');
      await o.cmd(`minecraft:item replace entity ${o.name} hotbar.0 with minecraft:air`);
      k.cyfra(1);
      await sleep(150);
      await assign(k,o,'uzdrowienie_innego','lewa');
      await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);
      await sleep(120);
      const hp0=await o.hp('MagCel'),mana0=await o.mana(),s=o.snapshot();
      await k.ppmTrzymaj(1250);
      await until(async()=>{const h=await o.hp('MagCel');return h>hp0+0.5?h:null},
        'leczenie MagCel',1600).catch(()=>null);
      const hp1=await o.hp('MagCel'),mana1=await o.mana();
      w.add('N5-uzdrowienie-'+i,hp1-hp0>4,
        'ładowane Uzdrowienie innego leczy wskazanego gracza',
        {...o.metrics(s),hp0,hp1,delta:hp1-hp0,mana0,mana1},
        {delta:'>4 HP'});
    });
  }finally{
    await o.cmd('rg flag -w world __global__ pvp deny').catch(()=>{});
    target?.quit();
  }
};
