'use strict';
const {scenariusz,sleep,plain,pair,pos} = require('./common');
scenariusz('F-manekin',async ({w,polacz,rcon})=>{
 const {a,b}=await pair(polacz,rcon);
 await pos(rcon,a,35,65,0); await pos(rcon,b,36,65,0);
 let barsA=0,barsB=0; a.on('actionBar',m=>{if(plain(m).includes('DPS'))barsA++;}); b.on('actionBar',m=>{if(plain(m).includes('DPS'))barsB++;});
 const dummy=Object.values(a.entities).find(e=>e.name==='iron_golem' && e.position.distanceTo(a.entity.position)<6);
 w.sprawdz('trzy-manekiny',Object.values(a.entities).filter(e=>e.name==='iron_golem'&&e.position.distanceTo(a.entity.position)<12).length>=3,'manekiny w strefie');
 if(!dummy){w.sprawdz('cel',false,'brak manekina');return;}
 const start=dummy.position.clone();
 for(let i=0;i<5;i++){a.attack(dummy); await sleep(250);}
 w.sprawdz('pasek-tylko-napastnika',barsA>0 && barsB===0,`A ${barsA}, B ${barsB}`);
 await sleep(5500);
 const next=Object.values(a.entities).find(e=>e.id===dummy.id);
 w.sprawdz('manekin-zyje-i-stoi',!!next && next.position.distanceTo(start)<1,`odległość ${next?.position.distanceTo(start)}`);
}, {limitMs:120000});
