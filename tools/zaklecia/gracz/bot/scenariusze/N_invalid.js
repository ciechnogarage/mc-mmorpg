'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  await w.step('N9-id-prawa',async()=>{
    await scene.prepare({target:false});
    await assign(k,o,'plomienie','prawa');
    const before=await o.placeholder('prawa');
    const selected=await o.selected();
    await o.cmd(`zaklecia przypisz ${o.name} nieistniejace_id prawa`);
    await sleep(150);
    const after=await o.placeholder('prawa');
    w.add('N9-id-prawa',before===after&&/Płomienie/i.test(after),
      'błędne id nie usuwa zaklęcia z prawej dłoni',
      {before,after,selectedBefore:selected.slice(0,120),selectedAfter:(await o.selected()).slice(0,120)});
  });
  await w.step('N9-id-lewa',async()=>{
    await scene.prepare({target:false});
    await assign(k,o,'odmrozenie','lewa');
    const before=await o.placeholder('lewa');
    await o.cmd(`zaklecia przypisz ${o.name} nieistniejace_id lewa`);
    await sleep(150);
    const after=await o.placeholder('lewa');
    w.add('N9-id-lewa',before===after&&/Odmrożenie/i.test(after),
      'błędne id nie usuwa zaklęcia z lewej dłoni',{before,after});
  });
};
