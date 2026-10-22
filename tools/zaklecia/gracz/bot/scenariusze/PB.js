'use strict';
const {Vec3}=require('vec3');const {sleep}=require('../lib/klient');const {assign}=require('../lib/menu');
module.exports=async({k,o,scene,w,connect})=>{
  const peers=[];const add=async name=>{const b=await connect(name);peers.push(b);return b;};
  const target='@e[type=minecraft:husk,tag=PBTarget,limit=1]';
  const prepare=async(block)=>{
    await scene.prepare({target:false});await o.cmd('minecraft:tp MagCel 9 80 0');await o.cmd(`minecraft:tp MagBlok ${block?'0 80 2':'9 80 1'}`);
    await o.cmd('minecraft:tp MagWspol -8 80 0');await assign(k,o,'plomienie','lewa');
    await o.cmd('minecraft:summon minecraft:husk 0 80 5 {NoAI:1b,Tags:["PBTarget"]}');
    await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);await sleep(200);
  };
  try{
    await add('MagCel');await add('MagBlok');await add('MagWspol');
    await prepare(false);const h0=await o.hp(target);await k.ppmTrzymaj(850);const h1=await o.hp(target),clear=h0-h1;
    w.add('PB-kontrola-strumienia',clear>0.5,`bez zasłony husk ${h0}→${h1}`,{damage:clear});await w.checker.check('PB-kontrola-strumienia');
    await prepare(true);const b0=await o.hp(target);await k.ppmTrzymaj(850);const b1=await o.hp(target),blocked=b0-b1;
    w.add('PB-zaslanianie',clear>0.5&&blocked<clear-0.1,`blokada gracza: obrażenia huska ${blocked}, bez blokady ${clear}`,{blocked,clear});await w.checker.check('PB-zaslanianie');
    await scene.prepare({target:false});await o.cmd('minecraft:tp MagBlok 9 80 1');await assign(k,o,'plomienie','lewa');
    await o.cmd('minecraft:summon minecraft:husk -3 80 5 {NoAI:1b,Tags:["PBLeft"]}');
    await o.cmd('minecraft:summon minecraft:husk 3 80 5 {NoAI:1b,Tags:["PBRight"]}');
    await k.bot.lookAt(new Vec3(-3,80.8,5),true);k.ppmWcisnij();for(let i=0;i<3;i++){await sleep(200);k.ppmWcisnij();}
    await k.bot.lookAt(new Vec3(3,80.8,5),true);for(let i=0;i<3;i++){await sleep(200);k.ppmWcisnij();}k.ppmPusc();await sleep(200);
    const left=await o.hp('@e[type=minecraft:husk,tag=PBLeft,limit=1]'),right=await o.hp('@e[type=minecraft:husk,tag=PBRight,limit=1]');
    w.add('PB-zmiana-celu',left<20&&right<20,`po zmianie celownika L=${left}, P=${right}`,{left,right});await w.checker.check('PB-zmiana-celu');
  }finally{for(const p of peers)p.quit();}
};
