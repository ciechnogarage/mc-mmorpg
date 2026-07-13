// Boss zabity REALNYMI obrazeniami przez bot.chat /damage (jedyny dzialajacy kanal + jedyne co pali MythicMobDeath).
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const HOST='127.0.0.1', PORT=25565, RPORT=25575, RPASS=process.env.RCON_PASS, USER='Dungeoneer';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=30000,st=250)=>{const t=Date.now();while(Date.now()-t<to){const v=await fn();if(v)return v;await sleep(st);}return false;};
let rcon;
const dimOf=async()=>(await rcon.send(`data get entity ${USER} Dimension`).catch(()=>'')).match(/"([^"]+)"/)?.[1];
(async()=>{
  rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
  const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
  let bossStill=false, finishChat=false, dmgFeedback=[];
  bot.on('resourcePack',()=>bot.acceptResourcePack());
  bot.on('message',j=>{const t=j.toString();
    if(/BOSS-ALIVE/.test(t))bossStill=true;
    if(/damage|obraz/i.test(t)&&!/❤/.test(t))dmgFeedback.push(t.slice(0,80));
    if(/you completed|uko[nń]czy|left the dungeon/i.test(t)){finishChat=true;console.log('[FINISH]',t);}});
  if(!await waitFor(()=>bot.entity,60000,500)){console.log('brak spawnu');process.exit(7);}
  await rcon.send(`op ${USER}`); await sleep(800);
  await rcon.send('say ===V5MARK===');
  bot.chat('/md play level_1'); await sleep(4500);
  await rcon.send(`gamemode creative ${USER}`).catch(()=>{});
  const dimIn=await dimOf();
  console.log('[bot] instancja=',dimIn);

  // zadaj realne obrazenia bossowi az finish (zmiana wymiaru) lub wyczerpanie prob
  console.log('[step] bot /damage @e[zombie] 1000 (real) w petli');
  let finished=false;
  for(let i=0;i<14 && !finished;i++){
    bot.chat('/damage @e[type=minecraft:zombie,distance=..40,limit=1,sort=nearest] 1000');
    await sleep(1300);
    const d=await dimOf();
    if(finishChat || (d&&d!==dimIn)){finished=true;break;}
  }
  // sprawdz czy boss jeszcze zyje
  bossStill=false;
  bot.chat('/execute if entity @e[type=minecraft:zombie,distance=..40] run say BOSS-ALIVE');
  await sleep(1500);
  const dimPo=await dimOf();
  console.log('[dmg-feedback]', dmgFeedback.slice(0,4).join(' | ')||'(brak)');
  console.log('[po damage] boss zyje?', bossStill, '| dimIn=',dimIn,'dimPo=',dimPo,'finishChat=',finishChat);
  console.log('\n[WERDYKT] boss zabity:', !bossStill?'TAK':'NIE', '| FINISH:', finished?'TAK ✅':'NIE ❌');
  bot.chat('/md leave'); await sleep(1500);
  try{await rcon.end();}catch{} try{bot.quit();}catch{}
  process.exit(0);
})().catch(e=>{console.error('FATAL',e);process.exit(1);});
