'use strict';
const fs=require('node:fs');
const {Vec3}=require('vec3');
const {Klient,sleep}=require('../lib/klient');const {Obserwator}=require('../lib/obserwator');const {assign}=require('../lib/menu');
module.exports=async({k,o,scene,w,connect},mode='both')=>{
  o.maxEvents=500;o.captureItemPackets=false;
  const hostLoad=()=>fs.readFileSync('/proc/loadavg','utf8').trim();
  const actors=[{bot:k.bot,o,k,external:false}];const packets={total:0,types:new Map()};
  let spectator=null;
  const sample=async(label,casters=[],both=false)=>{
    for(const a of casters){
      for(let retry=0;retry<4&&!a.k.allow();retry++){a.k.zamknij();await sleep(300);}
      if(!a.k.allow())throw Error(`okno blokuje strumień: ${a.bot.username}`);
    }
    const loadStart=hostLoad();
    const before=packets.total,typesBefore=new Map(packets.types),mana0=[];
    const targetTypes=casters.map(a=>{try{const t=a.k.target();return `${t?.type||'brak'}:${t?.entity?.name||''}`;}catch{return 'blad';}});
    for(const a of casters)mana0.push(await a.o.mana());
    const clientReady=casters.map(a=>({name:a.bot.username,allow:a.k.allow(),using:a.k.using,window:!!a.bot.currentWindow,screen:a.k.screen,slot:a.bot.quickBarSlot,main:a.k.item()?.name||null,off:a.k.item('off')?.name||null}));
    const targetHp0=await o.hp('@e[type=minecraft:husk,tag=PERFTarget,limit=1]');
    const msptBefore=await o.cmd('mspt');
    const sparkStart=await o.cmd('spark profiler --start');
    const start=Date.now();
    if(casters.length){
      await Promise.all(casters.map(async a=>{
        if(both){await a.k.obie(10000);a.k.lpmKlik();}
        else {a.k.ppmWcisnij();for(let t=0;t<50;t++){await sleep(200);a.k.ppmWcisnij();}a.k.ppmPusc();}
      }));
    }else await sleep(10000);
    const seconds=(Date.now()-start)/1000,msptAfter=await o.cmd('mspt'),mana1=[];
    for(const a of casters)mana1.push(await a.o.mana());
    const targetHp1=await o.hp('@e[type=minecraft:husk,tag=PERFTarget,limit=1]');
    const spark=await o.cmd('spark profiler --stop --save-to-file');
    const active=casters.filter((_,i)=>mana0[i]!==null&&mana1[i]!==null&&mana1[i]<mana0[i]-0.1).length;
    const sustained=casters.filter((_,i)=>mana0[i]!==null&&mana1[i]!==null&&mana0[i]-mana1[i]>=(both?14:6)).length;
    const byType=Object.fromEntries([...packets.types].map(([name,n])=>[name,Number(((n-(typesBefore.get(name)||0))/seconds).toFixed(1))]).filter(([,n])=>n>0).sort((a,b)=>b[1]-a[1]));
    const data={label,casters:casters.length,streamsRequested:casters.length*(both?2:1),active,sustained,clientReady,targetTypes,targetHp0,targetHp1,packetsPerSec:Number(((packets.total-before)/seconds).toFixed(1)),byType,msptBefore,msptAfter,sparkStart,spark,mana0,mana1,loadStart,loadEnd:hostLoad()};
    w.add(`PERF2-${label}`,casters.length===0||sustained===casters.length,`${label}: ${sustained}/${casters.length} botów utrzymuje ${both?'dwa':'jeden'} strumień, ${data.packetsPerSec} pakietów/s do sąsiada`,data);
    await w.checker.check(`PERF2-${label}`);
    console.log('PERF2-DATA '+JSON.stringify(data));
  };
  try{
    await scene.prepare({target:false});
    // Standardowa scena stawia mur przy z=7; blokował promień pierwszego bota.
    await o.cmd('minecraft:fill 0 80 7 3 83 7 minecraft:air');
    await o.cmd('minecraft:summon minecraft:husk 0 80 5 {NoAI:1b,PersistenceRequired:1b,Tags:["PERFTarget"]}');
    await o.cmd('minecraft:effect give @e[type=minecraft:husk,tag=PERFTarget] minecraft:resistance 1000 255 true');
    spectator=await connect('MagSpectator');
    await o.cmd('minecraft:tp MagSpectator 0 80 10 180 0');
    spectator._client.on('packet',(_,meta)=>{packets.total++;const name=meta?.name||'unknown';packets.types.set(name,(packets.types.get(name)||0)+1);});
    await assign(k,o,'plomienie','lewa');await o.cmd(`mmocore admin resource-mana set ${o.name} 20`);
    for(let i=0;i<9;i++){
      const bot=await connect(`MagPX${String(i+1).padStart(2,'0')}`),ob=new Obserwator(bot,o.rcon,bot.username),k=new Klient(bot,ob);ob.maxEvents=300;ob.captureItemPackets=false;
      actors.push({bot,o:ob,k,external:true});
      await assign(k,ob,'plomienie','lewa');await o.cmd(`mmocore admin resource-mana set ${bot.username} 20`);
    }
    for(let i=0;i<actors.length;i++){
      const a=actors[i],theta=2*Math.PI*i/actors.length;
      const x=i===5?-2.5:Number((4*Math.cos(theta)).toFixed(2)),z=Number((5+4*Math.sin(theta)).toFixed(2));
      await o.cmd(`minecraft:tp ${a.bot.username} ${x} 80 ${z} 0 0`);
    }
    await o.cmd('minecraft:effect give @a minecraft:resistance 1000 255 true');
    await sleep(450);
    for(const a of actors){a.k.cyfra(1);await a.bot.lookAt(new Vec3(0.5,80.8,5.5),true);}
    for(const a of actors){
      let p=await a.o.placeholder('lewa');
      if(!/Płomienie|plomienie/i.test(p))p=await assign(a.k,a.o,'plomienie','lewa');
      if(!/Płomienie|plomienie/i.test(p))throw Error(`brak Płomieni w lewej: ${a.bot.username}: ${p}`);
      for(let retry=0;retry<3&&!a.k.item('off');retry++){
        await o.cmd(`zaklecia przypisz ${a.bot.username} nic lewa`);
        p=await assign(a.k,a.o,'plomienie','lewa');await sleep(350);
      }
      if(a.k.item('off')?.name!=='debug_stick')throw Error(`brak fizycznego nosnika w lewej: ${a.bot.username}: ${a.k.item('off')?.name||'pusto'}`);
      a.k.ppmPusc();if(!a.k.allow())a.k.zamknij();
    }
    await o.cmd('spark profiler --stop --save-to-file');await sleep(500);
    await sample('bezczynność');
    if(mode!=='20')await sample('10-strumieni',actors);
    if(mode!=='10'){
      for(const a of actors){
        let p=await assign(a.k,a.o,'plomienie','prawa');
        if(!/Płomienie|plomienie/i.test(p))p=await assign(a.k,a.o,'plomienie','prawa');
        if(!/Płomienie|plomienie/i.test(p))throw Error(`brak Płomieni w prawej: ${a.bot.username}: ${p}`);
        const left=await a.o.placeholder('lewa');
        if(!/Płomienie|plomienie/i.test(left))throw Error(`prawa wyparła lewą: ${a.bot.username}: ${left}`);
        a.k.ppmPusc();if(!a.k.allow())a.k.zamknij();
        await o.cmd(`mmocore admin resource-mana set ${a.bot.username} 20`);
      }
      await sleep(2000);
      await sample('20-strumieni',actors,true);
    }
  }finally{for(const a of actors.filter(x=>x.external)){a.k.close();a.bot.quit();}spectator?.quit();}
};
