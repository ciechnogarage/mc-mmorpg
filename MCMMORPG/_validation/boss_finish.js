// Czekaj az bot ZOBACZY bossa, potem zabij i obserwuj finish. Pelna instrumentacja.
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const HOST='127.0.0.1', PORT=25565, RPORT=25575, RPASS=process.env.RCON_PASS, USER='Dungeoneer';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=30000,st=250)=>{const t=Date.now();while(Date.now()-t<to){const v=await fn();if(v)return v;await sleep(st);}return false;};
let rcon;
const dimOf=async()=>(await rcon.send(`data get entity ${USER} Dimension`).catch(()=>'')).match(/"([^"]+)"/)?.[1];
const zombies=bot=>{const me=bot.entity.position;return Object.values(bot.entities).filter(e=>e.name==='zombie'&&e.position).map(e=>({e,d:me.distanceTo(e.position)})).sort((a,b)=>a.d-b.d);};
(async()=>{
  rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
  const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
  let finishChat=false;
  bot.on('resourcePack',()=>bot.acceptResourcePack());
  bot.on('message',j=>{const t=j.toString();if(/you completed|uko[nń]czy|left the dungeon/i.test(t)){finishChat=true;console.log('[CHAT]',t);}});
  if(!await waitFor(()=>bot.entity,60000,500)){console.log('brak spawnu');process.exit(7);}
  await rcon.send(`op ${USER}`); await sleep(800);
  await rcon.send('say ===MARK456===');
  bot.chat('/md play level_1'); await sleep(4500);
  await rcon.send(`gamemode creative ${USER}`).catch(()=>{});
  const dimIn=await dimOf(); console.log('[bot] instancja=',dimIn);

  // czekaj az zobaczy zombie (boss) do 20s, loguj co sekunde
  let seen=false;
  for(let i=0;i<20 && !seen;i++){
    const zs=zombies(bot);
    if(i%2===0) console.log(`  t=${i}s zombie widoczne: ${zs.length}`+(zs[0]?` najbl. dist=${zs[0].d.toFixed(1)}`:''));
    if(zs.length){seen=true;break;}
    await sleep(1000);
  }
  console.log('[bot] boss widziany?', seen);
  if(!seen){console.log('[FATAL] boss nigdy nie pojawil sie w percepcji bota -> spawn flaky/nie dziala');process.exit(3);}

  // zabijaj najblizsze zombie az finish (zmiana wymiaru) lub 20s
  console.log('[step] zabijam bossa, czekam na finish');
  let finished=false, dimNow=dimIn;
  for(let i=0;i<14 && !finished;i++){
    bot.chat('/kill @e[type=minecraft:zombie,sort=nearest,limit=1,distance=..40]');
    await sleep(1400);
    dimNow=await dimOf();
    if(finishChat || (dimNow&&dimNow!==dimIn)){finished=true;}
  }
  console.log('[wynik] dimIn=',dimIn,'dimPo=',dimNow,'finishChat=',finishChat);
  console.log('\n[WERDYKT] FINISH:', finished?'TAK ✅':'NIE ❌');
  try{await rcon.end();}catch{} try{bot.quit();}catch{}
  process.exit(finished?0:1);
})().catch(e=>{console.error('FATAL',e);process.exit(1);});
