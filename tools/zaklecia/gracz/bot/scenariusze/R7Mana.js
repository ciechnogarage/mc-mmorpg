'use strict';
const {sleep}=require('../lib/klient');
const {assign}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  async function cast(both){
    await scene.prepare();
    await assign(k,o,'przywolany_miecz','lewa');
    if(both)await assign(k,o,'przywolany_miecz','prawa');
    await k.bot.look(0,0,true);await sleep(450);
    const before=await o.mana(),hand={prawa:await o.placeholder('prawa'),lewa:await o.placeholder('lewa')};
    const s=o.snapshot();
    const states=[];
    k.ppmWcisnij();
    for(let attempt=0;attempt<5;attempt++){
      await sleep(100);
      const state=await o.cmd(`papi parse ${o.name} %zaklecia_lewa_stan%`);
      if(state==='laduje'||state==='gotowe')break;
      if(!k.using)k.ppmWcisnij();
    }
    let lpmAccepted=null,usingAtLpm=null,selectedAtLpm=null;
    if(both){await sleep(70);usingAtLpm=k.using;selectedAtLpm=(await o.selected()).slice(0,160);
      lpmAccepted=k.lpmKlik();}
    for(let i=0;i<7;i++){await sleep(200);if(i<6&&!k.using)k.ppmWcisnij();
      states.push([await o.cmd(`papi parse ${o.name} %zaklecia_lewa_stan%`),
        await o.cmd(`papi parse ${o.name} %zaklecia_prawa_stan%`)]);}
    k.ppmPusc();await sleep(350);const after=await o.mana();
    return {before,after,delta:before-after,hand,states,lpmAccepted,usingAtLpm,selectedAtLpm,events:o.metrics(s)};
  }
  let single;
  await w.step('R7Mana-jedna',async()=>{single=await cast(false);w.add('R7Mana-jedna',single.delta>1.2,'koszt jednej dłoni',single);});
  await w.step('R7Mana-obie',async()=>{const both=await cast(true),ratio=both.delta/single.delta;
    w.add('R7Mana-obie',ratio>=2.2&&ratio<=3.4,'koszt oburącz ×2,8',{single,both,ratio},{ratio:'2,2–3,4'});
  });
};
