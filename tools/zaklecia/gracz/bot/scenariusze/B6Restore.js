'use strict';
module.exports=async({o,w})=>{
  await o.cmd('minecraft:gamerule keep_inventory false');
  const setFlag=await o.cmd('rg flag -w world __global__ pvp deny');
  const rule=await o.cmd('minecraft:gamerule keep_inventory');
  const info=await o.cmd('rg info -w world __global__');
  w.add('B6Restore',/false/i.test(rule)&&/DENY/i.test(info+setFlag),
    `keep_inventory=${rule}; setFlag=${setFlag.slice(0,180)}; info=${info.slice(0,180)}`,{rule,setFlag,info});
};
