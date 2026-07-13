// Naprawa pustego gamerules.yml mapy level_1.
// MD: "Gamerules file corrupted!! Open this dungeon in edit mode and save to fix it!"
// Bot wchodzi w /md edit level_1, ustawia kluczowe gamerule, potem /md leave (zapis+unload).
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const HOST='127.0.0.1', PORT=25565, RPORT=25575, RPASS=process.env.RCON_PASS, USER='Fixer';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=30000,st=300)=>{const t=Date.now();while(Date.now()-t<to){if(await fn())return true;await sleep(st);}return false;};
(async()=>{
  const rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
  const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
  const log=[],say=(...a)=>{console.log(...a);log.push(a.join(' '));};
  bot.on('resourcePack',()=>bot.acceptResourcePack());
  bot.on('message',j=>{const t=j.toString();if(/edit|gamerule|saved?|dungeon|corrupt/i.test(t))say('[CHAT]',t);});
  bot.on('kicked',r=>say('[KICK]',JSON.stringify(r)));
  if(!await waitFor(()=>bot.entity,60000,500)){say('[FATAL] brak spawnu');process.exit(7);}
  await rcon.send(`op ${USER}`); await sleep(800);

  say('[step] /md edit level_1');
  bot.chat('/md edit level_1');
  await sleep(5000);
  say('[bot] po edit -> dim=',bot.game.dimension,'pos=',bot.entity.position.toString());

  // Ustaw kluczowe gamerule w swiecie edycji (brakujacy doImmediateRespawn powodowal NPE).
  for(const g of ['doImmediateRespawn true','keepInventory true','showDeathMessages true','doDaylightCycle false','doMobSpawning false']){
    bot.chat('/gamerule '+g); await sleep(400);
  }
  await sleep(1000);

  say('[step] /md leave (zapis + unload)');
  bot.chat('/md leave');
  await sleep(5000);

  // wymus reload mapy zeby MD przeczytal nowy gamerules.yml
  say('[step] /md reload level_1');
  say('[rcon]', await rcon.send('md reload level_1').catch(e=>'ERR '+e.message));
  await sleep(2000);

  console.log('\n==== LOG ====\n'+log.join('\n'));
  try{await rcon.end();}catch{} try{bot.quit();}catch{}
  process.exit(0);
})().catch(e=>{console.error('FATAL',e);process.exit(1);});
