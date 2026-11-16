'use strict';
const {Vec3}=require('vec3');
const fs=require('node:fs');
const {sleep}=require('./klient');
const {assign}=require('./menu');
const SPELLS=Object.entries(require('../scenariusze/B').TYPES);
const CHARGE={ognisty_pocisk:8,kula_ognia:14,runa_ognia:16,przywolany_miecz:12,duch_wilka:12,atronach_ognia:12,uspokojenie:12,uzdrowienie_innego:12,zasklepienie_ran:14,wielkie_uzdrowienie:24};
const self=new Set(['leczenie','zasklepienie_ran','wielkie_uzdrowienie']);
const ally=new Set(['leczenie_dloni','uzdrowienie_innego']);
function percentile(values,p){if(!values.length)return null;const a=[...values].sort((x,y)=>x-y);return a[Math.ceil(p*a.length)-1];}
function ticks(ms){return ms===null?null:Math.round(ms/50);}
async function runLatency(part,{k,o,scene,w,connect},onlyIds=null){
  const ids=SPELLS.slice(part*5,part*5+5).filter(([id])=>!onlyIds||onlyIds.includes(id)),other=await connect('MagCel'),summary=[];
  w.checker.fastInventory=true;
  o.maxEvents=500;o.captureItemPackets=false;
  const hostLoad=()=>fs.readFileSync('/proc/loadavg','utf8').trim();
  const loadStart=hostLoad();
  try{
    for(const [id,type] of ids){
      await scene.prepare({target:false});
      await assign(k,o,id,'prawa');await assign(k,o,id,'lewa');
      for(const gesture of ['prawa','lewa','obie']){
        const starts=[],stops=[],endAfter=[];
        for(let rep=1;rep<=10;rep++)await w.step(`LAT-${id}-${gesture}-${rep}`,async()=>{
          await scene.clearArena();
          await o.cmd(`mmocore admin resource-mana set ${o.name} 20`);
          await o.cmd('minecraft:tp MagCel 12 80 12');
          if(id==='przywolany_miecz'){
            await assign(k,o,id,'prawa');await assign(k,o,id,'lewa');
          }
          const target=ally.has(id)?'MagCel':self.has(id)?o.name:'@e[type=minecraft:husk,tag=LATTarget,limit=1]';
          if(ally.has(id)){
            await o.cmd('minecraft:tp MagCel 0 80 5 180 0');
            await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');
            await sleep(650);await o.cmd('minecraft:damage MagCel 6');
          }else if(self.has(id)){
            await o.cmd(`minecraft:effect give ${o.name} minecraft:instant_health 1 10 true`);
            await sleep(650);await o.cmd(`minecraft:damage ${o.name} 6`);
          }else if(!['oslona','przywolany_miecz','duch_wilka','atronach_ognia'].includes(id))
            await o.cmd(`minecraft:summon minecraft:husk 0 80 ${id==='uspokojenie'?2:5} {NoAI:1b,PersistenceRequired:1b,Tags:["LATTarget"]}`);
          if(id==='runa_ognia')await k.bot.lookAt(new Vec3(0.5,79.8,3.5),true);
          else if(['duch_wilka','atronach_ognia'].includes(id))await k.bot.lookAt(new Vec3(-1,81,5),true);
          else if(self.has(id)||['oslona','przywolany_miecz'].includes(id))await k.bot.look(0,1.3,true);
          else await k.bot.lookAt(new Vec3(0.5,80.8,id==='uspokojenie'?2.5:5.5),true);
          await sleep(140);
          const m0=await o.mana(),h0=await o.hp(target),events=[];
          const targetId=ally.has(id)?k.bot.players?.MagCel?.entity?.id:self.has(id)?k.bot.entity?.id:
            Object.values(k.bot.entities).find(e=>e.name==='husk'&&e.position?.distanceTo(k.bot.entity.position)<8)?.id;
          const names=['damage_event','entity_status','spawn_entity','entity_metadata','entity_destroy','set_slot','boss_bar','update_health','experience','entity_effect','remove_entity_effect','update_attributes','world_particles','sound_effect','named_sound_effect'];
          const listeners=names.map(name=>{const fn=p=>events.push({name,at:Date.now(),p,source:'caster'});k.bot._client.on(name,fn);return [name,fn];});
          const allyHealth=p=>events.push({name:'update_health',at:Date.now(),p,source:'ally'});
          if(ally.has(id))other._client.on('update_health',allyHealth);
          const oldWrite=k.bot._client.write;let input=null,release=null;
          k.bot._client.write=function(name,...args){if(input===null&&['use_item','use_entity','arm_animation'].includes(name))input=Date.now();
            return oldWrite.call(this,name,...args);};
          const duration=type==='koncentracja'||type==='tarcza'?850:(CHARGE[id]||12)*50+750;
          try{
            if(gesture==='prawa'){
              k.lpmKlik();await sleep(duration);
              if(type==='koncentracja'||type==='tarcza')k.lpmKlik();release=Date.now();
            }else if(gesture==='lewa'){
              await k.ppmTrzymaj(duration);release=Date.now();
            }else{
              await k.obie(duration);
              if(type==='koncentracja'||type==='tarcza')k.lpmKlik();release=Date.now();
            }
            await sleep(type==='ladowany'?900:550);
          }finally{
            k.bot._client.write=oldWrite;
            for(const [name,fn] of listeners)k.bot._client.removeListener(name,fn);
            if(ally.has(id))other._client.removeListener('update_health',allyHealth);
          }
          const m1=await o.mana(),h1=await o.hp(target);
          const spawns=new Map(events.filter(e=>e.name==='spawn_entity').map(e=>[e.p.entityId,e.at]));
          const streams=new Set(events.filter(e=>e.name==='entity_metadata'&&JSON.stringify(e.p.metadata||[]).includes(`zaklecia:strumien_${id}`)).map(e=>e.p.entityId));
          const destroys=new Map();
          for(const e of events.filter(e=>e.name==='entity_destroy'))for(const entityId of e.p.entityIds||[])destroys.set(entityId,e.at);
          // N7 tworzy także pełny strumień ukrywany natychmiast przed rzucającym.
          // Widoczny strumień musi przeżyć do puszczenia przycisku.
          const visibleStreams=[...streams].filter(entityId=>spawns.has(entityId)&&
            (destroys.get(entityId)===undefined||destroys.get(entityId)>=release-100));
          const streamStarts=visibleStreams.map(entityId=>spawns.get(entityId)).filter(Boolean);
          const streamEnds=visibleStreams.map(entityId=>destroys.get(entityId)).filter(Boolean);
          // Entity cache can still contain the husk removed in the previous repeat.
          // The arena contains one fresh target, so use the received damage marker.
          const hurts=events.filter(e=>e.name==='damage_event').map(e=>e.at);
          const statuses=events.filter(e=>e.name==='entity_status'&&e.p.entityStatus===2).map(e=>e.at);
          const health=events.filter(e=>e.name==='update_health'&&e.source===(ally.has(id)?'ally':'caster')&&Number.isFinite(e.p.health));
          let previous=h0;const heals=[];
          if(self.has(id)||ally.has(id))for(const e of health){if(Number.isFinite(previous)&&e.p.health>previous+0.02)heals.push(e.at);previous=e.p.health;}
          const summoned=id==='duch_wilka'?'wolf':id==='atronach_ognia'?'blaze':id==='runa_ognia'?'armor_stand':null;
          const summoning= summoned?[...spawns].filter(([entityId])=>k.bot.entities[entityId]?.name===summoned).map(([,at])=>at):[];
          const conjured=id==='przywolany_miecz'?events.filter(e=>e.name==='set_slot'
            &&JSON.stringify(e.p.item||{}).includes('zaklecia:przywolany_miecz')).map(e=>e.at):[];
          const ownState= id==='oslona'?events.filter(e=>['entity_effect','remove_entity_effect','update_attributes'].includes(e.name)
            &&e.p.entityId===k.bot.entity.id):[];
          const shieldStart=ownState.filter(e=>e.name!=='remove_entity_effect'&&e.at<release).map(e=>e.at);
          const shieldStop=ownState.filter(e=>e.at>=release).map(e=>e.at);
          const startAt=[...streamStarts,...hurts,...statuses,...heals,...summoning,...conjured,...shieldStart].filter(Boolean).sort((a,b)=>a-b)[0]||null;
          const stopAt=streamEnds.length?Math.max(...streamEnds):hurts.length?Math.max(...hurts):heals.length?Math.max(...heals):shieldStop.length?Math.max(...shieldStop):summoning.length?Math.max(...summoning):null;
          const start=input!==null&&startAt!==null?ticks(startAt-input):null;
          const stop=input!==null&&stopAt!==null?ticks(stopAt-input):null;
          const after=release!==null&&stopAt!==null?ticks(stopAt-release):null;
          if(start!==null&&start>=0)starts.push(start);
          if(stop!==null&&stop>=0)stops.push(stop);
          if(after!==null)endAfter.push(after);
          const data={id,gesture,rep,targetId,input,release,startTicks:start,stopTicks:stop,stopAfterReleaseTicks:after,
            mana0:m0,mana1:m1,hp0:h0,hp1:h1,streamIds:[...streams],visibleStreamIds:visibleStreams,hurtPackets:hurts.length,statusPackets:statuses.length,
            healPackets:heals.length,summonPackets:summoning.length,conjuredItemPackets:conjured.length,shieldStatePackets:ownState.length,
            streamSpawnPackets:streamStarts.length,streamDestroyPackets:streamEnds.length,packetTypes:Object.fromEntries(names.map(n=>[n,events.filter(e=>e.name===n).length]))};
          w.add(`LAT-${id}-${gesture}-${rep}`,input!==null&&m0!==null&&m1!==null,
            `${id} ${gesture} #${rep}: start ${start}, stop ${stop} t; mana ${m0}→${m1}, HP ${h0}→${h1}`,data);
        });
        const data={id,gesture,attempts:10,startN:starts.length,stopN:stops.length,startMedian:percentile(starts,.5),startP95:percentile(starts,.95),
          stopMedian:percentile(stops,.5),stopP95:percentile(stops,.95),afterReleaseMedian:percentile(endAfter,.5),afterReleaseP95:percentile(endAfter,.95)};
        summary.push(data);
        w.add(`LAT-${id}-${gesture}-pokrycie`,starts.length===10&&stops.length===10,
          `${id} ${gesture}: start n=${starts.length}/10, stop n=${stops.length}/10`,data);
      }
    }
  }finally{other.quit();}
  console.log('LAT-SUMMARY '+JSON.stringify({loadStart,loadEnd:hostLoad(),summary}));
}
module.exports={runLatency,percentile,ticks};
