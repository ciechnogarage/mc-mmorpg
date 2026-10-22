'use strict';
const {sleep,Klient}=require('./klient');
const {Obserwator}=require('./obserwator');
const {Niezmienniki}=require('./niezmienniki');
const {until,assign}=require('./menu');
const weapons=new Set(['stone_sword','netherite_sword','bow']);
function rng(seed){let s=seed>>>0;return()=>{s^=s<<13;s^=s>>>17;s^=s<<5;return(s>>>0)/4294967296;};}
function counts(k){const out={stone_sword:0,netherite_sword:0,bow:0};
  for(let slot=9;slot<=45;slot++){if(slot===13||slot===37)continue;const item=k.bot.inventory.slots[slot];if(weapons.has(item?.name))out[item.name]+=item.count;}
  const item=k.bot.inventory.cursorItem;if(weapons.has(item?.name))out[item.name]+=item.count;
  return out;
}
async function run(seed,ctx){
  let {k,o,scene}=ctx;const rootK=k,{w,connect}=ctx;
  const random=rng(seed), errors=[],hits={};
  o.maxEvents=1500;o.compactItemPackets=true;
  await scene.prepare({target:false});
  const keep=await o.cmd('minecraft:gamerule keep_inventory');
  const keepOld=/false/i.test(keep)?'false':'true';
  await o.cmd('minecraft:gamerule keep_inventory true');
  const give=async(t,id)=>o.cmd(`mi give ${t} ${id} ${o.name} 1`);
  try{
    for(let i=0;i<3;i++)await give('SWORD','MIECZ_KAMIENNY');
    await give('GREATSWORD','MIECZ_RUNICZNY');await give('BOW','LUK_MYSLIWSKI');await sleep(300);
    await assign(k,o,'plomienie','lewa');
    await until(()=>{const c=counts(k);return c.stone_sword===3&&c.netherite_sword===1&&c.bow===1;},'pięć prawdziwych broni po /mi give',5000);
    const base=counts(k);
    const find=(name)=>k.bot.inventory.slots.findIndex((v,i)=>i>=14&&i<=44&&weapons.has(v?.name)&&(!name||v.name===name));
    const click=async(source,target,button=0,mode=0)=>{if(source<0)return;
      k.e();try{await k.bot.clickWindow(source,button,mode);if(target!==null)await k.bot.clickWindow(target,0,0);
        if(k.bot.inventory.cursorItem){await k.bot.clickWindow(source,0,0);}}
      finally{k.zamknij();}await sleep(110);};
  const actions=['assign','book','cursor','F','shift','number','spare','set','rejectBow','rejectTwo'];
    for(let step=1;step<=1000;step++){
      const pick=Math.floor(random()*100);
      const kind=step===160?'death':[250,500,750].includes(step)?'relog':pick<2?'book':pick<10?'assign':actions[2+Math.floor(random()*8)];
      hits[kind]=(hits[kind]||0)+1;
      try{
        if(k.bot.currentWindow)k.zamknij();
        switch(kind){
          case 'assign':await o.cmd(`zaklecia przypisz ${o.name} plomienie lewa`);break;
          case 'book':await assign(k,o,'plomienie','lewa');break;
          case 'cursor':await click(find('stone_sword'),45);break;
          case 'F':k.cyfra(1);k.f();await sleep(110);break;
          case 'shift':await click(find('stone_sword'),null,0,1);break;
          case 'number':{const at=find('stone_sword');if(at>=0){await click(at,41);await click(45,null,5,2);}break;}
          case 'spare':await click(12,null,1,0);break;
          case 'set':k.shift(true);k.f();await sleep(100);k.shift(false);break;
          case 'rejectBow':await o.cmd(`zaklecia przypisz ${o.name} plomienie lewa`);await click(find('bow'),45);break;
          case 'rejectTwo':await o.cmd(`zaklecia przypisz ${o.name} plomienie lewa`);await click(find('netherite_sword'),45);break;
          case 'relog':{await k.relog(1100);const bot=await connect(o.name),ob=new Obserwator(bot,o.rcon,o.name),kl=new Klient(bot,ob);
            ob.maxEvents=1500;ob.compactItemPackets=true;
            k=kl;o=ob;ctx.k=kl;ctx.o=ob;w.checker=new Niezmienniki(ob,kl,w);break;}
          case 'death':{let died=false;k.bot.once('death',()=>{died=true;});await o.cmd(`minecraft:kill ${o.name}`);await until(()=>died,'śmierć',6000);await sleep(1600);break;}
        }
        if(k.bot.currentWindow)k.zamknij();await sleep(80);
        if(JSON.stringify(counts(k))!==JSON.stringify(base))await until(()=>JSON.stringify(counts(k))===JSON.stringify(base),'powrót prawdziwych przedmiotów po '+kind,5000).catch(()=>{});
        const now=counts(k);if(JSON.stringify(now)!==JSON.stringify(base))errors.push({step,kind,type:'count',base,now});
        if(step%20===0||['F','shift','number','cursor','spare','set','rejectBow','rejectTwo','relog','death'].includes(kind)&&step%5===0){
          const inv=await w.checker.inventory();if(inv.bad.length||inv.errors.length)errors.push({step,kind,type:'sync',bad:inv.bad.slice(0,5),errors:inv.errors.slice(0,2)});
        }
      }catch(e){errors.push({step,kind,type:'exception',message:String(e).slice(0,180)});try{k.zamknij();k.shift(false);}catch{}}
      if(errors.length>=8)break;
      if(step%100===0)console.log(`B6FZ ${seed} ${step}/1000 errors=${errors.length}`);
    }
    if(k.bot.currentWindow)k.zamknij();await sleep(500);const end=counts(k),inv=await w.checker.inventory();
    const ok=errors.length===0&&JSON.stringify(base)===JSON.stringify(end)&&inv.bad.length===0&&inv.errors.length===0;
    w.add(`B6Fuzz-${seed}`,ok,`seed=${seed} kroki=${Object.values(hits).reduce((a,b)=>a+b,0)}/1000 błędy=${errors.length} real=${JSON.stringify(base)}→${JSON.stringify(end)} sync=${inv.bad.length}/${inv.errors.length}`,{hits,errors,base,end,inv});
  }finally{await o.cmd(`minecraft:gamerule keep_inventory ${keepOld}`);if(k!==rootK){try{k.close();k.bot.quit();}catch{}}}
}
module.exports={run};
