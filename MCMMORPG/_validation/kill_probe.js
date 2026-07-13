// Cel: realna smierc MythicMoba (onDeath -> TriggerMythicMobDeath -> finish), omijajac ME-invuln bazy.
const mineflayer=require('mineflayer');const{Rcon}=require('rcon-client');
const HOST='127.0.0.1',PORT=25565,RPORT=25575,RPASS=process.env.RCON_PASS,USER='Dungeoneer';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=60000,st=300)=>{const t=Date.now();while(Date.now()-t<to){if(await fn())return true;await sleep(st);}return false;};
let rcon;const C=async c=>(await rcon.send(c)).replace(/§[0-9a-fklmnorx]/g,'');
let DIM='';
const count=async()=>{await C(`scoreboard objectives add zc dummy`).catch(()=>{});await C(`execute in ${DIM} store result score N zc if entity @e[type=zombie]`).catch(()=>{});return parseInt(((await C('scoreboard players get N zc')).match(/has (\d+)/)||[])[1]||'0',10);};
(async()=>{
  rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
  const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
  bot.on('error',()=>{});process.on('uncaughtException',()=>{});
  let finishChat=false;bot.on('resourcePack',()=>bot.acceptResourcePack());
  bot.on('message',j=>{const t=j.toString();if(/dungeon|complet|uko[nń]czy|left the|polana|slain|died|gaju/i.test(t))console.log('[CHAT]',t);if(/completed|uko[nń]czy|left the dungeon/i.test(t))finishChat=true;});
  if(!await waitFor(()=>bot.entity)){console.log('no spawn');process.exit(7);}
  await C(`op ${USER}`);await sleep(800);await C(`gamemode survival ${USER}`);await sleep(3000);
  bot.chat('/md play level_1:NORMAL Dungeoneer');await sleep(6000);
  const d=((await C(`data get entity ${USER} Dimension`)).match(/"([^"]+)"/)||[])[1];DIM='minecraft:'+(d||'').replace(/^minecraft:/,'');
  console.log('DIM=',DIM);
  for(const [x,z] of [[0,0],[-16,-16],[16,16],[-16,16],[16,-16]])await C(`execute in ${DIM} run forceload add ${x} ${z}`);
  await sleep(1500);
  let bc=0;for(let i=0;i<8;i++){bc=await count();if(bc>0)break;await sleep(1000);}console.log('boss spawn=',bc);
  if(bc<1)process.exit(3);
  console.log('--- typy encji w instancji (struktura ME) ---');
  console.log(await C(`execute in ${DIM} run data get entity @e[limit=1,type=zombie] UUID`));
  for(const t of ['interaction','armor_stand','item_display','text_display','block_display','marker']){
    await C(`execute in ${DIM} store result score N zc if entity @e[type=${t}]`).catch(()=>{});
    const n=((await C('scoreboard players get N zc')).match(/has (\d+)/)||[])[1]||'0';
    if(n!=='0')console.log(`  ${t}: ${n}`);
  }
  console.log('--- metoda D: set Health:0 via /data (omija typ-damage) ---');
  console.log('  resp:',await C(`execute in ${DIM} run data merge entity @e[type=zombie,limit=1] {Health:0f}`));
  await sleep(2000);console.log('  zombie=',await count(),'finishChat=',finishChat);
  if(await count()>0){
    console.log('--- metoda E: damage void (czasem ME nie blokuje) ---');
    console.log('  resp:',await C(`execute in ${DIM} positioned 0 64 0 run damage @e[type=zombie,limit=1] 999999 minecraft:out_of_world`));
    await sleep(2000);console.log('  zombie=',await count(),'finishChat=',finishChat);
  }
  if(await count()>0){
    console.log('--- metoda F: damage interaction/hitbox positioned ---');
    console.log('  resp:',await C(`execute in ${DIM} positioned 0 64 0 run damage @e[type=interaction,limit=1,sort=nearest] 999999 minecraft:player_attack by ${USER}`));
    await sleep(2000);console.log('  zombie=',await count(),'finishChat=',finishChat);
  }
  for(let i=0;i<5;i++){await sleep(1500);if(finishChat){console.log('FINISH chat wykryty!');break;}}
  console.log(`\nVERDICT spawn=YES boss-die=${await count()===0} finish=${finishChat}`);
  try{await rcon.end();}catch{}try{bot.quit();}catch{}process.exit(0);
})().catch(e=>{console.error('FATAL',e.message);process.exit(1);});
