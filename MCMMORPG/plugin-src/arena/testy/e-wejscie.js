'use strict';
const {scenariusz,sleep,assertWorld,enter,pos} = require('./common');
scenariusz('E-wejscie',async ({w,polacz,rcon})=>{
 const a=await polacz({nick:'ArenaWejscie'});
 await rcon('minecraft:execute in minecraft:creator_limbo run minecraft:teleport ArenaWejscie 0 70 0'); await sleep(500);
 w.sprawdz('cel-kreator-osiagniety',String(a.game.dimension).includes('creator_limbo'),`świat ${a.game.dimension}`);
 await rcon('arena wejdz ArenaWejscie'); await sleep(500);
 w.sprawdz('kreator-odmowa',!assertWorld(a),`świat ${a.game.dimension}`);
 await rcon('minecraft:execute in minecraft:world run minecraft:teleport ArenaWejscie 0 100 0'); await sleep(500);
 w.sprawdz('cel-w-powietrzu',a.entity.position.y>90,`pozycja ${a.entity.position}`);
 await rcon('arena wejdz ArenaWejscie'); await sleep(500);
 w.sprawdz('spadanie-odmowa',!assertWorld(a),`świat ${a.game.dimension}`);
}, {limitMs:120000});
