// Walidacja bajerow walki na HARD: enrage <30% (boss dostaje SPEED), boss killowalny, dungeon konczy.
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const HOST='127.0.0.1', PORT=25565, RPORT=25575, RPASS=process.env.RCON_PASS, USER='Dungeoneer';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=25000,st=400)=>{const t=Date.now();while(Date.now()-t<to){const v=await fn();if(v)return v;await sleep(st);}return false;};
let rcon, bot, lastHealth=null, lastEffects='', finish=false, enrageTitle=false;
const dimOf=async()=>(await rcon.send(`data get entity ${USER} Dimension`).catch(()=>'')).match(/"([^"]+)"/)?.[1];
const inInst=d=>d&&d.includes('level_1_');

(async()=>{
  rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
  bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
  bot.on('resourcePack',()=>bot.acceptResourcePack());
  bot.on('message',j=>{const t=j.toString();
    const m=t.match(/following entity data:\s*(.+)$/); if(m)lastRaw=m[1];
    if(/WSCIEKLOSC|szal/i.test(t))enrageTitle=true;
    if(/you completed|uko[nń]czy|left the dungeon/i.test(t))finish=true;});
  let lastRaw=null;
  if(!await waitFor(()=>bot.entity,60000,500)){console.log('NO-SPAWN');process.exit(7);}
  await rcon.send(`op ${USER}`); await sleep(700);
  await rcon.send(`gamerule sendCommandFeedback true`).catch(()=>{});
  let d0=await dimOf(); if(inInst(d0)){bot.chat('/md leave');await waitFor(async()=>!inInst(await dimOf()),15000,500);}

  bot.chat('/md play level_1:HARD');
  const inst=await waitFor(async()=>{const d=await dimOf();return inInst(d)?d:false;},20000,500);
  if(!inst){console.log('ENTER-FAIL');process.exit(3);}
  await rcon.send(`gamemode creative ${USER}`).catch(()=>{});
  await rcon.send(`effect give ${USER} minecraft:resistance 999 4 true`).catch(()=>{});
  await sleep(3500); // pozwol bossowi sie zeskalowac + intro

  const readHP=async()=>{lastRaw=null;bot.chat('/data get entity @e[type=minecraft:zombie,distance=..60,limit=1,sort=nearest] Health');await sleep(800);return lastRaw?parseFloat(lastRaw):null;};
  const readEff=async()=>{lastRaw=null;bot.chat('/data get entity @e[type=minecraft:zombie,distance=..60,limit=1,sort=nearest] active_effects');await sleep(800);return lastRaw||'';};

  let speedSeenWhileLow=false, hpTrace=[];
  for(let i=0;i<14 && !finish;i++){
    const d=await dimOf(); if(!inInst(d)){finish=true;break;}
    let hp=await readHP();
    if(hp!=null)hpTrace.push(hp);
    // zadaj obrazenia w kawalkach by przejsc przez prog 30% (228 z 760)
    bot.chat('/damage @e[type=minecraft:zombie,distance=..60,limit=1,sort=nearest] 110');
    await sleep(1400);
    hp=await readHP();
    if(hp!=null && hp>0 && hp<228){
      const eff=await readEff();
      if(/speed/i.test(eff))speedSeenWhileLow=true;
    }
  }
  await sleep(1500);
  const dimEnd=await dimOf();
  console.log('HP-trace:', hpTrace.map(Math.round).join('->'));
  console.log('enrage SPEED <30%:', speedSeenWhileLow?'TAK ✅':'NIE', '| enrage-title:', enrageTitle?'TAK':'(bot nie lapie titli)');
  console.log('FINISH (dungeon ukonczony):', (finish||!inInst(dimEnd))?'TAK ✅':'NIE ❌','| dimEnd=',dimEnd);
  bot.chat('/md leave'); await sleep(1500);
  try{await rcon.end();}catch{} try{bot.quit();}catch{}
  process.exit(0);
})().catch(e=>{console.error('FATAL',e.message);process.exit(1);});
