'use strict';
const {scenariusz, sleep, pair, pos, attack, exp} = require('./common');
scenariusz('B-farma', async ({w,polacz,rcon}) => {
  const {a,b}=await pair(polacz,rcon);
  const before=await exp(rcon,a);
  await pos(rcon,a,0,65,0); await pos(rcon,b,1,65,0); await sleep(3200);
  for(let i=0;i<10;i++) await attack(a,b);
  const after=await exp(rcon,a);
  w.sprawdz('exp-bez-zysku', before===after && !before.includes('%mmocore_experience%'), `${before} -> ${after}`);
  await pos(rcon,a,35,65,0); await sleep(500);
  const dummy=Object.values(a.entities).find(e=>e.name==='iron_golem' && e.position.distanceTo(a.entity.position)<6);
  w.sprawdz('manekin-obecny',!!dummy, dummy?.name || 'brak');
  if(dummy) for(let i=0;i<5;i++){a.attack(dummy);await sleep(500);}
  const dummyAfter=await exp(rcon,a);
  w.sprawdz('manekin-bez-exp',dummyAfter===after,`${after} -> ${dummyAfter}`);
}, {limitMs:120000});
