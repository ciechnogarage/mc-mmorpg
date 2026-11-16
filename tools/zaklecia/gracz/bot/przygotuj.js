'use strict';
const {Obserwator}=require('./lib/obserwator');
(async()=>{const r=await Obserwator.connect();try{for(const c of [
  'minecraft:time set noon','minecraft:weather clear',
  'minecraft:fill -14 79 -14 14 79 14 minecraft:stone',
  'minecraft:fill -14 80 -14 14 85 14 minecraft:air',
  'minecraft:fill 0 80 7 3 83 7 minecraft:stone',
  'minecraft:spawnpoint @a 0 80 0'])console.log(c,await r.send(c));}
  finally{await r.end();}})().catch(e=>{console.error(e);process.exit(1)});
