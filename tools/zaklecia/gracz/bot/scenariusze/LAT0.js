'use strict';
const {Vec3}=require('vec3');
const {sleep}=require('../lib/klient');
const {assign}=require('../lib/menu');

// Sonda formatu pakietów: późniejszy pomiar LAT używa wyłącznie potwierdzonych pól.
module.exports=async({k,o,scene,w})=>{
  await scene.prepare({target:false});
  await o.cmd('minecraft:summon minecraft:husk 0 80 5 {NoAI:1b,PersistenceRequired:1b,Tags:["LAT0Target"]}');
  await assign(k,o,'plomienie','lewa');
  await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);
  await sleep(200);
  const names=['damage_event','entity_status','entity_effect','update_health','entity_metadata','spawn_entity','entity_destroy','set_slot','boss_bar','experience','update_attributes','scoreboard_score'];
  const events=[];
  const words=(x,out=[])=>{if(x&&typeof x==='object'){
    if(x.text?.type==='string')out.push(x.text.value);
    for(const [key,v] of Object.entries(x))if(key!=='text')words(v,out);
  }return out;};
  for(const name of names)k.bot._client.on(name,p=>{
    if(events.length<120)events.push({t:Date.now(),name,text:name==='boss_bar'?words(p.title).join('').slice(0,4000):'',data:JSON.stringify(p,(_,v)=>typeof v==='bigint'?String(v):v).slice(0,1200)});
  });
  const input=Date.now();
  await k.ppmTrzymaj(850);const release=Date.now();await sleep(450);
  const hp=await o.hp('@e[type=minecraft:husk,tag=LAT0Target,limit=1]');
  console.log('LAT0-PACKETS '+JSON.stringify({input,release,hp,events}));
  w.add('LAT0-pakiety',events.length>0&&hp<20,`zdarzeń ${events.length}, husk ${hp} HP`,{input,release,hp,events});
};
