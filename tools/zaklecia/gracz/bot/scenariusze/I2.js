'use strict';
const {sleep}=require('../lib/klient');const {assign}=require('../lib/menu');const {slotKlienta,clientItem}=require('../lib/niezmienniki');
module.exports=async({k,o,scene,w})=>{
  await scene.prepare({target:false});await assign(k,o,'plomienie','lewa');await assign(k,o,'odmrozenie','prawa');
  const inspect=async label=>{
    const n=8,own=k.bot.inventory.slots[slotKlienta(n)],active=clientItem(k.bot,n),win=k.bot.currentWindow;
    const server=await o.cmd(`data get entity ${o.name} Inventory[{Slot:${n}b}]`);
    const inv=await w.checker.inventory();
    w.add(`I2-${label}`,inv.bad.length===0,`${label}: rozbieżności ${inv.bad.length}; slot 8 serwer=${server.slice(0,100)}`,
      {own:own?.name||null,active:active?.name||null,window:win?.type||null,windowSlots:win?.slots?.length||0,bad:inv.bad.slice(0,5)});
    await w.checker.check(`I2-${label}`);
  };
  await inspect('przed-E');
  k.e();try{await k.bot.clickWindow(44,0,0);await k.bot.clickWindow(44,0,0);}finally{k.zamknij();}
  await inspect('zaraz-po-E');await sleep(800);await inspect('po-800ms');
};
