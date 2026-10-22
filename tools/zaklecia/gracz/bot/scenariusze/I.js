'use strict';
const {assign}=require('../lib/menu');const {sleep}=require('../lib/klient');
module.exports=async({k,o,scene,w})=>{
  await scene.prepare({target:false});await assign(k,o,'plomienie','lewa');await sleep(200);
  const paths=['Inventory[{Slot:-106b}]','Inventory[{Slot:-106}]','Inventory[-1]','Inventory[-2]','equipment.offhand','HandItems[1]'];
  const data={client:k.item('off')?.name,placeholder:await o.placeholder('lewa')};
  for(const p of paths)data[p]=await o.cmd(`data get entity ${o.name} ${p}`);
  console.log('DIAG-I '+JSON.stringify(data));
  w.add('I-odczyt-lewej',!!data.client&&data.placeholder==='Płomienie',`po przypisaniu lewej: ${data.placeholder}/${data.client}`,data);
  const s=o.snapshot();await k.shiftQ();await sleep(250);k.q();await sleep(250);
  console.log('DIAG-BAR '+JSON.stringify(o.events.slice(s.index).filter(e=>e.packet==='boss_bar').map(e=>e.data)));
};
