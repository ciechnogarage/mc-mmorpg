'use strict';
const {Klient,sleep}=require('../lib/klient');const {Obserwator}=require('../lib/obserwator');const {assign}=require('../lib/menu');
module.exports=async({k,o,scene,w,connect})=>{
  const actors=[{bot:k.bot,o,k,external:false}];const packets={total:0};
  const sample=async(label,casters=[])=>{
    const before=packets.total,start=Date.now(),mana0=[];
    for(const a of casters)mana0.push(await a.o.mana());
    const msptBefore=await o.cmd('mspt');
    if(casters.length){
      await Promise.all(casters.map(async a=>{
        a.k.ppmWcisnij();for(let t=0;t<50;t++){await sleep(200);a.k.ppmWcisnij();}a.k.ppmPusc();
      }));
    }else await sleep(10000);
    const seconds=(Date.now()-start)/1000,msptAfter=await o.cmd('mspt'),mana1=[];
    for(const a of casters)mana1.push(await a.o.mana());
    const spark=await o.cmd('spark profiler --stop --save-to-file');
    const active=casters.filter((_,i)=>mana0[i]!==null&&mana1[i]!==null&&mana1[i]<mana0[i]-0.1).length;
    const data={label,casters:casters.length,active,packetsPerSec:Number(((packets.total-before)/seconds).toFixed(1)),msptBefore,msptAfter,spark,mana0,mana1};
    w.add(`PERF-${label}`,casters.length===0||active===casters.length,`${label}: ${active}/${casters.length} botów pobiera manę, ${data.packetsPerSec} pakietów/s do sąsiada`,data);
    await w.checker.check(`PERF-${label}`);
    console.log('PERF-DATA '+JSON.stringify(data));
  };
  try{
    await scene.prepare({target:false});k.bot._client.on('packet',()=>packets.total++);
    await assign(k,o,'plomienie','lewa');await o.cmd(`mmocore admin resource-mana set ${o.name} 20`);
    for(let i=0;i<19;i++){
      const bot=await connect(`MagPerf${String(i+1).padStart(2,'0')}`),ob=new Obserwator(bot,o.rcon,bot.username),k=new Klient(bot,ob);
      actors.push({bot,o:ob,k,external:true});const x=-5+(i%5)*2,z=-5+Math.floor(i/5)*2;
      await o.cmd(`minecraft:tp ${bot.username} ${x} 80 ${z} 0 0`);
      await assign(k,ob,'plomienie','lewa');await o.cmd(`mmocore admin resource-mana set ${bot.username} 20`);
    }
    await o.cmd('spark profiler --stop --save-to-file');await sleep(500);
    await sample('bezczynność');await sample('10-botów',actors.slice(0,10));
    for(const a of actors.slice(0,10))await o.cmd(`mmocore admin resource-mana set ${a.bot.username} 20`);
    await sample('20-botów',actors);
  }finally{for(const a of actors.filter(x=>x.external)){a.k.close();a.bot.quit();}}
};
