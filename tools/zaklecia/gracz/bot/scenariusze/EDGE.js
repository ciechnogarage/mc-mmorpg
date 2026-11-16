'use strict';
const {Vec3}=require('vec3');const {sleep,Klient}=require('../lib/klient');const {Obserwator}=require('../lib/obserwator');const {Niezmienniki}=require('../lib/niezmienniki');const {assign,until}=require('../lib/menu');
module.exports=async({k,o,scene,w,connect})=>{
  const original=await o.cmd('minecraft:gamerule keep_inventory');let again;
  const result=async(...args)=>{w.add(...args);await w.checker.check(args[0]);};
  try{
    await w.step('EDGE-keepInventory-false',async()=>{
      await scene.prepare({target:false});await assign(k,o,'plomienie','lewa');
      const flag=await o.cmd('minecraft:gamerule keep_inventory false');
      await o.cmd('minecraft:setblock 0 111 0 minecraft:stone');await o.cmd(`minecraft:tp ${o.name} 0.5 112 0.5 0 0`);
      await until(()=>k.bot.entity.position.y>110,'wieża EDGE',4000);let died=false;k.bot.once('death',()=>{died=true;});
      k.w(true);await until(()=>k.bot.entity.position.z>1.6,'zejście EDGE',5000).catch(()=>{});k.w(false);
      await until(()=>died,'upadek EDGE',8000);await o.cmd('minecraft:setblock 0 111 0 minecraft:air');await sleep(1500);
      await result('EDGE-keepInventory-false',died,`śmierć z keep_inventory=false; ${flag}`,{died,hand:await o.placeholder('lewa')});
    });
    await w.step('EDGE-pelny-ekwipunek',async()=>{
      await scene.prepare({target:false});await assign(k,o,'przywolany_miecz','lewa');
      for(let n=0;n<27;n++)if(!k.bot.inventory.slots[n+9])await o.cmd(`minecraft:item replace entity ${o.name} inventory.${n} with minecraft:stone 64`);
      for(let n=0;n<9;n++)if(!k.bot.inventory.slots[n+36])await o.cmd(`minecraft:item replace entity ${o.name} hotbar.${n} with minecraft:stone 64`);
      await sleep(300);
      const occupied=k.bot.inventory.slots.slice(9,45).filter(Boolean).length;
      const before=(await w.checker.inventory()).bad;
      await k.bot.look(0,0,true);await k.ppmTrzymaj(1050);await sleep(200);
      const after=(await w.checker.inventory()).bad,hand=await o.placeholder('lewa');
      await result('EDGE-pelny-ekwipunek',occupied===36&&before.length===0&&after.length===0,`plecak ${occupied}/36 i przywołany miecz; rozbieżności ${before.length}→${after.length}`,{before,after,hand,occupied});
      await o.cmd(`minecraft:clear ${o.name} minecraft:stone`);
    });
    await w.step('EDGE-drugi-swiat',async()=>{
      await scene.prepare({target:false});await assign(k,o,'runa_ognia','lewa');await k.bot.lookAt(new Vec3(0.5,79.8,3.5),true);await k.ppmTrzymaj(1100);
      const old=await o.cmd('minecraft:execute in minecraft:overworld positioned 0 80 0 if entity @e[type=minecraft:armor_stand,distance=..12]');
      let create,world;
      try{create=await o.cmd('mv create zak_b_r1 normal');}catch(e){create=`RCON czekanie na tworzenie świata: ${e.message}`;await sleep(1500);}
      try{world=await o.cmd('mv tp MagTest zak_b_r1');}catch(e){world=`RCON czekanie na teleport: ${e.message}`;}
      await sleep(800);
      const now=await o.cmd(`data get entity ${o.name} Dimension`),left=await o.placeholder('lewa');
      await result('EDGE-drugi-swiat',/zak_b_r1/.test(now),`Multiverse: ${create}; ${world}; ${now}; runa przed=${old}`,{dimension:now,hand:left});
      await o.cmd(`minecraft:execute in minecraft:overworld run minecraft:tp ${o.name} 0 80 0`);
    });
    await w.step('EDGE-smierc-pasek',async()=>{
      await scene.prepare({target:false});await assign(k,o,'plomienie','lewa');await k.shiftQ();await sleep(250);
      const s=o.snapshot();await o.cmd('minecraft:setblock 0 111 0 minecraft:stone');await o.cmd(`minecraft:tp ${o.name} 0.5 112 0.5 0 0`);
      await until(()=>k.bot.entity.position.y>110,'wieża pasek',4000);let died=false;k.bot.once('death',()=>{died=true;});
      k.w(true);await until(()=>k.bot.entity.position.z>1.6,'zejście pasek',5000).catch(()=>{});k.w(false);await until(()=>died,'śmierć pasek',8000);
      await o.cmd('minecraft:setblock 0 111 0 minecraft:air');await sleep(1500);
      const removed=o.events.slice(s.index).filter(e=>e.packet==='boss_bar'&&e.data.action===1).length;
      await result('EDGE-smierc-pasek',removed>=2,`śmierć z paskiem: REMOVE=${removed}`,{removed,died});
    });
    for(const [id,aim] of [['plomienie','husk'],['ognisty_pocisk','husk'],['runa_ognia','ground'],['oslona','air'],['przywolany_miecz','air'],['duch_wilka','air'],['uspokojenie','husk']])await w.step(`EDGE-mana-${id}`,async()=>{
      await scene.prepare();if(id==='uspokojenie')await o.cmd('minecraft:tp @e[type=minecraft:husk,limit=1] 0 80 2');
      await assign(k,o,id,'lewa');
      if(aim==='husk')await k.bot.lookAt(new Vec3(0.5,80.8,id==='uspokojenie'?2.5:5.5),true);
      else if(aim==='ground')await k.bot.lookAt(new Vec3(0.5,79.8,3.5),true);else await k.bot.look(0,0,true);
      k.ppmWcisnij();await sleep(300);await o.cmd(`mmocore admin resource-mana set ${o.name} 0`);await sleep(850);k.ppmPusc();await sleep(250);
      const mana=await o.mana(),state=await o.cmd(`papi parse ${o.name} %zaklecia_lewa_stan%`);
      await result(`EDGE-mana-${id}`,mana!==null&&mana>=0&&state!=='rzuca',`${id}: mana=${mana}, stan=${state}`,{mana,state});
    });
    await w.step('EDGE-relog-pasek',async()=>{
      await scene.prepare({target:false});await assign(k,o,'plomienie','lewa');await k.shiftQ();await k.relog(1200);
      again=await connect(o.name);const obs=new Obserwator(again,o.rcon,o.name),kl=new Klient(again,obs);await sleep(300);
      const bars=obs.events.filter(e=>e.packet==='boss_bar'&&/✦|many\/s/.test(JSON.stringify(e.data.title||'')));
      w.add('EDGE-relog-pasek',bars.length===0,`relog po 1,2 s z paskiem, magiczne BossBary po powrocie=${bars.length}`,{bars:bars.length});
      const checker=new Niezmienniki(obs,kl,w);checker.rows=w.checker.rows;w.checker=checker;await checker.check('EDGE-relog-pasek');
    });
  }finally{
    try{await o.cmd(`minecraft:gamerule keep_inventory ${/true/i.test(original)?'true':'false'}`);}catch{}
    try{await o.cmd(`minecraft:execute in minecraft:overworld run minecraft:tp ${o.name} 0 80 0`);}catch{}
    again?.quit();
  }
};
