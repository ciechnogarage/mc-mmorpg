// Definitywny test finiszu (robust). Bot wchodzi, creative (przezywa), zabija bossa szeroko i dlugo,
// finish wykrywany przez ZMIANE WYMIARU (leave:true -> teleport poza instancje) lub chat "completed/left".
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const HOST='127.0.0.1', PORT=25565, RPORT=25575, RPASS=process.env.RCON_PASS, USER='Dungeoneer';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=30000,st=250)=>{const t=Date.now();while(Date.now()-t<to){if(await fn())return true;await sleep(st);}return false;};
const dimOf=async()=>(await rcon.send(`data get entity ${USER} Dimension`).catch(()=>'')).match(/"([^"]+)"/)?.[1];
let rcon;
(async()=>{
  rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
  const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
  let finishChat=false;
  bot.on('resourcePack',()=>bot.acceptResourcePack());
  bot.on('message',j=>{const t=j.toString();if(/you completed|uko[nń]czy|left the dungeon|powr[oó]/i.test(t)){finishChat=true;console.log('[CHAT]',t);}});
  if(!await waitFor(()=>bot.entity,60000,500)){console.log('brak spawnu');process.exit(7);}
  await rcon.send(`op ${USER}`); await sleep(800);
  await rcon.send('say ===MARKER123===');

  bot.chat('/md play level_1'); await sleep(4500);
  await rcon.send(`gamemode creative ${USER}`).catch(()=>{});
  await sleep(2000);
  const dimIn=await dimOf();
  console.log('[bot] w instancji dim=',dimIn);
  if(!dimIn || !/level_1_/.test(dimIn)){console.log('[FATAL] nie wszedl do instancji');process.exit(2);}

  // zabijaj szeroko przez ~24s; finish = zmiana wymiaru poza instancje
  console.log('[step] tepie wszystkie hostile w 64 blokach, czekam na finish (zmiana wymiaru)');
  let dimNow=dimIn, finished=false;
  for(let i=0;i<16 && !finished;i++){
    bot.chat('/kill @e[type=#minecraft:skeletons,distance=..64]');
    bot.chat('/kill @e[type=minecraft:zombie,distance=..64]');
    await sleep(1500);
    dimNow=await dimOf();
    if(finishChat || (dimNow && dimNow!==dimIn)){finished=true;break;}
  }
  console.log('[wynik] dimIn=',dimIn,'dimPo=',dimNow,'| finishChat=',finishChat);
  console.log('\n[WERDYKT] FINISH (boss kill -> dungeon koniec):', finished?'TAK ✅':'NIE ❌');
  try{await rcon.end();}catch{} try{bot.quit();}catch{}
  process.exit(finished?0:1);
})().catch(e=>{console.error('FATAL',e);process.exit(1);});
