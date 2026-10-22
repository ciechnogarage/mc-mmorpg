'use strict';
const fs=require('node:fs');const path=require('node:path');
const {Klient,sleep}=require('./klient');const {Obserwator}=require('./obserwator');const {Scena}=require('./scena');const {Niezmienniki}=require('./niezmienniki');
const {assign,until}=require('./menu');
function random(seed){let x=seed>>>0;return()=>{x^=x<<13;x^=x>>>17;x^=x<<5;return(x>>>0)/4294967296;};}
const ACTIONS=['ppm','lpm','obie','podwojne','kolko','cyfra','f','q','shiftq','e','ruch','skok','sprint','skradanie','smierc','relog','przedmiot','bron_lewa'];
function fingerprint(row){
  const d=row?.inventoryDifferences?.[0];
  if(d)return `inventory:${d.slot}:${d.server?.id||'-'}:${d.server?.count||0}:${d.client?.id||'-'}:${d.client?.count||0}`;
  if(row?.violations?.some(x=>x.startsWith('BossBar')))return 'bossbar';
  if(row?.violations?.some(x=>x.startsWith('mana')))return 'mana';
  if(row?.log?.length)return 'log-Zaklec';
  return null;
}
async function prepare(c){
  await c.scene.prepare();await assign(c.k,c.o,'plomienie','lewa');await assign(c.k,c.o,'odmrozenie','prawa');
  await c.o.cmd(`minecraft:give ${c.o.name} minecraft:stone 1`);
  await c.k.bot.lookAt(new (require('vec3').Vec3)(0.5,80.8,5.5),true);await sleep(200);
}
async function action(c,a,connect,w){
  const {k,o}=c;const bot=k.bot;
  switch(a.kind){
    case 'ppm':await k.ppmTrzymaj(260);break;
    case 'lpm':k.lpmKlik();await sleep(180);break;
    case 'obie':await k.obie(320);break;
    case 'podwojne':k.lpmKlik();await sleep(200);k.lpmKlik();break;
    case 'kolko':k.kolko(a.arg?1:-1);break;
    case 'cyfra':k.cyfra(1+a.arg%9);break;
    case 'f':k.f();break;
    case 'q':k.q();await sleep(160);if(bot.currentWindow)k.zamknij();break;
    case 'shiftq':await k.shiftQ();await sleep(150);k.kolko(a.arg?1:-1);k.ppmWcisnij();k.ppmPusc();k.q();break;
    case 'e':if(bot.currentWindow){k.zamknij();await sleep(500);break;}k.e();try{await bot.clickWindow(36+(a.arg%9),0,0);await bot.clickWindow(36+(a.arg%9),0,0);}finally{k.zamknij();}await sleep(500);break;
    case 'ruch':k.w(true);await sleep(200);k.w(false);break;
    case 'skok':k.skok(true);await sleep(200);k.skok(false);break;
    case 'sprint':k.sprint(true);k.w(true);await sleep(200);k.w(false);k.sprint(false);break;
    case 'skradanie':k.shift(true);await sleep(200);k.shift(false);break;
    case 'przedmiot':k.cyfra(a.arg%2?1:7);break;
    case 'bron_lewa':{
      if(bot.currentWindow||k.screen){k.zamknij();await sleep(120);}
      await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);
      const slot=await until(()=>{const n=bot.inventory.slots.findIndex((x,i)=>i>=9&&i<=44&&/sword|cutlass/i.test(`${x?.name||''} ${x?.displayName||''}`));return n<0?null:n;},'broń MMOItems do lewej',2000);
      k.e();try{await bot.clickWindow(slot,0,0);await bot.clickWindow(45,0,0);}finally{k.zamknij();}
      await sleep(250);break;
    }
    case 'smierc':{
      await o.cmd('minecraft:setblock 0 111 0 minecraft:stone');await o.cmd(`minecraft:tp ${o.name} 0.5 112 0.5 0 0`);
      await until(()=>bot.entity.position.y>110,'wieża fuzz',4000);let died=false;bot.once('death',()=>{died=true;});
      k.w(true);await until(()=>bot.entity.position.z>1.6,'krawędź wieży fuzz',5000).catch(()=>{});k.w(false);
      await until(()=>died,'upadek fuzz',8000);await o.cmd('minecraft:setblock 0 111 0 minecraft:air');await sleep(1500);break;
    }
    case 'relog':{
      await k.relog(1200);const next=await connect(o.name),ob=new Obserwator(next,o.rcon,o.name),kl=new Klient(next,ob);
      ob.maxEvents=1500;ob.compactItemPackets=true;c.k=kl;c.o=ob;c.scene=new Scena(ob);const checker=new Niezmienniki(ob,kl,w);checker.rows=w.checker?.rows||[];checker.fastInventory=true;w.checker=checker;break;
    }
    default:throw Error(`akcja ${a.kind}`);
  }
  await sleep(100);
}
async function replay(c,trace,connect,w,expected){
  const name=c.o.name,rcon=c.o.rcon,history=w.checker.rows;
  c.k.close();c.k.bot.quit();await sleep(1200);
  const bot=await connect(name),ob=new Obserwator(bot,rcon,name),kl=new Klient(bot,ob);
  ob.maxEvents=1500;ob.compactItemPackets=true;c.k=kl;c.o=ob;c.scene=new Scena(ob);w.checker=new Niezmienniki(ob,kl,w);w.checker.rows=history;w.checker.fastInventory=true;
  const expectedSlot=Number(expected.match(/^inventory:(-?\d+):/)?.[1]);
  if(Number.isFinite(expectedSlot))w.checker.prioritySlots=[expectedSlot];
  await prepare(c);const start=w.checker.rows.length,rows=w.rows.length;
  let failed=false;
  for(const a of trace){
    try{await action(c,a,connect,w);const row=await w.checker.check(`minimalizacja-${a.kind}`);if(fingerprint(row)===expected){failed=true;break;}}
    catch(e){if(expected===`blad:${e.message}`){failed=true;break;}}
  }
  w.checker.rows.splice(start);w.rows.splice(rows);return failed;
}
async function minimize(c,trace,connect,w,expected){
  let best=trace.slice(),n=2,attempts=0;
  let reproduced=false;
  try{reproduced=await replay(c,best,connect,w,expected);}catch(e){return {trace:best,attempts:1,reproduced:false,error:e.message};}
  if(!reproduced)return {trace:best,attempts:1,reproduced:false};
  while(best.length>1&&attempts<6){
    const width=Math.ceil(best.length/n);let changed=false;
    for(let i=0;i<best.length&&attempts<6;i+=width){
      const candidate=best.filter((_,j)=>j<i||j>=i+width);if(!candidate.length)continue;
      attempts++;let matched=false;try{matched=await replay(c,candidate,connect,w,expected);}catch{}
      if(matched){best=candidate;n=Math.max(2,n-1);changed=true;break;}
    }
    if(!changed){if(n>=best.length)break;n=Math.min(best.length,n*2);}
  }
  return {trace:best,attempts:attempts+1,reproduced:true};
}
async function runFuzz(seed,context,steps=300){
  const {k,o,scene,w,connect}=context;
  const c={k,o,scene};const rand=random(seed),trace=[];let violation=null,mini=null;
  const signatures=new Map(),minimized=[],paperPackets=[],divergencePackets=[];
  try{
    o.maxEvents=1500;o.compactItemPackets=true;await prepare(c);w.checker.fastInventory=true;const hp0=await o.hp('@e[type=minecraft:husk,limit=1,sort=nearest]'),mana0=await o.mana();
    await c.k.ppmTrzymaj(650);const hp1=await o.hp('@e[type=minecraft:husk,limit=1,sort=nearest]'),mana1=await o.mana();
    w.add(`FZ-${seed}-kontrola`,hp1<hp0&&mana1<mana0,`kontrola zaklęcia HP ${hp0}→${hp1}, mana ${mana0}→${mana1}`,{hpDelta:hp0-hp1,manaDelta:mana0-mana1});
    for(let i=0;i<steps;i++){
      const kind=i===110?'smierc':i===210?'relog':ACTIONS[Math.floor(rand()*ACTIONS.length)];
      const a={kind,arg:Math.floor(rand()*9)};trace.push(a);const id=`FZ-${seed}-${String(i+1).padStart(3,'0')}-${kind}`;
      w.checker.prioritySlots=kind==='bron_lewa'||kind==='f'?[ -106 ]:kind==='e'?[14,15]:[];
      const before=w.rows.length,cast=['ppm','lpm','obie','podwojne'].includes(kind),manaBefore=cast?await c.o.mana():null;
      await w.step(id,()=>action(c,a,connect,w),false);
      const inspect=(i+1)%5===0||['bron_lewa','smierc','relog','e','shiftq','f'].includes(kind);
      let latest=null;
      if(inspect)try{latest=await w.checker.check(id);}catch(e){w.blad(`INV-${id}`,e);}
      if(paperPackets.length<30&&latest?.inventoryDifferences?.some(x=>x.slot===14||x.slot===15)){
        const packets=c.o.events.filter(e=>['set_slot','window_items','open_window','close_window'].includes(e.packet)).slice(-12)
          .map(e=>({packet:e.packet,windowId:e.data.windowId??e.data.window,slot:e.data.slot,
            itemCount:e.data.item?.itemCount??e.data.item?.count,itemId:e.data.item?.itemId,
            stateId:e.data.stateId,at:e.at}));
        paperPackets.push({step:i+1,action:a,differences:latest.inventoryDifferences.filter(x=>x.slot===14||x.slot===15),packets});
      }
      if(divergencePackets.length<5&&latest?.inventoryDifferences?.some(x=>x.slot===0)){
        const packets=c.o.events.filter(e=>['set_slot','window_items','open_window','close_window','held_item_slot'].includes(e.packet)).slice(-35)
          .map(e=>({packet:e.packet,windowId:e.data.windowId??e.data.window,slot:e.data.slot,
            itemCount:e.data.item?.itemCount??e.data.item?.count,itemId:e.data.item?.itemId,
            stateId:e.data.stateId,at:e.at}));
        divergencePackets.push({step:i+1,action:a,differences:latest.inventoryDifferences.filter(x=>x.slot===0),packets});
      }
      if(cast&&manaBefore!==null&&latest&&latest.mana!==null&&latest.mana>manaBefore+0.3)
        w.add(`INV-${id}-mana`,false,`mana wzrosła podczas ${kind}: ${manaBefore}→${latest.mana}`);
      const failed=w.rows.slice(before).find(x=>x.status==='BLAD'||x.status==='FAIL');
      if(failed||latest?.violations?.length){
        const entry={index:i+1,id,fingerprint:fingerprint(latest)||`blad:${failed?.detail?.split('\n')[0]||'nieznany'}`};
        if(!violation)violation=entry;
        if(!signatures.has(entry.fingerprint))signatures.set(entry.fingerprint,entry);
      }
      if((i+1)%50===0)console.log(`FZ-PROGRESS ${seed} ${i+1}/${steps} sygnatury=${signatures.size}`);
    }
    if(context.finalInventoryCheck){
      if(c.k.bot.currentWindow)c.k.zamknij();
      await sleep(350);
      if(c.k.bot.currentWindow){c.k.zamknij();await sleep(350);}
      w.checker.fastInventory=false;
      await w.checker.check(`FZ-${seed}-po-zamknieciu-GUI`);
    }
    for(const entry of (context.skipMinimize?[]:[...signatures.values()].slice(0,2))){
      if(entry.index>120){minimized.push({violation:entry,mini:{trace:[],attempts:0,reproduced:false,reason:'dlugi prefiks; pelny slad w trace'}});continue;}
      let result;try{result=await minimize(c,trace.slice(0,entry.index),connect,w,entry.fingerprint);}
      catch(e){result={trace:trace.slice(0,entry.index),attempts:0,reproduced:false,error:e.message};}
      minimized.push({violation:entry,mini:result});
    }
    mini=minimized[0]?.mini||null;
    w.add(`FZ-${seed}-wynik`,!violation,`ziarno ${seed}; kroki ${trace.length}/${steps}; sygnatury ${signatures.size}; pierwsze naruszenie ${violation?.id||'brak'}`,{seed,steps:trace.length,minimized:minimized.map(x=>({fingerprint:x.violation.fingerprint,trace:x.mini.reproduced?x.mini.trace:[],attempts:x.mini.attempts,reproduced:x.mini.reproduced}))});
    const file=path.join(process.env.ZAK_WYNIKI||'/bot/wyniki/zaklecia',`fuzz-${seed}.json`);fs.writeFileSync(file,JSON.stringify({seed,trace,violation,mini,minimized,paperPackets,divergencePackets},null,2)+'\n');
  }finally{if(c.k!==k){c.k.close();c.k.bot.quit();}}
}
module.exports={runFuzz,random,ACTIONS,prepare,action,fingerprint,minimize};
