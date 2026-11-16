'use strict';
const {sleep}=require('../lib/klient');
const {until}=require('../lib/menu');
const {Vec3}=require('vec3');

module.exports=async({k,o,scene,w,connect})=>{
  const rear='@e[type=minecraft:husk,tag=NZRear,limit=1]';
  const front='@e[type=minecraft:husk,tag=NZFront,limit=1]';
  const aim=async()=>{await k.bot.lookAt(new Vec3(0.5,80.95,5.5),true);await sleep(180);};
  const spell=async()=>{
    await o.cmd(`zaklecia przypisz ${o.name} plomienie lewa`);
    await until(async()=>await o.placeholder('lewa')==='Płomienie', 'Płomienie w lewej',2500);
    await sleep(350);
  };
  const husk=async(tag,z)=>o.cmd(`minecraft:summon minecraft:husk 0 80 ${z} {NoAI:1b,PersistenceRequired:1b,Tags:["${tag}"]}`);
  const cast=async()=>{
    const hp0=await o.hp(rear),mana0=await o.mana();
    k.ppmWcisnij();
    for(let i=0;i<4;i++){await sleep(210);k.ppmWcisnij();}
    const visual=await o.cmd('minecraft:execute positioned 0 80 0 run minecraft:data get entity @e[type=minecraft:item_display,limit=1,sort=nearest] transformation.scale');
    k.ppmPusc();await sleep(180);
    const hp1=await o.hp(rear),mana1=await o.mana();
    const visualLength=Number(visual.match(/\[1\.0f,\s*1\.0f,\s*([\d.]+)f\]/)?.[1]);
    return {hp0,hp1,damage:hp0-hp1,mana0,mana1,visual,visualLength};
  };
  await w.step('N_zaslona-kontrola',async()=>{
    await scene.prepare({target:false});await husk('NZRear',5);await spell();await aim();
    const x=await cast();w.add('N_zaslona-kontrola',x.damage>1&&x.visualLength>3,'niezasłonięty husk otrzymuje obrażenia',x);
  });
  let friend;
  try{
    friend=await connect('MagCel');
    await w.step('N_zaslona-sojusznik',async()=>{
      await scene.prepare({target:false});await husk('NZRear',5);
      await o.cmd('minecraft:tp MagCel 0.5 80 2.5 180 0');
      await o.cmd('minecraft:effect give MagCel minecraft:resistance 10 255 true');
      await spell();await aim();const ally0=await o.hp('MagCel'),x=await cast(),ally1=await o.hp('MagCel');
      w.add('N_zaslona-sojusznik',x.damage<0.1&&ally1>=ally0-0.1&&x.visualLength<2.5,
        'sojusznik pochłania strumień bez obrażeń, husk za nim pozostaje cały',{...x,ally0,ally1});
    });
  }finally{friend?.quit();await sleep(350);}
  await w.step('N_zaslona-drugi-husk',async()=>{
    await scene.prepare({target:false});await husk('NZRear',5);await husk('NZFront',2.5);
    await spell();await aim();const front0=await o.hp(front),x=await cast(),front1=await o.hp(front);
    w.add('N_zaslona-drugi-husk',front0-front1>1&&x.damage<0.1&&x.visualLength<2.5,
      'pierwszy husk trafiony, drugi za nim osłonięty',{...x,front0,front1,frontDamage:front0-front1});
  });
  await w.step('N_zaslona-wlasny-wilk',async()=>{
    await scene.prepare({target:false});
    await o.cmd(`zaklecia przypisz ${o.name} duch_wilka prawa`);
    await until(async()=>await o.placeholder('prawa')==='Duch wilka','Duch wilka w prawej',2500);
    await sleep(350);await k.bot.lookAt(new Vec3(0.5,80,3.5),true);
    k.lpmKlik();
    await until(async()=>{
      const s=await o.cmd('minecraft:execute positioned 0 80 0 if entity @e[type=minecraft:wolf,distance=..12]');
      return /Test passed/i.test(s);
    },'własny wilk',3000);
    await o.cmd('minecraft:execute positioned 0 80 0 run minecraft:tp @e[type=minecraft:wolf,limit=1,sort=nearest] 0.5 80.6 2.5');
    await o.cmd('minecraft:execute positioned 0 80 0 run minecraft:data merge entity @e[type=minecraft:wolf,limit=1,sort=nearest] {NoAI:1b,NoGravity:1b}');
    const owner=await o.cmd('minecraft:execute positioned 0 80 0 run minecraft:data get entity @e[type=minecraft:wolf,limit=1,sort=nearest] BukkitValues');
    await o.cmd(`zaklecia przypisz ${o.name} nic prawa`);await husk('NZRear',5);await spell();await aim();
    const x=await cast();w.add('N_zaslona-wlasny-wilk-srodek',x.damage<0.1&&x.visualLength<2.5,
      'własny wilk zasłania środek huska i skraca model',{...x,owner:owner.slice(0,500)});
    await k.bot.lookAt(new Vec3(0.5,82.0,5.5),true);await sleep(200);
    const head=await cast();w.add('N_zaslona-wlasny-wilk-glowa',head.damage>1&&head.visualLength>3.5,
      'celowanie w widoczną głowę trafia huska i nie skraca modelu na wilku',head);
  });
  await w.step('N_zaslona-blok',async()=>{
    await scene.prepare({target:false});await husk('NZRear',5);
    await o.cmd('minecraft:fill -1 80 3 1 83 3 minecraft:stone');
    await spell();await aim();const x=await cast();
    w.add('N_zaslona-blok',x.damage<0.1&&x.visualLength<3,'blok nadal zasłania huska',x);
  });
};
