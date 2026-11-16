'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');

module.exports=async({k,o,scene,w})=>{
  for(const item of ['bread','stone','bow'])await w.step(`N3B-${item}`,async()=>{
    await scene.prepare({target:false});
    await assign(k,o,'plomienie','lewa');
    await o.cmd(`minecraft:item replace entity ${o.name} hotbar.5 with minecraft:${item}`);
    k.cyfra(6);
    const ready=await until(async()=>{
      const selected=await o.selected();
      return selected.includes(`minecraft:${item}`)?selected:null;
    },`${item} w SelectedItem`,3000);
    const mana0=await o.mana();
    await k.ppmTrzymaj(650);await sleep(200);
    const selected=await o.selected(),mana1=await o.mana(),slot=k.bot.quickBarSlot;
    w.add(`N3B-${item}`,selected.includes(`minecraft:${item}`)&&slot===5,
      `${item} w slocie 5 po PPM; slot ${slot}, mana ${mana0}→${mana1}`,
      {item,ready:ready.slice(0,120),selected:selected.slice(0,120),slot,mana0,mana1});
  });
};
