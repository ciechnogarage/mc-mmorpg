'use strict';
const {Vec3}=require('vec3');
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');

module.exports=async({k,o,scene,w})=>{
  const skill=await o.cmd(`mmocore admin attribute check ${o.name} bieglosc-miecz`);
  const points=Number(skill.match(/(\d+)\s+(?:points|punkt)/i)?.[1]||0);
  if(points<1)await o.cmd(`mmocore admin attribute give ${o.name} bieglosc-miecz 1`);
  try{
    await scene.prepare({target:false});
    await o.cmd(`minecraft:clear ${o.name} minecraft:stone_sword`);
    await assign(k,o,'odmrozenie','lewa');
    await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);
    const source=await until(()=>{const n=k.bot.inventory.slots.findIndex((x,i)=>i>=9&&i<=44&&x?.name==='stone_sword');return n<0?null:n;},'miecz MMOItems',5000);
    k.e();try{await k.bot.clickWindow(source,0,0);await k.bot.clickWindow(36,0,0);}finally{k.zamknij();}
    k.cyfra(1);await sleep(450);
    const selected=await o.selected();
    w.add('N_melee2-wyposazenie',/bron:s100c?\/stone_sword/.test(selected),`miecz w slocie 0: ${selected.slice(0,160)}`,{source,selected});
    await o.cmd('minecraft:summon minecraft:husk 0 80 2 {NoAI:1b,PersistenceRequired:1b,Tags:["NM2Target"]}');
    await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(350);
    const mob=await until(()=>Object.values(k.bot.entities).find(e=>e.name==='husk'&&e.position.distanceTo(k.bot.entity.position)<5),'husk N_melee2',2000);
    const target='@e[type=minecraft:husk,tag=NM2Target,limit=1]';
    const h0=await o.hp(target);
    k.bot.attack(mob);await sleep(1000);k.bot.attack(mob);await sleep(850);
    const h1=await o.hp(target);
    w.add('N_melee2-lpm',h1<h0,`LPM miecz + czar w lewej: ${h0}→${h1} HP`,{h0,h1,selected});
    const m0=await o.mana();await k.ppmTrzymaj(850);const m1=await o.mana(),h2=await o.hp(target);
    w.add('N_melee2-ppm',m1<m0&&h2<h1,`PPM odmrożenie: HP ${h1}→${h2}, mana ${m0}→${m1}`,{h1,h2,m0,m1});
    k.cyfra(6);await sleep(250);k.cyfra(1);await sleep(400);
    const after=await o.selected();
    w.add('N_melee2-powrot',/bron:s100c?\/stone_sword/.test(after),`miecz po zmianie slotu: ${after.slice(0,160)}`,{after});
  }finally{if(points<1)await o.cmd(`mmocore admin attribute take ${o.name} bieglosc-miecz 1`);}
};
