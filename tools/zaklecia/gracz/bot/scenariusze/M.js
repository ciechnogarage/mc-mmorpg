'use strict';
const {Vec3}=require('vec3');const {sleep}=require('../lib/klient');
module.exports=async({k,o,scene,w})=>{
  const bot=k.bot,tag='@e[type=minecraft:husk,tag=MeleeProbe,limit=1]';
  await scene.prepare({target:false});
  await o.cmd(`minecraft:item replace entity ${o.name} hotbar.6 with minecraft:stone_sword`);
  k.cyfra(7);await sleep(700);
  console.log('DIAG-INVENTORY '+(await o.cmd(`data get entity ${o.name} Inventory`)).slice(0,1300));
  const sent=[];const write=bot._client.write.bind(bot._client);
  bot._client.write=(name,data)=>{if(name==='attack'||name==='use_entity'||name==='arm_animation')sent.push({name,data});return write(name,data);};
  for(const [label,z,act] of [
    ['klient-2',2,()=>k.lpmKlik()],
    ['klient-1',1,()=>k.lpmKlik()],
    ['mineflayer-1',1,entity=>bot.attack(entity)],
    ['pakiet-use_entity-1',1,entity=>{bot._client.write('use_entity',{target:entity.id,mouse:1,sneaking:false,location:{x:0,y:0,z:0}});bot.swingArm('right');}]
  ]){
    await o.cmd('minecraft:execute in minecraft:overworld positioned 0 80 0 run minecraft:kill @e[type=minecraft:husk,distance=..8]');
    await o.cmd(`minecraft:summon minecraft:husk 0 80 ${z} {NoAI:1b,PersistenceRequired:1b,Tags:["MeleeProbe"]}`);
    await sleep(550);await bot.lookAt(new Vec3(0.5,80.8,z+0.5),true);await sleep(250);
    const entity=Object.values(bot.entities).find(e=>e.name==='husk'&&e.position.distanceTo(new Vec3(0.5,80,z+0.5))<0.9);
    const hp0=await o.hp(tag),t=k.target(),s=sent.length,serverUuid=await o.cmd(`data get entity ${tag} UUID`),serverPos=await o.cmd(`data get entity ${o.name} Pos`),serverRot=await o.cmd(`data get entity ${o.name} Rotation`);
    try{await act(entity);}catch(e){w.blad(`M-${label}-pakiet`,e);continue;}
    await sleep(900);const hp1=await o.hp(tag);
    w.add(`M-${label}`,hp1<hp0,`melee ${label}; HP ${hp0}→${hp1}; cel ${t.type} d=${t.distance}`,
      {hpDelta:hp0-hp1,packets:sent.slice(s),yaw:bot.entity.yaw,pitch:bot.entity.pitch,entityId:entity?.id,entityUuid:entity?.uuid,serverUuid,serverPos,serverRot,selected:(await o.selected()).slice(0,90)});
  }
  const hp0=await o.hp(tag);await o.cmd(`minecraft:damage ${tag} 2`);const hp1=await o.hp(tag);
  w.add('M-kontrola-pomiaru',hp1<hp0,`kontrola pomiaru HP ${hp0}→${hp1}`,{hpDelta:hp0-hp1});
};
