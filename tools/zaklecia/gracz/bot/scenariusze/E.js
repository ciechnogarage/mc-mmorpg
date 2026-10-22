'use strict';
const {sleep}=require('../lib/klient');const {assign}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  await scene.prepare();await assign(k,o,'plomienie','lewa');
  for(const [id,on,off] of [['bieg',()=>k.w(true),()=>k.w(false)],['sprint',()=>k.sprint(true),()=>k.sprint(false)],['skok',()=>k.skok(true),()=>k.skok(false)],['skradanie',()=>k.shift(true),()=>k.shift(false)]])await w.step(`E-${id}`,async()=>{
    await scene.prepare();await assign(k,o,'plomienie','lewa');
    const s=o.snapshot(),mana0=await o.mana();on();await k.ppmTrzymaj(650);off();const mana1=await o.mana(),delta=mana0!==null&&mana1!==null?mana0-mana1:null;w.add(`E-${id}`,delta!==null&&delta>0,`rzucanie podczas: ${id}`,{...o.metrics(s),manaDelta:delta},{latencyTicks:'≤10',setSlotPerSec:'≤20',manaDelta:'>0'});
  });
  await w.step('E-woda',async()=>{await o.cmd('minecraft:fill -3 80 -3 -1 81 -1 minecraft:water');await o.cmd(`minecraft:tp ${o.name} -2 80 -2`);const s=o.snapshot(),mana0=await o.mana();await k.ppmTrzymaj(600);const mana1=await o.mana(),delta=mana0!==null&&mana1!==null?mana0-mana1:null;w.add('E-woda',delta!==null&&delta>0,'rzucanie w wodzie',{...o.metrics(s),manaDelta:delta},{latencyTicks:'≤10',manaDelta:'>0'});await o.cmd('minecraft:fill -3 80 -3 -1 81 -1 minecraft:air');});
  await w.step('E-seria',async()=>{await assign(k,o,'plomienie','prawa');const s=o.snapshot(),mana0=await o.mana();for(let i=0;i<5;i++){k.lpmKlik();await sleep(240);}const mana1=await o.mana(),delta=mana0!==null&&mana1!==null?mana0-mana1:null;w.add('E-seria',delta!==null&&delta>0,'5 kliknięć LPM w serii',{...o.metrics(s),manaDelta:delta},{messages:'≤5',sounds:'≤5',manaDelta:'>0'});});
  await w.step('E-spam-paska',async()=>{const s=o.snapshot();for(let i=0;i<5;i++){await k.shiftQ();k.kolko(1);await k.shiftQ();}const m=o.metrics(s);w.add('E-spam-paska',m.bossBars>=10,'5 szybkich otwarć paska i ruchów kółka',m,{setSlotPerSec:'≤20',messages:'≤10',bossBars:'≥10'});});
};
