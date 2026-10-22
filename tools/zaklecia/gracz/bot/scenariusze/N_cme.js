'use strict';
const {Vec3}=require('vec3');
const {Klient,sleep}=require('../lib/klient');
const {assign}=require('../lib/menu');
const {Obserwator}=require('../lib/obserwator');

module.exports=async({o,scene,w,connect})=>{
  let caster,victim,ck,vk;
  try{
    await scene.prepare({target:false});
    caster=await connect('MagPX01');victim=await connect('MagPX05');
    const flag=await o.cmd('rg flag -w world __global__ pvp allow');
    const co=new Obserwator(caster,o.rcon,caster.username),vo=new Obserwator(victim,o.rcon,victim.username);
    ck=new Klient(caster,co);vk=new Klient(victim,vo);
    await assign(ck,co,'plomienie','lewa');await assign(vk,vo,'plomienie','lewa');
    await o.cmd('mmocore admin resource-mana set MagPX01 20');
    await o.cmd('mmocore admin resource-mana set MagPX05 20');
    await o.cmd('minecraft:tp MagPX01 0 80 0 0 0');
    await o.cmd('minecraft:tp MagPX05 0 80 2 180 0');
    await o.cmd('minecraft:attribute MagPX05 minecraft:max_health base set 4');
    await o.cmd('minecraft:effect clear MagPX05');
    await o.cmd('minecraft:effect give MagPX05 minecraft:instant_health 1 255 true');
    await sleep(5000); // ochrona po wejściu nowego gracza
    await caster.lookAt(new Vec3(0.5,80.8,2.5),true);
    await victim.lookAt(new Vec3(0.5,80.8,6.5),true);
    const h0=await o.hp('MagPX05'),m0=await co.mana();
    let dead=false;victim.on('death',()=>{dead=true;});
    await Promise.all([vk.ppmTrzymaj(2500),ck.ppmTrzymaj(2500)]);
    const h1=await o.hp('MagPX05'),m1=await co.mana();
    w.add('N_cme-zabicie',dead,`PvP: cel ${h0}→${h1} HP, mana ${m0}→${m1}`,{h0,h1,m0,m1,dead,flag});
    await sleep(300);
    const log=await o.cmd('minecraft:list uuids');
    w.add('N_cme-tik',true,'Tik po śmierci celu zakończył się; wyjątki sprawdza BLEDY-LOGU',{online:log.slice(0,100)});
  }finally{
    await o.cmd('minecraft:attribute MagPX05 minecraft:max_health base set 20').catch(()=>{});
    await o.cmd('rg flag -w world __global__ pvp deny').catch(()=>{});
    ck?.close();vk?.close();caster?.quit();victim?.quit();
  }
};
