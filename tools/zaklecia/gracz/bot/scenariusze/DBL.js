'use strict';
const {Vec3}=require('vec3');
const {sleep}=require('../lib/klient');
const {assign}=require('../lib/menu');

module.exports=async({k,o,scene,w,connect})=>{
  const other=await connect('MagCel');
  const summary=[];
  try{
    for(const id of ['leczenie','leczenie_dloni','wielkie_uzdrowienie','plomienie']){
      for(const mode of ['obie','dwa-lpm'])for(let rep=1;rep<=3;rep++)await w.step(`DBL-${id}-${mode}-${rep}`,async()=>{
        await scene.prepare({target:false});
        await o.cmd('minecraft:tp MagCel 0 80 5 180 0');
        await o.cmd(`mmocore admin resource-mana set ${o.name} 20`);
        await assign(k,o,id,'prawa');await assign(k,o,id,'lewa');
        const sel=id==='leczenie_dloni'?'MagCel':id==='plomienie'?'@e[type=minecraft:husk,limit=1]':o.name;
        if(id==='plomienie')await o.cmd('minecraft:summon minecraft:husk 0 80 5 {NoAI:1b,PersistenceRequired:1b}');
        else{
          const who=id==='leczenie_dloni'?'MagCel':o.name;
          await o.cmd(`minecraft:effect give ${who} minecraft:instant_health 1 10 true`);
          await sleep(650);await o.cmd(`minecraft:damage ${who} 6`);
        }
        if(id==='leczenie_dloni'||id==='plomienie')await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);
        else await k.bot.look(0,1.3,true);
        await sleep(150);
        const h0=await o.hp(sel),m0=await o.mana(),snap=o.snapshot();
        if(mode==='obie')await k.obie(id==='wielkie_uzdrowienie'?1700:950);
        else{k.lpmKlik();await sleep(210);k.lpmKlik();await sleep(id==='wielkie_uzdrowienie'?1490:740);}
        await sleep(350);
        const h1=await o.hp(sel),m1=await o.mana();
        const data={id,mode,rep,h0,h1,m0,m1,hpDelta:h1-h0,manaDelta:m0-m1,metrics:o.metrics(snap)};
        summary.push(data);
        w.add(`DBL-${id}-${mode}-${rep}`,Number.isFinite(h1)&&Number.isFinite(m1),`${id} ${mode} #${rep}: HP ${h0}→${h1}, mana ${m0}→${m1}`,data);
        if(mode==='obie'&&['leczenie','leczenie_dloni'].includes(id))k.lpmKlik();
      });
    }
  }finally{other.quit();}
  console.log('DBL-SUMMARY '+JSON.stringify(summary));
};
