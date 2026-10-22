'use strict';
const {sleep}=require('./klient');
const {until}=require('./menu');

module.exports=async({k,o,scene,w},gasi)=>{
  const label=gasi?'N_ppm1':'N_ppm';
  const state=async()=>({
    prawa:await o.cmd(`papi parse ${o.name} %zaklecia_prawa_stan%`),
    lewa:await o.cmd(`papi parse ${o.name} %zaklecia_lewa_stan%`),
    prawyPlaceholder:await o.placeholder('prawa'),
    lewyPlaceholder:await o.placeholder('lewa'),mana:await o.mana(),
    display:await o.cmd('minecraft:execute positioned 0 80 0 if entity @e[type=minecraft:item_display,distance=..12]')
  });
  const active=x=>x.prawa==='rzuca';
  const displays=x=>/Test failed/i.test(x.display)?0:Number(x.display.match(/(?:count:|Count:)\s*(\d+)/)?.[1]||(/Test passed/i.test(x.display)?1:0));
  await w.step(`${label}-kombo`,async()=>{
    await scene.prepare({target:false});
    await o.cmd(`zaklecia przypisz ${o.name} plomienie prawa`);
    await o.cmd(`zaklecia przypisz ${o.name} plomienie lewa`);
    await until(async()=>await o.placeholder('prawa')==='Płomienie'&&await o.placeholder('lewa')==='Płomienie',
      'oba placeholdery Płomienie',2500);
    await sleep(450);await k.bot.look(0,0,true);
    k.ppmWcisnij();await sleep(180);const clicked=k.lpmKlik();
    for(let i=0;i<4;i++){await sleep(170);k.ppmWcisnij();}
    const podczas=await state();k.ppmPusc();await sleep(1000);const po=await state();
    w.add(`${label}-kombo`,clicked&&active(podczas)&&displays(podczas)>=1&&po.lewa!=='rzuca'
      &&active(po)!==gasi&&(gasi?displays(po)===0:displays(po)>=1),
      `po puszczeniu PPM prawa ${gasi?'gaśnie':'trwa'}` ,{clicked,podczas,po,displayPodczas:displays(podczas),displayPo:displays(po),manaPoPuszczeniu:podczas.mana-po.mana});
  });
  await w.step(`${label}-sam-LPM`,async()=>{
    await scene.prepare({target:false});
    await o.cmd(`zaklecia przypisz ${o.name} nic lewa`);
    await o.cmd(`zaklecia przypisz ${o.name} nic prawa`);
    await sleep(200);
    await o.cmd(`zaklecia przypisz ${o.name} plomienie prawa`);
    await until(async()=>await o.placeholder('prawa')==='Płomienie','prawy placeholder',2500);
    await sleep(450);await k.bot.look(0,0,true);const clicked=k.lpmKlik();
    await sleep(650);const przed=await state();await sleep(700);const po=await state();
    w.add(`${label}-sam-LPM`,clicked&&active(przed)&&active(po)&&displays(po)>=1,
      'sam LPM pozostawia prawą jako przełącznik',{clicked,przed,po,displayPo:displays(po),manaZmiana:przed.mana-po.mana});
  });
};
