'use strict';
const {Vec3}=require('vec3');const {Klient,sleep}=require('../lib/klient');const {Obserwator}=require('../lib/obserwator');const {assign,until}=require('../lib/menu');
module.exports=async({k,o,scene,w,connect})=>{
  const peers=[];const add=async name=>{const bot=await connect(name),obs=new Obserwator(bot,o.rcon,name),client=new Klient(bot,obs);peers.push({bot,o:obs,k:client});return peers.at(-1);};
  const count=async type=>{const s=await o.cmd(`minecraft:execute in minecraft:overworld positioned 0 80 0 if entity @e[type=minecraft:${type},distance=..24]`);return Number(s.match(/Count:\s*(\d+)/i)?.[1]||0);};
  const result=async(...args)=>{w.add(...args);await w.checker.check(args[0]);};
  let cel,blok,drugi;
  try{
    cel=await add('MagCel');blok=await add('MagBlok');drugi=await add('MagWspol');
    await scene.prepare({target:false});await o.cmd('minecraft:tp MagBlok 20 80 0');await o.cmd('minecraft:tp MagWspol -5 80 0');await scene.targetBot();
    const deny=await o.cmd('rg flag -w world __global__ pvp deny');console.log('P-WG-DENY '+deny);
    await assign(k,o,'duch_wilka','prawa');await k.bot.lookAt(new Vec3(-1,81,5),true);k.lpmKlik();
    await until(async()=>await count('wolf')>0,'wilk PvP',4000);await o.cmd('minecraft:tp MagCel 0 80 2');
    await o.cmd(`mmocore admin attribute give ${o.name} bieglosc-miecz 1`);
    await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);
    const swordSlot=await until(()=>{const n=k.bot.inventory.slots.findIndex((x,i)=>i>=9&&i<=44&&x?.name==='stone_sword');return n<0?null:n},'miecz MMO PvP',2500);
    if(swordSlot<36){k.e();await k.bot.clickWindow(swordSlot,0,0);await k.bot.clickWindow(42,0,0);k.zamknij();k.cyfra(7);}else k.cyfra(swordSlot-35);
    const celEntity=await until(()=>Object.values(k.bot.entities).find(e=>e.username==='MagCel'),'cel PvP',2000);
    await k.bot.lookAt(celEntity.position.offset(0,1,0),true);
    await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');await sleep(500);
    const hpD0=await o.hp('MagCel');k.bot.attack(celEntity);await sleep(700);const hpD1=await o.hp('MagCel');
    await result('P-PvP-deny',Math.abs(hpD1-hpD0)<0.6&&/deny|set|ustaw/i.test(deny),`WorldGuard deny, MagCel ${hpD0}→${hpD1}; ${deny}`,{hpDelta:hpD0-hpD1,wolves:await count('wolf')});
    const allow=await o.cmd('rg flag -w world __global__ pvp allow');console.log('P-WG-ALLOW '+allow);
    await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');await sleep(1200);
    const hpA0=await o.hp('MagCel');k.bot.attack(celEntity);await sleep(700);const hpA1=await o.hp('MagCel');
    await result('P-PvP-allow',hpA1<hpA0,`WorldGuard allow, cios MagTest w MagCel z wilkiem: ${hpA0}→${hpA1}`,{hpDelta:hpA0-hpA1,wolves:await count('wolf'),targetId:celEntity.id});
    await scene.prepare({target:false});await assign(k,o,'duch_wilka','prawa');await k.bot.lookAt(new Vec3(-1,81,5),true);k.lpmKlik();
    await until(async()=>await count('wolf')>0,'pierwszy wilk',3000);
    await assign(drugi.k,drugi.o,'duch_wilka','prawa');await drugi.bot.lookAt(new Vec3(-5,81,5),true);drugi.k.lpmKlik();await sleep(700);
    await result('P-dwa-przywolania',await count('wolf')>=2,'dwóch graczy przywołuje wilki równocześnie',{wolves:await count('wolf')});
    await o.cmd('rg flag -w world __global__ pvp deny');await scene.prepare({target:false});await scene.targetBot();
    await o.cmd(`minecraft:damage ${o.name} 6`);await assign(k,o,'leczenie_dloni','lewa');
    const self0=await o.hp(o.name);await k.ppmTrzymaj(1000);const self1=await o.hp(o.name);
    await result('P-leczenie-kontrola',self1>self0+0.5,`leczenie siebie ${self0}→${self1}`,{heal:self1-self0});
    await scene.prepare({target:false});await scene.targetBot();
    await o.cmd('minecraft:damage MagCel 6');await assign(k,o,'uzdrowienie_innego','lewa');await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);
    const h0=await o.hp('MagCel');await k.ppmTrzymaj(900);const h1=await o.hp('MagCel');
    await result('P-leczenie-innego',h1>h0+0.5,`MagCel ${h0}→${h1}`,{heal:h1-h0});
    await scene.prepare({target:false});await assign(k,o,'plomienie','lewa');await o.cmd('minecraft:tp MagBlok 0 80 2');
    await o.cmd('minecraft:summon minecraft:husk 0 80 5 {NoAI:1b,Tags:["Ptarget"]}');await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);await sleep(200);
    const blocked0=await o.hp('@e[type=minecraft:husk,tag=Ptarget,limit=1]');await k.ppmTrzymaj(850);const blocked1=await o.hp('@e[type=minecraft:husk,tag=Ptarget,limit=1]');
    await o.cmd('minecraft:tp MagBlok 20 80 0');await scene.prepare({target:false});await assign(k,o,'plomienie','lewa');await o.cmd('minecraft:summon minecraft:husk 0 80 5 {NoAI:1b,Tags:["Ptarget"]}');await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);await sleep(200);
    const clear0=await o.hp('@e[type=minecraft:husk,tag=Ptarget,limit=1]');await k.ppmTrzymaj(850);const clear1=await o.hp('@e[type=minecraft:husk,tag=Ptarget,limit=1]');
    await result('P-zaslanianie',clear0-clear1>blocked0-blocked1,`husk z graczem na linii ${blocked0}→${blocked1}, bez gracza ${clear0}→${clear1}`,{blocked:blocked0-blocked1,clear:clear0-clear1});
    await scene.prepare({target:false});await assign(k,o,'plomienie','lewa');await o.cmd('minecraft:summon minecraft:husk -3 80 5 {NoAI:1b,Tags:["Pleft"]}');await o.cmd('minecraft:summon minecraft:husk 3 80 5 {NoAI:1b,Tags:["Pright"]}');
    await k.bot.lookAt(new Vec3(-3,80.8,5),true);k.ppmWcisnij();await sleep(600);await k.bot.lookAt(new Vec3(3,80.8,5),true);await sleep(600);k.ppmPusc();
    const left=await o.hp('@e[type=minecraft:husk,tag=Pleft,limit=1]'),right=await o.hp('@e[type=minecraft:husk,tag=Pright,limit=1]');
    await result('P-zmiana-celu',left<20&&right<20,`strumień po zmianie celownika: husk L=${left}, P=${right}`,{left,right});
  }finally{
    try{await o.cmd('rg flag -w world __global__ pvp deny');}catch{}
    for(const p of peers){p.k.close();p.bot.quit();}
  }
};
