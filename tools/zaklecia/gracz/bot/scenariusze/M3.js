'use strict';
const {Vec3}=require('vec3');const {sleep}=require('../lib/klient');
module.exports=async({k,o,scene,w})=>{
  const bot=k.bot;
  await scene.prepare({target:false});await o.cmd(`minecraft:item replace entity ${o.name} hotbar.6 with minecraft:stone_sword`);
  console.log('DIAG-OFFHAND '+JSON.stringify({b:(await o.cmd(`data get entity ${o.name} Inventory[{Slot:-106b}]`)),i:(await o.cmd(`data get entity ${o.name} Inventory[{Slot:-106}]`)),e:(await o.cmd(`data get entity ${o.name} equipment.offhand`)),client:bot.inventory.slots[45]?.name}));
  k.cyfra(7);await sleep(900);
  for(const [label,type,act] of [
    ['husk-zwykly','husk',e=>k.lpmKlik()],
    ['husk-machniecie-pierwsze','husk',e=>{bot.swingArm('right');bot._client.write('use_entity',{target:e.id,mouse:1,sneaking:false});}],
    ['krowa','cow',e=>k.lpmKlik()],
    ['krowa-sila','cow',async e=>{await o.cmd(`minecraft:effect give ${o.name} minecraft:strength 15 10 true`);k.lpmKlik();}],
    ['krowa-damage-by-player','cow',async e=>{await o.cmd(`minecraft:damage @e[type=minecraft:cow,tag=M3Target,limit=1] 2 minecraft:player_attack by ${o.name}`);}]
  ]){
    await o.cmd('minecraft:execute in minecraft:overworld positioned 0 80 0 run minecraft:kill @e[type=!minecraft:player,distance=..8]');
    await o.cmd(`minecraft:summon minecraft:${type} 0 80 2 {NoAI:1b,PersistenceRequired:1b,Tags:["M3Target"]}`);
    await sleep(700);await bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(500);
    const e=Object.values(bot.entities).filter(x=>x.name===type&&x.position?.distanceTo(new Vec3(0.5,80,2.5))<1).sort((a,b)=>b.id-a.id)[0];
    const selector=`@e[type=minecraft:${type},tag=M3Target,limit=1]`;
    const hp0=await o.hp(selector),uuid=await o.cmd(`data get entity ${selector} UUID`),attr=await o.cmd(`minecraft:attribute ${o.name} minecraft:attack_damage get`);
    await act(e);await sleep(850);const hp1=await o.hp(selector);
    w.add(`M3-${label}`,hp1<hp0,`${label} HP ${hp0}→${hp1}`,{hpDelta:hp0-hp1,entityId:e?.id,uuid:e?.uuid,serverUuid:uuid,attackDamage:attr});
  }
};
