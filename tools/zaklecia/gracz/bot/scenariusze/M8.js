'use strict';
const {Vec3}=require('vec3');const {sleep}=require('../lib/klient');const {until}=require('../lib/menu');
module.exports=async({k,o,scene,w,connect})=>{
  let target;
  try{
    target=await connect('MagCel');await scene.prepare({target:false});await scene.targetBot();await o.cmd('minecraft:tp MagCel 0 80 2');
    const flag=await o.cmd('rg flag -w world __global__ pvp allow');console.log('M8-WG '+flag);
    console.log('M8-WG-REGION '+(await o.cmd('rg info -w world __global__')).slice(0,1000));
    await o.cmd(`minecraft:item replace entity ${o.name} hotbar.6 with minecraft:stone_sword`);k.cyfra(7);
    await until(async()=>/stone_sword/.test(await o.selected())?true:null,'miecz M8',2000);
    await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(1000);
    await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');await sleep(300);
    const hp0=await o.hp('MagCel'),t=k.target(),s=o.snapshot();k.lpmKlik();await sleep(600);const hp1=await o.hp('MagCel');
    const events=o.events.slice(s.index).filter(e=>/chat|sound/.test(e.packet));
    w.add('M8-miecz-vanilla-PvP',hp1<hp0,`czysty miecz przeciw MagCel ${hp0}→${hp1}`,{hpDelta:hp0-hp1,target:t.type,distance:t.distance,events:events.map(e=>({packet:e.packet,text:JSON.stringify(e.data).slice(0,250)}))});
    await o.cmd(`mmocore admin attribute give ${o.name} bieglosc-miecz 1`);
    await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);
    const slot=await until(()=>{const n=k.bot.inventory.slots.findIndex((x,i)=>i>=9&&i<=44&&x?.name==='stone_sword'&&i!==42);return n<0?null:n},'MIECZ_KAMIENNY',2500);
    if(slot<36){k.e();await k.bot.clickWindow(slot,0,0);await k.bot.clickWindow(37,0,0);k.zamknij();k.cyfra(2);}else k.cyfra(slot-35);
    await sleep(1000);await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');await sleep(300);
    const hp2=await o.hp('MagCel'),selected=await o.selected();k.lpmKlik();await sleep(600);const hp3=await o.hp('MagCel');
    w.add('M8-miecz-MMO-PvP',hp3<hp2,`MIECZ_KAMIENNY przeciw MagCel ${hp2}→${hp3}`,{hpDelta:hp2-hp3,selected:selected.slice(0,130)});
  }finally{try{await o.cmd('rg flag -w world __global__ pvp');}catch{}target?.quit();}
};
