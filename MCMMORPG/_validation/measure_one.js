// Jednostrzalowy pomiar max HP bossa dla JEDNEJ trudnosci. Swiezy bot, czyste wyjscie.
// Uzycie: node measure_one.js HARD   (albo EASY / NORMAL)
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const HOST='127.0.0.1', PORT=25565, RPORT=25575, RPASS=process.env.RCON_PASS, USER='Dungeoneer';
const DIFF=(process.argv[2]||'HARD').toUpperCase();
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=25000,st=400)=>{const t=Date.now();while(Date.now()-t<to){const v=await fn();if(v)return v;await sleep(st);}return false;};
let rcon, bot, lastHealth=null, lives=null;
const dimOf=async()=>(await rcon.send(`data get entity ${USER} Dimension`).catch(()=>'')).match(/"([^"]+)"/)?.[1];
const inInst=d=>d&&d.includes('level_1_');

(async()=>{
  rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
  bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
  bot.on('resourcePack',()=>bot.acceptResourcePack());
  bot.on('message',j=>{const t=j.toString();
    const m=t.match(/following entity data:\s*([\d.]+)/); if(m)lastHealth=parseFloat(m[1]);
    if(/(life|lives|zyci|życi)/i.test(t))lives=t.slice(0,90);});
  if(!await waitFor(()=>bot.entity,60000,500)){console.log('NO-SPAWN');process.exit(7);}
  await rcon.send(`op ${USER}`); await sleep(700);
  await rcon.send(`gamerule sendCommandFeedback true`).catch(()=>{});

  // jesli ghost wrzucil bota do instancji — wyjdz najpierw
  let d0=await dimOf();
  if(inInst(d0)){ bot.chat('/md leave'); await waitFor(async()=>!inInst(await dimOf()),15000,500); }
  const hub=await dimOf();

  bot.chat(`/md play level_1:${DIFF}`);
  const inst=await waitFor(async()=>{const d=await dimOf();return inInst(d)?d:false;},20000,500);
  if(!inst){console.log(`RESULT ${DIFF} ENTER-FAIL hub=${hub}`);try{await rcon.end();}catch{}bot.quit();process.exit(3);}
  await rcon.send(`gamemode creative ${USER}`).catch(()=>{});

  let maxHP=0, reads=0;
  for(let i=0;i<16;i++){
    lastHealth=null;
    bot.chat('/data get entity @e[type=minecraft:zombie,distance=..60,limit=1,sort=nearest] Health');
    await sleep(900);
    if(lastHealth!=null){reads++; if(lastHealth>maxHP)maxHP=lastHealth;}
  }
  bot.chat('/md lives'); await sleep(1000);
  console.log(`RESULT ${DIFF} maxHP=${maxHP||'null'} reads=${reads} inst=${inst} lives="${lives||''}"`);
  bot.chat('/md leave'); await sleep(2000);
  try{await rcon.end();}catch{} try{bot.quit();}catch{}
  process.exit(0);
})().catch(e=>{console.error('FATAL',e.message);process.exit(1);});
