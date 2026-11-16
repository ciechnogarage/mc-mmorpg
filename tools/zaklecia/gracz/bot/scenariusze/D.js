'use strict';
const {sleep}=require('../lib/klient');const {until}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  const previous=await o.cmd('minecraft:gamerule keep_inventory');
  try{
  await scene.prepare({target:false});await o.cmd('minecraft:gamerule keep_inventory true');
  await o.cmd(`minecraft:give ${o.name} minecraft:stone 1`);await sleep(300);
  const before=await w.checker.inventory(),s=o.snapshot();
  await o.cmd('minecraft:setblock 0 111 0 minecraft:stone');await o.cmd(`minecraft:tp ${o.name} 0.5 112 0.5 0 0`);
  await until(()=>k.bot.entity.position.y>110,'wieża D',4000);let died=false;k.bot.once('death',()=>{died=true;});
  k.w(true);await until(()=>k.bot.entity.position.z>1.6,'krawędź D',5000).catch(()=>{});k.w(false);
  await until(()=>died,'upadek D',8000);await o.cmd('minecraft:setblock 0 111 0 minecraft:air');
  for(const ms of [500,2000,5000]){
    await sleep(ms);const inv=await w.checker.inventory();
    const events=o.events.slice(s.index).filter(e=>e.packet==='window_items'||e.packet==='set_slot');
    const windows=events.filter(e=>e.packet==='window_items').map(e=>({id:e.data.windowId??e.data.window,slots:e.data.items?.length,occupied:e.data.items?.filter(x=>x?.present||x?.name||x?.itemCount>0).length}));
    w.add(`D-inwentarz-${ms}`,inv.bad.length===0,`po śmierci i ${ms} ms: zyje=${k.bot.isAlive}, rozbieżności=${inv.bad.length}, pakiety=${events.length}`,{before:before.bad.length,bad:inv.bad.slice(0,8),read:inv.read,packets:events.length,windows,windowType:k.bot.currentWindow?.type||null,ownSlot1:k.bot.inventory.slots[37]?.name||null,activeSlot1:k.bot.currentWindow?.slots?.[k.bot.currentWindow.slots.length-8]?.name||null,last:events.slice(-3).map(e=>e.packet)});
    await w.checker.check(`D-inwentarz-${ms}`);
  }
  if(k.bot.currentWindow)k.zamknij();await sleep(800);
  const closed=await w.checker.inventory();w.add('D-po-zamknieciu',closed.bad.length===0,`po zamknięciu okna: ${closed.bad.length} rozbieżności`,{bad:closed.bad.slice(0,8),ownSlot1:k.bot.inventory.slots[37]?.name||null,window:k.bot.currentWindow?.type||null});
  await w.checker.check('D-po-zamknieciu');
  }finally{await o.cmd(`minecraft:gamerule keep_inventory ${/true/i.test(previous)?'true':'false'}`);}
};
