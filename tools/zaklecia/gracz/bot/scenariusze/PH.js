'use strict';
const {Vec3}=require('vec3');const {sleep}=require('../lib/klient');const {assign}=require('../lib/menu');
module.exports=async({k,o,scene,w,connect})=>{
  let target,observer;
  try{
    target=await connect('MagCel');observer=await connect('MagBlok');
    await scene.prepare({target:false});await o.cmd('rg flag -w world __global__ pvp deny');
    await o.cmd('minecraft:tp MagCel 0 80 4');await o.cmd('minecraft:tp MagBlok 9 80 0');
    await assign(k,o,'leczenie_dloni','lewa');await o.cmd(`minecraft:damage ${o.name} 6`);
    const s0=await o.hp(o.name);await k.ppmTrzymaj(1000);await sleep(250);const s1=await o.hp(o.name);
    w.add('PH-kontrola-leczenia',s1>s0+0.5,`samoleczenie ${s0}→${s1}`,{heal:s1-s0});await w.checker.check('PH-kontrola-leczenia');
    await scene.prepare({target:false});await o.cmd('minecraft:tp MagCel 0 80 4');
    await assign(k,o,'uzdrowienie_innego','lewa');await o.cmd('minecraft:damage MagCel 6');
    await k.bot.lookAt(new Vec3(0.5,80.8,4.5),true);await sleep(200);
    const h0=await o.hp('MagCel'),m0=await o.mana();await k.ppmTrzymaj(1100);await sleep(300);const h1=await o.hp('MagCel'),m1=await o.mana();
    w.add('PH-leczenie-innego',h1>h0+0.5&&m1<m0,`MagCel ${h0}→${h1}, mana ${m0}→${m1}`,{heal:h1-h0,manaDelta:m0-m1});await w.checker.check('PH-leczenie-innego');
  }finally{target?.quit();observer?.quit();}
};
