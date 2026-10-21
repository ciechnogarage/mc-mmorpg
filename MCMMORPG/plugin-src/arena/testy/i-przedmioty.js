'use strict';
const {scenariusz,sleep,enter,assertWorld,pos}=require('./common');
scenariusz('I-przedmioty',async ({w,polacz,rcon})=>{
 const a=await polacz({nick:'ArenaItem'}); await enter(rcon,a);
 await rcon('minecraft:give ArenaItem minecraft:stone 1'); await sleep(500);
 const stack=a.inventory.items().find(i=>i.name==='stone');
 w.sprawdz('przedmiot-testowy',!!stack,'kamień w ekwipunku');
 if(stack){try{await a.tossStack(stack);}catch(_){} await sleep(400);}
 w.sprawdz('wyrzucenie-zablokowane',a.inventory.items().some(i=>i.name==='stone'),'kamień nadal w ekwipunku');
 await rcon('minecraft:execute in minecraft:world run minecraft:teleport ArenaItem 0 80 0'); await sleep(500);
 w.sprawdz('komenda-nie-wychodzi',assertWorld(a),`świat ${a.game.dimension}`);
}, {limitMs:120000});
