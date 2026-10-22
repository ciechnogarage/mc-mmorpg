'use strict';
const {Vec3}=require('vec3');const {sleep,Klient}=require('../lib/klient');const {Obserwator}=require('../lib/obserwator');const {Scena}=require('../lib/scena');
module.exports=async({k,o,scene,w,connect})=>{
  let fresh;
  try{
    fresh=await connect('MagMeleeFresh');const obs=new Obserwator(fresh,o.rcon,fresh.username),client=new Klient(fresh,obs),arena=new Scena(obs);
    await arena.prepare({target:false});await obs.cmd(`minecraft:item replace entity ${obs.name} hotbar.6 with minecraft:stone_sword`);
    client.cyfra(7);await sleep(800);
    await obs.cmd('minecraft:summon minecraft:cow 0 80 2 {NoAI:1b,PersistenceRequired:1b,Tags:["M4Target"]}');await sleep(600);
    await fresh.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(500);
    const target=Object.values(fresh.entities).filter(x=>x.name==='cow').sort((a,b)=>b.id-a.id)[0],tag='@e[type=minecraft:cow,tag=M4Target,limit=1]';
    const hp0=await obs.hp(tag),uuid=await obs.cmd(`data get entity ${tag} UUID`);client.lpmKlik();await sleep(900);const hp1=await obs.hp(tag);
    w.add('M4-nowe-konto-miecz',hp1<hp0,`nowe konto, miecz, HP ${hp0}→${hp1}`,{hpDelta:hp0-hp1,entityId:target?.id,uuid:target?.uuid,serverUuid:uuid,selected:await obs.selected(),mode:client.gamemode()});
    const by=await obs.cmd(`minecraft:damage ${tag} 2 minecraft:player_attack by ${obs.name}`);const hp2=await obs.hp(tag);
    w.add('M4-damage-by-player',hp2<hp1,`damage by player: ${by}; HP ${hp1}→${hp2}`,{hpDelta:hp1-hp2});
    const generic=await obs.cmd(`minecraft:damage ${tag} 2`),hp3=await obs.hp(tag);
    w.add('M4-kontrola-hp',hp3<hp2,`damage generic: ${generic}; HP ${hp2}→${hp3}`,{hpDelta:hp2-hp3});
    await arena.cleanup();client.close();
  }finally{fresh?.quit();}
};
