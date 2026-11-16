'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  const state=hand=>o.cmd(`papi parse ${o.name} %zaklecia_${hand}_stan%`);
  const cast=async both=>{
    await scene.prepare({target:false});
    await assign(k,o,'przywolany_miecz','lewa');
    if(both)await assign(k,o,'przywolany_miecz','prawa');
    if(k.bot.currentWindow)k.zamknij();
    await sleep(1600); // filtr gestu Q po zamknięciu menu
    const before=await o.mana(),hands={left:await o.placeholder('lewa'),right:await o.placeholder('prawa')};
    let leftAtRelease=null,rightAtRelease=null,accepted=[],history=[];
    try{
      k.ppmWcisnij();
      await until(async()=>['laduje','gotowe'].includes(await state('lewa')),'ładowanie lewej',1800);
      if(both){
        for(let n=0;n<3;n++){
          accepted.push(k.lpmKlik());await sleep(170);
          if(['laduje','gotowe'].includes(await state('prawa')))break;
        }
        await until(async()=>['laduje','gotowe'].includes(await state('prawa')),'ładowanie prawej',1200);
      }
      let nextPulse=Date.now()+180;
      await until(async()=>{
        if(!k.using&&Date.now()>=nextPulse){k.ppmWcisnij();nextPulse=Date.now()+200;}
        const left=await state('lewa'),right=await state('prawa');
        history.push([left,right]);
        return left==='gotowe'&&(!both||right==='gotowe');
      },'gotowość dłoni przed zwolnieniem',2500);
      leftAtRelease=await state('lewa');rightAtRelease=await state('prawa');
    }finally{console.log('B5MANA-TRACE '+JSON.stringify({both,history,accepted}));k.ppmPusc();}
    await sleep(350);
    const after=await o.mana();
    return {before,after,delta:before-after,hands,leftAtRelease,rightAtRelease,accepted};
  };
  let one;
  await w.step('B5Mana-jedna',async()=>{one=await cast(false);w.add('B5Mana-jedna',one.delta>1.8&&one.delta<2.2,'koszt jednej dłoni',one,{delta:2});});
  await w.step('B5Mana-obie',async()=>{
    const both=await cast(true),ratio=both.delta/one.delta;
    w.add('B5Mana-obie',both.leftAtRelease==='gotowe'&&both.rightAtRelease==='gotowe'&&ratio>2.6&&ratio<3,
      'obie dłonie gotowe przed zwolnieniem PPM',{one,both,ratio},{ratio:2.8});
  });
};
