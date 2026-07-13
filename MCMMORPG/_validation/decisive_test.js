// Decydujacy test: kill bossa przez RCON-konsole (omija AllowCommands:false dungeona),
// weryfikacja percepcja bota PRZED/PO (czy boss zginal) + czy FINISH pala.
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const HOST='127.0.0.1', PORT=25565, RPORT=25575, RPASS=process.env.RCON_PASS, USER='Dungeoneer';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=30000,st=250)=>{const t=Date.now();while(Date.now()-t<to){const v=await fn();if(v)return v;await sleep(st);}return false;};
let rcon;
const dimOf=async()=>(await rcon.send(`data get entity ${USER} Dimension`).catch(()=>'')).match(/"([^"]+)"/)?.[1];
const zCount=bot=>Object.values(bot.entities).filter(e=>e.name==='zombie'&&e.position).length;
(async()=>{
  rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
  const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
  let finishChat=false, spawnMsg=false;
  bot.on('resourcePack',()=>bot.acceptResourcePack());
  bot.on('message',j=>{const t=j.toString();
    if(/budzi si|korzeni/i.test(t)){spawnMsg=true;console.log('[onSpawn]',t);}
    if(/you completed|uko[nń]czy|left the dungeon/i.test(t)){finishChat=true;console.log('[FINISH]',t);}});
  if(!await waitFor(()=>bot.entity,60000,500)){console.log('brak spawnu');process.exit(7);}
  await rcon.send(`op ${USER}`); await sleep(800);
  await rcon.send('say ===DEC789===');
  bot.chat('/md play level_1'); await sleep(4500);
  await rcon.send(`gamemode creative ${USER}`).catch(()=>{});
  const dimIn=await dimOf(); console.log('[bot] instancja=',dimIn);

  const sawBoss=await waitFor(()=>zCount(bot)>0,20000,500);
  console.log('[PRZED] boss/zombie widoczne:', zCount(bot), '| onSpawn-msg:', spawnMsg);
  if(!sawBoss){console.log('[FATAL] brak bossa w instancji');process.exit(3);}

  // KILL przez RCON-konsole, w swiecie gracza (execute as/at). Feedback pusty = normalne dla execute as.
  console.log('[step] RCON-konsola: kill zombie wokol gracza (x3)');
  for(let i=0;i<3;i++){
    await rcon.send(`execute as ${USER} at ${USER} run kill @e[type=minecraft:zombie,distance=..50]`).catch(()=>{});
    await sleep(1200);
  }
  await sleep(1500);
  console.log('[PO] zombie widoczne:', zCount(bot));

  const finished=await waitFor(async()=>finishChat||((await dimOf())!==dimIn),10000,500);
  const dimPo=await dimOf();
  console.log('[wynik] dimIn=',dimIn,'dimPo=',dimPo,'finishChat=',finishChat);
  console.log('\n[WERDYKT] boss zginal:', zCount(bot)===0?'TAK':'NIE/CZESC', '| FINISH:', finished?'TAK ✅':'NIE ❌');
  bot.chat('/md leave'); await sleep(1500); // czyste wyjscie
  try{await rcon.end();}catch{} try{bot.quit();}catch{}
  process.exit(0);
})().catch(e=>{console.error('FATAL',e);process.exit(1);});
