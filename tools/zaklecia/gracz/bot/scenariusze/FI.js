'use strict';
const fs=require('node:fs');const path=require('node:path');const {prepare,action}=require('../lib/fuzzer');
module.exports=async({k,o,scene,w,connect})=>{
  const input=JSON.parse(fs.readFileSync(path.join(process.env.ZAK_WYNIKI||'/bot/wyniki/zaklecia','fuzz-65537.json'),'utf8'));
  const trace=input.trace.slice(0,82),c={k,o,scene},hits=[];
  try{
    await prepare(c);
    for(let i=0;i<trace.length;i++){
      await action(c,trace[i],connect,w);
      const row=await w.checker.check(`FI-${String(i+1).padStart(3,'0')}-${trace[i].kind}`);
      const diff=row.inventoryDifferences.find(x=>x.slot===14);
      if(diff){const packets=c.o.events.filter(e=>e.packet==='set_slot'||e.packet==='window_items').slice(-5)
        .map(e=>({packet:e.packet,window:e.data.windowId??e.data.window,slot:e.data.slot,
          item:JSON.stringify(e.data.item??e.data.items?.[14]??{}).slice(0,180)}));
        hits.push({step:i+1,action:trace[i],server:diff.server,client:diff.client,packets});}
    }
    w.add('FI-papier-slot14',hits.length>0,`prefiks ${trace.length} akcji ziarna 65537: różnice slotu 14 w ${hits.length} krokach`,{hits});
  }finally{if(c.k!==k){c.k.close();c.k.bot.quit();}}
};
