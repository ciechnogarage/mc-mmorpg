// Test summonowania adds przez level_1_grove_guardian (codex2). Wejscie na trudnosci z argv,
// pobyt w fazie ready (bez bicia bossa), pomiar liczby level_1_grove_wolf(WOLF)+level_1_corrupted_sprout(HUSK)
// w wymiarze instancji przez RCON `execute store result` (dokladny licznik, nie percepcja bota).
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const HOST='127.0.0.1', PORT=25565, RPORT=25575, RPASS=process.env.RCON_PASS, USER='Dungeoneer';
const DIFF=(process.argv[2]||'HARD').toUpperCase();
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=30000,st=250)=>{const t=Date.now();while(Date.now()-t<to){const v=await fn();if(v)return v;await sleep(st);}return false;};
let rcon;
const dimOf=async()=>(await rcon.send(`data get entity ${USER} Dimension`).catch(()=>'')).match(/"([^"]+)"/)?.[1];
const count=async(dim,type)=>{
  await rcon.send(`scoreboard objectives add advcnt dummy`).catch(()=>{});
  await rcon.send(`execute in ${dim} store result score #c advcnt run execute if entity @e[type=${type}]`);
  const r=await rcon.send(`scoreboard players get #c advcnt`);
  return parseInt((r.match(/has (\d+)/)||[])[1]||'0',10);
};
(async()=>{
  rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
  const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
  let titles=[];
  bot.on('resourcePack',()=>bot.acceptResourcePack());
  bot.on('message',j=>{const t=j.toString(); if(/Szmer|Korzenie|Straznik|Wsciekly|posilki/i.test(t))titles.push(t.slice(0,60));});
  if(!await waitFor(()=>bot.entity,60000,500)){console.log('brak spawnu');process.exit(7);}
  await rcon.send(`op ${USER}`); await sleep(800);
  bot.chat(`/md play level_1:${DIFF}`); await sleep(4500);
  await rcon.send(`gamemode creative ${USER}`).catch(()=>{});
  const dim=await dimOf();
  console.log(`[${DIFF}] instancja=`,dim);
  if(!dim||/overworld/.test(dim)){console.log('NIE wszedl do instancji ❌');process.exit(3);}
  // boss to zombie; potwierdz ze zyje
  const boss=await count(dim,'minecraft:zombie');
  console.log(`[${DIFF}] boss(zombie) w instancji:`,boss);
  // czekaj na pierwsza fale ready (onTimer:220=~11s + delay20t)
  console.log(`[${DIFF}] czekam 16s na fale ready...`);
  await sleep(16000);
  const wolves=await count(dim,'minecraft:wolf');
  const sprouts=await count(dim,'minecraft:husk');
  console.log(`[${DIFF}] === SNAPSHOT FAZA READY ===`);
  console.log(`[${DIFF}] level_1_grove_wolf(wolf)=`,wolves,'| level_1_corrupted_sprout(husk)=',sprouts);
  console.log(`[${DIFF}] telegrafy zauwazone:`, titles.slice(0,3).join(' | ')||'(brak)');
  const ok = (wolves+sprouts)>0;
  console.log(`[${DIFF}] [WERDYKT] adds spawnuja:`, ok?'TAK ✅':'NIE ❌');
  bot.chat('/md leave'); await sleep(1500);
  try{await rcon.end();}catch{} try{bot.quit();}catch{}
  process.exit(ok?0:1);
})().catch(e=>{console.error('FATAL',e);process.exit(1);});
