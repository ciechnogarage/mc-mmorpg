'use strict';
const fs=require('node:fs');const path=require('node:path');
const {minimize}=require('../lib/fuzzer');
module.exports=async({k,o,scene,w,connect})=>{
  const trace=JSON.parse(fs.readFileSync(path.join(process.env.ZAK_WYNIKI||'/bot/wyniki/zaklecia','fuzz-65537.json'),'utf8')).trace.slice(0,23);
  const c={k,o,scene},fingerprint='inventory:14:minecraft:paper:2:minecraft:paper:1';
  try{
    const result=await minimize(c,trace,connect,w,fingerprint);
    w.add('FM-papier-slot14',result.reproduced,`slot14 papier 2:1, prefiks 23, minimalizacja ${result.trace.length} akcji, ${result.attempts} prób`,result);
  }finally{if(c.k!==k){c.k.close();c.k.bot.quit();}}
};
