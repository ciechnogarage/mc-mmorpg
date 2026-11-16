'use strict';
const {prepare,action}=require('../lib/fuzzer');
const trace=require('/bot/wyniki/zaklecia/fuzz-161803.json').mini.trace;

module.exports=async({k,o,scene,w,connect})=>{
  const c={k,o,scene};
  await prepare(c);
  let found=null;
  for(let i=0;i<trace.length;i++){
    const a=trace[i];
    await action(c,a,connect,w);
    c.o.maxEvents=10000;c.o.compactItemPackets=false;
    w.checker.prioritySlots=[0];
    const inv=await w.checker.inventory([0]);
    const mismatch=inv.bad.find(x=>x.slot===0&&x.server?.id==='minecraft:paper');
    if(mismatch){
      const packets=c.o.events.filter(e=>['set_slot','window_items','open_window','close_window'].includes(e.packet))
        .slice(-24).map(e=>({packet:e.packet,at:e.at,windowId:e.data.windowId??e.data.window_id,
          slot:e.data.slot,item:e.data.item?.itemId??e.data.item?.name,
          count:e.data.item?.itemCount??e.data.item?.count,stateId:e.data.stateId}));
      found={step:i+1,action:a,mismatch,packets,clientSlot0:c.k.bot.inventory.slots[36]?.name||null,
        windowBefore:c.k.bot.currentWindow?.id??null,screenBefore:c.k.screen};
      c.k.zamknij();await new Promise(r=>setTimeout(r,700));
      const closed=await w.checker.inventory([0]);
      found.afterClose={bad:closed.bad,window:c.k.bot.currentWindow?.id??null,clientSlot0:c.k.bot.inventory.slots[36]?.name||null,
        packets:c.o.events.filter(e=>e.packet==='set_slot'&&e.data.slot===36).slice(-6)
          .map(e=>({windowId:e.data.windowId??e.data.window_id,slot:e.data.slot,item:e.data.item?.itemId??e.data.item?.name,count:e.data.item?.itemCount??e.data.item?.count}))};
      break;
    }
  }
  console.log('N_FUZZ_DIAG '+JSON.stringify(found));
  const durable=!!found?.afterClose?.bad?.length;
  w.add('N_fuzz-52',!durable,found?`${durable?'trwały rozjazd':'maskowanie w otwartym GUI'} po kroku ${found.step} (${found.action.kind})`:'bez rozjazdu po 52 akcjach',found||{});
};
