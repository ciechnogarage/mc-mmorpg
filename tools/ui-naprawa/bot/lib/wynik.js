'use strict';
const fs=require('node:fs');const path=require('node:path');
class Wyniki {
  constructor(group){this.group=group;this.rows=[];}
  add(id,ok,detail,metrics={},limits={}){
    const meets=Object.entries(limits).every(([key,rule])=>{
      const m=String(rule).match(/^(≤|≥|<|>)\s*(\d+(?:[.,]\d+)?)/);if(!m)return true;
      const value=Number(metrics[key]),limit=Number(m[2].replace(',','.'));if(metrics[key]==null||!Number.isFinite(value))return false;
      return m[1]==='≤'?value<=limit:m[1]==='≥'?value>=limit:m[1]==='<'?value<limit:value>limit;
    });
    const status=ok?'PASS':'FAIL';const row={id,status,detail,FEEL:metrics,progi:limits,feelOK:meets};this.rows.push(row);
    console.log(`${status} ${id} ${detail}`);
    for(const [k,v] of Object.entries(metrics))console.log(`FEEL ${id} ${k}=${v} (próg ${limits[k]??'obserwacja'})`);
  }
  blad(id,e){const row={id,status:'BLAD',detail:String(e?.stack||e),FEEL:{blad:1},progi:{blad:0}};this.rows.push(row);console.log(`BLAD ${id} ${row.detail}`);}
  skip(id,detail){this.rows.push({id,status:'SKIP',detail,FEEL:{},progi:{}});console.log(`SKIP ${id} ${detail}`);}
  async step(id,fn){try{await fn();}catch(e){if(/Not connected|ECONNREFUSED|Server closed|Timeout for packet/i.test(String(e)))throw e;this.blad(id,e);}}
  finish(){const result={group:this.group,at:new Date().toISOString(),rows:this.rows};const dir=path.join(__dirname,'..','wyniki');fs.mkdirSync(dir,{recursive:true});fs.writeFileSync(path.join(dir,`${this.group}.json`),JSON.stringify(result,null,2)+'\n');console.log('JSON '+JSON.stringify(result));return result;}
}
module.exports={Wyniki};
