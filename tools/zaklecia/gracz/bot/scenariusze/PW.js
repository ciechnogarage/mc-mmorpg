'use strict';
const {Vec3}=require('vec3');const {sleep}=require('../lib/klient');const {assign,until}=require('../lib/menu');
module.exports=async({k,o,scene,w,connect})=>{
  let peer;
  try{
    peer=await connect('MagCel');await scene.prepare({target:false});await scene.targetBot();await o.cmd('minecraft:tp MagCel 0 80 2');
    await o.cmd('minecraft:effect clear MagCel');
    const flag=await o.cmd('rg flag -w world __global__ pvp allow');
    await o.cmd(`mmocore admin attribute give ${o.name} bieglosc-miecz 1`);
    await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);
    const sword=await until(()=>{const n=k.bot.inventory.slots.findIndex((x,i)=>i>=9&&i<=44&&x?.name==='stone_sword');return n<0?null:n},'miecz PvP',2500);
    const swordHotbar=sword<36?7:sword-35;
    if(sword<36){k.e();await k.bot.clickWindow(sword,0,0);await k.bot.clickWindow(42,0,0);k.zamknij();}
    k.cyfra(swordHotbar);
    const target=await until(()=>Object.values(k.bot.entities).find(e=>e.username==='MagCel'),'MagCel',2000);
    const hit=async id=>{await k.bot.lookAt(target.position.offset(0,1,0),true);await sleep(1100);await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');await sleep(350);const before=await o.hp('MagCel'),item=await o.selected(),ray=k.target();k.bot.attack(target);await sleep(650);const after=await o.hp('MagCel');w.add(id,after<before,`${id}: MagCel ${before}→${after}; ${flag}`,{damage:before-after,selected:item.slice(0,130),targetId:target.id,ray:ray.type,distance:ray.distance});await w.checker.check(id);};
    await hit('PW-bez-wilka');
    await assign(k,o,'duch_wilka','prawa');await k.bot.lookAt(new Vec3(-1,81,5),true);k.lpmKlik();
    await until(async()=>/Count: [1-9]/.test(await o.cmd('minecraft:execute positioned 0 80 0 if entity @e[type=minecraft:wolf,distance=..12]')),'wilk PW',3000);
    k.cyfra(swordHotbar);await hit('PW-z-wilkiem');
  }finally{try{await o.cmd('rg flag -w world __global__ pvp deny');}catch{}peer?.quit();}
};
