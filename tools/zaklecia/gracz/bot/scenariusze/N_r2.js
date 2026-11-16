'use strict';
const {sleep}=require('../lib/klient');
const {book,click,until}=require('../lib/menu');
const {closesBar}=require('../lib/niezmienniki');

module.exports=async({k,o,scene,w})=>{
  await w.step('N6-panel-dlonie',async()=>{
    await scene.prepare({target:false});
    await book(k);
    const before=JSON.stringify(k.bot.currentWindow?.slots?.[38]||null);
    const s=o.snapshot();
    const at=Date.now();
    await click(k.bot,23,0);
    await until(async()=>/Płomienie/i.test(await o.placeholder('prawa')),'przypisanie Płomieni',1500);
    await sleep(150);
    const after=JSON.stringify(k.bot.currentWindow?.slots?.[38]||null);
    const packets=o.events.slice(s.index).filter(e=>e.packet==='set_slot'&&
        (e.data.slot===38||e.data.slotId===38));
    w.add('N6-panel-dlonie',before!==after&&/Płomienie/i.test(after)&&Date.now()-at<700,
      'panel dłoni pokazuje nowy czar w otwartym oknie',
      {elapsedMs:Date.now()-at,packets:packets.length,changed:before!==after,
        containsSpell:/Płomienie/i.test(after),before:before.slice(-700),after:after.slice(-700)},
      {after:'Płomienie',okno:'pozostaje otwarte',elapsedMs:'<700'});
    k.zamknij();
  });
  await w.step('N4-bossbar-raw',async()=>{
    await scene.prepare({target:false});
    const start=o.snapshot();
    await k.shiftQ();
    await sleep(350);
    const opened=o.events.slice(start.index).filter(e=>e.packet==='boss_bar');
    const close=o.snapshot();
    k.q();
    await sleep(350);
    const closed=o.events.slice(close.index).filter(e=>e.packet==='boss_bar');
    const words=(value,out=[])=>{
      if(!value||typeof value!=='object')return out;
      if(value.text?.type==='string')out.push(value.text.value);
      for(const [key,child] of Object.entries(value))if(key!=='text')words(child,out);
      return out;
    };
    const brief=a=>a.map(e=>({id:e.data.entityUUID,action:e.data.action,
      title:words(e.data.title).join('').slice(0,130)})).slice(0,30);
    console.log('DIAG-N4-R2 '+JSON.stringify({opened:brief(opened),closed:brief(closed)}));
    const own=new Set(opened.filter(e=>/✦|many\/s/.test(JSON.stringify(e.data.title||'')))
      .map(e=>e.data.entityUUID));
    const removed=new Set(closed.filter(closesBar).map(e=>e.data.entityUUID));
    w.add('N4-bossbar-raw',[...own].every(id=>removed.has(id)),
      'zamknięcie paska po Q (REMOVE lub puste ADD MythicHUD)',{own:[...own],removed:[...removed]},{removed:'wszystkie otwarte paski'});
  });
};
