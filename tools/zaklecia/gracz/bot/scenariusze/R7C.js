'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');

const items=['bread','potion','bow','iron_pickaxe','stone','filled_map'];
async function inventory(o){
  const result={};
  for(const n of [...Array(36).keys(),100,101,102,103,-106]){
    const raw=await o.cmd(`minecraft:data get entity ${o.name} Inventory[{Slot:${n}b}]`);
    const id=raw.match(/id: "minecraft:([^\"]+)"/)?.[1]||'empty';
    const count=raw.match(/count: (\d+)/)?.[1]||0;
    result[n]=id==='empty'?'empty':`${id}×${count}`;
  }
  return result;
}
module.exports=async({k,o,scene,w})=>{
  for(const item of items)await w.step(`R7C-${item}`,async()=>{
    await scene.prepare({target:false});
    await assign(k,o,'plomienie','lewa');
    await o.cmd(`minecraft:item replace entity ${o.name} hotbar.5 with minecraft:${item}`);
    const source='hotbar.5';
    let ready=false;
    for(let attempt=0;attempt<4&&!ready;attempt++){
      k.cyfra(6);
      ready=!!(await until(async()=>(await o.selected()).includes(`minecraft:${item}`),'wybrany przedmiot',900).catch(()=>null));
    }
    const selectedBefore=await o.selected();
    const before=await inventory(o);
    await k.ppmTrzymaj(650);await sleep(250);
    const after=await inventory(o);
    const locations=Object.entries(after).filter(([,v])=>v.startsWith(`${item}×`));
    const copies=(state)=>Object.values(state).filter(v=>v.startsWith(`${item}×`))
      .reduce((sum,v)=>sum+Number(v.split('×')[1]),0);
    const preserved=copies(before)===copies(after);
    w.add(`R7C-${item}`,preserved&&after[5]?.startsWith(`${item}×`),
      `${item}: przedmiot po PPM w slocie 5`,{source,ready,selectedBefore:selectedBefore.slice(0,130),before,after,locations},
      {slot5:`${item}×1`,liczbaKopii:'bez zmian'});
  });
};
