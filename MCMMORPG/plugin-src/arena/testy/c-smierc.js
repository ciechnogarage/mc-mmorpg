'use strict';
const {scenariusz, sleep, enter, assertWorld, exp} = require('./common');
scenariusz('C-smierc', async ({w,polacz,rcon}) => {
  const a=await polacz({nick:'ArenaSmierc'}); await enter(rcon,a);
  await rcon('minecraft:give ArenaSmierc minecraft:diamond 1'); await sleep(500);
  const before=await exp(rcon,a);
  await rcon('minecraft:kill ArenaSmierc'); await sleep(4000);
  const after=await exp(rcon,a);
  w.sprawdz('odrodzenie-arena',assertWorld(a),`świat ${a.game.dimension}`);
  w.sprawdz('ekwipunek-zostal',a.inventory.items().some(i=>i.name==='diamond'),'diament po śmierci');
  w.sprawdz('exp-zostal',before===after && !before.includes('%mmocore_experience%'),`${before} -> ${after}`);
  const unluck=a.registry.effectsByName?.unluck;
  w.sprawdz('bez-pecha',!!unluck && !(unluck.id in (a.entity.effects||{})),`unluck id ${unluck?.id}; efekty ${Object.keys(a.entity.effects||{})}`);
}, {limitMs:120000});
