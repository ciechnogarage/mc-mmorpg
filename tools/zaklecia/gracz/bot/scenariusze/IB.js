'use strict';
const {assign}=require('../lib/menu');const {sleep}=require('../lib/klient');
const {closesBar}=require('../lib/niezmienniki');
module.exports=async({k,o,scene,w})=>{
  await scene.prepare({target:false});await assign(k,o,'plomienie','lewa');
  const s=o.snapshot();await k.shiftQ();await sleep(500);
  const before=o.events.filter(e=>e.packet==='boss_bar'&&e.at>=s.at),magic=new Set(before.filter(e=>/✦|many\/s/.test(JSON.stringify(e.data.title||''))).map(e=>e.data.entityUUID));
  k.q();await sleep(1200);
  const after=o.events.filter(e=>e.packet==='boss_bar'&&e.at>=k.barClosedAt),removed=new Set(after.filter(closesBar).map(e=>e.data.entityUUID));
  const missing=[...magic].filter(id=>!removed.has(id));
  w.add('IB-BossBar-po-Q',missing.length===0,`otwarte ${magic.size}, REMOVE ${removed.size}, brak ${missing.length}`,{magic:[...magic],removed:[...removed],missing,packetsAfter:after.length,actions:after.reduce((a,e)=>(a[e.data.action]=(a[e.data.action]||0)+1,a),{})});
  await w.checker.check('IB-BossBar-po-Q');
};
