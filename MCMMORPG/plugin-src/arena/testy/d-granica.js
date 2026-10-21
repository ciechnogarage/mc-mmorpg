'use strict';
const {scenariusz,sleep,pair,pos,attack} = require('./common');
scenariusz('D-granica',async ({w,polacz,rcon})=>{
 const {a,b}=await pair(polacz,rcon); await sleep(3200);
 await pos(rcon,a,0,65,0); await pos(rcon,b,0,65,-27);
 const hp=b.health; await attack(a,b);
 w.sprawdz('ring-do-trybun',b.health===hp,`HP ${hp} -> ${b.health}`);
 await pos(rcon,a,0,65,-27); await pos(rcon,b,0,65,0);
 const hp2=b.health; await attack(a,b);
 w.sprawdz('trybuny-do-ringu',b.health===hp2,`HP ${hp2} -> ${b.health}`);
 await pos(rcon,a,0,65,0); await sleep(3200); await attack(a,b);
 w.sprawdz('ring-do-ringu',b.health<hp2,`HP ${hp2} -> ${b.health}`);
}, {limitMs:120000});
