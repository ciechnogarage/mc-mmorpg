'use strict';
const {scenariusz,sleep,pair,pos,attack,assertWorld}=require('./common');
scenariusz('J-relog',async ({w,polacz,rcon})=>{
 const {a,b}=await pair(polacz,rcon);
 await pos(rcon,a,0,65,0); await pos(rcon,b,1,65,0); await sleep(3200); await attack(a,b);
 b.quit(); await sleep(1000);
 const again=await polacz({nick:'ArenaB'}); await sleep(1000);
 w.sprawdz('relog-na-odrodzeniu',assertWorld(again)&&again.entity.position.z<-24,`pozycja ${again.entity.position}`);
 await pos(rcon,again,0,65,0); await sleep(1200);
 w.sprawdz('kara-poddania',again.entity.position.z<-24,`pozycja ${again.entity.position}`);
}, {limitMs:120000});
