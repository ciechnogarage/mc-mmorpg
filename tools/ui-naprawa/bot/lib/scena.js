'use strict';
const {sleep}=require('./klient');
const ARENA={x:0,y:81,z:0};
class Scena {
  constructor(o){this.o=o;this.name=o.name;}
  async prepare({target=true,mana=true}={}){
    const c=s=>this.o.cmd(s);
    await c('minecraft:time set noon');await c('minecraft:weather clear');
    await c('minecraft:fill -14 79 -14 14 79 14 minecraft:stone');
    await c('minecraft:fill -14 80 -14 14 85 14 minecraft:air');
    await c('minecraft:fill 0 80 7 3 83 7 minecraft:stone');
    await c(`minecraft:tp ${this.name} 0 80 0 0 0`);
    await c(`minecraft:gamemode survival ${this.name}`);
    // Zachowujemy przyciski gracza w slotach 8 i 18 (Dziennik/Sakwa).
    // Pełne /clear kasowało je, a skrypt CoreTools odtwarza je dopiero po relogu.
    for(const item of ['debug_stick','shield','bread','potion','bow','iron_pickaxe','stone','stick','iron_sword','stone_sword'])await c(`minecraft:clear ${this.name} minecraft:${item}`);
    await c(`minecraft:effect clear ${this.name}`);
    if(mana){const max=await this.o.maxMana();await c(`mmocore admin resource-mana set ${this.name} ${max!==null&&max>0?max:20}`);}
    await c('minecraft:execute in minecraft:overworld positioned 0 80 0 run minecraft:kill @e[type=!minecraft:player,distance=..24]');
    if(target)await c('minecraft:summon minecraft:husk 0 80 5 {NoAI:1b,PersistenceRequired:1b}');
    await sleep(500);
  }
  async targetBot(){return this.o.cmd('minecraft:tp MagCel 0 80 5 180 0');}
  async cleanup(){
    const c=s=>this.o.cmd(s);
    await c('minecraft:execute in minecraft:overworld positioned 0 80 0 run minecraft:kill @e[type=!minecraft:player,distance=..24]');
    await c(`minecraft:effect clear ${this.name}`);
    for(const item of ['debug_stick','shield','bread','potion','bow','iron_pickaxe','stone','stick','iron_sword','stone_sword'])await c(`minecraft:clear ${this.name} minecraft:${item}`);
  }
}
module.exports={Scena,ARENA};
