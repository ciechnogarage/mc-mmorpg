'use strict';
const fs=require('node:fs');const {Vec3}=require('vec3');const {sleep}=require('./klient');const {assign}=require('./menu');
const SPELLS=Object.entries(require('../scenariusze/B').TYPES);
const CHARGE={ognisty_pocisk:8,kula_ognia:14,runa_ognia:16,przywolany_miecz:12,duch_wilka:12,atronach_ognia:12,uspokojenie:12,uzdrowienie_innego:12,zasklepienie_ran:14,wielkie_uzdrowienie:24};
function lore(id){
  const raw=fs.readFileSync(`/lore-items/${id}.yml`,'utf8');
  const lines=raw.split('\n').filter(x=>/^\s*-\s*'/.test(x)).map(x=>x.replace(/<[^>]*>/g,'').replace(/^\s*-\s*'/,'').replace(/'\s*$/,'').trim());
  const value=re=>{const m=lines.join(' ').match(re);return m?Number(m[1].replace(',','.')):null;};
  return {lines,cost:value(/Koszt:\s*(\d+(?:[.,]\d+)?)\s*many/i),damage:value(/Obrażenia:\s*(\d+(?:[.,]\d+)?)/i),range:value(/Zasięg:\s*(\d+(?:[.,]\d+)?)/i),heal:value(/(?:Leczy|leczy).*?(\d+(?:[.,]\d+)?)\s*HP/i)};
}
async function cast(k,gesture,type,ms){
  const continuous=type==='koncentracja'||type==='tarcza';let releasedAt;
  if(gesture==='lewa'){await k.ppmTrzymaj(ms);releasedAt=Date.now();}
  if(gesture==='prawa'){k.lpmKlik();await sleep(ms);if(continuous)k.lpmKlik();releasedAt=Date.now();}
  if(gesture==='obie'){await k.obie(ms);if(continuous)k.lpmKlik();releasedAt=Date.now();}
  if(gesture==='podwojne'){k.lpmKlik();await sleep(210);k.lpmKlik();releasedAt=Date.now();}
  return releasedAt;
}
async function runMatrix(part,{k,o,scene,w,connect},selectedIds,options={}){
  let target;const ids=selectedIds?SPELLS.filter(([id])=>selectedIds.includes(id)):SPELLS.slice(part*5,part*5+5);
  const gestures=options.gestures||['prawa','lewa','obie','podwojne'],targets=options.targets||['husk','sojusznik','pvp','powietrze','sciana'],repetitions=options.reps||5;
  try{
    target=await connect('MagCel');
    for(const [id,type] of ids){
      await scene.prepare({target:false});await o.cmd('minecraft:tp MagCel 12 80 12');
      await assign(k,o,id,'prawa');await assign(k,o,id,'lewa');
      const l=lore(id);w.add(`MX-lore-${id}`,true,`lore ${id}: koszt=${l.cost}, obrażenia=${l.damage}, leczenie=${l.heal}, zasięg=${l.range}`,l);
      let positive=false;
      for(const gesture of gestures)for(const targetType of targets)for(let rep=1;rep<=repetitions;rep++){
        const rowId=`MX-${id}-${gesture}-${targetType}-${rep}`;
        await w.step(rowId,async()=>{
          await o.cmd('minecraft:execute in minecraft:overworld positioned 0 80 0 run minecraft:kill @e[type=!minecraft:player,distance=..18]');
          await o.cmd('minecraft:execute in minecraft:overworld positioned 0 80 0 run minecraft:kill @e[type=!minecraft:player,distance=..18]');
          await o.cmd(`mmocore admin resource-mana set ${o.name} 20`);
          await o.cmd(`minecraft:effect clear ${o.name}`);
          const targetZ=id==='uspokojenie'?2.5:5;
          if(targetType==='husk'){
            await o.cmd('minecraft:tp MagCel 12 80 12');
            await o.cmd(`minecraft:summon minecraft:husk 0 80 ${targetZ} {NoAI:1b,PersistenceRequired:1b,Tags:["MXTarget"]}`);
          }else if(targetType==='sojusznik'||targetType==='pvp'){
            await o.cmd(`rg flag -w world __global__ pvp ${targetType==='pvp'?'allow':'deny'}`);
            await o.cmd(`minecraft:tp MagCel 0 80 ${targetZ} 180 0`);
            await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');
          }else await o.cmd('minecraft:tp MagCel 12 80 12');
          if(id==='leczenie'||id==='zasklepienie_ran'||id==='wielkie_uzdrowienie'){
            await o.cmd(`minecraft:effect give ${o.name} minecraft:instant_health 1 10 true`);
            await sleep(600);
            let healthBeforeDamage=await o.hp(o.name);
            for(let heals=0;healthBeforeDamage<12&&heals<4;heals++){
              await o.cmd(`minecraft:effect give ${o.name} minecraft:instant_health 1 10 true`);
              await sleep(180);healthBeforeDamage=await o.hp(o.name);
            }
            if(healthBeforeDamage<12)throw Error(`za malo HP ${o.name} przed przygotowaniem ${id}`);
            let hurt=false;
            for(let tries=0;tries<3&&!hurt;tries++){
              await o.cmd(`minecraft:damage ${o.name} 6`);
              const healthAfterDamage=await o.hp(o.name);
              hurt=healthAfterDamage>0&&healthAfterDamage<=healthBeforeDamage-4;
              if(!hurt)await sleep(550);
            }
            if(!hurt)throw Error(`nie udalo sie obnizyc HP ${o.name} przed ${id}`);
          }
          if(['leczenie_dloni','uzdrowienie_innego'].includes(id)&&['sojusznik','pvp'].includes(targetType)){
            await sleep(600);
            let healthBeforeDamage=await o.hp('MagCel');
            for(let heals=0;healthBeforeDamage<12&&heals<4;heals++){
              await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');
              await sleep(180);healthBeforeDamage=await o.hp('MagCel');
            }
            if(healthBeforeDamage<12)throw Error(`za malo HP MagCel przed przygotowaniem ${id}`);
            let hurt=false;
            for(let tries=0;tries<3&&!hurt;tries++){
              await o.cmd('minecraft:damage MagCel 6');
              const healthAfterDamage=await o.hp('MagCel');
              hurt=healthAfterDamage>0&&healthAfterDamage<=healthBeforeDamage-4;
              if(!hurt)await sleep(550);
            }
            if(!hurt)throw Error(`nie udalo sie obnizyc HP MagCel przed ${id}`);
          }
          if(targetType==='powietrze')await k.bot.look(0,1.3,true);
          else if(targetType==='sciana')await k.bot.lookAt(new Vec3(0.5,81,7),true);
          else if(id==='runa_ognia')await k.bot.lookAt(new Vec3(0.5,79.8,3.5),true);
          else if(id==='duch_wilka'||id==='atronach_ognia')await k.bot.lookAt(new Vec3(-1,81,5),true);
          else await k.bot.lookAt(new Vec3(0.5,80.8,targetZ+0.5),true);
          await sleep(120);
          const hpTarget=targetType==='husk'?'@e[type=minecraft:husk,tag=MXTarget,limit=1]':['sojusznik','pvp'].includes(targetType)?'MagCel':o.name;
          const m0=await o.mana(),hp0=await o.hp(hpTarget),start=o.snapshot(),duration=type==='koncentracja'||type==='tarcza'?850:(CHARGE[id]||12)*50+750;
          const released=await cast(k,gesture,type,duration),manaAtRelease=await o.mana();
          await sleep(type==='koncentracja'||type==='tarcza'?300:900);
          const m1=await o.mana(),hp1=await o.hp(hpTarget),met=o.metrics(start);
          const summonType=id==='duch_wilka'?'wolf':id==='atronach_ognia'?'blaze':null;
          const summonReply=summonType?await o.cmd(`minecraft:execute positioned 0 80 0 if entity @e[type=minecraft:${summonType},distance=..12]`):'';
          const summoned=summonType?/Test passed|Found [1-9]|Count: [1-9]/i.test(summonReply):null;
          const stop=o.events.find(e=>e.at>=released&&e.packet==='entity_destroy');
          const delta=m0!==null&&m1!==null?Number((m0-Math.min(manaAtRelease??m1,m1)).toFixed(2)):null,hpDelta=hp0!==null&&hp1!==null?Number((hp0-hp1).toFixed(3)):null;
          if(delta!==null&&delta>0.1||summoned)positive=true;
          w.add(rowId,m0!==null&&m1!==null&&m1>=0&&m1<=m0+0.3,
            `${id} ${gesture} ${targetType} #${rep}; mana ${m0}→${m1}, HP ${hp0}→${hp1}`,
            {...met,cost:delta,manaAtRelease,manaAfter:m1,summoned,hpDelta,damagePerSec:hpDelta!==null?Number((hpDelta/(duration/1000)).toFixed(2)):null,startMs:met.latencyTicks===null?null:met.latencyTicks*50,stopMs:stop?stop.at-released:null,durationMs:duration,loreCost:l.cost,loreDamage:l.damage,loreRange:l.range});
          if(id==='przywolany_miecz'){await assign(k,o,id,'prawa');await assign(k,o,id,'lewa');}
        });
      }
      if(options.targets)w.skip(`MX-kontrola-${id}`,'uzupełnienie wybranych celów');
      else w.add(`MX-kontrola-${id}`,positive,`co najmniej jedna próba ${id} pobiera manę lub tworzy przywołańca`);
    }
  }finally{try{await o.cmd('rg flag -w world __global__ pvp');}catch{}target?.quit();}
}
module.exports={runMatrix,lore};
