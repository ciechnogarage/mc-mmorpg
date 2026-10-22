'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');

const kinds=['bread','potion','bow','iron_pickaxe','stone','map'];
const slots=[...Array(36).keys(),100,101,102,103,-106];
async function whole(o){
  const out={};
  for(const slot of slots){
    const raw=await o.cmd(`minecraft:data get entity ${o.name} Inventory[{Slot:${slot}b}]`);
    const id=raw.match(/\bid:\s*"(minecraft:[^"]+)"/)?.[1]||null;
    const count=Number(raw.match(/\bcount:\s*(\d+)/)?.[1]||0);
    out[slot]=id?{id,count}:null;
  }
  return out;
}
function copies(state,id){return Object.values(state).reduce((n,x)=>n+(x?.id===id?x.count:0),0);}
function familyCopies(state,kind){return copies(state,`minecraft:${kind}`)+(kind==='map'?copies(state,'minecraft:filled_map'):0);}
module.exports=async({k,o,scene,w})=>{
  for(const kind of kinds)await w.step(`B5Items-${kind}`,async()=>{
    k.cyfra(1);await sleep(150);
    await scene.prepare({target:false});await assign(k,o,'plomienie','lewa');
    await o.cmd(`minecraft:clear ${o.name} minecraft:filled_map`);
    k.cyfra(1);await sleep(150);
    const id=`minecraft:${kind}`;
    await o.cmd(`minecraft:give ${o.name} ${id} 1`);
    let source=await until(()=>{
      const found=k.bot.inventory.slots.findIndex((item,index)=>index>=9&&index<=44&&item?.name===kind);
      return found<0?null:found;
    },`${kind} w ekwipunku po /give`,2500);
    const rawBeforeMove=await whole(o);
    if(source>=36){
      const backpack=k.bot.inventory.slots.findIndex((item,index)=>index>=20&&index<=35&&!item);
      if(backpack<0)throw Error(`brak wolnego slotu plecaka dla ${kind}`);
      k.e();try{await k.bot.clickWindow(source,0,0);await k.bot.clickWindow(backpack,0,0);}finally{k.zamknij();}
      source=backpack;await sleep(150);
    }
    k.e();try{await k.bot.clickWindow(source,0,0);await k.bot.clickWindow(41,0,0);}finally{k.zamknij();}
    k.cyfra(6);
    const selected=await until(async()=>{const raw=await o.selected();return raw.includes(id)?raw:null;},`${kind} wybrany w slocie 5`,1800);
    const before=await whole(o),clientBefore=k.bot.heldItem?.name||null;
    await k.ppmTrzymaj(650);await sleep(300);
    if(k.bot.currentWindow)k.zamknij();
    const after=await whole(o),clientAfter=k.bot.heldItem?.name||null;
    const pre=familyCopies(before,kind),post=familyCopies(after,kind);
    const afterExpected=kind==='map'?'minecraft:filled_map':id;
    w.add(`B5Items-${kind}`,before[5]?.id===id&&after[5]?.id===afterExpected&&pre===post,
      `${kind}: klik z plecaka ${source} do hotbar.5, przed/po PPM`,
      {source,initialCount:familyCopies(rawBeforeMove,kind),beforeCount:pre,afterCount:post,
       before5:before[5],after5:after[5],clientBefore,clientAfter,selected:selected.slice(0,120),
       before,after},{slot5:afterExpected,copies:'bez zmiany'});
  });
};
