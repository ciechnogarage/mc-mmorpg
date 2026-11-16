'use strict';
const {Vec3}=require('vec3');
const {sleep}=require('../lib/klient');
module.exports=async({k,o,scene,w})=>{
  for(const kind of ['miecz','luk'])await w.step(`LATC-${kind}`,async()=>{
    await scene.prepare({target:false});
    await o.cmd(`minecraft:item replace entity ${o.name} hotbar.0 with minecraft:${kind==='miecz'?'stone_sword':'bow'}`);
    if(kind==='luk')await o.cmd(`minecraft:give ${o.name} minecraft:arrow 64`);
    await o.cmd(`minecraft:summon minecraft:husk 0 80 ${kind==='miecz'?2:5} {NoAI:1b,PersistenceRequired:1b,Tags:["LATCTarget"]}`);
    k.cyfra(1);await k.bot.lookAt(new Vec3(0.5,80.8,kind==='miecz'?2.5:5.5),true);await sleep(700);
    const target='@e[type=minecraft:husk,tag=LATCTarget,limit=1]',hp0=await o.hp(target);
    const events=[];let input=null,release=null;
    const orig=k.bot._client.write;
    k.bot._client.write=function(name,...args){if(['arm_animation','use_entity','use_item'].includes(name)&&input===null)input=Date.now();
      if(name==='block_dig'&&args[0]?.status===5)release=Date.now();return orig.call(this,name,...args);};
    const onDamage=p=>events.push({at:Date.now(),packet:'damage_event',data:p});
    const onSpawn=p=>events.push({at:Date.now(),packet:'spawn_entity',data:p});
    k.bot._client.on('damage_event',onDamage);k.bot._client.on('spawn_entity',onSpawn);
    try{if(kind==='miecz')k.lpmKlik();else{k.ppmWcisnij();await sleep(1000);k.ppmPusc();}await sleep(kind==='miecz'?450:1100);}
    finally{k.bot._client.write=orig;k.bot._client.removeListener('damage_event',onDamage);k.bot._client.removeListener('spawn_entity',onSpawn);}
    const hp1=await o.hp(target);
    const data={kind,input,release,hp0,hp1,events:events.map(e=>({t:e.at-input,packet:e.packet,entityId:e.data.entityId,type:e.data.type}))};
    w.add(`LATC-${kind}`,hp1<hp0&&events.some(e=>e.packet==='damage_event'),
      `${kind}: husk ${hp0}→${hp1}, pakiet obrażeń ${events.filter(e=>e.packet==='damage_event').length}`,data);
  });
};
