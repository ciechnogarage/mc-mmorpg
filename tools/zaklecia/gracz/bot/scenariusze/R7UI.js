'use strict';
const {sleep}=require('../lib/klient');
const {visibleTitle}=require('../lib/niezmienniki');
module.exports=async({k,o,scene,w})=>{
  await w.step('R7UI-pasek',async()=>{
    await scene.prepare({target:false});
    const s=o.snapshot();
    await k.shiftQ();await sleep(300);
    const open=o.events.slice(s.index);
    k.kolko(1);await sleep(300);
    const wheel=o.events.slice(s.index+open.length);
    k.q();await sleep(300);
    const close=o.events.slice(s.index+open.length+wheel.length);
    const summary=(events)=>({
      slots:events.filter(e=>e.packet==='set_slot').map(e=>({slot:e.data.slot,windowId:e.data.windowId??e.data.window_id})),
      bars:events.filter(e=>e.packet==='boss_bar').map(e=>({id:e.data.entityUUID,action:e.data.action,title:visibleTitle(e.data.title).slice(0,100)}))
    });
    const detail={open:summary(open),wheel:summary(wheel),close:summary(close)};
    console.log('R7UI '+JSON.stringify(detail));
    w.add('R7UI-pasek',detail.wheel.slots.length<=1&&detail.wheel.bars.length<=2,
      'jedno przewinięcie paska: ≤1 set_slot, ≤2 bossbar',
      {slotWheel:detail.wheel.slots.length,barWheel:detail.wheel.bars.length});
  });
};
