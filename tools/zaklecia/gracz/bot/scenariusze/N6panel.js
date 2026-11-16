'use strict';
const {sleep}=require('../lib/klient');
const {book,click,until}=require('../lib/menu');

module.exports=async({k,o,scene,w})=>{
  await w.step('N6-ikony-dloni',async()=>{
    await scene.prepare({target:false});
    await book(k);
    const slot=(n)=>{const item=k.bot.currentWindow?.slots?.[n];return {
      model:item?.components?.find(c=>c.type==='item_model')?.data||null,
      lore:JSON.stringify(item?.components?.find(c=>c.type==='lore')?.data||[]).match(/Płomienie|Odmrożenie|pusta/g)||[]
    };};
    const panel=()=>({prawa:slot(37),lewa:slot(38)});
    const before=panel();
    await click(k.bot,23,0);
    await until(async()=>/Płomienie/i.test(await o.placeholder('prawa')),'prawa ręka');
    await sleep(250);
    await click(k.bot,24,1);
    await until(async()=>/Odmrożenie/i.test(await o.placeholder('lewa')),'lewa ręka');
    await sleep(200);
    const assigned=panel();
    await click(k.bot,37,0);
    await until(async()=>(await o.placeholder('prawa'))==='pusta','opróżnienie prawej');
    await sleep(250);
    await click(k.bot,38,1);
    await until(async()=>(await o.placeholder('lewa'))==='pusta','opróżnienie lewej');
    await sleep(200);
    const cleared=panel();
    const valid=assigned.prawa.model==='zaklecia:plomienie'&&assigned.lewa.model==='zaklecia:odmrozenie'
      &&assigned.prawa.lore.includes('Płomienie')&&assigned.lewa.lore.includes('Odmrożenie')
      &&before.prawa.model==='zaklecia:pusta'&&before.lewa.model==='zaklecia:pusta'
      &&cleared.prawa.model==='zaklecia:pusta'&&cleared.lewa.model==='zaklecia:pusta';
    w.add('N6-ikony-dloni',valid,'dwa sloty panelu: modele przypisane i puste',
      {before,assigned,cleared},
      {prawa:'zaklecia:plomienie → zaklecia:pusta',lewa:'zaklecia:odmrozenie → zaklecia:pusta'});
    k.zamknij();
  });
};
