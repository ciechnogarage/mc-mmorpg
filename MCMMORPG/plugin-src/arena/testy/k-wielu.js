'use strict';
const {scenariusz,sleep,plain,pair,pos}=require('./common');
scenariusz('K-wielu',async ({w,polacz,rcon})=>{
 const {a,b}=await pair(polacz,rcon); await pos(rcon,a,35,65,0); await pos(rcon,b,36,65,0);
 const target=Object.values(a.entities).find(e=>e.name==='iron_golem'&&e.position.distanceTo(a.entity.position)<6);
 let x=0,y=0,lastA='',lastB='';
 a.on('actionBar',m=>{const t=plain(m);if(t.includes('DPS')){x++;lastA=t;}});
 b.on('actionBar',m=>{const t=plain(m);if(t.includes('DPS')){y++;lastB=t;}});
 if(!target){w.sprawdz('cel',false,'brak manekina');return;}
 for(let i=0;i<8;i++){a.attack(target);if(i<2)b.attack(b.entities[target.id]||target);await sleep(300);}
 w.sprawdz('obaj-maja-licznik',x>0&&y>0,`A ${x}, B ${y}`);
 const count=t=>Number(/Trafienia (\d+)/.exec(t)?.[1]||0);
 w.sprawdz('liczniki-osobne',count(lastA)>count(lastB),`A: ${lastA}; B: ${lastB}`);
 w.sprawdz('brak-rozlaczen',!a.powodRozlaczenia&&!b.powodRozlaczenia,`A ${a.powodRozlaczenia}, B ${b.powodRozlaczenia}`);
}, {limitMs:120000});
