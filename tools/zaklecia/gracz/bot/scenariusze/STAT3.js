'use strict';
const {Vec3}=require('vec3');
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  const before=await o.cmd(`mmocore admin attribute check ${o.name} wiara`);
  const stats=async()=>({skill:await o.cmd(`papi parse ${o.name} %mmocore_stat_skill_damage%`),magic:await o.cmd(`papi parse ${o.name} %mmocore_stat_magic_damage%`),wiara:await o.cmd(`papi parse ${o.name} %mmocore_attribute_wiara%`)});
  const measurements=[];
  const baselineStats=await stats();
  const initialRaw=Number.parseFloat(String(baselineStats.wiara).replace(',','.'));
  const initial=Number.isFinite(initialRaw)?initialRaw:Number(before.match(/(\d+)\s+(?:points|punkt)/i)?.[1]);
  const grant=Number.isFinite(initial)?Math.max(0,40-initial):0;
  let granted=false;
  try{
    for(const points of [0,40]){
      const command=points===0?'stan początkowy':grant?await o.cmd(`mmocore admin attribute give ${o.name} wiara ${grant}`):'brak zmiany: nieznane punkty początkowe';
      if(points&&grant)granted=true;
      const stat=await stats();
      w.add(`STAT3-atrybut-${points}`,points===0||grant>0&&Number.parseFloat(String(stat.skill).replace(',','.'))>Number.parseFloat(String(baselineStats.skill).replace(',','.')),
        `odpowiedź: ${command}; stat ${JSON.stringify(stat)}`,{before,command,stat,initial,grant});
      for(const id of ['plomienie','odmrozenie'])for(let rep=1;rep<=5;rep++)await w.step(`STAT3-${id}-${points}-${rep}`,async()=>{
        await scene.prepare({target:false});
        await o.cmd(`mmocore admin resource-mana set ${o.name} 20`);
        await assign(k,o,id,'lewa');
        await o.cmd(`minecraft:item replace entity ${o.name} hotbar.5 with minecraft:stone_sword`);
        for(let attempt=0;attempt<4;attempt++){
          k.cyfra(6);
          if(await until(async()=>/minecraft:stone_sword/.test(await o.selected()),'stone_sword wybrany',450).catch(()=>null))break;
        }
        await o.cmd('minecraft:summon minecraft:husk 0 80 5 {NoAI:1b,PersistenceRequired:1b,Tags:["STATTarget"]}');
        await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);await sleep(160);
        const target='@e[type=minecraft:husk,tag=STATTarget,limit=1]',h0=await o.hp(target),m0=await o.mana(),selected=await o.selected();
        await k.ppmTrzymaj(850);const h1=await o.hp(target),m1=await o.mana();
        const data={points,id,rep,h0,h1,m0,m1,damage:h0-h1,cost:m0-m1,selected:selected.slice(0,150)};measurements.push(data);
        w.add(`STAT3-${id}-${points}-${rep}`,Number.isFinite(h1)&&m1<m0&&selected.includes('stone_sword'),`${id} Wiara ${points}: HP ${h0}→${h1}, mana ${m0}→${m1}`,data);
      });
    }
  }finally{if(granted){const reset=await o.cmd(`mmocore admin attribute take ${o.name} wiara ${grant}`);console.log('STAT-RESET '+reset);}}
  console.log('STAT3-SUMMARY '+JSON.stringify(measurements));
};
