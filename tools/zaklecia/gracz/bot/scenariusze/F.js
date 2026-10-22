'use strict';
const {sleep}=require('../lib/klient');const {assign}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  await scene.prepare();await assign(k,o,'plomienie','lewa');await sleep(450);const hand=await o.placeholder('lewa');
  const before=o.snapshot(),mana0=await o.mana();k.ppmWcisnij();await sleep(900);const stop=Date.now();k.ppmPusc();await sleep(500);const mana1=await o.mana(),m=o.metrics(before);
  const after=o.events.slice(before.index).filter(x=>x.at>=stop&&['entity_destroy','entity_metadata','action_bar'].includes(x.packet));
  const release=after.length?Math.round((after[0].at-stop)/50):null;
  w.add('F01',hand!=='pusta',`lewa=${hand}; mana ${mana0}→${mana1}`,{...m,releaseTicks:release,manaDelta:mana0!=null&&mana1!=null?mana0-mana1:null},{latencyTicks:'≤4',releaseTicks:'≤8',setSlotPerSec:'≤20',messages:'≤3',manaDelta:'0,5/tik kosztowy (10t)'});
  const selected=await o.selected(),off=k.bot.inventory.slots[45];w.add('F02',hand!=='pusta'&&!!off,'stan dłoni vs placeholder, hash ekwipunku',{placeholder:hand,offhand:off?.name,hash:o.hash(),selected:selected.slice(0,100)},{placeholder:'zgodny z nośnikiem',hash:'stabilny'});
  await w.step('F-ruch',async()=>{
    await scene.prepare();await assign(k,o,'plomienie','lewa');await sleep(450);const normal=await o.speed();
    k.ppmWcisnij();let magic=await o.speed();const untilTime=Date.now()+2000;
    while(normal&&magic>=normal*0.75&&Date.now()<untilTime){await sleep(50);magic=await o.speed();}
    k.ppmPusc();const ratio=normal&&magic?magic/normal:null;
    w.add('F-ruch',ratio!==null&&ratio>=0.35&&ratio<=0.75,'atrybut szybkości serwera podczas rzucania; ruch klienta do etapu 3',{normal,czar:magic,ratio},{ratio:'0,35–0,75 (config -0,5)'});
  });
  const errors=o.logErrors();
  if(errors===null)w.skip('F03','latest.log nie jest montowany w bocie; runner sprawdza BLEDY-LOGU po procesie');
  else w.add('F03',errors.length===0,'nowe ERROR/Exception w latest.log, po odfiltrowaniu luk bazowych',{noweBledy:errors.length,przyklady:errors.slice(-3)},{noweBledy:0});
};
