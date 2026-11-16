'use strict';
const {Vec3}=require('vec3');const {Klient,sleep}=require('../lib/klient');const {Obserwator}=require('../lib/obserwator');const {assign,until}=require('../lib/menu');
module.exports=async({k,o,scene,w,connect})=>{
  let other,observer;
  try{
    other=await connect('MagWspol');observer=await connect('MagCel');
    const oo=new Obserwator(other,o.rcon,other.username),ko=new Klient(other,oo);
    await scene.prepare({target:false});await o.cmd('minecraft:tp MagWspol -4 80 0');await o.cmd('minecraft:tp MagCel 9 80 0');
    await assign(k,o,'duch_wilka','prawa');await assign(ko,oo,'duch_wilka','prawa');
    const m0=await o.mana(),n0=await oo.mana();
    await k.bot.lookAt(new Vec3(0,81,5),true);await other.lookAt(new Vec3(-4,81,5),true);
    k.lpmKlik();await sleep(150);ko.lpmKlik();
    const count=async()=>{const s=await o.cmd('minecraft:execute in minecraft:overworld positioned 0 80 0 if entity @e[type=minecraft:wolf,distance=..18]');return Number(s.match(/Count:\s*(\d+)/)?.[1]||0);};
    await until(async()=>await count()>=2,'dwa wilki',5000).catch(()=>{});
    const wolves=await count(),m1=await o.mana(),n1=await oo.mana();
    w.add('PS-dwa-wilki',wolves>=2&&m1<m0&&n1<n0,`równoczesne przywołania: ${wolves} wilki, mana ${m0}→${m1} i ${n0}→${n1}`,{wolves,manaA:m0-m1,manaB:n0-n1});
    await w.checker.check('PS-dwa-wilki');ko.close();
  }finally{other?.quit();observer?.quit();}
};
