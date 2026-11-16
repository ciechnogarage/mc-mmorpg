'use strict';
const {Vec3}=require('vec3');
const {sleep}=require('../lib/klient');
const {assign}=require('../lib/menu');
const soundsById=require('minecraft-data/minecraft-data/data/pc/1.21.11/sounds.json');

module.exports=async({k,o,scene,w})=>{
  await scene.prepare({target:false});
  const own=new Set(),rows=[];
  const word=x=>{try{return JSON.stringify(x||'');}catch{return String(x||'');}};
  const words=(x,out=[])=>{if(typeof x==='string'){out.push(x);return out;}
    if(x&&typeof x==='object'){
      if(x.text?.type==='string')out.push(x.text.value);
      for(const key of ['content','extra','value'])if(x[key]&&typeof x[key]==='object')words(x[key],out);
      if(Array.isArray(x))for(const v of x)words(v,out);
    }return out;};
  const soundName=e=>{
    const s=e.data.soundName??e.data.soundId??e.data.sound??e.data.name;
    if(typeof s==='number')return soundsById[s]?.name||`id:${s}`;
    if(typeof s==='string')return s;
    if(typeof s?.soundId==='number')return soundsById[s.soundId]?.name||`id:${s.soundId}`;
    return s?.data?.soundName||word(s);
  };
  const measure=async(label,fn)=>{
    const snap=o.snapshot();await fn();await sleep(300);
    const events=o.events.slice(snap.index),bars=events.filter(e=>e.packet==='boss_bar');
    if(label==='otworz-pasek')for(const e of bars)
      if(/✦|many\/s|zakl[eę]ci/i.test(words(e.data.title).join('')))own.add(e.data.entityUUID);
    const barOwn=bars.filter(e=>own.has(e.data.entityUUID));
    const sound=events.filter(e=>e.packet==='sound_effect'||e.packet==='named_sound_effect').map(soundName);
    const rawMessages=events.filter(e=>['chat','system_chat','player_chat','action_bar'].includes(e.packet))
      .map(e=>words(e.data).join('').trim()).filter(Boolean);
    const messages=[...new Set(rawMessages)];
    const data={label,bossBar:barOwn.length,bossUpdates:barOwn.length,
      barActions:barOwn.map(e=>({id:e.data.entityUUID,action:e.data.action,title:word(e.data.title).slice(0,120)})),
      sounds:sound,messages,otherBar:bars.length-barOwn.length};
    rows.push(data);
    w.add(`NOISE-${label}`,sound.length<=1&&messages.length<=1&&data.bossBar<=1,
      `${label}: ${sound.length} dźwięków, ${messages.length} komunikatów, ${data.bossUpdates} aktualizacji paska Zaklęć`,data);
  };
  await measure('otworz-pasek',()=>k.shiftQ());
  await measure('kolko-paska',async()=>{k.kolko(1);});
  await measure('cyfra-paska',async()=>{k.cyfra(3);});
  await measure('f-paska',async()=>{k.f();});
  await measure('zamknij-pasek',async()=>{k.q();});
  await measure('przypisz-plomienie',()=>assign(k,o,'plomienie','lewa'));
  await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);
  await measure('ppm-strumien',()=>k.ppmTrzymaj(850));
  await measure('przypisz-prawa',()=>assign(k,o,'plomienie','prawa'));
  await measure('oburacz',()=>k.obie(850));
  await measure('zmien-slot',async()=>{k.cyfra(2);});
  console.log('NOISE-SUMMARY '+JSON.stringify({own:[...own],rows}));
};
